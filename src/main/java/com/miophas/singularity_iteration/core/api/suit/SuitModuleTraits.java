// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import java.util.EnumSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> Suit-class traits of a unit (public API since 0.1.8.5): which suit classes take it,
 * how many nano lattice points (LP, 1..6) it needs on a nano piece, and its burst ability.
 * A unit registered without traits is a quantum unit ({@link #QUANTUM_ONLY}), as before 0.1.8.5.
 * <p><b>ZH:</b> 单元的套装类别特性（0.1.8.5 起的公共 API）：哪些套装类别可以安装它、在纳米部件上需要多少纳米晶格点（LP，1..6），以及它的爆发能力。
 * 未带特性注册的单元为量子单元（{@link #QUANTUM_ONLY}），与 0.1.8.5 之前相同。
 * <p><b>JA:</b> ユニットのスーツ種別の特性（0.1.8.5 以降の公開 API）：どのスーツ種別に取り付けられるか、ナノ部位で何 LP（1..6）を
 * 使うか、バースト能力。特性なしで登録したユニットは量子ユニット（{@link #QUANTUM_ONLY}）で、0.1.8.5 より前と同じ。
 *
 * @param latticePoints LP the unit takes on a nano piece (ignored on a quantum piece, which counts units)
 * @param classes       suit classes that take the unit
 * @param burst         burst costs, or null for a unit without a burst
 */
public record SuitModuleTraits(int latticePoints, Set<SuitClass> classes, @Nullable SuitBurstSpec burst) {
    /** EN: Quantum only, no burst. ZH: 仅量子，无爆发。JA: 量子専用、バーストなし。 */
    public static final SuitModuleTraits QUANTUM_ONLY = new SuitModuleTraits(1, EnumSet.of(SuitClass.QUANTUM), null);

    public SuitModuleTraits {
        if (latticePoints < 1 || latticePoints > 6) throw new IllegalArgumentException("lattice points out of 1..6: " + latticePoints);
        if (classes == null || classes.isEmpty()) throw new IllegalArgumentException("a unit needs at least one suit class");
        classes = Set.copyOf(EnumSet.copyOf(classes));
    }

    /** EN: A nano-only unit. ZH: 仅纳米单元。JA: ナノ専用ユニット。 */
    public static SuitModuleTraits nano(int latticePoints, @Nullable SuitBurstSpec burst) {
        return new SuitModuleTraits(latticePoints, EnumSet.of(SuitClass.NANO), burst);
    }

    /** EN: A unit for both classes. ZH: 两种类别通用的单元。JA: 両種別に使えるユニット。 */
    public static SuitModuleTraits shared(int latticePoints) {
        return new SuitModuleTraits(latticePoints, EnumSet.allOf(SuitClass.class), null);
    }

    public boolean allows(SuitClass suitClass) { return classes.contains(suitClass); }
}
