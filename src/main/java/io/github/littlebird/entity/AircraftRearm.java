package io.github.littlebird.entity;

import io.github.littlebird.entity.weapon.AmmoSupply;
import io.github.littlebird.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/**
 * Finds the ammunition crate an aircraft is parked next to.
 *
 * Nothing regenerates ammunition on its own any more, so this search is the whole resupply
 * system: if there is no crate within SUPPLY_RADIUS_BLOCKS there is no ammunition.
 */
public final class AircraftRearm {
    private AircraftRearm() {
    }

    public static boolean crateNearby(Entity aircraft) {
        int radius = (int) Math.ceil(AmmoSupply.SUPPLY_RADIUS_BLOCKS);
        BlockPos centre = aircraft.blockPosition();
        BlockPos min = centre.offset(-radius, -3, -radius);
        BlockPos max = centre.offset(radius, 3, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (aircraft.level().getBlockState(pos).is(ModBlocks.AMMO_CRATE.get())) {
                return true;
            }
        }
        return false;
    }
}
