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
 * <p><b>EN:</b> Entanglement synergies of addon units (public API since 0.1.10.1). The wearer links
 * two installed units in the Singularity Management Panel; a link counts against the link capacity
 * of the quantum chestplate (2, 3 with a compute core). Register a synergy here with its two unit
 * keys: a built-in id such as {@code "entity_esp"} or an addon id such as {@code "addon:unit"}. Then
 * ask {@link #active} where the effect applies. Singularity Iteration draws the link and shows the
 * name; the addon implements the effect. Rules: the id needs the addon namespace; a second
 * registration of the same id replaces the first; register on both sides before the server starts.
 * Translation keys: {@link Entry#nameKey()} and {@code nameKey + ".desc"}.
 * <p><b>ZH:</b> 附属单元的纠缠协同（0.1.10.1 起的公共 API）。穿戴者在奇点管理面板中连接两个已安装的单元；每条连接占用量子胸甲的连接容量
 * （2，装有演算核心时为 3）。在此用两个单元键（内置 ID，如 {@code "entity_esp"}，或附属 ID，如 {@code "addon:unit"}）注册协同，
 * 然后在效果生效处查询 {@link #active}。奇点迭代绘制连接并显示名称；效果由附属实现。规则：ID 必须带附属命名空间；同一 ID
 * 的第二次注册会替换第一次；请在服务器启动前在两端注册。翻译键：{@link Entry#nameKey()} 与 {@code nameKey + ".desc"}。
 * <p><b>JA:</b> アドオンユニットのもつれシナジー（0.1.10.1 以降の公開 API）。着用者は奇点管理パネルで取り付け済みの 2 ユニットを
 * 結ぶ。リンクは量子チェストプレートのリンク容量（2、演算コアがあれば 3）を使う。ここに 2 つのユニットキー（組み込み ID の
 * {@code "entity_esp"} やアドオン ID の {@code "addon:unit"}）でシナジーを登録し、効果を出す場所で {@link #active} を問い合わせる。
 * 奇点迭代はリンクを描き名前を表示する。効果はアドオンが実装する。規則：ID にはアドオンの名前空間が必要。同じ ID の 2 回目の
 * 登録は 1 回目を置き換える。サーバー起動前に両側で登録する。翻訳キー：{@link Entry#nameKey()} と {@code nameKey + ".desc"}。
 */
public final class SuitSynergies {
    private SuitSynergies() {}

    /**
     * EN: A registered synergy. ZH: 已注册的协同。JA: 登録済みのシナジー。
     *
     * @param id    the synergy id
     * @param unitA the key of the first unit
     * @param unitB the key of the second unit
     */
    public record Entry(ResourceLocation id, String unitA, String unitB) {
        /** EN: "synergy.namespace.path". ZH: "synergy.命名空间.路径"。JA: "synergy.名前空間.パス"。 */
        public String nameKey() { return "synergy." + id.getNamespace() + "." + id.getPath().replace('/', '.'); }
    }

    /** EN: Implementation hook of Singularity Iteration (not for addons). ZH: 奇点迭代的实现挂钩（附属请勿使用）。JA: 奇点迭代の実装フック（アドオンは使わない）。 */
    public interface Query {
        boolean active(Player player, String key);
    }

    private static final Map<String, Entry> ENTRIES = Collections.synchronizedMap(new LinkedHashMap<>());
    private static volatile Query query = (player, key) -> false;

    /**
     * <p><b>EN:</b> Registers a synergy of two units. An id without a namespace or with "minecraft", or two equal unit keys,
     * throws {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 注册两个单元的协同。ID 没有命名空间或命名空间为 "minecraft"，或两个单元键相同时，抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> 2 ユニットのシナジーを登録する。ID に名前空間がない、"minecraft"、または 2 つのユニットキーが同じ場合は
     * {@link IllegalArgumentException} を投げる。
     */
    public static void register(ResourceLocation id, String unitA, String unitB) {
        if (id.getNamespace().isEmpty() || "minecraft".equals(id.getNamespace()))
            throw new IllegalArgumentException("a synergy id needs the addon namespace: " + id);
        if (unitA == null || unitB == null || unitA.isEmpty() || unitA.equals(unitB))
            throw new IllegalArgumentException("a synergy needs two different units: " + unitA + ", " + unitB);
        ENTRIES.put(id.toString(), new Entry(id, unitA, unitB));
    }

    /** EN: Entry by key, or null. ZH: 按键获取条目，没有则为 null。JA: キーで項目を得る。なければ null。 */
    @Nullable
    public static Entry get(String key) { return ENTRIES.get(key); }

    /** EN: All registered synergies in registration order (a copy). ZH: 按注册顺序的全部协同（副本）。JA: 登録順のすべてのシナジー（コピー）。 */
    public static Collection<Entry> all() {
        synchronized (ENTRIES) { return java.util.List.copyOf(ENTRIES.values()); }
    }

    /**
     * <p><b>EN:</b> True when the wearer has linked the synergy and both units are active (installed, on, powered) on worn pieces.
     * <p><b>ZH:</b> 穿戴者已连接该协同，且两个单元都在穿戴部件上生效（已安装、开启、有能量）时为真。
     * <p><b>JA:</b> 着用者がシナジーを結んでいて、2 ユニットとも着用中の部位で有効（取り付け済み、オン、電力あり）なら真。
     */
    public static boolean active(Player player, ResourceLocation id) {
        return query.active(player, id.toString());
    }

    /** EN: Called once by Singularity Iteration. ZH: 由奇点迭代调用一次。JA: 奇点迭代が 1 回だけ呼ぶ。 */
    public static void setQuery(Query implementation) { query = java.util.Objects.requireNonNull(implementation); }
}
