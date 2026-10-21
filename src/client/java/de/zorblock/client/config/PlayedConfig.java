package de.zorblock.client.config;

import de.zorblock.Played;
import de.zorblock.client.AtomicFileWriter;
import java.io.ByteArrayOutputStream;
import de.zorblock.client.PlaytimeStorage;
import de.zorblock.client.PlaytimeTracker;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.jspecify.annotations.NonNull;

public final class PlayedConfig {
	private static final String MOD_FOLDER = "Played";
	private static final String FILE_NAME = "config.properties";

	private static final String KEY_TRACK_WORLDS = "track_worlds";
	private static final String KEY_TRACK_SERVERS = "track_servers";
	private static final String KEY_SHOW_WORLD_PLAYTIME = "show_world_playtime";
	private static final String KEY_SHOW_SERVER_PLAYTIME = "show_server_playtime";
	private static final String KEY_SHOW_TOTAL_PLAYTIME = "show_total_playtime";
	private static final String KEY_SHOW_INGAME_PLAYTIME = "show_ingame_playtime";
	private static final String KEY_SHOW_HUD_PLAYTIME = "show_hud_playtime";
	private static final String KEY_SHOW_HUD_BACKGROUND = "show_hud_background";
	private static final String KEY_HUD_POSITION = "hud_position";
	private static final String KEY_HUD_TEMPLATE = "hud_template";
	private static final String KEY_HUD_SCALE_PERCENT = "hud_scale_percent";
	private static final String KEY_HUD_HORIZONTAL_OFFSET = "hud_horizontal_offset";
	private static final String KEY_HUD_VERTICAL_OFFSET = "hud_vertical_offset";
	private static final String KEY_SHOW_LIST_BACKGROUND = "show_list_background";
	private static final String KEY_SHOW_EDIT_WORLD_RESET_BUTTON = "show_edit_world_reset_button";
	private static final String KEY_SHOW_EDIT_SERVER_RESET_BUTTON = "show_edit_server_reset_button";
	private static final String KEY_SORT_WORLD_LIST = "sort_world_list";
	private static final String KEY_SORT_SERVER_LIST = "sort_server_list";
	private static final String KEY_TIME_FORMAT = "time_format";
	private static final String KEY_LIST_PREFIX = "list_prefix";
	private static final String KEY_MAIN_MENU_PREFIX = "main_menu_prefix";
	private static final String KEY_PAUSE_MENU_PREFIX = "pause_menu_prefix";
	private static final String KEY_PAUSE_ON_AFK = "pause_on_afk";
	private static final String KEY_AFK_TIMEOUT_SECONDS = "afk_timeout_seconds";

	private static Path configFile;
	private static boolean trackWorlds = true;
	private static boolean trackServers = true;
	private static boolean showWorldPlaytime = true;
	private static boolean showServerPlaytime = true;
	private static boolean showTotalPlaytime = true;
	private static boolean showIngamePlaytime = false;
	private static boolean showHudPlaytime = false;
	private static boolean showHudBackground = false;
	private static HudPosition hudPosition = HudPosition.TOP_LEFT;
	private static String hudTemplate = "<gradient:#a855f7:#ec4899>\u23F1 {current}</gradient>";
	private static int hudScalePercent = 100;
	private static int hudHorizontalOffset = 5;
	private static int hudVerticalOffset = 5;
	private static boolean showListBackground = false;
	private static boolean showEditWorldResetButton = true;
	private static boolean showEditServerResetButton = true;
	private static SortMode sortWorldList = SortMode.DISABLED;
	private static SortMode sortServerList = SortMode.DISABLED;
	private static TimeFormatMode timeFormat = TimeFormatMode.VERBOSE;
	private static String listPrefix = "<gradient:#a855f7:#ec4899>\u23F1 {time}</gradient>";
	private static String mainMenuPrefix = "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>";
	private static String pauseMenuPrefix = "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>";
	private static boolean pauseOnAfk = false;
	private static int afkTimeoutSeconds = 900;

	private PlayedConfig() {
	}

	public static synchronized void initialize(Path gameDirectory) {
		Path configDirectory = gameDirectory.resolve("config").resolve(MOD_FOLDER);
		Path oldConfigDirectory = gameDirectory.resolve("config").resolve("jonasgrimm.de").resolve(MOD_FOLDER);

		if (Files.isDirectory(oldConfigDirectory) && !Files.exists(configDirectory)) {
			try {
				Files.createDirectories(configDirectory.getParent());
				Files.move(oldConfigDirectory, configDirectory);
				try {
					Files.deleteIfExists(oldConfigDirectory.getParent());
				} catch (IOException ignored) {
				}
				de.zorblock.Played.LOGGER.info("Migrated config from {} to {}", oldConfigDirectory, configDirectory);
			} catch (IOException exception) {
				de.zorblock.Played.LOGGER.warn("Failed to migrate config", exception);
			}
		}

		configFile = configDirectory.resolve(FILE_NAME);
		load();
		save();
		Played.LOGGER.info("Config initialized at {}", configFile);
	}

	public static synchronized void load() {
		if (configFile == null || !Files.isRegularFile(configFile)) {
			Played.LOGGER.info("No config file found yet.");
			return;
		}

		Properties properties = readProperties(configFile);
		if (properties == null) {
			Path backupFile = AtomicFileWriter.backupFile(configFile);
			properties = readProperties(backupFile);
			if (properties == null) {
				return;
			}
			Played.LOGGER.warn("Recovered config from backup {}", backupFile);
		}

		try {
			trackWorlds = readBoolean(properties, KEY_TRACK_WORLDS, trackWorlds);
			trackServers = readBoolean(properties, KEY_TRACK_SERVERS, trackServers);
			showWorldPlaytime = readBoolean(properties, KEY_SHOW_WORLD_PLAYTIME, showWorldPlaytime);
			showServerPlaytime = readBoolean(properties, KEY_SHOW_SERVER_PLAYTIME, showServerPlaytime);
			showTotalPlaytime = readBoolean(properties, KEY_SHOW_TOTAL_PLAYTIME, showTotalPlaytime);
			showIngamePlaytime = readBoolean(properties, KEY_SHOW_INGAME_PLAYTIME, showIngamePlaytime);
			showHudPlaytime = readBoolean(properties, KEY_SHOW_HUD_PLAYTIME, showHudPlaytime);
			showHudBackground = readBoolean(properties, KEY_SHOW_HUD_BACKGROUND, showHudBackground);
			hudPosition = readHudPosition(properties, KEY_HUD_POSITION, hudPosition);
			hudTemplate = properties.getProperty(KEY_HUD_TEMPLATE, hudTemplate);
			hudScalePercent = clamp(readInt(properties, KEY_HUD_SCALE_PERCENT, hudScalePercent), 50, 200);
			hudHorizontalOffset = clamp(readInt(properties, KEY_HUD_HORIZONTAL_OFFSET, hudHorizontalOffset), 0, 1000);
			hudVerticalOffset = clamp(readInt(properties, KEY_HUD_VERTICAL_OFFSET, hudVerticalOffset), 0, 1000);
			showListBackground = readBoolean(properties, KEY_SHOW_LIST_BACKGROUND, showListBackground);
			showEditWorldResetButton = readBoolean(properties, KEY_SHOW_EDIT_WORLD_RESET_BUTTON, showEditWorldResetButton);
			showEditServerResetButton = readBoolean(properties, KEY_SHOW_EDIT_SERVER_RESET_BUTTON, showEditServerResetButton);
			sortWorldList = readSortMode(properties, KEY_SORT_WORLD_LIST, sortWorldList);
			sortServerList = readSortMode(properties, KEY_SORT_SERVER_LIST, sortServerList);
			timeFormat = readTimeFormatMode(properties, KEY_TIME_FORMAT, timeFormat);
			listPrefix = properties.getProperty(KEY_LIST_PREFIX, listPrefix);
			mainMenuPrefix = properties.getProperty(KEY_MAIN_MENU_PREFIX, mainMenuPrefix);
			pauseMenuPrefix = properties.getProperty(KEY_PAUSE_MENU_PREFIX, pauseMenuPrefix);
			pauseOnAfk = readBoolean(properties, KEY_PAUSE_ON_AFK, pauseOnAfk);
			afkTimeoutSeconds = Math.max(0, readInt(properties, KEY_AFK_TIMEOUT_SECONDS, afkTimeoutSeconds));
			
			// Migrate old default configs to new gradient format
			if ("\u23F1 {time}".equals(listPrefix)) listPrefix = "<gradient:#a855f7:#ec4899>\u23F1 {time}</gradient>";
			if ("Playtime: {time}".equals(mainMenuPrefix)) mainMenuPrefix = "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>";
			if ("Playtime: {time}".equals(pauseMenuPrefix)) pauseMenuPrefix = "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>";
			
			Played.LOGGER.info("Config loaded from {}", configFile);
		} catch (RuntimeException exception) {
			Played.LOGGER.warn("Could not apply config file {}", configFile, exception);
		}
	}

	private static Properties readProperties(Path file) {
		Properties properties = new Properties();
		try (InputStream inputStream = Files.newInputStream(file)) {
			properties.load(inputStream);
			return properties;
		} catch (IOException | IllegalArgumentException exception) {
			Played.LOGGER.warn("Could not read config file {}", file, exception);
			return null;
		}
	}

	public static synchronized void save() {
		if (configFile == null) {
			return;
		}

		Properties properties = new Properties();
		properties.setProperty(KEY_TRACK_WORLDS, Boolean.toString(trackWorlds));
		properties.setProperty(KEY_TRACK_SERVERS, Boolean.toString(trackServers));
		properties.setProperty(KEY_SHOW_WORLD_PLAYTIME, Boolean.toString(showWorldPlaytime));
		properties.setProperty(KEY_SHOW_SERVER_PLAYTIME, Boolean.toString(showServerPlaytime));
		properties.setProperty(KEY_SHOW_TOTAL_PLAYTIME, Boolean.toString(showTotalPlaytime));
		properties.setProperty(KEY_SHOW_INGAME_PLAYTIME, Boolean.toString(showIngamePlaytime));
		properties.setProperty(KEY_SHOW_HUD_PLAYTIME, Boolean.toString(showHudPlaytime));
		properties.setProperty(KEY_SHOW_HUD_BACKGROUND, Boolean.toString(showHudBackground));
		properties.setProperty(KEY_HUD_POSITION, hudPosition.name());
		properties.setProperty(KEY_HUD_TEMPLATE, hudTemplate);
		properties.setProperty(KEY_HUD_SCALE_PERCENT, Integer.toString(hudScalePercent));
		properties.setProperty(KEY_HUD_HORIZONTAL_OFFSET, Integer.toString(hudHorizontalOffset));
		properties.setProperty(KEY_HUD_VERTICAL_OFFSET, Integer.toString(hudVerticalOffset));
		properties.setProperty(KEY_SHOW_LIST_BACKGROUND, Boolean.toString(showListBackground));
		properties.setProperty(KEY_SHOW_EDIT_WORLD_RESET_BUTTON, Boolean.toString(showEditWorldResetButton));
		properties.setProperty(KEY_SHOW_EDIT_SERVER_RESET_BUTTON, Boolean.toString(showEditServerResetButton));
		properties.setProperty(KEY_SORT_WORLD_LIST, sortWorldList.name());
		properties.setProperty(KEY_SORT_SERVER_LIST, sortServerList.name());
		properties.setProperty(KEY_TIME_FORMAT, timeFormat.name());
		properties.setProperty(KEY_LIST_PREFIX, listPrefix);
		properties.setProperty(KEY_MAIN_MENU_PREFIX, mainMenuPrefix);
		properties.setProperty(KEY_PAUSE_MENU_PREFIX, pauseMenuPrefix);
		properties.setProperty(KEY_PAUSE_ON_AFK, Boolean.toString(pauseOnAfk));
		properties.setProperty(KEY_AFK_TIMEOUT_SECONDS, Integer.toString(afkTimeoutSeconds));

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			properties.store(outputStream, "Played Config");
			AtomicFileWriter.write(configFile, outputStream.toByteArray());
		} catch (IOException exception) {
			Played.LOGGER.warn("Could not write config file {}", configFile, exception);
		}
	}

	public static synchronized void reset() {
		trackWorlds = true;
		trackServers = true;
		showWorldPlaytime = true;
		showServerPlaytime = true;
		showTotalPlaytime = true;
		showIngamePlaytime = false;
		showHudPlaytime = false;
		showHudBackground = false;
		hudPosition = HudPosition.TOP_LEFT;
		hudTemplate = "<gradient:#a855f7:#ec4899>\u23F1 {current}</gradient>";
		hudScalePercent = 100;
		hudHorizontalOffset = 5;
		hudVerticalOffset = 5;
		showListBackground = false;
		showEditWorldResetButton = true;
		showEditServerResetButton = true;
		sortWorldList = SortMode.DISABLED;
		sortServerList = SortMode.DISABLED;
		timeFormat = TimeFormatMode.VERBOSE;
		listPrefix = "<gradient:#a855f7:#ec4899>\u23F1 {time}</gradient>";
		mainMenuPrefix = "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>";
		pauseMenuPrefix = "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>";
		pauseOnAfk = false;
		afkTimeoutSeconds = 900;
		save();
		Played.LOGGER.info("Config reset to defaults.");
	}

	public static boolean shouldTrackWorlds() {
		return trackWorlds;
	}

	public static void setTrackWorlds(boolean trackWorlds) {
		PlayedConfig.trackWorlds = trackWorlds;
		save();
	}

	public static boolean shouldTrackServers() {
		return trackServers;
	}

	public static void setTrackServers(boolean trackServers) {
		PlayedConfig.trackServers = trackServers;
		save();
	}

	public static boolean shouldShowWorldPlaytime() {
		return showWorldPlaytime;
	}

	public static void setShowWorldPlaytime(boolean showWorldPlaytime) {
		PlayedConfig.showWorldPlaytime = showWorldPlaytime;
		save();
	}

	public static boolean shouldShowServerPlaytime() {
		return showServerPlaytime;
	}

	public static void setShowServerPlaytime(boolean showServerPlaytime) {
		PlayedConfig.showServerPlaytime = showServerPlaytime;
		save();
	}

	public static boolean shouldShowTotalPlaytime() {
		return showTotalPlaytime;
	}

	public static void setShowTotalPlaytime(boolean showTotalPlaytime) {
		PlayedConfig.showTotalPlaytime = showTotalPlaytime;
		save();
	}

	public static boolean shouldShowIngamePlaytime() {
		return showIngamePlaytime;
	}

	public static void setShowIngamePlaytime(boolean showIngamePlaytime) {
		PlayedConfig.showIngamePlaytime = showIngamePlaytime;
		save();
	}

	public static boolean shouldShowHudPlaytime() {
		return showHudPlaytime;
	}

	public static void setShowHudPlaytime(boolean showHudPlaytime) {
		PlayedConfig.showHudPlaytime = showHudPlaytime;
		save();
	}

	public static boolean shouldShowHudBackground() {
		return showHudBackground;
	}

	public static void setShowHudBackground(boolean showHudBackground) {
		PlayedConfig.showHudBackground = showHudBackground;
		save();
	}

	public static HudPosition getHudPosition() {
		return hudPosition;
	}

	public static void setHudPosition(HudPosition hudPosition) {
		PlayedConfig.hudPosition = hudPosition == null ? HudPosition.TOP_LEFT : hudPosition;
		save();
	}

	public static String getHudTemplate() {
		return hudTemplate;
	}

	public static void setHudTemplate(String hudTemplate) {
		PlayedConfig.hudTemplate = hudTemplate == null ? "" : hudTemplate;
		save();
	}

	public static int getHudScalePercent() {
		return hudScalePercent;
	}

	public static void setHudScalePercent(int hudScalePercent) {
		PlayedConfig.hudScalePercent = clamp(hudScalePercent, 50, 200);
		save();
	}

	public static int getHudHorizontalOffset() {
		return hudHorizontalOffset;
	}

	public static void setHudHorizontalOffset(int hudHorizontalOffset) {
		PlayedConfig.hudHorizontalOffset = clamp(hudHorizontalOffset, 0, 1000);
		save();
	}

	public static int getHudVerticalOffset() {
		return hudVerticalOffset;
	}

	public static void setHudVerticalOffset(int hudVerticalOffset) {
		PlayedConfig.hudVerticalOffset = clamp(hudVerticalOffset, 0, 1000);
		save();
	}

	public static boolean shouldShowListBackground() {
		return showListBackground;
	}

	public static void setShowListBackground(boolean showListBackground) {
		PlayedConfig.showListBackground = showListBackground;
		save();
	}

	public static boolean shouldShowEditWorldResetButton() {
		return showEditWorldResetButton;
	}

	public static void setShowEditWorldResetButton(boolean showEditWorldResetButton) {
		PlayedConfig.showEditWorldResetButton = showEditWorldResetButton;
		save();
	}

	public static boolean shouldShowEditServerResetButton() {
		return showEditServerResetButton;
	}

	public static void setShowEditServerResetButton(boolean showEditServerResetButton) {
		PlayedConfig.showEditServerResetButton = showEditServerResetButton;
		save();
	}

	public static SortMode getSortWorldList() {
		return sortWorldList;
	}

	public static void setSortWorldList(SortMode sortWorldList) {
		PlayedConfig.sortWorldList = sortWorldList == null ? SortMode.DISABLED : sortWorldList;
		save();
	}

	public static SortMode getSortServerList() {
		return sortServerList;
	}

	public static void setSortServerList(SortMode sortServerList) {
		PlayedConfig.sortServerList = sortServerList == null ? SortMode.DISABLED : sortServerList;
		save();
	}

	public static TimeFormatMode getTimeFormat() {
		return timeFormat;
	}

	public static void setTimeFormat(TimeFormatMode timeFormat) {
		PlayedConfig.timeFormat = timeFormat == null ? TimeFormatMode.VERBOSE : timeFormat;
		save();
	}

	public static String getListPrefix() {
		return listPrefix;
	}

	public static void setListPrefix(String listPrefix) {
		PlayedConfig.listPrefix = listPrefix == null ? "" : listPrefix;
		save();
	}

	public static String getMainMenuPrefix() {
		return mainMenuPrefix;
	}

	public static void setMainMenuPrefix(String mainMenuPrefix) {
		PlayedConfig.mainMenuPrefix = mainMenuPrefix == null ? "" : mainMenuPrefix;
		save();
	}

	public static String getPauseMenuPrefix() {
		return pauseMenuPrefix;
	}

	public static void setPauseMenuPrefix(String pauseMenuPrefix) {
		PlayedConfig.pauseMenuPrefix = pauseMenuPrefix == null ? "" : pauseMenuPrefix;
		save();
	}

	public static boolean shouldPauseOnAfk() {
		return pauseOnAfk;
	}

	public static void setPauseOnAfk(boolean pauseOnAfk) {
		PlayedConfig.pauseOnAfk = pauseOnAfk;
		save();
	}

	public static int getAfkTimeoutSeconds() {
		return afkTimeoutSeconds;
	}

	public static void setAfkTimeoutSeconds(int afkTimeoutSeconds) {
		PlayedConfig.afkTimeoutSeconds = Math.max(0, afkTimeoutSeconds);
		save();
	}

	public static net.minecraft.network.chat.Component formatListPlaytime(String formattedTime) {
		return MiniMessageUtil.parse(formatPlaytime(listPrefix, formattedTime));
	}

	public static String formatTime(long millis) {
		return PlaytimeStorage.format(millis, timeFormat.storageFormat());
	}

	public static net.minecraft.network.chat.Component formatMainMenuPlaytime(String formattedTime) {
		return MiniMessageUtil.parse(formatPlaytime(mainMenuPrefix, formattedTime));
	}

	public static net.minecraft.network.chat.Component formatPauseMenuPlaytime(String formattedTime) {
		return MiniMessageUtil.parse(formatPlaytime(pauseMenuPrefix, formattedTime));
	}

	public static net.minecraft.network.chat.Component formatHudPlaytime(long millis) {
		return MiniMessageUtil.parse(formatPlaytime(hudTemplate, formatTime(millis)));
	}

	private static boolean readBoolean(Properties properties, String key, boolean fallback) {
		String value = properties.getProperty(key);
		if (value == null) {
			return fallback;
		}

		String normalized = value.trim();
		if ("true".equalsIgnoreCase(normalized)) {
			return true;
		}
		if ("false".equalsIgnoreCase(normalized)) {
			return false;
		}
		return fallback;
	}

	private static int readInt(Properties properties, String key, int fallback) {
		String value = properties.getProperty(key);
		if (value == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static SortMode readSortMode(Properties properties, String key, SortMode fallback) {
		String value = properties.getProperty(key);
		if (value == null) {
			return fallback;
		}

		try {
			return SortMode.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	private static TimeFormatMode readTimeFormatMode(Properties properties, String key, TimeFormatMode fallback) {
		String value = properties.getProperty(key);
		if (value == null) {
			return fallback;
		}

		try {
			return TimeFormatMode.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	private static HudPosition readHudPosition(Properties properties, String key, HudPosition fallback) {
		String value = properties.getProperty(key);
		if (value == null) {
			return fallback;
		}

		try {
			return HudPosition.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	private static String formatPlaytime(String template, String formattedTime) {
		net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
		long currentMillis = client == null ? 0L : PlaytimeTracker.getActivePlaytimeMillis(client);
		return formatTemplate(
			template,
			formattedTime,
			formatTime(PlaytimeTracker.getSessionPlaytimeMillis()),
			formatTime(currentMillis),
			formatTime(PlaytimeStorage.getAllTimeMillis())
		);
	}

	static String formatTemplate(String template, String formattedTime, String session, String current, String total) {
		String safeTemplate = template == null ? "" : template;
		String safeTime = formattedTime == null ? "" : formattedTime;
		boolean containsPlaceholder = safeTemplate.contains("{time}")
			|| safeTemplate.contains("{session}")
			|| safeTemplate.contains("{current}")
			|| safeTemplate.contains("{total}");
		if (containsPlaceholder) {
			return safeTemplate
				.replace("{time}", safeTime)
				.replace("{session}", session == null ? "" : session)
				.replace("{current}", current == null ? "" : current)
				.replace("{total}", total == null ? "" : total);
		}

		return safeTemplate + safeTime;
	}

	public enum HudPosition {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT
	}

	public enum SortMode {
		DISABLED("Disabled"),
		UP_DOWN("Descending"),
		DOWN_UP("Ascending");

		private final @NonNull String labelKey;

		SortMode(@NonNull String labelKey) {
			this.labelKey = labelKey;
		}

		public @NonNull String label() {
			return this.labelKey;
		}

		public SortMode next() {
			SortMode[] values = values();
			return values[(this.ordinal() + 1) % values.length];
		}
	}

	public enum TimeFormatMode {
		VERBOSE("Verbose", PlaytimeStorage.TimeFormat.VERBOSE),
		COMPACT("Compact", PlaytimeStorage.TimeFormat.COMPACT),
		CLOCK("Clock", PlaytimeStorage.TimeFormat.CLOCK);

		private final @NonNull String labelKey;
		private final PlaytimeStorage.TimeFormat storageFormat;

		TimeFormatMode(@NonNull String labelKey, PlaytimeStorage.TimeFormat storageFormat) {
			this.labelKey = labelKey;
			this.storageFormat = storageFormat;
		}

		public @NonNull String label() {
			return this.labelKey;
		}

		public PlaytimeStorage.TimeFormat storageFormat() {
			return this.storageFormat;
		}

		public TimeFormatMode next() {
			TimeFormatMode[] values = values();
			return values[(this.ordinal() + 1) % values.length];
		}
	}
}
