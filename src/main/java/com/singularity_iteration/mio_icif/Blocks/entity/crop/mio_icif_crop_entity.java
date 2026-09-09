package com.singularity_iteration.mio_icif.Blocks.entity.crop;

import com.singularity_iteration.mio_icif.Items.Crop.CropSeedItem;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.internal.crop.PlantHybridization;
import com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_crop_entity extends BlockEntity implements IPlanter {

    // 植物类型
    private PlantType plantType = null;

    // 成长阶段 (1 - maxStage)
    private int growthStage = 1;

    // 三属性?(0-31)
    private int growthSpeed = 0;
    private int yield = 0;
    private int resilience = 0;

    // 存储资源
    private int nutrients = 0;
    private int water = 0;
    private int weedControl = 0;

    // 成长
private int progress = 0;

    // 扫描等级
    private int scanLevel = 0;

    // 是否是杂交基
private boolean hybridBase = false;

    // 施肥冷却计时器（10?= 200 ticks
private int fertilizerCooldown = 0;
    public static final int FERTILIZER_COOLDOWN = 200;

    // 作物逻辑tick间隔（512 = 约25秒一次生长tick）
    public static final int CROP_TICK_RATE = 512;
    private int cropTicker = 0;

    // 自定义数
private CompoundTag customData = new CompoundTag();

 // 存的环境值（-1表示需要重新计算）
    private byte cachedHumidity = -1;
    private byte cachedSoilNutrients = -1;
    private byte cachedAirQuality = -1;

    public mio_icif_crop_entity(BlockPos pos, BlockState state) {
        this(com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.CROP_ENTITY_TYPE.get(), pos, state);
    }

    public mio_icif_crop_entity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state, mio_icif_crop_entity blockEntity) {
        if (level.isClientSide) return;

        if (fertilizerCooldown > 0) {
            fertilizerCooldown--;
        }

        cropTicker++;
        if (cropTicker % CROP_TICK_RATE != 0) return;

        // long time = level.getGameTime();
        cachedHumidity = updateHumidity();
        cachedSoilNutrients = updateSoilNutrients();
        cachedAirQuality = updateAirQuality();

        checkForWaterSource(level, pos);

        if (plantType == null) {
            if (!isUpgradedCropStick() || !PlantHybridization.tryHybridize(this)) {
                if (level.random.nextInt(100) == 0 && !hasWeedControl()) {
                    reset();
                    plantType = PlantRegistry.instance.getPlant("mio_icif", "weed");
                    growthStage = 1;
                    progress = 0;
                    updateState();
                } else {
                    if (weedControl > 0 && level.random.nextInt(10) == 0) {
                        weedControl--;
                    }
                    return;
                }
            }
        }

        if (plantType == null) return;

        plantType.tick(this);

        if (plantType.canGrow(this)) {
            progress += calcGrowthRate() * CROP_TICK_RATE;
            if (plantType == null) return;

            if (progress >= plantType.getGrowthTime(this)) {
                progress = 0;
                growthStage++;
                updateState();
            }
        }

        if (nutrients > 0) nutrients = Math.max(0, nutrients - CROP_TICK_RATE);
        if (water > 0) water = Math.max(0, water - CROP_TICK_RATE);

        if (plantType.isWeed(this) && level.random.nextInt(50) - growthSpeed <= 2) {
            generateWeed(level, pos);
        }
    }

    private int calcGrowthRate() {
        if (plantType == null) return 0;
        Level level = getLevel();
        if (level == null) return 0;

        int base = 2 + level.random.nextInt(5) + growthSpeed;
        int need = (plantType.getStats().getLevel() - 1) * 4 + growthSpeed + yield + resilience;
        if (need < 0) need = 0;

        int have = plantType.weightInfluences(this, getHumidity(), getSoilNutrients(), getAirQuality()) * 3;

        if (have >= need) {
            base = base * (100 + have - need) / 100;
        } else {
            int neg = (need - have) * 4;
            if (neg > 100 && level.random.nextInt(32) > resilience) {
                reset();
                base = 0;
            } else {
                base = base * (100 - neg) / 100;
                if (base < 0) base = 0;
            }
        }
        return base;
    }

    private boolean hasWeedControl() {
        if (weedControl > 0) {
            weedControl -= 5;
            if (weedControl < 0) weedControl = 0;
            return true;
        }
        return false;
    }

    private void generateWeed(Level level, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        switch (level.random.nextInt(4)) {
            case 0: x++; break;
            case 1: x--; break;
            case 2: z++; break;
            case 3: z--; break;
        }

        BlockPos neighborPos = new BlockPos(x, y, z);
        if (level.getBlockEntity(neighborPos) instanceof mio_icif_crop_entity neighbor) {
            PlantType neighborPlant = neighbor.getPlant();

            if (neighborPlant == null || (!neighborPlant.isWeed(neighbor) && level.random.nextInt(32) >= neighbor.getResilience() && !neighbor.hasWeedControl())) {
                int newGrowth = Math.max(this.growthSpeed, neighbor.growthSpeed);
                if (newGrowth < 31 && level.random.nextBoolean()) newGrowth++;

                neighbor.reset();
                neighbor.plantType = PlantRegistry.instance.getPlant("mio_icif", "weed");
                neighbor.growthStage = 1;
                neighbor.growthSpeed = newGrowth;
                neighbor.updateState();
            }
        } else if (level.isEmptyBlock(neighborPos)) {
            BlockState belowState = level.getBlockState(neighborPos.below());
            if (belowState.is(Blocks.DIRT) || belowState.is(Blocks.GRASS_BLOCK) || belowState.is(Blocks.FARMLAND)) {
                level.setBlock(neighborPos.below(), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(neighborPos, Blocks.TALL_GRASS.defaultBlockState(), 3);
            }
        }
    }

    private boolean isUpgradedCropStick() {
        if (level == null) return false;
        BlockState state = level.getBlockState(getBlockPos());
        return state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_crop_stick_upgraded;
    }

    private void checkForWaterSource(Level level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() == net.minecraft.world.level.block.Blocks.WATER) {
                        if (water < 100) {
                            water = Math.min(100, water + 10);
                            setChanged();
                        }
                        return;
                    }
                }
            }
        }
    }

    // ==================== IPlanter 实现 ====================

    @Override
    public PlantType getPlant() {
        return plantType;
    }

    @Override
    public void setPlant(PlantType plantType) {
        this.plantType = plantType;
        this.growthStage = 1;
        this.progress = 0;
        setChanged();
    }

    @Override
    public int getGrowthStage() {
        return growthStage;
    }

    @Override
    public void setGrowthStage(int stage) {
        this.growthStage = Math.max(1, stage);
        setChanged();
    }

    @Override
    public int getGrowthSpeed() {
        return growthSpeed;
    }

    @Override
    public void setGrowthSpeed(int speed) {
        this.growthSpeed = Math.max(0, Math.min(31, speed));
        setChanged();
    }

    @Override
    public int getYield() {
        return yield;
    }

    @Override
    public void setYield(int yield) {
        this.yield = Math.max(0, Math.min(31, yield));
        setChanged();
    }

    @Override
    public int getResilience() {
        return resilience;
    }

    @Override
    public void setResilience(int resilience) {
        this.resilience = Math.max(0, Math.min(31, resilience));
        setChanged();
    }

    @Override
    public int getNutrients() {
        return nutrients;
    }

    @Override
    public void setNutrients(int nutrients) {
        this.nutrients = Math.max(0, Math.min(100, nutrients));
        setChanged();
    }

    @Override
    public int getWater() {
        return water;
    }

    @Override
    public void setWater(int water) {
        this.water = Math.max(0, Math.min(100, water));
        setChanged();
    }

    @Override
    public int getWeedControl() {
        return weedControl;
    }

    @Override
    public void setWeedControl(int weedControl) {
        this.weedControl = Math.max(0, Math.min(100, weedControl));
        setChanged();
    }

    public int getFertilizerCooldown() {
        return fertilizerCooldown;
    }

    public void setFertilizerCooldown(int cooldown) {
        this.fertilizerCooldown = cooldown;
        setChanged();
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public void setProgress(int progress) {
        this.progress = progress;
        setChanged();
    }

    @Override
    public int getScanLevel() {
        return scanLevel;
    }

    @Override
    public void setScanLevel(int level) {
        this.scanLevel = Math.max(0, Math.min(4, level));
        setChanged();
    }

    @Override
    public boolean isHybridBase() {
        return hybridBase;
    }

    @Override
    public void setHybridBase(boolean hybridBase) {
        this.hybridBase = hybridBase;
        setChanged();
    }

    @Override
    public CompoundTag getCustomData() {
        return customData;
    }

    @Override
    public int getHumidity() {
        if (cachedHumidity < 0) {
            cachedHumidity = updateHumidity();
        }
        return cachedHumidity;
    }

    @Override
    public int getSoilNutrients() {
        if (cachedSoilNutrients < 0) {
            cachedSoilNutrients = updateSoilNutrients();
        }
        return cachedSoilNutrients;
    }

    @Override
    public int getAirQuality() {
        if (cachedAirQuality < 0) {
            cachedAirQuality = updateAirQuality();
        }
        return cachedAirQuality;
    }

    private byte updateHumidity() {
        Level level = getLevel();
        if (level == null) return 0;

        int value = getHumidityBiomeBonus(level, getBlockPos());

        // 下方方块湿度加成（耕地湿润度）
        BlockState belowState = level.getBlockState(getBlockPos().below());
        if (belowState.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE)) {
            int moisture = belowState.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE);
            if (moisture >= 7) {
                value += 2;
            }
        }

        // 水分存储加成
        if (water >= 5) {
            value += 2;
        }
        value += (water + 24) / 25;

        return (byte) value;
    }

    private byte updateSoilNutrients() {
        Level level = getLevel();
        if (level == null) return 0;

        int value = getNutrientBiomeBonus(level, getBlockPos());

        // 下方泥土层数加成（最?层）
        for (int i = 2; i < 5; i++) {
            if (level.getBlockState(getBlockPos().below(i)).is(Blocks.DIRT)) {
                value++;
            } else {
                break;
            }
        }

        // 营养存储加成
        value += (nutrients + 19) / 20;

        return (byte) value;
    }

    private byte updateAirQuality() {
        Level level = getLevel();
        if (level == null) return 0;

        int value = 0;

        // 高度加成（每15?1，最?
    int height = (getBlockPos().getY() - 64) / 15;
        if (height > 4) height = 4;
        if (height < 0) height = 0;
        value += height;

        // 周围空间加成（检?x3范围内空旷度
    int fresh = 9;
        for (int x = -1; x <= 1 && fresh > 0; x++) {
            for (int z = -1; z <= 1 && fresh > 0; z++) {
                BlockPos checkPos = getBlockPos().offset(x, 0, z);
                if (!level.isEmptyBlock(checkPos) || level.getBlockEntity(checkPos) instanceof mio_icif_crop_entity) {
                    fresh--;
                }
            }
        }
        value += fresh / 2;

        // 上方光照加成
        if (level.canSeeSky(getBlockPos().above())) {
            value += 2;
        }

        return (byte) value;
    }

    private int getHumidityBiomeBonus(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        int bonus = 0;

        if (biome.is(BiomeTags.IS_JUNGLE)) bonus = Math.max(bonus, 3);
        if (biome.is(BiomeTags.HAS_CLOSER_WATER_FOG)) bonus = Math.max(bonus, 3);
        if (biome.is(BiomeTags.IS_TAIGA)) bonus = Math.max(bonus, 2);
        if (biome.is(BiomeTags.IS_FOREST)) bonus = Math.max(bonus, 1);
        if (biome.is(BiomeTags.IS_RIVER)) bonus = Math.max(bonus, 1);
        if (biome.is(BiomeTags.IS_SAVANNA)) bonus = Math.max(bonus, -1);
        if (biome.is(BiomeTags.IS_HILL)) bonus = Math.max(bonus, -2);
        if (biome.is(BiomeTags.IS_MOUNTAIN)) bonus = Math.max(bonus, -2);
        if (biome.is(BiomeTags.IS_BADLANDS)) bonus = Math.max(bonus, -3);
        if (biome.is(BiomeTags.IS_NETHER)) bonus = Math.max(bonus, -4);
        if (biome.is(BiomeTags.IS_END)) bonus = Math.max(bonus, -4);

        return bonus;
    }

    private int getNutrientBiomeBonus(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        int bonus = 0;

        if (biome.is(BiomeTags.IS_JUNGLE)) bonus = Math.max(bonus, 10);
        if (biome.is(BiomeTags.HAS_CLOSER_WATER_FOG)) bonus = Math.max(bonus, 10);
        if (biome.is(BiomeTags.IS_TAIGA)) bonus = Math.max(bonus, 5);
        if (biome.is(BiomeTags.IS_FOREST)) bonus = Math.max(bonus, 5);
        if (biome.is(BiomeTags.IS_RIVER)) bonus = Math.max(bonus, 2);
        if (biome.is(BiomeTags.IS_SAVANNA)) bonus = Math.max(bonus, -2);
        if (biome.is(BiomeTags.IS_HILL)) bonus = Math.max(bonus, -5);
        if (biome.is(BiomeTags.IS_MOUNTAIN)) bonus = Math.max(bonus, -5);
        if (biome.is(BiomeTags.IS_BADLANDS)) bonus = Math.max(bonus, -8);
        if (biome.is(BiomeTags.IS_NETHER)) bonus = Math.max(bonus, -10);
        if (biome.is(BiomeTags.IS_END)) bonus = Math.max(bonus, -10);

        return bonus;
    }

    @Override
    public Level getPlanterWorld() {
        return getLevel();
    }

    @Override
    public BlockPos getPlanterPos() {
        return getBlockPos();
    }

    @Override
    public int getLightLevel() {
        Level level = getLevel();
        if (level == null) return 0;
        return level.getMaxLocalRawBrightness(getBlockPos());
    }

    @Override
    public boolean pick() {
        // 手动收获
        return doManualHarvest();
    }

    @Override
    public boolean doManualHarvest() {
        if (plantType == null || !plantType.isHarvestable(this)) {
            return false;
        }

        Level level = getLevel();
        BlockPos pos = getBlockPos();

        // 收获产物
        List<ItemStack> harvestDrops = doHarvest();
        boolean bonus = !harvestDrops.isEmpty() && harvestDrops.stream().anyMatch(s -> !s.isEmpty());

        // 掉落收获
    for (ItemStack drop : harvestDrops) {
            if (!drop.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                level.addFreshEntity(itemEntity);
            }
        }

        // 种子掉落概率计算（基于IC2的pick机制
    float firstChance = plantType.dropSeedChance(this);
        for (int i = 0; i < resilience; i++) {
            firstChance *= 1.1F;
        }

        int seedDrop = 0;

        if (bonus) {
            // 有收获产物时
            if (level.random.nextFloat() <= (firstChance + 1.0F) * 0.8F) {
                seedDrop++;
            }
            // 第二颗种子概
        float secondChance = plantType.dropSeedChance(this) + growthSpeed / 100.0F;
            for (int k = 23; k < yield; k++) {
                secondChance *= 0.95F;
            }
            if (level.random.nextFloat() <= secondChance) {
                seedDrop++;
            }
        } else {
            // 没有收获产物
        if (level.random.nextFloat() <= firstChance * 1.5F) {
                seedDrop++;
            }
        }

        // 掉落种子
        for (int j = 0; j < seedDrop; j++) {
            ItemStack seedStack = makeSeeds(plantType, growthStage, growthSpeed, yield, resilience);
            if (!seedStack.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity seedEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, seedStack);
                level.addFreshEntity(seedEntity);
            }
        }

        // 重置到收获后大小
        growthStage = plantType.getStageAfterHarvest();
        progress = 0;
        updateState();

        return true;
    }

    @Override
    public List<ItemStack> doHarvest() {
        if (plantType == null) {
            return List.of();
        }
        return List.of(plantType.getHarvest(this));
    }

    @Override
    public boolean doHarvestWithoutSeeds() {
        if (plantType == null || !plantType.isHarvestable(this)) {
            return false;
        }

        Level level = getLevel();
        BlockPos pos = getBlockPos();

        // 收获产物
        List<ItemStack> harvestDrops = doHarvest();

        // 只掉落收获物，不掉落种子
        for (ItemStack drop : harvestDrops) {
            if (!drop.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                level.addFreshEntity(itemEntity);
            }
        }

        // 重置到收获后大小
        growthStage = plantType.getStageAfterHarvest();
        progress = 0;
        updateState();

        return true;
    }

    @Override
    public void reset() {
        plantType = null;
        growthStage = 1;
        growthSpeed = 0;
        yield = 0;
        resilience = 0;
        nutrients = 0;
        water = 0;
        weedControl = 0;
        progress = 0;
        scanLevel = 0;
        hybridBase = false;
        customData = new CompoundTag();
        cachedHumidity = -1;
        cachedSoilNutrients = -1;
        cachedAirQuality = -1;
        updateState();
    }

    /**
     * 检查作物是否成
 * @return 是否已成
 */
    public boolean isMature() {
        if (plantType == null) {
            return false;
        }
        return growthStage >= plantType.getMaxGrowthStage();
    }

    /**
     * 获取特殊产物（用于作物分析仪功能
 * 如粘性树脂、毒马铃薯等第二阶段产物
     * @return 特殊产物，如果没有则返回空物品栈
     */
    public ItemStack getSpecialDrop() {
        if (plantType == null) {
            return ItemStack.EMPTY;
        }
        // 调用植物类型的特殊产物获取方
    return plantType.getSpecialDrop(this);
    }

    /**
     * 重置生长阶段但不移除作物
     * 用于收割特殊产物后让作物继续生长
     */
    public void resetGrowthStage() {
        if (plantType != null) {
            growthStage = plantType.getStageAfterHarvest();
        } else {
            growthStage = 1;
        }
        progress = 0;
        updateState();
    }

    /**
     * 获取收割产物列表
     * @return 产物列表
     */
    public List<ItemStack> getHarvestDrops() {
        if (plantType == null) {
            return List.of();
        }
        return List.of(plantType.getHarvest(this));
    }

    /**
     * 执行收割操作（原版IC2的performHarvest）
     * 返回产物列表，并重置作物到收获后阶段
     * 注意：不返回种子，只返回产物
     * @return 产物列表，如果不能收割则返回null
     */
    public List<ItemStack> performHarvest() {
        if (plantType == null || !plantType.canBeHarvested(this)) {
            return null;
        }

        // 计算产物数量（使用高斯分布）
        int dropCount = plantType.calculateDropCount(this);

        // 收集产物（使用getGains，只返回产物，不返回种子）
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < dropCount; i++) {
            ItemStack[] gains = plantType.getGains(this);
            for (ItemStack gain : gains) {
                if (!gain.isEmpty()) {
                    drops.add(gain.copy());
                }
            }
        }

        // 合并相同物品
        List<ItemStack> mergedDrops = new ArrayList<>();
        for (ItemStack drop : drops) {
            boolean merged = false;
            for (ItemStack existing : mergedDrops) {
                if (ItemStack.isSameItemSameComponents(existing, drop)) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space >= drop.getCount()) {
                        existing.grow(drop.getCount());
                        merged = true;
                        break;
                    } else if (space > 0) {
                        existing.grow(space);
                        drop.shrink(space);
                    }
                }
            }
            if (!merged && !drop.isEmpty()) {
                mergedDrops.add(drop);
            }
        }

        // 重置作物到收获后阶段
        growthStage = plantType.getStageAfterHarvest();
        progress = 0;
        setChanged();
        updateState();

        return mergedDrops;
    }

    @Override
    public void updateState() {
        setChanged();
        Level level = getLevel();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean isBlockBelow(net.minecraft.world.level.block.Block block) {
        Level level = getLevel();
        if (level == null) return false;
        // 检查作物架正下方（耕地）和耕地下方（矿石）
        // 作物?-> 耕地 -> 矿石
        BlockPos cropPos = getBlockPos();
        BlockPos farmlandPos = cropPos.below();
        BlockPos belowFarmlandPos = farmlandPos.below();
        return level.getBlockState(farmlandPos).is(block) ||
               level.getBlockState(belowFarmlandPos).is(block);
    }

    @Override
    public boolean isBlockBelow(String oredictName) {
        // TODO: 实现矿物词典检
    return false;
    }

    @Override
    public ItemStack makeSeeds(PlantType plantType, int stage, int growthSpeed, int yield, int resilience) {
        if (plantType == null) return ItemStack.EMPTY;
        return CropSeedItem.createSeedStack(
                mio_icif_normal.CROP_SEED.get(),
                plantType.getModId(),
                plantType.getTypeId(),
                growthSpeed,
                yield,
                resilience
        );
    }

    // ==================== NBT 保存/加载 ====================

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 加载植物类型
        if (tag.contains("PlantId") && tag.contains("PlantModId")) {
            String plantId = tag.getString("PlantId");
            String plantModId = tag.getString("PlantModId");
            plantType = PlantRegistry.instance.getPlant(plantModId, plantId);
        }

        growthStage = tag.getInt("GrowthStage");
        growthSpeed = tag.getInt("GrowthSpeed");
        yield = tag.getInt("Yield");
        resilience = tag.getInt("Resilience");
        nutrients = tag.getInt("Nutrients");
        water = tag.getInt("Water");
        weedControl = tag.getInt("WeedControl");
        progress = tag.getInt("Progress");
        scanLevel = tag.getInt("ScanLevel");
        hybridBase = tag.getBoolean("HybridBase");
        cropTicker = tag.getInt("CropTicker");

        if (tag.contains("CustomData")) {
            customData = tag.getCompound("CustomData");
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 保存植物类型
        if (plantType != null) {
            tag.putString("PlantId", plantType.getTypeId());
            tag.putString("PlantModId", plantType.getModId());
        }

        tag.putInt("GrowthStage", growthStage);
        tag.putInt("GrowthSpeed", growthSpeed);
        tag.putInt("Yield", yield);
        tag.putInt("Resilience", resilience);
        tag.putInt("Nutrients", nutrients);
        tag.putInt("Water", water);
        tag.putInt("WeedControl", weedControl);
        tag.putInt("Progress", progress);
        tag.putInt("ScanLevel", scanLevel);
        tag.putBoolean("HybridBase", hybridBase);
        tag.putInt("CropTicker", cropTicker);
        tag.put("CustomData", customData);
    }

    // ==================== 客户端同?====================

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        // 保存植物类型（null表示没有植物
    if (plantType != null) {
            tag.putString("PlantId", plantType.getTypeId());
            tag.putString("PlantModId", plantType.getModId());
        } else {
            tag.putString("PlantId", "");
            tag.putString("PlantModId", "");
        }
        tag.putInt("GrowthStage", growthStage);
        tag.putInt("GrowthSpeed", growthSpeed);
        tag.putInt("Yield", yield);
        tag.putInt("Resilience", resilience);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        // 客户端接收同步数
    String plantModId = tag.getString("PlantModId");
        String plantId = tag.getString("PlantId");

        if (!plantModId.isEmpty() && !plantId.isEmpty()) {
            plantType = com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry.instance.getPlant(plantModId, plantId);
        } else {
            plantType = null; // 清除植物
        }

        growthStage = tag.getInt("GrowthStage");
        growthSpeed = tag.getInt("GrowthSpeed");
        yield = tag.getInt("Yield");
        resilience = tag.getInt("Resilience");
    }
}