// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Server side of the Wingman system (0.1.8.0): deploy, mode change, recall, and the console
 * view. The Armory section of the Singularity Management Panel sends the actions through
 * {@link ArmoryConsolePacket} ({@link ArmoryRemoteService#WINGMAN_DEPLOY},
 * {@link ArmoryRemoteService#WINGMAN_MODE}, {@link ArmoryRemoteService#WINGMAN_RECALL}).
 *
 * <p>Owner registry: a map from owner to the loaded Wingmen of the owner. An entry is added when
 * the entity enters a level and removed when it leaves (unload, removal). Thus a lookup costs no
 * entity scan, and the map holds no unloaded entity. The map is cleared when the server stops.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class WingmanService {
    private WingmanService() {}

    /** Maximum number of Wingmen per player. */
    public static final int MAX_PER_PLAYER = 4;
    /** Same dimension and nearer than this: the Wingman takes off from the Armory, else it drops in above the owner. */
    static final double LAUNCH_RANGE = 128;

    /** Server thread only. */
    private static final Map<UUID, List<WingmanEntity>> BY_OWNER = new HashMap<>();

    /** One Wingman row of the console. {@code energy} is -1 without electric pieces. */
    public record View(int set, int mode, int state, float health, float energy, List<ItemStack> items) {
        void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(set); buf.writeByte(mode); buf.writeByte(state); buf.writeFloat(health); buf.writeFloat(energy);
            for (int i = 0; i < ArmoryPiece.COLUMNS.length; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }

        static View read(RegistryFriendlyByteBuf buf) {
            int set = buf.readVarInt(), mode = buf.readUnsignedByte(), state = buf.readUnsignedByte();
            float health = buf.readFloat(), energy = buf.readFloat();
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < ArmoryPiece.COLUMNS.length; i++) items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            return new View(set, mode, state, health, energy, items);
        }
    }

    // ------------------------------------------------------------------ registry
    static void track(WingmanEntity w) {
        if (w.owner() == null) return;
        List<WingmanEntity> list = BY_OWNER.computeIfAbsent(w.owner(), k -> new ArrayList<>());
        if (!list.contains(w)) list.add(w);
        renumber(list);
    }

    static void untrack(WingmanEntity w) {
        if (w.owner() == null) return;
        List<WingmanEntity> list = BY_OWNER.get(w.owner());
        if (list == null) return;
        list.remove(w);
        if (list.isEmpty()) BY_OWNER.remove(w.owner());
        else renumber(list);
    }

    private static void renumber(List<WingmanEntity> list) {
        list.sort(Comparator.comparingInt(WingmanEntity::getId));
        for (int i = 0; i < list.size(); i++) list.get(i).setSlot(i, list.size());
    }

    /** The loaded Wingmen of {@code player}, in a stable order (a copy). */
    public static List<WingmanEntity> of(ServerPlayer player) {
        List<WingmanEntity> list = BY_OWNER.get(player.getUUID());
        if (list == null) return List.of();
        List<WingmanEntity> out = new ArrayList<>();
        for (WingmanEntity w : list) if (w.isAlive() && !w.isRemoved()) out.add(w);
        return out;
    }

    public static List<View> views(ServerPlayer player) {
        List<View> out = new ArrayList<>();
        for (WingmanEntity w : of(player)) {
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack s : w.items()) items.add(s.copy());
            out.add(new View(w.set(), w.mode().ordinal(), w.state(), w.getHealth() / Math.max(1, w.getMaxHealth()), w.energyFraction(), items));
        }
        return out;
    }

    // ------------------------------------------------------------------ actions
    /** Deploys suit {@code set} of {@code armory} as a Wingman in {@code mode}. */
    public static Component deploy(ServerPlayer player, mio_icif_armory armory, int set, WingmanMode mode) {
        if (!armory.isOwner(player)) return Component.translatable("message.mio_icif.armory.not_owner");
        if (armory.isBusy()) return Component.translatable("message.mio_icif.armory.busy");
        if (set < 0 || set >= mio_icif_armory.SETS) return Component.translatable("message.mio_icif.armory.failed");
        if (!(armory.getLevel() instanceof ServerLevel home) || !player.isAlive() || player.isSpectator())
            return Component.translatable("message.mio_icif.armory.failed");
        if (of(player).size() >= MAX_PER_PLAYER) return Component.translatable("message.mio_icif.wingman.limit", MAX_PER_PLAYER);
        int armor = 0, pieces = 0;
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            if (armory.piece(set, p).isEmpty()) continue;
            pieces++;
            if (p.isArmor()) armor++;
        }
        if (armor == 0) return Component.translatable("message.mio_icif.wingman.no_armor", armory.setName(set));
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.miophas.singularity_iteration.core.api.armory.ArmoryEvent.WingmanDeploy(
                player, armory.getBlockPos(), set, mode.name())).isCanceled()) return Component.translatable("message.mio_icif.armory.cancelled");

        boolean sameDimension = player.level() == home;
        double straight = sameDimension ? player.position().distanceTo(armory.launchTop()) : -1;
        long cost = mio_icif_armory.cost(pieces, straight, sameDimension);
        var payment = armory.getEnergyStorageInternal().scexReserveInternal(cost);
        if (payment == null) return Component.translatable("message.mio_icif.armory.no_energy", cost, armory.getEnergyStorage().getAmount());
        payment.commit();

        ItemStack[] items = new ItemStack[ArmoryPiece.VALUES.length];
        var handler = armory.getItemHandler();
        for (ArmoryPiece p : ArmoryPiece.VALUES) items[p.ordinal()] = handler.extractItem(mio_icif_armory.slotOf(set, p), 64, false);
        armory.markStorageChanged();

        ServerLevel level = player.serverLevel();
        boolean launch = sameDimension && straight >= 0 && straight < LAUNCH_RANGE;
        Vec3 at = launch ? armory.launchTop().add(0, 0.5, 0) : player.position().add(0, 18, 0);
        WingmanEntity w = WingmanEntity.create(level, player, GlobalPos.of(home.dimension(), armory.getBlockPos()), set, mode, items, at);
        if (!level.addFreshEntity(w)) {
            // a different mod cancelled the spawn: the items go back, the energy stays spent
            w.secure();
            return Component.translatable("message.mio_icif.armory.failed");
        }
        if (launch) {
            home.playSound(null, armory.getBlockPos(), mio_icif_sounds.ARMORY_HATCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            home.playSound(null, armory.getBlockPos(), mio_icif_sounds.ARMORY_LAUNCH.get(), SoundSource.BLOCKS, 1.0F, 0.9F);
        } else {
            level.playSound(null, at.x, at.y, at.z, mio_icif_sounds.ARMORY_FLYBY.get(), SoundSource.NEUTRAL, 1.5F, 0.9F);
        }
        return Component.translatable("message.mio_icif.wingman.deployed", armory.setName(set),
            Component.translatable(mode.key()), cost);
    }

    /** Sets the mode of all Wingmen of {@code player}. */
    public static Component setMode(ServerPlayer player, WingmanMode mode) {
        List<WingmanEntity> list = of(player);
        if (list.isEmpty()) return Component.translatable("message.mio_icif.wingman.none");
        for (WingmanEntity w : list) if (w.state() == WingmanEntity.ACTIVE) w.setMode(mode);
        return Component.translatable("message.mio_icif.wingman.mode", Component.translatable(mode.key()));
    }

    /** Recalls Wingman {@code index} of the console list, or all for a negative or too large index. */
    public static Component recall(ServerPlayer player, int index) {
        List<WingmanEntity> list = of(player);
        if (list.isEmpty()) return Component.translatable("message.mio_icif.wingman.none");
        if (index >= 0 && index < list.size()) {
            list.get(index).startReturn(null);
            return Component.translatable("message.mio_icif.wingman.recalled", 1);
        }
        for (WingmanEntity w : list) w.startReturn(null);
        return Component.translatable("message.mio_icif.wingman.recalled", list.size());
    }

    // ------------------------------------------------------------------ lifecycle
    /** The owner leaves: every loaded Wingman puts its suit back at once (no flight). */
    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (WingmanEntity w : of(player)) {
            w.secure();
            w.discard();
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        BY_OWNER.clear();
    }
}
