package io.github.littlebird.entity;

import io.github.littlebird.entity.ground.GroundVehicleControls;

/** A ground hull that can call in a fire mission and follow its rounds from the air. */
public interface ArtilleryPlatform extends GroundVehicleControls {
    ArtilleryStrike strike();
}
