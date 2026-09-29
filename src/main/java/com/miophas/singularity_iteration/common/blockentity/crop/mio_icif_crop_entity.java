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
    public static final int CROP_TICK_RATE = 512;

    private PlantType plant;
    private int growthStage;
    private int growthSpeed;
    private int yield;
    private int resilience;
    private int nutrients = 100;
    private int water = 100;
    private int weedControl;
    private int fertilizerCooldown;
    private int progress;
    private int scanLevel;
    private boolean hybridBase;
    private int cropTicker;
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
        if (plant != null) plant.tick(this);
        cropTicker = (cropTicker + 1) % CROP_TICK_RATE;
        markChunkUnsaved();
        if (cropTicker == 0) {
            if (plant == null && hybridBase) PlantHybridization.tryHybridize(this);
            updateState();
        }
    }

    @Override public PlantType getPlant() { return plant; }
    @Override public void setPlant(PlantType plant) { if (this.plant != plant) { this.plant = plant; setChanged(); } }
    @Override public int getGrowthStage() { return growthStage; }
    @Override public void setGrowthStage(int value) { int next = Math.clamp(value, 0, plant == null ? 7 : plant.getMaxGrowthStage()); if (growthStage != next) { growthStage = next; setChanged(); } }
    @Override public int getGrowthSpeed() { return growthSpeed; }
    @Override public void setGrowthSpeed(int value) { int next = Math.clamp(value, 0, 31); if (growthSpeed != next) { growthSpeed = next; setChanged(); } }
    @Override public int getYield() { return yield; }
    @Override public void setYield(int value) { int next = Math.clamp(value, 0, 31); if (yield != next) { yield = next; setChanged(); } }
    @Override public int getResilience() { return resilience; }
    @Override public void setResilience(int value) { int next = Math.clamp(value, 0, 31); if (resilience != next) { resilience = next; setChanged(); } }
    @Override public int getNutrients() { return nutrients; }
    @Override public void setNutrients(int value) { int next = Math.clamp(value, 0, 100); if (nutrients != next) { nutrients = next; setChanged(); } }
    @Override public int getWater() { return water; }
    @Override public void setWater(int value) { int next = Math.clamp(value, 0, 100); if (water != next) { water = next; setChanged(); } }
    @Override public int getWeedControl() { return weedControl; }
    @Override public void setWeedControl(int value) { int next = Math.clamp(value, 0, 100); if (weedControl != next) { weedControl = next; setChanged(); } }
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

    @Override public int getHumidity() { return 5; }
    @Override public int getSoilNutrients() { return level != null && level.getBlockState(worldPosition.below()).is(Blocks.FARMLAND) ? 3 : 0; }
    @Override public int getAirQuality() { return Math.clamp((worldPosition.getY() + 15) / 15, 0, 10); }
    @Override public Level getPlanterWorld() { return level; }
    @Override public BlockPos getPlanterPos() { return worldPosition; }
    @Override
    public int getLightLevel() {
        Level currentLevel = level;
        if (currentLevel == null) return 0;
        long gameTime = currentLevel.getGameTime();
        if (lightCacheTick != gameTime) {
            lightCacheValue = currentLevel.getMaxLocalRawBrightness(worldPosition.above());
            lightCacheTick = gameTime;
        }
        return lightCacheValue;
    }

    @Override
    public boolean pick() {
        if (plant == null) return false;
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
        if (level != null && level.random.nextFloat() < plant.dropSeedChance(this)) {
            ItemStack seed = plant.getSeedItem(this);
            if (!seed.isEmpty()) drops.add(seed);
        }
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
        growthStage = growthSpeed = yield = resilience = progress = scanLevel = 0;
        hybridBase = false;
        customData = new CompoundTag();
        setChanged();
    }

    public boolean isMature() { return plant != null && growthStage >= plant.getMaxGrowthStage(); }
    public ItemStack getSpecialDrop() { return plant == null ? ItemStack.EMPTY : plant.getSpecialDrop(this); }

    public void resetGrowthStage() {
        if (plant != null) growthStage = Math.clamp(plant.getStageAfterHarvest(), 0, plant.getMaxGrowthStage());
        progress = 0;
        setChanged();
    }

    public List<ItemStack> getHarvestDrops() {
        if (plant == null || !plant.isHarvestable(this)) return new ArrayList<>();
        int count = Math.max(1, plant.calculateDropCount(this));
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack template : plant.getGains(this)) {
            if (template.isEmpty()) continue;
            ItemStack stack = template.copy();
            stack.setCount(Math.min(stack.getMaxStackSize(), Math.max(1, stack.getCount() * count)));
            result.add(stack);
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
        BlockState nextState = state;
        int age = Math.clamp(growthStage, 0, 7);
        if (state.hasProperty(mio_icif_crop_stick.AGE)) {
            nextState = state.setValue(mio_icif_crop_stick.AGE, age);
        } else if (state.hasProperty(mio_icif_crop_stick_upgraded.AGE)) {
            nextState = state.setValue(mio_icif_crop_stick_upgraded.AGE, age);
        }
        setChanged();
        if (nextState != state) {
            level.setBlock(worldPosition, nextState, Block.UPDATE_CLIENTS);
        } else {
            // Plant identity and statistics can change without changing AGE.
            level.sendBlockUpdated(worldPosition, state, nextState, Block.UPDATE_CLIENTS);
        }
    }

    @Override public boolean isBlockBelow(Block block) { return level != null && level.getBlockState(worldPosition.below()).is(block); }
    @Override public boolean isBlockBelow(String id) {
        if (level == null) return false;
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(worldPosition.below()).getBlock())
            .equals(ResourceLocation.parse(id));
    }

    @Override
    public ItemStack makeSeeds(PlantType plant, int count, int growth, int gain, int resistance) {
        if (plant == null) return ItemStack.EMPTY;
        return CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), plant.getModId(), plant.getTypeId(),
            growth, gain, resistance);
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
        nutrients = tag.contains("Nutrients") ? tag.getInt("Nutrients") : 100;
        water = tag.contains("Water") ? tag.getInt("Water") : 100;
        weedControl = tag.getInt("WeedControl");
        fertilizerCooldown = Math.max(0, tag.getInt("FertilizerCooldown"));
        progress = tag.getInt("Progress");
        scanLevel = tag.getInt("ScanLevel");
        hybridBase = tag.getBoolean("HybridBase");
        cropTicker = tag.getInt("CropTicker");
        customData = tag.contains("CustomData") ? tag.getCompound("CustomData") : new CompoundTag();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (plant != null) {
            tag.putString("PlantModId", plant.getModId());
            tag.putString("PlantId", plant.getTypeId());
        }
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
