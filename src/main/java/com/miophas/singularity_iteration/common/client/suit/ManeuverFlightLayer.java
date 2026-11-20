// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.QuantumNozzles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Special Maneuver Mode, body effects (0.1.8.0; 0.1.11.124, armour and flight overhaul E1: electromagnetic flight).
 *
 * <p>The quantum suit flies without reaction mass, so this layer no longer draws flames or cones of light at the nozzles:
 * the old drawing is archived in {@link com.miophas.singularity_iteration.common.client.legacy.flight.LegacyManeuverJets}
 * (see {@link com.miophas.singularity_iteration.common.client.legacy.flight.LegacyFlightFx}). It samples the back nozzles
 * for the light trails ({@link LightTrails}) and draws the field rings of a graviton core. The electromagnetic hover,
 * attitude pulses and high-speed pods follow in the next batches.
 *
 * <p>Model space: 1 unit = 1 block after the 1/16 scale of the parts; y points down. The local player in first person is
 * not rendered by vanilla, so this layer does not run then.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class ManeuverFlightLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public ManeuverFlightLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new ManeuverFlightLayer(renderer));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partial, float age, float headYaw, float headPitch) {
        if (!ManeuverModeClient.inManeuver(player)) return;
        float[] output = ManeuverModeClient.thrusterOutput(player);
        if (output == null) return;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        LightTrails.samplePlayer(pose, model, player, partial);   // 0.1.11.122: the light trails leave the back nozzles
        if (com.miophas.singularity_iteration.common.client.legacy.flight.LegacyFlightFx.ENABLED) {
            com.miophas.singularity_iteration.common.client.legacy.flight.LegacyManeuverJets.render(pose, buffers, player, model, output, partial);
            return;
        }
        // a graviton core has no nozzles: its field rings stay (a field, not an exhaust)
        java.util.List<QuantumNozzles.Nozzle> nozzles = NozzleDraw.worn(player, partial);
        float[] outs = ManeuverModeClient.nozzleOutput(player, nozzles, partial);
        float seconds = (player.tickCount + partial) / 20F;
        VertexConsumer vc = null;
        for (int i = 0; outs != null && i < outs.length; i++) {
            QuantumNozzles.Nozzle n = nozzles.get(i);
            if (n.kind() != QuantumNozzles.Kind.FIELD || outs[i] <= 0.02F || !NozzleDraw.push(pose, model, n)) continue;
            if (vc == null) vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            NozzleDraw.fieldPhase(vc, pose, n.size(), outs[i], seconds);
            pose.popPose();
        }
    }
}
