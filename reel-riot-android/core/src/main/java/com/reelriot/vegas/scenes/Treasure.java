package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.gfx.Figure;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Part;
import com.reelriot.vegas.gfx.ToyShader;
import com.reelriot.vegas.slots.Machine;
import com.reelriot.vegas.slots.SlotEngine;
import com.reelriot.vegas.ui.Hud;
import com.reelriot.vegas.ui.Ui;
import com.reelriot.vegas.world.Coins3D;
import com.reelriot.vegas.world.Hosts;

/** Goldbeak's treasure hunt: pick one of three chests; the others reveal what they held. */
public class Treasure implements Overlay, MachineScene.Fullscreen {
    private final ReelRiotGame game;
    private final MachineScene scene;
    private final SlotEngine.Prize[] shown = new SlotEngine.Prize[3];
    private final PerspectiveCamera cam = new PerspectiveCamera(42, 1, 1);
    private final Part world = Part.group();
    private final Part[] lids = new Part[3], glows = new Part[3];
    private final float[] open = new float[3];
    private final Figure parrot;
    private final Coins3D coins = new Coins3D(50);
    private final Matrix4 id = new Matrix4();
    private final Vector3 tmp = new Vector3();
    private int phase, picked = -1;
    private float t;
    private boolean done;
    private final Color C = new Color();

    Treasure(ReelRiotGame game, MachineScene scene, SlotEngine.Prize prize) {
        this.game = game;
        this.scene = scene;
        world.add(com.reelriot.vegas.world.Backdrop.make(game.ui.glow, new Color(0.03f, 0.25f, 0.33f, 1), new Color(0.4f, 0.9f, 1f, 1), -4.5f, 24, 18)).at(0, 1.5f, 0);
        world.add(Part.cyl(Mat.make(new Color(0.85f, 0.7f, 0.42f, 1), 0f, 0.9f)).at(0, -0.1f, 0).sized(7.5f, 0.2f, 5f));
        world.add(Part.ring(Mat.gold(), 0.02f).at(0, 0.01f, 0).sized(6.6f, 1, 4.4f));
        Material wood = Mat.satin(new Color(0.45f, 0.22f, 0.08f, 1)), gold = Mat.gold();
        for (int i = 0; i < 3; i++) {
            float x = (i - 1) * 1.75f;
            Part chest = world.add(Part.group().at(x, 0, 0.6f).turned((1 - i) * 10, 0, 0));
            chest.add(Part.box(wood).at(0, 0.4f, 0).sized(1.3f, 0.8f, 0.85f));
            for (int s = -1; s <= 1; s += 2) chest.add(Part.box(gold).at(s * 0.4f, 0.4f, 0).sized(0.12f, 0.82f, 0.88f));
            chest.add(Part.box(gold).at(0, 0.78f, 0).sized(1.34f, 0.08f, 0.89f));
            chest.add(Part.box(gold).at(0, 0.62f, 0.44f).sized(0.24f, 0.3f, 0.06f));
            glows[i] = chest.add(Part.quad(Mat.glowSprite(game.ui.glow, new Color(1f, 0.8f, 0.35f, 1), 0.9f)).at(0, 1.1f, 0).sized(2.4f));
            glows[i].visible = false;
            lids[i] = chest.add(Part.group().at(0, 0.8f, -0.42f));
            lids[i].add(Part.cyl(wood).at(0, 0.0f, 0.42f).turned(0, 0, 90).sized(0.62f, 1.3f, 0.86f));
            lids[i].add(Part.box(gold).at(0.4f, 0.0f, 0.42f).sized(0.12f, 0.46f, 0.9f));
            lids[i].add(Part.box(gold).at(-0.4f, 0.0f, 0.42f).sized(0.12f, 0.46f, 0.9f));
        }
        parrot = Hosts.make("goldbeak");
        parrot.where.set(0, 0, -1.6f);
        parrot.scale = 1.55f;
        parrot.set(Figure.Mood.TALK);
        coins.floor = -3;
        coins.gravity = -7;
        SlotEngine.Prize[] table = scene.engine.prizes();
        for (int i = 0; i < 3; i++) shown[i] = prize;
        // the two chests not picked show other prizes from the same table
        for (int k = 0; k < 3; k++) {
            SlotEngine.Prize p;
            do { p = table[MathUtils.random(table.length - 1)]; } while (p == prize && table.length > 1);
            shown[k] = p;
        }
        this.prizeRef = prize;
        ToyShader.Lighting L = ToyShader.LIGHT;
        L.keyDir.set(-0.3f, -0.8f, -0.5f).nor();
        L.pointPos[0].set(0, 2.5f, 2.5f);
        L.point[0].set(1f, 0.7f, 0.35f, 1);
        game.sfx.play("whoosh");
    }

    private final SlotEngine.Prize prizeRef;

    @Override
    public void update(float dt) {
        t += dt;
        float aspect = Math.max(0.3f, game.ui.W / game.ui.H);
        cam.viewportWidth = 1;
        cam.viewportHeight = 1 / aspect;
        float d = 3.1f / (MathUtils.tanDeg(21) * Math.min(aspect, 1f));
        cam.position.set(MathUtils.sin(t * 0.3f) * 0.3f, 2.4f + d * 0.18f, d);
        cam.lookAt(0, 0.9f, 0);
        cam.near = 0.5f;
        cam.far = 80;
        cam.update();
        if (phase == 0 && t > 1f) phase = 1;
        if (phase == 2 && t > 1.6f) {
            phase = 3;
            t = 0;
            for (int i = 0; i < 3; i++) if (i != picked) game.sfx.play("peg", 0.6f, 0.7f);
        }
        if (phase == 3 && t > 2.4f) phase = 4;
        world.reset();
        for (int i = 0; i < 3; i++) {
            boolean opening = (phase >= 2 && i == picked) || (phase >= 3 && i != picked);
            float want = opening ? 1 : 0;
            open[i] += (want - open[i]) * Math.min(1, dt * 5);
            lids[i].spin(1, 0, 0, -105 * open[i]);
            glows[i].visible = open[i] > 0.2f;
            if (glows[i].visible) Mat.opacity(glows[i].inst, (i == picked ? 0.9f : 0.35f) * open[i] * (0.8f + 0.2f * MathUtils.sin(t * 8)));
            if (phase == 1) lids[i].spin(1, 0, 0, -6 * Math.max(0, MathUtils.sin(t * 5 + i * 1.3f)));
        }
        world.update(id);
        parrot.update(dt);
        coins.update(dt);
    }

    private Vector3 project(int i, float y) {
        tmp.set((i - 1) * 1.75f, y, 0.6f);
        cam.project(tmp, 0, 0, game.ui.W * game.ui.px, game.ui.H * game.ui.px);
        tmp.x /= game.ui.px;
        tmp.y /= game.ui.px;
        return tmp;
    }

    @Override
    public PerspectiveCamera cam() { return cam; }

    @Override
    public void render3d(ModelBatch batch) {
        world.render(batch);
        parrot.render(batch);
        coins.render(batch);
    }

    @Override
    public void renderUi() {
        Ui ui = game.ui;
        ui.gold("TREASURE HUNT", Ui.W / 2, ui.H - 150, 104, Align.center, Math.min(1, t * 3 + (phase > 0 ? 1 : 0)));
        if (phase <= 1) {
            ui.shadowed(Ui.BOLD, 46, "Pick a chest, matey!", Ui.W / 2, ui.H - 262, Ui.CREAM, Align.center);
            for (int i = 0; i < 3 && phase == 1; i++) {
                Vector3 p = project(i, 1.5f);
                ui.shadowed(Ui.DISPLAY, 70, "?", p.x, p.y + 20 + MathUtils.sin(t * 4 + i) * 10, Ui.GOLD_HI, Align.center);
            }
        }
        for (int i = 0; i < 3; i++) {
            if (open[i] < 0.6f) continue;
            SlotEngine.Prize p = i == picked ? prizeRef : shown[i];
            Vector3 s = project(i, 1.75f);
            float a = i == picked ? 1 : 0.6f;
            String label = p.jackpot >= 0 ? Machine.JACKPOT_NAMES[p.jackpot] : Ui.shortFmt(Math.round(scene.bet() * p.x));
            if (p.jackpot >= 0) ui.glow(s.x, s.y, 150, C.set(Hud.JACKPOT_COLORS[p.jackpot]).mul(1, 1, 1, 0.6f * a));
            ui.panel(s.x - 150, s.y - 46, 300, 92, C.set(0.08f, 0.01f, 0.04f, 0.85f * a));
            if (i == picked) ui.gold(label, s.x, s.y, 62, Align.center, a);
            else ui.text(Ui.DISPLAY, 50, label, s.x, s.y, C.set(1, 1, 1, a), Align.center);
        }
        if (phase == 4) {
            float k = 0.6f + 0.4f * MathUtils.sin(t * 5);
            ui.shadowed(Ui.DISPLAY, 46, "Tap to collect", Ui.W / 2, 240, new Color(1f, 0.95f, 0.75f, k), Align.center);
        }
    }

    @Override
    public boolean touchDown(float x, float y) {
        if (phase == 1) {
            int best = -1;
            float bestD = 300;
            for (int i = 0; i < 3; i++) {
                Vector3 p = project(i, 0.6f);
                float d = Math.abs(p.x - x) + Math.abs(p.y - y) * 0.5f;
                if (d < bestD) { bestD = d; best = i; }
            }
            if (best < 0) best = x < Ui.W / 3 ? 0 : x > Ui.W * 2 / 3 ? 2 : 1;
            picked = best;
            phase = 2;
            t = 0;
            game.sfx.play("jackpot");
            game.sfx.buzz(80);
            parrot.set(Figure.Mood.CHEER);
            coins.burst((picked - 1) * 1.75f, 1.0f, 0.6f, 40, 5.5f, 0.6f);
            Vector3 p = project(picked, 1.2f);
            game.ui.fx.burst(p.x, p.y, 40, 800, new Color(1f, 0.85f, 0.4f, 1));
        } else if (phase == 4) {
            done = true;
        }
        return true;
    }

    @Override
    public boolean finished() { return done; }
}
