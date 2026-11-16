package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.SearchResult;
import java.util.List;

/** Interface for an authorized external web search provider. */
public interface WebSearchClient {
    List<SearchResult> search(String normalizedQuery, int limit);

    /** Message shown with this provider's results. */
    default String resultNotice() {
        return "結果已經去重與排序；來源可信度仍需驗證。";
    }

    /** Message when this provider returned nothing. */
    default String emptyNotice() {
        return "尚未連接網頁搜尋 API，沒有即時網頁結果。";
    }

    /** Queries known to return results, offered as examples in the UI. */
    default List<String> sampleQueries() {
        return List.of();
    }
}
