package io.github.littlebird.entity.flight;

/**
 * Pilot control bits shared by every aircraft in the pack. The axes the mouse cannot express
 * (collective and roll) arrive as a small bit field, which the entity turns back into the
 * analogue axes the flight model expects. Pure rules: no Minecraft classes.
 */
public final class FlightControls {
    public static final int CLIMB = 1;
    public static final int DESCEND = 2;
    public static final int ROLL_RIGHT = 4;
    public static final int ROLL_LEFT = 8;

    private FlightControls() {
    }

    /** -1..1, positive is stick right. */
    public static float rollAxis(int controls) {
        float axis = 0.0F;
        if ((controls & ROLL_RIGHT) != 0) {
            axis += 1.0F;
        }
        if ((controls & ROLL_LEFT) != 0) {
            axis -= 1.0F;
        }
        return axis;
    }

    /** -1..1, positive is collective up. */
    public static float collectiveAxis(int controls) {
        float axis = 0.0F;
        if ((controls & CLIMB) != 0) {
            axis += 1.0F;
        }
        if ((controls & DESCEND) != 0) {
            axis -= 1.0F;
        }
        return axis;
    }
}
