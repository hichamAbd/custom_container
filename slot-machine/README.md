# Reel Riot Casino

A flashy, phone-first slot machine game with three machines, progressive
jackpots and a settings popup (the "Control Room") where you can make jackpots
rarer or more frequent and set your coin balance by hand.

It is plain HTML, CSS and JavaScript with no build step and no dependencies.
Coins are for fun only and have no cash value.

## Play it on your phone

You need Python 3 on your computer (macOS and Linux have it; on Windows get it
from python.org).

1. Open a terminal in this `slot-machine` folder.
2. Run `python3 serve.py` (on Windows: `py serve.py`).
3. Make sure your phone is on the same Wi-Fi as the computer.
4. Scan the QR code the terminal prints with your phone camera, or type the
   `http://192.168.x.x:8080` address it shows into your phone's browser.

The first time, your computer may ask whether Python can accept network
connections: allow it on private networks. Press `Ctrl+C` to stop the server.

For a full-screen, app-like experience, use your browser's **Add to Home
Screen** option once the game is open.

On a computer you can simply open `http://localhost:8080` after starting the
server. Space bar spins.

## The three machines

| Machine | Grid | How it pays | Signature features | Risk |
| --- | --- | --- | --- | --- |
| Neon Fruit Frenzy | 5 × 3 | 20 paylines | Wilds on reels 2-4, free spins where every win pays ×3 | Low |
| Dragon's Hoard | 5 × 4 | 1,024 ways | Dragon Fire turns random symbols into wilds; free spins with sticky wilds | Medium |
| Cosmic Cascade | 6 × 5 | Clusters of 5+ touching symbols | Winning clusters explode and new symbols fall in; every cascade raises the multiplier (×1 up to ×20), and in free spins the multiplier never resets | High |

Every machine has:

- **Wilds** that stand in for normal symbols.
- **Bonus symbols** (scatters): 3 or more anywhere pay and start free spins,
  which can retrigger.
- **Jackpot coins**: 3 or more spin the **Jackpot Wheel** for the Mini, Minor,
  Major or Grand jackpot. When 2 bonus symbols or 2 coins have landed, the
  remaining reels slow down with a glowing frame for suspense.
- **Progressive jackpots** that grow a little with every spin and scale with
  your bet (the Grand is 1,000 to 3,000 times the bet).
- **Big win celebrations**: Big (10× bet), Mega (25×), Epic (50×) and
  Legendary (100×), with a counter that climbs through the tiers, coin showers
  and fireworks.
- Autoplay (10, 25, 50, 100 or until stopped), turbo spins, tap-to-stop reels,
  a paytable that shows real coin amounts for your current bet, synthesized
  sound effects and music, and vibration on Android phones.

The lobby also has a free coin wheel every 10 minutes, and an "out of coins"
top-up so the fun never stops.

## The Control Room (settings popup)

Tap the sliders icon at the top right.

- **Jackpot frequency** slider, from ×0.02 (nearly impossible) through ×1
  (normal) to ×50 (jackpot party). A table shows the live chance of each
  jackpot per spin, what it is worth right now, and the long-run return of the
  machine at that setting.
- **Coins**: type any amount, drag the slider (0 to 1 trillion, logarithmic),
  or use the quick buttons (+1M, +100M, +10B, reset to 10M).
- Sound effects, music, vibration and turbo switches.
- Your stats (spins, wagered, won, your actual return, biggest win, jackpots
  hit) and a reset.

Progress is saved in the browser, including a free spins round in progress if
you close the page mid-round.

## Balancing

Each machine is tuned so the house keeps a small edge at the normal jackpot
setting, small wins come often, and jackpots are rare. Every number below
comes from 2,000,000 simulated spins per machine using the same engine code
the game runs (`node tools/simulate.js`).

| | Neon Fruit Frenzy | Dragon's Hoard | Cosmic Cascade |
| --- | --- | --- | --- |
| Long-run return | 96.4% | 96.3% | 96.3% |
| Spins that pay something | 39% | 58% | 32% |
| Free spins | 1 in 136 spins, avg 24× bet | 1 in 158, avg 34× | 1 in 188, avg 52× |
| Big wins (10×+) | 1 in 80 spins | 1 in 72 | 1 in 85 |

Jackpot odds per spin at the normal (×1) setting, with the starting prize as a
multiple of the bet:

| Jackpot | Neon Fruit Frenzy | Dragon's Hoard | Cosmic Cascade |
| --- | --- | --- | --- |
| Mini | 1 in 400 (8×) | 1 in 500 (10×) | 1 in 500 (10×) |
| Minor | 1 in 1,500 (30×) | 1 in 2,000 (40×) | 1 in 2,000 (40×) |
| Major | 1 in 6,000 (150×) | 1 in 8,000 (180×) | 1 in 10,000 (200×) |
| Grand | 1 in 50,000 (1,000×) | 1 in 75,000 (2,000×) | 1 in 100,000 (3,000×) |

How it works:

- Each reel draws symbols from its own weighted table, and wilds only appear
  where the design allows them. Bonus symbols are placed at most once per reel.
- The jackpot is decided by its own roll each paid spin, then the coins are
  placed on the reels to match. That keeps the jackpot odds exact, so the
  Control Room slider simply multiplies them. Spins that miss sometimes still
  show one or two coins as a tease.
- About 15% extra on top of each jackpot's base return feeds its progressive
  growth, which resets to the starting value when it is won. Prizes are capped
  at three times the starting value.
- A single spin (or a whole free spins round) is capped at 5,000× the bet on
  Neon, 10,000× on Dragon and 15,000× on Cosmic.
- Random numbers come from the browser's cryptographic generator, which seeds
  a fast sfc32 generator.

To retune, edit the weights or paytables in `js/machines.js`, then run:

```sh
node tools/simulate.js              # 1,000,000 spins per machine
node tools/simulate.js 5000000 neon # more spins, one machine
node tools/simulate.js --factor 5   # with jackpots 5x more frequent
node tools/simulate.js --update     # store the measured return for the settings popup
```

## Files

```
index.html            page markup
css/style.css         all styling and the three themes
js/engine.js          math engine: RNG, spins, lines/ways/clusters, cascades, jackpots
js/machines.js        the three machines: symbols, weights, paytables, features
js/app.js             game controller: reels, animations, features, settings, saving
js/audio.js           synthesized sound effects and music (Web Audio, no files)
js/fx.js              particle effects (coins, sparks, confetti, fireworks)
tools/simulate.js     balance simulator
serve.py              local network server with a QR code for your phone
manifest.webmanifest  home screen install info
icons/                app icons
```

Tip for tinkering: in the browser console, `ReelRiot.force('grand')` makes the
next spin hit the Grand jackpot (also `fs`, `jackpot`, `big`, `mega`, `epic`,
`legendary`, `fire`, `cascade`).
