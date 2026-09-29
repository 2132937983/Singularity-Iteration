package example.sicore;

import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent;
import com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup;
import com.miophas.singularity_iteration.core.runtime.reactor.FluidReactorCycle;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CoolantLoopGameTests {
    @GameTest(template = "empty", timeoutTicks = 650)
    public static void builtinExchangersReturnCoolantAtRatedHeatOutput(GameTestHelper helper) {
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
            net.minecraft.resources.ResourceLocation.parse("mio_icif:hugenerator/block_heat_source_fluid"));
        var conductor = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            net.minecraft.resources.ResourceLocation.parse("mio_icif:resource/item_heatconductor"));
        var coldFluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
            net.minecraft.resources.ResourceLocation.parse("mio_icif:coolant"));
        var hotFluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
            net.minecraft.resources.ResourceLocation.parse("mio_icif:hotcoolant"));
        helper.assertTrue(coldFluid != Fluids.EMPTY && hotFluid != Fluids.EMPTY, "Missing coolant registry entries");
        var positions = new BlockPos[] { new BlockPos(1, 1, 1), new BlockPos(3, 1, 1) };
        for (var pos : positions) helper.setBlock(pos, block);
        {
            var machines = new com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity[2];
            var handlers = new IFluidHandler[2];
            for (int i = 0; i < 2; i++) {
                machines[i] = helper.getBlockEntity(positions[i]);
                for (int slot = 4; slot < 14; slot++) helper.assertTrue(machines[i].getItemHandler().insertItem(slot,
                    new net.minecraft.world.item.ItemStack(conductor), false).isEmpty(), "Heat conductor rejected at slot " + slot
                        + ", slots=" + machines[i].getItemHandler().getSlots() + ", item=" + conductor
                        + ", content=" + machines[i].getItemHandler().getStackInSlot(slot));
                handlers[i] = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(positions[i]), Direction.NORTH);
                helper.assertTrue(handlers[i] != null, "Missing exchanger fluid port");
            }
            var cold = new FluidTank(10000, stack -> stack.getFluid() == coldFluid);
            var hot = new FluidTank(10000, stack -> stack.getFluid() == hotFluid);
            cold.fill(new FluidStack(coldFluid, 400), EXECUTE);
            var reactor = FluidTankGroup.inputOutput(cold, hot);
            var supply = component(helper, reactor, stats(1, 0), side -> switch (side) {
                case DOWN -> handlers[0]; case UP -> handlers[1]; default -> null;
            });
            var returns = new FluidAutomationComponent[] {
                component(helper, handlers[0], stats(1, 0), side -> side == Direction.DOWN ? reactor : null),
                component(helper, handlers[1], stats(1, 0), side -> side == Direction.DOWN ? reactor : null)
            };
            long[] heat = new long[2];
            for (int step = 0; step < 600; step++) {
                final int tick = step;
                helper.runAfterDelay(step + 6, () -> {
                    if (tick % 20 == 0) {
                        var conversion = FluidReactorCycle.convert(100, cold.getFluidAmount(), hot.getFluidAmount(), 10000);
                        helper.assertTrue(conversion.returnedHeat() == 0, "Real exchanger loop starved at tick " + tick);
                        cold.drain(conversion.millibuckets(), EXECUTE);
                        hot.fill(new FluidStack(hotFluid, conversion.millibuckets()), EXECUTE);
                    }
                    supply.runAutomation();
                    for (int i = 0; i < 2; i++) {
                        // A full-load heat consumer: leave fluid conversion and tank roles to the real BE ticker.
                        heat[i] += machines[i].extractHeat(100, false);
                        returns[i].runAutomation();
                    }
                    int total = cold.getFluidAmount() + hot.getFluidAmount();
                    for (var handler : handlers) for (int i = 0; i < handler.getTanks(); i++) total += handler.getFluidInTank(i).getAmount();
                    helper.assertTrue(total == 400, "Real exchanger loop lost coolant");
                    if (tick == 599) {
                        helper.assertTrue(heat[0] >= 59000 && heat[1] >= 59000,
                            "Exchangers did not sustain rated output: " + heat[0] + ", " + heat[1]);
                        helper.assertTrue(cold.getFluidAmount() >= 380, "Real exchanger loop accumulated hot coolant");
                        helper.succeed();
                    }
                });
            }
        }
    }

    private static MachineUpgradeStats stats(int ejectors, int pullers) {
        return new MachineUpgradeStats(0, 0, 0, 0, 0, ejectors, pullers, false,
            List.of(), List.of(), List.of(), List.of());
    }

    @GameTest(template = "empty")
    public static void fluidUpgradeStacksKeepTheirOwnDirectionsAndRates(GameTestHelper helper) {
        int[] expected = {0, 50, 200, 800, 3200, 12800, 12800};
        for (int count = 0; count < expected.length; count++) {
            helper.assertTrue(IMachineUpgradeStats.fluidTransferPerStack(count) == expected[count], "Wrong stack multiplier");
        }
        var upgrades = List.of(new MachineUpgradeStats.DirectionalUpgrade(Direction.NORTH, 2),
            new MachineUpgradeStats.DirectionalUpgrade(Direction.SOUTH, 1),
            new MachineUpgradeStats.DirectionalUpgrade(null, 1));
        var stats = new MachineUpgradeStats(0, 0, 0, 0, 0, 4, 4, false, List.of(), List.of(), upgrades, upgrades);
        var source = new FluidTank(10000);
        source.fill(new FluidStack(Fluids.WATER, 10000), EXECUTE);
        var targets = new FluidTank[6];
        for (int i = 0; i < 6; i++) targets[i] = new FluidTank(2000);
        component(helper, source, stats, side -> targets[side.ordinal()]).ejectFluids(source, 4);
        for (Direction side : Direction.values()) {
            int allowance = side == Direction.NORTH ? 250 : side == Direction.SOUTH ? 100 : 50;
            helper.assertTrue(targets[side.ordinal()].getFluidAmount() == allowance, "Upgrade stack direction leaked: " + side);
        }
        var destination = new FluidTank(10000);
        component(helper, destination, stats, side -> targets[side.ordinal()]).pullFluids(destination, 4);
        helper.assertTrue(destination.getFluidAmount() == 550, "Pulling does not use per-stack, per-face allowances");
        for (var target : targets) helper.assertTrue(target.isEmpty(), "Pulling starved a neighbouring source");
        helper.assertTrue(source.getFluidAmount() + destination.getFluidAmount() == 10000, "Transfers did not conserve fluid");
        helper.succeed();
    }

    private static FluidAutomationComponent component(GameTestHelper helper, IFluidHandler own,
            IMachineUpgradeStats stats, java.util.function.Function<Direction, IFluidHandler> adjacent) {
        return new FluidAutomationComponent(new FluidAutomationComponent.Host() {
            public Level level() { return helper.getLevel(); }
            public BlockPos worldPosition() { return BlockPos.ZERO; }
            public IFluidHandler ownFluidHandler() { return own; }
            public IFluidHandler adjacentFluidHandler(BlockPos pos, Direction side) {
                return adjacent.apply(side.getOpposite());
            }
            public IMachineUpgradeStats upgradeStats() { return stats; }
        });
    }

    @GameTest(template = "empty")
    public static void everyAdjacentExchangerReceivesItsOwnUpgradeBudget(GameTestHelper helper) {
        var source = new FluidTank(10000);
        source.fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
        var targets = new FluidTank[6];
        for (int i = 0; i < targets.length; i++) targets[i] = new FluidTank(2000);
        component(helper, source, stats(1, 0), side -> targets[side.ordinal()]).runAutomation();
        for (var target : targets) helper.assertTrue(target.getFluidAmount() == 50,
            "IC2 single fluid upgrade must deliver 50 mB to EACH neighbour; got " + target.getFluidAmount());
        helper.assertTrue(source.getFluidAmount() == 700, "Fluid was lost or duplicated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void closedCoolantLoopDoesNotStarveParallelExchangers(GameTestHelper helper) {
        // Substitute water/lava for cold/hot fluid: the generic automation only sees tank roles.
        // IC2: 100 emitted heat / second -> 200 mB hot coolant / second;
        // two 100 HU/t exchangers consume 5 mB/t each, with only 400 mB in the whole loop.
        var cold = new FluidTank(10000);
        var hot = new FluidTank(10000);
        cold.fill(new FluidStack(Fluids.WATER, 400), EXECUTE);
        var reactor = FluidTankGroup.inputOutput(cold, hot);
        var inputs = new FluidTank[] { new FluidTank(2000), new FluidTank(2000) };
        var outputs = new FluidTank[] { new FluidTank(2000), new FluidTank(2000) };
        var exchangers = new IFluidHandler[] { FluidTankGroup.inputOutput(inputs[0], outputs[0]),
            FluidTankGroup.inputOutput(inputs[1], outputs[1]) };
        var supply = component(helper, reactor, stats(1, 0), side -> switch (side) {
            case DOWN -> exchangers[0]; case UP -> exchangers[1]; default -> null;
        });
        var returns = new FluidAutomationComponent[] {
            component(helper, exchangers[0], stats(1, 0), side -> side == Direction.DOWN ? reactor : null),
            component(helper, exchangers[1], stats(1, 0), side -> side == Direction.DOWN ? reactor : null)
        };
        long[] recovered = new long[2];
        for (int tick = 0; tick < 6000; tick++) {
            if (tick % 20 == 0) {
                var conversion = FluidReactorCycle.convert(100, cold.getFluidAmount(), hot.getFluidAmount(), 10000);
                helper.assertTrue(conversion.returnedHeat() == 0, "Coolant starvation at tick " + tick);
                cold.drain(conversion.millibuckets(), EXECUTE);
                hot.fill(new FluidStack(Fluids.LAVA, conversion.millibuckets()), EXECUTE);
            }
            supply.runAutomation();
            for (int i = 0; i < 2; i++) {
                int cooled = Math.min(5, Math.min(inputs[i].getFluidAmount(), 2000 - outputs[i].getFluidAmount()));
                inputs[i].drain(cooled, EXECUTE);
                outputs[i].fill(new FluidStack(Fluids.WATER, cooled), EXECUTE);
                recovered[i] += (long) cooled * FluidReactorCycle.COOLANT_HU_PER_MB;
                returns[i].runAutomation();
            }
            int total = cold.getFluidAmount() + hot.getFluidAmount();
            for (int i = 0; i < 2; i++) total += inputs[i].getFluidAmount() + outputs[i].getFluidAmount();
            helper.assertTrue(total == 400, "Closed loop did not conserve its 400 mB charge");
        }
        helper.assertTrue(recovered[0] == 600000 && recovered[1] == 600000, "Unequal/insufficient heat recovery");
        helper.assertTrue(cold.getFluidAmount() == 400 && hot.isEmpty(), "Hot fluid accumulated");
        helper.succeed();
    }
}
