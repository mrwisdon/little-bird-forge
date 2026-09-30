package io.github.littlebird.entity.weapon;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;

/**
 * Pure proportional-navigation steering for a guided missile: each tick the heading turns towards a
 * lead-predicted aim point, capped by the seeker's turn rate, and the speed is preserved.
 */
public final class MissileGuidance {
    /** IR homing turns hard with a short lead; radar reaches further but turns slower. */
    public record Spec(double maxTurnDegreesPerTick, double leadTicks) {
    }

    public static final Spec IR = new Spec(9.0D, 6.0D);
    public static final Spec RADAR = new Spec(6.0D, 12.0D);

    private MissileGuidance() {
    }

    /** Returns this tick's velocity after steering towards the target. */
    public static Vector3 home(Vector3 position, Vector3 velocity,
            Vector3 targetPosition, Vector3 targetVelocity, Spec spec) {
        Spec s = spec == null ? IR : spec;
        Vector3 lead = targetVelocity == null ? new Vector3(0.0D, 0.0D, 0.0D)
                : targetVelocity.scale(s.leadTicks());
        Vector3 aim = targetPosition.add(lead).subtract(position);
        if (aim.length() < 1.0E-6D) {
            return velocity;
        }
        double speed = velocity.length();
        if (speed < 1.0E-6D) {
            return aim.normalize().scale(1.0D);
        }
        Vector3 heading = velocity.scale(1.0D / speed);
        Vector3 desired = aim.normalize();
        double dot = Math.max(-1.0D, Math.min(1.0D, heading.dot(desired)));
        double angle = Math.acos(dot);
        double maxTurn = Math.toRadians(Math.max(0.0D, s.maxTurnDegreesPerTick()));
        Vector3 newHeading;
        if (angle <= maxTurn || angle < 1.0E-9D) {
            newHeading = desired;
        } else {
            double t = maxTurn / angle;
            newHeading = heading.scale(1.0D - t).add(desired.scale(t)).normalize();
        }
        return newHeading.scale(speed);
    }
}
