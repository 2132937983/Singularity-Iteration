package com.singularity_iteration.mio_icif.util;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 喷气背包按键状态管理器 - 服务端部分
 * 基于IC2 Keyboard实现，独立存储按键状态，不受受伤影响
 * 
 * <p>注意：此类在服务端和客户端都会加载，不能引用客户端专有类
 */
public class JetpackKeyHandler {
    
    // 服务端存储的玩家按键状态
    private static final Map<Player, KeyState> playerKeyStates = new WeakHashMap<>();
    
    /**
     * 按键状态类
     */
    public static class KeyState {
        public boolean jump = false;
        public boolean forward = false;
        public boolean sneak = false;
        public boolean modeSwitch = false;
        public boolean sprint = false;  // Boost键（冲刺键）
        
        public int toInt() {
            int state = 0;
            if (jump) state |= 1;
            if (forward) state |= 2;
            if (sneak) state |= 4;
            if (modeSwitch) state |= 8;
            if (sprint) state |= 16;
            return state;
        }
        
        public static KeyState fromInt(int state) {
            KeyState ks = new KeyState();
            ks.jump = (state & 1) != 0;
            ks.forward = (state & 2) != 0;
            ks.sneak = (state & 4) != 0;
            ks.modeSwitch = (state & 8) != 0;
            ks.sprint = (state & 16) != 0;
            return ks;
        }
    }
    
    /**
     * 服务端处理按键状态更新
     */
    public static void processKeyUpdate(Player player, int keyState) {
        playerKeyStates.put(player, KeyState.fromInt(keyState));
    }
    
    /**
     * 获取玩家的跳跃键状态
     * 这是关键：使用独立存储的状态，不受受伤影响
     */
    public static boolean isJumpKeyDown(Player player) {
        // 客户端：通过客户端处理器检测按键
        if (player.level().isClientSide) {
            return JetpackKeyHandlerClient.isJumpKeyDownClient();
        }
        // 服务端：从存储的状态读取
        KeyState state = playerKeyStates.get(player);
        return state != null && state.jump;
    }
    
    /**
     * 获取玩家的前进键状态
     */
    public static boolean isForwardKeyDown(Player player) {
        if (player.level().isClientSide) {
            return JetpackKeyHandlerClient.isForwardKeyDownClient();
        }
        KeyState state = playerKeyStates.get(player);
        return state != null && state.forward;
    }
    
    /**
     * 获取玩家的潜行键状态
     */
    public static boolean isSneakKeyDown(Player player) {
        if (player.level().isClientSide) {
            return JetpackKeyHandlerClient.isSneakKeyDownClient();
        }
        KeyState state = playerKeyStates.get(player);
        return state != null && state.sneak;
    }
    
    /**
     * 获取玩家的模式切换键状态
     */
    public static boolean isModeSwitchKeyDown(Player player) {
        if (player.level().isClientSide) {
            return JetpackKeyHandlerClient.isModeSwitchKeyDownClient();
        }
        KeyState state = playerKeyStates.get(player);
        return state != null && state.modeSwitch;
    }
    
    /**
     * 获取玩家的Alt键状态（用于功能切换）
     * 在IC2中，Alt键通常与其他键组合使用来切换功能
     */
    public static boolean isAltKeyDown(Player player) {
        // 客户端：直接检测左Alt或右Alt键
        if (player.level().isClientSide) {
            return JetpackKeyHandlerClient.isAltKeyDownClient();
        }
        // 服务端：Alt键是瞬时状态，不存储，只在客户端检测
        return false;
    }
    
    /**
     * 获取玩家的Boost键状态（冲刺键，默认左Ctrl）
     * 在IC2中，Boost键用于量子靴的高跳功能
     */
    public static boolean isBoostKeyDown(Player player) {
        // 客户端：检测左Ctrl键（冲刺键）
        if (player.level().isClientSide) {
            return JetpackKeyHandlerClient.isBoostKeyDownClient();
        }
        // 服务端：从同步的KeyState中获取
        KeyState state = playerKeyStates.get(player);
        return state != null && state.sprint;
    }

    /**
     * 玩家断开连接时清理状态
     */
    public static void removePlayer(Player player) {
        playerKeyStates.remove(player);
    }
}