package com.singularity_iteration.mio_icif.Items.Tools;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

/**
 * 铁链�?
 * 继承电力工具基础类，使用电力驱动
 * 具有铁斧的一切性质，不会因耐久度为0而损坏?
 * 还具有剪刀的性质：剪羊毛、精准采集蜘蛛网、树叶等
 * 有电时破坏速度与铁斧相当，没电时速度变得跟手挖一样快
 */
@SuppressWarnings("null")
public class mio_icif_iron_chainsaw extends mio_icif_tool_elc {

    // 铁链锯默认最大能量?(IC2原版: 30000 EU)
    public static final int IRON_CHAINSAW_MAX_ENERGY = 30000;

    // 铁链锯传输限制?(IC2原版: 100 EU/t)
    public static final int IRON_CHAINSAW_TRANSFER_LIMIT = 100;

    // 铁链锯每次使用消耗的能量 (IC2原版: 100 EU)
    public static final int IRON_CHAINSAW_ENERGY_PER_USE = 100;

    // 铁链锯的攻击伤害 (IC2原版: 9.0)
    public static final float IRON_CHAINSAW_ATTACK_DAMAGE = 9.0F;

    // 铁链锯的挖掘效率 (IC2原版: 8.0)
    public static final float IRON_CHAINSAW_EFFICIENCY = 8.0F;

    // 铁斧的挖掘等�?
    private static final Tier IRON_TIER = Tiers.IRON;

    /**
     * 默认构造函数（空电状态）
     * @param properties 物品属性?
     */
    public mio_icif_iron_chainsaw(Properties properties) {
        super(properties, IRON_CHAINSAW_MAX_ENERGY, IRON_CHAINSAW_MAX_ENERGY, "iron_chainsaw", IRON_CHAINSAW_TRANSFER_LIMIT, IRON_CHAINSAW_ENERGY_PER_USE, 1);
    }

    /**
     * 带初始能量的构造函数?
     * @param properties 物品属性?
     * @param initialEnergy 初始能量
     */
    public mio_icif_iron_chainsaw(Properties properties, int initialEnergy) {
        super(properties, IRON_CHAINSAW_MAX_ENERGY, IRON_CHAINSAW_MAX_ENERGY - initialEnergy, "iron_chainsaw", IRON_CHAINSAW_TRANSFER_LIMIT, IRON_CHAINSAW_ENERGY_PER_USE, 1);
    }

    /**
     * 获取攻击伤害加成
     * 铁链锯提取?点伤�?(IC2原版)
     */
    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        return IRON_CHAINSAW_ATTACK_DAMAGE;
    }

    /**
     * 检查是否可以挖掘指定方法?
     * 铁链锯可以挖掘铁斧能挖的所有方块，以及剪刀能处理的方块
     */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        // 检查是否有足够能量
        ItemStack stack = player.getMainHandItem();
        if (!hasEnoughEnergy(stack)) {
            // 没电时只能挖掘手可以挖的方块
            return state.getDestroySpeed(level, pos) > 0 && !state.requiresCorrectToolForDrops();
        }
        // 有电时可以挖�?
        return true;
    }

    /**
     * 获取挖掘速度
     * 有电时与铁斧/剪刀相当，没电时与手挖一�?
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // 检查是否有足够能量
        if (!hasEnoughEnergy(stack)) {
            // 没电时速度极慢（手挖速度�?
            return 1.0F;
        }

        // 剪刀类方块（高优先级�?
        if (state.is(Blocks.COBWEB)) {
            return 15.0F; // 蜘蛛�?- 剪刀速度
        }
        if (state.is(BlockTags.LEAVES)) {
            return 15.0F; // 树叶 - 剪刀速度
        }
        if (state.is(BlockTags.WOOL)) {
            return 5.0F; // 羊毛 - 剪刀速度
        }
        if (state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN)) {
            return 2.0F; // 藤蔓、发光地�?
        }

        // 斧子类方法?
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return IRON_CHAINSAW_EFFICIENCY;
        }

        // 对于其他方块，返回基础速度
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
        // 检查是否有足够能量
        if (!hasEnoughEnergy(stack)) {
            return false;
        }

        // 剪刀类方法?- 始终可以处理
        if (state.is(Blocks.COBWEB) ||
            state.is(BlockTags.LEAVES) ||
            state.is(BlockTags.WOOL) ||
            state.is(Blocks.VINE) ||
            state.is(Blocks.GLOW_LICHEN) ||
            state.is(Blocks.TRIPWIRE)) {
            return true;
        }

        // 斧子类方法?
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            // 检查方块需要的挖掘等级
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
                return false; // 需要钻石工�?
            }
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
                return true; // 需要铁工具
            }
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
                return true; // 需要石工具
            }
            // 不需要特定工具的方块
            return true;
        }

        return false;
    }

    /**
     * 右键点击实体（剪羊毛等）
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        // 检查是否有足够能量
        if (!hasEnoughEnergy(stack)) {
            return InteractionResult.PASS;
        }

        // 检查实体是否可以被剪切
        if (entity instanceof net.neoforged.neoforge.common.IShearable target) {
            BlockPos pos = entity.blockPosition();
            boolean isClient = entity.level().isClientSide();

            // 检查是否可以剪�?
            if (target.isShearable(player, stack, entity.level(), pos)) {
                // 在服务端执行剪切
                if (!isClient) {
                    List<ItemStack> drops = target.onSheared(player, stack, entity.level(), pos);
                    for (ItemStack drop : drops) {
                        target.spawnShearedDrop(entity.level(), pos, drop);
                    }
                }

                // 触发剪切游戏事件
                entity.gameEvent(GameEvent.SHEAR, player);

                // 在服务端消耗能量?
                if (!isClient) {
                    consumeEnergy(stack);
                }

                return InteractionResult.sidedSuccess(isClient);
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * 右键点击方块（剥离木头、去蜡等�?
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        // 检查是否有足够能量
        if (!hasEnoughEnergy(stack)) {
            return InteractionResult.PASS;
        }

        // 尝试执行斧子的右键功能（剥离木头、去蜡等�?
        // 使用 NeoForge �?ItemAbilities 系统
        if (player != null) {
            // 检查是否是可剥离的木头
            BlockState modifiedState = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
            if (modifiedState != null) {
                if (!level.isClientSide) {
                    level.setBlock(pos, modifiedState, 11);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, modifiedState));
                    consumeEnergy(stack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // 检查是否是可去蜡的铜块
            modifiedState = state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false);
            if (modifiedState != null) {
                if (!level.isClientSide) {
                    level.setBlock(pos, modifiedState, 11);
                    level.levelEvent(player, 3005, pos, 0);
                    consumeEnergy(stack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // 检查是否是可除锈的铜块
            modifiedState = state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false);
            if (modifiedState != null) {
                if (!level.isClientSide) {
                    level.setBlock(pos, modifiedState, 11);
                    level.levelEvent(player, 3004, pos, 0);
                    consumeEnergy(stack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * 检查是否可以执行特定动作（剪刀动作、斧子动作）
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        // 检查是否有足够能量
        if (!hasEnoughEnergy(stack)) {
            return false;
        }

        // 支持剪刀的所有动作?
        if (ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility)) {
            return true;
        }

        // 支持斧子的所有动作?
        if (ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility)) {
            return true;
        }

        return super.canPerformAction(stack, itemAbility);
    }

    /**
     * 重写：防止链锯损坏?
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        // 电力链锯不通过damageItem损坏，而是通过消耗能量?
        return 0;
    }

    /**
     * 重写：链锯永远不会消耗?
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
     * 获取附魔能力值（与铁斧相同）
     */
    @Override
    public int getEnchantmentValue() {
        return IRON_TIER.getEnchantmentValue();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

}