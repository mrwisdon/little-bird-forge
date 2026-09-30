package io.github.littlebird;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The crafting table is a 3x3 grid. A recipe that asks for more than nine slots is generated
 * happily, ships happily, and can never be crafted - both jets were written with eleven parts and
 * were unusable until this was caught. Every shipped recipe is checked here so it cannot happen
 * again.
 */
final class RecipeGridTest {
    private static final Path RECIPES =
            Path.of("src/generated/resources/data/battlefield/recipes");

    @Test
    void everyShippedRecipeFitsTheCraftingGrid() throws IOException {
        assertTrue(Files.isDirectory(RECIPES), "generated recipes are missing: " + RECIPES);
        try (Stream<Path> files = Files.list(RECIPES)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject recipe = JsonParser.parseString(
                        Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                String type = recipe.get("type").getAsString();
                String name = path.getFileName().toString();
                if (type.equals("minecraft:crafting_shapeless")) {
                    int slots = recipe.getAsJsonArray("ingredients").size();
                    assertTrue(slots <= 9,
                            name + " wants " + slots + " ingredients; a 3x3 grid holds 9");
                } else if (type.equals("minecraft:crafting_shaped")) {
                    JsonArray pattern = recipe.getAsJsonArray("pattern");
                    assertTrue(pattern.size() <= 3, name + " has " + pattern.size() + " rows");
                    for (JsonElement row : pattern) {
                        assertTrue(row.getAsString().length() <= 3,
                                name + " has a row wider than 3: '" + row.getAsString() + "'");
                    }
                }
            }
        }
    }

    @Test
    void everyVehicleHasARecipeThatActuallyFits() throws IOException {
        Set<String> produced = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(RECIPES)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject recipe = JsonParser.parseString(
                        Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject result = recipe.getAsJsonObject("result");
                if (result != null && result.has("item")) {
                    produced.add(result.get("item").getAsString());
                }
            }
        }
        for (String vehicle : new String[] {"little_bird", "apache", "f35", "su57",
                "main_battle_tank", "infantry_fighting_vehicle"}) {
            assertTrue(produced.contains("battlefield:" + vehicle),
                    "no shipped crafting recipe produces battlefield:" + vehicle);
        }
    }
}
