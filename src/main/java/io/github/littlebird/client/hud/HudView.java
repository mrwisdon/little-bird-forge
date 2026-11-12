package io.github.littlebird.client.hud;

/** One vehicle family's HUD. The overlay picks the first view that applies. */
public interface HudView {
    boolean appliesTo(HudContext context);

    void render(HudContext context);
}
