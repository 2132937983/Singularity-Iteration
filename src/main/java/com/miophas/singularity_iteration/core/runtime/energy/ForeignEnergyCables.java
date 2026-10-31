package com.miophas.singularity_iteration.core.runtime.energy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.1.10.39: cables of other mods. SI cables do not connect to them: two networks joined by FE would pass the same energy back
 * and forth (each one serves the other as a machine), so SI neither sends FE into them, nor pulls from them, nor takes FE that
 * they push. A block counts when it is in the block tag {@code mio_icif:foreign_energy_cables} (datapacks may add more), or when
 * its id names a cable ({@code cable}, {@code duct}, {@code conduit}, {@code transmitter}, {@code wire}, {@code energy_pipe},
 * {@code universal_pipe}). GregTech and Modern Industrialization cables are SI's EU bridges (1 EU = 1 EU, no FE) and stay
 * connected.
 */
public final class ForeignEnergyCables {
    private ForeignEnergyCables() {}

    public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("mio_icif", "foreign_energy_cables"));
    private static final Pattern NAME = Pattern.compile(".*(cable|duct|conduit|transmitter|wire|energy_pipe|universal_pipe).*");
    /** Namespaces whose cables are EU bridges of SI, not FE cables. */
    private static final Set<String> EU_BRIDGES = Set.of("mio_icif", "minecraft", "gtceu", "modern_industrialization");
    private static final Map<Block, Boolean> BY_NAME = new ConcurrentHashMap<>();

    /** True when the block at {@code pos} is another mod's energy cable. */
    public static boolean is(Level level, BlockPos pos) {
        return is(level.getBlockState(pos));
    }

    public static boolean is(BlockState state) {
        if (state.is(TAG)) return true;
        return BY_NAME.computeIfAbsent(state.getBlock(), block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            return !EU_BRIDGES.contains(id.getNamespace()) && NAME.matcher(id.getPath()).matches();
        });
    }
}
