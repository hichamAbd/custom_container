/*
 * Reel Riot - player progression: XP and levels, the 7-day login streak,
 * daily missions and the timed bonuses. Pure logic on a plain `progress`
 * object, so it runs in the browser and in Node alike.
 *
 * Every function that needs "now" uses P.now(progress), which adds the
 * Control Room's test clock offset to the real time.
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.SlotProgress = factory();
}(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  const MAX_LEVEL = 999;
  const HOUR = 3600 * 1000;
  const MINUTE = 60 * 1000;
  const INTERVALS = { coins: 1 * HOUR, wheel: 4 * HOUR, rescue: 15 * MINUTE };
  const FAST_INTERVALS = { coins: 1 * MINUTE, wheel: 2 * MINUTE, rescue: 0.5 * MINUTE };

  // "App-store tight" economy, before the level bonus is applied.
  const DAILY = [300e3, 500e3, 750e3, 1e6, 1.5e6, 2e6, 4e6];
  const FREE_COINS = 200e3;
  const RESCUE = 1e6;
  const WHEEL = [
    { v: 250e3, w: 22 }, { v: 1e6, w: 14 }, { v: 500e3, w: 20 }, { v: 2.5e6, w: 9 },
    { v: 750e3, w: 16 }, { v: 5e6, w: 6 }, { v: 1.5e6, w: 11 }, { v: 10e6, w: 2 }
  ];
  const MISSION_REWARD = { easy: { coins: 200e3, xp: 150 }, medium: { coins: 400e3, xp: 300 }, hard: { coins: 750e3, xp: 500 } };
  const CHEST = { coins: 1.5e6, xp: 1000 };

  const MISSIONS = {
    spins: { diff: 'easy', target: 40, event: 'spin' },
    wins: { diff: 'easy', target: 15, event: 'win' },
    bet: { diff: 'easy', target: 8e6, scale: true, event: 'bet', amount: true },
    neon: { diff: 'easy', target: 25, event: 'spin', machine: 'neon' },
    dragon: { diff: 'easy', target: 25, event: 'spin', machine: 'dragon' },
    cosmic: { diff: 'easy', target: 25, event: 'spin', machine: 'cosmic' },
    won: { diff: 'medium', target: 4e6, scale: true, event: 'won', amount: true },
    scatter: { diff: 'medium', target: 5, event: 'scatter2' },
    tease: { diff: 'medium', target: 2, event: 'coins2' },
    streak: { diff: 'medium', target: 1, event: 'streak3' },
    fire: { diff: 'medium', target: 2, event: 'fire' },
    cascade: { diff: 'medium', target: 1, event: 'cascade3' },
    big: { diff: 'hard', target: 1, event: 'bigwin' },
    fs: { diff: 'hard', target: 1, event: 'fs' }
  };
  const NAMES = { neon: 'Neon Fruit Frenzy', dragon: "Dragon's Hoard", cosmic: 'Cosmic Cascade' };

  /* ------------------------------------------------------------ helpers */

  function num(v) { return typeof v === 'number' && isFinite(v); }
  function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }

  // Two significant digits: 214,354 -> 210,000.
  function nice(v) {
    if (v <= 0) return 0;
    const mag = Math.pow(10, Math.max(0, Math.floor(Math.log10(v)) - 1));
    return Math.round(v / mag) * mag;
  }

  function short(n) {
    const units = [[1e15, 'Q'], [1e12, 'T'], [1e9, 'B'], [1e6, 'M'], [1e3, 'K']];
    for (let i = 0; i < units.length; i++) {
      if (n >= units[i][0]) {
        const x = n / units[i][0];
        const s = x.toFixed(x >= 100 ? 0 : x >= 10 ? 1 : 2);
        return (s.indexOf('.') >= 0 ? s.replace(/0+$/, '').replace(/\.$/, '') : s) + units[i][1];
      }
    }
    return String(Math.round(n));
  }

  function now(p) { return Date.now() + (p && num(p.clockOffset) ? p.clockOffset : 0); }

  function dayKey(ts) {
    const d = new Date(ts);
    const m = d.getMonth() + 1;
    const day = d.getDate();
    return d.getFullYear() + '-' + (m < 10 ? '0' : '') + m + '-' + (day < 10 ? '0' : '') + day;
  }
  function shiftDay(ts, days) {
    const d = new Date(ts);
    return new Date(d.getFullYear(), d.getMonth(), d.getDate() + days, 12).getTime();
  }

  /* -------------------------------------------------------------- state */

  function fresh() {
    return {
      level: 1,
      xp: 0,
      totalXp: 0,
      daily: { lastDay: null, streak: 0 },
      missions: null,
      timers: { coins: 0, wheel: 0, rescue: 0 },
      clockOffset: 0,
      fastTimers: false,
      winStreak: 0
    };
  }

  function normalize(raw) {
    const p = fresh();
    if (!raw || typeof raw !== 'object') return p;
    if (Number.isInteger(raw.level)) p.level = clamp(raw.level, 1, MAX_LEVEL);
    if (num(raw.xp)) p.xp = clamp(Math.floor(raw.xp), 0, xpNeeded(p.level) - 1);
    if (num(raw.totalXp)) p.totalXp = Math.max(0, raw.totalXp);
    if (raw.daily && typeof raw.daily === 'object') {
      if (typeof raw.daily.lastDay === 'string' && /^\d{4}-\d\d-\d\d$/.test(raw.daily.lastDay)) p.daily.lastDay = raw.daily.lastDay;
      if (Number.isInteger(raw.daily.streak)) p.daily.streak = clamp(raw.daily.streak, 0, 7);
    }
    if (raw.timers) ['coins', 'wheel', 'rescue'].forEach(function (k) { if (num(raw.timers[k])) p.timers[k] = raw.timers[k]; });
    if (num(raw.clockOffset)) p.clockOffset = raw.clockOffset;
    if (typeof raw.fastTimers === 'boolean') p.fastTimers = raw.fastTimers;
    if (Number.isInteger(raw.winStreak)) p.winStreak = Math.max(0, raw.winStreak);
    const ms = raw.missions;
    if (ms && typeof ms.day === 'string' && Array.isArray(ms.list) && ms.list.length === 3) {
      const list = ms.list.filter(function (m) {
        return m && MISSIONS[m.kind] && num(m.target) && num(m.progress) && num(m.coins) && num(m.xp);
      }).map(function (m) {
        return { kind: m.kind, target: m.target, progress: clamp(m.progress, 0, m.target), done: m.progress >= m.target, claimed: !!m.claimed, coins: m.coins, xp: m.xp, diff: MISSIONS[m.kind].diff };
      });
      if (list.length === 3) p.missions = { day: ms.day, list: list, chest: !!ms.chest };
    }
    return p;
  }

  /* ------------------------------------------------------- XP and levels */

  function xpNeeded(level) { return Math.round(600 * Math.pow(level, 1.45)); }
  function levelMult(level) { return 1 + 0.05 * (level - 1); }
  function levelReward(level) { return nice(100e3 * Math.pow(level, 1.1)); }
  // More XP for bigger bets, but sub-linear so huge bets do not skip whole levels.
  function spinXp(bet) { return Math.max(1, Math.round(10 * Math.sqrt(bet / 1000))); }

  function addXp(p, amount) {
    const ups = [];
    if (p.level >= MAX_LEVEL) return ups;
    p.xp += Math.max(0, Math.round(amount));
    p.totalXp += Math.max(0, Math.round(amount));
    while (p.level < MAX_LEVEL && p.xp >= xpNeeded(p.level)) {
      p.xp -= xpNeeded(p.level);
      p.level++;
      ups.push({ level: p.level, coins: levelReward(p.level) });
    }
    if (p.level >= MAX_LEVEL) p.xp = 0;
    return ups;
  }

  function setLevel(p, level, fraction) {
    p.level = clamp(Math.round(level), 1, MAX_LEVEL);
    p.xp = Math.floor(xpNeeded(p.level) * clamp(fraction || 0, 0, 0.999));
  }

  /* ----------------------------------------------------------- daily */

  function dailyAmount(p, day) { return nice(DAILY[clamp(day, 1, 7) - 1] * levelMult(p.level)); }

  function dailyStatus(p) {
    const t = now(p);
    const today = dayKey(t);
    const yesterday = dayKey(shiftDay(t, -1));
    const d = p.daily;
    const claimedToday = d.lastDay === today;
    let day;
    if (claimedToday) day = Math.max(1, d.streak);
    else if (d.lastDay === yesterday && d.streak > 0) day = d.streak % 7 + 1;
    else day = 1;
    const amounts = [];
    for (let i = 1; i <= 7; i++) amounts.push(dailyAmount(p, i));
    return { claimable: !claimedToday, claimedToday: claimedToday, day: day, amounts: amounts, today: today };
  }

  function claimDaily(p) {
    const st = dailyStatus(p);
    if (!st.claimable) return null;
    p.daily.lastDay = st.today;
    p.daily.streak = st.day;
    return { day: st.day, amount: st.amounts[st.day - 1] };
  }

  // Control Room helpers.
  function setStreakDay(p, day) {
    day = clamp(Math.round(day), 1, 7);
    const t = now(p);
    p.daily.streak = day - 1;
    p.daily.lastDay = day > 1 ? dayKey(shiftDay(t, -1)) : dayKey(shiftDay(t, -3));
  }
  function nextDay(p) {
    // Pretend today's bonus was collected yesterday, so the next one is ready now.
    if (p.daily.lastDay === dayKey(now(p))) p.daily.lastDay = dayKey(shiftDay(now(p), -1));
  }
  function breakStreak(p) {
    p.daily.lastDay = dayKey(shiftDay(now(p), -3));
  }

  /* --------------------------------------------------------- timers */

  function interval(p, k) { return (p.fastTimers ? FAST_INTERVALS : INTERVALS)[k]; }
  function timeLeft(p, k) { return Math.max(0, (p.timers[k] || 0) - now(p)); }
  function isReady(p, k) { return timeLeft(p, k) <= 0; }
  function arm(p, k) { p.timers[k] = now(p) + interval(p, k); }
  function makeReady(p, k) { p.timers[k] = 0; }
  function setFastTimers(p, on) {
    p.fastTimers = !!on;
    if (on) ['coins', 'wheel', 'rescue'].forEach(function (k) { p.timers[k] = Math.min(p.timers[k] || 0, now(p) + FAST_INTERVALS[k]); });
  }

  function freeCoinsAmount(p) { return nice(FREE_COINS * levelMult(p.level)); }
  function claimFreeCoins(p) {
    if (!isReady(p, 'coins')) return 0;
    arm(p, 'coins');
    return freeCoinsAmount(p);
  }
  function wheelPrizes(p) {
    return WHEEL.map(function (s) { return { v: nice(s.v * levelMult(p.level)), w: s.w }; });
  }
  function rescue(p) {
    if (!isReady(p, 'rescue')) return 0;
    arm(p, 'rescue');
    return RESCUE;
  }

  /* --------------------------------------------------------- missions */

  function makeMission(p, kind) {
    const def = MISSIONS[kind];
    const r = MISSION_REWARD[def.diff];
    const mult = levelMult(p.level);
    return {
      kind: kind,
      target: def.scale ? nice(def.target * mult) : def.target,
      progress: 0,
      done: false,
      claimed: false,
      coins: nice(r.coins * mult),
      xp: Math.round(r.xp * mult),
      diff: def.diff
    };
  }

  function pickMissions(p, rand) {
    rand = rand || Math.random;
    const by = { easy: [], medium: [], hard: [] };
    Object.keys(MISSIONS).forEach(function (k) { by[MISSIONS[k].diff].push(k); });
    return ['easy', 'medium', 'hard'].map(function (d) {
      const list = by[d];
      return makeMission(p, list[Math.floor(rand() * list.length)]);
    });
  }

  function ensureMissions(p, rand) {
    const today = dayKey(now(p));
    if (!p.missions || p.missions.day !== today) p.missions = { day: today, list: pickMissions(p, rand), chest: false };
    return p.missions;
  }

  function rerollMissions(p, rand) {
    p.missions = { day: dayKey(now(p)), list: pickMissions(p, rand), chest: false };
    return p.missions;
  }

  function describe(m) {
    const t = m.target;
    switch (m.kind) {
      case 'spins': return 'Spin ' + t + ' times';
      case 'wins': return 'Win ' + t + ' spins';
      case 'bet': return 'Bet ' + short(t) + ' coins in total';
      case 'neon': case 'dragon': case 'cosmic': return 'Play ' + t + ' spins on ' + NAMES[m.kind];
      case 'won': return 'Win ' + short(t) + ' coins in total';
      case 'scatter': return 'Land 2+ bonus symbols in one spin, ' + t + ' times';
      case 'tease': return 'Land 2+ jackpot coins in one spin, ' + t + ' times';
      case 'streak': return 'Win 3 spins in a row';
      case 'fire': return 'Trigger Dragon Fire ' + t + ' times';
      case 'cascade': return 'Chain 3 cascades in one spin on Cosmic Cascade';
      case 'big': return 'Land a Big Win (10× your bet or more)';
      case 'fs': return 'Trigger free spins on any machine';
      default: return m.kind;
    }
  }

  function bump(p, event, amount, machine, out) {
    if (!p.missions) return;
    p.missions.list.forEach(function (m) {
      if (m.done) return;
      const def = MISSIONS[m.kind];
      if (def.event !== event) return;
      if (def.machine && def.machine !== machine) return;
      m.progress = Math.min(m.target, m.progress + (def.amount ? amount : 1));
      if (m.progress >= m.target) { m.done = true; out.push(m); }
    });
  }

  /*
   * Records one finished paid spin (including anything it triggered).
   * info: { machine, bet, total, bigWin, freeSpins, scatters, coins, fire, cascades }
   * Returns the missions completed by this spin.
   */
  function recordSpin(p, info) {
    ensureMissions(p);
    const done = [];
    bump(p, 'spin', 1, info.machine, done);
    bump(p, 'bet', info.bet, info.machine, done);
    if (info.total > 0) {
      p.winStreak++;
      bump(p, 'win', 1, info.machine, done);
      bump(p, 'won', info.total, info.machine, done);
      if (p.winStreak >= 3) bump(p, 'streak3', 1, info.machine, done);
    } else {
      p.winStreak = 0;
    }
    if (info.bigWin) bump(p, 'bigwin', 1, info.machine, done);
    if (info.freeSpins) bump(p, 'fs', 1, info.machine, done);
    if (info.scatters >= 2) bump(p, 'scatter2', 1, info.machine, done);
    if (info.coins >= 2) bump(p, 'coins2', 1, info.machine, done);
    if (info.fire) bump(p, 'fire', 1, info.machine, done);
    if (info.cascades >= 3) bump(p, 'cascade3', 1, info.machine, done);
    return done;
  }

  function claimMission(p, i) {
    const m = p.missions && p.missions.list[i];
    if (!m || !m.done || m.claimed) return null;
    m.claimed = true;
    return { coins: m.coins, xp: m.xp };
  }

  function completeMission(p, i) {
    const m = p.missions && p.missions.list[i];
    if (!m || m.done) return false;
    m.progress = m.target;
    m.done = true;
    return true;
  }

  function chestStatus(p) {
    const list = p.missions ? p.missions.list : [];
    const claimed = list.filter(function (m) { return m.claimed; }).length;
    const mult = levelMult(p.level);
    return { claimed: claimed, ready: claimed === 3 && !p.missions.chest, opened: !!(p.missions && p.missions.chest), coins: nice(CHEST.coins * mult), xp: Math.round(CHEST.xp * mult) };
  }

  function claimChest(p) {
    const st = chestStatus(p);
    if (!st.ready) return null;
    p.missions.chest = true;
    return { coins: st.coins, xp: st.xp };
  }

  function msToNextDay(p) {
    const t = now(p);
    const d = new Date(t);
    return new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1).getTime() - t;
  }

  return {
    MAX_LEVEL: MAX_LEVEL,
    INTERVALS: INTERVALS,
    FAST_INTERVALS: FAST_INTERVALS,
    fresh: fresh,
    normalize: normalize,
    now: now,
    dayKey: dayKey,
    nice: nice,
    xpNeeded: xpNeeded,
    levelMult: levelMult,
    levelReward: levelReward,
    spinXp: spinXp,
    addXp: addXp,
    setLevel: setLevel,
    dailyStatus: dailyStatus,
    claimDaily: claimDaily,
    setStreakDay: setStreakDay,
    nextDay: nextDay,
    breakStreak: breakStreak,
    timeLeft: timeLeft,
    isReady: isReady,
    arm: arm,
    makeReady: makeReady,
    setFastTimers: setFastTimers,
    interval: interval,
    freeCoinsAmount: freeCoinsAmount,
    claimFreeCoins: claimFreeCoins,
    wheelPrizes: wheelPrizes,
    rescue: rescue,
    RESCUE: RESCUE,
    ensureMissions: ensureMissions,
    rerollMissions: rerollMissions,
    describe: describe,
    recordSpin: recordSpin,
    claimMission: claimMission,
    completeMission: completeMission,
    chestStatus: chestStatus,
    claimChest: claimChest,
    msToNextDay: msToNextDay
  };
}));
