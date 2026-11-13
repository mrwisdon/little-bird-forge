package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.ground.GroundDrive;
import io.github.littlebird.entity.ground.GroundMover;
import io.github.littlebird.entity.ground.GroundSuspension;
import io.github.littlebird.entity.ground.SlopeTracker;
import io.github.littlebird.entity.seat.GroundSeat;
import io.github.littlebird.entity.weapon.Phl191Ballistics;
import io.github.littlebird.entity.weapon.Phl191Effects;
import io.github.littlebird.registry.ModEntities;
import io.github.littlebird.registry.ModItems;
import io.github.littlebird.registry.ModSounds;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
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
 * PHL-191 8x370mm box launcher on a WS2400 8x8.
 *
 * <p>A ground hull like the tank: the driver's client predicts the drive, the server owns the
 * launcher. The launcher follows the driver's look (up to 65 degrees) and a held trigger ripples
 * the eight tubes out six ticks apart; the empty pods reload themselves after thirty seconds.
 * Rockets are real flying entities (not hitscan), so the smoke trail and the impact are something
 * the whole battlefield can watch.
 *
 * <p>The drive deliberately reuses {@link GroundDrive}; the launcher, the tube carousel and the
 * reload clock are this hull's own.
 */
public class Phl191Entity extends Entity
        implements GunshipRider, ArtilleryPlatform {
    public static final float MAX_HEALTH = 160.0F;

    private static final float FALL_GRAVITY = 0.08F;
    private static final double MAX_FALL_SPEED = 1.0D;
    private static final double GROUND_CONTACT_PROBE = 0.05D;

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.FLOAT);
    /** Launcher elevation in degrees, 5..65. */
    private static final EntityDataAccessor<Float> DATA_ELEVATION =
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.FLOAT);
    /** Rockets left in the pods, 0..8. */
    private static final EntityDataAccessor<Integer> DATA_TUBES =
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.INT);
    /** Ticks until the pods are reloaded; zero while there is nothing to wait for. */
    private static final EntityDataAccessor<Integer> DATA_RELOAD =
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.INT);
    /** True once the reload QTE has been scored, so the HUD drops the sweeping bar. */
    private static final EntityDataAccessor<Boolean> DATA_QTE_DONE =
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.BOOLEAN);
    private static final ArtilleryStrike.Data STRIKE_DATA = new ArtilleryStrike.Data(
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.FLOAT),
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.FLOAT),
            SynchedEntityData.defineId(Phl191Entity.class, EntityDataSerializers.INT));

    private final ArtilleryStrike strike = new ArtilleryStrike(this, STRIKE_DATA, ArtilleryStrike.Kind.PHL191);
    private final AircraftDestruction destruction = new AircraftDestruction();
    private final SlopeTracker slopeTracker = new SlopeTracker();
    private final GroundSuspension suspension = new GroundSuspension(
            Phl191Geometry.AXLE_FORWARD_UNITS, Phl191Geometry.TRACK_HALF_WIDTH_UNITS, 0.24D);
    /** Client prediction state; null whenever this client is not the driver. */
    private GroundDrive.State clientDrive;
    private int clientControls;
    /** Smooths the drawn launcher between the 20 Hz synched angle and the frame rate. */
    private float renderedElevation;
    private float previousRenderedElevation;

    private double elevation = Phl191Geometry.TRAVEL_ELEVATION_DEG;
    private int tubes = Phl191Ballistics.TUBES;
    private int nextTube;
    private int fireCooldown;
    private int reloadTicks;
    private boolean triggerHeld;
    private boolean elevationUp;
    private boolean elevationDown;
    private int supplyTick;

    public Phl191Entity(EntityType<? extends Phl191Entity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_ELEVATION, (float) Phl191Geometry.TRAVEL_ELEVATION_DEG);
        entityData.define(DATA_TUBES, Phl191Ballistics.TUBES);
        entityData.define(DATA_RELOAD, 0);
        entityData.define(DATA_QTE_DONE, false);
        STRIKE_DATA.define(entityData);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickClient();
            GroundMover.updateSuspension(this, suspension);
        } else {
            GroundMover.updateSuspension(this, suspension);
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
        Player driver = driver();
        if (driver != null) {
            // The launcher is laid by the two keys, not by the look pitch: Z raises, X lowers, and
            // the angle stays where it is put when neither is held.
            int direction = (elevationUp ? 1 : 0) - (elevationDown ? 1 : 0);
            elevation = Phl191Ballistics.slewBy(elevation, direction);
        } else {
            triggerHeld = false;
            elevationUp = false;
            elevationDown = false;
            elevation = Phl191Ballistics.slew(elevation, Phl191Geometry.TRAVEL_ELEVATION_DEG);
        }
        entityData.set(DATA_ELEVATION, (float) elevation);

        if (reloadTicks > 0) {
            reloadTicks--;
            if (reloadTicks <= 0) {
                tubes = Phl191Ballistics.TUBES;
                nextTube = 0;
                entityData.set(DATA_QTE_DONE, false);
                if (level() instanceof ServerLevel soundServer) {
                    soundServer.playSound(null, getX(), getY(), getZ(),
                            ModSounds.RELOAD_CLANK.get(), SoundSource.PLAYERS, 1.6F, 0.8F);
                }
            } else if (!entityData.get(DATA_QTE_DONE)) {
                // One tick per sweep so the player can hear the window coming up.
                int phase = (Phl191Ballistics.RELOAD_TICKS - reloadTicks) % Phl191Ballistics.QTE_TICKS;
                if (phase == Phl191Ballistics.QTE_WINDOW_START
                        && level() instanceof ServerLevel soundServer) {
                    soundServer.playSound(null, getX(), getY(), getZ(),
                            ModSounds.QTE_TICK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            entityData.set(DATA_RELOAD, reloadTicks);
        }
        entityData.set(DATA_TUBES, tubes);
        // Belt and braces: a reload that somehow reached zero with empty tubes still refills here,
        // so the hull can never sit at 0/8 with nothing counting down.
        if (reloadTicks == 0 && tubes <= 0) {
            tubes = Phl191Ballistics.TUBES;
            nextTube = 0;
            entityData.set(DATA_TUBES, tubes);
            entityData.set(DATA_QTE_DONE, false);
        }

        if (fireCooldown > 0) {
            fireCooldown--;
        }
        if (driver != null && triggerHeld && fireCooldown <= 0 && tubes > 0
                && level() instanceof ServerLevel server) {
            fireOne(server, driver);
            fireCooldown = Phl191Ballistics.FIRE_INTERVAL_TICKS;
        }
        if (level() instanceof ServerLevel strikeServer) {
            strike.tick(strikeServer);
        }
        if (getControllingPassenger() == null && !isRemoved()) {
            applyUnpilotedMotion();
        }
    }

    /** One rocket out of the next tube. */
    private void fireOne(ServerLevel server, Player driver) {
        int tube = nextTube;
        nextTube = (nextTube + 1) % Phl191Ballistics.TUBES;
        tubes--;
        // Salvo dispersion is a firing-angle deviation from the hull's heading: each tube leaves on
        // its own bearing, and the high row of the grid also carries a small elevation tolerance,
        // so the ripple fans out sideways and slightly in depth.
        double launchElevation = elevation;
        double spread = LittleBirdConfig.phl191SpreadRadius();
        double tolerance = LittleBirdConfig.phl191PitchToleranceDeg();
        if (tolerance > 0.0D) {
            launchElevation = Phl191Ballistics.clampElevation(elevation
                    + Phl191Ballistics.salvoPitchTrim(tube, Phl191Ballistics.TUBES, tolerance,
                            random.nextDouble()));
        }
        float launchYaw = getYRot();
        if (spread > 0.0D) {
            double lateral = Phl191Ballistics.salvoLateralOffset(tube, Phl191Ballistics.TUBES,
                    spread, random.nextDouble());
            double range = Phl191Ballistics.rangeAt(launchElevation);
            launchYaw = (float) (getYRot()
                    + Phl191Ballistics.yawOffsetForLateral(range, lateral));
        }
        Phl191Ballistics.V3 direction = Phl191Ballistics.direction(launchYaw, launchElevation);
        Vec3 spawn = muzzleOrigin(tube, elevation);
        Phl191RocketEntity rocket = new Phl191RocketEntity(ModEntities.PHL191_ROCKET.get(), server);
        rocket.setPos(spawn.x, spawn.y, spawn.z);
        rocket.launch(new Vec3(direction.x(), direction.y(), direction.z()),
                new Vec3(direction.x(), direction.y(), direction.z()), driver);
        server.addFreshEntity(rocket);
        Phl191Effects.sound(server, getX(), getY(), getZ(), ModSounds.PHL_LAUNCH.get(),
                (float) (0.9D + tube * 0.025D), 1.0F);
        // Muzzle blast: a short cone of fire and smoke out of the tube just used.
        Vec3 nozzle = spawn.add(new Vec3(direction.x(), direction.y(), direction.z()).scale(0.6D));
        Phl191Effects.particles(server, ParticleTypes.FLAME, nozzle.x, nozzle.y, nozzle.z, 14,
                direction.x() * 0.5D, direction.y() * 0.5D, direction.z() * 0.5D, 0.06D);
        Phl191Effects.particles(server, ParticleTypes.LAVA, nozzle.x, nozzle.y, nozzle.z, 4,
                0.3D, 0.3D, 0.3D, 0.0D);
        Phl191Effects.particles(server, ParticleTypes.END_ROD, nozzle.x, nozzle.y, nozzle.z, 3,
                direction.x() * 0.3D, direction.y() * 0.3D, direction.z() * 0.3D, 0.02D);
        Phl191Effects.particles(server, ParticleTypes.CAMPFIRE_COSY_SMOKE, nozzle.x, nozzle.y,
                nozzle.z, 18, 0.45D, 0.45D, 0.45D, 0.02D);
        // Back-blast: dust kicked out from under the wheels.
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 back = position().add(forward.scale(-3.4D));
        Phl191Effects.particles(server, ParticleTypes.CLOUD, back.x, 0.3D, back.z, 12,
                forward.x * -0.4D, 0.02D, forward.z * -0.4D, 0.03D);
        if (tubes <= 0) {
            reloadTicks = Phl191Ballistics.RELOAD_TICKS;
            entityData.set(DATA_QTE_DONE, false);
        }
        entityData.set(DATA_TUBES, tubes);
        entityData.set(DATA_RELOAD, reloadTicks);
    }

    /** Nobody driving: a hull on the ground stays put, one in the air falls. */
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
        previousRenderedElevation = renderedElevation;
        renderedElevation = getElevationDeg();
        if (!(isControlledByLocalInstance() && getControllingPassenger() instanceof Player)) {
            clientDrive = null;
            slopeTracker.reset();
            return;
        }
        GroundDrive.State prior = new GroundDrive.State(
                clientDrive == null ? 0.0D : clientDrive.speedBlocksPerTick(), getYRot());
        GroundDrive.Step step = GroundDrive.advance(prior, GroundDrive.throttle(clientControls),
                GroundDrive.steer(clientControls), LittleBirdConfig.phl191DriveSpec(), slopeTracker.gradeDegrees());
        clientDrive = step.state();
        setYRot((float) step.state().yawDegrees());
        setYHeadRot(getYRot());
        double dy = onGround() ? -GROUND_CONTACT_PROBE
                : Math.max(getDeltaMovement().y - FALL_GRAVITY, -MAX_FALL_SPEED);
        Vec3 drive = new Vec3(step.dx(), dy, step.dz());
        GroundMover.moveDrive(this, drive, LittleBirdConfig.tankStepHeightBlocks(), slopeTracker);
    }

    @Override
    public void setClientControls(int controls) {
        this.clientControls = controls;
    }

    @Override
    public void resetClientPrediction() {
        clientDrive = null;
        clientControls = 0;
        slopeTracker.reset();
    }

    /** Server: the driver holds Z (up) / X (down) on the launcher. */
    public void setElevationInput(boolean up, boolean down) {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        this.elevationUp = up;
        this.elevationDown = down;
    }

    /** Server: the driver reports the trigger and the reload key. */
    @Override
    public void applyWeaponInput(Player sender, boolean triggerHeld, boolean reload,
                                 int shellDelta, int weaponDelta) {
        if (level().isClientSide || isRemoved() || sender != driver()) {
            return;
        }
        this.triggerHeld = triggerHeld;
        if (!reload) {
            return;
        }
        if (reloadTicks > 0) {
            // Quick-time event: the two second bar keeps sweeping until it is scored. A press in
            // the 1.6 - 1.8 second band takes fifteen seconds off and ends the event; a press in
            // any other part of the sweep is mistimed and restarts the twenty second reload.
            if (!entityData.get(DATA_QTE_DONE)) {
                if (Phl191Ballistics.qteInWindow(reloadTicks)) {
                    reloadTicks = Phl191Ballistics.qteReward(reloadTicks);
                    entityData.set(DATA_QTE_DONE, true);
                    if (level() instanceof ServerLevel soundServer) {
                        soundServer.playSound(null, getX(), getY(), getZ(),
                                ModSounds.QTE_HIT.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
                    }
                } else {
                    reloadTicks = Phl191Ballistics.RELOAD_TICKS;
                    if (level() instanceof ServerLevel soundServer) {
                        soundServer.playSound(null, getX(), getY(), getZ(),
                                ModSounds.RELOAD_CLANK.get(), SoundSource.PLAYERS, 1.6F, 0.55F);
                    }
                }
                entityData.set(DATA_RELOAD, reloadTicks);
            }
            return;
        }
        if (tubes < Phl191Ballistics.TUBES) {
            reloadTicks = Phl191Ballistics.RELOAD_TICKS;
            entityData.set(DATA_RELOAD, reloadTicks);
            entityData.set(DATA_QTE_DONE, false);
            if (level() instanceof ServerLevel soundServer) {
                soundServer.playSound(null, getX(), getY(), getZ(),
                        ModSounds.RELOAD_CLANK.get(), SoundSource.PLAYERS, 1.4F, 0.7F);
            }
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
        ItemStack stack = new ItemStack(ModItems.PHL191.get());
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
        return Phl191Geometry.seatHeightBlocks(GroundSeat.PHL191_DRIVER);
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
        double forward = Phl191Geometry.seatForwardBlocks(seat);
        double lateral = Phl191Geometry.seatLateralBlocks(seat);
        double yaw = Math.toRadians(getYRot());
        double dx = forward * -Math.sin(yaw) + lateral * -Math.cos(yaw);
        double dz = forward * Math.cos(yaw) + lateral * -Math.sin(yaw);
        moveFunction.accept(passenger,
                getX() + dx,
                getY() + Phl191Geometry.seatHeightBlocks(seat) + passenger.getMyRidingOffset(),
                getZ() + dz);
    }

    @Override
    public net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return driver();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < Phl191Geometry.SEATS && passenger instanceof Player;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public int seatOf(Entity passenger) {
        int index = getPassengers().indexOf(passenger);
        return index < 0 ? -1 : GroundSeat.seatOf(index, Phl191Geometry.SEATS);
    }

    @Override
    public boolean isDriver(int seat) {
        return GroundSeat.isPhl191Driver(seat);
    }

    @Override
    public boolean isOperator(int seat) {
        return GroundSeat.isPhl191Driver(seat);
    }

    public Player driver() {
        return getPassengers().isEmpty() || !(getPassengers().get(0) instanceof Player player)
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
            // miss can never out-damage a direct hit.
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
            server.explode(this, getX(), getY(), getZ(), 4.0F, Level.ExplosionInteraction.NONE);
        }
        spawnAtLocation(Items.IRON_INGOT, 6 + random.nextInt(5));
        discard();
    }

    private void ejectCrew() {
        Vec3 motion = getDeltaMovement();
        for (Entity passenger : List.copyOf(getPassengers())) {
            passenger.stopRiding();
            passenger.resetFallDistance();
            passenger.invulnerableTime = 20;
            passenger.setDeltaMovement(motion.scale(0.4D).add(0.0D, 0.3D, 0.0D));
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
    // State
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
        entityData.set(DATA_HEALTH, health);
    }

    @Override
    public float healthFraction() {
        return Math.max(0.0F, Math.min(1.0F, getHealth() / MAX_HEALTH));
    }

    public GroundSuspension suspension() {
        return suspension;
    }

    /** World-space muzzle shared by the launch and the aiming preview. */
    public Vec3 muzzleOrigin(int tube, double elevation) {
        Phl191Geometry.Muzzle mouth = Phl191Geometry.muzzle(tube, elevation);
        float yaw = getYRot();
        double yawRadians = Math.toRadians(yaw);
        Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
        Vec3 left = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3 mountOffset = GroundMover.suspensionMountOffset(suspension.pose(1.0F), yaw,
                Phl191Geometry.trunnionForwardBlocks(), Phl191Geometry.trunnionHeightBlocks());
        return position()
                .add(mountOffset)
                .add(forward.scale(mouth.forwardBlocks()))
                .add(left.scale(mouth.lateralBlocks()))
                .add(0.0D, mouth.upBlocks(), 0.0D);
    }

    public float getElevationDeg() {
        return entityData.get(DATA_ELEVATION);
    }

    /** Smoothed launcher angle for the renderer. */
    public float renderElevationDeg(float partialTick) {
        float current = phrase(renderedElevation, getElevationDeg(), partialTick);
        return Math.max((float) Phl191Ballistics.MIN_ELEVATION_DEG,
                Math.min((float) Phl191Ballistics.MAX_ELEVATION_DEG, current));
    }

    private static float phrase(float previous, float next, float partialTick) {
        return previous + (next - previous) * partialTick;
    }

    public int getTubes() {
        return entityData.get(DATA_TUBES);
    }

    public int getReloadTicks() {
        return entityData.get(DATA_RELOAD);
    }

    public boolean isQteDone() {
        return entityData.get(DATA_QTE_DONE);
    }

    @Override
    public ArtilleryStrike strike() {
        return strike;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setHealth(tag.getFloat("Health"));
        elevation = tag.contains("Elevation")
                ? tag.getFloat("Elevation") : Phl191Geometry.TRAVEL_ELEVATION_DEG;
        tubes = tag.contains("Tubes") ? tag.getInt("Tubes") : Phl191Ballistics.TUBES;
        nextTube = tag.getInt("NextTube");
        reloadTicks = tag.getInt("ReloadTicks");
        entityData.set(DATA_ELEVATION, (float) elevation);
        entityData.set(DATA_TUBES, tubes);
        entityData.set(DATA_RELOAD, reloadTicks);
        entityData.set(DATA_QTE_DONE, tag.getBoolean("QteDone"));
        strike.read(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        tag.putFloat("Elevation", (float) elevation);
        tag.putInt("Tubes", tubes);
        tag.putInt("NextTube", nextTube);
        tag.putInt("ReloadTicks", reloadTicks);
        tag.putBoolean("QteDone", entityData.get(DATA_QTE_DONE));
        strike.write(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
