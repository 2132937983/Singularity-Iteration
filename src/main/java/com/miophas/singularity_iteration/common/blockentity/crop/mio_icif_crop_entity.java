// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.crop;

import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick;
import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick_upgraded;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.item.crop.CropSeedItem;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.runtime.crop.CropGrowthMath;
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

/**
 * <p><b>EN:</b> Crop planter block entity (crop sticks). It stores the plant, the three traits,
 * the resources (water, nutrients, Weed-EX) and the growth points. Every 256 ticks it runs one
 * IC2 crop cycle ({@link #performCropCycle()}); the formulas are in {@link CropGrowthMath}.
 * Clients get an update only when a visible value changes (see {@link #syncAfterCycle()}).
 *
 * <p><b>ZH:</b> 作物架方块实体。保存植物、三项属性、资源（水、营养、除草剂）与生长点。每 256 tick
 * 运行一次 IC2 作物周期（{@link #performCropCycle()}），公式见 {@link CropGrowthMath}。
 * 只有可见数值变化时才向客户端发送更新（见 {@link #syncAfterCycle()}）。
 *
 * <p><b>JA:</b> 作物棒のブロックエンティティ。植物、3 つの特性値、資源（水・栄養・除草剤）、成長ポイントを保存する。
 * 256 tick ごとに IC2 作物サイクル（{@link #performCropCycle()}）を 1 回実行する。計算式は {@link CropGrowthMath}。
 * 見える値が変わったときだけクライアントへ更新を送る（{@link #syncAfterCycle()} を参照）。
 */
public class mio_icif_crop_entity extends BlockEntity implements IPlanter {
    public static final int FERTILIZER_COOLDOWN = 200;
    public static final int CROP_TICK_RATE = CropGrowthMath.CYCLE_TICKS;

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
            // EN: The counter is saved, so the chunk must stay dirty. markChunkUnsaved() does
            //     this without the comparator neighbour update of setChanged().
            // ZH: 该计数会被保存，因此区块必须保持“未保存”状态。markChunkUnsaved() 不触发 setChanged() 的比较器邻居更新。
            // JA: このカウンタは保存対象なので、チャンクを未保存のままにする。markChunkUnsaved() は
            //     setChanged() のコンパレータ隣接更新を起こさない。
            markChunkUnsaved();
        }
        if (plant != null && !plant.usesIc2CropCycle()) plant.tick(this);
        cropTicker = (cropTicker + 1) % 10240;
        if (cropTicker % CROP_TICK_RATE == 0) {
            performCropCycle();
            syncAfterCycle();
        }
    }

    /**
     * <p><b>EN:</b> One IC2 crop cycle; also used by server integration tests. The random calls
     * keep the IC2 order: base growth roll first, then the death roll (only for a lethal penalty).
     * <p><b>ZH:</b> 一个 IC2 作物周期，服务器集成测试也会调用。随机调用保持 IC2 顺序：先掷基础生长，再掷死亡（仅在惩罚致命时）。
     * <p><b>JA:</b> IC2 作物サイクル 1 回分。サーバー統合テストからも呼ばれる。乱数の順序は IC2 と同じ：
     * まず基礎成長、次に枯死判定（致命的なペナルティのときだけ）。
     */
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
            int required = CropGrowthMath.requiredQuality(plant.getStats().getLevel(), growthSpeed, yield, resilience);
            int quality = plant.weightInfluences(this, getHumidity(), getSoilNutrients(), getAirQuality()) * 5;
            // EN: 0.1.8.2: a crop that a Cropmatron tends gets at least the requirement + margin.
            // ZH: 0.1.8.2：作物监护机照料的作物至少获得需求 + 余量的品质。
            // JA: 0.1.8.2：作物管理機の管理下の作物は、少なくとも要求＋余裕の品質を得る。
            if (isTended()) quality = CropGrowthMath.tendedQuality(quality, required);
            int penalty = CropGrowthMath.penalty(required, quality);
            if (penalty > CropGrowthMath.LETHAL_PENALTY && CropGrowthMath.dies(penalty, resilience, level.random.nextInt(32))) {
                reset();
                return;
            }
            setProgress(progress + CropGrowthMath.growthPoints(base, required, quality));
            if (progress >= plant.getGrowthTime(this)) {
                setProgress(0);
                setGrowthStage(growthStage + 1);
            }
        }
        // EN: Resource decay changes saved values only. The client sees them through the
        //     resource bucket in syncAfterCycle(), so the setters (and setChanged) are not used.
        // ZH: 资源衰减只改变需保存的数值。客户端通过 syncAfterCycle() 中的资源档位看到它们，因此不调用 setter（及 setChanged）。
        // JA: 資源の減少は保存値だけを変える。クライアントには syncAfterCycle() の資源段階で伝わるので、
        //     セッター（と setChanged）は使わない。
        if (nutrients > 0) nutrients--;
        if (water > 0) water--;
        markChunkUnsaved();
        if (plant.isWeed(this) && level.random.nextInt(50) - growthSpeed <= 2) spreadWeed();
    }

    /** EN: Signature of the client-visible state last sent. ZH: 上次发送的客户端可见状态签名。JA: 最後に送ったクライアント可視状態の署名。 */
    private int syncedSignature = Integer.MIN_VALUE;

    /**
     * <p><b>EN:</b> Sends the crop to clients only when a visible value changed: plant, stage,
     * traits, scan level, crossing flag, resource bucket, redstone signal or light. A farm of
     * 1,000 crops thus sends about 1/20 of the former block-entity packets. The chunk is still
     * marked unsaved, so no value is lost.
     * <p><b>ZH:</b> 仅当可见数值（植物、阶段、属性、扫描等级、杂交标志、资源档位、红石信号、光照）变化时才向客户端发送。
     * 1000 株作物的农场因此只发送原先约 1/20 的方块实体数据包。区块仍被标记为未保存，不会丢失数据。
     * <p><b>JA:</b> 見える値（植物、段階、特性値、スキャン段階、交配フラグ、資源段階、レッドストーン信号、光）が
     * 変わったときだけクライアントへ送る。作物 1,000 本の農場では、送るブロックエンティティパケットが従来の約 1/20 になる。
     * チャンクは未保存として印を付けるので、値は失われない。
     */
    private void syncAfterCycle() {
        if (visibleSignature() != syncedSignature) updateState();
        else markChunkUnsaved();
    }

    private int visibleSignature() {
        int h = plant == null ? 0 : plant.hashCode();
        h = 31 * h + growthStage;
        h = 31 * h + growthSpeed;
        h = 31 * h + yield;
        h = 31 * h + resilience;
        h = 31 * h + scanLevel;
        h = 31 * h + (hybridBase ? 1 : 0);
        h = 31 * h + CropGrowthMath.resourceBucket(water, nutrients, weedControl);
        h = 31 * h + (plant == null ? 0 : plant.getRedstoneSignal(this));
        return 31 * h + (plant == null ? 0 : plant.getLightEmission(this));
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
        if (!level.isLoaded(pos)) return;
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
    /**
     * <p><b>EN:</b> Sets the plant. 0.1.8.2 invariant: a planter with a plant is never a crossing base,
     * so this also clears the crossing flag (and gives the second stick back to nobody: the
     * crossing itself used it up).
     * <p><b>ZH:</b> 设置植物。0.1.8.2 不变量：有植物的作物架绝不是杂交基座，因此同时清除杂交标志（第二根作物架已被杂交过程消耗）。
     * <p><b>JA:</b> 植物を設定する。0.1.8.2 の不変条件：植物のある作物棒は交配台ではない。そのため交配フラグも消す（2 本目の棒は交配で消費済み）。
     */
    @Override public void setPlant(PlantType plant) {
        if (plant != null && hybridBase) { hybridBase = false; crossStick = ItemStack.EMPTY; setChanged(); }
        if (this.plant != plant) { this.plant = plant; environmentReady = false; setChanged(); }
    }

    // ---------------------------------------------------------------- 0.1.8.2: tending and crossing stick
    /** EN: Game time until which a Cropmatron tends this crop. ZH: 作物监护机照料本作物直到的游戏时间。JA: 作物管理機がこの作物を管理する期限のゲーム時刻。 */
    private long tendedUntil;
    /**
     * <p><b>EN:</b> The stick item that made this planter a crossing base. Removing the second stick
     * gives back exactly this item. Before 0.1.8.2 the block item was returned, so a normal stick
     * placed on an upgraded stick came back as an upgraded stick (and the reverse lost one).
     * <p><b>ZH:</b> 使本作物架成为杂交基座的作物架物品。移除第二根时原样返还该物品。0.1.8.2 之前返还的是方块物品，
     * 因此放在升级作物架上的普通作物架会以升级作物架的形式返还（反过来则会丢失一根）。
     * <p><b>JA:</b> この作物棒を交配台にした棒のアイテム。2 本目を外すとこのアイテムをそのまま返す。0.1.8.2 より前はブロックのアイテムを返していたため、
     * 強化作物棒に置いた通常の棒が強化作物棒として戻った（逆の場合は 1 本失われた）。
     */
    private ItemStack crossStick = ItemStack.EMPTY;

    @Override public void markTended(long untilGameTime) {
        if (untilGameTime > tendedUntil) { tendedUntil = untilGameTime; markChunkUnsaved(); }
    }
    @Override public boolean isTended() { return level != null && level.getGameTime() < tendedUntil; }

    /** EN: Records the stick that made the crossing base. ZH: 记录形成杂交基座的作物架。JA: 交配台にした棒を記録する。 */
    public void setCrossStick(ItemStack stick) {
        crossStick = stick == null || stick.isEmpty() ? ItemStack.EMPTY : stick.copyWithCount(1);
        setChanged();
    }

    /**
     * <p><b>EN:</b> Removes the crossing base and returns the stick to give back (the recorded item, or the block item for old saves).
     * <p><b>ZH:</b> 移除杂交基座并返回应返还的作物架（记录的物品；旧存档为方块物品）。
     * <p><b>JA:</b> 交配台を解除し、返す棒を返す（記録したアイテム。旧セーブではブロックのアイテム）。
     */
    public ItemStack takeCrossStick() {
        ItemStack stick = crossStick.isEmpty() ? new ItemStack(getBlockState().getBlock()) : crossStick.copy();
        crossStick = ItemStack.EMPTY;
        hybridBase = false;
        setChanged();
        return stick;
    }
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
    @Override public void setHybridBase(boolean value) {
        if (!value) crossStick = ItemStack.EMPTY;
        if (hybridBase != value) { hybridBase = value; setChanged(); }
    }
    @Override public CompoundTag getCustomData() { return customData; }

    private void refreshEnvironment() {
        if (level == null) return;
        boolean initial = !environmentReady;
        var biome = level.getBiome(worldPosition);
        if (initial || cropTicker % 10240 == 0) {
            float rain = biome.value().getModifiedClimateSettings().downfall();
            // 0.1.8.0: access transformer (META-INF/accesstransformer.cfg) instead of the invoker mixin
            float temp = biome.value().getTemperature(worldPosition);
            biomeHumidity = CropGrowthMath.biomeHumidity(rain, temp);
        }
        if (initial || cropTicker % 1024 == 0) {
            BlockState soil = level.getBlockState(worldPosition.below());
            boolean wet = soil.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE)
                && soil.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) >= 7;
            humidity = CropGrowthMath.humidity(biomeHumidity, wet, water);
        }
        if (initial || (cropTicker + 256) % 1024 == 0) {
            // EN: 0.1.8.1 fix. below(1) is always the farmland (crop sticks need farmland), so the
            //     old loop from i = 1 stopped at once and the dirt bonus was always 0. IC2 counts
            //     the dirt blocks under the farmland: below(2) to below(4).
            // ZH: 0.1.8.1 修复。below(1) 总是耕地（作物架必须放在耕地上），旧循环从 i = 1 开始会立即停止，
            //     泥土加成始终为 0。IC2 统计耕地下方的泥土方块：below(2) 至 below(4)。
            // JA: 0.1.8.1 修正。below(1) は常に耕地（作物棒は耕地が必要）なので、i = 1 から始める旧ループは
            //     すぐに止まり、土ボーナスは常に 0 だった。IC2 は耕地の下の土ブロック below(2)〜below(4) を数える。
            int dirt = 0;
            for (int i = 2; i < 5 && level.getBlockState(worldPosition.below(i)).is(Blocks.DIRT); i++) dirt++;
            soilNutrients = CropGrowthMath.soilNutrients(
                com.miophas.singularity_iteration.common.crop.CropEnvironment.nutrientBonus(biome), dirt, nutrients);
        }
        if (initial || (cropTicker + 512) % 1024 == 0) {
            int fresh = 9;
            // Preserve the Experimental source's asymmetric 2x2 neighborhood.
            for (int x = -1; x < 1; x++) for (int z = -1; z < 1; z++) {
                BlockPos pos = worldPosition.offset(x, 0, z);
                if (!level.isLoaded(pos) || level.getBlockState(pos).isSolidRender(level, pos)
                        || level.getBlockEntity(pos) instanceof IPlanter) fresh--;
            }
            airQuality = CropGrowthMath.airQuality(worldPosition.getY(), fresh, level.canSeeSky(worldPosition.above()));
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

    /**
     * <p><b>EN:</b> Manual harvest without a player (API callers): the drops spawn on top of the crop stick.
     * <p><b>ZH:</b> 无玩家的手动收获（API 调用者）：掉落物生成在作物架上方。
     * <p><b>JA:</b> プレイヤーなしの手動収穫（API 呼び出し側）：ドロップは作物棒の上に出る。
     */
    @Override
    public boolean doManualHarvest() {
        List<ItemStack> drops = doHarvest();
        if (drops.isEmpty() || level == null) return false;
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                // EN: Spawn above the stick, not inside its collision box.
                // ZH: 生成在作物架上方，而不是其碰撞箱内。
                // JA: 棒の当たり判定の中ではなく上に出す。
                ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.05D,
                    worldPosition.getZ() + 0.5D, drop, 0, 0.12, 0);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
            }
        }
        return true;
    }

    /**
     * <p><b>EN:</b> Right-click harvest for a player (0.1.8.2). The drops go into the player's inventory
     * ({@code ItemHandlerHelper.giveItemToPlayer}); the rest drops at the player's feet. Before, the
     * drops spawned inside the crop stick, and a harvest could give nothing (IC2 drop roll of 0 or an
     * unproductive stage) while it still reset the crop: players saw the harvest vanish.
     * <p><b>ZH:</b> 玩家的右键收获（0.1.8.2）。掉落物放入玩家物品栏（{@code ItemHandlerHelper.giveItemToPlayer}），其余掉在玩家脚下。
     * 此前掉落物生成在作物架内部，且收获可能一无所获（IC2 掉落次数为 0 或处于无产出阶段）却仍重置作物，玩家看到的是收获物“消失”。
     * <p><b>JA:</b> プレイヤーの右クリック収穫（0.1.8.2）。ドロップはプレイヤーのインベントリに入り（{@code ItemHandlerHelper.giveItemToPlayer}）、
     * 残りは足元に落ちる。以前はドロップが作物棒の内部に出て、しかも何も出ない収穫（IC2 のドロップ回数 0、または収穫物のない段階）でも作物をリセットしていた。
     * プレイヤーには収穫物が「消えた」ように見えた。
     */
    @Override
    public boolean doManualHarvest(net.minecraft.world.entity.player.Player player) {
        if (player == null || level == null || level.isClientSide) return doManualHarvest();
        List<ItemStack> drops = doHarvest();
        if (drops.isEmpty()) return false;
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) net.neoforged.neoforge.items.ItemHandlerHelper.giveItemToPlayer(player, drop);
        }
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.CROP_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.1F);
        return true;
    }

    /**
     * <p><b>EN:</b> Harvest for machines and API callers. 0.1.8.2: when the harvest would give no item,
     * the crop keeps its stage and the list is empty (no silent loss of a ripe crop).
     * <p><b>ZH:</b> 供机器与 API 调用的收获。0.1.8.2：收获不会产出物品时，作物保持阶段并返回空列表（成熟作物不会无声损失）。
     * <p><b>JA:</b> 機械と API 用の収穫。0.1.8.2：アイテムが出ない収穫では作物の段階を保ち、空のリストを返す（熟した作物を黙って失わない）。
     */
    @Override
    public List<ItemStack> doHarvest() {
        if (plant == null || !plant.isHarvestable(this)) return List.of();
        List<ItemStack> drops = getHarvestDrops();
        if (drops.isEmpty()) return List.of();
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
        crossStick = ItemStack.EMPTY;
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
        // EN: 0.1.8.2: at least one harvest roll. The IC2 Gaussian gives 0 rolls in about 30-40 % of
        //     the harvests of a high-tier crop with low Gain; a ripe crop then gave nothing.
        // ZH: 0.1.8.2：至少一次收获。IC2 的高斯分布对低产量的高等级作物约有 30～40% 的收获为 0 次，成熟作物因此一无所获。
        // JA: 0.1.8.2：収穫は最低 1 回。IC2 のガウス分布では、収量の低い高ティア作物の収穫の約 30～40 % が 0 回になり、
        //     熟した作物から何も得られなかった。
        int count = Math.max(1, plant.calculateDropCount(this));
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
        // EN: 0.1.8.1 fix. Block states look up properties by identity. The upgraded stick has its
        //     own CROSSING instance, so the old check never set the crossing state on it.
        // ZH: 0.1.8.1 修复。方块状态按实例（引用）查找属性。升级作物架有自己的 CROSSING 实例，旧检查从未为其设置杂交状态。
        // JA: 0.1.8.1 修正。ブロックステートはプロパティをインスタンス（参照）で探す。強化作物棒は独自の CROSSING を
        //     持つので、旧チェックでは交配状態が一度も設定されなかった。
        if (nextState.hasProperty(mio_icif_crop_stick.CROSSING)) nextState = nextState.setValue(mio_icif_crop_stick.CROSSING, hybridBase);
        else if (nextState.hasProperty(mio_icif_crop_stick_upgraded.CROSSING)) nextState = nextState.setValue(mio_icif_crop_stick_upgraded.CROSSING, hybridBase);
        int signal = plant == null ? 0 : plant.getRedstoneSignal(this);
        int light = plant == null ? 0 : plant.getLightEmission(this);
        if (signal != lastSignal) { lastSignal = signal; level.updateNeighborsAt(worldPosition, state.getBlock()); }
        if (light != lastLight) { lastLight = light; level.getChunkSource().getLightEngine().checkBlock(worldPosition); }
        syncedSignature = visibleSignature();
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
        // EN: 0.1.8.2: titanium / uranium moved from the per-tick model to the IC2 cycle; restart their stage.
        // ZH: 0.1.8.2：钛／铀从逐 tick 模型改为 IC2 周期，重新开始其当前阶段。
        // JA: 0.1.8.2：チタン／ウランは毎 tick モデルから IC2 サイクルへ移ったので、現在の段階をやり直す。
        if (tag.getInt("CropCycleVersion") < 2 && plant instanceof com.miophas.singularity_iteration.common.crop.OreRootCrop) progress = 0;
        scanLevel = tag.getInt("ScanLevel");
        hybridBase = tag.getBoolean("HybridBase");
        crossStick = tag.contains("CrossStick") ? ItemStack.parse(registries, tag.getCompound("CrossStick")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        tendedUntil = tag.getLong("TendedUntil");
        // EN: Repair saves where a plant and the crossing flag were both set.
        // ZH: 修复同时设置了植物与杂交标志的存档。
        // JA: 植物と交配フラグが両方立ったセーブを修復する。
        if (plant != null && hybridBase) { hybridBase = false; crossStick = ItemStack.EMPTY; }
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
        tag.putInt("CropCycleVersion", 2);
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
        if (!crossStick.isEmpty()) tag.put("CrossStick", crossStick.save(registries));
        if (tendedUntil > 0) tag.putLong("TendedUntil", tendedUntil);
        tag.putInt("CropTicker", cropTicker);
        if (!customData.isEmpty()) tag.put("CustomData", customData.copy());
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    /** 0.1.10.17 (E-2): save keys the client never reads (growth progress, tick phase, timers, save version). */
    private static final String[] SERVER_ONLY_KEYS = {"Progress", "CropTicker", "FertilizerCooldown", "TendedUntil", "CropCycleVersion"};

    /**
     * 0.1.10.17 (E-2): the visible state: plant, stage, traits, scan level, resources, crossing and the
     * addon data (the renderer, the light level and addon plants read them on the client). The keys in
     * {@link #SERVER_ONLY_KEYS} stay on the server; the scanner HUD asks the server for its readings.
     */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        for (String key : SERVER_ONLY_KEYS) tag.remove(key);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }
}
