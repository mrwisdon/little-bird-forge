package io.github.littlebird.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Only two new bindings are needed: the mouse-yaw scheme reuses the vanilla
 * movement keys for collective (W/S) and roll (A/D), and the hotbar keys 1/2
 * select the weapon while piloting.
 */
public final class LittleBirdKeyMappings {
    public static final String CATEGORY = "key.categories.battlefield";

    public static final KeyMapping EXIT = new KeyMapping(
            "key.battlefield.exit",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY);

    public static final KeyMapping COCKPIT = new KeyMapping(
            "key.battlefield.cockpit",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY);

    public static final KeyMapping SALVO = new KeyMapping(
            "key.battlefield.salvo",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY);

    /** AH-64: top the magazines up from the reserves. */
    public static final KeyMapping RELOAD = new KeyMapping(
            "key.battlefield.reload",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    /** Ground vehicles: switch between the driver's weapon slots (main gun / smoke). */
    public static final KeyMapping WEAPON_SWITCH = new KeyMapping(
            "key.battlefield.weapon_switch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY);

    /** Ground vehicles: step through the 120mm shell carousel. */
    public static final KeyMapping SHELL_SWITCH = new KeyMapping(
            "key.battlefield.shell_switch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY);

    /** F-35E: toggle the vector-thrust hover mode (Battlefield uses the prone key). */
    public static final KeyMapping VTOL = new KeyMapping(
            "key.battlefield.vtol",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY);

    /** Jets: drop a flare bundle to spoof an incoming missile. */
    public static final KeyMapping FLARES = new KeyMapping(
            "key.battlefield.flares",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY);

    /** Jets: flip the pitch axis between normal and inverted; the choice is saved to config. */
    public static final KeyMapping INVERT_PITCH = new KeyMapping(
            "key.battlefield.invert_pitch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            CATEGORY);

    /** Jets: swap the mouse between roll and yaw; the choice is saved to config. */
    public static final KeyMapping TOGGLE_MOUSE_YAW = new KeyMapping(
            "key.battlefield.mouse_yaw",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            CATEGORY);

    /** AH-64: swap between the rear (pilot) and front (gunner) seat. */
    public static final KeyMapping SEAT_SWITCH = new KeyMapping(
            "key.battlefield.seat_switch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    /** B-2: raise the bombing electro-optical pod sight. */
    public static final KeyMapping B2_POD = new KeyMapping(
            "key.battlefield.b2_pod",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY);

    private LittleBirdKeyMappings() {
    }
}
