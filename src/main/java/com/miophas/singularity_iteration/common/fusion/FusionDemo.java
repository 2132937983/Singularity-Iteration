package com.miophas.singularity_iteration.common.fusion;

import com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_creative_generator;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.93: a fusion reactor ready to run, for the sample world and the screenshots: the tokamak as laid out, its blanket
 * (the layout that reaches the cap: proton quad rods boxed in by iridium overclocked vents), 64 deuterium and 64 tritium fuel
 * rods, 60k coolant cells for the disruption mitigation, feed 16, a lever on the controller, a test supply of {@link #BANK} creative generators
 * of {@link #BANK_RATE} EU/t each (LuV packets: the controller's voltage; together its 2,097,152 EU/t input), and a GESU
 * (core and six input modules) on the power output port. The controller faces east, as in the layout.
 */
public final class FusionDemo {
    private FusionDemo() {}

    public static final String[] BLANKET = {"VVVVVVVVV", "VpVVpVVpV", "VVVVVVVVV", "VVVVVVVVV", "VpVVpVVpV", "VVVVVVVVV"};
    public static final int BANK = 64;
    public static final long BANK_RATE = 32_768L;

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path));
    }

    /** Places the tokamak as laid out (each port in its slot), its controller at {@code controller} facing east. */
    public static void placeStructure(ServerLevel level, BlockPos controller) {
        FusionStructure s = FusionStructure.tokamak();
        for (FusionStructure.Laid b : s.laid()) {
            BlockState state = block("fusion/block_fusion_" + b.name()).defaultBlockState();
            Direction d = b.facing().isEmpty() ? null : Direction.byName(b.facing());
            if (d != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
            level.setBlock(controller.offset(b.x(), b.y(), b.z()), state, Block.UPDATE_ALL);
        }
        for (FusionStructure.Cell c : s.cells())
            if (c.part() == FusionStructure.Part.PLASMA) level.setBlock(controller.offset(c.x(), c.y(), c.z()), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** The lowest layer of the structure, relative to the controller (the ground it stands on is one below). */
    private static int bottom() {
        int min = 0;
        for (FusionStructure.Laid b : FusionStructure.tokamak().laid()) min = Math.min(min, b.y());
        return min;
    }

    /** Equips a placed tokamak for a demonstration; returns the lever (off). */
    public static BlockPos equip(ServerLevel level, BlockPos controller, boolean signs) {
        if (!(level.getBlockEntity(controller) instanceof FusionControllerBlockEntity c)) throw new IllegalStateException("no fusion controller at " + controller);
        var items = (IItemHandlerModifiable) c.getItems();
        var rod = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_proton_quad"));
        var vent = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_iridium_oc_vent"));
        for (int row = 0; row < FusionBlanket.ROWS; row++)
            for (int col = 0; col < FusionBlanket.COLUMNS; col++)
                items.setStackInSlot(row * FusionBlanket.COLUMNS + col, new ItemStack(BLANKET[row].charAt(col) == 'p' ? rod : vent));
        items.setStackInSlot(FusionControllerBlockEntity.SLOT_DEUTERIUM, new ItemStack(mio_icif_normal.DEUTERIUM_CELL.get(), 64));
        items.setStackInSlot(FusionControllerBlockEntity.SLOT_TRITIUM, new ItemStack(mio_icif_normal.TRITIUM_CELL.get(), 64));
        // 0.1.11.137: frozen neon pellets for the disruption mitigation (60k coolant cells until 0.1.11.136, ice before)
        items.setStackInSlot(FusionControllerBlockEntity.SLOT_MITIGATION, new ItemStack(mio_icif_normal.FROZEN_NEON_PELLET.get(), 16));
        c.setFeed(FusionPlasma.REFERENCE_FEED);   // 0.1.11.134: the feed goes on past it, into the limits

        // the lever on top of the controller
        BlockPos lever = controller.above();
        level.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
            .setValue(LeverBlock.FACING, Direction.EAST).setValue(LeverBlock.POWERED, false), Block.UPDATE_ALL);

        // the test supply: a superconducting line from the controller's front down to an 8 x 8 bus under the generators
        BlockState cable = block("wiring/cable/block_superconducting_cable").defaultBlockState();
        int ground = controller.getY() + bottom();
        for (int y = ground; y <= controller.getY(); y++) level.setBlock(new BlockPos(controller.getX() + 1, y, controller.getZ()), cable, Block.UPDATE_ALL);
        for (int x = controller.getX() + 2; x <= controller.getX() + 3; x++) level.setBlock(new BlockPos(x, ground, controller.getZ()), cable, Block.UPDATE_ALL);
        BlockState generator = block("generator/block_creative_generator").defaultBlockState();
        for (int i = 0; i < BANK; i++) {
            BlockPos bus = new BlockPos(controller.getX() + 4 + i % 8, ground, controller.getZ() - 4 + i / 8);
            level.setBlock(bus, cable, Block.UPDATE_ALL);
            level.setBlock(bus.above(), generator, Block.UPDATE_ALL);
            if (level.getBlockEntity(bus.above()) instanceof mio_icif_creative_generator g) g.configure(BANK_RATE, true);
        }

        // the output: a superconducting line (no voltage ceiling) from the power output port down to a GESU on the ground,
        // its core boxed in by six input modules (they take any voltage)
        for (FusionStructure.Laid b : FusionStructure.tokamak().laid()) {
            if (!b.name().equals("power_port")) continue;
            Direction front = Direction.byName(b.facing());
            BlockPos port = controller.offset(b.x(), b.y(), b.z());
            BlockPos line = port.relative(front);
            for (int y = ground; y <= port.getY(); y++) level.setBlock(line.atY(y), cable, Block.UPDATE_ALL);
            BlockPos core = line.relative(front, 2).atY(ground + 1);
            BlockState module = block("wiring/block_gesu_input_iv").defaultBlockState();
            for (Direction d : Direction.values()) level.setBlock(core.relative(d), module, Block.UPDATE_ALL);
            level.setBlock(core, block("wiring/block_gesu_core").defaultBlockState(), Block.UPDATE_ALL);
        }
        // 0.1.11.99: an energy management terminal on the output line: it reads the reactor's generation
        level.setBlock(outputTerminal(controller), block("wiring/block_energy_terminal").defaultBlockState(), Block.UPDATE_ALL);

        if (signs) {
            BlockPos at = new BlockPos(controller.getX() + 1, ground, controller.getZ() + 6);
            sign(level, at, "聚变堆演示", "拉下控制台上的", "拉杆即启动", "右键控制台看状态");
            sign(level, at.east(), "启动流程", "磁体冷却30秒", "螺线管充能", "加热→点火→燃烧");
            sign(level, at.east(2), "测试电源", "64台创造发电机", "每台32768 EU/t", "共约2.1M EU/t");
            sign(level, at.east(3), "输出", "能量输出端口→", "超导导线→GESU", "满功率41.44M EU/t");   // 0.1.11.98: uncapped (was 39.2M)
            sign(level, at.east(4), "能源管理终端", "在输出导线底部", "右键看发电读数", "");   // 0.1.11.99
        }
        c.setChanged();
        return lever;
    }

    /** 0.1.11.99: where the demonstration's energy management terminal stands: beside the bottom of the output line. */
    public static BlockPos outputTerminal(BlockPos controller) {
        for (FusionStructure.Laid b : FusionStructure.tokamak().laid()) {
            if (!b.name().equals("power_port")) continue;
            Direction front = Direction.byName(b.facing());
            BlockPos line = controller.offset(b.x(), b.y(), b.z()).relative(front);
            return line.atY(controller.getY() + bottom()).relative(front.getClockWise());
        }
        throw new IllegalStateException("the tokamak has no power output port");
    }

    /** Turns the lever on or off (as a player would), so the controller sees the signal at once. */
    public static void lever(ServerLevel level, BlockPos lever, boolean on) {
        BlockState state = level.getBlockState(lever);
        if (state.getBlock() instanceof LeverBlock l && state.getValue(LeverBlock.POWERED) != on) l.pull(state, level, lever, null);
    }

    private static void sign(ServerLevel level, BlockPos at, String... lines) {
        level.setBlock(at, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0), Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
            sign.setText(text, true);
            sign.setChanged();
        }
    }
}
