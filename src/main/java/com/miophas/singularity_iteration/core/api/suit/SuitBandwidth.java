// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * <p><b>EN:</b> Compute bandwidth of the quantum suit (public API since 0.1.10.1). Each active unit on a
 * worn quantum piece loads the suit computer with its cost in QB (quantum bandwidth); the budget is
 * 20 QB, more with a compute core. Above the budget the suit saturates: the sensors refresh at half
 * rate and the coherence regenerates at half speed; nothing switches off. An addon unit costs 0 QB
 * unless it is registered here, so units registered before 0.1.10.1 behave as before.
 * <p><b>ZH:</b> 量子套装的演算带宽（0.1.10.1 起的公共 API）。穿戴中的量子部件上每个生效的单元以其 QB（量子带宽）消耗占用套装计算机；
 * 预算为 20 QB，装有演算核心时更多。超出预算时套装饱和：传感器刷新减半，相干度回复速度减半；不会关闭任何功能。附属单元除非在此注册，
 * 否则消耗为 0 QB，因此 0.1.10.1 之前注册的单元行为不变。
 * <p><b>JA:</b> 量子スーツの演算帯域（0.1.10.1 以降の公開 API）。着用中の量子部位で有効な各ユニットは、その QB（量子帯域）コストで
 * スーツの計算機に負荷をかける。予算は 20 QB で、演算コアで増える。予算を超えるとスーツは飽和し、センサーの更新が半分、
 * コヒーレンスの回復が半分の速さになる。何も停止しない。アドオンユニットはここに登録しない限り 0 QB なので、0.1.10.1 より前に
 * 登録したユニットの動作は変わらない。
 */
public final class SuitBandwidth {
    private SuitBandwidth() {}

    /** EN: Highest cost a unit may declare. ZH: 单元可声明的最高消耗。JA: ユニットが宣言できる最大コスト。 */
    public static final int MAX_COST = 16;

    /** EN: Implementation hook of Singularity Iteration (not for addons). ZH: 奇点迭代的实现挂钩（附属请勿使用）。JA: 奇点迭代の実装フック（アドオンは使わない）。 */
    public interface View {
        int load(Player player);
        int budget(Player player);
    }

    private static final Map<String, Integer> COSTS = Collections.synchronizedMap(new HashMap<>());
    private static volatile View view = new View() {
        @Override public int load(Player player) { return 0; }
        @Override public int budget(Player player) { return 20; }
    };

    /**
     * <p><b>EN:</b> Sets the QB cost of addon unit {@code unit} (0..{@link #MAX_COST}). A namespace "minecraft" or a cost out of
     * range throws {@link IllegalArgumentException}.
     * <p><b>ZH:</b> 设置附属单元 {@code unit} 的 QB 消耗（0..{@link #MAX_COST}）。命名空间为 "minecraft" 或消耗越界时抛出 {@link IllegalArgumentException}。
     * <p><b>JA:</b> アドオンユニット {@code unit} の QB コスト（0..{@link #MAX_COST}）を設定する。名前空間 "minecraft" や範囲外のコストは
     * {@link IllegalArgumentException} を投げる。
     */
    public static void register(ResourceLocation unit, int cost) {
        if (unit.getNamespace().isEmpty() || "minecraft".equals(unit.getNamespace()))
            throw new IllegalArgumentException("a unit id needs the addon namespace: " + unit);
        if (cost < 0 || cost > MAX_COST) throw new IllegalArgumentException("bandwidth cost out of 0.." + MAX_COST + ": " + cost);
        COSTS.put(unit.toString(), cost);
    }

    /** EN: The registered cost of an addon unit key, or 0. ZH: 附属单元键的已注册消耗，没有则为 0。JA: アドオンユニットキーの登録済みコスト。なければ 0。 */
    public static int cost(String key) {
        Integer c = COSTS.get(key);
        return c == null ? 0 : c;
    }

    /** EN: Current load in QB. ZH: 当前负载（QB）。JA: 現在の負荷（QB）。 */
    public static int load(Player player) { return view.load(player); }

    /** EN: Current budget in QB. ZH: 当前预算（QB）。JA: 現在の予算（QB）。 */
    public static int budget(Player player) { return view.budget(player); }

    /** EN: Called once by Singularity Iteration. ZH: 由奇点迭代调用一次。JA: 奇点迭代が 1 回だけ呼ぶ。 */
    public static void setView(View implementation) { view = java.util.Objects.requireNonNull(implementation); }
}
