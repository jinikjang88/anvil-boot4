// ch07 — 인증/인가 흐름 + JWT 디코더 + 보호된 API 호출
// localStorage 에 토큰 저장, Authorization 자동 첨부, JWT base64url 분해

const API = 'http://localhost:8080/api/v1';
const TOKEN_KEY = 'anvil.ch07.token';
const USER_KEY  = 'anvil.ch07.user';

const $ = (id) => document.getElementById(id);

// ─── 세션 ─────────────────────────────────────────────────────────────
function getToken() { return localStorage.getItem(TOKEN_KEY); }
function getUser()  { try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; } }
function setSession(token, user) {
  if (token) localStorage.setItem(TOKEN_KEY, token); else localStorage.removeItem(TOKEN_KEY);
  if (user)  localStorage.setItem(USER_KEY, JSON.stringify(user)); else localStorage.removeItem(USER_KEY);
  renderSession();
  renderJwt();
}

function renderSession() {
  const user = getUser();
  const badge = $('sessionBadge');
  if (user) {
    badge.innerHTML = `<span class="px-2 py-1 rounded bg-anvil-success text-white">로그인됨 · ${user.email} · ${user.role}</span>`;
  } else {
    badge.innerHTML = `<span class="px-2 py-1 rounded bg-anvil-muted text-white">로그인 안 됨</span>`;
  }
}

// ─── 회원가입 / 로그인 폼 ──────────────────────────────────────────────
$('registerForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.target;
  const payload = {
    email: form.email.value,
    password: form.password.value,
    name: form.name.value,
    phoneNumber: form.phoneNumber.value,
    role: form.role.value,
  };
  await callAuth('register', payload, /*storeSession=*/ false);
});

$('loginForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.target;
  const payload = { email: form.email.value, password: form.password.value };
  await callAuth('login', payload, /*storeSession=*/ true);
});

$('logout').addEventListener('click', () => {
  setSession(null, null);
  showAuthResponse({ message: 'logged out (token cleared)' }, 'muted');
  $('apiResponse').classList.add('hidden');
});

async function callAuth(endpoint, payload, storeSession) {
  try {
    const res = await fetch(`${API}/auth/${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const data = await res.json();
    if (!res.ok) {
      showAuthResponse(data, 'error');
      return;
    }
    if (storeSession && data.accessToken) {
      setSession(data.accessToken, data.user);
    }
    showAuthResponse(data, 'success');
  } catch (err) {
    showAuthResponse({ error: err.message }, 'error');
  }
}

function showAuthResponse(data, kind) {
  const el = $('authResponse');
  el.classList.remove('hidden', 'text-anvil-success', 'text-anvil-accent', 'text-anvil-muted');
  el.textContent = JSON.stringify(data, null, 2);
  if (kind === 'success')      el.classList.add('text-anvil-success');
  else if (kind === 'error')   el.classList.add('text-anvil-accent');
  else                         el.classList.add('text-anvil-muted');
}

// ─── JWT 디코더 (base64url) ────────────────────────────────────────────
function renderJwt() {
  const token = getToken();
  $('rawToken').textContent = token || '(없음 — 로그인하면 채워집니다)';

  if (!token) {
    $('jwtHeader').textContent  = '-';
    $('jwtPayload').textContent = '-';
    $('jwtSig').textContent     = '-';
    $('expiry').textContent     = '-';
    return;
  }

  const parts = token.split('.');
  if (parts.length !== 3) {
    $('jwtHeader').textContent = 'invalid JWT (not 3 parts)';
    return;
  }

  $('jwtHeader').textContent  = JSON.stringify(base64UrlDecodeJson(parts[0]), null, 2);
  const payload = base64UrlDecodeJson(parts[1]);
  $('jwtPayload').textContent = JSON.stringify(payload, null, 2);
  $('jwtSig').textContent     = parts[2];

  if (payload && payload.exp) {
    const expMs = payload.exp * 1000;
    const remaining = Math.max(0, Math.floor((expMs - Date.now()) / 1000));
    $('expiry').textContent = `만료까지 ≈ ${remaining}s (exp = ${new Date(expMs).toISOString()})`;
  }
}

function base64UrlDecodeJson(b64url) {
  try {
    const b64 = b64url.replace(/-/g, '+').replace(/_/g, '/');
    const padded = b64 + '='.repeat((4 - b64.length % 4) % 4);
    return JSON.parse(decodeURIComponent(escape(atob(padded))));
  } catch (err) {
    return { _error: err.message };
  }
}

// ─── 보호된 API 호출 ───────────────────────────────────────────────────
document.querySelectorAll('.api-btn').forEach((btn) => {
  btn.addEventListener('click', () => {
    const kind = btn.dataset.call;
    const useAuth = kind !== 'meNoAuth';
    const path = (kind === 'usersAll') ? '/users' : '/users/me';
    callApi(path, useAuth);
  });
});

async function callApi(path, useAuth) {
  const headers = {};
  if (useAuth && getToken()) {
    headers['Authorization'] = 'Bearer ' + getToken();
  }
  try {
    const res = await fetch(`${API}${path}`, { headers });
    const text = await res.text();
    let parsed;
    try { parsed = JSON.parse(text); } catch { parsed = text; }

    const out = $('apiResponse');
    out.classList.remove('hidden', 'text-anvil-success', 'text-anvil-accent', 'text-anvil-muted');
    out.textContent = `HTTP ${res.status} ${res.statusText}\n\n${typeof parsed === 'string' ? parsed : JSON.stringify(parsed, null, 2)}`;
    if (res.ok) {
      out.classList.add('text-anvil-success');
    } else {
      out.classList.add('text-anvil-accent');
    }
  } catch (err) {
    const out = $('apiResponse');
    out.classList.remove('hidden');
    out.classList.add('text-anvil-accent');
    out.textContent = `ERROR: ${err.message}`;
  }
}

// ─── 초기화 ────────────────────────────────────────────────────────────
renderSession();
renderJwt();
// 만료까지 시간 카운트다운 갱신
setInterval(renderJwt, 1000);
