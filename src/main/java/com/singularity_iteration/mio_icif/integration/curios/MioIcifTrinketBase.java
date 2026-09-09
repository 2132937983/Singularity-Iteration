package com.singularity_iteration.mio_icif.integration.curios;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_tool_elc;

@SuppressWarnings({"null", "deprecation"})
public abstract class MioIcifTrinketBase extends mio_icif_tool_elc implements ICurioItem {

    private final String slotIdentifier;

    public MioIcifTrinketBase(Properties properties, String slotIdentifier) {
        this(properties, DEFAULT_TOOL_MAX_ENERGY, 0, "trinket_elc", DEFAULT_TOOL_MAX_ENERGY, 100, 1, slotIdentifier);
    }

    public MioIcifTrinketBase(Properties properties, long maxEnergy, long initialEnergy, String slotIdentifier) {
        this(properties, maxEnergy, initialEnergy, "trinket_elc", maxEnergy, 100, 1, slotIdentifier);
    }

    public MioIcifTrinketBase(Properties properties, long maxEnergy, long initialEnergy, String texturePrefix, long chargeRate, long energyPerUse, int toolTier, String slotIdentifier) {
        super(properties, maxEnergy, initialEnergy, texturePrefix, chargeRate, energyPerUse, toolTier);
        this.slotIdentifier = slotIdentifier;
    }

    public String getSlotIdentifier() {
        return slotIdentifier;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals(slotIdentifier);
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.75f, 1.9f);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        entity.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.75f, 2.0f);
        onTrinketUnequipped(stack, entity);
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        entity.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.75f, 1.9f);
        onTrinketEquipped(stack, entity);
    }

    protected void onTrinketEquipped(ItemStack stack, LivingEntity entity) {}
    protected void onTrinketUnequipped(ItemStack stack, LivingEntity entity) {}
}