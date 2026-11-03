package com.group12.tvsearch.sources;

import com.group12.tvsearch.indexing.MetadataSearchIndex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/** Search an existing, explicitly selected local database without any provider request. */
public final class MetadataSearchCommand {
    private MetadataSearchCommand() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: MetadataSearchCommand <database-path> <query>");
        Path database = Path.of(args[0]);
        if (!Files.isRegularFile(database)) throw new IllegalArgumentException("Select an existing metadata database");
        var index = new MetadataSearchIndex(new SqliteMetadataRepository(database));
        var response = index.search(args[1], 10, null);
        var output = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        output.println(response.status() + ": " + response.results().size() + " local records");
        for (var row : response.results()) output.printf(java.util.Locale.ROOT, "%.1f %s %s%n",
            row.score(), row.result().title(), row.result().url());
        output.flush();
    }
}
