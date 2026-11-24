// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModules;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
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
    /** Tracks rapid consecutive deployments per owner: {lastGameTime, salvoCount}. */
    private static final Map<UUID, long[]> DEPLOY_SALVO = new HashMap<>();

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

    /** Zero-allocation check whether {@code player} has any loaded live Wingman. */
    public static boolean hasAny(ServerPlayer player) {
        List<WingmanEntity> list = BY_OWNER.get(player.getUUID());
        if (list == null || list.isEmpty()) return false;
        for (WingmanEntity w : list) if (w.isAlive() && !w.isRemoved()) return true;
        return false;
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

    /**
     * V-wedge staggered aerial drop point for squad {@code slot} (0..3). Tries wide V-formation first, then tight
     * indoor wedge, then falls back to {@link #dropInPoint(ServerPlayer)} so cramped spaces always stay safe.
     */
    @org.jetbrains.annotations.Nullable
    static Vec3 dropInPoint(ServerPlayer player, int slot) {
        Vec3 base = dropInPoint(player);
        if (base == null) return null;
        ServerLevel level = player.serverLevel();
        var size = ArmoryRegistry.WINGMAN.get().getDimensions();
        float yawRad = (float) Math.toRadians(player.getYRot());
        double baseHeight = base.y - player.getY();
        boolean outdoor = baseHeight >= 6.0;
        for (boolean tight : outdoor ? new boolean[]{false, true} : new boolean[]{true}) {
            double[] off = WingmanDeployRules.vFormationOffset(slot, yawRad, tight);
            Vec3 candidate = base.add(off[0], outdoor ? off[1] : 0.0, off[2]);
            if (level.noCollision(size.makeBoundingBox(candidate))) return candidate;
        }
        return base;
    }

    private static int nextSalvoIndex(UUID owner, long gameTime) {
        long[] state = DEPLOY_SALVO.computeIfAbsent(owner, k -> new long[]{Long.MIN_VALUE / 2, 0});
        if (gameTime - state[0] <= WingmanDeployRules.STAGGER_WINDOW_TICKS) {
            state[1] = Math.min(3, state[1] + 1);
        } else {
            state[1] = 0;
        }
        state[0] = gameTime;
        return (int) state[1];
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
        // 0.1.11.42 (decision 2 C): a piece below the return threshold would send the Wingman home at its first wear
        List<ItemStack> suit = new ArrayList<>();
        for (ArmoryPiece p : ArmoryPiece.VALUES) suit.add(armory.piece(set, p));
        ItemStack worn = WingmanEntity.wornPiece(suit);
        if (!worn.isEmpty()) return Component.translatable("message.mio_icif.wingman.needs_repair", worn.getHoverName());
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.miophas.singularity_iteration.core.api.armory.ArmoryEvent.WingmanDeploy(
                player, armory.getBlockPos(), set, mode.name())).isCanceled()) return Component.translatable("message.mio_icif.armory.cancelled");

        boolean sameDimension = player.level() == home;
        double straight = sameDimension ? player.position().distanceTo(armory.launchTop()) : -1;
        boolean launch = sameDimension && straight >= 0 && straight < LAUNCH_RANGE;
        int deploySlot = of(player).size();
        Vec3 at = launch ? armory.launchTop().add(0, 0.5, 0) : dropInPoint(player, deploySlot);
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
        com.miophas.singularity_iteration.common.advancement.SiAdvancements.grant(player, "wingman", "summoned");   // 0.1.11.79
        // 0.1.11.41 (decision 1 C): the upkeep reserve, as much of it as the Armory has after the deployment
        long reserve = armory.getEnergyStorageInternal().consumeEnergyInternal(WingmanConfig.reserveEu(), false);
        w.setReserve(reserve);
        armory.markStorageChanged();
        WingmanCallouts.say(w, WingmanCallouts.Kind.DEPLOYED);   // 0.1.11.58 (P6)

        // Salvo stagger, catapult/orbital-drop impulse, harmonic audio pitch & S2C deploy FX spectacle
        int salvoIdx = nextSalvoIndex(player.getUUID(), player.serverLevel().getGameTime());
        float pitch = WingmanDeployRules.staggerPitch(salvoIdx);
        float dropHeight = launch ? 0F : (float) Math.max(0.0, at.y - player.getY());
        if (launch) {
            double fanAngle = (salvoIdx - 1.5) * 0.45;
            w.setDeltaMovement(Math.sin(fanAngle) * 0.28, 1.30, Math.cos(fanAngle) * 0.28);
            home.playSound(null, armory.getBlockPos(), mio_icif_sounds.ARMORY_HATCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            home.playSound(null, armory.getBlockPos(), mio_icif_sounds.ARMORY_LAUNCH.get(), SoundSource.BLOCKS, 1.0F, pitch);
        } else {
            if (dropHeight >= 5.0F) w.setDeltaMovement(0, -1.05, 0);
            player.serverLevel().playSound(null, at.x, at.y, at.z, mio_icif_sounds.ARMORY_FLYBY.get(), SoundSource.NEUTRAL, 1.4F, pitch);
            player.serverLevel().playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 1.1F, pitch * 0.85F);
        }
        boolean livingLining = SuitModules.hasLivingLining(w.getItemBySlot(EquipmentSlot.CHEST));
        WingmanDeployFxPacket fxPacket = new WingmanDeployFxPacket(
            w.getId(), at.x, at.y + 0.9, at.z, armory.getBlockPos(), launch, (byte) mode.ordinal(),
            livingLining, WingmanDeployRules.staggerDelayTicks(salvoIdx), dropHeight);
        for (ServerPlayer viewer : player.serverLevel().players()) {
            if (viewer.connection != null && viewer.connection.hasChannel(WingmanDeployFxPacket.TYPE)
                && viewer.distanceToSqr(at) <= 96.0 * 96.0) {
                PacketDistributor.sendToPlayer(viewer, fxPacket);
            }
        }
        long upkeep = WingmanConfig.upkeepEuPerSecond();
        return Component.translatable("message.mio_icif.wingman.deployed_reserve", armory.setName(set), Component.translatable(mode.key()), cost,
            reserve, upkeep <= 0 ? "-" : formatSeconds(reserve / upkeep));
    }

    /**
     * 0.1.11.55 (gaze and Iron Man phase P3, decisions A / B / A): the owner's worn suit leaves the body and flies on as a
     * Wingman (the reverse of the suit swap). The four armour pieces and the gear in the hands (unstackable items) come off,
     * except a piece with Curse of Binding, and assemble into a Wingman in front of the owner, in Escort mode (its own,
     * individual mode). It belongs to the Armory of the owner's paired remote and takes that Armory's first free suit
     * position: there it goes when it comes home. At least two armour pieces are needed and none may need repair; no
     * launch cost (nothing flies from the Armory), the upkeep reserve is charged as at a deployment.
     */
    public static Component eject(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return Component.translatable("message.mio_icif.armory.failed");
        mio_icif_armory armory = ArmoryRemoteService.armory(player, ArmoryRemoteService.target(player));
        if (armory == null) return Component.translatable("message.mio_icif.wingman.eject.no_armory");
        if (!armory.isOwner(player)) return Component.translatable("message.mio_icif.armory.not_owner");
        if (!(armory.getLevel() instanceof ServerLevel home)) return Component.translatable("message.mio_icif.armory.failed");
        int limit = WingmanConfig.maxPerPlayer();
        if (count(player) >= limit) return Component.translatable("message.mio_icif.wingman.limit", limit);
        int set = armory.firstFreeSet();
        if (set < 0) return Component.translatable("message.mio_icif.wingman.eject.no_set");
        ItemStack[] items = new ItemStack[ArmoryPiece.VALUES.length];
        java.util.Arrays.fill(items, ItemStack.EMPTY);
        int armour = 0;
        List<ItemStack> suit = new ArrayList<>();
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            ItemStack worn = player.getItemBySlot(p.slot);
            boolean bound = !player.isCreative() && net.minecraft.world.item.enchantment.EnchantmentHelper.has(worn,
                net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
            if (bound || !WingmanEntity.givesToWingman(worn, p)) continue;
            items[p.ordinal()] = worn;
            suit.add(worn);
            if (p.isArmor()) armour++;
        }
        if (armour < 2) return Component.translatable("message.mio_icif.wingman.eject.too_few");
        ItemStack needsRepair = WingmanEntity.wornPiece(suit);
        if (!needsRepair.isEmpty()) return Component.translatable("message.mio_icif.wingman.needs_repair", needsRepair.getHoverName());
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.miophas.singularity_iteration.core.api.armory.ArmoryEvent.WingmanDeploy(
                player, armory.getBlockPos(), set, WingmanMode.ESCORT.name())).isCanceled()) return Component.translatable("message.mio_icif.armory.cancelled");
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = player.position().add(look.scale(1.8)).add(0, 0.05, 0);
        ItemStack[] none = new ItemStack[ArmoryPiece.VALUES.length];
        java.util.Arrays.fill(none, ItemStack.EMPTY);
        WingmanEntity w = WingmanEntity.create(level, player, GlobalPos.of(home.dimension(), armory.getBlockPos()), set, WingmanMode.ESCORT, none, at);
        w.setMode(WingmanMode.ESCORT, true);
        if (!level.addFreshEntity(w)) {
            w.secure();
            return Component.translatable("message.mio_icif.armory.failed");
        }
        // the pieces come off now and fly to the new Wingman, which holds them until they arrive (see the suit swap)
        for (ArmoryPiece p : ArmoryPiece.VALUES) if (!items[p.ordinal()].isEmpty()) ArmoryPieceEntity.equipSilently(player, p.slot, ItemStack.EMPTY);
        player.inventoryMenu.broadcastChanges();
        w.receiveSuit(player, items);
        armory.markDeployed(set, w.sortie());
        com.miophas.singularity_iteration.common.advancement.SiAdvancements.grant(player, "wingman", "summoned");   // 0.1.11.79
        long reserve = armory.getEnergyStorageInternal().consumeEnergyInternal(WingmanConfig.reserveEu(), false);
        w.setReserve(reserve);
        armory.markStorageChanged();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.ARMORY_UNLOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return Component.translatable("message.mio_icif.wingman.eject.done", armory.setName(set));
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
        DEPLOY_SALVO.clear();
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
