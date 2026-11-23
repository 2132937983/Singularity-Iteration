// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity;
import com.miophas.singularity_iteration.common.builder.BuilderAdjustPacket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.185: placing a Blueprint Builder's blueprint in the world, as a schematic mod does, instead of the screen's six axis
 * buttons: the screen closes and the player looks at the hologram. The mouse wheel moves the blueprint a block along the
 * axis the player looks along most (towards where they look on wheel up; with Ctrl five blocks), {@link #ROTATE} turns it,
 * {@link #MIRROR} mirrors it; a right-click, Esc (0.1.11.186: without the pause screen), opening any screen or going
 * {@link BuilderAdjustPacket#REACH} blocks away ends it. A line over the hotbar names the keys and the offset.
 *
 * <p>A move shows at once: the client keeps the offset it asked for ({@link #delta}) and draws the box and the ghosts that far
 * ahead until the server's plan has it; a turn or a mirror shows when the new plan arrives (a fraction of a second).
 */
@OnlyIn(Dist.CLIENT)
public final class BuilderAdjust {
    private BuilderAdjust() {}

    public static KeyMapping ROTATE, MIRROR;
    /** Ticks a prediction the server has not confirmed is kept after placing ends (the server refused it: running). */
    static final int CONFIRM_TICKS = 60;

    @Nullable private static BlockPos active;
    private static final Map<BlockPos, int[]> EXPECTED = new HashMap<>();
    private static final Map<BlockPos, Long> CHANGED = new HashMap<>();
    private static long ticks;

    public static boolean active() { return active != null; }

    /** Starts placing the blueprint of {@code be} (the screen then closes). */
    public static void enter(BlueprintBuilderBlockEntity be) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (be.placedByCreate()) {
            player.displayClientMessage(Component.translatable("gui.mio_icif.builder.adjust.create"), true);
            return;
        }
        active = be.getBlockPos().immutable();
        EXPECTED.computeIfAbsent(active, k -> be.planOffset().clone());
        if (!be.preview()) send(BlueprintBuilderBlockEntity.A_PREVIEW, 0);
        if (BuilderGhosts.mode == BuilderGhosts.FRAME) BuilderGhosts.mode = BuilderGhosts.BLOCKS;
    }

    public static void exit() { active = null; }

    /** How far ahead of the plan placed with {@code placed} to draw a builder's box and ghosts (null: not at all). */
    @Nullable
    public static int[] delta(BlockPos builder, int[] placed) {
        int[] e = EXPECTED.get(builder);
        if (e == null || placed.length != 3) return null;
        int dx = e[0] - placed[0], dy = e[1] - placed[1], dz = e[2] - placed[2];
        return dx == 0 && dy == 0 && dz == 0 ? null : new int[]{dx, dy, dz};
    }

    private static void send(int action, int index) {
        if (active != null) PacketDistributor.sendToServer(new BuilderAdjustPacket(active, action, index));
    }

    /** Moves the blueprint {@code steps} blocks along {@code axis} (0 x, 1 y, 2 z) in {@code dir} (±1), within the range. */
    private static void move(int axis, int dir, int steps) {
        int[] e = EXPECTED.get(active);
        if (e == null) return;
        for (int k = 0; k < steps; k++) {
            int v = e[axis] + dir;
            if (Math.abs(v) > BlueprintBuilderBlockEntity.OFFSET_LIMIT) break;
            e[axis] = v;
            send(BlueprintBuilderBlockEntity.A_OFFSET, axis * 2 + (dir > 0 ? 1 : 0));
        }
        CHANGED.put(active, ticks);
    }

    @Nullable
    private static BlueprintBuilderBlockEntity builder() {
        var level = Minecraft.getInstance().level;
        return active != null && level != null && level.getBlockEntity(active) instanceof BlueprintBuilderBlockEntity b ? b : null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        // the predictions the server has confirmed (or refused) go
        if (mc.level != null)
            for (Iterator<Map.Entry<BlockPos, int[]>> it = EXPECTED.entrySet().iterator(); it.hasNext(); ) {
                var en = it.next();
                if (en.getKey().equals(active)) continue;
                boolean confirmed = mc.level.getBlockEntity(en.getKey()) instanceof BlueprintBuilderBlockEntity b && java.util.Arrays.equals(b.planOffset(), en.getValue());
                if (confirmed || ticks - CHANGED.getOrDefault(en.getKey(), 0L) > CONFIRM_TICKS) { it.remove(); CHANGED.remove(en.getKey()); }
            }
        if (active == null) {
            while (ROTATE != null && ROTATE.consumeClick()) { }
            while (MIRROR != null && MIRROR.consumeClick()) { }
            return;
        }
        BlueprintBuilderBlockEntity be = builder();
        if (mc.player == null || be == null || mc.screen != null || mc.player.distanceToSqr(active.getCenter()) > BuilderAdjustPacket.REACH * BuilderAdjustPacket.REACH) {
            exit();
            return;
        }
        while (ROTATE != null && ROTATE.consumeClick()) if (!be.running()) send(BlueprintBuilderBlockEntity.A_ROTATE, 0);
        while (MIRROR != null && MIRROR.consumeClick()) if (!be.running()) send(BlueprintBuilderBlockEntity.A_MIRROR, 0);
    }

    /** The mouse wheel moves the blueprint along the axis the player looks along most (and does not change the hotbar slot). */
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        BlueprintBuilderBlockEntity be = builder();
        if (be == null || mc.screen != null || mc.player == null || event.getScrollDeltaY() == 0) return;
        event.setCanceled(true);
        if (be.running()) return;
        Vec3 look = mc.player.getLookAngle();
        double ax = Math.abs(look.x), ay = Math.abs(look.y), az = Math.abs(look.z);
        int axis = ay >= ax && ay >= az ? 1 : ax >= az ? 0 : 2;
        double component = axis == 0 ? look.x : axis == 1 ? look.y : look.z;
        int dir = (component >= 0 ? 1 : -1) * (event.getScrollDeltaY() > 0 ? 1 : -1);
        move(axis, dir, Screen.hasControlDown() ? 5 : 1);
    }

    /**
     * 0.1.11.186: Esc ends placing without the pause screen (it opened over the world): the pause screen that opens while the
     * blueprint is placed is not shown (nor the one of the window losing focus), placing ends instead.
     */
    public static void opening(net.neoforged.neoforge.client.event.ScreenEvent.Opening event) {
        if (active == null || !(event.getNewScreen() instanceof net.minecraft.client.gui.screens.PauseScreen)) return;
        event.setCanceled(true);
        exit();
    }

    /** A right-click ends placing; neither click uses an item or breaks a block meanwhile. */
    public static void interact(InputEvent.InteractionKeyMappingTriggered event) {
        if (active == null || Minecraft.getInstance().screen != null) return;
        if (!event.isUseItem() && !event.isAttack()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isUseItem()) exit();
    }

    /** The line over the hotbar: what is placed where, the keys, and whether every block is in range. */
    public static void hud(RenderGuiEvent.Post event) {
        BlueprintBuilderBlockEntity be = builder();
        Minecraft mc = Minecraft.getInstance();
        if (be == null || mc.screen != null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int cx = g.guiWidth() / 2, y = g.guiHeight() - 92;
        int[] e = EXPECTED.getOrDefault(active, be.planOffset());
        Component state = Component.translatable("gui.mio_icif.builder.adjust.state", e[0], e[1], e[2], be.rotationSteps() * 90,
            Component.translatable("gui.mio_icif.builder.mirror." + be.mirrorMode()));
        Component title = Component.translatable("gui.mio_icif.builder.adjust.title").withStyle(ChatFormatting.AQUA).append(" · ").append(state);
        Component keys = Component.translatable("gui.mio_icif.builder.adjust.keys", ROTATE.getTranslatedKeyMessage(), MIRROR.getTranslatedKeyMessage())
            .withStyle(ChatFormatting.GRAY);
        Component[] lines = be.running() ? new Component[]{title, keys,
            Component.translatable("gui.mio_icif.builder.adjust.running").withStyle(ChatFormatting.GOLD)} : new Component[]{title, keys};
        int i = 0;
        for (Component c : lines) {
            g.drawString(mc.font, c, cx - mc.font.width(c) / 2, y + i * 11, 0xFFFFFF, true);
            i++;
        }
    }

    public static void clear() {
        active = null;
        EXPECTED.clear();
        CHANGED.clear();
    }
}
