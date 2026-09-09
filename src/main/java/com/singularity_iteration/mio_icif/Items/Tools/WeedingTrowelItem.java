package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.IWeedingTrowelItem;

import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 除草�? * 用于清除作物架上的杂�? * 使用铁材质，具有铁工具的攻击伤害和攻击速度属性? */
@SuppressWarnings("null")
public class WeedingTrowelItem extends Item implements IWeedingTrowelItem {

    // 使用铁材质等�
public static final Tier TIER = Tiers.IRON;
    
    // 铁锄头属性参考：攻击伤害+1，攻击速度-2（即攻击速度�?.0�
public static final float ATTACK_DAMAGE_BONUS = 1.0F;
    public static final float ATTACK_SPEED = -2.0F;

    public WeedingTrowelItem(Properties properties) {
        super(properties
            .durability(TIER.getUses())
            .attributes(createAttributes(TIER, ATTACK_DAMAGE_BONUS, ATTACK_SPEED)));
    }
    
    /**
     * 创建工具属性（参考HoeItem.createAttributes�
 */
    public static ItemAttributeModifiers createAttributes(Tier tier, float attackDamageBonus, float attackSpeed) {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID, 
                    attackDamageBonus + tier.getAttackDamageBonus(), 
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID, 
                    attackSpeed, 
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IPlanter planter)) {
            return InteractionResult.PASS;
        }

        // 检查是否有杂草
        if (planter.getPlant() == null || !planter.getPlant().getTypeId().equals("weed")) {
            if (!level.isClientSide && player != null) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.weeding_trowel.no_weed"));
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            // 先获取杂草掉落物
            java.util.List<net.minecraft.world.item.ItemStack> drops = planter.doHarvest();

            // 掉落物品
            for (net.minecraft.world.item.ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                    level.addFreshEntity(itemEntity);
                }
            }

            // 清除杂草
            planter.reset();
            // 更新方块状态以同步客户端
        planter.updateState();

            // 消耗耐久（如果有耐久度）
            if (player != null && !player.getAbilities().instabuild) {
                context.getItemInHand().hurtAndBreak(1, player, null);
            }

            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.weeding_trowel.success"));
            }
        }

        return InteractionResult.SUCCESS;
    }
}


