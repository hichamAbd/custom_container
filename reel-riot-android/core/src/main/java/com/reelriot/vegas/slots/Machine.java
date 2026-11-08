package com.reelriot.vegas.slots;

import com.badlogic.gdx.graphics.Color;

/**
 * A machine's design: symbols, reel weights, paytable and bonus. Pays are
 * multiples of the line bet (bet / 20) for 3, 4 or 5 in a row on a line.
 * Wilds land on reels 2-4; bonus symbols on reels 1, 3 and 5 (all three
 * start the machine's bonus game).
 */
public final class Machine {
    public static final int REELS = 5, ROWS = 3, LINES = 20;
    /** Paytable scale, tuned with the simulator (desktop: --sim) for about 96% return. */
    static final float PAY_SCALE = 2.25f;

    public static final int[][] PAYLINES = {
        { 1, 1, 1, 1, 1 }, { 0, 0, 0, 0, 0 }, { 2, 2, 2, 2, 2 }, { 0, 1, 2, 1, 0 }, { 2, 1, 0, 1, 2 },
        { 0, 0, 1, 2, 2 }, { 2, 2, 1, 0, 0 }, { 1, 0, 0, 0, 1 }, { 1, 2, 2, 2, 1 }, { 1, 0, 1, 2, 1 },
        { 1, 2, 1, 0, 1 }, { 0, 1, 1, 1, 0 }, { 2, 1, 1, 1, 2 }, { 0, 1, 0, 1, 0 }, { 2, 1, 2, 1, 2 },
        { 1, 1, 0, 1, 1 }, { 1, 1, 2, 1, 1 }, { 0, 0, 2, 0, 0 }, { 2, 2, 0, 2, 2 }, { 0, 2, 0, 2, 0 }
    };

    public enum Bonus { LUCKY_DROP, MEGA_WHEEL, TREASURE }

    public static final class Sym {
        public final String id, image, name;
        public final int[] pays;
        public final int weight;
        public final boolean wild, bonus;

        Sym(String id, String name, String image, int weight, int p3, int p4, int p5, boolean wild, boolean bonus) {
            this.id = id;
            this.name = name;
            this.image = image;
            this.weight = weight;
            this.pays = new int[] { 0, 0, 0, Math.round(p3 * PAY_SCALE), Math.round(p4 * PAY_SCALE), Math.round(p5 * PAY_SCALE) };
            this.wild = wild;
            this.bonus = bonus;
        }
    }

    public final String id, name, host, tagline, bonusName, bonusHint;
    public final Bonus bonus;
    public final Sym[] syms;
    public final Color body, accent, glow;
    /** Jackpot prizes as multiples of the total bet: Mini, Minor, Major, Grand. */
    public static final int[] JACKPOT_X = { 10, 25, 100, 1000 };
    public static final String[] JACKPOT_NAMES = { "MINI", "MINOR", "MAJOR", "GRAND" };

    Machine(String id, String name, String host, String tagline, Bonus bonus, String bonusName, String bonusHint,
            Color body, Color accent, Color glow, Sym... syms) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.tagline = tagline;
        this.bonus = bonus;
        this.bonusName = bonusName;
        this.bonusHint = bonusHint;
        this.body = body;
        this.accent = accent;
        this.glow = glow;
        this.syms = syms;
    }

    public int index(String symId) {
        for (int i = 0; i < syms.length; i++) if (syms[i].id.equals(symId)) return i;
        return -1;
    }

    static Sym s(String id, String name, String img, int w, int p3, int p4, int p5) { return new Sym(id, name, img, w, p3, p4, p5, false, false); }

    static Sym wild(String id, String name, String img, int w) { return new Sym(id, name, img, w, 0, 0, 0, true, false); }

    static Sym bonus(String id, String name, String img, int w) { return new Sym(id, name, img, w, 0, 0, 0, false, true); }

    static Color c(float r, float g, float b) { return new Color(r, g, b, 1); }

    public static final Machine LUCKY_PAWS = new Machine("mochi", "Lucky Paws Palace", "mochi", "Golden cats, red lanterns, lucky coins",
            Bonus.LUCKY_DROP, "Lucky Drop", "3 Lucky Coins start the Lucky Drop",
            c(0.55f, 0.03f, 0.1f), c(1f, 0.25f, 0.3f), c(1f, 0.55f, 0.25f),
            s("ingot", "Gold Ingot", "symbols/ingot.png", 2, 40, 160, 800),
            s("koi", "Koi", "symbols/koi.png", 3, 30, 110, 400),
            s("lantern", "Lantern", "symbols/lantern.png", 4, 20, 80, 250),
            s("jade", "Jade Coin", "symbols/jade.png", 4, 16, 60, 180),
            s("A", "A", "symbols/royal-A.png", 5, 8, 26, 100),
            s("K", "K", "symbols/royal-K.png", 5, 8, 24, 90),
            s("Q", "Q", "symbols/royal-Q.png", 6, 5, 18, 70),
            s("J", "J", "symbols/royal-J.png", 6, 5, 15, 60),
            s("10", "10", "symbols/royal-10.png", 6, 4, 14, 50),
            wild("wild", "Mochi Wild", "hosts/mochi.png", 2),
            bonus("bonus", "Lucky Coin", "symbols/coin-paw.png", 3));

    public static final Machine HIGH_ROLLER = new Machine("bruno", "High Roller Lounge", "bruno", "Crowns, rubies and Bruno's big wheel",
            Bonus.MEGA_WHEEL, "Mega Wheel", "3 Wheel symbols spin Bruno's Mega Wheel",
            c(0.24f, 0.07f, 0.45f), c(0.75f, 0.45f, 1f), c(0.8f, 0.5f, 1f),
            s("crown", "Crown", "symbols/crown.png", 2, 40, 160, 800),
            s("ring", "Ruby Ring", "symbols/ring.png", 3, 30, 110, 400),
            s("ruby", "Ruby", "symbols/gem-ruby.png", 4, 20, 80, 250),
            s("sapphire", "Sapphire", "symbols/gem-sapphire.png", 4, 16, 60, 180),
            s("A", "A", "symbols/royal-A.png", 5, 8, 26, 100),
            s("K", "K", "symbols/royal-K.png", 5, 8, 24, 90),
            s("Q", "Q", "symbols/royal-Q.png", 6, 5, 18, 70),
            s("J", "J", "symbols/royal-J.png", 6, 5, 15, 60),
            s("10", "10", "symbols/royal-10.png", 6, 4, 14, 50),
            wild("wild", "Bruno Wild", "hosts/bruno.png", 2),
            bonus("bonus", "Wheel", "symbols/megawheel.png", 3));

    public static final Machine GOLDBEAK_COVE = new Machine("goldbeak", "Goldbeak's Cove", "goldbeak", "Maps, keys and buried treasure",
            Bonus.TREASURE, "Treasure Chests", "3 Treasure Chests start the treasure hunt",
            c(0.03f, 0.3f, 0.4f), c(0.3f, 0.85f, 0.95f), c(0.4f, 0.9f, 1f),
            s("chestkey", "Gold Key", "symbols/key.png", 2, 40, 160, 800),
            s("swords", "Cutlasses", "symbols/sword.png", 3, 30, 110, 400),
            s("map", "Treasure Map", "symbols/scroll.png", 4, 20, 80, 250),
            s("emerald", "Emerald", "symbols/gem-emerald.png", 4, 16, 60, 180),
            s("A", "A", "symbols/royal-A.png", 5, 8, 26, 100),
            s("K", "K", "symbols/royal-K.png", 5, 8, 24, 90),
            s("Q", "Q", "symbols/royal-Q.png", 6, 5, 18, 70),
            s("J", "J", "symbols/royal-J.png", 6, 5, 15, 60),
            s("10", "10", "symbols/royal-10.png", 6, 4, 14, 50),
            wild("wild", "Goldbeak Wild", "hosts/goldbeak.png", 2),
            bonus("bonus", "Treasure Chest", "symbols/chest.png", 3));

    public static final Machine[] ALL = { LUCKY_PAWS, GOLDBEAK_COVE, HIGH_ROLLER };

    public static Machine byId(String id) {
        for (Machine m : ALL) if (m.id.equals(id)) return m;
        return LUCKY_PAWS;
    }
}
