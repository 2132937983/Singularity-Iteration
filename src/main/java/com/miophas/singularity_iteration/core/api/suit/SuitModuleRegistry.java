// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> Registry of addon quantum suit units (public API since 0.1.8.0).
 * Procedure: (1) make an item that implements {@link ISuitModuleItem}; (2) in the mod constructor
 * or a setup event, call {@link #register} with the spec and the behavior; (3) add the translation
 * keys {@link SuitModuleSpec#nameKey()} and {@code nameKey + ".desc"}.
 * Rules: the id needs the addon namespace (ids without a namespace are built-in units); a second
 * registration of the same id replaces the first; register on both sides before the server starts
 * (the client uses the spec for names, colours and the panel); a piece keeps the id of a unit whose
 * mod was removed, and that unit does nothing and does not count against the capacity.
 * <p><b>ZH:</b> 附属量子套装单元的注册表（0.1.8.0 起的公共 API）。
 * 步骤：(1) 编写实现 {@link ISuitModuleItem} 的物品；(2) 在模组构造函数或初始化事件中，用 spec 与行为调用 {@link #register}；
 * (3) 添加翻译键 {@link SuitModuleSpec#nameKey()} 与 {@code nameKey + ".desc"}。
 * 规则：ID 必须带附属命名空间（无命名空间的 ID 属于内置单元）；同一 ID 的第二次注册会替换第一次；请在服务器启动前在两端注册
 * （客户端用 spec 显示名称、颜色与面板）；模组被移除后部件仍保留单元 ID，该单元不起作用，也不占用容量。
 * <p><b>JA:</b> アドオンの量子スーツユニットのレジストリ（0.1.8.0 以降の公開 API）。
 * 手順：(1) {@link ISuitModuleItem} を実装するアイテムを作る。(2) MOD のコンストラクタか初期化イベントで、spec と動作を渡して
 * {@link #register} を呼ぶ。(3) 翻訳キー {@link SuitModuleSpec#nameKey()} と {@code nameKey + ".desc"} を追加する。
 * 規則：ID にはアドオンの名前空間が必要（名前空間なしは組み込みユニット）。同じ ID の 2 回目の登録は 1 回目を置き換える。
 * サーバー起動前に両側で登録する（クライアントは名前・色・パネルに spec を使う）。MOD が外されても部位はユニット ID を保持するが、
 * そのユニットは何もせず、容量にも数えない。
 */
public final class SuitModuleRegistry {
    private SuitModuleRegistry() {}

    /**
     * EN: A registered unit. {@code traits} and {@code burst} since 0.1.8.5 (quantum only and null for a unit registered without them).
     * ZH: 已注册的单元。{@code traits} 与 {@code burst} 自 0.1.8.5 起（未提供时为仅量子与 null）。
     * JA: 登録済みのユニット。{@code traits} と {@code burst} は 0.1.8.5 から（指定なしなら量子専用と null）。
     */
    public record Entry(SuitModuleSpec spec, SuitModuleBehavior behavior, SuitModuleTraits traits, @Nullable SuitBurstBehavior burst) {
        public Entry(SuitModuleSpec spec, SuitModuleBehavior behavior) { this(spec, behavior, SuitModuleTraits.QUANTUM_ONLY, null); }
    }

    /** EN: Implementation hook of Singularity Iteration (not for addons). ZH: 奇点迭代的实现挂钩（附属请勿使用）。JA: 奇点迭代の実装フック（アドオンは使わない）。 */
    public interface Query {
        boolean active(Player player, SuitModuleSpec spec);
    }

    private static final Map<String, Entry> ENTRIES = Collections.synchronizedMap(new LinkedHashMap<>());
    private static volatile Query query = (player, spec) -> false;

    /**
     * <p><b>EN:</b> Registers a unit. An id without a namespace, or with "minecraft", throws {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 注册单元。ID 没有命名空间或命名空间为 "minecraft" 时抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> ユニットを登録する。ID に名前空間がない、または "minecraft" の場合は {@link IllegalArgumentException} を投げる。
     */
    public static void register(SuitModuleSpec spec, SuitModuleBehavior behavior) {
        register(spec, behavior, SuitModuleTraits.QUANTUM_ONLY, null);
    }

    /**
     * <p><b>EN:</b> Registers a unit with suit-class traits (0.1.8.5): a nano or shared unit, with an optional burst.
     * A burst unit needs {@code traits.burst()} and {@code burst} together, else {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 带套装类别特性注册单元（0.1.8.5）：纳米或通用单元，可带爆发。爆发单元必须同时提供 {@code traits.burst()} 与 {@code burst}，否则抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> スーツ種別の特性付きでユニットを登録する（0.1.8.5）：ナノまたは共用ユニット、任意でバースト付き。
     * バーストユニットには {@code traits.burst()} と {@code burst} の両方が必要で、片方だけなら {@link IllegalArgumentException}。
     */
    public static void register(SuitModuleSpec spec, SuitModuleBehavior behavior, SuitModuleTraits traits, @Nullable SuitBurstBehavior burst) {
        if (spec.id().getNamespace().isEmpty() || "minecraft".equals(spec.id().getNamespace()))
            throw new IllegalArgumentException("a unit id needs the addon namespace: " + spec.id());
        java.util.Objects.requireNonNull(traits, "traits");
        if ((traits.burst() == null) != (burst == null))
            throw new IllegalArgumentException("a burst unit needs both the burst spec and the burst behavior: " + spec.id());
        ENTRIES.put(spec.key(), new Entry(spec, behavior == null ? SuitModuleBehavior.NONE : behavior, traits, burst));
    }

    /** EN: Entry by key (the full id string), or null. ZH: 按键（完整 ID 字符串）获取条目，没有则为 null。JA: キー（完全な ID 文字列）で項目を得る。なければ null。 */
    @Nullable
    public static Entry get(String key) { return ENTRIES.get(key); }

    /** EN: Entry by id, or null. ZH: 按 ID 获取条目，没有则为 null。JA: ID で項目を得る。なければ null。 */
    @Nullable
    public static Entry get(ResourceLocation id) { return ENTRIES.get(id.toString()); }

    /** EN: All registered units in registration order (a copy). ZH: 按注册顺序的全部单元（副本）。JA: 登録順のすべてのユニット（コピー）。 */
    public static Collection<Entry> all() {
        synchronized (ENTRIES) { return java.util.List.copyOf(ENTRIES.values()); }
    }

    /** EN: True when no unit is registered. ZH: 没有注册任何单元时为真。JA: ユニットが 1 つも登録されていなければ真。 */
    public static boolean isEmpty() { return ENTRIES.isEmpty(); }

    /**
     * <p><b>EN:</b> True when the unit is active on {@code player}: installed, switched on, powered, and a visor worn when needed.
     * <p><b>ZH:</b> 单元在 {@code player} 身上生效时为真：已安装、已开启、有能量，并在需要时戴着面罩。
     * <p><b>JA:</b> ユニットが {@code player} で有効なら真：取り付け済み、オン、電力あり、必要ならバイザー着用。
     */
    public static boolean isActive(Player player, ResourceLocation id) {
        Entry e = get(id);
        return e != null && query.active(player, e.spec());
    }

    /** EN: Called once by Singularity Iteration. ZH: 由奇点迭代调用一次。JA: 奇点迭代が 1 回だけ呼ぶ。 */
    public static void setQuery(Query implementation) { query = implementation; }
}
