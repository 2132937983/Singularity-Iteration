// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.multiblock;

import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.1.10.19: a {@link StructureBlueprint} drawn in 3D inside a GUI rectangle (the JEI category
 * "Multiblock Structures"). The view turns slowly until the first drag; a drag turns it, the mouse wheel
 * zooms, and a layer can be shown alone. A position with more than one permitted block changes its block
 * every second. Blocks are drawn from their models (no block entity renderers).
 *
 * <p>0.1.11.189: also a fixed list of blocks ({@link #of}): the Blueprint Builder's structure catalog shows a structure as the
 * builder will place it (each port in its slot), not the alternatives of the plan.
 */
@OnlyIn(Dist.CLIENT)
public final class StructureView {
    private static final float MIN_ZOOM = 0.5F, MAX_ZOOM = 2.5F, SPIN_DEGREES_PER_SECOND = 12F;

    private record Shown(BlockPos pos, BlockState[] states) {}

    @org.jetbrains.annotations.Nullable private final StructureBlueprint blueprint;
    private final net.minecraft.resources.ResourceLocation id;
    private final int sizeX, sizeY, sizeZ;
    private final List<Shown> blocks = new ArrayList<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Shown> byPos =
        new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private List<Shown> cachedVisible;
    private int cachedLayer = Integer.MIN_VALUE;
    private int cachedAlt = Integer.MIN_VALUE;
    private boolean hasAlternatives;
    private float yaw = -35F, pitch = 28F, zoom = 1F;
    private boolean spinning = true;
    private final long created = Util.getMillis();
    /** -1: all layers; else the one layer that is shown (0 = bottom). */
    private int layer = -1;

    public StructureView(StructureBlueprint blueprint) {
        this(blueprint, blueprint.id(), blueprint.sizeX(), blueprint.sizeY(), blueprint.sizeZ());
        for (StructureBlueprint.Placement p : blueprint.placements()) {
            List<BlockState> states = new ArrayList<>();
            for (StructureBlueprint.BlockRef ref : p.key().blocks()) {
                BlockState state = ref.resolve();
                if (state != null) states.add(state);
            }
            if (!states.isEmpty()) addShown(new Shown(p.pos(), states.toArray(new BlockState[0])));
        }
    }

    private void addShown(Shown s) {
        blocks.add(s);
        byPos.put(s.pos().asLong(), s);
        if (s.states().length > 1) hasAlternatives = true;
    }

    private StructureView(@org.jetbrains.annotations.Nullable StructureBlueprint blueprint, net.minecraft.resources.ResourceLocation id,
                          int sizeX, int sizeY, int sizeZ) {
        this.blueprint = blueprint;
        this.id = id;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    /** 0.1.11.189: a view of fixed blocks ({@code positions} from 0 to the size, each with its one state). */
    public static StructureView of(net.minecraft.resources.ResourceLocation id, int sizeX, int sizeY, int sizeZ, List<BlockPos> positions, List<BlockState> states) {
        StructureView view = new StructureView(null, id, sizeX, sizeY, sizeZ);
        for (int i = 0; i < positions.size(); i++) view.addShown(new Shown(positions.get(i), new BlockState[]{states.get(i)}));
        return view;
    }

    /** The plan shown, or null for a view of fixed blocks ({@link #of}). */
    @org.jetbrains.annotations.Nullable
    public StructureBlueprint blueprint() { return blueprint; }

    /** The number of layers. */
    public int sizeY() { return sizeY; }

    /** -1 for all layers, else the layer shown alone. */
    public int layer() { return layer; }

    /** Shows the next layer up ({@code step} 1) or down (-1); after the top (or below the bottom) all layers. */
    public void stepLayer(int step) {
        int count = sizeY;
        if (count <= 1) { layer = -1; return; }
        int next = layer + step;
        layer = next < -1 ? count - 1 : next >= count ? -1 : next;
    }

    /** Turns the view by a mouse drag (pixels). */
    public void drag(double dx, double dy) {
        if (spinning) {
            yaw = currentYaw();
            spinning = false;
        }
        yaw += (float) dx * 1.6F;
        pitch = Math.max(-90F, Math.min(90F, pitch + (float) dy * 1.6F));
    }

    /** Zooms by mouse wheel steps. */
    public void zoom(double steps) {
        zoom = (float) Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * Math.pow(1.15, steps)));
    }

    private float currentYaw() {
        return spinning ? yaw + (Util.getMillis() - created) / 1000F * SPIN_DEGREES_PER_SECOND : yaw;
    }

    /** Draws the structure into the rectangle {@code x, y, width, height} (coordinates of the current pose). */
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (blocks.isEmpty()) return;
        Matrix4f matrix = graphics.pose().last().pose();
        Vector3f from = matrix.transformPosition(x, y, 0, new Vector3f());
        Vector3f to = matrix.transformPosition(x + width, y + height, 0, new Vector3f());
        // the scissor is in screen coordinates; the pose of a JEI recipe is translated
        graphics.enableScissor((int) Math.floor(from.x), (int) Math.floor(from.y), (int) Math.ceil(to.x), (int) Math.ceil(to.y));
        int sx = sizeX, sy = sizeY, sz = sizeZ;
        float diagonal = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        float scale = Math.min(width, height) / Math.max(1F, diagonal) * zoom;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        // the middle of the depth range stays in front of the GUI background and below tooltips (z 400)
        pose.translate(x + width / 2F, y + height / 2F, 150F);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(currentYaw()));
        pose.translate(-sx / 2F, -sy / 2F, -sz / 2F);
        RenderSystem.enableDepthTest();
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var buffers = graphics.bufferSource();
        int alternative = (int) (Util.getMillis() / 1000L);
        List<Shown> visible = visibleFor(layer, alternative);
        for (Shown shown : visible) {
            BlockState state = shown.states()[alternative % shown.states().length];
            pose.pushPose();
            pose.translate(shown.pos().getX(), shown.pos().getY(), shown.pos().getZ());
            try {
                dispatcher.renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            } catch (RuntimeException e) {
                // a broken model of another mod must not break the recipe screen
                com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures.report("structure-view-" + state.getBlock(),
                    "Drawing " + state + " in the structure view " + id, e);
            }
            pose.popPose();
        }
        graphics.flush();
        pose.popPose();
        graphics.disableScissor();
    }

    private List<Shown> visibleFor(int currentLayer, int alt) {
        int effectiveAlt = hasAlternatives ? alt : 0;
        if (cachedVisible != null && cachedLayer == currentLayer && cachedAlt == effectiveAlt) return cachedVisible;
        List<Shown> out = new ArrayList<>(blocks.size());
        for (Shown s : blocks) {
            int y = s.pos().getY();
            if (currentLayer >= 0 && y != currentLayer) continue;
            if (currentLayer < 0 && isFullyOccluded(s.pos(), effectiveAlt)) continue;
            out.add(s);
        }
        cachedVisible = out;
        cachedLayer = currentLayer;
        cachedAlt = effectiveAlt;
        return out;
    }

    private boolean isFullyOccluded(BlockPos pos, int alt) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return isOpaqueAt(x + 1, y, z, alt) && isOpaqueAt(x - 1, y, z, alt)
            && isOpaqueAt(x, y + 1, z, alt) && isOpaqueAt(x, y - 1, z, alt)
            && isOpaqueAt(x, y, z + 1, alt) && isOpaqueAt(x, y, z - 1, alt);
    }

    private boolean isOpaqueAt(int x, int y, int z, int alt) {
        Shown n = byPos.get(BlockPos.asLong(x, y, z));
        if (n == null) return false;
        BlockState s = n.states()[alt % n.states().length];
        return s.canOcclude() && s.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
