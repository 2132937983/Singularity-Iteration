package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
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

    /**
     * 0.1.10.36: the nano and quantum pieces take enchantments at the enchanting table. They have no durability, so the
     * vanilla rule (max damage needed) refused them; the pieces in {@code #minecraft:enchantable/armor} are enchantable. Which
     * enchantments apply comes from the item tags: these pieces are not in {@code enchantable/durability}, so they never get
     * Mending or Unbreaking (useless without durability). Jetpacks and energy packs are not in the tag and stay as before.
     */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return super.isEnchantable(stack) || stack.is(net.minecraft.tags.ItemTags.ARMOR_ENCHANTABLE);
    }

    /**
     * 0.1.8.0: armor sets that render a baked Blockbench model. The texture of a piece is
     * {@code textures/models/armor/<set>_<piece>.png}, the same file the baked model uses.
     * 0.1.8.3: the advanced quantum and heavy quantum chestplates joined.
     */
    public static final java.util.Set<String> BAKED_SETS = java.util.Set.of("quantum", "advanced_quantum", "heavy_quantum");

    /** Texture of a baked piece by its model name, for example {@code ultimate_solar_helmet} (0.1.8.3). */
    public static ResourceLocation bakedTexture(String model) {
        return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/models/armor/" + model + ".png");
    }

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        String suffix = (slot == EquipmentSlot.LEGS) ? "_2" : "_1";
        String prefix = getTexturePrefix();
        int slashIndex = prefix.indexOf('/');
        String path = slashIndex > 0 ? prefix.substring(slashIndex + 1) : prefix;
        if (BAKED_SETS.contains(path)) {
            String piece = switch (getType()) {
                case HELMET -> "helmet";
                case CHESTPLATE -> "chestplate";
                case LEGGINGS -> "leggings";
                default -> "boots";
            };
            // 0.1.10.30: the palette layers (plating, accent) of the quantum suit, the original texture without a palette
            return com.miophas.singularity_iteration.common.suit.SuitPalette.texture(stack, "textures/models/armor/" + path + "_" + piece,
                MioIcifArmorMaterials.isAccentLayer(layer));
        }
        return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/" + path + suffix + ".png");
    }

    /**
     * Never null: footstep/sound mods (Presence Footsteps etc.) call this unguarded. Charge changes
     * no longer replay it every tick — LivingEquipEnergyMixin ignores energy-only stack updates.
     */
    @Override
    public Holder<SoundEvent> getEquipSound() {
        Holder<SoundEvent> sound = super.getEquipSound();
        return sound != null ? sound : net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
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
            // 0.1.8.0: renew only when the effect is near its end. The old code renewed it
            // every tick: one effect packet and one advancement trigger per tick per player.
            MobEffectInstance current = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (current == null || current.getAmplifier() == 0 && !current.isInfiniteDuration() && current.getDuration() < 40) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false, false));
            }
        }
    }

    public boolean hasEnoughEnergy(ItemStack stack, long amount) {
        // 0.1.10.5: the cost after Energy Saving, as the debit takes it
        return getEnergy(stack) >= com.miophas.singularity_iteration.core.api.item.EnergySaving.maxCost(stack, amount);
    }

    public boolean consumeEnergy(ItemStack stack) {
        if (hasEnoughEnergy(stack)) {
            extractEnergy(stack, com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(stack, getEnergyPerTick()));
            return true;
        }
        return false;
    }
}