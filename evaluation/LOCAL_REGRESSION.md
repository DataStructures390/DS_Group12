# Local fixture regression set

LocalSearchIntegrationTest.twentyCaseLocalRegression defines 20 expected query cases over the twelve original fictional documents. Cases cover English title/case/fullwidth, Chinese titles/bigrams, unique review keywords, unknown and out-of-scope words. An additional test verifies that adding 攝影 ranks harbor-review first and that category mismatch returns NO_RESULTS.

These are deterministic functional checks, not manually reviewed Internet relevance results. Actual real-title alias, cross-country/five-language and live Precision@5 studies remain pending P2/P3. The 20-query PTT/Dcard provider coverage protocol in docs/SOURCE_POLICY.md is NOT RUN.
