// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.compat.geckolib;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.ArmoryRegistry;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanKeyframeChoice;
import com.miophas.singularity_iteration.common.armory.WingmanRig;
import com.miophas.singularity_iteration.common.client.armory.WingmanKeyframes;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
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
 * 0.1.11.225 (Wingman motion A0): the GeckoLib side of the Wingman's keyframe animations. Loaded only when GeckoLib is
 * installed ({@link WingmanKeyframes#init}).
 *
 * <p>GeckoLib does not draw the Wingman: one {@link GeoReplacedEntity} stands for every Wingman (an animation instance per
 * entity id), its controllers pick the animations of {@link WingmanKeyframeChoice}, and each frame {@link #pose} runs
 * GeckoLib's animation step on the AN1 rig ({@code geo/entity/si_humanoid_rig.geo.json}) without rendering it, reads the
 * seven bones, turns GeckoLib's values back into the authored ones (GeckoLib negates the x and y turns when it loads them)
 * and lays them on the vanilla model with {@link WingmanRig#flatten}. Every armour, layer and held item draws as before.
 */
@OnlyIn(Dist.CLIENT)
public final class GeckoWingman implements WingmanKeyframes.Driver {
    static final ResourceLocation MODEL = id("geo/entity/si_humanoid_rig.geo.json");
    static final ResourceLocation ANIMATIONS = id("animations/entity/wingman.animation.json");
    static final ResourceLocation TEXTURE = id("textures/entity/wingman_endoskeleton.png");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path); }

    private final Animatable animatable = new Animatable();
    private final Model model = new Model();

    private GeckoWingman() {}

    public static GeckoWingman create() { return new GeckoWingman(); }

    private static final Map<String, RawAnimation> ONCE = new HashMap<>(), LOOP = new HashMap<>();

    private static RawAnimation once(String name) {
        return ONCE.computeIfAbsent(name, n -> RawAnimation.begin().thenPlay(WingmanKeyframeChoice.PREFIX + n));
    }

    /** 0.1.11.229: action animations that loop, and those that hold their last frame. */
    private static final java.util.Set<String> ACTION_LOOPS = java.util.Set.of("aim_rifle", "aim_pistol", "cover_crouch", "slam_fall"),
        ACTION_HOLDS = java.util.Set.of("bow_draw", "peek_left", "peek_right");
    private static final Map<String, RawAnimation> ACTIONS = new HashMap<>();

    private static RawAnimation action(String name) {
        return ACTIONS.computeIfAbsent(name, n -> ACTION_LOOPS.contains(n) ? RawAnimation.begin().thenLoop(WingmanKeyframeChoice.PREFIX + n)
            : ACTION_HOLDS.contains(n) ? RawAnimation.begin().thenPlayAndHold(WingmanKeyframeChoice.PREFIX + n)
            : RawAnimation.begin().thenPlay(WingmanKeyframeChoice.PREFIX + n));
    }

    /** Animations that loop by themselves (0.1.11.226: the gaits and the air). */
    private static final java.util.Set<String> LOOPS = java.util.Set.of("idle_stand", "walk", "run", "skate", "air_rise", "air_fall", "cover_crouch");

    private static RawAnimation base(String name) {
        // the landing plays once into the stand; the idles loop (idle_look and idle_check play once, then stand)
        if (LOOPS.contains(name) && !name.equals("idle_stand")) return LOOP.computeIfAbsent(name, n -> RawAnimation.begin().thenLoop(WingmanKeyframeChoice.PREFIX + n));
        if (name.equals("thrust_hop") || name.equals("jump_takeoff"))   // the take-off rises into the air pose
            return LOOP.computeIfAbsent(name, n -> RawAnimation.begin().thenPlay(WingmanKeyframeChoice.PREFIX + n).thenLoop(WingmanKeyframeChoice.PREFIX + "air_rise"));
        if (name.equals(WingmanKeyframeChoice.FORCED)) return LOOP.computeIfAbsent(name, n -> RawAnimation.begin().thenPlayAndHold(WingmanKeyframeChoice.PREFIX + n));
        return LOOP.computeIfAbsent(name, n -> n.equals("idle_stand") ? RawAnimation.begin().thenLoop(WingmanKeyframeChoice.PREFIX + n)
            : RawAnimation.begin().thenPlay(WingmanKeyframeChoice.PREFIX + n).thenLoop(WingmanKeyframeChoice.PREFIX + "idle_stand"));
    }

    /** The animatable of all Wingmen. */
    static final class Animatable implements GeoReplacedEntity {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public EntityType<?> getReplacingEntityType() { return ArmoryRegistry.WINGMAN.get(); }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "base", 3, this::base));
            controllers.add(new AnimationController<>(this, "action", 2, this::action));   // 0.1.11.229: over the gait
            controllers.add(new AnimationController<>(this, "react", 1, this::react));
        }

        private PlayState base(AnimationState<Animatable> state) {
            if (!(state.getData(DataTickets.ENTITY) instanceof WingmanEntity w)) return PlayState.STOP;
            float pt = state.getPartialTick();
            if (w.ground() && !w.landed()) {   // 0.1.11.226 (G3): on foot
                WingmanKeyframeChoice.Pick pick = WingmanKeyframeChoice.onFoot(w.onGround(), w.footSpeed(), w.getY() - w.yo, w.sinceHop(pt), w.hopThrust(),
                    w.sinceTouch(pt), w.heavyTouch(), w.getId(), w.tickCount);
                state.setControllerSpeed(pick.speed());
                return state.setAndContinue(GeckoWingman.base(pick.name()));
            }
            state.setControllerSpeed(1);
            String name = WingmanKeyframeChoice.base(w.landed(), w.sinceLanding(pt), w.getId(), w.tickCount);
            if (name == null) return PlayState.STOP;
            return state.setAndContinue(GeckoWingman.base(name));
        }

        /** The last trigger each Wingman's action controller started (a new one restarts the animation). */
        private final java.util.Map<Integer, Integer> triggers = new java.util.HashMap<>();

        private PlayState action(AnimationState<Animatable> state) {
            if (!(state.getData(DataTickets.ENTITY) instanceof WingmanEntity w)) return PlayState.STOP;
            WingmanKeyframeChoice.Act act = WingmanKeyframes.action(w, state.getPartialTick());
            if (act == null) { triggers.remove(w.getId()); return PlayState.STOP; }
            Integer last = triggers.put(w.getId(), act.trigger());
            if (last == null || last != act.trigger()) state.getController().forceAnimationReset();
            return state.setAndContinue(GeckoWingman.action(act.name()));
        }

        private PlayState react(AnimationState<Animatable> state) {
            if (!(state.getData(DataTickets.ENTITY) instanceof WingmanEntity w)) return PlayState.STOP;
            AnimationController<Animatable> c = state.getController();
            if (w.hurtTime > 0 && w.hurtTime >= w.hurtDuration - 1) c.forceAnimationReset();   // a fresh blow restarts it
            if (w.hurtTime <= 0 && (c.getCurrentAnimation() == null || c.hasAnimationFinished())) return PlayState.STOP;
            return state.setAndContinue(once(WingmanKeyframeChoice.hit(w.yBodyRot, w.getHurtDir() + w.yBodyRot)));
        }
    }

    /** The AN1 rig and the Wingman's animations; the texture is never drawn. */
    @SuppressWarnings("deprecation")   // GeckoLib 4.9 marks the abstract one-argument resource getters deprecated, but they must be implemented
    static final class Model extends GeoModel<Animatable> {
        @Override public ResourceLocation getModelResource(Animatable a) { return MODEL; }
        @Override public ResourceLocation getTextureResource(Animatable a) { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(Animatable a) { return ANIMATIONS; }
    }

    @Override
    public boolean pose(WingmanEntity entity, HumanoidModel<?> target, float partialTick, WingmanKeyframes.Mask mask) {
        if (!WingmanKeyframes.active(entity, partialTick) && !WingmanKeyframes.actionActive(entity, partialTick)) return false;
        boolean upper = mask != WingmanKeyframes.Mask.LEGS, legs = mask != WingmanKeyframes.Mask.UPPER;
        model.getBakedModel(MODEL);
        AnimationState<Animatable> state = new AnimationState<>(animatable, 0, 0, partialTick, false);
        state.setData(DataTickets.TICK, (double) entity.tickCount);
        state.setData(DataTickets.ENTITY, entity);
        model.handleAnimations(animatable, entity.getId(), state, partialTick);
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
        if (mask != WingmanKeyframes.Mask.FULL) { deg[WingmanRig.ROOT] = new float[3]; px[WingmanRig.ROOT] = new float[3]; }   // a part alone: no whole-body offset
        float[][][] local = WingmanRig.fromAuthored(deg, px);
        WingmanRig.Part[] parts = WingmanRig.flatten(local[0], local[1]);
        if (upper) {
            set(target.body, parts[WingmanRig.BODY]);
            set(target.head, parts[WingmanRig.HEAD]);
            set(target.rightArm, parts[WingmanRig.RIGHT_ARM]);
            set(target.leftArm, parts[WingmanRig.LEFT_ARM]);
        }
        if (legs) {
            set(target.rightLeg, parts[WingmanRig.RIGHT_LEG]);
            set(target.leftLeg, parts[WingmanRig.LEFT_LEG]);
        }
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
