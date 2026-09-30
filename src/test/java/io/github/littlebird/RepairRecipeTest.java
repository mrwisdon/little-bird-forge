package io.github.littlebird;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The repair recipes are hand written JSON: the data generator does not emit same-item-in,
 * same-item-out shapeless recipes, and a missing one is invisible until a player tries to repair
 * a wrecked hull. Each vehicle keeps one repair recipe, priced at about one iron ingot per twenty
 * points of its maximum health, and no aircraft-only hole is left behind.
 */
final class RepairRecipeTest {
    /** Hull -> the iron ingots its repair recipe must consume. */
    private static final Map<String, Integer> HULLS = new LinkedHashMap<>();

    static {
        HULLS.put("little_bird", 3);
        HULLS.put("apache", 5);
        HULLS.put("f35", 5);
        HULLS.put("su57", 5);
        HULLS.put("main_battle_tank", 6);
        HULLS.put("infantry_fighting_vehicle", 5);
    }

    @Test
    void everyHullHasAWellFormedRepairRecipe() throws IOException {
        for (Map.Entry<String, Integer> hull : HULLS.entrySet()) {
            String id = hull.getKey();
            Path path = Path.of("src/main/resources/data/battlefield/recipes", id + "_repair.json");
            assertTrue(Files.exists(path), "missing repair recipe: " + id);
            JsonObject recipe = JsonParser.parseString(
                    Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString(),
                    id + " repair must be shapeless");
            assertEquals("battlefield:" + id,
                    recipe.getAsJsonObject("result").get("item").getAsString(),
                    id + " repair must return the hull itself");

            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            int iron = 0;
            boolean carriesHull = false;
            for (JsonElement element : ingredients) {
                String item = element.getAsJsonObject().get("item").getAsString();
                if (item.equals("minecraft:iron_ingot")) {
                    iron++;
                } else if (item.equals("battlefield:" + id)) {
                    carriesHull = true;
                }
            }
            assertTrue(carriesHull, id + " repair must take the damaged hull as an ingredient");
            assertEquals(hull.getValue().intValue(), iron,
                    id + " repair should consume " + hull.getValue() + " iron ingots");
        }
    }
}
