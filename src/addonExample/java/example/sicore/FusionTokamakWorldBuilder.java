package example.sicore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 0.1.11.89: builds the fusion reactor (tokamak) sample world offline. Run the server run {@code runFusionWorldBuild} (added by
 * {@code tools/fusion-world.init.gradle}, it sets {@code -Dsi.fusion.world=true}); {@code tools/fusion_world.py prepare} makes its
 * game folder (a superflat creative world) and copies the layout ({@code docs/design/fusion_tokamak_layout.json}, written by
 * {@code tools/fusion_layout_11589.py}) into it as {@code fusion-layout.json}; {@code install} copies the result into
 * {@code run/saves}. Nothing happens without the property (the GameTest server loads this class too).
 *
 * <p>Built: the torus as laid out, a cutaway of it (the south-east quarter removed, the plasma ring shown as magenta glass; in
 * the structure it is air), and every fusion block on its own with a sign.
 */
@EventBusSubscriber(modid = CoreExampleMod.ID)
public final class FusionTokamakWorldBuilder {
    private FusionTokamakWorldBuilder() {}

    static final int GROUND = -60;   // the first air layer of the default superflat world
    // 0.1.11.93: the cutaway moved east of the full model's test supply (its generators stand 16-23 blocks east of the centre)
    static final BlockPos FULL = new BlockPos(0, GROUND, 0), CUTAWAY = new BlockPos(50, GROUND, 0), SPAWN = new BlockPos(18, GROUND, 13);
    /** Reached by reflection: addon examples do not compile against the mod's internals. */
    private static final String DEMO = "com.miophas.singularity_iteration" + ".common.fusion.FusionDemo";
    static final int VERIFY_TICKS = 3_000;
    private static ServerLevel verifyLevel;
    private static BlockPos verifyController;
    private static int verifyTick;
    private static final java.util.List<String> verifyLog = new java.util.ArrayList<>();

    private static Object demo(String method, Object... args) {
        try {
            for (var m : Class.forName(DEMO).getMethods()) if (m.getName().equals(method) && m.getParameterCount() == args.length) return m.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(method, e);
        }
        throw new IllegalStateException("no " + method);
    }

    private static BlockState block(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "fusion/block_fusion_" + name)).defaultBlockState();
    }

    private static BlockState state(String name, String facing) {
        BlockState s = block(name);
        Direction d = facing.isEmpty() ? null : Direction.byName(facing);
        return d != null && s.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? s.setValue(BlockStateProperties.HORIZONTAL_FACING, d) : s;
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

    @SubscribeEvent
    public static void start(ServerStartedEvent event) {
        if (!Boolean.getBoolean("si.fusion.world")) return;
        MinecraftServer server = event.getServer();
        try {
            ServerLevel level = server.overworld();
            var rules = level.getGameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            level.setDayTime(6000);

            JsonObject layout = JsonParser.parseString(Files.readString(Path.of("fusion-layout.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray blocks = layout.getAsJsonArray("blocks");
            int bottom = Integer.MAX_VALUE;
            for (JsonElement e : blocks) bottom = Math.min(bottom, e.getAsJsonArray().get(1).getAsInt());
            int lift = -bottom;   // the lowest block stands on the ground
            Map<String, Integer> counts = new TreeMap<>();
            BlockPos controller = null;
            for (JsonElement e : blocks) {
                JsonArray b = e.getAsJsonArray();
                int x = b.get(0).getAsInt(), y = b.get(1).getAsInt() + lift, z = b.get(2).getAsInt();
                if (b.get(3).getAsString().equals("controller")) controller = FULL.offset(x, y, z);
                BlockState s = state(b.get(3).getAsString(), b.get(4).getAsString());
                level.setBlock(FULL.offset(x, y, z), s, Block.UPDATE_ALL);
                if (!(x >= 1 && z >= 1)) level.setBlock(CUTAWAY.offset(x, y, z), s, Block.UPDATE_ALL);
                counts.merge(b.get(3).getAsString(), 1, Integer::sum);
            }
            for (JsonElement e : layout.getAsJsonArray("plasma")) {
                JsonArray p = e.getAsJsonArray();
                int x = p.get(0).getAsInt(), z = p.get(2).getAsInt();
                if (!(x >= 1 && z >= 1)) level.setBlock(CUTAWAY.offset(x, p.get(1).getAsInt() + lift, z), Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(), Block.UPDATE_ALL);
            }
            // 0.1.11.93: the full model runs: blanket, fuel, a lever on the controller, a test supply and storage for its output
            BlockPos lever = (BlockPos) demo("equip", level, controller, true);
            sign(level, FULL.offset(0, 0, 15), "托卡马克", "聚变堆", "可运行（见控制台", "旁的说明牌）");
            sign(level, CUTAWAY.offset(0, 0, 15), "剖视模型", "东南角已移除", "品红玻璃=等离子体", "（实际为空气）");

            // every block on its own, with its name
            String[][] row = {{"controller", "核聚变堆", "控制台"}, {"central_solenoid", "中心", "螺线管"}, {"toroidal_coil", "环向场", "线圈"},
                {"poloidal_coil", "极向场", "线圈"}, {"vacuum_vessel", "真空室壁", ""}, {"blanket", "包层模块", ""}, {"divertor", "偏滤器", ""},
                {"cryostat", "低温恒温器", "外壳"}, {"nbi_port", "中性束", "注入器"}, {"ecrh_port", "电子回旋", "加热器"}, {"fuel_port", "加料端口", ""},
                {"exhaust_port", "排气端口", ""}, {"cryo_port", "低温端口", ""}, {"power_port", "聚变能量", "输出端口"}, {"mitigation_port", "破裂缓解", "端口"}};
            for (int i = 0; i < row.length; i++) {
                BlockPos at = new BlockPos(3 + i * 2, GROUND, 19);
                level.setBlock(at, state(row[i][0], "south"), Block.UPDATE_ALL);
                sign(level, at.south(), row[i][1], row[i][2]);
            }
            level.setDefaultSpawnPos(SPAWN, 146.0F);   // facing the controller and its test supply
            if (Boolean.getBoolean("si.fusion.verify")) {
                // a check run in a throwaway copy: pull the lever and log the controller every second (FUSION_VERIFY.json).
                // A server without players stops ticking block entities after 300 ticks unless chunks are forced: force the
                // reactor's, its supply's and its GESU's.
                for (int cx = (controller.getX() - 28) >> 4; cx <= (controller.getX() + 14) >> 4; cx++)
                    for (int cz = (controller.getZ() - 24) >> 4; cz <= (controller.getZ() + 12) >> 4; cz++)
                        level.setChunkForced(cx, cz, true);
                demo("lever", level, lever, true);
                verifyLevel = level;
                verifyController = controller;
                verifyTick = 0;
                return;
            }

            server.saveEverything(false, true, true);
            StringBuilder report = new StringBuilder("FUSION_WORLD_BUILT");
            counts.forEach((k, v) -> report.append(' ').append(k).append('=').append(v));
            Files.writeString(Path.of("FUSION_WORLD_BUILT"), report + "\n", StandardCharsets.UTF_8);
            System.out.println(report);
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.halt(false);
    }

    /** Deuterium and tritium cells left in the controller's fuel slots, and the GESU's charge ("d/t/gesu"). */
    private static String fuelCells(net.minecraft.world.level.block.entity.BlockEntity be) {
        try {
            var items = (net.neoforged.neoforge.items.IItemHandler) be.getClass().getMethod("getItems").invoke(be);
            return items.getStackInSlot(54).getCount() + "/" + items.getStackInSlot(55).getCount() + "/" + gesuStored();
        } catch (ReflectiveOperationException e) {
            return "?";
        }
    }

    private static BlockPos verifyGesu;

    private static long gesuStored() throws ReflectiveOperationException {
        if (verifyGesu == null) {
            for (BlockPos p : BlockPos.betweenClosed(verifyController.offset(-30, -6, -26), verifyController.offset(14, 2, 12))) {
                var be = verifyLevel.getBlockEntity(p);
                if (be != null && be.getClass().getSimpleName().equals("mio_icif_gesu_core_entity")) { verifyGesu = p.immutable(); break; }
            }
            if (verifyGesu == null) return -1;
        }
        var core = verifyLevel.getBlockEntity(verifyGesu);
        return core == null ? -1 : (long) core.getClass().getMethod("getStoredEnergy").invoke(core);
    }

    @SubscribeEvent
    public static void verify(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (verifyLevel == null) return;
        verifyTick++;
        if (verifyTick % 20 == 0) {
            var be = verifyLevel.getBlockEntity(verifyController);
            try {
                long[] v = (long[]) be.getClass().getMethod("screenValues").invoke(be);
                String[] states = {"OFF", "COOLING", "CHARGING", "HEATING", "BURNING", "DISRUPTED", "QUENCHED"};
                verifyLog.add(String.format(java.util.Locale.ROOT,
                    "{\"tick\":%d,\"state\":\"%s\",\"kev\":%.2f,\"q\":%s,\"fusion\":%d,\"output\":%d,\"stored\":%d,\"buffer\":%d,\"hull\":%d,\"deuterium\":%d,\"tritium\":%d,\"status\":%d,\"heaters\":%d,\"powerPorts\":%d,\"cooled\":%d,\"charged\":%d,\"heating\":%d,\"used\":%d,\"ash\":%d,\"burn\":%d,\"fuelCells\":\"%s\"}",
                    verifyTick, states[(int) v[0]], v[1] / 100.0, v[16] < 0 ? "\"inf\"" : String.valueOf(v[16] / 100.0), v[3], v[4], v[5], v[7], v[10], v[8], v[9], v[25], v[19], v[18],
                    v[12], v[13], v[33], v[34], v[15], v[14], fuelCells(be)));
            } catch (ReflectiveOperationException e) {
                verifyLog.add("{\"error\":\"" + e + "\"}");
            }
        }
        if (verifyTick >= VERIFY_TICKS) {
            var server = verifyLevel.getServer();
            verifyLevel = null;
            try {
                Files.writeString(Path.of("FUSION_VERIFY.json"), "[\n" + String.join(",\n", verifyLog) + "\n]\n", StandardCharsets.UTF_8);
                Files.writeString(Path.of("FUSION_WORLD_BUILT"), "FUSION_WORLD_VERIFIED\n", StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
            server.halt(false);
        }
    }
}
