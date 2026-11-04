package de.tenkayro.client;

public final class Milestones {
    private Milestones() {}
    /** Return only the highest newly crossed threshold, without replaying old milestones on join. */
    public static long crossed(long previous, long current) {
        long before = previous / 3_600_000L;
        long after = current / 3_600_000L;
        long highest = after / 1000 * 1000;
        if (highest >= 1000 && highest > before) return highest;
        if (after >= 500 && before < 500) return 500;
        if (after >= 100 && before < 100) return 100;
        if (after >= 10 && before < 10) return 10;
        return 0;
    }
}
