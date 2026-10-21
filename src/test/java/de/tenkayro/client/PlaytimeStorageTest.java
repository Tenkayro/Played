package de.tenkayro.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlaytimeStorageTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void persistsWorldServerAndAllTimeData() {
		Path world = this.temporaryDirectory.resolve("saves").resolve("World One");
		PlaytimeStorage.initialize(this.temporaryDirectory);

		PlaytimeStorage.addPlaytime(world, 1_250L);
		PlaytimeStorage.addServerPlaytime("Täst Server", " EXAMPLE.COM:25565 ", 2_750L);
		PlaytimeStorage.save(world);

		PlaytimeStorage.initialize(this.temporaryDirectory);
		assertEquals(1_250L, PlaytimeStorage.getWorldMillis(world));
		assertEquals(2_750L, PlaytimeStorage.getServerMillis("Renamed Server", "example.com:25565"));
		assertEquals(4_000L, PlaytimeStorage.getAllTimeMillis());
	}

	@Test
	void completesInterruptedLegacyMigrationButKeepsAnIntentionalEmptyServerFile() throws Exception {
		Path playedDirectory = Files.createDirectories(this.temporaryDirectory.resolve("Played"));
		Properties legacy = new Properties();
		legacy.setProperty("all_time_millis", "777");
		String address = "legacy.example:25565";
		String prefix = "server." + serverId(address);
		legacy.setProperty(prefix + ".address", address);
		legacy.setProperty(prefix + ".name", "Legacy");
		legacy.setProperty(prefix + ".millis", "333");
		store(playedDirectory.resolve("playtime.properties"), legacy);

		Properties newAllTime = new Properties();
		newAllTime.setProperty("all_time_millis", "888");
		store(playedDirectory.resolve("alltime.properties"), newAllTime);

		PlaytimeStorage.initialize(this.temporaryDirectory);
		assertEquals(888L, PlaytimeStorage.getAllTimeMillis());
		assertEquals(333L, PlaytimeStorage.getServerMillis("Legacy", address));

		PlaytimeStorage.deleteServerPlaytime();
		PlaytimeStorage.initialize(this.temporaryDirectory);
		assertEquals(0L, PlaytimeStorage.getServerMillis("Legacy", address));
	}

	@Test
	void saturatesInsteadOfOverflowing() {
		Path world = this.temporaryDirectory.resolve("saves").resolve("Overflow");
		PlaytimeStorage.initialize(this.temporaryDirectory);
		PlaytimeStorage.addPlaytime(world, Long.MAX_VALUE);
		PlaytimeStorage.addPlaytime(world, 1L);
		assertEquals(Long.MAX_VALUE, PlaytimeStorage.getWorldMillis(world));
		assertEquals(Long.MAX_VALUE, PlaytimeStorage.getAllTimeMillis());
	}

	private static void store(Path file, Properties properties) throws IOException {
		try (OutputStream output = Files.newOutputStream(file)) {
			properties.store(output, "test");
		}
	}

	private static String serverId(String address) throws Exception {
		byte[] hash = MessageDigest.getInstance("SHA-256")
			.digest(address.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(hash, 0, 8);
	}
}
