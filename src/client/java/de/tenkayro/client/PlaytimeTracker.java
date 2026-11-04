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
		// Initialize a new target before applying AFK detection, including stationary spawns.
		if (worldDirectory != null && (!tracking || !worldDirectory.equals(currentWorldDirectory))) {
			tickWorld(worldDirectory, nowNanos);
			return;
		}
		if (serverInfo != null && (!tracking || !normalizeServerAddress(serverInfo.ip).equals(currentServerAddress))) {
			tickServer(serverInfo, nowNanos);
			return;
		}
		if (client.isWindowActive() && client.gui.screen() == null) {
			var options = client.options;
			if (options.keyUp.isDown() || options.keyDown.isDown() || options.keyLeft.isDown()
				|| options.keyRight.isDown() || options.keyJump.isDown() || options.keyAttack.isDown()
				|| options.keyUse.isDown()) activity();
		}
		if (PlayedConfig.shouldPauseOnAfk()
			&& nowNanos - lastActivityNanos > PlayedConfig.getAfkTimeoutSeconds() * 1_000_000_000L) {
			pause();
			return;
		}
		if (worldDirectory != null) {
			tickWorld(worldDirectory, nowNanos);
			return;
		}

		tickServer(serverInfo, nowNanos);
	}

	public static void activity() { lastActivityNanos = System.nanoTime(); }

	public static boolean isPaused() { return paused; }

	public static void flush() {
		PlaytimeHistory.save();
		if (currentWorldDirectory != null) {
			PlaytimeStorage.save(currentWorldDirectory);
		} else {
			PlaytimeStorage.saveCentral();
		}
	}

	public static void stop() {
		PlaytimeHistory.finish();
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
			String name = worldDirectory.getFileName().toString();
			PlaytimeHistory.begin("world:" + name, name, System.currentTimeMillis());
			Played.LOGGER.info("Started world playtime tracking for {}", worldDirectory);
			return;
		}

		paused = false;
		long elapsedMillis = (nowNanos - lastTickNanos) / 1_000_000L;
		lastTickNanos += elapsedMillis * 1_000_000L;
		sessionMillis += elapsedMillis;
		long previous = PlaytimeStorage.getWorldMillis(worldDirectory);
		PlaytimeStorage.addPlaytime(worldDirectory, elapsedMillis);
		record(elapsedMillis, previous, PlaytimeStorage.getWorldMillis(worldDirectory), worldDirectory.getFileName().toString());

		if (nowNanos - lastSaveNanos >= SAVE_INTERVAL_MILLIS * 1_000_000L) {
			flush();
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
			PlaytimeHistory.begin("server:" + serverAddress, java.util.Objects.requireNonNullElse(serverInfo.name, serverAddress), System.currentTimeMillis());
			Played.LOGGER.info("Started server playtime tracking for {}", serverInfo.ip);
			return;
		}

		paused = false;
		long elapsedMillis = (nowNanos - lastTickNanos) / 1_000_000L;
		lastTickNanos += elapsedMillis * 1_000_000L;
		sessionMillis += elapsedMillis;
		long previous = PlaytimeStorage.getServerMillis(serverInfo.name, serverAddress);
		PlaytimeStorage.addServerPlaytime(serverInfo.name, serverAddress, elapsedMillis);
		record(elapsedMillis, previous, PlaytimeStorage.getServerMillis(serverInfo.name, serverAddress), java.util.Objects.requireNonNullElse(serverInfo.name, serverAddress));

		if (nowNanos - lastSaveNanos >= SAVE_INTERVAL_MILLIS * 1_000_000L) {
			flush();
			lastSaveNanos = nowNanos;
		}
	}

	private static void record(long elapsed, long previous, long current, String name) {
		PlaytimeHistory.record(elapsed, System.currentTimeMillis(), java.time.ZoneId.systemDefault());
		if (!PlayedConfig.shouldNotifyMilestones()) return;
		long milestone = Milestones.crossed(previous, current);
		if (milestone > 0) {
			var client = Minecraft.getInstance();
			net.minecraft.client.gui.components.toasts.SystemToast.add(client.gui.toastManager(),
				MILESTONE_TOAST, net.minecraft.network.chat.Component.translatable("played.milestone.title", milestone),
				net.minecraft.network.chat.Component.literal(java.util.Objects.requireNonNull(name)));
		}
	}

	private static final net.minecraft.client.gui.components.toasts.SystemToast.@org.jspecify.annotations.NonNull SystemToastId MILESTONE_TOAST =
		new net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId();

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

	private static @org.jspecify.annotations.NonNull String normalizeServerAddress(String serverAddress) {
		return java.util.Objects.requireNonNull(serverAddress.trim().toLowerCase(Locale.ROOT));
	}
}
