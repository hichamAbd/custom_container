package com.reelriot.vegas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Automated test runs on the desktop build: open a scene, advance a fixed
 * clock, save screenshots at given times, then quit. Never used on phones.
 */
public final class TestDriver {
    public final String scene;
    public final int width, height;
    public final String[] actions;
    private final Array<float[]> shotTimes = new Array<>();
    private final Array<String> shotFiles = new Array<>();
    private int next;
    public float clock;

    private TestDriver(String scene, int width, int height, String[] actions) {
        this.scene = scene;
        this.width = width;
        this.height = height;
        this.actions = actions;
    }

    /** --shot scene WxH t1:file1.png,t2:file2.png [action...] */
    public static TestDriver fromArgs(String[] args) {
        if (args.length < 4 || !args[0].equals("--shot")) return null;
        String[] size = args[2].split("x");
        String[] rest = new String[Math.max(0, args.length - 4)];
        System.arraycopy(args, 4, rest, 0, rest.length);
        TestDriver d = new TestDriver(args[1], Integer.parseInt(size[0]), Integer.parseInt(size[1]), rest);
        for (String part : args[3].split(",")) {
            String[] kv = part.split(":", 2);
            d.shotTimes.add(new float[] { Float.parseFloat(kv[0]) });
            d.shotFiles.add(kv[1]);
        }
        return d;
    }

    /** The fixed time step used instead of the real frame time. */
    public float step() { return 1f / 30f; }

    private final com.badlogic.gdx.utils.FloatArray taps = new com.badlogic.gdx.utils.FloatArray();
    private boolean tapsParsed;

    /** True once per "tap:SECONDS" action when the clock passes it. */
    public boolean tapNow() {
        if (!tapsParsed) {
            tapsParsed = true;
            for (String a : actions) if (a.startsWith("tap:")) taps.add(Float.parseFloat(a.substring(4)));
        }
        for (int i = 0; i < taps.size; i++) {
            if (clock >= taps.get(i)) { taps.removeIndex(i); return true; }
        }
        return false;
    }

    /** The first action that is not a tap or "spin" (a forced outcome). */
    public String forced() {
        for (String a : actions) if (!a.startsWith("tap:") && !a.equals("spin")) return a;
        return null;
    }

    public boolean hasAction(String name) {
        for (String a : actions) if (a.equals(name)) return true;
        return false;
    }

    /** Called after each rendered frame. */
    public void afterFrame(float dt) {
        clock += dt;
        while (next < shotTimes.size && clock >= shotTimes.get(next)[0]) {
            Pixmap pm = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            Pixmap flipped = new Pixmap(pm.getWidth(), pm.getHeight(), pm.getFormat());
            for (int y = 0; y < pm.getHeight(); y++) flipped.drawPixmap(pm, 0, y, 0, pm.getHeight() - 1 - y, pm.getWidth(), 1);
            FileHandle out = Gdx.files.absolute(shotFiles.get(next));
            PixmapIO.writePNG(out, flipped);
            pm.dispose();
            flipped.dispose();
            Gdx.app.log("shot", out.path() + " at " + clock);
            next++;
        }
        if (next >= shotTimes.size) Gdx.app.exit();
    }
}
