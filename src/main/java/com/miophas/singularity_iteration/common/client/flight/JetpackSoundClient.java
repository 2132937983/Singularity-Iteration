// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.flight;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
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
 * <p><b>EN:</b> Jetpack thruster sounds on the client (0.1.8.1, reworked in 0.1.8.4).
 * <ul>
 *   <li>Local player: the client flight controller already runs with the real key input. Its last
 *       thrust ({@link JetpackFlightController#lastThrust}) drives the sound without network delay.</li>
 *   <li>Other players: the state comes from {@link com.miophas.singularity_iteration.common.flight.JetpackThrustPacket}.</li>
 *   <li>One continuous loop per flier. Volume and pitch move smoothly toward the throttle target
 *       (rate-limited, no jumps).</li>
 *   <li>0.1.8.4 stutter fix (flying with A/D strafe or while turning): (1) the own loop played as a
 *       positional sound at the player's tick position, 0.8 blocks below the eye. Between ticks the
 *       interpolated listener moved past it, and turning swung its bearing, so the stereo pan
 *       flipped left and right: the loop crackled. The own loop and cues are now listener-relative
 *       (centred), as vanilla does for the elytra wind. (2) A short gap in the thrust samples
 *       (1-3 ticks) stopped the loop, played the shutdown cue, then restarted it with the ignition
 *       cue. Now a gap shorter than {@link #HOLD_TICKS} only dips the volume; a fading loop resumes
 *       instead of being replaced; cues need {@link #REIGNITE_TICKS} of silence and at least
 *       {@link #CUE_GAP_TICKS} between two cues of one flier.</li>
 *   <li>Range: players farther than {@link #RANGE} blocks are not played.</li>
 *   <li>0.1.10.3: quieter (profile volumes about -4 dB, cues lower, files rebuilt about 5 dB lower by
 *       {@code tools/sfx_1103.py}). The fade-out keeps the pitch; its downward glide made a short
 *       falling "biu" on the tonal loops. The shutdown cue is a noise flame-out, not a sine sweep.</li>
 * </ul>
 * <p><b>ZH:</b> 客户端的喷气背包推进器音效（0.1.8.1，0.1.8.4 重做）。本地玩家用客户端飞行控制器的推力，无网络延迟；
 * 其他玩家用 JetpackThrustPacket。每位飞行者一个连续循环音，音量与音高平滑趋近油门目标。0.1.8.4 修复左右平移与转向时的卡顿：
 * 本地循环音曾作为定位声源放在脚下 0.8 格的 tick 位置，插值后的听者在 tick 之间越过它，转向时方位摆动，左右声像来回跳；
 * 现在本地循环音与提示音相对听者播放（居中，与原版鞘翅风声相同）。1–3 tick 的推力空档曾导致停止→熄火音→点火音→重启；
 * 现在短于 HOLD_TICKS 的空档只降低音量，淡出中的循环音直接恢复，提示音需要 REIGNITE_TICKS 的静默且同一飞行者两次提示音间隔至少 CUE_GAP_TICKS。
 * 0.1.10.3：整体更安静（档位音量约 -4 dB，提示音更低，tools/sfx_1103.py 重建的音频约低 5 dB）。淡出时音高保持不变（以前下滑，在有音调的循环音上形成短促下降的“biu”）。熄火音改为噪声熄焰，不再是正弦扫频。
 * <p><b>JA:</b> クライアントのジェットパック・スラスター音（0.1.8.1、0.1.8.4 で作り直し）。ローカルプレイヤーはクライアントの飛行制御の推力で、
 * 通信遅延なしに鳴らす。他プレイヤーは JetpackThrustPacket。飛行者ごとに連続した 1 本のループ音で、音量と音程はスロットル目標へなめらかに近づく。
 * 0.1.8.4 の左右移動・旋回時の途切れ修正：自分のループ音は足元 0.8 ブロックの tick 位置に置いた定位音で、tick 間に補間後の聴取位置が
 * それを追い越し、旋回で方位が振れるため左右の定位が跳ねていた。自分のループ音と合図音は聴取者基準（中央）で鳴らす（バニラのエリトラの風音と同じ）。
 * 1〜3 tick の推力の切れ目で停止→停止音→点火音→再始動していた。HOLD_TICKS 未満の切れ目は音量が下がるだけにし、フェード中のループは再開し、
 * 合図音には REIGNITE_TICKS の無音と、同じ飛行者で CUE_GAP_TICKS 以上の間隔を必要とする。
 * 0.1.10.3：全体を静かにした（プロファイル音量は約 -4 dB、合図音も下げ、tools/sfx_1103.py で作り直した音源は約 5 dB 低い）。フェードアウト中は音程を保つ
 * （以前は下降し、音程のあるループで短く下がる「biu」になっていた）。停止音は正弦スイープではなく、ノイズによる消炎音にした。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class JetpackSoundClient {
    public static final double RANGE = 48.0;
    /** EN: Thrust gap that keeps the loop running. ZH: 保持循环音运行的推力空档。JA: ループを保つ推力の切れ目。 */
    public static final int HOLD_TICKS = 8;
    /** EN: Silence before an ignition cue may play again. ZH: 再次点火音前需要的静默。JA: 点火音を再び鳴らすまでの無音。 */
    public static final int REIGNITE_TICKS = 20;
    /** EN: Shortest flight that ends with a shutdown cue. ZH: 播放熄火音的最短飞行。JA: 停止音を鳴らす最短の飛行。 */
    public static final int MIN_CUE_TICKS = 10;
    /** EN: Shortest time between two cues of one flier. ZH: 同一飞行者两次提示音的最短间隔。JA: 同じ飛行者の合図音の最短間隔。 */
    public static final int CUE_GAP_TICKS = 16;

    /** EN: Sound set of one thruster profile. ZH: 一个推进器音效档的声音组。JA: 1 つのスラスター音プロファイルの音のセット。 */
    record Profile(Supplier<SoundEvent> loop, float volume, float pitch) {}
    // EN: 0.1.10.3: volumes lowered (0.55 / 0.7 / 0.62 before); the electric pitch is no longer raised (it sharpened the fan tone).
    // ZH: 0.1.10.3：降低音量（原为 0.55 / 0.7 / 0.62）；电动档不再升高音高（会使风扇音变尖）。
    // JA: 0.1.10.3：音量を下げた（以前は 0.55 / 0.7 / 0.62）。電動の音程はもう上げない（ファン音が鋭くなっていた）。
    private static final Map<String, Profile> PROFILES = Map.of(
        IJetpackItem.THRUSTER_ELECTRIC, new Profile(mio_icif_sounds.JETPACK_ELECTRIC_LOOP::get, 0.36F, 1.0F),
        IJetpackItem.THRUSTER_ADVANCED, new Profile(mio_icif_sounds.JETPACK_ADVANCED_LOOP::get, 0.44F, 0.95F),
        IJetpackItem.THRUSTER_IONIC, new Profile(mio_icif_sounds.JETPACK_IONIC_LOOP::get, 0.4F, 0.85F));
    /** EN: Fade-out length in ticks. ZH: 淡出时长（tick）。JA: フェードアウトの長さ（tick）。 */
    static final int FADE_TICKS = 10;

    /** EN: Remote thrust states by entity id (packed, see JetpackThrustSync#pack). ZH: 按实体 ID 的远程推力状态。JA: エンティティ ID ごとのリモート推力状態。 */
    private static final Map<Integer, Integer> REMOTE = new HashMap<>();
    /** EN: Sound state per flier. ZH: 每位飞行者的音效状态。JA: 飛行者ごとの音の状態。 */
    private static final Map<Integer, Track> TRACKS = new HashMap<>();

    /** EN: One flier: its loop, when thrust was last seen, when the flight started, the last cue. */
    private static final class Track {
        ThrusterLoop loop;
        long lastThrust = Long.MIN_VALUE / 2;
        long flightStart;
        long lastCue = Long.MIN_VALUE / 2;
        long silentSince = Long.MIN_VALUE / 2;
    }

    private JetpackSoundClient() {}

    /**
     * 0.1.10.31: jetpack thrust of a player now, 0..1 (0 when it does not fly on a jetpack), for the thruster effects: the own
     * player from its local thrust (continuous), another player from the relayed state (three levels, as the sound uses them).
     */
    public static float throttle(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || player == null) return 0F;
        if (player == mc.player) {
            var thrust = JetpackFlightController.lastThrust(player);
            return thrust == null || mc.level.getGameTime() - thrust.tick() > JetpackThrustSync.GRACE_TICKS ? 0F : thrust.throttle();
        }
        Integer state = REMOTE.get(player.getId());
        if (state == null) return 0F;
        return switch (state & 15) { case 1 -> 0.35F; case 2 -> 0.6F; default -> 1.0F; };
    }

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

        // 1. Thrust seen this tick: local prediction plus remote packets.
        Map<Integer, Integer> wanted = new HashMap<>(REMOTE);
        wanted.remove(mc.player.getId());
        int local = JetpackThrustSync.pack(JetpackFlightController.lastThrust(mc.player), now);
        if (local != 0) wanted.put(mc.player.getId(), local);

        // 2. Start, resume or update the loops of fliers with thrust.
        for (var e : wanted.entrySet()) {
            Entity entity = level.getEntity(e.getKey());
            if (!(entity instanceof Player) || !entity.isAlive() || entity.distanceToSqr(mc.player) > RANGE * RANGE) continue;
            int state = e.getValue();
            Profile profile = PROFILES.getOrDefault(JetpackThrustSync.profile(state >> 4), PROFILES.get(IJetpackItem.THRUSTER_ELECTRIC));
            float throttle = switch (state & 15) { case 1 -> 0.35F; case 2 -> 0.6F; default -> 1.0F; };
            // EN: local thrust gives a continuous throttle; remote states use the three levels.
            // ZH: 本地推力提供连续油门；远程状态使用三个档位。
            // JA: ローカルの推力は連続スロットル、リモート状態は 3 段階。
            if (entity == mc.player) {
                var thrust = JetpackFlightController.lastThrust(mc.player);
                if (thrust != null) throttle = thrust.throttle();
            }
            Track track = TRACKS.computeIfAbsent(e.getKey(), k -> new Track());
            boolean wasSilent = track.loop == null || track.loop.isStopped();
            if (track.loop != null && !track.loop.isStopped() && track.loop.profile != profile) {
                track.loop.fadeOut();        // the flier changed jetpacks in flight
                track.loop = null;
                wasSilent = true;
            }
            if (wasSilent) {
                if (now - track.silentSince > REIGNITE_TICKS && now - track.lastCue >= CUE_GAP_TICKS) {
                    cue(mc, entity, mio_icif_sounds.JETPACK_IGNITE.get(), profile, 0.7F);
                    track.lastCue = now;
                }
                track.loop = new ThrusterLoop(profile, entity, entity == mc.player);
                track.flightStart = now;
                mc.getSoundManager().play(track.loop);
            } else {
                track.loop.resume();
            }
            track.loop.throttle = throttle;
            track.lastThrust = now;
        }

        // 3. Fliers without thrust: hold through short gaps, then fade out (and maybe cue).
        for (Iterator<Map.Entry<Integer, Track>> it = TRACKS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Track track = e.getValue();
            Entity entity = level.getEntity(e.getKey());
            boolean gone = entity == null || !entity.isAlive() || entity.distanceToSqr(mc.player) > RANGE * RANGE;
            if (track.lastThrust == now && !gone) continue;
            ThrusterLoop loop = track.loop;
            if (loop != null && !loop.isStopped() && !loop.fading && (gone || now - track.lastThrust >= HOLD_TICKS)) {
                loop.fadeOut();
                track.silentSince = now;
                if (!gone && now - track.flightStart >= MIN_CUE_TICKS && now - track.lastCue >= CUE_GAP_TICKS) {
                    cue(mc, entity, mio_icif_sounds.JETPACK_SHUTDOWN.get(), loop.profile, 0.6F);
                    track.lastCue = now;
                }
            } else if (loop != null && !loop.fading && !loop.isStopped()) {
                loop.idle();                 // short gap: dip toward the idle level, keep playing
            }
            if ((loop == null || loop.isStopped()) && now - track.lastThrust > 200) it.remove();
        }
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) stopAll();
    }

    private static void cue(Minecraft mc, Entity entity, SoundEvent sound, Profile profile, float volume) {
        // 0.1.8.3: client options jetpack_cues and jetpack_volume
        if (!FcsClientConfig.jetpackCues()) return;
        float scale = FcsClientConfig.jetpackVolume();
        if (scale <= 0) return;
        float v = volume * scale * profile.volume / 0.4F;
        if (entity == mc.player) {
            // 0.1.8.4: the own cues are centred on the listener, like the own loop
            mc.getSoundManager().play(new SimpleSoundInstance(sound.getLocation(), SoundSource.PLAYERS, v, profile.pitch,
                RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
        } else {
            mc.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.PLAYERS, v,
                profile.pitch, RandomSource.create(), entity.getX(), entity.getY() + 0.8, entity.getZ()));
        }
    }

    private static void stopAll() {
        TRACKS.values().forEach(t -> { if (t.loop != null) t.loop.fadeOut(); });
        TRACKS.clear();
        REMOTE.clear();
    }

    /**
     * <p><b>EN:</b> Looping thruster sound of one flier. Target volume = profile volume × (0.45 +
     * 0.55 × throttle) × option volume; target pitch = profile pitch × (0.85 + 0.3 × throttle). The
     * volume moves 15 % of the way per tick; the pitch at most 0.015 per tick (no warble). During a
     * short thrust gap the target is the idle level (55 %). Fading takes {@link #FADE_TICKS} ticks with
     * the pitch held (0.1.10.3: no downward glide) and can be resumed.
     * The own loop is listener-relative (centred); other fliers are positional and followed.
     * <p><b>ZH:</b> 一位飞行者的推进器循环音。目标音量 = 档位音量 × (0.45 + 0.55 × 油门) × 选项音量；目标音高 = 档位音高 × (0.85 + 0.3 × 油门)。
     * 音量每 tick 趋近 15%，音高每 tick 最多变化 0.015（不颤音）。短暂推力空档时目标为怠速（55%）。淡出 FADE_TICKS tick，期间音高保持不变（0.1.10.3：不再下滑），可恢复。
     * 本地循环音相对听者（居中），其他飞行者为定位声源并跟随。
     * <p><b>JA:</b> 1 人の飛行者のスラスター・ループ音。目標音量 = プロファイル音量 × (0.45 + 0.55 × スロットル) × 設定音量、
     * 目標音程 = プロファイル音程 × (0.85 + 0.3 × スロットル)。音量は毎 tick 15 % ずつ、音程は毎 tick 最大 0.015 ずつ近づく（揺れない）。
     * 推力の短い切れ目ではアイドル（55 %）を目標にする。フェードは FADE_TICKS tick で音程を保ったまま（0.1.10.3：下降しない）行い、再開できる。自分のループは聴取者基準（中央）、他の飛行者は定位音で追従する。
     */
    static final class ThrusterLoop extends AbstractTickableSoundInstance {
        final Profile profile;
        private final Entity entity;
        private final boolean own;
        float throttle = 0.6F;
        boolean fading;
        private boolean idle;

        ThrusterLoop(Profile profile, Entity entity, boolean own) {
            super(profile.loop.get(), SoundSource.PLAYERS, RandomSource.create(entity.getId()));
            this.profile = profile;
            this.entity = entity;
            this.own = own;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.pitch = profile.pitch * 0.85F;
            if (own) {
                this.relative = true;
                this.attenuation = SoundInstance.Attenuation.NONE;
                this.x = this.y = this.z = 0;
            } else {
                this.attenuation = SoundInstance.Attenuation.LINEAR;
                follow();
            }
        }

        void fadeOut() { fading = true; }

        void resume() { fading = false; idle = false; }

        void idle() { idle = true; }

        private void follow() {
            this.x = entity.getX();
            this.y = entity.getY() + 0.8;
            this.z = entity.getZ();
        }

        @Override public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            if (entity.isRemoved()) { stop(); return; }
            if (!own) follow();
            float option = FcsClientConfig.jetpackVolume();
            float targetVolume = fading ? 0F : profile.volume * (0.45F + 0.55F * throttle) * option * (idle ? 0.55F : 1F);
            float targetPitch = profile.pitch * (0.85F + 0.3F * throttle);
            volume = fading ? Math.max(0F, volume - profile.volume / FADE_TICKS) : Mth.lerp(0.15F, volume, targetVolume);
            if (!fading) pitch = pitch + Mth.clamp(targetPitch - pitch, -0.015F, 0.015F);   // held while fading
            idle = false;
            if (fading && volume <= 0.01F) stop();
        }
    }
}
