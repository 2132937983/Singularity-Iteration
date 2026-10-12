// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.flight;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.flight.JetpackThrustSync;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * <p><b>EN:</b> Jetpack thruster sounds on the client (0.1.8.1).
 * <ul>
 *   <li>Local player: the client flight controller already runs with the real key input. Its last
 *       thrust ({@link JetpackFlightController#lastThrust}) starts the sound without network delay.</li>
 *   <li>Other players: the state comes from {@link com.miophas.singularity_iteration.common.flight.JetpackThrustPacket}.</li>
 *   <li>Start: an ignition cue, then a loop that follows the player. Throttle changes move the
 *       volume and the pitch smoothly. Stop: the loop fades out and a shutdown cue plays.</li>
 *   <li>Hover flicker guard: a restart within {@link #REIGNITE_TICKS} of a stop does not play the
 *       ignition cue again, and a flight shorter than {@link #MIN_CUE_TICKS} plays no shutdown cue.</li>
 *   <li>Range: players farther than {@link #RANGE} blocks are not played. At most one loop per player.</li>
 * </ul>
 * <p><b>ZH:</b> 客户端的喷气背包推进器音效（0.1.8.1）。
 * <ul>
 *   <li>本地玩家：客户端飞行控制器已按真实按键运行，其最近推力立即启动音效，无网络延迟。</li>
 *   <li>其他玩家：状态来自 JetpackThrustPacket。</li>
 *   <li>开始：点火音，然后是跟随玩家的循环音。油门变化平滑地改变音量与音高。停止：循环音淡出并播放熄火音。</li>
 *   <li>悬停抖动保护：停止后 {@link #REIGNITE_TICKS} tick 内重新启动不再播放点火音；短于 {@link #MIN_CUE_TICKS} tick 的飞行不播放熄火音。</li>
 *   <li>范围：超过 {@link #RANGE} 格的玩家不播放。每位玩家最多一个循环音。</li>
 * </ul>
 * <p><b>JA:</b> クライアントのジェットパック・スラスター音（0.1.8.1）。
 * <ul>
 *   <li>ローカルプレイヤー：クライアントの飛行制御は実際のキー入力で動いている。その最新推力で、通信遅延なしに音を始める。</li>
 *   <li>他のプレイヤー：状態は JetpackThrustPacket から得る。</li>
 *   <li>開始：点火音のあと、プレイヤーに追従するループ音。スロットルの変化で音量と音程がなめらかに変わる。
 *       停止：ループ音がフェードアウトし、停止音が鳴る。</li>
 *   <li>ホバーのちらつき対策：停止から {@link #REIGNITE_TICKS} tick 以内の再始動では点火音を鳴らさない。
 *       {@link #MIN_CUE_TICKS} tick より短い飛行では停止音を鳴らさない。</li>
 *   <li>範囲：{@link #RANGE} ブロックより遠いプレイヤーは鳴らさない。ループ音はプレイヤー 1 人につき最大 1 つ。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class JetpackSoundClient {
    public static final double RANGE = 48.0;
    public static final int REIGNITE_TICKS = 8;
    public static final int MIN_CUE_TICKS = 6;

    /** EN: Sound set of one thruster profile. ZH: 一个推进器音效档的声音组。JA: 1 つのスラスター音プロファイルの音のセット。 */
    record Profile(Supplier<SoundEvent> loop, float volume, float pitch) {}
    private static final Map<String, Profile> PROFILES = Map.of(
        IJetpackItem.THRUSTER_ELECTRIC, new Profile(mio_icif_sounds.JETPACK_ELECTRIC_LOOP::get, 0.55F, 1.15F),
        IJetpackItem.THRUSTER_ADVANCED, new Profile(mio_icif_sounds.JETPACK_ADVANCED_LOOP::get, 0.7F, 0.95F),
        IJetpackItem.THRUSTER_IONIC, new Profile(mio_icif_sounds.JETPACK_IONIC_LOOP::get, 0.62F, 0.8F));

    /** EN: Remote thrust states by entity id (packed, see JetpackThrustSync#pack). ZH: 按实体 ID 的远程推力状态。JA: エンティティ ID ごとのリモート推力状態。 */
    private static final Map<Integer, Integer> REMOTE = new HashMap<>();
    private static final Map<Integer, ThrusterLoop> LOOPS = new HashMap<>();
    private static final Map<Integer, Long> LAST_STOP = new HashMap<>();

    private JetpackSoundClient() {}

    /** EN: Packet handler entry (client main thread). ZH: 数据包处理入口（客户端主线程）。JA: パケット処理の入口（クライアントのメインスレッド）。 */
    public static void remoteState(int entityId, int state) {
        if (state == 0) REMOTE.remove(entityId);
        else REMOTE.put(entityId, state);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            stopAll();
            return;
        }
        if (mc.isPaused()) return;
        long now = level.getGameTime();

        // 1. Desired states: local prediction plus remote packets.
        Map<Integer, Integer> wanted = new HashMap<>(REMOTE);
        wanted.remove(mc.player.getId());
        int local = JetpackThrustSync.pack(JetpackFlightController.lastThrust(mc.player), now);
        if (local != 0) wanted.put(mc.player.getId(), local);

        // 2. Stop the loops that are no longer wanted.
        for (Iterator<Map.Entry<Integer, ThrusterLoop>> it = LOOPS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            ThrusterLoop loop = e.getValue();
            Entity entity = level.getEntity(e.getKey());
            boolean keep = wanted.containsKey(e.getKey()) && entity != null && entity.isAlive()
                && entity.distanceToSqr(mc.player) <= RANGE * RANGE;
            if (keep && !loop.isStopped()) continue;
            loop.fadeOut();
            if (entity != null && now - loop.started >= MIN_CUE_TICKS) cue(mc, entity, mio_icif_sounds.JETPACK_SHUTDOWN.get(), loop.profile, 0.8F);
            LAST_STOP.put(e.getKey(), now);
            it.remove();
        }

        // 3. Start or update the wanted loops.
        for (var e : wanted.entrySet()) {
            Entity entity = level.getEntity(e.getKey());
            if (!(entity instanceof Player) || !entity.isAlive() || entity.distanceToSqr(mc.player) > RANGE * RANGE) continue;
            int state = e.getValue();
            float throttle = switch (state & 15) { case 1 -> 0.35F; case 2 -> 0.6F; default -> 1.0F; };
            Profile profile = PROFILES.getOrDefault(JetpackThrustSync.profile(state >> 4), PROFILES.get(IJetpackItem.THRUSTER_ELECTRIC));
            ThrusterLoop loop = LOOPS.get(e.getKey());
            if (loop != null && loop.profile != profile) {   // the flier changed jetpacks in flight
                loop.fadeOut();
                LOOPS.remove(e.getKey());
                loop = null;
            }
            if (loop == null) {
                Long stopped = LAST_STOP.get(e.getKey());
                if (stopped == null || now - stopped > REIGNITE_TICKS) cue(mc, entity, mio_icif_sounds.JETPACK_IGNITE.get(), profile, 1.0F);
                loop = new ThrusterLoop(profile, entity, now);
                LOOPS.put(e.getKey(), loop);
                mc.getSoundManager().play(loop);
            }
            // EN: local thrust gives a continuous throttle; remote states use the three levels.
            // ZH: 本地推力提供连续油门；远程状态使用三个档位。
            // JA: ローカルの推力は連続スロットル、リモート状態は 3 段階を使う。
            if (entity == mc.player) {
                var thrust = JetpackFlightController.lastThrust(mc.player);
                if (thrust != null) throttle = thrust.throttle();
            }
            loop.throttle = throttle;
        }
        LAST_STOP.values().removeIf(t -> now - t > 200);
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) stopAll();
    }

    private static void cue(Minecraft mc, Entity entity, SoundEvent sound, Profile profile, float volume) {
        mc.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.PLAYERS, volume * profile.volume / 0.6F,
            profile.pitch, RandomSource.create(), entity.getX(), entity.getY() + 0.8, entity.getZ()));
    }

    private static void stopAll() {
        LOOPS.values().forEach(ThrusterLoop::fadeOut);
        LOOPS.clear();
        REMOTE.clear();
        LAST_STOP.clear();
    }

    /**
     * <p><b>EN:</b> Looping thruster sound bound to a player. Volume = profile volume × (0.45 + 0.55 ×
     * throttle); pitch = profile pitch × (0.85 + 0.3 × throttle). Both move 15 % of the way to the
     * target each tick (smooth). The sound stops itself when the player is removed.
     * <p><b>ZH:</b> 绑定到玩家的推进器循环音。音量 = 档位音量 × (0.45 + 0.55 × 油门)；音高 = 档位音高 × (0.85 + 0.3 × 油门)。
     * 两者每 tick 向目标移动 15%（平滑）。玩家被移除时自动停止。
     * <p><b>JA:</b> プレイヤーに追従するスラスターのループ音。音量 = プロファイル音量 × (0.45 + 0.55 × スロットル)、
     * 音程 = プロファイル音程 × (0.85 + 0.3 × スロットル)。どちらも毎 tick 目標へ 15 % ずつ近づく（なめらか）。
     * プレイヤーが消えると自動で止まる。
     */
    static final class ThrusterLoop extends AbstractTickableSoundInstance {
        final Profile profile;
        final long started;
        private final Entity entity;
        float throttle = 0.6F;
        private boolean fading;

        ThrusterLoop(Profile profile, Entity entity, long started) {
            super(profile.loop.get(), SoundSource.PLAYERS, RandomSource.create(entity.getId()));
            this.profile = profile;
            this.entity = entity;
            this.started = started;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.pitch = profile.pitch * 0.85F;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            follow();
        }

        void fadeOut() { fading = true; }

        private void follow() {
            this.x = entity.getX();
            this.y = entity.getY() + 0.8;
            this.z = entity.getZ();
        }

        @Override public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            if (entity.isRemoved()) { stop(); return; }
            follow();
            if (fading) {
                volume -= profile.volume / 5F;
                pitch = Math.max(0.5F, pitch - 0.02F);   // spool-down
                if (volume <= 0.01F) stop();
                return;
            }
            float targetVolume = profile.volume * (0.45F + 0.55F * throttle);
            float targetPitch = profile.pitch * (0.85F + 0.3F * throttle);
            volume = Mth.lerp(0.15F, volume, targetVolume);
            pitch = Mth.lerp(0.15F, pitch, targetPitch);
        }
    }
}
