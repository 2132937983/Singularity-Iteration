// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.BakedArmorModel;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * First-person arm of the Blockbench chestplates (nano, quantum, advanced quantum, heavy quantum).
 *
 * <p>Vanilla does not draw armor on the arm the player sees in first person. This handler draws
 * the whole arm itself: the skin arm, the sleeve layer and then the baked arm geometry of the worn
 * chestplate (the gauntlet), plus the hand cover when {@link FcsClientConfig#handArmorFirstPerson()}
 * is on.
 *
 * <p>0.1.8.3: the arm pose is set up here from a neutral state. Up to 0.1.8.2 the armor was
 * drawn before vanilla posed the shared player model, so it took the arm pose of whatever player
 * was rendered last (a bow, a crossbow, a gun pose, the Special Maneuver flight pose) while the
 * skin arm used the right pose: the gauntlet swung and twisted against the arm. Now both use one
 * pose: empty-hand arm poses, zero motion, {@code xRot = 0} (the pose of vanilla
 * {@code PlayerRenderer.renderHand}), with the flight and gun pose transforms skipped through
 * {@link FirstPersonArm}. The swing and the equip motion come from the pose stack that
 * {@code ItemInHandRenderer} has already set up, as for vanilla.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitHandRenderer {
    private SuitHandRenderer() { }

    /** Chestplate item id -> baked model name (for example "quantum_chestplate"). */
    private static final Map<ResourceLocation, String> BAKED_CHESTS = Map.of(
        id("armor/item_armor_nano_chestplate"), "nano_chestplate",
        id("armor/item_armor_quantum_chestplate"), "quantum_chestplate",
        id("armor/item_armor_advanced_quantum_chestplate"), "advanced_quantum_chestplate",
        id("armor/item_armor_heavy_quantum_chestplate"), "heavy_quantum_chestplate");
    private static final Map<String, BakedArmorModel> MODELS = new HashMap<>();

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (event.isCanceled()) return;
        AbstractClientPlayer player = event.getPlayer();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return;
        String name = BAKED_CHESTS.get(BuiltInRegistries.ITEM.getKey(chest.getItem()));
        if (name == null) return;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        BakedArmorModel baked = MODELS.computeIfAbsent(name, n -> new BakedArmorModel(id("armor_models/" + n + ".json")));
        var geometry = baked.geometry();
        if (geometry.isEmpty()) return;

        boolean right = event.getArm() == HumanoidArm.RIGHT;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        poseFirstPersonArm(model, player, right);

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        ResourceLocation skin = player.getSkin().texture();
        arm.render(pose, buffers.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY);
        if (sleeve.visible) sleeve.render(pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY);

        String base = "textures/models/armor/" + name;
        // Layer 0 (plates, or the original texture) and, with a palette, layer 1 (light strips).
        for (int layer = 0; layer < 2; layer++) {
            int tint = com.miophas.singularity_iteration.common.suit.SuitPalette.tint(chest, layer);
            if (tint == 0) continue;
            ResourceLocation texture = com.miophas.singularity_iteration.common.suit.SuitPalette.texture(chest, base, layer == 1);
            var buffer = com.miophas.singularity_iteration.common.client.render.SiRenderTypes.suitFoil(buffers,
                RenderType.armorCutoutNoCull(texture), layer == 0 && chest.hasFoil());
            BakedArmorModel.draw(pose, buffer, arm, geometry.get(right ? "right_arm" : "left_arm"), light, OverlayTexture.NO_OVERLAY, tint);
            if (FcsClientConfig.handArmorFirstPerson())
                BakedArmorModel.draw(pose, buffer, arm, geometry.get(right ? "right_hand" : "left_hand"), light, OverlayTexture.NO_OVERLAY, tint);
        }
        // 0.1.10.30: the light of the gauntlet (suit glow)
        SuitGlowLayer.drawArm(pose, buffers, player, arm, right, FcsClientConfig.handArmorFirstPerson());
        // 0.1.10.31 (S-9): the palm thruster while hovering in Special Maneuver flight
        SuitThrustLayer.drawPalm(pose, buffers, player, arm, right);
        event.setCanceled(true);
    }

    /**
     * Neutral first-person pose of one arm on the shared player model: only that arm (and its
     * sleeve when the skin shows it) visible, empty-hand poses, zero motion, {@code xRot = 0}.
     */
    static void poseFirstPersonArm(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player, boolean right) {
        model.setAllVisible(false);
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        arm.visible = true;
        sleeve.visible = player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        FirstPersonArm.run(() -> model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        arm.xRot = 0.0F;
        sleeve.copyFrom(arm);
    }
}
