// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * <p><b>EN:</b> Description of a quantum suit upgrade unit of an addon (public API since 0.1.8.0).
 * The quantum modification station installs the unit into a quantum suit piece that has a slot in
 * {@link #slots}. The Singularity Management Panel shows a switch for it. While the switch is on,
 * the unit costs {@link #drainPerTick} EU/t (collected once a second; the full-set energy pool
 * applies) and its {@link SuitModuleBehavior} runs.
 * <p><b>ZH:</b> 附属的量子套装升级单元描述（0.1.8.0 起的公共 API）。量子改装台把单元装入在 {@link #slots} 中有槽位的量子套装部件。
 * 奇点管理面板为其显示开关。开关打开时，单元耗电 {@link #drainPerTick} EU/t（每秒收取一次，适用整套能量池），并运行其 {@link SuitModuleBehavior}。
 * <p><b>JA:</b> アドオンの量子スーツ強化ユニットの記述（0.1.8.0 以降の公開 API）。量子改造台は、{@link #slots} にスロットを持つ量子スーツ部位へ
 * ユニットを取り付ける。奇点管理パネルにスイッチが表示される。スイッチがオンの間、ユニットは {@link #drainPerTick} EU/t を消費し
 * （1 秒に 1 回徴収、全身共有エネルギープールを使う）、{@link SuitModuleBehavior} が動く。
 *
 * @param id           unit id; its namespace must be the addon mod id (built-in units have no namespace)
 * @param slots        suit slots that take the unit (HEAD, CHEST, LEGS, FEET)
 * @param drainPerTick EU/t while the unit is on (0 = only event costs)
 * @param color        signature colour 0xRRGGBB (station list, HUD accents)
 * @param needsVisor   true when the unit works only with a quantum helmet worn (HUD units)
 * @param unitItem     id of the unit item; the station gives it back when the unit is removed
 */
public record SuitModuleSpec(ResourceLocation id, Set<EquipmentSlot> slots, long drainPerTick, int color, boolean needsVisor,
                             ResourceLocation unitItem) {
    public SuitModuleSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(unitItem, "unitItem");
        if (slots == null || slots.isEmpty()) throw new IllegalArgumentException("a unit needs at least one slot");
        for (EquipmentSlot s : slots) {
            if (s.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) throw new IllegalArgumentException("not an armor slot: " + s);
        }
        if (drainPerTick < 0) throw new IllegalArgumentException("negative drain");
        slots = Set.copyOf(EnumSet.copyOf(slots));
    }

    /** EN: Key in the suit piece data, the full id (for example "myaddon:sonar"). ZH: 套装部件数据中的键，即完整 ID（例如 "myaddon:sonar"）。JA: スーツ部位データ内のキー。完全な ID（例："myaddon:sonar"）。 */
    public String key() { return id.toString(); }

    /** EN: Panel switch key: fcs_&lt;namespace&gt;_&lt;path&gt;. ZH: 面板开关键：fcs_&lt;命名空间&gt;_&lt;路径&gt;。JA: パネルのスイッチキー：fcs_&lt;名前空間&gt;_&lt;パス&gt;。 */
    public String featureKey() { return "fcs_" + id.getNamespace() + "_" + id.getPath().replace('/', '_'); }

    /** EN: Name translation key module.&lt;namespace&gt;.&lt;path&gt;; add ".desc" for the description. ZH: 名称翻译键 module.&lt;命名空间&gt;.&lt;路径&gt;；说明加 ".desc"。JA: 名前の翻訳キー module.&lt;名前空間&gt;.&lt;パス&gt;。説明は ".desc" を付ける。 */
    public String nameKey() { return "module." + id.getNamespace() + "." + id.getPath().replace('/', '.'); }

    /** EN: True when the unit fits this slot. ZH: 单元适合该槽位时为真。JA: ユニットがこのスロットに合えば真。 */
    public boolean fits(EquipmentSlot slot) { return slots.contains(slot); }
}
