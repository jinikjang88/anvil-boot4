// ch08-jpa — 좌(LAZY) vs 우(FETCH) 비교 데모.
// 응답 봉투의 sqlLogs / sqlCount / elapsedMillis 를 화면에 반영.

const API = 'http://localhost:8080/api/v1/posts';
const KW = /\b(select|insert|update|delete|from|where|join|left|inner|distinct|on|order\s+by|limit)\b/gi;

const $ = (id) => document.getElementById(id);

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ));
}

function highlight(sql) {
  // 키워드 강조 — XSS 방지 위해 escape 후 mark.
  return escapeHtml(sql).replace(KW, (m) => `<span class="sql-kw">${m}</span>`);
}

function renderEnvelope(envelope, listEl, countEl, msEl) {
  countEl.textContent = String(envelope.sqlCount);
  msEl.textContent = String(envelope.elapsedMillis);
  listEl.innerHTML = '';
  for (const sql of envelope.sqlLogs) {
    const li = document.createElement('li');
    li.innerHTML = highlight(sql);
    listEl.appendChild(li);
  }
}

async function call(path) {
  const res = await fetch(`${API}${path}`);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json();
}

$('btnLazy').addEventListener('click', async () => {
  try {
    const data = await call('/lazy');
    renderEnvelope(data, $('lazySql'), $('lazyCount'), $('lazyMs'));
  } catch (e) {
    $('lazySql').innerHTML = `<li style="color:#C0392B">실패: ${e.message}</li>`;
  }
});

$('btnFetch').addEventListener('click', async () => {
  try {
    const data = await call('/fetch');
    renderEnvelope(data, $('fetchSql'), $('fetchCount'), $('fetchMs'));
  } catch (e) {
    $('fetchSql').innerHTML = `<li style="color:#C0392B">실패: ${e.message}</li>`;
  }
});

$('postForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  const res = await fetch(API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: f.title.value, content: f.content.value }),
  });
  const body = await res.json();
  const pre = $('postResp');
  pre.classList.remove('hidden');
  pre.textContent = `${res.status}\n` + JSON.stringify(body, null, 2);
  if (res.ok) f.reset();
});

$('commentForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  const res = await fetch(`${API}/${encodeURIComponent(f.postId.value)}/comments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ author: f.author.value, body: f.body.value }),
  });
  const body = await res.json();
  const pre = $('commentResp');
  pre.classList.remove('hidden');
  pre.textContent = `${res.status}\n` + JSON.stringify(body, null, 2);
  if (res.ok) f.reset();
});
