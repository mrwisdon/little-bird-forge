package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.GroundViewMath;
import io.github.littlebird.entity.ground.GroundContact;
import io.github.littlebird.entity.ground.GroundDrive;
import io.github.littlebird.entity.ground.GroundMover;
import io.github.littlebird.entity.ground.GroundVehicleControls;
import io.github.littlebird.entity.ground.GroundWeapons;
import io.github.littlebird.entity.ground.HullPose;
import io.github.littlebird.entity.ground.SlopeTracker;
import io.github.littlebird.entity.seat.GroundSeat;
import io.github.littlebird.entity.weapon.AmmoPool;
import io.github.littlebird.entity.weapon.AmmoSupply;
import io.github.littlebird.entity.weapon.ChainGunTurret;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.SmokeWall;
import io.github.littlebird.entity.weapon.TwoStageMagazine;
import io.github.littlebird.registry.ModItems;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
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
 * M2A2 Bradley infantry fighting vehicle: a six seat tracked hull with a two man turret crew and a
 * four man dismount compartment.
 *
 * <p>Its weapons are the real three: a 25mm M242 chain gun fed from a 300 round ready box over a
 * 600 round reserve, a twin TOW launcher that fires the pack's existing guided missile, and a
 * smoke wall laid five blocks in front of the nose. The roof .50 is the second seat's weapon.
 *
 * <p>Seat 0 drives the hull <em>and</em> works the turret - the same split the IFV uses, where the
 * driver's client is authoritative for motion and aims the turret while the second seat owns the
 * secondary weapon. Seat 1 works the roof machine gun. Seats 2-5 ride.
 */
public class BradleyEntity extends Entity
        implements GunshipRider, GroundVehicleControls, io.github.littlebird.client.GroundView,
                MissileSight {
    public static final float MAX_HEALTH = 110.0F;

    /** Driver weapon slots, cycled by the weapon key exactly like the tank's shell carousel. */
    public static final int WEAPON_AUTOCANNON = 0;
    public static final int WEAPON_TOW = 1;
    public static final int WEAPON_SMOKE = 2;
    public static final int WEAPON_SLOTS = 3;

    private static final float FALL_GRAVITY = 0.08F;
    private static final double MAX_FALL_SPEED = 1.0D;
    private static final double GROUND_CONTACT_PROBE = 0.05D;
    /** The TOW flies to where the sight line ends, so it needs a real sight range. */
    private static final float TOW_TRACER_WIDTH = 0.7F;

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURRET_YAW =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURRET_PITCH =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_AUTOCANNON_READY =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AUTOCANNON_RESERVE =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOW_READY =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOW_RESERVE =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_M2_AMMO =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SMOKE_CHARGES =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SMOKE_COOLDOWN =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WEAPON =
            SynchedEntityData.defineId(BradleyEntity.class, EntityDataSerializers.INT);

    private final AircraftDestruction destruction = new AircraftDestruction();

    private GroundDrive.State clientDrive;
    private int clientControls;
    private boolean interiorView;

    private ChainGunTurret.State turret = ChainGunTurret.State.initial();
    private TwoStageMagazine.Spec autocannonSpec;
    private TwoStageMagazine.State autocannon;
    private TwoStageMagazine.Spec towSpec;
    private TwoStageMagazine.State tow;
    private AmmoPool m2;
    private SmokeWall.Spec smokeSpec;
    private SmokeWall.State smoke;
    private int weapon = WEAPON_AUTOCANNON;
    private int towCooldown;
    private int m2Cooldown;
    private boolean driverTrigger;
    private boolean gunnerTrigger;
    private int supplyTick;
    private float hullRollDeg;
    private final HullPose hullPose = new HullPose(60.0D, 35.0D, 4.0D, 0.75D, 0.35D);
    private final SlopeTracker slopeTracker = new SlopeTracker();

    public BradleyEntity(EntityType<? extends BradleyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_TURRET_YAW, 0.0F);
        entityData.define(DATA_TURRET_PITCH, 0.0F);
        entityData.define(DATA_AUTOCANNON_READY, 0);
        entityData.define(DATA_AUTOCANNON_RESERVE, 0);
        entityData.define(DATA_TOW_READY, 0);
        entityData.define(DATA_TOW_RESERVE, 0);
        entityData.define(DATA_M2_AMMO, 0);
        entityData.define(DATA_SMOKE_CHARGES, 0);
        entityData.define(DATA_SMOKE_COOLDOWN, 0);
        entityData.define(DATA_WEAPON, WEAPON_AUTOCANNON);
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

    // ------------------------------------------------------------------ server

    private void tickServer() {
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        ensureMagazines();
        autocannon = TwoStageMagazine.tick(autocannon, autocannonSpec);
        tow = TwoStageMagazine.tick(tow, towSpec);
        smoke = SmokeWall.tick(smoke, smokeSpec);
        if (smoke.cloudActive() && level() instanceof ServerLevel server) {
            emitSmokeWall(server);
        }
        publishSmoke();
        tickTurret();
        tickAmmoSupply();
        if (getControllingPassenger() == null && !isRemoved()) {
            applyUnpilotedMotion();
        }
    }

    private void ensureMagazines() {
        if (autocannonSpec != null) {
            return;
        }
        autocannonSpec = LittleBirdConfig.bradleyAutocannonMagazine();
        autocannon = TwoStageMagazine.initial(autocannonSpec);
        towSpec = LittleBirdConfig.bradleyTowMagazine();
        tow = TwoStageMagazine.initial(towSpec);
        m2 = AmmoPool.full(LittleBirdConfig.bradleyM2Capacity());
        smokeSpec = LittleBirdConfig.bradleySmokeWall();
        smoke = SmokeWall.initial(smokeSpec);
        publishAmmo();
    }

    private void publishAmmo() {
        entityData.set(DATA_AUTOCANNON_READY, autocannon.ready());
        entityData.set(DATA_AUTOCANNON_RESERVE, autocannon.reserve());
        entityData.set(DATA_TOW_READY, tow.ready());
        entityData.set(DATA_TOW_RESERVE, tow.reserve());
        entityData.set(DATA_M2_AMMO, m2.rounds());
    }

    private void publishSmoke() {
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

        ChainGunTurret.Aim desired = driver == null
                ? ChainGunTurret.parkedAim(getYRot(), getXRot())
                : ChainGunTurret.desiredAim(driver.getYRot(), driver.getXRot());
        ChainGunTurret.Step step = ChainGunTurret.advance(turret, desired,
                driverTrigger && driver != null && weapon == WEAPON_AUTOCANNON,
                LittleBirdConfig.bradleyAutocannonFireIntervalTicks());
        turret = step.state();
        entityData.set(DATA_TURRET_YAW, (float) turret.aimYawDeg());
        entityData.set(DATA_TURRET_PITCH, (float) turret.aimPitchDeg());

        if (step.fire() && driver != null && level() instanceof ServerLevel server) {
            TwoStageMagazine.State fired = TwoStageMagazine.fire(autocannon, autocannonSpec);
            if (fired.ready() != autocannon.ready()) {
                autocannon = fired;
                publishAmmo();
                fireAutocannon(server, driver);
            }
        }

        if (towCooldown > 0) {
            towCooldown--;
        }
        if (m2Cooldown > 0) {
            m2Cooldown--;
        }

        if (driver != null && driverTrigger && weapon == WEAPON_TOW && towCooldown <= 0
                && tow.ready() > 0 && level() instanceof ServerLevel server) {
            tow = TwoStageMagazine.fire(tow, towSpec);
            towCooldown = LittleBirdConfig.bradleyTowFireIntervalTicks();
            publishAmmo();
            fireTow(server, driver);
        }

        if (driver != null && driverTrigger && weapon == WEAPON_SMOKE
                && level() instanceof ServerLevel server) {
            SmokeWall.State fired = SmokeWall.fire(smoke, smokeSpec);
            if (fired.charges() != smoke.charges()) {
                smoke = fired;
                publishSmoke();
            }
        }

        if (gunner != null && gunnerTrigger && m2Cooldown <= 0 && m2.fire(1)
                && level() instanceof ServerLevel server) {
            m2Cooldown = LittleBirdConfig.bradleyM2FireIntervalTicks();
            publishAmmo();
            fireRoofGun(server, gunner);
        }
    }

    /**
     * One 25mm round: a real shell entity down the turret's attitude, spread by heat exactly like
     * the pack's other chain guns, carrying a two block blast when it lands. No longer a hitscan -
     * the round flies, and the crew can watch it.
     */
    private void fireAutocannon(ServerLevel server, Player driver) {
        Vec3 aim = vec(ChainGunTurret.aimDirection(turret.aimYawDeg(), turret.aimPitchDeg()));
        Vec3 direction = vec(LittleBirdBallistics.spreadDirection(
                vector(aim), ChainGunTurret.spreadDegrees(turret.heat()),
                random.nextDouble(), random.nextDouble()));
        Vec3 origin = muzzle();
        AutocannonShellEntity round = new AutocannonShellEntity(
                io.github.littlebird.registry.ModEntities.AUTOCANNON_SHELL.get(), server);
        round.setPos(origin.x, origin.y, origin.z);
        round.configure(LittleBirdConfig.bradleyAutocannonDamage(),
                LittleBirdConfig.bradleyAutocannonVehicleDamage(),
                LittleBirdConfig.bradleyAutocannonHeavyDamage(),
                LittleBirdConfig.bradleyAutocannonSplashRadius());
        round.launch(direction, LittleBirdConfig.bradleyAutocannonShellSpeed(), driver);
        server.addFreshEntity(round);
        // The muzzle flash is pushed a little way down the bore: at five rounds a second the
        // flash is what tells the crew the gun is actually running, and a tight bright core at
        // the muzzle reads far harder than a puff of smoke alone. The counts stay tiny because
        // this fires 300 times a minute.
        Vec3 flash = origin.add(direction.scale(0.35D));
        server.sendParticles(ParticleTypes.FLAME, flash.x, flash.y, flash.z, 3,
                0.04D, 0.04D, 0.04D, 0.02D);
        server.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 3,
                0.05D, 0.05D, 0.05D, 0.01D);
        server.playSound(null, getX(), getY(), getZ(),
                io.github.littlebird.registry.ModSounds.GUN_SHOT.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.35F);
    }

    /**
     * One TOW: the pack's existing guided missile, launched from the turret's left launcher down
     * the sight line, exactly the way the Apache flies its own.
     */
    private void fireTow(ServerLevel server, Player driver) {
        Vec3 aim = turretDirection();
        Vec3 left = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
        Vec3 origin = muzzle().add(left.scale(-LittleBirdConfig.bradleyTowLauncherLateralBlocks()));
        ApacheMissileEntity missile = new ApacheMissileEntity(
                io.github.littlebird.registry.ModEntities.APACHE_MISSILE.get(), server);
        missile.setPos(origin.x, origin.y, origin.z);
        // The Bradley's own warhead figures rather than the shared Apache ones.
        missile.configureWarhead(LittleBirdConfig.bradleyTowDamage(),
                LittleBirdConfig.bradleyTowVehicleDamage(),
                LittleBirdConfig.bradleyTowHeavyDamage(),
                LittleBirdConfig.bradleyTowSplashRadius());
        missile.launch(aim, driver, ApacheMissileEntity.aimDepthBlocks(server, origin, aim, this));
        server.addFreshEntity(missile);
        server.playSound(null, getX(), getY(), getZ(),
                io.github.littlebird.registry.ModSounds.ROCKET_LAUNCH.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    /** The roof .50 in the second seat: hitscan down that player's own look line. */
    private void fireRoofGun(ServerLevel server, Player gunner) {
        Vec3 aim = Vec3.directionFromRotation(gunner.getXRot(), gunner.getYRot());
        Vec3 spread = vec(LittleBirdBallistics.spreadDirection(vector(aim),
                LittleBirdConfig.bradleyM2SpreadDegrees(),
                random.nextDouble(), random.nextDouble()));
        GroundWeapons.fireHitscan(server, this, gunner, m2Muzzle(gunner), spread,
                LittleBirdConfig.bradleyM2Damage(),
                LittleBirdConfig.bradleyM2VehicleDamage(),
                0.0F, 0.0F, 0.0F, 0.8F, io.github.littlebird.entity.weapon.DamageType.BULLET);
    }

    /**
     * The turret's sight line, which is what the TOW rides. Taken from the synced turret attitude
     * rather than the driver's own look so the round follows the barrel the player can see.
     */
    @Override
    public Vec3 missileSightDirection() {
        return vec(ChainGunTurret.aimDirection(getTurretYawDeg(), getTurretPitchDeg()));
    }

    /** The launcher mouth: the aim point is measured from the rail, not from the crew's heads. */
    @Override
    public Vec3 missileSightOrigin() {
        return muzzle();
    }

    private Vec3 turretDirection() {
        return vec(ChainGunTurret.aimDirection(turret.aimYawDeg(), turret.aimPitchDeg()));
    }

    private Vec3 muzzle() {
        // The barrel elevates on its trunnion at the mantlet, so the muzzle is the trunnion plus the
        // barrel reach along the gun line. The trunnion turns with the turret, hence the turret's
        // own forward rather than the hull's.
        Vec3 turretForward = Vec3.directionFromRotation(0.0F, (float) turret.aimYawDeg());
        return vec(ChainGunTurret.muzzleOrigin(vector(position()), vector(turretForward),
                new LittleBirdBallistics.Vector3(0.0D, 1.0D, 0.0D),
                BradleyGeometry.gunTrunnionHeightBlocks(),
                BradleyGeometry.gunTrunnionForwardBlocks(),
                BradleyGeometry.barrelReachBlocks(),
                turret.aimYawDeg(), turret.aimPitchDeg()));
    }

    /** Roof gun muzzle: the cupola mount, pushed out along the gunner's own aim. */
    private Vec3 m2Muzzle(Player gunner) {
        double yaw = Math.toRadians(getYRot());
        double forward = BradleyGeometry.toBlocks(BradleyGeometry.M2_MOUNT_FORWARD_UNITS);
        double lateral = BradleyGeometry.toBlocks(BradleyGeometry.M2_MOUNT_LATERAL_UNITS);
        double dx = forward * Math.sin(yaw) + lateral * Math.cos(yaw);
        double dz = forward * Math.cos(yaw) - lateral * Math.sin(yaw);
        Vec3 mount = position()
                .add(dx, BradleyGeometry.m2MountHeightBlocks(), dz);
        return mount.add(Vec3.directionFromRotation(gunner.getXRot(), gunner.getYRot())
                .scale(BradleyGeometry.m2BarrelBlocks()));
    }

    /**
     * A row of charges abreast, five blocks in front of the nose. Minecraft has no volumetric
     * smoke, so the wall is particles laid on the canister line the rules chose.
     */
    private void emitSmokeWall(ServerLevel server) {
        double yaw = Math.toRadians(getYRot());
        double ahead = SmokeWall.wallCenterAheadBlocks(
                BradleyGeometry.hullHalfLengthBlocks(), smokeSpec);
        double[] offsets = SmokeWall.lateralOffsetsBlocks(smokeSpec);
        for (double offset : offsets) {
            for (int i = 0; i < 3; i++) {
                double jitter = (random.nextDouble() - 0.5D) * 0.6D;
                double x = getX() + Math.sin(yaw) * ahead + Math.cos(yaw) * (offset + jitter);
                double z = getZ() + Math.cos(yaw) * ahead - Math.sin(yaw) * (offset + jitter);
                double y = getY() + 0.4D + random.nextDouble() * 1.6D;
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1,
                        0.04D, 0.04D, 0.04D, 0.004D);
            }
        }
    }

    private void tickAmmoSupply() {
        if (++supplyTick < 20) {
            return;
        }
        supplyTick = 0;
        if (!AircraftRearm.crateNearby(this)) {
            return;
        }
        AmmoSupply supply = LittleBirdConfig.ammoSupply();
        autocannon = TwoStageMagazine.supply(autocannon, autocannonSpec, supply.bullets(20));
        tow = TwoStageMagazine.supply(tow, towSpec, supply.missiles(20));
        m2.refill(supply.bullets(20));
        smoke = SmokeWall.supply(smoke, smokeSpec, 1);
        publishAmmo();
        publishSmoke();
    }

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

    // ------------------------------------------------------------------ client

    private void tickClient() {
        if (!(isControlledByLocalInstance() && getControllingPassenger() instanceof Player driver)) {
            clientDrive = null;
            return;
        }
        GroundContact.Sample surface = GroundMover.sampleSurface(this,
                getBbWidth() * 0.5D, getBbWidth() * 0.5D);
        hullPose.update(surface);
        double grade = slopeTracker.gradeDegrees();

        GroundDrive.State prior = new GroundDrive.State(
                clientDrive == null ? 0.0D : clientDrive.speedBlocksPerTick(), getYRot());
        GroundDrive.Step step = GroundDrive.advance(prior, GroundDrive.throttle(clientControls),
                GroundDrive.steer(clientControls), LittleBirdConfig.bradleyDriveSpec(), grade);
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
        GroundMover.stepUp(this, drive, LittleBirdConfig.bradleyStepHeightBlocks());
        slopeTracker.update(onGround() ? getY() - startY : 0.0D,
                onGround() ? requestedDistance : 0.0D);
        setXRot((float) -hullPose.pitchDegrees());
        hullRollDeg = (float) hullPose.rollDegrees();
        entityData.set(DATA_TURRET_YAW, driver.getYRot());
        entityData.set(DATA_TURRET_PITCH, driver.getXRot());
    }

    @Override
    public void setClientControls(int controls) {
        this.clientControls = controls;
    }

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

    @Override
    public boolean isInteriorView() {
        return interiorView;
    }

    @Override
    public void setInteriorView(boolean value) {
        this.interiorView = value;
    }

    /** Same rule as the tank: the interior view draws the local driver's own aim. */
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
            return (float) ChainGunTurret.desiredAim(0.0F,
                    driver.getViewXRot(partialTick)).pitchDeg();
        }
        return getTurretPitchDeg();
    }

    // ----------------------------------------------------------------- weapons

    @Override
    public void applyWeaponInput(Player sender, boolean triggerHeld, boolean reload,
                                int shellDelta, int weaponDelta) {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        if (sender == driver()) {
            driverTrigger = triggerHeld;
            if (weaponDelta != 0) {
                weapon = Math.floorMod(weapon + weaponDelta, WEAPON_SLOTS);
                entityData.set(DATA_WEAPON, weapon);
            }
            if (reload) {
                if (weapon == WEAPON_AUTOCANNON && autocannonSpec != null) {
                    autocannon = TwoStageMagazine.startReload(autocannon, autocannonSpec);
                } else if (weapon == WEAPON_TOW && towSpec != null) {
                    tow = TwoStageMagazine.startReload(tow, towSpec);
                }
                publishAmmo();
            }
        } else if (sender == gunner()) {
            gunnerTrigger = triggerHeld;
        }
    }

    // ------------------------------------------------------------------ riding

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
        ItemStack stack = new ItemStack(ModItems.BRADLEY.get());
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
        return BradleyGeometry.seatHeightBlocks(GroundSeat.BRADLEY_DRIVER);
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int seat = seatOf(passenger);
        double forward = BradleyGeometry.seatForwardBlocks(seat);
        double lateral = BradleyGeometry.seatLateralBlocks(seat);
        double yaw = Math.toRadians(GroundViewMath.followYawDeg(
                interiorView, getYRot(), renderTurretYawDeg(1.0F)));
        double dx = forward * -Math.sin(yaw) + lateral * -Math.cos(yaw);
        double dz = forward * Math.cos(yaw) + lateral * -Math.sin(yaw);
        double height = BradleyGeometry.seatHeightBlocks(seat)
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
        return getPassengers().size() < GroundSeat.BRADLEY_SEATS && passenger instanceof Player;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public int seatOf(Entity passenger) {
        int index = getPassengers().indexOf(passenger);
        return index < 0 ? -1 : GroundSeat.seatOf(index, GroundSeat.BRADLEY_SEATS);
    }

    @Override
    public boolean isDriver(int seat) {
        return GroundSeat.isBradleyDriver(seat);
    }

    @Override
    public boolean isOperator(int seat) {
        return GroundSeat.isBradleyDriver(seat) || GroundSeat.isBradleyGunner(seat);
    }

    public Player driver() {
        return getPassengers().isEmpty() || !(getPassengers().get(0) instanceof Player player)
                ? null : player;
    }

    public Player gunner() {
        return getPassengers().size() < 2 || !(getPassengers().get(1) instanceof Player player)
                ? null : player;
    }

    // ------------------------------------------------------------------ damage

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

    private void destroyVehicle() {
        if (!destruction.beginDestruction()) {
            return;
        }
        ejectCrew();
        if (level() instanceof ServerLevel server) {
            server.explode(this, getX(), getY(), getZ(), 3.0F, Level.ExplosionInteraction.NONE);
        }
        spawnAtLocation(Items.IRON_INGOT, 3 + random.nextInt(4));
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

    // ------------------------------------------------------------------ state

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

    public int getAutocannonReady() {
        return entityData.get(DATA_AUTOCANNON_READY);
    }

    public int getAutocannonReserve() {
        return entityData.get(DATA_AUTOCANNON_RESERVE);
    }

    public int getTowReady() {
        return entityData.get(DATA_TOW_READY);
    }

    public int getTowReserve() {
        return entityData.get(DATA_TOW_RESERVE);
    }

    public int getM2Ammo() {
        return entityData.get(DATA_M2_AMMO);
    }

    public int getSmokeCharges() {
        return entityData.get(DATA_SMOKE_CHARGES);
    }

    public int getSmokeCooldownTicks() {
        return entityData.get(DATA_SMOKE_COOLDOWN);
    }

    public int getWeapon() {
        return entityData.get(DATA_WEAPON);
    }

    public float getAutocannonHeatFraction() {
        return turret.heat() / (float) ChainGunTurret.MAX_HEAT;
    }

    public boolean isAutocannonOverheated() {
        return turret.lockTicks() > 0;
    }

    // ------------------------------------------------------------ persistence

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setHealth(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        turret = new ChainGunTurret.State(tag.getFloat("TurretYaw"), tag.getFloat("TurretPitch"),
                tag.getInt("TurretHeat"), tag.getDouble("TurretCooldown"), tag.getInt("TurretLock"));
        entityData.set(DATA_TURRET_YAW, (float) turret.aimYawDeg());
        entityData.set(DATA_TURRET_PITCH, (float) turret.aimPitchDeg());
        weapon = tag.contains("Weapon") ? tag.getInt("Weapon") : WEAPON_AUTOCANNON;
        entityData.set(DATA_WEAPON, weapon);
        if (tag.contains("AutocannonReady")) {
            autocannonSpec = LittleBirdConfig.bradleyAutocannonMagazine();
            towSpec = LittleBirdConfig.bradleyTowMagazine();
            smokeSpec = LittleBirdConfig.bradleySmokeWall();
            autocannon = new TwoStageMagazine.State(tag.getInt("AutocannonReady"),
                    tag.getInt("AutocannonReserve"), 0, 0);
            tow = new TwoStageMagazine.State(tag.getInt("TowReady"), tag.getInt("TowReserve"), 0, 0);
            m2 = AmmoPool.empty(LittleBirdConfig.bradleyM2Capacity());
            m2.refill(tag.getInt("M2Ammo"));
            smoke = new SmokeWall.State(tag.getInt("SmokeCharges"),
                    tag.getInt("SmokeCooldown"), 0);
            publishAmmo();
            publishSmoke();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        tag.putFloat("TurretYaw", (float) turret.aimYawDeg());
        tag.putFloat("TurretPitch", (float) turret.aimPitchDeg());
        tag.putInt("TurretHeat", turret.heat());
        tag.putDouble("TurretCooldown", turret.cooldown());
        tag.putInt("TurretLock", turret.lockTicks());
        tag.putInt("Weapon", weapon);
        if (autocannon != null) {
            tag.putInt("AutocannonReady", autocannon.ready());
            tag.putInt("AutocannonReserve", autocannon.reserve());
            tag.putInt("TowReady", tow.ready());
            tag.putInt("TowReserve", tow.reserve());
            tag.putInt("M2Ammo", m2.rounds());
            tag.putInt("SmokeCharges", smoke.charges());
            tag.putInt("SmokeCooldown", smoke.cooldownTicks());
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
