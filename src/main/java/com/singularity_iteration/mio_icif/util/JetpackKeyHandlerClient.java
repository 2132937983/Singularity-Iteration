package com.singularity_iteration.mio_icif.util;

import com.singularity_iteration.mio_icif.network.mio_icif_Network;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
// import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 喷气背包按键状态管理器 - 客户端部分
 * 仅在客户端加载，处理按键检测和网络发送
 */
@EventBusSubscriber(modid = "mio_icif", value = Dist.CLIENT)
public class JetpackKeyHandlerClient {
    
    // 使用mio_icif_ClientEvents中定义的JETPACK_MODE_KEY，避免重复定义
    // 模式切换键默认K键
    
    // 客户端当前按键状态
    private static JetpackKeyHandler.KeyState clientKeyState = new JetpackKeyHandler.KeyState();
    
    // 上次发送的按键状态（用于检测变化）
    private static int lastSentKeyState = 0;

    /**
     * 客户端tick事件 - 检测按键并发送状态
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // 只在没有打开GUI时检测按键
        Screen currentScreen = mc.screen;
        if (currentScreen != null) {
            // 打开GUI时，重置按键状态
            clientKeyState = new JetpackKeyHandler.KeyState();
            sendKeyUpdate();
            return;
        }
        
        // 检测按键
        clientKeyState.jump = mc.options.keyJump.isDown();
        clientKeyState.forward = mc.options.keyUp.isDown();
        clientKeyState.sneak = mc.options.keyShift.isDown();
        // 检测左Ctrl键（Boost键）
        clientKeyState.sprint = InputConstants.isKeyDown(mc.getWindow().getWindow(), InputConstants.KEY_LCONTROL);
        // 使用mio_icif_ClientEvents中定义的JETPACK_MODE_KEY检测模式切换
        clientKeyState.modeSwitch = com.singularity_iteration.mio_icif.client.mio_icif_ClientEvents.JETPACK_MODE_KEY != null 
            && com.singularity_iteration.mio_icif.client.mio_icif_ClientEvents.JETPACK_MODE_KEY.isDown();
        
        // 发送按键状态到服务端
        sendKeyUpdate();
    }
    
    /**
     * 发送按键状态到服务端
     */
    private static void sendKeyUpdate() {
        int currentState = clientKeyState.toInt();
        if (currentState != lastSentKeyState) {
            mio_icif_Network.sendJetpackKeyState(currentState);
            lastSentKeyState = currentState;
        }
    }
    
    // ========== 客户端按键状态查询方法 ==========
    
    /**
     * 获取客户端跳跃键状态
     */
    public static boolean isJumpKeyDownClient() {
        return clientKeyState.jump;
    }
    
    /**
     * 获取客户端前进键状态
     */
    public static boolean isForwardKeyDownClient() {
        return clientKeyState.forward;
    }
    
    /**
     * 获取客户端潜行键状态
     */
    public static boolean isSneakKeyDownClient() {
        return clientKeyState.sneak;
    }
    
    /**
     * 获取客户端模式切换键状态
     */
    public static boolean isModeSwitchKeyDownClient() {
        return clientKeyState.modeSwitch;
    }
    
    /**
     * 获取客户端Alt键状态
     */
    public static boolean isAltKeyDownClient() {
        Minecraft mc = Minecraft.getInstance();
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), InputConstants.KEY_LALT) ||
               InputConstants.isKeyDown(mc.getWindow().getWindow(), InputConstants.KEY_RALT);
    }
    
    /**
     * 获取客户端Boost键状态
     * 使用可配置的BOOST_KEY，而不是硬编码的左Ctrl
     */
    public static boolean isBoostKeyDownClient() {
        // 使用mio_icif_ClientEvents中定义的BOOST_KEY
        return com.singularity_iteration.mio_icif.client.mio_icif_ClientEvents.BOOST_KEY != null 
            && com.singularity_iteration.mio_icif.client.mio_icif_ClientEvents.BOOST_KEY.isDown();
    }
}