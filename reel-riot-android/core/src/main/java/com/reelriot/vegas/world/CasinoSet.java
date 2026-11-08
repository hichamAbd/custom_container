package com.reelriot.vegas.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Part;
import com.reelriot.vegas.gfx.Shapes;

/**
 * The casino stage: glossy wine floor with gold inlays, a round stage, a
 * velvet curtain, gold pillars, light beams, a string of bulbs and bokeh.
 */
public class CasinoSet {
    public final Part root = Part.group();
    public final Part curtainL, curtainR, stage;
    private final Array<Part> bulbs = new Array<>();
    private final Array<Part> beams = new Array<>();
    private final Matrix4 id = new Matrix4();
    private float t;
    /** 0 = closed, 1 = fully open. */
    public float open = 1f;

    public CasinoSet(Texture glowTex) {
        root.add(Part.cyl(Mat.lacquer(new Color(0.13f, 0.012f, 0.045f, 1))).at(0, -0.06f, 0).sized(16, 0.1f, 16));
        root.add(Part.ring(Mat.gold(), 0.012f).at(0, 0, 0).sized(7.4f, 1, 7.4f));
        root.add(Part.ring(Mat.gold(), 0.016f).at(0, 0, 0).sized(4.6f, 1, 4.6f));
        stage = root.add(Part.cyl(Mat.lacquer(new Color(0.42f, 0.03f, 0.1f, 1))).at(0, 0.12f, 0).sized(3.4f, 0.24f, 3.4f));
        root.add(Part.ring(Mat.gold(), 0.05f).at(0, 0.24f, 0).sized(3.42f, 1.4f, 3.42f));
        root.add(Part.cyl(Mat.make(new Color(0.55f, 0.04f, 0.12f, 1), 0f, 0.92f)).at(0, 4, -4.2f).turned(0, 90, 0).sized(14, 0.1f, 8.4f));
        Color red = new Color(0.62f, 0.03f, 0.1f, 1);
        curtainL = root.add(Part.group().at(-3.2f, 4, -3.6f));
        curtainL.add(new Part(Shapes.get().curtain(), Mat.velvet(red)).sized(6.6f, 8.2f, 6.6f));
        curtainR = root.add(Part.group().at(3.2f, 4, -3.6f));
        curtainR.add(new Part(Shapes.get().curtain(), Mat.velvet(red)).sized(6.6f, 8.2f, 6.6f));
        root.add(Part.box(Mat.velvet(new Color(0.5f, 0.03f, 0.09f, 1))).at(0, 7.6f, -3.4f).sized(14, 1.0f, 0.3f));
        root.add(Part.box(Mat.gold()).at(0, 7.05f, -3.22f).sized(14, 0.12f, 0.12f));
        for (int s = -1; s <= 1; s += 2) {
            root.add(Part.cyl(Mat.gold()).at(s * 4.3f, 3.6f, -3.0f).sized(0.55f, 7.2f, 0.55f));
            root.add(Part.box(Mat.darkGold()).at(s * 4.3f, 0.15f, -3.0f).sized(0.9f, 0.3f, 0.9f));
            root.add(Part.box(Mat.darkGold()).at(s * 4.3f, 7.25f, -3.0f).sized(0.9f, 0.3f, 0.9f));
            for (int k = 0; k < 6; k++) root.add(Part.ring(Mat.darkGold(), 0.25f).at(s * 4.3f, 0.9f + k * 1.15f, -3.0f).sized(0.62f, 0.5f, 0.62f));
        }
        for (int i = 0; i < 26; i++) {
            float a = MathUtils.PI * (0.08f + 0.84f * i / 25f);
            Part b = root.add(Part.ball(Mat.lit(i % 2 == 0 ? new Color(1f, 0.85f, 0.5f, 1) : new Color(1f, 0.3f, 0.45f, 1), 1.5f))
                    .at(MathUtils.cos(a) * 4.1f, 3.4f + MathUtils.sin(a) * 3.3f, -3.0f).sized(0.13f));
            bulbs.add(b);
        }
        Color[] beamCols = { new Color(1f, 0.85f, 0.55f, 1), new Color(1f, 0.45f, 0.75f, 1), new Color(1f, 0.85f, 0.55f, 1) };
        float[] bx = { -2.4f, 0f, 2.4f };
        for (int i = 0; i < 3; i++) {
            Part beam = root.add(Part.group().at(bx[i], 8.2f, 0.6f).turned(0, 0, -bx[i] * 7));
            beam.add(Part.cone(Mat.glow(beamCols[i], 0.07f)).at(0, -3.8f, 0).turned(0, 180, 0).sized(2.4f, 7.6f, 2.4f));
            beams.add(beam);
        }
        Color[] bok = { new Color(1f, 0.6f, 0.2f, 1), new Color(1f, 0.25f, 0.55f, 1), new Color(0.5f, 0.3f, 1f, 1), new Color(1f, 0.85f, 0.45f, 1) };
        for (int i = 0; i < 14; i++) {
            float x = MathUtils.random(-7f, 7f), y = MathUtils.random(1f, 7.5f);
            root.add(Part.quad(Mat.glowSprite(glowTex, bok[i % bok.length], MathUtils.random(0.12f, 0.3f))).at(x, y, -3.9f).sized(MathUtils.random(0.6f, 1.6f)));
        }
    }

    /** Move the whole set (for scenes that put it behind something). */
    public CasinoSet place(float x, float y, float z) {
        root.at(x, y, z);
        return this;
    }

    public void update(float dt) {
        t += dt;
        root.reset();
        for (int i = 0; i < bulbs.size; i++) {
            float k = 0.5f + 0.5f * MathUtils.sin(t * 6 + i * 0.9f);
            bulbs.get(i).pulse = 0.85f + 0.3f * k;
            Mat.emissive(bulbs.get(i).inst, 0.6f + 1.4f * k, (i % 2 == 0 ? 0.5f : 0.15f) + 1.0f * k, (i % 2 == 0 ? 0.2f : 0.3f) + 0.5f * k);
        }
        for (int i = 0; i < beams.size; i++) beams.get(i).spin(0, 0, 1, MathUtils.sin(t * 0.6f + i * 2) * 6);
        float slide = (1 - open) * 3.3f;
        curtainL.offset.x = slide;
        curtainR.offset.x = -slide;
        root.update(id);
    }

    public void render(ModelBatch batch) { root.render(batch); }
}
