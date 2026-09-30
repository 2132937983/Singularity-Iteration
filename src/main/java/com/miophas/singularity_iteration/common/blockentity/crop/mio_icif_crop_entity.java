// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.crop;

import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick;
import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick_upgraded;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.item.crop.CropSeedItem;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.runtime.crop.PlantHybridization;
import com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Independent persisted crop planter implementation. */
public class mio_icif_crop_entity extends BlockEntity implements IPlanter {
    public static final int FERTILIZER_COOLDOWN = 200;
    public static final int CROP_TICK_RATE = 256;

    private PlantType plant;
    private int growthStage;
    private int growthSpeed;
    private int yield;
    private int resilience;
    private int nutrients;
    private int water;
    private int weedControl;
    private int fertilizerCooldown;
    private int progress;
    private int scanLevel;
    private boolean hybridBase;
    private int cropTicker;
    private int humidity, soilNutrients, airQuality, biomeHumidity;
    private boolean environmentReady;
    private int lastSignal, lastLight;
    private CompoundTag customData = new CompoundTag();

    /**
     * Light lookup is the only environment query in the ordinary crop tick
     * that walks the world lighting graph.  A plant can ask for it more than
     * once during one game tick (for example from a custom growth policy and
     * a harvest check), so keep a one-tick snapshot.  The cache is deliberately
     * scoped to a single tick: block updates and light propagation are visible
     * on the following tick without requiring fragile neighbour listeners.
     */
    private long lightCacheTick = Long.MIN_VALUE;
    private int lightCacheValue;

    public mio_icif_crop_entity(BlockPos pos, BlockState state) {
        this(mio_icif_block_entities.CROP_ENTITY_TYPE.get(), pos, state);
    }

    public mio_icif_crop_entity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state, mio_icif_crop_entity entity) {
        if (level.isClientSide || entity != this) return;
        if (fertilizerCooldown > 0) {
            fertilizerCooldown--;
            // The changing value is persisted state, so every decrement must
            // keep the owning chunk dirty until the next successful save.
            setChanged();
        }
        if (plant != null && !plant.usesIc2CropCycle()) plant.tick(this);
        cropTicker = (cropTicker + 1) % 10240;
        if (cropTicker % CROP_TICK_RATE == 0) {
            performCropCycle();
            updateState();
        }
    }

    /** One IC2 crop cycle; also used by server integration tests. */
    public void performCropCycle() {
        if (level == null || level.isClientSide) return;
        refreshEnvironment();
        if (plant == null && (!hybridBase || !PlantHybridization.tryHybridize(this))
                && (!hybridBase || !PlantHybridization.trySpread(this))) {
            if (level.random.nextInt(100) == 0 && weedControl <= 0) becomeWeed(this, 0);
            else {
                if (weedControl > 0 && level.random.nextInt(10) == 0) setWeedControl(weedControl - 1);
                return;
            }
        }
        if (plant == null || !plant.usesIc2CropCycle()) return;
        plant.tick(this);
        if (plant == null) return;
        if (plant.canGrow(this)) {
            int base = 3 + level.random.nextInt(7) + growthSpeed;
            int required = Math.max(0, (plant.getStats().getLevel() - 1) * 4 + growthSpeed + yield + resilience);
            int quality = plant.weightInfluences(this, getHumidity(), getSoilNutrients(), getAirQuality()) * 5;
            int increment;
            if (quality >= required) increment = base * (100 + quality - required) / 100;
            else {
                int penalty = (required - quality) * 4;
                if (penalty > 100 && level.random.nextInt(32) > resilience) { reset(); return; }
                increment = Math.max(0, base * (100 - penalty) / 100);
            }
            setProgress(progress + increment);
            if (progress >= plant.getGrowthTime(this)) {
                setProgress(0);
                setGrowthStage(growthStage + 1);
            }
        }
        if (nutrients > 0) setNutrients(nutrients - 1);
        if (water > 0) setWater(water - 1);
        if (plant.isWeed(this) && level.random.nextInt(50) - growthSpeed <= 2) spreadWeed();
    }

    private void becomeWeed(IPlanter target, int growth) {
        PlantType weed = PlantRegistry.instance.getPlant("mio_icif", "weed");
        if (weed == null) return;
        target.reset(); target.setPlant(weed); target.setGrowthStage(1); target.setGrowthSpeed(growth); target.updateState();
    }

    private void spreadWeed() {
        var directions = new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH,
            net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST};
        BlockPos pos = worldPosition.relative(directions[level.random.nextInt(4)]);
        if (!level.hasChunkAt(pos)) return;
        if (level.getBlockEntity(pos) instanceof IPlanter other) {
            boolean invade = other.getPlant() == null;
            if (!invade && !other.getPlant().isWeed(other) && level.random.nextInt(32) >= other.getResilience()) {
                if (other.getWeedControl() > 0) other.setWeedControl(other.getWeedControl() - 5);
                else invade = true;
            }
            if (invade) {
                int growth = Math.max(growthSpeed, other.getGrowthSpeed());
                if (growth < 31 && level.random.nextBoolean()) growth++;
                becomeWeed(other, growth);
            }
        } else if (level.isEmptyBlock(pos)) {
            BlockState soil = level.getBlockState(pos.below());
            if (soil.is(Blocks.DIRT) || soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.FARMLAND)) {
                level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
                level.setBlockAndUpdate(pos, Blocks.SHORT_GRASS.defaultBlockState());
            }
        }
    }

    @Override public PlantType getPlant() { return plant; }
    @Override public void setPlant(PlantType plant) { if (this.plant != plant) { this.plant = plant; environmentReady = false; setChanged(); } }
    @Override public int getGrowthStage() { return growthStage; }
    @Override public void setGrowthStage(int value) { int next = Math.clamp(value, 0, plant == null ? 7 : plant.getMaxGrowthStage()); if (growthStage != next) { growthStage = next; setChanged(); } }
    @Override public int getGrowthSpeed() { return growthSpeed; }
    @Override public void setGrowthSpeed(int value) { int next = Math.clamp(value, 0, 31); if (growthSpeed != next) { growthSpeed = next; setChanged(); } }
    @Override public int getYield() { return yield; }
    @Override public void setYield(int value) { int next = Math.clamp(value, 0, 31); if (yield != next) { yield = next; setChanged(); } }
    @Override public int getResilience() { return resilience; }
    @Override public void setResilience(int value) { int next = Math.clamp(value, 0, 31); if (resilience != next) { resilience = next; setChanged(); } }
    @Override public int getNutrients() { return nutrients; }
    @Override public void setNutrients(int value) { int next = Math.clamp(value, 0, 199); if (nutrients != next) { nutrients = next; setChanged(); } }
    @Override public int getWater() { return water; }
    @Override public void setWater(int value) { int next = Math.clamp(value, 0, 200); if (water != next) { water = next; setChanged(); } }
    @Override public int getWeedControl() { return weedControl; }
    @Override public void setWeedControl(int value) { int next = Math.clamp(value, 0, 150); if (weedControl != next) { weedControl = next; setChanged(); } }
    public int getFertilizerCooldown() { return fertilizerCooldown; }
    public void setFertilizerCooldown(int value) { int next = Math.max(0, value); if (fertilizerCooldown != next) { fertilizerCooldown = next; setChanged(); } }
    @Override public int getProgress() { return progress; }
    @Override public void setProgress(int value) {
        int next = Math.max(0, value);
        if (progress == next) return;
        progress = next;
        markChunkUnsaved();
    }

    /** Persist counters immediately without issuing comparator updates or loading chunks. */
    private void markChunkUnsaved() {
        if (level instanceof ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
            if (chunk != null) chunk.setUnsaved(true);
        }
    }
    @Override public int getScanLevel() { return scanLevel; }
    @Override public void setScanLevel(int value) { int next = Math.clamp(value, 0, 4); if (scanLevel != next) { scanLevel = next; setChanged(); } }
    @Override public boolean isHybridBase() { return hybridBase; }
    @Override public void setHybridBase(boolean value) { if (hybridBase != value) { hybridBase = value; setChanged(); } }
    @Override public CompoundTag getCustomData() { return customData; }

    private void refreshEnvironment() {
        if (level == null) return;
        boolean initial = !environmentReady;
        var biome = level.getBiome(worldPosition);
        if (initial || cropTicker % 10240 == 0) {
            float rain = biome.value().getModifiedClimateSettings().downfall();
            float temp = ((com.miophas.singularity_iteration.common.mixin.CropBiomeAccessor) (Object) biome.value()).mio_icif$cropTemperature(worldPosition);
            int rainfall = Math.clamp((int) (25f * rain - 12.5), -10, 10);
            biomeHumidity = rainfall + Math.clamp((int) (Math.abs(rainfall) * (-2 * temp * temp + 4 * temp - 1)), -10, 10);
        }
        if (initial || cropTicker % 1024 == 0) {
            BlockState soil = level.getBlockState(worldPosition.below());
            humidity = biomeHumidity + (soil.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE)
                && soil.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) >= 7 ? 2 : 0)
                + (water >= 5 ? 2 : 0) + (water + 24) / 25;
        }
        if (initial || (cropTicker + 256) % 1024 == 0) {
            soilNutrients = com.miophas.singularity_iteration.common.crop.CropEnvironment.nutrientBonus(biome);
            for (int i = 1; i < 5 && level.getBlockState(worldPosition.below(i)).is(Blocks.DIRT); i++) soilNutrients++;
            soilNutrients += (nutrients + 19) / 20;
        }
        if (initial || (cropTicker + 512) % 1024 == 0) {
            int fresh = 9;
            // Preserve the Experimental source's asymmetric 2x2 neighborhood.
            for (int x = -1; x < 1; x++) for (int z = -1; z < 1; z++) {
                BlockPos pos = worldPosition.offset(x, 0, z);
                if (!level.hasChunkAt(pos) || level.getBlockState(pos).isSolidRender(level, pos)
                        || level.getBlockEntity(pos) instanceof IPlanter) fresh--;
            }
            airQuality = Math.clamp((int) Math.floor((worldPosition.getY() - 40) / 15.0), 0, 2)
                + fresh / 2 + (level.canSeeSky(worldPosition.above()) ? 4 : 0);
        }
        environmentReady = true;
    }
    @Override public int getHumidity() { if (!environmentReady) refreshEnvironment(); return humidity; }
    @Override public int getSoilNutrients() { if (!environmentReady) refreshEnvironment(); return soilNutrients; }
    @Override public int getAirQuality() { if (!environmentReady) refreshEnvironment(); return airQuality; }
    @Override public Level getPlanterWorld() { return level; }
    @Override public BlockPos getPlanterPos() { return worldPosition; }
    @Override
    public int getLightLevel() {
        Level currentLevel = level;
        if (currentLevel == null) return 0;
        long gameTime = currentLevel.getGameTime();
        if (lightCacheTick != gameTime) {
            lightCacheValue = currentLevel.getMaxLocalRawBrightness(worldPosition);
            lightCacheTick = gameTime;
        }
        return lightCacheValue;
    }

    @Override
    public boolean pick() {
        if (plant == null || level == null || level.isClientSide) return false;
        float chance = (float) (plant.dropSeedChance(this) * Math.pow(1.1, resilience));
        int count = 0;
        if (plant.canBeHarvested(this)) {
            if (level.random.nextFloat() <= (chance + 1) * 0.8f) count++;
            float second = plant.dropSeedChance(this) + growthSpeed / 100f;
            for (int i = 23; i < yield; i++) second *= 0.95f;
            if (level.random.nextFloat() <= second) count++;
        } else if (level.random.nextFloat() <= chance * 1.5f) count++;
        for (int i = 0; i < count; i++) Block.popResource(level, worldPosition, plant.getSeedItem(this));
        reset();
        updateState();
        return true;
    }

    @Override
    public boolean doManualHarvest() {
        List<ItemStack> drops = doHarvest();
        if (drops.isEmpty() || level == null) return false;
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) level.addFreshEntity(new ItemEntity(level,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D, drop));
        }
        return true;
    }

    @Override
    public List<ItemStack> doHarvest() {
        if (plant == null || !plant.isHarvestable(this)) return List.of();
        List<ItemStack> drops = getHarvestDrops();
        resetGrowthStage();
        updateState();
        return drops;
    }

    @Override
    public boolean doHarvestWithoutSeeds() {
        if (plant == null || !plant.isHarvestable(this)) return false;
        resetGrowthStage();
        updateState();
        return true;
    }

    public void reset() {
        plant = null;
        environmentReady = false;
        growthStage = growthSpeed = yield = resilience = progress = scanLevel = 0;
        hybridBase = false;
        customData = new CompoundTag();
        setChanged();
    }

    public boolean isMature() { return plant != null && growthStage >= plant.getMaxGrowthStage(); }
    public ItemStack getSpecialDrop() { return plant == null ? ItemStack.EMPTY : plant.getSpecialDrop(this); }

    public void resetGrowthStage() {
        if (plant != null) growthStage = Math.clamp(plant.getStageAfterHarvest(this), 0, plant.getMaxGrowthStage());
        progress = 0;
        setChanged();
    }

    public List<ItemStack> getHarvestDrops() {
        if (plant == null || !plant.isHarvestable(this)) return new ArrayList<>();
        int count = Math.max(0, plant.calculateDropCount(this));
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < count; i++) for (ItemStack drop : plant.getGains(this)) {
            if (!drop.isEmpty()) result.add(drop.copy());
        }
        ItemStack special = plant.getSpecialDrop(this);
        if (!special.isEmpty()) result.add(special.copy());
        return result;
    }

    public List<ItemStack> performHarvest() { return doHarvest(); }

    @Override
    public void updateState() {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!level.getBlockState(worldPosition).is(state.getBlock())) return; // Never restore a block while onRemove is dropping seeds.
        BlockState nextState = state;
        int age = Math.clamp(growthStage, 0, 7);
        if (state.hasProperty(mio_icif_crop_stick.AGE)) {
            nextState = state.setValue(mio_icif_crop_stick.AGE, age);
        } else if (state.hasProperty(mio_icif_crop_stick_upgraded.AGE)) {
            nextState = state.setValue(mio_icif_crop_stick_upgraded.AGE, age);
        }
        if (nextState.hasProperty(mio_icif_crop_stick.CROSSING)) nextState = nextState.setValue(mio_icif_crop_stick.CROSSING, hybridBase);
        int signal = plant == null ? 0 : plant.getRedstoneSignal(this);
        int light = plant == null ? 0 : plant.getLightEmission(this);
        if (signal != lastSignal) { lastSignal = signal; level.updateNeighborsAt(worldPosition, state.getBlock()); }
        if (light != lastLight) { lastLight = light; level.getChunkSource().getLightEngine().checkBlock(worldPosition); }
        setChanged();
        if (nextState != state) {
            level.setBlock(worldPosition, nextState, Block.UPDATE_CLIENTS);
        } else {
            // Plant identity and statistics can change without changing AGE.
            level.sendBlockUpdated(worldPosition, state, nextState, Block.UPDATE_CLIENTS);
        }
    }

    private boolean findRoot(java.util.function.Predicate<BlockState> match) {
        if (level == null || plant == null) return false;
        for (int i = 1; i < plant.getRootDepth(this); i++) {
            BlockPos pos = worldPosition.below(i);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) break;
            if (match.test(state)) return true;
        }
        return false;
    }
    @Override public boolean isBlockBelow(Block block) { return findRoot(state -> state.is(block)); }
    @Override public boolean isBlockBelow(String id) {
        String tag = id;
        if (id.startsWith("ore") && !id.contains(":")) tag = "c:ores/" + id.substring(3).toLowerCase(java.util.Locale.ROOT);
        if (id.startsWith("block") && !id.contains(":")) tag = "c:storage_blocks/" + id.substring(5).toLowerCase(java.util.Locale.ROOT);
        ResourceLocation location = ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
        if (location == null) return false;
        var blockTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, location);
        var itemTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, location);
        return findRoot(state -> state.is(blockTag) || state.getBlock().asItem().builtInRegistryHolder().is(itemTag)
            || BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(location));
    }

    @Override
    public ItemStack makeSeeds(PlantType plant, int count, int growth, int gain, int resistance) {
        if (plant == null) return ItemStack.EMPTY;
        ItemStack seeds = CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), plant.getModId(), plant.getTypeId(),
            growth, gain, resistance);
        seeds.setCount(Math.clamp(count, 1, seeds.getMaxStackSize()));
        CropSeedItem.setScanLevel(seeds, scanLevel);
        return seeds;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String mod = tag.getString("PlantModId");
        String id = tag.getString("PlantId");
        plant = mod.isEmpty() || id.isEmpty() ? null : PlantRegistry.instance.getPlant(mod, id);
        growthStage = tag.getInt("GrowthStage");
        growthSpeed = tag.getInt("GrowthSpeed");
        yield = tag.getInt("Yield");
        resilience = tag.getInt("Resilience");
        nutrients = tag.contains("Nutrients") ? tag.getInt("Nutrients") : 0;
        water = tag.contains("Water") ? tag.getInt("Water") : 0;
        weedControl = tag.getInt("WeedControl");
        fertilizerCooldown = Math.max(0, tag.getInt("FertilizerCooldown"));
        progress = Math.max(0, tag.getInt("Progress"));
        // Legacy thresholds used incompatible units; preserve the plant and traits, restart only this stage.
        if (tag.getInt("CropCycleVersion") < 1 && plant != null && plant.usesIc2CropCycle()) progress = 0;
        scanLevel = tag.getInt("ScanLevel");
        hybridBase = tag.getBoolean("HybridBase");
        cropTicker = Math.floorMod(tag.getInt("CropTicker"), 10240);
        environmentReady = false;
        customData = tag.contains("CustomData") ? tag.getCompound("CustomData") : new CompoundTag();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (plant != null) {
            tag.putString("PlantModId", plant.getModId());
            tag.putString("PlantId", plant.getTypeId());
        }
        tag.putInt("CropCycleVersion", 1);
        tag.putInt("GrowthStage", growthStage);
        tag.putInt("GrowthSpeed", growthSpeed);
        tag.putInt("Yield", yield);
        tag.putInt("Resilience", resilience);
        tag.putInt("Nutrients", nutrients);
        tag.putInt("Water", water);
        tag.putInt("WeedControl", weedControl);
        tag.putInt("FertilizerCooldown", fertilizerCooldown);
        tag.putInt("Progress", progress);
        tag.putInt("ScanLevel", scanLevel);
        tag.putBoolean("HybridBase", hybridBase);
        tag.putInt("CropTicker", cropTicker);
        if (!customData.isEmpty()) tag.put("CustomData", customData.copy());
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }
}
