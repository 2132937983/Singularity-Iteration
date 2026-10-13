// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

/**
 * <p><b>EN:</b> Suit classes that take upgrade units (0.1.8.5). A quantum piece counts its units
 * (head 4, chest 3, legs 2, feet 2). A nano piece has a nano lattice and counts lattice points (LP).
 * <p><b>ZH:</b> 可安装升级单元的套装类别（0.1.8.5）。量子部件按个数计（头 4、胸 3、腿 2、脚 2）。纳米部件有纳米晶格，按晶格点数（LP）计。
 * <p><b>JA:</b> 強化ユニットを取り付けられるスーツの種別（0.1.8.5）。量子部位は個数で数える（頭 4・胴 3・脚 2・足 2）。
 * ナノ部位はナノ格子を持ち、格子ポイント（LP）で数える。
 */
public enum SuitClass {
    QUANTUM,
    NANO
}
