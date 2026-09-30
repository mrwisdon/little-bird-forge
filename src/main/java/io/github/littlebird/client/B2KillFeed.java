package io.github.littlebird.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kill feed for the bomb run: what one release killed, drawn in the top right corner.
 *
 * <p>A stick is one release id, so a two bomb salvo keeps a single line even though the bombs land
 * six ticks apart. Lines expire on their own and the newest sits on top. Pure rules: no Minecraft
 * classes, so the merging, the counting and the expiry are unit tested.
 */
public final class B2KillFeed {
    /** How long a line stays up. */
    public static final int LIFETIME_TICKS = 100;
    /** Fade over the last of it. */
    public static final int FADE_TICKS = 20;
    /** A later bomb of the same release still belongs to the same line. */
    public static final int MERGE_TICKS = 40;
    public static final int MAX_ENTRIES = 5;
    public static final int MAX_NAMES = 4;

    /** One finished line, as the HUD wants it. */
    public record Display(int weapon, String text, int total, float alpha) {
    }

    private static final class Line {
        private final int releaseId;
        private final int weapon;
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private int total;
        private int ticksLeft = LIFETIME_TICKS;

        private Line(int releaseId, int weapon) {
            this.releaseId = releaseId;
            this.weapon = weapon;
        }
    }

    private final List<Line> lines = new ArrayList<>();

    /**
     * Adds one bomb's victims. They join the line of the same release while it is still fresh, so a
     * salvo reads as one event rather than two.
     */
    public void add(int releaseId, int weapon, List<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        // Clean first: a report of nothing but blanks must not open a line.
        List<String> cleaned = new ArrayList<>(names.size());
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                cleaned.add(name);
            }
        }
        if (cleaned.isEmpty()) {
            return;
        }
        Line line = null;
        for (Line candidate : lines) {
            if (candidate.releaseId == releaseId
                    && candidate.ticksLeft > LIFETIME_TICKS - MERGE_TICKS) {
                line = candidate;
                break;
            }
        }
        if (line == null) {
            line = new Line(releaseId, weapon);
            lines.add(0, line);
            while (lines.size() > MAX_ENTRIES) {
                lines.remove(lines.size() - 1);
            }
        }
        for (String name : cleaned) {
            line.counts.merge(name, 1, Integer::sum);
            line.total++;
        }
        line.ticksLeft = LIFETIME_TICKS;
    }

    /** Ages every line by one tick, dropping the ones that have run out. */
    public void tick() {
        for (int i = lines.size() - 1; i >= 0; i--) {
            Line line = lines.get(i);
            if (--line.ticksLeft <= 0) {
                lines.remove(i);
            }
        }
    }

    public void clear() {
        lines.clear();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** Newest first, ready to draw. */
    public List<Display> displays() {
        List<Display> out = new ArrayList<>(lines.size());
        for (Line line : lines) {
            out.add(new Display(line.weapon, summarise(line.counts), line.total,
                    alpha(line.ticksLeft)));
        }
        return out;
    }

    /**
     * "僵尸 ×3  牛 ×1" - identical names are counted, and past {@link #MAX_NAMES} distinct names the
     * rest are folded into a "+n" so one big blast cannot fill the corner.
     */
    public static String summarise(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        int shown = 0;
        int hidden = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (shown >= MAX_NAMES) {
                hidden++;
                continue;
            }
            if (shown > 0) {
                text.append("  ");
            }
            text.append(entry.getKey());
            if (entry.getValue() > 1) {
                text.append(" ×").append(entry.getValue());
            }
            shown++;
        }
        if (hidden > 0) {
            text.append("  +").append(hidden);
        }
        return text.toString();
    }

    /** 1.0 until the last {@link #FADE_TICKS}, then a linear fade. */
    public static float alpha(int ticksLeft) {
        if (ticksLeft >= FADE_TICKS) {
            return 1.0F;
        }
        return Math.max(0.0F, ticksLeft / (float) FADE_TICKS);
    }

    // ------------------------------------------------------------------
    // The client's own feed: the packet fills it, the HUD draws it.
    // ------------------------------------------------------------------

    private static final B2KillFeed CLIENT = new B2KillFeed();

    public static void report(int releaseId, int weapon, List<String> names) {
        CLIENT.add(releaseId, weapon, names);
    }

    public static void tickClient() {
        CLIENT.tick();
    }

    public static List<Display> clientDisplays() {
        return CLIENT.displays();
    }

    public static void clearClient() {
        CLIENT.clear();
    }
}
