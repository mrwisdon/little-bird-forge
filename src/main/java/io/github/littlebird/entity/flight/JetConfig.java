package io.github.littlebird.entity.flight;

/**
 * Tunable fixed-wing jet constants. Pure data: no Minecraft classes, so the model
 * stays unit-testable. Mirrors docs/plan/jet-plan.md section 5.
 *
 * <p>Speeds are blocks per tick. The cruise equilibrium is set by forwardAccel and
 * horizontalDrag; maxSpeed and afterburnerSpeed are the hard ceilings the model eases
 * down to, so level flight rides just under them.
 */
public final class JetConfig {
    /** Hard horizontal ceiling without the burner, blocks/tick. */
    public final double maxSpeed;
    /** Hard horizontal ceiling with the burner lit, blocks/tick. */
    public final double afterburnerSpeed;
    public final double forwardAccel;
    public final double afterburnerAccel;
    /** Throttle travel per tick while W/S is held. */
    public final double throttleRate;
    public final double afterburnerMinThrottle;
    public final double afterburnerDrain;
    public final double afterburnerRegen;
    /** Multiplier on the banked turn while the burner burns: faster but blunter. */
    public final double afterburnerTurnPenalty;
    public final double gravity;
    /** Fraction of maxSpeed below which the wing stops carrying the airframe. */
    public final double stallSpeedFraction;
    /** Degrees the nose drops per tick at a dead stall. */
    public final double stallNoseDropDegPerTick;
    public final double bankedTurnDegPerTick;
    public final double rudderYawRateDegPerTick;
    public final double pitchMaxDeg;
    public final double pitchRateDegPerTick;
    /** Ticks the nose takes to find level with the stick released (positive stability). */
    public final double pitchCenterRateDegPerTick;
    public final double pitchDeadzoneDeg;
    /** Roll is a rate command: degrees of bank gained per tick while the mouse moves. */
    public final double rollRateDegPerTick;
    public final double rollLevelRateDegPerTick;
    public final double horizontalDrag;
    /** Per-tick fraction of vertical motion the wing takes away when it is carrying. */
    public final double verticalDamping;
    public final double pitchSpeedCoupling;
    /** Speed (fraction of maxSpeed) where the airframe turns tightest. */
    public final double cornerSpeedFraction;
    public final double turnBaseAuthority;
    public final double pullTurnGain;
    /** Extra support the elevator adds to the vertical share of lift in a bank. */
    public final double pullSupport;
    /** Extra horizontal drag per tick while the stick is held back. */
    public final double inducedDrag;
    /** How far a dive may push the ceiling above maxSpeed. */
    public final double diveOverspeed;
    public final double maxClimbSpeed;
    public final double maxFallSpeed;
    public final double vtolMaxClimbSpeed;
    public final double vtolMaxDescentSpeed;
    public final double vtolAccel;
    public final double vtolHorizontalDrag;
    public final double vtolMaxHorizontalSpeed;
    public final double crashMinImpactSpeed;
    public final double verticalCrashSpeedThreshold;
    public final double diveCrashPitchDeg;

    private JetConfig(Builder b) {
        this.maxSpeed = b.maxSpeed;
        this.afterburnerSpeed = b.afterburnerSpeed;
        this.forwardAccel = b.forwardAccel;
        this.afterburnerAccel = b.afterburnerAccel;
        this.throttleRate = b.throttleRate;
        this.afterburnerMinThrottle = b.afterburnerMinThrottle;
        this.afterburnerDrain = b.afterburnerDrain;
        this.afterburnerRegen = b.afterburnerRegen;
        this.afterburnerTurnPenalty = b.afterburnerTurnPenalty;
        this.gravity = b.gravity;
        this.stallSpeedFraction = b.stallSpeedFraction;
        this.stallNoseDropDegPerTick = b.stallNoseDropDegPerTick;
        this.bankedTurnDegPerTick = b.bankedTurnDegPerTick;
        this.rudderYawRateDegPerTick = b.rudderYawRateDegPerTick;
        this.pitchMaxDeg = b.pitchMaxDeg;
        this.pitchRateDegPerTick = b.pitchRateDegPerTick;
        this.pitchCenterRateDegPerTick = b.pitchCenterRateDegPerTick;
        this.pitchDeadzoneDeg = b.pitchDeadzoneDeg;
        this.rollRateDegPerTick = b.rollRateDegPerTick;
        this.rollLevelRateDegPerTick = b.rollLevelRateDegPerTick;
        this.horizontalDrag = b.horizontalDrag;
        this.verticalDamping = b.verticalDamping;
        this.pitchSpeedCoupling = b.pitchSpeedCoupling;
        this.cornerSpeedFraction = b.cornerSpeedFraction;
        this.turnBaseAuthority = b.turnBaseAuthority;
        this.pullTurnGain = b.pullTurnGain;
        this.pullSupport = b.pullSupport;
        this.inducedDrag = b.inducedDrag;
        this.diveOverspeed = b.diveOverspeed;
        this.maxClimbSpeed = b.maxClimbSpeed;
        this.maxFallSpeed = b.maxFallSpeed;
        this.vtolMaxClimbSpeed = b.vtolMaxClimbSpeed;
        this.vtolMaxDescentSpeed = b.vtolMaxDescentSpeed;
        this.vtolAccel = b.vtolAccel;
        this.vtolHorizontalDrag = b.vtolHorizontalDrag;
        this.vtolMaxHorizontalSpeed = b.vtolMaxHorizontalSpeed;
        this.crashMinImpactSpeed = b.crashMinImpactSpeed;
        this.verticalCrashSpeedThreshold = b.verticalCrashSpeedThreshold;
        this.diveCrashPitchDeg = b.diveCrashPitchDeg;
    }

    public static JetConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Mutable builder with the F-35E baseline. */
    public static final class Builder {
        private double maxSpeed = 1.8D;
        private double afterburnerSpeed = 2.5D;
        private double forwardAccel = 0.038D;
        private double afterburnerAccel = 0.015D;
        private double throttleRate = 0.02D;
        private double afterburnerMinThrottle = 0.85D;
        private double afterburnerDrain = 0.015D;
        private double afterburnerRegen = 0.006D;
        private double afterburnerTurnPenalty = 0.85D;
        private double gravity = 0.040D;
        private double stallSpeedFraction = 0.35D;
        private double stallNoseDropDegPerTick = 2.0D;
        private double bankedTurnDegPerTick = 6.0D;
        private double rudderYawRateDegPerTick = 2.0D;
        private double pitchMaxDeg = 90.0D;
        private double pitchRateDegPerTick = 3.0D;
        private double pitchCenterRateDegPerTick = 0.8D;
        private double pitchDeadzoneDeg = 1.5D;
        private double rollRateDegPerTick = 6.0D;
        private double rollLevelRateDegPerTick = 0.0D;
        private double horizontalDrag = 0.020D;
        private double verticalDamping = 0.10D;
        private double pitchSpeedCoupling = 0.5D;
        private double cornerSpeedFraction = 0.55D;
        private double turnBaseAuthority = 0.30D;
        private double pullTurnGain = 0.70D;
        private double pullSupport = 1.00D;
        private double inducedDrag = 0.010D;
        private double diveOverspeed = 0.35D;
        private double maxClimbSpeed = 0.60D;
        private double maxFallSpeed = 1.60D;
        private double vtolMaxClimbSpeed = 0.40D;
        private double vtolMaxDescentSpeed = 0.50D;
        private double vtolAccel = 0.03D;
        private double vtolHorizontalDrag = 0.12D;
        private double vtolMaxHorizontalSpeed = 0.45D;
        private double crashMinImpactSpeed = 0.30D;
        private double verticalCrashSpeedThreshold = 1.10D;
        private double diveCrashPitchDeg = 60.0D;

        public Builder maxSpeed(double v) { this.maxSpeed = v; return this; }
        public Builder afterburnerSpeed(double v) { this.afterburnerSpeed = v; return this; }
        public Builder forwardAccel(double v) { this.forwardAccel = v; return this; }
        public Builder afterburnerAccel(double v) { this.afterburnerAccel = v; return this; }
        public Builder throttleRate(double v) { this.throttleRate = v; return this; }
        public Builder afterburnerMinThrottle(double v) { this.afterburnerMinThrottle = v; return this; }
        public Builder afterburnerDrain(double v) { this.afterburnerDrain = v; return this; }
        public Builder afterburnerRegen(double v) { this.afterburnerRegen = v; return this; }
        public Builder afterburnerTurnPenalty(double v) { this.afterburnerTurnPenalty = v; return this; }
        public Builder gravity(double v) { this.gravity = v; return this; }
        public Builder stallSpeedFraction(double v) { this.stallSpeedFraction = v; return this; }
        public Builder stallNoseDropDegPerTick(double v) { this.stallNoseDropDegPerTick = v; return this; }
        public Builder bankedTurnDegPerTick(double v) { this.bankedTurnDegPerTick = v; return this; }
        public Builder rudderYawRateDegPerTick(double v) { this.rudderYawRateDegPerTick = v; return this; }
        public Builder pitchMaxDeg(double v) { this.pitchMaxDeg = v; return this; }
        public Builder pitchRateDegPerTick(double v) { this.pitchRateDegPerTick = v; return this; }
        public Builder pitchCenterRateDegPerTick(double v) { this.pitchCenterRateDegPerTick = v; return this; }
        public Builder pitchDeadzoneDeg(double v) { this.pitchDeadzoneDeg = v; return this; }
        public Builder rollRateDegPerTick(double v) { this.rollRateDegPerTick = v; return this; }
        public Builder rollLevelRateDegPerTick(double v) { this.rollLevelRateDegPerTick = v; return this; }
        public Builder horizontalDrag(double v) { this.horizontalDrag = v; return this; }
        public Builder verticalDamping(double v) { this.verticalDamping = v; return this; }
        public Builder pitchSpeedCoupling(double v) { this.pitchSpeedCoupling = v; return this; }
        public Builder cornerSpeedFraction(double v) { this.cornerSpeedFraction = v; return this; }
        public Builder turnBaseAuthority(double v) { this.turnBaseAuthority = v; return this; }
        public Builder pullTurnGain(double v) { this.pullTurnGain = v; return this; }
        public Builder pullSupport(double v) { this.pullSupport = v; return this; }
        public Builder inducedDrag(double v) { this.inducedDrag = v; return this; }
        public Builder diveOverspeed(double v) { this.diveOverspeed = v; return this; }
        public Builder maxClimbSpeed(double v) { this.maxClimbSpeed = v; return this; }
        public Builder maxFallSpeed(double v) { this.maxFallSpeed = v; return this; }
        public Builder vtolMaxClimbSpeed(double v) { this.vtolMaxClimbSpeed = v; return this; }
        public Builder vtolMaxDescentSpeed(double v) { this.vtolMaxDescentSpeed = v; return this; }
        public Builder vtolAccel(double v) { this.vtolAccel = v; return this; }
        public Builder vtolHorizontalDrag(double v) { this.vtolHorizontalDrag = v; return this; }
        public Builder vtolMaxHorizontalSpeed(double v) { this.vtolMaxHorizontalSpeed = v; return this; }
        public Builder crashMinImpactSpeed(double v) { this.crashMinImpactSpeed = v; return this; }
        public Builder verticalCrashSpeedThreshold(double v) { this.verticalCrashSpeedThreshold = v; return this; }
        public Builder diveCrashPitchDeg(double v) { this.diveCrashPitchDeg = v; return this; }

        public JetConfig build() {
            return new JetConfig(this);
        }
    }
}
