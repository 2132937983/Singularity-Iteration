package com.singularity_iteration.mio_icif.Items.Armor.hazmat;

import com.singularity_iteration.mio_icif.api.armor.IHazmatLike;
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
 * 防化服头盔
 * 提供辐射防护功能
 */
@SuppressWarnings("null")
public class mio_icif_hazmat_helmet extends ArmorItem implements IHazmatLike {

    // 辐射防护等级�?-4�? 为完全防护）
    private static final int RADIATION_PROTECTION_LEVEL = 4;

    // 防化服护甲材质路径（头盔和胸甲使�?layer 1�
private static final ResourceLocation HAZMAT_ARMOR_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/hazmat_1.png");

    public mio_icif_hazmat_helmet(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    /**
     * 获取护甲材质
     * 使用自定义的防化服材料
 */
    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return HAZMAT_ARMOR_TEXTURE;
    }

    /**
     * 物品在物品栏中的更新
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // 只在服务端处理
    if (level.isClientSide || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        // 如果穿着整套防化服，提供完全辐射免疫
        if (isWearingFullHazmat(livingEntity)) {
            // 穿着整套时完全免疫辐�
        return;
        }
    }

    /**
     * 检查是否穿着整套防化学
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
     * 获取辐射防护等级
     */
    public static int getRadiationProtectionLevel() {
        return RADIATION_PROTECTION_LEVEL;
    }

    @Override
    public boolean providesHazmatProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        return true;
    }
}


