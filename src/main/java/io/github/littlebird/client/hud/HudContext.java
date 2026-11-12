package io.github.littlebird.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Everything a HUD view needs for one frame, so views never look the screen size up themselves. */
public final class HudContext {
    public final GuiGraphics graphics;
    public final Font font;
    public final Minecraft minecraft;
    public final int width;
    public final int height;
    public final float partialTick;

    public HudContext(GuiGraphics graphics, Font font, Minecraft minecraft, int width, int height,
                      float partialTick) {
        this.graphics = graphics;
        this.font = font;
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        this.partialTick = partialTick;
    }

    /** Left column x for a panel of the given width. */
    public int leftPanelX() {
        return 10;
    }

    /** Right column x for a panel of the given width. */
    public int rightPanelX(int panelWidth) {
        return width - panelWidth - 10;
    }

    public int centreX() {
        return width / 2;
    }

    public int centreY() {
        return height / 2;
    }
}
