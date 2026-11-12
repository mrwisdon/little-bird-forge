package io.github.littlebird.entity.weapon;

import java.util.Objects;

/** Orients a PHL-191 barrage grid along the line from the launcher to the marked centre. */
public final class RocketStrikeGrid {
    public static final int SIZE = 7;
    public static final int ROUNDS_PER_POINT = 2;
    public static final int TOTAL_ROUNDS = SIZE * SIZE * ROUNDS_PER_POINT;
    public static final int DURATION_SECONDS = 14;
    public static final double SPACING_BLOCKS = 6.0D;

    private final double forwardX;
    private final double forwardZ;

    private RocketStrikeGrid(double forwardX, double forwardZ) {
        this.forwardX = forwardX;
        this.forwardZ = forwardZ;
    }

    /** Uses the launcher's yaw when the mark is directly above or below the launcher. */
    public static RocketStrikeGrid facing(double targetDx, double targetDz, float fallbackYawDeg) {
        double distance = Math.hypot(targetDx, targetDz);
        if (distance > 0.0D && Double.isFinite(distance)) {
            return new RocketStrikeGrid(targetDx / distance, targetDz / distance);
        }
        double yaw = Math.toRadians(fallbackYawDeg);
        return new RocketStrikeGrid(-Math.sin(yaw), Math.cos(yaw));
    }

    /** Offset from the mark, with rows ordered from farthest to nearest. */
    public ArtilleryStrikeRules.Offset pointOffset(int row, int column) {
        Objects.checkIndex(row, SIZE);
        Objects.checkIndex(column, SIZE);
        int centre = SIZE / 2;
        double forward = (centre - row) * SPACING_BLOCKS;
        double lateral = (column - centre) * SPACING_BLOCKS;
        return new ArtilleryStrikeRules.Offset(forwardX * forward - forwardZ * lateral,
                forwardZ * forward + forwardX * lateral);
    }

    /** Visits every column in a row twice before moving to the next row. */
    public ArtilleryStrikeRules.Offset roundOffset(int index) {
        Objects.checkIndex(index, TOTAL_ROUNDS);
        int row = index / (SIZE * ROUNDS_PER_POINT);
        return pointOffset(row, index % SIZE);
    }
}
