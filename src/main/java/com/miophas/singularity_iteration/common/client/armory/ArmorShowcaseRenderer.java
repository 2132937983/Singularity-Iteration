package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmorShowcaseBlock;
import com.miophas.singularity_iteration.common.armory.ArmorShowcaseBlockEntity;
import com.miophas.singularity_iteration.common.armory.ArmoryPiece;
import com.miophas.singularity_iteration.common.client.render.RenderTypeTextures;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.nano.NanoRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Rotations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Holo display projector (0.1.8.0, was the Armor Showcase pedestal).
 *
 * <p>The outfit is a hologram: the mannequin body is not drawn. 0.1.10.30 (A-2): the gear is drawn twice through buffer
 * wrappers ({@link HoloSource}): a translucent, full-bright cyan body with the gear's own texture
 * ({@link RenderTypeTextures}; up to 0.1.10.29 an opaque tinted copy) and an additive hologram grid. A scan band rises
 * through the figure, horizontal slices jump aside now and then, and when the projected outfit changes the figure is
 * built up from the feet along a bright scan line. Over it, the projector adds additive effects: a light fan from the
 * emitter, scan rings that rise through the figure, a flicker, and a soft ground glow. Every armour, dyed layer, custom
 * armour model from other mods, trim and held item still renders through the vanilla armour stand renderer.
 *
 * <p>Facing follows the block and the 45 degree turntable steps; while powered by redstone the
 * hologram turns slowly. The label (name tag, linked suit name or "EXT-n" of a docked projector)
 * floats above the figure.
 */
public class ArmorShowcaseRenderer implements BlockEntityRenderer<ArmorShowcaseBlockEntity> {
    static final ResourceLocation MANNEQUIN = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/entity/armory_mannequin.png");

    private final BlockEntityRendererProvider.Context context;
    private final Map<ArmorShowcaseBlockEntity, ArmorStand> stands = new WeakHashMap<>();
    /** 0.1.10.30: per projector {outfit hash, game time when it changed} (the build-up). */
    private final Map<ArmorShowcaseBlockEntity, long[]> builds = new WeakHashMap<>();
    /** Build-up time, ticks. */
    static final float BUILD_TICKS = 24F;
    private MannequinRenderer renderer;

    public ArmorShowcaseRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    private MannequinRenderer renderer() {
        if (renderer == null) {
            Minecraft mc = Minecraft.getInstance();
            var entityContext = new EntityRendererProvider.Context(mc.getEntityRenderDispatcher(), mc.getItemRenderer(),
                mc.getBlockRenderer(), mc.getEntityRenderDispatcher().getItemInHandRenderer(), mc.getResourceManager(),
                mc.getEntityModels(), mc.font);
            renderer = new MannequinRenderer(entityContext);
        }
        return renderer;
    }

    @Override
    public void render(ArmorShowcaseBlockEntity showcase, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (showcase.getLevel() == null) return;
        ArmorStand stand = stands.computeIfAbsent(showcase, s -> mannequin(s));
        boolean any = false;
        long hash = 1;
        for (ArmoryPiece piece : ArmoryPiece.COLUMNS) {
            ItemStack want = showcase.display().get(piece.column());
            any |= !want.isEmpty();
            hash = hash * 31 + (want.isEmpty() ? 0 : net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(want.getItem()) + 1);
            if (!ItemStack.matches(want, stand.getItemBySlot(piece.slot))) stand.setItemSlot(piece.slot, want.copy());
        }
        var state = showcase.getBlockState();
        float time = showcase.getLevel().getGameTime() + partialTick;
        float facing = state.hasProperty(ArmorShowcaseBlock.FACING) ? state.getValue(ArmorShowcaseBlock.FACING).toYRot() : 0;
        float spin = state.hasProperty(ArmorShowcaseBlock.POWERED) && state.getValue(ArmorShowcaseBlock.POWERED) ? time * 1.5F : 0;
        float yaw = facing + showcase.rotation() + spin;
        stand.setYRot(yaw); stand.yRotO = yaw;
        stand.yBodyRot = yaw; stand.yBodyRotO = yaw;
        stand.yHeadRot = yaw; stand.yHeadRotO = yaw;

        // flicker: brief dips, different per projector
        long seed = showcase.getBlockPos().asLong();
        float flicker = 0.88F + 0.12F * (float) Math.sin(time * 0.37 + seed);
        if (((long) (time / 3) * 2654435761L + seed) % 97 == 0) flicker *= 0.55F;
        // 0.1.10.30: a new outfit builds up from the feet (not when the projector first comes into view)
        long[] build = builds.computeIfAbsent(showcase, s -> new long[]{Long.MIN_VALUE, Long.MIN_VALUE});
        if (build[0] != hash) {
            build[1] = build[0] == Long.MIN_VALUE ? Long.MIN_VALUE : showcase.getLevel().getGameTime();
            build[0] = hash;
        }
        float buildAge = build[1] == Long.MIN_VALUE ? BUILD_TICKS : showcase.getLevel().getGameTime() - build[1] + partialTick;
        float buildY = buildAge < BUILD_TICKS ? -0.05F + 2.15F * (buildAge / BUILD_TICKS) : -1F;
        if (any) {
            pose.pushPose();
            pose.translate(0.5, 0.28, 0.5);
            Holo holo = new Holo(new org.joml.Matrix4f(pose.last().pose()).invert(), flicker, time, buildY, slices(seed, time));
            renderer().render(stand, yaw, partialTick, pose, new HoloSource(buffers, holo, false), LightTexture.FULL_BRIGHT);
            renderer().render(stand, yaw, partialTick, pose, new HoloSource(buffers, holo, true), LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        effects(pose, buffers, time, flicker, any, showcase.docked());
        if (any && buildY >= 0) {
            // the build-up scan line
            VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            ring(vc, pose.last().pose(), 0.5F, 0.28F + buildY, 0.5F, 0.0F, 0.46F, 0.6F, 0.95F, 1F, 0.9F * flicker);
        }

        int labelLight = LevelRenderer.getLightColor(showcase.getLevel(), showcase.getBlockPos().above());
        String label = showcase.label();
        if (!label.isEmpty()) renderLabel(label, pose, buffers, labelLight);
    }

    /** Light fan, rising scan rings and the ground glow (additive). */
    private static void effects(PoseStack pose, MultiBufferSource buffers, float time, float flicker, boolean any, boolean docked) {
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        Matrix4f m = pose.last().pose();
        float cx = 0.5F, cz = 0.5F, base = 0.25F;
        float strength = (any ? 1.0F : 0.45F) * flicker;
        // emitter ring glow on the plate
        ring(vc, m, cx, base + 0.005F, cz, 0.30F, 0.36F, 0.4F, 0.85F, 1F, 0.55F * strength);
        if (docked) ring(vc, m, cx, base + 0.006F, cz, 0.40F, 0.43F, 0.3F, 1F, 0.6F, 0.35F * strength);
        if (!any) return;
        // light fan: from the emitter up to the figure outline
        int seg = 16;
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            float x0 = (float) Math.cos(a0), z0 = (float) Math.sin(a0), x1 = (float) Math.cos(a1), z1 = (float) Math.sin(a1);
            put(vc, m, cx + x0 * 0.12F, base, cz + z0 * 0.12F, 0.4F, 0.85F, 1F, 0.22F * strength);
            put(vc, m, cx + x1 * 0.12F, base, cz + z1 * 0.12F, 0.4F, 0.85F, 1F, 0.22F * strength);
            put(vc, m, cx + x1 * 0.42F, base + 1.9F, cz + z1 * 0.30F, 0.4F, 0.85F, 1F, 0);
            put(vc, m, cx + x0 * 0.42F, base + 1.9F, cz + z0 * 0.30F, 0.4F, 0.85F, 1F, 0);
        }
        // scan rings rising through the figure
        for (int k = 0; k < 3; k++) {
            float u = (time * 0.012F + k / 3F) % 1F;
            float y = base + 0.05F + u * 1.95F;
            float fade = (float) Math.sin(Math.PI * u);
            ring(vc, m, cx, y, cz, 0.36F, 0.40F, 0.45F, 0.9F, 1F, 0.45F * fade * strength);
        }
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float cx, float y, float cz, float r0, float r1,
                             float r, float g, float b, float a) {
        int seg = 24;
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            float x0 = (float) Math.cos(a0), z0 = (float) Math.sin(a0), x1 = (float) Math.cos(a1), z1 = (float) Math.sin(a1);
            put(vc, m, cx + x0 * r0, y, cz + z0 * r0, r, g, b, a);
            put(vc, m, cx + x1 * r0, y, cz + z1 * r0, r, g, b, a);
            put(vc, m, cx + x1 * r1, y, cz + z1 * r1, r, g, b, 0);
            put(vc, m, cx + x0 * r1, y, cz + z0 * r1, r, g, b, 0);
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, Math.max(0, Math.min(1, a)));
    }

    private static ArmorStand mannequin(ArmorShowcaseBlockEntity showcase) {
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, showcase.getLevel());
        stand.setNoBasePlate(true);
        stand.setShowArms(true);
        stand.setInvisible(true);      // hologram: only the projected gear is visible
        stand.setLeftArmPose(new Rotations(-8, 0, -6));
        stand.setRightArmPose(new Rotations(-12, 0, 8));
        stand.setPos(showcase.getBlockPos().getX() + 0.5, showcase.getBlockPos().getY() + 0.28, showcase.getBlockPos().getZ() + 0.5);
        return stand;
    }

    /** Suit name floating above the hologram. */
    private void renderLabel(String text, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        pose.pushPose();
        pose.translate(0.5, 2.45, 0.5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.022F, -0.022F, 0.022F);
        Matrix4f matrix = pose.last().pose();
        float x = -font.width(text) / 2.0F;
        int background = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(text, x, 0, 0x20FFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, light);
        font.drawInBatch(text, x, 0, 0xFF8FEFFF, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ArmorShowcaseBlockEntity showcase) {
        var p = showcase.getBlockPos();
        return new AABB(p.getX() - 0.5, p.getY(), p.getZ() - 0.5, p.getX() + 1.5, p.getY() + 2.8, p.getZ() + 1.5);
    }

    @Override
    public int getViewDistance() { return 48; }

    /**
     * 0.1.10.30: two or three short horizontal slices that jump aside for three ticks, now and then (per projector):
     * {lower y, upper y, shift} triples in blocks above the figure's feet; empty most of the time.
     */
    static float[] slices(long seed, float time) {
        long bucket = (long) Math.floor(time / 60F);
        long h = (bucket * 0x9E3779B97F4A7C15L) ^ (seed * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 29;
        float start = bucket * 60F + Math.floorMod(h, 57);                // one moment in each 3 s window
        if (time < start || time > start + 3F || Math.floorMod(h >>> 8, 3) == 0) return new float[0];
        int n = 2 + (int) Math.floorMod(h >>> 16, 2);
        float[] out = new float[n * 3];
        for (int i = 0; i < n; i++) {
            long k = h >>> (20 + i * 9);
            float y = 0.1F + Math.floorMod(k, 190) / 100F;
            float height = 0.05F + Math.floorMod(k >>> 3, 12) / 100F;
            out[i * 3] = y;
            out[i * 3 + 1] = y + height;
            out[i * 3 + 2] = (Math.floorMod(k >>> 5, 2) == 0 ? -1 : 1) * (0.025F + Math.floorMod(k >>> 7, 4) / 100F);
        }
        return out;
    }

    /**
     * The state of one hologram frame: {@code inverse} maps the camera space back to the figure (blocks, y up from its feet);
     * {@code buildY} is the height of the build-up scan line (-1 when built).
     */
    record Holo(org.joml.Matrix4f inverse, float flicker, float time, float buildY, float[] slices) {
        /** 0 above the build line, 1 below it. */
        float visible(float y) {
            if (buildY < 0) return 1F;
            float d = buildY - y;
            return d <= 0 ? 0F : Math.min(1F, d / 0.06F);
        }

        /** The bright front of the build line. */
        float front(float y) {
            if (buildY < 0) return 0F;
            float d = (y - buildY) / 0.05F;
            return (float) Math.exp(-d * d);
        }

        /** The scan band that rises through the figure (every 3.3 s). */
        float band(float y) {
            float c = (time * 0.015F % 1F) * 2.3F;
            float d = (y - c) / 0.12F;
            return (float) Math.exp(-d * d);
        }

        /** Sideways shift (camera space x) of a slice at {@code y}. */
        float shift(float y) {
            for (int i = 0; i + 2 < slices.length; i += 3) if (y >= slices[i] && y <= slices[i + 1]) return slices[i + 2];
            return 0F;
        }
    }

    /**
     * Buffer source of one hologram pass: every render type of the gear is drawn again with its texture in a translucent
     * full-bright type (body) or in the additive hologram grid (grid). Types without a texture and the enchantment glint
     * draw nothing.
     */
    static final class HoloSource implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final Holo holo;
        private final boolean grid;

        HoloSource(MultiBufferSource delegate, Holo holo, boolean grid) {
            this.delegate = delegate;
            this.holo = holo;
            this.grid = grid;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            var texture = RenderTypeTextures.of(type);
            if (texture.isEmpty() || RenderTypeTextures.isGlint(texture.get())) return NOTHING;
            RenderType target = grid ? NanoRender.HOLOGRID_FINE : RenderType.entityTranslucentEmissive(texture.get());
            return new HoloVertex(delegate.getBuffer(target), holo, grid);
        }
    }

    /** A consumer that drops every vertex. */
    static final VertexConsumer NOTHING = new VertexConsumer() {
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    };

    /**
     * Hologram vertex: the height above the figure's feet (through the inverse pose) sets the build-up, the scan band and the
     * slice shift of the vertex. Body: the gear colour tinted cyan, translucent. Grid: cyan light (additive).
     */
    static final class HoloVertex implements VertexConsumer {
        private final VertexConsumer delegate;
        private final Holo holo;
        private final boolean grid;
        private final org.joml.Vector3f local = new org.joml.Vector3f();
        private float y;

        HoloVertex(VertexConsumer delegate, Holo holo, boolean grid) {
            this.delegate = delegate;
            this.holo = holo;
            this.grid = grid;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            holo.inverse().transformPosition(x, y, z, local);
            this.y = local.y;
            delegate.addVertex(x + holo.shift(local.y), y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            float vis = holo.visible(y), front = holo.front(y), band = holo.band(y), f = holo.flicker();
            if (grid) {
                float k = Math.min(1F, (0.5F * vis + 0.55F * band * vis + 1.4F * front) * f);
                delegate.setColor((int) (90 * k), (int) (215 * k), (int) (255 * k), 255);
            } else {
                float k = (0.85F + 0.4F * band) * f;
                int a = (int) (255 * Math.min(1F, 0.36F * vis * f + 0.3F * front));
                delegate.setColor(Math.min(255, (int) (red * 0.5F * k + 40 * front)), Math.min(255, (int) (green * 0.92F * k + 60 * front)),
                    Math.min(255, (int) (blue * k + 60 * front)), a);
            }
            return this;
        }

        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    }

    /**
     * Blades of SlashBlade: Resharped in the hands of the hologram (0.1.10.6). Resharped draws a
     * held blade only through its own player layer: for every hand display context its item
     * renderer draws nothing, so on the projector (an armour stand) the blade was invisible. This
     * layer draws the blade part of the blade model at the hand, held like a vanilla sword: the
     * grip in the fist, the tip along the item's up axis (as the handheld display transform puts
     * a sword), with the same size as the Armory flight ({@link SlashBladeResharpedBridge#MODEL_SCALE}).
     */
    static final class BladeHandLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<ArmorStand, net.minecraft.client.model.ArmorStandArmorModel> {
        BladeHandLayer(net.minecraft.client.renderer.entity.RenderLayerParent<ArmorStand, net.minecraft.client.model.ArmorStandArmorModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, ArmorStand stand, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            renderHand(pose, buffers, light, stand.getMainHandItem(), net.minecraft.world.entity.HumanoidArm.RIGHT);
            renderHand(pose, buffers, light, stand.getOffhandItem(), net.minecraft.world.entity.HumanoidArm.LEFT);
        }

        private void renderHand(PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack, net.minecraft.world.entity.HumanoidArm arm) {
            if (!SlashBladeResharpedBridge.isBlade(stack)) return;
            pose.pushPose();
            // the frame of a held item, as vanilla ItemInHandLayer sets it up
            getParentModel().translateToHand(arm, pose);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            pose.translate((arm == net.minecraft.world.entity.HumanoidArm.LEFT ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            // where the handheld display transform puts the grip of a sword, then the blade:
            // tip (-x of blade.obj) onto +y, leaning 10 degrees like a held sword
            pose.translate(0.0F, -0.19F, 0.11F);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-10.0F));
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
            float s = SlashBladeResharpedBridge.MODEL_SCALE;
            pose.scale(s, s, s);
            pose.translate(-SlashBladeResharpedBridge.GRIP_X, 0, 0);
            SlashBladeResharpedBridge.renderBlade(stack, pose, buffers, light);
            pose.popPose();
        }
    }

    /** Armour stand renderer with the graphite mannequin texture (the body is invisible in hologram mode). */
    static final class MannequinRenderer extends ArmorStandRenderer {
        MannequinRenderer(EntityRendererProvider.Context context) {
            super(context);
            // 0.1.9.0: frame core attachments are part of the projected outfit
            addLayer(new com.miophas.singularity_iteration.common.client.suit.CoreAttachmentLayer<>(this));
            // 0.1.10.30: the light of a displayed nano or quantum suit
            addLayer(new com.miophas.singularity_iteration.common.client.suit.SuitGlowLayer<>(this));
            // 0.1.10.6: SlashBlade blades (their hand render contexts draw nothing on a non-player)
            addLayer(new BladeHandLayer(this));
        }

        @Override
        public ResourceLocation getTextureLocation(ArmorStand stand) {
            return MANNEQUIN;
        }

        @Override
        protected boolean shouldShowName(ArmorStand stand) {
            return false;
        }
    }
}
