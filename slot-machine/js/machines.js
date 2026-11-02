/*
 * Reel Riot - machine definitions.
 *
 * Paytables:
 *   lines   : symbol pays are multiples of the line bet (total bet / lines)
 *   ways    : symbol pays are multiples of (total bet / waysDivisor) per way
 *   cluster : symbol pays are multiples of the total bet, indexed by
 *             cluster size starting at cluster.min (last value = that size+)
 * Scatter pays are always multiples of the total bet.
 *
 * Jackpot tiers: `odds` is "1 in N base-game spins" at the Normal (x1)
 * jackpot frequency, `seed` is the starting value in x total bet.
 *
 * rtp.base is the long-run return of everything except jackpots, measured
 * with `node tools/simulate.js` (re-run it after changing any number here).
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.SlotMachines = factory();
}(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  const JACKPOT_SYMBOL = { kind: 'coin', name: 'Jackpot Coin', glyph: '', color: '#ffc531' };

  // 20 paylines on a 5x3 grid (row index per reel, 0 = top).
  const LINES_5x3 = [
    [1, 1, 1, 1, 1], [0, 0, 0, 0, 0], [2, 2, 2, 2, 2], [0, 1, 2, 1, 0],
    [2, 1, 0, 1, 2], [0, 0, 1, 2, 2], [2, 2, 1, 0, 0], [1, 0, 0, 0, 1],
    [1, 2, 2, 2, 1], [1, 0, 1, 2, 1], [1, 2, 1, 0, 1], [0, 1, 0, 1, 0],
    [2, 1, 2, 1, 2], [0, 1, 1, 1, 0], [2, 1, 1, 1, 2], [1, 1, 0, 1, 1],
    [1, 1, 2, 1, 1], [0, 2, 0, 2, 0], [2, 0, 2, 0, 2], [0, 2, 2, 2, 0]
  ];

  const neon = {
    id: 'neon',
    name: 'Neon Fruit Frenzy',
    short: 'Neon Fruits',
    tagline: 'Classic fruit, turned up to eleven',
    art: ['🍒', '💎', '🍉'],
    reels: 5,
    rows: 3,
    mode: 'lines',
    lines: LINES_5x3,
    volatility: 1,
    features: ['20 paylines', 'Free spins with all wins x3', 'Wilds on reels 2-4'],
    symbols: {
      cherry: { name: 'Cherry', glyph: '🍒', color: '#ff3d6e', pays: [0, 0, 0, 7, 25, 75] },
      lemon: { name: 'Lemon', glyph: '🍋', color: '#ffe14d', pays: [0, 0, 0, 7, 25, 75] },
      orange: { name: 'Orange', glyph: '🍊', color: '#ff9a3c', pays: [0, 0, 0, 10, 30, 90] },
      grapes: { name: 'Grapes', glyph: '🍇', color: '#b06cff', pays: [0, 0, 0, 15, 45, 150] },
      melon: { name: 'Watermelon', glyph: '🍉', color: '#3ee07a', pays: [0, 0, 0, 18, 60, 200] },
      bell: { name: 'Bell', glyph: '🔔', color: '#ffc531', pays: [0, 0, 0, 30, 90, 300] },
      diamond: { name: 'Diamond', glyph: '💎', color: '#4cc9ff', pays: [0, 0, 0, 45, 150, 600] },
      seven: { name: 'Lucky Seven', glyph: '7', text: true, color: '#ff2d55', pays: [0, 0, 0, 90, 400, 1500] },
      wild: { kind: 'wild', name: 'Wild', glyph: 'WILD', text: true, color: '#ff4fd8' },
      scatter: { kind: 'scatter', name: 'Lucky Star', glyph: '🌟', color: '#fff27a' },
      coin: JACKPOT_SYMBOL
    },
    weights: {
      cherry: 20, lemon: 20, orange: 18, grapes: 14, melon: 12, bell: 9, diamond: 6, seven: 4,
      wild: [0, 6, 6, 6, 0]
    },
    scatter: {
      chance: 0.095,                         // per reel, at most one star per reel
      pays: { 3: 2, 4: 10, 5: 50 },
      label: '3+ Lucky Stars anywhere'
    },
    freeSpins: {
      award: { 3: 10, 4: 15, 5: 25 },
      retrigger: { 3: 10, 4: 15, 5: 25 },
      multiplier: 3,
      title: 'ALL WINS x3'
    },
    jackpot: {
      tiers: [
        { id: 'mini', name: 'MINI', odds: 400, seed: 8 },
        { id: 'minor', name: 'MINOR', odds: 1500, seed: 30 },
        { id: 'major', name: 'MAJOR', odds: 6000, seed: 150 },
        { id: 'grand', name: 'GRAND', odds: 50000, seed: 1000 }
      ],
      hitCoins: [[3, 70], [4, 22], [5, 8]],
      teaseCoins: [[0, 86], [1, 10], [2, 4]],
      game: 'pick'
    },
    maxWin: 5000,
    rtp: { base: 0.8664 }
  };

  const dragon = {
    id: 'dragon',
    name: "Dragon's Hoard",
    short: "Dragon's Hoard",
    tagline: 'Wake the dragon, take the gold',
    art: ['👑', '🐉', '💍'],
    reels: 5,
    rows: 4,
    mode: 'ways',
    waysDivisor: 50,
    volatility: 2,
    features: ['1,024 ways to win', 'Dragon Fire random wilds', 'Free spins with sticky wilds'],
    symbols: {
      potion: { name: 'Potion', glyph: '🧪', color: '#35e0d0', pays: [0, 0, 0, 3, 7, 15] },
      scroll: { name: 'Scroll', glyph: '📜', color: '#e9c98f', pays: [0, 0, 0, 3, 7, 15] },
      key: { name: 'Gold Key', glyph: '🗝️', color: '#f6c445', pays: [0, 0, 0, 3, 8, 18] },
      bow: { name: 'Bow', glyph: '🏹', color: '#9be15d', pays: [0, 0, 0, 4, 12, 22] },
      shield: { name: 'Shield', glyph: '🛡️', color: '#6aa8ff', pays: [0, 0, 0, 6, 15, 40] },
      sword: { name: 'Crossed Swords', glyph: '⚔️', color: '#d9e2ec', pays: [0, 0, 0, 8, 22, 60] },
      ring: { name: 'Ruby Ring', glyph: '💍', color: '#ff6fae', pays: [0, 0, 0, 12, 40, 90] },
      crown: { name: 'Crown', glyph: '👑', color: '#ffd23f', pays: [0, 0, 0, 18, 60, 150] },
      dragon: { name: 'Dragon', glyph: '🐉', color: '#ff4b3a', pays: [0, 0, 0, 30, 110, 400] },
      wild: { kind: 'wild', name: 'Fire Wild', glyph: 'WILD', text: true, color: '#ff7a1a' },
      scatter: { kind: 'scatter', name: 'Dragon Egg', glyph: '🥚', color: '#ffe08a' },
      coin: JACKPOT_SYMBOL
    },
    weights: {
      potion: 20, scroll: 20, key: 18, bow: 16, shield: 12, sword: 10, ring: 8, crown: 6, dragon: 4,
      wild: [0, 3.3, 3.3, 3.3, 3.3]
    },
    fsWeights: {
      potion: 20, scroll: 20, key: 18, bow: 16, shield: 12, sword: 10, ring: 8, crown: 6, dragon: 4,
      wild: [0, 2.7, 2.7, 2.7, 2.7]
    },
    fire: {
      chance: 1 / 44,
      fromReel: 1,
      counts: [[2, 20], [3, 40], [4, 25], [5, 10], [6, 5]]
    },
    scatter: {
      chance: 0.09,
      pays: { 3: 2, 4: 10, 5: 40 },
      label: '3+ Dragon Eggs anywhere'
    },
    freeSpins: {
      award: { 3: 10, 4: 15, 5: 20 },
      retrigger: { 3: 5, 4: 8, 5: 10 },
      sticky: true,
      title: 'STICKY WILDS'
    },
    jackpot: {
      tiers: [
        { id: 'mini', name: 'MINI', odds: 500, seed: 10 },
        { id: 'minor', name: 'MINOR', odds: 2000, seed: 40 },
        { id: 'major', name: 'MAJOR', odds: 8000, seed: 180 },
        { id: 'grand', name: 'GRAND', odds: 75000, seed: 2000 }
      ],
      hitCoins: [[3, 65], [4, 25], [5, 10]],
      teaseCoins: [[0, 84], [1, 11], [2, 5]],
      game: 'holdspin'
    },
    // Jackpot game: held coins pay cash (x bet); a full grid is the Grand.
    holdSpin: {
      respins: 3,
      land: 0.035,
      grandLand: 0.3,
      values: [[0.25, 30], [0.5, 28], [1, 20], [1.5, 10], [2, 7], [5, 4], [10, 1]]
    },
    maxWin: 10000,
    rtp: { base: 0.8434, holdCash: 6.388 }
  };

  const cosmic = {
    id: 'cosmic',
    name: 'Cosmic Cascade',
    short: 'Cosmic Cascade',
    tagline: 'Chain reactions at light speed',
    art: ['🚀', '🪐', '👽'],
    reels: 6,
    rows: 5,
    mode: 'cluster',
    volatility: 3,
    features: ['Cluster pays (5+ touching)', 'Cascades raise the multiplier', 'Free spins keep the multiplier'],
    cluster: {
      min: 5,
      ladder: [1, 2, 3, 5, 8, 12, 20],
      fsLadder: { start: 1, step: 1 }
    },
    symbols: {
      moon: { name: 'Moon', glyph: '🌙', color: '#ffe066', pays: [0.5, 0.7, 1, 1.2, 1.5, 1.75, 2.5, 3.5, 5, 7, 10] },
      comet: { name: 'Comet', glyph: '☄️', color: '#ff9f45', pays: [0.6, 0.9, 1.2, 1.5, 1.75, 2.5, 3.5, 5, 7, 10, 15] },
      earth: { name: 'Earth', glyph: '🌍', color: '#3fd17f', pays: [0.9, 1.2, 1.5, 1.75, 2.5, 3.5, 5, 7, 10, 15, 25] },
      saturn: { name: 'Ringed Planet', glyph: '🪐', color: '#ffb35c', pays: [1.2, 1.5, 1.75, 2.5, 3.5, 5, 7, 10, 15, 25, 35] },
      ufo: { name: 'UFO', glyph: '🛸', color: '#9fb4ff', pays: [1.75, 2.5, 3.5, 5, 7, 10, 15, 20, 25, 35, 60] },
      rocket: { name: 'Rocket', glyph: '🚀', color: '#ff5277', pays: [3.5, 5, 7, 10, 15, 20, 25, 35, 50, 75, 125] },
      alien: { name: 'Alien', glyph: '👽', color: '#7dff8a', pays: [7, 10, 15, 20, 25, 35, 50, 75, 125, 175, 250] },
      wild: { kind: 'wild', name: 'Star Wild', glyph: 'WILD', text: true, color: '#b18cff' },
      scatter: { kind: 'scatter', name: 'Wormhole', glyph: '🌀', color: '#5ad7ff' },
      coin: JACKPOT_SYMBOL
    },
    weights: {
      moon: 20, comet: 19, earth: 17, saturn: 15, ufo: 12, rocket: 9, alien: 6, wild: 2.5
    },
    fsWeights: {
      moon: 20, comet: 19, earth: 17, saturn: 15, ufo: 12, rocket: 9, alien: 6, wild: 7
    },
    scatter: {
      chance: 0.068,
      pays: { 3: 2, 4: 5, 5: 20, 6: 100 },
      label: '3+ Wormholes anywhere'
    },
    freeSpins: {
      award: { 3: 10, 4: 12, 5: 15, 6: 20 },
      retrigger: { 3: 5, 4: 8, 5: 10, 6: 15 },
      title: 'MULTIPLIER NEVER RESETS'
    },
    jackpot: {
      tiers: [
        { id: 'mini', name: 'MINI', odds: 500, seed: 10 },
        { id: 'minor', name: 'MINOR', odds: 2000, seed: 40 },
        { id: 'major', name: 'MAJOR', odds: 10000, seed: 200 },
        { id: 'grand', name: 'GRAND', odds: 100000, seed: 3000 }
      ],
      hitCoins: [[3, 60], [4, 25], [5, 10], [6, 5]],
      teaseCoins: [[0, 84], [1, 11], [2, 5]],
      game: 'rocket'
    },
    maxWin: 15000,
    rtp: { base: 0.8616 }
  };

  const list = [neon, dragon, cosmic];
  const byId = {};
  list.forEach(function (m) { byId[m.id] = m; });

  return { list: list, byId: byId };
}));
