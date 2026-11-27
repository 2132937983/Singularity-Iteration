// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets;
import com.miophas.singularity_iteration.common.armory.WingmanCommands;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanPilot;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.11.60 (gaze and Iron Man phase P8): remote piloting on the client. The pilot key (default U) asks to fly the selected
 * Wingman (else the nearest; the server decides, {@code WingmanPilot}); pressed again it leaves. While piloting:
 * <ul>
 *   <li>the view is the Wingman's (camera entity), turned at once by the mouse (the angles come from the local player's
 *       look, so the turn has no network lag), no hand is drawn, and the own body is seen from outside;</li>
 *   <li>the body does not move (its movement input is cleared), attack and use do not act on the world (they are sent as
 *       the Wingman's controls), and every tick the controls go to the server;</li>
 *   <li>a line on top of the screen shows the Wingman, the link distance and the key to leave.</li>
 * </ul>
 * The server ends the flight on a broken link or a hurt body; the view then returns.
 */
public final class PilotClient {
    private PilotClient() {}

    public static KeyMapping KEY;
    private static int piloted = -1, missing;
    private static boolean useQueued;

    public static boolean piloting() { return piloted >= 0; }

    static void accept(WingmanPilot.State state) {
        piloted = state.entityId();
        missing = 0;
        if (piloted < 0) restore();
    }

    private static void restore() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getCameraEntity() != mc.player) mc.setCameraEntity(mc.player);
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        static void keys(RegisterKeyMappingsEvent event) {
            // 0.1.11.230 (key consolidation): unbound by default; the command wheel's suit fan starts the pilot
            KEY = new KeyMapping("key.mio_icif.pilot", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.mio_icif");
            event.register(KEY);
        }

        @SubscribeEvent
        static void setup(FMLClientSetupEvent event) { WingmanPilot.HOOK = PilotClient::accept; }

        @SubscribeEvent
        static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
                "wingman_pilot"), PilotClient::render);
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            while (KEY != null && KEY.consumeClick()) {
                if (mc.screen != null) continue;
                if (piloting()) SquadClient.send(WingmanCommands.A_PILOT_EXIT, WingmanCommandPackets.ALL, (byte) 0, null, -1);
                else {
                    UUID unit = SquadClient.selected();
                    SquadClient.send(WingmanCommands.A_PILOT, unit == null ? WingmanCommandPackets.ALL : unit, (byte) 0, null, -1);
                }
            }
            if (!piloting()) return;
            Entity e = mc.level == null ? null : mc.level.getEntity(piloted);
            if (!(e instanceof WingmanEntity)) {
                if (++missing > 40) SquadClient.send(WingmanCommands.A_PILOT_EXIT, WingmanCommandPackets.ALL, (byte) 0, null, -1);
                return;
            }
            missing = 0;
            if (mc.getCameraEntity() != e) mc.setCameraEntity(e);
            var o = mc.options;
            float forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
            float strafe = (o.keyRight.isDown() ? 1 : 0) - (o.keyLeft.isDown() ? 1 : 0);
            float vertical = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
            boolean attack = mc.screen == null && o.keyAttack.isDown();
            var input = new WingmanPilot.Input(mc.screen == null ? forward : 0, mc.screen == null ? strafe : 0, mc.screen == null ? vertical : 0,
                o.keySprint.isDown(), mc.player.getYRot(), mc.player.getXRot(), attack, useQueued);
            useQueued = false;
            if (mc.getConnection() != null && mc.getConnection().hasChannel(WingmanPilot.Input.TYPE)) PacketDistributor.sendToServer(input);
        }

        /** The body stays where it is. */
        @SubscribeEvent
        static void input(MovementInputUpdateEvent event) {
            if (!piloting()) return;
            var in = event.getInput();
            in.forwardImpulse = 0;
            in.leftImpulse = 0;
            in.up = in.down = in.left = in.right = in.jumping = in.shiftKeyDown = false;
        }

        /** Attack and use are the Wingman's controls, not the body's. */
        @SubscribeEvent
        static void interact(InputEvent.InteractionKeyMappingTriggered event) {
            if (!piloting()) return;
            if (event.isUseItem()) useQueued = true;
            event.setSwingHand(false);
            event.setCanceled(true);
        }

        /** The mouse turns the view at once (the local look; the Wingman follows on the server). */
        @SubscribeEvent
        static void angles(ViewportEvent.ComputeCameraAngles event) {
            Minecraft mc = Minecraft.getInstance();
            if (!piloting() || mc.player == null || !(mc.getCameraEntity() instanceof WingmanEntity)) return;
            float pt = (float) event.getPartialTick();
            event.setYaw(mc.player.getViewYRot(pt));
            event.setPitch(mc.player.getViewXRot(pt));
        }

        @SubscribeEvent
        static void hand(RenderHandEvent event) {
            if (piloting()) event.setCanceled(true);
        }

        @SubscribeEvent
        static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            piloted = -1;
            restore();
        }
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!piloting() || mc.player == null || mc.options.hideGui || mc.level == null) return;
        Entity e = mc.level.getEntity(piloted);
        if (!(e instanceof WingmanEntity w)) return;
        int dist = (int) w.distanceTo(mc.player), max = (int) WingmanPilot.RANGE;
        String name = SquadClient.units().stream().filter(u -> u.entityId() == piloted).findFirst().map(SquadClient::label).orElse("W?");
        String line = Component.translatable("gui.mio_icif.pilot.hud", name, dist, max, KEY.getTranslatedKeyMessage()).getString();
        float cx = g.guiWidth() / 2F, warn = dist > max * 0.8F ? 1 : 0;
        float wdt = FcsDraw.width(line, 0.7F);
        g.fill((int) (cx - wdt / 2 - 6), 6, (int) (cx + wdt / 2 + 6), 18, FcsDraw.argb(0x01060C, 0.6F));
        FcsDraw.textCentered(g, line, cx, 9, FcsDraw.argb(warn > 0 ? 0xFFB347 : 0x6FF3D6, 1F), 0.7F);
        // a thin frame round the screen: the view is a remote feed
        int w2 = g.guiWidth(), h2 = g.guiHeight(), col = FcsDraw.argb(0x6FF3D6, 0.35F);
        g.fill(2, 2, w2 - 2, 3, col);
        g.fill(2, h2 - 3, w2 - 2, h2 - 2, col);
        g.fill(2, 2, 3, h2 - 2, col);
        g.fill(w2 - 3, 2, w2 - 2, h2 - 2, col);
    }
}
