package de.tenkayro.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.tenkayro.Played;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Calendar history is separate from the legacy counters; old totals are never fabricated into days. */
public final class PlaytimeHistory {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int MAX_SESSIONS = 500;
    private static Path file;
    private static Data data = new Data();
    private static Session active;

    public static final class Data {
        public int version = 1;
        public Map<String, Long> days = new HashMap<>();
        public List<Session> sessions = new ArrayList<>();
        public long longestSession;
    }

    public record Session(String id, String target, String name, long start, long end, long millis) {}

    public static Session active() { return active; }

    public static boolean isActive(String id) {
        return active != null && active.id().equals(id);
    }

    private PlaytimeHistory() {}

    public static void initialize(Path gameDirectory) {
        file = gameDirectory.resolve("Played/history.json");
        data = new Data();
        active = null;
        for (Path candidate : List.of(file, AtomicFileWriter.backupFile(file))) {
            if (!Files.exists(candidate)) continue;
            try {
                data = validate(GSON.fromJson(Files.readString(candidate), Data.class));
                return;
            } catch (IOException | RuntimeException ex) {
                Played.LOGGER.warn("Could not read playtime history {}", candidate, ex);
            }
        }
    }

    public static void begin(String target, String name, long now) {
        finish();
        active = new Session(UUID.randomUUID().toString(), target, name, now, now, 0L);
    }

    public static void record(long millis, long end, ZoneId zone) {
        if (active == null || millis <= 0) return;
        active = new Session(active.id(), active.target(), active.name(), active.start(),
            Math.max(active.start(), end), add(active.millis(), millis));
        data.longestSession = Math.max(data.longestSession, active.millis());
        // Split an interval at local midnight, including daylight saving transitions.
        long cursor = end - millis;
        while (cursor < end) {
            LocalDate day = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate();
            long next = Math.min(end, day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());
            data.days.merge(day.toString(), next - cursor, (a, b) -> add(java.util.Objects.requireNonNull(a), java.util.Objects.requireNonNull(b)));
            cursor = next;
        }
    }

    public static void finish() {
        if (active != null && active.millis() > 0) data.sessions.add(active);
        active = null;
        trim();
    }

    private static void trim() {
        data.sessions.sort(Comparator.<Session>comparingLong(s -> java.util.Objects.requireNonNull(s).start()).reversed());
        if (data.sessions.size() > MAX_SESSIONS) data.sessions.subList(MAX_SESSIONS, data.sessions.size()).clear();
    }

    public static Data snapshot() {
        Data copy = new Data();
        copy.days.putAll(data.days);
        copy.sessions.addAll(data.sessions);
        if (active != null && active.millis() > 0) copy.sessions.add(active);
        copy.sessions.sort(Comparator.<Session>comparingLong(s -> java.util.Objects.requireNonNull(s).start()).reversed());
        if (copy.sessions.size() > MAX_SESSIONS) copy.sessions.subList(MAX_SESSIONS, copy.sessions.size()).clear();
        copy.longestSession = data.longestSession;
        return copy;
    }

    public static long day(LocalDate date) { return data.days.getOrDefault(date.toString(), 0L); }

    public static long longestSession() { return data.longestSession; }

    public static long week(LocalDate date) {
        LocalDate monday = date.minusDays(date.getDayOfWeek().getValue() - 1);
        long total = 0;
        for (int i = 0; i < 7; i++) total = add(total, day(monday.plusDays(i)));
        return total;
    }

    public static void merge(Data imported) {
        validate(imported);
        imported.days.forEach((date, millis) -> data.days.merge(date, millis, (a, b) -> Math.max(java.util.Objects.requireNonNull(a), java.util.Objects.requireNonNull(b))));
        Map<String, Session> sessions = new HashMap<>();
        for (Session s : data.sessions) sessions.put(s.id(), s);
        for (Session s : imported.sessions) {
            if (active != null && active.id().equals(s.id())) continue;
            sessions.merge(s.id(), s, (a, b) -> a.millis() >= b.millis() ? a : b);
        }
        data.sessions = new ArrayList<>(sessions.values());
        data.longestSession = Math.max(data.longestSession, imported.longestSession);
        trim();
    }

    public static Data validate(Data value) {
        if (value == null || value.version != 1 || value.days == null || value.sessions == null
            || value.days.size() > 100_000 || value.sessions.size() > MAX_SESSIONS + 1 || value.longestSession < 0)
            throw new IllegalArgumentException("Unsupported or invalid history");
        value.days.forEach((date, millis) -> {
            LocalDate.parse(date);
            if (millis == null || millis < 0) throw new IllegalArgumentException("Invalid daily time");
        });
        for (Session s : value.sessions) {
            if (s == null || s.id() == null || s.target() == null || s.name() == null || s.millis() < 0
                || s.start() < 0 || s.end() < s.start() || s.name().length() > 512 || s.target().length() > 1024)
                throw new IllegalArgumentException("Invalid session");
            UUID.fromString(s.id());
            value.longestSession = Math.max(value.longestSession, s.millis());
        }
        return value;
    }

    public static void save() {
        if (file == null) return;
        try {
            AtomicFileWriter.write(file, GSON.toJson(snapshot()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) { Played.LOGGER.warn("Could not save playtime history", ex); }
    }

    private static long add(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
}
