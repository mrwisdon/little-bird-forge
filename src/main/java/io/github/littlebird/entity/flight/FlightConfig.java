package io.github.littlebird.entity.flight;

/**
 * Tunable flight-model constants. Pure data: no Minecraft classes, so the model
 * stays unit-testable. Mirrors docs/plan/flight-control.md section 5.
 */
public final class FlightConfig {
    public final double maxForwardSpeed;
    public final double forwardAccel;
    public final double reverseAccel;
    public final double sideSlipAccel;
    public final double maxClimbSpeed;
    public final double maxDescentSpeed;
    public final double maxFallSpeed;
    public final double verticalAccel;
    public final double hoverDampAccel;
    public final double maxYawRateDegPerTick;
    /** Per-tick fraction of an uncommanded vertical rate that body drag takes away. */
    public final double verticalDrag;
    /** Cap on the coordinated turn a banked airframe generates, in degrees per tick. */
    public final double bankedTurnDegPerTick;
    public final double pitchMaxDeg;
    /** Nose angle that already commands full forward thrust, so the range can be wider. */
    public final double pitchThrustReferenceDeg;
    /** How strongly the nose attitude steers the vertical travel (1 = fully physical). */
    public final double pitchDiveCoupling;
    public final double pitchRateDegPerTick;
    public final double pitchCenterRateDegPerTick;
    public final double pitchDeadzoneDeg;
    /** Roll is a rate command: degrees of bank gained per tick while the key is held. */
    public final double rollRateDegPerTick;
    /** Ticks for the roll axis to reach full deflection: the roll's start-up damping. */
    public final double rollSpinUpTicks;
    public final double rollLevelRateDegPerTick;
    public final double rotorSpoolRate;
    public final double rotorMinRpm;
    public final double gravity;
    public final double crashSpeedThreshold;
    public final double crashDamagePerSpeed;
    /** Downward speed that wrecks the airframe when it meets the ground. */
    public final double verticalCrashSpeedThreshold;
    /** Nose-down angle that wrecks it on contact even at a modest sink rate. */
    public final double diveCrashPitchDeg;
    /** Below this impact speed the contact is a landing, however steep the nose is. */
    public final double crashMinImpactSpeed;
    public final double dampingYaw;
    public final double dampingPitch;
    public final double dampingRoll;
    public final double dampingCollective;
    public final double horizontalDrag;
    public final boolean hoverAssist;
    public final double hoverSinkAccel;
    public final double rotorIdleSpinFraction;

    private FlightConfig(Builder b) {
        this.maxForwardSpeed = b.maxForwardSpeed;
        this.forwardAccel = b.forwardAccel;
        this.reverseAccel = b.reverseAccel;
        this.sideSlipAccel = b.sideSlipAccel;
        this.maxClimbSpeed = b.maxClimbSpeed;
        this.maxDescentSpeed = b.maxDescentSpeed;
        this.maxFallSpeed = b.maxFallSpeed;
        this.verticalAccel = b.verticalAccel;
        this.hoverDampAccel = b.hoverDampAccel;
        this.maxYawRateDegPerTick = b.maxYawRateDegPerTick;
        this.verticalDrag = b.verticalDrag;
        this.bankedTurnDegPerTick = b.bankedTurnDegPerTick;
        this.pitchMaxDeg = b.pitchMaxDeg;
        this.pitchThrustReferenceDeg = b.pitchThrustReferenceDeg;
        this.pitchDiveCoupling = b.pitchDiveCoupling;
        this.pitchRateDegPerTick = b.pitchRateDegPerTick;
        this.pitchCenterRateDegPerTick = b.pitchCenterRateDegPerTick;
        this.pitchDeadzoneDeg = b.pitchDeadzoneDeg;
        this.rollRateDegPerTick = b.rollRateDegPerTick;
        this.rollSpinUpTicks = b.rollSpinUpTicks;
        this.rollLevelRateDegPerTick = b.rollLevelRateDegPerTick;
        this.rotorSpoolRate = b.rotorSpoolRate;
        this.rotorMinRpm = b.rotorMinRpm;
        this.gravity = b.gravity;
        this.crashSpeedThreshold = b.crashSpeedThreshold;
        this.crashDamagePerSpeed = b.crashDamagePerSpeed;
        this.verticalCrashSpeedThreshold = b.verticalCrashSpeedThreshold;
        this.diveCrashPitchDeg = b.diveCrashPitchDeg;
        this.crashMinImpactSpeed = b.crashMinImpactSpeed;
        this.dampingYaw = b.dampingYaw;
        this.dampingPitch = b.dampingPitch;
        this.dampingRoll = b.dampingRoll;
        this.dampingCollective = b.dampingCollective;
        this.horizontalDrag = b.horizontalDrag;
        this.hoverAssist = b.hoverAssist;
        this.hoverSinkAccel = b.hoverSinkAccel;
        this.rotorIdleSpinFraction = b.rotorIdleSpinFraction;
    }

    public static FlightConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private double maxForwardSpeed = 1.3889D;
        private double forwardAccel = 0.030D;
        private double reverseAccel = 0.040D;
        private double sideSlipAccel = 0.020D;
        private double maxClimbSpeed = 0.40D;
        private double maxDescentSpeed = 0.50D;
        private double maxFallSpeed = 1.20D;
        private double verticalAccel = 0.035D;
        private double hoverDampAccel = 0.020D;
        private double maxYawRateDegPerTick = 9.0D;
        private double verticalDrag = 0.10D;
        private double bankedTurnDegPerTick = 6.0D;
        private double pitchMaxDeg = 90.0D;
        private double pitchThrustReferenceDeg = 32.0D;
        private double pitchDiveCoupling = 1.0D;
        private double pitchRateDegPerTick = 6.0D;
        private double pitchCenterRateDegPerTick = 0.0D;
        private double pitchDeadzoneDeg = 1.5D;
        private double rollRateDegPerTick = 6.0D;
        private double rollSpinUpTicks = 10.0D;
        private double rollLevelRateDegPerTick = 0.0D;
        private double rotorSpoolRate = 0.10D;
        private double rotorMinRpm = 0.05D;
        private double gravity = 0.040D;
        private double crashSpeedThreshold = 0.45D;
        private double crashDamagePerSpeed = 90.0D;
        private double verticalCrashSpeedThreshold = 1.00D;
        private double diveCrashPitchDeg = 55.0D;
        private double crashMinImpactSpeed = 0.25D;
        private double dampingYaw = 0.20D;
        private double dampingPitch = 0.25D;
        private double dampingRoll = 0.25D;
        private double dampingCollective = 0.30D;
        private double horizontalDrag = 0.03D;
        private boolean hoverAssist = false;
        private double hoverSinkAccel = 0.006D;
        private double rotorIdleSpinFraction = 0.55D;

        public Builder maxForwardSpeed(double v) { this.maxForwardSpeed = v; return this; }
        public Builder forwardAccel(double v) { this.forwardAccel = v; return this; }
        public Builder reverseAccel(double v) { this.reverseAccel = v; return this; }
        public Builder sideSlipAccel(double v) { this.sideSlipAccel = v; return this; }
        public Builder maxClimbSpeed(double v) { this.maxClimbSpeed = v; return this; }
        public Builder maxDescentSpeed(double v) { this.maxDescentSpeed = v; return this; }
        public Builder maxFallSpeed(double v) { this.maxFallSpeed = v; return this; }
        public Builder verticalAccel(double v) { this.verticalAccel = v; return this; }
        public Builder hoverDampAccel(double v) { this.hoverDampAccel = v; return this; }
        public Builder maxYawRateDegPerTick(double v) { this.maxYawRateDegPerTick = v; return this; }
        public Builder verticalDrag(double v) { this.verticalDrag = v; return this; }

        public Builder bankedTurnDegPerTick(double v) { this.bankedTurnDegPerTick = v; return this; }
        public Builder pitchMaxDeg(double v) { this.pitchMaxDeg = v; return this; }
        public Builder pitchThrustReferenceDeg(double v) { this.pitchThrustReferenceDeg = v; return this; }

        public Builder pitchDiveCoupling(double v) { this.pitchDiveCoupling = v; return this; }

        public Builder pitchRateDegPerTick(double v) { this.pitchRateDegPerTick = v; return this; }
        public Builder pitchCenterRateDegPerTick(double v) { this.pitchCenterRateDegPerTick = v; return this; }
        public Builder pitchDeadzoneDeg(double v) { this.pitchDeadzoneDeg = v; return this; }
        public Builder rollRateDegPerTick(double v) { this.rollRateDegPerTick = v; return this; }

        public Builder rollSpinUpTicks(double v) { this.rollSpinUpTicks = v; return this; }
        public Builder rollLevelRateDegPerTick(double v) { this.rollLevelRateDegPerTick = v; return this; }
        public Builder rotorSpoolRate(double v) { this.rotorSpoolRate = v; return this; }
        public Builder rotorMinRpm(double v) { this.rotorMinRpm = v; return this; }
        public Builder gravity(double v) { this.gravity = v; return this; }
        public Builder crashSpeedThreshold(double v) { this.crashSpeedThreshold = v; return this; }
        public Builder crashDamagePerSpeed(double v) { this.crashDamagePerSpeed = v; return this; }

        public Builder verticalCrashSpeedThreshold(double v) { this.verticalCrashSpeedThreshold = v; return this; }

        public Builder diveCrashPitchDeg(double v) { this.diveCrashPitchDeg = v; return this; }

        public Builder crashMinImpactSpeed(double v) { this.crashMinImpactSpeed = v; return this; }

        public Builder dampingYaw(double v) { this.dampingYaw = v; return this; }

        public Builder dampingPitch(double v) { this.dampingPitch = v; return this; }

        public Builder dampingRoll(double v) { this.dampingRoll = v; return this; }

        public Builder dampingCollective(double v) { this.dampingCollective = v; return this; }

        public Builder horizontalDrag(double v) { this.horizontalDrag = v; return this; }

        public Builder hoverAssist(boolean v) { this.hoverAssist = v; return this; }

        public Builder hoverSinkAccel(double v) { this.hoverSinkAccel = v; return this; }

        public Builder rotorIdleSpinFraction(double v) { this.rotorIdleSpinFraction = v; return this; }

        public FlightConfig build() {
            return new FlightConfig(this);
        }
    }
}
