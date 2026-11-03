# 真人影集專屬智慧搜尋引擎 — DS_Group12

**TV Series Search Engine with Smart Ranking & AI-Assisted Query Expansion**

這是資料結構課程的組別專題。目標是建立可查詢全球**真人劇情影集與迷你劇**的網頁搜尋引擎，支援繁／簡中文、英文、韓文、日文輸入，並以自己實作的資料結構進行搜尋與排名。

> **Current status:** 規劃與需求文件階段；尚未完成搜尋引擎程式。

## Core features (planned)

- 混合搜尋：**自建 SQLite 影集資料庫＋倒排索引**，搭配合規的外部網頁搜尋 API。
- 多語言影集名稱／別名：以 TMDB 等合法資料來源輔助確認同一部影集。
- **Smart Ranking**：Keyword 45%、Series relevance 30%、Source relevance 15%、Freshness 10%（初始權重，待測試）。
- 搜尋結果分類：全部、網友討論、影集資訊、專業影評、觀看平台。
- **AI Fallback**：一般搜尋不足時，AI 僅輔助提出候選影集與新關鍵字；網頁與網址必須另外驗證。
- 沒有可靠結果時顯示搜尋建議，不產生假的網頁連結。

## Data structures (planned)

`HashMap`、`HashSet`、`ArrayList`、`Inverted Index`、`PriorityQueue`。

## Team and schedule

4–5 位組員；預計 4–6 週；API 總預算 NT$300–1,000。以分階段、可測試的實作為原則。

## Documentation

- [Confirmed specifications](docs/PROJECT_SPEC.md)
- [Development roadmap and testing](docs/ROADMAP.md)

## Important

PTT、Dcard、Wikipedia、TMDB、Rotten Tomatoes、豆瓣及官方串流網站是**預定或候選資料來源**；並不代表已取得 API 或使用授權。所有擷取、索引、展示與重新排序均需遵守來源及 API 條款。不得提交 API Key。

Current documents are planning artifacts, not claims of completed implementation.
