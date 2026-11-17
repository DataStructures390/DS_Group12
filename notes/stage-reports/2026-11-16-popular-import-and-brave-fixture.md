# Session Summary — 2026-11-16

Task IDs: P2-02 / P2-03 / P2-04 / P3-05. Owner: unassigned.

## 熱門 500 部實際匯入

- 帳戶持有人確認 TMDB 用途後，在自己的 PowerShell 執行匯入（金鑰只存在該視窗的環境變數，未經過聊天或檔案）。
- 第一次因環境變數未設定而停止；第二次 TMDB 回傳認證失敗；改設正確的 Read Access Token 後成功。
- 結果：`CATALOG_IMPORT: COMPLETE; accepted=500; examined=882; requests=928; expires=2027-05-16`。
  382 部因不在範圍內（動畫、綜藝、實境等）被排除。
- 瀏覽器實測（popular 模式、TMDB 即時資訊關閉）：首頁顯示 500 部；
  Breaking Bad、Stranger Things、The Last of Us、Game of Thrones、Squid Game、오징어 게임 皆找到；
  「絕命毒師」0 筆，符合目前只存英文與原文名稱的設計。
- `data/tmdb-popular.sqlite` 已被 .gitignore 排除，只存在執行者本機；2027-05-16 前須更新或刪除。

## 名稱更新

- 網頁與文件名稱由 SceneAtlas 改為「追劇雷達 DramaRadar」（分支 chore/rename-dramaradar）。

## Brave 假資料版

- 分支 feat/brave-search-fixture：Brave 回應格式轉換、網域分類、K/M/S/F 計分、假資料（.example 網域）。
- 192 個本機測試通過；瀏覽器實測 Breaking Bad 6 筆（去重 1 筆、略過 1 筆 ftp）、絕命毒師 3 筆。
- 沒有真實 Brave 請求、金鑰或費用。

## 下一步

- 使用者決定是否申請 Brave、確認條款並設定每月上限。
- A 補熱門劇的中文、日文、韓文名稱與出處。
- popular／curated 模式混入網頁結果（P2-05）。
