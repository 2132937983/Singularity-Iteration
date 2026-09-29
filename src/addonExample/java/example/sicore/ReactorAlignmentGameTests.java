package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

/** Exercises built-in content using public core interfaces, registry IDs and save codecs. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ReactorAlignmentGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack item(String suffix) { return new ItemStack(BuiltInRegistries.ITEM.get(id("reactor/item_reactor_" + suffix))); }

    private static ItemStack thermal(GameTestHelper helper, String suffix, int stored, int capacity) {
        CompoundTag tag = (CompoundTag) item(suffix).save(helper.getLevel().registryAccess());
        CompoundTag components = tag.getCompound("components");
        CompoundTag data = new CompoundTag();
        data.putInt("stored_value", stored); data.putInt("max_value", capacity);
        components.put("mio_icif:reactor_component_data", data); tag.put("components", components);
        return ItemStack.parse(helper.getLevel().registryAccess(), tag).orElseThrow();
    }

    private static int stored(GameTestHelper helper, ItemStack stack) {
        return ((CompoundTag) stack.save(helper.getLevel().registryAccess())).getCompound("components")
            .getCompound("mio_icif:reactor_component_data").getInt("stored_value");
    }

    private static int fuelRemaining(GameTestHelper helper, ItemStack stack) {
        return ((CompoundTag) stack.save(helper.getLevel().registryAccess())).getCompound("components")
            .getCompound("mio_icif:fuel_rod_durability").getInt("remaining_uses");
    }

    private static void insert(GameTestHelper helper, IReactorController reactor, int slot, ItemStack stack) {
        helper.assertTrue(reactor.getItemHandler().insertItem(slot, stack, false).isEmpty(), "Rejected reactor slot " + slot);
    }

    @GameTest(batch = "reactor_alignment", template = "empty", timeoutTicks = 40)
    public static void builtinReactorDistributesHeatInIc2Order(GameTestHelper helper) {
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, block("generator/block_nuclear_reactor_generator"));
        IReactorController reactor = (IReactorController) helper.getBlockEntity(pos);
        insert(helper, reactor, 10, item("uranium_simple"));
        for (int slot : new int[] {9, 1, 19}) insert(helper, reactor, slot, thermal(helper, "collant_simple", 0, 10000));
        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(stored(helper, reactor.getItemHandler().getStackInSlot(9)) == 1, "Left cell heat");
            helper.assertTrue(stored(helper, reactor.getItemHandler().getStackInSlot(1)) == 1, "Upper cell heat");
            helper.assertTrue(stored(helper, reactor.getItemHandler().getStackInSlot(19)) == 2, "Lower cell must receive remainder");
            helper.assertTrue(fuelRemaining(helper, reactor.getItemHandler().getStackInSlot(10)) == 19999, "Exactly one fuel cycle");
            helper.assertTrue(reactor.getCurrentEnergyGeneration() == 5 && reactor.getCurrentHeat() == 0, "Unexpected reactor output");
            helper.succeed();
        });
    }

    @GameTest(batch = "reactor_alignment", template = "empty", timeoutTicks = 40)
    public static void builtinMoxUsesPostThermalHeatAndPlating(GameTestHelper helper) {
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, block("generator/block_nuclear_reactor_generator"));
        BlockEntity entity = helper.getBlockEntity(pos);
        var tag = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putLong("HeatStored", 5000);
        entity.loadWithComponents(tag, helper.getLevel().registryAccess());
        IReactorController reactor = (IReactorController) entity;
        insert(helper, reactor, 0, item("mox_simple"));
        insert(helper, reactor, 47, item("heat_plate"));
        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(reactor.getCurrentHeat() == 5004 && reactor.getMaxHeat() == 12000, "Wrong thermal/plating state");
            helper.assertTrue(reactor.getCurrentEnergyGeneration() == 13, "MOX output must include plated capacity");
            helper.assertTrue(fuelRemaining(helper, reactor.getItemHandler().getStackInSlot(0)) == 9999, "MOX cycle was not committed");
            helper.succeed();
        });
    }

    @GameTest(batch = "reactor_alignment", template = "reactor", timeoutTicks = 60)
    public static void coolantCannotConvertIntoLeftoverHotWater(GameTestHelper helper) { mixedFluid(helper, false); }

    @GameTest(batch = "reactor_alignment", template = "reactor", timeoutTicks = 60)
    public static void waterCannotConvertIntoLeftoverHotCoolant(GameTestHelper helper) { mixedFluid(helper, true); }

    private static void mixedFluid(GameTestHelper helper, boolean water) {
        var pos = new BlockPos(3, 3, 3);
        helper.setBlock(pos, block("generator/block_nuclear_reactor_generator"));
        for (var side : Direction.values()) helper.setBlock(pos.relative(side), block("reactor/block_reactor_chamber"));
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) == 2)
                helper.setBlock(pos.offset(x, y, z), block("reactor/block_reactor_vessel"));
        }
        helper.setBlock(pos.east(2), block("reactor/block_reactor_redstone_port"));
        helper.setBlock(pos.east(3), Blocks.REDSTONE_BLOCK);
        BlockEntity entity = helper.getBlockEntity(pos);
        var cold = water ? Fluids.WATER : BuiltInRegistries.FLUID.get(id("coolant"));
        var wrongHot = BuiltInRegistries.FLUID.get(id(water ? "hotcoolant" : "hotwater"));
        var correctHot = BuiltInRegistries.FLUID.get(id(water ? "hotwater" : "hotcoolant"));
        var input = new FluidTank(10000); input.fill(new FluidStack(cold, 2000), EXECUTE);
        var output = new FluidTank(10000); output.fill(new FluidStack(wrongHot, 10), EXECUTE);
        var tag = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
        var fluids = tag.getCompound("FluidHandler");
        fluids.put("InputTank", input.writeToNBT(helper.getLevel().registryAccess(), new CompoundTag()));
        fluids.put("OutputTank", output.writeToNBT(helper.getLevel().registryAccess(), new CompoundTag()));
        tag.put("FluidHandler", fluids);
        entity.loadWithComponents(tag, helper.getLevel().registryAccess());
        IReactorController reactor = (IReactorController) entity;
        insert(helper, reactor, 0, item("uranium_simple"));
        insert(helper, reactor, 1, thermal(helper, "golden_vent", 100, 1000));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(reactor.isValidFluidReactorStructure(), "Fluid reactor did not form");
            var handler = reactor.getFluidHandler();
            helper.assertTrue(handler.getFluidInTank(0).getAmount() == 2000, "Wrong coolant was consumed");
            helper.assertTrue(handler.getFluidInTank(1).getAmount() == 10 && handler.getFluidInTank(1).getFluid() == wrongHot, "Hot tank changed despite incompatibility");
            helper.assertTrue(reactor.getCurrentHeat() == 20, "Unconverted heat must return to hull");
            helper.assertTrue(fuelRemaining(helper, reactor.getItemHandler().getStackInSlot(0)) == 19999, "Blocked conversion must not freeze fuel");
            helper.assertTrue(handler.drain(10, EXECUTE).getAmount() == 10, "Could not drain incompatible output");
        });
        helper.runAfterDelay(45, () -> {
            var handler = reactor.getFluidHandler();
            int expected = water ? 800 : 40;
            helper.assertTrue(handler.getFluidInTank(1).getFluid() == correctHot && handler.getFluidInTank(1).getAmount() == expected, "Conversion did not resume with correct fluid/rate");
            helper.assertTrue(handler.getFluidInTank(0).getAmount() == 2000 - expected, "Resumed conversion lost fluid");
            helper.assertTrue(reactor.getCurrentHeat() == 0, "Resumed cooling did not cool hull");
            helper.succeed();
        });
    }

    private static ItemStack craft(GameTestHelper helper, ItemStack... stacks) {
        var input = CraftingInput.of(stacks.length, 1, List.of(stacks));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        return recipe.map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }

    @GameTest(batch = "reactor_alignment", template = "empty")
    public static void loadedCondensatorRecipeUsesOneFillerPerSlot(GameTestHelper helper) {
        var lapis = thermal(helper, "condensator_lap", 95000, 100000);
        var redstone = new ItemStack(Items.REDSTONE, 64);
        var repaired = craft(helper, lapis, redstone);
        helper.assertTrue(!repaired.isEmpty() && stored(helper, repaired) == 90000, "One redstone slot must repair 5000 even when stacked");
        helper.assertTrue(stored(helper, lapis) == 95000 && redstone.getCount() == 64, "Recipe preview mutated ingredients");
        helper.assertTrue(stored(helper, craft(helper, lapis, redstone, redstone.copy())) == 85000, "Two filler slots must repair 10000");
        helper.assertTrue(stored(helper, craft(helper, lapis, new ItemStack(Items.LAPIS_LAZULI, 64))) == 55000, "Lapis repairs 40000 per slot");
        var rsh = thermal(helper, "condensator", 19000, 20000);
        helper.assertTrue(stored(helper, craft(helper, rsh, redstone)) == 9000, "RSH redstone repair must remain 10000");
        helper.assertTrue(craft(helper, rsh, new ItemStack(Items.LAPIS_LAZULI)).isEmpty(), "RSH must reject lapis");
        helper.assertTrue(craft(helper, lapis, redstone, new ItemStack(Items.LAPIS_LAZULI)).isEmpty(), "IC2 gradual recipes do not mix fillers");
        helper.assertTrue(craft(helper, lapis, lapis.copy(), redstone).isEmpty(), "Multiple condensators must be rejected");
        helper.succeed();
    }
}
