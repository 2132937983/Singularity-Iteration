// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanKeyframeChoice;
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

    /** Poses a model from keyframes. */
    public interface Driver {
        /**
         * Poses the six parts of {@code model} for {@code entity}; false when it has no keyframe animation now. Without
         * {@code upper} only the legs are posed (0.1.11.226: on foot in a fight the arms keep their weapon poses).
         */
        boolean pose(WingmanEntity entity, HumanoidModel<?> model, float partialTick, boolean upper);
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
    static boolean pose(WingmanEntity entity, HumanoidModel<?> model, float partialTick, boolean upper) {
        Driver d = driver;
        if (d == null) return false;
        try {
            return d.pose(entity, model, partialTick, upper);
        } catch (Throwable t) {
            LOG.warn("The Wingman keyframe driver failed; code-driven poses from now on", t);
            driver = null;
            return false;
        }
    }
}
