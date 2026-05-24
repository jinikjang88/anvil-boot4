// ch09b-jooq — 5가지 jOOQ 기능을 각각 호출하고 응답 봉투를 렌더한다.

const API = 'http://localhost:8080/api/v1/jooq';
const KW = /\b(with\s+recursive|with|select|insert|update|delete|from|where|join|left|inner|distinct|on|order\s+by|partition\s+by|over|union\s+all|union|limit|offset|and|or|not|exists|lower|like|values|conflict|do|excluded|returning)\b/gi;

const $ = (id) => document.getElementById(id);

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ));
}

function highlight(sql) {
  return escapeHtml(sql).replace(KW, (m) => `<span class="sql-kw">${m}</span>`);
}

function renderSqlInto(listEl, sqlLogs) {
  listEl.innerHTML = '';
  if (!sqlLogs || sqlLogs.length === 0) {
    listEl.innerHTML = '<li class="empty">(no SQL captured)</li>';
    return;
  }
  for (const sql of sqlLogs) {
    const li = document.createElement('li');
    li.innerHTML = highlight(sql);
    listEl.appendChild(li);
  }
}

// ───────── ① 검색 ─────────

$('btnSearch').addEventListener('click', async () => {
  const f = $('searchForm');
  const data = new FormData(f);
  const params = new URLSearchParams();
  const keyword = data.get('keyword')?.trim();
  const author  = data.get('author')?.trim();
  const from    = data.get('from');
  const to      = data.get('to');
  const hasC    = data.get('hasComments');
  if (keyword) params.set('keyword', keyword);
  if (author)  params.set('author',  author);
  if (from)    params.set('from', new Date(from).toISOString());
  if (to)      params.set('to',   new Date(to).toISOString());
  if (hasC)    params.set('hasComments', hasC);

  const res = await fetch(`${API}/search?${params}`);
  const env = await res.json();

  $('searchCount').textContent = env.sqlCount ?? '-';
  $('searchMs').textContent    = env.elapsedMillis ?? '-';
  $('searchTotal').textContent = env.totalElements ?? '-';
  renderSqlInto($('searchSql'), env.sqlLogs);
  const dataEl = $('searchData');
  dataEl.innerHTML = '';
  if (!env.data || env.data.length === 0) {
    dataEl.innerHTML = '<li class="empty">결과 없음</li>';
  } else {
    for (const p of env.data) {
      const li = document.createElement('li');
      li.innerHTML = `<div><b>#${p.id}</b> ${escapeHtml(p.title)}</div>
                      <div class="meta">by ${escapeHtml(p.author)} · ${new Date(p.createdAt).toLocaleString()}</div>`;
      dataEl.appendChild(li);
    }
  }
});

// ───────── ② 윈도우 함수 ─────────

$('btnRanking').addEventListener('click', async () => {
  const topN = $('rankTopN').value;
  const res = await fetch(`${API}/ranking?topN=${encodeURIComponent(topN)}`);
  const env = await res.json();

  $('rankCount').textContent = env.sqlCount ?? '-';
  $('rankMs').textContent    = env.elapsedMillis ?? '-';
  renderSqlInto($('rankSql'), env.sqlLogs);

  const tbl = $('rankData');
  tbl.innerHTML = `
    <thead><tr><th>rn</th><th>author</th><th>title</th><th>created</th></tr></thead>
    <tbody>${(env.data || []).map(r => `
      <tr><td class="rn">${r.rankInAuthor}</td>
          <td>${escapeHtml(r.author)}</td>
          <td>${escapeHtml(r.title)}</td>
          <td class="meta">${new Date(r.createdAt).toLocaleString()}</td></tr>
    `).join('')}</tbody>`;
});

// ───────── ③ WITH RECURSIVE ─────────

$('btnTree').addEventListener('click', async () => {
  const postId = $('treePostId').value;
  const res = await fetch(`${API}/tree/${encodeURIComponent(postId)}`);
  const env = await res.json();

  $('treeCount').textContent = env.sqlCount ?? '-';
  $('treeMs').textContent    = env.elapsedMillis ?? '-';
  $('treeTotal').textContent = env.totalNodes ?? '-';
  $('treeDepth').textContent = env.maxDepth ?? '-';
  renderSqlInto($('treeSql'), env.sqlLogs);

  const listEl = $('treeData');
  listEl.innerHTML = '';
  if (!env.data || env.data.length === 0) {
    listEl.innerHTML = '<li class="empty">댓글 없음</li>';
    return;
  }
  for (const node of env.data) {
    const li = document.createElement('li');
    li.className = `depth-${node.depth}`;
    li.innerHTML = `<div><b>${escapeHtml(node.author)}</b>: ${escapeHtml(node.body)}</div>
                    <div class="meta">id=${node.id} · parent=${node.parentId ?? 'null'} · depth=${node.depth}</div>`;
    listEl.appendChild(li);
  }
});

// ───────── ④ Upsert ─────────

$('btnUpsert').addEventListener('click', async () => {
  const f = $('upsertForm');
  const body = {
    title:   f.title.value,
    content: f.content.value,
    author:  f.author.value,
  };
  const res = await fetch(`${API}/upsert`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const env = await res.json();

  $('upsertCount').textContent = env.sqlCount ?? '-';
  $('upsertMs').textContent    = env.elapsedMillis ?? '-';
  $('upsertId').textContent    = env.id ?? '-';
  renderSqlInto($('upsertSql'), env.sqlLogs);
});

// ───────── ⑤ Bulk INSERT ─────────

$('btnBulk').addEventListener('click', async () => {
  const postId = parseInt($('bulkPostId').value, 10);
  const count  = parseInt($('bulkCount').value, 10);
  const comments = Array.from({ length: count }, (_, i) => ({
    author: `bot-${i}`,
    body:   `자동 생성 댓글 #${i + 1}`,
  }));

  const res = await fetch(`${API}/bulk-comments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ postId, comments }),
  });
  const env = await res.json();

  $('bulkSqlCount').textContent = env.sqlCount ?? '-';
  $('bulkMs').textContent       = env.elapsedMillis ?? '-';
  $('bulkRows').textContent     = env.rowsInserted ?? '-';
  renderSqlInto($('bulkSql'), env.sqlLogs);
});
