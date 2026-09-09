package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.AbstractElectricArmor;
import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * mio_icif 内部电力护甲基类。
 * 继承 {@link AbstractElectricArmor}，添加 mio_icif 特有的逻辑：
 * <ul>
 *   <li>纳米胸甲（tier=5）穿戴时给予抗性效果</li>
 *   <li>使用内部 {@link ArmorFeatureToggle} 的 Tooltip 格式</li>
 *   <li>硬编码 mio_icif 材质命名空间</li>
 * </ul>
 *
 * @deprecated Addon 开发者应直接继承 {@link AbstractElectricArmor}，
 *             而不是此类。此类仅为 mio_icif 内部向后兼容保留。
 */
@Deprecated
@SuppressWarnings("null")
public class mio_icif_armor_elc extends AbstractElectricArmor {

    public static final long DEFAULT_ARMOR_MAX_ENERGY = 10000;

    public mio_icif_armor_elc(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties, DEFAULT_ARMOR_MAX_ENERGY, 0, DEFAULT_ARMOR_MAX_ENERGY, 1, 0, "mio_icif/armor_elc");
    }

    public mio_icif_armor_elc(Holder<ArmorMaterial> material, Type type, Properties properties, long initialEnergy) {
        super(material, type, properties, DEFAULT_ARMOR_MAX_ENERGY, initialEnergy, DEFAULT_ARMOR_MAX_ENERGY, 1, 0, "mio_icif/armor_elc");
    }

    public mio_icif_armor_elc(Holder<ArmorMaterial> material, Type type, Properties properties, long maxEnergy, long initialEnergy) {
        super(material, type, properties, maxEnergy, initialEnergy, maxEnergy, 1, 0, "mio_icif/armor_elc");
    }

    public mio_icif_armor_elc(Holder<ArmorMaterial> material, Type type, Properties properties, long maxEnergy, long initialEnergy, long energyPerTick) {
        super(material, type, properties, maxEnergy, initialEnergy, maxEnergy, energyPerTick, 0, "mio_icif/armor_elc");
    }

    public mio_icif_armor_elc(Holder<ArmorMaterial> material, Type type, Properties properties, long maxEnergy, long initialEnergy, String texturePrefix, long chargeRate, long energyPerTick, int armorTier) {
        super(material, type, properties, maxEnergy, initialEnergy, chargeRate, energyPerTick, armorTier, "mio_icif/" + texturePrefix);
    }

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        String suffix = (slot == EquipmentSlot.LEGS) ? "_2" : "_1";
        String prefix = getTexturePrefix();
        int slashIndex = prefix.indexOf('/');
        String path = slashIndex > 0 ? prefix.substring(slashIndex + 1) : prefix;
        return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/" + path + suffix + ".png");
    }

    @Override
    public @Nullable Holder<SoundEvent> getEquipSound() {
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    protected void appendFeatureTooltip(ItemStack stack, List<Component> tooltipComponents) {
        List<ArmorFeatureInfo> features = getFeatures(stack);
        List<ArmorFeatureToggle.FeatureInfo> internalFeatures = features.stream()
            .map(ArmorFeatureToggle::fromApi)
            .toList();
        ArmorFeatureToggle.addFeaturesToTooltip(tooltipComponents, stack, internalFeatures);
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return Collections.emptyList();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        EquipmentSlot slot = getEquipmentSlot();
        boolean isWearing = player.getItemBySlot(slot) == stack;

        if (!isWearing) {
            return;
        }

        if (!isEmpty(stack) && getArmorTier() >= 5 && getArmorTier() < 6 && slot == EquipmentSlot.CHEST) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false, false));
        }
    }

    public boolean hasEnoughEnergy(ItemStack stack, long amount) {
        return getEnergy(stack) >= amount;
    }

    public boolean consumeEnergy(ItemStack stack) {
        if (hasEnoughEnergy(stack)) {
            extractEnergy(stack, getEnergyPerTick());
            return true;
        }
        return false;
    }
}