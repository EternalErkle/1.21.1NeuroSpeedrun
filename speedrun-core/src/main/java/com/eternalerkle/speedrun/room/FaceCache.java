package com.eternalerkle.speedrun.room;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Downloads each player's skin once and keeps the 8x8 front face with the hat layer applied, as 0xRRGGBB values.
 * Fetches run off the server thread. Missing faces fall back to Steve.
 */
public final class FaceCache {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	private final Map<UUID, int[]> faces = new ConcurrentHashMap<>();

	public void fetch(GameProfile profile) {
		if (faces.containsKey(profile.getId())) {
			return;
		}
		CompletableFuture.supplyAsync(() -> skinUrl(profile))
			.thenApply(url -> url == null ? null : download(url))
			.thenAccept(face -> {
				if (face != null) {
					faces.put(profile.getId(), face);
				}
			})
			.exceptionally(error -> {
				SpeedrunCore.LOGGER.warn("Could not fetch skin for {}: {}", profile.getName(), error.toString());
				return null;
			});
	}

	/** The player's face, or Steve's when it has not been fetched. Row-major, 64 entries. */
	public int[] face(UUID id) {
		return faces.getOrDefault(id, STEVE);
	}

	private static String skinUrl(GameProfile profile) {
		String texturesJson = null;
		for (Property property : profile.getProperties().get("textures")) {
			texturesJson = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
		}
		if (texturesJson == null) {
			texturesJson = lookupByName(profile.getName());
		}
		if (texturesJson == null) {
			return null;
		}
		JsonObject textures = JsonParser.parseString(texturesJson).getAsJsonObject().getAsJsonObject("textures");
		if (textures == null || !textures.has("SKIN")) {
			return null;
		}
		return textures.getAsJsonObject("SKIN").get("url").getAsString();
	}

	/** Offline-mode servers have no skin data in the profile, so resolve the name through Mojang's API. */
	private static String lookupByName(String name) {
		try {
			String idJson = get("https://api.mojang.com/users/profiles/minecraft/" + name);
			if (idJson == null) {
				return null;
			}
			String id = JsonParser.parseString(idJson).getAsJsonObject().get("id").getAsString();
			String profileJson = get("https://sessionserver.mojang.com/session/minecraft/profile/" + id);
			if (profileJson == null) {
				return null;
			}
			for (var property : JsonParser.parseString(profileJson).getAsJsonObject().getAsJsonArray("properties")) {
				JsonObject object = property.getAsJsonObject();
				if ("textures".equals(object.get("name").getAsString())) {
					return new String(Base64.getDecoder().decode(object.get("value").getAsString()), StandardCharsets.UTF_8);
				}
			}
		} catch (Exception e) {
			SpeedrunCore.LOGGER.debug("Name lookup failed for {}", name, e);
		}
		return null;
	}

	private static String get(String url) throws Exception {
		HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
		return response.statusCode() == 200 ? response.body() : null;
	}

	private static int[] download(String url) {
		try {
			HttpResponse<byte[]> response = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200) {
				return null;
			}
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
			return image == null ? null : extractFace(image);
		} catch (Exception e) {
			SpeedrunCore.LOGGER.warn("Skin download failed: {}", e.toString());
			return null;
		}
	}

	/** Face is at (8,8) and the hat overlay at (40,8) on a 64-wide skin. Handles HD skins by scaling. */
	static int[] extractFace(BufferedImage image) {
		int scale = Math.max(1, image.getWidth() / 64);
		int[] face = new int[64];
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				int base = image.getRGB((8 + x) * scale, (8 + y) * scale);
				int hat = image.getRGB((40 + x) * scale, (8 + y) * scale);
				int alpha = hat >>> 24;
				int color = alpha >= 128 ? hat : base;
				face[y * 8 + x] = color & 0xFFFFFF;
			}
		}
		return face;
	}

	private static final int H = 0x2B1E0D, h = 0x3F2A15, S = 0xB4846D, s = 0xAA7D66, W = 0xFFFFFF, E = 0x523D89, N = 0x9C6340, M = 0x6A4030;
	/** Approximation of the default Steve face. */
	static final int[] STEVE = {
		H, H, H, H, H, H, H, H,
		H, H, H, H, H, H, H, H,
		H, S, S, S, S, S, S, H,
		S, S, S, S, S, S, S, S,
		S, W, E, S, S, E, W, S,
		s, S, S, N, N, S, S, s,
		S, S, M, h, h, M, S, S,
		S, S, M, M, M, M, S, S,
	};
}
