// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.BakedArmorModel;
import com.miophas.singularity_iteration.common.client.render.FxBudget;
import com.miophas.singularity_iteration.common.client.suit.nano.NanoRender;
import com.miophas.singularity_iteration.common.suit.SuitPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 0.1.10.32 (S-6, A-5): the hit ripple over the armour of a nano or quantum suit. The server reports a hit the quantum armour
 * absorbed ({@code FX_ARMOR_HIT}, with or without the deflector); the full boot of an Armory suit-up starts one from the
 * chest core ({@link #core}). Every worn baked piece is drawn once more in the additive hexagon mesh of the nano kinetic
 * armour ({@code NanoRender.HEXMESH}), each vertex as bright as {@link SuitFxRules#ripple} at its distance to the hit point.
 * A whole-body pass: within the frame budget ({@link FxBudget}). Option {@code hit_ripple}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitRippleLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** One ripple: from the body centre along {@code dir} (or from the chest core), strength, game time it started. */
    record Ripple(Vec3 dir, boolean core, float strength, long born) { }

    private static final Int2ObjectOpenHashMap<List<Ripple>> RIPPLES = new Int2ObjectOpenHashMap<>();
    private static final RippleVertex VERTEX = new RippleVertex();

    public SuitRippleLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new SuitRippleLayer<>(renderer));
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer renderer && renderer.getModel() instanceof HumanoidModel) {
            renderer.addLayer(new SuitRippleLayer(renderer));
        }
    }

    /** From the effect packet: a hit of entity {@code id} from {@code dir}, {@code share} of it absorbed. */
    public static void hit(int id, Vec3 dir, float share) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !FcsClientConfig.hitRipple()) return;
        add(id, new Ripple(dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize(), false, SuitFxRules.rippleStrength(share),
            mc.level.getGameTime()));
    }

    /** A ripple from the chest core of {@code e} (the suit boot). */
    public static void core(LivingEntity e) {
        if (!FcsClientConfig.hitRipple()) return;
        add(e.getId(), new Ripple(Vec3.ZERO, true, 1F, e.level().getGameTime()));
    }

    private static void add(int id, Ripple r) {
        List<Ripple> list = RIPPLES.computeIfAbsent(id, k -> new ArrayList<>());
        list.removeIf(old -> r.born - old.born >= SuitFxRules.RIPPLE_TICKS);
        if (list.size() >= 4) list.remove(0);
        list.add(r);
    }

    /** Drops finished ripples (once a client tick). */
    public static void prune(long now) {
        RIPPLES.values().removeIf(list -> {
            list.removeIf(r -> now - r.born >= SuitFxRules.RIPPLE_TICKS);
            return list.isEmpty();
        });
    }

    public static void clear() {
        RIPPLES.clear();
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partial, float age, float headYaw, float headPitch) {
        List<Ripple> list = RIPPLES.get(entity.getId());
        if (list == null || list.isEmpty() || !FcsClientConfig.hitRipple()) return;
        if (!FxBudget.allow(entity)) return;
        draw(getParentModel(), pose, buffers, entity, partial, list);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void draw(HumanoidModel<?> parent, PoseStack pose, MultiBufferSource buffers, LivingEntity entity, float partial, List<Ripple> list) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 feet = entity.getPosition(partial);
        float yaw = (float) Math.toRadians(net.minecraft.util.Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        float now = entity.level().getGameTime() + partial;
        int n = 0;
        for (Ripple r : list) {
            float rage = now - r.born;
            if (rage < 0 || rage >= SuitFxRules.RIPPLE_TICKS || n >= RippleVertex.MAX) continue;
            Vec3 at = r.core ? feet.add(0, entity.getBbHeight() * 0.69, 0).add(forward.scale(0.16))
                : feet.add(0, entity.getBbHeight() * 0.55, 0).add(r.dir.scale(0.45));
            VERTEX.set(n++, at.subtract(cam), rage, r.strength);
        }
        if (n == 0) return;
        VERTEX.count = n;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            String name = SuitGlowLayer.modelOf(stack);
            if (name == null) continue;
            BakedArmorModel model = SuitGlowLayer.model(name);
            var geometry = model.geometry();
            if (geometry.isEmpty()) continue;
            ((HumanoidModel) parent).copyPropertiesTo(model);
            SuitPalette palette = SuitPalette.of(stack);
            int base = palette != null ? palette.accent() : name.startsWith("nano") ? SuitGlowRules.NANO : SuitGlowRules.QUANTUM;
            VERTEX.colour = SuitGlowRules.colour(base, SuitGlowLayer.charge(stack));
            VERTEX.delegate = buffers.getBuffer(NanoRender.HEXMESH);
            switch (slot) {
                case HEAD -> part(pose, model.head, geometry.get("head"));
                case CHEST -> {
                    part(pose, model.body, geometry.get("body"));
                    part(pose, model.rightArm, geometry.get("right_arm"));
                    part(pose, model.leftArm, geometry.get("left_arm"));
                    if (FcsClientConfig.handArmorThirdPerson()) {
                        part(pose, model.rightArm, geometry.get("right_hand"));
                        part(pose, model.leftArm, geometry.get("left_hand"));
                    }
                }
                case LEGS -> {
                    part(pose, model.body, geometry.get("body"));
                    part(pose, model.rightLeg, geometry.get("right_leg"));
                    part(pose, model.leftLeg, geometry.get("left_leg"));
                }
                default -> {
                    part(pose, model.rightLeg, geometry.get("right_leg"));
                    part(pose, model.leftLeg, geometry.get("left_leg"));
                }
            }
        }
        VERTEX.delegate = null;
    }

    private static void part(PoseStack pose, ModelPart part, float[][] quads) {
        if (quads == null || quads.length == 0 || !part.visible) return;
        BakedArmorModel.draw(pose, VERTEX, part, quads, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }

    /**
     * Vertex sink of the ripple pass: the positions arrive camera-relative (the entity pose), so each vertex finds its
     * distance to the ripple points (also camera-relative) and takes the ripple light times the colour.
     */
    static final class RippleVertex implements VertexConsumer {
        static final int MAX = 4;
        final double[] px = new double[MAX], py = new double[MAX], pz = new double[MAX];
        /** Prepared ripples (radius, strength x fade, spot), see {@link SuitFxRules#prepare}. */
        final float[] prep = new float[MAX * 3];
        int count;
        int colour;
        VertexConsumer delegate;
        private float x, y, z;

        void set(int i, Vec3 at, float age, float strength) {
            px[i] = at.x;
            py[i] = at.y;
            pz[i] = at.z;
            SuitFxRules.prepare(age, strength, prep, i * 3);
        }

        /** Light of the ripples at a point. */
        float light(double x, double y, double z) {
            float l = 0;
            for (int i = 0; i < count; i++) {
                double dx = x - px[i], dy = y - py[i], dz = z - pz[i];
                l += SuitFxRules.rippleAt(dx * dx + dy * dy + dz * dz, prep, i * 3);
            }
            return Math.min(1.5F, l);
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            delegate.addVertex(x, y, z);
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            float l = light(x, y, z);
            int rgb = SuitGlowRules.scale(colour, l);
            delegate.setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255);
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    }
}
