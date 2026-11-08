package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.Scene;
import com.reelriot.vegas.gfx.Figure;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Part;
import com.reelriot.vegas.gfx.ToyShader;
import com.reelriot.vegas.ui.Hud;
import com.reelriot.vegas.ui.Ui;
import com.reelriot.vegas.world.CasinoSet;
import com.reelriot.vegas.world.Coins3D;
import com.reelriot.vegas.world.Hosts;

/**
 * The opening cutscene: the curtains part, Vivi rises on the stage lift and
 * welcomes you, the three machine hosts appear in spotlights one by one,
 * and the show ends on the title with confetti and coins.
 */
public class IntroScene extends Scene {
    private final CasinoSet set;
    private final Figure vivi;
    private final Figure[] hosts = new Figure[3];
    private final Part[] pads = new Part[3];
    private final Part[] spots = new Part[3];
    private final Coins3D coins = new Coins3D(60);
    private final Matrix4 id = new Matrix4();
    private final Vector3 tmp = new Vector3();
    private final Color C = new Color();
    private static final String[] IDS = { "mochi", "goldbeak", "bruno" };
    private static final String[] NAMES = { "Mochi", "Captain Goldbeak", "Bruno \"Big Bucks\"" };
    private static final String[] ROLES = { "Lucky Paws Palace", "Goldbeak's Cove", "High Roller Lounge" };
    private static final float[] APPEAR = { 8.0f, 9.7f, 11.4f };
    private static final float[][] SPOT = { { -0.7f, 0.24f, 0.2f }, { 0.85f, 0.24f, -0.6f }, { 2.3f, 0.24f, 0.4f } };
    private static final float FINALE = 13.4f;
    private boolean confettiDone;
    private int lastBeat = -1;

    public IntroScene(ReelRiotGame game) {
        super(game);
        set = new CasinoSet(game.ui.glow);
        set.open = 0;
        vivi = Hosts.make("vivi");
        vivi.where.set(0, 0.24f, 1.8f);
        vivi.scale = 1.15f;
        for (int i = 0; i < 3; i++) {
            hosts[i] = Hosts.make(IDS[i]);
            hosts[i].where.set(SPOT[i][0], SPOT[i][1] + 0.2f, SPOT[i][2]);
            hosts[i].facing = -SPOT[i][0] * 10;
            hosts[i].scale = 0.85f;
            pads[i] = Part.cyl(Mat.gold()).at(SPOT[i][0], SPOT[i][1] + 0.08f, SPOT[i][2]).sized(1.3f, 0.16f, 1.3f);
            spots[i] = Part.group().at(SPOT[i][0], 7.5f, SPOT[i][2]);
            spots[i].add(Part.cone(Mat.glow(new Color(1f, 0.9f, 0.6f, 1), 0.12f)).at(0, -3.6f, 0).turned(0, 180, 0).sized(2.2f, 7.2f, 2.2f));
        }
    }

    @Override
    public void enter() {
        ToyShader.Lighting L = ToyShader.LIGHT;
        L.keyDir.set(-0.3f, -0.85f, -0.45f).nor();
        L.key.set(1f, 0.93f, 0.85f, 1);
        L.ambient.set(0.14f, 0.08f, 0.1f, 1);
        L.fill.set(0.45f, 0.22f, 0.4f, 1);
        L.pointPos[0].set(0, 3.5f, 4);
        L.point[0].set(1f, 0.7f, 0.4f, 1);
        L.pointPos[1].set(-3f, 2f, 2f);
        L.point[1].set(0.8f, 0.3f, 0.9f, 1);
        L.fogNear = 12;
        L.fogFar = 36;
        game.sfx.play("whoosh");
        if (game.driver != null && game.driver.hasAction("finale")) time = FINALE + 0.5f;
    }

    private float since(float at) { return time - at; }

    @Override
    public void update(float dt) {
        super.update(dt);
        float open = MathUtils.clamp((time - 0.8f) / 2.2f, 0, 1);
        set.open = Interpolation.pow2Out.apply(open);
        set.update(dt);
        // Vivi rises on the lift, then steps aside for the hosts
        float lift = MathUtils.clamp((time - 1.6f) / 1.2f, 0, 1);
        vivi.lift = -2.2f * (1 - Interpolation.swingOut.apply(lift));
        float aside = Interpolation.pow2.apply(MathUtils.clamp((time - 7.4f) / 0.8f, 0, 1));
        vivi.where.x = MathUtils.lerp(0, -2.3f, aside);
        vivi.where.z = MathUtils.lerp(1.8f, 1.2f, aside);
        vivi.facing = 18 * aside;
        beat(1, 1.8f, () -> { game.sfx.play("upgrade"); burst(0, 1.2f, 1.8f, 20); });
        beat(2, 3.0f, () -> vivi.react(Figure.Mood.WAVE, 2.5f));
        beat(3, 5.5f, () -> vivi.set(Figure.Mood.TALK));
        beat(4, 7.6f, () -> vivi.set(Figure.Mood.IDLE));
        for (int i = 0; i < 3; i++) {
            final int k = i;
            float a = MathUtils.clamp(since(APPEAR[i]) / 0.9f, 0, 1);
            hosts[i].lift = -2.6f * (1 - Interpolation.swingOut.apply(a));
            beat(10 + i, APPEAR[i], () -> {
                game.sfx.play("sparkle", 1, 0.9f + k * 0.1f);
                burst(SPOT[k][0], 1.3f, SPOT[k][2], 16);
            });
            beat(20 + i, APPEAR[i] + 0.9f, () -> hosts[k].react(Figure.Mood.WAVE, 1.8f));
        }
        beat(30, FINALE, () -> {
            vivi.set(Figure.Mood.CHEER);
            for (Figure h : hosts) h.set(Figure.Mood.CHEER);
            game.sfx.play("jackpot");
            game.sfx.buzz(100);
        });
        if (time > FINALE + 0.2f && !confettiDone) {
            confettiDone = true;
            game.ui.fx.confetti(Ui.W / 2, game.ui.H * 0.55f, 140, 1900);
            coins.burst(0, 2.5f, 1.5f, 50, 8, 1.1f);
        }
        vivi.update(dt);
        for (Figure h : hosts) h.update(dt);
        coins.update(dt);
        for (int i = 0; i < 3; i++) {
            pads[i].reset();
            pads[i].offset.y = hosts[i].lift;
            pads[i].update(id);
            spots[i].reset();
            spots[i].visible = time > APPEAR[i] - 0.3f;
            spots[i].update(id);
        }
        // the camera glides in from the back of the room, then drifts
        float k = Interpolation.pow2Out.apply(MathUtils.clamp(time / 4f, 0, 1));
        float aspect = cam.viewportWidth / cam.viewportHeight;
        float far = 3.5f / (MathUtils.tanDeg(22) * Math.min(aspect, 1.3f));
        cam.fieldOfView = 44;
        cam.position.set(MathUtils.sin(time * 0.3f) * 0.5f * k, MathUtils.lerp(5.5f, 3.3f, k), MathUtils.lerp(far * 1.6f, far, k));
        cam.lookAt(0, MathUtils.lerp(3.6f, 2.0f, k), 0);
        ToyShader.LIGHT.fogNear = far + 3;
        ToyShader.LIGHT.fogFar = far + 40;
        cam.near = 0.6f;
        cam.far = 90;
        cam.update();
    }

    private void beat(int id, float at, Runnable r) {
        if (time >= at && lastBeatDone(id)) r.run();
    }

    private final com.badlogic.gdx.utils.IntSet done = new com.badlogic.gdx.utils.IntSet();

    private boolean lastBeatDone(int id) { return done.add(id); }

    private void burst(float x, float y, float z, int n) {
        tmp.set(x, y, z);
        cam.project(tmp);
        game.ui.fx.burst(tmp.x / game.ui.px, tmp.y / game.ui.px, n, 700, new Color(1f, 0.85f, 0.45f, 1));
    }

    @Override
    public void render3d(ModelBatch batch) {
        set.render(batch);
        for (int i = 0; i < 3; i++) {
            if (time < APPEAR[i] - 0.4f) continue;
            spots[i].render(batch);
            pads[i].render(batch);
            hosts[i].render(batch);
        }
        if (time > 1.4f) vivi.render(batch);
        coins.render(batch);
    }

    @Override
    public void renderUi() {
        Ui ui = game.ui;
        String line = time < 3.2f ? null : time < 5.5f ? "Welcome to Reel Riot Vegas!" : time < 7.8f ? "I'm Vivi, your hostess." : time < FINALE ? "Meet your hosts!" : "Spin, win and have fun!";
        if (line != null) {
            tmp.set(0.3f, 2.5f * vivi.scale, vivi.where.z);
            cam.project(tmp);
            Hud.bubble(ui, line, tmp.x / ui.px + 200, tmp.y / ui.px + 90, tmp.x / ui.px + 40, tmp.y / ui.px + 10, 1);
        }
        for (int i = 2; i >= 0; i--) {
            float age = since(APPEAR[i] + 0.3f);
            if (age < 0 || time > FINALE) continue;
            float a = Math.min(1, age * 4) * (i < 2 && time > APPEAR[i + 1] + 0.3f ? 0 : 1);
            if (a <= 0) break;
            float y = ui.H * 0.22f, slide = (1 - Math.min(1, age * 4)) * 200;
            ui.panel(60 - slide, y - 80, Ui.W - 120, 160, C.set(0.85f, 0.6f, 0.15f, a));
            ui.panel(66 - slide, y - 74, Ui.W - 132, 148, C.set(0.12f, 0.02f, 0.06f, 0.95f * a));
            ui.gold(NAMES[i], Ui.W / 2 - slide, y + 22, 64, Align.center, a);
            ui.text(Ui.BOLD, 36, "Host of " + ROLES[i], Ui.W / 2 - slide, y - 40, C.set(Ui.CREAM).mul(1, 1, 1, a), Align.center);
            break;
        }
        if (time > FINALE) {
            float a = Math.min(1, since(FINALE) * 2.5f);
            float pop = 1 + Math.max(0, 0.3f - since(FINALE)) * 1.6f;
            ui.gold("REEL RIOT", Ui.W / 2, ui.H - 300, (int) (170 * pop), Align.center, a);
            ui.text(Ui.TITLE, 64, "V E G A S", Ui.W / 2, ui.H - 450, C.set(Ui.GOLD).mul(1, 1, 1, a), Align.center);
            if (since(FINALE) > 1.2f) {
                float k = 0.55f + 0.45f * MathUtils.sin(time * 5);
                ui.shadowed(Ui.DISPLAY, 56, "Tap to enter the casino", Ui.W / 2, 200, new Color(1f, 0.95f, 0.75f, k), Align.center);
            }
        } else {
            ui.shadowed(Ui.BOLD, 36, "Tap to skip", Ui.W - 150, 80, new Color(1, 1, 1, 0.6f), Align.center);
        }
        ui.fx.draw();
    }

    @Override
    public boolean touchDown(float x, float y) {
        if (time < FINALE) {
            time = FINALE;
            for (int i = 0; i < 3; i++) hosts[i].lift = 0;
        } else if (since(FINALE) > 0.8f) {
            game.save.introSeen = true;
            game.save.flush();
            game.go("lobby");
        }
        return true;
    }

    @Override
    public boolean back() {
        game.save.introSeen = true;
        game.go("lobby");
        return true;
    }
}
