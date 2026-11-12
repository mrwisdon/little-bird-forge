package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class RocketStrikeGridTest {
    @Test
    void theGridRotatesWithTheLauncherToTargetHeading() {
        RocketStrikeGrid south = RocketStrikeGrid.facing(0.0D, 100.0D, 180.0F);
        assertOffset(18.0D, 18.0D, south.pointOffset(0, 0));
        assertOffset(0.0D, 0.0D, south.pointOffset(3, 3));
        assertOffset(-18.0D, -18.0D, south.pointOffset(6, 6));
        assertOffset(0.0D, 12.0D, south.pointOffset(1, 3));

        RocketStrikeGrid diagonal = RocketStrikeGrid.facing(3.0D, 4.0D, 180.0F);
        assertOffset(25.2D, 3.6D, diagonal.pointOffset(0, 0));
        assertOffset(10.8D, 14.4D, diagonal.pointOffset(0, 3));
    }

    @Test
    void aCoincidentMarkUsesTheLauncherYaw() {
        RocketStrikeGrid west = RocketStrikeGrid.facing(0.0D, 0.0D, 90.0F);
        assertOffset(-18.0D, 0.0D, west.pointOffset(0, 3));
    }

    @Test
    void bothPassesHitEveryPointBeforeTheNextRow() {
        RocketStrikeGrid grid = RocketStrikeGrid.facing(3.0D, 4.0D, 0.0F);
        Set<ArtilleryStrikeRules.Offset> points = new HashSet<>();
        int size = RocketStrikeGrid.SIZE;
        assertEquals(98, RocketStrikeGrid.TOTAL_ROUNDS);
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                ArtilleryStrikeRules.Offset point = grid.pointOffset(row, column);
                int first = row * size * RocketStrikeGrid.ROUNDS_PER_POINT + column;
                assertTrue(points.add(point));
                assertEquals(point, grid.roundOffset(first));
                assertEquals(point, grid.roundOffset(first + size));
            }
        }
        assertEquals(49, points.size());
    }

    @Test
    void eachPointsSecondRoundIsDueTwentyTicksAfterItsFirst() {
        int duration = RocketStrikeGrid.DURATION_SECONDS;
        assertEquals(14, duration);
        int warmup = ArtilleryStrikeRules.DEFAULT_WARMUP_SECONDS;
        int firstTick = ArtilleryStrikeRules.warmupTicks(warmup);
        int endTick = ArtilleryStrikeRules.totalTicks(warmup, duration);
        List<Integer> launchTicks = new ArrayList<>();
        for (int tick = 0; tick <= endTick; tick++) {
            int due = ArtilleryStrikeRules.shellsDue(tick, warmup, duration, RocketStrikeGrid.TOTAL_ROUNDS);
            if (due > launchTicks.size()) {
                assertEquals(launchTicks.size() + 1, due);
                launchTicks.add(tick);
            }
        }
        assertEquals(RocketStrikeGrid.TOTAL_ROUNDS, launchTicks.size());
        assertEquals(firstTick, launchTicks.get(0));
        int size = RocketStrikeGrid.SIZE;
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int first = row * size * RocketStrikeGrid.ROUNDS_PER_POINT + column;
                assertEquals(20, launchTicks.get(first + size) - launchTicks.get(first));
            }
        }
    }

    private static void assertOffset(double dx, double dz, ArtilleryStrikeRules.Offset offset) {
        assertEquals(dx, offset.dx(), 1.0E-9D);
        assertEquals(dz, offset.dz(), 1.0E-9D);
    }
}
