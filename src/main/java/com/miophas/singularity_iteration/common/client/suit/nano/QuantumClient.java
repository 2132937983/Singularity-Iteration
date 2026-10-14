// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumActions;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumCore;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

/**
 * Client controller of the quantum core (0.1.10.0): the synced coherence state, the local cues of
 * the quantum actions (blink lens kick, dilation zoom), and the gate of the action keys. The keys
 * themselves are the nano burst keys ({@link NanoClient}); this class answers whether a quantum
 * action may be asked for and routes the quantum effect kinds to {@link QuantumActionFx} and
 * {@link QuantumHud}. It lives next to the nano client classes because both share the effect
 * timeline and the render passes.
 */
public final class QuantumClient {
    private QuantumClient() {}

    /** Last quantum core state from the server (null before the first sync). */
    @Nullable static QuantumPackets.CoreSync sync;
    /** Client ticks of the last own blink, decoherence, recoherence, dilation start and end. */
    static int blinkAt = -100, decohereAt = -1000, recohereAt = -1000, dilationOnAt = -1000, dilationOffAt = -1000, phaseAt = -1000;

    // ---------------------------------------------------------------- 0.1.10.1
    /** Bandwidth load and budget of the local wearer (refreshed each tick), and the synergies that work. */
    static int bandwidthLoad, bandwidthBudget = com.miophas.singularity_iteration.common.suit.quantum.QuantumBandwidth.BASE_BUDGET;
    static java.util.List<String> activeLinks = java.util.List.of();
    /** Last combo (kind, client tick) for the banner. */
    static int comboKind = -1, comboAt = -1000;
    /** Fleet Datalink: the marked creature and the creatures the Wingmen see (client tick of the last packet). */
    static int datalinkMarked = -1, datalinkAt = -1000;
    static final it.unimi.dsi.fastutil.ints.IntOpenHashSet DATALINK = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
    /** Last link toggle answer for the editor screen (key, result code, client tick). */
    static String linkResultKey = "";
    static int linkResult = -1, linkResultAt = -1000;

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        static void setup(FMLClientSetupEvent event) {
            QuantumPackets.SYNC_HOOK = QuantumClient::acceptSync;
            QuantumPackets.DATALINK_HOOK = QuantumClient::acceptDataLink;
            QuantumPackets.LINK_HOOK = QuantumClient::acceptLinkResult;
            QuantumCore.CLIENT_VIEW = player -> {
                QuantumPackets.CoreSync s = sync;
                if (s == null || player != Minecraft.getInstance().player) return null;
                return new float[]{s.coherence(), s.max(), s.decoherent()};
            };
        }

        @SubscribeEvent
        static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_hud"), QuantumHud::render);
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        /** Blink: a short lens kick; time dilation: a slight zoom while the field runs. */
        @SubscribeEvent
        static void fov(ComputeFovModifierEvent event) {
            float f = FcsClientConfig.quantumFx();
            float since = NanoClient.ticks - blinkAt;
            float k = 1;
            if (since >= 0 && since < 6) k *= 1 + 0.18F * (1 - since / 6F) * f;
            QuantumPackets.CoreSync s = sync;
            if (s != null && s.dilationTicks() > 0) {
                float in = Math.min(1, (NanoClient.ticks - dilationOnAt) / 8F), out = Math.min(1, s.dilationTicks() / 8F);
                k *= 1 - 0.06F * Math.min(in, out) * f;
            }
            if (k != 1) event.setNewFovModifier(event.getNewFovModifier() * k);
        }

        @SubscribeEvent
        static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            clear();
        }
    }

    // ------------------------------------------------------------------ packets

    static void acceptSync(QuantumPackets.CoreSync s) {
        QuantumPackets.CoreSync old = sync;
        sync = s;
        if (s.dilationTicks() > 0 && (old == null || old.dilationTicks() <= 0)) dilationOnAt = NanoClient.ticks;
        if (s.dilationTicks() <= 0 && old != null && old.dilationTicks() > 0) dilationOffAt = NanoClient.ticks;
    }

    static void acceptDataLink(QuantumPackets.DataLink d) {
        datalinkMarked = d.marked();
        DATALINK.clear();
        for (int id : d.seen()) DATALINK.add(id);
        datalinkAt = NanoClient.ticks;
    }

    static void acceptLinkResult(QuantumPackets.LinkResult r) {
        linkResultKey = r.key();
        linkResult = r.result();
        linkResultAt = NanoClient.ticks;
        var mc = Minecraft.getInstance();
        if (mc.player != null && r.result() > com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.UNLINKED) {
            NanoClient.play(mio_icif_sounds.NANO_DENIED.get(), 1.0F, 0.5F);
        }
    }

    /** 0.1.10.1: each client tick, the bandwidth and the working synergies of the local wearer. */
    static void tick(Minecraft mc) {
        if (mc.player == null) return;
        bandwidthLoad = com.miophas.singularity_iteration.common.suit.quantum.QuantumBandwidth.load(mc.player);
        bandwidthBudget = com.miophas.singularity_iteration.common.suit.quantum.QuantumBandwidth.budget(mc.player);
        activeLinks = com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.activeKeys(mc.player);
        if (NanoClient.ticks - datalinkAt > 40) { DATALINK.clear(); datalinkMarked = -1; }
    }

    /** True while the suit computer of the local wearer is above its bandwidth budget. */
    public static boolean saturated() {
        return bandwidthLoad > bandwidthBudget;
    }

    /** Quantum kinds of the shared effect payload (32 and up). */
    static void acceptFx(NanoPackets.Fx fx, boolean self) {
        switch (fx.kind()) {
            case QuantumPackets.FX_COMBO -> {
                if (self) { comboKind = (int) fx.a(); comboAt = NanoClient.ticks; }
                QuantumActionFx.combo(fx.entityId(), (int) fx.a());
            }
            case QuantumPackets.FX_RESONANCE -> QuantumActionFx.resonance(fx);
            case QuantumPackets.FX_DECOY -> QuantumActionFx.decoy(fx);
            case QuantumPackets.FX_DECOY_END -> QuantumActionFx.decoyEnd(fx.entityId());
            case QuantumPackets.FX_BARRAGE -> QuantumActionFx.barrage(fx);
            // 0.1.10.2 quantum command
            case QuantumPackets.FX_ORDER -> { if (self) com.miophas.singularity_iteration.common.client.armory.CommandFx.order(fx); }
            case QuantumPackets.FX_RECALL -> com.miophas.singularity_iteration.common.client.armory.CommandFx.recall(fx);
            case QuantumPackets.FX_BLINK -> {
                if (self) { blinkAt = NanoClient.ticks; NanoClient.usedAt[NanoClient.lastSlot] = NanoClient.ticks; }
                QuantumActionFx.blink(fx, self);
            }
            case QuantumPackets.FX_PHASE_ON -> {
                if (self) { phaseAt = NanoClient.ticks; NanoClient.usedAt[NanoClient.lastSlot] = NanoClient.ticks; }
                QuantumActionFx.phaseOn(fx.entityId(), (int) fx.a(), (int) fx.b());
            }
            case QuantumPackets.FX_PHASE_OFF -> QuantumActionFx.phaseOff(fx);
            case QuantumPackets.FX_FIELD_ON -> {
                if (self) NanoClient.usedAt[NanoClient.lastSlot] = NanoClient.ticks;
                QuantumActionFx.fieldOn(fx.entityId(), fx.a(), (int) fx.b(), true);
            }
            case QuantumPackets.FX_FIELD_KEEP -> QuantumActionFx.fieldOn(fx.entityId(), fx.a(), (int) fx.b(), false);
            case QuantumPackets.FX_FIELD_OFF -> QuantumActionFx.fieldOff(fx.entityId(), (int) fx.a());
            case QuantumPackets.FX_FIELD_CATCH -> QuantumActionFx.fieldCatch(fx);
            case QuantumPackets.FX_DILATION_ON -> {
                if (self) { dilationOnAt = NanoClient.ticks; NanoClient.usedAt[NanoClient.lastSlot] = NanoClient.ticks; }
                QuantumActionFx.dilationOn(fx.entityId(), fx.a(), (int) fx.b(), (int) fx.c(), true);
            }
            case QuantumPackets.FX_DILATION_KEEP -> QuantumActionFx.dilationOn(fx.entityId(), fx.a(), (int) fx.b(), (int) fx.c(), false);
            case QuantumPackets.FX_DILATION_OFF -> {
                if (self) dilationOffAt = NanoClient.ticks;
                QuantumActionFx.dilationOff(fx.entityId());
            }
            case QuantumPackets.FX_VOLLEY -> {
                if (self) NanoClient.usedAt[NanoClient.lastSlot] = NanoClient.ticks;
                QuantumActionFx.volley(fx, self);
            }
            case QuantumPackets.FX_DECOHERE -> {
                if (self) decohereAt = NanoClient.ticks;
                QuantumActionFx.decohere(fx.entityId());
            }
            case QuantumPackets.FX_RECOHERE -> {
                if (self) {
                    recohereAt = NanoClient.ticks;
                    NanoClient.play(mio_icif_sounds.VISOR_BOOT.get(), 1.35F, 0.35F);
                }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ queries

    /** Last synced state, or null. */
    @Nullable
    public static QuantumPackets.CoreSync sync() {
        return sync;
    }

    /** True while the local core is locked by decoherence. */
    public static boolean decoherent() {
        QuantumPackets.CoreSync s = sync;
        return s != null && s.decoherent() > 0;
    }

    /** Glitch strength of the HUD 0..1: strong at the collapse, fading over the lock; a flicker at the recovery. */
    public static float glitch(float partial) {
        QuantumPackets.CoreSync s = sync;
        float t = NanoClient.ticks + partial;
        float g = 0;
        if (s != null && s.decoherent() > 0) {
            float since = t - decohereAt;
            g = since < 10 ? 1F : 0.35F + 0.25F * (s.decoherent() / (float) Math.max(1, s.decoherentMax()));
        }
        float rec = t - recohereAt;
        if (rec >= 0 && rec < 8) g = Math.max(g, 0.5F * (1 - rec / 8F));
        return g * (FcsClientConfig.decoherenceGlitch() ? 1F : 0.25F);
    }

    /**
     * Local gate of an action key: refusal reason, or 0 when the press goes to the server.
     * The server checks again; this only gives an instant answer.
     */
    static int gate(String key) {
        QuantumPackets.CoreSync s = sync;
        if (s == null) return 0;
        UnitRef unit = UnitRef.byKey(key);
        // a running force field always takes the key (it switches off)
        if (QuantumActions.kind(unit) == QuantumActions.Kind.FIELD && s.field()) return 0;
        if (s.decoherent() > 0) return QuantumActions.DENY_DECOHERENT;
        var spec = QuantumActions.spec(unit);
        if (spec != null && (spec.sustained() ? s.coherence() <= spec.coherenceCost() : s.coherence() + 0.01F < spec.coherenceCost())) {
            return QuantumActions.DENY_COHERENCE;
        }
        return 0;
    }

    /** True when the key switches a running toggle action off (it then takes no cooldown check). */
    static boolean togglesOff(String key) {
        QuantumPackets.CoreSync s = sync;
        return s != null && s.field() && QuantumActions.kind(UnitRef.byKey(key)) == QuantumActions.Kind.FIELD;
    }

    /** True when the action on {@code key} could fire now (chip glow). */
    static boolean ready(String key, int cooldown) {
        return cooldown <= 0 && gate(key) == 0;
    }

    static void clear() {
        sync = null;
        DATALINK.clear();
        datalinkMarked = -1;
        activeLinks = java.util.List.of();
        bandwidthLoad = 0;
        QuantumActionFx.clear();
    }

    /** 0.1.10.2: the hostile creatures the Wingmen see (data link or Fleet Datalink), for the maps and the holo table. */
    public static it.unimi.dsi.fastutil.ints.IntSet datalinkSeen() {
        return it.unimi.dsi.fastutil.ints.IntSets.unmodifiable(DATALINK);
    }

    /** 0.1.10.1: the working synergies of the local wearer (HUD, panel). */
    public static java.util.List<String> activeLinks() {
        return activeLinks;
    }

    /** 0.1.10.1: bandwidth load and budget of the local wearer. */
    public static int bandwidthLoad() { return bandwidthLoad; }
    public static int bandwidthBudget() { return bandwidthBudget; }

    /** 0.1.10.1: the last link toggle answer: result code, or -1 when it is older than 3 s or about another key. */
    public static int linkResult(String key) {
        return key.equals(linkResultKey) && NanoClient.ticks - linkResultAt < 60 ? linkResult : -1;
    }
}
