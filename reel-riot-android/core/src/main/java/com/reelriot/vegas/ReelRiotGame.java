package com.reelriot.vegas;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.reelriot.vegas.gfx.Shapes;
import com.reelriot.vegas.gfx.ToyShader;
import com.reelriot.vegas.scenes.SceneFactory;
import com.reelriot.vegas.ui.Ui;

/** The app: owns the shared renderers, sound and save file, and switches scenes with a fade. */
public class ReelRiotGame extends ApplicationAdapter {
    public final TestDriver driver;
    public Ui ui;
    public ModelBatch batch;
    public Save save;
    public Sfx sfx;
    private Scene scene, pending;
    private float fade = 1f, fadeDir = -1f;
    private final Color tmp = new Color();

    public ReelRiotGame(TestDriver driver) { this.driver = driver; }

    @Override
    public void create() {
        Shapes.get();
        batch = new ModelBatch(new ToyShader.Provider());
        ui = new Ui();
        save = new Save(driver != null);
        sfx = new Sfx(save, driver != null);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                return pointer == 0 && scene != null && fade <= 0.01f && scene.touchDown(ui.toX(x), ui.toY(y));
            }

            @Override
            public boolean touchDragged(int x, int y, int pointer) {
                return pointer == 0 && scene != null && scene.touchDragged(ui.toX(x), ui.toY(y));
            }

            @Override
            public boolean touchUp(int x, int y, int pointer, int button) {
                return pointer == 0 && scene != null && scene.touchUp(ui.toX(x), ui.toY(y));
            }

            @Override
            public boolean keyDown(int key) {
                if ((key == Input.Keys.BACK || key == Input.Keys.ESCAPE) && scene != null) {
                    if (!scene.back()) Gdx.app.exit();
                    return true;
                }
                if (key == Input.Keys.SPACE && scene != null) return scene.touchDown(-1, -1);
                return false;
            }
        });
        String first = driver != null ? driver.scene : (save.introSeen ? "lobby" : "intro");
        scene = SceneFactory.make(this, first);
        scene.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        scene.enter();
        sfx.music(true);
    }

    /** Switch scenes through a short fade to black. */
    public void go(String id) {
        if (pending != null) return;
        pending = SceneFactory.make(this, id);
        fadeDir = 1f;
    }

    @Override
    public void resize(int w, int h) {
        ui.resize(w, h);
        if (scene != null) scene.resize(w, h);
    }

    @Override
    public void render() {
        float dt = driver != null ? driver.step() : Math.min(Gdx.graphics.getDeltaTime(), 1 / 20f);
        fade = Math.max(0, Math.min(1, fade + fadeDir * dt * 3.2f));
        if (pending != null && fade >= 1f) {
            scene.dispose();
            scene = pending;
            pending = null;
            ui.fx.clear();
            scene.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            scene.enter();
            fadeDir = -1f;
        }
        if (driver != null && pending == null && driver.tapNow()) scene.touchDown(Ui.W / 2, ui.H / 2);
        scene.update(dt);
        ui.fx.update(dt);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClearColor(0.05f, 0.005f, 0.02f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(scene.cam);
        scene.render3d(batch);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        ui.begin();
        scene.renderUi();
        if (fade > 0) ui.rect(0, 0, Ui.W, ui.H, tmp.set(0, 0, 0, fade));
        ui.end();
        if (driver != null) driver.afterFrame(dt);
    }

    @Override
    public void pause() {
        save.flush();
        sfx.music(false);
    }

    @Override
    public void resume() { sfx.music(true); }

    @Override
    public void dispose() {
        save.flush();
        if (scene != null) scene.dispose();
        batch.dispose();
        ui.dispose();
        sfx.dispose();
        Shapes.get().dispose();
    }
}
