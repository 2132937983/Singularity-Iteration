package com.miophas.singularity_iteration.common.client.area;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.area.WorkArea;
import com.miophas.singularity_iteration.common.area.WorkAreaProvider;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.network.AreaPreviewPacket;
import com.miophas.singularity_iteration.common.network.AreaPreviewRequestPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client half of the Area Preview: keeps the work areas the server sent, draws them as
 * translucent boxes with crisp outlines, marks their corners with a few dust particles,
 * and adds a small toggle button above the GUI of every machine that has a range.
 *
 * <p>Colour language: yellow mining, red defence, cyan fluid, green collection, light-green
 * farming, brown terraforming, purple chunk loading, blue field checks. The machine's own
 * main volume is filled, single cells (pump intake, found source) are filled stronger, and
 * outer search limits are drawn as outlines only.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public final class AreaPreviewClient {
    private AreaPreviewClient() {}

    private static final double MAX_RENDER_DISTANCE = 256.0;
    private static final int PARTICLE_PERIOD = 10;

    private record Shown(List<WorkArea> areas, long until) { }
    private static final Map<BlockPos, Shown> SHOWN = new HashMap<>();
    private static ClientLevel boundLevel;

    // last machine the local player right-clicked (decides which GUIs get the button)
    private static BlockPos lastClicked;
    private static long lastClickedTime = Long.MIN_VALUE;

    // ------------------------------------------------------------------ state
    public static void accept(AreaPreviewPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        bind(level);
        if (packet.entries().isEmpty() && packet.ticks() == 0) {
            SHOWN.clear();
            return;
        }
        long until = level.getGameTime() + Math.max(20, packet.ticks());
        for (AreaPreviewPacket.Entry entry : packet.entries()) {
            if (packet.toggle() && SHOWN.containsKey(entry.machine())) SHOWN.remove(entry.machine());
            else SHOWN.put(entry.machine(), new Shown(List.copyOf(entry.areas()), until));
        }
    }

    public static boolean isShown(BlockPos pos) {
        return SHOWN.containsKey(pos);
    }

    private static void bind(ClientLevel level) {
        if (boundLevel != level) {
            SHOWN.clear();
            boundLevel = level;
        }
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) { SHOWN.clear(); boundLevel = null; return; }
        bind(level);
        if (SHOWN.isEmpty()) return;
        long now = level.getGameTime();
        SHOWN.values().removeIf(s -> s.until <= now);
        if (now % PARTICLE_PERIOD != 0) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        for (Shown shown : SHOWN.values()) {
            for (WorkArea area : shown.areas) {
                if (area.kind() == WorkArea.ENVELOPE) continue;
                AABB b = area.box();
                if (b.getCenter().distanceToSqr(player.position()) > 96 * 96 && !b.contains(player.position())) continue;
                DustParticleOptions dust = new DustParticleOptions(rgb(area.color()), 1.2F);
                // corners of the box, clamped vertically near the player so very tall
                // mining columns still show markers where the player can see them
                double lowY = Math.max(b.minY, player.getY() - 24), highY = Math.min(b.maxY, player.getY() + 24);
                if (lowY > highY) { lowY = b.minY; highY = b.maxY; }
                for (int i = 0; i < 4; i++) {
                    double x = (i & 1) == 0 ? b.minX : b.maxX, z = (i & 2) == 0 ? b.minZ : b.maxZ;
                    level.addParticle(dust, x, lowY, z, 0, 0, 0);
                    level.addParticle(dust, x, highY, z, 0, 0, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------ rendering
    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || SHOWN.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != boundLevel) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float t = (mc.level.getGameTime() % 40 + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 40.0F;
        float pulse = 0.75F + 0.25F * (float) Math.sin(t * Math.PI * 2);

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer fill = buffers.getBuffer(RenderType.debugFilledBox());
        for (Shown shown : SHOWN.values()) {
            for (WorkArea area : shown.areas) {
                if (area.kind() == WorkArea.ENVELOPE || !visible(event, area.box(), cam)) continue;
                Vector3f c = rgb(area.color());
                float alpha = (area.kind() == WorkArea.MARKER ? 0.30F : 0.10F) * pulse;
                AABB b = area.box().deflate(0.002);
                LevelRenderer.addChainedFilledBoxVertices(pose, fill, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, c.x, c.y, c.z, alpha);
            }
        }
        buffers.endBatch(RenderType.debugFilledBox());

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Shown shown : SHOWN.values()) {
            for (WorkArea area : shown.areas) {
                if (!visible(event, area.box(), cam)) continue;
                Vector3f c = rgb(area.color());
                float alpha = area.kind() == WorkArea.ENVELOPE ? 0.45F : 0.95F;
                LevelRenderer.renderLineBox(pose, lines, area.box().deflate(0.001), c.x, c.y, c.z, alpha);
            }
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static boolean visible(RenderLevelStageEvent event, AABB box, Vec3 cam) {
        if (box.contains(cam)) return true;
        double dx = Math.max(0, Math.max(box.minX - cam.x, cam.x - box.maxX));
        double dy = Math.max(0, Math.max(box.minY - cam.y, cam.y - box.maxY));
        double dz = Math.max(0, Math.max(box.minZ - cam.z, cam.z - box.maxZ));
        if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) return false;
        return event.getFrustum().isVisible(box);
    }

    private static Vector3f rgb(int color) {
        return new Vector3f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }

    // ------------------------------------------------------------------ GUI button
    @SubscribeEvent
    static void rememberClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof WorkAreaProvider) {
            lastClicked = event.getPos().immutable();
            lastClickedTime = event.getLevel().getGameTime();
        } else {
            lastClicked = null;
        }
    }

    @SubscribeEvent
    static void addButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || lastClicked == null || mc.level.getGameTime() - lastClickedTime > 100) return;
        var menuType = screen.getMenu().getType();
        var key = menuType == null ? null : BuiltInRegistries.MENU.getKey(menuType);
        if (key == null || !Singularity_Iteration.MOD_ID.equals(key.getNamespace())) return;
        if (!(mc.level.getBlockEntity(lastClicked) instanceof WorkAreaProvider)) return;
        BlockPos target = lastClicked;
        int containerId = screen.getMenu().containerId;
        if (screen instanceof com.miophas.singularity_iteration.common.client.screen.mio_icif_screen<?> si) {
            // left utility dock: keeps the upper-right corner free for inventory-sorting mods
            si.addDockButton(new com.miophas.singularity_iteration.common.client.screen.mio_icif_screen.DockButton((g, x, y, hov) -> {
                int c = hov ? 0xFF3A6EA5 : 0xFF2A2E33, a = 0xFF5FD3F5;
                for (int i = 0; i < 12; i += 3) {          // dashed work-area square
                    g.fill(x + 2 + i, y + 2, x + 4 + i, y + 3, c);
                    g.fill(x + 2 + i, y + 13, x + 4 + i, y + 14, c);
                    g.fill(x + 2, y + 2 + i, x + 3, y + 4 + i, c);
                    g.fill(x + 13, y + 2 + i, x + 14, y + 4 + i, c);
                }
                g.fill(x + 6, y + 6, x + 10, y + 10, a);   // the machine in the middle
            }, () -> java.util.List.of(Component.translatable("gui.mio_icif.area_preview.button")),
                () -> PacketDistributor.sendToServer(new AreaPreviewRequestPacket(containerId))));
            return;
        }
        int x = Math.max(0, screen.getGuiLeft() - 17);   // outside the left edge, never the top-right corner
        int y = screen.getGuiTop() + 4;
        SiButton button = new SiButton(x, y, 15, 12, label(target), b -> {
            PacketDistributor.sendToServer(new AreaPreviewRequestPacket(containerId));
        });
        button.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.area_preview.button")));
        event.addListener(button);
    }

    private static Component label(BlockPos pos) {
        return Component.literal("⬚");
    }
}