// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.reactor;

import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * <p><b>EN:</b> One change of a reactor component, shown by the JEI category "Nuclear Fuel Breeding & Decay" (public API since
 * 0.1.10.37): the component {@code input} becomes {@code output}. {@code amount} is the reactor cycles of a fresh fuel rod
 * ({@link Process#DEPLETION}), the breeding progress at which it changes ({@link Process#BREEDING}), or its decay cycles
 * ({@link Process#DECAY}); 0 = not shown. Items are named by id, so an entry can be made before the item registry is complete;
 * an entry whose input or output item does not exist is not shown. {@code notes} are lines under the entry.
 * <p><b>ZH:</b> 反应堆元件的一次转变，由 JEI 分类“核燃料增殖与衰变”显示（0.1.10.37 起的公共 API）：元件 {@code input} 变为
 * {@code output}。{@code amount} 为新燃料棒的反应堆周期数（{@link Process#DEPLETION}）、发生转变时的增殖进度
 * （{@link Process#BREEDING}）或衰变周期数（{@link Process#DECAY}）；0 表示不显示。物品以 ID 指定，因此可在物品注册表完成前创建条目；
 * 输入或输出物品不存在的条目不显示。{@code notes} 是条目下方的说明行。
 * <p><b>JA:</b> 原子炉部品の 1 回の変化。JEI カテゴリ「核燃料の増殖と崩壊」が表示する（0.1.10.37 以降の公開 API）。部品 {@code input} が
 * {@code output} になる。{@code amount} は新しい燃料棒の原子炉サイクル数（{@link Process#DEPLETION}）、変化する増殖の進行
 * （{@link Process#BREEDING}）、崩壊サイクル数（{@link Process#DECAY}）で、0 は表示しない。アイテムは ID で指定するので、アイテムの
 * レジストリが完成する前に作れる。入力か出力のアイテムがないエントリは表示しない。{@code notes} はエントリの下の説明行。
 */
public record ReactorTransformation(ResourceLocation id, Process process, ResourceLocation input, ResourceLocation output,
                                    int amount, List<Component> notes) {
    /** EN: How the component changes. ZH: 元件转变的方式。JA: 部品が変わる仕方。 */
    public enum Process {
        /** EN: a fuel rod burns out. ZH: 燃料棒燃尽。JA: 燃料棒が燃え尽きる。 */
        DEPLETION,
        /** EN: a component absorbs neutrons until its progress is full. ZH: 元件吸收中子直到进度满。JA: 部品が中性子を吸収し、進行が満ちる。 */
        BREEDING,
        /** EN: a component decays over time. ZH: 元件随时间衰变。JA: 部品が時間とともに崩壊する。 */
        DECAY
    }

    public ReactorTransformation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(process, "process");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if (amount < 0) throw new IllegalArgumentException("negative amount for reactor transformation " + id);
        notes = List.copyOf(notes);
    }

    /** EN: An entry with the notes as varargs. ZH: 以可变参数给出说明行的条目。JA: 説明行を可変長引数で渡すエントリ。 */
    public static ReactorTransformation of(ResourceLocation id, Process process, ResourceLocation input, ResourceLocation output,
                                           int amount, Component... notes) {
        return new ReactorTransformation(id, process, input, output, amount, List.of(notes));
    }
}
