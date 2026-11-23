// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.block.energycontainer.TesuRelayBlock;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuWirelessBeams;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.188: the TESU wireless field's streams to the machines it fills: a wireless output port or a relay that the server
 * reports ({@link TesuWirelessBeams}) draws the wireless node's particle stream
 * ({@link WirelessTransmissionNodeRenderer#renderStream}) from itself (a relay: from the emitter at its pylon's top) to each
 * machine, for {@link #KEEP} ticks after the last report. Only within {@link #DISTANCE} blocks of the camera.
 */
@OnlyIn(Dist.CLIENT)
public final class TesuBeamClient {
    private TesuBeamClient() {}

    static final int KEEP = 30;
    static final double DISTANCE = 128;

    private record Beam(boolean relay, long[] targets, long until) {}

    private static final Map<BlockPos, Beam> BEAMS = new HashMap<>();
    @Nullable private static ClientLevel lastLevel;
    private static long ticks;

    static void accept(TesuWirelessBeams.Packet p) {
        BEAMS.put(p.node().immutable(), new Beam(p.relay(), p.targets(), ticks + KEEP));
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        static void setup(FMLClientSetupEvent event) { TesuWirelessBeams.CLIENT_HOOK = TesuBeamClient::accept; }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            ticks++;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != lastLevel) { BEAMS.clear(); lastLevel = mc.level; }
            if (!BEAMS.isEmpty()) BEAMS.values().removeIf(b -> b.until < ticks);
        }

        @SubscribeEvent
        static void render(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BEAMS.isEmpty()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Vec3 cam = event.getCamera().getPosition();
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            PoseStack pose = event.getPoseStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (var e : BEAMS.entrySet()) {
                BlockPos node = e.getKey();
                Beam beam = e.getValue();
                Vec3 from = beam.relay ? new Vec3(node.getX() + 0.5, node.getY() + TesuRelayBlock.EMITTER_Y, node.getZ() + 0.5) : Vec3.atCenterOf(node);
                if (from.distanceToSqr(cam) > DISTANCE * DISTANCE) continue;
                pose.pushPose();
                pose.translate(node.getX() - cam.x, node.getY() - cam.y, node.getZ() - cam.z);
                for (long t : beam.targets) {
                    at.set(t);
                    WirelessTransmissionNodeRenderer.renderStream(mc.level, node, from, Vec3.atCenterOf(at), partial, pose, buffers);
                }
                pose.popPose();
            }
            buffers.endBatch(RenderType.debugQuads());
        }
    }
}
