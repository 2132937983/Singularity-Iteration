package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.tool.ToggleableElectricTool;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;


/**
 * 采矿镭射枪类
 * 继承电力工具基础值?
 * 右键发射激光子弹实�?
 * 支持模式切换：采矿、低焦、远距、水平、战斗、超级热线、散射、爆破�?x3、核能爆�?
 * 切换方式：手持镭射枪时蹲着右键
 */
@SuppressWarnings("null")
public class mio_icif_laser_miner extends mio_icif_tool_elc implements ToggleableElectricTool {

    public static final int LASER_MINER_MAX_ENERGY = 300000;

    // 各模式能量消耗（对齐 IC2 原版�?
    public static final int ENERGY_MINING = 1250;          // 采矿模式�?250 EU
    public static final int ENERGY_LOW_FOCUS = 100;        // 低焦模式�?00 EU
    public static final int ENERGY_LONG_RANGE = 5000;      // 远距模式�?000 EU
    public static final int ENERGY_HORIZONTAL = 3000;      // 水平模式�?000 EU
    public static final int ENERGY_COMBAT = 8000;          // 战斗模式�?000 EU（高耗能，高伤害�?
    public static final int ENERGY_SUPER_HEAT = 2500;      // 超级热线模式�?500 EU
    public static final int ENERGY_SCATTER = 10000;        // 散射模式�?0000 EU
    public static final int ENERGY_EXPLOSIVE = 5000;       // 爆破模式�?000 EU
    public static final int ENERGY_3X3 = 7500;             // 3x3模式�?500 EU
    public static final int ENERGY_NUCLEAR_EXPLOSIVE = 25000; // 核能爆破模式�?5000 EU（自定义�?

    // 子弹实体类型（需要在主类中注册）
    public static EntityType<mio_icif_laser_bullet> LASER_BULLET_ENTITY;

    // NBT 键名
    private static final String MODE_KEY = "LaserMode";

    // 模式常量（对�?IC2 原版顺序�?
    public static final int MODE_MINING = 0;          // 采矿模式：无限距离，power=5，无限方块破�?
    public static final int MODE_LOW_FOCUS = 1;       // 低焦模式：短距离(range=4)，power=5，只能打1个方法?
    public static final int MODE_LONG_RANGE = 2;      // 远距模式：无限距离，power=20，无限方块破�?
    public static final int MODE_HORIZONTAL = 3;      // 水平模式：需要点击方块触发，水平方向采矿
    public static final int MODE_COMBAT = 4;          // 战斗模式：不破坏方块，高伤害，power=25
    public static final int MODE_SUPER_HEAT = 5;      // 超级热线模式：烧制方块，power=8
    public static final int MODE_SCATTER = 6;         // 散射模式�?5个子弹，5x5分散，power=12
    public static final int MODE_EXPLOSIVE = 7;       // 爆破模式：爆炸效果，power=12
    public static final int MODE_3X3 = 8;             // 3x3模式：需要点击方块触发，3x3断面向前
    public static final int MODE_NUCLEAR_EXPLOSIVE = 9;  // 核能爆破模式�?00倍TNT当量（自定义模式�?

    /**
     * 默认构造函数（空电状态）
     * @param properties 物品属性?
     */
    public mio_icif_laser_miner(Properties properties) {
        super(properties, LASER_MINER_MAX_ENERGY, LASER_MINER_MAX_ENERGY, "laser_miner", 512, ENERGY_MINING, 3);
    }

    /**
     * 带初始能量的构造函数?
     * @param properties 物品属性?
     * @param initialEnergy 初始能量
     */
    public mio_icif_laser_miner(Properties properties, int initialEnergy) {
        super(properties, LASER_MINER_MAX_ENERGY, initialEnergy, "laser_miner", 512, ENERGY_MINING, 3);
    }

    /**
     * 完整参数的构造函数?
     * @param properties 物品属性?
     * @param maxEnergy 最大能量?
     * @param initialEnergy 初始能量
     * @param texturePrefix 贴图前缀
     * @param chargeRate 充电速率
     * @param energyPerUse 每次使用消耗的能量
     * @param toolTier 工具等级
     */
    public mio_icif_laser_miner(Properties properties, int maxEnergy, int initialEnergy, String texturePrefix, int chargeRate, int energyPerUse, int toolTier) {
        super(properties, maxEnergy, initialEnergy, texturePrefix, chargeRate, energyPerUse, toolTier);
    }

    /**
     * 设置子弹实体类型（在实体注册时调用）
     * @param entityType 子弹实体类型
     */
    public static void setLaserBulletEntity(EntityType<mio_icif_laser_bullet> entityType) {
        LASER_BULLET_ENTITY = entityType;
    }

    /**
     * 获取当前模式
     * @param stack 物品栈?
     * @return 当前模式�?-6�?
     */
    public int getMode(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(MODE_KEY)) {
                return tag.getInt(MODE_KEY);
            }
        }
        return MODE_MINING; // 默认为采矿模式?
    }

    /**
     * 设置模式
     * @param stack 物品栈?
     * @param mode 模式�?-6�?
     */
    public void setMode(ItemStack stack, int mode) {
        CompoundTag tag = new CompoundTag();
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt(MODE_KEY, mode);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    /**
     * 切换模式
     * @param stack 物品栈?
     * @return 切换后的模式
     */
    public int toggleMode(ItemStack stack) {
        int currentMode = getMode(stack);
        int newMode = (currentMode + 1) % 10;
        if (newMode == MODE_NUCLEAR_EXPLOSIVE && !com.singularity_iteration.mio_icif.Singularity_Iteration_Config.ENABLE_LASER_NUCLEAR_EXPLOSIVE.get()) {
            newMode = (newMode + 1) % 10;
        }
        setMode(stack, newMode);
        return newMode;
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        int newMode = toggleMode(stack);
        player.sendSystemMessage(
            Component.translatable("hud.mio_icif.laser_miner.switch_mode", getModeName(newMode))
        );
    }

    /**
     * 获取模式名称
     * @param mode 模式
     * @return 模式名称
     */
    public Component getModeName(int mode) {
        return switch (mode) {
            case MODE_MINING -> Component.translatable("hud.mio_icif.laser_miner.mode_mining");
            case MODE_LOW_FOCUS -> Component.translatable("hud.mio_icif.laser_miner.mode_low_focus");
            case MODE_LONG_RANGE -> Component.translatable("hud.mio_icif.laser_miner.mode_long_range");
            case MODE_HORIZONTAL -> Component.translatable("hud.mio_icif.laser_miner.mode_horizontal");
            case MODE_COMBAT -> Component.translatable("hud.mio_icif.laser_miner.mode_combat");
            case MODE_SUPER_HEAT -> Component.translatable("hud.mio_icif.laser_miner.mode_super_heat");
            case MODE_SCATTER -> Component.translatable("hud.mio_icif.laser_miner.mode_scatter");
            case MODE_EXPLOSIVE -> Component.translatable("hud.mio_icif.laser_miner.mode_explosive");
            case MODE_3X3 -> Component.translatable("hud.mio_icif.laser_miner.mode_3x3");
            case MODE_NUCLEAR_EXPLOSIVE -> Component.translatable("hud.mio_icif.laser_miner.mode_nuclear_explosive");
            default -> Component.translatable("hud.mio_icif.laser_miner.mode_mining");
        };
    }

    /**
     * 重写右键使用方法
     * 发射激光子弹实体，消耗能量?
     * 如果玩家蹲下，则切换模式
     * @param level 世界
     * @param player 玩家
     * @param hand 手持位置
     * @return 交互结果
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (LASER_BULLET_ENTITY == null) {
            return InteractionResultHolder.fail(stack);
        }

        int currentMode = getMode(stack);

        // 如果超高能爆破模式被禁用，且当前处于该模式，自动切换到采矿模式?
        if (currentMode == MODE_NUCLEAR_EXPLOSIVE && !com.singularity_iteration.mio_icif.Singularity_Iteration_Config.ENABLE_LASER_NUCLEAR_EXPLOSIVE.get()) {
            currentMode = MODE_MINING;
            setMode(stack, currentMode);
            if (!level.isClientSide) {
                player.sendSystemMessage(
                    Component.translatable("hud.mio_icif.laser_miner.mode_disabled", getModeName(MODE_NUCLEAR_EXPLOSIVE))
                );
            }
        }
        
        // 根据当前模式检查对应能量（对齐 IC2 原版�?
        int energyRequired = switch (currentMode) {
            case MODE_MINING -> ENERGY_MINING;
            case MODE_LOW_FOCUS -> ENERGY_LOW_FOCUS;
            case MODE_LONG_RANGE -> ENERGY_LONG_RANGE;
            case MODE_HORIZONTAL -> ENERGY_HORIZONTAL;
            case MODE_COMBAT -> ENERGY_COMBAT;
            case MODE_SUPER_HEAT -> ENERGY_SUPER_HEAT;
            case MODE_SCATTER -> ENERGY_SCATTER;
            case MODE_EXPLOSIVE -> ENERGY_EXPLOSIVE;
            case MODE_3X3 -> ENERGY_3X3;
            case MODE_NUCLEAR_EXPLOSIVE -> ENERGY_NUCLEAR_EXPLOSIVE;
            default -> ENERGY_MINING;
        };
        
        // 检查是否有足够能量
        if (!hasEnoughEnergy(stack, energyRequired)) {
            return InteractionResultHolder.fail(stack);
        }

        // 只在服务端处理?
        if (!level.isClientSide) {
            switch (currentMode) {
                case MODE_MINING -> fireMiningMode(level, player, stack);
                case MODE_LOW_FOCUS -> fireLowFocusMode(level, player, stack);
                case MODE_LONG_RANGE -> fireLongRangeMode(level, player, stack);
                case MODE_HORIZONTAL -> fireHorizontalMode(level, player, stack);
                case MODE_COMBAT -> fireCombatMode(level, player, stack);
                case MODE_SUPER_HEAT -> fireSuperHeatMode(level, player, stack);
                case MODE_SCATTER -> fireScatterMode(level, player, stack);
                case MODE_EXPLOSIVE -> fireExplosiveMode(level, player, stack);
                case MODE_3X3 -> fire3x3Mode(level, player, stack);
                case MODE_NUCLEAR_EXPLOSIVE -> fireNuclearExplosiveMode(level, player, stack);
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * 采矿模式：单发子弹，power=5，无限距离，无限方块破坏 (1250 EU)
     */
    private void fireMiningMode(Level level, Player player, ItemStack stack) {
        mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
        bullet.setMiningMode(true);
        bullet.setPenetratingMode(false);
        bullet.setPower(5.0f);
        bullet.setBlockBreaks(Integer.MAX_VALUE);
        bullet.setRange(mio_icif_laser_bullet.RANGE_MINING);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
        level.addFreshEntity(bullet);
        consumeEnergy(stack, ENERGY_MINING);
        playShootSound(level, player);
    }

    /**
     * 低焦模式：短距离(range=4)，power=5，只能打1个方法?(100 EU)
     */
    private void fireLowFocusMode(Level level, Player player, ItemStack stack) {
        mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
        bullet.setMiningMode(true);
        bullet.setPenetratingMode(false);
        bullet.setPower(5.0f);
        bullet.setBlockBreaks(1);
        bullet.setRange(mio_icif_laser_bullet.RANGE_LOW_FOCUS);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
        level.addFreshEntity(bullet);
        consumeEnergy(stack, ENERGY_LOW_FOCUS);
        playShootSound(level, player);
    }

    /**
     * 远距模式：无限距离，power=20，无限方块破�?(5000 EU)
     */
    private void fireLongRangeMode(Level level, Player player, ItemStack stack) {
        mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
        bullet.setMiningMode(true);
        bullet.setPenetratingMode(true);
        bullet.setPower(20.0f);
        bullet.setBlockBreaks(Integer.MAX_VALUE);
        bullet.setRange(mio_icif_laser_bullet.RANGE_LONG_RANGE);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
        level.addFreshEntity(bullet);
        consumeEnergy(stack, ENERGY_LONG_RANGE);
        playShootSound(level, player);
    }

    /**
     * 水平模式：需要点击方块触发，水平方向采矿，power=5 (3000 EU)
     * IC2原版中此模式需要点击方块使�?onItemUseFirst)，这里简化为右键发射
     * 子弹平行地面发射（垂直角度为0）
     */
    private void fireHorizontalMode(Level level, Player player, ItemStack stack) {
        mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
        bullet.setMiningMode(true);
        bullet.setPenetratingMode(false);
        bullet.setPower(5.0f);
        bullet.setBlockBreaks(Integer.MAX_VALUE);
        bullet.setRange(mio_icif_laser_bullet.RANGE_MINING);
        // 水平模式：XRot设为0，使子弹平行地面发射；保持YRot（水平方向）
        bullet.shootFromRotation(player, 0.0F, player.getYRot(), 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
        level.addFreshEntity(bullet);
        consumeEnergy(stack, ENERGY_HORIZONTAL);
        playShootSound(level, player);
    }

    /**
     * 战斗模式：单发子弹，不破坏方块，高伤害，power=25 (8000 EU)
     * 非采矿模式，子弹碰到方块直接消失，对实体造成25点伤�?
     */
    private void fireCombatMode(Level level, Player player, ItemStack stack) {
        mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
        bullet.setMiningMode(false);
        bullet.setPenetratingMode(false);
        bullet.setPower(25.0f);
        bullet.setBlockBreaks(Integer.MAX_VALUE);
        bullet.setRange(mio_icif_laser_bullet.RANGE_COMBAT);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
        level.addFreshEntity(bullet);
        consumeEnergy(stack, ENERGY_COMBAT);
        playShootSound(level, player);
    }

    /**
     * 超级热线模式：烧制方块，power=8 (2500 EU)
     */
    private void fireSuperHeatMode(Level level, Player player, ItemStack stack) {
        mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
        bullet.setMiningMode(false);
        bullet.setPenetratingMode(false);
        bullet.setSuperHeatMode(true);
        bullet.setPower(8.0f);
        bullet.setBlockBreaks(Integer.MAX_VALUE);
        bullet.setRange(mio_icif_laser_bullet.RANGE_SUPER_HEAT);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
        level.addFreshEntity(bullet);
        consumeEnergy(stack, ENERGY_SUPER_HEAT);
        playShootSound(level, player);
    }

    /**
     * 散射模式�?5个子弹，5x5分散，power=12 (10000 EU)
     */
    private void fireScatterMode(Level level, Player player, ItemStack stack) {
        float baseYaw = player.getYRot();
        float basePitch = player.getXRot();

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
                bullet.setMiningMode(true);
                bullet.setPenetratingMode(false);
                bullet.setPower(12.0f);
                bullet.setBlockBreaks(Integer.MAX_VALUE);
                bullet.setRange(mio_icif_laser_bullet.RANGE_SCATTER);

                float yawOffset = x * 3.0F;
                float pitchOffset = y * 3.0F;

                bullet.shootFromRotation(player, basePitch + pitchOffset, baseYaw + yawOffset, 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
                level.addFreshEntity(bullet);
            }
        }
        consumeEnergy(stack, ENERGY_SCATTER);
        playShootSound(level, player);
    }

    /**
     * 爆破模式：爆炸效果，power=12 (5000 EU)
     */
    private void fireExplosiveMode(Level level, Player player, ItemStack stack) {
        mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
        bullet.setMiningMode(false);
        bullet.setPenetratingMode(false);
        bullet.setExplosiveMode(true);
        bullet.setPower(12.0f);
        bullet.setBlockBreaks(Integer.MAX_VALUE);
        bullet.setRange(mio_icif_laser_bullet.RANGE_EXPLOSIVE);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
        level.addFreshEntity(bullet);
        consumeEnergy(stack, ENERGY_EXPLOSIVE);
        playShootSound(level, player);
    }

    /**
     * 核能爆破模式�?00倍TNT当量爆炸 (25000 EU，自定义模式)
     */
    private void fireNuclearExplosiveMode(Level level, Player player, ItemStack stack) {
        mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
        bullet.setMiningMode(false);
        bullet.setPenetratingMode(false);
        bullet.setExplosiveMode(true);
        bullet.setNuclearMode(true);
        bullet.setPower(100.0f);  // 设置足够的power，确保能飞行到目标
        bullet.setBlockBreaks(Integer.MAX_VALUE);
        bullet.setRange(mio_icif_laser_bullet.RANGE_EXPLOSIVE);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
        level.addFreshEntity(bullet);
        consumeEnergy(stack, ENERGY_NUCLEAR_EXPLOSIVE);
        playShootSound(level, player);
        
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    /**
     * 3x3模式�?个子弹，3x3断面向前，power=5 (7500 EU)
     */
    private void fire3x3Mode(Level level, Player player, ItemStack stack) {
        float baseYaw = player.getYRot();
        float basePitch = player.getXRot();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                mio_icif_laser_bullet bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, player, level);
                bullet.setMiningMode(true);
                bullet.setPenetratingMode(false);
                bullet.setPower(5.0f);
                bullet.setBlockBreaks(Integer.MAX_VALUE);
                bullet.setRange(mio_icif_laser_bullet.RANGE_3X3);

                float yawOffset = x * 5.0F;
                float pitchOffset = y * 5.0F;

                bullet.shootFromRotation(player, basePitch + pitchOffset, baseYaw + yawOffset, 0.0F, (float) mio_icif_laser_bullet.LASER_SPEED, 0.0F);
                level.addFreshEntity(bullet);
            }
        }
        consumeEnergy(stack, ENERGY_3X3);
        playShootSound(level, player);
    }

    /**
     * 播放射击音效
     */
    private void playShootSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    /**
     * 获取镭射枪的采集范围
     * @return 采集范围
     */
    public double getLaserRange() {
        return 32.0;
    }

    /**
     * 重写：获取默认实例时初始化为空电状态?
     * 确保通过创造模式�?give 命令等方式获取的镭射枪也是空电状态?
     */
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        stack.setDamageValue(LASER_MINER_MAX_ENERGY);
        return stack;
    }

    /**
     * 重写：每 tick 调用，当手持镭射枪时在聊天栏输出模式和能量信息?
     * @param stack 物品栈?
     * @param level 世界
     * @param entity 实体
     * @param slotId 槽位 ID
     * @param isSelected 是否被选中（手持）
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // 只在服务端且玩家手持镭射枪时输出信息
        if (!level.isClientSide && isSelected && entity instanceof Player player) {
            long currentEnergy = getEnergy(stack);
            int mode = getMode(stack);
            Component modeName = getModeName(mode);
            player.displayClientMessage(
                Component.translatable("hud.mio_icif.laser_miner.display",
                    modeName, currentEnergy, getMaxEnergy()),
                true
            );
        }
    }
}