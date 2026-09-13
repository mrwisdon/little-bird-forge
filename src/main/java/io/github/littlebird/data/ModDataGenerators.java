package io.github.littlebird.data;

import io.github.littlebird.LittleBird;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Data generation hook. The recipes are written in Java rather than as hand written JSON so a
 * mistyped ingredient fails the build instead of quietly producing an uncraftable recipe.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) {
            return;
        }
        event.getGenerator().addProvider(true,
                new ModRecipeProvider(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(event.includeClient(), new ModItemModelProvider(
                event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
    }
}
