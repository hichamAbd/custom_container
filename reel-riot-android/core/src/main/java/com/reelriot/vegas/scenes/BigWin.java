package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.gfx.Figure;
import com.reelriot.vegas.ui.Ui;

/**
 * The big win celebration: light rays, a title that climbs through Big,
 * Mega and Epic as the counter rises, 3D coins bursting out of the
 * cabinet, a coin rain, confetti and the host cheering. Jackpots and bonus
 * results use a fixed title.
 */
public class BigWin implements Overlay, MachineScene.Fullscreen {
    private final ReelRiotGame game;
    private final MachineScene scene;
    private final long amount, bet;
    private final String fixedTitle;
    private final float dur;
    private float t, shown, titlePop, burstT;
    private int tier = -1;
    private boolean counted, done;
    public boolean bonusResult;
    private final Color C = new Color();
    private static final String[] TIERS = { "BIG WIN", "MEGA WIN", "EPIC WIN", "LEGENDARY" };
    private static final float[] AT = { 10, 25, 50, 100 };

    BigWin(ReelRiotGame game, MachineScene scene, long amount, long bet, String title) {
        this.game = game;
        this.scene = scene;
        this.amount = amount;
        this.bet = bet;
        this.fixedTitle = title;
        float x = amount / (float) bet;
        dur = title != null ? 4.5f : x >= 50 ? 6f : x >= 25 ? 4.6f : 3.2f;
        game.sfx.play(title != null ? "jackpot" : "bigwin");
        game.sfx.buzz(80);
        scene.host.set(Figure.Mood.CHEER);
        if (title != null) { tier = 3; titlePop = 1; }
    }

    @Override
    public void update(float dt) {
        t += dt;
        titlePop = Math.max(0, titlePop - dt * 2.5f);
        if (!counted) {
            float k = Math.min(1, t / dur);
            shown = amount * Interpolation.pow2Out.apply(k);
            if (fixedTitle == null) {
                float x = shown / bet;
                int nt = -1;
                for (int i = 0; i < AT.length; i++) if (x >= AT[i]) nt = i;
                if (nt > tier) {
                    tier = nt;
                    titlePop = 1;
                    game.sfx.play("upgrade", 0.8f, 1 + tier * 0.12f);
                    game.ui.fx.confetti(Ui.W / 2, game.ui.H * 0.62f, 50 + tier * 20, 1500);
                    scene.coins.burst(0, 0.5f, 1.5f, 12 + tier * 8, 7, 1.4f);
                }
            }
            burstT -= dt;
            if (burstT <= 0) {
                burstT = 0.32f;
                scene.coins.burst(MathUtils.random(-2f, 2f), MathUtils.random(-0.5f, 1.5f), 1.2f, 4, 6.5f, 1.4f);
                game.ui.fx.rain(3);
                game.sfx.play("coin", 0.5f, MathUtils.random(0.9f, 1.3f));
            }
            if (k >= 1) {
                counted = true;
                shown = amount;
                game.ui.fx.coins(Ui.W / 2, game.ui.H * 0.45f, 40, 1800);
                game.sfx.play("win");
            }
        } else if (t > dur + 4f) {
            finish();
        }
    }

    private void finish() {
        done = true;
        scene.host.set(Figure.Mood.IDLE);
    }

    @Override
    public PerspectiveCamera cam() { return null; }

    @Override
    public void render3d(ModelBatch batch) { }

    @Override
    public void renderUi() {
        Ui ui = game.ui;
        float cx = Ui.W / 2, cy = ui.H * 0.6f;
        float a = Math.min(1, t * 4);
        ui.rect(0, 0, Ui.W, ui.H, C.set(0.05f, 0, 0.03f, 0.38f * a));
        // rotating light rays
        ui.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < 18; i++) {
            float ang = t * 12 + i * 20;
            ui.batch.setColor(1f, 0.62f, 0.18f, (i % 2 == 0 ? 0.075f : 0.04f) * a);
            ui.batch.draw(ui.white, cx, cy - 60, 0, 60, 1500, 120, 1, 1, ang, 0, 0, 4, 4, false, false);
        }
        ui.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ui.glow(cx, cy, 520, C.set(1f, 0.75f, 0.3f, 0.45f * a));
        String title = fixedTitle != null ? fixedTitle : tier >= 0 ? TIERS[tier] : "";
        float pop = 1 + titlePop * 0.5f;
        int size = (int) ((fixedTitle != null && fixedTitle.length() > 12 ? 104 : 140) * pop);
        if (!title.isEmpty()) ui.gold(title, cx, cy + 210, size, Align.center, a);
        ui.panel(cx - 430, cy - 110, 860, 190, C.set(0.08f, 0.01f, 0.04f, 0.85f * a));
        ui.panel(cx - 422, cy - 102, 844, 174, C.set(0.3f, 0.04f, 0.12f, 0.9f * a));
        ui.gold(Ui.fmt(Math.round(shown)), cx, cy - 12, 112, Align.center, a);
        ui.text(Ui.BOLD, 42, Math.round(shown / bet) + " × your bet", cx, cy - 170, C.set(Ui.CREAM).mul(1, 1, 1, a), Align.center);
        if (counted) {
            float k = 0.6f + 0.4f * MathUtils.sin(t * 5);
            ui.text(Ui.DISPLAY, 46, "Tap to continue", cx, ui.H * 0.2f, C.set(1f, 0.95f, 0.75f, k), Align.center);
        }
    }

    @Override
    public boolean touchDown(float x, float y) {
        if (!counted) {
            // skip to the end of the count
            t = dur;
        } else if (t > dur + 0.4f) {
            finish();
        }
        return true;
    }

    @Override
    public boolean finished() { return done; }
}
