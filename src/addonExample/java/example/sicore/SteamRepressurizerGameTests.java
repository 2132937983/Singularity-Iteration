package example.sicore;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated native-machine regression tests, not an addon dependency example. */
@GameTestHolder("si_steam_audit")
@PrefixGameTestTemplate(false)
public final class SteamRepressurizerGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static FluidStack steam(String path, int amount) {
        return new FluidStack(BuiltInRegistries.FLUID.get(id(path)), amount);
    }
    private static AbstractHeatBlockEntity machine(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(id("producer/block_steam_repressurizer"))
            .defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        return h.getBlockEntity(pos);
    }
    private static FluidTank tank(AbstractHeatBlockEntity m, String name) {
        return (FluidTank) ArmoryServiceGameTests.call(m, name);
    }
    private static void tick(GameTestHelper h, AbstractHeatBlockEntity m) {
        ArmoryServiceGameTests.call(m.getClass(), "tick", h.getLevel(), m.getBlockPos(), m.getBlockState(), m);
    }
    private static void check(GameTestHelper h, boolean condition, String message) { h.assertTrue(condition, message); }

    @GameTest(template="empty", batch="steam_repressurizer", timeoutTicks=20)
    public static void consumesWholeHeatBatchAndAlwaysOutputsOrdinarySteam(GameTestHelper h) {
        var m = machine(h, new BlockPos(2, 1, 2));
        var input = tank(m, "getInputTank");
        var output = tank(m, "getOutputTank");
        input.fill(steam("steam", 10000), IFluidHandler.FluidAction.EXECUTE);
        m.generateHeatInternal(100, false);
        tick(h, m);
        check(h, input.getFluidAmount() == 9000 && output.getFluidAmount() == 1600 && m.getHeatStored() == 0,
            "100 HU must process 1000 mB in ONE tick; no 1-HU throttle");
        input.setFluid(steam("superheatedsteam", 10));
        m.generateHeatInternal(1, false);
        tick(h, m);
        check(h, output.getFluidAmount() == 1632 && output.getFluid().is(steam("steam", 1).getFluid()),
            "Superheated input must join existing ordinary-steam output (+32), not block or output superheated steam");
        check(h, input.getCapacity() == 10000 && output.getCapacity() == 10000, "Both tanks must be 10000 mB");
        h.succeed();
    }

    @GameTest(template="empty", batch="steam_repressurizer", timeoutTicks=20)
    public static void blocksOnInsufficientOutputSpaceAndPreservesRemainders(GameTestHelper h) {
        var m = machine(h, new BlockPos(2, 1, 2));
        var input = tank(m, "getInputTank");
        var output = tank(m, "getOutputTank");
        input.setFluid(steam("steam", 19));
        output.setFluid(steam("steam", 9985));
        m.generateHeatInternal(3, false);
        tick(h, m);
        check(h, input.getFluidAmount() == 19 && output.getFluidAmount() == 9985 && m.getHeatStored() == 3,
            "Partial output capacity must not consume a batch");
        check(h, !(boolean) ArmoryServiceGameTests.call(m, "isWorking"), "Blocked machine must not report work");
        output.drain(1, IFluidHandler.FluidAction.EXECUTE);
        tick(h, m);
        check(h, input.getFluidAmount() == 9 && output.getFluidAmount() == 10000 && m.getHeatStored() == 2,
            "Full batch must fit exactly; retain <10 mB input and leftover HU");
        h.succeed();
    }

    @GameTest(template="empty", batch="steam_repressurizer", timeoutTicks=20)
    public static void allSixFacesDrawOnDemandAndRespectSourceOrientation(GameTestHelper h) {
        for (Direction side : Direction.values()) {
            var centre = new BlockPos(2, 2, 2);
            var m = machine(h, centre);
            var sourcePos = centre.relative(side);
            h.setBlock(sourcePos, BuiltInRegistries.BLOCK.get(id("hugenerator/block_heat_generator_elc"))
                .defaultBlockState().setValue(BlockStateProperties.FACING, side.getOpposite()));
            AbstractProcessingMachineBlockEntity source = h.getBlockEntity(sourcePos);
            for (int i=1; i<=10; i++) source.getItemHandler().insertItem(i,
                new ItemStack(BuiltInRegistries.ITEM.get(id("resource/item_coil"))), false);
            var port = Objects.requireNonNull(h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
                h.absolutePos(sourcePos), side.getOpposite()));
            ((IMioIcifCapabilities.IHeatStorage) source).generateHeatInternal(100, false);
            tick(h, m);
            check(h, port.getHeatStored() == 100 && m.getHeatStored() == 0, "Empty tank drew heat on " + side);
            tank(m, "getInputTank").setFluid(steam("steam", 1000));
            tick(h, m);
            check(h, tank(m, "getOutputTank").getFluidAmount() == 1600 && port.getHeatStored() == 0,
                "Failed to draw 100 HU through face " + side);
            h.setBlock(sourcePos, net.minecraft.world.level.block.Blocks.AIR);
        }
        var m = machine(h, new BlockPos(2, 2, 2));
        var sourcePos = new BlockPos(2, 2, 1);
        h.setBlock(sourcePos, BuiltInRegistries.BLOCK.get(id("hugenerator/block_heat_generator_elc"))
            .defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        var source = (IMioIcifCapabilities.IHeatStorage) h.getBlockEntity(sourcePos);
        source.generateHeatInternal(100, false);
        tank(m, "getInputTank").setFluid(steam("steam", 1000));
        tick(h, m);
        check(h, tank(m, "getOutputTank").isEmpty() && source.getHeatStored() == 100,
            "Back of heat source must not provide heat");
        h.succeed();
    }

    @GameTest(template="empty", batch="steam_repressurizer", timeoutTicks=20)
    public static void fluidPortIsInputOnlyOutputOnlyAndDoesNotAutoEject(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        var m = machine(h, pos);
        h.setBlock(pos.north(), BuiltInRegistries.BLOCK.get(id("build/block_iron_tank")));
        for (Direction side : Direction.values()) {
            var handler = Objects.requireNonNull(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                h.absolutePos(pos), side));
            check(h, handler.fill(steam("steam", 10), IFluidHandler.FluidAction.EXECUTE) == 10,
                "Input missing on face " + side);
            check(h, handler.drain(100, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "Input is externally drainable");
            check(h, handler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 10),
                IFluidHandler.FluidAction.EXECUTE) == 0, "Water accepted");
        }
        m.generateHeatInternal(6, false);
        tick(h, m);
        check(h, tank(m, "getOutputTank").getFluidAmount() == 96, "Output automatically ejected without a pump");
        var handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, m.getBlockPos(), null);
        check(h, handler.drain(96, IFluidHandler.FluidAction.SIMULATE).getAmount() == 96
            && tank(m, "getOutputTank").getFluidAmount() == 96, "Fluid simulation mutated output");
        check(h, handler.drain(96, IFluidHandler.FluidAction.EXECUTE).getAmount() == 96, "Cannot extract output");
        check(h, !tank(m, "getOutputTank").isFluidValid(steam("superheatedsteam", 10)), "Wrong output validator");
        h.succeed();
    }

    @GameTest(template="empty", batch="steam_repressurizer", timeoutTicks=20)
    public static void oldSaveBalancesAndItemsSurviveAndMigrationRunsOnce(GameTestHelper h) throws Exception {
        var m = machine(h, new BlockPos(2, 1, 2));
        ((net.neoforged.neoforge.items.ItemStackHandler) m.getItemHandler()).setStackInSlot(2, new ItemStack(Items.DIAMOND, 3));
        tank(m, "getOutputTank").setFluid(steam("superheatedsteam", 15000));
        var tag = m.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.remove("repressurizerVersion");
        tag.putLong("heat", 12345); tag.putInt("currentHeat", 7);
        m.loadWithComponents(tag, h.getLevel().registryAccess());
        check(h, m.getHeatStored() == 12352 && tank(m, "getOutputTank").getFluidAmount() == 15000,
            "Legacy balances were truncated");
        check(h, tank(m, "getOutputTank").getFluid().is(steam("steam", 1).getFluid()), "Legacy product not migrated");
        var again = m.saveWithoutMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(again, h.getLevel().registryAccess());
        check(h, m.getHeatStored() == 12352, "Reload duplicated HU");
        var recovery = Objects.requireNonNull(h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, m.getBlockPos(), null));
        check(h, recovery.insertItem(0, new ItemStack(Items.STICK), false).getCount() == 1, "New machine accepted a cell/upgrade");
        check(h, recovery.extractItem(2, 3, false).getCount() == 3, "Legacy items cannot be recovered");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = m.createMenu(1, player.getInventory(), player);
        check(h, menu != null && menu.slots.size() == 36 && menu.slots.get(0).x == 8 && menu.slots.get(0).y == 84
            && menu.slots.get(27).y == 142, "GUI must contain only IC2's player inventory");
        var data = (ContainerData) ArmoryServiceGameTests.call(m, "getContainerData");
        check(h, data.getCount() == 8 && data.get(7) == BuiltInRegistries.FLUID.getId(steam("steam", 1).getFluid()),
            "Output fluid identity missing from menu sync");
        // The two-argument constructor follows the client's no-BE path. Feed it the same
        // synchronized fields and verify that tank renderers get usable fluid stacks.
        var receiver = menu.getClass().getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class)
            .newInstance(2, player.getInventory());
        var receivedData = (ContainerData) ArmoryServiceGameTests.call(receiver, "getData");
        for (int i=0; i<data.getCount(); i++) receivedData.set(i, data.get(i));
        var receivedOutput = (FluidStack) ArmoryServiceGameTests.call(receiver, "getOutputFluid");
        check(h, receivedOutput.getAmount() == 15000 && receivedOutput.is(steam("steam", 1).getFluid()),
            "Client menu cannot reconstruct synchronized output");
        receivedData.set(0, 32);
        receivedData.set(6, BuiltInRegistries.FLUID.getId(steam("superheatedsteam", 1).getFluid()));
        check(h, ((FluidStack) ArmoryServiceGameTests.call(receiver, "getInputFluid")).is(steam("superheatedsteam", 1).getFluid()),
            "Client menu ignores input fluid type changes");
        receivedData.set(0, 0);
        check(h, ((FluidStack) ArmoryServiceGameTests.call(receiver, "getInputFluid")).isEmpty(),
            "Client kept a stale fluid stack after draining");
        h.succeed();
    }

    @GameTest(template="empty", batch="steam_repressurizer", timeoutTicks=40)
    public static void realElectricHeatSourceProcessesContinuously(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        var m = machine(h, pos);
        var sourcePos = pos.south();
        h.setBlock(sourcePos, BuiltInRegistries.BLOCK.get(id("hugenerator/block_heat_generator_elc"))
            .defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        AbstractProcessingMachineBlockEntity source = h.getBlockEntity(sourcePos);
        for (int i=1; i<=10; i++) source.getItemHandler().insertItem(i,
            new ItemStack(BuiltInRegistries.ITEM.get(id("resource/item_coil"))), false);
        ArmoryServiceGameTests.call(ArmoryServiceGameTests.call(source, "getEnergyStorageInternal"), "setEnergy", 1000L);
        tank(m, "getInputTank").setFluid(steam("steam", 10000));
        int[] last = {-1};
        for (int delay=2; delay<=5; delay++) {
            final int currentDelay = delay;
            h.runAfterDelay(delay, () -> {
                int produced = tank(m, "getOutputTank").getFluidAmount();
                check(h, produced >= 1600, "Real source did not supply 100 HU/t");
                if (last[0] >= 0) check(h, produced - last[0] == 1600,
                    "Continuous source stalled: output delta " + (produced-last[0]));
                check(h, tank(m, "getInputTank").getFluidAmount() == 10000 - produced / 16 * 10,
                    "Steam batch ratio drifted");
                last[0] = produced;
                if (currentDelay == 5) h.succeed();
            });
        }
    }

    @GameTest(template="empty", batch="steam_repressurizer", timeoutTicks=20)
    public static void configurableConversionRatesAreActuallyUsed(GameTestHelper h) throws Exception {
        var normal = (net.neoforged.neoforge.common.ModConfigSpec.IntValue)
            CommonConfigGameTests.config("REPRESSURIZER_STEAM_OUTPUT");
        var superheated = (net.neoforged.neoforge.common.ModConfigSpec.IntValue)
            CommonConfigGameTests.config("REPRESSURIZER_SUPERHEATED_OUTPUT");
        int beforeNormal = normal.get(), beforeSuper = superheated.get();
        try {
            normal.set(24); superheated.set(48);
            var m = machine(h, new BlockPos(2, 1, 2));
            tank(m, "getInputTank").setFluid(steam("steam", 10)); m.generateHeatInternal(1, false); tick(h, m);
            tank(m, "getInputTank").setFluid(steam("superheatedsteam", 10)); m.generateHeatInternal(1, false); tick(h, m);
            check(h, tank(m, "getOutputTank").getFluidAmount() == 72 && m.getHeatStored() == 0,
                "Configured 24/48 mB conversion did not take effect");
        } finally { normal.set(beforeNormal); superheated.set(beforeSuper); }
        h.succeed();
    }
}
