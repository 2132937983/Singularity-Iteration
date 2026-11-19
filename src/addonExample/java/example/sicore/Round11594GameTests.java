package example.sicore;

import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.94: the deuterium fuel rod (a neutron source in a fission reactor, fuel in the fusion reactor; spent, an empty fuel
 * rod), its chain (hot water cell -> thermal centrifuge -> unstable hot water cell -> canner with an empty fuel rod), the
 * coolant cells of the disruption mitigation (60k best, ice no longer), the meltdown (a blast 50 x the fission reactor's power
 * limit by default, none with the multiplier at 0, a local one with the nuclear terrain damage off) and the structure (the
 * cryogenic port in the middle duct, the corner coils closed at y = +-2, every coil block facing along its coil).
 * (The mod's common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11594GameTests {
    private Round11594GameTests() {}

    private static final String INVENTORY = "reactor.ReactorInventory", CONTROLLER = "blockentity.fusion.FusionControllerBlockEntity",
        CONFIG = "Singularity_Iteration_Config";

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(id(path)); }
    private static Class<?> cls(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static long lng(Object o) { return ((Number) o).longValue(); }

    private static Object staticField(String owner, String name) {
        try {
            Field f = cls(owner).getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // the superclass
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(name, e);
            }
        }
        throw new AssertionError("no field " + name);
    }

    @GameTest(template = "empty", batch = "round11594_deuterium", timeoutTicks = 20)
    public static void deuteriumRodIsANeutronSourceThatEndsAsAnEmptyRod(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        ItemStack rod = new ItemStack(item("normal/item_deuterium_cell"));
        if (!(Boolean) call(cls(INVENTORY), "supported", rod)) wrong.add("a fission reactor does not take the deuterium rod");
        var part = (ReactorCycle.Part) call(cls(INVENTORY), "read", rod);
        if (part == null || part.profile().kind() != ReactorCycle.Kind.NEUTRON_SOURCE || part.profile().capacity() != 5000
                || part.stored() != 0 || part.profile().selfCooling() != 0)
            wrong.add("the deuterium rod is not a fresh 5,000-cycle neutron source without decay heat: " + part);
        if (rod.getMaxStackSize() != 64) wrong.add("the deuterium rod does not stack to 64");
        // its neutrons: a uranium rod next to it fissions once more per cycle (more EU) than alone
        var uranium = (ReactorCycle.Profile) call(cls(INVENTORY), "profile", "uranium_simple");
        ReactorCycle.Part[] alone = new ReactorCycle.Part[54], beside = new ReactorCycle.Part[54];
        alone[10] = new ReactorCycle.Part(uranium, 0, 100);
        beside[10] = new ReactorCycle.Part(uranium, 0, 100);
        beside[11] = part;
        double euAlone = ReactorCycle.step(alone, 9, 0, true).euPerTick(), euBeside = ReactorCycle.step(beside, 9, 0, true).euPerTick();
        if (!(euBeside > euAlone)) wrong.add("no extra pulse beside the deuterium rod: " + euBeside + " vs " + euAlone + " EU/t");
        // spent: the empty fuel rod
        var spent = part.withStored(part.profile().capacity());
        ItemStack after = (ItemStack) call(cls(INVENTORY), "write", rod, part, spent, false);
        if (!after.is(item("normal/item_fuel_rod"))) wrong.add("a spent deuterium rod leaves " + after + ", not the empty fuel rod");
        h.assertTrue(wrong.isEmpty(), "deuterium rod: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11594_deuterium", timeoutTicks = 20)
    public static void deuteriumComesFromHotWaterThroughTheCentrifugeAndTheCanner(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        var fluid = BuiltInRegistries.FLUID.get(id("unstable_hotwater"));
        if (fluid == Fluids.EMPTY) wrong.add("no unstable hot water fluid");
        else {
            if (fluid.getBucket() != item("unstable_hotwater_bucket")) wrong.add("the fluid has no bucket");
            if (fluid.getFluidType().getTemperature() != 343) wrong.add("unstable hot water is not hot");
        }
        if (BuiltInRegistries.FLUID.get(id("unstable_hotwater_flowing")) == Fluids.EMPTY) wrong.add("no flowing unstable hot water");
        if (BuiltInRegistries.BLOCK.get(id("unstable_hotwater")) == Blocks.AIR) wrong.add("no unstable hot water block");
        ItemStack hotCell = new ItemStack(item("cell/item_cell_hotwater")), waterCell = new ItemStack(item("cell/item_cell_water"));
        ItemStack unstableCell = new ItemStack(item("cell/item_cell_unstable_hotwater")), emptyRod = new ItemStack(item("normal/item_fuel_rod"));
        if (unstableCell.isEmpty()) wrong.add("no unstable hot water cell");
        var recipes = h.getLevel().getRecipeManager();
        var registries = h.getLevel().registryAccess();
        if (recipes.byKey(id("fusion/deuterium_cell")).isPresent()) wrong.add("the centrifuge still makes deuterium from water");
        var centrifuge = recipes.byKey(id("fusion/unstable_hotwater_cell"));
        if (centrifuge.isEmpty()) wrong.add("no centrifuge recipe for unstable hot water");
        else {
            Recipe<?> r = centrifuge.get().value();
            Ingredient in = (Ingredient) call(r, "getIngredient");
            if (!in.test(hotCell) || in.test(waterCell)) wrong.add("the centrifuge does not take hot water cells (only)");
            if (!r.getResultItem(registries).is(unstableCell.getItem())) wrong.add("the centrifuge does not give the unstable hot water cell");
        }
        var canning = recipes.byKey(id("fusion/deuterium_fuel_rod"));
        if (canning.isEmpty()) wrong.add("no canning recipe for the deuterium rod");
        else {
            Recipe<?> r = canning.get().value();
            Ingredient can = (Ingredient) call(r, "getCanIngredient"), food = (Ingredient) call(r, "getFoodIngredient");
            if (!can.test(unstableCell) || can.test(hotCell)) wrong.add("the canner's left slot does not take the unstable hot water cell (only)");
            if (!food.test(emptyRod)) wrong.add("the canner's middle slot does not take the empty fuel rod");
            if (!r.getResultItem(registries).is(item("normal/item_deuterium_cell"))) wrong.add("the canner does not give the deuterium rod");
        }
        h.assertTrue(wrong.isEmpty(), "deuterium chain: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11594_fusion", timeoutTicks = 40)
    public static void controllerBurnsDeuteriumRodsAndMitigatesWithCoolantCells(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, FusionBlocks.controller().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        BlockEntity c = h.getBlockEntity(at);
        var items = (IItemHandlerModifiable) call(c, "getItems");
        int d = (int) lng(staticField(CONTROLLER, "SLOT_DEUTERIUM")), mit = (int) lng(staticField(CONTROLLER, "SLOT_MITIGATION"));
        int out1 = (int) lng(staticField(CONTROLLER, "SLOT_EMPTY_CELL")), out2 = (int) lng(staticField(CONTROLLER, "SLOT_EMPTY_ROD"));
        List<String> wrong = new ArrayList<>();
        ItemStack rod = new ItemStack(item("normal/item_deuterium_cell"));
        if (!items.isItemValid(d, rod)) wrong.add("the deuterium slot refuses the deuterium rod");
        if (items.isItemValid(mit, new ItemStack(Items.PACKED_ICE))) wrong.add("the mitigation slot still takes ice");
        for (String cell : new String[] {"reactor/item_reactor_collant_simple", "reactor/item_reactor_collant_triple", "reactor/item_reactor_coolant_six"})
            if (!items.isItemValid(mit, new ItemStack(item(cell)))) wrong.add("the mitigation slot refuses " + cell);
        if (lng(call(cls(CONTROLLER), "fuelUnits", rod)) != 10_000) wrong.add("a fresh deuterium rod is not 10,000 units");
        // the more cold, the less heat; 60k leaves 5 %
        double s10 = (double) call(cls(CONTROLLER), "mitigatedHeatShare", 10_000L), s30 = (double) call(cls(CONTROLLER), "mitigatedHeatShare", 30_000L),
            s60 = (double) call(cls(CONTROLLER), "mitigatedHeatShare", 60_000L);
        if (!(s10 > s30 && s30 > s60) || Math.abs(s60 - 0.05) > 1e-9) wrong.add("mitigation shares " + s10 + " / " + s30 + " / " + s60);
        long hull0 = ((long[]) call(c, "screenValues"))[10];
        items.setStackInSlot(mit, new ItemStack(item("reactor/item_reactor_coolant_six")));
        call(c, "disrupt", true, 15.0);
        long hull60 = ((long[]) call(c, "screenValues"))[10] - hull0;
        items.setStackInSlot(mit, new ItemStack(item("reactor/item_reactor_collant_simple")));
        call(c, "disrupt", true, 15.0);
        long hull10 = ((long[]) call(c, "screenValues"))[10] - hull0 - hull60;
        if (!(hull10 > hull60) || hull60 <= 0) wrong.add("a 10k cell (" + hull10 + " HU) is not worse than a 60k cell (" + hull60 + " HU)");
        items.setStackInSlot(d, new ItemStack(item("normal/item_deuterium_cell"), 2));
        h.runAfterDelay(5, () -> {
            long deuterium = ((long[]) call(c, "screenValues"))[8];
            int rods = items.getStackInSlot(out1).getCount() + items.getStackInSlot(out2).getCount();
            if (deuterium != 20_000) wrong.add("two deuterium rods gave " + deuterium + " units");
            if (rods != 2 || !items.getStackInSlot(out1).is(item("normal/item_fuel_rod"))) wrong.add("the empty fuel rods did not come out: " + rods);
            h.assertTrue(wrong.isEmpty(), "controller: " + wrong);
            h.succeed();
        });
    }

    /** The fusion controller's block, without the fusion classes. */
    private static final class FusionBlocks {
        static net.minecraft.world.level.block.Block controller() { return BuiltInRegistries.BLOCK.get(id("fusion/block_fusion_controller")); }
    }

    @GameTest(template = "empty", batch = "round11594_fusion_meltdown", timeoutTicks = 200)
    public static void meltdownFollowsTheConfig(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        if (Math.abs((float) call(cls(CONTROLLER), "explosionPower", 50.0, 45) - 2250F) > 1e-3) wrong.add("50 x 45 is not 2,250");
        if ((float) call(cls(CONTROLLER), "explosionPower", 0.0, 45) != 0F) wrong.add("a multiplier of 0 still blasts");
        Object multiplier = staticField(CONFIG, "FUSION_EXPLOSION_MULTIPLIER"), nuclear = staticField(CONFIG, "ENABLE_NUCLEAR_EXPLOSION");
        if (((Number) call(multiplier, "getDefault")).doubleValue() != 50.0) wrong.add("the default multiplier is not 50");
        BlockPos at = h.absolutePos(new BlockPos(2, 6, 2));
        call(cls("fusion.FusionDemo"), "placeStructure", h.getLevel(), at);
        h.runAfterDelay(3, () -> {
            BlockEntity c = h.getLevel().getBlockEntity(at);
            call(c, "validate");
            if (!(Boolean) call(c, "isFormed")) wrong.add("the tokamak as laid out did not form");
            double m0 = ((Number) call(multiplier, "get")).doubleValue();
            boolean n0 = (Boolean) call(nuclear, "get");
            try {
                // the multiplier at 0: a blanket at its maximum heat does not melt down
                call(multiplier, "set", 0.0);
                setField(c, "hull", ((long[]) call(c, "screenValues"))[11]);
                if ((Boolean) call(c, "meltdown", h.getLevel())) wrong.add("it melted down with the multiplier at 0");
                if (!h.getLevel().getBlockState(at).is(FusionBlocks.controller())) wrong.add("the controller went with the multiplier at 0");
                // on, without the nuclear terrain damage: a local explosion; the controller and its contents go
                call(multiplier, "set", 50.0);
                call(nuclear, "set", false);
                var items = (IItemHandlerModifiable) call(c, "getItems");
                items.setStackInSlot(0, new ItemStack(item("reactor/item_reactor_iridium_oc_vent")));
                if (!(Boolean) call(c, "meltdown", h.getLevel())) wrong.add("no meltdown at the maximum heat");
                if (!h.getLevel().getBlockState(at).isAir()) wrong.add("the controller survived its meltdown");
                if (!items.getStackInSlot(0).isEmpty()) wrong.add("the blanket's contents survived");
            } finally {
                // a controller left by a failed check must not melt down for real once the config is back
                if (h.getLevel().getBlockEntity(at) == c) setField(c, "hull", 0L);
                call(multiplier, "set", m0);
                call(nuclear, "set", n0);
            }
            h.assertTrue(wrong.isEmpty(), "meltdown: " + wrong);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "round11594_fusion", timeoutTicks = 20)
    public static void structureHasTheMiddleCryoPortAndClosedCornerCoils(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        Object s = call(cls("fusion.FusionStructure"), "tokamak");
        // laid out relative to the controller at the torus's (12, 0, 4): the cryogenic port at (12, 0, -4), a corner leg at (8, +-2, 8)
        boolean cryo = false, coilsFace = true;
        int corners = 0;
        for (Object b : (List<?>) call(s, "laid")) {
            int x = ((Number) call(b, "x")).intValue() + 12, y = ((Number) call(b, "y")).intValue(), z = ((Number) call(b, "z")).intValue() + 4;
            String name = (String) call(b, "name"), facing = (String) call(b, "facing");
            if (name.equals("cryo_port") && x == 12 && y == 0 && z == -4 && facing.equals("east")) cryo = true;
            if (name.equals("toroidal_coil")) {
                if (facing.isEmpty()) coilsFace = false;
                if (Math.abs(x) == 8 && Math.abs(z) == 8 && Math.abs(y) == 2) corners++;
            }
        }
        if (!cryo) wrong.add("the cryogenic port is not in the middle duct (12, 0, -4) facing east");
        if (corners != 8) wrong.add(corners + " corner coil blocks at y = +-2, expected 8");
        if (!coilsFace) wrong.add("a toroidal coil block has no facing in the layout");
        var coil = BuiltInRegistries.BLOCK.get(id("fusion/block_fusion_toroidal_coil")).defaultBlockState();
        if (!coil.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) wrong.add("the toroidal coil block has no facing");
        h.assertTrue(wrong.isEmpty(), "structure: " + wrong);
        h.succeed();
    }
}
