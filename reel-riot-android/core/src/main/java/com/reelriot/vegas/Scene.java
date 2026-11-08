package com.reelriot.vegas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

/**
 * One screen of the game: a 3D world drawn with the ToyShader, then a 2D
 * layer of text and buttons on top in "UI units" (1080 wide).
 */
public abstract class Scene {
    protected final ReelRiotGame game;
    protected final PerspectiveCamera cam;
    protected float time;

    protected Scene(ReelRiotGame game) {
        this.game = game;
        cam = new PerspectiveCamera(40, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.05f;
        cam.far = 80f;
    }

    /** Called when the scene becomes active. */
    public void enter() { }

    public void update(float dt) { time += dt; }

    /** Draw the 3D world (the batch is begun with this scene's camera). */
    public abstract void render3d(ModelBatch batch);

    /** Draw the 2D layer with game.ui (already begun). */
    public void renderUi() { }

    public boolean touchDown(float x, float y) { return false; }

    public boolean touchDragged(float x, float y) { return false; }

    public boolean touchUp(float x, float y) { return false; }

    /** Android back button / Escape. Return true when handled. */
    public boolean back() { return false; }

    public void resize(int w, int h) {
        cam.viewportWidth = w;
        cam.viewportHeight = h;
        cam.update();
    }

    public void dispose() { }
}
