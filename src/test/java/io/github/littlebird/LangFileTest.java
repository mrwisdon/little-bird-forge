package io.github.littlebird;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Checks the language files the way the game does.
 *
 * A single missing comma makes a lang file unparseable, and Minecraft then shows the raw keys -
 * item.battlefield.airframe_section instead of Airframe Section - in the creative tab and in JEI,
 * with no error anywhere. That happened, so it is tested now.
 */
final class LangFileTest {
    private static final Path LANG = Path.of("src/main/resources/assets/battlefield/lang");
    private static final String[] LANGUAGES = {"en_us.json", "zh_cn.json"};
    /** Keys the code looks up, spot checked: a missing one always shows as a raw key. */
    private static final String[] REQUIRED = {
        "item.battlefield.little_bird",
        "item.battlefield.apache",
        "item.battlefield.airframe_section",
        "block.battlefield.ammo_crate",
        "hud.battlefield.ammo",
        "hud.battlefield.weapon.rockets",
        "hud.battlefield.weapon.apache_rockets",
        "advancements.battlefield.apache.title",
    };

    @Test
    void bothLanguagesParse() throws IOException {
        for (String language : LANGUAGES) {
            JsonObject json = read(language);
            assertTrue(json.size() > 0, language + " is empty");
        }
    }

    @Test
    void bothLanguagesDefineTheSameKeys() throws IOException {
        Set<String> english = read("en_us.json").keySet();
        Set<String> chinese = read("zh_cn.json").keySet();
        Set<String> missingFromChinese = new LinkedHashSet<>(english);
        missingFromChinese.removeAll(chinese);
        Set<String> missingFromEnglish = new LinkedHashSet<>(chinese);
        missingFromEnglish.removeAll(english);
        assertTrue(missingFromChinese.isEmpty(), "zh_cn is missing " + missingFromChinese);
        assertTrue(missingFromEnglish.isEmpty(), "en_us is missing " + missingFromEnglish);
    }

    @Test
    void everyKeyTheCodeUsesExistsAndIsNotBlank() throws IOException {
        for (String language : LANGUAGES) {
            JsonObject json = read(language);
            for (String key : REQUIRED) {
                assertTrue(json.has(key), language + " has no entry for " + key);
                assertEquals(false, json.get(key).getAsString().isBlank(),
                        language + " leaves " + key + " blank");
            }
            for (String key : json.keySet()) {
                assertFalse(json.get(key).getAsString().isBlank(), language + " blanks " + key);
            }
        }
    }

    @Test
    void theChineseFileIsReallyChinese() throws IOException {
        // The first character of the crate name has to be a CJK ideograph: if the file was ever
        // written through the wrong encoding it comes back as mojibake, which is what happened.
        String crate = read("zh_cn.json").get("block.battlefield.ammo_crate").getAsString();
        char first = crate.charAt(0);
        assertTrue(Character.UnicodeScript.of(first) == Character.UnicodeScript.HAN,
                "expected a Han character, got U+" + Integer.toHexString(first));
    }

    private static JsonObject read(String language) throws IOException {
        Path path = LANG.resolve(language);
        assertTrue(Files.exists(path), path + " does not exist");
        JsonElement parsed = JsonParser.parseString(
                Files.readString(path, StandardCharsets.UTF_8));
        assertTrue(parsed.isJsonObject(), language + " is not a JSON object");
        return parsed.getAsJsonObject();
    }
}
