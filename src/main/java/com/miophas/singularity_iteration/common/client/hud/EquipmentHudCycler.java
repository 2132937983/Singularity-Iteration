package com.miophas.singularity_iteration.common.client.hud;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.item.IEquipmentHudProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 装备 HUD 循环显示器。
 *
 * <p>收集玩家主手、副手与护甲槽中所有实现 {@link IEquipmentHudProvider} 的物品所提供的文本，
 * 在同一位置（actionbar）按固定顺序轮流显示，每个条目停留 {@link #DWELL_TICKS} tick。
 * 这样多个装备同时提供状态时不会互相覆盖闪烁；不再提供文本时停止刷新，让原版 actionbar 自然淡出。
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public final class EquipmentHudCycler {

    /** 每个条目在 actionbar 上停留的 tick 数（40 tick = 2 秒）。 */
    private static final int DWELL_TICKS = 40;

    /** 本 tick 收集到的条目（用于替换显示集合）。 */
    private static final Map<String, Component> PENDING = new LinkedHashMap<>();
    /** 当前正在显示的条目（值每 tick 刷新，如能量）。 */
    private static final Map<String, Component> CURRENT = new LinkedHashMap<>();
    /** 当前条目的稳定顺序。 */
    private static final List<String> ORDER = new ArrayList<>();

    private static int index;
    private static int timer;

    private EquipmentHudCycler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            reset();
            return;
        }

        collect(player);

        // 没有任何装备提供 HUD 时停止刷新（不主动清空 actionbar，让其自然淡出）
        if (PENDING.isEmpty()) {
            reset();
            return;
        }

        boolean sameSet = PENDING.keySet().equals(CURRENT.keySet());
        CURRENT.clear();
        CURRENT.putAll(PENDING);
        PENDING.clear();

        if (!sameSet) {
            ORDER.clear();
            ORDER.addAll(CURRENT.keySet());
            index = 0;
            timer = DWELL_TICKS;
        }

        if (ORDER.size() > 1) {
            if (--timer <= 0) {
                index = (index + 1) % ORDER.size();
                timer = DWELL_TICKS;
            }
        } else {
            index = 0;
        }

        minecraft.gui.setOverlayMessage(CURRENT.get(ORDER.get(index)), false);
    }

    private static void collect(Player player) {
        submit("main", player.getMainHandItem());
        submit("off", player.getOffhandItem());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                submit("armor_" + slot.getName(), player.getItemBySlot(slot));
            }
        }
    }

    private static void submit(String key, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IEquipmentHudProvider provider)) return;
        Component text = provider.getEquipmentHudText(stack);
        if (text != null) PENDING.put(key, text);
    }

    private static void reset() {
        PENDING.clear();
        CURRENT.clear();
        ORDER.clear();
        index = 0;
        timer = 0;
    }
}
