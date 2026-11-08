package com.reelriot.vegas.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.reelriot.vegas.ReelRiotGame;
import com.reelriot.vegas.TestDriver;

/**
 * Desktop build for testing on a computer. With "--shot scene seconds file.png"
 * it plays a scene on a fixed clock and saves a screenshot, then quits.
 */
public class DesktopLauncher {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--sim")) {
            com.reelriot.vegas.slots.SlotSim.run(args.length > 1 ? Integer.parseInt(args[1]) : 2_000_000);
            return;
        }
        TestDriver driver = TestDriver.fromArgs(args);
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Reel Riot Vegas");
        int w = driver != null ? driver.width : 540, h = driver != null ? driver.height : 1080;
        config.setWindowedMode(w, h);
        config.setResizable(false);
        config.setBackBufferConfig(8, 8, 8, 8, 24, 0, driver != null ? 0 : 4);
        config.setForegroundFPS(60);
        config.useVsync(true);
        new Lwjgl3Application(new ReelRiotGame(driver), config);
    }
}
