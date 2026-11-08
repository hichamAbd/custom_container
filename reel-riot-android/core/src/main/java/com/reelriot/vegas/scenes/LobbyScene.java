package com.reelriot.vegas.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
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
import com.reelriot.vegas.ui.Button;
import com.reelriot.vegas.ui.Hud;
import com.reelriot.vegas.ui.Ui;
import com.reelriot.vegas.world.CasinoSet;
import com.reelriot.vegas.world.Coins3D;
import com.reelriot.vegas.world.Hosts;

/** The lobby: a turntable of machine cabinets on the casino stage, hosted by Vivi. */
public class LobbyScene extends Scene {
    private static final float RADIUS = 3.3f;
    private final CasinoSet set;
    private final Part turntable = Part.group();
    private final Figure vivi;
    private final Figure[] hosts = new Figure[Machine.ALL.length];
    private final Array<Part> bulbs = new Array<>();
    private final Array<FrameBuffer> fbs = new Array<>();
    private final Array<TextTex> texts = new Array<>();
    final Coins3D coins = new Coins3D(50);
    private final Array<Button> buttons = new Array<>();
    private final Button freeBtn, dailyBtn;
    private int sel;
    private float angle, dragX = -1, dragStart, sayT;
    private String say;
    private Overlay overlay;
    private long shown;
    private String toast;
    private float toastT;
    private final Color C = new Color();
    private final Vector3 tmp = new Vector3();
    private final Matrix4 id = new Matrix4();
    private static final String[] TIPS = {
        "Swipe to pick a machine, tap PLAY to sit down!",
        "Every machine has its own bonus game.",
        "Free coins every hour, darling!",
        "The Control Room lets you test anything.",
        "Mochi's Lucky Drop pays a jackpot every time!"
    };

    public LobbyScene(ReelRiotGame game) {
        super(game);
        set = new CasinoSet(game.ui.glow);
        turntable.at(0, 0.24f, -0.4f);
        turntable.add(Part.cyl(Mat.lacquer(new Color(0.2f, 0.02f, 0.07f, 1))).at(0, 0.02f, 0).sized(9.2f, 0.06f, 9.2f));
        for (int i = 0; i < Machine.ALL.length; i++) {
            Machine m = Machine.ALL[i];
            float a = i * MathUtils.PI2 / Machine.ALL.length;
            Part cab = turntable.add(Part.group().at(MathUtils.sin(a) * RADIUS, 0, MathUtils.cos(a) * RADIUS).turned(a * MathUtils.radDeg, 0, 0));
            buildCabinet(cab, m);
            hosts[i] = Hosts.make(m.host);
            hosts[i].scale = 0.78f;
        }
        vivi = Hosts.make("vivi");
        vivi.where.set(-2.15f, 0.24f, 3.0f);
        vivi.facing = 22;
        vivi.scale = 1.0f;
        shown = game.save.coins;
        float by = 300;
        Button play = new Button("PLAY", Button.Style.GREEN, Ui.W / 2 - 210, by + 190, 420, 130, this::play);
        play.size = 74;
        play.pulse = true;
        buttons.add(play);
        freeBtn = new Button("FREE COINS", Button.Style.GOLD, 40, by, 320, 110, this::freeCoins);
        freeBtn.size = 36;
        dailyBtn = new Button("DAILY BONUS", Button.Style.GOLD, Ui.W / 2 - 160, by, 320, 110, () -> overlay = new DailyBonus(game, this));
        dailyBtn.size = 36;
        Button cr = new Button("CONTROL", Button.Style.DARK, Ui.W - 360, by, 320, 110, () -> overlay = new ControlRoom(game, null));
        cr.size = 36;
        buttons.add(freeBtn);
        buttons.add(dailyBtn);
        buttons.add(cr);
        Button left = new Button("‹", Button.Style.DARK, 30, 0, 110, 150, () -> select(sel - 1));
        Button right = new Button("›", Button.Style.DARK, Ui.W - 140, 0, 110, 150, () -> select(sel + 1));
        left.size = right.size = 80;
        buttons.add(left);
        buttons.add(right);
        layout();
    }

    private void layout() {
        for (Button b : buttons) if (b.label.equals("‹") || b.label.equals("›")) b.y = game.ui.H * 0.5f;
    }

    private void buildCabinet(Part cab, Machine m) {
        cab.add(Part.box(Mat.lacquer(m.body)).at(0, 1.45f, 0).sized(2.1f, 2.9f, 1.3f));
        cab.add(Part.box(Mat.gold()).at(0, 2.98f, 0).sized(2.3f, 0.16f, 1.4f));
        cab.add(Part.box(Mat.gold()).at(0, 0.06f, 0).sized(2.3f, 0.12f, 1.4f));
        cab.add(Part.box(Mat.lacquer(new Color(m.body).mul(0.6f))).at(0, 0.62f, 0.75f).sized(2.1f, 0.3f, 0.5f));
        FrameBuffer fb = preview(m);
        fbs.add(fb);
        cab.add(Part.box(Mat.gold()).at(0, 1.62f, 0.66f).sized(1.8f, 1.66f, 0.04f));
        cab.add(Part.quad(Mat.decal(fb.getColorBufferTexture(), true)).at(0, 1.62f, 0.73f).sized(1.66f, 1.52f, 1));
        TextTex tt = new TextTex(game.ui, m.name, Ui.TITLE, 900, 200, new Color(1f, 0.88f, 0.45f, 1), new Color(0.3f, 0.1f, 0f, 1));
        texts.add(tt);
        cab.add(Part.box(Mat.velvet(new Color(m.body).mul(0.3f))).at(0, 2.66f, 0.62f).sized(2.0f, 0.46f, 0.1f));
        cab.add(Part.quad(Mat.decal(tt.texture, true)).at(0, 2.66f, 0.72f).sized(1.95f, 0.43f, 1));
        for (int k = 0; k < 10; k++) {
            float u = k / 9f;
            bulbs.add(cab.add(Part.ball(Mat.lit(k % 2 == 0 ? new Color(1f, 0.85f, 0.5f, 1) : m.accent, 1.4f)).at(-0.95f + u * 1.9f, 2.98f, 0.72f).sized(0.09f)));
        }
        cab.add(Part.cyl(Mat.gold()).at(0, 3.12f, 0).sized(1.1f, 0.14f, 1.0f));
    }

    /** The cabinet's screen: a 3 x 3 sample of its symbols. */
    private FrameBuffer preview(Machine m) {
        int w = 520, h = 476;
        FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Matrix4 old = game.ui.batch.getProjectionMatrix().cpy();
        fb.begin();
        Gdx.gl.glClearColor(m.body.r * 0.4f, m.body.g * 0.4f, m.body.b * 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.ui.batch.setProjectionMatrix(new Matrix4().setToOrtho(0, w, h, 0, -1, 1));
        game.ui.batch.begin();
        int[] pick = { 0, 9, 4, 2, 1, 5, 10, 3, 0 };
        for (int k = 0; k < 9; k++) {
            int cx = k % 3, cy = k / 3;
            float x = 10 + cx * 170, y = 8 + cy * 156;
            game.ui.batch.setColor(m.body.r * 0.9f, m.body.g * 0.9f, m.body.b * 0.9f, 1);
            game.ui.batch.draw(game.ui.white, x, y, 160, 148);
            game.ui.batch.setColor(Color.WHITE);
            Texture t = game.ui.image(m.syms[pick[k] % m.syms.length].image);
            game.ui.batch.draw(t, x + 12, y + 6, 136, 136);
        }
        game.ui.batch.end();
        fb.end();
        game.ui.batch.setProjectionMatrix(old);
        return fb;
    }

    @Override
    public void enter() {
        ToyShader.Lighting L = ToyShader.LIGHT;
        L.keyDir.set(-0.4f, -0.8f, -0.45f).nor();
        L.key.set(1f, 0.95f, 0.88f, 1);
        L.fill.set(0.45f, 0.25f, 0.4f, 1);
        L.ambient.set(0.18f, 0.11f, 0.13f, 1);
        L.pointPos[0].set(2.5f, 3.5f, 4);
        L.point[0].set(1f, 0.55f, 0.3f, 1);
        L.pointPos[1].set(-3f, 2f, 3f);
        L.point[1].set(0.7f, 0.3f, 0.9f, 1);
        L.fogNear = 12;
        L.fogFar = 34;
        speak("Welcome back, darling!", 2.6f);
        vivi.react(Figure.Mood.WAVE, 2.4f);
        if (dailyReady() && game.driver == null) overlay = new DailyBonus(game, this);
        if (game.driver != null && game.driver.hasAction("daily")) overlay = new DailyBonus(game, this);
        if (game.driver != null && game.driver.hasAction("control")) overlay = new ControlRoom(game, null);
    }

    boolean dailyReady() { return game.save.lastDailyDay != Save.today(); }

    static final long FREE_EVERY = 60 * 60 * 1000L;

    private boolean freeReady() { return System.currentTimeMillis() - game.save.lastFree >= FREE_EVERY; }

    long levelScaled(long v) { return Math.round(v * (1 + 0.05 * (game.save.level - 1)) / 10_000.0) * 10_000; }

    private void freeCoins() {
        if (!freeReady()) {
            long left = (FREE_EVERY - (System.currentTimeMillis() - game.save.lastFree)) / 1000;
            toast("Free coins in " + (left / 60) + ":" + String.format("%02d", left % 60));
            return;
        }
        long amount = levelScaled(200_000);
        game.save.coins += amount;
        game.save.lastFree = System.currentTimeMillis();
        game.save.flush();
        toast("+" + Ui.fmt(amount) + " free coins!");
        speak("Here you go, darling!", 2f);
        vivi.react(Figure.Mood.CHEER, 1.8f);
        game.sfx.play("coin");
        game.sfx.play("win");
        coins.burst(vivi.where.x, 2.2f, vivi.where.z, 26, 6, 1.1f);
        game.ui.fx.coins(Ui.W / 2, 420, 30, 1500);
    }

    void speak(String s, float sec) { say = s; sayT = sec; }

    void toast(String s) { toast = s; toastT = 2.4f; }

    private void select(int i) {
        int n = Machine.ALL.length;
        sel = ((i % n) + n) % n;
        game.sfx.play("whoosh", 0.6f, 1.2f);
        hosts[sel].react(Figure.Mood.WAVE, 1.6f);
    }

    private void play() {
        game.sfx.play("jackpot", 0.6f, 1.2f);
        hosts[sel].react(Figure.Mood.CHEER, 1.5f);
        game.go("machine:" + Machine.ALL[sel].id);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        set.update(dt);
        vivi.update(dt);
        coins.update(dt);
        float want = -sel * MathUtils.PI2 / Machine.ALL.length;
        float diff = ((want - angle) % MathUtils.PI2 + MathUtils.PI2 * 1.5f) % MathUtils.PI2 - MathUtils.PI;
        angle += diff * Math.min(1, dt * 4);
        turntable.reset();
        turntable.rot.setFromAxisRad(0, 1, 0, angle);
        for (int i = 0; i < bulbs.size; i++) {
            float k = 0.5f + 0.5f * MathUtils.sin(time * 6 + i);
            Mat.emissive(bulbs.get(i).inst, 0.5f + 1.5f * k, 0.4f + k, 0.2f + 0.5f * k);
        }
        turntable.update(id);
        for (int i = 0; i < hosts.length; i++) {
            Part cab = turntable.kids.get(i + 1);
            tmp.set(0, 3.2f, 0).mul(cab.world);
            hosts[i].where.set(tmp);
            hosts[i].facing = (angle + i * MathUtils.PI2 / hosts.length) * MathUtils.radDeg;
            hosts[i].update(dt);
        }
        float aspect = cam.viewportWidth / cam.viewportHeight;
        cam.fieldOfView = 46;
        float d = 4.4f / (MathUtils.tanDeg(23) * Math.min(aspect, 1.3f));
        cam.position.set(MathUtils.sin(time * 0.25f) * 0.4f, 4.6f, d * 0.92f);
        ToyShader.LIGHT.fogNear = d + 4;
        ToyShader.LIGHT.fogFar = d + 40;
        cam.lookAt(0, 2.15f, 0);
        cam.near = 0.6f;
        cam.far = 80;
        cam.update();
        long target = game.save.coins;
        if (shown != target) shown += Math.signum(target - shown) * Math.max(1, Math.abs(target - shown) / 8);
        sayT -= dt;
        toastT -= dt;
        if (sayT < -5 && overlay == null) speak(TIPS[MathUtils.random(TIPS.length - 1)], 3.4f);
        freeBtn.sub = freeReady() ? "Ready!" : null;
        freeBtn.pulse = freeReady();
        dailyBtn.pulse = dailyReady();
        dailyBtn.sub = dailyReady() ? "Ready!" : null;
        if (overlay != null) {
            overlay.update(dt);
            if (overlay.finished()) overlay = null;
        }
    }

    @Override
    public void render3d(ModelBatch batch) {
        if (overlay != null && overlay.cam() != null) {
            batch.end();
            batch.begin(overlay.cam());
            overlay.render3d(batch);
            return;
        }
        set.render(batch);
        turntable.render(batch);
        for (Figure h : hosts) h.render(batch);
        vivi.render(batch);
        coins.render(batch);
    }

    @Override
    public void renderUi() {
        Ui ui = game.ui;
        if (overlay == null || !(overlay instanceof MachineScene.Fullscreen)) {
            Hud.coinBar(ui, shown, game.save, ui.H - 132);
            ui.gold("REEL RIOT", Ui.W / 2, ui.H - 270, 120, Align.center, 1);
            ui.text(Ui.TITLE, 40, "V E G A S", Ui.W / 2, ui.H - 360, Ui.GOLD, Align.center);
            Machine m = Machine.ALL[sel];
            ui.panel(60, 640, Ui.W - 120, 190, C.set(0.08f, 0.01f, 0.04f, 0.82f));
            ui.gold(m.name, Ui.W / 2, 760, 64, Align.center, 1);
            ui.text(Ui.BOLD, 36, m.tagline, Ui.W / 2, 696, Ui.CREAM, Align.center);
            ui.text(Ui.BOLD, 30, "Bonus: " + m.bonusName, Ui.W / 2, 658, Ui.GOLD, Align.center);
            if (sayT > 0) {
                tmp.set(vivi.where.x + 0.2f, 2.35f, vivi.where.z);
                cam.project(tmp);
                float hx = tmp.x / ui.px, hy = tmp.y / ui.px;
                Hud.bubble(ui, say, hx + 260, hy + 80, hx + 30, hy, Math.min(1, sayT * 3));
            }
            for (Button b : buttons) b.draw(ui, time, 1 / 60f);
        }
        ui.fx.draw();
        if (overlay != null) overlay.renderUi();
        if (toastT > 0 && toast != null) {
            float a = Math.min(1, toastT * 3);
            ui.panel(Ui.W / 2 - 380, ui.H * 0.6f - 60, 760, 120, C.set(0.08f, 0.01f, 0.04f, 0.9f * a));
            ui.text(Ui.DISPLAY, 46, toast, Ui.W / 2, ui.H * 0.6f, C.set(Ui.GOLD_HI).mul(1, 1, 1, a), Align.center);
        }
    }

    @Override
    public boolean touchDown(float x, float y) {
        if (overlay != null) return overlay.touchDown(x, y);
        for (Button b : buttons) {
            if (b.hit(x, y)) {
                b.press();
                game.sfx.play("click");
                b.onClick.run();
                return true;
            }
        }
        dragX = x;
        dragStart = x;
        return true;
    }

    @Override
    public boolean touchUp(float x, float y) {
        if (dragX >= 0 && overlay == null) {
            float dx = x - dragStart;
            if (Math.abs(dx) > 90) select(sel + (dx < 0 ? 1 : -1));
            else if (y > 900 && y < game.ui.H * 0.75f) play();
        }
        dragX = -1;
        return true;
    }

    @Override
    public boolean back() {
        if (overlay instanceof ControlRoom) { ((ControlRoom) overlay).close(); return true; }
        if (overlay != null) return true;
        return false;
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        layout();
    }

    @Override
    public void dispose() {
        for (FrameBuffer f : fbs) f.dispose();
        for (TextTex t : texts) t.dispose();
    }

    Figure vivi() { return vivi; }

    PerspectiveCamera camera() { return cam; }
}
