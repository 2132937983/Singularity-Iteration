package com.miophas.singularity_iteration.common.item.armor.hazmat;

import com.miophas.singularity_iteration.core.api.armor.IHazmatLike;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Hazmat chestplate.
 * It gives radiation protection.
 */
@SuppressWarnings("null")
public class mio_icif_hazmat_chestplate extends ArmorItem implements IHazmatLike {

    // Radiation protection level (0-4, 4 is full protection).
    private static final int RADIATION_PROTECTION_LEVEL = 4;

    // Hazmat armor texture path. The helmet and the chestplate use layer 1.
    private static final ResourceLocation HAZMAT_ARMOR_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/hazmat_1.png");

    public mio_icif_hazmat_chestplate(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }

    /**
     * Returns the armor texture.
     * It uses the custom hazmat texture.
     */
    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return HAZMAT_ARMOR_TEXTURE;
    }

    /**
     * Updates the item while it is in an inventory.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Process only on the server.
        if (level.isClientSide || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        // Check if the entity wears a full hazmat suit.
        if (isWearingFullHazmat(livingEntity)) {
            // A full hazmat suit gives full radiation immunity.
            return;
        }
    }

    /**
     * Checks if the player wears a full hazmat suit.
     */
    private boolean isWearingFullHazmat(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);

        return helmet.getItem() instanceof mio_icif_hazmat_helmet &&
               chestplate.getItem() instanceof mio_icif_hazmat_chestplate &&
               leggings.getItem() instanceof mio_icif_hazmat_leggings &&
               boots.getItem() instanceof mio_icif_hazmat_boots;
    }

    /**
     * Gets the radiation protection level.
     */
    public static int getRadiationProtectionLevel() {
        return RADIATION_PROTECTION_LEVEL;
    }

    @Override
    public boolean providesHazmatProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        return true;
    }
}


