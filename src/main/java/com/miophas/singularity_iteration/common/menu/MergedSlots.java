package com.miophas.singularity_iteration.common.menu;

/**
 * 0.1.11.176: where the slots of the processing machines go in the merged screen (docs/gui_processing_machines.md, "合并布局";
 * the screen side is {@code ProcessInstrumentScreen}). Item positions (the 18 x 18 frame starts one pixel up and left).
 *
 * <p>0.1.11.178: every machine has its own drawing with the slots in it, as the canner does (the template of 0.1.11.176 only
 * put a window with the old figure between an input column and an output column): the input sits where the machine takes
 * it (in the hopper, on the die, on the hearth, on the infeed table), the output in the tray at the outlet after the
 * progress arrow, and the battery at the lower left with the energy bolt standing on it and a cable to the motor or heater.
 * One nested class per drawing; the screens draw round these places.
 *
 * <p>0.1.11.180: the screens are the vanilla 176 wide (they were 248): every drawing fits between x 8 and 168, the
 * outputs at the right edge, the icon buttons above them at x 153.
 */
public final class MergedSlots {
    private MergedSlots() {}

    public static final int BATTERY_X = 11, BATTERY_Y = 74;
    /** The energy bolt (9 x 13) right on top of the battery slot's frame, no gap. */
    public static final int BOLT_X = 14, BOLT_Y = BATTERY_Y - 1 - 13;
    /** The upper left column (the ore washer's water bucket and empty bucket). */
    public static final int LEFT_X = 11, LEFT_TOP_Y = 18, LEFT_SECOND_Y = 37;

    /** The macerator's hammer mill: the input in the hopper on the mill, the output in the tray after the discharge. */
    public static final class Mill {
        private Mill() {}
        public static final int IN_X = 73, IN_Y = 19, OUT_X = 148, OUT_Y = 66;
    }

    /** The cyclone mill: the input in the hopper on the ring mill, the arrow up to the cyclone, the output in the tray under its spigot. */
    public static final class Cyclone {
        private Cyclone() {}
        public static final int IN_X = 71, IN_Y = 19, OUT_X = 133, OUT_Y = 70;
    }

    /** The compressors' press: the charge on the die between the columns, under the platen; the output in the tray. */
    public static final class Press {
        private Press() {}
        public static final int IN_X = 80, IN_Y = 57, OUT_X = 150, OUT_Y = 57;
    }

    /** The extractor's screw press: the input in the hopper at the barrel's inlet, the output in the tray at its outlet. */
    public static final class Screw {
        private Screw() {}
        public static final int IN_X = 58, IN_Y = 19, OUT_X = 148, OUT_Y = 49;
    }

    /** The electric furnace: the input on the hearth in the chamber, the output in the tray outside. */
    public static final class Oven {
        private Oven() {}
        public static final int IN_X = 75, IN_Y = 50, OUT_X = 150, OUT_Y = 50;
    }

    /** The induction furnace: two lanes, each from a hopper on top into the crucible and out to a tray on the right (0.1.11.180: the hoppers side by side). */
    public static final class Induction {
        private Induction() {}
        public static final int IN_A_X = 63, IN_B_X = 91, IN_Y = 19, OUT_X = 146, OUT_A_Y = 46, OUT_B_Y = 70;
    }

    /** The metal formers' bench: the work on the infeed table, the tool in the middle, the output in the tray on the outfeed. */
    public static final class Former {
        private Former() {}
        public static final int IN_X = 36, IN_Y = 52, OUT_X = 150, OUT_Y = 52;
    }

    /** The block cutter: the block on the table under the saw, the blade on the rack by the column, the output in the tray. */
    public static final class Cutter {
        private Cutter() {}
        public static final int IN_X = 88, IN_Y = 58, BLADE_X = 34, BLADE_Y = 26, OUT_X = 150, OUT_Y = 58;
    }

    /** The recycler: the input in the hopper on the shredder, the scrap in the tray beside the bin. */
    public static final class Shredder {
        private Shredder() {}
        public static final int IN_X = 74, IN_Y = 19, OUT_X = 148, OUT_Y = 74;
    }

    /** The ore washer: the ore in the hopper on the drum, the three outputs in the rack on the right (the buckets at the left). */
    public static final class Washer {
        private Washer() {}
        public static final int IN_X = 78, IN_Y = 19, OUT_X = 150, OUT_TOP_Y = 23, OUT_STEP = 20;
    }

    /** The thermal centrifuge: the input in the hopper on the casing, the three outputs in the rack on the right. */
    public static final class Centrifuge {
        private Centrifuge() {}
        public static final int IN_X = 72, IN_Y = 19, OUT_X = 150, OUT_TOP_Y = 23, OUT_STEP = 20;
    }
}
