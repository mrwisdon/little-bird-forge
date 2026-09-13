package io.github.littlebird.entity.weapon;

/**
 * Loadout maths for the AH-64's two 19 tube rocket pods.
 *
 * The shared ready count is what the crew can fire before reloading, and the wings alternate,
 * so each pod's remaining rounds are derived from it rather than tracked separately.
 * Pure rules: no Minecraft classes.
 */
public final class ApachePods {
    public static final int PODS = 2;
    /** Rounds the crew can fire before a reload; the reserve holds as many again. */
    public static final int READY_CAPACITY = 19;

    private ApachePods() {
    }

    /** Left pod takes the odd round: 10 tubes, the right one 9. */
    public static int podCapacity(int pod) {
        return pod == 0 ? (READY_CAPACITY + 1) / 2 : READY_CAPACITY / 2;
    }

    /** Rounds left in one pod. The two pods always add up to the shared ready count. */
    public static int podReady(int readyRemaining, int pod) {
        int fired = Math.max(0, Math.min(READY_CAPACITY, READY_CAPACITY - readyRemaining));
        int leftFired = (fired + 1) / 2;
        int rightFired = fired / 2;
        return podCapacity(pod) - (pod == 0 ? leftFired : rightFired);
    }

    /**
     * Signed lateral offset of the pod that fires next, in blocks: negative is the left wing.
     * The server launches from here and the client predicts the impact from the same place, so
     * the reticle tells the truth instead of drifting a pod's width off the real rockets.
     */
    public static double launchLateralBlocks(int readyRemaining, double lateralBlocks) {
        return nextPod(readyRemaining) == 0 ? -lateralBlocks : lateralBlocks;
    }

    // ------------------------------------------------------------------
    // Rocket reticle dots
    // ------------------------------------------------------------------
    /** Indicator dot grid: two columns, at most five rows. */
    public static final int HUD_COLUMNS = 2;
    public static final int HUD_ROWS = 5;

    /** column 0 is the outer column, row 0 the top, as on the reference reticle. */
    public record DotPosition(int column, int row) {
    }

    /** Tubes in one pod, which is also how many indicator dots it lights. */
    public static int tubeCount(int pod) {
        return podCapacity(pod);
    }

    /** True while that tube still holds a round; tubes fire from index 0 outwards. */
    public static boolean tubeLoaded(int pod, int tube, int readyRemaining) {
        return tube >= 0 && tube < tubeCount(pod) && tube < podReady(readyRemaining, pod);
    }

    /**
     * Where a tube's indicator dot sits. The grid empties left to right and top to bottom, so
     * the tube that fires first owns the dot that goes out last. The ten tube pod fills the
     * whole 2 by 5 grid; the nine tube pod keeps the reference reticle's empty cell in the
     * inner column's top row, which is exactly the round it is short by.
     */
    public static DotPosition tubeDot(int pod, int tube) {
        int count = tubeCount(pod);
        if (tube < 0 || tube >= count) {
            throw new IllegalArgumentException("tube out of range: " + tube);
        }
        int position = count - 1 - tube;
        if (count == HUD_COLUMNS * HUD_ROWS) {
            return new DotPosition(position % HUD_COLUMNS, position / HUD_COLUMNS);
        }
        if (position == 0) {
            return new DotPosition(0, 0);
        }
        return new DotPosition((position - 1) % HUD_COLUMNS, 1 + (position - 1) / HUD_COLUMNS);
    }

    /** Index of the pod that fires next; the wings alternate, starting on the left. */
    public static int nextPod(int readyRemaining) {
        int fired = Math.max(0, Math.min(READY_CAPACITY, READY_CAPACITY - readyRemaining));
        return fired % PODS;
    }
}
