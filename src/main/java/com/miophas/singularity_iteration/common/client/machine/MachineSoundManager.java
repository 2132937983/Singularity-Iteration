package com.miophas.singularity_iteration.common.client.machine;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Running-machine sound loops, entirely client side.
 *
 * <p>The running state comes from the block state the server already syncs (lit/active), so
 * there is no network traffic and no server tick cost (the old design broadcast a work sound
 * from every machine every 40 ticks). Every {@link #SCAN_INTERVAL} ticks the block entities of
 * the 3x3 chunks around the player are scanned; the nearest {@link #MAX_LOOPS} running machines
 * within {@link #RADIUS} blocks get a looping, fading sound instance, everything else is
 * silent. Big factories therefore cost one short scan every half second and at most a handful
 * of sound channels. Press and crusher loops start on the machine's animation phase so the
 * impacts line up with the moving parts.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class MachineSoundManager {
    public static final int SCAN_INTERVAL = 10;
    public static final int MAX_LOOPS = 6;
    public static final double RADIUS = 16.0;
    public static final int MAX_PER_SOUND = 2;

    /** Loop sound and volume per machine (registry path). Machines not listed are silent. */
    record Profile(Supplier<SoundEvent> sound, float volume, boolean phased) {}
    private static final Map<String, Profile> BY_PATH = new HashMap<>();
    private static final Map<Block, Profile> CACHE = new IdentityHashMap<>();
    private static final Profile SILENT = new Profile(() -> null, 0, false);

    static {
        Profile motor = p(mio_icif_sounds.LOOP_MOTOR, 0.55F), press = new Profile(mio_icif_sounds.LOOP_PRESS::get, 0.6F, true),
            crusher = new Profile(mio_icif_sounds.LOOP_CRUSHER::get, 0.6F, true), centrifuge = p(mio_icif_sounds.LOOP_CENTRIFUGE, 0.5F),
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
        map(electric, "producer/block_matter_elc", "producer/block_replicator_elc", "producer/block_teleporter_elc",
            "producer/block_molecular_transformer", "producer/block_neutron_polymerizer", "producer/block_chunk_loader",
            "producer/block_terra_elc", "producer/block_tesla", "generator/block_quantum_generator", "generator/block_geomagnetic_generator",
            "wiring/block_wireless_power_transmission_node");
        map(hum, "wiring/transformer_lv_mv", "wiring/transformer_mv_hv", "wiring/transformer_hv_ev", "wiring/transformer_ev_sc",
            "wiring/transformer_iv_luv", "wiring/transformer_luv_zpmv");
        map(generator, "generator/block_geo_generator", "generator/block_semifluid_generator", "generator/block_advanced_semifluid_generator",
            "generator/block_diesel_generator", "generator/block_drop_generator", "generator/block_advanced_drop_generator",
            "generator/block_experience_generator", "generator/block_advanced_experience_generator", "hugenerator/block_solid_heat_generator");
        map(turbine, "generator/block_kinetic_generator", "generator/block_turbo_kinetic_generator", "generator/block_twin_turbo_kinetic_generator",
            "generator/block_stirling_generator", "generator/block_advanced_stirling_generator", "kugenerator/block_stirling_kinetic_generator",
            "producer/block_steam_kinetic_generator");
        map(reactor, "generator/block_nuclear_reactor_generator", "generator/block_rt_generator", "hugenerator/block_rt_heat_generator");
        map(drill, "producer/block_miner_elc", "producer/block_advanced_miner_elc", "oilrig/block_oil_rig_panel");
        map(motor, "producer/block_batch_crafter");
    }

    private static Profile p(Supplier<SoundEvent> s, float v) { return new Profile(s, v, false); }
    private static void map(Profile p, String... paths) { for (String s : paths) BY_PATH.put(s, p); }

    static Profile profile(Block block) {
        return CACHE.computeIfAbsent(block, b -> {
            var key = BuiltInRegistries.BLOCK.getKey(b);
            if (!key.getNamespace().equals(Singularity_Iteration.MOD_ID)) return SILENT;
            return BY_PATH.getOrDefault(key.getPath(), SILENT);
        });
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
        if (!PENDING.isEmpty()) startPhased(mc, level);
        if (++tick % SCAN_INTERVAL != 0) return;
        long t0 = System.nanoTime();
        scan(mc, level);
        lastScanNanos = System.nanoTime() - t0;
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
                    if (prof == SILENT || !MachineRunState.isRunning(be.getBlockState())) continue;
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
        MachineLoopSound s = new MachineLoopSound(sound, pos, prof.volume);
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

        MachineLoopSound(SoundEvent sound, BlockPos pos, float volume) {
            super(sound, SoundSource.BLOCKS, RandomSource.create(pos.asLong()));
            this.pos = pos;
            this.target = volume;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.pitch = 0.97F + (MachineRunState.phaseOffset(pos) % 7) * 0.01F;   // slight per-machine detune
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
            if (!fading && (!level.isLoaded(pos) || !MachineRunState.isRunning(level.getBlockState(pos)))) fading = true;
            if (fading) {
                volume -= target / 8F;
                if (volume <= 0.01F) stop();
            } else if (volume < target) {
                volume = Math.min(target, volume + target / 6F);
            }
        }
    }
}
