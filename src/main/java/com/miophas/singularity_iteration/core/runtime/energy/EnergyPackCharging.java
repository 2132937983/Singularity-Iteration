// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IEnergyPackItem;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * IC2 1.12.2 {@code ElectricItemManager.chargeFromArmor()} 的 SI 等价实现。
 *
 * <p>充电背包自身没有任何 tick 逻辑，它们只是被动的能量源；充电由使用方（手持的电动物品）
 * 主动发起：物品在读取/消耗能量前从穿戴的充电背包中取电补满自己，与 IC2 原版
 * "工具使用时从护甲取电"一致。
 *
 * <p>按本次对齐要求，不做 Tier 限制。
 */
public final class EnergyPackCharging {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    /** 手持中的物品 -> 使用者。以 ItemStack 身份为键，不做强引用。 */
    private static final Map<ItemStack, ServerPlayer> HOLDERS = Collections.synchronizedMap(new WeakHashMap<>());
    /** 手持物品 -> 最近一次补电的游戏刻，用于限制"每 tick 最多补一次"。 */
    private static final Map<ItemStack, Long> LAST_PULL_TICK = Collections.synchronizedMap(new WeakHashMap<>());
    /** 补电过程中目标物品的能量读取会再次触发补电，用守卫阻断递归。 */
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private EnergyPackCharging() { }

    /**
     * 由物品的 {@code inventoryTick} 每 tick 调用，记录"谁手持着它"；
     * 只有手持中的物品才会从护甲取电。
     */
    public static void trackHolder(ItemStack stack, Entity entity) {
        if (!(entity instanceof ServerPlayer player) || stack.getCount() != 1) return;
        if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) {
            HOLDERS.put(stack, player);
        } else {
            HOLDERS.remove(stack);
        }
    }

    /**
     * 读取/消耗能量前的补电入口（对应 IC2 chargeFromArmor 的调用时机）。
     *
     * <p>关键约束：同一个手持物品**每 tick 最多补一次**。因为补电是从 {@code getEnergy()} 触发的，
     * 而 {@code BatteryTransfer} 的操作依赖"同一 tick 内重复读取返回同一数值"来校验不变量
     * （如 {@code consume()} 扣电前读一次、扣电后再读一次做核对）。若第二次读取又补满，
     * 就会被判定为"提交后的余额被篡改"并抛出异常。因此这里用每 tick 一次的守卫，
     * 保证一次操作内部后续的读取是纯的。
     */
    public static void pullFromArmor(ItemStack stack) {
        ServerPlayer holder = HOLDERS.get(stack);
        if (holder == null) return;
        long tick = holder.level().getGameTime();
        Long previous = LAST_PULL_TICK.get(stack);
        if (previous != null && previous == tick) return;
        LAST_PULL_TICK.put(stack, tick);
        chargeFromArmor(stack, holder);
    }

    /**
     * 从实体穿戴的充电背包向目标物品补电，直到目标充满或所有背包耗尽。
     *
     * @param target 需要充电的物品堆（通常是手持的电动物品）
     * @param entity 持有者
     * @return 实际充入目标物品的能量
     */
    public static long chargeFromArmor(ItemStack target, LivingEntity entity) {
        if (Boolean.TRUE.equals(ACTIVE.get())) return 0;
        if (target.isEmpty() || target.getCount() != 1) return 0;
        if (!(target.getItem() instanceof IBatteryItem receiver)) return 0;
        if (!(entity.level() instanceof ServerLevel server) || !server.getServer().isSameThread()) return 0;
        ACTIVE.set(Boolean.TRUE);
        try {
            long movedTotal = 0;
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                if (receiver.isFull(target)) break;
                ItemStack donor = entity.getItemBySlot(slot);
                if (!(donor.getItem() instanceof IEnergyPackItem)) continue;
                if (!(donor.getItem() instanceof IBatteryItem battery)) continue;
                long available = battery.getEnergy(donor);
                if (available <= 0) continue;
                movedTotal += BatteryTransfer.move(donor, battery, target, receiver, available);
            }
            return movedTotal;
        } finally {
            ACTIVE.set(Boolean.FALSE);
        }
    }
}
