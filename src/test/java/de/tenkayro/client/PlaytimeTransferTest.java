package de.tenkayro.client;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlaytimeTransferTest {
    @TempDir Path directory;

    private Path backup(String worldKey) throws Exception {
        var history = new PlaytimeHistory.Data();
        history.days.put("2026-11-04", 1_000L);
        var value = new PlaytimeTransfer.Backup(1, 12_000,
            List.of(new PlaytimeStorage.Entry("world", worldKey, "Test", 5_000),
                new PlaytimeStorage.Entry("server", "EXAMPLE.COM", "Test server", 7_000)), history);
        Path file = directory.resolve("backup.json");
        Files.writeString(file, new Gson().toJson(value));
        return file;
    }

    @Test void mergesExistingWorldsAndServersIdempotentlyWithoutReplacingHigherCounters() throws Exception {
        Path world = Files.createDirectories(directory.resolve("saves/Test"));
        PlaytimeStorage.initialize(directory);
        PlaytimeHistory.initialize(directory);
        PlaytimeStorage.addPlaytime(world, 6_000);
        PlaytimeStorage.save(world);
        var value = PlaytimeTransfer.read(backup("Test"));
        PlaytimeTransfer.merge(value);
        PlaytimeTransfer.merge(value);
        assertEquals(6_000, PlaytimeStorage.getWorldMillis(world));
        assertEquals(7_000, PlaytimeStorage.getServerMillis("Test server", "example.com"));
        assertEquals(12_000, PlaytimeStorage.getAllTimeMillis());
        assertEquals(1_000, PlaytimeHistory.day(java.time.LocalDate.of(2026, 11, 4)));
        PlaytimeStorage.initialize(directory);
        assertEquals(6_000, PlaytimeStorage.getWorldMillis(world));
        assertEquals(7_000, PlaytimeStorage.getServerMillis("Test server", "example.com"));
    }

    @Test void rejectsTraversalAndInvalidDataBeforeAnyCounterChanges() throws Exception {
        PlaytimeStorage.initialize(directory);
        for (String name : List.of("../outside", "..\\outside", "C:\\outside", "..", ".", ".. ", "...")) {
            assertThrows(IOException.class, () -> PlaytimeTransfer.read(backup(name)));
        }
        assertEquals(0, PlaytimeStorage.getAllTimeMillis());
        Path file = backup("Test");
        Files.writeString(file, Files.readString(file).replace("12000", "-1"));
        assertThrows(IOException.class, () -> PlaytimeTransfer.read(file));
    }

    @Test void neverCreatesWorldFoldersFromABackup() throws Exception {
        PlaytimeStorage.initialize(directory);
        PlaytimeHistory.initialize(directory);
        PlaytimeTransfer.merge(PlaytimeTransfer.read(backup("Missing world")));
        assertFalse(Files.exists(directory.resolve("saves/Missing world")));
        assertEquals(12_000, PlaytimeStorage.getAllTimeMillis());
    }

    @Test void exportsPortableJsonAndEscapedCsvThatRoundTripWithoutDoubleCounting() throws Exception {
        Path world = Files.createDirectories(directory.resolve("saves/Test"));
        PlaytimeStorage.initialize(directory);
        PlaytimeHistory.initialize(directory);
        PlaytimeStorage.addPlaytime(world, 2_000);
        PlaytimeStorage.save(world);
        PlaytimeStorage.addServerPlaytime("=unsafe,\"name\"", "example.com", 3_000);
        PlaytimeHistory.begin("world:Test", "Test", 1_800_000_000_000L);
        PlaytimeHistory.record(2_000, 1_800_000_002_000L, java.time.ZoneId.of("UTC"));
        Path exported = PlaytimeTransfer.export(directory);
        var value = PlaytimeTransfer.read(exported);
        assertEquals(5_000, value.total());
        assertEquals(2, value.entries().size());
        assertEquals(1, value.history().sessions.size());
        assertFalse(Files.readString(exported).contains(directory.toString()));
        String stem = exported.getFileName().toString().replace(".json", "");
        String csv = Files.readString(exported.resolveSibling(stem + ".csv"));
        assertTrue(csv.contains("\"'=unsafe,\"\"name\"\"\""));
        assertTrue(Files.isRegularFile(exported.resolveSibling(stem + "-days.csv")));
        assertTrue(Files.isRegularFile(exported.resolveSibling(stem + "-sessions.csv")));
        PlaytimeTransfer.merge(value);
        assertEquals(5_000, PlaytimeStorage.getAllTimeMillis());
        assertEquals(1, PlaytimeHistory.snapshot().sessions.size());
    }

    @Test void validatesProgrammaticImportsBeforeMutatingStorage() {
        PlaytimeStorage.initialize(directory);
        PlaytimeHistory.initialize(directory);
        var invalid = new PlaytimeTransfer.Backup(1, 50_000,
            List.of(new PlaytimeStorage.Entry("world", "../outside", "Bad", 1_000)), new PlaytimeHistory.Data());
        assertThrows(IllegalArgumentException.class, () -> PlaytimeTransfer.merge(invalid));
        assertEquals(0, PlaytimeStorage.getAllTimeMillis());
    }

    @Test void milestonesOnlyTriggerOnNewThresholdCrossings() {
        long hour = 3_600_000L;
        assertEquals(10, Milestones.crossed(10 * hour - 1, 10 * hour));
        assertEquals(0, Milestones.crossed(10 * hour, 10 * hour + 1));
        assertEquals(100, Milestones.crossed(99 * hour, 101 * hour));
        assertEquals(500, Milestones.crossed(499 * hour, 501 * hour));
        assertEquals(2000, Milestones.crossed(1999 * hour, 2000 * hour));
        assertEquals(0, Milestones.crossed(2000 * hour, 2001 * hour));
    }
}
