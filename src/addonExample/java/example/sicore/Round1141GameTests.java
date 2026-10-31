package example.sicore;

import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry;
import java.util.List;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.41: Steam Turbine Rotors in the Steam Tunnel. Each rotor adds its tier's EU to every step of 1,024 mB of steam
 * (bronze +32, iron +64, refined iron +80, titanium +96, iridium +128, netherite +160); one power port carries the full output.
 * The tunnel is built from its JEI structure plan, as in {@code Round1139GameTests}; internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1141GameTests {
    private Round1141GameTests() {}

    /** The plan placed here fills x 0..4, y 1..3, z 1..3; the controller faces east, so the inside runs from x 3 (front) to x 1. */
    private static final BlockPos ORIGIN = new BlockPos(0, 1, 1);
    private static final BlockPos CONTROLLER = new BlockPos(4, 2, 2), FLUID_PORT = new BlockPos(2, 2, 1), POWER_PORT = new BlockPos(2, 2, 3);
    private static final BlockPos[] INSIDE = {new BlockPos(3, 2, 2), new BlockPos(2, 2, 2), new BlockPos(1, 2, 2)};
    private static final List<String> TIERS = List.of("bronze", "iron", "adviron", "titanium", "iridium", "netherite");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Block rotor(String tier) { return BuiltInRegistries.BLOCK.get(id("generator/block_steam_turbine_rotor_" + tier)); }

    private static void build(GameTestHelper h, String... rotors) {
        StructureBlueprint plan = StructureBlueprintRegistry.get(id("steam_tunnel"));
        h.assertTrue(plan != null, "the Steam Tunnel plan is missing");
        for (StructureBlueprint.Placement p : plan.placements()) h.setBlock(ORIGIN.offset(p.pos()), p.key().blocks().get(0).resolve());
        for (int i = 0; i < rotors.length; i++) h.setBlock(INSIDE[i], rotor(rotors[i]));
    }

    private static BlockEntity tunnel(GameTestHelper h) { return h.getBlockEntity(CONTROLLER); }

    private static boolean formed(GameTestHelper h) { return (Boolean) call(tunnel(h), "isFormed"); }

    private static long stepEu(GameTestHelper h) { return (Long) call(tunnel(h), "stepEu"); }

    private static long portEnergy(GameTestHelper h) {
        return (Long) call(call(h.getBlockEntity(POWER_PORT), "getEnergyStorageInternal"), "getAmount");
    }

    private static long total(GameTestHelper h) { return (Long) call(tunnel(h), "energy") + portEnergy(h); }

    private static void steam(GameTestHelper h, int amount) {
        IFluidHandler port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(FLUID_PORT), Direction.NORTH);
        h.assertTrue(port != null && port.fill(new FluidStack(BuiltInRegistries.FLUID.get(id("steam")), amount), IFluidHandler.FluidAction.EXECUTE) == amount,
            "the fluid port took no steam");
    }

    private static void setLong(Object target, String name, long value) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try { var f = k.getDeclaredField(name); f.setAccessible(true); f.setLong(target, value); return; }
            catch (NoSuchFieldException e) { /* the superclass then */ }
            catch (ReflectiveOperationException e) { throw new AssertionError(name, e); }
        }
        throw new AssertionError("no field " + name);
    }

    /** Three iron rotors: 704 EU per step, so 4,096 mB of steam make 2,816 EU in one tick (2,048 without rotors). */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 60)
    public static void threeIronRotorsRaiseEveryStep(GameTestHelper h) {
        build(h, "iron", "iron", "iron");
        h.runAfterDelay(25, () -> {
            h.assertTrue(formed(h), "the tunnel with rotors did not form: " + call(tunnel(h), "status"));
            h.assertTrue(stepEu(h) == 704, "three iron rotors give " + stepEu(h) + " EU per step, not 704");
            steam(h, 4_096);
            h.runAfterDelay(3, () -> {
                h.assertTrue(total(h) == 2_816, "4,096 mB of steam made " + total(h) + " EU with three iron rotors, not 2,816");
                h.succeed();
            });
        });
    }

    /** Tiers mix, front to back: bronze, titanium and netherite give 512 + 32 + 96 + 160 = 800 EU per step (3,200 for 4,096 mB). */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 60)
    public static void mixedRotorTiersAddUp(GameTestHelper h) {
        build(h, "bronze", "titanium", "netherite");
        h.runAfterDelay(25, () -> {
            h.assertTrue(stepEu(h) == 800, "bronze + titanium + netherite give " + stepEu(h) + " EU per step, not 800");
            h.assertTrue(call(tunnel(h), "rotors").toString().equals("[BRONZE, TITANIUM, NETHERITE]"), "rotors front to back: " + call(tunnel(h), "rotors"));
            steam(h, 4_096);
            h.runAfterDelay(3, () -> {
                h.assertTrue(total(h) == 3_200, "4,096 mB of steam made " + total(h) + " EU, not 3,200");
                h.succeed();
            });
        });
    }

    /** Another block inside stops the tunnel and the state names it; a rotor in its place lets it form again. */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 60)
    public static void otherBlocksInsideStopTheTunnel(GameTestHelper h) {
        build(h, "iron");
        h.setBlock(INSIDE[1], Blocks.STONE);
        h.runAfterDelay(25, () -> {
            h.assertTrue(!formed(h) && call(tunnel(h), "status").toString().contains("problem.air"),
                "stone inside was accepted: " + call(tunnel(h), "status"));
            h.setBlock(INSIDE[1], rotor("netherite"));   // placing a rotor checks the tunnel at once
            h.assertTrue(formed(h) && stepEu(h) == 512 + 64 + 160, "with iron and netherite rotors: formed " + formed(h) + ", " + stepEu(h) + " EU per step");
            h.succeed();
        });
    }

    /** Removing a rotor lowers the bonus at once (the tunnel checks itself when a rotor goes), and with none it is 512 EU again. */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 60)
    public static void removingARotorLowersTheBonusAtOnce(GameTestHelper h) {
        build(h, "iron", "iron", "iron");
        h.runAfterDelay(25, () -> {
            h.assertTrue(stepEu(h) == 704, "three iron rotors give " + stepEu(h) + " EU per step");
            h.setBlock(INSIDE[1], Blocks.AIR);
            h.assertTrue(stepEu(h) == 640, "after removing one iron rotor: " + stepEu(h) + " EU per step, not 640");
            h.setBlock(INSIDE[0], Blocks.AIR);
            h.setBlock(INSIDE[2], Blocks.AIR);
            h.assertTrue(formed(h) && stepEu(h) == 512, "without rotors: formed " + formed(h) + ", " + stepEu(h) + " EU per step");
            h.succeed();
        });
    }

    /**
     * With three netherite rotors (992 EU per step, 3,968 EU/t) the one power port sends 8 HV packets a tick: it takes 4,096 EU a tick
     * from a full buffer (2,048 without rotors).
     */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 60)
    public static void onePowerPortCarriesTheFullOutput(GameTestHelper h) {
        build(h, "netherite", "netherite", "netherite");
        h.runAfterDelay(25, () -> {
            h.assertTrue(stepEu(h) == 992 && (Long) call(tunnel(h), "maxEuPerTick") == 3_968, "three netherite rotors: " + stepEu(h) + " EU per step");
            var port = h.getBlockEntity(POWER_PORT);
            h.assertTrue((Integer) call(port, "getPacketCount") == 8 && (Long) call(port, "rate") == 4_096,
                "the power port sends " + call(port, "getPacketCount") + " packets, " + call(port, "rate") + " EU a tick");
            setLong(tunnel(h), "energy", 10_000);
            call(call(port, "getEnergyStorageInternal"), "setStored", 0L);
            h.runAfterDelay(1, () -> {
                long taken = portEnergy(h);
                h.assertTrue(taken == 4_096 || taken == 8_192, "the power port took " + taken + " EU in a tick, not 4,096");
                h.succeed();
            });
        });
    }

    private static long call0(GameTestHelper h, String name) { return ((Number) call(tunnel(h), name)).longValue(); }

    /**
     * Fed 4,096 mB of steam every tick with three netherite rotors (and its buffer and port emptied every tick, as a big consumer
     * would), the tunnel makes 3,968 EU/t on average over a second, uses 4,096 mB/t and waits for nothing: the full output.
     */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 120)
    public static void fullSteamFeedGivesTheFullOutput(GameTestHelper h) {
        build(h, "netherite", "netherite", "netherite");
        int[] tick = {0};
        h.onEachTick(() -> {
            if (++tick[0] < 25 || !formed(h)) return;
            setLong(tunnel(h), "energy", 0);
            call(call(h.getBlockEntity(POWER_PORT), "getEnergyStorageInternal"), "setStored", 0L);
            IFluidHandler port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(FLUID_PORT), Direction.NORTH);
            if (port != null) port.fill(new FluidStack(BuiltInRegistries.FLUID.get(id("steam")), 4_096), IFluidHandler.FluidAction.EXECUTE);
        });
        h.runAfterDelay(25 + 50, () -> {
            h.assertTrue(call0(h, "euPerTick") == 3_968, "with 4,096 mB/t of steam the output is " + call0(h, "euPerTick") + " EU/t, not 3,968");
            h.assertTrue(call0(h, "steamUsePerTick") == 4_096, "steam use " + call0(h, "steamUsePerTick") + " mB/t, not 4,096");
            h.assertTrue(call0(h, "waitReason") == 0, "at full feed it reports waiting (" + call0(h, "waitReason") + ")");
            h.assertTrue(call0(h, "maxEuPerTick") == 3_968, "maximum " + call0(h, "maxEuPerTick") + " EU/t");
            h.succeed();
        });
    }

    /** Without steam the tunnel says it waits for steam (Jade and the right-click status). */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 80)
    public static void aTunnelWithoutSteamSaysSo(GameTestHelper h) {
        build(h, "iron");
        h.runAfterDelay(50, () -> {
            h.assertTrue(call0(h, "waitReason") == 1, "without steam the wait reason is " + call0(h, "waitReason") + ", not 1 (steam)");
            h.assertTrue(call(tunnel(h), "status").toString().contains("waiting_steam"), "status: " + call(tunnel(h), "status"));
            h.succeed();
        });
    }

    /** With its buffer and its power port full (nothing draws the power) and steam waiting, it says its buffer is full. */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 100)
    public static void aTunnelWithAFullBufferSaysSo(GameTestHelper h) {
        build(h);
        int[] tick = {0};
        h.onEachTick(() -> {
            if (++tick[0] < 25 || !formed(h)) return;
            setLong(tunnel(h), "energy", 10_000);
            call(call(h.getBlockEntity(POWER_PORT), "getEnergyStorageInternal"), "setStored", 8_192L);
            if (tick[0] == 25) steam(h, 8_192);
        });
        h.runAfterDelay(25 + 45, () -> {
            h.assertTrue(call0(h, "waitReason") == 2, "with a full buffer the wait reason is " + call0(h, "waitReason") + ", not 2 (buffer)");
            h.assertTrue(call(tunnel(h), "status").toString().contains("buffer_full"), "status: " + call(tunnel(h), "status"));
            h.succeed();
        });
    }

    /**
     * Rotors face the player who places them, in all six directions: looking down places one facing up (a vertical axis, a vertical
     * outline), looking north one facing south. A rotor facing up counts in the tunnel like any other.
     */
    @SuppressWarnings("unchecked")
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 40)
    public static void rotorsFaceAllSixDirections(GameTestHelper h) {
        Block iron = rotor("iron");
        var facing = (net.minecraft.world.level.block.state.properties.Property<Direction>) iron.getStateDefinition().getProperty("facing");
        h.assertTrue(facing != null && facing.getPossibleValues().size() == 6, "rotors do not have six facings: " + facing);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack = new ItemStack(iron);
        BlockPos at = h.absolutePos(new BlockPos(1, 1, 1));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.UP, at, false);
        player.setXRot(90);   // looking straight down
        var down = iron.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit));
        h.assertTrue(down != null && down.getValue(facing) == Direction.UP, "looking down placed " + down);
        var shape = down.getShape(h.getLevel(), at).bounds();
        h.assertTrue(shape.minY == 0 && shape.maxY == 1 && shape.minX > 0, "the outline of an upright rotor is " + shape);
        player.setXRot(0);
        player.setYRot(180);  // looking north (the look direction follows the head)
        player.setYHeadRot(180);
        var north = iron.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit));
        h.assertTrue(north != null && north.getValue(facing) == Direction.SOUTH, "looking north placed " + north);
        build(h);
        h.setBlock(INSIDE[1], iron.defaultBlockState().setValue(facing, Direction.UP));
        h.runAfterDelay(25, () -> {
            h.assertTrue(formed(h) && stepEu(h) == 576, "an upright iron rotor: formed " + formed(h) + ", " + stepEu(h) + " EU per step, not 576");
            h.succeed();
        });
    }

    /** The blades and the shaft of each crafting tier: the X of them makes the steam rotor, the plus the kinetic rotor. */
    private static final java.util.Map<String, String[]> PARTS = java.util.Map.of(
        "bronze", new String[] {"resource/item_bronze_rotor_blade", "resource/item_bronze_shaft"},
        "iron", new String[] {"resource/item_iron_rotor_blade", "resource/item_iron_shaft"},
        "adviron", new String[] {"resource/item_adviron_rotor_blade", "resource/item_adviron_shaft"},
        "titanium", new String[] {"resource/item_titanium_iron_alloy_rotor_blade", "resource/item_titanium_shaft"},
        "iridium", new String[] {"resource/item_super_iridium_alloy_rotor_blade", "resource/item_adviron_shaft"});

    private static net.minecraft.world.item.crafting.CraftingInput grid(String pattern, String blade, String shaft) {
        List<ItemStack> items = new java.util.ArrayList<>();
        for (char c : pattern.toCharArray())
            items.add(c == 'B' ? new ItemStack(BuiltInRegistries.ITEM.get(id(blade))) : c == 'S' ? new ItemStack(BuiltInRegistries.ITEM.get(id(shaft))) : ItemStack.EMPTY);
        return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, items);
    }

    /**
     * The X of four blades around a shaft is the only recipe for that grid and makes the steam rotor; the plus of the same parts no
     * longer does (it is the kinetic rotor's recipe: both used to match).
     */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 20)
    public static void rotorRecipesDoNotClashWithKineticRotors(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        for (var entry : PARTS.entrySet()) {
            String tier = entry.getKey(), blade = entry.getValue()[0], shaft = entry.getValue()[1];
            var x = grid("B B" + " S " + "B B", blade, shaft);
            var found = recipes.getRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, x, h.getLevel());
            h.assertTrue(found.size() == 1 && found.get(0).value().assemble(x, h.getLevel().registryAccess()).is(rotor(tier).asItem()),
                "the X of " + tier + " blades matches " + found.stream().map(r -> r.id().toString()).toList());
            var plus = grid(" B " + "BSB" + " B ", blade, shaft);
            boolean steam = recipes.getRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, plus, h.getLevel()).stream()
                .anyMatch(r -> r.value().assemble(plus, h.getLevel().registryAccess()).is(rotor(tier).asItem()));
            h.assertTrue(!steam, "the plus of " + tier + " blades still makes a steam rotor");
        }
        h.succeed();
    }

    /** Each tier has a crafting recipe (netherite: smithing from the iridium rotor), a blockstate, block and item models and a texture. */
    @GameTest(batch = "round1141_rotors", template = "empty", timeoutTicks = 20)
    public static void everyRotorTierHasRecipesAndAssets(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        var registries = h.getLevel().registryAccess();
        for (String tier : TIERS) {
            String name = "block_steam_turbine_rotor_" + tier;
            String recipe = tier.equals("netherite") ? "smithing_steam_turbine_rotor_netherite" : "shaped_steam_turbine_rotor_" + tier;
            var holder = recipes.byKey(id(recipe));
            h.assertTrue(holder.isPresent(), "no recipe " + recipe);
            ItemStack made = holder.get().value().getResultItem(registries);
            h.assertTrue(made.is(rotor(tier).asItem()), recipe + " makes " + made);
            for (String asset : List.of("blockstates/generator/" + name + ".json", "models/block/generator/" + name + ".json",
                    "models/item/generator/" + name + ".json", "textures/block/generator/steam_turbine_rotor_" + tier + ".png"))
                h.assertTrue(Round1141GameTests.class.getClassLoader().getResource("assets/mio_icif/" + asset) != null, "missing " + asset);
        }
        h.assertTrue(Round1141GameTests.class.getClassLoader().getResource("assets/mio_icif/models/block/generator/block_steam_turbine_rotor.json") != null,
            "missing the shared rotor model");
        h.succeed();
    }
}
