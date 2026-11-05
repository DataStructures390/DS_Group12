package com.group12.tvsearch.sources;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.Series;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TmdbVerifyCommandTest {
    @Test void confirmsOneResolutionWithoutPrintingTitlesOrCredentials() {
        var bytes = new ByteArrayOutputStream();
        int[] calls = {0};
        int code = TmdbVerifyCommand.verify(query -> {
            calls[0]++;
            assertEquals("Example", query);
            return Optional.of(new Series(123, "Title kept out of diagnostics", "US", "tmdb:tv:123"));
        }, "Example", new PrintStream(bytes));
        assertEquals(0, code);
        assertEquals(1, calls[0]);
        assertEquals("TMDB_VERIFY: SUCCESS\nTV_ID: 123\n",
                bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }

    @Test void emptyResultDoesNotClaimAuthenticationFailedOrSearchSucceeded() {
        var bytes = new ByteArrayOutputStream();
        assertEquals(3, TmdbVerifyCommand.verify(q -> Optional.empty(), "Example", new PrintStream(bytes)));
        assertEquals("TMDB_VERIFY: NO_CONFIRMED_SERIES",
                bytes.toString(StandardCharsets.UTF_8).trim());
    }

    @Test void reportsEverySafeFailureWithoutRawTransportDetails() {
        for (var reason : TmdbAccessException.Reason.values()) {
            var bytes = new ByteArrayOutputStream();
            assertEquals(1, TmdbVerifyCommand.verify(q -> {
                throw new TmdbAccessException(reason);
            }, "Example", new PrintStream(bytes)));
            assertEquals("TMDB_VERIFY: " + reason.name(),
                    bytes.toString(StandardCharsets.UTF_8).trim());
        }
    }
}
