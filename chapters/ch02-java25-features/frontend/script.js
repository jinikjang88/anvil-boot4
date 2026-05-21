// ch02 — Platform vs Virtual Thread 비교 데모
// 1) 두 엔드포인트를 sequentially 호출 (병렬로 띄우면 같은 JVM 자원을 경쟁해 측정이 흐려진다)
// 2) Chart.js 막대 차트로 elapsed 비교
// 3) 색상은 CSS 변수에서 읽어 단일 출처 유지

const API_BASE = 'http://localhost:8080/api/orders';
const SAMPLE_LIMIT = 10;

const $ = (id) => document.getElementById(id);

const $count           = $('count');
const $countLabel      = $('countLabel');
const $run             = $('run');
const $status          = $('status');
const $results         = $('results');
const $platformCounts  = $('platformCounts');
const $virtualCounts   = $('virtualCounts');
const $platformSamples = $('platformSamples');
const $virtualSamples  = $('virtualSamples');

const palette = getComputedStyleVars(['--anvil-accent', '--anvil-success']);

const chart = new Chart($('chart').getContext('2d'), {
  type: 'bar',
  data: {
    labels: ['Platform (10 thread pool)', 'Virtual (per task)'],
    datasets: [{
      label: 'elapsed (ms)',
      data: [0, 0],
      backgroundColor: [palette['--anvil-accent'], palette['--anvil-success']],
    }],
  },
  options: {
    indexAxis: 'y',
    scales: { x: { beginAtZero: true, title: { display: true, text: 'milliseconds' } } },
    plugins: { legend: { display: false } },
  },
});

$count.addEventListener('input', () => {
  $countLabel.textContent = $count.value;
});

$run.addEventListener('click', run);

async function run() {
  const count = Number($count.value);
  setBusy(true);
  setStatus('Platform 호출 중…', 'muted');

  try {
    const platform = await callBatch('platform', count);
    setStatus('Virtual 호출 중…', 'muted');
    const virtual  = await callBatch('virtual', count);

    chart.data.datasets[0].data = [platform.elapsedMs, virtual.elapsedMs];
    chart.update();
    renderResults(platform, virtual);

    const ratio = (platform.elapsedMs / Math.max(virtual.elapsedMs, 1)).toFixed(1);
    setStatus(
      `완료 — Platform ${platform.elapsedMs}ms · Virtual ${virtual.elapsedMs}ms (≈ ${ratio}× 차이)`,
      'success'
    );
  } catch (err) {
    setStatus(
      `에러: ${err.message} (백엔드가 8080 에 떠 있는지, local 프로필인지 확인)`,
      'error'
    );
  } finally {
    setBusy(false);
  }
}

async function callBatch(mode, count) {
  const res = await fetch(`${API_BASE}/process-${mode}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ count }),
  });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status} ${res.statusText} ${body}`);
  }
  return res.json();
}

function renderResults(platform, virtual) {
  $results.classList.remove('hidden');
  $platformCounts.textContent  = formatCounts(platform.counts);
  $virtualCounts.textContent   = formatCounts(virtual.counts);
  $platformSamples.textContent = platform.samples.slice(0, SAMPLE_LIMIT).join('\n');
  $virtualSamples.textContent  = virtual.samples.slice(0, SAMPLE_LIMIT).join('\n');
}

function formatCounts(counts) {
  return `OK ${counts.approved} · NG ${counts.rejected} · REV ${counts.pending}`;
}

function setBusy(busy) {
  $run.disabled = busy;
  $run.textContent = busy ? '실행 중…' : '비교 실행';
}

function setStatus(text, kind) {
  $status.textContent = text;
  $status.classList.remove('text-anvil-muted', 'text-anvil-success', 'text-anvil-accent');
  if (kind === 'success')      $status.classList.add('text-anvil-success');
  else if (kind === 'error')   $status.classList.add('text-anvil-accent');
  else                         $status.classList.add('text-anvil-muted');
}

function getComputedStyleVars(names) {
  const style = getComputedStyle(document.documentElement);
  return Object.fromEntries(names.map((n) => [n, style.getPropertyValue(n).trim()]));
}
