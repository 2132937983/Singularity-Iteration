package com.singularity_iteration.mio_icif.Blocks.entity.build;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 建筑泡沫方块实体
 * 存储伪装方块的ID，用于遮蔽器复制纹理功能
 */
@SuppressWarnings("null")
public class mio_icif_block_foam_entity extends BlockEntity {

    private static final String TAG_DISGUISED_BLOCK = "DisguisedBlockId";

    private ResourceLocation disguisedBlockId = null;

    public mio_icif_block_foam_entity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.FOAM_ENTITY_TYPE.get(), pos, state);
    }

    public ResourceLocation getDisguisedBlockId() {
        return disguisedBlockId;
    }

    public void setDisguisedBlockId(ResourceLocation blockId) {
        this.disguisedBlockId = blockId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void clearDisguise() {
        this.disguisedBlockId = null;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean hasDisguise() {
        return disguisedBlockId != null;
    }

    public Block getDisguisedBlock() {
        if (disguisedBlockId == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(disguisedBlockId);
        if (block == Blocks.AIR) {
            return null;
        }
        return block;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (disguisedBlockId != null) {
            tag.putString(TAG_DISGUISED_BLOCK, disguisedBlockId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_DISGUISED_BLOCK)) {
            String idStr = tag.getString(TAG_DISGUISED_BLOCK);
            ResourceLocation id = ResourceLocation.tryParse(idStr);
            if (id != null) {
                this.disguisedBlockId = id;
            } else {
                this.disguisedBlockId = null;
            }
        } else {
            this.disguisedBlockId = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (disguisedBlockId != null) {
            tag.putString(TAG_DISGUISED_BLOCK, disguisedBlockId.toString());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains(TAG_DISGUISED_BLOCK)) {
            String idStr = tag.getString(TAG_DISGUISED_BLOCK);
            ResourceLocation id = ResourceLocation.tryParse(idStr);
            if (id != null) {
                this.disguisedBlockId = id;
            } else {
                this.disguisedBlockId = null;
            }
        } else {
            this.disguisedBlockId = null;
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

