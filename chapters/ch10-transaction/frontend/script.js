// ch10-transaction — 4축 시연 프론트
const API = 'http://localhost:8080/api/v1/tx';
const INITIAL_TOTAL = 300000;
const $ = (id) => document.getElementById(id);

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// ── 계좌 현황 렌더 ───────────────────────────────────────────────────
function renderAccounts(accounts, total) {
  const bar = $('accountsBar');
  bar.innerHTML = accounts.map(a => `
    <div class="account-card">
      <div class="name">#${a.id} ${escapeHtml(a.owner)}</div>
      <div class="balance">${Number(a.balance).toLocaleString()}원</div>
      <div class="version">v${a.version}</div>
    </div>
  `).join('');

  const totalEl = $('totalBalance');
  const t = Number(total);
  totalEl.textContent = t.toLocaleString() + '원';
  totalEl.className = t === INITIAL_TOTAL ? 'conservation-ok' : 'conservation-fail';
}

// ── 결과 렌더 ────────────────────────────────────────────────────────
function renderResult(res) {
  $('resultPanel').style.display = '';
  $('resultScenario').textContent = res.scenario;
  $('resultMessage').textContent = res.message;
  $('resultMs').textContent = res.elapsedMillis;

  const badge = $('resultBadge');
  badge.textContent = res.success ? 'SUCCESS' : 'FAIL';
  badge.className = 'text-sm font-bold px-2 py-0.5 rounded ' + (res.success ? 'badge-ok' : 'badge-fail');

  renderAccounts(res.accounts, res.totalBalance);

  const logsSection = $('resultLogs');
  if (res.logs && res.logs.length > 0) {
    logsSection.style.display = '';
    $('logList').innerHTML = res.logs.map(l =>
      `<li><b>${escapeHtml(l.scenario)}</b>: ${escapeHtml(l.message)}</li>`
    ).join('');
  } else {
    logsSection.style.display = 'none';
  }
}

// ── API 호출 ──────────────────────────────────────────────────────────
async function callScenario(path, withBody) {
  const opts = { method: 'POST', headers: { 'Content-Type': 'application/json' } };
  if (withBody) {
    opts.body = JSON.stringify({
      fromId: parseInt($('fromId').value, 10),
      toId:   parseInt($('toId').value, 10),
      amount: parseInt($('amount').value, 10),
    });
  }
  const res = await fetch(API + path, opts);
  return res.json();
}

// ── 이벤트 ───────────────────────────────────────────────────────────
document.querySelectorAll('.scenario-btn').forEach(btn => {
  btn.addEventListener('click', async () => {
    const url = btn.dataset.url;
    const noBody = btn.classList.contains('no-body');
    try {
      const data = await callScenario(url, !noBody);
      renderResult(data);
    } catch (e) {
      $('resultPanel').style.display = '';
      $('resultScenario').textContent = 'ERROR';
      $('resultMessage').textContent = e.message;
      $('resultBadge').textContent = 'ERROR';
      $('resultBadge').className = 'text-sm font-bold px-2 py-0.5 rounded badge-fail';
    }
  });
});

$('btnReset').addEventListener('click', async () => {
  const data = await callScenario('/reset', false);
  renderResult(data);
});

// 초기 로드
(async () => {
  try {
    const res = await fetch(API + '/accounts');
    const data = await res.json();
    renderAccounts(data.accounts, data.totalBalance);
  } catch (e) { /* 백엔드 미실행 */ }
})();
