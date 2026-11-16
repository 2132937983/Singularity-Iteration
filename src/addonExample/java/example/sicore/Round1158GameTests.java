package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.8: the Particle Polymerizer's job: recipe values with the accelerator warm-up, chain polymerization (one input
 * to the chosen product, no intermediate item), neutron radiation and its lead shielding, and the recipes' process field.
 * Common classes are reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1158GameTests {
    private Round1158GameTests() {}

    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static final BlockPos AT = new BlockPos(3, 2, 3);

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)));
    }

    private static void setField(Object target, String name, Object value) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); f.set(target, value); return; }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
            throw new AssertionError("no field " + name);
        } catch (IllegalAccessException e) { throw new AssertionError(name, e); }
    }

    /** A polymerizer with {@code input} in its input slot and a full buffer, kept full. */
    private static BlockEntity machine(GameTestHelper h, ItemStack input) {
        h.setBlock(AT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_neutron_polymerizer")));
        BlockEntity m = h.getBlockEntity(AT);
        var handler = (net.neoforged.neoforge.items.IItemHandlerModifiable) call(m, "getItemHandler");
        int inputSlot = ((int[]) call(call(m, "getSlotLayout"), "getInputSlots"))[0];
        handler.setStackInSlot(inputSlot, input);
        h.onEachTick(() -> {
            var storage = call(m, "getEnergyStorageInternal");
            call(storage, "setStored", 2_304_000L);
        });
        return m;
    }

    private static boolean outputHas(BlockEntity m, net.minecraft.world.item.Item item) {
        var handler = (net.neoforged.neoforge.items.IItemHandler) call(m, "getItemHandler");
        for (int slot : (int[]) call(call(m, "getSlotLayout"), "getOutputSlots")) if (handler.getStackInSlot(slot).is(item)) return true;
        return false;
    }

    /** A cold machine draws 15 % of the recipe's 1536 EU/t and warms up while it works. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void polymerizerWarmsUp(GameTestHelper h) {
        BlockEntity m = machine(h, new ItemStack(Items.IRON_INGOT, 4));
        long cold = (long) call(m, "getEnergyPerTick");
        h.assertTrue(cold >= 220 && cold <= 240, "a cold start draws about 15 % of 1536 EU/t, got " + cold);
        h.runAfterDelay(40, () -> {
            float ramp = (float) call(m, "getRamp");
            h.assertTrue(ramp > 0.1F && ramp < 0.5F, "40 ticks of work warm the accelerator a little, ramp " + ramp);
            long warm = (long) call(m, "getEnergyPerTick");
            h.assertTrue(warm > cold, "the draw rises with the warm-up: " + cold + " -> " + warm);
            h.succeed();
        });
    }

    /** Chain to nano living metal: one iron ingot goes in, nano living metal comes out, no niobium-titanium in between. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void polymerizerRunsAChain(GameTestHelper h) {
        BlockEntity m = machine(h, new ItemStack(Items.IRON_INGOT, 2));
        h.assertTrue((boolean) call(m, "cycleTarget"), "the target button works");
        var target = call(m, "getTarget");
        h.assertTrue(target == item("resource/item_nano_living_metal").getItem(), "the first target is nano living metal, got " + target);
        setField(m, "ramp", 1F);
        h.runAfterDelay(3, () -> {
            setField(m, "jobProgress", 2995.0);                // two steps of 1500 ticks: almost done
            h.runAfterDelay(10, () -> {
                h.assertTrue(outputHas(m, item("resource/item_nano_living_metal").getItem()), "the chain made nano living metal");
                h.assertTrue(!outputHas(m, item("resource/item_niobium_titanium_ingot").getItem()), "no niobium-titanium left the machine");
                var handler = (net.neoforged.neoforge.items.IItemHandler) call(m, "getItemHandler");
                int inputSlot = ((int[]) call(call(m, "getSlotLayout"), "getInputSlots"))[0];
                h.assertTrue(handler.getStackInSlot(inputSlot).getCount() == 1, "one iron ingot was used");
                h.succeed();
            });
        });
    }

    /** Making neutron plate irradiates a pig next to the machine; six lead blocks around it stop that. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void polymerizerIrradiatesUnlessShielded(GameTestHelper h) {
        BlockEntity m = machine(h, item("resource/item_nano_living_metal").copyWithCount(4));
        var pig = h.spawn(EntityType.PIG, new BlockPos(5, 2, 3));
        var radiation = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("mio_icif", "radiation")).orElseThrow();
        h.runAfterDelay(30, () -> {
            h.assertTrue(pig.hasEffect(radiation), "an unshielded neutron-plate step irradiates the pig");
            h.assertTrue((int) call(m, "getRadiationRadius") == 6, "reach 6 without lead, got " + call(m, "getRadiationRadius"));
            pig.removeEffect(radiation);
            var lead = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "block_lead"));
            for (Direction d : Direction.values()) h.setBlock(AT.relative(d), lead);
            h.runAfterDelay(30, () -> {
                h.assertTrue((int) call(m, "getRadiationRadius") == 0, "six lead blocks shield it, reach " + call(m, "getRadiationRadius"));
                h.assertTrue(!pig.hasEffect(radiation), "the shielded machine leaves the pig alone");
                h.succeed();
            });
        });
    }

    /** The recipes carry their process: neutron plate radiates, its densities are a game setting. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void polymerizerRecipesCarryTheirProcess(GameTestHelper h) {
        Object recipe = h.getLevel().getRecipeManager()
            .byKey(ResourceLocation.fromNamespaceAndPath("mio_icif", "neutron_polymerizer/nano_living_metal_to_neutron_plate"))
            .map(holder -> (Object) holder.value()).orElseThrow();
        Object process = call(recipe, "getProcess");
        h.assertTrue((boolean) call(process, "radiation"), "neutron plate radiates");
        h.assertTrue(!(boolean) call(process, "real"), "neutron plate is an invented material");
        h.assertTrue((float) call(process, "densityTo") > 1e14F, "neutron plate is at neutron-star density");
        h.succeed();
    }
}
