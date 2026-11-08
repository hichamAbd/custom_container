package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Align;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.Save;
import com.reelriot.vegas.ui.Button;
import com.reelriot.vegas.ui.Ui;

/** Vivi's break reminder with honest numbers for this session. */
public class Care implements Overlay, MachineScene.Fullscreen {
    private final ReelRiotGame game;
    private final Button rest, keep;
    private final long minutes, spins, net;
    private boolean done;
    private float t;
    private final Color C = new Color();

    Care(ReelRiotGame game) {
        this.game = game;
        Save s = game.save;
        minutes = Math.max(1, (System.currentTimeMillis() - s.sessionStart) / 60_000);
        spins = s.spins - s.sessionSpins0;
        net = s.coins - s.sessionCoins0;
        rest = new Button("TAKE A BREAK", Button.Style.GOLD, 90, 520, Ui.W - 180, 120, () -> { done = true; game.go("lobby"); });
        keep = new Button("KEEP PLAYING", Button.Style.DARK, 90, 370, Ui.W - 180, 110, () -> done = true);
        rest.size = 50;
        keep.size = 44;
        game.sfx.play("sparkle");
    }

    @Override
    public void update(float dt) { t += dt; }

    @Override
    public PerspectiveCamera cam() { return null; }

    @Override
    public void render3d(ModelBatch batch) { }

    @Override
    public void renderUi() {
        Ui ui = game.ui;
        float a = Math.min(1, t * 4);
        ui.rect(0, 0, Ui.W, ui.H, C.set(0.04f, 0, 0.02f, 0.88f * a));
        ui.image("hosts/vivi.png", Ui.W / 2, ui.H * 0.72f, 360, 360, a);
        ui.gold("Time for a stretch?", Ui.W / 2, ui.H * 0.56f, 76, Align.center, a);
        ui.text(Ui.BOLD, 40, "You've been playing for " + minutes + (minutes == 1 ? " minute." : " minutes."), Ui.W / 2, ui.H * 0.56f - 100, C.set(Ui.CREAM).mul(1, 1, 1, a), Align.center);
        float y = ui.H * 0.56f - 230;
        stat(ui, "Spins", Ui.fmt(spins), 90, y, a, Ui.GOLD_HI);
        stat(ui, "Coins this session", (net >= 0 ? "+" : "") + Ui.fmt(net), Ui.W / 2 + 10, y, a, net >= 0 ? new Color(0.5f, 1f, 0.6f, 1) : new Color(1f, 0.6f, 0.65f, 1));
        rest.draw(ui, t, 1 / 60f);
        keep.draw(ui, t, 1 / 60f);
        ui.text(Ui.BOLD, 30, "Reminders every 45 minutes. Turn them off in the Control Room.", Ui.W / 2, 290, C.set(Ui.CREAM).mul(1, 1, 1, 0.7f * a), Align.center);
    }

    private void stat(Ui ui, String label, String value, float x, float y, float a, Color vc) {
        ui.panel(x, y - 80, Ui.W / 2 - 100, 150, C.set(0.15f, 0.02f, 0.07f, a));
        ui.text(Ui.BOLD, 32, label, x + (Ui.W / 2 - 100) / 2, y + 30, C.set(Ui.CREAM).mul(1, 1, 1, a), Align.center);
        ui.text(Ui.DISPLAY, 50, value, x + (Ui.W / 2 - 100) / 2, y - 25, C.set(vc).mul(1, 1, 1, a), Align.center);
    }

    @Override
    public boolean touchDown(float x, float y) {
        for (Button b : new Button[] { rest, keep }) {
            if (b.hit(x, y)) {
                b.press();
                game.sfx.play("click");
                b.onClick.run();
            }
        }
        return true;
    }

    @Override
    public boolean finished() { return done; }
}
