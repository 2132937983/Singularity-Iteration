// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.suit.core.FrameCore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Exhaust centres in Minecraft part-local pixels, Y down and +Z aft. The two main
 * nozzles follow the authored acceleration pod transform. The ten RCS outlets
 * follow their own arm, torso or boot, so their allocator and visual axes agree. */
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

    // Shared with the visual pod rotation. These are the model pivot and
    // thruster_nozzle_left/right, converted from GEO to body-local pixels.
    public static final float POD_HINGE_Y = 5.35F, POD_HINGE_Z = 7.29F, POD_ANGLE = 38F;
    public static final float POD_SLIDE_Y = 0.55F, POD_SLIDE_Z = 0.42F;

    private static Nozzle main(String side, float x, float opening, float size) {
        double angle = Math.toRadians(POD_ANGLE * opening);
        float dy = (float) Math.cos(angle), dz = (float) Math.sin(angle);
        return Nozzle.of("thruster_nozzle_" + side, Part.BODY, x,
            POD_HINGE_Y + POD_SLIDE_Y * opening + 6.08F * dy,
            POD_HINGE_Z + POD_SLIDE_Z * opening + 6.08F * dz, 0, dy, dz, Kind.MAIN, size);
    }

    private static final List<Nozzle> CHEST_RCS = List.of(
        Nozzle.of("rcs_locator_shoulder_right_up", Part.RIGHT_ARM, -3.32F, -2.87F, 2.58F, 0,-1,0, Kind.RCS, .45F),
        Nozzle.of("rcs_locator_shoulder_left_up", Part.LEFT_ARM, 3.32F, -2.87F, 2.58F, 0,-1,0, Kind.RCS, .45F),
        Nozzle.of("rcs_locator_shoulder_right_front", Part.RIGHT_ARM, -4.32F, -2.34F, 2.05F, 0,0,-1, Kind.RCS, .45F),
        Nozzle.of("rcs_locator_shoulder_left_front", Part.LEFT_ARM, 4.32F, -2.34F, 2.05F, 0,0,-1, Kind.RCS, .45F),
        Nozzle.of("rcs_locator_waist_right_out", Part.BODY, -3.83F, 8.97F, 3.68F, -1,0,0, Kind.RCS, .5F),
        Nozzle.of("rcs_locator_waist_left_out", Part.BODY, 3.83F, 8.97F, 3.68F, 1,0,0, Kind.RCS, .5F));

    private static final List<Nozzle> BOOTS = List.of(
        Nozzle.of("rcs_locator_calf_right_back", Part.RIGHT_LEG, -.19F, 7.62F, 4.24F, 0,0,1, Kind.RCS, .45F),
        Nozzle.of("rcs_locator_calf_left_back", Part.LEFT_LEG, .19F, 7.62F, 4.24F, 0,0,1, Kind.RCS, .45F),
        Nozzle.of("rcs_locator_calf_right_down", Part.RIGHT_LEG, -1.19F, 8.15F, 3.71F, 0,1,0, Kind.RCS, .45F),
        Nozzle.of("rcs_locator_calf_left_down", Part.LEFT_LEG, 1.19F, 8.15F, 3.71F, 0,1,0, Kind.RCS, .45F),
        Nozzle.of("sole_right", Part.RIGHT_LEG, -.19F, 10.6F, 3.71F, 0,1,0, Kind.LIFT, 1F),
        Nozzle.of("sole_left", Part.LEFT_LEG, .19F, 10.6F, 3.71F, 0,1,0, Kind.LIFT, 1F));

    private static final List<Nozzle> GRAVITON = List.of(
        Nozzle.of("gyro", Part.BODY, 0, 9.6F, 4.3F, 0,0,1, Kind.FIELD, 1.4F),
        Nozzle.of("pad_right", Part.RIGHT_ARM, -3.32F, -2.87F, 2.58F, 0,-1,0, Kind.FIELD, .9F),
        Nozzle.of("pad_left", Part.LEFT_ARM, 3.32F, -2.87F, 2.58F, 0,-1,0, Kind.FIELD, .9F));
    private static final List<Nozzle> GRAVITON_BOOTS = List.of(
        Nozzle.of("sole_right", Part.RIGHT_LEG, -.19F, 10.6F, 3.71F, 0,1,0, Kind.FIELD, .8F),
        Nozzle.of("sole_left", Part.LEFT_LEG, .19F, 10.6F, 3.71F, 0,1,0, Kind.FIELD, .8F));

    public static List<Nozzle> chest(Variant variant) {
        return chest(variant, 0);
    }

    public static List<Nozzle> chest(Variant variant, float opening) {
        if (variant == Variant.GRAVITON) return new ArrayList<>(GRAVITON);
        float size = variant == Variant.PROPULSION ? 1.3F : .7F;
        List<Nozzle> out = new ArrayList<>(CHEST_RCS);
        out.add(main("left", 2.83F, opening, size));
        out.add(main("right", -2.83F, opening, size));
        return out;
    }

    public static List<Nozzle> boots(Variant variant) {
        return variant == Variant.GRAVITON ? GRAVITON_BOOTS : BOOTS;
    }

    /**
     * R0 output of a nozzle from the four limb outputs of the maneuver flight ({right hand, left hand, right foot, left foot},
     * 0..1) and the throttle (0..1). Main thrust grows with the cruise (none in the hover); the lift follows the limb it sits
     * on (the tail port the feet, fading in the cruise); RCS is not lit here (see {@link RcsAllocator}); a field ring follows its
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
