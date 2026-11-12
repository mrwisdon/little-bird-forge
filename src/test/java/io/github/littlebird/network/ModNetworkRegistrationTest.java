package io.github.littlebird.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Reads the network registry the way Forge does - literally - and checks the two mistakes that are
 * silent at startup and fatal at runtime.
 *
 * <p>A builder chain that never calls {@code add()} looks perfectly healthy: it compiles, the game
 * starts, and the first time that message is sent the server dies with
 * {@code IllegalArgumentException: Invalid message ...}. That happened once, so it is tested now.
 */
final class ModNetworkRegistrationTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/io/github/littlebird/network/ModNetwork.java");
    private static final Pattern BUILDER = Pattern.compile("messageBuilder\\(");
    private static final Pattern ID = Pattern.compile("messageBuilder\\([^,]+,\\s*(\\d+)");

    @Test
    void everyBuilderChainIsAdded() throws IOException {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        List<String> chains = new ArrayList<>();
        Matcher matcher = BUILDER.matcher(source);
        while (matcher.find()) {
            int start = matcher.start();
            int end = source.indexOf(';', start);
            assertTrue(end > start, "unterminated messageBuilder chain at " + start);
            chains.add(source.substring(start, end + 1));
        }
        assertTrue(chains.size() >= 15, "only found " + chains.size() + " registrations");
        for (String chain : chains) {
            assertTrue(chain.contains(".add()"),
                    "a registration never calls add(), so the packet is unknown when it is sent:\n"
                            + chain);
            assertTrue(chain.contains(".encoder(") && chain.contains(".decoder(")
                            && chain.contains(".consumerMainThread("),
                    "incomplete registration:\n" + chain);
        }
    }

    @Test
    void discriminatorIdsAreUnique() throws IOException {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        Set<String> ids = new HashSet<>();
        Matcher matcher = ID.matcher(source);
        while (matcher.find()) {
            assertTrue(ids.add(matcher.group(1)), "duplicate packet id " + matcher.group(1));
        }
        assertEquals(22, ids.size(), "expected 22 registered packets, saw " + ids);
    }
}
