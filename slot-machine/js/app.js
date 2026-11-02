/*
 * Reel Riot - game controller: lobby, machines, reel animation, win
 * presentation, features, overlays, the Control Room and saving.
 */
(function () {
  'use strict';

  const E = window.SlotEngine;
  const Sound = window.SlotSound;
  const FX = window.SlotFX;
  const LIST = window.SlotMachines.list.map(function (cfg) { return E.compile(cfg); });
  const MACH = {};
  LIST.forEach(function (m) { MACH[m.id] = m; });

  const BETS = [1e3, 2e3, 5e3, 1e4, 2e4, 5e4, 1e5, 2e5, 5e5, 1e6, 2e6, 5e6, 1e7, 2e7, 5e7, 1e8, 2.5e8, 5e8, 1e9];
  const DEFAULT_BET = 6; // 100K
  const START_COINS = 1e7;
  const REFILL = 1e7;
  const MAX_COINS = 1e15;
  const BONUS_EVERY = 10 * 60 * 1000;
  const JP_LOG_MIN = -1.7;
  const JP_LOG_MAX = 1.7;
  const FRAME_PAD = 12;
  const SAVE_KEY = 'reel-riot-v1';
  const TIER_COLORS = { mini: '#1fbf63', minor: '#1a8fe0', major: '#8f3ee8', grand: '#e01e2e' };
  const LINE_COLORS = ['#ff3d6e', '#2ee8ff', '#ffd84d', '#4dff9a', '#b867ff', '#ff8a3d', '#ff4fd8', '#7dd3fc', '#a3e635', '#f472b6'];
  const JP_WHEEL = ['mini', 'minor', 'mini', 'major', 'mini', 'minor', 'mini', 'grand', 'mini', 'minor', 'mini', 'major'];
  const BONUS_WHEEL = [
    { v: 1e6, w: 18, c: '#e0157a' }, { v: 2.5e6, w: 14, c: '#6a2ee8' }, { v: 5e5, w: 20, c: '#0fa3c9' },
    { v: 5e6, w: 10, c: '#e08a00' }, { v: 1e6, w: 18, c: '#e0157a' }, { v: 1e7, w: 6, c: '#6a2ee8' },
    { v: 2.5e6, w: 12, c: '#0fa3c9' }, { v: 2.5e7, w: 2, c: '#d01c1c' }
  ];
  const REDUCED = !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);

  const rng = E.makeRng();
  const $ = function (s, el) { return (el || document).querySelector(s); };
  const $$ = function (s, el) { return Array.prototype.slice.call((el || document).querySelectorAll(s)); };

  /* ------------------------------------------------------------ helpers */

  const fmt = function (n) { return Math.round(n).toLocaleString('en-US'); };
  function trimZeros(s) { return s.indexOf('.') < 0 ? s : s.replace(/0+$/, '').replace(/\.$/, ''); }
  function short(n) {
    const a = Math.abs(n);
    const units = [[1e15, 'Q'], [1e12, 'T'], [1e9, 'B'], [1e6, 'M'], [1e3, 'K']];
    for (let i = 0; i < units.length; i++) {
      if (a >= units[i][0]) {
        const x = n / units[i][0];
        const d = Math.abs(x) >= 100 ? 0 : Math.abs(x) >= 10 ? 1 : 2;
        return trimZeros(x.toFixed(d)) + units[i][1];
      }
    }
    return String(Math.round(n));
  }
  function oneIn(p) { return p > 0 ? '1 in ' + fmt(Math.max(1, 1 / p)) : 'never'; }
  function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }
  function rgb(hex) {
    const n = parseInt(hex.slice(1), 16);
    return ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255);
  }
  function htmlEl(html) {
    const t = document.createElement('div');
    t.innerHTML = html;
    return t.firstElementChild;
  }
  const easeOut = function (p) { return 1 - Math.pow(1 - p, 3); };
  const easeOutQuad = function (p) { return 1 - (1 - p) * (1 - p); };
  function bump(el, cls) {
    if (!el) return;
    cls = cls || 'bump';
    el.classList.remove(cls);
    void el.offsetWidth;
    el.classList.add(cls);
  }

  // Shrinks a `.fit` element's font until its text fits on one line.
  const fitEls = new Set();
  function fit(el) {
    fitEls.add(el);
    el.style.fontSize = '';
    const cw = el.clientWidth;
    if (!cw) { el._len = -1; return; }
    const sw = el.scrollWidth;
    if (sw > cw) {
      const max = parseFloat(getComputedStyle(el).fontSize);
      el.style.fontSize = Math.max(10, Math.floor(max * cw / sw * 0.98)) + 'px';
    }
  }
  function setText(el, text) {
    if (!el) return;
    if (el._t === text && el._len === text.length) return;
    el._t = text;
    el.textContent = text;
    if (el.classList.contains('fit') && el._len !== text.length) {
      el._len = text.length;
      fit(el);
    }
  }
  function refit() {
    fitEls.forEach(function (el) {
      if (!el.isConnected) return;
      el._len = (el._t || '').length;
      fit(el);
    });
  }

  // Waits and tweens that a tap can fast-forward.
  const skippers = new Set();
  function skipAll() { Array.from(skippers).forEach(function (f) { f(); }); }
  function sleep(ms, skippable) {
    return new Promise(function (resolve) {
      let timer = 0;
      const done = function () { clearTimeout(timer); skippers.delete(done); resolve(); };
      timer = setTimeout(done, ms);
      if (skippable !== false) skippers.add(done);
    });
  }
  function tween(ms, fn, ease, skippable) {
    ease = ease || easeOut;
    return new Promise(function (resolve) {
      const t0 = performance.now();
      let done = false;
      const finish = function () {
        if (done) return;
        done = true;
        skippers.delete(finish);
        fn(1);
        resolve();
      };
      const frame = function (t) {
        if (done) return;
        const p = Math.min(1, (t - t0) / ms);
        if (p >= 1) { finish(); return; }
        fn(ease(p));
        requestAnimationFrame(frame);
      };
      if (skippable !== false) skippers.add(finish);
      if (ms <= 0) finish(); else requestAnimationFrame(frame);
    });
  }
  // Resolves on a tap of `target` (after a short guard) or after `ms`.
  function waitTap(target, ms) {
    return new Promise(function (resolve) {
      const t0 = performance.now();
      let done = false;
      let timer = 0;
      const finish = function () {
        if (done) return;
        done = true;
        clearTimeout(timer);
        target.removeEventListener('click', onClick);
        if (rt.tapper === finish) rt.tapper = null;
        resolve();
      };
      const onClick = function () { if (performance.now() - t0 > 300) finish(); };
      target.addEventListener('click', onClick);
      rt.tapper = finish;
      if (ms) timer = setTimeout(finish, ms);
    });
  }

  // Animates a number shown in `el` towards `v`. Newer calls win.
  function rollTo(el, v, ms, onTick) {
    const from = el._v == null ? v : el._v;
    el._v = v;
    const tok = (el._tok = (el._tok || 0) + 1);
    if (!ms || from === v) { setText(el, fmt(v)); return Promise.resolve(); }
    return tween(ms, function (p) {
      if (el._tok !== tok) return;
      setText(el, fmt(from + (v - from) * p));
      if (onTick) onTick(p);
    }, easeOut, true);
  }

  let toastTimer = 0;
  function toast(msg, bad) {
    const el = $('#toast');
    el.textContent = msg;
    el.className = 'toast show' + (bad ? ' bad' : '');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.className = 'toast'; }, 2400);
  }
  function buzz(p) {
    if (!state.settings.vibrate) return;
    try { if (navigator.vibrate) navigator.vibrate(p); } catch (e) { /* not allowed here */ }
  }
  function flash() { bump($('#flash'), 'go'); }
  function shake() {
    if (REDUCED) return;
    const el = $('#machine');
    bump(el, 'shake');
    setTimeout(function () { el.classList.remove('shake'); }, 600);
  }

  /* -------------------------------------------------------------- state */

  function freshStats() {
    return { spins: 0, wagered: 0, won: 0, biggest: 0, biggestX: 0, biggestOn: '', freeSpins: 0, jackpots: { mini: 0, minor: 0, major: 0, grand: 0 } };
  }
  function defaults() {
    const pools = {};
    const bets = {};
    LIST.forEach(function (m) { pools[m.id] = E.createPools(m); bets[m.id] = DEFAULT_BET; });
    return {
      v: 1,
      balance: START_COINS,
      bets: bets,
      pools: pools,
      settings: { jackpotLog: 0, sound: true, music: true, vibrate: true, turbo: false, muted: false },
      stats: freshStats(),
      bonusAt: 0,
      pendingFs: null,
      last: null
    };
  }
  const num = function (v) { return typeof v === 'number' && isFinite(v); };

  // Builds a clean state from saved data, ignoring anything malformed.
  function normalize(s) {
    const d = defaults();
    if (!s || typeof s !== 'object') return d;
    if (num(s.balance)) d.balance = clamp(Math.round(s.balance), 0, MAX_COINS);
    LIST.forEach(function (m) {
      const b = s.bets && s.bets[m.id];
      if (Number.isInteger(b) && b >= 0 && b < BETS.length) d.bets[m.id] = b;
      const p = s.pools && s.pools[m.id];
      if (p) m.jackpot.tiers.forEach(function (t) { if (num(p[t.id])) d.pools[m.id][t.id] = clamp(p[t.id], t.seed, t.cap); });
    });
    const st = s.settings || {};
    if (num(st.jackpotLog)) d.settings.jackpotLog = clamp(st.jackpotLog, JP_LOG_MIN, JP_LOG_MAX);
    ['sound', 'music', 'vibrate', 'turbo', 'muted'].forEach(function (k) { if (typeof st[k] === 'boolean') d.settings[k] = st[k]; });
    const ss = s.stats || {};
    ['spins', 'wagered', 'won', 'biggest', 'biggestX', 'freeSpins'].forEach(function (k) { if (num(ss[k])) d.stats[k] = ss[k]; });
    if (typeof ss.biggestOn === 'string') d.stats.biggestOn = ss.biggestOn.slice(0, 40);
    if (ss.jackpots) Object.keys(d.stats.jackpots).forEach(function (k) { if (num(ss.jackpots[k])) d.stats.jackpots[k] = ss.jackpots[k]; });
    if (num(s.bonusAt)) d.bonusAt = Math.min(s.bonusAt, Date.now() + BONUS_EVERY);
    if (typeof s.last === 'string' && MACH[s.last]) d.last = s.last;
    const pf = s.pendingFs;
    if (pf && MACH[pf.machine] && pf.fs && num(pf.fs.spins) && num(pf.fs.played) && num(pf.fs.bet) && num(pf.fs.total) &&
        num(pf.fs.capLeft) && pf.fs.played < pf.fs.spins && Array.isArray(pf.fs.sticky) && !pf.fs.done) {
      const m = MACH[pf.machine];
      const sticky = pf.fs.sticky.filter(function (c) {
        return Array.isArray(c) && Number.isInteger(c[0]) && Number.isInteger(c[1]) && c[0] >= 0 && c[0] < m.reels && c[1] >= 0 && c[1] < m.rows;
      });
      d.pendingFs = {
        machine: pf.machine,
        fs: {
          spins: pf.fs.spins, played: pf.fs.played, total: pf.fs.total, bet: pf.fs.bet, sticky: sticky,
          ladder: num(pf.fs.ladder) ? pf.fs.ladder : 0, capLeft: pf.fs.capLeft, retriggers: num(pf.fs.retriggers) ? pf.fs.retriggers : 0, done: false
        }
      };
    }
    return d;
  }
  function load() {
    try { return normalize(JSON.parse(localStorage.getItem(SAVE_KEY) || 'null')); } catch (e) { return defaults(); }
  }
  function save() {
    try { localStorage.setItem(SAVE_KEY, JSON.stringify(state)); } catch (e) { /* storage unavailable */ }
  }

  let state = defaults();
  const rt = {
    screen: 'lobby', m: null, busy: false, spinning: false, quick: false, cascading: false,
    auto: 0, autoTimer: 0, anims: [], reelEls: [], grid: null, cw: 60, ch: 60, gap: 4, laidOut: '',
    cache: {}, cycle: 0, fs: null, shown: 0, winShown: 0, sticky: [], layoutPending: false,
    wheelRot: 0, oddsId: 'neon', force: null, modal: null, tapper: null, hint: 0, lastWins: []
  };

  const betOf = function (m) { return BETS[state.bets[m.id]]; };
  const jpFactor = function () { return Math.pow(10, state.settings.jackpotLog); };

  function track(amount, bet, m) {
    const s = state.stats;
    if (amount > s.biggest) { s.biggest = amount; s.biggestX = amount / bet; s.biggestOn = m.short; }
  }

  function showBalance(v, ms) {
    rt.shown = v;
    rollTo($('#balance'), v, ms);
    rollTo($('#lobby-balance'), v, ms);
  }
  function addShown(delta, ms) { showBalance(rt.shown + delta, ms); }
  function syncShown() { if (Math.round(rt.shown) !== Math.round(state.balance)) showBalance(state.balance, 400); }

  /* -------------------------------------------------------------- lobby */

  function buildLobby() {
    const vol = ['Low', 'Medium', 'High'];
    $('#cabinets').innerHTML = LIST.map(function (m) {
      const bars = [1, 2, 3].map(function (i) { return '<i' + (i <= m.volatility ? ' class="on"' : '') + '></i>'; }).join('');
      const meta = m.mode === 'lines' ? m.lines.length + ' paylines' : m.mode === 'ways' ? fmt(Math.pow(m.rows, m.reels)) + ' ways' : 'Cluster pays';
      return '<button type="button" class="cab" data-machine="' + m.id + '" data-id="' + m.id + '" aria-label="Play ' + m.name + '">' +
        '<span class="cab-art"><span class="cab-icons" aria-hidden="true">' + m.art.map(function (g) { return '<span>' + g + '</span>'; }).join('') + '</span>' +
        '<span class="cab-title">' + m.name + '</span><span class="cab-grid">' + m.reels + ' &times; ' + m.rows + ' &middot; ' + meta + '</span>' +
        '<span class="cab-pending" hidden>Free spins waiting</span></span>' +
        '<span class="cab-body">' +
        '<span class="cab-row"><span class="cab-meta">' + m.tagline + '</span><span class="cab-vol" title="Volatility">' + vol[m.volatility - 1] + ' risk ' + bars + '</span></span>' +
        '<span class="cab-grand"><span class="cab-grand-label">Grand</span><span class="cab-grand-val fit" data-grand="' + m.id + '">0</span></span>' +
        '<span class="cab-feats">' + m.features.map(function (f) { return '<span>' + f + '</span>'; }).join('') + '</span>' +
        '<span class="cab-play">Play</span></span></button>';
    }).join('');
    $$('#cabinets .cab').forEach(function (b) {
      b.addEventListener('click', function () { Sound.click(); enterMachine(b.dataset.id); });
    });
  }

  function renderLobby() {
    LIST.forEach(function (m) {
      const el = $('[data-grand="' + m.id + '"]');
      el._len = -1;
      setText(el, fmt(state.pools[m.id].grand * betOf(m)));
      $('.cab[data-id="' + m.id + '"] .cab-pending').hidden = !(state.pendingFs && state.pendingFs.machine === m.id);
    });
    showBalance(state.balance);
    updateBonus();
    const s = state.stats;
    const jp = s.jackpots.mini + s.jackpots.minor + s.jackpots.major + s.jackpots.grand;
    $('#lobby-stats').textContent = s.spins
      ? fmt(s.spins) + ' spins played, ' + jp + ' jackpot' + (jp === 1 ? '' : 's') + ' hit, biggest win ' + fmt(s.biggest) + '. Coins have no cash value.'
      : 'Coins are for fun and have no cash value. Jackpots are rare by design; the Control Room lets you change that.';
  }

  function updateBonus() {
    const btn = $('#btn-bonus');
    const left = state.bonusAt - Date.now();
    if (left <= 0) {
      btn.classList.add('ready');
      btn.classList.remove('waiting');
      setText($('#bonus-text'), 'Spin the free coin wheel');
    } else {
      btn.classList.remove('ready');
      btn.classList.add('waiting');
      const mm = Math.floor(left / 60000);
      const ss = Math.floor(left / 1000) % 60;
      setText($('#bonus-text'), 'Free coin wheel in ' + mm + ':' + String(ss).padStart(2, '0'));
    }
  }

  /* ------------------------------------------------------- machine view */

  function enterMachine(id) {
    if (rt.busy || !MACH[id]) return;
    const m = MACH[id];
    rt.m = m;
    $('#app').dataset.machine = id;
    state.last = id;
    $('#lobby').hidden = true;
    $('#machine').hidden = false;
    rt.screen = 'machine';
    $('#m-name').textContent = m.name;
    $('#fs-extra').textContent = m.freeSpins.title;
    $('#fs-banner').hidden = true;
    $('#machine').classList.remove('in-fs');
    rt.laidOut = '';
    buildReels();
    buildLadder();
    clearSticky();
    clearHighlight();
    renderBet();
    $$('.jp-val').forEach(function (el) { el._v = null; el._len = -1; });
    renderJackpots(false);
    $('#balance')._len = -1;
    showBalance(state.balance);
    rt.winShown = 0;
    $('#winbar').classList.remove('has-win');
    setMsg(m.tagline);
    layout();
    requestAnimationFrame(function () { layout(); refit(); });
    Sound.music(id);
    save();
    if (state.pendingFs && state.pendingFs.machine === id) setTimeout(resumeFreeSpins, 400);
  }

  function leaveMachine() {
    if (rt.busy) return;
    stopAuto();
    stopCycle();
    closeAutoMenu();
    $('#machine').hidden = true;
    $('#lobby').hidden = false;
    rt.screen = 'lobby';
    rt.m = null;
    renderLobby();
    requestAnimationFrame(refit);
    Sound.music('lobby');
    save();
  }

  function layout() {
    const m = rt.m;
    if (!m || rt.screen !== 'machine') return;
    if (rt.spinning || rt.cascading) { rt.layoutPending = true; return; }
    const stage = $('#stage');
    const w = stage.clientWidth;
    const h = stage.clientHeight;
    if (!w || !h) return;
    const gap = w < 420 ? 3 : 5;
    // Cells may be taller than wide (like real reels) to use a tall screen.
    const aw = Math.floor((w - FRAME_PAD * 2 - gap * (m.reels - 1)) / m.reels);
    const ah = Math.floor((h - FRAME_PAD * 2) / m.rows);
    let cw;
    let ch;
    if (ah >= aw) { cw = aw; ch = Math.min(ah, Math.round(aw * 1.45)); } else { ch = ah; cw = Math.min(aw, Math.round(ah * 1.15)); }
    cw = clamp(cw, 30, 160);
    ch = clamp(ch, 30, 210);
    if (cw === rt.cw && ch === rt.ch && gap === rt.gap && rt.laidOut === m.id) return;
    rt.cw = cw;
    rt.ch = ch;
    rt.gap = gap;
    rt.laidOut = m.id;
    const el = $('#machine');
    el.style.setProperty('--cw', cw + 'px');
    el.style.setProperty('--ch', ch + 'px');
    el.style.setProperty('--cell', Math.min(cw, ch) + 'px');
    el.style.setProperty('--gap', gap + 'px');
    const W = m.reels * cw + (m.reels - 1) * gap;
    const H = m.rows * ch;
    $('#paylines').setAttribute('viewBox', '0 0 ' + W + ' ' + H);
    placeBulbs(W + FRAME_PAD * 2, H + FRAME_PAD * 2);
    positionSticky();
  }

  function placeBulbs(W, H) {
    const ins = (5 + FRAME_PAD) / 2;
    const w = W - 2 * ins;
    const h = H - 2 * ins;
    const per = 2 * (w + h);
    const n = Math.max(16, Math.round(per / 22));
    let html = '';
    for (let i = 0; i < n; i++) {
      let d = (i / n) * per;
      let x;
      let y;
      if (d < w) { x = d; y = 0; } else if ((d -= w) < h) { x = w; y = d; } else if ((d -= h) < w) { x = w - d; y = h; } else { d -= w; x = 0; y = h - d; }
      html += '<i style="left:' + (x + ins).toFixed(1) + 'px;top:' + (y + ins).toFixed(1) + 'px;--p:' + (i % 3) + '"></i>';
    }
    $('#bulbs').innerHTML = html;
  }

  function renderBet() {
    const m = rt.m;
    const i = state.bets[m.id];
    setText($('#bet-value'), short(BETS[i]));
    const lock = rt.busy || !!rt.fs;
    $('#bet-down').disabled = lock || i === 0;
    $('#bet-up').disabled = lock || i === BETS.length - 1;
  }

  function changeBet(d) {
    const m = rt.m;
    if (!m || rt.busy || rt.fs) return;
    const i = clamp(state.bets[m.id] + d, 0, BETS.length - 1);
    if (i === state.bets[m.id]) { Sound.error(); return; }
    state.bets[m.id] = i;
    Sound.bet(d > 0);
    renderBet();
    renderJackpots(true);
    save();
  }

  function renderJackpots(animate, hold) {
    const m = rt.m;
    if (!m) return;
    const bet = betOf(m);
    const pools = state.pools[m.id];
    m.jackpot.tiers.forEach(function (t) {
      const v = (hold && hold[t.id] != null ? hold[t.id] : pools[t.id]) * bet;
      rollTo($('#jp-' + t.id), v, animate ? 700 : 0);
    });
  }

  /* -------------------------------------------------------------- reels */

  function symHTML(m, sym) {
    const c = rt.cache[m.id] || (rt.cache[m.id] = {});
    if (c[sym]) return c[sym];
    const s = m.symbols[sym];
    const kind = s.kind || 'normal';
    let inner;
    if (kind === 'coin') inner = '<span class="g g-coin"><b>JACK</b><b>POT</b></span>';
    else if (kind === 'wild') inner = '<span class="g g-wild">WILD</span>';
    else if (kind === 'scatter') inner = '<span class="g g-scatter"><span class="g-e">' + s.glyph + '</span><span class="g-tag">BONUS</span></span>';
    else if (s.text) inner = '<span class="g g-text g-' + sym + '">' + s.glyph + '</span>';
    else inner = '<span class="g g-e">' + s.glyph + '</span>';
    c[sym] = '<div class="cell k-' + kind + ' s-' + sym + '" style="--c:' + rgb(s.color) + '"><span class="tile"></span>' + inner + '</div>';
    return c[sym];
  }

  function fillerSym(m, r) {
    const x = Math.random();
    if (x < 0.035) return 'scatter';
    if (x < 0.055) return 'coin';
    return E.pick(m.reelTables[r], Math.random);
  }

  function buildReels() {
    const m = rt.m;
    const reels = $('#reels');
    reels.style.setProperty('--R', m.reels);
    reels.style.setProperty('--H', m.rows);
    reels.innerHTML = '';
    rt.reelEls = [];
    for (let r = 0; r < m.reels; r++) {
      const reel = document.createElement('div');
      reel.className = 'reel';
      const strip = document.createElement('div');
      strip.className = 'strip';
      reel.appendChild(strip);
      reels.appendChild(reel);
      rt.reelEls.push({ reel: reel, strip: strip });
    }
    rt.grid = [];
    for (let r = 0; r < m.reels; r++) {
      const col = [];
      for (let h = 0; h < m.rows; h++) col.push(E.pick(m.reelTables[r], Math.random));
      rt.grid.push(col);
      renderColumn(r, col);
    }
  }

  function renderColumn(r, col) {
    const s = rt.reelEls[r].strip;
    s.innerHTML = col.map(function (x) { return symHTML(rt.m, x); }).join('');
    s.style.transform = 'translate3d(0,0,0)';
  }

  const cellEl = function (r, h) { return rt.reelEls[r] ? rt.reelEls[r].strip.children[h] : null; };
  const keyOf = function (r, h) { return r * 100 + h; };

  function cellPoint(r, h) {
    const el = cellEl(r, h);
    if (!el) return { x: innerWidth / 2, y: innerHeight / 2 };
    const b = el.getBoundingClientRect();
    return { x: b.left + b.width / 2, y: b.top + b.height / 2 };
  }

  // Reels after two scatters or two jackpot coins spin longer, for suspense.
  function anticipation(grid) {
    const out = [];
    let sc = 0;
    let co = 0;
    for (let r = 0; r < grid.length; r++) {
      out.push(r > 0 && (sc >= 2 || co >= 2));
      grid[r].forEach(function (s) { if (s === 'scatter') sc++; else if (s === 'coin') co++; });
    }
    return out;
  }

  function spinReels(grid) {
    const m = rt.m;
    const R = m.reels;
    const H = m.rows;
    const cell = rt.ch;
    const turbo = state.settings.turbo || REDUCED;
    const base = turbo ? 380 : 820;
    const per = turbo ? 80 : 160;
    const anticExtra = turbo ? 650 : 1250;
    const speed = turbo ? 0.055 : 0.04;
    const antic = anticipation(grid);
    const stops = [];
    let t = base;
    for (let r = 0; r < R; r++) {
      if (r) t += per;
      if (antic[r]) t += anticExtra;
      stops.push(t);
    }
    const prev = rt.grid || grid;
    rt.spinning = true;
    rt.quick = false;
    renderSpinBtn();
    Sound.spinStart();
    buzz(8);
    return new Promise(function (resolve) {
      let stopped = 0;
      let sc = 0;
      let co = 0;
      rt.anims = [];
      for (let r = 0; r < R; r++) {
        const reel = rt.reelEls[r].reel;
        const strip = rt.reelEls[r].strip;
        const dur = stops[r];
        const extra = Math.max(5, Math.round(dur * speed));
        const syms = [fillerSym(m, r)].concat(grid[r]);
        for (let i = 0; i < extra; i++) syms.push(fillerSym(m, r));
        prev[r].forEach(function (s) { syms.push(s); });
        strip.innerHTML = syms.map(function (x) { return symHTML(m, x); }).join('');
        const startY = -(1 + H + extra) * cell;
        const endY = -cell;
        const kick = Math.min(0.14, 120 / dur);
        const settle = Math.max(0.8, 1 - 150 / dur);
        strip.classList.add('moving');
        if (!REDUCED) strip.classList.add('blur');
        const anim = strip.animate([
          { transform: 'translate3d(0,' + startY + 'px,0)', easing: 'ease-out' },
          { transform: 'translate3d(0,' + (startY - cell * 0.24) + 'px,0)', offset: kick, easing: 'cubic-bezier(.35,0,.2,1)' },
          { transform: 'translate3d(0,' + (endY + cell * 0.16) + 'px,0)', offset: settle, easing: 'ease-in-out' },
          { transform: 'translate3d(0,' + endY + 'px,0)' }
        ], { duration: dur, fill: 'forwards' });
        rt.anims.push(anim);
        const blurOff = setTimeout(function () { strip.classList.remove('blur'); }, Math.max(0, dur - 280));
        anim.finished.then(function () {
          clearTimeout(blurOff);
          renderColumn(r, grid[r]);
          strip.classList.remove('moving', 'blur');
          anim.cancel();
          Sound.reelStop(r);
          reel.classList.remove('anticipate');
          Sound.anticipationEnd();
          grid[r].forEach(function (s, h) {
            if (s === 'scatter') { sc++; Sound.scatter(sc); land(r, h); }
            else if (s === 'coin') { co++; Sound.coin(co); land(r, h); }
          });
          if (r + 1 < R && antic[r + 1] && !rt.quick) {
            rt.reelEls[r + 1].reel.classList.add('anticipate');
            Sound.anticipation(stops[r + 1] - stops[r]);
          }
          stopped++;
          if (stopped === R) {
            rt.spinning = false;
            rt.grid = grid;
            rt.anims = [];
            renderSpinBtn();
            if (rt.layoutPending) { rt.layoutPending = false; layout(); }
            resolve();
          }
        }).catch(function () { /* cancelled */ });
      }
    });
  }

  function land(r, h) {
    const el = cellEl(r, h);
    if (!el) return;
    el.classList.add('landed');
    const p = cellPoint(r, h);
    FX.burst(p.x, p.y, 'rgb(' + el.style.getPropertyValue('--c') + ')', 10, 180);
    buzz(12);
  }

  function quickStop() {
    if (!rt.spinning || rt.quick) return;
    rt.quick = true;
    rt.anims.forEach(function (a) { try { a.playbackRate = 5; } catch (e) { /* finished */ } });
    $$('.reel.anticipate').forEach(function (el) { el.classList.remove('anticipate'); });
    Sound.anticipationEnd();
  }

  function hurry() {
    if (rt.spinning) quickStop();
    else skipAll();
  }

  /* ------------------------------------------------------ win showcase */

  function highlight(keys) {
    $('#reels').classList.add('dim');
    for (let r = 0; r < rt.m.reels; r++) {
      for (let h = 0; h < rt.m.rows; h++) {
        const el = cellEl(r, h);
        if (el) el.classList.toggle('win', keys.has(keyOf(r, h)));
      }
    }
    $$('#sticky .cell').forEach(function (el) { el.classList.toggle('win', keys.has(+el.dataset.k)); });
  }

  function clearHighlight() {
    $('#reels').classList.remove('dim');
    $$('#reels .cell.win, #sticky .cell.win').forEach(function (el) { el.classList.remove('win'); });
    $('#paylines').innerHTML = '';
  }

  function drawLines(wins) {
    const m = rt.m;
    const step = rt.cw + rt.gap;
    const hx = rt.cw / 2;
    const hy = rt.ch / 2;
    $('#paylines').innerHTML = wins.filter(function (w) { return w.kind === 'line'; }).map(function (w) {
      const L = m.lines[w.line];
      const pts = L.map(function (h, r) { return (r * step + hx).toFixed(1) + ',' + (h * rt.ch + hy).toFixed(1); });
      pts.unshift('2,' + (L[0] * rt.ch + hy).toFixed(1));
      pts.push(((m.reels - 1) * step + rt.cw - 2).toFixed(1) + ',' + (L[m.reels - 1] * rt.ch + hy).toFixed(1));
      return '<polyline class="pl" pathLength="100" style="--lc:' + LINE_COLORS[w.line % LINE_COLORS.length] + '" points="' + pts.join(' ') + '"/>';
    }).join('');
  }

  function symLabel(m, id) { const s = m.symbols[id]; return s.text ? s.name : s.glyph; }

  function describe(w) {
    const m = rt.m;
    if (w.kind === 'line') return 'Line ' + (w.line + 1) + ': ' + w.count + ' × ' + symLabel(m, w.sym) + ' pays ' + fmt(w.amount);
    if (w.kind === 'ways') return w.count + ' reels of ' + symLabel(m, w.sym) + ', ' + w.ways + (w.ways === 1 ? ' way' : ' ways') + ', pays ' + fmt(w.amount);
    if (w.kind === 'scatter') return w.count + ' × ' + symLabel(m, 'scatter') + ' scatter pays ' + fmt(w.amount);
    return 'Cluster of ' + w.size + ' ' + symLabel(m, w.sym) + ' pays ' + fmt(w.amount);
  }

  function setMsg(text, hot) {
    const el = $('#win-msg');
    el.textContent = text;
    el.classList.toggle('hot', !!hot);
  }

  function hints(m) {
    return [
      m.scatter.label + ' start free spins',
      'Land 3 jackpot coins to spin the Jackpot Wheel',
      m.id === 'neon' ? 'Wilds land on reels 2, 3 and 4' : m.id === 'dragon' ? 'Dragon Fire can torch the reels with wilds' : 'Every cascade raises the multiplier',
      'Bigger bets mean bigger jackpots'
    ];
  }

  function rollWin(to, ms) {
    const bar = $('#winbar');
    const el = $('#win-value');
    if (!bar.classList.contains('has-win')) { bar.classList.add('has-win'); el._len = -1; }
    $('#win-label').textContent = rt.fs ? 'Total win' : 'Win';
    rt.winShown = to;
    let last = 0;
    return rollTo(el, to, ms, function (p) {
      const now = performance.now();
      if (p < 1 && now - last > 70) { last = now; Sound.tick(p); }
    });
  }

  function countTime(win, bet) {
    const x = win / bet;
    const ms = clamp(350 + x * 140, 380, 1800);
    return state.settings.turbo ? ms * 0.5 : ms;
  }

  function startCycle(wins) {
    stopCycle();
    if (!wins.length) return;
    let i = 0;
    const show = function () {
      const w = wins[i % wins.length];
      i++;
      const keys = new Set(w.cells.map(function (c) { return keyOf(c[0], c[1]); }));
      highlight(keys);
      drawLines([w]);
      setMsg(describe(w));
    };
    show();
    if (wins.length > 1) rt.cycle = setInterval(show, 1600);
  }

  function stopCycle() {
    clearInterval(rt.cycle);
    rt.cycle = 0;
  }

  function presentWins(res) {
    const all = res.steps[0].wins.slice();
    if (res.scatter.pay > 0) all.push({ kind: 'scatter', count: res.scatter.count, cells: res.scatter.cells, amount: res.scatter.pay });
    if (!all.length) return all;
    const keys = new Set();
    all.forEach(function (w) { w.cells.forEach(function (c) { keys.add(keyOf(c[0], c[1])); }); });
    highlight(keys);
    drawLines(all);
    $('#cabinet').classList.add('party');
    Sound.win(res.win >= res.bet * 5 ? 2 : res.win >= res.bet ? 1 : 0);
    buzz(15);
    return all;
  }

  function floatAmount(w) {
    const m = rt.m;
    let sr = 0;
    let sh = 0;
    w.cells.forEach(function (c) { sr += c[0]; sh += c[1]; });
    const r = sr / w.cells.length;
    const h = sh / w.cells.length;
    const el = document.createElement('div');
    el.className = 'float-win';
    el.style.setProperty('--c', rgb(m.symbols[w.sym].color));
    el.style.left = (r * (rt.cw + rt.gap) + rt.cw / 2) + 'px';
    el.style.top = (h * rt.ch + rt.ch / 2) + 'px';
    el.textContent = '+' + short(w.amount);
    $('#reel-fx').appendChild(el);
    setTimeout(function () { el.remove(); }, 1300);
  }

  function explode(keys) {
    const H = rt.m.rows;
    keys.forEach(function (k) {
      const r = Math.floor(k / H);
      const h = k % H;
      const el = cellEl(r, h);
      if (!el) return;
      el.classList.add('pop');
      const p = cellPoint(r, h);
      FX.burst(p.x, p.y, 'rgb(' + el.style.getPropertyValue('--c') + ')', 8, 220);
    });
    Sound.explode();
    buzz(10);
  }

  function dropIn(prev, next) {
    const m = rt.m;
    const H = m.rows;
    const cell = rt.ch;
    const removed = new Set(prev.removed);
    const anims = [];
    for (let r = 0; r < m.reels; r++) {
      const kept = [];
      for (let h = 0; h < H; h++) if (!removed.has(r * H + h)) kept.push(h);
      const k = H - kept.length;
      if (!k) continue;
      renderColumn(r, next[r]);
      for (let h = 0; h < H; h++) {
        const from = h < k ? h - k : kept[h - k];
        const dist = h - from;
        if (dist <= 0) continue;
        const el = cellEl(r, h);
        anims.push(el.animate([
          { transform: 'translateY(' + (-dist * cell) + 'px)', easing: 'cubic-bezier(.5,0,.9,.5)' },
          { transform: 'translateY(' + (cell * 0.07) + 'px)', offset: 0.85, easing: 'ease-out' },
          { transform: 'translateY(0)' }
        ], { duration: (state.settings.turbo ? 160 : 240) + dist * 45, delay: r * 22 }).finished);
      }
    }
    Sound.drop();
    return Promise.all(anims).catch(function () {});
  }

  function buildLadder() {
    const lad = $('#ladder');
    const m = rt.m;
    if (m.mode !== 'cluster') { lad.hidden = true; lad.innerHTML = ''; return; }
    lad.hidden = false;
    lad.innerHTML = m.cluster.ladder.map(function (v, i) {
      return '<span class="lad' + (i === 0 ? ' on' : '') + '">×' + v + '</span>';
    }).join('');
  }

  function setLadder(i, pop) {
    if (!rt.m || rt.m.mode !== 'cluster') return;
    const chips = $$('#ladder .lad');
    if (!chips.length || chips[0].classList.contains('lad-fs')) { buildLadder(); return setLadder(i, pop); }
    const k = Math.min(i, chips.length - 1);
    chips.forEach(function (c, j) { c.classList.toggle('on', j === k); });
    if (pop) bump(chips[k], 'pop');
  }

  function showLadderFs(v, pop) {
    const lad = $('#ladder');
    lad.hidden = false;
    let chip = $('.lad-fs', lad);
    if (!chip) { lad.innerHTML = '<span class="lad lad-fs on"></span>'; chip = $('.lad-fs', lad); }
    chip.textContent = 'Multiplier ×' + v;
    if (pop) bump(chip, 'pop');
  }

  async function presentCascade(res, opts) {
    const fast = state.settings.turbo || rt.auto > 0;
    const all = [];
    let running = opts.free ? rt.winShown : 0;
    rt.cascading = true;
    try {
      for (let i = 0; i < res.steps.length; i++) {
        const st = res.steps[i];
        if (i > 0) await dropIn(res.steps[i - 1], st.grid);
        if (!st.wins.length) break;
        if (opts.free) showLadderFs(st.mult, i > 0); else setLadder(i, i > 0);
        const keys = new Set();
        st.wins.forEach(function (w) {
          all.push(w);
          w.cells.forEach(function (c) { keys.add(keyOf(c[0], c[1])); });
          floatAmount(w);
        });
        highlight(keys);
        $('#cabinet').classList.add('party');
        Sound.win(Math.min(2, i));
        if (i > 0) Sound.multiplier(i);
        buzz(12);
        running += st.win;
        rollWin(running, fast ? 350 : 700);
        setMsg(st.wins.length > 1 ? st.wins.length + ' clusters' + (st.mult > 1 ? ' ×' + st.mult : '') : describe(st.wins[0]) + (st.mult > 1 ? ' (×' + st.mult + ')' : ''));
        await sleep(fast ? 450 : 900);
        explode(st.removed);
        await sleep(fast ? 180 : 280, false);
        clearHighlight();
      }
    } finally {
      rt.cascading = false;
    }
    rt.grid = res.finalGrid;
    if (res.scatter.pay > 0) {
      running += res.scatter.pay;
      highlight(new Set(res.scatter.finalCells.map(function (c) { return keyOf(c[0], c[1]); })));
      setMsg(res.scatter.count + ' × ' + symLabel(rt.m, 'scatter') + ' scatter pays ' + fmt(res.scatter.pay));
      await rollWin(running, 500);
    }
    if (rt.layoutPending) { rt.layoutPending = false; layout(); }
    return all;
  }

  async function presentFire(res) {
    const cab = $('#cabinet');
    Sound.fire();
    buzz([30, 40, 70]);
    shake();
    cab.classList.add('on-fire');
    const sweep = document.createElement('div');
    sweep.className = 'fire-sweep';
    $('#reel-fx').appendChild(sweep);
    setMsg('Dragon Fire!', true);
    await sleep(420, false);
    for (let i = 0; i < res.fire.length; i++) {
      const r = res.fire[i][0];
      const h = res.fire[i][1];
      const old = cellEl(r, h);
      const el = htmlEl(symHTML(rt.m, 'wild'));
      el.classList.add('ignite');
      if (old) old.replaceWith(el);
      const p = cellPoint(r, h);
      FX.burst(p.x, p.y, '#ff7a1a', 16, 260);
      FX.burst(p.x, p.y, '#ffd84d', 8, 160);
      Sound.ignite();
      await sleep(state.settings.turbo ? 70 : 150, false);
    }
    await sleep(380, false);
    sweep.remove();
    cab.classList.remove('on-fire');
    rt.grid = res.featureGrid;
  }

  /* --------------------------------------------------------- spin flow */

  async function playResult(res, opts) {
    opts = opts || {};
    const m = rt.m;
    stopCycle();
    clearHighlight();
    $('#cabinet').classList.remove('party');
    if (!opts.free) {
      rt.winShown = 0;
      $('#winbar').classList.remove('has-win');
      setMsg('Good luck!');
      if (m.mode === 'cluster') setLadder(0);
    }
    await spinReels(res.grid);
    if (res.fire) await presentFire(res);
    let all = [];
    if (m.mode === 'cluster') all = await presentCascade(res, opts);
    else all = presentWins(res);

    if (res.win > 0) {
      const tier = E.winTier(res.win, res.bet);
      const big = tier && (!opts.free || tier.x >= 50);
      const target = (opts.free ? (m.mode === 'cluster' ? rt.winShown : rt.winShown + res.win) : res.win);
      if (big) {
        if (m.mode !== 'cluster') rollWin(target, 300);
        await sleep(350);
        await bigWin(res.win, res.bet);
      } else if (m.mode !== 'cluster') {
        if (all.length === 1) setMsg(describe(all[0]));
        else setMsg(all.length + ' wins!');
        await rollWin(target, countTime(res.win, res.bet));
      }
      addShown(res.win, 450);
      if (res.capped) toast('Max win reached!');
    } else if (!opts.free) {
      const hs = hints(m);
      setMsg(hs[rt.hint++ % hs.length]);
    }
    setTimeout(function () { $('#cabinet').classList.remove('party'); }, 1200);
    rt.lastWins = m.mode === 'cluster' ? [] : all;
    return all;
  }

  function forcedResult(m, bet) {
    const kind = rt.force;
    if (!kind) return null;
    rt.force = null;
    const tests = {
      fs: function (r) { return r.scatter.spins > 0; },
      jackpot: function (r) { return !!r.jackpot; },
      mini: function (r) { return r.jackpot === 'mini'; },
      minor: function (r) { return r.jackpot === 'minor'; },
      major: function (r) { return r.jackpot === 'major'; },
      grand: function (r) { return r.jackpot === 'grand'; },
      big: function (r) { return r.win >= 10 * bet && r.win < 25 * bet && !r.jackpot && !r.scatter.spins; },
      mega: function (r) { return r.win >= 25 * bet && r.win < 50 * bet && !r.jackpot && !r.scatter.spins; },
      epic: function (r) { return r.win >= 50 * bet && r.win < 100 * bet && !r.jackpot && !r.scatter.spins; },
      legendary: function (r) { return r.win >= 100 * bet && !r.jackpot && !r.scatter.spins; },
      fire: function (r) { return !!r.fire && r.win > 0 && !r.jackpot && !r.scatter.spins; },
      cascade: function (r) { return r.steps.length >= 4 && !r.jackpot && !r.scatter.spins; },
      win: function (r) { return r.win > 0 && !r.jackpot && !r.scatter.spins; },
      tease: function (r) { return !r.jackpot && r.coins.count === 2 && r.scatter.count === 2; }
    };
    const test = tests[kind];
    if (!test) return null;
    const factor = /^(jackpot|mini|minor|major|grand)$/.test(kind) ? 50 : jpFactor();
    for (let i = 0; i < 3000000; i++) {
      const r = E.spin(m, { mode: 'base', bet: bet, jackpotFactor: factor }, rng);
      if (test(r)) return r;
    }
    return null;
  }

  async function doSpin() {
    const m = rt.m;
    if (!m || rt.screen !== 'machine') return;
    if (rt.busy) { hurry(); return; }
    closeAutoMenu();
    const bet = betOf(m);
    if (state.balance < bet) { stopAuto(); insufficient(); return; }
    Sound.unlock();
    setBusy(true);
    try {
      state.balance -= bet;
      const st = state.stats;
      st.spins++;
      st.wagered += bet;
      E.growPools(m, state.pools[m.id]);
      const res = forcedResult(m, bet) || E.spin(m, { mode: 'base', bet: bet, jackpotFactor: jpFactor() }, rng);
      let jp = 0;
      let hold = null;
      let poolsView = null;
      if (res.jackpot) {
        hold = {};
        hold[res.jackpot] = state.pools[m.id][res.jackpot];
        poolsView = Object.assign({}, state.pools[m.id]);
        jp = E.awardJackpot(m, state.pools[m.id], res.jackpot, bet);
        st.jackpots[res.jackpot]++;
      }
      let fs = null;
      if (res.scatter.spins) {
        fs = E.createFreeSpins(m, res.scatter.spins, bet);
        state.pendingFs = { machine: m.id, fs: fs };
        st.freeSpins++;
      }
      state.balance = Math.min(MAX_COINS, state.balance + res.win + jp);
      st.won += res.win + jp;
      track(res.win + jp, bet, m);
      save();
      addShown(-bet, 250);
      renderJackpots(true, hold);
      await playResult(res, {});
      if (res.jackpot) await jackpotSequence(res, jp, poolsView);
      if (fs) {
        await celebrateScatters(res);
        await runFreeSpins(fs, false);
      }
      syncShown();
    } finally {
      setBusy(false);
      save();
    }
    afterSpin();
  }

  function afterSpin() {
    if (rt.auto > 0) {
      if (rt.auto !== Infinity) rt.auto--;
      renderAuto();
      if (rt.auto > 0) {
        if (state.balance < betOf(rt.m)) { stopAuto(); insufficient(); return; }
        clearTimeout(rt.autoTimer);
        rt.autoTimer = setTimeout(function () {
          if (rt.auto > 0 && !rt.busy && rt.screen === 'machine' && !rt.modal) doSpin();
        }, state.settings.turbo ? 150 : 420);
        return;
      }
    }
    if (rt.lastWins.length) startCycle(rt.lastWins);
  }

  function setBusy(b) {
    rt.busy = b;
    $('#btn-back').disabled = b;
    if (rt.m) renderBet();
    renderSpinBtn();
  }

  function renderSpinBtn() {
    const btn = $('#btn-spin');
    const lab = $('#spin-label');
    const sub = $('#spin-sub');
    btn.classList.toggle('busy', rt.busy);
    let l = 'Spin';
    let s = '';
    if (rt.fs) { l = 'Free'; s = Math.max(0, rt.fs.spins - rt.fs.played) + ' left'; }
    else if (rt.auto > 0) { l = 'Stop'; s = rt.auto === Infinity ? 'auto ∞' : 'auto ' + rt.auto; }
    else if (rt.spinning) { l = 'Stop'; }
    else if (rt.busy) { l = 'Skip'; }
    lab.textContent = l;
    sub.textContent = s;
    btn.setAttribute('aria-label', l === 'Spin' ? 'Spin' : l === 'Free' ? 'Free spins in progress, tap to speed up' : l === 'Stop' ? 'Stop' : 'Skip');
  }

  function onSpinButton() {
    Sound.unlock();
    if (rt.auto > 0 && !rt.fs) {
      stopAuto();
      if (rt.busy) hurry();
      return;
    }
    if (rt.busy) { hurry(); return; }
    Sound.click();
    doSpin();
  }

  function insufficient() {
    const m = rt.m;
    let i = state.bets[m.id];
    while (i > 0 && BETS[i] > state.balance) i--;
    if (BETS[i] <= state.balance && i !== state.bets[m.id]) {
      state.bets[m.id] = i;
      renderBet();
      renderJackpots(true);
      toast('Not enough coins. Bet lowered to ' + short(BETS[i]), true);
      Sound.error();
      save();
    } else if (BETS[i] > state.balance) {
      openModal('modal-broke');
    }
  }

  /* -------------------------------------------------------------- auto */

  function renderAuto() {
    const on = rt.auto > 0;
    $('#btn-auto').classList.toggle('on', on);
    setText($('#auto-label'), on ? (rt.auto === Infinity ? '∞' : String(rt.auto)) : 'Auto');
    renderSpinBtn();
  }
  function stopAuto() {
    rt.auto = 0;
    clearTimeout(rt.autoTimer);
    renderAuto();
  }
  function closeAutoMenu() {
    $('#auto-menu').hidden = true;
    $('#btn-auto').setAttribute('aria-expanded', 'false');
  }

  /* -------------------------------------------------------- free spins */

  async function celebrateScatters(res) {
    const keys = new Set(res.scatter.finalCells.map(function (c) { return keyOf(c[0], c[1]); }));
    highlight(keys);
    $('#cabinet').classList.add('party');
    setMsg(res.scatter.count + ' × ' + symLabel(rt.m, 'scatter') + ' Free spins!', true);
    res.scatter.finalCells.forEach(function (c) {
      const p = cellPoint(c[0], c[1]);
      FX.burst(p.x, p.y, 'rgb(' + rgb(rt.m.symbols.scatter.color) + ')', 18, 280);
    });
    Sound.scatter(5);
    buzz([20, 30, 20]);
    await sleep(1300);
    clearHighlight();
  }

  function renderFs() {
    if (!rt.fs) return;
    setText($('#fs-left'), String(Math.max(0, rt.fs.spins - rt.fs.played)));
    renderSpinBtn();
  }

  function setSticky(cells) {
    clearSticky();
    cells.forEach(function (c) { addSticky(c, false); });
  }
  function addSticky(c, animate) {
    const el = htmlEl(symHTML(rt.m, 'wild'));
    el.dataset.k = keyOf(c[0], c[1]);
    el.dataset.r = c[0];
    el.dataset.h = c[1];
    if (animate) el.classList.add('lock');
    $('#sticky').appendChild(el);
    placeSticky(el);
    rt.sticky.push([c[0], c[1]]);
  }
  function placeSticky(el) {
    el.style.left = (+el.dataset.r * (rt.cw + rt.gap)) + 'px';
    el.style.top = (+el.dataset.h * rt.ch) + 'px';
  }
  function positionSticky() { $$('#sticky .cell').forEach(placeSticky); }
  function clearSticky() { $('#sticky').innerHTML = ''; rt.sticky = []; }
  async function lockSticky(cells) {
    for (let i = 0; i < cells.length; i++) {
      addSticky(cells[i], true);
      Sound.lock();
      const p = cellPoint(cells[i][0], cells[i][1]);
      FX.burst(p.x, p.y, '#ff8a1a', 12, 200);
      await sleep(150, false);
    }
  }

  async function runFreeSpins(fs, resumed) {
    const m = rt.m;
    rt.fs = fs;
    renderFs();
    renderBet();
    if (!resumed) await featureIntro(fs);
    else toast('Welcome back! Finishing your free spins');
    $('#machine').classList.add('in-fs');
    $('#fs-banner').hidden = false;
    rt.winShown = fs.total;
    $('#winbar').classList.add('has-win');
    $('#win-label').textContent = 'Total win';
    $('#win-value')._len = -1;
    rollTo($('#win-value'), fs.total, 0);
    setMsg(m.freeSpins.title, true);
    if (m.freeSpins.sticky) setSticky(fs.sticky);
    if (m.mode === 'cluster') showLadderFs(E.ladderValue(m, 'free', fs.ladder));
    requestAnimationFrame(function () { layout(); refit(); });
    while (!fs.done) {
      await sleep(state.settings.turbo ? 200 : 450, false);
      const res = E.freeSpin(m, fs, rng);
      state.balance = Math.min(MAX_COINS, state.balance + res.win);
      state.stats.won += res.win;
      save();
      renderFs();
      await playResult(res, { free: true });
      if (res.newSticky && res.newSticky.length) await lockSticky(res.newSticky);
      if (res.scatter.spins) await retrigger(res);
      if (m.mode === 'cluster') showLadderFs(E.ladderValue(m, 'free', fs.ladder));
    }
    track(fs.total, fs.bet, m);
    state.pendingFs = null;
    save();
    await featureOutro(fs);
    $('#machine').classList.remove('in-fs');
    $('#fs-banner').hidden = true;
    clearSticky();
    rt.fs = null;
    renderBet();
    renderSpinBtn();
    if (m.mode === 'cluster') buildLadder();
    $('#win-label').textContent = 'Free spins won';
    setMsg('Free spins paid ' + (fs.total / fs.bet).toFixed(1) + '× your bet');
    requestAnimationFrame(function () { layout(); refit(); });
  }

  async function resumeFreeSpins() {
    const p = state.pendingFs;
    if (!p || !rt.m || p.machine !== rt.m.id || rt.busy) return;
    setBusy(true);
    try {
      await runFreeSpins(p.fs, true);
      syncShown();
    } finally {
      setBusy(false);
      save();
    }
  }

  async function retrigger(res) {
    const keys = new Set(res.scatter.finalCells.map(function (c) { return keyOf(c[0], c[1]); }));
    highlight(keys);
    Sound.retrigger();
    toast('+' + res.scatter.spins + ' free spins!');
    bump($('#fs-banner'));
    FX.confetti(50);
    await sleep(1100);
    clearHighlight();
  }

  async function featureIntro(fs) {
    const m = rt.m;
    const ov = $('#ov-feature');
    $('#ft-kicker').textContent = 'You won';
    $('#ft-title').textContent = 'Free spins';
    $('#ft-sub').textContent = m.freeSpins.title;
    $('#ft-btn').textContent = 'Start';
    ov.hidden = false;
    const big = $('#ft-big');
    big._len = -1;
    big._t = null;
    setText(big, String(fs.spins));
    bump(big, 'go');
    Sound.duck(true);
    Sound.freeSpins();
    flash();
    FX.confetti(110);
    buzz([20, 40, 20, 40, 60]);
    await waitTap(ov, rt.auto ? 2200 : 9000);
    ov.hidden = true;
    Sound.duck(false);
  }

  async function featureOutro(fs) {
    const ov = $('#ov-feature');
    $('#ft-kicker').textContent = 'Free spins complete';
    $('#ft-title').textContent = 'Total win';
    $('#ft-sub').textContent = (fs.total / fs.bet).toFixed(1) + '× your bet' + (fs.retriggers ? ' with ' + fs.retriggers + ' retrigger' + (fs.retriggers > 1 ? 's' : '') : '');
    $('#ft-btn').textContent = 'Collect';
    ov.hidden = false;
    const big = $('#ft-big');
    big._len = -1;
    big._t = null;
    setText(big, '0');
    Sound.duck(true);
    Sound.fanfare(1);
    if (fs.total > 0) FX.coinShower(2.5, 22);
    await tween(state.settings.turbo ? 1200 : 2400, function (p) { setText(big, fmt(fs.total * p)); }, easeOut);
    if (E.winTier(fs.total, fs.bet)) { FX.fireworks(4); Sound.coins(); }
    await waitTap(ov, rt.auto ? 2000 : 9000);
    ov.hidden = true;
    FX.stop();
    Sound.duck(false);
  }

  /* ---------------------------------------------------------- big wins */

  const RANK = { big: 0, mega: 1, epic: 2, legendary: 3 };

  async function bigWin(amount, bet, opts) {
    opts = opts || {};
    const ov = $('#ov-bigwin');
    const title = $('#bw-title');
    const amt = $('#bw-amount');
    const xEl = $('#bw-x');
    const final = opts.title ? null : E.winTier(amount, bet);
    const durs = { big: 2800, mega: 4200, epic: 5600, legendary: 7200 };
    let dur = opts.duration || durs[final ? final.id : 'mega'];
    if (state.settings.turbo || rt.auto) dur *= 0.6;
    let cur = 'big';
    ov.className = 'overlay ov-bigwin ' + (opts.cls || 'tier-big');
    title.textContent = opts.title || 'Big win';
    ov.hidden = false;
    amt._len = -1;
    amt._t = null;
    setText(amt, '0');
    xEl.textContent = '';
    $('#bw-hint').textContent = 'Tap to speed up';
    Sound.duck(true);
    Sound.fanfare(opts.level || 0);
    buzz([30, 50, 30]);
    flash();
    shake();
    FX.fountain(dur / 1000 * 0.85, REDUCED ? 6 : 26);
    FX.coinShower(dur / 1000, REDUCED ? 4 : 14);
    if (opts.fireworks) FX.fireworks(opts.fireworks);
    let lastClink = 0;
    await tween(dur, function (p) {
      const v = amount * p;
      setText(amt, fmt(v));
      if (!opts.noX) xEl.textContent = (v / bet).toFixed(v / bet >= 100 ? 0 : 1) + '× bet';
      if (!opts.title) {
        const t = E.winTier(v, bet);
        if (t && RANK[t.id] > RANK[cur]) {
          cur = t.id;
          ov.className = 'overlay ov-bigwin tier-' + cur;
          title.textContent = t.label;
          bump(title);
          Sound.tierUp(RANK[cur]);
          FX.firework();
          buzz(40);
        }
      }
      const now = performance.now();
      if (p < 1 && now - lastClink > 90) { lastClink = now; Sound.clink(); }
    }, easeOutQuad);
    FX.fireworks(3);
    Sound.coins();
    $('#bw-hint').textContent = 'Tap to continue';
    await waitTap(ov, rt.auto ? 1600 : 6000);
    ov.hidden = true;
    FX.stop();
    Sound.duck(false);
  }

  /* ------------------------------------------------------------ wheels */

  function drawWheel(cv, segs) {
    const dpr = Math.min(2, window.devicePixelRatio || 1);
    const size = Math.round(clamp((cv.clientWidth || 320) * dpr, 320, 960));
    cv.width = cv.height = size;
    const g = cv.getContext('2d');
    const r = size / 2;
    const n = segs.length;
    const a = Math.PI * 2 / n;
    g.clearRect(0, 0, size, size);
    g.save();
    g.translate(r, r);
    for (let i = 0; i < n; i++) {
      const s0 = -Math.PI / 2 + i * a;
      g.beginPath();
      g.moveTo(0, 0);
      g.arc(0, 0, r * 0.93, s0, s0 + a);
      g.closePath();
      g.fillStyle = segs[i].color;
      g.fill();
      g.lineWidth = r * 0.014;
      g.strokeStyle = 'rgba(255, 236, 160, 0.95)';
      g.stroke();
    }
    const sh = g.createRadialGradient(0, 0, r * 0.1, 0, 0, r * 0.93);
    sh.addColorStop(0, 'rgba(255,255,255,0.32)');
    sh.addColorStop(0.55, 'rgba(255,255,255,0)');
    sh.addColorStop(1, 'rgba(0,0,0,0.35)');
    g.beginPath();
    g.arc(0, 0, r * 0.93, 0, Math.PI * 2);
    g.fillStyle = sh;
    g.fill();
    g.textAlign = 'center';
    g.textBaseline = 'middle';
    g.lineJoin = 'round';
    for (let i = 0; i < n; i++) {
      g.save();
      g.rotate(-Math.PI / 2 + (i + 0.5) * a + Math.PI / 2);
      const big = n <= 8 ? 0.1 : 0.082;
      g.font = Math.round(r * big) + 'px Bungee, "Arial Black", Impact, sans-serif';
      g.lineWidth = r * 0.025;
      g.strokeStyle = 'rgba(0,0,0,0.6)';
      g.fillStyle = '#fff';
      g.strokeText(segs[i].label, 0, -r * 0.72);
      g.fillText(segs[i].label, 0, -r * 0.72);
      if (segs[i].sub) {
        g.font = '800 ' + Math.round(r * 0.08) + 'px "Barlow Condensed", "Arial Narrow", sans-serif';
        g.fillStyle = '#fff3bf';
        g.strokeText(segs[i].sub, 0, -r * 0.58);
        g.fillText(segs[i].sub, 0, -r * 0.58);
      }
      g.restore();
    }
    const rim = g.createLinearGradient(-r, -r, r, r);
    rim.addColorStop(0, '#fff3bf');
    rim.addColorStop(0.5, '#ffb21e');
    rim.addColorStop(1, '#a85a0c');
    g.beginPath();
    g.arc(0, 0, r * 0.965, 0, Math.PI * 2);
    g.lineWidth = r * 0.065;
    g.strokeStyle = rim;
    g.stroke();
    for (let i = 0; i < n * 2; i++) {
      const ang = -Math.PI / 2 + i * a / 2;
      g.beginPath();
      g.arc(Math.cos(ang) * r * 0.965, Math.sin(ang) * r * 0.965, r * 0.02, 0, Math.PI * 2);
      g.fillStyle = i % 2 ? '#fffbe0' : '#ff7a1a';
      g.fill();
    }
    g.restore();
  }

  function spinWheel(cv, n, idx, u, ms) {
    return new Promise(function (resolve) {
      const segDeg = 360 / n;
      const alpha = (idx + u) * segDeg;
      const from = rt.wheelRot;
      const base = from + 360 * (ms > 4000 ? 7 : 5);
      const target = base + (((360 - alpha - (base % 360)) % 360) + 360) % 360;
      const ptr = $('#wheel-pointer');
      const t0 = performance.now();
      let lastSeg = -1;
      const frame = function (t) {
        const p = Math.min(1, (t - t0) / ms);
        const e = 1 - Math.pow(1 - p, 4);
        const rot = from + (target - from) * e;
        cv.style.transform = 'rotate(' + rot.toFixed(2) + 'deg)';
        const seg = Math.floor((((-rot % 360) + 360) % 360) / segDeg);
        if (seg !== lastSeg) {
          if (lastSeg >= 0) { Sound.wheelTick(); bump(ptr, 'tick'); }
          lastSeg = seg;
        }
        if (p < 1) requestAnimationFrame(frame);
        else {
          rt.wheelRot = target % 360;
          cv.style.transform = 'rotate(' + rt.wheelRot.toFixed(2) + 'deg)';
          resolve();
        }
      };
      requestAnimationFrame(frame);
    });
  }

  async function wheelOverlay(o) {
    const ov = $('#ov-wheel');
    const cv = $('#wheel');
    const hub = $('#wheel-hub');
    $('#wheel-title').textContent = o.title;
    $('#wheel-sub').textContent = o.sub;
    ov.hidden = false;
    drawWheel(cv, o.segs);
    rt.wheelRot = Math.random() * 360;
    cv.style.transform = 'rotate(' + rt.wheelRot + 'deg)';
    hub.disabled = false;
    hub.classList.add('ready');
    Sound.duck(true);
    await waitTap(ov, rt.auto ? 900 : 15000);
    hub.disabled = true;
    hub.classList.remove('ready');
    $('#wheel-sub').textContent = 'Good luck!';
    Sound.click();
    await spinWheel(cv, o.segs.length, o.idx, o.u, state.settings.turbo ? 3800 : 5800);
    Sound.wheelStop();
    buzz(60);
    $('#wheel-sub').textContent = o.result;
    await sleep(1200, false);
    ov.hidden = true;
    Sound.duck(false);
  }

  async function jackpotSequence(res, amount, pools) {
    const m = rt.m;
    const tier = m.jackpot.tiers.filter(function (t) { return t.id === res.jackpot; })[0];
    const coins = res.coins.finalCells;
    clearHighlight();
    stopCycle();
    highlight(new Set(coins.map(function (c) { return keyOf(c[0], c[1]); })));
    $('#cabinet').classList.add('party');
    const banner = document.createElement('div');
    banner.className = 'jackpot-banner';
    banner.textContent = 'Jackpot!';
    $('#reel-fx').appendChild(banner);
    setMsg('Jackpot coins! Spin the wheel', true);
    Sound.alarm();
    flash();
    shake();
    buzz([60, 40, 60, 40, 120]);
    coins.forEach(function (c) {
      const p = cellPoint(c[0], c[1]);
      FX.burst(p.x, p.y, '#ffd84d', 22, 320);
      FX.coinsAt(p.x, p.y, 6);
    });
    await sleep(1700);
    banner.remove();
    clearHighlight();

    const bet = res.bet;
    const segs = JP_WHEEL.map(function (id) { return { label: id.toUpperCase(), sub: short(pools[id] * bet), color: TIER_COLORS[id] }; });
    const cands = [];
    JP_WHEEL.forEach(function (id, i) { if (id === res.jackpot) cands.push(i); });
    let idx = cands[Math.floor(Math.random() * cands.length)];
    let u = 0.2 + Math.random() * 0.6;
    // A mini that stops right next to the grand makes for a good groan.
    if (res.jackpot === 'mini' && Math.random() < 0.4) {
      if (Math.random() < 0.5) { idx = 6; u = 0.86 + Math.random() * 0.1; } else { idx = 8; u = 0.04 + Math.random() * 0.1; }
    }
    await wheelOverlay({
      title: 'Jackpot wheel',
      sub: 'Tap anywhere to spin',
      segs: segs,
      idx: idx,
      u: u,
      result: tier.name + ' jackpot!'
    });
    const meter = $('#jp-' + tier.id).closest('.jp');
    meter.classList.add('hit');
    setTimeout(function () { meter.classList.remove('hit'); }, 3200);
    const durs = { mini: 3200, minor: 4400, major: 6000, grand: 8500 };
    if (tier.id === 'grand' || tier.id === 'major') FX.confetti(tier.id === 'grand' ? 200 : 120);
    Sound.jackpot(['mini', 'minor', 'major', 'grand'].indexOf(tier.id));
    await bigWin(amount, bet, { title: tier.name + ' jackpot', cls: 'jp-' + tier.id, level: 3, duration: durs[tier.id], fireworks: tier.id === 'grand' ? 8 : tier.id === 'major' ? 5 : 2 });
    addShown(amount, 600);
    renderJackpots(true);
    rt.winShown = res.win + amount;
    rollWin(rt.winShown, 400);
    $('#win-label').textContent = 'Win + jackpot';
    setMsg(tier.name + ' jackpot: ' + fmt(amount));
  }

  async function bonusWheel() {
    if (rt.busy) return;
    Sound.unlock();
    const left = state.bonusAt - Date.now();
    if (left > 0) {
      Sound.error();
      toast('The free coin wheel comes back in ' + Math.ceil(left / 60000) + ' min');
      return;
    }
    rt.busy = true;
    try {
      let total = 0;
      BONUS_WHEEL.forEach(function (b) { total += b.w; });
      let x = Math.random() * total;
      let idx = 0;
      for (; idx < BONUS_WHEEL.length - 1; idx++) { if (x < BONUS_WHEEL[idx].w) break; x -= BONUS_WHEEL[idx].w; }
      const prize = BONUS_WHEEL[idx].v;
      state.bonusAt = Date.now() + BONUS_EVERY;
      state.balance = Math.min(MAX_COINS, state.balance + prize);
      save();
      await wheelOverlay({
        title: 'Free coin wheel',
        sub: 'Tap anywhere to spin',
        segs: BONUS_WHEEL.map(function (b) { return { label: short(b.v), color: b.c }; }),
        idx: idx,
        u: 0.2 + Math.random() * 0.6,
        result: 'You won ' + fmt(prize) + ' coins!'
      });
      await bigWin(prize, prize, { title: 'Free coins', cls: 'jp-minor', duration: 2600, noX: true });
      showBalance(state.balance, 800);
    } finally {
      rt.busy = false;
      renderLobby();
    }
  }

  /* ------------------------------------------------------ control room */

  function openModal(id) {
    const el = $('#' + id);
    el.hidden = false;
    rt.modal = el;
    stopAuto();
    const f = el.querySelector('[data-close]') || el.querySelector('button');
    if (f) { try { f.focus({ preventScroll: true }); } catch (e) { f.focus(); } }
  }
  function closeModal(el) {
    el.hidden = true;
    if (rt.modal === el) rt.modal = null;
  }

  function freqName(f) {
    if (f < 0.1) return 'Nearly impossible';
    if (f < 0.4) return 'Very rare';
    if (f < 0.85) return 'Rare';
    if (f <= 1.18) return 'Normal';
    if (f <= 3) return 'Frequent';
    if (f <= 12) return 'Very frequent';
    return 'Jackpot party';
  }

  function openSettings() {
    rt.oddsId = rt.m ? rt.m.id : (state.last || 'neon');
    syncSettings();
    openModal('modal-settings');
  }

  function syncSettings() {
    $('#jp-freq').value = state.settings.jackpotLog;
    renderFreq();
    $('#bal-input').value = String(Math.round(state.balance));
    $('#bal-slider').value = balToSlider(state.balance);
    $('#bal-out').textContent = fmt(state.balance);
    $('#opt-sound').checked = state.settings.sound;
    $('#opt-music').checked = state.settings.music;
    $('#opt-vibrate').checked = state.settings.vibrate;
    $('#opt-turbo').checked = state.settings.turbo;
    renderStats();
  }

  function renderFreq() {
    const f = jpFactor();
    $('#jp-freq-out').textContent = '×' + (f >= 10 ? f.toFixed(0) : f >= 1 ? f.toFixed(1) : f.toFixed(2)) + ' · ' + freqName(f);
    $('#odds-tabs').innerHTML = LIST.map(function (m) {
      return '<button type="button" role="tab" data-odds="' + m.id + '" aria-selected="' + (m.id === rt.oddsId) + '">' + m.short + '</button>';
    }).join('');
    const m = MACH[rt.oddsId];
    const ch = E.jackpotChances(m, f);
    const bet = betOf(m);
    const rows = m.jackpot.tiers.map(function (t, i) { return { t: t, p: ch.ps[i] }; }).reverse();
    $('#odds-body').innerHTML = rows.map(function (o) {
      return '<tr><td class="t-' + o.t.id + '">' + o.t.name + '</td><td>' + oneIn(o.p) + ' spins</td><td>' + short(state.pools[m.id][o.t.id] * bet) + '</td></tr>';
    }).join('');
    const rtp = E.estimateRtp(m, f).total;
    const verdict = rtp >= 1
      ? '<b class="good">' + (rtp * 100).toFixed(1) + '%</b>, so you beat the house over time'
      : '<b>' + (rtp * 100).toFixed(1) + '%</b>, a house edge of ' + ((1 - rtp) * 100).toFixed(1) + '%';
    $('#odds-foot').innerHTML = 'Any jackpot: <b>' + oneIn(ch.total) + '</b> spins. Long-run return on ' + m.short + ' at ' + short(bet) + ' per spin: ' + verdict + '.';
  }

  function balToSlider(b) { return b <= 0 ? 0 : clamp(Math.log10(b), 0, 12); }
  function sliderToBal(v) {
    if (v <= 0.02) return 0;
    const x = Math.pow(10, v);
    const mag = Math.pow(10, Math.max(0, Math.floor(Math.log10(x)) - 1));
    return Math.round(x / mag) * mag;
  }
  function setCoins(v, from) {
    v = clamp(Math.round(v), 0, MAX_COINS);
    state.balance = v;
    showBalance(v, 300);
    if (from !== 'input') $('#bal-input').value = String(v);
    if (from !== 'slider') $('#bal-slider').value = balToSlider(v);
    $('#bal-out').textContent = fmt(v);
    save();
  }

  function renderStats() {
    const s = state.stats;
    const ret = s.wagered ? (s.won / s.wagered * 100).toFixed(1) + '%' : '—';
    const items = [
      ['Spins', fmt(s.spins)],
      ['Your return', ret],
      ['Wagered', short(s.wagered)],
      ['Won', short(s.won)],
      ['Biggest win', s.biggest ? short(s.biggest) + ' (' + s.biggestX.toFixed(0) + '×, ' + s.biggestOn + ')' : '—'],
      ['Free spin rounds', fmt(s.freeSpins)],
      ['Jackpots', s.jackpots.mini + ' mini · ' + s.jackpots.minor + ' minor · ' + s.jackpots.major + ' major · ' + s.jackpots.grand + ' grand']
    ];
    $('#stats').innerHTML = items.map(function (it, i) {
      return '<div' + (i === items.length - 1 ? ' style="grid-column:1/-1"' : '') + '><dt>' + it[0] + '</dt><dd>' + it[1] + '</dd></div>';
    }).join('');
  }

  function applySettings() {
    const s = state.settings;
    Sound.setSfx(s.sound && !s.muted);
    Sound.setMusic(s.music && !s.muted);
    $('#btn-turbo').setAttribute('aria-pressed', String(!!s.turbo));
    $('#btn-sound').setAttribute('aria-pressed', String(!!s.muted));
    $('#btn-sound').setAttribute('aria-label', s.muted ? 'Unmute sound' : 'Mute sound');
  }

  function renderInfo() {
    const m = rt.m || MACH[state.last || 'neon'];
    const bet = betOf(m);
    const syms = m.payingSymbols.slice().reverse();
    let pays = '';
    syms.forEach(function (id) {
      const s = m.symbols[id];
      const rows = [];
      if (m.mode === 'lines') {
        for (let n = m.reels; n >= 3; n--) if (s.pays[n]) rows.push('<li>' + n + ' in a row <b>' + short(s.pays[n] * bet / m.lines.length) + '</b></li>');
      } else if (m.mode === 'ways') {
        for (let n = m.reels; n >= 3; n--) if (s.pays[n]) rows.push('<li>' + n + ' reels <b>' + short(s.pays[n] * bet / m.waysDivisor) + '</b> a way</li>');
      } else {
        [15, 10, 7, 5].forEach(function (size) {
          const i = Math.min(size - m.cluster.min, s.pays.length - 1);
          rows.push('<li>' + size + (size === 15 ? '+' : '') + ' touching <b>' + short(s.pays[i] * bet) + '</b></li>');
        });
      }
      pays += '<div class="pay">' + symHTML(m, id) + '<ul>' + rows.join('') + '</ul></div>';
    });
    const scPays = Object.keys(m.scatter.pays).map(function (k) { return k + ' pay ' + short(m.scatter.pays[k] * bet); }).join(', ');
    const fsAward = Object.keys(m.freeSpins.award).map(function (k) { return k + ' give ' + m.freeSpins.award[k]; }).join(', ');
    const how = m.mode === 'lines'
      ? 'Wins pay left to right on ' + m.lines.length + ' paylines, starting from the first reel. Only the best win on each line pays.'
      : m.mode === 'ways'
        ? 'Matching symbols on neighbouring reels pay from the left, in any row: ' + fmt(Math.pow(m.rows, m.reels)) + ' ways to win. Every matching symbol multiplies the ways.'
        : 'Groups of ' + m.cluster.min + ' or more matching symbols touching up, down, left or right pay. Winning groups explode, new symbols fall in, and each cascade raises the multiplier (' + m.cluster.ladder.map(function (v) { return '×' + v; }).join(', ') + ').';
    const feature = m.id === 'neon'
      ? '<li>' + symHTML(m, 'scatter') + '<span><strong>Free spins:</strong> ' + fsAward + ' free spins. Every win during free spins pays <strong>×3</strong>, and stars can retrigger more.</span></li>'
      : m.id === 'dragon'
        ? '<li>' + symHTML(m, 'scatter') + '<span><strong>Free spins:</strong> ' + fsAward + ' free spins. Every wild that lands <strong>sticks</strong> until the round ends.</span></li>' +
          '<li>' + symHTML(m, 'wild') + '<span><strong>Dragon Fire:</strong> about 1 spin in ' + Math.round(1 / m.fire.chance) + ', the dragon torches 2 to 6 symbols into wilds.</span></li>'
        : '<li>' + symHTML(m, 'scatter') + '<span><strong>Free spins:</strong> ' + fsAward + ' free spins. The cascade multiplier <strong>never resets</strong> during the round; it grows by 1 with every cascade.</span></li>';
    $('#info-title').textContent = m.name;
    $('#info-body').innerHTML =
      '<p class="info-note">' + how + ' Amounts below are for your current bet of ' + short(bet) + '.</p>' +
      '<div class="pay-grid">' + pays + '</div>' +
      '<ul class="rules">' +
      '<li>' + symHTML(m, 'wild') + '<span><strong>Wild</strong> stands in for every symbol except bonus symbols and jackpot coins.</span></li>' +
      '<li>' + symHTML(m, 'scatter') + '<span><strong>' + m.symbols.scatter.name + '</strong> pays anywhere: ' + scPays + '.</span></li>' +
      feature +
      '<li>' + symHTML(m, 'coin') + '<span><strong>Jackpot coins:</strong> 3 or more spin the Jackpot Wheel for Mini, Minor, Major or Grand. Jackpots grow with every spin and scale with your bet.</span></li>' +
      '</ul>' +
      '<p class="info-note">Long-run return is about ' + (E.estimateRtp(m, 1).total * 100).toFixed(1) + '% at normal jackpot frequency (measured over millions of simulated spins). One spin can win up to ' + fmt(m.maxWin) + '× the bet.</p>';
  }

  /* ------------------------------------------------------------ wiring */

  function bind() {
    $('#btn-spin').addEventListener('click', onSpinButton);
    $('#reels-wrap').addEventListener('click', function () { if (rt.busy) hurry(); });
    $('#bet-down').addEventListener('click', function () { changeBet(-1); });
    $('#bet-up').addEventListener('click', function () { changeBet(1); });
    $('#btn-back').addEventListener('click', function () { Sound.click(); leaveMachine(); });
    $('#btn-settings').addEventListener('click', function () { Sound.click(); openSettings(); });
    $('#btn-settings-lobby').addEventListener('click', function () { Sound.click(); openSettings(); });
    $('#btn-info').addEventListener('click', function () { Sound.click(); renderInfo(); openModal('modal-info'); });
    $('#btn-bonus').addEventListener('click', bonusWheel);
    $('#btn-turbo').addEventListener('click', function () {
      state.settings.turbo = !state.settings.turbo;
      applySettings();
      Sound.click();
      toast(state.settings.turbo ? 'Turbo on' : 'Turbo off');
      save();
    });
    $('#btn-sound').addEventListener('click', function () {
      state.settings.muted = !state.settings.muted;
      Sound.unlock();
      applySettings();
      save();
    });
    $('#btn-auto').addEventListener('click', function () {
      Sound.click();
      if (rt.auto > 0) { stopAuto(); return; }
      const menu = $('#auto-menu');
      menu.hidden = !menu.hidden;
      $('#btn-auto').setAttribute('aria-expanded', String(!menu.hidden));
    });
    $$('#auto-menu [data-auto]').forEach(function (b) {
      b.addEventListener('click', function () {
        Sound.click();
        closeAutoMenu();
        rt.auto = Number(b.dataset.auto);
        renderAuto();
        if (!rt.busy) doSpin();
      });
    });
    document.addEventListener('click', function (e) {
      const menu = $('#auto-menu');
      if (!menu.hidden && !menu.contains(e.target) && !$('#btn-auto').contains(e.target)) closeAutoMenu();
    });
    $('#ov-bigwin').addEventListener('click', skipAll);
    $('#ov-feature').addEventListener('click', skipAll);

    $$('.modal').forEach(function (mod) {
      mod.addEventListener('click', function (e) { if (e.target === mod) closeModal(mod); });
      $$('[data-close]', mod).forEach(function (b) { b.addEventListener('click', function () { closeModal(mod); }); });
    });

    // Control Room: jackpot frequency
    $('#jp-freq').addEventListener('input', function (e) {
      let v = parseFloat(e.target.value);
      if (Math.abs(v) < 0.05) v = 0;
      state.settings.jackpotLog = clamp(v, JP_LOG_MIN, JP_LOG_MAX);
      renderFreq();
    });
    $('#jp-freq').addEventListener('change', save);
    $('#jp-reset').addEventListener('click', function () {
      state.settings.jackpotLog = 0;
      $('#jp-freq').value = 0;
      renderFreq();
      save();
    });
    $('#odds-tabs').addEventListener('click', function (e) {
      const b = e.target.closest('[data-odds]');
      if (!b) return;
      rt.oddsId = b.dataset.odds;
      renderFreq();
    });

    // Control Room: coins
    $('#bal-slider').addEventListener('input', function (e) { setCoins(sliderToBal(parseFloat(e.target.value)), 'slider'); });
    $('#bal-set').addEventListener('click', function () {
      const v = parseFloat($('#bal-input').value);
      if (!isFinite(v) || v < 0) { toast('Type a number of coins, like 50000000', true); return; }
      setCoins(v, 'button');
      toast('Coins set to ' + fmt(state.balance));
    });
    $('#bal-input').addEventListener('keydown', function (e) { if (e.key === 'Enter') $('#bal-set').click(); });
    $$('#modal-settings .chips button').forEach(function (b) {
      b.addEventListener('click', function () {
        Sound.click();
        if (b.dataset.add) setCoins(state.balance + Number(b.dataset.add), 'chip');
        else setCoins(Number(b.dataset.set), 'chip');
      });
    });

    // Control Room: options
    [['opt-sound', 'sound'], ['opt-music', 'music'], ['opt-vibrate', 'vibrate'], ['opt-turbo', 'turbo']].forEach(function (p) {
      $('#' + p[0]).addEventListener('change', function (e) {
        state.settings[p[1]] = e.target.checked;
        if (p[1] === 'sound' || p[1] === 'music') state.settings.muted = false;
        Sound.unlock();
        applySettings();
        save();
      });
    });
    $('#stats-reset').addEventListener('click', function () {
      state.stats = freshStats();
      renderStats();
      save();
      toast('Stats reset');
    });
    const resetBtn = $('#reset-all');
    let resetTimer = 0;
    resetBtn.addEventListener('click', function () {
      if (!resetBtn.classList.contains('confirm')) {
        resetBtn.classList.add('confirm');
        resetBtn.textContent = 'Tap again to reset everything';
        clearTimeout(resetTimer);
        resetTimer = setTimeout(function () { resetBtn.classList.remove('confirm'); resetBtn.textContent = 'Reset everything'; }, 3500);
        return;
      }
      clearTimeout(resetTimer);
      resetBtn.classList.remove('confirm');
      resetBtn.textContent = 'Reset everything';
      if (rt.busy) { toast('Wait for the spin to finish', true); return; }
      state = defaults();
      save();
      applySettings();
      closeModal($('#modal-settings'));
      if (rt.screen === 'machine') enterMachine(rt.m.id); else renderLobby();
      toast('Everything is back to the start');
    });

    // Out of coins
    $('#broke-collect').addEventListener('click', function () {
      state.balance = Math.min(MAX_COINS, state.balance + REFILL);
      showBalance(state.balance, 800);
      save();
      closeModal($('#modal-broke'));
      Sound.coins();
      FX.coinShower(1.5, 30);
    });
    $('#broke-settings').addEventListener('click', function () {
      closeModal($('#modal-broke'));
      openSettings();
    });

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') {
        if (rt.modal) { closeModal(rt.modal); return; }
        closeAutoMenu();
      }
      if (rt.modal) return;
      if (e.code === 'Space' || (e.key === 'Enter' && (!document.activeElement || document.activeElement === document.body))) {
        if (rt.screen !== 'machine' && !rt.tapper) return;
        e.preventDefault();
        if (skippers.size) skipAll();
        else if (rt.tapper) rt.tapper();
        else if (rt.screen === 'machine') onSpinButton();
      }
    });

    const unlock = function () { Sound.unlock(); };
    ['pointerdown', 'touchend', 'keydown'].forEach(function (ev) { document.addEventListener(ev, unlock, { passive: true }); });
    document.addEventListener('visibilitychange', function () {
      if (document.hidden) { stopAuto(); Sound.suspend(); } else { Sound.resume(); }
    });

    let raf = 0;
    const onResize = function () {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(function () { layout(); refit(); });
    };
    window.addEventListener('resize', onResize);
    if (window.ResizeObserver) new ResizeObserver(onResize).observe($('#stage'));
  }

  /* -------------------------------------------------------------- boot */

  function start(data) {
    state = data && data.state ? normalize(data.state) : load();
    rt.shown = state.balance;
    document.documentElement.style.setProperty('--frame-pad', FRAME_PAD + 'px');
    FX.init($('#fx'));
    buildLobby();
    bind();
    applySettings();
    renderLobby();
    Sound.music('lobby');
    if (data && data.screen && MACH[data.screen]) enterMachine(data.screen);
    setInterval(function () { if (rt.screen === 'lobby') updateBonus(); }, 1000);
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(function () { refit(); rt.laidOut = ''; layout(); });
  }

  // Small hook for testing and tinkering from the browser console.
  window.ReelRiot = {
    force: function (kind) { rt.force = kind; return 'next spin: ' + kind; },
    spin: function () { return doSpin(); },
    enter: function (id) { enterMachine(id); },
    lobby: function () { leaveMachine(); },
    state: function () { return state; },
    busy: function () { return rt.busy; }
  };

  const hot = window.claude && window.claude.hot;
  if (hot && hot.snapshot) {
    hot.snapshot(function () { return { state: state, screen: rt.screen === 'machine' && rt.m ? rt.m.id : null }; });
  }
  if (hot && hot.ready) hot.ready(start);
  else start((hot && hot.data) || {});
}());
