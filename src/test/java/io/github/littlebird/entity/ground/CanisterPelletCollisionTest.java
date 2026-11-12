package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

final class CanisterPelletCollisionTest {
    @Test
    void thePelletVolumeHitsTheNearerBlockEvenWhenItsCentreMisses() {
        Vec3 muzzle = new Vec3(0.0D, 0.5D, 0.75D);
        Vec3 end = new Vec3(18.0D, 0.5D, 0.75D);
        VoxelShape farther = Shapes.create(new AABB(8.0D, 0.0D, 0.0D, 9.0D, 1.0D, 1.0D));
        VoxelShape nearer = Shapes.create(new AABB(7.0D, 0.0D, 1.0D, 8.0D, 1.0D, 2.0D));
        List<VoxelShape> blocks = List.of(farther, nearer);

        Vec3 impact = GroundWeapons.sweepPelletBlocks(muzzle, end, 0.5D,
                query -> blocks.stream().filter(shape -> shape.bounds().intersects(query)).toList());

        assertNotNull(impact);
        assertEquals(6.5D, impact.x, 1.0E-9D);
        assertEquals(muzzle.y, impact.y, 1.0E-9D);
        assertEquals(muzzle.z, impact.z, 1.0E-9D);
    }
}
