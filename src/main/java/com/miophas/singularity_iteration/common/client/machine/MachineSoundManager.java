package com.miophas.singularity_iteration.common.client.machine;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.machine.ClientRunState;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.core.api.machine.MachineStatus;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * <p><b>EN:</b> Running-machine sound loops, entirely client side.
 * <ul>
 *   <li>Run state: the block-state property (active / lit / working / running), the synced status
 *       lamp ({@link AbstractEnergyBlockEntity#clientMachineStatus()} = RUNNING), or a synced flag
 *       ({@link ClientRunState}). No new network traffic and no server tick cost.</li>
 *   <li>Every {@link #SCAN_INTERVAL} ticks the block entities of the 3 x 3 chunks around the
 *       player are scanned. The nearest {@link #MAX_LOOPS} running machines within
 *       {@link #RADIUS} blocks get a looping, fading sound; at most {@link #MAX_PER_SOUND} use the
 *       same loop. Big factories thus cost one short scan every half second and a few channels.</li>
 *   <li>Press and crusher loops start on the machine's animation phase, so the impacts line up
 *       with the moving parts.</li>
 *   <li>Each mod machine is either mapped to a loop ({@code map}) or marked silent with a reason
 *       ({@code silent}). {@code tools/sound_audit_181.py} reads both lists and reports every
 *       machine that has neither.</li>
 * </ul>
 * <p><b>ZH:</b> 运行中机器的循环音效，完全在客户端处理。
 * <ul>
 *   <li>运行状态：方块状态属性（active / lit / working / running）、已同步的状态灯（RUNNING）或已同步的标志
 *       （{@link ClientRunState}）。不增加网络流量，也不占用服务器 tick。</li>
 *   <li>每 {@link #SCAN_INTERVAL} tick 扫描玩家周围 3 × 3 区块的方块实体。{@link #RADIUS} 格内最近的
 *       {@link #MAX_LOOPS} 台运行中机器播放带淡入淡出的循环音；同一循环音最多 {@link #MAX_PER_SOUND} 台。
 *       大型工厂每半秒只需一次短扫描和少量声道。</li>
 *   <li>冲压机与粉碎机的循环音在机器动画周期的起点开始，撞击声与运动部件对齐。</li>
 *   <li>每台本模组机器要么映射到循环音（{@code map}），要么带理由标记为静音（{@code silent}）。
 *       {@code tools/sound_audit_181.py} 读取这两个列表，并报告两者都没有的机器。</li>
 * </ul>
 * <p><b>JA:</b> 稼働中の機械のループ音。すべてクライアント側で処理する。
 * <ul>
 *   <li>稼働状態：ブロックステートのプロパティ（active / lit / working / running）、同期済みのステータスランプ（RUNNING）、
 *       または同期済みフラグ（{@link ClientRunState}）。通信量もサーバーの tick コストも増えない。</li>
 *   <li>{@link #SCAN_INTERVAL} tick ごとにプレイヤー周囲 3 × 3 チャンクのブロックエンティティを走査する。
 *       {@link #RADIUS} ブロック以内で最も近い {@link #MAX_LOOPS} 台がフェード付きループ音を鳴らす。同じループ音は最大
 *       {@link #MAX_PER_SOUND} 台。大工場でも 0.5 秒ごとの短い走査と少数のチャンネルで済む。</li>
 *   <li>プレス機と粉砕機のループ音は機械のアニメーション周期の始点で始まり、打撃音が可動部と合う。</li>
 *   <li>本 MOD の各機械は、ループ音への割り当て（{@code map}）か、理由付きの無音指定（{@code silent}）のどちらかを持つ。
 *       {@code tools/sound_audit_181.py} が両方の一覧を読み、どちらもない機械を報告する。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class MachineSoundManager {
    public static final int SCAN_INTERVAL = 10;
    public static final int MAX_LOOPS = 6;
    public static final double RADIUS = 16.0;
    public static final int MAX_PER_SOUND = 2;

    /** Loop sound, volume and pitch per machine (registry path). Machines not listed are silent. */
    record Profile(Supplier<SoundEvent> sound, float volume, float pitch, boolean phased) {}
    private static final Map<String, Profile> BY_PATH = new HashMap<>();
    private static final Map<Block, Profile> CACHE = new IdentityHashMap<>();
    private static final Profile SILENT = new Profile(() -> null, 0, 1, false);

    static {
        Profile motor = p(mio_icif_sounds.LOOP_MOTOR, 0.55F), press = new Profile(mio_icif_sounds.LOOP_PRESS::get, 0.6F, 1F, true),
            crusher = new Profile(mio_icif_sounds.LOOP_CRUSHER::get, 0.6F, 1F, true), centrifuge = p(mio_icif_sounds.LOOP_CENTRIFUGE, 0.5F),
            saw = p(mio_icif_sounds.LOOP_SAW, 0.45F), furnace = p(mio_icif_sounds.LOOP_FURNACE, 0.5F), fluid = p(mio_icif_sounds.LOOP_FLUID, 0.5F),
            electric = p(mio_icif_sounds.LOOP_ELECTRIC, 0.45F), hum = p(mio_icif_sounds.LOOP_ELECTRIC, 0.25F),
            generator = p(mio_icif_sounds.LOOP_GENERATOR, 0.55F), turbine = p(mio_icif_sounds.LOOP_TURBINE, 0.5F),
            reactor = p(mio_icif_sounds.LOOP_REACTOR, 0.6F), drill = p(mio_icif_sounds.LOOP_DRILL, 0.55F);
        map(press, "producer/block_compressor_elc", "producer/block_compressor_advanced_elc", "producer/block_extruding_machine",
            "producer/block_metal_former", "producer/block_metal_former_advanced");
        map(crusher, "producer/block_powder_elc", "producer/block_powder_advanced_elc", "producer/block_recycler_elc");
        map(saw, "producer/block_block_cutter", "producer/block_lathe");
        map(centrifuge, "producer/block_extractor_elc", "producer/block_centrifuge_elc", "producer/block_washer_elc");
        map(furnace, "producer/block_furnace_elc", "producer/block_induction_elc", "producer/block_blast_furnace",
            "producer/block_blast_furnace_advanced", "producer/block_blast_furnace_elc", "hugenerator/block_heat_generator_elc",
            "generator/block_thermal_generator");
        map(fluid, "producer/block_pump_elc", "producer/block_canner_elc", "producer/block_fermenter_elc", "producer/block_condenser",
            "producer/block_fluid_distributor_elc", "producer/block_weighted_fluid_distributor_elc", "producer/block_fluid_regulator_elc",
            "producer/block_solar_distiller", "producer/block_oil_refinery_elc", "producer/block_lapis_reactor_coolant_injector",
            "producer/block_redstone_reactor_coolant_injector", "producer/block_steam_repressurizer", "producer/block_steam_generator",
            "hugenerator/block_fluid_heat_generator", "hugenerator/block_heat_source_fluid");
        // 0.1.8.0: dedicated drive loops for the UU-matter fabricator and the kinetic generators
        map(p(mio_icif_sounds.LOOP_UU_FABRICATOR, 0.5F), "producer/block_matter_elc");
        map(p(mio_icif_sounds.LOOP_KINETIC_DRIVE, 0.5F), "generator/block_kinetic_generator", "generator/block_turbo_kinetic_generator",
            "generator/block_twin_turbo_kinetic_generator");
        map(electric, "producer/block_replicator_elc", "producer/block_teleporter_elc",
            "producer/block_molecular_transformer", "producer/block_neutron_polymerizer", "producer/block_chunk_loader",
            "producer/block_terra_elc", "generator/block_quantum_generator", "generator/block_geomagnetic_generator",
            "wiring/block_wireless_power_transmission_node");
        map(hum, "wiring/transformer_lv_mv", "wiring/transformer_mv_hv", "wiring/transformer_hv_ev", "wiring/transformer_ev_sc",
            "wiring/transformer_iv_luv", "wiring/transformer_luv_zpmv");
        map(generator, "generator/block_geo_generator", "generator/block_semifluid_generator", "generator/block_advanced_semifluid_generator",
            "generator/block_diesel_generator", "generator/block_drop_generator", "generator/block_advanced_drop_generator",
            "generator/block_experience_generator", "generator/block_advanced_experience_generator", "hugenerator/block_solid_heat_generator");
        map(turbine, "generator/block_steam_tunnel");   // 0.1.10.39
        map(turbine, "generator/block_stirling_generator", "generator/block_advanced_stirling_generator", "kugenerator/block_stirling_kinetic_generator",
            "producer/block_steam_kinetic_generator");
        map(reactor, "generator/block_nuclear_reactor_generator", "generator/block_rt_generator", "hugenerator/block_rt_heat_generator");
        map(drill, "producer/block_miner_elc", "producer/block_advanced_miner_elc", "oilrig/block_oil_rig_panel");
        map(motor, "producer/block_batch_crafter");

        // 0.1.8.1 sound audit (tools/sound_audit_181.py): loops for the machines that were silent
        map(p(mio_icif_sounds.LOOP_DISCHARGE, 0.5F), "producer/block_tesla");
        map(p(mio_icif_sounds.LOOP_COMPRESSOR, 0.45F), "producer/block_matron_elc");
        map(p(mio_icif_sounds.LOOP_SERVO, 0.45F), "producer/block_harvest_elc", "producer/block_sorter_elc");
        map(new Profile(mio_icif_sounds.LOOP_SERVO::get, 0.35F, 1.1F, false), "producer/block_blueprint_builder");
        map(new Profile(mio_icif_sounds.LOOP_SERVO::get, 0.25F, 1.2F, false), "producer/block_item_buffer_elc");
        map(p(mio_icif_sounds.LOOP_BUBBLER, 0.5F), "producer/block_electrolyzer_elc");
        map(new Profile(mio_icif_sounds.LOOP_BUBBLER::get, 0.25F, 0.85F, false), "producer/block_barrel");
        map(new Profile(mio_icif_sounds.LOOP_ELECTRIC::get, 0.4F, 0.75F, false), "producer/block_magnetizer");
        map(new Profile(mio_icif_sounds.LOOP_ELECTRIC::get, 0.3F, 1.25F, false), "producer/block_scanner_elc");
        map(new Profile(mio_icif_sounds.LOOP_ELECTRIC::get, 0.3F, 1.2F, false), "producer/block_quantum_modification_station");
        map(new Profile(mio_icif_sounds.LOOP_ELECTRIC::get, 0.18F, 1.3F, false), "producer/block_pattern_storage");
        map(new Profile(mio_icif_sounds.LOOP_ELECTRIC::get, 0.22F, 1.15F, false), "wiring/block_batbox_charger", "wiring/block_cesu_charger",
            "wiring/block_mfe_charger", "wiring/block_mfsu_charger", "wiring/block_lesu_charger", "wiring/block_eesu_charger");
        map(new Profile(mio_icif_sounds.LOOP_ELECTRIC::get, 0.18F, 1.35F, false), "generator/block_photon_resonance_solar_generator",
            "generator/block_ultimate_photon_resonance_solar_generator", "generator/block_quantum_solar_panel");
        map(new Profile(mio_icif_sounds.LOOP_UU_FABRICATOR::get, 0.6F, 0.85F, false), "producer/block_large_fabricator_core");
        map(p(mio_icif_sounds.LOOP_MOTOR, 0.5F), "kugenerator/block_kinetic_generator_elc");
        map(new Profile(mio_icif_sounds.LOOP_TURBINE::get, 0.28F, 0.6F, false), "generator/block_wind_generator",
            "kugenerator/block_wind_kinetic_generator");
        map(new Profile(mio_icif_sounds.LOOP_FLUID::get, 0.32F, 0.8F, false), "generator/block_water_generator",
            "kugenerator/block_water_kinetic_generator");

        silent("solar panel without moving parts", "generator/block_solar_generator", "generator/block_advanced_solar_panel",
            "generator/block_hybrid_solar_panel", "generator/block_ultimate_hybrid_solar_panel", "generator/block_mets_advanced_solar_generator");
        silent("creative test generator", "generator/block_unlimit_generator", "generator/block_creative_generator");
        silent("plays its own suit-up sequence", "producer/block_armory");
        silent("plays its own lock / charge / fire / hit cues", "producer/block_laser_defense_tower", "producer/block_sky_patrol_laser_tower");
        silent("plays its own ambient cue (EnergyTerminalClient)", "wiring/block_energy_terminal");
        silent("cable; the state is a redstone signal", "wiring/block_eu_detector_cable", "wiring/block_eu_splitter_cable");
    }

    private static Profile p(Supplier<SoundEvent> s, float v) { return new Profile(s, v, 1F, false); }
    private static void map(Profile p, String... paths) { for (String s : paths) BY_PATH.put(s, p); }
    /** EN: Silent on purpose; the reason goes into the sound audit. ZH: 有意静音，理由写入音效审计。JA: 意図的に無音。理由は音の監査に載る。 */
    private static void silent(String reason, String... paths) { for (String s : paths) BY_PATH.put(s, SILENT); }

    static Profile profile(Block block) {
        return CACHE.computeIfAbsent(block, b -> {
            var key = BuiltInRegistries.BLOCK.getKey(b);
            if (!key.getNamespace().equals(Singularity_Iteration.MOD_ID)) return SILENT;
            return BY_PATH.getOrDefault(key.getPath(), SILENT);
        });
    }

    /**
     * <p><b>EN:</b> True when the client can see that the machine runs (property, lamp or flag).
     * <p><b>ZH:</b> 客户端能看出机器在运行时为真（属性、状态灯或标志）。
     * <p><b>JA:</b> クライアントから機械の稼働が分かるとき真（プロパティ、ランプ、フラグ）。
     */
    static boolean isRunning(BlockEntity be) {
        if (MachineRunState.isRunning(be.getBlockState())) return true;
        if (be instanceof AbstractEnergyBlockEntity energy && energy.clientMachineStatus() == MachineStatus.RUNNING) return true;
        return be instanceof ClientRunState flag && flag.clientRunning();
    }

    private static final Map<BlockPos, MachineLoopSound> PLAYING = new HashMap<>();
    private static final Map<BlockPos, Profile> PENDING = new HashMap<>();
    private static int tick;
    private static long lastScanNanos;

    private MachineSoundManager() {}

    /** Diagnostics: positions and sound ids of the loops currently playing. */
    public static String describe() {
        StringBuilder b = new StringBuilder().append(String.format("scan %.3f ms; ", lastScanNanos / 1e6)).append(PLAYING.size()).append(" loops, ").append(PENDING.size()).append(" pending");
        PLAYING.forEach((pos, s) -> b.append(" | ").append(pos.toShortString()).append(' ').append(s.getLocation().getPath()));
        return b.toString();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) {
            if (level == null) stopAll();
            return;
        }
        if (muted()) {
            if (!PLAYING.isEmpty() || !PENDING.isEmpty()) stopAll();
            return;
        }
        if (!PENDING.isEmpty()) startPhased(mc, level);
        if (++tick % SCAN_INTERVAL != 0) return;
        long t0 = System.nanoTime();
        scan(mc, level);
        lastScanNanos = System.nanoTime() - t0;
    }

    /**
     * 0.1.10.14: machine noise is off: the client config ({@code machine_sounds}) or the local player wears
     * the Soundproof Earmuffs. The loops fade out and the machine sounds the server sends are dropped.
     */
    public static boolean muted() {
        Minecraft mc = Minecraft.getInstance();
        return !com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.machineSounds()
            || mc.player != null && com.miophas.singularity_iteration.common.item.armor.mio_icif_soundproof_earmuffs.isWorn(mc.player);
    }

    /** Machine working sounds of this mod: the loops, the job-finished chime, the work and switch cues, the terminal hum. */
    static boolean isMachineNoise(ResourceLocation id) {
        if (!id.getNamespace().equals(Singularity_Iteration.MOD_ID)) return false;
        String path = id.getPath();
        return path.startsWith("machine.") && !path.equals("machine.demolish") || path.equals("terminal.ambient");
    }

    /** Drops the machine sounds the server broadcasts (the job-finished chime above all) while muted. */
    @SubscribeEvent
    static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound != null && isMachineNoise(sound.getLocation()) && muted()) event.setSound(null);
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) stopAll();
    }

    private static void scan(Minecraft mc, ClientLevel level) {
        var player = mc.player.position();
        int pcx = mc.player.chunkPosition().x, pcz = mc.player.chunkPosition().z;
        List<BlockPos> candidates = new ArrayList<>();
        Map<BlockPos, Profile> profiles = new HashMap<>();
        double r2 = RADIUS * RADIUS;
        for (int cx = pcx - 1; cx <= pcx + 1; cx++) {
            for (int cz = pcz - 1; cz <= pcz + 1; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    Profile prof = profile(be.getBlockState().getBlock());
                    if (prof == SILENT || !isRunning(be)) continue;
                    BlockPos pos = be.getBlockPos();
                    if (pos.getCenter().distanceToSqr(player) > r2) continue;
                    candidates.add(pos);
                    profiles.put(pos, prof);
                }
            }
        }
        candidates.sort((a, b) -> Double.compare(a.getCenter().distanceToSqr(player), b.getCenter().distanceToSqr(player)));
        // nearest machines first, at most MAX_PER_SOUND loops of one kind (a varied, uncluttered mix)
        List<BlockPos> wanted = new ArrayList<>(MAX_LOOPS);
        Map<Profile, Integer> perKind = new IdentityHashMap<>();
        for (BlockPos pos : candidates) {
            if (wanted.size() >= MAX_LOOPS) break;
            Profile prof = profiles.get(pos);
            int n = perKind.getOrDefault(prof, 0);
            if (n >= MAX_PER_SOUND) continue;
            perKind.put(prof, n + 1);
            wanted.add(pos);
        }

        for (Iterator<Map.Entry<BlockPos, MachineLoopSound>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (!wanted.contains(e.getKey()) || e.getValue().isStopped()) {
                e.getValue().fadeOut();
                it.remove();
            }
        }
        PENDING.keySet().removeIf(p -> !wanted.contains(p));
        for (BlockPos pos : wanted) {
            if (PLAYING.containsKey(pos) || PENDING.containsKey(pos)) continue;
            Profile prof = profiles.get(pos);
            if (prof.phased) PENDING.put(pos, prof); else start(mc, pos, prof);
        }
    }

    /** Phased loops (press/crusher) start exactly on the machine's animation cycle boundary. */
    private static void startPhased(Minecraft mc, ClientLevel level) {
        long time = level.getGameTime();
        for (Iterator<Map.Entry<BlockPos, Profile>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if ((time + MachineRunState.phaseOffset(e.getKey())) % MachineRunState.CYCLE_TICKS == 0) {
                start(mc, e.getKey(), e.getValue());
                it.remove();
            }
        }
    }

    private static void start(Minecraft mc, BlockPos pos, Profile prof) {
        SoundEvent sound = prof.sound.get();
        if (sound == null) return;
        MachineLoopSound s = new MachineLoopSound(sound, pos, prof.volume, prof.pitch);
        PLAYING.put(pos, s);
        mc.getSoundManager().play(s);
    }

    private static void stopAll() {
        PLAYING.values().forEach(MachineLoopSound::fadeOut);
        PLAYING.clear();
        PENDING.clear();
    }

    /** Looping positional sound with fade in/out; stops itself when the machine stops or goes away. */
    static final class MachineLoopSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private final float target;
        private boolean fading;

        MachineLoopSound(SoundEvent sound, BlockPos pos, float volume, float pitch) {
            super(sound, SoundSource.BLOCKS, RandomSource.create(pos.asLong()));
            this.pos = pos;
            this.target = volume;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.pitch = pitch * (0.97F + (MachineRunState.phaseOffset(pos) % 7) * 0.01F);   // slight per-machine detune
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
        }

        void fadeOut() { fading = true; }

        @Override
        public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) { stop(); return; }
            if (!fading) {
                BlockEntity be = level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
                if (be == null || !isRunning(be)) fading = true;
            }
            if (fading) {
                volume -= target / 8F;
                if (volume <= 0.01F) stop();
            } else if (volume < target) {
                volume = Math.min(target, volume + target / 6F);
            }
        }
    }
}
