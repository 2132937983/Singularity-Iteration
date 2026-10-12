// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

/**
 * <p><b>EN:</b> Launch profile of a ranged weapon for the ballistic calculator (public API since 0.1.8.0).
 * The calculator steps the projectile one game tick at a time: move ({@code pos += vel}), drag
 * ({@code vel *= drag}, or {@code waterDrag} in a fluid source block), gravity
 * ({@code vel.y -= gravity}). Vanilla arrows and thrown projectiles use this order.
 * <p><b>ZH:</b> 供弹道计算器使用的远程武器发射参数（0.1.8.0 起的公共 API）。计算器每次推进一个游戏 tick：移动（{@code pos += vel}）、
 * 阻力（{@code vel *= drag}，在流体源方块中为 {@code waterDrag}）、重力（{@code vel.y -= gravity}）。原版箭与投掷物使用此顺序。
 * <p><b>JA:</b> 弾道計算機用の遠距離武器の発射パラメータ（0.1.8.0 以降の公開 API）。計算機は弾を 1 ゲーム tick ずつ進める：移動（{@code pos += vel}）、
 * 抵抗（{@code vel *= drag}。液体の水源ブロック内では {@code waterDrag}）、重力（{@code vel.y -= gravity}）。バニラの矢と投擲物はこの順序。
 *
 * @param name      short HUD label, for example "BOW" (at most about 8 characters)
 * @param speed     launch speed along the view axis, blocks per tick
 * @param gravity   downward acceleration, blocks per tick per tick (0 = no gravity)
 * @param drag      velocity multiplier per tick in air (1 = no drag; arrows 0.99)
 * @param waterDrag velocity multiplier per tick in a fluid source block (arrows 0.6)
 * @param ready     true when a shot released now fires at {@code speed} (fully drawn, loaded)
 */
public record BallisticProfile(String name, double speed, double gravity, double drag, double waterDrag, boolean ready) {
    /**
     * <p><b>EN:</b> Vanilla arrow physics (gravity 0.05, drag 0.99, water drag 0.6) at {@code speed}.
     * <p><b>ZH:</b> 原版箭的物理参数（重力 0.05、阻力 0.99、水中阻力 0.6），速度为 {@code speed}。
     * <p><b>JA:</b> バニラの矢の物理（重力 0.05、抵抗 0.99、水中抵抗 0.6）、速度 {@code speed}。
     */
    public static BallisticProfile arrow(String name, double speed, boolean ready) {
        return new BallisticProfile(name, speed, 0.05, 0.99, 0.6, ready);
    }

    /**
     * <p><b>EN:</b> Vanilla thrown projectile physics (gravity 0.03, drag 0.99, water drag 0.8) at {@code speed}.
     * <p><b>ZH:</b> 原版投掷物的物理参数（重力 0.03、阻力 0.99、水中阻力 0.8），速度为 {@code speed}。
     * <p><b>JA:</b> バニラの投擲物の物理（重力 0.03、抵抗 0.99、水中抵抗 0.8）、速度 {@code speed}。
     */
    public static BallisticProfile thrown(String name, double speed, boolean ready) {
        return new BallisticProfile(name, speed, 0.03, 0.99, 0.8, ready);
    }

    /**
     * <p><b>EN:</b> No gravity (energy bolts, no-gravity arrows), with the given drag.
     * <p><b>ZH:</b> 无重力（能量弹、无重力箭），使用给定的阻力。
     * <p><b>JA:</b> 重力なし（エネルギー弾、無重力の矢）。抵抗は引数の値。
     */
    public static BallisticProfile straight(String name, double speed, double drag, double waterDrag, boolean ready) {
        return new BallisticProfile(name, speed, 0.0, drag, waterDrag, ready);
    }

    /**
     * <p><b>EN:</b> Vanilla bow draw power (0..1) after {@code useTicks} of draw, with a full draw at {@code fullDrawTicks}.
     * <p><b>ZH:</b> 拉弓 {@code useTicks} tick 后的原版弓力（0..1），满弓时间为 {@code fullDrawTicks}。
     * <p><b>JA:</b> {@code useTicks} tick 引いた後のバニラの弓の力（0..1）。全力までの時間は {@code fullDrawTicks}。
     */
    public static double drawPower(int useTicks, int fullDrawTicks) {
        double f = Math.max(0, useTicks) / (double) Math.max(1, fullDrawTicks);
        f = (f * f + f * 2.0) / 3.0;
        return Math.min(1.0, f);
    }
}
