// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> Quantum actions of addon units (public API since 0.1.10.0). Register the unit with
 * {@link SuitModuleRegistry} first (quantum class: registered without traits, or with traits that
 * allow {@link SuitClass#QUANTUM}), then its action here. The action works only while the unit sits
 * on a worn quantum piece; it appears in the action key choices of the Singularity Management Panel.
 * Rules: the id needs the addon namespace; a second registration of the same id replaces the
 * first; register on both sides before the server starts. Units registered before 0.1.10.0 have no
 * action and behave as before.
 * <p><b>ZH:</b> 附属单元的量子行动（0.1.10.0 起的公共 API）。先用 {@link SuitModuleRegistry} 注册单元（量子类别：不带特性注册，
 * 或特性允许 {@link SuitClass#QUANTUM}），再在此注册其行动。行动只在单元位于穿戴中的量子部件上时有效，并出现在奇点管理面板的行动键选项中。
 * 规则：ID 必须带附属命名空间；同一 ID 的第二次注册会替换第一次；请在服务器启动前在两端注册。0.1.10.0 之前注册的单元没有行动，行为不变。
 * <p><b>JA:</b> アドオンユニットの量子アクション（0.1.10.0 以降の公開 API）。先に {@link SuitModuleRegistry} でユニットを登録し
 * （量子種別：特性なしで登録、または {@link SuitClass#QUANTUM} を許す特性で登録）、そのあとここでアクションを登録する。
 * アクションはユニットが着用中の量子部位にあるときだけ動き、奇点管理パネルのアクションキーの選択肢に出る。
 * 規則：ID にはアドオンの名前空間が必要。同じ ID の 2 回目の登録は 1 回目を置き換える。サーバー起動前に両側で登録する。
 * 0.1.10.0 より前に登録したユニットにはアクションがなく、動作は変わらない。
 */
public final class SuitQuantumActions {
    private SuitQuantumActions() {}

    /** EN: A registered action. ZH: 已注册的行动。JA: 登録済みのアクション。 */
    public record Entry(ResourceLocation unit, SuitActionSpec spec, SuitActionBehavior behavior) { }

    private static final Map<String, Entry> ENTRIES = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * <p><b>EN:</b> Registers the action of addon unit {@code unit}. An id without a namespace, or with "minecraft", throws
     * {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 注册附属单元 {@code unit} 的行动。ID 没有命名空间或命名空间为 "minecraft" 时抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> アドオンユニット {@code unit} のアクションを登録する。ID に名前空間がない、または "minecraft" の場合は
     * {@link IllegalArgumentException} を投げる。
     */
    public static void register(ResourceLocation unit, SuitActionSpec spec, SuitActionBehavior behavior) {
        if (unit.getNamespace().isEmpty() || "minecraft".equals(unit.getNamespace()))
            throw new IllegalArgumentException("an action unit id needs the addon namespace: " + unit);
        java.util.Objects.requireNonNull(spec, "spec");
        java.util.Objects.requireNonNull(behavior, "behavior");
        ENTRIES.put(unit.toString(), new Entry(unit, spec, behavior));
    }

    /** EN: Entry by unit key (the full id string), or null. ZH: 按单元键（完整 ID 字符串）获取条目，没有则为 null。JA: ユニットキー（完全な ID 文字列）で項目を得る。なければ null。 */
    @Nullable
    public static Entry get(String key) { return ENTRIES.get(key); }

    /** EN: All registered actions in registration order (a copy). ZH: 按注册顺序的全部行动（副本）。JA: 登録順のすべてのアクション（コピー）。 */
    public static Collection<Entry> all() {
        synchronized (ENTRIES) { return java.util.List.copyOf(ENTRIES.values()); }
    }

    /** EN: True when no action is registered. ZH: 没有注册任何行动时为真。JA: アクションが 1 つも登録されていなければ真。 */
    public static boolean isEmpty() { return ENTRIES.isEmpty(); }
}
