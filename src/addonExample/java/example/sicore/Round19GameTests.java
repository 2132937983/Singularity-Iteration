package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.7.19: chargeable items never stack, every armor has an equip sound, the reactor feed-rate
 * multiplier (linear EU / fuel, super-linear heat, clamped and persisted), the METS solar
 * discharge buffer slot and the Steam Re-pressurizer recipe.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round19GameTests {
    private Round19GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }

    @GameTest(batch = "round19", template = "empty")
    public static void chargeableItemsNeverStack(GameTestHelper h) {
        int checked = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!"mio_icif".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) continue;
            if (!(item instanceof IBatteryItem)) continue;
            checked++;
            h.assertTrue(new ItemStack(item).getMaxStackSize() == 1,
                "Chargeable item stacks: " + BuiltInRegistries.ITEM.getKey(item) + " max " + new ItemStack(item).getMaxStackSize());
        }
        h.assertTrue(checked > 20, "Expected many chargeable items, found " + checked);
        h.assertTrue(item("item_tool_laser_miner").getMaxStackSize() == 1, "Mining laser must not stack");
        // two half-charged lasers with different charge can never merge
        ItemStack a = item("item_tool_laser_miner"), b = item("item_tool_laser_miner");
        ((IBatteryItem) a.getItem()).setEnergy(a, 1000);
        ((IBatteryItem) b.getItem()).setEnergy(b, 2000);
        h.assertTrue(!ItemStack.isSameItemSameComponents(a, b) || a.getMaxStackSize() == 1, "Lasers merge");
        h.succeed();
    }

    @GameTest(batch = "round19", template = "empty")
    public static void everyArmorHasAnEquipSound(GameTestHelper h) {
        int checked = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!"mio_icif".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()) || !(item instanceof ArmorItem armor)) continue;
            checked++;
            h.assertTrue(armor.getEquipSound() != null, "Null equip sound: " + BuiltInRegistries.ITEM.getKey(item));
        }
        h.assertTrue(checked >= 20, "Expected the SI armor set, found " + checked);
        h.succeed();
    }

    static Object call(Object target, String name, Object... args) {
        return ArmoryServiceGameTests.call(target, name, args);
    }

    private static IReactorController reactor(GameTestHelper h, BlockPos pos, int feed) {
        h.setBlock(pos, block("generator/block_nuclear_reactor_generator"));
        IReactorController reactor = (IReactorController) h.getBlockEntity(pos);
        call(reactor, "setFeedRate", feed);
        h.assertTrue(reactor.getItemHandler().insertItem(10, item("reactor/item_reactor_uranium_simple"), false).isEmpty(), "Fuel rejected");
        h.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
        return reactor;
    }

    private static int fuelRemaining(GameTestHelper h, ItemStack stack) {
        return ((CompoundTag) stack.save(h.getLevel().registryAccess())).getCompound("components")
            .getCompound("mio_icif:fuel_rod_durability").getInt("remaining_uses");
    }

    @GameTest(batch = "round19", template = "empty", timeoutTicks = 60)
    public static void reactorFeedRateScalesOutputLinearlyAndHeatSuperLinearly(GameTestHelper h) {
        var normal = reactor(h, new BlockPos(1, 1, 1), 1);
        var fast = reactor(h, new BlockPos(4, 1, 1), 4);
        var clamp = reactor(h, new BlockPos(1, 1, 4), 99);
        h.assertTrue((int) call(clamp, "getFeedRate") == 16, "Feed rate clamps to 16x");
        h.runAfterDelay(25, () -> {
            h.assertTrue(normal.getCurrentEnergyGeneration() == 5 && normal.getCurrentHeat() == 4, "1x: 5 EU/t and 4 HU, got "
                + normal.getCurrentEnergyGeneration() + " / " + normal.getCurrentHeat());
            h.assertTrue(fuelRemaining(h, normal.getItemHandler().getStackInSlot(10)) == 19999, "1x burns one cycle");
            h.assertTrue(fast.getCurrentEnergyGeneration() == 20, "4x: EU/t is linear, got " + fast.getCurrentEnergyGeneration());
            h.assertTrue(fuelRemaining(h, fast.getItemHandler().getStackInSlot(10)) == 19996, "4x burns four cycles");
            long expectedHeat = Math.round(4 * Math.pow(4, 1.6));   // 4 HU per cycle x 4^1.6
            h.assertTrue(Math.abs(fast.getCurrentHeat() - expectedHeat) <= 1, "4x heat must be ~" + expectedHeat + ", got " + fast.getCurrentHeat());
            // persisted
            var be = h.getBlockEntity(new BlockPos(4, 1, 1));
            CompoundTag tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
            h.assertTrue(tag.getInt("FeedRate") == 4, "Feed rate saved");
            call(fast, "setFeedRate", 1);
            be.loadWithComponents(tag, h.getLevel().registryAccess());
            h.assertTrue((int) call(fast, "getFeedRate") == 4, "Feed rate restored");
            h.succeed();
        });
    }

    @GameTest(batch = "round19", template = "empty", timeoutTicks = 40)
    public static void photonSolarChargesBatteryInItsOnlySlot(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, block("generator/block_photon_resonance_solar_generator"));
        var gen = (AbstractEnergyBlockEntity) h.getBlockEntity(pos);
        ItemStack battery = item("normal/item_advbat_lev0");
        ((IBatteryItem) battery.getItem()).setEnergy(battery, 0);
        var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), null);
        h.assertTrue(handler != null && handler.insertItem(0, battery, false).isEmpty(), "Charge slot accepts a battery");
        h.onEachTick(() -> gen.getEnergyStorageInternal().setStored(gen.getEnergyStorageInternal().getCapacity() / 2));
        h.runAfterDelay(10, () -> {
            ItemStack charged = handler.getStackInSlot(0);
            h.assertTrue(((IBatteryItem) charged.getItem()).getEnergy(charged) > 0, "Generator charges the battery from its buffer");
            h.succeed();
        });
    }

    @GameTest(batch = "round19", template = "empty")
    public static void steamRepressurizerIsCraftable(GameTestHelper h) {
        var target = BuiltInRegistries.ITEM.get(id("producer/block_steam_repressurizer"));
        boolean found = h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
            .anyMatch(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(target));
        h.assertTrue(found, "No crafting recipe makes the Steam Re-pressurizer");
        h.succeed();
    }
}
