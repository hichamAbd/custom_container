package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
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

/**
 * Lucky Drop: Mochi drops a gold coin down a board of pegs into jackpot
 * buckets. The coin really bounces off the pegs; the jackpot was decided
 * when the bonus started, and a gentle pull guides the coin to it.
 */
public class LuckyDrop implements Overlay, MachineScene.Fullscreen {
    private static final int[] BUCKET_TIER = { 0, 1, 2, 3, 2, 1, 0 };
    private static final float PEG_R = 0.09f, COIN_R = 0.25f, BUCKET_Y = -3.55f;

    private final ReelRiotGame game;
    private final MachineScene scene;
    private final SlotEngine.Prize prize;
    private final PerspectiveCamera cam = new PerspectiveCamera(50, 1, 1);
    private final Part board = Part.group();
    private final Array<Part> pegs = new Array<>();
    private final Array<Vector3> pegPos = new Array<>();
    private final float[] pegGlow;
    private final Part coin;
    private Part coinBody;
    private final Figure mochi;
    private final Coins3D burst = new Coins3D(40);
    private final Matrix4 id = new Matrix4();
    private final int target;
    private int phase;
    private float t, px = 0, py = 3.35f, vx, vy, spin;
    private boolean done;
    private final Color C = new Color();
    private final Vector3 tmp = new Vector3();

    LuckyDrop(ReelRiotGame game, MachineScene scene, SlotEngine.Prize prize) {
        this.game = game;
        this.scene = scene;
        this.prize = prize;
        IntPicker pick = new IntPicker();
        for (int i = 0; i < BUCKET_TIER.length; i++) if (BUCKET_TIER[i] == prize.jackpot) pick.add(i);
        target = pick.any();
        board.add(com.reelriot.vegas.world.Backdrop.make(game.ui.glow, new Color(0.4f, 0.03f, 0.1f, 1), new Color(1f, 0.55f, 0.25f, 1), -4f, 22, 18));
        board.add(Part.box(Mat.lacquer(new Color(0.32f, 0.02f, 0.08f, 1))).at(0, -0.2f, -0.25f).sized(6.8f, 9.2f, 0.2f));
        board.add(Part.quad(Mat.glowSprite(game.ui.glow, new Color(1f, 0.5f, 0.25f, 1), 0.25f)).at(0, 0.5f, -0.14f).sized(7, 9, 1));
        for (int s = -1; s <= 1; s += 2) board.add(Part.box(Mat.gold()).at(s * 3.4f, -0.2f, 0).sized(0.18f, 9.4f, 0.5f));
        board.add(Part.box(Mat.gold()).at(0, 4.4f, 0).sized(7, 0.18f, 0.5f));
        for (int j = 0; j < 9; j++) {
            int n = j % 2 == 0 ? 8 : 7;
            float x0 = j % 2 == 0 ? -2.45f : -2.1f;
            for (int i = 0; i < n; i++) {
                Vector3 p = new Vector3(x0 + i * 0.7f, 2.6f - j * 0.68f, 0);
                pegPos.add(p);
                pegs.add(board.add(Part.cyl(Mat.gold()).at(p.x, p.y, 0.05f).turned(0, 90, 0).sized(PEG_R * 2, 0.4f, PEG_R * 2)));
            }
        }
        pegGlow = new float[pegs.size];
        for (int i = 0; i < 7; i++) {
            float x = -2.4f + i * 0.8f;
            Color col = com.reelriot.vegas.ui.Hud.JACKPOT_COLORS[BUCKET_TIER[i]];
            board.add(Part.box(Mat.jewel(col)).at(x, BUCKET_Y - 0.15f, 0).sized(0.74f, 0.7f, 0.4f));
            if (i < 6) board.add(Part.box(Mat.gold()).at(x + 0.4f, BUCKET_Y + 0.05f, 0.1f).sized(0.06f, 1.0f, 0.5f));
        }
        board.add(Part.box(Mat.gold()).at(0, BUCKET_Y - 0.55f, 0.05f).sized(6.8f, 0.14f, 0.5f));
        coin = Part.group();
        coin.add(Part.quad(Mat.glowSprite(game.ui.glow, new Color(1f, 0.8f, 0.35f, 1), 0.85f)).at(0, 0, -0.08f).sized(1.4f));
        coinBody = coin.add(Part.cyl(Mat.gold()).turned(0, 90, 0).sized(COIN_R * 2, 0.08f, COIN_R * 2));
        mochi = Hosts.make("mochi");
        mochi.where.set(0.55f, 3.55f, 0.6f);
        mochi.scale = 0.75f;
        mochi.set(Figure.Mood.TALK);
        ToyShader.Lighting L = ToyShader.LIGHT;
        L.keyDir.set(-0.3f, -0.6f, -0.75f).nor();
        L.pointPos[0].set(0, 1, 4);
        L.point[0].set(1f, 0.6f, 0.3f, 1);
        burst.floor = -6;
        game.sfx.play("whoosh");
    }

    @Override
    public void update(float dt) {
        t += dt;
        float aspect = Math.max(0.3f, game.ui.W / game.ui.H);
        cam.viewportWidth = 1;
        cam.viewportHeight = 1 / aspect;
        cam.fieldOfView = 50;
        float d = 3.7f / (MathUtils.tanDeg(25) * Math.min(aspect, 1.2f));
        cam.position.set(MathUtils.sin(t * 0.4f) * 0.25f, 0.3f, d);
        cam.lookAt(0, 0.1f, 0);
        cam.near = 0.5f;
        cam.far = 80;
        cam.update();
        mochi.update(dt);
        burst.update(dt);
        if (phase == 0 && t > 1.2f) phase = 1;
        if (phase == 2) step(dt);
        if (phase == 3 && t > 2.6f) done = true;
        board.reset();
        for (int i = 0; i < pegs.size; i++) {
            pegGlow[i] = Math.max(0, pegGlow[i] - dt * 2.5f);
            Mat.emissive(pegs.get(i).inst, pegGlow[i] * 2f, pegGlow[i] * 1.5f, pegGlow[i] * 0.6f);
            pegs.get(i).pulse = 1 + pegGlow[i] * 0.35f;
        }
        board.update(id);
        if (phase < 2) { px = 0.2f; py = 3.3f; }
        coin.reset();
        coin.pos.set(px, py, 0.15f);
        coinBody.spin(0, 0, 1, spin);
        coin.update(id);
    }

    /** Coin physics against the pegs, with a pull toward the decided bucket. */
    private void step(float dt) {
        int sub = 4;
        float h = dt / sub;
        float tx = -2.4f + target * 0.8f;
        for (int s = 0; s < sub; s++) {
            vy -= 9.0f * h;
            float depth = MathUtils.clamp((3.3f - py) / 6.6f, 0, 1);
            vx += (tx - px) * (0.6f + depth * depth * 9f) * h;
            vx *= 1 - 0.6f * h;
            px += vx * h;
            py += vy * h;
            spin += vx * 400 * h;
            for (int i = 0; i < pegPos.size; i++) {
                Vector3 p = pegPos.get(i);
                float dx = px - p.x, dy = py - p.y, dist = (float) Math.sqrt(dx * dx + dy * dy), min = PEG_R + COIN_R;
                if (dist < min && dist > 1e-4f) {
                    float nx = dx / dist, ny = dy / dist;
                    px = p.x + nx * min;
                    py = p.y + ny * min;
                    float vn = vx * nx + vy * ny;
                    if (vn < 0) {
                        vx -= 1.45f * vn * nx;
                        vy -= 1.45f * vn * ny;
                        vx += MathUtils.random(-0.25f, 0.25f);
                        if (pegGlow[i] < 0.5f) {
                            game.sfx.play("peg", 0.7f, 0.85f + (9 - Math.round((2.6f - p.y) / 0.68f)) * 0.04f);
                            game.sfx.buzz(8);
                        }
                        pegGlow[i] = 1;
                    }
                }
            }
            if (px < -3.1f) { px = -3.1f; vx = Math.abs(vx) * 0.5f; }
            if (px > 3.1f) { px = 3.1f; vx = -Math.abs(vx) * 0.5f; }
            if (py < BUCKET_Y + 0.3f) {
                py = BUCKET_Y + 0.3f;
                px = tx;
                land();
                return;
            }
        }
    }

    private void land() {
        phase = 3;
        t = 0;
        game.sfx.play("drop");
        game.sfx.play("jackpot");
        game.sfx.buzz(120);
        burst.burst(px, py + 0.2f, 0.4f, 30, 7, 1.1f);
        mochi.set(Figure.Mood.CHEER);
        Vector3 p = project(px, py);
        game.ui.fx.burst(p.x, p.y, 40, 700, Hud.JACKPOT_COLORS[prize.jackpot]);
        game.ui.fx.confetti(Ui.W / 2, game.ui.H * 0.4f, 90, 1700);
    }

    private Vector3 project(float x, float y) {
        tmp.set(x, y, 0.2f);
        cam.project(tmp, 0, 0, game.ui.W * game.ui.px, game.ui.H * game.ui.px);
        tmp.x /= game.ui.px;
        tmp.y /= game.ui.px;
        return tmp;
    }

    @Override
    public PerspectiveCamera cam() { return cam; }

    @Override
    public void render3d(ModelBatch batch) {
        board.render(batch);
        coin.render(batch);
        mochi.render(batch);
        burst.render(batch);
    }

    @Override
    public void renderUi() {
        Ui ui = game.ui;
        for (int i = 0; i < 7; i++) {
            Vector3 p = project(-2.4f + i * 0.8f, BUCKET_Y - 0.15f);
            boolean hit = phase == 3 && i == target;
            if (hit) ui.glow(p.x, p.y, 200, C.set(1f, 0.85f, 0.4f, 0.6f + 0.3f * MathUtils.sin(t * 10)));
            ui.text(Ui.DISPLAY, 26, Machine.JACKPOT_NAMES[BUCKET_TIER[i]], p.x, p.y + 12, Color.WHITE, Align.center);
            ui.text(Ui.BOLD, 24, Ui.shortFmt(scene.bet() * Machine.JACKPOT_X[BUCKET_TIER[i]]), p.x, p.y - 18, Ui.GOLD_HI, Align.center);
        }
        ui.gold("LUCKY DROP", Ui.W / 2, ui.H - 150, 110, Align.center, Math.min(1, t * 3 + (phase > 0 ? 1 : 0)));
        if (phase == 1) {
            float k = 0.6f + 0.4f * MathUtils.sin(t * 6);
            ui.shadowed(Ui.DISPLAY, 50, "Tap to drop the Lucky Coin!", Ui.W / 2, ui.H - 270, new Color(1f, 0.95f, 0.75f, k), Align.center);
        } else if (phase == 0) {
            ui.shadowed(Ui.BOLD, 44, "Every bucket is a jackpot!", Ui.W / 2, ui.H - 270, Ui.CREAM, Align.center);
        } else if (phase == 3) {
            ui.gold(Machine.JACKPOT_NAMES[prize.jackpot] + "!", Ui.W / 2, ui.H * 0.5f, 150, Align.center, Math.min(1, t * 4));
        }
    }

    @Override
    public boolean touchDown(float x, float y) {
        if (phase == 1) {
            phase = 2;
            vx = MathUtils.random(-0.6f, 0.6f);
            vy = 0;
            mochi.react(Figure.Mood.WAVE, 1.2f);
            game.sfx.play("whoosh", 0.6f, 1.3f);
        }
        return true;
    }

    @Override
    public boolean finished() { return done; }

    /** Picks one of the buckets showing the decided jackpot. */
    private static final class IntPicker {
        private final com.badlogic.gdx.utils.IntArray a = new com.badlogic.gdx.utils.IntArray();

        void add(int i) { a.add(i); }

        int any() { return a.size == 0 ? 3 : a.get(MathUtils.random(a.size - 1)); }
    }
}
