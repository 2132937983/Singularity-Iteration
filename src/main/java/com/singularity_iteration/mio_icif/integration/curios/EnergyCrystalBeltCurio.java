package com.singularity_iteration.mio_icif.integration.curios;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

public class EnergyCrystalBeltCurio extends MioIcifTrinketBase {

    private static final int STORAGE_ENERGY = 2000000;
    private static final int TRANSFER_SPEED = 512;
    private static final int TIER = 3;

    public EnergyCrystalBeltCurio() {
        super(new Item.Properties().stacksTo(1), STORAGE_ENERGY, 0, "energy_crystal_belt", STORAGE_ENERGY, 0, TIER, "belt");
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player) || entity.level().isClientSide()) return;

        long worldTime = entity.level().getGameTime();
        if (worldTime % 10L >= TIER) return;

        Inventory inv = player.getInventory();
        long limit = TRANSFER_SPEED;

        for (int i = 0; i < 9 && limit > 0; i++) {
            ItemStack toCharge = inv.getItem(i);
            if (toCharge.isEmpty()) continue;

            long chargeAmount = 0;
            var api = MioIcifAPI.instance().getItemAPI();
            if (api.isBattery(toCharge) && !api.isBatteryFull(toCharge)) {
                chargeAmount = Math.min(limit, api.getBatteryCapacity(toCharge) - api.getBatteryStored(toCharge));
            } else if (api.isElectricArmor(toCharge) && api.getElectricArmorStored(toCharge) < api.getElectricArmorMaxEnergy(toCharge)) {
                chargeAmount = Math.min(limit, api.getElectricArmorMaxEnergy(toCharge) - api.getElectricArmorStored(toCharge));
            }

            if (chargeAmount > 0) {
                long discharged = extractEnergy(stack, chargeAmount);
                if (api.isBattery(toCharge)) {
                    api.chargeBattery(toCharge, discharged, false);
                } else if (api.isElectricArmor(toCharge)) {
                    api.chargeElectricArmor(toCharge, discharged, false);
                }
                limit -= discharged;
            }
        }
    }
}