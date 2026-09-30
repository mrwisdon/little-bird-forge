package io.github.littlebird.entity;

import io.github.littlebird.entity.flight.JetType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Twin-engine Su-57 Felon: the same systems as the F-35E on a different airframe, no VTOL. */
public class Su57Entity extends JetEntity {
    public Su57Entity(EntityType<? extends Su57Entity> type, Level level) {
        super(type, level);
    }

    @Override
    public JetType jetType() {
        return JetType.SU57;
    }
}
