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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 0.1.11.157: builds the TESU (太级储电罐) sample world offline. Run the server run {@code runTesuWorldBuild} (added by
 * {@code tools/tesu-world.init.gradle}, it sets {@code -Dsi.tesu.world=true}); {@code tools/tesu_world.py prepare} makes its game
 * folder and copies the layout ({@code data/mio_icif/multiblock/tesu.json}, written by {@code tools/tesu_layout_111157.py}) into
 * it; {@code install} copies the result into {@code run/saves}. Nothing happens without the property (the GameTest server loads
 * this class too).
 *
 * <p>Built with the TESU blocks: a working TESU facing the spawn (input ports in the west half of the base ring, output ports in
 * the east half, a wireless output port at the back right, the controller half charged, a creative generator feeding the front
 * input port), a cutaway of it (the front-right quarter removed, showing the cells), each block on its own with a sign, and
 * (0.1.11.158) a wireless demo to the west: a relay 16 blocks from the wireless port, linked to it, and 12 blocks further two
 * electric furnaces, one with the wireless upgrade (filled through the relay) and one without.
 */
@EventBusSubscriber(modid = CoreExampleMod.ID)
public final class TesuWorldBuilder {
    private TesuWorldBuilder() {}

    static final int GROUND = -60;   // the first air layer of the default superflat world
    static final BlockPos FULL = new BlockPos(0, GROUND, 0), CUTAWAY = new BlockPos(12, GROUND, 0), SPAWN = new BlockPos(9, GROUND, 18);
    static final long CHARGE = 100_000_000_000L;            // half the capacity, so the screen shows something
    static final long SUPPLY_PACKET = 33_554_432L;           // the creative generator's packet (UIV)

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState();
    }

    /** The TESU block for a role; a port slot holds an input port in the west half (x < 3), an output port in the east half, the
     *  wireless output port at the back right (4, 6). */
    private static BlockState real(String role, int x, int z) {
        return switch (role) {
            case "casing" -> block("mio_icif:wiring/block_tesu_casing");
            case "glass" -> block("mio_icif:build/block_alloy_glass");
            case "cell" -> block("mio_icif:wiring/block_tesu_cell");
            case "controller" -> block("mio_icif:wiring/block_tesu_controller");
            case "port" -> block(x == 4 && z == 6 ? "mio_icif:wiring/block_tesu_wireless_port"
                : x < 3 ? "mio_icif:wiring/block_tesu_input" : "mio_icif:wiring/block_tesu_output");
            default -> throw new IllegalArgumentException(role);
        };
    }

    /** The local frame (front at z = 0, north) turned half round: the front faces south, towards the spawn. */
    private static BlockPos place(BlockPos origin, int x, int y, int z) { return origin.offset(6 - x, y, 6 - z); }

    private static BlockState facing(BlockState s, Direction d) {
        return s.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? s.setValue(BlockStateProperties.HORIZONTAL_FACING, d) : s;
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

    // ------------------------------------------------------------------ 0.1.11.177: the showcase data pack

    /** The processing machines with the merged screens, in a row in front of the spawn (z 16), every other block from x -12. */
    static final String[] MACHINES = {"canner_elc", "block_cutter", "centrifuge_elc", "compressor_elc", "compressor_advanced_elc",
        "extractor_elc", "furnace_elc", "induction_elc", "metal_former", "metal_former_advanced", "powder_elc", "powder_advanced_elc",
        "recycler_elc", "washer_elc"};
    static final int ROW_Z = 16, ROW_X = -12;
    static final String PACK = "si_tesu_showcase";

    private static String xyz(BlockPos p) { return p.getX() + " " + p.getY() + " " + p.getZ(); }

    /** A standing sign facing the spawn with up to four lines (JSON text components), set only where the place is free. */
    private static String signLine(BlockPos at, String... components) {
        StringBuilder messages = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) messages.append(',');
            String c = i < components.length ? components[i] : "{\"text\":\"\"}";
            messages.append('\'').append(c.replace("\\", "\\\\").replace("'", "\\'")).append('\'');
        }
        return "setblock " + xyz(at) + " minecraft:oak_sign[rotation=0]{front_text:{messages:[" + messages + "]}} keep";
    }

    private static String text(String s) { return "{\"text\":" + new com.google.gson.Gson().toJson(s) + "}"; }

    /**
     * 0.1.11.177: writes {@code showcase-pack}, a data pack for the installed TESU world ({@code tools/tesu_world.py install-pack}
     * copies it in). The world was played with many other mods, so a server without them must not load it (it would drop
     * their blocks); the pack runs once instead, in the player's own game, on the world's next load:
     * <ul>
     *   <li>the TESU and its cutaway up to the current structure (a 7 x 7 x 13 box, windows three columns wide; the world's was
     *       built as a cylinder): each place of the layout's casing, glass or cells gets the right one where it is air ({@code
     *       keep}) or holds one of the other two ({@code fill ... replace}); ports, the controller and any other block stay;</li>
     *   <li>the processing machines in a row in front of the spawn, facing it, each with a wireless upgrade (the wireless port
     *       covers the row) and, for the simple ones, something to work on, with a sign; only where the place is free.</li>
     * </ul>
     * The machines' block entity data comes from real machines set up here (this run's fresh world), so it is what the game
     * saves. A scoreboard mark keeps the pack from running twice; {@code /function si_tesu_showcase:place} runs it again.
     */
    private static void showcasePack(MinecraftServer server) {
        StringBuilder report = new StringBuilder("TESU_SHOWCASE_PACK");
        try {
            ServerLevel level = server.overworld();
            JsonObject layout = JsonParser.parseString(Files.readString(Path.of("tesu-layout.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            java.util.List<String> lines = new java.util.ArrayList<>();
            lines.add("# 0.1.11.177 (generated by example.sicore.TesuWorldBuilder): the TESU up to the current structure, then the machines");
            Map<String, String> ids = Map.of("casing", "mio_icif:wiring/block_tesu_casing", "glass", "mio_icif:build/block_alloy_glass",
                "cell", "mio_icif:wiring/block_tesu_cell");
            int structure = 0;
            for (JsonElement e : layout.getAsJsonArray("blocks")) {
                JsonArray b = e.getAsJsonArray();
                int x = b.get(0).getAsInt(), y = b.get(1).getAsInt(), z = b.get(2).getAsInt();
                String want = ids.get(b.get(3).getAsString());
                if (want == null) continue;   // ports and the controller stay as they are
                for (BlockPos origin : new BlockPos[]{FULL, CUTAWAY}) {
                    if (origin == CUTAWAY && x >= 3 && z <= 3) continue;   // the cutaway's front-right quarter stays off
                    String p = xyz(place(origin, x, y, z));
                    lines.add("setblock " + p + " " + want + " keep");
                    for (String other : ids.values()) if (!other.equals(want)) lines.add("fill " + p + " " + p + " " + want + " replace " + other);
                    structure++;
                }
            }
            var upgrade = BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:upgrade/wireless_upgrade"));
            Map<String, net.minecraft.world.item.ItemStack> inputs = Map.of(
                "powder_elc", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64),
                "powder_advanced_elc", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64),
                "compressor_elc", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SNOWBALL, 64),
                "compressor_advanced_elc", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SNOWBALL, 64),
                "furnace_elc", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 64),
                "induction_elc", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 64),
                "metal_former", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64),
                "metal_former_advanced", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64),
                "recycler_elc", new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 64));
            java.util.List<String> noWireless = new java.util.ArrayList<>();
            for (int i = 0; i < MACHINES.length; i++) {
                BlockState state = facing(block("mio_icif:producer/block_" + MACHINES[i]), Direction.SOUTH);
                BlockPos scratch = new BlockPos(2 * i, GROUND, 30);   // a real machine here, for the data the game saves
                level.setBlock(scratch, state, Block.UPDATE_ALL);
                BlockEntity be = level.getBlockEntity(scratch);
                String data = "";
                if (be != null) {
                    var items = (net.neoforged.neoforge.items.IItemHandler) Round33GameTests.call(be, "getItemHandler");
                    int slot = (Integer) Round33GameTests.call(be, "getUpgradeSlotStart");
                    if (!items.insertItem(slot, new net.minecraft.world.item.ItemStack(upgrade), false).isEmpty()) noWireless.add(MACHINES[i]);
                    var input = inputs.get(MACHINES[i]);
                    if (input != null) items.insertItem(0, input.copy(), false);
                    data = be.saveWithoutMetadata(level.registryAccess()).toString();
                }
                BlockPos at = new BlockPos(ROW_X + 2 * i, GROUND, ROW_Z);
                lines.add("setblock " + xyz(at) + " " + net.minecraft.commands.arguments.blocks.BlockStateParser.serialize(state) + data + " keep");
                lines.add(signLine(at.south(), "{\"translate\":\"" + state.getBlock().getDescriptionId() + "\"}", text("新界面"), text("无线供电")));
            }
            lines.add(signLine(new BlockPos(ROW_X - 2, GROUND, ROW_Z + 1), text("加工机器"), text("（合并布局界面）"), text("由 TESU 无线端口"), text("供电")));
            lines.add("scoreboard players set #done " + PACK + " 1");
            lines.add("tellraw @a " + text("[SI] TESU 展示：已补齐 TESU 结构，并在出生点前放好加工机器（已有方块的位置跳过）"));

            Path root = Path.of("showcase-pack");
            Path fn = root.resolve("data/" + PACK + "/function"), tag = root.resolve("data/minecraft/tags/function");
            Files.createDirectories(fn);
            Files.createDirectories(tag);
            Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":48,\"description\":\"SI TESU showcase: the current TESU structure and the processing machines (runs once)\"}}\n", StandardCharsets.UTF_8);
            Files.writeString(fn.resolve("place.mcfunction"), String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
            Files.writeString(fn.resolve("load.mcfunction"), "scoreboard objectives add " + PACK + " dummy\n"
                + "execute unless score #done " + PACK + " matches 1 run function " + PACK + ":place\n", StandardCharsets.UTF_8);
            Files.writeString(tag.resolve("load.json"), "{\"values\":[\"" + PACK + ":load\"]}\n", StandardCharsets.UTF_8);
            report.append(" structure_places=").append(structure).append(" machines=").append(MACHINES.length).append(" lines=").append(lines.size())
                .append(" no_wireless=").append(noWireless);
            Files.writeString(Path.of("TESU_SHOWCASE_PACK"), report + "\n", StandardCharsets.UTF_8);
            System.out.println(report);
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.halt(false);
    }

    @SubscribeEvent
    public static void start(ServerStartedEvent event) {
        if (Boolean.getBoolean("si.tesu.world.pack")) { showcasePack(event.getServer()); return; }   // 0.1.11.177
        if (!Boolean.getBoolean("si.tesu.world")) return;
        MinecraftServer server = event.getServer();
        try {
            ServerLevel level = server.overworld();
            var rules = level.getGameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            level.setDayTime(6000);

            JsonObject layout = JsonParser.parseString(Files.readString(Path.of("tesu-layout.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Integer> counts = new TreeMap<>();
            BlockPos controller = null;
            for (JsonElement e : layout.getAsJsonArray("blocks")) {
                JsonArray b = e.getAsJsonArray();
                int x = b.get(0).getAsInt(), y = b.get(1).getAsInt(), z = b.get(2).getAsInt();
                String role = b.get(3).getAsString();
                BlockState s = real(role, x, z);
                if (role.equals("controller")) {
                    s = facing(s, Direction.SOUTH);   // it looks out of the structure's front
                    controller = place(FULL, x, y, z);
                }
                level.setBlock(place(FULL, x, y, z), s, Block.UPDATE_ALL);
                if (!(x >= 3 && z <= 3)) level.setBlock(place(CUTAWAY, x, y, z), s, Block.UPDATE_ALL);   // front-right quarter off
                counts.merge(role, 1, Integer::sum);
            }
            // the working model: half charged, and a creative generator in front of the front input port (local 2, 1, -1)
            BlockEntity core = level.getBlockEntity(controller);
            if (core != null) Round33GameTests.call(Round33GameTests.call(core, "getEnergyStorageInternal"), "setEnergy", CHARGE);
            BlockPos supply = place(FULL, 2, 1, -1);
            level.setBlock(supply, block("mio_icif:generator/block_creative_generator"), Block.UPDATE_ALL);
            BlockEntity gen = level.getBlockEntity(supply);
            if (gen != null) Round33GameTests.call(gen, "configure", SUPPLY_PACKET, true);

            // 0.1.11.158: the wireless demo, west of the TESU: a relay 16 from the wireless port, linked to it; 12 further two furnaces
            BlockPos wireless = place(FULL, 4, 1, 6), relay = FULL.offset(-14, 0, 2);
            level.setBlock(relay, block("mio_icif:wiring/block_tesu_relay"), Block.UPDATE_ALL);
            BlockEntity relayBe = level.getBlockEntity(relay);
            if (relayBe != null) Round33GameTests.call(relayBe, "link", wireless);
            BlockPos withUpgrade = FULL.offset(-26, 0, 2), without = FULL.offset(-26, 0, 5);
            for (BlockPos f : new BlockPos[]{withUpgrade, without}) level.setBlock(f, block("mio_icif:producer/block_furnace_elc"), Block.UPDATE_ALL);
            BlockEntity furnace = level.getBlockEntity(withUpgrade);
            if (furnace != null) {
                var items = (net.neoforged.neoforge.items.IItemHandler) Round33GameTests.call(furnace, "getItemHandler");
                int slot = (Integer) Round33GameTests.call(furnace, "getUpgradeSlotStart");
                items.insertItem(slot, new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:upgrade/wireless_upgrade"))), false);
            }
            sign(level, relay.south(), "TESU 中继器", "已连到无线端口", "（16 格外）", "右键看状态");
            sign(level, withUpgrade.south(), "电炉", "装了无线升级", "由中继器充满", "");
            sign(level, without.south(), "电炉", "没装无线升级", "不充电", "");

            sign(level, FULL.offset(3, 0, 9), "TESU", "太级储电罐", "可运行：右键控制器", "看界面");
            sign(level, FULL.offset(5, 0, 9), "左半圈：输入端口", "右半圈：输出端口", "背面右：无线端口", "输出口右键换电压");
            sign(level, CUTAWAY.offset(3, 0, 9), "剖视模型", "右前四分之一", "已移除", "（不成型）");

            String[][] legend = {
                {"mio_icif:wiring/block_tesu_controller", "TESU", "控制器", ""},
                {"mio_icif:wiring/block_tesu_input", "TESU", "输入端口", ""},
                {"mio_icif:wiring/block_tesu_output", "TESU", "输出端口", "（右键设置输出）"},
                {"mio_icif:wiring/block_tesu_casing", "TESU", "外壳", ""},
                {"mio_icif:wiring/block_tesu_cell", "TESU", "储能单元", ""},
                {"mio_icif:build/block_alloy_glass", "防爆玻璃", "（观察窗）", ""},
                {"mio_icif:wiring/block_tesu_wireless_port", "TESU", "无线输出端口", ""},
                {"mio_icif:wiring/block_tesu_relay", "TESU", "中继器", ""}};
            for (int i = 0; i < legend.length; i++) {
                BlockPos at = new BlockPos(i * 3, GROUND, 13);
                level.setBlock(at, facing(block(legend[i][0]), Direction.SOUTH), Block.UPDATE_ALL);
                sign(level, at.south(), legend[i][1], legend[i][2], legend[i][3]);
            }
            level.setDefaultSpawnPos(SPAWN, 180.0F);   // facing north, the two models ahead

            server.saveEverything(false, true, true);
            StringBuilder report = new StringBuilder("TESU_WORLD_BUILT");
            counts.forEach((k, v) -> report.append(' ').append(k).append('=').append(v));
            report.append(" charge=").append(core != null ? Round33GameTests.call(Round33GameTests.call(core, "getEnergyStorageInternal"), "getAmount") : "none");
            Files.writeString(Path.of("TESU_WORLD_BUILT"), report + "\n", StandardCharsets.UTF_8);
            System.out.println(report);
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.halt(false);
    }
}
