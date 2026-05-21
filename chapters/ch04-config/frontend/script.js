// ch04-config — 활성 설정 노출 + 키 추적기 + 환경 비교 + rate limit 시연

const API = 'http://localhost:8080';
const $ = (id) => document.getElementById(id);

// ─── 환경 비교 표 (정적 데이터 — application-{profile}.yml 의 값을 정리) ─────────
const PROFILE_TABLE = [
  { key: 'environment-label',         local: '"로컬 (개발)"',         dev: '"개발 서버 (dev)"',          prod: '"운영 (prod)"' },
  { key: 'rate-limit.max-per-minute', local: '-1 (무제한)',           dev: '100',                       prod: '10' },
  { key: 'sender.base-url',           local: 'localhost mock',         dev: 'sandbox-api.anvil.dev',     prod: 'api.anvil.run' },
  { key: 'sender.api-key',            local: 'secret 파일 / placeholder', dev: '환경변수 주입',         prod: 'Secrets Manager / KMS' },
  { key: 'management.endpoints.web',  local: 'health,info,env',        dev: 'health,info,env',           prod: 'health,info (env 차단)' },
];

// ─── 활성 설정 로드 ────────────────────────────────────────────────────────
async function loadActive() {
  const res = await fetch(`${API}/api/config/active`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();

  const activeProfile = data.activeProfiles[0] || 'unknown';
  $('profileBadge').textContent = activeProfile;

  renderActiveTable(data);
  renderCompareTable(activeProfile);
}

function renderActiveTable(data) {
  const rows = [
    ['anvil.name', data.name],
    ['anvil.version', data.version],
    ['anvil.environment-label', data.environmentLabel],
    ['anvil.rate-limit.max-per-minute',
      data.rateLimit.maxPerMinute < 0 ? '-1 (무제한)' : data.rateLimit.maxPerMinute],
    ['anvil.rate-limit.window-seconds', data.rateLimit.windowSeconds],
    ['anvil.sender.base-url', data.sender.baseUrl],
    ['anvil.sender.api-key', `${data.sender.apiKey}  (masked)`],
  ];

  const tbody = $('activeTable');
  tbody.innerHTML = '';
  for (const [k, v] of rows) {
    const tr = document.createElement('tr');
    tr.className = 'border-b border-anvil-muted/30';

    const td1 = document.createElement('td');
    td1.className = 'py-1 pr-3 text-anvil-muted whitespace-nowrap';
    td1.textContent = k;

    const td2 = document.createElement('td');
    td2.className = 'py-1';
    td2.textContent = String(v);

    tr.append(td1, td2);
    tbody.appendChild(tr);
  }
}

function renderCompareTable(activeProfile) {
  const tbody = $('compareTable');
  tbody.innerHTML = '';
  for (const row of PROFILE_TABLE) {
    const tr = document.createElement('tr');
    tr.className = 'border-b border-anvil-muted/30';

    const tdKey = document.createElement('td');
    tdKey.className = 'py-1 pr-3 text-anvil-muted';
    tdKey.textContent = `anvil.${row.key}`;
    tr.appendChild(tdKey);

    for (const p of ['local', 'dev', 'prod']) {
      const td = document.createElement('td');
      td.className = 'py-1 pr-3';
      td.textContent = row[p];
      if (p === activeProfile) {
        td.classList.add('font-bold', 'text-anvil-success');
      }
      tr.appendChild(td);
    }
    tbody.appendChild(tr);
  }
}

// ─── 키 출처 추적 ──────────────────────────────────────────────────────────
async function traceKey(key) {
  const result = $('traceResult');
  result.classList.remove('hidden');
  result.textContent = '추적 중…';

  try {
    const res = await fetch(`${API}/api/config/source?key=${encodeURIComponent(key)}`);
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
    result.textContent = JSON.stringify(data, null, 2);
  } catch (err) {
    result.textContent = `에러: ${err.message}`;
  }
}

$('traceBtn').addEventListener('click', () => traceKey($('keyInput').value.trim()));
$('keyInput').addEventListener('keydown', (e) => {
  if (e.key === 'Enter') traceKey($('keyInput').value.trim());
});
document.querySelectorAll('.trace-quick').forEach((btn) => {
  btn.addEventListener('click', () => {
    const k = btn.dataset.key;
    $('keyInput').value = k;
    traceKey(k);
  });
});

// ─── 연속 발송 (rate limit 시연) ────────────────────────────────────────────
async function burst() {
  const recipient = $('recipient').value.trim();
  const repeat = Math.max(1, Math.min(200, Number($('repeat').value) || 1));

  const result = $('burstResult');
  result.classList.remove('hidden');
  result.innerHTML = '';
  $('burst').disabled = true;

  let ok = 0;
  let blocked = 0;
  for (let i = 0; i < repeat; i++) {
    const res = await fetch(`${API}/api/notify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ recipient, message: `burst #${i + 1}` }),
    });
    const data = await res.json().catch(() => ({}));
    const line = document.createElement('div');
    if (res.ok) {
      ok++;
      line.className = 'text-anvil-success';
      line.textContent = `#${i + 1}  200 OK  count=${data.currentCount ?? '?'}`;
    } else {
      blocked++;
      line.className = 'text-anvil-accent';
      line.textContent = `#${i + 1}  ${res.status}  ${data.error || ''} (max=${data.maxPerMinute ?? '?'})`;
    }
    result.appendChild(line);
  }

  const summary = document.createElement('div');
  summary.className = 'mt-2 pt-2 border-t border-anvil-muted text-anvil-muted';
  summary.textContent = `총 ${repeat}건 — 성공 ${ok}건, 차단 ${blocked}건`;
  result.appendChild(summary);

  $('burst').disabled = false;
}

$('burst').addEventListener('click', burst);
$('refreshActive').addEventListener('click', loadActive);

// 초기 로드
loadActive().catch((err) => {
  $('profileBadge').textContent = '에러';
  $('activeTable').innerHTML =
    `<tr><td colspan="2" class="text-anvil-accent">${err.message}</td></tr>`;
});
