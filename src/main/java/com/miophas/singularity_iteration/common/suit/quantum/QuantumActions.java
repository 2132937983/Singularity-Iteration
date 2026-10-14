// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.common.suit.nano.NanoBursts;
import com.miophas.singularity_iteration.common.suit.nano.NanoCoreState;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.core.api.suit.SuitActionSpec;
import com.miophas.singularity_iteration.core.api.suit.SuitClass;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry;
import com.miophas.singularity_iteration.core.api.suit.SuitQuantumActions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Quantum actions (0.1.10.0): the five built-in actions, addon actions of the public API, and the
 * key request path. The action keys are the nano burst keys (V, B): {@code NanoBursts.request}
 * hands a quantum action unit over to {@link #request}. Each action needs its unit on a worn
 * quantum piece (the deflector and the behaviour predictor as Mk.II) and is paid with coherence.
 *
 * <table>
 *   <caption>Built-in actions (default numbers, server config section QuantumSuit)</caption>
 *   <tr><th>Action</th><th>Unit</th><th>Coherence</th><th>Cooldown</th></tr>
 *   <tr><td>Quantum Blink</td><td>phase jump drive</td><td>25</td><td>2.5 s per charge (Mk.II: two charges)</td></tr>
 *   <tr><td>Phase Shift</td><td>phase modulator</td><td>35</td><td>8 s</td></tr>
 *   <tr><td>Force Field (toggle)</td><td>deflector Mk.II</td><td>15 + 10/s</td><td>6 s after it falls</td></tr>
 *   <tr><td>Time Dilation</td><td>behaviour predictor Mk.II</td><td>60</td><td>25 s</td></tr>
 *   <tr><td>Entangled Volley</td><td>entanglement relay</td><td>20</td><td>10 s</td></tr>
 * </table>
 */
public final class QuantumActions {
    private QuantumActions() {}

    /** Refusal reasons beyond the nano ones (sent in the nano DENIED effect). */
    public static final int DENY_DECOHERENT = 6, DENY_COHERENCE = 7, DENY_BLOCKED = 8, DENY_NO_TARGET = 9, DENY_NO_WINGMAN = 10;
    /** Ticks between two blinks while a charge is left. */
    static final int BLINK_GAP = 6;

    /** The built-in actions in key priority order. */
    public enum Kind {
        BLINK("blink", SuitModuleType.PHASE_JUMP_DRIVE, 1),
        PHASE("phase_shift", SuitModuleType.PHASE_MODULATOR, 1),
        FIELD("force_field", SuitModuleType.DEFLECTOR, 2),
        DILATION("time_dilation", SuitModuleType.BEHAVIOR_PREDICTOR, 2),
        VOLLEY("volley", SuitModuleType.ENTANGLEMENT_RELAY, 1);

        public final String id;
        public final SuitModuleType unit;
        /** 1, or 2 for the actions that the Mk.II of an older unit unlocks. */
        public final int minTier;
        public static final Kind[] VALUES = values();

        Kind(String id, SuitModuleType unit, int minTier) {
            this.id = id;
            this.unit = unit;
            this.minTier = minTier;
        }

        public String nameKey() { return "quantum.mio_icif.action." + id; }
    }

    // ------------------------------------------------------------------ queries (both sides)

    @Nullable
    public static Kind kind(@Nullable UnitRef unit) {
        if (unit == null || unit.builtin() == null) return null;
        for (Kind k : Kind.VALUES) if (k.unit == unit.builtin()) return k;
        return null;
    }

    /** True for a unit whose key does a quantum action (built-in or addon). */
    public static boolean isAction(@Nullable UnitRef unit) {
        if (unit == null) return false;
        if (kind(unit) != null) return true;
        return unit.addon() != null && SuitQuantumActions.get(unit.key()) != null;
    }

    public static boolean isActionKey(String key) {
        return key != null && !key.isEmpty() && !"-".equals(key) && isAction(UnitRef.byKey(key));
    }

    /**
     * Quantum action units on worn quantum pieces, in key priority order: blink, phase shift,
     * force field (deflector Mk.II), time dilation (behaviour predictor Mk.II), volley, then the
     * addon actions in registration order.
     */
    public static List<UnitRef> available(Player player) {
        List<UnitRef> out = new ArrayList<>(5);
        for (Kind k : Kind.VALUES) {
            if (!QuantumConfig.enabled(k.id)) continue;
            ItemStack piece = SuitModules.wornPieceWith(player, k.unit);
            if (!piece.isEmpty() && SuitModules.isQuantumPiece(piece) && SuitModules.tier(piece, k.unit.id()) >= k.minTier) out.add(UnitRef.of(k.unit));
        }
        if (!SuitQuantumActions.isEmpty()) {
            for (var action : SuitQuantumActions.all()) {
                var entry = SuitModuleRegistry.get(action.unit());
                if (entry == null || !entry.traits().allows(SuitClass.QUANTUM)) continue;
                UnitRef unit = UnitRef.of(entry.spec());
                ItemStack piece = SuitModules.wornPieceWith(player, unit);
                if (!piece.isEmpty() && SuitModules.isQuantumPiece(piece)) out.add(unit);
            }
        }
        return out;
    }

    /** Costs of an action unit after the config, or null. */
    @Nullable
    public static SuitActionSpec spec(@Nullable UnitRef unit) {
        Kind k = kind(unit);
        if (k != null) {
            return switch (k) {
                case BLINK -> SuitActionSpec.single(QuantumConfig.blinkCost(), QuantumConfig.blinkCooldown());
                case PHASE -> SuitActionSpec.single(QuantumConfig.phaseCost(), QuantumConfig.phaseCooldown());
                case FIELD -> SuitActionSpec.sustained(QuantumConfig.fieldStartCost(), QuantumConfig.fieldPerSecond(), QuantumConfig.fieldCooldown());
                case DILATION -> SuitActionSpec.single(QuantumConfig.dilationCost(), QuantumConfig.dilationCooldown());
                case VOLLEY -> SuitActionSpec.single(QuantumConfig.volleyCost(), QuantumConfig.volleyCooldown());
            };
        }
        var entry = unit == null ? null : SuitQuantumActions.get(unit.key());
        return entry == null ? null : entry.spec();
    }

    /** Translation key of the action name (the unit name for an addon action). */
    public static String nameKey(UnitRef unit) {
        Kind k = kind(unit);
        return k != null ? k.nameKey() : unit.nameKey();
    }

    /** Blink charges of the worn phase jump drive: 0 without one, 1 for Mk.I, 2 for Mk.II. */
    public static int blinkMaxCharges(Player player) {
        ItemStack piece = SuitModules.wornPieceWith(player, SuitModuleType.PHASE_JUMP_DRIVE);
        if (piece.isEmpty() || !SuitModules.isQuantumPiece(piece)) return 0;
        return SuitModules.tier(piece, SuitModuleType.PHASE_JUMP_DRIVE.id()) >= 2 ? 2 : 1;
    }

    // ------------------------------------------------------------------ request (server)

    /** Server: action key pressed for {@code unit} (a quantum action unit). */
    public static void request(ServerPlayer player, NanoCoreState keys, UnitRef unit, Vec3 input) {
        QuantumCoreState s = QuantumCore.state(player);
        Kind kind = kind(unit);
        // a second press ends a running toggle action
        if (kind == Kind.FIELD && s.fieldOn) { QuantumField.stop(player, s, 0); return; }
        if (kind == null && unit.key().equals(s.addonKey)) { endAddon(player, s, true); return; }
        ItemStack piece = SuitModules.wornPieceWith(player, unit);
        int tier = piece.isEmpty() ? 0 : Math.max(1, SuitModules.tier(piece, unit.key()));
        boolean on = !piece.isEmpty() && SuitModules.isQuantumPiece(piece) && SuitModules.isActive(player, unit)
            && (kind == null || QuantumConfig.enabled(kind.id) && tier >= kind.minTier);
        if (!on) { deny(player, NanoBursts.DENY_OFF); return; }
        if (s.decoherentTicks > 0) { deny(player, DENY_DECOHERENT); return; }
        if (keys.cooldown(unit.key()) > 0) { deny(player, NanoBursts.DENY_COOLDOWN); return; }
        SuitActionSpec spec = spec(unit);
        if (spec == null) { deny(player, NanoBursts.DENY_NONE); return; }
        // 0.1.10.1 Quantum Tunnel combo: a phase shift right after a blink costs half
        boolean tunnel = kind == Kind.PHASE && QuantumSynergies.tunnel(player, s);
        float cost = tunnel ? spec.coherenceCost() / 2F : spec.coherenceCost();
        // a toggle action needs more than its start cost, or it would fall at once
        if (spec.sustained() ? s.coherence <= cost : s.coherence < cost) { deny(player, DENY_COHERENCE); return; }
        boolean fired;
        if (kind == null) {
            fired = fireAddon(player, s, unit, piece, input, spec);
        } else {
            fired = switch (kind) {
                case BLINK -> blink(player, s, tier);
                case PHASE -> phase(player, s, tier);
                case FIELD -> QuantumField.start(player, s, tier);
                case DILATION -> QuantumDilation.start(player, s);
                case VOLLEY -> QuantumVolley.order(player);
            };
        }
        if (!fired) return;
        if (tunnel) {
            QuantumSynergies.combo(player, QuantumSynergies.COMBO_TUNNEL);
            s.lastBlink = -1000;                                  // one discount per blink
        }
        QuantumCore.spend(player, s, cost);
        if (kind == Kind.BLINK) {
            int max = Math.max(1, blinkMaxCharges(player));
            if (s.blinkCharges < 0 || s.blinkCharges > max) s.blinkCharges = max;
            s.blinkCharges = Math.max(0, s.blinkCharges - 1);
            if (s.blinkRecharge <= 0) s.blinkRecharge = Math.max(1, spec.cooldownTicks());
            keys.cooldown.put(unit.key(), s.blinkCharges > 0 ? BLINK_GAP : s.blinkRecharge);
        } else if (!spec.sustained() && spec.cooldownTicks() > 0) {
            keys.cooldown.put(unit.key(), spec.cooldownTicks());
        }
        keys.dirty();
    }

    static void deny(ServerPlayer player, int reason) {
        SuitPackets.send(player, NanoPackets.Fx.simple(player, NanoPackets.FX_DENIED, reason, 0, 0));
    }

    /** True when an action of {@code player} acts on {@code e}: hostile creatures and creatures that hunt the wearer; players only with PvP. */
    public static boolean affects(LivingEntity e, ServerPlayer player, boolean pvpSwitch) {
        if (e == player || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e instanceof Player other) return pvpSwitch && player.server.isPvpAllowed() && !other.isCreative() && !player.isAlliedTo(other);
        if (player.isAlliedTo(e)) return false;
        if (e instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID())) return false;
        if (e instanceof com.miophas.singularity_iteration.common.armory.WingmanEntity w && player.getUUID().equals(w.owner())) return false;
        return e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == player;
    }

    /** True when a projectile is the wearer's own or an ally's (the field and the dilation leave it alone). */
    public static boolean friendly(Projectile projectile, ServerPlayer player, boolean pvpSwitch) {
        Entity owner = projectile.getOwner();
        if (owner == null) return false;
        if (owner == player) return true;
        if (owner instanceof Player other) return !(pvpSwitch && player.server.isPvpAllowed()) || player.isAlliedTo(other);
        if (owner instanceof com.miophas.singularity_iteration.common.armory.WingmanEntity w && player.getUUID().equals(w.owner())) return true;
        if (owner instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID())) return true;
        return player.isAlliedTo(owner);
    }

    // ------------------------------------------------------------------ quantum blink

    /**
     * Jumps to the point in view, at most {@code range} blocks away. Never through a wall: the eye
     * line, a line at the feet and one at the waist from the start to the landing must be clear; the
     * body must fit at the landing (a step up of up to 1.8 blocks is tried); no lava or fire there;
     * the chunk must be loaded and inside the world border. A failed search sends DENY_BLOCKED.
     */
    static boolean blink(ServerPlayer player, QuantumCoreState s, int tier) {
        if (player.isPassenger() || player.isSleeping() || player.isFallFlying()) { deny(player, DENY_BLOCKED); return false; }
        ServerLevel level = player.serverLevel();
        double range = QuantumConfig.blinkRange(tier);
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1.0F);
        var hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean onTop = hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == net.minecraft.core.Direction.UP;
        double reach = hit.getType() == HitResult.Type.MISS ? range : hit.getLocation().distanceTo(eye) - 0.35;
        Vec3 start = player.position();
        double eyeHeight = player.getEyeHeight();
        double[] lifts = {0, 0.6, 1.2, 1.8, -0.6, -1.2};
        for (double d = reach; d >= 1.0; d -= 0.5) {
            Vec3 feet;
            if (onTop && d == reach) feet = hit.getLocation().add(0, 0.01, 0);
            else {
                Vec3 at = eye.add(look.scale(d));
                feet = new Vec3(at.x, at.y - eyeHeight, at.z);
            }
            for (double lift : lifts) {
                Vec3 spot = feet.add(0, lift, 0);
                if (spot.distanceTo(start) < 1.0) continue;
                if (!fits(level, player, spot) || !clearPath(level, player, start, spot)) continue;
                teleport(player, s, start, spot, tier);
                return true;
            }
        }
        deny(player, DENY_BLOCKED);
        return false;
    }

    private static boolean fits(ServerLevel level, ServerPlayer player, Vec3 spot) {
        if (!level.getWorldBorder().isWithinBounds(spot.x, spot.z) || spot.y < level.getMinBuildHeight() + 1 || spot.y > level.getMaxBuildHeight()) return false;
        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(spot))) return false;
        AABB box = player.getBoundingBox().move(spot.subtract(player.position()));
        if (!level.noCollision(player, box)) return false;
        // no landing in lava or fire
        return level.getBlockStates(box.inflate(0.05)).noneMatch(st -> st.getFluidState().is(FluidTags.LAVA) || st.is(BlockTags.FIRE));
    }

    /** The feet, the waist and the eyes travel from the start to the spot without a block in the way. */
    private static boolean clearPath(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        double[] heights = {0.2, player.getBbHeight() * 0.5, player.getEyeHeight()};
        for (double h : heights) {
            Vec3 a = from.add(0, h, 0), b = to.add(0, h, 0);
            if (level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) return false;
        }
        return true;
    }

    private static void teleport(ServerPlayer player, QuantumCoreState s, Vec3 from, Vec3 to, int tier) {
        ServerLevel level = player.serverLevel();
        // the /tp path: moves the server player and tells its client (no momentum carried over)
        player.teleportTo(level, to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        s.blinkSafeTicks = 10;
        s.lastBlink = level.getGameTime();                      // 0.1.10.1 Quantum Tunnel combo window
        QuantumSynergies.decoy(player, s, from);                // 0.1.10.1 Afterimage Decoy synergy
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), QuantumPackets.FX_BLINK, (float) from.x, (float) from.y, (float) from.z,
            (float) to.x, (float) to.y, (float) to.z, new int[]{tier}, new float[0]), 128);
        level.playSound(null, from.x, from.y, from.z, mio_icif_sounds.QUANTUM_BLINK.get(), SoundSource.PLAYERS, 0.9F, 1.15F);
        level.playSound(null, to.x, to.y, to.z, mio_icif_sounds.QUANTUM_BLINK.get(), SoundSource.PLAYERS, 1.0F, 0.95F);
    }

    // ------------------------------------------------------------------ phase shift

    /** Out of phase: creature and projectile damage passes through, no attacks and no mining (QuantumEvents). */
    static boolean phase(ServerPlayer player, QuantumCoreState s, int tier) {
        s.phaseTier = tier;
        s.phaseTicks = s.phaseMax = QuantumConfig.phaseTicks(tier);
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_PHASE_ON, s.phaseMax, tier, 0), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.PHASE_ON.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    /**
     * The phase shift ends; Mk.II releases a shock wave (damage and push) on the creatures around.
     * 0.1.10.1 Phase Counter synergy: the damage that passed through adds to the wave (a Mk.I
     * modulator then gets a wave too).
     */
    static void endPhase(ServerPlayer player, QuantumCoreState s) {
        int tier = s.phaseTier;
        s.phaseTicks = 0;
        float counter = s.phaseAbsorbed;
        s.phaseAbsorbed = 0;
        IntArrayList hit = new IntArrayList();
        float radius = 0;
        if (tier >= 2 || counter > 0) {
            radius = QuantumConfig.phaseShockRadius();
            Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
            float damage = (tier >= 2 ? (float) QuantumConfig.phaseShockDamage() : 0) + counter;
            boolean players = counter > 0 ? QuantumConfig.synergyVsPlayers() && QuantumConfig.phaseShockVsPlayers() : QuantumConfig.phaseShockVsPlayers();
            double r2 = (double) radius * radius;
            for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                    e -> e.distanceToSqr(c) <= r2)) {
                if (!affects(e, player, players)) continue;
                if (damage > 0) e.hurt(player.damageSources().playerAttack(player), damage);
                Vec3 away = e.position().subtract(player.position());
                e.knockback(1.1, -away.x, -away.z);
                e.hurtMarked = true;
                if (hit.size() < NanoPackets.Fx.MAX_TARGETS) hit.add(e.getId());
            }
        }
        // c = the stored counter damage (the client draws a heavier wave for it)
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), QuantumPackets.FX_PHASE_OFF, (float) player.getX(), (float) player.getY(),
            (float) player.getZ(), tier, radius, counter, hit.toIntArray(), new float[0]), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.PHASE_OFF.get(), SoundSource.PLAYERS,
            radius > 0 ? 1.1F : 0.8F, radius > 0 ? 0.85F : 1.0F);
        if (counter > 0) player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.RESONANCE_BURST.get(),
            SoundSource.PLAYERS, 0.8F, 1.25F);
    }

    // ------------------------------------------------------------------ addon actions

    private static boolean fireAddon(ServerPlayer player, QuantumCoreState s, UnitRef unit, ItemStack piece, Vec3 input, SuitActionSpec spec) {
        var entry = SuitQuantumActions.get(unit.key());
        if (entry == null) return false;
        boolean fired;
        try {
            fired = entry.behavior().fire(player, piece, input);
        } catch (RuntimeException e) {
            Singularity_Iteration.LOGGER.error("Addon quantum action {} failed", unit.key(), e);
            return false;
        }
        if (fired && spec.sustained()) s.addonKey = unit.key();
        return fired;
    }

    /** Server, each tick of a running toggle addon action: drain, the unit check, the behaviour. */
    static void tickAddon(ServerPlayer player, QuantumCoreState s) {
        var entry = SuitQuantumActions.get(s.addonKey);
        UnitRef unit = UnitRef.byKey(s.addonKey);
        if (entry == null || unit == null || !SuitModules.isActive(player, unit)) { endAddon(player, s, false); return; }
        ItemStack piece = SuitModules.wornPieceWith(player, unit);
        if (piece.isEmpty() || !SuitModules.isQuantumPiece(piece)) { endAddon(player, s, false); return; }
        if (!QuantumCore.drain(player, s, entry.spec().sustainPerSecond() / 20F)) return;     // decoherence ended it
        boolean keep;
        try {
            keep = entry.behavior().sustain(player, piece);
        } catch (RuntimeException e) {
            Singularity_Iteration.LOGGER.error("Addon quantum action {} failed; it stops", s.addonKey, e);
            keep = false;
        }
        if (!keep) endAddon(player, s, false);
    }

    /** Ends the running toggle addon action and starts its cooldown. */
    static void endAddon(ServerPlayer player, QuantumCoreState s, boolean byKey) {
        String key = s.addonKey;
        if (key.isEmpty()) return;
        s.addonKey = "";
        var entry = SuitQuantumActions.get(key);
        UnitRef unit = UnitRef.byKey(key);
        if (entry != null) {
            try {
                entry.behavior().end(player, unit == null ? ItemStack.EMPTY : SuitModules.wornPieceWith(player, unit));
            } catch (RuntimeException e) {
                Singularity_Iteration.LOGGER.error("Addon quantum action {} failed to end", key, e);
            }
            if (entry.spec().cooldownTicks() > 0) {
                NanoCoreState keys = com.miophas.singularity_iteration.common.suit.nano.NanoSuit.state(player);
                keys.cooldown.put(key, entry.spec().cooldownTicks());
                keys.dirty();
            }
        }
    }
}
