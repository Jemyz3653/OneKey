package dev.jemyz.onekey.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;

import dev.jemyz.onekey.client.KeyScreen;
import dev.jemyz.onekey.client.OneKeyClient;
import dev.jemyz.onekey.server.KeyManager;

/**
 * Joins a dedicated server running OneKey and walks through: key prompt, wrong key, right key,
 * boxes + invisible "players" (mobs stand in for players, there is only one client), toggles,
 * and /OneKey new revoking access. Screenshots: build/run/clientGameTest/screenshots.
 */
public class OneKeyClientGameTest implements FabricClientGameTest {
	private final List<String> notes = new ArrayList<>();
	private Path shotDir;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(client -> client.options.guiScale().set(2));
		OneKeyClient.testAllLiving = true;

		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
				TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();

			boolean prompt = context.waitFor(c -> c.gui.screen() instanceof KeyScreen, 200) >= 0;
			note("key screen shown: " + prompt);
			shot(context, "1_key_screen");

			context.getInput().typeChars("ABCD-EFGH");
			context.clickScreenButton("onekey.screen.activate");
			context.waitTicks(10);
			shot(context, "2_wrong_key");
			note("active after wrong key: " + context.computeOnClient(c -> OneKeyClient.active()));

			String key = KeyManager.key();
			note("server key: " + key);
			context.runOnClient(c -> {
				if (c.gui.screen() instanceof KeyScreen) c.gui.setScreen(null);
			});
			context.waitTicks(2);
			context.getInput().pressKey(o -> findKey(o, "key.onekey.enter"));
			context.waitTicks(5);
			context.getInput().typeChars(key.toLowerCase());
			context.clickScreenButton("onekey.screen.activate");
			context.waitTicks(10);
			note("active after right key: " + context.computeOnClient(c -> OneKeyClient.active()));

			// scene: player looks south, a visible and an invisible mob in front of it, one behind a wall
			server.runCommand("/gamemode creative @a");
			server.runCommand("/tp @a 0 -60 0 0 0");
			server.runCommand("/fill -6 -61 3 6 -61 14 minecraft:stone");
			server.runCommand("/summon minecraft:villager 3 -60 8 {NoAI:1b,CustomName:'Visible'}");
			server.runCommand("/summon minecraft:villager -3 -60 8 {NoAI:1b,CustomName:'Invisible',active_effects:[{id:\"minecraft:invisibility\",duration:-1,show_particles:0b}]}");
			server.runCommand("/fill -2 -60 11 2 -56 11 minecraft:stone_bricks");
			server.runCommand("/summon minecraft:villager 0 -60 13 {NoAI:1b,CustomName:'Behind wall'}");
			connection.waitForChunksRender();
			context.waitTicks(20);
			shot(context, "3_activated_esp");

			context.getInput().pressKey(o -> findKey(o, "key.onekey.invisible"));
			context.waitTicks(5);
			shot(context, "4_invisible_off");

			context.getInput().pressKey(o -> findKey(o, "key.onekey.boxes"));
			context.waitTicks(5);
			shot(context, "5_boxes_off");

			context.getInput().pressKey(o -> findKey(o, "key.onekey.boxes"));
			context.getInput().pressKey(o -> findKey(o, "key.onekey.invisible"));
			context.waitTicks(5);

			server.runCommand("/OneKey new");
			context.waitTicks(10);
			note("active after /OneKey new: " + context.computeOnClient(c -> OneKeyClient.active()));
			shot(context, "6_revoked");
		} catch (Throwable t) {
			note("FAILED: " + t);
			t.printStackTrace();
		}

		OneKeyClient.testAllLiving = false;
		writeNotes();
	}

	private static net.minecraft.client.KeyMapping findKey(net.minecraft.client.Options options, String name) {
		for (net.minecraft.client.KeyMapping k : options.keyMappings) {
			if (k.getName().equals(name)) return k;
		}

		throw new IllegalStateException("no key " + name);
	}

	private void shot(ClientGameTestContext context, String name) {
		Path p = context.takeScreenshot(name);
		shotDir = p.getParent();
		note("screenshot " + name);
	}

	private void note(String s) {
		notes.add(s);
		System.out.println("[OneKeyTest] " + s);
	}

	private void writeNotes() {
		if (shotDir == null) return;

		try {
			Files.writeString(shotDir.resolve("notes.txt"), String.join("\n", notes) + "\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
