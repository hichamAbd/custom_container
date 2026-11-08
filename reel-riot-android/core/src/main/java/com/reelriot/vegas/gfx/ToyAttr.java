package com.reelriot.vegas.gfx;

import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;

/** Registers the ToyShader's own material attribute types (libGDX keeps register() protected). */
public final class ToyAttr extends FloatAttribute {
    public static final long METAL = register("metal");
    public static final long ROUGH = register("rough");
    public static final long UNLIT = register("unlit");

    private ToyAttr() { super(METAL); }
}
