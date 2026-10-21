package de.tenkayro.client;

import de.tenkayro.Played;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class PlaytimeStorage {
	private static final String MOD_FOLDER = "Played";
	private static final String WORLD_FILE_NAME = "playtime.properties";
	private static final String ALL_TIME_FILE_NAME = "alltime.properties";
	private static final String SERVER_FILE_NAME = "server.properties";
	private static final String LEGACY_CENTRAL_FILE_NAME = "playtime.properties";
	private static final String KEY_ALL_TIME = "all_time_millis";
	private static final String KEY_WORLD_TIME = "world_time_millis";
	private static final String SERVER_PREFIX = "server.";
	private static final String SERVER_MILLIS_SUFFIX = ".millis";
	private static final String SERVER_NAME_SUFFIX = ".name";
	private static final String SERVER_ADDRESS_SUFFIX = ".address";
	private static final String MAX_MILLIS_VALUE = Long.toString(Long.MAX_VALUE);

	private static final Map<Path, Long> WORLD_CACHE = new HashMap<>();
	private static final Map<String, ServerPlaytime> SERVER_CACHE = new HashMap<>();
	private static final Map<String, String> SERVER_ID_CACHE = new HashMap<>();
	private static Path gameDirectory;
	private static Path allTimeFile;
	private static Path serverFile;
	private static Path legacyCentralFile;
	private static long allTimeMillis;

	private PlaytimeStorage() {
	}

	public static synchronized void initialize(Path gameDirectory) {
		PlaytimeStorage.gameDirectory = normalize(gameDirectory);
		WORLD_CACHE.clear();
		SERVER_CACHE.clear();
		SERVER_ID_CACHE.clear();
		Path centralDirectory = PlaytimeStorage.gameDirectory.resolve(MOD_FOLDER);
		Path oldCentralDirectory = PlaytimeStorage.gameDirectory.resolve("jonasgrimm.de").resolve(MOD_FOLDER);

		if (Files.isDirectory(oldCentralDirectory) && !Files.exists(centralDirectory)) {
			try {
				Files.createDirectories(centralDirectory.getParent());
				Files.move(oldCentralDirectory, centralDirectory);
				try {
					Files.deleteIfExists(oldCentralDirectory.getParent());
				} catch (IOException ignored) {
				}
				de.tenkayro.Played.LOGGER.info("Migrated playtime storage from {} to {}", oldCentralDirectory, centralDirectory);
			} catch (IOException exception) {
				de.tenkayro.Played.LOGGER.warn("Failed to migrate playtime storage", exception);
			}
		}
		allTimeFile = centralDirectory.resolve(ALL_TIME_FILE_NAME);
		serverFile = centralDirectory.resolve(SERVER_FILE_NAME);
		legacyCentralFile = centralDirectory.resolve(LEGACY_CENTRAL_FILE_NAME);
		loadCentral();
		writeCentral();
		Played.LOGGER.info("Playtime storage initialized at {}", centralDirectory);
	}

	public static synchronized void addPlaytime(Path worldDirectory, long millis) {
		if (millis <= 0L) {
			return;
		}

		Path normalizedWorldDirectory = normalize(worldDirectory);
		long worldMillis = addMillis(getWorldMillis(normalizedWorldDirectory), millis);
		WORLD_CACHE.put(normalizedWorldDirectory, worldMillis);
		allTimeMillis = addMillis(allTimeMillis, millis);
	}

	public static synchronized void addServerPlaytime(String serverName, String serverAddress, long millis) {
		if (millis <= 0L || serverAddress == null || serverAddress.isBlank()) {
			return;
		}

		String normalizedAddress = normalizeServerAddress(serverAddress);
		String serverId = serverId(normalizedAddress);
		ServerPlaytime serverPlaytime = getServerPlaytime(serverName, normalizedAddress);
		SERVER_CACHE.put(serverId, new ServerPlaytime(displayServerName(serverName, normalizedAddress), normalizedAddress, addMillis(serverPlaytime.millis(), millis)));
		allTimeMillis = addMillis(allTimeMillis, millis);
	}

	public static synchronized long getAllTimeMillis() {
		return allTimeMillis;
	}

	public static synchronized long getWorldMillis(Path worldDirectory) {
		Path normalizedWorldDirectory = normalize(worldDirectory);
		Long cached = WORLD_CACHE.get(normalizedWorldDirectory);
		if (cached != null) {
			return cached;
		}

		long millis = readMillis(worldFile(normalizedWorldDirectory), KEY_WORLD_TIME);
		WORLD_CACHE.put(normalizedWorldDirectory, millis);
		return millis;
	}

	public static synchronized long getServerMillis(String serverName, String serverAddress) {
		return getServerPlaytime(serverName, serverAddress).millis();
	}

	public static synchronized void save(Path worldDirectory) {
		Path normalizedWorldDirectory = normalize(worldDirectory);
		writeWorld(normalizedWorldDirectory, getWorldMillis(normalizedWorldDirectory));
		writeCentral();
		Played.LOGGER.info("Saved playtime data for world {}", normalizedWorldDirectory);
	}

	public static synchronized void saveCentral() {
		writeCentral();
		Played.LOGGER.info("Saved central playtime data.");
	}

	public static synchronized void deleteWorldPlaytime() {
		WORLD_CACHE.clear();
		if (gameDirectory != null) {
			Path savesDirectory = gameDirectory.resolve("saves");
			if (Files.isDirectory(savesDirectory)) {
				try (var worlds = Files.list(savesDirectory)) {
					worlds.map(PlaytimeStorage::worldFile).forEach(PlaytimeStorage::deleteFile);
				} catch (IOException exception) {
					Played.LOGGER.warn("Could not delete world playtime files in {}", savesDirectory, exception);
				}
			}
		}
		Played.LOGGER.info("Deleted saved world playtime data.");
	}

	public static synchronized void resetWorldPlaytime(Path worldDirectory) {
		Path normalizedWorldDirectory = normalize(worldDirectory);
		WORLD_CACHE.put(normalizedWorldDirectory, 0L);
		writeWorld(normalizedWorldDirectory, 0L);
		Played.LOGGER.info("Reset playtime data for world {}.", normalizedWorldDirectory);
	}

	public static synchronized void deleteServerPlaytime() {
		SERVER_CACHE.clear();
		SERVER_ID_CACHE.clear();
		writeServerProperties();
		Played.LOGGER.info("Deleted saved server playtime data.");
	}

	public static synchronized void resetServerPlaytime(String serverName, String serverAddress) {
		if (serverAddress == null || serverAddress.isBlank()) {
			return;
		}

		String normalizedAddress = normalizeServerAddress(serverAddress);
		String serverId = serverId(normalizedAddress);
		SERVER_CACHE.put(serverId, new ServerPlaytime(displayServerName(serverName, normalizedAddress), normalizedAddress, 0L));
		writeServerProperties();
		Played.LOGGER.info("Reset playtime data for server {}.", serverAddress);
	}

	public static synchronized void deleteAllTimePlaytime() {
		allTimeMillis = 0L;
		Properties allTimeProperties = new Properties();
		allTimeProperties.setProperty(KEY_ALL_TIME, "0");
		writeProperties(allTimeFile, allTimeProperties);
		Played.LOGGER.info("Deleted saved all-time playtime data.");
	}

	public static String format(long millis) {
		return format(millis, TimeFormat.VERBOSE);
	}

	public static String format(long millis, TimeFormat timeFormat) {
		TimeFormat safeTimeFormat = timeFormat == null ? TimeFormat.VERBOSE : timeFormat;
		if (safeTimeFormat == TimeFormat.CLOCK) {
			return formatClock(millis);
		}

		if (safeTimeFormat == TimeFormat.COMPACT) {
			return formatCompact(millis);
		}

		long seconds = Math.max(0L, millis / 1000L);
		long minutes = seconds / 60L;
		long hours = minutes / 60L;
		long days = hours / 24L;

		seconds %= 60L;
		minutes %= 60L;
		hours %= 24L;

		if (days > 0L) {
			return days + "d " + hours + "h " + minutes + "m";
		}

		if (hours > 0L) {
			return hours + "h " + minutes + "m";
		}

		if (minutes > 0L) {
			return minutes + "m " + seconds + "s";
		}

		return seconds + "s";
	}

	private static String formatCompact(long millis) {
		long seconds = Math.max(0L, millis / 1000L);
		long minutes = seconds / 60L;
		long hours = minutes / 60L;
		long days = hours / 24L;

		seconds %= 60L;
		minutes %= 60L;
		hours %= 24L;

		if (days > 0L) {
			return days + "d" + hours + "h";
		}

		if (hours > 0L) {
			return hours + "h" + minutes + "m";
		}

		if (minutes > 0L) {
			return minutes + "m" + seconds + "s";
		}

		return seconds + "s";
	}

	private static String formatClock(long millis) {
		long seconds = Math.max(0L, millis / 1000L);
		long minutes = seconds / 60L;
		long hours = minutes / 60L;
		long days = hours / 24L;

		seconds %= 60L;
		minutes %= 60L;
		hours %= 24L;

		if (days > 0L) {
			return String.format(Locale.ROOT, "%dd %02d:%02d:%02d", days, hours, minutes, seconds);
		}

		if (hours > 0L) {
			return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
		}

		return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
	}

	private static Path worldFile(Path worldDirectory) {
		return worldDirectory.resolve(MOD_FOLDER).resolve(WORLD_FILE_NAME);
	}

	private static long readMillis(Path file, String key) {
		return parseMillis(readProperties(file).getProperty(key, "0"));
	}

	private static void loadCentral() {
		allTimeMillis = 0L;
		SERVER_CACHE.clear();

		boolean hasAllTimeFile = Files.isRegularFile(allTimeFile);
		boolean hasServerFile = Files.isRegularFile(serverFile);
		Properties allTimeProperties = readProperties(allTimeFile);
		Properties serverProperties = readProperties(serverFile);
		Properties legacyProperties = readProperties(legacyCentralFile);

		Properties effectiveAllTimeProperties = hasAllTimeFile ? allTimeProperties : legacyProperties;
		Properties effectiveServerProperties = hasServerFile ? serverProperties : legacyProperties;
		allTimeMillis = parseMillis(effectiveAllTimeProperties.getProperty(KEY_ALL_TIME, "0"));
		loadServers(effectiveServerProperties);
		if ((!legacyProperties.isEmpty()) && (!hasAllTimeFile || !hasServerFile)) {
			Played.LOGGER.info("Recovered missing central playtime data from legacy file {}", legacyCentralFile);
		}
		Played.LOGGER.info("Loaded central playtime data.");
	}

	private static ServerPlaytime getServerPlaytime(String serverName, String serverAddress) {
		String normalizedAddress = normalizeServerAddress(serverAddress);
		String serverId = serverId(normalizedAddress);
		ServerPlaytime cached = SERVER_CACHE.get(serverId);
		if (cached != null) {
			String displayName = displayServerName(serverName, normalizedAddress);
			if (!cached.name().equals(displayName) || !cached.address().equals(normalizedAddress)) {
				cached = new ServerPlaytime(displayName, normalizedAddress, cached.millis());
				SERVER_CACHE.put(serverId, cached);
			}
			return cached;
		}

		ServerPlaytime serverPlaytime = new ServerPlaytime(displayServerName(serverName, normalizedAddress), normalizedAddress, 0L);
		SERVER_CACHE.put(serverId, serverPlaytime);
		return serverPlaytime;
	}

	private static void writeWorld(Path worldDirectory, long worldMillis) {
		Properties properties = new Properties();
		properties.setProperty(KEY_WORLD_TIME, Long.toString(Math.max(0L, worldMillis)));
		writeProperties(worldFile(worldDirectory), properties);
	}

	private static void writeCentral() {
		if (allTimeFile == null || serverFile == null) {
			return;
		}

		Properties allTimeProperties = new Properties();
		allTimeProperties.setProperty(KEY_ALL_TIME, Long.toString(Math.max(0L, allTimeMillis)));
		writeProperties(allTimeFile, allTimeProperties);

		writeServerProperties();
	}

	private static void loadServers(Properties properties) {
		for (String key : properties.stringPropertyNames()) {
			if (!key.startsWith(SERVER_PREFIX) || !key.endsWith(SERVER_MILLIS_SUFFIX)) {
				continue;
			}

			String serverId = key.substring(SERVER_PREFIX.length(), key.length() - SERVER_MILLIS_SUFFIX.length());
			String prefix = SERVER_PREFIX + serverId;
			String address = properties.getProperty(prefix + SERVER_ADDRESS_SUFFIX, "");
			if (address.isBlank()) {
				continue;
			}

			String name = properties.getProperty(prefix + SERVER_NAME_SUFFIX, address);
			long millis = parseMillis(properties.getProperty(key, "0"));
			SERVER_CACHE.put(serverId, new ServerPlaytime(name, address, millis));
		}
	}

	private static Properties readProperties(Path file) {
		Properties properties = readPropertiesFile(file);
		if (properties != null) {
			return properties;
		}

		Path backupFile = file == null ? null : AtomicFileWriter.backupFile(file);
		properties = readPropertiesFile(backupFile);
		if (properties != null) {
			Played.LOGGER.warn("Recovered playtime data from backup {}", backupFile);
			return properties;
		}

		return new Properties();
	}

	private static Properties readPropertiesFile(Path file) {
		if (file == null || !Files.isRegularFile(file)) {
			return null;
		}

		Properties properties = new Properties();
		try (InputStream inputStream = Files.newInputStream(file)) {
			properties.load(inputStream);
			return properties;
		} catch (IOException | IllegalArgumentException exception) {
			Played.LOGGER.warn("Could not read playtime file {}", file, exception);
			return null;
		}
	}

	private static void writeProperties(Path file, Properties properties) {
		if (file == null) {
			return;
		}

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			properties.store(outputStream, "Played");
			AtomicFileWriter.write(file, outputStream.toByteArray());
		} catch (IOException exception) {
			Played.LOGGER.warn("Could not write playtime file {}", file, exception);
		}
	}

	private static void writeServerProperties() {
		if (serverFile == null) {
			return;
		}

		List<Map.Entry<String, ServerPlaytime>> servers = new ArrayList<>(SERVER_CACHE.entrySet());
		servers.sort(Comparator.comparing(entry -> entry.getValue().name().toLowerCase(Locale.ROOT)));

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.ISO_8859_1))) {
				writer.write("#Played");
				writer.newLine();
				writer.write("#" + new Date());
				writer.newLine();

				for (Map.Entry<String, ServerPlaytime> entry : servers) {
					ServerPlaytime serverPlaytime = entry.getValue();
					String prefix = SERVER_PREFIX + entry.getKey();
					writer.write("#-----");
					writer.newLine();
					writePropertyLine(writer, prefix + SERVER_ADDRESS_SUFFIX, serverPlaytime.address());
					writePropertyLine(writer, prefix + SERVER_MILLIS_SUFFIX, Long.toString(Math.max(0L, serverPlaytime.millis())));
					writePropertyLine(writer, prefix + SERVER_NAME_SUFFIX, serverPlaytime.name());
				}
			}
			AtomicFileWriter.write(serverFile, outputStream.toByteArray());
		} catch (IOException exception) {
			Played.LOGGER.warn("Could not write server playtime file {}", serverFile, exception);
		}
	}

	private static void writePropertyLine(BufferedWriter writer, String key, String value) throws IOException {
		writer.write(escapeProperty(key, true));
		writer.write("=");
		writer.write(escapeProperty(value, false));
		writer.newLine();
	}

	private static String escapeProperty(String value, boolean key) {
		StringBuilder builder = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
				case '\\' -> builder.append("\\\\");
				case '\t' -> builder.append("\\t");
				case '\n' -> builder.append("\\n");
				case '\r' -> builder.append("\\r");
				case '\f' -> builder.append("\\f");
				case ' ', ':', '=', '#', '!' -> {
					if (key || i == 0 || character != ' ') {
						builder.append('\\');
					}
					builder.append(character);
				}
				default -> {
					if (character < 0x20 || character > 0x7E) {
						builder.append(String.format("\\u%04X", (int) character));
					} else {
						builder.append(character);
					}
				}
			}
		}

		return builder.toString();
	}

	private static Path normalize(Path path) {
		return path.toAbsolutePath().normalize();
	}

	private static void deleteFile(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException exception) {
			Played.LOGGER.warn("Could not delete playtime file {}", file, exception);
		}
	}

	private static long parseMillis(String value) {
		if (value == null) {
			return 0L;
		}

		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return 0L;
		}

		if (trimmed.charAt(0) == '+') {
			trimmed = trimmed.substring(1);
		}

		if (trimmed.isEmpty() || trimmed.charAt(0) == '-') {
			return 0L;
		}

		for (int i = 0; i < trimmed.length(); i++) {
			char character = trimmed.charAt(i);
			if (character < '0' || character > '9') {
				return 0L;
			}
		}

		trimmed = stripLeadingZeroes(trimmed);
		if (trimmed.length() > MAX_MILLIS_VALUE.length() || (trimmed.length() == MAX_MILLIS_VALUE.length() && trimmed.compareTo(MAX_MILLIS_VALUE) > 0)) {
			return Long.MAX_VALUE;
		}

		return Long.parseLong(trimmed);
	}

	private static String stripLeadingZeroes(String value) {
		int index = 0;
		while (index < value.length() - 1 && value.charAt(index) == '0') {
			index++;
		}

		return value.substring(index);
	}

	private static long addMillis(long currentMillis, long addedMillis) {
		if (currentMillis < 0L) {
			return Math.max(0L, addedMillis);
		}

		if (Long.MAX_VALUE - currentMillis < addedMillis) {
			return Long.MAX_VALUE;
		}

		return currentMillis + addedMillis;
	}

	private static String displayServerName(String serverName, String serverAddress) {
		return serverName == null || serverName.isBlank() ? serverAddress : serverName;
	}

	private static String serverId(String serverAddress) {
		String normalizedAddress = normalizeServerAddress(serverAddress);
		String cached = SERVER_ID_CACHE.get(normalizedAddress);
		if (cached != null) {
			return cached;
		}

		String serverId;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(normalizedAddress.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < 8; i++) {
				builder.append(String.format("%02x", hash[i]));
			}
			serverId = builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			serverId = Integer.toHexString(normalizedAddress.hashCode());
		}

		SERVER_ID_CACHE.put(normalizedAddress, serverId);
		return serverId;
	}

	private static String normalizeServerAddress(String serverAddress) {
		return serverAddress.trim().toLowerCase(Locale.ROOT);
	}

	private record ServerPlaytime(String name, String address, long millis) {
	}

	public enum TimeFormat {
		VERBOSE,
		COMPACT,
		CLOCK
	}
}
