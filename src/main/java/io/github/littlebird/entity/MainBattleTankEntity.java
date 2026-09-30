package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.GroundViewMath;
import io.github.littlebird.entity.ground.GroundContact;
import io.github.littlebird.entity.ground.GroundDrive;
import io.github.littlebird.entity.ground.GroundMover;
import io.github.littlebird.entity.ground.HullPose;
import io.github.littlebird.entity.ground.SlopeTracker;
import io.github.littlebird.entity.ground.GroundVehicleControls;
import io.github.littlebird.entity.ground.GroundWeapons;
import io.github.littlebird.entity.ground.MainGun;
import io.github.littlebird.entity.seat.GroundSeat;
import io.github.littlebird.entity.weapon.AmmoPool;
import io.github.littlebird.entity.weapon.AmmoSupply;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.MainGunAmmo;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
import io.github.littlebird.entity.weapon.SmokeLauncher;
import io.github.littlebird.entity.weapon.TwoStageMagazine;
import io.github.littlebird.entity.weapon.WeaponSelection;
import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import io.github.littlebird.registry.ModEntities;
import io.github.littlebird.registry.ModItems;
import io.github.littlebird.registry.ModSounds;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * M1A5 main battle tank: a two seat, tracked ground vehicle.
 *
 * <p>Motion follows the pack's established split: the driver's client is authoritative for the
 * hull, which is what vanilla's ServerboundMoveVehiclePacket carries, while the turret, the guns,
 * the ammunition and damage stay server authoritative. The hull is driven with throttle and
 * steering (a tank is not flown), which is the one place this vehicle deliberately does not reuse
 * the helicopter model - {@link GroundDrive} is its own {@code GroundDrive} rule set.
 *
 * <p>The driver lays and fires the 120mm main gun; the gunner fires the coaxial machine gun. Both
 * share the turret's attitude, which keeps a two man crew useful without inventing a second
 * turret this hull does not have (a documented simplification).
 *
 * <p>Deliberately not a subclass or a copy of the IFV: the two sharing only {@link GroundDrive},
 * {@link GroundWeapons} and the seat markers, never each other's weapon logic.
 */
public class MainBattleTankEntity extends Entity
        implements GunshipRider, GroundVehicleControls, io.github.littlebird.client.GroundView {
    public static final float MAX_HEALTH = 120.0F;

    private static final float FALL_GRAVITY = 0.08F;
    private static final double MAX_FALL_SPEED = 1.0D;
    /**
     * Grounded hulls keep a small downward request: Entity.move clears onGround on a strictly zero
     * vertical move, and a flickering onGround then stalled the step assist and the grade probe on
     * alternate ticks.
     */
    private static final double GROUND_CONTACT_PROBE = 0.05D;
    private static final float COAX_SPLASH_RADIUS = 1.0F;
    private static final float SPLASH_DAMAGE_FRACTION = 0.5F;

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURRET_YAW =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURRET_PITCH =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_READY_ROUNDS =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RESERVE_ROUNDS =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COAX_AMMO =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CUPOLA_AMMO =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    /** Ordinal of the loaded 120mm shell type, for the HUD. */
    private static final EntityDataAccessor<Integer> DATA_SHELL_TYPE =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    /** Slot the driver has selected (main gun / smoke). */
    private static final EntityDataAccessor<Integer> DATA_SELECTED_WEAPON =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SMOKE_CHARGES =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    /** Reload cooldown and resupply cooldown of the 120mm, both in ticks, for the HUD. */
    private static final EntityDataAccessor<Integer> DATA_MAIN_GUN_RELOAD =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAIN_GUN_RESUPPLY =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SMOKE_COOLDOWN =
            SynchedEntityData.defineId(MainBattleTankEntity.class, EntityDataSerializers.INT);

    private final AircraftDestruction destruction = new AircraftDestruction();

    /** Client prediction state; null whenever this client is not the driver. */
    private GroundDrive.State clientDrive;
    private int clientControls;

    private MainGun.State turret = MainGun.State.initial();
    /** One ready rack and reserve per 120mm shell type. */
    private MainGunAmmo mainGunAmmo;
    private AmmoPool coax;
    /** Seat two's cupola gun: a separate belt and heat clock from the driver's coax. */
    private AmmoPool cupola;
    /** Smoke discharger state and the driver's weapon carousel (main gun / smoke). */
    private SmokeLauncher.Spec smokeSpec;
    private SmokeLauncher.State smoke;
    private final WeaponSelection driverWeapons = WeaponSelection.forTankDriver();
    /** Client only: whether this hull is shown through its turret interior camera. */
    private boolean interiorView;
    private int coaxCooldown;
    private int cupolaCooldown;
    private boolean driverTrigger;
    private boolean gunnerTrigger;
    private int supplyTick;
    /** Smoothed hull pitch and roll fitted from the four-corner contact plane. */
    private final HullPose hullPose = new HullPose(60.0D, 35.0D, 4.0D, 0.75D, 0.35D);
    /** Travelled slope, kept as the fallback for when no contact plane can be fitted. */
    private final SlopeTracker slopeTracker = new SlopeTracker();
    /** Client-side hull roll for the renderer; positive when the right side is lower. */
    private float hullRollDeg;

    public MainBattleTankEntity(EntityType<? extends MainBattleTankEntity> type, Level level) {
        super(type, level);
        // Entity.move ignores maxUpStep (only LivingEntity reads it), so the climb is done by
        // GroundMover.stepUp from tickClient, capped by LittleBirdConfig.tankStepHeightBlocks().
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_TURRET_YAW, 0.0F);
        entityData.define(DATA_TURRET_PITCH, 0.0F);
        entityData.define(DATA_READY_ROUNDS, 0);
        entityData.define(DATA_RESERVE_ROUNDS, 0);
        entityData.define(DATA_COAX_AMMO, 0);
        entityData.define(DATA_CUPOLA_AMMO, 0);
        entityData.define(DATA_SHELL_TYPE, 0);
        entityData.define(DATA_SELECTED_WEAPON, WeaponSelection.MAIN_GUN);
        entityData.define(DATA_SMOKE_CHARGES, 0);
        entityData.define(DATA_SMOKE_COOLDOWN, 0);
        entityData.define(DATA_MAIN_GUN_RELOAD, 0);
        entityData.define(DATA_MAIN_GUN_RESUPPLY, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickClient();
        } else {
            tickServer();
        }
    }

    // ------------------------------------------------------------------
    // Server
    // ------------------------------------------------------------------

    private void tickServer() {
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        ensureMagazines();
        mainGunAmmo.tick();
        // Reload cooldown: with auto reload on, the gun starts its own reload the moment the ready
        // rack is not full, so the cooldown plays without the crew pressing the reload key.
        if (LittleBirdConfig.tankMainGunAutoReload() && mainGunAmmo.startReload()) {
            publishAmmo();
        }
        smoke = SmokeLauncher.tick(smoke, smokeSpec);
        if (smoke.cloudActive() && level() instanceof ServerLevel server) {
            emitSmoke(server);
        }
        tickTurret();
        tickAmmoSupply();
        publishSmoke();
        if (getControllingPassenger() == null && !isRemoved()) {
            applyUnpilotedMotion();
        }
    }

    /** Lazily builds the magazines from the config, once the config is definitely loaded. */
    private void ensureMagazines() {
        // Fill in whatever a load path left behind. An old save can restore the magazines but no
        // smoke launcher, so this must be idempotent rather than a one-shot guard.
        if (mainGunAmmo == null) {
            mainGunAmmo = MainGunAmmo.initial(LittleBirdConfig.tankMainGunMagazine());
        }
        if (coax == null) {
            coax = AmmoPool.full(LittleBirdConfig.tankCoaxCapacity());
        }
        if (cupola == null) {
            cupola = AmmoPool.full(LittleBirdConfig.tankCupolaCapacity());
        }
        if (smokeSpec == null) {
            smokeSpec = LittleBirdConfig.tankSmokeSpec();
        }
        if (smoke == null) {
            smoke = SmokeLauncher.initial(smokeSpec);
        }
        publishAmmo();
        publishSmoke();
    }

    private void publishAmmo() {
        entityData.set(DATA_READY_ROUNDS, mainGunAmmo.ready());
        entityData.set(DATA_RESERVE_ROUNDS, mainGunAmmo.reserve());
        entityData.set(DATA_COAX_AMMO, coax.rounds());
        entityData.set(DATA_CUPOLA_AMMO, cupola.rounds());
        entityData.set(DATA_SHELL_TYPE, mainGunAmmo.selected().ordinal());
        entityData.set(DATA_MAIN_GUN_RELOAD, mainGunAmmo.state().reloadTicks());
        entityData.set(DATA_MAIN_GUN_RESUPPLY, mainGunAmmo.state().resupplyTicks());
    }

    private void publishSmoke() {
        entityData.set(DATA_SELECTED_WEAPON, driverWeapons.selected());
        entityData.set(DATA_SMOKE_CHARGES, smoke.charges());
        entityData.set(DATA_SMOKE_COOLDOWN, smoke.cooldownTicks());
    }

    private void tickTurret() {
        Player driver = driver();
        Player gunner = gunner();
        if (driver == null) {
            driverTrigger = false;
        }
        if (gunner == null) {
            gunnerTrigger = false;
        }

        MainGun.Aim desired = driver == null
                ? MainGun.desiredAim(getYRot(), getXRot())
                : MainGun.desiredAim(driver.getYRot(), driver.getXRot());
        MainGun.Step step = MainGun.advance(turret, desired,
                driverTrigger && driver != null && driverWeapons.is(WeaponSelection.MAIN_GUN));
        turret = step.state();
        entityData.set(DATA_TURRET_YAW, (float) turret.aimYawDegrees());
        entityData.set(DATA_TURRET_PITCH, (float) turret.aimPitchDegrees());

        if (step.fire() && driver != null && mainGunAmmo.ready() > 0
                && level() instanceof ServerLevel server) {
            mainGunAmmo.fire();
            publishAmmo();
            fireMainGun(server, driver);
            // Start the interval only now, on a round that really left the barrel. A trigger pulled
            // on an empty rack, or one the gate refused, must not lock the gun out.
            turret = MainGun.afterFire(turret, LittleBirdConfig.tankMainGunFireIntervalTicks());
        }

        if (coaxCooldown > 0) {
            coaxCooldown--;
        }
        if (cupolaCooldown > 0) {
            cupolaCooldown--;
        }
        // Seat two has the cupola gun; the driver's selectable coaxial gun is a separate mount with
        // its own belt and heat clock, so one seat cannot drain or slow the other's gun.
        if (gunner != null && gunnerTrigger && cupolaCooldown <= 0 && cupola.fire(1)
                && level() instanceof ServerLevel server) {
            cupolaCooldown = LittleBirdConfig.tankCupolaFireIntervalTicks();
            publishAmmo();
            fireCoax(server, gunner, true);
        }
        if (driver != null && driverTrigger && driverWeapons.is(WeaponSelection.COAX)
                && coaxCooldown <= 0 && coax.fire(1) && level() instanceof ServerLevel server) {
            coaxCooldown = LittleBirdConfig.tankCoaxFireIntervalTicks();
            publishAmmo();
            fireCoax(server, driver, false);
        }

        // Smoke is the driver's other slot: one charge per trigger, gated by its own cooldown.
        if (driver != null && driverTrigger && driverWeapons.is(WeaponSelection.SMOKE)) {
            SmokeLauncher.State fired = SmokeLauncher.fire(smoke, smokeSpec);
            if (fired.charges() != smoke.charges()) {
                smoke = fired;
                publishSmoke();
            }
        }
    }

    /** A short-lived cloud around the hull: Minecraft has no volumetric smoke, so this is particles. */
    private void emitSmoke(ServerLevel server) {
        double radius = LittleBirdConfig.tankSmokeRadiusBlocks();
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double spread = radius * Math.sqrt(random.nextDouble());
            double x = getX() + Math.cos(angle) * spread;
            double z = getZ() + Math.sin(angle) * spread;
            double y = getY() + 0.4D + random.nextDouble() * 1.2D;
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1,
                    0.02D, 0.02D, 0.02D, 0.005D);
        }
    }

    /**
     * One 120mm round: a shell entity down the turret's attitude, priced by the loaded shell type.
     * Unlike the coaxial gun this is not a hitscan round - the player sees the muzzle fireball
     * travel and the round break what it lands on.
     */
    private void fireMainGun(ServerLevel server, Player driver) {
        MainGunAmmoType shell = mainGunAmmo.selected();
        Vec3 muzzle = muzzle();
        if (shell == MainGunAmmoType.CANISTER) {
            // Canister is a muzzle burst, not a travelling shell: forty pellet hitscans down the
            // turret's attitude. The same muzzle flash and report wrap it as a main gun round.
            GroundWeapons.fireCanister(server, this, driver, muzzle, turretDirection(),
                    LittleBirdConfig.tankCanisterPellets(),
                    LittleBirdConfig.tankCanisterSpreadHorizontalDegrees(),
                    LittleBirdConfig.tankCanisterSpreadVerticalDegrees(),
                    LittleBirdConfig.tankCanisterPelletDamage(),
                    LittleBirdConfig.tankCanisterPelletVehicleDamage(),
                    0.0F,
                    LittleBirdConfig.tankCanisterRange());
            MainGunShellEntity.muzzleFlash(server, muzzle, LittleBirdConfig.mainGunParticleScale());
            server.playSound(null, getX(), getY(), getZ(), ModSounds.GUN_SHOT.get(),
                    SoundSource.PLAYERS, 1.6F, 0.5F);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.EXPLOSION.get(),
                    SoundSource.PLAYERS, 1.3F, 0.45F);
            return;
        }
        MainGunShellEntity round = new MainGunShellEntity(ModEntities.MAIN_GUN_SHELL.get(), server);
        round.setPos(muzzle.x, muzzle.y, muzzle.z);
        round.configure(shell, LittleBirdConfig.tankMainGunDamage(shell),
                LittleBirdConfig.tankMainGunVehicleDamage(shell),
                LittleBirdConfig.tankMainGunHeavyDamage(shell),
                LittleBirdConfig.tankMainGunSplashRadius(shell));
        round.launch(turretDirection(), MainGunShellBallistics.MUZZLE_SPEED, driver);
        server.addFreshEntity(round);
        MainGunShellEntity.muzzleFlash(server, muzzle, LittleBirdConfig.mainGunParticleScale());
        // The cannon report is two existing clips layered: a sharp crack plus a low boom, with the
        // HE round a touch deeper than AP. No new audio files are shipped (this rig cannot encode
        // ogg), so the whole chain reuses the mod's own sounds.
        float pitch = shell == MainGunAmmoType.HE ? 0.5F : 0.62F;
        server.playSound(null, getX(), getY(), getZ(), ModSounds.GUN_SHOT.get(),
                SoundSource.PLAYERS, 1.5F, pitch);
        server.playSound(null, getX(), getY(), getZ(), ModSounds.EXPLOSION.get(),
                SoundSource.PLAYERS, 1.2F, pitch - 0.1F);
    }

    /** One machine gun round from the firing seat's own barrel, not the main gun's muzzle. */
    private void fireCoax(ServerLevel server, Player shooter, boolean cupola) {
        GroundWeapons.fireHitscan(server, this, shooter, machineGunMuzzle(cupola),
                turretDirection(),
                LittleBirdConfig.tankCoaxDamage(), LittleBirdConfig.tankCoaxVehicleDamage(),
                0.0F, COAX_SPLASH_RADIUS, SPLASH_DAMAGE_FRACTION, 0.9F,
                io.github.littlebird.entity.weapon.DamageType.BULLET);
    }

    /**
     * Muzzle of the machine gun the given seat is firing. The driver's coaxial gun is the model's
     * M240 beside the ring; seat two's is the cupola .50 on top. Both barrels are fixed to the
     * turret, so the ring offset turns with the turret's aim. Two players crewing the tank must
     * see two different muzzle points, not one shared hole at the main gun.
     */
    private Vec3 machineGunMuzzle(boolean cupola) {
        MainBattleTankGeometry.MachineGunMuzzle mg =
                MainBattleTankGeometry.machineGunMuzzle(cupola);
        Vec3 ring = position().add(0.0D, MainBattleTankGeometry.turretPivotHeightBlocks(), 0.0D);
        Vec3 aimRight = vec(MainGun.aimDirection(turret.aimYawDegrees() - 90.0D, 0.0D));
        if (aimRight.lengthSqr() > 1.0E-9D) {
            ring = ring.add(aimRight.normalize().scale(mg.lateralBlocks()));
        }
        ring = ring.add(0.0D, mg.heightBlocks(), 0.0D);
        return ring.add(turretDirection().scale(mg.barrelBlocks()));
    }

    private Vec3 turretDirection() {
        return vec(MainGun.aimDirection(turret.aimYawDegrees(), turret.aimPitchDegrees()));
    }

    private Vec3 muzzle() {
        Vec3 hullForward = Vec3.directionFromRotation(0.0F, getYRot());
        // The barrel pivots on its trunnion at the mantlet, so the muzzle is the trunnion plus the
        // barrel reach along the gun line - not the ring plus the whole barrel. The trunnion turns
        // with the turret, hence the turret's own forward rather than the hull's.
        Vec3 turretForward = Vec3.directionFromRotation(0.0F, (float) turret.aimYawDegrees());
        return vec(MainGun.muzzleOrigin(vector(position()), vector(turretForward),
                new LittleBirdBallistics.Vector3(0.0D, 1.0D, 0.0D),
                MainBattleTankGeometry.gunTrunnionHeightBlocks(),
                MainBattleTankGeometry.gunTrunnionForwardBlocks(),
                MainBattleTankGeometry.barrelReachBlocks(),
                turret.aimYawDegrees(), turret.aimPitchDegrees()));
    }

    /** Ammunition comes back only from an ammunition crate parked next to the tank. */
    private void tickAmmoSupply() {
        if (++supplyTick < 20) {
            return;
        }
        supplyTick = 0;
        if (!AircraftRearm.crateNearby(this)) {
            return;
        }
        AmmoSupply supply = LittleBirdConfig.ammoSupply();
        coax.refill(supply.bullets(20));
        cupola.refill(supply.bullets(20));
        mainGunAmmo.supply(supply.rockets(20));
        smoke = SmokeLauncher.supply(smoke, smokeSpec, 1);
        publishAmmo();
    }

    /** Nobody is driving: a hull on the ground stays put, one in the air falls. */
    private void applyUnpilotedMotion() {
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 motion = getDeltaMovement();
        Vec3 falling = new Vec3(motion.x * 0.9D,
                Math.max(motion.y - FALL_GRAVITY, -MAX_FALL_SPEED), motion.z * 0.9D);
        setDeltaMovement(falling);
        move(MoverType.SELF, falling);
    }

    // ------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------

    private void tickClient() {
        if (!(isControlledByLocalInstance() && getControllingPassenger() instanceof Player driver)) {
            clientDrive = null;
            return;
        }
        // The contact plane only drives the render attitude. The drive's grade stays on the
        // travelled slope, which cannot look at ground the hull has not reached, so the speed
        // cannot lead the terrain. The probes sit inside the hull's own AABB footprint, so the
        // sample is the surface it is standing on rather than the one ahead.
        GroundContact.Sample surface = GroundMover.sampleSurface(this,
                getBbWidth() * 0.5D, getBbWidth() * 0.5D);
        hullPose.update(surface);
        double grade = slopeTracker.gradeDegrees();

        GroundDrive.State prior = new GroundDrive.State(
                clientDrive == null ? 0.0D : clientDrive.speedBlocksPerTick(), getYRot());
        GroundDrive.Step step = GroundDrive.advance(prior, GroundDrive.throttle(clientControls),
                GroundDrive.steer(clientControls), LittleBirdConfig.tankDriveSpec(), grade);
        clientDrive = step.state();
        setYRot((float) step.state().yawDegrees());
        setYHeadRot(getYRot());
        double dy = onGround() ? -GROUND_CONTACT_PROBE
                : Math.max(getDeltaMovement().y - FALL_GRAVITY, -MAX_FALL_SPEED);
        double startY = getY();
        double requestedDistance = Math.sqrt(step.dx() * step.dx() + step.dz() * step.dz());
        Vec3 drive = new Vec3(step.dx(), dy, step.dz());
        setDeltaMovement(drive);
        move(MoverType.SELF, drive);
        GroundMover.stepUp(this, drive, LittleBirdConfig.tankStepHeightBlocks());
        // Travelled slope: the drive's grade, and the fallback for the pose when no plane fits.
        slopeTracker.update(onGround() ? getY() - startY : 0.0D,
                onGround() ? requestedDistance : 0.0D);
        setXRot((float) -hullPose.pitchDegrees());
        hullRollDeg = (float) hullPose.rollDegrees();
        // No local turret preview. The server slews and clamps the barrel with MainGun and
        // publishes it; a second writer here (the raw, unclamped look angles) fought that synced
        // value every tick, which is exactly what made the barrel twitch while driving. The
        // attitude now has one source of truth.
    }

    /** Client only: the drive axes for this client's driver. */
    @Override
    public void setClientControls(int controls) {
        this.clientControls = controls;
    }

    /** Client only: forgets the prediction when this hull is left behind. */
    @Override
    public void resetClientPrediction() {
        this.clientDrive = null;
        this.clientControls = 0;
        this.hullPose.reset();
        this.slopeTracker.reset();
        this.hullRollDeg = 0.0F;
    }

    /** Client only: hull roll from the contact plane, positive when the right side is lower. */
    public float getHullRollDeg() {
        return hullRollDeg;
    }

    /** Server: the driver or gunner reports the trigger (and the reload key). */
    @Override
    public boolean isInteriorView() {
        return interiorView;
    }

    @Override
    public void setInteriorView(boolean value) {
        this.interiorView = value;
    }

    /**
     * While this machine is looking through the turret interior, the barrel is the driver's own aim
     * one for one, so the freshest and smoothest yaw is the driver's own interpolated view yaw.
     * Reading the synched copy instead samples the 20 Hz server tick and steps the whole view.
     */
    @Override
    public float renderTurretYawDeg(float partialTick) {
        if (interiorView && isControlledByLocalInstance()
                && getControllingPassenger() instanceof Player driver) {
            return driver.getViewYRot(partialTick);
        }
        return getTurretYawDeg();
    }

    @Override
    public float renderTurretPitchDeg(float partialTick) {
        if (interiorView && isControlledByLocalInstance()
                && getControllingPassenger() instanceof Player driver) {
            return (float) MainGun.desiredAim(0.0F, driver.getViewXRot(partialTick)).pitchDegrees();
        }
        return getTurretPitchDeg();
    }

    @Override
    public void applyWeaponInput(Player sender, boolean triggerHeld, boolean reload,
                               int shellDelta, int weaponDelta) {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        boolean isDriver = sender == driver();
        if (isDriver) {
            driverTrigger = triggerHeld;
        } else if (sender == gunner()) {
            gunnerTrigger = triggerHeld;
        } else {
            return;
        }
        if (isDriver && shellDelta != 0 && mainGunAmmo != null) {
            // The driver owns the carousel: step to the next (or previous) shell type.
            mainGunAmmo.select(MainGunAmmoType.byOrdinal(
                    mainGunAmmo.selected().ordinal() + shellDelta));
            publishAmmo();
        }
        if (isDriver && weaponDelta != 0) {
            driverWeapons.cycle(weaponDelta);
            publishSmoke();
        }
        if (reload && mainGunAmmo != null && mainGunAmmo.startReload()) {
            publishAmmo();
        }
    }

    // ------------------------------------------------------------------
    // Riding
    // ------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (player.getMainHandItem().isEmpty() && getPassengers().isEmpty()) {
                if (!level().isClientSide) {
                    packInto(player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return InteractionResult.PASS;
        }
        if (!canAddPassenger(player)) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void packInto(Player player) {
        ItemStack stack = new ItemStack(ModItems.MAIN_BATTLE_TANK.get());
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Health", getHealth());
        stack.setTag(tag);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        discard();
    }

    @Override
    public double getPassengersRidingOffset() {
        return MainBattleTankGeometry.seatHeightBlocks(GroundSeat.TANK_DRIVER);
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int seat = seatOf(passenger);
        double forward = MainBattleTankGeometry.seatForwardBlocks(seat);
        double lateral = MainBattleTankGeometry.seatLateralBlocks(seat);
        // The turret interior camera is rigidly attached to the turret: while that view is up the
        // seat offset is measured from the synched turret yaw, so the turret's own shell cannot
        // sweep across the lens. Every other camera keeps the hull yaw, exactly as before; the
        // camera pitch still comes from the player, never from the barrel's elevation.
        double yaw = Math.toRadians(GroundViewMath.followYawDeg(
                interiorView, getYRot(), renderTurretYawDeg(1.0F)));
        // Minecraft yaw 0 faces +Z; the hull's forward is (-sin, cos) and its right is (-cos, -sin).
        double dx = forward * -Math.sin(yaw) + lateral * -Math.cos(yaw);
        double dz = forward * Math.cos(yaw) + lateral * -Math.sin(yaw);
        // The turret interior camera rides a little above the seat, still inside the turret ring.
        // The same lift the aircraft cockpit uses, so both interiors sit at the same eye height.
        double height = MainBattleTankGeometry.seatHeightBlocks(seat)
                + (interiorView ? LittleBirdConfig.cockpitEyeLiftBlocks() : 0.0D);
        moveFunction.accept(passenger,
                getX() + dx,
                getY() + height + passenger.getMyRidingOffset(),
                getZ() + dz);
    }

    @Override
    public net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return driver();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < GroundSeat.TANK_SEATS && passenger instanceof Player;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    /** Seat index of a rider: 0 is the driver, or -1 if they are not aboard. */
    @Override
    public int seatOf(Entity passenger) {
        int index = getPassengers().indexOf(passenger);
        return index < 0 ? -1 : GroundSeat.seatOf(index, GroundSeat.TANK_SEATS);
    }

    @Override
    public boolean isDriver(int seat) {
        return GroundSeat.isTankDriver(seat);
    }

    @Override
    public boolean isOperator(int seat) {
        return GroundSeat.isTankDriver(seat) || GroundSeat.isTankGunner(seat);
    }

    public Player driver() {
        return getPassengers().isEmpty() || !(getPassengers().get(0) instanceof Player player)
                ? null : player;
    }

    public Player gunner() {
        return getPassengers().size() < 2 || !(getPassengers().get(1) instanceof Player player)
                ? null : player;
    }

    // ------------------------------------------------------------------
    // Damage
    // ------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || source.is(DamageTypeTags.IS_FALL)
                || isInvulnerableTo(source) || !destruction.acceptsDamage()) {
            return false;
        }
        float remaining = getHealth() - amount;
        setHealth(remaining);
        if (remaining <= 0.0F) {
            destroyVehicle();
        }
        return true;
    }

    /** Writes the hull off: the crew is thrown clear, then the wreck goes up. Once only. */
    private void destroyVehicle() {
        if (!destruction.beginDestruction()) {
            return;
        }
        ejectCrew();
        if (level() instanceof ServerLevel server) {
            server.explode(this, getX(), getY(), getZ(), 3.5F, Level.ExplosionInteraction.NONE);
        }
        // Scrap after the blast, not before: anything dropped first would be blown up with it.
        spawnAtLocation(Items.IRON_INGOT, 4 + random.nextInt(5));
        discard();
    }

    private void ejectCrew() {
        Vec3 motion = getDeltaMovement();
        for (Entity passenger : List.copyOf(getPassengers())) {
            passenger.stopRiding();
            passenger.resetFallDistance();
            passenger.invulnerableTime = 20;
            passenger.setDeltaMovement(motion.scale(0.4D).add(0.0D, 0.25D, 0.0D));
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    // ------------------------------------------------------------------
    // State accessors
    // ------------------------------------------------------------------

    public float getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    public void setHealth(float health) {
        entityData.set(DATA_HEALTH, Math.max(0.0F, Math.min(MAX_HEALTH, health)));
    }

    @Override
    public float healthFraction() {
        return getHealth() / MAX_HEALTH;
    }

    public float getTurretYawDeg() {
        return entityData.get(DATA_TURRET_YAW);
    }

    public float getTurretPitchDeg() {
        return entityData.get(DATA_TURRET_PITCH);
    }

    public int getReadyRounds() {
        return entityData.get(DATA_READY_ROUNDS);
    }

    public int getReserveRounds() {
        return entityData.get(DATA_RESERVE_ROUNDS);
    }

    public int getCoaxAmmo() {
        return entityData.get(DATA_COAX_AMMO);
    }

    /** Seat two's cupola belt, for the gunner's HUD row. */
    public int getCupolaAmmo() {
        return entityData.get(DATA_CUPOLA_AMMO);
    }

    /** Ticks left on the 120mm reload cooldown, for the HUD. */
    public int getMainGunReloadTicks() {
        return entityData.get(DATA_MAIN_GUN_RELOAD);
    }

    /** Ticks left on the 120mm resupply cooldown, for the HUD. */
    public int getMainGunResupplyTicks() {
        return entityData.get(DATA_MAIN_GUN_RESUPPLY);
    }

    /** Ordinal of the loaded 120mm shell type, for the HUD. */
    public int getShellTypeOrdinal() {
        return entityData.get(DATA_SHELL_TYPE);
    }

    /** Slot the driver has selected (see WeaponSelection), for the HUD. */
    public int getSelectedWeapon() {
        return entityData.get(DATA_SELECTED_WEAPON);
    }

    public int getSmokeCharges() {
        return entityData.get(DATA_SMOKE_CHARGES);
    }

    public int getSmokeCooldownTicks() {
        return entityData.get(DATA_SMOKE_COOLDOWN);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setHealth(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        turret = new MainGun.State(tag.getFloat("TurretYaw"), tag.getFloat("TurretPitch"),
                tag.getInt("TurretCooldown"));
        entityData.set(DATA_TURRET_YAW, (float) turret.aimYawDegrees());
        entityData.set(DATA_TURRET_PITCH, (float) turret.aimPitchDegrees());
        if (tag.contains("ShellType")) {
            TwoStageMagazine.Spec spec = LittleBirdConfig.tankMainGunMagazine();
            TwoStageMagazine.State[] racks = new TwoStageMagazine.State[MainGunAmmoType.types()];
            for (MainGunAmmoType type : MainGunAmmoType.values()) {
                racks[type.ordinal()] = new TwoStageMagazine.State(
                        tag.getInt("Ammo_" + type.id() + "_Ready"),
                        tag.getInt("Ammo_" + type.id() + "_Reserve"), 0, 0);
            }
            mainGunAmmo = MainGunAmmo.of(spec, MainGunAmmoType.byOrdinal(tag.getInt("ShellType")),
                    racks);
            coax = AmmoPool.empty(LittleBirdConfig.tankCoaxCapacity());
            coax.refill(tag.getInt("CoaxAmmo"));
            smokeSpec = LittleBirdConfig.tankSmokeSpec();
            smoke = tag.contains("SmokeCharges")
                    ? new SmokeLauncher.State(tag.getInt("SmokeCharges"),
                            tag.getInt("SmokeCooldown"), tag.getInt("SmokeActive"))
                    : SmokeLauncher.initial(smokeSpec);
            driverWeapons.select(WeaponSelection.clampSlot(tag.getInt("WeaponSlot")));
            ensureMagazines();
        } else if (tag.contains("MagReady")) {
            // A save written before the carousel existed: the single rack becomes the first type.
            TwoStageMagazine.Spec spec = LittleBirdConfig.tankMainGunMagazine();
            mainGunAmmo = MainGunAmmo.of(spec, MainGunAmmoType.AP,
                    new TwoStageMagazine.State[]{
                            new TwoStageMagazine.State(tag.getInt("MagReady"),
                                    tag.getInt("MagReserve"), 0, 0),
                            TwoStageMagazine.initial(spec)});
            coax = AmmoPool.empty(LittleBirdConfig.tankCoaxCapacity());
            coax.refill(tag.getInt("CoaxAmmo"));
            ensureMagazines();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        tag.putFloat("TurretYaw", (float) turret.aimYawDegrees());
        tag.putFloat("TurretPitch", (float) turret.aimPitchDegrees());
        tag.putInt("TurretCooldown", turret.cooldownTicks());
        if (mainGunAmmo != null) {
            tag.putInt("ShellType", mainGunAmmo.selected().ordinal());
            for (MainGunAmmoType type : MainGunAmmoType.values()) {
                tag.putInt("Ammo_" + type.id() + "_Ready", mainGunAmmo.ready(type));
                tag.putInt("Ammo_" + type.id() + "_Reserve", mainGunAmmo.reserve(type));
            }
            tag.putInt("CoaxAmmo", coax.rounds());
            tag.putInt("WeaponSlot", driverWeapons.selected());
            if (smoke != null) {
                tag.putInt("SmokeCharges", smoke.charges());
                tag.putInt("SmokeCooldown", smoke.cooldownTicks());
                tag.putInt("SmokeActive", smoke.activeTicks());
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private static LittleBirdBallistics.Vector3 vector(Vec3 value) {
        return new LittleBirdBallistics.Vector3(value.x, value.y, value.z);
    }

    private static Vec3 vec(LittleBirdBallistics.Vector3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }
}
