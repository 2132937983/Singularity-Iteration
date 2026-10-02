package com.miophas.singularity_iteration.common.util;

/**
 * Client-visible appearance switches shared by item code (which also loads on servers).
 *
 * <p>{@link #classic} mirrors whether the built-in "SI Classic" resource pack is selected: with
 * it the refined machine models and DSP icons are
 * replaced by the plain pre-Blockbench IC2 look. The client refreshes it on every resource reload.
 */
public final class SiVisuals {
    private SiVisuals() {}

    /** Built-in resource pack id of the classic look. */
    public static final String CLASSIC_PACK = "mod/mio_icif:resourcepacks/si_classic";

    public static volatile boolean classic = false;
}
