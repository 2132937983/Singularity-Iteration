// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.crop.mio_icif_crop_entity;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Renders planted IC2 crops in the static chunk mesh instead of a per-frame BlockEntityRenderer.
 * Mirrors {@link DisguiseModels}: each crop stick variant is wrapped, reads the crop's plant and
 * growth stage via {@link ModelData} during chunk meshing, and emits the cached 8-quad hash cross
 * in {@link RenderType#cutout()}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class CropModels {
    private CropModels() {}

    public static final ModelProperty<ResourceLocation> PLANT_TEXTURE = new ModelProperty<>();
    private static final ChunkRenderTypeSet CUTOUT_SET = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final Map<String, ResourceLocation> TEXTURE_IDS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<BakedQuad>> QUAD_CACHE = new ConcurrentHashMap<>();
    private static final ResourceLocation[] FALLBACK = {
        ResourceLocation.fromNamespaceAndPath("mio_icif", "block/crop/fallback_1"),
        ResourceLocation.fromNamespaceAndPath("mio_icif", "block/crop/fallback_2"),
        ResourceLocation.fromNamespaceAndPath("mio_icif", "block/crop/fallback_3"),
        ResourceLocation.fromNamespaceAndPath("mio_icif", "block/crop/fallback_4")
    };

    private static ResourceLocation parseOrFallback(String path) {
        ResourceLocation id = ResourceLocation.tryParse(path);
        return id != null ? id : FALLBACK[0];
    }

    @SubscribeEvent
    static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        QUAD_CACHE.clear();
        var models = event.getModels();
        int wrapped = 0;
        Block[] cropBlocks = {mio_icif_blocks.CROP_STICK.get(), mio_icif_blocks.CROP_STICK_UPGRADED.get()};
        for (Block block : cropBlocks) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(key);
                if (model == null || model instanceof Wrapped) continue;
                models.put(key, new Wrapped(model));
                wrapped++;
            }
        }
        Singularity_Iteration.LOGGER.info("Crops: wrapped {} crop stick model variants for static chunk meshing", wrapped);
    }

    @Nullable
    private static ResourceLocation resolveTexture(mio_icif_crop_entity crop) {
        PlantType plant = crop.getPlant();
        if (plant == null) return null;
        int stage = crop.getGrowthStage();
        String texturePath = plant.getTexture(stage);
        if (texturePath == null || texturePath.isEmpty()) return null;
        ResourceLocation loc = TEXTURE_IDS.computeIfAbsent(texturePath, CropModels::parseOrFallback);
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.apply(loc);
        if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            int frame = Math.clamp(Math.round(4f * Math.max(1, stage) / Math.max(1, plant.getMaxGrowthStage())), 1, 4);
            loc = FALLBACK[frame - 1];
            sprite = atlas.apply(loc);
            if (sprite == null) return null;
        }
        return loc;
    }

    private static List<BakedQuad> quadsFor(ResourceLocation textureLoc) {
        return QUAD_CACHE.computeIfAbsent(textureLoc, CropModels::bakeHashCross);
    }

    /**
     * Bakes the 4 double-sided planes (8 quads) matching the legacy CropEntityRenderer geometry:
     * Y in [0, 13] (0..0.8125), planes at 8 +- 4.1 (0.5 +- 0.25625), span 8 +- 6.4 (0.5 +- 0.4).
     */
    private static List<BakedQuad> bakeHashCross(ResourceLocation textureLoc) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(textureLoc);
        if (sprite == null) return List.of();
        FaceBakery bakery = new FaceBakery();
        BlockFaceUV uv = new BlockFaceUV(new float[]{0f, 0f, 16f, 16f}, 0);
        List<BakedQuad> out = new ArrayList<>(8);

        float yTop = 13.0f;       // 0.8125 * 16
        float minSpan = 1.6f;     // (0.5 - 0.4) * 16
        float maxSpan = 14.4f;    // (0.5 + 0.4) * 16
        float pLow = 3.9f;        // (0.5 - 0.25625) * 16
        float pHigh = 12.1f;      // (0.5 + 0.25625) * 16

        // Plane 1 & 2: north-south (X = pLow, X = pHigh)
        addDoubleSided(out, bakery, sprite, uv, pLow, 0f, minSpan, pLow, yTop, maxSpan, Direction.WEST, Direction.EAST);
        addDoubleSided(out, bakery, sprite, uv, pHigh, 0f, minSpan, pHigh, yTop, maxSpan, Direction.WEST, Direction.EAST);
        // Plane 3 & 4: east-west (Z = pLow, Z = pHigh)
        addDoubleSided(out, bakery, sprite, uv, minSpan, 0f, pLow, maxSpan, yTop, pLow, Direction.NORTH, Direction.SOUTH);
        addDoubleSided(out, bakery, sprite, uv, minSpan, 0f, pHigh, maxSpan, yTop, pHigh, Direction.NORTH, Direction.SOUTH);
        return List.copyOf(out);
    }

    private static void addDoubleSided(List<BakedQuad> out, FaceBakery bakery, TextureAtlasSprite sprite, BlockFaceUV uv,
                                       float x1, float y1, float z1, float x2, float y2, float z2,
                                       Direction front, Direction back) {
        Vector3f from = new Vector3f(x1, y1, z1);
        Vector3f to = new Vector3f(x2, y2, z2);
        BlockElementFace faceA = new BlockElementFace(null, -1, "", uv);
        BlockElementFace faceB = new BlockElementFace(null, -1, "", uv);
        out.add(bakery.bakeQuad(from, to, faceA, sprite, front, BlockModelRotation.X0_Y0, null, false));
        out.add(bakery.bakeQuad(from, to, faceB, sprite, back, BlockModelRotation.X0_Y0, null, false));
    }

    private static final class Wrapped extends BakedModelWrapper<BakedModel> {
        Wrapped(BakedModel original) {
            super(original);
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            if (level.getBlockEntity(pos) instanceof mio_icif_crop_entity crop) {
                ResourceLocation tex = resolveTexture(crop);
                if (tex != null) return data.derive().with(PLANT_TEXTURE, tex).build();
            }
            return data;
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            ChunkRenderTypeSet own = originalModel.getRenderTypes(state, rand, data);
            return data.get(PLANT_TEXTURE) != null ? ChunkRenderTypeSet.union(own, CUTOUT_SET) : own;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                        ModelData data, @Nullable RenderType renderType) {
            List<BakedQuad> own = originalModel.getQuads(state, side, rand, data, renderType);
            if (side != null || (renderType != null && renderType != RenderType.cutout())) return own;
            ResourceLocation tex = data.get(PLANT_TEXTURE);
            if (tex == null) return own;
            List<BakedQuad> cropQuads = quadsFor(tex);
            if (cropQuads.isEmpty()) return own;
            if (own.isEmpty()) return cropQuads;
            List<BakedQuad> combined = new ArrayList<>(own.size() + cropQuads.size());
            combined.addAll(own);
            combined.addAll(cropQuads);
            return combined;
        }
    }
}
