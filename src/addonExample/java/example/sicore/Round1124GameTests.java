package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.24: the fermenter takes heat as IC2 1.12.2 does (only when a run can start, at most 100 HU per game
 * tick over all paths; the GUI heat bar is the heat toward the run), the built-in experimental resource pack and the
 * texture style cards of the Look tab are gone, and the user's 11 tool models follow the rules of the 1.21.1 model loader.
 * The fermenter block entity is internal: reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1124GameTests {
    private Round1124GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static final String[] MODELS = {"item_electric_lighter", "item_rocket", "item_tool_advanced_electric_rifle",
        "item_tool_electric_plasma_gun", "item_tool_electric_rifle", "item_tool_laser_miner", "item_tool_plasma_air_cannon",
        "item_tool_plasma_launcher", "item_tool_rocket_launcher", "item_tool_tachyon_disruptor", "item_tool_tactical_laser_rifle"};

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Fluid fluid(String path) { return BuiltInRegistries.FLUID.get(rl(path)); }

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static String read(String path) {
        var file = ModList.get().getModFileById("mio_icif").getFile();
        try { return Files.readString(file.findResource(path.split("/"))); } catch (java.io.IOException e) { throw new AssertionError(path, e); }
    }

    private static boolean exists(String path) {
        return Files.exists(ModList.get().getModFileById("mio_icif").getFile().findResource(path.split("/")));
    }

    /** No heat without one run of biomass or without room for one run of biogas; 100 HU per tick; the gauge is the run's heat. */
    @GameTest(batch = "round1124_fermenter", template = "empty", timeoutTicks = 60)
    public static void fermenterTakesHeatOnlyWhenARunCanStart(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(rl("producer/block_fermenter_elc")));
        BlockPos abs = h.absolutePos(pos);
        BlockEntity be = h.getBlockEntity(pos);
        var heat = h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, abs, null);
        h.assertTrue(heat != null, "no heat capability");
        h.assertTrue(!heat.canReceiveHeat() && heat.receiveHeat(100, false) == 0, "an empty fermenter takes no heat");

        IFluidHandler fluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, null);
        fluids.fill(new FluidStack(fluid("biomass"), 19), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(heat.receiveHeat(100, false) == 0, "19 mB of biomass is less than one run: no heat");
        fluids.fill(new FluidStack(fluid("biomass"), 1), IFluidHandler.FluidAction.EXECUTE);
        IFluidHandler biogas = (IFluidHandler) call(be, "getBiogasTank");
        h.assertTrue(biogas.fill(new FluidStack(fluid("biogas"), 1700), IFluidHandler.FluidAction.EXECUTE) == 1700, "biogas filled");
        h.assertTrue(!heat.canReceiveHeat() && heat.receiveHeat(100, false) == 0, "no room for one run of biogas: no heat");
        biogas.drain(1700, IFluidHandler.FluidAction.EXECUTE);

        h.assertTrue(heat.canReceiveHeat(), "a run can start: the fermenter takes heat");
        h.assertTrue(heat.receiveHeat(150, true) == 100 && heat.receiveHeat(150, false) == 100, "at most 100 HU in one tick");
        h.assertTrue(heat.receiveHeat(100, false) == 0 && !heat.canReceiveHeat(), "the 100 HU count over all calls of the tick");
        h.assertTrue(((Number) call(be, "getRunHeat")).longValue() == 100 && ((Number) call(be, "getRunHeatRequired")).intValue() == 4000,
            "the heat bar shows the heat toward the run: " + call(be, "getRunHeat") + " / " + call(be, "getRunHeatRequired"));
        long start = h.getLevel().getGameTime();
        h.succeedWhen(() -> {
            h.assertTrue(h.getLevel().getGameTime() > start, "waiting for the next tick");
            h.assertTrue(heat.receiveHeat(100, true) == 100, "a new tick allows 100 HU again");
        });
    }

    /** The experimental pack, its switch class, its name and the style cards of the Look tab are gone. */
    @GameTest(batch = "round1124_assets", template = "empty")
    public static void experimentalPackAndItsSwitchAreGone(GameTestHelper h) {
        h.assertTrue(!exists("resourcepacks/si_experimental/pack.mcmeta"), "the experimental pack still ships");
        boolean switchClass;
        try {
            Class.forName(COMMON + "client.AppearanceStyle", false, Round1124GameTests.class.getClassLoader());
            switchClass = true;
        } catch (ClassNotFoundException e) {
            switchClass = false;
        }
        h.assertTrue(!switchClass, "the style switch class still exists");
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String text = read("assets/mio_icif/lang/" + lang + ".json");
            h.assertTrue(!text.contains("\"pack.mio_icif.experimental\"") && !text.contains("equipment_console.appearance.experimental")
                && !text.contains("equipment_console.appearance.texture_style"), lang + ": pack name or style cards left");
            h.assertTrue(text.contains("\"gui.mio_icif.equipment_console.appearance.page.suit\""), lang + ": the first Look page is Suit");
        }
        h.succeed();
    }

    /** Textures in item/3d with a particle texture; element rotations 0 / 22.5 / 45 degrees on one axis. */
    @GameTest(batch = "round1124_assets", template = "empty")
    public static void userToolModelsFollowTheModelRules(GameTestHelper h) {
        for (String id : MODELS) {
            JsonObject model = JsonParser.parseString(read("assets/mio_icif/models/item/" + id + ".json")).getAsJsonObject();
            JsonObject textures = model.getAsJsonObject("textures");
            h.assertTrue(textures.has("particle"), id + ": no particle texture");
            for (var entry : textures.entrySet())
                h.assertTrue(entry.getValue().getAsString().equals("mio_icif:item/3d/" + id), id + ": texture " + entry.getValue());
            h.assertTrue(exists("assets/mio_icif/textures/item/3d/" + id + ".png"), id + ": texture file missing");
            for (var element : model.getAsJsonArray("elements")) {
                JsonObject rotation = element.getAsJsonObject().getAsJsonObject("rotation");
                if (rotation == null) continue;
                h.assertTrue(rotation.has("angle") && rotation.has("axis"), id + ": rotation without angle / axis " + rotation);
                float angle = rotation.get("angle").getAsFloat();
                h.assertTrue(angle == 0F || Math.abs(angle) == 22.5F || Math.abs(angle) == 45F, id + ": rotation " + angle + " is not loaded");
            }
        }
        h.assertTrue(!exists("assets/mio_icif/textures/item/3d/item_tool_plasma_launcher.png.mcmeta"),
            "the new plasma launcher texture is one frame: no animation file");
        h.assertTrue(read("assets/mio_icif/textures/item/resource/item_quantum_core.png.mcmeta").contains("animation"),
            "the new quantum core texture is animated");
        h.succeed();
    }
}
