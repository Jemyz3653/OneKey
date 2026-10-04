package dev.jemyz.onekey.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import dev.jemyz.onekey.OneKey;
import dev.jemyz.onekey.net.AuthPayload;
import dev.jemyz.onekey.net.HelloPayload;
import dev.jemyz.onekey.net.ResultPayload;

public final class OneKeyClient implements ClientModInitializer {
	/** True only after the server accepted our key. Nothing works without it. */
	private static volatile boolean active;
	private static boolean serverHasOneKey;
	private static boolean promptPending;
	private static boolean lastWasAuto;
	private static String lastSubmitted = "";

	private static boolean showBoxes = true;
	private static boolean showInvisible = true;

	private static KeyMapping boxesKey;
	private static KeyMapping invisibleKey;
	private static KeyMapping enterKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(OneKey.id("main"));
		boxesKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.onekey.boxes", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_F7, category));
		invisibleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.onekey.invisible", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_F8, category));
		enterKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.onekey.enter", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_K, category));

		ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (payload, context) -> onHello());
		ClientPlayNetworking.registerGlobalReceiver(ResultPayload.TYPE, (payload, context) -> onResult(payload));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
		ClientTickEvents.END_CLIENT_TICK.register(OneKeyClient::tick);

		HudElementRegistry.addLast(OneKey.id("boxes"), EspHud::extract);
	}

	// ------------------------------------------------------------------ state

	public static boolean active() {
		return active;
	}

	public static boolean boxesOn() {
		return active && showBoxes;
	}

	public static boolean invisibleOn() {
		return active && showInvisible;
	}

	private static void reset() {
		active = false;
		serverHasOneKey = false;
		promptPending = false;
		lastWasAuto = false;
	}

	/** Address of the current server, used to remember the key per server. */
	private static String serverId() {
		Minecraft mc = Minecraft.getInstance();
		ServerData data = mc.getCurrentServer();
		return data != null ? data.ip.toLowerCase(java.util.Locale.ROOT) : "singleplayer";
	}

	// ------------------------------------------------------------------ network

	private static void onHello() {
		serverHasOneKey = true;
		String saved = SavedKeys.get(serverId());

		if (saved != null) {
			submit(saved, true);
		} else {
			promptPending = true;
		}
	}

	static void submit(String key, boolean auto) {
		lastSubmitted = key;
		lastWasAuto = auto;
		ClientPlayNetworking.send(new AuthPayload(key));
	}

	private static void onResult(ResultPayload result) {
		Minecraft mc = Minecraft.getInstance();
		KeyScreen screen = mc.gui.screen() instanceof KeyScreen ks ? ks : null;

		switch (result.status()) {
			case ResultPayload.OK -> {
				active = true;
				SavedKeys.put(serverId(), lastSubmitted);
				if (screen != null) screen.onClose();
				actionBar(Component.translatable("onekey.msg.activated").withStyle(ChatFormatting.GREEN));
			}
			case ResultPayload.WRONG, ResultPayload.LOCKED -> {
				active = false;

				if (lastWasAuto) {
					// the server got a new key since last time
					SavedKeys.remove(serverId());
					promptPending = true;
				} else if (screen != null) {
					Component text = result.status() == ResultPayload.LOCKED
							? Component.literal(result.message())
							: Component.translatable("onekey.msg.wrong");
					screen.setStatus(text, 0xFFFF5555);
				}
			}
			case ResultPayload.REVOKED -> {
				active = false;
				SavedKeys.remove(serverId());
				actionBar(Component.translatable("onekey.msg.revoked").withStyle(ChatFormatting.RED));
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ tick / keys

	private static void tick(Minecraft mc) {
		if (mc.player == null) return;

		if (promptPending && mc.gui.screen() == null) {
			promptPending = false;
			mc.gui.setScreen(new KeyScreen());
		}

		while (enterKey.consumeClick()) {
			if (serverHasOneKey && !active) {
				mc.gui.setScreen(new KeyScreen());
			} else if (!serverHasOneKey) {
				actionBar(Component.translatable("onekey.msg.not_supported").withStyle(ChatFormatting.GRAY));
			}
		}

		while (boxesKey.consumeClick()) {
			if (!active) {
				notActive();
				continue;
			}

			showBoxes = !showBoxes;
			actionBar(Component.translatable(showBoxes ? "onekey.msg.boxes_on" : "onekey.msg.boxes_off"));
		}

		while (invisibleKey.consumeClick()) {
			if (!active) {
				notActive();
				continue;
			}

			showInvisible = !showInvisible;
			actionBar(Component.translatable(showInvisible ? "onekey.msg.invisible_on" : "onekey.msg.invisible_off"));
		}
	}

	private static void notActive() {
		actionBar(Component.translatable(serverHasOneKey ? "onekey.msg.need_key" : "onekey.msg.not_supported").withStyle(ChatFormatting.GRAY));
	}

	private static void actionBar(Component text) {
		Minecraft.getInstance().gui.hud.setOverlayMessage(text, false);
	}
}
