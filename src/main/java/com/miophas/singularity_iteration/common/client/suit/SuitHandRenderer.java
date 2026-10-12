// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.BakedArmorModel;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * First-person arm armor of the nano and quantum suits (0.1.8.0).
 *
 * <p>Vanilla does not draw armor on the arm that the player sees in first person. This handler
 * draws the baked arm geometry of the worn chestplate (the gauntlet) on that arm, and the hand
 * cover when {@link FcsClientConfig#handArmorFirstPerson()} is on.
 *
 * <p>The handler runs before vanilla draws the arm. It poses the player model as vanilla does
 * for the first-person arm ({@code setupAnim} with zero motion, arm {@code xRot = 0}), so the
 * armor lines up with the skin arm. The depth buffer orders the two. The event is not cancelled.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitHandRenderer {
    private SuitHandRenderer() { }

    /** Chestplate item id -> baked set name (for example "quantum_chestplate"). */
    private static final Map<ResourceLocation, String> BAKED_CHESTS = Map.of(
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor/item_armor_nano_chestplate"), "nano_chestplate",
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor/item_armor_quantum_chestplate"), "quantum_chestplate");
    private static final Map<String, BakedArmorModel> MODELS = new HashMap<>();

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return;
        String name = BAKED_CHESTS.get(BuiltInRegistries.ITEM.getKey(chest.getItem()));
        if (name == null) return;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        BakedArmorModel baked = MODELS.computeIfAbsent(name, n ->
            new BakedArmorModel(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor_models/" + n + ".json")));
        var geometry = baked.geometry();
        if (geometry.isEmpty()) return;
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        // The same pose as PlayerRenderer.renderHand.
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ModelPart arm = right ? model.rightArm : model.leftArm;
        arm.xRot = 0.0F;
        String base = "textures/models/armor/" + name;
        int light = event.getPackedLight();
        // Layer 0 (plates, or the original texture) and, with a palette, layer 1 (light strips).
        for (int layer = 0; layer < 2; layer++) {
            int tint = com.miophas.singularity_iteration.common.suit.SuitPalette.tint(chest, layer);
            if (tint == 0) continue;
            ResourceLocation texture = com.miophas.singularity_iteration.common.suit.SuitPalette.texture(chest, base, layer == 1);
            var buffer = ItemRenderer.getArmorFoilBuffer(event.getMultiBufferSource(), RenderType.armorCutoutNoCull(texture), layer == 0 && chest.hasFoil());
            BakedArmorModel.draw(event.getPoseStack(), buffer, arm, geometry.get(right ? "right_arm" : "left_arm"), light, OverlayTexture.NO_OVERLAY, tint);
            if (FcsClientConfig.handArmorFirstPerson())
                BakedArmorModel.draw(event.getPoseStack(), buffer, arm, geometry.get(right ? "right_hand" : "left_hand"), light, OverlayTexture.NO_OVERLAY, tint);
        }
    }
}
