package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(LittleBirdKeyMappings.EXIT);
        event.register(LittleBirdKeyMappings.SALVO);
        event.register(LittleBirdKeyMappings.COCKPIT);
        event.register(LittleBirdKeyMappings.RELOAD);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                LittleBirdModel.LITTLE_BIRD_LAYER, LittleBirdModel::createBodyLayer);
        event.registerLayerDefinition(
                LittleBirdRocketModel.ROCKET_LAYER, LittleBirdRocketModel::createBodyLayer);
        event.registerLayerDefinition(
                MainGunShellModel.SHELL_LAYER, MainGunShellModel::createBodyLayer);
        event.registerLayerDefinition(ApacheModel.APACHE_LAYER, ApacheModel::createBodyLayer);
        event.registerLayerDefinition(MainBattleTankModel.MAIN_BATTLE_TANK_LAYER,
                MainBattleTankModel::createBodyLayer);
        event.registerLayerDefinition(InfantryFightingVehicleModel.INFANTRY_FIGHTING_VEHICLE_LAYER,
                InfantryFightingVehicleModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(
            net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("little_bird_hud", new LittleBirdHud());
        event.registerAboveAll("apache_hud", new ApacheHud());
        event.registerAboveAll("ground_hud", new GroundHud());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LITTLE_BIRD.get(), LittleBirdRenderer::new);
        event.registerEntityRenderer(ModEntities.APACHE.get(), ApacheRenderer::new);
        event.registerEntityRenderer(ModEntities.MAIN_BATTLE_TANK.get(),
                MainBattleTankRenderer::new);
        event.registerEntityRenderer(ModEntities.INFANTRY_FIGHTING_VEHICLE.get(),
                InfantryFightingVehicleRenderer::new);
        // The guided missile reuses the 70mm rocket's model for now: a dedicated TOW shape is
        // polish, not behaviour.
        event.<net.minecraft.world.entity.Entity>registerEntityRenderer(
                ModEntities.APACHE_MISSILE.get(), LittleBirdRocketRenderer::new);
        event.<net.minecraft.world.entity.Entity>registerEntityRenderer(
                ModEntities.LITTLE_BIRD_ROCKET.get(), LittleBirdRocketRenderer::new);
        // The shell draws nothing (its fireball does), but the type still needs a renderer or the
        // render dispatcher nulls out and the client crashes on the frustum check.
        event.registerEntityRenderer(ModEntities.MAIN_GUN_SHELL.get(), MainGunShellRenderer::new);
    }
}
