// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader;
import com.miophas.singularity_iteration.common.energy.ChunkLoaderPolicy;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

/**
 * {@code /mio_icif chunkloaders} (0.1.10.11, permission level 2): the SI chunk loaders of the server.
 * <ul>
 *   <li>(no argument): totals, rules, then every loaded loader (dimension, position, owner, chunks
 *       granted / selected, state) and the parked ones; a click on a position teleports there.</li>
 *   <li>{@code player <name>}: the loaders of one owner.</li>
 *   <li>{@code disable <pos> [<dimension>]} / {@code enable ...}: admin lock of one loader (saved
 *       with it). Enabling wakes the loader at once.</li>
 *   <li>{@code disable_all}: locks every loaded loader (emergency stop of all SI chunk loading).</li>
 * </ul>
 */
public final class ChunkLoaderCommand {
    private ChunkLoaderCommand() { }
    private static final int MAX_LINES = 60;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("chunkloaders")
            .requires(source -> source.hasPermission(2))
            .executes(context -> list(context.getSource(), null))
            .then(Commands.literal("player").then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> list(context.getSource(), StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("disable").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> lock(context, true, false))
                .then(Commands.argument("dimension", DimensionArgument.dimension()).executes(context -> lock(context, true, true)))))
            .then(Commands.literal("enable").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(context -> lock(context, false, false))
                .then(Commands.argument("dimension", DimensionArgument.dimension()).executes(context -> lock(context, false, true)))))
            .then(Commands.literal("disable_all").executes(context -> disableAll(context.getSource())))));
    }

    private static int list(CommandSourceStack source, String player) {
        List<ChunkLoaderPolicy.Entry> entries = ChunkLoaderPolicy.entries();
        if (player != null) entries.removeIf(e -> !player.equalsIgnoreCase(e.ownerName()));
        entries.sort(Comparator.comparing((ChunkLoaderPolicy.Entry e) -> e.at().dimension().location().toString())
            .thenComparingLong(e -> e.at().pos().asLong()));
        int granted = 0, activeCount = 0;
        for (var e : entries) if (e.granted() > 0) { activeCount++; granted += e.granted(); }
        int loaded = entries.size(), chunks = granted, act = activeCount;
        source.sendSuccess(() -> Component.translatable("command.mio_icif.chunkloaders.header", loaded, act, chunks), false);
        source.sendSuccess(() -> Component.translatable("command.mio_icif.chunkloaders.rules",
            on(ChunkLoaderPolicy.enabled()), ChunkLoaderPolicy.maxChunksPerLoader(), limit(ChunkLoaderPolicy.maxChunksPerPlayer()),
            limit(ChunkLoaderPolicy.maxLoadersPerPlayer()), ChunkLoaderPolicy.offlineMinutes() < 0 ? "-" : Integer.toString(ChunkLoaderPolicy.offlineMinutes()),
            ChunkLoaderPolicy.euPerChunk(), on(ChunkLoaderPolicy.randomTicks())).withStyle(ChatFormatting.GRAY), false);
        int shown = 0;
        for (var e : entries) {
            if (shown++ >= MAX_LINES) {
                int rest = entries.size() - MAX_LINES;
                source.sendSuccess(() -> Component.translatable("command.mio_icif.chunkloaders.more", rest), false);
                break;
            }
            String state = e.status() >= 0 && e.status() < mio_icif_chunk_loader.STATUS_NAMES.length ? mio_icif_chunk_loader.STATUS_NAMES[e.status()] : "starting";
            MutableComponent line = Component.translatable("command.mio_icif.chunkloaders.entry", where(e.at()),
                e.owner() == null ? Component.translatable("command.mio_icif.chunkloaders.no_owner") : Component.literal(e.ownerName()),
                e.granted(), e.selected(), Component.translatable("gui.mio_icif.chunk_loader.state." + state));
            source.sendSuccess(() -> line, false);
        }
        var parked = ChunkLoaderPolicy.parked(source.getServer());
        if (player != null) parked.removeIf(p -> entries.stream().noneMatch(e -> e.at().equals(p.at())) && !ownerNamed(source, p, player));
        if (!parked.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.mio_icif.chunkloaders.parked_header", parked.size()), false);
            int n = 0;
            for (var p : parked) {
                if (n++ >= MAX_LINES) break;
                MutableComponent line = Component.translatable("command.mio_icif.chunkloaders.parked", where(p.at()),
                    Component.translatable("command.mio_icif.chunkloaders.reason." + p.reason().name().toLowerCase(java.util.Locale.ROOT)));
                source.sendSuccess(() -> line, false);
            }
        }
        return loaded;
    }

    private static boolean ownerNamed(CommandSourceStack source, ChunkLoaderPolicy.Parked p, String name) {
        if (p.owner() == null) return false;
        var profile = source.getServer().getProfileCache() == null ? null : source.getServer().getProfileCache().get(p.owner()).orElse(null);
        return profile != null && name.equalsIgnoreCase(profile.getName());
    }

    private static Component where(GlobalPos at) {
        BlockPos p = at.pos();
        String text = at.dimension().location() + " " + p.getX() + " " + p.getY() + " " + p.getZ();
        String tp = "/execute in " + at.dimension().location() + " run tp @s " + p.getX() + " " + (p.getY() + 1) + " " + p.getZ();
        return Component.literal(text).withStyle(style -> style.withColor(ChatFormatting.AQUA)
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.mio_icif.chunkloaders.teleport"))));
    }

    private static Component on(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private static String limit(int value) {
        return value <= 0 ? "-" : Integer.toString(value);
    }

    private static int lock(CommandContext<CommandSourceStack> context, boolean disable, boolean withDimension) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = withDimension ? DimensionArgument.getDimension(context, "dimension") : source.getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);   // admin command: load the chunk if it is not
        if (!(level.getBlockEntity(pos) instanceof mio_icif_chunk_loader loader)) {
            source.sendFailure(Component.translatable("command.mio_icif.chunkloaders.not_found", where(GlobalPos.of(level.dimension(), pos))));
            return 0;
        }
        loader.setAdminDisabled(disable);
        if (!disable) ChunkLoaderPolicy.wake(source.getServer(), GlobalPos.of(level.dimension(), pos));
        source.sendSuccess(() -> Component.translatable(disable ? "command.mio_icif.chunkloaders.disabled" : "command.mio_icif.chunkloaders.enabled",
            where(GlobalPos.of(level.dimension(), pos))), true);
        return 1;
    }

    private static int disableAll(CommandSourceStack source) {
        int count = 0;
        for (var e : ChunkLoaderPolicy.entries()) {
            ServerLevel level = source.getServer().getLevel(e.at().dimension());
            if (level != null && level.getBlockEntity(e.at().pos()) instanceof mio_icif_chunk_loader loader && !loader.adminDisabled()) {
                loader.setAdminDisabled(true);
                count++;
            }
        }
        int n = count;
        source.sendSuccess(() -> Component.translatable("command.mio_icif.chunkloaders.disabled_all", n), true);
        return count;
    }
}
