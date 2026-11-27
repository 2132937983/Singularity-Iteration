// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanKeyframeChoice;
import com.miophas.singularity_iteration.common.armory.WingmanPose;
import net.minecraft.client.model.HumanoidModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 0.1.11.225 (Wingman motion A0): the optional keyframe animations of the Wingman. GeckoLib is an optional dependency; with
 * it installed, {@link #init} sets a {@link Driver} (the GeckoLib classes are only touched there, so the mod runs without
 * them), and the renderer asks it to pose the model each frame. Without GeckoLib, or when the driver fails once, the
 * code-driven poses of {@link WingmanPose} stay.
 */
@OnlyIn(Dist.CLIENT)
public final class WingmanKeyframes {
    private WingmanKeyframes() {}

    private static final Logger LOG = LoggerFactory.getLogger("mio_icif/wingman-keyframes");

    /**
     * Which parts the keyframes pose: all, the upper body only (0.1.11.229: an action in flight, the legs keep the flight pose)
     * or the legs only (0.1.11.226: on foot in a fight between strikes, the arms keep their weapon poses).
     */
    public enum Mask { FULL, UPPER, LEGS }

    /** Poses a model from keyframes. */
    public interface Driver {
        /** Poses the parts of {@code mask} of {@code model} for {@code entity}; false when it has no keyframe animation now. */
        boolean pose(WingmanEntity entity, HumanoidModel<?> model, float partialTick, Mask mask);
    }

    @Nullable private static Driver driver;

    /** Client setup: the GeckoLib driver when GeckoLib is installed. */
    public static void init() {
        if (!ModList.get().isLoaded("geckolib")) {
            LOG.info("GeckoLib not installed: the Wingman keeps its code-driven poses");
            return;
        }
        try {
            driver = com.miophas.singularity_iteration.common.client.compat.geckolib.GeckoWingman.create();
            LOG.info("GeckoLib found: Wingman keyframe animations on");
        } catch (Throwable t) {
            LOG.warn("GeckoLib found, but the Wingman keyframe driver failed to start; code-driven poses stay", t);
            driver = null;
        }
    }

    /** True when a keyframe animation poses {@code entity} now (the renderer then skips its code-driven touch-down dip). */
    public static boolean active(WingmanEntity entity, float partialTick) {
        return driver != null && (entity.ground()
            || WingmanKeyframeChoice.base(entity.landed(), entity.sinceLanding(partialTick), entity.getId(), entity.tickCount) != null);
    }

    /** Poses {@code model} from keyframes; false (nothing changed) without a driver or a keyframe animation. */
    /** 0.1.11.229: the action animation of {@code entity} now ({@link WingmanKeyframeChoice#action}), or null. */
    @Nullable
    public static WingmanKeyframeChoice.Act action(WingmanEntity entity, float partialTick) {
        float shotAge = entity.shotAge(partialTick);
        return WingmanKeyframeChoice.action(
            WingmanPose.holdOf(entity.getMainHandItem(), SlashBladeResharpedBridge.isBlade(entity.getMainHandItem())),
            WingmanEntity.heavyGun(entity.getMainHandItem()), entity.combat(partialTick), shotAge, (int) (entity.tickCount - entity.shotAge(0)),
            entity.shotCooldown(), entity.action(), entity.sinceAction(partialTick), entity.actionAt(), entity.sinceGlare(partialTick),
            entity.glareAt(), entity.coverSide(), entity.onGround());
    }

    /** True when there is a driver and {@code entity} plays an action animation now. */
    public static boolean actionActive(WingmanEntity entity, float partialTick) { return driver != null && action(entity, partialTick) != null; }

    static boolean pose(WingmanEntity entity, HumanoidModel<?> model, float partialTick, Mask mask) {
        Driver d = driver;
        if (d == null) return false;
        try {
            return d.pose(entity, model, partialTick, mask);
        } catch (Throwable t) {
            LOG.warn("The Wingman keyframe driver failed; code-driven poses from now on", t);
            driver = null;
            return false;
        }
    }
}
