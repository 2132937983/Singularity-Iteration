package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Nuclear_Reactor_Generator;
import com.miophas.singularity_iteration.common.block.reactor.mio_icif_Block_Reactor_Chamber;
import com.miophas.singularity_iteration.common.blockentity.reactor.NukeExplosionScheduler;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_boots_nano;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_boots_quantum;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.network.mio_icif_KeyboardManager;
import com.miophas.singularity_iteration.common.util.NuclearRadiationHandler;
import com.miophas.singularity_iteration.common.util.RadiationProtectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
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
            long energyPerDamage = com.miophas.singularity_iteration.core.api.item.EnergySaving.rate(boots, quantumBoots.getEnergyPerDamage());
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
                long energyCost = com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(boots, (long) blocksOverFree * mio_icif_boots_nano.FALL_ENERGY_PER_BLOCK);
                // 0.1.8.6 shock gel: the fall protection costs 70 % less (Mk.II 85 %)
                energyCost = (long) Math.ceil(energyCost * com.miophas.singularity_iteration.common.suit.nano.NanoSuit.fallCostFactor(entity));
                if (itemApi.getElectricArmorStored(boots) >= energyCost) {
                    itemApi.dischargeElectricArmor(boots, energyCost, false);
                    event.setCanceled(true);
                }
            } else {
                long energyCost = com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(boots, (long) (mio_icif_boots_nano.FULL_ABSORB_FALL_DISTANCE - mio_icif_boots_nano.FREE_FALL_DISTANCE)
                        * mio_icif_boots_nano.FALL_ENERGY_PER_BLOCK);
                energyCost = (long) Math.ceil(energyCost * com.miophas.singularity_iteration.common.suit.nano.NanoSuit.fallCostFactor(entity));
                if (itemApi.getElectricArmorStored(boots) >= energyCost) {
                    itemApi.dischargeElectricArmor(boots, energyCost, false);
                    event.setDistance(fallDistance - mio_icif_boots_nano.FULL_ABSORB_FALL_DISTANCE);
                }
            }
        }
    }

    /** Shared array: the damage handler runs for every hit, so it must not allocate one each call. */
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * 玩家受到伤害时处理电力护甲的能量吸收（0.1.8.0 重写，见 {@link ElectricArmorDamage}）。
     *
     * <p>通过 {@link IElectricArmorItem#getDamageAbsorptionRatio(EquipmentSlot)} 和
     * {@link IElectricArmorItem#getEnergyPerDamage()} 两个 API 方法驱动伤害吸收逻辑，
     * Addon 开发者只需实现这两个方法即可自定义装甲的伤害吸收行为。
     *
     * <ul>
     *   <li>各部件比例相加（不再按剩余伤害连乘）；总比例上限 100%。量子全套 = 100%。</li>
     *   <li>纳米全套：总比例 90%。</li>
     *   <li>全套（四件同一 tier）：耗电由四件按剩余电量比例共同承担。</li>
     *   <li>可吸收除虚空、/kill、饥饿、世界边界以外的全部原版伤害（含魔法、凋零、冰冻、音波、破甲攻击）。</li>
     * </ul>
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

        boolean isRadiation = source.getMsgId().equals("mio_icif_killed_by_uranium");

        boolean radiationProtected = RadiationProtectionUtil.isRadiationProtected(entity);

        if (radiationProtected && isRadiation) {
            event.setNewDamage(0);
            return;
        }

        if (RadiationProtectionUtil.isWearingFullHazmat(entity) && hazmatAbsorbs(source)) {
            event.setNewDamage(0);
            return;
        }

        // 0.1.8.0: additive ratios, full-set energy pool, all vanilla damage types
        // except void / kill / starvation / world border. See ElectricArmorDamage.
        var result = ElectricArmorDamage.absorb(entity, source, damage);
        if (result.absorbed() > 0) {
            event.setNewDamage(Math.max(0, damage - result.absorbed()));
        }
    }

    /**
     * Exhaustion of a player before a hit that electric armor (or the deflector)
     * reduced. Vanilla adds the food exhaustion of the damage type after the
     * Pre event. A powered suit carries that load, so the Post event restores it.
     */
    private static final java.util.Map<Player, Float> HIT_EXHAUSTION = new java.util.WeakHashMap<>();

    /** Runs after the armor handler and the deflector (LOW): records the exhaustion of a reduced hit. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onLivingDamageGuard(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (event.getNewDamage() < event.getOriginalDamage() && wearsPoweredElectricArmor(player))
            HIT_EXHAUSTION.put(player, player.getFoodData().getExhaustionLevel());
        else HIT_EXHAUSTION.remove(player);
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        Float before = HIT_EXHAUSTION.remove(player);
        if (before != null && player.getFoodData().getExhaustionLevel() > before)
            player.getFoodData().setExhaustion(before);
    }

    private static boolean wearsPoweredElectricArmor(LivingEntity entity) {
        var api = MioIcifAPI.instance().getItemAPI();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && api.isElectricArmor(stack) && api.getElectricArmorStored(stack) > 0) return true;
        }
        return false;
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
     * 按已加载的相邻核心数量检查反应仓，失去唯一连接时正常移除并掉落。
     */
    private static void checkChamberMultipleReactors(Level level,BlockPos chamberPos) {
        com.miophas.singularity_iteration.common.reactor.ChamberTopology.checkAndDrop(level,chamberPos);
    }

    /**
     * 玩家退出时清理按键状态引用
     * 对应IC2：Keyboard.removePlayerReferences()
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        mio_icif_KeyboardManager.removePlayerReferences(event.getEntity());
        com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController.removePlayer(event.getEntity());
    }

    /** Drop per-dimension explosion work as soon as a level unloads. */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            NukeExplosionScheduler.clear(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Held key snapshots remain valid; only activity in the old dimension expires.
        com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController.removePlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController.removePlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onChestEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.CHEST
                && event.getFrom().getItem() != event.getTo().getItem()
                && event.getEntity() instanceof Player player) {
            com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController.removePlayer(player);
        }
    }

    private static boolean hazmatAbsorbs(DamageSource source) {
        return source.is(DamageTypes.LAVA)
            || source.is(DamageTypes.IN_FIRE)
            || source.is(DamageTypes.ON_FIRE)
            || source.is(DamageTypes.HOT_FLOOR)
            || source.is(DamageTypes.LIGHTNING_BOLT)
            || source.getMsgId().equals("mio_icif_killed_by_uranium");
    }
}