package io.github.littlebird.assembly;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the vehicle factory can build, and out of what.
 *
 * <p>A plain table on purpose: the factory reads no recipe book, and the unit test cross checks
 * every row against the generated crafting recipe for the same item. Adding a vehicle means adding
 * one row here and keeping its crafting recipe in step; forgetting either fails the build.
 */
public final class VehicleAssembly {
    /** One buildable vehicle: the item id it produces, the parts it eats and the ticks it takes. */
    public record Spec(String output, Map<String, Integer> parts, int ticks) {
        public int partCount() {
            int total = 0;
            for (int count : parts.values()) {
                total += count;
            }
            return total;
        }
    }

    /** The most expensive buildable vehicle wins a tie, so the table order only breaks exact ties. */
    public static final List<Spec> ALL = List.of(
            spec("battlefield:little_bird", 160,
                    "battlefield:airframe_section", 3,
                    "battlefield:rotor_assembly", 1,
                    "battlefield:aircraft_engine", 1,
                    "battlefield:canopy_glass", 1,
                    "battlefield:avionics_unit", 1),
            spec("battlefield:apache", 200,
                    "battlefield:apache_airframe", 1,
                    "battlefield:apache_weapons", 1,
                    "battlefield:canopy_glass", 1),
            spec("battlefield:f35", 240,
                    "battlefield:f35_airframe", 1,
                    "battlefield:aircraft_engine", 2,
                    "battlefield:f35_weapons", 1),
            spec("battlefield:su57", 240,
                    "battlefield:su57_airframe", 1,
                    "battlefield:aircraft_engine", 2,
                    "battlefield:su57_weapons", 1),
            spec("battlefield:main_battle_tank", 260,
                    "battlefield:tank_hull", 1,
                    "battlefield:armor_plate", 1,
                    "battlefield:cannon_barrel", 1,
                    "battlefield:aircraft_engine", 1,
                    "battlefield:targeting_computer", 1),
            spec("battlefield:infantry_fighting_vehicle", 220,
                    "battlefield:ifv_hull", 1,
                    "battlefield:cannon_barrel", 1,
                    "battlefield:aircraft_engine", 1,
                    "battlefield:avionics_unit", 1),
            spec("battlefield:bradley", 230,
                    "battlefield:ifv_hull", 1,
                    "battlefield:cannon_barrel", 1,
                    "battlefield:armor_plate", 1,
                    "battlefield:avionics_unit", 1,
                    "battlefield:targeting_computer", 1));

    private VehicleAssembly() {
    }

    /** The most expensive vehicle the given stock can pay for, if any. */
    public static Optional<Spec> match(Map<String, Integer> stock) {
        return ALL.stream()
                .filter(spec -> canPay(stock, spec.parts()))
                .max(Comparator.comparingInt(Spec::partCount));
    }

    /** True when every part is present in at least the required amount. */
    public static boolean canPay(Map<String, Integer> stock, Map<String, Integer> cost) {
        for (Map.Entry<String, Integer> part : cost.entrySet()) {
            if (stock.getOrDefault(part.getKey(), 0) < part.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static Spec spec(String output, int ticks, Object... parts) {
        Map<String, Integer> cost = new LinkedHashMap<>();
        for (int i = 0; i < parts.length; i += 2) {
            cost.put((String) parts[i], (Integer) parts[i + 1]);
        }
        return new Spec(output, Map.copyOf(cost), ticks);
    }
}
