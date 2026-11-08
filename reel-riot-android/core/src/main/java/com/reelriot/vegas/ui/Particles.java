package com.reelriot.vegas.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

/** 2D sparkles, confetti and spinning coins drawn over the scene. */
public final class Particles {
    public static final int SPARK = 0, CONFETTI = 1, COIN = 2, GLOW = 3;
    private static final int MAX = 700;
    private static final Color[] CONFETTI_COLORS = {
        new Color(1f, 0.82f, 0.29f, 1), new Color(1f, 0.25f, 0.45f, 1), new Color(0.35f, 0.7f, 1f, 1),
        new Color(0.4f, 1f, 0.6f, 1), new Color(0.8f, 0.45f, 1f, 1), new Color(1f, 1f, 1f, 1)
    };
    private final Ui ui;
    private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX];
    private final float[] life = new float[MAX], max = new float[MAX], size = new float[MAX], rot = new float[MAX], vr = new float[MAX], grav = new float[MAX];
    private final int[] kind = new int[MAX];
    private final Color[] color = new Color[MAX];
    private int count;

    Particles(Ui ui) {
        this.ui = ui;
        for (int i = 0; i < MAX; i++) color[i] = new Color();
    }

    private int slot() {
        if (count < MAX) return count++;
        return MathUtils.random(MAX - 1);
    }

    public void emit(int k, float px, float py, float pvx, float pvy, float s, float l, float g, Color c) {
        int i = slot();
        kind[i] = k;
        x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy;
        size[i] = s; life[i] = max[i] = l; grav[i] = g;
        rot[i] = MathUtils.random(360f);
        vr[i] = MathUtils.random(-500f, 500f);
        color[i].set(c);
    }

    /** Sparkles flying out of a point. */
    public void burst(float px, float py, int n, float speed, Color c) {
        for (int j = 0; j < n; j++) {
            float a = MathUtils.random(MathUtils.PI2), v = speed * MathUtils.random(0.3f, 1f);
            emit(SPARK, px, py, MathUtils.cos(a) * v, MathUtils.sin(a) * v, MathUtils.random(22f, 54f), MathUtils.random(0.5f, 1.1f), -300, c);
        }
    }

    public void confetti(float px, float py, int n, float speed) {
        for (int j = 0; j < n; j++) {
            float a = MathUtils.random(0.15f, MathUtils.PI - 0.15f), v = speed * MathUtils.random(0.4f, 1f);
            emit(CONFETTI, px, py, MathUtils.cos(a) * v, MathUtils.sin(a) * v, MathUtils.random(16f, 30f), MathUtils.random(1.8f, 3.2f), -900,
                    CONFETTI_COLORS[MathUtils.random(CONFETTI_COLORS.length - 1)]);
        }
    }

    /** Gold coins thrown up from a point. */
    public void coins(float px, float py, int n, float speed) {
        for (int j = 0; j < n; j++) {
            float a = MathUtils.random(0.35f, MathUtils.PI - 0.35f), v = speed * MathUtils.random(0.5f, 1f);
            emit(COIN, px, py, MathUtils.cos(a) * v, MathUtils.sin(a) * v, MathUtils.random(54f, 92f), 3f, -1800, Color.WHITE);
        }
    }

    /** Coins raining from the top of the screen. */
    public void rain(int n) {
        for (int j = 0; j < n; j++)
            emit(COIN, MathUtils.random(Ui.W), ui.H + MathUtils.random(40f, 300f), MathUtils.random(-60f, 60f), MathUtils.random(-500f, -200f),
                    MathUtils.random(50f, 86f), 4f, -900, Color.WHITE);
    }

    public void update(float dt) {
        for (int i = 0; i < count; i++) {
            life[i] -= dt;
            if (life[i] <= 0 || y[i] < -200) {
                count--;
                copy(count, i);
                i--;
                continue;
            }
            vy[i] += grav[i] * dt;
            if (kind[i] == CONFETTI) { vx[i] *= 0.985f; vy[i] = Math.max(vy[i], -380); }
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            rot[i] += vr[i] * dt;
        }
    }

    private void copy(int from, int to) {
        x[to] = x[from]; y[to] = y[from]; vx[to] = vx[from]; vy[to] = vy[from];
        life[to] = life[from]; max[to] = max[from]; size[to] = size[from]; rot[to] = rot[from];
        vr[to] = vr[from]; grav[to] = grav[from]; kind[to] = kind[from]; color[to].set(color[from]);
    }

    public void clear() { count = 0; }

    public void draw() {
        Texture coin = ui.image("symbols/coin.png");
        for (int i = 0; i < count; i++) {
            float k = life[i] / max[i];
            if (kind[i] == CONFETTI) {
                float flip = Math.abs(MathUtils.sinDeg(rot[i] * 1.7f));
                ui.batch.setColor(color[i].r, color[i].g, color[i].b, Math.min(1, k * 2));
                ui.batch.draw(ui.white, x[i] - size[i] / 2, y[i] - size[i] * 0.3f, size[i] / 2, size[i] * 0.3f, size[i], size[i] * 0.6f * flip + 2, 1, 1, rot[i], 0, 0, 4, 4, false, false);
            } else if (kind[i] == COIN) {
                float flip = MathUtils.cosDeg(rot[i]);
                ui.batch.setColor(1, 1, 1, Math.min(1, k * 3));
                float w = size[i] * Math.max(0.12f, Math.abs(flip));
                ui.batch.draw(coin, x[i] - w / 2, y[i] - size[i] / 2, w, size[i]);
            }
        }
        ui.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < count; i++) {
            float k = life[i] / max[i];
            if (kind[i] == SPARK) {
                ui.batch.setColor(color[i].r, color[i].g, color[i].b, k);
                float s = size[i] * (0.6f + 0.4f * k);
                ui.batch.draw(ui.star, x[i] - s / 2, y[i] - s / 2, s / 2, s / 2, s, s, 1, 1, rot[i] * 0.2f, 0, 0, ui.star.getWidth(), ui.star.getHeight(), false, false);
            } else if (kind[i] == GLOW) {
                ui.batch.setColor(color[i].r, color[i].g, color[i].b, k * 0.8f);
                float s = size[i] * (2 - k);
                ui.batch.draw(ui.glow, x[i] - s / 2, y[i] - s / 2, s, s);
            }
        }
        ui.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ui.batch.setColor(Color.WHITE);
    }
}
