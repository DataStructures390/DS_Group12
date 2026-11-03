# Group 12 — Project Overview — TODO

**Owner：待組員分配｜文件建立：2026-11-03**

> 已勾選代表程式或文件已新增，不代表 Maven 測試已通過或正式功能已完成。

- [x] 完成專題目標、十項決策及技術規格文件
- [x] 新增 Stage 0 Java／Maven／Spring Boot 原始碼和單元測試原始碼
- [x] 建立 GitHub Actions CI 設定
- [ ] 確認 CI 真正編譯與測試通過
- [ ] 向老師確認 Stage、Tree、網頁抓取與 LLM 比較是否為正式評分要求
- [ ] 確定組員及各模組負責人
- [ ] 驗證合法 Web Search API 與 TMDB 整合方式
- [ ] 完成跨語言別名、SQLite、本地索引和網頁混合排名
- [ ] 整合 AI Fallback、搜尋 UI 和測試
- [ ] 交付 Proposal、期末報告與 Demo

**驗收標準：** 每個完成項有程式碼、測試結果或文件連結作為證據。

### Stage 1 local milestone
- [x] 本地倒排索引接入 SearchService，12 筆自寫虛構文件與實際本地連結
- [x] 本地分類、可解釋排名、去重與 API 整合測試
- [x] Maven Wrapper 與本機執行說明
- [x] 建立 EXECUTION_BACKLOG 與 SOURCE_POLICY 初查；外部權限未確認項目保持待辦
- [ ] 完成供應商選擇與 20 組 PTT/Dcard 真實 API 涵蓋率實驗

證據：notes/stage-reports/stage0-local-search-report.md。原有跨語言／SQLite／外部整合／AI 的總體項目仍未完成。

- [x] P2-01：SQLite 資料基礎與測試完成；尚未接入 SearchService／TMDB，詳見 docs/SQLITE_METADATA.md

- [x] 已核對 3 部真實影集、五種語言／文字名稱與 15 個來源檢視連結；選配 SQLite → Index → API → UI 已連通，118 個本機測試通過
- [ ] 擴充真實影集集合及獨立人工審查的廣泛多語品質評估；目前小樣本不代表全球涵蓋率
