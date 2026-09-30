package io.github.littlebird.entity;

import io.github.littlebird.entity.flight.FlightConfig;
import io.github.littlebird.entity.flight.FlightControls;
import io.github.littlebird.entity.flight.HelicopterFlightModel;
import io.github.littlebird.entity.flight.HelicopterInput;
import io.github.littlebird.entity.flight.HelicopterInputDamping;
import io.github.littlebird.entity.flight.HelicopterRollDamper;
import io.github.littlebird.entity.flight.HelicopterState;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.AmmoSupply;
import io.github.littlebird.entity.weapon.LittleBirdGunCycle;
import io.github.littlebird.entity.weapon.RocketPodState;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.network.LittleBirdControlPacket;
import io.github.littlebird.network.LittleBirdCrashPacket;
import io.github.littlebird.network.LittleBirdImpactPacket;
import io.github.littlebird.network.LittleBirdTracerPacket;
import io.github.littlebird.network.ModNetwork;
import io.github.littlebird.registry.ModEntities;
import io.github.littlebird.registry.ModItems;
import io.github.littlebird.registry.ModSounds;
import io.github.littlebird.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import io.github.littlebird.entity.seat.LittleBirdSeat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import java.util.List;

/**
 * Little Bird light attack helicopter.
 *
 * Flight is pilot-client authoritative, exactly like vanilla boats and horses:
 * the pilot's client steps HelicopterFlightModel and moves the entity while
 * vanilla's LocalPlayer synchronises the root vehicle pose with
 * ServerboundMoveVehiclePacket. Roll is not part of that packet, so the pilot
 * client reports it through LittleBirdControlPacket and the server republishes
 * it through synched entity data. Weapons, damage and ammunition stay
 * server-authoritative in later milestones.
 */
public class LittleBirdEntity extends Entity implements GunshipRider {
    // The control bit field is shared with every other aircraft; these aliases keep the names
    // the network packets and tests already use.
    public static final int CONTROL_CLIMB = FlightControls.CLIMB;
    public static final int CONTROL_DESCEND = FlightControls.DESCEND;
    public static final int CONTROL_ROLL_RIGHT = FlightControls.ROLL_RIGHT;
    public static final int CONTROL_ROLL_LEFT = FlightControls.ROLL_LEFT;

    public static final float MAX_HEALTH = 80.0F;
    /** Roll is only reported to the server when it moves by at least this much. */
    private static final float ROLL_SYNC_THRESHOLD_DEG = 0.5F;
    /** Fraction of the speed kept after hitting a block horizontally. */
    private static final double BLOCKED_SPEED_KEEP = 0.15D;
    /** The bank is unbounded on the client, so the synced value spans the whole circle. */
    private static final float MAX_SYNCED_ROLL_DEG = 180.0F;
    private static final int IMPACT_PACKET_COOLDOWN_TICKS = 10;
    private static final int IMPACT_DAMAGE_COOLDOWN_TICKS = 10;

    private static final EntityDataAccessor<Float> DATA_ROLL_DEG =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ENGINE_ON =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_ROTOR_RPM =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_WEAPON =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SALVO =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GUN_STATE =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GUN_HEAT =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.INT);
    /** 7.62mm left in the belt. Nothing restocks it except an ammunition crate. */
    private static final EntityDataAccessor<Integer> DATA_GUN_AMMO =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROCKET_RESERVE =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROCKETS_LEFT =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROCKETS_RIGHT =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.INT);
    /** Ticks left on the reload cooldown, zero when idle. Read by the HUD. */
    private static final EntityDataAccessor<Integer> DATA_ROCKET_RELOAD =
            SynchedEntityData.defineId(LittleBirdEntity.class, EntityDataSerializers.INT);

    public static final int WEAPON_GUN = 0;
    public static final int WEAPON_ROCKETS = 1;
    /** Rounds the belt holds; the crate is the only thing that fills it. */
    public static final int GUN_AMMO_CAPACITY = 2000;
    public static final float GUN_DAMAGE = 3.5F;
    /** Hardpoint geometry, shared with the model so the two cannot drift apart. */
    private static final double MUZZLE_HEIGHT = LittleBirdGeometry.gunHeightBlocks();
    private static final double ROCKET_POD_LATERAL = LittleBirdGeometry.podLateralBlocks();
    private static final double ROCKET_POD_HEIGHT = LittleBirdGeometry.podHeightBlocks();
    private static final double ROCKET_MUZZLE_FORWARD = LittleBirdGeometry.podForwardBlocks();

    private static final FlightConfig FLIGHT = FlightConfig.defaults();
    /** Runs the write-off exactly once: see AircraftDestruction for the crash it prevents. */
    private final AircraftDestruction destruction = new AircraftDestruction();

    // Client-only prediction state.
    private HelicopterState clientState;
    private HelicopterInputDamping.State clientDamping =
            HelicopterInputDamping.State.initial(0.0F);
    private float clientRollAxis;
    private int clientControls;
    private boolean clientEngineRequested = true;
    private boolean cockpitView;
    private boolean shoulderView;
    private float lastSentRollDeg = Float.NaN;
    private boolean lastSentEngineOn;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;
    private float lerpXRot;
    private int lerpSteps;
    private LittleBirdGunCycle.Snapshot gunCycle = LittleBirdGunCycle.Snapshot.initial();
    private RocketPodState.Snapshot podState = RocketPodState.Snapshot.initial();
    /** Reload cooldown: the key starts it, the pods fill when it lands. */
    private RocketPodState.Reload rocketReload = RocketPodState.Reload.idle();
    private boolean triggerHeld;
    private int gunRoundsFired;
    private int supplyTick;
    private int impactPacketCooldown;
    private int crashPacketCooldown;
    private int impactDamageCooldown;

    public LittleBirdEntity(EntityType<? extends LittleBirdEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ROLL_DEG, 0.0F);
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_ENGINE_ON, false);
        entityData.define(DATA_ROTOR_RPM, 0.0F);
        entityData.define(DATA_WEAPON, WEAPON_GUN);
        entityData.define(DATA_SALVO, false);
        entityData.define(DATA_GUN_STATE, LittleBirdGunCycle.IDLE);
        entityData.define(DATA_GUN_HEAT, 0);
        RocketPodState.Ready ready = RocketPodState.splitReady(RocketPodState.READY_CAPACITY);
        entityData.define(DATA_GUN_AMMO, GUN_AMMO_CAPACITY);
        entityData.define(DATA_ROCKET_RESERVE, RocketPodState.RESERVE_CAPACITY);
        entityData.define(DATA_ROCKETS_LEFT, ready.left());
        entityData.define(DATA_ROCKET_RELOAD, 0);
        entityData.define(DATA_ROCKETS_RIGHT, ready.right());
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

    private void tickClient() {
        if (isControlledByLocalInstance() && getControllingPassenger() instanceof Player pilot) {
            applyLocalFlight(pilot);
        } else {
            tickRemoteInterpolation();
        }
    }

    /** Steps the flight model on the pilot's client and applies the result. */
    private void applyLocalFlight(Player pilot) {
        HelicopterState state = clientState == null
                ? HelicopterState.initial(pilot.getYRot())
                : clientState;
        state = state.withEngineOn(clientEngineRequested);

        FlightConfig flight = LittleBirdConfig.clientFlight();
        // Coordinated turn: a banked airframe yaws itself. The pilot's yaw is both the camera
        // reference and the model's heading target, so the turn is applied to the pilot and
        // to the damping filter together: the view and the nose then turn in lockstep
        // instead of the aircraft crabbing away from the crosshair.
        double bankTurn = HelicopterFlightModel.bankedTurnDegPerTick(
                Math.hypot(state.vx(), state.vz()), state.rollDeg(),
                HelicopterFlightModel.liftScale(state.rotorRpm(), flight), flight);
        if (Math.abs(bankTurn) > 1.0E-9D) {
            pilot.setYRot(HelicopterFlightModel.wrapDegrees(
                    pilot.getYRot() + (float) bankTurn));
            clientDamping = HelicopterInputDamping.rotateYaw(clientDamping, (float) bankTurn);
        }
        HelicopterInput rawInput = new HelicopterInput(
                pilot.getYRot(), pitchAxis(pilot, flight.pitchMaxDeg), rollAxis(),
                collectiveAxis(), onGround());
        clientDamping = HelicopterInputDamping.step(clientDamping, rawInput, flight);
        HelicopterInput smoothed = HelicopterInputDamping.apply(rawInput, clientDamping);
        // Roll spin-up: the damping filter shapes the command, this slews the rate itself,
        // so a roll eases in over roll_spin_up_ticks instead of snapping to full rate.
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
            // Diving into the ground writes the airframe off: this is a crash, not a landing.
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

    /**
     * Positive look pitch means looking down, which is also positive nose-down
     * in the flight model, so the pilot axis maps without a sign flip. The reference is
     * the configured pitch range, so the nose tracks the look angle one to one all the
     * way to straight up and straight down.
     */
    private static float pitchAxis(Player pilot, double referenceDeg) {
        if (referenceDeg <= 0.0D) {
            return 0.0F;
        }
        return (float) (pilot.getXRot() / referenceDeg);
    }

    private float rollAxis() {
        return FlightControls.rollAxis(clientControls);
    }

    private float collectiveAxis() {
        return FlightControls.collectiveAxis(clientControls);
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

    private void tickServer() {
        // Riding is the crew's protection: the airframe is what descends, explodes and
        // gets shot at (see LittleBirdRiderProtection for the damage half). Keeping the
        // passenger's fall distance clean also means stepping out on the ground is safe,
        // while jumping out at altitude starts a fresh - and very much lethal - fall.
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        Player pilot = getControllingPassenger() instanceof Player player ? player : null;
        setEngineOn(pilot != null);
        tickRotor();
        if (pilot == null) {
            triggerHeld = false;
            gunCycle = LittleBirdGunCycle.Snapshot.initial();
            entityData.set(DATA_GUN_STATE, LittleBirdGunCycle.IDLE);
            entityData.set(DATA_GUN_HEAT, 0);
            applyUnpilotedFall();
        } else if (level() instanceof ServerLevel server) {
            tickWeapons(server, pilot);
        }
    }

    private void tickRotor() {
        float target = isEngineOn() ? 1.0F : 0.0F;
        float rpm = (float) HelicopterFlightModel.approach(
                getRotorRpm(), target, FLIGHT.rotorSpoolRate);
        entityData.set(DATA_ROTOR_RPM, Math.max(0.0F, Math.min(1.0F, rpm)));
    }

    /** An empty aircraft falls instead of hovering in place. */
    private void applyUnpilotedFall() {
        Vec3 motion = getDeltaMovement();
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        double vy = Math.max(motion.y - FLIGHT.gravity, -FLIGHT.maxFallSpeed);
        Vec3 next = new Vec3(0.0D, vy, 0.0D);
        setDeltaMovement(next);
        move(MoverType.SELF, next);
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------
    // Client input plumbing (fed by client/LittleBirdInput)
    // ------------------------------------------------------------------

    /** Client only: packed CONTROL_* flags for the current tick. */
    public void setClientControls(int controls) {
        this.clientControls = controls;
    }

    /** Client only: engine request, false while the pilot is shutting down. */
    public void setClientEngineRequested(boolean requested) {
        this.clientEngineRequested = requested;
    }

    /** Client only: true while the pilot is using the interior camera. */
    public boolean isCockpitView() {
        return cockpitView;
    }

    /** Client only: switches between the exterior and cockpit cameras. */
    public void setCockpitView(boolean value) {
        this.cockpitView = value;
    }

    /** Client only: forgets prediction state when this aircraft is left behind. */
    public void resetClientPrediction() {
        this.cockpitView = false;
        this.shoulderView = false;
        this.clientState = null;
        this.clientDamping = HelicopterInputDamping.State.initial(0.0F);
        this.clientRollAxis = 0.0F;
        this.clientControls = 0;
        this.crashPacketCooldown = 0;
        this.clientEngineRequested = true;
        this.lastSentRollDeg = Float.NaN;
        this.lastSentEngineOn = false;
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
        setEngineOn(engineOn);
    }

    // ------------------------------------------------------------------
    // Remote interpolation (other players' clients)
    // ------------------------------------------------------------------

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot,
                       int steps, boolean teleport) {
        if (!level().isClientSide) {
            super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
            return;
        }
        if (isControlledByLocalInstance()) {
            return;
        }
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = Math.max(1, steps);
    }

    private void tickRemoteInterpolation() {
        if (lerpSteps <= 0) {
            return;
        }
        double fraction = 1.0D / lerpSteps;
        setPos(
                Mth.lerp(fraction, getX(), lerpX),
                Mth.lerp(fraction, getY(), lerpY),
                Mth.lerp(fraction, getZ(), lerpZ));
        setYRot((float) Mth.rotLerp((float) fraction, getYRot(), lerpYRot));
        setXRot((float) Mth.lerp(fraction, getXRot(), lerpXRot));
        --lerpSteps;
    }

    // ------------------------------------------------------------------
    // Weapons (server authoritative)
    // ------------------------------------------------------------------

    /** Server: validated weapon intent reported by the pilot's client. */
    public void applyWeaponInput(
            Player sender, int weapon, boolean salvoMode, boolean held) {
        if (level().isClientSide || getControllingPassenger() != sender) {
            return;
        }
        entityData.set(DATA_WEAPON, weapon == WEAPON_ROCKETS ? WEAPON_ROCKETS : WEAPON_GUN);
        entityData.set(DATA_SALVO, salvoMode);
        this.triggerHeld = held;
    }

    /** Server: validated crash report from the pilot's client. */
    public void applyImpact(Player sender, float speed) {
        if (level().isClientSide || getControllingPassenger() != sender
                || impactDamageCooldown > 0 || !Float.isFinite(speed)) {
            return;
        }
        double threshold = FLIGHT.crashSpeedThreshold;
        double clamped = Math.min(speed, 3.0D);
        if (clamped <= threshold) {
            return;
        }
        float damage = (float) ((clamped - threshold) * FLIGHT.crashDamagePerSpeed);
        impactDamageCooldown = IMPACT_DAMAGE_COOLDOWN_TICKS;
        hurt(level().damageSources().flyIntoWall(), damage);
        sender.hurt(level().damageSources().flyIntoWall(), damage * 0.25F);
    }

    private void tickWeapons(ServerLevel server, Player pilot) {
        if (impactDamageCooldown > 0) {
            --impactDamageCooldown;
        }

        boolean firingGun = triggerHeld && getWeapon() == WEAPON_GUN;
        LittleBirdGunCycle.Step gunStep = LittleBirdGunCycle.advance(firingGun, gunCycle);
        gunCycle = gunStep.snapshot();
        entityData.set(DATA_GUN_STATE, gunCycle.state());
        entityData.set(DATA_GUN_HEAT, gunCycle.heat());
        if (gunStep.fire() && getGunAmmo() > 0) {
            fireGun(server, pilot, gunStep.barrel());
            entityData.set(DATA_GUN_AMMO, getGunAmmo() - 1);
        }

        boolean firingRockets = triggerHeld && getWeapon() == WEAPON_ROCKETS;
        RocketPodState.Step podStep = RocketPodState.advance(podState, firingRockets, isSalvoMode());
        podState = podStep.snapshot();
        entityData.set(DATA_ROCKETS_LEFT, podState.leftRemaining());
        entityData.set(DATA_ROCKETS_RIGHT, podState.rightRemaining());
        advanceRocketReload(server);
        if (podStep.launch()) {
            launchRocket(server, pilot, podStep.pod(), podStep.tube());
        }
        tickAmmoSupply(server);
    }

    /**
     * Ammunition comes back from one place only: an ammunition crate parked next to the
     * aircraft. The old free rearm timer is gone, so a long fight means going home.
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
        int bullets = supply.bullets(20);
        int rockets = supply.rockets(20);
        int belt = Math.min(GUN_AMMO_CAPACITY, getGunAmmo() + bullets);
        int reserve = Math.min(RocketPodState.RESERVE_CAPACITY, getRocketReserve() + rockets);
        if (belt == getGunAmmo() && reserve == getRocketReserve()) {
            return;
        }
        entityData.set(DATA_GUN_AMMO, belt);
        entityData.set(DATA_ROCKET_RESERVE, reserve);
    }

    /** Server: the reload key. Moves reserve rounds into the pods, if there are any. */
    /**
     * Server: the reload key. Starts the reserve-to-pod transfer; the cooldown then owns it, so a
     * second press while the clock runs does nothing and the rounds only appear when it lands.
     */
    public void applyReload(Player sender) {
        if (level().isClientSide || getControllingPassenger() != sender) {
            return;
        }
        if (rocketReload.running()) {
            return;
        }
        int missing = RocketPodState.READY_CAPACITY - getRocketsRemaining();
        int reserve = getRocketReserve();
        if (missing <= 0 || reserve <= 0) {
            return;
        }
        rocketReload = RocketPodState.begin(rocketReload,
                LittleBirdConfig.littleBirdRocketReloadTicks(), missing, reserve);
        if (rocketReload.running()) {
            entityData.set(DATA_ROCKET_RELOAD, rocketReload.ticksLeft());
            if (level() instanceof ServerLevel server) {
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS, 0.4F, 0.7F);
            }
            return;
        }
        // Zero tick reload: the config asked for the old instant behaviour.
        if (level() instanceof ServerLevel server) {
            fillPodsFromReserve(server);
        }
    }

    /** Advances the reload cooldown and fills the pods on the tick it lands. */
    private void advanceRocketReload(ServerLevel server) {
        if (!rocketReload.running()) {
            return;
        }
        RocketPodState.Reload before = rocketReload;
        rocketReload = RocketPodState.tick(rocketReload);
        entityData.set(DATA_ROCKET_RELOAD, rocketReload.ticksLeft());
        if (RocketPodState.completes(before, rocketReload)) {
            fillPodsFromReserve(server);
        }
    }

    /** Moves what the pods are missing out of the reserve and reports it to the pilot. */
    private void fillPodsFromReserve(ServerLevel server) {
        int moved = RocketPodState.transferAmount(podState.totalRemaining(), getRocketReserve());
        if (moved <= 0) {
            return;
        }
        podState = RocketPodState.rearm(podState, moved);
        entityData.set(DATA_ROCKETS_LEFT, podState.leftRemaining());
        entityData.set(DATA_ROCKETS_RIGHT, podState.rightRemaining());
        entityData.set(DATA_ROCKET_RESERVE, getRocketReserve() - moved);
        entityData.set(DATA_ROCKET_RELOAD, 0);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.6F, 1.4F);
    }


    private void fireGun(ServerLevel server, Player pilot, int barrel) {
        Vec3 aim = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
        Vec3 origin = position().add(0.0D, MUZZLE_HEIGHT, 0.0D);
        LittleBirdBallistics.Vector3 center =
                LittleBirdBallistics.muzzleCenter(vector(origin), vector(aim));
        LittleBirdBallistics.Vector3 tube =
                LittleBirdBallistics.muzzle(center, vector(right), barrel);
        LittleBirdBallistics.Vector3 convergence =
                LittleBirdBallistics.convergencePoint(center, vector(aim));
        LittleBirdBallistics.Vector3 spread = LittleBirdBallistics.convergingDirection(
                tube, convergence);
        spread = LittleBirdBallistics.spreadDirection(spread,
                LittleBirdBallistics.spreadDegrees(gunCycle.heat()),
                random.nextDouble(), random.nextDouble());
        Vec3 muzzle = vec(tube);
        Vec3 direction = vec(spread);
        Vec3 far = muzzle.add(direction.scale(LittleBirdBallistics.RANGE));

        BlockHitResult blockHit = server.clip(new ClipContext(
                muzzle, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 impact = blockHit.getType() == HitResult.Type.MISS ? far : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(server, this, muzzle, impact,
                getBoundingBox().expandTowards(direction.scale(muzzle.distanceTo(impact))).inflate(1.0D),
                candidate -> candidate != this && candidate != pilot
                        && candidate.isAlive() && !candidate.isSpectator());
        if (entityHit != null) {
            impact = entityHit.getLocation();
            // Every airframe is fair game now, and each one is priced on its own hull: a round
            // costs an aircraft a fraction of a point instead of the full anti-personnel round.
            LittleBirdBallistics.Airframe airframe =
                    AirframeTargets.classify(entityHit.getEntity());
            // These are machine gun rounds: they chew up aircraft and infantry, and a tracked hull
            // absorbs them completely.
            float damage = LittleBirdBallistics.damageTo(
                    io.github.littlebird.entity.weapon.DamageType.BULLET,
                    AirframeTargets.armorClass(entityHit.getEntity()), airframe,
                    LittleBirdConfig.gunDamage(),
                    LittleBirdConfig.gunDamageAircraft(),
                    LittleBirdConfig.gunDamageApache());
            boolean applied = entityHit.getEntity()
                    .hurt(pilot.damageSources().playerAttack(pilot), damage);
            if (applied && pilot instanceof ServerPlayer serverPlayer) {
                HitFeedback.report(serverPlayer, entityHit.getEntity());
            }
        }

        gunRoundsFired++;
        // The bullet itself is a client-side tracer: one packet per round tells every
        // tracking client where it started and where it stopped.
        int impactType = entityHit != null
                ? LittleBirdTracerPacket.IMPACT_ENTITY
                : blockHit.getType() == HitResult.Type.MISS
                        ? LittleBirdTracerPacket.IMPACT_MISS
                        : LittleBirdTracerPacket.IMPACT_BLOCK;
        ModNetwork.sendTracer(this, new LittleBirdTracerPacket(
                muzzle.x, muzzle.y, muzzle.z, impact.x, impact.y, impact.z,
                impactType, barrel));
        server.sendParticles(ParticleTypes.SMOKE,
                muzzle.x, muzzle.y, muzzle.z, 1, 0.03D, 0.03D, 0.03D, 0.002D);
        // The gun report itself is a looping client sound driven by the synced gun
        // state (LittleBirdSoundManager), so nothing is sent per round from here.
    }

    private static LittleBirdBallistics.Vector3 vector(Vec3 value) {
        return new LittleBirdBallistics.Vector3(value.x, value.y, value.z);
    }

    private static Vec3 vec(LittleBirdBallistics.Vector3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }

    private void launchRocket(ServerLevel server, Player pilot, int pod, int tube) {
        Vec3 aim = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
        double lateral = (pod == 0 ? -1.0D : 1.0D) * ROCKET_POD_LATERAL
                + (tube - 3.0D) * 0.05D;
        double vertical = ((tube % 2) - 0.5D) * 0.05D;
        Vec3 origin = position()
                .add(0.0D, ROCKET_POD_HEIGHT + vertical, 0.0D)
                .add(right.scale(lateral))
                .add(aim.scale(ROCKET_MUZZLE_FORWARD));

        LittleBirdRocketEntity rocket = new LittleBirdRocketEntity(
                ModEntities.LITTLE_BIRD_ROCKET.get(), level());
        rocket.setPos(origin.x, origin.y, origin.z);
        rocket.launch(aim, LittleBirdRocketEntity.INITIAL_SPEED, pilot);
        server.addFreshEntity(rocket);
        server.playSound(null, getX(), getY(), getZ(), ModSounds.ROCKET_LAUNCH.get(),
                SoundSource.PLAYERS, 1.4F, 1.0F);
    }

    // ------------------------------------------------------------------
    // State accessors
    // ------------------------------------------------------------------

    public float getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    /** Also used by the deploy item, so a packed airframe keeps the health it was packed at. */
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

    private void setEngineOn(boolean engineOn) {
        entityData.set(DATA_ENGINE_ON, engineOn);
    }

    public float getRotorRpm() {
        return entityData.get(DATA_ROTOR_RPM);
    }

    public int getWeapon() {
        return entityData.get(DATA_WEAPON);
    }

    public boolean isSalvoMode() {
        return entityData.get(DATA_SALVO);
    }

    public int getGunState() {
        return entityData.get(DATA_GUN_STATE);
    }

    public int getGunHeat() {
        return entityData.get(DATA_GUN_HEAT);
    }

    public int getRocketsLeft() {
        return entityData.get(DATA_ROCKETS_LEFT);
    }

    public int getRocketsRight() {
        return entityData.get(DATA_ROCKETS_RIGHT);
    }

    public int getRocketsRemaining() {
        return getRocketsLeft() + getRocketsRight();
    }

    public int getGunAmmo() {
        return entityData.get(DATA_GUN_AMMO);
    }

    public int getRocketReserve() {
        return entityData.get(DATA_ROCKET_RESERVE);
    }

    /** Ticks left on the rocket reload cooldown, zero when no reload is running. */
    public int getRocketReloadTicks() {
        return entityData.get(DATA_ROCKET_RELOAD);
    }

    /** 0..1 progress of the running reload, for the HUD bar. */
    public float getRocketReloadFraction() {
        int total = LittleBirdConfig.littleBirdRocketReloadTicks();
        if (total <= 0) {
            return 0.0F;
        }
        return 1.0F - Math.min(total, Math.max(0, getRocketReloadTicks())) / (float) total;
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

    /** Rotor spin factor from the vertical rate: idling in the hover, fast on climb. */
    public float getRenderRotorSpinFactor() {
        return (float) HelicopterFlightModel.rotorSpinFactor(
                Math.abs(getDeltaMovement().y), FLIGHT);
    }

    public float getRenderRotorRpm() {
        if (level() != null && level().isClientSide && clientState != null
                && isControlledByLocalInstance()) {
            return clientState.rotorRpm();
        }
        return getRotorRpm();
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
        // A 70mm rocket bills an airframe once, on the hit itself: the warhead's own blast
        // must not charge the hull a second time, or one rocket would be worth several hits.
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

    /**
     * Server: validated dive-crash report. The airframe is written off - which throws the crew
     * clear and then explodes the wreck. The brief invulnerability the eject grants matters
     * here: riding immunity ends the moment they step off, and the blast is right under them.
     */
    public void applyCrash(Player sender) {
        if (level().isClientSide || isRemoved() || getControllingPassenger() != sender) {
            return;
        }
        destroyAircraft();
    }

    /**
     * Remaining health of a hit target as a 0..1 fraction, for the pilot's target readout.
     * Airframes are measured against their own maximum, so a fresh aircraft reads 100%.
     */
    public static float healthFractionOf(Entity target) {
        return HitFeedback.healthFractionOf(target);
    }

    /** Writes the airframe off: the crew is thrown clear, then the wreck goes up. Once only. */
    private void destroyAircraft() {
        if (!destruction.beginDestruction()) {
            return;
        }
        ejectCrew();
        if (level() instanceof ServerLevel server) {
            server.explode(this, getX(), getY(), getZ(), 3.0F, Level.ExplosionInteraction.NONE);
        }
        // Scrap after the blast, not before: anything dropped first would be blown up with it.
        spawnAtLocation(Items.IRON_INGOT, 2 + random.nextInt(3));
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

    // ------------------------------------------------------------------
    // Riding
    // ------------------------------------------------------------------

    /**
     * Right-click to board, the way a boat works. Sneaking counts as the
     * secondary action and is left to the item in hand, so shift-right-click on
     * the airframe does not mount it.
     */
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
        ItemStack stack = new ItemStack(ModItems.LITTLE_BIRD.get());
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Health", getHealth());
        stack.setTag(tag);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        discard();
    }


    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < LittleBirdSeat.SEATS && passenger instanceof Player;
    }

    /** Seat index of a rider: 0 is the pilot, or -1 if they are not aboard. */
    public int seatOf(Entity passenger) {
        return getPassengers().indexOf(passenger);
    }

    /**
     * True for the pilot only. Passengers ride along: they cannot fly, shoot, reload or use the
     * cockpit camera, and the server refuses their control packets anyway.
     */
    public boolean isPilot(Entity passenger) {
        return seatOf(passenger) == LittleBirdSeat.PILOT;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    /**
     * Seat height. In cockpit view the offset is the camera height instead, so the
     * eye clears the nose deck. That variant is client only, so the server always
     * keeps the pilot at the normal seat above ground.
     */
    @Override
    public double getPassengersRidingOffset() {
        if (isCockpitCamera()) {
            return LittleBirdGeometry.cockpitRiderOffsetBlocks(
                    (float) LittleBirdConfig.cockpitEyeLiftBlocks());
        }
        return LittleBirdGeometry.riderOffsetBlocks();
    }

    /**
     * Cockpit view also moves the rider forward, past the windscreen frame and the
     * dashboard, which is what turns those parts from an obstruction into a frame
     * that sits behind and below the camera.
     */
    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        if (isCockpitCamera() && isPilot(passenger)) {
            // The eye sits ahead of the cabin along the airframe's own nose axis, so it
            // follows the nose up and down as well as around; without the pitch term a
            // ninety degree dive would leave the camera hanging outside the airframe.
            LittleBirdCameraMath.Offset seat = LittleBirdCameraMath.cockpitSeatOffset(
                    getYRot(), getXRot(), LittleBirdGeometry.cockpitSeatForwardBlocks());
            moveFunction.accept(passenger,
                    getX() + seat.x(),
                    getY() + getPassengersRidingOffset()
                            + passenger.getMyRidingOffset() + seat.y(),
                    getZ() + seat.z());
            return;
        }
        if (isShoulderCamera() && isPilot(passenger)) {
            // Third person orbits the hidden rider, so shifting the rider sideways
            // moves the camera the same way and the airframe slides off centre.
            LittleBirdCameraMath.Offset offset =
                    LittleBirdCameraMath.shoulderOffset(
                            getYRot(),
                            LittleBirdConfig.shoulderOffsetBlocks(),
                            LittleBirdConfig.shoulderHeightBlocks());
            moveFunction.accept(passenger,
                    getX() + offset.x(),
                    getY() + getPassengersRidingOffset()
                            + passenger.getMyRidingOffset() + offset.y(),
                    getZ() + offset.z());
            return;
        }
        // Seated: the pilot up front, the passengers on the benches behind. The offsets are
        // rotated into the airframe's own axes so a passenger stays in their seat through a roll.
        int seat = seatOf(passenger);
        double forward = LittleBirdSeat.forwardBlocks(seat);
        double lateral = LittleBirdSeat.lateralBlocks(seat);
        double yaw = Math.toRadians(getYRot());
        moveFunction.accept(passenger,
                getX() - Math.sin(yaw) * forward - Math.cos(yaw) * lateral,
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                getZ() + Math.cos(yaw) * forward - Math.sin(yaw) * lateral);
    }

    /** Client only: true while this aircraft renders through the cockpit camera. */
    private boolean isCockpitCamera() {
        return cockpitView && level() != null && level().isClientSide;
    }

    /** Client only: true while the over-the-shoulder third person camera is held. */
    public void setShoulderView(boolean value) {
        this.shoulderView = value;
    }

    private boolean isShoulderCamera() {
        return shoulderView && !cockpitView && level() != null && level().isClientSide;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3.0D, 1.5D, 3.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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
        entityData.set(DATA_WEAPON, tag.getInt("Weapon"));
        entityData.set(DATA_SALVO, tag.getBoolean("Salvo"));
        entityData.set(DATA_GUN_AMMO, tag.contains("GunAmmo")
                ? tag.getInt("GunAmmo") : GUN_AMMO_CAPACITY);
        entityData.set(DATA_ROCKET_RESERVE, tag.contains("RocketReserve")
                ? tag.getInt("RocketReserve") : RocketPodState.RESERVE_CAPACITY);
        RocketPodState.Ready ready = RocketPodState.splitReady(RocketPodState.READY_CAPACITY);
        entityData.set(DATA_ROCKETS_LEFT, tag.contains("RocketsLeft")
                ? tag.getInt("RocketsLeft") : ready.left());
        entityData.set(DATA_ROCKETS_RIGHT, tag.contains("RocketsRight")
                ? tag.getInt("RocketsRight") : ready.right());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        tag.putBoolean("EngineOn", isEngineOn());
        tag.putFloat("RotorRpm", getRotorRpm());
        tag.putInt("Weapon", getWeapon());
        tag.putBoolean("Salvo", isSalvoMode());
        tag.putInt("GunAmmo", getGunAmmo());
        tag.putInt("RocketReserve", getRocketReserve());
        tag.putInt("RocketsLeft", getRocketsLeft());
        tag.putInt("RocketsRight", getRocketsRight());
    }
}
