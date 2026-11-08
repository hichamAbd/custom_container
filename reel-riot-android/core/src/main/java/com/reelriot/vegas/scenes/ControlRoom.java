package com.reelriot.vegas.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.Save;
import com.reelriot.vegas.ui.Button;
import com.reelriot.vegas.ui.Ui;

/**
 * The Control Room: test controls for everything. Coins, bonus frequency,
 * forcing the next spin, levels, timers, sound and the intro.
 */
public class ControlRoom implements Overlay, MachineScene.Fullscreen {
    private final ReelRiotGame game;
    private final MachineScene machine;
    private final Array<Button> buttons = new Array<>();
    private final Array<Object[]> labels = new Array<>();
    private boolean done;
    private float t;
    private String note = "";
    private final Color C = new Color();
    private static final float[] BOOSTS = { 0.25f, 1f, 5f, 20f };

    ControlRoom(ReelRiotGame game, MachineScene machine) {
        this.game = game;
        this.machine = machine;
        build();
    }

    private void build() {
        buttons.clear();
        labels.clear();
        Save s = game.save;
        float y = game.ui.H - 330, x0 = 60, w = Ui.W - 120;
        y = row("Coins", y, new String[] { "+1M", "+10M", "+100M", "Reset" }, i -> {
            long[] add = { 1_000_000, 10_000_000, 100_000_000 };
            if (i < 3) s.coins += add[i]; else s.coins = Save.START_COINS;
            say("Coins: " + Ui.fmt(s.coins));
        }, -1);
        int sel = 1;
        for (int i = 0; i < BOOSTS.length; i++) if (Math.abs(BOOSTS[i] - s.bonusBoost) < 0.01f) sel = i;
        y = row("Bonus games: how often", y, new String[] { "×0.25", "×1", "×5", "×20" }, i -> {
            s.bonusBoost = BOOSTS[i];
            say("Bonus games ×" + BOOSTS[i] + " as often");
            build();
        }, sel);
        if (machine != null) {
            y = row("Next spin", y, new String[] { "Bonus", "Big", "Mega", "Epic" }, i -> {
                String[] f = { "bonus", "big", "mega", "epic" };
                machine.forceNext(f[i]);
                say("The next spin will be: " + f[i]);
            }, -1);
            y = row("", y + 40, new String[] { "Tease", "Lose", "Any win" }, i -> {
                String[] f = { "tease", "lose", "win" };
                machine.forceNext(f[i]);
                say("The next spin will be: " + f[i]);
            }, -1);
        }
        y = row("Level " + s.level, y, new String[] { "+1 level", "XP to 95%", "Level 1" }, i -> {
            if (i == 0) { s.addXp(Save.xpNeeded(s.level) - s.xp); game.sfx.play("jackpot"); }
            else if (i == 1) s.xp = Math.round(Save.xpNeeded(s.level) * 0.95);
            else { s.level = 1; s.xp = 0; }
            say("Level " + s.level + ", XP " + s.xp + " of " + Save.xpNeeded(s.level));
            build();
        }, -1);
        y = row("Rewards", y, new String[] { "Free coins now", "Daily bonus now", "Next day" }, i -> {
            if (i == 0) s.lastFree = 0;
            else if (i == 1) s.lastDailyDay = Save.today() - 1;
            else { s.lastDailyDay = Save.today() - 1; }
            say(i == 0 ? "Free coins are ready" : i == 1 ? "The daily bonus is ready" : "A new day: the streak continues");
        }, -1);
        y = row("Settings", y, new String[] { "Sound " + (s.sound ? "on" : "off"), "Music " + (s.music ? "on" : "off"), "Vibrate " + (s.haptics ? "on" : "off") }, i -> {
            if (i == 0) s.sound = !s.sound;
            else if (i == 1) { s.music = !s.music; game.sfx.music(true); }
            else s.haptics = !s.haptics;
            build();
        }, -1);
        y = row("", y + 40, new String[] { "Breaks " + (s.care ? "on" : "off"), "Replay intro" }, i -> {
            if (i == 0) { s.care = !s.care; build(); }
            else { s.introSeen = false; s.flush(); game.go("intro"); done = true; }
        }, -1);
        Button close = new Button("CLOSE", Button.Style.GOLD, Ui.W / 2 - 200, 90, 400, 110, this::close);
        close.size = 52;
        buttons.add(close);
    }

    interface Pick { void run(int i); }

    private float row(String title, float y, String[] names, Pick pick, int selected) {
        if (!title.isEmpty()) {
            labels.add(new Object[] { title, y });
            y -= 70;
        } else {
            y -= 30;
        }
        float gap = 14, x0 = 60, w = (Ui.W - 120 - gap * (names.length - 1)) / names.length;
        for (int i = 0; i < names.length; i++) {
            final int k = i;
            Button b = new Button(names[i], i == selected ? Button.Style.GREEN : Button.Style.DARK, x0 + i * (w + gap), y - 92, w, 92, () -> {
                game.sfx.play("click");
                pick.run(k);
                game.save.flush();
            });
            b.size = names[i].length() > 10 ? 32 : 38;
            buttons.add(b);
        }
        return y - 120;
    }

    private void say(String s) { note = s; }

    void close() {
        game.save.flush();
        done = true;
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
        ui.rect(0, 0, Ui.W, ui.H, C.set(0.04f, 0f, 0.02f, 0.92f));
        ui.image("hosts/vivi.png", 150, ui.H - 150, 220, 220, 1);
        ui.gold("CONTROL ROOM", Ui.W / 2 + 80, ui.H - 130, 84, Align.center, 1);
        ui.text(Ui.BOLD, 34, "Test anything. Coins are for fun only.", Ui.W / 2 + 80, ui.H - 210, Ui.CREAM, Align.center);
        for (Object[] l : labels) ui.text(Ui.DISPLAY, 38, (String) l[0], 64, (Float) l[1], Ui.GOLD, Align.left);
        for (Button b : buttons) b.draw(ui, t, 1 / 60f);
        if (!note.isEmpty()) ui.text(Ui.BOLD, 36, note, Ui.W / 2, 250, Ui.GOLD_HI, Align.center);
    }

    @Override
    public boolean touchDown(float x, float y) {
        for (Button b : buttons) {
            if (b.hit(x, y)) {
                b.press();
                b.onClick.run();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean finished() { return done; }
}
