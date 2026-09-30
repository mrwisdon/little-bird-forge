package io.github.littlebird.entity.flight;

/**
 * Arcade fixed-wing jet model, no Minecraft classes, mirroring docs/plan/jet-plan.md
 * section 5.
 *
 * <p>Axes follow the Battlefield 2042 jet preset: mouse Y pitches, mouse X rolls as a
 * rate command, A/D works the rudder and W/S integrates the throttle. A banked airframe
 * turns through the same coordinated-turn formula the helicopters use. The wing carries
 * the airframe above the stall speed and stops carrying it below, where the nose drops
 * and the jet falls. The afterburner is a charge that buys thrust and top speed at the
 * cost of turn authority.
 */
public final class JetFlightModel {
    /** Roll input below this counts as "mouse at rest". */
    private static final double ROLL_INPUT_EPSILON = 1.0E-3D;
    /** Pitch input below this counts as "stick centred", so the nose finds level. */
    private static final double PITCH_INPUT_EPSILON = 1.0E-3D;
    /** How quickly the turn rate eases off above the corner speed. */
    private static final double HIGH_SPEED_TURN_FALLOFF = 0.8D;
    /** Downward sink left while the wheels are down, so the next move still touches the ground. */
    private static final double GROUND_CONTACT_PROBE = 0.05D;

    private JetFlightModel() {
    }

    public static JetState step(JetState state, JetInput rawInput, JetConfig config) {
        JetState s = sanitizeState(state, rawInput);
        JetInput in = rawInput.sanitized();

        float roll = integrateRoll(s.rollDeg(), in, config);
        double throttle = HelicopterFlightModel.clamp01(
                s.throttle() + in.throttleInput() * config.throttleRate);

        // Stall keys off total airspeed so a dive keeps its authority and can recover, while
        // the lift the wing makes still keys off the horizontal travel, so a straight-down
        // dive stays unsupported and keeps accelerating.
        double flow = s.horizontalSpeed();
        double airspeed = s.airspeed();
        double stallSpeed = config.stallSpeedFraction * config.maxSpeed;

        float pitch;
        // A stalled wing drops the nose only once the jet is airborne. On the ground the gear
        // holds the attitude and the pilot keeps full pitch authority, which is what lets the
        // takeoff roll rotate off the runway instead of nosing straight into it.
        boolean stalled = !in.vtol() && stallSpeed > 0.0D && airspeed < stallSpeed;
        if (stalled && !in.onGround()) {
            // Stalled: the wing is no longer flying, so the nose falls regardless of the
            // stick, which is the cue the pilot has to trade for airspeed to recover.
            double collapse = 1.0D - HelicopterFlightModel.clamp01(airspeed / stallSpeed);
            pitch = (float) (s.pitchDeg() + config.stallNoseDropDegPerTick * collapse);
        } else {
            // A banked elevator turns the jet rather than pointing the nose at the sky, so the
            // world pitch the stick asks for fades out as the bank deepens. At ninety degrees a
            // full pull is a flat turn; at level it is a loop.
            double bankFactor = Math.cos(Math.toRadians(roll));
            pitch = (float) HelicopterFlightModel.approach(
                    s.pitchDeg(), pitchTarget(in, config) * bankFactor, pitchRate(in, config));
        }
        pitch = (float) Math.max(-config.pitchMaxDeg,
                Math.min(config.pitchMaxDeg, pitch));

        boolean afterburner = in.afterburner()
                && throttle >= config.afterburnerMinThrottle
                && s.afterburnerCharge() > 0.0F;
        // The tank only refills once the pilot lets the key go, so holding a dry burner
        // does not flicker between on and off.
        double charge = HelicopterFlightModel.clamp01(s.afterburnerCharge()
                + (afterburner ? -config.afterburnerDrain
                        : (in.afterburner() ? 0.0D : config.afterburnerRegen)));

        double thrust = config.forwardAccel * throttle
                + (afterburner ? config.afterburnerAccel : 0.0D);

        double yawRad = Math.toRadians(s.yawDeg());
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        // Minecraft yaw 0 faces +Z, so forward is (-sin yaw, ., +cos yaw); positive pitch
        // is nose-down, so the vertical share is -sin pitch.
        double forwardX = -Math.sin(yawRad) * cosPitch;
        double forwardZ = Math.cos(yawRad) * cosPitch;
        double forwardY = -sinPitch;

        double vx = s.vx();
        double vy = s.vy();
        double vz = s.vz();
        double yaw = s.yawDeg();
        double capScale = 1.0D;

        if (in.vtol()) {
            // Vector-thrust hover: the throttle trims a vertical rate and hover drag holds
            // the jet roughly in place. Mouse X becomes yaw at the input layer.
            double rate = throttle >= 0.5D ? config.vtolMaxClimbSpeed : config.vtolMaxDescentSpeed;
            double climb = (throttle - 0.5D) * 2.0D * rate;
            vy = HelicopterFlightModel.approach(vy, climb, config.vtolAccel);
            vx += thrust * forwardX * 0.5D;
            vz += thrust * forwardZ * 0.5D;
            double hoverDrop = 1.0D - config.vtolHorizontalDrag;
            vx *= hoverDrop;
            vz *= hoverDrop;
            // Hover is a hover: without its own cap the vector thrust creeps the jet up to
            // cruise speed and the mode stops reading as a hover at all.
            double hoverLimit = config.vtolMaxHorizontalSpeed;
            double hoverSpeed = Math.sqrt(vx * vx + vz * vz);
            if (hoverLimit > 0.0D && hoverSpeed > hoverLimit) {
                double scale = hoverLimit / hoverSpeed;
                vx *= scale;
                vz *= scale;
            }
            yaw = HelicopterFlightModel.wrapDegrees((float) (yaw
                    + in.yawInput() * config.rudderYawRateDegPerTick));
        } else {
            vx += thrust * forwardX;
            vz += thrust * forwardZ;
            vy += thrust * forwardY;
            // The elevator tells the wing how hard to pull. Bank picks the turn plane; the
            // pull both restores the vertical share of lift and scales the turn rate, which is
            // the BF2042 roll-then-pull maneuver.
            double pull = HelicopterFlightModel.clamp01(-in.pitchInput());
            double lift = stallSpeed <= 0.0D
                    ? 1.0D
                    : HelicopterFlightModel.clamp01(flow * flow / (stallSpeed * stallSpeed));
            double support = lift * HelicopterFlightModel.clamp01(
                    Math.cos(Math.toRadians(roll)) + config.pullSupport * pull);
            vy -= config.gravity * (1.0D - support);
            vy -= vy * support * config.verticalDamping;
            double turn = turnRateDegPerTick(flow, roll, pull, config)
                    * (afterburner ? config.afterburnerTurnPenalty : 1.0D);
            yaw = HelicopterFlightModel.wrapDegrees((float) (yaw + turn
                    + in.yawInput() * config.rudderYawRateDegPerTick));
            // The flight path follows the nose. Rotating the heading without rotating the
            // velocity would smear the thrust across every heading and dump all the speed; a
            // coordinated turn carries the velocity vector around with the airframe instead.
            double dYaw = HelicopterFlightModel.wrapDegrees((float) (yaw - s.yawDeg()));
            if (dYaw != 0.0D) {
                double rad = Math.toRadians(dYaw);
                double cy = Math.cos(rad);
                double sy = Math.sin(rad);
                double rx = vx * cy - vz * sy;
                double rz = vx * sy + vz * cy;
                vx = rx;
                vz = rz;
            }
            // A held pull costs energy as induced drag, so a sustained turn bleeds speed
            // gradually instead of clamping it away in a single tick.
            double drag = config.horizontalDrag + config.inducedDrag * pull;
            vx *= 1.0D - drag;
            vz *= 1.0D - drag;
            // Energy: gravity pulls along the flight path, so a dive accelerates the airframe
            // and a climb bleeds it. Without this a jet holds speed at any attitude, the floaty
            // "on rails" feel the plan calls out as the key to jet handling.
            double climb = forwardY;
            double along = -config.gravity * config.pitchSpeedCoupling * climb;
            vx += along * forwardX;
            vz += along * forwardZ;
            // Only a dive extends the ceiling; a climb no longer clamps the speed down.
            capScale = Math.min(1.5D, 1.0D + config.diveOverspeed * Math.max(0.0D, -climb));
        }

        double cap = (afterburner ? config.afterburnerSpeed : config.maxSpeed) * capScale;
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        if (cap > 0.0D && horizontal > cap) {
            double scale = cap / horizontal;
            vx *= scale;
            vz *= scale;
        }
        vy = Math.max(vy, -config.maxFallSpeed);
        vy = Math.min(vy, config.maxClimbSpeed);
        // The ground holds the airframe, but not by zeroing the sink rate: Entity#move sets
        // onGround from verticalCollisionBelow, i.e. from a strictly downward move, so a hard
        // zero cleared onGround every other tick. That flicker re-armed the stall nose drop on
        // the runway, which is the twitch the jet showed while spooling up. A small downward
        // probe keeps the wheels in contact and the move's own collision resolves it. A positive
        // request - rotating off the runway or a hover climb - is left untouched.
        if (in.onGround() && vy <= 0.0D) {
            vy = -GROUND_CONTACT_PROBE;
        }

        return new JetState((float) yaw, pitch, roll, vx, vy, vz,
                (float) throttle, (float) charge, in.vtol());
    }

    /**
     * Signed yaw rate a banked airframe generates, in degrees per tick. Bank picks the turn
     * plane, the elevator pull scales how hard the jet pulls through it, and the rate peaks at
     * the corner speed: too slow and the wing cannot pull, too fast and the turn radius grows.
     * This is the arcade "roll, then pull" the reference notes describe, not a pure
     * coordinated-turn formula.
     */
    public static double turnRateDegPerTick(double speed, float rollDeg, double pull,
            JetConfig config) {
        double cap = config.bankedTurnDegPerTick;
        if (cap <= 0.0D) {
            return 0.0D;
        }
        double reference = config.maxSpeed > 0.0D ? config.maxSpeed : 1.0D;
        double stall = config.stallSpeedFraction * reference;
        double corner = Math.max(stall + 1.0E-4D, config.cornerSpeedFraction * reference);
        double v = Math.max(speed, 0.0D);
        double lowSpeedAuthority = HelicopterFlightModel.clamp01((v - stall) / (corner - stall));
        double highSpeedFalloff = 1.0D / (1.0D + HIGH_SPEED_TURN_FALLOFF
                * Math.max(0.0D, v - corner) / corner);
        double pullAuthority = config.turnBaseAuthority
                + config.pullTurnGain * HelicopterFlightModel.clamp01(pull);
        double bank = Math.sin(Math.toRadians(rollDeg));
        return cap * lowSpeedAuthority * highSpeedFalloff * pullAuthority * bank;
    }

    /** A hard ground contact wrecked the jet (steep descent or a deep nose-down attitude). */
    public static boolean isDiveCrash(double verticalSpeed, float pitchDeg, JetConfig config) {
        double descent = -verticalSpeed;
        if (!(descent >= config.crashMinImpactSpeed)) {
            return false;
        }
        return descent >= config.verticalCrashSpeedThreshold
                || pitchDeg >= config.diveCrashPitchDeg;
    }

    private static float integrateRoll(float rollDeg, JetInput in, JetConfig config) {
        double command = in.rollInput();
        if (Math.abs(command) < ROLL_INPUT_EPSILON) {
            return (float) HelicopterFlightModel.approachAngle(
                    rollDeg, 0.0D, config.rollLevelRateDegPerTick);
        }
        return HelicopterFlightModel.wrapDegrees(
                (float) (rollDeg + command * config.rollRateDegPerTick));
    }

    private static double pitchTarget(JetInput in, JetConfig config) {
        return shapedPitch(in, config) * config.pitchMaxDeg;
    }

    private static double shapedPitch(JetInput in, JetConfig config) {
        double deadzone = config.pitchMaxDeg <= 0.0D
                ? 0.0D : config.pitchDeadzoneDeg / config.pitchMaxDeg;
        double axis = in.pitchInput();
        if (Math.abs(axis) <= deadzone || deadzone >= 1.0D) {
            return 0.0D;
        }
        return (axis - Math.signum(axis) * deadzone) / (1.0D - deadzone);
    }

    private static double pitchRate(JetInput in, JetConfig config) {
        return Math.abs(in.pitchInput()) < PITCH_INPUT_EPSILON
                ? config.pitchCenterRateDegPerTick
                : config.pitchRateDegPerTick;
    }

    private static JetState sanitizeState(JetState s, JetInput rawInput) {
        boolean vtol = rawInput != null && rawInput.vtol();
        if (s == null) {
            return JetState.initial(0.0F, vtol);
        }
        return new JetState(
                HelicopterInput.finiteOr(s.yawDeg(), 0.0F),
                HelicopterInput.finiteOr(s.pitchDeg(), 0.0F),
                HelicopterInput.finiteOr(s.rollDeg(), 0.0F),
                finiteOr(s.vx()),
                finiteOr(s.vy()),
                finiteOr(s.vz()),
                (float) HelicopterFlightModel.clamp01(
                        HelicopterInput.finiteOr(s.throttle(), 0.0F)),
                (float) HelicopterFlightModel.clamp01(
                        HelicopterInput.finiteOr(s.afterburnerCharge(), 0.0F)),
                s.vtol());
    }

    private static double finiteOr(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : value;
    }
}
