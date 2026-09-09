package com.singularity_iteration.mio_icif.client;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 核爆辐射尘客户端管理器
 * 处理核爆区域的迷雾效果和天空变暗
 * 参考玄武岩三角洲的迷雾效果
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public class NuclearFalloutClientManager {

    // 辐射区域检测半径
    private static final int CHECK_RADIUS = 64;
    // 辐射影响最大半径
    private static final int MAX_EFFECT_RADIUS = 128;
    // 迷雾密度
    private static final float FOG_DENSITY = 0.15f;
    // 天空变暗程度 (0-1, 越大越暗)
    @SuppressWarnings("unused")
    private static final float SKY_DARKENING = 0.6f;

    // 当前玩家是否在辐射区域
    private static boolean inRadiationZone = false;
    // 距离最近的辐射方块的距离
    @SuppressWarnings("unused")
    private static double distanceToRadiation = Double.MAX_VALUE;
    // 辐射强度 (0-1)
    private static float radiationIntensity = 0.0f;
    // 目标辐射强度（用于平滑过渡）
    private static float targetRadiationIntensity = 0.0f;

    /**
     * 客户端tick事件 - 检测玩家是否在辐射区域
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.player == null) {
            inRadiationZone = false;
            targetRadiationIntensity = 0.0f;
            return;
        }

        Vec3 playerPos = minecraft.player.position();
        BlockPos playerBlockPos = minecraft.player.blockPosition();

        // 检测周围是否有辐射方块
        double closestDistance = Double.MAX_VALUE;
        boolean foundRadiation = false;

        // 只在玩家移动一定距离后或每隔几tick检测一次，优化性能
        if (level.getGameTime() % 5 == 0) {
            int checkRange = CHECK_RADIUS;

            for (int x = -checkRange; x <= checkRange; x += 4) {
                for (int y = -checkRange / 2; y <= checkRange / 2; y += 4) {
                    for (int z = -checkRange; z <= checkRange; z += 4) {
                        BlockPos checkPos = playerBlockPos.offset(x, y, z);
                        BlockState state = level.getBlockState(checkPos);
                        Block block = state.getBlock();

                        if (block == mio_icif_blocks.BLOCK_RADIATING_STONE.get() ||
                            block == mio_icif_blocks.BLOCK_RADIATING_DIRT.get() ||
                            block == mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get()) {

                            double dx = playerPos.x - (checkPos.getX() + 0.5);
                            double dy = playerPos.y - (checkPos.getY() + 0.5);
                            double dz = playerPos.z - (checkPos.getZ() + 0.5);
                            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                            if (dist < closestDistance) {
                                closestDistance = dist;
                            }
                            foundRadiation = true;
                        }
                    }
                }
            }

            distanceToRadiation = closestDistance;
            inRadiationZone = foundRadiation;

            // 计算目标辐射强度
            if (foundRadiation && closestDistance < MAX_EFFECT_RADIUS) {
                // 距离越近，强度越大
            targetRadiationIntensity = 1.0f - (float) (closestDistance / MAX_EFFECT_RADIUS);
                targetRadiationIntensity = Math.max(0.0f, Math.min(1.0f, targetRadiationIntensity));
            } else {
                targetRadiationIntensity = 0.0f;
            }
        }

        // 平滑过渡辐射强度
        radiationIntensity = Mth.lerp(0.05f, radiationIntensity, targetRadiationIntensity);

        // 在辐射区域内生成粒子效果（辐射尘）
    if (radiationIntensity > 0.1f) {
            spawnFalloutParticles(level, playerPos);
        }
    }

    /**
     * 生成辐射尘粒子 - 类似玄武岩三角洲的白色灰烬效果
     */
    private static void spawnFalloutParticles(ClientLevel level, Vec3 playerPos) {
        // 根据辐射强度决定粒子生成概率
        if (level.random.nextFloat() > radiationIntensity * 0.5f) {
            return;
        }

        // 在玩家周围随机位置生成粒子
    double offsetX = (level.random.nextDouble() - 0.5) * 48;
        double offsetY = level.random.nextDouble() * 30 + 5;
        double offsetZ = (level.random.nextDouble() - 0.5) * 48;

        double x = playerPos.x + offsetX;
        double y = playerPos.y + offsetY;
        double z = playerPos.z + offsetZ;

        BlockPos pos = BlockPos.containing(x, y, z);

        // 只在露天位置生成
        if (level.canSeeSky(pos)) {
            // 使用白色灰烬粒子（类似玄武岩三角洲）
            // 或者使用大型烟雾粒子
        if (level.random.nextFloat() < 0.7f) {
                // 白色灰烬粒子 - 类似下界灰烬
                level.addParticle(
                    ParticleTypes.WHITE_ASH,
                    x, y, z,
                    (level.random.nextDouble() - 0.5) * 0.1,
                    -0.05 - level.random.nextDouble() * 0.1,
                    (level.random.nextDouble() - 0.5) * 0.1
                );
            } else {
                // 大型烟雾粒子 - 更浓的辐射尘
                level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    x, y, z,
                    (level.random.nextDouble() - 0.5) * 0.02,
                    -0.02 - level.random.nextDouble() * 0.03,
                    (level.random.nextDouble() - 0.5) * 0.02
                );
            }
        }
    }

    /**
     * 渲染迷雾事件 - 修改迷雾密度和颜色
     * 参考玄武岩三角洲的效果
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (radiationIntensity <= 0.01f) {
            return;
        }

        // 计算迷雾密度
        float density = FOG_DENSITY * radiationIntensity;

        // 设置迷雾范围 - 让迷雾更近更新
    float nearPlane = event.getNearPlaneDistance();
        float farPlane = event.getFarPlaneDistance();

        // 缩短可视距离，创造压抑感
        float newFarPlane = farPlane * (1.0f - density * 0.7f);
        float newNearPlane = nearPlane * (1.0f - density * 0.3f);

        event.setFarPlaneDistance(Math.max(newFarPlane, 20.0f));
        event.setNearPlaneDistance(Math.max(newNearPlane, 1.0f));

        // 设置圆柱形迷雾（类似下界效果）
    event.setFogShape(FogShape.CYLINDER);

        // 取消默认迷雾，使用我们的自定义设置
    event.setCanceled(true);
    }

    /**
     * 计算迷雾颜色 - 改为灰暗的辐射尘颜色
     * 参考玄武岩三角洲的灰暗色调
     */
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (radiationIntensity <= 0.01f) {
            return;
        }

        // 玄武岩三角洲风格的灰暗色调
    // 灰暗的棕灰色调，类似核冬天的天空
        float baseR = 0.35f; // 基础红色
        float baseG = 0.32f; // 基础绿色
        float baseB = 0.28f; // 基础蓝色

        // 获取原始颜色
        float originalR = event.getRed();
        float originalG = event.getGreen();
        float originalB = event.getBlue();

        // 混合原始颜色和辐射尘颜色
        float blendFactor = radiationIntensity * 0.8f;

        event.setRed(Mth.lerp(blendFactor, originalR, baseR));
        event.setGreen(Mth.lerp(blendFactor, originalG, baseG));
        event.setBlue(Mth.lerp(blendFactor, originalB, baseB));
    }

    /**
     * 渲染天空后事件 - 添加辐射尘天空覆盖层
     * 通过渲染一个覆盖层来让天空变暗
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        if (radiationIntensity <= 0.01f) {
            return;
        }

        // 渲染辐射尘天空覆盖层
        renderFalloutSkyOverlay(event);
    }

    /**
     * 渲染辐射尘天空覆盖层
     * 在天空渲染后添加一个变暗的覆盖层
     */
    private static void renderFalloutSkyOverlay(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        // 获取渲染系统
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        // 计算天空变暗程度
        float darkenFactor = radiationIntensity * 0.7f;

        // 辐射尘天空颜色 - 灰暗的棕灰色
        @SuppressWarnings("unused") float r = 0.25f;
        @SuppressWarnings("unused") float g = 0.22f;
        @SuppressWarnings("unused") float b = 0.18f;
        @SuppressWarnings("unused") float a = darkenFactor * 0.5f;

        // 这里我们使用一个简化的方法
        // 实际的天空渲染修改比较复杂，我们主要通过迷雾颜色和粒子效果来实现氛围

        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /**
     * 获取当前辐射强度
     */
    public static float getRadiationIntensity() {
        return radiationIntensity;
    }

    /**
     * 检查是否在辐射区域
     */
    public static boolean isInRadiationZone() {
        return inRadiationZone;
    }
}

