/*
 * Reel Riot - slot math engine.
 *
 * Pure game logic with no DOM access, so the exact same code runs in the
 * browser and in tools/simulate.js (which measures RTP and hit rates).
 *
 * Money is always expressed as a multiple of the total bet inside the
 * paytables; the engine converts to coins with the bet passed to spin().
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.SlotEngine = factory();
}(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  /* ------------------------------------------------------------------ RNG */

  function splitmix32(a) {
    return function () {
      a = (a + 0x9e3779b9) | 0;
      let t = a ^ (a >>> 16);
      t = Math.imul(t, 0x21f0aaad);
      t ^= t >>> 15;
      t = Math.imul(t, 0x735a2d97);
      return (t ^ (t >>> 15)) >>> 0;
    };
  }

  // sfc32: small, fast, and statistically solid PRNG.
  function sfc32(a, b, c, d) {
    return function () {
      a >>>= 0; b >>>= 0; c >>>= 0; d >>>= 0;
      let t = (a + b) | 0;
      a = b ^ (b >>> 9);
      b = (c + (c << 3)) | 0;
      c = (c << 21) | (c >>> 11);
      d = (d + 1) | 0;
      t = (t + d) | 0;
      c = (c + t) | 0;
      return (t >>> 0) / 4294967296;
    };
  }

  // Seeded when a seed is given (simulations), crypto-seeded otherwise.
  function makeRng(seed) {
    let w;
    if (seed === undefined || seed === null) {
      w = new Uint32Array(4);
      const c = typeof crypto !== 'undefined' ? crypto : null;
      if (c && c.getRandomValues) c.getRandomValues(w);
      else for (let i = 0; i < 4; i++) w[i] = (Math.random() * 4294967296) >>> 0;
    } else {
      const sm = splitmix32(seed >>> 0);
      w = [sm(), sm(), sm(), sm()];
    }
    const rng = sfc32(w[0], w[1], w[2], w[3]);
    for (let i = 0; i < 15; i++) rng();
    return rng;
  }

  /* -------------------------------------------------------- weight tables */

  function makeTable(weights) {
    const syms = [];
    const cum = [];
    let total = 0;
    Object.keys(weights).forEach(function (k) {
      const w = weights[k];
      if (w > 0) { total += w; syms.push(k); cum.push(total); }
    });
    return { syms: syms, cum: cum, total: total };
  }

  function pick(table, rng) {
    const x = rng() * table.total;
    const cum = table.cum;
    for (let i = 0; i < cum.length; i++) if (x < cum[i]) return table.syms[i];
    return table.syms[table.syms.length - 1];
  }

  function pickWeighted(pairs, rng) {
    // pairs: [[value, weight], ...]
    let total = 0;
    for (let i = 0; i < pairs.length; i++) total += pairs[i][1];
    let x = rng() * total;
    for (let i = 0; i < pairs.length; i++) {
      if (x < pairs[i][1]) return pairs[i][0];
      x -= pairs[i][1];
    }
    return pairs[pairs.length - 1][0];
  }

  function weightsForReel(weights, r) {
    const out = {};
    Object.keys(weights).forEach(function (k) {
      const v = weights[k];
      out[k] = Array.isArray(v) ? (v[r] || 0) : v;
    });
    return out;
  }

  /* --------------------------------------------------------------- compile */

  // Turns a machine config into a runtime machine with precomputed tables.
  function compile(cfg) {
    const m = Object.assign({}, cfg);
    m.reelTables = [];
    m.fsReelTables = [];
    for (let r = 0; r < cfg.reels; r++) {
      m.reelTables.push(makeTable(weightsForReel(cfg.weights, r)));
      m.fsReelTables.push(makeTable(weightsForReel(cfg.fsWeights || cfg.weights, r)));
    }
    m.payingSymbols = Object.keys(cfg.symbols).filter(function (k) {
      const s = cfg.symbols[k];
      return !s.kind || s.kind === 'normal';
    });
    m.symbolList = Object.keys(cfg.symbols);
    if (m.jackpot) {
      const share = m.jackpot.growthShare == null ? 0.15 : m.jackpot.growthShare;
      m.jackpot.tiers.forEach(function (t) {
        // Progressive growth per spin (in x bet). Over a long run this adds
        // `share` on top of the seed-based return of each tier.
        t.growth = share * t.seed / t.odds;
        t.cap = t.seed * (t.capMult || 3);
      });
    }
    return m;
  }

  /* ----------------------------------------------------------------- utils */

  function emptyGrid(R, H) {
    const g = new Array(R);
    for (let r = 0; r < R; r++) g[r] = new Array(H);
    return g;
  }

  function cloneGrid(g) {
    return g.map(function (col) { return col.slice(); });
  }

  function key(m, r, h) { return r * m.rows + h; }

  function freeRow(m, r, blocked, rng) {
    const options = [];
    for (let h = 0; h < m.rows; h++) if (!blocked.has(key(m, r, h))) options.push(h);
    if (!options.length) return -1;
    return options[Math.floor(rng() * options.length)];
  }

  function shuffled(arr, rng) {
    const a = arr.slice();
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(rng() * (i + 1));
      const t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }

  /* --------------------------------------------------------------- jackpot */

  // Per-spin probability of each jackpot tier for a given frequency factor.
  function jackpotChances(m, factor) {
    const tiers = m.jackpot.tiers;
    let ps = tiers.map(function (t) { return factor / t.odds; });
    let total = ps.reduce(function (a, b) { return a + b; }, 0);
    const cap = m.jackpot.maxChance || 0.4;
    if (total > cap) {
      ps = ps.map(function (p) { return p * cap / total; });
      total = cap;
    }
    return { ps: ps, total: total };
  }

  function rollJackpot(m, factor, rng) {
    const ps = jackpotChances(m, factor).ps;
    let x = rng();
    for (let i = 0; i < ps.length; i++) {
      if (x < ps[i]) return m.jackpot.tiers[i].id;
      x -= ps[i];
    }
    return null;
  }

  function createPools(m) {
    const pools = {};
    m.jackpot.tiers.forEach(function (t) { pools[t.id] = t.seed; });
    return pools;
  }

  function growPools(m, pools) {
    m.jackpot.tiers.forEach(function (t) {
      const v = typeof pools[t.id] === 'number' && isFinite(pools[t.id]) ? pools[t.id] : t.seed;
      pools[t.id] = Math.min(t.cap, v + t.growth);
    });
  }

  function awardJackpot(m, pools, tierId, bet) {
    const tier = m.jackpot.tiers.filter(function (t) { return t.id === tierId; })[0];
    const amount = Math.round((pools[tierId] || tier.seed) * bet);
    pools[tierId] = tier.seed;
    return amount;
  }

  // Long-run return of the whole machine for a jackpot frequency factor.
  // `m.rtp.base` (lines/ways/clusters + scatters + free spins + features) is
  // measured by tools/simulate.js; the jackpot part is exact.
  function estimateRtp(m, factor) {
    const ch = jackpotChances(m, factor);
    let jp = 0;
    m.jackpot.tiers.forEach(function (t, i) { jp += ch.ps[i] * t.seed + t.growth; });
    // Cash coins collected in the Hold & Spin jackpot game (measured per trigger).
    if (m.holdSpin && m.rtp.holdCash) jp += ch.total * m.rtp.holdCash;
    return { base: m.rtp.base, jackpot: jp, total: m.rtp.base + jp };
  }

  /* --------------------------------------------------------- placements */

  function placeScatters(m, grid, mode, blocked, rng) {
    const sc = m.scatter;
    const chance = mode === 'free' && sc.fsChance != null ? sc.fsChance : sc.chance;
    const cells = [];
    for (let r = 0; r < m.reels; r++) {
      const p = Array.isArray(chance) ? chance[r] : chance;
      if (rng() < p) {
        const h = freeRow(m, r, blocked, rng);
        if (h >= 0) {
          grid[r][h] = 'scatter';
          blocked.add(key(m, r, h));
          cells.push([r, h]);
        }
      }
    }
    return cells;
  }

  // Jackpot coins: 3+ on different reels means a jackpot was hit; 1-2 coins
  // are teasers that show up on ordinary spins.
  function placeCoins(m, grid, count, blocked, rng) {
    const cells = [];
    if (!count) return cells;
    const reels = shuffled(Array.from({ length: m.reels }, function (_, i) { return i; }), rng);
    for (let i = 0; i < reels.length && cells.length < count; i++) {
      const r = reels[i];
      const h = freeRow(m, r, blocked, rng);
      if (h < 0) continue;
      grid[r][h] = 'coin';
      blocked.add(key(m, r, h));
      cells.push([r, h]);
    }
    return cells;
  }

  function placeFire(m, grid, blocked, rng) {
    const f = m.fire;
    const n = pickWeighted(f.counts, rng);
    const options = [];
    for (let r = f.fromReel; r < m.reels; r++) {
      for (let h = 0; h < m.rows; h++) {
        if (!blocked.has(key(m, r, h)) && grid[r][h] !== 'wild') options.push([r, h]);
      }
    }
    const cells = shuffled(options, rng).slice(0, n);
    cells.forEach(function (c) { grid[c[0]][c[1]] = 'wild'; });
    return cells;
  }

  /* ---------------------------------------------------------- evaluation */

  function payOf(m, sym, n) {
    const s = m.symbols[sym];
    if (!s || !s.pays) return 0;
    return s.pays[n] || 0;
  }

  function evalLines(m, grid, unit, mult) {
    const wins = [];
    const R = m.reels;
    for (let li = 0; li < m.lines.length; li++) {
      const L = m.lines[li];
      let wildRun = 0;
      while (wildRun < R && grid[wildRun][L[wildRun]] === 'wild') wildRun++;
      let sym = null;
      let n = 0;
      if (wildRun < R) {
        const s = grid[wildRun][L[wildRun]];
        if (m.symbols[s] && m.symbols[s].pays) {
          sym = s;
          n = wildRun + 1;
          for (let r = n; r < R; r++) {
            const c = grid[r][L[r]];
            if (c === s || c === 'wild') n++; else break;
          }
        }
      }
      let pay = sym ? payOf(m, sym, n) : 0;
      let paySym = sym;
      let payN = n;
      const wp = payOf(m, 'wild', wildRun);
      if (wp > pay) { pay = wp; paySym = 'wild'; payN = wildRun; }
      if (pay > 0) {
        const cells = [];
        for (let r = 0; r < payN; r++) cells.push([r, L[r]]);
        wins.push({ kind: 'line', line: li, sym: paySym, count: payN, cells: cells, amount: pay * unit * mult });
      }
    }
    return wins;
  }

  function evalWays(m, grid, unit, mult) {
    const wins = [];
    const seen = {};
    for (let h = 0; h < m.rows; h++) {
      const s = grid[0][h];
      if (seen[s] || !m.symbols[s] || !m.symbols[s].pays) continue;
      seen[s] = true;
      let ways = 1;
      let n = 0;
      const cells = [];
      for (let r = 0; r < m.reels; r++) {
        let c = 0;
        for (let k = 0; k < m.rows; k++) {
          const x = grid[r][k];
          if (x === s || x === 'wild') { c++; cells.push([r, k]); }
        }
        if (c === 0) break;
        ways *= c;
        n++;
      }
      const pay = payOf(m, s, n);
      if (pay > 0) wins.push({ kind: 'ways', sym: s, count: n, ways: ways, cells: cells, amount: pay * ways * unit * mult });
    }
    return wins;
  }

  function clusterPay(m, sym, size) {
    const pays = m.symbols[sym].pays;
    const i = Math.min(size - m.cluster.min, pays.length - 1);
    return i < 0 ? 0 : pays[i];
  }

  function evalClusters(m, grid, bet, mult) {
    const R = m.reels;
    const H = m.rows;
    const wins = [];
    for (let si = 0; si < m.payingSymbols.length; si++) {
      const s = m.payingSymbols[si];
      const visited = new Uint8Array(R * H);
      for (let r = 0; r < R; r++) {
        for (let h = 0; h < H; h++) {
          const start = r * H + h;
          if (visited[start] || grid[r][h] !== s) continue;
          visited[start] = 1;
          const stack = [start];
          const comp = [];
          while (stack.length) {
            const cur = stack.pop();
            const cr = (cur / H) | 0;
            const ch = cur - cr * H;
            comp.push([cr, ch]);
            const nb = [[cr - 1, ch], [cr + 1, ch], [cr, ch - 1], [cr, ch + 1]];
            for (let k = 0; k < 4; k++) {
              const nr = nb[k][0];
              const nh = nb[k][1];
              if (nr < 0 || nr >= R || nh < 0 || nh >= H) continue;
              const ni = nr * H + nh;
              if (visited[ni]) continue;
              const v = grid[nr][nh];
              if (v === s || v === 'wild') { visited[ni] = 1; stack.push(ni); }
            }
          }
          if (comp.length >= m.cluster.min) {
            wins.push({ kind: 'cluster', sym: s, size: comp.length, cells: comp, amount: clusterPay(m, s, comp.length) * bet * mult });
          }
        }
      }
    }
    return wins;
  }

  function dropCells(m, grid, removed, tables, rng) {
    const next = [];
    for (let r = 0; r < m.reels; r++) {
      const kept = [];
      for (let h = 0; h < m.rows; h++) if (!removed.has(key(m, r, h))) kept.push(grid[r][h]);
      const fresh = [];
      for (let i = kept.length; i < m.rows; i++) fresh.push(pick(tables[r], rng));
      next.push(fresh.concat(kept));
    }
    return next;
  }

  function ladderValue(m, mode, idx) {
    const L = mode === 'free' ? m.cluster.fsLadder : m.cluster.ladder;
    if (Array.isArray(L)) return L[Math.min(idx, L.length - 1)];
    return L.start + idx * L.step; // linear ladder {start, step}
  }

  /* -------------------------------------------------------- hold & spin */

  /*
   * Builds a Hold & Spin round for a jackpot that was already decided.
   * The trigger coins are held, every empty cell can land a coin on each
   * respin, any landing resets the respins, and only the Grand fills the
   * grid. The won jackpot (Mini/Minor/Major) is printed on one coin; all
   * other coins carry cash worth `v` times the bet.
   */
  function holdAndSpin(m, tier, startCells, rng) {
    const hs = m.holdSpin;
    const total = m.reels * m.rows;
    const grand = tier === 'grand';
    const p = grand ? hs.grandLand : hs.land;
    const held = {};
    const order = [];
    const landedOrder = [];
    function addCoin(r, h, initial) {
      const c = { r: r, h: h, v: pickWeighted(hs.values, rng), jp: null };
      held[key(m, r, h)] = c;
      order.push(c);
      if (!initial) landedOrder.push(c);
      return c;
    }
    const start = startCells.map(function (c) { return addCoin(c[0], c[1], true); });
    const steps = [];
    let respins = hs.respins;
    while (respins > 0 && order.length < total) {
      const empty = [];
      for (let r = 0; r < m.reels; r++) for (let h = 0; h < m.rows; h++) if (!held[key(m, r, h)]) empty.push([r, h]);
      let landed = empty.filter(function () { return rng() < p; });
      if (!grand && order.length + landed.length >= total) landed = landed.slice(0, total - 1 - order.length);
      if (grand && !landed.length && respins === 1) landed = [empty[Math.floor(rng() * empty.length)]];
      const step = { landed: landed.map(function (c) { return addCoin(c[0], c[1], false); }), respins: 0 };
      respins = step.landed.length ? hs.respins : respins - 1;
      step.respins = respins;
      steps.push(step);
    }
    if (!grand) {
      const pool = landedOrder.length && rng() < 0.75 ? landedOrder : order;
      const c = pool[Math.floor(rng() * pool.length)];
      c.jp = tier;
      c.v = 0;
    }
    let cashX = 0;
    order.forEach(function (c) { cashX += c.v; });
    return { tier: tier, start: start, steps: steps, coins: order, cashX: cashX, full: order.length === total, respins: hs.respins };
  }

  /* ----------------------------------------------------------------- spin */

  /*
   * One spin. opts:
   *   bet            total bet in coins
   *   mode           'base' | 'free'
   *   jackpotFactor  multiplier on jackpot chances (settings slider)
   *   fs             free spins state (from createFreeSpins) in 'free' mode
   */
  function spin(m, opts, rng) {
    const mode = opts.mode || 'base';
    const bet = opts.bet;
    const fs = opts.fs || null;
    const R = m.reels;
    const H = m.rows;
    const tables = mode === 'free' ? m.fsReelTables : m.reelTables;

    const grid = emptyGrid(R, H);
    for (let r = 0; r < R; r++) for (let h = 0; h < H; h++) grid[r][h] = pick(tables[r], rng);

    const blocked = new Set();
    if (fs && fs.sticky) {
      fs.sticky.forEach(function (c) { grid[c[0]][c[1]] = 'wild'; blocked.add(key(m, c[0], c[1])); });
    }

    const scatterCells = placeScatters(m, grid, mode, blocked, rng);

    let jackpot = null;
    let coinCells = [];
    if (m.jackpot && mode === 'base') {
      jackpot = rollJackpot(m, opts.jackpotFactor == null ? 1 : opts.jackpotFactor, rng);
      const count = jackpot ? pickWeighted(m.jackpot.hitCoins, rng) : pickWeighted(m.jackpot.teaseCoins, rng);
      coinCells = placeCoins(m, grid, count, blocked, rng);
    }

    const holdSpin = jackpot && m.holdSpin ? holdAndSpin(m, jackpot, coinCells, rng) : null;

    let fire = null;
    let preFireGrid = null;
    if (m.fire && mode === 'base' && rng() < m.fire.chance) {
      preFireGrid = cloneGrid(grid);
      fire = placeFire(m, grid, blocked, rng);
    }

    let newSticky = null;
    if (mode === 'free' && m.freeSpins.sticky) {
      newSticky = [];
      for (let r = 0; r < R; r++) {
        for (let h = 0; h < H; h++) {
          if (grid[r][h] === 'wild' && !blocked.has(key(m, r, h))) newSticky.push([r, h]);
        }
      }
    }

    const mult = mode === 'free' ? (m.freeSpins.multiplier || 1) : 1;
    const steps = [];
    let lineWin = 0;
    let ladderIdx = mode === 'free' && fs ? fs.ladder : 0;

    if (m.mode === 'cluster') {
      let g = grid;
      const refill = mode === 'free' ? m.fsReelTables : m.reelTables;
      for (let guard = 0; guard < 60; guard++) {
        const lv = ladderValue(m, mode, ladderIdx);
        const wins = evalClusters(m, g, bet, lv);
        if (!wins.length) {
          steps.push({ grid: g, wins: [], win: 0, mult: lv, removed: null });
          break;
        }
        const removed = new Set();
        let w = 0;
        wins.forEach(function (x) {
          w += x.amount;
          x.cells.forEach(function (c) { removed.add(key(m, c[0], c[1])); });
        });
        steps.push({ grid: g, wins: wins, win: w, mult: lv, removed: Array.from(removed) });
        lineWin += w;
        g = dropCells(m, g, removed, refill, rng);
        ladderIdx++;
      }
    } else {
      const wins = m.mode === 'lines'
        ? evalLines(m, grid, bet / m.lines.length, mult)
        : evalWays(m, grid, bet / m.waysDivisor, mult);
      wins.forEach(function (x) { lineWin += x.amount; });
      steps.push({ grid: grid, wins: wins, win: lineWin, mult: mult, removed: null });
    }

    // Scatter pays and free spins awards.
    const sc = scatterCells.length;
    const scatterMult = m.mode === 'cluster' ? 1 : mult;
    const scatterPay = (m.scatter.pays[sc] || 0) * bet * scatterMult;
    const awardTable = mode === 'free' ? m.freeSpins.retrigger : m.freeSpins.award;
    const spinsAwarded = awardTable[sc] || 0;

    let win = lineWin + scatterPay;
    let capped = false;
    const capLeft = mode === 'free' && fs ? fs.capLeft : m.maxWin * bet;
    if (win >= capLeft) { win = capLeft; capped = true; }
    win = Math.round(win);

    // Final grid after cascades (scatters and coins can move down).
    const finalGrid = steps[steps.length - 1].grid;
    const finalScatters = [];
    const finalCoins = [];
    for (let r = 0; r < R; r++) {
      for (let h = 0; h < H; h++) {
        if (finalGrid[r][h] === 'scatter') finalScatters.push([r, h]);
        else if (finalGrid[r][h] === 'coin') finalCoins.push([r, h]);
      }
    }

    return {
      mode: mode,
      bet: bet,
      grid: preFireGrid || grid,   // what the reels land on
      featureGrid: grid,           // after the dragon fire feature
      fire: fire,
      steps: steps,
      finalGrid: finalGrid,
      lineWin: lineWin,
      scatter: { count: sc, cells: scatterCells, finalCells: finalScatters, pay: scatterPay, spins: spinsAwarded },
      coins: { count: coinCells.length, cells: coinCells, finalCells: finalCoins },
      jackpot: jackpot,
      holdSpin: holdSpin,
      newSticky: newSticky,
      ladderAfter: m.mode === 'cluster' ? ladderIdx : null,
      multiplier: mult,
      win: win,
      capped: capped
    };
  }

  /* ------------------------------------------------------------ free spins */

  function createFreeSpins(m, spins, bet) {
    return {
      spins: spins,
      played: 0,
      total: 0,
      bet: bet,
      sticky: [],
      ladder: 0,
      capLeft: m.maxWin * bet,
      retriggers: 0,
      done: false
    };
  }

  // Plays one free spin and updates the free spins state in place.
  function freeSpin(m, fs, rng) {
    const res = spin(m, { mode: 'free', bet: fs.bet, fs: fs }, rng);
    fs.capLeft -= res.win;
    fs.total += res.win;
    fs.played++;
    if (res.newSticky && res.newSticky.length) fs.sticky = fs.sticky.concat(res.newSticky);
    if (res.ladderAfter != null) fs.ladder = res.ladderAfter;
    if (res.scatter.spins) { fs.spins += res.scatter.spins; fs.retriggers++; }
    if (res.capped || fs.played >= fs.spins) fs.done = true;
    return res;
  }

  /* ------------------------------------------------------------ win tiers */

  const WIN_TIERS = [
    { id: 'legendary', label: 'LEGENDARY WIN', x: 100 },
    { id: 'epic', label: 'EPIC WIN', x: 50 },
    { id: 'mega', label: 'MEGA WIN', x: 25 },
    { id: 'big', label: 'BIG WIN', x: 10 }
  ];

  function winTier(win, bet) {
    const x = win / bet;
    for (let i = 0; i < WIN_TIERS.length; i++) if (x >= WIN_TIERS[i].x) return WIN_TIERS[i];
    return null;
  }

  return {
    makeRng: makeRng,
    compile: compile,
    spin: spin,
    createFreeSpins: createFreeSpins,
    freeSpin: freeSpin,
    jackpotChances: jackpotChances,
    createPools: createPools,
    growPools: growPools,
    awardJackpot: awardJackpot,
    estimateRtp: estimateRtp,
    holdAndSpin: holdAndSpin,
    winTier: winTier,
    WIN_TIERS: WIN_TIERS,
    pick: pick,
    ladderValue: ladderValue
  };
}));
