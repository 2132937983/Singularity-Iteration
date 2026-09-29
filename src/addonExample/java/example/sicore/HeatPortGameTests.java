package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.PlatformHeatStorage;
import com.miophas.singularity_iteration.core.runtime.heat.HeatStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class HeatPortGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    @GameTest(batch = "heat_ports", template = "empty")
    public static void heatAdaptersAcceptBothCoreInterfaces(GameTestHelper h) {
        var api = MioIcifAPI.instance().getCapabilities();
        var numeric = new HeatStorage(100, 0, 100, 20, 1000, 0);
        numeric.generateHeatInternal(100, false);
        var port = api.adaptHeatStorage(numeric);
        h.assertTrue(port != null, "Basic heat interface must not disappear because it has no limit getters");
        h.assertTrue(port.extractHeat(40, true) == 40 && numeric.getHeatStored() == 100, "Simulation changed heat");
        h.assertTrue(port.extractHeat(40, false) == 40 && numeric.getHeatStored() == 60, "Adapter detached from owned heat");
        numeric.setHeat(20);
        h.assertTrue(port.getHeatStored() == 20 && port.getMaxExtract() == 100, "Adapter cached old heat or lost nominal limit");
        var platform = new PlatformHeatStorage(100, 0, 100);
        h.assertTrue(api.adaptHeatStorage(platform) == platform, "Existing platform capability must preserve identity");
        h.succeed();
    }

    @GameTest(batch = "heat_ports", template = "empty")
    public static void cachedExchangerHeatPortTracksRefillsAndConductorRemoval(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(id("hugenerator/block_heat_source_fluid")).defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.NORTH));
        AbstractHeatBlockEntity machine = h.getBlockEntity(pos);
        for (int slot = 4; slot < 14; slot++) machine.getItemHandler().insertItem(slot,
            new ItemStack(BuiltInRegistries.ITEM.get(id("resource/item_heatconductor"))), false);
        var fluid = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null);
        fluid.fill(new FluidStack(BuiltInRegistries.FLUID.get(id("hotcoolant")), 100), IFluidHandler.FluidAction.EXECUTE);
        var cached = new IMioIcifCapabilities.IHeatStorage[1];
        h.runAfterDelay(5, () -> {
            cached[0] = h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, h.absolutePos(pos), Direction.NORTH);
            h.assertTrue(cached[0] != null, "Missing exchanger heat port");
            h.assertTrue(h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, h.absolutePos(pos), Direction.SOUTH) == null,
                "Heat must only leave through exchanger front");
            h.assertTrue(cached[0].extractHeat(100, true) == 100 && machine.getHeatStored() == 100, "Simulation consumed heat");
            h.assertTrue(cached[0].extractHeat(100, false) == 100 && machine.getHeatStored() == 0, "Real extraction failed");
            var fresh = h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, h.absolutePos(pos), Direction.NORTH);
            h.assertTrue(fresh.extractHeat(100, false) == 0, "Separate capability query duplicated heat");
        });
        h.runAfterDelay(6, () -> {
            h.assertTrue(cached[0].getHeatStored() == 100 && cached[0].extractHeat(100, false) == 100,
                "Cached capability did not observe next refill");
            for (int slot = 4; slot < 14; slot++) machine.getItemHandler().extractItem(slot, 1, false);
        });
        h.runAfterDelay(7, () -> {
            h.assertTrue(cached[0].getMaxExtract() == 0 && !cached[0].canExtractHeat(), "Conductor removal left stale bandwidth");
            h.succeed();
        });
    }

    @GameTest(batch = "heat_ports", template = "empty")
    public static void boilerCountsUnusedHeatExactlyOnce(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(id("producer/block_steam_generator")));
        h.setBlock(pos.east(), BuiltInRegistries.BLOCK.get(id("build/block_iron_tank")));
        BlockEntity be = h.getBlockEntity(pos);
        float target = 100.0F + 221 / 220.0F * 100.0F * 2.74F;
        var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putInt("pressure", 221); tag.putInt("inputMB", 1000); tag.putFloat("systemHeat", target);
        be.loadWithComponents(tag, h.getLevel().registryAccess());
        var water = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null);
        water.fill(new FluidStack(BuiltInRegistries.FLUID.get(id("distilledwater")), 10000), IFluidHandler.FluidAction.EXECUTE);
        ((AbstractHeatBlockEntity) be).generateHeatInternal(400, false);
        h.runAfterDelay(1, () -> {
            float actual = be.saveWithoutMetadata(h.getLevel().registryAccess()).getFloat("systemHeat");
            float expected = target + (400.0F - (100.0F + 221 / 220.0F * 100.0F)) * 0.0005F;
            h.assertTrue(Math.abs(actual - expected) < 0.001F, "Unused HU counted incorrectly: " + actual + " expected " + expected);
            h.assertTrue(((AbstractHeatBlockEntity) be).getHeatStored() == 0, "HU batch was not debited");
            h.succeed();
        });
    }
}
