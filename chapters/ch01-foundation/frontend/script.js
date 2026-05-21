// ch01-foundation — GET /api/hello 호출 데모
// 핵심 학습 포인트:
//  - fetch 만으로 백엔드 호출 (프레임워크 없음)
//  - 정상/에러 상태를 오행 컬러로 구분 (木 성공 / 火 에러 / 金 대기)

const API = 'http://localhost:8080/api/hello';

const $name = document.getElementById('name');
const $btn = document.getElementById('call');
const $status = document.getElementById('status');
const $result = document.getElementById('result');

async function callHello() {
  const name = $name.value.trim();
  const url = name ? `${API}?name=${encodeURIComponent(name)}` : API;

  setBusy(true);
  setStatus('호출 중…', 'muted');
  hideResult();

  try {
    const res = await fetch(url, { method: 'GET' });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status} ${res.statusText}`);
    }
    const json = await res.json();
    setStatus(`성공 · ${url}`, 'success');
    showResult(json);
  } catch (err) {
    // CORS 차단, 백엔드 미기동 등 — 사용자에게 원인 힌트를 같이 보여준다.
    setStatus(
      `에러: ${err.message} (백엔드가 8080 에 떠 있는지, local 프로필인지 확인)`,
      'error'
    );
  } finally {
    setBusy(false);
  }
}

function setBusy(busy) {
  $btn.disabled = busy;
  $btn.textContent = busy ? '호출 중…' : '호출';
}

function setStatus(text, kind) {
  $status.textContent = text;
  $status.classList.remove(
    'text-anvil-muted',
    'text-anvil-success',
    'text-anvil-accent'
  );
  if (kind === 'success') {
    $status.classList.add('text-anvil-success');
  } else if (kind === 'error') {
    $status.classList.add('text-anvil-accent');
  } else {
    $status.classList.add('text-anvil-muted');
  }
}

function showResult(json) {
  $result.textContent = JSON.stringify(json, null, 2);
  $result.classList.remove('hidden');
}

function hideResult() {
  $result.classList.add('hidden');
  $result.textContent = '';
}

$btn.addEventListener('click', callHello);
$name.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') {
    event.preventDefault();
    callHello();
  }
});
