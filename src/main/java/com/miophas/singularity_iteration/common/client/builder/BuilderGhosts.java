// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity;
import com.miophas.singularity_iteration.common.builder.BuilderPlanPackets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * 0.1.11.184: the Blueprint Builder's block hologram: every block still to place, drawn as a translucent ghost of itself (as a
 * schematic mod does), without slowing the game on a large blueprint.
 *
 * <h2>How it stays light</h2>
 * <ul>
 *   <li>The plan comes from the server once per version ({@link BuilderPlanPackets}), only for a builder near the player
 *       with its area shown; it is dropped when the builder has not been drawn for {@link #FORGET} ticks.</li>
 *   <li>The ghosts are baked by 16-block sections into vertex buffers on the graphics card; a frame only draws the buffers
 *       of the sections in view and within {@link #MAX_DISTANCE} blocks. The faces between two ghosts, and between a ghost
 *       and a solid block, are left out, so a filled volume costs its outer shell only.</li>
 *   <li>Baking takes at most {@link #BAKE_NANOS} of a frame: a large blueprint appears over a few frames instead of
 *       stopping one.</li>
 *   <li>Only the blocks still to place are drawn (a position with the plan's block, or a block that cannot be replaced,
 *       shows nothing). The client checks {@link #CHECKS_PER_TICK} positions a tick round the plan and re-bakes only a
 *       section whose blocks changed.</li>
 *   <li>The "layer" view draws only the lowest layer with blocks still to place (where the drones work).</li>
 * </ul>
 *
 * <p>0.1.11.185: while the player places the blueprint in the world ({@link BuilderAdjust}) the ghosts are drawn as far ahead
 * as the move asked for, until the new plan arrives. The client asks again after {@link #ASK_GAP} ticks (it was 40).
 *
 * <p>0.1.11.186 (the builder's range limit went, plans may be very large): the ghosts show as far as the render distance (at
 * most {@link #MAX_DISTANCE}); a frame sets the shader up once and gives each section only its offset, as the terrain is
 * drawn (it was a full shader set-up a section); the red of blocks out of range went with the range.
 *
 * <p>0.1.11.187 (a blueprint has no block limit any more): each part of the plan shows as it arrives (the server sends a few
 * a tick); the plan comes bottom layer first, so a section is baked once every block below its top has arrived (it is not
 * baked again for each part). A plan this client's memory cannot hold ({@link #BYTES_PER_BLOCK} a block) shows only its
 * box, with a line over the hotbar.
 */
@OnlyIn(Dist.CLIENT)
public final class BuilderGhosts {
    private BuilderGhosts() {}

    /** The view while the area shows (chosen on the builder's screen, this client only). */
    public static final int BLOCKS = 0, LAYER = 1, FRAME = 2;
    public static int mode = BLOCKS;

    static final double MAX_DISTANCE = 512;
    static final long BAKE_NANOS = 2_000_000L;
    static final int CHECKS_PER_TICK = 8192, ASK_GAP = 10, FORGET = 100;
    static final float ALPHA = 0.5F;
    /** 0.1.11.187: what a block of a plan costs this client (positions, states, the index, the sections). */
    static final long BYTES_PER_BLOCK = 96;

    private static final Map<BlockPos, Ghost> GHOSTS = new HashMap<>();
    private static final Map<BlockPos, Long> ASKED = new HashMap<>();
    @Nullable private static ClientLevel lastLevel;
    private static long ticks;

    /**
     * A builder's plan on this client: its blocks, which of them are still to place, its baked sections. 0.1.11.187: filled
     * part by part as the parts arrive ({@link #add}); {@link #size} blocks so far.
     */
    static final class Ghost {
        final BlockPos builder;
        final int version, count;
        /** 0.1.11.185: the offset the plan was placed with (the in-world placing draws ahead of it). */
        int[] offset = {0, 0, 0};
        final BitSet received = new BitSet();
        /** 0.1.11.187: the last block's height of each part, and how many parts from the first have all arrived. */
        final int[] lastY;
        int contiguous, frontier = Integer.MIN_VALUE;
        boolean ready, tooLarge;
        int size;
        long[] pos;
        BlockState[] state;
        int[] sectionOf;
        final BitSet missing = new BitSet();
        final Long2IntOpenHashMap index = new Long2IntOpenHashMap();
        final Long2IntOpenHashMap sectionIndex = new Long2IntOpenHashMap();
        final List<Section> sections = new ArrayList<>();
        int cursor, layer = Integer.MAX_VALUE, passLowest = Integer.MAX_VALUE;
        long seen;

        Ghost(BlockPos builder, int version, int count) {
            this.builder = builder;
            this.version = version;
            this.count = Math.max(0, count);
            long capacity = (long) this.count * BuilderPlanPackets.PER_PART;
            tooLarge = capacity > Integer.MAX_VALUE - 8 || !com.miophas.singularity_iteration.common.builder.Blueprint.affordable(capacity * BYTES_PER_BLOCK);
            int n = tooLarge ? 0 : (int) capacity;
            pos = new long[n];
            state = new BlockState[n];
            sectionOf = new int[n];
            lastY = new int[tooLarge ? 0 : this.count];
            index.defaultReturnValue(-1);
            sectionIndex.defaultReturnValue(-1);
        }

        /** Adds part {@code p}'s blocks (still to place until checked: the bake checks each block itself). */
        void add(BuilderPlanPackets.Part p) {
            int n = p.blocks();
            if (n == 0 || size + n > pos.length) return;
            for (int k = 0; k < n; k++) {
                BlockPos at = p.pos(k);
                int i = size++;
                pos[i] = at.asLong();
                state[i] = Block.stateById(p.stateId(k));
                index.put(pos[i], i);
                layer = Math.min(layer, at.getY());
                long key = SectionPos.asLong(at.getX() >> 4, at.getY() >> 4, at.getZ() >> 4);
                int s = sectionIndex.get(key);
                if (s < 0) {
                    s = sections.size();
                    sectionIndex.put(key, s);
                    sections.add(new Section(key));
                }
                Section section = sections.get(s);
                section.entries.add(i);
                section.dirty = true;
                sectionOf[i] = s;
                missing.set(i);
            }
            lastY[p.index()] = p.pos(n - 1).getY();
            received.set(p.index());
            while (contiguous < count && received.get(contiguous)) frontier = lastY[contiguous++];
            ready = true;
        }

        /**
         * Whether every block of {@code s} has arrived: all parts have, or the parts in order have reached above its top (the
         * plan comes bottom layer first, so a block lower than the last one arrived is there).
         */
        boolean complete(Section s) { return contiguous >= count || s.sy * 16 + 15 < frontier; }

        void free() {
            for (Section s : sections) s.free();
        }
    }

    /** A 16-block section of a plan: its blocks and its baked ghosts. */
    static final class Section {
        final int sx, sy, sz;
        final IntArrayList entries = new IntArrayList();
        final AABB box;
        @Nullable VertexBuffer mesh;
        boolean dirty = true, empty;
        int bakedMode = -1, bakedLayer = Integer.MIN_VALUE;

        Section(long key) {
            sx = SectionPos.x(key); sy = SectionPos.y(key); sz = SectionPos.z(key);
            box = new AABB(sx * 16, sy * 16, sz * 16, sx * 16 + 16, sy * 16 + 16, sz * 16 + 16);
        }

        void free() {
            if (mesh != null) mesh.close();
            mesh = null;
        }
    }

    // ------------------------------------------------------------------ the plan from the server

    /** Called by the builder's renderer while its area shows: asks for its plan when this client has another version. */
    public static void want(BlueprintBuilderBlockEntity be) {
        BlockPos at = be.getBlockPos().immutable();
        Ghost g = GHOSTS.get(at);
        if (g != null && g.version == be.planVersion()) { g.seen = ticks; return; }
        Long asked = ASKED.get(at);
        if (asked != null && ticks - asked < ASK_GAP) return;
        ASKED.put(at, ticks);
        PacketDistributor.sendToServer(new BuilderPlanPackets.Request(at, be.planVersion()));
    }

    /** A part of a plan from the server: shown at once (0.1.11.187: it waited for the whole plan). */
    public static void accept(BuilderPlanPackets.Part p) {
        BlockPos at = p.builder().immutable();
        Ghost g = GHOSTS.get(at);
        if (g == null || g.version != p.version() || g.count != p.count()) {
            if (g != null) g.free();
            g = new Ghost(at, p.version(), p.count());
            g.offset = p.offset().length == 3 ? p.offset().clone() : g.offset;
            g.seen = ticks;
            GHOSTS.put(at, g);
            var player = Minecraft.getInstance().player;
            if (g.tooLarge && player != null)   // 0.1.11.187: the box shows, the blocks do not
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.mio_icif.builder.preview.too_large"), true);
        }
        if (p.count() == 0 || g.tooLarge) { g.ready = true; return; }   // nothing to show
        if (p.index() < 0 || p.index() >= g.count || g.received.get(p.index())) return;
        g.add(p);
    }

    // ------------------------------------------------------------------ checking the world

    /** Whether the plan's block at {@code at} is still to place and can be: the position holds another, replaceable block. */
    static boolean missing(ClientLevel level, BlockPos at, BlockState target) {
        if (!level.isLoaded(at)) return false;
        BlockState now = level.getBlockState(at);
        return now.getBlock() != target.getBlock() && now.canBeReplaced();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        if (mc.level != lastLevel) { clear(); lastLevel = mc.level; }
        if (mc.level == null || GHOSTS.isEmpty()) return;
        // forget the builders not drawn for a while (out of range, gone, area hidden)
        for (Iterator<Ghost> it = GHOSTS.values().iterator(); it.hasNext(); ) {
            Ghost g = it.next();
            if (ticks - g.seen > FORGET || !(mc.level.getBlockEntity(g.builder) instanceof BlueprintBuilderBlockEntity)) {
                g.free();
                it.remove();
                ASKED.remove(g.builder);
            }
        }
        // check positions round the plans; a change re-bakes its section
        int budget = CHECKS_PER_TICK;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (Ghost g : GHOSTS.values()) {
            if (!g.ready || g.size == 0) continue;
            int n = Math.min(budget, g.size);
            budget -= n;
            for (int k = 0; k < n; k++) {
                int i = g.cursor;
                at.set(g.pos[i]);
                boolean miss = missing(mc.level, at, g.state[i]);
                if (miss) g.passLowest = Math.min(g.passLowest, at.getY());
                if (miss != g.missing.get(i)) {
                    g.missing.set(i, miss);
                    g.sections.get(g.sectionOf[i]).dirty = true;
                }
                if (++g.cursor >= g.size) {   // a full pass: the lowest layer with blocks to place
                    g.cursor = 0;
                    g.layer = g.passLowest;
                    g.passLowest = Integer.MAX_VALUE;
                }
            }
            if (budget <= 0) break;
        }
    }

    public static void clear() {
        for (Ghost g : GHOSTS.values()) g.free();
        GHOSTS.clear();
        ASKED.clear();
    }

    // ------------------------------------------------------------------ drawing

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || mode == FRAME || GHOSTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        var frustum = event.getFrustum();
        double reach = Math.min(MAX_DISTANCE, mc.options.getEffectiveRenderDistance() * 16.0 + 16);   // as far as the terrain shows
        long deadline = System.nanoTime() + BAKE_NANOS;
        List<Section> draw = new ArrayList<>();
        List<int[]> shifts = new ArrayList<>();
        for (Ghost g : GHOSTS.values()) {
            if (!g.ready || g.size == 0 || ticks - g.seen > 2) continue;   // drawn only while the builder's renderer wants it
            int[] d = BuilderAdjust.delta(g.builder, g.offset);
            int[] shift = d == null ? new int[3] : d;   // 0.1.11.185: ahead of the plan while the player moves the blueprint
            for (Section s : g.sections) {
                if (mode == LAYER && (g.layer < s.sy * 16 || g.layer >= s.sy * 16 + 16)) continue;
                AABB box = d == null ? s.box : s.box.move(d[0], d[1], d[2]);
                if (box.getCenter().distanceToSqr(cam) > reach * reach || !frustum.isVisible(box)) continue;
                boolean stale = s.dirty || s.bakedMode != mode || mode == LAYER && s.bakedLayer != g.layer;
                if (stale && System.nanoTime() < deadline && g.complete(s)) bake(mc.level, g, s);   // the rest next frame
                if (s.bakedMode == mode && s.mesh != null && !s.empty) { draw.add(s); shifts.add(shift); }   // a section waiting keeps its last ghosts
            }
        }
        if (draw.isEmpty()) return;
        RenderType type = RenderType.translucent();
        type.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            // 0.1.11.186: one set-up for every section; each only moves the chunk offset (as LevelRenderer draws the terrain)
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, event.getModelViewMatrix(), event.getProjectionMatrix(), mc.getWindow());
            shader.apply();
            var offset = shader.CHUNK_OFFSET;
            for (int k = 0; k < draw.size(); k++) {
                Section s = draw.get(k);
                int[] d = shifts.get(k);
                if (offset != null) {
                    offset.set((float) (s.sx * 16 + d[0] - cam.x), (float) (s.sy * 16 + d[1] - cam.y), (float) (s.sz * 16 + d[2] - cam.z));
                    offset.upload();
                }
                s.mesh.bind();
                s.mesh.draw();
            }
            if (offset != null) offset.set(0F, 0F, 0F);
            shader.clear();
            VertexBuffer.unbind();
        }
        type.clearRenderState();
    }

    /** Bakes a section's ghosts (the blocks still to place; in the layer view those of the current layer) into its buffer. */
    static void bake(ClientLevel level, Ghost g, Section s) {
        s.dirty = false;
        s.bakedMode = mode;
        s.bakedLayer = g.layer;
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        View view = new View(level, g);
        PoseStack pose = new PoseStack();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        try (ByteBufferBuilder memory = new ByteBufferBuilder(Math.max(4096, s.entries.size() * 192))) {
            BufferBuilder builder = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            VertexConsumer ghost = new Tint(builder);
            for (int e = 0, n = s.entries.size(); e < n; e++) {
                int i = s.entries.getInt(e);
                at.set(g.pos[i]);
                boolean miss = missing(level, at, g.state[i]);
                g.missing.set(i, miss);
                if (!miss || mode == LAYER && at.getY() != g.layer) continue;
                BlockState state = g.state[i];
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                var model = blocks.getBlockModel(state);
                pose.pushPose();
                pose.translate(at.getX() - s.sx * 16, at.getY() - s.sy * 16, at.getZ() - s.sz * 16);
                long seed = state.getSeed(at);
                random.setSeed(seed);
                for (RenderType layer : model.getRenderTypes(state, random, ModelData.EMPTY))
                    blocks.getModelRenderer().tesselateBlock(view, model, state, at, pose, ghost, true, random, seed, OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY, layer);
                pose.popPose();
            }
            MeshData mesh = builder.build();
            if (mesh == null) {
                s.empty = true;
                s.free();
                return;
            }
            if (s.mesh == null) s.mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
            s.mesh.bind();
            s.mesh.upload(mesh);
            VertexBuffer.unbind();
            s.empty = false;
        }
    }

    /** The world as the bake sees it: the ghosts still to place stand where they will be (their shared faces are left out). */
    private record View(ClientLevel level, Ghost g) implements BlockAndTintGetter {
        @Override
        public BlockState getBlockState(BlockPos pos) {
            int i = g.index.get(pos.asLong());
            return i >= 0 && g.missing.get(i) ? g.state[i] : level.getBlockState(pos);
        }

        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override @Nullable public BlockEntity getBlockEntity(BlockPos pos) { return null; }
        @Override public int getHeight() { return level.getHeight(); }
        @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
        @Override public float getShade(Direction direction, boolean shade) { return level.getShade(direction, shade); }
        @Override public LevelLightEngine getLightEngine() { return level.getLightEngine(); }
        @Override public int getBlockTint(BlockPos pos, ColorResolver resolver) { return level.getBlockTint(pos, resolver); }
        @Override public int getBrightness(LightLayer layer, BlockPos pos) { return 15; }   // ghosts glow evenly
        @Override public int getRawBrightness(BlockPos pos, int amount) { return 15; }
    }

    /** The ghosts' colour: half transparent, a little towards the hologram's cyan, full bright. */
    private record Tint(VertexConsumer out) implements VertexConsumer {
        private int tint(int argb) {
            int a = (int) (((argb >>> 24) & 0xFF) * ALPHA), r0 = (argb >> 16) & 0xFF, g0 = (argb >> 8) & 0xFF, b0 = argb & 0xFF;
            return a << 24 | (int) (r0 * 0.82F) << 16 | (int) (g0 * 0.94F) << 8 | Math.min(255, (int) (b0 * 1.08F) + 10);
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { out.addVertex(x, y, z); return this; }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            int c = tint(alpha << 24 | red << 16 | green << 8 | blue);
            out.setColor(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24);
            return this;
        }

        @Override public VertexConsumer setUv(float u, float v) { out.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { out.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { out.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { out.setNormal(x, y, z); return this; }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            out.addVertex(x, y, z, tint(color), u, v, overlay, LightTexture.FULL_BRIGHT, nx, ny, nz);
        }
    }
}
