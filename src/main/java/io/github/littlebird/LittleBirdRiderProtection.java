package io.github.littlebird;

import io.github.littlebird.entity.GunshipRider;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The crew of a gunship is untouchable while they are strapped in, in either seat.
 *
 * Riding already means the airframe is what descends, explodes and gets shot at, so the
 * pilot taking fall and blast damage on top of that only ever read as the aircraft
 * punishing them twice. Cancelling the attack event is enough: the hull still takes its own
 * damage, and stepping out puts the pilot back to normal vulnerability.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LittleBirdRiderProtection {
    private LittleBirdRiderProtection() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().getVehicle() instanceof GunshipRider) {
            event.setCanceled(true);
        }
    }
}
