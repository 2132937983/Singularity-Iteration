// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.upgrade.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> The work cycle that an {@link IWorkCycleUpgrade} sees (public API since 0.1.11.2). Only the server
 * makes it.
 * <ul>
 *   <li>{@code recipeId}: the id of the recipe of this cycle, or null when the machine has no recipe concept or
 *       does not report it. At the end of a cycle it is the id found at the start of the cycle (after a world reload in
 *       the middle of a cycle, or when the upgrade came in during the cycle: the id that the machine reports at the end,
 *       which can be null).</li>
 *   <li>{@code baseTicks}: the length of the cycle in ticks with the installed upgrades (overclockers and others), before
 *       the factors of {@link CycleDecision}.</li>
 *   <li>{@code baseEnergyPerTick}: the EU per tick with the installed upgrades, before the factors of {@link CycleDecision}.</li>
 *   <li>{@code machine}: the machine. Do not break or replace its block in a callback.</li>
 * </ul>
 * <p><b>ZH:</b> {@link IWorkCycleUpgrade} 看到的工作周期（0.1.11.2 起的公共 API），只在服务端创建。
 * {@code recipeId}：本周期配方的标识，机器没有配方概念或不报告时为空；周期结束时是周期开始时取得的标识（周期中途重新加载世界、或升级在周期中途装入时：
 * 结束时机器报告的标识，可能为空）。{@code baseTicks}：计入已装升级（超频等）、未乘 {@link CycleDecision} 系数的周期刻数。
 * {@code baseEnergyPerTick}：计入已装升级、未乘系数的每刻 EU。{@code machine}：机器本身；回调中不得破坏或替换它的方块。
 * <p><b>JA:</b> {@link IWorkCycleUpgrade} が見る作業サイクル（0.1.11.2 以降の公開 API）。サーバーだけが作る。
 * {@code recipeId}：このサイクルのレシピ ID。レシピの概念がない、または報告しない機械では null。サイクル終了時はサイクル開始時に得た ID
 * （サイクル途中でワールドを再読み込みした場合や、サイクル途中でアップグレードを入れた場合は、終了時に機械が報告する ID で、null のこともある）。{@code baseTicks}：装着済みアップグレード（オーバークロッカー等）
 * を含み、{@link CycleDecision} の係数を掛ける前のサイクル長（tick）。{@code baseEnergyPerTick}：同じく係数前の tick あたり EU。
 * {@code machine}：機械。コールバック内でそのブロックを壊したり置き換えたりしない。
 */
public record WorkCycleContext(Level level, BlockPos pos, @Nullable ResourceLocation recipeId, int baseTicks,
        long baseEnergyPerTick, IUpgradableBlock machine) {
}
