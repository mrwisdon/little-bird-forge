package io.github.littlebird.entity.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class JetFlightModelTest {
    private static final JetConfig CONFIG = F35Profile.config();

    private static JetState step(JetState s, JetInput in) {
        return JetFlightModel.step(s, in, CONFIG);
    }

    /** Holds full throttle until the jet settles onto its cruise ceiling. */
    private static JetState cruising() {
        JetState s = JetState.initial(0.0F);
        for (int i = 0; i < 600; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 1.0F, false));
        }
        return s;
    }

    private static JetInput stick(float pitch, float roll, float yaw, float throttle,
            boolean afterburner) {
        return new JetInput(pitch, roll, yaw, throttle, afterburner, false, false);
    }

    private static JetInput vtol(float throttle) {
        return new JetInput(0.0F, 0.0F, 0.0F, throttle, false, true, false);
    }

    private static void assertFinite(JetState s) {
        assertTrue(Float.isFinite(s.yawDeg()), "yaw " + s.yawDeg());
        assertTrue(Float.isFinite(s.pitchDeg()), "pitch " + s.pitchDeg());
        assertTrue(Float.isFinite(s.rollDeg()), "roll " + s.rollDeg());
        assertTrue(Double.isFinite(s.vx()) && Double.isFinite(s.vy())
                && Double.isFinite(s.vz()), "velocity");
        assertTrue(Float.isFinite(s.throttle()) && Float.isFinite(s.afterburnerCharge()),
                "throttle/charge");
    }

    @Test
    void throttleIntegratesToFull() {
        JetState s = JetState.initial(0.0F);
        for (int i = 0; i < 80; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 1.0F, false));
        }
        assertEquals(1.0F, s.throttle(), 1.0E-6F);
    }

    @Test
    void cruiseRidesTheSpeedCapAndHoldsAltitude() {
        JetState s = cruising();
        assertTrue(s.horizontalSpeed() > CONFIG.maxSpeed - 0.05D,
                "expected to reach the ceiling, speed=" + s.horizontalSpeed());
        assertTrue(s.horizontalSpeed() <= CONFIG.maxSpeed + 1.0E-6D,
                "must not exceed the ceiling, speed=" + s.horizontalSpeed());
        // Minecraft yaw 0 faces +Z, so cruise must run along +Z and leave x alone.
        assertTrue(s.vz() > 0.0D, "expected +Z travel, vz=" + s.vz());
        assertEquals(0.0D, s.vx(), 1.0E-3D);
        assertTrue(Math.abs(s.vy()) < 0.05D, "level flight must not sink, vy=" + s.vy());
        assertTrue(Math.abs(s.pitchDeg()) < 10.0F, "pitch=" + s.pitchDeg());
    }

    @Test
    void throttleHoldsWhenReleased() {
        JetState s = cruising();
        float held = s.throttle();
        for (int i = 0; i < 40; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 0.0F, false));
        }
        assertEquals(held, s.throttle(), 1.0E-6F);
    }

    @Test
    void afterburnerLiftsTheCapAndDrainsCharge() {
        JetState s = cruising();
        float before = s.afterburnerCharge();
        for (int i = 0; i < 40; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 1.0F, true));
        }
        assertTrue(s.horizontalSpeed() > CONFIG.maxSpeed + 0.2D,
                "the burner must lift the ceiling, speed=" + s.horizontalSpeed());
        assertTrue(s.afterburnerCharge() < before - 0.3F,
                "the burner must burn its charge, charge=" + s.afterburnerCharge());
    }

    @Test
    void holdingTheBurnerRunsItDryAndReleasingRefillsIt() {
        JetState s = cruising();
        for (int i = 0; i < 300; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 1.0F, true));
        }
        assertEquals(0.0F, s.afterburnerCharge(), 1.0E-4F);
        assertTrue(s.horizontalSpeed() <= CONFIG.maxSpeed + 1.0E-4D,
                "a dry burner must fall back to the cruise ceiling, speed="
                        + s.horizontalSpeed());
        for (int i = 0; i < 100; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 1.0F, false));
        }
        assertTrue(s.afterburnerCharge() > 0.3F,
                "releasing the key must refill the tank, charge=" + s.afterburnerCharge());
    }

    @Test
    void diveGainsForwardSpeedPastTheCruiseCap() {
        JetState s = cruising();
        for (int i = 0; i < 60; i++) {
            s = step(s, stick(0.5F, 0.0F, 0.0F, 1.0F, false));
        }
        assertTrue(s.pitchDeg() > 35.0F, "the nose must follow the stick, pitch="
                + s.pitchDeg());
        assertTrue(s.horizontalSpeed() > CONFIG.maxSpeed + 0.01D,
                "a dive must trade altitude for speed, speed=" + s.horizontalSpeed());
    }

    @Test
    void sustainedClimbBleedsForwardSpeed() {
        JetState s = cruising();
        for (int i = 0; i < 120; i++) {
            s = step(s, stick(-0.5F, 0.0F, 0.0F, 1.0F, false));
        }
        assertTrue(s.pitchDeg() < -35.0F, "the nose must climb, pitch=" + s.pitchDeg());
        assertTrue(s.horizontalSpeed() < 1.2D,
                "a sustained climb must bleed speed, speed=" + s.horizontalSpeed());
    }

    @Test
    void atLowSpeedTheJetSinksAndTheNoseDrops() {
        JetState s = JetState.initial(0.0F);
        // The stall lasts only until the falling airframe has enough airspeed, so sample the
        // trend inside that window rather than after the nose has already recovered.
        for (int i = 0; i < 5; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 0.0F, false));
        }
        float early = s.pitchDeg();
        for (int i = 0; i < 10; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 0.0F, false));
        }
        assertTrue(s.pitchDeg() > early + 5.0F, "a stall must keep dropping the nose, early="
                + early + " late=" + s.pitchDeg());
        assertTrue(s.vy() < -0.4D, "a stall must sink, vy=" + s.vy());
    }

    @Test
    void releasedPitchSlowlyFindsLevel() {
        JetState s = cruising();
        for (int i = 0; i < 30; i++) {
            s = step(s, stick(0.5F, 0.0F, 0.0F, 1.0F, false));
        }
        assertTrue(s.pitchDeg() > 20.0F, "the stick must pitch the nose, pitch="
                + s.pitchDeg());
        for (int i = 0; i < 120; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 1.0F, false));
        }
        assertTrue(Math.abs(s.pitchDeg()) < 5.0F,
                "the nose must find level with the stick released, pitch=" + s.pitchDeg());
    }

    @Test
    void pitchHoldsWhenAutoLevelIsDisabled() {
        JetConfig fixed = JetConfig.builder()
                .pitchCenterRateDegPerTick(0.0D)
                .stallNoseDropDegPerTick(0.0D)
                .build();
        JetState s = JetState.initial(0.0F);
        for (int i = 0; i < 80; i++) {
            s = JetFlightModel.step(s, stick(0.0F, 0.0F, 0.0F, 1.0F, false), fixed);
        }
        for (int i = 0; i < 30; i++) {
            s = JetFlightModel.step(s, stick(0.5F, 0.0F, 0.0F, 1.0F, false), fixed);
        }
        float held = s.pitchDeg();
        for (int i = 0; i < 120; i++) {
            s = JetFlightModel.step(s, stick(0.0F, 0.0F, 0.0F, 1.0F, false), fixed);
        }
        assertEquals(held, s.pitchDeg(), 0.5F);
    }

    @Test
    void rollIsARateCommand() {
        JetState s = JetState.initial(0.0F);
        JetState t = step(s, new JetInput(0.0F, 1.0F, 0.0F, 0.0F, false, false, false));
        assertEquals((float) CONFIG.rollRateDegPerTick, t.rollDeg(), 1.0E-4F);
    }

    @Test
    void bankedTurnPeaksAtCornerSpeedAndGrowsWithBank() {
        double corner = CONFIG.cornerSpeedFraction * CONFIG.maxSpeed;
        double slow = JetFlightModel.turnRateDegPerTick(0.6D, 90.0F, 1.0D, CONFIG);
        double mid = JetFlightModel.turnRateDegPerTick(corner, 90.0F, 1.0D, CONFIG);
        double fast = JetFlightModel.turnRateDegPerTick(CONFIG.maxSpeed, 90.0F, 1.0D, CONFIG);
        assertTrue(mid > slow, "mid speed must out-turn a slow jet, mid=" + mid
                + " slow=" + slow);
        assertTrue(mid > fast, "mid speed must out-turn a fast jet, mid=" + mid
                + " fast=" + fast);
        double shallow = JetFlightModel.turnRateDegPerTick(corner, 30.0F, 1.0D, CONFIG);
        double steep = JetFlightModel.turnRateDegPerTick(corner, 75.0F, 1.0D, CONFIG);
        assertTrue(steep > shallow, "a steeper bank must turn harder, shallow=" + shallow
                + " steep=" + steep);
        assertTrue(JetFlightModel.turnRateDegPerTick(corner, -75.0F, 1.0D, CONFIG) < 0.0D,
                "the other bank must turn the other way");
        assertTrue(JetFlightModel.turnRateDegPerTick(corner, 90.0F, 1.0D, CONFIG)
                <= CONFIG.bankedTurnDegPerTick + 1.0E-9D, "the turn must be capped");
        assertTrue(JetFlightModel.turnRateDegPerTick(corner, 90.0F, 1.0D, CONFIG)
                > JetFlightModel.turnRateDegPerTick(corner, 90.0F, 0.0D, CONFIG),
                "pulling the stick must tighten the turn");
    }

    @Test
    void bankedPullTurnsTightlyWithoutStalling() {
        JetState s = cruising();
        for (int i = 0; i < 15; i++) {
            s = step(s, stick(0.0F, 1.0F, 0.0F, 0.0F, false));
        }
        assertTrue(Math.abs(s.rollDeg()) > 80.0F, "the roll must reach a steep bank, roll="
                + s.rollDeg());
        double before = s.yawDeg();
        double stall = CONFIG.stallSpeedFraction * CONFIG.maxSpeed;
        for (int i = 0; i < 40; i++) {
            s = step(s, stick(-1.0F, 0.0F, 0.0F, 0.0F, false));
            assertTrue(s.airspeed() > stall, "a banked pull must not stall, tick=" + i
                    + " airspeed=" + s.airspeed());
        }
        assertTrue(Math.abs(s.yawDeg() - before) > 100.0D,
                "a banked pull must turn hard, dYaw=" + (s.yawDeg() - before));
        assertTrue(s.horizontalSpeed() > 0.6D, "the turn must stay flyable, speed="
                + s.horizontalSpeed());
    }

    @Test
    void rudderYawsTheJet() {
        JetState s = cruising();
        double before = s.yawDeg();
        JetState t = step(s, stick(0.0F, 0.0F, 1.0F, 1.0F, false));
        assertEquals(CONFIG.rudderYawRateDegPerTick, t.yawDeg() - before, 1.0E-3D);
    }

    @Test
    void vtolTracksTheVerticalThrottle() {
        JetState s = JetState.initial(0.0F, true);
        for (int i = 0; i < 80; i++) {
            s = JetFlightModel.step(s, vtol(1.0F), CONFIG);
        }
        assertTrue(s.vy() > 0.3D, "full hover throttle must climb, vy=" + s.vy());
        for (int i = 0; i < 120; i++) {
            s = JetFlightModel.step(s, vtol(-1.0F), CONFIG);
        }
        assertTrue(s.vy() < -0.3D, "closed hover throttle must descend, vy=" + s.vy());
    }

    @Test
    void vtolHoldsPositionHorizontally() {
        JetState s = new JetState(0.0F, 0.0F, 0.0F, 1.0D, 0.0D, 0.0D, 0.5F, 1.0F, true);
        for (int i = 0; i < 40; i++) {
            s = JetFlightModel.step(s, vtol(0.0F), CONFIG);
        }
        assertTrue(s.horizontalSpeed() < 0.1D,
                "hover drag must kill drift, speed=" + s.horizontalSpeed());
    }

    @Test
    void diveCrashRules() {
        assertTrue(JetFlightModel.isDiveCrash(-1.2D, 10.0F, CONFIG));
        assertTrue(JetFlightModel.isDiveCrash(-0.4D, 70.0F, CONFIG));
        assertFalse(JetFlightModel.isDiveCrash(-0.4D, 10.0F, CONFIG));
        assertFalse(JetFlightModel.isDiveCrash(-0.1D, 80.0F, CONFIG));
    }

    @Test
    void nonFiniteStateAndInputStayFinite() {
        JetState broken = new JetState(Float.NaN, Float.POSITIVE_INFINITY, Float.NaN,
                Double.NaN, Double.POSITIVE_INFINITY, Double.NaN, Float.NaN, Float.NaN,
                false);
        JetState out = JetFlightModel.step(broken, new JetInput(Float.NaN, Float.NaN,
                Float.NaN, Float.NaN, false, false, false), CONFIG);
        assertFinite(out);
        assertFinite(JetFlightModel.step(null, JetInput.idle(false), CONFIG));
    }

    @Test
    void vtolHoverHoldsAltitudeAtMidThrottle() {
        JetState state = new JetState(0.0F, 0.0F, 0.0F, 0.0D, 0.0D, 0.0D, 0.5F, 1.0F, true);
        JetInput input = new JetInput(0.0F, 0.0F, 0.0F, 0.0F, false, true, false);
        for (int i = 0; i < 120; i++) {
            state = JetFlightModel.step(state, input, CONFIG);
        }
        assertTrue(state.vtol(), "the hover flag must survive the step");
        assertEquals(0.0D, state.vy(), 0.02D, "mid throttle should hold the hover");
    }

    @Test
    void vtolCapsHorizontalSpeedToTheHoverLimit() {
        JetConfig hover = JetConfig.builder().vtolMaxHorizontalSpeed(0.10D).build();
        JetState state = JetState.initial(0.0F, true);
        JetInput input = new JetInput(0.0F, 0.0F, 0.0F, 1.0F, false, true, false);
        for (int i = 0; i < 200; i++) {
            state = JetFlightModel.step(state, input, hover);
        }
        double horizontal = Math.sqrt(state.vx() * state.vx() + state.vz() * state.vz());
        assertTrue(horizontal <= 0.10D + 1.0E-6D,
                "hover must not run away, was " + horizontal);
    }

    @Test
    void groundRollKeepsTheNoseLevelBelowStallSpeed() {
        JetState state = JetState.initial(0.0F);
        JetInput grounded = new JetInput(0.0F, 0.0F, 0.0F, 0.0F, false, false, true);
        for (int i = 0; i < 60; i++) {
            state = step(state, grounded);
        }
        assertEquals(0.0F, state.pitchDeg(), 0.01F,
                "a jet parked below stall speed must not nose into the runway");
    }

    @Test
    void groundedJetKeepsADownwardProbeForGroundDetection() {
        JetState state = JetState.initial(0.0F);
        JetInput grounded = new JetInput(0.0F, 0.0F, 0.0F, 0.0F, false, false, true);
        for (int i = 0; i < 60; i++) {
            state = step(state, grounded);
            assertTrue(state.vy() < 0.0D,
                    "a grounded jet must keep sinking a little so move() still reports onGround");
            assertTrue(state.vy() > -0.2D,
                    "the ground probe must not build up speed, vy=" + state.vy());
        }
        assertEquals(0.0F, state.pitchDeg(), 0.01F,
                "the probe must not unlevel the parked nose");
    }

    @Test
    void groundRollCanRotateTheNoseUpForTakeoff() {
        JetState state = JetState.initial(0.0F);
        JetInput rotate = new JetInput(-1.0F, 0.0F, 0.0F, 0.0F, false, false, true);
        for (int i = 0; i < 30; i++) {
            state = step(state, rotate);
        }
        assertTrue(state.pitchDeg() < -30.0F,
                "stick back on the ground must raise the nose, was " + state.pitchDeg());
    }

    @Test
    void airborneStallStillDropsTheNose() {
        // Low forward speed with no fall yet, so the wing stays stalled for the whole step.
        JetState stalled = new JetState(0.0F, 0.0F, 0.0F, 0.2D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        JetInput airborne = new JetInput(0.0F, 0.0F, 0.0F, 0.0F, false, false, false);
        JetState next = step(stalled, airborne);
        assertTrue(next.pitchDeg() > 1.0F,
                "below stall speed the airborne nose must fall, was " + next.pitchDeg());
    }

    @Test
    void profilesDifferAsDesigned() {
        assertTrue(Su57Profile.config().maxSpeed > F35Profile.config().maxSpeed);
        assertTrue(Su57Profile.config().afterburnerSpeed
                > F35Profile.config().afterburnerSpeed);
        assertTrue(Su57Profile.config().bankedTurnDegPerTick
                > F35Profile.config().bankedTurnDegPerTick);
        assertTrue(Su57Profile.config().forwardAccel > F35Profile.config().forwardAccel);
        assertEquals(130, F35Profile.topSpeedKmh());
        assertEquals(180, F35Profile.afterburnerTopSpeedKmh());
        assertEquals(137, Su57Profile.topSpeedKmh());
        assertEquals(194, Su57Profile.afterburnerTopSpeedKmh());
    }
}
