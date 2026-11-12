package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Flight and fire-control rules of the PLZ-05 155mm howitzer.
 *
 * <p>The howitzer is a single-shot platform: one shell in the breech, one ballistic arc with no
 * motor, a long reach and a much heavier crater than the 370mm rocket.
 */
final class Plz05BallisticsTest {
    private static final double EPS = 1.0E-9D;

    @Test
    void launchKeepsTheMuzzleSpeedWhicheverWayItPoints() {
        Plz05Ballistics.V3 flat = Plz05Ballistics.launchVelocity(
                new Plz05Ballistics.V3(0.0D, 0.0D, 1.0D));
        assertEquals(Plz05Ballistics.INITIAL_SPEED, flat.length(), 1.0E-9D);
        Plz05Ballistics.V3 diagonal = Plz05Ballistics.launchVelocity(
                new Plz05Ballistics.V3(0.3D, 0.4D, 0.5D));
        assertEquals(Plz05Ballistics.INITIAL_SPEED, diagonal.length(), 1.0E-9D);
    }

    @Test
    void gravityIsTheOnlyThrustAfterLaunch() {
        Plz05Ballistics.V3 heading = new Plz05Ballistics.V3(0.0D, 0.6D, 0.8D);
        Plz05Ballistics.V3 velocity = Plz05Ballistics.launchVelocity(heading);
        Plz05Ballistics.V3 first = Plz05Ballistics.advance(velocity, heading, 1);
        Plz05Ballistics.V3 late = Plz05Ballistics.advance(velocity, heading,
                Plz05Ballistics.LIFETIME_TICKS - 1);
        assertEquals(-Plz05Ballistics.GRAVITY, first.y() - velocity.y(), EPS);
        assertEquals(velocity.x(), first.x(), EPS);
        assertEquals(velocity.z(), first.z(), EPS);
        // No motor: the hundredth tick behaves like the first one.
        assertEquals(first.y(), late.y(), EPS);
    }

    @Test
    void rangePeaksAroundFortyFiveDegreesAndTopsOutAtTwoHundred() {
        double low = Plz05Ballistics.rangeAt(10.0D);
        double best = Plz05Ballistics.rangeAt(45.0D);
        double high = Plz05Ballistics.rangeAt(70.0D);
        assertTrue(best > low, "45 degrees must outrange 10");
        assertTrue(best > high, "45 degrees must outrange 70");
        // The gun was retuned to a 200 m weapon: the apex shot must land on that bracket.
        assertEquals(200.0D, best, 2.0D);
        assertTrue(best < 210.0D, "the 155mm must not outrange 200 blocks, saw " + best);
    }

    @Test
    void requiredElevationHitsTheRequestedRange() {
        for (double distance : new double[] {60.0D, 120.0D, 180.0D, 195.0D}) {
            double elevation = Plz05Ballistics.requiredElevation(distance);
            double range = Plz05Ballistics.rangeAt(elevation);
            assertTrue(Math.abs(range - distance) < 12.0D,
                    "aiming at " + distance + " landed " + range);
        }
    }

    @Test
    void elevationIsClampedIntoTheGunTravel() {
        assertEquals(Plz05Ballistics.MIN_ELEVATION_DEG, Plz05Ballistics.clampElevation(-40.0D), EPS);
        assertEquals(Plz05Ballistics.MAX_ELEVATION_DEG, Plz05Ballistics.clampElevation(95.0D), EPS);
        assertEquals(Plz05Ballistics.MIN_ELEVATION_DEG, Plz05Ballistics.clampElevation(Double.NaN), EPS);
    }

    @Test
    void oneShellIsInTheBreechAndTheReloadIsLongerThanTheQteReward() {
        assertEquals(1, Plz05Ballistics.SHELLS);
        assertTrue(Plz05Ballistics.RELOAD_TICKS > Plz05Ballistics.QTE_REWARD_TICKS,
                "one good press must not finish the reload");
        assertEquals(Plz05Ballistics.QTE_MIN_REMAINING,
                Plz05Ballistics.qteReward(Plz05Ballistics.QTE_MIN_REMAINING));
        assertEquals(Plz05Ballistics.QTE_MIN_REMAINING,
                Plz05Ballistics.qteReward(Plz05Ballistics.QTE_MIN_REMAINING + 10));
    }

    @Test
    void theSkillWindowIsInsideOneSweep() {
        assertTrue(Plz05Ballistics.QTE_WINDOW_START < Plz05Ballistics.QTE_WINDOW_END);
        assertTrue(Plz05Ballistics.QTE_WINDOW_END <= Plz05Ballistics.QTE_TICKS);
        // Elapsed ticks equal the window start at this remaining time.
        int remaining = Plz05Ballistics.RELOAD_TICKS - Plz05Ballistics.QTE_WINDOW_START;
        assertTrue(Plz05Ballistics.qteInWindow(remaining));
        assertTrue(!Plz05Ballistics.qteInWindow(
                Plz05Ballistics.RELOAD_TICKS - Plz05Ballistics.QTE_WINDOW_END));
    }

    @Test
    void aShellHitsHarderThanARocket() {
        assertTrue(Plz05Ballistics.DIRECT_DAMAGE > Phl191Ballistics.DIRECT_DAMAGE);
        assertTrue(Plz05Ballistics.SPLASH_RADIUS > Phl191Ballistics.SPLASH_RADIUS);
        assertTrue(Plz05Ballistics.EXPLOSION_POWER > 4.0F);
        assertTrue(Plz05Ballistics.DEFAULT_CRATER_RADIUS > 5.0D,
                "the 155mm crater must dwarf the rocket's");
    }
}
