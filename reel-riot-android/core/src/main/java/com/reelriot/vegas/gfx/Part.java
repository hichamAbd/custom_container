package com.reelriot.vegas.gfx;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * A node in a figure: a position and rotation that its children inherit,
 * and an optional mesh with its own size (size is not inherited, so a
 * squashed body does not squash the arm attached to it).
 */
public class Part {
    public ModelInstance inst;
    public final Vector3 pos = new Vector3();
    public final Quaternion rot = new Quaternion();
    public final Vector3 size = new Vector3(1, 1, 1);
    /** Extra per-frame offset and scale for animation, reset by the owner. */
    public final Vector3 offset = new Vector3();
    public float pulse = 1f;
    public boolean visible = true;
    public final Array<Part> kids = new Array<>();
    public final Matrix4 world = new Matrix4();
    /** Rest pose, so animations can add to it. */
    public final Vector3 restPos = new Vector3();
    public final Quaternion restRot = new Quaternion();
    public final Vector3 restSize = new Vector3(1, 1, 1);

    public Part() { }

    public Part(Model model, Material m) {
        inst = Shapes.get().make(model, m);
    }

    public Part at(float x, float y, float z) {
        pos.set(x, y, z);
        restPos.set(pos);
        return this;
    }

    public Part sized(float x, float y, float z) {
        size.set(x, y, z);
        restSize.set(size);
        return this;
    }

    public Part sized(float s) { return sized(s, s, s); }

    /** Rest rotation in degrees: yaw about Y, pitch about X, roll about Z. */
    public Part turned(float yaw, float pitch, float roll) {
        rot.setEulerAngles(yaw, pitch, roll);
        restRot.set(rot);
        return this;
    }

    public Part add(Part p) {
        kids.add(p);
        return p;
    }

    /** Back to the rest pose before applying this frame's animation. */
    public void reset() {
        pos.set(restPos);
        rot.set(restRot);
        size.set(restSize);
        offset.setZero();
        pulse = 1f;
        for (Part k : kids) k.reset();
    }

    private static final Quaternion tq = new Quaternion();

    /** Rotate on top of the rest rotation, in degrees about an axis. */
    public Part spin(float ax, float ay, float az, float deg) {
        tq.setFromAxis(ax, ay, az, deg);
        rot.mul(tq);
        return this;
    }

    public void update(Matrix4 parent) {
        world.set(parent).translate(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z).rotate(rot);
        if (inst != null) inst.transform.set(world).scale(size.x * pulse, size.y * pulse, size.z * pulse);
        for (Part k : kids) k.update(world);
    }

    public void render(ModelBatch batch) {
        if (!visible) return;
        if (inst != null) batch.render(inst);
        for (Part k : kids) k.render(batch);
    }

    /* builders */

    public static Part group() { return new Part(); }

    public static Part ball(Material m) { return new Part(Shapes.get().sphere(), m); }

    public static Part gem(Material m) { return new Part(Shapes.get().gemBall(), m); }

    public static Part cyl(Material m) { return new Part(Shapes.get().cylinder(), m); }

    public static Part cone(Material m) { return new Part(Shapes.get().cone(), m); }

    public static Part box(Material m) { return new Part(Shapes.get().box(), m); }

    public static Part ring(Material m, float tube) { return new Part(Shapes.get().torus(tube), m); }

    public static Part quad(Material m) { return new Part(Shapes.get().quad(), m); }
}
