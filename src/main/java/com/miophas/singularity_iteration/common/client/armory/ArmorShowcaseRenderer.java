package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmorShowcaseBlock;
import com.miophas.singularity_iteration.common.armory.ArmorShowcaseBlockEntity;
import com.miophas.singularity_iteration.common.armory.ArmoryPiece;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Rotations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the showcase's outfit on a graphite mannequin standing on the pedestal.
 *
 * <p>The mannequin is a client-only armour stand (never added to the world) drawn through
 * a stand renderer with our own texture, so every armour, dyed layer, custom armour model
 * from other mods, trim and held item renders exactly as when worn. Facing follows the
 * block and the 45 degree turntable steps; while powered by redstone it spins slowly.
 */
public class ArmorShowcaseRenderer implements BlockEntityRenderer<ArmorShowcaseBlockEntity> {
    static final ResourceLocation MANNEQUIN = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/entity/armory_mannequin.png");

    private final BlockEntityRendererProvider.Context context;
    private final Map<ArmorShowcaseBlockEntity, ArmorStand> stands = new WeakHashMap<>();
    private MannequinRenderer renderer;

    public ArmorShowcaseRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    private MannequinRenderer renderer() {
        if (renderer == null) {
            Minecraft mc = Minecraft.getInstance();
            var entityContext = new EntityRendererProvider.Context(mc.getEntityRenderDispatcher(), mc.getItemRenderer(),
                mc.getBlockRenderer(), mc.getEntityRenderDispatcher().getItemInHandRenderer(), mc.getResourceManager(),
                mc.getEntityModels(), mc.font);
            renderer = new MannequinRenderer(entityContext);
        }
        return renderer;
    }

    @Override
    public void render(ArmorShowcaseBlockEntity showcase, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (showcase.getLevel() == null) return;
        ArmorStand stand = stands.computeIfAbsent(showcase, s -> mannequin(s));
        for (ArmoryPiece piece : ArmoryPiece.COLUMNS) {
            ItemStack want = showcase.display().get(piece.column());
            if (!ItemStack.matches(want, stand.getItemBySlot(piece.slot))) stand.setItemSlot(piece.slot, want.copy());
        }
        var state = showcase.getBlockState();
        float facing = state.hasProperty(ArmorShowcaseBlock.FACING) ? state.getValue(ArmorShowcaseBlock.FACING).toYRot() : 0;
        float spin = state.hasProperty(ArmorShowcaseBlock.POWERED) && state.getValue(ArmorShowcaseBlock.POWERED)
            ? (showcase.getLevel().getGameTime() + partialTick) * 1.5F : 0;
        float yaw = facing + showcase.rotation() + spin;
        stand.setYRot(yaw); stand.yRotO = yaw;
        stand.yBodyRot = yaw; stand.yBodyRotO = yaw;
        stand.yHeadRot = yaw; stand.yHeadRotO = yaw;

        int standLight = LevelRenderer.getLightColor(showcase.getLevel(), showcase.getBlockPos().above());
        pose.pushPose();
        pose.translate(0.5, 0.25, 0.5);
        renderer().render(stand, yaw, partialTick, pose, buffers, Math.max(standLight, light));
        pose.popPose();

        if (showcase.linked() && !showcase.label().isEmpty()) renderLabel(showcase.label(), pose, buffers, standLight);
    }

    private static ArmorStand mannequin(ArmorShowcaseBlockEntity showcase) {
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, showcase.getLevel());
        stand.setNoBasePlate(true);
        stand.setShowArms(true);
        stand.setLeftArmPose(new Rotations(-8, 0, -6));
        stand.setRightArmPose(new Rotations(-12, 0, 8));
        stand.setPos(showcase.getBlockPos().getX() + 0.5, showcase.getBlockPos().getY() + 0.25, showcase.getBlockPos().getZ() + 0.5);
        return stand;
    }

    /** Suit name floating above the mannequin, like a museum plaque. */
    private void renderLabel(String text, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        pose.pushPose();
        pose.translate(0.5, 2.45, 0.5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.022F, -0.022F, 0.022F);
        Matrix4f matrix = pose.last().pose();
        float x = -font.width(text) / 2.0F;
        int background = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(text, x, 0, 0x20FFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, light);
        font.drawInBatch(text, x, 0, 0xFFBFE3FF, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ArmorShowcaseBlockEntity showcase) {
        var p = showcase.getBlockPos();
        return new AABB(p.getX() - 0.5, p.getY(), p.getZ() - 0.5, p.getX() + 1.5, p.getY() + 2.8, p.getZ() + 1.5);
    }

    @Override
    public int getViewDistance() { return 48; }

    /** Armour stand renderer with the graphite mannequin texture. */
    static final class MannequinRenderer extends ArmorStandRenderer {
        MannequinRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(ArmorStand stand) {
            return MANNEQUIN;
        }

        @Override
        protected boolean shouldShowName(ArmorStand stand) {
            return false;
        }
    }
}
