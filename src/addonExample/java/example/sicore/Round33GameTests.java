package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.7.33: quantum suit upgrade units and the modification station, the FCS server side
 * (threat sensor, deflector, chunk grid read-out), Viltrum flight, and the GuideME manual data.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round33GameTests {
    private Round33GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    static Class<?> common(String name) {
        try {
            return Class.forName(COMMON + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }

    static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)));
    }

    static ItemStack unit(String id) { return item("module/item_module_" + id); }

    static ItemStack charged(String path) {
        ItemStack stack = item(path);
        if (stack.getItem() instanceof AbstractElectricArmor armor) armor.setEnergy(stack, armor.getMaxEnergy(stack));
        return stack;
    }

    static Object unitType(String id) {
        return call(common("suit.SuitModuleType"), "byId", id);
    }

    static boolean install(ItemStack piece, String id) {
        return (boolean) call(common("suit.SuitModules"), "install", piece, unitType(id));
    }

    static boolean active(net.minecraft.world.entity.player.Player player, String id) {
        return (boolean) call(common("suit.SuitModules"), "isActive", player, unitType(id));
    }

    static List<String> installed(ItemStack piece) {
        Object modules = call(common("suit.SuitModules"), "installed", piece);
        @SuppressWarnings("unchecked") List<String> ids = (List<String>) call(modules, "ids");
        return ids;
    }


    /**
     * A real survival ServerPlayer on an embedded connection. GameTestHelper's mock server
     * player reports isCreative() = true, so it never takes damage and never loses flight.
     */
    static ServerPlayer survivalPlayer(GameTestHelper h) {
        var level = h.getLevel();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "si-test-" + (System.nanoTime() % 100000));
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)));
        try {
            // no 60-tick join protection: the test removes the player again in the same tick
            Field f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(player, 0);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("spawnInvulnerableTime", e);
        }
        return player;
    }

    static void remove(ServerPlayer player) {
        player.getServer().getPlayerList().remove(player);
    }

    private static final BlockPos STATION = new BlockPos(2, 2, 2);

    static AbstractProcessingMachineBlockEntity station(GameTestHelper h) {
        h.setBlock(STATION, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_quantum_modification_station")));
        var be = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(STATION);
        ((AbstractEnergyBlockEntity) be).getEnergyStorageInternal().setEnergy(40_000);
        return be;
    }

    static IItemHandlerModifiable slots(AbstractProcessingMachineBlockEntity be) {
        return (IItemHandlerModifiable) be.getItemHandler();
    }

    // ------------------------------------------------------------------ station

    /** The station installs a unit in 100 ticks for 64 EU/t, and the GUI button takes it out again. */
    @GameTest(batch = "round33_station", template = "reactor_loop", timeoutTicks = 200)
    public static void stationInstallsAndRemovesUnits(GameTestHelper h) {
        var be = station(h);
        ItemStack helmet = charged("armor/item_armor_quantum_helmet");
        slots(be).setStackInSlot(1, helmet);
        slots(be).setStackInSlot(2, unit("entity_esp").copyWithCount(2));
        long start = ((AbstractEnergyBlockEntity) be).getEnergyStorageInternal().getAmount();
        h.runAfterDelay(60, () -> {
            h.assertTrue(installed(slots(be).getStackInSlot(1)).isEmpty(), "the unit went in before the install time");
            h.assertTrue(String.valueOf(call(be, "status")).equals("WORKING"), "station status: " + call(be, "status"));
        });
        h.runAfterDelay(115, () -> {
            ItemStack piece = slots(be).getStackInSlot(1);
            h.assertTrue(installed(piece).contains("entity_esp"), "ESP unit not installed: " + installed(piece));
            h.assertTrue(slots(be).getStackInSlot(2).getCount() == 1, "the station must use one unit, left " + slots(be).getStackInSlot(2).getCount());
            long used = start - ((AbstractEnergyBlockEntity) be).getEnergyStorageInternal().getAmount();
            h.assertTrue(used >= 100 * 64 && used <= 100 * 64 + 64, "install cost " + used + " EU, expected 6400");
            // the second ESP unit is refused: already installed
            h.assertTrue(String.valueOf(call(be, "evaluate")).equals("ALREADY_INSTALLED"), "duplicate unit: " + call(be, "evaluate"));
            h.assertTrue(ArmorFeatures.features(piece).stream().anyMatch(f -> f.featureKey().equals("fcs_entity_esp")),
                "installed unit has no console switch");
            var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            h.assertTrue((boolean) call(be, "removeUnit", 0, player), "remove button refused");
            h.assertTrue(installed(slots(be).getStackInSlot(1)).isEmpty(), "unit still installed after removal");
            h.assertTrue(slots(be).getStackInSlot(2).getCount() == 2, "removed unit did not return to the unit slot");
            h.succeed();
        });
    }

    /** Wrong piece, full piece and missing power stop the station. */
    @GameTest(batch = "round33_station_rules", template = "reactor_loop", timeoutTicks = 40)
    public static void stationRefusesWrongPieceAndFullPiece(GameTestHelper h) {
        var be = station(h);
        slots(be).setStackInSlot(1, charged("armor/item_armor_quantum_boots"));
        slots(be).setStackInSlot(2, unit("entity_esp"));
        h.assertTrue(String.valueOf(call(be, "evaluate")).equals("WRONG_SLOT"), "ESP in boots: " + call(be, "evaluate"));
        ItemStack helmet = charged("armor/item_armor_quantum_helmet");
        for (String id : new String[]{"entity_esp", "ballistic", "behavior_predictor", "holomap"})
            h.assertTrue(install(helmet, id), id + " did not fit an empty helmet slot");
        h.assertTrue(!install(helmet, "grid_telemetry"), "a fifth unit went into the helmet (capacity 4)");
        slots(be).setStackInSlot(1, helmet);
        slots(be).setStackInSlot(2, unit("grid_telemetry"));
        h.assertTrue(String.valueOf(call(be, "evaluate")).equals("FULL"), "full helmet: " + call(be, "evaluate"));
        slots(be).setStackInSlot(1, item("armor/item_armor_nano_helmet"));
        h.assertTrue(!slots(be).isItemValid(1, item("armor/item_armor_nano_helmet")), "the station accepted a nano helmet");
        h.succeed();
    }

    // ------------------------------------------------------------------ units on the player

    /** Active units draw their EU/t from their own piece; a switched-off unit draws nothing. */
    @GameTest(batch = "round33_drain", template = "empty", timeoutTicks = 20)
    public static void unitsDrawPowerFromTheirPiece(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack helmet = charged("armor/item_armor_quantum_helmet");
        install(helmet, "entity_esp");
        install(helmet, "holomap");
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
        var armor = (AbstractElectricArmor) worn.getItem();
        long before = armor.getEnergy(worn);
        h.assertTrue(active(player, "entity_esp") && active(player, "holomap"), "installed units are not active");
        call(common("suit.SuitModules"), "drain", player);
        long used = before - armor.getEnergy(worn);
        h.assertTrue(used == (12 + 10) * 20, "drain per second " + used + " EU, expected 440");
        ArmorFeatures.setEnabled(worn, "fcs_entity_esp", false);
        h.assertTrue(!active(player, "entity_esp"), "switched-off unit still active");
        before = armor.getEnergy(worn);
        call(common("suit.SuitModules"), "drain", player);
        h.assertTrue(before - armor.getEnergy(worn) == 10 * 20, "switched-off unit still draws power");
        armor.setEnergy(worn, 0);
        h.assertTrue(!active(player, "holomap"), "unit active without power");
        h.succeed();
    }

    /** HUD units need a quantum helmet visor; the deflector does not. */
    @GameTest(batch = "round33_visor", template = "empty", timeoutTicks = 20)
    public static void hudUnitsNeedTheQuantumVisor(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack boots = charged("armor/item_armor_quantum_boots");
        install(boots, "ore_scanner");
        player.setItemSlot(EquipmentSlot.FEET, boots);
        ItemStack chest = charged("armor/item_armor_quantum_chestplate");
        install(chest, "deflector");
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        h.assertTrue(!active(player, "ore_scanner"), "ore scanner works without a visor");
        h.assertTrue(active(player, "deflector"), "deflector needs no visor");
        player.setItemSlot(EquipmentSlot.HEAD, charged("armor/item_armor_ultimate_solar_helmet"));
        h.assertTrue(active(player, "ore_scanner"), "the ultimate solar helmet (made from the quantum helmet) is a visor");
        h.succeed();
    }

    /** The deflector cancels damage the armour lets through and pays for it in EU. */
    @GameTest(batch = "round33_deflector", template = "empty", timeoutTicks = 20)
    public static void deflectorCancelsDamage(GameTestHelper h) {
        ServerPlayer plain = survivalPlayer(h);
        plain.setItemSlot(EquipmentSlot.CHEST, charged("armor/item_armor_quantum_chestplate"));
        ServerPlayer shielded = survivalPlayer(h);
        ItemStack chest = charged("armor/item_armor_quantum_chestplate");
        install(chest, "deflector");
        shielded.setItemSlot(EquipmentSlot.CHEST, chest);
        {
            plain.setHealth(20);
            shielded.setHealth(20);
            plain.invulnerableTime = 0;
            shielded.invulnerableTime = 0;
            plain.hurt(h.getLevel().damageSources().cactus(), 10);
            ItemStack worn = shielded.getItemBySlot(EquipmentSlot.CHEST);
            long before = ((AbstractElectricArmor) worn.getItem()).getEnergy(worn);
            shielded.hurt(h.getLevel().damageSources().cactus(), 10);
            float lostPlain = 20 - plain.getHealth(), lostShielded = 20 - shielded.getHealth();
            long paid = before - ((AbstractElectricArmor) worn.getItem()).getEnergy(worn);
            remove(plain);
            remove(shielded);
            h.assertTrue(lostPlain > 0.5F, "reference player took no damage (" + lostPlain + ")");
            h.assertTrue(lostShielded < 0.01F, "deflector let damage through: " + lostShielded + " (plain " + lostPlain + ")");
            h.assertTrue(paid >= 4000, "deflector paid only " + paid + " EU");
            h.succeed();
        }
    }

    /** Red when a mob targets the wearer, yellow when a neutral creature or player looks at the wearer. */
    @GameTest(batch = "round33_threat", template = "empty", timeoutTicks = 20)
    public static void threatSensorLevels(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)));
        Class<?> server = common("suit.SuitServer");
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 4.5)));
        h.getLevel().addFreshEntity(zombie);
        zombie.setTarget(player);
        h.assertTrue((int) call(server, "threatLevel", zombie, player) == 2, "targeting zombie is not red");
        zombie.setTarget(null);
        var wolf = EntityType.WOLF.create(h.getLevel());
        wolf.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, 1.5)));
        h.getLevel().addFreshEntity(wolf);
        wolf.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        h.assertTrue((int) call(server, "threatLevel", wolf, player) == 1, "watching wolf is not yellow");
        wolf.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.getEyePosition().add(0, 0, 40));
        h.assertTrue((int) call(server, "threatLevel", wolf, player) == 0, "wolf looking away still counts");
        var cow = EntityType.COW.create(h.getLevel());
        cow.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 6.5)));
        h.getLevel().addFreshEntity(cow);
        cow.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        h.assertTrue((int) call(server, "threatLevel", cow, player) == 0, "a cow is not a threat");
        zombie.discard();
        wolf.discard();
        cow.discard();
        h.succeed();
    }

    /** The chunk read-out counts the EU blocks in the chunk. */
    @GameTest(batch = "round33_grid", template = "reactor_loop", timeoutTicks = 40)
    public static void chunkGridReadOut(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 2), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/block_mfe")));
        h.setBlock(new BlockPos(3, 2, 2), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_thermal_generator")));
        h.runAfterDelay(5, () -> {
            Object grid = call(common("suit.SuitServer"), "chunkGrid", h.getLevel(), new ChunkPos(h.absolutePos(new BlockPos(2, 2, 2))));
            h.assertTrue((int) call(grid, "storages") >= 1, "MFE not counted: " + grid);
            h.assertTrue((int) call(grid, "generators") >= 1, "generator not counted: " + grid);
            h.assertTrue((long) call(grid, "capacity") > 0, "no capacity summed: " + grid);
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Viltrum flight

    /** The switch starts off; on, it grants free flight; off again, it takes it away. */
    @GameTest(batch = "round33_viltrum", template = "empty", timeoutTicks = 20)
    public static void viltrumFlightSwitchGrantsFlight(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        ItemStack chest = charged("armor/item_armor_quantum_chestplate");
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
        h.assertTrue(ArmorFeatures.features(worn).stream().anyMatch(f -> f.featureKey().equals("viltrum_flight")), "no Viltrum switch in the console");
        h.assertTrue(!ArmorFeatures.isEnabled(worn, "viltrum_flight"), "Viltrum flight must start switched off");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        h.assertTrue(!player.getAbilities().mayfly, "flight granted while the switch is off");
        ArmorFeatures.setEnabled(worn, "viltrum_flight", true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        h.assertTrue(player.getAbilities().mayfly, "switch on: no free flight (canUse=" + call(common("suit.ViltrumFlight"), "canUse", player)
            + ", creative=" + player.isCreative() + ", enabled=" + ArmorFeatures.isEnabled(player.getItemBySlot(EquipmentSlot.CHEST), "viltrum_flight") + ")");
        ArmorFeatures.setEnabled(worn, "viltrum_flight", false);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        h.assertTrue(!player.getAbilities().mayfly, "switch off: flight not removed");
        // advanced quantum chestplate has it too
        h.assertTrue(ArmorFeatures.features(item("armor/item_armor_advanced_quantum_chestplate")).stream()
            .anyMatch(f -> f.featureKey().equals("viltrum_flight")), "advanced quantum chestplate lacks Viltrum flight");
        double cruise = (double) call(common("suit.ViltrumFlight"), "costPerTick", 1.1);
        h.assertTrue(cruise > 30 && cruise < 60, "cruise cost " + cruise);
        remove(player);
        h.succeed();
    }

    // ------------------------------------------------------------------ data

    /** Station and unit recipes load; the quantum suit tag holds the eight pieces. */
    @GameTest(batch = "round33_data", template = "empty", timeoutTicks = 20)
    public static void recipesAndTags(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        List<String> missing = new ArrayList<>();
        String[] ids = {"block_quantum_modification_station", "item_module_ore_scanner", "item_module_grid_telemetry", "item_module_entity_esp",
            "item_module_ballistic", "item_module_blast_warning", "item_module_behavior_predictor", "item_module_holomap",
            "item_module_threat_sensor", "item_module_deflector"};
        for (String id : ids) {
            if (recipes.byKey(ResourceLocation.fromNamespaceAndPath("mio_icif", "suit/" + id)).isEmpty()) missing.add(id);
        }
        h.assertTrue(missing.isEmpty(), "recipes missing: " + missing);
        TagKey<Item> suit = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("mio_icif", "quantum_suit"));
        for (String p : new String[]{"armor/item_armor_quantum_helmet", "armor/item_armor_quantum_chestplate", "armor/item_armor_quantum_leggings",
                "armor/item_armor_quantum_boots", "armor/item_armor_advanced_quantum_chestplate"})
            h.assertTrue(item(p).is(suit), p + " not in mio_icif:quantum_suit");
        h.assertTrue(!item("armor/item_armor_nano_chestplate").is(suit), "nano suit must not take units");
        h.succeed();
    }

    /** A resource of the SI mod file (the addon test classes live in another module). */
    static String resource(String path) {
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
            Path p = file.findResource(path.startsWith("/") ? path.substring(1) : path);
            return Files.exists(p) ? Files.readString(p, StandardCharsets.UTF_8) : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** The GuideME manual: guide definition, an index in three languages, a page for every machine. */
    @GameTest(batch = "round33_guide", template = "empty", timeoutTicks = 20)
    public static void guideManualIsComplete(GameTestHelper h) {
        String base = "/assets/mio_icif/guides/mio_icif/manual/";
        h.assertTrue(resource("/assets/mio_icif/guideme_guides/manual.json") != null, "guide definition missing");
        for (String lang : new String[]{"", "_zh_cn/", "_ja_jp/"})
            h.assertTrue(resource(base + lang + "index.md") != null, "index missing for " + (lang.isEmpty() ? "en" : lang));
        List<String> missing = new ArrayList<>();
        int pages = 0;
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            var key = e.getKey().location();
            if (!key.getNamespace().equals("mio_icif") || !(e.getValue() instanceof EntityBlock)) continue;
            BlockEntity be;
            try {
                be = ((EntityBlock) e.getValue()).newBlockEntity(BlockPos.ZERO, e.getValue().defaultBlockState());
            } catch (RuntimeException ex) { continue; }
            if (!(be instanceof AbstractEnergyBlockEntity)) continue;
            String page = "machines/" + key.getPath().replace('/', '_') + ".md";
            for (String lang : new String[]{"", "_zh_cn/", "_ja_jp/"}) {
                if (resource(base + lang + page) == null) missing.add(lang + page);
            }
            pages++;
        }
        h.assertTrue(pages > 60, "only " + pages + " energy blocks found");
        h.assertTrue(missing.isEmpty(), "manual pages missing: " + missing);
        h.succeed();
    }

    /** Every new language key exists in en_us, zh_cn and ja_jp. */
    @GameTest(batch = "round33_lang", template = "empty", timeoutTicks = 20)
    public static void newKeysAreTranslated(GameTestHelper h) {
        List<String> missing = new ArrayList<>();
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp"}) {
            String json = resource("/assets/mio_icif/lang/" + lang + ".json");
            h.assertTrue(json != null, lang + " missing");
            for (String id : new String[]{"ore_scanner", "grid_telemetry", "entity_esp", "ballistic", "blast_warning", "behavior_predictor",
                    "holomap", "threat_sensor", "deflector"}) {
                for (String key : new String[]{"module.mio_icif." + id, "module.mio_icif." + id + ".desc", "item.mio_icif.module.item_module_" + id})
                    if (!json.contains("\"" + key + "\"")) missing.add(lang + ":" + key);
            }
            for (String key : new String[]{"block.mio_icif.producer.block_quantum_modification_station", "tooltip.mio_icif.armor.feature_viltrum_flight",
                    "key.mio_icif.fcs_hud", "screen.mio_icif.hud_layout"})
                if (!json.contains("\"" + key + "\"")) missing.add(lang + ":" + key);
        }
        h.assertTrue(missing.isEmpty(), "untranslated: " + missing);
        h.succeed();
    }

    /**
     * Writes the measured power data of every SI energy block to {@code si_specs.json} in the game
     * directory (input for tools/gen_guide.py, the manual's specification tables).
     */
    @GameTest(batch = "round33_specs", template = "empty", timeoutTicks = 20)
    public static void dumpMachineSpecs(GameTestHelper h) {
        StringBuilder out = new StringBuilder("{\n");
        int n = 0;
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            var key = e.getKey().location();
            if (!key.getNamespace().equals("mio_icif") || !(e.getValue() instanceof EntityBlock eb)) continue;
            BlockEntity be;
            try {
                be = eb.newBlockEntity(BlockPos.ZERO, e.getValue().defaultBlockState());
            } catch (RuntimeException ex) { continue; }
            if (!(be instanceof AbstractEnergyBlockEntity energy)) continue;
            var storage = energy.getEnergyStorage();
            long perTick = -1, ticks = -1;
            if (be instanceof AbstractProcessingMachineBlockEntity) {
                perTick = longField(be, "energyPerTick");
                ticks = longField(be, "baseMaxProgress");
            }
            String tier;
            try { tier = String.valueOf(call(energy.getEffectiveCableTier(), "getName")); } catch (Throwable t) { tier = ""; }
            if (n++ > 0) out.append(",\n");
            out.append(String.format(Locale.ROOT,
                "  \"%s\": {\"class\": \"%s\", \"capacity\": %d, \"maxReceive\": %d, \"maxExtract\": %d, \"sinkTier\": %d, \"sourceTier\": %d, "
                    + "\"source\": %b, \"powerOutput\": %d, \"perTick\": %d, \"ticks\": %d, \"cable\": \"%s\"}",
                key.getPath(), be.getClass().getSimpleName(), storage.getCapacity(), storage.getMaxReceive(), storage.getMaxExtract(),
                safeTier(energy, true), safeTier(energy, false), energy.isPowerSource(), energy.getPowerOutput(), perTick, ticks, tier));
        }
        out.append("\n}\n");
        try {
            Files.writeString(Path.of("si_specs.json"), out.toString());
        } catch (IOException ex) {
            throw new AssertionError("cannot write si_specs.json", ex);
        }
        h.assertTrue(n > 60, "only " + n + " energy blocks");
        h.succeed();
    }

    private static int safeTier(AbstractEnergyBlockEntity e, boolean sink) {
        try { return sink ? e.getSinkTier() : e.getSourceTier(); } catch (RuntimeException ex) { return -1; }
    }

    private static long longField(Object target, String name) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return ((Number) f.get(target)).longValue();
            } catch (NoSuchFieldException ignored) {
                // superclass
            } catch (IllegalAccessException ex) {
                return -1;
            }
        }
        return -1;
    }
}
