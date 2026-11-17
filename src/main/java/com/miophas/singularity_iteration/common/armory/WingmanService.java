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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
 * 0.1.10.2: a Wingman that leaves with its chunk goes to the saved {@link WingmanRoster} instead,
 * and comes off it when it loads again ({@link WingmanCommands} reads both).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class WingmanService {
    private WingmanService() {}

    /** Default maximum number of Wingmen per player (the live value is {@link WingmanConfig#maxPerPlayer()}, 0.1.11.39). */
    public static final int MAX_PER_PLAYER = 4;
    /** Same dimension and nearer than this: the Wingman takes off from the Armory, else it drops in above the owner. */
    static final double LAUNCH_RANGE = 128;

    /** Server thread only. */
    private static final Map<UUID, List<WingmanEntity>> BY_OWNER = new HashMap<>();

    /**
     * One Wingman row of the console. {@code energy} is -1 without electric pieces. 0.1.11.39: {@code entityId} names the
     * Wingman for a recall (a row number could point at another Wingman after a change of the list).
     */
    public record View(int entityId, int set, int mode, int state, float health, float energy, int endurance, List<ItemStack> items) {
        void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(entityId); buf.writeVarInt(set); buf.writeByte(mode); buf.writeByte(state); buf.writeFloat(health); buf.writeFloat(energy);
            buf.writeVarInt(endurance + 2);   // 0.1.11.41: seconds, or ENDURANCE_FREE (-1) / ENDURANCE_ARMORY (-2)
            for (int i = 0; i < ArmoryPiece.COLUMNS.length; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }

        static View read(RegistryFriendlyByteBuf buf) {
            int entityId = buf.readVarInt(), set = buf.readVarInt(), mode = buf.readUnsignedByte(), state = buf.readUnsignedByte();
            float health = buf.readFloat(), energy = buf.readFloat();
            int endurance = buf.readVarInt() - 2;
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < ArmoryPiece.COLUMNS.length; i++) items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            return new View(entityId, set, mode, state, health, energy, endurance, items);
        }
    }

    // ------------------------------------------------------------------ registry
    static void track(WingmanEntity w) {
        if (w.owner() == null) return;
        List<WingmanEntity> list = BY_OWNER.computeIfAbsent(w.owner(), k -> new ArrayList<>());
        if (!list.contains(w)) list.add(w);
        renumber(list);
        // 0.1.10.2: loaded again, so not on the roster of unloaded Wingmen
        WingmanRoster roster = WingmanRoster.get(w.level().getServer());
        if (roster != null) roster.remove(w.getUUID());
    }

    static void untrack(WingmanEntity w) {
        if (w.owner() == null) return;
        // 0.1.10.2: a Wingman that leaves with its chunk stays on the roster (the quantum recall can reach it)
        WingmanRoster roster = WingmanRoster.get(w.level().getServer());
        if (roster != null) {
            if (w.getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK) {
                roster.put(new WingmanRoster.Entry(w.getUUID(), w.owner(), w.level().dimension(), w.position(), w.set()));
            } else {
                roster.remove(w.getUUID());
            }
        }
        List<WingmanEntity> list = BY_OWNER.get(w.owner());
        if (list == null) return;
        list.remove(w);
        if (list.isEmpty()) BY_OWNER.remove(w.owner());
        else renumber(list);
    }

    /** 0.1.10.5: true while a Wingman is loaded anywhere on the server. */
    static boolean anyTracked() {
        return !BY_OWNER.isEmpty();
    }

    /** 0.1.10.5: true when a loaded Wingman other than {@code self} flies sortie {@code token}. */
    static boolean flying(@org.jetbrains.annotations.Nullable UUID owner, UUID token, WingmanEntity self) {
        List<WingmanEntity> list = owner == null ? null : BY_OWNER.get(owner);
        if (list == null) return false;
        for (WingmanEntity w : list) if (w != self && !w.isRemoved() && token.equals(w.sortie())) return true;
        return false;
    }

    /**
     * 0.1.11.39 (G-13): the squad order follows the suit sets (then the entity id), so a Wingman that loads or unloads no
     * longer shuffles the places of the others (before: the entity id, which a new deployment always puts last).
     */
    private static void renumber(List<WingmanEntity> list) {
        list.sort(Comparator.comparingInt(WingmanEntity::set).thenComparingInt(WingmanEntity::getId));
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
            out.add(new View(w.getId(), w.set(), w.mode().ordinal(), w.state(), w.getHealth() / Math.max(1, w.getMaxHealth()), w.energyFraction(),
                w.enduranceSeconds(), items));
        }
        return out;
    }

    /**
     * 0.1.11.39 (G-2): every Wingman of {@code player}: the loaded ones and the ones in unloaded chunks (roster). The limit
     * counts them all; before, a Wingman sent far away and unloaded freed its place, so the limit did not hold.
     */
    public static int count(ServerPlayer player) {
        WingmanRoster roster = WingmanRoster.get(player.server);
        return of(player).size() + (roster == null ? 0 : roster.of(player.getUUID()).size());
    }

    /**
     * 0.1.11.39 (G-4): where a Wingman that cannot take off from its Armory drops in: the highest place up to 18 blocks
     * above the owner with room for its body and an open column down to the owner, else beside the owner; null when even
     * that place is blocked. Before, it always appeared 18 blocks up, inside the rock of a cave or the Nether, suffocated
     * and flew home at once.
     */
    @org.jetbrains.annotations.Nullable
    static Vec3 dropInPoint(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        var size = ArmoryRegistry.WINGMAN.get().getDimensions();
        Vec3 feet = player.position();
        int top = 0;
        for (int dy = 1; dy <= 18; dy++) {   // the open column above the owner
            if (!level.noCollision(size.makeBoundingBox(feet.add(0, dy, 0)))) break;
            top = dy;
        }
        if (top >= 2) return feet.add(0, top, 0);
        for (double[] o : new double[][]{{1.2, 0}, {-1.2, 0}, {0, 1.2}, {0, -1.2}, {0, 0}}) {
            Vec3 at = feet.add(o[0], 0, o[1]);
            if (level.noCollision(size.makeBoundingBox(at))) return at;
        }
        return null;
    }

    // ------------------------------------------------------------------ actions
    /** Deploys suit {@code set} of {@code armory} as a Wingman in {@code mode}. */
    public static Component deploy(ServerPlayer player, mio_icif_armory armory, int set, WingmanMode mode) {
        if (!armory.isOwner(player)) return Component.translatable("message.mio_icif.armory.not_owner");
        if (armory.isBusy()) return Component.translatable("message.mio_icif.armory.busy");
        if (set < 0 || set >= mio_icif_armory.SETS) return Component.translatable("message.mio_icif.armory.failed");
        if (!(armory.getLevel() instanceof ServerLevel home) || !player.isAlive() || player.isSpectator())
            return Component.translatable("message.mio_icif.armory.failed");
        int limit = WingmanConfig.maxPerPlayer();
        if (count(player) >= limit) return Component.translatable("message.mio_icif.wingman.limit", limit);
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
        boolean launch = sameDimension && straight >= 0 && straight < LAUNCH_RANGE;
        Vec3 at = launch ? armory.launchTop().add(0, 0.5, 0) : dropInPoint(player);
        if (at == null) return Component.translatable("message.mio_icif.wingman.no_room");   // 0.1.11.39 (G-4): before the payment
        long cost = mio_icif_armory.cost(pieces, straight, sameDimension);
        var payment = armory.getEnergyStorageInternal().scexReserveInternal(cost);
        if (payment == null) return Component.translatable("message.mio_icif.armory.no_energy", cost, armory.getEnergyStorage().getAmount());

        WingmanEntity w;
        try {
            ItemStack[] items = new ItemStack[ArmoryPiece.VALUES.length];
            var handler = armory.getItemHandler();
            for (ArmoryPiece p : ArmoryPiece.VALUES) items[p.ordinal()] = handler.extractItem(mio_icif_armory.slotOf(set, p), 64, false);
            armory.markStorageChanged();
            ServerLevel level = player.serverLevel();
            w = WingmanEntity.create(level, player, GlobalPos.of(home.dimension(), armory.getBlockPos()), set, mode, items, at);
            if (!level.addFreshEntity(w)) {
                // a different mod cancelled the spawn: the items go back and (0.1.11.39) the energy too
                w.secure();
                payment.cancel();
                return Component.translatable("message.mio_icif.armory.failed");
            }
        } finally {
            if (payment.isPending()) payment.commit();   // settled on every path (a throw keeps the old behaviour: spent)
        }
        armory.markDeployed(set, w.sortie());   // 0.1.11.39 (G-1): the suit keeps its positions while it flies
        // 0.1.11.41 (decision 1 C): the upkeep reserve, as much of it as the Armory has after the deployment
        long reserve = armory.getEnergyStorageInternal().consumeEnergyInternal(WingmanConfig.reserveEu(), false);
        w.setReserve(reserve);
        armory.markStorageChanged();
        if (launch) {
            home.playSound(null, armory.getBlockPos(), mio_icif_sounds.ARMORY_HATCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            home.playSound(null, armory.getBlockPos(), mio_icif_sounds.ARMORY_LAUNCH.get(), SoundSource.BLOCKS, 1.0F, 0.9F);
        } else {
            player.serverLevel().playSound(null, at.x, at.y, at.z, mio_icif_sounds.ARMORY_FLYBY.get(), SoundSource.NEUTRAL, 1.5F, 0.9F);
        }
        long upkeep = WingmanConfig.upkeepEuPerSecond();
        return Component.translatable("message.mio_icif.wingman.deployed_reserve", armory.setName(set), Component.translatable(mode.key()), cost,
            reserve, upkeep <= 0 ? "-" : formatSeconds(reserve / upkeep));
    }

    /** 0.1.11.41: m:ss. */
    public static String formatSeconds(long seconds) {
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Sets the squad mode: every Wingman of {@code player} takes it, except a Wingman with an
     * individual mode (0.1.10.2 command wheel), which keeps its own.
     */
    public static Component setMode(ServerPlayer player, WingmanMode mode) {
        player.getData(com.miophas.singularity_iteration.common.suit.SuitRegistry.WINGMAN_COMMAND).squadMode = mode.ordinal();
        List<WingmanEntity> list = of(player);
        if (list.isEmpty()) return Component.translatable("message.mio_icif.wingman.none");
        for (WingmanEntity w : list) if (w.state() == WingmanEntity.ACTIVE && !w.individual()) w.setMode(mode);
        return Component.translatable("message.mio_icif.wingman.mode", Component.translatable(mode.key()));
    }

    /**
     * Recalls Wingman {@code index} of the console list, or all for a negative or too large index. 0.1.11.39 (G-2, G-10):
     * "all" also reaches the Wingmen in unloaded chunks (their chunk loads for a moment and the suit goes home), so a
     * player without the quantum recall can get them back too.
     */
    public static Component recall(ServerPlayer player, int index) {
        List<WingmanEntity> list = of(player);
        if (index >= 0 && index < list.size()) {
            list.get(index).startReturn(null);
            return Component.translatable("message.mio_icif.wingman.recalled", 1);
        }
        WingmanRoster roster = WingmanRoster.get(player.server);
        int far = roster == null ? 0 : WingmanCommands.recallUnloaded(player.server, player.getUUID(), roster.of(player.getUUID()));
        if (list.isEmpty() && far == 0) return Component.translatable("message.mio_icif.wingman.none");
        for (WingmanEntity w : list) w.startReturn(null);
        return Component.translatable("message.mio_icif.wingman.recalled", list.size() + far);
    }

    /** 0.1.11.39 (G-12): recalls the Wingman with entity id {@code entityId} (a console row). */
    public static Component recallById(ServerPlayer player, int entityId) {
        for (WingmanEntity w : of(player)) {
            if (w.getId() != entityId) continue;
            w.startReturn(null);
            return Component.translatable("message.mio_icif.wingman.recalled", 1);
        }
        return Component.translatable("message.mio_icif.wingman.none");
    }

    // ------------------------------------------------------------------ lifecycle
    /**
     * The owner leaves: every loaded Wingman puts its suit back at once (no flight). 0.1.11.39 (G-10): the Wingmen in
     * unloaded chunks too (their chunk loads for a moment); before, their suits waited there until somebody came by.
     */
    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (WingmanEntity w : of(player)) {
            w.secure();
            w.discard();
        }
        WingmanRoster roster = WingmanRoster.get(player.server);
        if (roster != null && player.server.isRunning()) WingmanCommands.recallUnloaded(player.server, player.getUUID(), roster.of(player.getUUID()));
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        BY_OWNER.clear();
    }

    /** 0.1.10.5: a copy of a Wingman does not enter a level (see {@link WingmanSorties}). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof WingmanEntity w)) return;
        if (!w.admit()) event.setCanceled(true);
    }

    /**
     * 0.1.10.5: no item works on a Wingman. Mob capture tools (soul vials, the mob imprisonment
     * tool, containment jars, capture balls used by hand) take a mob through this interaction; a
     * captured Wingman kept its suit in the item while the Armory got the suit back. The owner's
     * empty-hand recall ({@link WingmanEntity#mobInteract}) is not affected. Tools that skip these
     * events are stopped by the entity type tags ({@code c:capturing_not_supported} and others)
     * and, in the end, by the sortie check.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof WingmanEntity && !event.getItemStack().isEmpty()) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget() instanceof WingmanEntity && !event.getItemStack().isEmpty()) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }
}
