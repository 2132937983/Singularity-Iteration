// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * <p><b>EN:</b> Receives additional outputs at the end of a work cycle (public API since 0.1.11.2). The machine
 * puts each stack into its output slots after the normal output: first on stacks of the same item, then in empty
 * output slots. The part that does not fit is discarded; a bonus output never makes the machine wait. The machine
 * copies the stack, so the caller can keep it.
 * <p><b>ZH:</b> 在工作周期结束时接收追加产物（0.1.11.2 起的公共 API）。机器在正常产物之后把每个物品堆放进输出槽：先叠到相同物品上，
 * 再放进空的输出槽。放不下的部分丢弃；追加产物不会让机器等待。机器复制该物品堆，调用方可以保留原对象。
 * <p><b>JA:</b> 作業サイクルの終わりに追加の産物を受け取る（0.1.11.2 以降の公開 API）。機械は通常の産物の後で各スタックを出力スロットへ
 * 入れる：まず同じアイテムのスタックへ、次に空の出力スロットへ。入りきらない分は捨てる。追加産物で機械が待つことはない。
 * 機械はスタックをコピーするので、呼び出し側はそのまま保持してよい。
 */
@FunctionalInterface
public interface BonusOutputs {
    /** EN: Adds one stack (an empty stack does nothing). ZH: 追加一个物品堆（空堆无效果）。JA: スタックを 1 つ追加する（空なら何もしない）。 */
    void add(ItemStack stack);
}
