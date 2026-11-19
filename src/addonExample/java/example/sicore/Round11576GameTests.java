package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.76: the reactor config card. Import writes the reactor layout on the card; export makes it the guide of another
 * reactor (saved with the reactor); fill takes one matching part per empty guided slot from the player, by item id only
 * (a used part counts and keeps its data), leaves occupied slots alone and reports what is missing; Shift-export clears
 * the guide. The card slot is the last menu slot on both reactor screens, takes only a card, and gives the card back
 * when the screen closes. Sneak + use clears a card.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11576GameTests {
    private Round11576GameTests() {}

    private static final int IMPORT = 20, EXPORT = 21, CLEAR = 22, FILL = 23;

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static ItemStack part(String suffix) { return item("reactor/item_reactor_" + suffix); }
    private static ItemStack card() { return item("reactor/item_reactor_config_card"); }

    private static IReactorController reactor(GameTestHelper h, BlockPos pos, String block) {
        h.setBlock(pos, block(block));
        return (IReactorController) h.getBlockEntity(pos);
    }

    private static AbstractContainerMenu open(IReactorController reactor, ServerPlayer player, int id) {
        return ((MenuProvider) reactor).createMenu(id, player.getInventory(), player);
    }

    private static int cardSlot(AbstractContainerMenu menu) { return menu.slots.size() - 1; }

    private static void insert(GameTestHelper h, IReactorController reactor, int slot, ItemStack stack) {
        h.assertTrue(reactor.getItemHandler().insertItem(slot, stack, false).isEmpty(), "Rejected reactor slot " + slot);
    }

    /** The layout on a card as slot -> item id ("" = none), read from the saved component. */
    private static String[] layout(GameTestHelper h, ItemStack card) {
        String[] out = new String[54];
        java.util.Arrays.fill(out, "");
        CompoundTag tag = (CompoundTag) card.save(h.getLevel().registryAccess());
        for (Tag raw : tag.getCompound("components").getList("mio_icif:reactor_layout", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) raw;
            out[e.getInt("slot")] = e.getString("item");
        }
        return out;
    }

    /** A vent that has taken heat (its reactor component data differs from a fresh vent). */
    private static ItemStack hotVent(GameTestHelper h, int heat) {
        CompoundTag tag = (CompoundTag) part("vent").save(h.getLevel().registryAccess());
        CompoundTag components = tag.getCompound("components");
        CompoundTag data = new CompoundTag();
        data.putInt("stored_value", heat); data.putInt("max_value", 1000);
        components.put("mio_icif:reactor_component_data", data); tag.put("components", components);
        return ItemStack.parse(h.getLevel().registryAccess(), tag).orElseThrow();
    }

    private static int stored(GameTestHelper h, ItemStack stack) {
        return ((CompoundTag) stack.save(h.getLevel().registryAccess())).getCompound("components")
            .getCompound("mio_icif:reactor_component_data").getInt("stored_value");
    }

    private static int count(ServerPlayer player, ItemStack like) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(like.getItem())) n += player.getInventory().getItem(i).getCount();
        return n;
    }

    @GameTest(template = "empty", batch = "round11576_card", timeoutTicks = 40)
    public static void importExportAndFill(GameTestHelper h) {
        IReactorController source = reactor(h, new BlockPos(1, 1, 1), "generator/block_nuclear_reactor_generator");
        IReactorController target = reactor(h, new BlockPos(4, 1, 1), "generator/block_nuclear_reactor_generator");
        insert(h, source, 1, part("uranium_simple"));
        for (int slot : new int[] {0, 2, 10}) insert(h, source, slot, part("vent"));
        ServerPlayer player = h.makeMockServerPlayerInLevel();

        AbstractContainerMenu a = open(source, player, 1);
        int cardIndex = cardSlot(a);
        h.assertTrue(a.getSlot(cardIndex).mayPlace(card()) && !a.getSlot(cardIndex).mayPlace(part("vent")), "The card slot must take only a config card");
        h.assertTrue(a.clickMenuButton(player, IMPORT), "Import without a card must still answer");
        a.getSlot(cardIndex).set(card());
        h.assertTrue(a.clickMenuButton(player, IMPORT), "Import failed");
        String[] written = layout(h, a.getSlot(cardIndex).getItem());
        h.assertTrue(written[1].equals("mio_icif:reactor/item_reactor_uranium_simple") && written[0].equals("mio_icif:reactor/item_reactor_vent")
            && written[2].equals(written[0]) && written[10].equals(written[0]) && written[3].isEmpty(), "Wrong layout on the card: " + String.join(",", written));
        a.removed(player);
        h.assertTrue(a.getSlot(cardIndex).getItem().isEmpty() && count(player, card()) == 1, "The card did not go back to the player");

        AbstractContainerMenu b = open(target, player, 2);
        int from = -1;
        for (int i = 0; i < cardSlot(b); i++) if (b.getSlot(i).container == player.getInventory() && b.getSlot(i).getItem().is(card().getItem())) from = i;
        h.assertTrue(from >= 0, "The written card is not in the player inventory");
        b.quickMoveStack(player, from);
        h.assertTrue(!b.getSlot(cardSlot(b)).getItem().isEmpty(), "Shift-click did not put the card into the card slot");
        h.assertTrue(b.clickMenuButton(player, EXPORT), "Export failed");

        // the guide is saved with the reactor
        CompoundTag saved = ((BlockEntity) target).saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.getList("scex_layout_guide", Tag.TAG_COMPOUND).size() == 4, "The guide is not saved: " + saved.get("scex_layout_guide"));

        // one used part per kind is enough; the third vent is missing
        player.getInventory().add(part("uranium_simple"));
        player.getInventory().add(hotVent(h, 37));
        player.getInventory().add(part("vent"));
        h.assertTrue(b.clickMenuButton(player, FILL), "Fill failed");
        var slots = target.getItemHandler();
        h.assertTrue(slots.getStackInSlot(1).is(part("uranium_simple").getItem()), "The rod was not placed");
        int vents = 0, heat = 0;
        for (int slot : new int[] {0, 2, 10}) if (slots.getStackInSlot(slot).is(part("vent").getItem())) { vents++; heat += stored(h, slots.getStackInSlot(slot)); }
        h.assertTrue(vents == 2 && heat == 37, "Two vents (one of them hot, kept as it was) must be placed: " + vents + " / " + heat);
        h.assertTrue(count(player, part("uranium_simple")) == 0 && count(player, part("vent")) == 0, "The parts did not leave the player");

        // an occupied guided slot is left alone; the free one fills later
        int empty = slots.getStackInSlot(10).isEmpty() ? 10 : slots.getStackInSlot(2).isEmpty() ? 2 : 0;
        insert(h, target, empty, part("uranium_simple"));
        player.getInventory().add(part("vent"));
        b.clickMenuButton(player, FILL);
        h.assertTrue(slots.getStackInSlot(empty).is(part("uranium_simple").getItem()) && count(player, part("vent")) == 1,
            "Fill replaced a part or took a vent with no free guided slot");

        // Shift-export clears the guide: nothing more is placed
        slots.extractItem(empty, 1, false);
        h.assertTrue(b.clickMenuButton(player, CLEAR), "Clear failed");
        b.clickMenuButton(player, FILL);
        h.assertTrue(slots.getStackInSlot(empty).isEmpty() && count(player, part("vent")) == 1, "Fill worked after the guide was cleared");
        CompoundTag cleared = ((BlockEntity) target).saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(!cleared.contains("scex_layout_guide"), "A cleared guide is still saved");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11576_saved", timeoutTicks = 40)
    public static void savedGuideFillsAfterLoad(GameTestHelper h) {
        IReactorController first = reactor(h, new BlockPos(1, 1, 1), "generator/block_nuclear_reactor_generator");
        insert(h, first, 19, part("uranium_simple"));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        AbstractContainerMenu menu = open(first, player, 1);
        menu.getSlot(cardSlot(menu)).set(card());
        menu.clickMenuButton(player, IMPORT);
        ItemStack written = menu.getSlot(cardSlot(menu)).getItem().copy();
        // the card keeps its layout through the item save codec
        ItemStack parsed = ItemStack.parse(h.getLevel().registryAccess(), written.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(ItemStack.isSameItemSameComponents(written, parsed), "The card layout does not survive a save");

        IReactorController second = reactor(h, new BlockPos(4, 1, 1), "generator/block_nuclear_reactor_generator");
        AbstractContainerMenu other = open(second, player, 2);
        other.getSlot(cardSlot(other)).set(written);
        other.clickMenuButton(player, EXPORT);
        CompoundTag tag = ((BlockEntity) second).saveWithoutMetadata(h.getLevel().registryAccess());
        IReactorController third = reactor(h, new BlockPos(7, 1, 1), "generator/block_nuclear_reactor_generator");
        ((BlockEntity) third).loadWithComponents(tag, h.getLevel().registryAccess());
        player.getInventory().add(part("uranium_simple"));
        open(third, player, 3).clickMenuButton(player, FILL);
        h.assertTrue(third.getItemHandler().getStackInSlot(19).is(part("uranium_simple").getItem()), "A loaded guide does not fill");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11576_steam", timeoutTicks = 40)
    public static void steamReactorScreenHasTheCard(GameTestHelper h) {
        IReactorController steam = reactor(h, new BlockPos(1, 1, 1), "generator/block_steam_reactor");
        insert(h, steam, 9, part("uranium_simple"));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        AbstractContainerMenu menu = open(steam, player, 1);
        int cardIndex = cardSlot(menu);
        h.assertTrue(menu.getSlot(cardIndex).mayPlace(card()), "The steam reactor screen has no card slot");
        player.getInventory().add(card());
        int from = -1;
        for (int i = 0; i < cardIndex; i++) if (menu.getSlot(i).container == player.getInventory() && menu.getSlot(i).getItem().is(card().getItem())) from = i;
        menu.quickMoveStack(player, from);
        menu.clickMenuButton(player, IMPORT);
        h.assertTrue(layout(h, menu.getSlot(cardIndex).getItem())[9].equals("mio_icif:reactor/item_reactor_uranium_simple"), "Steam reactor import failed");
        menu.quickMoveStack(player, cardIndex);
        h.assertTrue(menu.getSlot(cardIndex).getItem().isEmpty() && count(player, card()) == 1, "Shift-click did not take the card out");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11576_clear", timeoutTicks = 20)
    public static void sneakUseClearsTheCard(GameTestHelper h) {
        IReactorController reactor = reactor(h, new BlockPos(1, 1, 1), "generator/block_nuclear_reactor_generator");
        insert(h, reactor, 0, part("vent"));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        AbstractContainerMenu menu = open(reactor, player, 1);
        menu.getSlot(cardSlot(menu)).set(card());
        menu.clickMenuButton(player, IMPORT);
        ItemStack written = menu.getSlot(cardSlot(menu)).getItem().copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, written);
        written.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(!layout(h, player.getMainHandItem())[0].isEmpty(), "Use without sneaking cleared the card");
        player.setShiftKeyDown(true);
        player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(layout(h, player.getMainHandItem())[0].isEmpty() && ItemStack.isSameItemSameComponents(player.getMainHandItem(), card()),
            "Sneak + use did not clear the card");
        h.succeed();
    }
}
