<div align="center">

# TV SERIES SEARCH ENGINE
### Smart Ranking · Cross-Language Discovery · AI-Assisted Query Expansion

**資料結構期末專題｜Group 12**

探索全球真人影集，以多語言名稱辨識、合法來源檢索與可解釋的排名演算法，找到真正相關的網頁。

**Project Status: SceneAtlas functional prototype (feature branches)** · **Language: Java 17+** · **Timeline: 4–6 weeks (planning)**

[四人詳細分工](docs/TEAM_WORKPLAN.md) · [專題規格](docs/PROJECT_SPEC.md) · [開發時程與測試](docs/ROADMAP.md) · [執行看板](docs/EXECUTION_BACKLOG.md)

</div>

---

> [!IMPORTANT]
> **2026-11-06 功能分支已驗證：** 本地倒排索引、排名／分類、SQLite 整理資料、SceneAtlas 簡潔介面與 TMDB 獨立資訊區；PR #8 的 139 個測試及 Java CI 通過，Breaking Bad 即時辨識已實測。
> **目前尚未完成：** 即時網頁搜尋 API、完整多語影集辨識、PTT／Dcard 覆蓋及正式排名評估。此狀態不表示 PR 已合併 main。
> 本 README 區分「已確定需求」與「預定功能」，不將規劃當成成果。

## Contents

1. [專案概覽](#1-專案概覽)
2. [核心功能與使用情境](#2-核心功能與使用情境)
3. [系統架構](#3-系統架構)
4. [Smart Ranking 與資料結構](#4-smart-ranking-與資料結構)
5. [資料來源與技術選擇](#5-資料來源與技術選擇)
6. [開發階段與交付標準](#6-開發階段與交付標準)
7. [進度看板](#7-進度看板)
8. [預計的程式架構](#8-預計的程式架構)
9. [測試與成果衡量](#9-測試與成果衡量)
10. [協作約定](#10-協作約定)
11. [風險、限制與下一步](#11-風險限制與下一步)
12. [專案文件](#12-專案文件)

## 1. 專案概覽

### 1.1 我們想解決什麼問題？

同一部真人影集可能有不同地區的譯名、簡繁體寫法、日韓文名稱、角色暱稱，甚至只有劇情線索。一般關鍵字比對容易漏掉有價值的討論，使用者也常在許多不同網站之間反覆搜尋。

本專題的目標不是取代整個網路搜尋引擎，而是建立一個**聚焦真人影集的領域搜尋系統**：辨識使用者想找的作品、查找真實網頁、消除重複結果，再由自己設計的 **Smart Ranking** 排序。

### 1.2 範圍與定位

| 面向 | 專案定義 |
|:--|:--|
| 搜尋主題 | 全球真人劇情影集、迷你劇 |
| 排除範圍 | 電影、動畫、綜藝、實境節目 |
| 支援查詢語言 | 繁體中文、簡體中文、英文、日文、韓文 |
| 結果形式 | 真實網頁標題、合法可展示的摘要、來源標記與可點擊 URL |
| 資料來源 | 影集 metadata + 授權可用的網頁搜尋候選結果 + 自建合法索引 |
| 技術核心 | HashMap、Inverted Index、HashSet、PriorityQueue；視課程要求增加 Tree／BFS |
| 開發資源 | 4–5 人；4–6 週；整組 API 費用上限 NT$300–1,000 |

### 1.3 設計原則

- **Relevance first：** 不用無關頁面湊數；搜尋建議不算已找到的結果。
- **Explainable ranking：** 排名來自可解釋的分數與資料結構，而非讓 LLM 任意決定。
- **Cross-language, cross-region：** 輸入韓文也能找到美國、台灣影集；輸入中文也能找到韓國、日本影集。
- **Legal data use：** 來源網站與搜尋 API 的展示、儲存、索引及重排均遵循實際授權。
- **Incremental delivery：** 每階段有可驗收成果，不靠空白 commit 製造進度。

## 2. 核心功能與使用情境

### 2.1 預定功能一覽

| Feature | Description | Priority |
|:--|:--|:--:|
| Domain-aware Search | 僅將符合範圍的真人影集視為主要搜尋對象 | Core |
| Cross-Language Resolution | 用同一個影集 ID 串連驗證過的各語言別名 | Core |
| Hybrid Retrieval | 結合本地 Inverted Index 與合規網頁搜尋來源 | Core |
| Multi-Factor Ranking | 用 Keyword、Series、Source、Freshness 四因素排序 | Core |
| Result Categories | 全部、網友討論、影集資訊、專業影評、觀看平台 | Core |
| Smart Query Expansion | 透過別名、角色、作品 metadata 補充查詢詞 | Core |
| Conditional AI Fallback | 一般搜尋不足時，請 AI 提議查詢詞，再重新驗證 | Core |
| Search Suggestions | 無可靠結果時顯示誠實的搜尋建議 | Core |
| Site Tree / BFS | 合法可索引來源的網站樹建構與層級計分 | Extension / rubric-dependent |
| LLM Comparison | 比較純 LLM 與本系統檢索支援的效果 | Extension |

### 2.2 使用情境（預期行為，尚未實測）

| Query | 理想辨識方向 | 搜尋重點 |
|:--|:--|:--|
| 絕命毒師 | Breaking Bad | 台灣討論、影集資訊與影評 |
| 브레이킹 배드 | Breaking Bad | 韓文輸入仍可找到英語或華語的相關網頁 |
| ブレイキング・バッド | Breaking Bad | 使用驗證的日文名稱跨語言檢索 |
| 一個化學老師開始販毒 | Breaking Bad（需再驗證） | 劇情描述 → 候選影集 → 重查 |
| 怪奇物語 心得 | Stranger Things | 優先呈現真正的觀影討論 |
| 芒果 | 不預設對應某部作品 | 先查直接關聯，無法確認則顯示建議 |

> 範例為測試設計，不是現有系統已回傳的真實搜尋結果。

### 2.3 使用者看到的結果

搜尋結果預計具有：**標題、來源網站、內容分類、連結、摘要、相關性得分說明**。若找到符合授權條件的影集資料，頁面可另外顯示作品介紹卡片。對未驗證的作品、來源或網址，則不冒充已確認的資訊。

## 3. 系統架構

以下為預定的高階設計。各元件先透過介面隔離，以便組員平行開發及離線測試。

~~~mermaid
flowchart TD
    A["User Query"] --> B["Query Processor"]
    B --> C["Series Resolver / Verified Aliases"]
    C --> D["Local Inverted Index"]
    C --> E["Authorized Web Search Adapter"]
    D --> F["Merge + URL Dedup"]
    E --> F
    F --> G["Relevance Validation"]
    G --> H{"Enough relevant results?"}
    H -->|Yes| I["Smart Ranking"]
    H -->|No| J["Expand verified aliases / sources"]
    J --> K{"Still insufficient?"}
    K -->|No| F
    K -->|Yes| L["Optional AI Query Suggestions"]
    L --> M["Validate candidate series / keywords"]
    M --> N["Bounded retry"]
    N --> F
    I --> O["Category Tabs + Real Links"]
    G -. "No verified match after budget" .-> P["No-result Message + Suggestions"]
~~~

**角色分工：** TMDB 幫忙辨識作品，搜尋供應商取得候選真實網頁，自己的資料結構負責合法本地資料查詢、去重與授權範圍內的排名；AI 不負責編造 URL 或直接給最終排序。

## 4. Smart Ranking 與資料結構

### 4.1 我們的排名公式

四個項目先正規化為 0～100，再用下式計算。權重是**待實測校準的初始設計**：

~~~text
Score = 0.45 × K + 0.30 × M + 0.15 × S + 0.10 × F

K = Keyword Relevance
M = Series Relevance
S = Source Relevance
F = Freshness
~~~

相關性不只看關鍵字出現次數。系統還應判斷網頁是否真的談到目標影集、來源是否適合使用者需求，以及文章日期是否可靠。

### 4.2 資料結構與可展示價值

| Structure | Intended use | Demo evidence |
|:--|:--|:--|
| HashMap | 多語言別名映射、詞項查表 | 同一部影集各語言名稱對應到同一 ID |
| Inverted Index | 從關鍵字快速找到本地合法索引文件 | 不依靠外部搜尋也能查到已索引內容 |
| HashSet | 網址正規化後去除重複結果 | 不同來源撞到同一網址時只顯示一次 |
| PriorityQueue | 提取分數最高的 Top-K 候選結果 | 顯示排序前後與分數明細 |
| ArrayList | 儲存並合併候選結果 | 分類篩選與結果展示 |
| Queue / Tree (optional) | BFS 子頁探索及樹狀網站計分 | 視老師評分規範與網站授權調整 |

完整設計、演算法與複雜度推導見 [PROJECT_SPEC.md](docs/PROJECT_SPEC.md)。

## 5. 資料來源與技術選擇

### 5.1 預定技術組合

| Layer | Candidate technology | Responsibility |
|:--|:--|:--|
| Language | Java 17+ | 系統主體與資料結構實作 |
| Build | Maven | 編譯與相依管理 |
| Backend | Spring Boot | API、服務與網頁入口 |
| Database | SQLite | 影集、別名、索引與測試資料 |
| Frontend | HTML / CSS / JavaScript | Google 式查詢與分類頁籤 |
| Metadata | TMDB API | 作品辨識、別名與影集資料 |
| External search | Permitted Web Search API | 取得真實網頁候選結果 |
| Fallback | OpenAI API (optional) | 搜尋不足時提出關鍵字候選 |
| Testing | JUnit 5 | 演算法與服務整合測試 |

### 5.2 目標來源

PTT、Dcard、Wikipedia、TMDB、Rotten Tomatoes、豆瓣與官方串流服務是**評估中的資訊來源**。並非每一個網站都有可直接使用的 API，也不能假設搜尋、重排、爬取或快取的使用權已經獲得許可。

若某供應商不允許重新排序或儲存搜尋結果，必須更換授權可行的資料取得方式，或將自訂排名限制在合法可索引資料。

## 6. 開發階段與交付標準

我們採取以實作成果為單位的六階段流程；Stage 編號只是**本組的專案規劃**，不代表已核對老師的正式評分級距。

| Stage | Scheduled window | Core deliverable | Acceptance gate |
|:--|:--|:--|:--|
| 0 — Planning & Scaffold | 起始階段 | 需求文件、Java/Maven 骨架、基本單元測試 | CI／本機實際通過後才算驗收 |
| 1 — Feasibility | 第 1 週 | Java 骨架、搜尋介面合約、Mock Adapter、來源授權調查 | Mock 能執行、來源可行性有紀錄 |
| 2 — Metadata | 第 2 週 | TMDB 整合、SQLite、驗證的跨語言別名 | 相同影集跨語言對應正確 |
| 3 — Local Index | 第 2～3 週 | 自建 Inverted Index 與離線搜尋 | 不連外部搜尋仍可查本地合法資料 |
| 4 — Ranking | 第 3～4 週 | URL 去重、多因素加權、PriorityQueue、分類 | 可重現的分數計算與排序測試 |
| 5 — Fallback & UI | 第 4～5 週 | 多階段重試、可選 AI、完整搜尋頁面 | 超時／零結果／API 錯誤均可處理 |
| 6 — Evaluation | 第 5～6 週 | 攻防題庫、績效分析、展示與報告 | 有人工標記的結果與實測指標 |

[Detailed roadmap](docs/ROADMAP.md)

## 7. 進度看板

**最後文件更新：2026-11-03。此表只紀錄已驗證的工作，不能把計畫當作完工。**

| Item | Status | Evidence |
|:--|:--|:--|
| 專題主題與十項方向 | 完成 | [PROJECT_SPEC](docs/PROJECT_SPEC.md) |
| 多語言需求與搜尋規則 | 完成規劃 | [PROJECT_SPEC](docs/PROJECT_SPEC.md) |
| 開發及測試分期 | 完成規劃 | [ROADMAP](docs/ROADMAP.md) |
| Java/Maven 基礎架構 | 原始碼已上傳，編譯待驗證 | [pom.xml](pom.xml)、[src](src) |
| 基本本地 Inverted Index | 已新增 Java 實作及測試原始碼 | [indexing](src/main/java/com/group12/tvsearch/indexing) |
| TMDB 與外部搜尋整合 | 待開發 | — |
| Smart Ranking 自動測試 | 待開發 | — |
| AI Fallback 與搜尋 UI | 待開發 | — |
| 期末展示與評估 | 待開發 | — |

## 8. 預計的程式架構

已建立以下實際資料夾和檔案，並為每個主要模組加入 TODO；尚未完成的功能請依清單開發。

~~~text
DS_Group12/
├── .github/workflows/          # CI 設定與 TODO
├── .gitignore / .env.example   # 安全設定
├── README.md / TODO.md
├── pom.xml                     # Java 17、Spring Boot、JUnit
├── docs/
│   ├── PROJECT_SPEC.md
│   ├── ROADMAP.md
│   ├── TODO.md
│   ├── proposal/TODO.md
│   ├── diagrams/TODO.md
│   └── presentation/TODO.md
├── data/TODO.md
├── evaluation/TODO.md
├── notes/
│   ├── TODO.md
│   └── stage-reports/TEMPLATE.md, TODO.md
└── src/
    ├── main/java/com/group12/tvsearch/
    │   ├── TvSearchApplication.java, TODO.md
    │   ├── model/       # Series, SearchResult, TODO
    │   ├── indexing/    # Tokenizer, InvertedIndex, TODO
    │   ├── sources/     # WebSearchClient, TmdbClient, TODO
    │   ├── ranking/     # RankingAlgorithm, ResultMerger, TODO
    │   ├── query/       # QueryProcessor, KeywordExpander, TODO
    │   ├── service/     # SearchService, Fallback interface, TODO
    │   └── controller/  # SearchController, TODO
    ├── main/resources/
    │   ├── application.properties / TODO.md
    │   └── static/      # index.html, CSS, JS, TODO
    └── test/java/com/group12/tvsearch/
        ├── indexing/, query/, ranking/, service/
        └── TODO.md
~~~

模組只透過明確的介面協作，可讓「搜尋來源、資料庫、排名、前端」的開發互不阻塞。Mock 與真實 API Adapter 須保持可替換。

### Stage 0：本機執行

需要安裝 **JDK 17+** 與 **Maven**，在 Repository 根目錄執行：

    mvn test
    mvn spring-boot:run

啟動後開啟 http://localhost:8080 或查詢 http://localhost:8080/api/search?q=Breaking%20Bad。

目前沒有串接實際搜尋 API，因此服務會明確返回 `NO_LIVE_SEARCH`，不會顯示假網頁。

> Maven 測試的實際結果應以本機與 GitHub Actions 為準，不能因有測試檔案就宣稱已通過。

### 每個模組的 TODO

請從 [總 TODO](TODO.md)、[docs](docs/TODO.md)、[data](data/TODO.md)、[evaluation](evaluation/TODO.md)、[notes](notes/TODO.md) 開始，進入 Java package 後也會看到各自的 `TODO.md`。

## 9. 測試與成果衡量

我們將準備涵蓋**完整劇名、跨語言別名、角色暱稱、劇情描述、冷門作品、錯字與無關詞**的人工標記測試集合。

| Metric | Definition |
|:--|:--|
| Relevant Hit Rate | 有正解的題目中，至少找到一筆真正相關網頁的比例 |
| Precision@5 | 前五名搜尋結果的相關比例 |
| Zero-result Rate | 沒有驗證過結果的題目比例，須區分可回答／不可回答題 |
| Fallback Success | 使用擴展查詢後真正補救成功的比例 |
| Efficiency | 搜尋延遲、外部 API 呼叫次數及預估成本 |

「顯示相關搜尋建議」不算成功搜尋；測試時要記錄 **query、期望影集、實際結果、來源、分數與判斷理由**。所有指標應報告真實測值，而非預測值。

## 10. 協作約定

**團隊成員與實際負責人尚待填寫。** 以下為建議角色，可視 4–5 位組員調整：

| Workstream | Responsibilities |
|:--|:--|
| Retrieval | 合規搜尋來源、外部 API、錯誤與限流 |
| Metadata & Index | SQLite、TMDB、跨語言別名、Inverted Index |
| Ranking | 分數特徵、去重、PriorityQueue、可解釋性 |
| Fallback | 查詢擴展、AI 呼叫條件、驗證與預算 |
| UI & Quality | 結果介面、分類、JUnit、期末展示 |

### Git 工作流程

1. 新功能在個人 branch 開發，並透過 PR 交互檢查；團隊確認後合併。
2. PR 必須描述**改動內容、為什麼改、如何測試、剩餘限制**。
3. 不上傳 token、憑證、違規抓取的資料、個人隱私或大容量未授權快取。
4. 只有完成真實文件修訂、功能、Bug Fix 或可驗證測試才提交；不製造空 commit。
5. 每個里程碑完成後更新看板並留下可重現的測試證據。

## 11. 風險、限制與下一步

| Risk | Mitigation |
|:--|:--|
| PTT / Dcard 等候選來源搜尋涵蓋率不足 | 第一週先測 20 組關鍵字；保留合法替代來源 |
| 外部搜尋 API 授權不允許重排／快取 | 先確認條款再選供應商；本地索引只收合法內容 |
| 日文／韓文別名與實際劇名錯配 | 同一作品 ID 驗證、多語言人工測試 |
| AI 產生不存在的劇名或網址 | AI 僅輸出候選詞；外部檢索再驗證 |
| 網路中斷或 API 額度不足 | Mock 測試、超時處理、授權內的快取、請求上限 |
| 範圍超出時間預算 | 先完成自訂排名及本地索引；進階功能有完成條件 |

**立即下一步：** 釐清老師對 Tree／HTMLHandler 與 Stage 評分的正式規定，完成 API 使用授權評估，建立 Java Stage 1 骨架，並準備第一批多語言測試案例。

## 12. 專案文件

| Document | Purpose |
|:--|:--|
| [PROJECT_SPEC.md](docs/PROJECT_SPEC.md) | 最完整的產品規格、資料結構、查詢與排名演算法、資料表、API 契約及風險控管 |
| [ROADMAP.md](docs/ROADMAP.md) | 4–6 週計畫、工作流與測試策略 |
| README.md | 對外專題介紹與進度看板 |

---

<div align="center">
<sub>Group 12 · Data Structures Project · Designed for explainable, multilingual TV-series retrieval</sub>
</div>

## Stage 1 local-search demo (2026-11-03)

The local index is now connected to the service/API and category UI on the feature branch. Twelve fictional, self-authored documents have working local links and explicit LOCAL_DEMO labels. External search/TMDB/SQLite/AI remain unimplemented. See [local runbook](docs/LOCAL_DEMO.md), [execution backlog](docs/EXECUTION_BACKLOG.md), [contract](docs/LOCAL_SEARCH_CONTRACT.md), and [source policy](docs/SOURCE_POLICY.md). Earlier Stage 0 descriptions above are historical baseline statements; this section records the incremental local implementation. Use the Maven Wrapper: Windows .\mvnw.cmd test; Unix ./mvnw test. Test evidence is in [stage report](notes/stage-reports/stage0-local-search-report.md).

## Checked real-series collection (2026-11-04)

An opt-in SQLite-backed mode now searches three real series across verified Traditional/Simplified Chinese, English, Japanese and Korean names. Results link to checked Wikipedia views with project-authored short notes; no article bodies are copied and no live search is implied. Full local clean verify passes 118 tests. See [collection runbook and source evidence](docs/CURATED_COLLECTION.md). Earlier Stage 0/1 descriptions above are historical; external provider coverage, live TMDB verification, retries/AI and final evaluation remain incomplete.
