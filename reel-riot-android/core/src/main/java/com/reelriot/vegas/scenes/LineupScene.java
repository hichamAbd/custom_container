package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Align;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.Scene;
import com.reelriot.vegas.gfx.Figure;
import com.reelriot.vegas.ui.Ui;
import com.reelriot.vegas.world.CasinoSet;
import com.reelriot.vegas.world.Hosts;

/** Test scene: all four hosts on the stage. */
public class LineupScene extends Scene {
    private final CasinoSet set;
    private final Figure[] hosts = new Figure[4];

    public LineupScene(ReelRiotGame game) {
        super(game);
        set = new CasinoSet(game.ui.glow);
        String[] ids = { "goldbeak", "mochi", "vivi", "bruno" };
        Figure.Mood[] moods = { Figure.Mood.TALK, Figure.Mood.IDLE, Figure.Mood.WAVE, Figure.Mood.IDLE };
        for (int i = 0; i < 4; i++) {
            hosts[i] = Hosts.make(ids[i]);
            hosts[i].where.set(-2.1f + i * 1.4f, 0.24f, 0.2f);
            hosts[i].facing = (1.5f - i) * 8;
            hosts[i].set(moods[i]);
        }
        cam.position.set(0, 2.4f, 11.5f);
        cam.lookAt(0, 1.2f, 0);
        cam.update();
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        set.update(dt);
        for (Figure f : hosts) f.update(dt);
    }

    @Override
    public void render3d(ModelBatch batch) {
        set.render(batch);
        for (Figure f : hosts) f.render(batch);
    }

    @Override
    public void renderUi() {
        game.ui.gold("REEL RIOT", Ui.W / 2, game.ui.H - 200, 150, Align.center, 1);
        game.ui.shadowed(Ui.BOLD, 48, "Goldbeak · Mochi · Vivi · Bruno", Ui.W / 2, game.ui.H - 330, Ui.CREAM, Align.center);
    }
}
