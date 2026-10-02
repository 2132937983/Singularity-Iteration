package com.miophas.singularity_iteration.common.client.machine;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Moving mechanisms of running machines (press pistons, crusher jaws).
 *
 * <p>Performance design - everything else that moves on a machine (fans, saw blades, drums,
 * cores, status lamps) is a texture animation (.mcmeta) inside the chunk mesh and costs no
 * CPU. Only the few machines with a linear/rocking mechanism get this renderer, and it is
 * built to cost almost nothing:
 * <ul>
 *   <li>Idle machines return after a single cached block-state read: their mechanism is part
 *       of the static chunk model (rest pose), so no draw call and no matrix work happens.</li>
 *   <li>Running machines use a block-state model without the moving parts; this renderer adds
 *       them back as one or two pre-baked part models with a single transform - no per-tick
 *       state, no allocation, no network traffic (the phase is derived from game time).</li>
 *   <li>Vanilla culls block-entity renderers outside the view frustum (tight render box) and
 *       beyond {@link #getViewDistance()}; beyond {@link #FULL_RATE_DISTANCE} the motion is
 *       quantised to 4 ticks (lower level of detail, fewer distinct poses).</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class MachinePartAnimator implements BlockEntityRenderer<BlockEntity> {

    /** Animated machines: block registry path -> mechanism (matches tools/assetgen anim_manifest.json). */
    public static final Map<String, Kind> MACHINES = new LinkedHashMap<>();
    // Empty since the return to classic IC2 full-cube machine models: those have no
    // separate mechanism parts. Addons with sculpted machines can still register here
    // (block path -> kind) and ship "<model>_anim_a/_anim_b" part models.

    public enum Kind { PISTON, JAWS }

    /** Global switch (-Dmio_icif.partAnimation=false disables moving parts; they then stay at rest). */
    public static boolean enabled = !"false".equals(System.getProperty("mio_icif.partAnimation"));

    /** Optional self-timing for profiling (off by default; set by tooling). */
    public static boolean profile;
    public static long profileNanos, profileCalls, profileDrawn;

    /** Piston travel in model units (the head is modelled retracted). */
    static final float PISTON_STROKE = 3.0F / 16F;
    /** Crusher jaw swing (degrees) and pivots (model units, north-facing model space). */
    static final float JAW_SWING = 7.0F;
    static final float JAW_A_PIVOT_X = 9.75F / 16F, JAW_B_PIVOT_X = 6.25F / 16F, JAW_PIVOT_Y = 12.0F / 16F, JAW_PIVOT_Z = 2.0F / 16F;
    static final int VIEW_DISTANCE = 48;
    static final int FULL_RATE_DISTANCE = 20;

    private record Parts(Kind kind, ModelResourceLocation a, ModelResourceLocation b) {}
    private static final Map<Block, Parts> PARTS = new IdentityHashMap<>();

    public MachinePartAnimator(BlockEntityRendererProvider.Context context) {}

    private static ModelResourceLocation part(String path, String suffix) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "block/" + path + "_anim_" + suffix));
    }

    @Override
    public void render(BlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!profile) { renderImpl(be, partialTick, pose, buffers, light, overlay); return; }
        long t0 = System.nanoTime();
        renderImpl(be, partialTick, pose, buffers, light, overlay);
        profileNanos += System.nanoTime() - t0;
        profileCalls++;
    }

    private void renderImpl(BlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!MachineRunState.isRunning(state)) return;          // idle: mechanism is in the chunk mesh
        if (!enabled) { drawRest(be, state, pose, buffers, light, overlay); return; }
        Parts parts = PARTS.get(state.getBlock());
        if (parts == null || be.getLevel() == null) return;

        // motion phase (0..1) from game time: shared with the loop sound, no per-tick state
        Minecraft mc = Minecraft.getInstance();
        long gameTime = be.getLevel().getGameTime();
        float ticks = gameTime + partialTick;
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        if (cam.distanceToSqr(be.getBlockPos().getCenter()) > FULL_RATE_DISTANCE * FULL_RATE_DISTANCE) {
            ticks = (gameTime >> 2) << 2;                       // distant: 4-tick steps
        }
        float t = ((ticks + MachineRunState.phaseOffset(be.getBlockPos())) % MachineRunState.CYCLE_TICKS) / MachineRunState.CYCLE_TICKS;

        var models = mc.getModelManager();
        ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());

        pose.pushPose();
        float yRot = switch (MachineRunState.facing(state)) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
        if (yRot != 0) {
            pose.translate(0.5F, 0, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(-yRot));
            pose.translate(-0.5F, 0, -0.5F);
        }
        if (profile) profileDrawn++;
        if (parts.kind == Kind.PISTON) {
            pose.translate(0, -PISTON_STROKE * pistonStroke(t), 0);
            draw(renderer, models.getModel(parts.a), pose, buffer, light, overlay);
        } else {
            float bite = jawBite(t) * JAW_SWING;
            drawSwung(renderer, models.getModel(parts.a), pose, buffer, light, overlay, JAW_A_PIVOT_X, bite);
            drawSwung(renderer, models.getModel(parts.b), pose, buffer, light, overlay, JAW_B_PIVOT_X, -bite);
        }
        pose.popPose();
    }

    private static void drawRest(BlockEntity be, BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Parts parts = PARTS.get(state.getBlock());
        if (parts == null) return;
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        float yRot = switch (MachineRunState.facing(state)) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
        pose.translate(0.5F, 0, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-yRot));
        pose.translate(-0.5F, 0, -0.5F);
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        draw(mc.getBlockRenderer().getModelRenderer(), mc.getModelManager().getModel(parts.a), pose, buffer, light, overlay);
        if (parts.b != null) draw(mc.getBlockRenderer().getModelRenderer(), mc.getModelManager().getModel(parts.b), pose, buffer, light, overlay);
        pose.popPose();
    }

    /** Press stroke 0 (retracted) .. 1 (on the die), synced with sfx_loop_press: extend 0-0.6 s,
     *  contact at 0.6 s, dwell, return 0.7-1.3 s, rest. */
    static float pistonStroke(float t) {
        if (t < 0.30F) { float x = t / 0.30F; return x * x * (3 - 2 * x) * 0.85F + x * x * x * x * 0.15F; }
        if (t < 0.35F) return 1F;
        if (t < 0.65F) { float x = (t - 0.35F) / 0.30F; return 1F - x * x * (3 - 2 * x); }
        return 0F;
    }

    /** Jaw closure 0..1: five bites per cycle, peaks at the impact times of sfx_loop_crusher. */
    static float jawBite(float t) {
        float c = 0.5F + 0.5F * Mth.cos((float) (2 * Math.PI) * (5 * t - 0.15F));
        return c * c * c;
    }

    private static void drawSwung(ModelBlockRenderer renderer, BakedModel model, PoseStack pose, VertexConsumer buffer,
                                  int light, int overlay, float pivotX, float degrees) {
        pose.pushPose();
        pose.translate(pivotX, JAW_PIVOT_Y, JAW_PIVOT_Z);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        pose.translate(-pivotX, -JAW_PIVOT_Y, -JAW_PIVOT_Z);
        draw(renderer, model, pose, buffer, light, overlay);
        pose.popPose();
    }

    /** Flattened quads per part model, collected once after each resource reload. */
    private static final Map<BakedModel, BakedQuad[]> QUADS = new IdentityHashMap<>();

    private static BakedQuad[] quads(BakedModel model) {
        return QUADS.computeIfAbsent(model, m -> {
            List<BakedQuad> all = new ArrayList<>();
            RandomSource rand = RandomSource.create(42L);
            for (Direction d : Direction.values()) all.addAll(m.getQuads(null, d, rand, ModelData.EMPTY, null));
            all.addAll(m.getQuads(null, null, rand, ModelData.EMPTY, null));
            return all.toArray(BakedQuad[]::new);
        });
    }

    /** Bulk-emits the cached quads with one pose: no per-face model queries, no allocation. */
    private static void draw(ModelBlockRenderer renderer, BakedModel model, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        PoseStack.Pose last = pose.last();
        for (BakedQuad q : quads(model)) {
            buffer.putBulkData(last, q, 1F, 1F, 1F, 1F, light, overlay);
        }
    }

    /** Drops cached quads when models are reloaded (resource packs, F3+T). */
    public static void clearCache() {
        QUADS.clear();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntity be) {
        return new AABB(be.getBlockPos());                       // tight box: precise frustum culling
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    // ------------------------------------------------------------------ registration
    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        static void onBake(ModelEvent.BakingCompleted event) {
            clearCache();
        }

        @SubscribeEvent
        static void registerModels(ModelEvent.RegisterAdditional event) {
            MACHINES.forEach((path, kind) -> {
                event.register(part(path, "a"));
                if (kind == Kind.JAWS) event.register(part(path, "b"));
            });
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @SubscribeEvent
        static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            Map<Block, Parts> found = new IdentityHashMap<>();
            MACHINES.forEach((path, kind) -> {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path));
                found.put(block, new Parts(kind, part(path, "a"), kind == Kind.JAWS ? part(path, "b") : null));
            });
            PARTS.putAll(found);
            for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
                if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).getNamespace().equals(Singularity_Iteration.MOD_ID)) continue;
                for (Block block : found.keySet()) {
                    if (type.isValid(block.defaultBlockState())) {
                        event.registerBlockEntityRenderer((BlockEntityType) type, MachinePartAnimator::new);
                        break;
                    }
                }
            }
        }
    }
}
