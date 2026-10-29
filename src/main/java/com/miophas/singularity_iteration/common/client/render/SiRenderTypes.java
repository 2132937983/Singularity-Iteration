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

    /** See-through lines (FCS sensor wireframes): no depth test, translucent, 2 px. */
    public static final RenderType XRAY_LINES = create("mio_icif_xray_lines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 16384, false, false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new LineStateShard(java.util.OptionalDouble.of(2.0)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(NO_DEPTH_TEST)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));

    /** See-through translucent quads (sensor box fill, impact ring). */
    public static final RenderType XRAY_FILL = create("mio_icif_xray_fill",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 16384, false, true,
        CompositeState.builder()
            .setShaderState(POSITION_COLOR_SHADER)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(NO_DEPTH_TEST)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));

    /** Depth-tested additive quads in the main target (deflector field cells, flight shock rings). */
    public static final RenderType FIELD_GLOW = create("mio_icif_field_glow",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 16384, false, false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));

    /**
     * 0.1.10.36: the enchantment glint of the baked (Blockbench) suit armor. The vanilla armor glint with the same shader,
     * texture, depth test, layering and the player's glint speed / strength options; only the texture scale differs. The
     * suit textures are 128x128 at one texel per model pixel (vanilla armor: 64x32), so the vanilla scale drew the
     * pattern 2x too wide and 4x too tall on these meshes. A fixed buffer (RegisterRenderBuffersEvent), like the vanilla
     * glints: it is drawn after the plates it lies on and can be filled together with them.
     */
    public static final RenderType SUIT_ARMOR_GLINT = create("mio_icif_suit_armor_glint",
        DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER)
            .setTextureState(new TextureStateShard(net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ENTITY, true, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(new TexturingStateShard("mio_icif_suit_glint_texturing", SiRenderTypes::suitGlintTexturing,
                com.mojang.blaze3d.systems.RenderSystem::resetTextureMatrix))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false));

    /** Vanilla {@code setupGlintTexturing(0.16F)} with the scale of a 128x128 suit texture: 2x across, 4x down. */
    private static void suitGlintTexturing() {
        long time = (long) (net.minecraft.Util.getMillis() * net.minecraft.client.Minecraft.getInstance().options.glintSpeed().get() * 8.0);
        float u = (float) (time % 110000L) / 110000.0F;
        float v = (float) (time % 30000L) / 30000.0F;
        org.joml.Matrix4f matrix = new org.joml.Matrix4f().translation(-u, v, 0.0F);
        matrix.rotateZ(0.17453292F).scale(0.16F * 2.0F, 0.16F * 4.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.setTextureMatrix(matrix);
    }

    /** {@link net.minecraft.client.renderer.entity.ItemRenderer#getArmorFoilBuffer} with the suit glint. */
    public static com.mojang.blaze3d.vertex.VertexConsumer suitFoil(net.minecraft.client.renderer.MultiBufferSource buffers, RenderType base, boolean foil) {
        return foil ? com.mojang.blaze3d.vertex.VertexMultiConsumer.create(buffers.getBuffer(SUIT_ARMOR_GLINT), buffers.getBuffer(base))
            : buffers.getBuffer(base);
    }

    /** Depth-tested alpha-blended ribbons and veils (wake trails, vapour cone): no depth write, no cull. */
    public static final RenderType VAPOR = create("mio_icif_vapor",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 16384, false, true,
        CompositeState.builder()
            .setShaderState(POSITION_COLOR_SHADER)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));
}
