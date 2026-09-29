// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Server-owned menu binding; never loads a chunk to authorize an action. */
public final class MachineMenuAccess {
    private MachineMenuAccess(){}
    public static boolean valid(Player player,BlockEntity owner) {
        if(player==null||owner==null||owner.isRemoved()||!player.isAlive()||player.isSpectator()
                ||!(owner.getLevel() instanceof ServerLevel level)||player.level()!=level||!level.getServer().isSameThread())return false;
        var pos=owner.getBlockPos();
        if(player.distanceToSqr(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)>64)return false;
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk!=null&&level.shouldTickBlocksAt(ChunkPos.asLong(pos))
            &&chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.CHECK)==owner;
    }
    public static boolean action(Player player,AbstractContainerMenu menu,BlockEntity owner) {
        return player.containerMenu==menu&&valid(player,owner);
    }
}
