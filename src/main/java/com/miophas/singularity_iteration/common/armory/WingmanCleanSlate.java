// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.11.59 (gaze and Iron Man phase P7, decisions A / A): the Clean Slate protocol. The owner gives it twice within
 * {@link #CONFIRM_TICKS} (the command wheel, squad control; the first time only asks to confirm): every loaded Wingman of
 * the owner climbs into the sky and, one after another, bursts into a firework in the colours of its armour. It is a show:
 * nothing is destroyed and no one is hurt (the burst is drawn by the clients from a packet, no firework rocket flies); the
 * suits go home to their Armory as in a return.
 */
public final class WingmanCleanSlate {
    private WingmanCleanSlate() {}

    /** Ticks within which the second request confirms the first. */
    public static final int CONFIRM_TICKS = 60;
    /** Ticks of the climb before the first burst, and between two bursts. */
    public static final int RISE_TICKS = 30, STAGGER = 8;

    private static final Map<UUID, Long> ASKED = new HashMap<>();

    /** True when a request at {@code now} confirms one asked at {@code asked} (null: none). */
    public static boolean confirms(Long asked, long now) {
        return asked != null && now - asked >= 0 && now - asked <= CONFIRM_TICKS;
    }

    /** The owner asks for the protocol: the first request asks to confirm, the second runs it. */
    public static Component request(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        Long asked = ASKED.get(player.getUUID());
        if (!confirms(asked, now)) {
            ASKED.put(player.getUUID(), now);
            return Component.translatable("message.mio_icif.wingman.clean_slate.confirm", CONFIRM_TICKS / 20);
        }
        ASKED.remove(player.getUUID());
        int n = 0;
        for (WingmanEntity w : WingmanService.of(player)) {
            if (w.state() != WingmanEntity.ACTIVE || w.isRemoved()) continue;
            w.cleanSlate(RISE_TICKS + n * STAGGER);
            n++;
        }
        return n == 0 ? Component.translatable("message.mio_icif.command.no_unit")
            : Component.translatable("message.mio_icif.wingman.clean_slate.go", n);
    }

    /** The firework colours of a suit: from its chestplate (else any armour piece); {main, fade}. */
    public static int[] colours(Iterable<ItemStack> armour) {
        for (ItemStack s : armour) {
            if (!(s.getItem() instanceof ArmorItem armor)) continue;
            DyedItemColor dyed = s.get(DataComponents.DYED_COLOR);
            if (dyed != null) return new int[]{dyed.rgb(), 0xFFFFFF};
            var m = armor.getMaterial();
            if (m.is(ArmorMaterials.GOLD)) return new int[]{0xFFD83D, 0xFFF4B0};
            if (m.is(ArmorMaterials.DIAMOND)) return new int[]{0x4DE6E6, 0xFFFFFF};
            if (m.is(ArmorMaterials.NETHERITE)) return new int[]{0x5A4646, 0xFF7A3D};
            if (m.is(ArmorMaterials.IRON)) return new int[]{0xD8D8D8, 0xFFFFFF};
            if (m.is(ArmorMaterials.CHAIN)) return new int[]{0x9AA0A6, 0xD8D8D8};
            if (m.is(ArmorMaterials.LEATHER)) return new int[]{0xA0652B, 0xFFD8A0};
            if (m.is(ArmorMaterials.TURTLE)) return new int[]{0x47BF4A, 0xFFFFFF};
            return new int[]{0x5CEBFF, 0xB98CFF};
        }
        return new int[]{0x5CEBFF, 0xB98CFF};
    }

    /** The explosions of one burst: a large ball with trail and twinkle, and a star. */
    public static List<FireworkExplosion> burst(int[] colours) {
        return List.of(
            new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(colours[0]), IntList.of(colours[1]), true, true),
            new FireworkExplosion(FireworkExplosion.Shape.STAR, IntList.of(colours[0], 0xFFFFFF), IntList.of(), false, true));
    }

    /** A burst for the clients to draw. */
    public record Burst(Vec3 at, List<FireworkExplosion> explosions) implements CustomPacketPayload {
        public static final Type<Burst> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "wingman_clean_slate"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Burst> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, b -> b.at.x, ByteBufCodecs.DOUBLE, b -> b.at.y, ByteBufCodecs.DOUBLE, b -> b.at.z,
            FireworkExplosion.STREAM_CODEC.apply(ByteBufCodecs.list(8)), Burst::explosions,
            (x, y, z, e) -> new Burst(new Vec3(x, y, z), e));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client hook (set by the client setup). */
    public static volatile java.util.function.Consumer<Burst> HOOK;
}
