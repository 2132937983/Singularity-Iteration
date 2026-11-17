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
 * {@link #CONFIRM_TICKS} (the command wheel, squad control; the first time only asks to confirm). It is a show: nothing is
 * destroyed and no one is hurt (the burst is drawn by the clients from a packet); the suits go home to their Armory.
 *
 * <p>0.1.11.64 (hit feel and constructivist FX phase Q3, decision five A): every loaded Wingman of the owner climbs into a
 * ring {@link #RING_HEIGHT} blocks above the owner, facing its centre; then, one every {@link #STAGGER} ticks, each bursts:
 * a growing white ring with a red one inside, eight red wedges thrown out and a white pillar through it, all cut after
 * 12 ticks, with a deep blast; its pieces fly home in straight lines (visual echoes) while the items go to the Armory. The
 * classic style (client option) still draws the firework of 0.1.11.59.
 */
public final class WingmanCleanSlate {
    private WingmanCleanSlate() {}

    /** Ticks within which the second request confirms the first. */
    public static final int CONFIRM_TICKS = 60;
    /** Ticks of the climb into the ring before the first burst, and between two bursts (0.1.11.64: 40, was 30). */
    public static final int RISE_TICKS = 40, STAGGER = 8;
    /** Height of the ring above the owner, blocks. */
    public static final double RING_HEIGHT = 12;

    /** 0.1.11.64: the ring radius for {@code n} Wingmen (room for them, at least 4 blocks). */
    public static double ringRadius(int n) { return Math.max(4, 1.6 + n * 1.1); }

    /** 0.1.11.64: the place of Wingman {@code i} of {@code n} in the ring above {@code owner}. */
    public static net.minecraft.world.phys.Vec3 ringSlot(net.minecraft.world.phys.Vec3 owner, int i, int n) {
        double a = Math.PI * 2 * i / Math.max(1, n), r = ringRadius(n);
        return owner.add(Math.cos(a) * r, RING_HEIGHT, Math.sin(a) * r);
    }

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
        // 0.1.11.64 (decision five A): the squad climbs into a ring above the owner, then bursts one after another
        java.util.List<WingmanEntity> squad = new java.util.ArrayList<>();
        for (WingmanEntity w : WingmanService.of(player)) if (w.state() == WingmanEntity.ACTIVE && !w.isRemoved()) squad.add(w);
        int n = squad.size();
        for (int i = 0; i < n; i++) squad.get(i).cleanSlate(RISE_TICKS + i * STAGGER, ringSlot(player.position(), i, n), player.position().add(0, RING_HEIGHT, 0));
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
