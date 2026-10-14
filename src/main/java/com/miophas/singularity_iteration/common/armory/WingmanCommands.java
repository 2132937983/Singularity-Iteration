// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.common.suit.nano.NanoCoreState;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.common.suit.nano.NanoSuit;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumActions;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumConfig;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumCore;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumCoreState;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumPackets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Quantum command, server side (0.1.10.2): the orders of the command wheel (key G) and the holo
 * table, the data link setting, the quantum recall and the squad view sent to the owner.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The command link needs an active entanglement relay on a worn quantum piece. Without it
 *       every order is refused with a message.</li>
 *   <li>Orders reach the owner's loaded Wingmen in the owner's dimension, at any distance
 *       ({@link WingmanOrder}). A Wingman in an unloaded chunk cannot move; it is listed as
 *       unloaded and only the quantum recall reaches it.</li>
 *   <li>A mode for one Wingman is individual: the squad mode (panel or wheel, all Wingmen) no
 *       longer changes it until "follow squad".</li>
 *   <li>Quantum recall: every Wingman of the owner, in any dimension, loaded or not, goes back to
 *       its Armory at once (the suit items return as in a normal return). It costs coherence and
 *       has a cooldown. An unloaded Wingman's chunk loads for up to 15 s with a ticket; the suit is
 *       sent home as soon as the entity appears.</li>
 *   <li>Every order posts {@code ArmoryEvent.WingmanCommand} first; a canceled event refuses it.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class WingmanCommands {
    private WingmanCommands() {}

    public static final byte A_MOVE = 0, A_HOLD = 1, A_SUPPRESS = 2, A_FOCUS = 3, A_FORM_UP = 4, A_MODE = 5, A_SQUAD = 6,
        A_VOLLEY = 7, A_DATALINK = 8, A_RETURN = 9, A_QUANTUM_RECALL = 10;
    /** Cooldown key of the quantum recall in the nano core cooldown map. */
    public static final String RECALL_KEY = "mio_icif:quantum_recall";
    /** Ticks of the chunk ticket of an unloaded Wingman's recall. */
    static final int RECALL_TICKET_TICKS = 300;
    static final TicketType<ChunkPos> RECALL_TICKET = TicketType.create("mio_icif_wingman_recall", Comparator.comparingLong(ChunkPos::toLong),
        RECALL_TICKET_TICKS);

    private record Pending(ResourceKey<Level> dimension, UUID owner, long deadline) { }
    /** Recalls that wait for an unloaded Wingman to appear, by Wingman UUID. Server thread only. */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    // ------------------------------------------------------------------ queries

    /** True when the command link works: an active entanglement relay on a worn quantum piece. */
    public static boolean commander(ServerPlayer player) {
        var piece = SuitModules.wornPieceWith(player, SuitModuleType.ENTANGLEMENT_RELAY);
        return !piece.isEmpty() && SuitModules.isQuantumPiece(piece) && SuitModules.isActive(player, SuitModuleType.ENTANGLEMENT_RELAY);
    }

    /** True when the data link works (the command link and the data link setting). */
    public static boolean datalink(ServerPlayer player) {
        return commander(player) && player.getData(SuitRegistry.WINGMAN_COMMAND).datalink;
    }

    // ------------------------------------------------------------------ orders

    /** Handles one command of the owner (rate limited by the payload handler). */
    static void handle(ServerPlayer player, WingmanCommandPackets.Command c) {
        if (!player.isAlive() || player.isSpectator()) return;
        if (!commander(player)) {
            refuse(player, "message.mio_icif.command.no_relay");
            return;
        }
        WingmanCommandState state = player.getData(SuitRegistry.WINGMAN_COMMAND);
        switch (c.action()) {
            case A_VOLLEY -> {
                QuantumActions.request(player, NanoSuit.state(player), UnitRef.of(SuitModuleType.ENTANGLEMENT_RELAY), Vec3.ZERO);
                return;
            }
            case A_DATALINK -> {
                state.datalink = c.arg() != 0;
                player.displayClientMessage(Component.translatable(state.datalink ? "message.mio_icif.command.datalink_on"
                    : "message.mio_icif.command.datalink_off"), true);
                player.playNotifySound(mio_icif_sounds.ORDER_ACK.get(), SoundSource.PLAYERS, 0.6F, state.datalink ? 1.2F : 0.8F);
                state.syncTimer = 100;
                return;
            }
            case A_QUANTUM_RECALL -> {
                quantumRecall(player);
                return;
            }
            default -> { }
        }
        List<WingmanEntity> units = selection(player, c.unit());
        if (units.isEmpty()) {
            refuse(player, "message.mio_icif.command.no_unit");
            return;
        }
        Vec3 point = null;
        LivingEntity focus = null;
        String name;
        switch (c.action()) {
            case A_MOVE, A_SUPPRESS -> {
                point = point(player, c.x(), c.y(), c.z());
                if (point == null) { refuse(player, "message.mio_icif.command.bad_point"); return; }
                name = c.action() == A_MOVE ? "MOVE" : "SUPPRESS";
            }
            case A_HOLD -> {
                if (c.arg() != 1) {
                    point = point(player, c.x(), c.y(), c.z());
                    if (point == null) { refuse(player, "message.mio_icif.command.bad_point"); return; }
                }
                name = "HOLD";
            }
            case A_FOCUS -> {
                Entity e = player.serverLevel().getEntity(c.target());
                if (!(e instanceof LivingEntity living) || !units.stream().anyMatch(w -> w.focusValid(living, player))) {
                    refuse(player, "message.mio_icif.command.bad_target");
                    return;
                }
                focus = living;
                name = "FOCUS";
            }
            case A_FORM_UP -> name = "FORM_UP";
            case A_MODE -> {
                if (c.arg() < 0 || c.arg() >= WingmanMode.VALUES.length) return;
                name = "MODE";
            }
            case A_SQUAD -> name = "SQUAD";
            case A_RETURN -> name = "RETURN";
            default -> { return; }
        }
        if (NeoForge.EVENT_BUS.post(new com.miophas.singularity_iteration.core.api.armory.ArmoryEvent.WingmanCommand(player, name, units.size(),
                point, focus)).isCanceled()) {
            refuse(player, "message.mio_icif.armory.cancelled");
            return;
        }
        boolean all = WingmanCommandPackets.ALL.equals(c.unit());
        int done = 0;
        for (WingmanEntity w : units) {
            switch (c.action()) {
                case A_MOVE -> w.order(WingmanOrder.MOVE, point, null);
                case A_HOLD -> w.order(WingmanOrder.HOLD, point, null);
                case A_SUPPRESS -> w.order(WingmanOrder.SUPPRESS, point, null);
                case A_FOCUS -> {
                    if (!w.focusValid(focus, player)) continue;
                    w.order(WingmanOrder.FOCUS, null, focus);
                }
                case A_FORM_UP -> w.clearOrder();
                case A_MODE -> {
                    WingmanMode mode = WingmanMode.byId(c.arg());
                    w.setMode(mode, !all);
                }
                case A_SQUAD -> {
                    WingmanMode squad = state.squadMode >= 0 ? WingmanMode.byId(state.squadMode) : w.mode();
                    w.setMode(squad, false);
                }
                case A_RETURN -> w.startReturn(null);
                default -> { }
            }
            done++;
        }
        if (c.action() == A_MODE && all) state.squadMode = c.arg();
        // feedback: threads to the Wingmen and the marker on the owner's client, an acknowledgement chirp
        int[] ids = units.stream().mapToInt(Entity::getId).toArray();
        Vec3 at = point != null ? point : focus != null ? focus.position() : player.position();
        SuitPackets.send(player, new NanoPackets.Fx(player.getId(), QuantumPackets.FX_ORDER, (float) at.x, (float) at.y, (float) at.z,
            c.action(), c.arg(), focus == null ? -1 : focus.getId(), ids, new float[0]));
        player.playNotifySound(mio_icif_sounds.ORDER_ACK.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        Component what = Component.translatable("gui.mio_icif.command.action." + c.action());
        player.displayClientMessage(Component.translatable("message.mio_icif.command.done", what, done), true);
        state.syncTimer = 100;                                         // send the squad view at once
    }

    /** The owner's loaded Wingmen in the owner's dimension that can take an order: all, or the one with the UUID. */
    static List<WingmanEntity> selection(ServerPlayer player, UUID unit) {
        List<WingmanEntity> out = new ArrayList<>();
        for (WingmanEntity w : WingmanService.of(player)) {
            if (w.level() != player.level() || w.state() == WingmanEntity.RETURNING) continue;
            if (WingmanCommandPackets.ALL.equals(unit) || w.getUUID().equals(unit)) out.add(w);
        }
        return out;
    }

    /** A checked order point in the owner's dimension (finite, in the world height and the world border), or null. */
    @Nullable
    static Vec3 point(ServerPlayer player, double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
        ServerLevel level = player.serverLevel();
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) return null;
        return new Vec3(x, y, z);
    }

    private static void refuse(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        player.playNotifySound(mio_icif_sounds.NANO_DENIED.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    // ------------------------------------------------------------------ quantum recall

    /** Every Wingman of the owner returns at once (see the class comment). Returns the number of suits reached. */
    public static int quantumRecall(ServerPlayer player) {
        NanoCoreState keys = NanoSuit.state(player);
        if (keys.cooldown(RECALL_KEY) > 0) {
            refuse(player, "message.mio_icif.command.recall_cooldown");
            return 0;
        }
        QuantumCoreState s = QuantumCore.state(player);
        int cost = QuantumConfig.recallCost();
        if (s.decoherentTicks > 0 || s.coherence < cost) {
            refuse(player, "message.mio_icif.command.recall_coherence");
            return 0;
        }
        List<WingmanEntity> loaded = WingmanService.of(player);
        WingmanRoster roster = WingmanRoster.get(player.server);
        List<WingmanRoster.Entry> far = roster == null || !QuantumConfig.unloadedRecall() ? List.of() : roster.of(player.getUUID());
        if (loaded.isEmpty() && far.isEmpty()) {
            refuse(player, "message.mio_icif.wingman.none");
            return 0;
        }
        if (NeoForge.EVENT_BUS.post(new com.miophas.singularity_iteration.core.api.armory.ArmoryEvent.WingmanCommand(player, "QUANTUM_RECALL",
                loaded.size() + far.size(), null, null)).isCanceled()) {
            refuse(player, "message.mio_icif.armory.cancelled");
            return 0;
        }
        for (WingmanEntity w : loaded) dematerialize(w);
        int requested = 0;
        for (WingmanRoster.Entry e : far) {
            ServerLevel level = player.server.getLevel(e.dimension());
            if (level == null) continue;
            ChunkPos chunk = new ChunkPos(BlockPos.containing(e.pos()));
            level.getChunkSource().addRegionTicket(RECALL_TICKET, chunk, 2, chunk);
            PENDING.put(e.wingman(), new Pending(e.dimension(), player.getUUID(), player.server.getTickCount() + RECALL_TICKET_TICKS / 2));
            requested++;
        }
        QuantumCore.spend(player, s, cost);
        keys.cooldown.put(RECALL_KEY, QuantumConfig.recallCooldown());
        keys.dirty();
        player.playNotifySound(mio_icif_sounds.QUANTUM_RECALL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.mio_icif.command.recall", loaded.size() + requested, requested), true);
        player.getData(SuitRegistry.WINGMAN_COMMAND).syncTimer = 100;
        return loaded.size() + requested;
    }

    /** The suit of {@code w} folds into the Armory: the effect for the players near, then the items go home. */
    static void dematerialize(WingmanEntity w) {
        NanoPackets.broadcast(w, new NanoPackets.Fx(w.getId(), QuantumPackets.FX_RECALL, (float) w.getX(), (float) w.getY(), (float) w.getZ(),
            w.getBbHeight(), 0, 0, new int[0], new float[0]), 128);
        w.level().playSound(null, w.getX(), w.getY(), w.getZ(), mio_icif_sounds.QUANTUM_RECALL.get(), SoundSource.NEUTRAL, 1.0F, 1.1F);
        w.secure();
        w.discard();
    }

    /** Recalls of unloaded Wingmen: send each home when its chunk has loaded it; give up after the deadline. */
    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        MinecraftServer server = event.getServer();
        for (Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Pending p = e.getValue();
            ServerLevel level = server.getLevel(p.dimension());
            Entity entity = level == null ? null : level.getEntity(e.getKey());
            if (entity instanceof WingmanEntity w && !w.isRemoved()) {
                dematerialize(w);
                it.remove();
            } else if (level == null || server.getTickCount() > p.deadline()) {
                it.remove();
                ServerPlayer owner = server.getPlayerList().getPlayer(p.owner());
                if (owner != null) owner.displayClientMessage(Component.translatable("message.mio_icif.command.recall_lost"), true);
                // the entity never appeared: it no longer exists (removed while unloaded); forget it
                WingmanRoster roster = WingmanRoster.get(server);
                if (roster != null && entity == null) roster.remove(e.getKey());
            }
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    // ------------------------------------------------------------------ squad view

    /** Every 10 ticks while the command link works: the squad view to the owner; once (empty) when it stops. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WingmanCommandState st = player.getData(SuitRegistry.WINGMAN_COMMAND);
        boolean commander = commander(player);
        if (!commander) {
            if (st.sentActive) {
                SuitPackets.send(player, new WingmanCommandPackets.Squad(false, st.datalink, (byte) st.squadMode, 0, 0, 0, List.of()));
                st.sentActive = false;
            }
            return;
        }
        if (++st.syncTimer < 10) return;
        st.syncTimer = 0;
        // always sent while the link works (a few bytes without Wingmen): the client takes a silent link as lost
        st.sentActive = true;
        SuitPackets.send(player, squad(player, st));
    }

    /** The squad view of {@code player}: the loaded Wingmen first (in squad order), then the unloaded ones of the roster. */
    public static WingmanCommandPackets.Squad squad(ServerPlayer player, WingmanCommandState st) {
        List<WingmanCommandPackets.Unit> units = new ArrayList<>();
        for (WingmanEntity w : WingmanService.of(player)) {
            boolean here = w.level() == player.level();
            Vec3 o = w.orderPos();
            units.add(new WingmanCommandPackets.Unit(w.getUUID(), here ? w.getId() : -1, (byte) w.set(), (byte) w.mode().ordinal(), w.individual(),
                (byte) w.order().ordinal(), o == null ? 0 : (float) o.x, o == null ? 0 : (float) o.y, o == null ? 0 : (float) o.z,
                w.orderTargetId(), w.getHealth() / Math.max(1, w.getMaxHealth()), w.energyFraction(), w.state(), (byte) (here ? 0 : 1),
                (float) w.getX(), (float) w.getY(), (float) w.getZ()));
        }
        WingmanRoster roster = WingmanRoster.get(player.server);
        if (roster != null) {
            for (WingmanRoster.Entry e : roster.of(player.getUUID())) {
                if (units.size() >= WingmanCommandPackets.Squad.MAX_UNITS) break;
                units.add(new WingmanCommandPackets.Unit(e.wingman(), -1, (byte) e.set(), (byte) -1, false, (byte) 0, 0, 0, 0, -1, 1, -1,
                    WingmanEntity.ACTIVE, (byte) 2, (float) e.pos().x, (float) e.pos().y, (float) e.pos().z));
            }
        }
        int cooldown = NanoSuit.state(player).cooldown(RECALL_KEY);
        return new WingmanCommandPackets.Squad(true, st.datalink, (byte) st.squadMode, cooldown, QuantumConfig.holdRadius(),
            QuantumConfig.suppressRadius(), units);
    }
}
