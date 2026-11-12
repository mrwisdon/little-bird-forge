package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pins the pure arithmetic of the called-in 155mm barrage: when rounds are due, which phase the
 * mission is in, where the salvo lands and how long a caller waits before calling again.
 *
 * <p>The defaults used throughout are the shipped ones: five seconds of warm-up, ten seconds of
 * fire, thirty rounds. That mission is 100 warm-up ticks plus 200 barrage ticks.
 */
final class ArtilleryStrikeRulesTest {
    private static final double EPSILON = 1.0E-9D;

    private static final int WARMUP_SECONDS = ArtilleryStrikeRules.DEFAULT_WARMUP_SECONDS;
    private static final int DURATION_SECONDS = ArtilleryStrikeRules.DEFAULT_DURATION_SECONDS;
    private static final int SHELLS = ArtilleryStrikeRules.DEFAULT_SHELLS;
    private static final int WARMUP_TICKS = 100;
    private static final int DURATION_TICKS = 200;
    private static final int TOTAL_TICKS = 300;

    // ---------------------------------------------------------------- shellsDue

    @Test
    void shellsDueIsZeroUntilTheWarmupEnds() {
        assertEquals(0, ArtilleryStrikeRules.shellsDue(0, WARMUP_SECONDS, DURATION_SECONDS, SHELLS),
                "nothing is due at the call-in tick");
        assertEquals(0, ArtilleryStrikeRules.shellsDue(99, WARMUP_SECONDS, DURATION_SECONDS, SHELLS),
                "nothing is due one tick before the warm-up ends");
        assertEquals(1, ArtilleryStrikeRules.shellsDue(100, WARMUP_SECONDS, DURATION_SECONDS, SHELLS),
                "the first round goes the instant the warm-up ends");
    }

    @Test
    void theWholeSalvoHasFiredByTheEndOfTheBarrage() {
        assertEquals(SHELLS,
                ArtilleryStrikeRules.shellsDue(WARMUP_TICKS + DURATION_TICKS,
                        WARMUP_SECONDS, DURATION_SECONDS, SHELLS),
                "the last round goes exactly at warm-up + duration");
        assertEquals(SHELLS,
                ArtilleryStrikeRules.shellsDue(WARMUP_TICKS + DURATION_TICKS + 40,
                        WARMUP_SECONDS, DURATION_SECONDS, SHELLS),
                "a finished mission never fires more than its shell count");
    }

    @Test
    void shellsDueNeverGoesBackwards() {
        int previous = -1;
        for (int elapsed = 0; elapsed <= 600; elapsed++) {
            int due = ArtilleryStrikeRules.shellsDue(
                    elapsed, WARMUP_SECONDS, DURATION_SECONDS, SHELLS);
            assertTrue(due >= previous,
                    "shells due dropped at tick " + elapsed + ": " + previous + " -> " + due);
            assertTrue(due >= 0 && due <= SHELLS,
                    "shells due out of range at tick " + elapsed + ": " + due);
            previous = due;
        }
    }

    @Test
    void successiveRoundsAreSixOrSevenTicksApart() {
        List<Integer> firingTicks = new ArrayList<>();
        int previous = 0;
        for (int elapsed = 0; elapsed <= TOTAL_TICKS; elapsed++) {
            int due = ArtilleryStrikeRules.shellsDue(
                    elapsed, WARMUP_SECONDS, DURATION_SECONDS, SHELLS);
            if (due > previous) {
                assertEquals(previous + 1, due,
                        "two rounds left the battery in the same tick at " + elapsed);
                firingTicks.add(elapsed);
            }
            previous = due;
        }
        assertEquals(SHELLS, firingTicks.size(), "firing ticks seen: " + firingTicks);
        for (int i = 1; i < firingTicks.size(); i++) {
            int gap = firingTicks.get(i) - firingTicks.get(i - 1);
            assertTrue(gap >= 6 && gap <= 7,
                    "round " + i + " came " + gap + " ticks after round " + (i - 1)
                            + ": ticks=" + firingTicks);
        }
        assertTrue(firingTicks.get(firingTicks.size() - 1) <= TOTAL_TICKS,
                "the last round must land inside the barrage, was at " + firingTicks);
    }

    @Test
    void aZeroDurationBarrageFiresEverythingAtTheWarmupTick() {
        assertEquals(0, ArtilleryStrikeRules.shellsDue(99, WARMUP_SECONDS, 0, SHELLS),
                "still nothing before the warm-up ends");
        assertEquals(SHELLS, ArtilleryStrikeRules.shellsDue(100, WARMUP_SECONDS, 0, SHELLS),
                "a zero-length barrage is complete the instant the warm-up ends");
        assertEquals(SHELLS, ArtilleryStrikeRules.shellsDue(160, WARMUP_SECONDS, 0, SHELLS),
                "and it stays complete");
    }

    // ---------------------------------------------------------------- phaseAt

    @Test
    void phaseAtSitsOnTheWarmupBarrageAndDoneBoundaries() {
        assertEquals(ArtilleryStrikeRules.Phase.IDLE,
                ArtilleryStrikeRules.phaseAt(-1, WARMUP_SECONDS, DURATION_SECONDS),
                "a mission that has not started is idle");
        assertEquals(ArtilleryStrikeRules.Phase.WARMUP,
                ArtilleryStrikeRules.phaseAt(0, WARMUP_SECONDS, DURATION_SECONDS));
        assertEquals(ArtilleryStrikeRules.Phase.WARMUP,
                ArtilleryStrikeRules.phaseAt(99, WARMUP_SECONDS, DURATION_SECONDS));
        assertEquals(ArtilleryStrikeRules.Phase.BARRAGE,
                ArtilleryStrikeRules.phaseAt(100, WARMUP_SECONDS, DURATION_SECONDS));
        assertEquals(ArtilleryStrikeRules.Phase.BARRAGE,
                ArtilleryStrikeRules.phaseAt(299, WARMUP_SECONDS, DURATION_SECONDS));
        assertEquals(ArtilleryStrikeRules.Phase.DONE,
                ArtilleryStrikeRules.phaseAt(300, WARMUP_SECONDS, DURATION_SECONDS));
    }

    // ---------------------------------------------------------------- scatterOffset

    @Test
    void everyRoundLandsInsideTheClampedSpreadDisc() {
        double spread = ArtilleryStrikeRules.clampSpread(ArtilleryStrikeRules.DEFAULT_SPREAD_RADIUS);
        assertEquals(15.0D, spread, EPSILON);
        for (int index = 0; index < SHELLS; index++) {
            ArtilleryStrikeRules.Offset offset =
                    ArtilleryStrikeRules.scatterOffset(index, SHELLS, spread);
            double distanceSquared = offset.dx() * offset.dx() + offset.dz() * offset.dz();
            assertTrue(distanceSquared <= spread * spread + EPSILON,
                    "round " + index + " landed outside the disc: distance^2=" + distanceSquared
                            + " > " + spread * spread);
        }
    }

    @Test
    void theFirstTwoRoundsHitTheMarkedPointTightly() {
        for (int index = 0; index < 2; index++) {
            ArtilleryStrikeRules.Offset offset =
                    ArtilleryStrikeRules.scatterOffset(index, SHELLS,
                            ArtilleryStrikeRules.DEFAULT_SPREAD_RADIUS);
            double distance = Math.hypot(offset.dx(), offset.dz());
            assertTrue(distance <= ArtilleryStrikeRules.CENTRE_PAIR_RADIUS + EPSILON,
                    "round " + index + " is " + distance + " blocks from the mark, radius limit is "
                            + ArtilleryStrikeRules.CENTRE_PAIR_RADIUS);
        }
    }

    @Test
    void scatterOffsetIsDeterministic() {
        for (int index = 0; index < SHELLS; index++) {
            ArtilleryStrikeRules.Offset first =
                    ArtilleryStrikeRules.scatterOffset(index, SHELLS,
                            ArtilleryStrikeRules.DEFAULT_SPREAD_RADIUS);
            ArtilleryStrikeRules.Offset second =
                    ArtilleryStrikeRules.scatterOffset(index, SHELLS,
                            ArtilleryStrikeRules.DEFAULT_SPREAD_RADIUS);
            assertEquals(first.dx(), second.dx(), EPSILON,
                    "dx changed between identical calls for round " + index);
            assertEquals(first.dz(), second.dz(), EPSILON,
                    "dz changed between identical calls for round " + index);
        }
    }

    @Test
    void theThirtyRoundsLandOnThirtyDistinctPoints() {
        Set<ArtilleryStrikeRules.Offset> points = new HashSet<>();
        for (int index = 0; index < SHELLS; index++) {
            points.add(ArtilleryStrikeRules.scatterOffset(index, SHELLS,
                    ArtilleryStrikeRules.DEFAULT_SPREAD_RADIUS));
        }
        assertEquals(SHELLS, points.size(), "duplicate landing points: " + points);
    }

    @Test
    void theSalvoCoversAllFourQuadrants() {
        boolean northEast = false;
        boolean southWest = false;
        boolean southEast = false;
        boolean northWest = false;
        for (int index = 0; index < SHELLS; index++) {
            ArtilleryStrikeRules.Offset offset = ArtilleryStrikeRules.scatterOffset(index, SHELLS,
                    ArtilleryStrikeRules.DEFAULT_SPREAD_RADIUS);
            double dx = offset.dx();
            double dz = offset.dz();
            if (dx > 0.0D && dz > 0.0D) {
                northEast = true;
            }
            if (dx < 0.0D && dz < 0.0D) {
                southWest = true;
            }
            if (dx > 0.0D && dz < 0.0D) {
                southEast = true;
            }
            if (dx < 0.0D && dz > 0.0D) {
                northWest = true;
            }
        }
        assertTrue(northEast, "no round in the +x/+z quadrant");
        assertTrue(southWest, "no round in the -x/-z quadrant");
        assertTrue(southEast, "no round in the +x/-z quadrant");
        assertTrue(northWest, "no round in the -x/+z quadrant");
    }

    // ---------------------------------------------------------------- inRange

    @Test
    void inRangeAcceptsOnlyFinitePointsInsideAPositiveRadius() {
        assertTrue(ArtilleryStrikeRules.inRange(0.0D, 0.0D, 200.0D),
                "the mark itself is in range");
        assertTrue(ArtilleryStrikeRules.inRange(200.0D, 0.0D, 200.0D),
                "the edge of the picture is in range");
        assertFalse(ArtilleryStrikeRules.inRange(200.5D, 0.0D, 200.0D),
                "just outside the picture is rejected");
        assertFalse(ArtilleryStrikeRules.inRange(Double.NaN, 0.0D, 200.0D),
                "a NaN offset is rejected");
        assertFalse(ArtilleryStrikeRules.inRange(0.0D, 0.0D, -1.0D),
                "a negative range is rejected");
    }

    // ---------------------------------------------------------------- clamps and timings

    @Test
    void shellCountsAndDropHeightsAreClamped() {
        assertEquals(ArtilleryStrikeRules.MAX_SHELLS, ArtilleryStrikeRules.clampShells(1000),
                "shell count is capped at " + ArtilleryStrikeRules.MAX_SHELLS);
        assertEquals(0, ArtilleryStrikeRules.clampShells(-5), "a negative shell count is zero");
        assertEquals(ArtilleryStrikeRules.MAX_DROP_HEIGHT,
                ArtilleryStrikeRules.clampDropHeight(1000.0D),
                "drop height is capped at " + ArtilleryStrikeRules.MAX_DROP_HEIGHT);
        assertEquals(20, ArtilleryStrikeRules.clampDropHeight(5.0D),
                "drop height never goes below 20 blocks");
    }

    @Test
    void spreadRadiusIsClampedToTheCeiling() {
        assertEquals(0.0D, ArtilleryStrikeRules.clampSpread(-1.0D), EPSILON,
                "a negative radius collapses to zero");
        assertEquals(ArtilleryStrikeRules.MAX_SPREAD_RADIUS,
                ArtilleryStrikeRules.clampSpread(1000.0D), EPSILON,
                "spread radius is capped at " + ArtilleryStrikeRules.MAX_SPREAD_RADIUS);
    }

    @Test
    void theTimingsMatchTheShippedDefaults() {
        assertEquals(100, ArtilleryStrikeRules.secondsToTicks(5), "five seconds is 100 ticks");
        assertEquals(2400, ArtilleryStrikeRules.cooldownTicks(120),
                "the two minute cooldown is 2400 ticks");
        assertEquals(300, ArtilleryStrikeRules.totalTicks(5, 10),
                "warm-up plus barrage is 300 ticks");
        assertEquals(7, ArtilleryStrikeRules.ticksBetweenShells(10, 30),
                "200 ticks over 29 gaps rounds to 7");
    }

    @Test
    void progressRunsFromZeroToOne() {
        assertEquals(0.0D, ArtilleryStrikeRules.progress(0, 5, 20), EPSILON,
                "a fresh mission is at zero progress");
        assertEquals(1.0D, ArtilleryStrikeRules.progress(300, 5, 10), EPSILON,
                "the finished mission is at full progress");
        assertNotEquals(0.0D, ArtilleryStrikeRules.progress(150, 5, 10), EPSILON,
                "halfway through is not still zero");
    }

    // ---------------------------------------------------------------- fallTicks

    @Test
    void fallTicksMatchesTheVisibleFallFromNinetyBlocks() {
        int fall = ArtilleryStrikeRules.fallTicks(90.0D, 1.2D);
        assertTrue(fall >= 30 && fall <= 50,
                "a 90 block fall takes 30..50 ticks, was " + fall);
        int longFall = ArtilleryStrikeRules.fallTicks(200.0D, 1.2D);
        assertTrue(longFall > fall,
                "a 200 block fall must take longer: 200 -> " + longFall + ", 90 -> " + fall);
    }
}
