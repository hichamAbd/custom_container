package com.reelriot.vegas.world;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Shapes;

/** Tumbling solid-gold 3D coins with simple physics. */
public class Coins3D {
    private final int n;
    private final ModelInstance[] inst;
    private final Vector3[] p, v, r, w;
    private final float[] life, size;
    public float gravity = -9f, floor = -20f;

    public Coins3D(int count) {
        n = count;
        inst = new ModelInstance[n];
        p = new Vector3[n]; v = new Vector3[n]; r = new Vector3[n]; w = new Vector3[n];
        life = new float[n];
        size = new float[n];
        for (int i = 0; i < n; i++) {
            inst[i] = Shapes.get().make(Shapes.get().cylinder(), Mat.gold());
            p[i] = new Vector3(); v[i] = new Vector3(); r[i] = new Vector3(); w[i] = new Vector3();
        }
    }

    private int free() {
        for (int i = 0; i < n; i++) if (life[i] <= 0) return i;
        return MathUtils.random(n - 1);
    }

    /** Throw coins out of a point. */
    public void burst(float x, float y, float z, int count, float speed, float scale) {
        for (int k = 0; k < count; k++) {
            int i = free();
            p[i].set(x + MathUtils.random(-0.2f, 0.2f), y, z);
            float a = MathUtils.random(MathUtils.PI2);
            float s = speed * MathUtils.random(0.5f, 1f);
            v[i].set(MathUtils.cos(a) * s * 0.6f, s * MathUtils.random(0.8f, 1.3f), MathUtils.sin(a) * s * 0.4f + speed * 0.25f);
            spinUp(i, scale);
        }
    }

    /** Rain coins down over an area. */
    public void rain(float x, float y, float z, float width, int count, float scale) {
        for (int k = 0; k < count; k++) {
            int i = free();
            p[i].set(x + MathUtils.random(-width / 2, width / 2), y + MathUtils.random(0f, 3f), z + MathUtils.random(-1f, 1f));
            v[i].set(MathUtils.random(-0.3f, 0.3f), MathUtils.random(-2f, 0f), 0);
            spinUp(i, scale);
        }
    }

    private void spinUp(int i, float scale) {
        r[i].set(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f));
        w[i].set(MathUtils.random(-720f, 720f), MathUtils.random(-360f, 360f), MathUtils.random(-720f, 720f));
        life[i] = 6;
        size[i] = scale * MathUtils.random(0.85f, 1.15f);
    }

    public boolean busy() {
        for (int i = 0; i < n; i++) if (life[i] > 0) return true;
        return false;
    }

    public void update(float dt) {
        for (int i = 0; i < n; i++) {
            if (life[i] <= 0) continue;
            life[i] -= dt;
            v[i].y += gravity * dt;
            p[i].mulAdd(v[i], dt);
            r[i].mulAdd(w[i], dt);
            if (p[i].y < floor) life[i] = 0;
            inst[i].transform.setToTranslation(p[i]).rotate(Vector3.X, r[i].x).rotate(Vector3.Y, r[i].y).rotate(Vector3.Z, r[i].z)
                    .scale(0.34f * size[i], 0.05f * size[i], 0.34f * size[i]);
        }
    }

    public void render(ModelBatch batch) {
        for (int i = 0; i < n; i++) if (life[i] > 0) batch.render(inst[i]);
    }
}
