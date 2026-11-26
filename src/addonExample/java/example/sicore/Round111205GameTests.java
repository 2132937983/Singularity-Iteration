package example.sicore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.205: what a player puts into a machine slot by hand comes out by hand. A Mekanism creative fluid tank put into the
 * Diesel Generator's fuel slot stayed there for ever: the screen's slots went through the automation rules (the fuel slot
 * gives nothing out, only the empty-container slot does), and a container that never empties never moved on.
 *
 * <p>The audit places every block of the mod that has a screen, opens it for a player, and for each machine slot puts in the
 * first of a set of typical items that the slot takes (fluid buckets, cells, fuels, batteries, upgrades, reactor parts...);
 * the player must be able to take it back out.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111205GameTests {
    private Round111205GameTests() {}

    /** Items that a machine slot is likely to take: fluid containers, fuels, batteries, upgrades, parts. */
    private static final Pattern CANDIDATE = Pattern.compile(
        "bucket|cell|battery|crystal|upgrade|rod|rotor|scrap|fuel|pellet|vent|plating|reflector|condensator|blade|drill|coil|"
        + "dust|ingot|plate|seed|resin|harz|circuit|pattern|card|schematic|lens|filter|amplifier|uu|matter|kit|turbine");

    /**
     * Slots that keep what goes in on purpose: the barrel's water bucket is used up by the brewing, and the sorter gives its
     * Transformer Upgrade back only while the safety key is held (taking it out would drop its voltage under load).
     */
    private static final java.util.Set<String> ON_PURPOSE = java.util.Set.of("producer/block_barrel 2",
        "producer/block_sorter_elc 1", "producer/block_sorter_elc 2", "producer/block_sorter_elc 3");

    private static List<ItemStack> candidates() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : new Item[]{Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.BUCKET, Items.COAL, Items.IRON_INGOT, Items.COBBLESTONE,
                Items.REDSTONE, Items.OAK_LOG, Items.WHEAT_SEEDS, Items.IRON_ORE, Items.SAND, Items.BONE_MEAL, Items.GLASS_BOTTLE})
            out.add(new ItemStack(item));
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if ("mio_icif".equals(id.getNamespace()) && CANDIDATE.matcher(id.getPath()).find()) out.add(new ItemStack(item));
        }
        return out;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void whatGoesInByHandComesOutByHand(GameTestHelper h) {
        ServerPlayer player = ArmoryServiceGameTests.player(h);
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        List<ItemStack> candidates = candidates();
        List<String> wrong = new ArrayList<>();
        int machines = 0, slots = 0;
        try {
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (!"mio_icif".equals(id.getNamespace()) || !(block instanceof EntityBlock)) continue;
                try {
                    h.getLevel().setBlock(at, block.defaultBlockState(), Block.UPDATE_ALL);
                    BlockEntity be = h.getLevel().getBlockEntity(at);
                    if (!(be instanceof MenuProvider provider)) continue;
                    AbstractContainerMenu menu;
                    try {
                        menu = provider.createMenu(1, player.getInventory(), player);
                    } catch (RuntimeException e) {
                        continue;   // a screen that needs more than the block (a formed structure...)
                    }
                    if (menu == null) continue;
                    machines++;
                    for (Slot slot : menu.slots) {
                        if (slot.container == player.getInventory() || slot.hasItem()) continue;
                        for (ItemStack candidate : candidates) {
                            ItemStack one = candidate.copy();
                            if (!slot.mayPlace(one)) continue;
                            slot.set(one);
                            if (!slot.hasItem()) break;
                            slots++;
                            if (!slot.mayPickup(player) && !ON_PURPOSE.contains(id.getPath() + " " + slot.getContainerSlot()))
                                wrong.add(id.getPath() + " slot " + slot.getContainerSlot() + " (" + BuiltInRegistries.ITEM.getKey(one.getItem()).getPath() + ")");
                            slot.set(ItemStack.EMPTY);
                            break;
                        }
                    }
                } catch (RuntimeException e) {
                    // a block that cannot stand alone: not a hand-slot question
                } finally {
                    h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        } finally {
            player.discard();
        }
        h.assertTrue(machines > 50 && slots > 100, "the audit reached only " + machines + " screens and " + slots + " slots");
        h.assertTrue(wrong.isEmpty(), wrong.size() + " slots take an item by hand but do not give it back: " + String.join("; ", wrong));
        h.succeed();
    }
}
