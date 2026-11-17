// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.suit.core.FrameCore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * 0.1.11.66 (quantum flight phase R0, decision one A): the nozzles of the quantum suit, read from its models, so that every
 * flame of the flight comes out of a port the armour really has.
 *
 * <p>Each {@link Nozzle} is in the space of its model part: pixels, y down (towards the feet), +z towards the back, relative
 * to the pivot of the part (the {@code armor_models/quantum_*.json} and {@code attachments/*_quantum_chestplate.json}
 * coordinates). The direction is the way the exhaust leaves (the push is the opposite way). The set depends on the frame core
 * of the chestplate:
 * <ul>
 *   <li><b>Base</b>: four slanted pods on the back (two per side; the lit face looks down and back), the spine panel (back),
 *       the crown vent at the top of the back unit (up), the tail port under it (down); the forearm fins (outwards), the
 *       palms.</li>
 *   <li><b>Propulsion core</b>: its two boosters cover the pods and the spine; their two nozzles (down) are the main
 *       thrust. The crown vent, the tail port, the forearms and the palms stay.</li>
 *   <li><b>Graviton core</b>: no flame at all. Its mount covers the pods; the gyro on the back, the two shoulder pads,
 *       the palms and the soles show field rings ({@link Kind#FIELD}).</li>
 * </ul>
 * The quantum boots add a sole (down) and a heel port (down and back) to each leg.
 *
 * <p>Kinds: {@link Kind#MAIN} pushes the flight, {@link Kind#LIFT} holds the hover, {@link Kind#RCS} are the small
 * attitude and correction jets (lit by the allocator of R1; dark in R0), {@link Kind#FIELD} is a graviton ring.
 */
public final class QuantumNozzles {
    private QuantumNozzles() {}

    /** The model part a nozzle sits on. */
    public enum Part { BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

    public enum Kind { MAIN, LIFT, RCS, FIELD }

    /** The nozzle set of a chestplate, by its frame core. */
    public enum Variant {
        BASE, PROPULSION, GRAVITON;

        public static Variant of(@Nullable FrameCore core) {
            if (core == FrameCore.PROPULSION) return PROPULSION;
            if (core == FrameCore.GRAVITON) return GRAVITON;
            return BASE;
        }
    }

    /**
     * One nozzle: its part, its place (pixels, part space), the unit direction of the exhaust and its size (1 = a palm).
     */
    public record Nozzle(String name, Part part, float x, float y, float z, float dx, float dy, float dz, Kind kind, float size) {
        static Nozzle of(String name, Part part, float x, float y, float z, float dx, float dy, float dz, Kind kind, float size) {
            float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            return new Nozzle(name, part, x, y, z, dx / l, dy / l, dz / l, kind, size);
        }
    }

    private static final List<Nozzle> BASE_BODY = List.of(
        // the pods: slanted boxes, their lit face looks down and back (normal (0, 1, 1))
        Nozzle.of("pod_right_upper", Part.BODY, -3.0F, 3.8F, 6.2F, 0, 1, 1, Kind.MAIN, 0.7F),
        Nozzle.of("pod_left_upper", Part.BODY, 3.0F, 3.8F, 6.2F, 0, 1, 1, Kind.MAIN, 0.7F),
        Nozzle.of("pod_right_lower", Part.BODY, -2.7F, 7.4F, 4.5F, 0, 1, 1, Kind.MAIN, 0.7F),
        Nozzle.of("pod_left_lower", Part.BODY, 2.7F, 7.4F, 4.5F, 0, 1, 1, Kind.MAIN, 0.7F),
        Nozzle.of("spine", Part.BODY, 0, 5.4F, 4.4F, 0, 0, 1, Kind.RCS, 0.6F));

    private static final List<Nozzle> PROPULSION_BODY = List.of(
        Nozzle.of("booster_right", Part.BODY, -2.6F, 13.1F, 8.5F, 0, 1, 0, Kind.MAIN, 1.3F),
        Nozzle.of("booster_left", Part.BODY, 2.6F, 13.1F, 8.5F, 0, 1, 0, Kind.MAIN, 1.3F));

    /** Ports the base back unit keeps under the propulsion core. */
    private static final List<Nozzle> SHARED_BODY = List.of(
        Nozzle.of("crown", Part.BODY, 0, 0.2F, 3.9F, 0, -1, 0, Kind.RCS, 0.6F),
        Nozzle.of("tail", Part.BODY, 0, 10.3F, 2.7F, 0, 1, 0, Kind.LIFT, 0.8F));

    private static final List<Nozzle> ARMS = List.of(
        Nozzle.of("forearm_right", Part.RIGHT_ARM, -4.7F, 6.6F, 0, -1, 0, 0, Kind.RCS, 0.45F),
        Nozzle.of("forearm_left", Part.LEFT_ARM, 4.7F, 6.6F, 0, 1, 0, 0, Kind.RCS, 0.45F),
        Nozzle.of("palm_right", Part.RIGHT_ARM, -1.2F, 10.4F, 0, 0, 1, 0, Kind.LIFT, 1F),
        Nozzle.of("palm_left", Part.LEFT_ARM, 1.2F, 10.4F, 0, 0, 1, 0, Kind.LIFT, 1F));

    private static final List<Nozzle> GRAVITON = List.of(
        Nozzle.of("gyro", Part.BODY, 0, 5.4F, 7.9F, 0, 0, 1, Kind.FIELD, 1.4F),
        Nozzle.of("pad_right", Part.RIGHT_ARM, -1.9F, -5.5F, 0, 0, -1, 0, Kind.FIELD, 0.9F),
        Nozzle.of("pad_left", Part.LEFT_ARM, 1.9F, -5.3F, 0, 0, -1, 0, Kind.FIELD, 0.9F),
        Nozzle.of("palm_right", Part.RIGHT_ARM, -1.2F, 10.4F, 0, 0, 1, 0, Kind.FIELD, 0.7F),
        Nozzle.of("palm_left", Part.LEFT_ARM, 1.2F, 10.4F, 0, 0, 1, 0, Kind.FIELD, 0.7F));

    private static final List<Nozzle> BOOTS = List.of(
        Nozzle.of("sole_right", Part.RIGHT_LEG, 0, 12.4F, -0.5F, 0, 1, 0, Kind.LIFT, 1F),
        Nozzle.of("sole_left", Part.LEFT_LEG, 0, 12.4F, -0.5F, 0, 1, 0, Kind.LIFT, 1F),
        // the heel port: lit faces down (12.2) and back (3.7)
        Nozzle.of("heel_right", Part.RIGHT_LEG, -0.1F, 12.0F, 3.5F, 0, 1, 0.6F, Kind.MAIN, 0.6F),
        Nozzle.of("heel_left", Part.LEFT_LEG, 0.1F, 12.0F, 3.5F, 0, 1, 0.6F, Kind.MAIN, 0.6F));

    private static final List<Nozzle> GRAVITON_BOOTS = List.of(
        Nozzle.of("sole_right", Part.RIGHT_LEG, 0, 12.4F, -0.5F, 0, 1, 0, Kind.FIELD, 0.8F),
        Nozzle.of("sole_left", Part.LEFT_LEG, 0, 12.4F, -0.5F, 0, 1, 0, Kind.FIELD, 0.8F));

    /** The nozzles of the chestplate (back unit and arms) for a variant. */
    public static List<Nozzle> chest(Variant v) {
        List<Nozzle> out = new ArrayList<>();
        switch (v) {
            case GRAVITON -> out.addAll(GRAVITON);
            case PROPULSION -> { out.addAll(PROPULSION_BODY); out.addAll(SHARED_BODY); out.addAll(ARMS); }
            default -> { out.addAll(BASE_BODY); out.addAll(SHARED_BODY); out.addAll(ARMS); }
        }
        return out;
    }

    /** The nozzles of the quantum boots (rings under a graviton chestplate). */
    public static List<Nozzle> boots(Variant v) {
        return v == Variant.GRAVITON ? GRAVITON_BOOTS : BOOTS;
    }

    /**
     * R0 output of a nozzle from the four limb outputs of the maneuver flight ({right hand, left hand, right foot, left foot},
     * 0..1) and the throttle (0..1). Main thrust grows with the cruise (none in the hover); the lift follows the limb it sits
     * on (the tail port the feet, fading in the cruise); RCS stays dark until the allocator (R1); a field ring follows its
     * limb, the gyro the larger of throttle and feet.
     */
    public static float output(Nozzle n, float[] limbs, float throttle) {
        float thr = clamp(throttle), cruise = clamp(thr / 0.35F);
        float hands = Math.max(at(limbs, 0), at(limbs, 1)), feet = 0.5F * (at(limbs, 2) + at(limbs, 3));
        float limb = switch (n.part()) {
            case RIGHT_ARM -> at(limbs, 0);
            case LEFT_ARM -> at(limbs, 1);
            case RIGHT_LEG -> at(limbs, 2);
            case LEFT_LEG -> at(limbs, 3);
            case BODY -> feet;
        };
        return switch (n.kind()) {
            case RCS -> 0F;
            case MAIN -> n.part() == Part.BODY ? cruise * (0.35F + 0.65F * thr) : cruise * limb;
            case LIFT -> n.part() == Part.BODY ? feet * (1 - 0.6F * cruise) : limb;
            case FIELD -> n.part() == Part.BODY ? Math.max(thr, feet) : n.part() == Part.RIGHT_ARM || n.part() == Part.LEFT_ARM ? Math.max(limb, 0.4F * hands) : limb;
        };
    }

    private static float at(float[] a, int i) { return a == null || i >= a.length ? 0F : clamp(a[i]); }

    private static float clamp(float v) { return Float.isFinite(v) ? Math.max(0F, Math.min(1F, v)) : 0F; }
}
