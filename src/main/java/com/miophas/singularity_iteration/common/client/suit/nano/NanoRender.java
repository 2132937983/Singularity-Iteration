// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Render types and shaders of the nano suit effects (0.1.8.5).
 *
 * <ul>
 *   <li>{@link #HOLOGRID}: additive scrolling hologram grid (vanilla energy-swirl shader with the
 *       nano grid texture), for afterimages and the camouflage deploy and release.</li>
 *   <li>{@link #REFRACT}: entity models that bend the scene behind them (shader
 *       {@code mio_icif:nano_refract}); Sampler0 is a copy of the frame made just before.</li>
 *   <li>{@link #HAZE}: heat-haze quads (shader {@code mio_icif:nano_haze}) for the dash streak and
 *       the EMP shock wave.</li>
 * </ul>
 * When a shader fails to load or the client turns refraction off, {@link #refraction()} is false
 * and the effects fall back to the hologram grid and additive glow.
 */
@SuppressWarnings("null")
public final class NanoRender extends RenderType {
    private NanoRender(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
                       Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    public static final ResourceLocation HOLOGRID_TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
        "textures/effect/nano_hologrid.png");
    public static final ResourceLocation SCENE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
        "nano_scene_copy");

    @Nullable private static ShaderInstance refractShader, hazeShader;
    @Nullable private static TextureTarget copy;
    private static boolean textureRegistered;
    private static boolean failed;
    /** Hologram grid scroll (texture units), advanced each frame by {@link #setScroll}. */
    private static float scrollU, scrollV;

    /** Additive hologram grid on entity models (vanilla energy-swirl shader, nano grid texture). */
    public static final RenderType HOLOGRID = create("mio_icif_nano_hologrid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
        1536, false, true, CompositeState.builder()
            .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
            .setTextureState(new TextureStateShard(HOLOGRID_TEXTURE, false, false))
            .setTexturingState(new TexturingStateShard("mio_icif_nano_hologrid_scroll",
                () -> RenderSystem.setTextureMatrix(new Matrix4f().translation(scrollU, scrollV, 0.0F)),
                RenderSystem::resetTextureMatrix))
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));

    /**
     * 0.1.10.30: the hologram grid four times finer, for gear with 128 x 128 textures (the holo projector): the grid cells
     * of {@link #HOLOGRID} were laid out for 64 x 64 skins.
     */
    public static final RenderType HOLOGRID_FINE = create("mio_icif_nano_hologrid_fine", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
        1536, false, true, CompositeState.builder()
            .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
            .setTextureState(new TextureStateShard(HOLOGRID_TEXTURE, false, false))
            .setTexturingState(new TexturingStateShard("mio_icif_nano_hologrid_fine_scroll",
                () -> RenderSystem.setTextureMatrix(new Matrix4f().translation(scrollU, scrollV * 0.5F, 0.0F).scale(4.0F, 4.0F, 1.0F)),
                RenderSystem::resetTextureMatrix))
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));

    public static final ResourceLocation HEXMESH_TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
        "textures/effect/nano_hexmesh.png");

    /** 0.1.8.6: additive hexagonal plating of the kinetic armor mesh (same pipeline as the grid, slower scroll). */
    public static final RenderType HEXMESH = create("mio_icif_nano_hexmesh", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
        1536, false, true, CompositeState.builder()
            .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
            .setTextureState(new TextureStateShard(HEXMESH_TEXTURE, false, false))
            .setTexturingState(new TexturingStateShard("mio_icif_nano_hexmesh_scroll",
                () -> RenderSystem.setTextureMatrix(new Matrix4f().translation(scrollU * 0.25F, -scrollV * 0.15F, 0.0F)),
                RenderSystem::resetTextureMatrix))
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));

    /** Entity models that refract the scene copy. */
    public static final RenderType REFRACT = create("mio_icif_nano_refract", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
        1536, false, false, CompositeState.builder()
            .setShaderState(new ShaderStateShard(() -> refractShader))
            .setTextureState(new TextureStateShard(SCENE_TEXTURE, false, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setCullState(CULL)
            .setLightmapState(NO_LIGHTMAP)
            .setOverlayState(NO_OVERLAY)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));

    /** Heat-haze quads over the scene copy. */
    public static final RenderType HAZE = create("mio_icif_nano_haze", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
        4096, false, false, CompositeState.builder()
            .setShaderState(new ShaderStateShard(() -> hazeShader))
            .setTextureState(new TextureStateShard(SCENE_TEXTURE, false, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false));

    /** Mod bus: loads the two shaders; a failure only disables refraction. */
    static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "nano_refract"), DefaultVertexFormat.NEW_ENTITY),
                s -> refractShader = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "nano_haze"), DefaultVertexFormat.POSITION_COLOR),
                s -> hazeShader = s);
            failed = false;
        } catch (IOException | RuntimeException e) {
            failed = true;
            refractShader = null;
            hazeShader = null;
            Singularity_Iteration.LOGGER.warn("Nano suit refraction shaders did not load; the camouflage uses the hologram grid only", e);
        }
    }

    /**
     * True when the refraction pass may run this frame. 0.1.10.30 (P-1): not while an Iris shader pack is on (the scene copy
     * would read the wrong buffer; the effects use their fallbacks).
     */
    public static boolean refraction() {
        return !failed && refractShader != null && hazeShader != null && FcsClientConfig.nanoRefraction()
            && !com.miophas.singularity_iteration.common.client.render.IrisCompat.shaderPackInUse();
    }

    static void setScroll(float u, float v) {
        scrollU = u;
        scrollV = v;
    }

    /**
     * Copies the colour of the main target into the scene texture (Sampler0 of the refraction
     * shaders). Call once per frame before the refraction draws.
     */
    static boolean copyScene() {
        if (!refraction()) return false;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        try {
            if (copy == null || copy.width != main.width || copy.height != main.height) {
                if (copy == null) copy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
                else copy.resize(main.width, main.height, Minecraft.ON_OSX);
                copy.setFilterMode(GL11.GL_LINEAR);
            }
            if (!textureRegistered) {
                mc.getTextureManager().register(SCENE_TEXTURE, new SceneTexture());
                textureRegistered = true;
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, copy.width, copy.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            main.bindWrite(false);
            return true;
        } catch (RuntimeException e) {
            failed = true;
            Singularity_Iteration.LOGGER.warn("Nano suit scene copy failed; refraction is off for this session", e);
            main.bindWrite(false);
            return false;
        }
    }

    /** The texture-manager entry of the scene copy: its id is the colour attachment of the copy target. */
    private static final class SceneTexture extends AbstractTexture {
        @Override
        public void load(ResourceManager manager) { }

        @Override
        public int getId() {
            return copy == null ? 0 : copy.getColorTextureId();
        }

        @Override
        public void releaseId() { }
    }
}
