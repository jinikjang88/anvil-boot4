// ch06 — 송금 폼 + 시나리오 + ProblemDetail 응답 처리
// 핵심: 응답이 RFC 7807 ProblemDetail 형태로 오면 errors[] 의 field 를 따라
// 폼의 해당 입력칸 아래에 빨간 메시지를 채운다. 클라이언트가 "프로그래밍 가능" 한 에러를 받는 효용.

const API = 'http://localhost:8080/api/v1/transfers';

const $ = (id) => document.getElementById(id);

// ─── 시나리오 데이터 ────────────────────────────────────────────────────
const SCENARIOS = [
  {
    label: '정상 송금',
    data: { fromAccount: 'ACC-001', toAccount: 'ACC-002', amount: 10000, currency: 'KRW',
            memo: '월세', urgent: false, notifyEmail: '', scheduledAt: '' },
  },
  {
    label: '음수 금액',
    data: { fromAccount: 'ACC-001', toAccount: 'ACC-002', amount: -1000, currency: 'KRW',
            memo: '음수 송금 시도', urgent: false, notifyEmail: '', scheduledAt: '' },
  },
  {
    label: 'urgent + email 없음',
    data: { fromAccount: 'ACC-001', toAccount: 'ACC-002', amount: 50000, currency: 'KRW',
            memo: '긴급', urgent: true, notifyEmail: '', scheduledAt: '' },
  },
  {
    label: '잘못된 통화 (won)',
    data: { fromAccount: 'ACC-001', toAccount: 'ACC-002', amount: 5000, currency: 'won',
            memo: '', urgent: false, notifyEmail: '', scheduledAt: '' },
  },
  {
    label: '과거 날짜 예약',
    data: { fromAccount: 'ACC-001', toAccount: 'ACC-002', amount: 5000, currency: 'KRW',
            memo: '', urgent: false, notifyEmail: '', scheduledAt: '2020-01-01' },
  },
  {
    label: '전부 빈 값',
    data: { fromAccount: '', toAccount: '', amount: '', currency: '',
            memo: '', urgent: true, notifyEmail: 'not-an-email', scheduledAt: '' },
  },
];

const $scenarios = $('scenarios');
SCENARIOS.forEach((s) => {
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.textContent = s.label;
  btn.className = 'px-3 py-1 text-xs rounded border border-anvil-muted hover:bg-anvil-muted/10';
  btn.addEventListener('click', () => fillForm(s.data));
  $scenarios.appendChild(btn);
});

// ─── 폼 채우기 + 에러 클리어 ───────────────────────────────────────────
function fillForm(data) {
  for (const [name, value] of Object.entries(data)) {
    const el = document.querySelector(`[name="${name}"]`);
    if (!el) continue;
    if (el.type === 'checkbox') el.checked = Boolean(value);
    else el.value = value ?? '';
  }
  clearErrors();
}

function clearErrors() {
  document.querySelectorAll('.err').forEach((el) => { el.textContent = ''; });
  document.querySelectorAll('.field').forEach((el) => el.classList.remove('field-error'));
}

function setFieldError(field, message) {
  const err = document.querySelector(`[data-err-for="${field}"]`);
  if (err) err.textContent = message;
  // 같은 필드의 input 도 빨간 테두리
  const baseName = field.split('.').pop();
  const input = document.querySelector(`[name="${baseName}"]`);
  if (input) input.classList.add('field-error');
}

// ─── 송금 요청 ──────────────────────────────────────────────────────────
$('form').addEventListener('submit', async (event) => {
  event.preventDefault();
  clearErrors();

  const form = event.target;
  const payload = {
    fromAccount: form.fromAccount.value,
    toAccount:   form.toAccount.value,
    amount:      form.amount.value === '' ? null : Number(form.amount.value),
    currency:    form.currency.value,
    memo:        form.memo.value,
    options: {
      urgent:       form.urgent.checked,
      notifyEmail:  form.notifyEmail.value || null,
      scheduledAt:  form.scheduledAt.value || null,
    },
  };

  setStatus('호출 중…', 'muted');
  try {
    const res = await fetch(API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const data = await res.json().catch(() => ({}));
    renderResponse(res, data);

    if (res.ok) {
      await loadList();
    } else if (Array.isArray(data.errors)) {
      data.errors.forEach((e) => setFieldError(e.field, e.message));
    }
  } catch (err) {
    setStatus(`네트워크 에러: ${err.message}`, 'error');
  }
});

function renderResponse(res, data) {
  const kind = res.ok ? 'success' : 'error';
  setStatus(`HTTP ${res.status} ${res.statusText}`, kind);
  const $resp = $('response');
  $resp.classList.remove('hidden');
  $resp.textContent = JSON.stringify(data, null, 2);
}

function setStatus(text, kind) {
  const el = $('statusLine');
  el.textContent = text;
  el.classList.remove('text-anvil-muted', 'text-anvil-success', 'text-anvil-accent');
  if (kind === 'success')      el.classList.add('text-anvil-success');
  else if (kind === 'error')   el.classList.add('text-anvil-accent');
  else                         el.classList.add('text-anvil-muted');
}

// ─── 컬렉션 ─────────────────────────────────────────────────────────────
$('refreshList').addEventListener('click', loadList);

async function loadList() {
  try {
    const res = await fetch(API);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const list = await res.json();
    $('totalBadge').textContent = list.length;

    const $list = $('list');
    $list.innerHTML = '';
    for (const t of list) {
      const li = document.createElement('li');
      li.className = 'border-b border-anvil-muted/30 py-1';
      li.textContent = `${t.id.substring(0, 8)} · ${t.fromAccount} → ${t.toAccount} · ${t.amount} ${t.currency}${t.urgent ? ' [urgent]' : ''}`;
      $list.appendChild(li);
    }
  } catch (err) {
    $('list').innerHTML = `<li class="text-anvil-accent">${err.message}</li>`;
  }
}

// 초기화 — 정상 시나리오를 채우고 목록 한 번 로드
fillForm(SCENARIOS[0].data);
loadList();
