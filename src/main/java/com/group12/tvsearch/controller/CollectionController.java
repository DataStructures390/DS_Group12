package com.group12.tvsearch.controller;

import com.group12.tvsearch.sources.CuratedSearchProvider;
import com.group12.tvsearch.sources.PopularSearchProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CollectionController {
    private final Optional<CuratedSearchProvider> curated;
    private final boolean demoEnabled;
    private final Optional<PopularSearchProvider> popular;
    public CollectionController(Optional<CuratedSearchProvider> curated,
                                Optional<PopularSearchProvider> popular,
                                @Value("${search.local-demo.enabled:true}") boolean demoEnabled) {
        this.curated = curated; this.popular = popular; this.demoEnabled = demoEnabled;
    }
    @GetMapping("/api/collection")
    public CollectionInfo info() {
        if (popular.isPresent()) return new CollectionInfo("popular",
            popular.get().fresh() ? popular.get().description() : "影集資料快取已過期或無法讀取，請更新後重新啟動。",
            popular.get().fresh() ? popular.get().examples() : List.of());
        if (curated.isPresent()) return new CollectionInfo("curated",
            "人工整理資料集：真實影集與已核對 Wikipedia 連結，自寫導覽文字；不是即時搜尋。核對日期 2026-11-04。只有影集資訊分類。",
            List.of("絕命毒師", "기묘한 이야기", "ダーク"));
        return new CollectionInfo(demoEnabled ? "demo" : "disabled",
            demoEnabled ? "本地搜尋展示：12 筆自寫虛構文件，不代表真實影集、影評或播放服務。尚未接上即時網頁搜尋。"
                : "本地示範已關閉，尚未接上即時網頁搜尋。",
            demoEnabled ? List.of("港灣燈火", "Midnight Garden", "山間列車") : List.of());
    }
    public record CollectionInfo(String collection, String message, List<String> examples) {}
}
