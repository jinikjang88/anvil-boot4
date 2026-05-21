// ch03 — 알림 발송 + D3 의존성 그래프
// 색상 정의는 style.css 의 CSS 변수에서 일관되게 읽어 single source of truth 유지.

const API = 'http://localhost:8080';
const $ = (id) => document.getElementById(id);

const palette = (() => {
  const s = getComputedStyle(document.documentElement);
  const v = (name) => s.getPropertyValue(name).trim();
  return {
    accent:  v('--anvil-accent'),   // 火
    data:    v('--anvil-data'),     // 水
    success: v('--anvil-success'),  // 木
    muted:   v('--anvil-muted'),    // 金
    fg:      v('--anvil-fg'),
  };
})();

// 노드 kind → 색. 오행 5색을 모두 사용한다.
const colorByKind = {
  controller: palette.fg,
  service:    palette.data,
  component:  palette.success,
  config:     palette.accent,
  external:   palette.muted,
};

const kindLabel = {
  controller: '컨트롤러',
  service:    '서비스',
  component:  '컴포넌트',
  config:     '컨피그',
  external:   '외부',
};

// ─── 알림 발송 ─────────────────────────────────────────────────────
$('send').addEventListener('click', sendNotification);

async function sendNotification() {
  const payload = {
    recipient: $('recipient').value.trim(),
    subject:   $('subject').value.trim(),
    body:      $('body').value.trim(),
  };

  setBusy(true);
  setStatus('발송 중…', 'muted');
  hideResult();

  try {
    const res = await fetch(`${API}/api/notify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.error || `HTTP ${res.status}`);
    }
    setStatus(`전송 완료 — ${json.channel.toUpperCase()} 채널`, 'success');
    showResult(json);
  } catch (err) {
    setStatus(`에러: ${err.message}`, 'error');
  } finally {
    setBusy(false);
  }
}

function setBusy(busy) {
  $('send').disabled = busy;
  $('send').textContent = busy ? '발송 중…' : '발송';
}

function setStatus(text, kind) {
  const el = $('status');
  el.textContent = text;
  el.classList.remove('text-anvil-muted', 'text-anvil-success', 'text-anvil-accent');
  if (kind === 'success')     el.classList.add('text-anvil-success');
  else if (kind === 'error')  el.classList.add('text-anvil-accent');
  else                        el.classList.add('text-anvil-muted');
}

function showResult(json) {
  const el = $('result');
  el.textContent = JSON.stringify(json, null, 2);
  el.classList.remove('hidden');
}

function hideResult() {
  $('result').classList.add('hidden');
  $('result').textContent = '';
}

// ─── 빈 그래프 ─────────────────────────────────────────────────────
$('refresh').addEventListener('click', loadGraph);
loadGraph();
renderLegend();

async function loadGraph() {
  try {
    const res = await fetch(`${API}/api/beans/graph`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    renderGraph(data);
  } catch (err) {
    d3.select('#graph').selectAll('*').remove();
    d3.select('#graph')
      .append('text')
      .attr('x', 20).attr('y', 30)
      .attr('fill', palette.accent)
      .text(`그래프 로드 실패: ${err.message}`);
  }
}

function renderGraph(data) {
  const width = 800;
  const height = 480;

  const svg = d3.select('#graph')
    .attr('viewBox', [-width / 2, -height / 2, width, height])
    .attr('preserveAspectRatio', 'xMidYMid meet');
  svg.selectAll('*').remove();

  // 화살표 마커
  svg.append('defs').append('marker')
    .attr('id', 'arrow-end')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 22)
    .attr('refY', 0)
    .attr('markerWidth', 6)
    .attr('markerHeight', 6)
    .attr('orient', 'auto')
    .append('path')
    .attr('d', 'M0,-5L10,0L0,5')
    .attr('fill', palette.muted);

  // d3.forceLink 는 source/target 을 객체로 변환시키니, edge 데이터를 복사해 보호
  const nodes = data.nodes.map((n) => ({ ...n }));
  const links = data.edges.map((e) => ({ source: e.from, target: e.to }));

  const sim = d3.forceSimulation(nodes)
    .force('link', d3.forceLink(links).id((d) => d.id).distance(110))
    .force('charge', d3.forceManyBody().strength(-380))
    .force('center', d3.forceCenter(0, 0))
    .force('collide', d3.forceCollide().radius(60));

  const link = svg.append('g')
    .selectAll('line')
    .data(links)
    .join('line')
    .attr('stroke', palette.muted)
    .attr('stroke-width', 1.5)
    .attr('marker-end', 'url(#arrow-end)');

  const node = svg.append('g')
    .selectAll('g')
    .data(nodes)
    .join('g')
    .call(makeDrag(sim));

  node.append('circle')
    .attr('r', 14)
    .attr('fill', (d) => colorByKind[d.kind] || palette.muted)
    .attr('stroke', '#fff')
    .attr('stroke-width', 1.5);

  node.append('text')
    .text((d) => d.id)
    .attr('x', 18)
    .attr('y', 4)
    .attr('font-size', 11)
    .attr('fill', palette.fg);

  sim.on('tick', () => {
    link
      .attr('x1', (d) => d.source.x)
      .attr('y1', (d) => d.source.y)
      .attr('x2', (d) => d.target.x)
      .attr('y2', (d) => d.target.y);
    node.attr('transform', (d) => `translate(${d.x},${d.y})`);
  });
}

function makeDrag(sim) {
  return d3.drag()
    .on('start', (event) => {
      if (!event.active) sim.alphaTarget(0.3).restart();
      event.subject.fx = event.subject.x;
      event.subject.fy = event.subject.y;
    })
    .on('drag', (event) => {
      event.subject.fx = event.x;
      event.subject.fy = event.y;
    })
    .on('end', (event) => {
      if (!event.active) sim.alphaTarget(0);
      event.subject.fx = null;
      event.subject.fy = null;
    });
}

function renderLegend() {
  const root = d3.select('#legend');
  root.selectAll('*').remove();
  for (const [kind, color] of Object.entries(colorByKind)) {
    const item = root.append('span').attr('class', 'inline-flex items-center gap-1');
    item.append('span')
      .style('display', 'inline-block')
      .style('width', '10px')
      .style('height', '10px')
      .style('background-color', color)
      .style('border-radius', '50%');
    item.append('span').text(kindLabel[kind] || kind);
  }
}
