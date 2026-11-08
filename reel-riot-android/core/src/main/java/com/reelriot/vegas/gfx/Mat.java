package com.reelriot.vegas.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

/** The material palette of the Vegas Gold look. */
public final class Mat {
    private Mat() { }

    /** Draw without lighting (glows, screens, text). */
    public static final long Unlit = ToyAttr.UNLIT;

    public static final Color GOLD = new Color(1.0f, 0.8f, 0.4f, 1);
    public static final Color RUBY = new Color(0.86f, 0.06f, 0.18f, 1);
    public static final Color WINE = new Color(0.35f, 0.03f, 0.1f, 1);
    public static final Color ROYAL = new Color(0.38f, 0.14f, 0.72f, 1);
    public static final Color SAPPHIRE = new Color(0.1f, 0.36f, 0.86f, 1);
    public static final Color EMERALD = new Color(0.05f, 0.56f, 0.3f, 1);
    public static final Color CREAM = new Color(1.0f, 0.95f, 0.85f, 1);

    public static Material make(Color c, float metal, float rough) {
        return new Material(ColorAttribute.createDiffuse(c), new FloatAttribute(ToyShader.Metal, metal), new FloatAttribute(ToyShader.Rough, rough));
    }

    public static Material gold() { return make(GOLD, 1f, 0.2f); }
    public static Material darkGold() { return make(new Color(0.78f, 0.5f, 0.16f, 1), 1f, 0.34f); }
    public static Material chrome() { return make(new Color(0.85f, 0.88f, 0.92f, 1), 1f, 0.12f); }
    public static Material lacquer(Color c) { return make(c, 0f, 0.1f); }
    public static Material glossy(Color c) { return make(c, 0f, 0.28f); }
    public static Material satin(Color c) { return make(c, 0f, 0.55f); }
    public static Material velvet(Color c) { return make(c, 0f, 0.95f); }
    public static Material jewel(Color c) {
        Material m = make(c, 0.35f, 0.04f);
        m.set(ColorAttribute.createEmissive(c.r * 0.25f, c.g * 0.25f, c.b * 0.25f, 1));
        return m;
    }

    /** A self-lit surface, like a bulb or a glowing screen. */
    public static Material lit(Color c, float strength) {
        Material m = make(c, 0f, 0.3f);
        m.set(ColorAttribute.createEmissive(c.r * strength, c.g * strength, c.b * strength, 1));
        return m;
    }

    /** Additive glow sprite or halo, seen from both sides. */
    public static Material glow(Color c, float opacity) {
        Material m = new Material(ColorAttribute.createDiffuse(c), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, opacity),
                IntAttribute.createCullFace(GL20.GL_NONE), new IntAttribute(Unlit, 1));
        return m;
    }

    /** An additive glow sprite using a soft round texture (light bokeh, halos). */
    public static Material glowSprite(Texture t, Color c, float opacity) {
        Material m = new Material(ColorAttribute.createDiffuse(c), TextureAttribute.createDiffuse(t),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, opacity), IntAttribute.createCullFace(GL20.GL_NONE), new IntAttribute(Unlit, 1));
        return m;
    }

    public static Material textured(Texture t, float rough) {
        Material m = make(Color.WHITE, 0f, rough);
        m.set(TextureAttribute.createDiffuse(t));
        return m;
    }

    /** A flat textured card (text, signs, sprites) with alpha blending. */
    public static Material decal(Texture t, boolean unlit) {
        Material m = make(Color.WHITE, 0f, 0.4f);
        m.set(TextureAttribute.createDiffuse(t));
        m.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f));
        m.set(IntAttribute.createCullFace(GL20.GL_NONE));
        if (unlit) m.set(new IntAttribute(Unlit, 1));
        return m;
    }

    /** Replace an instance's material (instances own copies, so this is safe). */
    public static ModelInstance apply(ModelInstance inst, Material m) {
        Material target = inst.materials.get(0);
        target.clear();
        target.set(m);
        return inst;
    }

    /** Fade a blended material. */
    public static void opacity(ModelInstance inst, float a) {
        BlendingAttribute b = (BlendingAttribute) inst.materials.get(0).get(BlendingAttribute.Type);
        if (b != null) b.opacity = a;
    }

    public static void tint(ModelInstance inst, Color c) {
        ColorAttribute d = (ColorAttribute) inst.materials.get(0).get(ColorAttribute.Diffuse);
        if (d != null) d.color.set(c);
    }

    public static void emissive(ModelInstance inst, float r, float g, float b) {
        Material m = inst.materials.get(0);
        ColorAttribute e = (ColorAttribute) m.get(ColorAttribute.Emissive);
        if (e == null) m.set(ColorAttribute.createEmissive(r, g, b, 1));
        else e.color.set(r, g, b, 1);
    }
}
