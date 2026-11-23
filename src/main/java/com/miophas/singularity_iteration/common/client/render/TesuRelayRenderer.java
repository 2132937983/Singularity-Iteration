package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.block.energycontainer.TesuRelayBlock;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuRelayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.11.159: a powered TESU relay draws the wireless node's particle stream from the node it is linked to (a wireless output
 * port or a relay) to itself ({@link WirelessTransmissionNodeRenderer#renderStream}). Its culling box spans both ends, so the
 * stream stays drawn while the relay itself is out of view.
 *
 * <p>0.1.11.167: the relay is a pylon three blocks tall; the stream runs between the emitters at the pylons' tops (from a port:
 * from the port's middle).
 */
@SuppressWarnings("null")
public class TesuRelayRenderer implements BlockEntityRenderer<TesuRelayBlockEntity> {
    public TesuRelayRenderer(BlockEntityRendererProvider.Context context) {}

    private static Vec3 emitter(BlockPos relay) {
        return new Vec3(relay.getX() + 0.5, relay.getY() + TesuRelayBlock.EMITTER_Y, relay.getZ() + 0.5);
    }

    @Override
    public void render(TesuRelayBlockEntity relay, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockPos parent = relay.parent();
        if (parent == null || !relay.isPowered()) return;
        Level level = relay.getLevel();
        Vec3 from = level != null && level.getBlockEntity(parent) instanceof TesuRelayBlockEntity ? emitter(parent) : Vec3.atCenterOf(parent);
        WirelessTransmissionNodeRenderer.renderStream(level, relay.getBlockPos(), from, emitter(relay.getBlockPos()), partialTick, poseStack, buffers);
    }

    @Override
    public AABB getRenderBoundingBox(TesuRelayBlockEntity relay) {
        AABB own = new AABB(relay.getBlockPos()).expandTowards(0, 2, 0);
        BlockPos parent = relay.parent();
        return parent == null ? own : own.minmax(new AABB(parent).expandTowards(0, 2, 0)).inflate(0.5);
    }

    @Override public int getViewDistance() { return 128; }
    @Override public boolean shouldRenderOffScreen(TesuRelayBlockEntity relay) { return true; }
}
