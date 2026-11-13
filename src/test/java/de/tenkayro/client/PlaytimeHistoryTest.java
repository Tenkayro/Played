package de.tenkayro.client;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlaytimeHistoryTest {
    @TempDir Path directory;
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @Test void splitsActiveTimeAtLocalMidnightAndKeepsMondayWeekBoundaries() {
        PlaytimeHistory.initialize(directory);
        long end = ZonedDateTime.of(2026, 11, 2, 0, 0, 2, 0, BERLIN).toInstant().toEpochMilli();
        PlaytimeHistory.begin("world:Test", "Test", end - 5_000);
        PlaytimeHistory.record(5_000, end, BERLIN);
        assertEquals(3_000, PlaytimeHistory.day(LocalDate.of(2026, 11, 1)));
        assertEquals(2_000, PlaytimeHistory.day(LocalDate.of(2026, 11, 2)));
        assertEquals(2_000, PlaytimeHistory.week(LocalDate.of(2026, 11, 4)));
        assertEquals(3_000, PlaytimeHistory.week(LocalDate.of(2026, 11, 1)));
        assertEquals(5_000, PlaytimeHistory.snapshot().longestSession);
    }

    @Test void handlesTheTwentyFiveHourDayWhenDaylightSavingEnds() {
        PlaytimeHistory.initialize(directory);
        long start = LocalDate.of(2026, 10, 25).atStartOfDay(BERLIN).toInstant().toEpochMilli();
        long end = LocalDate.of(2026, 10, 26).atStartOfDay(BERLIN).toInstant().toEpochMilli();
        PlaytimeHistory.begin("server:test", "Test", start);
        PlaytimeHistory.record(end - start, end, BERLIN);
        assertEquals(25 * 3_600_000L, PlaytimeHistory.day(LocalDate.of(2026, 10, 25)));
        assertEquals(0, PlaytimeHistory.day(LocalDate.of(2026, 10, 26)));
    }

    @Test void checkpointsAnActiveSessionAndDoesNotChargePauseGaps() {
        PlaytimeHistory.initialize(directory);
        long start = 1_800_000_000_000L;
        PlaytimeHistory.begin("world:Test", "Test", start);
        PlaytimeHistory.record(1_000, start + 1_000, BERLIN);
        PlaytimeHistory.record(2_000, start + 60_000, BERLIN);
        PlaytimeHistory.save();
        PlaytimeHistory.initialize(directory);
        var sessions = PlaytimeHistory.snapshot().sessions;
        assertEquals(1, sessions.size());
        assertEquals(3_000, sessions.getFirst().millis());
        assertEquals(start + 60_000, sessions.getFirst().end());
    }

    @Test void mergesRepeatedSnapshotsWithoutDuplicatingTheActiveSession() {
        PlaytimeHistory.initialize(directory);
        PlaytimeHistory.begin("world:Test", "Test", 1_800_000_000_000L);
        PlaytimeHistory.record(1_000, 1_800_000_001_000L, BERLIN);
        var backup = PlaytimeHistory.snapshot();
        PlaytimeHistory.merge(backup);
        PlaytimeHistory.merge(backup);
        assertEquals(1, PlaytimeHistory.snapshot().sessions.size());
        PlaytimeHistory.finish();
        PlaytimeHistory.merge(backup);
        assertEquals(1, PlaytimeHistory.snapshot().sessions.size());
        assertEquals(1_000, PlaytimeHistory.snapshot().days.values().stream().mapToLong(v -> java.util.Objects.requireNonNull(v).longValue()).sum());
    }

    @Test void boundsTheSessionListButPreservesTheLongestSession() {
        PlaytimeHistory.initialize(directory);
        for (int i = 0; i < 505; i++) {
            PlaytimeHistory.begin("world:Test", "Test", 1_800_000_000_000L + i * 100_000);
            PlaytimeHistory.record(i == 0 ? 50_000 : 1_000, 1_800_000_050_000L + i * 100_000, BERLIN);
            PlaytimeHistory.finish();
        }
        assertEquals(500, PlaytimeHistory.snapshot().sessions.size());
        assertEquals(50_000, PlaytimeHistory.snapshot().longestSession);
    }

    @Test void recoversAnUnreadableHistoryFromThePreviousAtomicBackup() throws Exception {
        PlaytimeHistory.initialize(directory);
        PlaytimeHistory.begin("world:Test", "Test", 1_800_000_000_000L);
        PlaytimeHistory.record(1_000, 1_800_000_001_000L, BERLIN);
        PlaytimeHistory.save();
        PlaytimeHistory.record(1_000, 1_800_000_002_000L, BERLIN);
        PlaytimeHistory.save();
        Files.writeString(directory.resolve("Played/history.json"), "broken");
        PlaytimeHistory.initialize(directory);
        assertEquals(1_000, PlaytimeHistory.snapshot().longestSession);
    }

    @Test void computesBestDayRecord() {
        PlaytimeHistory.initialize(directory);
        long day1 = LocalDate.of(2026, 11, 10).atStartOfDay(BERLIN).toInstant().toEpochMilli() + 3_600_000L;
        long day2 = LocalDate.of(2026, 11, 11).atStartOfDay(BERLIN).toInstant().toEpochMilli() + 3_600_000L;
        PlaytimeHistory.begin("world:Test", "Test", day1);
        PlaytimeHistory.record(10_000L, day1 + 10_000L, BERLIN);
        PlaytimeHistory.begin("world:Test", "Test", day2);
        PlaytimeHistory.record(50_000L, day2 + 50_000L, BERLIN);
        var best = PlaytimeHistory.bestDay();
        assertNotNull(best);
        assertEquals("2026-11-11", best.getKey());
        assertEquals(50_000L, best.getValue());
    }
}
