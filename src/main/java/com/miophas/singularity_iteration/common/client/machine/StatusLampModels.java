package com.miophas.singularity_iteration.common.client.machine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.machine.MachineStatus;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Front status lamp of the refined machines (green running, blinking amber no power, red
 * blocked, dark off). The lamp is the slit in the bottom frame band of the front; its four
 * looks are small standalone models ({@code block/status/lamp_*}). At bake time every listed
 * machine variant ({@code si_status_lamps.json}, generated with the models) is wrapped: the
 * wrapper appends the lamp quads, rotated like the variant, for the {@link MachineStatus}
 * of the block entity (read into the model data when the chunk is meshed). Only a status change re-meshes the block; the
 * amber blink is a texture animation, so nothing ticks on the client.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class StatusLampModels {
    private StatusLampModels() {}

    /** Model data key of the lamp state, filled from the block entity when the chunk is meshed. */
    public static final net.neoforged.neoforge.client.model.data.ModelProperty<MachineStatus> STATUS =
        new net.neoforged.neoforge.client.model.data.ModelProperty<>();

    private static final String[] LAMPS = {"off", "run", "nopower", "blocked"};   // MachineStatus order

    private static ModelResourceLocation lamp(int i) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/status/lamp_" + LAMPS[i]));
    }

    @SubscribeEvent
    static void registerAdditional(ModelEvent.RegisterAdditional event) {
        for (int i = 0; i < LAMPS.length; i++) event.register(lamp(i));
    }

    @SubscribeEvent
    static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        // lamp quads per status, per y rotation (0, 90, 180, 270)
        @SuppressWarnings("unchecked")
        List<BakedQuad>[][] quads = new List[LAMPS.length][4];
        RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < LAMPS.length; i++) {
            BakedModel model = models.get(lamp(i));
            if (model == null) return;
            List<BakedQuad> base = new ArrayList<>(model.getQuads(null, null, random, ModelData.EMPTY, null));
            for (Direction d : Direction.values()) base.addAll(model.getQuads(null, d, random, ModelData.EMPTY, null));
            for (int r = 0; r < 4; r++) {
                if (r == 0) { quads[i][r] = List.copyOf(base); continue; }
                Matrix4f m = new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY((float) Math.toRadians(-90.0 * r)).translate(-0.5F, 0F, -0.5F);
                IQuadTransformer t = QuadTransformers.applying(new Transformation(m));
                quads[i][r] = List.copyOf(t.process(base));
            }
        }
        Map<ResourceLocation, Map<String, Integer>> blocks = readList();
        int wrapped = 0;
        for (var key : new ArrayList<>(models.keySet())) {
            Map<String, Integer> variants = blocks.get(key.id());
            if (variants == null || "inventory".equals(key.getVariant())) continue;
            Integer y = variants.get(key.getVariant());
            if (y == null) y = variants.get(stripUnknown(key.getVariant(), variants));
            if (y == null) continue;
            int r = Math.floorMod(y / 90, 4);
            List<BakedQuad>[] byStatus = new List[LAMPS.length];
            for (int i = 0; i < LAMPS.length; i++) byStatus[i] = quads[i][r];
            models.put(key, new Wrapped(models.get(key), byStatus));
            wrapped++;
        }
        Singularity_Iteration.LOGGER.info("Status lamps: wrapped {} machine model variants", wrapped);
    }

    /** Variant keys in the list cover the properties the blockstate file names; drop extra ones (e.g. waterlogged). */
    private static String stripUnknown(String variant, Map<String, Integer> variants) {
        String sample = variants.keySet().iterator().next();
        List<String> names = new ArrayList<>();
        for (String kv : sample.split(",")) names.add(kv.split("=")[0]);
        List<String> kept = new ArrayList<>();
        for (String kv : variant.split(",")) if (names.contains(kv.split("=")[0])) kept.add(kv);
        return String.join(",", kept);
    }

    private static Map<ResourceLocation, Map<String, Integer>> readList() {
        Map<ResourceLocation, Map<String, Integer>> out = new HashMap<>();
        var rl = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "si_status_lamps.json");
        var resource = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (resource.isEmpty()) return out;
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var el : root.getAsJsonArray("blocks")) {
                JsonObject o = el.getAsJsonObject();
                Map<String, Integer> variants = new HashMap<>();
                for (var v : o.getAsJsonObject("variants").entrySet()) variants.put(v.getKey(), v.getValue().getAsInt());
                out.put(ResourceLocation.parse(o.get("block").getAsString()), variants);
            }
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.warn("Status lamp list unreadable", e);
        }
        return out;
    }

    /** A machine model plus the lamp of its current status. */
    static final class Wrapped extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad>[] lamps;

        Wrapped(BakedModel original, List<BakedQuad>[] lamps) {
            super(original);
            this.lamps = lamps;
        }

        @Override
        public ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState state,
                                      ModelData modelData) {
            ModelData data = super.getModelData(level, pos, state, modelData);
            if (level.getBlockEntity(pos) instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity machine) {
                return data.derive().with(STATUS, machine.clientMachineStatus()).build();
            }
            return data;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                                        @Nullable RenderType renderType) {
            List<BakedQuad> base = super.getQuads(state, side, rand, data, renderType);
            if (side != null) return base;
            if (renderType != null) {
                ChunkRenderTypeSet types = originalModel.getRenderTypes(state, rand, data);
                RenderType target = types.contains(RenderType.solid()) ? RenderType.solid() : types.iterator().next();
                if (renderType != target) return base;
            }
            MachineStatus status = data.get(STATUS);
            List<BakedQuad> lamp = lamps[status == null ? 0 : status.ordinal()];
            if (lamp.isEmpty()) return base;
            List<BakedQuad> out = new ArrayList<>(base.size() + lamp.size());
            out.addAll(base);
            out.addAll(lamp);
            return out;
        }
    }
}
