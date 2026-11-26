// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.molecular;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.11.213 (MT1 X1): the "atoms" of an item in the molecular transformer's chamber. The chamber shows the item as its
 * texture's pixels, each a small cube that later flies apart and rebuilds into the product (X2). This class is plain Java with
 * no client classes, so the server-side tests can run it.
 */
public final class MolecularAtoms {
    private MolecularAtoms() {}

    /** Atoms are laid out on a GRID x GRID raster, the size of a vanilla item texture. */
    public static final int GRID = 16;

    /** One pixel: column {@code i} and row {@code j} on the 16x16 raster (row 0 at the top), colour 0xRRGGBB. */
    public record Atom(int i, int j, int rgb) {}

    /**
     * The atoms of a texture: {@code argb} holds {@code w} x {@code h} pixels (0xAARRGGBB, row by row). An animated texture
     * is a column of square frames, so only the top {@code w} x {@code w} frame counts. Larger textures are sampled down to
     * 16x16 (nearest pixel), smaller ones are centred. Pixels with alpha below 128 are left out.
     */
    public static List<Atom> sample(int[] argb, int w, int h) {
        List<Atom> out = new ArrayList<>();
        if (argb == null || w <= 0 || h <= 0 || argb.length < w * h) return out;
        int size = Math.min(w, h);
        if (size >= GRID) {
            for (int j = 0; j < GRID; j++) {
                for (int i = 0; i < GRID; i++) {
                    int c = argb[(j * size / GRID) * w + (i * size / GRID)];
                    if ((c >>> 24) >= 128) out.add(new Atom(i, j, c & 0xFFFFFF));
                }
            }
        } else {
            int off = (GRID - size) / 2;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int c = argb[y * w + x];
                    if ((c >>> 24) >= 128) out.add(new Atom(x + off, y + off, c & 0xFFFFFF));
                }
            }
        }
        return out;
    }

    /**
     * Fewer atoms for the "Fast" graphics setting: every other pixel in a checkerboard, so the item keeps its shape. An item
     * with {@code minimum} atoms or fewer is kept whole.
     */
    public static List<Atom> thin(List<Atom> atoms, int minimum) {
        if (atoms.size() <= minimum) return atoms;
        List<Atom> out = new ArrayList<>(atoms.size() / 2 + 1);
        for (Atom a : atoms) if (((a.i() + a.j()) & 1) == 0) out.add(a);
        return out;
    }

    /** A plain stand-in when an item has no readable texture: a 6x6 grey block in the middle. */
    public static List<Atom> fallback() {
        List<Atom> out = new ArrayList<>();
        for (int j = 5; j < 11; j++) for (int i = 5; i < 11; i++) out.add(new Atom(i, j, (i + j) % 3 == 0 ? 0xB8BCC2 : 0x8C9096));
        return out;
    }
}
