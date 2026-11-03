# Service — TODO

**Owner：待組員分配｜文件建立：2026-11-03**

> 已勾選代表程式或文件已新增，不代表 Maven 測試已通過或正式功能已完成。

- [x] 建立 SearchService、DefaultSearchService、SearchResponse
- [x] 建立 AiFallbackService 接口
- [x] 無外部搜尋來源時明確回傳 NO_LIVE_SEARCH
- [ ] 整合 SQLite、Inverted Index 與外部搜尋候選
- [ ] 加入影集身份驗證、範圍過濾與 URL 驗證
- [ ] 只在不足時啟動別名／來源／選擇性 AI 擴展
- [ ] AI 只能提出待驗證候選詞，不可以產生假連結
- [ ] 處理部分結果、無結果、外部 API 失效及費用控制

**驗收標準：** 所有搜尋階段可記錄狀態並以測試重現。
