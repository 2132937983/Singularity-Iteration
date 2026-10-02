package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class UserReactorLoopGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static IFluidHandler fluids(GameTestHelper h, BlockPos at) {
        var handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(at), null);
        h.assertTrue(handler != null, "Missing fluid handler at " + at);
        return handler;
    }
    private static void insert(GameTestHelper h, BlockPos at, int slot, String item) {
        var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), null);
        h.assertTrue(handler != null && handler.insertItem(slot, item(item), false).isEmpty(), "Rejected item at " + at + " slot " + slot);
    }

    /** Real reactor, nine independent ports, nine LHEs and three real 221-bar boilers.
     * Distilled-water supply and steam consumers are external test boundaries;
     * nothing injects coolant, moves it manually or extracts HU for the machines.
     */
    @GameTest(batch = "user_reactor_loop", template = "reactor_loop", timeoutTicks = 10100)
    public static void nineExchangersWithFourFourOneBoilersSustainUserLayout(GameTestHelper h) {
        var center = new BlockPos(6, 6, 6);
        h.setBlock(center, block("generator/block_nuclear_reactor_generator"));
        for (var side : Direction.values()) h.setBlock(center.relative(side), block("reactor/block_reactor_chamber"));
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++)
            if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) == 2)
                h.setBlock(center.offset(x, y, z), block("reactor/block_reactor_vessel"));
        h.setBlock(center.west(2), block("reactor/block_reactor_redstone_port"));
        var boilers = new BlockPos[] {center.above(3), center.below(3), center.east(4)};
        var sinks = new BlockPos[] {boilers[0].above(), boilers[1].below(), boilers[2].east()};
        var exchangers = new ArrayList<BlockPos>();
        var ports = new ArrayList<BlockPos>();
        for (int group = 0; group < 3; group++) {
            h.setBlock(boilers[group], block("producer/block_steam_generator"));
            BlockEntity be = h.getBlockEntity(boilers[group]);
            var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
            tag.putInt("pressure", 221); tag.putInt("inputMB", group == 2 ? 1 : 1000);
            be.loadWithComponents(tag, h.getLevel().registryAccess());
            h.setBlock(sinks[group], block("build/block_iron_tank"));
            var sides = group < 2 ? new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}
                : new Direction[] {Direction.WEST};
            for (var side : sides) {
                var exchanger = boilers[group].relative(side);
                var port = group < 2 ? exchanger.relative(group == 0 ? Direction.DOWN : Direction.UP) : exchanger.west();
                h.setBlock(port, block("reactor/block_reactor_fluid_port"));
                h.setBlock(exchanger, block("hugenerator/block_heat_source_fluid").defaultBlockState()
                    .setValue(BlockStateProperties.FACING, side.getOpposite()));
                for (int slot = 4; slot < 14; slot++) insert(h, exchanger, slot, "resource/item_heatconductor");
                insert(h, exchanger, 14, "upgrade/fluid_ejector_upgrade");
                insert(h, port, 0, "upgrade/fluid_ejector_upgrade");
                exchangers.add(exchanger); ports.add(port);
            }
        }
        IReactorController reactor = (IReactorController) h.getBlockEntity(center);
        String[] rows = {"CYAAJAJAA", "YSYJCJCJA", "AYJCJCJCJ", "AJCJCJCJA", "JCJCJCJCJ", "AJAJAJAJA"};
        for (int y = 0; y < 6; y++) for (int x = 0; x < 9; x++) {
            String suffix = switch (rows[y].charAt(x)) {
                case 'C' -> "golden_vent"; case 'Y' -> "iridium_reflector";
                case 'S' -> "uranium_quad"; case 'J' -> "vent_spread"; default -> null;
            };
            if (suffix != null) h.assertTrue(reactor.getItemHandler().insertItem(y * 9 + x,
                item("reactor/item_reactor_" + suffix), false).isEmpty(), "Rejected layout item");
        }
        var cold = BuiltInRegistries.FLUID.get(id("coolant"));
        var water = BuiltInRegistries.FLUID.get(id("distilledwater"));
        h.runAfterDelay(5, () -> {
            h.assertTrue(reactor.isValidFluidReactorStructure(), "Reactor did not form");
            h.assertTrue(reactor.getFluidHandler().fill(new FluidStack(cold, 2000), EXECUTE) == 2000, "Initial coolant charge rejected");
            h.setBlock(center.west(3), Blocks.REDSTONE_BLOCK);
        });
        long[] steam = new long[3];
        h.onEachTick(() -> {
            for (int i = 0; i < 3; i++) {
                fluids(h, boilers[i]).fill(new FluidStack(water, 10000), EXECUTE);
                steam[i] += fluids(h, sinks[i]).drain(10000, EXECUTE).getAmount();
            }
            if (h.getTick() < 6) return;
            if (h.getTick() == 30) h.assertTrue(reactor.isRunning(), "Reactor never started");
            var own = reactor.getFluidHandler();
            int total = own.getFluidInTank(0).getAmount() + own.getFluidInTank(1).getAmount();
            var amounts = new ArrayList<Integer>();
            var buffered = new ArrayList<Long>();
            var temperatures = new ArrayList<Float>();
            for (var at : boilers) {
                BlockEntity boiler = h.getBlockEntity(at);
                temperatures.add(boiler.saveWithoutMetadata(h.getLevel().registryAccess()).getFloat("systemHeat"));
            }
            for (var at : exchangers) {
                var handler = fluids(h, at);
                total += handler.getFluidInTank(0).getAmount() + handler.getFluidInTank(1).getAmount();
                amounts.add(handler.getFluidInTank(0).getAmount());
                buffered.add(((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity) h.getBlockEntity(at)).getHeatStored());
            }
            if (h.getTick() % 1000 == 0) System.out.println("USER_LOOP tick=" + h.getTick() + " cold=" + own.getFluidInTank(0).getAmount()
                + " hot=" + own.getFluidInTank(1).getAmount() + " exchangerHot=" + amounts + " hull=" + reactor.getCurrentHeat()
                + " steam=" + java.util.Arrays.toString(steam) + " HU=" + buffered + " boilerTemp=" + temperatures);
            h.assertTrue(total == 2000, "Coolant lost: total=" + total);
            h.assertTrue(reactor.getCurrentHeat() <= 32, "Coolant starvation: tick=" + h.getTick() + " cold="
                + own.getFluidInTank(0).getAmount() + " hot=" + own.getFluidInTank(1).getAmount() + " exchangerHot=" + amounts
                + " hull=" + reactor.getCurrentHeat());
            if (h.getTick() == 10000) {
                h.assertTrue(steam[0] > 600000 && steam[1] > 600000 && steam[2] > 10000, "Boilers did not sustain steam production");
                h.succeed();
            }
        });
    }
}
