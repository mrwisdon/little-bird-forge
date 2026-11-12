package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

final class TankOpticTest {
    @Test
    void aThinWallBetweenTickEndpointsStopsThePrediction() {
        AABB wall = new AABB(-1.0D, -1.0D, 2.0D, 1.0D, 1.0D, 2.1D);
        TankOptic.Flight flight = TankOptic.predict(Vec3.ZERO, new Vector3(0.0D, 0.0D, 1.0D),
                MainGunAmmoType.HE,
                (start, end) -> wall.clip(start, end)
                        .map(point -> new TankOptic.SegmentHit(point, TankOptic.ImpactKind.TERRAIN))
                        .orElse(null));

        assertTrue(flight.hasImpact());
        assertEquals(2.0D, flight.point().z, 1.0E-9D);
        assertEquals(-0.008D, flight.point().y, 1.0E-9D);
        assertEquals(0.4D, flight.ticks(), 1.0E-9D);
    }

    @Test
    void theOpticPredictsLessDropAndFlightTimeForArmourPiercing() {
        Vector3 aim = new Vector3(0.0D, 0.0D, 1.0D);
        AABB wall = new AABB(-1.0D, -20.0D, 90.5D, 1.0D, 1.0D, 91.0D);
        TankOptic.SegmentTrace trace = (start, end) -> wall.clip(start, end)
                .map(point -> new TankOptic.SegmentHit(point, TankOptic.ImpactKind.TERRAIN))
                .orElse(null);

        TankOptic.Flight ap = TankOptic.predict(Vec3.ZERO, aim, MainGunAmmoType.AP, trace);
        TankOptic.Flight he = TankOptic.predict(Vec3.ZERO, aim, MainGunAmmoType.HE, trace);

        assertEquals(90.5D / 9.0D, ap.ticks(), 1.0E-9D);
        assertEquals(90.5D / 5.0D, he.ticks(), 1.0E-9D);
        assertTrue(ap.point().y > he.point().y);
    }

    @Test
    void thePredictionStopsAtUnloadedTerrain() {
        AtomicInteger segments = new AtomicInteger();
        TankOptic.Flight flight = TankOptic.predict(Vec3.ZERO, new Vector3(0.0D, 0.0D, 1.0D),
                MainGunAmmoType.HE,
                (start, end) -> segments.incrementAndGet() == 2
                        ? new TankOptic.SegmentHit(null, TankOptic.ImpactKind.UNLOADED) : null);

        assertEquals(2, segments.get());
        assertEquals(TankOptic.ImpactKind.UNLOADED, flight.kind());
        assertFalse(flight.hasImpact());
    }

    @Test
    void clearFlightUsesExactlyTheServerShellLifetime() {
        AtomicInteger segments = new AtomicInteger();
        TankOptic.Flight flight = TankOptic.predict(Vec3.ZERO, new Vector3(0.0D, 0.0D, 1.0D),
                MainGunAmmoType.HE,
                (start, end) -> {
                    segments.incrementAndGet();
                    return null;
                });

        assertEquals(MainGunShellBallistics.LIFETIME_TICKS, segments.get());
        assertEquals(TankOptic.ImpactKind.NO_IMPACT, flight.kind());
    }

    @Test
    void theProjectionKeepsCameraRollZoomAndBehindCameraRejection() {
        Matrix4f rolled = new Matrix4f().perspective((float) Math.toRadians(70.0D), 16.0F / 9.0F,
                0.05F, 512.0F).rotateZ((float) Math.toRadians(90.0D));
        Vec3 point = new Vec3(1.0D, 0.0D, -10.0D);
        AimProjection.Screen wide = TankOptic.project(rolled, point, 1920, 1080);
        Matrix4f zoomed = new Matrix4f().perspective((float) Math.toRadians(35.0D), 16.0F / 9.0F,
                0.05F, 512.0F).rotateZ((float) Math.toRadians(90.0D));
        AimProjection.Screen narrow = TankOptic.project(zoomed, point, 1920, 1080);

        assertTrue(wide.visible());
        assertEquals(960.0D, wide.x(), 0.001D);
        assertTrue(narrow.y() < wide.y());
        assertFalse(TankOptic.project(rolled, new Vec3(0.0D, 0.0D, 10.0D), 1920, 1080)
                .visible());
    }
}
