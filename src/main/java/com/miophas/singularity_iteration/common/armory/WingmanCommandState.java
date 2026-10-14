// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Player attachment {@code mio_icif:wingman_command} (0.1.10.2): the quantum command settings of
 * one player. Saved, copied on death. A player without the attachment has the defaults, which give
 * the 0.1.10.1 behaviour (no order, all Wingmen in the squad mode).
 */
public final class WingmanCommandState {
    /** Data link: the Wingmen's sightings go to the owner's HUD and maps (needs the entanglement relay). */
    public boolean datalink = true;
    /** Last squad mode (a {@link WingmanMode} ordinal), -1 before the first mode change; "follow squad" takes it. */
    public int squadMode = -1;

    // not saved: squad sync to the client
    int syncTimer;
    boolean sentActive;

    public static final Codec<WingmanCommandState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("datalink", true).forGetter(s -> s.datalink),
        Codec.INT.optionalFieldOf("squad_mode", -1).forGetter(s -> s.squadMode)
    ).apply(i, (datalink, squadMode) -> {
        WingmanCommandState s = new WingmanCommandState();
        s.datalink = datalink;
        s.squadMode = squadMode < 0 || squadMode >= WingmanMode.VALUES.length ? -1 : squadMode;
        return s;
    }));
}
