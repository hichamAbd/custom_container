package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.Save;
import com.reelriot.vegas.Scene;
import com.reelriot.vegas.gfx.Figure;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Part;
import com.reelriot.vegas.gfx.TextTex;
import com.reelriot.vegas.gfx.ToyShader;
import com.reelriot.vegas.slots.Machine;
import com.reelriot.vegas.slots.Reels3D;
import com.reelriot.vegas.slots.SlotEngine;
import com.reelriot.vegas.ui.Button;
import com.reelriot.vegas.ui.Hud;
import com.reelriot.vegas.ui.Ui;
import com.reelriot.vegas.world.CasinoSet;
import com.reelriot.vegas.world.Coins3D;
import com.reelriot.vegas.world.Hosts;

/** A playable machine: a 3D cabinet with real reel drums and its host on top. */
public class MachineScene extends Scene {
    static final long CARE_EVERY = 45 * 60 * 1000L;
    public static final long[] BETS = { 50_000, 100_000, 250_000, 500_000, 1_000_000, 2_500_000, 5_000_000, 10_000_000 };

    final Machine m;
    final SlotEngine engine;
    final Reels3D reels;
    final Figure host;
    private final CasinoSet set;
    private final Part cabinet = Part.group();
    private final Array<Part> bulbs = new Array<>();
    private final TextTex marquee;
    final Coins3D coins = new Coins3D(70);
    private final Array<Button> buttons = new Array<>();
    private final Button spinBtn, autoBtn;
    private int betIdx = 1;
    private SlotEngine.Result res;
    private boolean busy, auto, anticipating, spinOnEnter;
    private float idleT, winT, lineCycleT;
    private int lineShown;
    private long shownCoins, lastWin, winShown;
    Overlay overlay;
    private String force;
    private String say;
    private float sayT;
    private String toast;
    private float toastT;
    private final Vector3 tmpV = new Vector3();
    private final Color C = new Color();
    private float camBaseY, camLook;

    public MachineScene(ReelRiotGame game, String id) {
        super(game);
        m = Machine.byId(id);
        engine = new SlotEngine(m, System.nanoTime());
        reels = new Reels3D(engine);
        set = new CasinoSet(game.ui.glow).place(0, -3.05f, -7.5f);
        host = Hosts.make(m.host);
        host.where.set(-2.25f, 3.95f, 0.15f);
        host.scale = 1.05f;
        host.facing = 12;
        marquee = new TextTex(game.ui, m.name, Ui.TITLE, 1024, 220, new Color(1f, 0.88f, 0.45f, 1), new Color(0.35f, 0.12f, 0f, 1));
        buildCabinet();
        shownCoins = game.save.coins;
        if (game.driver != null) {
            force = game.driver.forced();
            spinOnEnter = game.driver.hasAction("spin");
        }

        float bottom = 120;
        spinBtn = add(new Button("SPIN", Button.Style.SPIN, Ui.W / 2 - 130, bottom, 260, 260, this::spinPressed));
        spinBtn.size = 70;
        add(new Button("−", Button.Style.DARK, 40, bottom + 70, 96, 96, () -> changeBet(-1))).size = 60;
        add(new Button("+", Button.Style.DARK, 264, bottom + 70, 96, 96, () -> changeBet(1))).size = 60;
        autoBtn = add(new Button("AUTO", Button.Style.DARK, Ui.W - 300, bottom + 70, 230, 96, null));
        autoBtn.onClick = this::toggleAuto;
        autoBtn.size = 46;
        add(new Button("‹", Button.Style.ICON, 30, 0, 96, 96, () -> back())).size = 70;
        Button cr = add(new Button(null, Button.Style.ICON, Ui.W - 126, 0, 96, 96, () -> overlay = new ControlRoom(game, this)));
        cr.icon = "hosts/vivi.png";
        layoutTop();
    }

    private void toggleAuto() {
        auto = !auto;
        autoBtn.style = auto ? Button.Style.GREEN : Button.Style.DARK;
        if (auto && !busy) spinPressed();
    }

    private Button add(Button b) {
        buttons.add(b);
        return b;
    }

    private void layoutTop() {
        for (Button b : buttons) if (b.style == Button.Style.ICON) b.y = game.ui.H - 130;
    }

    /* ------------------------------------------------------------ cabinet */

    private void buildCabinet() {
        float hw = 2.62f, hh = 1.52f;
        com.badlogic.gdx.graphics.g3d.Material body = Mat.lacquer(m.body);
        com.badlogic.gdx.graphics.g3d.Material gold = Mat.gold();
        // the face of the cabinet around the reel window
        cabinet.add(Part.box(body).at(0, hh + 1.15f, -0.2f).sized(7.6f, 2.3f, 0.4f));
        cabinet.add(Part.box(body).at(0, -hh - 0.75f, -0.2f).sized(7.6f, 1.5f, 0.4f));
        for (int s = -1; s <= 1; s += 2) cabinet.add(Part.box(body).at(s * 3.31f, 0, -0.2f).sized(1.38f, 2 * hh + 0.05f, 0.4f));
        // gold frame around the window, dividers between reels
        cabinet.add(Part.box(gold).at(0, hh + 0.09f, 0.05f).sized(2 * hw + 0.36f, 0.18f, 0.2f));
        cabinet.add(Part.box(gold).at(0, -hh - 0.09f, 0.05f).sized(2 * hw + 0.36f, 0.18f, 0.2f));
        for (int s = -1; s <= 1; s += 2) cabinet.add(Part.box(gold).at(s * (hw + 0.09f), 0, 0.05f).sized(0.18f, 2 * hh + 0.36f, 0.2f));
        for (int i = 0; i < 4; i++) cabinet.add(Part.box(Mat.darkGold()).at(Reels3D.x(i) + (Reels3D.C + Reels3D.GAP) / 2, 0, 0.02f).sized(0.05f, 2 * hh, 0.06f));
        // marquee with the machine's name, bulbs all around
        cabinet.add(Part.box(gold).at(0, hh + 1.32f, -0.02f).sized(6.6f, 1.62f, 0.16f));
        cabinet.add(Part.box(Mat.velvet(new Color(m.body).mul(0.35f))).at(0, hh + 1.32f, 0.16f).sized(6.3f, 1.36f, 0.04f));
        cabinet.add(Part.quad(Mat.decal(marquee.texture, true)).at(0, hh + 1.32f, 0.26f).sized(6.1f, 1.31f, 1));
        cabinet.add(Part.quad(Mat.glowSprite(game.ui.glow, m.glow, 0.16f)).at(0, hh + 1.32f, 0.3f).sized(6.5f, 2.2f, 1));
        for (int i = 0; i < 22; i++) {
            float u = i / 21f;
            Part b = cabinet.add(Part.ball(Mat.lit(i % 2 == 0 ? new Color(1f, 0.85f, 0.5f, 1) : m.accent, 1.4f)).at(-3.3f + u * 6.6f, hh + 2.2f, 0.14f).sized(0.13f));
            bulbs.add(b);
        }
        for (int i = 0; i < 9; i++) {
            for (int s = -1; s <= 1; s += 2) {
                Part b = cabinet.add(Part.ball(Mat.lit(i % 2 == 0 ? new Color(1f, 0.85f, 0.5f, 1) : m.accent, 1.4f)).at(s * 3.55f, -hh + i * (2 * hh / 8f), 0.12f).sized(0.12f));
                bulbs.add(b);
            }
        }
        // control deck with a gold rail
        cabinet.add(Part.box(Mat.lacquer(new Color(m.body).mul(0.6f))).at(0, -hh - 1.6f, 0.4f).sized(7.6f, 0.4f, 1.2f));
        cabinet.add(Part.box(gold).at(0, -hh - 1.38f, 1.0f).sized(7.6f, 0.08f, 0.08f));
        // the host's pedestal
        cabinet.add(Part.cyl(gold).at(-2.25f, hh + 2.32f, 0.15f).sized(1.4f, 0.18f, 1.2f));
        cabinet.add(Part.box(Mat.lacquer(new Color(0.08f, 0.01f, 0.03f, 1))).at(0, 0, -6.6f).sized(7.4f, 4.0f, 0.2f));
        // glass
        cabinet.add(Part.quad(Mat.glowSprite(game.ui.glow, new Color(1f, 0.9f, 0.8f, 1), 0.07f)).at(-1.2f, 0.7f, 0.11f).sized(4.4f, 2.6f, 1));
    }

    private void fitCamera() {
        cam.fieldOfView = 50;
        float aspect = cam.viewportWidth / cam.viewportHeight;
        float t = MathUtils.tanDeg(cam.fieldOfView / 2);
        float d = 4.0f / (t * Math.min(aspect, 1.4f));
        // reels sit a little below the middle of the screen, the host above them
        camLook = 0.9f + (aspect < 0.6f ? 0.6f : 0.2f);
        camBaseY = camLook + 0.6f;
        cam.position.set(0, camBaseY, d);
        cam.lookAt(0, camLook, 0);
        cam.near = 1.0f;
        cam.far = 70f;
        cam.update();
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        fitCamera();
        layoutTop();
    }

    @Override
    public void enter() {
        lightUp();
        speak(hello(), 2.6f);
        host.react(Figure.Mood.WAVE, 2.2f);
        game.sfx.play("sparkle");
        if (spinOnEnter) spinPressed();
        if (game.driver != null && game.driver.hasAction("care")) overlay = new Care(game);
    }

    void lightUp() {
        ToyShader.Lighting L = ToyShader.LIGHT;
        L.keyDir.set(-0.35f, -0.7f, -0.62f).nor();
        L.key.set(1f, 0.95f, 0.88f, 1);
        L.fill.set(m.glow.r * 0.45f, m.glow.g * 0.3f, m.glow.b * 0.45f, 1);
        L.ambient.set(0.2f, 0.13f, 0.15f, 1);
        L.pointPos[0].set(2.8f, 4.5f, 3);
        L.point[0].set(m.glow.r * 1.2f, m.glow.g * 0.9f, m.glow.b * 0.8f, 1);
        L.pointPos[1].set(-3f, 1f, 3.5f);
        L.point[1].set(0.9f, 0.5f, 0.3f, 1);
        L.envTint.set(1, 1, 1, 1);
        L.fogNear = 14;
        L.fogFar = 40;
    }

    /* --------------------------------------------------------------- talk */

    String hello() {
        switch (m.host) {
            case "goldbeak": return "Ahoy! Treasure ahead!";
            case "bruno": return "Welcome to the Lounge, pal.";
            default: return "Nya! Welcome to my palace!";
        }
    }

    String line(String what) {
        boolean g = m.host.equals("goldbeak"), b = m.host.equals("bruno");
        switch (what) {
            case "win": return g ? "Shiny! Squawk!" : b ? "Nice roll." : "Lucky paws!";
            case "big": return g ? "Pieces of eight!" : b ? "Now we're talkin'!" : "So much gold, nya!";
            case "tease": return g ? "Land ho... almost!" : b ? "Easy... easy..." : "Come on, Lucky Coin...";
            case "bonus": return g ? "Treasure hunt!" : b ? "Spin the big wheel!" : "Lucky Drop time!";
            case "rescue": return g ? "Here, have some doubloons!" : b ? "On the house, pal." : "Nya! A gift for you!";
            default: return g ? "Arr, steady now." : b ? "House luck. Again?" : "Nya... next one!";
        }
    }

    void speak(String text, float seconds) {
        say = text;
        sayT = seconds;
    }

    void toast(String text) {
        toast = text;
        toastT = 2.6f;
    }

    /* --------------------------------------------------------------- play */

    long bet() { return BETS[betIdx]; }

    private void changeBet(int d) {
        if (busy) return;
        betIdx = MathUtils.clamp(betIdx + d, 0, BETS.length - 1);
        game.sfx.play("click", 0.8f, 1 + betIdx * 0.04f);
    }

    void forceNext(String what) { force = what; }

    private void spinPressed() {
        if (overlay != null) return;
        if (busy) {
            reels.slam();
            return;
        }
        Save s = game.save;
        if (s.coins < bet()) {
            if (s.coins < BETS[0]) {
                s.coins += 1_000_000;
                toast("+1,000,000 rescue coins");
                speak(line("rescue"), 2.5f);
                host.react(Figure.Mood.CHEER, 1.5f);
                game.ui.fx.coins(Ui.W / 2, game.ui.H * 0.55f, 26, 1500);
                game.sfx.play("coin");
            } else {
                while (betIdx > 0 && BETS[betIdx] > s.coins) betIdx--;
                toast("Bet lowered to " + Ui.shortFmt(bet()));
            }
            auto = false;
            autoBtn.style = Button.Style.DARK;
            return;
        }
        s.coins -= bet();
        s.totalBet += bet();
        s.spins++;
        int levels = s.addXp(Math.max(1, Math.round(10 * Math.sqrt(bet() / 1000.0))));
        if (levels > 0) {
            toast("Level " + s.level + "!  +" + Ui.shortFmt(Save.levelReward(s.level)) + " coins");
            game.sfx.play("jackpot", 0.7f, 1.1f);
            game.ui.fx.confetti(Ui.W / 2, game.ui.H * 0.75f, 60, 1400);
        }
        res = force != null ? engine.forced(bet(), force) : engine.spin(bet(), s.bonusBoost);
        force = null;
        anticipating = engine.anticipation(res);
        reels.spin(res, false, anticipating);
        busy = true;
        lastWin = 0;
        winShown = 0;
        lineShown = -1;
        game.sfx.play("spin", 0.7f, 1);
        game.sfx.buzz(15);
        host.set(Figure.Mood.IDLE);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        set.update(dt);
        host.update(dt);
        coins.update(dt);
        reels.update(dt, this::reelStopped);
        cabinet.reset();
        for (int i = 0; i < bulbs.size; i++) {
            float k = 0.5f + 0.5f * MathUtils.sin(time * (busy ? 14 : 5) + i * 0.8f);
            Mat.emissive(bulbs.get(i).inst, 0.5f + 1.5f * k, (i % 2 == 0 ? 0.45f : m.accent.g * 0.5f) + 1f * k, 0.2f + 0.6f * k);
        }
        cabinet.update(new com.badlogic.gdx.math.Matrix4());
        // a gentle camera drift, and a lean in while the last reel teases
        float lean = anticipating && reels.reelSpinning(4) ? 1 : 0;
        cam.position.y = camBaseY + MathUtils.sin(time * 0.5f) * 0.05f;
        cam.position.x = MathUtils.sin(time * 0.33f) * 0.12f;
        cam.position.z += ((cam.position.z - lean * 0.02f) - cam.position.z);
        cam.lookAt(0, camLook, 0);
        cam.update();
        sayT -= dt;
        toastT -= dt;
        // the balance counts toward the real number
        long target = game.save.coins;
        if (shownCoins != target) {
            long diff = target - shownCoins;
            long step = Math.max(1, Math.abs(diff) / 8);
            shownCoins += Math.signum(diff) * Math.min(Math.abs(diff), step);
        }
        if (winShown < lastWin) winShown = Math.min(lastWin, winShown + Math.max(1, lastWin / 30));
        if (overlay != null) {
            overlay.update(dt);
            if (overlay.finished()) {
                Overlay done = overlay;
                overlay = null;
                afterOverlay(done);
            }
        }
        if (!busy && res != null && res.lines.size > 0 && overlay == null) {
            lineCycleT -= dt;
            if (lineCycleT <= 0) {
                lineShown = (lineShown + 1) % res.lines.size;
                lineCycleT = 1.4f;
            }
        }
        if (!busy && overlay == null && game.save.care && System.currentTimeMillis() - game.save.lastCare > CARE_EVERY) {
            game.save.lastCare = System.currentTimeMillis();
            auto = false;
            autoBtn.style = Button.Style.DARK;
            overlay = new Care(game);
        }
        if (!busy && overlay == null) {
            idleT += dt;
            if (auto && idleT > 0.7f) spinPressed();
        }
        if (winT > 0) winT -= dt;
    }

    private void reelStopped(int reel) {
        game.sfx.play("stop", 0.8f, 0.95f + reel * 0.03f);
        int bonusIdx = m.index("bonus");
        for (int y = 0; y < Machine.ROWS; y++) {
            if (res.grid[reel][y] == bonusIdx) {
                Vector3 p = project(reel, y);
                game.ui.fx.burst(p.x, p.y, 18, 420, m.glow);
                game.sfx.play("sparkle", 0.8f, 1 + reel * 0.05f);
            }
        }
        if (anticipating && reel == 2) {
            speak(line("tease"), 2.2f);
            host.set(Figure.Mood.WOW);
            game.sfx.play("upgrade", 0.5f, 0.8f);
        }
        if (reel == Machine.REELS - 1) finishSpin();
    }

    private void finishSpin() {
        anticipating = false;
        host.set(Figure.Mood.IDLE);
        Save s = game.save;
        if (res.lineWin > 0) {
            s.coins += res.lineWin;
            s.totalWon += res.lineWin;
            s.biggest = Math.max(s.biggest, res.lineWin);
            lastWin = res.lineWin;
            lineCycleT = 0;
            winT = 1.2f;
            float x = res.lineWin / (float) bet();
            if (x >= 10) {
                overlay = new BigWin(game, this, res.lineWin, bet(), null);
            } else {
                game.sfx.play("win", 0.8f, 1);
                game.ui.fx.coins(Ui.W / 2, 470, Math.min(30, 6 + (int) (x * 4)), 1300);
                speak(line("win"), 1.6f);
                host.react(Figure.Mood.CHEER, 1.4f);
            }
        } else if (!res.bonus && MathUtils.randomBoolean(0.12f)) {
            speak(line("sad"), 1.6f);
            host.react(Figure.Mood.SAD, 1.6f);
        }
        if (res.bonus && overlay == null) startBonus();
        else if (!res.bonus) endSpin();
        s.flush();
    }

    private void startBonus() {
        speak(line("bonus"), 2.4f);
        host.react(Figure.Mood.CHEER, 2.5f);
        game.sfx.play("jackpot");
        game.sfx.buzz(60);
        game.ui.fx.confetti(Ui.W / 2, game.ui.H * 0.5f, 80, 1600);
        switch (m.bonus) {
            case MEGA_WHEEL: overlay = new MegaWheel(game, this, res.prize); break;
            case TREASURE: overlay = new Treasure(game, this, res.prize); break;
            default: overlay = new LuckyDrop(game, this, res.prize); break;
        }
    }

    private void afterOverlay(Overlay done) {
        lightUp();
        if (done instanceof BigWin) {
            if (res != null && res.bonus && !((BigWin) done).bonusResult) {
                startBonus();
                return;
            }
            endSpin();
            return;
        }
        if (done instanceof LuckyDrop || done instanceof MegaWheel || done instanceof Treasure) {
            Save s = game.save;
            s.coins += res.bonusWin;
            s.totalWon += res.bonusWin;
            s.biggest = Math.max(s.biggest, res.bonusWin);
            lastWin += res.bonusWin;
            String title = res.prize.jackpot >= 0 ? Machine.JACKPOT_NAMES[res.prize.jackpot] + " JACKPOT" : m.bonusName.toUpperCase() + " WIN";
            BigWin bw = new BigWin(game, this, res.bonusWin, bet(), title);
            bw.bonusResult = true;
            overlay = bw;
            s.flush();
            return;
        }
        if (done instanceof ControlRoom) {
            if (!busy) endSpin();
        }
    }

    private void endSpin() {
        busy = false;
        idleT = 0;
    }

    Vector3 project(int reel, int row) {
        reels.cellPos(reel, row, tmpV);
        cam.project(tmpV);
        tmpV.x /= game.ui.px;
        tmpV.y /= game.ui.px;
        return tmpV;
    }

    /* -------------------------------------------------------------- draw */

    @Override
    public void render3d(ModelBatch batch) {
        PerspectiveCamera oc = overlay != null ? overlay.cam() : null;
        if (oc != null) {
            // a bonus game takes over the whole screen with its own world
            batch.end();
            batch.begin(oc);
            overlay.render3d(batch);
            return;
        }
        set.render(batch);
        reels.render(batch);
        cabinet.render(batch);
        host.render(batch);
        coins.render(batch);
        if (overlay != null) overlay.render3d(batch);
    }

    @Override
    public void renderUi() {
        Ui ui = game.ui;
        float H = ui.H;
        if (overlay == null || !(overlay instanceof Fullscreen)) {
            drawWins(ui);
            Hud.coinBar(ui, shownCoins, game.save, H - 132);
            Hud.plaques(ui, bet(), H - 170, time);
            // the host's speech bubble
            if (sayT > 0) {
                tmpV.set(host.where.x + 0.2f, host.where.y + 2.2f * host.scale, host.where.z);
                cam.project(tmpV);
                float hx = tmpV.x / ui.px, hy = tmpV.y / ui.px;
                Hud.bubble(ui, say, hx + 240, hy + 70, hx + 40, hy, Math.min(1, sayT * 3));
            }
            drawDeck(ui);
            for (Button b : buttons) b.draw(ui, time, 1 / 60f);
        }
        ui.fx.draw();
        if (overlay != null) overlay.renderUi();
        if (toastT > 0 && toast != null) {
            float a = Math.min(1, toastT * 3);
            ui.panel(Ui.W / 2 - 380, H * 0.62f - 60, 760, 120, C.set(0.08f, 0.01f, 0.04f, 0.9f * a));
            ui.text(Ui.DISPLAY, 46, toast, Ui.W / 2, H * 0.62f, C.set(Ui.GOLD_HI).mul(1, 1, 1, a), Align.center);
        }
    }

    private void drawDeck(Ui ui) {
        float y = 420;
        ui.panel(40, y, Ui.W - 80, 112, C.set(0.08f, 0.01f, 0.04f, 0.88f));
        ui.panel(44, y + 4, Ui.W - 88, 104, C.set(m.body).mul(0.5f, 0.5f, 0.5f, 0.8f));
        ui.text(Ui.DISPLAY, 34, "WIN", 100, y + 56, C.set(Ui.GOLD), Align.left);
        if (winShown > 0) ui.gold(Ui.fmt(winShown), Ui.W / 2 + 30, y + 56, 64, Align.center, 1);
        else ui.text(Ui.BOLD, 38, busy ? "Good luck!" : m.bonusHint, Ui.W / 2 + 30, y + 54, C.set(Ui.CREAM).mul(1, 1, 1, 0.8f), Align.center);
        ui.text(Ui.BOLD, 30, "BET", 200, 330, Ui.GOLD, Align.center);
        ui.text(Ui.DISPLAY, 54, Ui.shortFmt(bet()), 200, 270, Color.WHITE, Align.center);
        spinBtn.label = busy ? "STOP" : "SPIN";
    }

    /** Glowing frames on winning cells and the payline through them. */
    private void drawWins(Ui ui) {
        if (busy || res == null || res.lines.size == 0 || lineShown < 0 || overlay != null) return;
        SlotEngine.LineWin w = res.lines.get(lineShown);
        int[] rows = Machine.PAYLINES[w.line];
        float pulse = 0.75f + 0.25f * MathUtils.sin(time * 9);
        float prevX = 0, prevY = 0;
        for (int r = 0; r < Machine.REELS; r++) {
            Vector3 p = project(r, rows[r]);
            float x = p.x, y = p.y;
            if (r > 0) {
                float dx = x - prevX, dy = y - prevY, len = (float) Math.sqrt(dx * dx + dy * dy);
                ui.batch.setColor(1f, 0.85f, 0.3f, 0.85f);
                ui.batch.draw(ui.white, prevX, prevY - 4, 0, 4, len, 8, 1, 1, MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees, 0, 0, 4, 4, false, false);
                ui.batch.setColor(Color.WHITE);
            }
            prevX = x;
            prevY = y;
        }
        for (int r = 0; r < w.count; r++) {
            Vector3 p = project(r, rows[r]);
            float s = 150;
            ui.glow(p.x, p.y, s * 0.9f, C.set(1f, 0.8f, 0.3f, 0.5f * pulse));
            ui.batch.setColor(1f, 0.85f, 0.35f, pulse);
            float t = 7;
            ui.batch.draw(ui.white, p.x - s / 2, p.y + s / 2 - t, s, t);
            ui.batch.draw(ui.white, p.x - s / 2, p.y - s / 2, s, t);
            ui.batch.draw(ui.white, p.x - s / 2, p.y - s / 2, t, s);
            ui.batch.draw(ui.white, p.x + s / 2 - t, p.y - s / 2, t, s);
            ui.batch.setColor(Color.WHITE);
        }
        Vector3 c = project(2, 2);
        ui.panel(Ui.W / 2 - 300, c.y - 150, 600, 80, C.set(0.08f, 0.01f, 0.04f, 0.85f));
        ui.text(Ui.BOLD, 40, (w.count) + " × " + m.syms[w.sym].name + "  pays " + Ui.fmt(w.amount), Ui.W / 2, c.y - 110, Ui.GOLD_HI, Align.center);
    }

    /* ------------------------------------------------------------- input */

    @Override
    public boolean touchDown(float x, float y) {
        if (overlay != null) return overlay.touchDown(x, y);
        if (x < 0) { spinPressed(); return true; }
        for (Button b : buttons) {
            if (b.hit(x, y)) {
                b.press();
                game.sfx.play("click");
                b.onClick.run();
                return true;
            }
        }
        // tapping the reels while they spin stops them early
        if (busy) { reels.slam(); return true; }
        return false;
    }

    @Override
    public boolean back() {
        if (overlay instanceof ControlRoom) { ((ControlRoom) overlay).close(); return true; }
        if (overlay != null) return true;
        if (busy) return true;
        game.save.flush();
        game.go("lobby");
        return true;
    }

    @Override
    public void dispose() {
        reels.dispose();
        marquee.dispose();
        game.save.flush();
    }

    /** Overlays that replace the whole HUD. */
    interface Fullscreen { }
}
