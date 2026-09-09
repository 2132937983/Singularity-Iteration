package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.ITreeTapItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 电动树脂提取器
 * 继承电力工具基础类，使用电力驱动
 * 属性与小型驱动把手看齐
 * 用于从橡胶木中提取树脂
 */
@SuppressWarnings("null")
public class mio_icif_treetap_elc extends mio_icif_tool_elc implements ITreeTapItem {

    // 电动树脂提取器默认最大能量（与小型驱动把手相同）
    public static final int TREETAP_ELC_MAX_ENERGY = 5000;

    // 电动树脂提取器每次使用消耗的能量
    public static final int TREETAP_ELC_ENERGY_PER_USE = 10;

    /**
     * 默认构造函数（空电状态）
     * @param properties 物品属性?
     */
    public mio_icif_treetap_elc(Properties properties) {
        super(properties, TREETAP_ELC_MAX_ENERGY, TREETAP_ELC_MAX_ENERGY, "treetap_elc", TREETAP_ELC_MAX_ENERGY, TREETAP_ELC_ENERGY_PER_USE, 1);
    }

    /**
     * 带初始能量的构造函数?
     * @param properties 物品属性?
     * @param initialEnergy 初始能量
     */
    public mio_icif_treetap_elc(Properties properties, int initialEnergy) {
        super(properties, TREETAP_ELC_MAX_ENERGY, TREETAP_ELC_MAX_ENERGY - initialEnergy, "treetap_elc", TREETAP_ELC_MAX_ENERGY, TREETAP_ELC_ENERGY_PER_USE, 1);
    }

    /**
     * 重写：防止电动树脂提取器损坏
     */
    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        // 电动树脂提取器不通过damageItem损坏，而是通过消耗能量?
        return 0;
    }

    /**
     * 重写：电动树脂提取器永远不会消失
     */
    @Override
    public boolean isDamaged(ItemStack stack) {
        // 当能量未满时返回true，显示耐久度信息?
        return getEnergy(stack) < getMaxEnergy();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // 用于扩展交互距离的 AttributeModifier ID (ResourceLocation)
    private static final ResourceLocation REACH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "treetap_elc_reach");

    /**
     * 当玩家手持电动木龙头时，增加方块交互距离
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;

        AttributeInstance reachAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (reachAttr == null) return;

        boolean hasModifier = reachAttr.getModifier(REACH_MODIFIER_ID) != null;

        if (isSelected) {
            // 手持电动木龙头时，添加 +2 格交互距离
            if (!hasModifier) {
                reachAttr.addTransientModifier(
                    new AttributeModifier(REACH_MODIFIER_ID, 2.0, AttributeModifier.Operation.ADD_VALUE)
                );
            }
        } else {
            // 未手持时，移除修饰符
            if (hasModifier) {
                reachAttr.removeModifier(REACH_MODIFIER_ID);
            }
        }
    }

}