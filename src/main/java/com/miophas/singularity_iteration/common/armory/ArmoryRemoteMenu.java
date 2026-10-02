package com.miophas.singularity_iteration.common.armory;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Remote controller console. Has no slots; the server pushes an {@link ArmorySnapshot}
 * on open and then twice a second (only when it changed), summon requests come back as
 * {@link ArmoryActionPacket}s.
 */
@SuppressWarnings("null")
public class ArmoryRemoteMenu extends AbstractContainerMenu {
    private static final int SYNC_INTERVAL = 10;

    private final Player player;
    private final GlobalPos target;
    private final InteractionHand hand;
    private int timer;
    private int lastHash = Integer.MIN_VALUE;
    /** Client copy, replaced by sync packets. */
    @Nullable private ArmorySnapshot snapshot;
    private int snapshotVersion;
    /** Client: last summon result line from the server. */
    private net.minecraft.network.chat.Component lastMessage = net.minecraft.network.chat.Component.empty();

    public ArmoryRemoteMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, GlobalPos.STREAM_CODEC.decode(buf), buf.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public ArmoryRemoteMenu(int containerId, Inventory inventory, GlobalPos target, InteractionHand hand) {
        super(ArmoryRegistry.REMOTE_MENU.get(), containerId);
        this.player = inventory.player;
        this.target = target;
        this.hand = hand;
    }

    public GlobalPos target() { return target; }
    @Nullable public ArmorySnapshot snapshot() { return snapshot; }
    public int snapshotVersion() { return snapshotVersion; }
    public net.minecraft.network.chat.Component lastMessage() { return lastMessage; }

    public void acceptSnapshot(ArmorySnapshot s, net.minecraft.network.chat.Component message) {
        snapshot = s;
        snapshotVersion++;
        if (message != null && !message.getString().isEmpty()) lastMessage = message;
    }

    /** Server: resolves the Armory (loading its chunk if needed) and checks ownership. */
    @Nullable
    public mio_icif_armory armory() {
        if (!(player instanceof ServerPlayer sp)) return null;
        ServerLevel level = sp.server.getLevel(target.dimension());
        if (level == null || !level.isInWorldBounds(target.pos())) return null;
        return level.getBlockEntity(target.pos()) instanceof mio_icif_armory a ? a : null;
    }

    public ArmorySnapshot buildSnapshot() {
        mio_icif_armory armory = armory();
        if (armory == null) return ArmorySnapshot.of(target, ArmorySnapshot.MISSING);
        if (armory.owner() == null) return ArmorySnapshot.of(target, ArmorySnapshot.UNBOUND);
        if (!armory.isOwner(player)) return ArmorySnapshot.of(target, ArmorySnapshot.NOT_OWNER);
        return ArmorySnapshot.of(target, armory, ArmorySnapshot.OK);
    }

    public void push(net.minecraft.network.chat.Component message) {
        if (!(player instanceof ServerPlayer sp)) return;
        ArmorySnapshot s = buildSnapshot();
        lastHash = hash(s);
        if (sp.connection != null && sp.connection.hasChannel(ArmoryRemoteSyncPacket.TYPE)) {
            PacketDistributor.sendToPlayer(sp, new ArmoryRemoteSyncPacket(containerId, s, message));
        }
    }

    private static int hash(ArmorySnapshot s) {
        int h = s.status() * 31 + Long.hashCode(s.energy()) * 17 + (s.busy() ? 1 : 0) + s.log().size() * 101 + s.names().hashCode();
        for (int i = 0; i < s.suits().size(); i++) for (ItemStack st : s.suits().get(i)) h = h * 31 + ItemStack.hashItemAndComponents(st) + st.getCount();
        if (!s.log().isEmpty()) h = h * 31 + Long.hashCode(s.log().getFirst().epochMillis());
        return h;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer)) return;
        if (timer-- > 0) return;
        timer = SYNC_INTERVAL;
        ArmorySnapshot s = buildSnapshot();
        if (hash(s) == lastHash) return;
        push(net.minecraft.network.chat.Component.empty());
    }

    /** Server: one-tap summon from the list. */
    public void summon(int set) {
        if (!(player instanceof ServerPlayer sp)) return;
        mio_icif_armory armory = armory();
        net.minecraft.network.chat.Component message;
        if (armory == null) message = net.minecraft.network.chat.Component.translatable("message.mio_icif.armory.missing");
        else message = armory.summon(sp, set).message();
        sp.displayClientMessage(message, true);
        push(message);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        ItemStack held = player.getItemInHand(hand);
        return player.isAlive() && held.getItem() instanceof ArmoryRemoteItem;
    }
}
