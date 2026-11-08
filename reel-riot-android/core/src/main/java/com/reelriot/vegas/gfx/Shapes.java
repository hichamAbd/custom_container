package com.reelriot.vegas.gfx;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Shared unit-sized meshes (sphere, cylinder, cone, box, torus, quad, drum).
 * Every figure, set piece and coin is built from these, scaled and colored.
 */
public final class Shapes implements Disposable {
    public static final long ATTR = Usage.Position | Usage.Normal | Usage.TextureCoordinates;
    private static Shapes inst;
    private final ModelBuilder mb = new ModelBuilder();
    private final ObjectMap<String, Model> cache = new ObjectMap<>();
    private final MeshPartBuilder.VertexInfo vi = new MeshPartBuilder.VertexInfo();

    public static Shapes get() {
        if (inst == null) inst = new Shapes();
        return inst;
    }

    private Model cached(String key) { return cache.get(key); }

    private Model keep(String key, Model m) { cache.put(key, m); return m; }

    private static Material blank() { return Mat.gold(); }

    /** Sphere of diameter 1. */
    public Model sphere() {
        Model m = cached("sphere");
        return m != null ? m : keep("sphere", mb.createSphere(1, 1, 1, 40, 28, blank(), ATTR));
    }

    /** Low-poly sphere for gems and small parts. */
    public Model gemBall() {
        Model m = cached("gem");
        return m != null ? m : keep("gem", mb.createSphere(1, 1, 1, 8, 6, blank(), ATTR));
    }

    /** Cylinder of diameter 1 and height 1 along Y. */
    public Model cylinder() {
        Model m = cached("cyl");
        return m != null ? m : keep("cyl", mb.createCylinder(1, 1, 1, 40, blank(), ATTR));
    }

    /** Cone of diameter 1 and height 1 along Y, tip up. */
    public Model cone() {
        Model m = cached("cone");
        return m != null ? m : keep("cone", mb.createCone(1, 1, 1, 32, blank(), ATTR));
    }

    public Model box() {
        Model m = cached("box");
        return m != null ? m : keep("box", mb.createBox(1, 1, 1, blank(), ATTR));
    }

    /** Torus in the XZ plane: ring radius 0.5, tube radius 0.5 * tube. */
    public Model torus(float tube) {
        String key = "torus" + tube;
        Model m = cached(key);
        if (m != null) return m;
        mb.begin();
        MeshPartBuilder b = mb.part("torus", GL20.GL_TRIANGLES, ATTR, blank());
        int seg = 48, side = 16;
        float R = 0.5f, r = 0.5f * tube;
        for (int i = 0; i <= seg; i++) {
            float u = i / (float) seg * MathUtils.PI2;
            float cu = MathUtils.cos(u), su = MathUtils.sin(u);
            for (int j = 0; j <= side; j++) {
                float v = j / (float) side * MathUtils.PI2;
                float cv = MathUtils.cos(v), sv = MathUtils.sin(v);
                b.vertex(vi.setPos((R + r * cv) * cu, r * sv, (R + r * cv) * su).setNor(cv * cu, sv, cv * su).setUV(i / (float) seg, j / (float) side));
            }
        }
        for (int i = 0; i < seg; i++) {
            for (int j = 0; j < side; j++) {
                short a = (short) (i * (side + 1) + j), c = (short) ((i + 1) * (side + 1) + j);
                b.triangle(a, (short) (a + 1), c);
                b.triangle(c, (short) (a + 1), (short) (c + 1));
            }
        }
        return keep(key, mb.end());
    }

    /** Unit quad in the XY plane facing +Z, UVs with v = 0 at the top. */
    public Model quad() {
        Model m = cached("quad");
        if (m != null) return m;
        mb.begin();
        MeshPartBuilder b = mb.part("quad", GL20.GL_TRIANGLES, ATTR, blank());
        short p0 = b.vertex(vi.setPos(-0.5f, -0.5f, 0).setNor(0, 0, 1).setUV(0, 1));
        short p1 = b.vertex(vi.setPos(0.5f, -0.5f, 0).setNor(0, 0, 1).setUV(1, 1));
        short p2 = b.vertex(vi.setPos(0.5f, 0.5f, 0).setNor(0, 0, 1).setUV(1, 0));
        short p3 = b.vertex(vi.setPos(-0.5f, 0.5f, 0).setNor(0, 0, 1).setUV(0, 0));
        b.triangle(p0, p1, p2);
        b.triangle(p0, p2, p3);
        return keep("quad", mb.end());
    }

    /**
     * A reel drum: a cylinder along X of radius 0.5 and width 1, its outside
     * mapped so the texture's height wraps once around (top of the texture
     * faces the viewer at angle 0 and runs downward as the drum turns).
     */
    public Model drum() {
        Model m = cached("drum");
        if (m != null) return m;
        mb.begin();
        MeshPartBuilder b = mb.part("drum", GL20.GL_TRIANGLES, ATTR, blank());
        int seg = 160;
        for (int i = 0; i <= seg; i++) {
            float t = i / (float) seg;
            float a = t * MathUtils.PI2;
            // t = 0 faces the viewer (+Z); increasing t runs down the front face.
            // Rotating the drum by +phi about X moves its surface downward.
            float yy = -MathUtils.sin(a) * 0.5f, zz = MathUtils.cos(a) * 0.5f;
            float nyy = -MathUtils.sin(a), nzz = MathUtils.cos(a);
            b.vertex(vi.setPos(-0.5f, yy, zz).setNor(0, nyy, nzz).setUV(0, t));
            b.vertex(vi.setPos(0.5f, yy, zz).setNor(0, nyy, nzz).setUV(1, t));
        }
        for (int i = 0; i < seg; i++) {
            short a = (short) (i * 2), c = (short) (i * 2 + 2);
            b.triangle(a, c, (short) (a + 1));
            b.triangle((short) (a + 1), c, (short) (c + 1));
        }
        return keep("drum", mb.end());
    }

    /** A hanging velvet curtain: 1 x 1 in XY with deep vertical folds. */
    public Model curtain() {
        Model m = cached("curtain");
        if (m != null) return m;
        mb.begin();
        MeshPartBuilder b = mb.part("curtain", GL20.GL_TRIANGLES, ATTR, blank());
        int nx = 160, ny = 8;
        float folds = 14, depth = 0.025f;
        for (int j = 0; j <= ny; j++) {
            for (int i = 0; i <= nx; i++) {
                float u = i / (float) nx, v = j / (float) ny;
                float a = u * folds * MathUtils.PI2;
                float z = MathUtils.sin(a) * depth * (0.7f + 0.3f * v);
                float dz = MathUtils.cos(a) * depth * folds * MathUtils.PI2;
                float len = (float) Math.sqrt(dz * dz + 1);
                b.vertex(vi.setPos(u - 0.5f, v - 0.5f, z).setNor(-dz / len, 0, 1 / len).setUV(u, 1 - v));
            }
        }
        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                short a0 = (short) (j * (nx + 1) + i), a1 = (short) (a0 + 1), c0 = (short) (a0 + nx + 1), c1 = (short) (c0 + 1);
                b.triangle(a0, a1, c1);
                b.triangle(a0, c1, c0);
            }
        }
        return keep("curtain", mb.end());
    }

    /**
     * A flat ring in the XY plane facing +Z, from radius r0 to r1, with
     * planar UVs over a disc of radius 1 (so several rings share one texture).
     */
    public Model annulus(float r0, float r1) {
        String key = "annulus" + r0 + "_" + r1;
        Model m = cached(key);
        if (m != null) return m;
        mb.begin();
        MeshPartBuilder b = mb.part("annulus", GL20.GL_TRIANGLES, ATTR, blank());
        int seg = 128;
        for (int i = 0; i <= seg; i++) {
            float a = i / (float) seg * MathUtils.PI2, c = MathUtils.cos(a), s = MathUtils.sin(a);
            b.vertex(vi.setPos(c * r0, s * r0, 0).setNor(0, 0, 1).setUV(c * r0 * 0.5f + 0.5f, s * r0 * 0.5f + 0.5f));
            b.vertex(vi.setPos(c * r1, s * r1, 0).setNor(0, 0, 1).setUV(c * r1 * 0.5f + 0.5f, s * r1 * 0.5f + 0.5f));
        }
        for (int i = 0; i < seg; i++) {
            short a0 = (short) (i * 2), a1 = (short) (a0 + 1), c0 = (short) (a0 + 2), c1 = (short) (a0 + 3);
            b.triangle(a0, a1, c0);
            b.triangle(a1, c1, c0);
        }
        return keep(key, mb.end());
    }

    public ModelInstance make(Model model, Material m) {
        return Mat.apply(new ModelInstance(model), m);
    }

    @Override
    public void dispose() {
        for (Model m : cache.values()) m.dispose();
        cache.clear();
        inst = null;
    }
}
