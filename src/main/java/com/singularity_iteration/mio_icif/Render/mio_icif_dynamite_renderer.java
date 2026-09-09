package com.singularity_iteration.mio_icif.Render;

import com.singularity_iteration.mio_icif.entity.dynamite.mio_icif_dynamite_entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_dynamite_renderer extends EntityRenderer<mio_icif_dynamite_entity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/item/normal/item_dynamite.png");
    private final ItemRenderer itemRenderer;
    private final ItemStack dynamiteStack;

    public mio_icif_dynamite_renderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.dynamiteStack = new ItemStack(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.DYNAMITE.get());
    }

    @Override
    public void render(mio_icif_dynamite_entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.5F, 0.5F, 0.5F);

        BakedModel model = this.itemRenderer.getModel(this.dynamiteStack, entity.level(), null, 0);
        this.itemRenderer.render(
            this.dynamiteStack,
            ItemDisplayContext.GROUND,
            false,
            poseStack,
            buffer,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            model
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(mio_icif_dynamite_entity entity) {
        return TEXTURE;
    }
}

