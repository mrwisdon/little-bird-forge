package io.github.littlebird;

import io.github.littlebird.entity.flight.FlightConfig;
import io.github.littlebird.entity.ground.GroundDrive;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Player-facing tuning. CLIENT holds the mouse-yaw feel and HUD options that
 * only the pilot's own client evaluates; COMMON holds the values the server has
 * to agree on, such as weapon damage and rearming.
 */
public final class LittleBirdConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec COMMON_SPEC;

    // CLIENT
    private static final ForgeConfigSpec.DoubleValue MAX_FORWARD_SPEED;
    private static final ForgeConfigSpec.DoubleValue YAW_RATE;
    private static final ForgeConfigSpec.DoubleValue PITCH_RATE;
    private static final ForgeConfigSpec.DoubleValue PITCH_DEADZONE;
    private static final ForgeConfigSpec.DoubleValue PITCH_MAX;
    private static final ForgeConfigSpec.DoubleValue PITCH_DIVE;
    private static final ForgeConfigSpec.BooleanValue AUTO_LEVEL;
    private static final ForgeConfigSpec.DoubleValue BANKED_TURN;
    private static final ForgeConfigSpec.DoubleValue ROLL_RATE;
    private static final ForgeConfigSpec.IntValue ROLL_SPIN_UP;
    private static final ForgeConfigSpec.DoubleValue DAMPING_YAW;
    private static final ForgeConfigSpec.DoubleValue DAMPING_PITCH;
    private static final ForgeConfigSpec.DoubleValue DAMPING_ROLL;
    private static final ForgeConfigSpec.DoubleValue DAMPING_COLLECTIVE;
    private static final ForgeConfigSpec.DoubleValue CAMERA_ROLL_FACTOR;
    private static final ForgeConfigSpec.DoubleValue THIRD_PERSON_FOV;
    private static final ForgeConfigSpec.DoubleValue APACHE_THIRD_PERSON_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue JET_THIRD_PERSON_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue B2_THIRD_PERSON_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue B2_THIRD_PERSON_FOV;
    private static final ForgeConfigSpec.IntValue JET_FLARE_COUNT;
    private static final ForgeConfigSpec.DoubleValue JET_LOCK_RANGE;
    private static final ForgeConfigSpec.BooleanValue JET_INVERT_PITCH;
    private static final ForgeConfigSpec.BooleanValue JET_MOUSE_YAW;
    private static final ForgeConfigSpec.DoubleValue COCKPIT_FOV;

    private static final ForgeConfigSpec.DoubleValue COCKPIT_EYE_LIFT;
    private static final ForgeConfigSpec.IntValue HUD_SCALE;
    private static final ForgeConfigSpec.BooleanValue SHOW_GUN_RETICLE;
    private static final ForgeConfigSpec.BooleanValue SHOW_CENTER_CROSSHAIR;
    private static final ForgeConfigSpec.BooleanValue SHOW_ROCKET_RETICLE;
    private static final ForgeConfigSpec.BooleanValue RETICLE_FOLLOWS_WEAPON;
    private static final ForgeConfigSpec.BooleanValue SHOULDER_VIEW;
    private static final ForgeConfigSpec.DoubleValue SHOULDER_OFFSET;
    private static final ForgeConfigSpec.DoubleValue SHOULDER_HEIGHT;
    // CLIENT - B-2 pod optics and the rigid cockpit view
    private static final ForgeConfigSpec.DoubleValue POD_ZOOM_TIGHT_FOV;
    private static final ForgeConfigSpec.DoubleValue POD_ZOOM_WIDE_FOV;
    private static final ForgeConfigSpec.DoubleValue POD_GIMBAL_YAW_LIMIT;
    private static final ForgeConfigSpec.DoubleValue POD_SIGHT_DOWN_DEG;
    private static final ForgeConfigSpec.DoubleValue POD_SIGHT_UP_DEG;
    private static final ForgeConfigSpec.BooleanValue COCKPIT_RIGID_VIEW;

    // CLIENT - ground vehicles
    private static final ForgeConfigSpec.DoubleValue TANK_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue TANK_ACCELERATION;
    private static final ForgeConfigSpec.DoubleValue TANK_BRAKING;
    private static final ForgeConfigSpec.DoubleValue TANK_TURN_RATE;
    private static final ForgeConfigSpec.DoubleValue TANK_TURN_STANDSTILL;
    private static final ForgeConfigSpec.DoubleValue TANK_STEP_HEIGHT;
    private static final ForgeConfigSpec.DoubleValue TANK_MAX_GRADE;
    private static final ForgeConfigSpec.DoubleValue IFV_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue IFV_ACCELERATION;
    private static final ForgeConfigSpec.DoubleValue IFV_BRAKING;
    private static final ForgeConfigSpec.DoubleValue IFV_TURN_RATE;
    private static final ForgeConfigSpec.DoubleValue IFV_TURN_STANDSTILL;
    private static final ForgeConfigSpec.DoubleValue IFV_STEP_HEIGHT;
    private static final ForgeConfigSpec.DoubleValue IFV_MAX_GRADE;
    private static final ForgeConfigSpec.DoubleValue GROUND_SIGHT_FOV;
    private static final ForgeConfigSpec.DoubleValue MAIN_GUN_TRACER_LENGTH;
    private static final ForgeConfigSpec.DoubleValue MAIN_GUN_IMPACT_SHAKE;
    private static final ForgeConfigSpec.DoubleValue MAIN_GUN_SHELL_VISIBLE_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue MAIN_GUN_TRACER_MIN_PIXEL_WIDTH;
    private static final ForgeConfigSpec.DoubleValue MAIN_GUN_TRACER_MIN_PIXEL_LENGTH;
    private static final ForgeConfigSpec.DoubleValue MAIN_GUN_IMPACT_VISIBLE_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue AUTOCANNON_SHELL_VISIBLE_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue AUTOCANNON_IMPACT_VISIBLE_DISTANCE;

    // COMMON
    private static final ForgeConfigSpec.IntValue TOW_LAUNCH_TICKS;
    private static final ForgeConfigSpec.DoubleValue TOW_TURN_RATE;
    private static final ForgeConfigSpec.DoubleValue TOW_SIGHT_RANGE;
    private static final ForgeConfigSpec.DoubleValue GUN_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue ROCKET_DIRECT_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue GUN_DAMAGE_AIRCRAFT;
    private static final ForgeConfigSpec.DoubleValue ROCKET_DAMAGE_AIRCRAFT;
    private static final ForgeConfigSpec.DoubleValue GUN_DAMAGE_APACHE;
    private static final ForgeConfigSpec.DoubleValue ROCKET_DAMAGE_APACHE;
    private static final ForgeConfigSpec.DoubleValue ROCKET_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.IntValue ROCKET_REARM_SECONDS;
    private static final ForgeConfigSpec.IntValue ROCKET_REARM_BATCH;
    private static final ForgeConfigSpec.IntValue LITTLE_BIRD_ROCKET_RELOAD_SECONDS;
    private static final ForgeConfigSpec.BooleanValue ROCKETS_BREAK_BLOCKS;
    private static final ForgeConfigSpec.DoubleValue APACHE_ROCKET_DAMAGE_SCALE;
    private static final ForgeConfigSpec.DoubleValue APACHE_GUN_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue APACHE_GUN_AIRFRAME_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue APACHE_GUN_SPLASH_RADIUS;
    private static final ForgeConfigSpec.DoubleValue APACHE_GUN_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue APACHE_MISSILE_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_HE_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue IFV_AUTOCANNON_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue IFV_ATGM_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_AUTOCANNON_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_TOW_HEAVY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue APACHE_MISSILE_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue APACHE_MISSILE_AIRFRAME_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue APACHE_MISSILE_SPLASH_RADIUS;
    private static final ForgeConfigSpec.DoubleValue MISSILE_PARTICLE_RANGE;
    private static final ForgeConfigSpec.IntValue AMMO_CRATE_BULLETS_PER_SECOND;
    private static final ForgeConfigSpec.IntValue AMMO_CRATE_ROCKETS_PER_SECOND;
    private static final ForgeConfigSpec.IntValue AMMO_CRATE_MISSILES_PER_SECOND;
    private static final ForgeConfigSpec.BooleanValue MISSILE_DIRECT_HIT_KILLS;
    private static final ForgeConfigSpec.IntValue APACHE_ROCKET_RELOAD_SECONDS;
    private static final ForgeConfigSpec.IntValue APACHE_ROCKET_RESUPPLY_SECONDS;
    private static final ForgeConfigSpec.IntValue APACHE_MISSILE_RELOAD_SECONDS;
    private static final ForgeConfigSpec.IntValue APACHE_MISSILE_RESUPPLY_SECONDS;
    // COMMON - B-2 bomb bay
    private static final ForgeConfigSpec.DoubleValue JDAM_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue INCENDIARY_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue JDAM_BLAST_RADIUS;
    private static final ForgeConfigSpec.DoubleValue INCENDIARY_BLAST_RADIUS;
    private static final ForgeConfigSpec.IntValue INCENDIARY_FIRE_TICKS;
    private static final ForgeConfigSpec.IntValue BOMB_BAY_CAPACITY_JDAM;
    private static final ForgeConfigSpec.IntValue BOMB_BAY_CAPACITY_INCENDIARY;

    // COMMON - ground vehicles
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_SPLASH_RADIUS;
    private static final ForgeConfigSpec.IntValue TANK_MAIN_GUN_FIRE_INTERVAL;
    private static final ForgeConfigSpec.IntValue TANK_MAIN_GUN_READY;
    private static final ForgeConfigSpec.IntValue TANK_MAIN_GUN_RESERVE;
    private static final ForgeConfigSpec.IntValue TANK_MAIN_GUN_RELOAD;
    private static final ForgeConfigSpec.BooleanValue TANK_MAIN_GUN_AUTO_RELOAD;
    private static final ForgeConfigSpec.IntValue TANK_MAIN_GUN_RESUPPLY_TICKS;
    private static final ForgeConfigSpec.IntValue TANK_MAIN_GUN_RESUPPLY_BATCH;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_HE_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_HE_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_HE_SPLASH_RADIUS;
    private static final ForgeConfigSpec.IntValue TANK_MAIN_GUN_CANISTER_PELLETS;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_CANISTER_PELLET_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_CANISTER_PELLET_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_CANISTER_SPREAD_HORIZONTAL_DEGREES;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_CANISTER_SPREAD_VERTICAL_DEGREES;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_CANISTER_RANGE;
    private static final ForgeConfigSpec.BooleanValue TANK_MAIN_GUN_BREAKS_BLOCKS;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_PARTICLE_SCALE;
    private static final ForgeConfigSpec.DoubleValue TANK_COAX_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_COAX_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.IntValue TANK_COAX_CAPACITY;
    private static final ForgeConfigSpec.IntValue TANK_COAX_FIRE_INTERVAL;
    private static final ForgeConfigSpec.IntValue TANK_CUPOLA_CAPACITY;
    private static final ForgeConfigSpec.IntValue TANK_CUPOLA_FIRE_INTERVAL;
    private static final ForgeConfigSpec.IntValue TANK_SMOKE_CHARGES;
    private static final ForgeConfigSpec.IntValue TANK_SMOKE_COOLDOWN_SECONDS;
    private static final ForgeConfigSpec.IntValue TANK_SMOKE_DURATION_SECONDS;
    private static final ForgeConfigSpec.DoubleValue TANK_SMOKE_RADIUS;
    private static final ForgeConfigSpec.DoubleValue IFV_AUTOCANNON_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue IFV_AUTOCANNON_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.IntValue IFV_AUTOCANNON_CAPACITY;
    private static final ForgeConfigSpec.DoubleValue IFV_ATGM_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue IFV_ATGM_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue IFV_ATGM_SPLASH_RADIUS;
    private static final ForgeConfigSpec.IntValue IFV_ATGM_READY;
    private static final ForgeConfigSpec.IntValue IFV_ATGM_RESERVE;
    private static final ForgeConfigSpec.IntValue IFV_ATGM_RELOAD;
    private static final ForgeConfigSpec.IntValue IFV_ATGM_FIRE_INTERVAL;

    // M2A2 Bradley: a tracked six seat IFV with a 25mm chain gun, a TOW rail and a smoke wall.
    private static final ForgeConfigSpec.DoubleValue BRADLEY_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_ACCELERATION;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_BRAKING;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_TURN_RATE;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_TURN_STANDSTILL;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_STEP_HEIGHT;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_MAX_GRADE;
    private static final ForgeConfigSpec.IntValue BRADLEY_AUTOCANNON_ROUNDS_PER_MINUTE;
    private static final ForgeConfigSpec.BooleanValue BRADLEY_AUTOCANNON_BREAKS_BLOCKS;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_AUTOCANNON_SPLASH_RADIUS;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_AUTOCANNON_SHELL_SPEED;
    private static final ForgeConfigSpec.IntValue BRADLEY_AUTOCANNON_SHELL_LIFETIME;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_AUTOCANNON_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_AUTOCANNON_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.IntValue BRADLEY_AUTOCANNON_READY;
    private static final ForgeConfigSpec.IntValue BRADLEY_AUTOCANNON_RESERVE;
    private static final ForgeConfigSpec.IntValue BRADLEY_AUTOCANNON_RELOAD;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_TOW_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_TOW_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_TOW_SPLASH_RADIUS;
    private static final ForgeConfigSpec.IntValue BRADLEY_TOW_READY;
    private static final ForgeConfigSpec.IntValue BRADLEY_TOW_RESERVE;
    private static final ForgeConfigSpec.IntValue BRADLEY_TOW_RELOAD;
    private static final ForgeConfigSpec.IntValue BRADLEY_TOW_FIRE_INTERVAL;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_TOW_LAUNCHER_LATERAL;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_M2_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_M2_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.IntValue BRADLEY_M2_CAPACITY;
    private static final ForgeConfigSpec.IntValue BRADLEY_M2_FIRE_INTERVAL;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_M2_SPREAD;
    private static final ForgeConfigSpec.IntValue BRADLEY_SMOKE_CANISTERS;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_SMOKE_SPACING;
    private static final ForgeConfigSpec.DoubleValue BRADLEY_SMOKE_AHEAD;
    private static final ForgeConfigSpec.IntValue BRADLEY_SMOKE_COOLDOWN;
    private static final ForgeConfigSpec.IntValue BRADLEY_SMOKE_DURATION;
    private static final ForgeConfigSpec.IntValue BRADLEY_SMOKE_CHARGES;

    static {
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.comment("Mouse-yaw flight feel. Higher rates make the helicopter turn faster.")
                .push("flight");
        MAX_FORWARD_SPEED = client
                .comment("Maximum horizontal speed in blocks per tick.",
                        "1.3889 = 27.8 blocks/s = 100 km/h, which is what the HUD reads.")
                .defineInRange("max_forward_speed", 1.3889D, 0.2D, 2.0D);
        YAW_RATE = client
                .comment("Maximum yaw change per tick in degrees (9 = 180 degrees/s).")
                .defineInRange("yaw_rate_degrees_per_tick", 9.0D, 1.0D, 30.0D);
        PITCH_RATE = client
                .comment("How fast the nose follows the mouse pitch, in degrees per tick.")
                .defineInRange("pitch_rate_degrees_per_tick", 6.0D, 1.0D, 30.0D);
        PITCH_DEADZONE = client
                .comment("Mouse pitch deadzone in degrees, to damp hand shake.")
                .defineInRange("pitch_deadzone_degrees", 1.5D, 0.0D, 10.0D);
        PITCH_MAX = client
                .comment("How far the nose can pitch, in degrees. The nose follows the mouse all",
                        "the way to straight up and straight down; full forward thrust is",
                        "already reached at 32 degrees, so cruise power needs no dive.")
                .defineInRange("pitch_max_degrees", 90.0D, 10.0D, 90.0D);
        PITCH_DIVE = client
                .comment("How strongly the nose attitude steers the vertical travel.",
                        "1.0 is fully physical: nosing down trades the rotor's lift for a dive",
                        "and nosing up flares the aircraft into a climb. 0 keeps the old flat",
                        "model, where the nose only ever pushed the aircraft forward and the",
                        "collective alone held the altitude.")
                .defineInRange("pitch_dive_coupling", 1.0D, 0.0D, 1.0D);
        AUTO_LEVEL = client
                .comment("Return the nose and the bank to level when the controls are released.",
                        "Off by default: the aircraft holds the attitude you left it in.")
                .define("auto_level", false);
        BANKED_TURN = client
                .comment("Cap on the coordinated turn a bank produces, in degrees per tick: a steeper",
                        "bank or a slower aircraft turns harder until it reaches this cap.",
                        "0 disables the turn assist and leaves only the sideways slip.")
                .defineInRange("banked_turn_degrees_per_tick", 6.0D, 0.0D, 15.0D);
        ROLL_RATE = client
                .comment("How fast the bank grows while the roll key is held, in degrees per tick",
                        "(6 = 120 degrees/s, so a full roll takes three seconds). Roll is a rate",
                        "command: the bank stays where it is when the key is released.")
                .defineInRange("roll_rate_degrees_per_tick", 6.0D, 1.0D, 30.0D);
        ROLL_SPIN_UP = client
                .comment("Ticks the roll rate takes to build up to full. This is the start-up",
                        "damping of a roll: higher feels heavier, lower snaps over faster.",
                        "Releasing the key is never damped, so the bank is held exactly.")
                .defineInRange("roll_spin_up_ticks", 10, 2, 40);
        client.comment("Control damping: fraction of the commanded deflection applied per tick.",
                        "1.0 disables damping, lower values make the helicopter feel heavier.")
                .push("damping");
        DAMPING_YAW = client.defineInRange("yaw_damping", 0.20D, 0.02D, 1.0D);
        DAMPING_PITCH = client.defineInRange("pitch_damping", 0.25D, 0.02D, 1.0D);
        DAMPING_ROLL = client.defineInRange("roll_damping", 0.25D, 0.02D, 1.0D);
        DAMPING_COLLECTIVE = client.defineInRange("collective_damping", 0.30D, 0.02D, 1.0D);
        client.pop();
        client.comment("Presentation options.").push("view");
        CAMERA_ROLL_FACTOR = client
                .comment("How strongly the camera rolls with the aircraft (0 disables it).")
                .defineInRange("camera_roll_factor", 1.0D, 0.0D, 1.0D);
        THIRD_PERSON_FOV = client
                .comment("Field of view multiplier for the exterior camera.")
                .defineInRange("third_person_fov_multiplier", 1.35D, 0.5D, 2.5D);
        COCKPIT_FOV = client
                .comment("Field of view multiplier for the cockpit camera.")
                .defineInRange("cockpit_fov_multiplier", 1.00D, 0.5D, 2.0D);

        APACHE_THIRD_PERSON_DISTANCE = client
                .comment("Exterior camera distance for the Apache, in blocks. Vanilla sits four",
                        "blocks behind the seat, which is inside a nine block airframe, so the",
                        "aircraft filled the screen; this is how far back the crew camera goes.")
                .defineInRange("apache_third_person_distance", 9.0D, 4.0D, 24.0D);
        JET_THIRD_PERSON_DISTANCE = client
                .comment("Exterior camera distance for the F-35E, in blocks. The jet is eleven",
                        "blocks long, so it needs more room behind the seat than the helicopters.")
                .defineInRange("jet_third_person_distance", 11.0D, 4.0D, 32.0D);
        B2_THIRD_PERSON_DISTANCE = client
                .comment("Exterior camera distance for the B-2, in blocks. The flying wing is 38",
                        "blocks across: the camera has to pull right back or the lens ends up",
                        "inside the wing and the whole span never fits on screen.")
                .defineInRange("b2_third_person_distance", 52.0D, 12.0D, 120.0D);
        B2_THIRD_PERSON_FOV = client
                .comment("Field of view multiplier for the B-2's exterior camera. Wider than the",
                        "fighter's so a 38 block span still reads as one aeroplane.")
                .defineInRange("b2_third_person_fov_multiplier", 1.60D, 0.5D, 3.0D);
        JET_FLARE_COUNT = client
                .comment("Flare bundles the F-35E carries before it has to rearm.")
                .defineInRange("jet_flare_count", 8, 0, 64);
        JET_LOCK_RANGE = client
                .comment("IR seeker lock range against airframes, in blocks.")
                .defineInRange("jet_lock_range_blocks", 96.0D, 16.0D, 256.0D);
        JET_INVERT_PITCH = client
                .comment("Invert the jet's pitch axis so mouse up pushes the nose down. The",
                        "same flag is on a key binding (default I) that saves it back here.")
                .define("jet_invert_pitch", false);
        JET_MOUSE_YAW = client
                .comment("Fly the jet with the helicopters' scheme: mouse X yaws and A/D rolls.",
                        "Off is the Battlefield preset (mouse X rolls, A/D yaws). The same flag",
                        "is on a key binding (default Y) that saves it back here.")
                .define("jet_mouse_yaw", false);
        COCKPIT_EYE_LIFT = client
                .comment("How far the cockpit camera is lifted out of the airframe, in blocks.",
                        "The cabin shell and roof are hidden in this view, so lifting it only",
                        "raises the sight line over the nose deck.")
                .defineInRange("cockpit_eye_lift_blocks", 0.40D, 0.0D, 1.5D);
        SHOW_GUN_RETICLE = client
                .comment("Draw the projected machine gun impact cross.")
                .define("show_gun_reticle", true);
        SHOULDER_VIEW = client
                .comment("Hold the aim button in third person for an over-the-shoulder camera.")
                .define("shoulder_view", true);
        SHOULDER_OFFSET = client
                .comment("How far the shoulder camera shifts towards the aircraft's left, in blocks.")
                .defineInRange("shoulder_offset_blocks", 1.6D, 0.0D, 4.0D);
        SHOULDER_HEIGHT = client
                .comment("How far the shoulder camera rises, in blocks.")
                .defineInRange("shoulder_height_blocks", 0.6D, -1.0D, 3.0D);
        RETICLE_FOLLOWS_WEAPON = client
                .comment("Show only the selected weapon's impact reticle, switching as you",
                        "change weapons. Off draws every enabled reticle at once.")
                .define("reticle_follows_weapon", true);
        SHOW_CENTER_CROSSHAIR = client
                .comment("Draw the crosshair in the middle of the screen while piloting.",
                        "Off by default: the projected impact reticles are the aiming reference,",
                        "and Minecraft's own crosshair is suppressed while piloting anyway.")
                .define("show_center_crosshair", false);
        SHOW_ROCKET_RETICLE = client
                .comment("Draw the projected rocket impact reticle.")
                .define("show_rocket_reticle", true);
        HUD_SCALE = client
                .comment("HUD scale multiplier applied on top of the GUI scale.")
                .defineInRange("hud_scale", 2, 1, 4);
        client.comment("Weapon fire effects. All of these are presentation only: the server",
                        "never reads them, so two clients can tune the same shot differently.")
                .push("effects");
        GROUND_SIGHT_FOV = client
                .comment("Field of view multiplier for the ground vehicles' gun sight camera.",
                        "Below 1.0 zooms the optic in: the tank's sight is a magnifying",
                        "telescope, and the reticle is drawn against this same zoom.")
                .defineInRange("ground_sight_fov_multiplier", 0.55D, 0.20D, 1.50D);
        MAIN_GUN_TRACER_LENGTH = client
                .comment("Length of the rigid tracer streak drawn behind a flying 120mm round,",
                        "in blocks. A longer streak reads as a harder, faster dart; 0 draws",
                        "the round without a streak.")
                .defineInRange("main_gun_tracer_length_blocks", 6.0D, 0.0D, 24.0D);
        MAIN_GUN_IMPACT_SHAKE = client
                .comment("Camera shock from a 120mm impact, 0 disables it. 1.0 is the nominal",
                        "punch at the point of impact; it falls off with distance and is",
                        "deliberately allowed past 1.0 for a heavier feel.")
                .defineInRange("main_gun_impact_shake", 1.0D, 0.0D, 2.0D);
        MAIN_GUN_SHELL_VISIBLE_DISTANCE = client
                .comment("How far away a flying 120mm round is still drawn, in blocks.",
                        "Vanilla culls an entity once it is about 64 AABB sizes away, which",
                        "for a 0.3 block shell is under twenty blocks; a tracer meant to be",
                        "watched across a valley has to say otherwise. The world's own",
                        "render distance is still the hard upper bound.")
                .defineInRange("main_gun_shell_visible_distance_blocks", 512.0D, 64.0D, 2048.0D);
        MAIN_GUN_TRACER_MIN_PIXEL_WIDTH = client
                .comment("Minimum on-screen thickness of the tracer streak, in framebuffer",
                        "pixels. A fixed world-space bar shrinks below a pixel past a hundred",
                        "blocks and starts to flicker out, so the streak widens with range",
                        "until it is at least this thick. 0 keeps the strict world size.")
                .defineInRange("main_gun_tracer_min_pixel_width", 2.0D, 0.0D, 8.0D);
        MAIN_GUN_TRACER_MIN_PIXEL_LENGTH = client
                .comment("Minimum on-screen length of the tracer streak, in framebuffer pixels.",
                        "Same reasoning as the width: a six block streak is a quarter of a",
                        "degree at eight hundred blocks, which is a speck. 0 keeps the",
                        "strict world length.")
                .defineInRange("main_gun_tracer_min_pixel_length", 24.0D, 0.0D, 160.0D);
        AUTOCANNON_SHELL_VISIBLE_DISTANCE = client
                .comment("How far away a flying 25mm round is still drawn, in blocks. Vanilla",
                        "culls an entity 64 bounding box sizes out, which is sixteen blocks",
                        "for this round - the Bradley's tracer used to vanish almost at the",
                        "muzzle. Default covers the round's whole 360 block flight.")
                .defineInRange("autocannon_shell_visible_distance_blocks", 384.0D, 64.0D, 2048.0D);
        AUTOCANNON_IMPACT_VISIBLE_DISTANCE = client
                .comment("How far away a 25mm impact is still reported to a client, in blocks.",
                        "Shorter than the 120mm's: this gun lands five rounds a second, so",
                        "the hit has to read without shipping a packet to the whole map.")
                .defineInRange("autocannon_impact_visible_distance_blocks", 192.0D, 64.0D, 1024.0D);
        MAIN_GUN_IMPACT_VISIBLE_DISTANCE = client
                .comment("How far away a 120mm impact is still reported to a client that",
                        "cannot see the server's own particles, in blocks. Vanilla only",
                        "ships particles and sounds within about 32 blocks, so a long shot",
                        "needs its hit sent separately or the round simply vanishes.")
                .defineInRange("main_gun_impact_visible_distance_blocks", 512.0D, 64.0D, 1024.0D);
        client.pop();
        client.comment("Ground vehicle handling. Motion is client authoritative, so this is a",
                        "client side feel setting just like the flight block above. Speeds are",
                        "blocks per tick: 0.45 is nine blocks a second.").push("ground");
        client.comment("M1A5 main battle tank: heavy, slow, but it pivots on its tracks.").push("tank");
        TANK_MAX_SPEED = client
                .comment("Top speed in blocks per tick. 0.45 is nine blocks a second.")
                .defineInRange("max_speed", 0.45D, 0.05D, 1.0D);
        TANK_ACCELERATION = client
                .comment("Speed gained per tick while the throttle is held.")
                .defineInRange("acceleration", 0.05D, 0.001D, 0.2D);
        TANK_BRAKING = client
                .comment("Speed lost per tick when braking or coasting.")
                .defineInRange("braking", 0.05D, 0.001D, 0.3D);
        TANK_TURN_RATE = client
                .comment("Degrees of heading change per tick at full speed.")
                .defineInRange("turn_rate_degrees_per_tick", 2.4D, 0.2D, 15.0D);
        TANK_TURN_STANDSTILL = client
                .comment("Fraction of the turn rate available stopped, so a tracked hull can",
                        "pivot on the spot.")
                .defineInRange("turn_at_standstill", 0.5D, 0.0D, 1.0D);
        TANK_STEP_HEIGHT = client
                .comment("Tallest lip the tracks climb, in blocks. Vanilla only gives a",
                        "LivingEntity step height, so GroundMover climbs for this hull.")
                .defineInRange("step_height", 1.5D, 0.0D, 3.0D);
        TANK_MAX_GRADE = client
                .comment("Grade at which the hull is down to its crawl speed, in degrees: about a",
                        "third of top speed here, and slower still past it. Ground under an eighth",
                        "of this is treated as flat, and steep ground always crawls instead of",
                        "stalling in place.")
                .defineInRange("max_grade_degrees", 50.0D, 5.0D, 60.0D);
        client.pop();
        client.comment("EBAA Wildcat IFV: faster and nimbler, but it cannot pivot in place.")
                .push("ifv");
        IFV_MAX_SPEED = client
                .comment("Top speed in blocks per tick. 0.62 is about twelve blocks a second.")
                .defineInRange("max_speed", 0.62D, 0.05D, 1.0D);
        IFV_ACCELERATION = client
                .comment("Speed gained per tick while the throttle is held.")
                .defineInRange("acceleration", 0.07D, 0.001D, 0.2D);
        IFV_BRAKING = client
                .comment("Speed lost per tick when braking or coasting.")
                .defineInRange("braking", 0.065D, 0.001D, 0.3D);
        IFV_TURN_RATE = client
                .comment("Degrees of heading change per tick at full speed.")
                .defineInRange("turn_rate_degrees_per_tick", 4.0D, 0.2D, 15.0D);
        IFV_TURN_STANDSTILL = client
                .comment("Fraction of the turn rate available stopped. Wheels barely steer",
                        "standing still.")
                .defineInRange("turn_at_standstill", 0.25D, 0.0D, 1.0D);
        IFV_STEP_HEIGHT = client
                .comment("Tallest lip the wheels climb, in blocks. Lower than the tank's.")
                .defineInRange("step_height", 1.0D, 0.0D, 3.0D);
        IFV_MAX_GRADE = client
                .comment("Grade at which the hull is down to its crawl speed, in degrees: about a",
                        "third of top speed here, and slower still past it. Ground under an eighth",
                        "of this is treated as flat, and steep ground always crawls instead of",
                        "stalling in place. 50 is the design target the wheels must hold.")
                .defineInRange("max_grade_degrees", 50.0D, 5.0D, 60.0D);
        client.pop();
        client.comment("M2A2 Bradley: a tracked IFV. It pivots on the spot, which the wheeled",
                        "Wildcat cannot, but it tops out lower and turns more slowly at speed.")
                .push("bradley");
        BRADLEY_MAX_SPEED = client
                .comment("Top speed in blocks per tick. 0.52 is about ten blocks a second.")
                .defineInRange("max_speed", 0.52D, 0.05D, 1.0D);
        BRADLEY_ACCELERATION = client
                .comment("Speed gained per tick while the throttle is held.")
                .defineInRange("acceleration", 0.05D, 0.001D, 0.2D);
        BRADLEY_BRAKING = client
                .comment("Speed lost per tick when braking or coasting.")
                .defineInRange("braking", 0.07D, 0.001D, 0.3D);
        BRADLEY_TURN_RATE = client
                .comment("Degrees of heading change per tick at full speed.")
                .defineInRange("turn_rate_degrees_per_tick", 5.5D, 0.2D, 15.0D);
        BRADLEY_TURN_STANDSTILL = client
                .comment("Fraction of the turn rate available stopped. Tracks pivot, so this is",
                        "much higher than the wheeled IFV's.")
                .defineInRange("turn_at_standstill", 0.55D, 0.0D, 1.0D);
        BRADLEY_STEP_HEIGHT = client
                .comment("Tallest lip the tracks climb, in blocks.")
                .defineInRange("step_height", 1.0D, 0.0D, 3.0D);
        BRADLEY_MAX_GRADE = client
                .comment("Grade at which the hull is down to its crawl speed, in degrees. Ground",
                        "under an eighth of this is flat, and steep ground crawls rather than",
                        "stalling.")
                .defineInRange("max_grade_degrees", 50.0D, 5.0D, 60.0D);
        client.pop();
        client.pop();
        client.comment("B-2 Spirit: the electro-optical pod and the rigid cockpit view.",
                        "Client side only - the server never reads these.").push("b2");
        POD_ZOOM_TIGHT_FOV = client
                .comment("Tightest sight picture, in degrees of field of view (smaller is more",
                        "magnified; 6 degrees is about a 12x sight).")
                .defineInRange("pod_zoom_tight_fov", 6.0D, 2.0D, 60.0D);
        POD_ZOOM_WIDE_FOV = client
                .comment("Widest sight picture, in degrees. 70 matches a normal Minecraft lens, so",
                        "the pod starts with the ground and the horizon both in frame.")
                .defineInRange("pod_zoom_wide_fov", 70.0D, 20.0D, 110.0D);
        POD_GIMBAL_YAW_LIMIT = client
                .comment("How far the pod gimbal may swing off the nose, in degrees.")
                .defineInRange("pod_gimbal_yaw_limit", 110.0D, 0.0D, 180.0D);
        POD_SIGHT_DOWN_DEG = client
                .comment("How far the sight may tilt DOWN from the horizon, in degrees. This is the",
                        "number that decides whether the pod can see the ground at all: 85 is",
                        "nearly straight down. Values under 45 are raised to 45 in code.")
                .defineInRange("pod_sight_down_deg", 85.0D, 10.0D, 90.0D);
        POD_SIGHT_UP_DEG = client
                .comment("How far the sight may tilt UP from the horizon, in degrees. 12 is enough",
                        "to frame a target ahead and above while keeping the ground in shot.")
                .defineInRange("pod_sight_up_deg", 12.0D, 0.0D, 90.0D);
        COCKPIT_RIGID_VIEW = client
                .comment("Lock the cockpit camera rigidly to the airframe: no smoothing and no",
                        "head lag. Off leaves the vanilla softening in place.")
                .define("cockpit_rigid_view", true);
        client.pop();
        CLIENT_SPEC = client.build();

        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.comment("Ammunition crate: aircraft parked next to one are restocked at these",
                        "rates. Nothing regenerates on its own any more, so these are the only",
                        "way a crew gets its belt and its rockets back.")
                .push("ammo_crate");
        AMMO_CRATE_BULLETS_PER_SECOND = common
                .comment("Rounds of 7.62mm or 30mm added per second.")
                .defineInRange("bullets_per_second", 40, 1, 400);
        AMMO_CRATE_ROCKETS_PER_SECOND = common
                .comment("70mm rockets added to the reserve per second.")
                .defineInRange("rockets_per_second", 2, 1, 40);
        AMMO_CRATE_MISSILES_PER_SECOND = common
                .comment("TOW missiles added to the reserve per second.")
                .defineInRange("missiles_per_second", 1, 1, 20);
        common.pop();
        MISSILE_PARTICLE_RANGE = common
                .comment("How far away players still see the TOW's twin smoke trail, in blocks.",
                        "A vanilla particle broadcast is only delivered within 32 blocks, so the",
                        "trail used to disappear while the missile itself was still tracked.")
                .defineInRange("missile_particle_range", 96.0D, 64.0D, 256.0D);
        common.comment("Weapon and airframe balance. Values must match on client and server.")
                .push("balance");
        common.comment("The command line-of-sight missile both the Apache and the Bradley carry.",
                        "The round is steered onto the sight line the operator is holding, so the",
                        "turn rate is what decides how sharply moving the crosshair drags it.")
                .push("tow");
        TOW_LAUNCH_TICKS = common
                .comment("Ticks the round flies straight out of the tube before the guidance",
                        "starts steering it. 0 makes it start turning on the launch tick.")
                .defineInRange("launch_ticks", 6, 0, 40);
        TOW_TURN_RATE = common
                .comment("Degrees per tick the missile may turn towards the sight line.",
                        "7 was the old fixed value and reads as barely steering; 12 makes a",
                        "swing of the crosshair visibly whip the round.")
                .defineInRange("turn_rate_degrees_per_tick", 12.0D, 1.0D, 45.0D);
        TOW_SIGHT_RANGE = common
                .comment("How far along the sight line the aim point is looked for, in blocks.")
                .defineInRange("sight_range_blocks", 64.0D, 8.0D, 160.0D);
        common.pop();
        GUN_DAMAGE = common
                .comment("Damage per machine gun round (14 kills an unarmoured player in two hits).")
                .defineInRange("gun_damage", 14.0D, 0.5D, 60.0D);
        ROCKET_DIRECT_DAMAGE = common
                .comment("Damage a 70mm rocket deals to the entity it directly hits.")
                .defineInRange("rocket_direct_hit_damage", 40.0D, 0.0D, 200.0D);
        GUN_DAMAGE_AIRCRAFT = common
                .comment("Damage a machine gun round does to another Little Bird's airframe.",
                        "One point on purpose: a dogfight is a long burst, not two hits.")
                .defineInRange("gun_damage_aircraft", 1.0D, 0.0D, 60.0D);
        ROCKET_DAMAGE_AIRCRAFT = common
                .comment("Damage a 70mm rocket does to another Little Bird's airframe. Direct",
                        "hit only: the warhead's blast never bills an airframe a second time.")
                .defineInRange("rocket_damage_aircraft", 10.0D, 0.0D, 200.0D);
        GUN_DAMAGE_APACHE = common
                .comment("Damage a machine gun round does to the AH-64's airframe: 0.5 takes 1% of",
                        "the 100 point hull every two rounds.")
                .defineInRange("gun_damage_apache", 0.5D, 0.0D, 60.0D);
        ROCKET_DAMAGE_APACHE = common
                .comment("Damage a 70mm rocket does to the AH-64's airframe, flat and whatever",
                        "launcher fired it: the Apache's own rocket_damage_scale is a per launcher",
                        "price and must not change what a hit on a hull is worth.")
                .defineInRange("rocket_damage_apache", 7.0D, 0.0D, 200.0D);
        ROCKET_HEAVY_DAMAGE = common
                .comment("70mm rockets barely scratch heavy armour: they are an anti-personnel round.")
                .defineInRange("rocket_heavy_damage", 8.0D, 0.0D, 200.0D);
        ROCKET_REARM_SECONDS = common
                .comment("Seconds between rocket resupply ticks (0 disables resupply).")
                .defineInRange("rocket_rearm_seconds", 10, 0, 600);
        ROCKET_REARM_BATCH = common
                .comment("Rockets added per resupply tick, split between the two pods.")
                .defineInRange("rocket_rearm_batch", 7, 1, 14);
        LITTLE_BIRD_ROCKET_RELOAD_SECONDS = common
                .comment("Little Bird reload cooldown: seconds between pressing the reload key and",
                        "the reserve rounds appearing in the pods (0 = instant, as it used to be).")
                .defineInRange("little_bird_rocket_reload_seconds", 2, 0, 60);
        ROCKETS_BREAK_BLOCKS = common
                .comment("Whether 70mm rockets destroy terrain. Off by default.")
                .define("rockets_break_blocks", false);
        common.comment("AH-64 Apache: 19 + 19 rockets, 1 + 5 missiles and a 30mm chain gun.")
                .push("apache");
        APACHE_ROCKET_DAMAGE_SCALE = common
                .comment("Damage multiplier for the Apache's 70mm rounds. It carries 38 of them",
                        "against the Little Bird's 14, so each round is worth less.")
                .defineInRange("rocket_damage_scale", 0.6D, 0.1D, 2.0D);
        MISSILE_DIRECT_HIT_KILLS = common
                .comment("A TOW that hits an aircraft destroys it outright instead of chipping",
                        "its health: the missile takes the rotor off. Off falls back to the",
                        "ordinary missile airframe damage below.")
                .define("missile_direct_hit_kills", true);
        APACHE_GUN_DAMAGE = common
                .comment("Damage a 30mm chain gun round deals to a player or a mob.")
                .defineInRange("chain_gun_damage", 8.0D, 0.5D, 60.0D);
        APACHE_GUN_AIRFRAME_DAMAGE = common
                .comment("Damage a 30mm round deals to an airframe.")
                .defineInRange("chain_gun_airframe_damage", 3.0D, 0.0D, 60.0D);
        APACHE_GUN_HEAVY_DAMAGE = common
                .comment("The Apache's 30mm is a light cannon: sustained fire, not a tank killer.")
                .defineInRange("chain_gun_heavy_damage", 2.0D, 0.0D, 60.0D);
        APACHE_GUN_SPLASH_RADIUS = common
                .comment("Splash radius of a 30mm round, in blocks (0 disables splash).")
                .defineInRange("chain_gun_splash_radius", 1.0D, 0.0D, 6.0D);
        APACHE_MISSILE_DAMAGE = common
                .comment("Damage a TOW missile deals to the entity it directly hits.")
                .defineInRange("missile_damage", 60.0D, 0.0D, 300.0D);
        APACHE_MISSILE_AIRFRAME_DAMAGE = common
                .comment("Damage a TOW missile deals to an airframe.")
                .defineInRange("missile_airframe_damage", 20.0D, 0.0D, 300.0D);
        APACHE_MISSILE_HEAVY_DAMAGE = common
                .comment("The TOW is the pack's anti-armour answer.")
                .defineInRange("missile_heavy_damage", 45.0D, 0.0D, 400.0D);
        APACHE_MISSILE_SPLASH_RADIUS = common
                .comment("Splash radius of a TOW warhead, in blocks.")
                .defineInRange("missile_splash_radius", 2.0D, 0.0D, 12.0D);
        APACHE_ROCKET_RELOAD_SECONDS = common
                .comment("Seconds to move the rocket reserve into the ready position (the R key).")
                .defineInRange("rocket_reload_seconds", 2, 0, 60);
        APACHE_ROCKET_RESUPPLY_SECONDS = common
                .comment("Seconds between rocket reserve resupplies.")
                .defineInRange("rocket_resupply_seconds", 15, 0, 600);
        APACHE_MISSILE_RELOAD_SECONDS = common
                .comment("Seconds to load one TOW missile from the reserve onto the rail.")
                .defineInRange("missile_reload_seconds", 5, 0, 60);
        APACHE_MISSILE_RESUPPLY_SECONDS = common
                .comment("Seconds between single missile reserve resupplies.")
                .defineInRange("missile_resupply_seconds", 10, 0, 600);
        common.pop();
        common.comment("Ground vehicles: a 120mm main battle tank and a 30mm IFV. Values must",
                        "match on client and server.").push("ground");
        common.comment("M1A5 main battle tank: 120mm main gun with a five round ready rack and",
                        "a coaxial machine gun.").push("main_battle_tank");
        TANK_MAIN_GUN_DAMAGE = common
                .comment("Damage the 120mm round deals to an unarmoured target.")
                .defineInRange("main_gun_damage", 80.0D, 0.0D, 400.0D);
        TANK_MAIN_GUN_VEHICLE_DAMAGE = common
                .comment("Damage the 120mm round deals to a vehicle (airframe or ground hull).")
                .defineInRange("main_gun_vehicle_damage", 40.0D, 0.0D, 400.0D);
        TANK_MAIN_GUN_HEAVY_DAMAGE = common
                .comment("The 120mm AP round: three hits take out a tank.")
                .defineInRange("main_gun_heavy_damage", 40.0D, 0.0D, 400.0D);
        TANK_MAIN_GUN_SPLASH_RADIUS = common
                .comment("Blast radius of the 120mm round, in blocks (0 disables splash).")
                .defineInRange("main_gun_splash_radius", 3.0D, 0.0D, 12.0D);
        TANK_MAIN_GUN_FIRE_INTERVAL = common
                .comment("Ticks between 120mm rounds: the single knob for the main gun's rate of fire.",
                        "20 ticks = 1 second; 50 = 2.5 seconds. The clock starts only when a round",
                        "really leaves the barrel and keeps running while the trigger is released,",
                        "so tapping the fire key cannot beat the interval.")
                .defineInRange("main_gun_fire_interval_ticks", 50, 5, 400);
        TANK_MAIN_GUN_READY = common
                .comment("Rounds held in the ready rack.")
                .defineInRange("main_gun_ready_rounds", 5, 1, 20);
        TANK_MAIN_GUN_RESERVE = common
                .comment("Rounds held in reserve, loaded with the reload key.")
                .defineInRange("main_gun_reserve_rounds", 15, 0, 100);
        TANK_MAIN_GUN_RELOAD = common
                .comment("Reload cooldown: ticks to move a round from the reserve into the ready",
                        "rack once a reload has started.")
                .defineInRange("main_gun_reload_ticks", 60, 0, 400);
        TANK_MAIN_GUN_AUTO_RELOAD = common
                .comment("Start the reload cooldown by itself once the ready rack is not full.",
                        "Off means the reload key is the only way to load a round.")
                .define("main_gun_auto_reload", true);
        TANK_MAIN_GUN_RESUPPLY_TICKS = common
                .comment("Resupply cooldown: ticks between reserve rounds coming back on their own",
                        "(0 disables it; the ammunition crate still tops the rack up in bulk).")
                .defineInRange("main_gun_resupply_ticks", 100, 0, 2400);
        TANK_MAIN_GUN_RESUPPLY_BATCH = common
                .comment("Rounds the resupply cooldown returns each time it fires.")
                .defineInRange("main_gun_resupply_batch", 1, 1, 10);
        // High explosive round. The armour-piercing keys above stay the AP rack's numbers, so a
        // server can tune the two shell types apart without touching this mod's defaults.
        TANK_MAIN_GUN_HE_DAMAGE = common
                .comment("Damage the 120mm HE round deals to an unarmoured target.")
                .defineInRange("main_gun_he_damage", 80.0D, 0.0D, 400.0D);
        TANK_MAIN_GUN_HE_VEHICLE_DAMAGE = common
                .comment("Damage the 120mm HE round deals to a vehicle (less than AP).")
                .defineInRange("main_gun_he_vehicle_damage", 25.0D, 0.0D, 400.0D);
        TANK_MAIN_GUN_HE_HEAVY_DAMAGE = common
                .comment("HE is the worse anti-armour round, as it should be.")
                .defineInRange("main_gun_he_heavy_damage", 30.0D, 0.0D, 400.0D);
        TANK_MAIN_GUN_HE_SPLASH_RADIUS = common
                .comment("Blast radius of the 120mm HE round, in blocks (larger than AP).")
                .defineInRange("main_gun_he_splash_radius", 4.0D, 0.0D, 12.0D);
        // Canister round: a shotgun load of pellets instead of one shell. No splash; the damage
        // is all in the pellets that hit.
        TANK_MAIN_GUN_CANISTER_PELLETS = common
                .comment("Pellets fired by one 120mm canister round.")
                .defineInRange("main_gun_canister_pellets", 40, 1, 200);
        TANK_MAIN_GUN_CANISTER_PELLET_DAMAGE = common
                .comment("Damage each canister pellet deals to an unarmoured target.")
                .defineInRange("main_gun_canister_pellet_damage", 40.0D, 0.0D, 100.0D);
        TANK_MAIN_GUN_CANISTER_PELLET_VEHICLE_DAMAGE = common
                .comment("Damage each canister pellet deals to a vehicle hull.")
                .defineInRange("main_gun_canister_pellet_vehicle_damage", 40.0D, 0.0D, 100.0D);
        TANK_MAIN_GUN_CANISTER_SPREAD_HORIZONTAL_DEGREES = common
                .comment("Half-angle of the canister spread along the crosshair's horizontal line,",
                        "in degrees. No pellet goes further than this to either side.")
                .defineInRange("main_gun_canister_spread_horizontal_degrees", 5.0D, 0.0D, 45.0D);
        TANK_MAIN_GUN_CANISTER_SPREAD_VERTICAL_DEGREES = common
                .comment("Half-angle of the canister spread above and below the crosshair's",
                        "horizontal line, in degrees. No pellet goes further than this up or down.")
                .defineInRange("main_gun_canister_spread_vertical_degrees", 2.0D, 0.0D, 45.0D);
        TANK_MAIN_GUN_CANISTER_RANGE = common
                .comment("Maximum distance one canister pellet can travel, in blocks.")
                .defineInRange("main_gun_canister_range", 40.0D, 1.0D, 128.0D);
        TANK_MAIN_GUN_BREAKS_BLOCKS = common
                .comment("Whether the 120mm shell destroys blocks where it lands.")
                .define("main_gun_breaks_blocks", true);
        TANK_MAIN_GUN_PARTICLE_SCALE = common
                .comment("Scale on the 120mm muzzle flash, fireball and impact dust counts.")
                .defineInRange("main_gun_particle_scale", 1.0D, 0.2D, 4.0D);
        TANK_COAX_DAMAGE = common
                .comment("Damage the coaxial machine gun deals to an unarmoured target.")
                .defineInRange("coax_damage", 8.0D, 0.5D, 60.0D);
        TANK_COAX_VEHICLE_DAMAGE = common
                .comment("Damage the coaxial machine gun deals to a vehicle.")
                .defineInRange("coax_vehicle_damage", 3.0D, 0.0D, 60.0D);
        TANK_COAX_CAPACITY = common
                .comment("Driver's coaxial machine gun belt capacity.")
                .defineInRange("coax_capacity", 300, 10, 4000);
        TANK_COAX_FIRE_INTERVAL = common
                .comment("Ticks between the driver's coaxial rounds.")
                .defineInRange("coax_fire_interval_ticks", 2, 1, 40);
        TANK_CUPOLA_CAPACITY = common
                .comment("Seat two's cupola machine gun belt capacity. The two seats have their",
                        "own belts, so one does not drain the other.")
                .defineInRange("cupola_capacity", 200, 10, 4000);
        TANK_CUPOLA_FIRE_INTERVAL = common
                .comment("Ticks between seat two's cupola rounds.")
                .defineInRange("cupola_fire_interval_ticks", 3, 1, 40);
        // Placeholder balance for the smoke discharger: the BF2042 numbers are not published, so
        // these are a starting point to be confirmed rather than a claim of parity.
        TANK_SMOKE_CHARGES = common
                .comment("Smoke charges carried before the launcher needs a crate refill.")
                .defineInRange("smoke_charges", 2, 1, 12);
        TANK_SMOKE_COOLDOWN_SECONDS = common
                .comment("Seconds between smoke charges.")
                .defineInRange("smoke_cooldown_seconds", 20, 0, 300);
        TANK_SMOKE_DURATION_SECONDS = common
                .comment("Seconds a smoke cloud lasts.")
                .defineInRange("smoke_duration_seconds", 10, 1, 120);
        TANK_SMOKE_RADIUS = common
                .comment("Radius of the smoke cloud in blocks.")
                .defineInRange("smoke_radius", 4.0D, 1.0D, 16.0D);
        common.pop();
        common.comment("EBAA Wildcat IFV: a twin 30mm autocannon in the turret and two ATGMs on",
                        "the rail.").push("infantry_fighting_vehicle");
        IFV_AUTOCANNON_DAMAGE = common
                .comment("Damage one 30mm round deals to an unarmoured target.")
                .defineInRange("autocannon_damage", 8.0D, 0.5D, 60.0D);
        IFV_AUTOCANNON_VEHICLE_DAMAGE = common
                .comment("Damage one 30mm round deals to a vehicle. Low on purpose: an IFV",
                        "cannot chew through a tank with its cannon, it needs the missile.")
                .defineInRange("autocannon_vehicle_damage", 3.0D, 0.0D, 60.0D);
        BRADLEY_AUTOCANNON_HEAVY_DAMAGE = common
                .comment("The Bradley's 25mm M242: three hits on a tank, forty on the belt.")
                .defineInRange("autocannon_heavy_damage", 3.0D, 0.0D, 60.0D);
        IFV_AUTOCANNON_HEAVY_DAMAGE = common
                .comment("The Wildcat's twin 30mm is anti-air and anti-infantry; a tank needs its whole belt.")
                .defineInRange("autocannon_heavy_damage", 1.0D, 0.0D, 60.0D);
        IFV_AUTOCANNON_CAPACITY = common
                .comment("Twin 30mm belt capacity.")
                .defineInRange("autocannon_capacity", 600, 10, 4000);
        IFV_ATGM_DAMAGE = common
                .comment("Damage an ATGM deals to an unarmoured target.")
                .defineInRange("atgm_damage", 60.0D, 0.0D, 300.0D);
        IFV_ATGM_VEHICLE_DAMAGE = common
                .comment("Damage an ATGM deals to a vehicle.")
                .defineInRange("atgm_vehicle_damage", 20.0D, 0.0D, 300.0D);
        IFV_ATGM_HEAVY_DAMAGE = common
                .comment("The IFV's anti-tank missile, priced with the TOW.")
                .defineInRange("atgm_heavy_damage", 45.0D, 0.0D, 400.0D);
        IFV_ATGM_SPLASH_RADIUS = common
                .comment("Blast radius of an ATGM warhead, in blocks.")
                .defineInRange("atgm_splash_radius", 2.0D, 0.0D, 12.0D);
        IFV_ATGM_READY = common
                .comment("ATGMs ready on the rail.")
                .defineInRange("atgm_ready_rounds", 2, 1, 8);
        IFV_ATGM_RESERVE = common
                .comment("ATGMs held in reserve.")
                .defineInRange("atgm_reserve_rounds", 6, 0, 40);
        IFV_ATGM_RELOAD = common
                .comment("Ticks to load one ATGM from the reserve onto the rail.")
                .defineInRange("atgm_reload_ticks", 100, 0, 600);
        IFV_ATGM_FIRE_INTERVAL = common
                .comment("Minimum ticks between ATGM launches.")
                .defineInRange("atgm_fire_interval_ticks", 20, 1, 200);
        common.pop();
        common.comment("M2A2 Bradley: a 25mm M242 chain gun fed from a 300 round ready box over",
                        "a 600 round reserve, a twin TOW rail that fires the pack's guided",
                        "missile, and a smoke wall laid ahead of the nose.")
                .push("bradley");
        BRADLEY_AUTOCANNON_ROUNDS_PER_MINUTE = common
                .comment("25mm rate of fire in rounds per minute. Twenty ticks a second, so 180 is",
                        "one round every six and two thirds; the cadence is carried as a fraction",
                        "rather than rounded to a whole tick, so the gun really does fire 180 and",
                        "not 171. The turret refuses to fire faster than one round every two",
                        "ticks, which is the 600 ceiling here.")
                .defineInRange("autocannon_rounds_per_minute", 180, 30, 600);
        BRADLEY_AUTOCANNON_SPLASH_RADIUS = common
                .comment("Blast radius of one 25mm shell, in blocks.")
                .defineInRange("autocannon_splash_radius", 2.0D, 0.0D, 8.0D);
        BRADLEY_AUTOCANNON_BREAKS_BLOCKS = common
                .comment("Whether a 25mm shell breaks terrain. Off by default: at five rounds a",
                        "second an autocannon that craters the map is unusable.")
                .define("autocannon_breaks_blocks", false);
        BRADLEY_AUTOCANNON_SHELL_SPEED = common
                .comment("Muzzle speed of the 25mm shell, in blocks per tick. 6.0 is a flat,",
                        "hard round that crosses a hundred blocks in under three blocks of",
                        "drop; 2.0 is a lobbed tracer arc. The round is watched across the",
                        "whole flight, so this is both the feel and the effective range",
                        "(speed x lifetime ticks, 360 blocks at the default).")
                .defineInRange("autocannon_shell_speed", 6.0D, 0.5D, 12.0D);
        BRADLEY_AUTOCANNON_SHELL_LIFETIME = common
                .comment("Ticks a 25mm shell flies before it gives up and despawns.")
                .defineInRange("autocannon_shell_lifetime_ticks", 60, 5, 400);
        BRADLEY_AUTOCANNON_DAMAGE = common
                .comment("Damage one 25mm round deals to an unarmoured target.")
                .defineInRange("autocannon_damage", 6.0D, 0.5D, 60.0D);
        BRADLEY_AUTOCANNON_VEHICLE_DAMAGE = common
                .comment("Damage one 25mm round deals to a vehicle. The Bradley wins the",
                        "infantry fight with the chain gun and the armour fight with the TOW.")
                .defineInRange("autocannon_vehicle_damage", 4.0D, 0.0D, 60.0D);
        BRADLEY_AUTOCANNON_READY = common
                .comment("25mm rounds in the ready box. The real vehicle carries 300 ready.")
                .defineInRange("autocannon_ready_rounds", 300, 1, 2000);
        BRADLEY_AUTOCANNON_RESERVE = common
                .comment("25mm rounds held in reserve. The real vehicle carries 600 stowed.")
                .defineInRange("autocannon_reserve_rounds", 600, 0, 4000);
        BRADLEY_AUTOCANNON_RELOAD = common
                .comment("Ticks to top the ready box up from the reserve.")
                .defineInRange("autocannon_reload_ticks", 80, 0, 600);
        BRADLEY_TOW_DAMAGE = common
                .comment("Damage a TOW deals to an unarmoured target.")
                .defineInRange("tow_damage", 90.0D, 0.0D, 400.0D);
        BRADLEY_TOW_VEHICLE_DAMAGE = common
                .comment("Damage a TOW deals to a vehicle. The heavy punch of the hull.")
                .defineInRange("tow_vehicle_damage", 40.0D, 0.0D, 400.0D);
        BRADLEY_TOW_HEAVY_DAMAGE = common
                .comment("The Bradley's TOW, same warhead class as the Apache's.")
                .defineInRange("tow_heavy_damage", 45.0D, 0.0D, 400.0D);
        BRADLEY_TOW_SPLASH_RADIUS = common
                .comment("Blast radius of the TOW warhead, in blocks.")
                .defineInRange("tow_splash_radius", 2.0D, 0.0D, 12.0D);
        BRADLEY_TOW_READY = common
                .comment("TOWs ready on the launcher.")
                .defineInRange("tow_ready_rounds", 2, 1, 8);
        BRADLEY_TOW_RESERVE = common
                .comment("TOWs held in reserve.")
                .defineInRange("tow_reserve_rounds", 5, 0, 40);
        BRADLEY_TOW_RELOAD = common
                .comment("Ticks to load one TOW from the reserve onto the launcher.")
                .defineInRange("tow_reload_ticks", 120, 0, 600);
        BRADLEY_TOW_FIRE_INTERVAL = common
                .comment("Minimum ticks between TOW launches.")
                .defineInRange("tow_fire_interval_ticks", 40, 1, 200);
        BRADLEY_TOW_LAUNCHER_LATERAL = common
                .comment("Lateral offset of the TOW launcher from the turret centre line, in",
                        "blocks. Positive is the vehicle's left, where the real launcher sits.")
                .defineInRange("tow_launcher_lateral_blocks", 0.55D, 0.0D, 3.0D);
        BRADLEY_M2_DAMAGE = common
                .comment("Damage one .50 round deals to an unarmoured target.")
                .defineInRange("m2_damage", 6.0D, 0.5D, 60.0D);
        BRADLEY_M2_VEHICLE_DAMAGE = common
                .comment("Damage one .50 round deals to a vehicle.")
                .defineInRange("m2_vehicle_damage", 1.5D, 0.0D, 60.0D);
        BRADLEY_M2_CAPACITY = common
                .comment("Roof .50 belt capacity.")
                .defineInRange("m2_capacity", 400, 10, 4000);
        BRADLEY_M2_FIRE_INTERVAL = common
                .comment("Ticks between roof machine gun rounds.")
                .defineInRange("m2_fire_interval_ticks", 3, 1, 40);
        BRADLEY_M2_SPREAD = common
                .comment("Dispersion of the roof machine gun, in degrees.")
                .defineInRange("m2_spread_degrees", 0.6D, 0.0D, 6.0D);
        BRADLEY_SMOKE_CANISTERS = common
                .comment("Charges in one wall. They are laid abreast on the hull's centre line.")
                .defineInRange("smoke_canisters", 6, 1, 16);
        BRADLEY_SMOKE_SPACING = common
                .comment("Gap between adjacent charges in the wall, in blocks.")
                .defineInRange("smoke_spacing_blocks", 1.3D, 0.1D, 8.0D);
        BRADLEY_SMOKE_AHEAD = common
                .comment("How far in front of the hull's nose the wall is laid, in blocks.")
                .defineInRange("smoke_ahead_of_nose_blocks", 5.0D, 0.5D, 24.0D);
        BRADLEY_SMOKE_COOLDOWN = common
                .comment("Ticks between smoke walls.")
                .defineInRange("smoke_cooldown_ticks", 200, 0, 2400);
        BRADLEY_SMOKE_DURATION = common
                .comment("How long one wall's cloud lasts, in ticks.")
                .defineInRange("smoke_duration_ticks", 240, 1, 2400);
        BRADLEY_SMOKE_CHARGES = common
                .comment("Walls per load of the discharger.")
                .defineInRange("smoke_charges", 2, 1, 12);
        common.pop();
        common.pop();
        common.pop();
        common.comment("B-2 Spirit ordnance: the JDAM and the incendiary canister. Values must",
                        "match on client and server.").push("b2");
        JDAM_DAMAGE = common
                .comment("Damage a JDAM deals to the entity it directly hits.")
                .defineInRange("jdam_damage", 120.0D, 0.0D, 400.0D);
        INCENDIARY_DAMAGE = common
                .comment("Damage an incendiary canister deals to the entity it directly hits.")
                .defineInRange("incendiary_damage", 45.0D, 0.0D, 400.0D);
        JDAM_BLAST_RADIUS = common
                .comment("Splash radius of a JDAM warhead, in blocks.")
                .defineInRange("jdam_blast_radius", 6.0D, 0.0D, 24.0D);
        INCENDIARY_BLAST_RADIUS = common
                .comment("Splash radius of an incendiary canister, in blocks. Also the radius of",
                        "the burning ground it leaves behind.")
                .defineInRange("incendiary_blast_radius", 4.0D, 0.0D, 24.0D);
        INCENDIARY_FIRE_TICKS = common
                .comment("how long the impact area burns, in ticks (0 disables the fire).")
                .defineInRange("incendiary_fire_ticks", 600, 0, 24000);
        BOMB_BAY_CAPACITY_JDAM = common
                .comment("JDAM stores in a full B-2 bay.")
                .defineInRange("bomb_bay_capacity_jdam", 16, 0, 64);
        BOMB_BAY_CAPACITY_INCENDIARY = common
                .comment("Incendiary canisters in a full B-2 bay.")
                .defineInRange("bomb_bay_capacity_incendiary", 24, 0, 64);
        common.pop();
        COMMON_SPEC = common.build();
    }

    private LittleBirdConfig() {
    }

    /** Flight constants for the pilot's own client, from the CLIENT config. */
    public static FlightConfig clientFlight() {
        return FlightConfig.builder()
                .maxForwardSpeed(MAX_FORWARD_SPEED.get())
                .maxYawRateDegPerTick(YAW_RATE.get())
                .pitchRateDegPerTick(PITCH_RATE.get())
                .pitchDeadzoneDeg(PITCH_DEADZONE.get())
                .pitchMaxDeg(PITCH_MAX.get())
                .pitchDiveCoupling(PITCH_DIVE.get())
                .rollRateDegPerTick(ROLL_RATE.get())
                .rollSpinUpTicks(ROLL_SPIN_UP.get())
                .pitchCenterRateDegPerTick(AUTO_LEVEL.get() ? 3.0D : 0.0D)
                .rollLevelRateDegPerTick(AUTO_LEVEL.get() ? 4.5D : 0.0D)
                .bankedTurnDegPerTick(BANKED_TURN.get())
                .dampingYaw(DAMPING_YAW.get())
                .dampingPitch(DAMPING_PITCH.get())
                .dampingRoll(DAMPING_ROLL.get())
                .dampingCollective(DAMPING_COLLECTIVE.get())
                .build();
    }

    public static float cameraRollFactor() {
        return CAMERA_ROLL_FACTOR.get().floatValue();
    }

    public static double thirdPersonFovMultiplier() {
        return THIRD_PERSON_FOV.get();
    }

    public static double cockpitFovMultiplier() {
        return COCKPIT_FOV.get();
    }



    public static double apacheThirdPersonDistance() {
        return APACHE_THIRD_PERSON_DISTANCE.get();
    }

    public static double jetThirdPersonDistance() {
        return JET_THIRD_PERSON_DISTANCE.get();
    }

    /** Exterior camera distance for the B-2 flying wing, in blocks. */
    public static double b2ThirdPersonDistance() {
        return B2_THIRD_PERSON_DISTANCE.get();
    }

    /** Exterior camera field of view multiplier for the B-2. */
    public static double b2ThirdPersonFovMultiplier() {
        return B2_THIRD_PERSON_FOV.get();
    }

    public static int jetFlareCount() {
        return JET_FLARE_COUNT.get();
    }

    public static double jetLockRangeBlocks() {
        return JET_LOCK_RANGE.get();
    }

    public static boolean jetInvertPitch() {
        return JET_INVERT_PITCH.get();
    }

    /** Flips the pitch-inversion setting and writes it back to the config file. */
    public static boolean toggleJetInvertPitch() {
        boolean inverted = !JET_INVERT_PITCH.get();
        JET_INVERT_PITCH.set(inverted);
        CLIENT_SPEC.save();
        return inverted;
    }

    /** True when the mouse yaws and A/D rolls instead of the other way round. */
    public static boolean jetMouseYaw() {
        return JET_MOUSE_YAW.get();
    }

    /** Flips the mouse-yaw setting and writes it back to the config file. */
    public static boolean toggleJetMouseYaw() {
        boolean mouseYaw = !JET_MOUSE_YAW.get();
        JET_MOUSE_YAW.set(mouseYaw);
        CLIENT_SPEC.save();
        return mouseYaw;
    }

    public static double cockpitEyeLiftBlocks() {
        return COCKPIT_EYE_LIFT.get();
    }

    public static boolean showGunReticle() {
        return SHOW_GUN_RETICLE.get();
    }

    public static boolean showCenterCrosshair() {
        return SHOW_CENTER_CROSSHAIR.get();
    }

    public static boolean shoulderView() {
        return SHOULDER_VIEW.get();
    }

    public static double shoulderOffsetBlocks() {
        return SHOULDER_OFFSET.get();
    }

    public static double shoulderHeightBlocks() {
        return SHOULDER_HEIGHT.get();
    }

    public static boolean reticleFollowsWeapon() {
        return RETICLE_FOLLOWS_WEAPON.get();
    }

    public static boolean showRocketReticle() {
        return SHOW_ROCKET_RETICLE.get();
    }

    public static int hudScale() {
        return HUD_SCALE.get();
    }

    /** Ticks the guided missile flies straight before its guidance takes over. */
    public static int towLaunchTicks() {
        return TOW_LAUNCH_TICKS.get();
    }

    /** How hard the guided missile may turn towards the aim point. */
    public static double towTurnRateDegreesPerTick() {
        return TOW_TURN_RATE.get();
    }

    /** How far along the sight line a guided missile looks for its aim point. */
    public static double towSightRangeBlocks() {
        return TOW_SIGHT_RANGE.get();
    }

    public static float gunDamage() {
        return GUN_DAMAGE.get().floatValue();
    }

    public static float gunDamageAircraft() {
        return GUN_DAMAGE_AIRCRAFT.get().floatValue();
    }

    public static float rocketDamageAircraft() {
        return ROCKET_DAMAGE_AIRCRAFT.get().floatValue();
    }

    public static float gunDamageApache() {
        return GUN_DAMAGE_APACHE.get().floatValue();
    }

    public static float rocketDamageApache() {
        return ROCKET_DAMAGE_APACHE.get().floatValue();
    }

    public static float rocketDirectHitDamage() {
        return ROCKET_DIRECT_DAMAGE.get().floatValue();
    }

    public static int rocketRearmTicks() {
        return ROCKET_REARM_SECONDS.get() * 20;
    }

    public static int rocketRearmBatch() {
        return ROCKET_REARM_BATCH.get();
    }

    /** Reload cooldown for the Little Bird's rocket pods, in ticks. */
    public static int littleBirdRocketReloadTicks() {
        return LITTLE_BIRD_ROCKET_RELOAD_SECONDS.get() * 20;
    }

    public static int littleBirdRocketReloadSeconds() {
        return LITTLE_BIRD_ROCKET_RELOAD_SECONDS.get();
    }

    public static boolean rocketsBreakBlocks() {
        return ROCKETS_BREAK_BLOCKS.get();
    }

    // ------------------------------------------------------------------
    // AH-64 Apache balance
    // ------------------------------------------------------------------

    public static float apacheRocketDamageScale() {
        return APACHE_ROCKET_DAMAGE_SCALE.get().floatValue();
    }

    public static float apacheChainGunDamage() {
        return APACHE_GUN_DAMAGE.get().floatValue();
    }

    public static float apacheChainGunAirframeDamage() {
        return APACHE_GUN_AIRFRAME_DAMAGE.get().floatValue();
    }

    public static double apacheChainGunSplashRadius() {
        return APACHE_GUN_SPLASH_RADIUS.get();
    }

    public static float apacheMissileDamage() {
        return APACHE_MISSILE_DAMAGE.get().floatValue();
    }

    public static float apacheMissileAirframeDamage() {
        return APACHE_MISSILE_AIRFRAME_DAMAGE.get().floatValue();
    }

    /** An ammunition crate restocking rates, for AircraftRearm. */
    public static io.github.littlebird.entity.weapon.AmmoSupply ammoSupply() {
        return new io.github.littlebird.entity.weapon.AmmoSupply(
                AMMO_CRATE_BULLETS_PER_SECOND.get(),
                AMMO_CRATE_ROCKETS_PER_SECOND.get(),
                AMMO_CRATE_MISSILES_PER_SECOND.get());
    }

    /** True when a direct TOW hit is meant to destroy the aircraft it hits. */
    public static boolean apacheMissileDirectHitKills() {
        return MISSILE_DIRECT_HIT_KILLS.get();
    }

    /** How far the TOW trail is broadcast, in blocks (never below 64). */
    public static double apacheMissileParticleRange() {
        return MISSILE_PARTICLE_RANGE.get();
    }

    public static float apacheMissileSplashRadius() {
        return APACHE_MISSILE_SPLASH_RADIUS.get().floatValue();
    }

    // ------------------------------------------------------------------
    // B-2 Spirit: ordnance (COMMON) and pod optics (CLIENT).
    // ------------------------------------------------------------------
    /** JDAM stores in a full bay. */
    public static int bombBayCapacityJdam() {
        return BOMB_BAY_CAPACITY_JDAM.get();
    }

    /** Incendiary canisters in a full bay. */
    public static int bombBayCapacityIncendiary() {
        return BOMB_BAY_CAPACITY_INCENDIARY.get();
    }

    public static double jdamDamage() {
        return JDAM_DAMAGE.get();
    }

    public static double incendiaryDamage() {
        return INCENDIARY_DAMAGE.get();
    }

    public static double jdamBlastRadius() {
        return JDAM_BLAST_RADIUS.get();
    }

    public static double incendiaryBlastRadius() {
        return INCENDIARY_BLAST_RADIUS.get();
    }

    /** Ticks the incendiary impact area burns; 0 disables the fire. */
    public static int incendiaryFireTicks() {
        return INCENDIARY_FIRE_TICKS.get();
    }

    /** Tightest pod field of view multiplier. */
    /** How far off the nose the sight may swing, in degrees. */
    public static double podGimbalYawLimit() {
        return POD_GIMBAL_YAW_LIMIT.get();
    }

    /** Tightest sight field of view, in degrees. */
    public static double podZoomTightFov() {
        return POD_ZOOM_TIGHT_FOV.get();
    }

    /** Widest sight field of view, in degrees. */
    public static double podZoomWideFov() {
        return POD_ZOOM_WIDE_FOV.get();
    }

    /** How far down from the horizon the sight may tilt, in degrees. */
    public static double podSightDownDeg() {
        return POD_SIGHT_DOWN_DEG.get();
    }

    /** How far up from the horizon the sight may tilt, in degrees. */
    public static double podSightUpDeg() {
        return POD_SIGHT_UP_DEG.get();
    }

    /** True when the cockpit camera is locked rigidly to the airframe. */
    public static boolean cockpitRigidView() {
        return COCKPIT_RIGID_VIEW.get();
    }

    public static int apacheRocketReloadTicks() {
        return APACHE_ROCKET_RELOAD_SECONDS.get() * 20;
    }

    public static int apacheRocketResupplyTicks() {
        return APACHE_ROCKET_RESUPPLY_SECONDS.get() * 20;
    }

    public static int apacheMissileReloadTicks() {
        return APACHE_MISSILE_RELOAD_SECONDS.get() * 20;
    }

    public static int apacheMissileResupplyTicks() {
        return APACHE_MISSILE_RESUPPLY_SECONDS.get() * 20;
    }

    // ------------------------------------------------------------------
    // Ground vehicles
    // ------------------------------------------------------------------

    /** Handling of the main battle tank, from the CLIENT ground section. */
    public static GroundDrive.Spec tankDriveSpec() {
        return new GroundDrive.Spec(TANK_MAX_SPEED.get(), TANK_ACCELERATION.get(),
                TANK_BRAKING.get(), TANK_TURN_RATE.get(), TANK_TURN_STANDSTILL.get(),
                TANK_MAX_GRADE.get());
    }

    /** Handling of the infantry fighting vehicle, from the CLIENT ground section. */
    public static GroundDrive.Spec ifvDriveSpec() {
        return new GroundDrive.Spec(IFV_MAX_SPEED.get(), IFV_ACCELERATION.get(),
                IFV_BRAKING.get(), IFV_TURN_RATE.get(), IFV_TURN_STANDSTILL.get(),
                IFV_MAX_GRADE.get());
    }

    /** Tallest lip the tank climbs, in blocks; consumed by GroundMover. */
    public static double tankStepHeightBlocks() {
        return TANK_STEP_HEIGHT.get();
    }

    /** Tallest lip the IFV climbs, in blocks; consumed by GroundMover. */
    public static double ifvStepHeightBlocks() {
        return IFV_STEP_HEIGHT.get();
    }

    /**
     * The two 120mm shell types are priced separately. The armour-piercing keys keep their original
     * names, so an existing config file keeps working; HE has its own {@code main_gun_he_*} keys.
     */
    public static float tankMainGunDamage(
            io.github.littlebird.entity.weapon.MainGunAmmoType type) {
        return switch (type) {
            case HE -> TANK_MAIN_GUN_HE_DAMAGE.get().floatValue();
            case CANISTER -> TANK_MAIN_GUN_CANISTER_PELLET_DAMAGE.get().floatValue();
            default -> TANK_MAIN_GUN_DAMAGE.get().floatValue();
        };
    }

    public static float tankMainGunVehicleDamage(
            io.github.littlebird.entity.weapon.MainGunAmmoType type) {
        return switch (type) {
            case HE -> TANK_MAIN_GUN_HE_VEHICLE_DAMAGE.get().floatValue();
            case CANISTER -> TANK_MAIN_GUN_CANISTER_PELLET_VEHICLE_DAMAGE.get().floatValue();
            default -> TANK_MAIN_GUN_VEHICLE_DAMAGE.get().floatValue();
        };
    }

    public static double tankMainGunSplashRadius(
            io.github.littlebird.entity.weapon.MainGunAmmoType type) {
        return switch (type) {
            case HE -> TANK_MAIN_GUN_HE_SPLASH_RADIUS.get();
            case CANISTER -> 0.0D;
            default -> TANK_MAIN_GUN_SPLASH_RADIUS.get();
        };
    }

    /** Pellets one 120mm canister round fires. */
    public static int tankCanisterPellets() {
        return TANK_MAIN_GUN_CANISTER_PELLETS.get();
    }

    public static float tankCanisterPelletDamage() {
        return TANK_MAIN_GUN_CANISTER_PELLET_DAMAGE.get().floatValue();
    }

    public static float tankCanisterPelletVehicleDamage() {
        return TANK_MAIN_GUN_CANISTER_PELLET_VEHICLE_DAMAGE.get().floatValue();
    }

    /** Half-angle of the canister spread along the crosshair's horizontal line, in degrees. */
    public static double tankCanisterSpreadHorizontalDegrees() {
        return TANK_MAIN_GUN_CANISTER_SPREAD_HORIZONTAL_DEGREES.get();
    }

    /** Half-angle of the canister spread above and below the crosshair line, in degrees. */
    public static double tankCanisterSpreadVerticalDegrees() {
        return TANK_MAIN_GUN_CANISTER_SPREAD_VERTICAL_DEGREES.get();
    }

    /** Range of one canister pellet, in blocks. */
    public static double tankCanisterRange() {
        return TANK_MAIN_GUN_CANISTER_RANGE.get();
    }

    /** The 120mm shell destroys terrain by default; a server can turn that off. */
    public static boolean mainGunBreaksBlocks() {
        return TANK_MAIN_GUN_BREAKS_BLOCKS.get();
    }

    /** Scale on the 120mm muzzle flash, fireball and impact dust particle counts. */
    public static double mainGunParticleScale() {
        return TANK_MAIN_GUN_PARTICLE_SCALE.get();
    }

    /** Field of view multiplier for a ground vehicle's magnifying gun sight. */
    public static double groundSightFovMultiplier() {
        return GROUND_SIGHT_FOV.get();
    }

    /** Length of the rigid tracer streak every shell draws behind it, in blocks. */
    public static double shellTracerLengthBlocks() {
        return MAIN_GUN_TRACER_LENGTH.get();
    }

    /** Scale on the camera shock a 120mm impact gives the viewer. */
    public static double mainGunImpactShake() {
        return MAIN_GUN_IMPACT_SHAKE.get();
    }

    /** How far away a flying 120mm round is still drawn, in blocks. */
    public static double mainGunShellVisibleDistanceBlocks() {
        return MAIN_GUN_SHELL_VISIBLE_DISTANCE.get();
    }

    /** Minimum on-screen thickness of a shell tracer streak, in framebuffer pixels. */
    public static double shellTracerMinPixelWidth() {
        return MAIN_GUN_TRACER_MIN_PIXEL_WIDTH.get();
    }

    /** Minimum on-screen length of a shell tracer streak, in framebuffer pixels. */
    public static double shellTracerMinPixelLength() {
        return MAIN_GUN_TRACER_MIN_PIXEL_LENGTH.get();
    }

    /** How far away a 120mm impact is still reported to a client, in blocks. */
    public static double mainGunImpactVisibleDistanceBlocks() {
        return MAIN_GUN_IMPACT_VISIBLE_DISTANCE.get();
    }

    /** How far away a flying 25mm autocannon round is still drawn, in blocks. */
    public static double autocannonShellVisibleDistanceBlocks() {
        return AUTOCANNON_SHELL_VISIBLE_DISTANCE.get();
    }

    /** How far away a 25mm impact is still reported to a client, in blocks. */
    public static double autocannonImpactVisibleDistanceBlocks() {
        return AUTOCANNON_IMPACT_VISIBLE_DISTANCE.get();
    }

    /** 120mm cadence in ticks: the minimum time between two rounds that really left the barrel. */
    public static int tankMainGunFireIntervalTicks() {
        return TANK_MAIN_GUN_FIRE_INTERVAL.get();
    }

    /** The 120mm ready rack and its reserve, loaded with the reload key. */
    public static io.github.littlebird.entity.weapon.TwoStageMagazine.Spec tankMainGunMagazine() {
        // Both cooldowns the 120mm obeys: main_gun_reload_ticks per reloaded round, and the slower
        // main_gun_resupply_ticks clock that drips reserve rounds back until the crate tops it up.
        return new io.github.littlebird.entity.weapon.TwoStageMagazine.Spec(
                TANK_MAIN_GUN_READY.get(), TANK_MAIN_GUN_RESERVE.get(),
                TANK_MAIN_GUN_RELOAD.get(), TANK_MAIN_GUN_RESUPPLY_TICKS.get(),
                TANK_MAIN_GUN_RESUPPLY_BATCH.get());
    }

    /** Whether the main gun starts its own reload cooldown when the ready rack is not full. */
    public static boolean tankMainGunAutoReload() {
        return TANK_MAIN_GUN_AUTO_RELOAD.get();
    }

    public static float tankCoaxDamage() {
        return TANK_COAX_DAMAGE.get().floatValue();
    }

    public static float tankCoaxVehicleDamage() {
        return TANK_COAX_VEHICLE_DAMAGE.get().floatValue();
    }

    public static int tankCoaxCapacity() {
        return TANK_COAX_CAPACITY.get();
    }

    public static int tankCoaxFireIntervalTicks() {
        return TANK_COAX_FIRE_INTERVAL.get();
    }

    /** Seat two's cupola machine gun belt capacity. */
    public static int tankCupolaCapacity() {
        return TANK_CUPOLA_CAPACITY.get();
    }

    /** Ticks between seat two's cupola rounds. */
    public static int tankCupolaFireIntervalTicks() {
        return TANK_CUPOLA_FIRE_INTERVAL.get();
    }

    /** Smoke discharger balance: charges, cooldown and cloud duration (placeholder values). */
    public static io.github.littlebird.entity.weapon.SmokeLauncher.Spec tankSmokeSpec() {
        return new io.github.littlebird.entity.weapon.SmokeLauncher.Spec(
                TANK_SMOKE_CHARGES.get(), TANK_SMOKE_COOLDOWN_SECONDS.get() * 20,
                TANK_SMOKE_DURATION_SECONDS.get() * 20);
    }

    /** Radius of the smoke cloud, in blocks. */
    public static double tankSmokeRadiusBlocks() {
        return TANK_SMOKE_RADIUS.get();
    }

    public static float ifvAutocannonDamage() {
        return IFV_AUTOCANNON_DAMAGE.get().floatValue();
    }

    public static float ifvAutocannonVehicleDamage() {
        return IFV_AUTOCANNON_VEHICLE_DAMAGE.get().floatValue();
    }

    public static int ifvAutocannonCapacity() {
        return IFV_AUTOCANNON_CAPACITY.get();
    }

    public static float ifvAtgmDamage() {
        return IFV_ATGM_DAMAGE.get().floatValue();
    }

    public static float ifvAtgmVehicleDamage() {
        return IFV_ATGM_VEHICLE_DAMAGE.get().floatValue();
    }

    public static double ifvAtgmSplashRadius() {
        return IFV_ATGM_SPLASH_RADIUS.get();
    }

    public static int ifvAtgmFireIntervalTicks() {
        return IFV_ATGM_FIRE_INTERVAL.get();
    }

    /** The ATGM rail and its reserve, loaded with the reload key. */
    public static io.github.littlebird.entity.weapon.TwoStageMagazine.Spec ifvAtgmMagazine() {
        return new io.github.littlebird.entity.weapon.TwoStageMagazine.Spec(
                IFV_ATGM_READY.get(), IFV_ATGM_RESERVE.get(), IFV_ATGM_RELOAD.get(), 0, 1);
    }
    /** Handling of the Bradley, from the CLIENT ground section. */
    public static GroundDrive.Spec bradleyDriveSpec() {
        return new GroundDrive.Spec(BRADLEY_MAX_SPEED.get(), BRADLEY_ACCELERATION.get(),
                BRADLEY_BRAKING.get(), BRADLEY_TURN_RATE.get(), BRADLEY_TURN_STANDSTILL.get(),
                BRADLEY_MAX_GRADE.get());
    }

    /** Tallest lip the Bradley climbs, in blocks; consumed by GroundMover. */
    public static double bradleyStepHeightBlocks() {
        return BRADLEY_STEP_HEIGHT.get();
    }

    public static float bradleyAutocannonDamage() {
        return BRADLEY_AUTOCANNON_DAMAGE.get().floatValue();
    }

    public static float bradleyAutocannonVehicleDamage() {
        return BRADLEY_AUTOCANNON_VEHICLE_DAMAGE.get().floatValue();
    }

    /** 25mm rate of fire, in rounds per minute. */
    public static int bradleyAutocannonRoundsPerMinute() {
        return BRADLEY_AUTOCANNON_ROUNDS_PER_MINUTE.get();
    }

    /**
     * The same rate expressed as ticks between rounds, which is what the turret rules take.
     *
     * <p>Deliberately fractional: 180 rounds a minute is one round every six and two thirds ticks,
     * and the turret carries the remainder between shots, so this is the honest figure rather than
     * a rounded one that would fire at a rate nobody configured.
     */
    public static double bradleyAutocannonFireIntervalTicks() {
        int roundsPerMinute = Math.max(30, bradleyAutocannonRoundsPerMinute());
        return 1200.0D / roundsPerMinute;
    }

    public static double bradleyAutocannonSplashRadius() {
        return BRADLEY_AUTOCANNON_SPLASH_RADIUS.get();
    }

    public static boolean bradleyAutocannonBreaksBlocks() {
        return BRADLEY_AUTOCANNON_BREAKS_BLOCKS.get();
    }

    public static double bradleyAutocannonShellSpeed() {
        return BRADLEY_AUTOCANNON_SHELL_SPEED.get();
    }

    public static int bradleyAutocannonShellLifetimeTicks() {
        return BRADLEY_AUTOCANNON_SHELL_LIFETIME.get();
    }

    /** The 25mm ready box over its reserve, loaded with the reload key. */
    public static io.github.littlebird.entity.weapon.TwoStageMagazine.Spec bradleyAutocannonMagazine() {
        return new io.github.littlebird.entity.weapon.TwoStageMagazine.Spec(
                BRADLEY_AUTOCANNON_READY.get(), BRADLEY_AUTOCANNON_RESERVE.get(),
                BRADLEY_AUTOCANNON_RELOAD.get(), 0, 1);
    }

    public static float bradleyTowDamage() {
        return BRADLEY_TOW_DAMAGE.get().floatValue();
    }

    public static float bradleyTowVehicleDamage() {
        return BRADLEY_TOW_VEHICLE_DAMAGE.get().floatValue();
    }

    public static double bradleyTowSplashRadius() {
        return BRADLEY_TOW_SPLASH_RADIUS.get();
    }

    public static int bradleyTowFireIntervalTicks() {
        return BRADLEY_TOW_FIRE_INTERVAL.get();
    }

    public static double bradleyTowLauncherLateralBlocks() {
        return BRADLEY_TOW_LAUNCHER_LATERAL.get();
    }

    /** The TOW rail and its reserve, loaded with the reload key. */
    public static io.github.littlebird.entity.weapon.TwoStageMagazine.Spec bradleyTowMagazine() {
        return new io.github.littlebird.entity.weapon.TwoStageMagazine.Spec(
                BRADLEY_TOW_READY.get(), BRADLEY_TOW_RESERVE.get(), BRADLEY_TOW_RELOAD.get(), 0, 1);
    }

    public static float bradleyM2Damage() {
        return BRADLEY_M2_DAMAGE.get().floatValue();
    }

    public static float bradleyM2VehicleDamage() {
        return BRADLEY_M2_VEHICLE_DAMAGE.get().floatValue();
    }

    public static int bradleyM2Capacity() {
        return BRADLEY_M2_CAPACITY.get();
    }

    public static int bradleyM2FireIntervalTicks() {
        return BRADLEY_M2_FIRE_INTERVAL.get();
    }

    public static double bradleyM2SpreadDegrees() {
        return BRADLEY_M2_SPREAD.get();
    }

    /** The wall the discharger lays: layout, clocks and salvos. */
    public static io.github.littlebird.entity.weapon.SmokeWall.Spec bradleySmokeWall() {
        return new io.github.littlebird.entity.weapon.SmokeWall.Spec(
                BRADLEY_SMOKE_CANISTERS.get(), BRADLEY_SMOKE_SPACING.get(),
                BRADLEY_SMOKE_AHEAD.get(), BRADLEY_SMOKE_COOLDOWN.get(),
                BRADLEY_SMOKE_DURATION.get(), BRADLEY_SMOKE_CHARGES.get());
    }

    /** Heavy-armour column of the shared 70mm rocket. */
    public static float rocketHeavyDamage() {
        return ROCKET_HEAVY_DAMAGE.get().floatValue();
    }

    /** Heavy-armour column of the Apache's 30mm. */
    public static float apacheGunHeavyDamage() {
        return APACHE_GUN_HEAVY_DAMAGE.get().floatValue();
    }

    /** Heavy-armour column of the TOW. */
    public static float apacheMissileHeavyDamage() {
        return APACHE_MISSILE_HEAVY_DAMAGE.get().floatValue();
    }

    /** Heavy-armour column of the 120mm, priced per shell type. */
    public static float tankMainGunHeavyDamage(
            io.github.littlebird.entity.weapon.MainGunAmmoType type) {
        return switch (type) {
            case HE -> TANK_MAIN_GUN_HE_HEAVY_DAMAGE.get().floatValue();
            default -> TANK_MAIN_GUN_HEAVY_DAMAGE.get().floatValue();
        };
    }

    public static float ifvAutocannonHeavyDamage() {
        return IFV_AUTOCANNON_HEAVY_DAMAGE.get().floatValue();
    }

    public static float ifvAtgmHeavyDamage() {
        return IFV_ATGM_HEAVY_DAMAGE.get().floatValue();
    }

    public static float bradleyAutocannonHeavyDamage() {
        return BRADLEY_AUTOCANNON_HEAVY_DAMAGE.get().floatValue();
    }

    public static float bradleyTowHeavyDamage() {
        return BRADLEY_TOW_HEAVY_DAMAGE.get().floatValue();
    }
}
