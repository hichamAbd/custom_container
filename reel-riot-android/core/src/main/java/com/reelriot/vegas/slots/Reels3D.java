package com.reelriot.vegas.slots;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Shapes;

/**
 * Five real reel drums. Each drum carries SLOTS symbols around its rim on
 * a texture that is repainted at the start of every spin: the symbols in
 * view stay put, and the result is painted on the far side, so the reel
 * really turns to it.
 */
public class Reels3D implements Disposable {
    public static final int SLOTS = 20, CELL = 192;
    public static final float C = 1.0f, GAP = 0.08f;
    public static final float R = SLOTS * C / MathUtils.PI2;

    private final SlotEngine engine;
    private final Machine m;
    private final Pixmap[] cells;
    private final Pixmap[] strips = new Pixmap[Machine.REELS];
    private final Texture[] tex = new Texture[Machine.REELS];
    public final ModelInstance[] drums = new ModelInstance[Machine.REELS];
    private final ModelInstance[] caps = new ModelInstance[Machine.REELS * 2];
    private final int[][] slotSym = new int[Machine.REELS][SLOTS];
    /** Drum angle (radians) and the slot centered in the middle row. */
    private final float[] phi = new float[Machine.REELS];
    private final int[] center = new int[Machine.REELS];
    private final float[] from = new float[Machine.REELS], to = new float[Machine.REELS], t0 = new float[Machine.REELS], dur = new float[Machine.REELS];
    private final boolean[] spinning = new boolean[Machine.REELS], landed = new boolean[Machine.REELS];
    private final float[] bounce = new float[Machine.REELS];
    private float clock;

    public interface Listener { void reelStopped(int reel); }

    public Reels3D(SlotEngine engine) {
        this.engine = engine;
        this.m = engine.m;
        cells = new Pixmap[m.syms.length];
        for (int i = 0; i < m.syms.length; i++) cells[i] = makeCell(m.syms[i]);
        for (int r = 0; r < Machine.REELS; r++) {
            strips[r] = new Pixmap(CELL, CELL * SLOTS, Pixmap.Format.RGBA8888);
            for (int k = 0; k < SLOTS; k++) slotSym[r][k] = filler(r);
            center[r] = 5;
            paint(r);
            tex[r] = new Texture(strips[r]);
            tex[r].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            drums[r] = Shapes.get().make(Shapes.get().drum(), Mat.textured(tex[r], 0.35f));
            phi[r] = angleFor(center[r]);
            for (int s = 0; s < 2; s++) caps[r * 2 + s] = Shapes.get().make(Shapes.get().cylinder(), Mat.darkGold());
        }
    }

    public static float x(int reel) { return (reel - 2) * (C + GAP); }

    /** World position of a cell's center on the drum front (row 0 = top). */
    public Vector3 cellPos(int reel, int row, Vector3 out) {
        return out.set(x(reel), (1 - row) * C, 0.02f);
    }

    private int filler(int reel) {
        int[] s = engine.strips[reel];
        return s[MathUtils.random(s.length - 1)];
    }

    private static float angleFor(int slot) { return -MathUtils.PI2 * (slot + 0.5f) / SLOTS; }

    /** Paint a symbol tile: a jewel-toned tile with the symbol on it. */
    private Pixmap makeCell(Machine.Sym s) {
        Pixmap src = new Pixmap(Gdx.files.internal(s.image));
        Pixmap p = new Pixmap(CELL, CELL, Pixmap.Format.RGBA8888);
        Color top = new Color(m.body).lerp(Color.WHITE, 0.08f), bot = new Color(m.body).mul(0.35f);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < CELL; y++) {
            float k = y / (float) CELL;
            Color c = new Color(top).lerp(bot, k);
            for (int x = 0; x < CELL; x++) {
                float dx = (x - CELL / 2f) / (CELL / 2f), dy = (y - CELL * 0.42f) / (CELL / 2f);
                float g = MathUtils.clamp(1 - (dx * dx + dy * dy), 0, 1) * 0.35f;
                p.drawPixel(x, y, Color.rgba8888(Math.min(1, c.r + g * m.glow.r * 0.6f), Math.min(1, c.g + g * m.glow.g * 0.6f), Math.min(1, c.b + g * m.glow.b * 0.6f), 1));
            }
        }
        for (int x = 0; x < CELL; x++) {
            p.drawPixel(x, 0, Color.rgba8888(1f, 0.85f, 0.45f, 1));
            p.drawPixel(x, 1, Color.rgba8888(0.6f, 0.4f, 0.12f, 1));
            p.drawPixel(x, CELL - 1, Color.rgba8888(0.25f, 0.12f, 0.02f, 1));
        }
        p.setBlending(Pixmap.Blending.SourceOver);
        p.setFilter(Pixmap.Filter.BiLinear);
        int size = s.wild ? 182 : 168, off = (CELL - size) / 2;
        p.drawPixmap(src, 0, 0, src.getWidth(), src.getHeight(), off, off, size, size);
        src.dispose();
        return p;
    }

    private void paint(int r) {
        Pixmap p = strips[r];
        for (int k = 0; k < SLOTS; k++) p.drawPixmap(cells[slotSym[r][k]], 0, k * CELL);
    }

    /** Start spinning to the given result. Reels after an anticipation reel spin longer. */
    public void spin(SlotEngine.Result res, boolean turbo, boolean anticipate) {
        for (int r = 0; r < Machine.REELS; r++) {
            // keep the visible slots, put the result on the far side of the drum
            int target = (center[r] + SLOTS / 2) % SLOTS;
            int[] s = engine.strips[r];
            for (int k = 0; k < SLOTS; k++) {
                int d = Math.abs(((k - center[r]) % SLOTS + SLOTS + SLOTS / 2) % SLOTS - SLOTS / 2);
                if (d > 1) slotSym[r][k] = filler(r);
            }
            for (int row = 0; row < Machine.ROWS; row++) slotSym[r][(target - 1 + row + SLOTS) % SLOTS] = s[(res.stops[r] - 1 + row + s.length) % s.length];
            paint(r);
            tex[r].draw(strips[r], 0, 0);
            float extra = r == 4 && anticipate ? (turbo ? 1.2f : 2.4f) : 0;
            float turns = (turbo ? 2 : 3) + r * (turbo ? 0.5f : 1f) + extra * 1.6f;
            from[r] = phi[r];
            float end = angleFor(target);
            float k = MathUtils.ceil((from[r] + turns * MathUtils.PI2 - end) / MathUtils.PI2);
            to[r] = end + k * MathUtils.PI2;
            t0[r] = clock + r * 0.04f;
            dur[r] = (turbo ? 0.9f : 1.6f) + r * (turbo ? 0.16f : 0.32f) + extra;
            spinning[r] = true;
            landed[r] = false;
            center[r] = target;
        }
    }

    public boolean spinning() {
        for (boolean b : spinning) if (b) return true;
        return false;
    }

    public boolean reelSpinning(int r) { return spinning[r]; }

    /** Seconds left until a reel lands. */
    public float remaining(int r) { return spinning[r] ? t0[r] + dur[r] - clock : 0; }

    /** Hurry every reel to land soon. */
    public void slam() {
        for (int r = 0; r < Machine.REELS; r++) {
            if (!spinning[r]) continue;
            float left = t0[r] + dur[r] - clock;
            float want = 0.18f + r * 0.06f;
            if (left > want) {
                float u = MathUtils.clamp((clock - t0[r]) / dur[r], 0, 1);
                phi[r] = from[r] + (to[r] - from[r]) * ease(u);
                from[r] = phi[r];
                float end = angleFor(center[r]);
                float k = MathUtils.ceil((from[r] + 0.6f * MathUtils.PI2 - end) / MathUtils.PI2);
                to[r] = end + k * MathUtils.PI2;
                t0[r] = clock;
                dur[r] = want;
            }
        }
    }

    /** Fast launch, long slowdown, a small overshoot and settle. */
    private static float ease(float u) {
        float s = 1.25f;
        float x = u - 1;
        return 1 + (s + 1) * x * x * x + s * x * x;
    }

    public void update(float dt, Listener listener) {
        clock += dt;
        for (int r = 0; r < Machine.REELS; r++) {
            if (spinning[r]) {
                float u = MathUtils.clamp((clock - t0[r]) / dur[r], 0, 1);
                if (u <= 0) {
                    // a quick wind-up before launch
                    phi[r] = from[r] - 0.05f * MathUtils.sin(MathUtils.clamp((clock - t0[r] + 0.1f) / 0.1f, 0, 1) * MathUtils.PI);
                } else {
                    phi[r] = from[r] + (to[r] - from[r]) * ease(u);
                }
                if (u >= 1) {
                    spinning[r] = false;
                    phi[r] = angleFor(center[r]);
                    bounce[r] = 1;
                    if (listener != null) listener.reelStopped(r);
                }
            }
            bounce[r] = Math.max(0, bounce[r] - dt * 4);
            float cx = x(r);
            drums[r].transform.setToTranslation(cx, 0, -R).rotateRad(Vector3.X, phi[r]).scale(C, 2 * R, 2 * R);
            for (int s = 0; s < 2; s++) {
                float side = s == 0 ? -1 : 1;
                caps[r * 2 + s].transform.setToTranslation(cx + side * (C / 2 + 0.02f), 0, -R).rotate(Vector3.Z, 90).scale(2 * R + 0.08f, 0.04f, 2 * R + 0.08f);
            }
        }
    }

    public void render(ModelBatch batch) {
        for (ModelInstance d : drums) batch.render(d);
        for (ModelInstance c : caps) batch.render(c);
    }

    @Override
    public void dispose() {
        for (Pixmap p : cells) p.dispose();
        for (Pixmap p : strips) p.dispose();
        for (Texture t : tex) t.dispose();
    }
}
