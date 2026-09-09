package com.singularity_iteration.mio_icif.network;

import net.minecraft.world.entity.player.Player;

import java.util.WeakHashMap;

/**
 * 服务端按键状态管理器（IC2RIn120原版移植�?
 *
 * 对应IC2：ic2.core.util.Keyboard
 * 存储每个玩家的按键状态，供服务端inventoryTick使用
 */
@SuppressWarnings("null")
public class mio_icif_KeyboardManager {

    // 按键状态位定义（对应IC2 Keyboard.Key�?
    public static final int KEY_JUMP = 0;      // 跳跃键（空格�?
    public static final int KEY_BOOST = 1;     // Boost键（左Control�?
    public static final int KEY_FORWARD = 2;   // 前进键（W�?
    public static final int KEY_SNEAK = 3;     // 疾跑键（左Shift）
    public static final int KEY_SAFETY = 4;    // 防呆按键（右Alt）

    // 存储每个玩家的按键状态（WeakHashMap自动清理离线玩家�?
    private static final WeakHashMap<Player, Integer> playerKeyStates = new WeakHashMap<>();

    /**
     * 处理按键状态更新?
     * 对应IC2：Keyboard.processKeyUpdate()
     */
    public static void processKeyUpdate(Player player, int keyState) {
        playerKeyStates.put(player, keyState);
    }

    /**
     * 检测跳跃键是否按下
     * 对应IC2：Keyboard.isJumpKeyDown()
     */
    public static boolean isJumpKeyDown(Player player) {
        return getKeyState(player, KEY_JUMP);
    }

    /**
     * 检测Boost键是否按�?
     * 对应IC2：Keyboard.isBoostKeyDown()
     */
    public static boolean isBoostKeyDown(Player player) {
        return getKeyState(player, KEY_BOOST);
    }

    /**
     * 检测前进键是否按下
     * 对应IC2：Keyboard.isForwardKeyDown()
     */
    public static boolean isForwardKeyDown(Player player) {
        return getKeyState(player, KEY_FORWARD);
    }

    /**
     * 检测疾跑键是否按下
     */
    public static boolean isSneakKeyDown(Player player) {
        return getKeyState(player, KEY_SNEAK);
    }

    /**
     * 检测防呆按键是否按下
     */
    public static boolean isSafetyKeyDown(Player player) {
        return getKeyState(player, KEY_SAFETY);
    }

    /**
     * 获取指定按键的状态?
     */
    private static boolean getKeyState(Player player, int keyIndex) {
        Integer keyState = playerKeyStates.get(player);
        if (keyState == null) return false;
        return (keyState & (1 << keyIndex)) != 0;
    }

    /**
     * 移除玩家引用（玩家退出时调用�?
     * 对应IC2：Keyboard.removePlayerReferences()
     */
    public static void removePlayerReferences(Player player) {
        playerKeyStates.remove(player);
    }
}