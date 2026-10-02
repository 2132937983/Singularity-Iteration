package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Server side of the Armory section of the Equipment Console.
 *
 * <p>The Armory Remote is an accessory: worn in a Curios charm / belt slot (or simply carried),
 * it links the player to their Armory. The console then lists the Armory's suits <i>and</i> every
 * Armor Showcase of the network - showcases with their own outfit near the player or near the
 * Armory - and summons any of them: the pieces fly in, and the suit the player was wearing
 * bursts off and flies to where the new one came from (an Armory suit slot, or the showcase
 * mannequin - a two-way exchange). A "return" action sends the worn suit home.
 */
public final class ArmoryRemoteService {
    private ArmoryRemoteService() {}

    public static final int REQUEST = 0, SUMMON = 1, SHOWCASE = 2, RETURN = 3;
    /** Showcases this close to the player, or to the paired Armory, belong to the network. */
    public static final int PLAYER_RANGE = 24, ARMORY_RANGE = 16;

    /** A showcase entry of the console list. */
    public record ShowcaseView(BlockPos pos, String label, List<ItemStack> items) { }

    // ------------------------------------------------------------------ the remote accessory
    /** The paired remote the player wears (Curios first) or carries; empty when none. */
    public static ItemStack findRemote(Player player) {
        for (ItemStack stack : ArmorFeatureSlots.accessoryStacks(player)) if (paired(stack)) return stack;
        for (ItemStack stack : player.getInventory().items) if (paired(stack)) return stack;
        if (paired(player.getOffhandItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    /** Any remote, paired or not (for the console's hints). */
    public static boolean hasRemote(Player player) {
        for (ItemStack stack : ArmorFeatureSlots.accessoryStacks(player)) if (stack.getItem() instanceof ArmoryRemoteItem) return true;
        for (ItemStack stack : player.getInventory().items) if (stack.getItem() instanceof ArmoryRemoteItem) return true;
        return player.getOffhandItem().getItem() instanceof ArmoryRemoteItem;
    }

    private static boolean paired(ItemStack stack) {
        return stack.getItem() instanceof ArmoryRemoteItem && stack.get(ArmoryComponents.TARGET.get()) != null;
    }

    @Nullable
    public static GlobalPos target(Player player) {
        ItemStack remote = findRemote(player);
        return remote.isEmpty() ? null : remote.get(ArmoryComponents.TARGET.get());
    }

    @Nullable
    static mio_icif_armory armory(ServerPlayer player, @Nullable GlobalPos target) {
        if (target == null) return null;
        ServerLevel level = player.server.getLevel(target.dimension());
        if (level == null || !level.isInWorldBounds(target.pos())) return null;
        return level.getBlockEntity(target.pos()) instanceof mio_icif_armory a ? a : null;
    }

    // ------------------------------------------------------------------ showcases of the network
    /** Own-outfit showcases near the player (same dimension) and near the paired Armory. */
    public static List<ArmorShowcaseBlockEntity> showcases(ServerPlayer player, @Nullable mio_icif_armory armory) {
        List<ArmorShowcaseBlockEntity> out = new ArrayList<>();
        collect(player.serverLevel(), player.blockPosition(), PLAYER_RANGE, out);
        if (armory != null && armory.getLevel() instanceof ServerLevel home) collect(home, armory.getBlockPos(), ARMORY_RANGE, out);
        out.sort(java.util.Comparator.comparingDouble(s -> s.getLevel() == player.level()
            ? s.getBlockPos().distSqr(player.blockPosition()) : Double.MAX_VALUE));
        return out;
    }

    private static void collect(ServerLevel level, BlockPos center, int range, List<ArmorShowcaseBlockEntity> out) {
        int c0x = (center.getX() - range) >> 4, c1x = (center.getX() + range) >> 4;
        int c0z = (center.getZ() - range) >> 4, c1z = (center.getZ() + range) >> 4;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (var be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof ArmorShowcaseBlockEntity showcase && showcase.hasOwnOutfit()
                            && be.getBlockPos().distSqr(center) <= (double) range * range && !out.contains(showcase)) {
                        out.add(showcase);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ console actions
    public static void handle(ServerPlayer player, int action, int index) {
        GlobalPos target = target(player);
        mio_icif_armory armory = armory(player, target);
        Component message = Component.empty();
        switch (action) {
            case SUMMON -> message = armory == null ? Component.translatable("message.mio_icif.armory.missing")
                : armory.summon(player, index).message();
            case SHOWCASE -> {
                List<ArmorShowcaseBlockEntity> list = showcases(player, armory);
                message = index >= 0 && index < list.size() ? summonShowcase(player, armory, list.get(index))
                    : Component.translatable("message.mio_icif.armory.failed");
            }
            case RETURN -> message = armory == null ? Component.translatable("message.mio_icif.armory.missing")
                : returnSuit(player, armory);
            default -> { }
        }
        if (!message.getString().isEmpty()) player.displayClientMessage(message, true);
        push(player, target, armory, message);
    }

    static void push(ServerPlayer player, @Nullable GlobalPos target, @Nullable mio_icif_armory armory, Component message) {
        ArmorySnapshot snapshot;
        if (target == null) snapshot = null;
        else if (armory == null) snapshot = ArmorySnapshot.of(target, ArmorySnapshot.MISSING);
        else if (armory.owner() == null) snapshot = ArmorySnapshot.of(target, ArmorySnapshot.UNBOUND);
        else if (!armory.isOwner(player)) snapshot = ArmorySnapshot.of(target, ArmorySnapshot.NOT_OWNER);
        else snapshot = ArmorySnapshot.of(target, armory, ArmorySnapshot.OK);
        List<ShowcaseView> views = new ArrayList<>();
        if (target != null) {
            for (ArmorShowcaseBlockEntity showcase : showcases(player, armory)) {
                List<ItemStack> items = new ArrayList<>();
                for (ArmoryPiece p : ArmoryPiece.COLUMNS) items.add(showcase.item(p).copy());
                views.add(new ShowcaseView(showcase.getBlockPos(), showcase.label(), items));
            }
        }
        try {
            if (player.connection != null && player.connection.hasChannel(ArmoryConsoleSyncPacket.TYPE)) {
                PacketDistributor.sendToPlayer(player, new ArmoryConsoleSyncPacket(hasRemote(player), snapshot, views, message));
            }
        } catch (RuntimeException ignored) {
            // detached / fake connections (automation, tests) have no client to update
        }
    }

    /**
     * Summons the outfit on a showcase: it flies to the player, and the player's old pieces burst
     * off and fly onto the mannequin in exchange. Paid from the paired Armory's energy.
     */
    public static Component summonShowcase(ServerPlayer player, @Nullable mio_icif_armory armory, ArmorShowcaseBlockEntity showcase) {
        if (armory == null) return Component.translatable("message.mio_icif.armory.missing");
        if (!armory.isOwner(player)) return Component.translatable("message.mio_icif.armory.not_owner");
        if (armory.isBusy()) return Component.translatable("message.mio_icif.armory.busy");
        if (!(showcase.getLevel() instanceof ServerLevel world) || !showcase.hasOwnOutfit())
            return Component.translatable("message.mio_icif.armory.failed");
        if (!player.isAlive() || player.isSpectator()) return Component.translatable("message.mio_icif.armory.failed");
        List<ArmoryPiece> flying = new ArrayList<>();
        for (ArmoryPiece p : ArmoryFlight.SEQUENCE) {
            if (showcase.item(p).isEmpty() || !ArmoryRules.isSummonable(showcase.item(p))) continue;
            ItemStack worn = player.getItemBySlot(p.slot);
            if (!worn.isEmpty() && !player.isCreative() && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) continue;
            flying.add(p);
        }
        if (flying.isEmpty()) return Component.translatable("message.mio_icif.showcase.empty");
        boolean sameDimension = player.level() == world;
        double straight = sameDimension ? player.position().distanceTo(showcase.launchTop()) : -1;
        long cost = mio_icif_armory.cost(flying.size(), straight, sameDimension);
        var payment = armory.getEnergyStorageInternal().scexReserveInternal(cost);
        if (payment == null) return Component.translatable("message.mio_icif.armory.no_energy", cost, armory.getEnergyStorage().getAmount());
        payment.commit();

        ServerLevel playerLevel = (ServerLevel) player.level();
        GlobalPos home = GlobalPos.of(world.dimension(), showcase.getBlockPos());
        Vec3 feet = player.position();
        int order = 0;
        long latest = 0;
        for (ArmoryPiece p : flying) {
            ItemStack item = showcase.takePiece(p);
            if (item.isEmpty()) continue;
            Vec3 dock = ArmoryFlight.dockPoint(feet, player.getBbHeight(), player.yBodyRot, p);
            Vec3 hold = ArmoryFlight.holdPoint(dock, player.yBodyRot);
            Vec3 start = ArmoryFlight.launchPoint(dock, showcase.launchTop(), sameDimension, p);
            int duration = ArmoryFlight.ticksFor(ArmoryFlight.approachLength(start, hold, ArmoryFlight.forward(player.yBodyRot), p));
            int delay = ArmoryFlight.SEQUENCE_LEAD / 2 + order++ * ArmoryFlight.STAGGER;
            playerLevel.addFreshEntity(ArmoryPieceEntity.deliver(playerLevel, player, item, p, start, home, 0, delay, duration));
            latest = Math.max(latest, delay + ArmoryFlight.arrivalTicks(duration));
        }
        // the worn suit bursts off and takes the showcase's place
        ArmoryPieceEntity.burstPurge(player, flying, home, showcase::storePiece);
        world.playSound(null, showcase.getBlockPos(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ARMORY_ALARM.get(),
            SoundSource.BLOCKS, 0.8F, 1.15F);
        String label = showcase.label().isEmpty() ? showcase.getBlockPos().toShortString() : showcase.label();
        return Component.translatable("message.mio_icif.showcase.summoned", label, cost);
    }

    /** Sends the worn suit home to the Armory (first empty suit), with the burst purge. */
    public static Component returnSuit(ServerPlayer player, mio_icif_armory armory) {
        if (!armory.isOwner(player)) return Component.translatable("message.mio_icif.armory.not_owner");
        if (!(armory.getLevel() instanceof ServerLevel world)) return Component.translatable("message.mio_icif.armory.failed");
        List<ArmoryPiece> worn = new ArrayList<>();
        for (ArmoryPiece p : ArmoryFlight.SEQUENCE) {
            ItemStack stack = player.getItemBySlot(p.slot);
            if (!stack.isEmpty() && ArmoryRules.isSummonable(stack) && ArmoryRules.fits(stack, p) && p.isArmor()) worn.add(p);
        }
        if (worn.isEmpty()) return Component.translatable("message.mio_icif.armory.nothing_worn");
        int set = 0;
        for (int s = 0; s < mio_icif_armory.SETS; s++) if (armory.pieceCount(s) == 0) { set = s; break; }
        boolean sameDimension = player.level() == world;
        double straight = sameDimension ? player.position().distanceTo(armory.launchTop()) : -1;
        long cost = mio_icif_armory.cost(worn.size(), straight, sameDimension) / 2;
        var payment = armory.getEnergyStorageInternal().scexReserveInternal(cost);
        if (payment == null) return Component.translatable("message.mio_icif.armory.no_energy", cost, armory.getEnergyStorage().getAmount());
        payment.commit();
        int target = set;
        int n = ArmoryPieceEntity.burstPurge(player, worn, GlobalPos.of(world.dimension(), armory.getBlockPos()),
            (p, stack) -> armory.store(target, p, stack));
        return Component.translatable("message.mio_icif.armory.returned", n, armory.setName(set));
    }
}
