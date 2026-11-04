package de.tenkayro.client;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SessionDateFormatTest {
    @Test void invalidTranslationPatternsFallBackWithoutCrashing() {
        Instant time = Instant.parse("2026-11-04T12:00:00Z");
        for (String pattern : new String[] {"not a date pattern", "yyyy-MM-dd '", "yyyy-MM-dd SSSSSSSSSS"}) {
            assertFalse(SessionDateFormat.create(pattern).format(time).isBlank());
        }
    }

    @Test void validPatternsAndExtremeImportedTimestampsRemainRenderable() {
        assertEquals("2026", SessionDateFormat.create("yyyy").format(Instant.parse("2026-11-04T12:00:00Z")));
        assertDoesNotThrow(() -> SessionDateFormat.create("dd.MM.yy HH:mm").format(Instant.ofEpochMilli(Long.MAX_VALUE)));
    }
}
