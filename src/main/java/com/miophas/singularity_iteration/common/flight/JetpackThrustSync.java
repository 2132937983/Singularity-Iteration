// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.flight;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * <p><b>EN:</b> Server side of the jetpack thruster sound (0.1.8.1). Each server tick it reads the
 * last thrust of each player ({@link JetpackFlightController#lastThrust}). When the packed state
 * (throttle level and sound profile) changes, it sends {@link JetpackThrustPacket} to the players
 * that track the flier. A player that starts to track an active flier gets the state at once.
 * Memory: one map entry per flying player; logout and server stop remove the entries.
 *
 * <p><b>ZH:</b> 喷气背包推进器音效的服务器端（0.1.8.1）。每个服务器 tick 读取每位玩家最近的推力。打包状态（油门档位与音效档）
 * 变化时，向追踪该玩家的玩家发送 {@link JetpackThrustPacket}。开始追踪正在飞行的玩家时立即收到状态。
 * 内存：每位飞行玩家一个映射项；登出与服务器停止时删除。
 *
 * <p><b>JA:</b> ジェットパックのスラスター音のサーバー側（0.1.8.1）。毎サーバー tick、各プレイヤーの最新推力を読む。
 * 圧縮状態（スロットル段階と音プロファイル）が変わると、飛行者を追跡しているプレイヤーへ {@link JetpackThrustPacket} を送る。
 * 飛行中のプレイヤーの追跡を始めたプレイヤーには、すぐに状態を送る。メモリ：飛行中のプレイヤー 1 人につき 1 項目。
 * ログアウトとサーバー停止で削除する。
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class JetpackThrustSync {
    /** EN: Ticks after the last thrust that still count as active. ZH: 最后一次推力后仍视为活动的 tick 数。JA: 最後の推力から活動中とみなす tick 数。 */
    public static final int GRACE_TICKS = 2;
    private static final String[] PROFILES = {IJetpackItem.THRUSTER_ELECTRIC, IJetpackItem.THRUSTER_ADVANCED, IJetpackItem.THRUSTER_IONIC};
    private static final Map<UUID, Integer> SENT = new HashMap<>();

    private JetpackThrustSync() {}

    /**
     * <p><b>EN:</b> Packed state: 0 when off; else the throttle level (1 below 0.45, 2 below 0.75,
     * else 3) in bits 0-3 and the profile index in bits 4-7.
     * <p><b>ZH:</b> 打包状态：关闭为 0；否则位 0-3 为油门档位（< 0.45 为 1，< 0.75 为 2，其余为 3），位 4-7 为音效档索引。
     * <p><b>JA:</b> 圧縮状態：停止なら 0。それ以外はビット 0-3 にスロットル段階（0.45 未満で 1、0.75 未満で 2、それ以上で 3）、
     * ビット 4-7 にプロファイル番号。
     */
    public static int pack(JetpackFlightController.Thrust thrust, long now) {
        if (thrust == null || now - thrust.tick() > GRACE_TICKS) return 0;
        int level = thrust.throttle() < 0.45F ? 1 : thrust.throttle() < 0.75F ? 2 : 3;
        return level | profileIndex(thrust.sound()) << 4;
    }

    /** EN: Index of a profile key (unknown keys use 0, electric). ZH: 音效档索引（未知为 0）。JA: プロファイル番号（不明なら 0）。 */
    public static int profileIndex(String sound) {
        for (int i = 0; i < PROFILES.length; i++) if (PROFILES[i].equals(sound)) return i;
        return 0;
    }

    /** EN: Profile key of an index. ZH: 索引对应的音效档。JA: 番号に対応するプロファイル。 */
    public static String profile(int index) {
        return PROFILES[Math.floorMod(index, PROFILES.length)];
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            int state = pack(JetpackFlightController.lastThrust(player), player.level().getGameTime());
            Integer sent = SENT.get(player.getUUID());
            if (sent == null ? state == 0 : sent == state) continue;
            if (state == 0) SENT.remove(player.getUUID());
            else SENT.put(player.getUUID(), state);
            PacketDistributor.sendToPlayersTrackingEntity(player, new JetpackThrustPacket(player.getId(), state));
        }
    }

    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof ServerPlayer flier) || !(event.getEntity() instanceof ServerPlayer tracker)) return;
        Integer state = SENT.get(flier.getUUID());
        if (state != null) PacketDistributor.sendToPlayer(tracker, new JetpackThrustPacket(flier.getId(), state));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        SENT.clear();
    }
}
