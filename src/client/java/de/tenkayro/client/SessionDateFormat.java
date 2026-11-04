package de.tenkayro.client;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Invalid resource-pack date patterns must not prevent the session screen from opening. */
public final class SessionDateFormat {
    private SessionDateFormat() {}

    public static DateTimeFormatter create(String pattern) {
        try {
            return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
        } catch (IllegalArgumentException ex) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault());
        }
    }
}
