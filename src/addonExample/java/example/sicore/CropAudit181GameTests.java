package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.8.1 agriculture audit. The tests use public contracts; reflection only reaches the
 * machine cycle and the pending-drop counter.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CropAudit181GameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static PlantType plant(String name) { return MioIcifAPI.instance().getCropAPI().getPlant("mio_icif", name); }

    private static IPlanter crop(GameTestHelper h, BlockPos pos, String stick, String species, int stage) {
        h.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        h.setBlock(pos, block(stick));
        IPlanter crop = (IPlanter) h.getBlockEntity(pos);
        if (species != null) { crop.setPlant(plant(species)); crop.setGrowthStage(stage); }
        return crop;
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
        try {
            var method = target.getClass().getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(name, failure); }
    }

    /** The dirt bonus counts dirt under the farmland (below(2)..below(4)); 0.1.8.0 always counted 0. */
    @GameTest(template = "reactor_loop", batch = "agriculture181", timeoutTicks = 100)
    public static void soilNutrientsCountDirtUnderTheFarmland(GameTestHelper h) {
        BlockPos onDirt = new BlockPos(2, 5, 2), onStone = new BlockPos(5, 5, 5);
        for (int i = 2; i <= 4; i++) {
            h.setBlock(onDirt.below(i), Blocks.DIRT);
            h.setBlock(onStone.below(i), Blocks.STONE);
        }
        IPlanter a = crop(h, onDirt, "crop/stick", "wheat", 1);
        IPlanter b = crop(h, onStone, "crop/stick", "wheat", 1);
        int diff = a.getSoilNutrients() - b.getSoilNutrients();
        h.assertTrue(diff == 3, "Three dirt blocks under the farmland must add 3 soil nutrients, got " + diff);
        h.succeed();
    }

    /** The upgraded stick has its own CROSSING property; the crossing state must reach it. */
    @GameTest(template = "reactor_loop", batch = "agriculture181", timeoutTicks = 100)
    public static void upgradedStickShowsTheCrossingState(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        IPlanter crop = crop(h, pos, "crop/stick_upgraded", null, 0);
        crop.setHybridBase(true);
        crop.updateState();
        var state = h.getBlockState(pos);
        boolean crossing = state.getProperties().stream()
            .filter(p -> p instanceof BooleanProperty && p.getName().equals("crossing"))
            .map(p -> state.getValue((BooleanProperty) p)).findFirst().orElse(false);
        h.assertTrue(crossing, "Upgraded crop stick did not get the crossing state");
        h.succeed();
    }

    /** A weed from a weed block starts clean: no traits, no growth points, no crossing flag. */
    @GameTest(template = "reactor_loop", batch = "agriculture181", timeoutTicks = 100)
    public static void weedInvasionResetsTheOldPlant(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        IPlanter crop = crop(h, pos, "crop/stick", "wheat", 5);
        crop.setGrowthSpeed(31); crop.setYield(20); crop.setResilience(10); crop.setProgress(900); crop.setScanLevel(4);
        Block weedBlock = block("crop/weed");
        try {
            var method = weedBlock.getClass().getDeclaredMethod("becomeWeed", IPlanter.class, PlantType.class);
            method.setAccessible(true);
            method.invoke(null, crop, plant("weed"));
        } catch (ReflectiveOperationException failure) { throw new AssertionError("becomeWeed", failure); }
        h.assertTrue(crop.getPlant() == plant("weed") && crop.getGrowthStage() == 1, "Planter did not become a weed");
        h.assertTrue(crop.getGrowthSpeed() == 0 && crop.getYield() == 0 && crop.getResilience() == 0
            && crop.getProgress() == 0 && crop.getScanLevel() == 0 && !crop.isHybridBase(), "Weed kept state of the old plant");
        h.succeed();
    }

    /** Storage full: the harvester keeps the drops (pending list) and spills nothing into the world. */
    @GameTest(template = "reactor_loop", batch = "agriculture181", timeoutTicks = 200)
    public static void harvesterKeepsDropsWhenStorageIsFull(GameTestHelper h) {
        BlockPos machinePos = new BlockPos(6, 3, 6);
        h.setBlock(machinePos, block("producer/block_harvest_elc"));
        var machine = (AbstractEnergyBlockEntity) h.getBlockEntity(machinePos);
        machine.getEnergyStorageInternal().setEnergy(5000);
        var inv = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(machinePos), Direction.WEST);
        // 14 full slots and one slot with 63 dirt: not "full", but no room for wheat. The storage slots are output slots, which
        // refuse inserts through the capability: fill them directly
        var storage = (net.neoforged.neoforge.items.IItemHandlerModifiable) ((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity) machine).getItemHandler();
        for (int slot = 0; slot < 15; slot++) storage.setStackInSlot(slot, new ItemStack(Items.DIRT, slot == 14 ? 63 : 64));
        IPlanter crop = crop(h, new BlockPos(3, 2, 2), "crop/stick", "wheat", 7);
        crop.setYield(31);
        h.getLevel().random.setSeed(4242);
        invoke(machine, "scanAndHarvest", new Class<?>[]{Level.class, BlockPos.class}, h.getLevel(), h.absolutePos(machinePos));
        int pending = (int) invoke(machine, "pendingCount", new Class<?>[0]);
        long stacks = (5000 - machine.getEnergyStorageInternal().getAmount()) / 20;   // 20 EU per returned stack
        var spilled = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(machinePos)).inflate(3));
        h.assertTrue(spilled.isEmpty(), "Harvester spilled drops into the world");
        h.assertTrue(stacks > 0 ? pending > 0 : pending == 0, "Harvest drops are not in the pending list");
        // free one slot: the next ticks move the pending drops into storage
        inv.extractItem(0, 64, false);
        h.runAfterDelay(3, () -> {
            h.assertTrue((int) invoke(machine, "pendingCount", new Class<?>[0]) == 0, "Pending drops did not move into free storage");
            h.succeed();
        });
    }
}
