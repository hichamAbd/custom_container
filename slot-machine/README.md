# Reel Riot Casino

A flashy, phone-first slot machine game: three themed machines, each hosted by
an animated mascot, with its own jackpot bonus game, plus levels, a daily login
streak, daily missions and timed bonuses. A settings popup (the "Control Room")
lets you override everything: coins, jackpot odds, level and XP, the streak,
timers, missions, and even the result of the next spin.

It is plain HTML, CSS and JavaScript with no build step and no dependencies.
Coins are for fun only and have no cash value.

## Play it on your phone

The easiest way is the hosted version, which needs nothing installed:

**https://hichamabd.github.io/custom_container/slot-machine/**

Open it in your phone's browser, then add it to your home screen so it opens
full screen with its own icon, like an app:

- **iPhone (Safari):** tap the Share button, then **Add to Home Screen**.
- **Android (Chrome):** tap the ⋮ menu, then **Add to Home screen** (or
  **Install app**).

The hosted version is served by GitHub Pages. To turn it on (once), open the
repository's **Settings > Pages**, choose **Deploy from a branch**, pick the
branch with the game and the **/ (root)** folder, and save. Every later push
to that branch updates the game automatically.

On a computer you can also just double-click `index.html`. Space bar spins.

### Without internet hosting: your own Wi-Fi

If you'd rather not host it, your computer can serve it to your phone over
Wi-Fi. You need Python 3 (macOS and Linux have it; on Windows get it from
python.org).

1. Open a terminal in this `slot-machine` folder.
2. Run `python3 serve.py` (on Windows: `py serve.py`).
3. Make sure your phone is on the same Wi-Fi as the computer.
4. Scan the QR code the terminal prints with your phone camera, or type the
   `http://192.168.x.x:8080` address it shows into your phone's browser.

The first time, your computer may ask whether Python can accept network
connections: allow it on private networks. Press `Ctrl+C` to stop the server.

Each address keeps its own saved game, so the hosted version and the Wi-Fi
version have separate coins and progress.

## The three machines

| Machine | Host | Grid | How it pays | Signature feature | Jackpot game | Risk |
| --- | --- | --- | --- | --- | --- | --- |
| Neon Fruit Frenzy | DJ Pina, a pineapple in neon shades | 5 × 3 | 20 paylines | Free spins where every win pays ×3 | Jackpot Pick | Low |
| Dragon's Hoard | Ember, a crimson dragon | 5 × 4 | 1,024 ways | Ember breathes Dragon Fire to turn symbols wild; free spins with sticky wilds | Hold & Spin | Medium |
| Cosmic Cascade | Zib, an alien in a flying saucer | 6 × 5 | Clusters of 5+ touching symbols | Winning clusters explode and new symbols fall in; every cascade raises the multiplier (×1 up to ×20), and in free spins it never resets | Rocket Jackpot | High |

**Dragon's Hoard art.** This machine is the first with the richer, more
realistic style: its symbols are painted in code with metal and gem shading,
bevels, highlights and texture, Ember is a lit, scaled dragon, and Dragon
Fire is a real-time fire jet from Ember's mouth (WebGL; without it the
original fire sweep plays instead).

**Mascots.** Each host sits on top of its machine, idles (bobbing, blinking,
wings flapping, antennae wobbling) and reacts to what happens: it leans in when
the reels slow down for a possible bonus, hops on small wins, cheers on bigger
ones, sulks after a cold streak, and talks in speech bubbles. Ember actually
breathes the Dragon Fire onto the reels and Zib beams the cascades. The hosts
also star in the big win, free spins, jackpot and level-up scenes, and on the
lobby cards.

**Jackpot games.** Landing 3 or more jackpot coins starts the machine's game:

- **Jackpot Pick** (Neon): 12 tiles hide 3 of each jackpot. Tap to flip them;
  the first jackpot you find 3 times is yours, then the rest are revealed.
- **Hold & Spin** (Dragon): the coins lock in place and you get 3 respins.
  Every new coin resets the respins and carries a cash prize or a Mini, Minor
  or Major jackpot; filling all 20 spots wins the Grand.
- **Rocket Jackpot** (Cosmic): Zib's rocket counts down and blasts off past
  the Mini, Minor, Major and Grand rings. The highest ring it reaches is your
  jackpot, sometimes after a nail-biting sputter just below the next one.

**Free spins** open with a full-screen scene (a burst of light, the host
dancing, the spin count rolling up), change the machine's scenery while they
last (neon spotlights, rising embers, hyperspace streaks), and end with a
total-win scene. **Big wins** climb through Big (10× bet), Mega (25×), Epic
(50×) and Legendary (100×) with coin showers and fireworks.

## Ways to earn coins

The economy is deliberately tight, like an app-store slot. Every reward below
grows by 5% for each level above 1.

| Source | Reward at level 1 | How often |
| --- | --- | --- |
| Daily login streak | 300K, 500K, 750K, 1M, 1.5M, 2M, then 4M on day 7 | Once a day. Missing a day restarts at day 1; after day 7 it starts over |
| Free coins | 200K | Every hour |
| Bonus wheel | 250K to 10M (usually 250K to 1M) | Every 4 hours |
| Daily missions | Easy 200K + 150 XP, medium 400K + 300 XP, hard 750K + 500 XP | 3 new missions every day |
| Mission chest | 1.5M + 1,000 XP | When all 3 missions are claimed |
| Level-ups | 210K at level 2, 590K at level 5, 1.3M at level 10, 2.7M at level 20 | Every level |
| Rescue coins | 1M | When you can't afford the smallest bet, at most every 15 minutes |

Every paid spin earns XP: 10 × the square root of (bet ÷ 1,000), so a 100K
bet earns 100 XP. Level 2 needs 600 XP and each level needs a little more.
Levels never lock machines or bets; everything is open from the start.

Missions come from a pool, one easy, one medium and one hard each day: spin or
win a number of times, bet or win an amount, play a given machine, land
bonus symbols or jackpot coins, win 3 spins in a row, trigger Dragon Fire,
chain cascades, land a Big Win or trigger free spins.

## The Control Room (settings popup)

Tap the sliders icon at the top right. It has five tabs:

- **Jackpots**: the jackpot frequency slider, from ×0.02 (nearly impossible)
  through ×1 (normal) to ×50 (jackpot party), with a live table of each
  jackpot's chance per spin and current value, and the machine's long-run
  return at that setting.
- **Coins & XP**: type any coin amount, drag the coin slider (0 to 1 trillion)
  or use the quick buttons. Set your level, drag your XP toward the next level,
  add 1,000 XP, play a real level-up with its reward, or go back to level 1.
- **Bonuses**: choose which day of the login streak you collect today,
  simulate the next day, break the streak, make free coins, the bonus wheel or
  rescue coins ready now, switch on fast timers (1 to 2 minutes), jump the game
  clock 1 hour or 1 day ahead (or back to real time), complete any of today's
  missions, or roll new ones.
- **Test lab**: force the next spin to give free spins, any of the four
  jackpots, a Big, Mega, Epic or Legendary win, the machine's feature, or a
  near miss; and open the level-up, daily bonus, missions or out-of-coins
  screens on demand.
- **Options**: sound effects, music, vibration, turbo, your stats, and a full
  reset.

Progress is saved in the browser, including a free spins round in progress if
you close the page mid-round.

## Balancing

Each machine is tuned so the house keeps a small edge at the normal jackpot
setting, small wins come often, and jackpots are rare. Every number below
comes from simulating millions of spins with the same engine code the game
runs (`node tools/simulate.js`).

| | Neon Fruit Frenzy | Dragon's Hoard | Cosmic Cascade |
| --- | --- | --- | --- |
| Long-run return | 96.4% | 96.3% | 96.5% |
| Spins that pay something | 39% | 58% | 32% |
| Free spins | 1 in 135 spins, avg 24× bet | 1 in 158, avg 34× | 1 in 188, avg 52× |
| Machine feature | | Dragon Fire 1 in 44, avg 7.4× | about 0.38 cascades per spin |
| Big wins (10×+) | 1 in 80 spins | 1 in 74 | 1 in 86 |

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
- The jackpot games are shows built around that decided result: the pick'em
  board and the rocket always land on the jackpot already won. Hold & Spin is
  generated by the engine for the decided jackpot, including every cash coin,
  so its coins are part of the measured return (about 6.4× the bet per round).
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
node tools/simulate.js --update     # store the measured returns for the settings popup
```

## Files

```
index.html            page markup
css/style.css         core styling and the three themes
css/mascots.css       mascot moods and animations
css/features.css      jackpot games, free spins scenery, rewards and Control Room tabs
js/engine.js          math engine: RNG, spins, lines/ways/clusters, cascades, jackpots, Hold & Spin
js/machines.js        the three machines: symbols, weights, paytables, features
js/progress.js        levels and XP, login streak, missions, timed bonuses, test clock
js/mascots.js         the three hosts drawn as animated SVG
js/bonus.js           the jackpot games: Jackpot Pick, Hold & Spin, Rocket Jackpot
js/art.js             painted symbol art (Dragon's Hoard), drawn once with Canvas 2D
js/firefx.js          real-time fire jet (WebGL) for Dragon Fire
js/app.js             game controller: reels, animations, features, rewards, settings, saving
js/audio.js           synthesized sound effects and music (Web Audio, no files)
js/fx.js              particle effects (coins, sparks, confetti, fireworks)
tools/simulate.js     balance simulator
serve.py              local network server with a QR code for your phone
manifest.webmanifest  home screen install info
icons/                app icons
```

Tip for tinkering: in the browser console, `ReelRiot.force('grand')` makes the
next spin hit the Grand jackpot (also `fs`, `mini`, `minor`, `major`, `big`,
`mega`, `epic`, `legendary`, `feature`, `tease`). The Test lab tab does the
same from the Control Room.
