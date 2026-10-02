package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryFlight;
import com.miophas.singularity_iteration.common.armory.ArmoryPiece;
import com.miophas.singularity_iteration.common.armory.ArmoryPieceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Draws a flying suit piece.
 *
 * <p>Armour is drawn with the wearer's real armour model and textures (including dyes,
 * custom models from other mods via {@link IClientItemExtensions}, and enchantment
 * glint), positioned as if worn by a "ghost" body whose dock point is the piece: on
 * arrival the ghost coincides with the player, so the piece clicks seamlessly into the
 * worn armour layer. During the cruise it tumbles, then for the last quarter it turns to
 * the wearer's body yaw and settles. Hand-held items are drawn as their item model.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class ArmoryPieceRenderer extends EntityRenderer<ArmoryPieceEntity> {
    private final HumanoidModel<LivingEntity> inner;
    private final HumanoidModel<LivingEntity> outer;
    private final ItemRenderer items;

    public ArmoryPieceRenderer(EntityRendererProvider.Context context) {
        super(context);
        inner = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        outer = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        items = context.getItemRenderer();
        shadowRadius = 0.15F;
    }

    @Override
    public void render(ArmoryPieceEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack stack = entity.item();
        boolean ret = entity.isReturn();
        float age = entity.age(partialTick);
        if (stack.isEmpty() || (ret && (age < 0 || age > entity.duration() + 1))) return;
        if (!ret && age < -ArmoryFlight.HATCH_TICKS) return;                // still inside the Armory
        Vec3 at = entity.flightPosition(partialTick);
        Vec3 origin = entity.getPosition(partialTick);
        Entity target = entity.target();
        float bodyYaw = target instanceof LivingEntity living ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)
            : target != null ? target.getYRot() : 0;
        float height = target != null ? target.getBbHeight() : 1.8F;
        int flight = entity.duration();
        float latch = entity.latchAge(partialTick);

        // Body alignment k: 0 = flying free (nose along its path), 1 = squared up to the body.
        // Delivery: free while launching and cruising, swings square during the air brake.
        float k;
        float thrustPitch = 0, jitterAmp;
        if (ret) {
            k = 1 - Mth.clamp((age - ArmoryFlight.PURGE_TICKS) / 6.0F, 0, 1);
            jitterAmp = 2.5F * (1 - k);
        } else if (age < 0) {
            k = 1;                                                               // in the hatch, facing out
            jitterAmp = 1.5F;
        } else if (age < flight) {
            double t = age / flight;
            k = (float) ArmoryFlight.ease(Mth.clamp((t - ArmoryFlight.BRAKE_AT) / (1 - ArmoryFlight.BRAKE_AT), 0, 1));
            double accel = t < 0.12 ? 1 - t / 0.12 : 0;
            double brake = t >= ArmoryFlight.BRAKE_AT ? Math.pow(1 - (t - ArmoryFlight.BRAKE_AT) / (1 - ArmoryFlight.BRAKE_AT), 1.5) : 0;
            thrustPitch = (float) (-20 * accel + 30 * brake);                  // lean in on launch, flare on the brake
            jitterAmp = (float) (1.2 + 3.5 * accel + 4.0 * brake);
        } else if (latch < 0) {
            float hover = age - flight;
            thrustPitch = 30 * (float) Math.exp(-hover / 2.0);                  // flare settling out
            k = 1;
            jitterAmp = 1.6F;
        } else {
            k = 1;
            jitterAmp = 0;
        }

        // Travel attitude: nose along the velocity, pitch into climbs/dives, bank hard into turns.
        Vec3 v0 = entity.flightPosition(partialTick - 0.5F), v1 = entity.flightPosition(partialTick + 0.5F);
        Vec3 v2 = entity.flightPosition(partialTick + 1.5F);
        Vec3 vel = v1.subtract(v0), vel2 = v2.subtract(v1);
        float travelYaw = vel.horizontalDistanceSqr() > 1.0E-5 ? (float) Math.toDegrees(Math.atan2(-vel.x, vel.z)) : bodyYaw;
        float nextYaw = vel2.horizontalDistanceSqr() > 1.0E-5 ? (float) Math.toDegrees(Math.atan2(-vel2.x, vel2.z)) : travelYaw;
        float pitch = (float) Math.toDegrees(Math.atan2(vel.y, Math.sqrt(vel.horizontalDistanceSqr()) + 1.0E-4));
        float roll = Mth.clamp(Mth.wrapDegrees(nextYaw - travelYaw) * 4.5F, -55.0F, 55.0F);
        // thruster buffeting: two incommensurate frequencies so it never looks periodic
        float buffet = jitterAmp * (Mth.sin(age * 2.3F) + 0.5F * Mth.sin(age * 5.1F + 1.3F));
        float buffetYaw = jitterAmp * 0.6F * Mth.sin(age * 3.7F + 0.4F);

        float yaw = Mth.rotLerp(k, 180.0F - travelYaw, 180.0F - bodyYaw) + buffetYaw;
        float scale = ret ? 1 + 0.08F * (float) Math.sin(Math.min(age, ArmoryFlight.PURGE_TICKS) / ArmoryFlight.PURGE_TICKS * Math.PI)
            : ArmoryFlight.latchScale(latch);
        float wobble = buffet - thrustPitch;
        pose.pushPose();
        pose.translate(at.x - origin.x, at.y - origin.y, at.z - origin.z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees((1 - k) * -pitch * 0.8F + wobble));
        pose.mulPose(Axis.ZP.rotationDegrees((1 - k) * roll + buffet * 0.5F));
        pose.scale(scale, scale, scale);
        ArmoryPiece piece = entity.piece();
        boolean drawn = false;
        boolean flash = !ret && latch >= 0 && latch < 3.5F;
        if (piece.isArmor() && stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == piece.slot) {
            drawn = renderArmor(pose, buffers, light, stack, armor, piece, height, target, flash);
        }
        if (!drawn) {
            float s = 0.55F + 0.2F * k;
            pose.scale(s, s, s);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        }
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    private boolean renderArmor(PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack, ArmorItem armor,
                                ArmoryPiece piece, float height, Entity target, boolean flash) {
        LivingEntity wearer = target instanceof LivingEntity l ? l : Minecraft.getInstance().player;
        if (wearer == null) return false;
        pose.pushPose();
        try {
            EquipmentSlot slot = piece.slot;
            boolean innerLayer = slot == EquipmentSlot.LEGS;
            HumanoidModel<LivingEntity> base = innerLayer ? inner : outer;
            base.setAllVisible(false);
            switch (slot) {
                case HEAD -> { base.head.visible = true; base.hat.visible = true; }
                case CHEST -> { base.body.visible = true; base.rightArm.visible = true; base.leftArm.visible = true; }
                case LEGS -> { base.body.visible = true; base.rightLeg.visible = true; base.leftLeg.visible = true; }
                case FEET -> { base.rightLeg.visible = true; base.leftLeg.visible = true; }
                default -> { }
            }
            base.crouching = false;
            base.young = false;
            var model = ClientHooks.getArmorModel(wearer, stack, slot, base);

            // ghost body: dock point at the origin, feet below it, standard living-entity transform
            pose.translate(0, -height * piece.heightFraction, 0);
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.translate(0, -1.501F, 0);
            ArmorMaterial material = armor.getMaterial().value();
            IClientItemExtensions ext = IClientItemExtensions.of(stack);
            int fallback = ext.getDefaultDyeColor(stack);
            for (int i = 0; i < material.layers().size(); i++) {
                ArmorMaterial.Layer layer = material.layers().get(i);
                int color = ext.getArmorLayerTintColor(stack, wearer, layer, i, fallback);
                if (color == 0) continue;
                ResourceLocation texture = ClientHooks.getArmorTexture(wearer, stack, layer, innerLayer, slot);
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY, color);
            }
            // Enchantment glint, and a shimmer while the plate seats and locks.
            if (stack.hasFoil() || flash) {
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY);
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        } finally {
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ArmoryPieceEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
