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

    // CLIENT - ground vehicles
    private static final ForgeConfigSpec.DoubleValue TANK_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue TANK_ACCELERATION;
    private static final ForgeConfigSpec.DoubleValue TANK_BRAKING;
    private static final ForgeConfigSpec.DoubleValue TANK_TURN_RATE;
    private static final ForgeConfigSpec.DoubleValue TANK_TURN_STANDSTILL;
    private static final ForgeConfigSpec.DoubleValue IFV_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue IFV_ACCELERATION;
    private static final ForgeConfigSpec.DoubleValue IFV_BRAKING;
    private static final ForgeConfigSpec.DoubleValue IFV_TURN_RATE;
    private static final ForgeConfigSpec.DoubleValue IFV_TURN_STANDSTILL;

    // COMMON
    private static final ForgeConfigSpec.DoubleValue GUN_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue ROCKET_DIRECT_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue GUN_DAMAGE_AIRCRAFT;
    private static final ForgeConfigSpec.DoubleValue ROCKET_DAMAGE_AIRCRAFT;
    private static final ForgeConfigSpec.DoubleValue GUN_DAMAGE_APACHE;
    private static final ForgeConfigSpec.DoubleValue ROCKET_DAMAGE_APACHE;
    private static final ForgeConfigSpec.IntValue ROCKET_REARM_SECONDS;
    private static final ForgeConfigSpec.IntValue ROCKET_REARM_BATCH;
    private static final ForgeConfigSpec.BooleanValue ROCKETS_BREAK_BLOCKS;
    private static final ForgeConfigSpec.DoubleValue APACHE_ROCKET_DAMAGE_SCALE;
    private static final ForgeConfigSpec.DoubleValue APACHE_GUN_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue APACHE_GUN_AIRFRAME_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue APACHE_GUN_SPLASH_RADIUS;
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
    private static final ForgeConfigSpec.BooleanValue TANK_MAIN_GUN_BREAKS_BLOCKS;
    private static final ForgeConfigSpec.DoubleValue TANK_MAIN_GUN_PARTICLE_SCALE;
    private static final ForgeConfigSpec.DoubleValue TANK_COAX_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue TANK_COAX_VEHICLE_DAMAGE;
    private static final ForgeConfigSpec.IntValue TANK_COAX_CAPACITY;
    private static final ForgeConfigSpec.IntValue TANK_COAX_FIRE_INTERVAL;
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
        client.comment("Ground vehicle handling. Motion is client authoritative, so this is a",
                        "client side feel setting just like the flight block above. Speeds are",
                        "blocks per tick: 0.30 is six blocks a second.").push("ground");
        client.comment("M1A5 main battle tank: heavy, slow, but it pivots on its tracks.").push("tank");
        TANK_MAX_SPEED = client
                .comment("Top speed in blocks per tick.")
                .defineInRange("max_speed", 0.30D, 0.05D, 1.0D);
        TANK_ACCELERATION = client
                .comment("Speed gained per tick while the throttle is held.")
                .defineInRange("acceleration", 0.02D, 0.001D, 0.2D);
        TANK_BRAKING = client
                .comment("Speed lost per tick when braking or coasting.")
                .defineInRange("braking", 0.04D, 0.001D, 0.3D);
        TANK_TURN_RATE = client
                .comment("Degrees of heading change per tick at full speed.")
                .defineInRange("turn_rate_degrees_per_tick", 2.0D, 0.2D, 15.0D);
        TANK_TURN_STANDSTILL = client
                .comment("Fraction of the turn rate available stopped, so a tracked hull can",
                        "pivot on the spot.")
                .defineInRange("turn_at_standstill", 0.35D, 0.0D, 1.0D);
        client.pop();
        client.comment("EBAA Wildcat IFV: faster and nimbler, but it cannot pivot in place.")
                .push("ifv");
        IFV_MAX_SPEED = client
                .comment("Top speed in blocks per tick.")
                .defineInRange("max_speed", 0.45D, 0.05D, 1.0D);
        IFV_ACCELERATION = client
                .comment("Speed gained per tick while the throttle is held.")
                .defineInRange("acceleration", 0.03D, 0.001D, 0.2D);
        IFV_BRAKING = client
                .comment("Speed lost per tick when braking or coasting.")
                .defineInRange("braking", 0.05D, 0.001D, 0.3D);
        IFV_TURN_RATE = client
                .comment("Degrees of heading change per tick at full speed.")
                .defineInRange("turn_rate_degrees_per_tick", 3.5D, 0.2D, 15.0D);
        IFV_TURN_STANDSTILL = client
                .comment("Fraction of the turn rate available stopped. Wheels barely steer",
                        "standing still.")
                .defineInRange("turn_at_standstill", 0.20D, 0.0D, 1.0D);
        client.pop();
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
        ROCKET_REARM_SECONDS = common
                .comment("Seconds between rocket resupply ticks (0 disables resupply).")
                .defineInRange("rocket_rearm_seconds", 10, 0, 600);
        ROCKET_REARM_BATCH = common
                .comment("Rockets added per resupply tick, split between the two pods.")
                .defineInRange("rocket_rearm_batch", 7, 1, 14);
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
        APACHE_GUN_SPLASH_RADIUS = common
                .comment("Splash radius of a 30mm round, in blocks (0 disables splash).")
                .defineInRange("chain_gun_splash_radius", 1.0D, 0.0D, 6.0D);
        APACHE_MISSILE_DAMAGE = common
                .comment("Damage a TOW missile deals to the entity it directly hits.")
                .defineInRange("missile_damage", 60.0D, 0.0D, 300.0D);
        APACHE_MISSILE_AIRFRAME_DAMAGE = common
                .comment("Damage a TOW missile deals to an airframe.")
                .defineInRange("missile_airframe_damage", 20.0D, 0.0D, 300.0D);
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
        TANK_MAIN_GUN_HE_SPLASH_RADIUS = common
                .comment("Blast radius of the 120mm HE round, in blocks (larger than AP).")
                .defineInRange("main_gun_he_splash_radius", 4.0D, 0.0D, 12.0D);
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
                .comment("Coaxial machine gun belt capacity.")
                .defineInRange("coax_capacity", 300, 10, 4000);
        TANK_COAX_FIRE_INTERVAL = common
                .comment("Ticks between coaxial rounds.")
                .defineInRange("coax_fire_interval_ticks", 2, 1, 40);
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
        IFV_AUTOCANNON_CAPACITY = common
                .comment("Twin 30mm belt capacity.")
                .defineInRange("autocannon_capacity", 600, 10, 4000);
        IFV_ATGM_DAMAGE = common
                .comment("Damage an ATGM deals to an unarmoured target.")
                .defineInRange("atgm_damage", 60.0D, 0.0D, 300.0D);
        IFV_ATGM_VEHICLE_DAMAGE = common
                .comment("Damage an ATGM deals to a vehicle.")
                .defineInRange("atgm_vehicle_damage", 20.0D, 0.0D, 300.0D);
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
        common.pop();
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
                TANK_BRAKING.get(), TANK_TURN_RATE.get(), TANK_TURN_STANDSTILL.get());
    }

    /** Handling of the infantry fighting vehicle, from the CLIENT ground section. */
    public static GroundDrive.Spec ifvDriveSpec() {
        return new GroundDrive.Spec(IFV_MAX_SPEED.get(), IFV_ACCELERATION.get(),
                IFV_BRAKING.get(), IFV_TURN_RATE.get(), IFV_TURN_STANDSTILL.get());
    }

    /**
     * The two 120mm shell types are priced separately. The armour-piercing keys keep their original
     * names, so an existing config file keeps working; HE has its own {@code main_gun_he_*} keys.
     */
    public static float tankMainGunDamage(
            io.github.littlebird.entity.weapon.MainGunAmmoType type) {
        return type == io.github.littlebird.entity.weapon.MainGunAmmoType.HE
                ? TANK_MAIN_GUN_HE_DAMAGE.get().floatValue()
                : TANK_MAIN_GUN_DAMAGE.get().floatValue();
    }

    public static float tankMainGunVehicleDamage(
            io.github.littlebird.entity.weapon.MainGunAmmoType type) {
        return type == io.github.littlebird.entity.weapon.MainGunAmmoType.HE
                ? TANK_MAIN_GUN_HE_VEHICLE_DAMAGE.get().floatValue()
                : TANK_MAIN_GUN_VEHICLE_DAMAGE.get().floatValue();
    }

    public static double tankMainGunSplashRadius(
            io.github.littlebird.entity.weapon.MainGunAmmoType type) {
        return type == io.github.littlebird.entity.weapon.MainGunAmmoType.HE
                ? TANK_MAIN_GUN_HE_SPLASH_RADIUS.get()
                : TANK_MAIN_GUN_SPLASH_RADIUS.get();
    }

    /** The 120mm shell destroys terrain by default; a server can turn that off. */
    public static boolean mainGunBreaksBlocks() {
        return TANK_MAIN_GUN_BREAKS_BLOCKS.get();
    }

    /** Scale on the 120mm muzzle flash, fireball and impact dust particle counts. */
    public static double mainGunParticleScale() {
        return TANK_MAIN_GUN_PARTICLE_SCALE.get();
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
}
