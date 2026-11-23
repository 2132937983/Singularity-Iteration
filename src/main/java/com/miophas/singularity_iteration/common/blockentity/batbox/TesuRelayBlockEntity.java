package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.block.energycontainer.TesuRelayBlock;
import com.miophas.singularity_iteration.common.block.energycontainer.TesuRelayColumnBlock;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.167: the relay is the base of a pylon three blocks tall ({@link TesuRelayBlock}). A relay from before grows its column
 * the first time it ticks, when the two blocks above are free (once: {@code TesuColumn}); the pylon lights while powered.
 *
 * <p>0.1.11.158: the TESU relay. Linked (with the TESU wireless connector) to a wireless output port or to another relay whose cube
 * it stands in, it spreads the wireless field ±{@link TesuWireless#RANGE} blocks round itself, without loss. It keeps the
 * position it is linked to, and (0.1.11.159) once a second whether its links reach a TESU, sent to clients: a powered relay
 * draws the particle stream from the node it is linked to, as the wireless power transmission node does.
 */
@SuppressWarnings("null")
public class TesuRelayBlockEntity extends BlockEntity {
    @Nullable private BlockPos parent;
    private boolean powered;
    private int checkIn;
    /** 0.1.11.167: whether the column was looked after (grown on a relay from before, or placed with the relay). */
    private boolean column;

    public TesuRelayBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.TESU_RELAY.get(), pos, state);
    }

    @Nullable public BlockPos parent() { return parent; }
    /** Whether the relay's links reached a formed TESU at the last check (the client draws the stream from it). */
    public boolean isPowered() { return powered; }

    /** Links the relay to the node at {@code pos} (or unlinks it: {@code null}). */
    public void link(@Nullable BlockPos pos) {
        parent = pos == null ? null : pos.immutable();
        checkIn = 0;
        setChanged();
        sync();
    }

    /** The TESU feeding this relay through its links, or {@code null}. */
    @Nullable
    public TesuControllerBlockEntity feed() { return level == null ? null : TesuWireless.feed(level, worldPosition); }

    public static void tick(Level level, BlockPos pos, BlockState state, TesuRelayBlockEntity relay) {
        if (level.isClientSide() || --relay.checkIn > 0) return;
        relay.checkIn = 20;
        if (!relay.column) {
            relay.column = true;
            if (!TesuRelayColumnBlock.standsOn(level, pos) && pos.getY() + 2 < level.getMaxBuildHeight()
                && level.getBlockState(pos.above()).canBeReplaced() && level.getBlockState(pos.above(2)).canBeReplaced())
                TesuRelayColumnBlock.place(level, pos, relay.powered);
            relay.setChanged();
        }
        boolean now = relay.feed() != null;
        if (now != relay.powered) {
            relay.powered = now;
            relay.sync();
        }
        // the pylon's lights follow (a state from before 0.1.11.167 has them dark)
        BlockState current = level.getBlockState(pos);
        if (current.hasProperty(TesuRelayBlock.POWERED) && current.getValue(TesuRelayBlock.POWERED) != relay.powered)
            level.setBlock(pos, current.setValue(TesuRelayBlock.POWERED, relay.powered), Block.UPDATE_CLIENTS);
        TesuRelayColumnBlock.power(level, pos, relay.powered);
    }

    private void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) TesuWireless.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) TesuWireless.remove(level, worldPosition);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (parent != null) tag.putIntArray("TesuParent", new int[]{parent.getX(), parent.getY(), parent.getZ()});
        if (column) tag.putBoolean("TesuColumn", true);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] p = tag.getIntArray("TesuParent");
        parent = p.length == 3 ? new BlockPos(p[0], p[1], p[2]) : null;
        column = tag.getBoolean("TesuColumn");
        powered = tag.getBoolean("TesuPowered");   // only in the client update
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putBoolean("TesuPowered", powered);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
