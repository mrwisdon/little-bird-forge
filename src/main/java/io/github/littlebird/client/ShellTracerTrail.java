package io.github.littlebird.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Geometry rules for the rigid tracer streaks the shells fly with, kept free of every
 * Minecraft class so a plain JUnit test can pin them down without a client. It is
 * shared by the 120mm main gun round and the 25mm autocannon round so the two cannot
 * drift apart.
 *
 * <p>Why a history instead of one line pinned to the nose: the rounds are stepped
 * on the server and drawn from interpolated positions, and at five or six blocks per
 * tick a line pinned to the shell can visibly slide off the shell it belongs to. The
 * trail therefore keeps the last {@link #MAX_FRAMES} drawn positions per shell
 * and emits them as short segments. Across the span a 24 pixel streak covers the
 * round only drops about 0.25 blocks (0.02 blocks/tick of gravity over the five
 * ticks it takes to fly one), so the result still reads as one straight,
 * constant-width bar - it just cannot swing away from its shell.
 *
 * <p>One sample is kept per server tick, not per rendered frame, so
 * {@link #MAX_FRAMES} is a tick budget: a shell at five blocks per tick always has
 * about 35 blocks of path in hand whether the client runs at 20 or at 240 fps.
 * That is what lets the streak still be 24 pixels long when the round is far
 * away, without depending on the player's frame rate.
 *
 * <p>The teleport guard clears a shell's history rather than drawing a line
 * across the map. Its threshold has to clear both rounds' own muzzle speed, five and
 * six blocks per tick, by a wide margin, otherwise every honest frame would look like
 * a jump, so it sits at {@link #JUMP_BLOCKS_PER_TICK} blocks per tick.
 *
 * <p>The screen size helpers ({@link #pixelsPerRadian},
 * {@link #halfWidthForDistance}, {@link #lengthForDistance}) keep the bar
 * readable at range by growing it until it covers a minimum number of pixels,
 * and capping it so a close round is never drawn as a slab.
 *
 * <p>State is keyed by entity id and is therefore static. Callers should
 * {@link #forget(int)} an entity once it is removed; the stale sweep and the hard
 * cap on the map size back that up so a missed call cannot leak.
 */
public final class ShellTracerTrail {
    /**
     * Drawn positions kept per shell. One sample is kept per server tick, so this
     * is really a tick budget: 8 samples is about 35 blocks of path at the round's
     * 5 blocks/tick, comfortably more than the longest streak the minimum pixel
     * length can ask for inside either round's render distance.
     */
    public static final int MAX_FRAMES = 8;
    /**
     * Teleport guard, in blocks per tick. The rounds fly at five and six blocks per
     * tick, so a lower value would clear the history on every honest frame; this only
     * fires
     * on a real teleport, a respawn or a dimension change.
     */
    public static final double JUMP_BLOCKS_PER_TICK = 24.0D;
    /** Hard cap on tracked shells, so a static map can never grow without bound. */
    public static final int MAX_TRACKED_SHELLS = 64;
    /** A history this many ticks older than the newest sample is dropped as abandoned. */
    public static final double STALE_TICKS = 100.0D;
    /** Half width of the bright core in blocks when nothing has to be compensated. */
    public static final double HALF_WIDTH = 0.05D;
    /**
     * Hard cap on the compensated half width. The projection would ask for more at
     * extreme range, but past this a streak reads as a slab of light rather than as
     * a tracer.
     */
    public static final double MAX_HALF_WIDTH = 0.75D;
    /** Hard cap on the streak's length in blocks, compensation included. */
    public static final double MAX_LENGTH_BLOCKS = 48.0D;
    /** Brightness at the shell end of the streak and at its far end. */
    public static final float HEAD_BRIGHTNESS = 1.0F;
    public static final float TAIL_BRIGHTNESS = 0.25F;
    /** Segments shorter than this are not worth a quad. */
    public static final double MIN_SEGMENT_BLOCKS = 0.01D;
    private static final double DIRECTION_EPSILON_SQR = 1.0E-8D;
    private static final double COINCIDENT_EPSILON = 1.0E-9D;

    private static final Map<Integer, History> HISTORIES = new HashMap<>();

    private ShellTracerTrail() {
    }

    /**
     * One straight piece of the streak. {@code a} is the far, older end and
     * {@code b} is the end at the shell, so a renderer can taper along it.
     */
    public record Segment(double ax, double ay, double az,
                          double bx, double by, double bz,
                          double halfWidth, float brightness) {
        /** Length of this piece in blocks. */
        public double length() {
            double dx = this.bx - this.ax;
            double dy = this.by - this.ay;
            double dz = this.bz - this.az;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    /**
     * How many framebuffer pixels one radian covers vertically: a pinhole camera
     * with a vertical field of view {@code verticalFovDeg} puts
     * {@code (height / 2) / tan(fov / 2)} pixels on each radian of a distant
     * object. Anything unusable - a zero or silly field of view, a missing
     * framebuffer, a NaN - reports 0, which every caller reads as "no
     * compensation", never as an enormous one.
     */
    public static double pixelsPerRadian(double verticalFovDeg, double framebufferHeightPixels) {
        if (!Double.isFinite(verticalFovDeg) || !Double.isFinite(framebufferHeightPixels)
                || framebufferHeightPixels <= 0.0D
                || verticalFovDeg <= 0.0D || verticalFovDeg >= 180.0D) {
            return 0.0D;
        }
        double tangent = Math.tan(Math.toRadians(verticalFovDeg) * 0.5D);
        if (!Double.isFinite(tangent) || tangent <= 0.0D) {
            return 0.0D;
        }
        return framebufferHeightPixels * 0.5D / tangent;
    }

    /**
     * Half width that keeps a streak at least {@code minimumPixels} pixels thick
     * on screen at {@code distanceBlocks}. Half of the minimum is used because the
     * width is a radius: the rendered bar is twice this.
     *
     * <p>Anything unusable - no direction, no minimum, a distance that is not a
     * distance - falls back to {@code baseHalfWidth}, and the result never goes
     * below the base or above {@link #MAX_HALF_WIDTH}. A minimum of 0 is therefore
     * the switch that asks for strict world units.
     */
    public static double halfWidthForDistance(double baseHalfWidth, double distanceBlocks,
                                              double pixelsPerRadian, double minimumPixels) {
        return compensatedSize(baseHalfWidth, distanceBlocks, pixelsPerRadian, minimumPixels,
                0.5D, MAX_HALF_WIDTH);
    }

    /**
     * Length that keeps a streak at least {@code minimumPixels} long on screen at
     * {@code distanceBlocks}. The clamp works exactly like
     * {@link #halfWidthForDistance}: never shorter than the base, never longer than
     * {@link #MAX_LENGTH_BLOCKS}, and back to the base for any unusable input.
     */
    public static double lengthForDistance(double baseLengthBlocks, double distanceBlocks,
                                           double pixelsPerRadian, double minimumPixels) {
        return compensatedSize(baseLengthBlocks, distanceBlocks, pixelsPerRadian, minimumPixels,
                1.0D, MAX_LENGTH_BLOCKS);
    }

    /**
     * Shared clamp: {@code clamp(max(base, scale * minimum * distance / pixelsPerRadian),
     * base, maximum)}. A base that is not a usable size is handed straight back, so a
     * caller that switches the streak off with 0 still gets 0 instead of a compensated bar.
     */
    private static double compensatedSize(double base, double distanceBlocks,
                                          double pixelsPerRadian, double minimumPixels,
                                          double scale, double maximum) {
        if (!Double.isFinite(base) || base <= 0.0D) {
            return base;
        }
        if (!Double.isFinite(distanceBlocks) || !Double.isFinite(pixelsPerRadian)
                || !Double.isFinite(minimumPixels)) {
            return base;
        }
        if (distanceBlocks <= 0.0D || pixelsPerRadian <= 0.0D || minimumPixels <= 0.0D) {
            return base;
        }
        double required = scale * minimumPixels * distanceBlocks / pixelsPerRadian;
        if (!Double.isFinite(required) || required > maximum) {
            return maximum;
        }
        return Math.max(base, required);
    }

    /**
     * Records one drawn position for a shell. {@code tickStamp} is game time in
     * ticks including the partial tick, so the teleport guard can discount a frame
     * that took longer than one tick instead of mistaking it for a jump.
     *
     * <p>Only the freshest position of a server tick is kept: the frames inside one
     * tick overwrite each other, which turns {@link #MAX_FRAMES} into a tick budget
     * rather than a frame budget.
     */
    public static void track(int shellId, double tickStamp, double x, double y, double z) {
        if (!Double.isFinite(tickStamp) || !Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(z)) {
            forget(shellId);
            return;
        }
        History history = HISTORIES.get(shellId);
        if (history == null) {
            history = new History();
            HISTORIES.put(shellId, history);
        }
        Sample previous = history.last();
        if (previous != null && tickStamp < previous.tick) {
            history.clear();
            previous = null;
        }
        if (previous == null) {
            history.add(tickStamp, x, y, z);
        } else {
            double elapsed = tickStamp - previous.tick;
            double dx = x - previous.x;
            double dy = y - previous.y;
            double dz = z - previous.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > JUMP_BLOCKS_PER_TICK * Math.max(elapsed, 1.0D)) {
                // No honest flight covers this much ground in the time we saw: start over
                // rather than draw a streak spanning the map.
                history.clear();
                history.add(tickStamp, x, y, z);
            } else if (Math.floor(tickStamp) == Math.floor(previous.tick)) {
                // A later frame inside the same tick: keep the newest drawn position and spend
                // no history slot on it, so the streak covers the same distance at any fps.
                history.replaceLast(x, y, z);
            } else {
                history.add(tickStamp, x, y, z);
            }
        }
        history.trim(MAX_FRAMES);
        prune(tickStamp);
    }

    /** Drops a shell's history. The renderer calls this for any entity it sees removed. */
    public static void forget(int shellId) {
        HISTORIES.remove(shellId);
    }

    /** Forgets every shell, so a reload or a test starts from a clean slate. */
    public static void reset() {
        HISTORIES.clear();
    }

    /** Number of shells currently holding a history; used by the tests. */
    public static int trackedShells() {
        return HISTORIES.size();
    }

    /** Number of positions kept for one shell; used by the tests. */
    public static int sampleCount(int shellId) {
        History history = HISTORIES.get(shellId);
        return history == null ? 0 : history.size();
    }

    /**
     * Streak segments for one shell at the default {@link #HALF_WIDTH}, newest
     * piece first, clipped to {@code lengthBlocks} of total length. An empty list
     * means draw nothing.
     */
    public static List<Segment> segments(int shellId, double lengthBlocks,
                                         double headingX, double headingY, double headingZ) {
        return segments(shellId, lengthBlocks, HALF_WIDTH, headingX, headingY, headingZ);
    }

    /**
     * As above with an explicit half width, which the renderer picks per frame from
     * the camera distance so the streak keeps a minimum size on screen. A width that
     * is not a usable size falls back to {@link #HALF_WIDTH}.
     *
     * <p>With no usable history - just spawned, or the teleport guard just cleared
     * it - the streak degrades to a fixed length bar along {@code heading}, which
     * is the shell's own facing, and a non-finite heading then means nothing is
     * drawn at all rather than an arbitrary bar in some made-up direction.
     */
    public static List<Segment> segments(int shellId, double lengthBlocks, double halfWidth,
                                         double headingX, double headingY, double headingZ) {
        double width = Double.isFinite(halfWidth) && halfWidth > 0.0D ? halfWidth : HALF_WIDTH;
        List<Segment> segments = new ArrayList<>();
        if (!Double.isFinite(lengthBlocks) || lengthBlocks <= MIN_SEGMENT_BLOCKS) {
            return segments;
        }
        History history = HISTORIES.get(shellId);
        if (history == null || history.size() < 2) {
            return fallbackSegments(history, lengthBlocks, width, headingX, headingY, headingZ);
        }
        double remaining = lengthBlocks;
        double consumed = 0.0D;
        for (int index = history.size() - 1; index >= 1 && remaining > MIN_SEGMENT_BLOCKS; index--) {
            Sample near = history.get(index);
            Sample far = history.get(index - 1);
            double dx = near.x - far.x;
            double dy = near.y - far.y;
            double dz = near.z - far.z;
            double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (step <= COINCIDENT_EPSILON) {
                continue;
            }
            double used = Math.min(remaining, step);
            double fraction = used / step;
            // Keep the clipped end on the path the shell actually flew.
            double ax = near.x - dx * fraction;
            double ay = near.y - dy * fraction;
            double az = near.z - dz * fraction;
            segments.add(new Segment(ax, ay, az, near.x, near.y, near.z, width,
                    brightnessAt(consumed / lengthBlocks)));
            remaining -= used;
            consumed += used;
        }
        if (segments.isEmpty()) {
            return fallbackSegments(history, lengthBlocks, width, headingX, headingY, headingZ);
        }
        return segments;
    }

    /** Brightness at a fraction of the way from the shell to the far end of the streak. */
    private static float brightnessAt(double fraction) {
        double clamped = Math.max(0.0D, Math.min(1.0D, fraction));
        return (float) (HEAD_BRIGHTNESS + (TAIL_BRIGHTNESS - HEAD_BRIGHTNESS) * clamped);
    }

    /**
     * The zero-speed case: the shell is standing still (or its history was just
     * cleared), so there is no path to follow and the streak becomes one bar of
     * the requested length pointing away from the shell's facing.
     */
    private static List<Segment> fallbackSegments(History history, double lengthBlocks,
                                                  double halfWidth, double headingX,
                                                  double headingY, double headingZ) {
        List<Segment> segments = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            return segments;
        }
        double lengthSqr = headingX * headingX + headingY * headingY + headingZ * headingZ;
        if (!Double.isFinite(lengthSqr) || lengthSqr <= DIRECTION_EPSILON_SQR) {
            return segments;
        }
        double inverse = 1.0D / Math.sqrt(lengthSqr);
        double dx = headingX * inverse * lengthBlocks;
        double dy = headingY * inverse * lengthBlocks;
        double dz = headingZ * inverse * lengthBlocks;
        Sample head = history.last();
        segments.add(new Segment(head.x - dx, head.y - dy, head.z - dz,
                head.x, head.y, head.z, halfWidth, HEAD_BRIGHTNESS));
        return segments;
    }

    /**
     * Drops abandoned histories and then the oldest ones if the map is still over
     * its cap. Both are keyed off the newest stamp seen, so a shell that stops
     * being drawn for five seconds is forgotten even if nobody calls forget.
     */
    private static void prune(double now) {
        double cutoff = now - STALE_TICKS;
        Iterator<Map.Entry<Integer, History>> iterator = HISTORIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, History> entry = iterator.next();
            Sample last = entry.getValue().last();
            if (last == null || last.tick < cutoff) {
                iterator.remove();
            }
        }
        while (HISTORIES.size() > MAX_TRACKED_SHELLS) {
            boolean found = false;
            int oldestId = 0;
            double oldestTick = Double.POSITIVE_INFINITY;
            for (Map.Entry<Integer, History> entry : HISTORIES.entrySet()) {
                double tick = entry.getValue().last().tick;
                if (tick < oldestTick) {
                    oldestTick = tick;
                    oldestId = entry.getKey();
                    found = true;
                }
            }
            if (!found) {
                return;
            }
            HISTORIES.remove(oldestId);
        }
    }

    /** One drawn position, with the tick stamp that identifies it. */
    private static final class Sample {
        private final double tick;
        private final double x;
        private final double y;
        private final double z;

        private Sample(double tick, double x, double y, double z) {
            this.tick = tick;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** A bounded ring of samples, oldest first. */
    private static final class History {
        private final List<Sample> samples = new ArrayList<>();

        private void add(double tick, double x, double y, double z) {
            this.samples.add(new Sample(tick, x, y, z));
        }

        /** Keeps the sample's own tick: the tick it belongs to is what the frame count counts. */
        private void replaceLast(double x, double y, double z) {
            Sample last = last();
            if (last != null) {
                this.samples.set(this.samples.size() - 1, new Sample(last.tick, x, y, z));
            }
        }

        private void trim(int maximum) {
            while (this.samples.size() > maximum) {
                this.samples.remove(0);
            }
        }

        private void clear() {
            this.samples.clear();
        }

        private boolean isEmpty() {
            return this.samples.isEmpty();
        }

        private int size() {
            return this.samples.size();
        }

        private Sample get(int index) {
            return this.samples.get(index);
        }

        private Sample last() {
            return this.samples.isEmpty() ? null : this.samples.get(this.samples.size() - 1);
        }
    }
}

