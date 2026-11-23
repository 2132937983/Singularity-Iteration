// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.fusion.FusionStructure;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.188: the multiblock structures of this mod (and of addons) as blueprints for the Blueprint Builder, from the same
 * build plans the JEI category "Multiblock Structures" and the manual show ({@link StructureBlueprintRegistry}). The builder
 * places them as any blueprint (turn, mirror, place in the world, hologram).
 *
 * <p>A position whose plan allows several blocks gets the first one (the one the views show), except:
 * <ul>
 *   <li>the fusion reactor (tokamak) is laid out as its layout file has it: each port in its own slot with its facing (the
 *       first block of the port slot would have made every port a power port);</li>
 *   <li>the TESU's port slots take an input port and an output port in turn (the first block would have made them all
 *       inputs).</li>
 * </ul>
 * Positions the structure needs empty (the plasma channel, the inside of a TESU or a steam tunnel) are not cleared.
 */
public final class BuilderStructures {
    private BuilderStructures() {}

    static final ResourceLocation FUSION = id("fusion_reactor"), TESU = id("tesu");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    /** The structures the builder offers, in the registry's order. */
    public static List<StructureBlueprint> all() { return List.copyOf(StructureBlueprintRegistry.all()); }

    /** The blueprint of structure {@code id}, or null when there is no such structure (or none of its blocks exists). */
    @Nullable
    public static Blueprint blueprint(ResourceLocation id) {
        StructureBlueprint plan = StructureBlueprintRegistry.get(id);
        if (plan == null) return null;
        List<Blueprint.Entry> raw = FUSION.equals(id) ? fusion() : plain(plan, TESU.equals(id));
        if (raw.isEmpty()) return null;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Blueprint.Entry e : raw) {
            minX = Math.min(minX, e.pos().getX()); minY = Math.min(minY, e.pos().getY()); minZ = Math.min(minZ, e.pos().getZ());
            maxX = Math.max(maxX, e.pos().getX()); maxY = Math.max(maxY, e.pos().getY()); maxZ = Math.max(maxZ, e.pos().getZ());
        }
        List<Blueprint.Entry> entries = new ArrayList<>(raw.size());
        for (Blueprint.Entry e : raw) entries.add(new Blueprint.Entry(e.pos().offset(-minX, -minY, -minZ), e.state()));
        return new Blueprint(plan.titleKey(), List.copyOf(entries), new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1));
    }

    /** Each position's first block; with {@code tesu} the port slots alternate an input and an output port. */
    private static List<Blueprint.Entry> plain(StructureBlueprint plan, boolean tesu) {
        List<Blueprint.Entry> out = new ArrayList<>();
        Map<StructureBlueprint.Key, Integer> seen = new HashMap<>();
        for (StructureBlueprint.Placement p : plan.placements()) {
            List<StructureBlueprint.BlockRef> blocks = p.key().blocks();
            if (blocks.isEmpty()) continue;
            int n = seen.merge(p.key(), 1, Integer::sum) - 1;
            StructureBlueprint.BlockRef ref = blocks.get(0);
            if (tesu && p.key().symbol() == 'P' && blocks.size() > 1) ref = blocks.get(n % 2);
            BlockState state = ref.resolve();
            if (state != null && !state.isAir()) out.add(new Blueprint.Entry(p.pos(), state));
        }
        return out;
    }

    /** The tokamak as its layout file lays it out (relative to the controller, which faces east). */
    private static List<Blueprint.Entry> fusion() {
        List<Blueprint.Entry> out = new ArrayList<>();
        for (FusionStructure.Laid b : FusionStructure.tokamak().laid()) {
            Block block = BuiltInRegistries.BLOCK.getOptional(id("fusion/block_fusion_" + b.name())).orElse(null);
            if (block == null) continue;
            BlockState state = block.defaultBlockState();
            Direction d = b.facing().isEmpty() ? null : Direction.byName(b.facing());
            if (d != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && d.getAxis().isHorizontal())
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
            out.add(new Blueprint.Entry(new BlockPos(b.x(), b.y(), b.z()), state));
        }
        return out;
    }
}
