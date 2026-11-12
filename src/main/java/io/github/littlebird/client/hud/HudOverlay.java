package io.github.littlebird.client.hud;

import io.github.littlebird.client.B2PodView;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** The single HUD overlay: dispatches to the view that matches what the player is riding. */
public final class HudOverlay implements IGuiOverlay {
    private static final List<HudView> VIEWS = List.of(
            new HudB2(),
            new HudAir(),
            new HudGround(),
            new HudArtillery());

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        HudContext context = new HudContext(graphics, minecraft.font, minecraft, width, height,
                partialTick);
        for (HudView view : VIEWS) {
            if (view.appliesTo(context)) {
                view.render(context);
                return;
            }
        }
    }
}
