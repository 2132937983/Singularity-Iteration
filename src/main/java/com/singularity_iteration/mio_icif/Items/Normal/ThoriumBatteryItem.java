package com.singularity_iteration.mio_icif.Items.Normal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ThoriumBatteryItem extends mio_icif_bat {
    private static final int AUTO_CHARGE_RATE = 9;

    public ThoriumBatteryItem() {
        super(new Properties(), Integer.MAX_VALUE, Integer.MAX_VALUE, "item_thorium_battery", AUTO_CHARGE_RATE);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;
        if (level.getGameTime() % 20 != 0) return; // 每20tick(1秒)执行一次，而不是每tick
        if (getEnergy(stack) < getMaxEnergy()) {
            addEnergy(stack, AUTO_CHARGE_RATE * 20); // 补偿：每次充20倍的量
            // 只在玩家打开容器界面时才广播变化，避免每tick发送网络包
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
        }
    }
}