/*
 * Reel Riot - particle effects on a full-screen canvas: coin showers,
 * fountains, spark bursts, confetti and fireworks. The loop only runs while
 * something is on screen.
 */
(function (root) {
  'use strict';

  let cv = null;
  let cx = null;
  let W = 0;
  let H = 0;
  let parts = [];
  let emitters = [];
  let raf = 0;
  let last = 0;
  let maxParts = 320;
  let reduced = false;
  let coinSprite = null;
  let glowSprites = {};

  function init(canvas) {
    cv = canvas;
    cx = cv.getContext('2d');
    reduced = !!(root.matchMedia && root.matchMedia('(prefers-reduced-motion: reduce)').matches);
    const small = Math.min(root.innerWidth, root.innerHeight) < 600;
    maxParts = reduced ? 60 : small ? 240 : 380;
    coinSprite = makeCoin(64);
    resize();
    root.addEventListener('resize', resize);
  }

  function resize() {
    if (!cv) return;
    const dpr = Math.min(2, root.devicePixelRatio || 1);
    W = root.innerWidth;
    H = root.innerHeight;
    cv.width = Math.round(W * dpr);
    cv.height = Math.round(H * dpr);
    cv.style.width = W + 'px';
    cv.style.height = H + 'px';
    cx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  function makeCoin(size) {
    const c = document.createElement('canvas');
    c.width = c.height = size;
    const g = c.getContext('2d');
    const r = size / 2;
    const grd = g.createRadialGradient(r * 0.7, r * 0.6, r * 0.1, r, r, r);
    grd.addColorStop(0, '#fff7c4');
    grd.addColorStop(0.35, '#ffd84d');
    grd.addColorStop(0.78, '#e38f00');
    grd.addColorStop(1, '#8c4c00');
    g.fillStyle = grd;
    g.beginPath();
    g.arc(r, r, r - 1, 0, Math.PI * 2);
    g.fill();
    g.lineWidth = size * 0.06;
    g.strokeStyle = 'rgba(255, 236, 150, 0.9)';
    g.beginPath();
    g.arc(r, r, r * 0.7, 0, Math.PI * 2);
    g.stroke();
    g.fillStyle = 'rgba(125, 60, 0, 0.85)';
    g.font = '900 ' + Math.round(size * 0.5) + 'px "Arial Black", Arial, sans-serif';
    g.textAlign = 'center';
    g.textBaseline = 'middle';
    g.fillText('$', r, r + size * 0.03);
    return c;
  }

  function glow(color) {
    if (glowSprites[color]) return glowSprites[color];
    const s = 32;
    const c = document.createElement('canvas');
    c.width = c.height = s;
    const g = c.getContext('2d');
    const grd = g.createRadialGradient(s / 2, s / 2, 0, s / 2, s / 2, s / 2);
    grd.addColorStop(0, '#ffffff');
    grd.addColorStop(0.25, color);
    grd.addColorStop(1, 'rgba(0,0,0,0)');
    g.fillStyle = grd;
    g.fillRect(0, 0, s, s);
    glowSprites[color] = c;
    return c;
  }

  function add(p) {
    if (!cx || parts.length >= maxParts) return;
    p.life0 = p.life;
    parts.push(p);
    run();
  }

  function run() {
    if (!raf) {
      last = performance.now();
      raf = root.requestAnimationFrame(loop);
    }
  }

  function loop(t) {
    const dt = Math.min(0.05, (t - last) / 1000);
    last = t;
    for (let i = emitters.length - 1; i >= 0; i--) {
      const e = emitters[i];
      e.t += dt;
      e.acc += e.rate * dt;
      while (e.acc >= 1) { e.acc -= 1; e.spawn(); }
      if (e.t >= e.dur) emitters.splice(i, 1);
    }
    cx.clearRect(0, 0, W, H);
    let n = parts.length;
    for (let i = 0; i < n; i++) {
      const p = parts[i];
      p.life -= dt;
      if (p.life <= 0 || p.y > H + 80) {
        parts[i] = parts[n - 1];
        parts.pop();
        n--;
        i--;
        continue;
      }
      const k = 1 - p.drag * dt;
      p.vx *= k;
      p.vy = p.vy * k + p.g * dt;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.rot += p.vr * dt;
      draw(p);
    }
    cx.globalAlpha = 1;
    cx.globalCompositeOperation = 'source-over';
    if (parts.length || emitters.length) raf = root.requestAnimationFrame(loop);
    else { raf = 0; cx.clearRect(0, 0, W, H); }
  }

  function draw(p) {
    const a = Math.min(1, p.life / (p.fade || 0.4));
    cx.globalAlpha = a;
    if (p.type === 'coin') {
      const s = p.size;
      const sx = Math.max(0.08, Math.abs(Math.cos(p.rot)));
      const dpr = cv.width / W;
      // Squash horizontally to fake a spinning coin.
      cx.setTransform(dpr * sx, 0, 0, dpr, p.x * dpr, p.y * dpr);
      cx.drawImage(coinSprite, -s / 2, -s / 2, s, s);
      cx.setTransform(dpr, 0, 0, dpr, 0, 0);
    } else if (p.type === 'confetti') {
      cx.save();
      cx.translate(p.x, p.y);
      cx.rotate(p.rot);
      cx.scale(1, Math.cos(p.rot * 1.7));
      cx.fillStyle = p.color;
      cx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
      cx.restore();
    } else {
      cx.globalCompositeOperation = 'lighter';
      const s = p.size * (0.4 + 0.6 * a);
      cx.drawImage(glow(p.color), p.x - s, p.y - s, s * 2, s * 2);
      cx.globalCompositeOperation = 'source-over';
    }
  }

  function rand(a, b) { return a + Math.random() * (b - a); }

  function coin(x, y, vx, vy, opts) {
    add({
      type: 'coin', x: x, y: y, vx: vx, vy: vy, g: (opts && opts.g) || 900, drag: 0.4,
      rot: rand(0, 6.28), vr: rand(6, 14) * (Math.random() < 0.5 ? -1 : 1),
      size: rand(18, 34) * ((opts && opts.scale) || 1), life: (opts && opts.life) || 3, fade: 0.5
    });
  }

  const api = {
    init: init,
    resize: resize,

    // Coins falling from the top of the screen.
    coinShower: function (seconds, rate) {
      if (reduced) rate = Math.min(rate, 10);
      emitters.push({
        t: 0, acc: 0, dur: seconds, rate: rate || 40,
        spawn: function () { coin(rand(0, W), -30, rand(-60, 60), rand(50, 260), { g: 700, life: 4 }); }
      });
      run();
    },

    // Coins erupting upward from a point (defaults to bottom center).
    fountain: function (seconds, rate, x, y) {
      if (reduced) rate = Math.min(rate, 10);
      emitters.push({
        t: 0, acc: 0, dur: seconds, rate: rate || 30,
        spawn: function () {
          const px = x == null ? W / 2 : x;
          const py = y == null ? H + 20 : y;
          coin(px + rand(-30, 30), py, rand(-W * 0.45, W * 0.45), rand(-H * 1.35, -H * 0.85), { g: 1300, life: 3.2 });
        }
      });
      run();
    },

    burst: function (x, y, color, count, speed) {
      const n = reduced ? Math.min(6, count || 18) : (count || 18);
      for (let i = 0; i < n; i++) {
        const a = rand(0, Math.PI * 2);
        const v = rand(0.3, 1) * (speed || 260);
        add({ type: 'spark', x: x, y: y, vx: Math.cos(a) * v, vy: Math.sin(a) * v, g: 280, drag: 2.2, rot: 0, vr: 0, size: rand(4, 10), color: color || '#ffd84d', life: rand(0.35, 0.8), fade: 0.35 });
      }
    },

    coinsAt: function (x, y, count) {
      const n = reduced ? 3 : (count || 8);
      for (let i = 0; i < n; i++) coin(x, y, rand(-180, 180), rand(-520, -220), { g: 1100, life: 1.6, scale: 0.8 });
    },

    confetti: function (count) {
      const colors = ['#ff2e88', '#ffd84d', '#2ee8ff', '#4dff9a', '#b867ff', '#ff8a3d', '#ffffff'];
      const n = reduced ? 20 : (count || 120);
      for (let i = 0; i < n; i++) {
        add({ type: 'confetti', x: rand(0, W), y: rand(-H * 0.3, -10), vx: rand(-80, 80), vy: rand(60, 260), g: 160, drag: 0.9, rot: rand(0, 6.28), vr: rand(-8, 8), size: rand(8, 14), color: colors[i % colors.length], life: rand(3, 5), fade: 0.8 });
      }
    },

    firework: function (x, y, color) {
      const n = reduced ? 10 : 46;
      const px = x == null ? rand(W * 0.15, W * 0.85) : x;
      const py = y == null ? rand(H * 0.12, H * 0.45) : y;
      for (let i = 0; i < n; i++) {
        const a = (i / n) * Math.PI * 2;
        const v = rand(200, 330);
        add({ type: 'spark', x: px, y: py, vx: Math.cos(a) * v, vy: Math.sin(a) * v, g: 220, drag: 1.4, rot: 0, vr: 0, size: rand(5, 9), color: color || '#ffd84d', life: rand(0.9, 1.4), fade: 0.6 });
      }
    },

    fireworks: function (count, colors) {
      const cs = colors || ['#ffd84d', '#ff2e88', '#2ee8ff', '#4dff9a', '#b867ff'];
      for (let i = 0; i < count; i++) {
        setTimeout(function () { api.firework(null, null, cs[i % cs.length]); }, i * 260);
      }
    },

    stop: function () { emitters.length = 0; },
    clear: function () { emitters.length = 0; parts.length = 0; if (cx) cx.clearRect(0, 0, W, H); }
  };

  root.SlotFX = api;
}(typeof self !== 'undefined' ? self : this));
