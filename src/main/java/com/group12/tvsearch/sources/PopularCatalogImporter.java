package com.group12.tvsearch.sources;

import java.io.PrintStream;
import java.sql.SQLException;

/** Sequential and bounded; successful details commit atomically, failed requests remain resumable. */
public final class PopularCatalogImporter {
    public interface Pause { void beforeRequest() throws InterruptedException; }
    private final TmdbCatalogSource source;
    private final PopularCatalogStore store;
    private final Pause pause;
    public PopularCatalogImporter(TmdbCatalogSource source, PopularCatalogStore store, Pause pause) {
        this.source = source; this.store = store; this.pause = pause;
    }
    public String run(int requestBudget, PrintStream output) throws SQLException, InterruptedException {
        if (requestBudget < 1 || requestBudget > 2500) throw new IllegalArgumentException("Request budget must be 1..2500");
        store.requireFresh();
        for (int requests = 0; requests < requestBudget; requests++) {
            var state = store.state();
            if (state.accepted() == state.target()) return "COMPLETE";
            var pending = store.pending();
            if (pending.isEmpty() && state.nextPage() > state.totalPages()) return "SOURCE_EXHAUSTED";
            pause.beforeRequest();
            store.recordRequest();
            if (pending.isPresent()) {
                long id = pending.getAsLong();
                store.finish(id, source.basic(id));
                var updated = store.state();
                output.printf("CATALOG_PROGRESS: %d/%d accepted; %d examined; %d total requests%n",
                    updated.accepted(), updated.target(), updated.examined(), updated.requests());
            } else store.addPage(source.popular(state.nextPage()));
        }
        return store.state().accepted() == store.state().target() ? "COMPLETE" : "REQUEST_BUDGET";
    }
}
