package com.reelriot.vegas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Everything the game remembers between launches. */
public final class Save {
    private final Preferences p;
    public long coins, xp, totalWon, totalBet, biggest, spins;
    public int level, streak;
    public long lastFree, lastDailyDay, sessionStart, lastCare, sessionSpins0, sessionCoins0;
    public boolean introSeen, sound, music, haptics, care;
    public float bonusBoost;

    public static final long START_COINS = 10_000_000L;

    Save(boolean fresh) {
        p = Gdx.app.getPreferences("reel-riot-vegas");
        if (fresh) p.clear();
        coins = p.getLong("coins", START_COINS);
        xp = p.getLong("xp", 0);
        level = p.getInteger("level", 1);
        streak = p.getInteger("streak", 0);
        lastFree = p.getLong("lastFree", 0);
        lastDailyDay = p.getLong("lastDailyDay", -1);
        totalWon = p.getLong("totalWon", 0);
        totalBet = p.getLong("totalBet", 0);
        biggest = p.getLong("biggest", 0);
        spins = p.getLong("spins", 0);
        introSeen = p.getBoolean("introSeen", false);
        sound = p.getBoolean("sound", true);
        music = p.getBoolean("music", true);
        haptics = p.getBoolean("haptics", true);
        care = p.getBoolean("care", true);
        bonusBoost = p.getFloat("bonusBoost", 1f);
        sessionStart = lastCare = System.currentTimeMillis();
        sessionSpins0 = spins;
        sessionCoins0 = coins;
    }

    public void flush() {
        p.putLong("coins", coins).putLong("xp", xp).putInteger("level", level).putInteger("streak", streak)
         .putLong("lastFree", lastFree).putLong("lastDailyDay", lastDailyDay).putLong("totalWon", totalWon)
         .putLong("totalBet", totalBet).putLong("biggest", biggest).putLong("spins", spins)
         .putBoolean("introSeen", introSeen).putBoolean("sound", sound).putBoolean("music", music)
         .putBoolean("haptics", haptics).putBoolean("care", care).putFloat("bonusBoost", bonusBoost);
        p.flush();
    }

    /* levels: each level needs a little more XP, and pays a reward */

    public static long xpNeeded(int level) { return Math.round(600 * Math.pow(level, 1.45)); }

    public static long levelReward(int level) {
        long v = Math.round(100_000 * Math.pow(level, 1.1));
        return Math.round(v / 10_000.0) * 10_000;
    }

    /** Adds XP; returns how many levels were gained. */
    public int addXp(long amount) {
        xp += amount;
        int gained = 0;
        while (xp >= xpNeeded(level)) {
            xp -= xpNeeded(level);
            level++;
            gained++;
            coins += levelReward(level);
        }
        return gained;
    }

    public static long today() { return Math.floorDiv(System.currentTimeMillis() + java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()), 86_400_000L); }
}
