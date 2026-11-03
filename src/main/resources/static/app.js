const form = document.querySelector("#search-form");
const status = document.querySelector("#status");
form.addEventListener("submit", async event => {
  event.preventDefault();
  const query = new FormData(form).get("q");
  status.textContent = "正在查詢本地 Stage 0 API…";
  try {
    const response = await fetch("/api/search?q=" + encodeURIComponent(query));
    if (!response.ok) throw new Error("HTTP " + response.status);
    const data = await response.json();
    status.textContent = data.message || "目前沒有可驗證的搜尋結果。";
  } catch (error) {
    status.textContent = "連線失敗：" + error.message;
  }
});
