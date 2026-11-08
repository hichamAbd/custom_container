package com.reelriot.vegas.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Part;
import com.reelriot.vegas.gfx.Shapes;

/** A velvet wall with soft bokeh lights, for bonus-game worlds. */
public final class Backdrop {
    private Backdrop() { }

    public static Part make(Texture glow, Color wall, Color light, float z, float width, float height) {
        Part p = Part.group();
        p.add(new Part(Shapes.get().curtain(), Mat.velvet(wall)).at(0, 0, z).sized(width, height, width));
        Color[] cols = { light, new Color(1f, 0.85f, 0.45f, 1), new Color(1f, 0.35f, 0.6f, 1) };
        for (int i = 0; i < 16; i++) {
            float x = MathUtils.random(-width / 2, width / 2), y = MathUtils.random(-height / 2, height / 2);
            p.add(Part.quad(Mat.glowSprite(glow, cols[i % 3], MathUtils.random(0.12f, 0.32f))).at(x, y, z + 0.4f).sized(MathUtils.random(0.8f, 2.4f)));
        }
        p.add(Part.quad(Mat.glowSprite(glow, light, 0.35f)).at(0, height * 0.1f, z + 0.3f).sized(width * 0.9f, height * 0.7f, 1));
        return p;
    }
}
