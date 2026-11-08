package com.reelriot.vegas.slots;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;

import java.util.Random;

/**
 * Spins a machine: weighted reel strips, 20 paylines with wilds, the bonus
 * trigger, and the bonus prize. Bonus prizes are decided here, before the
 * bonus game is shown, so their odds are exact and testable.
 */
public final class SlotEngine {
    public final Machine m;
    public final int[][] strips = new int[Machine.REELS][];
    private final Random rng;
    private final int bonusIdx, wildIdx;

    /** Bonus prize table: multiples of the bet with their weights, and which jackpot (if any) each is. */
    public static final class Prize {
        public final float x;
        public final int weight, jackpot, ring;

        Prize(float x, int weight, int jackpot, int ring) {
            this.x = x;
            this.weight = weight;
            this.jackpot = jackpot;
            this.ring = ring;
        }
    }

    public static final class LineWin {
        public int line, sym, count;
        public long amount;
    }

    public static final class Result {
        public final int[] stops = new int[Machine.REELS];
        public final int[][] grid = new int[Machine.REELS][Machine.ROWS];
        public final Array<LineWin> lines = new Array<>();
        public long lineWin;
        public boolean bonus;
        public Prize prize;
        public long bonusWin;

        public long total() { return lineWin + bonusWin; }
    }

    public SlotEngine(Machine m, long seed) {
        this.m = m;
        this.rng = new Random(seed);
        bonusIdx = m.index("bonus");
        wildIdx = m.index("wild");
        Random build = new Random(m.id.hashCode() * 31L + 7);
        for (int r = 0; r < Machine.REELS; r++) strips[r] = buildStrip(r, build);
    }

    private int[] buildStrip(int reel, Random build) {
        IntArray list = new IntArray();
        for (int i = 0; i < m.syms.length; i++) {
            Machine.Sym s = m.syms[i];
            if (s.wild && (reel == 0 || reel == 4)) continue;
            if (s.bonus && reel % 2 == 1) continue;
            for (int k = 0; k < s.weight; k++) list.add(i);
        }
        // shuffle until no two bonus or wild symbols sit within 3 places of each other
        for (int attempt = 0; attempt < 5000; attempt++) {
            for (int i = list.size - 1; i > 0; i--) {
                int j = build.nextInt(i + 1);
                int t = list.get(i);
                list.set(i, list.get(j));
                list.set(j, t);
            }
            if (spaced(list)) break;
        }
        return list.toArray();
    }

    private boolean spaced(IntArray l) {
        int n = l.size;
        for (int i = 0; i < n; i++) {
            int a = l.get(i);
            if (a != bonusIdx && a != wildIdx) continue;
            for (int d = 1; d <= 3; d++) if (l.get((i + d) % n) == a) return false;
        }
        return true;
    }

    public static final Prize[] LUCKY_DROP = {
        new Prize(Machine.JACKPOT_X[0], 592, 0, 0), new Prize(Machine.JACKPOT_X[1], 300, 1, 0),
        new Prize(Machine.JACKPOT_X[2], 100, 2, 0), new Prize(Machine.JACKPOT_X[3], 8, 3, 0)
    };
    /** Mega Wheel: ring 0 = outer, 1 = middle (via UPGRADE), 2 = center. */
    public static final Prize[] MEGA_WHEEL = {
        new Prize(5, 120, -1, 0), new Prize(8, 160, -1, 0), new Prize(10, 160, -1, 0), new Prize(15, 140, -1, 0), new Prize(20, 120, -1, 0),
        new Prize(Machine.JACKPOT_X[1], 120, 1, 1), new Prize(40, 80, -1, 1), new Prize(60, 50, -1, 1),
        new Prize(Machine.JACKPOT_X[2], 40, 2, 2), new Prize(Machine.JACKPOT_X[3], 10, 3, 2)
    };
    public static final Prize[] TREASURE = {
        new Prize(Machine.JACKPOT_X[0], 400, 0, 0), new Prize(20, 300, -1, 0), new Prize(Machine.JACKPOT_X[1], 120, 1, 0),
        new Prize(40, 100, -1, 0), new Prize(Machine.JACKPOT_X[2], 75, 2, 0), new Prize(Machine.JACKPOT_X[3], 5, 3, 0)
    };

    public Prize[] prizes() {
        switch (m.bonus) {
            case MEGA_WHEEL: return MEGA_WHEEL;
            case TREASURE: return TREASURE;
            default: return LUCKY_DROP;
        }
    }

    public Prize rollPrize() {
        Prize[] p = prizes();
        int total = 0;
        for (Prize x : p) total += x.weight;
        int k = rng.nextInt(total);
        for (Prize x : p) { if (k < x.weight) return x; k -= x.weight; }
        return p[0];
    }

    public float random() { return rng.nextFloat(); }

    public int randomInt(int n) { return rng.nextInt(n); }

    /** One paid spin. boost > 1 adds bonus triggers, < 1 removes some (test control). */
    public Result spin(long bet, float boost) {
        Result r = new Result();
        roll(r);
        if (boost > 1f && !r.bonus && rng.nextFloat() < (boost - 1f) * baseTrigger()) {
            for (int i = 0; i < 20000 && !r.bonus; i++) roll(r);
        } else if (boost < 1f && r.bonus && rng.nextFloat() > boost) {
            for (int i = 0; i < 200 && r.bonus; i++) roll(r);
        }
        settle(r, bet);
        return r;
    }

    /** A spin that satisfies a test condition ("bonus", "big", "mega", "epic", "lose", "win"). */
    public Result forced(long bet, String what) {
        Result r = new Result();
        for (int i = 0; i < 400_000; i++) {
            roll(r);
            settle(r, bet);
            float x = r.lineWin / (float) bet;
            boolean ok;
            switch (what) {
                case "bonus": ok = r.bonus; break;
                case "big": ok = !r.bonus && x >= 10 && x < 25; break;
                case "mega": ok = !r.bonus && x >= 25 && x < 50; break;
                case "epic": ok = !r.bonus && x >= 50; break;
                case "lose": ok = !r.bonus && r.lineWin == 0; break;
                case "tease": ok = !r.bonus && teaser(r); break;
                default: ok = r.lineWin > 0 && !r.bonus; break;
            }
            if (ok) return r;
        }
        return r;
    }

    /** Two bonus symbols on reels 1 and 3 but not on 5. */
    private boolean teaser(Result r) { return count(r, 0) && count(r, 2) && !count(r, 4); }

    private boolean count(Result r, int reel) {
        for (int y = 0; y < Machine.ROWS; y++) if (r.grid[reel][y] == bonusIdx) return true;
        return false;
    }

    /** True when the first two bonus reels already show a bonus symbol. */
    public boolean anticipation(Result r) { return count(r, 0) && count(r, 2); }

    private void roll(Result r) {
        for (int reel = 0; reel < Machine.REELS; reel++) {
            int[] s = strips[reel];
            int stop = rng.nextInt(s.length);
            r.stops[reel] = stop;
            for (int y = 0; y < Machine.ROWS; y++) r.grid[reel][y] = s[(stop - 1 + y + s.length) % s.length];
        }
        r.bonus = count(r, 0) && count(r, 2) && count(r, 4);
    }

    private void settle(Result r, long bet) {
        r.lines.clear();
        r.lineWin = 0;
        long lineBet = bet / Machine.LINES;
        for (int l = 0; l < Machine.LINES; l++) {
            int[] rows = Machine.PAYLINES[l];
            int target = -1;
            for (int reel = 0; reel < Machine.REELS; reel++) {
                int s = r.grid[reel][rows[reel]];
                if (s != wildIdx) { target = s; break; }
            }
            if (target < 0 || target == bonusIdx) continue;
            int n = 0;
            for (int reel = 0; reel < Machine.REELS; reel++) {
                int s = r.grid[reel][rows[reel]];
                if (s == target || s == wildIdx) n++;
                else break;
            }
            int pay = m.syms[target].pays[Math.min(n, 5)];
            if (n >= 3 && pay > 0) {
                LineWin w = new LineWin();
                w.line = l;
                w.sym = target;
                w.count = n;
                w.amount = lineBet * pay;
                r.lines.add(w);
                r.lineWin += w.amount;
            }
        }
        if (r.bonus) {
            r.prize = rollPrize();
            r.bonusWin = Math.round(bet * r.prize.x);
        } else {
            r.prize = null;
            r.bonusWin = 0;
        }
    }

    /** Chance of the bonus on one spin, from the strips. */
    public float baseTrigger() {
        float p = 1;
        for (int reel = 0; reel < 5; reel += 2) {
            int n = 0;
            for (int s : strips[reel]) if (s == bonusIdx) n++;
            p *= 3f * n / strips[reel].length;
        }
        return p;
    }

    /** Expected bonus prize in bets. */
    public float bonusMean() {
        float sum = 0, w = 0;
        for (Prize p : prizes()) { sum += p.x * p.weight; w += p.weight; }
        return sum / w;
    }
}
