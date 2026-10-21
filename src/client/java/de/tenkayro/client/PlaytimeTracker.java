package de.tenkayro.client;

import de.tenkayro.client.config.PlayedConfig;
import de.tenkayro.Played;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

public final class PlaytimeTracker {
	private static final long SAVE_INTERVAL_MILLIS = 15_000L;

	private static Path currentWorldDirectory;
	private static String currentServerAddress;
	private static long lastTickNanos;
	private static long lastSaveNanos;
	private static long sessionMillis;
	private static boolean tracking;
	private static boolean paused;

	private static double lastX;
	private static double lastY;
	private static double lastZ;
	private static float lastXRot;
	private static float lastYRot;
	private static long lastActivityNanos;

	private PlaytimeTracker() {
	}

	public static void tick(Minecraft client) {
		Path worldDirectory = PlayedConfig.shouldTrackWorlds() ? getCurrentWorldDirectory(client) : null;
		ServerData serverInfo = PlayedConfig.shouldTrackServers() ? getCurrentServer(client) : null;
		if ((worldDirectory == null && serverInfo == null) || client.isPaused()) {
			pause();
			return;
		}

		long nowNanos = System.nanoTime();
		boolean isAfk = false;
		var player = client.player;
		if (PlayedConfig.shouldPauseOnAfk() && player != null) {
			double x = player.getX();
			double y = player.getY();
			double z = player.getZ();
			float xRot = player.getXRot();
			float yRot = player.getYRot();
			
			if (x != lastX || y != lastY || z != lastZ || xRot != lastXRot || yRot != lastYRot) {
				lastX = x;
				lastY = y;
				lastZ = z;
				lastXRot = xRot;
				lastYRot = yRot;
				lastActivityNanos = nowNanos;
			}
			
			if (nowNanos - lastActivityNanos > PlayedConfig.getAfkTimeoutSeconds() * 1_000_000_000L) {
				isAfk = true;
			}
		}

		if (isAfk) {
			pause();
			return;
		}
		if (worldDirectory != null) {
			tickWorld(worldDirectory, nowNanos);
			return;
		}

		tickServer(serverInfo, nowNanos);
	}

	public static void flush() {
		if (currentWorldDirectory != null) {
			PlaytimeStorage.save(currentWorldDirectory);
		} else {
			PlaytimeStorage.saveCentral();
		}
	}

	public static void stop() {
		flush();
		currentWorldDirectory = null;
		currentServerAddress = null;
		lastTickNanos = 0L;
		lastSaveNanos = 0L;
		sessionMillis = 0L;
		tracking = false;
		paused = false;
	}

	public static long getSessionPlaytimeMillis() {
		return sessionMillis;
	}

	public static long getCurrentPlaytimeMillis(Minecraft client) {
		Path worldDirectory = getCurrentWorldDirectory(client);
		if (worldDirectory != null) {
			return PlaytimeStorage.getWorldMillis(worldDirectory);
		}

		ServerData serverInfo = getCurrentServer(client);
		if (serverInfo != null) {
			return PlaytimeStorage.getServerMillis(serverInfo.name, serverInfo.ip);
		}

		return PlaytimeStorage.getAllTimeMillis();
	}

	public static long getActivePlaytimeMillis(Minecraft client) {
		Path worldDirectory = getCurrentWorldDirectory(client);
		if (worldDirectory != null) {
			return PlaytimeStorage.getWorldMillis(worldDirectory);
		}

		ServerData serverInfo = getCurrentServer(client);
		if (serverInfo != null) {
			return PlaytimeStorage.getServerMillis(serverInfo.name, serverInfo.ip);
		}

		return 0L;
	}

	private static void tickWorld(Path worldDirectory, long nowNanos) {
		if (!tracking || currentWorldDirectory == null || !worldDirectory.equals(currentWorldDirectory)) {
			flush();
			currentWorldDirectory = worldDirectory;
			currentServerAddress = null;
			lastTickNanos = nowNanos;
			lastSaveNanos = nowNanos;
			sessionMillis = 0L;
			lastActivityNanos = nowNanos;
			tracking = true;
			paused = false;
			PlaytimeStorage.getWorldMillis(worldDirectory);
			Played.LOGGER.info("Started world playtime tracking for {}", worldDirectory);
			return;
		}

		paused = false;
		long elapsedMillis = (nowNanos - lastTickNanos) / 1_000_000L;
		lastTickNanos += elapsedMillis * 1_000_000L;
		sessionMillis += elapsedMillis;
		PlaytimeStorage.addPlaytime(worldDirectory, elapsedMillis);

		if (nowNanos - lastSaveNanos >= SAVE_INTERVAL_MILLIS * 1_000_000L) {
			PlaytimeStorage.save(worldDirectory);
			lastSaveNanos = nowNanos;
		}
	}

	private static void tickServer(ServerData serverInfo, long nowNanos) {
		String serverAddress = normalizeServerAddress(serverInfo.ip);
		if (!tracking || currentServerAddress == null || !serverAddress.equals(currentServerAddress)) {
			flush();
			currentWorldDirectory = null;
			currentServerAddress = serverAddress;
			lastTickNanos = nowNanos;
			lastSaveNanos = nowNanos;
			sessionMillis = 0L;
			lastActivityNanos = nowNanos;
			tracking = true;
			paused = false;
			PlaytimeStorage.getServerMillis(serverInfo.name, serverInfo.ip);
			Played.LOGGER.info("Started server playtime tracking for {}", serverInfo.ip);
			return;
		}

		paused = false;
		long elapsedMillis = (nowNanos - lastTickNanos) / 1_000_000L;
		lastTickNanos += elapsedMillis * 1_000_000L;
		sessionMillis += elapsedMillis;
		PlaytimeStorage.addServerPlaytime(serverInfo.name, serverAddress, elapsedMillis);

		if (nowNanos - lastSaveNanos >= SAVE_INTERVAL_MILLIS * 1_000_000L) {
			PlaytimeStorage.saveCentral();
			lastSaveNanos = nowNanos;
		}
	}

	private static void pause() {
		if (tracking) {
			if (!paused) {
				flush();
				paused = true;
			}
			lastTickNanos = System.nanoTime();
		}
	}

	private static Path getCurrentWorldDirectory(Minecraft client) {
		IntegratedServer server = client.getSingleplayerServer();
		if (client.level == null || client.player == null || !client.hasSingleplayerServer() || server == null) {
			return null;
		}

		return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
	}

	private static ServerData getCurrentServer(Minecraft client) {
		if (client.level == null || client.player == null || client.hasSingleplayerServer()) {
			return null;
		}

		ServerData serverInfo = client.getCurrentServer();
		return serverInfo != null && serverInfo.ip != null && !serverInfo.ip.isBlank() ? serverInfo : null;
	}

	private static String normalizeServerAddress(String serverAddress) {
		return serverAddress.trim().toLowerCase(Locale.ROOT);
	}
}
