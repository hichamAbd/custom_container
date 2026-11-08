package com.reelriot.vegas.slots;

/** Measures each machine's return by simulating spins (desktop: --sim [spins]). */
public final class SlotSim {
    private SlotSim() { }

    public static void run(int spins) {
        for (Machine m : Machine.ALL) {
            SlotEngine e = new SlotEngine(m, 12345);
            long bet = 100_000, wagered = 0, line = 0, bonus = 0, hits = 0, bonusCount = 0, big = 0, mega = 0;
            for (int i = 0; i < spins; i++) {
                SlotEngine.Result r = e.spin(bet, 1f);
                wagered += bet;
                line += r.lineWin;
                bonus += r.bonusWin;
                if (r.total() > 0) hits++;
                if (r.bonus) bonusCount++;
                if (r.lineWin >= bet * 10) big++;
                if (r.lineWin >= bet * 25) mega++;
            }
            System.out.printf("%-20s base %.2f%%  bonus %.2f%% (1 in %.0f, mean %.1fx)  total %.2f%%  hits %.1f%%  big 1/%.0f  mega 1/%.0f  strips %d/%d/%d/%d/%d%n",
                    m.name, 100.0 * line / wagered, 100.0 * bonus / wagered, (double) spins / Math.max(1, bonusCount), e.bonusMean(),
                    100.0 * (line + bonus) / wagered, 100.0 * hits / spins, (double) spins / Math.max(1, big), (double) spins / Math.max(1, mega),
                    e.strips[0].length, e.strips[1].length, e.strips[2].length, e.strips[3].length, e.strips[4].length);
        }
    }
}
