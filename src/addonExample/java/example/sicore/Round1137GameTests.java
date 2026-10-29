package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import com.miophas.singularity_iteration.core.api.reactor.ReactorTransformation;
import com.miophas.singularity_iteration.core.api.reactor.ReactorTransformationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.37. A tritium rod that finishes its decay leaves an empty fuel rod and the reactor keeps running (before,
 * the cycle that decayed it threw: the rod stayed at 9999 and the reactor stopped); the component changes of the JEI category
 * "Nuclear Fuel Breeding & Decay" come from the reactor's tables and an addon's entry with missing items is left out.
 * Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1137GameTests {
    private Round1137GameTests() {}

    private static final BlockPos REACTOR = new BlockPos(2, 1, 2);

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }

    /** A reactor component holding {@code stored} of {@code max}. */
    private static ItemStack component(GameTestHelper h, String path, int stored, int max) {
        CompoundTag tag = (CompoundTag) item(path).save(h.getLevel().registryAccess());
        CompoundTag components = tag.getCompound("components");
        CompoundTag data = new CompoundTag();
        data.putInt("stored_value", stored); data.putInt("max_value", max);
        components.put("mio_icif:reactor_component_data", data); tag.put("components", components);
        return ItemStack.parse(h.getLevel().registryAccess(), tag).orElseThrow();
    }

    private static int fuelRemaining(GameTestHelper h, ItemStack stack) {
        return ((CompoundTag) stack.save(h.getLevel().registryAccess())).getCompound("components")
            .getCompound("mio_icif:fuel_rod_durability").getInt("remaining_uses");
    }

    /**
     * A uranium rod next to a tritium rod one cycle short of its 10,000 (the reported bug): the next cycle commits, the tritium
     * rod becomes an empty fuel rod, which is thrown out at the cycle after; the reactor runs on.
     */
    @GameTest(batch = "round1137_tritium", template = "empty", timeoutTicks = 80)
    public static void decayedTritiumRodLeavesAnEmptyFuelRodAndTheReactorKeepsRunning(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        BlockEntity entity = h.getBlockEntity(REACTOR);
        IReactorController reactor = (IReactorController) entity;
        h.assertTrue(reactor.getItemHandler().insertItem(10, item("reactor/item_reactor_uranium_simple"), false).isEmpty(), "Fuel rejected");
        h.assertTrue(reactor.getItemHandler().insertItem(11, component(h, "normal/item_tritium_cell", 9999, 10000), false).isEmpty(),
            "Tritium rod rejected");
        h.setBlock(REACTOR.east(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(25, () -> {
            var slot = reactor.getItemHandler().getStackInSlot(11);
            h.assertTrue(slot.is(BuiltInRegistries.ITEM.get(id("normal/item_fuel_rod"))), "The decayed tritium rod did not become an empty fuel rod: " + slot);
            h.assertTrue(fuelRemaining(h, reactor.getItemHandler().getStackInSlot(10)) == 19999, "The decay cycle did not commit");
            h.runAfterDelay(20, () -> {
                h.assertTrue(reactor.getItemHandler().getStackInSlot(11).isEmpty(), "The empty fuel rod must leave the reactor");
                var near = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(REACTOR)).inflate(2),
                    e -> e.getItem().is(BuiltInRegistries.ITEM.get(id("normal/item_fuel_rod"))));
                h.assertTrue(!near.isEmpty(), "The empty fuel rod was not dropped next to the reactor");
                h.assertTrue(fuelRemaining(h, reactor.getItemHandler().getStackInSlot(10)) == 19998, "The reactor stopped after the decay");
                h.assertTrue(reactor.getCurrentEnergyGeneration() > 0, "The reactor makes no EU after the decay");
                h.succeed();
            });
        });
    }

    /**
     * SI's entries come from the reactor's tables: every fuel rod burns out into its depleted rod, the two breeders and the
     * tritium rod change into their products, and the reactor reads every input and output (an unreadable product is what
     * stopped the reactor).
     */
    @GameTest(batch = "round1137_registry", template = "empty", timeoutTicks = 20)
    public static void reactorTransformationsComeFromTheReactorTablesAndAreReadable(GameTestHelper h) {
        Class<?> inventory = Round33GameTests.common("reactor.ReactorInventory");
        int depletion = 0;
        for (ReactorTransformation change : ReactorTransformationRegistry.all()) {
            if (!change.id().getNamespace().equals("mio_icif")) continue;
            if (change.process() == ReactorTransformation.Process.DEPLETION) depletion++;
            for (ResourceLocation item : new ResourceLocation[]{change.input(), change.output()}) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(item), change.id() + ": no item " + item);
                Round33GameTests.call(inventory, "read", new ItemStack(BuiltInRegistries.ITEM.get(item)));   // throws if unreadable
            }
        }
        h.assertTrue(depletion == 9, "uranium, MOX and proton rods (3 sizes each) burn out: " + depletion);
        expect(h, "breeding/lithium_cell", ReactorTransformation.Process.BREEDING, "reactor/item_reactor_lithium_cell", "normal/item_tritium_cell", 10000);
        expect(h, "breeding/isotope_rod", ReactorTransformation.Process.BREEDING, "reactor/item_reactor_isotope_rod", "normal/item_re_enriched_uranium", 10000);
        expect(h, "decay/tritium_cell", ReactorTransformation.Process.DECAY, "normal/item_tritium_cell", "normal/item_fuel_rod", 10000);
        expect(h, "depletion/uranium_simple", ReactorTransformation.Process.DEPLETION, "reactor/item_reactor_uranium_simple",
            "reactor/item_reactor_uranium_simple_depleted", 20000);
        expect(h, "depletion/proton_quad", ReactorTransformation.Process.DEPLETION, "reactor/item_reactor_proton_quad",
            "reactor/item_reactor_proton_quad_depleted", 40000);
        h.assertTrue(ReactorTransformationRegistry.get(id("depletion/proton_quad")).notes().size() == 1, "the proton rod notes its EU and heat factors");
        h.assertTrue(ReactorTransformationRegistry.get(id("breeding/lithium_cell")).notes().size() == 3,
            "the lithium cell notes its gain, its heat and the rod beside it");
        h.succeed();
    }

    private static void expect(GameTestHelper h, String path, ReactorTransformation.Process process, String input, String output, int amount) {
        ReactorTransformation change = ReactorTransformationRegistry.get(id(path));
        h.assertTrue(change != null, "missing " + path);
        h.assertTrue(change.process() == process && change.input().equals(id(input)) && change.output().equals(id(output))
            && change.amount() == amount, path + ": " + change);
    }

    /**
     * An addon registers its own change; JEI shows only the entries whose items exist, so an addon item that is missing never
     * breaks the category. A second registration replaces the first; the namespace "minecraft" and a negative amount are refused.
     */
    @GameTest(batch = "round1137_registry", template = "empty", timeoutTicks = 20)
    public static void addonTransformationsAreListedAndMissingItemsAreLeftOut(GameTestHelper h) {
        var real = ResourceLocation.fromNamespaceAndPath("si_test", "plutonium_rod");
        ReactorTransformationRegistry.register(ReactorTransformation.of(real, ReactorTransformation.Process.DECAY,
            ResourceLocation.withDefaultNamespace("ender_pearl"), ResourceLocation.withDefaultNamespace("ender_eye"), 500,
            Component.literal("test")));
        var ghost = ResourceLocation.fromNamespaceAndPath("si_test", "ghost_rod");
        ReactorTransformationRegistry.register(ReactorTransformation.of(ghost, ReactorTransformation.Process.BREEDING,
            ResourceLocation.fromNamespaceAndPath("si_test_missing", "rod"), ResourceLocation.withDefaultNamespace("stone"), 10));
        h.assertTrue(ReactorTransformationRegistry.all().stream().anyMatch(t -> t.id().equals(ghost)), "the registry keeps every entry");
        h.assertTrue(ReactorTransformationRegistry.available().stream().anyMatch(t -> t.id().equals(real)), "an addon entry is listed");
        h.assertTrue(ReactorTransformationRegistry.available().stream().noneMatch(t -> t.id().equals(ghost)), "an entry with a missing item is left out");
        ReactorTransformationRegistry.register(ReactorTransformation.of(real, ReactorTransformation.Process.DECAY,
            ResourceLocation.withDefaultNamespace("ender_pearl"), ResourceLocation.withDefaultNamespace("ender_eye"), 600));
        h.assertTrue(ReactorTransformationRegistry.get(real).amount() == 600, "a second registration replaces the first");
        boolean refused = false;
        try {
            ReactorTransformationRegistry.register(ReactorTransformation.of(ResourceLocation.withDefaultNamespace("rod"),
                ReactorTransformation.Process.DECAY, ResourceLocation.withDefaultNamespace("stone"), ResourceLocation.withDefaultNamespace("stone"), 1));
        } catch (IllegalArgumentException expected) { refused = true; }
        h.assertTrue(refused, "the namespace minecraft must be refused");
        refused = false;
        try {
            ReactorTransformation.of(ghost, ReactorTransformation.Process.DECAY, ResourceLocation.withDefaultNamespace("stone"),
                ResourceLocation.withDefaultNamespace("stone"), -1);
        } catch (IllegalArgumentException expected) { refused = true; }
        h.assertTrue(refused, "a negative amount must be refused");
        h.succeed();
    }
}
