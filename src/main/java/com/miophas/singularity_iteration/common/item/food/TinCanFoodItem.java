package com.miophas.singularity_iteration.common.item.food;

import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Filled tin can (IC2 canned food).
 *
 * <ul>
 *   <li>Eaten in {@value #EAT_TICKS} ticks (0.6 s) instead of the 32 ticks of ordinary food.</li>
 *   <li>Each can restores 2 hunger (one shank, IC2 classic) and 3.2 saturation (IC2). The canner
 *       makes one can per nutrition point of the food it was filled with, so a can stack is worth
 *       twice the food's hunger plus far more saturation.</li>
 *   <li>The empty tin can comes back: into the hand when it was the last can, otherwise into the
 *       inventory, or dropped at the player's feet when the inventory is full.</li>
 * </ul>
 */
public class TinCanFoodItem extends Item {
    public static final int EAT_TICKS = 12;
    public static final int NUTRITION = 2;
    public static final float SATURATION = 3.2F;
    public static final FoodProperties FOOD =
        new FoodProperties(NUTRITION, SATURATION, false, EAT_TICKS / 20F, Optional.empty(), List.of());

    public TinCanFoodItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return EAT_TICKS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        boolean creative = entity instanceof Player player && player.getAbilities().instabuild;
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof Player player) || creative) return rest;
        ItemStack empty = new ItemStack(mio_icif_normal.TIN_EMPTY_CAN.get());
        if (rest.isEmpty()) return empty;                 // last can: the empty one stays in hand
        if (!level.isClientSide && !player.getInventory().add(empty)) player.drop(empty, false);
        return rest;
    }
}
