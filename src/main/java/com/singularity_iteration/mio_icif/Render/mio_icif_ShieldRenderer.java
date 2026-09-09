package com.singularity_iteration.mio_icif.Render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class mio_icif_ShieldRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/entity/item_tool_electric_shield.png"
    );
    private final ModelPart plate;
    private final ModelPart handle;

    public mio_icif_ShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("plate", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-6.0F, -11.0F, -2.0F, 12.0F, 22.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("handle", CubeListBuilder.create()
            .texOffs(26, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 6.0F), PartPose.ZERO);

        LayerDefinition layerDefinition = LayerDefinition.create(mesh, 64, 64);
        ModelPart modelPart = layerDefinition.bakeRoot();
        this.plate = modelPart.getChild("plate");
        this.handle = modelPart.getChild("handle");
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        this.handle.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        this.plate.render(poseStack, vertexConsumer, packedLight, packedOverlay);

        poseStack.popPose();
    }
}