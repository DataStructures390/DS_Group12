package com.group12.tvsearch.ranking;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.SearchResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RankingDifferentialTest {
    private static double referenceScore(SearchResult row) {
        double score = 0;
        double[] features = {row.keywordRelevance(), row.seriesRelevance(), row.sourceRelevance(), row.freshness()};
        double[] weights = {.45, .30, .15, .10};
        for (int i = 0; i < features.length; i++) {
            double feature = features[i];
            if (!Double.isFinite(feature)) continue;
            if (feature < 0) feature = 0;
            if (feature > 100) feature = 100;
            score += weights[i] * feature;
        }
        return score;
    }
    @Test void boundedHeapAgreesWithFullSortAcrossLimitsAndInputOrders() {
        var random = new Random(20261104);
        List<SearchResult> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) rows.add(new SearchResult("https://example.org/fixture/" + i,
            "Synthetic " + i, "Original test", "TEST_FIXTURE", ResultCategory.SERIES_INFORMATION,
            i % 9 == 0 ? Double.NaN : random.nextInt(301) - 100,
            i % 11 == 0 ? Double.POSITIVE_INFINITY : random.nextInt(301) - 100,
            random.nextInt(301) - 100, random.nextInt(301) - 100));
        var expected = rows.stream().sorted(Comparator.comparingDouble(RankingDifferentialTest::referenceScore)
            .reversed().thenComparing(SearchResult::url)).toList();
        var ranking = new RankingAlgorithm();
        for (int k : new int[]{0, 1, 3, 50, 500, 1000, 2000}) {
            java.util.Collections.shuffle(rows, random);
            var actual = ranking.rank(rows, k);
            assertEquals(expected.subList(0, Math.min(k, expected.size())),
                actual.stream().map(r -> r.result()).toList(), "Top-K " + k);
            for (var r : actual) assertEquals(referenceScore(r.result()), r.score(), .000001);
        }
    }
}
