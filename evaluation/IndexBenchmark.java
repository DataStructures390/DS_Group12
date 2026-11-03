import com.group12.tvsearch.indexing.InvertedIndex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/** Offline experiment using only generated original tokens. Run after mvnw compile. */
public class IndexBenchmark {
    private static volatile long consumed;
    private record Document(String id, Set<String> words) {}
    private static Set<String> scan(List<Document> documents, String query) {
        Set<String> found = new HashSet<>();
        for (Document document : documents) if (document.words().contains(query)) found.add(document.id());
        return found;
    }
    private static double medianMicros(Supplier<Set<String>> search, int batchSize) {
        for (int i = 0; i < 100; i++) consumed += search.get().size();
        double[] samples = new double[11];
        for (int sample = 0; sample < samples.length; sample++) {
            long start = System.nanoTime();
            for (int i = 0; i < batchSize; i++) consumed += search.get().size();
            samples[sample] = (System.nanoTime() - start) / (1000.0 * batchSize);
        }
        java.util.Arrays.sort(samples);
        return samples[samples.length / 2];
    }
    public static void main(String[] args) {
        // Warm the actual tokenizer/index implementation before measuring build time.
        InvertedIndex warm = new InvertedIndex();
        for (int i = 0; i < 100; i++) warm.addDocument("warm" + i, "term" + i + " warmup");
        System.out.println("{\"experiment\":\"local-synthetic-index-v1\",\"java\":\""
            + System.getProperty("java.version") + "\",\"batchSize\":100,\"samples\":11,\"rows\":[");
        boolean first = true;
        for (int size : new int[]{100, 1000, 10000}) {
            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < size; i++) documents.add(new Document("d" + i,
                Set.of("term" + (i % 100), "group" + (i % 10), "unique" + i)));
            InvertedIndex index = new InvertedIndex();
            long start = System.nanoTime();
            for (Document d : documents) index.addDocument(d.id(), String.join(" ", d.words()));
            double buildMillis = (System.nanoTime() - start) / 1_000_000.0;
            for (String query : new String[]{"unique" + (size / 2), "term7", "absent"}) {
                Set<String> expected = scan(documents, query);
                if (!expected.equals(index.search(query))) throw new AssertionError("Index and scan disagree");
                // Both paths allocate a result set; no result is discarded without consumption.
                double indexed = medianMicros(() -> index.search(query), 100);
                double scanned = medianMicros(() -> scan(documents, query), 100);
                if (!first) System.out.println(",");
                first = false;
                System.out.printf(Locale.ROOT,
                    "{\"documents\":%d,\"termsPerDocument\":3,\"query\":\"%s\",\"hits\":%d,\"buildMillis\":%.3f,\"indexedMedianMicros\":%.3f,\"scanMedianMicros\":%.3f}",
                    size, query, expected.size(), buildMillis, indexed, scanned);
            }
        }
        System.out.println("],\"consumedHits\":" + consumed + "}");
    }
}
