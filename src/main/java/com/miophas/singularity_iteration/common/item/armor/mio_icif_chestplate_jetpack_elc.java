// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.api.item.IBackSlotItem;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.runtime.flight.JetpackFlightController;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Independently implemented electric jetpack from frozen ABI and gameplay observations. */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_chestplate_jetpack_elc extends mio_icif_armor_elc
        implements IJetpackItem, IBackSlotItem {
    public static final int JETPACK_MAX_ENERGY = 30000;
    public static final int TRANSFER_LIMIT = 60;
    public static final int TIER = 1;
    public static final int ENERGY_PER_TICK = 2;
    public static final int HOVER_ENERGY_PER_TICK = 1;
    public static final float JETPACK_POWER = 0.7F;
    public static final double MAX_ASCENT_SPEED = 0.4D;
    public static final float HOVER_ASCENT_SPEED = 0.1F;
    public static final float HOVER_DESCENT_SPEED = -0.1F;
    public static final float WORLD_HEIGHT_DIVISOR = 1.28F;
    public static final float DROP_PERCENTAGE = 0.05F;
    public static final int MODE_JETPACK = 0;
    public static final int MODE_HOVER = 1;
    private static final String MODE_KEY = "JetpackMode";
    private static final String TOGGLE_TIMER_KEY = "JetpackToggleTimer";

    public mio_icif_chestplate_jetpack_elc(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, JETPACK_MAX_ENERGY, 0,
            "jetpack", TRANSFER_LIMIT, ENERGY_PER_TICK, TIER);
    }

    public int toggleMode(ItemStack stack) {
        int next = getModeInternal(stack) == MODE_HOVER ? MODE_JETPACK : MODE_HOVER;
        setModeInternal(stack, next);
        return next;
    }

    public int getToggleTimer(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.max(0, data.copyTag().getInt(TOGGLE_TIMER_KEY));
    }

    public void setToggleTimer(ItemStack stack, int timer) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TOGGLE_TIMER_KEY, Math.max(0, timer));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public Component getModeName(int mode) {
        return Component.translatable(mode == MODE_HOVER
            ? "hud.mio_icif.jetpack.mode_hover"
            : "hud.mio_icif.jetpack.mode_jetpack");
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (!(entity instanceof Player player) || player.getItemBySlot(EquipmentSlot.CHEST) != stack) return;
        tickFlight(player, stack);
    }

    @Override
    public void tickInBackSlot(Player player, ItemStack stack) {
        tickFlight(player, stack);
    }

    private void tickFlight(Player player, ItemStack stack) {
        JetpackFlightController.tick(player, stack, this, HOVER_ENERGY_PER_TICK,
            MAX_ASCENT_SPEED, HOVER_ASCENT_SPEED, HOVER_DESCENT_SPEED, false);
    }

    public static boolean isPlayerFlying(Player player) {
        return JetpackFlightController.isFlying(player);
    }

    public double getChargeLevel(ItemStack stack) {
        return getMaxEnergy(stack) <= 0 ? 0.0D : (double)getEnergy(stack) / getMaxEnergy(stack);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
            ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/jetpack_1.png");
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(new ArmorFeatureInfo(EquipmentSlot.CHEST, "jetpack_mode",
            "tooltip.mio_icif.armor.feature_jetpack_mode", true, getModeName(getModeInternal(stack))));
    }

    @Override public float getThrust() { return JETPACK_POWER; }
    @Override public long getEnergyPerTickFlying() { return ENERGY_PER_TICK; }
    @Override public float getMaxHeight() { return 310.0F; }

    @Override
    public JetpackMode getMode(ItemStack stack) {
        return getModeInternal(stack) == MODE_HOVER ? JetpackMode.HOVER : JetpackMode.FLIGHT;
    }

    @Override
    public void setMode(ItemStack stack, JetpackMode mode) {
        setModeInternal(stack, mode == JetpackMode.HOVER ? MODE_HOVER : MODE_JETPACK);
    }

    public int getModeInternal(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getInt(MODE_KEY) == MODE_HOVER ? MODE_HOVER : MODE_JETPACK;
    }

    public void setModeInternal(ItemStack stack, int mode) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MODE_KEY, mode == MODE_HOVER ? MODE_HOVER : MODE_JETPACK);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}