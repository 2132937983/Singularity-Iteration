// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.BakedArmorModel;
import com.miophas.singularity_iteration.common.client.render.GlowMasks;
import com.miophas.singularity_iteration.common.suit.SuitPalette;
import com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

/**
 * 0.1.10.30: the light of the worn nano and quantum suits (S-1 of the visuals proposal).
 *
 * <p>Up to 0.1.10.29 the suits were drawn only by the vanilla armour layer, lit by the world: at night a suit was a dark shape
 * and nothing on it showed its energy. This layer draws, after the armour, the glow mask of each worn piece
 * ({@link GlowMasks}: the light pixels and the generated circuit lines) with {@link RenderType#eyes} (vanilla shader, full
 * bright, additive; shader packs treat it as emissive). Colour and strength follow {@link SuitGlowRules}: the palette accent or
 * the suit's own light colour, amber and red as the charge falls, dark when empty, breathing, a heartbeat at low charge, a
 * flicker when hit, brighter with thrust, an energy band down the body, and a power-on sweep from the chest core when a charged
 * piece comes on (put on, suit-up of the Armory, charged from empty).
 *
 * <p>Only the quads under lit mask pixels are drawn (the full quantum suit: 1,218 quads, a few hundred lit). The layer is added
 * to players, armour stands, Wingmen and the holo display mannequin, and the first-person arm draws the same light
 * ({@link #drawArm}). Client options {@code suit_visuals.suit_glow} and {@code suit_glow_strength}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Glowing pieces: item id path -> baked model name. */
    private static final Map<String, String> PIECES = Map.of(
        "armor/item_armor_nano_helmet", "nano_helmet", "armor/item_armor_nano_chestplate", "nano_chestplate",
        "armor/item_armor_nano_leggings", "nano_leggings", "armor/item_armor_nano_boots", "nano_boots",
        "armor/item_armor_quantum_helmet", "quantum_helmet", "armor/item_armor_quantum_chestplate", "quantum_chestplate",
        "armor/item_armor_quantum_leggings", "quantum_leggings", "armor/item_armor_quantum_boots", "quantum_boots");
    private static final Map<String, BakedArmorModel> MODELS = new HashMap<>();
    private static final Map<LivingEntity, Power> POWER = new WeakHashMap<>();
    /** 0.1.10.31 (S-9): the last energy weapon shot per entity id: {right hand 1/0, colour, power x 1000, nano time}. */
    private static final Map<Integer, long[]> SHOTS = new HashMap<>();
    /** The chest core (model pixels): the power-on sweep starts here. */
    private static final float CORE_X = 0F, CORE_Y = 4F, CORE_Z = -2.5F;

    public SuitGlowLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new SuitGlowLayer<>(renderer));
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer renderer && renderer.getModel() instanceof HumanoidModel) {
            renderer.addLayer(new SuitGlowLayer(renderer));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if (!FcsClientConfig.suitGlow()) return;
        draw(getParentModel(), pose, buffers, entity, partial);
    }

    /** The light of one worn piece this frame. */
    record Light(String model, int colour, float strength, float seconds, float since) { }

    /** Baked model name of a glowing piece, else null. */
    @Nullable
    static String modelOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Singularity_Iteration.MOD_ID.equals(id.getNamespace()) ? PIECES.get(id.getPath()) : null;
    }

    /** Charge 0..1 of an electric piece (1 for a piece without a battery). */
    static float charge(ItemStack stack) {
        if (!(stack.getItem() instanceof AbstractElectricArmor armor)) return 1F;
        long max = armor.getMaxEnergy(stack);
        return max <= 0 ? 0F : (float) Math.min(1.0, armor.getEnergy(stack) / (double) max);
    }

    /** Light of the piece in {@code slot} of {@code entity}, or null when it does not glow now. */
    @Nullable
    static Light light(LivingEntity entity, EquipmentSlot slot, float partial) {
        ItemStack stack = entity.getItemBySlot(slot);
        String model = modelOf(stack);
        float charge = model == null ? 0F : charge(stack);
        float since = power(entity, slot, model != null && charge > 0 ? stack.getItem() : null);
        if (model == null) return null;
        float seconds = (entity.tickCount + partial) / 20F;
        float strength = SuitGlowRules.strength(charge, seconds, entity.hurtTime, boost(entity, partial), entity.getId() & 255)
            * FcsClientConfig.suitGlowStrength();
        if (strength <= 0.01F) return null;
        SuitPalette palette = SuitPalette.of(stack);
        int base = palette != null ? palette.accent() : model.startsWith("nano") ? SuitGlowRules.NANO : SuitGlowRules.QUANTUM;
        return new Light(model, SuitGlowRules.colour(base, charge), strength, seconds, since);
    }

    /** Thrust 0..1 of the wearer (Special Maneuver throttle, jetpack flight). */
    private static float boost(LivingEntity entity, float partial) {
        if (!(entity instanceof Player player)) return 0F;
        if (ManeuverModeClient.inManeuver(player)) return ManeuverModeClient.throttleOf(player, partial);
        return JetpackFlightController.isFlying(player) ? 0.5F : 0F;
    }

    /**
     * Seconds since the piece in {@code slot} came on (-1 when it came on before this entity was first seen, or no sweep):
     * a charged glowing piece that was not there (or was empty) the frame before starts the power-on sweep.
     */
    private static float power(LivingEntity entity, EquipmentSlot slot, @Nullable Item on) {
        Power p = POWER.computeIfAbsent(entity, e -> new Power());
        int i = slot.getIndex();
        long now = System.currentTimeMillis();
        if (p.item[i] != on) {
            p.onAt[i] = on != null && p.seen[i] ? now : 0L;
            p.item[i] = on;
        }
        p.seen[i] = true;
        return p.onAt[i] == 0L ? -1F : (now - p.onAt[i]) / 1000F;
    }

    private static final class Power {
        final Item[] item = new Item[4];
        final long[] onAt = new long[4];
        final boolean[] seen = new boolean[4];
    }

    private static BakedArmorModel model(String name) {
        return MODELS.computeIfAbsent(name, n -> new BakedArmorModel(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor_models/" + n + ".json")));
    }

    /**
     * Draws the light of the worn pieces of {@code entity} with the pose of {@code parent}. Each buffer is taken right before
     * its quads (an immediate buffer source shares one buffer between render types, see {@code CoreAttachmentLayer}).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void draw(HumanoidModel<?> parent, PoseStack pose, MultiBufferSource buffers, LivingEntity entity, float partial) {
        for (EquipmentSlot slot : SLOTS) {
            Light light = light(entity, slot, partial);
            if (light == null) continue;
            BakedArmorModel model = model(light.model());
            var geometry = model.geometry();
            if (geometry.isEmpty()) continue;
            GlowMasks.Mask mask = GlowMasks.of(light.model(), geometry);
            if (mask.isEmpty()) continue;
            ((HumanoidModel) parent).copyPropertiesTo(model);
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(mask.texture()));
            switch (slot) {
                case HEAD -> part(pose, vc, model.head, mask, "head", light);
                case CHEST -> {
                    part(pose, vc, model.body, mask, "body", light);
                    part(pose, vc, model.rightArm, mask, "right_arm", light);
                    part(pose, vc, model.leftArm, mask, "left_arm", light);
                    if (FcsClientConfig.handArmorThirdPerson()) {
                        part(pose, vc, model.rightArm, mask, "right_hand", fist(entity, true, light));
                        part(pose, vc, model.leftArm, mask, "left_hand", fist(entity, false, light));
                    }
                }
                case LEGS -> {
                    part(pose, vc, model.body, mask, "body", light);
                    part(pose, vc, model.rightLeg, mask, "right_leg", light);
                    part(pose, vc, model.leftLeg, mask, "left_leg", light);
                }
                default -> {
                    part(pose, vc, model.rightLeg, mask, "right_leg", light);
                    part(pose, vc, model.leftLeg, mask, "left_leg", light);
                }
            }
        }
    }

    /**
     * First-person arm (S-9): the light of the worn chestplate on the arm and hand cover the player sees, with the posed arm of
     * {@code SuitHandRenderer}.
     */
    public static void drawArm(PoseStack pose, MultiBufferSource buffers, LivingEntity entity, ModelPart arm, boolean right, boolean hand) {
        if (!FcsClientConfig.suitGlow()) return;
        Light light = light(entity, EquipmentSlot.CHEST, net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (light == null) return;
        var geometry = model(light.model()).geometry();
        if (geometry.isEmpty()) return;
        GlowMasks.Mask mask = GlowMasks.of(light.model(), geometry);
        if (mask.isEmpty()) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(mask.texture()));
        part(pose, vc, arm, mask, right ? "right_arm" : "left_arm", light);
        if (hand) part(pose, vc, arm, mask, right ? "right_hand" : "left_hand", fist(entity, right, light));
    }

    /** 0.1.10.31 (S-9): an energy weapon shot of {@code shooter} (from the shot effect, every client). */
    public static void fired(LivingEntity shooter, boolean mainHand, int colour, float power) {
        boolean right = mainHand == (shooter.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
        long now = System.nanoTime();
        if (SHOTS.size() > 32) SHOTS.values().removeIf(s -> now - s[3] > 1_000_000_000L);
        SHOTS.put(shooter.getId(), new long[]{right ? 1 : 0, colour & 0xFFFFFF, Math.round(power * 1000), now});
    }

    /** The light of a hand: after a shot of that hand brighter and towards the gun colour ({@link SuitGlowRules#fist}). */
    private static Light fist(LivingEntity entity, boolean right, Light light) {
        long[] shot = SHOTS.get(entity.getId());
        if (shot == null || shot[0] != (right ? 1 : 0)) return light;
        float k = SuitGlowRules.fist((System.nanoTime() - shot[3]) / 1.0E9F, shot[2] / 1000F);
        if (k <= 0F) return light;
        return new Light(light.model(), SuitGlowRules.mix(light.colour(), (int) shot[1], 0.6F * k), light.strength() * (1F + 1.6F * k),
            light.seconds(), light.since());
    }

    /** The lit quads of one baked part with the transform of {@code part}; light per vertex from its height and its distance to the core. */
    private static void part(PoseStack pose, VertexConsumer vc, ModelPart part, GlowMasks.Mask mask, String name, Light light) {
        float[][] quads = mask.quads().get(name);
        if (quads == null || quads.length == 0 || part.skipDraw) return;
        pose.pushPose();
        part.translateAndRotate(pose);
        PoseStack.Pose last = pose.last();
        for (float[] q : quads) {
            for (int i = 0; i < 4; i++) {
                int k = i * 5;
                float mx = part.x + q[k], my = part.y + q[k + 1], mz = part.z + q[k + 2];
                float dx = mx - CORE_X, dy = my - CORE_Y, dz = mz - CORE_Z;
                float l = SuitGlowRules.vertexLight(light.strength(), SuitGlowRules.band(my, light.seconds()),
                    SuitGlowRules.powerOn((float) Math.sqrt(dx * dx + dy * dy + dz * dz), light.since()));
                int rgb = SuitGlowRules.scale(light.colour(), l);
                vc.addVertex(last, q[k] / 16F, q[k + 1] / 16F, q[k + 2] / 16F)
                    .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255)
                    .setUv(q[k + 3], q[k + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(last, q[20], q[21], q[22]);
            }
        }
        pose.popPose();
    }
}
