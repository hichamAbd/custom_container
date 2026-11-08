package com.reelriot.vegas.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.reelriot.vegas.Save;
import com.reelriot.vegas.slots.Machine;

/** Shared HUD pieces: the coin bar with level, and the jackpot plaques. */
public final class Hud {
    private Hud() { }

    private static final Color C = new Color();
    public static final Color[] JACKPOT_COLORS = {
        new Color(0.05f, 0.5f, 0.27f, 1), new Color(0.08f, 0.3f, 0.78f, 1), new Color(0.38f, 0.12f, 0.7f, 1), new Color(0.75f, 0.04f, 0.16f, 1)
    };

    /** Coins pill in the middle of the top bar, level badge with XP ring on its left. */
    public static void coinBar(Ui ui, long coins, Save save, float y) {
        float w = 470, h = 92, x = Ui.W / 2 - w / 2 + 40;
        ui.pill(x - 4, y - 4, w + 8, h + 8, C.set(0.85f, 0.6f, 0.15f, 1));
        ui.pill(x, y, w, h, C.set(0.1f, 0.01f, 0.05f, 0.95f));
        ui.image("symbols/coin.png", x + 44, y + h / 2, 76, 76, 1);
        ui.text(Ui.DISPLAY, 50, Ui.fmt(coins), x + w / 2 + 34, y + h / 2 + 2, Ui.GOLD_HI, Align.center);
        float bx = x - 72, by = y + h / 2;
        float k = Save.xpNeeded(save.level) > 0 ? save.xp / (float) Save.xpNeeded(save.level) : 0;
        ui.pill(bx - 56, by - 56, 112, 112, C.set(0.85f, 0.6f, 0.15f, 1));
        ui.pill(bx - 50, by - 50, 100, 100, C.set(0.24f, 0.06f, 0.5f, 1));
        // XP progress as a ring of dots
        int dots = 24;
        for (int i = 0; i < dots; i++) {
            float a = MathUtils.PI / 2 - i / (float) dots * MathUtils.PI2;
            boolean on = i < Math.round(k * dots);
            ui.circle(bx + MathUtils.cos(a) * 43, by + MathUtils.sin(a) * 43, 5, on ? Ui.GOLD : C.set(1, 1, 1, 0.18f));
        }
        ui.text(Ui.DISPLAY, 44, Integer.toString(save.level), bx, by + 2, Color.WHITE, Align.center);
    }

    /** GRAND across the top, then MAJOR, MINOR and MINI. */
    public static void plaques(Ui ui, long bet, float top, float time) {
        float m = 34;
        plaque(ui, 3, bet * Machine.JACKPOT_X[3], m, top - 130, Ui.W - 2 * m, 120, 60, time);
        float w = (Ui.W - 2 * m - 2 * 16) / 3f;
        for (int i = 2; i >= 0; i--) {
            float x = m + (2 - i) * (w + 16);
            plaque(ui, i, bet * Machine.JACKPOT_X[i], x, top - 130 - 16 - 100, w, 100, 38, time);
        }
    }

    public static void plaque(Ui ui, int tier, long value, float x, float y, float w, float h, int size, float time) {
        Color col = JACKPOT_COLORS[tier];
        float glow = 0.3f + 0.15f * MathUtils.sin(time * 3 + tier);
        ui.glow(x + w / 2, y + h / 2, w * 0.55f, C.set(col.r, col.g, col.b, glow));
        ui.panel(x - 5, y - 5, w + 10, h + 10, C.set(1f, 0.8f, 0.32f, 1));
        ui.panel(x, y, w, h, C.set(col).mul(0.85f, 0.85f, 0.85f, 1));
        ui.panel(x + 6, y + h * 0.5f, w - 12, h * 0.44f, C.set(1, 1, 1, 0.12f));
        ui.text(Ui.DISPLAY, (int) (size * 0.55f), Machine.JACKPOT_NAMES[tier], x + w / 2, y + h * 0.77f, C.set(1, 1, 1, 0.85f), Align.center);
        ui.text(Ui.DISPLAY, size, Ui.fmt(value), x + w / 2, y + h * 0.36f, Ui.GOLD_HI, Align.center);
    }

    /** A rounded speech bubble with a tail pointing down-left to (tx, ty). */
    public static void bubble(Ui ui, String text, float cx, float cy, float tx, float ty, float alpha) {
        if (alpha <= 0.01f) return;
        float w = Math.min(640, ui.textWidth(ui.font(Ui.DISPLAY, 42), text) + 70), h = 96;
        float x = MathUtils.clamp(cx - w / 2, 20, Ui.W - w - 20);
        ui.panel(x - 4, cy - h / 2 - 4, w + 8, h + 8, C.set(0.85f, 0.6f, 0.15f, alpha));
        ui.panel(x, cy - h / 2, w, h, C.set(1f, 0.98f, 0.94f, alpha));
        float bx = MathUtils.clamp(tx, x + 40, x + w - 40);
        for (int i = 0; i < 4; i++) ui.circle(bx + (tx - bx) * i / 4f, cy - h / 2 - 4 - i * 13, 13 - i * 2.5f, C.set(1f, 0.98f, 0.94f, alpha));
        ui.text(Ui.DISPLAY, 42, text, x + w / 2, cy + 2, C.set(0.3f, 0.08f, 0.02f, alpha), Align.center);
    }
}
