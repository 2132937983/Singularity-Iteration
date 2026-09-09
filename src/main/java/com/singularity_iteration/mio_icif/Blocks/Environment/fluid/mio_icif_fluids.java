package com.singularity_iteration.mio_icif.Blocks.Environment.fluid;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.fluid.GasFluidFlowing;
import com.singularity_iteration.mio_icif.api.fluid.GasFluidSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 流体注册�? * 统一管理所有模组的自定义流�? */
@SuppressWarnings("null")
public class mio_icif_fluids {

    // 流体类型注册�
public static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Singularity_Iteration.MOD_ID);

    // 流体注册�
public static final DeferredRegister<Fluid> FLUIDS =
        DeferredRegister.create(BuiltInRegistries.FLUID, Singularity_Iteration.MOD_ID);

    // 流体方块注册�
public static final DeferredRegister.Blocks FLUID_BLOCKS =
        DeferredRegister.createBlocks(Singularity_Iteration.MOD_ID);

    // 流体物品注册�
public static final DeferredRegister.Items FLUID_ITEMS =
        DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // 生物气体纹理
    public static final ResourceLocation BIOGAS_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/biogas_still");
    public static final ResourceLocation BIOGAS_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/biogas_still");

    // 生物气体流体类型
    public static final DeferredHolder<FluidType, FluidType> BIOGAS_TYPE = FLUID_TYPES.register("biogas",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(1000)
            .density(1000)
            .canConvertToSource(false)));

    // 获取生物气体静止纹理
    public static ResourceLocation getBiogasStillTexture() {
        return BIOGAS_STILL_TEXTURE;
    }

    // 获取生物气体流动纹理
    public static ResourceLocation getBiogasFlowingTexture() {
        return BIOGAS_FLOWING_TEXTURE;
    }

    // 生物气体属性
private static final BaseFlowingFluid.Properties BIOGAS_PROPERTIES = new BaseFlowingFluid.Properties(
        BIOGAS_TYPE,
        () -> mio_icif_fluids.BIOGAS.get(),
        () -> mio_icif_fluids.BIOGAS_FLOWING.get())
        .block(() -> mio_icif_fluids.BIOGAS_BLOCK.get())
        .bucket(() -> mio_icif_fluids.BIOGAS_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> BIOGAS = FLUIDS.register("biogas",
        () -> new GasFluidSource(BIOGAS_PROPERTIES, 320));

    public static final DeferredHolder<Fluid, FlowingFluid> BIOGAS_FLOWING = FLUIDS.register("biogas_flowing",
        () -> new GasFluidFlowing(BIOGAS_PROPERTIES, 320));

    // 生物气体流体方块
    public static final DeferredBlock<LiquidBlock> BIOGAS_BLOCK = FLUID_BLOCKS.register("biogas",
        () -> new LiquidBlock(BIOGAS.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable()));

    // 生物气体桶物�
public static final DeferredItem<BucketItem> BIOGAS_BUCKET = FLUID_ITEMS.register("biogas_bucket",
        () -> new BucketItem(BIOGAS.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // 热水纹理
    public static final ResourceLocation HOTWATER_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/hotwater_still");
    public static final ResourceLocation HOTWATER_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/hotwater_flow");

    // 热水流体类型
    public static final DeferredHolder<FluidType, FluidType> HOTWATER_TYPE = FLUID_TYPES.register("hotwater",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(true)
            .canDrown(true)
            .canPushEntity(true)
            .canExtinguish(false)
            .viscosity(1000)
            .density(1000)
            .temperature(343)  // 70°C
            .canConvertToSource(false)));

    // 获取热水静止纹理
    public static ResourceLocation getHotwaterStillTexture() {
        return HOTWATER_STILL_TEXTURE;
    }

    // 获取热水流动纹理
    public static ResourceLocation getHotwaterFlowingTexture() {
        return HOTWATER_FLOWING_TEXTURE;
    }

    // 热水属性
private static final BaseFlowingFluid.Properties HOTWATER_PROPERTIES = new BaseFlowingFluid.Properties(
        HOTWATER_TYPE,
        () -> mio_icif_fluids.HOTWATER.get(),
        () -> mio_icif_fluids.HOTWATER_FLOWING.get())
        .block(() -> mio_icif_fluids.HOTWATER_BLOCK.get())
        .bucket(() -> mio_icif_fluids.HOTWATER_BUCKET.get());

    // 热水源流�
public static final DeferredHolder<Fluid, FlowingFluid> HOTWATER = FLUIDS.register("hotwater",
        () -> new BaseFlowingFluid.Source(HOTWATER_PROPERTIES));

    // 热水流动流体
    public static final DeferredHolder<Fluid, FlowingFluid> HOTWATER_FLOWING = FLUIDS.register("hotwater_flowing",
        () -> new BaseFlowingFluid.Flowing(HOTWATER_PROPERTIES));

    // 热水流体方块
    public static final DeferredBlock<LiquidBlock> HOTWATER_BLOCK = FLUID_BLOCKS.register("hotwater",
        () -> new mio_icif_block_fluid_effect(HOTWATER.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable(), mio_icif_block_fluid_effect.HOT_WATER));

    // 热水桶物�
public static final DeferredItem<BucketItem> HOTWATER_BUCKET = FLUID_ITEMS.register("hotwater_bucket",
        () -> new BucketItem(HOTWATER.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 生物�?(Biomass) ====================
    public static final ResourceLocation BIOMASS_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/biomass_still");
    public static final ResourceLocation BIOMASS_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/biomass_flow");

    public static final DeferredHolder<FluidType, FluidType> BIOMASS_TYPE = FLUID_TYPES.register("biomass",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(1500)
            .density(1200)
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties BIOMASS_PROPERTIES = new BaseFlowingFluid.Properties(
        BIOMASS_TYPE,
        () -> mio_icif_fluids.BIOMASS.get(),
        () -> mio_icif_fluids.BIOMASS_FLOWING.get())
        .block(() -> mio_icif_fluids.BIOMASS_BLOCK.get())
        .bucket(() -> mio_icif_fluids.BIOMASS_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> BIOMASS = FLUIDS.register("biomass",
        () -> new BaseFlowingFluid.Source(BIOMASS_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> BIOMASS_FLOWING = FLUIDS.register("biomass_flowing",
        () -> new BaseFlowingFluid.Flowing(BIOMASS_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> BIOMASS_BLOCK = FLUID_BLOCKS.register("biomass",
        () -> new LiquidBlock(BIOMASS.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable()));
    public static final DeferredItem<BucketItem> BIOMASS_BUCKET = FLUID_ITEMS.register("biomass_bucket",
        () -> new BucketItem(BIOMASS.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 建筑泡沫 (Construction Foam) ====================
    public static final ResourceLocation CONSTRUCTIONFOAM_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/constructionfoam_still");
    public static final ResourceLocation CONSTRUCTIONFOAM_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/constructionfoam_flow");

    public static final DeferredHolder<FluidType, FluidType> CONSTRUCTIONFOAM_TYPE = FLUID_TYPES.register("constructionfoam",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(2000)
            .density(1500)
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties CONSTRUCTIONFOAM_PROPERTIES = new BaseFlowingFluid.Properties(
        CONSTRUCTIONFOAM_TYPE,
        () -> mio_icif_fluids.CONSTRUCTIONFOAM.get(),
        () -> mio_icif_fluids.CONSTRUCTIONFOAM_FLOWING.get())
        .block(() -> mio_icif_fluids.CONSTRUCTIONFOAM_BLOCK.get())
        .bucket(() -> mio_icif_fluids.CONSTRUCTIONFOAM_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> CONSTRUCTIONFOAM = FLUIDS.register("constructionfoam",
        () -> new BaseFlowingFluid.Source(CONSTRUCTIONFOAM_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> CONSTRUCTIONFOAM_FLOWING = FLUIDS.register("constructionfoam_flowing",
        () -> new BaseFlowingFluid.Flowing(CONSTRUCTIONFOAM_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> CONSTRUCTIONFOAM_BLOCK = FLUID_BLOCKS.register("constructionfoam",
        () -> new mio_icif_block_fluid_effect(CONSTRUCTIONFOAM.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable(), mio_icif_block_fluid_effect.CONSTRUCTION_FOAM));
    public static final DeferredItem<BucketItem> CONSTRUCTIONFOAM_BUCKET = FLUID_ITEMS.register("constructionfoam_bucket",
        () -> new BucketItem(CONSTRUCTIONFOAM.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 冷却�?(Coolant) ====================
    public static final ResourceLocation COOLANT_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/coolant_still");
    public static final ResourceLocation COOLANT_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/coolant_flow");

    public static final DeferredHolder<FluidType, FluidType> COOLANT_TYPE = FLUID_TYPES.register("coolant",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(true)
            .viscosity(1000)
            .density(1000)
            .temperature(273)  // 0°C
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties COOLANT_PROPERTIES = new BaseFlowingFluid.Properties(
        COOLANT_TYPE,
        () -> mio_icif_fluids.COOLANT.get(),
        () -> mio_icif_fluids.COOLANT_FLOWING.get())
        .block(() -> mio_icif_fluids.COOLANT_BLOCK.get())
        .bucket(() -> mio_icif_fluids.COOLANT_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> COOLANT = FLUIDS.register("coolant",
        () -> new BaseFlowingFluid.Source(COOLANT_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> COOLANT_FLOWING = FLUIDS.register("coolant_flowing",
        () -> new BaseFlowingFluid.Flowing(COOLANT_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> COOLANT_BLOCK = FLUID_BLOCKS.register("coolant",
        () -> new LiquidBlock(COOLANT.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable()));
    public static final DeferredItem<BucketItem> COOLANT_BUCKET = FLUID_ITEMS.register("coolant_bucket",
        () -> new BucketItem(COOLANT.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 蒸馏�?(Distilled Water) ====================
    public static final ResourceLocation DISTILLEDWATER_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/distilledwater_still");
    public static final ResourceLocation DISTILLEDWATER_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/distilledwater_flow");

    public static final DeferredHolder<FluidType, FluidType> DISTILLEDWATER_TYPE = FLUID_TYPES.register("distilledwater",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(true)
            .canDrown(true)
            .canPushEntity(true)
            .canExtinguish(true)
            .viscosity(1000)
            .density(1000)
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties DISTILLEDWATER_PROPERTIES = new BaseFlowingFluid.Properties(
        DISTILLEDWATER_TYPE,
        () -> mio_icif_fluids.DISTILLEDWATER.get(),
        () -> mio_icif_fluids.DISTILLEDWATER_FLOWING.get())
        .block(() -> mio_icif_fluids.DISTILLEDWATER_BLOCK.get())
        .bucket(() -> mio_icif_fluids.DISTILLEDWATER_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> DISTILLEDWATER = FLUIDS.register("distilledwater",
        () -> new BaseFlowingFluid.Source(DISTILLEDWATER_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> DISTILLEDWATER_FLOWING = FLUIDS.register("distilledwater_flowing",
        () -> new BaseFlowingFluid.Flowing(DISTILLEDWATER_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> DISTILLEDWATER_BLOCK = FLUID_BLOCKS.register("distilledwater",
        () -> new LiquidBlock(DISTILLEDWATER.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable()));
    public static final DeferredItem<BucketItem> DISTILLEDWATER_BUCKET = FLUID_ITEMS.register("distilledwater_bucket",
        () -> new BucketItem(DISTILLEDWATER.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 热冷却液 (Hot Coolant) ====================
    public static final ResourceLocation HOTCOOLANT_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/hotcoolant_still");
    public static final ResourceLocation HOTCOOLANT_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/hotcoolant_flow");

    public static final DeferredHolder<FluidType, FluidType> HOTCOOLANT_TYPE = FLUID_TYPES.register("hotcoolant",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(1000)
            .density(1000)
            .temperature(373)  // 100°C
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties HOTCOOLANT_PROPERTIES = new BaseFlowingFluid.Properties(
        HOTCOOLANT_TYPE,
        () -> mio_icif_fluids.HOTCOOLANT.get(),
        () -> mio_icif_fluids.HOTCOOLANT_FLOWING.get())
        .block(() -> mio_icif_fluids.HOTCOOLANT_BLOCK.get())
        .bucket(() -> mio_icif_fluids.HOTCOOLANT_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> HOTCOOLANT = FLUIDS.register("hotcoolant",
        () -> new BaseFlowingFluid.Source(HOTCOOLANT_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> HOTCOOLANT_FLOWING = FLUIDS.register("hotcoolant_flowing",
        () -> new BaseFlowingFluid.Flowing(HOTCOOLANT_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> HOTCOOLANT_BLOCK = FLUID_BLOCKS.register("hotcoolant",
        () -> new mio_icif_block_fluid_effect(HOTCOOLANT.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable(), mio_icif_block_fluid_effect.HOT_COOLANT));
    public static final DeferredItem<BucketItem> HOTCOOLANT_BUCKET = FLUID_ITEMS.register("hotcoolant_bucket",
        () -> new BucketItem(HOTCOOLANT.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 熔岩 (Pahoehoe Lava) ====================
    public static final ResourceLocation PAHOEHOELAVA_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/pahoehoelava_still");

    public static final DeferredHolder<FluidType, FluidType> PAHOEHOELAVA_TYPE = FLUID_TYPES.register("pahoehoelava",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(6000)
            .density(3000)
            .temperature(1300)  // 高温
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties PAHOEHOELAVA_PROPERTIES = new BaseFlowingFluid.Properties(
        PAHOEHOELAVA_TYPE,
        () -> mio_icif_fluids.PAHOEHOELAVA.get(),
        () -> mio_icif_fluids.PAHOEHOELAVA_FLOWING.get())
        .block(() -> mio_icif_fluids.PAHOEHOELAVA_BLOCK.get())
        .bucket(() -> mio_icif_fluids.PAHOEHOELAVA_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> PAHOEHOELAVA = FLUIDS.register("pahoehoelava",
        () -> new BaseFlowingFluid.Source(PAHOEHOELAVA_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> PAHOEHOELAVA_FLOWING = FLUIDS.register("pahoehoelava_flowing",
        () -> new BaseFlowingFluid.Flowing(PAHOEHOELAVA_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> PAHOEHOELAVA_BLOCK = FLUID_BLOCKS.register("pahoehoelava",
        () -> new mio_icif_block_fluid_effect(PAHOEHOELAVA.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable(), mio_icif_block_fluid_effect.PAHOEHOE_LAVA));
    public static final DeferredItem<BucketItem> PAHOEHOELAVA_BUCKET = FLUID_ITEMS.register("pahoehoelava_bucket",
        () -> new BucketItem(PAHOEHOELAVA.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 蒸汽 (Steam) ====================
    public static final ResourceLocation STEAM_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/steam_still");

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(100)
            .density(50)
            .temperature(373)  // 100°C
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties STEAM_PROPERTIES = new BaseFlowingFluid.Properties(
        STEAM_TYPE,
        () -> mio_icif_fluids.STEAM.get(),
        () -> mio_icif_fluids.STEAM_FLOWING.get())
        .block(() -> mio_icif_fluids.STEAM_BLOCK.get())
        .bucket(() -> mio_icif_fluids.STEAM_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> STEAM = FLUIDS.register("steam",
        () -> new GasFluidSource(STEAM_PROPERTIES, 320));
    public static final DeferredHolder<Fluid, FlowingFluid> STEAM_FLOWING = FLUIDS.register("steam_flowing",
        () -> new GasFluidFlowing(STEAM_PROPERTIES, 320));
    public static final DeferredBlock<LiquidBlock> STEAM_BLOCK = FLUID_BLOCKS.register("steam",
        () -> new mio_icif_block_fluid_effect(STEAM.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable(), mio_icif_block_fluid_effect.STEAM));
    public static final DeferredItem<BucketItem> STEAM_BUCKET = FLUID_ITEMS.register("steam_bucket",
        () -> new BucketItem(STEAM.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 过热蒸汽 (Superheated Steam) ====================
    public static final ResourceLocation SUPERHEATEDSTEAM_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/superheatedsteam_still");

    public static final DeferredHolder<FluidType, FluidType> SUPERHEATEDSTEAM_TYPE = FLUID_TYPES.register("superheatedsteam",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(50)
            .density(30)
            .temperature(673)  // 400°C
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties SUPERHEATEDSTEAM_PROPERTIES = new BaseFlowingFluid.Properties(
        SUPERHEATEDSTEAM_TYPE,
        () -> mio_icif_fluids.SUPERHEATEDSTEAM.get(),
        () -> mio_icif_fluids.SUPERHEATEDSTEAM_FLOWING.get())
        .block(() -> mio_icif_fluids.SUPERHEATEDSTEAM_BLOCK.get())
        .bucket(() -> mio_icif_fluids.SUPERHEATEDSTEAM_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> SUPERHEATEDSTEAM = FLUIDS.register("superheatedsteam",
        () -> new GasFluidSource(SUPERHEATEDSTEAM_PROPERTIES, 320));
    public static final DeferredHolder<Fluid, FlowingFluid> SUPERHEATEDSTEAM_FLOWING = FLUIDS.register("superheatedsteam_flowing",
        () -> new GasFluidFlowing(SUPERHEATEDSTEAM_PROPERTIES, 320));
    public static final DeferredBlock<LiquidBlock> SUPERHEATEDSTEAM_BLOCK = FLUID_BLOCKS.register("superheatedsteam",
        () -> new mio_icif_block_fluid_effect(SUPERHEATEDSTEAM.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable(), mio_icif_block_fluid_effect.STEAM));
    public static final DeferredItem<BucketItem> SUPERHEATEDSTEAM_BUCKET = FLUID_ITEMS.register("superheatedsteam_bucket",
        () -> new BucketItem(SUPERHEATEDSTEAM.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== UU物质 (UU-Matter) ====================
    public static final ResourceLocation UUMATTER_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/uumatter_still");
    public static final ResourceLocation UUMATTER_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/uumatter_flow");

    public static final DeferredHolder<FluidType, FluidType> UUMATTER_TYPE = FLUID_TYPES.register("uumatter",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(3000)
            .density(2000)
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties UUMATTER_PROPERTIES = new BaseFlowingFluid.Properties(
        UUMATTER_TYPE,
        () -> mio_icif_fluids.UUMATTER.get(),
        () -> mio_icif_fluids.UUMATTER_FLOWING.get())
        .block(() -> mio_icif_fluids.UUMATTER_BLOCK.get())
        .bucket(() -> mio_icif_fluids.UUMATTER_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> UUMATTER = FLUIDS.register("uumatter",
        () -> new BaseFlowingFluid.Source(UUMATTER_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> UUMATTER_FLOWING = FLUIDS.register("uumatter_flowing",
        () -> new BaseFlowingFluid.Flowing(UUMATTER_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> UUMATTER_BLOCK = FLUID_BLOCKS.register("uumatter",
        () -> new mio_icif_block_fluid_effect(UUMATTER.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable(), mio_icif_block_fluid_effect.UU_MATTER));
    public static final DeferredItem<BucketItem> UUMATTER_BUCKET = FLUID_ITEMS.register("uumatter_bucket",
        () -> new BucketItem(UUMATTER.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 压缩空气 (Compressed Air) ====================
    public static final ResourceLocation AIR_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/air_still");
    public static final ResourceLocation AIR_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/air_flow");

    public static final DeferredHolder<FluidType, FluidType> AIR_TYPE = FLUID_TYPES.register("air",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(100)
            .density(-1000)
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties AIR_PROPERTIES = new BaseFlowingFluid.Properties(
        AIR_TYPE,
        () -> mio_icif_fluids.AIR.get(),
        () -> mio_icif_fluids.AIR_FLOWING.get())
        .block(() -> mio_icif_fluids.AIR_BLOCK.get())
        .bucket(() -> mio_icif_fluids.AIR_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> AIR = FLUIDS.register("air",
        () -> new GasFluidSource(AIR_PROPERTIES, 320));
    public static final DeferredHolder<Fluid, FlowingFluid> AIR_FLOWING = FLUIDS.register("air_flowing",
        () -> new GasFluidFlowing(AIR_PROPERTIES, 320));
    public static final DeferredBlock<LiquidBlock> AIR_BLOCK = FLUID_BLOCKS.register("air",
        () -> new LiquidBlock(AIR.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .noOcclusion()
            .strength(100.0F)
            .noLootTable()
            .replaceable()));
    public static final DeferredItem<BucketItem> AIR_BUCKET = FLUID_ITEMS.register("air_bucket",
        () -> new BucketItem(AIR.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 原油 (Crude Oil) ====================
    public static final ResourceLocation CRUDEOIL_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/crude_oil_still");
    public static final ResourceLocation CRUDEOIL_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/crude_oil_flow");

    public static final DeferredHolder<FluidType, FluidType> CRUDEOIL_TYPE = FLUID_TYPES.register("crudeoil",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(1000)
            .density(1000)
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties CRUDEOIL_PROPERTIES = new BaseFlowingFluid.Properties(
        CRUDEOIL_TYPE,
        () -> mio_icif_fluids.CRUDEOIL.get(),
        () -> mio_icif_fluids.CRUDEOIL_FLOWING.get())
        .block(() -> mio_icif_fluids.CRUDEOIL_BLOCK.get())
        .bucket(() -> mio_icif_fluids.CRUDEOIL_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> CRUDEOIL = FLUIDS.register("crudeoil",
        () -> new BaseFlowingFluid.Source(CRUDEOIL_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> CRUDEOIL_FLOWING = FLUIDS.register("crudeoil_flowing",
        () -> new BaseFlowingFluid.Flowing(CRUDEOIL_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> CRUDEOIL_BLOCK = FLUID_BLOCKS.register("crudeoil",
        () -> new LiquidBlock(CRUDEOIL.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable()));
    public static final DeferredItem<BucketItem> CRUDEOIL_BUCKET = FLUID_ITEMS.register("crudeoil_bucket",
        () -> new BucketItem(CRUDEOIL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ==================== 柴油 (Diesel Oil) ====================
    public static final ResourceLocation DIESELOIL_STILL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/diesel_oil_still");
    public static final ResourceLocation DIESELOIL_FLOWING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block/fluids/diesel_oil_flow");

    public static final DeferredHolder<FluidType, FluidType> DIESELOIL_TYPE = FLUID_TYPES.register("dieseloil",
        () -> new FluidType(FluidType.Properties.create()
            .canSwim(false)
            .canDrown(false)
            .canPushEntity(false)
            .canExtinguish(false)
            .viscosity(1000)
            .density(1000)
            .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties DIESELOIL_PROPERTIES = new BaseFlowingFluid.Properties(
        DIESELOIL_TYPE,
        () -> mio_icif_fluids.DIESELOIL.get(),
        () -> mio_icif_fluids.DIESELOIL_FLOWING.get())
        .block(() -> mio_icif_fluids.DIESELOIL_BLOCK.get())
        .bucket(() -> mio_icif_fluids.DIESELOIL_BUCKET.get());

    public static final DeferredHolder<Fluid, FlowingFluid> DIESELOIL = FLUIDS.register("dieseloil",
        () -> new BaseFlowingFluid.Source(DIESELOIL_PROPERTIES));
    public static final DeferredHolder<Fluid, FlowingFluid> DIESELOIL_FLOWING = FLUIDS.register("dieseloil_flowing",
        () -> new BaseFlowingFluid.Flowing(DIESELOIL_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> DIESELOIL_BLOCK = FLUID_BLOCKS.register("dieseloil",
        () -> new LiquidBlock(DIESELOIL.get(), BlockBehaviour.Properties.of()
            .noCollission()
            .strength(100.0F)
            .noLootTable()
            .replaceable()));
    public static final DeferredItem<BucketItem> DIESELOIL_BUCKET = FLUID_ITEMS.register("dieseloil_bucket",
        () -> new BucketItem(DIESELOIL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /**
     * 注册所有流�
 * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
        FLUID_BLOCKS.register(eventBus);
        FLUID_ITEMS.register(eventBus);
    }
}