package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidationResult;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractTankBlockEntity;
import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import com.miophas.singularity_iteration.core.runtime.uu.UuScanResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private static final BlockPos LEFT = new BlockPos(1, 1, 1);
    private static final BlockPos RIGHT = new BlockPos(2, 1, 1);

    @GameTest(batch = "energy_pipelines", template = "empty", timeoutTicks = 300)
    public static void builtinGeneratorBatboxFurnacePipeline(GameTestHelper helper) {
        var generator = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:generator/block_thermal_generator"));
        var batbox = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:wiring/block_bat_box"));
        var furnace = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:producer/block_furnace_elc"));
        var target = new BlockPos(3, 1, 1);
        helper.setBlock(LEFT, generator);
        helper.setBlock(RIGHT, batbox.defaultBlockState().setValue(
            (DirectionProperty) batbox.getStateDefinition().getProperty("facing"), Direction.EAST));
        helper.setBlock(target, furnace);
        helper.runAfterDelay(5, () -> {
            AbstractGeneratorBlockEntity source = helper.getBlockEntity(LEFT);
            AbstractProcessingMachineBlockEntity machine = helper.getBlockEntity(target);
            helper.assertTrue(source.getItemHandler().insertItem(0, new ItemStack(Items.COAL), false).isEmpty(), "Generator rejected coal");
            helper.assertTrue(machine.getItemHandler().insertItem(0, new ItemStack(Items.IRON_ORE), false).isEmpty(), "Furnace rejected ore");
            helper.succeedWhen(() -> {
                var result = machine.getItemHandler().getStackInSlot(2);
                helper.assertTrue(result.is(Items.IRON_INGOT) && result.getCount() == 1, "Builtin generation/storage/processing pipeline failed");
                helper.assertTrue(machine.getItemHandler().getStackInSlot(0).isEmpty(), "Recipe did not consume input");
                helper.assertTrue(source.getItemHandler().getStackInSlot(0).isEmpty(), "Generator did not consume fuel");
            });
        });
    }

    @GameTest(batch = "energy_pipelines", template = "empty", timeoutTicks = 300)
    public static void addonFeedsBuiltinCableAndTransformer(GameTestHelper helper) {
        var sourcePos = new BlockPos(0, 1, 1);
        var cablePos = new BlockPos(1, 1, 1);
        var transformerPos = new BlockPos(2, 1, 1);
        var sinkPos = new BlockPos(3, 1, 1);
        var cable = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:wiring/cable/block_cable"));
        var transformer = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:wiring/transformer_lv_mv"));
        helper.assertTrue(cable != Blocks.AIR && transformer != Blocks.AIR, "Builtin registry IDs changed");
        var facing = (DirectionProperty) transformer.getStateDefinition().getProperty("facing");
        helper.setBlock(sourcePos, CoreExampleMod.SOURCE.get());
        helper.setBlock(cablePos, cable);
        helper.setBlock(transformerPos, transformer.defaultBlockState().setValue(facing, Direction.WEST));
        helper.setBlock(sinkPos, CoreExampleMod.SINK.get());
        helper.runAfterDelay(5, () -> {
            InheritedSource source = helper.getBlockEntity(sourcePos);
            InheritedSink sink = helper.getBlockEntity(sinkPos);
            source.generate(256);
            helper.succeedWhen(() -> {
                helper.assertTrue(sink.stored() > 0, "Addon did not transfer through builtin cable/transformer");
                helper.assertTrue(source.stored() + sink.stored() <= 256, "Cable/transformer created energy");
            });
        });
    }

    @GameTest(batch = "energy_pipelines", template = "empty", timeoutTicks = 200)
    public static void inheritedSourceFeedsComposedStorage(GameTestHelper helper) {
        helper.setBlock(LEFT, CoreExampleMod.SOURCE.get());
        helper.setBlock(RIGHT, CoreExampleMod.STORAGE.get());
        helper.runAfterDelay(5, () -> {
            InheritedSource source = helper.getBlockEntity(LEFT);
            ComposedStorage sink = helper.getBlockEntity(RIGHT);
            helper.assertTrue(source.generate(128) == 128, "Source generation failed");
            helper.succeedWhen(() -> {
                helper.assertTrue(sink.energyComponent().stored() == 128, "Different-namespace native input did not arrive");
                helper.assertTrue(source.stored() == 0, "Native transfer must debit the source once");
            });
        });
    }

    @GameTest(batch = "energy_pipelines", template = "empty", timeoutTicks = 200)
    public static void composedStorageFeedsInheritedSink(GameTestHelper helper) {
        helper.setBlock(LEFT, CoreExampleMod.STORAGE.get());
        helper.setBlock(RIGHT, CoreExampleMod.SINK.get());
        helper.runAfterDelay(5, () -> {
            ComposedStorage source = helper.getBlockEntity(LEFT);
            InheritedSink sink = helper.getBlockEntity(RIGHT);
            helper.assertTrue(source.energyComponent().generate(128, false) == 128, "Component generation failed");
            helper.succeedWhen(() -> {
                helper.assertTrue(sink.stored() == 128, "Composed native output did not arrive");
                helper.assertTrue(source.energyComponent().stored() == 0, "Composed output must debit the source once");
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void mixedInputSimulationPersistenceAndRemoval(GameTestHelper helper) {
        helper.setBlock(LEFT, CoreExampleMod.STORAGE.get());
        helper.runAfterDelay(5, () -> {
            ComposedStorage block = helper.getBlockEntity(LEFT);
            var energy = block.energyComponent();
            var at = helper.absolutePos(LEFT);
            var eu = helper.getLevel().getCapability(EUApi.SIDED, at, Direction.WEST);
            var fe = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, Direction.NORTH);
            helper.assertTrue(eu != null && fe != null, "Addon capabilities were not registered");
            helper.assertTrue(eu.receive(32, true) == 32 && fe.receiveEnergy(128, true) == 128, "Input simulation is incorrect");
            helper.assertTrue(energy.stored() == 0, "Simulation changed the balance");
            helper.assertTrue(eu.receive(16, false) == 16, "EU input failed");
            helper.assertTrue(fe.receiveEnergy(64, false) == 64, "FE input failed");
            helper.assertTrue(eu.receive(1, false) == 0 && fe.receiveEnergy(1, false) == 0, "Input budget bypass across interfaces/faces");
            helper.assertTrue(energy.stored() == 32, "Input conservation failed");
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(fe.receiveEnergy(1, false) == 1, "Next-tick input budget did not reset");
                var saved = new CompoundTag();
                energy.save(saved);
                helper.assertTrue(saved.getLong("scex_energy_fraction") > 0, "Fractional FE was lost");
                helper.setBlock(LEFT, Blocks.AIR);
                helper.assertTrue(eu.receive(1, false) == 0 && fe.receiveEnergy(1, false) == 0, "Removed owner's cached input remains active");
                helper.assertTrue(!eu.canReceive() && !fe.canReceive(), "Removed capability still advertises input");
                helper.setBlock(LEFT, CoreExampleMod.STORAGE.get());
                ComposedStorage restored = helper.getBlockEntity(LEFT);
                restored.energyComponent().load(saved);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(restored.energyComponent().stored() == 32, "Whole saved EU changed");
                    helper.assertTrue(restored.energyComponent().fePort(Direction.WEST).getEnergyStored() == 129, "Fractional saved FE changed");
                    helper.assertTrue(eu.receive(1, false) == 0, "Replacement revived the old owner's port");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void sharedOutputBudgetAndChangingFaces(GameTestHelper helper) {
        helper.setBlock(LEFT, CoreExampleMod.STORAGE.get());
        helper.runAfterDelay(5, () -> {
            ComposedStorage block = helper.getBlockEntity(LEFT);
            var energy = block.energyComponent();
            energy.generate(128, false);
            var eu = energy.euPort(Direction.EAST);
            var fe = energy.fePort(Direction.EAST);
            helper.assertTrue(energy.euPort(Direction.WEST).extract(1, false) == 0, "Wrong-side output was accepted");
            helper.assertTrue(eu.extract(32, true) == 32 && energy.stored() == 128, "Output simulation changed balance");
            helper.assertTrue(eu.extract(16, false) == 16 && fe.extractEnergy(64, false) == 64, "Mixed output failed");
            helper.assertTrue(eu.extract(1, false) == 0 && fe.extractEnergy(1, false) == 0, "Output budget bypass");
            helper.assertTrue(energy.stored() == 96, "Output conservation failed");
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(eu.extract(32, true) == 32, "Output budget did not reset");
                block.closePorts();
                helper.assertTrue(eu.extract(1, false) == 0 && fe.extractEnergy(4, false) == 0, "Cached ports ignored a face change");
                helper.assertTrue(!eu.canExtract() && !fe.canExtract(), "Closed port advertised output");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void inheritedCachedPortDiesWithOwner(GameTestHelper helper) {
        helper.setBlock(LEFT, CoreExampleMod.SOURCE.get());
        helper.runAfterDelay(5, () -> {
            InheritedSource source = helper.getBlockEntity(LEFT);
            source.generate(128);
            var port = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(LEFT), Direction.EAST);
            helper.assertTrue(port != null && port.extractEnergy(4, true) == 4, "Inherited FE port unavailable");
            helper.setBlock(LEFT, Blocks.AIR);
            helper.assertTrue(port.extractEnergy(4, false) == 0 && !port.canExtract(), "Inherited cached port survived removal");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void multiblockBuilderLifecycleCallbacks(GameTestHelper helper) {
        helper.setBlock(LEFT, Blocks.IRON_BLOCK);
        helper.setBlock(RIGHT, Blocks.IRON_BLOCK);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.IRON_BLOCK);
        boolean[] formed = {false};
        boolean[] broken = {false};
        IMultiblockValidator validator = new IMultiblockValidator() {
            @Override
            public IMultiblockValidationResult validate(Level level, BlockPos controllerPos) {
                List<BlockPos> blocks = List.of(controllerPos, controllerPos.east(), controllerPos.south());
                for (BlockPos pos : blocks) {
                    if (!level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
                        return IMultiblockValidationResult.failure("Gametest structure incomplete");
                    }
                }
                return IMultiblockValidationResult.success(new HashSet<>(blocks));
            }
        };
        IMultiblockStructure structure = IMultiblockStructure.builder()
            .validator(validator)
            .onFormed((level, pos) -> formed[0] = true)
            .onBroken((level, pos) -> broken[0] = true)
            .buildAndRegister(helper.getLevel(), helper.absolutePos(LEFT));
        helper.assertTrue(structure != null && structure.isValid(), "Builder did not form the structure");
        helper.assertTrue(formed[0], "onFormed callback was never executed");
        helper.assertTrue(IMultiblockStructure.find(helper.getLevel(), helper.absolutePos(LEFT)) == structure,
            "Formed structure is not discoverable at its controller");
        var blocks = structure.getStructureBlocks();
        helper.assertTrue(blocks.contains(helper.absolutePos(RIGHT)) && blocks.contains(helper.absolutePos(new BlockPos(1, 1, 2))),
            "Structure membership is incomplete");
        helper.assertTrue(structure.invalidate(helper.getLevel()), "invalidate did not dismantle the structure");
        helper.assertTrue(broken[0], "onBroken callback was never executed");
        helper.assertTrue(IMultiblockStructure.find(helper.getLevel(), helper.absolutePos(LEFT)) == null,
            "Invalidated structure is still active");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void addonCompressorRecipeExtension(GameTestHelper helper) {
        var api = MioIcifAPI.instance();
        var id = ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "gametest_compress_clay");
        helper.assertTrue(api.getRecipeRegistrationAPI().registerCompressorRecipe(
                id, Ingredient.of(Items.CLAY), new ItemStack(Items.BRICK), 100),
            "Addon compressor recipe registration failed");
        api.getRecipeRegistrationAPI().syncToRecipeManager();
        var found = api.getRecipeAPI().findCompressorRecipe(new ItemStack(Items.CLAY), helper.getLevel());
        helper.assertTrue(found.isPresent(), "Registered addon recipe is not findable");
        helper.assertTrue(found.get().id().equals(id), "Found recipe is not the registered addon recipe");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void builtinTankVerticalGroupSharesFluid(GameTestHelper helper) {
        var tank = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:build/block_iron_tank"));
        helper.assertTrue(tank != Blocks.AIR, "Builtin tank registry ID changed");
        var bottom = new BlockPos(1, 1, 1);
        var top = new BlockPos(1, 2, 1);
        helper.setBlock(bottom, tank);
        helper.setBlock(top, tank);
        helper.runAfterDelay(5, () -> {
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(bottom), null);
            helper.assertTrue(handler != null, "Tank fluid capability unavailable");
            int filled = handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 40000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 40000, "Two stacked tanks did not accept a full fill");
            AbstractTankBlockEntity<?> lower = helper.getBlockEntity(bottom);
            AbstractTankBlockEntity<?> upper = helper.getBlockEntity(top);
            helper.assertTrue(lower.getConnectedTankCount() == 2, "Vertical tank group was not connected");
            helper.assertTrue(handler.getTankCapacity(0) == 64000, "Group capacity ignored the second tank");
            helper.assertTrue(lower.getTotalFluidAmount() == 40000, "Group fluid amount is wrong");
            helper.assertTrue(lower.getFluidTank().getFluidAmount() <= 32000 && upper.getFluidTank().getFluidAmount() > 0,
                "Fluid was not distributed across the group");
            var drained = handler.drain(40000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(drained.getAmount() == 40000, "Group drain did not return the stored fluid");
            helper.assertTrue(lower.getTotalFluidAmount() == 0, "Group still holds fluid after a full drain");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fluidAutomationComponentBudgetAndPersistence(GameTestHelper helper) {
        var tank = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:build/block_iron_tank"));
        helper.assertTrue(tank != Blocks.AIR, "Builtin tank registry ID changed");
        var machine = new BlockPos(1, 1, 1);
        var neighbour = new BlockPos(2, 1, 1);
        helper.setBlock(machine, tank);
        helper.setBlock(neighbour, tank);
        helper.runAfterDelay(5, () -> {
            AbstractTankBlockEntity<?> source = helper.getBlockEntity(machine);
            AbstractTankBlockEntity<?> target = helper.getBlockEntity(neighbour);
            var water = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5000);
            helper.assertTrue(source.getFluidTank().fill(water,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 5000,
                "Source tank rejected the test fluid");
            var stats = com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.empty();
            var component = new com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent(
                new com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent.Host() {
                    @Override public Level level() { return helper.getLevel(); }
                    @Override public BlockPos worldPosition() { return helper.absolutePos(machine); }
                    @Override public net.neoforged.neoforge.fluids.capability.IFluidHandler ownFluidHandler() {
                        return source.getFluidHandlerCapability(null);
                    }
                    @Override public net.neoforged.neoforge.fluids.capability.IFluidHandler adjacentFluidHandler(BlockPos pos, Direction side) {
                        return pos.equals(helper.absolutePos(neighbour)) ? target.getFluidHandlerCapability(null) : null;
                    }
                    @Override public IMachineUpgradeStats upgradeStats() { return stats; }
                });
            component.ejectFluids(source.getFluidHandlerCapability(null), 1);
            helper.assertTrue(target.getTotalFluidAmount() == 50, "One fluid ejector upgrade did not move its IC2 50 mB allowance");
            helper.assertTrue(source.getTotalFluidAmount() == 4950, "Ejected fluid was not debited from the source");
            var saved = new CompoundTag();
            component.save(saved, helper.getLevel().registryAccess());
            helper.assertTrue(saved.contains("scex_fluid_output_pending") && saved.contains("scex_fluid_input_pending"),
                "Component did not keep the archived fluid buffer keys");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fluidPortGroupAndSidedRouting(GameTestHelper helper) {
        var water = new FluidTank(4000, stack -> stack.getFluid() == java.util.Objects.requireNonNull(
            net.minecraft.world.level.material.Fluids.WATER));
        var lava = new FluidTank(4000, stack -> stack.getFluid() == java.util.Objects.requireNonNull(
            net.minecraft.world.level.material.Fluids.LAVA));
        var group = new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup(List.of(
            new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Tank(water,
                com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Role.IO),
            new com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Tank(lava,
                com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup.Role.IO)));
        helper.assertTrue(group.getTanks() == 2, "Tank group did not expose both tanks");
        helper.assertTrue(group.getTankCapacity(0) == 4000 && group.getTankCapacity(1) == 4000, "Tank group capacities are wrong");
        int filled = group.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1500),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1500, "Tank group did not route water into the water tank");
        helper.assertTrue(lava.getFluidAmount() == 0, "Tank group filled a tank that rejects the fluid");
        helper.assertTrue(group.getFluidInTank(0).getAmount() == 1500, "Tank group reported the wrong content");
        var drained = group.drain(1500, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(drained.getAmount() == 1500, "Tank group drain order did not reach the filled tank");

        var onlyFill = new com.miophas.singularity_iteration.core.prefab.fluid.SidedFluidHandler(water, true, false);
        helper.assertTrue(onlyFill.fill(new net.neoforged.neoforge.fluids.FluidStack(
            net.minecraft.world.level.material.Fluids.WATER, 500),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 500, "Fill-only side rejected fluid");
        helper.assertTrue(onlyFill.drain(100, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).isEmpty(),
            "Fill-only side allowed draining");
        helper.assertTrue(onlyFill.getTankCapacity(0) == 4000, "Sided view did not keep the wrapped capacity");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void builderDeclaredFluidTanksAreReal(GameTestHelper helper) {
        var machinePos = new BlockPos(1, 1, 1);
        var plainPos = new BlockPos(3, 1, 1);
        helper.setBlock(machinePos, CoreExampleMod.FLUID_MACHINE.get());
        helper.setBlock(plainPos, CoreExampleMod.SINK.get());
        helper.runAfterDelay(5, () -> {
            FluidMachine machine = helper.getBlockEntity(machinePos);
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(machinePos), null);
            helper.assertTrue(handler != null, "Builder-declared tanks did not produce a fluid capability");
            helper.assertTrue(handler.getTanks() == 2, "Builder-declared tank count is wrong");
            helper.assertTrue(handler.getTankCapacity(0) == FluidMachine.TANK_CAPACITY, "Builder-declared tank capacity is wrong");
            int filled = handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 5000, "Builder-declared tank rejected fluid");
            var drained = handler.drain(5000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(drained.getAmount() == 5000, "Builder-declared tank did not return the fluid");
            helper.assertTrue(machine.fluidHandler(null) != null, "Public fluid accessor is unavailable");

            var plainHandler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(plainPos), null);
            helper.assertTrue(plainHandler == null, "A machine without declared tanks must not expose a fluid capability");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void uuQuoteBookStateLifecycle(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var index = new IndependentUuValueIndex(16);
        helper.assertTrue(index.register(new ItemStack(Items.CLAY), 1.5), "Finite UU price rejected");
        helper.assertTrue(!index.register(new ItemStack(Items.CLAY), -1), "Negative UU price accepted");
        var dirt = IndependentUuValueIndex.keyOf(new ItemStack(Items.DIRT));
        long generation = UuQuoteBook.install(server, index, Set.of(dirt), Set.of(dirt));
        try {
            helper.assertTrue(generation > 0 && UuQuoteBook.generation(server) == generation,
                "Published generation is not observable");
            var quote = UuQuoteBook.quote(server, new ItemStack(Items.CLAY));
            helper.assertTrue(quote != null && quote.generation() == generation && Double.compare(quote.buckets(), 1.5) == 0,
                "Finite quote is wrong");
            helper.assertTrue(UuQuoteBook.classify(server, new ItemStack(Items.CLAY)).disposition() == UuQuoteBook.Disposition.FINITE,
                "Finite item misclassified");
            var denied = UuQuoteBook.classify(server, new ItemStack(Items.DIRT));
            helper.assertTrue(denied.disposition() == UuQuoteBook.Disposition.KNOWN_DENIED && denied.deniedScanEligible(),
                "Denied item misclassified");
            helper.assertTrue(UuQuoteBook.classify(server, new ItemStack(Items.DIAMOND)).disposition() == UuQuoteBook.Disposition.UNSUPPORTED,
                "Unknown item misclassified");
            var result = new UuScanResult(new ItemStack(Items.CLAY), 1.5, 100);
            helper.assertTrue(result.getUuMatterCostMB() == 1500, "Scan result MB conversion is wrong");
            try {
                new UuScanResult(ItemStack.EMPTY, 1.5, 100);
                helper.fail("Invalid scan result was accepted");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        } finally {
            UuQuoteBook.remove(server);
        }
        helper.assertTrue(UuQuoteBook.generation(server) == -1 && UuQuoteBook.quote(server, new ItemStack(Items.CLAY)) == null,
            "Server-bound UU state was not released");
        helper.succeed();
    }
}
