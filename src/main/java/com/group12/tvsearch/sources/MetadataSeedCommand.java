package com.group12.tvsearch.sources;

import java.nio.file.Path;

/** Explicit offline utility; never invoked by normal Spring application startup. */
public final class MetadataSeedCommand {
    private MetadataSeedCommand() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: MetadataSeedCommand <dedicated-demo-database-path>");
        var repository = new SqliteMetadataRepository(Path.of(args[0]));
        repository.seedLocalDemo(new LocalDocumentRepository());
        System.out.println("LOCAL_DEMO: persisted " + repository.findDocuments().size()
            + " original fictional documents; no external data fetched.");
    }
}
