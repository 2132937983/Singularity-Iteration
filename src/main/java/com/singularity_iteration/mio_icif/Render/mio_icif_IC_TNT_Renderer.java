package com.singularity_iteration.mio_icif.Render;

import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Entity_IC_TNT_Primed;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_IC_TNT_Renderer extends EntityRenderer<mio_icif_Entity_IC_TNT_Primed> {
    private final BlockRenderDispatcher blockRenderer;

    public mio_icif_IC_TNT_Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(mio_icif_Entity_IC_TNT_Primed entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        int i = entity.getFuse();
        if ((float)i - partialTicks + 1.0F < 10.0F) {
            float f = 1.0F - ((float)i - partialTicks + 1.0F) / 10.0F;
            f = Mth.clamp(f, 0.0F, 1.0F);
            f *= f;
            f *= f;
            float f1 = 1.0F + f * 0.3F;
            poseStack.scale(f1, f1, f1);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, -0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        // 渲染TNT方块，使用工业TNT方块状态
    var blockState = entity.getBlockState();
        if (blockState == null) {
            blockState = Blocks.TNT.defaultBlockState();
        }

        // 使用TntMinecartRenderer的渲染方法来渲染白色闪烁效果
        net.minecraft.client.renderer.entity.TntMinecartRenderer.renderWhiteSolidBlock(
            this.blockRenderer,
            blockState,
            poseStack,
            buffer,
            packedLight,
            i / 5 % 2 == 0
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(mio_icif_Entity_IC_TNT_Primed entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

