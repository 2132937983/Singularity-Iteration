// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.compat.geckolib;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.WingmanRig;
import com.miophas.singularity_iteration.common.client.suit.SuitKeyframes;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 0.1.11.229 (AN1 part 3): the GeckoLib side of a suited player's keyframe animations, the same bridge as
 * {@link GeckoWingman}: the AN1 rig animated with {@code animations/entity/suit_player.animation.json}, read back and laid
 * on the player's model ({@link WingmanRig#flatten}); armour and every layer draw as before. Loaded only with GeckoLib.
 */
@OnlyIn(Dist.CLIENT)
public final class GeckoSuit implements SuitKeyframes.Driver {
    static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "animations/entity/suit_player.animation.json");
    static final String PREFIX = "animation.suit.";

    private final Animatable animatable = new Animatable();
    private final Model model = new Model();

    private GeckoSuit() {}

    public static GeckoSuit create() { return new GeckoSuit(); }

    private static final Map<String, RawAnimation> RAW = new HashMap<>();

    private static RawAnimation raw(String name) {
        return RAW.computeIfAbsent(name, n -> n.equals("hover") ? RawAnimation.begin().thenLoop(PREFIX + n) : RawAnimation.begin().thenPlay(PREFIX + n));
    }

    static final class Animatable implements GeoReplacedEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private final Map<Integer, Integer> triggers = new HashMap<>();

        @Override public EntityType<?> getReplacingEntityType() { return EntityType.PLAYER; }

        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "suit", 3, this::suit));
        }

        private PlayState suit(AnimationState<Animatable> state) {
            if (!(state.getData(DataTickets.ENTITY) instanceof Player p)) return PlayState.STOP;
            SuitKeyframes.Pick pick = SuitKeyframes.pick(p);
            if (pick == null) { triggers.remove(p.getId()); return PlayState.STOP; }
            Integer last = triggers.put(p.getId(), pick.trigger());
            if (last == null || last != pick.trigger()) state.getController().forceAnimationReset();
            return state.setAndContinue(raw(pick.animation()));
        }
    }

    @SuppressWarnings("deprecation")   // GeckoLib 4.9 marks the abstract one-argument resource getters deprecated, but they must be implemented
    static final class Model extends GeoModel<Animatable> {
        @Override public ResourceLocation getModelResource(Animatable a) { return GeckoWingman.MODEL; }
        @Override public ResourceLocation getTextureResource(Animatable a) { return GeckoWingman.TEXTURE; }
        @Override public ResourceLocation getAnimationResource(Animatable a) { return ANIMATIONS; }
    }

    @Override
    public boolean pose(Player player, HumanoidModel<?> target, float partialTick, SuitKeyframes.Pick pick) {
        model.getBakedModel(GeckoWingman.MODEL);
        AnimationState<Animatable> state = new AnimationState<>(animatable, 0, 0, partialTick, false);
        state.setData(DataTickets.TICK, (double) player.tickCount);
        state.setData(DataTickets.ENTITY, player);
        model.handleAnimations(animatable, player.getId(), state, partialTick);
        float[][] deg = new float[WingmanRig.BONES][3], px = new float[WingmanRig.BONES][3];
        for (int b = 0; b < WingmanRig.BONES; b++) {
            GeoBone bone = model.getBone(WingmanRig.NAMES[b]).orElse(null);
            if (bone == null) return false;
            deg[b][0] = (float) -Math.toDegrees(bone.getRotX());
            deg[b][1] = (float) -Math.toDegrees(bone.getRotY());
            deg[b][2] = (float) Math.toDegrees(bone.getRotZ());
            px[b][0] = bone.getPosX();
            px[b][1] = bone.getPosY();
            px[b][2] = bone.getPosZ();
        }
        if (pick.upper()) { deg[WingmanRig.ROOT] = new float[3]; px[WingmanRig.ROOT] = new float[3]; }
        float[][][] local = WingmanRig.fromAuthored(deg, px);
        WingmanRig.Part[] parts = WingmanRig.flatten(local[0], local[1]);
        float headY = target.head.yRot, headX = target.head.xRot;   // the vanilla look (net head yaw and pitch)
        set(target.body, parts[WingmanRig.BODY]);
        set(target.head, parts[WingmanRig.HEAD]);
        set(target.rightArm, parts[WingmanRig.RIGHT_ARM]);
        set(target.leftArm, parts[WingmanRig.LEFT_ARM]);
        if (!pick.upper()) {
            set(target.rightLeg, parts[WingmanRig.RIGHT_LEG]);
            set(target.leftLeg, parts[WingmanRig.LEFT_LEG]);
        }
        target.head.yRot += headY;
        target.head.xRot += Mth.clamp(headX, -0.8F, 0.8F);
        target.hat.copyFrom(target.head);
        return true;
    }

    private static void set(ModelPart part, WingmanRig.Part p) {
        part.x = p.x();
        part.y = p.y();
        part.z = p.z();
        part.xRot = p.xRot();
        part.yRot = p.yRot();
        part.zRot = p.zRot();
    }
}
