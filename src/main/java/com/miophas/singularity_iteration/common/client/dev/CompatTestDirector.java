package com.miophas.singularity_iteration.common.client.dev;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Development-only GameTest runner for the client dist (excluded from the release jar).
 *
 * <p>Some mods (GregTech Modern) cannot boot on the dev dedicated-server dist, so their
 * compat GameTests run inside a headless client instead: {@code -Dsi.compattest=<file>}
 * creates a flat world, runs {@code /test runall}, writes the summary line to the file and
 * quits.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class CompatTestDirector {
    private static final String OUT = System.getProperty("si.compattest");
    private static boolean worldRequested, started, finished;
    private static int ticks, pending, passed, failed;
    private static final StringBuilder report = new StringBuilder();
    private static final java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();

    /** One test at a time: a new /test run clears the previous run's tracker. */
    private static void runNext() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        String next = queue.poll();
        if (server == null || next == null) return;
        server.execute(() -> {
            var player = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
            if (player != null) server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4), "test run " + next);
        });
    }

    private CompatTestDirector() {}

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (OUT == null || finished) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (!worldRequested && mc.getOverlay() == null && mc.screen != null) {
                worldRequested = true;
                var settings = new LevelSettings("si_compat", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("si_compat_" + System.currentTimeMillis(), settings,
                    new WorldOptions(4321L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            }
            return;
        }
        if (mc.player == null || started || ++ticks < 60) return;
        started = true;
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player == null) return;
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), "gamerule doDaylightCycle false");
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), "time set noon");
            String only = System.getProperty("si.compattests", "");
            if (only.isBlank()) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), "test runall");
            } else {
                for (String name : only.split(",")) queue.add(name.strip());
                pending = queue.size();
                runNext();
            }
        });
    }

    @SubscribeEvent
    static void chat(ClientChatReceivedEvent event) {
        if (OUT == null || finished) return;
        String text = event.getMessage().getString();
        Singularity_Iteration.LOGGER.info("[CompatTest] {}", text);
        if (pending > 0) {
            boolean about = text.startsWith("compat");
            if (about && text.contains(" passed!")) { passed++; report.append(text).append(System.lineSeparator()); }
            else if ((about && text.contains(" failed")) || text.contains("Unknown") || text.contains("Incorrect")) {
                failed++; report.append(text).append(System.lineSeparator());
            }
            if (passed + failed < pending) {
                if (about && (text.contains(" passed!") || text.contains(" failed"))) runNext();
                return;
            }
            text = report + (failed == 0 ? "All " + passed + " selected tests passed" : failed + " selected tests failed");
        } else if (!text.contains("required tests passed") && !text.contains("required tests failed")
                && !text.contains("required test failed")) return;
        finished = true;
        try {
            Files.writeString(Path.of(OUT), text + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Singularity_Iteration.LOGGER.error("Cannot write compat test result", e);
        }
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }
}
