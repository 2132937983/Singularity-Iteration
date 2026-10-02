package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.7.20: Electric Nano Blast Furnace works end to end, quantum blade sweeps and shows its
 * damage, electric lights are removable, tachyon tuning trades EU for damage, the life-support
 * ring's efficiency slider, and generator-mode reactor liquid cooling (no component wear).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round20GameTests {
    private Round20GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }

    @GameTest(batch = "round20", template = "empty", timeoutTicks = 200)
    public static void nanoBlastFurnaceCraftsPlacesAndRuns(GameTestHelper h) {
        var result = BuiltInRegistries.ITEM.get(id("producer/block_blast_furnace_elc"));
        boolean craftable = h.getLevel().getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
            .anyMatch(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(result));
        h.assertTrue(craftable, "Electric Nano Blast Furnace has a crafting recipe");
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, block("producer/block_blast_furnace_elc"));
        var furnace = (AbstractEnergyBlockEntity) h.getBlockEntity(pos);
        h.assertTrue(furnace != null, "Block entity created");
        var items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), null);
        h.assertTrue(items != null, "Item handler exposed");
        // input slot: first slot that accepts iron
        boolean inserted = false;
        for (int s = 0; s < items.getSlots() && !inserted; s++) inserted = items.insertItem(s, new ItemStack(Items.IRON_INGOT, 2), false).isEmpty();
        h.assertTrue(inserted, "Iron ingots accepted");
        h.onEachTick(() -> furnace.getEnergyStorageInternal().setStored(furnace.getEnergyStorageInternal().getCapacity()));
        var adviron = BuiltInRegistries.ITEM.get(id("resource/item_adviron_ingot"));
        h.succeedWhen(() -> {
            boolean made = false;
            for (int s = 0; s < items.getSlots(); s++) made |= items.getStackInSlot(s).is(adviron);
            h.assertTrue(made, "Nano blast furnace did not produce advanced iron");
        });
    }

    @GameTest(batch = "round20", template = "empty")
    public static void quantumBladeSweepsAndShowsDamage(GameTestHelper h) {
        ItemStack sword = item("item_tool_quantum_sword");
        var mods = sword.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        double[] damage = {0};
        mods.forEach(EquipmentSlotGroup.MAINHAND, (attr, mod) -> { if (attr.value() == Attributes.ATTACK_DAMAGE.value()) damage[0] += mod.amount(); });
        h.assertTrue(damage[0] == 24.0, "Attack Damage modifier shown in tooltip (24 + 1 base), got " + damage[0]);
        h.assertTrue(sword.canPerformAction(ItemAbilities.SWORD_SWEEP), "Quantum blade performs sweeping attacks");
        h.succeed();
    }

    @GameTest(batch = "round20", template = "empty")
    public static void electricLightsAreRemovable(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var light = block("block_electric_light");
        var pos = new BlockPos(1, 2, 1);
        h.setBlock(pos, light);
        h.assertTrue(h.getBlockState(pos).canBeReplaced(), "Light is replaceable by placing blocks");
        h.assertTrue(h.getBlockState(pos).getShape(h.getLevel(), h.absolutePos(pos), CollisionContext.empty()).isEmpty(),
            "Light has no outline unless the generator is held");
        ItemStack generator = item("item_electric_lighter");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, generator);
        h.assertTrue(!h.getBlockState(pos).getShape(h.getLevel(), h.absolutePos(pos), CollisionContext.of(player)).isEmpty(),
            "Holding the generator makes lights targetable");
        h.setBlock(pos.east(2), light);
        player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)));
        int removed = (int) call(generator.getItem().getClass(), "clearArea", h.getLevel(), player);
        h.assertTrue(removed == 2, "Shift-use clears nearby lights, removed " + removed);
        h.assertBlockNotPresent(light, pos);
        h.succeed();
    }

    private static ItemStack tuned(String path, String key, int value) {
        ItemStack stack = item(path);
        CompoundTag root = new CompoundTag(), tag = new CompoundTag();
        tag.putInt(key, value);
        root.put("SiTuning", tag);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    @GameTest(batch = "round20", template = "empty")
    public static void tachyonTuningTradesEnergyForPower(GameTestHelper h) {
        ItemStack base = item("item_tool_tachyon_disruptor");
        long baseCost = (long) call(base.getItem(), "shotCost", base);
        h.assertTrue(baseCost == 50_000, "Baseline (100%) keeps the stock 50k EU shot, got " + baseCost);
        ItemStack out = tuned("item_tool_tachyon_disruptor", "output", 200);
        h.assertTrue((float) call(out.getItem(), "damage", out) == 240F, "200% output doubles damage");
        h.assertTrue((long) call(out.getItem(), "shotCost", out) == 200_000, "...for 4x the EU");
        ItemStack multi = tuned("item_tool_tachyon_disruptor", "multi", 4);
        h.assertTrue((long) call(multi.getItem(), "shotCost", multi) > 400_000, "Multi-hit costs dramatically more");
        ItemStack clamp = tuned("item_tool_tachyon_disruptor", "true_damage", 999);
        h.assertTrue((long) call(clamp.getItem(), "shotCost", clamp) == 150_000, "Values are clamped (100% true = 3x)");
        var type = h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, id("tachyon_true")));
        h.assertTrue(type.is(DamageTypeTags.BYPASSES_ARMOR) && type.is(DamageTypeTags.BYPASSES_RESISTANCE)
            && type.is(DamageTypeTags.BYPASSES_ENCHANTMENTS), "True damage ignores armor, resistance and protection");
        h.succeed();
    }

    @GameTest(batch = "round20", template = "empty", timeoutTicks = 60)
    public static void reactorLiquidCoolingStopsComponentWear(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, block("generator/block_nuclear_reactor_generator"));
        IReactorController reactor = (IReactorController) h.getBlockEntity(pos);
        h.assertTrue(reactor.getItemHandler().insertItem(10, item("reactor/item_reactor_uranium_simple"), false).isEmpty(), "Fuel");
        h.assertTrue(reactor.getItemHandler().insertItem(11, item("reactor/item_reactor_collant_simple"), false).isEmpty(), "Coolant cell");
        IFluidHandler tank = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null);
        h.assertTrue(tank != null, "Generator-mode reactor accepts coolant");
        var coolant = BuiltInRegistries.FLUID.get(id("coolant"));
        h.assertTrue(tank.fill(new FluidStack(coolant, 4000), IFluidHandler.FluidAction.EXECUTE) == 4000, "Coolant fills the cooling tank");
        h.assertTrue(tank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
            "Lava is not a coolant");
        h.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(45, () -> {
            ItemStack cell = reactor.getItemHandler().getStackInSlot(11);
            int stored = ((CompoundTag) cell.save(h.getLevel().registryAccess())).getCompound("components")
                .getCompound("mio_icif:reactor_component_data").getInt("stored_value");
            h.assertTrue(stored == 0, "Coolant drains component heat (no wear), stored " + stored);
            h.assertTrue(reactor.getCurrentHeat() == 0, "Hull stays cold, heat " + reactor.getCurrentHeat());
            int left = tank.getFluidInTank(0).getAmount(), hot = tank.getFluidInTank(1).getAmount();
            h.assertTrue(left < 4000 && hot > 0 && left + hot == 4000, "Coolant circulates to hot coolant: " + left + " / " + hot);
            h.assertTrue(reactor.getCurrentEnergyGeneration() > 0, "Reactor still generates");
            h.succeed();
        });
    }

    @GameTest(batch = "round20", template = "empty")
    public static void coolantTableCoversForeignFluids(GameTestHelper h) {
        Class<?> cooling = null;
        try {
            cooling = Class.forName("com.miophas.singularity_iteration" + ".common.blockentity.generator.ReactorLiquidCooling");
        } catch (ClassNotFoundException e) { throw new AssertionError(e); }
        var water = call(cooling, "coolant", net.minecraft.world.level.material.Fluids.WATER);
        var lava = call(cooling, "coolant", net.minecraft.world.level.material.Fluids.LAVA);
        var si = call(cooling, "coolant", BuiltInRegistries.FLUID.get(id("coolant")));
        h.assertTrue(water != null && lava == null && si != null, "Water and SI coolant are coolants, lava is not");
        h.assertTrue((int) call(si, "huPerMB") > (int) call(water, "huPerMB"), "Coolant carries more heat per mB than water");
        h.succeed();
    }
}
