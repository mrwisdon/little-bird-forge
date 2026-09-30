package io.github.littlebird.block;

import io.github.littlebird.assembly.VehicleAssembly;
import io.github.littlebird.registry.ModBlockEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Holds the parts, counts the build down and holds the finished vehicle.
 *
 * <p>Slots 0-8 are the 3x3 of parts, slot 9 is the finished vehicle. Automation may push parts
 * into the first nine and pull the vehicle out of the last one; it can never do the reverse.
 */
public class VehicleFactoryBlockEntity extends BlockEntity {
    /** The 3x3 of parts a recipe consumes. */
    public static final int INPUT_SLOTS = 9;
    /** One finished vehicle. */
    public static final int OUTPUT_SLOT = INPUT_SLOTS;

    private final ItemStackHandler inventory = new ItemStackHandler(INPUT_SLOTS + 1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != OUTPUT_SLOT;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == OUTPUT_SLOT ? stack : super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT_SLOT ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LazyOptional<ItemStackHandler> itemHandler = LazyOptional.of(() -> inventory);

    private String building;
    private int progress;

    public VehicleFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VEHICLE_FACTORY.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** Hoppers see the parts and the finished vehicle; the block itself is the only other user. */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == ForgeCapabilities.ITEM_HANDLER
                ? itemHandler.cast()
                : super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    /** Server side ticker registered by the block. */
    public static void tick(Level level, BlockPos pos, BlockState state,
            VehicleFactoryBlockEntity factory) {
        factory.buildStep();
    }

    private void buildStep() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            stop();
            return;
        }
        Optional<VehicleAssembly.Spec> match = VehicleAssembly.match(stock());
        if (match.isEmpty()) {
            stop();
            return;
        }
        VehicleAssembly.Spec spec = match.get();
        if (!spec.output().equals(building)) {
            building = spec.output();
            progress = 0;
        }
        progress++;
        setChanged();
        if (progress < spec.ticks()) {
            return;
        }
        if (!consume(spec.parts())) {
            stop();
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(spec.output());
        Item finished = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        if (finished != null) {
            inventory.setStackInSlot(OUTPUT_SLOT, new ItemStack(finished));
        }
        stop();
    }

    private Map<String, Integer> stock() {
        Map<String, Integer> stock = new HashMap<>();
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stock.merge(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(),
                        stack.getCount(), Integer::sum);
            }
        }
        return stock;
    }

    private boolean consume(Map<String, Integer> parts) {
        if (!VehicleAssembly.canPay(stock(), parts)) {
            return false;
        }
        for (Map.Entry<String, Integer> part : parts.entrySet()) {
            int left = part.getValue();
            for (int slot = 0; slot < INPUT_SLOTS && left > 0; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()
                        && ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()
                                .equals(part.getKey())) {
                    int taken = Math.min(left, stack.getCount());
                    ItemStack kept = stack.copy();
                    kept.shrink(taken);
                    inventory.setStackInSlot(slot, kept);
                    left -= taken;
                }
            }
        }
        return true;
    }

    private void stop() {
        building = null;
        progress = 0;
    }

    /** Called by the block: put parts in, take the vehicle out, sneak to empty the hopper. */
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                ejectParts();
                return;
            }
            ItemStack finished = inventory.getStackInSlot(OUTPUT_SLOT);
            if (!finished.isEmpty()) {
                inventory.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
                if (!player.getInventory().add(finished)) {
                    player.drop(finished, false);
                }
            }
            return;
        }
        ItemStack remainder = held.copy();
        for (int slot = 0; slot < INPUT_SLOTS && !remainder.isEmpty(); slot++) {
            remainder = inventory.insertItem(slot, remainder, false);
        }
        player.setItemInHand(hand, remainder);
    }

    private void ejectParts() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                Block.popResource(level, worldPosition, stack);
            }
        }
        stop();
    }

    /** Drops everything when the block is broken. */
    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < INPUT_SLOTS + 1; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                Block.popResource(level, worldPosition, stack);
            }
        }
    }

    /** 0 idle, 15 with a finished vehicle waiting, otherwise the build progress. */
    public int comparatorSignal() {
        if (!inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            return 15;
        }
        if (building == null || progress <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(14, 1 + progress * 13 / ticksOf(building)));
    }

    private static int ticksOf(String output) {
        for (VehicleAssembly.Spec spec : VehicleAssembly.ALL) {
            if (spec.output().equals(output)) {
                return spec.ticks();
            }
        }
        return 1;
    }
}
