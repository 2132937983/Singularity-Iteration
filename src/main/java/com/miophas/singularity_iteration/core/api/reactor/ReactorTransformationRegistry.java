// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.reactor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> Registry of the component changes of the JEI category "Nuclear Fuel Breeding & Decay" (public API since
 * 0.1.10.37). Singularity Iteration registers its own from the tables its reactor runs on (fuel rods burning out, the lithium
 * cell and the isotope rod breeding, the tritium rod decaying), so the category always shows what the reactor does. An addon
 * registers the changes of its own components here, in the mod constructor or in a setup event; JEI reads the registry when it
 * loads its recipes (when a world is joined). A second registration of the same id replaces the first; the namespace
 * "minecraft" is refused. The registry holds data only: it does not change what a reactor does.
 * <p><b>ZH:</b> JEI 分类“核燃料增殖与衰变”的元件转变注册表（0.1.10.37 起的公共 API）。奇点迭代按其反应堆运行所用的表注册自己的条目
 * （燃料棒燃尽、锂单元与同位素棒增殖、氚燃料棒衰变），因此该分类始终显示反应堆实际的行为。附属请在模组构造函数或初始化事件中注册其元件的转变；
 * JEI 在加载配方时（进入世界时）读取注册表。同一 ID 的第二次注册会替换第一次；拒绝命名空间 "minecraft"。注册表只保存数据，不改变反应堆的行为。
 * <p><b>JA:</b> JEI カテゴリ「核燃料の増殖と崩壊」の部品の変化のレジストリ（0.1.10.37 以降の公開 API）。奇点迭代は自分の原子炉が使う表から
 * 自分のエントリを登録する（燃料棒の燃え尽き、リチウムセルと同位体棒の増殖、トリチウム燃料棒の崩壊）。そのためカテゴリは常に原子炉の実際の動作を示す。
 * アドオンは自分の部品の変化を MOD のコンストラクタか初期化イベントで登録する。JEI はレシピを読み込むとき（ワールドに入ったとき）にレジストリを読む。
 * 同じ ID の 2 回目の登録は 1 回目を置き換え、名前空間 "minecraft" は拒否する。レジストリはデータだけを持ち、原子炉の動作は変えない。
 */
public final class ReactorTransformationRegistry {
    private ReactorTransformationRegistry() {}

    private static final Map<ResourceLocation, ReactorTransformation> ENTRIES = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * <p><b>EN:</b> Registers a change. The namespace "minecraft" throws {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 注册一次转变。命名空间为 "minecraft" 时抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> 変化を登録する。名前空間が "minecraft" なら {@link IllegalArgumentException} を投げる。
     */
    public static void register(ReactorTransformation transformation) {
        if ("minecraft".equals(transformation.id().getNamespace()))
            throw new IllegalArgumentException("a reactor transformation id needs the mod namespace: " + transformation.id());
        ENTRIES.put(transformation.id(), transformation);
    }

    /** EN: The change with this id, or null. ZH: 该 ID 的转变，没有则为 null。JA: この ID の変化。なければ null。 */
    @Nullable
    public static ReactorTransformation get(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    /** EN: All changes in registration order (a copy). ZH: 按注册顺序的全部转变（副本）。JA: 登録順のすべての変化（コピー）。 */
    public static Collection<ReactorTransformation> all() {
        synchronized (ENTRIES) {
            return List.copyOf(ENTRIES.values());
        }
    }

    /**
     * <p><b>EN:</b> The changes whose input and output items exist (what JEI shows), in registration order. Call it after the
     * item registry is complete.
     * <p><b>ZH:</b> 输入与输出物品都存在的转变（JEI 显示的内容），按注册顺序。请在物品注册表完成后调用。
     * <p><b>JA:</b> 入力と出力のアイテムがある変化（JEI が表示するもの）、登録順。アイテムのレジストリが完成した後に呼ぶ。
     */
    public static List<ReactorTransformation> available() {
        return all().stream()
            .filter(t -> BuiltInRegistries.ITEM.containsKey(t.input()) && BuiltInRegistries.ITEM.containsKey(t.output()))
            .toList();
    }
}
