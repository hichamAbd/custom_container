package com.reelriot.vegas.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/** A tappable button drawn in the Vegas Gold style. */
public class Button {
    public enum Style { GOLD, GREEN, DARK, RUBY, SPIN, ICON }

    public float x, y, w, h;
    public String label, sub, icon;
    public Style style;
    public Runnable onClick;
    public boolean enabled = true, visible = true, pulse;
    public int size = 44;
    private float pressed;

    public Button(String label, Style style, float x, float y, float w, float h, Runnable onClick) {
        this.label = label;
        this.style = style;
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.onClick = onClick;
    }

    public boolean hit(float px, float py) {
        return visible && enabled && px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public void press() { pressed = 0.15f; }

    private static final Color C = new Color();

    public void draw(Ui ui, float time, float dt) {
        if (!visible) return;
        pressed = Math.max(0, pressed - dt);
        float sink = pressed > 0 ? 6 : 0;
        float glowK = pulse && enabled ? 0.5f + 0.5f * MathUtils.sin(time * 5) : 0;
        float a = enabled ? 1 : 0.45f;
        switch (style) {
            case SPIN: {
                float cx = x + w / 2, cy = y + h / 2, r = w / 2;
                ui.glow(cx, cy - sink, r * 1.55f, C.set(1f, 0.75f, 0.2f, (0.35f + glowK * 0.35f) * a));
                ui.pill(cx - r, cy - r - 10, r * 2, r * 2, C.set(0.35f, 0.16f, 0f, a));
                ui.pill(cx - r, cy - r - sink, r * 2, r * 2, C.set(0.62f, 0.36f, 0.04f, a));
                ui.pill(cx - r * 0.9f, cy - r * 0.9f - sink, r * 1.8f, r * 1.8f, C.set(1f, 0.8f, 0.25f, a));
                ui.pill(cx - r * 0.7f, cy - r * 0.55f - sink, r * 1.4f, r * 1.2f, C.set(1f, 0.93f, 0.6f, a * 0.55f));
                ui.text(Ui.DISPLAY, size, label, cx, cy - sink + 4, C.set(0.3f, 0.1f, 0f, a), com.badlogic.gdx.utils.Align.center);
                if (sub != null) ui.text(Ui.BOLD, 26, sub, cx, cy - sink - size * 0.62f, C.set(0.35f, 0.14f, 0f, a), com.badlogic.gdx.utils.Align.center);
                return;
            }
            case ICON: {
                ui.pill(x, y - sink, w, h, C.set(0.12f, 0.02f, 0.06f, 0.85f * a));
                ui.pill(x + 3, y + 3 - sink, w - 6, h - 6, C.set(0.3f, 0.06f, 0.14f, 0.9f * a));
                if (icon != null) ui.image(icon, x + w / 2, y + h / 2 - sink, h * 0.72f, h * 0.72f, a);
                if (label != null) ui.text(Ui.DISPLAY, size, label, x + w / 2, y + h / 2 - sink, C.set(Ui.GOLD).mul(1, 1, 1, a), com.badlogic.gdx.utils.Align.center);
                return;
            }
            default:
                break;
        }
        Color top, mid, text;
        switch (style) {
            case GREEN: top = C.set(0.55f, 0.95f, 0.4f, a); mid = new Color(0.1f, 0.58f, 0.18f, a); text = Color.WHITE; break;
            case RUBY: top = C.set(1f, 0.4f, 0.5f, a); mid = new Color(0.68f, 0.03f, 0.15f, a); text = Color.WHITE; break;
            case DARK: top = C.set(0.4f, 0.08f, 0.18f, a); mid = new Color(0.16f, 0.02f, 0.07f, a); text = Ui.GOLD; break;
            default: top = C.set(1f, 0.93f, 0.62f, a); mid = new Color(0.95f, 0.66f, 0.1f, a); text = new Color(0.28f, 0.09f, 0f, 1); break;
        }
        ui.pill(x - 4, y - 10, w + 8, h + 8, new Color(0.3f, 0.14f, 0f, 0.9f * a));
        ui.pill(x - 4, y - 4 - sink, w + 8, h + 8, new Color(1f, 0.85f, 0.4f, a));
        ui.pill(x, y - sink, w, h, mid);
        ui.pill(x + h * 0.12f, y + h * 0.45f - sink, w - h * 0.24f, h * 0.45f, new Color(top.r, top.g, top.b, 0.55f * a));
        if (glowK > 0) ui.glow(x + w / 2, y + h / 2, w * 0.7f, new Color(1f, 0.85f, 0.4f, glowK * 0.4f));
        float ty = sub != null ? y + h * 0.6f - sink : y + h / 2 - sink + 2;
        Color tc = new Color(text.r, text.g, text.b, a);
        if (style == Style.GREEN || style == Style.RUBY) ui.text(Ui.DISPLAY, size, label, x + w / 2 + 2, ty - 3, new Color(0, 0.15f, 0, 0.6f * a), com.badlogic.gdx.utils.Align.center);
        ui.text(Ui.DISPLAY, size, label, x + w / 2, ty, tc, com.badlogic.gdx.utils.Align.center);
        if (sub != null) ui.text(Ui.BOLD, (int) (size * 0.6f), sub, x + w / 2, y + h * 0.26f - sink, tc, com.badlogic.gdx.utils.Align.center);
    }
}
