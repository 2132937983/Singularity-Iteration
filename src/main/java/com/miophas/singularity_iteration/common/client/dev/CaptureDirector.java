package com.miophas.singularity_iteration.common.client.dev;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.area.AreaPreviewService;
import com.miophas.singularity_iteration.common.armory.ArmoryComponents;
import com.miophas.singularity_iteration.common.armory.ArmoryPiece;
import com.miophas.singularity_iteration.common.armory.ArmoryRegistry;
import com.miophas.singularity_iteration.common.armory.ArmoryRemoteItem;
import com.miophas.singularity_iteration.common.armory.mio_icif_armory;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_laser_tower;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Development-only screenshot director (excluded from the release jar).
 *
 * <p>Enabled with {@code -Dsi.capture=<dir>}: creates a flat creative world, builds the
 * showcase scenes on the integrated server, takes screenshots of machines, the Armory,
 * its GUIs, a summon in flight and the Area Preview, then quits.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class CaptureDirector {
    private static final String OUT = System.getProperty("si.capture");
    private static final List<Step> STEPS = new ArrayList<>();
    private static int index, wait;
    private static boolean worldRequested;
    /** GUI-relative point the virtual mouse rests on (null = bottom-left corner). */
    private static int[] hoverGui = {-4000, 4000};

    private static void hover(Minecraft mc, int gx, int gy) {
        double scale = mc.getWindow().getGuiScale();
        double px = 2, py = mc.getWindow().getScreenHeight() - 2;
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> acs && gx > -1000) {
            px = (acs.getGuiLeft() + gx) * scale;
            py = (acs.getGuiTop() + gy) * scale;
        }
        try {
            var m = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            m.setAccessible(true);
            m.invoke(mc.mouseHandler, mc.getWindow().getWindow(), px, py);
        } catch (ReflectiveOperationException ignored) { }
    }

    private record Step(int delayTicks, Runnable action, java.util.function.BooleanSupplier until) {
        Step(int delayTicks, Runnable action) { this(delayTicks, action, null); }
    }

    /** Waits (up to maxTicks) until the condition holds, then runs nothing. */
    private static void until(int maxTicks, java.util.function.BooleanSupplier condition) {
        STEPS.add(new Step(maxTicks, () -> { }, condition));
    }

    private CaptureDirector() {}

    private static void client(int delay, Runnable r) { STEPS.add(new Step(delay, r)); }

    private static void server(int delay, Consumer<ServerPlayer> r) {
        STEPS.add(new Step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            var server = mc.getSingleplayerServer();
            if (server == null || mc.player == null) return;
            var uuid = mc.player.getUUID();
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
                if (sp != null) r.accept(sp);
            });
        }));
    }

    private static void shot(int delay, String name) {
        client(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            File target = new File(OUT, "screenshots/" + name + ".png");
            target.getParentFile().mkdirs();
            Screenshot.grab(new File(OUT), name + ".png", mc.getMainRenderTarget(), c -> { });
        });
    }

    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)); }

    private static ItemStack item(String id) {
        var key = ResourceLocation.parse(id.contains(":") ? id : "mio_icif:" + id);
        return new ItemStack(BuiltInRegistries.ITEM.get(key));
    }

    private static void place(ServerLevel level, BlockPos pos, String id, Direction facing) {
        BlockState state = block(id).defaultBlockState();
        var p = state.getBlock().getStateDefinition().getProperty("facing");
        if (p instanceof DirectionProperty d && d.getPossibleValues().contains(facing)) state = state.setValue(d, facing);
        level.setBlock(pos, state, 3);
    }

    private static void look(ServerPlayer sp, double x, double y, double z, float yaw, float pitch) {
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
        sp.teleportTo(sp.serverLevel(), x, y, z, yaw, pitch);
    }

    private static final BlockPos ARMORY = new BlockPos(20, -60, 12);

    /** GUI gallery mode: -Dsi.capture.gallery=id1,id2,... opens each machine GUI and screenshots it. */
    private static void gallery(String list) {
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            sp.setGameMode(GameType.CREATIVE);
            look(sp, 0.5, -60, -3.5, 0, 0);
        });
        client(5, () -> Minecraft.getInstance().options.hideGui = false);
        // ALL: the registry is only populated once the game runs, so the shots are queued from a step
        if ("ALL".equals(list)) client(1, () -> galleryShots(allMenuBlocks()));
        else galleryShots(list.split(","));
    }

    private static void galleryShots(String[] ids) {
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i].strip();
            BlockPos at = new BlockPos(i * 3, -60, 0);
            server(2, sp -> {
                var level = sp.serverLevel();
                level.setBlock(at, block(id).defaultBlockState(), 3);
                if (level.getBlockEntity(at) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity() / 2);
                sp.teleportTo(level, at.getX() + 0.5, -60, at.getZ() - 2.5, 0, 20);
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
                level.getBlockState(at).useWithoutItem(level, sp, hit);
            });
            client(20, () -> dumpUpgradeLayout(id));
            shot(5, "gallery/" + id.replace('/', '_'));
            server(1, ServerPlayer::closeContainer);
        }
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Every SI block whose block entity opens a menu (GUI review gallery). */
    private static String[] allMenuBlocks() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            var key = e.getKey().location();
            if (!key.getNamespace().equals("mio_icif") || !(e.getValue() instanceof net.minecraft.world.level.block.EntityBlock eb)) continue;
            try {
                var be = eb.newBlockEntity(BlockPos.ZERO, e.getValue().defaultBlockState());
                if (be instanceof net.minecraft.world.MenuProvider) out.add(key.getPath());
            } catch (RuntimeException ignored) { }
        }
        java.util.Collections.sort(out);
        return out.toArray(String[]::new);
    }

    /** Dev tooling: records the upgrade-slot positions a menu asked for and the screen's GUI textures. */
    private static void dumpUpgradeLayout(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) return;
        if (!(screen.getMenu() instanceof com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu menu)) return;
        StringBuilder line = new StringBuilder(id).append('|').append(screen.getClass().getSimpleName()).append('|');
        for (int[] p : menu.legacyUpgradePositions()) line.append(p[0]).append(',').append(p[1]).append(';');
        line.append('|');
        for (Class<?> c = screen.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (var f : c.getDeclaredFields()) {
                if (f.getType() != net.minecraft.resources.ResourceLocation.class || !java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(null);
                    if (v != null && v.toString().contains("textures/gui/") && !v.toString().contains("atlas")) line.append(v).append(';');
                } catch (ReflectiveOperationException ignored) { }
            }
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(OUT, "upgrade_layout.txt"), line + "\n",
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) { }
    }

    /** Scene mode: -Dsi.capture.scene=1 shoots the mining-laser poses and the Matrix Core towers in action. */
    private static void scene() {
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            ItemStack laser = item("item_tool_laser_miner");
            if (laser.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(laser, b.getMaxEnergy(laser));
            sp.setItemInHand(InteractionHand.MAIN_HAND, laser);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, -60, 0.5, 180, 0);
        });
        client(5, () -> { Minecraft.getInstance().options.hideGui = false; Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON); });
        shot(40, "scene/laser_fp_idle");
        server(1, sp -> sp.getMainHandItem().use(sp.serverLevel(), sp, InteractionHand.MAIN_HAND));
        shot(2, "scene/laser_fp_fire");
        client(20, () -> { Minecraft.getInstance().options.hideGui = true; Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(60, "scene/laser_tp_lowready");
        server(1, sp -> sp.getMainHandItem().use(sp.serverLevel(), sp, InteractionHand.MAIN_HAND));
        shot(1, "scene/laser_tp_fire");
        shot(5, "scene/laser_tp_aim");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        server(1, sp -> sp.getMainHandItem().use(sp.serverLevel(), sp, InteractionHand.MAIN_HAND));
        shot(2, "scene/laser_tp_back_fire");
        // side view of a bolt in flight (spawned crossing the view)
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        server(5, sp -> {
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            look(sp, 0.5, -60, 0.5, 180, 0);
        });
        for (int k = 0; k < 2; k++) {
            server(10, sp -> {
                var type = com.miophas.singularity_iteration.common.item.tools.mio_icif_laser_miner.LASER_BULLET_ENTITY;
                var bolt = type.create(sp.serverLevel());
                if (bolt == null) return;
                bolt.setPos(-5, -58.4, -4);
                bolt.shoot(1, 0, 0, 1.2F, 0);
                sp.serverLevel().addFreshEntity(bolt);
            });
            shot(4, "scene/laser_side_" + k);
        }
        // ---- Matrix Core Lightning Towers vs. a pair of zombies
        server(10, sp -> {
            var level = sp.serverLevel();
            level.getServer().getWorldData().setDifficulty(Difficulty.NORMAL);
            place(level, new BlockPos(20, -60, 20), "producer/block_laser_defense_tower", Direction.NORTH);
            place(level, new BlockPos(24, -60, 20), "producer/block_sky_patrol_laser_tower", Direction.NORTH);
            for (var at : new BlockPos[]{new BlockPos(20, -60, 20), new BlockPos(24, -60, 20)})
                if (level.getBlockEntity(at) instanceof mio_icif_laser_tower tower) tower.getEnergyStorageInternal().setStored(400_000);
            for (int i = 0; i < 3; i++) {
                var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
                if (z == null) continue;
                z.moveTo(17.5 + i * 4, -60, 25.5, 180, 0);
                z.setNoAi(true);
                z.setHealth(z.getMaxHealth());
                z.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 2000, 4));
                level.addFreshEntity(z);
            }
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            look(sp, 22.5, -58.6, 15.2, 0, 18);
        });
        client(2, () -> { Minecraft.getInstance().options.hideGui = true; Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON); });
        shot(40, "scene/tower_idle");
        for (int i = 0; i < 8; i++) shot(3, String.format("scene/tower_fire_%02d", i));
        server(1, sp -> look(sp, 21.2, -59.3, 18.3, -20, 25));
        for (int i = 0; i < 6; i++) shot(3, String.format("scene/tower_close_%02d", i));
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 2 (-Dsi.capture.scene=2): scanner GUI, tuning sliders, reactor liquid cooling, new item models. */
    private static void scene2() {
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            String[] ores = {"minecraft:iron_ore", "minecraft:coal_ore", "minecraft:copper_ore", "minecraft:gold_ore", "minecraft:diamond_ore",
                "minecraft:redstone_ore", "minecraft:lapis_ore", "minecraft:emerald_ore", "minecraft:deepslate_iron_ore", "minecraft:nether_quartz_ore"};
            for (int i = 0; i < 60; i++) {
                var b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(ores[i % ores.length]));
                level.setBlock(new BlockPos(-3 + i % 7, -62, -3 + i / 7), b.defaultBlockState(), 3);
            }
            sp.teleportTo(level, 0.5, -60, 0.5, 180, 30);
            ItemStack scanner = item("item_tool_od_scanner");
            sp.setItemInHand(InteractionHand.MAIN_HAND, scanner);
        });
        client(5, () -> Minecraft.getInstance().options.hideGui = false);
        server(5, sp -> sp.getMainHandItem().use(sp.serverLevel(), sp, InteractionHand.MAIN_HAND));
        shot(30, "scene2/od_scanner_gui");
        server(1, ServerPlayer::closeContainer);
        // tachyon tuning sliders
        server(10, sp -> {
            ItemStack gun = item("item_tool_tachyon_disruptor");
            if (gun.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(gun, b.getMaxEnergy(gun));
            if (gun.getItem() instanceof com.miophas.singularity_iteration.common.item.tuning.ITunableItem t) {
                com.miophas.singularity_iteration.common.item.tuning.ITunableItem.set(gun, t.spec(gun, "output"), 150);
                com.miophas.singularity_iteration.common.item.tuning.ITunableItem.set(gun, t.spec(gun, "multi"), 2);
                com.miophas.singularity_iteration.common.item.tuning.ITunableItem.set(gun, t.spec(gun, "true_damage"), 50);
            }
            sp.setItemInHand(InteractionHand.MAIN_HAND, gun);
        });
        client(10, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features().select("mainhand")));
        shot(15, "scene2/tachyon_tuning");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        server(2, sp -> sp.setItemInHand(InteractionHand.MAIN_HAND, item("item_trinket_life_support_ring")));
        client(10, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features().select("mainhand")));
        shot(15, "scene2/ring_tuning");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        // held models: tachyon (third person) and the light generator with a visible light marker
        server(2, sp -> {
            ItemStack gun = item("item_tool_tachyon_disruptor");
            sp.setItemInHand(InteractionHand.MAIN_HAND, gun);
            sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 180, 0);
        });
        client(5, () -> { Minecraft.getInstance().options.hideGui = true; Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(30, "scene2/tachyon_held");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        shot(10, "scene2/tachyon_fp");
        server(1, sp -> {
            sp.setItemInHand(InteractionHand.MAIN_HAND, item("item_electric_lighter"));
            sp.serverLevel().setBlock(new BlockPos(0, -59, -2), block("block_electric_light").defaultBlockState(), 3);
            sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 180, 15);
        });
        shot(20, "scene2/lighter_fp");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(10, "scene2/lighter_held");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 3 (0.1.7.22): armour burst purge + segmented assembly, console Armory and Energy sections. */
    private static void scene3() {
        BlockPos armoryAt = new BlockPos(8, -60, 12), showcaseAt = new BlockPos(-4, -60, 4);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                ItemStack[][] suits = {
                    {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS),
                        new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.SHIELD)},
                    {new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.GOLDEN_CHESTPLATE), ItemStack.EMPTY, new ItemStack(Items.GOLDEN_BOOTS), ItemStack.EMPTY, ItemStack.EMPTY}};
                for (int st = 0; st < suits.length; st++)
                    for (int c = 0; c < 6; c++)
                        if (!suits[st][c].isEmpty()) inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.COLUMNS[c]), suits[st][c], false);
                armory.rename(sp, 0, "Diamond Mk.II");
                armory.rename(sp, 1, "Parade Gold");
            }
            level.setBlock(showcaseAt, block("producer/block_armor_showcase").defaultBlockState()
                .setValue(com.miophas.singularity_iteration.common.armory.ArmorShowcaseBlock.FACING, Direction.EAST), 3);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(showcaseAt), Direction.UP, showcaseAt, false);
            for (ItemStack stack : new ItemStack[]{new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE),
                    new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS)}) {
                sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
                level.getBlockState(showcaseAt).useItemOn(sp.getMainHandItem(), level, sp, InteractionHand.MAIN_HAND, hit);
            }
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ItemStack remote = new ItemStack(ArmoryRegistry.REMOTE.get());
            remote.set(ArmoryComponents.TARGET.get(), GlobalPos.of(level.dimension(), armoryAt));
            if (!com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots.write(sp, "curio:charm:0", remote)) sp.getInventory().add(remote);
            sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            sp.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            sp.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            sp.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 0);
            sp.setYBodyRot(0);
            sp.setYHeadRot(0);
        });
        // console: Armory section (Curios-worn remote, suits + showcase)
        client(20, () -> { Minecraft.getInstance().options.hideGui = false;
            Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features()
                .tab(com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features.TAB_ARMORY)); });
        shot(40, "scene3/console_armory");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        // burst purge + exchange with the showcase, then segmented assembly
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        server(5, sp -> com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.handle(sp,
            com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.SHOWCASE, 0));
        for (int f = 0; f < 45; f++) shot(1, String.format("scene3/burst/f%03d", f));
        for (int f = 0; f < 70; f++) shot(2, String.format("scene3/assembly/f%03d", f));
        shot(20, "scene3/after_exchange");
        // summon the Armory suit (netherite bursts off and flies home to the Armory)
        server(5, sp -> com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.handle(sp,
            com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.SUMMON, 0));
        for (int f = 0; f < 60; f++) shot(2, String.format("scene3/summon/f%03d", f));
        client(40, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.hideGui = false; });
        // energy statistics: electric suit with features on, a crystal belt charging it
        server(5, sp -> {
            sp.setGameMode(GameType.SURVIVAL);
            String[][] suit = {{"HEAD", "armor/item_armor_quantum_helmet"}, {"CHEST", "armor/item_armor_quantum_chestplate"},
                {"LEGS", "armor/item_armor_quantum_leggings"}, {"FEET", "armor/item_armor_nano_boots"}};
            for (String[] p : suit) {
                ItemStack st = item(p[1]);
                if (st.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(st, b.getMaxEnergy(st) * 3 / 5);
                for (var f : com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.features(st))
                    if (!f.isMode() && !com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.isEnabled(st, f.featureKey()))
                        com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.toggle(st, f.featureKey());
                sp.setItemSlot(EquipmentSlot.valueOf(p[0]), st);
            }
            ItemStack belt = item("item_trinket_energy_crystal_belt");
            if (belt.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(belt, b.getMaxEnergy(belt) / 2);
            com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots.write(sp, "curio:belt:0", belt);
            ItemStack laser = item("item_tool_laser_miner");
            if (laser.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(laser, b.getMaxEnergy(laser) / 3);
            sp.setItemInHand(InteractionHand.MAIN_HAND, laser);
            sp.setSprinting(true);
        });
        client(160, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features()
            .tab(com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features.TAB_ENERGY)));
        shot(30, "scene3/console_energy");
        client(2, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features()));
        shot(10, "scene3/console_equip");
        // refined machine models: status slits / indicator LEDs (running = glowing), by night and by day
        client(2, () -> { Minecraft.getInstance().setScreen(null); Minecraft.getInstance().options.hideGui = true; });
        server(5, sp -> {
            var level = sp.serverLevel();
            sp.setGameMode(GameType.CREATIVE);
            String[] ids = {"producer/block_compressor_elc", "producer/block_furnace_elc", "generator/block_geo_generator", "wiring/block_mfsu",
                "producer/block_replicator_elc", "generator/block_solar_generator", "producer/block_powder_elc", "kugenerator/block_wind_kinetic_generator"};
            for (int i = 0; i < ids.length; i++) {
                BlockPos at = new BlockPos(-30 + i, -60, 40);
                place(level, at, ids[i], Direction.NORTH);
                BlockState st = level.getBlockState(at);
                var lit = st.getBlock().getStateDefinition().getProperty("lit");
                if (lit instanceof net.minecraft.world.level.block.state.properties.BooleanProperty b && i % 2 == 0) {
                    level.setBlock(at, st.setValue(b, true), 3);
                    level.removeBlockEntity(at);          // display piece: keep the running look without a ticking machine
                }
            }
            level.setDayTime(18000);
            look(sp, -26.0, -59.4, 37.4, 0, 12);
        });
        shot(40, "scene3/machines_night");
        server(1, sp -> sp.serverLevel().setDayTime(6000));
        shot(20, "scene3/machines_day");
        // energy terminal on a live network: generators (nameplate vs actual), consumers, storage
        BlockPos term = new BlockPos(-30, -60, 61);
        server(5, sp -> {
            var level = sp.serverLevel();
            for (int x = -30; x <= -20; x++) place(level, new BlockPos(x, -60, 60), "wiring/cable/block_cable", Direction.NORTH);
            place(level, term, "wiring/block_energy_terminal", Direction.NORTH);
            place(level, new BlockPos(-29, -60, 59), "generator/block_geo_generator", Direction.NORTH);
            for (int x = -28; x <= -26; x++) place(level, new BlockPos(x, -60, 59), "generator/block_solar_generator", Direction.NORTH);
            place(level, new BlockPos(-25, -60, 59), "wiring/block_mfsu", Direction.NORTH);
            String[] users = {"producer/block_furnace_elc", "producer/block_furnace_elc", "producer/block_powder_elc", "producer/block_compressor_elc", "producer/block_extractor_elc"};
            for (int i = 0; i < users.length; i++) {
                BlockPos at = new BlockPos(-24 + i, -60, 59);
                place(level, at, users[i], Direction.NORTH);
                var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, at, null);
                if (handler != null) handler.insertItem(0, new ItemStack(i < 2 ? Items.CACTUS : Items.COBBLESTONE, 64), false);
            }
            level.setDayTime(6000);
            look(sp, -25.5, -59, 63.5, 180, 30);
        });
        server(240, sp -> {
            var level = sp.serverLevel();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(term), Direction.SOUTH, term, false);
            level.getBlockState(term).useWithoutItem(level, sp, hit);
        });
        client(1, () -> Minecraft.getInstance().options.hideGui = false);
        shot(60, "scene3/energy_terminal_live");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 4 (0.1.7.23): machine item icons in the inventory (no corner badge). */
    private static void scene4() {
        server(40, sp -> {
            sp.setGameMode(GameType.SURVIVAL);
            String[] ids = {"producer/block_compressor_elc", "producer/block_powder_elc", "producer/block_extractor_elc", "producer/block_furnace_elc",
                "producer/block_induction_elc", "producer/block_recycler_elc", "producer/block_canner_elc", "producer/block_centrifuge_elc",
                "producer/block_magnetizer_elc", "producer/block_metal_former", "producer/block_replicator_elc", "producer/block_scanner_elc",
                "producer/block_pump_elc", "producer/block_miner_elc", "producer/block_teleporter_elc", "producer/block_tesla",
                "generator/block_generator", "generator/block_geo_generator", "generator/block_solar_generator", "generator/block_wind_generator",
                "generator/block_water_generator", "generator/block_nuclear_reactor_generator", "wiring/block_bat_box", "wiring/block_mfe",
                "wiring/block_mfsu", "wiring/transformer_lv_mv", "wiring/block_energy_terminal", "kugenerator/block_wind_kinetic_generator",
                "hugenerator/block_solid_heat_generator", "producer/block_armory", "producer/block_laser_defense_tower", "producer/block_matter_elc",
                "producer/block_washer_elc", "producer/block_electrolyzer_elc", "generator/block_rt_generator", "reactor/block_reactor_chamber"};
            for (int i = 0; i < ids.length && i < 36; i++) sp.getInventory().setItem(i, item(ids[i]));
        });
        client(20, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(30, "scene4/inventory_icons");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 5 (0.1.7.24): status lamps (green / blinking amber / red / dark) and the redesigned machine fronts. */
    private static void scene5() {
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            // row 1: four furnaces, one per lamp state
            for (int i = 0; i < 4; i++) {
                BlockPos at = new BlockPos(i, -60, 4);
                place(level, at, "producer/block_furnace_elc", Direction.NORTH);
                var items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, at, null);
                if (items != null && (i == 0 || i == 1)) items.insertItem(0, new ItemStack(Items.CACTUS, 64), false);
                if (level.getBlockEntity(at) instanceof AbstractEnergyBlockEntity e)
                    e.getEnergyStorageInternal().setStored(i == 0 || i == 2 ? e.getEnergyStorageInternal().getCapacity() : 0);
            }
            // row 2: machine fronts
            String[] ids = {"producer/block_compressor_elc", "producer/block_powder_elc", "producer/block_extractor_elc", "producer/block_centrifuge_elc",
                "producer/block_induction_elc", "producer/block_canner_elc", "producer/block_recycler_elc", "producer/block_electrolyzer_elc",
                "producer/block_washer_elc", "producer/block_metal_former", "producer/block_block_cutter", "producer/block_magnetizer",
                "generator/block_geo_generator", "generator/block_solar_generator", "kugenerator/block_wind_kinetic_generator", "wiring/block_mfsu"};
            for (int i = 0; i < ids.length; i++) place(level, new BlockPos(-6 + i, -60, 9), ids[i], Direction.NORTH);
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            look(sp, 1.5, -59.9, 1.2, 0, 8);
        });
        for (int k = 0; k < 6; k++) server(k == 0 ? 40 : 1, sp -> {
            for (int i = 0; i < 4; i += 2) if (sp.serverLevel().getBlockEntity(new BlockPos(i, -60, 4)) instanceof AbstractEnergyBlockEntity e)
                e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity());
        });
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; });
        shot(2, "scene5/lamps_a");
        shot(8, "scene5/lamps_b");
        server(1, sp -> sp.serverLevel().setDayTime(18000));
        shot(20, "scene5/lamps_night_a");
        shot(8, "scene5/lamps_night_b");
        server(1, sp -> { sp.serverLevel().setDayTime(6000); look(sp, 1.5, -59.7, 4.3, 0, 6); });
        shot(20, "scene5/fronts_row");
        server(1, sp -> look(sp, -2.5, -59.6, 7.0, 0, 8));
        shot(15, "scene5/fronts_left");
        server(1, sp -> look(sp, 5.5, -59.6, 7.0, 0, 8));
        shot(15, "scene5/fronts_right");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 6 (0.1.7.25): inventory crash check, terminal global mode, detector GUI + HUD, physical fronts, suits, appearance tab, classic look. */
    private static void scene6() {
        BlockPos terminal = new BlockPos(2, -60, 7), suitAt = new BlockPos(30, -60, 30);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            // MV storage -> MV cable -> LV/MV transformer -> LV cable -> machines; terminal on the MV side
            place(level, new BlockPos(0, -60, 6), "wiring/block_cesu", Direction.EAST);   // 128 EU packets: MV cable
            for (int x = 1; x <= 3; x++) place(level, new BlockPos(x, -60, 6), "wiring/cable/block_cable", Direction.NORTH);
            place(level, new BlockPos(4, -60, 6), "wiring/transformer_lv_mv", Direction.WEST);
            for (int x = 5; x <= 7; x++) place(level, new BlockPos(x, -60, 6), "wiring/cable/block_tin_cable", Direction.NORTH);
            place(level, new BlockPos(8, -60, 6), "producer/block_powder_elc", Direction.NORTH);
            place(level, new BlockPos(6, -60, 7), "producer/block_furnace_elc", Direction.NORTH);
            place(level, new BlockPos(5, -60, 5), "producer/block_compressor_elc", Direction.NORTH);
            place(level, terminal, "wiring/block_energy_terminal", Direction.SOUTH);
            var mac = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, new BlockPos(8, -60, 6), null);
            if (mac != null) mac.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
            var fur = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, new BlockPos(6, -60, 7), null);
            if (fur != null) fur.insertItem(0, new ItemStack(Items.CACTUS, 64), false);
            // physical fronts: storage sockets, kinetic shafts, heat ports
            String[] fronts = {"wiring/block_bat_box", "wiring/block_cesu", "wiring/block_mfe", "wiring/block_mfsu",
                "kugenerator/block_kinetic_generator_elc", "kugenerator/block_stirling_kinetic_generator", "kugenerator/block_wind_kinetic_generator",
                "kugenerator/block_water_kinetic_generator", "hugenerator/block_solid_heat_generator", "hugenerator/block_fluid_heat_generator",
                "hugenerator/block_rt_heat_generator", "hugenerator/block_heat_generator_elc"};
            for (int i = 0; i < fronts.length; i++) place(level, new BlockPos(-4 + i, -60, 14), fronts[i], Direction.NORTH);
            sp.getInventory().setItem(0, item("item_tool_meter"));
            sp.getInventory().selected = 0;
            look(sp, 3.5, -59, 1.5, 0, 25);
        });
        for (int k = 0; k < 120; k++) server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(new BlockPos(0, -60, 6)) instanceof AbstractEnergyBlockEntity e)
                e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity());
        });
        // 1) inventory screens (crashed before: AreaPreview queried InventoryMenu#getType)
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(20, "scene6/inventory_survival");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        server(2, sp -> sp.setGameMode(GameType.CREATIVE));
        client(10, () -> { Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true)); });
        shot(20, "scene6/inventory_creative");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        // 2) energy terminal: local, then global across the transformer
        server(5, sp -> {
            var level = sp.serverLevel();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(terminal), Direction.NORTH, terminal, false);
            level.getBlockState(terminal).useWithoutItem(level, sp, hit);
        });
        shot(50, "scene6/terminal_local");
        server(1, sp -> sp.containerMenu.clickMenuButton(sp, 1));
        shot(50, "scene6/terminal_global");
        server(1, sp -> { sp.containerMenu.clickMenuButton(sp, 0); sp.closeContainer(); });
        // 3) voltage detector GUI on a bare LV cable
        server(5, sp -> {
            BlockPos cable = new BlockPos(6, -60, 6);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(cable), Direction.UP, cable, false);
            sp.getMainHandItem().onItemUseFirst(new net.minecraft.world.item.context.UseOnContext(sp, InteractionHand.MAIN_HAND, hit));
        });
        shot(80, "scene6/meter_cable");
        server(1, ServerPlayer::closeContainer);
        // 4) smart HUD: holding the detector and aiming at the MV cable
        server(5, sp -> look(sp, 2.5, -60, 5.0, 0, 37));
        shot(40, "scene6/hud_cable");
        server(1, sp -> look(sp, 4.5, -60, 5.0, 0, 37));
        shot(30, "scene6/hud_transformer");
        // 5) physical fronts
        client(1, () -> Minecraft.getInstance().options.hideGui = true);
        server(1, sp -> look(sp, 1.5, -59.4, 10.8, 0, 6));
        shot(25, "scene6/fronts_physical");
        // 6) suits: nano on the player, quantum on an armour stand
        server(1, sp -> {
            var level = sp.serverLevel();
            sp.setItemSlot(EquipmentSlot.HEAD, item("armor/item_armor_nano_helmet"));
            sp.setItemSlot(EquipmentSlot.CHEST, item("armor/item_armor_nano_chestplate"));
            sp.setItemSlot(EquipmentSlot.LEGS, item("armor/item_armor_nano_leggings"));
            sp.setItemSlot(EquipmentSlot.FEET, item("armor/item_armor_nano_boots"));
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, suitAt.getX() + 1.6, suitAt.getY(), suitAt.getZ() + 0.5);
            stand.moveTo(suitAt.getX() + 1.6, suitAt.getY(), suitAt.getZ() + 0.5, 180, 0);
            stand.setYHeadRot(180); stand.setYBodyRot(180);
            stand.setItemSlot(EquipmentSlot.HEAD, item("armor/item_armor_quantum_helmet"));
            stand.setItemSlot(EquipmentSlot.CHEST, item("armor/item_armor_quantum_chestplate"));
            stand.setItemSlot(EquipmentSlot.LEGS, item("armor/item_armor_quantum_leggings"));
            stand.setItemSlot(EquipmentSlot.FEET, item("armor/item_armor_quantum_boots"));
            try { var m = net.minecraft.world.entity.decoration.ArmorStand.class.getDeclaredMethod("setShowArms", boolean.class); m.setAccessible(true); m.invoke(stand, true); } catch (ReflectiveOperationException ignored) { }
            level.addFreshEntity(stand);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            sp.teleportTo(level, suitAt.getX() + 0.5, suitAt.getY(), suitAt.getZ() + 0.5, 180, 10);
            sp.setYBodyRot(180); sp.setYHeadRot(180);
        });
        client(5, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(40, "scene6/suits_front_day");
        server(1, sp -> sp.serverLevel().setDayTime(18000));
        shot(30, "scene6/suits_front_night");
        server(1, sp -> { sp.teleportTo(sp.serverLevel(), suitAt.getX() + 0.5, suitAt.getY(), suitAt.getZ() + 0.5, 0, 10); sp.setYBodyRot(0); sp.setYHeadRot(0); });
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot(30, "scene6/suits_back_night");
        server(1, sp -> { sp.serverLevel().setDayTime(6000); sp.teleportTo(sp.serverLevel(), suitAt.getX() + 0.5, suitAt.getY(), suitAt.getZ() + 0.5, 210, 5);
            sp.setYBodyRot(210); sp.setYHeadRot(210); });
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(30, "scene6/suits_angle_day");
        // 7) Equipment Console appearance tab
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features()
                .tab(com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features.TAB_APPEARANCE)); });
        shot(30, "scene6/console_appearance");
        // 8) classic look: switch, reload, re-shoot fronts + suits + icons, switch back
        client(2, () -> com.miophas.singularity_iteration.common.client.AppearanceStyle.setExperimental(false));
        until(1200, () -> Minecraft.getInstance().getOverlay() == null);
        shot(20, "scene6/console_appearance_classic");
        client(2, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(null); mc.options.hideGui = true; });
        server(1, sp -> { sp.getAbilities().flying = true; sp.onUpdateAbilities(); look(sp, 1.5, -59.4, 10.8, 0, 6); });
        shot(25, "scene6/classic_fronts");
        server(1, sp -> look(sp, 3.5, -59.0, 2.8, 0, 20));
        shot(20, "scene6/classic_network");
        server(1, sp -> { sp.getAbilities().flying = false; sp.onUpdateAbilities();
            sp.teleportTo(sp.serverLevel(), suitAt.getX() + 0.5, suitAt.getY(), suitAt.getZ() + 0.5, 210, 5); sp.setYBodyRot(210); sp.setYHeadRot(210); });
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(30, "scene6/classic_suits");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        server(1, sp -> { sp.setGameMode(GameType.SURVIVAL);
            String[] ids = {"producer/block_compressor_elc", "producer/block_powder_elc", "producer/block_furnace_elc", "wiring/block_mfsu",
                "wiring/block_bat_box", "kugenerator/block_wind_kinetic_generator", "hugenerator/block_solid_heat_generator", "generator/block_geo_generator", "wiring/transformer_lv_mv"};
            for (int i = 0; i < ids.length; i++) sp.getInventory().setItem(9 + i, item(ids[i])); });
        client(10, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(20, "scene6/classic_icons");
        client(2, () -> { Minecraft.getInstance().setScreen(null); com.miophas.singularity_iteration.common.client.AppearanceStyle.setExperimental(true); });
        until(1200, () -> Minecraft.getInstance().getOverlay() == null);
        client(10, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(20, "scene6/refined_icons");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 7 (0.1.7.26): items with the supplied replacement textures, classic suits restored. */
    private static void scene7() {
        String[] ids = {
            "item_electric_fishing_rod",
            "item_electric_wireless_manager",
            "item_geomagnetic_detector",
            "item_rocket",
            "item_tool_advanced_electric_rifle",
            "item_tool_cutter",
            "item_tool_diamond_driller",
            "item_tool_electric_plasma_gun",
            "item_tool_electric_rifle",
            "item_tool_hammer",
            "item_tool_iridium_driller",
            "item_tool_iron_chainsaw",
            "item_tool_iron_driller",
            "item_tool_meter",
            "item_tool_od_scanner",
            "item_tool_ov_scanner",
            "item_tool_plasma_air_cannon",
            "item_tool_power_unit",
            "item_tool_power_unit_small",
            "item_tool_rocket_launcher",
            "item_tool_tactical_laser_rifle",
            "item_tool_treetap_elc",
            "item_tool_windmeter",
            "item_tool_wooden_treetap",
            "item_tool_wrench",
            "item_tool_wrench_elc",
            "item_trinket_energy_crystal_belt",
            "item_trinket_flight_ring",
            "item_trinket_lapotron_crystal_belt",
            "item_trinket_life_support_ring",
            "item_trinket_life_support_ring/item_trinket_life_support_ring_1",
            "item_trinket_life_support_ring/item_trinket_life_support_ring_2",
            "item_trinket_life_support_ring/item_trinket_life_support_ring_3",
            "item_trinket_life_support_ring/item_trinket_life_support_ring_4",
            "reactor/item_reactor_isotope_rod",
            "resource/item_adviron_casing",
            "resource/item_adviron_denseplate",
            "resource/item_adviron_ingot",
            "resource/item_adviron_plate",
            "resource/item_ash",
            "resource/item_bronze_casing",
            "resource/item_bronze_denseplate",
            "resource/item_bronze_dust_small",
            "resource/item_bronze_plate",
            "resource/item_copper_casing",
            "resource/item_copper_denseplate",
            "resource/item_copper_nugget",
            "resource/item_copper_ore_crushed",
            "resource/item_copper_ore_crushed_purified",
            "resource/item_copper_plate",
            "resource/item_gold_ore_crushed",
            "resource/item_gold_ore_crushed_purified",
            "resource/item_golden_casing",
            "resource/item_golden_denseplate",
            "resource/item_golden_plate",
            "resource/item_ingot_bronze",
            "resource/item_ingot_tin",
            "resource/item_iron_casing",
            "resource/item_iron_denseplate",
            "resource/item_iron_ore_crushed",
            "resource/item_iron_ore_crushed_purified",
            "resource/item_iron_plate",
            "resource/item_lapi_denseplate",
            "resource/item_lapi_plate",
            "resource/item_lead_casing",
            "resource/item_lead_denseplate",
            "resource/item_lead_ingot",
            "resource/item_lead_nugget",
            "resource/item_lead_ore_crushed",
            "resource/item_lead_ore_crushed_purified",
            "resource/item_lead_plate",
            "resource/item_niobium_casing",
            "resource/item_niobium_denseplate",
            "resource/item_niobium_ingot",
            "resource/item_niobium_ore_crushed",
            "resource/item_niobium_ore_crushed_purified",
            "resource/item_niobium_plate",
            "resource/item_niobium_titanium_ingot",
            "resource/item_niobium_titanium_plate",
            "resource/item_obsidian_denseplate",
            "resource/item_obsidian_dust_small",
            "resource/item_obsidian_plate",
            "resource/item_raw_lead_ore",
            "resource/item_raw_niobium_ore",
            "resource/item_raw_silver_ore",
            "resource/item_raw_tin_ore",
            "resource/item_raw_titanium_ore",
            "resource/item_silver_casing",
            "resource/item_silver_ingot",
            "resource/item_silver_ore_crushed",
            "resource/item_silver_ore_crushed_purified",
            "resource/item_tin_casing",
            "resource/item_tin_denseplate",
            "resource/item_tin_dust_small",
            "resource/item_tin_nugget",
            "resource/item_tin_ore_crushed",
            "resource/item_tin_ore_crushed_purified",
            "resource/item_tin_plate",
            "resource/item_titanium_casing",
            "resource/item_titanium_denseplate",
            "resource/item_titanium_ingot",
            "resource/item_titanium_nugget",
            "resource/item_titanium_ore_crushed",
            "resource/item_titanium_ore_crushed_purified",
            "resource/item_titanium_plate"};
        BlockPos suitAt = new BlockPos(30, -60, 30);
        for (int page = 0; page * 36 < ids.length; page++) {
            int from = page * 36;
            server(page == 0 ? 40 : 2, sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.getInventory().clearContent();
                for (int i = 0; i < 36 && from + i < ids.length; i++) sp.getInventory().setItem(i, item(ids[from + i]));
            });
            client(10, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false;
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
            String name = "scene7/items_" + page;
            shot(20, name);
            client(2, () -> Minecraft.getInstance().setScreen(null));
        }
        server(1, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            sp.getInventory().clearContent();
            sp.setItemSlot(EquipmentSlot.HEAD, item("armor/item_armor_nano_helmet"));
            sp.setItemSlot(EquipmentSlot.CHEST, item("armor/item_armor_nano_chestplate"));
            sp.setItemSlot(EquipmentSlot.LEGS, item("armor/item_armor_nano_leggings"));
            sp.setItemSlot(EquipmentSlot.FEET, item("armor/item_armor_nano_boots"));
            var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, suitAt.getX() + 1.6, suitAt.getY(), suitAt.getZ() + 0.5);
            stand.moveTo(suitAt.getX() + 1.6, suitAt.getY(), suitAt.getZ() + 0.5, 180, 0);
            stand.setYHeadRot(180); stand.setYBodyRot(180);
            stand.setItemSlot(EquipmentSlot.HEAD, item("armor/item_armor_quantum_helmet"));
            stand.setItemSlot(EquipmentSlot.CHEST, item("armor/item_armor_quantum_chestplate"));
            stand.setItemSlot(EquipmentSlot.LEGS, item("armor/item_armor_quantum_leggings"));
            stand.setItemSlot(EquipmentSlot.FEET, item("armor/item_armor_quantum_boots"));
            level.addFreshEntity(stand);
            sp.teleportTo(level, suitAt.getX() + 0.5, suitAt.getY(), suitAt.getZ() + 0.5, 180, 10);
            sp.setYBodyRot(180); sp.setYHeadRot(180);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(40, "scene7/suits_classic");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 8 (0.1.7.27): probe between two transformers, terminal default GLOBAL, Default vs Experimental look (blocks + items). */
    private static void scene8() {
        BlockPos terminal = new BlockPos(4, -60, 7);
        String[] row = {"producer/block_compressor_elc", "producer/block_powder_elc", "producer/block_furnace_elc", "producer/block_extractor_elc",
            "generator/block_geo_generator", "generator/block_solar_generator", "wiring/block_bat_box", "wiring/block_mfsu",
            "kugenerator/block_wind_kinetic_generator", "hugenerator/block_solid_heat_generator"};
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            // CESU - MV cable - transformer - superconducting alloy cable (probe) - transformer - LV cable - machines
            place(level, new BlockPos(0, -60, 6), "wiring/block_cesu", Direction.EAST);
            place(level, new BlockPos(1, -60, 6), "wiring/cable/block_cable", Direction.NORTH);
            place(level, new BlockPos(2, -60, 6), "wiring/transformer_lv_mv", Direction.WEST);
            for (int x = 3; x <= 5; x++) place(level, new BlockPos(x, -60, 6), "wiring/cable/block_superconducting_cable", Direction.NORTH);
            place(level, new BlockPos(6, -60, 6), "wiring/transformer_lv_mv", Direction.EAST);
            place(level, new BlockPos(7, -60, 6), "wiring/cable/block_tin_cable", Direction.NORTH);
            place(level, new BlockPos(8, -60, 6), "producer/block_furnace_elc", Direction.NORTH);
            place(level, new BlockPos(7, -60, 5), "producer/block_powder_elc", Direction.NORTH);
            place(level, terminal, "wiring/block_energy_terminal", Direction.SOUTH);
            var fur = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, new BlockPos(8, -60, 6), null);
            if (fur != null) fur.insertItem(0, new ItemStack(Items.CACTUS, 64), false);
            for (int i = 0; i < row.length; i++) place(level, new BlockPos(-1 + i, -60, 12), row[i], Direction.NORTH);
            sp.getInventory().clearContent();
            sp.getInventory().setItem(0, item("item_tool_meter"));
            for (int i = 0; i < row.length; i++) sp.getInventory().setItem(9 + i, item(row[i]));
            sp.getInventory().selected = 0;
            look(sp, 4.5, -59, 3.0, 0, 25);
        });
        for (int k = 0; k < 60; k++) server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(new BlockPos(0, -60, 6)) instanceof AbstractEnergyBlockEntity e)
                e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity());
        });
        client(1, () -> Minecraft.getInstance().options.hideGui = false);
        // detector on the alloy cable between the two transformers
        server(1, sp -> {
            BlockPos cable = new BlockPos(4, -60, 6);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(cable), Direction.UP, cable, false);
            sp.getMainHandItem().onItemUseFirst(new net.minecraft.world.item.context.UseOnContext(sp, InteractionHand.MAIN_HAND, hit));
        });
        shot(80, "scene8/meter_between_transformers");
        server(1, ServerPlayer::closeContainer);
        server(5, sp -> {
            var level = sp.serverLevel();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(terminal), Direction.NORTH, terminal, false);
            level.getBlockState(terminal).useWithoutItem(level, sp, hit);
        });
        shot(50, "scene8/terminal_default_global");
        server(1, ServerPlayer::closeContainer);
        server(5, sp -> look(sp, 4.5, -60, 5.0, 0, 37));
        shot(40, "scene8/hud_alloy_cable");
        // Default look: row + inventory
        client(1, () -> Minecraft.getInstance().options.hideGui = true);
        server(1, sp -> look(sp, 3.5, -59.4, 9.0, 0, 8));
        shot(25, "scene8/default_blocks");
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(20, "scene8/default_items");
        client(2, () -> { Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features()
            .tab(com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features.TAB_APPEARANCE)); });
        shot(20, "scene8/look_tab_default");
        // Experimental look
        client(2, () -> com.miophas.singularity_iteration.common.client.AppearanceStyle.setExperimental(true));
        until(1200, () -> Minecraft.getInstance().getOverlay() == null);
        shot(20, "scene8/look_tab_experimental");
        client(2, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(null); mc.options.hideGui = true; });
        shot(25, "scene8/experimental_blocks");
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(20, "scene8/experimental_items");
        client(2, () -> { Minecraft.getInstance().setScreen(null); com.miophas.singularity_iteration.common.client.AppearanceStyle.setExperimental(false); });
        until(1200, () -> Minecraft.getInstance().getOverlay() == null);
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 9 (0.1.7.28): advanced miner filter ghost icons while it works, and wrench pick-up keeping its settings. */
    private static void scene9() {
        BlockPos miner = new BlockPos(2, -60, 4);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            place(level, miner, "producer/block_advanced_miner_elc", Direction.NORTH);
            if (level.getBlockEntity(miner) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_advanced_miner_elc m) {
                m.setFilterStack(0, new ItemStack(Items.DIRT));
                m.setFilterStack(1, new ItemStack(Items.GRASS_BLOCK));
                m.setFilterStack(4, new ItemStack(Items.DIAMOND_ORE));
                m.setWhitelistMode(false);
                m.getItemHandler().insertItem(1, item("item_tool_od_scanner"), false);
                m.getEnergyStorageInternal().setStored(m.getEnergyStorageInternal().getCapacity());
            }
            look(sp, 2.5, -60, 1.5, 0, 20);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(miner), Direction.NORTH, miner, false);
            level.getBlockState(miner).useWithoutItem(level, sp, hit);
        });
        client(5, () -> Minecraft.getInstance().options.hideGui = false);
        for (int t = 0; t < 6; t++) {
            final int n = t;
            for (int k = 0; k < 40; k++) server(1, sp -> {
                if (sp.serverLevel().getBlockEntity(miner) instanceof AbstractEnergyBlockEntity e)
                    e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity());
            });
            shot(1, "scene9/miner_gui_t" + n);
        }
        // log server vs client filter state
        client(1, () -> {
            Minecraft mc = Minecraft.getInstance();
            var clientBe = mc.level.getBlockEntity(miner);
            var sb = new StringBuilder("client:");
            if (clientBe instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_advanced_miner_elc m)
                for (int i = 0; i < 6; i++) sb.append(' ').append(m.getFilterStack(i).getItem());
            if (mc.player.containerMenu instanceof com.miophas.singularity_iteration.common.menu.producer.AdvancedMinerElcMenu menu)
                sb.append(" | menuBE same=").append(menu.getMinerBlockEntity() == clientBe);
            try { java.nio.file.Files.writeString(java.nio.file.Path.of(OUT, "scene9_state.txt"), sb + "\n",
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (java.io.IOException ignored) { }
        });
        server(1, ServerPlayer::closeContainer);
        // wrench pick-up and re-place two blocks east, then reopen the GUI
        BlockPos again = miner.east(3);
        server(5, sp -> {
            var level = sp.serverLevel();
            ItemStack wrench = item("item_tool_wrench");
            sp.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            try {
                var m = wrench.getItem().getClass().getDeclaredMethod("dismantleBlock", net.minecraft.world.level.Level.class, BlockPos.class,
                    BlockState.class, net.minecraft.world.entity.player.Player.class, ItemStack.class, InteractionHand.class);
                m.setAccessible(true);
                m.invoke(wrench.getItem(), level, miner, level.getBlockState(miner), sp, wrench, InteractionHand.MAIN_HAND);
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
            ItemStack machine = ItemStack.EMPTY;
            for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(miner).inflate(2))) {
                if (e.getItem().is(block("producer/block_advanced_miner_elc").asItem())) { machine = e.getItem().copy(); }
                e.discard();
            }
            sp.setItemInHand(InteractionHand.MAIN_HAND, machine);
            var below = again.below();
            machine.useOn(new net.minecraft.world.item.context.UseOnContext(sp, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false)));
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(again), Direction.NORTH, again, false);
            level.getBlockState(again).useWithoutItem(level, sp, hit);
        });
        shot(40, "scene9/miner_gui_after_wrench_replace");
        server(1, ServerPlayer::closeContainer);
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 10 (-Dsi.capture.scene=10): every heavy gun in first person, third person front and back. */
    private static void scene10() {
        String[] guns = {"item_tool_laser_miner", "item_tool_electric_rifle", "item_tool_advanced_electric_rifle",
            "item_tool_tactical_laser_rifle", "item_tool_tachyon_disruptor", "item_tool_electric_plasma_gun",
            "item_tool_plasma_air_cannon", "item_tool_rocket_launcher"};
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, -60, 0.5, 150, 0);
        });
        for (String id : guns) {
            server(2, sp -> {
                ItemStack gun = item(id);
                if (gun.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(gun, b.getMaxEnergy(gun));
                sp.setItemInHand(InteractionHand.MAIN_HAND, gun);
                sp.getInventory().add(item("item_rocket").copyWithCount(16));
                sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 150, 0);
            });
            // F1 (hideGui) also hides the first-person hand
            client(2, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); });
            shot(30, "scene10/" + id + "_fp");
            client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
            shot(25, "scene10/" + id + "_tp_front");
            client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
            shot(10, "scene10/" + id + "_tp_back");
            server(1, sp -> sp.getMainHandItem().use(sp.serverLevel(), sp, InteractionHand.MAIN_HAND));
            client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            shot(3, "scene10/" + id + "_tp_aim");
            server(1, sp -> sp.getInventory().clearContent());
        }
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 11 (-Dsi.capture.scene=11): canner fills tin cans with a food-component-only item, then a can is eaten. */
    private static void scene11() {
        BlockPos at = new BlockPos(0, -60, 2);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            sp.setGameMode(GameType.SURVIVAL);
            look(sp, 0.5, -60, -0.5, 0, 20);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            level.setBlock(at, block("producer/block_canner_elc").defaultBlockState(), 3);
            var be = level.getBlockEntity(at);
            if (be instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity());
            if (be instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity m
                    && m.getItemHandler() instanceof net.neoforged.neoforge.items.IItemHandlerModifiable inv) {
                inv.setStackInSlot(0, item("normal/item_tin_empty_can").copyWithCount(32));
                ItemStack modFood = new ItemStack(Items.STICK);   // stands in for another mod's food: food component only
                modFood.set(net.minecraft.core.component.DataComponents.FOOD,
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.4F).build());
                modFood.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Mod Larva (food component)"));
                inv.setStackInSlot(2, modFood.copyWithCount(3));
            }
        });
        server(200, sp -> {
            var level = sp.serverLevel();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
            level.getBlockState(at).useWithoutItem(level, sp, hit);
        });
        client(5, () -> Minecraft.getInstance().options.hideGui = false);
        shot(20, "scene11/canner_mod_food");
        server(1, ServerPlayer::closeContainer);
        server(5, sp -> {
            sp.getInventory().clearContent();
            sp.getFoodData().setFoodLevel(6);
            sp.setItemInHand(InteractionHand.MAIN_HAND, item("normal/item_tin_filled_can").copyWithCount(5));
            sp.startUsingItem(InteractionHand.MAIN_HAND);
        });
        shot(6, "scene11/eating_can");
        shot(20, "scene11/after_one_can");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Real server detonation, negotiated visual packet, hidden-window OpenGL screenshots. */
    private static void nuclearScene(boolean reactor) {
        String directory = reactor ? "reactor_nuclear" : "nuclear";
        client(1, () -> {
            var mc = Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
        });
        server(40, sp -> {
            sp.serverLevel().setDayTime(6000);
            sp.setGameMode(GameType.CREATIVE);
            look(sp, 0, 12, -96, 0, 10);
        });
        server(30, sp -> {
            var level = sp.serverLevel();
            var at = new BlockPos(0, -60, 64);
            if (reactor) {
                level.setBlock(at, block("generator/block_nuclear_reactor_generator").defaultBlockState(), 3);
                level.setBlock(at.east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                var machine = (com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator) level.getBlockEntity(at);
                for (int slot = 0; slot < 3; slot++)
                    machine.getItemHandler().insertItem(slot, item("reactor/item_reactor_uranium_quad"), false);
                machine.getHeatStorage().setHeat(9999L);
            } else {
                level.setBlock(at, block("reactor/block_reactor_nuke").defaultBlockState(), 3);
                var nuke = (com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_nuke) level.getBlockEntity(at);
                nuke.setItem(0, item("reactor/block_ic_tnt").copyWithCount(64));
                com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_nuke.triggerExplosion(level, at, level.getBlockState(at), nuke);
            }
        });
        if (reactor) server(24, sp -> {
            if (!sp.serverLevel().getBlockState(new BlockPos(0, -60, 64)).isAir())
                throw new IllegalStateException("Reactor failed to overheat in the capture scene");
        });
        shot(5, directory + "/flash");
        shot(90, directory + "/stem_and_shock");
        shot(220, directory + "/rolling_cap");
        server(5, sp -> look(sp, 35, -28, 12, 34, 29));
        shot(700, directory + "/crater");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Scene 12 (-Dsi.capture.scene=12): 0.1.7.32 features - embedded cables, wireless receiver, GESU module, futures, tool traits. */
    private static void scene12() {
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            for (int x = 0; x < 4; x++) {
                BlockPos at = new BlockPos(x, -60, 4);
                level.setBlock(at, block("wiring/cable/block_tin_cable_1").defaultBlockState(), 3);
                var st = level.getBlockState(at);
                var def = st.getBlock().getStateDefinition();
                var reinforced = (net.minecraft.world.level.block.state.properties.BooleanProperty) def.getProperty("foam_reinforced");
                var foam = (net.minecraft.world.level.block.state.properties.BooleanProperty) def.getProperty("foamlogged");
                var hard = (net.minecraft.world.level.block.state.properties.BooleanProperty) def.getProperty("foam_hardened");
                if (x >= 1) st = st.setValue(reinforced, true);
                if (x >= 2) st = st.setValue(foam, true);
                if (x >= 3) st = st.setValue(hard, true);
                level.setBlock(at, st, 3);
            }
            place(level, new BlockPos(6, -60, 4), "wiring/block_wireless_power_transmission_node", Direction.NORTH);
            place(level, new BlockPos(9, -60, 4), "wiring/block_wireless_power_transmission_node", Direction.NORTH);
            place(level, new BlockPos(10, -60, 4), "producer/block_furnace_elc", Direction.NORTH);
            if (level.getBlockEntity(new BlockPos(6, -60, 4)) instanceof com.miophas.singularity_iteration.common.blockentity.wiring.mio_icif_wireless_power_transmission_node tx) {
                tx.setTargetPosition(new BlockPos(9, -60, 4));
                tx.getEnergyStorageInternal().setStored(tx.getEnergyStorageInternal().getCapacity());
            }
            look(sp, 5.0, -59.0, 0.5, 0, 25);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        shot(60, "scene12/cables_and_wireless");
        server(5, sp -> {
            var level = sp.serverLevel();
            BlockPos core = new BlockPos(20, -58, 4);
            for (Direction d : Direction.values())
                level.setBlock(core.relative(d), block(d == Direction.UP ? "wiring/block_gesu_output_iv" : "wiring/block_gesu_input_iv").defaultBlockState(), 3);
            level.setBlock(core, block("wiring/block_gesu_core").defaultBlockState(), 3);
            look(sp, 20.5, -60, 1.0, 0, 0);
        });
        server(30, sp -> {
            var level = sp.serverLevel();
            BlockPos module = new BlockPos(20, -58, 3);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(module), Direction.NORTH, module, false);
            level.getBlockState(module).useWithoutItem(level, sp, hit);
        });
        client(5, () -> Minecraft.getInstance().options.hideGui = false);
        shot(20, "scene12/gesu_module_opens_core_gui");
        server(1, ServerPlayer::closeContainer);
        server(5, sp -> {
            var level = sp.serverLevel();
            BlockPos at = new BlockPos(30, -60, 4);
            place(level, at, "producer/block_future_elc", Direction.NORTH);
            if (level.getBlockEntity(at) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity());
            look(sp, 30.5, -60, 1.5, 0, 20);
        });
        server(10, sp -> {
            var level = sp.serverLevel();
            BlockPos at = new BlockPos(30, -60, 4);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
            level.getBlockState(at).useWithoutItem(level, sp, hit);
        });
        shot(30, "scene12/futures_datapack");
        server(1, ServerPlayer::closeContainer);
        server(5, sp -> sp.setItemInHand(InteractionHand.MAIN_HAND, item("item_tool_nanosaber")));
        client(10, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features()));
        client(5, () -> { var sc = Minecraft.getInstance().screen; if (sc != null) for (int i = 0; i < 4; i++) sc.keyPressed(264, 0, 0); });
        shot(20, "scene12/console_tool_traits");
        client(10, () -> Minecraft.getInstance().stop());
    }


    // ------------------------------------------------------------------ scene 13 (0.1.7.33)

    private static ItemStack suitPiece(String path, String... units) {
        ItemStack stack = item(path);
        if (stack.getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor a) a.setEnergy(stack, a.getMaxEnergy(stack));
        for (String u : units) com.miophas.singularity_iteration.common.suit.SuitModules.install(stack,
            com.miophas.singularity_iteration.common.suit.SuitModuleType.byId(u));
        return stack;
    }

    private static void suitUp(ServerPlayer sp) {
        sp.setItemSlot(EquipmentSlot.HEAD, suitPiece("armor/item_armor_quantum_helmet", "entity_esp", "ballistic", "behavior_predictor", "holomap"));
        sp.setItemSlot(EquipmentSlot.CHEST, suitPiece("armor/item_armor_quantum_chestplate", "threat_sensor", "blast_warning", "deflector"));
        sp.setItemSlot(EquipmentSlot.LEGS, suitPiece("armor/item_armor_quantum_leggings", "grid_telemetry"));
        sp.setItemSlot(EquipmentSlot.FEET, suitPiece("armor/item_armor_quantum_boots", "ore_scanner"));
    }

    private static <T extends net.minecraft.world.entity.Mob> T mob(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, double x, double y, double z,
                                                                  boolean noAi, ServerPlayer face) {
        T m = type.create(level);
        m.moveTo(x, y, z, 0, 0);
        m.setNoAi(noAi);
        m.setPersistenceRequired();
        level.addFreshEntity(m);
        if (face != null) m.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, face.getEyePosition());
        if (face != null) { m.setYBodyRot(m.getYRot()); m.setYHeadRot(m.getYRot()); }
        return m;
    }

    /** Scene 13 (-Dsi.capture.scene=13): FCS HUD, deflector field, Viltrum flight, modification station, layout editor. */
    private static void scene13() {
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            sp.setGameMode(GameType.SURVIVAL);
            // ores under the turf, a small grid, mobs
            int[][] ores = {{2, -62, 6}, {-3, -63, 8}, {4, -63, 10}, {-1, -62, 12}, {6, -62, 4}, {-5, -62, 5}, {1, -63, 3}, {3, -62, 14}};
            String[] kinds = {"minecraft:diamond_ore", "minecraft:gold_ore", "minecraft:iron_ore", "minecraft:redstone_ore", "minecraft:lapis_ore",
                "minecraft:emerald_ore", "minecraft:copper_ore", "minecraft:coal_ore"};
            for (int i = 0; i < ores.length; i++)
                level.setBlock(new BlockPos(ores[i][0], ores[i][1], ores[i][2]),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse(kinds[i])).defaultBlockState(), 3);
            place(level, new BlockPos(-4, -60, 9), "generator/block_geo_generator", Direction.EAST);
            place(level, new BlockPos(-3, -60, 9), "wiring/block_mfe", Direction.EAST);
            place(level, new BlockPos(-2, -60, 9), "producer/block_powder_elc", Direction.SOUTH);
            if (level.getBlockEntity(new BlockPos(-3, -60, 9)) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity() / 2);
            look(sp, 0.5, -60, 0.5, 0, 6);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            suitUp(sp);
            var crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
            sp.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
            var husk = mob(level, net.minecraft.world.entity.EntityType.HUSK, 3.5, -60, 9.5, true, sp);
            husk.setTarget(sp);
            var husk2 = mob(level, net.minecraft.world.entity.EntityType.HUSK, -1.5, -60, 15.5, true, sp);
            husk2.setTarget(sp);
            mob(level, net.minecraft.world.entity.EntityType.WOLF, 5.5, -60, 6.5, true, sp);
            mob(level, net.minecraft.world.entity.EntityType.COW, -5.5, -60, 12.5, true, null);
            var pig = mob(level, net.minecraft.world.entity.EntityType.PIG, 1.5, -60, 7.5, false, null);
            pig.setDeltaMovement(0.12, 0, 0);
            var tnt = new net.minecraft.world.entity.item.PrimedTnt(level, 6.5, -60, 12.5, null);
            tnt.setFuse(500);
            level.addFreshEntity(tnt);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        shot(90, "scene13/fcs_hud_overview");
        // telemetry: look at the macerator
        server(5, sp -> sp.teleportTo(sp.serverLevel(), -2.5, -60, 5.0, 0, 14));
        client(45, () -> Singularity_Iteration.LOGGER.warn("[scene13] ores={} first={} telemetryPos={} reply={} hit={}",
            com.miophas.singularity_iteration.common.client.suit.OreScanner.hits().size(),
            com.miophas.singularity_iteration.common.client.suit.OreScanner.hits().isEmpty() ? null : com.miophas.singularity_iteration.common.client.suit.OreScanner.hits().get(0).pos(),
            com.miophas.singularity_iteration.common.client.suit.FcsClient.telemetryPos(),
            com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket.latest(), Minecraft.getInstance().hitResult));
        shot(5, "scene13/fcs_grid_telemetry");
        // deflector: hits from three sides, third person front
        server(5, sp -> {
            sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 180, 0);
            sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class, sp.getBoundingBox().inflate(40)).forEach(e -> e.discard());
        });
        client(5, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        for (int i = 0; i < 3; i++) {
            final int k = i;
            server(i == 0 ? 20 : 3, sp -> {
                var level = sp.serverLevel();
                var src = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, sp.getBoundingBox().inflate(30)).stream().findFirst().orElse(null);
                if (src == null) return;
                double[][] at = {{-1.2, 1.1, -1.4}, {1.4, 1.5, -0.6}, {0.2, 0.6, -1.6}};
                src.moveTo(sp.getX() + at[k][0], sp.getY() + at[k][1] - 1, sp.getZ() + at[k][2]);
                sp.invulnerableTime = 0;
                sp.hurt(level.damageSources().mobAttack(src), 6);
            });
        }
        client(1, () -> Singularity_Iteration.LOGGER.warn("[scene13] shield hits on client: {}",
            com.miophas.singularity_iteration.common.suit.SuitSensorData.shieldHits().size()));
        shot(3, "scene13/deflector_field");
        // Viltrum flight
        server(30, sp -> {
            var chest = sp.getItemBySlot(EquipmentSlot.CHEST);
            com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.setEnabled(chest, "viltrum_flight", true);
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            sp.teleportTo(sp.serverLevel(), 0.5, -40, -60.5, 0, -4);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
        });
        client(5, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
        });
        client(20, () -> { var pl = Minecraft.getInstance().player;
            Singularity_Iteration.LOGGER.warn("[scene13] flight: pos={} abilitiesFlying={} viltrum={} speed={}", pl.position(), pl.getAbilities().flying,
                com.miophas.singularity_iteration.common.client.suit.ViltrumFlightClient.isFlying(),
                com.miophas.singularity_iteration.common.client.suit.ViltrumFlightClient.speed()); });
        shot(2, "scene13/viltrum_flight_back");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        shot(6, "scene13/viltrum_flight_hud");
        client(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        // station
        server(20, sp -> {
            var level = sp.serverLevel();
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            BlockPos at = new BlockPos(30, -60, 4);
            place(level, at, "producer/block_quantum_modification_station", Direction.NORTH);
            if (level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.suit.QuantumModStationBlockEntity st) {
                st.getEnergyStorageInternal().setStored(30_000);
                var h = (net.neoforged.neoforge.items.IItemHandlerModifiable) st.getItemHandler();
                h.setStackInSlot(1, suitPiece("armor/item_armor_quantum_helmet", "entity_esp", "holomap"));
                h.setStackInSlot(2, new ItemStack(com.miophas.singularity_iteration.common.suit.SuitRegistry.unitItem(
                    com.miophas.singularity_iteration.common.suit.SuitModuleType.BALLISTIC), 2));
            }
            sp.teleportTo(level, 30.5, -60, 1.6, 0, 28);
        });
        client(3, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.hideGui = true; });
        shot(40, "scene13/station_block");
        server(2, sp -> {
            var level = sp.serverLevel();
            BlockPos at = new BlockPos(30, -60, 4);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
            level.getBlockState(at).useWithoutItem(level, sp, hit);
        });
        client(5, () -> Minecraft.getInstance().options.hideGui = false);
        shot(25, "scene13/station_gui");
        server(1, ServerPlayer::closeContainer);
        client(5, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.suit.HudLayoutScreen(null)));
        shot(15, "scene13/hud_layout_editor");
        client(2, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features().select("chest")));
        shot(15, "scene13/console_viltrum_units");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "13".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene13();
        } else if (OUT != null && "12".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene12();
        } else if (OUT != null && ("nuclear".equals(System.getProperty("si.capture.scene"))
                || "reactor-nuclear".equals(System.getProperty("si.capture.scene")))) {
            new File(OUT).mkdirs();
            nuclearScene("reactor-nuclear".equals(System.getProperty("si.capture.scene")));
        } else if (OUT != null && "11".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene11();
        } else if (OUT != null && "10".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene10();
        } else if (OUT != null && "9".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene9();
        } else if (OUT != null && "8".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene8();
        } else if (OUT != null && "7".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene7();
        } else if (OUT != null && "6".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene6();
        } else if (OUT != null && "5".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene5();
        } else if (OUT != null && "4".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene4();
        } else if (OUT != null && "3".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene3();
        } else if (OUT != null && "2".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene2();
        } else if (OUT != null && System.getProperty("si.capture.scene") != null) {
            new File(OUT).mkdirs();
            scene();
        } else {
        String galleryList = System.getProperty("si.capture.gallery");
        if (OUT != null && galleryList != null) {
            new File(OUT).mkdirs();
            gallery(galleryList);
        } else if (OUT != null) {
            new File(OUT).mkdirs();
            // ---- world setup
            server(40, sp -> {
                var level = sp.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
                sp.setGameMode(GameType.CREATIVE);
                String[] row = {"producer/block_compressor_elc", "producer/block_powder_elc", "producer/block_extractor_elc",
                    "producer/block_furnace_elc", "producer/block_canner_elc", "producer/block_tesla", "producer/block_armory",
                    "wiring/transformer_ev_sc", "producer/block_miner_elc", "producer/block_recycler_elc"};
                for (int i = 0; i < row.length; i++) place(level, new BlockPos(i, -60, 2), row[i], Direction.NORTH);
                String[] row2 = {"generator/block_geo_generator", "generator/block_thermal_generator", "generator/block_nuclear_reactor_generator",
                    "wiring/block_mfsu", "producer/block_teleporter_elc", "producer/block_tesla", "producer/block_pump_elc",
                    "generator/block_solar_generator", "wiring/transformer_iv_luv", "producer/block_electrolyzer_elc"};
                for (int i = 0; i < row2.length; i++) place(level, new BlockPos(i, -60, 3), row2[i], Direction.NORTH);
                place(level, new BlockPos(5, -59, 2), "producer/block_tesla", Direction.NORTH);
                look(sp, 4.5, -58.3, -2.6, -8, 22);
            });
            client(5, () -> { Minecraft.getInstance().options.hideGui = true; Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON); });
            shot(60, "01_machines_row");
            server(1, sp -> look(sp, 5.6, -59.0, 0.2, 20, 18));
            shot(30, "02_tesla_armory_closeup");

            // ---- armory scene
            server(1, sp -> {
                var level = sp.serverLevel();
                place(level, ARMORY, "producer/block_armory", Direction.NORTH);
                place(level, ARMORY.east(), "producer/block_tesla", Direction.NORTH);
                place(level, ARMORY.west(), "wiring/block_mfsu", Direction.EAST);
                if (level.getBlockEntity(ARMORY) instanceof mio_icif_armory armory) {
                    armory.getEnergyStorageInternal().setStored(850_000);
                    armory.tryBind(sp);
                    var inv = armory.getItemHandler();
                    ItemStack[][] suits = {
                        {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS),
                            new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.SHIELD)},
                        {new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
                            new ItemStack(Items.IRON_BOOTS), new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY},
                        {new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE), ItemStack.EMPTY,
                            ItemStack.EMPTY, new ItemStack(Items.BOW), ItemStack.EMPTY},
                        {new ItemStack(Items.GOLDEN_HELMET), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.GOLDEN_BOOTS), ItemStack.EMPTY, ItemStack.EMPTY}};
                    for (int s = 0; s < suits.length; s++)
                        for (int c = 0; c < 6; c++)
                            if (!suits[s][c].isEmpty()) inv.insertItem(mio_icif_armory.slotOf(s, ArmoryPiece.COLUMNS[c]), suits[s][c], false);
                    armory.rename(sp, 0, "Diamond Mk.II");
                    armory.rename(sp, 1, "Field Kit");
                    armory.rename(sp, 2, "Netherite Mk.V");
                }
                sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                sp.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                sp.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                sp.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
                look(sp, ARMORY.getX() + 0.5, -60, ARMORY.getZ() - 3.5, 0, 25);
            });
            shot(40, "03_armory_block");
            server(1, sp -> {
                // worn-out gear goes back in: maintenance starts
                if (sp.serverLevel().getBlockEntity(ARMORY) instanceof mio_icif_armory armory) {
                    var inv = armory.getItemHandler();
                    ItemStack chest = inv.extractItem(mio_icif_armory.slotOf(1, ArmoryPiece.CHEST), 1, false);
                    chest.setDamageValue(150);
                    inv.insertItem(mio_icif_armory.slotOf(1, ArmoryPiece.CHEST), chest, false);
                    ItemStack helm = inv.extractItem(mio_icif_armory.slotOf(2, ArmoryPiece.HEAD), 1, false);
                    helm.setDamageValue(180);
                    inv.insertItem(mio_icif_armory.slotOf(2, ArmoryPiece.HEAD), helm, false);
                    inv.insertItem(mio_icif_armory.slotOf(3, ArmoryPiece.MAINHAND),
                        new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:normal/item_lithium_battery"))), false);
                    armory.cycleAutomation(sp);
                }
                sp.openMenu((mio_icif_armory) sp.serverLevel().getBlockEntity(ARMORY), buf -> buf.writeBlockPos(ARMORY));
            });
            client(1, () -> { Minecraft.getInstance().options.hideGui = false; hoverGui = new int[]{140, 140}; });
            shot(40, "04_armory_gui");
            client(1, () -> hoverGui = new int[]{-4000, 4000});
            server(1, ServerPlayer::closeContainer);

            // ---- summon: cinematic camera (a marker stand), the Armory behind the wearer
            server(10, sp -> {
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.teleportTo(sp.serverLevel(), ARMORY.getX() + 0.5, -60, ARMORY.getZ() + 7.5, 0, 0);
                sp.setYBodyRot(0);
                sp.setYHeadRot(0);
                var level = sp.serverLevel();
                var cam = new net.minecraft.world.entity.decoration.ArmorStand(level, ARMORY.getX() + 5.6, -60.6, ARMORY.getZ() + 9.8);
                cam.setInvisible(true);
                cam.setNoGravity(true);
                cam.addTag("si_capture_camera");
                cam.setCustomName(net.minecraft.network.chat.Component.literal("si_capture_camera"));   // tags do not sync
                cam.setCustomNameVisible(false);
                double dx = (ARMORY.getX() + 0.2) - cam.getX(), dz = (ARMORY.getZ() + 6.6) - cam.getZ(), dy = -59.25 - cam.getEyeY();
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                cam.moveTo(cam.getX(), cam.getY(), cam.getZ(), yaw, pitch);
                cam.setYHeadRot(yaw);
                level.addFreshEntity(cam);
            });
            client(10, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(60);
                for (var e : mc.level.entitiesForRendering())
                    if (e.getCustomName() != null && e.getCustomName().getString().equals("si_capture_camera")) mc.setCameraEntity(e);
            });
            server(10, sp -> {
                if (sp.serverLevel().getBlockEntity(ARMORY) instanceof mio_icif_armory armory) armory.summon(sp, 0);
            });
            for (int f = 0; f < 120; f++) shot(1, String.format("flight/f%03d", f));
            shot(10, "06_suit_docked");
            client(1, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setCameraEntity(mc.player);
                mc.options.fov().set(70);
            });
            server(1, sp -> sp.serverLevel().getEntities((net.minecraft.world.entity.Entity) null, sp.getBoundingBox().inflate(40),
                e -> e.getTags().contains("si_capture_camera")).forEach(net.minecraft.world.entity.Entity::discard));

            // ---- maintenance finishes: completion toast bottom right
            server(1, sp -> look(sp, ARMORY.getX() + 0.5, -60, ARMORY.getZ() - 4.5, 0, 15));
            client(1, () -> Minecraft.getInstance().options.hideGui = false);
            until(900, com.miophas.singularity_iteration.common.client.armory.ArmoryToastOverlay::showing);
            shot(14, "10_maintenance_toast");

            // ---- remote GUI (after one more summon so the log has two entries)
            server(10, sp -> {
                if (sp.serverLevel().getBlockEntity(ARMORY) instanceof mio_icif_armory armory) armory.summon(sp, 1);
            });
            server(80, sp -> {
                ItemStack remote = new ItemStack(ArmoryRegistry.REMOTE.get());
                remote.set(ArmoryComponents.TARGET.get(), GlobalPos.of(sp.serverLevel().dimension(), ARMORY));
                sp.setItemInHand(InteractionHand.MAIN_HAND, remote);
                sp.teleportTo(sp.serverLevel(), ARMORY.getX() + 6.5, -60, ARMORY.getZ() - 14.5, 0, 10);
            });
            client(1, () -> { Minecraft.getInstance().options.hideGui = false; hoverGui = new int[]{118, 42};
                Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features()
                    .tab(com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features.TAB_ARMORY)); });
            shot(40, "05_remote_gui");
            client(1, () -> hoverGui = new int[]{-4000, 4000});
            server(1, ServerPlayer::closeContainer);

            // ---- showcases
            server(5, sp -> {
                var level = sp.serverLevel();
                BlockPos base = new BlockPos(40, -60, 40);
                Block showcase = block("producer/block_armor_showcase");
                for (int i = 0; i < 3; i++) {
                    level.setBlock(base.east(i * 2), showcase.defaultBlockState()
                        .setValue(com.miophas.singularity_iteration.common.armory.ArmorShowcaseBlock.FACING, Direction.SOUTH), 3);
                }
                ItemStack[][] outfits = {
                    {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS),
                        new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.SHIELD)},
                    {},
                    {new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.NETHERITE_LEGGINGS),
                        new ItemStack(Items.NETHERITE_BOOTS), new ItemStack(Items.NETHERITE_AXE), ItemStack.EMPTY}};
                for (int i = 0; i < 3; i++) {
                    BlockPos at = base.east(i * 2);
                    var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.UP, at, false);
                    for (ItemStack stack : outfits[i]) {
                        if (stack.isEmpty()) continue;
                        sp.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                        level.getBlockState(at).useItemOn(sp.getMainHandItem(), level, sp, InteractionHand.MAIN_HAND, hit);
                    }
                }
                // middle one mirrors the Armory's "Field Kit"
                ItemStack remote = new ItemStack(ArmoryRegistry.REMOTE.get());
                remote.set(ArmoryComponents.TARGET.get(), GlobalPos.of(level.dimension(), ARMORY));
                BlockPos mid = base.east(2);
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(mid), Direction.UP, mid, false);
                sp.setItemInHand(InteractionHand.MAIN_HAND, remote);
                level.getBlockState(mid).useItemOn(remote, level, sp, InteractionHand.MAIN_HAND, hit);
                level.getBlockState(mid).useItemOn(remote, level, sp, InteractionHand.MAIN_HAND, hit);
                sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                level.setBlock(base.east(4).east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                look(sp, base.getX() + 2.5, -59.2, base.getZ() + 5.2, 180, 12);
            });
            client(1, () -> Minecraft.getInstance().options.hideGui = true);
            shot(40, "09_showcase");
            server(1, sp -> look(sp, 40.9, -59.0, 44.6, 174, 9));
            shot(20, "09b_showcase_closeup");

            // ---- area preview
            server(5, sp -> {
                var level = sp.serverLevel();
                place(level, new BlockPos(-30, -60, 30), "producer/block_laser_defense_tower", Direction.NORTH);
                place(level, new BlockPos(-14, -60, 30), "generator/block_drop_generator", Direction.NORTH);
                place(level, new BlockPos(-8, -60, 30), "producer/block_harvest_elc", Direction.NORTH);
                place(level, new BlockPos(-20, -60, 22), "generator/block_wind_generator", Direction.NORTH);
                place(level, new BlockPos(-2, -60, 24), "producer/block_tesla", Direction.NORTH);
                if (level.getBlockEntity(new BlockPos(-30, -60, 30)) instanceof mio_icif_laser_tower tower) {
                    tower.getEnergyStorageInternal().setStored(200_000);
                    tower.getItemHandler().insertItem(1, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:upgrade/overclocker_upgrade")), 2), false);
                    tower.getItemHandler().insertItem(2, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:upgrade/transformer_upgrade")), 1), false);
                }
                sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:item_tool_area_scanner"))));
                look(sp, -14.5, -44, 0.5, 0, 38);
            });
            server(20, sp -> AreaPreviewService.sendNearby(sp, 48));
            client(1, () -> Minecraft.getInstance().options.hideGui = true);
            shot(40, "07_area_preview");
            server(1, sp -> {
                var pos = new BlockPos(-30, -60, 30);
                sp.teleportTo(sp.serverLevel(), -29.5, -60, 27.5, 0, 20);
                if (sp.serverLevel().getBlockEntity(pos) instanceof mio_icif_laser_tower tower) sp.openMenu(tower, buf -> buf.writeBlockPos(pos));
            });
            client(1, () -> Minecraft.getInstance().options.hideGui = false);
            shot(30, "08_laser_tower_upgrades");
            server(1, ServerPlayer::closeContainer);
            client(20, () -> Minecraft.getInstance().stop());
        }
    }
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (OUT == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (!worldRequested && mc.getOverlay() == null && mc.screen != null) {
                Singularity_Iteration.LOGGER.info("Capture: creating world from {}", mc.screen.getClass().getSimpleName());
                worldRequested = true;
                var settings = new LevelSettings("si_capture", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("si_capture_" + System.currentTimeMillis(), settings,
                    new WorldOptions(1234L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            }
            return;
        }
        if (mc.player == null || index >= STEPS.size()) return;
        mc.getToasts().clear();
        mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
        if (hoverGui != null) hover(mc, hoverGui[0], hoverGui[1]);
        Step step = STEPS.get(index);
        if (step.until() != null) {
            if (!step.until().getAsBoolean() && wait < step.delayTicks()) { wait++; return; }
        } else if (wait < step.delayTicks()) { wait++; return; }
        wait = 0;
        try {
            Singularity_Iteration.LOGGER.info("Capture step {}/{}", index, STEPS.size());
            STEPS.get(index++).action().run();
        } catch (RuntimeException e) {
            Singularity_Iteration.LOGGER.error("Capture step {} failed", index - 1, e);
        }
    }
}
