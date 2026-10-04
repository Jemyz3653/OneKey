package dev.jemyz.onekey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.jemyz.onekey.net.AuthPayload;
import dev.jemyz.onekey.net.HelloPayload;
import dev.jemyz.onekey.net.ResultPayload;
import dev.jemyz.onekey.server.KeyManager;
import dev.jemyz.onekey.server.OneKeyCommand;

public final class OneKey implements ModInitializer {
	public static final String MOD_ID = "onekey";
	public static final Logger LOGGER = LoggerFactory.getLogger("OneKey");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ResultPayload.TYPE, ResultPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AuthPayload.TYPE, AuthPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(AuthPayload.TYPE, (payload, context) -> KeyManager.onAuth(context.player(), payload.key()));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			String key = KeyManager.newKey();
			LOGGER.info("[OneKey] Key for this session: {}   (/OneKey shows it again, /OneKey new makes a new one)", key);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, HelloPayload.TYPE)) {
				sender.sendPacket(new HelloPayload(HelloPayload.PROTOCOL));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> KeyManager.forget(handler.player));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> OneKeyCommand.register(dispatcher));
	}
}
