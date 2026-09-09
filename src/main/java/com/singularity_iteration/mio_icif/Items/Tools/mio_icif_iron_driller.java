package com.singularity_iteration.mio_icif.Items.Tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.singularity_iteration.mio_icif.api.tool.IMiningDrill;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * 铁钻�?
 * 继承电力工具基础类，使用电力驱动
 * 具有铁镐的一切性质，不会因耐久度为0而损坏?
 * 有电时破坏速度与铁镐相当，没电时速度变得跟手挖一样快
 */
@SuppressWarnings("null")
public class mio_icif_iron_driller extends mio_icif_tool_elc implements IMiningDrill {

    // 铁钻头默认最大能量?(IC2原版: 30000 EU)
    public static final int IRON_DRILLER_MAX_ENERGY = 30000;

    // 铁钻头传输限制?(IC2原版: 100 EU/t)
    public static final int IRON_DRILLER_TRANSFER_LIMIT = 100;

    // 铁钻头每次使用消耗的能量 (IC2原版: 50 EU)
    public static final int IRON_DRILLER_ENERGY_PER_USE = 50;

    // 铁钻头的攻击伤害 (IC2原版: 8.0)
    public static final float IRON_DRILLER_ATTACK_DAMAGE = 8.0F;

    // 铁钻头的挖掘效率 (IC2原版: 8.0)
    public static final float IRON_DRILLER_EFFICIENCY = 8.0F;

    // 铁镐的挖掘等�?
    private static final Tier IRON_TIER = Tiers.IRON;

    /**
     * 默认构造函数（空电状态）
     * @param properties 物品属性?
     */
    public mio_icif_iron_driller(Properties properties) {
        super(properties, IRON_DRILLER_MAX_ENERGY, IRON_DRILLER_MAX_ENERGY, "iron_driller", IRON_DRILLER_TRANSFER_LIMIT, IRON_DRILLER_ENERGY_PER_USE, 1);
    }

    /**
     * 带初始能量的构造函数?
     * @param properties 物品属性?
     * @param initialEnergy 初始能量
     */
    public mio_icif_iron_driller(Properties properties, int initialEnergy) {
        super(properties, IRON_DRILLER_MAX_ENERGY, IRON_DRILLER_MAX_ENERGY - initialEnergy, "iron_driller", IRON_DRILLER_TRANSFER_LIMIT, IRON_DRILLER_ENERGY_PER_USE, 1);
    }

    /**
     * 获取攻击伤害加成
     * 铁钻头提取?点伤�?(IC2原版)
     */
    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        return IRON_DRILLER_ATTACK_DAMAGE;
    }

    /**
     * 检查是否可以挖掘指定方法?
     * 铁钻头可以挖掘铁镐能挖的所有方法?
     */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        // 检查是否有足够能量
        ItemStack stack = player.getMainHandItem();
        if (!hasEnoughEnergy(stack)) {
            // 没电时只能挖掘手可以挖的方块
            return state.getDestroySpeed(level, pos) > 0 && !state.requiresCorrectToolForDrops();
        }
        // 有电时可以挖掘铁镐能挖的方块
        return true;
    }

    /**
     * 检查是否可以执行特定动作（镐子+铲子动作�?
     * 这是解决挖掘进度重置问题的关键方法?
     * NeoForge在挖掘过程中会通过此方法验证工具能量?
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (!hasEnoughEnergy(stack)) {
            return false;
        }

        if (ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility)) {
            return true;
        }

        if (ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility)) {
            return true;
        }

        // 保留斧头动作支持，防止挖掘木头时进度重置
        // 但getDestroySpeed中不会给予快速挖掘速度
        // isCorrectToolForDrops确保木头不会掉落
        if (ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility)) {
            return true;
        }

        return super.canPerformAction(stack, itemAbility);
    }

    /**
     * 获取挖掘速度
     * 有电时与铁镐相当，没电时与手挖一�?
     * 钻头只能快速挖掘镐子和铲子类方法?
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) {
            return 1.0F;
        }

        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) {
            return IRON_DRILLER_EFFICIENCY;
        }

        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) {
            return IRON_DRILLER_EFFICIENCY;
        }

        return 1.0F;
    }

    /**
     * 挖掘方块时消耗能量?
     * 对齐IC2原版：只在方块实际被破坏后消耗能量，且只在硬度不为0时消耗
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        // 只在服务端、方块硬度不为0时消耗能量
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            consumeEnergy(stack);
        }
        return true;
    }

    /**
     * 检查是否正确工�?
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) {
            return false;
        }
        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)) {
                return false;
            }
            return true;
        }
        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) {
            if (state.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 重写：防止钻头损坏?
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        // 电力钻头不通过damageItem损坏，而是通过消耗能量?
        return 0;
    }

    /**
     * 重写：钻头永远不会消耗?
     */
    @Override
    public boolean isDamaged(ItemStack stack) {
        // 当能量未满时返回true，显示耐久度信息?
        return getEnergy(stack) < getMaxEnergy();
    }

    /**
     * 是否可以附魔
     */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    /**
     * 获取附魔能力值（与铁镐相同）
     */
    @Override
    public int getEnchantmentValue() {
        return IRON_TIER.getEnchantmentValue();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // ==================== IMiningDrill 接口实现 ====================

    @Override
    public long energyUse(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        return IRON_DRILLER_ENERGY_PER_USE;
    }

    @Override
    public int breakTime(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness < 0.0F) return -1;
        if (hardness == 0.0F) return 0;
        float speed = getDestroySpeed(stack, state);
        if (speed <= 0.0F) speed = 1.0F;
        return (int) Math.ceil((hardness * 1.5F / speed) * 20.0F);
    }

    @Override
    public boolean breakBlock(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        if (!(world instanceof Level level) || level.isClientSide) return false;
        if (!hasEnoughEnergy(stack)) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        boolean removed = level.destroyBlock(pos, true);
        if (removed) consumeEnergy(stack);
        return removed;
    }

}