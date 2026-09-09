package com.singularity_iteration.mio_icif.client.render;

import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_fluid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * 管道流体渲染�? *
 * 在管道中心及连接臂内部渲染流体的材质量? * 使用精确的模型位置（4/16~12/16），消除内缩量：
 * - 中心立方体：渲染全部6个面（双面渲染），覆盖在模型之上
 * - 连接臂：渲染5个面（双面渲染），外端面检查相邻方块避免重�? * - 使用 CUTOUT 渲染：BER 在模型之后绘制，利用 LEQUAL 深度测试覆盖模型
 */
@SuppressWarnings("null")
public class PipeFluidRenderer implements BlockEntityRenderer<mio_icif_pipe_fluid> {

    // 管道中心区域边界�?/16~12/16�
private static final float MIN = 4.0f / 16.0f;   // 0.25
    private static final float MAX = 12.0f / 16.0f;  // 0.75

    // 连接臂延伸边�
private static final float BOUNDARY_MIN = 0.0f;
    private static final float BOUNDARY_MAX = 1.0f;

    public PipeFluidRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(mio_icif_pipe_fluid blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 流体渲染已禁止?- 只使用模型纹�
    return;
        /*
        FluidStack fluid = blockEntity.getFluid();
        if (fluid.isEmpty()) return;

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluidType());
        ResourceLocation stillTexture = extensions.getStillTexture();
        if (stillTexture == null) return;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTexture);
        if (sprite == null) return;

        int color = extensions.getTintColor(fluid);
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // 水等流体�?CUTOUT，半透明流体�?translucent
        boolean isTranslucent = a < 0.95f;
        RenderType renderType = isTranslucent ? RenderType.translucent() : RenderType.cutout();
        VertexConsumer builder = bufferSource.getBuffer(renderType);
        Matrix4f matrix = poseStack.last().pose();

        BlockState state = blockEntity.getBlockState();
        Level level = blockEntity.getLevel();

        // 获取各方向连接状态
    boolean down  = isConnected(state, Direction.DOWN);
        boolean up    = isConnected(state, Direction.UP);
        boolean north = isConnected(state, Direction.NORTH);
        boolean south = isConnected(state, Direction.SOUTH);
        boolean west  = isConnected(state, Direction.WEST);
        boolean east  = isConnected(state, Direction.EAST);

        // 1. 渲染中心流体立方体（全部6个面，双面渲染）
        renderCenter(builder, matrix, sprite, r, g, b, a, packedLight);

        // 2. 渲染各连接臂
        if (down)  renderArm(builder, matrix, sprite, level, blockEntity.getBlockPos(),
                         Direction.DOWN,  r, g, b, a, packedLight);
        if (up)    renderArm(builder, matrix, sprite, level, blockEntity.getBlockPos(),
                         Direction.UP,    r, g, b, a, packedLight);
        if (north) renderArm(builder, matrix, sprite, level, blockEntity.getBlockPos(),
                         Direction.NORTH, r, g, b, a, packedLight);
        if (south) renderArm(builder, matrix, sprite, level, blockEntity.getBlockPos(),
                         Direction.SOUTH, r, g, b, a, packedLight);
        if (west)  renderArm(builder, matrix, sprite, level, blockEntity.getBlockPos(),
                         Direction.WEST,  r, g, b, a, packedLight);
        if (east)  renderArm(builder, matrix, sprite, level, blockEntity.getBlockPos(),
                         Direction.EAST,  r, g, b, a, packedLight);
        */
    }

    // ========= 中心立方法?=========

    /**
     * 渲染管道中心的流体立方体（全�?个面�
 * 每个面双面渲染，确保从任意角度可�
 */
    @SuppressWarnings("unused")
    private void renderCenter(VertexConsumer builder, Matrix4f matrix,
                              TextureAtlasSprite sprite,
                              float r, float g, float b, float a, int packedLight) {
        float min = MIN;
        float max = MAX;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float uRange = u1 - u0;
        float vRange = v1 - v0;

        // 平铺系数：中�?8px = 0.5 纹理周期
        float tile = (max - min);
        float uStep = uRange * tile;
        float vStep = vRange * tile;

        // 所�?个面 - 每个面由 4 个顶点定�
    // 为了避免 z-fighting 且能在模型之上正确显示，渲染在精确的模型位置
        // 由于 BER 在模型之后绘制，LEQUAL 深度测试会让流体覆盖模型

        // DOWN: y=min, 平面�?x,z 定义
        renderQuadDS(builder, matrix,
            min, min, min,  max, min, max,
            u0 + min * uStep, v0 + min * vStep,
            u0 + max * uStep, v0 + max * vStep,
            0, -1, 0, r, g, b, a, packedLight);
        // UP: y=max
        renderQuadDS(builder, matrix,
            min, max, min,  max, max, max,
            u0 + min * uStep, v0 + min * vStep,
            u0 + max * uStep, v0 + max * vStep,
            0, 1, 0, r, g, b, a, packedLight);
        // NORTH: z=min, 平面�?x,y 定义
        renderQuadDS(builder, matrix,
            min, min, min,  max, max, min,
            u0 + min * uStep, v0 + min * vStep,
            u0 + max * uStep, v0 + max * vStep,
            0, 0, -1, r, g, b, a, packedLight);
        // SOUTH: z=max
        renderQuadDS(builder, matrix,
            min, min, max,  max, max, max,
            u0 + min * uStep, v0 + min * vStep,
            u0 + max * uStep, v0 + max * vStep,
            0, 0, 1, r, g, b, a, packedLight);
        // WEST: x=min, 平面�?z,y 定义
        renderQuadDS(builder, matrix,
            min, min, min,  min, max, max,
            u0 + min * uStep, v0 + min * vStep,
            u0 + max * uStep, v0 + max * vStep,
            -1, 0, 0, r, g, b, a, packedLight);
        // EAST: x=max
        renderQuadDS(builder, matrix,
            max, min, min,  max, max, max,
            u0 + min * uStep, v0 + min * vStep,
            u0 + max * uStep, v0 + max * vStep,
            1, 0, 0, r, g, b, a, packedLight);
    }

    // ========= 连接受?=========

    /**
     * 渲染一个连接臂的流�
 * 臂从中心延伸到方块边�
 * 
     * @param dir 连接方向
     */
    @SuppressWarnings("unused")
    private void renderArm(VertexConsumer builder, Matrix4f matrix,
                           TextureAtlasSprite sprite, Level level, 
                           net.minecraft.core.BlockPos pos, Direction dir,
                           float r, float g, float b, float a, int packedLight) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float uRange = u1 - u0;
        float vRange = v1 - v0;
        float tile = (MAX - MIN);
        float uStep = uRange * tile;
        float vStep = vRange * tile;

        // 检查相邻方块是否是水管
        boolean adjacentIsPipe = false;
        if (level != null && pos != null) {
            BlockState adjState = level.getBlockState(pos.relative(dir));
            adjacentIsPipe = adjState.getBlock() instanceof mio_icif_block_pipe_water;
        }

        // 臂的坐标范围
        float ax1, ay1, az1, ax2, ay2, az2;

        switch (dir) {
            case DOWN:
                ax1 = MIN; ay1 = BOUNDARY_MIN; az1 = MIN;
                ax2 = MAX; ay2 = MIN;           az2 = MAX;
                break;
            case UP:
                ax1 = MIN; ay1 = MAX;           az1 = MIN;
                ax2 = MAX; ay2 = BOUNDARY_MAX;  az2 = MAX;
                break;
            case NORTH:
                ax1 = MIN; ay1 = MIN; az1 = BOUNDARY_MIN;
                ax2 = MAX; ay2 = MAX; az2 = MIN;
                break;
            case SOUTH:
                ax1 = MIN; ay1 = MIN; az1 = MAX;
                ax2 = MAX; ay2 = MAX; az2 = BOUNDARY_MAX;
                break;
            case WEST:
                ax1 = BOUNDARY_MIN; ay1 = MIN; az1 = MIN;
                ax2 = MIN;          ay2 = MAX; az2 = MAX;
                break;
            case EAST:
                ax1 = MAX;          ay1 = MIN; az1 = MIN;
                ax2 = BOUNDARY_MAX; ay2 = MAX; az2 = MAX;
                break;
            default:
                return;
        }

        // 外端面（连接器开口处理? 只有当相邻方块不是水管时才渲�
    // 避免两个管道连接处双重渲染外端面
        if (!adjacentIsPipe) {
            switch (dir) {
                case DOWN:
                    renderQuadDS(builder, matrix,
                        ax1, ay1, az1,  ax2, ay1, az2,
                        u0 + ax1 * uStep, v0 + az1 * vStep,
                        u0 + ax2 * uStep, v0 + az2 * vStep,
                        0, -1, 0, r, g, b, a, packedLight);
                    break;
                case UP:
                    renderQuadDS(builder, matrix,
                        ax1, ay2, az1,  ax2, ay2, az2,
                        u0 + ax1 * uStep, v0 + az1 * vStep,
                        u0 + ax2 * uStep, v0 + az2 * vStep,
                        0, 1, 0, r, g, b, a, packedLight);
                    break;
                case NORTH:
                    renderQuadDS(builder, matrix,
                        ax1, ay1, az1,  ax2, ay2, az1,
                        u0 + ax1 * uStep, v0 + ay1 * vStep,
                        u0 + ax2 * uStep, v0 + ay2 * vStep,
                        0, 0, -1, r, g, b, a, packedLight);
                    break;
                case SOUTH:
                    renderQuadDS(builder, matrix,
                        ax1, ay1, az2,  ax2, ay2, az2,
                        u0 + ax1 * uStep, v0 + ay1 * vStep,
                        u0 + ax2 * uStep, v0 + ay2 * vStep,
                        0, 0, 1, r, g, b, a, packedLight);
                    break;
                case WEST:
                    renderQuadDS(builder, matrix,
                        ax1, ay1, az1,  ax1, ay2, az2,
                        u0 + az1 * uStep, v0 + ay1 * vStep,
                        u0 + az2 * uStep, v0 + ay2 * vStep,
                        -1, 0, 0, r, g, b, a, packedLight);
                    break;
                case EAST:
                    renderQuadDS(builder, matrix,
                        ax2, ay1, az1,  ax2, ay2, az2,
                        u0 + az1 * uStep, v0 + ay1 * vStep,
                        u0 + az2 * uStep, v0 + ay2 * vStep,
                        1, 0, 0, r, g, b, a, packedLight);
                    break;
            }
        }

        // 4个侧面（沿着臂方向延伸）
        switch (dir) {
            case DOWN:
            case UP:
                // 侧面: x=min, UV 使用 z,y
                renderQuadDS(builder, matrix, ax1, ay1, az1, ax1, ay2, az2,
                    u0 + az1 * uStep, v0 + ay1 * vStep,
                    u0 + az2 * uStep, v0 + ay2 * vStep,
                    -1, 0, 0, r, g, b, a, packedLight);
                // x=max
                renderQuadDS(builder, matrix, ax2, ay1, az1, ax2, ay2, az2,
                    u0 + az1 * uStep, v0 + ay1 * vStep,
                    u0 + az2 * uStep, v0 + ay2 * vStep,
                    1, 0, 0, r, g, b, a, packedLight);
                // z=min
                renderQuadDS(builder, matrix, ax1, ay1, az1, ax2, ay2, az1,
                    u0 + ax1 * uStep, v0 + ay1 * vStep,
                    u0 + ax2 * uStep, v0 + ay2 * vStep,
                    0, 0, -1, r, g, b, a, packedLight);
                // z=max
                renderQuadDS(builder, matrix, ax1, ay1, az2, ax2, ay2, az2,
                    u0 + ax1 * uStep, v0 + ay1 * vStep,
                    u0 + ax2 * uStep, v0 + ay2 * vStep,
                    0, 0, 1, r, g, b, a, packedLight);
                break;
            case NORTH:
            case SOUTH:
                // 侧面: x=min
                renderQuadDS(builder, matrix, ax1, ay1, az1, ax1, ay2, az2,
                    u0 + az1 * uStep, v0 + ay1 * vStep,
                    u0 + az2 * uStep, v0 + ay2 * vStep,
                    -1, 0, 0, r, g, b, a, packedLight);
                // x=max
                renderQuadDS(builder, matrix, ax2, ay1, az1, ax2, ay2, az2,
                    u0 + az1 * uStep, v0 + ay1 * vStep,
                    u0 + az2 * uStep, v0 + ay2 * vStep,
                    1, 0, 0, r, g, b, a, packedLight);
                // y=min
                renderQuadDS(builder, matrix, ax1, ay1, az1, ax2, ay1, az2,
                    u0 + ax1 * uStep, v0 + az1 * vStep,
                    u0 + ax2 * uStep, v0 + az2 * vStep,
                    0, -1, 0, r, g, b, a, packedLight);
                // y=max
                renderQuadDS(builder, matrix, ax1, ay2, az1, ax2, ay2, az2,
                    u0 + ax1 * uStep, v0 + az1 * vStep,
                    u0 + ax2 * uStep, v0 + az2 * vStep,
                    0, 1, 0, r, g, b, a, packedLight);
                break;
            case WEST:
            case EAST:
                // 侧面: y=min
                renderQuadDS(builder, matrix, ax1, ay1, az1, ax2, ay1, az2,
                    u0 + az1 * uStep, v0 + ax1 * vStep,
                    u0 + az2 * uStep, v0 + ax2 * vStep,
                    0, -1, 0, r, g, b, a, packedLight);
                // y=max
                renderQuadDS(builder, matrix, ax1, ay2, az1, ax2, ay2, az2,
                    u0 + az1 * uStep, v0 + ax1 * vStep,
                    u0 + az2 * uStep, v0 + ax2 * vStep,
                    0, 1, 0, r, g, b, a, packedLight);
                // z=min
                renderQuadDS(builder, matrix, ax1, ay1, az1, ax2, ay2, az1,
                    u0 + ax1 * uStep, v0 + ay1 * vStep,
                    u0 + ax2 * uStep, v0 + ay2 * vStep,
                    0, 0, -1, r, g, b, a, packedLight);
                // z=max
                renderQuadDS(builder, matrix, ax1, ay1, az2, ax2, ay2, az2,
                    u0 + ax1 * uStep, v0 + ay1 * vStep,
                    u0 + ax2 * uStep, v0 + ay2 * vStep,
                    0, 0, 1, r, g, b, a, packedLight);
                break;
        }
    }

    // ========= 四边形渲�?=========

    /**
     * 双面渲染一个矩形面
     * 正面和背面都渲染，确保从任意角度可见
     * 
     * 坐标 (x1,y1,z1) �?(x2,y2,z2) 定义面的对角�
 * 根据法向量自动确定平面轴
     */
    private void renderQuadDS(VertexConsumer builder, Matrix4f matrix,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float u0, float v0, float u1, float v1,
                              float nx, float ny, float nz,
                              float r, float g, float b, float a, int packedLight) {
        // 渲染正面 (CCW winding)
        renderQuadOneSide(builder, matrix, x1, y1, z1, x2, y2, z2,
            u0, v0, u1, v1, nx, ny, nz, r, g, b, a, packedLight, false);
        // 渲染背面 (CW winding, 法向量相同?
        renderQuadOneSide(builder, matrix, x1, y1, z1, x2, y2, z2,
            u0, v0, u1, v1, -nx, -ny, -nz, r, g, b, a, packedLight, true);
    }

    /**
     * 渲染四边形的一个面
     */
    private void renderQuadOneSide(VertexConsumer builder, Matrix4f matrix,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float u0, float v0, float u1, float v1,
                                   float nx, float ny, float nz,
                                   float r, float g, float b, float a, int packedLight,
                                   boolean reverse) {
        // 根据法向量确定面所在的平面
        if (ny != 0) {
            // 水平面：使用 x �?z
            float minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            float minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            float y = (y1 + y2) / 2;

            if (!reverse) {
                // 正面 CCW
                if (ny < 0) {
                    // DOWN: 从下方看 CCW
                    addV(builder, matrix, minX, y, minZ, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, minX, y, maxZ, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, y, maxZ, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, y, minZ, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                } else {
                    // UP: 从上方看 CCW
                    addV(builder, matrix, minX, y, minZ, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, y, minZ, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, y, maxZ, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, minX, y, maxZ, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                }
            } else {
                // 背面 CW
                if (ny < 0) {
                    addV(builder, matrix, minX, y, minZ, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, y, minZ, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, y, maxZ, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, minX, y, maxZ, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                } else {
                    addV(builder, matrix, minX, y, minZ, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, minX, y, maxZ, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, y, maxZ, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, y, minZ, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                }
            }
        } else if (nz != 0) {
            // 南北面：使用 x �?y
            float minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            float minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            float z = (z1 + z2) / 2;

            if (!reverse) {
                if (nz < 0) {
                    // NORTH
                    addV(builder, matrix, minX, minY, z, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, minX, maxY, z, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, maxY, z, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, minY, z, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                } else {
                    // SOUTH
                    addV(builder, matrix, minX, minY, z, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, minY, z, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, maxY, z, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, minX, maxY, z, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                }
            } else {
                if (nz < 0) {
                    addV(builder, matrix, minX, minY, z, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, minY, z, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, maxY, z, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, minX, maxY, z, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                } else {
                    addV(builder, matrix, minX, minY, z, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, minX, maxY, z, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, maxY, z, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, maxX, minY, z, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                }
            }
        } else {
            // 东西面：使用 z �?y
            float minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            float minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            float x = (x1 + x2) / 2;

            if (!reverse) {
                if (nx < 0) {
                    // WEST
                    addV(builder, matrix, x, minY, minZ, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, maxY, minZ, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, maxY, maxZ, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, minY, maxZ, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                } else {
                    // EAST
                    addV(builder, matrix, x, minY, minZ, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, minY, maxZ, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, maxY, maxZ, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, maxY, minZ, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                }
            } else {
                if (nx < 0) {
                    addV(builder, matrix, x, minY, minZ, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, minY, maxZ, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, maxY, maxZ, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, maxY, minZ, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                } else {
                    addV(builder, matrix, x, minY, minZ, u0, v1, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, maxY, minZ, u0, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, maxY, maxZ, u1, v0, nx, ny, nz, r, g, b, a, packedLight);
                    addV(builder, matrix, x, minY, maxZ, u1, v1, nx, ny, nz, r, g, b, a, packedLight);
                }
            }
        }
    }

    private void addV(VertexConsumer builder, Matrix4f matrix,
                      float x, float y, float z,
                      float u, float v,
                      float nx, float ny, float nz,
                      float r, float g, float b, float a, int packedLight) {
        builder.addVertex(matrix, x, y, z)
               .setColor(r, g, b, a)
               .setUv(u, v)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(packedLight)
               .setNormal(nx, ny, nz);
    }

    /**
     * 检查管道指定方向是否有连接
     */
    @SuppressWarnings("unused")
    private boolean isConnected(BlockState state, Direction dir) {
        if (state == null) return false;
        switch (dir) {
            case DOWN:  return state.getValue(mio_icif_block_pipe_water.DOWN);
            case UP:    return state.getValue(mio_icif_block_pipe_water.UP);
            case NORTH: return state.getValue(mio_icif_block_pipe_water.NORTH);
            case SOUTH: return state.getValue(mio_icif_block_pipe_water.SOUTH);
            case WEST:  return state.getValue(mio_icif_block_pipe_water.WEST);
            case EAST:  return state.getValue(mio_icif_block_pipe_water.EAST);
        }
        return false;
    }

    @Override
    public boolean shouldRenderOffScreen(mio_icif_pipe_fluid blockEntity) {
        return false;
    }
}

