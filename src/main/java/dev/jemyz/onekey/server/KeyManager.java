package dev.jemyz.onekey.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.jemyz.onekey.OneKey;
import dev.jemyz.onekey.net.ResultPayload;

/** The server key and who has unlocked OneKey with it. Server thread only, except reads. */
public final class KeyManager {
	/** No 0/O, 1/I/L: easy to read out loud and to type. */
	private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private static final int MAX_FAILS = 5;
	private static final long FAIL_WINDOW_MS = 60_000;

	private static volatile String key = "";
	private static final Set<UUID> AUTHORIZED = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, Deque<Long>> FAILS = new ConcurrentHashMap<>();

	private KeyManager() {
	}

	public static String key() {
		return key;
	}

	public static String newKey() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 8; i++) {
			if (i == 4) sb.append('-');
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}

		key = sb.toString();
		AUTHORIZED.clear();
		FAILS.clear();
		return key;
	}

	static String normalize(String input) {
		return input == null ? "" : input.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
	}

	public static boolean isAuthorized(ServerPlayer player) {
		return AUTHORIZED.contains(player.getUUID());
	}

	public static Collection<UUID> authorized() {
		return AUTHORIZED;
	}

	public static void forget(ServerPlayer player) {
		AUTHORIZED.remove(player.getUUID());
	}

	/** A client sent a key. */
	public static void onAuth(ServerPlayer player, String input) {
		UUID id = player.getUUID();
		long now = System.currentTimeMillis();
		Deque<Long> fails = FAILS.computeIfAbsent(id, u -> new ArrayDeque<>());

		while (!fails.isEmpty() && now - fails.peekFirst() > FAIL_WINDOW_MS) fails.pollFirst();

		if (fails.size() >= MAX_FAILS) {
			long wait = (FAIL_WINDOW_MS - (now - fails.peekFirst())) / 1000 + 1;
			reply(player, ResultPayload.LOCKED, "Too many wrong keys. Try again in " + wait + " s.");
			return;
		}

		byte[] a = normalize(input).getBytes(StandardCharsets.US_ASCII);
		byte[] b = normalize(key).getBytes(StandardCharsets.US_ASCII);

		if (b.length > 0 && MessageDigest.isEqual(a, b)) {
			AUTHORIZED.add(id);
			fails.clear();
			reply(player, ResultPayload.OK, "OneKey activated");
			OneKey.LOGGER.info("[OneKey] {} unlocked OneKey", player.getName().getString());
		} else {
			fails.addLast(now);
			int left = MAX_FAILS - fails.size();
			reply(player, ResultPayload.WRONG, left > 0 ? "Wrong key (" + left + " tries left)" : "Wrong key. Locked for a minute.");
			OneKey.LOGGER.info("[OneKey] {} entered a wrong key", player.getName().getString());
		}
	}

	/** Take OneKey away from everybody (new key) or one player. */
	public static void revokeAll(MinecraftServer server, String reason) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (AUTHORIZED.contains(p.getUUID())) reply(p, ResultPayload.REVOKED, reason);
		}

		AUTHORIZED.clear();
	}

	public static boolean revoke(ServerPlayer player, String reason) {
		boolean had = AUTHORIZED.remove(player.getUUID());
		if (had) reply(player, ResultPayload.REVOKED, reason);
		return had;
	}

	private static void reply(ServerPlayer player, int status, String message) {
		if (ServerPlayNetworking.canSend(player, ResultPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ResultPayload(status, message));
		}
	}
}
