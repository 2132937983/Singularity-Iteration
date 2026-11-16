package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.ITreeTapItem;
import net.minecraft.resources.ResourceLocation;
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
     * Default constructor. The item has no energy.
     * @param properties the item properties
     */
    public mio_icif_treetap_elc(Properties properties) {
        super(properties, TREETAP_ELC_MAX_ENERGY, TREETAP_ELC_MAX_ENERGY, "treetap_elc", TREETAP_ELC_MAX_ENERGY, TREETAP_ELC_ENERGY_PER_USE, 1);
    }

    /**
     * Constructor with initial energy.
     * @param properties the item properties
     * @param initialEnergy the initial energy
     */
    public mio_icif_treetap_elc(Properties properties, int initialEnergy) {
        super(properties, TREETAP_ELC_MAX_ENERGY, TREETAP_ELC_MAX_ENERGY - initialEnergy, "treetap_elc", TREETAP_ELC_MAX_ENERGY, TREETAP_ELC_ENERGY_PER_USE, 1);
    }

    /**
     * 电动树脂提取器不消耗耐久，改为消耗电能。
     */
    @Override
    public int getDurabilityCost() {
        return 0;
    }

    /**
     * 电动树脂提取器效率更高，每次掉落 2~4 个树脂。
     */
    @Override
    public int getResinDropMin() {
        return 2;
    }

    @Override
    public int getResinDropMax() {
        return 4;
    }

    /**
     * Override. Prevents damage to the electric treetap.
     */
    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        // The electric treetap does not take damage through damageItem. It uses energy instead.
        return 0;
    }

    /**
     * Override. The electric treetap never breaks.
     */
    @Override
    public boolean isDamaged(ItemStack stack) {
        // Return true when the energy is not full. This shows the durability information.
        return getEnergy(stack) < getMaxEnergy();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // 用于扩展交互距离的 AttributeModifier ID (ResourceLocation)
    private static final ResourceLocation REACH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "treetap_elc_reach");

    /**
     * 0.1.11.3: +2 block interaction range in the main hand, as an item attribute modifier that vanilla adds and removes
     * when the hand changes. It used to be added and removed from inventoryTick, so a treetap dropped with Q or moved
     * into a chest left the +2 on the player until relog, and a second treetap in the inventory removed it every tick.
     */
    private static final net.minecraft.world.item.component.ItemAttributeModifiers REACH = net.minecraft.world.item.component.ItemAttributeModifiers.builder()
        .add(Attributes.BLOCK_INTERACTION_RANGE, new AttributeModifier(REACH_MODIFIER_ID, 2.0, AttributeModifier.Operation.ADD_VALUE),
            net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
        .build();

    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return REACH;
    }

}