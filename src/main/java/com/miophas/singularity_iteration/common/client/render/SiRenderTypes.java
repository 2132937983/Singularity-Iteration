package com.miophas.singularity_iteration.common.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/** Custom render types. Extends RenderType only to reach its protected state shards. */
@SuppressWarnings("null")
public final class SiRenderTypes extends RenderType {
    private SiRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                          boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    /**
     * Additive, double-sided energy beam: position+colour, lightning shader, no depth
     * write (overlapping glow layers never occlude each other) but depth-tested so
     * walls still hide the beam. Drawn into the particles target, matching the
     * AFTER_PARTICLES stage it is rendered from (Fabulous graphics composites it).
     */
    public static final RenderType ENERGY_BEAM = create("mio_icif_energy_beam",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .setOutputState(PARTICLES_TARGET)
            .createCompositeState(false));
}
