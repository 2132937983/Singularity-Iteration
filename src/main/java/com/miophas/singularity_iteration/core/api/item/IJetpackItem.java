// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * <p><b>EN:</b> Public jetpack contract. {@code JetpackFlightController} gives an implementing item
 * the shared lift, hover, forward thrust, height limit and energy use; since 0.1.8.1 it also
 * records the thrust for the thruster sound ({@link #getThrusterSound()}).
 * <p><b>ZH:</b> 公共喷气背包约定。{@code JetpackFlightController} 为实现本接口的物品提供统一的升力、悬停、前向推力、高度限制与能耗；
 * 自 0.1.8.1 起还记录推力，用于推进器音效（{@link #getThrusterSound()}）。
 * <p><b>JA:</b> 公開ジェットパック契約。{@code JetpackFlightController} が、実装したアイテムに共通の揚力・ホバー・前進推力・高度制限・電力消費を与える。
 * 0.1.8.1 以降は推力も記録し、スラスター音（{@link #getThrusterSound()}）に使う。
 */
public interface IJetpackItem extends IBatteryItem, IArmorModeFeatureItem {

    /** EN: Feature key of the jetpack mode (a mode-type feature). ZH: 喷气背包模式的特性键（模式型特性）。JA: ジェットパックモードの機能キー（モード型機能）。 */
    String FEATURE_MODE_KEY = "jetpack_mode";

    enum JetpackMode {
        OFF,
        NORMAL,
        HOVER,
        FLIGHT
    }

    /** EN: Lift power (0.7 electric, 1.0 quantum). ZH: 升力（电力 0.7，量子 1.0）。JA: 揚力（電気 0.7、量子 1.0）。 */
    float getThrust();

    /** EN: EU per tick in flight mode while airborne. ZH: 飞行模式下离地时每 tick 的 EU 消耗。JA: 飛行モードで空中にいる間の 1 tick あたりの EU。 */
    long getEnergyPerTickFlying();

    /** EN: Current mode of the stack. ZH: 物品当前的模式。JA: スタックの現在のモード。 */
    JetpackMode getMode(ItemStack stack);

    /** EN: Sets the mode of the stack. ZH: 设置物品的模式。JA: スタックのモードを設定する。 */
    void setMode(ItemStack stack, JetpackMode mode);

    /** EN: Height where the lift reaches 0 (it fades over the last 25 blocks). ZH: 升力降为 0 的高度（在最后 25 格内逐渐减弱）。JA: 揚力が 0 になる高さ（最後の 25 ブロックで弱まる）。 */
    default float getMaxHeight() {
        return 310.0F;
    }

    /** EN: True when getMaxHeight() applies. ZH: getMaxHeight() 生效时为真。JA: getMaxHeight() を適用するなら真。 */
    default boolean hasHeightLimit() {
        return true;
    }

    /** EN: Hover height offset (blocks). ZH: 悬停高度偏移（格）。JA: ホバー高さのオフセット（ブロック）。 */
    default float getHoverHeightOffset() {
        return 1.0F;
    }

    /** EN: Built-in thruster sound profiles. ZH: 内置推进器音效档。JA: 組み込みのスラスター音プロファイル。 */
    String THRUSTER_ELECTRIC = "electric", THRUSTER_ADVANCED = "advanced", THRUSTER_IONIC = "ionic";

    /**
     * <p><b>EN:</b> Thruster sound profile (since 0.1.8.1). The client plays an ignition cue, a loop
     * that follows the thrust, and a shutdown cue. Return {@link #THRUSTER_ELECTRIC} (light
     * turbine), {@link #THRUSTER_ADVANCED} (heavy roar) or {@link #THRUSTER_IONIC} (deep ion hum).
     * An unknown value uses the electric profile.
     * <p><b>ZH:</b> 推进器音效档（0.1.8.1 起）。客户端播放点火音、随推力变化的循环音和熄火音。返回
     * {@link #THRUSTER_ELECTRIC}（轻型涡轮）、{@link #THRUSTER_ADVANCED}（重型轰鸣）或 {@link #THRUSTER_IONIC}（低沉离子嗡鸣）。
     * 未知值使用 electric 档。
     * <p><b>JA:</b> スラスター音プロファイル（0.1.8.1 以降）。クライアントは点火音、推力に追従するループ音、停止音を鳴らす。
     * {@link #THRUSTER_ELECTRIC}（軽いタービン）、{@link #THRUSTER_ADVANCED}（重い轟音）、{@link #THRUSTER_IONIC}（低いイオン音）
     * のいずれかを返す。不明な値は electric を使う。
     */
    default String getThrusterSound() {
        return THRUSTER_ELECTRIC;
    }

    /** EN: True when the stack has the energy for one flight tick. ZH: 物品有足够一个飞行 tick 的能量时为真。JA: 1 飛行 tick 分の電力があれば真。 */
    default boolean canFlyTick(ItemStack stack) {
        long cost = Math.max(0L, getEnergyPerTickFlying());
        return !stack.isEmpty() && stack.getCount() == 1 && getEnergy(stack) >= cost;
    }

    /** EN: Takes the energy of one flight tick (Energy Saving applies); all or nothing. ZH: 扣除一个飞行 tick 的能量（适用节能），要么全部要么不扣。JA: 1 飛行 tick 分の電力を取る（省エネ適用）。全部取るか、まったく取らない。 */
    default boolean consumeFlyTick(ItemStack stack) {
        long cost = Math.max(0L, getEnergyPerTickFlying());
        if (!canFlyTick(stack)) return false;
        cost = EnergySaving.apply(stack, cost);
        if (cost == 0L) return true;
        long extracted = extractEnergy(stack, cost);
        if (extracted == cost) return true;
        if (extracted > 0L) addEnergy(stack, extracted);
        return false;
    }

    /**
     * <p><b>EN:</b> Default mode switch: hover ↔ flight. Any mode other than HOVER switches to HOVER.
     * A jetpack needs no own {@link IArmorModeFeatureItem} implementation; the GUI, the command and
     * the tooltip use this default.
     * <p><b>ZH:</b> 默认模式切换：悬停 ↔ 飞行。HOVER 以外的任何模式都切换为 HOVER。喷气背包无需自行实现 {@link IArmorModeFeatureItem}，
     * 界面、命令与提示都使用此默认实现。
     * <p><b>JA:</b> 既定のモード切替：ホバー ↔ 飛行。HOVER 以外のモードはすべて HOVER に切り替わる。ジェットパックは独自の
     * {@link IArmorModeFeatureItem} 実装を必要としない。GUI、コマンド、ツールチップはこの既定実装を使う。
     *
     * @return true if the mode changed; false if {@code featureKey} is not {@link #FEATURE_MODE_KEY} or the stack is empty
     */
    @Override
    default boolean cycleMode(ItemStack stack, String featureKey) {
        if (!FEATURE_MODE_KEY.equals(featureKey) || stack.isEmpty()) {
            return false;
        }
        JetpackMode next = getMode(stack) == JetpackMode.HOVER ? JetpackMode.FLIGHT : JetpackMode.HOVER;
        setMode(stack, next);
        return true;
    }
}
