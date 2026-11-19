package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.heat.IHeatAPI;
import com.miophas.singularity_iteration.core.api.kinetic.IKineticAPI;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.87: the heat and kinetic APIs see every block that exposes the matching capability, and the Water Kinetic
 * Generator takes rotors from automation as the Wind Kinetic Generator does.
 * <ul>
 *   <li>The heat API (MioIcifAPI.getHeatAPI) knew only block entities that implement the core API heat interfaces, as the
 *       kinetic API did before 0.1.11.86. SI heat blocks that only expose HEAT_STORAGE_BLOCK were invisible to it.</li>
 *   <li>The audit goes over every SI block with a block entity: each one that exposes the capability on some face must
 *       be a heat (kinetic) tile with a storage for the API. New blocks are checked without a new test.</li>
 *   <li>The Water Kinetic Generator had an automation item handler (rotors only) that was never registered.</li>
 * </ul>
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11587GameTests {
    private Round11587GameTests() {}

    private static final Direction[] FACES = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    /** Whether a block entity not in the world exposes {@code cap}; a provider that fails on it counts as "maybe". */
    private static boolean mayExpose(ServerLevel level, BlockPos pos, BlockState state, BlockEntity loose, BlockCapability<?, Direction> cap) {
        for (Direction face : FACES) {
            try {
                if (level.getCapability(cap, pos, state, loose, face) != null) return true;
            } catch (Throwable t) {
                return true;
            }
        }
        return false;
    }

    private static boolean exposes(ServerLevel level, BlockPos pos, BlockCapability<?, Direction> cap) {
        for (Direction face : FACES) if (level.getCapability(cap, pos, face) != null) return true;
        return false;
    }

    @GameTest(template = "empty", batch = "round11587_api_audit", timeoutTicks = 100)
    public static void heatAndKineticApisSeeEveryCapabilityBlock(GameTestHelper h) {
        h.runAfterDelay(1, () -> {
            ServerLevel level = h.getLevel();
            BlockPos rel = new BlockPos(2, 1, 2), abs = h.absolutePos(rel);
            IHeatAPI heat = MioIcifAPI.instance().getHeatAPI();
            IKineticAPI kinetic = MioIcifAPI.instance().getKineticAPI();
            List<String> heatBlocks = new ArrayList<>(), kineticBlocks = new ArrayList<>(), wrong = new ArrayList<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
                if (!key.getNamespace().equals("mio_icif") || !(block instanceof EntityBlock entityBlock)) continue;
                BlockState state = block.defaultBlockState();
                BlockEntity loose;
                try {
                    loose = entityBlock.newBlockEntity(abs, state);
                } catch (Throwable t) {
                    continue;
                }
                if (loose == null) continue;
                loose.setLevel(level);
                if (!mayExpose(level, abs, state, loose, IMioIcifCapabilities.HEAT_STORAGE_BLOCK)
                    && !mayExpose(level, abs, state, loose, IMioIcifCapabilities.KINETIC_STORAGE_BLOCK)) continue;
                try {
                    h.setBlock(rel, state);
                    if (exposes(level, abs, IMioIcifCapabilities.HEAT_STORAGE_BLOCK)) {
                        heatBlocks.add(key.getPath());
                        if (!heat.hasHeatTile(level, abs)) wrong.add(key.getPath() + ": hasHeatTile false");
                        if (heat.getHeatStorage(level, abs).isEmpty()) wrong.add(key.getPath() + ": no heat storage");
                    }
                    if (exposes(level, abs, IMioIcifCapabilities.KINETIC_STORAGE_BLOCK)) {
                        kineticBlocks.add(key.getPath());
                        if (!kinetic.hasKineticTile(level, abs)) wrong.add(key.getPath() + ": hasKineticTile false");
                        if (kinetic.getKineticStorage(level, abs).isEmpty()) wrong.add(key.getPath() + ": no kinetic storage");
                    }
                } catch (Throwable t) {
                    wrong.add(key.getPath() + ": " + t);
                } finally {
                    h.setBlock(rel, Blocks.AIR);
                }
            }
            // the audit must have found the heat machines and the kinetic blocks, or it checks nothing
            h.assertTrue(heatBlocks.size() >= 12, "only " + heatBlocks.size() + " heat capability blocks found: " + heatBlocks);
            h.assertTrue(kineticBlocks.size() >= 9, "only " + kineticBlocks.size() + " kinetic capability blocks found: " + kineticBlocks);
            h.assertTrue(wrong.isEmpty(), "APIs miss capability blocks (" + heatBlocks.size() + " heat, " + kineticBlocks.size() + " kinetic): " + wrong);
            h.succeed();
        });
    }

    /** HU goes into a heat consumer through the heat API, as a heat source beside it would put it there. */
    @GameTest(template = "empty", batch = "round11587_api_audit", timeoutTicks = 40)
    public static void heatApiMovesHeatIntoSiMachines(GameTestHelper h) {
        BlockPos boiler = new BlockPos(1, 1, 1);
        h.setBlock(boiler, BuiltInRegistries.BLOCK.get(id("producer/block_steam_generator")));
        h.runAfterDelay(2, () -> {
            IHeatAPI heat = MioIcifAPI.instance().getHeatAPI();
            BlockPos abs = h.absolutePos(boiler);
            var storage = heat.getHeatStorage(h.getLevel(), abs);
            h.assertTrue(storage.isPresent(), "the heat API has no storage for the Steam Generator");
            long before = storage.get().getHeatStored();
            long simulated = heat.insertHeat(h.getLevel(), abs, 100L, true);
            long inserted = heat.insertHeat(h.getLevel(), abs, 100L, false);
            h.assertTrue(inserted > 0 && simulated == inserted, "the Steam Generator took " + simulated + " (simulated) and " + inserted + " HU through the heat API");
            h.assertTrue(storage.get().getHeatStored() - before == inserted,
                "the Steam Generator stores " + (storage.get().getHeatStored() - before) + " HU more after taking " + inserted);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "round11587_api_audit", timeoutTicks = 40)
    public static void waterKineticGeneratorTakesRotorsFromAutomation(GameTestHelper h) {
        BlockPos water = new BlockPos(1, 1, 1);
        h.setBlock(water, BuiltInRegistries.BLOCK.get(id("kugenerator/block_water_kinetic_generator")));
        h.runAfterDelay(2, () -> {
            IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(water), Direction.UP);
            h.assertTrue(items != null, "the Water Kinetic Generator has no item handler for pipes and hoppers");
            ItemStack wood = new ItemStack(BuiltInRegistries.ITEM.get(id("rotors/item_wood_rotor")));
            ItemStack bronze = new ItemStack(BuiltInRegistries.ITEM.get(id("rotors/item_bronze_rotor")));
            h.assertTrue(items.insertItem(0, new ItemStack(Items.STICK), false).getCount() == 1, "the Water Kinetic Generator took a stick");
            h.assertTrue(items.insertItem(0, wood, false).getCount() == 1, "the Water Kinetic Generator took a wooden rotor (not for water)");
            h.assertTrue(items.insertItem(0, bronze, false).isEmpty(), "the Water Kinetic Generator refused a bronze rotor");
            ItemStack inSlot = (ItemStack) Round33GameTests.call(h.getBlockEntity(water), "getRotorStack");
            h.assertTrue(inSlot.is(bronze.getItem()), "the rotor slot holds " + inSlot + " after a pipe put a bronze rotor in");
            h.succeed();
        });
    }
}
