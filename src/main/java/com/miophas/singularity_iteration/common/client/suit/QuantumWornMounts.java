// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.BakedArmorModel;
import com.miophas.singularity_iteration.common.suit.QuantumNozzles;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitPalette;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.common.suit.core.FrameCore;
import com.miophas.singularity_iteration.common.suit.core.FrameCores;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Worn quantum equipment. Item renders and idle pieces use the ordinary stowed bake. */
public final class QuantumWornMounts {
    private QuantumWornMounts() {}
    private static final ResourceLocation MODEL = id("armor_models/quantum_mounts.json");
    // All four pieces share the same approved atlas; existing per-piece palette layers still apply.
    private static final ResourceLocation GLOW = id("textures/models/armor/quantum_mounts_glow.png");
    private record Textures(ResourceLocation base, ResourceLocation plates, ResourceLocation accent) {
        Textures(String piece) {
            this(tex(piece, ""), tex(piece, "_plating"), tex(piece, "_accent"));
        }
    }
    private static final Map<EquipmentSlot, Textures> SHELL = Map.of(
        EquipmentSlot.HEAD, new Textures("helmet"), EquipmentSlot.CHEST, new Textures("chestplate"),
        EquipmentSlot.LEGS, new Textures("leggings"), EquipmentSlot.FEET, new Textures("boots"));
    private record Mount(String kind, String id, EquipmentSlot slot, String part, float[][] faces) {}
    private record Geometry(List<Mount> mounts, Set<Integer> movingFaces) {}
    private static Geometry data;
    private static final Map<Map<String,float[][]>, Map<String,float[][]>> STATIONARY = new IdentityHashMap<>();

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path); }
    private static ResourceLocation tex(String piece, String suffix) { return id("textures/models/armor/quantum_" + piece + suffix + ".png"); }
    public static void invalidate() { data = null; STATIONARY.clear(); }

    private static Geometry geometry() {
        if (data != null) return data;
        var mounts = new ArrayList<Mount>();
        var moving = new HashSet<Integer>();
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(MODEL)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            var parts = BakedArmorModel.parse(root);
            for (var entry : root.getAsJsonArray("segments")) {
                var s = entry.getAsJsonObject();
                float[][] faces = parts.get(s.get("name").getAsString());
                mounts.add(new Mount(s.get("kind").getAsString(), s.get("id").getAsString(),
                    EquipmentSlot.byName(s.get("slot").getAsString()), s.get("part").getAsString(), faces));
                if (s.get("moving").getAsBoolean()) for (float[] q : faces) moving.add(faceKey(q));
            }
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.warn("Cannot load quantum armor mounts {}", MODEL, e);
            mounts.clear(); moving.clear();
        }
        return data = new Geometry(List.copyOf(mounts), Set.copyOf(moving));
    }

    // Every authored face has a non-overlapping 1:1 UV chart on the 128-square atlas.
    private static int faceKey(float[] q) {
        int x = (int) ((q[3]+q[8]+q[13]+q[18]) * 32F);
        int y = (int) ((q[4]+q[9]+q[14]+q[19]) * 32F);
        return y * 128 + x;
    }
    private static boolean flying(@Nullable LivingEntity e) {
        return e instanceof Player p && ManeuverModeClient.inManeuver(p)
            && SuitModules.isQuantumPiece(p.getItemBySlot(EquipmentSlot.CHEST));
    }

    /** Use the maneuver renderer's existing full-burn throttle range. No speed or flight-state change. */
    public static float podOpening(LivingEntity e, float partial) {
        if (!flying(e) || !FcsClientConfig.suitGimmicks() || NozzleDraw.variant(e) == QuantumNozzles.Variant.GRAVITON) return 0;
        return Mth.clamp((ManeuverModeClient.throttleOf((Player)e, partial) - .72F) / .2F, 0, 1);
    }

    /** A flying wearer redraws the halo, wings and two pods in their animated positions. */
    public static Map<String,float[][]> stationaryGeometry(ResourceLocation source, Map<String,float[][]> all,
                                                           @Nullable LivingEntity entity, float partial) {
        if (!source.getNamespace().equals(Singularity_Iteration.MOD_ID)
            || !(source.getPath().equals("armor_models/quantum_helmet.json") || source.getPath().equals("armor_models/quantum_chestplate.json"))
            || !flying(entity) || !FcsClientConfig.suitGimmicks()) return all;
        return STATIONARY.computeIfAbsent(all, input -> {
            Set<Integer> moving = geometry().movingFaces();
            var kept = new HashMap<>(input);
            for (String part : List.of("head", "body")) {
                if (input.containsKey(part))
                    kept.put(part, Arrays.stream(input.get(part)).filter(q -> !moving.contains(faceKey(q))).toArray(float[][]::new));
            }
            return kept;
        });
    }

    public static void draw(HumanoidModel<?> model, PoseStack pose, MultiBufferSource buffers, int light,
                            LivingEntity entity, float partial) {
        draw(model, pose, buffers, light, entity, partial, null);
    }
    public static void drawArm(HumanoidModel<?> model, PoseStack pose, MultiBufferSource buffers, int light,
                               LivingEntity entity, float partial, boolean right) {
        draw(model, pose, buffers, light, entity, partial, right ? "right" : "left");
    }

    private static void draw(HumanoidModel<?> model, PoseStack pose, MultiBufferSource buffers, int light,
                             LivingEntity entity, float partial, @Nullable String arm) {
        if (!SuitModules.isQuantumPiece(entity.getItemBySlot(EquipmentSlot.CHEST))
            && (arm != null || (!SuitModules.isQuantumPiece(entity.getItemBySlot(EquipmentSlot.HEAD))
            && !SuitModules.isQuantumPiece(entity.getItemBySlot(EquipmentSlot.LEGS))
            && !SuitModules.isQuantumPiece(entity.getItemBySlot(EquipmentSlot.FEET))))) return;
        boolean flying = flying(entity);
        float opening = flying ? podOpening(entity, partial) : 0;
        float throttle = flying ? ManeuverModeClient.throttleOf((Player)entity, partial) : 0;
        boolean moving = flying && FcsClientConfig.suitGimmicks();
        boolean hover = flying && throttle < .35F;
        float bob = hover ? Mth.sin(((entity.tickCount + partial) % 64F) * Mth.TWO_PI / 64F) : 0;
        Map<String, Float> jets = Map.of();
        if (flying && FcsClientConfig.suitGlow()) {
            List<QuantumNozzles.Nozzle> nozzles = NozzleDraw.worn(entity, partial);
            float[] powers = ManeuverModeClient.nozzleOutput((Player)entity, nozzles, partial);
            if (powers != null) {
                jets = new HashMap<>();
                for (int i=0; i<powers.length; i++) if (powers[i] > .02F) jets.put(nozzles.get(i).name(), powers[i]);
            }
        }
        for (Mount m : geometry().mounts()) {
            if (arm != null && (m.slot != EquipmentSlot.CHEST || !m.part.startsWith(arm + "_"))) continue;
            ModelPart part = switch (m.part) {
                case "head" -> model.head; case "body" -> model.body;
                case "left_arm","left_hand" -> model.leftArm; case "right_arm","right_hand" -> model.rightArm;
                case "left_leg" -> model.leftLeg; case "right_leg" -> model.rightLeg;
                default -> null;
            };
            if (part == null || !part.visible || part.skipDraw) continue;
            ItemStack stack = entity.getItemBySlot(m.slot);
            if (!SuitModules.isQuantumPiece(stack)) continue;
            float intensity = SuitModules.energy(stack) > 0 ? 1F : 0;
            boolean surface = true;
            switch (m.kind) {
                case "unit" -> {
                    var type = SuitModuleType.byId(m.id);
                    if (type == null || SuitModules.tier(stack, m.id) <= 0) continue;
                    if (type.switchable()) {
                        boolean powered = !type.needsPower() || (entity instanceof Player p
                            ? SuitModules.canPay(p, stack, Math.max(1, type.drainPerTick())) : intensity > 0);
                        boolean visor = !type.needsVisor() || (entity instanceof Player p && SuitModules.hasVisor(p, UnitRef.of(type)));
                        intensity = powered && visor && ArmorFeatureState.isEnabled(stack,type.featureKey()) ? 1F : 0F;
                    }
                }
                case "bay" -> { if (SuitModules.bayLevel(stack) == 0) continue; }
                case "frame_core" -> {
                    var core = FrameCores.core(stack);
                    if (!FcsClientConfig.coreAttachments() || core == null || !core.id().equals(m.id)
                        || !ArmorFeatureState.isEnabled(stack,FrameCore.KEY_VISUAL)) continue;
                    if (core.isSolar()) intensity = ArmorFeatureState.isEnabled(stack, FrameCore.KEY_SOLAR)
                        && entity.level().isDay() && entity.level().canSeeSky(entity.blockPosition().above()) ? 1F : 0F;
                }
                case "halo", "wing", "mach_pod" -> { if (!moving) continue; }
                case "mach_rail" -> { if (opening < 1F) continue; }
                case "hover_strip" -> { if (!hover) continue; }
                case "rcs" -> { intensity = jets.getOrDefault(m.id, 0F); if (intensity == 0) continue; surface = false; }
                default -> { continue; }
            }
            pose.pushPose();
            part.translateAndRotate(pose);
            if (m.kind.equals("wing")) {
                pose.translate(0, (.12F * bob + .15F * opening) / 16F, .28F * opening / 16F);
                pivotX(pose, 24F - 18.32F, 5.67F, 6F * opening);
            } else if (m.kind.equals("halo")) {
                pose.translate(0, -.1F * bob / 16F, 0);
            } else if (m.kind.equals("mach_pod") || m.kind.equals("mach_rail")) {
                pose.translate(0, QuantumNozzles.POD_SLIDE_Y * opening / 16F, QuantumNozzles.POD_SLIDE_Z * opening / 16F);
                pivotX(pose, QuantumNozzles.POD_HINGE_Y, QuantumNozzles.POD_HINGE_Z, QuantumNozzles.POD_ANGLE * opening);
            }
            var palette = SuitPalette.of(stack);
            if (surface) {
                var tex = SHELL.get(m.slot);
                if (palette == null) render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(tex.base)), m.faces, light, -1);
                else {
                    render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(tex.plates)), m.faces, light, 0xFF000000 | palette.primary());
                    render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(tex.accent)), m.faces, light, 0xFF000000 | palette.accent());
                }
            }
            if (FcsClientConfig.suitGlow() && intensity > 0) {
                int tint = palette == null ? 0xFFFFFF : palette.accent();
                float level = Math.min(1F, intensity * FcsClientConfig.suitGlowStrength());
                int color = 0xFF000000 | SuitGlowRules.scale(tint, level);
                render(pose, buffers.getBuffer(RenderType.eyes(GLOW)), m.faces, LightTexture.FULL_BRIGHT, color);
            }
            pose.popPose();
        }
    }

    private static void pivotX(PoseStack pose, float y, float z, float degrees) {
        pose.translate(0,y / 16F,z / 16F);
        pose.mulPose(Axis.XP.rotationDegrees(degrees));
        pose.translate(0,-y / 16F,-z / 16F);
    }
    private static void render(PoseStack pose, VertexConsumer out, float[][] faces, int light, int color) {
        var last = pose.last();
        for (var q : faces) for (int i = 0; i < 20; i += 5)
            out.addVertex(last,q[i] / 16F,q[i+1] / 16F,q[i+2] / 16F).setColor(color)
                .setUv(q[i+3],q[i+4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(last,q[20],q[21],q[22]);
    }
}
