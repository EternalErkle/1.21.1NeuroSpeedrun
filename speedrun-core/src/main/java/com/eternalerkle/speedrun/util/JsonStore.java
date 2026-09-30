package com.eternalerkle.speedrun.util;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

/** Reads and atomically writes JSON files under config/speedrun-core/. */
public final class JsonStore {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private JsonStore() {
	}

	public static <T> T load(Path file, Class<T> type, Supplier<T> fallback) {
		if (!Files.exists(file)) {
			return fallback.get();
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			T value = GSON.fromJson(reader, type);
			return value != null ? value : fallback.get();
		} catch (Exception e) {
			Path broken = file.resolveSibling(file.getFileName() + ".broken");
			SpeedrunCore.LOGGER.error("Failed to read {}, moving it to {} and using defaults", file, broken, e);
			try {
				Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
			}
			return fallback.get();
		}
	}

	public static void save(Path file, Object value) {
		try {
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(value, writer);
			}
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			SpeedrunCore.LOGGER.error("Failed to write {}", file, e);
		}
	}
}
