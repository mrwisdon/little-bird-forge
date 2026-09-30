package io.github.littlebird.entity;

import io.github.littlebird.entity.flight.JetType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * B-2 Spirit: the same flight, weapon and seat systems as the two fighters on a flying
 * wing airframe, so it needs nothing of its own beyond the type selector and the seat.
 *
 * <p>A flying wing has no tail, no canard, no turret and no vector thrust, so
 * {@link io.github.littlebird.client.B2Model} has no articulated part to drive and
 * {@link B2Geometry} carries the wing-sized envelope instead of {@link JetGeometry}.
 *
 * <p>The cockpit of a flying wing is further forward and higher than the F-35's, so the
 * seat is pinned to {@link B2Geometry}: 3.75 blocks ahead of the origin and 2.0 blocks up,
 * against the 3.0 and 1.25 {@link JetEntity#positionRider} uses for the two fighters.
 *
 * <p>{@link JetEntity} also has a shoulder-camera rider branch, but its
 * {@code isShoulderCamera()} test and {@code shoulderView} field are private, and no code
 * path ever sets that flag on a jet - {@code JetInput} clears it every tick and only the
 * helicopters ever set it true - so the placement below covers every reachable state.
 * Widening that one method to {@code protected} is all it would take to keep the branch
 * should the jets ever grow a shoulder camera.
 */
public class B2Entity extends JetEntity {
    public B2Entity(EntityType<? extends B2Entity> type, Level level) {
        super(type, level);
    }

    @Override
    public JetType jetType() {
        return JetType.B2;
    }

    /** Canopy floor: 2.0 blocks up, against the F-35's 1.25. */
    @Override
    public double getPassengersRidingOffset() {
        return B2Geometry.seatHeightBlocks();
    }

    /** Seat pan 3.75 blocks ahead of the origin, under the forward canopy of the wing. */
    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        double forward = B2Geometry.seatForwardBlocks();
        double yaw = Math.toRadians(getYRot());
        moveFunction.accept(passenger,
                getX() - Math.sin(yaw) * forward,
                getY() + B2Geometry.seatHeightBlocks() + passenger.getMyRidingOffset(),
                getZ() + Math.cos(yaw) * forward);
    }

    // ----------------------------------------------------------------------------------------
    // Bomb bay, targeting pod designation and bomb weapon selection (task-11, b2-wiring).
    // Appended below the airframe code on purpose: every type this block needs is written fully
    // qualified so the append adds no imports and changes no existing line. The bomb entities and
    // the LittleBirdConfig accessors are frozen in docs/plan/b2-systems-spec.md 0.8 / 0.9.
    // ----------------------------------------------------------------------------------------

    /** Bomb bay 0: the JDAM bay. Separate from {@link JetEntity}'s cannon/missile numbering. */
    public static final int WEAPON_JDAM = 0;
    /** Bomb bay 1: the incendiary bay. */
    public static final int WEAPON_INCENDIARY = 1;
    /** How many bomb bays the selector can cycle through. */
    public static final int WEAPON_COUNT = 2;

    // ---- synced mirror -------------------------------------------------------------------
    // The server owns the bay state; the HUD reads it on the client through entity data. All
    // accessors are fully qualified so this block still adds no imports to the file.
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_BOMB_WEAPON =
            net.minecraft.network.syncher.SynchedEntityData.defineId(B2Entity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_JDAM_LEFT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(B2Entity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_INCENDIARY_LEFT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(B2Entity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_BOMB_BAY =
            net.minecraft.network.syncher.SynchedEntityData.defineId(B2Entity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_POD_DESIGNATED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(B2Entity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_BOMB_WEAPON, WEAPON_JDAM);
        entityData.define(DATA_JDAM_LEFT,
                io.github.littlebird.LittleBirdConfig.bombBayCapacityJdam());
        entityData.define(DATA_INCENDIARY_LEFT,
                io.github.littlebird.LittleBirdConfig.bombBayCapacityIncendiary());
        entityData.define(DATA_BOMB_BAY, false);
        entityData.define(DATA_POD_DESIGNATED, false);
    }

    private <T> void pushSynced(net.minecraft.network.syncher.EntityDataAccessor<T> key, T value) {
        if (!java.util.Objects.equals(entityData.get(key), value)) {
            entityData.set(key, value);
        }
    }

    /** Server side: mirror the bay state the HUD and the other clients read. */
    private void syncBombState() {
        pushSynced(DATA_BOMB_WEAPON, bombWeapon);
        pushSynced(DATA_JDAM_LEFT, jdamBay.rounds());
        pushSynced(DATA_INCENDIARY_LEFT, incendiaryBay.rounds());
        pushSynced(DATA_BOMB_BAY, bombBayOpen);
        pushSynced(DATA_POD_DESIGNATED, podDesignated);
    }

    private final io.github.littlebird.entity.weapon.AmmoPool jdamBay =
            io.github.littlebird.entity.weapon.AmmoPool.full(
                    io.github.littlebird.LittleBirdConfig.bombBayCapacityJdam());
    private final io.github.littlebird.entity.weapon.AmmoPool incendiaryBay =
            io.github.littlebird.entity.weapon.AmmoPool.full(
                    io.github.littlebird.LittleBirdConfig.bombBayCapacityIncendiary());

    /** Selected bay, one of {@link #WEAPON_JDAM} or {@link #WEAPON_INCENDIARY}. */
    private int bombWeapon = WEAPON_JDAM;
    /** Ticks left before another release is accepted. */
    private int bombCooldown;
    /** Bombs still to leave the bay in the release sequence in flight. */
    private int pendingBombs;
    /** Ticks until the next bomb of that sequence leaves. */
    private int releaseTimer;
    /** Bay of the sequence in flight, so a switch mid-drop cannot mix the two bays. */
    private int pendingWeapon = WEAPON_JDAM;
    private boolean bombBayOpen;
    /** One number per release, so a stick's kills land on one line of the feed. */
    private int releaseSequence;
    /**
     * Who ordered this stick. The bombs credit their owner as a player by UUID, so they have to be
     * handed the crew rather than the airframe: a B-2's UUID is not a player, which left every kill
     * unattributed and the direct hit feedback silent.
     */
    private java.util.UUID releaseSenderUuid;

    // The airframe's own motion history. Vanilla's xo/yo/zo cannot be used for this: absMoveTo
    // writes them to the position the pilot's client has just reported, so on the server they read
    // zero however fast the aeroplane is going - which is exactly how sticks ended up falling
    // straight down. These are sampled once a tick instead, on both sides.
    private double[] pilotVelocity;
    private double lastSampleX;
    private double lastSampleY;
    private double lastSampleZ;
    private boolean hasMotionHistory;
    /** Server ticks covered by the next position sample, so a dropped packet cannot double speed. */
    private int ticksSinceSample = 1;
    private boolean podDesignated;
    private double podX;
    private double podY;
    private double podZ;
    private long podTick;

    /** The bay a weapon id refers to; anything unknown falls back to the JDAM bay. */
    private io.github.littlebird.entity.weapon.AmmoPool bay(int weapon) {
        return weapon == WEAPON_INCENDIARY ? incendiaryBay : jdamBay;
    }

    /** 0 = JDAM, 1 = incendiary. Synced: the HUD reads this on the client. */
    public int getBombWeapon() {
        return entityData.get(DATA_BOMB_WEAPON);
    }

    /**
     * The B-2 carries no cannon, so any value the client invents - including the shared
     * {@link JetEntity#WEAPON_CANNON} slot - falls back to the JDAM bay.
     */
    public void setBombWeapon(int weapon) {
        this.bombWeapon = weapon == WEAPON_INCENDIARY ? WEAPON_INCENDIARY : WEAPON_JDAM;
    }

    /** Rounds left in that bay. Synced, so the cockpit HUD shows the real count. */
    public int bombCount(int weapon) {
        return entityData.get(weapon == WEAPON_INCENDIARY ? DATA_INCENDIARY_LEFT : DATA_JDAM_LEFT);
    }

    /** Size of that bay. */
    public int bombCapacity(int weapon) {
        return bay(weapon).capacity();
    }

    /** True from the start of a release until the last bomb of the sequence has gone. Synced. */
    public boolean bombBayOpen() {
        return entityData.get(DATA_BOMB_BAY);
    }

    /**
     * One press of the release key. Server side only: the bay opens, one bomb leaves at once and
     * the rest follow at {@code BombBay.RELEASE_INTERVAL_TICKS} from {@link #tick()}, then the bay
     * closes and the cooldown keeps the next press out for the whole drop.
     */
    public void applyBombRelease(net.minecraft.server.level.ServerPlayer sender, boolean salvo) {
        if (sender == null || sender.getVehicle() != this) {
            return;
        }
        if (pendingBombs > 0) {
            return;
        }
        io.github.littlebird.entity.weapon.AmmoPool pool = bay(bombWeapon);
        if (!io.github.littlebird.entity.weapon.BombBay.canRelease(pool.rounds(), bombCooldown)) {
            return;
        }
        int count = io.github.littlebird.entity.weapon.BombBay.bombsToDrop(salvo, pool.rounds());
        if (count <= 0) {
            return;
        }
        pendingWeapon = bombWeapon;
        releaseSequence++;
        releaseSenderUuid = sender.getUUID();
        pendingBombs = count;
        releaseTimer = 0;
        bombBayOpen = true;
        bombCooldown = io.github.littlebird.entity.weapon.BombBay.nextCooldown(salvo);
    }

    /**
     * Samples the airframe's own movement once a tick so a release can inherit it.
     *
     * <p>Filtered rather than raw: a teleport is ignored and an empty tick (the pilot's move packet
     * has not arrived yet) keeps the last known velocity, so a stick never leaves the bay with no
     * forward speed just because of packet timing.
     */
    private void trackPilotMotion() {
        double x = getX();
        double y = getY();
        double z = getZ();
        if (hasMotionHistory) {
            double dx = x - lastSampleX;
            double dy = y - lastSampleY;
            double dz = z - lastSampleZ;
            if (dx * dx + dy * dy + dz * dz > 1.0E-12D) {
                // A fresh position: divide by the ticks it actually covers, so a doubled packet is
                // not a doubled speed and a turn is not reported late.
                pilotVelocity = io.github.littlebird.entity.weapon.B2BombAim
                        .averageReleaseVelocity(dx, dy, dz, ticksSinceSample);
                ticksSinceSample = 1;
                lastSampleX = x;
                lastSampleY = y;
                lastSampleZ = z;
            } else {
                // No movement reported this tick: the packet has not arrived, so keep the last
                // measured velocity and let the next sample divide over the whole gap.
                ticksSinceSample++;
            }
        } else {
            lastSampleX = x;
            lastSampleY = y;
            lastSampleZ = z;
        }
        hasMotionHistory = true;
    }

    /** The airframe's velocity in blocks per tick: what anything it launches inherits. */
    public double[] pilotVelocity() {
        double[] velocity = pilotVelocity;
        return velocity == null ? new double[]{0.0D, 0.0D, 0.0D}
                : new double[]{velocity[0], velocity[1], velocity[2]};
    }

    @Override
    public void tick() {
        super.tick();
        trackPilotMotion();
        if (!level().isClientSide()) {
            syncBombState();
        }
        if (releaseTimer > 0) {
            releaseTimer--;
        }
        if (pendingBombs > 0 && releaseTimer <= 0) {
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                releaseOne(server, pendingWeapon);
            } else {
                pendingBombs = 0;
            }
            pendingBombs--;
            releaseTimer = io.github.littlebird.entity.weapon.BombBay.RELEASE_INTERVAL_TICKS;
            if (pendingBombs <= 0) {
                bombBayOpen = false;
            }
        }
        if (bombCooldown > 0) {
            bombCooldown--;
        }
    }

    /**
     * Spawns one bomb at the belly station with the aircraft's own velocity, handing it the pod
     * designation when there is one so a JDAM can converge on it. The incendiary bay is always
     * ballistic - its {@code BombKind} is unguided.
     */
    /**
     * Tells the pilot's client which round to follow with the chase camera. Sent to the pilot only,
     * so a crew mate in the back does not have their view yanked around.
     */
    private void notifyBombSpawn(net.minecraft.world.entity.Entity bomb) {
        if (getControllingPassenger() instanceof net.minecraft.server.level.ServerPlayer pilot) {
            io.github.littlebird.network.ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> pilot),
                    new io.github.littlebird.network.B2BombSpawnPacket(bomb.getId()));
        }
    }

    private void releaseOne(net.minecraft.server.level.ServerLevel server, int weapon) {
        io.github.littlebird.entity.weapon.AmmoPool pool = bay(weapon);
        if (!pool.fire(1)) {
            return;
        }
        double x = getX();
        double y = getY() + io.github.littlebird.entity.B2Geometry.bellyClearanceBlocks();
        double z = getZ();
        // Never xo/yo/zo and never getDeltaMovement(): on the server both are zero for a ridden
        // vehicle, because the position arrives from the pilot's client through absMoveTo. This is
        // the entity's own filtered history.
        double[] velocity = pilotVelocity();
        double vx = velocity[0];
        double vy = velocity[1];
        double vz = velocity[2];
        // Hand the bombs the crew, not the airframe: they resolve their owner as a player, and the
        // aeroplane also wants to be on the do-not-hit-own-hull list.
        net.minecraft.world.entity.Entity shooter = null;
        if (releaseSenderUuid != null) {
            shooter = server.getPlayerByUUID(releaseSenderUuid);
        }
        if (shooter == null
                && getControllingPassenger() instanceof net.minecraft.world.entity.player.Player pilot) {
            shooter = pilot;
        }
        if (shooter == null) {
            shooter = this;
        }
        if (weapon == WEAPON_INCENDIARY) {
            io.github.littlebird.entity.IncendiaryBombEntity bomb = io.github.littlebird.registry
                    .ModEntities.INCENDIARY_BOMB.get().create(server);
            if (bomb == null) {
                return;
            }
            bomb.launchFrom(shooter, x, y, z, vx, vy, vz, false, releaseSequence);
            server.addFreshEntity(bomb);
            notifyBombSpawn(bomb);
        } else {
            io.github.littlebird.entity.JdamBombEntity bomb = io.github.littlebird.registry
                    .ModEntities.JDAM_BOMB.get().create(server);
            if (bomb == null) {
                return;
            }
            boolean guided = hasPodDesignation();
            bomb.launchFrom(shooter, x, y, z, vx, vy, vz, guided, releaseSequence);
            if (guided) {
                bomb.setTarget(podDesignationX(), podDesignationY(), podDesignationZ());
            }
            server.addFreshEntity(bomb);
            notifyBombSpawn(bomb);
        }
    }

    /** Records a pod designation and the game time it was taken, so it can be aged out. */
    public void setPodDesignation(double x, double y, double z) {
        this.podX = x;
        this.podY = y;
        this.podZ = z;
        this.podDesignated = true;
        this.podTick = level().getGameTime();
    }

    public void clearPodDesignation() {
        this.podDesignated = false;
        this.podTick = 0L;
    }

    public boolean hasPodDesignation() {
        return entityData.get(DATA_POD_DESIGNATED);
    }

    public double podDesignationX() {
        return podX;
    }

    public double podDesignationY() {
        return podY;
    }

    public double podDesignationZ() {
        return podZ;
    }

    /** Game time of the designation, for expiry. The lifetime itself is not frozen yet. */
    public long podDesignationTick() {
        return podTick;
    }
}
