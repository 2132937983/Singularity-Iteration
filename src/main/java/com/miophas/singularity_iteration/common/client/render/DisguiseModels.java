package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.block.build.mio_icif_block_foam;
import com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
import com.miophas.singularity_iteration.common.blockentity.build.mio_icif_block_foam_entity;
import com.miophas.singularity_iteration.common.blockentity.mio_icif_wire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.11.24: disguised construction foam and disguised wires are drawn by the chunk mesh. Until 0.1.11.23 two block
 * entity renderers drew them every frame ({@code renderSingleBlock}: the whole disguise model, and for a wire its own
 * model too, re-tessellated each frame, with no face culling and the flat light of the block), and the renderer of every
 * wire in view ran each frame, disguised or not.
 *
 * <p>Every {@code disguised=true} model of the foam and of each wire is wrapped. When the chunk is meshed the wrapper reads
 * the block entity (as the status lamps do): the disguise block's default state. It then gives the disguise model's quads
 * in the disguise's own render types (cutout leaves, translucent glass) and, for a wire, its own model's quads in its own
 * types; a wire shows the disguise only when it is foamlogged, as before. Tinted disguise quads (grass, leaves) carry
 * their tint index moved up by {@link #TINT_OFFSET}, so the wire's own tint 0 (the cable colour) and the disguise's
 * never meet; the colour handlers below send those to the disguise block's handler, at the block's position (the old
 * renderer passed no position, so grass and leaves took the default colour, not the biome's). Without the block entity's
 * data yet a disguised block shows nothing, as before; the data's arrival re-meshes the section.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class DisguiseModels {
    private DisguiseModels() {}

    /** The disguise's state, read from the block entity when the chunk is meshed. */
    public static final ModelProperty<BlockState> DISGUISE = new ModelProperty<>();
    /** Tint indices of disguise quads start here. */
    public static final int TINT_OFFSET = 1000;

    @SubscribeEvent
    static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        int wrapped = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            boolean foam = block instanceof mio_icif_block_foam, wire = block instanceof mio_icif_block_wire;
            if (!foam && !wire) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (!disguised(state)) continue;
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(key);
                if (model == null || model instanceof Wrapped) continue;
                models.put(key, new Wrapped(model, wire));
                wrapped++;
            }
        }
        Singularity_Iteration.LOGGER.info("Disguises: wrapped {} foam and wire model variants", wrapped);
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        // the foam has no tint of its own: every tinted quad is the disguise's
        List<Block> foams = new ArrayList<>(), bareWires = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof mio_icif_block_foam) foams.add(block);
            else if (block instanceof mio_icif_block_wire && !tintedWire(block)) bareWires.add(block);
        }
        event.register((state, level, pos, tint) -> disguiseColor(level, pos, tint >= TINT_OFFSET ? tint - TINT_OFFSET : tint),
            foams.toArray(Block[]::new));
        event.register((state, level, pos, tint) -> tint >= TINT_OFFSET ? disguiseColor(level, pos, tint - TINT_OFFSET) : -1,
            bareWires.toArray(Block[]::new));
    }

    /** The insulated wires (and glass fibre) have the cable-colour handler of the client setup, which calls {@link #wireTint}. */
    private static boolean tintedWire(Block block) {
        return block == com.miophas.singularity_iteration.common.registry.mio_icif_blocks.WIRE_ISOLATION_LV.get()
            || block == com.miophas.singularity_iteration.common.registry.mio_icif_blocks.WIRE_ISOLATION_MV.get()
            || block == com.miophas.singularity_iteration.common.registry.mio_icif_blocks.WIRE_ISOLATION_HV.get()
            || block == com.miophas.singularity_iteration.common.registry.mio_icif_blocks.WIRE_ISOLATION_HV_2.get()
            || block == com.miophas.singularity_iteration.common.registry.mio_icif_blocks.WIRE_ISOLATION_EV.get()
            || block == com.miophas.singularity_iteration.common.registry.mio_icif_blocks.WIRE_ISOLATION_EV_2.get()
            || block == com.miophas.singularity_iteration.common.registry.mio_icif_blocks.WIRE_ISOLATION_EV_3.get()
            || block == com.miophas.singularity_iteration.common.registry.mio_icif_blocks.WIRE_IV.get();
    }

    /** For the cable-colour handler: the colour of a disguise quad, or null for the wire's own tint. */
    public static @Nullable Integer wireTint(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tint) {
        return tint >= TINT_OFFSET ? disguiseColor(level, pos, tint - TINT_OFFSET) : null;
    }

    private static int disguiseColor(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tint) {
        if (level == null || pos == null) return -1;
        BlockState disguise = disguiseAt(level, pos);
        return disguise == null ? -1 : Minecraft.getInstance().getBlockColors().getColor(disguise, level, pos, tint);
    }

    private static boolean disguised(BlockState state) {
        if (state.getBlock() instanceof mio_icif_block_foam) return state.getValue(mio_icif_block_foam.DISGUISED);
        return state.hasProperty(mio_icif_block_wire.CASING) && mio_icif_block_wire.disguised(state);
    }

    /** The disguise shown at {@code pos} (null for none): a foam's, or a foamlogged wire's. */
    static @Nullable BlockState disguiseAt(BlockAndTintGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        Block block = null;
        if (be instanceof mio_icif_block_foam_entity foam && foam.hasDisguise()) block = foam.getDisguisedBlock();
        else if (be instanceof mio_icif_wire wire && wire.hasDisguise()) {
            BlockState state = wire.getBlockState();
            if (state.hasProperty(mio_icif_block_wire.CASING) && mio_icif_block_wire.foamlogged(state)) block = wire.getDisguisedBlock();
        }
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }

    private static BakedModel modelOf(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }

    static final class Wrapped extends BakedModelWrapper<BakedModel> {
        private final boolean wire;

        Wrapped(BakedModel original, boolean wire) {
            super(original);
            this.wire = wire;
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
            ModelData data = super.getModelData(level, pos, state, modelData);
            BlockState disguise = disguiseAt(level, pos);
            return disguise == null ? data : data.derive().with(DISGUISE, disguise).build();
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            BlockState disguise = data.get(DISGUISE);
            ChunkRenderTypeSet own = wire ? originalModel.getRenderTypes(state, rand, data) : ChunkRenderTypeSet.none();
            if (disguise == null) return own;
            ChunkRenderTypeSet shown = modelOf(disguise).getRenderTypes(disguise, rand, ModelData.EMPTY);
            return wire ? ChunkRenderTypeSet.union(own, shown) : shown;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                                        @Nullable RenderType renderType) {
            BlockState disguise = data.get(DISGUISE);
            List<BakedQuad> own = List.of();
            if (wire && (renderType == null || originalModel.getRenderTypes(state, rand, data).contains(renderType)))
                own = originalModel.getQuads(state, side, rand, data, renderType);
            if (disguise == null) return own;
            BakedModel model = modelOf(disguise);
            if (renderType != null && !model.getRenderTypes(disguise, rand, ModelData.EMPTY).contains(renderType)) return own;
            List<BakedQuad> shown = model.getQuads(disguise, side, rand, ModelData.EMPTY, renderType);
            if (shown.isEmpty()) return own;
            List<BakedQuad> out = new ArrayList<>(own.size() + shown.size());
            out.addAll(own);
            for (BakedQuad q : shown) out.add(q.isTinted() ? retint(q) : q);
            return out;
        }

        private static BakedQuad retint(BakedQuad q) {
            return new BakedQuad(q.getVertices(), q.getTintIndex() + TINT_OFFSET, q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion());
        }

        @Override
        public net.neoforged.neoforge.common.util.TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
            BlockState disguise = data.get(DISGUISE);
            return disguise != null && !wire ? modelOf(disguise).useAmbientOcclusion(disguise, ModelData.EMPTY, renderType)
                : super.useAmbientOcclusion(state, data, renderType);
        }

        @Override
        public TextureAtlasSprite getParticleIcon(ModelData data) {
            BlockState disguise = data.get(DISGUISE);
            return disguise != null ? modelOf(disguise).getParticleIcon(ModelData.EMPTY) : super.getParticleIcon(data);
        }
    }
}
