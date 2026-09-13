package io.github.littlebird.entity.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HelicopterFlightModelTest {
    private static final FlightConfig CONFIG = FlightConfig.defaults();

    private static HelicopterState step(HelicopterState s, HelicopterInput in) {
        return HelicopterFlightModel.step(s, in, CONFIG);
    }

    /** Spools the rotor to full RPM with the pilot on the sticks but idle. */
    private static HelicopterState hovering() {
        HelicopterState s = HelicopterState.initial(0.0F).withEngineOn(true);
        for (int i = 0; i < 40; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        return s;
    }

    private static HelicopterInput stick(float yaw, float pitch, float roll, float collective) {
        return new HelicopterInput(yaw, pitch, roll, collective, false);
    }

    @Test
    void rotorSpoolsUpToFullRpm() {
        HelicopterState s = HelicopterState.initial(0.0F).withEngineOn(true);
        assertEquals(0.0F, s.rotorRpm(), 1.0E-6F);
        for (int i = 0; i < 40; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertEquals(1.0F, s.rotorRpm(), 0.01F);
        assertEquals(1.0D, HelicopterFlightModel.liftScale(s.rotorRpm(), CONFIG), 1.0E-6D);
    }

    @Test
    void noseDownCruiseReachesTopSpeedWithoutExceedingIt() {
        HelicopterState s = hovering();
        for (int i = 0; i < 400; i++) {
            s = step(s, stick(0.0F, 0.5F, 0.0F, 0.0F));
        }
        assertEquals(CONFIG.maxForwardSpeed, s.horizontalSpeed(), 1.0E-6D);
        // Minecraft yaw 0 faces +Z, so forward flight must move along +Z.
        assertTrue(s.vz() > CONFIG.maxForwardSpeed * 0.99D, "expected +Z travel, vz=" + s.vz());
        assertEquals(0.0D, s.vx(), 1.0E-9D);
    }

    @Test
    void noseUpDeceleratesBackwards() {
        HelicopterState s = hovering();
        for (int i = 0; i < 40; i++) {
            s = step(s, stick(0.0F, -0.5F, 0.0F, 0.0F));
        }
        assertTrue(s.vz() < -0.1D, "expected reverse travel, vz=" + s.vz());
        assertTrue(s.pitchDeg() < -40.0F, "the nose comes up, pitch=" + s.pitchDeg());
    }

    @Test
    void theNoseSteersTheTravel() {
        // Nosing down aims the thrust at the ground as well as forward, so the cruise
        // sinks; nosing up aims it backwards and upwards, so braking climbs (a flare).
        HelicopterState down = hovering();
        for (int i = 0; i < 60; i++) {
            down = step(down, stick(0.0F, 0.5F, 0.0F, 0.0F));
        }
        assertTrue(down.vz() > 0.3D, "nosing down must travel forward, vz=" + down.vz());
        assertTrue(down.vy() < -0.1D, "and it must sink, vy=" + down.vy());

        HelicopterState up = hovering();
        for (int i = 0; i < 60; i++) {
            up = step(up, stick(0.0F, -0.5F, 0.0F, 0.0F));
        }
        assertTrue(up.vz() < -0.1D, "nosing up must brake and reverse, vz=" + up.vz());
        assertTrue(up.vy() > 0.1D, "and the flare must climb, vy=" + up.vy());
    }

    @Test
    void aNinetyDegreeDiveTradesForwardSpeedForDescent() {
        HelicopterState s = hovering();
        for (int i = 0; i < 90; i++) {
            s = step(s, stick(0.0F, 1.0F, 0.0F, 0.0F));
        }
        assertEquals(CONFIG.pitchMaxDeg, s.pitchDeg(), 0.01F);
        assertTrue(s.horizontalSpeed() < 0.05D,
                "straight down there is nothing left pushing forward, speed="
                        + s.horizontalSpeed());
        assertTrue(s.vy() < -0.5D,
                "the dive is the travel instead, and body drag caps how fast, vy=" + s.vy());
    }

    @Test
    void pitchHoldsWhenReleasedBecauseAutoReturnIsOff() {
        HelicopterState s = hovering();
        for (int i = 0; i < 20; i++) {
            s = step(s, stick(0.0F, 1.0F, 0.0F, 0.0F));
        }
        assertEquals(CONFIG.pitchMaxDeg, s.pitchDeg(), 0.01D);
        for (int i = 0; i < 40; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertEquals(CONFIG.pitchMaxDeg, s.pitchDeg(), 0.01D,
                "with auto-return disabled the nose keeps the attitude it had");
    }

    @Test
    void pitchReturnsToLevelWhenAutoReturnIsEnabled() {
        FlightConfig autoLevel = FlightConfig.builder()
                .pitchCenterRateDegPerTick(3.0D)
                .rollLevelRateDegPerTick(4.5D)
                .build();
        HelicopterState s = HelicopterState.initial(0.0F).withEngineOn(true);
        for (int i = 0; i < 40; i++) {
            s = HelicopterFlightModel.step(s, HelicopterInput.idle(0.0F, false), autoLevel);
        }
        for (int i = 0; i < 20; i++) {
            s = HelicopterFlightModel.step(s, stick(0.0F, 1.0F, 0.0F, 0.0F), autoLevel);
        }
        assertEquals(autoLevel.pitchMaxDeg, s.pitchDeg(), 0.01D);
        for (int i = 0; i < 40; i++) {
            s = HelicopterFlightModel.step(s, HelicopterInput.idle(0.0F, false), autoLevel);
        }
        assertEquals(0.0F, s.pitchDeg(), 0.01F, "auto-return brings the nose back");
    }

    @Test
    void pitchDeadzoneIgnoresTinyInput() {
        HelicopterState s = hovering();
        float tiny = (float) (CONFIG.pitchDeadzoneDeg / CONFIG.pitchMaxDeg) * 0.5F;
        for (int i = 0; i < 40; i++) {
            s = step(s, stick(0.0F, tiny, 0.0F, 0.0F));
        }
        assertEquals(0.0F, s.pitchDeg(), 1.0E-6F);
        assertEquals(0.0D, s.horizontalSpeed(), 1.0E-9D);
    }

    @Test
    void pitchReachesTheFullNinetyDegrees() {
        HelicopterState s = hovering();
        for (int i = 0; i < 30; i++) {
            s = step(s, stick(0.0F, 1.0F, 0.0F, 0.0F));
        }
        assertEquals(90.0F, s.pitchDeg(), 1.0E-4F, "the nose reaches straight down");
        assertEquals(CONFIG.pitchMaxDeg, s.pitchDeg(), 1.0E-4F);
        for (int i = 0; i < 30; i++) {
            s = step(s, stick(0.0F, -1.0F, 0.0F, 0.0F));
        }
        assertEquals(-90.0F, s.pitchDeg(), 1.0E-4F, "and straight up");
    }

    @Test
    void theNoseFollowsTheMouseOneToOne() {
        HelicopterState s = hovering();
        for (int i = 0; i < 30; i++) {
            s = step(s, stick(0.0F, 0.5F, 0.0F, 0.0F));
        }
        assertTrue(Math.abs(s.pitchDeg() - 45.0F) < 2.0F,
                "half a look angle is half the attitude, pitch=" + s.pitchDeg());
    }

    @Test
    void aModerateNoseDownAlreadyReachesFullSpeed() {
        HelicopterState s = hovering();
        for (int i = 0; i < 200; i++) {
            s = step(s, stick(0.0F, 0.38F, 0.0F, 0.0F));
        }
        assertTrue(s.pitchDeg() > 30.0F && s.pitchDeg() < 40.0F,
                "a third of the stick is a cruise attitude, pitch=" + s.pitchDeg());
        assertEquals(CONFIG.maxForwardSpeed, s.horizontalSpeed(), 1.0E-6D,
                "the wide pitch range still gives full power at that attitude");
    }

    @Test
    void divingIntoTheGroundWrecksTheAirframe() {
        assertTrue(HelicopterFlightModel.isDiveCrash(-1.10D, 80.0F, CONFIG),
                "a steep dive at full sink speed writes it off");
        assertTrue(HelicopterFlightModel.isDiveCrash(-1.10D, 5.0F, CONFIG),
                "terminal speed wrecks it even with the nose level");
        assertTrue(HelicopterFlightModel.isDiveCrash(-0.40D, 60.0F, CONFIG),
                "a steep nose down on contact wrecks it at a modest sink rate");
    }

    @Test
    void gentleDescentsStayLandings() {
        assertFalse(HelicopterFlightModel.isDiveCrash(-0.45D, 5.0F, CONFIG),
                "a collective descent is still a landing");
        assertFalse(HelicopterFlightModel.isDiveCrash(-0.20D, 40.0F, CONFIG),
                "a flat settled touchdown is still a landing");
        assertFalse(HelicopterFlightModel.isDiveCrash(-0.40D, 60.0F,
                FlightConfig.builder().diveCrashPitchDeg(179.0D).build()),
                "the dive angle is tunable");
        assertFalse(HelicopterFlightModel.isDiveCrash(Double.NaN, 0.0F, CONFIG), "NaN safe");
        assertTrue(HelicopterFlightModel.isDiveCrash(-1.20D, -90.0F, CONFIG),
                "terminal speed wrecks it whatever the nose is doing");
    }

    @Test
    void pitchIsClampedToTheConfiguredRange() {
        HelicopterState s = new HelicopterState(
                0.0F, 120.0F, 0.0F, 0.0D, 0.0D, 0.0D, 1.0F, true);
        HelicopterState next = step(s, HelicopterInput.idle(0.0F, false));
        assertEquals(CONFIG.pitchMaxDeg, next.pitchDeg(), 1.0E-4F,
                "an out-of-range attitude is pulled back inside the envelope");
    }

    @Test
    void rollIsARateCommandSoHoldingRollsFurther() {
        HelicopterState s = hovering();
        HelicopterState one = step(s, stick(0.0F, 0.0F, 1.0F, 0.0F));
        HelicopterState two = step(one, stick(0.0F, 0.0F, 1.0F, 0.0F));
        assertEquals(CONFIG.rollRateDegPerTick, one.rollDeg(), 1.0E-4D,
                "one tick of roll gives one tick of bank, not a fixed angle");
        assertEquals(CONFIG.rollRateDegPerTick * 2.0D, two.rollDeg(), 1.0E-4D);
    }

    @Test
    void rollKeepsTurningWhileHeldAndWrapsOverTheTop() {
        HelicopterState s = hovering();
        for (int i = 0; i < 20; i++) {
            s = step(s, stick(0.0F, 0.0F, 1.0F, 0.0F));
        }
        assertEquals(20.0D * CONFIG.rollRateDegPerTick, s.rollDeg(), 1.0E-3D);
        assertTrue(s.rollDeg() > 90.0F,
                "there is no bank limit any more, roll=" + s.rollDeg());
        for (int i = 0; i < 11; i++) {
            s = step(s, stick(0.0F, 0.0F, 1.0F, 0.0F));
        }
        assertEquals(31.0D * CONFIG.rollRateDegPerTick - 360.0D, s.rollDeg(), 1.0E-3D,
                "past 180 degrees the bank wraps instead of running away");
    }

    @Test
    void releasedRollHoldsTheBankBecauseAutoReturnIsOff() {
        HelicopterState s = hovering();
        for (int i = 0; i < 5; i++) {
            s = step(s, stick(0.0F, 0.0F, 1.0F, 0.0F));
        }
        float held = s.rollDeg();
        assertTrue(held > 1.0F, "the test needs a real bank, roll=" + held);
        for (int i = 0; i < 40; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertEquals(held, s.rollDeg(), 1.0E-4D,
                "the bank is held until the pilot rolls it back");
        for (int i = 0; i < 5; i++) {
            s = step(s, stick(0.0F, 0.0F, -1.0F, 0.0F));
        }
        assertEquals(0.0F, s.rollDeg(), 1.0E-4D, "opposite roll rolls it back");
    }

    @Test
    void rollLevelsOutWhenAutoReturnIsEnabled() {
        FlightConfig autoLevel = FlightConfig.builder()
                .rollLevelRateDegPerTick(4.5D)
                .build();
        HelicopterState s = new HelicopterState(
                0.0F, 0.0F, 150.0F, 0.0D, 0.0D, 0.0D, 1.0F, true);
        for (int i = 0; i < 60; i++) {
            s = HelicopterFlightModel.step(s, HelicopterInput.idle(0.0F, false), autoLevel);
        }
        assertEquals(0.0F, s.rollDeg(), 0.01F,
                "auto-return takes the shortest way back to level");
    }

    @Test
    void bankedHoverSlipsSideways() {
        // Banked 30 degrees and left alone: auto-return is off, so the bank is held.
        HelicopterState s = new HelicopterState(
                0.0F, 0.0F, 30.0F, 0.0D, 0.0D, 0.0D, 1.0F, true);
        for (int i = 0; i < 40; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertEquals(30.0F, s.rollDeg(), 1.0E-4D, "the bank is held through the slip");
        // A right bank at yaw 0 pushes the aircraft toward -X (right of +Z).
        assertTrue(s.vx() < -0.05D, "expected -X slip, vx=" + s.vx());
    }

    @Test
    void bankedCruiseTurnsTheHeading() {
        double right = HelicopterFlightModel.bankedTurnDegPerTick(0.6D, 45.0F, 1.0D, CONFIG);
        assertTrue(right > 0.5D, "a right bank must yaw right, rate=" + right);
        assertTrue(right <= CONFIG.bankedTurnDegPerTick, "and stays under the cap");
        assertEquals(-right, HelicopterFlightModel.bankedTurnDegPerTick(
                0.6D, -45.0F, 1.0D, CONFIG), 1.0E-9D, "left bank turns the other way");
    }

    @Test
    void steeperBanksAndSlowerFlightTurnHarder() {
        double shallow = HelicopterFlightModel.bankedTurnDegPerTick(0.6D, 20.0F, 1.0D, CONFIG);
        double steep = HelicopterFlightModel.bankedTurnDegPerTick(0.6D, 40.0F, 1.0D, CONFIG);
        double fast = HelicopterFlightModel.bankedTurnDegPerTick(0.9D, 20.0F, 1.0D, CONFIG);
        assertTrue(steep > shallow, "steeper bank: " + shallow + " -> " + steep);
        assertTrue(shallow > fast, "slower flight: " + fast + " -> " + shallow);
    }

    @Test
    void bankedHoverAndDeadRotorDoNotTurn() {
        assertEquals(0.0D, HelicopterFlightModel.bankedTurnDegPerTick(
                0.0D, 45.0F, 1.0D, CONFIG), 1.0E-9D, "a hover slips, it does not turn");
        assertEquals(0.0D, HelicopterFlightModel.bankedTurnDegPerTick(
                0.6D, 45.0F, 0.0D, CONFIG), 1.0E-9D, "no rotor lift, no turn");
        assertEquals(0.0D, HelicopterFlightModel.bankedTurnDegPerTick(0.6D, 45.0F, 1.0D,
                FlightConfig.builder().bankedTurnDegPerTick(0.0D).build()),
                1.0E-9D, "the assist can be switched off");
    }

    @Test
    void yawFollowIsRateLimited() {
        HelicopterState s = hovering();
        HelicopterState next = step(s, stick(90.0F, 0.0F, 0.0F, 0.0F));
        assertEquals(CONFIG.maxYawRateDegPerTick, next.yawDeg(), 1.0E-4D);
        HelicopterState opposite = step(s, stick(180.0F, 0.0F, 0.0F, 0.0F));
        assertEquals(CONFIG.maxYawRateDegPerTick, Math.abs(opposite.yawDeg()), 1.0E-4D);
    }

    @Test
    void yawTakesTheShortestPathAcrossTheWrap() {
        HelicopterState s = HelicopterState.initial(179.0F).withEngineOn(true);
        for (int i = 0; i < 40; i++) {
            s = step(s, HelicopterInput.idle(179.0F, false));
        }
        HelicopterState next = step(s, stick(-179.0F, 0.0F, 0.0F, 0.0F));
        assertEquals(-179.0F, next.yawDeg(), 0.01F);
        assertTrue(HelicopterFlightModel.wrapDegrees(next.yawDeg()) >= -180.0F);
    }

    @Test
    void collectiveClimbsAndDescends() {
        HelicopterState s = hovering();
        // The spool-up sag means the climb starts from a negative rate, so the
        // collective is held long enough to reach the target.
        for (int i = 0; i < 60; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, 1.0F));
        }
        assertEquals(CONFIG.maxClimbSpeed, s.vy(), 1.0E-6D);
        for (int i = 0; i < 60; i++) {
            s = step(s, stick(0.0F, 0.0F, 0.0F, -1.0F));
        }
        assertEquals(-CONFIG.maxDescentSpeed, s.vy(), 1.0E-6D);
    }

    @Test
    void engineStartOnlySagsGently() {
        HelicopterState s = HelicopterState.initial(0.0F).withEngineOn(true);
        for (int i = 0; i < 20; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertTrue(s.vy() > -0.3D,
                "spin-up must not drop the aircraft like a stone, vy=" + s.vy());
        assertEquals(1.0F, s.rotorRpm(), 0.01F, "one second to full RPM");
    }

    @Test
    void centredCollectiveSettlesInsteadOfHovering() {
        HelicopterState s = hovering();
        for (int i = 0; i < 40; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertTrue(s.vy() < 0.0D,
                "with hover assist off the aircraft settles, vy=" + s.vy());
        for (int i = 0; i < 400; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertTrue(s.vy() > -0.2D,
                "a hands off sink settles on body drag instead of running to the fall cap,"
                        + " vy=" + s.vy());
    }

    @Test
    void aTiltedRotorDescentAlsoSettles() {
        HelicopterState s = hovering();
        for (int i = 0; i < 400; i++) {
            s = step(s, stick(0.0F, 0.5F, 0.0F, 0.0F));
        }
        assertTrue(s.vy() > -0.6D,
                "nosing down for a descent must stay landable, vy=" + s.vy());
        assertTrue(s.vy() < 0.0D, "and it is still descending, vy=" + s.vy());
    }

    @Test
    void hoverAssistStillHoldsAltitudeWhenEnabled() {
        FlightConfig assist = FlightConfig.builder().hoverAssist(true).build();
        HelicopterState s = new HelicopterState(
                0.0F, 0.0F, 0.0F, 0.0D, 0.30D, 0.0D, 1.0F, true);
        for (int i = 0; i < 40; i++) {
            s = HelicopterFlightModel.step(s, HelicopterInput.idle(0.0F, false), assist);
        }
        assertEquals(0.0D, s.vy(), 1.0E-6D);
    }

    @Test
    void releasedNoseLetsTheAircraftSlowDown() {
        HelicopterState s = hovering();
        for (int i = 0; i < 60; i++) {
            s = step(s, stick(0.0F, 0.5F, 0.0F, 0.0F));
        }
        double accelerated = s.horizontalSpeed();
        assertTrue(accelerated > 0.5D, "it should be flying forward, speed=" + accelerated);
        for (int i = 0; i < 60; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertTrue(s.horizontalSpeed() < accelerated * 0.6D,
                "air drag must bleed the speed off, was " + s.horizontalSpeed());
        assertTrue(s.horizontalSpeed() > 0.0D, "drag never reverses the motion");
    }

    @Test
    void fullThrustStillReachesTopSpeedWithDrag() {
        HelicopterState s = hovering();
        for (int i = 0; i < 400; i++) {
            s = step(s, stick(0.0F, 0.5F, 0.0F, 0.0F));
        }
        assertEquals(CONFIG.maxForwardSpeed, s.horizontalSpeed(), 1.0E-6D,
                "drag is cancelled by full thrust, so the cap still applies");
    }

    @Test
    void deadRotorSinksAndFallSpeedIsClamped() {
        HelicopterState s = HelicopterState.initial(0.0F);
        for (int i = 0; i < 400; i++) {
            s = step(s, HelicopterInput.idle(0.0F, false));
        }
        assertTrue(s.vy() < 0.0D);
        assertEquals(-CONFIG.maxFallSpeed, s.vy(), 1.0E-6D);
        assertFalse(s.engineOn());
    }

    @Test
    void groundContactStopsDescent() {
        HelicopterState s = hovering();
        s = step(s, new HelicopterInput(0.0F, 0.0F, 0.0F, -1.0F, true));
        assertEquals(0.0D, s.vy(), 1.0E-9D);
    }

    @Test
    void nonFiniteInputAndStateStayFinite() {
        HelicopterState s = hovering();
        HelicopterState r = step(s, new HelicopterInput(
                Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN, false));
        assertFinite(r);
        HelicopterState broken = new HelicopterState(
                Float.NaN, Float.NaN, Float.NaN, Double.NaN, Double.NaN, Double.NaN, Float.NaN, true);
        assertFinite(step(broken, HelicopterInput.idle(0.0F, false)));
    }

    private static void assertFinite(HelicopterState s) {
        assertTrue(Float.isFinite(s.yawDeg()), "yaw");
        assertTrue(Float.isFinite(s.pitchDeg()), "pitch");
        assertTrue(Float.isFinite(s.rollDeg()), "roll");
        assertTrue(Double.isFinite(s.vx()), "vx");
        assertTrue(Double.isFinite(s.vy()), "vy");
        assertTrue(Double.isFinite(s.vz()), "vz");
        assertTrue(Float.isFinite(s.rotorRpm()), "rpm");
    }
}
