package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import org.junit.jupiter.api.Test;

final class ApacheMissileTrailTest {

    private static final Vector3 FORWARD = new Vector3(0.0D, 0.0D, 1.0D);

    @Test
    void thereAreTwoTailsEitherSideOfTheAxis() {
        Vector3 right = ApacheMissileTrail.rightOf(FORWARD);
        assertEquals(2, ApacheMissileTrail.TAILS);
        Vector3[] anchors = ApacheMissileTrail.tailAnchors(new Vector3(0.0D, 0.0D, 0.0D), right);
        assertEquals(2, anchors.length);
        double gap = anchors[1].distanceTo(anchors[0]);
        assertEquals(ApacheMissileTrail.TAIL_SEPARATION * 2.0D, gap, 1.0E-9D,
                "the two tails are one separation apart");
        assertTrue(gap <= 0.3D, "and they stay tight enough to read as one plume: " + gap);
        assertTrue(Math.abs(anchors[0].y()) < 1.0E-9D && Math.abs(anchors[1].y()) < 1.0E-9D,
                "the tails are level with the axis, not above or below it");
    }

    @Test
    void theRightVectorIsPerpendicularAndUnit() {
        Vector3 right = ApacheMissileTrail.rightOf(new Vector3(0.3D, -0.4D, 0.866D));
        assertEquals(1.0D, right.length(), 1.0E-9D);
        assertEquals(0.0D, right.dot(new Vector3(0.3D, -0.4D, 0.866D).normalize()), 1.0E-9D,
                "a tail offset to the side must not stretch the plume along the axis");
    }

    @Test
    void aVerticalShotStillGetsAHorizontalRightVector() {
        // Straight up: the cross product with the world up axis collapses, so the fallback has
        // to keep the tails apart instead of folding them onto the same line.
        for (Vector3 forward : new Vector3[] {
                new Vector3(0.0D, 1.0D, 0.0D), new Vector3(0.0D, -1.0D, 0.0D)}) {
            Vector3 right = ApacheMissileTrail.rightOf(forward);
            assertEquals(1.0D, right.length(), 1.0E-9D);
            assertEquals(0.0D, right.dot(forward), 1.0E-9D);
            Vector3[] anchors = ApacheMissileTrail.tailAnchors(new Vector3(0.0D, 0.0D, 0.0D), right);
            assertTrue(anchors[1].distanceTo(anchors[0]) > 1.0E-6D,
                    "the tails must not collapse into one for " + forward);
        }
    }

    @Test
    void theNozzleSitsBehindTheMissileOnItsAxis() {
        Vector3 centre = new Vector3(10.0D, 64.0D, -3.0D);
        Vector3 nozzle = ApacheMissileTrail.nozzle(centre, FORWARD);
        assertEquals(ApacheMissileTrail.NOZZLE_BACK_OFFSET,
                nozzle.distanceTo(centre), 1.0E-9D);
        assertEquals(centre.x(), nozzle.x(), 1.0E-9D);
        assertEquals(centre.y(), nozzle.y(), 1.0E-9D);
        assertTrue(nozzle.z() < centre.z(), "and behind the nose, not in front of it");
    }

    @Test
    void samplesCoverThisTicksFlight() {
        Vector3 from = new Vector3(0.0D, 0.0D, 0.0D);
        Vector3 to = new Vector3(0.0D, 0.0D, 3.0D);
        Vector3[] points = ApacheMissileTrail.samples(from, to, 3);
        assertEquals(3, points.length);
        assertEquals(from, points[0], "the oldest sample is where the tick started");
        assertEquals(2.0D, points[2].z(), 1.0E-9D, "and they stop short of the missile itself");
        assertTrue(points[1].z() > points[0].z() && points[1].z() < points[2].z());
        assertEquals(1, ApacheMissileTrail.samples(from, to, 0).length,
                "a silly substep count still yields a usable sample");
    }

    @Test
    void oneEmissionCoversEveryTickSinceTheLast() {
        // Three blocks a tick and an emission every other tick: a single tick of samples would
        // leave a three block hole in the smoke, so the span has to be the interval, not the tick.
        assertTrue(ApacheMissileTrail.TRAIL_INTERVAL_TICKS >= 1);
        double speed = 3.0D;
        Vector3 end = new Vector3(0.0D, 70.0D, 0.0D);
        Vector3 start = end.subtract(FORWARD.scale(
                speed * ApacheMissileTrail.TRAIL_INTERVAL_TICKS));
        Vector3[] points = ApacheMissileTrail.samples(start, end, 3);
        assertEquals(speed * ApacheMissileTrail.TRAIL_INTERVAL_TICKS,
                points[0].distanceTo(end), 1.0E-9D,
                "the oldest sample is a whole interval behind the missile");
        for (int i = 1; i < points.length; i++) {
            assertTrue(points[i].distanceTo(points[i - 1]) <= speed * 1.0D + 1.0E-9D,
                    "and consecutive puffs are never more than a tick of flight apart");
        }
    }

    @Test
    void theTrailRangeNeverDropsBelowSixtyFourBlocks() {
        assertEquals(64.0D, ApacheMissileTrail.particleRange(8.0D), 1.0E-9D,
                "the request was at least 64 blocks");
        assertEquals(96.0D, ApacheMissileTrail.particleRange(96.0D), 1.0E-9D);
        assertEquals(256.0D, ApacheMissileTrail.particleRange(4096.0D), 1.0E-9D);
        assertEquals(ApacheMissileTrail.DEFAULT_PARTICLE_RANGE,
                ApacheMissileTrail.particleRange(Double.NaN), 1.0E-9D);
        assertTrue(ApacheMissileTrail.particleRange(0.0D)
                > ApacheMissileTrail.VANILLA_PARTICLE_RANGE,
                "and always further than the vanilla broadcast reaches");
    }
}
