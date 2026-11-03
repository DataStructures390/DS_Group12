package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.LocalDocument;
import com.group12.tvsearch.model.ResultCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Twelve original fictional records, four categories for each of three demo series. */
@Component
public class LocalDocumentRepository {
    private final List<LocalDocument> documents;

    public LocalDocumentRepository() {
        this.documents = List.of(
            doc("harbor-info", 1, "Harbor Lights", "港灣燈火", "Harbor Lights 港灣燈火：影集設定",
                "虛構真人迷你劇：港口工程師在海邊修復燈塔。A fictional live-action drama about a harbor engineer and a lighthouse.", ResultCategory.SERIES_INFORMATION),
            doc("harbor-discussion", 1, "Harbor Lights", "港灣燈火", "港灣燈火 結局心得 discussion",
                "自寫示範討論：工程師與家人如何面對離別？Discussion of the fictional finale and family relationships.", ResultCategory.COMMUNITY_DISCUSSION),
            doc("harbor-review", 1, "Harbor Lights", "港灣燈火", "Harbor Lights 攝影影評 review",
                "自寫示範影評：以燈塔意象分析攝影與敘事。An original sample review of cinematography and storytelling.", ResultCategory.PROFESSIONAL_REVIEW),
            doc("harbor-platform", 1, "Harbor Lights", "港灣燈火", "港灣燈火 播放資訊 streaming",
                "僅供分類測試的虛構平台文件，沒有真實播放服務。Fictional streaming category fixture; no actual availability is asserted.", ResultCategory.STREAMING_PLATFORM),
            doc("garden-info", 2, "Midnight Garden", "午夜花園", "Midnight Garden 午夜花園：影集設定",
                "虛構真人影集：園藝師在夜間照顧社區花園。A fictional live-action series about a gardener and a community garden.", ResultCategory.SERIES_INFORMATION),
            doc("garden-discussion", 2, "Midnight Garden", "午夜花園", "午夜花園 角色心得 discussion",
                "自寫示範討論：鄰居合作種植的角色成長。Original discussion of fictional character development and friendship.", ResultCategory.COMMUNITY_DISCUSSION),
            doc("garden-review", 2, "Midnight Garden", "午夜花園", "Midnight Garden 配樂影評 review",
                "自寫示範影評：分析配樂節奏與夜景敘事。Original sample review of music, pacing and night scenes.", ResultCategory.PROFESSIONAL_REVIEW),
            doc("garden-platform", 2, "Midnight Garden", "午夜花園", "午夜花園 播放資訊 streaming",
                "僅供分類測試，沒有真實播放服務。Fictional streaming fixture without a real subscription or availability claim.", ResultCategory.STREAMING_PLATFORM),
            doc("train-info", 3, "Mountain Train", "山間列車", "Mountain Train 山間列車：影集設定",
                "虛構真人影集：列車長與乘客在山區旅途中相遇。A fictional live-action railway drama about a conductor and passengers.", ResultCategory.SERIES_INFORMATION),
            doc("train-discussion", 3, "Mountain Train", "山間列車", "山間列車 旅途心得 discussion",
                "自寫示範討論：乘客如何在旅途中互相理解。Original discussion of fictional passengers and their journey.", ResultCategory.COMMUNITY_DISCUSSION),
            doc("train-review", 3, "Mountain Train", "山間列車", "Mountain Train 場景影評 review",
                "自寫示範影評：分析山區場景與人物對白。Original sample review of mountain scenery and dialogue.", ResultCategory.PROFESSIONAL_REVIEW),
            doc("train-platform", 3, "Mountain Train", "山間列車", "山間列車 播放資訊 streaming",
                "僅供分類測試，沒有真實播放服務。Fictional streaming fixture, not an official platform link.", ResultCategory.STREAMING_PLATFORM)
        );
    }

    private static LocalDocument doc(String id, long seriesId, String title, String alias,
                                     String heading, String body, ResultCategory category) {
        return new LocalDocument(id, seriesId, title, List.of(alias), heading, body, category);
    }

    public List<LocalDocument> findAll() { return documents; }
    public Optional<LocalDocument> findById(String id) {
        return documents.stream().filter(d -> d.id().equals(id)).findFirst();
    }
}
