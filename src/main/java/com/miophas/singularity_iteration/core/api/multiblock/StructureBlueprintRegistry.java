// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.multiblock;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> Registry of the multiblock blueprints (public API since 0.1.10.19): the JEI category
 * "Multiblock Structures" shows each entry, and the guide tag {@code <SiStructure id="..."/>} finds them here.
 * Register in the mod constructor or in a setup event (on both sides; a dedicated server only stores them).
 * A second registration of the same id replaces the first. The ids of Singularity Iteration use the namespace
 * {@code mio_icif}; an addon uses its own namespace.
 * <p><b>ZH:</b> 多方块建造图的注册表（0.1.10.19 起的公共 API）：JEI 分类“多方块结构”显示每个条目，手册标签
 * {@code <SiStructure id="..."/>} 从这里查找建造图。请在模组构造函数或初始化事件中注册（两端均可；专用服务器只保存它们）。
 * 同一 ID 的第二次注册会替换第一次。奇点迭代的 ID 使用命名空间 {@code mio_icif}，附属请使用自己的命名空间。
 * <p><b>JA:</b> マルチブロック建造図のレジストリ（0.1.10.19 以降の公開 API）：JEI カテゴリ「マルチブロック構造」が各項目を表示し、
 * ガイドのタグ {@code <SiStructure id="..."/>} はここから建造図を探す。MOD のコンストラクタか初期化イベントで登録する（両側で可。
 * 専用サーバーは保存するだけ）。同じ ID の 2 回目の登録は 1 回目を置き換える。奇点迭代の ID は名前空間 {@code mio_icif} を使い、
 * アドオンは自分の名前空間を使う。
 */
public final class StructureBlueprintRegistry {
    private StructureBlueprintRegistry() {}

    private static final Map<ResourceLocation, StructureBlueprint> BLUEPRINTS = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * <p><b>EN:</b> Registers a blueprint. The namespace "minecraft" throws {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 注册建造图。命名空间为 "minecraft" 时抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> 建造図を登録する。名前空間が "minecraft" なら {@link IllegalArgumentException} を投げる。
     */
    public static void register(StructureBlueprint blueprint) {
        if ("minecraft".equals(blueprint.id().getNamespace()))
            throw new IllegalArgumentException("a blueprint id needs the mod namespace: " + blueprint.id());
        BLUEPRINTS.put(blueprint.id(), blueprint);
    }

    /** EN: The blueprint with this id, or null. ZH: 该 ID 的建造图，没有则为 null。JA: この ID の建造図。なければ null。 */
    @Nullable
    public static StructureBlueprint get(ResourceLocation id) {
        return BLUEPRINTS.get(id);
    }

    /** EN: All blueprints in registration order (a copy). ZH: 按注册顺序的全部建造图（副本）。JA: 登録順のすべての建造図（コピー）。 */
    public static Collection<StructureBlueprint> all() {
        synchronized (BLUEPRINTS) {
            return List.copyOf(BLUEPRINTS.values());
        }
    }
}
