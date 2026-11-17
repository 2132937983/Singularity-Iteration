// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.58 (gaze and Iron Man phase P6, decisions A / all four groups): short J.A.R.V.I.S.-style callouts of the
 * Wingmen, shown on the owner's screen as a subtitle line beside the squad HUD ({@code CalloutHud}) with a chirp. The
 * events: lock-on and kill; a piece badly damaged and the flight home for repairs; the reserve under a minute, the flight
 * home for low power and the escort speed it cannot pay; the owner hit, a projectile intercepted, a suit swap, an eject, a
 * catch, a deployment.
 *
 * <p>Not every event is said: one Wingman says at most one line every {@link #WINGMAN_GAP} ticks and the squad one every
 * {@link #SQUAD_GAP} ticks; urgent lines (repairs, low power, a catch, a suit-up) always go through.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class WingmanCallouts {
    private WingmanCallouts() {}

    public enum Kind {
        LOCK, KILL, DAMAGED, REPAIR(true), RESERVE_LOW, LOW_POWER(true), SPEED_STARVED, OWNER_HIT, OWNER_HURT, INTERCEPT,
        SUITED(true), REJOINED, EJECTED, CAUGHT(true), RESCUE_SUIT(true), DEPLOYED;

        public final boolean urgent;

        Kind() { this(false); }

        Kind(boolean urgent) { this.urgent = urgent; }

        public String key() { return "callout.mio_icif.wingman." + name().toLowerCase(Locale.ROOT); }
    }

    /** Ticks between two lines of one Wingman, and of the squad. */
    public static final int WINGMAN_GAP = 60, SQUAD_GAP = 30;

    private static final Map<UUID, Long> LAST_SQUAD = new HashMap<>();

    /** True when a line may be said now. */
    public static boolean allow(long now, long lastWingman, long lastSquad, boolean urgent) {
        return urgent || now - lastWingman >= WINGMAN_GAP && now - lastSquad >= SQUAD_GAP;
    }

    /** The callout payload: the line and whether it is urgent (a sharper chirp, a warm colour). */
    public record Line(Component text, boolean urgent) implements CustomPacketPayload {
        public static final Type<Line> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "wingman_callout"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Line> CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, Line::text, ByteBufCodecs.BOOL, Line::urgent, Line::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client hook (set by the client setup). */
    public static volatile java.util.function.Consumer<Line> HOOK;

    /** {@code w} says {@code kind} to its owner (when the limits allow); its squad label ("W1"..) is the first argument. */
    public static boolean say(WingmanEntity w, Kind kind, Object... args) {
        ServerPlayer owner = w.ownerPlayer();
        if (owner == null) return false;
        long now = w.level().getGameTime();
        if (!allow(now, w.lastCallout, LAST_SQUAD.getOrDefault(owner.getUUID(), Long.MIN_VALUE / 2), kind.urgent)) return false;
        w.lastCallout = now;
        LAST_SQUAD.put(owner.getUUID(), now);
        Object[] all = new Object[args.length + 1];
        all[0] = Component.literal("W" + (Math.floorMod(w.set(), 32) + 1));   // as the squad HUD labels it
        System.arraycopy(args, 0, all, 1, args.length);
        Line line = new Line(Component.translatable(kind.key(), all), kind.urgent);
        if (owner.connection != null && owner.connection.hasChannel(Line.TYPE)) PacketDistributor.sendToPlayer(owner, line);
        return true;
    }

    /** The owner was hurt: the nearest Wingman calls it (with the attacker when there is one). */
    @SubscribeEvent
    static void onOwnerHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || event.getNewDamage() <= 0) return;
        WingmanEntity nearest = null;
        double best = 48 * 48;
        for (WingmanEntity w : WingmanService.of(owner)) {
            if (w.level() != owner.level() || w.state() != WingmanEntity.ACTIVE) continue;
            double d = w.distanceToSqr(owner);
            if (d < best) { best = d; nearest = w; }
        }
        if (nearest == null) return;
        @Nullable Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker != owner) say(nearest, Kind.OWNER_HIT, attacker.getName());
        else say(nearest, Kind.OWNER_HURT);
    }

    @SubscribeEvent
    static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SQUAD.remove(event.getEntity().getUUID());
    }
}
