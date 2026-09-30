package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Rigid tracer geometry shared by every shell that flies with a streak. */
class ShellTracerTrailTest {
    private static final double EPSILON = 1.0E-9D;

    @BeforeEach
    void clearTrail() {
        ShellTracerTrail.reset();
    }

    private static double totalLength(List<ShellTracerTrail.Segment> segments) {
        double total = 0.0D;
        for (ShellTracerTrail.Segment segment : segments) {
            total += segment.length();
        }
        return total;
    }

    @Test
    void historyKeepsOnlyTheNewestFrames() {
        for (int frame = 0; frame < 20; frame++) {
            ShellTracerTrail.track(1, frame, frame * 0.1D, 0.0D, 0.0D);
        }
        assertEquals(ShellTracerTrail.MAX_FRAMES, ShellTracerTrail.sampleCount(1));
        List<ShellTracerTrail.Segment> segments =
                ShellTracerTrail.segments(1, 100.0D, 1.0D, 0.0D, 0.0D);
        assertEquals(ShellTracerTrail.MAX_FRAMES - 1, segments.size());
        assertEquals(1.9D, segments.get(0).bx(), EPSILON, "the head is the newest drawn position");
        assertEquals(1.8D, segments.get(0).ax(), EPSILON);
        assertEquals(1.2D, segments.get(segments.size() - 1).ax(), EPSILON,
                "the oldest kept frame is the far end");
    }

    @Test
    void lengthBudgetClipsTheStreakAndTapersIt() {
        for (int frame = 0; frame < 8; frame++) {
            ShellTracerTrail.track(2, frame, frame, 0.0D, 0.0D);
        }
        List<ShellTracerTrail.Segment> segments =
                ShellTracerTrail.segments(2, 6.0D, 1.0D, 0.0D, 0.0D);
        assertEquals(6.0D, totalLength(segments), EPSILON, "7 blocks of path clipped to 6");
        assertEquals(7.0D, segments.get(0).bx(), EPSILON, "the head stays on the shell");
        assertEquals(1.0D, segments.get(segments.size() - 1).ax(), EPSILON,
                "the far end is clipped to the budget");
        assertEquals(ShellTracerTrail.HEAD_BRIGHTNESS, segments.get(0).brightness(), EPSILON);
        assertTrue(segments.get(segments.size() - 1).brightness()
                < ShellTracerTrail.HEAD_BRIGHTNESS, "the far end dims towards the tail");
        for (ShellTracerTrail.Segment segment : segments) {
            assertEquals(ShellTracerTrail.HALF_WIDTH, segment.halfWidth(), EPSILON,
                    "constant width is what makes the bar rigid");
        }
    }

    @Test
    void nominalMuzzleSpeedIsNotATeleport() {
        // 5 blocks/tick is the round's real speed; treating it as a jump would mean no
        // streak ever survives one frame.
        for (int frame = 0; frame < 6; frame++) {
            ShellTracerTrail.track(3, frame, frame * 5.0D, 0.0D, 0.0D);
        }
        assertEquals(6, ShellTracerTrail.sampleCount(3));
        List<ShellTracerTrail.Segment> segments =
                ShellTracerTrail.segments(3, 6.0D, 1.0D, 0.0D, 0.0D);
        assertEquals(6.0D, totalLength(segments), EPSILON);
        assertEquals(25.0D, segments.get(0).bx(), EPSILON);
        // A slow frame is discounted too: 30 blocks over 3 ticks is only 10 blocks/tick.
        ShellTracerTrail.track(4, 0.0D, 0.0D, 0.0D, 0.0D);
        ShellTracerTrail.track(4, 3.0D, 30.0D, 0.0D, 0.0D);
        assertEquals(2, ShellTracerTrail.sampleCount(4));
    }

    @Test
    void teleportResetsTheHistoryInsteadOfDrawingAcrossTheMap() {
        for (int frame = 0; frame < 5; frame++) {
            ShellTracerTrail.track(5, frame, frame, 0.0D, 0.0D);
        }
        ShellTracerTrail.track(5, 5.0D, 900.0D, 0.0D, 0.0D);
        assertEquals(1, ShellTracerTrail.sampleCount(5));
        List<ShellTracerTrail.Segment> segments =
                ShellTracerTrail.segments(5, 6.0D, 1.0D, 0.0D, 0.0D);
        assertEquals(1, segments.size(), "one fallback bar, not a 900 block fake line");
        assertEquals(6.0D, segments.get(0).length(), EPSILON);
        assertEquals(900.0D, segments.get(0).bx(), EPSILON);
        assertEquals(894.0D, segments.get(0).ax(), EPSILON);
    }

    @Test
    void rewindAndNonFinitePositionsDropTheHistory() {
        ShellTracerTrail.track(6, 10.0D, 1.0D, 2.0D, 3.0D);
        ShellTracerTrail.track(6, 4.0D, 1.1D, 2.0D, 3.0D);
        assertEquals(1, ShellTracerTrail.sampleCount(6), "a rewound tick stamp is a reused id");
        ShellTracerTrail.track(6, 5.0D, Double.NaN, 2.0D, 3.0D);
        assertEquals(0, ShellTracerTrail.sampleCount(6));
        assertTrue(ShellTracerTrail.segments(6, 6.0D, 1.0D, 0.0D, 0.0D).isEmpty());
    }

    @Test
    void zeroSpeedFallsBackToTheFacing() {
        ShellTracerTrail.track(7, 0.0D, 10.0D, 64.0D, -3.0D);
        ShellTracerTrail.track(7, 1.0D, 10.0D, 64.0D, -3.0D);
        List<ShellTracerTrail.Segment> segments =
                ShellTracerTrail.segments(7, 6.0D, 0.0D, 0.0D, -1.0D);
        assertEquals(1, segments.size());
        assertEquals(6.0D, segments.get(0).length(), EPSILON, "fixed length along the facing");
        assertEquals(-3.0D, segments.get(0).bz(), EPSILON);
        assertEquals(3.0D, segments.get(0).az(), EPSILON, "the bar points away from the shell");
    }

    @Test
    void unusableInputDrawsNothing() {
        assertTrue(ShellTracerTrail.segments(8, 6.0D, 1.0D, 0.0D, 0.0D).isEmpty(),
                "a shell that was never tracked has no position to draw from");
        ShellTracerTrail.track(9, 0.0D, 0.0D, 0.0D, 0.0D);
        assertTrue(ShellTracerTrail.segments(9, 6.0D, Double.NaN, 0.0D, 0.0D).isEmpty(),
                "non-finite facing with no history is better left undrawn");
        assertTrue(ShellTracerTrail.segments(9, 6.0D, 0.0D, 0.0D, 0.0D).isEmpty(),
                "a zero facing vector is not a direction");
        ShellTracerTrail.track(9, 1.0D, 1.0D, 0.0D, 0.0D);
        assertTrue(ShellTracerTrail.segments(9, 0.0D, 1.0D, 0.0D, 0.0D).isEmpty(),
                "the config can switch the streak off");
        assertTrue(ShellTracerTrail.segments(9, -3.0D, 1.0D, 0.0D, 0.0D).isEmpty());
    }

    @Test
    void removedAndAbandonedShellsDoNotLeak() {
        ShellTracerTrail.track(20, 0.0D, 1.0D, 2.0D, 3.0D);
        assertEquals(1, ShellTracerTrail.sampleCount(20));
        ShellTracerTrail.forget(20);
        assertEquals(0, ShellTracerTrail.sampleCount(20));
        ShellTracerTrail.track(21, 0.0D, 1.0D, 2.0D, 3.0D);
        ShellTracerTrail.track(22, ShellTracerTrail.STALE_TICKS + 1.0D, 0.0D, 0.0D, 0.0D);
        assertEquals(0, ShellTracerTrail.sampleCount(21), "an abandoned history is swept");
        assertEquals(1, ShellTracerTrail.sampleCount(22));
    }

    @Test
    void theTrackedMapIsCapped() {
        for (int id = 0; id < 200; id++) {
            ShellTracerTrail.track(id, 50.0D, 0.0D, 0.0D, 0.0D);
        }
        assertTrue(ShellTracerTrail.trackedShells() <= ShellTracerTrail.MAX_TRACKED_SHELLS,
                "a static map keyed by entity id must not grow without bound");
        assertTrue(ShellTracerTrail.trackedShells() > 0);
    }

    @Test
    void pixelsPerRadianMatchesTheProjection() {
        // 70 degrees of vertical field of view on a 1080 pixel framebuffer: half the height
        // over tan(35 degrees).
        assertEquals(771.2D, ShellTracerTrail.pixelsPerRadian(70.0D, 1080.0D), 0.1D);
        // 90 degrees is the easy case: one radian is exactly half the framebuffer.
        assertEquals(50.0D, ShellTracerTrail.pixelsPerRadian(90.0D, 100.0D), EPSILON);
        assertTrue(ShellTracerTrail.pixelsPerRadian(45.0D, 100.0D)
                > ShellTracerTrail.pixelsPerRadian(90.0D, 100.0D), "a narrower view magnifies");
        assertEquals(1080.0D, ShellTracerTrail.pixelsPerRadian(90.0D, 2160.0D), EPSILON);
    }

    @Test
    void pixelsPerRadianRejectsIllegalInput() {
        assertEquals(0.0D, ShellTracerTrail.pixelsPerRadian(0.0D, 1080.0D));
        assertEquals(0.0D, ShellTracerTrail.pixelsPerRadian(-70.0D, 1080.0D));
        assertEquals(0.0D, ShellTracerTrail.pixelsPerRadian(180.0D, 1080.0D));
        assertEquals(0.0D, ShellTracerTrail.pixelsPerRadian(Double.NaN, 1080.0D));
        assertEquals(0.0D, ShellTracerTrail.pixelsPerRadian(Double.POSITIVE_INFINITY, 1080.0D));
        assertEquals(0.0D, ShellTracerTrail.pixelsPerRadian(70.0D, 0.0D));
        assertEquals(0.0D, ShellTracerTrail.pixelsPerRadian(70.0D, -5.0D));
        assertEquals(0.0D, ShellTracerTrail.pixelsPerRadian(70.0D, Double.NaN));
    }

    @Test
    void halfWidthCompensatesForDistanceAndCaps() {
        double pixelsPerRadian = ShellTracerTrail.pixelsPerRadian(70.0D, 1080.0D);
        // Close up the base width already covers more than the minimum, so nothing changes.
        assertEquals(ShellTracerTrail.HALF_WIDTH,
                ShellTracerTrail.halfWidthForDistance(ShellTracerTrail.HALF_WIDTH, 10.0D,
                        pixelsPerRadian, 2.0D), EPSILON);
        // At 400 blocks the projection asks for 0.5 * 2 * 400 / 771.2 = 0.52 blocks, which is a
        // 1.04 block bar, and that is two pixels on screen.
        double far = ShellTracerTrail.halfWidthForDistance(ShellTracerTrail.HALF_WIDTH, 400.0D,
                pixelsPerRadian, 2.0D);
        assertEquals(0.5187D, far, 1.0E-3D);
        assertEquals(2.0D, 2.0D * far / 400.0D * pixelsPerRadian, 1.0E-6D,
                "the compensated bar is exactly the minimum width on screen");
        // The cap stops a very distant round from becoming a slab of light.
        assertEquals(ShellTracerTrail.MAX_HALF_WIDTH,
                ShellTracerTrail.halfWidthForDistance(ShellTracerTrail.HALF_WIDTH, 4000.0D,
                        pixelsPerRadian, 2.0D), EPSILON);
    }

    @Test
    void halfWidthFallsBackToTheBase() {
        double pixelsPerRadian = ShellTracerTrail.pixelsPerRadian(70.0D, 1080.0D);
        // A minimum of 0 pixels is the switch back to strict world units.
        assertEquals(ShellTracerTrail.HALF_WIDTH,
                ShellTracerTrail.halfWidthForDistance(ShellTracerTrail.HALF_WIDTH, 4000.0D,
                        pixelsPerRadian, 0.0D), EPSILON);
        // Every unusable input lands on the base instead of on an enormous or a zero bar.
        assertEquals(0.05D, ShellTracerTrail.halfWidthForDistance(0.05D, Double.NaN,
                pixelsPerRadian, 2.0D), EPSILON);
        assertEquals(0.05D, ShellTracerTrail.halfWidthForDistance(0.05D, 400.0D, 0.0D, 2.0D),
                EPSILON);
        assertEquals(0.05D, ShellTracerTrail.halfWidthForDistance(0.05D, 400.0D, pixelsPerRadian,
                Double.NaN), EPSILON);
        assertEquals(0.05D, ShellTracerTrail.halfWidthForDistance(0.05D, -400.0D,
                pixelsPerRadian, 2.0D), EPSILON);
        // The real chain: a broken field of view means 0 pixels per radian, which means no
        // compensation anywhere downstream.
        assertEquals(0.05D, ShellTracerTrail.halfWidthForDistance(0.05D, 400.0D,
                ShellTracerTrail.pixelsPerRadian(0.0D, 1080.0D), 2.0D), EPSILON);
        assertEquals(0.0D, ShellTracerTrail.halfWidthForDistance(0.0D, 400.0D, pixelsPerRadian,
                2.0D), EPSILON);
    }

    @Test
    void lengthCompensationGrowsWithDistanceAndCaps() {
        double pixelsPerRadian = ShellTracerTrail.pixelsPerRadian(70.0D, 1080.0D);
        double base = 6.0D;
        // 200 blocks: barely changed, 6.22 blocks instead of 6.
        assertEquals(6.224D, ShellTracerTrail.lengthForDistance(base, 200.0D, pixelsPerRadian,
                24.0D), 1.0E-3D);
        // 400 blocks: 12.45 blocks, which is the 24 pixel minimum on screen.
        double mid = ShellTracerTrail.lengthForDistance(base, 400.0D, pixelsPerRadian, 24.0D);
        assertEquals(12.448D, mid, 1.0E-3D);
        assertEquals(24.0D, mid / 400.0D * pixelsPerRadian, 1.0E-6D);
        // 800 blocks: 24.9 blocks.
        assertEquals(24.897D, ShellTracerTrail.lengthForDistance(base, 800.0D, pixelsPerRadian,
                24.0D), 1.0E-3D);
        // And the hard cap.
        assertEquals(ShellTracerTrail.MAX_LENGTH_BLOCKS,
                ShellTracerTrail.lengthForDistance(base, 8000.0D, pixelsPerRadian, 24.0D),
                EPSILON);
        // Strict world units again.
        assertEquals(base, ShellTracerTrail.lengthForDistance(base, 8000.0D, pixelsPerRadian,
                0.0D), EPSILON);
        assertEquals(base, ShellTracerTrail.lengthForDistance(base, Double.NaN, pixelsPerRadian,
                24.0D), EPSILON);
    }

    @Test
    void oneSampleIsKeptPerServerTick() {
        // Three frames inside tick 100 collapse into that tick's freshest position, so the history
        // budget is spent on ticks and the streak does not shrink at a high frame rate.
        ShellTracerTrail.track(30, 100.05D, 0.0D, 0.0D, 0.0D);
        ShellTracerTrail.track(30, 100.25D, 0.2D, 0.0D, 0.0D);
        ShellTracerTrail.track(30, 100.95D, 1.0D, 0.0D, 0.0D);
        assertEquals(1, ShellTracerTrail.sampleCount(30));
        assertEquals(1.0D, ShellTracerTrail.segments(30, 6.0D, 0.05D, 1.0D, 0.0D, 0.0D)
                .get(0).bx(), EPSILON, "the kept sample is the newest drawn position");
        // The next tick does spend a slot.
        ShellTracerTrail.track(30, 101.2D, 5.0D, 0.0D, 0.0D);
        assertEquals(2, ShellTracerTrail.sampleCount(30));
    }

    @Test
    void farDistanceStreakStillReachesItsLength() {
        // One sample per tick at the round's 5 blocks/tick: eight ticks of history hold 35 blocks
        // of path, enough for the 24 pixel streak the minimum length can ask for at range.
        for (int tick = 0; tick < ShellTracerTrail.MAX_FRAMES; tick++) {
            ShellTracerTrail.track(31, tick, tick * 5.0D, 0.0D, 0.0D);
        }
        assertEquals(ShellTracerTrail.MAX_FRAMES, ShellTracerTrail.sampleCount(31));
        List<ShellTracerTrail.Segment> segments =
                ShellTracerTrail.segments(31, 24.0D, 0.52D, 1.0D, 0.0D, 0.0D);
        assertEquals(24.0D, totalLength(segments), EPSILON);
        assertEquals(35.0D, segments.get(0).bx(), EPSILON);
        assertEquals(11.0D, segments.get(segments.size() - 1).ax(), EPSILON);
        for (ShellTracerTrail.Segment segment : segments) {
            assertEquals(0.52D, segment.halfWidth(), EPSILON,
                    "the compensated width is used as given");
        }
    }

    @Test
    void widthOverloadMatchesTheDefaultGeometry() {
        for (int frame = 0; frame < 4; frame++) {
            ShellTracerTrail.track(32, frame, frame, 0.0D, 0.0D);
        }
        List<ShellTracerTrail.Segment> plain =
                ShellTracerTrail.segments(32, 3.0D, 1.0D, 0.0D, 0.0D);
        List<ShellTracerTrail.Segment> explicit = ShellTracerTrail.segments(32, 3.0D,
                ShellTracerTrail.HALF_WIDTH, 1.0D, 0.0D, 0.0D);
        assertEquals(plain.size(), explicit.size());
        for (int index = 0; index < plain.size(); index++) {
            assertEquals(plain.get(index), explicit.get(index),
                    "the width overload is the same rule, only the width is chosen per frame");
        }
        // A width that is not a usable size goes back to the constant rather than to a zero bar.
        assertEquals(ShellTracerTrail.HALF_WIDTH, ShellTracerTrail
                .segments(32, 3.0D, Double.NaN, 1.0D, 0.0D, 0.0D).get(0).halfWidth(), EPSILON);
        assertEquals(ShellTracerTrail.HALF_WIDTH, ShellTracerTrail
                .segments(32, 3.0D, 0.0D, 1.0D, 0.0D, 0.0D).get(0).halfWidth(), EPSILON);
    }
}

