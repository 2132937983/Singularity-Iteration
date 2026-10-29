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

    // ------------------------------------------------------------------ nuclear transmutation at 0.02 %

    /**
     * A blast turns 0.02 % of the deepslate ores into iridium ore: about 200 of 1,000,000 positions. The decision is a fixed hash
     * of the blast and the position (a chunk resumed after a reload decides the same way); 1 turns every ore, 0 none. A new blast
     * takes the config value (0.0002 by default).
     */
    @GameTest(batch = "round1137_transmute", template = "empty", timeoutTicks = 20)
    public static void nuclearTransmutationTurnsTwoInTenThousandOres(GameTestHelper h) throws ReflectiveOperationException {
        var transmutes = Round33GameTests.common("blockentity.reactor.NuclearFalloutChunkTask")
            .getMethod("transmutes", long.class, int.class, int.class, int.class, double.class);
        long seed = 0x5EED_1137L;
        int hits = 0;
        for (int i = 0; i < 1_000_000; i++) {
            int x = i % 100, y = -64 + (i / 100) % 100, z = i / 10_000;
            if ((Boolean) transmutes.invoke(null, seed, x, y, z, 0.0002)) hits++;
        }
        h.assertTrue(hits >= 140 && hits <= 260, "0.02 % of 1,000,000 ores is about 200: " + hits);
        boolean first = (Boolean) transmutes.invoke(null, seed, 5, -20, 7, 0.5);
        for (int i = 0; i < 5; i++) h.assertTrue((Boolean) transmutes.invoke(null, seed, 5, -20, 7, 0.5) == first, "the same blast and block decide the same way");
        h.assertTrue((Boolean) transmutes.invoke(null, seed, 1, 2, 3, 1.0) && !(Boolean) transmutes.invoke(null, seed, 1, 2, 3, 0.0), "1 turns every ore, 0 none");

        var manager = Round33GameTests.common("blockentity.reactor.NuclearFalloutManager");
        java.util.UUID id = java.util.UUID.randomUUID();
        Round33GameTests.call(manager, "register", h.getLevel(), id, net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(REACTOR)), 3, 3);
        var region = com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState.get(h.getLevel()).regions.get(id);
        double chance = region.transmuteChance;
        Round33GameTests.call(manager, "remove", h.getLevel(), id);
        h.assertTrue(chance == 0.0002, "a new blast takes the config value 0.0002: " + chance);
        h.succeed();
    }

    // ------------------------------------------------------------------ the Steam Reactor in JEI

    private static Object steamReport(GameTestHelper h, String layout) {
        var plan = com.miophas.singularity_iteration.core.api.reactor.ReactorLayoutRegistry.get(id(layout));
        h.assertTrue(plan != null, "missing layout " + layout);
        Object result = Round33GameTests.call(Round33GameTests.call(Round33GameTests.common("reactor.ReactorLayoutAnalysis"), "of", plan), "result");
        h.assertTrue(result != null, layout + " was not simulated");
        h.assertTrue(Round33GameTests.call(result, "verdict").toString().equals("STABLE"), layout + " is not stable: " + Round33GameTests.call(result, "verdict"));
        return Round33GameTests.call(result, "report");
    }

    /**
     * The Steam Reactor layouts run as steam reactors: four Advanced Steam Vents on a dual rod make 960 mB/s (24 HU/s, 6 units of
     * 40 mB per vent), four Overclocked Steam Vents on a quad rod 3,840 mB/s; neither makes EU. An electric layout makes no steam.
     */
    @GameTest(batch = "round1137_steam_jei", template = "empty", timeoutTicks = 20)
    public static void steamLayoutsAreSimulatedAsSteamReactors(GameTestHelper h) {
        h.assertTrue(com.miophas.singularity_iteration.core.api.reactor.ReactorLayoutRegistry.get(id("steam_starter")).steam(), "steam_starter is a steam layout");
        Object starter = steamReport(h, "steam_starter");
        h.assertTrue((Long) Round33GameTests.call(starter, "steamPerCycle") == 960, "steam_starter: " + Round33GameTests.call(starter, "steamPerCycle") + " mB/s");
        h.assertTrue((Double) Round33GameTests.call(starter, "euPerTick") == 0, "a steam reactor makes no EU");
        Object quad = steamReport(h, "steam_quad");
        h.assertTrue((Long) Round33GameTests.call(quad, "steamPerCycle") == 3840, "steam_quad: " + Round33GameTests.call(quad, "steamPerCycle") + " mB/s");
        Object electric = steamReport(h, "dual_rod");
        h.assertTrue((Long) Round33GameTests.call(electric, "steamPerCycle") == 0 && Math.round((Double) Round33GameTests.call(electric, "euPerTick")) == 20,
            "an electric layout makes EU and no steam");
        h.succeed();
    }

    /**
     * The plan "Steam Reactor" in "Multiblock Structures": the reactor as controller, five steam chambers and a turbine against
     * the reactor. Built in the world from its placements, it works: the reactor has 3 + 5 columns.
     */
    @GameTest(batch = "round1137_steam_jei", template = "empty", timeoutTicks = 40)
    public static void steamReactorPlanBuildsAWorkingReactor(GameTestHelper h) {
        var plan = com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry.get(id("steam_reactor"));
        h.assertTrue(plan != null, "the Steam Reactor plan is not registered");
        h.assertTrue(plan.controller().blocks().get(0).block().equals(id("generator/block_steam_reactor")), "the controller is not the steam reactor");
        BlockPos base = new BlockPos(1, 1, 1);
        int chambers = 0, turbines = 0;
        // the controller first, as a player builds: a chamber placed without a reactor beside it drops at once
        h.setBlock(base.offset(plan.controllerPos()), plan.controller().blocks().get(0).resolve());
        for (var placement : plan.placements()) {
            if (placement.pos().equals(plan.controllerPos())) continue;
            var block = placement.key().blocks().get(0);
            if (block.block().equals(id("reactor/block_steam_reactor_chamber"))) chambers++;
            if (block.block().equals(id("generator/block_steam_turbine"))) turbines++;
            h.setBlock(base.offset(placement.pos()), block.resolve());
        }
        h.assertTrue(chambers == 5 && turbines == 1, "five chambers and one turbine: " + chambers + ", " + turbines);
        BlockPos reactor = base.offset(plan.controllerPos());
        h.runAfterDelay(2, () -> {
            int columns = ((IReactorController) h.getBlockEntity(reactor)).getAvailableColumns();
            h.assertTrue(columns == 8, "3 + 5 chambers = 8 columns: " + columns);
            h.succeed();
        });
    }
}
