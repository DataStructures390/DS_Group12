const form = document.querySelector('#search-form');
const status = document.querySelector('#status');
const results = document.querySelector('#results');
const input = document.querySelector('#q');
const labels = { COMMUNITY_DISCUSSION: '網友討論', SERIES_INFORMATION: '影集資訊', PROFESSIONAL_REVIEW: '專業影評', STREAMING_PLATFORM: '觀看平台' };
let category = '';
let activeRequest;
let collectionExamples = [];
let submittedQuery = '';
function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}
function safeLink(result) {
  try {
    const url = new URL(result.url, location.origin);
    if (result.source === 'LOCAL_DEMO') return url.origin === location.origin && /^\/demo\/documents\/[a-z0-9-]+$/.test(url.pathname) ? url.href : null;
    return ['https:', 'http:'].includes(url.protocol) ? url.href : null;
  } catch { return null; }
}
function render(items) {
  results.replaceChildren();
  for (const item of items) {
    const r = item.result;
    const card = element('article', undefined, 'result');
    const demo = r.source === 'LOCAL_DEMO';
    const sourceLabel = demo ? '本地虛構文件' : r.source === 'CURATED_LOCAL' ? '人工核對 Wikipedia 連結' : r.source;
    card.append(element('p', `${sourceLabel} · ${labels[r.category] || r.category}`, 'meta'));
    const heading = element('h2');
    const href = safeLink(r);
    const title = element(href ? 'a' : 'span', r.title);
    if (href) title.href = href;
    heading.append(title);
    card.append(heading, element('p', r.snippet));
    const details = element('details');
    details.append(element('summary', `排名分數 ${item.score.toFixed(1)} / 100 · 查看依據`));
    details.append(element('p', `關鍵字 K ${r.keywordRelevance.toFixed(1)} · 影集 M ${r.seriesRelevance.toFixed(1)} · 來源 S ${r.sourceRelevance.toFixed(1)} · 日期 F ${r.freshness.toFixed(1)}`));
    if (demo || r.source === 'CURATED_LOCAL') details.append(element('p',
      `K：查詢詞在索引欄位的覆蓋率；M：查詢詞在${demo ? '虛構劇名與示範別名' : '原名與有出處別名'}的覆蓋率。S/F 缺少可靠資料，以 0 分處理；不代表來源評價低。`));
    card.append(details);
    results.append(card);
  }
}
async function search(query = input.value.trim()) {
  activeRequest?.abort();
  const request = new AbortController();
  activeRequest = request;
  results.replaceChildren();
  if (!query) { status.textContent = '請輸入影集名稱或關鍵字。'; results.setAttribute('aria-busy', 'false'); return; }
  submittedQuery = query;
  results.setAttribute('aria-busy', 'true');
  status.textContent = '正在搜尋本地索引…';
  const params = new URLSearchParams({ q: query, limit: '10' });
  if (category) params.set('category', category);
  try {
    const response = await fetch('/api/search?' + params, { signal: request.signal });
    if (!response.ok) throw new Error('HTTP ' + response.status);
    const data = await response.json();
    if (activeRequest !== request) return;
    render(data.results);
    status.textContent = `${data.results.length} 筆結果。${data.message}`;
    if (!data.results.length && category) {
      const reset = element('button', '查看全部分類');
      reset.addEventListener('click', () => document.querySelector('[data-category=""]').click());
      results.append(reset);
    }
    if (!data.results.length) status.textContent += collectionExamples.length
      ? ` 可試試 ${collectionExamples.map(q => '「' + q + '」').join('、')}，或切換分類。`
      : ' 請檢查關鍵字或切換分類。';
  } catch (error) {
    if (activeRequest === request && error.name !== 'AbortError') status.textContent = '搜尋失敗，請確認服務是否啟動後重試。';
  } finally { if (activeRequest === request) results.setAttribute('aria-busy', 'false'); }
}
form.addEventListener('submit', event => { event.preventDefault(); search(); });
document.querySelectorAll('[data-category]').forEach(button => button.addEventListener('click', () => {
  category = button.dataset.category;
  document.querySelectorAll('[data-category]').forEach(b => b.setAttribute('aria-pressed', String(b === button)));
  if (submittedQuery) search(submittedQuery);
}));
const examples = document.querySelector('.examples');
examples.addEventListener('click', event => {
  const button = event.target.closest('[data-query]');
  if (button && examples.contains(button)) { input.value = button.dataset.query; search(); }
});
async function loadCollection() {
  const banner = document.querySelector('#collection-info');
  try {
    const response = await fetch('/api/collection');
    if (!response.ok) throw new Error('Collection unavailable');
    const data = await response.json();
    collectionExamples = data.examples;
    banner.textContent = data.message;
    examples.replaceChildren();
    if (data.examples.length) examples.append(document.createTextNode('試試：'));
    for (const query of data.examples) {
      const button = element('button', query);
      button.type = 'button'; button.dataset.query = query;
      examples.append(button);
    }
  } catch { banner.textContent = '無法讀取資料集設定；請依搜尋結果的來源標記判讀，或重新整理頁面。'; }
}
loadCollection();
