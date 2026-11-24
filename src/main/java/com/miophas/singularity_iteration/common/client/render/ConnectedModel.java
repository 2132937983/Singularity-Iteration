// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.1.11.140: the connected model of the fusion reactor's component blocks (loader {@code mio_icif:connected}): a block joins the
 * blocks of the same kind next to it, so a wall of them reads as one panel. Baked into the chunk like any block model (no per-frame
 * cost): when the chunk is rebuilt the model reads which of its 18 neighbours (6 across the faces, 12 across the edges) join it
 * ({@link #getModelData}) and takes the quads baked for that mask (each mask baked once, on first use).
 *
 * <p>The model file holds ordinary elements, and:
 * <ul>
 *   <li>{@code "connect"}: {@code "self"} (the same block) or {@code "same_facing"} (the same block with the same facing);</li>
 *   <li>on an element, {@code "drop_if"}: the sides on which a joined neighbour removes it (an edge frame), and {@code "extend"}:
 *       the sides on which a joined neighbour stretches it to the block's edge (so a frame's neighbour or the glass runs on);</li>
 *   <li>{@code "ctm"}: texture variables whose faces take connected textures, each to the prefix of its tiles (made by
 *       {@code tools/fusion_ctm_tiles.py}): {@code <prefix>_h}, {@code _v}, {@code _none}, {@code _inner}. Such a face is cut into its
 *       four quarters, and each quarter shows its corner of the base texture or of a tile without the borders toward the joined
 *       neighbours (with the corner pixel when the block across the corner does not join). These faces take the default UVs of their
 *       position.</li>
 * </ul>
 * A face on the block's edge toward a joined neighbour is not drawn (no inner walls between two joined blocks).
 */
public final class ConnectedModel {
    private ConnectedModel() {}

    public static final ResourceLocation LOADER = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "connected");
    /** The neighbours that join, as bits of {@link #OFFSETS} in the model's own (unrotated) frame. */
    static final ModelProperty<Integer> MASK = new ModelProperty<>();

    /** The 18 neighbours: the 6 faces (bits 0-5, in {@link Direction} order), then the 12 edges. */
    static final int[][] OFFSETS;
    /** For each edge index i in [6..17], bitmask of its two constituent faces in [0..5]. */
    static final int[] EDGE_FACE_MASKS;

    static {
        List<int[]> list = new ArrayList<>();
        for (Direction d : Direction.values()) list.add(new int[] {d.getStepX(), d.getStepY(), d.getStepZ()});
        for (int x = -1; x <= 1; x++)
            for (int y = -1; y <= 1; y++)
                for (int z = -1; z <= 1; z++)
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) == 2) list.add(new int[] {x, y, z});
        OFFSETS = list.toArray(new int[0][]);
        EDGE_FACE_MASKS = new int[OFFSETS.length];
        for (int i = 6; i < OFFSETS.length; i++) {
            int[] o = OFFSETS[i];
            int fm = 0;
            for (Direction d : Direction.values()) {
                if ((o[0] != 0 && d.getStepX() == o[0])
                        || (o[1] != 0 && d.getStepY() == o[1])
                        || (o[2] != 0 && d.getStepZ() == o[2])) {
                    fm |= 1 << d.ordinal();
                }
            }
            EDGE_FACE_MASKS[i] = fm;
        }
    }

    /** The bit of a neighbour offset (a face or an edge), or -1. */
    static int bit(int x, int y, int z) {
        for (int i = 0; i < OFFSETS.length; i++) if (OFFSETS[i][0] == x && OFFSETS[i][1] == y && OFFSETS[i][2] == z) return i;
        return -1;
    }

    static boolean joined(int mask, Direction d) { return (mask & 1 << d.ordinal()) != 0; }

    static boolean joined(int mask, Direction a, Direction b) {
        int i = bit(a.getStepX() + b.getStepX(), a.getStepY() + b.getStepY(), a.getStepZ() + b.getStepZ());
        return i >= 0 && (mask & 1 << i) != 0;
    }

    /** Per face: the neighbours above, below, left and right of its texture with the default UVs. */
    static final Direction[][] AROUND = new Direction[6][];

    static {
        AROUND[Direction.DOWN.ordinal()] = new Direction[] {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST};
        AROUND[Direction.UP.ordinal()] = new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        AROUND[Direction.NORTH.ordinal()] = new Direction[] {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
        AROUND[Direction.SOUTH.ordinal()] = new Direction[] {Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};
        AROUND[Direction.WEST.ordinal()] = new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
        AROUND[Direction.EAST.ordinal()] = new Direction[] {Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH};
    }

    /** The tile a quarter of a face shows: its horizontal and vertical neighbours, and the one across its corner. */
    static String tile(boolean horizontal, boolean vertical, boolean corner) {
        if (!horizontal && !vertical) return "";
        if (horizontal && !vertical) return "_h";
        if (!horizontal) return "_v";
        return corner ? "_none" : "_inner";
    }

    /** One element as read: the vanilla element, and the sides that drop and stretch it. */
    record Part(BlockElement element, List<Direction> dropIf, List<Direction> extend) {}

    public enum Loader implements IGeometryLoader<Geometry> {
        INSTANCE;

        @Override
        public Geometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            String connect = GsonHelper.getAsString(json, "connect", "self");
            if (!connect.equals("self") && !connect.equals("same_facing")) throw new JsonParseException("connected: connect must be self or same_facing");
            List<Part> parts = new ArrayList<>();
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "elements")) {
                JsonObject o = e.getAsJsonObject();
                BlockElement element = context.deserialize(o, BlockElement.class);
                parts.add(new Part(element, sides(o, "drop_if"), sides(o, "extend")));
            }
            Map<String, String> ctm = new HashMap<>();
            if (json.has("ctm")) for (var entry : GsonHelper.getAsJsonObject(json, "ctm").entrySet()) ctm.put(entry.getKey(), entry.getValue().getAsString());
            return new Geometry(parts, ctm, connect.equals("same_facing"));
        }

        private static List<Direction> sides(JsonObject o, String key) {
            List<Direction> out = new ArrayList<>();
            if (!o.has(key)) return out;
            JsonArray array = GsonHelper.getAsJsonArray(o, key);
            for (JsonElement s : array) {
                Direction d = Direction.byName(s.getAsString());
                if (d == null) throw new JsonParseException("connected: unknown side " + s);
                out.add(d);
            }
            return out;
        }
    }

    public record Geometry(List<Part> parts, Map<String, String> ctm, boolean sameFacing) implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state,
                               ItemOverrides overrides) {
            // every texture the faces may need, resolved now (the quads are baked later, per mask)
            Map<String, TextureAtlasSprite> resolved = new HashMap<>();
            for (Part p : parts)
                for (BlockElementFace face : p.element().faces.values()) {
                    String var = face.texture().startsWith("#") ? face.texture().substring(1) : face.texture();
                    resolved.computeIfAbsent(var, v -> sprites.apply(context.getMaterial(face.texture())));
                    String prefix = ctm.get(var);
                    if (prefix != null)
                        for (String suffix : new String[] {"_h", "_v", "_none", "_inner"})
                            resolved.computeIfAbsent(var + suffix, v -> sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.parse(prefix + suffix))));
                }
            RenderTypeGroup group = context.getRenderTypeHint() != null ? context.getRenderType(context.getRenderTypeHint()) : RenderTypeGroup.EMPTY;
            return new Baked(this, resolved, state, sprites.apply(context.getMaterial("particle")), context.getTransforms(),
                context.useAmbientOcclusion(), context.isGui3d(), context.useBlockLight(), group);
        }
    }

    static final class Baked implements IDynamicBakedModel {
        private final Geometry geometry;
        private final Map<String, TextureAtlasSprite> sprites;
        private final ModelState state;
        private final Matrix4f rotation;
        private final TextureAtlasSprite particle;
        private final ItemTransforms transforms;
        private final boolean ao, gui3d, blockLight;
        private final RenderTypeGroup group;
        private final ChunkRenderTypeSet blockRenderTypes;
        private final boolean hasCtm;
        /** The world offset of each of the 18 neighbours, in this model's (rotated) placement. */
        private final int[][] worldOffsets = new int[OFFSETS.length][];
        private final Map<Integer, List<BakedQuad>[]> cache = new ConcurrentHashMap<>();

        Baked(Geometry geometry, Map<String, TextureAtlasSprite> sprites, ModelState state, TextureAtlasSprite particle, ItemTransforms transforms,
              boolean ao, boolean gui3d, boolean blockLight, RenderTypeGroup group) {
            this.geometry = geometry;
            this.sprites = sprites;
            this.state = state;
            this.rotation = state.getRotation().getMatrix();
            this.particle = particle;
            this.transforms = transforms;
            this.ao = ao;
            this.gui3d = gui3d;
            this.blockLight = blockLight;
            this.group = group;
            this.blockRenderTypes = group.isEmpty() ? ChunkRenderTypeSet.of(RenderType.solid()) : ChunkRenderTypeSet.of(group.block());
            this.hasCtm = !geometry.ctm().isEmpty();
            for (int i = 0; i < OFFSETS.length; i++) {
                Vector3f v = rotation.transformDirection(new Vector3f(OFFSETS[i][0], OFFSETS[i][1], OFFSETS[i][2]));
                worldOffsets[i] = new int[] {Math.round(v.x()), Math.round(v.y()), Math.round(v.z())};
            }
        }

        private boolean joins(BlockState self, BlockState other) {
            if (!other.is(self.getBlock())) return false;
            if (!geometry.sameFacing()) return true;
            if (self.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                return other.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    && self.getValue(BlockStateProperties.HORIZONTAL_FACING) == other.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (self.hasProperty(BlockStateProperties.FACING))
                return other.hasProperty(BlockStateProperties.FACING)
                    && self.getValue(BlockStateProperties.FACING) == other.getValue(BlockStateProperties.FACING);
            return true;
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            int mask = 0;
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int i = 0; i < 6; i++) {
                at.setWithOffset(pos, worldOffsets[i][0], worldOffsets[i][1], worldOffsets[i][2]);
                if (joins(state, level.getBlockState(at))) mask |= 1 << i;
            }
            if (hasCtm && mask != 0) {
                for (int i = 6; i < worldOffsets.length; i++) {
                    int req = EDGE_FACE_MASKS[i];
                    if ((mask & req) != req) continue;
                    at.setWithOffset(pos, worldOffsets[i][0], worldOffsets[i][1], worldOffsets[i][2]);
                    if (joins(state, level.getBlockState(at))) mask |= 1 << i;
                }
            }
            return data.derive().with(MASK, mask).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                                        @Nullable RenderType renderType) {
            Integer mask = data.get(MASK);
            List<BakedQuad>[] quads = cache.computeIfAbsent(state == null || mask == null ? 0 : mask, this::bakeMask);
            return quads[side == null ? 6 : side.ordinal()];
        }

        /** The quads of one mask: [6 culled sides, unculled]. */
        @SuppressWarnings("unchecked")
        private List<BakedQuad>[] bakeMask(int mask) {
            List<BakedQuad>[] out = new List[7];
            for (int i = 0; i < 7; i++) out[i] = new ArrayList<>();
            FaceBakery bakery = new FaceBakery();
            for (Part p : geometry.parts()) {
                boolean dropped = false;
                for (Direction d : p.dropIf()) dropped |= joined(mask, d);
                if (dropped) continue;
                BlockElement e = p.element();
                Vector3f from = new Vector3f(e.from), to = new Vector3f(e.to);
                for (Direction d : p.extend()) {
                    if (!joined(mask, d)) continue;
                    if (d.getAxisDirection() == Direction.AxisDirection.POSITIVE) set(to, d.getAxis(), 16);
                    else set(from, d.getAxis(), 0);
                }
                for (Map.Entry<Direction, BlockElementFace> f : e.faces.entrySet()) {
                    Direction d = f.getKey();
                    BlockElementFace face = f.getValue();
                    // no inner walls: a face on the block's edge toward a joined neighbour is not drawn
                    boolean onEdge = d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? get(to, d.getAxis()) >= 16 : get(from, d.getAxis()) <= 0;
                    if (onEdge && joined(mask, d)) continue;
                    if (face.cullForDirection() != null && joined(mask, face.cullForDirection())) continue;
                    Direction cull = face.cullForDirection() == null ? null : Direction.rotate(rotation, face.cullForDirection());
                    List<BakedQuad> into = out[cull == null ? 6 : cull.ordinal()];
                    String var = face.texture().startsWith("#") ? face.texture().substring(1) : face.texture();
                    if (!geometry.ctm().containsKey(var)) {
                        // as the model file has it (its extent may have been stretched: its UVs follow the new extent)
                        BlockElementFace use = from.equals(e.from) && to.equals(e.to) ? face
                            : new BlockElementFace(face.cullForDirection(), face.tintIndex(), face.texture(), new BlockFaceUV(null, face.uv().rotation));
                        BlockElement shape = new BlockElement(from, to, Map.of(d, use), e.rotation, e.shade);
                        into.add(bakery.bakeQuad(from, to, shape.faces.get(d), sprites.get(var), d, state, e.rotation, e.shade));
                        continue;
                    }
                    // a connected face: its four quarters
                    Direction[] around = AROUND[d.ordinal()];
                    for (float[] q : quarters(from, to, d)) {
                        Vector3f qFrom = new Vector3f(q[0], q[1], q[2]), qTo = new Vector3f(q[3], q[4], q[5]);
                        BlockElement shape = new BlockElement(qFrom, qTo,
                            Map.of(d, new BlockElementFace(face.cullForDirection(), face.tintIndex(), face.texture(), new BlockFaceUV(null, 0))), e.rotation, e.shade);
                        float[] uv = shape.faces.get(d).uv().uvs;
                        boolean left = (uv[0] + uv[2]) / 2 < 8, top = (uv[1] + uv[3]) / 2 < 8;
                        Direction h = left ? around[2] : around[3], v = top ? around[0] : around[1];
                        String suffix = tile(joined(mask, h), joined(mask, v), joined(mask, h, v));
                        into.add(bakery.bakeQuad(qFrom, qTo, shape.faces.get(d), sprites.get(var + suffix), d, state, e.rotation, e.shade));
                    }
                }
            }
            for (int i = 0; i < 7; i++) out[i] = List.copyOf(out[i]);
            return out;
        }

        @Override public boolean useAmbientOcclusion() { return ao; }
        @Override public boolean isGui3d() { return gui3d; }
        @Override public boolean usesBlockLight() { return blockLight; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return particle; }
        @Override public ItemTransforms getTransforms() { return transforms; }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            return blockRenderTypes;
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return List.of(group.isEmpty() ? Sheets.solidBlockSheet() : fabulous ? group.entityFabulous() : group.entity());
        }
    }

    /** The face {@code d} of the box from..to cut at the block's middle (8) along the face's two axes: each quarter as {from, to}. */
    static List<float[]> quarters(Vector3f from, Vector3f to, Direction d) {
        Direction.Axis[] plane = switch (d.getAxis()) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
        List<float[]> a = cuts(get(from, plane[0]), get(to, plane[0])), b = cuts(get(from, plane[1]), get(to, plane[1]));
        List<float[]> out = new ArrayList<>();
        for (float[] ra : a)
            for (float[] rb : b) {
                Vector3f f = new Vector3f(from), t = new Vector3f(to);
                set(f, plane[0], ra[0]); set(t, plane[0], ra[1]);
                set(f, plane[1], rb[0]); set(t, plane[1], rb[1]);
                out.add(new float[] {f.x(), f.y(), f.z(), t.x(), t.y(), t.z()});
            }
        return out;
    }

    private static List<float[]> cuts(float lo, float hi) {
        return lo < 8 && hi > 8 ? List.of(new float[] {lo, 8}, new float[] {8, hi}) : List.of(new float[] {lo, hi});
    }

    static float get(Vector3f v, Direction.Axis axis) {
        return switch (axis) { case X -> v.x(); case Y -> v.y(); case Z -> v.z(); };
    }

    static void set(Vector3f v, Direction.Axis axis, float value) {
        switch (axis) {
            case X -> v.x = value;
            case Y -> v.y = value;
            case Z -> v.z = value;
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        static void loaders(ModelEvent.RegisterGeometryLoaders event) {
            event.register(LOADER, Loader.INSTANCE);
        }
    }
}
