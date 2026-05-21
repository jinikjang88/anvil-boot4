// ch05 — REST 메뉴판 시뮬레이터
// 5개 메서드 카드 + 현재 컬렉션 표. 호출하면 응답을 카드 안에 표시하고,
// 컬렉션은 GET /api/v1/orders 로 다시 가져와 갱신한다.

const API = 'http://localhost:8080/api/v1/orders';

// id placeholder 는 가장 최근에 만든 주문 id 로 자동 채워진다 (편의용)
let lastId = null;

// ─── 카드 정의 (메뉴판) ───────────────────────────────────────────────
const MENU = [
  {
    method: 'POST',
    path: '',
    label: '주문 생성',
    body: {
      customer: 'alice',
      items: [{ sku: 'sku-1', name: '도토리', quantity: 2, price: 1500 }],
      memo: '메모',
    },
    expect: '201 Created + Location 헤더',
  },
  {
    method: 'GET',
    path: '?page=0&size=10',
    label: '목록 조회 (+ status 필터 가능)',
    body: null,
    expect: '200 + 페이지 메타',
  },
  {
    method: 'GET',
    path: '/{id}',
    label: '단건 조회',
    body: null,
    expect: '200 또는 404',
  },
  {
    method: 'PATCH',
    path: '/{id}',
    label: '부분 수정 (memo)',
    body: { memo: '변경된 메모' },
    expect: '200 또는 404',
  },
  {
    method: 'PUT',
    path: '/{id}/status',
    label: '상태 전이',
    body: { status: 'CONFIRMED' },
    expect: '200 / 404 / 409',
  },
  {
    method: 'DELETE',
    path: '/{id}',
    label: '삭제',
    body: null,
    expect: '204 또는 404',
  },
];

const METHOD_COLOR = {
  GET:    'bg-anvil-data',
  POST:   'bg-anvil-success',
  PUT:    'bg-anvil-accent',
  PATCH:  'bg-anvil-accent',
  DELETE: 'bg-anvil-fg',
};

// ─── 메뉴판 렌더 ───────────────────────────────────────────────────────
const $menu = document.getElementById('menu');

MENU.forEach((entry, idx) => {
  const card = document.createElement('div');
  card.className = 'card p-4';
  card.innerHTML = `
    <div class="flex items-baseline gap-2 mb-2">
      <span class="px-2 py-0.5 text-xs font-bold text-white rounded ${METHOD_COLOR[entry.method]}">${entry.method}</span>
      <code class="font-mono text-sm">/api/v1/orders<span data-path>${entry.path}</span></code>
    </div>
    <p class="text-xs text-anvil-muted mb-2">${entry.label} · <span class="text-anvil-muted">예상: ${entry.expect}</span></p>
    ${entry.body
        ? `<textarea data-body class="w-full font-mono text-xs border border-anvil-muted rounded p-2 mb-2" rows="${entry.method === 'POST' ? 5 : 2}">${JSON.stringify(entry.body, null, 2)}</textarea>`
        : ''}
    <button data-run class="bg-anvil-accent text-white px-3 py-1.5 rounded text-sm hover:opacity-90">호출</button>
    <pre data-response class="hidden mt-2 p-2 bg-white border border-anvil-muted rounded text-xs font-mono overflow-x-auto"></pre>
  `;
  $menu.appendChild(card);

  const $path = card.querySelector('[data-path]');
  const $body = card.querySelector('[data-body]');
  const $run  = card.querySelector('[data-run]');
  const $resp = card.querySelector('[data-response]');

  $run.addEventListener('click', async () => {
    let path = entry.path;
    if (path.includes('{id}')) {
      if (!lastId) {
        showResponse($resp, '먼저 POST 로 주문을 하나 만들어 주세요.', 'muted');
        return;
      }
      path = path.replace('{id}', lastId);
      $path.textContent = path;
    }

    $run.disabled = true;
    $run.textContent = '호출 중…';

    try {
      const opts = { method: entry.method, headers: {} };
      if ($body) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = $body.value;
      }
      const res = await fetch(`${API}${path}`, opts);

      const headerLine = `${res.status} ${res.statusText}`;
      const location = res.headers.get('Location');
      const text = await res.text();
      let parsed = text;
      try { parsed = JSON.stringify(JSON.parse(text), null, 2); } catch { /* not json (e.g. empty 204) */ }

      const kind = res.ok ? 'success' : 'error';
      const out = [
        `# ${headerLine}`,
        location ? `Location: ${location}` : null,
        parsed ? '' : null,
        parsed,
      ].filter((l) => l !== null).join('\n');
      showResponse($resp, out, kind);

      // 201 면 id 캡처
      if (res.status === 201) {
        try {
          const json = JSON.parse(text);
          lastId = json.id;
        } catch { /* ignore */ }
      }

      await loadOrders();
    } catch (err) {
      showResponse($resp, `# ERROR\n${err.message}`, 'error');
    } finally {
      $run.disabled = false;
      $run.textContent = '호출';
    }
  });
});

function showResponse($el, text, kind) {
  $el.classList.remove('hidden', 'text-anvil-success', 'text-anvil-accent', 'text-anvil-muted');
  if (kind === 'success')      $el.classList.add('text-anvil-success');
  else if (kind === 'error')   $el.classList.add('text-anvil-accent');
  else                         $el.classList.add('text-anvil-muted');
  $el.textContent = text;
}

// ─── 컬렉션 ────────────────────────────────────────────────────────────
const $body = document.getElementById('ordersBody');
const $total = document.getElementById('totalBadge');
document.getElementById('refreshList').addEventListener('click', loadOrders);

async function loadOrders() {
  try {
    const res = await fetch(`${API}?page=0&size=50`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const page = await res.json();
    $total.textContent = page.totalElements;

    $body.innerHTML = '';
    for (const order of page.items) {
      const tr = document.createElement('tr');
      tr.className = 'border-b border-anvil-muted/30';
      tr.innerHTML = `
        <td class="py-1 pr-2">${order.id.substring(0, 8)}</td>
        <td class="py-1 pr-2">${order.customer}</td>
        <td class="py-1 pr-2 status-${order.status}">${order.status}</td>
        <td class="py-1 pr-2 text-right">${order.totalAmount}</td>
        <td class="py-1 truncate max-w-xs" title="${order.memo}">${order.memo || ''}</td>
      `;
      $body.appendChild(tr);
    }
  } catch (err) {
    $body.innerHTML = `<tr><td colspan="5" class="text-anvil-accent py-1">${err.message}</td></tr>`;
  }
}

loadOrders();
