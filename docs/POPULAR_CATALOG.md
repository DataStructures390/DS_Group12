# 熱門 500 部基本資料匯入

使用者於 2026-11-07 選擇：熱門優先、先完成 500 部基本資料，五語別名後補。

## 現況與用途確認

程式及合成測試可先完成；不代表真實資料已匯入。
實際批次下載目前等待帳戶持有人確認 TMDB 已核准本應用的搜尋及保存用途。
免費開發者權杖的認證成功不等於所有用途皆獲准。

2026-11-07 重新閱讀的官方條款列出搜尋引擎相關用途及最長六個月的快取限制；
本專案未假定非商業課堂用途必然涵蓋所有條款。請以帳戶適用的書面核准／協議為準。
條款頁：https://www.themoviedb.org/api-terms-of-use
來源政策及 AI fallback 的使用範圍也需一併確認；不要將快取提供給 AI 訓練或驗證。

## 實作方式

- 使用 TV popular 端點，保留頁面回傳順序，逐一讀 TV details 並沿用 Scripted／Miniseries 與類型排除檢查。
- 未通過範圍的候選不計入目標。不是抓 500 筆就宣稱完成 500 部。
- 保存：TV ID、原名、en-US 顯示名稱、原始語言、製作國家、可用的首播日期、來源 URL、取得時間。
- 不取得海報、劇情簡介、影評、評分、串流地區，亦不補造五語名稱。日期與國家缺值明確標示。
- 每部產生一個指向 TMDB TV 頁的資訊結果。這是已匯入的 metadata cache，不是即時論壇／影評搜尋。
- 原名與 en-US 顯示名稱進入別名索引；若原名就是英文且名稱相同，唯一鍵會去重，因此不是固定每部兩個別名。
- 使用獨立 data/tmdb-popular.sqlite；原有三部的 curated.sqlite 不改動。匯入器拒絕已有其他資料的資料庫。
- 每個候選的影集、別名、資訊結果、基本欄位及進度在同一 SQLite 交易中提交。重新執行會續跑，已處理 ID 不重複查詢。
- 每次請求前等待 350ms，單執行緒；每次執行最多 2500 個請求、最多 100 頁。401／403／429／網路錯誤立即停止，沒有自動重試。
- 500 部完成後再次執行不發請求。配額用盡或候選不足時，回報實際筆數與未完成狀態，不湊資料。
- TMDB 熱門排序會變動，分頁與不同日期續跑也可能變動；這是熱門來源的範圍內樣本，不宣稱某一瞬間全球精確 Top 500。
- 目前不跨國平均分配，不將 TMDB 熱度當作我們的搜尋排名分數。

## 在取得適用用途核准後執行

先在原本的權杖 PowerShell 視窗執行。不要將權杖放入命令參數或聊天室。
程式要先完成 package；若舊 JAR 正在執行，先 Ctrl+C 停止後再打包。

```powershell
java "-Dloader.main=com.group12.tvsearch.sources.PopularCatalogImportCommand" -cp target/tvseries-search-0.1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher data/tmdb-popular.sqlite --approved-use
```

--approved-use 是執行者已確認用途的明確聲明，不是程式代替 TMDB 授權。
預期完成標記：CATALOG_IMPORT: COMPLETE; accepted=500。
若顯示 RATE_LIMITED 或其他失敗，保留資料庫並依原因處理；不要反覆快速重試。
若保留途中進度但想改變目標或重新抓目前熱門排序，應使用另一個新的資料庫路徑。

啟動搜尋：

```powershell
java -jar target/tvseries-search-0.1.0-SNAPSHOT.jar --search.collection=popular --search.local-demo.enabled=false --metadata.tmdb.enabled=true
```

離線搜尋也可停用 metadata.tmdb.enabled。資料庫在啟動時建立索引；匯入後須重新啟動才能讀到新增資料。
首頁資料說明顯示實際筆數，只有影集資訊分類有結果。
快取原名／英文名稱可以直接定位 TV ID，再按需取得即時資訊；同名衝突不任選。

## 保存期限

快取到期時間保守地以整個匯入開始時間加六個曆月計算；不是每次讀取延長期限。
啟動、查詢與續跑都檢查期限，到期拒絕提供資料。但程式關閉時無法自動清理磁碟：
帳戶持有人必須在適用期限前更新或移除舊快取及備份，不能將「不再顯示」當成已刪除資料。
目前未加入背景更新或自動刪除。需要更新／移除時先確認專用資料庫路徑，另行執行。

## 驗證

178 個本機測試通過（2026-11-07）。500 筆測試為合成影集，不是 TMDB 真實資料。
主要測試涵蓋精確 500 筆停止、範圍排除後繼續、去重、斷點續跑、429 不重試、
交易回滾、拒絕覆寫 curated 資料、到期拒絕使用、離線搜尋、分類及 Spring 啟動。
實際下載筆數、真實標題查詢與瀏覽器驗收目前仍未執行。

官方技術參考：
- https://developer.themoviedb.org/reference/tv-series-popular-list
- https://developer.themoviedb.org/reference/tv-series-details
- https://developer.themoviedb.org/docs/rate-limiting
