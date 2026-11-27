// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets;
import com.miophas.singularity_iteration.common.armory.WingmanCommands;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanOrder;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Key and set-up of the quantum command:
 * <ul>
 *   <li><b>Tap</b> the command key (default {@code X}, independent of tool toggle {@code G}):
 *       <b>Smart Ping</b> without opening a screen — aiming at an enemy focuses fire, aiming at a
 *       block moves &amp; holds that position, and aiming at the sky recalls anchored Wingmen to
 *       form up (or sends them to the air point if already in formation).</li>
 *   <li><b>Hold</b> the command key ({@value #HOLD_TICKS} ticks): opens the streamlined 8-sector
 *       tactical command wheel; releasing on a sector executes that order.</li>
 * </ul>
 */
public final class CommandWheelClient {
    private CommandWheelClient() {}

    public static KeyMapping KEY;
    /** Range of the target capture in blocks. */
    static final double CAPTURE_RANGE = 256, SKY_POINT = 48;
    /** Holding the command key for this many ticks opens the wheel; a shorter tap runs Smart Ping. */
    static final int HOLD_TICKS = 5;

    /** Point and creature in the crosshair when the command triggered. */
    public record Target(Vec3 point, boolean sky, int entityId, @Nullable Component entityName, double distance) { }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        static void keys(RegisterKeyMappingsEvent event) {
            KEY = new KeyMapping("key.mio_icif.command_wheel", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X,
                "key.category.mio_icif");
            event.register(KEY);
        }

        @SubscribeEvent
        static void setup(FMLClientSetupEvent event) {
            WingmanCommandPackets.SQUAD_HOOK = SquadClient::accept;
            com.miophas.singularity_iteration.common.armory.WingmanCallouts.HOOK = CalloutHud::accept;   // 0.1.11.58 (P6)
            com.miophas.singularity_iteration.common.armory.WingmanEntity.CLASSIC_FX =
                com.miophas.singularity_iteration.common.client.suit.FcsClientConfig::classicFlightFx;   // 0.1.11.62 (Q1)
            // 0.1.11.59 (P7): a Clean Slate burst is drawn as a firework explosion (no rocket entity, no damage)
            com.miophas.singularity_iteration.common.armory.WingmanCleanSlate.HOOK = b -> {
                var level = net.minecraft.client.Minecraft.getInstance().level;
                if (level == null) return;
                // 0.1.11.74 (W4): the phase burst (0.1.11.64: constructivist); the classic style keeps the firework
                if (com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.classicFlightFx())
                    level.createFireworks(b.at().x, b.at().y, b.at().z, 0, 0, 0, b.explosions());
                else com.miophas.singularity_iteration.common.client.render.PhaseBursts.cleanSlate(b.at());
            };
        }

        @SubscribeEvent
        static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
                "squad_hud"), SquadHud::render);
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
                "wingman_callouts"), CalloutHud::render);   // 0.1.11.58 (P6)
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            SquadClient.ticks++;
            CommandFx.tick();
            WingmanEngineSound.tick(Minecraft.getInstance());   // 0.1.11.65 (Q4)
            HoloTable.tick();
            Minecraft mc = Minecraft.getInstance();
            if (waitRelease && !HeldKeys.down(KEY)) waitRelease = false;
            LocalPlayer player = mc.player;
            // Consume any queued click events so vanilla doesn't buffer them
            boolean pressedThisTick = false;
            while (KEY != null && KEY.consumeClick()) pressedThisTick = true;

            if (player == null || mc.screen != null || waitRelease) {
                holdTicks = 0;
                return;
            }
            boolean down = HeldKeys.down(KEY);
            // 0.1.11.230 (key consolidation): the wheel opens for anyone (its gear and favourites fans need no command link;
            // the squad sectors are grey without one); only the tap (Smart Ping) needs the link
            if (pressedThisTick && holdTicks == 0) holdTicks = 1;
            if (holdTicks > 0) {
                if (down) {
                    if (++holdTicks >= HOLD_TICKS) {
                        holdTicks = 0;
                        mc.setScreen(new CommandWheelScreen(capture(player)));
                    }
                } else {
                    // Released before HOLD_TICKS: execute instant Smart Ping!
                    holdTicks = 0;
                    smartPing(player);
                }
            }
        }

        @SubscribeEvent
        static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            SquadClient.clear();
            CommandFx.clear();
            HoloTable.close(true);
            ArmoryToastOverlay.clear();   // 0.1.11.40 (U-9)
            waitRelease = false;
            holdTicks = 0;
        }
    }

    /** 0.1.11.40: set when a quantum command screen closes while the wheel key is held; cleared at its release. */
    private static boolean waitRelease;
    /** Ticks the command key has been held down in-game (for tap = Smart Ping vs hold = Wheel). */
    private static int holdTicks;

    /** Called when the wheel or the holo table closes: a still held wheel key must not reopen the wheel. */
    static void waitForRelease() {
        waitRelease = HeldKeys.down(KEY);
        holdTicks = 0;
    }

    /** Instant context-sensitive Smart Ping (tap command key without opening the wheel). */
    static void smartPing(LocalPlayer player) {
        if (!SquadClient.commander()) {
            if (wearsQuantum(player)) player.displayClientMessage(Component.translatable("message.mio_icif.command.no_relay"), true);
            return;
        }
        WingmanCommandPackets.Unit one = SquadClient.selectedUnit();
        List<WingmanCommandPackets.Unit> sel = new ArrayList<>();
        for (WingmanCommandPackets.Unit u : SquadClient.orderable()) {
            if (one == null || u.id().equals(one.id())) sel.add(u);
        }
        if (sel.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mio_icif.command.no_unit"), true);
            return;
        }
        Target t = capture(player);
        var unit = SquadClient.selected();
        if (t.entityId() >= 0) {
            SquadClient.send(WingmanCommands.A_FOCUS, unit, (byte) 0, null, t.entityId());
        } else if (!t.sky()) {
            SquadClient.send(WingmanCommands.A_MOVE, unit, (byte) 0, t.point(), -1);
        } else if (sel.stream().anyMatch(u -> SquadClient.order(u) != WingmanOrder.NONE)) {
            SquadClient.send(WingmanCommands.A_FORM_UP, unit, (byte) 0, null, -1);
        } else {
            SquadClient.send(WingmanCommands.A_MOVE, unit, (byte) 0, t.point(), -1);
        }
    }

    static boolean wearsQuantum(LocalPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (SuitModules.isQuantumPiece(player.getItemBySlot(slot))) return true;
        }
        return false;
    }

    /** The point and creature in the crosshair (see the class comment). */
    static Target capture(LocalPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F), look = player.getViewVector(1.0F);
        Vec3 far = eye.add(look.scale(CAPTURE_RANGE));
        HitResult block = player.level().clip(new ClipContext(eye, far, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1.5),
            e -> e instanceof LivingEntity && e != player && !(e instanceof WingmanEntity) && e.isAlive() && !e.isSpectator(), eye.distanceToSqr(end));
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return new Target(living.position(), false, living.getId(), living.getDisplayName(), eye.distanceTo(living.position()));
        }
        if (block.getType() == HitResult.Type.MISS) {
            Vec3 air = eye.add(look.scale(SKY_POINT));
            return new Target(air, true, -1, null, SKY_POINT);
        }
        return new Target(block.getLocation(), false, -1, null, eye.distanceTo(block.getLocation()));
    }
}
