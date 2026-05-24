// ch09-querydsl — 세 가지 검색 호출 (JPQL / Builder / Expression) 을 한 번에 실행하고
// 각 응답 봉투의 sqlLogs / sqlCount / elapsedMillis / data 를 사이드바이사이드로 렌더한다.

const API = 'http://localhost:8080/api/v1/posts';
const KW = /\b(select|insert|update|delete|from|where|join|left|inner|distinct|on|order\s+by|limit|offset|and|or|not|exists|lower|like)\b/gi;

const $ = (id) => document.getElementById(id);

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ));
}

function highlight(sql) {
  return escapeHtml(sql).replace(KW, (m) => `<span class="sql-kw">${m}</span>`);
}

// ───────── 검색 조건 수집 ─────────

function readForm() {
  const f = $('searchForm');
  const data = new FormData(f);
  const params = new URLSearchParams();
  // 빈 값은 보내지 않음 — 백엔드의 null 처리를 검증하기 위함.
  const keyword = data.get('keyword')?.trim();
  const author  = data.get('author')?.trim();
  const from    = data.get('from');
  const to      = data.get('to');
  const hasC    = data.get('hasComments');

  if (keyword) params.set('keyword', keyword);
  if (author)  params.set('author',  author);
  // datetime-local 은 "YYYY-MM-DDTHH:mm" → ISO 변환 (로컬 → UTC).
  if (from)    params.set('from', new Date(from).toISOString());
  if (to)      params.set('to',   new Date(to).toISOString());
  if (hasC)    params.set('hasComments', hasC);

  return params;
}

// ───────── 렌더 ─────────

function renderEnvelope(env, prefix) {
  $(`${prefix}Count`).textContent = String(env.sqlCount);
  $(`${prefix}Ms`).textContent    = String(env.elapsedMillis);
  $(`${prefix}Total`).textContent = String(env.totalElements);

  const sqlEl = $(`${prefix}Sql`);
  sqlEl.innerHTML = '';
  for (const sql of env.sqlLogs) {
    const li = document.createElement('li');
    li.innerHTML = highlight(sql);
    sqlEl.appendChild(li);
  }
  if (env.sqlLogs.length === 0) {
    sqlEl.innerHTML = '<li class="empty">(no SQL captured)</li>';
  }

  const dataEl = $(`${prefix}Data`);
  dataEl.innerHTML = '';
  if (env.data.length === 0) {
    dataEl.innerHTML = '<li class="empty">결과 없음</li>';
    return;
  }
  for (const p of env.data) {
    const li = document.createElement('li');
    const t  = document.createElement('div');
    t.className = 'title';
    t.textContent = `#${p.id} ${p.title}`;
    const m  = document.createElement('div');
    m.className = 'meta';
    const ts = new Date(p.createdAt).toLocaleString();
    m.textContent = `by ${p.author} · ${ts} · 💬 ${p.commentCount}`;
    li.appendChild(t);
    li.appendChild(m);
    dataEl.appendChild(li);
  }
}

async function call(path, params) {
  const url = params.toString() ? `${API}${path}?${params}` : `${API}${path}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json();
}

async function runAll() {
  const base = readForm();

  // builder / expression 만 page/size 받으니 미리 세팅.
  const paged = new URLSearchParams(base);
  paged.set('page', '0');
  paged.set('size', '20');

  // 병렬 호출 — 학습 의도: 세 호출이 같은 결과를 내는지 시각적으로 비교.
  const [jpql, builder, expr] = await Promise.all([
    call('/search/jpql', base).catch(e => errorEnvelope(e.message)),
    call('/search/builder', paged).catch(e => errorEnvelope(e.message)),
    call('/search/expression', paged).catch(e => errorEnvelope(e.message)),
  ]);

  renderEnvelope(jpql,    'jpql');
  renderEnvelope(builder, 'builder');
  renderEnvelope(expr,    'expr');
}

function errorEnvelope(msg) {
  return { sqlCount: 0, elapsedMillis: 0, totalElements: 0, sqlLogs: [msg], data: [] };
}

// ───────── 이벤트 ─────────

$('btnRun').addEventListener('click', (e) => { e.preventDefault(); runAll(); });
$('searchForm').addEventListener('submit', (e) => { e.preventDefault(); runAll(); });
$('btnReset').addEventListener('click', () => $('searchForm').reset());

$('postForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  const res = await fetch(API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: f.title.value, content: f.content.value, author: f.author.value }),
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

// 초기 자동 호출 — 시드 데이터 보기.
runAll();
