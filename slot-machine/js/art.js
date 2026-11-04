/*
 * Reel Riot - painted symbol art. Draws a machine's symbols once with
 * Canvas 2D (metal and gem gradients, bevels, inner shadows, specular
 * highlights and grain) and hands the reels cached PNG images.
 *
 * Only Dragon's Hoard uses it for now; other machines keep their glyphs.
 * Shapes are drawn in a 100 x 100 box.
 */
(function (root) {
  'use strict';

  const SIZE = 240;
  const K = SIZE / 100;
  const cache = {};

  const METAL = {
    gold: [[0, '#fff4c4'], [0.16, '#ffd257'], [0.4, '#b06e08'], [0.5, '#7a4600'], [0.58, '#e3a52a'], [0.8, '#ffe08a'], [1, '#8a5200']],
    darkgold: [[0, '#ffe08a'], [0.3, '#c98a1a'], [0.55, '#6a3a05'], [0.75, '#b07818'], [1, '#4a2600']],
    silver: [[0, '#ffffff'], [0.2, '#e3e9ef'], [0.44, '#7d8996'], [0.52, '#4a5560'], [0.6, '#c7d0da'], [0.85, '#f4f7fa'], [1, '#6b7682']]
  };
  const GEM = {
    ruby: ['#ffd0da', '#ff3d63', '#c8002c', '#6a0014'],
    sapphire: ['#d6eaff', '#3d8bff', '#0a3fc0', '#041a5c'],
    emerald: ['#d6ffe9', '#2fe08a', '#06964f', '#023d1f']
  };

  function grad(g, stops) { stops.forEach(s => g.addColorStop(s[0], s[1])); return g; }
  function lin(c, x0, y0, x1, y1, stops) { return grad(c.createLinearGradient(x0, y0, x1, y1), stops); }
  function rad(c, x0, y0, r0, x1, y1, r1, stops) { return grad(c.createRadialGradient(x0, y0, r0, x1, y1, r1), stops); }
  function P(d) { return new Path2D(d); }
  function circle(x, y, r) { const p = new Path2D(); p.arc(x, y, r, 0, Math.PI * 2); return p; }
  function ellipse(x, y, rx, ry) { const p = new Path2D(); p.ellipse(x, y, rx, ry, 0, 0, Math.PI * 2); return p; }

  let noiseCanvas = null;
  function noise(c) {
    if (!noiseCanvas) {
      noiseCanvas = document.createElement('canvas');
      noiseCanvas.width = noiseCanvas.height = 96;
      const n = noiseCanvas.getContext('2d', { willReadFrequently: true });
      const img = n.createImageData(96, 96);
      for (let i = 0; i < img.data.length; i += 4) {
        const v = 110 + Math.random() * 90;
        img.data[i] = img.data[i + 1] = img.data[i + 2] = v;
        img.data[i + 3] = 255;
      }
      n.putImageData(img, 0, 0);
    }
    return c.createPattern(noiseCanvas, 'repeat');
  }

  // Fill a shape with a drop shadow, then light its inner edges and add grain.
  function solid(c, path, fill, o) {
    o = o || {};
    c.save();
    c.shadowColor = o.shadow || 'rgba(0, 0, 0, 0.55)';
    c.shadowBlur = K * (o.blur == null ? 5 : o.blur);
    c.shadowOffsetY = K * (o.drop == null ? 2.5 : o.drop);
    c.fillStyle = fill;
    c.fill(path, o.rule || 'nonzero');
    c.restore();
    if (o.grain) grain(c, path, o.grain, o.rule);
    if (o.bevel !== false) {
      inset(c, path, o.light || 'rgba(255, 255, 255, 0.75)', o.lightBlur || 1.6, 1.1, 1.3, o.rule);
      inset(c, path, o.dark || 'rgba(40, 10, 0, 0.6)', o.darkBlur || 2.2, -1.2, -1.6, o.rule);
    }
    if (o.stroke) { c.lineWidth = o.strokeWidth || 1.2; c.strokeStyle = o.stroke; c.stroke(path); }
  }
  // Inner shadow or highlight: the shadow of everything outside the shape, clipped inside it.
  function inset(c, path, color, blur, ox, oy, rule) {
    c.save();
    c.clip(path, rule || 'nonzero');
    const hole = new Path2D();
    hole.rect(-60, -60, 220, 220);
    hole.addPath(path);
    c.shadowColor = color;
    c.shadowBlur = blur * (SIZE / 100);
    c.shadowOffsetX = ox * (SIZE / 100);
    c.shadowOffsetY = oy * (SIZE / 100);
    c.fillStyle = '#000';
    c.fill(hole, 'evenodd');
    c.restore();
  }
  function grain(c, path, alpha, rule) {
    c.save();
    c.clip(path, rule || 'nonzero');
    c.globalAlpha = alpha;
    c.globalCompositeOperation = 'overlay';
    c.setTransform(1, 0, 0, 1, 0, 0);
    c.fillStyle = noise(c);
    c.fillRect(0, 0, SIZE, SIZE);
    c.restore();
  }
  function shine(c, path, x, y, r, alpha) {
    c.save();
    c.clip(path);
    c.globalCompositeOperation = 'lighter';
    c.fillStyle = rad(c, x, y, 0, x, y, r, [[0, 'rgba(255,255,255,' + alpha + ')'], [1, 'rgba(255,255,255,0)']]);
    c.fillRect(0, 0, 100, 100);
    c.restore();
  }
  function sparkle(c, x, y, s) {
    c.save();
    c.globalCompositeOperation = 'lighter';
    c.fillStyle = rad(c, x, y, 0, x, y, s * 1.6, [[0, 'rgba(255,255,240,0.9)'], [1, 'rgba(255,255,240,0)']]);
    c.fillRect(x - s * 2, y - s * 2, s * 4, s * 4);
    c.fillStyle = '#fffef5';
    c.beginPath();
    c.moveTo(x, y - s); c.quadraticCurveTo(x, y, x + s, y); c.quadraticCurveTo(x, y, x, y + s);
    c.quadraticCurveTo(x, y, x - s, y); c.quadraticCurveTo(x, y, x, y - s);
    c.fill();
    c.restore();
  }
  // A faceted gem seen from above.
  function gem(c, x, y, r, pal, sides) {
    sides = sides || 8;
    const pt = (k, rr) => { const a = -Math.PI / 2 + k * 2 * Math.PI / sides; return [x + Math.cos(a) * rr, y + Math.sin(a) * rr]; };
    const outer = new Path2D();
    for (let k = 0; k < sides; k++) { const p = pt(k, r); k ? outer.lineTo(p[0], p[1]) : outer.moveTo(p[0], p[1]); }
    outer.closePath();
    c.save();
    c.shadowColor = 'rgba(0,0,0,0.6)'; c.shadowBlur = K * (4); c.shadowOffsetY = K * (2);
    c.fillStyle = pal[3]; c.fill(outer);
    c.restore();
    for (let k = 0; k < sides; k++) {
      const a = pt(k, r), b = pt(k + 1, r), ia = pt(k, r * 0.55), ib = pt(k + 1, r * 0.55);
      const f = new Path2D();
      f.moveTo(a[0], a[1]); f.lineTo(b[0], b[1]); f.lineTo(ib[0], ib[1]); f.lineTo(ia[0], ia[1]); f.closePath();
      const lightSide = Math.cos(-Math.PI / 2 + (k + 0.5) * 2 * Math.PI / sides + 2.3);
      c.fillStyle = lightSide > 0.35 ? pal[0] : lightSide > -0.2 ? pal[1] : lightSide > -0.7 ? pal[2] : pal[3];
      c.fill(f);
      c.strokeStyle = 'rgba(255,255,255,0.25)'; c.lineWidth = 0.4; c.stroke(f);
    }
    const table = new Path2D();
    for (let k = 0; k < sides; k++) { const p = pt(k, r * 0.55); k ? table.lineTo(p[0], p[1]) : table.moveTo(p[0], p[1]); }
    table.closePath();
    c.fillStyle = lin(c, x - r, y - r, x + r, y + r, [[0, pal[0]], [0.45, pal[1]], [1, pal[2]]]);
    c.fill(table);
    shine(c, table, x - r * 0.3, y - r * 0.35, r * 0.6, 0.55);
    c.strokeStyle = 'rgba(255,255,255,0.35)'; c.lineWidth = 0.5; c.stroke(table);
    sparkle(c, x - r * 0.35, y - r * 0.4, r * 0.35);
  }
  function metal(c, name, y0, y1) { return lin(c, 0, y0, 0, y1, METAL[name]); }

  // Dragon head silhouette (facing left) used for embossed emblems.
  const EMBLEM = 'M20 54C24 46 31 42 40 40L50 32C54 26 61 22 69 20L66 28C74 26 82 28 87 34L80 36C84 42 84 50 80 55L87 62L76 62C70 70 61 74 51 72L47 79L41 71C35 69 29 67 25 63L14 65L20 58Z';
  function emblem(c, x, y, s, base) {
    c.save();
    c.translate(x, y); c.scale(s, s); c.translate(-50, -50);
    const p = P(EMBLEM);
    c.fillStyle = 'rgba(50, 20, 0, 0.85)';
    c.save(); c.translate(1.4, 1.6); c.fill(p); c.restore();
    c.fillStyle = 'rgba(255, 250, 225, 0.95)';
    c.save(); c.translate(-1, -1.1); c.fill(p); c.restore();
    c.fillStyle = base;
    c.fill(p);
    c.fillStyle = 'rgba(60, 20, 0, 0.8)';
    c.beginPath(); c.arc(62, 37, 2.6, 0, Math.PI * 2); c.fill();
    c.restore();
  }

  /* ------------------------------------------------------------ symbols */

  const DRAW = {};

  DRAW.potion = c => {
    const flask = P('M43 16H57V36C71 40 81 50 81 64A31 31 0 1 1 19 64C19 50 29 40 43 36Z');
    c.save();
    c.shadowColor = 'rgba(30, 240, 210, 0.75)'; c.shadowBlur = K * (16);
    c.fillStyle = 'rgba(160, 230, 255, 0.16)'; c.fill(flask);
    c.restore();
    c.save();
    c.clip(flask);
    const liquid = P('M10 54C24 50 34 58 50 54C66 50 76 58 92 54V100H10Z');
    c.fillStyle = rad(c, 42, 64, 2, 50, 70, 36, [[0, '#b8fff4'], [0.35, '#2ee6cc'], [0.8, '#078a7c'], [1, '#03423b']]);
    c.fill(liquid);
    c.fillStyle = 'rgba(220, 255, 250, 0.55)';
    [[38, 78, 2.6], [56, 70, 1.8], [46, 66, 1.4], [62, 82, 2.2], [34, 66, 1.2]].forEach(b => { c.beginPath(); c.arc(b[0], b[1], b[2], 0, Math.PI * 2); c.fill(); });
    c.restore();
    inset(c, flask, 'rgba(0, 40, 50, 0.75)', 3, -1.6, -2);
    inset(c, flask, 'rgba(255, 255, 255, 0.8)', 1, 0.8, 0.8);
    c.strokeStyle = 'rgba(230, 250, 255, 0.7)'; c.lineWidth = 1.1; c.stroke(flask);
    c.save(); c.clip(flask);
    c.strokeStyle = 'rgba(255,255,255,0.85)'; c.lineWidth = 3.4; c.lineCap = 'round';
    c.beginPath(); c.arc(50, 64, 24, Math.PI * 1.02, Math.PI * 1.32); c.stroke();
    c.lineWidth = 1.6; c.beginPath(); c.moveTo(47, 20); c.lineTo(47, 34); c.stroke();
    c.restore();
    sparkle(c, 33, 50, 3.2);
    const cork = P('M40 7Q50 4 60 7L59 19Q50 21 41 19Z');
    solid(c, cork, lin(c, 0, 5, 0, 20, [[0, '#e3b07a'], [0.5, '#a86a34'], [1, '#5a3010']]), { grain: 0.5 });
  };

  DRAW.scroll = c => {
    const sheet = P('M27 20C40 23 60 17 73 20L72 80C60 77 40 83 28 80Z');
    solid(c, sheet, rad(c, 44, 40, 4, 50, 50, 44, [[0, '#fff3cf'], [0.55, '#e8c886'], [1, '#a87534']]), { grain: 0.55, light: 'rgba(255,255,240,0.6)' });
    c.strokeStyle = 'rgba(110, 60, 20, 0.45)'; c.lineWidth = 1.2; c.lineCap = 'round';
    for (let y = 31; y <= 62; y += 6.5) { c.beginPath(); c.moveTo(34, y); c.bezierCurveTo(44, y - 1.5, 54, y + 1.5, 66 - (y > 55 ? 14 : 0), y); c.stroke(); }
    const roll = y => {
      const r = P('M20 ' + y + 'H80A4 6 0 0 1 80 ' + (y + 12) + 'H20A4 6 0 0 1 20 ' + y + 'Z');
      solid(c, r, lin(c, 0, y, 0, y + 12, [[0, '#a8753a'], [0.35, '#fff1c8'], [0.6, '#d9ae66'], [1, '#6a3e14']]), { grain: 0.4 });
      [20, 80].forEach(x => {
        const cap = P('M' + (x - 4) + ' ' + (y + 6) + 'A4 6.4 0 1 0 ' + (x + 4) + ' ' + (y + 6) + 'A4 6.4 0 1 0 ' + (x - 4) + ' ' + (y + 6) + 'Z');
        solid(c, cap, rad(c, x - 1, y + 4, 0.5, x, y + 6, 6, METAL.darkgold.map(s => [s[0], s[1]])), { drop: 1, blur: 2 });
      });
    };
    roll(11); roll(77);
    const ribbonL = P('M58 66L54 86L59 82L62 88L63 68Z'), ribbonR = P('M66 66L68 87L71 81L76 84L70 66Z');
    solid(c, ribbonL, lin(c, 54, 66, 62, 88, [[0, '#d8203a'], [1, '#6a0010']]), { bevel: false, blur: 2 });
    solid(c, ribbonR, lin(c, 66, 66, 76, 88, [[0, '#d8203a'], [1, '#6a0010']]), { bevel: false, blur: 2 });
    const seal = P('M64 56C70 55 75 59 74.5 65C76 71 70 75 64 74.5C58 75.5 53 71 53.5 65C53 59 58 55.5 64 56Z');
    solid(c, seal, rad(c, 60, 61, 1, 64, 65, 11, [[0, '#ff6a7a'], [0.5, '#c4142e'], [1, '#5a0010']]), { light: 'rgba(255,200,200,0.7)' });
    emblem(c, 64, 65, 0.14, '#a0102a');
  };

  DRAW.key = c => {
    c.save();
    c.translate(50, 50); c.rotate(-0.72); c.translate(-50, -50);
    const k = P('M50 6C58 6 64 12 64 20C64 26 60 31 55 33V64H66V71H55V76H62V83H55V94H45V33C40 31 36 26 36 20C36 12 42 6 50 6ZM50 13C46 13 43 16 43 20C43 24 46 27 50 27C54 27 57 24 57 20C57 16 54 13 50 13Z');
    solid(c, k, metal(c, 'gold', 6, 94), { rule: 'evenodd', grain: 0.35, stroke: 'rgba(90,50,0,0.6)', strokeWidth: 0.8 });
    gem(c, 50, 40, 4.2, GEM.ruby, 6);
    shine(c, k, 44, 16, 16, 0.45);
    c.restore();
    sparkle(c, 30, 30, 4.5);
  };

  DRAW.bow = c => {
    c.save();
    c.translate(50, 50); c.rotate(-0.55); c.translate(-50, -50);
    c.strokeStyle = 'rgba(255, 250, 230, 0.85)'; c.lineWidth = 0.9;
    c.beginPath(); c.moveTo(33, 11); c.lineTo(33, 89); c.stroke();
    const limb = P('M30 8C38 6 44 12 44 20C56 36 58 64 44 80C44 88 38 94 30 92C38 90 40 86 39 80C52 64 50 36 39 20C40 14 38 10 30 8Z');
    solid(c, limb, lin(c, 30, 0, 60, 0, [[0, '#5a2a0a'], [0.35, '#c47a3a'], [0.55, '#8a4a1a'], [1, '#3a1804']]), { grain: 0.6 });
    const grip = P('M44.5 43H53V57H44.5Z');
    solid(c, grip, lin(c, 0, 43, 0, 57, [[0, '#3a1a0a'], [0.5, '#7a3a1a'], [1, '#2a1004']]), { drop: 1, blur: 2 });
    c.strokeStyle = 'rgba(240, 190, 120, 0.5)'; c.lineWidth = 0.7;
    for (let y = 45; y < 57; y += 2.6) { c.beginPath(); c.moveTo(44.5, y); c.lineTo(53, y + 1.6); c.stroke(); }
    [[30, 8], [30, 92]].forEach(p => { const tip = P('M' + (p[0] - 3) + ' ' + (p[1] - 3) + 'h6v6h-6Z'); solid(c, tip, metal(c, 'gold', p[1] - 3, p[1] + 3), { blur: 2, drop: 1 }); });
    const shaft = P('M22 48.8H82V51.2H22Z');
    solid(c, shaft, lin(c, 0, 48, 0, 52, [[0, '#f0d6a8'], [1, '#8a6a3a']]), { bevel: false, blur: 2, drop: 1 });
    const head = P('M80 45L94 50L80 55L82 50Z');
    solid(c, head, metal(c, 'silver', 45, 55), { blur: 3 });
    const fl1 = P('M22 49L14 42L28 42L32 49Z'), fl2 = P('M22 51L14 58L28 58L32 51Z');
    solid(c, fl1, lin(c, 14, 42, 32, 49, [[0, '#ff6a5a'], [1, '#9a0a10']]), { blur: 2, drop: 1 });
    solid(c, fl2, lin(c, 14, 51, 32, 58, [[0, '#d01a20'], [1, '#6a0008']]), { blur: 2, drop: 1 });
    c.restore();
  };

  DRAW.shield = c => {
    const outer = P('M50 6C64 12 78 12 89 10C92 46 81 74 50 95C19 74 8 46 11 10C22 12 36 12 50 6Z');
    solid(c, outer, metal(c, 'silver', 6, 95), { grain: 0.3 });
    const field = P('M50 13C62 18 74 18 83 16.5C84.5 46 75 68 50 86.5C25 68 15.5 46 17 16.5C26 18 38 18 50 13Z');
    solid(c, field, rad(c, 40, 32, 2, 50, 50, 50, [[0, '#4f9bff'], [0.5, '#0f44c0'], [1, '#04164a']]), { drop: 0, blur: 0, grain: 0.35, dark: 'rgba(0,0,30,0.8)', darkBlur: 4 });
    const chevron = P('M17 44L50 26L83 44V54L50 36L17 54Z');
    c.save(); c.clip(field);
    solid(c, chevron, metal(c, 'gold', 26, 54), { blur: 3, grain: 0.3 });
    c.restore();
    solid(c, circle(50, 56, 8.5), rad(c, 46.5, 52.5, 0.5, 50, 56, 9.5, [[0, '#ffffff'], [0.4, '#c7d0da'], [1, '#4a5560']]));
    [[22, 20], [78, 20], [50, 84], [20, 44], [80, 44]].forEach(p => {
      solid(c, circle(p[0], p[1], 2), rad(c, p[0] - 0.6, p[1] - 0.6, 0.2, p[0], p[1], 2.4, [[0, '#ffffff'], [1, '#5a6570']]), { bevel: false, blur: 1, drop: 0.6 });
    });
    c.save(); c.clip(outer); c.globalCompositeOperation = 'lighter';
    c.fillStyle = lin(c, 10, 0, 90, 100, [[0.3, 'rgba(255,255,255,0)'], [0.42, 'rgba(255,255,255,0.28)'], [0.5, 'rgba(255,255,255,0)']]);
    c.fillRect(0, 0, 100, 100); c.restore();
  };

  DRAW.sword = c => {
    const one = rot => {
      c.save();
      c.translate(50, 54); c.rotate(rot); c.translate(-50, -54);
      const blade = P('M46.5 64V14L50 5L53.5 14V64Z');
      solid(c, blade, lin(c, 46.5, 0, 53.5, 0, [[0, '#ffffff'], [0.45, '#d5dde6'], [0.55, '#6d7a88'], [1, '#aab6c2']]), { blur: 4, light: 'rgba(255,255,255,0.9)' });
      c.strokeStyle = 'rgba(70, 85, 100, 0.7)'; c.lineWidth = 0.8;
      c.beginPath(); c.moveTo(50, 12); c.lineTo(50, 62); c.stroke();
      const guard = P('M34 63C40 61 60 61 66 63L66 68C60 66 40 66 34 68Z');
      solid(c, guard, metal(c, 'gold', 61, 68), { grain: 0.3 });
      const grip = P('M47.4 68H52.6V84H47.4Z');
      solid(c, grip, lin(c, 47, 0, 53, 0, [[0, '#2a1004'], [0.5, '#6a3414'], [1, '#1a0802']]), { blur: 2, drop: 1 });
      c.strokeStyle = 'rgba(255, 210, 140, 0.45)'; c.lineWidth = 0.6;
      for (let y = 69.5; y < 84; y += 2.4) { c.beginPath(); c.moveTo(47.4, y); c.lineTo(52.6, y + 1.4); c.stroke(); }
      gem(c, 50, 88, 4.4, GEM.ruby, 8);
      c.restore();
    };
    one(-0.66); one(0.66);
    sparkle(c, 50, 22, 4.5);
  };

  DRAW.ring = c => {
    const band = P('M50 50C68 50 82 58 82 68C82 78 68 86 50 86C32 86 18 78 18 68C18 58 32 50 50 50ZM50 57C36 57 26 62 26 68C26 74 36 79 50 79C64 79 74 74 74 68C74 62 64 57 50 57Z');
    solid(c, band, metal(c, 'gold', 50, 86), { rule: 'evenodd', grain: 0.3 });
    c.save(); c.clip(band, 'evenodd');
    c.fillStyle = 'rgba(60, 25, 0, 0.45)';
    c.fillRect(0, 50, 100, 13);
    c.restore();
    const prongs = P('M38 48L44 34H56L62 48L56 52H44Z');
    solid(c, prongs, metal(c, 'gold', 34, 52), { grain: 0.3 });
    gem(c, 50, 34, 17, GEM.ruby, 8);
    sparkle(c, 70, 70, 4);
  };

  DRAW.crown = c => {
    const top = P('M18 66L12 28L28 47L33 18L44 44L50 9L56 44L67 18L72 47L88 28L82 66Z');
    solid(c, top, metal(c, 'gold', 9, 66), { grain: 0.35 });
    c.save(); c.clip(top);
    c.fillStyle = rad(c, 50, 70, 2, 50, 70, 30, [[0, 'rgba(120, 0, 30, 0.75)'], [1, 'rgba(120, 0, 30, 0)']]);
    c.fillRect(0, 40, 100, 30); c.restore();
    [[12, 27], [33, 17], [50, 8], [67, 17], [88, 27]].forEach(p => {
      solid(c, circle(p[0], p[1], 4), rad(c, p[0] - 1.4, p[1] - 1.6, 0.3, p[0], p[1], 4.6, [[0, '#ffffff'], [0.5, '#f1e9dc'], [1, '#a8957a']]), { bevel: false, blur: 2, drop: 1 });
    });
    const base = P('M15 64H85Q87 64 87 66V80Q87 83 84 83H16Q13 83 13 80V66Q13 64 15 64Z');
    solid(c, base, metal(c, 'gold', 64, 83), { grain: 0.35 });
    gem(c, 50, 73.5, 7.2, GEM.ruby, 8);
    gem(c, 30, 73.5, 5, GEM.sapphire, 6);
    gem(c, 70, 73.5, 5, GEM.sapphire, 6);
    gem(c, 50, 47, 4.4, GEM.emerald, 6);
    shine(c, top, 34, 30, 22, 0.4);
  };

  DRAW.dragon = c => {
    const frame = circle(50, 50, 47); frame.addPath(circle(50, 50, 39));
    const inner = circle(50, 50, 39);
    c.save(); c.clip(inner);
    c.fillStyle = rad(c, 50, 50, 4, 50, 50, 44, [[0, '#7a0a10'], [1, '#22030a']]);
    c.fillRect(0, 0, 100, 100);
    for (let row = 0; row < 12; row++) {
      for (let col = 0; col < 12; col++) {
        const x = col * 9 + (row % 2 ? 4.5 : 0) - 2, y = row * 7.4 + 4;
        const sc = new Path2D();
        sc.moveTo(x - 5, y); sc.quadraticCurveTo(x, y + 9, x + 5, y); sc.quadraticCurveTo(x, y + 3, x - 5, y);
        c.fillStyle = lin(c, 0, y, 0, y + 8, [[0, 'rgba(255, 90, 60, 0.55)'], [1, 'rgba(60, 0, 0, 0.6)']]);
        c.fill(sc);
        c.strokeStyle = 'rgba(20, 0, 0, 0.55)'; c.lineWidth = 0.6; c.stroke(sc);
      }
    }
    c.restore();
    const eye = P('M12 50C28 25 72 25 88 50C72 75 28 75 12 50Z');
    c.save();
    c.shadowColor = 'rgba(255, 140, 0, 0.9)'; c.shadowBlur = K * (14);
    c.fillStyle = rad(c, 50, 50, 1, 50, 50, 34, [[0, '#fff8c8'], [0.22, '#ffd23a'], [0.55, '#ff7a00'], [0.82, '#b42200'], [1, '#3a0800']]);
    c.fill(eye);
    c.restore();
    c.save(); c.clip(eye);
    for (let i = 0; i < 90; i++) {
      const a = (i / 90) * Math.PI * 2 + Math.random() * 0.05, r0 = 6 + Math.random() * 4, r1 = 20 + Math.random() * 14;
      c.strokeStyle = Math.random() < 0.5 ? 'rgba(255, 245, 190, 0.35)' : 'rgba(120, 20, 0, 0.35)';
      c.lineWidth = 0.5 + Math.random() * 0.6;
      c.beginPath(); c.moveTo(50 + Math.cos(a) * r0, 50 + Math.sin(a) * r0); c.lineTo(50 + Math.cos(a) * r1, 50 + Math.sin(a) * r1); c.stroke();
    }
    const pupil = P('M50 28C56 38 56 62 50 72C44 62 44 38 50 28Z');
    c.save(); c.shadowColor = 'rgba(255, 60, 0, 0.9)'; c.shadowBlur = K * (5); c.fillStyle = '#0a0000'; c.fill(pupil); c.restore();
    c.restore();
    inset(c, eye, 'rgba(20, 0, 0, 0.9)', 5, 0, 2.5);
    inset(c, eye, 'rgba(20, 0, 0, 0.7)', 3, 0, -2);
    c.save(); c.clip(eye);
    c.fillStyle = 'rgba(255, 255, 255, 0.85)';
    c.beginPath(); c.ellipse(39, 41, 6, 3.6, -0.5, 0, Math.PI * 2); c.fill();
    c.beginPath(); c.arc(60, 58, 1.6, 0, Math.PI * 2); c.fill();
    c.restore();
    c.strokeStyle = 'rgba(30, 0, 0, 0.9)'; c.lineWidth = 1.6; c.stroke(eye);
    solid(c, frame, metal(c, 'gold', 3, 97), { rule: 'evenodd', grain: 0.35 });
    [0, 1, 2, 3, 4, 5, 6, 7].forEach(k => {
      const a = k * Math.PI / 4 - Math.PI / 2;
      solid(c, circle(50 + Math.cos(a) * 43, 50 + Math.sin(a) * 43, 2), rad(c, 50 + Math.cos(a) * 43 - 0.6, 50 + Math.sin(a) * 43 - 0.6, 0.2, 50 + Math.cos(a) * 43, 50 + Math.sin(a) * 43, 2.4, [[0, '#fffbe0'], [1, '#8a5200']]), { bevel: false, blur: 1, drop: 0.5 });
    });
  };

  DRAW.wild = c => {
    c.save();
    c.globalCompositeOperation = 'lighter';
    const flame = (x, base, h, w) => {
      const f = new Path2D();
      f.moveTo(x - w, base);
      f.bezierCurveTo(x - w, base - h * 0.45, x - w * 0.15, base - h * 0.55, x + w * 0.1, base - h);
      f.bezierCurveTo(x + w * 0.3, base - h * 0.6, x + w, base - h * 0.4, x + w, base);
      f.closePath();
      c.fillStyle = rad(c, x, base, 1, x, base - h * 0.3, h * 0.9, [[0, 'rgba(255, 250, 200, 0.95)'], [0.3, 'rgba(255, 190, 40, 0.85)'], [0.65, 'rgba(255, 70, 0, 0.6)'], [1, 'rgba(160, 0, 0, 0)']]);
      c.fill(f);
    };
    flame(20, 74, 52, 13); flame(80, 74, 50, 13); flame(36, 72, 64, 14); flame(64, 72, 62, 14); flame(50, 70, 72, 16);
    c.restore();
    c.fillStyle = rad(c, 50, 52, 4, 50, 52, 44, [[0, 'rgba(255, 120, 20, 0.35)'], [1, 'rgba(255, 60, 0, 0)']]);
    c.fillRect(0, 0, 100, 100);
    const letters = P('M4 36H11L15 53L18.5 39H23.5L27 53L31 36H38L30.5 66H24L21 54L18 66H11.5ZM41 36H49V66H41ZM52 36H60V59H72V66H52ZM75 36H84C93 36 98 42 98 51C98 60 93 66 84 66H75ZM82 42.5V59.5H84C88.5 59.5 90.5 56 90.5 51C90.5 46 88.5 42.5 84 42.5Z');
    c.save();
    c.translate(0, 4); c.scale(1, 1.08); c.translate(0, -4);
    for (let d = 5; d >= 1; d--) {
      c.save(); c.translate(0, d * 0.9);
      c.fillStyle = d > 3 ? '#3a1800' : '#7a3e00';
      if (d === 5) { c.shadowColor = 'rgba(0,0,0,0.7)'; c.shadowBlur = K * (6); c.shadowOffsetY = K * (2); }
      c.fill(letters, 'evenodd');
      c.restore();
    }
    solid(c, letters, metal(c, 'gold', 36, 66), { rule: 'evenodd', blur: 0, drop: 0, grain: 0.3, stroke: 'rgba(80,40,0,0.8)', strokeWidth: 0.6 });
    shine(c, letters, 30, 38, 26, 0.5);
    c.restore();
    sparkle(c, 12, 38, 3.8);
    sparkle(c, 90, 62, 3.2);
  };

  DRAW.scatter = c => {
    c.save();
    c.globalCompositeOperation = 'lighter';
    c.fillStyle = rad(c, 50, 54, 4, 50, 54, 46, [[0, 'rgba(255, 170, 40, 0.6)'], [1, 'rgba(255, 80, 0, 0)']]);
    c.fillRect(0, 0, 100, 100);
    c.restore();
    [[30, 84, 13], [70, 85, 13], [50, 89, 14]].forEach(p => {
      solid(c, ellipse(p[0], p[1], p[2], p[2] * 0.42), metal(c, 'gold', p[1] - 6, p[1] + 6), { blur: 3, drop: 1.5 });
    });
    const egg = P('M50 8C72 8 84 40 84 60C84 80 69 90 50 90C31 90 16 80 16 60C16 40 28 8 50 8Z');
    solid(c, egg, rad(c, 40, 34, 2, 50, 54, 46, [[0, '#ff7a5a'], [0.4, '#b4122a'], [0.8, '#4a0414'], [1, '#22020a']]), { blur: 6 });
    c.save(); c.clip(egg);
    for (let row = 0; row < 12; row++) {
      for (let col = 0; col < 9; col++) {
        const x = col * 10 + (row % 2 ? 5 : 0) + 6, y = row * 7 + 10;
        const sc = new Path2D();
        sc.moveTo(x - 5.5, y); sc.quadraticCurveTo(x, y + 9, x + 5.5, y); sc.quadraticCurveTo(x, y + 2.5, x - 5.5, y);
        c.fillStyle = 'rgba(255, 150, 110, 0.22)'; c.fill(sc);
        c.strokeStyle = 'rgba(30, 0, 5, 0.55)'; c.lineWidth = 0.7; c.stroke(sc);
      }
    }
    c.restore();
    inset(c, egg, 'rgba(10, 0, 0, 0.85)', 6, -3, -4);
    inset(c, egg, 'rgba(255, 210, 180, 0.8)', 2, 1.4, 1.6);
    const crack = new Path2D('M30 40L38 46L35 54L44 58L42 66M60 30L56 38L63 44L59 52M64 62L70 68L66 76');
    c.save(); c.clip(egg);
    c.lineCap = 'round'; c.lineJoin = 'round';
    c.shadowColor = 'rgba(255, 170, 30, 1)'; c.shadowBlur = K * (8);
    c.strokeStyle = '#ffb02e'; c.lineWidth = 2.4; c.stroke(crack);
    c.shadowBlur = K * (0); c.strokeStyle = '#fff6c8'; c.lineWidth = 0.9; c.stroke(crack);
    c.restore();
    shine(c, egg, 38, 26, 18, 0.55);
    sparkle(c, 36, 24, 3.6);
  };

  DRAW.coin = c => {
    const edge = circle(50, 50, 43);
    c.save(); c.translate(0, 2.4);
    c.fillStyle = '#5a3000'; c.fill(edge);
    c.restore();
    solid(c, edge, metal(c, 'gold', 7, 93), { grain: 0.3, blur: 6 });
    c.save(); c.clip(edge);
    c.strokeStyle = 'rgba(90, 50, 0, 0.55)'; c.lineWidth = 0.7;
    for (let k = 0; k < 72; k++) {
      const a = k * Math.PI / 36;
      c.beginPath(); c.moveTo(50 + Math.cos(a) * 39.5, 50 + Math.sin(a) * 39.5); c.lineTo(50 + Math.cos(a) * 43, 50 + Math.sin(a) * 43); c.stroke();
    }
    c.restore();
    const face = circle(50, 50, 37);
    solid(c, face, rad(c, 40, 36, 2, 50, 50, 40, [[0, '#fff3bf'], [0.45, '#f2b51e'], [0.85, '#b47208'], [1, '#7a4600']]), { drop: 0, blur: 0, grain: 0.35, light: 'rgba(80,40,0,0.7)', dark: 'rgba(255,250,220,0.8)' });
    c.save(); c.clip(face);
    c.fillStyle = 'rgba(110, 60, 0, 0.6)';
    for (let k = 0; k < 28; k++) { const a = k * Math.PI / 14; c.beginPath(); c.arc(50 + Math.cos(a) * 32, 50 + Math.sin(a) * 32, 1.1, 0, Math.PI * 2); c.fill(); }
    c.restore();
    emblem(c, 50, 47, 0.56, '#b87008');
    c.save(); c.clip(edge); c.globalCompositeOperation = 'lighter';
    c.fillStyle = lin(c, 14, 10, 86, 90, [[0.25, 'rgba(255,255,255,0)'], [0.38, 'rgba(255,255,240,0.45)'], [0.48, 'rgba(255,255,255,0)']]);
    c.fillRect(0, 0, 100, 100); c.restore();
    sparkle(c, 30, 26, 4.6);
  };

  function render(name) {
    const cv = document.createElement('canvas');
    cv.width = cv.height = SIZE;
    // CPU rendering: the art is drawn once, and GPU canvases can drop shadowed fills.
    const c = cv.getContext('2d', { willReadFrequently: true });
    c.scale(SIZE / 100, SIZE / 100);
    c.lineJoin = 'round';
    const z = ZOOM[name] || 1;
    if (z !== 1) { c.translate(50, 50); c.scale(z, z); c.translate(-50, -50); }
    DRAW[name](c);
    return cv.toDataURL('image/png');
  }

  const ZOOM = { key: 1.2, sword: 1.14, bow: 1.14 };
  const SETS = { dragon: ['potion', 'scroll', 'key', 'bow', 'shield', 'sword', 'ring', 'crown', 'dragon', 'wild', 'scatter', 'coin'] };

  // Image URL for a machine's symbol, or null when the machine has no painted art.
  function get(machine, sym) {
    const set = SETS[machine];
    if (!set || set.indexOf(sym) < 0 || typeof document === 'undefined') return null;
    const key = machine + ':' + sym;
    if (!(key in cache)) {
      try { cache[key] = render(sym); } catch (e) { cache[key] = null; }
    }
    return cache[key];
  }

  root.SlotArt = { get: get, has: m => !!SETS[m], SIZE: SIZE, _draw: DRAW, _render: render };
}(typeof self !== 'undefined' ? self : this));
