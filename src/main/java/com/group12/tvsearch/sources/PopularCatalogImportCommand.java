package com.group12.tvsearch.sources;

import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.time.Clock;

/** Run only after the account holder confirms permission for this application's cache/search use. */
public final class PopularCatalogImportCommand {
    private PopularCatalogImportCommand() {}
    public static void main(String[] args) {
        if (args.length != 2 || !"--approved-use".equals(args[1])) {
            System.out.println("Usage: PopularCatalogImportCommand <new-dedicated-database> --approved-use");
            System.out.println("Confirm account permission for the intended search/cache use before running.");
            System.exit(2);
        }
        try {
            Path database = Path.of(args[0]).toAbsolutePath().normalize();
            Files.createDirectories(database.getParent());
            try (var channel = FileChannel.open(Path.of(database + ".import.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var lock = channel.tryLock()) {
                if (lock == null) { System.out.println("CATALOG_IMPORT: ANOTHER_IMPORT_RUNNING"); System.exit(2); }
                var store = new PopularCatalogStore(database, 500, true, Clock.systemUTC());
                var client = new HttpTmdbClient(System.getenv("TMDB_READ_ACCESS_TOKEN"), System.getenv("TMDB_API_KEY"));
                String result = new PopularCatalogImporter(client, store, () -> Thread.sleep(350)).run(2500, System.out);
                var state = store.state();
                System.out.printf("CATALOG_IMPORT: %s; accepted=%d; examined=%d; requests=%d; expires=%s%n",
                    result, state.accepted(), state.examined(), state.requests(), state.expiresAt());
                System.exit("COMPLETE".equals(result) ? 0 : 3);
            }
        } catch (TmdbAccessException e) {
            System.out.println("CATALOG_IMPORT: " + e.reason().name() + "; progress saved; no automatic retry");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("CATALOG_IMPORT: INTERRUPTED; progress saved"); System.exit(1);
        } catch (Exception e) {
            // JDBC/transport messages may include paths, request URLs or account information.
            System.out.println("CATALOG_IMPORT: LOCAL_CONFIGURATION_OR_STORAGE_ERROR; no credentials printed");
            System.exit(2);
        }
    }
}
