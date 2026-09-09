package com.singularity_iteration.mio_icif.client;

import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_foam;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.tool.ToggleableElectricTool;
import com.singularity_iteration.mio_icif.Screen.mio_icif_gui_armor_features;
import com.singularity_iteration.mio_icif.client.checker.mio_icif_checker_renderer;

import com.singularity_iteration.mio_icif.network.KeyboardStatePacket;
import com.singularity_iteration.mio_icif.network.ToolTogglePacket;
import com.singularity_iteration.mio_icif.network.mio_icif_KeyboardManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端事件处理器
 * 处理按键输入、泡沫遮挡叠加层等客户端事件
 *
 * IC2RIn120原版架构：
 * - 每tick检测按键状态，通过KeyboardStatePacket同步到服务端
 * - 服务端存储玩家按键状态，在inventoryTick中直接使用
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_ClientEvents {

    // K键 - 切换喷气背包模式（仅用于按键注册，实际检测在JetpackKeyHandlerClient中）
    public static KeyMapping JETPACK_MODE_KEY;

    // G键 - 切换电力工具开关
    public static KeyMapping TOOL_TOGGLE_KEY;

    // IC2RIn120原版：Boost键（对应IC2的boostKey，默认左Control）
    public static KeyMapping BOOST_KEY;

    // H键 - 打开装备特性管理GUI
    public static KeyMapping ARMOR_FEATURES_KEY;

    // 防呆按键（默认右Alt）- 取出高压升级时需要按住
    public static KeyMapping SAFETY_KEY;

    // 泡沫遮挡叠加层纹理（类似南瓜头效果）
    private static final ResourceLocation FOAM_OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Singularity_Iteration.MOD_ID, "textures/misc/foam_overlay.png"
    );

    // 泡沫叠加层淡出计时器（离开泡沫后渐隐）
    private static int foamOverlayFade = 0;
    private static final int FOAM_FADE_TICKS = 10; // 0.5秒淡出

    // IC2RIn120原版：按键状态同步
    private static int lastKeyState = 0;

    /**
     * 注册按键映射
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        JETPACK_MODE_KEY = new KeyMapping(
            "key.mio_icif.jetpack_mode",
            GLFW.GLFW_KEY_K,
            "key.category.mio_icif"
        );
        event.register(JETPACK_MODE_KEY);

        TOOL_TOGGLE_KEY = new KeyMapping(
            "key.mio_icif.tool_toggle",
            GLFW.GLFW_KEY_G,
            "key.category.mio_icif"
        );
        event.register(TOOL_TOGGLE_KEY);

        // IC2RIn120原版：Boost键（对应IC2的boostKey，默认左Control）
        BOOST_KEY = new KeyMapping(
            "key.mio_icif.boost",
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.category.mio_icif"
        );
        event.register(BOOST_KEY);

        // H键 - 打开装备特性管理GUI（默认左Alt）
        ARMOR_FEATURES_KEY = new KeyMapping(
            "key.mio_icif.armor_features",
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.category.mio_icif"
        );
        event.register(ARMOR_FEATURES_KEY);

        // 防呆按键（默认右Alt）- 取出高压升级时需要按住
        SAFETY_KEY = new KeyMapping(
            "key.mio_icif.safety",
            GLFW.GLFW_KEY_RIGHT_ALT,
            "key.category.mio_icif"
        );
        event.register(SAFETY_KEY);
    }

    /**
     * 客户端tick 事件 - 处理按键输入和泡沫叠加层淡出
     *
     * IC2RIn120原版：每tick发送按键状态到服务端
     * 对应KeyboardClient.sendKeyUpdate()
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null) return;

        // 泡沫叠加层淡出计时
        if (foamOverlayFade > 0) {
            foamOverlayFade--;
        }

        // 检查是否按下 G 键
        if (TOOL_TOGGLE_KEY != null && TOOL_TOGGLE_KEY.consumeClick()) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            if (mainHand.getItem() instanceof ToggleableElectricTool || offHand.getItem() instanceof ToggleableElectricTool) {
                PacketDistributor.sendToServer(new ToolTogglePacket());
            }
        }

        // 检测装备特性管理GUI按键
        if (ARMOR_FEATURES_KEY != null && ARMOR_FEATURES_KEY.consumeClick() && minecraft.screen == null) {
            // 按键按下且没有GUI打开时，打开装备特性管理GUI
            minecraft.setScreen(new mio_icif_gui_armor_features());
        }

        // IC2RIn120原版：发送按键状态到服务端
        sendKeyStateUpdate(player);

        // 充电座粒子特效
        spawnChargerParticles(minecraft);
    }

    /**
     * IC2RIn120原版：发送按键状态到服务端
     * 对应KeyboardClient.sendKeyUpdate()
     *
     * 检测以下按键：
     * - 跳跃键（空格）
     * - Boost键（左Control）
     * - 前进键（W）
     * - 疾跑（左Shift）
     */
    private static void sendKeyStateUpdate(Player player) {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();

        int currentKeyState = 0;

        if (mc.screen == null) {
            // 检测跳跃键（空格）- 对应IC2 Key.jump
            if (mc.options.keyJump.isDown()) {
                currentKeyState |= 1 << 0;
            }

            // 检测Boost键（左Control） 对应IC2 Key.boost
            if (BOOST_KEY != null && BOOST_KEY.isDown()) {
                currentKeyState |= 1 << 1;
            }

            // 检测前进键（W） 对应IC2 Key.forward
            if (mc.options.keyUp.isDown()) {
                currentKeyState |= 1 << 2;
            }

            // 检测疾跑键（左Shift） 对应IC2 Key.sneak
            if (mc.options.keyShift.isDown()) {
                currentKeyState |= 1 << 3;
            }
        }

        // 检测防呆按键（默认右Alt）- 使用GLFW直接检测，确保GUI打开时也能工作
        boolean safetyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
        if (safetyDown) {
            currentKeyState |= 1 << mio_icif_KeyboardManager.KEY_SAFETY;
        }

        mio_icif_KeyboardManager.processKeyUpdate(player, currentKeyState);

        if (currentKeyState != lastKeyState) {
            lastKeyState = currentKeyState;
            PacketDistributor.sendToServer(new KeyboardStatePacket(currentKeyState));
        }
    }

    /**
     * 渲染GUI事件 - 处理电压检测器的显示和泡沫遮挡叠加层
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        mio_icif_checker_renderer.onRenderGui(event);

        renderItemHudInfo(event);

        renderFoamOverlay(event);
    }

    /**
     * 渲染手持物品的 HUD 信息（IItemHudInfo / IItemHudBarProvider）
     */
    private static void renderItemHudInfo(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return;

        if (heldItem.getItem() instanceof com.singularity_iteration.mio_icif.api.util.IItemHudInfo hudInfo) {
            GuiGraphics gui = event.getGuiGraphics();
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();

            boolean extended = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            java.util.List<String> lines = hudInfo.getHudInfo(heldItem, extended);
            int y = screenHeight / 2 + 10;
            for (String line : lines) {
                int x = screenWidth / 2 - minecraft.font.width(line) / 2;
                gui.drawString(minecraft.font, line, x, y, 0xFFFFFF);
                y += 10;
            }

            if (heldItem.getItem() instanceof com.singularity_iteration.mio_icif.api.util.IItemHudInfo.IItemHudBarProvider barProvider) {
                int barWidth = 80;
                int barHeight = 5;
                int barX = screenWidth / 2 - barWidth / 2;
                int percent = barProvider.getBarPercent(heldItem);
                gui.fill(barX, y, barX + barWidth, y + barHeight, 0xFF333333);
                gui.fill(barX, y, barX + barWidth * percent / 100, y + barHeight, 0xFF00FF00);
            }
        }
    }

    /**
     * 渲染泡沫遮挡叠加层（类似南瓜头效果）
     * 当玩家头部在泡沫方块内时，屏幕被泡沫遮挡
     * 离开泡沫后有0.5秒淡出效果
     */
    private static void renderFoamOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        // 检查玩家头部位置是否在泡沫方块内
        double eyeY = player.getY() + player.getEyeHeight();
        BlockPos eyePos = BlockPos.containing(player.getX(), eyeY, player.getZ());
        BlockState eyeState = minecraft.level.getBlockState(eyePos);

        boolean inFoam = eyeState.getBlock() instanceof mio_icif_block_foam;

        if (inFoam) {
            foamOverlayFade = FOAM_FADE_TICKS;
        }

        // 只有在泡沫中或淡出期间才渲染
        if (foamOverlayFade <= 0) return;

        // 计算透明度（淡出时逐渐降低）
        float alpha = inFoam ? 1.0F : (float) foamOverlayFade / FOAM_FADE_TICKS;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // 渲染泡沫遮挡叠加层
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(FOAM_OVERLAY_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static final org.joml.Vector3f CHARGE_PARTICLE_COLOR = new org.joml.Vector3f(0.2F, 0.2F, 1.0F);
    private static final net.minecraft.core.particles.DustParticleOptions CHARGE_EFFECT =
        new net.minecraft.core.particles.DustParticleOptions(CHARGE_PARTICLE_COLOR, 1.0F);

    private static void spawnChargerParticles(Minecraft minecraft) {
        if (minecraft.level == null) return;
        net.minecraft.world.level.Level level = minecraft.level;
        net.minecraft.util.RandomSource rnd = level.random;

        if (rnd.nextInt(8) != 0) return;

        Player player = minecraft.player;
        if (player == null) return;

        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;

        for (int cx = playerChunkX - 2; cx <= playerChunkX + 2; cx++) {
            for (int cz = playerChunkZ - 2; cz <= playerChunkZ + 2; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cx, cz);
                for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.getBlockState().getBlock() instanceof com.singularity_iteration.mio_icif.api.block.IChargepadBlock chargepad) {
                        BlockPos bePos = be.getBlockPos();
                        BlockState beState = be.getBlockState();

                        if (chargepad.isCharging(beState)) {
                            for (int particles = 20; particles > 0; particles--) {
                                double x = bePos.getX() + rnd.nextFloat();
                                double y = bePos.getY() + 0.9D + rnd.nextFloat();
                                double z = bePos.getZ() + rnd.nextFloat();
                                level.addParticle(CHARGE_EFFECT, x, y, z, 0.0D, 0.1D, 0.0D);
                            }
                        }
                    }
                }
            }
        }
    }
}