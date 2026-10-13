// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.CoreAttachmentModel;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.core.FrameCore;
import com.miophas.singularity_iteration.common.suit.core.FrameCores;
import com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * Frame core attachments (0.1.9.0): the exterior of the core in the socket of a worn nano or
 * quantum helmet or quantum chestplate. Solar Array I adds a sun panel on a mast, Solar Array II
 * a panel with two wings, Solar Array III wings, a sun core on the brow and a fan behind the head;
 * the Graviton Bulwark core a turning graviton ring on the chest, a gyro on the back and graviton
 * pads on the shoulders; the Advanced Propulsion core twin boosters with stabilisers and a chest
 * reactor.
 *
 * <p>Animations: the solar wings and panels open in daylight under open sky and fold in the dark
 * or under a roof; the fan sweeps while it collects light; the graviton ring and gyro turn while
 * the suit holds energy, and the graviton lights pulse; the propulsion nozzles and reactor glow
 * with the thrust, and the stabilisers spread in flight.
 *
 * <p>The layer is added to players, armour stands, Wingmen and the holo display mannequin. It
 * follows the client option {@code suit_visuals.core_attachments} and the "core exterior" switch
 * of each piece.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CoreAttachmentLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST};
    private static final Map<LivingEntity, Motion> MOTION = new WeakHashMap<>();

    public CoreAttachmentLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new CoreAttachmentLayer<>(renderer));
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer renderer
                && renderer.getModel() instanceof HumanoidModel) {
            renderer.addLayer(new CoreAttachmentLayer(renderer));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity,
                       float limbSwing, float limbSwingAmount, float partial, float age, float headYaw, float headPitch) {
        if (!FcsClientConfig.coreAttachments()) return;
        draw(getParentModel(), pose, buffers, light, entity, partial);
    }

    /** Draws the attachments of the worn pieces of {@code entity} with the posed parts of {@code model}. */
    public static void draw(HumanoidModel<?> model, PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity, float partial) {
        Motion motion = null;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            FrameCore core = FrameCores.core(stack);
            if (core == null || core.slot() != slot || !ArmorFeatureState.isEnabled(stack, FrameCore.KEY_VISUAL)) continue;
            String base = baseName(stack.getItem());
            if (base == null) continue;
            CoreAttachmentModel attachment = CoreAttachmentModel.get(core.id() + "_" + base);
            if (attachment.isEmpty()) continue;
            if (motion == null) motion = motion(entity, partial);
            boolean active = core.isSolar() || SuitModules.energy(stack) > 0;
            boolean solarOn = core.isSolar() && ArmorFeatureState.isEnabled(stack, FrameCore.KEY_SOLAR);
            float time = (entity.tickCount + partial) / 20F;
            float solar = solarOn ? motion.solar(partial) : 0F;
            float thrust = active ? motion.thrust(partial) : 0F;
            VertexConsumer solid = buffers.getBuffer(RenderType.armorCutoutNoCull(attachment.texture()));
            for (CoreAttachmentModel.Segment segment : attachment.segments()) {
                ModelPart part = part(model, segment.part());
                if (part == null || !part.visible || part.skipDraw || segment.quads().length == 0) continue;
                pose.pushPose();
                part.translateAndRotate(pose);
                if (segment.anim() != null) animate(pose, segment.anim(), time, solar, thrust, active);
                quads(pose, solid, segment.quads(), segment.emissive() ? LightTexture.FULL_BRIGHT : light, 0xFFFFFFFF);
                float glow = glow(segment, time, solar, thrust, active, entity.getId());
                if (glow > 0.02F) {
                    int c = Mth.clamp((int) (glow * 255), 0, 255);
                    quads(pose, buffers.getBuffer(RenderType.eyes(attachment.texture())), segment.quads(), LightTexture.FULL_BRIGHT,
                        FastColor.ARGB32.color(255, c, c, c));
                }
                pose.popPose();
            }
        }
    }

    /** Baked model name of a socketed piece; null for other items. */
    @Nullable
    static String baseName(Item item) {
        if (item == mio_icif_items_armors.ARMOR_NANO_HELMET.get()) return "nano_helmet";
        if (item == mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get()) return "quantum_helmet";
        if (item == mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE.get()) return "quantum_chestplate";
        return null;
    }

    @Nullable
    private static ModelPart part(HumanoidModel<?> model, CoreAttachmentModel.Part part) {
        return switch (part) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
        };
    }

    private static void animate(PoseStack pose, CoreAttachmentModel.Anim a, float time, float solar, float thrust, boolean active) {
        float drive = switch (a.drive()) {
            case SOLAR -> solar;
            case THRUST -> thrust;
            case ACTIVE -> active ? 1F : 0F;
        };
        float degrees = switch (a.type()) {
            case "deploy" -> Mth.lerp(drive, a.closed(), a.open());
            case "spin" -> drive > 0 ? (a.speed() * time) % 360F : 0F;
            case "sweep" -> drive * a.amplitude() * Mth.sin(time * Mth.TWO_PI / a.period());
            default -> 0F;
        };
        if (Math.abs(degrees) < 1.0E-3F) return;
        pose.translate(a.px() / 16F, a.py() / 16F, a.pz() / 16F);
        pose.mulPose(new Quaternionf().rotateAxis(degrees * Mth.DEG_TO_RAD, a.ax(), a.ay(), a.az()));
        pose.translate(-a.px() / 16F, -a.py() / 16F, -a.pz() / 16F);
    }

    /** Strength of the additive glow pass, 0..1. */
    private static float glow(CoreAttachmentModel.Segment segment, float time, float solar, float thrust, boolean active, int seed) {
        float g = switch (segment.glow()) {
            case NONE -> 0F;
            case SOLAR -> solar * (0.55F + 0.25F * Mth.sin(time * 1.3F + seed));
            case THRUST -> active ? 0.25F + 0.75F * thrust * (0.85F + 0.15F * Mth.sin(time * 31F + seed)) : 0F;
            case PULSE -> active ? 0.45F + 0.4F * Mth.sin(time * 2.2F + seed) : 0F;
            case IDLE -> active ? 0.4F : 0F;
        };
        return segment.emissive() ? g : g * 0.35F;
    }

    private static void quads(PoseStack pose, VertexConsumer vc, float[][] quads, int light, int color) {
        PoseStack.Pose last = pose.last();
        for (float[] q : quads) {
            for (int i = 0; i < 4; i++) {
                int k = i * 5;
                vc.addVertex(last, q[k] / 16F, q[k + 1] / 16F, q[k + 2] / 16F)
                    .setColor(color)
                    .setUv(q[k + 3], q[k + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(last, q[20], q[21], q[22]);
            }
        }
    }

    // ------------------------------------------------------------------ motion state per wearer

    private static Motion motion(LivingEntity entity, float partial) {
        Motion m = MOTION.computeIfAbsent(entity, e -> new Motion());
        m.update(entity);
        return m;
    }

    /** Smoothed solar deploy (0 folded .. 1 open) and thrust (0 .. 1), updated once per tick. */
    static final class Motion {
        private int tick = Integer.MIN_VALUE;
        private float solar, solarO, thrust, thrustO;
        private boolean first = true;

        void update(LivingEntity entity) {
            if (entity.tickCount == tick) return;
            tick = entity.tickCount;
            float solarTarget = sunlit(entity) ? 1F : 0F;
            float thrustTarget = thrustOf(entity);
            if (first) {
                solar = solarO = solarTarget;
                thrust = thrustO = thrustTarget;
                first = false;
                return;
            }
            solarO = solar;
            thrustO = thrust;
            solar += Mth.clamp(solarTarget - solar, -0.05F, 0.05F);
            thrust += (thrustTarget - thrust) * 0.25F;
        }

        float solar(float partial) { return Mth.lerp(partial, solarO, solar); }
        float thrust(float partial) { return Mth.lerp(partial, thrustO, thrust); }

        private static boolean sunlit(LivingEntity entity) {
            var level = entity.level();
            if (level == null) return false;
            BlockPos head = BlockPos.containing(entity.getX(), entity.getEyeY() + 0.5, entity.getZ());
            long time = level.getDayTime() % 24000L;
            return time < 12500L && !level.isRaining() && level.canSeeSky(head);
        }

        private static float thrustOf(LivingEntity entity) {
            if (entity instanceof Player player) {
                if (JetpackFlightController.isFlying(player) || ManeuverModeClient.inManeuver(player)) return 1F;
                if (player.getAbilities().flying) return 0.5F;
            }
            if (entity.onGround() || entity.isPassenger() || entity.isInWater()) return 0F;
            // airborne: rising or hovering reads as thrust (other players, Wingmen)
            double dy = entity.getY() - entity.yo;
            return dy > 0.02 ? 0.8F : dy > -0.05 ? 0.45F : 0.1F;
        }
    }
}
