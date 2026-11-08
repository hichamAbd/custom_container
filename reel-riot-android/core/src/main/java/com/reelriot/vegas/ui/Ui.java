package com.reelriot.vegas.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * The 2D layer: a 1080-wide coordinate space, fonts, panels, buttons, gold
 * lettering with an extruded edge and a gradient face, and image loading.
 */
public final class Ui implements Disposable {
    public static final float W = 1080;
    public float H;
    public float px;
    public final SpriteBatch batch = new SpriteBatch(2000);
    public final OrthographicCamera cam = new OrthographicCamera();
    public final Particles fx = new Particles(this);

    public static final String DISPLAY = "fonts/LilitaOne-Regular.ttf";
    public static final String TITLE = "fonts/CinzelDecorative-Black.ttf";
    public static final String BODY = "fonts/BarlowCondensed-SemiBold.ttf";
    public static final String BOLD = "fonts/BarlowCondensed-Bold.ttf";
    private static final String CHARS = FreeTypeFontGenerator.DEFAULT_CHARS + "×–—’‘“”…·•✓‹›−★";

    private final ObjectMap<String, FreeTypeFontGenerator> gens = new ObjectMap<>();
    private final ObjectMap<String, BitmapFont> fonts = new ObjectMap<>();
    private final ObjectMap<String, Texture> images = new ObjectMap<>();
    private final GlyphLayout layout = new GlyphLayout();
    private final ShaderProgram goldShader;

    public final Texture white, glow, star, ring, disc;
    public final NinePatch panel, panelSmall;
    private final TextureRegion capL, capR, capMid;

    public static final Color GOLD_HI = new Color(1f, 0.96f, 0.78f, 1);
    public static final Color GOLD = new Color(1f, 0.82f, 0.29f, 1);
    public static final Color GOLD_LO = new Color(0.48f, 0.26f, 0f, 1);
    public static final Color CREAM = new Color(0.97f, 0.91f, 0.82f, 1);
    public static final Color INK = new Color(0.07f, 0.01f, 0.04f, 1);

    public Ui() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        white = makeWhite();
        glow = makeGlow(128);
        star = makeStar(96);
        ring = makeRing(128);
        panel = new NinePatch(makeRounded(96, 30, true), 34, 34, 34, 34);
        panelSmall = new NinePatch(makeRounded(32, 10, true), 12, 12, 12, 12);
        disc = makeRounded(128, 64, false);
        capL = new TextureRegion(disc, 0, 0, 64, 128);
        capR = new TextureRegion(disc, 64, 0, 64, 128);
        capMid = new TextureRegion(disc, 63, 0, 2, 128);
        ShaderProgram.pedantic = false;
        goldShader = new ShaderProgram(GOLD_VERT, GOLD_FRAG);
        if (!goldShader.isCompiled()) Gdx.app.error("ui", goldShader.getLog());
    }

    public void resize(int w, int h) {
        px = w / W;
        H = h / px;
        cam.setToOrtho(false, W, H);
        cam.update();
        batch.setProjectionMatrix(cam.combined);
    }

    public void begin() {
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
    }

    public void end() { batch.end(); }

    /* ------------------------------------------------------------ fonts */

    /** A font generated at this size (UI units) for the current screen. */
    public BitmapFont font(String face, int size) {
        String key = face + size;
        BitmapFont f = fonts.get(key);
        if (f != null) return f;
        FreeTypeFontGenerator g = gens.get(face);
        if (g == null) gens.put(face, g = new FreeTypeFontGenerator(Gdx.files.internal(face)));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = Math.max(8, Math.round(size * px));
        p.characters = CHARS;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.kerning = true;
        p.incremental = false;
        f = g.generateFont(p);
        f.getData().setScale(1f / px);
        f.setUseIntegerPositions(false);
        fonts.put(key, f);
        return f;
    }

    public float textWidth(BitmapFont f, String s) {
        layout.setText(f, s);
        return layout.width;
    }

    /** Draw text; align is Align.left / center / right; y is the text's vertical center. */
    public void text(BitmapFont f, CharSequence s, float x, float y, Color c, int align) {
        f.setColor(c);
        layout.setText(f, s, c, 0, Align.left, false);
        float w = layout.width;
        float dx = (align & Align.center) != 0 ? -w / 2 : (align & Align.right) != 0 ? -w : 0;
        f.draw(batch, layout, x + dx, y + layout.height / 2);
    }

    public void text(String face, int size, CharSequence s, float x, float y, Color c, int align) {
        text(font(face, size), s, x, y, c, align);
    }

    /** Text with a soft dark shadow for legibility over 3D scenes. */
    public void shadowed(String face, int size, CharSequence s, float x, float y, Color c, int align) {
        BitmapFont f = font(face, size);
        tmp.set(0, 0, 0, 0.55f * c.a);
        text(f, s, x + size * 0.03f, y - size * 0.05f, tmp, align);
        text(f, s, x, y, c, align);
    }

    private final Color tmp = new Color();

    /** Wrapped text inside a width, centered vertically on y. */
    public float paragraph(String face, int size, String s, float x, float y, float width, Color c, int align) {
        BitmapFont f = font(face, size);
        f.setColor(c);
        layout.setText(f, s, c, width, align, true);
        f.draw(batch, layout, x, y + layout.height / 2);
        return layout.height;
    }

    /**
     * Big gold lettering: a shadow, a stepped brown-to-gold extruded edge,
     * a dark outline and a face with a polished gold gradient.
     */
    public void gold(CharSequence s, float x, float y, int size, int align, float alpha) {
        gold(s, x, y, size, align, alpha, GOLD_HI, GOLD, GOLD_LO);
    }

    public void gold(CharSequence s, float x, float y, int size, int align, float alpha, Color hi, Color mid, Color lo) {
        BitmapFont f = font(DISPLAY, size);
        layout.setText(f, s);
        float w = layout.width, h = layout.height;
        float x0 = (align & Align.center) != 0 ? x - w / 2 : (align & Align.right) != 0 ? x - w : x;
        float top = y + h / 2;
        float depth = size * 0.075f;
        tmp.set(0, 0, 0, 0.45f * alpha);
        f.setColor(tmp);
        f.draw(batch, s, x0 + size * 0.03f, top - depth - size * 0.06f);
        int steps = 6;
        for (int i = steps; i >= 1; i--) {
            float k = i / (float) steps;
            tmp.set(lo).lerp(mid, 1 - k).mul(0.85f);
            tmp.a = alpha;
            f.setColor(tmp);
            f.draw(batch, s, x0, top - depth * k);
        }
        float o = Math.max(1.5f, size * 0.03f);
        tmp.set(0.28f, 0.12f, 0.0f, alpha);
        f.setColor(tmp);
        for (int i = 0; i < 8; i++) {
            float a = i * MathUtils.PI / 4;
            f.draw(batch, s, x0 + MathUtils.cos(a) * o, top + MathUtils.sin(a) * o);
        }
        // the face: a vertical gold gradient computed per pixel
        batch.flush();
        batch.setShader(goldShader);
        goldShader.setUniformf("u_top", top * px);
        goldShader.setUniformf("u_bottom", (top - h) * px);
        goldShader.setUniformf("u_hi", hi.r, hi.g, hi.b);
        goldShader.setUniformf("u_mid", mid.r, mid.g, mid.b);
        goldShader.setUniformf("u_lo", lo.r, lo.g, lo.b);
        tmp.set(1, 1, 1, alpha);
        f.setColor(tmp);
        f.draw(batch, s, x0, top);
        batch.flush();
        batch.setShader(null);
    }

    /* ----------------------------------------------------------- shapes */

    public void rect(float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(white, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void panel(float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        (Math.min(w, h) < 72 ? panelSmall : panel).draw(batch, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /** A capsule (or a circle when w <= h) of any size, lit from the top. */
    public void pill(float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        if (w <= h) {
            batch.draw(disc, x + (w - h) / 2, y, h, h);
        } else {
            float r = h / 2;
            batch.draw(capL, x, y, r, h);
            batch.draw(capMid, x + r, y, w - h, h);
            batch.draw(capR, x + w - r, y, r, h);
        }
        batch.setColor(Color.WHITE);
    }

    /** A filled circle. */
    public void circle(float cx, float cy, float r, Color c) {
        batch.setColor(c);
        batch.draw(disc, cx - r, cy - r, 2 * r, 2 * r);
        batch.setColor(Color.WHITE);
    }

    /** A soft additive glow. */
    public void glow(float cx, float cy, float r, Color c) {
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
        batch.setColor(c);
        batch.draw(glow, cx - r, cy - r, r * 2, r * 2);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void image(String path, float cx, float cy, float w, float h, float alpha) {
        Texture t = image(path);
        batch.setColor(1, 1, 1, alpha);
        batch.draw(t, cx - w / 2, cy - h / 2, w, h);
        batch.setColor(Color.WHITE);
    }

    public void image(TextureRegion r, float cx, float cy, float w, float h, float rot, Color c) {
        batch.setColor(c);
        batch.draw(r, cx - w / 2, cy - h / 2, w / 2, h / 2, w, h, 1, 1, rot);
        batch.setColor(Color.WHITE);
    }

    public Texture image(String path) {
        Texture t = images.get(path);
        if (t == null) {
            t = new Texture(Gdx.files.internal(path), true);
            t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            images.put(path, t);
        }
        return t;
    }

    /** Screen pixel (top-left origin) to UI units (bottom-left origin). */
    public float toX(int sx) { return sx / px; }

    public float toY(int sy) { return H - sy / px; }

    /* ---------------------------------------------------------- formats */

    public static String fmt(long n) {
        String s = Long.toString(Math.abs(n));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) b.append(',');
            b.append(s.charAt(i));
        }
        return (n < 0 ? "-" : "") + b;
    }

    public static String shortFmt(long n) {
        if (n >= 1_000_000_000L) return trim(n / 1e9) + "B";
        if (n >= 1_000_000L) return trim(n / 1e6) + "M";
        if (n >= 1_000L) return trim(n / 1e3) + "K";
        return Long.toString(n);
    }

    private static String trim(double v) {
        String s = v >= 100 ? String.format("%.0f", v) : v >= 10 ? String.format("%.1f", v) : String.format("%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    /* ------------------------------------------------------- textures */

    private static Texture finish(Pixmap p) {
        Texture t = new Texture(p, true);
        t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private static Texture makeWhite() {
        Pixmap p = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private static Texture makeGlow(int n) {
        Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float d = (float) Math.hypot(x - n / 2f + 0.5f, y - n / 2f + 0.5f) / (n / 2f);
            float a = MathUtils.clamp(1 - d, 0, 1);
            a = a * a;
            p.drawPixel(x, y, Color.rgba8888(1, 1, 1, a));
        }
        return finish(p);
    }

    private static Texture makeRing(int n) {
        Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float d = (float) Math.hypot(x - n / 2f + 0.5f, y - n / 2f + 0.5f) / (n / 2f);
            float a = MathUtils.clamp(1 - Math.abs(d - 0.8f) / 0.12f, 0, 1);
            p.drawPixel(x, y, Color.rgba8888(1, 1, 1, a));
        }
        return finish(p);
    }

    private static Texture makeStar(int n) {
        Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float c = n / 2f;
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float dx = Math.abs(x - c + 0.5f) / c, dy = Math.abs(y - c + 0.5f) / c;
            float cross = Math.max(1 - (dx * 7 + dy * 0.9f), 1 - (dy * 7 + dx * 0.9f));
            float core = 1 - (float) Math.hypot(dx, dy) * 3.2f;
            float a = MathUtils.clamp(Math.max(cross, core), 0, 1);
            p.drawPixel(x, y, Color.rgba8888(1, 1, 1, a));
        }
        return finish(p);
    }

    /** A rounded rectangle with a top-lit gradient and a light rim. */
    private static Texture makeRounded(int n, int r, boolean rim) {
        Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float cx = MathUtils.clamp(x + 0.5f, r, n - r), cy = MathUtils.clamp(y + 0.5f, r, n - r);
            float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - cy);
            float a = MathUtils.clamp(r - d + 0.5f, 0, 1);
            float shade = 1f - 0.22f * (y / (float) n);
            float edge = rim ? MathUtils.clamp(1 - (r - d) / 3f, 0, 1) * 0.25f : 0;
            float v = Math.min(1, shade + edge);
            p.drawPixel(x, y, Color.rgba8888(v, v, v, a));
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    @Override
    public void dispose() {
        batch.dispose();
        for (BitmapFont f : fonts.values()) f.dispose();
        for (FreeTypeFontGenerator g : gens.values()) g.dispose();
        for (Texture t : images.values()) t.dispose();
        white.dispose();
        glow.dispose();
        star.dispose();
        ring.dispose();
        panel.getTexture().dispose();
        panelSmall.getTexture().dispose();
        disc.dispose();
        goldShader.dispose();
    }

    private static final String GOLD_VERT =
        "attribute vec4 a_position;\nattribute vec4 a_color;\nattribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\nvarying vec4 v_color;\nvarying vec2 v_uv;\n" +
        "void main() { v_color = a_color; v_color.a = v_color.a * (255.0/254.0); v_uv = a_texCoord0; gl_Position = u_projTrans * a_position; }\n";

    private static final String GOLD_FRAG =
        "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
        "varying vec4 v_color;\nvarying vec2 v_uv;\nuniform sampler2D u_texture;\n" +
        "uniform float u_top;\nuniform float u_bottom;\nuniform vec3 u_hi;\nuniform vec3 u_mid;\nuniform vec3 u_lo;\n" +
        "void main() {\n" +
        "  float k = clamp((u_top - gl_FragCoord.y) / max(1.0, u_top - u_bottom), 0.0, 1.0);\n" +
        "  vec3 c = k < 0.42 ? mix(u_hi, u_mid, k / 0.42) : k < 0.56 ? mix(u_mid, u_lo * 1.6, (k - 0.42) / 0.14) : mix(u_lo * 1.6, u_mid * 1.05, (k - 0.56) / 0.44);\n" +
        "  vec4 t = texture2D(u_texture, v_uv);\n" +
        "  gl_FragColor = vec4(c * v_color.rgb, t.a * v_color.a);\n" +
        "}\n";
}
