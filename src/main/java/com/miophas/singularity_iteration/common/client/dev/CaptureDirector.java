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
            if (Boolean.getBoolean("si.capture.guiBench")) guiBench(mc, name);
        });
    }

    /**
     * 0.1.11.24: with -Dsi.capture.guiBench=true, after each shot with a screen open, the screen is drawn
     * {@code GUI_BENCH_FRAMES} times more (after a warm-up), with a GPU sync before and after, and the time per frame is
     * logged as "[guibench]". The time includes the driver's work on the draw calls, which is what batching saves.
     */
    private static void guiBench(Minecraft mc, String name) {
        if (mc.screen == null) return;
        var g = new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource());
        int mx = -4000, my = 4000;
        for (int i = 0; i < 60; i++) { mc.screen.render(g, mx, my, 0); g.flush(); }
        org.lwjgl.opengl.GL11.glFinish();
        long[] each = new long[GUI_BENCH_FRAMES];
        for (int i = 0; i < GUI_BENCH_FRAMES; i++) {
            long t = System.nanoTime();
            mc.screen.render(g, mx, my, 0);
            g.flush();
            org.lwjgl.opengl.GL11.glFinish();
            each[i] = System.nanoTime() - t;
        }
        java.util.Arrays.sort(each);
        long sum = 0;
        for (long v : each) sum += v;
        org.slf4j.LoggerFactory.getLogger(CaptureDirector.class).info("[guibench] {} ({}) | mean {} us | p50 {} us | p90 {} us",
            name, mc.screen.getClass().getSimpleName(), String.format("%.0f", sum / (double) GUI_BENCH_FRAMES / 1000),
            String.format("%.0f", each[GUI_BENCH_FRAMES / 2] / 1000.0), String.format("%.0f", each[GUI_BENCH_FRAMES * 9 / 10] / 1000.0));
    }

    private static final int GUI_BENCH_FRAMES = 300;

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

    /**
     * JEI review mode: -Dsi.capture.jei=ALL (or uid,uid,...) opens each SI JEI recipe category at GUI
     * scale 3 and screenshots it (tools/jei-capture.init.gradle).
     */
    private static void jeiGallery(String list) {
        if ("TRANSFER".equals(list)) {
            jeiTransferCheck();
            return;
        }
        server(40, sp -> {
            sp.serverLevel().setDayTime(6000);
            sp.setGameMode(GameType.CREATIVE);
            look(sp, 0.5, -60, -3.5, 0, 0);
        });
        client(5, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            mc.options.guiScale().set(3);
            mc.resizeDisplay();
        });
        // JEI builds its runtime after the world is joined
        until(600, () -> !com.miophas.singularity_iteration.common.client.integration.jei.JeiBridge.categories("mio_icif").isEmpty());
        client(20, () -> {
            java.util.List<ResourceLocation> uids = "ALL".equals(list)
                ? com.miophas.singularity_iteration.common.client.integration.jei.JeiBridge.categories("mio_icif")
                : java.util.Arrays.stream(list.split(",")).map(s -> ResourceLocation.parse(s.contains(":") ? s.strip() : "mio_icif:" + s.strip())).toList();
            for (var uid : uids) {
                client(2, () -> com.miophas.singularity_iteration.common.client.integration.jei.JeiBridge.showCategory(uid));
                shot(15, "jei/" + uid.getPath());
                client(1, () -> Minecraft.getInstance().setScreen(null));
            }
            client(10, () -> Minecraft.getInstance().stop());
        });
    }

    /**
     * -Dsi.capture.jei=TRANSFER (0.1.10.14): JEI's crafting "+" on a crafting table and on an Industrial
     * Workbench, with charged and empty batteries and with the right and an uncut turning blank. One line
     * per case goes to jei_transfer.txt ("ok" or the slots JEI finds missing).
     */
    private static void jeiTransferCheck() {
        BlockPos table = new BlockPos(2, -60, 0), bench = new BlockPos(-2, -60, 0);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            sp.setGameMode(GameType.CREATIVE);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 0);
            level.setBlock(table, net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            level.setBlock(bench, block("producer/block_industrial_workbench").defaultBlockState(), 3);
        });
        until(600, () -> !com.miophas.singularity_iteration.common.client.integration.jei.JeiBridge.categories("mio_icif").isEmpty());
        transferCase(table, "shaped_8_batbox", true, false, false);
        transferCase(table, "shaped_8_batbox", false, false, false);
        transferCase(table, "shaped_9_mfe", true, false, false);
        transferCase(table, "shaped_350_laser_miner", true, false, false);
        transferCase(table, "shaped_350_laser_miner", true, true, false);
        transferCase(bench, "shaped_8_batbox", true, false, true);
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Gives the player one of each input of {@code recipe}, opens the menu at {@code at} and records JEI's verdict. */
    private static void transferCase(BlockPos at, String recipe, boolean charged, boolean uncutBlank, boolean hotbar) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mio_icif", recipe);
        server(5, sp -> {
            sp.closeContainer();
            sp.getInventory().clearContent();
            var holder = sp.serverLevel().getRecipeManager().byKey(id).orElse(null);
            if (holder == null) return;
            int slot = 0;
            for (var ingredient : holder.value().getIngredients()) {
                ItemStack[] options = ingredient.getItems();
                if (options.length == 0) continue;
                ItemStack stack = options[0].copyWithCount(1);
                if (uncutBlank && stack.getItem() instanceof com.miophas.singularity_iteration.common.item.lathe.LatheTurningBlanksItem) {
                    stack = new ItemStack(stack.getItem());
                }
                if (charged && stack.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem) {
                    var items = com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI();
                    items.setBatteryEnergy(stack, items.getBatteryCapacity(stack));
                }
                if (hotbar) sp.getInventory().setItem(slot++, stack);
                else sp.getInventory().add(stack);
            }
            var level = sp.serverLevel();
            if (level.getBlockState(at).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)) {
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider((containerId, inventory, player) -> new net.minecraft.world.inventory.CraftingMenu(
                    containerId, inventory, net.minecraft.world.inventory.ContainerLevelAccess.create(level, at)), net.minecraft.network.chat.Component.literal("Crafting")));
            } else if (level.getBlockEntity(at) instanceof net.minecraft.world.MenuProvider provider) {
                sp.openMenu(provider);
            }
        });
        client(10, () -> {
            var menu = Minecraft.getInstance().player.containerMenu;
            String line = recipe + (charged ? " charged" : " empty") + (uncutBlank ? " uncut-blank" : "") + (hotbar ? " hotbar" : "")
                + " in " + menu.getClass().getSimpleName() + ": "
                + com.miophas.singularity_iteration.common.client.integration.jei.JeiBridge.transferCheck(id) + "\n";
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(OUT, "jei_transfer.txt"), line,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (java.io.IOException ignored) { }
        });
    }

    private static void galleryShots(String[] ids) {
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i].strip();
            BlockPos at = new BlockPos(i * 3, -60, 0);
            server(2, sp -> {
                var level = sp.serverLevel();
                level.setBlock(at, block(id).defaultBlockState(), 3);
                if (level.getBlockEntity(at) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity() / 2);
                // 0.1.11.110: -Dsi.capture.galleryInput=item_id puts 32 of it in slot 0 (machines shot while working)
                String input = System.getProperty("si.capture.galleryInput");
                if (input != null && level.getBlockEntity(at) != null) {
                    var handler = level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity pm
                        ? pm.getItemHandler() : null;
                    var item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(input));
                    if (handler instanceof net.neoforged.neoforge.items.IItemHandlerModifiable m) m.setStackInSlot(0, new net.minecraft.world.item.ItemStack(item, 32));
                    // 0.1.11.115: -Dsi.capture.gallerySlot1=item_id puts one of it in slot 1 (the block cutter's blade)
                    String second = System.getProperty("si.capture.gallerySlot1");
                    if (second != null && handler instanceof net.neoforged.neoforge.items.IItemHandlerModifiable m2)
                        m2.setStackInSlot(1, new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(second))));
                    if (level.getBlockEntity(at) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity());
                }
                // 0.1.11.114: -Dsi.capture.galleryWater=mB fills the machine's tank with water
                Integer water = Integer.getInteger("si.capture.galleryWater");
                if (water != null) {
                    var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, at, null);
                    if (tank != null) tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, water),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
                sp.teleportTo(level, at.getX() + 0.5, -60, at.getZ() - 2.5, 0, 20);
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
                level.getBlockState(at).useWithoutItem(level, sp, hit);
            });
            client(Integer.getInteger("si.capture.galleryWait", 20), () -> dumpUpgradeLayout(id));   // 0.1.11.110: longer wait on request
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
        // 8) classic look: fronts + suits + icons (0.1.10.24: the only look; the experimental pack and its switch are gone)
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
        // 0.1.10.24: the experimental look (built-in pack) is gone, no second set of shots
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
                var casing = new com.miophas.singularity_iteration.common.block.wire.WireCasing[]{com.miophas.singularity_iteration.common.block.wire.WireCasing.NONE, com.miophas.singularity_iteration.common.block.wire.WireCasing.SCAFFOLD, com.miophas.singularity_iteration.common.block.wire.WireCasing.REINFORCED_FOAM, com.miophas.singularity_iteration.common.block.wire.WireCasing.CONSTRUCTION_WALL}[x];
                level.setBlock(at, com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire.withCasing(st, casing), 3);
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

    /** Scene 13 (-Dsi.capture.scene=13): FCS HUD, deflector field, Special Maneuver Mode, modification station, layout editor. */
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
        // Special Maneuver Mode
        server(30, sp -> {
            var chest = sp.getItemBySlot(EquipmentSlot.CHEST);
            com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.setEnabled(chest, "special_maneuver", true);
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
            Singularity_Iteration.LOGGER.warn("[scene13] flight: pos={} abilitiesFlying={} maneuver={} speed={}", pl.position(), pl.getAbilities().flying,
                com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.isFlying(),
                com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.speed()); });
        shot(2, "scene13/maneuver_flight_back");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        shot(6, "scene13/maneuver_flight_hud");
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
        shot(15, "scene13/console_maneuver_units");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene 14 (-Dsi.capture.scene=14), 0.1.7.34: ore sonar echo, ballistic arc, blast warning,
     * cube holomap, refined panels, compass off, Special Maneuver Mode (hover, cruise, shock
     * barrier, flight HUD) and the nano suit model and icons.
     */
    private static void scene14() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);   // 0.1.11.63: a lost focus paused the flight
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            sp.setGameMode(GameType.SURVIVAL);
            int[][] ores = {{2, -62, 6}, {-3, -63, 8}, {4, -63, 10}, {-1, -62, 12}, {6, -62, 4}, {-5, -62, 5}, {1, -63, 3}, {3, -62, 14},
                {-2, -63, 4}, {5, -63, 7}, {0, -62, 9}, {-4, -62, 11}};
            String[] kinds = {"minecraft:diamond_ore", "minecraft:gold_ore", "minecraft:iron_ore", "minecraft:redstone_ore", "minecraft:lapis_ore",
                "minecraft:emerald_ore", "minecraft:copper_ore", "minecraft:coal_ore", "minecraft:diamond_ore", "minecraft:iron_ore",
                "minecraft:gold_ore", "minecraft:redstone_ore"};
            for (int i = 0; i < ores.length; i++)
                level.setBlock(new BlockPos(ores[i][0], ores[i][1], ores[i][2]),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse(kinds[i])).defaultBlockState(), 3);
            // structures for the holomap: a walled yard, a tower and a pit
            var bricks = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
            var glass = net.minecraft.world.level.block.Blocks.CYAN_STAINED_GLASS.defaultBlockState();
            for (int x = -9; x <= -3; x++) for (int y = -60; y <= -58; y++) { level.setBlock(new BlockPos(x, y, -6), bricks, 3); level.setBlock(new BlockPos(x, y, -1), bricks, 3); }
            for (int z = -6; z <= -1; z++) for (int y = -60; y <= -58; y++) level.setBlock(new BlockPos(-9, y, z), bricks, 3);
            for (int y = -60; y <= -52; y++) for (int x = 6; x <= 8; x++) for (int z = -5; z <= -3; z++)
                if (x != 7 || z != -4) level.setBlock(new BlockPos(x, y, z), y % 3 == 0 ? glass : bricks, 3);
            for (int x = -2; x <= 2; x++) for (int z = -10; z <= -7; z++) for (int y = -63; y <= -61; y++)
                level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(new BlockPos(0, -63, -9), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            place(level, new BlockPos(-4, -60, 9), "generator/block_geo_generator", Direction.EAST);
            place(level, new BlockPos(-3, -60, 9), "wiring/block_mfe", Direction.EAST);
            place(level, new BlockPos(-2, -60, 9), "producer/block_powder_elc", Direction.SOUTH);
            if (level.getBlockEntity(new BlockPos(-3, -60, 9)) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity() / 2);
            look(sp, 0.5, -60, 0.5, 0, 34);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            suitUp(sp);
            sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SNOWBALL, 16));
            mob(level, net.minecraft.world.entity.EntityType.HUSK, 3.5, -60, 9.5, true, sp).setTarget(sp);
            mob(level, net.minecraft.world.entity.EntityType.WOLF, 5.5, -60, 6.5, true, sp);
            mob(level, net.minecraft.world.entity.EntityType.COW, -5.5, -60, 12.5, true, null);
        });
        client(5, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.setShowCompass(true);
        });
        // ore echo: restart the scanner to fire a fresh pulse, shoot it mid-glitch, mid-wave and after
        client(60, () -> com.miophas.singularity_iteration.common.client.suit.OreScanner.reset());
        shot(8, "scene14/ore_echo_glitch");
        shot(14, "scene14/ore_echo_wave");
        shot(40, "scene14/ore_echo_revealed");
        client(1, () -> Singularity_Iteration.LOGGER.warn("[scene14] ores={} echo={}",
            com.miophas.singularity_iteration.common.client.suit.OreScanner.hits().size(),
            com.miophas.singularity_iteration.common.client.suit.OreScanner.echoProgress()));
        // ballistic: snowball arc, then crossbow lock on the husk
        server(2, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 35, -22));
        shot(25, "scene14/ballistic_arc");
        server(2, sp -> {
            var crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
            sp.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
            var husk = sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, sp.getBoundingBox().inflate(30)).stream().findFirst().orElse(null);
            if (husk != null) {
                var d = husk.getEyePosition().subtract(sp.getEyePosition());
                float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
                float pitch = (float) (-Math.toDegrees(Math.atan2(d.y - 0.6, Math.sqrt(d.x * d.x + d.z * d.z))));
                sp.teleportTo(sp.serverLevel(), sp.getX(), sp.getY(), sp.getZ(), yaw, pitch);
            }
        });
        shot(25, "scene14/ballistic_lock");
        // blast warning: one charge in front, one behind the wearer
        server(2, sp -> {
            var level = sp.serverLevel();
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 22);
            var a = new net.minecraft.world.entity.item.PrimedTnt(level, 2.0, -60, 4.5, null);
            a.setFuse(80);
            level.addFreshEntity(a);
            var b = new net.minecraft.world.entity.item.PrimedTnt(level, -1.5, -60, -2.0, null);
            b.setFuse(110);
            level.addFreshEntity(b);
        });
        shot(34, "scene14/blast_warning");
        server(1, sp -> sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class, sp.getBoundingBox().inflate(40)).forEach(e -> e.discard()));
        // overview with the refined panels and the cube holomap, then the holomap enlarged, then compass off
        server(2, sp -> sp.teleportTo(sp.serverLevel(), -1.5, -60, 3.5, 200, 12));
        shot(40, "scene14/fcs_panels_overview");
        client(1, () -> { if (com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.loaded())
            com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.HOLOMAP_SCALE.set(1.8); });
        shot(10, "scene14/holomap_cube");
        server(2, sp -> sp.teleportTo(sp.serverLevel(), -1.5, -60, 3.5, 120, 12));
        shot(20, "scene14/holomap_cube_turned");
        client(1, () -> {
            if (com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.loaded())
                com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.HOLOMAP_SCALE.set(1.0);
            com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.setShowCompass(false);
        });
        shot(5, "scene14/compass_off");
        client(1, () -> com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.setShowCompass(true));
        client(3, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.suit.HudLayoutScreen(null)));
        shot(12, "scene14/hud_layout_compass_toggle");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        // Special Maneuver Mode: hover (front + back), cruise, shock barrier, flight HUD
        server(10, sp -> {
            var chest = sp.getItemBySlot(EquipmentSlot.CHEST);
            com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.setEnabled(chest, "special_maneuver", true);
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            sp.teleportTo(sp.serverLevel(), 0.5, -52, -20.5, 0, 8);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
        });
        client(3, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        client(5, () -> Minecraft.getInstance().options.keyUp.setDown(true));
        shot(14, "scene14/maneuver_hover_front");
        client(1, () -> { Minecraft.getInstance().options.keyUp.setDown(false); Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK); });
        shot(10, "scene14/maneuver_hover_back");
        server(2, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -40, -60.5, 0, -4));
        client(4, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
        });
        shot(22, "scene14/maneuver_cruise");
        client(1, () -> Singularity_Iteration.LOGGER.warn("[scene14] cruise: flying={} throttle={} speed={}",
            com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.isFlying(),
            com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.throttle(),
            com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.speed()));
        // a gentle turn while the throttle passes the shock barrier: drift, bank and vapour cone
        client(8, () -> { var pl = Minecraft.getInstance().player; pl.setYRot(pl.getYRot() + 6); });
        client(2, () -> { var pl = Minecraft.getInstance().player; pl.setYRot(pl.getYRot() + 6); });
        // 0.1.11.5: the barrier is the measured speed, so wait for it instead of a fixed tick
        until(120, () -> com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.supersonic(Minecraft.getInstance().player));
        shot(3, "scene14/maneuver_shock_barrier");
        shot(10, "scene14/maneuver_supersonic");
        // drop below M 0.9 and cross again in first person: the edge flash of the own boom
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.keySprint.setDown(false); });
        until(120, () -> !com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.supersonic(Minecraft.getInstance().player));
        client(1, () -> Minecraft.getInstance().options.keySprint.setDown(true));
        until(160, () -> com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.supersonic(Minecraft.getInstance().player));
        shot(2, "scene14/maneuver_boom_first_person");
        shot(12, "scene14/maneuver_flight_hud");
        client(1, () -> Singularity_Iteration.LOGGER.warn("[scene14] supersonic: throttle={} speed={}",
            com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.throttle(),
            com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.speed()));
        client(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
        });
        // nano suit
        server(30, sp -> {
            var level = sp.serverLevel();
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) {
                ItemStack st = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "armor/item_armor_nano_" + pieces[i])));
                sp.setItemSlot(slots[i], st);
                sp.getInventory().setItem(i, st.copy());
            }
            sp.teleportTo(level, 20.5, -60, 20.5, 150, 6);
        });
        client(3, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); mc.options.hideGui = true; });
        shot(30, "scene14/nano_suit_front");
        server(1, sp -> sp.teleportTo(sp.serverLevel(), 20.5, -60, 20.5, 210, 6));
        shot(10, "scene14/nano_suit_side");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot(10, "scene14/nano_suit_back");
        client(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        });
        shot(15, "scene14/nano_icons_inventory");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene 15 (-Dsi.capture.scene=15), 0.1.7.35: nano suit 3D item icons and turned boots, Armory
     * GUI tooltips (slot area, energy bar, status line), battery auto-charge and Energy Saving
     * tooltips, the equipment HUD line fading after 3 s, and an Armory delivery in flight.
     */
    private static void scene15() {
        BlockPos armoryAt = new BlockPos(8, -60, 12);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                ItemStack[] suit = {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS),
                    new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.SHIELD)};
                for (int c = 0; c < 6; c++) inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.COLUMNS[c]), suit[c], false);
                armory.rename(sp, 0, "Diamond Mk.II");
            }
            var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var saving = enchantments.getOrThrow(com.miophas.singularity_iteration.core.api.item.EnergySaving.KEY);
            String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) {
                ItemStack st = item("armor/item_armor_nano_" + pieces[i]);
                sp.setItemSlot(slots[i], st);
                sp.getInventory().setItem(i, st.copy());
            }
            ItemStack sword = item("item_tool_quantum_sword");
            if (sword.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(sword, b.getMaxEnergy(sword) / 2);
            sword.enchant(saving, 5);
            sp.getInventory().setItem(4, sword);
            ItemStack battery = item("normal/item_lapotron_crystal_lev0");
            if (battery.isEmpty()) battery = item("normal/item_crystal_lev0");
            if (battery.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(battery, b.getMaxEnergy(battery));
            com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge.toggle(battery);
            sp.getInventory().setItem(5, battery);
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
                new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY) {{ set(saving, 5); }}.toImmutable());
            sp.getInventory().setItem(6, book);
            sp.getInventory().selected = 7;
            sp.teleportTo(level, 8.5, -60, 9.0, 0, 20);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        // Armory GUI: the maintenance tooltip only over the status line
        server(10, sp -> sp.openMenu((mio_icif_armory) sp.serverLevel().getBlockEntity(armoryAt), buf -> buf.writeBlockPos(armoryAt)));
        client(1, () -> hoverGui = new int[]{100, 60});
        shot(30, "scene15/armory_gui_no_tooltip_mid");
        client(1, () -> hoverGui = new int[]{120, 141});
        shot(5, "scene15/armory_gui_status_tooltip");
        client(1, () -> hoverGui = new int[]{80, 132});
        shot(5, "scene15/armory_gui_energy_tooltip");
        client(1, () -> hoverGui = new int[]{30, 30});
        shot(5, "scene15/armory_gui_rename_tooltip");
        client(1, () -> hoverGui = new int[]{-4000, 4000});
        server(1, ServerPlayer::closeContainer);
        // inventory: 3D nano icons, battery and Energy Saving tooltips
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(15, "scene15/nano_icons_3d_inventory");
        client(1, () -> hoverGui = new int[]{8 + 5 * 18 + 8, 150});
        shot(5, "scene15/battery_auto_charge_tooltip");
        client(1, () -> hoverGui = new int[]{8 + 4 * 18 + 8, 150});
        shot(5, "scene15/energy_saving_sword_tooltip");
        client(1, () -> hoverGui = new int[]{8 + 6 * 18 + 8, 150});
        shot(5, "scene15/energy_saving_book_tooltip");
        client(1, () -> { hoverGui = new int[]{-4000, 4000}; Minecraft.getInstance().setScreen(null); });
        // worn nano suit: the boots now face forward
        client(2, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        server(1, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -60, -6.5, 180, 30));
        client(1, () -> Minecraft.getInstance().options.hideGui = true);
        shot(20, "scene15/nano_boots_front");
        server(1, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -60, -6.5, 90, 20));
        shot(10, "scene15/nano_boots_side");
        server(1, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -60, -6.5, 0, 5));
        shot(10, "scene15/nano_suit_back");
        // equipment HUD: the sword line shows, then fades (no burn-in)
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        server(1, sp -> { sp.getInventory().selected = 4; sp.teleportTo(sp.serverLevel(), 0.5, -60, -6.5, 0, 0); });
        client(1, () -> Minecraft.getInstance().player.getInventory().selected = 4);
        shot(20, "scene15/hud_sword_line_shown");
        shot(110, "scene15/hud_sword_line_faded");
        // Armory delivery in flight
        client(1, () -> Minecraft.getInstance().player.getInventory().selected = 7);
        server(5, sp -> {
            sp.getInventory().selected = 7;
            sp.teleportTo(sp.serverLevel(), armoryAt.getX() + 0.5, -60, armoryAt.getZ() - 6.5, 180, 0);
            if (sp.serverLevel().getBlockEntity(armoryAt) instanceof mio_icif_armory armory) armory.summon(sp, 0);
        });
        client(2, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(70, "scene15/armory_delivery_a");
        shot(4, "scene15/armory_delivery_b");
        shot(30, "scene15/armory_delivery_docked");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "15".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene15();
        }
    }

    /**
     * Scene 1136 (-Dsi.capture.scene=1136), 0.1.10.36: the enchantment glint on the baked suit armor (scaled to its
     * 128x128 texture) next to vanilla-model bronze, worn and on armor stands, the first-person arm and the inventory icons;
     * the Miner's Retract button and its tooltip.
     */
    /** Scene 1136: the platform 40 blocks above the spawn point (chunks there are loaded; other places may still generate). */
    private static BlockPos base1136 = BlockPos.ZERO;

    private static BlockPos at1136(int dx, int dy, int dz) { return base1136.offset(dx, dy, dz); }

    private static void scene1136() {
        server(40, sp -> {
            var level = sp.serverLevel();
            base1136 = new BlockPos(sp.getBlockX(), Math.min(level.getMaxBuildHeight() - 24, sp.getBlockY() + 40), sp.getBlockZ());
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            for (int x = -8; x <= 10; x++) for (int z = -12; z <= 8; z++)
                level.setBlock(at1136(x, -1, z), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            var protection = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
            String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            String[] sets = {"bronze", "nano", "quantum", "quantum"};
            for (int s = 0; s < sets.length; s++) {
                BlockPos p = at1136(-3 + s * 2, 0, 0);
                var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                stand.setYRot(180); stand.setYBodyRot(180); stand.setYHeadRot(180);
                for (int i = 0; i < 4; i++) {
                    ItemStack st = item("armor/item_armor_" + sets[s] + "_" + pieces[i]);
                    if (s < 3) st.enchant(protection, 4);   // the last quantum stand stays plain, for comparison
                    stand.setItemSlot(slots[i], st);
                }
                level.addFreshEntity(stand);
            }
            sp.getInventory().clearContent();
            for (int i = 0; i < 4; i++) {
                ItemStack st = item("armor/item_armor_quantum_" + pieces[i]);
                st.enchant(protection, 4);
                sp.setItemSlot(slots[i], st);
                ItemStack icon = item("armor/item_armor_" + (i < 2 ? "nano_" : "quantum_") + pieces[i]);
                icon.enchant(protection, 4);
                sp.getInventory().setItem(i, icon);
            }
            ItemStack bronze = item("armor/item_armor_bronze_chestplate");
            bronze.enchant(protection, 4);
            sp.getInventory().setItem(4, bronze);
            sp.getInventory().selected = 8;
            place(level, at1136(6, 0, 4), "producer/block_miner_elc", Direction.NORTH);
            look(sp, base1136.getX() + 0.5, base1136.getY() + 1.0, base1136.getZ() - 4.5, 0, 10);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.getInventory().selected = 8; });
        shot(80, "scene1136/glint_stands_a");
        shot(7, "scene1136/glint_stands_b");
        server(1, sp -> look(sp, base1136.getX() + 2.5, base1136.getY() + 0.2, base1136.getZ() - 1.4, 0, 12));
        shot(30, "scene1136/glint_quantum_close");
        server(1, sp -> look(sp, base1136.getX() - 2.5, base1136.getY() + 0.2, base1136.getZ() - 1.4, 0, 12));
        shot(30, "scene1136/glint_bronze_nano_close");
        // first-person arm (empty hand) and the worn suit from the front
        server(1, sp -> { sp.getAbilities().flying = false; sp.onUpdateAbilities();
            sp.teleportTo(sp.serverLevel(), base1136.getX() + 0.5, base1136.getY(), base1136.getZ() - 8.5, 180, 10); });
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.player.getInventory().selected = 8; });
        shot(30, "scene1136/glint_first_person_arm");
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(25, "scene1136/glint_worn_front");
        // inventory: the survival screen shows the worn pieces and the icons
        server(1, sp -> sp.setGameMode(GameType.SURVIVAL));
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(20, "scene1136/glint_inventory_icons");
        client(1, () -> Minecraft.getInstance().setScreen(null));
        // the Miner: Retract button and its tooltip
        server(5, sp -> {
            BlockPos miner = at1136(6, 0, 4);
            sp.teleportTo(sp.serverLevel(), miner.getX() + 0.5, miner.getY(), miner.getZ() - 2.5, 0, 30);
            if (sp.serverLevel().getBlockEntity(miner) instanceof net.minecraft.world.MenuProvider menu)
                sp.openMenu(menu, buf -> buf.writeBlockPos(miner));
        });
        client(1, () -> hoverGui = new int[]{-4000, 4000});
        shot(25, "scene1136/miner_gui");
        client(1, () -> hoverGui = new int[]{152, 10});
        shot(10, "scene1136/miner_retract_tooltip");
        client(1, () -> hoverGui = new int[]{-4000, 4000});
        // screenshots are written on a background thread: let them finish before the game stops
        client(80, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "1136".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene1136();
        }
    }

    // ------------------------------------------------------------------ 0.1.11.184 the Blueprint Builder's block hologram

    static {
        if (OUT != null && "ghost".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneGhost();
        }
    }

    private static BlockPos ghostBase = BlockPos.ZERO;

    /** A structure file of the given blocks (relative positions) in the server's schematics folder. */
    private static void ghostFile(String name, java.util.Map<BlockPos, BlockState> blocks) {
        java.util.List<BlockState> palette = new java.util.ArrayList<>();
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        int sx = 1, sy = 1, sz = 1;
        for (var e : blocks.entrySet()) {
            int i = palette.indexOf(e.getValue());
            if (i < 0) { i = palette.size(); palette.add(e.getValue()); }
            var b = new net.minecraft.nbt.CompoundTag();
            b.putInt("state", i);
            var pos = new net.minecraft.nbt.ListTag();
            pos.add(net.minecraft.nbt.IntTag.valueOf(e.getKey().getX()));
            pos.add(net.minecraft.nbt.IntTag.valueOf(e.getKey().getY()));
            pos.add(net.minecraft.nbt.IntTag.valueOf(e.getKey().getZ()));
            b.put("pos", pos);
            list.add(b);
            sx = Math.max(sx, e.getKey().getX() + 1); sy = Math.max(sy, e.getKey().getY() + 1); sz = Math.max(sz, e.getKey().getZ() + 1);
        }
        var root = new net.minecraft.nbt.CompoundTag();
        var pal = new net.minecraft.nbt.ListTag();
        for (BlockState st : palette) pal.add(net.minecraft.nbt.NbtUtils.writeBlockState(st));
        root.put("palette", pal);
        root.put("blocks", list);
        var size = new net.minecraft.nbt.ListTag();
        size.add(net.minecraft.nbt.IntTag.valueOf(sx)); size.add(net.minecraft.nbt.IntTag.valueOf(sy)); size.add(net.minecraft.nbt.IntTag.valueOf(sz));
        root.put("size", size);
        try {
            var dir = com.miophas.singularity_iteration.common.builder.BlueprintReader.folder();
            java.nio.file.Files.createDirectories(dir);
            net.minecraft.nbt.NbtIo.writeCompressed(root, dir.resolve(name));
        } catch (java.io.IOException e) {
            note("ghost file " + name + ": " + e);
        }
    }

    /** Loads the schematics file {@code name} into the builder at {@code at} (steps from the first file: load the files in their order). */
    private static void ghostLoad(ServerPlayer sp, BlockPos at, String name) {
        if (!(sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity b)) return;
        b.action(sp, com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity.A_STOP, 0);
        int index = b.files().indexOf(name);
        for (int k = 0; k < index; k++) b.action(sp, com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity.A_FILE_NEXT, 0);
        b.action(sp, com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity.A_LOAD, 0);
        note("ghost load " + name + " (index " + index + ", " + b.total() + " blocks)");
    }

    private static final java.util.List<Integer> GHOST_FPS = new java.util.ArrayList<>();

    /** Records the frame rate each second for {@code seconds}, then notes the mean as {@code label}. */
    private static void ghostFps(String label, int seconds) {
        client(5, () -> GHOST_FPS.clear());   // a lambda: the scene is queued before the field is set
        for (int k = 0; k < seconds; k++) client(20, () -> GHOST_FPS.add(Minecraft.getInstance().getFps()));
        client(1, () -> note("ghost fps " + label + ": " + GHOST_FPS + " mean "
            + GHOST_FPS.stream().mapToInt(Integer::intValue).average().orElse(0)));
    }

    /**
     * Scene "ghost" (-Dsi.capture.scene=ghost), 0.1.11.184: a builder with a small house (part of it already built: only the
     * rest shows as ghosts), in the block, layer and frame views; then a 64 x 24 x 64 block (about 98,000 blocks) with the
     * frame rate in the block view against the frame view.
     */
    private static void sceneGhost() {
        server(40, sp -> {
            var level = sp.serverLevel();
            ghostBase = new BlockPos(sp.getBlockX() + 3, -60, sp.getBlockZ() + 3);
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            java.util.Map<BlockPos, BlockState> house = new java.util.LinkedHashMap<>();
            for (int x = 0; x < 9; x++) for (int z = 0; z < 7; z++) house.put(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
            for (int y = 1; y <= 4; y++) for (int x = 0; x < 9; x++) for (int z = 0; z < 7; z++) {
                boolean wall = x == 0 || x == 8 || z == 0 || z == 6;
                if (!wall) continue;
                boolean corner = (x == 0 || x == 8) && (z == 0 || z == 6);
                BlockState st = corner ? net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState()
                    : y == 2 && (x == 2 || x == 6) ? net.minecraft.world.level.block.Blocks.GLASS_PANE.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState();
                if (z == 0 && x == 4 && y <= 2) continue;   // the doorway
                house.put(new BlockPos(x, y, z), st);
            }
            for (int x = 0; x < 9; x++) for (int z = 0; z < 7; z++) house.put(new BlockPos(x, 5, z), net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS.defaultBlockState());
            for (int x = 1; x < 8; x++) for (int z = 1; z < 6; z++) house.put(new BlockPos(x, 6, z), net.minecraft.world.level.block.Blocks.SPRUCE_SLAB.defaultBlockState());
            house.put(new BlockPos(2, 1, 3), net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState());
            house.put(new BlockPos(6, 1, 3), net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());
            ghostFile("si_ghost_1_house.nbt", house);
            java.util.Map<BlockPos, BlockState> big = new java.util.LinkedHashMap<>();
            BlockState[] layers = {net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Blocks.ANDESITE.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.GRANITE.defaultBlockState(), net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState()};
            for (int y = 0; y < 24; y++) for (int x = 0; x < 64; x++) for (int z = 0; z < 64; z++)
                if ((x + z + y) % 7 != 0) big.put(new BlockPos(x, y, z), layers[(y / 3) % layers.length]);
            ghostFile("si_ghost_2_big.nbt", big);
            for (int x = -2; x <= 70; x++) for (int z = -2; z <= 70; z++) level.setBlock(ghostBase.offset(x, -1, z), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            place(level, ghostBase, "producer/block_blueprint_builder", Direction.NORTH);
        });
        server(5, sp -> {
            if (sp.serverLevel().getBlockEntity(ghostBase) instanceof com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity b)
                b.action(sp, com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity.A_REFRESH, 0);
        });
        server(5, sp -> {
            ghostLoad(sp, ghostBase, "si_ghost_1_house.nbt");
            // part of it built already: the floor and the first ring of the walls (only the rest shows)
            var level = sp.serverLevel();
            for (int x = 0; x < 9; x++) for (int z = 0; z < 7; z++) level.setBlock(ghostBase.offset(1 + x, 0, 1 + z), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
            look(sp, ghostBase.getX() - 5.5, ghostBase.getY() + 7.5, ghostBase.getZ() - 6.5, -40, 28);
        });
        client(1, () -> com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.mode = com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.BLOCKS);
        shot(80, "ghost_house_blocks");
        client(1, () -> com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.mode = com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.LAYER);
        shot(40, "ghost_house_layer");
        client(1, () -> com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.mode = com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.FRAME);
        shot(20, "ghost_house_frame");
        // the large blueprint: the frame rate with the blocks shown and with the frame only
        client(1, () -> {   // no frame cap, no vsync: the frame rate shows the cost
            Minecraft mc = Minecraft.getInstance();
            mc.options.framerateLimit().set(260);
            mc.options.enableVsync().set(false);
            mc.getWindow().updateVsync(false);
        });
        server(5, sp -> {
            ghostLoad(sp, ghostBase, "si_ghost_2_big.nbt");
            look(sp, ghostBase.getX() - 18.5, ghostBase.getY() + 34.5, ghostBase.getZ() - 18.5, -45, 35);
        });
        client(1, () -> com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.mode = com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.FRAME);
        ghostFps("big frame", 6);
        client(1, () -> com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.mode = com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.BLOCKS);
        ghostFps("big blocks (baking)", 3);
        ghostFps("big blocks", 6);
        shot(5, "ghost_big_blocks");
        client(1, () -> com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.mode = com.miophas.singularity_iteration.common.client.builder.BuilderGhosts.LAYER);
        ghostFps("big layer", 4);
        shot(5, "ghost_big_layer");
        client(20, () -> Minecraft.getInstance().stop());
    }

    // ------------------------------------------------------------------ 0.1.10.15 player feedback review

    static {
        if (OUT != null && "feedback".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            feedbackScene();
        }
    }

    /** Chat lines the client received (feedback scene). */
    private static final List<String> CHAT = new ArrayList<>();
    /** Lets the chat show in the feedback screenshots (the other scenes hide it). */
    private static boolean showChat;
    /** Machine sounds that reached the sound engine, after the mute filter (feedback scene). */
    private static int machineSoundsPlayed, playedBefore;

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    static void countMachineSounds(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
        if (OUT == null || event.getSound() == null) return;
        var id = event.getSound().getLocation();
        if (id.getNamespace().equals(Singularity_Iteration.MOD_ID) && id.getPath().startsWith("machine.")) machineSoundsPlayed++;
    }

    @SubscribeEvent
    static void recordChat(net.neoforged.neoforge.client.event.ClientChatReceivedEvent event) {
        if (OUT != null) CHAT.add(event.getMessage().getString());
    }

    private static void note(String line) {
        Singularity_Iteration.LOGGER.info("[feedback] {}", line);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(OUT, "feedback.txt"), line + "\n",
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) { }
    }

    /** Machine sound instances the sound engine is playing now. */
    private static int activeMachineSounds() {
        try {
            var engineField = net.minecraft.client.sounds.SoundManager.class.getDeclaredField("soundEngine");
            engineField.setAccessible(true);
            Object engine = engineField.get(Minecraft.getInstance().getSoundManager());
            var channels = net.minecraft.client.sounds.SoundEngine.class.getDeclaredField("instanceToChannel");
            channels.setAccessible(true);
            int n = 0;
            for (Object o : ((java.util.Map<?, ?>) channels.get(engine)).keySet()) {
                var id = ((net.minecraft.client.resources.sounds.SoundInstance) o).getLocation();
                if (id.getNamespace().equals(Singularity_Iteration.MOD_ID) && id.getPath().startsWith("machine.")) n++;
            }
            return n;
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    private static void soundPhase(String phase, boolean expectSound) {
        int played = machineSoundsPlayed - playedBefore, active = activeMachineSounds();
        boolean ok = expectSound ? active > 0 : active == 0 && played == 0;
        var head = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD);
        note((ok ? "PASS " : "FAIL ") + phase + ": head slot " + (head.isEmpty() ? "empty" : head.getItem()) + ", playing " + active + " machine sounds, " + played + " new machine sounds since the phase began, muted "
            + com.miophas.singularity_iteration.common.client.machine.MachineSoundManager.muted() + "; " + com.miophas.singularity_iteration.common.client.machine.MachineSoundManager.describe());
    }

    /** The job-finished chime from the server, next to the player (what a finishing machine sends). */
    private static void chime(ServerPlayer sp) {
        sp.serverLevel().playSound(null, sp.blockPosition(), com.miophas.singularity_iteration.core.platform.neoforge.CoreSounds.MACHINE_COMPLETE.get(),
            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** Puts {@code stack} into the Curios head slot (reflection: Curios is optional). False without Curios. */
    private static boolean curiosHead(ServerPlayer sp, ItemStack stack) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return false;
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object inventory = ((java.util.Optional<?>) api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, sp)).orElseThrow();
            Object handler = ((java.util.Optional<?>) Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                .getMethod("getStacksHandler", String.class).invoke(inventory, "head")).orElseThrow();
            Object stacks = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler").getMethod("getStacks").invoke(handler);
            ((net.neoforged.neoforge.items.IItemHandlerModifiable) stacks).setStackInSlot(0, stack);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            note("FAIL Curios head slot: " + e);
            return false;
        }
    }

    /** The menu slot holding a Transformer Upgrade, or -1. */
    private static int transformerSlot() {
        var menu = Minecraft.getInstance().player.containerMenu;
        for (int i = 0; i < menu.slots.size(); i++) if (menu.getSlot(i).getItem().is(item("upgrade/transformer_upgrade").getItem())) return i;
        return -1;
    }

    private static void sendSafetyKey(boolean down) {
        int state = down ? 1 << com.miophas.singularity_iteration.common.network.mio_icif_KeyboardManager.KEY_SAFETY : 0;
        com.miophas.singularity_iteration.common.network.mio_icif_KeyboardManager.processKeyUpdate(Minecraft.getInstance().player, state);
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.miophas.singularity_iteration.common.network.KeyboardStatePacket(state));
    }

    /**
     * -Dsi.capture.scene=feedback (0.1.10.15): the in-game pass over the player-feedback fixes that GameTests
     * cannot see. Machine sound muting by the config toggle, by the earmuffs in the helmet slot and (with
     * Curios installed) in the Curios head slot; the chat line when a Transformer Upgrade refuses a click,
     * and the click that works with the Safety Key; the cable icons. Results go to feedback.txt, with
     * screenshots under feedback/.
     */
    private static void feedbackScene() {
        BlockPos fab = new BlockPos(1, -60, 3), gen = new BlockPos(2, -60, 3), furnace = new BlockPos(-1, -60, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            // survival: the inventory screen shows the armor slots (creative opens the creative inventory)
            sp.setGameMode(GameType.SURVIVAL);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 15);
            level.setBlock(fab, block("producer/block_matter_elc").defaultBlockState(), 3);
            level.setBlock(gen, block("generator/block_creative_generator").defaultBlockState(), 3);
            if (level.getBlockEntity(gen) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_creative_generator g) g.configure(100_000L, true);
            level.setBlock(furnace, block("producer/block_furnace_elc").defaultBlockState(), 3);
            if (level.getBlockEntity(furnace) instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity m) {
                var slots = m.getItemHandler();
                ItemStack upgrade = item("upgrade/transformer_upgrade");
                for (int i = 0; i < slots.getSlots(); i++) {
                    if (slots.getStackInSlot(i).isEmpty() && slots.isItemValid(i, upgrade)) { slots.insertItem(i, upgrade, false); break; }
                }
            }
            sp.getInventory().clearContent();
            String[] cables = {"wiring/cable/block_gold_cable", "wiring/cable/block_gold_cable_1", "wiring/cable/block_gold_cable_2",
                "wiring/cable/block_iron_cable", "wiring/cable/block_iron_cable_1", "wiring/cable/block_iron_cable_2", "wiring/cable/block_iron_cable_3"};
            for (int i = 0; i < cables.length; i++) sp.getInventory().setItem(i, item(cables[i]));
            sp.getInventory().setItem(8, item("armor/item_soundproof_earmuffs"));
            sp.getInventory().selected = 7;
        });
        client(5, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            mc.options.showSubtitles().set(true);
            mc.options.guiScale().set(3);
            mc.resizeDisplay();
            com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.MACHINE_SOUNDS.set(true);
            note("Curios installed: " + net.neoforged.fml.ModList.get().isLoaded("curios"));
        });
        // ---- cable icons: gold (HV) bare, 1x, 2x; iron (EV) bare, 1x, 2x, 3x; slot 9 the earmuffs
        shot(40, "feedback/cable_icons_hotbar");
        // ---- machine sounds: a running Mass Fabricator two blocks away
        client(1, () -> playedBefore = machineSoundsPlayed);
        client(100, () -> soundPhase("baseline (machine_sounds on, no earmuffs)", true));
        shot(1, "feedback/sounds_1_baseline");
        client(1, () -> com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.MACHINE_SOUNDS.set(false));
        client(20, () -> playedBefore = machineSoundsPlayed);
        server(1, CaptureDirector::chime);
        client(60, () -> soundPhase("machine_sounds = false (loops and a server chime)", false));
        shot(1, "feedback/sounds_2_toggle_off");
        client(1, () -> com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.MACHINE_SOUNDS.set(true));
        client(1, () -> playedBefore = machineSoundsPlayed);
        client(80, () -> soundPhase("machine_sounds back on", true));
        server(1, sp -> sp.setItemSlot(EquipmentSlot.HEAD, item("armor/item_soundproof_earmuffs")));
        client(20, () -> playedBefore = machineSoundsPlayed);
        server(1, CaptureDirector::chime);
        client(60, () -> soundPhase("earmuffs in the helmet slot (loops and a server chime)", false));
        shot(1, "feedback/sounds_3_earmuffs_helmet");
        client(1, () -> Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(Minecraft.getInstance().player)));
        shot(10, "feedback/earmuffs_helmet_inventory");
        client(1, () -> { Minecraft.getInstance().setScreen(null); Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(10, "feedback/earmuffs_worn_front");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        server(1, sp -> sp.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY));
        client(1, () -> playedBefore = machineSoundsPlayed);
        client(80, () -> soundPhase("earmuffs taken off", true));
        server(1, sp -> { if (!curiosHead(sp, item("armor/item_soundproof_earmuffs"))) note("SKIP Curios head slot: Curios is not installed"); });
        client(20, () -> playedBefore = machineSoundsPlayed);
        server(1, CaptureDirector::chime);
        client(60, () -> {
            if (net.neoforged.fml.ModList.get().isLoaded("curios")) soundPhase("earmuffs in the Curios head slot (loops and a server chime)", false);
        });
        shot(1, "feedback/sounds_4_earmuffs_curios");
        server(1, sp -> curiosHead(sp, ItemStack.EMPTY));
        client(1, () -> playedBefore = machineSoundsPlayed);
        client(80, () -> {
            if (net.neoforged.fml.ModList.get().isLoaded("curios")) soundPhase("earmuffs out of the Curios head slot", true);
        });
        // ---- Transformer Upgrade: a click without the Safety Key is refused with a chat line
        client(1, () -> { showChat = true; CHAT.clear(); });
        server(1, sp -> { if (sp.serverLevel().getBlockEntity(furnace) instanceof net.minecraft.world.MenuProvider p) sp.openMenu(p); });
        client(10, () -> {
            Minecraft mc = Minecraft.getInstance();
            int slot = transformerSlot();
            if (slot < 0) { note("FAIL no Transformer Upgrade slot in " + mc.player.containerMenu.getClass().getSimpleName()); return; }
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
        });
        client(10, () -> {
            var menu = Minecraft.getInstance().player.containerMenu;
            boolean kept = transformerSlot() >= 0 && menu.getCarried().isEmpty();
            boolean told = CHAT.stream().anyMatch(s -> s.contains("Safety Key"));
            note((kept && told ? "PASS " : "FAIL ") + "click without the Safety Key: upgrade kept " + kept + ", chat " + CHAT);
        });
        client(1, () -> Minecraft.getInstance().player.closeContainer());
        shot(5, "feedback/transformer_refused_chat");
        // with the Safety Key held the same click takes it
        client(1, () -> sendSafetyKey(true));
        server(2, sp -> { if (sp.serverLevel().getBlockEntity(furnace) instanceof net.minecraft.world.MenuProvider p) sp.openMenu(p); });
        client(10, () -> {
            Minecraft mc = Minecraft.getInstance();
            int slot = transformerSlot();
            if (slot >= 0) mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
        });
        client(10, () -> {
            var carried = Minecraft.getInstance().player.containerMenu.getCarried();
            boolean taken = carried.is(item("upgrade/transformer_upgrade").getItem());
            note((taken ? "PASS " : "FAIL ") + "click with the Safety Key: carried " + carried);
        });
        client(1, () -> { sendSafetyKey(false); Minecraft.getInstance().player.closeContainer(); showChat = false; });
        client(10, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "holo" (-Dsi.capture.scene=holo), 0.1.11.5: the tactical holomap in three places (open yard, by a wall, inside a
     * roofed bunker with a lava pool and a pit) with creatures above, below, behind a wall and out of range. Shot at the
     * enlarged holomap scale for before/after comparisons.
     */
    private static void sceneHolo() {
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            sp.setGameMode(GameType.SURVIVAL);
            var bricks = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
            var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            // open yard: an L wall and a tower (west), a pit with water (north)
            for (int x = -9; x <= -3; x++) for (int y = -60; y <= -58; y++) level.setBlock(new BlockPos(x, y, -6), bricks, 3);
            for (int z = -6; z <= -1; z++) for (int y = -60; y <= -58; y++) level.setBlock(new BlockPos(-9, y, z), bricks, 3);
            for (int y = -60; y <= -54; y++) for (int x = 6; x <= 8; x++) for (int z = -5; z <= -3; z++) level.setBlock(new BlockPos(x, y, z), bricks, 3);
            for (int x = -2; x <= 2; x++) for (int z = -10; z <= -7; z++) for (int y = -63; y <= -61; y++) level.setBlock(new BlockPos(x, y, z), air, 3);
            level.setBlock(new BlockPos(0, -63, -9), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            // bunker at x 30..44, z -7..7: stone shell, roof at -55, a lava pool and a 3-deep pit inside, a ledge
            for (int x = 30; x <= 44; x++) for (int z = -7; z <= 7; z++) {
                level.setBlock(new BlockPos(x, -55, z), stone, 3);
                for (int y = -60; y <= -56; y++) if (x == 30 || x == 44 || z == -7 || z == 7) level.setBlock(new BlockPos(x, y, z), stone, 3);
            }
            for (int y = -60; y <= -58; y++) level.setBlock(new BlockPos(30, y, 0), air, 3);   // doorway
            for (int x = 39; x <= 41; x++) for (int z = 2; z <= 4; z++) level.setBlock(new BlockPos(x, -61, z), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            for (int x = 35; x <= 37; x++) for (int z = -5; z <= -3; z++) for (int y = -63; y <= -61; y++) level.setBlock(new BlockPos(x, y, z), air, 3);
            for (int x = 41; x <= 43; x++) for (int z = -6; z <= -4; z++) for (int y = -60; y <= -58; y++) level.setBlock(new BlockPos(x, y, z), stone, 3);
            for (int z = -2; z <= 2; z++) for (int y = -60; y <= -57; y++) level.setBlock(new BlockPos(38, y, z), bricks, 3);   // inner wall
            look(sp, 0.5, -60, 0.5, 0, 20);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            suitUp(sp);
            // yard creatures: a husk ahead, a wolf, a cow, a zombie on the tower (above), a skeleton in the pit (below), a far creeper
            mob(level, net.minecraft.world.entity.EntityType.HUSK, 3.5, -60, 7.5, true, sp);
            mob(level, net.minecraft.world.entity.EntityType.WOLF, 5.5, -60, 3.5, true, sp);
            mob(level, net.minecraft.world.entity.EntityType.COW, -5.5, -60, 9.5, true, null);
            mob(level, net.minecraft.world.entity.EntityType.ZOMBIE, 7.5, -53, -3.5, true, sp);
            mob(level, net.minecraft.world.entity.EntityType.SKELETON, 0.5, -63, -8.5, true, sp);
            mob(level, net.minecraft.world.entity.EntityType.CREEPER, 0.5, -60, 19.5, true, sp);
            // bunker creatures: a zombie behind the inner wall, a skeleton in the pit, a spider on the ledge
            mob(level, net.minecraft.world.entity.EntityType.ZOMBIE, 41.5, -60, 0.5, true, null);
            mob(level, net.minecraft.world.entity.EntityType.SKELETON, 36.5, -63, -3.5, true, null);
            mob(level, net.minecraft.world.entity.EntityType.SPIDER, 42.5, -57, -4.5, true, null);
        });
        client(5, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.loaded())
                com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.HOLOMAP_SCALE.set(1.8);
        });
        server(40, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 0, 15));
        shot(30, "holo/1_yard");
        server(2, sp -> sp.teleportTo(sp.serverLevel(), -7.5, -60, -3.5, 300, 10));
        shot(30, "holo/2_wall");
        server(2, sp -> sp.teleportTo(sp.serverLevel(), 33.5, -60, 0.5, 270, 10));
        shot(40, "holo/3_bunker");
        // 0.1.11.5: the detail view (held key), zoomed, then a steeper elevation
        client(1, () -> { com.miophas.singularity_iteration.common.client.suit.FcsClient.forceDetail = true;
            com.miophas.singularity_iteration.common.client.suit.HoloMap.resetDetailView(); });
        shot(10, "holo/4_detail_bunker");
        client(1, () -> { for (int i = 0; i < 3; i++) com.miophas.singularity_iteration.common.client.suit.HoloMap.scroll(1, false); });
        shot(10, "holo/5_detail_zoom");
        client(1, () -> { for (int i = 0; i < 5; i++) com.miophas.singularity_iteration.common.client.suit.HoloMap.scroll(1, true); });
        shot(10, "holo/6_detail_tilt");
        client(1, () -> { com.miophas.singularity_iteration.common.client.suit.FcsClient.forceDetail = false;
            com.miophas.singularity_iteration.common.client.suit.HoloMap.resetDetailView(); });
        // 0.1.11.5: passive: no hostile left, the small map goes away after its linger time
        server(2, sp -> {
            sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 0, 15);
            sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(80),
                m -> m instanceof net.minecraft.world.entity.monster.Enemy).forEach(m -> m.discard());
        });
        shot(120, "holo/7_passive_hidden");
        client(1, () -> { if (com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.loaded())
            com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.HOLOMAP_SCALE.set(1.0); });
        client(10, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "mt" (-Dsi.capture.scene=mt), 0.1.11.6: the Molecular Transformer screen with its observation window. A
     * transformer is fed a fixed number of EU per tick (the buffer is set each tick, the machine spends it all) and shot
     * in each process class: transmutation in three phases, lattice, a continuous molecular run, an anomalous recipe,
     * low power and idle.
     */
    private static void sceneMt() {
        BlockPos at = new BlockPos(0, -60, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(at, block("producer/block_molecular_transformer").defaultBlockState(), 3);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 20);
        });
        server(5, sp -> {
            var level = sp.serverLevel();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
            level.getBlockState(at).useWithoutItem(level, sp, hit);
        });
        mtLoad(at, "minecraft:iron_ingot");
        mtFeed(at, 32768, 30);
        shot(1, "mt/1_nuclear_ionise");
        mtFeed(at, 32768, 95);
        shot(1, "mt/2_nuclear_transmute");
        mtFeed(at, 32768, 125);
        shot(1, "mt/3_nuclear_deposit");
        mtLoad(at, "minecraft:coal");
        mtFeed(at, 32768, 150);
        shot(1, "mt/4_lattice");
        mtLoad(at, "minecraft:netherrack");
        mtFeed(at, 32768, 12);
        shot(1, "mt/5_molecular_continuous");
        mtLoad(at, "minecraft:wither_skeleton_skull");
        mtFeed(at, 1_200_000, 50);
        shot(1, "mt/6_exotic");
        mtLoad(at, "minecraft:iron_ingot");
        mtFeed(at, 32768, 75);
        mtFeed(at, 4096, 60);
        shot(1, "mt/7_low_power");
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_molecular_transformer mt
                && mt.getItemHandler() instanceof net.neoforged.neoforge.items.IItemHandlerModifiable h) h.setStackInSlot(0, ItemStack.EMPTY);
        });
        shot(30, "mt/8_idle");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Puts 64 of {@code id} in the transformer's input and empties the output. */
    private static void mtLoad(BlockPos at, String id) {
        server(2, sp -> {
            if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_molecular_transformer mt
                && mt.getItemHandler() instanceof net.neoforged.neoforge.items.IItemHandlerModifiable h) {
                h.setStackInSlot(0, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), 64));
                h.setStackInSlot(1, ItemStack.EMPTY);
            }
        });
    }

    /** Sets the transformer's buffer to {@code perTick} EU on each of the next {@code ticks} ticks, then logs its state. */
    private static void mtFeed(BlockPos at, long perTick, int ticks) {
        for (int i = 0; i < ticks; i++) {
            server(1, sp -> {
                if (sp.serverLevel().getBlockEntity(at) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(perTick);
            });
        }
        server(0, sp -> {
            if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_molecular_transformer mt) {
                var d = mt.getContainerData();
                Singularity_Iteration.LOGGER.warn("[mt] progress={} working={} recipeEU={} euPerTick={} in={} out={}", d.get(0), d.get(2), d.get(5), d.get(6),
                    mt.getItemHandler().getStackInSlot(0), mt.getItemHandler().getStackInSlot(1));
            }
        });
    }

    /**
     * Scene "pump" (-Dsi.capture.scene=pump), 0.1.11.7: the pump screen. A lava pool deeper in the middle is surveyed and
     * drained (keep level set), then turned into water (an infinite pool), then a second pump discharges lava into a pit.
     */
    private static void scenePump() {
        BlockPos pump = new BlockPos(6, -61, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            for (int z = 4; z <= 12; z++) {
                int depth = 1 + (z >= 6 && z <= 10 ? 1 : 0) + (z >= 7 && z <= 9 ? 1 : 0);
                for (int x = 3; x <= 9; x++) for (int y = -61; y > -61 - depth; y--)
                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            }
            level.setBlock(pump, block("producer/block_pump_elc").defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.SOUTH), 3);
            sp.teleportTo(level, 6.5, -60, -1.5, 0, 30);
        });
        server(5, sp -> openAt(sp, pump));
        pumpFeed(pump, 60);
        shot(1, "pump/1_lava_pool");
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(pump) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pump_elc p)
                for (int i = 0; i < 4; i++) p.handleButton(5);                     // keep 40 buckets
        });
        pumpFeed(pump, 240);
        shot(1, "pump/2_lava_draining");
        server(1, sp -> {
            var level = sp.serverLevel();
            // the same basin, refilled with water (lava left in it would turn to obsidian where the water touches it)
            for (int z = 4; z <= 12; z++) {
                int depth = 1 + (z >= 6 && z <= 10 ? 1 : 0) + (z >= 7 && z <= 9 ? 1 : 0);
                for (int x = 3; x <= 9; x++) for (int y = -61; y > -61 - depth; y--)
                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
            for (int z = 4; z <= 12; z++) {
                int depth = 1 + (z >= 6 && z <= 10 ? 1 : 0) + (z >= 7 && z <= 9 ? 1 : 0);
                for (int x = 3; x <= 9; x++) for (int y = -61; y > -61 - depth; y--)
                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            }
        });
        pumpFeed(pump, 160);
        shot(1, "pump/3_water_infinite");
        // a second pump pours lava into an empty pit (lava does not spread into new sources, so the fill stays visible)
        BlockPos pump2 = new BlockPos(24, -60, 6);
        server(2, sp -> {
            var level = sp.serverLevel();
            sp.closeContainer();
            for (int x = 21; x <= 27; x++) for (int z = 4; z <= 8; z++) for (int y = -61; y >= -63; y--)
                level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pump2, block("producer/block_pump_elc").defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.DOWN), 3);
            if (level.getBlockEntity(pump2) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pump_elc p) {
                p.handleButton(0);
                p.handleButton(8);
                p.getFluidHandler().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 8000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
            sp.teleportTo(level, 24.5, -60, 1.5, 0, 30);
        });
        server(5, sp -> openAt(sp, pump2));
        pumpFeed(pump2, 120);
        shot(1, "pump/4_discharge");
        client(10, () -> Minecraft.getInstance().stop());
    }

    private static void openAt(net.minecraft.server.level.ServerPlayer sp, BlockPos at) {
        var level = sp.serverLevel();
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
        level.getBlockState(at).useWithoutItem(level, sp, hit);
    }

    /** Keeps the pump's small buffer full for {@code ticks} ticks. */
    private static void pumpFeed(BlockPos at, int ticks) {
        for (int i = 0; i < ticks; i++) {
            server(1, sp -> {
                if (sp.serverLevel().getBlockEntity(at) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(20);
                // a pipe that takes the pumped fluid away (the tank never fills up and stalls the pump)
                if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pump_elc p
                        && !p.discharging())
                    p.getFluidHandler().drain(8000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            });
        }
    }

    /**
     * Scene "poly" (-Dsi.capture.scene=poly), 0.1.11.8: the Particle Polymerizer screen. Iron ingots on a chain to neutron
     * plate, shot in each of the three steps (the job progress is set directly, a step takes 75 s), then coal to diamond,
     * then idle.
     */
    private static void scenePoly() {
        BlockPos at = new BlockPos(0, -60, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(at, block("producer/block_neutron_polymerizer").defaultBlockState(), 3);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 20);
            polyInput(sp, at, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 16));
        });
        server(5, sp -> openAt(sp, at));
        server(2, sp -> {
            if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_neutron_polymerizer p) {
                p.cycleTarget();
                p.cycleTarget();                                                    // nano living metal, then neutron plate
            }
        });
        polyFeed(at, 50);
        polyJob(at, 600);
        polyFeed(at, 6);
        shot(1, "poly/1_chain_step1");
        polyJob(at, 1500 + 800);
        polyFeed(at, 6);
        shot(1, "poly/2_chain_step2");
        polyJob(at, 3000 + 1100);
        polyFeed(at, 25);
        shot(1, "poly/3_neutron_plate");
        server(1, sp -> polyInput(sp, at, new ItemStack(net.minecraft.world.item.Items.COAL, 16)));
        polyFeed(at, 4);
        polyJob(at, 900);
        polyFeed(at, 6);
        shot(1, "poly/4_coal_diamond");
        server(1, sp -> polyInput(sp, at, ItemStack.EMPTY));
        polyFeed(at, 20);
        shot(1, "poly/5_idle");
        client(10, () -> Minecraft.getInstance().stop());
    }

    private static void polyInput(net.minecraft.server.level.ServerPlayer sp, BlockPos at, ItemStack stack) {
        if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_neutron_polymerizer p
                && p.getItemHandler() instanceof net.neoforged.neoforge.items.IItemHandlerModifiable h)
            h.setStackInSlot(p.getSlotLayout().getInputSlots()[0], stack);
    }

    /** Sets the job progress (a step takes 1500 ticks; the scene jumps through them). */
    private static void polyJob(BlockPos at, double ticks) {
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_neutron_polymerizer p) {
                try {
                    var f = p.getClass().getDeclaredField("jobProgress");
                    f.setAccessible(true);
                    f.setDouble(p, ticks);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    private static void polyFeed(BlockPos at, int ticks) {
        for (int i = 0; i < ticks; i++) {
            server(1, sp -> {
                if (sp.serverLevel().getBlockEntity(at) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(2_304_000L);
            });
        }
    }

    /**
     * Scene "reactor" (-Dsi.capture.scene=reactor), 0.1.11.21: the control room. A nine-column reactor (six chambers)
     * with quad uranium rods and golden vents, the hull started at 55 % with the trip at 70 %, run for 40 cycles.
     */
    private static void sceneReactor() {
        BlockPos core = new BlockPos(0, -57, 6);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(core, block("generator/block_nuclear_reactor_generator").defaultBlockState(), 3);
            for (Direction d : Direction.values()) level.setBlock(core.relative(d), block("reactor/block_reactor_chamber").defaultBlockState(), 3);
            level.setBlock(core.east(2), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, -57, 2.5, 0, 10);
        });
        server(25, sp -> {
            if (!(sp.serverLevel().getBlockEntity(core) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator r)) return;
            var quad = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_uranium_quad"));
            var vent = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_golden_vent"));
            int[][] rods = {{1, 1}, {1, 4}, {1, 7}, {4, 1}, {4, 4}, {4, 7}};
            for (int[] rc : rods) {
                r.getItemHandler().insertItem(rc[0] * 9 + rc[1], new net.minecraft.world.item.ItemStack(quad), false);
                for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                    int row = rc[0] + d[0], col = rc[1] + d[1];
                    if (row >= 0 && row < 6 && col >= 0 && col < 9) r.getItemHandler().insertItem(row * 9 + col, new net.minecraft.world.item.ItemStack(vent), false);
                }
            }
            r.setScramSetpoint(70);
            r.getHeatStorage().setHeat(5500);
        });
        server(1, sp -> openAt(sp, core));
        server(800, sp -> {});
        shot(1, "reactor/1_control_room");
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(core) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator r) r.scram();
        });
        server(60, sp -> {});
        shot(1, "reactor/2_tripped");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "reactor_card" (-Dsi.capture.scene=reactor_card), 0.1.11.76: the reactor config card. Reactor A (nine columns)
     * gets a layout and a blank card is imported; reactor B gets the card exported (faded parts, a wrong part in red),
     * then the fill; a steam reactor shows the same controls on the fluid screen.
     */
    private static void sceneReactorCard() {
        BlockPos a = new BlockPos(0, -57, 6), b = new BlockPos(5, -57, 6), s = new BlockPos(10, -57, 6);
        ResourceLocation quadId = ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_uranium_quad");
        ResourceLocation ventId = ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_golden_vent");
        int[][] rods = {{1, 1}, {1, 4}, {1, 7}, {4, 1}, {4, 4}, {4, 7}};
        java.util.function.Consumer<ServerPlayer> cardIn = sp -> {
            var m = sp.containerMenu;
            var inv = sp.getInventory();
            for (int i = 0; i < inv.items.size(); i++)
                if (inv.items.get(i).getItem() instanceof com.miophas.singularity_iteration.common.item.reactor.ReactorConfigCardItem) {
                    m.getSlot(m.slots.size() - 1).set(inv.removeItemNoUpdate(i));
                    return;
                }
        };
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            for (BlockPos core : new BlockPos[] {a, b}) {
                level.setBlock(core, block("generator/block_nuclear_reactor_generator").defaultBlockState(), 3);
                for (Direction d : Direction.values()) level.setBlock(core.relative(d), block("reactor/block_reactor_chamber").defaultBlockState(), 3);
            }
            level.setBlock(s, block("generator/block_steam_reactor").defaultBlockState(), 3);
            for (Direction d : Direction.values()) level.setBlock(s.relative(d), block("reactor/block_steam_reactor_chamber").defaultBlockState(), 3);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 2.5, -57, 2.5, 0, 10);
            sp.getInventory().clearContent();
            sp.getInventory().add(new ItemStack(com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactors.REACTOR_CONFIG_CARD.get()));
        });
        server(25, sp -> {
            if (!(sp.serverLevel().getBlockEntity(a) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator r)) return;
            for (int[] rc : rods) {
                r.getItemHandler().insertItem(rc[0] * 9 + rc[1], new ItemStack(BuiltInRegistries.ITEM.get(quadId)), false);
                for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                    int row = rc[0] + d[0], col = rc[1] + d[1];
                    if (row >= 0 && row < 6 && col >= 0 && col < 9) r.getItemHandler().insertItem(row * 9 + col, new ItemStack(BuiltInRegistries.ITEM.get(ventId)), false);
                }
            }
        });
        // 1: import on reactor A (the status line reports it)
        server(1, sp -> openAt(sp, a));
        server(20, sp -> { cardIn.accept(sp); sp.containerMenu.clickMenuButton(sp, 20); });
        client(1, () -> hoverGui = new int[]{17, 248});   // over the import button
        shot(20, "reactor_card/1_import");
        // 2: export on reactor B: faded parts; one real rod, one wrong part (a vent where a rod goes)
        client(1, () -> hoverGui = new int[]{-4000, 4000});
        server(1, ServerPlayer::closeContainer);
        server(5, sp -> {
            if (sp.serverLevel().getBlockEntity(b) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator r) {
                r.getItemHandler().insertItem(13, new ItemStack(BuiltInRegistries.ITEM.get(quadId)), false);
                r.getItemHandler().insertItem(10, new ItemStack(BuiltInRegistries.ITEM.get(ventId)), false);
            }
            openAt(sp, b);
        });
        server(20, sp -> {
            cardIn.accept(sp);
            sp.containerMenu.clickMenuButton(sp, 21);
            sp.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(quadId), 3));
            sp.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ventId), 12));
        });
        client(1, () -> hoverGui = new int[]{14 + 4 * 18 + 8, 20 + 4 * 18 + 8});   // over the faded rod at row 4, column 4
        shot(20, "reactor_card/2_export");
        client(1, () -> hoverGui = new int[]{14 + 18 + 8, 20 + 18 + 8});   // over the wrong part
        shot(5, "reactor_card/2b_wrong_part");
        // 3: fill from the inventory (3 rods, 12 vents: not enough for all)
        client(1, () -> hoverGui = new int[]{53, 248});   // over the fill button
        server(1, sp -> sp.containerMenu.clickMenuButton(sp, 23));
        shot(20, "reactor_card/3_fill");
        // 4: the steam reactor's fluid screen with the card exported
        client(1, () -> hoverGui = new int[]{-4000, 4000});
        server(1, ServerPlayer::closeContainer);
        server(5, sp -> openAt(sp, s));
        server(20, sp -> { cardIn.accept(sp); sp.containerMenu.clickMenuButton(sp, 21); });
        shot(20, "reactor_card/4_steam_export");
        client(1, () -> hoverGui = new int[]{13, 192});   // over the import button of the fluid screen
        shot(5, "reactor_card/5_steam_tooltip");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "fusion_coils" (-Dsi.capture.scene=fusion_coils), 0.1.11.94: the tokamak as laid out, cold: the east column of
     * ports (disruption mitigation, cryogenic, exhaust), a corner toroidal coil's outer leg (closed at y = +-2), the coil faces
     * (narrow edge, copper sides, cross-section on top) and the whole torus.
     */
    private static void sceneFusionCoils() {
        BlockPos c = new BlockPos(30, -55, 10);   // as in "fusion": the torus's centre at (18, -55, 6)
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setWeatherParameters(6000, 0, false, false);
            sp.getAbilities().mayfly = true;
            look(sp, 44.5, -45, 28.5, 130, 16);
        });
        server(20, sp -> {
            com.miophas.singularity_iteration.common.fusion.FusionDemo.placeStructure(sp.serverLevel(), c);
            com.miophas.singularity_iteration.common.fusion.FusionDemo.equip(sp.serverLevel(), c, false);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); hoverGui = null; mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        shot(80, "fusion_coils/1_overview");
        server(1, sp -> look(sp, 35.5, -54, 6.5, 129, 8));
        shot(40, "fusion_coils/2_east_ports");
        server(1, sp -> look(sp, 31.5, -55, -6.5, 45, 0));
        shot(40, "fusion_coils/3_corner_coil");
        server(1, sp -> look(sp, 33.5, -48, -9.5, 40, 35));
        shot(40, "fusion_coils/4_corner_from_above");
        server(1, sp -> look(sp, 18.5, -44, 25.5, 180, 40));
        shot(40, "fusion_coils/5_south_side");
        client(80, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "fusion" (-Dsi.capture.scene=fusion), 0.1.11.93: the tokamak demonstration of the sample world
     * ({@code FusionDemo}): the reactor ready and cold, its instructions, then the lever is pulled and the controller screen is
     * shot in each stage (magnet cooling, solenoid charging, heating, ignition, full power), then the running reactor from
     * outside and the GESU it charges. Each shot logs the controller's values ("[fusioncapture]").
     */
    /**
     * Scene "fusion_meltdown", 0.1.11.137: the demonstration tokamak burning, then its blanket overheated: the default
     * meltdown (the magnets' stored energy as an ordinary blast, a contaminated zone) from outside, before and after.
     */
    /**
     * Scene "fusion_fx", 0.1.11.139: the plasma through the demonstration's east viewports: the breakdown, the plasma growing,
     * the burn, then a disruption (a toroidal field coil block broken) and its collapse; and the viewports from outside.
     */
    /**
     * Scene "fusion_ctm", 0.1.11.140: the connected model (ConnectedModel): 3 x 3 walls of the vacuum vessel, the viewport, the
     * blanket, the toroidal and the poloidal field coil side by side (with a lone block before each), an L of vessel walls and a
     * 2 x 2 x 2 vessel block for the corners; then the tokamak.
     */
    private static void sceneFusionCtm() {
        String[] names = {"vacuum_vessel", "viewport", "blanket", "toroidal_coil", "poloidal_coil"};
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setWeatherParameters(6000, 0, false, false);
            sp.getAbilities().mayfly = true;
            sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            for (int i = 0; i < names.length; i++) {
                var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "fusion/block_fusion_" + names[i]));
                int x0 = -40 + i * 5;
                for (int dx = 0; dx < 3; dx++)
                    for (int dy = 0; dy < 3; dy++) level.setBlock(new BlockPos(x0 + dx, -60 + dy, -30), block.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x0 + 1, -60, -26), block.defaultBlockState(), 3);   // a lone one in front
            }
            var vessel = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "fusion/block_fusion_vacuum_vessel"));
            for (int k = 0; k < 4; k++) { level.setBlock(new BlockPos(-14 + k, -60, -30), vessel.defaultBlockState(), 3); level.setBlock(new BlockPos(-14, -60, -30 + k), vessel.defaultBlockState(), 3); }
            for (int dx = 0; dx < 2; dx++) for (int dy = 0; dy < 2; dy++) for (int dz = 0; dz < 2; dz++)
                level.setBlock(new BlockPos(-8 + dx, -60 + dy, -30 + dz), vessel.defaultBlockState(), 3);
            look(sp, -29, -58.5, -21, 180, 12);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(60, "fusion_ctm/1_walls");
        server(1, sp -> look(sp, -37.5, -58.5, -25.5, 180, 10));
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(30, "fusion_ctm/2_vessel_viewport_close");
        server(1, sp -> look(sp, -26.5, -58.5, -25.5, 180, 10));
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(30, "fusion_ctm/3_coils_close");
        server(1, sp -> look(sp, -9, -57, -24, 200, 30));
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(30, "fusion_ctm/4_corners");
        server(1, sp -> {
            com.miophas.singularity_iteration.common.fusion.FusionDemo.placeStructure(sp.serverLevel(), new BlockPos(30, -55, 10));
            look(sp, 44.5, -45, 28.5, 130, 16);
        });
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(80, "fusion_ctm/5_tokamak");
        server(1, sp -> look(sp, 33.0, -53, 9, 120, 8));
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(30, "fusion_ctm/6_tokamak_close");
        client(40, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "viewport_ctm", 0.1.11.149: the diagnostic viewport's connected texture: a 3 x 3 wall, a ring of 8 (the middle
     * missing), an L of three and a lone viewport, before two rows of vacuum vessel blocks (seen through the glass); from the
     * front, at an angle and close up. Waits for the world (the first server step of a scene is skipped while there is no player).
     */
    private static void sceneViewportCtm() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        until(600, () -> Minecraft.getInstance().player != null && Minecraft.getInstance().getSingleplayerServer() != null);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setWeatherParameters(6000, 0, false, false);
            sp.getAbilities().mayfly = true;
            sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var viewport = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "fusion/block_fusion_viewport")).defaultBlockState();
            var vessel = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "fusion/block_fusion_vacuum_vessel")).defaultBlockState();
            BlockPos o = new BlockPos(0, -60, -20);
            for (int dx = 0; dx < 3; dx++)
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(o.offset(dx, dy, 0), viewport, 3);                                 // the 3 x 3 wall
                    if (dx != 1 || dy != 1) level.setBlock(o.offset(4 + dx, dy, 0), viewport, 3);    // the ring of 8
                }
            for (BlockPos p : new BlockPos[] {o.offset(8, 0, 0), o.offset(9, 0, 0), o.offset(8, 1, 0), o.offset(11, 0, 0)})
                level.setBlock(p, viewport, 3);                                                          // the L and the lone one
            for (int x = -1; x <= 12; x++)
                for (int y = 0; y < 2; y++) level.setBlock(o.offset(x, y, -3), vessel, 3);              // behind the glass
            look(sp, 6.0, -59, -11, 180, 8);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        client(60, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "viewport_ctm/1_front");
        server(1, sp -> look(sp, 14.5, -58, -13, 140, 12));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "viewport_ctm/2_angle");
        server(1, sp -> look(sp, 1.5, -59.6, -16.0, 180, 2));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "viewport_ctm/3_close");
        client(40, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "slot_silhouettes", 0.1.11.151: the empty slots of machines show what goes there, as vanilla's armour slots: each
     * machine placed, its screen opened by a right click (as a player opens it) and shot: the condenser (vents, battery), the
     * canner, the electric furnace, the heat generator (coils), the steam kinetic generator (turbine), the RTG (pellets), the wind
     * kinetic generator (rotor), the fluid heat generator (cells), the miner (drill, scanner) and the fusion fuel injection port.
     */
    private static void sceneSlotSilhouettes() {
        String[][] machines = {
            {"condenser", "producer/block_condenser"},
            {"canner", "producer/block_canner_elc"},
            {"furnace", "producer/block_furnace_elc"},
            {"heat_generator", "hugenerator/block_heat_generator_elc", "heat_generator/block_heat_generator_elc"},
            {"steam_kinetic", "kugenerator/block_steam_kinetic_generator", "kinetic_generator/block_steam_kinetic_generator",
                "producer/block_steam_kinetic_generator", "block_steam_kinetic_generator"},
            {"rtg", "generator/block_rt_generator"},
            {"wind", "kugenerator/block_wind_kinetic_generator", "kinetic_generator/block_wind_kinetic_generator"},
            {"fluid_heat", "hugenerator/block_fluid_heat_generator", "heat_generator/block_fluid_heat_generator"},
            {"miner", "producer/block_miner_elc", "block_miner_elc"},
            {"fusion_fuel_port", "fusion/block_fusion_fuel_port"}};
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        until(600, () -> Minecraft.getInstance().player != null && Minecraft.getInstance().getSingleplayerServer() != null);
        server(40, sp -> {
            sp.serverLevel().setDayTime(6000);
            sp.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, sp.serverLevel().getServer());
            sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        });
        for (int i = 0; i < machines.length; i++) {
            String[] m = machines[i];
            BlockPos at = new BlockPos(3 * i, -60, 6);
            server(5, sp -> {
                var level = sp.serverLevel();
                net.minecraft.world.level.block.Block block = null;
                for (int k = 1; k < m.length && block == null; k++) {
                    var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", m[k]);
                    if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)) block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
                }
                if (block == null) { Singularity_Iteration.LOGGER.warn("Capture: no block for {}", m[0]); return; }
                level.setBlock(at, block.defaultBlockState(), 3);
                look(sp, at.getX() + 0.5, -60, at.getZ() - 2.5, 0, 20);
                var hit = new net.minecraft.world.phys.BlockHitResult(at.getCenter(), net.minecraft.core.Direction.NORTH, at, false);
                level.getBlockState(at).useWithoutItem(level, sp, hit);
            });
            client(25, () -> { CaptureDirector.hoverGui = null; });
            shot(1, "slot_silhouettes/" + (i + 1) + "_" + m[0]);
            client(1, () -> { if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.closeContainer(); });
        }
        client(40, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "solar_models", 0.1.11.154: the eight solar panels with SCXSYGL's models, fronts to the camera, three cells apart;
     * behind them a grid of four advanced panels (left to right) with one cable to an MFSU, and a grid of four METS panels
     * (front to back) with cables under their wings. From the front, from the side, at an angle, the grids, and the screen of a
     * panel in a grid (the shared storage).
     */
    private static void sceneSolarModels() {
        String[] panels = {"block_solar_generator", "block_advanced_solar_panel", "block_hybrid_solar_panel", "block_ultimate_hybrid_solar_panel",
            "block_quantum_solar_panel", "block_mets_advanced_solar_generator", "block_photon_resonance_solar_generator",
            "block_ultimate_photon_resonance_solar_generator"};
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        until(600, () -> Minecraft.getInstance().player != null && Minecraft.getInstance().getSingleplayerServer() != null);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setWeatherParameters(6000, 0, false, false);
            sp.getAbilities().mayfly = true;
            sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            java.util.function.Function<String, net.minecraft.world.level.block.Block> block = path ->
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", path));
            var facing = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
            var south = net.minecraft.core.Direction.SOUTH;
            for (int i = 0; i < panels.length; i++)
                level.setBlock(new BlockPos(3 * i, -60, -20), block.apply("generator/" + panels[i]).defaultBlockState().setValue(facing, south), 3);
            // the advanced grid: left to right, one cable to an MFSU
            for (int i = 0; i < 4; i++)
                level.setBlock(new BlockPos(i, -60, -27), block.apply("generator/block_advanced_solar_panel").defaultBlockState().setValue(facing, south), 3);
            level.setBlock(new BlockPos(4, -60, -27), block.apply("wiring/cable/block_glass_cable").defaultBlockState(), 3);
            level.setBlock(new BlockPos(5, -60, -27), block.apply("wiring/block_mfsu").defaultBlockState(), 3);
            // the METS grid: front to back, cables under the wings on one side
            for (int i = 0; i < 4; i++) {
                level.setBlock(new BlockPos(12, -60, -30 + i), block.apply("generator/block_mets_advanced_solar_generator").defaultBlockState().setValue(facing, south), 3);
                level.setBlock(new BlockPos(13, -60, -30 + i), block.apply("wiring/cable/block_glass_cable").defaultBlockState(), 3);
            }
            look(sp, 10.5, -57.5, -11.5, 180, 14);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        client(60, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "solar_models/1_front");
        server(1, sp -> look(sp, -6.5, -58.0, -19.5, 270, 12));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "solar_models/2_side");
        server(1, sp -> look(sp, 4.5, -57.0, -14.5, 210, 22));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "solar_models/3_angle_left");
        server(1, sp -> look(sp, 18.5, -57.0, -14.5, 150, 22));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "solar_models/4_angle_right");
        server(1, sp -> look(sp, 7.5, -56.5, -21.0, 160, 30));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "solar_models/5_grids");
        server(1, sp -> {
            var level = sp.serverLevel();
            BlockPos at = new BlockPos(1, -60, -27);
            look(sp, 1.5, -60, -24.5, 180, 20);
            var hit = new net.minecraft.world.phys.BlockHitResult(at.getCenter(), net.minecraft.core.Direction.SOUTH, at, false);
            level.getBlockState(at).useWithoutItem(level, sp, hit);
        });
        client(30, () -> { CaptureDirector.hoverGui = null; Minecraft.getInstance().options.hideGui = false; });
        shot(1, "solar_models/6_grid_screen");
        client(40, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "tesu_preview", 0.1.11.157: the TESU (太级储电罐) structure from data/mio_icif/multiblock/tesu.json, built with the
     * placeholder blocks of the sample world (machine hull, reinforced glass, redstone blocks as cells, the GESU core and modules,
     * a lightning rod), and a cutaway with the front-right quarter removed; from the front, at an angle and from above.
     */
    private static void sceneTesuPreview() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        until(600, () -> Minecraft.getInstance().player != null && Minecraft.getInstance().getSingleplayerServer() != null);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setWeatherParameters(6000, 0, false, false);
            sp.getAbilities().mayfly = true;
            sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            java.util.function.Function<String, net.minecraft.world.level.block.state.BlockState> block = id ->
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(id)).defaultBlockState();
            try (var in = CaptureDirector.class.getResourceAsStream("/data/mio_icif/multiblock/tesu.json")) {
                var layout = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                BlockPos full = new BlockPos(0, -60, -30), cut = new BlockPos(12, -60, -30);
                for (var e : layout.getAsJsonArray("blocks")) {
                    var b = e.getAsJsonArray();
                    int x = b.get(0).getAsInt(), y = b.get(1).getAsInt(), z = b.get(2).getAsInt();
                    String role = b.get(3).getAsString();
                    // 0.1.11.157: the real TESU blocks (the sample world still has placeholders)
                    var state = switch (role) {
                        case "casing" -> block.apply("mio_icif:wiring/block_tesu_casing");
                        case "glass" -> block.apply("mio_icif:build/block_alloy_glass");
                        case "cell" -> block.apply("mio_icif:wiring/block_tesu_cell");
                        case "controller" -> block.apply("mio_icif:wiring/block_tesu_controller")
                            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.SOUTH);
                        case "port" -> block.apply(z == 0 || z == 4 ? "mio_icif:wiring/block_tesu_output" : "mio_icif:wiring/block_tesu_input");
                        default -> throw new IllegalStateException("unknown TESU role " + role);   // 0.1.11.166: no electrode
                    };
                    // the front (local z = 0) faces south, towards the camera
                    level.setBlock(full.offset(6 - x, y, 6 - z), state, 3);
                    if (!(x >= 3 && z <= 3)) level.setBlock(cut.offset(6 - x, y, 6 - z), state, 3);
                }
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            look(sp, 9.5, -56, -6.5, 180, 12);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        client(60, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "tesu_preview/1_front");
        server(1, sp -> look(sp, -6.5, -52, -10.5, 220, 22));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "tesu_preview/2_angle");
        server(1, sp -> look(sp, 22.5, -52, -14.5, 130, 25));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "tesu_preview/3_cutaway");
        server(1, sp -> look(sp, 3.5, -38, -21.5, 180, 65));
        client(30, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(1, "tesu_preview/4_above");
        // the controller's screen once the structure has formed (it checks every second)
        server(40, sp -> {
            var level = sp.serverLevel();
            BlockPos at = new BlockPos(3, -59, -24);   // the controller: local (3, 1, 0) turned to face south
            look(sp, 3.5, -60, -21.5, 180, 15);
            level.getBlockState(at).useWithoutItem(level, sp, new net.minecraft.world.phys.BlockHitResult(at.getCenter(), net.minecraft.core.Direction.SOUTH, at, false));
        });
        client(30, () -> { CaptureDirector.hoverGui = null; Minecraft.getInstance().options.hideGui = false; });
        shot(1, "tesu_preview/5_screen");
        client(40, () -> Minecraft.getInstance().stop());
    }

    private static void sceneFusionFx() {
        BlockPos c = new BlockPos(30, -55, 10);
        var ref = new java.util.concurrent.atomic.AtomicReference<com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity>();
        var lever = new java.util.concurrent.atomic.AtomicReference<BlockPos>();
        java.util.function.Function<com.miophas.singularity_iteration.common.fusion.FusionPlasma.State, java.util.function.BooleanSupplier> reached =
            st -> () -> ref.get() != null && ref.get().plasma().state() == st;
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(18000);   // night: the glow reads better
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setWeatherParameters(6000, 0, false, false);
            sp.getAbilities().mayfly = true;
            sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            look(sp, 32.5, -55, 6.5, 90, 0);
        });
        server(20, sp -> {
            var level = sp.serverLevel();
            com.miophas.singularity_iteration.common.fusion.FusionDemo.placeStructure(level, c);
            lever.set(com.miophas.singularity_iteration.common.fusion.FusionDemo.equip(level, c, false));
            if (level.getBlockEntity(c) instanceof com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity f) ref.set(f);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        until(400, () -> ref.get() != null && ref.get().isFormed());
        server(1, sp -> look(sp, 32.5, -55, 6.5, 90, 0));
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(20, "fusion_fx/1_viewports_cold");
        server(1, sp -> com.miophas.singularity_iteration.common.fusion.FusionDemo.lever(sp.serverLevel(), lever.get(), true));
        until(2400, reached.apply(com.miophas.singularity_iteration.common.fusion.FusionPlasma.State.HEATING));
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(4, "fusion_fx/2_breakdown");
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(36, "fusion_fx/3_growing");
        until(1200, reached.apply(com.miophas.singularity_iteration.common.fusion.FusionPlasma.State.BURNING));
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(200, "fusion_fx/4_burning");
        server(1, sp -> look(sp, 44.5, -45, 28.5, 130, 16));
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(30, "fusion_fx/5_outside_night");
        server(1, sp -> look(sp, 32.5, -55, 6.5, 90, 0));
        // a toroidal field coil block broken: the structure goes, the plasma disrupts
        server(20, sp -> {
            var level = sp.serverLevel();
            BlockPos centre = new BlockPos(18, -55, 6);
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-13, -6, -13), centre.offset(13, 6, 13)))
                if (level.getBlockState(p).is(com.miophas.singularity_iteration.common.registry.mio_icif_blocks.FUSION_TOROIDAL_COIL.get()) && p.getX() < 10) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    break;
                }
        });
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(3, "fusion_fx/6_disruption_flare");
        client(1, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
        shot(12, "fusion_fx/7_disruption_collapse");
        client(60, () -> Minecraft.getInstance().stop());
    }

    private static void sceneFusionMeltdown() {
        BlockPos c = new BlockPos(30, -55, 10);
        var ref = new java.util.concurrent.atomic.AtomicReference<com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity>();
        var lever = new java.util.concurrent.atomic.AtomicReference<BlockPos>();
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setWeatherParameters(6000, 0, false, false);
            sp.getAbilities().mayfly = true;
            sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            look(sp, 44.5, -45, 28.5, 130, 16);
        });
        server(20, sp -> {
            var level = sp.serverLevel();
            com.miophas.singularity_iteration.common.fusion.FusionDemo.placeStructure(level, c);
            lever.set(com.miophas.singularity_iteration.common.fusion.FusionDemo.equip(level, c, false));
            if (level.getBlockEntity(c) instanceof com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity f) ref.set(f);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        until(400, () -> ref.get() != null && ref.get().isFormed());
        server(1, sp -> com.miophas.singularity_iteration.common.fusion.FusionDemo.lever(sp.serverLevel(), lever.get(), true));
        until(3000, () -> ref.get() != null && ref.get().plasma().state() == com.miophas.singularity_iteration.common.fusion.FusionPlasma.State.BURNING);
        server(40, sp -> look(sp, 44.5, -45, 28.5, 130, 16));
        shot(10, "fusion_meltdown/1_burning");
        server(1, sp -> {
            ref.get().devOverheat();
            org.slf4j.LoggerFactory.getLogger(CaptureDirector.class).info("[fusioncapture] overheat | hull {}/{} | formed {}", ref.get().hull(), ref.get().maxHull(), ref.get().isFormed());
        });
        server(1, sp -> org.slf4j.LoggerFactory.getLogger(CaptureDirector.class).info("[fusioncapture] next tick | hull {}/{} | removed {} | block {}",
            ref.get().hull(), ref.get().maxHull(), ref.get().isRemoved(), sp.serverLevel().getBlockState(c).getBlock()));
        server(60, sp -> {
            var level = sp.serverLevel();
            BlockPos centre = new BlockPos(18, -55, 6);
            org.slf4j.LoggerFactory.getLogger(CaptureDirector.class).info("[fusioncapture] meltdown | console {} | radiation at centre {} | zone {}",
                level.getBlockState(c).getBlock(), String.format("%.2f",
                    com.miophas.singularity_iteration.common.blockentity.reactor.NukeRadiationZoneManager.getRadiationIntensity(level, centre)),
                com.miophas.singularity_iteration.common.blockentity.reactor.NukeRadiationZoneManager.isInRadiationZone(level, centre.offset(40, 0, 0)));
            look(sp, 44.5, -45, 28.5, 130, 16);
        });
        shot(20, "fusion_meltdown/2_after");
        server(1, sp -> look(sp, 18.5, -40, 6.5, 0, 89));
        shot(30, "fusion_meltdown/3_crater_from_above");
        client(80, () -> Minecraft.getInstance().stop());
    }

    private static void sceneFusion() {
        BlockPos c = new BlockPos(30, -55, 10);   // the structure stands on the grass (y -61) from x 6 to 30, z -6 to 18
        BlockPos gesu = new BlockPos(22, -59, -9);
        var ref = new java.util.concurrent.atomic.AtomicReference<com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity>();
        var lever = new java.util.concurrent.atomic.AtomicReference<BlockPos>();
        java.util.function.Function<com.miophas.singularity_iteration.common.fusion.FusionPlasma.State, java.util.function.BooleanSupplier> reached =
            s -> () -> ref.get() != null && ref.get().plasma().state() == s;
        java.util.function.Consumer<String> log = name -> {
            var f = ref.get();
            if (f == null) return;
            org.slf4j.LoggerFactory.getLogger(CaptureDirector.class).info("[fusioncapture] {} | state {} | {} keV | fusion {} | output {} | stored {} | buffer {} | D {} | T {} | hull {}/{}",
                name, f.plasma().state(), String.format("%.2f", f.plasma().kev()), f.plasma().lastFusion(), f.output(), f.screenValues()[5], f.buffer(),
                f.deuterium(), f.tritium(), f.hull(), f.maxHull());
        };
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setWeatherParameters(6000, 0, false, false);
            sp.getAbilities().mayfly = true;
            look(sp, 44.5, -45, 28.5, 130, 16);
        });
        server(20, sp -> {
            var level = sp.serverLevel();
            com.miophas.singularity_iteration.common.fusion.FusionDemo.placeStructure(level, c);
            lever.set(com.miophas.singularity_iteration.common.fusion.FusionDemo.equip(level, c, true));
            if (level.getBlockEntity(c) instanceof com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity f) ref.set(f);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        until(400, () -> ref.get() != null && ref.get().isFormed());
        shot(80, "fusion/1_overview_cold");
        server(1, sp -> look(sp, 33.0, -59.6, 18.1, 180, 12));
        shot(40, "fusion/2_instructions");
        server(1, sp -> look(sp, 37.5, -53, 17.5, 135, 11));
        shot(30, "fusion/3_controller_lever_supply");
        // the lever is pulled; the controller screen from here on
        server(1, sp -> {
            com.miophas.singularity_iteration.common.fusion.FusionDemo.lever(sp.serverLevel(), lever.get(), true);
            look(sp, 32.5, -55, 10.5, 90, 0);
        });
        server(5, sp -> openAt(sp, c));
        client(1, () -> Minecraft.getInstance().options.hideGui = false);
        until(100, reached.apply(com.miophas.singularity_iteration.common.fusion.FusionPlasma.State.COOLING));
        client(100, () -> log.accept("4_cooling"));
        shot(1, "fusion/4_gui_cooling");
        until(900, reached.apply(com.miophas.singularity_iteration.common.fusion.FusionPlasma.State.CHARGING));
        client(40, () -> log.accept("5_charging"));
        shot(1, "fusion/5_gui_charging");
        until(900, reached.apply(com.miophas.singularity_iteration.common.fusion.FusionPlasma.State.HEATING));
        client(30, () -> log.accept("6_heating"));
        shot(1, "fusion/6_gui_heating");
        until(1200, reached.apply(com.miophas.singularity_iteration.common.fusion.FusionPlasma.State.BURNING));
        client(5, () -> log.accept("7_ignition"));
        shot(1, "fusion/7_gui_ignition");
        until(2400, () -> ref.get() != null && ref.get().output() >= com.miophas.singularity_iteration.common.fusion.FusionPlasma.FULL_POWER);
        client(40, () -> log.accept("8_full_power"));
        shot(1, "fusion/8_gui_full_power");
        // 0.1.11.133: the plasma's power balance over the readouts
        client(1, () -> hoverGui = new int[]{176 + 40, 95 + 20});   // 0.1.11.138: the readout window
        shot(5, "fusion/8b_gui_balance");
        client(1, () -> hoverGui = new int[]{176 + 120, 18 + 36});   // the right-hand plasma
        shot(5, "fusion/8c_gui_figure_tip");
        client(1, () -> hoverGui = new int[]{-4000, 4000});
        // outside, running
        // without a screen the virtual mouse would turn the camera when the game grabs the mouse again: park it
        server(1, ServerPlayer::closeContainer);
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); hoverGui = null; mc.setScreen(null); mc.options.hideGui = true; });
        server(10, sp -> look(sp, 44.5, -45, 28.5, 130, 16));
        server(30, sp -> look(sp, 44.5, -45, 28.5, 130, 16));
        shot(10, "fusion/9_overview_running");
        server(1, sp -> look(sp, 27.5, -56, -13.5, 45, 14));
        server(30, sp -> look(sp, 27.5, -56, -13.5, 45, 14));
        shot(10, "fusion/10_output_gesu");
        server(1, sp -> { look(sp, 24.5, -58.5, -12.5, 30, 10); openAt(sp, gesu); });
        client(1, () -> Minecraft.getInstance().options.hideGui = false);
        client(30, () -> log.accept("11_gesu"));
        shot(1, "fusion/11_gesu_gui");
        server(1, ServerPlayer::closeContainer);
        client(80, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "disguise" (-Dsi.capture.scene=disguise), 0.1.11.24: disguised construction foam and disguised wires, as the
     * chunk mesh now draws them. A row of foam as stone, glass, grass, oak leaves and bricks; foamlogged tin cables
     * disguised as planks and grass, a bare one; behind them a 32 x 12 wall of disguised foam and a layer of disguised
     * cables. Logs the frame rate ("[disguisebench]", no frame cap, no vsync) looking at the wall.
     */
    private static void sceneDisguise() {
        String[] looks = {"stone", "glass", "grass_block", "oak_leaves", "bricks"};
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            var foam = block("build/block_construction_foam");
            var disguised = (net.minecraft.world.level.block.state.properties.BooleanProperty) foam.getStateDefinition().getProperty("disguised");
            for (int i = 0; i < looks.length; i++) disguiseFoam(level, new BlockPos(i * 2, -60, 6), foam, disguised, looks[i]);
            String[] wires = {"oak_planks", "grass_block", null};
            for (int i = 0; i < wires.length; i++) disguiseWire(level, new BlockPos(11 + i * 2, -60, 6), wires[i]);
            for (int x = -12; x < 20; x++) for (int y = 0; y < 12; y++) {
                disguiseFoam(level, new BlockPos(x, -60 + y, 14), foam, disguised, (x + y) % 3 == 0 ? "glass" : "stone");
                disguiseWire(level, new BlockPos(x, -60 + y, 15), "oak_planks");
            }
            look(sp, 4.5, -59, 1.5, 0, 5);
        });
        client(5, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.enableVsync().set(false);
            mc.options.framerateLimit().set(260);
            mc.getWindow().updateVsync(false);
        });
        client(60, () -> Minecraft.getInstance().setScreen(null));
        shot(40, "disguise/1_row");
        for (int k = 0; k < 3; k++) client(40, () -> org.slf4j.LoggerFactory.getLogger(CaptureDirector.class).info("[disguisebench] fps {}", Minecraft.getInstance().getFps()));
        client(10, () -> Minecraft.getInstance().stop());
    }

    private static void disguiseFoam(ServerLevel level, BlockPos at, Block foam, net.minecraft.world.level.block.state.properties.BooleanProperty disguised, String look) {
        level.setBlock(at, foam.defaultBlockState().setValue(disguised, true), 3);
        if (level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.build.mio_icif_block_foam_entity f)
            f.setDisguisedBlockId(ResourceLocation.withDefaultNamespace(look));
    }

    private static void disguiseWire(ServerLevel level, BlockPos at, String look) {
        var state = block("wiring/cable/block_tin_cable_1").defaultBlockState();
        // 0.1.11.33: a disguise needs foam (casing); without a look the cable stays bare
        if (look != null) state = com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire.withCasing(state, com.miophas.singularity_iteration.common.block.wire.WireCasing.DISGUISED_FOAM);
        level.setBlock(at, state, 3);
        if (look != null && level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.mio_icif_wire w)
            w.setDisguisedBlockId(ResourceLocation.withDefaultNamespace(look));
    }

    static {
        if (OUT != null && "disguise".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneDisguise();
        }
    }

    /**
     * Scene "cables" (-Dsi.capture.scene=cables), 0.1.11.25: every wire block as a short run with a branch, in daylight,
     * close and from above; then the cable items and other redrawn items in the survival inventory.
     */
    private static BlockPos superconductorAt;

    private static void sceneCables() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            int i = 0;
            for (Block b : BuiltInRegistries.BLOCK) {
                if (!(b instanceof com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire)) continue;
                int col = i % 6, row = i / 6;
                int bx = col * 4 - 10, bz = 4 + row * 3;
                for (int x = 0; x < 3; x++) level.setBlock(new BlockPos(bx + x, -60, bz), b.defaultBlockState(), 3);
                level.setBlock(new BlockPos(bx + 1, -60, bz + 1), b.defaultBlockState(), 3);
                level.setBlock(new BlockPos(bx + 1, -59, bz), b.defaultBlockState(), 3);
                if (BuiltInRegistries.BLOCK.getKey(b).getPath().equals("wiring/cable/block_superconducting_cable")) superconductorAt = new BlockPos(bx + 1, -60, bz);
                i++;
            }
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, -55.5, 2.0, 0, 55);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        shot(80, "cables/1_overview");
        server(1, sp -> sp.teleportTo(sp.serverLevel(), -6.5, -58.3, 2.6, 0, 35));
        shot(40, "cables/2_close_left");
        server(1, sp -> sp.teleportTo(sp.serverLevel(), 5.5, -58.3, 2.6, 0, 35));
        shot(40, "cables/3_close_right");
        server(1, sp -> { if (superconductorAt != null) sp.teleportTo(sp.serverLevel(), superconductorAt.getX() + 0.5, -58.6, superconductorAt.getZ() - 1.6, 0, 40); });
        shot(40, "cables/5_superconducting");
        // 0.1.11.33: every casing value on an insulated tin cable (the disguised ones look like stone), and a detector
        // cable under wet foam and set
        server(1, sp -> {
            var level = sp.serverLevel();
            var values = com.miophas.singularity_iteration.common.block.wire.WireCasing.values();
            for (int i = 0; i < values.length + 2; i++) {
                boolean detector = i >= values.length;
                var casing = detector ? (i == values.length ? com.miophas.singularity_iteration.common.block.wire.WireCasing.FOAM : com.miophas.singularity_iteration.common.block.wire.WireCasing.CF_WALL) : values[i];
                var at = new BlockPos(-11 + i * 2, -60, -2);
                level.setBlock(at, com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire.withCasing(block(detector ? "wiring/block_eu_detector_cable" : "wiring/cable/block_tin_cable_1").defaultBlockState(), casing), 3);
                if (casing.disguised && level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.mio_icif_wire w)
                    w.setDisguisedBlockId(ResourceLocation.withDefaultNamespace("stone"));
            }
            sp.teleportTo(level, 0.5, -56.5, -13.5, 0, 22);
        });
        shot(40, "cables/6_casings");
        String[][] pages = {
            {"wiring/cable/block_tin_cable", "wiring/cable/block_tin_cable_1", "wiring/cable/block_cable_o", "wiring/cable/block_cable",
             "wiring/cable/block_gold_cable", "wiring/cable/block_gold_cable_1", "wiring/cable/block_gold_cable_2", "wiring/cable/block_iron_cable",
             "wiring/cable/block_iron_cable_1", "wiring/cable/block_iron_cable_2", "wiring/cable/block_iron_cable_3", "wiring/cable/block_glass_cable",
             "wiring/cable/block_superconducting_cable", "wiring/cable/block_eu_detector_cable", "wiring/cable/block_eu_splitter_cable",
             "wiring/cable/block_eu_fuse_cable", "wiring/cable/block_eu_breaker_cable", "build/item_cf_sprayer", "reactor/item_reactor_isotope_rod",
             "item_tool_od_scanner", "item_tool_ov_scanner", "item_tool_meter", "item_tool_wooden_treetap"}};
        client(1, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
        });
        server(1, sp -> {
            sp.getInventory().clearContent();
            int slot = 0;
            for (String id : pages[0]) {
                var stack = item(id);
                if (stack.isEmpty()) continue;
                sp.getInventory().setItem(slot++, stack);
            }
        });
        client(10, () -> Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(Minecraft.getInstance().player)));
        shot(20, "cables/4_inventory");
        // 0.1.11.30: the tooltip of the bare tin cable (hotbar slot 0), then the meter HUD on a powered cable
        client(1, () -> hoverGui = new int[]{8 + 8, 142 + 8});
        shot(10, "cables/7_tooltip");
        client(1, () -> { hoverGui = new int[]{-4000, 4000}; Minecraft.getInstance().setScreen(null); });
        server(1, sp -> {
            var level = sp.serverLevel();
            level.setBlock(new BlockPos(30, -60, 10), block("generator/block_thermal_generator").defaultBlockState(), 3);
            if (level.getBlockEntity(new BlockPos(30, -60, 10)) instanceof com.miophas.singularity_iteration.common.blockentity.mio_icif_Thermal_Generator g)
                ((net.neoforged.neoforge.items.IItemHandlerModifiable) g.getItemHandler()).setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.COAL, 64));
            for (int x = 31; x <= 35; x++) level.setBlock(new BlockPos(x, -60, 10), block("wiring/cable/block_tin_cable").defaultBlockState(), 3);
            place(level, new BlockPos(36, -60, 10), "wiring/block_bat_box", Direction.EAST);   // output away from the cable
            sp.getInventory().setItem(0, item("item_tool_meter"));
            sp.getInventory().selected = 0;
            sp.teleportTo(level, 33.5, -59.0, 8.0, 0, 40);
        });
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.player.getInventory().selected = 0; mc.options.hideGui = false; });
        shot(80, "cables/8_meter_hud");
        // 0.1.11.34: the settings of a new breaker in a tin line: flow not set, voltage following the line (32 EU)
        BlockPos breakerAt = new BlockPos(0, -60, 8);
        server(1, sp -> {
            var level = sp.serverLevel();
            level.setBlock(breakerAt.west(), block("wiring/cable/block_tin_cable_1").defaultBlockState(), 3);
            level.setBlock(breakerAt, block("wiring/block_eu_breaker_cable").defaultBlockState(), 3);
            level.setBlock(breakerAt.east(), block("wiring/cable/block_tin_cable_1").defaultBlockState(), 3);
            sp.teleportTo(level, 0.5, -59, 6.5, 0, 30);
        });
        client(20, () -> com.miophas.singularity_iteration.common.client.screen.ProtectionSettingsScreen.open(breakerAt));
        shot(30, "cables/9_protection");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "cables".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneCables();
        }
    }

    /** Scene "chunkloader" (-Dsi.capture.scene=chunkloader), 0.1.11.35: the chunk loader's map in the Nether (one grey before). */
    private static void sceneChunkLoader() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos at = new BlockPos(8, 70, 8);
        server(40, sp -> {
            var nether = sp.server.getLevel(net.minecraft.world.level.Level.NETHER);
            sp.setGameMode(GameType.CREATIVE);
            sp.teleportTo(nether, at.getX() + 0.5, at.getY() + 2, at.getZ() - 1.5, 0, 40);
        });
        server(60, sp -> {
            var nether = sp.serverLevel();
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) nether.setBlock(at.offset(x, -1, z), net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState(), 3);
            nether.setBlock(at, block("producer/block_chunk_loader").defaultBlockState(), 3);
            if (nether.getBlockEntity(at) instanceof net.minecraft.world.MenuProvider provider) sp.openMenu(provider, buf -> buf.writeBlockPos(at));
        });
        shot(60, "chunkloader/1_nether_map");
        // 0.1.11.38: the overworld map: a 3 x 3 loading, its running ring, two selected chunks waiting over a limit of 9
        BlockPos home = new BlockPos(8, -60, 8);
        client(1, () -> Minecraft.getInstance().setScreen(null));
        server(20, sp -> {
            var level = sp.server.overworld();
            sp.teleportTo(level, home.getX() + 0.5, home.getY() + 2, home.getZ() - 1.5, 0, 40);
        });
        server(40, sp -> {
            var level = sp.serverLevel();
            com.miophas.singularity_iteration.common.energy.ChunkLoaderPolicy.configure(true, 9, 0, 0, -1, 1.0, true, false);
            level.setBlock(home, block("producer/block_chunk_loader").defaultBlockState(), 3);
            if (level.getBlockEntity(home) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader loader) {
                loader.getEnergyStorageInternal().setStored(2500);
                loader.selectArea(-1, -1, 1, 1, true);
                loader.selectArea(3, -3, 3, -2, true);
                sp.openMenu(loader, buf -> buf.writeBlockPos(home));
            }
        });
        shot(60, "chunkloader/2_overworld_map");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "chunkloader".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneChunkLoader();
        }
    }

    /**
     * Scene "wingman" (-Dsi.capture.scene=wingman), 0.1.11.39: the console's Armory page with suit 0 out as a Wingman ("Out"
     * row, the Wingman row and the count against the server limit).
     */
    private static void sceneWingman() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(8, -60, 12);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.teleportTo(level, 2.5, -60, 2.5, 0, 0);
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                ItemStack[][] suits = {
                    {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS),
                        new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY},
                    {new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.GOLDEN_CHESTPLATE), ItemStack.EMPTY, new ItemStack(Items.GOLDEN_BOOTS), ItemStack.EMPTY, ItemStack.EMPTY}};
                for (int st = 0; st < suits.length; st++)
                    for (int c = 0; c < 6; c++)
                        if (!suits[st][c].isEmpty()) inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.COLUMNS[c]), suits[st][c], false);
                armory.rename(sp, 0, "Diamond Mk.II");
                armory.rename(sp, 1, "Parade Gold");
            }
            ItemStack remote = new ItemStack(ArmoryRegistry.REMOTE.get());
            remote.set(ArmoryComponents.TARGET.get(), GlobalPos.of(level.dimension(), armoryAt));
            if (!com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots.write(sp, "curio:charm:0", remote)) sp.getInventory().add(remote);
        });
        server(20, sp -> {
            if (sp.serverLevel().getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                var msg = com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT);
                Singularity_Iteration.LOGGER.info("capture wingman: {}", msg.getString());
            }
        });
        client(40, () -> { Minecraft.getInstance().options.hideGui = false;
            Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features()
                .tab(com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features.TAB_ARMORY)); });
        shot(50, "wingman/1_console_out");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(40, "wingman/2_escort");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingman();
        }
    }

    /**
     * Scene "wingman_ui" (-Dsi.capture.scene=wingman_ui), 0.1.11.40: the squad HUD (W1 / W3 by suit, an order), the command
     * wheel at GUI scale 3 and 4 (it scales to fit), the same in Chinese, and the holo table with its chips.
     */
    private static void sceneWingmanUi() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-6, -60, 10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            look(sp, 0.5, -60, 0.5, 0, 10);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            ItemStack helmet = suitPiece("armor/item_armor_quantum_helmet", "entity_esp");
            com.miophas.singularity_iteration.common.suit.SuitModules.install(helmet,
                com.miophas.singularity_iteration.common.suit.UnitRef.of(com.miophas.singularity_iteration.common.suit.SuitModuleType.HOLOMAP), 2);
            sp.setItemSlot(EquipmentSlot.HEAD, helmet);
            sp.setItemSlot(EquipmentSlot.CHEST, suitPiece("armor/item_armor_quantum_chestplate", "entanglement_relay"));
            sp.setItemSlot(EquipmentSlot.LEGS, suitPiece("armor/item_armor_quantum_leggings"));
            sp.setItemSlot(EquipmentSlot.FEET, suitPiece("armor/item_armor_quantum_boots"));
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.CHEST), new ItemStack(Items.DIAMOND_CHESTPLATE), false);
                inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.MAINHAND), new ItemStack(Items.DIAMOND_SWORD), false);
                inv.insertItem(mio_icif_armory.slotOf(2, ArmoryPiece.CHEST), new ItemStack(Items.IRON_CHESTPLATE), false);
                inv.insertItem(mio_icif_armory.slotOf(2, ArmoryPiece.MAINHAND), new ItemStack(Items.BOW), false);
                com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 2,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.OFFENSIVE);
                com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT);
            }
            mob(level, net.minecraft.world.entity.EntityType.ZOMBIE, 0.5, -60, 14.5, true, sp);
        });
        server(60, sp -> {
            for (var w : com.miophas.singularity_iteration.common.armory.WingmanService.of(sp)) {
                if (w.set() == 2) w.order(com.miophas.singularity_iteration.common.armory.WingmanOrder.HOLD, new net.minecraft.world.phys.Vec3(8.5, -60, 18.5), null);
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.guiScale().set(3); mc.resizeDisplay(); });
        shot(60, "wingman_ui/1_hud_en");
        Runnable wheel = () -> {
            Minecraft mc = Minecraft.getInstance();
            var target = new com.miophas.singularity_iteration.common.client.armory.CommandWheelClient.Target(
                new net.minecraft.world.phys.Vec3(0.5, -60, 14.5), false, -1, null, 14);
            mc.setScreen(new com.miophas.singularity_iteration.common.client.armory.CommandWheelScreen(target));
        };
        client(5, wheel);
        shot(20, "wingman_ui/2_wheel_scale3");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.guiScale().set(4); mc.resizeDisplay(); });
        client(5, wheel);
        shot(20, "wingman_ui/3_wheel_scale4");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.guiScale().set(3); mc.resizeDisplay();
            mc.getLanguageManager().setSelected("zh_cn"); mc.options.languageCode = "zh_cn"; mc.reloadResourcePacks(); });
        shot(120, "wingman_ui/4_hud_zh");
        client(5, wheel);
        shot(20, "wingman_ui/5_wheel_zh");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        client(5, com.miophas.singularity_iteration.common.client.armory.HoloTable::open);
        shot(60, "wingman_ui/6_holo_zh");
        client(2, () -> Minecraft.getInstance().setScreen(null));
        client(5, () -> { Minecraft mc = Minecraft.getInstance();
            mc.getLanguageManager().setSelected("en_us"); mc.options.languageCode = "en_us"; mc.reloadResourcePacks(); });
        client(80, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_ui".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanUi();
        }
    }

    /**
     * Scene "wingman_motion" (-Dsi.capture.scene=wingman_motion), Wingman motion audit: a sword Wingman and a bow Wingman
     * fighting still zombies, seen from the side (frames every 2 ticks), then a hover close-up and a landed one.
     */
    private static void sceneWingmanMotion() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);   // peaceful removes the zombies at once
            sp.setGameMode(GameType.SURVIVAL);
            look(sp, 0.5, -60, -6.5, 0, 8);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                ItemStack[][] suits = {
                    {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS),
                        new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY},
                    {new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
                        new ItemStack(Items.IRON_BOOTS), new ItemStack(Items.BOW), ItemStack.EMPTY},
                    // 0.1.11.46 (M2): a crossbow and a trident
                    {new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.GOLDEN_CHESTPLATE), new ItemStack(Items.GOLDEN_LEGGINGS),
                        new ItemStack(Items.GOLDEN_BOOTS), new ItemStack(Items.CROSSBOW), ItemStack.EMPTY},
                    {new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.NETHERITE_LEGGINGS),
                        new ItemStack(Items.NETHERITE_BOOTS), new ItemStack(Items.TRIDENT), ItemStack.EMPTY}};
                for (int st = 0; st < suits.length; st++)
                    for (int c = 0; c < 6; c++)
                        if (!suits[st][c].isEmpty()) inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.COLUMNS[c]), suits[st][c], false);
                for (int st = 0; st < suits.length; st++)
                    Singularity_Iteration.LOGGER.info("capture motion deploy {}: {}", st, com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, st,
                        com.miophas.singularity_iteration.common.armory.WingmanMode.OFFENSIVE).getString());
            }
            Singularity_Iteration.LOGGER.info("capture motion: player at {} yaw {}", sp.position(), sp.getYRot());
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        client(60, () -> { Minecraft mc = Minecraft.getInstance();
            Singularity_Iteration.LOGGER.info("capture motion client: at {} yaw {} pitch {} camera {} entities {}", mc.player.position(), mc.player.getYRot(),
                mc.player.getXRot(), mc.getCameraEntity(), mc.level.getEntityCount()); });
        shot(1, "wingman_motion/00_hover");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(10, "wingman_motion/01_formation_front");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        server(1, sp -> {
            var level = sp.serverLevel();
            for (double[] at : new double[][]{{-4.5, 8.5}, {5.5, 10.5}}) {
                var z = mob(level, net.minecraft.world.entity.EntityType.ZOMBIE, at[0], -60, at[1], true, sp);
                z.setHealth(z.getMaxHealth());
                z.setInvulnerable(true);
                Singularity_Iteration.LOGGER.info("capture motion zombie at {}", z.position());
            }
        });
        for (int f = 0; f < 50; f++) shot(2, String.format("wingman_motion/fight_%02d", f));
        server(1, sp -> { for (var e : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, sp.getBoundingBox().inflate(40))) e.discard(); });
        shot(120, "wingman_motion/90_landed");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_motion".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanMotion();
        }
    }

    /**
     * Scene "wingman_keyframes" (-Dsi.capture.scene=wingman_keyframes), 0.1.11.225 (Wingman motion A0): one Wingman (diamond
     * suit, sword) lands beside its still owner; a camera stand takes it from its right side and from the front while it
     * stands, looks round and takes two blows. With -PsiWingmanKeyframe=calibrate it holds the AN1 calibration pose (right arm
     * forward, left leg back, head to its own left, 2 pixels up), which checks the GeckoLib bridge's signs.
     */
    private static void sceneWingmanKeyframes() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            look(sp, 0.5, -60, -6.5, 0, 8);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                ItemStack[] suit = {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS),
                    new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY};
                for (int c = 0; c < 6; c++) if (!suit[c].isEmpty()) inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.COLUMNS[c]), suit[c], false);
                Singularity_Iteration.LOGGER.info("capture keyframes deploy: {}", com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT).getString());
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        // the Wingman lands after 40 still ticks of its owner
        server(160, sp -> {
            var w = sp.serverLevel().getEntitiesOfClass(com.miophas.singularity_iteration.common.armory.WingmanEntity.class, sp.getBoundingBox().inflate(16)).stream().findFirst().orElse(null);
            if (w == null) { Singularity_Iteration.LOGGER.warn("capture keyframes: no Wingman"); return; }
            Singularity_Iteration.LOGGER.info("capture keyframes: Wingman at {} landed {} yaw {}", w.position(), w.landed(), w.getYRot());
            double yaw = Math.toRadians(w.getYRot());
            double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -fz, rz = fx;   // its forward and its right
            lightTrailCamera(sp, w.getX() + rx * 3.4, w.getY() + 0.4, w.getZ() + rz * 3.4, w.getX(), w.getY() + 1.0, w.getZ());
        });
        client(3, CaptureDirector::attachCaptureCamera);
        for (int f = 0; f < 6; f++) shot(f == 0 ? 2 : 20, String.format("wingman_keyframes/side_%02d", f));
        server(1, sp -> {
            var w = sp.serverLevel().getEntitiesOfClass(com.miophas.singularity_iteration.common.armory.WingmanEntity.class, sp.getBoundingBox().inflate(16)).stream().findFirst().orElse(null);
            if (w == null) return;
            double yaw = Math.toRadians(w.getYRot());
            double fx = -Math.sin(yaw), fz = Math.cos(yaw);
            lightTrailCamera(sp, w.getX() + fx * 3.6, w.getY() + 0.4, w.getZ() + fz * 3.6, w.getX(), w.getY() + 1.0, w.getZ());
        });
        client(3, CaptureDirector::attachCaptureCamera);
        for (int f = 0; f < 4; f++) shot(f == 0 ? 2 : 20, String.format("wingman_keyframes/front_%02d", f));
        // two light blows (the hit reactions)
        server(1, sp -> sp.serverLevel().getEntitiesOfClass(com.miophas.singularity_iteration.common.armory.WingmanEntity.class, sp.getBoundingBox().inflate(16))
            .forEach(w -> w.hurt(sp.serverLevel().damageSources().generic(), 0.5F)));
        for (int f = 0; f < 6; f++) shot(1, String.format("wingman_keyframes/hit_%02d", f));
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_keyframes".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanKeyframes();
        }
    }

    /**
     * Scene "wingman_cave" (-Dsi.capture.scene=wingman_cave), 0.1.11.228 (cave overhaul G6): a 3-high stone tunnel with a glass
     * side wall, the owner at one end, a sword Wingman, and a zombie (hunting the owner) 16 blocks down the tunnel with a
     * 1-block step between them. A camera outside the glass follows the fight (frames every 3 ticks): the Wingman sets down,
     * skates and runs in on foot, steps up and fights on the floor. Last, the command wheel with the control fan open on the
     * new stance entries.
     */
    private static void sceneWingmanCave() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        int x0 = -2, x1 = 26, z = 6;   // the tunnel runs along x at z = 5..7, floor y = -61, air y = -60 .. -58
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), glass = net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(),
                air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            for (int x = x0 - 1; x <= x1 + 1; x++) for (int y = -61; y <= -57; y++) for (int zz = z - 2; zz <= z + 2; zz++) {
                boolean inside = x >= x0 && x <= x1 && y >= -60 && y <= -58 && zz >= z - 1 && zz <= z + 1;
                level.setBlock(new BlockPos(x, y, zz), inside ? air : zz == z - 2 && y >= -60 && y <= -58 && x >= x0 && x <= x1 ? glass : stone, 3);
            }
            for (int x = 13; x <= x1; x++) for (int zz = z - 1; zz <= z + 1; zz++) level.setBlock(new BlockPos(x, -60, zz), stone, 3);   // the step
            sp.setGameMode(GameType.SURVIVAL);
            sp.teleportTo(level, x0 + 1.5, -60, z + 0.5, -90, 5);
            sp.getAbilities().flying = false;
            sp.getAbilities().invulnerable = true;
            sp.onUpdateAbilities();
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                ItemStack[] suit = {new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
                    new ItemStack(Items.IRON_BOOTS), new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY};
                for (int c = 0; c < 6; c++) if (!suit[c].isEmpty()) inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.COLUMNS[c]), suit[c], false);
                Singularity_Iteration.LOGGER.info("capture cave deploy: {}", com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.OFFENSIVE).getString());
            }
            var w = level.getEntitiesOfClass(com.miophas.singularity_iteration.common.armory.WingmanEntity.class, sp.getBoundingBox().inflate(40)).stream().findFirst().orElse(null);
            if (w != null) w.teleportTo(x0 + 3.5, -60, z + 0.5);   // into the tunnel at once (the deploy places it by the owner)
            lightTrailCamera(sp, 9.5, -59.2, z - 7.5, 9.5, -59.2, z + 0.5);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; });
        client(3, CaptureDirector::attachCaptureCamera);
        shot(40, "wingman_cave/00_set_down");
        server(1, sp -> {
            var zombie = mob(sp.serverLevel(), net.minecraft.world.entity.EntityType.ZOMBIE, 22.5, -59, z + 0.5, true, sp);
            zombie.setTarget(sp);
            zombie.setHealth(zombie.getMaxHealth());
            zombie.setInvulnerable(true);
        });
        for (int f = 0; f < 50; f++) shot(3, String.format("wingman_cave/fight_%02d", f));
        server(1, sp -> {
            for (var e : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, sp.getBoundingBox().inflate(60))) e.discard();
            var w = sp.serverLevel().getEntitiesOfClass(com.miophas.singularity_iteration.common.armory.WingmanEntity.class, sp.getBoundingBox().inflate(60)).stream().findFirst().orElse(null);
            if (w != null) Singularity_Iteration.LOGGER.info("capture cave: Wingman at {} ground {} onGround {} hops {}", w.position(), w.ground(), w.onGround(), w.hops());
        });
        for (int f = 0; f < 10; f++) shot(10, String.format("wingman_cave/back_%02d", f));
        // the command wheel, the control fan open on the stance entries
        client(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            mc.setCameraEntity(mc.player);
            var target = new com.miophas.singularity_iteration.common.client.armory.CommandWheelClient.Target(mc.player.position(), false, -1, null, 8);
            var screen = new com.miophas.singularity_iteration.common.client.armory.CommandWheelScreen(target);
            mc.setScreen(screen);
            try {
                Class<?> k = screen.getClass();
                Class<?> entry = Class.forName(k.getName() + "$Entry");
                Object control = java.util.Arrays.stream(entry.getEnumConstants()).filter(e -> ((Enum<?>) e).name().equals("CONTROL")).findFirst().orElseThrow();
                var hover = k.getDeclaredField("hover"); hover.setAccessible(true); hover.setInt(screen, ((Enum<?>) control).ordinal());
                var fan = k.getDeclaredField("fanOwner"); fan.setAccessible(true); fan.set(screen, control);
                var sub = k.getDeclaredField("sub"); sub.setAccessible(true); sub.setInt(screen, 6);
            } catch (ReflectiveOperationException e) { Singularity_Iteration.LOGGER.warn("capture cave: wheel fan", e); }
        });
        shot(15, "wingman_cave/wheel_stance");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_cave".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanCave();
        }
    }

    /**
     * Scene "wingman_combat" (-Dsi.capture.scene=wingman_combat), 0.1.11.229 (AN1 parts 2 and 3): a sword Wingman fights an
     * invulnerable zombie in the open, seen from the side (frames every 2 ticks: the dive slam, the combo, the finisher); then
     * the owner in a quantum suit plays its suit-up and an order from a camera in front of it (third person).
     */
    private static void sceneWingmanCombat() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            sp.setGameMode(GameType.SURVIVAL);
            look(sp, 0.5, -60, -6.5, 0, 8);
            sp.getAbilities().flying = false;
            sp.getAbilities().invulnerable = true;
            sp.onUpdateAbilities();
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                ItemStack[] suit = {new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
                    new ItemStack(Items.IRON_BOOTS), new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY};
                for (int c = 0; c < 6; c++) if (!suit[c].isEmpty()) inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.COLUMNS[c]), suit[c], false);
                com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0, com.miophas.singularity_iteration.common.armory.WingmanMode.OFFENSIVE);
            }
            lightTrailCamera(sp, 0.5, -59.0, 4.5, 8.5, -59.0, 4.5);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; });
        client(3, CaptureDirector::attachCaptureCamera);
        server(30, sp -> {
            var z = mob(sp.serverLevel(), net.minecraft.world.entity.EntityType.ZOMBIE, 8.5, -60, 9.5, true, sp);
            z.setHealth(z.getMaxHealth());
            z.setInvulnerable(true);
            z.setTarget(sp);
            lightTrailCamera(sp, 8.5, -59.0, 2.0, 8.5, -59.0, 9.5);
        });
        client(2, CaptureDirector::attachCaptureCamera);
        for (int f = 0; f < 70; f++) shot(2, String.format("wingman_combat/fight_%02d", f));
        server(1, sp -> {
            for (var e : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, sp.getBoundingBox().inflate(60))) e.discard();
            sp.setItemSlot(EquipmentSlot.HEAD, item("armor/item_armor_quantum_helmet"));
            sp.setItemSlot(EquipmentSlot.CHEST, item("armor/item_armor_quantum_chestplate"));
            sp.setItemSlot(EquipmentSlot.LEGS, item("armor/item_armor_quantum_leggings"));
            sp.setItemSlot(EquipmentSlot.FEET, item("armor/item_armor_quantum_boots"));
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.setCameraEntity(mc.player); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            com.miophas.singularity_iteration.common.client.suit.SuitKeyframes.mark(mc.player, com.miophas.singularity_iteration.common.client.suit.SuitKeyframes.Kind.SUIT_UP); });
        for (int f = 0; f < 10; f++) shot(3, String.format("wingman_combat/suit_up_%02d", f));
        client(2, () -> com.miophas.singularity_iteration.common.client.suit.SuitKeyframes.mark(Minecraft.getInstance().player,
            com.miophas.singularity_iteration.common.client.suit.SuitKeyframes.Kind.POINT));
        for (int f = 0; f < 4; f++) shot(3, String.format("wingman_combat/point_%02d", f));
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_combat".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanCombat();
        }
    }

    /**
     * Scene "wheel_gear" (-Dsi.capture.scene=wheel_gear), 0.1.11.230 (key consolidation): a player without Wingmen or a relay
     * opens the command wheel (the squad sectors grey), then its gear fan and its favourites fan.
     */
    private static void sceneWheelGear() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> { sp.setGameMode(GameType.SURVIVAL); look(sp, 0.5, -60, 0.5, 0, 10); });
        client(5, () -> {
            Minecraft mc = Minecraft.getInstance();
            var target = new com.miophas.singularity_iteration.common.client.armory.CommandWheelClient.Target(mc.player.position(), false, -1, null, 8);
            mc.setScreen(new com.miophas.singularity_iteration.common.client.armory.CommandWheelScreen(target));
        });
        shot(15, "wheel_gear/0_wheel");
        client(1, () -> { if (Minecraft.getInstance().screen instanceof com.miophas.singularity_iteration.common.client.armory.CommandWheelScreen w) w.previewFan("GEAR"); });
        shot(5, "wheel_gear/1_gear");
        client(1, () -> { if (Minecraft.getInstance().screen instanceof com.miophas.singularity_iteration.common.client.armory.CommandWheelScreen w) w.previewFan("FAVORITES"); });
        shot(5, "wheel_gear/2_favorites");
        client(1, () -> { if (Minecraft.getInstance().screen instanceof com.miophas.singularity_iteration.common.client.armory.CommandWheelScreen w) w.previewFan("SUIT"); });
        shot(5, "wheel_gear/3_suit");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wheel_gear".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWheelGear();
        }
    }

    /** A named SlashBlade blade of this mod (by reflection; empty without SlashBlade or when the blade is not loaded). */
    private static ItemStack namedBlade(ServerLevel level, String name) {
        try {
            Class<?> def = Class.forName("mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition");
            @SuppressWarnings("unchecked")
            var key = (net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Object>>) def.getField("REGISTRY_KEY").get(null);
            Object d = level.registryAccess().registryOrThrow(key).get(ResourceLocation.fromNamespaceAndPath("mio_icif", name));
            if (d == null) {
                Singularity_Iteration.LOGGER.warn("capture sb1: no named blade mio_icif:{}", name);
                return ItemStack.EMPTY;
            }
            return (ItemStack) def.getMethod("getBlade", net.minecraft.core.HolderLookup.Provider.class).invoke(d, level.registryAccess());
        } catch (ReflectiveOperationException | RuntimeException e) {
            Singularity_Iteration.LOGGER.warn("capture sb1: cannot make mio_icif:{}", name, e);
            return ItemStack.EMPTY;
        }
    }

    /**
     * Scene "sb1_blades" (-Dsi.capture.scene=sb1_blades), 0.1.11.232 (art batch SB1): the three collab blades (SlashBlade:
     * Resharped must be in the capture client's mods folder). For each blade in turn: held at the waist from the front and
     * from behind, then a combo from the side (a frame every 2 ticks); then the hotbar and the inventory, and the three
     * blades in item frames on a wall.
     */
    private static void sceneSb1Blades() {
        String[] blades = {"nano_blade", "quantum_blade", "singularity_blade"};
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().invulnerable = true;
            look(sp, 0.5, -60, 0.5, 0, 0);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            for (int i = 0; i < blades.length; i++) {
                ItemStack b = namedBlade(level, blades[i]);
                Singularity_Iteration.LOGGER.info("capture sb1: {} -> {}", blades[i], b);
                sp.getInventory().setItem(i, b);
            }
            // a wall 4 blocks south with the three blades in item frames (seen later from the front)
            for (int x = -2; x <= 2; x++) for (int y = -60; y <= -57; y++)
                level.setBlock(new BlockPos(x, y, 8), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int i = 0; i < blades.length; i++) {
                var frame = new net.minecraft.world.entity.decoration.ItemFrame(level, new BlockPos(i - 1, -59, 7), Direction.NORTH);
                frame.setItem(namedBlade(level, blades[i]));
                level.addFreshEntity(frame);
            }
        });
        for (int i = 0; i < blades.length; i++) {
            final int slot = i;
            final String name = blades[i].replace("_blade", "");
            // the blade alone in slot 0: with another slot selected SlashBlade also draws the slot 0 blade at the waist
            server(5, sp -> {
                for (int k = 0; k < blades.length; k++) sp.getInventory().setItem(k, k == 0 ? namedBlade(sp.serverLevel(), blades[slot]) : ItemStack.EMPTY);
                sp.getInventory().selected = 0;
                look(sp, 0.5, -60, 0.5, 0, 0);
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
            });
            client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.player.getInventory().selected = 0; mc.options.hideGui = true; });
            client(5, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            shot(30, "sb1_blades/" + name + "_0_front");
            client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
            server(1, sp -> look(sp, 0.5, -60, 0.5, -60, 10));
            shot(20, "sb1_blades/" + name + "_1_back");
            // a combo, seen from behind: SlashBlade reads the attack key as held (L_DOWN) and swings on the vanilla click
            server(1, sp -> look(sp, 0.5, -60, 0.5, -30, 5));
            client(5, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
            for (int f = 0; f < 36; f++) {
                final int frame = f;
                client(1, () -> {
                    var key = Minecraft.getInstance().options.keyAttack;
                    if (frame % 9 == 0) {
                        key.setDown(true);
                        net.minecraft.client.KeyMapping.click(key.getKey());
                    } else if (frame % 9 == 3) key.setDown(false);
                });
                shot(1, String.format("sb1_blades/%s_2_swing%02d", name, f));
            }
            client(30, () -> Minecraft.getInstance().options.keyAttack.setDown(false));
        }
        // the hotbar and the inventory
        server(1, sp -> {
            for (int k = 0; k < blades.length; k++) sp.getInventory().setItem(k, namedBlade(sp.serverLevel(), blades[k]));
            look(sp, 0.5, -60, 0.5, 0, 0);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.hideGui = false; });
        shot(20, "sb1_blades/3_hotbar");
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); });
        shot(15, "sb1_blades/4_inventory");
        client(1, () -> Minecraft.getInstance().setScreen(null));
        // the item frames, close up
        server(1, sp -> { look(sp, 0.5, -59.6, 5.3, 0, 0); sp.getAbilities().flying = true; sp.onUpdateAbilities(); });
        client(5, () -> Minecraft.getInstance().options.hideGui = true);
        shot(30, "sb1_blades/5_frames");
        // 0.1.11.233 (B1): the blade card right of the hotbar, for each blade with some resources; the Armory's charge path
        server(1, sp -> {
            var c = com.miophas.singularity_iteration.common.armory.blade.BladeCore.class;
            Object[][] cores = {
                {new com.miophas.singularity_iteration.common.armory.blade.BladeCore("nano_blade", 150_000, 72, false, 0, false)},
                {new com.miophas.singularity_iteration.common.armory.blade.BladeCore("quantum_blade", 300_000, 0, false, 0, false)},
                {new com.miophas.singularity_iteration.common.armory.blade.BladeCore("singularity_blade", 1_200_000, 0, false, 42, false)}};
            for (int k = 0; k < blades.length; k++) {
                ItemStack b = sp.getInventory().getItem(k);
                Singularity_Iteration.LOGGER.info("capture sb1: {} is {}, Armory room before {} EU", blades[k],
                    com.miophas.singularity_iteration.common.armory.blade.CollabBlade.of(b),
                    com.miophas.singularity_iteration.common.armory.ArmoryItemEnergy.room(b));
                com.miophas.singularity_iteration.common.armory.blade.CollabBlade.set(b,
                    (com.miophas.singularity_iteration.common.armory.blade.BladeCore) cores[k][0]);
                Singularity_Iteration.LOGGER.info("capture sb1: {} charged by the Armory path: {} EU", blades[k],
                    com.miophas.singularity_iteration.common.armory.ArmoryItemEnergy.charge(b, 1000, false));
            }
            look(sp, 0.5, -60, 0.5, 0, 10);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        for (int i = 0; i < blades.length; i++) {
            final int slot = i;
            client(5, () -> Minecraft.getInstance().player.getInventory().selected = slot);
            shot(20, "sb1_blades/6_hud_" + blades[i].replace("_blade", ""));
        }
        // 0.1.11.234 (B2): each blade's slash art, released on the server at a still husk 5 blocks ahead (a frame a tick);
        // the log shows the art, its combo and the resources before and after
        server(1, sp -> {
            var level = sp.serverLevel();
            look(sp, 0.5, -60, -3.5, 180, 5);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            // 0.1.11.235 (B3): three still husks, so the collapse cut falls on several targets
            for (double[] at : new double[][]{{0.5, -8.5}, {-3.5, -7.5}, {4.5, -9.5}}) {
                var husk = mob(level, net.minecraft.world.entity.EntityType.HUSK, at[0], -60, at[1], true, sp);
                husk.setInvulnerable(true);
                husk.setNoAi(true);
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); });
        for (int i = 0; i < blades.length; i++) {
            final int slot = i;
            final String name = blades[i].replace("_blade", "");
            server(5, sp -> {
                var level = sp.serverLevel();
                ItemStack b = namedBlade(level, blades[slot]);
                com.miophas.singularity_iteration.common.armory.blade.CollabBlade.set(b, new com.miophas.singularity_iteration.common.armory.blade.BladeCore(
                    blades[slot], 1_000_000, 20, false, 30, false));
                sp.getInventory().setItem(0, b);
                sp.getInventory().selected = 0;
            });
            client(3, () -> Minecraft.getInstance().player.getInventory().selected = 0);
            server(10, sp -> {
                ItemStack b = sp.getMainHandItem();
                var before = com.miophas.singularity_iteration.common.armory.blade.CollabBlade.core(b);
                net.minecraft.world.entity.Entity target = sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    sp.getBoundingBox().inflate(12)).stream().findFirst().orElse(null);
                var combo = com.miophas.singularity_iteration.common.armory.SlashBladeCombat.slashArts(sp, target);
                Singularity_Iteration.LOGGER.info("capture sb1 art: {} -> combo {}, {} husks near, core {} -> {}", blades[slot], combo,
                    sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, sp.getBoundingBox().inflate(12)).size(), before,
                    com.miophas.singularity_iteration.common.armory.blade.CollabBlade.core(sp.getMainHandItem()));
            });
            for (int f = 0; f < 36; f++) {
                // two ticks in, the camera turns 75 degrees: the standing wave along the first look is seen from the side
                if (f == 2 && slot == 0) client(0, () -> { var p = Minecraft.getInstance().player; p.setYRot(p.getYRot() + 75); p.yRotO = p.getYRot(); });
                client(0, () -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); });
                shot(1, String.format("sb1_blades/7_art_%s_%02d", name, f));
            }
            client(20, () -> { var p = Minecraft.getInstance().player; p.setYRot(180); p.yRotO = 180; });
        }
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "sb1_blades".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneSb1Blades();
        }
    }

    /**
     * Scene "wingman_blade" (-Dsi.capture.scene=wingman_blade), 0.1.11.48 (motion audit M3): a Wingman with a SlashBlade:
     * Resharped blade (the mod must be in the capture client's mods folder) attacking a still zombie from the side;
     * 0.1.11.53 (P1): on its own AI (the combo chains), a frame every 2 ticks. Without SlashBlade the scene shows nothing
     * useful.
     */
    private static void sceneWingmanBlade() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(18000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            level.setDayTime(6000);
            sp.setGameMode(GameType.SURVIVAL);
            look(sp, -6.5, -60, 6.5, -90, 6);   // looking east at the fight from the side
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                ItemStack blade = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade_white")));
                Singularity_Iteration.LOGGER.info("capture blade: {}, the Armory takes it: {}", blade,
                    inv.isItemValid(mio_icif_armory.slotOf(0, ArmoryPiece.MAINHAND), blade));
                ItemStack[] suit = {new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
                    new ItemStack(Items.IRON_BOOTS), blade, ItemStack.EMPTY};
                for (int c = 0; c < 6; c++) if (!suit[c].isEmpty()) inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.COLUMNS[c]), suit[c], false);
                Singularity_Iteration.LOGGER.info("capture blade deploy: {}", com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.OFFENSIVE).getString());
            }
        });
        // 0.1.11.49 (F0): the Armory takes the blade as it is (no connector kit): the Wingman deployed from it holds the blade
        server(10, sp -> {
            for (var w : com.miophas.singularity_iteration.common.armory.WingmanService.of(sp))
                Singularity_Iteration.LOGGER.info("capture blade held by the Wingman: {}", w.getMainHandItem());
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        shot(60, "wingman_blade/00_formation");
        // 0.1.11.53 (P1): the Wingman fights a still zombie on its own (the stare, the cleave from above, ground and aerial
        // chains); every click and the combo it led to is logged ("blade ... click ->"); a frame every 2 ticks
        server(1, sp -> {
            var level = sp.serverLevel();
            var z = mob(level, net.minecraft.world.entity.EntityType.ZOMBIE, 2.5, -60, 6.5, true, sp);
            z.setInvulnerable(true);
            for (var w : com.miophas.singularity_iteration.common.armory.WingmanService.of(sp)) {
                w.teleportTo(0.5, -55.5, 6.5);
                w.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            }
            look(sp, 0.5, -60, 0.0, 0, 8);   // looking south at the fight from the side
        });
        for (int f = 0; f < 200; f++) shot(2, String.format("wingman_blade/f%03d", f));
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_blade".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanBlade();
        }
    }

    /**
     * Scene "wingman_escort" (-Dsi.capture.scene=wingman_escort), 0.1.11.51 (escort and open Armory phase F2): four escort
     * Wingmen form up on a hovering owner, who then flies a circle of 64 blocks 20 blocks above the ground, speeding up to
     * 4 blocks per tick (the maneuver flight's top speed) in 60 ticks and flying 600 server ticks (about six laps), at the
     * end looking back over the shoulder (first person); the camera looks back at the
     * owner's face (the V behind). The distances of the Wingmen to the owner are logged every 20 ticks.
     */
    private static void sceneWingmanEscort() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        double cx = 0.5, cz = 0.5, radius = 64, height = -40;
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            look(sp, cx + radius, height, cz, 0, 10);   // at the start of the circle, facing south (+z): the way round
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                net.minecraft.world.item.Item[][] sets = {
                    {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_SWORD},
                    {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.BOW},
                    {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.CROSSBOW},
                    {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.TRIDENT}};
                for (int st = 0; st < sets.length; st++) {
                    for (int c = 0; c < 5; c++) inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.COLUMNS[c]), new ItemStack(sets[st][c]), false);
                    Singularity_Iteration.LOGGER.info("capture escort deploy {}: {}", st, com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, st,
                        com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT).getString());
                }
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        // the squad flies up from the Armory and forms up on the hovering owner
        for (int t = 0; t < 140; t++) server(1, sp -> sp.connection.teleport(cx + radius, height, cz, 0, 10));
        shot(1, "wingman_escort/00_formed");
        // the flight runs on the server tick (a client step can come every other server tick), 600 ticks; at the end the
        // owner looks back over the shoulder while flying on (the formation follows the flight, not the view)
        java.util.concurrent.atomic.AtomicBoolean lookBack = new java.util.concurrent.atomic.AtomicBoolean();
        server(1, sp -> {
            var uuid = sp.getUUID();
            int[] tick = {0};
            double[] angle = {0};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Pre e) -> {
                if (tick[0] >= 600) return;
                ServerPlayer p = e.getServer().getPlayerList().getPlayer(uuid);
                if (p == null) return;
                int t = tick[0]++;
                double speed = Math.min(4.0, (t + 1) * 4.0 / 60);
                angle[0] += speed / radius;
                double a = angle[0];
                // counter-clockwise seen from above: the velocity is (-sin a, cos a), the yaw a
                float yaw = (float) Math.toDegrees(a) + (lookBack.get() ? 150 : 0);
                p.connection.teleport(cx + Math.cos(a) * radius, height, cz + Math.sin(a) * radius, yaw, lookBack.get() ? 5 : 10);
                if (t % 20 == 19) {
                    StringBuilder line = new StringBuilder();
                    for (var w : com.miophas.singularity_iteration.common.armory.WingmanService.of(p))
                        line.append(String.format(java.util.Locale.ROOT, " [%.1f m, %.2f b/t]", w.distanceTo(p), w.getDeltaMovement().length()));
                    Singularity_Iteration.LOGGER.info("capture escort t={} speed {}:{}", t + 1, String.format(java.util.Locale.ROOT, "%.2f", speed), line);
                }
            });
        });
        for (int f = 0; f < 30; f++) shot(5, String.format("wingman_escort/f%02d", f));
        client(1, () -> { lookBack.set(true); Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON); });
        for (int f = 0; f < 10; f++) shot(5, String.format("wingman_escort/back_%02d", f));
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_escort".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanEscort();
        }
    }

    /**
     * Scene "wingman_swap" (-Dsi.capture.scene=wingman_swap), 0.1.11.54 (gaze and Iron Man phase P2): the owner in iron
     * armour calls a Wingman in a diamond suit to swap; seen from behind the owner (the Wingman comes to face them), a
     * frame every 2 ticks until the Wingman stands in the iron suit.
     */
    private static void sceneWingmanSwap() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            look(sp, 0.5, -60, 0.5, 0, 5);
            sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            sp.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            sp.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            sp.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            sp.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                net.minecraft.world.item.Item[] set = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                    Items.DIAMOND_SWORD};
                for (int c = 0; c < 5; c++) inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.COLUMNS[c]), new ItemStack(set[c]), false);
                Singularity_Iteration.LOGGER.info("capture swap deploy: {}", com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT).getString());
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); });
        shot(120, "wingman_swap/00_before");
        server(1, sp -> {
            for (var w : com.miophas.singularity_iteration.common.armory.WingmanService.of(sp))
                Singularity_Iteration.LOGGER.info("capture swap start: {}", w.suitUp(sp));
        });
        for (int f = 0; f < 70; f++) shot(2, String.format("wingman_swap/f%02d", f));
        server(1, sp -> {
            for (var w : com.miophas.singularity_iteration.common.armory.WingmanService.of(sp))
                Singularity_Iteration.LOGGER.info("capture swap after: owner {} {} / Wingman {} {} phase {}", sp.getItemBySlot(EquipmentSlot.CHEST),
                    sp.getMainHandItem(), w.getItemBySlot(EquipmentSlot.CHEST), w.getMainHandItem(), w.swapPhase());
        });
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(40, "wingman_swap/90_after");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_swap".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanSwap();
        }
    }

    /**
     * Scene "wingman_eject" (-Dsi.capture.scene=wingman_eject), 0.1.11.55 (gaze and Iron Man phase P3): the owner in a
     * golden suit with a paired remote sends the suit on: the pieces come off and a Wingman stands up in them in front of
     * the owner; seen from the front, a frame every 2 ticks.
     */
    private static void sceneWingmanEject() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            look(sp, 0.5, -60, 0.5, 0, 5);
            sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            sp.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
            sp.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
            sp.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
            sp.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                ItemStack remote = new ItemStack(ArmoryRegistry.REMOTE.get());
                remote.set(ArmoryComponents.TARGET.get(), GlobalPos.of(level.dimension(), armoryAt));
                sp.getInventory().setItem(8, remote);
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(40, "wingman_eject/00_before");
        server(1, sp -> Singularity_Iteration.LOGGER.info("capture eject: {}",
            com.miophas.singularity_iteration.common.armory.WingmanService.eject(sp).getString()));
        for (int f = 0; f < 40; f++) shot(2, String.format("wingman_eject/f%02d", f));
        server(1, sp -> {
            for (var w : com.miophas.singularity_iteration.common.armory.WingmanService.of(sp))
                Singularity_Iteration.LOGGER.info("capture eject after: owner chest {} / Wingman {} {} mode {}", sp.getItemBySlot(EquipmentSlot.CHEST),
                    w.getItemBySlot(EquipmentSlot.CHEST), w.getMainHandItem(), w.mode());
        });
        shot(20, "wingman_eject/90_after");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_eject".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanEject();
        }
    }

    /**
     * Scene "wingman_rescue" (-Dsi.capture.scene=wingman_rescue), 0.1.11.56 (gaze and Iron Man phase P4): an owner without
     * armour falls from 45 blocks; the escort Wingman (diamond suit) catches them in the air and swaps suits. Seen in third
     * person, a frame every 2 ticks.
     */
    private static void sceneWingmanRescue() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().flying = false;
            sp.getAbilities().mayfly = false;
            sp.onUpdateAbilities();
            look(sp, 0.5, -60, 0.5, 0, 5);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                sp.setItemSlot(slot, ItemStack.EMPTY);
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                net.minecraft.world.item.Item[] set = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                    Items.DIAMOND_SWORD};
                for (int c = 0; c < 5; c++) inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.COLUMNS[c]), new ItemStack(set[c]), false);
                Singularity_Iteration.LOGGER.info("capture rescue deploy: {}", com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT).getString());
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); });
        shot(100, "wingman_rescue/00_formed");
        server(1, sp -> sp.connection.teleport(0.5, -15, 0.5, 0, 30));   // 45 blocks up: the fall starts
        for (int f = 0; f < 60; f++) {
            int frame = f;
            server(0, sp -> {
                if (frame % 5 == 0) Singularity_Iteration.LOGGER.info("capture rescue f{}: y {} fall {} slow {} chest {}", frame,
                    String.format(java.util.Locale.ROOT, "%.1f", sp.getY()), String.format(java.util.Locale.ROOT, "%.1f", sp.fallDistance),
                    sp.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), sp.getItemBySlot(EquipmentSlot.CHEST));
            });
            shot(2, String.format("wingman_rescue/f%02d", f));
        }
        server(1, sp -> Singularity_Iteration.LOGGER.info("capture rescue after: alive {} health {} chest {} on ground {}", sp.isAlive(),
            sp.getHealth(), sp.getItemBySlot(EquipmentSlot.CHEST), sp.onGround()));
        client(40, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_rescue".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanRescue();
        }
    }

    /**
     * Scene "piece_call" (-Dsi.capture.scene=piece_call), 0.1.11.57 (gaze and Iron Man phase P5): the piece wheel, then a
     * helmet called from the Armory flying onto the owner (seen from the front), a frame every 2 ticks.
     */
    private static void scenePieceCall() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-6, -60, 6);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            look(sp, 0.5, -60, 0.5, 0, 5);
            sp.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            sp.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                armory.getItemHandler().insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.HEAD), new ItemStack(Items.DIAMOND_HELMET), false);
                ItemStack remote = new ItemStack(ArmoryRegistry.REMOTE.get());
                remote.set(ArmoryComponents.TARGET.get(), GlobalPos.of(level.dimension(), armoryAt));
                sp.getInventory().setItem(8, remote);
            }
        });
        client(5, () -> Minecraft.getInstance().setScreen(new com.miophas.singularity_iteration.common.client.armory.PieceCallScreen()));
        shot(20, "piece_call/00_wheel");
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(null); mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        server(5, sp -> Singularity_Iteration.LOGGER.info("capture piece call: {}", com.miophas.singularity_iteration.common.armory.ArmoryPieceCall.call(sp,
            sp.serverLevel().getBlockEntity(armoryAt) instanceof mio_icif_armory a ? a : null, ArmoryPiece.HEAD.ordinal()).getString()));
        for (int f = 0; f < 50; f++) shot(2, String.format("piece_call/f%02d", f));
        server(1, sp -> Singularity_Iteration.LOGGER.info("capture piece call after: head {} chest {}", sp.getItemBySlot(EquipmentSlot.HEAD),
            sp.getItemBySlot(EquipmentSlot.CHEST)));
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "piece_call".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scenePieceCall();
        }
    }

    /**
     * Scene "wingman_callouts" (-Dsi.capture.scene=wingman_callouts), 0.1.11.58 (gaze and Iron Man phase P6): a Wingman in
     * a badly damaged suit (sparks, smoke, an unsteady flight) fights two zombies near the owner; the GUI is on, so the
     * callouts (deployed, lock-on, kill, ...) show beside the squad HUD. A frame every 10 ticks.
     */
    private static void sceneWingmanCallouts() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            look(sp, 0.5, -60, -6.5, 0, 8);
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                net.minecraft.world.item.Item[] set = {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_SWORD};
                for (int c = 0; c < 5; c++) {
                    ItemStack stack = new ItemStack(set[c]);
                    if (c == 1) stack.setDamageValue(stack.getMaxDamage() - stack.getMaxDamage() / 5);   // 20 %: sparks and smoke
                    inv.insertItem(mio_icif_armory.slotOf(0, ArmoryPiece.COLUMNS[c]), stack, false);
                }
                Singularity_Iteration.LOGGER.info("capture callouts deploy: {}", com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, 0,
                    com.miophas.singularity_iteration.common.armory.WingmanMode.OFFENSIVE).getString());
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        server(60, sp -> {
            var level = sp.serverLevel();
            mob(level, net.minecraft.world.entity.EntityType.ZOMBIE, -2.5, -60, 6.5, true, sp);
            mob(level, net.minecraft.world.entity.EntityType.ZOMBIE, 3.5, -60, 7.5, true, sp);
        });
        for (int f = 0; f < 24; f++) shot(10, String.format("wingman_callouts/f%02d", f));
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_callouts".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanCallouts();
        }
    }

    /**
     * Scene "clean_slate" (-Dsi.capture.scene=clean_slate), 0.1.11.59 (gaze and Iron Man phase P7): three Wingmen (gold,
     * diamond, netherite) form up; the protocol is asked twice; they climb and burst in their colours. Looking up, a frame
     * every 3 ticks.
     */
    private static void sceneCleanSlate() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(14000);   // night: the bursts show
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            look(sp, 0.5, -60, 0.5, 0, -10);
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                net.minecraft.world.item.Item[][] sets = {
                    {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
                    {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
                    {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}};
                for (int st = 0; st < sets.length; st++) {
                    for (int c = 0; c < 4; c++) inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.COLUMNS[c]), new ItemStack(sets[st][c]), false);
                    com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, st,
                        com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT);
                }
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        shot(120, "clean_slate/00_formed");
        server(1, sp -> {
            Singularity_Iteration.LOGGER.info("capture clean slate 1: {}", com.miophas.singularity_iteration.common.armory.WingmanCleanSlate.request(sp).getString());
            Singularity_Iteration.LOGGER.info("capture clean slate 2: {}", com.miophas.singularity_iteration.common.armory.WingmanCleanSlate.request(sp).getString());
        });
        server(5, sp -> look(sp, sp.getX(), sp.getY(), sp.getZ(), 0, -70));
        for (int f = 0; f < 40; f++) shot(3, String.format("clean_slate/f%02d", f));
        server(1, sp -> {
            var level = sp.serverLevel();
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory)
                Singularity_Iteration.LOGGER.info("capture clean slate after: Wingmen {}, Armory chest pieces {} {} {}",
                    com.miophas.singularity_iteration.common.armory.WingmanService.of(sp).size(), armory.piece(0, ArmoryPiece.CHEST),
                    armory.piece(1, ArmoryPiece.CHEST), armory.piece(2, ArmoryPiece.CHEST));
        });
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "clean_slate".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneCleanSlate();
        }
    }

    /**
     * Scene "wingman_pilot" (-Dsi.capture.scene=wingman_pilot), 0.1.11.60 (gaze and Iron Man phase P8): the owner (quantum
     * chestplate with the entanglement relay) pilots an iron Wingman: the view from its eyes with the pilot line on top, a
     * flight ahead (the forward key held), a look back at the body that stayed, then the way back into the body.
     */
    private static void sceneWingmanPilot() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            look(sp, 0.5, -60, 0.5, 0, 5);
            ItemStack chest = item("armor/item_armor_quantum_chestplate");
            if (chest.getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor armor) armor.setEnergy(chest, armor.getMaxEnergy(chest));
            com.miophas.singularity_iteration.common.suit.SuitModules.install(chest, com.miophas.singularity_iteration.common.suit.SuitModuleType.ENTANGLEMENT_RELAY);
            sp.setItemSlot(EquipmentSlot.CHEST, chest);
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                net.minecraft.world.item.Item[][] sets = {
                    {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_SWORD},
                    {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.BOW}};
                for (int st = 0; st < sets.length; st++) {
                    for (int c = 0; c < 5; c++) inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.COLUMNS[c]), new ItemStack(sets[st][c]), false);
                    com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, st,
                        com.miophas.singularity_iteration.common.armory.WingmanMode.OFFENSIVE);
                }
            }
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        server(100, sp -> {
            var list = com.miophas.singularity_iteration.common.armory.WingmanService.of(sp);
            var w = list.stream().filter(x -> x.getMainHandItem().is(Items.IRON_SWORD)).findFirst().orElse(list.get(0));
            Singularity_Iteration.LOGGER.info("capture pilot start: {} (relay {})", com.miophas.singularity_iteration.common.armory.WingmanPilot.start(sp, w),
                com.miophas.singularity_iteration.common.armory.WingmanCommands.commander(sp));
        });
        shot(20, "wingman_pilot/00_remote_view");
        client(1, () -> net.minecraft.client.KeyMapping.set(Minecraft.getInstance().options.keyUp.getKey(), true));
        for (int f = 0; f < 6; f++) shot(5, String.format("wingman_pilot/f%02d_ahead", f));
        client(1, () -> net.minecraft.client.KeyMapping.set(Minecraft.getInstance().options.keyUp.getKey(), false));
        client(10, () -> { var p = Minecraft.getInstance().player; if (p != null) { p.setYRot(180); p.setXRot(15); } });
        shot(10, "wingman_pilot/10_look_back");
        server(1, sp -> Singularity_Iteration.LOGGER.info("capture pilot during: piloting {}, body at {}",
            com.miophas.singularity_iteration.common.armory.WingmanPilot.piloting(sp), sp.position()));
        client(1, () -> com.miophas.singularity_iteration.common.client.armory.SquadClient.send(
            com.miophas.singularity_iteration.common.armory.WingmanCommands.A_PILOT_EXIT,
            com.miophas.singularity_iteration.common.armory.WingmanCommandPackets.ALL, (byte) 0, null, -1));
        shot(20, "wingman_pilot/20_back_in_body");
        server(1, sp -> Singularity_Iteration.LOGGER.info("capture pilot after: piloting {}",
            com.miophas.singularity_iteration.common.armory.WingmanPilot.piloting(sp)));
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wingman_pilot".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWingmanPilot();
        }
    }

    /**
     * Scene "wheel_fans" (-Dsi.capture.scene=wheel_fans), 0.1.11.65 (hit feel and constructivist FX phase Q4): the command
     * wheel with the squad control fan (modes, follow squad, return) and with the new suit fan (swap, send, pilot, Clean
     * Slate). The owner wears the entanglement relay and has two Wingmen.
     */
    private static void sceneWheelFans() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            sp.setGameMode(GameType.SURVIVAL);
            look(sp, 0.5, -60, 0.5, 0, 5);
            ItemStack chest = item("armor/item_armor_quantum_chestplate");
            if (chest.getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor armor) armor.setEnergy(chest, armor.getMaxEnergy(chest));
            com.miophas.singularity_iteration.common.suit.SuitModules.install(chest, com.miophas.singularity_iteration.common.suit.SuitModuleType.ENTANGLEMENT_RELAY);
            sp.setItemSlot(EquipmentSlot.CHEST, chest);
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                for (int st = 0; st < 2; st++) {
                    inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.CHEST), new ItemStack(Items.IRON_CHESTPLATE), false);
                    inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.HEAD), new ItemStack(Items.IRON_HELMET), false);
                    com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, st,
                        com.miophas.singularity_iteration.common.armory.WingmanMode.OFFENSIVE);
                }
            }
        });
        client(120, () -> {
            Minecraft mc = Minecraft.getInstance();
            var screen = new com.miophas.singularity_iteration.common.client.armory.CommandWheelScreen(
                new com.miophas.singularity_iteration.common.client.armory.CommandWheelClient.Target(mc.player.position(), false, -1, null, 0));
            mc.setScreen(screen);
            screen.previewFan("CONTROL");
        });
        shot(20, "wheel_fans/1_control");
        client(1, () -> { if (Minecraft.getInstance().screen instanceof com.miophas.singularity_iteration.common.client.armory.CommandWheelScreen w) w.previewFan("SUIT"); });
        shot(10, "wheel_fans/2_suit");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "wheel_fans".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWheelFans();
        }
    }

    /**
     * Scene "nozzles" (-Dsi.capture.scene=nozzles), 0.1.11.66 (quantum flight phase R0): the full quantum suit in maneuver
     * flight with each frame core (none, propulsion, graviton): hover from the back and the front, then the cruise from the
     * back; the base suit once more in the classic style.
     */
    private static void sceneNozzles() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
        });
        // 0.1.11.124 (E1): no "base_classic" pass any more (the classic style is archived)
        String[] cores = {null, "PROPULSION", "GRAVITON"};
        String[] names = {"base", "propulsion", "graviton"};
        for (int i = 0; i < cores.length; i++) {
            final String core = cores[i], name = names[i];
            client(1, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(null);   // another scene's timer opened the management screen here
                mc.options.keyUp.setDown(false);
                mc.options.keySprint.setDown(false);
            });
            server(5, sp -> {
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                String[][] suit = {{"HEAD", "armor/item_armor_quantum_helmet"}, {"CHEST", "armor/item_armor_quantum_chestplate"},
                    {"LEGS", "armor/item_armor_quantum_leggings"}, {"FEET", "armor/item_armor_quantum_boots"}};
                for (String[] p : suit) {
                    ItemStack st = item(p[1]);
                    if (st.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(st, b.getMaxEnergy(st));
                    if (p[0].equals("CHEST")) {
                        com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.setEnabled(st, "special_maneuver", true);
                        if (core != null) com.miophas.singularity_iteration.common.suit.core.FrameCores.install(st,
                            com.miophas.singularity_iteration.common.suit.core.FrameCore.valueOf(core));
                    }
                    sp.setItemSlot(EquipmentSlot.valueOf(p[0]), st);
                }
                sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                sp.teleportTo(sp.serverLevel(), 0.5, -50, -20.5, 0, 8);
                sp.getAbilities().mayfly = true;
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            client(3, () -> { Minecraft.getInstance().setScreen(null); Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK); });
            client(29, () -> Minecraft.getInstance().setScreen(null));
            shot(1, "nozzles/" + name + "_1_hover_back");
            client(1, () -> { Minecraft.getInstance().setScreen(null); Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
            client(7, () -> Minecraft.getInstance().setScreen(null));
            shot(1, "nozzles/" + name + "_2_hover_front");
            client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
            server(2, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -40, -60.5, 0, -4));
            client(3, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(null); mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); });
            client(19, () -> Minecraft.getInstance().setScreen(null));
            shot(1, "nozzles/" + name + "_3_cruise");
            client(1, () -> { Minecraft.getInstance().setScreen(null); Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
            client(3, () -> Minecraft.getInstance().setScreen(null));
            shot(1, "nozzles/" + name + "_4_cruise_front");
        }
        client(1, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
        });
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "nozzles".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneNozzles();
        }
    }

    /** 0.1.11.67: puts on the full charged quantum suit, maneuver armed, with a frame core (null: none), flying at a spot. */
    private static void quantumFlyer(net.minecraft.server.level.ServerPlayer sp, String core, double x, double y, double z) {
        sp.getAbilities().flying = false;
        sp.onUpdateAbilities();
        String[][] suit = {{"HEAD", "armor/item_armor_quantum_helmet"}, {"CHEST", "armor/item_armor_quantum_chestplate"},
            {"LEGS", "armor/item_armor_quantum_leggings"}, {"FEET", "armor/item_armor_quantum_boots"}};
        for (String[] p : suit) {
            ItemStack st = item(p[1]);
            if (st.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(st, b.getMaxEnergy(st));
            if (p[0].equals("CHEST")) {
                com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.setEnabled(st, "special_maneuver", true);
                if (core != null) com.miophas.singularity_iteration.common.suit.core.FrameCores.install(st,
                    com.miophas.singularity_iteration.common.suit.core.FrameCore.valueOf(core));
            }
            sp.setItemSlot(EquipmentSlot.valueOf(p[0]), st);
        }
        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        sp.teleportTo(sp.serverLevel(), x, y, z, 0, 8);
        sp.getAbilities().mayfly = true;
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
    }

    /** 0.1.11.67: releases the movement keys and closes any screen (another scene's timer opens the management screen). */
    private static void releaseKeys() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(null);
        for (var k : new net.minecraft.client.KeyMapping[]{mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keySprint})
            k.setDown(false);
    }

    /**
     * Scene "rcs" (-Dsi.capture.scene=rcs), 0.1.11.67 (quantum flight phase R1): the reaction control of the quantum suit.
     * Hover: a turn to the right (the right pod side jets), a strafe to the left (right side jets and forearm), the drift
     * stopped after the forward key is let go (chest jets); cruise: a pull up (crown), the brake after the sprint is let go
     * (crown and retro jets, main thrust cut); the propulsion core turning (booster side jets).
     */
    private static void sceneRcs() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            quantumFlyer(sp, null, 0.5, -50, -20.5);
        });
        client(3, () -> { releaseKeys(); Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK); });
        // hover, turning right
        client(30, CaptureDirector::releaseKeys);
        for (int i = 0; i < 6; i++) client(1, () -> { var pl = Minecraft.getInstance().player; pl.setYRot(pl.getYRot() + 9); });
        shot(1, "rcs/1_hover_yaw_right");
        client(0, () -> { var pl = Minecraft.getInstance().player;   // 0.1.11.127 (E4): which ports fire at the shot
            var nz = com.miophas.singularity_iteration.common.client.suit.NozzleDraw.worn(pl, 0F);
            Singularity_Iteration.LOGGER.info("capture rcs outputs {}", java.util.Arrays.toString(
                com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.nozzleOutput(pl, nz, 0F))); });
        // hover, strafing left
        client(10, () -> { releaseKeys(); Minecraft.getInstance().options.keyLeft.setDown(true); });
        shot(6, "rcs/2_hover_strafe_left");
        // hover, the forward drift stopped
        client(1, () -> { releaseKeys(); Minecraft.getInstance().options.keyUp.setDown(true); });
        client(14, CaptureDirector::releaseKeys);
        shot(2, "rcs/3_hover_drift_brake");
        // cruise, pulling up, then braking
        server(10, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -40, -60.5, 0, 4));
        client(3, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(null); mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); });
        client(30, () -> Minecraft.getInstance().setScreen(null));
        for (int i = 0; i < 5; i++) client(1, () -> { var pl = Minecraft.getInstance().player; pl.setXRot(pl.getXRot() - 5); });
        shot(1, "rcs/4_cruise_pull_up");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));   // 0.1.11.71: the light tunnel
        shot(2, "rcs/4b_cruise_first_person");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        client(15, () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(null); mc.player.setXRot(0); });
        client(1, () -> Minecraft.getInstance().options.keySprint.setDown(false));
        shot(2, "rcs/5_cruise_brake");
        // the propulsion core, hover turning right
        client(1, CaptureDirector::releaseKeys);
        server(20, sp -> quantumFlyer(sp, "PROPULSION", 0.5, -50, -20.5));
        client(30, CaptureDirector::releaseKeys);
        for (int i = 0; i < 6; i++) client(1, () -> { var pl = Minecraft.getInstance().player; pl.setYRot(pl.getYRot() + 9); });
        shot(1, "rcs/6_propulsion_yaw_right");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "rcs".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneRcs();
        }
    }

    /**
     * Scene "impact" (-Dsi.capture.scene=impact), 0.1.11.68 (quantum flight phase R2): the weight of the quantum flight. A
     * dive into the ground (hard landing: cracks and the heavy ring), the take-off burst (staged at the feet), the boost
     * out of the hover (stretched main wedges, wider view), the hard brake (the brake jets together), a cruise into a
     * stone wall (cracks in the wall, the ring out of it). 0.1.11.69: pigs around the touchdown are thrown off by the
     * shockwave.
     */
    private static void sceneImpact() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            for (int x = -8; x <= 8; x++) for (int y = -60; y <= -44; y++) level.setBlock(new BlockPos(x, y, 40), stone, 3);
            // 0.1.11.69 (R3): a ring of pigs around the touchdown, for the shockwave
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4, r = i % 2 == 0 ? 2.2 : 3.6;
                var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
                if (pig == null) continue;
                pig.moveTo(0.5 + Math.cos(a) * r, -60, -20.5 + Math.sin(a) * r, (float) Math.toDegrees(a), 0);
                pig.setPersistenceRequired();   // with AI: a mob without AI takes no push at all
                level.addFreshEntity(pig);
            }
            quantumFlyer(sp, null, 0.5, -36, -20.5);
            Singularity_Iteration.LOGGER.warn("[impact] pigs placed: {}", level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class,
                sp.getBoundingBox().inflate(40)).size());
        });
        // the dive: straight down at full throttle
        client(5, () -> { releaseKeys(); Minecraft mc = Minecraft.getInstance(); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); mc.player.setXRot(90); });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.player.setXRot(90); mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); });
        until(200, () -> com.miophas.singularity_iteration.common.client.suit.FlightImpactClient.trauma() > 0.3F);
        client(1, CaptureDirector::releaseKeys);
        shot(3, "impact/1_hard_landing");
        server(1, sp -> Singularity_Iteration.LOGGER.warn("[impact] pigs after the touchdown at {}: {}", sp.position(), sp.serverLevel().getEntitiesOfClass(
            net.minecraft.world.entity.animal.Pig.class, sp.getBoundingBox().inflate(40)).stream().map(e -> e.position().toString()).toList()));
        client(1, () -> Minecraft.getInstance().player.setXRot(55));
        shot(4, "impact/2_hard_landing_cracks");
        // the take-off burst at the feet (staged: a capture cannot double-tap the jump key)
        server(30, sp -> quantumFlyer(sp, null, 0.5, -59.6, -10.5));
        client(3, () -> { releaseKeys(); var pl = Minecraft.getInstance().player; pl.setXRot(35);
            com.miophas.singularity_iteration.common.client.render.PhaseBursts.ground(pl.position(), 1F, true);
            com.miophas.singularity_iteration.common.client.suit.FlightImpactClient.takeoff(pl); });
        shot(2, "impact/3_takeoff_burst");
        // the boost out of the hover, then the hard brake
        server(30, sp -> quantumFlyer(sp, null, 0.5, -45, -40.5));
        client(30, () -> { releaseKeys(); Minecraft.getInstance().player.setXRot(4); });
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); });
        shot(2, "impact/4_boost");
        client(18, () -> Minecraft.getInstance().setScreen(null));
        client(1, () -> Minecraft.getInstance().options.keySprint.setDown(false));
        shot(2, "impact/5_hard_brake");
        // into the wall at speed
        server(10, sp -> quantumFlyer(sp, null, 0.5, -52, -50.5));
        client(20, () -> { releaseKeys(); Minecraft.getInstance().player.setXRot(0); });
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); });
        until(240, () -> com.miophas.singularity_iteration.common.client.suit.FlightImpactClient.trauma() > 0.5F);
        client(1, CaptureDirector::releaseKeys);
        shot(2, "impact/6_wall_hit");
        // stand back from the wall to see its cracks
        server(1, sp -> sp.teleportTo(sp.serverLevel(), -3.5, -52, 34.5, -25, 5));
        shot(3, "impact/7_wall_hit_cracks");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "impact".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneImpact();
        }
    }

    /**
     * Scene "warp" (-Dsi.capture.scene=warp), 0.1.11.72 (phase warp style phase W2): a cruise from the hover through the sound
     * barrier, from behind: the gather, the flash at the crossing, the warp a little later (streams, afterimages, ripples)
     * and the drop; then once more in first person (the gather and the warp round the camera).
     */
    private static void sceneWarp() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            quantumFlyer(sp, null, 0.5, -40, -200.5);
        });
        java.util.function.Supplier<Float> gather = () -> com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.warpGather(Minecraft.getInstance().player, 1F);
        java.util.function.Supplier<Float> since = () -> com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient.warpSince(Minecraft.getInstance().player, 0F);
        for (String view : new String[]{"back", "first"}) {
            client(20, () -> { releaseKeys(); Minecraft mc = Minecraft.getInstance();
                mc.options.setCameraType(view.equals("back") ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON); mc.player.setXRot(-2); });
            client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); });
            // 0.1.11.73: each shot is timed from the gather and the crossing themselves (fixed waits landed long after the warp)
            until(400, () -> gather.get() > 0.5F || since.get() < 3F);
            client(1, () -> Minecraft.getInstance().setScreen(null));
            shot(1, "warp/" + view + "_1_gather");
            until(200, () -> since.get() < 1.2F);
            shot(1, "warp/" + view + "_2_flash");
            until(60, () -> since.get() >= 9F);
            shot(1, "warp/" + view + "_3_warp");
            until(60, () -> since.get() >= 22F);
            client(1, () -> { if (view.equals("back")) Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT); });   // the afterimages trail behind
            shot(1, "warp/" + view + "_4_afterimages");
            client(1, () -> { if (view.equals("back")) Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK); });
            until(60, () -> since.get() >= 46F);
            shot(1, "warp/" + view + "_5_drop");
            // slow down below the gather again before the next pass
            client(1, CaptureDirector::releaseKeys);
            server(80, sp -> quantumFlyer(sp, null, 0.5, -40, -200.5));
        }
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "warp".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWarp();
        }
    }

    /**
     * Scene "lighttrail" (-Dsi.capture.scene=lighttrail), 0.1.11.122: the light trails of the special flight. The quantum
     * flyer speeds up past the barrier on a straight line south, then a fixed camera (an invisible armour stand, as in the
     * Armory scene) beside the line shoots it flying past: from the side, from ahead and to the side, and at night. Then the
     * flyer and two escort Wingmen fly a circle at 4 blocks per tick past a camera outside the circle. A slow pass shows the
     * faint trail below Mach 0.9.
     */
    private static void sceneLightTrail() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
        });
        java.util.function.DoubleSupplier speed = com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient::speed;
        Runnable go = () -> { Minecraft mc = Minecraft.getInstance(); mc.setScreen(null); mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); };
        // {side offset (x), height offset, ahead (z), name, day time}
        Object[][] passes = {{9.0, 0.5, 34.0, "1_side_day", 6000L}, {6.0, 1.5, 44.0, "2_ahead_day", 6000L}, {9.0, 0.5, 34.0, "3_side_night", 18000L}};
        for (Object[] pass : passes) {
            server(1, sp -> { sp.serverLevel().setDayTime((long) pass[4]); quantumFlyer(sp, null, 0.5, -40, -400.5); });
            client(20, () -> { releaseKeys(); Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true;
                mc.setCameraEntity(mc.player); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); mc.player.setYRot(0); mc.player.setXRot(-2); });
            client(1, go);
            until(400, () -> speed.getAsDouble() > 2.8);
            client(5, go);
            server(1, sp -> lightTrailCamera(sp, sp.getX() + (double) pass[0], sp.getY() + (double) pass[1], sp.getZ() + (double) pass[2], sp.getX(), sp.getY() + 0.8, sp.getZ() + (double) pass[2] - 3));
            client(3, CaptureDirector::attachCaptureCamera);
            until(200, () -> { Minecraft mc = Minecraft.getInstance(); return mc.getCameraEntity() != mc.player && mc.player.getZ() > mc.getCameraEntity().getZ() - 7; });
            for (int f = 0; f < 4; f++) {
                String name = "lighttrail/" + pass[3] + "_" + f;
                shot(f == 0 ? 0 : 2, name);
                client(0, () -> Singularity_Iteration.LOGGER.info("capture lighttrail {}:{}", name, com.miophas.singularity_iteration.common.client.suit.LightTrails.describe()));
            }
            client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.setCameraEntity(mc.player); });
            server(1, CaptureDirector::dropCaptureCameras);
        }
        // a slow pass: Mach 0.6, the trail half way up its ramp
        server(1, sp -> { sp.serverLevel().setDayTime(6000L); quantumFlyer(sp, null, 0.5, -40, -400.5); });
        client(20, () -> { releaseKeys(); Minecraft mc = Minecraft.getInstance(); mc.player.setYRot(0); mc.player.setXRot(-2); });
        client(1, go);
        until(400, () -> speed.getAsDouble() > 2.8);
        client(1, CaptureDirector::releaseKeys);
        until(200, () -> speed.getAsDouble() < 1.5);
        server(1, sp -> lightTrailCamera(sp, sp.getX() + 7, sp.getY() + 0.5, sp.getZ() + 8, sp.getX(), sp.getY() + 0.8, sp.getZ() + 6));
        client(2, CaptureDirector::attachCaptureCamera);
        shot(1, "lighttrail/4_slowing");
        client(0, () -> Singularity_Iteration.LOGGER.info("capture lighttrail {}:{}", "slow " + speed.getAsDouble(), com.miophas.singularity_iteration.common.client.suit.LightTrails.describe()));
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.setCameraEntity(mc.player); });
        server(1, CaptureDirector::dropCaptureCameras);
        // the Wingmen: an escort pair flies a circle with its owner (as in "wingman_escort"), past a camera outside the circle
        client(1, CaptureDirector::releaseKeys);
        BlockPos armoryAt = new BlockPos(-10, -60, -10);
        double cx = 0.5, cz = 0.5, radius = 64, height = -40;
        server(1, sp -> {
            var level = sp.serverLevel();
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) sp.setItemSlot(s, ItemStack.EMPTY);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            look(sp, cx + radius, height, cz, 0, 10);
            place(level, armoryAt, "producer/block_armory", Direction.NORTH);
            if (level.getBlockEntity(armoryAt) instanceof mio_icif_armory armory) {
                armory.getEnergyStorageInternal().setStored(900_000);
                armory.tryBind(sp);
                var inv = armory.getItemHandler();
                net.minecraft.world.item.Item[][] sets = {
                    {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_SWORD},
                    {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.BOW}};
                for (int st = 0; st < sets.length; st++) {
                    for (int c = 0; c < 5; c++) inv.insertItem(mio_icif_armory.slotOf(st, ArmoryPiece.COLUMNS[c]), new ItemStack(sets[st][c]), false);
                    com.miophas.singularity_iteration.common.armory.WingmanService.deploy(sp, armory, st,
                        com.miophas.singularity_iteration.common.armory.WingmanMode.ESCORT);
                }
            }
        });
        for (int t = 0; t < 140; t++) server(1, sp -> sp.connection.teleport(cx + radius, height, cz, 0, 10));
        server(1, sp -> {
            var uuid = sp.getUUID();
            int[] tick = {0};
            double[] angle = {0};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Pre e) -> {
                if (tick[0] >= 400) return;
                ServerPlayer p = e.getServer().getPlayerList().getPlayer(uuid);
                if (p == null) return;
                int t = tick[0]++;
                angle[0] += Math.min(4.0, (t + 1) * 4.0 / 60) / radius;
                double a = angle[0];
                p.connection.teleport(cx + Math.cos(a) * radius, height, cz + Math.sin(a) * radius, (float) Math.toDegrees(a), 10);
            });
        });
        // a camera placed ahead of the first Wingman and out to the side of its path, once the circle is at full speed
        server(120, sp -> {
            var squad = com.miophas.singularity_iteration.common.armory.WingmanService.of(sp);
            if (squad.isEmpty()) return;
            var w = squad.get(0);
            net.minecraft.world.phys.Vec3 at = w.position(), v = at.subtract(w.xo, w.yo, w.zo), ahead = at.add(v.scale(7));
            net.minecraft.world.phys.Vec3 out = new net.minecraft.world.phys.Vec3(ahead.x - cx, 0, ahead.z - cz).normalize();
            lightTrailCamera(sp, ahead.x + out.x * 8, ahead.y + 1.2, ahead.z + out.z * 8, ahead.x, ahead.y + 0.6, ahead.z);
        });
        client(2, CaptureDirector::attachCaptureCamera);
        for (int f = 0; f < 6; f++) {
            String name = String.format("lighttrail/5_wingmen_%02d", f);
            shot(f == 0 ? 2 : 1, name);
            client(0, () -> Singularity_Iteration.LOGGER.info("capture lighttrail {}:{}", name, com.miophas.singularity_iteration.common.client.suit.LightTrails.describe()));
        }
        client(10, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "hover" (-Dsi.capture.scene=hover), 0.1.11.126 (armour and flight overhaul E3): the electromagnetic hover. A
     * quantum flyer hovers 2.5 blocks over the ground; a fixed camera (an invisible armour stand) shoots it from the side and
     * from below-front, twice each a quarter of a beat apart (the ripples move), by day and at night, then with a graviton
     * frame (its own field rings at the soles).
     */
    private static void sceneHover() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
        });
        Object[][] passes = {{"1_day", 6000L, null}, {"2_night", 18000L, null}, {"3_graviton", 6000L, "GRAVITON"}};
        for (Object[] pass : passes) {
            server(1, sp -> {
                sp.serverLevel().setDayTime((long) pass[1]);
                quantumFlyer(sp, (String) pass[2], 0.5, -57.5, 0.5);
            });
            client(30, () -> { releaseKeys(); Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.setCameraEntity(mc.player); });
            server(1, sp -> sp.teleportTo(sp.serverLevel(), 0.5, -57.5, 0.5, 0, 8));   // the take-off of the first pass climbs
            server(10, sp -> lightTrailCamera(sp, sp.getX() + 4.2, sp.getY() + 0.4, sp.getZ() + 1.0, sp.getX(), sp.getY() + 0.2, sp.getZ()));
            client(3, CaptureDirector::attachCaptureCamera);
            for (int f = 0; f < 2; f++) shot(f == 0 ? 10 : 8, "hover/" + pass[0] + "_side_" + f);
            client(0, () -> { Minecraft mc = Minecraft.getInstance(); Singularity_Iteration.LOGGER.info("capture hover {}: weight {}, y {}", pass[0],
                com.miophas.singularity_iteration.common.client.suit.QuantumWornMounts.hoverWeight(mc.player, 1F), mc.player.getY()); });
            server(1, sp -> lightTrailCamera(sp, sp.getX() + 1.5, sp.getY() - 1.2, sp.getZ() + 3.2, sp.getX(), sp.getY() - 0.6, sp.getZ()));
            client(3, CaptureDirector::attachCaptureCamera);
            for (int f = 0; f < 2; f++) shot(f == 0 ? 6 : 8, "hover/" + pass[0] + "_low_" + f);
            client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.setCameraEntity(mc.player); });
            server(1, CaptureDirector::dropCaptureCameras);
        }
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "hover".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneHover();
        }
    }

    /** 0.1.11.122: an invisible armour stand at (x, y, z) looking at (tx, ty, tz), for {@link #attachCaptureCamera}. */
    private static void lightTrailCamera(ServerPlayer sp, double x, double y, double z, double tx, double ty, double tz) {
        dropCaptureCameras(sp);
        var level = sp.serverLevel();
        var cam = new net.minecraft.world.entity.decoration.ArmorStand(level, x, y, z);
        cam.setInvisible(true);
        cam.setNoGravity(true);
        cam.addTag("si_capture_camera");
        cam.setCustomName(net.minecraft.network.chat.Component.literal("si_capture_camera"));
        cam.setCustomNameVisible(false);
        double dx = tx - x, dz = tz - z, dy = ty - cam.getEyeY();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        cam.moveTo(x, y, z, yaw, pitch);
        cam.setYHeadRot(yaw);
        level.addFreshEntity(cam);
    }

    private static void attachCaptureCamera() {
        Minecraft mc = Minecraft.getInstance();
        for (var e : mc.level.entitiesForRendering())
            if (e.getCustomName() != null && e.getCustomName().getString().equals("si_capture_camera")) mc.setCameraEntity(e);
    }

    private static void dropCaptureCameras(ServerPlayer sp) {
        sp.serverLevel().getEntities((net.minecraft.world.entity.Entity) null, sp.getBoundingBox().inflate(200),
            e -> e.getTags().contains("si_capture_camera")).forEach(net.minecraft.world.entity.Entity::discard);
    }

    static {
        if (OUT != null && "lighttrail".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneLightTrail();
        }
    }

    /** Scene "resin" (-Dsi.capture.scene=resin), 0.1.11.26: the resin collector in its four facings, close, and as an item. */
    private static void sceneResin() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            sp.setGameMode(GameType.SURVIVAL);
            Direction[] faces = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (int i = 0; i < 4; i++) place(level, new BlockPos(i * 2 - 3, -60, 4), "producer/block_resin_collector", faces[i]);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.0, -58.4, 1.6, 0, 30);
            sp.getInventory().clearContent();
            sp.getInventory().setItem(0, item("producer/block_resin_collector"));
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        shot(80, "resin/1_facings");
        server(1, sp -> sp.teleportTo(sp.serverLevel(), -2.4, -59.2, 2.7, 0, 25));
        shot(30, "resin/2_close");
        server(1, sp -> sp.teleportTo(sp.serverLevel(), -0.6, -59.0, 6.4, 180, 30));
        shot(30, "resin/3_back");
        client(1, () -> Minecraft.getInstance().options.hideGui = false);
        client(10, () -> Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(Minecraft.getInstance().player)));
        shot(20, "resin/4_item");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "resin".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneResin();
        }
    }

    /**
     * Scene "suits" (-Dsi.capture.scene=suits), 0.1.11.27: the nano and quantum suits on armour stands: plain, with a
     * palette (crimson, cobalt), and with frame cores (solar arrays, graviton, propulsion); front, back, three-quarter and
     * night; then the player in the quantum suit, third person and first person (the hand).
     */
    private static void sceneSuits() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        // suit, palette (primary, accent; -1 none), head core, chest core
        Object[][] stands = {
            {"nano", -1, -1, null, null}, {"quantum", -1, -1, null, null},
            {"nano", 0xC8323C, 0xFFD24A, null, null}, {"quantum", 0x2F5FD0, 0xE8F4FF, null, null},
            {"nano", -1, -1, "solar_array_2", null}, {"quantum", -1, -1, "solar_array_3", "graviton"},
            {"quantum", 0x3A3F46, 0x37E35A, "solar_array_1", "propulsion"}};
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            for (int i = 0; i < stands.length; i++) {
                Object[] d = stands[i];
                double x = i * 1.6 - 4.8, z = 5.5;
                var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, x, -60, z);
                stand.moveTo(x, -60, z, 180, 0);
                stand.setYHeadRot(180); stand.setYBodyRot(180);
                for (int k = 0; k < 4; k++) {
                    ItemStack st = item("armor/item_armor_" + d[0] + "_" + pieces[k]);
                    if ((int) d[1] >= 0) st.set(com.miophas.singularity_iteration.common.registry.mio_icif_data_components.SUIT_PALETTE.get(),
                        new com.miophas.singularity_iteration.common.suit.SuitPalette((int) d[1], (int) d[2]));
                    if (k == 0 && d[3] != null) st.set(com.miophas.singularity_iteration.common.registry.mio_icif_data_components.SUIT_CORE.get(), (String) d[3]);
                    if (k == 1 && d[4] != null) st.set(com.miophas.singularity_iteration.common.registry.mio_icif_data_components.SUIT_CORE.get(), (String) d[4]);
                    if (st.getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor armor) armor.setEnergy(st, Long.MAX_VALUE / 4);
                    stand.setItemSlot(slots[k], st);
                }
                try { var m = net.minecraft.world.entity.decoration.ArmorStand.class.getDeclaredMethod("setShowArms", boolean.class); m.setAccessible(true); m.invoke(stand, true); } catch (ReflectiveOperationException ignored) { }
                level.addFreshEntity(stand);
            }
            look(sp, 0.0, -59.0, 1.0, 0, 8);
        });
        client(5, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.FIRST_PERSON); });
        shot(80, "suits/1_front_day");
        server(1, sp -> look(sp, -3.2, -59.2, 3.2, -20, 5));
        shot(30, "suits/2_close_plain");
        server(1, sp -> look(sp, 3.2, -59.2, 3.2, 20, 5));
        shot(30, "suits/3_close_cores");
        server(1, sp -> look(sp, 0.0, -59.0, 10.0, 180, 8));
        shot(30, "suits/4_back_day");
        server(1, sp -> look(sp, -6.5, -58.8, 2.0, -45, 12));
        shot(30, "suits/5_three_quarter");
        server(1, sp -> { sp.serverLevel().setDayTime(18000); look(sp, 0.0, -59.0, 1.0, 0, 8); });
        shot(40, "suits/6_front_night");
        server(1, sp -> {
            sp.serverLevel().setDayTime(6000);
            for (int k = 0; k < 4; k++) {
                ItemStack st = item("armor/item_armor_quantum_" + pieces[k]);
                if (st.getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor armor) armor.setEnergy(st, Long.MAX_VALUE / 4);
                sp.setItemSlot(slots[k], st);
            }
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            sp.teleportTo(sp.serverLevel(), 0.5, -60, -3.0, 180, 10);
            sp.setYBodyRot(180); sp.setYHeadRot(180);
        });
        client(5, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(40, "suits/7_player_front");
        client(1, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot(30, "suits/8_player_back");
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.hideGui = false; });
        shot(30, "suits/9_first_person_hand");
        server(1, sp -> sp.serverLevel().setDayTime(18000));
        client(1, () -> { Minecraft mc = Minecraft.getInstance(); mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(40, "suits/10_player_night");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "suits".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneSuits();
        }
    }

    /**
     * Scene "guns" (-Dsi.capture.scene=guns), 0.1.11.28: the mining laser and the CF sprayer (OBJ models): first person idle
     * and firing, third person front and side, on the ground, in item frames, and in the inventory.
     */
    private static void sceneGuns() {
        client(1, () -> Minecraft.getInstance().options.pauseOnLostFocus = false);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            sp.setGameMode(GameType.CREATIVE);
            ItemStack laser = item("item_tool_laser_miner");
            if (laser.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem b) b.setEnergy(laser, b.getMaxEnergy(laser));
            sp.getInventory().clearContent();
            sp.setItemInHand(InteractionHand.MAIN_HAND, laser);
            sp.getInventory().setItem(1, item("build/item_cf_sprayer"));
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, -60, 0.5, 180, 0);
            // on the ground and in item frames, a few blocks ahead (north)
            for (int i = 0; i < 2; i++) {
                var drop = new net.minecraft.world.entity.item.ItemEntity(level, -0.5 + i * 2, -60, -3.5, item(i == 0 ? "item_tool_laser_miner" : "build/item_cf_sprayer"));
                drop.setNoPickUpDelay(); drop.setPickUpDelay(32767); drop.setNeverPickUp(); drop.setUnlimitedLifetime();
                drop.setDeltaMovement(0, 0, 0);
                level.addFreshEntity(drop);
                level.setBlock(new BlockPos(-1 + i * 2, -59, -6), net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState(), 3);
                var frame = new net.minecraft.world.entity.decoration.ItemFrame(level, new BlockPos(-1 + i * 2, -59, -5), Direction.SOUTH);
                frame.setItem(item(i == 0 ? "item_tool_laser_miner" : "build/item_cf_sprayer"), false);
                level.addFreshEntity(frame);
            }
        });
        client(5, () -> { Minecraft.getInstance().options.hideGui = false; Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON); });
        shot(40, "guns/1_laser_fp_idle");
        server(1, sp -> sp.getMainHandItem().use(sp.serverLevel(), sp, InteractionHand.MAIN_HAND));
        shot(2, "guns/2_laser_fp_fire");
        client(20, () -> { Minecraft.getInstance().options.hideGui = true; Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        shot(40, "guns/3_laser_tp_lowready");
        server(1, sp -> sp.getMainHandItem().use(sp.serverLevel(), sp, InteractionHand.MAIN_HAND));
        shot(3, "guns/4_laser_tp_aim");
        server(30, sp -> { sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 90, 0); sp.setYBodyRot(90); sp.setYHeadRot(90); });
        shot(20, "guns/5_laser_tp_side");
        server(1, sp -> {
            sp.setItemInHand(InteractionHand.MAIN_HAND, item("build/item_cf_sprayer"));
            sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 180, 0); sp.setYBodyRot(180); sp.setYHeadRot(180);
        });
        shot(20, "guns/6_sprayer_tp_front");
        server(1, sp -> { sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 90, 0); sp.setYBodyRot(90); sp.setYHeadRot(90); });
        shot(20, "guns/7_sprayer_tp_side");
        client(1, () -> { Minecraft.getInstance().options.hideGui = false; Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON); });
        server(1, sp -> { sp.teleportTo(sp.serverLevel(), 0.5, -60, 0.5, 180, 0); });
        shot(20, "guns/8_sprayer_fp");
        server(1, sp -> { sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); sp.teleportTo(sp.serverLevel(), 0.0, -59.0, -1.0, 180, 25); });
        client(1, () -> Minecraft.getInstance().options.hideGui = true);
        shot(30, "guns/9_ground_and_frames");
        client(1, () -> Minecraft.getInstance().options.hideGui = false);
        client(10, () -> Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(Minecraft.getInstance().player)));
        shot(20, "guns/10_inventory");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "guns".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneGuns();
        }
    }

    static {
        if (OUT != null && "fusion_coils".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneFusionCoils();
        }
    }

    static {
        if (OUT != null && "fusion_ctm".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneFusionCtm();
        }
    }

    static {
        if (OUT != null && "slot_silhouettes".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneSlotSilhouettes();
        }
    }

    static {
        if (OUT != null && "tesu_preview".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneTesuPreview();
        }
    }

    static {
        if (OUT != null && "solar_models".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneSolarModels();
        }
    }

    static {
        if (OUT != null && "viewport_ctm".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneViewportCtm();
        }
    }

    static {
        if (OUT != null && "fusion_fx".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneFusionFx();
        }
    }

    static {
        if (OUT != null && "fusion_meltdown".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneFusionMeltdown();
        }
    }

    static {
        if (OUT != null && "fusion".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneFusion();
        }
    }

    static {
        if (OUT != null && "reactor".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneReactor();
        }
    }

    static {
        if (OUT != null && "reactor_card".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneReactorCard();
        }
    }

    /**
     * Scene "rtg" (-Dsi.capture.scene=rtg), 0.1.11.20: the RTG with ice beside it, the RT heat generator, and a complete
     * geomagnetic generator on a stone column above sea level, the clock set some hours into a storm of this world.
     */
    private static void sceneRtg() {
        BlockPos rtg = new BlockPos(2, -60, 4), heat = new BlockPos(-2, -60, 4), geo = new BlockPos(0, 70, 8);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            var pellet = new net.minecraft.world.item.ItemStack(com.miophas.singularity_iteration.common.item.resource.mio_icif_resources.RTG_PELLET.get());
            level.setBlock(rtg, block("generator/block_rt_generator").defaultBlockState(), 3);
            level.setBlock(rtg.east(), net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), 3);
            if (level.getBlockEntity(rtg) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_rt_generator r)
                for (int i = 0; i < 6; i++) r.getItemHandler().insertItem(i, pellet.copy(), false);
            level.setBlock(heat, block("hugenerator/block_rt_heat_generator").defaultBlockState(), 3);
            if (level.getBlockEntity(heat) instanceof com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator.mio_icif_rt_heat_generator r)
                for (int i = 0; i < 4; i++) r.getItemHandler().insertItem(i, pellet.copy(), false);
            for (int y = 50; y < 70; y++) level.setBlock(new BlockPos(0, y, 8), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            for (BlockPos p : new BlockPos[]{geo.offset(1, -1, 0), geo.offset(-1, -1, 0), geo.offset(0, -1, 1), geo.offset(0, -1, -1)})
                level.setBlock(p, block("generator/block_geomagnetic_pedestal").defaultBlockState(), 3);
            level.setBlock(geo, block("generator/block_geomagnetic_generator").defaultBlockState(), 3);
            level.setBlock(geo.above(), block("generator/block_geomagnetic_antenna").defaultBlockState(), 3);
            level.setBlock(geo.above(2), block("generator/block_geomagnetic_antenna").defaultBlockState(), 3);
            long seed = level.getSeed();
            long time = 6000;
            for (long d = 3; d < 400; d++) {
                var storm = com.miophas.singularity_iteration.common.energy.GeomagPhysics.storm(seed, d);
                if (storm != null) { time = storm.start() + 1500; break; }
            }
            level.setDayTime(time);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, -58, 0.5, 0, 30);
        });
        server(80, sp -> {
            for (BlockPos p : new BlockPos[]{rtg, geo})
                if (sp.serverLevel().getBlockEntity(p) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(0);
        });
        server(1, sp -> openAt(sp, rtg));
        server(30, sp -> {});
        shot(1, "rtg/1_rtg_ice");
        server(1, sp -> openAt(sp, heat));
        server(30, sp -> {});
        shot(1, "rtg/2_rt_heat");
        server(1, sp -> sp.teleportTo(sp.serverLevel(), 0.5, 71, 4.5, 0, 20));
        server(10, sp -> openAt(sp, geo));
        server(40, sp -> {});
        shot(1, "rtg/3_magnetogram");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "rtg".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneRtg();
        }
    }

    /**
     * Scene "geo" (-Dsi.capture.scene=geo), 0.1.11.19: the recorder screens. A geothermal generator in a pocket of lava
     * (two lava blocks on faces, two further off, one flowing) with lava in its tank; an incinerator fed coal with now and
     * then a cobblestone (the chart shows the warm-up and the quenches); an experience generator fed orbs.
     */
    private static void sceneGeo() {
        BlockPos geo = new BlockPos(0, -60, 4), inc = new BlockPos(3, -60, 4), xp = new BlockPos(-3, -60, 4);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(geo, block("generator/block_geo_generator").defaultBlockState(), 3);
            for (BlockPos l : new BlockPos[]{geo.below(), geo.south(), geo.offset(1, -1, 1), geo.offset(-1, -1, 1)})
                level.setBlock(l, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            for (BlockPos stone : new BlockPos[]{geo.offset(0, -2, 0), geo.offset(1, -2, 1), geo.offset(-1, -2, 1), geo.offset(0, -2, 1), geo.offset(0, -1, 2),
                    geo.offset(1, -1, 2), geo.offset(-1, -1, 2), geo.offset(2, -1, 1), geo.offset(-2, -1, 1), geo.offset(0, 0, 2), geo.offset(1, 0, 1), geo.offset(-1, 0, 1)})
                level.setBlock(stone, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            if (level.getBlockEntity(geo) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_geo_generator g)
                g.getLavaHandler().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 6000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            level.setBlock(inc, block("generator/block_drop_generator").defaultBlockState(), 3);
            level.setBlock(xp, block("generator/block_experience_generator").defaultBlockState(), 3);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, -58, 0.5, 0, 30);
        });
        for (int i = 0; i < 600; i++) {
            final int step = i;
            server(1, sp -> {
                var level = sp.serverLevel();
                if (level.getBlockEntity(inc) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(0);
                if (level.getBlockEntity(xp) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(0);
                if (level.getBlockEntity(geo) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(0);
                if (step % 20 == 0) {
                    var item = step % 100 == 40 || step % 100 == 60 ? net.minecraft.world.item.Items.COBBLESTONE : net.minecraft.world.item.Items.COAL;
                    level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, inc.getX() + 0.5, inc.getY() + 0.5, inc.getZ() + 0.5,
                        new net.minecraft.world.item.ItemStack(item)));
                }
                if (step % 40 == 0)
                    level.addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level, xp.getX() + 0.5, xp.getY() + 0.5, xp.getZ() + 0.5, 7));
            });
        }
        server(1, sp -> openAt(sp, geo));
        server(20, sp -> {});
        shot(1, "geo/1_geothermal");
        server(1, sp -> openAt(sp, inc));
        server(20, sp -> {});
        shot(1, "geo/2_incinerator");
        server(1, sp -> openAt(sp, xp));
        server(20, sp -> {});
        shot(1, "geo/3_experience");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "geo".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneGeo();
        }
    }

    /**
     * Scene "water" (-Dsi.capture.scene=water), 0.1.11.18: the water kinetic generator's rotor cross-section with a
     * waterfall pouring through its disc (three stones in it), and the water generator under a second waterfall.
     */
    private static void sceneWater() {
        BlockPos rotor = new BlockPos(0, -57, 6), gen = new BlockPos(6, -60, 6);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(rotor, facing(block("kugenerator/block_water_kinetic_generator").defaultBlockState(), Direction.NORTH), 3);
            if (level.getBlockEntity(rotor) instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Water_Kinetic_Generator w)
                w.setRotorStack(new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "rotors/item_iron_rotor"))));
            // a waterfall over the disc (z = 5, x -3..3, y -60..-54): sources above it, stones in three cells
            for (int x = -3; x <= 3; x++) level.setBlock(new BlockPos(x, -53, 5), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            for (int x = -3; x <= 3; x++) level.setBlock(new BlockPos(x, -52, 5), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            for (BlockPos stone : new BlockPos[]{new BlockPos(2, -56, 5), new BlockPos(3, -56, 5), new BlockPos(2, -57, 5)})
                level.setBlock(stone, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(gen, facing(block("generator/block_water_generator").defaultBlockState(), Direction.NORTH), 3);
            level.setBlock(gen.north().above(3), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            if (level.getBlockEntity(gen) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_water_generator g)
                g.getItemHandler().insertItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET), false);
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 2.5, -55, 1.5, 0, 20);   // flying above the spreading water (it pushed the player out of reach)
        });
        server(100, sp -> {});
        server(1, sp -> openAt(sp, rotor));
        server(50, sp -> {});
        shot(1, "water/1_rotor_waterfall");
        server(1, sp -> openAt(sp, gen));
        server(50, sp -> {});
        shot(1, "water/2_water_generator");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "water".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWater();
        }
    }

    /**
     * Scene "stirling" (-Dsi.capture.scene=stirling), 0.1.11.17: the Stirling and shaft mimic screens. A Stirling generator
     * in air, an advanced Stirling generator with water beside it, a Stirling kinetic generator driving a kinetic generator,
     * and a turbo kinetic generator with nothing in front.
     */
    private static void sceneStirling() {
        BlockPos basic = new BlockPos(0, -60, 3), advanced = new BlockPos(2, -60, 3), water = new BlockPos(2, -61, 3);   // a pit under it: no spreading
        BlockPos kinetic = new BlockPos(-3, -60, 3), kgen = new BlockPos(-3, -60, 2), turbo = new BlockPos(-6, -60, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(basic, block("generator/block_stirling_generator").defaultBlockState(), 3);
            level.setBlock(advanced, block("generator/block_advanced_stirling_generator").defaultBlockState(), 3);
            level.setBlock(water, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            level.setBlock(kinetic, facing(block("kugenerator/block_stirling_kinetic_generator").defaultBlockState(), Direction.NORTH), 3);
            level.setBlock(kgen, facing(block("generator/block_kinetic_generator").defaultBlockState(), Direction.SOUTH), 3);
            level.setBlock(turbo, facing(block("generator/block_turbo_kinetic_generator").defaultBlockState(), Direction.NORTH), 3);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 20);
        });
        stirlingRun(basic, advanced, kinetic, kgen, 400);
        server(1, sp -> openAt(sp, basic));
        stirlingRun(basic, advanced, kinetic, kgen, 30);
        shot(1, "stirling/1_stirling_air");
        server(1, sp -> openAt(sp, advanced));
        stirlingRun(basic, advanced, kinetic, kgen, 30);
        shot(1, "stirling/2_advanced_water");
        server(1, sp -> openAt(sp, kinetic));
        stirlingRun(basic, advanced, kinetic, kgen, 30);
        shot(1, "stirling/3_kinetic");
        server(1, sp -> openAt(sp, kgen));
        stirlingRun(basic, advanced, kinetic, kgen, 30);
        shot(1, "stirling/4_kinetic_generator");
        server(1, sp -> openAt(sp, turbo));
        stirlingRun(basic, advanced, kinetic, kgen, 30);
        shot(1, "stirling/5_turbo_no_source");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Heat into the three engines, their storages drained, the kinetic engine's water topped up and hot water taken. */
    private static void stirlingRun(BlockPos basic, BlockPos advanced, BlockPos kinetic, BlockPos kgen, int ticks) {
        for (int i = 0; i < ticks; i++) {
            server(1, sp -> {
                var level = sp.serverLevel();
                if (level.getBlockEntity(basic) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_stirling_generator s) {
                    s.getHeatStorageCapability(null).receiveHeat(200, false);
                    s.getEnergyStorage().setEnergy(0);
                }
                if (level.getBlockEntity(advanced) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_advanced_stirling_generator a) {
                    var heat = a.getHeatStorageCapability(Direction.UP);
                    if (heat != null) heat.receiveHeat(150, false);
                    a.getEnergyStorage().setEnergy(0);
                }
                if (level.getBlockEntity(kinetic) instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Stirling_Kinetic_Generator k) {
                    var heat = k.getHeatStorageCapability(Direction.SOUTH);
                    if (heat != null) heat.receiveHeat(400, false);
                    k.getWaterTank().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    k.getHotWaterTank().drain(2000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
                if (level.getBlockEntity(kgen) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(0);
            });
        }
    }

    static {
        if (OUT != null && "stirling".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneStirling();
        }
    }

    /**
     * Scene "steam" (-Dsi.capture.scene=steam), 0.1.11.16: the plant mimic screens of the steam chain. A boiler above the
     * critical point feeds a steam turbine with a condenser beside it; a steam kinetic generator with a rotor drives a
     * kinetic generator, its exhaust into a second condenser; a repressurizer with superheated steam in it.
     */
    private static void sceneSteam() {
        BlockPos boiler = new BlockPos(0, -60, 3), turbine = new BlockPos(1, -60, 3), condenser = new BlockPos(2, -60, 3);
        BlockPos kinetic = new BlockPos(-3, -60, 3), kgen = new BlockPos(-3, -60, 2), condenser2 = new BlockPos(-4, -60, 3);
        BlockPos repress = new BlockPos(-6, -60, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(boiler, block("producer/block_steam_generator").defaultBlockState(), 3);
            level.setBlock(turbine, block("generator/block_steam_turbine").defaultBlockState(), 3);
            level.setBlock(condenser, block("producer/block_condenser").defaultBlockState(), 3);
            level.setBlock(kinetic, facing(block("producer/block_steam_kinetic_generator").defaultBlockState(), Direction.NORTH), 3);
            level.setBlock(kgen, facing(block("generator/block_kinetic_generator").defaultBlockState(), Direction.SOUTH), 3);
            level.setBlock(condenser2, block("producer/block_condenser").defaultBlockState(), 3);
            level.setBlock(repress, block("producer/block_steam_repressurizer").defaultBlockState(), 3);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 20);
            var be = level.getBlockEntity(boiler);
            if (be != null) {
                var tag = be.saveWithoutMetadata(level.registryAccess());
                tag.putInt("pressure", 221);
                tag.putInt("inputMB", 1);
                tag.putFloat("systemHeat", 100F + 221F / 220F * 100F * 2.74F);
                be.loadWithComponents(tag, level.registryAccess());
            }
            if (be instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_steam_generator b)
                b.getWaterTank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                    com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.DISTILLEDWATER.get(), 8000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (level.getBlockEntity(kinetic) instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_steam_kinetic_generator k)
                k.getItemHandler().setStackInSlot(2, new net.minecraft.world.item.ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "normal/item_steam_turbin"))));
            if (level.getBlockEntity(condenser2) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_condenser c)
                for (int slot = 0; slot < 4; slot++) c.getItemHandler().insertItem(slot, new net.minecraft.world.item.ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_vent"))), false);
            if (level.getBlockEntity(repress) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_steam_repressurizer r)
                r.getInputTank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                    com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.SUPERHEATEDSTEAM.get(), 6000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        });
        steamRun(boiler, turbine, kinetic, kgen, condenser2, 80);
        server(1, sp -> openAt(sp, boiler));
        steamRun(boiler, turbine, kinetic, kgen, condenser2, 40);
        shot(1, "steam/1_boiler");
        server(1, sp -> openAt(sp, turbine));
        steamRun(boiler, turbine, kinetic, kgen, condenser2, 30);
        shot(1, "steam/2_turbine_condensing");
        server(1, sp -> openAt(sp, condenser));
        steamRun(boiler, turbine, kinetic, kgen, condenser2, 30);
        shot(1, "steam/3_condenser");
        server(1, sp -> openAt(sp, kinetic));
        steamRun(boiler, turbine, kinetic, kgen, condenser2, 30);
        shot(1, "steam/4_kinetic");
        server(1, sp -> openAt(sp, repress));
        steamRun(boiler, turbine, kinetic, kgen, condenser2, 30);
        shot(1, "steam/5_repressurizer");
        client(10, () -> Minecraft.getInstance().stop());
    }

    private static net.minecraft.world.level.block.state.BlockState facing(net.minecraft.world.level.block.state.BlockState state, Direction d) {
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING))
            return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, d);
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING))
            return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, d);
        return state;
    }

    /** Heat into the boiler, the turbine's and the kinetic generator's storage drained, the kinetic turbine kept in steam. */
    private static void steamRun(BlockPos boiler, BlockPos turbine, BlockPos kinetic, BlockPos kgen, BlockPos condenser2, int ticks) {
        for (int i = 0; i < ticks; i++) {
            server(1, sp -> {
                var level = sp.serverLevel();
                if (level.getBlockEntity(boiler) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_steam_generator b) {
                    var heat = b.getHeatStorageCapability(null);
                    if (heat != null) heat.receiveHeat(b.getSystemHeat() < 376 ? 1200 : 300, false);
                }
                if (level.getBlockEntity(turbine) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(0);
                if (level.getBlockEntity(kgen) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(0);
                if (level.getBlockEntity(condenser2) instanceof AbstractEnergyBlockEntity e) e.getEnergyStorageInternal().setStored(e.getEnergyStorageInternal().getCapacity());
                if (level.getBlockEntity(kinetic) instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_steam_kinetic_generator k
                        && k.getSteamTank().getFluidAmount() < 8000)
                    k.getSteamTank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                        com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.STEAM.get(), 4000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            });
        }
    }

    static {
        if (OUT != null && "steam".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneSteam();
        }
    }

    /**
     * Scene "engine" (-Dsi.capture.scene=engine), 0.1.11.15: the engine room monitor of a diesel generator. A cold start
     * under a varying load, the engine warm at load, then a storage filled to the top (stopped).
     */
    private static void sceneEngine() {
        BlockPos at = new BlockPos(0, -60, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(at, block("generator/block_diesel_generator").defaultBlockState(), 3);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 20);
            if (level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_diesel_generator d)
                d.getFuelTank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                    com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.DIESELOIL.get(), 180_000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        });
        server(5, sp -> openAt(sp, at));
        engineLoad(at, 140);
        shot(1, "engine/1_cold_start");
        engineLoad(at, 900);
        shot(1, "engine/2_warm_running");
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_diesel_generator d)
                d.getEnergyStorageInternal().setStored(d.getEnergyStorageInternal().getCapacity());
        });
        server(60, sp -> {});
        shot(1, "engine/3_stopped_full");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** A grid taking a varying load (30..110 EU/t) from the generator for {@code ticks} ticks, the storage held near half. */
    private static void engineLoad(BlockPos at, int ticks) {
        for (int i = 0; i < ticks; i++) {
            server(1, sp -> {
                if (!(sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_diesel_generator d)) return;
                var store = d.getEnergyStorageInternal();
                long t = sp.serverLevel().getGameTime();
                long take = Math.round(70 + 40 * Math.sin(t / 90.0));
                store.setStored(Math.max(0, Math.min(store.getAmount(), 500_000) - take));
            });
        }
    }

    static {
        if (OUT != null && "engine".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneEngine();
        }
    }

    /**
     * Scene "solar" (-Dsi.capture.scene=solar), 0.1.11.14: the solar panel screen. A hybrid panel is driven through the
     * morning against the world clock (a shower around 09:00), then shot at noon, under a roof, and at night on its core.
     */
    private static void sceneSolar() {
        BlockPos at = new BlockPos(0, -60, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
            level.setBlock(at, block("generator/block_hybrid_solar_panel").defaultBlockState(), 3);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 20);
            solarDrive(level, at, 0, 6200);
        });
        server(5, sp -> openAt(sp, at));
        server(30, sp -> {});
        shot(1, "solar/1_noon");
        server(1, sp -> sp.serverLevel().setBlock(at.above(3), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3));
        server(30, sp -> {});
        shot(1, "solar/2_covered");
        server(1, sp -> sp.serverLevel().removeBlock(at.above(3), false));
        server(20, sp -> solarDrive(sp.serverLevel(), at, 6200, 15000));   // after the sky light under the old roof is back
        server(30, sp -> {});
        shot(1, "solar/3_night_core");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /** Runs the panel's tick against the world clock from {@code from} to {@code to} (a shower 2500..3800), leaving the clock at {@code to}. */
    private static void solarDrive(net.minecraft.server.level.ServerLevel level, BlockPos at, long from, long to) {
        if (!(level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_solar_generator panel)) return;
        try {
            var refresh = com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_solar_generator.class.getDeclaredField("refreshIn");
            refresh.setAccessible(true);
            for (long t = from; t <= to; t += 10) {
                level.setDayTime(t);
                boolean shower = t >= 2500 && t < 3800;
                level.setRainLevel(shower ? 1 : 0);
                level.setThunderLevel(0);
                refresh.setInt(panel, 0);
                com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_solar_generator.tick(level, at, level.getBlockState(at), panel);
                panel.getEnergyStorageInternal().setStored(Math.max(0, panel.getEnergyStorageInternal().getAmount() - 600));
            }
            level.setRainLevel(0);
            level.setDayTime(to);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static {
        if (OUT != null && "solar".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneSolar();
        }
    }

    /**
     * Scene "thermal" (-Dsi.capture.scene=thermal), 0.1.11.10: the generator's chart recorder. Most of a game day of record
     * is written first (the furnace is driven against earlier times with a load that rises and falls, and a refuel gap),
     * then the screen at full output, banked (storage full), and in a cold start.
     */
    private static void sceneThermal() {
        BlockPos at = new BlockPos(0, -60, 3);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setBlock(at, block("generator/block_thermal_generator").defaultBlockState(), 3);
            sp.teleportTo(level, 0.5, -60, 0.5, 0, 20);
            if (!(level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.mio_icif_Thermal_Generator g)) return;
            var fuel = g.getItemHandler();
            try {
                var tick = g.getClass().getDeclaredMethod("tickFurnace", net.minecraft.world.level.Level.class, BlockPos.class);
                var record = g.getClass().getDeclaredMethod("record", long.class);
                tick.setAccessible(true);
                record.setAccessible(true);
                long now = level.getGameTime(), span = 20000;
                var store = g.getEnergyStorageInternal();
                for (long k = 0; k < span; k++) {
                    long t = now - span + k;
                    // six lumps, a cold gap until a refuel, then a long run
                    if (k == 0 || k == 6000 || k == 18000)
                        ((net.neoforged.neoforge.items.IItemHandlerModifiable) fuel).setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.COAL, k == 0 ? 6 : k == 6000 ? 30 : 64));
                    tick.invoke(g, level, at);
                    // the grid's draw: a day curve, light in the small hours (the storage fills and the fire is banked)
                    double load = 7 + 5 * Math.sin(k / 3200.0) + 1.5 * Math.sin(k / 410.0);
                    store.setStored(Math.max(0, store.getAmount() - Math.round(Math.max(0, load))));
                    record.invoke(g, t);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            store(g, 1200);
        });
        server(5, sp -> openAt(sp, at));
        thermalDrain(at, 60, 12);
        shot(1, "thermal/1_full_output");
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.mio_icif_Thermal_Generator g)
                store(g, g.getEnergyStorageInternal().getCapacity());
        });
        server(160, sp -> {});
        shot(1, "thermal/2_banked");
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.mio_icif_Thermal_Generator g) {
                ((net.neoforged.neoforge.items.IItemHandlerModifiable) g.getItemHandler()).setStackInSlot(0, ItemStack.EMPTY);
                try {
                    for (String f : new String[]{"heat", "burnLeft"}) {
                        var field = g.getClass().getDeclaredField(f);
                        field.setAccessible(true);
                        field.setDouble(g, 0);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                store(g, 0);
                ((net.neoforged.neoforge.items.IItemHandlerModifiable) g.getItemHandler()).setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.CHARCOAL, 23));
            }
        });
        thermalDrain(at, 70, 12);
        shot(1, "thermal/3_cold_start");
        client(10, () -> Minecraft.getInstance().stop());
    }

    private static void store(com.miophas.singularity_iteration.common.blockentity.mio_icif_Thermal_Generator g, long eu) {
        g.getEnergyStorageInternal().setStored(eu);
    }

    /** A load of {@code perTick} EU taken from the generator each tick for {@code ticks} ticks. */
    private static void thermalDrain(BlockPos at, int ticks, int perTick) {
        for (int i = 0; i < ticks; i++) {
            server(1, sp -> {
                if (sp.serverLevel().getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.mio_icif_Thermal_Generator g)
                    store(g, Math.max(0, g.getEnergyStorageInternal().getAmount() - perTick));
            });
        }
    }

    static {
        if (OUT != null && "thermal".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneThermal();
        }
    }

    /**
     * Scene "wind" (-Dsi.capture.scene=wind), 0.1.11.9: the two wind machine screens. A carbon-rotor turbine high up with
     * another one upstream (its wake), in a fair wind, then a storm (feathered, then overloaded without storm protection);
     * then the wind generator. The wind field is set directly and the roses get a minute of varied wind.
     */
    private static void sceneWind() {
        BlockPos kin = new BlockPos(0, 140, 12), up = new BlockPos(0, 140, 4), gen = new BlockPos(20, 140, 12);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            var state = block("kugenerator/block_wind_kinetic_generator").defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.NORTH);
            level.setBlock(kin, state, 3);
            level.setBlock(up, state, 3);
            level.setBlock(gen, block("generator/block_wind_generator").defaultBlockState(), 3);
            var rotor = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "rotors/item_carbon_rotor")));
            for (BlockPos at : new BlockPos[]{kin, up})
                if (level.getBlockEntity(at) instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator k)
                    k.setRotorStack(rotor.copy());
            windField(level, 14, 20);
            var rnd = new java.util.Random(7);
            for (BlockPos at : new BlockPos[]{kin, gen}) {
                Object rose = fieldOf(level.getBlockEntity(at), "rose");
                for (int i = 0; i < 64; i++) {
                    int b = Math.floorMod((int) (20 + rnd.nextGaussian() * 40), 360);
                    ((com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WindRoseBuffer) rose).add(b, 30 + rnd.nextInt(60));
                }
            }
            sp.setGameMode(GameType.CREATIVE);                                     // flies: a fall would close the screen
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, 141, 16.5, 180, 10);
        });
        server(5, sp -> openAt(sp, kin));
        server(140, sp -> {});
        shot(1, "wind/1_kinetic_wake");
        server(1, sp -> {
            // a thunderstorm (wind x1.5) and the upstream turbine gone: well above the carbon rotor's cut-out
            sp.serverLevel().setWeatherParameters(0, 6000, true, true);
            sp.serverLevel().removeBlock(up, false);
            windField(sp.serverLevel(), 30, 200);
        });
        server(140, sp -> {});
        shot(1, "wind/2_storm_feathered");
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(kin) instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator k)
                k.toggleStormProtection();
        });
        server(140, sp -> {});
        shot(1, "wind/3_storm_overload");
        server(1, sp -> {
            sp.closeContainer();
            sp.serverLevel().setWeatherParameters(6000, 0, false, false);
            windField(sp.serverLevel(), 20, 60);
            sp.teleportTo(sp.serverLevel(), 20.5, 141, 16.5, 180, 10);
        });
        server(5, sp -> openAt(sp, gen));
        server(260, sp -> {});
        shot(1, "wind/4_generator");
        client(10, () -> Minecraft.getInstance().stop());
    }

    /**
     * Scene "quantum" (-Dsi.capture.scene=quantum), 0.1.11.131: the quantum generator screen while it generates (a third
     * full), then with a full storage (core dim, coils dark, the pairs only annihilate).
     */
    private static void sceneQuantum() {
        BlockPos gen = new BlockPos(0, 140, 12);
        server(40, sp -> {
            var level = sp.serverLevel();
            level.setBlock(gen, block("generator/block_quantum_generator").defaultBlockState(), 3);
            if (level.getBlockEntity(gen) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_QuantumGenerator q)
                q.getEnergyStorageInternal().setStored(33_000_000L);
            sp.setGameMode(GameType.CREATIVE);
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, 0.5, 141, 16.5, 180, 10);
        });
        server(5, sp -> openAt(sp, gen));
        server(60, sp -> {});
        shot(1, "quantum/1_generating");
        server(1, sp -> {
            if (sp.serverLevel().getBlockEntity(gen) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_QuantumGenerator q)
                q.getEnergyStorageInternal().setStored(q.getEnergyStorageInternal().getCapacity());
        });
        server(60, sp -> {});
        shot(1, "quantum/2_full");
        client(10, () -> Minecraft.getInstance().stop());
    }

    static {
        if (OUT != null && "quantum".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneQuantum();
        }
    }

    /** Sets the dimension's wind field (strength 0..30, bearing). */
    private static void windField(net.minecraft.server.level.ServerLevel level, int strength, int bearing) {
        Object sim = com.miophas.singularity_iteration.core.runtime.world.WindSim.get(level);
        setField(sim, "windStrength", strength);
        setField(sim, "windDirection", bearing);
    }

    private static Object fieldOf(Object target, String name) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object target, String name, int value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setInt(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static {
        if (OUT != null && "wind".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneWind();
        }
    }

    static {
        if (OUT != null && "poly".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scenePoly();
        }
    }

    static {
        if (OUT != null && "pump".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scenePump();
        }
    }

    static {
        if (OUT != null && "mt".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneMt();
        }
    }

    static {
        if (OUT != null && "holo".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            sceneHolo();
        }
    }

    static {
        if (OUT != null && "14".equals(System.getProperty("si.capture.scene"))) {
            new File(OUT).mkdirs();
            scene14();
        }
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
        } else if (OUT != null && System.getProperty("si.capture.scene") != null && STEPS.isEmpty()) {
            // a named scene of its own (registered above, so its steps are queued) does not get the default scene too
            new File(OUT).mkdirs();
            scene();
        } else {
        String jeiList = System.getProperty("si.capture.jei");
        String galleryList = System.getProperty("si.capture.gallery");
        if (OUT != null && jeiList != null) {
            new File(OUT).mkdirs();
            jeiGallery(jeiList);
        } else if (OUT != null && galleryList != null) {
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
        mc.options.chatVisibility().set(showChat ? net.minecraft.world.entity.player.ChatVisiblity.FULL : net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
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
