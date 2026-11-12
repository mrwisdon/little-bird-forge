package io.github.littlebird.entity.weapon;

/**
 * Timing for called-in artillery and the 155mm barrage scatter pattern.
 *
 * <p>The crew marks a point on the radar and the battery answers. Nothing is calculated backwards
 * from the gun for this mission: the shells are not fired out of the hull in front of the player,
 * they arrive from off the map and fall straight down, so the only numbers that matter are when
 * each round appears, where it lands, and how long the caller has to wait before calling again.
 *
 * <p>Everything here is expressed in ticks and blocks and kept free of Minecraft classes, so the
 * client HUD, the server mission and the tests all read the same arithmetic.
 */
public final class ArtilleryStrikeRules {
    /** Call-in delay before the first shell: five seconds of warning for whoever is underneath. */
    public static final int DEFAULT_WARMUP_SECONDS = 5;
    /** Length of the barrage itself: ten seconds of fire, everything a battery can keep up. */
    public static final int DEFAULT_DURATION_SECONDS = 10;
    /** Rounds in one mission: thirty. */
    public static final int DEFAULT_SHELLS = 30;
    /** Wait after the last round before the battery will answer again: two minutes. */
    public static final int DEFAULT_COOLDOWN_SECONDS = 120;
    /** Radius the salvo covers around the marked point, in blocks. */
    public static final double DEFAULT_SPREAD_RADIUS = 15.0D;
    /** The first pair is kept inside this radius so the marked point is unquestionably hit. */
    public static final double CENTRE_PAIR_RADIUS = 1.5D;
    /** Height above the target the rounds appear at, in blocks: a long visible fall. */
    public static final double DEFAULT_DROP_HEIGHT = 90.0D;
    /** Downward speed a round starts with, blocks per tick. */
    public static final double DEFAULT_FALL_SPEED = 1.2D;
    /** The same gravity {@link Plz05Ballistics} flies its fired rounds with. */
    public static final double FALL_GRAVITY = 0.045D;
    /** Crater one barrage round tears: the same bowl an aimed 155mm shell leaves. */
    public static final double DEFAULT_CRATER_RADIUS = Plz05Ballistics.DEFAULT_CRATER_RADIUS;

    /** Golden angle: successive points never line up, so the salvo covers a disc evenly. */
    public static final double GOLDEN_ANGLE_DEGREES = 137.50776405003785D;

    /** Hard ceilings, so a misconfigured server cannot turn one call into a server stall. */
    public static final int MAX_SHELLS = 200;
    public static final int MAX_SECONDS = 120;
    public static final int MAX_DROP_HEIGHT = 320;
    public static final double MAX_SPREAD_RADIUS = 64.0D;

    /** Where a mission is in its life. */
    public enum Phase {
        /** No mission running. */
        IDLE,
        /** Called in, warning smoke going up, no shells yet. */
        WARMUP,
        /** Shells arriving. */
        BARRAGE,
        /** The last round has landed; only the cooldown is left. */
        DONE
    }

    /** One round's landing offset from the marked point, in blocks. */
    public record Offset(double dx, double dz) {
    }

    private ArtilleryStrikeRules() {
    }

    /** Seconds to ticks, clamped to the ceilings above. */
    public static int secondsToTicks(int seconds) {
        int clamped = Math.max(0, Math.min(MAX_SECONDS, seconds));
        return clamped * 20;
    }

    public static int warmupTicks(int warmupSeconds) {
        return secondsToTicks(warmupSeconds);
    }

    public static int durationTicks(int durationSeconds) {
        return Math.max(0, secondsToTicks(durationSeconds));
    }

    /** Warm-up plus barrage: the whole mission, in ticks. */
    public static int totalTicks(int warmupSeconds, int durationSeconds) {
        return warmupTicks(warmupSeconds) + durationTicks(durationSeconds);
    }

    public static int cooldownTicks(int cooldownSeconds) {
        return secondsToTicks(cooldownSeconds);
    }

    /** Clamped shell count, so the schedule and the entity agree. */
    public static int clampShells(int shells) {
        return Math.max(0, Math.min(MAX_SHELLS, shells));
    }

    public static int clampDropHeight(double dropHeight) {
        if (!Double.isFinite(dropHeight)) {
            return (int) DEFAULT_DROP_HEIGHT;
        }
        return (int) Math.max(20.0D, Math.min(MAX_DROP_HEIGHT, dropHeight));
    }

    public static double clampSpread(double spreadRadius) {
        if (!Double.isFinite(spreadRadius) || spreadRadius <= 0.0D) {
            return 0.0D;
        }
        return Math.min(MAX_SPREAD_RADIUS, spreadRadius);
    }

    /** Phase of a mission that has run for this many ticks. */
    public static Phase phaseAt(int elapsedTicks, int warmupSeconds, int durationSeconds) {
        if (elapsedTicks < 0) {
            return Phase.IDLE;
        }
        int warmup = warmupTicks(warmupSeconds);
        int duration = durationTicks(durationSeconds);
        if (elapsedTicks < warmup) {
            return Phase.WARMUP;
        }
        if (elapsedTicks < warmup + duration) {
            return Phase.BARRAGE;
        }
        return Phase.DONE;
    }

    /**
     * How many rounds should have left the battery by this tick.
     *
     * <p>The first round goes the instant the warm-up ends and the last one exactly at the end of
     * the barrage, so a thirty-round mission over two hundred ticks really does deliver thirty
     * rounds, six or seven ticks apart, instead of drifting a round short.
     */
    public static int shellsDue(int elapsedTicks, int warmupSeconds, int durationSeconds,
            int shells) {
        int capped = clampShells(shells);
        if (capped <= 0) {
            return 0;
        }
        int warmup = warmupTicks(warmupSeconds);
        int duration = durationTicks(durationSeconds);
        if (elapsedTicks < warmup) {
            return 0;
        }
        if (duration <= 0) {
            return capped;
        }
        long fired = (long) (elapsedTicks - warmup) * capped / duration + 1L;
        return (int) Math.min(capped, fired);
    }

    /** 0..1 progress through the whole mission, for the HUD bar. */
    public static double progress(int elapsedTicks, int warmupSeconds, int durationSeconds) {
        int total = totalTicks(warmupSeconds, durationSeconds);
        if (total <= 0) {
            return 1.0D;
        }
        double fraction = elapsedTicks / (double) total;
        return Math.max(0.0D, Math.min(1.0D, fraction));
    }

    /** Average gap between two rounds, in ticks; used for the HUD readout and for the tests. */
    public static int ticksBetweenShells(int durationSeconds, int shells) {
        int capped = clampShells(shells);
        if (capped <= 1) {
            return durationTicks(durationSeconds);
        }
        return Math.round(durationTicks(durationSeconds) / (float) (capped - 1));
    }

    /**
     * Where round {@code index} lands, relative to the marked point.
     *
     * <p>The first two rounds are pulled in tight so the point the crew clicked is hit even if the
     * rest of the salvo is walking around; the remainder is laid on a golden-angle spiral whose
     * radius grows as the square root of the round number, which is the cheapest way to scatter
     * points evenly over a disc instead of clumping them near the middle.
     */
    public static Offset scatterOffset(int index, int shells, double spreadRadius) {
        int capped = Math.max(1, clampShells(shells));
        double spread = clampSpread(spreadRadius);
        int safeIndex = Math.max(0, index);
        double radius;
        if (safeIndex < 2 && spread > 0.0D) {
            radius = CENTRE_PAIR_RADIUS * (safeIndex == 0 ? 0.55D : 0.85D);
        } else {
            radius = spread * Math.sqrt((safeIndex + 0.5D) / capped);
        }
        double angle = Math.toRadians(GOLDEN_ANGLE_DEGREES * safeIndex);
        return new Offset(Math.cos(angle) * radius, Math.sin(angle) * radius);
    }

    /** True when a landing offset is inside a picture of this radius: the client only sends these. */
    public static boolean inRange(double dx, double dz, double maxRange) {
        if (!Double.isFinite(dx) || !Double.isFinite(dz) || !Double.isFinite(maxRange)
                || maxRange <= 0.0D) {
            return false;
        }
        return dx * dx + dz * dz <= maxRange * maxRange;
    }

    /**
     * Ticks a round takes to fall the drop height, stepping the same velocity-then-move order the
     * shell entity uses, so the client can warn with the right numbers.
     */
    public static int fallTicks(double dropHeight, double fallSpeed) {
        int height = clampDropHeight(dropHeight);
        double speed = Double.isFinite(fallSpeed) ? Math.max(0.05D, fallSpeed) : DEFAULT_FALL_SPEED;
        double travelled = 0.0D;
        int ticks = 0;
        while (travelled < height && ticks < 4000) {
            speed += FALL_GRAVITY;
            travelled += speed;
            ticks++;
        }
        return ticks;
    }
}
