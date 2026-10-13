// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.nano.NanoBursts;
import com.miophas.singularity_iteration.common.suit.nano.NanoCoreState;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.common.suit.nano.NanoSuit;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Client controller of the nano suit (0.1.8.5): burst keys (V primary, B secondary), the synced
 * nano core state, the Kinetic Dash motion of the local player and the effect timeline that
 * {@link NanoFx} and {@link NanoHud} draw.
 */
public final class NanoClient {
    private NanoClient() {}

    public static KeyMapping PRIMARY_KEY, SECONDARY_KEY;
    /** 0.1.9.0: core burst key (arc discharge of the advanced propulsion core). */
    public static KeyMapping CORE_KEY;
    /** Tick of the last own arc discharge (cooldown display) and of the last refusal and its reason. */
    static int dischargeAt = -10_000, coreDeniedAt = -100, coreDeniedReason;

    /** Last core state from the server (null before the first sync or without a nano piece). */
    @Nullable static NanoPackets.CoreSync sync;
    /** Client tick counter (effect timeline). */
    static int ticks;
    /** Local Kinetic Dash: tick index (-1 = none), direction and length. */
    static int dashTick = -1;
    static Vec3 dashDir = Vec3.ZERO;
    static float dashDistance;
    /** HUD feedback: per burst key, the tick of the last use and of the last refusal and its reason. */
    static final int[] usedAt = {-100, -100}, deniedAt = {-100, -100}, deniedReason = {0, 0};
    static int lastSlot;
    static int overheatAt = -100;
    static boolean hfSync;
    static int hfSyncSince;
    /** 0.1.8.6: HF sync level of the local player (0..3). */
    static int hfLevel;
    /** Shock gel: tick of the last landing from 2 blocks or more; fall distance seen while airborne. */
    static int landedAt = -100;
    private static float airFall;
    private static boolean wasOnGround = true;

    /** Speed profile of the dash over {@link NanoBursts#DASH_TICKS} ticks (sum normalised at use). */
    private static final float[] DASH_PROFILE = {1.0F, 1.0F, 0.92F, 0.78F, 0.56F, 0.34F};

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        static void keys(RegisterKeyMappingsEvent event) {
            PRIMARY_KEY = new KeyMapping("key.mio_icif.nano_burst_primary", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.category.mio_icif");
            SECONDARY_KEY = new KeyMapping("key.mio_icif.nano_burst_secondary", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.category.mio_icif");
            CORE_KEY = new KeyMapping("key.mio_icif.core_burst", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.category.mio_icif");
            event.register(PRIMARY_KEY);
            event.register(SECONDARY_KEY);
            event.register(CORE_KEY);
        }

        @SubscribeEvent
        static void shaders(RegisterShadersEvent event) {
            NanoRender.registerShaders(event);
        }

        @SubscribeEvent
        static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "nano_hud"), NanoHud::render);
        }

        @SubscribeEvent
        static void setup(FMLClientSetupEvent event) {
            NanoPackets.SYNC_HOOK = NanoClient::acceptSync;
            NanoPackets.FX_HOOK = NanoClient::acceptFx;
            NanoSuit.CLIENT_VIEW = player -> {
                Minecraft mc = Minecraft.getInstance();
                NanoPackets.CoreSync s = sync;
                if (s == null || player != mc.player) return null;
                NanoCoreState view = new NanoCoreState();
                view.reserve = s.reserve();
                view.heat = s.heat();
                view.overheated = s.overheated();
                view.primary = s.primaryChoice();
                view.secondary = s.secondaryChoice();
                view.camoTicks = s.camoTicks();
                return view;
            };
        }
    }

    // ------------------------------------------------------------------ packets

    static void acceptSync(NanoPackets.CoreSync s) {
        sync = s;
    }

    static void acceptFx(NanoPackets.Fx fx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        boolean self = mc.player != null && fx.entityId() == mc.player.getId();
        switch (fx.kind()) {
            case NanoPackets.FX_DASH -> {
                if (self) {
                    dashDir = new Vec3(fx.a(), 0, fx.b());
                    dashDistance = fx.c();
                    dashTick = 0;
                    usedAt[lastSlot] = ticks;
                }
                NanoFx.startDash(fx, self);
            }
            case NanoPackets.FX_EMP -> {
                if (self) usedAt[lastSlot] = ticks;
                NanoFx.startEmp(fx, self);
            }
            case NanoPackets.FX_CAMO_ON -> {
                if (self) usedAt[lastSlot] = ticks;
                NanoFx.camoOn(fx.entityId(), fx.a() > 0.5F, true);
            }
            case NanoPackets.FX_CAMO_KEEP -> NanoFx.camoOn(fx.entityId(), fx.a() > 0.5F, false);
            case NanoPackets.FX_CAMO_OFF -> NanoFx.camoOff(fx.entityId(), (int) fx.a());
            case NanoPackets.FX_OVERHEAT -> {
                if (self) overheatAt = ticks;
                NanoFx.overheat(fx.entityId());
            }
            case NanoPackets.FX_DENIED -> {
                if (self) deny((int) fx.a());
            }
            case NanoPackets.FX_MESH_ON -> {
                if (self) usedAt[lastSlot] = ticks;
                NanoFx.meshOn(fx.entityId(), (int) fx.a(), (int) fx.b());
            }
            case NanoPackets.FX_MESH_OFF -> NanoFx.meshOff(fx.entityId());
            case NanoPackets.FX_MESH_HIT -> NanoFx.meshHit(fx.entityId(), new Vec3(fx.a(), fx.b(), fx.c()));
            case NanoPackets.FX_RECON -> {
                if (self) {
                    usedAt[lastSlot] = ticks;
                    NanoFx.recon(fx.targets(), (int) fx.a());
                }
            }
            case NanoPackets.FX_RECON_CLOUD -> NanoFx.reconCloud(fx);
            case NanoPackets.FX_REBOUND -> NanoFx.rebound(fx);
            case NanoPackets.FX_DISCHARGE -> {
                if (self) dischargeAt = ticks;
                NanoFx.startDischarge(fx, self);
            }
            case NanoPackets.FX_CORE_DENIED -> {
                if (self) {
                    coreDeniedAt = ticks;
                    coreDeniedReason = (int) fx.a();
                    play(mio_icif_sounds.NANO_DENIED.get(), 0.85F, 0.6F);
                }
            }
            default -> { }
        }
    }

    private static void deny(int reason) {
        deniedAt[lastSlot] = ticks;
        deniedReason[lastSlot] = reason;
        play(mio_icif_sounds.NANO_DENIED.get(), 1.0F, 0.6F);
    }

    static void play(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    // ------------------------------------------------------------------ ticks

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) {
                clear();
                return;
            }
            ticks++;
            while (PRIMARY_KEY != null && PRIMARY_KEY.consumeClick()) press(mc, player, 0);
            while (SECONDARY_KEY != null && SECONDARY_KEY.consumeClick()) press(mc, player, 1);
            while (CORE_KEY != null && CORE_KEY.consumeClick()) pressCore(mc, player);
            int level = NanoSuit.hfLevel(player);
            if (level > hfLevel) hfSyncSince = ticks;
            hfLevel = level;
            hfSync = level > 0;
            NanoFx.tick(mc, ticks);
        }

        /** The local Kinetic Dash: the client drives its own motion (smooth, no rubber band). */
        @SubscribeEvent
        static void playerTick(PlayerTickEvent.Pre event) {
            if (!(event.getEntity() instanceof LocalPlayer player) || dashTick < 0) return;
            if (dashTick >= DASH_PROFILE.length || player.isPassenger() || player.getAbilities().flying) {
                dashTick = -1;
                return;
            }
            float sum = 0;
            for (float w : DASH_PROFILE) sum += w;
            float speed = dashDistance * DASH_PROFILE[dashTick] / sum;
            Vec3 v = player.getDeltaMovement();
            double vy = player.onGround() ? 0.0 : Math.max(v.y, -0.04);
            player.setDeltaMovement(dashDir.x * speed, vy, dashDir.z * speed);
            player.fallDistance = 0;
            dashTick++;
        }

        /**
         * 0.1.8.6 local movement units (the client owns its motion): gecko setae climb and cling,
         * ice grip, and the shock gel landing window.
         */
        @SubscribeEvent
        static void playerTickPost(PlayerTickEvent.Post event) {
            if (!(event.getEntity() instanceof LocalPlayer player)) return;
            // shock gel: remember a landing from 2 blocks or more
            if (!player.onGround()) airFall = Math.max(airFall, player.fallDistance);
            else {
                if (!wasOnGround && airFall >= 2) landedAt = ticks;
                airFall = 0;
            }
            wasOnGround = player.onGround();
            int gecko = NanoSuit.level(player, com.miophas.singularity_iteration.common.suit.SuitModuleType.GECKO_SETAE);
            if (gecko == 0 || player.getAbilities().flying || player.isPassenger()) return;
            Vec3 v = player.getDeltaMovement();
            if (player.isShiftKeyDown() && touchesWall(player)) {
                double climb = gecko >= 2 ? 0.3 : 0.2;
                if (player.horizontalCollision) player.setDeltaMovement(v.x, climb, v.z);
                else player.setDeltaMovement(v.x, gecko >= 2 ? 0.0 : Math.max(v.y, -0.08), v.z);   // Mk.II clings, Mk.I slides slowly
                player.fallDistance = 0;
            } else if (player.onGround()) {
                // grip: on slippery blocks the feet hold like on stone
                var below = player.getBlockPosBelowThatAffectsMyMovement();
                float friction = player.level().getBlockState(below).getFriction(player.level(), below, player);
                if (friction > 0.7F) player.setDeltaMovement(v.x * 0.6 / friction, v.y, v.z * 0.6 / friction);
            }
        }

        /** Shock gel rebound: a jump right after a landing goes higher (Mk.I x1.5, Mk.II x1.8). */
        @SubscribeEvent
        static void jump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
            if (!(event.getEntity() instanceof LocalPlayer player) || ticks - landedAt > com.miophas.singularity_iteration.common.suit.nano.NanoEvents.REBOUND_WINDOW) return;
            int gel = NanoSuit.level(player, com.miophas.singularity_iteration.common.suit.SuitModuleType.SHOCK_GEL);
            if (gel == 0) return;
            landedAt = -100;
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 1.15, v.y * (gel >= 2 ? 1.8 : 1.5), v.z * 1.15);
        }

        /** The view widens during a dash (space stretch). */
        @SubscribeEvent
        static void fov(ComputeFovModifierEvent event) {
            if (dashTick < 0 || dashTick > DASH_PROFILE.length) return;
            float k = DASH_PROFILE[Math.min(DASH_PROFILE.length - 1, dashTick)];
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.12F * k * NanoFxIntensity.get()));
        }

        @SubscribeEvent
        static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            clear();
        }
    }

    private static void press(Minecraft mc, LocalPlayer player, int slot) {
        if (mc.screen != null || player.isSpectator()) return;
        lastSlot = slot;
        NanoPackets.CoreSync s = sync;
        if (s == null || !s.online()) {
            player.displayClientMessage(Component.translatable("hud.mio_icif.nano.no_core"), true);
            deny(NanoBursts.DENY_NONE);
            return;
        }
        String key = slot == 0 ? s.primaryKey() : s.secondaryKey();
        if (key.isEmpty()) { deny(NanoBursts.DENY_NONE); return; }
        // a running camouflage always accepts the key (it switches off)
        boolean camoOff = key.equals(com.miophas.singularity_iteration.common.suit.SuitModuleType.OPTICAL_CAMO.id()) && s.camoTicks() > 0;
        if (!camoOff) {
            if (s.overheated()) { deny(NanoBursts.DENY_OVERHEAT); return; }
            int cd = slot == 0 ? s.primaryCooldown() : s.secondaryCooldown();
            if (cd > 2) { deny(NanoBursts.DENY_COOLDOWN); return; }
        }
        var connection = mc.getConnection();
        if (connection == null || !connection.hasChannel(NanoPackets.BurstRequest.TYPE)) return;
        PacketDistributor.sendToServer(new NanoPackets.BurstRequest((byte) slot, player.zza, player.xxa));
    }

    /** True when a solid block touches the side of the player (a wall to climb or cling to). */
    static boolean touchesWall(LocalPlayer player) {
        return !player.level().noCollision(player, player.getBoundingBox().inflate(0.08, -0.1, 0.08));
    }

    /** HF sync level of the local player (blade tremble, HUD). */
    public static int hfLevel() {
        return hfLevel;
    }

    /** Last synced core state, or null. */
    @Nullable
    public static NanoPackets.CoreSync sync() {
        return sync;
    }

    /** Panel: assigns burst key {@code slot} ("" = automatic, "-" = nothing). */
    public static void assign(int slot, String key) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(NanoPackets.BurstAssign.TYPE)) return;
        PacketDistributor.sendToServer(new NanoPackets.BurstAssign((byte) slot, key));
    }

    /** First-person blade tremble: the local player is in HF sync and holds this saber in the main hand. */
    public static boolean hfJitter(LocalPlayer player, net.minecraft.world.item.ItemStack stack) {
        return hfSync && player.getMainHandItem() == stack;
    }

    /** 0.1.9.0: the core burst key. Only the advanced propulsion core has a burst. */
    private static void pressCore(Minecraft mc, LocalPlayer player) {
        if (mc.screen != null || player.isSpectator()) return;
        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (com.miophas.singularity_iteration.common.suit.core.FrameCores.core(chest) != com.miophas.singularity_iteration.common.suit.core.FrameCore.PROPULSION) {
            player.displayClientMessage(Component.translatable("hud.mio_icif.core.no_burst"), true);
            coreDeniedAt = ticks;
            coreDeniedReason = com.miophas.singularity_iteration.common.suit.core.CoreEvents.DENY_NO_CORE;
            return;
        }
        var connection = mc.getConnection();
        if (connection == null || !connection.hasChannel(NanoPackets.CoreBurst.TYPE)) return;
        PacketDistributor.sendToServer(new NanoPackets.CoreBurst());
    }

    /** Ticks left of the own arc discharge cooldown (from the config value; the server decides). */
    static int dischargeCooldownLeft() {
        return Math.max(0, com.miophas.singularity_iteration.common.suit.core.CoreConfig.dischargeCooldown() - (ticks - dischargeAt));
    }

    static void clear() {
        sync = null;
        dashTick = -1;
        hfSync = false;
        hfLevel = 0;
        NanoFx.clear();
    }

    /** Effect strength from the client config. */
    static final class NanoFxIntensity {
        static float get() {
            return com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.nanoFx();
        }
    }
}
