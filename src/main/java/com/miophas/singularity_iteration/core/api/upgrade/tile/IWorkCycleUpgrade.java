// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.upgrade.tile;

import java.util.Collection;
import net.minecraft.world.item.ItemStack;

/**
 * <p><b>EN:</b> An upgrade item that the processing machines call at the start and at the end of each work cycle
 * (public API since 0.1.11.2). Put the item in an upgrade slot of a machine of Singularity Iteration (the prefab
 * {@code AbstractProcessingMachineBlockEntity} and its subclasses).
 * <ul>
 *   <li>{@link #onCycleStart}: before the progress of a cycle starts from 0. When one upgrade returns
 *       {@link CycleDecision#hold()}, the machine does not start in this tick. The factors of all upgrades are
 *       multiplied, the product is clamped to 0.25..4 and then applies on top of the other upgrades, for this cycle
 *       only.</li>
 *   <li>{@link #onCycleComplete}: after each operation that made an output (an overclocked machine can make more
 *       than one output in one cycle; then it calls this method once for each output).</li>
 * </ul>
 * The calls occur on the server only, in the order of the upgrade slots, one time for each stack (not for each item
 * of the stack). The machine also posts {@code MachineWorkStartEvent} (cancel = hold) and
 * {@code MachineWorkCompleteEvent} at the same two points. The duration factor does not apply to a machine that sets
 * the length of its cycle again during the cycle; the energy factor does not apply to a machine that calculates its
 * EU per tick without the prefab method.
 * <p>This interface gives defaults for {@link #onTick} (false) and {@link #onProcessEnd} (no change); the machines do
 * not call them.
 * <p><b>ZH:</b> 加工机器在每个工作周期开始与结束时回调的升级物品（0.1.11.2 起的公共 API），放入奇点迭代机器
 * （预制基类 {@code AbstractProcessingMachineBlockEntity} 及其子类）的升级槽即可。{@link #onCycleStart}：周期进度从 0 开始之前调用；
 * 任一升级返回 {@link CycleDecision#hold()} 时本刻不开工；各升级的系数相乘，乘积钳制到 0.25..4，再叠加在其他升级之上，只作用于本周期。
 * {@link #onCycleComplete}：每次完成一次产出之后调用（超频机器一个周期可以有多次产出，每次各调用一次）。回调只在服务端，按升级槽顺序，
 * 每个物品堆调用一次（不按数量）。机器在同样两个时点投递 {@code MachineWorkStartEvent}（取消 = hold）与 {@code MachineWorkCompleteEvent}。
 * 周期中途重新设定周期长度的机器不受耗时系数影响；不经预制方法计算每刻 EU 的机器不受耗电系数影响。本接口为 {@link #onTick}（假）与
 * {@link #onProcessEnd}（不改动）提供默认实现，机器不调用它们。
 * <p><b>JA:</b> 加工機械が各作業サイクルの開始時と終了時に呼ぶアップグレードアイテム（0.1.11.2 以降の公開 API）。奇点迭代の機械
 * （プレハブ {@code AbstractProcessingMachineBlockEntity} とそのサブクラス）のアップグレードスロットに入れる。{@link #onCycleStart}：
 * サイクルの進捗が 0 から始まる前。1 つでも {@link CycleDecision#hold()} を返すと、この tick では開始しない。全アップグレードの係数を掛け合わせ、
 * 積を 0.25..4 に制限し、他のアップグレードの上に重ねる（このサイクルだけ）。{@link #onCycleComplete}：産物を作った各操作の後
 * （オーバークロックした機械は 1 サイクルで複数回作ることがあり、そのたびに呼ぶ）。呼び出しはサーバーだけで、スロット順に、スタックごとに
 * 1 回（個数ごとではない）。機械は同じ 2 時点で {@code MachineWorkStartEvent}（キャンセル = hold）と {@code MachineWorkCompleteEvent}
 * も投げる。サイクル中に自分でサイクル長を設定し直す機械には所要時間係数が効かず、プレハブのメソッドを使わずに tick あたり EU を計算する機械には
 * 電力係数が効かない。このインターフェースは {@link #onTick}（false）と {@link #onProcessEnd}（変更なし）の既定実装を持ち、機械はそれらを呼ばない。
 */
public interface IWorkCycleUpgrade extends IUpgradeItem {

    /**
     * EN: Called before a cycle starts. Return {@link CycleDecision#proceed()} for no change.
     * ZH: 周期开始前调用。返回 {@link CycleDecision#proceed()} 表示不改动。
     * JA: サイクル開始前に呼ばれる。変更しないなら {@link CycleDecision#proceed()} を返す。
     *
     * @param upgrade the upgrade stack in the slot (do not change it)
     * @param ctx the cycle
     * @return the decision; null is the same as proceed()
     */
    default CycleDecision onCycleStart(ItemStack upgrade, WorkCycleContext ctx) {
        return CycleDecision.proceed();
    }

    /**
     * EN: Called after an operation made its output.
     * ZH: 一次操作完成产出之后调用。
     * JA: 操作が産物を作った後に呼ばれる。
     *
     * @param upgrade the upgrade stack in the slot (do not change it)
     * @param ctx the cycle
     * @param out the sink for additional outputs
     */
    default void onCycleComplete(ItemStack upgrade, WorkCycleContext ctx, BonusOutputs out) {
    }

    /** EN: Not called by the machines; default false. ZH: 机器不调用；默认假。JA: 機械は呼ばない。既定は false。 */
    @Override
    default boolean onTick(ItemStack stack, IUpgradableBlock block) {
        return false;
    }

    /** EN: Not called by the machines; default: the outputs without a change. ZH: 机器不调用；默认原样返回。JA: 機械は呼ばない。既定は変更なし。 */
    @Override
    @Deprecated
    default Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock block, Collection<ItemStack> outputs) {
        return outputs;
    }
}
