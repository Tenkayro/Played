package de.tenkayro.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayedConfigTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void rejectsInvalidBooleansAndClampsNegativeAfkTimeout() throws Exception {
		PlayedConfig.initialize(this.temporaryDirectory);
		PlayedConfig.reset();

		Path configFile = this.temporaryDirectory.resolve("config").resolve("Played").resolve("config.properties");
		Properties properties = new Properties();
		properties.setProperty("track_worlds", "not-a-boolean");
		properties.setProperty("afk_timeout_seconds", "-50");
		properties.setProperty("hud_position", "sideways");
		properties.setProperty("hud_scale_percent", "500");
		properties.setProperty("hud_horizontal_offset", "-10");
		properties.setProperty("hud_vertical_offset", "2000");
		try (OutputStream output = Files.newOutputStream(configFile)) {
			properties.store(output, "test");
		}

		PlayedConfig.load();
		assertTrue(PlayedConfig.shouldTrackWorlds());
		assertEquals(0, PlayedConfig.getAfkTimeoutSeconds());
		assertEquals(PlayedConfig.HudPosition.TOP_LEFT, PlayedConfig.getHudPosition());
		assertEquals(200, PlayedConfig.getHudScalePercent());
		assertEquals(0, PlayedConfig.getHudHorizontalOffset());
		assertEquals(1000, PlayedConfig.getHudVerticalOffset());
	}

	@Test
	void expandsAllPlaytimePlaceholdersWithoutDuplicatingThePrimaryTime() {
		assertEquals(
			"Time 1m | Session 2m | Current 3m | Total 4m",
			PlayedConfig.formatTemplate(
				"Time {time} | Session {session} | Current {current} | Total {total}",
				"1m",
				"2m",
				"3m",
				"4m"
			)
		);
		assertEquals("Total 4m", PlayedConfig.formatTemplate("Total {total}", "1m", "2m", "3m", "4m"));
		assertEquals("Playtime: 1m", PlayedConfig.formatTemplate("Playtime: ", "1m", "2m", "3m", "4m"));
	}
}
