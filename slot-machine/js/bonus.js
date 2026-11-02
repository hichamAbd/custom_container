/*
 * Reel Riot - jackpot bonus games. The engine has already decided which
 * jackpot is won (and, for Hold & Spin, every coin); each game is a show
 * that lands on that result.
 *
 *   pick(o)     Neon Fruit Frenzy: tap tiles, the first jackpot found 3 times wins
 *   holdSpin(o) Dragon's Hoard: coins lock in, every landing resets 3 respins,
 *               a full board is the Grand
 *   rocket(o)   Cosmic Cascade: Zib's rocket climbs the jackpot ladder
 *
 * o: { tier, values: {mini, minor, major, grand} in coins, bet, auto, turbo,
 *      machine (Hold & Spin), hold (engine Hold & Spin result) }
 */
(function (root) {
  'use strict';

  const TIERS = ['grand', 'major', 'minor', 'mini'];
  const LABEL = { mini: 'Mini', minor: 'Minor', major: 'Major', grand: 'Grand' };
  const RANK = { mini: 0, minor: 1, major: 2, grand: 3 };
  const HEX = { mini: '#42e682', minor: '#33b5ff', major: '#b867ff', grand: '#ff3d3d' };

  const U = function () { return root.RR.util; };
  const S = function () { return root.SlotSound; };
  const F = function () { return root.SlotFX; };
  const M = function () { return root.SlotMascots; };

  function shuffle(a) {
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      const t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }
  function center(el) {
    const b = el.getBoundingClientRect();
    return { x: b.left + b.width / 2, y: b.top + b.height / 2 };
  }
  function open(cls, html) {
    const ov = document.getElementById('ov-bonus');
    ov.className = 'overlay ov-bonus ' + cls;
    ov.innerHTML = html;
    ov.hidden = false;
    return ov;
  }
  function close() {
    const ov = document.getElementById('ov-bonus');
    ov.hidden = true;
    ov.innerHTML = '';
    F().stop();
  }
  function head(title, sub) {
    return '<div class="bn-head"><div class="bn-title">' + title + '</div><div class="bn-sub">' + sub + '</div></div>';
  }

  /* ------------------------------------------------------------ pick'em */

  // Two of the winning tier hidden among near misses, then the third.
  function pickSequence(tier) {
    const seq = [tier, tier];
    TIERS.forEach(function (t) {
      if (t === tier) return;
      const two = RANK[t] >= 2 ? 0.45 : 0.3;
      const r = Math.random();
      const n = r < two ? 2 : r < two + 0.35 ? 1 : 0;
      for (let i = 0; i < n; i++) seq.push(t);
    });
    shuffle(seq);
    seq.push(tier);
    return seq;
  }

  function coin(t) { return '<span class="pk-coin t-' + t + '"><span>' + LABEL[t] + '</span></span>'; }

  async function pick(o) {
    const u = U();
    const meters = TIERS.map(function (t) {
      return '<div class="pk-meter t-' + t + '" data-t="' + t + '"><span class="pk-name">' + LABEL[t] + '</span>' +
        '<span class="pk-val">' + u.short(o.values[t]) + '</span><span class="pk-dots"><i></i><i></i><i></i></span></div>';
    }).join('');
    let grid = '';
    for (let i = 0; i < 12; i++) {
      grid += '<button type="button" class="pk-tile" aria-label="Tile ' + (i + 1) + '"><span class="pk-inner">' +
        '<span class="pk-face pk-back"><b>?</b></span><span class="pk-face pk-front"></span></span></button>';
    }
    const ov = open('bn-pick', '<div class="bn-card">' + head('Jackpot pick', 'Tap tiles. Find 3 of the same jackpot to win it.') +
      '<div class="pk-meters">' + meters + '</div><div class="pk-grid">' + grid + '</div></div>' +
      '<div class="bn-host">' + M().html('neon') + '</div>');
    const host = ov.querySelector('.bn-host .mascot');
    const tiles = Array.prototype.slice.call(ov.querySelectorAll('.pk-tile'));
    M().mood(host, 'cheer', 1600);
    M().say(host, 'Pick a tile!', 2400);
    S().freeSpins();
    F().confetti(50);

    const seq = pickSequence(o.tier);
    const counts = { mini: 0, minor: 0, major: 0, grand: 0 };
    let k = 0;
    let busy = false;
    let autoTimer = 0;
    await new Promise(function (resolve) {
      const reveal = async function (tile) {
        if (busy || tile.classList.contains('open') || counts[o.tier] >= 3) return;
        busy = true;
        const t = seq[k++];
        tile.querySelector('.pk-front').innerHTML = coin(t);
        tile.classList.add('open', 't-' + t);
        S().flip();
        S().reveal(RANK[t]);
        u.buzz(15);
        const p = center(tile);
        F().burst(p.x, p.y, HEX[t], 16, 240);
        counts[t]++;
        const meter = ov.querySelector('.pk-meter[data-t="' + t + '"]');
        meter.querySelectorAll('i')[counts[t] - 1].classList.add('on');
        u.bump(meter);
        if (counts[t] === 2) {
          meter.classList.add('hot');
          M().mood(host, 'focus', 900);
          M().say(host, t === 'grand' ? 'One more Grand!' : 'One more ' + LABEL[t] + '!', 1300);
        } else if (counts[t] === 1 && t === 'grand') {
          M().mood(host, 'wow', 900);
        }
        await u.sleep(400, false);
        busy = false;
        if (counts[t] === 3) { clearTimeout(autoTimer); resolve(); }
      };
      tiles.forEach(function (el) { el.addEventListener('click', function () { reveal(el); }); });
      if (o.auto) {
        const auto = function () {
          if (counts[o.tier] >= 3) return;
          const closed = tiles.filter(function (e) { return !e.classList.contains('open'); });
          reveal(closed[Math.floor(Math.random() * closed.length)]);
          autoTimer = setTimeout(auto, 700);
        };
        autoTimer = setTimeout(auto, 1000);
      }
    });

    ov.querySelector('.pk-meter[data-t="' + o.tier + '"]').classList.add('won');
    tiles.forEach(function (e) { if (e.classList.contains('t-' + o.tier)) e.classList.add('win'); });
    const left = [];
    TIERS.forEach(function (t) { for (let i = counts[t]; i < 3; i++) left.push(t); });
    shuffle(left);
    tiles.filter(function (e) { return !e.classList.contains('open'); }).forEach(function (e, i) {
      e.querySelector('.pk-front').innerHTML = coin(left[i]);
      setTimeout(function () { e.classList.add('open', 'dim'); }, 500 + 110 * i);
    });
    M().base(host, 'dance');
    M().say(host, LABEL[o.tier] + ' jackpot!', 2400);
    S().jackpot(RANK[o.tier]);
    u.flash();
    F().fireworks(3, [HEX[o.tier], '#ffd84d']);
    await u.sleep(o.turbo ? 1600 : 2600);
    close();
  }

  /* -------------------------------------------------------- hold & spin */

  async function holdSpin(o) {
    const u = U();
    const m = o.machine;
    const hs = o.hold;
    const R = m.reels;
    const H = m.rows;
    let cells = '';
    for (let h = 0; h < H; h++) for (let r = 0; r < R; r++) cells += '<div class="hs-cell" data-k="' + (r * H + h) + '"></div>';
    const jps = TIERS.map(function (t) {
      return '<div class="hs-jp t-' + t + '" data-t="' + t + '"><span>' + LABEL[t] + '</span><b>' + u.short(o.values[t]) + '</b></div>';
    }).join('');
    const ov = open('bn-hold', '<div class="bn-card">' + head('Hold &amp; Spin', 'Each new coin resets the respins. Fill the board for the Grand!') +
      '<div class="hs-jps">' + jps + '</div>' +
      '<div class="hs-row"><div class="hs-respins"><span>Respins</span><span class="hs-dots"><i></i><i></i><i></i></span></div>' +
      '<div class="hs-total"><span>Collected</span><b class="hs-total-v">0</b></div></div>' +
      '<div class="hs-board" style="--R:' + R + ';--H:' + H + '">' + cells + '</div></div>' +
      '<div class="bn-host">' + M().html('dragon') + '</div>');
    const host = ov.querySelector('.bn-host .mascot');
    const board = ov.querySelector('.hs-board');
    const dots = Array.prototype.slice.call(ov.querySelectorAll('.hs-dots i'));
    const totalEl = ov.querySelector('.hs-total-v');
    const cellOf = function (c) { return board.querySelector('[data-k="' + (c.r * H + c.h) + '"]'); };
    const setRespins = function (n, reset) {
      dots.forEach(function (d, i) { d.classList.toggle('on', i < n); });
      if (reset) u.bump(ov.querySelector('.hs-respins'));
    };
    let landed = 0;
    const land = function (c) {
      const el = cellOf(c);
      el.innerHTML = c.jp
        ? '<span class="hs-coin jp t-' + c.jp + '"><span>' + LABEL[c.jp] + '</span></span>'
        : '<span class="hs-coin"><span>' + u.short(c.v * o.bet) + '</span></span>';
      el.classList.add('held');
      landed++;
      S().coin(Math.min(8, landed));
      u.buzz(12);
      const p = center(el);
      F().burst(p.x, p.y, c.jp ? HEX[c.jp] : '#ffd84d', 14, 220);
      if (c.jp) {
        u.bump(ov.querySelector('.hs-jp[data-t="' + c.jp + '"]'));
        M().mood(host, 'wow', 1500);
        M().say(host, LABEL[c.jp] + ' jackpot coin!', 1800);
      }
    };

    S().fanfare(1);
    M().mood(host, 'cheer', 1600);
    M().say(host, 'Hold & Spin!', 1800);
    setRespins(hs.respins);
    await u.sleep(o.turbo ? 300 : 700, false);
    for (let i = 0; i < hs.start.length; i++) {
      land(hs.start[i]);
      await u.sleep(o.turbo ? 110 : 230, false);
    }
    await u.sleep(o.turbo ? 300 : 800);

    for (let s = 0; s < hs.steps.length; s++) {
      const step = hs.steps[s];
      board.classList.add('spinning');
      S().spinStart();
      await u.sleep(o.turbo ? 420 : 900, false);
      board.classList.remove('spinning');
      if (step.landed.length) {
        for (let i = 0; i < step.landed.length; i++) {
          land(step.landed[i]);
          await u.sleep(o.turbo ? 110 : 240, false);
        }
        if (!step.landed.some(function (c) { return c.jp; })) M().mood(host, 'happy', 900);
        setRespins(step.respins, true);
        S().retrigger();
      } else {
        S().dud();
        setRespins(step.respins);
        if (step.respins === 1) { M().mood(host, 'focus', 1200); M().say(host, 'Last respin!', 1300); }
      }
      await u.sleep(o.turbo ? 220 : 480);
    }

    if (hs.full) {
      board.classList.add('full');
      M().base(host, 'fire');
      M().say(host, 'Board full! GRAND!', 2600);
      S().jackpot(3);
      u.flash();
      u.shake();
      F().fireworks(6);
      await u.sleep(2000, false);
    } else {
      M().base(host, 'cheer');
      M().say(host, 'Collect!', 1400);
    }

    const ordered = hs.coins.slice().sort(function (a, b) { return a.h - b.h || a.r - b.r; });
    let total = 0;
    for (let i = 0; i < ordered.length; i++) {
      const c = ordered[i];
      const el = cellOf(c);
      el.classList.add('collect');
      if (c.jp) S().reveal(3);
      else {
        total += c.v * o.bet;
        totalEl.textContent = u.fmt(total);
        S().clink();
      }
      u.bump(totalEl);
      await u.sleep(o.turbo ? 60 : 140, false);
    }
    await u.sleep(o.turbo ? 700 : 1300);
    close();
  }

  /* -------------------------------------------------------------- rocket */

  const ROCKET = '<svg viewBox="0 0 120 210" class="rk-svg" aria-hidden="true" focusable="false">' +
    '<path d="M33 136L8 182L35 172Z" fill="#ff3d6e" stroke="#7a0a2a" stroke-width="3" stroke-linejoin="round"/>' +
    '<path d="M87 136L112 182L85 172Z" fill="#ff3d6e" stroke="#7a0a2a" stroke-width="3" stroke-linejoin="round"/>' +
    '<path d="M60 6C92 36 98 96 90 174L30 174C22 96 28 36 60 6Z" fill="url(#rkBody)" stroke="#3c4470" stroke-width="3"/>' +
    '<path d="M60 6C74 20 82 34 86 50L34 50C38 34 46 20 60 6Z" fill="#ff3d6e" stroke="#7a0a2a" stroke-width="3" stroke-linejoin="round"/>' +
    '<circle cx="60" cy="92" r="22" fill="#10224a" stroke="#c9d2f5" stroke-width="6"/>' +
    '<ellipse cx="60" cy="97" rx="14" ry="12" fill="#72e46c"/><ellipse cx="54" cy="96" rx="3.5" ry="5" fill="#10131f"/><ellipse cx="66" cy="96" rx="3.5" ry="5" fill="#10131f"/>' +
    '<path d="M56 104Q60 107 64 104" stroke="#1d4a1a" stroke-width="2" fill="none" stroke-linecap="round"/>' +
    '<path d="M44 80A22 22 0 0 1 60 72" stroke="rgba(255,255,255,0.7)" stroke-width="3" fill="none" stroke-linecap="round"/>' +
    '<rect x="31" y="138" width="58" height="9" fill="#2ee8ff"/>' +
    '<path d="M42 174L78 174L72 190L48 190Z" fill="#555f96" stroke="#3c4470" stroke-width="3" stroke-linejoin="round"/>' +
    '<rect x="56" y="148" width="8" height="38" rx="3" fill="#ff3d6e" stroke="#7a0a2a" stroke-width="2"/>' +
    '</svg>';

  async function rocket(o) {
    const u = U();
    const order = ['mini', 'minor', 'major', 'grand'];
    const levelAt = function (i) { return 0.22 + i * 0.21; };
    const levels = order.map(function (t, i) {
      return '<div class="rk-level t-' + t + '" style="bottom:' + (levelAt(i) * 100).toFixed(1) + '%"><span class="rk-pill"><span class="rk-name">' + LABEL[t] + '</span>' +
        '<span class="rk-val">' + u.short(o.values[t]) + '</span></span></div>';
    }).join('');
    const ov = open('bn-rocket', '<div class="bn-card">' + head('Rocket jackpot', 'Zib blasts off. Every ring passed is a bigger jackpot!') +
      '<div class="rk-tower"><div class="rk-stars"></div>' + levels + '<div class="rk-pad"></div>' +
      '<div class="rk-rocket"><div class="rk-flame"></div>' + ROCKET + '</div><div class="rk-count"></div></div>' +
      '<button type="button" class="btn-gold rk-launch">Launch!</button></div>');
    M().ensureDefs();
    const tower = ov.querySelector('.rk-tower');
    const rk = ov.querySelector('.rk-rocket');
    const btn = ov.querySelector('.rk-launch');
    const countEl = ov.querySelector('.rk-count');
    const lv = Array.prototype.slice.call(ov.querySelectorAll('.rk-level'));
    let y = 0;
    const place = function (px) { rk.style.transform = 'translate(-50%, ' + (-px).toFixed(1) + 'px)'; };
    const targetPx = function (i) { return levelAt(i) * tower.clientHeight - rk.offsetHeight * 0.55 - tower.clientHeight * 0.03; };
    const fly = function (to, ms, ease) {
      const from = y;
      y = to;
      return u.tween(ms, function (p) { place(from + (to - from) * p); }, ease || function (p) { return p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2; }, false);
    };
    S().alarm();
    await u.waitTap(btn, o.auto ? 900 : 20000);
    btn.disabled = true;
    btn.classList.add('gone');
    for (let n = 3; n >= 1; n--) {
      countEl.textContent = String(n);
      u.bump(countEl, 'go');
      S().countdown(false);
      await u.sleep(o.turbo ? 320 : 560, false);
    }
    countEl.textContent = 'Lift off!';
    u.bump(countEl, 'go');
    S().countdown(true);
    S().launch();
    u.shake();
    u.buzz([40, 30, 60]);
    rk.classList.add('burn');
    ov.classList.add('flying');
    setTimeout(function () { countEl.classList.add('low'); }, 700);
    let flying = true;
    (function puff() {
      if (!flying) return;
      const p = center(rk.querySelector('.rk-flame'));
      F().burst(p.x, p.y + 12, Math.random() < 0.5 ? '#ffd84d' : '#ff7a1a', 4, 160);
      setTimeout(puff, 80);
    }());
    const target = RANK[o.tier];
    for (let i = 0; i <= target; i++) {
      await fly(targetPx(i), o.turbo ? 650 : 1100);
      lv[i].classList.add('lit');
      S().level(i);
      u.buzz(30);
      const p = center(lv[i].querySelector('.rk-pill'));
      F().burst(p.x, p.y, HEX[order[i]], 20, 260);
      if (i < target) {
        countEl.textContent = 'Boost!';
        u.bump(countEl, 'go');
        rk.classList.add('boost');
        await u.sleep(o.turbo ? 200 : 420, false);
        rk.classList.remove('boost');
      }
    }
    if (target < 3 && Math.random() < 0.55) {
      const stop = y + (targetPx(target + 1) - y) * (0.45 + Math.random() * 0.35);
      countEl.textContent = 'Higher...';
      u.bump(countEl, 'go');
      await fly(stop, o.turbo ? 450 : 850);
      rk.classList.add('sputter');
      S().sputter();
      await u.sleep(o.turbo ? 300 : 650, false);
      S().fall();
      await fly(targetPx(target), 650, function (p) { return 1 - Math.pow(1 - p, 3); });
      rk.classList.remove('sputter');
    }
    flying = false;
    rk.classList.remove('burn');
    ov.classList.remove('flying');
    lv[target].classList.add('won');
    countEl.textContent = LABEL[o.tier] + '!';
    u.bump(countEl, 'go');
    S().jackpot(target);
    u.flash();
    F().fireworks(target + 2, [HEX[o.tier], '#ffd84d', '#2ee8ff']);
    await u.sleep(o.turbo ? 1500 : 2400);
    close();
  }

  root.SlotBonus = { pick: pick, holdSpin: holdSpin, rocket: rocket, LABEL: LABEL };
}(typeof self !== 'undefined' ? self : this));
