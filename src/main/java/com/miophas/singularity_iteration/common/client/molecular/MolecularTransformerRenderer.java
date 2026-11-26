// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.molecular;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_molecular_transformer;
import com.miophas.singularity_iteration.common.client.render.SiFxBuffers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * 0.1.11.213 (MT1 X1): draws the inside of the molecular transformer's glass chamber. This version draws the input as its
 * pixel atoms at the sample position (the chamber centre), turning while the machine works and standing still otherwise.
 * The process effects come in the next versions. Nothing is drawn beyond {@link #VIEW_DISTANCE} blocks, and the "Fast"
 * graphics setting halves the atoms.
 */
public class MolecularTransformerRenderer implements BlockEntityRenderer<mio_icif_molecular_transformer> {
    public MolecularTransformerRenderer(BlockEntityRendererProvider.Context context) { }

    /** Blocks within which the chamber is drawn. */
    public static final int VIEW_DISTANCE = 32;
    /** The sample is 5/16 of a block wide (the model leaves 5.5..10.5 free), so one atom is 5/256 of a block. */
    static final float SAMPLE = 5F / 16F, PIXEL = SAMPLE / MolecularAtoms.GRID;
    /** Turn of a working sample, radians per tick (1.25 rad/s). */
    static final float SPIN = 0.0625F;
    /** Faces' shade: up, down, north/south, east/west (as the vanilla block shading). */
    private static final float UP = 1F, DOWN = 0.5F, NS = 0.8F, EW = 0.6F;

    @Override
    public void render(mio_icif_molecular_transformer machine, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = machine.getLevel();
        ItemStack input = machine.viewInput();
        if (level == null || input.isEmpty()) return;
        List<MolecularAtoms.Atom> atoms = MolecularAtomCache.of(input, level);
        if (Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST) atoms = MolecularAtoms.thin(atoms, 24);
        if (atoms.isEmpty()) return;

        double time = level.getGameTime() + partial;
        boolean working = machine.viewWorking();
        float angle = working ? (float) (time * SPIN % (Math.PI * 2)) : 0.6F;
        float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
        float bright = brightness(level, light, working);

        VertexConsumer vc = SiFxBuffers.translucent(buffers);
        Matrix4f m = pose.last().pose();
        float h = PIXEL / 2;
        for (MolecularAtoms.Atom a : atoms) {
            float lx = (a.i() - 7.5F) * PIXEL, ly = (7.5F - a.j()) * PIXEL;
            cube(vc, m, 0.5F + lx * cos, 0.5F + ly, 0.5F + lx * sin, h, a.rgb(), bright);
        }
    }

    /** World light on the atoms: block or sky light (darkened at night); a working chamber lights its sample. */
    static float brightness(Level level, int packedLight, boolean working) {
        int block = LightTexture.block(packedLight), sky = Math.max(0, LightTexture.sky(packedLight) - level.getSkyDarken());
        float f = 0.25F + 0.75F * Math.max(block, sky) / 15F;
        return working ? Math.max(f, 0.85F) : f;
    }

    static void cube(VertexConsumer vc, Matrix4f m, float x, float y, float z, float h, int rgb, float bright) {
        float r = (rgb >> 16 & 255) / 255F * bright, g = (rgb >> 8 & 255) / 255F * bright, b = (rgb & 255) / 255F * bright;
        float x0 = x - h, x1 = x + h, y0 = y - h, y1 = y + h, z0 = z - h, z1 = z + h;
        quad(vc, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r * UP, g * UP, b * UP);
        quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r * DOWN, g * DOWN, b * DOWN);
        quad(vc, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r * NS, g * NS, b * NS);
        quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r * NS, g * NS, b * NS);
        quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r * EW, g * EW, b * EW);
        quad(vc, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r * EW, g * EW, b * EW);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float r, float g, float b) {
        vc.addVertex(m, ax, ay, az).setColor(r, g, b, 1F);
        vc.addVertex(m, bx, by, bz).setColor(r, g, b, 1F);
        vc.addVertex(m, cx, cy, cz).setColor(r, g, b, 1F);
        vc.addVertex(m, dx, dy, dz).setColor(r, g, b, 1F);
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
