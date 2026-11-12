package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.client.particle.SmokePuffParticle;
import io.github.littlebird.registry.ModEntities;
import io.github.littlebird.registry.ModParticles;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
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
        event.register(LittleBirdKeyMappings.HUD_SETTINGS);
        event.register(LittleBirdKeyMappings.SALVO);
        event.register(LittleBirdKeyMappings.COCKPIT);
        event.register(LittleBirdKeyMappings.B2_POD);
        event.register(LittleBirdKeyMappings.RELOAD);
        event.register(LittleBirdKeyMappings.VTOL);
        event.register(LittleBirdKeyMappings.INVERT_PITCH);
        event.register(LittleBirdKeyMappings.TOGGLE_MOUSE_YAW);
        event.register(LittleBirdKeyMappings.PHL191_UP);
        event.register(LittleBirdKeyMappings.PHL191_DOWN);
        event.register(LittleBirdKeyMappings.PHL191_RECON);
        event.register(LittleBirdKeyMappings.ARTILLERY_STRIKE);
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
        event.registerLayerDefinition(JetModel.JET_LAYER, JetModel::createBodyLayer);
        event.registerLayerDefinition(Su57Model.SU57_LAYER, Su57Model::createBodyLayer);
        event.registerLayerDefinition(B2Model.B2_LAYER, B2Model::createBodyLayer);
        event.registerLayerDefinition(ApacheModel.APACHE_LAYER, ApacheModel::createBodyLayer);
        event.registerLayerDefinition(MainBattleTankModel.MAIN_BATTLE_TANK_LAYER,
                MainBattleTankModel::createBodyLayer);
        event.registerLayerDefinition(InfantryFightingVehicleModel.INFANTRY_FIGHTING_VEHICLE_LAYER,
                InfantryFightingVehicleModel::createBodyLayer);
        event.registerLayerDefinition(BradleyModel.BRADLEY_LAYER, BradleyModel::createBodyLayer);
        event.registerLayerDefinition(AutocannonShellModel.SHELL_LAYER,
                AutocannonShellModel::createBodyLayer);
        event.registerLayerDefinition(Phl191Model.PHL191_LAYER, Phl191Model::createBodyLayer);
        event.registerLayerDefinition(Phl191RocketModel.PHL191_ROCKET_LAYER,
                Phl191RocketModel::createBodyLayer);
        event.registerLayerDefinition(Plz05Model.PLZ05_LAYER, Plz05Model::createBodyLayer);
        event.registerLayerDefinition(Plz05ShellModel.PLZ05_ROCKET_LAYER,
                Plz05ShellModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(
            net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        // One overlay, dispatching to the HUD view that matches the vehicle.
        event.registerAboveAll("hud", new io.github.littlebird.client.hud.HudOverlay());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LITTLE_BIRD.get(), LittleBirdRenderer::new);
        event.registerEntityRenderer(ModEntities.APACHE.get(), ApacheRenderer::new);
        // Both fighters draw hand-built box models (the pack's Minecraft style).
        event.registerEntityRenderer(ModEntities.F35.get(), JetRenderer::new);
        event.registerEntityRenderer(ModEntities.SU57.get(), Su57Renderer::new);
        // The B-2 is a flying wing: draw the Blender OBJ planform, not the box model.
        event.registerEntityRenderer(ModEntities.B2.get(), B2ObjRenderer::new);
        event.registerEntityRenderer(ModEntities.MAIN_BATTLE_TANK.get(),
                MainBattleTankRenderer::new);
        event.registerEntityRenderer(ModEntities.INFANTRY_FIGHTING_VEHICLE.get(),
                InfantryFightingVehicleRenderer::new);
        event.registerEntityRenderer(ModEntities.BRADLEY.get(), BradleyRenderer::new);
        event.registerEntityRenderer(ModEntities.PHL191.get(), Phl191Renderer::new);
        event.registerEntityRenderer(ModEntities.PHL191_ROCKET.get(), Phl191RocketRenderer::new);
        event.registerEntityRenderer(ModEntities.PLZ05.get(), Plz05Renderer::new);
        event.registerEntityRenderer(ModEntities.PLZ05_SHELL.get(),
                Plz05ShellRenderer::new);
        // The 25mm round is drawn by its tracer, but the type still needs a renderer.
        // Bombs share one generic casing renderer: no baked model, no extra texture.
        event.registerEntityRenderer(ModEntities.JDAM_BOMB.get(), BombRenderer::new);
        event.registerEntityRenderer(ModEntities.INCENDIARY_BOMB.get(), BombRenderer::new);
        event.registerEntityRenderer(ModEntities.AUTOCANNON_SHELL.get(),
                AutocannonShellRenderer::new);
        // The guided missile reuses the 70mm rocket's model for now: a dedicated TOW shape is
        // polish, not behaviour.
        event.<net.minecraft.world.entity.Entity>registerEntityRenderer(
                ModEntities.APACHE_MISSILE.get(), LittleBirdRocketRenderer::new);
        event.<net.minecraft.world.entity.Entity>registerEntityRenderer(
                ModEntities.LITTLE_BIRD_ROCKET.get(), LittleBirdRocketRenderer::new);
        // The air-to-air missile shares the rocket renderer until it gets its own shape.
        event.<net.minecraft.world.entity.Entity>registerEntityRenderer(
                ModEntities.LOCK_ON_MISSILE.get(), LittleBirdRocketRenderer::new);
        // The shell draws nothing (its fireball does), but the type still needs a renderer or the
        // render dispatcher nulls out and the client crashes on the frustum check.
        event.registerEntityRenderer(ModEntities.MAIN_GUN_SHELL.get(), MainGunShellRenderer::new);
        // The canister is an ordinary thrown item: the vanilla renderer already draws it spinning.
        event.registerEntityRenderer(ModEntities.SMOKE_GRENADE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        // The cloud draws itself out of particles, so its entity is invisible scenery: the type
        // still needs a renderer, or the render dispatcher is null and the client crashes on the
        // frustum check (see the note above).
        event.registerEntityRenderer(ModEntities.SMOKE_CLOUD.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Sixteen frames of cloudy wisp: the particle walks them over its life so the smoke boils.
        event.registerSpriteSet(ModParticles.SMOKE_PUFF.get(), SmokePuffParticle.Provider::new);
    }
}
