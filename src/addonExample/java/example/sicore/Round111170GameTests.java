package example.sicore;

import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.170: the toolbox and the advanced toolbox were plain items (no GUI, nothing went in). Now they open a GUI of 9 and 27
 * slots that takes tools, weapons and electric items (not blocks, not a toolbox), keep what is put in when closed, the advanced
 * toolbox is made from a toolbox and carbon plates with its contents, and a toolbox used up in another recipe gives its contents
 * back. Multiblock casings and ports (mio_icif:wrenchable) are taken apart by the wrench.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111170GameTests {
    private Round111170GameTests() {}

    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Class<?> toolbox() { return Round33GameTests.common("item.tools.mio_icif_toolbox"); }
    private static boolean fits(ItemStack stack) { return (Boolean) Round33GameTests.call(toolbox(), "canBeStored", stack); }

    private static ItemStack[] stored(GameTestHelper h, ItemStack box, int slots) {
        ItemStack[] out = new ItemStack[slots];
        AbstractElectricTool.loadHandHeldInventory(box, out, h.getLevel().registryAccess());
        return out;
    }

    /**
     * The menu the toolbox opens, made by its own provider (the test's mock player cannot be sent NeoForge's open-screen
     * packet, so {@code use} cannot open it here).
     */
    private static AbstractContainerMenu open(ServerPlayer player, ItemStack box) {
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        int slots = (Integer) Round33GameTests.call(box.getItem(), "slots");
        try {
            Object provider = Round33GameTests.common("item.tools.mio_icif_toolbox$Provider")
                .getConstructor(ItemStack.class, InteractionHand.class, int.class).newInstance(box, InteractionHand.MAIN_HAND, slots);
            return (AbstractContainerMenu) Round33GameTests.call(provider, "createMenu", 1, player.getInventory(), player);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @GameTest(template = "empty", batch = "round111170_toolbox")
    public static void bothToolboxesOpenTheirGui(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        AbstractContainerMenu small = open(player, new ItemStack(item("normal/item_toolbox")));
        h.assertTrue(small.getClass().getSimpleName().equals("mio_icif_toolbox_menu") && small.slots.size() == 9 + 36,
            "the toolbox opened " + small.getClass().getSimpleName() + " with " + small.slots.size() + " slots");
        AbstractContainerMenu big = open(player, new ItemStack(item("normal/item_toolbox_x")));
        h.assertTrue(big.getClass().getSimpleName().equals("mio_icif_toolbox_menu") && big.slots.size() == 27 + 36,
            "the advanced toolbox opened " + big.getClass().getSimpleName() + " with " + big.slots.size() + " slots");
        // three rows of nine, the player's inventory below them
        h.assertTrue(big.getSlot(26).y == 41 + 2 * 18 && big.getSlot(27).y == 84 + 2 * 18,
            "the advanced toolbox's last slot is at y " + big.getSlot(26).y + ", the inventory at " + big.getSlot(27).y);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111170_toolbox")
    public static void toolsGoInBlocksDoNot(GameTestHelper h) {
        for (ItemStack in : List.of(new ItemStack(item("item_tool_wrench")), new ItemStack(item("item_tool_cutter")),
                new ItemStack(item("build/item_painter_red")), new ItemStack(item("item_tool_iron_driller")),
                new ItemStack(item("normal/item_tesu_connector")), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.SHEARS)))
            h.assertTrue(fits(in), in + " does not go into a toolbox");
        for (ItemStack out : List.of(new ItemStack(Items.DIRT), new ItemStack(Items.SHULKER_BOX), new ItemStack(item("normal/item_toolbox")),
                new ItemStack(item("normal/item_toolbox_x")), new ItemStack(Items.DIAMOND)))
            h.assertTrue(!fits(out), out + " goes into a toolbox");
        h.succeed();
    }

    /** A wrench shift-clicked in stays in the advanced toolbox after the GUI is closed; dirt is refused. */
    @GameTest(template = "empty", batch = "round111170_toolbox")
    public static void theContentsStayWhenClosed(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack box = new ItemStack(item("normal/item_toolbox_x"));
        player.getInventory().setItem(9, new ItemStack(item("item_tool_wrench")));
        player.getInventory().setItem(10, new ItemStack(Items.DIRT, 5));
        AbstractContainerMenu menu = open(player, box);
        // the player's main inventory starts after the toolbox's 27 slots: inventory 9 is menu slot 27
        menu.quickMoveStack(player, 27);
        menu.quickMoveStack(player, 28);
        h.assertTrue(player.getInventory().getItem(9).isEmpty(), "the wrench stayed in the player's inventory");
        h.assertTrue(player.getInventory().getItem(10).getCount() == 5, "dirt went into the toolbox");
        menu.removed(player);   // closing the GUI
        ItemStack[] inside = stored(h, box, 27);
        h.assertTrue(inside[0].is(item("item_tool_wrench")), "after closing the toolbox holds " + inside[0]);
        h.succeed();
    }

    /** Toolbox + 5 carbon plates = advanced toolbox, with the toolbox's contents. */
    @GameTest(template = "empty", batch = "round111170_toolbox")
    public static void theAdvancedToolboxKeepsTheContents(GameTestHelper h) {
        var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("mio_icif", "shaped_157_toolbox_x")).orElse(null);
        h.assertTrue(holder != null && holder.value() instanceof CraftingRecipe, "no advanced toolbox recipe");
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        Item plate = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "plates/carbon")))
            .orElseThrow().get(0).value();
        ItemStack box = new ItemStack(item("normal/item_toolbox"));
        ItemStack[] contents = new ItemStack[9];
        java.util.Arrays.fill(contents, ItemStack.EMPTY);
        contents[0] = new ItemStack(item("item_tool_wrench"));
        AbstractElectricTool.saveHandHeldInventory(box, contents, h.getLevel().registryAccess());
        ItemStack p = new ItemStack(plate);
        CraftingInput input = CraftingInput.of(3, 2, List.of(p, box, p.copy(), p.copy(), p.copy(), p.copy()));
        h.assertTrue(recipe.matches(input, h.getLevel()), "toolbox + 5 carbon plates do not match");
        ItemStack result = recipe.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(result.is(item("normal/item_toolbox_x")), "the recipe makes " + result);
        h.assertTrue(stored(h, result, 27)[0].is(item("item_tool_wrench")), "the advanced toolbox lost the wrench");
        h.succeed();
    }

    /** A toolbox crafted into something else (not a toolbox) gives its contents to the player. */
    @GameTest(template = "empty", batch = "round111170_toolbox")
    public static void aToolboxUsedUpGivesItsContentsBack(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        ItemStack box = new ItemStack(item("normal/item_toolbox"));
        ItemStack[] contents = new ItemStack[9];
        java.util.Arrays.fill(contents, ItemStack.EMPTY);
        contents[3] = new ItemStack(item("item_tool_cutter"));
        AbstractElectricTool.saveHandHeldInventory(box, contents, h.getLevel().registryAccess());
        SimpleContainer grid = new SimpleContainer(9);
        grid.setItem(4, box);
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, new ItemStack(item("producer/block_industrial_workbench")), grid));
        h.assertTrue(player.getInventory().countItem(item("item_tool_cutter")) == 1, "the cutter in the used toolbox was lost");
        h.succeed();
    }

    /** The wrench takes a TESU casing apart (it drops the casing) and every part of mio_icif:wrenchable counts as wrenchable. */
    @GameTest(template = "empty", batch = "round111170_wrench")
    public static void theWrenchTakesCasingsApart(GameTestHelper h) {
        for (String id : new String[]{"wiring/block_tesu_casing", "wiring/block_tesu_cell", "reactor/block_reactor_vessel",
                "reactor/block_reactor_access_hatch", "reactor/block_reactor_fluid_port", "reactor/block_reactor_redstone_port",
                "producer/block_machine_hull_basic", "producer/block_machine_hull_advanced", "producer/block_material_interface",
                "generator/block_geomagnetic_antenna", "generator/block_geomagnetic_pedestal"}) {
            var state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)).defaultBlockState();
            h.assertTrue((Boolean) Round33GameTests.call(Round33GameTests.common("event.WrenchHarvest"), "wrenchable", state), id + " is not wrenchable");
        }
        h.assertTrue(!(Boolean) Round33GameTests.call(Round33GameTests.common("event.WrenchHarvest"), "wrenchable", Blocks.STONE.defaultBlockState()),
            "stone is wrenchable");
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/block_tesu_casing")));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack wrench = new ItemStack(item("item_tool_wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        BlockPos abs = h.absolutePos(at);
        player.gameMode.useItemOn(player, h.getLevel(), wrench, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        h.assertBlockPresent(Blocks.AIR, at);
        h.assertItemEntityPresent(item("wiring/block_tesu_casing"), at, 2.0);
        h.succeed();
    }
}
