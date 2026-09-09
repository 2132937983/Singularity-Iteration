package com.singularity_iteration.mio_icif.Items.Normal;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;



@SuppressWarnings("null")
public class mio_icif_scrapbox extends Item {

    private static class LootEntry {
        public final ItemStack item;
        public final double weight;

        public LootEntry(ItemStack item, double weight) {
            this.item = item;
            this.weight = weight;
        }
    }

    private static final List<LootEntry> lootTable = new ArrayList<>();
    private static double totalWeight = 0.0;

    static {
        addEntry(Items.BLAZE_ROD, 0.08);
        addEntry(Items.NETHERRACK, 3.87);
        addEntry(Items.COOKED_PORKCHOP, 1.74);
        addEntry(Items.GOLDEN_HELMET, 0.02);
        addEntry(Items.WOODEN_SHOVEL, 1.93);
        addEntry(Items.CAKE, 0.97);
        addEntry(Items.LEATHER, 1.93);
        addEntry(Items.APPLE, 2.90);
        addEntry(Items.IRON_ORE, 0.97);
        addEntry(Items.OAK_SIGN, 1.93);
        addEntry(Items.WOODEN_SWORD, 1.93);
        addEntry(Items.COOKED_BEEF, 1.74);
        addEntry(Items.COAL, 1.55);
        addEntry(Items.DIAMOND, 0.19);
        addEntry(Items.BONE, 1.93);
        addEntry(Items.ENDER_PEARL, 0.15);
        addEntry(Items.REDSTONE, 1.74);
        addEntry(Items.MINECART, 0.02);
        addEntry(Items.DIRT, 9.67);
        addEntry(Items.BREAD, 2.90);
        addEntry(Items.STICK, 7.74);
        addEntry(Items.WOODEN_PICKAXE, 1.93);
        addEntry(Items.ROTTEN_FLESH, 3.87);
        addEntry(Items.GRASS_BLOCK, 5.80);
        addEntry(Items.COOKED_CHICKEN, 1.74);
        addEntry(Items.GOLD_ORE, 0.97);
        addEntry(Items.GLOWSTONE_DUST, 1.55);
        addEntry(Items.EMERALD, 0.10);
        addEntry(Items.PUMPKIN, 1.74);
        addEntry(Items.GRAVEL, 5.80);
        addEntry(Items.WOODEN_HOE, 9.69);
        addEntry(Items.SLIME_BALL, 1.16);
        addEntry(Items.FEATHER, 1.93);
        addEntry(Items.EGG, 1.55);
        addEntry(Items.SOUL_SAND, 1.93);
    }

    private static void addEntry(Item item, double weight) {
        lootTable.add(new LootEntry(new ItemStack(item), weight));
        totalWeight += weight;
    }

    public mio_icif_scrapbox(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ItemStack loot = getRandomLoot(level.random);
            if (loot.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.scrapbox.empty"));
            } else {
                if (!player.getInventory().add(loot)) {
                    player.drop(loot, false);
                }
                player.sendSystemMessage(Component.translatable("message.mio_icif.scrapbox.got", loot.getDisplayName()));
            }

            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public ItemStack getRandomLoot(RandomSource random) {
        double roll = random.nextDouble() * totalWeight;
        double current = 0.0;

        for (LootEntry entry : lootTable) {
            current += entry.weight;
            if (roll <= current) {
                return entry.item.copy();
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public int getBurnTime(ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType) {
        return 1600;
    }

    public static void registerDispenserBehavior() {
        DispenserBlock.registerBehavior(mio_icif_normal.SCRAPBOX.get(), new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                mio_icif_scrapbox scrapbox = (mio_icif_scrapbox) stack.getItem();
                ItemStack loot = scrapbox.getRandomLoot(source.level().random);
                
                if (!loot.isEmpty()) {
                    // 发射物品
                    return loot;
                } else {
                    return stack;
                }
            }
        });
    }
}

