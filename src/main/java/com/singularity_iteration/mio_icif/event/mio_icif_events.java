package com.singularity_iteration.mio_icif.event;

import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_scaffold;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Chamber;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.NukeExplosionScheduler;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.NukeExplosionSchedulerOctree;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.ReactorExplosionSchedulerWave;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.IElectricArmorItem;
import com.singularity_iteration.mio_icif.api.item.IJetpackItem;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_boots_nano;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_boots_quantum;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.network.mio_icif_KeyboardManager;
import com.singularity_iteration.mio_icif.util.NuclearRadiationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 主事件处理器
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
@SuppressWarnings("null")
public class mio_icif_events {

    // 每20 tick 检查一次
    private static final int TICK_INTERVAL = 20; // 每秒检查一次

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        // 只处理生物实体
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        // 只在服务端执行
        if (livingEntity.level().isClientSide) {
            return;
        }

        // 每20 tick检查一次
        if (livingEntity.level().getGameTime() % TICK_INTERVAL != 0) {
            return;
        }

        if (NuclearRadiationHandler.hasNuclearItem(livingEntity)) {
            NuclearRadiationHandler.applyRadiationEffect(livingEntity);
        }

        @SuppressWarnings("unused")
        Level level = livingEntity.level();
        @SuppressWarnings("unused")
        BlockPos entityPos = livingEntity.blockPosition();
    }
    
    /**
     * 每tick 更新核弹/反应堆爆炸任务
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }

        // 电网管理已移至GridEventHandler（IC2 风格架构）
        
        // 更新核弹爆炸任务
        NukeExplosionScheduler.tick(level);
        
        // 更新核弹爆炸任务 (Octree版本)
        NukeExplosionSchedulerOctree.tick(level);
        
        // 更新核反应堆爆炸任务 (Wave Propagation版本)
        ReactorExplosionSchedulerWave.tick(level);
    }

    /**
     * 玩家摔落时处理电力靴子的摔落免疫
     * 量子靴：消耗能量抵挡摔落伤害（每点伤害消耗10000EU）
     * 纳米靴：
     *   - 摔落≤3格：免费免疫
     *   - 摔落4~12格：消耗能量免疫（每格5000EU）
     *   - 摔落>12格：减免12格伤害，剩余伤害穿透，消耗12格能量
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        // 喷气背包安全网：如果玩家正在喷气背包飞行中（包括减速阶段），取消摔落伤害
        // 参考IC2 JetpackHandler.livingAttack：IC2通过事件监听来防止喷气背包飞行后的摔落伤害
        if (entity instanceof Player player) {
            ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!chestplate.isEmpty() && chestplate.getItem() instanceof IJetpackItem) {
                // Check if player is currently flying using jetpack (fall distance tracking)
                if (player.fallDistance < 3.0F) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty() || !MioIcifAPI.instance().getItemAPI().isElectricArmor(boots)) {
            return;
        }

        var itemApi = MioIcifAPI.instance().getItemAPI();
        if (itemApi.getElectricArmorStored(boots) <= 0) {
            return;
        }

        if (boots.getItem() instanceof mio_icif_boots_quantum quantumBoots) {
            int fallDamage = Math.max((int) event.getDistance() - 10, 0);
            if (fallDamage <= 0) {
                event.setCanceled(true);
                return;
            }
            long energyPerDamage = quantumBoots.getEnergyPerDamage();
            long energyCost = fallDamage * energyPerDamage;
            if (itemApi.getElectricArmorStored(boots) >= energyCost) {
                itemApi.dischargeElectricArmor(boots, energyCost, false);
                event.setCanceled(true);
            }
            return;
        }

        if (boots.getItem() instanceof mio_icif_boots_nano) {
            float fallDistance = event.getDistance();

            if (fallDistance <= mio_icif_boots_nano.FREE_FALL_DISTANCE) {
                event.setCanceled(true);
                return;
            }

            if (fallDistance <= mio_icif_boots_nano.FULL_ABSORB_FALL_DISTANCE) {
                int blocksOverFree = (int) (fallDistance - mio_icif_boots_nano.FREE_FALL_DISTANCE);
                int energyCost = blocksOverFree * mio_icif_boots_nano.FALL_ENERGY_PER_BLOCK;
                if (itemApi.getElectricArmorStored(boots) >= energyCost) {
                    itemApi.dischargeElectricArmor(boots, energyCost, false);
                    event.setCanceled(true);
                }
            } else {
                int energyCost = (mio_icif_boots_nano.FULL_ABSORB_FALL_DISTANCE - mio_icif_boots_nano.FREE_FALL_DISTANCE)
                        * mio_icif_boots_nano.FALL_ENERGY_PER_BLOCK;
                if (itemApi.getElectricArmorStored(boots) >= energyCost) {
                    itemApi.dischargeElectricArmor(boots, energyCost, false);
                    event.setDistance(fallDistance - mio_icif_boots_nano.FULL_ABSORB_FALL_DISTANCE);
                }
            }
        }
    }

    private static final int QUANTUM_ARMOR_TIER = 6;
    private static final int NANO_ARMOR_TIER = 5;

    private static boolean isQuantumArmor(ItemStack stack) {
        return stack.getItem() instanceof IElectricArmorItem armor && armor.getArmorTier() == QUANTUM_ARMOR_TIER;
    }

    private static boolean isNanoArmor(ItemStack stack) {
        return stack.getItem() instanceof IElectricArmorItem armor && armor.getArmorTier() == NANO_ARMOR_TIER;
    }

    private static boolean isWearingFullQuantumSet(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !MioIcifAPI.instance().getItemAPI().isElectricArmor(stack) || !isQuantumArmor(stack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWearingFullNanoSet(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !MioIcifAPI.instance().getItemAPI().isElectricArmor(stack) || !isNanoArmor(stack)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 玩家受到伤害时处理电力护甲的能量吸收
     *
     * <p>通过 {@link IElectricArmorItem#getDamageAbsorptionRatio(EquipmentSlot)} 和
     * {@link IElectricArmorItem#getEnergyPerDamage()} 两个 API 方法驱动伤害吸收逻辑，
     * Addon 开发者只需实现这两个方法即可自定义装甲的伤害吸收行为。
     *
     * <p>量子套特殊规则（tier=6）：
     * - 可以抵挡凋灵、龙息、魔法、爆炸等所有非BYPASSES_ARMOR伤害
     * - 不能抵挡虚空伤害、/kill、饥饿、溺水等穿甲伤害
     * - 全套总计吸收108%，完全抵挡所有非穿甲伤害
     *
     * <p>纳米套特殊规则（tier=5）：
     * - 穿齐整套时：吸收90%伤害
     * - 未穿齐时：各部件按独立比例减免伤害
     *
     * <p>其他电力护甲：通过 getDamageAbsorptionRatio + getEnergyPerDamage 驱动
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        float damage = event.getNewDamage();
        if (damage <= 0) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        boolean isUnblockable = source.is(DamageTypeTags.BYPASSES_ARMOR) || source.is(DamageTypeTags.BYPASSES_EFFECTS);
        boolean isExplosion = source.is(DamageTypeTags.IS_EXPLOSION);
        boolean isRadiation = source.getMsgId().equals("mio_icif_killed_by_uranium");

        boolean fullQuantumSet = isWearingFullQuantumSet(entity);
        boolean fullNanoSet = isWearingFullNanoSet(entity);

        if (fullQuantumSet && isRadiation) {
            event.setNewDamage(0);
            return;
        }

        float remainingDamage = damage;
        float totalAbsorbed = 0;

        EquipmentSlot[] armorSlots = {
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

        var itemApi = MioIcifAPI.instance().getItemAPI();
        for (EquipmentSlot slot : armorSlots) {
            if (remainingDamage <= 0) {
                break;
            }

            ItemStack armorStack = entity.getItemBySlot(slot);
            if (armorStack.isEmpty() || !itemApi.isElectricArmor(armorStack)) {
                continue;
            }

            if (itemApi.getElectricArmorStored(armorStack) <= 0) {
                continue;
            }

            if (!(armorStack.getItem() instanceof IElectricArmorItem armorItem)) {
                continue;
            }

            long energyPerDamage = armorItem.getEnergyPerDamage();
            float absorptionRatio = armorItem.getDamageAbsorptionRatio(slot);

            if (energyPerDamage <= 0 || absorptionRatio <= 0) {
                continue;
            }

            boolean isQuantum = armorItem.getArmorTier() == QUANTUM_ARMOR_TIER;
            boolean isNano = armorItem.getArmorTier() == NANO_ARMOR_TIER;

            if (isQuantum && isUnblockable && !isExplosion) {
                continue;
            }

            if (isNano && fullNanoSet) {
                absorptionRatio = 0.9F;
            }

            float absorbAmount = remainingDamage * absorptionRatio;
            long energyCost = (long) (absorbAmount * energyPerDamage);

            if (itemApi.getElectricArmorStored(armorStack) >= energyCost) {
                itemApi.dischargeElectricArmor(armorStack, energyCost, false);
                totalAbsorbed += absorbAmount;
                remainingDamage -= absorbAmount;
            } else {
                long canAbsorb = itemApi.getElectricArmorStored(armorStack) / energyPerDamage;
                if (canAbsorb > 0) {
                    float actualAbsorbed = Math.min(canAbsorb, absorbAmount);
                    itemApi.dischargeElectricArmor(armorStack, (long) (actualAbsorbed * energyPerDamage), false);
                    totalAbsorbed += actualAbsorbed;
                    remainingDamage -= actualAbsorbed;
                }
            }
        }

        if (totalAbsorbed > 0) {
            float finalDamage = Math.max(0, damage - totalAbsorbed);
            event.setNewDamage(finalDamage);
        }
    }

    /**
     * 脚手架攀爬事件处理
     * 当玩家贴着脚手架方块时，实现类似梯子的攀爬功能
     * - 按跳跃键：向上攀爬
     * - 按Shift键：停留在当前位置（不下滑）
 * - 不按键：慢下滑
 * 注意：站在脚手架顶部不会触发减掉落效果
     */
    @SubscribeEvent
    public static void onScaffoldClimb(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 创造模式飞行不做处理
        if (player.getAbilities().flying) {
            return;
        }

        Level level = player.level();

        // 检查玩家是否贴着脚手架（像梯子一样，四面都能爬）
        // 参考MC原版梯子逻辑：检测玩家碰撞箱是否与脚手架方块区域重叠
        // 排除站在脚手架顶部的情况（玩家脚部在方块顶部之上）
        BlockPos playerPos = player.blockPosition();
        boolean touchingScaffold = false;

        // 玩家碰撞箱
        double playerMinX = player.getX() - player.getBbWidth() / 2;
        double playerMaxX = player.getX() + player.getBbWidth() / 2;
        double playerMinZ = player.getZ() - player.getBbWidth() / 2;
        double playerMaxZ = player.getZ() + player.getBbWidth() / 2;
        double playerMinY = player.getY();
        double playerMaxY = player.getY() + player.getBbHeight();

        // 检查玩家周围3x3x3范围内的脚手架
        for (int dx = -1; dx <= 1 && !touchingScaffold; dx++) {
            for (int dy = 0; dy <= 2 && !touchingScaffold; dy++) {
                for (int dz = -1; dz <= 1 && !touchingScaffold; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);
                    BlockState checkState = level.getBlockState(checkPos);
                    if (checkState.getBlock() instanceof mio_icif_block_scaffold) {
                        // 脚手架方块的完整区域1x1x1
                        double blockMinX = checkPos.getX();
                        double blockMaxX = checkPos.getX() + 1.0;
                        double blockMinZ = checkPos.getZ();
                        double blockMaxZ = checkPos.getZ() + 1.0;
                        double blockMinY = checkPos.getY();
                        double blockMaxY = checkPos.getY() + 1.0;

                        // 检查玩家碰撞箱是否与方块区域有水平重叠
                        boolean overlapsX = playerMinX < blockMaxX && playerMaxX > blockMinX;
                        boolean overlapsZ = playerMinZ < blockMaxZ && playerMaxZ > blockMinZ;
                        // 垂直方向：玩家身体与方块有重叠
                        boolean overlapsY = playerMinY < blockMaxY && playerMaxY > blockMinY;

                        // 排除站在脚手架顶部的情况
                        // 玩家脚部在方块顶部或之上 = 站在顶部，不算贴着
                        boolean standingOnTop = playerMinY >= blockMaxY - 0.01;

                        if (overlapsX && overlapsZ && overlapsY && !standingOnTop) {
                            touchingScaffold = true;
                        }
                    }
                }
            }
        }

        if (!touchingScaffold) {
            return;
        }

        // 重置摔落距离（服务端）
        if (!level.isClientSide) {
            player.resetFallDistance();
        }

        // 获取当前速度
        var movement = player.getDeltaMovement();

        if (player.isShiftKeyDown()) {
            // 潜行时：停留在当前位置（不下滑）
            player.setDeltaMovement(movement.x, 0, movement.z);
        } else {
            // 检测跳跃键状态（通过反射访问私有字段）
            boolean wantsToClimb = isJumpKeyDown(player, level.isClientSide);

            if (wantsToClimb) {
                // 向上攀爬
                player.setDeltaMovement(movement.x, 0.2D, movement.z);
            } else {
 // 慢下滑（未按跳跃键且未潜行）
                player.setDeltaMovement(
                    movement.x,
                    Math.max(movement.y, -0.15D),
                    movement.z
                );
            }
        }
    }

    private static java.lang.reflect.Method cachedClientIsJumpKeyDown;
    private static java.lang.reflect.Field cachedJumpingField;

    private static boolean isJumpKeyDown(Player player, boolean isClientSide) {
        try {
            if (isClientSide) {
                if (cachedClientIsJumpKeyDown == null) {
                    Class<?> helperClass = Class.forName(
                        "com.singularity_iteration.mio_icif.client.ClientKeyHelper");
                    cachedClientIsJumpKeyDown = helperClass.getMethod("isJumpKeyDown");
                }
                return (boolean) cachedClientIsJumpKeyDown.invoke(null);
            } else {
                if (cachedJumpingField == null) {
                    cachedJumpingField = LivingEntity.class.getDeclaredField("jumping");
                    cachedJumpingField.setAccessible(true);
                }
                return cachedJumpingField.getBoolean(player);
            }
        } catch (Exception e) {
            return player.getDeltaMovement().y > 0;
        }
    }

    /**
     * 检测核反应仓是否同时接触多个核反应堆
     * 当核反应堆被放置时触发检测
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof Level level) || level.isClientSide()) {
            return;
        }

        BlockPos notifiedPos = event.getPos();
        BlockState notifiedState = event.getState();

        // 只有核反应堆放置时才需要检查周围的核反应仓
        if (!(notifiedState.getBlock() instanceof mio_icif_Block_Nuclear_Reactor_Generator)) {
            return;
        }

        // 检查周围的 6 个面是否有核反应堆
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = notifiedPos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            // 如果是核反应仓，检查它是否同时接触多个核反应堆
            if (neighborState.getBlock() instanceof mio_icif_Block_Reactor_Chamber) {
                checkChamberMultipleReactors(level, neighborPos);
            }
        }
    }

    /**
     * 检查核反应仓是否同时接触多个核反应堆，如果是则爆炸
     */
    private static void checkChamberMultipleReactors(Level level, BlockPos chamberPos) {
        int reactorCount = 0;

        // 检查核反应仓周围的6个面
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = chamberPos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof mio_icif_Block_Nuclear_Reactor_Generator) {
                reactorCount++;
            }
        }

        // 如果连接了多个核反应堆，立即爆炸
        if (reactorCount > 1) {
            // 产生爆炸
            level.explode(null, chamberPos.getX() + 0.5, chamberPos.getY() + 0.5, chamberPos.getZ() + 0.5,
                1.5f, Level.ExplosionInteraction.BLOCK);

            // 移除核反应仓方块（不触发掉落）
            level.removeBlock(chamberPos, false);
        }
    }

    /**
     * 玩家退出时清理按键状态引用
     * 对应IC2：Keyboard.removePlayerReferences()
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        mio_icif_KeyboardManager.removePlayerReferences(event.getEntity());
        // 清理喷气背包按键状态
        com.singularity_iteration.mio_icif.util.JetpackKeyHandler.removePlayer(event.getEntity());
    }
}