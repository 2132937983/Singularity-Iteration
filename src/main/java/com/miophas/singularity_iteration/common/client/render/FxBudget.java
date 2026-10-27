// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 0.1.10.32 (P-3): the level of detail of the effects ({@link FxLodRules}) for the renderers, and the budget of whole-body
 * passes of a frame: a renderer asks {@link #allow} before a whole-body pass; the nearest {@code fx_budget} askers of the last
 * frame may draw. Options {@code fx_lod} and {@code fx_budget}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class FxBudget {
    private FxBudget() {}

    private static final FxLodRules.Budget BUDGET = new FxLodRules.Budget(FxLodRules.DEFAULT_BUDGET);

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        BUDGET.limit(FcsClientConfig.fxBudget());
        BUDGET.frame();
    }

    private static Vec3 camera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    /** Squared distance of {@code at} to the camera. */
    public static double distSq(Vec3 at) {
        return at.distanceToSqr(camera());
    }

    /** Detail tier of an effect at {@code at} ({@link FxLodRules#NEAR}, MID, FAR). */
    public static int tier(Vec3 at) {
        return FxLodRules.tier(distSq(at), FcsClientConfig.fxLod());
    }

    public static int tier(Entity e) {
        return tier(e.position());
    }

    /** Asks for a whole-body pass of {@code e} this frame. */
    public static boolean allow(Entity e) {
        return allow(e.getId(), e.position());
    }

    /** Asks for a whole-body pass of the effect {@code id} at {@code at} (block entities use a negative id). */
    public static boolean allow(int id, Vec3 at) {
        BUDGET.request(id, distSq(at));
        return BUDGET.allowed(id);
    }
}
