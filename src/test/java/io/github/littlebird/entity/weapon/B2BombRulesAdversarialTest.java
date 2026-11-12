package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Adversarial acceptance for the B-2 bomb rules (frozen spec docs/plan/b2-systems-spec.md 0.2).
 *
 * <p>Verifier-written: the cases below break the rules at a boundary (empty bay, negative stock,
 * oversized stock, back-to-back pulls, an odd number left over, a two-hundredfold off-axis
 * designation, null state, terminal velocity, a thousand-tick fall) instead of replaying the happy
 * path. Nothing here touches Minecraft classes, so it runs as a plain unit test.
 */
final class B2BombRulesAdversarialTest {
    private static final double G = BombBallistics.GRAVITY_BLOCKS_PER_TICK2;
    private static final double TERMINAL = BombBallistics.TERMINAL_FALL_PER_TICK;
    private static final double AUTHORITY = BombBallistics.GUIDANCE_AUTHORITY;

    // ---------------------------------------------------------------- bay: canRelease

    @Test
    void releaseNeedsStockAndAnExpiredCooldown() {
        assertFalse(BombBay.canRelease(0, 0), "an empty bay may never release");
        assertFalse(BombBay.canRelease(0, -5));
        assertFalse(BombBay.canRelease(-1, 0), "negative stock must not read as ready");
        assertFalse(BombBay.canRelease(-1000, -1000));
        assertTrue(BombBay.canRelease(1, 0), "one bomb and no cooldown is a legal pull");
        assertTrue(BombBay.canRelease(1, -5), "an over-expired cooldown counts as ready");
        assertTrue(BombBay.canRelease(Integer.MAX_VALUE, 0));
    }

    @Test
    void anyLiveCooldownBlocksTheNextPull() {
        for (int cooldown = 1; cooldown <= BombBay.RELEASE_INTERVAL_TICKS * 2; cooldown++) {
            assertFalse(BombBay.canRelease(4, cooldown),
                    "cooldown " + cooldown + " must still block the trigger");
        }
    }

    // ---------------------------------------------------------------- bay: bombsToDrop

    @Test
    void salvoDropsTwoButNeverOverdrawsTheBay() {
        assertEquals(0, BombBay.bombsToDrop(true, 0));
        assertEquals(1, BombBay.bombsToDrop(true, 1), "a two-bomb salvo with one left drops one");
        assertEquals(BombBay.SALVO_COUNT, BombBay.bombsToDrop(true, BombBay.SALVO_COUNT));
        assertEquals(BombBay.SALVO_COUNT, BombBay.bombsToDrop(true, BombBay.SALVO_COUNT + 1));
        assertEquals(BombBay.SALVO_COUNT, BombBay.bombsToDrop(true, Integer.MAX_VALUE),
                "an oversized stock must not overflow the drop count");
        assertEquals(0, BombBay.bombsToDrop(true, -3));
    }

    @Test
    void singleDropDropsExactlyOneWhileStockLasts() {
        assertEquals(0, BombBay.bombsToDrop(false, 0));
        assertEquals(0, BombBay.bombsToDrop(false, -7));
        assertEquals(1, BombBay.bombsToDrop(false, 1));
        assertEquals(1, BombBay.bombsToDrop(false, Integer.MAX_VALUE));
    }

    @Test
    void dropCountIsAlwaysWithinTheRemainingStock() {
        for (int remaining = -2; remaining <= 40; remaining++) {
            for (boolean salvo : new boolean[] {true, false}) {
                int drop = BombBay.bombsToDrop(salvo, remaining);
                assertTrue(drop >= 0, "negative drop at remaining=" + remaining);
                assertTrue(drop <= Math.max(0, remaining),
                        "dropped more than the bay holds: remaining=" + remaining + " drop=" + drop);
                assertTrue(drop <= (salvo ? BombBay.SALVO_COUNT : 1),
                        "one trigger pull dropped too many: drop=" + drop);
            }
        }
    }

    // ---------------------------------------------------------------- bay: nextCooldown

    @Test
    void cooldownStaysOnTheFrozenGridAndASalvoLocksAtLeastAsLongAsASingle() {
        assertEquals(6, BombBay.RELEASE_INTERVAL_TICKS, "frozen interval");
        assertEquals(2, BombBay.SALVO_COUNT, "frozen salvo size");
        int single = BombBay.nextCooldown(false);
        int salvo = BombBay.nextCooldown(true);
        assertEquals(0, single % BombBay.RELEASE_INTERVAL_TICKS,
                "single cooldown must sit on the frozen grid: " + single);
        assertEquals(0, salvo % BombBay.RELEASE_INTERVAL_TICKS,
                "salvo cooldown must sit on the frozen grid: " + salvo);
        assertTrue(single >= BombBay.RELEASE_INTERVAL_TICKS || single > 0,
                "a pull must always rearm: " + single);
        assertTrue(salvo >= single, "a salvo must keep the bay shut at least as long as a single");
    }

    @Test
    void emptyingTheBayByRepeatedSalvosTerminatesAndNeverUnderflows() {
        for (int capacity : new int[] {1, 2, 3, 4, 16, 23, 24, 25, 64}) {
            int remaining = capacity;
            int dropped = 0;
            int pulls = 0;
            int cooldown = 0;
            while (true) {
                if (!BombBay.canRelease(remaining, cooldown)) {
                    if (cooldown > 0) {
                        cooldown--;
                        continue;
                    }
                    break;
                }
                int drop = BombBay.bombsToDrop(true, remaining);
                assertTrue(drop >= 1, "release allowed but nothing dropped at remaining=" + remaining);
                remaining -= drop;
                dropped += drop;
                cooldown = BombBay.nextCooldown(true);
                pulls++;
                assertTrue(pulls <= capacity + 2, "salvo loop did not terminate (cap " + capacity + ")");
            }
            assertEquals(0, remaining, "salvo loop left " + remaining + " bombs (cap " + capacity + ")");
            assertEquals(capacity, dropped, "dropped count must equal the loaded stock");
        }
    }

    // ---------------------------------------------------------------- kinds and fuses

    @Test
    void exactlyTheTwoFrozenKindsExist() {
        assertEquals(2, BombKind.values().length);
        assertTrue(BombKind.valueOf("JDAM") == BombKind.JDAM);
        assertTrue(BombKind.valueOf("INCENDIARY") == BombKind.INCENDIARY);
    }

    @Test
    void bombKindFiguresMatchTheFrozenContract() {
        assertTrue(BombKind.JDAM.guided(), "the JDAM steers");
        assertFalse(BombKind.INCENDIARY.guided(), "the canister does not");
        assertEquals(240.0F, BombKind.JDAM.damage(), 1.0E-6F);
        assertEquals(60.0F, BombKind.JDAM.groundVehicleDamage(), 1.0E-6F);
        assertEquals(0.0F, BombKind.INCENDIARY.groundVehicleDamage(), 1.0E-6F);
        assertEquals(10.0F, BombKind.JDAM.blastRadius(), 1.0E-6F);
        assertEquals(90.0F, BombKind.INCENDIARY.damage(), 1.0E-6F);
        assertEquals(7.0F, BombKind.INCENDIARY.blastRadius(), 1.0E-6F);
        assertTrue(BombKind.JDAM.massKg() > BombKind.INCENDIARY.massKg(), "2000 lb vs a canister");
        for (BombKind kind : BombKind.values()) {
            assertTrue(kind.damage() > 0.0F, kind + " damage");
            assertTrue(kind.blastRadius() > 0.0F, kind + " blast");
            assertTrue(kind.massKg() > 0.0D, kind + " mass");
            assertTrue(kind.drag() > 0.0D && kind.drag() < 1.0D, kind + " drag is a fraction kept");
        }
        assertEquals(BombKind.JDAM, BombKind.byOrdinal(0));
        assertEquals(BombKind.INCENDIARY, BombKind.byOrdinal(1));
        assertEquals(BombKind.JDAM, BombKind.byOrdinal(-1), "an unknown wire ordinal falls back");
        assertEquals(BombKind.JDAM, BombKind.byOrdinal(99));
    }

    @Test
    void detonationTriggersOnGroundOrBelowTheWorldFloor() {
        assertTrue(BombBallistics.shouldDetonate(100.0D, true), "a ground hit detonates at any height");
        assertTrue(BombBallistics.shouldDetonate(0.0D, true));
        assertFalse(BombBallistics.shouldDetonate(0.0D, false),
                "y=0 in mid air is still flight, it is not a floor");
        assertFalse(BombBallistics.shouldDetonate(0.001D, false));
        assertTrue(BombBallistics.shouldDetonate(BombBallistics.FLOOR_DETONATE_Y, false),
                "the documented floor detonates inclusively");
        assertFalse(BombBallistics.shouldDetonate(BombBallistics.FLOOR_DETONATE_Y + 0.001D, false));
        assertTrue(BombBallistics.shouldDetonate(-1000.0D, false));
        assertTrue(BombBallistics.shouldDetonate(Double.NEGATIVE_INFINITY, false));
        assertFalse(BombBallistics.shouldDetonate(Double.POSITIVE_INFINITY, false));
    }

    // ---------------------------------------------------------------- flight model

    @Test
    void unguidedBombOnlyFallsAndBleedsForwardSpeed() {
        double[] pos = {0.0D, 200.0D, 0.0D};
        double[] vel = {0.5D, 0.0D, -0.25D};
        double drag = BombKind.JDAM.drag();
        double[] out = BombBallistics.step(pos, vel, BombKind.JDAM, null, false);
        assertEquals(6, out.length, "step returns {px,py,pz,vx,vy,vz}");
        assertEquals(0.5D * drag, out[3], 1.0E-12D, "forward speed bleeds by the drag each tick");
        assertEquals(-0.25D * drag, out[5], 1.0E-12D);
        assertEquals(0.5D * drag, out[0], 1.0E-12D, "x advances by the new velocity");
        assertEquals(-0.25D * drag, out[2], 1.0E-12D);
        assertEquals(-G, out[4], 1.0E-12D, "gravity pulls down one step from rest");
        assertEquals(200.0D - G, out[1], 1.0E-12D);
    }

    @Test
    void fallSpeedSaturatesAtTerminal() {
        double[] out = BombBallistics.step(new double[] {0.0D, 100.0D, 0.0D},
                new double[] {0.0D, -50.0D, 0.0D}, BombKind.JDAM, null, false);
        assertEquals(-TERMINAL, out[4], 1.0E-12D, "a bomb dropped from altitude does not speed up forever");
        double[] climbing = BombBallistics.step(new double[] {0.0D, 100.0D, 0.0D},
                new double[] {0.0D, 50.0D, 0.0D}, BombKind.JDAM, null, false);
        assertEquals(50.0D - G, climbing[4], 1.0E-12D, "an upward throw is decelerated, not clamped");
    }

    @Test
    void guidanceSteersOnlyWhenTheRoundIsGuidedAndTheTargetIsInRange() {
        double[] pos = {0.0D, 100.0D, 0.0D};
        double[] vel = {0.0D, -0.5D, 0.0D};
        double[] near = {10.0D, 90.0D, 0.0D};
        double[] far = {500.0D, 90.0D, 0.0D};
        double[] ballistic = BombBallistics.step(pos, vel, BombKind.JDAM, near, false);
        double[] glide = BombBallistics.step(pos, vel, BombKind.JDAM, near, true);
        assertTrue(glide[3] > ballistic[3], "a guided JDAM pulls towards the designation");
        assertArrayEquals(ballistic, BombBallistics.step(pos, vel, BombKind.JDAM, far, true), 1.0E-12D,
                "past GUIDANCE_RANGE_BLOCKS the fuse is plain ballistic");
        assertArrayEquals(BombBallistics.step(pos, vel, BombKind.INCENDIARY, near, false),
                BombBallistics.step(pos, vel, BombKind.INCENDIARY, near, true), 1.0E-12D,
                "an unguided canister must ignore the designation");
        assertArrayEquals(ballistic, BombBallistics.step(pos, vel, BombKind.JDAM, null, true), 1.0E-12D,
                "guided with no designation is ballistic");
    }

    @Test
    void guidanceAuthorityIsCapped() {
        double[] pos = {0.0D, 100.0D, 0.0D};
        double[] vel = {0.0D, -1.0D, 0.0D};
        double[] sharp = {0.001D, 99.999D, 0.0D};
        double[] guided = BombBallistics.step(pos, vel, BombKind.JDAM, sharp, true);
        double[] plain = BombBallistics.step(pos, vel, BombKind.JDAM, sharp, false);
        double dx = guided[3] - plain[3];
        double dy = guided[4] - plain[4];
        double dz = guided[5] - plain[5];
        double correction = Math.sqrt(dx * dx + dy * dy + dz * dz);
        assertTrue(correction <= AUTHORITY + 1.0E-9D,
                "one tick of steering exceeded the authority: " + correction);
        assertTrue(correction > 1.0E-6D, "a 180 degree demand must still correct something");
    }

    @Test
    void nullStateIsTheOriginAtRest() {
        double[] out = BombBallistics.step(null, null, null, null, false);
        assertEquals(6, out.length);
        assertEquals(0.0D, out[0], 1.0E-12D);
        assertEquals(-G, out[1], 1.0E-12D, "a bomb at rest falls one gravity step");
        assertEquals(0.0D, out[2], 1.0E-12D);
        assertEquals(0.0D, out[3], 1.0E-12D);
        assertEquals(-G, out[4], 1.0E-12D);
        assertEquals(0.0D, out[5], 1.0E-12D, "a null kind means the JDAM");
    }

    @Test
    void theInputArraysAreNeverModified() {
        double[] pos = {1.0D, 2.0D, 3.0D};
        double[] vel = {0.4D, 0.5D, 0.6D};
        BombBallistics.step(pos, vel, BombKind.JDAM, new double[] {9.0D, 9.0D, 9.0D}, true);
        assertArrayEquals(new double[] {1.0D, 2.0D, 3.0D}, pos, 0.0D);
        assertArrayEquals(new double[] {0.4D, 0.5D, 0.6D}, vel, 0.0D);
    }

    @Test
    void aThousandTickFallStaysFiniteMonotoneAndEndsBelowTheFloor() {
        double[] pos = {0.0D, 320.0D, 0.0D};
        double[] vel = {0.0D, 0.0D, 0.0D};
        double previousY = pos[1];
        for (int tick = 0; tick < 1000; tick++) {
            double[] out = BombBallistics.step(pos, vel, BombKind.JDAM, null, false);
            for (double value : out) {
                assertTrue(Double.isFinite(value), "non-finite state at tick " + tick + ": " + value);
            }
            assertTrue(out[1] < previousY, "the bomb must keep falling at tick " + tick);
            assertTrue(out[4] >= -TERMINAL - 1.0E-12D, "fall speed passed terminal at tick " + tick);
            previousY = out[1];
            pos = new double[] {out[0], out[1], out[2]};
            vel = new double[] {out[3], out[4], out[5]};
        }
        assertTrue(previousY < 0.0D, "1000 ticks must bring it down from 320 blocks");
        assertTrue(BombBallistics.shouldDetonate(previousY, false), "it flew past the world floor");
        double[] hot = BombBallistics.step(new double[] {0.0D, 1000.0D, 0.0D},
                new double[] {1.0E6D, 1.0E6D, -1.0E6D}, BombKind.JDAM, null, false);
        for (double value : hot) {
            assertTrue(Double.isFinite(value), "a silly fast bomb must stay finite: " + value);
        }
    }

    @Test
    void nonFiniteInputsAreSurvivable() {
        assertDoesNotThrow(() -> BombBallistics.step(
                new double[] {Double.NaN, Double.NaN, Double.NaN},
                new double[] {Double.NaN, Double.NaN, Double.NaN}, BombKind.JDAM, null, false));
        assertDoesNotThrow(() -> BombBallistics.step(null, null, BombKind.JDAM,
                new double[] {Double.NaN, Double.NaN, Double.NaN}, true));
        assertDoesNotThrow(() -> BombBallistics.step(new double[] {0.0D, 0.0D, 0.0D},
                new double[] {0.0D, 0.0D, 0.0D}, BombKind.JDAM,
                new double[] {Double.POSITIVE_INFINITY, 0.0D, 0.0D}, true));
        assertDoesNotThrow(() -> BombBallistics.shouldDetonate(Double.NaN, false));
        assertDoesNotThrow(() -> BombBallistics.dropLeadDistance(Double.NaN, Double.NaN));
        assertDoesNotThrow(() -> BombBallistics.dropLeadDistance(Double.POSITIVE_INFINITY, 10.0D));
        assertDoesNotThrow(() -> BombBay.canRelease(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertDoesNotThrow(() -> BombBay.bombsToDrop(true, Integer.MIN_VALUE));
        assertFalse(BombBay.canRelease(Integer.MIN_VALUE, Integer.MIN_VALUE),
                "the most negative stock is still an empty bay");
        assertEquals(0, BombBay.bombsToDrop(true, Integer.MIN_VALUE));
        assertFalse(BombBallistics.shouldDetonate(0.0D, false));
    }

    // ---------------------------------------------------------------- release lead

    @Test
    void leadDistanceGrowsWithSpeedAndAltitudeAndNeverGoesNegative() {
        assertEquals(0.0D, BombBallistics.dropLeadDistance(0.0D, 0.0D), 1.0E-9D);
        assertEquals(0.0D, BombBallistics.dropLeadDistance(0.0D, 120.0D), 1.0E-9D);
        assertEquals(0.0D, BombBallistics.dropLeadDistance(-3.0D, 120.0D), 1.0E-9D,
                "negative speed carries no lead");
        double slow = BombBallistics.dropLeadDistance(0.5D, 40.0D);
        double fast = BombBallistics.dropLeadDistance(1.5D, 40.0D);
        double high = BombBallistics.dropLeadDistance(0.5D, 160.0D);
        assertTrue(slow > 0.0D, "a moving release leads the aircraft");
        assertTrue(fast > slow, "faster release must lead further: " + fast + " vs " + slow);
        assertTrue(high > slow, "higher release must lead further: " + high + " vs " + slow);
        assertEquals(0.0D, BombBallistics.dropLeadDistance(0.5D, -50.0D), 1.0E-9D,
                "a below-ground release carries no lead");
        assertTrue(Double.isFinite(BombBallistics.dropLeadDistance(1.0E6D, 1.0E6D)),
                "absurd but finite inputs must stay finite");
    }

    @Test
    void leadDistanceAgreesWithASteppedFlight() {
        double[][] probes = {{0.5D, 40.0D}, {1.0D, 120.0D}, {0.9D, 300.0D}, {1.4D, 8.0D},
                {0.2D, 800.0D}};
        for (double[] probe : probes) {
            double speed = probe[0];
            double altitude = probe[1];
            double closed = BombBallistics.dropLeadDistance(speed, altitude);
            double moveThenDrag = simulateLead(speed, altitude, BombKind.JDAM.drag(), false);
            double dragThenMove = simulateLead(speed, altitude, BombKind.JDAM.drag(), true);
            double relA = Math.abs(closed - moveThenDrag) / moveThenDrag;
            double relB = Math.abs(closed - dragThenMove) / dragThenMove;
            assertTrue(Math.min(relA, relB) < 0.01D,
                    "closed form must match a stepped flight within 1%: speed=" + speed
                            + " altitude=" + altitude + " closed=" + closed
                            + " moveThenDrag=" + moveThenDrag + " rel=" + relA
                            + " dragThenMove=" + dragThenMove + " rel=" + relB);
        }
    }

    /** Independent re-implementation of the documented flight, used to audit the closed form. */
    private static double simulateLead(double speed, double altitude, double drag,
                                       boolean dragFirst) {
        double y = 0.0D;
        double vy = 0.0D;
        double vx = speed;
        double lead = 0.0D;
        int guard = 0;
        while (y > -altitude && guard++ < 200000) {
            if (dragFirst) {
                vx *= drag;
            }
            lead += vx;
            vy = Math.max(vy - G, -TERMINAL);
            y += vy;
            if (!dragFirst) {
                vx *= drag;
            }
        }
        return lead;
    }
}
