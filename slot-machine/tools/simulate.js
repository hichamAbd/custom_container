#!/usr/bin/env node
/*
 * Monte Carlo balance check for every machine.
 *
 *   node tools/simulate.js                 1,000,000 spins per machine
 *   node tools/simulate.js 5000000 neon    5M spins of one machine
 *   node tools/simulate.js --factor 5      with jackpots 5x more frequent
 *   node tools/simulate.js --update        write measured rtp.base into js/machines.js
 *
 * It follows the same sequence as the game: pay the bet, grow the jackpot
 * pools, spin, pay wins, award any jackpot, then play free spins to the end.
 */
'use strict';

const fs = require('fs');
const path = require('path');
const Engine = require('../js/engine.js');
const Machines = require('../js/machines.js');

const args = process.argv.slice(2);
let spins = 1000000;
let only = null;
let factor = 1;
let update = false;
let seed = 20261101;
for (let i = 0; i < args.length; i++) {
  const a = args[i];
  if (a === '--factor') factor = parseFloat(args[++i]);
  else if (a === '--seed') seed = parseInt(args[++i], 10);
  else if (a === '--update') update = true;
  else if (/^\d+$/.test(a)) spins = parseInt(a, 10);
  else only = a;
}

function pct(x) { return (x * 100).toFixed(2) + '%'; }
function oneIn(count, n) { return count ? '1 in ' + Math.round(n / count).toLocaleString('en-US') : 'never'; }

function simulate(cfg) {
  const m = Engine.compile(JSON.parse(JSON.stringify(cfg)));
  const rng = Engine.makeRng(seed);
  const bet = 1000000; // large bet so whole-coin rounding is negligible
  const pools = Engine.createPools(m);
  const s = {
    bet: 0, base: 0, scatter: 0, free: 0, fire: 0, fireCount: 0, jackpot: 0,
    hits: 0, fsTriggers: 0, fsSpins: 0, fsWinX: [], tiers: {}, tierPaid: {},
    maxX: 0, sumSq: 0, buckets: { big: 0, mega: 0, epic: 0, legendary: 0 }, cascades: 0
  };
  m.jackpot.tiers.forEach(function (t) { s.tiers[t.id] = 0; s.tierPaid[t.id] = 0; });

  for (let i = 0; i < spins; i++) {
    s.bet += bet;
    Engine.growPools(m, pools);
    const res = Engine.spin(m, { mode: 'base', bet: bet, jackpotFactor: factor }, rng);
    let spinTotal = res.win;
    s.base += res.win - res.scatter.pay;
    s.scatter += res.scatter.pay;
    if (res.fire) {
      s.fireCount++;
      s.fire += res.win;
    }
    if (m.mode === 'cluster') s.cascades += res.steps.length - 1;
    if (res.jackpot) {
      const amt = Engine.awardJackpot(m, pools, res.jackpot, bet);
      s.jackpot += amt;
      s.tiers[res.jackpot]++;
      s.tierPaid[res.jackpot] += amt;
      spinTotal += amt;
    }
    if (res.scatter.spins) {
      s.fsTriggers++;
      const st = Engine.createFreeSpins(m, res.scatter.spins, bet);
      while (!st.done) Engine.freeSpin(m, st, rng);
      s.fsSpins += st.played;
      s.free += st.total;
      s.fsWinX.push(st.total / bet);
      spinTotal += st.total;
    }
    if (spinTotal > 0) s.hits++;
    const x = spinTotal / bet;
    if (x > s.maxX) s.maxX = x;
    s.sumSq += x * x;
    if (x >= 100) s.buckets.legendary++;
    else if (x >= 50) s.buckets.epic++;
    else if (x >= 25) s.buckets.mega++;
    else if (x >= 10) s.buckets.big++;
  }
  return { m: m, s: s, bet: bet };
}

function report(r) {
  const m = r.m;
  const s = r.s;
  const bet = r.bet;
  const total = s.base + s.scatter + s.free + s.jackpot;
  const nonJackpot = (s.base + s.scatter + s.free) / s.bet;
  const mean = total / s.bet;
  const sd = Math.sqrt(s.sumSq / spins - mean * mean);
  const fsAvg = s.fsWinX.length ? s.fsWinX.reduce(function (a, b) { return a + b; }, 0) / s.fsWinX.length : 0;
  const sorted = s.fsWinX.slice().sort(function (a, b) { return a - b; });
  const fsMedian = sorted.length ? sorted[Math.floor(sorted.length / 2)] : 0;
  const est = Engine.estimateRtp(m, factor);

  console.log('\n=== ' + m.name + ' (' + m.reels + 'x' + m.rows + ', ' + m.mode + ') — ' + spins.toLocaleString('en-US') + ' spins, jackpot x' + factor + ' ===');
  console.log('  Total RTP ............ ' + pct(mean) + '   (house edge ' + pct(1 - mean) + ')');
  console.log('    base game wins ..... ' + pct(s.base / s.bet));
  console.log('    scatter pays ....... ' + pct(s.scatter / s.bet));
  console.log('    free spins ......... ' + pct(s.free / s.bet));
  console.log('    jackpots ........... ' + pct(s.jackpot / s.bet) + '   (engine estimate ' + pct(est.jackpot) + ')');
  console.log('  Non-jackpot RTP ...... ' + pct(nonJackpot) + '   <- rtp.base in machines.js is ' + pct(m.rtp.base));
  console.log('  Hit frequency ........ ' + pct(s.hits / spins) + ' of spins pay something');
  console.log('  Free spins ........... ' + oneIn(s.fsTriggers, spins) + ', avg ' + fsAvg.toFixed(1) + 'x bet, median ' + fsMedian.toFixed(1) + 'x, avg ' + (s.fsTriggers ? (s.fsSpins / s.fsTriggers).toFixed(1) : 0) + ' spins');
  if (m.fire) console.log('  Dragon Fire .......... ' + oneIn(s.fireCount, spins) + ', avg win ' + (s.fireCount ? (s.fire / s.fireCount / bet).toFixed(2) : 0) + 'x');
  if (m.mode === 'cluster') console.log('  Cascades ............. ' + (s.cascades / spins).toFixed(3) + ' per spin');
  console.log('  Jackpots:');
  m.jackpot.tiers.forEach(function (t) {
    const n = s.tiers[t.id];
    const expected = Math.round(t.odds / Math.min(factor, 1e9));
    console.log('    ' + t.name.padEnd(6) + ' ' + oneIn(n, spins).padEnd(16) + ' (target 1 in ' + expected.toLocaleString('en-US') + ')  hits ' + n + ', avg ' + (n ? (s.tierPaid[t.id] / n / bet).toFixed(0) : '-') + 'x');
  });
  console.log('  Big wins (>=10x) ..... ' + oneIn(s.buckets.big + s.buckets.mega + s.buckets.epic + s.buckets.legendary, spins)
    + ' | mega 25x+ ' + oneIn(s.buckets.mega + s.buckets.epic + s.buckets.legendary, spins)
    + ' | epic 50x+ ' + oneIn(s.buckets.epic + s.buckets.legendary, spins)
    + ' | legendary 100x+ ' + oneIn(s.buckets.legendary, spins));
  console.log('  Biggest single spin .. ' + s.maxX.toFixed(0) + 'x bet   volatility (sd) ' + sd.toFixed(1));
  return nonJackpot;
}

const results = {};
Machines.list.forEach(function (cfg) {
  if (only && cfg.id !== only) return;
  const t0 = Date.now();
  const r = simulate(cfg);
  results[cfg.id] = report(r);
  console.log('  (' + ((Date.now() - t0) / 1000).toFixed(1) + 's)');
});

if (update) {
  const file = path.join(__dirname, '..', 'js', 'machines.js');
  let src = fs.readFileSync(file, 'utf8');
  Object.keys(results).forEach(function (id) {
    const re = new RegExp("(id: '" + id + "'[\\s\\S]*?rtp: \\{ base: )([0-9.]+)( \\})");
    src = src.replace(re, function (_, a, __, c) { return a + results[id].toFixed(4) + c; });
  });
  fs.writeFileSync(file, src);
  console.log('\nUpdated rtp.base in js/machines.js');
}
