package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.client.AimProjection.Screen;
import io.github.littlebird.client.AimProjection.Vector;
import org.junit.jupiter.api.Test;

final class AimProjectionTest {
    private static final Vector EYE = new Vector(0.0D, 0.0D, 0.0D);
    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;
    private static final double FOV = 70.0D;
    private static final double EPSILON = 1.0E-6D;

    private static Screen project(Vector point, float yaw, float pitch, float roll) {
        return AimProjection.project(point, EYE, yaw, pitch, roll, FOV, WIDTH, HEIGHT);
    }

    @Test
    void pointStraightAheadLandsOnTheScreenCentre() {
        // Yaw 0 faces +Z in Minecraft.
        Screen screen = project(new Vector(0.0D, 0.0D, 10.0D), 0.0F, 0.0F, 0.0F);
        assertTrue(screen.visible());
        assertEquals(WIDTH / 2.0D, screen.x(), EPSILON);
        assertEquals(HEIGHT / 2.0D, screen.y(), EPSILON);
    }

    @Test
    void higherPointsProjectAboveTheCentre() {
        Screen screen = project(new Vector(0.0D, 2.0D, 10.0D), 0.0F, 0.0F, 0.0F);
        assertTrue(screen.y() < HEIGHT / 2.0D, "screen y grows downwards");
    }

    @Test
    void yawNinetyFacesMinusXInMinecraft() {
        Screen ahead = project(new Vector(-10.0D, 0.0D, 0.0D), 90.0F, 0.0F, 0.0F);
        assertTrue(ahead.visible(), "yaw 90 looks towards -X");
        assertEquals(WIDTH / 2.0D, ahead.x(), 1.0E-3D);
    }

    @Test
    void rightHandSideProjectsRightOfCentre() {
        // Facing +Z (south), the camera's right is -X (west).
        Screen screen = project(new Vector(-2.0D, 0.0D, 10.0D), 0.0F, 0.0F, 0.0F);
        assertTrue(screen.x() > WIDTH / 2.0D, "was " + screen.x());
    }

    @Test
    void pointsBehindTheCameraAreNotVisible() {
        assertFalse(project(new Vector(0.0D, 0.0D, -10.0D), 0.0F, 0.0F, 0.0F).visible());
    }

    @Test
    void rollRotatesTheProjectedPointAroundTheCentre() {
        Screen level = project(new Vector(-2.0D, 0.0D, 10.0D), 0.0F, 0.0F, 0.0F);
        Screen rolled = project(new Vector(-2.0D, 0.0D, 10.0D), 0.0F, 0.0F, 90.0F);
        double levelRadius = Math.hypot(level.x() - WIDTH / 2.0D, level.y() - HEIGHT / 2.0D);
        double rolledRadius = Math.hypot(rolled.x() - WIDTH / 2.0D, rolled.y() - HEIGHT / 2.0D);
        assertEquals(levelRadius, rolledRadius, 1.0E-3D, "roll must not change the radius");
        assertTrue(Math.abs(level.y() - rolled.y()) > 1.0D, "roll must move the point");
    }
}