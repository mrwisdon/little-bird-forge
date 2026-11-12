package io.github.littlebird.client.hud;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.LittleBirdKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** HUD settings (F10): scale, centre crosshair, reticle follows weapon. */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class HudSettingsScreen extends Screen {
    private static final int PANEL_W = 260;
    private static final int PANEL_H = 128;

    public HudSettingsScreen() {
        super(Component.literal("HUD settings"));
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            LittleBirdConfig.setHudScale(LittleBirdConfig.hudScale() - 1);
            refresh();
        }).bounds(left + 96, top + 30, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            LittleBirdConfig.setHudScale(LittleBirdConfig.hudScale() + 1);
            refresh();
        }).bounds(left + 168, top + 30, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Centre crosshair: "
                        + (LittleBirdConfig.showCenterCrosshair() ? "ON" : "OFF")), b -> {
            LittleBirdConfig.setShowCenterCrosshair(!LittleBirdConfig.showCenterCrosshair());
            refresh();
        }).bounds(left + 12, top + 58, 172, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Reticles follow weapon: "
                        + (LittleBirdConfig.reticleFollowsWeapon() ? "ON" : "OFF")), b -> {
            LittleBirdConfig.setReticleFollowsWeapon(!LittleBirdConfig.reticleFollowsWeapon());
            refresh();
        }).bounds(left + 12, top + 82, 172, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + PANEL_W - 80, top + PANEL_H - 26, 68, 18).build());
    }

    private void refresh() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        HudCanvas.panel(graphics, this.font, "HUD SETTINGS", null, left, top, PANEL_W, PANEL_H,
                HudStyle.ACCENT);
        HudCanvas.row(graphics, this.font, left + 12, left + PANEL_W - 12, top + 16, "SCALE",
                Integer.toString(LittleBirdConfig.hudScale()), HudStyle.TEXT);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (LittleBirdKeyMappings.HUD_SETTINGS.consumeClick()) {
            minecraft.setScreen(new HudSettingsScreen());
        }
    }
}
