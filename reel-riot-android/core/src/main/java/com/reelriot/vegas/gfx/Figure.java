package com.reelriot.vegas.gfx;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

/**
 * An animated character built from parts. Subclasses build the body and
 * pose it every frame for the current mood.
 */
public abstract class Figure {
    public enum Mood { IDLE, TALK, WAVE, CHEER, WOW, SAD }

    public final Part root = Part.group();
    public final Vector3 where = new Vector3();
    public float facing;
    public float scale = 1f;
    /** Extra whole-body lift (jumps, entrances). */
    public float lift;
    protected float t;
    protected Mood mood = Mood.IDLE;
    private Mood after = Mood.IDLE;
    private float moodLeft = -1;
    protected float moodT;
    private final Matrix4 base = new Matrix4();

    public Mood mood() { return mood; }

    /** Hold a mood until changed. */
    public void set(Mood m) {
        if (m != mood) moodT = 0;
        mood = m;
        after = m;
        moodLeft = -1;
    }

    /** Play a mood for a while, then go back to the held one. */
    public void react(Mood m, float seconds) {
        if (m != mood) moodT = 0;
        mood = m;
        moodLeft = seconds;
    }

    public void update(float dt) {
        t += dt;
        moodT += dt;
        if (moodLeft > 0 && (moodLeft -= dt) <= 0) { mood = after; moodT = 0; moodLeft = -1; }
        root.reset();
        pose(t);
        base.setToTranslation(where.x, where.y + lift, where.z).rotate(Vector3.Y, facing).scale(scale, scale, scale);
        root.update(base);
    }

    protected abstract void pose(float t);

    public void render(ModelBatch batch) { root.render(batch); }

    /* animation helpers */

    protected static float wave(float t, float speed) { return MathUtils.sin(t * speed); }

    /** 0..1 blink amount: closed briefly every few seconds. */
    protected static float blink(float t, float every, float phase) {
        float k = (t + phase) % every;
        return k < 0.12f ? 1 - Math.abs(k - 0.06f) / 0.06f : 0f;
    }

    protected static float hop(float t, float speed, float height) { return Math.abs(MathUtils.sin(t * speed)) * height; }
}
