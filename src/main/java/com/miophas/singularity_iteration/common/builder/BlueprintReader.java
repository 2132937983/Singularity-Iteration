// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

/**
 * Reads blueprints (0.1.8.0) without a dependency on the schematic mods.
 *
 * <ul>
 *   <li><b>Structure NBT</b> ({@code .nbt}): the vanilla structure format. Create schematics use
 *       this format.</li>
 *   <li><b>Litematica</b> ({@code .litematic}): all regions, with the packed block state
 *       arrays of the Litematica format.</li>
 *   <li><b>Create schematic item</b>: a deployed schematic (after the Schematic Table upload).
 *       The item names the file in {@code schematics/uploaded/<owner>/}; the anchor, rotation
 *       and mirror come from the item.</li>
 * </ul>
 *
 * <p>Files come from the {@code schematics} folder of the game directory (on a dedicated server,
 * the server folder). The file list is read only on request. A path never leaves the folder.
 * Block entity data is not copied: a container is placed empty.
 */
public final class BlueprintReader {
    private BlueprintReader() {}

    /** Largest file that the reader opens (compressed size). */
    static final long MAX_FILE_BYTES = 16L << 20;
    /** NBT memory quota for one file. */
    static final long NBT_QUOTA = 0x10000000L;

    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve("schematics").toAbsolutePath().normalize();
    }

    /** The {@code .nbt} and {@code .litematic} files in the schematics folder (no sub folders), sorted. */
    public static List<String> list() {
        Path dir = folder();
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(n -> n.endsWith(".nbt") || n.endsWith(".litematic"))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(256)
                .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Reads {@code name} from the schematics folder. Null on any error (logged). */
    @Nullable
    public static Blueprint readFile(String name, HolderGetter<Block> blocks) {
        Path dir = folder();
        Path file = dir.resolve(name).normalize();
        if (!file.startsWith(dir) || !file.getParent().equals(dir)) return null;
        return read(file, name, blocks);
    }

    @Nullable
    static Blueprint read(Path file, String name, HolderGetter<Block> blocks) {
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) return null;
            CompoundTag root;
            try (InputStream in = Files.newInputStream(file)) {
                root = NbtIo.readCompressed(in, NbtAccounter.create(NBT_QUOTA));
            }
            return name.endsWith(".litematic") ? litematic(root, name, blocks) : structure(root, name, blocks);
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.warn("Blueprint Builder: cannot read {}: {}", name, e.toString());
            return null;
        }
    }

    // ------------------------------------------------------------------ structure NBT
    static Blueprint structure(CompoundTag root, String name, HolderGetter<Block> blocks) {
        ListTag paletteTag = root.contains("palettes", Tag.TAG_LIST)
            ? root.getList("palettes", Tag.TAG_LIST).getList(0) : root.getList("palette", Tag.TAG_COMPOUND);
        BlockState[] palette = new BlockState[paletteTag.size()];
        for (int i = 0; i < palette.length; i++) palette[i] = NbtUtils.readBlockState(blocks, paletteTag.getCompound(i));
        ListTag size = root.getList("size", Tag.TAG_INT);
        List<Blueprint.Entry> entries = new ArrayList<>();
        ListTag list = root.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && entries.size() < Blueprint.MAX_BLOCKS; i++) {
            CompoundTag b = list.getCompound(i);
            int index = b.getInt("state");
            if (index < 0 || index >= palette.length || palette[index].isAir()) continue;
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            entries.add(new Blueprint.Entry(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), palette[index]));
        }
        return new Blueprint(name, List.copyOf(entries), new Vec3i(size.getInt(0), size.getInt(1), size.getInt(2)));
    }

    // ------------------------------------------------------------------ Litematica
    static Blueprint litematic(CompoundTag root, String name, HolderGetter<Block> blocks) {
        CompoundTag regions = root.getCompound("Regions");
        List<Blueprint.Entry> raw = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (String key : regions.getAllKeys()) {
            CompoundTag r = regions.getCompound(key);
            CompoundTag p = r.getCompound("Position"), s = r.getCompound("Size");
            int sx = s.getInt("x"), sy = s.getInt("y"), sz = s.getInt("z");
            int ax = Math.abs(sx), ay = Math.abs(sy), az = Math.abs(sz);
            if (ax == 0 || ay == 0 || az == 0) continue;
            // region minimum corner: a negative size extends from the position towards minus
            int ox = p.getInt("x") + (sx < 0 ? sx + 1 : 0);
            int oy = p.getInt("y") + (sy < 0 ? sy + 1 : 0);
            int oz = p.getInt("z") + (sz < 0 ? sz + 1 : 0);
            ListTag paletteTag = r.getList("BlockStatePalette", Tag.TAG_COMPOUND);
            BlockState[] palette = new BlockState[paletteTag.size()];
            for (int i = 0; i < palette.length; i++) palette[i] = NbtUtils.readBlockState(blocks, paletteTag.getCompound(i));
            long[] data = r.getLongArray("BlockStates");
            int bits = LitematicBits.bitsFor(palette.length);
            long volume = (long) ax * ay * az;
            if (!LitematicBits.fits(data, bits, volume)) continue;            // damaged region
            for (long i = 0; i < volume && raw.size() < Blueprint.MAX_BLOCKS; i++) {
                int v = LitematicBits.get(data, bits, i);
                if (v >= palette.length || palette[v].isAir()) continue;
                int x = (int) (i % ax), z = (int) ((i / ax) % az), y = (int) (i / ((long) ax * az));
                BlockPos at = new BlockPos(ox + x, oy + y, oz + z);
                raw.add(new Blueprint.Entry(at, palette[(int) v]));
                minX = Math.min(minX, at.getX()); minY = Math.min(minY, at.getY()); minZ = Math.min(minZ, at.getZ());
                maxX = Math.max(maxX, at.getX()); maxY = Math.max(maxY, at.getY()); maxZ = Math.max(maxZ, at.getZ());
            }
        }
        if (raw.isEmpty()) return new Blueprint(name, List.of(), Vec3i.ZERO);
        List<Blueprint.Entry> entries = new ArrayList<>(raw.size());
        for (Blueprint.Entry e : raw) entries.add(new Blueprint.Entry(e.pos().offset(-minX, -minY, -minZ), e.state()));
        return new Blueprint(name, List.copyOf(entries), new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1));
    }

    // ------------------------------------------------------------------ Create schematic item
    /** A deployed Create schematic: the blueprint and how Create places it. */
    public record CreatePlacement(Blueprint blueprint, BlockPos anchor, net.minecraft.world.level.block.Rotation rotation,
                                  net.minecraft.world.level.block.Mirror mirror) { }

    /** True when {@code stack} is a Create schematic with a file (deployed or not). */
    public static boolean isCreateSchematic(ItemStack stack) {
        return component(stack, "schematic_file") instanceof String;
    }

    /** Reads a deployed Create schematic item; null when it is not deployed or the file is missing. */
    @Nullable
    public static CreatePlacement readCreate(ItemStack stack, HolderGetter<Block> blocks) {
        if (!(component(stack, "schematic_file") instanceof String file) || !(component(stack, "schematic_owner") instanceof String owner)) return null;
        if (!Boolean.TRUE.equals(component(stack, "schematic_deployed"))) return null;
        if (!(component(stack, "schematic_anchor") instanceof BlockPos anchor)) return null;
        var rotation = component(stack, "schematic_rotation") instanceof net.minecraft.world.level.block.Rotation r ? r : net.minecraft.world.level.block.Rotation.NONE;
        var mirror = component(stack, "schematic_mirror") instanceof net.minecraft.world.level.block.Mirror m ? m : net.minecraft.world.level.block.Mirror.NONE;
        Path dir = FMLPaths.GAMEDIR.get().resolve("schematics").resolve("uploaded").toAbsolutePath().normalize();
        Path path = dir.resolve(owner).resolve(file).normalize();
        if (!path.startsWith(dir)) return null;
        Blueprint blueprint = read(path, file, blocks);
        return blueprint == null ? null : new CreatePlacement(blueprint, anchor, rotation, mirror);
    }

    @Nullable
    private static Object component(ItemStack stack, String path) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("create", path));
        return type == null ? null : stack.get(type);
    }
}
