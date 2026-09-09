package com.singularity_iteration.mio_icif.Render;

import com.singularity_iteration.mio_icif.entity.mio_icif_Entity_Nuke_Primed;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_Nuke_Renderer extends EntityRenderer<mio_icif_Entity_Nuke_Primed> {
    private final BlockRenderDispatcher blockRenderer;

    public mio_icif_Nuke_Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(mio_icif_Entity_Nuke_Primed entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);

        // 获取引信时间
        int fuse = entity.getFuse();

        // 倒计时最终?0秒开始闪烁放大效果
    if ((float)fuse - partialTicks + 1.0F < 200.0F) { // 200 ticks = 10�
        float f = 1.0F - ((float)fuse - partialTicks + 1.0F) / 200.0F;
            f = Mth.clamp(f, 0.0F, 1.0F);
            f *= f;
            f *= f;
            float scale = 1.0F + f * 0.3F;
            poseStack.scale(scale, scale, scale);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, -0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        // 获取核弹方块状态
    var blockState = com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.NUKE.get().defaultBlockState();

        // 使用TntMinecartRenderer的渲染方法来渲染白色闪烁效果
        // 最终?秒闪烁频率加载
    boolean flash = fuse <= 100 ? fuse / 2 % 2 == 0 : fuse / 5 % 2 == 0;

        net.minecraft.client.renderer.entity.TntMinecartRenderer.renderWhiteSolidBlock(
            this.blockRenderer,
            blockState,
            poseStack,
            buffer,
            packedLight,
            flash
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(mio_icif_Entity_Nuke_Primed entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

