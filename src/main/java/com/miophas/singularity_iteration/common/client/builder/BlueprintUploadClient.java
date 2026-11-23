// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.builder.BlueprintReader;
import com.miophas.singularity_iteration.common.builder.BlueprintUploads;
import com.miophas.singularity_iteration.common.builder.BuilderUploadPackets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.182: the client side of a blueprint upload ({@link BlueprintUploads}): lists this computer's {@code schematics}
 * folder, reads the chosen file, sends its hash first and, once the server accepts, the pieces ({@link #PER_TICK} a tick,
 * about 1 MB a second). The Blueprint Builder screen shows the progress. A status from the server ends it; so does leaving
 * the world or {@link #TIMEOUT} ticks without an answer.
 */
@OnlyIn(Dist.CLIENT)
public final class BlueprintUploadClient {
    private BlueprintUploadClient() {}

    /** A file of the local schematics folder. */
    public record LocalFile(String name, long size) {}

    static final int PER_TICK = 2, TIMEOUT = 600;
    /** The largest file the client reads (the server's own limit is usually lower; it says so). */
    static final long MAX_BYTES = 16L << 20;
    private static final int IDLE = 0, WAITING = 1, SENDING = 2, FINISHING = 3;
    private static int state = IDLE, sent, waited;
    private static byte[] data = new byte[0];
    private static String name = "";

    public static boolean busy() { return state != IDLE; }

    /** The share sent (0..1), or -1 when nothing is being uploaded. */
    public static float progress() { return state == IDLE ? -1 : sent / (float) Math.max(1, data.length); }

    /** Whether the server has the whole file and is checking it. */
    public static boolean finishing() { return state == FINISHING; }

    /** The {@code .nbt} and {@code .litematic} files of this computer's schematics folder, with their sizes. */
    public static List<LocalFile> localFiles() {
        Path dir = BlueprintReader.folder();
        List<LocalFile> out = new ArrayList<>();
        for (String n : BlueprintReader.list()) {
            try {
                out.add(new LocalFile(n, Files.size(dir.resolve(n))));
            } catch (IOException e) {
                out.add(new LocalFile(n, -1));
            }
        }
        return out;
    }

    /** Begins uploading {@code fileName} to the builder menu {@code containerId}; null, or why it cannot. */
    @Nullable
    public static Component start(int containerId, String fileName) {
        if (busy()) return Component.translatable("gui.mio_icif.builder.upload.busy");
        if (BlueprintUploads.cleanName(fileName) == null) return Component.translatable("message.mio_icif.builder.upload.error." + BlueprintUploads.ERR_NAME);
        Path dir = BlueprintReader.folder(), file = dir.resolve(fileName).normalize();
        if (!file.getParent().equals(dir)) return Component.translatable("message.mio_icif.builder.upload.error." + BlueprintUploads.ERR_NAME);
        byte[] bytes;
        try {
            if (Files.size(file) > MAX_BYTES) return Component.translatable("message.mio_icif.builder.upload.error." + BlueprintUploads.ERR_SIZE, MAX_BYTES >> 10);
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            return Component.translatable("gui.mio_icif.builder.upload.unreadable");
        }
        data = bytes;
        name = fileName;
        sent = 0;
        waited = 0;
        state = WAITING;
        PacketDistributor.sendToServer(new BuilderUploadPackets.Begin(containerId, fileName, bytes.length, BlueprintUploads.sha256(bytes)));
        return null;
    }

    /** A status from the server: accepted (send the pieces), done (loaded), or an error (the server wrote why in the chat). */
    public static void onStatus(int code, int extra) {
        if (code == BlueprintUploads.ACCEPT) {
            if (state == WAITING) { state = SENDING; waited = 0; }
            return;
        }
        if (code == BlueprintUploads.DONE) {
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(Component.translatable("message.mio_icif.builder.upload.done", name), true);
        }
        reset();
    }

    public static void tick() {
        if (state == IDLE) return;
        if (Minecraft.getInstance().getConnection() == null) { reset(); return; }
        if (state != SENDING) {
            if (++waited > TIMEOUT) reset();
            return;
        }
        for (int k = 0; k < PER_TICK && sent < data.length; k++) {
            int n = Math.min(BlueprintUploads.CHUNK, data.length - sent);
            PacketDistributor.sendToServer(new BuilderUploadPackets.Piece(sent, Arrays.copyOfRange(data, sent, sent + n)));
            sent += n;
        }
        if (sent >= data.length) { state = FINISHING; waited = 0; }
    }

    private static void reset() {
        state = IDLE;
        data = new byte[0];
        sent = 0;
        waited = 0;
    }
}
