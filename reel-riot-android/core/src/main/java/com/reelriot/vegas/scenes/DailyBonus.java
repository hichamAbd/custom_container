package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.Save;
import com.reelriot.vegas.gfx.Figure;
import com.reelriot.vegas.ui.Button;
import com.reelriot.vegas.ui.Ui;

/** Vivi presents the daily login bonus: a 7-day streak that grows each day. */
public class DailyBonus implements Overlay {
    static final long[] DAILY = { 300_000, 500_000, 750_000, 1_000_000, 1_500_000, 2_000_000, 4_000_000 };
    private final ReelRiotGame game;
    private final LobbyScene lobby;
    private final int day;
    private final long amount;
    private final Button collect;
    private float t;
    private boolean done, claimed;
    private final Color C = new Color();

    DailyBonus(ReelRiotGame game, LobbyScene lobby) {
        this.game = game;
        this.lobby = lobby;
        Save s = game.save;
        long today = Save.today();
        int streak = s.lastDailyDay == today - 1 ? s.streak + 1 : 1;
        if (streak > 7) streak = 1;
        if (s.lastDailyDay == today) { streak = Math.max(1, s.streak); claimed = true; }
        day = streak;
        amount = lobby.levelScaled(DAILY[day - 1]);
        collect = new Button(claimed ? "COME BACK TOMORROW" : "COLLECT", claimed ? Button.Style.DARK : Button.Style.GREEN, Ui.W / 2 - 260, 330, 520, 130, this::claim);
        collect.size = claimed ? 40 : 64;
        collect.pulse = !claimed;
        lobby.vivi().set(Figure.Mood.TALK);
        lobby.speak(claimed ? "See you tomorrow, darling!" : "Day " + day + "! This one's on me.", 3f);
        game.sfx.play("sparkle");
    }

    private void claim() {
        if (claimed) { done = true; lobby.vivi().set(Figure.Mood.IDLE); return; }
        claimed = true;
        Save s = game.save;
        s.coins += amount;
        s.streak = day;
        s.lastDailyDay = Save.today();
        s.flush();
        game.sfx.play("jackpot");
        game.sfx.buzz(80);
        lobby.vivi().react(Figure.Mood.CHEER, 2.5f);
        lobby.coins.burst(-2f, 2.4f, 3f, 40, 7, 1.1f);
        game.ui.fx.coins(Ui.W / 2, game.ui.H * 0.45f, 40, 1700);
        game.ui.fx.confetti(Ui.W / 2, game.ui.H * 0.6f, 80, 1600);
        lobby.toast("+" + Ui.fmt(amount) + " coins!");
        t = 0;
        collect.label = "LET'S PLAY";
        collect.style = Button.Style.GOLD;
        collect.pulse = false;
        collect.onClick = () -> { done = true; lobby.vivi().set(Figure.Mood.IDLE); };
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
        float a = Math.min(1, t * 4 + (claimed ? 1 : 0));
        ui.rect(0, 0, Ui.W, ui.H * 0.55f, C.set(0.05f, 0f, 0.03f, 0.75f));
        ui.panel(40, 260, Ui.W - 80, 760, C.set(0.85f, 0.6f, 0.15f, a));
        ui.panel(48, 268, Ui.W - 96, 744, C.set(0.24f, 0.03f, 0.1f, 0.97f * a));
        ui.gold("DAILY BONUS", Ui.W / 2, 940, 92, Align.center, a);
        ui.text(Ui.BOLD, 36, "Come back every day: the streak grows to a big day 7.", Ui.W / 2, 860, Ui.CREAM, Align.center);
        float w = (Ui.W - 140 - 6 * 12) / 7f;
        for (int i = 0; i < 7; i++) {
            float x = 70 + i * (w + 12), y = 650;
            boolean past = i + 1 < day || (claimed && i + 1 == day), now = i + 1 == day;
            Color col = past ? C.set(1f, 0.8f, 0.3f, a) : now ? C.set(0.15f, 0.6f, 0.25f, a) : C.set(0.12f, 0.02f, 0.06f, a);
            if (now && !claimed) ui.glow(x + w / 2, y + 80, 120, new Color(0.4f, 1f, 0.5f, (0.4f + 0.3f * MathUtils.sin(t * 5)) * a));
            ui.panel(x, y, w, 160, col);
            Color tc = past ? new Color(0.3f, 0.1f, 0f, a) : new Color(1, 1, 1, a);
            ui.text(Ui.DISPLAY, 28, "DAY " + (i + 1), x + w / 2, y + 122, tc, Align.center);
            ui.text(Ui.DISPLAY, 30, past ? "✓" : Ui.shortFmt(lobby.levelScaled(DAILY[i])), x + w / 2, y + 58, tc, Align.center);
        }
        ui.gold("+" + Ui.fmt(amount), Ui.W / 2, 545, 96, Align.center, a);
        collect.draw(ui, t, 1 / 60f);
    }

    @Override
    public boolean touchDown(float x, float y) {
        if (collect.hit(x, y)) {
            collect.press();
            game.sfx.play("click");
            collect.onClick.run();
        }
        return true;
    }

    @Override
    public boolean finished() { return done; }
}
