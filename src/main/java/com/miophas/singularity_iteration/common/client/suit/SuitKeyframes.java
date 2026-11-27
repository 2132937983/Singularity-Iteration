// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.SuitModules;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 0.1.11.229 (AN1 part 3): the optional keyframe animations of a player in a quantum or nano suit, third person only. With
 * GeckoLib installed {@link #init} sets a {@link Driver} (the GeckoLib side, {@code GeckoSuit}); without it nothing changes.
 *
 * <p>The client marks the moments it already sees ({@link #mark}): the take-off, a heavy landing, a boost and a hard brake of
 * the maneuver flight ({@code FlightImpactClient}), the own suit-up, suit eject and orders to the squad (the wheel). Each
 * plays its animation once ({@link Kind#ticks}); a player hovering in maneuver flight with no moment under way plays
 * {@code hover}. Boost, brake and the orders move the upper body only (the flight pose keeps the legs).
 */
@OnlyIn(Dist.CLIENT)
public final class SuitKeyframes {
    private SuitKeyframes() {}

    private static final Logger LOG = LoggerFactory.getLogger("mio_icif/suit-keyframes");

    /** A moment and its animation (AN1 part 3), its length in ticks, and whether it moves the upper body only. */
    public enum Kind {
        TAKEOFF("takeoff", 8, false), LAND_HEAVY("land_heavy", 16, false), BOOST("boost", 6, true), BRAKE("brake", 8, true),
        SUIT_UP("suit_up", 30, false), EJECT("eject", 20, false), POINT("command_point", 12, true), RECALL("command_recall", 14, true);

        public final String animation;
        public final int ticks;
        public final boolean upper;

        Kind(String animation, int ticks, boolean upper) { this.animation = animation; this.ticks = ticks; this.upper = upper; }
    }

    /** What a player plays now: an animation (short name), its trigger (a new one restarts it) and the upper-body-only flag. */
    public record Pick(String animation, int trigger, boolean upper) { }

    /** Poses a player's model from keyframes. */
    public interface Driver {
        boolean pose(Player player, HumanoidModel<?> model, float partialTick, Pick pick);
    }

    @Nullable private static Driver driver;
    private record Mark(Kind kind, int at) { }
    private static final Int2ObjectOpenHashMap<Mark> MARKS = new Int2ObjectOpenHashMap<>();

    /** Client setup: the GeckoLib driver when GeckoLib is installed. */
    public static void init() {
        if (!ModList.get().isLoaded("geckolib")) return;
        try {
            driver = com.miophas.singularity_iteration.common.client.compat.geckolib.GeckoSuit.create();
        } catch (Throwable t) {
            LOG.warn("GeckoLib found, but the suit keyframe driver failed to start", t);
            driver = null;
        }
    }

    /** True when {@code p} wears a quantum or nano chestplate (the suits these animations are for). */
    public static boolean suited(Player p) {
        var chest = p.getItemBySlot(EquipmentSlot.CHEST);
        return SuitModules.isQuantumPiece(chest) || SuitModules.isNanoPiece(chest);
    }

    /** A moment of {@code p}. */
    public static void mark(@Nullable Player p, Kind kind) {
        if (p == null || driver == null || !suited(p)) return;
        MARKS.put(p.getId(), new Mark(kind, p.tickCount));
    }

    /** What {@code p} plays now, or null. */
    @Nullable
    public static Pick pick(Player p) {
        Mark m = MARKS.get(p.getId());
        if (m != null && p.tickCount - m.at >= 0 && p.tickCount - m.at < m.kind.ticks) return new Pick(m.kind.animation, m.at, m.kind.upper);
        if (m != null && (p.tickCount - m.at >= m.kind.ticks || p.tickCount < m.at)) MARKS.remove(p.getId());
        if (ManeuverModeClient.hovering(p)) return new Pick("hover", 0, false);
        return null;
    }

    /** Poses the model of {@code p} from keyframes (third person); false when nothing plays. */
    public static boolean pose(Player p, HumanoidModel<?> model, float partialTick) {
        Driver d = driver;
        if (d == null || !suited(p)) return false;
        Pick pick = pick(p);
        if (pick == null) return false;
        try {
            return d.pose(p, model, partialTick, pick);
        } catch (Throwable t) {
            LOG.warn("The suit keyframe driver failed; off from now on", t);
            driver = null;
            return false;
        }
    }
}
