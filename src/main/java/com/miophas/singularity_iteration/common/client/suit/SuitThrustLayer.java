// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.flight.JetpackSoundClient;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.nano.ThrusterFx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.1.10.31 (S-4): thrusters of the quantum suit in normal mode, drawn on the worn model (third person, other players).
 * <ul>
 *   <li><b>Jetpack flight</b> of the quantum chestplate: two jets from the thruster pods under the back unit (body pixels
 *       x = +-2.9, y = 12, z = 5.4, tilted 12 degrees back) and, with the quantum boots, from the soles. The thrust comes
 *       from {@link JetpackSoundClient#throttle} (the own player continuous, other players the relayed three levels).</li>
 *   <li><b>Boost jump</b> of the quantum boots: the soles burn for a moment ({@link SuitMotionFx#soleBurst}).</li>
 * </ul>
 * The flames are those of {@link ThrusterFlame} (thrust colour, animated strip, sparks, heat haze through {@link ThrusterFx}).
 * Special Maneuver flight draws its own thrusters ({@link ManeuverFlightLayer}). The first-person palm light of the hover is
 * {@link #drawPalm} (S-9). Option {@code motion_fx}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitThrustLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation CHEST = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor/item_armor_quantum_chestplate");
    private static final ResourceLocation BOOTS = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor/item_armor_quantum_boots");
    /** Back thruster pods of the quantum chestplate (body space, pixels). */
    static final float POD_X = 2.92F, POD_Y = 12.0F, POD_Z = 5.4F, POD_TILT = 12F;
    private final ThrusterFlame.Strips strips = new ThrusterFlame.Strips();
    /** 0.1.10.32 (P-3): detail tier of the player being drawn. */
    private int lodTier;
    private static final ThrusterFlame.Strips PALM = new ThrusterFlame.Strips();

    public SuitThrustLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new SuitThrustLayer(renderer));
        }
    }

    static boolean worn(LivingEntity e, EquipmentSlot slot, ResourceLocation item) {
        var stack = e.getItemBySlot(slot);
        return !stack.isEmpty() && item.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())) && SuitGlowLayer.charge(stack) > 0F;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if (!FcsClientConfig.motionFx() || ManeuverModeClient.inManeuver(player)) return;
        float jet = worn(player, EquipmentSlot.CHEST, CHEST) ? JetpackSoundClient.throttle(player) : 0F;
        boolean boots = worn(player, EquipmentSlot.FEET, BOOTS);
        float burst = boots ? SuitMotionFx.soleBurst(player, partial) : 0F;
        if (jet <= 0.02F && burst <= 0.02F) return;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        float seconds = (player.tickCount + partial) / 20F;
        int id = player.getId();
        lodTier = com.miophas.singularity_iteration.common.client.render.FxBudget.tier(player);                    // 0.1.10.32 (P-3)
        strips.clear();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        if (jet > 0.02F) {
            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                model.body.translateAndRotate(pose);
                pose.translate(side * POD_X / 16F, POD_Y / 16F, POD_Z / 16F);
                pose.mulPose(Axis.XP.rotationDegrees(POD_TILT));
                nozzle(pose, vc, 0.085F, 0.18F + 0.5F * jet, jet, jet, seconds, id, 4 + (side + 1) / 2);
                pose.popPose();
            }
        }
        float sole = Math.max(boots ? jet * 0.75F : 0F, burst);
        if (sole > 0.02F) {
            sole(pose, vc, model.rightLeg, sole, Math.max(jet, burst), seconds, id, 6);
            sole(pose, vc, model.leftLeg, sole, Math.max(jet, burst), seconds, id, 7);
        }
        strips.draw(buffers);
    }

    private void sole(PoseStack pose, VertexConsumer vc, ModelPart leg, float power, float throttle, float seconds, int id, int seed) {
        if (!leg.visible) return;
        pose.pushPose();
        leg.translateAndRotate(pose);
        pose.translate(0, 12.2F / 16F, 0);
        nozzle(pose, vc, 0.07F, 0.14F + 0.4F * power, power, throttle, seconds, id, seed);
        pose.popPose();
    }

    private void nozzle(PoseStack pose, VertexConsumer vc, float radius, float length, float power, float throttle, float seconds, int id, int seed) {
        Matrix4f m = pose.last().pose();
        float flicker = 0.88F + 0.12F * (float) Math.sin(seconds * 37 + seed * 1.7) * (float) Math.sin(seconds * 23 + seed);
        ThrusterFlame.glow(vc, m, radius, length, power, throttle, flicker, seconds, id * 8 + seed,
            lodTier == com.miophas.singularity_iteration.common.client.render.FxLodRules.FAR ? null : strips, com.miophas.singularity_iteration.common.client.render.FxLodRules.segments(10, lodTier));
        if (FcsClientConfig.thrusterFx() && lodTier == com.miophas.singularity_iteration.common.client.render.FxLodRules.NEAR) {
            Vector3f a = m.transformPosition(0, 0, 0, new Vector3f()), b = m.transformPosition(0, length, 0, new Vector3f());
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            ThrusterFx.jet(id * 8L + seed, new Vec3(a.x + cam.x, a.y + cam.y, a.z + cam.z), new Vec3(b.x + cam.x, b.y + cam.y, b.z + cam.z),
                power * 0.8F, throttle);
        }
    }

    /**
     * S-9, first person: the palm thruster of the arm the player sees while hovering in Special Maneuver flight (the third-person
     * flight layer does not run in first person). The pose is the posed arm of {@code SuitHandRenderer}.
     */
    public static void drawPalm(PoseStack pose, MultiBufferSource buffers, Player player, ModelPart arm, boolean right) {
        if (!ManeuverModeClient.inManeuver(player)) return;
        float[] output = ManeuverModeClient.thrusterOutput(player);
        if (output == null) return;
        float power = output[right ? 0 : 1];
        if (power <= 0.02F) return;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float thr = ManeuverModeClient.throttleOf(player, partial);
        float seconds = (player.tickCount + partial) / 20F;
        pose.pushPose();
        arm.translateAndRotate(pose);
        pose.translate((right ? -1F : 1F) / 16F, 10.5F / 16F, 0);
        PALM.clear();
        float flicker = 0.88F + 0.12F * (float) Math.sin(seconds * 37) * (float) Math.sin(seconds * 23 + (right ? 1 : 0));
        // shorter than in third person: the flame runs past the bottom of the screen
        ThrusterFlame.glow(buffers.getBuffer(SiRenderTypes.FIELD_GLOW), pose.last().pose(), 0.05F + 0.03F * power, 0.1F + 0.22F * power,
            power, Math.max(thr, 0.5F * power), flicker, seconds, right ? 0 : 1, PALM);
        PALM.draw(buffers);
        pose.popPose();
    }
}
