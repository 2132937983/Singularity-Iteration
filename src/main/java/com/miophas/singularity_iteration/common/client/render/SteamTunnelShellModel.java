package com.miophas.singularity_iteration.common.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_block_steam_tunnel_part;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelStructure;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * 0.1.10.39: the connected-texture model of the Steam Tunnel's shell blocks (casing, fluid port, power port), loader
 * {@code mio_icif:steam_tunnel_shell}. Baked once: each face takes one of 16 tiles by the block's {@code LINKS} (bit 1 up, 2 down,
 * 4 left, 8 right of the face as drawn; a set bit drops the border on that side), and a casing on the front ring of a formed tunnel
 * shows its piece of the big fan on its front face instead, the animated one while the tunnel runs. Model fields: {@code tiles}
 * (the 16 textures by mask) and, for the casing, {@code fan} (the prefix of {@code <place>} and {@code <place>_spin}).
 * <p>A multipart blockstate did the same before, but vanilla bakes a multipart model once per block state (thousands of copies).
 */
public final class SteamTunnelShellModel {
    private SteamTunnelShellModel() {}

    public static final ResourceLocation LOADER = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "steam_tunnel_shell");

    /** Per face (3D data value): the neighbour above, below, left and right of its texture with the default UVs. */
    private static final Direction[][] AROUND = new Direction[6][];

    static {
        AROUND[Direction.DOWN.get3DDataValue()] = new Direction[] {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST};
        AROUND[Direction.UP.get3DDataValue()] = new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        AROUND[Direction.NORTH.get3DDataValue()] = new Direction[] {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
        AROUND[Direction.SOUTH.get3DDataValue()] = new Direction[] {Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};
        AROUND[Direction.WEST.get3DDataValue()] = new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
        AROUND[Direction.EAST.get3DDataValue()] = new Direction[] {Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH};
    }

    /** The tile of face {@code side}: which of its four neighbours in the plane are shell blocks. */
    static int mask(BlockState state, Direction side) {
        if (!state.hasProperty(mio_icif_block_steam_tunnel_part.LINKS.get(side))) return 0;
        Direction[] around = AROUND[side.get3DDataValue()];
        int mask = 0;
        for (int i = 0; i < 4; i++) if (state.getValue(mio_icif_block_steam_tunnel_part.LINKS.get(around[i]))) mask |= 1 << i;
        return mask;
    }

    public enum Loader implements IGeometryLoader<Geometry> {
        INSTANCE;

        @Override
        public Geometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            JsonArray array = GsonHelper.getAsJsonArray(json, "tiles");
            if (array.size() != 16) throw new JsonParseException("steam_tunnel_shell: 16 tiles expected, got " + array.size());
            List<String> tiles = new ArrayList<>();
            array.forEach(element -> tiles.add(element.getAsString()));
            return new Geometry(tiles, json.has("fan") ? GsonHelper.getAsString(json, "fan") : null);
        }
    }

    public record Geometry(List<String> tiles, @Nullable String fan) implements IUnbakedGeometry<Geometry> {
        @SuppressWarnings("unchecked")
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state,
                ItemOverrides overrides) {
            FaceBakery bakery = new FaceBakery();
            List<BakedQuad>[][] faces = new List[6][16];
            for (Direction side : Direction.values())
                for (int mask = 0; mask < 16; mask++) faces[side.get3DDataValue()][mask] = face(bakery, sprite(sprites, tiles.get(mask)), side);
            List<BakedQuad>[][] fanFaces = null;
            if (fan != null) {
                fanFaces = new List[SteamTunnelStructure.Fan.values().length][];
                for (SteamTunnelStructure.Fan piece : SteamTunnelStructure.Fan.values()) {
                    if (piece == SteamTunnelStructure.Fan.NONE) continue;
                    String name = fan + piece.placeName();
                    fanFaces[piece.ordinal()] = new List[] {face(bakery, sprite(sprites, name), piece.front()), face(bakery, sprite(sprites, name + "_spin"), piece.front())};
                }
            }
            return new Baked(faces, fanFaces, sprites.apply(context.getMaterial("particle")), context.getTransforms());
        }

        private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> sprites, String name) {
            return sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.parse(name)));
        }

        /** The full face {@code side} of a cube with the default UVs (as a model element from 0 to 16 draws it). */
        private static List<BakedQuad> face(FaceBakery bakery, TextureAtlasSprite sprite, Direction side) {
            BlockElementFace face = new BlockElementFace(side, -1, "", new BlockFaceUV(new float[] {0, 0, 16, 16}, 0));
            return List.of(bakery.bakeQuad(new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), face, sprite, side, BlockModelRotation.X0_Y0, null, true));
        }
    }

    private record Baked(List<BakedQuad>[][] faces, List<BakedQuad>[][] fan, TextureAtlasSprite particle, ItemTransforms transforms)
            implements BakedModel {
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            if (side == null) return List.of();
            if (state == null) return faces[side.get3DDataValue()][0];
            if (fan != null && state.hasProperty(mio_icif_block_steam_tunnel_part.Casing.FAN)) {
                SteamTunnelStructure.Fan piece = state.getValue(mio_icif_block_steam_tunnel_part.Casing.FAN);
                if (piece != SteamTunnelStructure.Fan.NONE && piece.front() == side)
                    return fan[piece.ordinal()][state.getValue(mio_icif_block_steam_tunnel_part.Casing.SPINNING) ? 1 : 0];
            }
            return faces[side.get3DDataValue()][mask(state, side)];
        }

        @Override public boolean useAmbientOcclusion() { return true; }

        @Override public boolean isGui3d() { return true; }

        @Override public boolean usesBlockLight() { return true; }

        @Override public boolean isCustomRenderer() { return false; }

        @Override public TextureAtlasSprite getParticleIcon() { return particle; }

        @Override public ItemTransforms getTransforms() { return transforms; }

        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
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
