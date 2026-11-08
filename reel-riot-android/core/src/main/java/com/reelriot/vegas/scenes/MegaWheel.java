package com.reelriot.vegas.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.gfx.Figure;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Part;
import com.reelriot.vegas.gfx.Shapes;
import com.reelriot.vegas.gfx.ToyShader;
import com.reelriot.vegas.slots.Machine;
import com.reelriot.vegas.slots.SlotEngine;
import com.reelriot.vegas.ui.Ui;
import com.reelriot.vegas.world.Coins3D;
import com.reelriot.vegas.world.Hosts;

/**
 * Bruno's Mega Wheel: three rings, one inside the other. Landing on
 * UPGRADE moves the spin to the next ring in, where the prizes are bigger;
 * the center holds the Major and the Grand.
 */
public class MegaWheel implements Overlay, MachineScene.Fullscreen {
    private static final float UP = -1;
    private static final float[][] RINGS = {
        { 5, 8, UP, 10, 15, 5, 20, UP, 8, 10, 5, 15, UP, 20, 8, 10 },
        { 25, 40, UP, 60, 25, 40, UP, 60, 25, 40 },
        { 100, 1000, 100, 100, 1000, 100 }
    };
    private static final float[][] RADII = { { 0.71f, 1.0f, 0.86f }, { 0.43f, 0.69f, 0.565f }, { 0.15f, 0.41f, 0.29f } };
    private static final Color[][] PALETTE = {
        { c(0.78f, 0.06f, 0.18f), c(0.36f, 0.12f, 0.66f), c(0.09f, 0.35f, 0.84f), c(0.04f, 0.54f, 0.29f) },
        { c(0.48f, 0.14f, 0.78f), c(0.78f, 0.06f, 0.18f), c(0.06f, 0.28f, 0.72f) },
        { c(0.04f, 0.54f, 0.29f), c(0.78f, 0.06f, 0.18f) }
    };

    private final ReelRiotGame game;
    private final MachineScene scene;
    private final SlotEngine.Prize prize;
    private final PerspectiveCamera cam = new PerspectiveCamera(40, 1, 1);
    private final Part wheel = Part.group(), tilt = Part.group();
    private final Part[] rings = new Part[3];
    private final Part[] pointers = new Part[3];
    private final float[] angle = new float[3], kick = new float[3], kickV = new float[3];
    private final Array<Part> bulbs = new Array<>();
    private final FrameBuffer fb;
    private final Figure bruno;
    private final Coins3D coins = new Coins3D(50);
    private final Matrix4 id = new Matrix4();
    private final int[][] path;
    private int step = -1, phase;
    private float t, spinT, spinDur, from, to, camDist, camY;
    private String banner;
    private float bannerT;
    private boolean done;
    private final Color C = new Color();

    private static Color c(float r, float g, float b) { return new Color(r, g, b, 1); }

    MegaWheel(ReelRiotGame game, MachineScene scene, SlotEngine.Prize prize) {
        this.game = game;
        this.scene = scene;
        this.prize = prize;
        fb = paint(scene.bet());
        Texture tex = fb.getColorBufferTexture();
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        wheel.add(Part.cyl(Mat.darkGold()).at(0, 0, -0.06f).turned(0, 90, 0).sized(2.28f, 0.1f, 2.28f));
        for (int r = 0; r < 3; r++) {
            rings[r] = wheel.add(Part.group());
            rings[r].add(new Part(Shapes.get().annulus(RADII[r][0], RADII[r][1]), Mat.textured(tex, 0.3f)));
            int n = RINGS[r].length;
            for (int i = 0; i < n; i++) {
                float a = (90 - i * 360f / n + 180f / n) * MathUtils.degRad;
                float rr = RADII[r][1] - 0.03f;
                rings[r].add(Part.cyl(Mat.gold()).at(MathUtils.cos(a) * rr, MathUtils.sin(a) * rr, 0.04f).turned(0, 90, 0).sized(0.035f, 0.08f, 0.035f));
            }
            angle[r] = MathUtils.random(MathUtils.PI2);
        }
        float[][] rims = { { 2.07f, 0.05f }, { 1.4f, 0.03f }, { 0.84f, 0.04f } };
        for (float[] rim : rims) wheel.add(Part.ring(Mat.gold(), rim[1] * 2 / rim[0] * 1.6f).at(0, 0, 0.02f).turned(0, 90, 0).sized(rim[0], rim[0], rim[0]));
        for (int i = 0; i < 36; i++) {
            float a = i / 36f * MathUtils.PI2;
            bulbs.add(wheel.add(Part.ball(Mat.lit(i % 2 == 0 ? c(1f, 0.85f, 0.5f) : c(1f, 0.3f, 0.45f), 1.4f)).at(MathUtils.cos(a) * 1.04f, MathUtils.sin(a) * 1.04f, 0.06f).sized(0.055f)));
        }
        wheel.add(Part.cyl(Mat.gold()).at(0, 0, 0.05f).turned(0, 90, 0).sized(0.3f, 0.1f, 0.3f));
        wheel.add(Part.gem(Mat.jewel(Mat.RUBY)).at(0, 0, 0.14f).sized(0.17f, 0.17f, 0.1f));
        for (int r = 0; r < 3; r++) {
            float big = r == 0 ? 1 : 0.6f;
            pointers[r] = wheel.add(Part.group().at(0, RADII[r][1] + (r == 0 ? 0.08f : 0.0f), 0.1f));
            pointers[r].add(Part.cone(r == 0 ? Mat.gold() : Mat.jewel(Mat.RUBY)).turned(0, 0, 180).sized(0.14f * big, 0.2f * big, 0.08f * big));
        }
        tilt.add(wheel);
        tilt.add(com.reelriot.vegas.world.Backdrop.make(game.ui.glow, new Color(0.22f, 0.05f, 0.4f, 1), new Color(0.8f, 0.45f, 1f, 1), -3.5f, 14, 16));
        bruno = Hosts.make("bruno");
        bruno.where.set(-0.62f, -1.95f, 0.5f);
        bruno.scale = 0.6f;
        bruno.facing = 18;
        bruno.set(Figure.Mood.TALK);
        coins.floor = -6;
        // the decided prize: UPGRADE on every ring before the prize's ring
        path = new int[prize.ring + 1][2];
        for (int r = 0; r <= prize.ring; r++) {
            IntArray hits = new IntArray();
            for (int i = 0; i < RINGS[r].length; i++) {
                float v = RINGS[r][i];
                if (r < prize.ring ? v == UP : v == prize.x) hits.add(i);
            }
            path[r][0] = r;
            path[r][1] = hits.size > 0 ? hits.get(MathUtils.random(hits.size - 1)) : 0;
        }
        ToyShader.Lighting L = ToyShader.LIGHT;
        L.keyDir.set(-0.4f, -0.7f, -0.6f).nor();
        L.pointPos[0].set(1.5f, -1, 2.5f);
        L.point[0].set(1f, 0.45f, 0.25f, 1);
        game.sfx.play("whoosh");
    }

    /** Paints all three rings, their prizes and dividers onto one texture. */
    private FrameBuffer paint(long bet) {
        int S = 1536;
        FrameBuffer f = new FrameBuffer(Pixmap.Format.RGBA8888, S, S, false);
        ShapeRenderer sr = new ShapeRenderer();
        sr.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, S, S));
        f.begin();
        Gdx.gl.glClearColor(0.07f, 0.01f, 0.04f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float cx = S / 2f, K = S / 2f;
        for (int r = 0; r < 3; r++) {
            float[] ring = RINGS[r];
            int n = ring.length;
            float step = 360f / n;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            int ci = 0;
            for (int i = 0; i < n; i++) {
                float mid = 90 - i * step;
                Color col = ring[i] == UP ? c(0.92f, 0.62f, 0.05f) : ring[i] >= 1000 ? c(0.95f, 0.72f, 0.1f) : PALETTE[r][ci++ % PALETTE[r].length];
                sr.setColor(col.r * 0.55f, col.g * 0.55f, col.b * 0.55f, 1);
                sr.arc(cx, cx, RADII[r][1] * K, mid - step / 2, step, 24);
                sr.setColor(col);
                sr.arc(cx, cx, (RADII[r][1] - 0.012f) * K, mid - step / 2 + 0.6f, step - 1.2f, 24);
            }
            sr.setColor(0.07f, 0.01f, 0.04f, 1);
            sr.circle(cx, cx, RADII[r][0] * K, 96);
            sr.end();
        }
        sr.dispose();
        BitmapFont font = game.ui.font(Ui.DISPLAY, Math.round(58 / game.ui.px));
        float old = font.getData().scaleX;
        GlyphLayout gl = new GlyphLayout();
        com.badlogic.gdx.graphics.g2d.SpriteBatch b = game.ui.batch;
        Matrix4 oldProj = b.getProjectionMatrix().cpy(), oldTrans = b.getTransformMatrix().cpy();
        b.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, S, S));
        b.begin();
        for (int r = 0; r < 3; r++) {
            float[] ring = RINGS[r];
            int n = ring.length;
            float step = 360f / n;
            for (int i = 0; i < n; i++) {
                float mid = 90 - i * step;
                String label = ring[i] == UP ? "UPGRADE" : ring[i] >= 1000 ? "GRAND" : ring[i] >= 100 ? "MAJOR" : ring[i] == 25 && r == 1 ? "MINOR" : Ui.shortFmt((long) (bet * ring[i]));
                float scale = (r == 0 ? 1f : 0.9f) * (label.length() > 5 ? 0.72f : 1f);
                font.getData().setScale(scale);
                gl.setText(font, label);
                float rr = RADII[r][2] * K;
                b.setTransformMatrix(new Matrix4().translate(cx + MathUtils.cosDeg(mid) * rr, cx + MathUtils.sinDeg(mid) * rr, 0).rotate(0, 0, 1, mid - 90));
                font.setColor(0.15f, 0.03f, 0f, 0.85f);
                font.draw(b, label, -gl.width / 2 + 3, gl.height / 2 - 3);
                font.setColor(ring[i] == UP ? c(1f, 0.97f, 0.8f) : Color.WHITE);
                font.draw(b, label, -gl.width / 2, gl.height / 2);
            }
        }
        b.end();
        b.setTransformMatrix(oldTrans);
        b.setProjectionMatrix(oldProj);
        font.getData().setScale(old);
        font.setColor(Color.WHITE);
        f.end();
        return f;
    }

    private void startSpin() {
        step++;
        int r = path[step][0], idx = path[step][1];
        int n = RINGS[r].length;
        float stepRad = MathUtils.PI2 / n;
        float base = idx * stepRad + MathUtils.random(-0.3f, 0.3f) * stepRad;
        from = angle[r];
        float turns = 5 - r;
        float k = MathUtils.ceil((base - from + turns * MathUtils.PI2) / MathUtils.PI2);
        to = base - k * MathUtils.PI2;
        spinT = 0;
        spinDur = 4.6f - r * 0.6f;
        phase = 2;
        banner = null;
        bruno.set(Figure.Mood.WOW);
        game.sfx.play("spin", 0.6f, 0.8f);
    }

    @Override
    public void update(float dt) {
        t += dt;
        bannerT -= dt;
        int focus = step < 0 ? 0 : path[Math.min(step, path.length - 1)][0];
        float aspect = Math.max(0.3f, game.ui.W / game.ui.H);
        cam.viewportWidth = 1;
        cam.viewportHeight = 1 / aspect;
        float[] view = { 1.3f, 0.86f, 0.58f };
        float want = view[focus] / (MathUtils.tanDeg(cam.fieldOfView / 2) * Math.min(aspect, 1f)) + 0.4f;
        if (camDist == 0) camDist = want;
        camDist += (want - camDist) * Math.min(1, dt * 2.5f);
        camY += ((focus == 0 ? -0.25f : 0.05f + focus * 0.12f) - camY) * Math.min(1, dt * 2.5f);
        cam.position.set(0, camY, camDist);
        cam.lookAt(0, camY * 0.5f, 0);
        cam.near = 0.3f;
        cam.far = 50;
        cam.update();
        if (phase == 0 && t > 1.0f) phase = 1;
        if (phase == 2) {
            int r = path[step][0];
            spinT += dt;
            float u = Math.min(1, spinT / spinDur);
            float e = 1 - (float) Math.pow(1 - u, 4);
            float prev = angle[r];
            angle[r] = from + (to - from) * e;
            float stepRad = MathUtils.PI2 / RINGS[r].length;
            if (MathUtils.floor(prev / stepRad) != MathUtils.floor(angle[r] / stepRad)) {
                kickV[r] += 9;
                game.sfx.play("tick", 0.6f, 1 + r * 0.15f);
            }
            if (u >= 1) landed(r);
        }
        if (phase == 4 && t > 3f) done = true;
        tilt.reset();
        tilt.turned(0, -14, 0);
        wheel.reset();
        for (int r = 0; r < 3; r++) {
            rings[r].rot.setFromAxisRad(0, 0, 1, angle[r]);
            kickV[r] += (-kick[r] * 70 - kickV[r] * 9) * dt;
            kick[r] += kickV[r] * dt;
            pointers[r].spin(0, 0, 1, kick[r] * 30);
            pointers[r].visible = r == focus || phase == 0 || phase == 1;
        }
        for (int i = 0; i < bulbs.size; i++) {
            float k = 0.5f + 0.5f * MathUtils.sin(t * 9 + i * 1.7f);
            Mat.emissive(bulbs.get(i).inst, 0.6f + 1.4f * k, (i % 2 == 0 ? 0.5f : 0.15f) + k, 0.3f + 0.5f * k);
        }
        tilt.update(id);
        bruno.update(dt);
        coins.update(dt);
    }

    private void landed(int r) {
        game.sfx.play("stop");
        game.sfx.buzz(40);
        if (step < path.length - 1) {
            banner = "UPGRADE!";
            bannerT = 1.4f;
            game.sfx.play("upgrade");
            game.ui.fx.burst(Ui.W / 2, game.ui.H * 0.6f, 40, 800, new Color(1f, 0.8f, 0.3f, 1));
            phase = 3;
            t = 0;
        } else {
            phase = 4;
            t = 0;
            banner = prize.jackpot >= 0 ? Machine.JACKPOT_NAMES[prize.jackpot] + "!" : Ui.shortFmt(Math.round(scene.bet() * prize.x)) + "!";
            bannerT = 99;
            game.sfx.play("jackpot");
            bruno.set(Figure.Mood.CHEER);
            coins.burst(0, 0.2f, 0.8f, 40, 6, 0.5f);
            game.ui.fx.confetti(Ui.W / 2, game.ui.H * 0.45f, 90, 1700);
        }
    }

    @Override
    public PerspectiveCamera cam() { return cam; }

    @Override
    public void render3d(ModelBatch batch) {
        tilt.render(batch);
        bruno.render(batch);
        coins.render(batch);
    }

    @Override
    public void renderUi() {
        Ui ui = game.ui;
        if (phase == 3 && t > 1.0f) startSpin();
        ui.gold("MEGA WHEEL", Ui.W / 2, ui.H - 150, 112, Align.center, Math.min(1, t * 3 + (phase > 0 ? 1 : 0)));
        if (phase <= 1) {
            ui.shadowed(Ui.BOLD, 44, "Land on UPGRADE to move in to bigger prizes", Ui.W / 2, ui.H - 260, Ui.CREAM, Align.center);
            if (phase == 1) ui.shadowed(Ui.DISPLAY, 52, "Tap to spin!", Ui.W / 2, 220, new Color(1f, 0.95f, 0.75f, 0.6f + 0.4f * MathUtils.sin(t * 6)), Align.center);
        }
        if (banner != null && bannerT > 0) {
            float k = Math.min(1, (phase == 4 ? t : 1.4f - bannerT) * 5);
            ui.panel(Ui.W / 2 - 330, ui.H * 0.5f - 80, 660, 160, C.set(0.08f, 0.01f, 0.04f, 0.88f * k));
            ui.gold(banner, Ui.W / 2, ui.H * 0.5f, 110, Align.center, k);
        }
    }

    @Override
    public boolean touchDown(float x, float y) {
        if (phase == 1) startSpin();
        else if (phase == 4 && t > 1.2f) done = true;
        return true;
    }

    @Override
    public boolean finished() {
        if (done) fb.dispose();
        return done;
    }
}
