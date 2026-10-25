// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.reactor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> Registry of the reactor layouts of the JEI category "Reactor Layouts" (public API since 0.1.10.19).
 * Register in the mod constructor or in a setup event; JEI reads the registry when it loads its recipes
 * (when a world is joined). A second registration of the same id replaces the first. The ids of
 * Singularity Iteration use the namespace {@code mio_icif}; an addon uses its own namespace.
 * The registry holds data only: the client computes the numbers.
 * <p><b>ZH:</b> JEI 分类“反应堆布局”的布局注册表（0.1.10.19 起的公共 API）。请在模组构造函数或初始化事件中注册；
 * JEI 在加载配方时（进入世界时）读取注册表。同一 ID 的第二次注册会替换第一次。奇点迭代的 ID 使用命名空间 {@code mio_icif}，
 * 附属请使用自己的命名空间。注册表只保存数据，数值由客户端计算。
 * <p><b>JA:</b> JEI カテゴリ「原子炉レイアウト」のレイアウトのレジストリ（0.1.10.19 以降の公開 API）。MOD のコンストラクタか初期化イベントで
 * 登録する。JEI はレシピを読み込むとき（ワールドに入ったとき）にレジストリを読む。同じ ID の 2 回目の登録は 1 回目を置き換える。
 * 奇点迭代の ID は名前空間 {@code mio_icif} を使い、アドオンは自分の名前空間を使う。レジストリはデータだけを持ち、数値はクライアントが計算する。
 */
public final class ReactorLayoutRegistry {
    private ReactorLayoutRegistry() {}

    private static final Map<ResourceLocation, ReactorLayout> LAYOUTS = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * <p><b>EN:</b> Registers a layout. The namespace "minecraft" throws {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 注册布局。命名空间为 "minecraft" 时抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> レイアウトを登録する。名前空間が "minecraft" なら {@link IllegalArgumentException} を投げる。
     */
    public static void register(ReactorLayout layout) {
        if ("minecraft".equals(layout.id().getNamespace()))
            throw new IllegalArgumentException("a reactor layout id needs the mod namespace: " + layout.id());
        LAYOUTS.put(layout.id(), layout);
    }

    /** EN: The layout with this id, or null. ZH: 该 ID 的布局，没有则为 null。JA: この ID のレイアウト。なければ null。 */
    @Nullable
    public static ReactorLayout get(ResourceLocation id) {
        return LAYOUTS.get(id);
    }

    /** EN: All layouts in registration order (a copy). ZH: 按注册顺序的全部布局（副本）。JA: 登録順のすべてのレイアウト（コピー）。 */
    public static Collection<ReactorLayout> all() {
        synchronized (LAYOUTS) {
            return List.copyOf(LAYOUTS.values());
        }
    }
}
