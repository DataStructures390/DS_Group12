# TV Series Search Engine — Project Specification

> **Group 12｜資料結構專題｜2026-11-03**  
> **文件狀態：已確認的開發規劃；不代表功能已完成。**

## 1. 專題名稱與目標

**TV Series Search Engine with Smart Ranking & AI-Assisted Query Expansion**

建立以**全球真人劇情影集與迷你劇**為範圍的搜尋引擎。使用者可以輸入中、英、日、韓文影集名稱、別名、角色或模糊劇情描述；系統整合合法取得的網頁候選結果、自建影集資料與倒排索引，再用自訂資料結構演算法排名，顯示**真實可點擊**的網頁連結。

不收錄：電影、動畫影集、綜藝、實境節目。**輸入語言不限制影集來源國家**，例如日文輸入可找到美國、台灣等地的真人影集。

## 2. 已確定的 10 項決策

| # | 決策 | 已確認方向 |
|---|---|---|
| Q1 | 搜尋範圍 | 全球真人劇情影集／迷你劇 |
| Q2 | 網站策略 | PTT、Dcard 等指定來源優先，不足再擴大合法來源 |
| Q3 | AI 使用 | **僅**在一般搜尋不足時理解及擴展關鍵字；不直接提供未驗證結果 |
| Q4 | 使用介面 | Google 式混合結果；分類頁籤 |
| Q5 | 搜尋架構 | Web Search API + SQLite 影集資料庫 + **自建 Inverted Index** |
| Q6 | 擴展策略 | 直接相關結果優先；合適時給相關影集資訊／平台（須清楚標示類型） |
| Q7 | 開發資源 | 4–5 人、4–6 週、整組 API 預算 NT$300–1,000 |
| Q8 | 目標來源 | PTT、Dcard、Wikipedia、TMDB、Rotten Tomatoes、豆瓣、官方串流平台 |
| Q9 | Smart Ranking | 關鍵字 45%、影集相關性 30%、來源適切性 15%、時效性 10% |
| Q10 | 找不到結果 | 多階段搜尋＋AI Fallback；仍無相關網頁時提供搜尋建議 |

第三方網站與搜尋 API 的擷取、展示、修改排序、快取、索引及連結方式，均以**實際授權與使用條款**為準；列為目標來源不等於已完成串接。

## 3. 搜尋結果介面

預設「**全部**」混合顯示；可切換：
- **網友討論**：PTT、Dcard 等
- **影集資訊**：Wikipedia、TMDB 等
- **專業影評**：經授權可呈現的影評網站
- **觀看平台**：經驗證的官方影集連結

每筆網頁搜尋結果應有：標題、真實 URL、合法可展示的摘要、來源、分類；有足夠資料時可顯示評分依據。不得捏造網頁或把搜尋建議充作已找到的結果。

## 4. 多語言跨地區搜尋（追加必做需求）

支援：
- 繁體中文、簡體中文、英文、韓文、日文
- 同一部影集的多語言名稱與別名對應到**同一個穩定 Series ID / TMDB ID**
- 透過驗證過的別名擴展搜尋詞；非正式譯名／AI 猜測需再驗證
- 搜尋語言與影集**來源國家**分離；使用韓文可以搜尋台灣、美國影集，反之亦然
- 查詢次數要有上限，避免多語言擴展造成 API 成本暴增

例如「絕命毒師」「Breaking Bad」或經驗證的日韓文片名應辨識成同一部影集，再以不同語言查詢真實網頁來源。

## 5. 系統架構

```text
User Query
   ↓
QueryProcessor (normalize language, aliases, keywords)
   ↓
SeriesResolver (local aliases → TMDB verification)
   ↓
Local Inverted Index  +  Permitted Web Search API
   ↓
SearchResultMerger (HashSet URL deduplication)
   ↓
Relevance Validation → Smart Ranking (PriorityQueue)
   ↓
Categorized, clickable real web results
   │
   └─ If insufficient verified results:
        expand verified aliases / sources → retry
        → optional AI-generated query suggestions → verify → retry
        → show "no verified match" + related-search suggestions
```

**重要區分**：TMDB 用來辨識影集與取得影集資訊，**不是** PTT 或 Dcard 的網頁搜尋 API。Web Search API 回傳外部候選網頁；本地 Inverted Index 只處理本組**有權索引**的文件與合法資料。

## 6. 資料結構核心

| Data Structure | 專題實際用途 |
|---|---|
| `HashMap<String, Set<Integer>>` | 多語言別名／關鍵字的倒排索引 |
| `HashSet<String>` | URL 去重複 |
| `ArrayList<SearchResult>` | 合併候選搜尋結果 |
| `PriorityQueue<SearchResult>` | 取出排名分數最高的結果 |
| `SQLite` | 影集、驗證別名、授權文件、來源規則、測試資料 |

建議 Classes：`Series`、`SeriesAlias`、`SearchResult`、`InvertedIndex`、`QueryProcessor`、`SeriesResolver`、`KeywordExpander`、`WebSearchClient`、`TMDBClient`、`SearchResultMerger`、`RankingAlgorithm`、`AIFallbackService`、`SearchService`、`SearchController`。

老師範例包含 Keyword／Node／Tree／HTMLHandler 等觀念；是否需要完全依照範例實作，**仍需向老師確認**，不應視為已核定的強制規定。

## 7. Smart Ranking

所有因素先正規化為 0–100 分，初始權重：

```text
Score = 0.45*K + 0.30*M + 0.15*S + 0.10*F

K = Keyword relevance (title / permitted fields / aliases)
M = Series relevance (evidence of same live-action series)
S = Source relevance (suitability of source for category)
F = Freshness (when date exists and is reliable)
```

以上為**待測試的初始公式**，不是已證明最佳的參數。排序應能解釋分數來源，不可讓 AI 直接決定最終網頁排名；不得根據無法讀取的文章全文擅自推斷相關性。

## 8. 外部服務與合規原則

- **TMDB**：辨識影集、名稱與合法使用的 metadata，遵守 API attribution/授權。
- **Web Search API（候選：Brave）**：取得即時候選網頁；正式選用前確認允許的展示、合併、排序與快取行為。
- **Wikipedia／其他開放資料**：備援，依各自 API 使用規定及內容授權。
- **OpenAI API**：只在相關性不足時推測影集／產生查詢詞，結果要驗證。
- **PTT、Dcard、Rotten Tomatoes、豆瓣與官方串流頁面**：依來源使用條款與技術可行性分階段支援。

不違規爬取、不儲存禁止保存的網頁或搜尋 API 結果、不繞過登入、不提交 API Key、不產生假網址。若外部 API 不允許重新排序，必須調整服務供應商或將自訂排名限於授權允許的資料。

## 9. 預期成果

一個可執行的 Java 搜尋網站，包含：
1. 真實、經驗證的影集相關網頁連結
2. 自建 SQLite 影集資料庫及真實倒排索引
3. 自訂加權 Smart Ranking 與結果去重
4. 多語言別名與跨地區搜尋
5. 分類切換
6. 分階段搜尋與受限的 AI Fallback
7. 可重現的搜尋品質測試與展示

**目前僅確定需求與設計，所有實作進度以後續實際程式碼、測試及 Commit 為準。**
