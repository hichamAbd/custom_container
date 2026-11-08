package com.reelriot.vegas.scenes;

import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.Scene;

/** Creates scenes by name: "intro", "lobby", "machine:mochi", "lineup" (test). */
public final class SceneFactory {
    private SceneFactory() { }

    public static Scene make(ReelRiotGame game, String id) {
        if (id.startsWith("machine:")) return new MachineScene(game, id.substring(8));
        switch (id) {
            case "intro": return new IntroScene(game);
            case "lineup": return new LineupScene(game);
            default: return new LobbyScene(game);
        }
    }
}
