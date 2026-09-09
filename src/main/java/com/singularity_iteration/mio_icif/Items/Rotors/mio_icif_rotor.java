package com.singularity_iteration.mio_icif.Items.Rotors;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

@SuppressWarnings("null")
public class mio_icif_rotor extends Item implements com.singularity_iteration.mio_icif.api.item.IKineticRotor {

    private final int maxDurability;
    private final int diameter;
    private final float efficiency;
    private final int minWindStrength;
    private final int maxWindStrength;
    private final ResourceLocation renderTexture;
    private final boolean waterCompatible;

    public mio_icif_rotor(Properties properties, int maxDurability, int diameter,
                          float efficiency, int minWindStrength, int maxWindStrength,
                          ResourceLocation renderTexture, boolean waterCompatible) {
        super(properties.durability(maxDurability));
        this.maxDurability = maxDurability;
        this.diameter = diameter;
        this.efficiency = efficiency;
        this.minWindStrength = minWindStrength;
        this.maxWindStrength = maxWindStrength;
        this.renderTexture = renderTexture;
        this.waterCompatible = waterCompatible;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getDiameter(ItemStack stack) {
        return diameter;
    }

    public float getEfficiency(ItemStack stack) {
        return efficiency;
    }

    public int getMinWindStrength(ItemStack stack) {
        return minWindStrength;
    }

    public int getMaxWindStrength(ItemStack stack) {
        return maxWindStrength;
    }

    public ResourceLocation getRenderTexture(ItemStack stack) {
        return renderTexture;
    }

    @Override
    public ResourceLocation getRotorRenderTexture(ItemStack stack) {
        return renderTexture;
    }

    @Override
    public boolean isAcceptedType(ItemStack stack, com.singularity_iteration.mio_icif.api.item.IKineticRotor.GearboxType type) {
        return type == com.singularity_iteration.mio_icif.api.item.IKineticRotor.GearboxType.WATER ? waterCompatible : true;
    }

    public boolean isWaterCompatible() {
        return waterCompatible;
    }

    public int getDurability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    public boolean isDamaged(ItemStack stack) {
        return stack.getDamageValue() > 0;
    }

    public void damageRotor(ItemStack stack, int amount) {
        if (stack.isEmpty()) return;

        int unbreakingLevel = getUnbreakingLevel(stack);
        int effectiveDamage = applyUnbreaking(stack, amount, unbreakingLevel);

        if (effectiveDamage <= 0) return;

        stack.setDamageValue(stack.getDamageValue() + effectiveDamage);

        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            stack.shrink(1);
        }
    }

    private int getUnbreakingLevel(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            var key = entry.getKey().unwrapKey();
            if (key.isPresent() && key.get().location().getPath().equals("unbreaking")) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private int applyUnbreaking(ItemStack stack, int amount, int unbreakingLevel) {
        if (unbreakingLevel <= 0) return amount;

        int effectiveDamage = 0;
        for (int i = 0; i < amount; i++) {
            if (!stack.isDamageableItem()) break;

            if (net.minecraft.util.RandomSource.create().nextInt(unbreakingLevel + 1) > 0) {
                continue;
            }
            effectiveDamage++;
        }
        return effectiveDamage;
    }

    public boolean hasMending(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            var key = entry.getKey().unwrapKey();
            if (key.isPresent() && key.get().location().getPath().equals("mending")) {
                return true;
            }
        }
        return false;
    }

    public void applyMending(ItemStack stack, int xpAmount) {
        if (!hasMending(stack) || !stack.isDamaged()) return;

        int damageToRepair = xpAmount * 2;
        int currentDamage = stack.getDamageValue();
        int newDamage = Math.max(0, currentDamage - damageToRepair);
        stack.setDamageValue(newDamage);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }
}
