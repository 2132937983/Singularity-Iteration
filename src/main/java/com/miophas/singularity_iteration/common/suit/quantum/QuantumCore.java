// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.suit.SyncHash;

import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.core.api.suit.SuitQuantumCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Quantum core rules (0.1.10.0): the coherence of each wearer, the decoherence lock, the running
 * actions and the state sync to the wearer.
 *
 * <p>Coherence: 0..maximum (100, +{@link QuantumConfig#capacitorBonus()} with a coherence
 * capacitor). It regains {@link QuantumConfig#coherenceRegen()} points per second (+25 % with the
 * capacitor), {@link QuantumConfig#regenDelay()} ticks after the last action, while a worn quantum
 * piece carries an action unit or the capacitor; each point costs {@link QuantumConfig#euPerCoherence()}
 * EU from the suit energy pool. No regeneration while a toggle action runs or during the lock.
 *
 * <p>Decoherence: when an action or a toggle drain takes the coherence to 0, the core decoheres for
 * {@link QuantumConfig#decoherenceTicks()} ticks: actions are refused, running ones fall, the HUD
 * glitches and the sensors refresh at half rate. Then the coherence regains as usual.
 */
public final class QuantumCore {
    private QuantumCore() {}

    public static final float BASE_MAX = 100;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static boolean bootstrapped;
    /** Client side of the public view: the last synced state of the local player (set by the client set-up). */
    public static volatile java.util.function.Function<Player, float[]> CLIENT_VIEW;

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        QuantumLinks.bootstrap();                          // 0.1.10.1 synergy query of the public API
        QuantumBandwidth.bootstrap();                      // 0.1.10.1 bandwidth view of the public API
        SuitQuantumCore.setView(new SuitQuantumCore.View() {
            @Override public float coherence(Player player) { float[] v = view(player); return v == null ? 0 : v[0]; }
            @Override public float coherenceMax(Player player) { float[] v = view(player); return v == null ? BASE_MAX : v[1]; }
            @Override public boolean decoherent(Player player) { float[] v = view(player); return v != null && v[2] > 0; }
            @Nullable
            private float[] view(Player player) {
                if (player.level().isClientSide) return CLIENT_VIEW == null ? null : CLIENT_VIEW.apply(player);
                QuantumCoreState s = player.getExistingDataOrNull(SuitRegistry.QUANTUM_CORE);
                return s == null ? null : new float[]{s.coherence, max(player), s.decoherentTicks};
            }
        });
    }

    // ------------------------------------------------------------------ queries

    /** The quantum core of a player (created on first use; server side). */
    public static QuantumCoreState state(Player player) {
        return player.getData(SuitRegistry.QUANTUM_CORE);
    }

    /** True while the player is out of phase (server: exact; client: the local player's sync is not used here). */
    public static boolean phased(Player player) {
        QuantumCoreState s = player.getExistingDataOrNull(SuitRegistry.QUANTUM_CORE);
        return s != null && s.phased();
    }

    /** True when a capacitor works on a worn quantum piece. */
    public static boolean capacitor(Player player) {
        if (!QuantumConfig.enabled("coherence_capacitor")) return false;
        ItemStack piece = SuitModules.wornPieceWith(player, SuitModuleType.COHERENCE_CAPACITOR);
        return !piece.isEmpty() && SuitModules.isQuantumPiece(piece) && SuitModules.isActive(player, SuitModuleType.COHERENCE_CAPACITOR);
    }

    /** Coherence maximum: 100, plus the capacitor. */
    public static float max(Player player) {
        return BASE_MAX + (capacitor(player) ? QuantumConfig.capacitorBonus() : 0);
    }

    /**
     * The worn quantum piece that pays the coherence (the chestplate first), or null when the core
     * is offline: no worn quantum piece carries an action unit or a capacitor.
     */
    @Nullable
    public static ItemStack corePiece(Player player) {
        if (QuantumActions.available(player).isEmpty() && !capacitor(player)) return null;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = player.getItemBySlot(slot);
            if (SuitModules.isQuantumPiece(s)) return s;
        }
        return null;
    }

    // ------------------------------------------------------------------ coherence (server)

    /** Pays {@code amount} coherence for an action; 0 or less decoheres the core. */
    /** Pays {@code amount} coherence (0.1.10.2: public for the quantum recall of the command wheel). */
    public static void spend(ServerPlayer player, QuantumCoreState s, float amount) {
        s.lastAction = player.level().getGameTime();
        s.coherence = Math.max(0, s.coherence - amount);
        if (s.coherence <= 0.001F) decohere(player, s);
        s.sentHash = 0;
    }

    /** Drains a toggle action for one tick. False when the coherence ran out (the core decohered). */
    static boolean drain(ServerPlayer player, QuantumCoreState s, float amount) {
        s.lastAction = player.level().getGameTime();
        s.coherence = Math.max(0, s.coherence - amount);
        if (s.coherence > 0.001F) return true;
        decohere(player, s);
        return false;
    }

    /** The coherence collapses: actions lock, running ones fall. */
    static void decohere(ServerPlayer player, QuantumCoreState s) {
        s.coherence = 0;
        int ticks = QuantumConfig.decoherenceTicks();
        s.decoherentTicks = s.decoherentMax = ticks;
        if (s.fieldOn) QuantumField.stop(player, s, 2);
        if (!s.addonKey.isEmpty()) QuantumActions.endAddon(player, s, false);
        s.sentHash = 0;
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_DECOHERE, ticks, 0, 0), 64);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.DECOHERENCE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    // ------------------------------------------------------------------ server tick

    /** Called from {@code SuitServer.onPlayerTick} for every server player. */
    public static void tick(ServerPlayer player) {
        QuantumCoreState existing = player.getExistingDataOrNull(SuitRegistry.QUANTUM_CORE);
        ItemStack core = existing == null && !wearsQuantum(player) ? null : corePiece(player);
        if (core == null && existing == null) return;
        QuantumCoreState s = existing != null ? existing : state(player);
        boolean online = core != null;
        if (!online && s.idle() && s.sentOffline) return;
        float max = max(player);
        if (s.coherence > max) s.coherence = max;               // a capacitor came off
        if (s.decoherentTicks > 0 && --s.decoherentTicks == 0) {
            s.sentHash = 0;
            SuitPackets.send(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_RECOHERE, 0, 0, 0));
        }
        if (s.phaseTicks > 0 && --s.phaseTicks == 0) QuantumActions.endPhase(player, s);
        if (s.blinkSafeTicks > 0) {
            s.blinkSafeTicks--;
            player.resetFallDistance();
        }
        if (s.fieldOn) QuantumField.tick(player, s);
        else if (!s.frozen.isEmpty()) QuantumField.release(player, s);
        if (s.dilationTicks > 0) QuantumDilation.tick(player, s);
        else if (!s.slowed.isEmpty() || !s.projectileSpeed.isEmpty() || s.realSlow) QuantumDilation.end(player, s, false);
        if (!s.addonKey.isEmpty()) QuantumActions.tickAddon(player, s);
        int blinkMax = blinkCharges(player, s);   // 0.1.10.8: once per tick (also for the sync)
        QuantumSynergies.tickDecoy(player, s);             // 0.1.10.1 Afterimage Decoy
        if (online || s.markedTarget != -1) QuantumSynergies.tickDatalink(player, s);   // 0.1.10.1 Fleet Datalink
        regen(player, s, core, max);
        sync(player, s, online, max, blinkMax);
    }

    private static boolean wearsQuantum(Player player) {
        for (EquipmentSlot slot : ARMOR) if (SuitModules.isQuantumPiece(player.getItemBySlot(slot))) return true;
        return false;
    }

    /** Recharges the blink charges; returns the maximum charges of the worn phase jump drive. */
    private static int blinkCharges(ServerPlayer player, QuantumCoreState s) {
        int max = QuantumActions.blinkMaxCharges(player);
        if (max == 0) {
            s.blinkCharges = -1;
            s.blinkRecharge = 0;
            return max;
        }
        if (s.blinkCharges < 0 || s.blinkCharges > max) s.blinkCharges = max;
        if (s.blinkCharges >= max) {
            s.blinkRecharge = 0;
            return max;
        }
        if (s.blinkRecharge <= 0) s.blinkRecharge = Math.max(1, QuantumConfig.blinkCooldown());
        if (--s.blinkRecharge <= 0) {
            s.blinkCharges++;
            s.blinkRecharge = s.blinkCharges < max ? Math.max(1, QuantumConfig.blinkCooldown()) : 0;
            s.sentHash = 0;
        }
        return max;
    }

    private static void regen(ServerPlayer player, QuantumCoreState s, @Nullable ItemStack core, float max) {
        if (core == null || s.decoherentTicks > 0 || s.sustaining() || s.coherence >= max) return;
        if (player.level().getGameTime() - s.lastAction < QuantumConfig.regenDelay() || player.tickCount % 5 != 0) return;
        // 0.1.10.1: a saturated suit computer (load above the bandwidth budget) regains at half speed
        double rate = QuantumConfig.coherenceRegen() * (capacitor(player) ? 1.25 : 1.0) * (QuantumBandwidth.saturated(player) ? 0.5 : 1.0);
        float points = (float) Math.min(max - s.coherence, rate / 4.0);
        long cost = (long) Math.ceil(points * QuantumConfig.euPerCoherence());
        if (points <= 0 || cost > 0 && !SuitModules.canPay(player, core, cost)) return;
        if (cost > 0) SuitModules.extract(player, core, cost);
        s.coherence = Math.min(max, s.coherence + points);
    }

    /** True while the coherence regains (HUD). */
    private static boolean regenerating(ServerPlayer player, QuantumCoreState s, boolean online, float max) {
        return online && s.decoherentTicks <= 0 && !s.sustaining() && s.coherence < max
            && player.level().getGameTime() - s.lastAction >= QuantumConfig.regenDelay();
    }

    // ------------------------------------------------------------------ sync

    static void sync(ServerPlayer player, QuantumCoreState s, boolean online, float max, int blinkMax) {
        if (++s.syncTimer < 2) return;
        boolean regen = regenerating(player, s, online, max);
        // 0.1.10.8: the values of Objects.hash(...) without the array and the boxing
        int hash = SyncHash.START;
        hash = SyncHash.add(hash, online);
        hash = SyncHash.add(hash, regen);
        hash = SyncHash.add(hash, s.decoherentTicks > 0);
        hash = SyncHash.add(hash, s.phaseTicks > 0);
        hash = SyncHash.add(hash, s.fieldOn);
        hash = SyncHash.add(hash, s.dilationTicks > 0);
        hash = SyncHash.add(hash, s.blinkCharges);
        hash = SyncHash.add(hash, blinkMax);
        hash = SyncHash.add(hash, max);
        boolean running = s.phaseTicks > 0 || s.fieldOn || s.dilationTicks > 0 || s.decoherentTicks > 0 || s.blinkRecharge > 0;
        boolean changed = hash != s.sentHash || Math.abs(s.coherence - s.sentCoherence) >= 0.5F || running;
        if (!changed && s.syncTimer < 20) return;
        s.syncTimer = 0;
        s.sentHash = hash;
        s.sentCoherence = s.coherence;
        s.sentOffline = !online;
        SuitPackets.send(player, new QuantumPackets.CoreSync(s.coherence, max, s.decoherentTicks, Math.max(1, s.decoherentMax), online, regen,
            s.phaseTicks, Math.max(1, s.phaseMax), s.fieldOn, s.fieldTicks, s.dilationTicks, Math.max(1, s.dilationMax),
            Math.max(0, s.blinkCharges), blinkMax, s.blinkRecharge, Math.max(1, QuantumConfig.blinkCooldown())));
    }
}
