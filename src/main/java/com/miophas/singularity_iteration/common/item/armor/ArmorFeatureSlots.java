package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * 装备特性可管理槽位提供者。
 *
 * <p>统一为 Tooltip 交互、特性管理 GUI、命令与网络包提供"玩家身上可切换特性的装备槽"：
 * <ul>
 *   <li>原版四格护甲槽（head / chest / legs / feet）；</li>
 *   <li>可选的外部背槽（Curios {@code back}），仅在对应模组存在且槽内有装备特性物品时出现。</li>
 * </ul>
 *
 * <p>Curios 为可选依赖，通过反射调用 {@code JetpackCuriosAdapter}，无 Curios 时自动退化为仅原版四格。
 * 该方法不区分具体物品类型：任何实现 {@code IElectricArmorItem} 的物品都会被纳入。
 */
public final class ArmorFeatureSlots {

    /** 背槽槽位标识。 */
    public static final String BACK = "back";
    /** Accessory slot ids: {@code curio:<slotType>:<index>}. */
    public static final String CURIO_PREFIX = "curio:";

    private static final EquipmentSlot[] VANILLA = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * 一个可管理特性的装备槽。
     *
     * @param id            槽位标识（head/chest/legs/feet/back）
     * @param equipmentSlot 原版装备槽；背槽等扩展槽为 {@code null}
     * @param stack         物品堆（可能为空）
     */
    public record EquippedSlot(String id, @Nullable EquipmentSlot equipmentSlot, ItemStack stack) {
    }

    private ArmorFeatureSlots() {
    }

    /**
     * 列出玩家身上全部可管理特性的装备槽（含空槽，便于 GUI 显示空行）。
     */
    public static List<EquippedSlot> equipped(Player player) {
        List<EquippedSlot> slots = new ArrayList<>(VANILLA.length + 1);
        if (player == null) {
            return slots;
        }
        for (EquipmentSlot slot : VANILLA) {
            slots.add(new EquippedSlot(slot.getName().toLowerCase(), slot, player.getItemBySlot(slot)));
        }
        ItemStack back = backSlotStack(player);
        if (!back.isEmpty()) {
            slots.add(new EquippedSlot(BACK, null, back));
        }
        // accessories (rings, belts, necklaces...) with tuning / modes / charge, so the console manages them too
        for (Object[] accessory : accessorySlots(player)) {
            slots.add(new EquippedSlot((String) accessory[0], null, (ItemStack) accessory[1]));
        }
        return slots;
    }

    /**
     * 解析原版装备槽标识。
     *
     * @return 非原版槽位标识返回 {@code null}
     */
    @Nullable
    public static EquipmentSlot equipmentSlot(String id) {
        if (id == null) {
            return null;
        }
        return switch (id.toLowerCase()) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    /**
     * 按槽位标识读取物品堆。
     */
    public static ItemStack stackById(Player player, String id) {
        if (player == null || id == null) {
            return ItemStack.EMPTY;
        }
        if (BACK.equalsIgnoreCase(id)) {
            return backSlotStack(player);
        }
        if (id.startsWith(CURIO_PREFIX)) {
            return accessoryStack(player, id);
        }
        EquipmentSlot slot = equipmentSlot(id);
        return slot == null ? ItemStack.EMPTY : player.getItemBySlot(slot);
    }

    /**
     * 把修改后的物品堆写回指定槽位。
     *
     * @return 是否已交由槽位宿主处理
     */
    public static boolean write(Player player, String id, ItemStack stack) {
        if (player == null || id == null || stack == null) {
            return false;
        }
        EquipmentSlot slot = equipmentSlot(id);
        if (slot != null) {
            player.setItemSlot(slot, stack);
            return true;
        }
        if (BACK.equalsIgnoreCase(id)) {
            return writeBackSlotStack(player, stack);
        }
        if (id.startsWith(CURIO_PREFIX)) {
            return writeAccessory(player, id, stack);
        }
        return false;
    }

    /**
     * 在全部已装备槽位中查找声明了指定特性的物品堆。
     *
     * @return 未找到返回 {@link ItemStack#EMPTY}
     */
    public static ItemStack findDeclaring(Player player, String featureKey) {
        for (EquippedSlot slot : equipped(player)) {
            if (!slot.stack().isEmpty() && ArmorFeatures.isDeclared(slot.stack(), null, featureKey)) {
                return slot.stack();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 在全部已装备槽位中查找声明了指定特性的槽位标识。
     *
     * @return 未找到返回 {@code null}
     */
    @Nullable
    public static String findDeclaringSlotId(Player player, String featureKey) {
        for (EquippedSlot slot : equipped(player)) {
            if (!slot.stack().isEmpty() && ArmorFeatures.isDeclared(slot.stack(), null, featureKey)) {
                return slot.id();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> accessorySlots(Player player) {
        CuriosBridge.resolve();
        if (player == null || !CuriosBridge.available) return List.of();
        try {
            Object result = CuriosBridge.ACCESSORIES.invoke(null, player);
            return result instanceof List<?> list ? (List<Object[]>) list : List.of();
        } catch (ReflectiveOperationException | LinkageError failure) {
            CuriosBridge.fail(failure);
            return List.of();
        }
    }

    /** Every item in the player's Curios slots (any slot type, cosmetic slots excluded); empty without Curios. */
    @SuppressWarnings("unchecked")
    public static List<ItemStack> accessoryStacks(Player player) {
        CuriosBridge.resolve();
        if (player == null || !CuriosBridge.available) return List.of();
        try {
            Object result = CuriosBridge.ALL_ACCESSORIES.invoke(null, player);
            return result instanceof List<?> list ? (List<ItemStack>) list : List.of();
        } catch (ReflectiveOperationException | LinkageError failure) {
            CuriosBridge.fail(failure);
            return List.of();
        }
    }

    private static ItemStack accessoryStack(Player player, String id) {
        CuriosBridge.resolve();
        if (!CuriosBridge.available) return ItemStack.EMPTY;
        try {
            return CuriosBridge.ACCESSORY_STACK.invoke(null, player, id) instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | LinkageError failure) {
            CuriosBridge.fail(failure);
            return ItemStack.EMPTY;
        }
    }

    private static boolean writeAccessory(Player player, String id, ItemStack stack) {
        CuriosBridge.resolve();
        if (!CuriosBridge.available) return false;
        try {
            return CuriosBridge.WRITE_ACCESSORY.invoke(null, player, id, stack) instanceof Boolean b && b;
        } catch (ReflectiveOperationException | LinkageError failure) {
            CuriosBridge.fail(failure);
            return false;
        }
    }

    // ==================== 可选背槽（Curios，反射调用） ====================

    /** 读取背槽中可管理特性的物品堆；无 Curios 或无内容时返回空。 */
    public static ItemStack backSlotStack(Player player) {
        CuriosBridge.resolve();
        if (player == null || !CuriosBridge.available) {
            return ItemStack.EMPTY;
        }
        try {
            Object result = CuriosBridge.BACK_STACK.invoke(null, player);
            return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | LinkageError failure) {
            CuriosBridge.fail(failure);
            return ItemStack.EMPTY;
        }
    }

    private static boolean writeBackSlotStack(Player player, ItemStack stack) {
        CuriosBridge.resolve();
        if (!CuriosBridge.available) {
            return false;
        }
        try {
            Object result = CuriosBridge.WRITE_BACK.invoke(null, player, stack);
            return result instanceof Boolean written && written;
        } catch (ReflectiveOperationException | LinkageError failure) {
            CuriosBridge.fail(failure);
            return false;
        }
    }

    /** 惰性解析可选 Curios 桥；缺失时静默退化。 */
    private static final class CuriosBridge {
        private static final String ADAPTER =
            "com.miophas.singularity_iteration.common.integration.curios.JetpackCuriosAdapter";

        private static boolean resolved;
        private static boolean available;
        private static boolean failed;
        private static Method BACK_STACK;
        private static Method WRITE_BACK;
        private static Method ACCESSORIES;
        private static Method ACCESSORY_STACK;
        private static Method WRITE_ACCESSORY;
        private static Method ALL_ACCESSORIES;

        private CuriosBridge() {
        }

        static synchronized void resolve() {
            if (resolved) {
                return;
            }
            resolved = true;
            if (!ModList.get().isLoaded("curios")) {
                return;
            }
            try {
                Class<?> type = Class.forName(ADAPTER);
                BACK_STACK = type.getMethod("backFeatureStack", Player.class);
                WRITE_BACK = type.getMethod("writeBackFeatureStack", Player.class, ItemStack.class);
                ACCESSORIES = type.getMethod("accessorySlots", Player.class);
                ACCESSORY_STACK = type.getMethod("accessoryStack", Player.class, String.class);
                WRITE_ACCESSORY = type.getMethod("writeAccessory", Player.class, String.class, ItemStack.class);
                ALL_ACCESSORIES = type.getMethod("allAccessoryStacks", Player.class);
                available = true;
            } catch (ReflectiveOperationException | LinkageError failure) {
                fail(failure);
            }
        }

        static void fail(Throwable failure) {
            available = false;
            if (!failed) {
                failed = true;
                Singularity_Iteration.LOGGER.warn("Curios back-slot feature integration is unavailable", failure);
            }
        }
    }
}
