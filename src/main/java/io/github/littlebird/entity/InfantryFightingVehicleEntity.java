package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.GroundViewMath;
import io.github.littlebird.entity.ground.GroundDrive;
import io.github.littlebird.entity.ground.GroundMover;
import io.github.littlebird.entity.ground.GroundSuspension;
import io.github.littlebird.entity.ground.GroundVehicleControls;
import io.github.littlebird.entity.ground.GroundWeapons;
import io.github.littlebird.entity.ground.SlopeTracker;
import io.github.littlebird.entity.seat.GroundSeat;
import io.github.littlebird.entity.weapon.AmmoPool;
import io.github.littlebird.entity.weapon.AmmoSupply;
import io.github.littlebird.entity.weapon.ChainGunTurret;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.TwoStageMagazine;
import io.github.littlebird.registry.ModItems;
import java.util.List;
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
 * EBAA Wildcat infantry fighting vehicle: a four seat, wheeled ground vehicle.
 *
 * <p>It is not a smaller tank and shares no weapon code with one. Its turret is the pack's
 * existing 30mm {@link ChainGunTurret} - fast, heat limited, cheap per round - and its heavy
 * punch is a two round ATGM rail the gunner fires. That is the battlefield role: the IFV wins by
 * rate of fire against infantry and light targets and carries dismounts, while a main battle tank
 * wins the armour duel its autocannon cannot.
 *
 * <p>Motion matches the tank's split (driver's client authoritative, {@link GroundDrive} for the
 * physics) but with the IFV's own handling numbers and seat plan: driver, gunner, two rear
 * passengers who ride and do not operate anything.
 */
public class InfantryFightingVehicleEntity extends Entity
        implements GunshipRider, GroundVehicleControls, io.github.littlebird.client.GroundView {
    public static final float MAX_HEALTH = 90.0F;
    /** Client only: whether this hull is shown through its turret interior camera. */
    private boolean interiorView;

    private static final float FALL_GRAVITY = 0.08F;
    private static final double MAX_FALL_SPEED = 1.0D;
    /**
     * Grounded hulls keep a small downward request: Entity.move clears onGround on a strictly zero
     * vertical move, and a flickering onGround then stalled the step assist and the grade probe on
     * alternate ticks.
     */
    private static final double GROUND_CONTACT_PROBE = 0.05D;
    private static final float AUTOCANNON_SPLASH_RADIUS = 1.0F;
    private static final float SPLASH_DAMAGE_FRACTION = 0.5F;

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(InfantryFightingVehicleEntity.class,
                    EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURRET_YAW =
            SynchedEntityData.defineId(InfantryFightingVehicleEntity.class,
                    EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURRET_PITCH =
            SynchedEntityData.defineId(InfantryFightingVehicleEntity.class,
                    EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_AUTOCANNON_AMMO =
            SynchedEntityData.defineId(InfantryFightingVehicleEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATGM_READY =
            SynchedEntityData.defineId(InfantryFightingVehicleEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATGM_RESERVE =
            SynchedEntityData.defineId(InfantryFightingVehicleEntity.class,
                    EntityDataSerializers.INT);

    private final AircraftDestruction destruction = new AircraftDestruction();

    private GroundDrive.State clientDrive;
    private int clientControls;

    private ChainGunTurret.State turret = ChainGunTurret.State.initial();
    private AmmoPool autocannon;
    private TwoStageMagazine.Spec atgmSpec;
    private TwoStageMagazine.State atgm;
    private int atgmCooldown;
    private boolean driverTrigger;
    private boolean gunnerTrigger;
    private int supplyTick;
    private final GroundSuspension suspension = new GroundSuspension(
            InfantryFightingVehicleGeometry.axleForwardUnits(),
            InfantryFightingVehicleGeometry.WHEEL_LATERAL_UNITS, 0.18D);
    private final SlopeTracker slopeTracker = new SlopeTracker();

    public InfantryFightingVehicleEntity(
            EntityType<? extends InfantryFightingVehicleEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_TURRET_YAW, 0.0F);
        entityData.define(DATA_TURRET_PITCH, 0.0F);
        entityData.define(DATA_AUTOCANNON_AMMO, 0);
        entityData.define(DATA_ATGM_READY, 0);
        entityData.define(DATA_ATGM_RESERVE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickClient();
            GroundMover.updateSuspension(this, suspension);
            if (clientDrive != null) {
                setXRot(-suspension.pose(1.0F).pitchDegrees());
            }
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
        tickTurret();
        tickAmmoSupply();
        if (getControllingPassenger() == null && !isRemoved()) {
            applyUnpilotedMotion();
        }
    }

    private void ensureMagazines() {
        if (atgmSpec != null) {
            return;
        }
        atgmSpec = LittleBirdConfig.ifvAtgmMagazine();
        atgm = TwoStageMagazine.initial(atgmSpec);
        autocannon = AmmoPool.full(LittleBirdConfig.ifvAutocannonCapacity());
        publishAmmo();
    }

    private void publishAmmo() {
        entityData.set(DATA_AUTOCANNON_AMMO, autocannon.rounds());
        entityData.set(DATA_ATGM_READY, atgm.ready());
        entityData.set(DATA_ATGM_RESERVE, atgm.reserve());
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
                ? ChainGunTurret.parkedAim(getYRot(), getXRot(), stops())
                : ChainGunTurret.desiredAim(driver.getYRot(), driver.getXRot(),
                        stops());
        ChainGunTurret.Step step = ChainGunTurret.advance(turret, desired,
                driverTrigger && driver != null, stops());
        turret = step.state();
        entityData.set(DATA_TURRET_YAW, (float) turret.aimYawDeg());
        entityData.set(DATA_TURRET_PITCH, (float) turret.aimPitchDeg());

        if (step.fire() && driver != null && autocannon.fire(1)
                && level() instanceof ServerLevel server) {
            publishAmmo();
            fireAutocannon(server, driver);
        }

        if (atgmCooldown > 0) {
            atgmCooldown--;
        }
        if (gunner != null && gunnerTrigger && atgmCooldown <= 0 && atgm.ready() > 0
                && level() instanceof ServerLevel server) {
            atgm = TwoStageMagazine.fire(atgm, atgmSpec);
            atgmCooldown = LittleBirdConfig.ifvAtgmFireIntervalTicks();
            publishAmmo();
            fireAtgm(server, gunner);
        }
    }

    /**
     * The mount's stops measured against the hull, not the horizon: 85 degrees of elevation and 10 of
     * depression are rig limits, so a hull nose-down on a slope can point the gun further down in
     * absolute terms and still be inside them. The chain gun clamp works on absolute pitch, so the
     * stops are shifted by the hull's own pitch for this tick.
     */
    private ChainGunTurret.Stops stops() {
        float hullPitch = getXRot();
        return new ChainGunTurret.Stops(
                ChainGunTurret.Stops.WILDCAT.upDeg() - hullPitch,
                ChainGunTurret.Stops.WILDCAT.downDeg() + hullPitch);
    }

    /** One 30mm round, spread by heat exactly like the Apache's chain gun. */
    private void fireAutocannon(ServerLevel server, Player driver) {
        Vec3 aim = vec(ChainGunTurret.aimDirection(turret.aimYawDeg(), turret.aimPitchDeg(),
                stops()));
        Vec3 direction = vec(LittleBirdBallistics.spreadDirection(
                vector(aim), ChainGunTurret.spreadDegrees(turret.heat()),
                random.nextDouble(), random.nextDouble()));
        GroundWeapons.fireHitscan(server, this, driver, muzzle(), direction,
                io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.IFV_AUTOCANNON,
                AUTOCANNON_SPLASH_RADIUS, SPLASH_DAMAGE_FRACTION, 0.9F);
    }

    /** One ATGM: the gunner's heavy punch, down the turret line with a two block blast. */
    private void fireAtgm(ServerLevel server, Player gunner) {
        GroundWeapons.fireHitscan(server, this, gunner, muzzle(), turretDirection(),
                io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.IFV_ATGM,
                LittleBirdConfig.ifvAtgmSplashRadius(), SPLASH_DAMAGE_FRACTION, 0.7F);
    }

    private Vec3 turretDirection() {
        return vec(ChainGunTurret.aimDirection(turret.aimYawDeg(), turret.aimPitchDeg(),
                stops()));
    }

    private Vec3 muzzle() {
        Vec3 hullForward = Vec3.directionFromRotation(0.0F, getYRot());
        return vec(ChainGunTurret.muzzleOrigin(vector(position()), vector(hullForward),
                new LittleBirdBallistics.Vector3(0.0D, 1.0D, 0.0D),
                InfantryFightingVehicleGeometry.turretPivotHeightBlocks(), 0.0D,
                InfantryFightingVehicleGeometry.barrelBlocks(),
                turret.aimYawDeg(), turret.aimPitchDeg(), stops()));
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
        autocannon.refill(supply.bullets(20));
        atgm = TwoStageMagazine.supply(atgm, atgmSpec, supply.missiles(20));
        publishAmmo();
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

    // ------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------

    private void tickClient() {
        if (!(isControlledByLocalInstance() && getControllingPassenger() instanceof Player driver)) {
            clientDrive = null;
            return;
        }
        double grade = slopeTracker.gradeDegrees();

        GroundDrive.State prior = new GroundDrive.State(
                clientDrive == null ? 0.0D : clientDrive.speedBlocksPerTick(), getYRot());
        GroundDrive.Step step = GroundDrive.advance(prior, GroundDrive.throttle(clientControls),
                GroundDrive.steer(clientControls), LittleBirdConfig.ifvDriveSpec(), grade);
        clientDrive = step.state();
        setYRot((float) step.state().yawDegrees());
        setYHeadRot(getYRot());
        double dy = onGround() ? -GROUND_CONTACT_PROBE
                : Math.max(getDeltaMovement().y - FALL_GRAVITY, -MAX_FALL_SPEED);
        Vec3 drive = new Vec3(step.dx(), dy, step.dz());
        GroundMover.moveDrive(this, drive, LittleBirdConfig.ifvStepHeightBlocks(), slopeTracker);
        // Local preview of the turret must obey the mount's stops: writing the raw look here made
        // the HUD aim (and the camera clamp) disagree with the gun the server actually fires, which
        // is what put the reticle off the barrel whenever the barrel ran out of elevation.
        ChainGunTurret.Aim preview = ChainGunTurret.desiredAim(driver.getYRot(),
                driver.getXRot(), stops());
        entityData.set(DATA_TURRET_YAW, (float) preview.yawDeg());
        entityData.set(DATA_TURRET_PITCH, (float) preview.pitchDeg());
    }

    @Override
    public void setClientControls(int controls) {
        this.clientControls = controls;
    }

    @Override
    public void resetClientPrediction() {
        this.clientDrive = null;
        this.clientControls = 0;
        this.slopeTracker.reset();
    }

    /** Local suspension pose used by the renderer. */
    public GroundSuspension suspension() {
        return suspension;
    }

    @Override
    public boolean isInteriorView() {
        return interiorView;
    }

    @Override
    public void setInteriorView(boolean value) {
        this.interiorView = value;
    }

    /** Same rule as the tank: the interior view draws the local driver's own aim, not the synched copy. */
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
                    driver.getViewXRot(partialTick), stops()).pitchDeg();
        }
        return getTurretPitchDeg();
    }

    @Override
    public void applyWeaponInput(Player sender, boolean triggerHeld, boolean reload,
                                int shellDelta, int weaponDelta) {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        if (sender == driver()) {
            driverTrigger = triggerHeld;
        } else if (sender == gunner()) {
            gunnerTrigger = triggerHeld;
        } else {
            return;
        }
        if (reload && atgmSpec != null) {
            atgm = TwoStageMagazine.startReload(atgm, atgmSpec);
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
        ItemStack stack = new ItemStack(ModItems.INFANTRY_FIGHTING_VEHICLE.get());
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
        return InfantryFightingVehicleGeometry.seatHeightBlocks(GroundSeat.IFV_DRIVER);
    }

    /**
     * A hull driven on its own client owns its position. The server's tracking updates still arrive
     * every tick and lag the prediction, so applying them dragged the local hull backwards and let
     * the prediction snap it forward again - read on screen as the model flashing ahead. Remote
     * viewers keep the normal interpolation.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps,
                       boolean teleport) {
        if (level().isClientSide && isControlledByLocalInstance()) {
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int seat = seatOf(passenger);
        double forward = InfantryFightingVehicleGeometry.seatForwardBlocks(seat);
        double lateral = InfantryFightingVehicleGeometry.seatLateralBlocks(seat);
        // Same rule as the tank: the turret interior camera rides the turret's own yaw so the
        // turret shell stays fixed in the lens, while every other camera keeps the hull yaw.
        double yaw = Math.toRadians(GroundViewMath.followYawDeg(
                interiorView, getYRot(), renderTurretYawDeg(1.0F)));
        double dx = forward * -Math.sin(yaw) + lateral * -Math.cos(yaw);
        double dz = forward * Math.cos(yaw) + lateral * -Math.sin(yaw);
        // The same lift the aircraft cockpit uses, so both interiors sit at the same eye height.
        double height = InfantryFightingVehicleGeometry.seatHeightBlocks(seat)
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
        return getPassengers().size() < GroundSeat.IFV_SEATS && passenger instanceof Player;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public int seatOf(Entity passenger) {
        int index = getPassengers().indexOf(passenger);
        return index < 0 ? -1 : GroundSeat.seatOf(index, GroundSeat.IFV_SEATS);
    }

    @Override
    public boolean isDriver(int seat) {
        return GroundSeat.isIfvDriver(seat);
    }

    @Override
    public boolean isOperator(int seat) {
        return GroundSeat.isIfvDriver(seat) || GroundSeat.isIfvGunner(seat);
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
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            // A hull never takes a blast price: warheads settle their damage through
            // VehicleDamageTable (direct hit and the priced splash in WarheadSplash), so a near
            // miss can never out-damage a direct hit. Keep the JDAM check below for its own blast.
            return false;
        }
        if (JdamBombEntity.isWarheadBlast(source)) {
            // The warhead settles its anti-vehicle damage itself; its blast must not stack on it.
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

    // ------------------------------------------------------------------
    // State accessors
    // ------------------------------------------------------------------

    public float getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    @Override
    public float maxHealth() {
        return MAX_HEALTH;
    }

    @Override
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

    public int getAutocannonAmmo() {
        return entityData.get(DATA_AUTOCANNON_AMMO);
    }

    public int getAtgmReady() {
        return entityData.get(DATA_ATGM_READY);
    }

    public int getAtgmReserve() {
        return entityData.get(DATA_ATGM_RESERVE);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setHealth(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        turret = new ChainGunTurret.State(tag.getFloat("TurretYaw"), tag.getFloat("TurretPitch"),
                tag.getInt("TurretHeat"), tag.getDouble("TurretCooldown"), tag.getInt("TurretLock"));
        entityData.set(DATA_TURRET_YAW, (float) turret.aimYawDeg());
        entityData.set(DATA_TURRET_PITCH, (float) turret.aimPitchDeg());
        if (tag.contains("AtgmReady")) {
            atgmSpec = LittleBirdConfig.ifvAtgmMagazine();
            atgm = new TwoStageMagazine.State(tag.getInt("AtgmReady"), tag.getInt("AtgmReserve"),
                    tag.getInt("AtgmReload"), 0);
            autocannon = AmmoPool.empty(LittleBirdConfig.ifvAutocannonCapacity());
            autocannon.refill(tag.getInt("AutocannonAmmo"));
            publishAmmo();
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
        if (atgm != null) {
            tag.putInt("AtgmReady", atgm.ready());
            tag.putInt("AtgmReserve", atgm.reserve());
            tag.putInt("AtgmReload", atgm.reloadTicks());
            tag.putInt("AutocannonAmmo", autocannon.rounds());
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
