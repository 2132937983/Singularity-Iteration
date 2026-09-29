// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.miophas.singularity_iteration.common.blockentity.pipe.mio_icif_pipe_item;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;

/** Explicit operator reconciliation. Never guesses what a third-party inventory committed. */
public final class PipeRecoveryCommand {
    private PipeRecoveryCommand() { }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("pipe").requires(source -> source.hasPermission(2))
            .then(Commands.literal("inspect").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(context -> inspect(context))))
            .then(Commands.literal("resolve").then(Commands.argument("pos",BlockPosArgument.blockPos())
                .then(Commands.argument("transfer",StringArgumentType.word())
                    .then(Commands.argument("confirmed",IntegerArgumentType.integer(0)).executes(PipeRecoveryCommand::resolve)))))));
    }
    private static mio_icif_pipe_item pipe(CommandContext<CommandSourceStack> context) {
        var source=context.getSource();var pos=BlockPosArgument.getBlockPos(context,"pos");
        if(!source.hasPermission(2) || !source.getServer().isSameThread())return null;
        var chunk=source.getLevel().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk!=null && chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.CHECK) instanceof mio_icif_pipe_item pipe ? pipe : null;
    }
    private static int inspect(CommandContext<CommandSourceStack> context) {
        var pipe=pipe(context);
        if(pipe==null){context.getSource().sendFailure(Component.literal("No loaded item pipe at this position."));return 0;}
        context.getSource().sendSuccess(() -> Component.literal("Pipe transfer: "+pipe.getUncertainTransferId()+"; phase="
            +pipe.getUncertainTransferPhase()+"; buffer="+pipe.getBufferItem()+"; uncertain="+pipe.getUncertainItem()
            +". Resolve only after verifying the actual accepted (insert) or removed (extract) count in the external inventory."),false);
        return 1;
    }
    private static int resolve(CommandContext<CommandSourceStack> context) {
        var pipe=pipe(context);String id=StringArgumentType.getString(context,"transfer");
        int confirmed=IntegerArgumentType.getInteger(context,"confirmed");
        if(pipe==null || !pipe.resolveUncertainTransfer(id,confirmed)) {
            context.getSource().sendFailure(Component.literal("Unresolved: pipe unavailable, stale transfer ID, invalid count or unknown item data. Nothing was changed."));return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Resolved pipe transfer "+id+" with externally confirmed count "+confirmed+"."),true);
        return 1;
    }
}
