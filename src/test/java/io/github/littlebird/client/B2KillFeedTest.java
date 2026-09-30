package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The kill feed: one line per release, counts, merge window, expiry and caps. */
final class B2KillFeedTest {
    @Test
    void oneReleaseKeepsOneLineHoweverManyBombsItTakes() {
        B2KillFeed feed = new B2KillFeed();
        feed.add(7, 0, List.of("僵尸", "僵尸"));
        feed.add(7, 0, List.of("牛"));
        List<B2KillFeed.Display> lines = feed.displays();
        assertEquals(1, lines.size(), "a salvo must not open a second line");
        assertEquals(3, lines.get(0).total());
        assertEquals("僵尸 ×2  牛", lines.get(0).text());
        assertEquals(1.0F, lines.get(0).alpha(), 1.0E-6F);
    }

    @Test
    void aLaterReleaseOpensItsOwnLineOnTop() {
        B2KillFeed feed = new B2KillFeed();
        feed.add(1, 0, List.of("僵尸"));
        feed.add(2, 1, List.of("牛"));
        List<B2KillFeed.Display> lines = feed.displays();
        assertEquals(2, lines.size());
        assertEquals(1, lines.get(0).weapon(), "newest on top");
        // A release that has been up longer than the merge window is not reused.
        for (int i = 0; i < B2KillFeed.LIFETIME_TICKS - B2KillFeed.MERGE_TICKS + 1; i++) {
            feed.tick();
        }
        feed.add(1, 0, List.of("牛"));
        assertEquals(3, feed.displays().size());
    }

    @Test
    void linesExpireAndFade() {
        assertEquals(1.0F, B2KillFeed.alpha(B2KillFeed.FADE_TICKS), 1.0E-6F);
        assertEquals(0.5F, B2KillFeed.alpha(B2KillFeed.FADE_TICKS / 2), 1.0E-6F);
        assertEquals(0.0F, B2KillFeed.alpha(0), 1.0E-6F);
        B2KillFeed feed = new B2KillFeed();
        feed.add(1, 0, List.of("僵尸"));
        for (int i = 0; i < B2KillFeed.LIFETIME_TICKS - 1; i++) {
            feed.tick();
        }
        assertFalse(feed.isEmpty(), "still on screen one tick before the end");
        feed.tick();
        assertTrue(feed.isEmpty(), "gone once its time is up");
    }

    @Test
    void theCornerIsCappedAndEmptyReportsAreIgnored() {
        B2KillFeed feed = new B2KillFeed();
        feed.add(0, 0, List.of());
        feed.add(1, 0, null);
        assertTrue(feed.isEmpty());
        for (int i = 0; i < B2KillFeed.MAX_ENTRIES + 3; i++) {
            feed.add(100 + i, 0, List.of("僵尸"));
        }
        assertEquals(B2KillFeed.MAX_ENTRIES, feed.displays().size());
        // Blank names never open a line of their own.
        B2KillFeed blanks = new B2KillFeed();
        blanks.add(1, 0, List.of("", "   "));
        assertTrue(blanks.isEmpty());
    }

    @Test
    void aHugeBlastFoldsTheTailAway() {
        String text = B2KillFeed.summarise(new java.util.LinkedHashMap<>(Map.of(
                "僵尸", 1, "骷髅", 1, "牛", 1, "猪", 1, "羊", 1)));
        assertTrue(text.endsWith("+1"), text);
        assertEquals("僵尸 ×3", B2KillFeed.summarise(new java.util.LinkedHashMap<>(Map.of("僵尸", 3))));
        assertEquals("", B2KillFeed.summarise(null));
        assertEquals("", B2KillFeed.summarise(new java.util.LinkedHashMap<>()));
    }
}
