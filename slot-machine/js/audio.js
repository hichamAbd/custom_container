/*
 * Reel Riot - synthesized sound effects and background music.
 * Everything is generated with the Web Audio API, so there are no files to load.
 */
(function (root) {
  'use strict';

  const AC = root.AudioContext || root.webkitAudioContext;
  let ctx = null;
  let sfxBus = null;
  let musicBus = null;
  let noiseBuf = null;
  let sfxOn = true;
  let musicOn = true;
  let theme = null;
  let seqTimer = 0;
  let nextTime = 0;
  let step = 0;
  let antic = null;

  function init() {
    if (ctx) return true;
    if (!AC) return false;
    try { ctx = new AC(); } catch (e) { ctx = null; return false; }
    const comp = ctx.createDynamicsCompressor();
    comp.threshold.value = -16;
    comp.knee.value = 20;
    comp.ratio.value = 5;
    comp.attack.value = 0.004;
    comp.release.value = 0.25;
    const master = ctx.createGain();
    master.gain.value = 0.9;
    sfxBus = ctx.createGain();
    sfxBus.gain.value = 0.8;
    musicBus = ctx.createGain();
    musicBus.gain.value = 0;
    sfxBus.connect(comp);
    musicBus.connect(comp);
    comp.connect(master);
    master.connect(ctx.destination);
    const len = ctx.sampleRate * 2;
    noiseBuf = ctx.createBuffer(1, len, ctx.sampleRate);
    const d = noiseBuf.getChannelData(0);
    for (let i = 0; i < len; i++) d[i] = Math.random() * 2 - 1;
    return true;
  }

  function unlock() {
    if (!init()) return;
    if (ctx.state === 'suspended') ctx.resume();
    if (musicOn && theme && !seqTimer) startSeq();
  }

  const midi = function (n) { return 440 * Math.pow(2, (n - 69) / 12); };

  // Plays one enveloped oscillator note at absolute time `at`.
  function note(o, at, bus) {
    const t = at;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = o.type || 'sine';
    osc.frequency.setValueAtTime(o.f, t);
    if (o.to) osc.frequency.exponentialRampToValueAtTime(o.to, t + (o.slide || o.d || 0.2));
    if (o.detune) osc.detune.setValueAtTime(o.detune, t);
    const v = o.v == null ? 0.2 : o.v;
    const a = o.a || 0.005;
    const d = o.d || 0.2;
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(v, t + a);
    g.gain.exponentialRampToValueAtTime(0.0001, t + a + d);
    osc.connect(g);
    let out = g;
    if (o.filter) {
      const f = ctx.createBiquadFilter();
      f.type = o.filter.type || 'lowpass';
      f.frequency.setValueAtTime(o.filter.f, t);
      if (o.filter.to) f.frequency.exponentialRampToValueAtTime(o.filter.to, t + a + d);
      f.Q.value = o.filter.q || 1;
      g.connect(f);
      out = f;
    }
    out.connect(bus || sfxBus);
    osc.start(t);
    osc.stop(t + a + d + 0.05);
  }

  function noise(o, at, bus) {
    const t = at;
    const src = ctx.createBufferSource();
    src.buffer = noiseBuf;
    const f = ctx.createBiquadFilter();
    f.type = o.type || 'bandpass';
    f.frequency.setValueAtTime(o.f || 1000, t);
    if (o.to) f.frequency.exponentialRampToValueAtTime(o.to, t + (o.d || 0.2));
    f.Q.value = o.q || 1;
    const g = ctx.createGain();
    const v = o.v == null ? 0.2 : o.v;
    const a = o.a || 0.005;
    const d = o.d || 0.2;
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(v, t + a);
    g.gain.exponentialRampToValueAtTime(0.0001, t + a + d);
    src.connect(f);
    f.connect(g);
    g.connect(bus || sfxBus);
    src.start(t, Math.random() * 1.5);
    src.stop(t + a + d + 0.05);
  }

  function ready() { return sfxOn && ctx && ctx.state === 'running'; }
  function T(dt) { return ctx.currentTime + (dt || 0); }
  function tone(o, dt) { note(o, T(dt)); }
  function hiss(o, dt) { noise(o, T(dt)); }

  const PENTA = [0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24];

  const sfx = {
    click: function () {
      tone({ type: 'square', f: 1250, d: 0.035, v: 0.05, filter: { f: 3200 } });
    },
    bet: function (up) {
      tone({ type: 'triangle', f: up ? 660 : 520, to: up ? 990 : 390, d: 0.09, v: 0.12 });
    },
    error: function () {
      tone({ type: 'square', f: 240, to: 150, d: 0.22, v: 0.07, filter: { f: 1400 } });
    },
    spinStart: function () {
      hiss({ type: 'bandpass', f: 500, to: 2600, d: 0.32, v: 0.16, q: 1.4 });
      tone({ type: 'sawtooth', f: 160, to: 420, d: 0.22, v: 0.04, filter: { f: 1800 } });
    },
    reelStop: function (i) {
      tone({ type: 'sine', f: 170 - i * 6, to: 62, d: 0.13, v: 0.34 });
      hiss({ type: 'highpass', f: 2600, d: 0.035, v: 0.09 });
    },
    scatter: function (n) {
      const base = midi(76 + PENTA[Math.min(n, 8)]);
      tone({ type: 'sine', f: base, d: 0.7, v: 0.22 });
      tone({ type: 'sine', f: base * 2.005, d: 0.4, v: 0.07 });
      tone({ type: 'triangle', f: base / 2, d: 0.5, v: 0.08 });
    },
    coin: function (n) {
      const f = 1600 * (1 + n * 0.14);
      tone({ type: 'square', f: f * 1.5, d: 0.05, v: 0.04, filter: { type: 'bandpass', f: f * 1.5, q: 6 } });
      tone({ type: 'sine', f: f, d: 0.4, v: 0.14 });
      tone({ type: 'sine', f: f * 1.5, d: 0.3, v: 0.07, a: 0.01 });
    },
    anticipation: function (ms) {
      if (antic) return;
      const t = T();
      const osc = ctx.createOscillator();
      const lfo = ctx.createOscillator();
      const lg = ctx.createGain();
      const g = ctx.createGain();
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(180, t);
      osc.frequency.exponentialRampToValueAtTime(760, t + ms / 1000);
      lfo.frequency.setValueAtTime(9, t);
      lfo.frequency.linearRampToValueAtTime(22, t + ms / 1000);
      lg.gain.value = 0.035;
      g.gain.setValueAtTime(0.0001, t);
      g.gain.exponentialRampToValueAtTime(0.05, t + 0.15);
      const f = ctx.createBiquadFilter();
      f.type = 'lowpass';
      f.frequency.value = 2200;
      lfo.connect(lg);
      lg.connect(g.gain);
      osc.connect(f);
      f.connect(g);
      g.connect(sfxBus);
      osc.start(t);
      lfo.start(t);
      antic = { osc: osc, lfo: lfo, g: g };
    },
    anticipationEnd: function () {
      if (!antic) return;
      const t = T();
      try {
        antic.g.gain.cancelScheduledValues(t);
        antic.g.gain.setValueAtTime(0.05, t);
        antic.g.gain.exponentialRampToValueAtTime(0.0001, t + 0.12);
        antic.osc.stop(t + 0.15);
        antic.lfo.stop(t + 0.15);
      } catch (e) { /* already stopped */ }
      antic = null;
    },
    win: function (level) {
      const n = level >= 2 ? 6 : level >= 1 ? 4 : 3;
      for (let i = 0; i < n; i++) {
        const f = midi(72 + PENTA[i]);
        tone({ type: 'triangle', f: f, d: 0.25, v: 0.13 }, i * 0.07);
        tone({ type: 'sine', f: f * 2, d: 0.18, v: 0.05 }, i * 0.07);
      }
    },
    line: function (i) {
      const f = midi(74 + PENTA[i % 8]);
      tone({ type: 'triangle', f: f, d: 0.2, v: 0.1 });
      tone({ type: 'sine', f: f * 1.5, d: 0.15, v: 0.04 }, 0.02);
    },
    tick: function (p) {
      tone({ type: 'sine', f: 900 + p * 1400, d: 0.03, v: 0.045 });
    },
    clink: function () {
      const f = 2600 + Math.random() * 1600;
      tone({ type: 'sine', f: f, d: 0.09, v: 0.05 });
      tone({ type: 'square', f: f * 1.34, d: 0.03, v: 0.012, filter: { f: 6000 } });
    },
    fanfare: function (level) {
      const chords = [[60, 64, 67], [65, 69, 72], [67, 71, 74], [72, 76, 79]];
      const steps = Math.min(4, 2 + level);
      for (let i = 0; i < steps; i++) {
        const ch = chords[i % chords.length];
        ch.forEach(function (m) {
          tone({ type: 'sawtooth', f: midi(m + 12), d: 0.32, v: 0.06, filter: { f: 900, to: 4200 } }, i * 0.16);
          tone({ type: 'square', f: midi(m), d: 0.3, v: 0.03, filter: { f: 2400 } }, i * 0.16);
        });
        tone({ type: 'sine', f: 140, to: 42, d: 0.28, v: 0.4 }, i * 0.16);
      }
      hiss({ type: 'highpass', f: 5000, d: 1.4, v: 0.08, a: 0.01 }, steps * 0.16);
    },
    tierUp: function (level) {
      hiss({ type: 'bandpass', f: 400, to: 5000, d: 0.35, v: 0.14 });
      [0, 4, 7, 12].forEach(function (s, i) {
        tone({ type: 'square', f: midi(67 + level * 2 + s), d: 0.2, v: 0.05, filter: { f: 3500 } }, 0.05 + i * 0.05);
      });
      tone({ type: 'sine', f: 120, to: 40, d: 0.3, v: 0.4 });
    },
    freeSpins: function () {
      for (let i = 0; i < 12; i++) {
        tone({ type: 'sine', f: midi(67 + PENTA[i % 10] + (i >= 10 ? 12 : 0)), d: 0.3, v: 0.1 }, i * 0.06);
      }
      hiss({ type: 'highpass', f: 6000, d: 1.2, v: 0.07, a: 0.3 });
      tone({ type: 'triangle', f: midi(55), d: 1.2, v: 0.12, a: 0.05 });
    },
    retrigger: function () {
      for (let i = 0; i < 6; i++) tone({ type: 'square', f: midi(76 + PENTA[i]), d: 0.12, v: 0.05, filter: { f: 4000 } }, i * 0.05);
    },
    alarm: function () {
      for (let i = 0; i < 8; i++) {
        tone({ type: 'square', f: i % 2 ? 1180 : 880, d: 0.13, v: 0.06, filter: { f: 3000 } }, i * 0.15);
      }
      tone({ type: 'sine', f: 110, to: 40, d: 0.5, v: 0.5 });
      hiss({ type: 'highpass', f: 4000, d: 1.6, v: 0.1, a: 0.02 });
    },
    wheelTick: function () {
      tone({ type: 'square', f: 2300, d: 0.014, v: 0.05, filter: { f: 5000 } });
    },
    wheelStop: function () {
      [72, 76, 79, 84].forEach(function (m, i) { tone({ type: 'sine', f: midi(m), d: 1.0, v: 0.12 }, i * 0.04); });
    },
    jackpot: function (level) {
      sfx.fanfare(3);
      for (let i = 0; i < 6 + level * 3; i++) {
        tone({ type: 'sine', f: midi(84 + PENTA[i % 8]), d: 0.4, v: 0.07 }, 0.6 + i * 0.09);
      }
    },
    explode: function () {
      hiss({ type: 'lowpass', f: 1400, to: 200, d: 0.28, v: 0.24 });
      tone({ type: 'sine', f: 220, to: 55, d: 0.22, v: 0.24 });
    },
    drop: function () {
      tone({ type: 'triangle', f: 520, to: 260, d: 0.1, v: 0.07 });
    },
    multiplier: function (level) {
      const f = midi(64 + Math.min(level, 10) * 2);
      tone({ type: 'square', f: f, d: 0.16, v: 0.06, filter: { f: 3200 } });
      tone({ type: 'sine', f: f * 2, d: 0.22, v: 0.08 }, 0.04);
    },
    fire: function () {
      hiss({ type: 'lowpass', f: 260, to: 2400, d: 0.55, v: 0.4, q: 0.8 });
      hiss({ type: 'lowpass', f: 2400, to: 300, d: 0.7, v: 0.3, q: 0.8 }, 0.5);
      tone({ type: 'sawtooth', f: 96, to: 48, d: 1.0, v: 0.12, filter: { f: 600 } });
    },
    ignite: function () {
      hiss({ type: 'bandpass', f: 900, to: 3000, d: 0.16, v: 0.16 });
      tone({ type: 'triangle', f: midi(70 + Math.floor(Math.random() * 6) * 2), d: 0.14, v: 0.07 });
    },
    lock: function () {
      hiss({ type: 'bandpass', f: 2800, d: 0.07, v: 0.18, q: 3 });
      tone({ type: 'square', f: 330, d: 0.09, v: 0.05, filter: { f: 1800 } });
      tone({ type: 'sine', f: 1320, d: 0.25, v: 0.06 }, 0.03);
    },
    coins: function () {
      for (let i = 0; i < 10; i++) sfx.clinkAt(i * 0.05 + Math.random() * 0.03);
    },
    clinkAt: function (dt) {
      const f = 2400 + Math.random() * 1800;
      tone({ type: 'sine', f: f, d: 0.1, v: 0.05 }, dt);
    }
  };

  /* ---------------------------------------------------------------- music */

  const THEMES = {
    lobby: { bpm: 112, root: 48, prog: [[0, 4, 7, 11], [9, 12, 16, 19], [5, 9, 12, 16], [7, 11, 14, 17]], arp: [0, 1, 2, 3, 2, 1, 2, 3], arpType: 'square', bass: [0, null, 0, null, 7, null, 0, null], kick: [0, 8], hat: true, arpEvery: 2, swing: 0 },
    neon: { bpm: 104, root: 45, prog: [[0, 3, 7, 12], [8, 12, 15, 20], [3, 7, 10, 15], [10, 14, 17, 22]], arp: [0, 1, 2, 3, 2, 3, 1, 2], arpType: 'sawtooth', bass: [0, 0, 12, 0, 0, 12, 0, 12], kick: [0, 4, 8, 12], hat: true, arpEvery: 1 },
    dragon: { bpm: 84, root: 38, prog: [[0, 3, 7, 12], [-2, 2, 5, 10], [0, 3, 7, 12], [-4, 0, 3, 8]], arp: [0, 2, 1, 3, 2, 1, 3, 2], arpType: 'triangle', bass: [0, null, null, null, 0, null, 7, null], kick: [0, 6, 8], hat: false, arpEvery: 2 },
    cosmic: { bpm: 96, root: 40, prog: [[0, 7, 12, 15], [-4, 3, 8, 12], [3, 10, 15, 19], [-2, 5, 10, 14]], arp: [0, 1, 2, 3, 1, 2, 3, 2], arpType: 'sine', bass: [0, null, null, 0, null, null, 0, null], kick: [0, 10], hat: true, arpEvery: 1 }
  };

  function playStep(th, s, t) {
    const sixteenth = 60 / th.bpm / 4;
    const bar = Math.floor(s / 16);
    const chord = th.prog[bar % th.prog.length];
    const pos = s % 16;
    if (pos % 2 === 0) {
      const b = th.bass[(pos / 2) % th.bass.length];
      if (b !== null && b !== undefined) {
        note({ type: th.id === 'cosmic' ? 'sine' : 'triangle', f: midi(th.root + chord[0] + b), d: sixteenth * 1.8, v: 0.16, a: 0.01 }, t, musicBus);
      }
    }
    if (pos % th.arpEvery === 0) {
      const idx = th.arp[(pos / th.arpEvery) % th.arp.length];
      const m = th.root + 24 + chord[idx % chord.length];
      note({ type: th.arpType, f: midi(m), d: sixteenth * 1.4, v: th.arpType === 'sine' ? 0.06 : 0.028, a: 0.004, filter: th.arpType === 'sine' ? null : { f: 2600 } }, t, musicBus);
      if (th.id === 'cosmic' && pos % 4 === 0) {
        note({ type: 'sine', f: midi(m + 12), d: sixteenth * 3, v: 0.02, a: 0.01 }, t + sixteenth * 3, musicBus);
      }
    }
    if (th.kick.indexOf(pos) >= 0) {
      note({ type: 'sine', f: 120, to: 40, d: 0.16, v: th.id === 'dragon' ? 0.3 : 0.22 }, t, musicBus);
    }
    if (th.hat && pos % 4 === 2) {
      noise({ type: 'highpass', f: 7000, d: 0.04, v: 0.035 }, t, musicBus);
    }
    if (pos === 0) {
      chord.slice(0, 3).forEach(function (c) {
        note({ type: 'sine', f: midi(th.root + 12 + c), d: sixteenth * 14, v: 0.025, a: 0.25 }, t, musicBus);
      });
    }
  }

  function startSeq() {
    if (!ctx || !theme || seqTimer) return;
    nextTime = ctx.currentTime + 0.08;
    step = 0;
    musicBus.gain.cancelScheduledValues(ctx.currentTime);
    musicBus.gain.setValueAtTime(musicBus.gain.value, ctx.currentTime);
    musicBus.gain.linearRampToValueAtTime(0.55, ctx.currentTime + 1.2);
    seqTimer = setInterval(function () {
      if (!ctx || ctx.state !== 'running') return;
      const th = THEMES[theme];
      const sixteenth = 60 / th.bpm / 4;
      while (nextTime < ctx.currentTime + 0.15) {
        playStep(th, step, nextTime);
        nextTime += sixteenth;
        step++;
      }
    }, 30);
  }

  function stopSeq() {
    if (seqTimer) { clearInterval(seqTimer); seqTimer = 0; }
    if (ctx) {
      musicBus.gain.cancelScheduledValues(ctx.currentTime);
      musicBus.gain.setValueAtTime(musicBus.gain.value, ctx.currentTime);
      musicBus.gain.linearRampToValueAtTime(0, ctx.currentTime + 0.3);
    }
  }

  Object.keys(THEMES).forEach(function (k) { THEMES[k].id = k; });

  const api = {
    unlock: unlock,
    setSfx: function (on) { sfxOn = !!on; if (!on) sfx.anticipationEnd && antic && sfx.anticipationEnd(); },
    setMusic: function (on) {
      musicOn = !!on;
      if (!musicOn) stopSeq();
      else if (ctx && theme) startSeq();
    },
    music: function (id) {
      if (theme === id && seqTimer) return;
      theme = id;
      if (seqTimer) { clearInterval(seqTimer); seqTimer = 0; }
      if (musicOn && ctx && ctx.state === 'running') startSeq();
    },
    duck: function (on) {
      if (!ctx || !seqTimer) return;
      musicBus.gain.cancelScheduledValues(ctx.currentTime);
      musicBus.gain.setValueAtTime(musicBus.gain.value, ctx.currentTime);
      musicBus.gain.linearRampToValueAtTime(on ? 0.15 : 0.55, ctx.currentTime + 0.4);
    },
    suspend: function () { if (ctx && ctx.state === 'running') ctx.suspend(); },
    resume: function () { if (ctx && ctx.state === 'suspended') ctx.resume(); }
  };

  // Every effect is a no-op until audio is unlocked by a tap, or while muted.
  Object.keys(sfx).forEach(function (name) {
    api[name] = function () {
      if (!ready()) return;
      try { sfx[name].apply(null, arguments); } catch (e) { /* ignore audio glitches */ }
    };
  });

  root.SlotSound = api;
}(typeof self !== 'undefined' ? self : this));
