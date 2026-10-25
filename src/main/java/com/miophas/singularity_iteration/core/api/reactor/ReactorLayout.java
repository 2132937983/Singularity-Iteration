// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.reactor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> A nuclear reactor design shown in the JEI category "Reactor Layouts" (public API since 0.1.10.19).
 * Singularity Iteration runs each layout through the model of the reactor block. It shows the EU/t,
 * the heat balance, the risk (stable, components melting, meltdown) and the explosion size.
 * {@code rows} are the reactor rows from the top (1 to 6 rows); each row is a list of cells separated by
 * commas. A cell is a component key of Singularity Iteration ({@code "uranium_quad"}, {@code "iridium_vent"}),
 * the item id of a reactor component ({@code "mio_icif:reactor/item_reactor_vent"}), or {@code "_"} for an
 * empty slot. The cells of the first row set the columns (3 to 9 = 3 + reactor chambers); a longer row is cut.
 * {@code titleKey} and {@code noteKey} are translation keys (null: the default key {@code reactor_layout.<namespace>.<path>}
 * for the title, no note).
 * <p><b>ZH:</b> 在 JEI 分类“反应堆布局”中显示的核反应堆设计（0.1.10.19 起的公共 API）。奇点迭代用反应堆方块的模型运行每个布局，
 * 并显示 EU/t、热量平衡、风险（稳定、组件熔化、熔毁）与爆炸规模。{@code rows} 是自上而下的反应堆行（1 到 6 行），每行用逗号分隔单元格。
 * 单元格可以是奇点迭代的组件键（{@code "uranium_quad"}、{@code "iridium_vent"}）、反应堆组件的物品 ID
 * （{@code "mio_icif:reactor/item_reactor_vent"}）或表示空槽的 {@code "_"}。第一行的单元格数决定列数（3 到 9 = 3 + 反应仓数），
 * 更长的行会被截断。{@code titleKey} 与 {@code noteKey} 是翻译键（为 null 时：标题使用默认键 {@code reactor_layout.<命名空间>.<路径>}，没有说明）。
 * <p><b>JA:</b> JEI カテゴリ「原子炉レイアウト」に表示する原子炉の設計（0.1.10.19 以降の公開 API）。奇点迭代は各レイアウトを原子炉ブロックと
 * 同じモデルで動かし、EU/t、熱収支、リスク（安定・部品の溶融・メルトダウン）と爆発の規模を表示する。{@code rows} は上からの原子炉の行
 * （1〜6 行）で、各行はセルをカンマで区切る。セルは奇点迭代の部品キー（{@code "uranium_quad"}、{@code "iridium_vent"}）、原子炉部品の
 * アイテム ID（{@code "mio_icif:reactor/item_reactor_vent"}）、または空きスロットを表す {@code "_"}。1 行目のセル数が列数
 * （3〜9 = 3 + 原子炉チャンバー数）になり、それより長い行は切り詰める。{@code titleKey} と {@code noteKey} は翻訳キー
 * （null なら、タイトルは既定キー {@code reactor_layout.<名前空間>.<パス>}、説明なし）。
 *
 * <pre>{@code
 * ReactorLayoutRegistry.register(ReactorLayout.of(ResourceLocation.fromNamespaceAndPath("mymod", "starter"),
 *     "_,vent,_/vent,uranium_simple,vent/_,vent,_"));
 * }</pre>
 */
public record ReactorLayout(ResourceLocation id, List<String> rows, @Nullable String titleKey, @Nullable String noteKey) {
    /** EN: Most rows of a reactor. ZH: 反应堆的最大行数。JA: 原子炉の最大行数。 */
    public static final int MAX_ROWS = 6;
    /** EN: Fewest and most columns (3 + 0 to 6 chambers). ZH: 最少与最多列数（3 + 0 到 6 个反应仓）。JA: 最小・最大の列数（3 + チャンバー 0〜6）。 */
    public static final int MIN_COLUMNS = 3, MAX_COLUMNS = 9;
    /** EN: The cell of an empty slot. ZH: 空槽的单元格。JA: 空きスロットのセル。 */
    public static final String EMPTY = "_";

    /**
     * <p><b>EN:</b> Checks the layout: an id, 1 to 6 rows, and 3 to 9 cells in the first row. Else {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 检查布局：需要 ID、1 到 6 行、第一行 3 到 9 个单元格，否则抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> レイアウトを検査する：ID、1〜6 行、1 行目に 3〜9 セルが必要。満たさなければ {@link IllegalArgumentException}。
     */
    public ReactorLayout {
        Objects.requireNonNull(id, "id");
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        if (rows.isEmpty() || rows.size() > MAX_ROWS) throw new IllegalArgumentException("a reactor layout has 1 to 6 rows: " + id);
        int first = cells(rows.get(0)).size();
        if (first < MIN_COLUMNS || first > MAX_COLUMNS)
            throw new IllegalArgumentException("the first row of a reactor layout has 3 to 9 cells: " + id);
    }

    /**
     * <p><b>EN:</b> A layout from the preset form "row/row/row" (the form of the guide's reactor simulator), with the default title.
     * <p><b>ZH:</b> 用预设形式 "行/行/行"（手册反应堆模拟器的形式）创建布局，使用默认标题。
     * <p><b>JA:</b> プリセット形式「行/行/行」（ガイドの原子炉シミュレーターと同じ形式）からレイアウトを作る。タイトルは既定。
     */
    public static ReactorLayout of(ResourceLocation id, String preset) {
        return of(id, preset, null, null);
    }

    /**
     * <p><b>EN:</b> A layout from the preset form with a title key and a note key (each may be null).
     * <p><b>ZH:</b> 用预设形式创建布局，并指定标题键与说明键（均可为 null）。
     * <p><b>JA:</b> プリセット形式からタイトルキーと説明キー付きでレイアウトを作る（どちらも null 可）。
     */
    public static ReactorLayout of(ResourceLocation id, String preset, @Nullable String titleKey, @Nullable String noteKey) {
        Objects.requireNonNull(preset, "preset");
        return new ReactorLayout(id, Arrays.asList(preset.trim().split("/")), titleKey, noteKey);
    }

    /** EN: The columns (3 to 9). ZH: 列数（3 到 9）。JA: 列数（3〜9）。 */
    public int columns() {
        return cells(rows.get(0)).size();
    }

    /** EN: The reactor chambers that the layout needs (0 to 6). ZH: 布局需要的反应仓数（0 到 6）。JA: レイアウトに必要な原子炉チャンバー数（0〜6）。 */
    public int chambers() {
        return columns() - MIN_COLUMNS;
    }

    /**
     * <p><b>EN:</b> The cell at {@code row}, {@code column}, trimmed; {@link #EMPTY} outside the layout or for a blank cell.
     * <p><b>ZH:</b> {@code row} 行 {@code column} 列的单元格（已去除空白）；在布局之外或为空白时返回 {@link #EMPTY}。
     * <p><b>JA:</b> {@code row} 行 {@code column} 列のセル（前後の空白を除く）。レイアウト外や空白なら {@link #EMPTY}。
     */
    public String cell(int row, int column) {
        if (row < 0 || row >= rows.size() || column < 0 || column >= columns()) return EMPTY;
        List<String> cells = cells(rows.get(row));
        if (column >= cells.size()) return EMPTY;
        String cell = cells.get(column).trim();
        return cell.isEmpty() ? EMPTY : cell;
    }

    /** EN: The layout in the preset form "row/row". ZH: 预设形式 "行/行" 的布局。JA: プリセット形式「行/行」のレイアウト。 */
    public String preset() {
        return String.join("/", rows);
    }

    /** EN: The title key: {@code titleKey}, or {@code reactor_layout.<namespace>.<path>}. ZH: 标题键：{@code titleKey} 或 {@code reactor_layout.<命名空间>.<路径>}。JA: タイトルキー：{@code titleKey}、なければ {@code reactor_layout.<名前空間>.<パス>}。 */
    public String effectiveTitleKey() {
        return titleKey != null ? titleKey : "reactor_layout." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    private static List<String> cells(String row) {
        return Arrays.asList(row.split(",", -1));
    }
}
