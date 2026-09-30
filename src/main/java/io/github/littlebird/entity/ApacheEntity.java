package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.flight.ApacheFlightProfile;
import io.github.littlebird.entity.flight.FlightConfig;
import io.github.littlebird.entity.flight.FlightControls;
import io.github.littlebird.entity.flight.HelicopterFlightModel;
import io.github.littlebird.entity.flight.HelicopterInput;
import io.github.littlebird.entity.flight.HelicopterInputDamping;
import io.github.littlebird.entity.flight.HelicopterRollDamper;
import io.github.littlebird.entity.flight.HelicopterState;
import io.github.littlebird.entity.seat.ApacheSeat;
import io.github.littlebird.entity.weapon.AmmoSupply;
import io.github.littlebird.entity.weapon.ApachePods;
import io.github.littlebird.entity.weapon.LittleBirdRocketBallistics;
import io.github.littlebird.entity.weapon.ChainGunTurret;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.TwoStageMagazine;
import io.github.littlebird.network.LittleBirdControlPacket;
import io.github.littlebird.network.LittleBirdCrashPacket;
import io.github.littlebird.network.LittleBirdImpactPacket;
import io.github.littlebird.network.LittleBirdTracerPacket;
import io.github.littlebird.network.ModNetwork;
import io.github.littlebird.registry.ModEntities;
import io.github.littlebird.registry.ModItems;
import io.github.littlebird.registry.ModSounds;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * AH-64 Apache: a two seat gunship. The rear seat flies the aircraft and owns the rocket pods
 * and missiles, the front seat runs the 30mm chain gun on its own turret.
 *
 * Motion follows the Little Bird's model: the pilot's client is authoritative for the airframe
 * (vanilla's ServerboundMoveVehiclePacket carries the pose) and reports the bank angle, which
 * is not part of that packet, on the mod's own channel. Turrets, weapons, ammunition and
 * damage stay server authoritative.
 *
 * Weapons land in the next slices; this class carries the seats, the flight model, the hull and
 * the turret aim plumbing.
 */
public class ApacheEntity extends Entity implements GunshipRider, MissileSight {
    public static final float MAX_HEALTH = 100.0F;
    /** Rear seat weapons. */
    public static final int WEAPON_ROCKETS = 0;
    public static final int WEAPON_MISSILES = 1;

    private static final FlightConfig FLIGHT = ApacheFlightProfile.config();
    private static final float ROLL_SYNC_THRESHOLD_DEG = 0.5F;
    private static final float MAX_SYNCED_ROLL_DEG = 180.0F;
    private static final double BLOCKED_SPEED_KEEP = 0.15D;
    private static final int IMPACT_PACKET_COOLDOWN_TICKS = 10;
    private static final double PILOT_PITCH_REFERENCE_DEG = 90.0D;
    /** Full loadout, used until the weapon slice owns the magazines. */
    // The load is defined once, in the pure magazine class; these read it back so the HUD,
    // the synched data defaults and the NBT fallbacks can never disagree with the weapon.
    /** Rounds the 30mm magazine holds. */
    public static final int CHAIN_GUN_AMMO_CAPACITY = 600;
    public static final int ROCKET_READY_CAPACITY = TwoStageMagazine.rocketBay().readyCapacity();
    public static final int ROCKET_RESERVE_CAPACITY = TwoStageMagazine.rocketBay().reserveCapacity();
    public static final int MISSILE_READY_CAPACITY = TwoStageMagazine.missileRail().readyCapacity();
    public static final int MISSILE_RESERVE_CAPACITY = TwoStageMagazine.missileRail().reserveCapacity();

    // Ammunition rules: 19 + 19 rockets, 1 + 5 missiles, per the design. The reload and
    // resupply times come from the config, so the specs are built in the constructor.
    private final TwoStageMagazine.Spec rocketBay;
    private final TwoStageMagazine.Spec missileRail;
    /** The chain gun is hitscan like the Little Bird's, out to the same 128 blocks. */
    private static final double CHAIN_GUN_RANGE = 128.0D;
    /** Ten rockets a second while the trigger is held: a pod empties in under two seconds. */
    private static final int ROCKET_FIRE_INTERVAL_TICKS = 2;
    private static final int MISSILE_FIRE_INTERVAL_TICKS = 10;
    // The same 70mm rocket, but this aircraft carries 38 of them instead of 14, so each one
    // hits for less; the scale lives in the config.

    private static final EntityDataAccessor<Float> DATA_ROLL_DEG =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ENGINE_ON =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_ROTOR_RPM =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_WEAPON =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SEATS_SWAPPED =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * The barrel's absolute world attitude, not an angle relative to the airframe: the gun
     * points where the gunner aims, so the airframe's own yaw and pitch must not appear in it.
     * Clients turn the absolute attitude into the turret's local swing when they draw the model.
     */
    private static final EntityDataAccessor<Float> DATA_TURRET_AIM_YAW =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURRET_AIM_PITCH =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TURRET_HEAT =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    // Loadout readouts. The magazines that drive them arrive with the weapons slice; until
    // then they hold a full aircraft so the model and HUD have something real to show.
    private static final EntityDataAccessor<Integer> DATA_READY_ROCKETS =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    /** 30mm left in the magazine; only an ammunition crate fills it. */
    private static final EntityDataAccessor<Integer> DATA_CHAIN_AMMO =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RESERVE_ROCKETS =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_READY_MISSILES =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RESERVE_MISSILES =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    // Remaining reload/resupply ticks, so the HUD bars are true progress bars rather than a
    // guess about how long ago a round disappeared.
    private static final EntityDataAccessor<Integer> DATA_ROCKET_RELOAD =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROCKET_SUPPLY =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MISSILE_RELOAD =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MISSILE_SUPPLY =
            SynchedEntityData.defineId(ApacheEntity.class, EntityDataSerializers.INT);

    // Client only prediction state, exactly like the Little Bird.
    private HelicopterState clientState;
    private HelicopterInputDamping.State clientDamping =
            HelicopterInputDamping.State.initial(0.0F);
    private float clientRollAxis;
    private int clientControls;
    private boolean clientEngineRequested = true;
    private float lastSentRollDeg = Float.NaN;
    private boolean lastSentEngineOn;
    private int impactPacketCooldown;
    private int crashPacketCooldown;
    private boolean cockpitView;
    private boolean shoulderView;

    /** Runs the write-off exactly once: see AircraftDestruction for the crash it prevents. */
    private final AircraftDestruction destruction = new AircraftDestruction();

    // Server side turret and gunner intent.
    private ChainGunTurret.State turret = ChainGunTurret.State.initial();
    private TwoStageMagazine.State rocketMagazine;
    private TwoStageMagazine.State missileMagazine;
    private boolean triggerHeld;
    private int fireCooldown;
    private int supplyTick;
    private float gunnerYawDeg;
    private float gunnerPitchDeg;
    private boolean gunnerTriggerHeld;

    public ApacheEntity(EntityType<? extends ApacheEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        // Resupply clocks stay at zero: the ammunition crate is the only source now.
        this.rocketBay = TwoStageMagazine.rocketBay().withTimings(
                LittleBirdConfig.apacheRocketReloadTicks(), 0);
        this.missileRail = TwoStageMagazine.missileRail().withTimings(
                LittleBirdConfig.apacheMissileReloadTicks(), 0);
        this.rocketMagazine = TwoStageMagazine.initial(this.rocketBay);
        this.missileMagazine = TwoStageMagazine.initial(this.missileRail);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ROLL_DEG, 0.0F);
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_ENGINE_ON, false);
        entityData.define(DATA_ROTOR_RPM, 0.0F);
        entityData.define(DATA_WEAPON, WEAPON_ROCKETS);
        entityData.define(DATA_SEATS_SWAPPED, false);
        entityData.define(DATA_TURRET_AIM_YAW, 0.0F);
        entityData.define(DATA_TURRET_AIM_PITCH, 0.0F);
        entityData.define(DATA_TURRET_HEAT, 0);
        entityData.define(DATA_READY_ROCKETS, ROCKET_READY_CAPACITY);
        entityData.define(DATA_RESERVE_ROCKETS, ROCKET_RESERVE_CAPACITY);
        entityData.define(DATA_READY_MISSILES, MISSILE_READY_CAPACITY);
        entityData.define(DATA_RESERVE_MISSILES, MISSILE_RESERVE_CAPACITY);
        entityData.define(DATA_CHAIN_AMMO, CHAIN_GUN_AMMO_CAPACITY);
        entityData.define(DATA_ROCKET_RELOAD, 0);
        entityData.define(DATA_ROCKET_SUPPLY, 0);
        entityData.define(DATA_MISSILE_RELOAD, 0);
        entityData.define(DATA_MISSILE_SUPPLY, 0);
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
    // Seats
    // ------------------------------------------------------------------

    /** Which seat a passenger is in, honouring the seat swap key. */
    public int seatOf(Entity passenger) {
        return ApacheSeat.seatOf(getPassengers().indexOf(passenger), isSeatsSwapped());
    }

    public boolean isSeatsSwapped() {
        return entityData.get(DATA_SEATS_SWAPPED);
    }

    /** The rear seat, or null while nobody is flying. */
    public Player pilot() {
        int index = ApacheSeat.indexOf(ApacheSeat.PILOT, isSeatsSwapped(), getPassengers().size());
        return index >= 0 && getPassengers().get(index) instanceof Player player ? player : null;
    }

    /** The front seat, or null when the gunner has not boarded. */
    public Player gunner() {
        int index = ApacheSeat.indexOf(ApacheSeat.GUNNER, isSeatsSwapped(), getPassengers().size());
        return index >= 0 && getPassengers().get(index) instanceof Player player ? player : null;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return pilot();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < ApacheSeat.SEATS && passenger instanceof Player;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    /** Server: the seat switch key. Flips which passenger sits where. */
    public void switchSeats(Player sender) {
        if (level().isClientSide || isRemoved() || !hasPassenger(sender)) {
            return;
        }
        entityData.set(DATA_SEATS_SWAPPED, !isSeatsSwapped());
    }

    /** Server: validated gunner aim report from the front seat. */
    public void applyGunnerAim(Player sender, float yawDeg, float pitchDeg, boolean triggerHeld) {
        if (level().isClientSide || isRemoved() || gunner() != sender) {
            return;
        }
        if (Float.isFinite(yawDeg)) {
            this.gunnerYawDeg = yawDeg;
        }
        if (Float.isFinite(pitchDeg)) {
            this.gunnerPitchDeg = pitchDeg;
        }
        this.gunnerTriggerHeld = triggerHeld;
    }

    /** Absolute world yaw the barrel points at, independent of the airframe's own heading. */
    public float getTurretAimYawDeg() {
        return entityData.get(DATA_TURRET_AIM_YAW);
    }

    /** Absolute world pitch the barrel points at, Minecraft convention (positive is down). */
    public float getTurretAimPitchDeg() {
        return entityData.get(DATA_TURRET_AIM_PITCH);
    }

    /** Barrel bearing relative to the nose, in +/-180 - what the front seat panel reads out. */
    public float getTurretRelativeBearingDeg() {
        return HelicopterFlightModel.wrapDegrees(getTurretAimYawDeg() - getYRot());
    }

    public int getTurretHeat() {
        return entityData.get(DATA_TURRET_HEAT);
    }

    /** 0..1 heat of the chain gun barrel, for the HUD and the glow pass. */
    public float getChainGunHeatFraction() {
        return Math.max(0.0F, Math.min(1.0F, getTurretHeat() / (float) ChainGunTurret.MAX_HEAT));
    }

    public int getChainAmmo() {
        return entityData.get(DATA_CHAIN_AMMO);
    }

    public int getReadyRockets() {
        return entityData.get(DATA_READY_ROCKETS);
    }

    public int getReserveRockets() {
        return entityData.get(DATA_RESERVE_ROCKETS);
    }

    public int getReadyMissiles() {
        return entityData.get(DATA_READY_MISSILES);
    }

    public int getReserveMissiles() {
        return entityData.get(DATA_RESERVE_MISSILES);
    }

    public void setRocketLoadout(int ready, int reserve) {
        entityData.set(DATA_READY_ROCKETS, clampLoadout(ready, ROCKET_READY_CAPACITY));
        entityData.set(DATA_RESERVE_ROCKETS, clampLoadout(reserve, ROCKET_RESERVE_CAPACITY));
    }

    public void setMissileLoadout(int ready, int reserve) {
        entityData.set(DATA_READY_MISSILES, clampLoadout(ready, MISSILE_READY_CAPACITY));
        entityData.set(DATA_RESERVE_MISSILES, clampLoadout(reserve, MISSILE_RESERVE_CAPACITY));
    }

    private static int clampLoadout(int value, int capacity) {
        return Math.max(0, Math.min(capacity, value));
    }

    public int getRocketReloadTicks() {
        return entityData.get(DATA_ROCKET_RELOAD);
    }

    public int getRocketResupplyTicks() {
        return entityData.get(DATA_ROCKET_SUPPLY);
    }

    public int getMissileReloadTicks() {
        return entityData.get(DATA_MISSILE_RELOAD);
    }

    public int getMissileResupplyTicks() {
        return entityData.get(DATA_MISSILE_SUPPLY);
    }

    /** 0..1 fill of the reload bar: how far the reserve-to-ready transfer has got. */
    public float getRocketReloadFraction() {
        return fraction(getRocketReloadTicks(), LittleBirdConfig.apacheRocketReloadTicks());
    }

    /** 0..1 fill of the automatic reserve resupply bar. */
    public float getRocketResupplyFraction() {
        return fraction(getRocketResupplyTicks(), LittleBirdConfig.apacheRocketResupplyTicks());
    }

    public float getMissileReloadFraction() {
        return fraction(getMissileReloadTicks(), LittleBirdConfig.apacheMissileReloadTicks());
    }

    public float getMissileResupplyFraction() {
        return fraction(getMissileResupplyTicks(), LittleBirdConfig.apacheMissileResupplyTicks());
    }

    /** A timer that is idle (0) reads as empty; a running one fills as it counts down. */
    private static float fraction(int remainingTicks, int totalTicks) {
        if (remainingTicks <= 0 || totalTicks <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, 1.0F - remainingTicks / (float) totalTicks));
    }

    /** Client only: true while the local crew member is in a seat view. */
    public boolean isCockpitView() {
        return cockpitView;
    }

    /** Client only: switches between the exterior and the seat camera. */
    public void setCockpitView(boolean value) {
        this.cockpitView = value;
    }

    /** Client only: true while the over-the-shoulder third person camera is held. */
    public void setShoulderView(boolean value) {
        this.shoulderView = value;
    }

    private boolean isShoulderCamera() {
        return shoulderView && !cockpitView && level() != null && level().isClientSide;
    }

    /** Server side: true while the front seat is holding the chain gun trigger. */
    public boolean isGunnerTriggerHeld() {
        return gunnerTriggerHeld;
    }

    // ------------------------------------------------------------------
    // Riding
    // ------------------------------------------------------------------

    /** Right-click to board, like a boat: the first player takes the rear seat. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            // Sneaking with an empty hand packs the airframe up instead of boarding it.
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

    /**
     * Sneak and an empty hand packs the airframe back into an item, carrying its health, so a
     * damaged aircraft can be carried home and repaired instead of being abandoned.
     */
    private void packInto(Player player) {
        ItemStack stack = new ItemStack(ModItems.APACHE.get());
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
        return ApacheGeometry.seatHeightBlocks(ApacheSeat.PILOT);
    }

    /**
     * Each seat gets its own spot: the pilot sits higher and behind the gunner, which is what
     * makes the tandem cockpit read from outside as well as in.
     */
    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int seat = seatOf(passenger);
        if (isShoulderCamera() && seat == ApacheSeat.PILOT) {
            // Third person orbits the hidden rider, so shifting the rider sideways moves the
            // camera the same way and the airframe slides off centre. Only the pilot gets it:
            // the front seat is aiming a turret, not framing a shot.
            LittleBirdCameraMath.Offset offset = LittleBirdCameraMath.shoulderOffset(
                    getYRot(),
                    LittleBirdConfig.shoulderOffsetBlocks(),
                    LittleBirdConfig.shoulderHeightBlocks());
            moveFunction.accept(passenger,
                    getX() + offset.x(),
                    getY() + ApacheGeometry.seatHeightBlocks(seat)
                            + passenger.getMyRidingOffset() + offset.y(),
                    getZ() + offset.z());
            return;
        }
        double forward = ApacheGeometry.seatForwardBlocks(seat);
        double yaw = Math.toRadians(getYRot());
        moveFunction.accept(passenger,
                getX() - Math.sin(yaw) * forward,
                getY() + ApacheGeometry.seatHeightBlocks(seat) + passenger.getMyRidingOffset(),
                getZ() + Math.cos(yaw) * forward);
    }

    // ------------------------------------------------------------------
    // Client flight
    // ------------------------------------------------------------------

    public void setClientControls(int controls) {
        this.clientControls = controls;
    }

    public void setClientEngineRequested(boolean requested) {
        this.clientEngineRequested = requested;
    }

    /** Client only: forgets prediction state when this aircraft is left behind. */
    public void resetClientPrediction() {
        this.clientState = null;
        this.clientDamping = HelicopterInputDamping.State.initial(0.0F);
        this.clientRollAxis = 0.0F;
        this.clientControls = 0;
        this.clientEngineRequested = true;
        this.lastSentRollDeg = Float.NaN;
        this.lastSentEngineOn = false;
        this.crashPacketCooldown = 0;
        this.shoulderView = false;
    }

    private void tickClient() {
        if (isControlledByLocalInstance() && getControllingPassenger() instanceof Player pilot) {
            applyLocalFlight(pilot);
        } else {
            tickRemoteInterpolation();
        }
    }

    private void tickRemoteInterpolation() {
        // Remote clients see the vanilla pose updates; nothing to interpolate here yet.
    }

    /** Steps the flight model on the pilot's client and applies the result. */
    private void applyLocalFlight(Player pilot) {
        HelicopterState state = clientState == null
                ? HelicopterState.initial(pilot.getYRot())
                : clientState;
        state = state.withEngineOn(clientEngineRequested);
        FlightConfig flight = FLIGHT;

        double bankTurn = HelicopterFlightModel.bankedTurnDegPerTick(
                Math.hypot(state.vx(), state.vz()), state.rollDeg(),
                HelicopterFlightModel.liftScale(state.rotorRpm(), flight), flight);
        if (Math.abs(bankTurn) > 1.0E-9D) {
            pilot.setYRot(HelicopterFlightModel.wrapDegrees(
                    pilot.getYRot() + (float) bankTurn));
            clientDamping = HelicopterInputDamping.rotateYaw(clientDamping, (float) bankTurn);
        }

        HelicopterInput rawInput = new HelicopterInput(
                pilot.getYRot(), pitchAxis(pilot),
                FlightControls.rollAxis(clientControls),
                FlightControls.collectiveAxis(clientControls), onGround());
        clientDamping = HelicopterInputDamping.step(clientDamping, rawInput, flight);
        HelicopterInput smoothed = HelicopterInputDamping.apply(rawInput, clientDamping);
        clientRollAxis = HelicopterRollDamper.step(clientRollAxis, smoothed.rollInput(), flight);
        HelicopterInput input = new HelicopterInput(smoothed.yawDeg(), smoothed.pitchInput(),
                clientRollAxis, smoothed.collectiveInput(), smoothed.onGround());
        HelicopterState next = HelicopterFlightModel.step(state, input, flight);

        setYRot(next.yawDeg());
        setXRot(next.pitchDeg());

        Vec3 delta = new Vec3(next.vx(), next.vy(), next.vz());
        setDeltaMovement(delta);
        move(MoverType.SELF, delta);

        double impactSpeed = Math.sqrt(next.vx() * next.vx() + next.vz() * next.vz());
        if (horizontalCollision && impactSpeed > FLIGHT.crashSpeedThreshold
                && impactPacketCooldown <= 0) {
            ModNetwork.CHANNEL.sendToServer(new LittleBirdImpactPacket((float) impactSpeed));
            impactPacketCooldown = IMPACT_PACKET_COOLDOWN_TICKS;
        }
        if (impactPacketCooldown > 0) {
            --impactPacketCooldown;
        }
        if (crashPacketCooldown > 0) {
            --crashPacketCooldown;
        }

        double vx = next.vx();
        double vy = next.vy();
        if (horizontalCollision) {
            vx *= BLOCKED_SPEED_KEEP;
            next = new HelicopterState(next.yawDeg(), next.pitchDeg(), next.rollDeg(),
                    vx, vy, next.vz() * BLOCKED_SPEED_KEEP, next.rotorRpm(), next.engineOn());
        }
        if (verticalCollision && vy < 0.0D) {
            if (crashPacketCooldown <= 0
                    && HelicopterFlightModel.isDiveCrash(vy, next.pitchDeg(), flight)) {
                ModNetwork.CHANNEL.sendToServer(new LittleBirdCrashPacket());
                crashPacketCooldown = IMPACT_PACKET_COOLDOWN_TICKS;
            }
            vy = 0.0D;
            next = new HelicopterState(next.yawDeg(), next.pitchDeg(), next.rollDeg(),
                    next.vx(), vy, next.vz(), next.rotorRpm(), next.engineOn());
        }
        clientState = next;
        syncControlToServer(next.rollDeg(), next.engineOn());
    }

    private static float pitchAxis(Player pilot) {
        return (float) (pilot.getXRot() / PILOT_PITCH_REFERENCE_DEG);
    }

    private void syncControlToServer(float rollDeg, boolean engineOn) {
        if (!Float.isNaN(lastSentRollDeg)
                && Math.abs(rollDeg - lastSentRollDeg) < ROLL_SYNC_THRESHOLD_DEG
                && engineOn == lastSentEngineOn) {
            return;
        }
        lastSentRollDeg = rollDeg;
        lastSentEngineOn = engineOn;
        ModNetwork.CHANNEL.sendToServer(new LittleBirdControlPacket(rollDeg, engineOn));
    }

    // ------------------------------------------------------------------
    // Server tick
    // ------------------------------------------------------------------

    private void tickServer() {
        // The crew is protected while they are strapped in: see LittleBirdRiderProtection.
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        setEngineOn(true);
        tickRotor();
        tickTurret();
        tickMagazines();
        if (level() instanceof ServerLevel server) {
            tickWeapons(server);
        }
        if (getControllingPassenger() == null) {
            applyUnpilotedMotion();
        }
    }

    /**
     * Nobody is flying. With a crew still aboard the rotor holds the aircraft, so a lone pilot
     * switching to the gunner seat is not dropped out of the sky; an empty airframe falls.
     */
    private void applyUnpilotedMotion() {
        Vec3 motion = getDeltaMovement();
        if (getPassengers().isEmpty() || !isEngineOn() || getRotorRpm() < 0.5F) {
            if (onGround()) {
                // Parked: an airframe standing on the ground has nothing left to fall through,
                // and letting gravity keep pulling would creep it downhill.
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            Vec3 falling = new Vec3(motion.x, Math.max(motion.y - FLIGHT.gravity,
                    -FLIGHT.maxFallSpeed), motion.z);
            setDeltaMovement(falling);
            move(MoverType.SELF, falling);
            return;
        }
        Vec3 hovering = new Vec3(motion.x * 0.85D,
                motion.y * 0.80D - FLIGHT.hoverSinkAccel, motion.z * 0.85D);
        setDeltaMovement(hovering);
        move(MoverType.SELF, hovering);
    }

    private void tickRotor() {
        float target = isEngineOn() ? 1.0F : 0.0F;
        float rpm = (float) HelicopterFlightModel.approach(
                getRotorRpm(), target, FLIGHT.rotorSpoolRate);
        entityData.set(DATA_ROTOR_RPM, Math.max(0.0F, Math.min(1.0F, rpm)));
    }

    /**
     * Slews the chain gun onto wherever the front seat is looking. The gunner's look direction
     * *is* the barrel's direction: no airframe term is mixed in, so the pilot cannot drag the gun
     * off the target and an unpiloted aircraft still has a fully usable chin gun. With nobody in
     * the front seat the barrel parks on the nose so the airframe still reads correctly.
     */
    private void tickTurret() {
        Player gunner = gunner();
        if (gunner == null) {
            gunnerTriggerHeld = false;
        }
        ChainGunTurret.Aim desired = gunner == null
                ? ChainGunTurret.parkedAim(getYRot(), getXRot())
                : ChainGunTurret.desiredAim(gunnerYawDeg, gunnerPitchDeg);
        ChainGunTurret.Step step = ChainGunTurret.advance(
                turret, desired, gunnerTriggerHeld && gunner != null);
        turret = step.state();
        entityData.set(DATA_TURRET_AIM_YAW, (float) turret.aimYawDeg());
        entityData.set(DATA_TURRET_AIM_PITCH, (float) turret.aimPitchDeg());
        entityData.set(DATA_TURRET_HEAT, turret.heat());
        if (step.fire() && gunner != null && getChainAmmo() > 0
                && level() instanceof ServerLevel server) {
            fireChainGun(server, gunner);
            entityData.set(DATA_CHAIN_AMMO, getChainAmmo() - 1);
        }
    }

    /**
     * One 30mm round: hitscan along the barrel's own attitude, with a one block splash at the
     * impact point. Damage is priced by target, so an airframe costs three points a round while a
     * player takes the full eight.
     */
    private void fireChainGun(ServerLevel server, Player gunner) {
        Vec3 aim = vec(ChainGunTurret.aimDirection(
                turret.aimYawDeg(), turret.aimPitchDeg()));
        Vec3 forward = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 up = Vec3.directionFromRotation(getXRot() - 90.0F, getYRot());
        Vec3 muzzle = vec(ChainGunTurret.muzzleOrigin(
                vector(position()), vector(forward), vector(up),
                ApacheGeometry.turretPivotHeightBlocks(),
                ApacheGeometry.turretPivotForwardBlocks(),
                ApacheGeometry.chainGunBarrelBlocks(),
                turret.aimYawDeg(), turret.aimPitchDeg()));
        Vec3 direction = vec(LittleBirdBallistics.spreadDirection(
                vector(aim), ChainGunTurret.spreadDegrees(turret.heat()),
                random.nextDouble(), random.nextDouble()));
        Vec3 far = muzzle.add(direction.scale(CHAIN_GUN_RANGE));

        BlockHitResult blockHit = server.clip(new ClipContext(
                muzzle, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 impact = blockHit.getType() == HitResult.Type.MISS ? far : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(server, this, muzzle, impact,
                getBoundingBox().expandTowards(direction.scale(muzzle.distanceTo(impact))).inflate(1.0D),
                candidate -> candidate != this && candidate != gunner
                        && candidate.isAlive() && !candidate.isSpectator());
        if (entityHit != null) {
            impact = entityHit.getLocation();
            applyChainGunDamage(server, gunner, entityHit.getEntity(),
                    LittleBirdConfig.apacheChainGunDamage(),
                    LittleBirdConfig.apacheChainGunAirframeDamage());
        }
        splashChainGun(server, gunner, impact,
                entityHit == null ? null : entityHit.getEntity());

        int impactType = entityHit != null
                ? LittleBirdTracerPacket.IMPACT_ENTITY
                : blockHit.getType() == HitResult.Type.MISS
                        ? LittleBirdTracerPacket.IMPACT_MISS
                        : LittleBirdTracerPacket.IMPACT_BLOCK;
        ModNetwork.sendTracer(this, new LittleBirdTracerPacket(
                muzzle.x, muzzle.y, muzzle.z, impact.x, impact.y, impact.z, impactType, 1));
        server.sendParticles(ParticleTypes.SMOKE,
                muzzle.x, muzzle.y, muzzle.z, 1, 0.03D, 0.03D, 0.03D, 0.004D);
        server.playSound(null, getX(), getY(), getZ(), ModSounds.GUN_SHOT.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 0.75F);
    }

    /** Small blast around the impact: half the round's damage, falling off with distance. */
    private void splashChainGun(ServerLevel server, Player gunner, Vec3 impact, Entity directHit) {
        double radius = LittleBirdConfig.apacheChainGunSplashRadius();
        if (radius <= 0.0D) {
            return;
        }
        AABB blast = new AABB(impact, impact).inflate(radius);
        for (Entity nearby : server.getEntities(this, blast,
                candidate -> candidate != directHit && candidate != this && candidate != gunner
                        && candidate.isAlive() && !candidate.isSpectator())) {
            double distance = nearby.position().distanceTo(impact);
            double falloff = Math.max(0.0D, 1.0D - distance / radius);
            float damage = LittleBirdBallistics.damageTo(
                    io.github.littlebird.entity.weapon.DamageType.SHELL,
                    AirframeTargets.armorClass(nearby),
                    new LittleBirdBallistics.WeaponDamage(
                            LittleBirdConfig.apacheChainGunDamage(),
                            LittleBirdConfig.apacheChainGunAirframeDamage(),
                            LittleBirdConfig.apacheGunHeavyDamage()))
                    * (float) (ChainGunTurret.SPLASH_DAMAGE_FRACTION * falloff);
            if (damage > 0.0F) {
                applyChainGunDamage(server, gunner, nearby, damage, damage);
            }
        }
    }

    private void applyChainGunDamage(
            ServerLevel server, Player gunner, Entity target, float normal, float airframe) {
        float damage = LittleBirdBallistics.damageTo(
                io.github.littlebird.entity.weapon.DamageType.SHELL,
                AirframeTargets.armorClass(target),
                new LittleBirdBallistics.WeaponDamage(normal, airframe,
                        LittleBirdConfig.apacheGunHeavyDamage()));
        if (damage <= 0.0F) {
            return;
        }
        boolean applied = target.hurt(server.damageSources().playerAttack(gunner), damage);
        if (applied && gunner instanceof ServerPlayer serverPlayer) {
            HitFeedback.report(serverPlayer, target);
        }
    }

    private static boolean isAirframe(Entity entity) {
        return AirframeTargets.isAirframe(entity);
    }

    private static LittleBirdBallistics.Vector3 vector(Vec3 value) {
        return new LittleBirdBallistics.Vector3(value.x, value.y, value.z);
    }

    private static Vec3 vec(LittleBirdBallistics.Vector3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }

    // ------------------------------------------------------------------
    // Weapons (server authoritative)
    // ------------------------------------------------------------------

    /** Advances both magazines and republishes what the crew can fire. */
    /**
     * Ammunition comes back from one place only: an ammunition crate parked next to the
     * aircraft. This is checked once a second, in the same place the magazines advance.
     */
    private void tickAmmoSupply() {
        if (++supplyTick < 20) {
            return;
        }
        supplyTick = 0;
        if (!AircraftRearm.crateNearby(this)) {
            return;
        }
        AmmoSupply supply = LittleBirdConfig.ammoSupply();
        int ammo = Math.min(CHAIN_GUN_AMMO_CAPACITY, getChainAmmo() + supply.bullets(20));
        if (ammo != getChainAmmo()) {
            entityData.set(DATA_CHAIN_AMMO, ammo);
        }
        rocketMagazine = TwoStageMagazine.supply(rocketMagazine, rocketBay, supply.rockets(20));
        missileMagazine = TwoStageMagazine.supply(missileMagazine, missileRail,
                supply.missiles(20));
        setRocketLoadout(rocketMagazine.ready(), rocketMagazine.reserve());
        setMissileLoadout(missileMagazine.ready(), missileMagazine.reserve());
    }

    private void tickMagazines() {
        rocketMagazine = TwoStageMagazine.tick(rocketMagazine, rocketBay);
        missileMagazine = TwoStageMagazine.tick(missileMagazine, missileRail);
        setRocketLoadout(rocketMagazine.ready(), rocketMagazine.reserve());
        setMissileLoadout(missileMagazine.ready(), missileMagazine.reserve());
        entityData.set(DATA_ROCKET_RELOAD, rocketMagazine.reloadTicks());
        entityData.set(DATA_ROCKET_SUPPLY, rocketMagazine.resupplyTicks());
        entityData.set(DATA_MISSILE_RELOAD, missileMagazine.reloadTicks());
        entityData.set(DATA_MISSILE_SUPPLY, missileMagazine.resupplyTicks());
        tickAmmoSupply();
    }

    private void tickWeapons(ServerLevel server) {
        if (fireCooldown > 0) {
            --fireCooldown;
        }
        Player pilot = pilot();
        if (pilot == null || !triggerHeld || fireCooldown > 0) {
            if (pilot == null) {
                triggerHeld = false;
            }
            return;
        }
        if (getWeapon() == WEAPON_MISSILES) {
            if (missileMagazine.ready() <= 0) {
                return;
            }
            launchMissile(server, pilot);
            missileMagazine = TwoStageMagazine.fire(missileMagazine, missileRail);
            fireCooldown = MISSILE_FIRE_INTERVAL_TICKS;
            return;
        }
        if (rocketMagazine.ready() <= 0) {
            return;
        }
        launchRocket(server, pilot);
        rocketMagazine = TwoStageMagazine.fire(rocketMagazine, rocketBay);
        fireCooldown = ROCKET_FIRE_INTERVAL_TICKS;
    }

    /** Fires the next rocket from whichever wing is up; the pods alternate. */
    private void launchRocket(ServerLevel server, Player pilot) {
        Vec3 aim = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
        double side = ApachePods.launchLateralBlocks(
                rocketMagazine.ready(), ApacheGeometry.podLateralBlocks());
        Vec3 muzzle = position()
                .add(0.0D, ApacheGeometry.podHeightBlocks(), 0.0D)
                .add(right.scale(side))
                .add(aim.scale(ApacheGeometry.podForwardBlocks()));
        LittleBirdRocketEntity rocket = new LittleBirdRocketEntity(
                ModEntities.LITTLE_BIRD_ROCKET.get(), server);
        rocket.setPos(muzzle.x, muzzle.y, muzzle.z);
        rocket.setDamageScale(LittleBirdConfig.apacheRocketDamageScale());
        rocket.launch(aim, LittleBirdRocketBallistics.INITIAL_SPEED, pilot);
        server.addFreshEntity(rocket);
        server.playSound(null, getX(), getY(), getZ(), ModSounds.ROCKET_LAUNCH.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.1F);
    }

    /** Fires the next missile from the rail that still has one; the rails alternate. */
    private void launchMissile(ServerLevel server, Player pilot) {
        Vec3 aim = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
        int fired = MISSILE_READY_CAPACITY - missileMagazine.ready();
        double side = fired % 2 == 0 ? -ApacheGeometry.railLateralBlocks()
                : ApacheGeometry.railLateralBlocks();
        Vec3 muzzle = position()
                .add(0.0D, ApacheGeometry.railHeightBlocks(), 0.0D)
                .add(right.scale(side))
                .add(aim.scale(ApacheGeometry.railForwardBlocks()));
        ApacheMissileEntity missile = new ApacheMissileEntity(
                ModEntities.APACHE_MISSILE.get(), server);
        missile.setPos(muzzle.x, muzzle.y, muzzle.z);
        // The TOW is flown onto the spot the sight line ends at, so the depth of that spot is
        // measured once here: the first block ahead, or the sight range over open sky.
        missile.launch(aim, pilot, ApacheMissileEntity.aimDepthBlocks(
                server, muzzle, aim, this));
        server.addFreshEntity(missile);
        server.playSound(null, getX(), getY(), getZ(), ModSounds.ROCKET_LAUNCH.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    /** The hull's sight line, handed to any missile it fires. */
    @Override
    public Vec3 missileSightDirection() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    @Override
    public Vec3 missileSightOrigin() {
        Player pilot = pilot();
        return pilot != null ? pilot.getEyePosition() : position();
    }

    /** Server: validated weapon intent from the rear seat. */
    public void applyWeaponInput(Player sender, int weapon, boolean triggerHeld) {
        if (level().isClientSide || isRemoved() || getControllingPassenger() != sender) {
            return;
        }
        setWeapon(weapon);
        this.triggerHeld = triggerHeld;
    }

    /**
     * Server: the reload key. Both magazines top up from their own reserves, so a pilot who
     * has fired the pods dry can reload while still lining up the next pass.
     */
    public void applyReload(Player sender) {
        if (level().isClientSide || isRemoved() || getControllingPassenger() != sender) {
            return;
        }
        rocketMagazine = TwoStageMagazine.startReload(rocketMagazine, rocketBay);
        missileMagazine = TwoStageMagazine.startReload(missileMagazine, missileRail);
    }

    /** Server: validated roll/engine report from the pilot's client. */
    public void applyClientControl(float rollDeg, boolean engineOn, Player sender) {
        if (level().isClientSide || getControllingPassenger() != sender) {
            return;
        }
        float clamped = Math.max(-MAX_SYNCED_ROLL_DEG, Math.min(MAX_SYNCED_ROLL_DEG, rollDeg));
        if (Float.isNaN(clamped)) {
            return;
        }
        entityData.set(DATA_ROLL_DEG, clamped);
    }

    /** Server: validated wall impact report from the pilot's client. */
    public void applyImpact(Player sender, float speed) {
        if (level().isClientSide || getControllingPassenger() != sender
                || impactPacketCooldown > 0 || !Float.isFinite(speed)) {
            return;
        }
        double threshold = FLIGHT.crashSpeedThreshold;
        double clamped = Math.min(speed, 3.0D);
        if (clamped <= threshold) {
            return;
        }
        float damage = (float) ((clamped - threshold) * FLIGHT.crashDamagePerSpeed);
        impactPacketCooldown = IMPACT_PACKET_COOLDOWN_TICKS;
        hurt(level().damageSources().flyIntoWall(), damage);
        sender.hurt(level().damageSources().flyIntoWall(), damage * 0.25F);
    }

    /** Server: validated dive-crash report; the airframe is written off and the crew thrown clear. */
    public void applyCrash(Player sender) {
        if (level().isClientSide || isRemoved() || getControllingPassenger() != sender) {
            return;
        }
        Vec3 motion = getDeltaMovement();
        for (Entity passenger : List.copyOf(getPassengers())) {
            passenger.stopRiding();
            passenger.resetFallDistance();
            passenger.invulnerableTime = 20;
            passenger.setDeltaMovement(motion.scale(0.4D).add(0.0D, 0.25D, 0.0D));
        }
        destroyAircraft();
    }

    /** Remaining health of a hit target as a 0..1 fraction (shared with the Little Bird). */
    public static float healthFractionOf(Entity target) {
        return HitFeedback.healthFractionOf(target);
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
        if (source.is(DamageTypeTags.IS_EXPLOSION)
                && (source.getDirectEntity() instanceof LittleBirdRocketEntity
                        || source.getDirectEntity() instanceof ApacheMissileEntity)) {
            return false;
        }
        float remaining = getHealth() - amount;
        setHealth(remaining);
        if (remaining <= 0.0F) {
            destroyAircraft();
        }
        return true;
    }

    /** Writes the airframe off: the crew is thrown clear, then the wreck goes up. Once only. */
    private void destroyAircraft() {
        if (!destruction.beginDestruction()) {
            return;
        }
        ejectCrew();
        if (level() instanceof ServerLevel server) {
            server.explode(this, getX(), getY(), getZ(), 3.5F, Level.ExplosionInteraction.NONE);
        }
        // Scrap after the blast, not before: anything dropped first would be blown up with it.
        spawnAtLocation(Items.IRON_INGOT, 3 + random.nextInt(4));
        discard();
    }

    /**
     * Nobody may be left riding a wreck: Entity#remove does not dismount passengers, so a crew
     * that was shot down would keep sitting on an entity that no longer exists.
     */
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

    public boolean isEngineOn() {
        return entityData.get(DATA_ENGINE_ON);
    }

    public void setEngineOn(boolean engineOn) {
        entityData.set(DATA_ENGINE_ON, engineOn);
    }

    public float getRotorRpm() {
        return entityData.get(DATA_ROTOR_RPM);
    }

    public int getWeapon() {
        return entityData.get(DATA_WEAPON);
    }

    public void setWeapon(int weapon) {
        entityData.set(DATA_WEAPON, weapon == WEAPON_MISSILES ? WEAPON_MISSILES : WEAPON_ROCKETS);
    }

    public float getRollDeg() {
        return entityData.get(DATA_ROLL_DEG);
    }

    /** Roll used by the renderer: local prediction for the pilot, synced otherwise. */
    public float getRenderRollDeg() {
        if (level() != null && level().isClientSide && clientState != null
                && isControlledByLocalInstance()) {
            return clientState.rollDeg();
        }
        return getRollDeg();
    }

    public float getRenderRotorRpm() {
        if (level() != null && level().isClientSide && clientState != null
                && isControlledByLocalInstance()) {
            return clientState.rotorRpm();
        }
        return getRotorRpm();
    }

    public float getRenderRotorSpinFactor() {
        return (float) HelicopterFlightModel.rotorSpinFactor(
                Math.abs(getDeltaMovement().y), FLIGHT);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setHealth(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        setEngineOn(tag.getBoolean("EngineOn"));
        entityData.set(DATA_ROTOR_RPM, tag.contains("RotorRpm")
                ? tag.getFloat("RotorRpm") : 0.0F);
        setWeapon(tag.getInt("Weapon"));
        entityData.set(DATA_SEATS_SWAPPED, tag.getBoolean("SeatsSwapped"));
        entityData.set(DATA_CHAIN_AMMO, tag.contains("ChainAmmo")
                ? tag.getInt("ChainAmmo") : CHAIN_GUN_AMMO_CAPACITY);
        if (tag.contains("MagRocketReady")) {
            rocketMagazine = new TwoStageMagazine.State(tag.getInt("MagRocketReady"),
                    tag.getInt("MagRocketReserve"), tag.getInt("MagRocketReload"),
                    tag.getInt("MagRocketResupply"));
            missileMagazine = new TwoStageMagazine.State(tag.getInt("MagMissileReady"),
                    tag.getInt("MagMissileReserve"), tag.getInt("MagMissileReload"),
                    tag.getInt("MagMissileResupply"));
        }
        setRocketLoadout(tag.contains("ReadyRockets")
                ? tag.getInt("ReadyRockets") : ROCKET_READY_CAPACITY,
                tag.contains("ReserveRockets")
                        ? tag.getInt("ReserveRockets") : ROCKET_RESERVE_CAPACITY);
        setMissileLoadout(tag.contains("ReadyMissiles")
                ? tag.getInt("ReadyMissiles") : MISSILE_READY_CAPACITY,
                tag.contains("ReserveMissiles")
                        ? tag.getInt("ReserveMissiles") : MISSILE_RESERVE_CAPACITY);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        tag.putBoolean("EngineOn", isEngineOn());
        tag.putFloat("RotorRpm", getRotorRpm());
        tag.putInt("Weapon", getWeapon());
        tag.putBoolean("SeatsSwapped", isSeatsSwapped());
        tag.putInt("MagRocketReady", rocketMagazine.ready());
        tag.putInt("MagRocketReserve", rocketMagazine.reserve());
        tag.putInt("MagRocketReload", rocketMagazine.reloadTicks());
        tag.putInt("MagRocketResupply", rocketMagazine.resupplyTicks());
        tag.putInt("ChainAmmo", getChainAmmo());
        tag.putInt("MagMissileReady", missileMagazine.ready());
        tag.putInt("MagMissileReserve", missileMagazine.reserve());
        tag.putInt("MagMissileReload", missileMagazine.reloadTicks());
        tag.putInt("MagMissileResupply", missileMagazine.resupplyTicks());
        tag.putInt("ReadyRockets", getReadyRockets());
        tag.putInt("ReserveRockets", getReserveRockets());
        tag.putInt("ReadyMissiles", getReadyMissiles());
        tag.putInt("ReserveMissiles", getReserveMissiles());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
