package com.miophas.singularity_iteration.common.machine;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.machine.MachineStatus;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p><b>EN:</b> Server side of the front status lamps. It sends {@link MachineStatusPacket} (server to
 * client). Since 0.1.8.1 the client machine loop sounds also use the synced state.
 *
 * <h2>How it works</h2>
 * <ul>
 *   <li>A machine registers itself when it loads and unregisters itself when it is removed
 *       ({@link AbstractEnergyBlockEntity#STATUS_LIFECYCLE}).</li>
 *   <li>Every {@link #INTERVAL} game ticks, the tracker reads the status of each tracked machine.
 *       It collects only the machines whose status changed since the last send.</li>
 *   <li>It sends one packet per chunk with changes, to each player that tracks the chunk.</li>
 *   <li>When a player starts to watch a chunk, the player receives one packet with each lamp of
 *       that chunk that is not {@link MachineStatus#OFF}. The client sets new block entities to
 *       {@code OFF}, so the tracker does not send {@code OFF} lamps.</li>
 * </ul>
 *
 * <h2>Rate rule and why it exists</h2>
 * The tracker sends at most one packet per chunk per {@link #INTERVAL} ticks (0.5 s). It sends
 * only changes. One entry costs 9 bytes (8 for the position, 1 for the status). A vanilla
 * block entity update packet would contain the full update tag. The client re-meshes a block
 * only when its lamp changes. Thus a factory that runs without
 * a change costs no bandwidth and no render work.
 *
 * <h2>Hold rule</h2>
 * A machine that ran in the last {@link #HOLD} ticks, and now reports
 * {@link MachineStatus#BLOCKED}, stays {@link MachineStatus#RUNNING}. Thus a short gap between
 * two operations does not make the lamp flash red. {@link MachineStatus#NO_POWER} and
 * {@link MachineStatus#OFF} show at once.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>{@link Entry#sent} is the last status that the tracker sent for the machine (or
 *       {@code OFF} before the first send). The clients that track the chunk have this
 *       status.</li>
 *   <li>The tracker removes a chunk map when the last machine of the chunk goes away. The
 *       level tick also removes each chunk map that it finds empty.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * All maps are for the server thread only. The lifecycle hook, the level tick and the chunk
 * watch event all run on the server thread.
 *
 * <h2>Memory cleanup</h2>
 * <ul>
 *   <li>Machine removal (break or chunk unload): {@link #lifecycle} removes the entry.</li>
 *   <li>The level tick also removes each machine that {@link AbstractEnergyBlockEntity#isRemoved()}
 *       reports as removed.</li>
 *   <li>Level unload: {@link #levelUnload} removes all data of the level.</li>
 *   <li>Server start and server stop: all data is cleared.</li>
 * </ul>
 *
 * <h2>Missing channel</h2>
 * A vanilla client cannot connect, because the channel is not optional. The send method checks
 * the channel for a single receiver. It ignores send errors from fake or detached connections.
 *
 * <p><b>ZH:</b> 正面状态灯的服务器端，发送 {@link MachineStatusPacket}（服务器到客户端）。自 0.1.8.1 起客户端机器循环音效也使用同步的状态。
 * <b>工作方式：</b>机器加载时注册、移除时注销（{@link AbstractEnergyBlockEntity#STATUS_LIFECYCLE}）；每 {@link #INTERVAL} tick 读取所有被追踪机器的状态，
 * 只收集自上次发送以来变化的机器；每个有变化的区块向追踪该区块的玩家发送一个数据包；玩家开始观察区块时，收到该区块所有非
 * {@link MachineStatus#OFF} 的状态灯（客户端新方块实体默认为 OFF，因此不发送 OFF）。
 * <b>频率规则：</b>每区块每 {@link #INTERVAL} tick（0.5 秒）最多一个数据包，只发送变化；每条 9 字节（位置 8、状态 1）。原版方块实体更新包会包含完整更新标签。
 * 客户端只在状态灯变化时重建方块网格，因此无变化运行的工厂不消耗带宽与渲染。
 * <b>保持规则：</b>最近 {@link #HOLD} tick 内运行过、现在报告 {@link MachineStatus#BLOCKED} 的机器保持 {@link MachineStatus#RUNNING}，
 * 两次操作之间的短暂间隔不会让灯闪红；NO_POWER 与 OFF 立即显示。
 * <b>线程与内存：</b>所有映射只在服务器线程使用；机器移除、区块卸载、世界卸载与服务器启停时清理数据。
 * <b>缺少通道：</b>该通道不是可选的，原版客户端无法连接；发送方法对单个接收者检查通道，并忽略假连接或已断开连接的发送错误。
 *
 * <p><b>JA:</b> 正面ステータスランプのサーバー側。{@link MachineStatusPacket}（サーバーからクライアントへ）を送る。0.1.8.1 以降、クライアントの
 * 機械ループ音も同期された状態を使う。
 * <b>動作：</b>機械は読み込み時に登録し、削除時に登録を解除する（{@link AbstractEnergyBlockEntity#STATUS_LIFECYCLE}）。{@link #INTERVAL} tick ごとに
 * 追跡中の全機械の状態を読み、前回送信から変化した機械だけを集める。変化のあるチャンクごとに、そのチャンクを追跡するプレイヤーへ 1 パケットを送る。
 * プレイヤーがチャンクを見始めると、そのチャンクの {@link MachineStatus#OFF} 以外のランプをまとめて受け取る（クライアントの新しいブロックエンティティは
 * OFF なので、OFF は送らない）。
 * <b>頻度の規則：</b>チャンクごとに {@link #INTERVAL} tick（0.5 秒）に最大 1 パケット、変化だけを送る。1 項目 9 バイト（位置 8、状態 1）。バニラの
 * ブロックエンティティ更新パケットは更新タグ全体を含む。クライアントはランプが変わったときだけ再メッシュするので、変化なく動く工場は帯域も描画も使わない。
 * <b>保持の規則：</b>直近 {@link #HOLD} tick 以内に稼働し、今は {@link MachineStatus#BLOCKED} を報告する機械は {@link MachineStatus#RUNNING} のままにする。
 * 2 つの作業の間の短い空きでランプが赤く点滅しない。NO_POWER と OFF はすぐに表示する。
 * <b>スレッドとメモリ：</b>すべてのマップはサーバースレッド専用。機械の削除、チャンクの解放、レベルの解放、サーバーの起動・停止でデータを消す。
 * <b>チャンネルなし：</b>チャンネルは必須なので、バニラのクライアントは接続できない。送信メソッドは単一の受信者についてチャンネルを確認し、
 * 偽の接続や切断済みの接続の送信エラーは無視する。
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class MachineStatusTracker {
    private MachineStatusTracker() {}

    /**
     * {@code INTERVAL}: game ticks between two status reads (10 ticks = 0.5 s).
     * {@code HOLD}: game ticks for which a machine that ran stays green when it is blocked (30 ticks = 1.5 s).
     */
    public static final int INTERVAL = 10, HOLD = 30;

    /** Per-machine tracker state. */
    private static final class Entry {
        /** Last status sent to the clients. */
        MachineStatus sent = MachineStatus.OFF;
        /** Game time (ticks) of the last {@code RUNNING} read. The start value is far in the past without an overflow risk. */
        long lastRunning = Long.MIN_VALUE / 2;
    }

    /**
     * Level to chunk ({@link ChunkPos#toLong()}) to machine to entry. Server thread only.
     * The level and machine maps use identity keys.
     */
    private static final Map<ServerLevel, Map<Long, Map<AbstractEnergyBlockEntity, Entry>>> TRACKED = new IdentityHashMap<>();

    static {
        install();
    }

    /** Connects the lifecycle hook of {@link AbstractEnergyBlockEntity} to this tracker. */
    static void install() {
        AbstractEnergyBlockEntity.STATUS_LIFECYCLE = MachineStatusTracker::lifecycle;
    }

    /** Sets the hook again and removes data that a previous integrated server left. */
    @SubscribeEvent
    static void serverStarting(ServerStartingEvent event) {
        install();
        TRACKED.clear();
        STALE.clear();
    }

    /** Releases the level, and through it the integrated server, when the level unloads. */
    @SubscribeEvent
    static void levelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) { TRACKED.remove(level); STALE.remove(level); }
    }

    /** Removes all data when the server stops. */
    @SubscribeEvent
    static void serverStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        TRACKED.clear();
        STALE.clear();
    }

    /**
     * Lifecycle hook. {@code loaded = true} from {@code onLoad()} adds the machine.
     * {@code loaded = false} from {@code setRemoved()} removes the machine, and its chunk map
     * when the map is empty. Server side only.
     */
    private static void lifecycle(AbstractEnergyBlockEntity machine, Boolean loaded) {
        if (!(machine.getLevel() instanceof ServerLevel level)) return;
        long chunk = ChunkPos.asLong(machine.getBlockPos());
        if (loaded) {
            TRACKED.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(chunk, c -> new IdentityHashMap<>())
                .putIfAbsent(machine, new Entry());
        } else {
            var chunks = TRACKED.get(level);
            if (chunks == null) return;
            var inChunk = chunks.get(chunk);
            if (inChunk == null) return;
            inChunk.remove(machine);
            if (inChunk.isEmpty()) chunks.remove(chunk);
        }
    }

    /**
     * The status that a player who watches the machine sees, after the hold rule: the last sent
     * status in a watched chunk. 0.1.10.10: the tracker does not read the machines of a chunk that no
     * player watches; for such a machine this returns its current status. Null when the tracker does
     * not track the machine. For tests and tools.
     */
    public static MachineStatus sentStatus(AbstractEnergyBlockEntity machine) {
        if (!(machine.getLevel() instanceof ServerLevel level)) return null;
        var chunks = TRACKED.get(level);
        long key = ChunkPos.asLong(machine.getBlockPos());
        var inChunk = chunks == null ? null : chunks.get(key);
        Entry e = inChunk == null ? null : inChunk.get(machine);
        if (e == null) return null;
        if (!watched(level, key)) {
            try { return machine.machineStatus(); } catch (RuntimeException ex) { return e.sent; }
        }
        return e.sent;
    }

    /**
     * 0.1.10.10 (D2): the tick of the interval at which a chunk is read (0 .. INTERVAL - 1), from its
     * position. The chunks of a level are spread over the interval instead of all at one tick.
     */
    static int phase(long chunk) {
        return Math.floorMod(ChunkPos.getX(chunk) * 31 + ChunkPos.getZ(chunk), INTERVAL);
    }

    /** True when at least one player tracks the chunk. */
    private static boolean watched(ServerLevel level, long chunk) {
        return !level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunk), false).isEmpty();
    }

    /**
     * 0.1.10.10 (D2): chunks whose machines were not read because no player watched them. Their
     * {@link Entry#sent} values are old; the first watcher gets freshly read lamps.
     */
    private static final Map<ServerLevel, it.unimi.dsi.fastutil.longs.LongOpenHashSet> STALE = new IdentityHashMap<>();

    /** Reads the status of one machine with the hold rule and records it as sent; true when it changed. */
    private static boolean read(AbstractEnergyBlockEntity machine, Entry entry, long now) {
        MachineStatus status;
        try {
            status = machine.machineStatus();
        } catch (RuntimeException ex) {
            return false;                                   // A lamp error must never stop a machine tick.
        }
        com.miophas.singularity_iteration.core.runtime.PerfCounters.add(com.miophas.singularity_iteration.core.runtime.PerfCounters.Kind.LAMP_CHECKS);
        // Hold rule: BLOCKED shortly after RUNNING stays RUNNING.
        if (status == MachineStatus.RUNNING) entry.lastRunning = now;
        else if (entry.sent == MachineStatus.RUNNING && status == MachineStatus.BLOCKED && now - entry.lastRunning < HOLD) {
            status = MachineStatus.RUNNING;             // Gap between two operations.
        }
        if (status == entry.sent) return false;
        entry.sent = status;
        return true;
    }

    /** Reused snapshot lists for {@link #levelTick}. Server thread only. They save one allocation per tick. */
    private static final List<Map.Entry<Long, Map<AbstractEnergyBlockEntity, Entry>>> CHUNK_SCRATCH = new ArrayList<>();
    private static final List<Map.Entry<AbstractEnergyBlockEntity, Entry>> MACHINE_SCRATCH = new ArrayList<>();

    /**
     * Reads the status of each tracked machine every {@link #INTERVAL} ticks and sends the changes,
     * one packet per chunk, to the players that track the chunk. 0.1.10.10 (D2): each chunk is read at
     * its own tick of the interval ({@link #phase}), and a chunk that no player watches is not read
     * (it is marked stale; {@link #chunkWatched} reads it for the first watcher).
     */
    @SubscribeEvent
    static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        var chunks = TRACKED.get(level);
        if (chunks == null || chunks.isEmpty()) return;
        int slot = (int) Math.floorMod(now, (long) INTERVAL);
        // Iterate over snapshots in reused scratch lists. A status read can load or unload a
        // machine, and that changes the maps.
        CHUNK_SCRATCH.clear();
        for (var chunkEntry : chunks.entrySet()) if (phase(chunkEntry.getKey()) == slot) CHUNK_SCRATCH.add(chunkEntry);
        for (var chunkEntry : CHUNK_SCRATCH) {
            long key = chunkEntry.getKey();
            if (chunkEntry.getValue().isEmpty()) { chunks.remove(key); continue; }
            if (!watched(level, key)) {
                STALE.computeIfAbsent(level, l -> new it.unimi.dsi.fastutil.longs.LongOpenHashSet()).add(key);
                continue;
            }
            var stale = STALE.get(level);
            if (stale != null) stale.remove(key);
            List<Long> pos = null; List<Byte> st = null;
            MACHINE_SCRATCH.clear();
            MACHINE_SCRATCH.addAll(chunkEntry.getValue().entrySet());
            for (var e : MACHINE_SCRATCH) {
                AbstractEnergyBlockEntity machine = e.getKey();
                if (machine.isRemoved()) { chunkEntry.getValue().remove(machine); continue; }
                // Never load a chunk for a lamp. The redstone check reads the neighbour blocks.
                var p = machine.getBlockPos();
                if (!level.hasChunksAt(p.offset(-1, -1, -1), p.offset(1, 1, 1))) continue;
                if (!read(machine, e.getValue(), now)) continue;
                if (pos == null) { pos = new ArrayList<>(); st = new ArrayList<>(); }
                pos.add(machine.getBlockPos().asLong());
                st.add((byte) e.getValue().sent.ordinal());
            }
            if (pos != null) send(level, new ChunkPos(key), pos, st, null);
            if (chunkEntry.getValue().isEmpty()) chunks.remove(key);
        }
        CHUNK_SCRATCH.clear();
        MACHINE_SCRATCH.clear();
    }

    /**
     * Sends the lamps of a chunk to a player that starts to watch it. NeoForge fires
     * {@code ChunkWatchEvent.Sent} after the chunk data packet, so the client already has the
     * block entities. Only lamps that are not {@code OFF} go out.
     */
    @SubscribeEvent
    static void chunkWatched(ChunkWatchEvent.Sent event) {
        var chunks = TRACKED.get(event.getLevel());
        var inChunk = chunks == null ? null : chunks.get(event.getPos().toLong());
        if (inChunk == null || inChunk.isEmpty()) return;
        // 0.1.10.10 (D2): a chunk that nobody watched has old lamps: read them now (nobody else has them)
        var stale = STALE.get(event.getLevel());
        if (stale != null && stale.remove(event.getPos().toLong())) {
            long now = event.getLevel().getGameTime();
            for (var e : List.copyOf(inChunk.entrySet())) {
                if (e.getKey().isRemoved()) { inChunk.remove(e.getKey()); continue; }
                var p = e.getKey().getBlockPos();
                if (event.getLevel().hasChunksAt(p.offset(-1, -1, -1), p.offset(1, 1, 1))) read(e.getKey(), e.getValue(), now);
            }
        }
        List<Long> pos = new ArrayList<>(); List<Byte> st = new ArrayList<>();
        for (var e : inChunk.entrySet()) {
            if (e.getValue().sent == MachineStatus.OFF) continue;
            pos.add(e.getKey().getBlockPos().asLong());
            st.add((byte) e.getValue().sent.ordinal());
        }
        if (!pos.isEmpty()) send(event.getLevel(), event.getPos(), pos, st, event.getPlayer());
    }

    /**
     * Builds one {@link MachineStatusPacket} and sends it.
     *
     * @param level the level of the chunk
     * @param chunk the chunk of all positions
     * @param pos   packed block positions ({@code BlockPos.asLong()})
     * @param st    status ordinals, same index as {@code pos}
     * @param only  a single receiver, or null for each player that tracks the chunk
     */
    private static void send(ServerLevel level, ChunkPos chunk, List<Long> pos, List<Byte> st, net.minecraft.server.level.ServerPlayer only) {
        // Split into packets of at most MAX_ENTRIES (the decoder rejects a larger count).
        int total = Math.min(pos.size(), st.size());
        for (int from = 0; from < total; from += MachineStatusPacket.MAX_ENTRIES) {
            int len = Math.min(MachineStatusPacket.MAX_ENTRIES, total - from);
            long[] p = new long[len]; byte[] s = new byte[len];
            for (int i = 0; i < len; i++) { p[i] = pos.get(from + i); s[i] = st.get(from + i); }
            sendOne(level, chunk, new MachineStatusPacket(p, s), only);
        }
    }

    private static void sendOne(ServerLevel level, ChunkPos chunk, MachineStatusPacket packet, net.minecraft.server.level.ServerPlayer only) {
        try {
            if (only != null) {
                if (only.connection != null && only.connection.hasChannel(MachineStatusPacket.TYPE)) PacketDistributor.sendToPlayer(only, packet);
            } else {
                PacketDistributor.sendToPlayersTrackingChunk(level, chunk, packet);
            }
        } catch (RuntimeException ignored) {
            // A fake or detached connection can throw. The lamp state is not important for it.
        }
    }
}
