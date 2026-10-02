package com.miophas.singularity_iteration.common.menu.wiring;

import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyTerminalBlockEntity;
import com.miophas.singularity_iteration.common.network.EnergyTerminalSyncPacket;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Terminal menu (no slots). The server pushes a full {@link EnergyNetworkSnapshot}
 * whenever the terminal publishes a new one (once per second) while the menu is open.
 */
@SuppressWarnings("null")
public class EnergyTerminalMenu extends AbstractContainerMenu {
    @Nullable private final EnergyTerminalBlockEntity terminal;
    private final Player player;
    @Nullable private BlockPos pos;
    private int sentVersion = -1;
    private EnergyNetworkSnapshot clientSnapshot = new EnergyNetworkSnapshot();
    private int clientSyncs;

    public EnergyTerminalMenu(int containerId, Inventory inventory, @Nullable RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (EnergyTerminalBlockEntity) null);
        if (buf != null) pos = buf.readBlockPos();
    }

    public EnergyTerminalMenu(int containerId, Inventory inventory, @Nullable EnergyTerminalBlockEntity terminal) {
        super(mio_icif_menus.ENERGY_TERMINAL_MENU_TYPE.get(), containerId);
        this.terminal = terminal;
        this.player = inventory.player;
        if (terminal != null) pos = terminal.getBlockPos();
    }

    @Nullable public BlockPos pos() { return pos; }
    public EnergyNetworkSnapshot snapshot() { return terminal != null && !player.level().isClientSide ? terminal.snapshot() : clientSnapshot; }
    public int syncCount() { return clientSyncs; }

    public void acceptSync(EnergyNetworkSnapshot snapshot) {
        clientSnapshot = snapshot;
        clientSyncs++;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (terminal == null || !(player instanceof ServerPlayer serverPlayer) || terminal.version() == sentVersion) return;
        sentVersion = terminal.version();
        PacketDistributor.sendToPlayer(serverPlayer, new EnergyTerminalSyncPacket(containerId, terminal.snapshot()));
    }

    /** Button 1: global mode on, 0: this sub-network only. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (terminal == null || (id != 0 && id != 1)) return false;
        terminal.setGlobalMode(id == 1);
        return true;
    }

    /** Server side: validated toggle request from the GUI. */
    public void toggle(BlockPos device) {
        if (terminal != null) terminal.toggle(device);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (terminal == null) return true;
        if (terminal.isRemoved() || terminal.getLevel() != player.level()) return false;
        BlockPos at = terminal.getBlockPos();
        return player.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) <= 64.0;
    }
}
