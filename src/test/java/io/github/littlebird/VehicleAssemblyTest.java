package io.github.littlebird;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.littlebird.assembly.VehicleAssembly;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The factory builds from a plain table, and the table is only trustworthy while it agrees with
 * the generated crafting recipes. Every row is compared against the recipe that produces the same
 * item, so a recipe change that forgets the factory fails the build.
 */
final class VehicleAssemblyTest {
    private static final Path RECIPES = Path.of("src/generated/resources/data/battlefield/recipes");

    @Test
    void everyFactoryRowMatchesItsCraftingRecipe() throws IOException {
        for (VehicleAssembly.Spec spec : VehicleAssembly.ALL) {
            String name = spec.output().substring("battlefield:".length());
            Path path = RECIPES.resolve(name + ".json");
            assertTrue(Files.exists(path), "no crafting recipe for " + spec.output());
            JsonObject recipe = JsonParser.parseString(
                    Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(spec.parts(), ingredientsOf(recipe),
                    spec.output() + " factory parts disagree with its crafting recipe");
        }
    }

    @Test
    void theFactoryPicksTheMostExpensiveBuildableVehicle() {
        assertTrue(VehicleAssembly.match(Map.of()).isEmpty(), "empty stock must build nothing");
        Map<String, Integer> stock = new HashMap<>();
        stock.put("battlefield:airframe_section", 3);
        stock.put("battlefield:rotor_assembly", 1);
        stock.put("battlefield:aircraft_engine", 1);
        stock.put("battlefield:canopy_glass", 1);
        stock.put("battlefield:avionics_unit", 1);
        Optional<VehicleAssembly.Spec> built = VehicleAssembly.match(stock);
        assertTrue(built.isPresent(), "the little bird parts should build something");
        assertEquals("battlefield:little_bird", built.get().output());
    }

    private static Map<String, Integer> ingredientsOf(JsonObject recipe) {
        Map<String, Integer> cost = new HashMap<>();
        if (recipe.get("type").getAsString().equals("minecraft:crafting_shapeless")) {
            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            for (JsonElement element : ingredients) {
                count(cost, element.getAsJsonObject().get("item").getAsString());
            }
        } else {
            JsonObject key = recipe.getAsJsonObject("key");
            for (JsonElement row : recipe.getAsJsonArray("pattern")) {
                for (char symbol : row.getAsString().toCharArray()) {
                    if (symbol != ' ') {
                        count(cost, key.getAsJsonObject(String.valueOf(symbol))
                                .get("item").getAsString());
                    }
                }
            }
        }
        return cost;
    }

    private static void count(Map<String, Integer> cost, String item) {
        cost.merge(item, 1, Integer::sum);
    }
}
