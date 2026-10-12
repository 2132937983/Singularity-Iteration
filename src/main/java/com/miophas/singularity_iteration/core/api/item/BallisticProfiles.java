// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> Ballistic profile lookup (public API since 0.1.8.0). Order: an {@link IBallisticWeapon}
 * item, then the registered providers (the first non-null result wins, in registration order).
 * The fire-control computer then uses the profile that it learned from observed shots, then the
 * built-in vanilla profiles. Register during mod setup; {@link #find} runs on the client and
 * server threads.
 * <p><b>ZH:</b> 弹道参数查询（0.1.8.0 起的公共 API）。顺序：{@link IBallisticWeapon} 物品，然后是已注册的提供器（按注册顺序，第一个非 null 结果生效）。
 * 之后火控计算机使用从观测射击中学到的参数，最后是内置原版参数。请在模组初始化时注册；{@link #find} 在客户端与服务器线程上运行。
 * <p><b>JA:</b> 弾道パラメータの検索（0.1.8.0 以降の公開 API）。順序：{@link IBallisticWeapon} のアイテム、次に登録済みプロバイダー（登録順で、
 * 最初の null 以外の結果を使う）。その後、火器管制コンピューターは観測した射撃から学んだ値、最後に組み込みのバニラ値を使う。
 * 登録は MOD の初期化中に行う。{@link #find} はクライアントとサーバーの両スレッドで動く。
 */
public final class BallisticProfiles {
    private BallisticProfiles() {}

    /**
     * <p><b>EN:</b> Profile source for items that do not implement {@link IBallisticWeapon} (for example the weapons of a different mod).
     * <p><b>ZH:</b> 为未实现 {@link IBallisticWeapon} 的物品（例如其他模组的武器）提供参数的来源。
     * <p><b>JA:</b> {@link IBallisticWeapon} を実装しないアイテム（例：他 MOD の武器）向けのパラメータ提供元。
     */
    @FunctionalInterface
    public interface Provider {
        /**
         * <p><b>EN:</b> The profile for {@code stack}, or null when this provider does not know the item.
         * <p><b>ZH:</b> {@code stack} 的参数；本提供器不认识该物品时返回 null。
         * <p><b>JA:</b> {@code stack} のパラメータ。このプロバイダーが知らないアイテムなら null。
         */
        @Nullable
        BallisticProfile get(ItemStack stack, LivingEntity shooter, int useTicks);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    /**
     * <p><b>EN:</b> Adds a provider at the end of the lookup order.
     * <p><b>ZH:</b> 把提供器加到查询顺序的末尾。
     * <p><b>JA:</b> プロバイダーを検索順の最後に追加する。
     */
    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    /**
     * <p><b>EN:</b> The API profile of {@code stack}, or null when neither the item nor a provider knows it.
     * <p><b>ZH:</b> {@code stack} 的 API 参数；物品与提供器都不认识它时返回 null。
     * <p><b>JA:</b> {@code stack} の API パラメータ。アイテムもプロバイダーも知らなければ null。
     */
    @Nullable
    public static BallisticProfile find(ItemStack stack, LivingEntity shooter, int useTicks) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof IBallisticWeapon weapon) {
            BallisticProfile p = weapon.ballistics(stack, shooter, useTicks);
            if (p != null) return p;
        }
        for (Provider provider : PROVIDERS) {
            BallisticProfile p = provider.get(stack, shooter, useTicks);
            if (p != null) return p;
        }
        return null;
    }
}
