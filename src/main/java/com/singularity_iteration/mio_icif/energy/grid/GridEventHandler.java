package com.singularity_iteration.mio_icif.energy.grid;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
public class GridEventHandler {

    private static boolean initialized = false;

    // Keep the load delay close to IC2's one-tick batching window.
    private static final int DEFERRED_TICKS = 1;
    private final Map<Level, List<DeferredTile>> deferredTiles = new Object2ObjectOpenHashMap<>();

    private record DeferredTile(IEnergyTile tile, Level world, long tickToRegister) {}

    public static void init() {
        if (initialized) throw new IllegalStateException("already initialized");
        initialized = true;
        EnergyNetGlobal.init();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new GridEventHandler());
    }

    @SubscribeEvent
    public void onTileLoad(EnergyTileLoadEvent event) {
        if (event.world.isClientSide()) return;
        Level world = event.world;
        List<DeferredTile> list = deferredTiles.computeIfAbsent(world, k -> new ObjectArrayList<>());
        for (DeferredTile deferredTile : list) {
            if (deferredTile.tile == event.tile) return;
        }
        list.add(new DeferredTile(event.tile, world, world.getGameTime() + DEFERRED_TICKS));
    }

    @SubscribeEvent
    public void onTileUnload(EnergyTileUnloadEvent event) {
        if (event.world.isClientSide()) return;
        Level world = event.world;
        List<DeferredTile> list = deferredTiles.get(event.world);
        if (list != null) {
            list.removeIf(deferredTile -> deferredTile.tile == event.tile);
            if (list.isEmpty()) deferredTiles.remove(event.world);
        }
        if (!WorldData.has(event.world)) return;
        EnergyNetGlobal.removeTile(event.tile, world, EnergyNetGlobal.getPos(event.tile));
    }

    @SubscribeEvent
    public void onApiTileRegister(com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent.EnergyTileRegisterEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        Level world = serverLevel;
        com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent.IEnergyTileMarker marker = event.getTile();
        BlockPos pos = event.getPos();
        ApiTileCompat wrapper = ApiTileCompat.create(marker, world, pos);
        ApiTileCompat.putMarker(marker, wrapper);
        List<DeferredTile> list = deferredTiles.computeIfAbsent(world, k -> new ObjectArrayList<>());
        for (DeferredTile dt : list) {
            if (dt.tile == wrapper) return;
        }
        list.add(new DeferredTile(wrapper, world, world.getGameTime() + DEFERRED_TICKS));
    }

    @SubscribeEvent
    public void onApiTileUnregister(com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent.EnergyTileUnregisterEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent.IEnergyTileMarker marker = event.getTile();
        ApiTileCompat wrapper = ApiTileCompat.removeMarker(marker);
        if (wrapper == null) return;
        List<DeferredTile> list = deferredTiles.get(serverLevel);
        if (list != null) list.removeIf(dt -> dt.tile == wrapper);
        if (WorldData.has(serverLevel)) EnergyNetGlobal.removeTile(wrapper, serverLevel, EnergyNetGlobal.getPos(wrapper));
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            WorldData.remove(serverLevel);
            deferredTiles.remove(serverLevel);
            
            // 清理机器注册表，防止内存泄漏
            com.singularity_iteration.mio_icif.api.machine.MachineAPIImpl.onDimensionUnload(serverLevel);
        }
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
 // 数据包重载时清除配方存和注册表存
        com.singularity_iteration.mio_icif.api.recipe.RecipeAPIImpl recipeApi = 
            (com.singularity_iteration.mio_icif.api.recipe.RecipeAPIImpl) 
            com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getRecipeAPI();
        recipeApi.invalidateCache();
        
        // 重新注册表缓存
        com.singularity_iteration.mio_icif.api.registry.MioIcifRegistriesImpl registries = 
            (com.singularity_iteration.mio_icif.api.registry.MioIcifRegistriesImpl) 
            com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getRegistries();
        registries.refreshCache();
    }

    @SubscribeEvent
    public void onLevelTickPre(LevelTickEvent.Pre event) {
        if (event.getLevel().isClientSide()) return;

        Level level = event.getLevel();
        List<DeferredTile> list = deferredTiles.get(level);
        if (list != null && !list.isEmpty()) {
            long currentTick = level.getGameTime();
            int registrations = 0;
            Iterator<DeferredTile> iterator = list.iterator();
            while (iterator.hasNext()) {
                DeferredTile dt = iterator.next();
                if (registrations < EnergyNetSettings.maxTileRegistrationsPerTick
                        && currentTick >= dt.tickToRegister) {
                    EnergyNetGlobal.addTile(dt.tile, dt.world, EnergyNetGlobal.getPos(dt.tile));
                    iterator.remove();
                    registrations++;
                }
            }
            if (list.isEmpty()) {
                deferredTiles.remove(level);
            }
        }

        if (!WorldData.has(level)) return;
        EnergyNetLocal enet = EnergyNetGlobal.getLocal(level);
        enet.onTickStart();
    }

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        if (!WorldData.has(event.getLevel())) return;
        EnergyNetLocal enet = EnergyNetGlobal.getLocal(event.getLevel());
        enet.onTickEnd();
    }
}