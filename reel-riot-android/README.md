# Reel Riot Vegas (native Android app)

A native Android version of Reel Riot in the "Vegas Gold" style, built
around the concept board: real-time 3D, animated hosts, cutscenes and a
bonus game per machine. Coins are for fun only and have no cash value. The
web version in `../slot-machine` is separate and unchanged.

## Install it on your phone

1. On the phone, open this link in Chrome and download the app:
   **https://github.com/hichamAbd/custom_container/raw/claude/adoring-dirac-wqznpk/reel-riot-android/release/ReelRiotVegas.apk**
2. Open the downloaded file. Android asks to allow installing apps from this
   source (Chrome or Files): allow it, go back, and tap **Install**.
3. Open **Reel Riot Vegas** from your app drawer.

Later versions install over this one and keep your coins, because every
build is signed with the same development key (`android/reelriot-dev.keystore`).
That key is only for installing on your own phone, not for a store.

Needs Android 7 or newer with OpenGL ES 3 (any phone from the last several
years; tested in development against a Galaxy Note 8 profile, Android 9).

## What's inside

- **Opening cutscene**: the curtains part, Vivi Velvet rises on the stage
  lift and welcomes you, then Mochi, Captain Goldbeak and Bruno rise into
  spotlights one by one, ending on the title with confetti and gold coins.
  Tap to skip. Replay it from the Control Room.
- **Lobby**: a 3D turntable of machine cabinets on the casino stage, each
  with its host on top. Swipe or use the arrows, tap PLAY. Vivi gives tips.
- **Three machines** (5 reels, 3 rows, 20 lines), each a 3D cabinet with real
  spinning reel drums, a marquee, blinking bulbs and its host on top who
  talks and reacts (waves, cheers, leans in when the bonus is close):
  - **Lucky Paws Palace** with Mochi: 3 Lucky Coins start **Lucky Drop**, a
    3D peg board where Mochi drops a coin that bounces into a jackpot
    bucket (every bucket is a jackpot).
  - **Goldbeak's Cove** with Captain Goldbeak: 3 Treasure Chests start the
    **Treasure Hunt**: pick one of three 3D chests; the others reveal what
    they held.
  - **High Roller Lounge** with Bruno: 3 Wheel symbols spin the **Mega
    Wheel**, three rings where UPGRADE moves the spin inward to bigger
    prizes (Major and Grand in the center).
- **Big wins**: Big, Mega, Epic and Legendary climb as the counter rises,
  with light rays, 3D coins bursting out of the cabinet, coin rain and
  confetti. Jackpots get their own title.
- **Rewards**: a 7-day daily bonus presented by Vivi (300K up to 4M on
  day 7), free coins every hour, XP and levels with a coin reward per
  level, rescue coins when you run out.
- **Player care**: a break reminder every 45 minutes with your session's
  spins and coin change (switch it off in the Control Room).
- **Control Room** (Vivi's portrait in a machine, CONTROL in the lobby):
  add or reset coins, make bonus games ×0.25 to ×20 as frequent, force the
  next spin (bonus, big, mega, epic, near miss, loss, any win), change level
  and XP, make free coins or the daily bonus ready, start a new day, sound,
  music, vibration, break reminders, replay the intro.

Each machine returns about 95 to 97% over millions of spins, with a bonus
game about 1 spin in 123 (`--sim` below measures it).

## How it's made

- [libGDX](https://libgdx.com) 1.12.1 (Java), OpenGL ES 3.
- All 3D models are built in code from shaded primitives: the hosts are
  figurines with animated parts (`world/Hosts.java`), the stage, cabinets,
  reel drums, peg board, wheel and chests too.
- One custom shader (`gfx/ToyShader.java`) lights everything per pixel with
  a key, a fill and two colored lights, and reflects an imagined casino
  studio so gold, lacquer and velvet each look like themselves.
- Reel drums carry 20 symbols around their rim on a texture that is
  repainted at the start of each spin, with the result on the far side,
  so the reels really turn to it.
- Symbol art comes from the web game's painted art (rendered to PNG); sound
  effects and the lounge music loop are synthesized (`assets/sfx`).
- Fonts: Lilita One, Cinzel Decorative and Barlow Condensed (SIL Open Font
  License, in `assets/fonts`).

## Build

Needs JDK 17+, Gradle 8.9+ and the Android SDK (platform 34, build tools 34).
Put `sdk.dir=/path/to/android-sdk` in `local.properties`.

```sh
gradle :android:assembleRelease          # APK in android/build/outputs/apk/release/
gradle :desktop:dist                     # desktop build for testing
java -jar desktop/build/libs/reel-riot-desktop.jar                   # play on a computer
java -cp desktop/build/libs/reel-riot-desktop.jar com.reelriot.vegas.desktop.DesktopLauncher --sim 2000000
```

Automated screenshots (used during development, works under `xvfb-run`):

```sh
java -jar desktop/build/libs/reel-riot-desktop.jar --shot machine:mochi 540x1110 "7:/tmp/a.png,12:/tmp/b.png" bonus spin tap:7
```

`--shot SCENE WxH TIME:FILE,...` plays a scene on a fixed clock and saves
screenshots; extra words force the next spin (`bonus`, `epic`, ...), `spin`
spins on entry, `tap:SECONDS` taps the screen, and `daily`, `control`,
`care`, `finale` open those screens. Scenes: `intro`, `lobby`,
`machine:mochi`, `machine:goldbeak`, `machine:bruno`, `lineup`.

## Files

```
core/        game code shared by phone and desktop
  ReelRiotGame.java   app, scene switching, save, sound
  gfx/                shader, materials, meshes, figures, 3D text
  world/              hosts, casino stage, 3D coins, backdrops
  slots/              machines, spin engine, simulator, 3D reels
  scenes/             intro, lobby, machine, bonus games, big win, Control Room, daily bonus, break reminder
  ui/                 2D layer: fonts, gold lettering, buttons, HUD, particles
android/     Android launcher, manifest, icon, signing key
desktop/     desktop launcher, screenshot and simulation modes
assets/      fonts, symbols, host portraits, sounds
release/     the installable APK
```
