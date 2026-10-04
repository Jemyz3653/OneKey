package dev.jemyz.onekey.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.loader.api.FabricLoader;

import dev.jemyz.onekey.OneKey;

/** config/onekey-client.json: server address -> key that worked there. */
final class SavedKeys {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Map<String, String> keys;

	private SavedKeys() {
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("onekey-client.json");
	}

	private static Map<String, String> map() {
		if (keys == null) {
			keys = new LinkedHashMap<>();

			try {
				if (Files.exists(path())) {
					try (Reader r = Files.newBufferedReader(path(), StandardCharsets.UTF_8)) {
						Map<String, String> loaded = GSON.fromJson(r, new TypeToken<Map<String, String>>() { }.getType());
						if (loaded != null) keys.putAll(loaded);
					}
				}
			} catch (Exception e) {
				OneKey.LOGGER.warn("Could not read {}", path(), e);
			}
		}

		return keys;
	}

	static String get(String server) {
		return map().get(server);
	}

	static void put(String server, String key) {
		map().put(server, key);
		save();
	}

	static void remove(String server) {
		if (map().remove(server) != null) save();
	}

	private static void save() {
		try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
			GSON.toJson(map(), w);
		} catch (Exception e) {
			OneKey.LOGGER.warn("Could not write {}", path(), e);
		}
	}
}
