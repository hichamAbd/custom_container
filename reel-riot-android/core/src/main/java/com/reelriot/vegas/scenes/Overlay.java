package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

/** Something played on top of a scene: a bonus game, a celebration, a menu. */
public interface Overlay {
    void update(float dt);

    /** A camera for this overlay's own 3D world, or null to keep the scene's view. */
    PerspectiveCamera cam();

    void render3d(ModelBatch batch);

    void renderUi();

    boolean touchDown(float x, float y);

    boolean finished();
}
