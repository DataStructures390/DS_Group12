# Sources — TODO

**Owner：待組員分配｜文件建立：2026-11-03**

> 已勾選代表程式或文件已新增，不代表 Maven 測試已通過或正式功能已完成。

- [x] 建立 WebSearchClient、TmdbClient 介面
- [x] 建立 NoOpWebSearchClient，避免捏造外部網頁結果
- [ ] 取得 TMDB 權限並串接經驗證的影集 metadata
- [ ] 確認 Web Search API 對外部結果展示、重排及快取的條款
- [ ] 測試 PTT／Dcard 等候選來源搜尋涵蓋率
- [ ] 處理 API 429、逾時、費用與重試上限
- [ ] 增加 Mock Adapter，避免單元測試依賴外部網路

**驗收標準：** 確實獲准搜尋的來源可回傳真實 URL，且具可靠錯誤處理。

- [x] P2-02 groundwork：選配 TMDB HTTP adapter／NoOp／安全錯誤與本機模擬測試；85 個完整測試通過
- [ ] P2-02 live：真實授權憑證 smoke test、實際回應／類型規則查核與 TMDB attribution 展示
