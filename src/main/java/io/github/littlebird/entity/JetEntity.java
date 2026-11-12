package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.flight.JetType;
import io.github.littlebird.entity.flight.HelicopterFlightModel;
import io.github.littlebird.entity.flight.JetAxes;
import io.github.littlebird.entity.flight.JetConfig;
import io.github.littlebird.entity.flight.JetFlightModel;
import io.github.littlebird.entity.flight.JetInput;
import io.github.littlebird.entity.flight.JetState;
import io.github.littlebird.entity.ground.GroundWeapons;
import io.github.littlebird.entity.weapon.AmmoPool;
import io.github.littlebird.entity.weapon.AmmoSupply;
import io.github.littlebird.entity.weapon.Countermeasures;
import io.github.littlebird.entity.weapon.JetCannon;
import io.github.littlebird.entity.weapon.MissileKind;
import io.github.littlebird.entity.weapon.TargetLock;
import io.github.littlebird.entity.weapon.RocketPodState;
import io.github.littlebird.network.JetControlPacket;
import io.github.littlebird.network.LittleBirdCrashPacket;
import io.github.littlebird.network.LittleBirdImpactPacket;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * F-35E Panther: a single seat, twin tail, fixed wing stealth fighter. The first aircraft in the
 * pack that flies without a rotor, so it keeps the Little Bird's authority split: the pilot's own
 * client steps {@link JetFlightModel} and the vanilla vehicle packet carries the pose, while the
 * bank angle, throttle, burner charge and hover mode ride the mod's own {@link JetControlPacket}.
 * Damage stays server authoritative.
 *
 * <p>The Battlefield 2042 control preset is replicated here: mouse Y pitches, mouse X rolls and
 * A/D works the rudder (the mouse-yaw switch swaps the mouse and A/D axes). Mouse X arrives as the frame's yaw delta, which the model reads as a roll
 * rate command, and the pilot's view is anchored back to the airframe each tick so the camera does
 * not spin. W/S integrates the throttle and the sprint key lights the afterburner.
 */
public class JetEntity extends Entity implements GunshipRider {
    /** F-35E hull: a light stealth fighter, priced between the Little Bird and the Apache. */
    public static final float MAX_HEALTH = 100.0F;

    public static final int CONTROL_THROTTLE_UP = 1;
    public static final int CONTROL_THROTTLE_DOWN = 2;
    public static final int CONTROL_RUDDER_RIGHT = 4;
    public static final int CONTROL_RUDDER_LEFT = 8;

    public static final int WEAPON_CANNON = 0;
    public static final int WEAPON_ROCKETS = 1;
    public static final int WEAPON_IR_MISSILE = 2;
    public static final int WEAPON_AGM = 3;
    public static final int WEAPON_RADAR_MISSILE = 4;

    /** 25mm belt: enough for several long bursts. */
    public static final int CANNON_AMMO_CAPACITY = 500;
    /** Six air-to-air missiles, matching the F-35E's internal bay load. */
    public static final int MISSILE_AMMO_CAPACITY = 6;

    private static final float ROLL_SYNC_THRESHOLD_DEG = 0.5F;
    private static final float SYNC_AXIS_THRESHOLD = 0.005F;
    private static final float MAX_SYNCED_ROLL_DEG = 180.0F;
    private static final double BLOCKED_SPEED_KEEP = 0.15D;
    private static final int IMPACT_PACKET_COOLDOWN_TICKS = 10;
    /** Mouse yaw per client tick that commands full roll: 6 degrees is a firm flick. */
    private static final float ROLL_MOUSE_FULL_DEG = 10.0F;
    private static final double PILOT_PITCH_REFERENCE_DEG = 90.0D;
    private static final double CRASH_DAMAGE_PER_SPEED = 14.0D;
    /** 25mm direct hit: light, so a jet strafes infantry and light armour, not a tank. */
    /** Hydra-70 direct hit, priced against the Little Bird's own 70mm rocket. */
    private static final float ROCKET_DAMAGE_SCALE = 1.0F;
    private static final double LOCK_RANGE_BLOCKS = 96.0D;
    /** Roughly a 26-degree half-angle lock cone. */
    private static final double LOCK_CONE_COS = 0.90D;
    private static final double AGM_LOCK_RANGE_BLOCKS = 128.0D;
    private static final double AGM_LOCK_CONE_COS = 0.85D;
    private static final double RADAR_LOCK_RANGE_BLOCKS = 128.0D;
    private static final double RADAR_LOCK_CONE_COS = 0.80D;
    private static final int MISSILE_FIRE_COOLDOWN_TICKS = 10;

    private static final EntityDataAccessor<Float> DATA_ROLL_DEG =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_THROTTLE =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AFTERBURNER_CHARGE =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_VTOL =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENGINE_ON =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_WEAPON =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CANNON_STATE =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CANNON_HEAT =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CANNON_AMMO =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROCKET_READY =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROCKET_RESERVE =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MISSILE_AMMO =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLARES =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LOCK_TARGET =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LOCK_PROGRESS =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LOCKED =
            SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.BOOLEAN);

    private final AircraftDestruction destruction = new AircraftDestruction();

    private JetState clientState;
    private int clientControls;
    /** Client only: accumulated mouse pitch, in degrees, fed to the model as the stick. */
    private float pitchCommandDeg;
    /** Client only: mouse travel banked since the last anchor, in degrees. */
    private float pendingMouseYawDeg;
    private float pendingMousePitchDeg;

    /**
     * Set while an external sight (the B-2 electro-optical pod) owns the mouse: the pilot's look
     * travel is neither banked into the flight model nor used to anchor the view, so the airframe
     * stops taking mouse input and its own roll leveller flies the bomb run straight.
     */
    private boolean clientInputSuppressed;

    public void setClientInputSuppressed(boolean value) {
        this.clientInputSuppressed = value;
    }

    public boolean isClientInputSuppressed() {
        return clientInputSuppressed;
    }
    /** Client only: the view the pilot was last anchored to; NaN before the first anchor. */
    private float lastAnchorYawDeg = Float.NaN;
    private float lastAnchorPitchDeg = Float.NaN;
    /** Client only: previous render roll, for partial-tick banking interpolation. */
    private float renderRollO;
    private boolean clientAfterburner;
    private boolean clientVtol;
    private float lastSentRollDeg = Float.NaN;
    private float lastSentThrottle = Float.NaN;
    private float lastSentCharge = Float.NaN;
    private boolean lastSentVtol;
    private int impactPacketCooldown;
    private int crashPacketCooldown;
    private boolean cockpitView;
    private boolean shoulderView;

    private final AmmoPool cannonAmmo = AmmoPool.full(CANNON_AMMO_CAPACITY);
    private JetCannon.Snapshot cannonState = JetCannon.Snapshot.initial();
    private RocketPodState.Snapshot podState = RocketPodState.Snapshot.initial();
    private boolean triggerHeld;
    private int supplyTick;

    private final AmmoPool missileAmmo = AmmoPool.full(MISSILE_AMMO_CAPACITY);
    private TargetLock.Snapshot lockState = TargetLock.Snapshot.NONE;
    private Countermeasures.Snapshot flareState =
            Countermeasures.Snapshot.full(flareSpec());
    private int missileCooldown;

    public JetEntity(EntityType<? extends JetEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    /** Airframe numbers; the Su-57 overrides this with its own flight profile and gun. */
    public JetType jetType() {
        return JetType.F35;
    }

    protected JetConfig flightConfig() {
        return jetType().config();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ROLL_DEG, 0.0F);
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_THROTTLE, 0.0F);
        entityData.define(DATA_AFTERBURNER_CHARGE, 1.0F);
        entityData.define(DATA_VTOL, false);
        entityData.define(DATA_ENGINE_ON, false);
        entityData.define(DATA_WEAPON, WEAPON_CANNON);
        entityData.define(DATA_CANNON_STATE, JetCannon.IDLE);
        entityData.define(DATA_CANNON_HEAT, 0);
        entityData.define(DATA_CANNON_AMMO, CANNON_AMMO_CAPACITY);
        entityData.define(DATA_ROCKET_READY, RocketPodState.READY_CAPACITY);
        entityData.define(DATA_ROCKET_RESERVE, RocketPodState.RESERVE_CAPACITY);
        entityData.define(DATA_MISSILE_AMMO, MISSILE_AMMO_CAPACITY);
        entityData.define(DATA_FLARES, LittleBirdConfig.jetFlareCount());
        entityData.define(DATA_LOCK_TARGET, -1);
        entityData.define(DATA_LOCK_PROGRESS, 0);
        entityData.define(DATA_LOCKED, false);
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
    // Seats: one cockpit, one pilot.
    // ------------------------------------------------------------------

    @Override
    public LivingEntity getControllingPassenger() {
        if (getPassengers().isEmpty()) {
            return null;
        }
        return getPassengers().get(0) instanceof Player pilot ? pilot : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public double getPassengersRidingOffset() {
        return JetGeometry.seatHeightBlocks();
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        if (isShoulderCamera()) {
            // Third person orbits the hidden rider, so shifting the rider sideways moves the
            // camera the same way and frames the airframe off centre.
            LittleBirdCameraMath.Offset offset = LittleBirdCameraMath.shoulderOffset(
                    getYRot(),
                    LittleBirdConfig.shoulderOffsetBlocks(),
                    LittleBirdConfig.shoulderHeightBlocks());
            moveFunction.accept(passenger,
                    getX() + offset.x(),
                    getY() + JetGeometry.seatHeightBlocks()
                            + passenger.getMyRidingOffset() + offset.y(),
                    getZ() + offset.z());
            return;
        }
        double forward = JetGeometry.seatForwardBlocks();
        double yaw = Math.toRadians(getYRot());
        moveFunction.accept(passenger,
                getX() - Math.sin(yaw) * forward,
                getY() + JetGeometry.seatHeightBlocks() + passenger.getMyRidingOffset(),
                getZ() + Math.cos(yaw) * forward);
    }

    // ------------------------------------------------------------------
    // Client flight
    // ------------------------------------------------------------------

    /** Client only: the key axes the mouse cannot express (throttle and rudder). */
    public void setClientControls(int controls) {
        this.clientControls = controls;
    }

    /** Client only: true while the sprint key holds the afterburner. */
    public void setClientAfterburner(boolean value) {
        this.clientAfterburner = value;
    }

    /** Client only: hover mode for the F-35E (VTOL). Airframes without it ignore the request. */
    public void setClientVtol(boolean value) {
        this.clientVtol = value && jetType().supportsVtol();
    }

    public boolean isCockpitView() {
        return cockpitView;
    }

    public void setCockpitView(boolean value) {
        this.cockpitView = value;
    }

    public void setShoulderView(boolean value) {
        this.shoulderView = value;
    }

    private boolean isShoulderCamera() {
        return shoulderView && !cockpitView && level() != null && level().isClientSide;
    }

    /** Client only: forgets prediction state when the jet is left behind. */
    public void resetClientPrediction() {
        this.clientState = null;
        this.clientControls = 0;
        this.pitchCommandDeg = 0.0F;
        this.pendingMouseYawDeg = 0.0F;
        this.pendingMousePitchDeg = 0.0F;
        this.lastAnchorYawDeg = Float.NaN;
        this.lastAnchorPitchDeg = Float.NaN;
        this.clientAfterburner = false;
        this.clientVtol = false;
        this.lastSentRollDeg = Float.NaN;
        this.lastSentThrottle = Float.NaN;
        this.lastSentCharge = Float.NaN;
        this.lastSentVtol = false;
        this.crashPacketCooldown = 0;
        this.shoulderView = false;
    }

    private void tickClient() {
        // Remember the roll this tick started from so the renderer can lerp across it.
        renderRollO = getRenderRollDeg();
        if (isControlledByLocalInstance() && getControllingPassenger() instanceof Player pilot) {
            applyLocalFlight(pilot);
        }
    }

    /** Steps the flight model on the pilot's client and applies the result. */
    private void applyLocalFlight(Player pilot) {
        boolean firstTick = clientState == null;
        JetState state = firstTick
                ? JetState.initial(pilot.getYRot(), clientVtol)
                : clientState.withVtol(clientVtol);
        if (firstTick) {
            // The player usually placed the jet while looking at the ground. The command is
            // accumulated from mouse travel only, so the takeoff roll starts level without
            // banking that old look angle; the view itself is slaved to the airframe below.
            pitchCommandDeg = 0.0F;
            pendingMousePitchDeg = 0.0F;
        }

        // The mouse flies the jet through the travel banked by anchorPilotView on the render
        // frames since this tick. The view is no longer read here, so anchoring the camera every
        // frame cannot eat or double the input.
        if (clientInputSuppressed) {
            // Bomb run: wings level and the nose on the horizon, both commanded to zero so the
            // flight model's own leveller does the flying.
            pendingMouseYawDeg = 0.0F;
            pendingMousePitchDeg = 0.0F;
            pitchCommandDeg = 0.0F;
        }
        float mouseAxis = JetInput.clampAxis(pendingMouseYawDeg / ROLL_MOUSE_FULL_DEG);
        float throttleInput = ((clientControls & CONTROL_THROTTLE_UP) != 0 ? 1.0F : 0.0F)
                - ((clientControls & CONTROL_THROTTLE_DOWN) != 0 ? 1.0F : 0.0F);
        float keyAxis = ((clientControls & CONTROL_RUDDER_RIGHT) != 0 ? 1.0F : 0.0F)
                - ((clientControls & CONTROL_RUDDER_LEFT) != 0 ? 1.0F : 0.0F);
        // Battlefield preset: the mouse rolls and A/D yaws. The mouse-yaw switch swaps the two
        // so the jet can be flown like the helicopters; pitch is untouched either way.
        JetAxes axes = JetAxes.of(mouseAxis, keyAxis, LittleBirdConfig.jetMouseYaw());
        float rollInput = axes.roll();
        float yawInput = axes.yaw();
        float pitchDelta = pendingMousePitchDeg;
        if (LittleBirdConfig.jetInvertPitch()) {
            pitchDelta = -pitchDelta;
        }
        pendingMouseYawDeg = 0.0F;
        pendingMousePitchDeg = 0.0F;
        pitchCommandDeg = (float) Math.max(-PILOT_PITCH_REFERENCE_DEG,
                Math.min(PILOT_PITCH_REFERENCE_DEG, pitchCommandDeg + pitchDelta));
        float pitchInput = (float) (pitchCommandDeg / PILOT_PITCH_REFERENCE_DEG);

        JetInput input = new JetInput(pitchInput, rollInput, yawInput, throttleInput,
                clientAfterburner, clientVtol, onGround());
        JetState next = JetFlightModel.step(state, input, flightConfig());

        setYRot(next.yawDeg());
        setXRot(next.pitchDeg());
        // The pilot's own view is slaved to the airframe by anchorPilotView on every rendered
        // frame. Writing it here too would feed this tick's yaw change back in as mouse travel.

        Vec3 delta = new Vec3(next.vx(), next.vy(), next.vz());
        setDeltaMovement(delta);
        move(MoverType.SELF, delta);

        double impactSpeed = Math.sqrt(next.vx() * next.vx() + next.vz() * next.vz());
        if (horizontalCollision && impactSpeed > flightConfig().crashMinImpactSpeed
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
            next = rebuilt(next, vx, vy, next.vz() * BLOCKED_SPEED_KEEP);
        }
        if (verticalCollision && vy < 0.0D) {
            if (crashPacketCooldown <= 0
                    && JetFlightModel.isDiveCrash(vy, next.pitchDeg(), flightConfig())) {
                ModNetwork.CHANNEL.sendToServer(new LittleBirdCrashPacket());
                crashPacketCooldown = IMPACT_PACKET_COOLDOWN_TICKS;
            }
            next = rebuilt(next, next.vx(), 0.0D, next.vz());
        }
        clientState = next;
        syncControlToServer(next);
    }

    private static JetState rebuilt(JetState state, double vx, double vy, double vz) {
        return new JetState(state.yawDeg(), state.pitchDeg(), state.rollDeg(),
                vx, vy, vz, state.throttle(), state.afterburnerCharge(), state.vtol());
    }

    private void syncControlToServer(JetState next) {
        if (!Float.isNaN(lastSentRollDeg)
                && Math.abs(next.rollDeg() - lastSentRollDeg) < ROLL_SYNC_THRESHOLD_DEG
                && Math.abs(next.throttle() - lastSentThrottle) < SYNC_AXIS_THRESHOLD
                && Math.abs(next.afterburnerCharge() - lastSentCharge) < SYNC_AXIS_THRESHOLD
                && next.vtol() == lastSentVtol) {
            return;
        }
        lastSentRollDeg = next.rollDeg();
        lastSentThrottle = next.throttle();
        lastSentCharge = next.afterburnerCharge();
        lastSentVtol = next.vtol();
        ModNetwork.CHANNEL.sendToServer(new JetControlPacket(
                next.rollDeg(), next.throttle(), next.afterburnerCharge(), next.vtol(), true));
    }

    // ------------------------------------------------------------------
    // Server tick
    // ------------------------------------------------------------------

    private void tickServer() {
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        setEngineOn(true);
        if (level() instanceof ServerLevel server) {
            tickWeapons(server);
        }
        if (getControllingPassenger() == null) {
            applyUnpilotedMotion();
        }
    }

    // ------------------------------------------------------------------
    // Weapons (server authoritative)
    // ------------------------------------------------------------------

    /** Server: validated weapon-slot and trigger report from the pilot's client. */
    public void applyWeaponInput(Player sender, int weapon, boolean salvoMode, boolean held) {
        if (level().isClientSide || getControllingPassenger() != sender) {
            return;
        }
        entityData.set(DATA_WEAPON, switch (weapon) {
            case WEAPON_ROCKETS -> WEAPON_ROCKETS;
            case WEAPON_IR_MISSILE -> WEAPON_IR_MISSILE;
            case WEAPON_AGM -> WEAPON_AGM;
            case WEAPON_RADAR_MISSILE -> WEAPON_RADAR_MISSILE;
            default -> WEAPON_CANNON;
        });
        this.triggerHeld = held;
    }

    private void tickWeapons(ServerLevel server) {
        Player pilot = getControllingPassenger() instanceof Player player ? player : null;

        boolean firingCannon = triggerHeld && getWeapon() == WEAPON_CANNON;
        JetCannon.Step cannonStep = JetCannon.advance(firingCannon, cannonState,
                jetType().cannon());
        cannonState = cannonStep.snapshot();
        entityData.set(DATA_CANNON_STATE, cannonState.state());
        entityData.set(DATA_CANNON_HEAT, cannonState.heat());
        if (cannonStep.fire() && pilot != null && !cannonAmmo.isEmpty()) {
            cannonAmmo.fire(1);
            entityData.set(DATA_CANNON_AMMO, cannonAmmo.rounds());
            fireCannon(server, pilot);
        }

        boolean firingRockets = triggerHeld && getWeapon() == WEAPON_ROCKETS;
        RocketPodState.Step podStep = RocketPodState.advance(podState, firingRockets, true);
        podState = podStep.snapshot();
        entityData.set(DATA_ROCKET_READY, podState.totalRemaining());
        if (podStep.launch() && pilot != null) {
            launchRocket(server, pilot, podStep.pod(), podStep.tube());
        }

        tickGuidedWeapons(server, pilot);
        tickAmmoSupply(server);
    }

    /** Lock acquisition, fire-and-forget missiles and the flare dispenser. */
    private void tickGuidedWeapons(ServerLevel server, Player pilot) {
        if (missileCooldown > 0) {
            --missileCooldown;
        }
        if (pilot != null) {
            updateLock(server, pilot);
            int weapon = getWeapon();
            boolean firingMissiles = triggerHeld
                    && (weapon == WEAPON_IR_MISSILE || weapon == WEAPON_AGM
                            || weapon == WEAPON_RADAR_MISSILE);
            // The radar missile may be fired without a mature lock and self-acquires in flight.
            boolean mayFire = weapon == WEAPON_RADAR_MISSILE || lockState.locked();
            if (firingMissiles && mayFire && missileCooldown == 0 && !missileAmmo.isEmpty()) {
                missileAmmo.fire(1);
                missileCooldown = MISSILE_FIRE_COOLDOWN_TICKS;
                entityData.set(DATA_MISSILE_AMMO, missileAmmo.rounds());
                fireMissile(server, pilot, kindFor(weapon));
            }
        }
        flareState = Countermeasures.advance(flareState);
        entityData.set(DATA_FLARES, flareState.flares());
    }

    /**
     * Looks for the best airframe in a narrow cone ahead and advances the lock. The lock lives
     * entirely server-side; the pilot only sees the synced progress and target id.
     */
    private void updateLock(ServerLevel server, Player pilot) {
        int weapon = getWeapon();
        boolean airTargets = weapon == WEAPON_IR_MISSILE || weapon == WEAPON_RADAR_MISSILE;
        boolean groundTargets = weapon == WEAPON_AGM;
        if (!airTargets && !groundTargets) {
            // No secondary seeker selected: clear the lock rather than keep a stale one.
            lockState = TargetLock.Snapshot.NONE;
            entityData.set(DATA_LOCK_TARGET, -1);
            entityData.set(DATA_LOCK_PROGRESS, 0);
            entityData.set(DATA_LOCKED, false);
            return;
        }
        double range = groundTargets ? AGM_LOCK_RANGE_BLOCKS
                : weapon == WEAPON_RADAR_MISSILE ? RADAR_LOCK_RANGE_BLOCKS
                        : LittleBirdConfig.jetLockRangeBlocks();
        double cone = groundTargets ? AGM_LOCK_CONE_COS
                : weapon == WEAPON_RADAR_MISSILE ? RADAR_LOCK_CONE_COS : LOCK_CONE_COS;
        TargetLock.Spec spec = airTargets && weapon == WEAPON_IR_MISSILE
                ? TargetLock.Spec.IR : TargetLock.Spec.RADAR;

        Vec3 nose = Vec3.directionFromRotation(getXRot(), getYRot());
        Entity best = null;
        double bestDot = cone;
        for (Entity candidate : server.getEntities(this, getBoundingBox().inflate(range),
                e -> e != this && e != pilot && e.isAlive() && !e.isSpectator()
                        && (groundTargets
                                ? AirframeTargets.isGroundVehicle(e)
                                : AirframeTargets.isAirframe(e)))) {
            Vec3 offset = candidate.position()
                    .add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D)
                    .subtract(position());
            double distance = offset.length();
            if (distance < 1.0E-4D || distance > range) {
                continue;
            }
            double dot = offset.normalize().dot(nose);
            if (dot > bestDot) {
                bestDot = dot;
                best = candidate;
            }
        }
        int candidateId = best == null ? -1 : best.getId();
        lockState = TargetLock.advance(lockState, candidateId, best != null, spec);
        entityData.set(DATA_LOCK_TARGET, lockState.targetId());
        entityData.set(DATA_LOCK_PROGRESS, lockState.progress());
        entityData.set(DATA_LOCKED, lockState.locked());
    }

    private static MissileKind kindFor(int weapon) {
        return switch (weapon) {
            case WEAPON_AGM -> MissileKind.AGM;
            case WEAPON_RADAR_MISSILE -> MissileKind.RADAR;
            default -> MissileKind.IR;
        };
    }

    private void fireMissile(ServerLevel server, Player pilot, MissileKind kind) {
        // A radar shot may have no target yet: the missile self-acquires in flight.
        Entity target = server.getEntity(lockState.targetId());
        Vec3 aim = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
        double side = missileAmmo.rounds() % 2 == 0 ? -1.0D : 1.0D;
        Vec3 origin = position()
                .add(0.0D, JetGeometry.hardpointHeightBlocks(), 0.0D)
                .add(right.scale(JetGeometry.hardpointLateralBlocks() * side))
                .add(aim.scale(JetGeometry.hardpointForwardBlocks()));

        LockOnMissileEntity missile =
                new LockOnMissileEntity(ModEntities.LOCK_ON_MISSILE.get(), level());
        missile.setPos(origin.x, origin.y, origin.z);
        missile.launch(aim, LockOnMissileEntity.INITIAL_SPEED, pilot);
        missile.lockOnto(target, kind);
        server.addFreshEntity(missile);
        server.playSound(null, getX(), getY(), getZ(), ModSounds.ROCKET_LAUNCH.get(),
                SoundSource.PLAYERS, 1.4F, 1.2F);
    }

    /** Server: one press of the flare key. */
    public void applyFlare(Player sender) {
        if (level().isClientSide || getControllingPassenger() != sender) {
            return;
        }
        flareState = Countermeasures.deploy(flareState, flareSpec());
        entityData.set(DATA_FLARES, flareState.flares());
    }

    /** Flare dispenser numbers, read from config so a server can retune the loadout. */
    private static Countermeasures.Spec flareSpec() {
        return new Countermeasures.Spec(LittleBirdConfig.jetFlareCount(), 20, 30, 0.6D);
    }

    /** Called by an inbound missile: true when this jet's open flare window spoofs it. */
    public boolean countermeasureSeduces(double roll) {
        return Countermeasures.seduces(flareState, flareSpec(), roll);
    }

    private void fireCannon(ServerLevel server, Player pilot) {
        Vec3 aim = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 muzzle = position()
                .add(0.0D, JetGeometry.cannonMuzzleHeightBlocks(), 0.0D)
                .add(aim.scale(JetGeometry.cannonMuzzleForwardBlocks()));
        GroundWeapons.fireHitscan(server, this, pilot, muzzle, aim,
                io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.JET_CANNON,
                0.0D, 0.0F, 1.3F);
    }

    private void launchRocket(ServerLevel server, Player pilot, int pod, int tube) {
        Vec3 aim = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
        double lateral = (pod == 0 ? -1.0D : 1.0D) * JetGeometry.hardpointLateralBlocks()
                + (tube - (RocketPodState.TUBES_PER_POD - 1) / 2.0D) * 0.08D;
        double vertical = ((tube % 2) - 0.5D) * 0.06D;
        Vec3 origin = position()
                .add(0.0D, JetGeometry.hardpointHeightBlocks() + vertical, 0.0D)
                .add(right.scale(lateral))
                .add(aim.scale(JetGeometry.hardpointForwardBlocks()));

        LittleBirdRocketEntity rocket = new LittleBirdRocketEntity(
                ModEntities.LITTLE_BIRD_ROCKET.get(), level());
        rocket.setPos(origin.x, origin.y, origin.z);
        rocket.setDamageScale(ROCKET_DAMAGE_SCALE);
        rocket.launch(aim, LittleBirdRocketEntity.INITIAL_SPEED, pilot);
        server.addFreshEntity(rocket);
        server.playSound(null, getX(), getY(), getZ(), ModSounds.ROCKET_LAUNCH.get(),
                SoundSource.PLAYERS, 1.4F, 1.0F);
    }

    /**
     * Ammunition comes back from an ammunition crate parked next to the jet, the same rule the two
     * helicopters live by: a long fight means going home.
     */
    private void tickAmmoSupply(ServerLevel server) {
        if (++supplyTick < 20) {
            return;
        }
        supplyTick = 0;
        if (!AircraftRearm.crateNearby(this)) {
            return;
        }
        AmmoSupply supply = LittleBirdConfig.ammoSupply();
        cannonAmmo.refill(supply.bullets(20));
        missileAmmo.refill(supply.missiles(20));
        flareState = Countermeasures.refill(flareState, flareSpec(),
                supply.missiles(20));
        int reserve = Math.min(RocketPodState.RESERVE_CAPACITY,
                getRocketReserve() + supply.rockets(20));
        entityData.set(DATA_CANNON_AMMO, cannonAmmo.rounds());
        entityData.set(DATA_MISSILE_AMMO, missileAmmo.rounds());
        entityData.set(DATA_FLARES, flareState.flares());
        entityData.set(DATA_ROCKET_RESERVE, reserve);
    }

    /** Server: reload key. Moves rocket reserve rounds into the ready pods. */
    public void applyReload(Player sender) {
        if (level().isClientSide || getControllingPassenger() != sender) {
            return;
        }
        int missing = RocketPodState.READY_CAPACITY - getRocketReady();
        int moved = Math.min(Math.max(0, missing), getRocketReserve());
        if (moved <= 0) {
            return;
        }
        podState = RocketPodState.rearm(podState, moved);
        entityData.set(DATA_ROCKET_READY, podState.totalRemaining());
        entityData.set(DATA_ROCKET_RESERVE, getRocketReserve() - moved);
    }

    /** Nobody is flying: the airframe drops, or parks once it is resting on the ground. */
    private void applyUnpilotedMotion() {
        Vec3 motion = getDeltaMovement();
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 falling = new Vec3(motion.x,
                Math.max(motion.y - flightConfig().gravity, -flightConfig().maxFallSpeed), motion.z);
        setDeltaMovement(falling);
        move(MoverType.SELF, falling);
    }

    /** Server: validated attitude/engine report from the pilot's client. */
    public void applyClientControl(float rollDeg, float throttle, float afterburnerCharge,
            boolean vtol, boolean engineOn, Player sender) {
        if (level().isClientSide || getControllingPassenger() != sender) {
            return;
        }
        float clamped = Math.max(-MAX_SYNCED_ROLL_DEG,
                Math.min(MAX_SYNCED_ROLL_DEG, rollDeg));
        if (Float.isNaN(clamped)) {
            return;
        }
        entityData.set(DATA_ROLL_DEG, clamped);
        entityData.set(DATA_THROTTLE, clamp01(throttle));
        entityData.set(DATA_AFTERBURNER_CHARGE, clamp01(afterburnerCharge));
        entityData.set(DATA_VTOL, vtol && jetType().supportsVtol());
        setEngineOn(engineOn);
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    /** Server: validated wall impact report from the pilot's client. */
    public void applyImpact(Player sender, float speed) {
        if (level().isClientSide || getControllingPassenger() != sender
                || impactPacketCooldown > 0 || !Float.isFinite(speed)) {
            return;
        }
        double threshold = flightConfig().crashMinImpactSpeed;
        double clamped = Math.min(speed, 4.0D);
        if (clamped <= threshold) {
            return;
        }
        float damage = (float) ((clamped - threshold) * CRASH_DAMAGE_PER_SPEED);
        impactPacketCooldown = IMPACT_PACKET_COOLDOWN_TICKS;
        hurt(level().damageSources().flyIntoWall(), damage);
        sender.hurt(level().damageSources().flyIntoWall(), damage * 0.25F);
    }

    /** Server: validated dive-crash report; the airframe is written off and the pilot thrown clear. */
    public void applyCrash(Player sender) {
        if (level().isClientSide || isRemoved() || getControllingPassenger() != sender) {
            return;
        }
        destroyAircraft();
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
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            // Splash never damages a vehicle, aircraft included: hits are priced on the hit.
            return false;
        }
        float remaining = getHealth() - amount;
        setHealth(remaining);
        if (remaining <= 0.0F) {
            destroyAircraft();
        }
        return true;
    }

    /** Writes the airframe off: the pilot is thrown clear, then the wreck goes up. Once only. */
    private void destroyAircraft() {
        if (!destruction.beginDestruction()) {
            return;
        }
        ejectCrew();
        if (level() instanceof ServerLevel server) {
            server.explode(this, getX(), getY(), getZ(), 3.5F, Level.ExplosionInteraction.NONE);
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
    // Riding
    // ------------------------------------------------------------------

    /** Right-click to board; sneak with an empty hand packs the jet back into an item. */
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
        ItemStack stack = new ItemStack(ModItems.F35.get());
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Health", getHealth());
        stack.setTag(tag);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        discard();
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

    public float getRollDeg() {
        return entityData.get(DATA_ROLL_DEG);
    }

    public float getThrottle() {
        return entityData.get(DATA_THROTTLE);
    }

    public float getAfterburnerCharge() {
        return entityData.get(DATA_AFTERBURNER_CHARGE);
    }

    public boolean isVtol() {
        return entityData.get(DATA_VTOL);
    }

    public int getWeapon() {
        return entityData.get(DATA_WEAPON);
    }

    public int getCannonState() {
        return entityData.get(DATA_CANNON_STATE);
    }

    public int getCannonHeat() {
        return entityData.get(DATA_CANNON_HEAT);
    }

    public int getCannonAmmo() {
        return entityData.get(DATA_CANNON_AMMO);
    }

    public int getRocketReady() {
        return entityData.get(DATA_ROCKET_READY);
    }

    public int getRocketReserve() {
        return entityData.get(DATA_ROCKET_RESERVE);
    }

    public int getMissileAmmo() {
        return entityData.get(DATA_MISSILE_AMMO);
    }

    public int getFlares() {
        return entityData.get(DATA_FLARES);
    }

    public int getLockTargetId() {
        return entityData.get(DATA_LOCK_TARGET);
    }

    public int getLockProgress() {
        return entityData.get(DATA_LOCK_PROGRESS);
    }

    public boolean isLocked() {
        return entityData.get(DATA_LOCKED);
    }

    /** Acquire window of the lock the given weapon slot uses, for the HUD bar. */
    public static int lockAcquireTicksFor(int weapon) {
        return weapon == WEAPON_IR_MISSILE
                ? TargetLock.Spec.IR.acquireTicks() : TargetLock.Spec.RADAR.acquireTicks();
    }

    /** Heat scale of this airframe's cannon, for the HUD bar. */
    public int cannonMaxHeat() {
        return jetType().cannon().maxHeat();
    }

    /** Roll used by the renderer: local prediction for the pilot, synced otherwise. */
    public float getRenderRollDeg() {
        if (level() != null && level().isClientSide && clientState != null
                && isControlledByLocalInstance()) {
            return clientState.rollDeg();
        }
        return getRollDeg();
    }

    /** Previous tick's render roll lerped to the current one, so banking does not step. */
    public float interpolatedRenderRollDeg(float partialTick) {
        return Mth.lerp(partialTick, renderRollO, getRenderRollDeg());
    }

    /**
     * Client render only: slaves the pilot's view to the airframe so the vanilla third person
     * camera (positioned and aimed from the player's own view rotation) rides the nose instead of
     * the mouse, and banks this frame's mouse travel for the next flight tick. Doing this every
     * rendered frame, rather than once per tick, is what stops the camera following the raw mouse
     * on the frames in between and snapping back.
     */
    public void anchorPilotView(Player pilot) {
        if (clientInputSuppressed) {
            // The pod sight has the mouse. Bank nothing and leave the player's own view alone:
            // the caller reads its travel to drive the gimbal instead.
            pendingMouseYawDeg = 0.0F;
            pendingMousePitchDeg = 0.0F;
            return;
        }
        if (Float.isNaN(lastAnchorYawDeg)) {
            lastAnchorYawDeg = pilot.getYRot();
            lastAnchorPitchDeg = pilot.getXRot();
        } else {
            pendingMouseYawDeg += HelicopterFlightModel.wrapDegrees(
                    pilot.getYRot() - lastAnchorYawDeg);
            pendingMousePitchDeg += pilot.getXRot() - lastAnchorPitchDeg;
        }
        // Copy the airframe's own interpolation endpoints onto the pilot, so the vanilla camera
        // lerp (and the rider model) reproduce the exact attitude the jet model is drawn with,
        // instead of double-interpolating a value already smoothed here.
        pilot.yRotO = yRotO;
        pilot.setYRot(getYRot());
        pilot.xRotO = xRotO;
        pilot.setXRot(getXRot());
        lastAnchorYawDeg = pilot.getYRot();
        lastAnchorPitchDeg = pilot.getXRot();
    }

    /** Throttle used by the HUD: local prediction for the pilot, synced otherwise. */
    public float getRenderThrottle() {
        if (level() != null && level().isClientSide && clientState != null
                && isControlledByLocalInstance()) {
            return clientState.throttle();
        }
        return getThrottle();
    }

    /** Burner charge used by the HUD: local prediction for the pilot, synced otherwise. */
    public float getRenderAfterburnerCharge() {
        if (level() != null && level().isClientSide && clientState != null
                && isControlledByLocalInstance()) {
            return clientState.afterburnerCharge();
        }
        return getAfterburnerCharge();
    }

    public boolean isRenderVtol() {
        if (level() != null && level().isClientSide && clientState != null
                && isControlledByLocalInstance()) {
            return clientState.vtol();
        }
        return isVtol();
    }

    /** Remaining health of a hit target as a 0..1 fraction, for the pilot's target readout. */
    public static float healthFractionOf(Entity target) {
        return HitFeedback.healthFractionOf(target);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setHealth(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        setEngineOn(tag.getBoolean("EngineOn"));
        entityData.set(DATA_THROTTLE, tag.contains("Throttle") ? tag.getFloat("Throttle") : 0.0F);
        entityData.set(DATA_AFTERBURNER_CHARGE,
                tag.contains("AfterburnerCharge") ? tag.getFloat("AfterburnerCharge") : 1.0F);
        entityData.set(DATA_VTOL, tag.getBoolean("Vtol"));
        entityData.set(DATA_WEAPON,
                tag.contains("Weapon") ? tag.getInt("Weapon") : WEAPON_CANNON);
        int savedAmmo = tag.contains("CannonAmmo")
                ? Math.min(CANNON_AMMO_CAPACITY, Math.max(0, tag.getInt("CannonAmmo")))
                : CANNON_AMMO_CAPACITY;
        cannonAmmo.refill(savedAmmo);
        entityData.set(DATA_CANNON_AMMO, cannonAmmo.rounds());
        entityData.set(DATA_ROCKET_RESERVE, tag.contains("RocketReserve")
                ? tag.getInt("RocketReserve") : RocketPodState.RESERVE_CAPACITY);
        int ready = tag.contains("RocketReady")
                ? Math.max(0, Math.min(RocketPodState.READY_CAPACITY, tag.getInt("RocketReady")))
                : RocketPodState.READY_CAPACITY;
        podState = ready <= 0 ? RocketPodState.empty()
                : RocketPodState.rearm(RocketPodState.empty(), ready);
        entityData.set(DATA_ROCKET_READY, podState.totalRemaining());
        int savedMissiles = tag.contains("MissileAmmo")
                ? Math.min(MISSILE_AMMO_CAPACITY, Math.max(0, tag.getInt("MissileAmmo")))
                : MISSILE_AMMO_CAPACITY;
        missileAmmo.refill(savedMissiles);
        entityData.set(DATA_MISSILE_AMMO, missileAmmo.rounds());
        int savedFlares = tag.contains("Flares")
                ? Math.max(0, Math.min(LittleBirdConfig.jetFlareCount(), tag.getInt("Flares")))
                : LittleBirdConfig.jetFlareCount();
        flareState = new Countermeasures.Snapshot(savedFlares, 0, 0);
        entityData.set(DATA_FLARES, flareState.flares());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        tag.putBoolean("EngineOn", isEngineOn());
        tag.putFloat("Throttle", getThrottle());
        tag.putFloat("AfterburnerCharge", getAfterburnerCharge());
        tag.putBoolean("Vtol", isVtol());
        tag.putInt("Weapon", getWeapon());
        tag.putInt("CannonAmmo", getCannonAmmo());
        tag.putInt("RocketReady", getRocketReady());
        tag.putInt("RocketReserve", getRocketReserve());
        tag.putInt("MissileAmmo", getMissileAmmo());
        tag.putInt("Flares", getFlares());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
