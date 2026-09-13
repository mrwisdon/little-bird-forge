package io.github.littlebird.entity.flight;

/**
 * Arcade four-axis helicopter model (yaw / pitch / roll / collective) with no
 * Minecraft classes, mirroring docs/plan/flight-control.md section 4.
 *
 * Axis semantics match Battlefield's suggested mouse-yaw preset: the mouse gives
 * yaw and pitch, A/D gives roll, W/S gives collective.
 */
public final class HelicopterFlightModel {
    /** Roll input below this counts as "key released". */
    private static final double ROLL_INPUT_EPSILON = 1.0E-3D;
    /** Minimum speed used by the turn formula so a hover cannot divide by zero. */
    private static final double TURN_MIN_SPEED_FRACTION = 0.05D;
    /** Below this fraction of top speed a bank slips the aircraft instead of turning it. */
    private static final double TURN_FADE_FRACTION = 0.35D;

    private HelicopterFlightModel() {
    }

    public static HelicopterState step(
            HelicopterState state, HelicopterInput rawInput, FlightConfig config) {
        HelicopterState s = sanitizeState(state);
        HelicopterInput input = rawInput.sanitized();

        float rotorRpm = (float) clamp01(approach(
                s.rotorRpm(), s.engineOn() ? 1.0D : 0.0D, config.rotorSpoolRate));
        double lift = liftScale(rotorRpm, config);

        // The wrap is part of the state contract: the entity, the heading box and the
        // coordinated turn all read yaw back as -180..180.
        float yaw = wrapDegrees((float) approachAngle(
                s.yawDeg(), input.yawDeg(), config.maxYawRateDegPerTick));

        float roll = integrateRoll(s.rollDeg(), input, config);
        // The envelope is a hard +-pitchMaxDeg: the target already respects it, but a
        // state left outside it (config shrink, saved vehicle, NaN recovery) is pulled in.
        float pitch = (float) Math.max(-config.pitchMaxDeg, Math.min(config.pitchMaxDeg,
                approach(s.pitchDeg(), pitchTarget(input, config),
                        pitchRate(input, config))));

        // Thrust follows the pilot's command rather than the held attitude: with
        // auto-return off the nose would otherwise stay down and keep pushing the
        // aircraft forward forever after the mouse is released. The command and the
        // attitude both saturate at pitchThrustReferenceDeg, so widening the pitch range
        // to +-90 does not mean having to dive ninety degrees to reach full power.
        double thrustReference = config.pitchThrustReferenceDeg > 0.0D
                ? config.pitchThrustReferenceDeg : config.pitchMaxDeg;
        double command = clamp01(Math.abs(shapedPitch(input, config))
                * config.pitchMaxDeg / thrustReference);
        double attitude = Math.min(1.0D, Math.abs(pitch) / thrustReference);
        double thrust = attitude
                * (pitch >= 0.0F ? config.forwardAccel : config.reverseAccel)
                * lift * command;
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        // The thrust leaves along the airframe's nose axis, so where the nose points is
        // where the aircraft travels: nosing down aims it forward *and* down, nosing up
        // aims it backward (braking) *and* up (a flare). cos(pitch) is the share that
        // still pushes forward; the vertical share is diveAccel further down.
        double forwardAccel = (pitch >= 0.0F ? thrust : -thrust) * cosPitch;
        // Tilting the rotor disc away from vertical also costs the rotor vertical
        // authority, which is what turns a steep dive into a dive instead of a level
        // descent the collective could hold. One knob scales the whole coupling: 0 keeps
        // the old flat model (pitch only ever pushed forward), 1 is fully physical.
        double steering = clamp01(config.pitchDiveCoupling);
        double verticalAuthority = lift * (1.0D - steering * (1.0D - cosPitch));
        double diveAccel = thrust * sinPitch * steering;
        // A bank pushes the aircraft sideways: strongest in the hover, fading out with
        // speed because the same bank becomes a coordinated turn (bankedTurnDegPerTick).
        double topSpeed = config.maxForwardSpeed <= 0.0D ? 1.0D : config.maxForwardSpeed;
        double speedFraction = clamp01(s.horizontalSpeed() / topSpeed);
        double sideAccel = Math.sin(Math.toRadians(roll)) * config.sideSlipAccel * lift
                * (1.0D - speedFraction);
        double forwardX = -Math.sin(Math.toRadians(yaw));
        double forwardZ = Math.cos(Math.toRadians(yaw));
        double rightX = -Math.cos(Math.toRadians(yaw));
        double rightZ = -Math.sin(Math.toRadians(yaw));

        double vx = s.vx() + forwardAccel * forwardX + sideAccel * rightX;
        double vz = s.vz() + forwardAccel * forwardZ + sideAccel * rightZ;
        // Air drag: the forward thrust cancels it, so letting the controls go lets the
        // aircraft slow down instead of keeping its speed forever. What cancels drag is
        // the forward component of the thrust, not the throttle: with the nose straight
        // down nothing is pushing forward any more, so the speed bleeds away.
        double drive = config.forwardAccel > 0.0D
                ? clamp01(Math.abs(forwardAccel) / config.forwardAccel) : 0.0D;
        double drag = 1.0D - config.horizontalDrag * (1.0D - drive);
        vx *= drag;
        vz *= drag;
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        if (horizontal > config.maxForwardSpeed) {
            double scale = config.maxForwardSpeed / horizontal;
            vx *= scale;
            vz *= scale;
        }

        double collective = input.collectiveInput();
        double climbTarget = collective > 0.0F
                ? collective * config.maxClimbSpeed
                : collective * config.maxDescentSpeed;
        double vy = s.vy() - diveAccel;
        if (verticalAuthority > 0.0D) {
            if (Math.abs(collective) < 1.0E-3F) {
                // Collective centred. Hover stabilisation is off by default, so the
                // aircraft settles instead of hanging in the air; the assist is kept
                // behind a config switch for players who prefer it.
                vy = config.hoverAssist
                        ? approach(vy, 0.0D, config.hoverDampAccel * verticalAuthority)
                        : vy - config.hoverSinkAccel * verticalAuthority;
                // Nobody is commanding a vertical rate, so body drag sets it: a hover sink
                // or a tilted-rotor descent settles at a sane rate instead of accelerating
                // all the way to the fall cap. Commanded rates keep the collective's
                // authority, so holding S still gives the full descent rate on demand.
                vy *= 1.0D - clamp01(config.verticalDrag);
            } else {
                vy = approach(vy, climbTarget * verticalAuthority,
                        config.verticalAccel * verticalAuthority);
            }
        }
        // Rotor lift rises with the square of RPM, so the gravity the rotor still has to
        // cover falls off quadratically: spin-up sags instead of dropping. A tilted rotor
        // gives up the same share of its lift, so past a certain nose-down the aircraft
        // stops holding altitude and the dive takes over.
        vy -= config.gravity * (1.0D - verticalAuthority)
                * (1.0D - verticalAuthority);
        if (input.onGround() && vy < 0.0D) {
            vy = 0.0D;
        }
        vy = Math.max(vy, -config.maxFallSpeed);
        vy = Math.min(vy, config.maxClimbSpeed);

        return new HelicopterState(yaw, pitch, roll, vx, vy, vz, rotorRpm, s.engineOn());
    }

    private static HelicopterState sanitizeState(HelicopterState s) {
        if (s == null) {
            return HelicopterState.initial(0.0F);
        }
        return new HelicopterState(
                HelicopterInput.finiteOr(s.yawDeg(), 0.0F),
                HelicopterInput.finiteOr(s.pitchDeg(), 0.0F),
                HelicopterInput.finiteOr(s.rollDeg(), 0.0F),
                finiteOr(s.vx()),
                finiteOr(s.vy()),
                finiteOr(s.vz()),
                (float) clamp01(HelicopterInput.finiteOr(s.rotorRpm(), 0.0F)),
                s.engineOn());
    }

    private static double finiteOr(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : value;
    }

    /** Rotor spin rate factor: idling in the hover, faster with vertical thrust. */
    public static double rotorSpinFactor(double verticalSpeed, FlightConfig config) {
        double idle = clamp01(config.rotorIdleSpinFraction);
        if (config.maxClimbSpeed <= 0.0D) {
            return idle;
        }
        double throttle = clamp01(Math.abs(verticalSpeed) / config.maxClimbSpeed);
        return idle + (1.0D - idle) * throttle;
    }

    public static double liftScale(float rotorRpm, FlightConfig config) {
        double span = 1.0D - config.rotorMinRpm;
        if (span <= 0.0D) {
            return 1.0D;
        }
        return clamp01((rotorRpm - config.rotorMinRpm) / span);
    }

    /**
     * Roll is a rate command, not a deflection: the key rolls the airframe for as long as
     * it is held, so the bank is whatever the pilot rolled it to, there is no bank limit
     * (a full roll over and inverted flight both work), and letting go holds the bank
     * where it is unless auto-level is switched on.
     */
    static float integrateRoll(float rollDeg, HelicopterInput input, FlightConfig config) {
        double command = input.rollInput();
        if (Math.abs(command) < ROLL_INPUT_EPSILON) {
            return (float) approachAngle(rollDeg, 0.0D, config.rollLevelRateDegPerTick);
        }
        return wrapDegrees((float) (rollDeg + command * config.rollRateDegPerTick));
    }

    /**
     * Coordinated turn. A banked turn of radius v^2 / (g * tan(bank)) yaws the airframe at
     * g * tan(bank) / v, so a steeper bank or a slower aircraft turns harder. The rate is
     * capped by bankedTurnDegPerTick (0 disables the assist) and fades in with speed,
     * because in the hover the same bank only slips the aircraft sideways.
     */
    public static double bankedTurnDegPerTick(
            double horizontalSpeed, float rollDeg, double lift, FlightConfig config) {
        double cap = config.bankedTurnDegPerTick;
        double usableLift = clamp01(lift);
        double reference = config.maxForwardSpeed;
        if (cap <= 0.0D || usableLift <= 0.0D || reference <= 0.0D) {
            return 0.0D;
        }
        double speed = Math.max(horizontalSpeed, reference * TURN_MIN_SPEED_FRACTION);
        double tangent = Math.tan(Math.toRadians(rollDeg));
        if (!Double.isFinite(tangent)) {
            return 0.0D;
        }
        double rate = Math.toDegrees(config.gravity * tangent / speed);
        double fade = clamp01(horizontalSpeed / (reference * TURN_FADE_FRACTION));
        return Math.max(-cap, Math.min(cap, rate)) * fade * usableLift;
    }

    /**
     * True when touching down at this attitude and rate wrecks the airframe.
     *
     * Two ways to do it: come down faster than the collective could ever fly the aircraft
     * down, or meet the ground with the nose steeply down. A collective descent, or a flat
     * settled touchdown, stays a landing however slow or fast it is.
     */
    public static boolean isDiveCrash(double verticalSpeed, float pitchDeg, FlightConfig config) {
        double descent = -verticalSpeed;
        if (!(descent >= config.crashMinImpactSpeed)) {
            return false;
        }
        return descent >= config.verticalCrashSpeedThreshold
                || pitchDeg >= config.diveCrashPitchDeg;
    }

    private static double pitchTarget(HelicopterInput input, FlightConfig config) {
        return shapedPitch(input, config) * config.pitchMaxDeg;
    }

    /** Deadzone-shaped pitch command in -1..1. */
    private static double shapedPitch(HelicopterInput input, FlightConfig config) {
        double deadzone = config.pitchMaxDeg <= 0.0D
                ? 0.0D : config.pitchDeadzoneDeg / config.pitchMaxDeg;
        double axis = input.pitchInput();
        if (Math.abs(axis) <= deadzone || deadzone >= 1.0D) {
            return 0.0D;
        }
        return (axis - Math.signum(axis) * deadzone) / (1.0D - deadzone);
    }

    private static double pitchRate(HelicopterInput input, FlightConfig config) {
        return Math.abs(input.pitchInput()) < 1.0E-3F
                ? config.pitchCenterRateDegPerTick
                : config.pitchRateDegPerTick;
    }

    /** Moves current toward target by at most maxDelta. */
    public static double approach(double current, double target, double maxDelta) {
        double delta = target - current;
        if (Math.abs(delta) <= maxDelta) {
            return target;
        }
        return current + Math.signum(delta) * maxDelta;
    }

    /** Yaw follow takes the shortest angular path. */
    public static double approachAngle(double current, double target, double maxDelta) {
        return current + approach(0.0D, wrapDegrees((float) (target - current)), maxDelta);
    }

    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
