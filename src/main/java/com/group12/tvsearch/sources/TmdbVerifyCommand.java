package com.group12.tvsearch.sources;

import java.io.PrintStream;
import java.util.Optional;
import com.group12.tvsearch.model.Series;

/** Explicit, bounded live smoke check; reads process environment and never persists responses. */
public final class TmdbVerifyCommand {
    private TmdbVerifyCommand() {}

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank() || args[0].length() > 300) {
            System.out.println("USAGE: TmdbVerifyCommand <exact TV series title>");
            System.exit(2);
        }
        try {
            var client = new HttpTmdbClient(System.getenv("TMDB_READ_ACCESS_TOKEN"),
                    System.getenv("TMDB_API_KEY"));
            System.exit(verify(client, args[0], System.out));
        } catch (IllegalArgumentException exception) {
            // Do not print exception messages from credentials or HTTP libraries.
            System.out.println("TMDB_VERIFY: INVALID_CREDENTIAL_CONFIGURATION");
            System.exit(2);
        }
    }

    static int verify(TmdbClient client, String query, PrintStream output) {
        try {
            Optional<Series> series = client.resolveSeries(query);
            if (series.isEmpty()) {
                output.println("TMDB_VERIFY: NO_CONFIRMED_SERIES");
                return 3;
            }
            output.println("TMDB_VERIFY: SUCCESS");
            output.println("TV_ID: " + series.get().id());
            return 0;
        } catch (TmdbAccessException exception) {
            output.println("TMDB_VERIFY: " + exception.reason().name());
            return 1;
        }
    }
}
