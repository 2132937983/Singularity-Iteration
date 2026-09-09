package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.IWeaponItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_rocket_launcher extends mio_icif_tool_elc implements IWeaponItem {
    public static final int MAX_ENERGY = 1000000;
    public static final int CHARGE_RATE = 512;
    public static final int TIER = 3;
    public static final int ENERGY_PER_SHOT = 50000;
    public static final int COOLDOWN_TICKS = 10;

    public static EntityType<mio_icif_rocket_entity> ROCKET_ENTITY;

    public static void setRocketEntity(EntityType<mio_icif_rocket_entity> entityType) {
        ROCKET_ENTITY = entityType;
    }

    public mio_icif_rocket_launcher(Properties properties) {
        super(properties, MAX_ENERGY, MAX_ENERGY, "rocket_launcher", CHARGE_RATE, 100, TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!hasEnoughEnergy(stack, ENERGY_PER_SHOT)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.rocket_launcher.no_energy"));
            }
            return InteractionResultHolder.fail(stack);
        }

        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty()) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.rocket_launcher.no_ammo"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            if (ROCKET_ENTITY == null) {
                return InteractionResultHolder.fail(stack);
            }

            consumeEnergy(stack, ENERGY_PER_SHOT);
            ammo.shrink(1);

            mio_icif_rocket_entity rocket = new mio_icif_rocket_entity(ROCKET_ENTITY, player, level);
            rocket.setDamage(0);
            rocket.setExplosionPower(5.0F);
            rocket.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 0.0F);
            level.addFreshEntity(rocket);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private ItemStack findAmmo(Player player) {
        if (mio_icif_items_tools.ROCKET.get() == null) return ItemStack.EMPTY;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHand.isEmpty() && mainHand.getItem() == mio_icif_items_tools.ROCKET.get()) return mainHand;

        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!offHand.isEmpty() && offHand.getItem() == mio_icif_items_tools.ROCKET.get()) return offHand;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == mio_icif_items_tools.ROCKET.get()) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.rocket_launcher.ammo"));
    }

    @Override
    public float getDamage() {
        return 0;
    }

    @Override
    public float getRange() {
        return 2.5F * 20;
    }

    @Override
    public long getEnergyPerShot() {
        return ENERGY_PER_SHOT;
    }

    @Override
    public boolean isRanged() {
        return true;
    }
}
