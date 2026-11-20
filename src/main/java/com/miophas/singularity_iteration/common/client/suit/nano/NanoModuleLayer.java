// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.BakedArmorModel;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Worn nano module housings follow the posed limb; lamps respond to the installed unit's state. */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class NanoModuleLayer {
    private static final String HOOD = "module_sealed_helmet";
    private static final ResourceLocation BINDINGS = asset("armor_models/nano_module_bindings.json");
    private static final BakedArmorModel MODEL = new BakedArmorModel(asset("armor_models/nano_modules.json"));
    private static final RenderType SHELL = RenderType.armorCutoutNoCull(asset("textures/models/armor/nano_modules.png"));
    private static final RenderType LAMPS = RenderType.eyes(asset("textures/models/armor/nano_modules_glow.png"));
    private static Map<EquipmentSlot, Map<String, Mount>> mounts = Map.of();

    private record Mount(SuitModuleType type, String limb, String shell, String glow, String grade) {}
    private record Lamp(ModelPart limb, float[][] quads, int tint) {}

    private NanoModuleLayer() {}

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path);
    }

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) NanoModuleLayer::reload);
    }

    private static void reload(ResourceManager resources) {
        var resource = resources.getResource(BINDINGS);
        if (resource.isEmpty()) {
            mounts = Map.of();
            return;
        }
        try (var reader = resource.get().openAsReader()) {
            var entries = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("modules");
            var result = new EnumMap<EquipmentSlot, Map<String, Mount>>(EquipmentSlot.class);
            for (var row : entries) {
                var entry = row.getAsJsonObject();
                SuitModuleType type = SuitModuleType.byId(entry.get("id").getAsString());
                EquipmentSlot slot = EquipmentSlot.byName(entry.get("slot").getAsString());
                String limb = entry.get("part").getAsString();
                if (type == null || !type.fits(slot) || !List.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg").contains(limb))
                    throw new IllegalArgumentException("Invalid nano mount: " + entry);
                String bone = entry.get("bone").getAsString();
                result.computeIfAbsent(slot, key -> new HashMap<>()).put(type.id(), new Mount(type, limb, bone, bone + "_glow",
                        entry.get("mk2").getAsBoolean() ? bone + "_mk2" : null));
            }
            mounts = result;
        } catch (IOException | RuntimeException ex) {
            mounts = Map.of();
            Singularity_Iteration.LOGGER.error("Unable to load nano module mounts {}", BINDINGS, ex);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void coverHair(RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        var hat = event.getRenderer().getModel().hat;
        if (!hat.visible || player.isSpectator() || mounts.isEmpty()) return;
        var stack = player.getItemBySlot(EquipmentSlot.HEAD);
        var filter = SuitModuleType.TOXIN_FILTER;
        if (SuitModules.isNanoPiece(stack) && SuitModules.tier(stack, filter.id()) > 0
                && ArmorFeatureState.isEnabled(stack, filter.featureKey()) && MODEL.geometry().containsKey(HOOD)) {
            // PlayerRenderer reapplies the player's skin switches before each Pre event.
            hat.visible = false;
        }
    }

    private static ModelPart limb(HumanoidModel<?> model, String name) {
        return switch (name) {
            case "head" -> model.head;
            case "body" -> model.body;
            case "left_arm" -> model.leftArm;
            case "right_arm" -> model.rightArm;
            case "left_leg" -> model.leftLeg;
            case "right_leg" -> model.rightLeg;
            default -> throw new IllegalArgumentException(name);
        };
    }

    public static void draw(HumanoidModel<?> model, PoseStack pose, MultiBufferSource buffers, int light,
                            LivingEntity entity, float partial) {
        render(model, null, null, pose, buffers, light, entity, partial);
    }

    public static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
                               ModelPart arm, boolean right, float partial) {
        render(null, arm, right ? "right_arm" : "left_arm", pose, buffers, light, entity, partial);
    }

    private static void render(HumanoidModel<?> model, ModelPart arm, String armName, PoseStack pose,
                               MultiBufferSource buffers, int light, LivingEntity entity, float partial) {
        if (entity.isSpectator() || mounts.isEmpty()) return;
        var geometry = MODEL.geometry();
        boolean lights = FcsClientConfig.suitGlow();
        VertexConsumer solid = null;
        List<Lamp> lamps = null;
        for (var slot : mounts.entrySet()) {
            if (arm != null && slot.getKey() != EquipmentSlot.CHEST) continue;
            ItemStack stack = entity.getItemBySlot(slot.getKey());
            if (!SuitModules.isNanoPiece(stack)) continue;
            for (String id : SuitModules.installed(stack).ids()) {
                var mount = slot.getValue().get(id);
                if (mount == null || (arm != null && !armName.equals(mount.limb))) continue;
                int tier = SuitModules.tier(stack, id);
                ModelPart limb = arm == null ? limb(model, mount.limb) : arm;
                if (!limb.visible || limb.skipDraw) continue;
                if (solid == null) solid = buffers.getBuffer(SHELL);
                BakedArmorModel.draw(pose, solid, limb, geometry.get(mount.shell), light, OverlayTexture.NO_OVERLAY, -1);
                if (tier >= 2 && mount.grade != null)
                    BakedArmorModel.draw(pose, solid, limb, geometry.get(mount.grade), light, OverlayTexture.NO_OVERLAY, -1);
                boolean sealed = mount.type == SuitModuleType.TOXIN_FILTER && ArmorFeatureState.isEnabled(stack, mount.type.featureKey());
                if (sealed) BakedArmorModel.draw(pose, solid, limb, geometry.get(HOOD), light, OverlayTexture.NO_OVERLAY, -1);
                if (!lights) continue;
                float level = NanoModuleSignals.level(entity, stack, mount.type, partial);
                if (level <= 0) continue;
                int strength = Mth.clamp(Math.round(255 * level * FcsClientConfig.suitGlowStrength()), 0, 255);
                int tint = 0xFF000000 | strength * 0x010101;
                if (lamps == null) lamps = new ArrayList<>();
                lamps.add(new Lamp(limb, geometry.get(mount.glow), tint));
                if (sealed) lamps.add(new Lamp(limb, geometry.get(HOOD + "_glow"), tint));
            }
        }
        // An immediate buffer source flushes the shell consumer when the eyes batch begins.
        if (lamps != null) {
            var glow = buffers.getBuffer(LAMPS);
            for (var lamp : lamps)
                BakedArmorModel.draw(pose, glow, lamp.limb, lamp.quads, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lamp.tint);
        }
    }
}
