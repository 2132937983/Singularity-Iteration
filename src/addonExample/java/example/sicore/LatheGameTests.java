package example.sicore;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import com.miophas.singularity_iteration.core.api.util.IBoxable;
import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Lathe, its tools and its turning blanks against IC2 2.2.828 (TileEntityLathe, ItemLatheDefault,
 * ItemLathingTool): cuts, KU cost and buffer, decay, automation sides, recipes.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class LatheGameTests {
    private LatheGameTests() {}

    private static final BlockPos LATHE = new BlockPos(1, 1, 1);

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(rl(path))); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static BlockEntity placeLathe(GameTestHelper h) {
        h.setBlock(LATHE, BuiltInRegistries.BLOCK.get(rl("producer/block_lathe")));
        BlockEntity be = h.getBlockEntity(LATHE);
        h.assertTrue(be != null, "no lathe block entity");
        return be;
    }

    private static int ku(BlockEntity be) {
        try { return be.getClass().getField("kUBuffer").getInt(be); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void setKu(BlockEntity be, int value) {
        try { be.getClass().getField("kUBuffer").setInt(be, value); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static boolean cut(BlockEntity be, int position) { return (Boolean) call(be, "process", position); }

    private static void assertEq(GameTestHelper h, long expected, long actual, String what) {
        h.assertTrue(expected == actual, what + ": expected " + expected + ", got " + actual);
    }

    /** IC2 ItemLathingTool (hardness 3, 50 uses, toolbox) and ItemLatheDefault (widths, hardness, by-products). */
    @GameTest(batch = "lathe", template = "empty", timeoutTicks = 20)
    public static void toolAndBlanksMatchIc2(GameTestHelper h) {
        ItemStack tool = item("item_iron_lathing_tool");
        h.assertTrue(tool.getItem() instanceof ILatheItem.ILatheTool, "iron lathing tool is not a lathe tool");
        ILatheItem.ILatheTool t = (ILatheItem.ILatheTool) tool.getItem();
        assertEq(h, 3, t.getHardness(tool), "tool hardness");
        assertEq(h, 50, t.getMaxCustomDamage(tool), "tool uses");
        assertEq(h, 1, tool.getMaxStackSize(), "tool stack size");
        h.assertTrue(tool.getItem() instanceof IBoxable b && b.canBeStoredInToolbox(tool), "tool does not fit the toolbox");
        t.setCustomDamage(tool, 7);
        assertEq(h, 7, tool.getDamageValue(), "custom damage is the item damage");

        ItemStack iron = item("normal/item_iron_turning_blanks");
        ItemStack wood = item("normal/item_wooden_turning_blanks");
        ILatheItem li = (ILatheItem) iron.getItem();
        ILatheItem lw = (ILatheItem) wood.getItem();
        assertEq(h, 5, li.getWidth(iron), "iron width");
        assertEq(h, 2, li.getHardness(iron), "iron hardness");
        assertEq(h, 3, lw.getWidth(wood), "wood width");
        assertEq(h, 0, lw.getHardness(wood), "wood hardness");
        h.assertTrue(lw.getOutputItem(wood, 0).is(Items.STICK), "wood gives sticks");
        h.assertTrue(BuiltInRegistries.ITEM.getKey(li.getOutputItem(iron, 0).getItem()).getPath().contains("dust"), "iron gives small iron dust");
        h.assertTrue(Math.abs(li.getOutputChance(iron, 0) - 0.5F) < 1e-6 && Math.abs(lw.getOutputChance(wood, 0) - 0.1F) < 1e-6, "by-product chances");

        // state: full width by default, clamped to [1, width]
        h.assertTrue(java.util.Arrays.equals(new int[]{5, 5, 5, 5, 5}, li.getCurrentState(iron)), "fresh iron blank state");
        li.setState(iron, 2, 0);
        li.setState(iron, 3, 9);
        h.assertTrue(java.util.Arrays.equals(new int[]{5, 5, 1, 5, 5}, li.getCurrentState(iron)), "state clamps to [1, width]");
        assertEq(h, 0, li.getCurrentState(ItemStack.EMPTY).length, "empty stack has no state");

        // the profile sheet the GUI draws exists
        for (ItemStack blank : new ItemStack[]{iron, wood}) {
            ResourceLocation tex = ((ILatheItem) blank.getItem()).getTexture(blank);
            var file = net.neoforged.fml.ModList.get().getModFileById(tex.getNamespace()).getFile();
            h.assertTrue(Files.exists(file.findResource("assets/" + tex.getNamespace() + "/" + tex.getPath())), "missing profile texture " + tex);
        }
        h.succeed();
    }

    /** The translation key and arguments of the tooltip's stage line. */
    private static net.minecraft.network.chat.contents.TranslatableContents stage(GameTestHelper h, ItemStack blank) {
        for (var line : blank.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(h.getLevel()), null,
                net.minecraft.world.item.TooltipFlag.Default.NORMAL)) {
            if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().startsWith("item.mio_icif.turning_blanks.stage.")) return t;
        }
        throw new AssertionError("no stage line");
    }

    /**
     * Turning blanks show their state: the item model is drawn by the profile (builtin/entity), a bar
     * shows the material left once cut, the tooltip gives the stage and the profile image.
     */
    @GameTest(batch = "lathe", template = "empty", timeoutTicks = 20)
    public static void blanksShowTheirState(GameTestHelper h) {
        for (String name : new String[]{"item_iron_turning_blanks", "item_wooden_turning_blanks"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/models/item/normal/" + name + ".json");
            h.assertTrue(json != null && json.contains("minecraft:builtin/entity") && json.contains("\"gui_light\": \"front\""),
                name + " is not drawn by its profile");
        }

        ItemStack iron = item("normal/item_iron_turning_blanks");
        ILatheItem l = (ILatheItem) iron.getItem();
        h.assertTrue(iron.getTooltipImage().isPresent(), "no profile tooltip image");
        h.assertTrue(!iron.isBarVisible(), "an uncut blank shows a bar");
        h.assertTrue(stage(h, iron).getKey().endsWith(".blank"), "uncut stage");

        l.setState(iron, 0, 4);
        h.assertTrue(iron.isBarVisible(), "no bar after a cut");
        assertEq(h, 12, iron.getBarWidth(), "bar after 1 of 20 cuts");
        var turned = stage(h, iron);
        h.assertTrue(turned.getKey().endsWith(".turned") && turned.getArgs().length == 2
            && Integer.valueOf(1).equals(turned.getArgs()[0]) && Integer.valueOf(20).equals(turned.getArgs()[1]), "turned stage " + turned);

        for (int j = 0; j < 5; j++) l.setState(iron, j, 1);
        h.assertTrue(stage(h, iron).getKey().endsWith(".finished"), "finished stage");
        assertEq(h, 0, iron.getBarWidth(), "bar of a fully turned blank");

        ItemStack wood = item("normal/item_wooden_turning_blanks");
        ((ILatheItem) wood.getItem()).setState(wood, 2, 2);
        var woodTurned = stage(h, wood);
        h.assertTrue(Integer.valueOf(10).equals(woodTurned.getArgs()[1]), "wooden blank takes 10 cuts: " + woodTurned);
        h.succeed();
    }

    /** The translation keys of all tooltip lines. */
    private static java.util.List<String> tooltipKeys(GameTestHelper h, ItemStack stack) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (var line : stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(h.getLevel()), null,
                net.minecraft.world.item.TooltipFlag.Default.NORMAL)) {
            if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) keys.add(t.getKey());
        }
        return keys;
    }

    /**
     * A blank cut to a recipe's profile is a named part: the tooltip gives its name and the item it is
     * for; an iron blank that can still reach a named profile hints at the Shift list.
     */
    @GameTest(batch = "lathe", template = "empty", timeoutTicks = 20)
    public static void namedShapesInTooltips(GameTestHelper h) {
        ItemStack tip = item("normal/item_iron_turning_blanks");
        ILatheItem l = (ILatheItem) tip.getItem();
        for (int j = 1; j < 5; j++) l.setState(tip, j, 5 - j);
        var keys = tooltipKeys(h, tip);
        h.assertTrue(keys.contains("ic2.itemTurningBlanks.tooltip.54321") && keys.contains("item.mio_icif.turning_blanks.used_in"),
            "5-4-3-2-1 is not named: " + keys);

        var fresh = tooltipKeys(h, item("normal/item_iron_turning_blanks"));
        h.assertTrue(fresh.contains("item.mio_icif.turning_blanks.targets.hint") && !fresh.contains("item.mio_icif.turning_blanks.used_in"),
            "uncut iron blank: " + fresh);
        var wood = tooltipKeys(h, item("normal/item_wooden_turning_blanks"));
        h.assertTrue(!wood.contains("item.mio_icif.turning_blanks.targets.hint"), "wooden blanks have no named shapes: " + wood);
        h.succeed();
    }

    /** IC2 TileEntityLathe.process / canWork: 1000 KU and 1 durability per cut, thickness never below 1. */
    @GameTest(batch = "lathe", template = "empty", timeoutTicks = 20)
    public static void cutsLikeIc2(GameTestHelper h) {
        BlockEntity be = placeLathe(h);
        IItemHandler inv = (IItemHandler) call(be, "getItemHandler");
        inv.insertItem(0, item("item_iron_lathing_tool"), false);
        inv.insertItem(1, item("normal/item_iron_turning_blanks"), false);
        ItemStack blank = inv.getStackInSlot(1);
        ILatheItem l = (ILatheItem) blank.getItem();
        h.assertTrue(!inv.getStackInSlot(0).isEmpty() && !blank.isEmpty(), "tool or blank not inserted");

        setKu(be, 2500);
        h.assertTrue(cut(be, 0), "first cut failed");
        assertEq(h, 4, l.getCurrentState(inv.getStackInSlot(1))[0], "position 0 after one cut");
        assertEq(h, 1, inv.getStackInSlot(0).getDamageValue(), "tool damage after one cut");
        assertEq(h, 1500, ku(be), "KU after one cut");
        h.assertTrue(cut(be, 1), "second cut failed");
        assertEq(h, 500, ku(be), "KU after two cuts");
        h.assertTrue(!cut(be, 2), "cut without 1000 KU");
        assertEq(h, 5, l.getCurrentState(inv.getStackInSlot(1))[2], "no cut without KU");
        h.assertTrue(!cut(be, -1) && !cut(be, 5), "cut outside 0-4");

        setKu(be, 10000);
        for (int i = 0; i < 3; i++) h.assertTrue(cut(be, 0), "cut " + i + " at position 0 failed");
        assertEq(h, 1, l.getCurrentState(inv.getStackInSlot(1))[0], "position 0 thinned to 1");
        h.assertTrue(!cut(be, 0), "cut below thickness 1");
        assertEq(h, 7000, ku(be), "KU after three more cuts");
        assertEq(h, 5, inv.getStackInSlot(0).getDamageValue(), "tool damage after five cuts");
        ItemStack out = inv.getStackInSlot(2);
        h.assertTrue(out.getCount() <= 5 && (out.isEmpty() || ItemStack.isSameItem(out, l.getOutputItem(blank, 0))), "by-products " + out);

        // the tool is used up at its 50th cut
        ((ILatheItem.ILatheTool) inv.getStackInSlot(0).getItem()).setCustomDamage(inv.getStackInSlot(0), 49);
        h.assertTrue(cut(be, 4), "last cut of the tool failed");
        h.assertTrue(inv.getStackInSlot(0).isEmpty(), "tool not destroyed at 50 uses");
        h.assertTrue(!cut(be, 4), "cut without a tool");
        h.succeed();
    }

    /** IC2 InvSlotConsumableClass / InvSlotOutput: automation inserts tool and blank, extracts only the output. */
    @GameTest(batch = "lathe", template = "empty", timeoutTicks = 20)
    public static void automationOnlyExtractsOutput(GameTestHelper h) {
        BlockEntity be = placeLathe(h);
        IItemHandler side = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(LATHE), Direction.UP);
        h.assertTrue(side != null, "no item capability");
        h.assertTrue(side.insertItem(2, item("normal/item_iron_turning_blanks"), false).getCount() == 1, "blank inserted into the output");
        h.assertTrue(side.insertItem(1, new ItemStack(Items.STICK), false).getCount() == 1, "non-blank inserted as workpiece");
        h.assertTrue(side.insertItem(1, item("normal/item_iron_turning_blanks"), false).isEmpty(), "blank not inserted");
        h.assertTrue(side.insertItem(0, item("item_iron_lathing_tool"), false).isEmpty(), "tool not inserted");
        h.assertTrue(side.extractItem(0, 1, false).isEmpty() && side.extractItem(1, 1, false).isEmpty(), "tool or workpiece extracted");

        IItemHandler inv = (IItemHandler) call(be, "getItemHandler");
        ((net.neoforged.neoforge.items.IItemHandlerModifiable) inv).setStackInSlot(2, new ItemStack(Items.STICK, 3));
        assertEq(h, 3, side.extractItem(2, 64, false).getCount(), "output extracted");

        // a Container, so breaking the lathe spills the tool and the workpiece
        h.assertTrue(be instanceof Container c && c.getContainerSize() == 3 && !c.isEmpty(), "lathe is not a 3-slot container");
        h.succeed();
    }

    /** IC2 updateEntityServer: active above 500 KU, 1% integer decay, buffer of 10000 fed from any side. */
    @GameTest(batch = "lathe", template = "empty", timeoutTicks = 20)
    public static void kineticBufferLikeIc2(GameTestHelper h) {
        BlockEntity be = placeLathe(h);
        var kinetic = h.getLevel().getCapability(IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, h.absolutePos(LATHE), Direction.NORTH);
        h.assertTrue(kinetic != null && kinetic.canReceiveKinetic() && !kinetic.canExtractKinetic(), "no receive-only KU input");
        assertEq(h, 3000, kinetic.receiveKinetic(3000, false), "KU accepted");
        assertEq(h, 3000, ku(be), "KU buffer");
        assertEq(h, 7000, kinetic.receiveKinetic(20000, true), "room up to 10000");
        assertEq(h, 0, kinetic.extractKinetic(1000, false), "KU extracted from the lathe");

        Class<?> type = be.getClass();
        BlockPos pos = h.absolutePos(LATHE);
        int[][] decay = {{10000, 9900}, {150, 149}, {99, 99}, {0, 0}};
        for (int[] d : decay) {
            setKu(be, d[0]);
            call(type, "tick", h.getLevel(), pos, h.getLevel().getBlockState(pos), be);
            assertEq(h, d[1], ku(be), "decay of " + d[0]);
        }

        setKu(be, 600);
        call(type, "tick", h.getLevel(), pos, h.getLevel().getBlockState(pos), be);
        BlockState state = h.getLevel().getBlockState(pos);
        BooleanProperty lit = (BooleanProperty) state.getBlock().getStateDefinition().getProperty("lit");
        h.assertTrue(lit != null && state.getValue(lit), "not active above 500 KU");
        setKu(be, 500);
        call(type, "tick", h.getLevel(), pos, h.getLevel().getBlockState(pos), be);
        h.assertTrue(!h.getLevel().getBlockState(pos).getValue(lit), "active at 500 KU");
        h.succeed();
    }

    /** IC2 shaped_recipes.ini: Lathe "C C|SMS|PPP", Lathing Tool "S  | I |  I". */
    @GameTest(batch = "lathe", template = "empty", timeoutTicks = 20)
    public static void recipesMatchIc2(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        var tool = recipes.byKey(rl("item_iron_lathing_tool"));
        h.assertTrue(tool.isPresent(), "no lathing tool recipe");
        h.assertTrue(tool.get().value().getResultItem(h.getLevel().registryAccess()).is(item("item_iron_lathing_tool").getItem()), "lathing tool recipe result");
        NonNullList<Ingredient> toolGrid = tool.get().value().getIngredients();
        h.assertTrue(toolGrid.size() == 9 && !toolGrid.get(0).isEmpty() && !toolGrid.get(4).isEmpty() && !toolGrid.get(8).isEmpty()
            && toolGrid.get(1).isEmpty() && toolGrid.get(2).isEmpty(), "lathing tool pattern " + toolGrid.size());

        var lathe = recipes.byKey(rl("shaped_325_lathe"));
        h.assertTrue(lathe.isPresent(), "no lathe recipe");
        NonNullList<Ingredient> latheGrid = lathe.get().value().getIngredients();
        h.assertTrue(latheGrid.size() == 9 && !latheGrid.get(0).isEmpty() && latheGrid.get(1).isEmpty() && !latheGrid.get(2).isEmpty(),
            "lathe top row is not C C");
        h.succeed();
    }
}
