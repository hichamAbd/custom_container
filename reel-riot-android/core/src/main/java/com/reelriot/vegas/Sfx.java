package com.reelriot.vegas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/** Sound effects, the lounge music loop and vibration, following the player's settings. */
public final class Sfx implements Disposable {
    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final Save save;
    private Music music;
    private final boolean silent;

    Sfx(Save save, boolean silent) {
        this.save = save;
        this.silent = silent;
    }

    public void play(String name) { play(name, 1f, 1f); }

    public void play(String name, float volume, float pitch) {
        if (silent || !save.sound) return;
        Sound s = sounds.get(name);
        if (s == null) {
            try { s = Gdx.audio.newSound(Gdx.files.internal("sfx/" + name + ".wav")); } catch (Exception e) { return; }
            sounds.put(name, s);
        }
        s.play(volume, pitch, 0);
    }

    public void music(boolean on) {
        if (silent) return;
        if (music == null) {
            try {
                music = Gdx.audio.newMusic(Gdx.files.internal("sfx/lounge.wav"));
                music.setLooping(true);
                music.setVolume(0.35f);
            } catch (Exception e) { return; }
        }
        if (on && save.music) { if (!music.isPlaying()) music.play(); } else music.pause();
    }

    public void buzz(int ms) {
        if (!silent && save.haptics) Gdx.input.vibrate(ms);
    }

    @Override
    public void dispose() {
        for (Sound s : sounds.values()) s.dispose();
        if (music != null) music.dispose();
    }
}
