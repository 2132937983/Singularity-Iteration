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
 * SI equivalent of IC2 1.12.2 {@code ElectricItemManager.chargeFromArmor()}.
 *
 * <p>Energy packs have no tick logic of their own. They are only passive energy sources. The user (the electric item in hand)
 * starts the charge: before the item reads or uses energy, it takes energy from the worn energy packs to fill itself. This agrees with
 * the original IC2 behavior "a tool takes energy from the armor when it is used".
 *
 * <p>As this alignment requires, there is no tier limit.
 */
public final class EnergyPackCharging {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    /** Item in hand -> user. The key is the ItemStack identity. There is no strong reference. */
    private static final Map<ItemStack, ServerPlayer> HOLDERS = Collections.synchronizedMap(new WeakHashMap<>());
    /** Item in hand -> game tick of the last charge. It limits the charge to "one time per tick at most". */
    private static final Map<ItemStack, Long> LAST_PULL_TICK = Collections.synchronizedMap(new WeakHashMap<>());
    /** During a charge, an energy read on the target item starts a charge again. This guard stops the recursion. */
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private EnergyPackCharging() { }

    /**
     * The item calls this method from {@code inventoryTick} each tick. It records "who holds the item".
     * Only items in a hand take energy from the armor.
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
     * Charge entry point before energy is read or used (the same call time as IC2 chargeFromArmor).
     *
     * <p>Important rule: the same item in hand charges **one time per tick at most**. The charge starts from {@code getEnergy()}.
     * {@code BatteryTransfer} operations use the rule "repeated reads in the same tick return the same value" to verify invariants
     * (for example, {@code consume()} reads one time before it removes energy and one time after to verify). If the second read fills the item again,
     * the operation sees "the balance changed after commit" and throws an exception. Thus this guard allows one charge per tick.
     * It makes sure that the subsequent reads in one operation have no side effects.
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
     * Charges the target item from the energy packs that the entity wears, until the target is full or all packs are empty.
     * Server thread only. On another thread or on the client, this method returns 0.
     * @param target the item stack to charge (usually the electric item in hand)
     * @param entity the holder
     * @return the energy that was put into the target item (EU)
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
