package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.common.energy.SolarGeneratorProfile;
import com.miophas.singularity_iteration.core.runtime.energy.SolarItemCharging;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_MetsSolarGeneratorBase extends AbstractGeneratorBlockEntity {

    public static final int SLOT_COUNT = 4;

    protected final int dayPower;
    protected final int tier;
    protected final int skyUpdateInterval;
    protected final float minSkyBrightness;
    protected final BooleanProperty litProperty;

    private int ticker;
    private float skyLight = 0.0F;
    private boolean isGenerating = false;
    private final SolarGeneratorProfile scexProfile;
    private boolean scexRefreshPending = true;

    public mio_icif_MetsSolarGeneratorBase(BlockPos pos, BlockState state,
                                            int dayPower, long capacity, int tier,
                                            int skyUpdateInterval, float minSkyBrightness,
                                            BlockEntityType<?> type,
                                            BooleanProperty litProperty) {
        this(pos, state, dayPower, dayPower, capacity, tier, skyUpdateInterval, minSkyBrightness, type, litProperty);
    }

    public mio_icif_MetsSolarGeneratorBase(BlockPos pos, BlockState state,
                                            int dayPower, int production, long capacity, int tier,
                                            int skyUpdateInterval, float minSkyBrightness,
                                            BlockEntityType<?> type,
                                            BooleanProperty litProperty) {
        super(pos, state, type != null ? type : mio_icif_block_entities.SOLAR_GENERATOR_ENTITY_TYPE.get(),
              SlotLayout.builder().extra(4).build(), production, capacity, 0,
              getCableTierFromIndex(tier - 1).powerRating, getCableTierFromIndex(tier - 1));
        this.scexProfile = energyStorage.scexNetworkControlled()
            ? SolarGeneratorProfile.find(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
                .filter(profile -> !profile.discrete()).orElseThrow() : null;
        this.dayPower = scexProfile != null ? scexProfile.dayPower() : dayPower;
        this.tier = tier;
        this.skyUpdateInterval = scexProfile != null ? scexProfile.refreshTicks() : skyUpdateInterval;
        this.minSkyBrightness = scexProfile != null ? scexProfile.minimumBrightness() : minSkyBrightness;
        this.litProperty = litProperty;
        this.ticker = (int) (Math.random() * skyUpdateInterval);
        if (scexProfile != null) {
            this.energyStorage.setCapacity(scexProfile.capacity());
            this.energyStorage.setMaxExtract(scexProfile.outputPacket());
        }
    }

    private static CableTier getCableTierFromIndex(int index) {
        return switch (index) {
            case 0 -> CableTier.LV;
            case 1 -> CableTier.MV;
            case 2 -> CableTier.HV;
            case 3 -> CableTier.EV;
            case 4 -> CableTier.IV;
            case 5 -> CableTier.LuV;
            case 6 -> CableTier.ZPMV;
            case 7 -> CableTier.UV;
            case 8 -> CableTier.UHV;
            case 9 -> CableTier.UEV;
            case 10 -> CableTier.UIV;
            case 11 -> CableTier.UXV;
            case 12 -> CableTier.OpV;
            case 13 -> CableTier.MAX;
            default -> CableTier.LV;
        };
    }

    public float getSkyLight() { return skyLight; }
    public int getDayPower() { return dayPower; }
    public int getTier() { return tier; }
    public boolean isGenerating() { return isGenerating; }

    /**
     * 实时发电功率。基类把 energyGenerationRate 设成了白天额定值，但 METS 是按天空亮度缩放的，
     * 因此这里报告当前实际值，与 {@link #generateEnergy()} 保持一致
     * （Jade「输出功率」与机器 API 的 getEnergyPerTick 读的都是这个方法）。
     */
    @Override
    public long getPowerOutput() {
        return scexProfile != null ? scexProfile.scaledOutput(skyLight) : (long) (dayPower * skyLight);
    }

    public boolean canGenerate(Level level, BlockPos pos) {
        return skyLight > 0.0F;
    }

    protected boolean hasSkyLight(Level level) {
        return level.dimensionType().hasSkyLight();
    }

    public void updateSunVisibility(Level level, BlockPos pos) {
        this.skyLight = calculateSkyLight(level, pos.above());
        if (scexProfile != null) this.skyLight = SolarGeneratorProfile.safeBrightness(this.skyLight);
    }

    public float calculateSkyLight(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight()) {
            return minSkyBrightness;
        }

        float sunBrightness = calculateSunBrightness(level);

        if (!isDesertBiome(level, pos)) {
            sunBrightness *= 1.0F - level.getRainLevel(1.0F) * 5.0F / 16.0F;
            sunBrightness *= 1.0F - level.getThunderLevel(1.0F) * 5.0F / 16.0F;
            sunBrightness = Math.max(0.0F, Math.min(1.0F, sunBrightness));
        }

        if (sunBrightness == 0.0F) {
            sunBrightness = minSkyBrightness;
        }

        int skyLightValue = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        return skyLightValue / 15.0F * sunBrightness;
    }

    private float calculateSunBrightness(Level level) {
        float celestialAngleRadians = level.getSunAngle(1.0F);
        float cos = (float) Math.cos(celestialAngleRadians);
        float brightness = cos * 2.0F + 0.2F;
        return Math.max(0.0F, Math.min(1.0F, brightness));
    }

    private static final TagKey<net.minecraft.world.level.biome.Biome> SANDY_BIOMES =
        TagKey.create(Registries.BIOME, ResourceLocation.parse("c:sandy"));

    private boolean isDesertBiome(Level level, BlockPos pos) {
        return level.getBiome(pos).is(SANDY_BIOMES);
    }

    @Override
    protected void generateEnergy() {
        if (scexProfile != null) {
            energyStorage.scexGenerateEnergy(EnergyAmount.of(scexProfile.scaledOutput(skyLight)), false);
            return;
        }
        if (skyLight > 0.0F) {
            long energyToGenerate = (long) (dayPower * skyLight);
            if (energyToGenerate > 0) {
                long energyGenerated = Math.min(energyToGenerate,
                    energyStorage.getCapacity() - energyStorage.getAmount());
                if (energyGenerated > 0) {
                    apiGenerateEnergy(energyGenerated);
                }
            }
        }
    }

    @Override
    protected void chargeItems() {
        if (scexProfile != null) {
            if (SolarItemCharging.charge(itemHandler, scexProfile.chargeSlots(), energyStorage, getItemAPI())) setChanged();
            return;
        }
        for (int i = 0; i < 4; i++) {
            ItemStack chargeStack = itemHandler.getStackInSlot(i);
            if (chargeStack.isEmpty()) continue;
            if (getItemAPI().isBattery(chargeStack)) {
                var api = getItemAPI();
                long currentEnergy = api.getBatteryStored(chargeStack);
                long batteryMaxEnergy = api.getBatteryCapacity(chargeStack);
                long batteryChargeRate = api.getChargeRate(chargeStack);
                if (currentEnergy >= batteryMaxEnergy) continue;
                long availableEnergy = energyStorage.getAmount();
                if (availableEnergy <= 0) continue;
                long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);
                energyToCharge = Math.min(energyToCharge, availableEnergy);
                long energyExtracted = apiExtractEnergy(energyToCharge, false);
                api.chargeBattery(chargeStack, energyExtracted, false);
                setChanged();
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_MetsSolarGeneratorBase blockEntity) {
        if (level.isClientSide()) return;

        boolean wasGenerating = blockEntity.isGenerating;

        blockEntity.chargeItems();

        if (!blockEntity.energyStorage.scexNetworkControlled() && blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        }

        if (blockEntity.scexProfile != null) {
            // Recompute from the world on first tick/reload; never trust saved light as generation input.
            if (blockEntity.scexRefreshPending || ++blockEntity.ticker >= blockEntity.skyUpdateInterval) {
                blockEntity.updateSunVisibility(level, pos);
                blockEntity.ticker = 0;
                blockEntity.scexRefreshPending = false;
            }
        } else if (++blockEntity.ticker % blockEntity.skyUpdateInterval == 0) {
            blockEntity.updateSunVisibility(level, pos);
        }

        boolean canGenerate = blockEntity.scexProfile != null
            ? blockEntity.scexProfile.scaledOutput(blockEntity.skyLight) > 0
                && !blockEntity.energyStorage.scexExactAmount().roomBelow(blockEntity.energyStorage.getCapacity()).isZero()
            : blockEntity.skyLight > 0.0F;
        if (canGenerate) {
            blockEntity.generateEnergy();
            blockEntity.isGenerating = true;
        } else {
            blockEntity.isGenerating = false;
        }

        if (wasGenerating != blockEntity.isGenerating) {
            BlockState newState = level.getBlockState(pos);
            if (blockEntity.litProperty != null && newState.hasProperty(blockEntity.litProperty)) {
                newState = newState.setValue(blockEntity.litProperty, blockEntity.isGenerating);
                level.setBlock(pos, newState, 3);
            }
        }

        blockEntity.setChanged();
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) { return 0; }

    @Override
    public boolean isBurning() { return isGenerating; }

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable net.minecraft.core.Direction side) {
        return slot >= 0 && slot < Math.min(SLOT_COUNT, itemHandler.getSlots()) && isBattery(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot >= 0 && slot < Math.min(SLOT_COUNT, itemHandler.getSlots());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsGenerating", isGenerating);
        tag.putFloat("SkyLight", skyLight);
        tag.putInt("Ticker", ticker);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
        skyLight = tag.getFloat("SkyLight");
        ticker = tag.getInt("Ticker");
        if (scexProfile != null) {
            skyLight = 0;
            ticker = 0;
            scexRefreshPending = true;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IsGenerating", isGenerating);
        tag.putFloat("SkyLight", skyLight);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        isGenerating = tag.getBoolean("IsGenerating");
        skyLight = tag.getFloat("SkyLight");
        if (scexProfile != null) skyLight = SolarGeneratorProfile.safeBrightness(skyLight);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.mets_advanced_solar_generator");
    }
}