# Brave 網頁搜尋：假資料版

更新：2026-11-16。對應 EXECUTION_BACKLOG 的 P2-04（部分）。

## 這一版做了什麼

- `BraveWebSearchClient` 接上既有的 `WebSearchClient` 介面，讀取 Brave Web Search API 的回應格式（`web.results[]` 的 title、url、description、page_age）。
- `FixtureBraveTransport` 不連網路，從 `src/main/resources/data/brave-fixture.json` 讀出假回應。
  所有網址都在保留網域 `.example` 上，標題都以「[假資料]」開頭，網頁上不可點擊。
- 只收錄三個示範查詢：Breaking Bad、絕命毒師、Squid Game。其他查詢誠實回傳 0 筆。
- 沒有真實 Brave 連線、沒有金鑰、沒有任何費用。

## 分數怎麼算（初始設計，待校準）

| 項目 | 計算方式 |
|---|---|
| K 關鍵字 | 查詢詞在標題＋摘要中的覆蓋率 |
| M 影集 | 查詢詞在標題中的覆蓋率 |
| S 來源 | 依網域的初始權重：Wikipedia／TMDB 80、IMDb 75、Rotten Tomatoes／Metacritic 75、PTT／Dcard／串流平台 70、Reddit 60、未知網域 20 |
| F 新鮮度 | page_age 在 30 天內 100 分，之後線性下降，3 年後 0 分；缺日期或未來日期為 0 |

分類也依網域決定：PTT／Dcard／Reddit 為網友討論，百科／TMDB／IMDb 為影集資訊，
Rotten Tomatoes／Metacritic 為專業影評，Netflix／Disney+／Prime Video 為觀看平台，未知網域歸入影集資訊。
非 http/https 網址、相對網址與沒有標題的結果會被略過；同網址（大小寫、預設埠、#片段不同）由既有 HashSet 去重。

2026-11-19 修正：搜尋服務一律向網頁來源要最多筆候選（Brave 單次上限 20 筆），先分類篩選與去重，最後才取前 limit 筆。
先前會先截斷成 limit 筆再篩選，所以 `limit=1&category=PROFESSIONAL_REVIEW` 會錯誤回傳 0 筆，`limit=2` 也可能因重複網址只剩 1 筆。

S 權重是設計值，不是量測出的可信度；正式評估後再調整。

## 怎麼開啟

```powershell
.\mvnw.cmd -q clean package
java -jar target/tvseries-search-0.1.0-SNAPSHOT.jar --search.web.provider=brave-fixture
```

開 http://localhost:8080 ，點「Breaking Bad」或「絕命毒師」。也可用環境變數 `WEB_SEARCH_PROVIDER=brave-fixture`。
預設值是 `none`，行為與先前相同。本地示範資料有結果時仍優先顯示本地資料。

## 尚未完成

- 真實 HTTP 連線（需要使用者選定方案、確認條款允許混合與重新排名、自行申請金鑰並設定每月上限）。
- curated／popular 模式尚未混入網頁結果；目前只有 demo 模式在本地無結果時才查網頁。
- PTT／Dcard 實際覆蓋率（SOURCE_POLICY.md 的 20 筆實驗）仍為 NOT RUN。

## 驗證

本機 `.\mvnw.cmd clean verify`：192 個測試全部通過（新增 13 個）。
瀏覽器實測：Breaking Bad 6 筆（8 筆假資料去掉 1 筆重複、1 筆 ftp），絕命毒師 3 筆，標示為假資料且不可點擊。
