package com.reelriot.vegas.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.reelriot.vegas.ui.Ui;

/** Text rendered into a texture for 3D signs (marquees, plaques). */
public final class TextTex implements Disposable {
    public final Texture texture;
    private final FrameBuffer fb;

    /** Gold-on-transparent lettering with an outline, sized to fit w x h pixels. */
    public TextTex(Ui ui, String text, String face, int w, int h, Color fill, Color outline) {
        fb = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        texture = fb.getColorBufferTexture();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        BitmapFont font = ui.font(face, Math.round(h * 0.62f / ui.px));
        float oldScale = font.getData().scaleX;
        GlyphLayout gl = new GlyphLayout();
        font.getData().setScale(1f);
        gl.setText(font, text);
        float fit = Math.min(1f, (w * 0.92f) / gl.width);
        font.getData().setScale(fit);
        gl.setText(font, text);
        SpriteBatch b = ui.batch;
        Matrix4 old = b.getProjectionMatrix().cpy();
        fb.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        // a top-down projection, so the texture reads right side up on our quads
        b.setProjectionMatrix(new Matrix4().setToOrtho(0, w, h, 0, -1, 1));
        b.begin();
        float x = (w - gl.width) / 2, y = (h - gl.height) / 2;
        float o = Math.max(2, h * 0.035f);
        font.setColor(0, 0, 0, 0.5f);
        font.draw(b, text, x + o, y + o * 1.6f + gl.height);
        if (outline != null) {
            font.setColor(outline);
            for (int i = 0; i < 8; i++) {
                float a = i * MathUtils.PI / 4;
                font.draw(b, text, x + MathUtils.cos(a) * o, y + MathUtils.sin(a) * o + gl.height);
            }
        }
        font.setColor(fill);
        font.draw(b, text, x, y + gl.height);
        b.end();
        if (System.getenv("REELRIOT_DUMP") != null) {
            Pixmap pm = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0, 0, w, h);
            com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.absolute(System.getenv("REELRIOT_DUMP") + "/tex-" + text.hashCode() + ".png"), pm);
            pm.dispose();
        }
        fb.end();
        b.setProjectionMatrix(old);
        font.getData().setScale(oldScale);
        font.setColor(Color.WHITE);
    }

    @Override
    public void dispose() { fb.dispose(); }
}
