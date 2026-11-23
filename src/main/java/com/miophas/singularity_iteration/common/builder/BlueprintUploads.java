// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.182: blueprints uploaded from a player's own computer, as Create's Schematic Table uploads its schematics. On a
 * dedicated server (or as a guest of a LAN world) the Blueprint Builder cannot see the player's {@code schematics} folder:
 * the Upload button of its screen lists that folder on the client, and the chosen file comes over in pieces of at most
 * {@link #CHUNK} bytes ({@link BuilderUploadPackets}).
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>One upload at a time per player, begun while the player has the builder's screen open (and the builder is not
 *       running). It goes to that builder, even if the screen closes before the last piece arrives.</li>
 *   <li>The name is a plain file name ending in {@code .nbt} or {@code .litematic}, at most 64 characters (letters, digits,
 *       space and {@code ._-()[]+}; no Windows device names). The size is at most the common config {@code uploadMaxKB}.</li>
 *   <li>The pieces come in order; the whole must match the SHA-256 the client sent first, and must read as a blueprint
 *       ({@link BlueprintReader}) before it is kept. A file already there with the same name and content is not sent again.</li>
 *   <li>Files are kept in {@code schematics/uploads/<player UUID>/} of the server; the oldest go when a player has more than
 *       {@code uploadFilesPerPlayer}. An upload idle for {@link #IDLE_TICKS} ticks, or of a player who left, is dropped.</li>
 * </ul>
 * The builder refers to an upload as {@code "<uuid>/<name>"} ({@link #resolve}), so it reloads it after a restart.
 */
public final class BlueprintUploads {
    private BlueprintUploads() {}

    /** The largest piece, in bytes (well under the 32 KiB limit of a client-to-server payload). */
    public static final int CHUNK = 24 * 1024;
    public static final long IDLE_TICKS = 600;
    /** Results (also sent to the client as the status code). */
    public static final int ACCEPT = 1, DONE = 2, MORE = 3, ERR_DISABLED = 10, ERR_NAME = 11, ERR_SIZE = 12, ERR_ORDER = 13, ERR_HASH = 14,
        ERR_READ = 15, ERR_IO = 16, ERR_RUNNING = 17, ERR_MENU = 18;
    private static final Set<String> DEVICES = Set.of("con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7",
        "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    private static final class Session {
        final String name;
        final int size;
        final byte[] sha;
        final ByteArrayOutputStream data;
        final ResourceKey<Level> dimension;
        final BlockPos builder;
        long lastTick;

        Session(String name, int size, byte[] sha, ResourceKey<Level> dimension, BlockPos builder, long now) {
            this.name = name;
            this.size = size;
            this.sha = sha;
            this.data = new ByteArrayOutputStream(Math.min(size, 1 << 20));
            this.dimension = dimension;
            this.builder = builder;
            this.lastTick = now;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static Path root() { return BlueprintReader.folder().resolve("uploads"); }

    public static Path folder(UUID owner) { return root().resolve(owner.toString()); }

    /** The largest upload in bytes (common config). */
    public static int maxBytes() { return Singularity_Iteration_Config.BUILDER_UPLOAD_MAX_KB.get() * 1024; }

    /** {@code name} if it may be used as an upload's file name, else null. */
    @Nullable
    public static String cleanName(@Nullable String name) {
        if (name == null || name.isEmpty() || name.length() > 64 || name.startsWith(".")) return null;
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".nbt") && !lower.endsWith(".litematic")) return null;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && " ._-()[]+".indexOf(c) < 0) return null;
        }
        String base = lower.substring(0, lower.indexOf('.'));
        return DEVICES.contains(base) ? null : name;
    }

    /** The file of an upload reference {@code "<uuid>/<name>"}, inside the uploads folder, or null. */
    @Nullable
    public static Path resolve(String ref) {
        int slash = ref.indexOf('/');
        if (slash <= 0) return null;
        UUID owner;
        try {
            owner = UUID.fromString(ref.substring(0, slash));
        } catch (IllegalArgumentException e) {
            return null;
        }
        String name = cleanName(ref.substring(slash + 1));
        if (name == null) return null;
        Path root = root().toAbsolutePath().normalize(), file = folder(owner).resolve(name).toAbsolutePath().normalize();
        return file.startsWith(root) ? file : null;
    }

    /** Reads the upload {@code ref}; null when it is gone or cannot be read. */
    @Nullable
    public static Blueprint read(String ref, HolderGetter<Block> blocks) {
        Path file = resolve(ref);
        return file == null ? null : BlueprintReader.read(file, ref.substring(ref.indexOf('/') + 1), blocks);
    }

    /**
     * Begins an upload of {@code name} ({@code size} bytes, SHA-256 {@code sha}) by {@code player} to {@code builder}:
     * {@link #ACCEPT} (send the pieces), {@link #DONE} (the same file is there already: it is loaded) or an error.
     */
    public static int begin(ServerPlayer player, BlueprintBuilderBlockEntity builder, String name, int size, byte[] sha) {
        long now = player.serverLevel().getGameTime();
        SESSIONS.values().removeIf(s -> now - s.lastTick > IDLE_TICKS || now < s.lastTick);
        SESSIONS.remove(player.getUUID());
        if (!Singularity_Iteration_Config.BUILDER_UPLOADS.get()) return ERR_DISABLED;
        String clean = cleanName(name);
        if (clean == null || sha == null || sha.length != 32) return ERR_NAME;
        if (size <= 0 || size > maxBytes()) return ERR_SIZE;
        if (builder.running()) return ERR_RUNNING;
        Path existing = folder(player.getUUID()).resolve(clean);
        try {
            if (Files.isRegularFile(existing) && Files.size(existing) == size && Arrays.equals(sha256(Files.readAllBytes(existing)), sha))
                return select(player, builder, clean) ? DONE : ERR_READ;
        } catch (IOException e) {
            return ERR_IO;
        }
        SESSIONS.put(player.getUUID(), new Session(clean, size, sha.clone(), builder.getLevel().dimension(), builder.getBlockPos(), now));
        return ACCEPT;
    }

    /** The piece at {@code offset}: {@link #MORE}, {@link #DONE} (the last one, kept and loaded) or an error (the upload ends). */
    public static int chunk(ServerPlayer player, int offset, byte[] bytes) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || offset != s.data.size() || bytes.length == 0 || bytes.length > CHUNK || offset + bytes.length > s.size) {
            SESSIONS.remove(player.getUUID());
            return ERR_ORDER;
        }
        s.data.write(bytes, 0, bytes.length);
        s.lastTick = player.serverLevel().getGameTime();
        if (s.data.size() < s.size) return MORE;
        SESSIONS.remove(player.getUUID());
        return finish(player, s);
    }

    private static int finish(ServerPlayer player, Session s) {
        byte[] bytes = s.data.toByteArray();
        if (!Arrays.equals(sha256(bytes), s.sha)) return ERR_HASH;
        Path dir = folder(player.getUUID()), part = dir.resolve(s.name + ".part"), file = dir.resolve(s.name);
        try {
            Files.createDirectories(dir);
            Files.write(part, bytes);
            ServerLevel level = player.server.getLevel(s.dimension);
            HolderGetter<Block> blocks = (level != null ? level : player.serverLevel()).holderLookup(Registries.BLOCK);
            boolean readable;
            try {
                readable = BlueprintReader.read(part, s.name, blocks) != null;
            } catch (Blueprint.TooLarge e) {
                readable = true;   // 0.1.11.187: a sound file; the builder says it is too large for the memory when it reads it
            }
            if (!readable) {
                Files.deleteIfExists(part);
                return ERR_READ;
            }
            Files.move(part, file, StandardCopyOption.REPLACE_EXISTING);
            prune(dir, file);
        } catch (IOException e) {
            Singularity_Iteration.LOGGER.warn("Blueprint upload of {} by {}: {}", s.name, player.getGameProfile().getName(), e.toString());
            try { Files.deleteIfExists(part); } catch (IOException ignored) { }
            return ERR_IO;
        }
        ServerLevel level = player.server.getLevel(s.dimension);
        if (level != null && level.isLoaded(s.builder) && level.getBlockEntity(s.builder) instanceof BlueprintBuilderBlockEntity builder && !builder.running())
            select(player, builder, s.name);
        return DONE;
    }

    /** Keeps the newest {@code uploadFilesPerPlayer} uploads of a player (the one just made always). */
    private static void prune(Path dir, Path keep) throws IOException {
        int limit = Singularity_Iteration_Config.BUILDER_UPLOAD_FILES.get();
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(Files::isRegularFile).filter(p -> !p.equals(keep) && cleanName(p.getFileName().toString()) != null)
                .sorted(Comparator.comparingLong(BlueprintUploads::modified).reversed()).toList();
        }
        for (int i = limit - 1; i < files.size(); i++) Files.deleteIfExists(files.get(i));
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private static boolean select(ServerPlayer player, BlueprintBuilderBlockEntity builder, String name) {
        return builder.selectUpload(player, player.getUUID() + "/" + name);
    }

    public static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Whether {@code player} has an upload under way (the tests; the screen asks its client side). */
    public static boolean uploading(UUID player) { return SESSIONS.containsKey(player); }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            SESSIONS.remove(event.getEntity().getUUID());
        }
    }
}
