package example.sicore;

import static example.sicore.Round33GameTests.call;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.182: a blueprint uploaded from a player's computer (the Blueprint Builder's Upload button, as Create's Schematic
 * Table) comes in pieces in order, is checked against its hash and read as a blueprint, kept in the player's upload folder
 * and loaded by the builder; the same file again is not sent; bad names, an oversized file, a piece out of order and a
 * file that is no blueprint are refused (and nothing is kept). (Classes by name.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111182GameTests {
    private Round111182GameTests() {}

    private static final BlockPos AT = new BlockPos(1, 2, 1);
    private static final int ACCEPT = 1, DONE = 2, MORE = 3, ERR_NAME = 11, ERR_SIZE = 12, ERR_ORDER = 13, ERR_READ = 15, READY = 1;

    /** A structure file of two stone blocks side by side. */
    static byte[] structure() throws Exception {   // also Round111184GameTests
        CompoundTag root = new CompoundTag();
        ListTag size = new ListTag();
        size.add(IntTag.valueOf(2)); size.add(IntTag.valueOf(1)); size.add(IntTag.valueOf(1));
        root.put("size", size);
        ListTag palette = new ListTag();
        CompoundTag stone = new CompoundTag();
        stone.put("Name", StringTag.valueOf("minecraft:stone"));
        palette.add(stone);
        root.put("palette", palette);
        ListTag blocks = new ListTag();
        for (int x = 0; x < 2; x++) {
            CompoundTag b = new CompoundTag();
            b.putInt("state", 0);
            ListTag pos = new ListTag();
            pos.add(IntTag.valueOf(x)); pos.add(IntTag.valueOf(0)); pos.add(IntTag.valueOf(0));
            b.put("pos", pos);
            blocks.add(b);
        }
        root.put("blocks", blocks);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, out);
        return out.toByteArray();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void uploadedBlueprintIsKeptAndLoaded(GameTestHelper h) throws Exception {
        h.setBlock(AT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_blueprint_builder")));
        BlockEntity builder = h.getBlockEntity(AT);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        Class<?> up = Round33GameTests.common("builder.BlueprintUploads");
        Path dir = (Path) call(up, "folder", p.getUUID());
        try {
            byte[] file = structure(), sha = (byte[]) call(up, "sha256", file);
            String name = "si gametest (upload).nbt";
            // in two pieces, in order
            h.assertTrue((int) call(up, "begin", p, builder, name, file.length, sha) == ACCEPT, "the upload is not accepted");
            int half = file.length / 2;
            h.assertTrue((int) call(up, "chunk", p, 0, Arrays.copyOfRange(file, 0, half)) == MORE, "the first piece ends the upload");
            h.assertTrue((int) call(up, "chunk", p, half, Arrays.copyOfRange(file, half, file.length)) == DONE, "the last piece does not finish it");
            h.assertTrue(Files.isRegularFile(dir.resolve(name)), "the upload is not kept in " + dir);
            h.assertTrue(call(builder, "upload").equals(p.getUUID() + "/" + name), "the builder uses " + call(builder, "upload"));
            h.assertTrue((int) call(builder, "status") == READY, "the builder is not ready: " + call(builder, "status"));
            // the same file again is not sent
            h.assertTrue((int) call(up, "begin", p, builder, name, file.length, sha) == DONE, "the same file is sent again");
            h.assertTrue(!(boolean) call(up, "uploading", p.getUUID()), "an upload is still open");
            // refused: names, size, a piece out of order, a file that is no blueprint
            for (String bad : new String[]{"../evil.nbt", "sub/evil.nbt", "con.nbt", "notes.txt", ".hidden.nbt", "a".repeat(70) + ".nbt"})
                h.assertTrue((int) call(up, "begin", p, builder, bad, file.length, sha) == ERR_NAME, "the name " + bad + " is taken");
            h.assertTrue((int) call(up, "begin", p, builder, "big.nbt", (int) call(up, "maxBytes") + 1, sha) == ERR_SIZE, "an oversized file is taken");
            h.assertTrue((int) call(up, "begin", p, builder, "order.nbt", file.length, sha) == ACCEPT, "the second upload is not accepted");
            h.assertTrue((int) call(up, "chunk", p, 5, Arrays.copyOfRange(file, 5, 9)) == ERR_ORDER, "a piece out of order is taken");
            h.assertTrue((int) call(up, "chunk", p, 0, file) == ERR_ORDER, "the upload went on after the broken piece");
            byte[] junk = "this is not a blueprint".getBytes(StandardCharsets.UTF_8);
            h.assertTrue((int) call(up, "begin", p, builder, "junk.nbt", junk.length, call(up, "sha256", junk)) == ACCEPT, "the junk upload is not begun");
            h.assertTrue((int) call(up, "chunk", p, 0, junk) == ERR_READ, "a file that is no blueprint is kept");
            h.assertTrue(!Files.exists(dir.resolve("junk.nbt")) && !Files.exists(dir.resolve("junk.nbt.part")), "the junk left a file");
            h.assertTrue(call(builder, "upload").equals(p.getUUID() + "/" + name), "a refused upload changed the builder");
        } finally {
            if (Files.isDirectory(dir)) try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
            p.discard();
        }
        h.succeed();
    }
}
