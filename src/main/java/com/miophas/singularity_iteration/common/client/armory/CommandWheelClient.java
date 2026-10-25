// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.client.input.SharedGKeyContext;
import com.miophas.singularity_iteration.common.client.mio_icif_ClientEvents;
import com.miophas.singularity_iteration.core.api.tool.ToggleableElectricTool;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.mojang.blaze3d.platform.InputConstants;
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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Key and set-up of the quantum command (0.1.10.2). Hold the command wheel key (default G) to open
 * the wheel; release it on a sector to give the order. G is shared with the tool mode toggle by
 * context: with a toggleable electric tool in a hand, G toggles the tool as before.
 *
 * <p>When the wheel opens, the point and the creature in the crosshair are captured (256 blocks):
 * MOVE, HOLD and SUPPRESS use the point, FOCUS the creature. A look at the sky gives a point in the
 * air 48 blocks ahead.
 */
public final class CommandWheelClient {
    private CommandWheelClient() {}

    public static KeyMapping KEY;
    /** Range of the target capture in blocks. */
    static final double CAPTURE_RANGE = 256, SKY_POINT = 48;

    /** Point and creature in the crosshair when the wheel opened. */
    public record Target(Vec3 point, boolean sky, int entityId, @Nullable Component entityName, double distance) { }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        static void keys(RegisterKeyMappingsEvent event) {
            KEY = new KeyMapping("key.mio_icif.command_wheel", SharedGKeyContext.INSTANCE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
                "key.category.mio_icif");
            event.register(KEY);
        }

        @SubscribeEvent
        static void setup(FMLClientSetupEvent event) {
            WingmanCommandPackets.SQUAD_HOOK = SquadClient::accept;
        }

        @SubscribeEvent
        static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
                "squad_hud"), SquadHud::render);
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            SquadClient.ticks++;
            CommandFx.tick();
            HoloTable.tick();
            Minecraft mc = Minecraft.getInstance();
            while (KEY != null && KEY.consumeClick()) {
                LocalPlayer player = mc.player;
                if (player == null || mc.screen != null) continue;
                if (sharesWithTool() && holdsToggleTool(player)) continue;    // the tool toggle takes this press
                if (!SquadClient.commander()) {
                    if (wearsQuantum(player)) player.displayClientMessage(Component.translatable("message.mio_icif.command.no_relay"), true);
                    continue;
                }
                mc.setScreen(new CommandWheelScreen(capture(player)));
            }
        }

        @SubscribeEvent
        static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            SquadClient.clear();
            CommandFx.clear();
            HoloTable.close(true);
        }
    }

    /** True when the wheel key and the tool toggle key are bound to the same key. */
    static boolean sharesWithTool() {
        KeyMapping tool = mio_icif_ClientEvents.TOOL_TOGGLE_KEY;
        return tool != null && KEY != null && tool.same(KEY);
    }

    static boolean holdsToggleTool(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof ToggleableElectricTool || player.getOffhandItem().getItem() instanceof ToggleableElectricTool;
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
