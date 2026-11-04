package de.tenkayro.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Portable snapshots merge using maxima, making repeated imports idempotent. */
public final class PlaytimeTransfer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_BYTES = 16 * 1024 * 1024;
    public record Backup(int version, long total, List<PlaytimeStorage.@org.jspecify.annotations.NonNull Entry> entries, PlaytimeHistory.Data history) {}
    private PlaytimeTransfer() {}

    public static Path export(Path gameDirectory) throws IOException {
        PlaytimeTracker.flush();
        Path directory = gameDirectory.resolve("Played/exports");
        String name = "played-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss-SSS"));
        Path json = directory.resolve(name + ".json");
        Backup backup = new Backup(1, PlaytimeStorage.getAllTimeMillis(), PlaytimeStorage.entries(), PlaytimeHistory.snapshot());
        AtomicFileWriter.write(json, GSON.toJson(backup).getBytes(StandardCharsets.UTF_8));
        StringBuilder csv = new StringBuilder("kind,name,key,milliseconds\r\n");
        for (var entry : backup.entries()) csv.append(csv(entry.kind())).append(',').append(csv(entry.name()))
            .append(',').append(csv(entry.key())).append(',').append(entry.millis()).append("\r\n");
        AtomicFileWriter.write(directory.resolve(name + ".csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder days = new StringBuilder("date,milliseconds\r\n");
        backup.history().days.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
            .forEach(e -> days.append(e.getKey()).append(',').append(e.getValue()).append("\r\n"));
        AtomicFileWriter.write(directory.resolve(name + "-days.csv"), days.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder sessions = new StringBuilder("id,target,name,start_epoch_ms,end_epoch_ms,active_milliseconds\r\n");
        for (var s : backup.history().sessions) sessions.append(csv(s.id())).append(',').append(csv(s.target()))
            .append(',').append(csv(s.name())).append(',').append(s.start()).append(',').append(s.end())
            .append(',').append(s.millis()).append("\r\n");
        AtomicFileWriter.write(directory.resolve(name + "-sessions.csv"), sessions.toString().getBytes(StandardCharsets.UTF_8));
        return json;
    }

    public static Backup read(Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) throw new IOException("Backup missing or larger than 16 MiB");
        try {
            @org.jspecify.annotations.Nullable Backup value = GSON.fromJson(Files.readString(file), Backup.class);
            validate(value);
            return java.util.Objects.requireNonNull(value);
        } catch (RuntimeException ex) { throw new IOException("Invalid Played backup", ex); }
    }

    private static void validate(@org.jspecify.annotations.Nullable Backup value) {
        if (value == null || value.version() != 1 || value.total() < 0 || value.entries() == null || value.entries().size() > 100_000)
            throw new IllegalArgumentException("Unsupported backup");
        for (var entry : value.entries()) {
            // Gson can populate nulls despite the Java type contract; validate at the boundary.
            var checked = java.util.Objects.requireNonNull(entry, "Missing entry");
            String kind = java.util.Objects.requireNonNull(checked.kind(), "Missing entry kind");
            String entryKey = java.util.Objects.requireNonNull(checked.key(), "Missing entry key");
            String name = java.util.Objects.requireNonNull(checked.name(), "Missing entry name");
            if (entryKey.isBlank() || entryKey.length() > 512 || name.length() > 512 || checked.millis() < 0
                || !("world".equals(kind) || "server".equals(kind)))
                throw new IllegalArgumentException("Invalid entry");
            if ("world".equals(kind)) {
                if (entryKey.contains("/") || entryKey.contains("\\") || entryKey.contains(":")
                    || entryKey.endsWith(".") || entryKey.endsWith(" "))
                    throw new IllegalArgumentException("Invalid save-folder name");
                Path.of(entryKey);
            }
        }
        PlaytimeHistory.validate(value.history());
    }

    public static void merge(Backup backup) {
        // Revalidate before any mutation, even when called independently of the UI.
        validate(backup);
        PlaytimeStorage.mergeEntries(backup.entries(), backup.total());
        PlaytimeHistory.merge(backup.history());
        PlaytimeHistory.save();
    }

    private static String csv(String value) {
        // Prevent spreadsheet formula evaluation for user-controlled world/server names.
        String trimmed = value.stripLeading();
        String safe = !trimmed.isEmpty() && "=+@-".indexOf(trimmed.charAt(0)) >= 0 ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
