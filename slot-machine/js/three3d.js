/*
 * Reel Riot - real 3D effects with three.js (WebGL):
 *
 *   megaWheel(o)   a three-ring 3D prize wheel. Landing on UPGRADE moves
 *                  the spin to the next ring in, where the prizes are bigger.
 *   coinStorm(s)   tumbling metallic coins over the big win celebrations.
 *
 * three.js loads on first use (the game's own copy, then a CDN). Every
 * effect creates its canvas and WebGL context on demand and releases them
 * when it ends, so phones keep their memory. Callers check available()
 * and fall back to the 2D versions.
 */
(function (root) {
  'use strict';

  const LOCAL = 'js/vendor/three.min.js';
  const CDN = 'https://cdn.jsdelivr.net/npm/three@0.158.0/build/three.min.js';
  let loading = null;
  let glOk = null;

  function webgl() {
    if (glOk !== null) return glOk;
    try {
      const c = document.createElement('canvas');
      glOk = !!(c.getContext('webgl2') || c.getContext('webgl'));
    } catch (e) { glOk = false; }
    return glOk;
  }

  function load() {
    if (root.THREE) return Promise.resolve(root.THREE);
    if (loading) return loading;
    loading = new Promise(function (resolve, reject) {
      function attempt(src, next) {
        const s = document.createElement('script');
        s.src = src;
        s.async = true;
        s.onload = function () { if (root.THREE) resolve(root.THREE); else if (next) next(); else reject(new Error('three.js did not load')); };
        s.onerror = function () { s.remove(); if (next) next(); else reject(new Error('three.js did not load')); };
        document.head.appendChild(s);
      }
      attempt(LOCAL, function () { attempt(CDN, null); });
    });
    loading.catch(function () { loading = null; });
    return loading;
  }

  function available() { return webgl(); }
  function ready() { return !!root.THREE && webgl(); }

  /* ------------------------------------------------------------ stage */

  // A renderer, scene and camera on a fresh full-screen canvas.
  function stage(host, opts) {
    const T = root.THREE;
    const canvas = document.createElement('canvas');
    canvas.className = 'x3d-canvas';
    host.appendChild(canvas);
    const renderer = new T.WebGLRenderer({ canvas: canvas, antialias: true, alpha: true, powerPreference: 'high-performance' });
    renderer.setPixelRatio(Math.min(root.devicePixelRatio || 1, opts.maxDpr || 1.75));
    renderer.outputColorSpace = T.SRGBColorSpace;
    renderer.toneMapping = T.ACESFilmicToneMapping;
    renderer.toneMappingExposure = opts.exposure || 1.15;
    renderer.setClearColor(0x000000, 0);
    const scene = new T.Scene();
    const camera = new T.PerspectiveCamera(opts.fov || 38, 1, 0.1, 100);
    const pmrem = new T.PMREMGenerator(renderer);
    const envRT = pmrem.fromScene(envScene(T), 0.035);
    scene.environment = envRT.texture;
    pmrem.dispose();
    const disposables = [envRT];
    function fit() {
      const w = host.clientWidth || innerWidth, h = host.clientHeight || innerHeight;
      renderer.setSize(w, h, false);
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
    }
    fit();
    addEventListener('resize', fit);
    return {
      T: T, renderer: renderer, scene: scene, camera: camera, canvas: canvas,
      track: function (x) { disposables.push(x); return x; },
      fit: fit,
      dispose: function () {
        removeEventListener('resize', fit);
        scene.traverse(function (o) {
          if (o.geometry) o.geometry.dispose();
          if (o.material) [].concat(o.material).forEach(function (m) { if (m.map) m.map.dispose(); m.dispose(); });
        });
        disposables.forEach(function (d) { if (d && d.dispose) d.dispose(); });
        renderer.dispose();
        renderer.forceContextLoss();
        canvas.remove();
      }
    };
  }

  // A small glowing room for reflections: warm key panels, colored side lights.
  function envScene(T) {
    const s = new T.Scene();
    const room = new T.Mesh(new T.BoxGeometry(10, 10, 10), new T.MeshBasicMaterial({ color: 0x1a0810, side: T.BackSide }));
    s.add(room);
    const panel = function (w, h, color, k, x, y, z) {
      const m = new T.Mesh(new T.PlaneGeometry(w, h), new T.MeshBasicMaterial({ color: new T.Color(color).multiplyScalar(k), side: T.DoubleSide }));
      m.position.set(x, y, z);
      m.lookAt(0, 0, 0);
      s.add(m);
    };
    panel(6, 2, 0xfff1d0, 7, 0, 4.8, 1.5);
    panel(3, 3, 0xffb050, 3, -4.8, 1, 2);
    panel(3, 3, 0xff4fa0, 2.2, 4.8, 0, 1);
    panel(4, 1.2, 0xffffff, 5, 0, -1, 4.8);
    panel(2, 2, 0x6ab0ff, 1.6, 2, -4.8, -1);
    return s;
  }

  function goldMaterial(T, rough) {
    return new T.MeshStandardMaterial({ color: 0xffc23a, metalness: 1, roughness: rough == null ? 0.24 : rough });
  }

  // Planar UVs over a disc of radius R, so rings and the center share one texture.
  function planarUV(geom, R) {
    const pos = geom.attributes.position, uv = geom.attributes.uv;
    for (let i = 0; i < pos.count; i++) uv.setXY(i, pos.getX(i) / (2 * R) + 0.5, pos.getY(i) / (2 * R) + 0.5);
    uv.needsUpdate = true;
    return geom;
  }

  function easeOut(k) { return 1 - Math.pow(1 - k, 3); }
  function easeInOut(k) { return k < 0.5 ? 4 * k * k * k : 1 - Math.pow(-2 * k + 2, 3) / 2; }
  function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

  /* -------------------------------------------------------- mega wheel */

  const RINGS = [
    { r0: 0.71, r1: 1.0, label: 0.86, font: 0.085 },
    { r0: 0.43, r1: 0.69, label: 0.565, font: 0.075 },
    { r0: 0.15, r1: 0.41, label: 0.3, font: 0.075 }
  ];

  // One texture for all three rings: wedges, dividers and labels.
  function wheelTexture(T, rings, font) {
    const S = 1536, C = S / 2, K = C;
    const cv = document.createElement('canvas');
    cv.width = cv.height = S;
    const g = cv.getContext('2d');
    g.fillStyle = '#12020a';
    g.fillRect(0, 0, S, S);
    rings.forEach(function (ring, ri) {
      const geo = RINGS[ri], n = ring.segs.length, step = Math.PI * 2 / n;
      ring.segs.forEach(function (seg, i) {
        // segment i is centered at world angle pi/2 - i*step; canvas angles run clockwise
        const mid = -(Math.PI / 2 - i * step);
        const a0 = mid - step / 2, a1 = mid + step / 2;
        const gr = g.createRadialGradient(C, C, geo.r0 * K, C, C, geo.r1 * K);
        gr.addColorStop(0, seg.dark);
        gr.addColorStop(0.55, seg.color);
        gr.addColorStop(1, seg.light);
        g.beginPath();
        g.arc(C, C, geo.r1 * K, a0, a1);
        g.arc(C, C, geo.r0 * K, a1, a0, true);
        g.closePath();
        g.fillStyle = gr;
        g.fill();
        g.strokeStyle = 'rgba(255, 226, 140, 0.9)';
        g.lineWidth = 5;
        g.beginPath();
        g.moveTo(C + Math.cos(a0) * geo.r0 * K, C + Math.sin(a0) * geo.r0 * K);
        g.lineTo(C + Math.cos(a0) * geo.r1 * K, C + Math.sin(a0) * geo.r1 * K);
        g.stroke();
        // label, reading outward along the radius
        g.save();
        g.translate(C + Math.cos(mid) * geo.label * K, C + Math.sin(mid) * geo.label * K);
        g.rotate(mid + Math.PI / 2);
        const px = Math.round(geo.font * K * (seg.up ? 0.78 : 1));
        g.font = px + 'px ' + font;
        g.textAlign = 'center';
        g.textBaseline = 'middle';
        g.lineJoin = 'round';
        g.lineWidth = px * 0.22;
        g.strokeStyle = 'rgba(30, 6, 0, 0.9)';
        g.fillStyle = seg.up ? '#fff6c8' : '#ffffff';
        const lines = seg.label.split('\n');
        lines.forEach(function (t, li) {
          const y = (li - (lines.length - 1) / 2) * px * 0.95;
          g.strokeText(t, 0, y);
          g.fillText(t, 0, y);
        });
        g.restore();
      });
    });
    const tex = new T.CanvasTexture(cv);
    tex.colorSpace = T.SRGBColorSpace;
    tex.anisotropy = 4;
    return tex;
  }

  function buildWheel(st, rings, font) {
    const T = st.T;
    const wheel = new T.Group();
    const tex = wheelTexture(T, rings, font);
    const gold = goldMaterial(T);
    const darkGold = goldMaterial(T, 0.4);
    darkGold.color.set(0xa86a10);
    const parts = rings.map(function (ring, ri) {
      const geo = RINGS[ri];
      const mesh = new T.Mesh(
        planarUV(new T.RingGeometry(geo.r0, geo.r1, 128, 1), 1),
        new T.MeshStandardMaterial({ map: tex, roughness: 0.35, metalness: 0.1, emissive: 0xffffff, emissiveMap: tex, emissiveIntensity: 0.35 })
      );
      const spin = new T.Group();
      spin.add(mesh);
      // pegs on the outer edge of each ring
      const n = ring.segs.length, step = Math.PI * 2 / n;
      const pegGeo = new T.CylinderGeometry(0.016, 0.016, 0.07, 10);
      for (let i = 0; i < n; i++) {
        const a = Math.PI / 2 - i * step + step / 2;
        const peg = new T.Mesh(pegGeo, gold);
        peg.rotation.x = Math.PI / 2;
        peg.position.set(Math.cos(a) * (geo.r1 - 0.035), Math.sin(a) * (geo.r1 - 0.035), 0.035);
        spin.add(peg);
      }
      wheel.add(spin);
      return { spin: spin, mesh: mesh, angle: 0, n: n, step: step };
    });
    // rims between and around the rings
    [[1.035, 0.05], [0.7, 0.022], [0.42, 0.022]].forEach(function (r) {
      const t = new T.Mesh(new T.TorusGeometry(r[0], r[1], 18, 160), gold);
      t.position.z = 0.012;
      wheel.add(t);
    });
    // bulbs around the outside
    const bulbs = [];
    const bulbGeo = new T.SphereGeometry(0.026, 14, 10);
    for (let i = 0; i < 36; i++) {
      const a = i / 36 * Math.PI * 2;
      const m = new T.MeshStandardMaterial({ color: 0xfff1c0, emissive: i % 2 ? 0xffd25a : 0xff3d6a, emissiveIntensity: 1.2, roughness: 0.3 });
      const b = new T.Mesh(bulbGeo, m);
      b.position.set(Math.cos(a) * 1.035, Math.sin(a) * 1.035, 0.055);
      wheel.add(b);
      bulbs.push(m);
    }
    // back plate and hub with a ruby
    const back = new T.Mesh(new T.CylinderGeometry(1.12, 1.16, 0.08, 96), darkGold);
    back.rotation.x = Math.PI / 2;
    back.position.z = -0.05;
    wheel.add(back);
    const hub = new T.Mesh(new T.CylinderGeometry(0.14, 0.16, 0.09, 48), gold);
    hub.rotation.x = Math.PI / 2;
    hub.position.z = 0.04;
    wheel.add(hub);
    const ruby = new T.Mesh(new T.OctahedronGeometry(0.09, 0), new T.MeshStandardMaterial({ color: 0xff1a4a, emissive: 0x8a0020, emissiveIntensity: 0.8, metalness: 0.2, roughness: 0.05, flatShading: true }));
    ruby.position.z = 0.12;
    ruby.scale.set(1, 1, 0.6);
    wheel.add(ruby);
    // pointers: a big gold one for the outer ring, ruby markers for the inner rings
    const pointers = RINGS.map(function (geo, ri) {
      const sh = new T.Shape();
      const w = ri ? 0.045 : 0.075, h = ri ? 0.08 : 0.15;
      sh.moveTo(0, -h * 0.55);
      sh.lineTo(w, h * 0.45);
      sh.lineTo(-w, h * 0.45);
      sh.closePath();
      const p = new T.Mesh(
        new T.ExtrudeGeometry(sh, { depth: 0.03, bevelEnabled: true, bevelThickness: 0.012, bevelSize: 0.01, bevelSegments: 2 }),
        ri ? new T.MeshStandardMaterial({ color: 0xff2a55, emissive: 0x6a0010, emissiveIntensity: 0.6, metalness: 0.3, roughness: 0.15 }) : gold
      );
      const holder = new T.Group();
      holder.position.set(0, geo.r1 + (ri ? 0.005 : 0.06), 0.09);
      holder.add(p);
      wheel.add(holder);
      return { holder: holder, kick: 0, vel: 0 };
    });
    return { group: wheel, parts: parts, bulbs: bulbs, pointers: pointers, ruby: ruby };
  }

  /*
   * o.rings: [{ segs: [{ label, color, light, dark, up }] }] x 3, outer first.
   * o.path:  [{ ring, idx }] in order; every step but the last lands on an UPGRADE.
   * o.title, o.result: text for the overlay; o.onTick(ring), o.onLand(step), o.turbo.
   * Resolves when the player closes the result.
   */
  function megaWheel(o) {
    return load().then(function () {
      const T = root.THREE;
      const host = document.createElement('div');
      host.className = 'x3d x3d-wheel';
      host.innerHTML =
        '<div class="x3d-head"><h2 class="x3d-title"></h2><p class="x3d-sub"></p></div>' +
        '<div class="x3d-banner" hidden></div>' +
        '<p class="x3d-hint"></p>';
      document.body.appendChild(host);
      const titleEl = host.querySelector('.x3d-title');
      const subEl = host.querySelector('.x3d-sub');
      const hintEl = host.querySelector('.x3d-hint');
      const banner = host.querySelector('.x3d-banner');
      titleEl.textContent = o.title || 'Mega Wheel';
      subEl.textContent = o.sub || 'Land on UPGRADE to move in to bigger prizes';
      hintEl.textContent = 'Tap anywhere to spin';
      const st = stage(host, { fov: 36 });
      const scene = st.scene, camera = st.camera;
      scene.add(new T.HemisphereLight(0xffe8c8, 0x200810, 0.6));
      const key = new T.DirectionalLight(0xffffff, 1.6);
      key.position.set(-1.5, 2.5, 3);
      scene.add(key);
      const rim = new T.PointLight(0xff5a2a, 6, 6);
      rim.position.set(1.6, -1.2, 1.2);
      scene.add(rim);

      const W = buildWheel(st, o.rings, o.font || "'Bungee', Impact, sans-serif");
      const tilt = new T.Group();
      tilt.add(W.group);
      tilt.rotation.x = -0.32;
      scene.add(tilt);
      W.parts.forEach(function (p) { p.angle = Math.random() * Math.PI * 2; p.spin.rotation.z = p.angle; });

      const sparkGeo = new T.BufferGeometry();
      const SPARKS = 160;
      const sp = new Float32Array(SPARKS * 3), sv = new Float32Array(SPARKS * 3), sl = new Float32Array(SPARKS);
      sparkGeo.setAttribute('position', new T.BufferAttribute(sp, 3));
      const sparkTex = glowTexture(T);
      const sparks = new T.Points(sparkGeo, new T.PointsMaterial({ size: 0.07, map: sparkTex, color: 0xffd27a, transparent: true, depthWrite: false, blending: T.AdditiveBlending }));
      scene.add(sparks);
      st.track(sparkTex);
      function burst(n, x, y, z, speed) {
        for (let i = 0, made = 0; i < SPARKS && made < n; i++) {
          if (sl[i] > 0) continue;
          sp[i * 3] = x; sp[i * 3 + 1] = y; sp[i * 3 + 2] = z;
          const a = Math.random() * Math.PI * 2, e = (Math.random() - 0.3) * 1.4, v = speed * (0.4 + Math.random());
          sv[i * 3] = Math.cos(a) * v; sv[i * 3 + 1] = Math.sin(a) * v; sv[i * 3 + 2] = e * v * 0.5;
          sl[i] = 0.7 + Math.random() * 0.8;
          made++;
        }
      }

      const coins = coinField(T, 70);
      scene.add(coins.mesh);

      // Camera distance that fits a disc of radius r on screen, whatever the screen shape.
      const VIEW = [1.24, 0.82, 0.54];
      function fitZ(r) {
        const t = Math.tan(camera.fov * Math.PI / 360);
        return r / Math.min(t, t * camera.aspect) + 0.25;
      }
      let camZ = fitZ(VIEW[0]), camY = 0.05, camYTarget = 0.05, focus = 0;
      let last = performance.now(), running = true, raf = 0, t0 = last;
      function frame(now) {
        if (!running) return;
        const dt = Math.min(0.05, (now - last) / 1000);
        last = now;
        const time = (now - t0) / 1000;
        camZ += (fitZ(VIEW[focus]) - camZ) * Math.min(1, dt * 3);
        camY += (camYTarget - camY) * Math.min(1, dt * 3);
        camera.position.set(0, camY, camZ);
        camera.lookAt(0, camY * 0.6, 0);
        W.group.position.y = Math.sin(time * 1.3) * 0.01;
        W.bulbs.forEach(function (m, i) { m.emissiveIntensity = 0.5 + 1.1 * (0.5 + 0.5 * Math.sin(time * 9 + i * 1.7 + (i % 2) * Math.PI)); });
        W.ruby.rotation.z = time * 0.8;
        W.parts.forEach(function (p, ri) {
          p.spin.rotation.z = p.angle;
          // dim the rings that are not in play
          const want = ri === focus ? 0.35 : ri < focus ? 0.05 : 0.18;
          p.mesh.material.emissiveIntensity += (want - p.mesh.material.emissiveIntensity) * Math.min(1, dt * 4);
          const c = ri < focus ? 0.45 : 1;
          p.mesh.material.color.setScalar(p.mesh.material.color.r + (c - p.mesh.material.color.r) * Math.min(1, dt * 4));
        });
        W.pointers.forEach(function (pt) {
          pt.vel += (-pt.kick * 60 - pt.vel * 9) * dt;
          pt.kick += pt.vel * dt;
          pt.holder.rotation.z = pt.kick;
        });
        for (let i = 0; i < SPARKS; i++) {
          if (sl[i] <= 0) { sp[i * 3 + 2] = -50; continue; }
          sl[i] -= dt;
          sv[i * 3 + 1] -= 1.6 * dt;
          sp[i * 3] += sv[i * 3] * dt; sp[i * 3 + 1] += sv[i * 3 + 1] * dt; sp[i * 3 + 2] += sv[i * 3 + 2] * dt;
        }
        sparkGeo.attributes.position.needsUpdate = true;
        coins.step(dt);
        st.renderer.render(scene, camera);
        raf = requestAnimationFrame(frame);
      }
      raf = requestAnimationFrame(frame);

      function tap() {
        return new Promise(function (resolve) {
          const on = function () { host.removeEventListener('pointerdown', on); resolve(); };
          host.addEventListener('pointerdown', on);
        });
      }
      function spinTo(ring, idx, turns, ms) {
        const p = W.parts[ring];
        const off = (Math.random() - 0.5) * p.step * 0.6;
        const base = idx * p.step + off;
        const from = p.angle;
        const k = Math.ceil((base - from + turns * Math.PI * 2) / (Math.PI * 2));
        const to = base - k * Math.PI * 2;
        const pt = W.pointers[ring];
        let lastPeg = Math.floor(from / p.step);
        return new Promise(function (resolve) {
          const start = performance.now();
          function tick(now) {
            const kk = Math.min(1, (now - start) / ms);
            // fast start, long suspenseful slowdown, a tiny settle at the end
            const e = kk < 1 ? 1 - Math.pow(1 - kk, 4) : 1;
            p.angle = from + (to - from) * e;
            const peg = Math.floor(p.angle / p.step);
            if (peg !== lastPeg) {
              lastPeg = peg;
              pt.vel += 6;
              if (o.onTick) o.onTick(ring);
            }
            if (kk < 1) requestAnimationFrame(tick); else resolve();
          }
          requestAnimationFrame(tick);
        });
      }
      function showBanner(text, cls) {
        banner.textContent = text;
        banner.className = 'x3d-banner ' + (cls || '');
        banner.hidden = false;
        void banner.offsetWidth;
        banner.classList.add('pop');
      }

      const done = (async function () {
        await sleep(250);
        await tap();
        hintEl.textContent = '';
        const turbo = !!o.turbo;
        for (let s = 0; s < o.path.length; s++) {
          const step = o.path[s];
          focus = step.ring;
          camYTarget = [0.05, 0.2, 0.28][step.ring];
          if (s > 0) await sleep(turbo ? 350 : 700);
          banner.hidden = true;
          await spinTo(step.ring, step.idx, turbo ? 3 : 5 - step.ring, turbo ? 2600 : 4600 - step.ring * 500);
          const geo = RINGS[step.ring];
          const y = geo.r1 * Math.cos(0.32), z = geo.r1 * Math.sin(0.32) * -1 + 0.15;
          burst(50, 0, y, z, 1.2);
          if (o.onLand) o.onLand(s);
          if (s < o.path.length - 1) {
            showBanner('UPGRADE!', 'up');
            await sleep(turbo ? 500 : 1100);
          }
        }
        coins.burst(70);
        burst(120, 0, 0.1, 0.3, 1.8);
        showBanner(o.result || 'You win!', 'win');
        hintEl.textContent = 'Tap to collect';
        await sleep(600);
        await tap();
      })();

      return done.then(function () {
        running = false;
        cancelAnimationFrame(raf);
        host.classList.add('out');
        return sleep(260);
      }).then(function () {
        st.dispose();
        host.remove();
      });
    });
  }

  function glowTexture(T) {
    const c = document.createElement('canvas');
    c.width = c.height = 64;
    const g = c.getContext('2d');
    const gr = g.createRadialGradient(32, 32, 0, 32, 32, 32);
    gr.addColorStop(0, 'rgba(255,255,255,1)');
    gr.addColorStop(0.3, 'rgba(255,230,160,0.8)');
    gr.addColorStop(1, 'rgba(255,160,40,0)');
    g.fillStyle = gr;
    g.fillRect(0, 0, 64, 64);
    const t = new T.CanvasTexture(c);
    t.colorSpace = T.SRGBColorSpace;
    return t;
  }

  /* ------------------------------------------------------------ coins */

  // The stamped face of a gold coin: rim, inner ring and an embossed star.
  function coinFace(T) {
    const c = document.createElement('canvas');
    c.width = c.height = 128;
    const g = c.getContext('2d');
    const gr = g.createRadialGradient(46, 40, 6, 64, 64, 64);
    gr.addColorStop(0, '#fff6c8'); gr.addColorStop(0.35, '#ffd24a'); gr.addColorStop(0.75, '#e9a000'); gr.addColorStop(1, '#a86200');
    g.fillStyle = gr;
    g.fillRect(0, 0, 128, 128);
    g.lineWidth = 7; g.strokeStyle = '#b87400';
    g.beginPath(); g.arc(64, 64, 52, 0, Math.PI * 2); g.stroke();
    const star = function (ox, oy, col) {
      g.fillStyle = col;
      g.beginPath();
      for (let i = 0; i < 10; i++) {
        const a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 ? 15 : 34;
        g[i ? 'lineTo' : 'moveTo'](64 + ox + Math.cos(a) * r, 64 + oy + Math.sin(a) * r);
      }
      g.closePath();
      g.fill();
    };
    star(2, 3, 'rgba(110, 60, 0, 0.7)'); star(-1.5, -1.5, 'rgba(255, 250, 220, 0.85)'); star(0, 0, '#e9a000');
    const t = new T.CanvasTexture(c);
    t.colorSpace = T.SRGBColorSpace;
    return t;
  }

  // Instanced gold coins with simple physics: burst() throws them, step() moves them.
  function coinField(T, count) {
    const geo = new T.CylinderGeometry(0.075, 0.075, 0.014, 28);
    const face = coinFace(T);
    const side = new T.MeshStandardMaterial({ color: 0xd99a1a, metalness: 0.9, roughness: 0.3, emissive: 0x6a3a00, emissiveIntensity: 0.5 });
    const cap = new T.MeshStandardMaterial({ map: face, metalness: 0.8, roughness: 0.28, emissive: 0xffffff, emissiveMap: face, emissiveIntensity: 0.35 });
    const mesh = new T.InstancedMesh(geo, [side, cap, cap], count);
    mesh.frustumCulled = false;
    const dummy = new T.Object3D();
    const c = [];
    for (let i = 0; i < count; i++) {
      c.push({ live: false, p: new T.Vector3(), v: new T.Vector3(), r: new T.Euler(), w: new T.Vector3(), s: 1 });
      dummy.position.set(0, -50, 0);
      dummy.updateMatrix();
      mesh.setMatrixAt(i, dummy.matrix);
    }
    function burst(n, opts) {
      opts = opts || {};
      for (let i = 0, made = 0; i < count && made < n; i++) {
        const k = c[i];
        if (k.live) continue;
        k.live = true;
        if (opts.rain) {
          k.p.set((Math.random() - 0.5) * opts.width, opts.top + Math.random() * opts.spread, (Math.random() - 0.5) * 1.2);
          k.v.set((Math.random() - 0.5) * 0.4, -Math.random() * 0.6, 0);
        } else {
          k.p.set((Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, 0.3);
          const a = Math.random() * Math.PI * 2, sp = 1.2 + Math.random() * 2.2;
          k.v.set(Math.cos(a) * sp * 0.8, 1.6 + Math.random() * 2.2, 0.6 + Math.random() * 1.6);
        }
        k.r.set(Math.random() * 6, Math.random() * 6, Math.random() * 6);
        k.w.set((Math.random() - 0.5) * 14, (Math.random() - 0.5) * 6, (Math.random() - 0.5) * 10);
        k.s = (opts.scale || 1) * (0.8 + Math.random() * 0.5);
        made++;
      }
    }
    function step(dt) {
      let alive = 0;
      for (let i = 0; i < count; i++) {
        const k = c[i];
        if (!k.live) continue;
        k.v.y -= 4.2 * dt;
        k.p.addScaledVector(k.v, dt);
        k.r.x += k.w.x * dt; k.r.y += k.w.y * dt; k.r.z += k.w.z * dt;
        if (k.p.y < -4) { k.live = false; dummy.position.set(0, -50, 0); dummy.scale.setScalar(1); }
        else { dummy.position.copy(k.p); dummy.rotation.copy(k.r); dummy.scale.setScalar(k.s); alive++; }
        dummy.updateMatrix();
        mesh.setMatrixAt(i, dummy.matrix);
      }
      mesh.instanceMatrix.needsUpdate = true;
      return alive;
    }
    return { mesh: mesh, burst: burst, step: step };
  }

  /*
   * A storm of 3D coins over whatever is on screen, for `seconds`.
   * Returns a handle; stop() lets the coins in the air finish falling.
   */
  function coinStorm(seconds, strength) {
    const handle = { stop: function () { handle.stopped = true; }, stopped: false };
    if (!webgl()) return handle;
    load().then(function () {
      if (handle.stopped) return;
      const T = root.THREE;
      const host = document.createElement('div');
      host.className = 'x3d x3d-storm';
      document.body.appendChild(host);
      const st = stage(host, { fov: 40, maxDpr: 1.5 });
      st.scene.add(new T.HemisphereLight(0xfff0d0, 0x301008, 0.7));
      const key = new T.DirectionalLight(0xffffff, 1.8);
      key.position.set(-1, 2, 3);
      st.scene.add(key);
      st.camera.position.set(0, 0, 5);
      const field = coinField(T, Math.round(60 + 60 * Math.min(1, strength || 0.5)));
      st.scene.add(field.mesh);
      const h = 2 * Math.tan(20 * Math.PI / 180) * 5, w = h * st.camera.aspect;
      field.burst(Math.round(25 + 30 * (strength || 0.5)), { scale: 1.05 });
      let acc = 0, last = performance.now();
      const end = last + seconds * 1000;
      function frame(now) {
        const dt = Math.min(0.05, (now - last) / 1000);
        last = now;
        if (!handle.stopped && now < end) {
          acc += dt * (14 + 26 * (strength || 0.5));
          const n = Math.floor(acc);
          if (n) { acc -= n; field.burst(n, { rain: true, width: w * 1.1, top: h / 2 + 0.3, spread: 0.6, scale: 1.1 }); }
        }
        const alive = field.step(dt);
        st.renderer.render(st.scene, st.camera);
        if (alive > 0 || (!handle.stopped && now < end)) requestAnimationFrame(frame);
        else { st.dispose(); host.remove(); }
      }
      requestAnimationFrame(frame);
    }).catch(function () { /* no 3D: the 2D coins already play */ });
    return handle;
  }

  // Warm the library up in the background so the first 3D effect starts at once.
  function preload() {
    if (!webgl()) return;
    const go = function () { load().catch(function () {}); };
    if (root.requestIdleCallback) root.requestIdleCallback(go, { timeout: 4000 }); else setTimeout(go, 2500);
  }

  root.SlotThree = { available: available, ready: ready, load: load, preload: preload, megaWheel: megaWheel, coinStorm: coinStorm };
}(typeof self !== 'undefined' ? self : this));
