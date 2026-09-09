package com.singularity_iteration.mio_icif.Blocks.Crop;

import com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 高级作物架方块
 * 用于种植更高级IC2风格农作物的平台
 * 可以放置在耕地上，提供更好的生长环境
 */
@SuppressWarnings("null")
public class mio_icif_crop_stick_upgraded extends BaseEntityBlock {

    public static final MapCodec<mio_icif_crop_stick_upgraded> CODEC = simpleCodec(mio_icif_crop_stick_upgraded::new);

    // 生长阶段属性(0-7, 0=空架, 1-7=生长阶段)
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);

    // 是否有水属性（高级作物架可以存储水分）
    public static final BooleanProperty HYDRATED = BooleanProperty.create("hydrated");

    // 作物架的形状（十字形，较细）8x8
    protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);

    public mio_icif_crop_stick_upgraded(Properties properties) {

super(properties);

this.registerDefaultState(this.stateDefinition.any()

    .setValue(AGE, 0)

    .setValue(HYDRATED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {

return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {

return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

return new mio_icif_crop_entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {

return createTickerHelper(blockEntityType, mio_icif_block_entities.CROP_ENTITY_TYPE.get(),


(level1, pos, state1, blockEntity) -> blockEntity.tick(level1, pos, state1, blockEntity));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

builder.add(AGE, HYDRATED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {

// 检查下方是否是耕地

BlockPos belowPos = pos.below();

BlockState belowState = level.getBlockState(belowPos);

return belowState.is(net.minecraft.world.level.block.Blocks.FARMLAND);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {

// 检查下方是否是耕地

BlockPos belowPos = context.getClickedPos().below();

BlockState belowState = context.getLevel().getBlockState(belowPos);

if (!belowState.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {

    return null;

}

return this.defaultBlockState().setValue(AGE, 0).setValue(HYDRATED, false);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {

return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {

return 1.0F;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {

super.neighborChanged(state, level, pos, block, fromPos, isMoving);

// 如果下方的耕地被破坏，作物架也应该被破坏

if (fromPos.equals(pos.below())) {

    BlockState belowState = level.getBlockState(pos.below());

    if (!belowState.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {


// 破坏作物架并掉落物品


level.destroyBlock(pos, true);

    }

}
    }

    /**
     * 检查作物架是否为空（没有种植作物）
     */
    public boolean isEmpty(BlockState state) {

return state.getValue(AGE) == 0;
    }

    /**
     * 获取当前生长阶段
     */
    public int getAge(BlockState state) {

return state.getValue(AGE);
    }

    /**
     * 获取最大生长阶段
     */
    public int getMaxAge() {

return 7;
    }

    /**
     * 设置生长阶段
     */
    public BlockState withAge(int age) {

return this.defaultBlockState().setValue(AGE, age);
    }

    /**
     * 检查是否已成熟
     */
    public boolean isMaxAge(BlockState state) {

return state.getValue(AGE) >= getMaxAge();
    }

    /**
     * 检查是否已浇水
     */
    public boolean isHydrated(BlockState state) {

return state.getValue(HYDRATED);
    }

    /**
     * 获取生长速度加成
     * 高级作物架提供10%的生长速度加成
     */
    public float getGrowthSpeedBonus() {

return 0.10f;
    }

    /**
     * 获取产量加成
     * 高级作物架提供10%的产量加成
     */
    public float getYieldBonus() {

return 0.10f;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

if (level.isClientSide) {

    return ItemInteractionResult.SUCCESS;

}


// 获取方块实体

BlockEntity blockEntity = level.getBlockEntity(pos);

if (blockEntity instanceof com.singularity_iteration.mio_icif.api.crop.IPlanter planter) {

    ItemStack heldItem = player.getItemInHand(hand);


    // 检查是否是除草剂

    if (com.singularity_iteration.mio_icif.Items.Normal.MatronHerbicideItem.isHerbicide(heldItem)) {


// 检查是否有杂草


if (planter.getPlant() != null && planter.getPlant().getTypeId().equals("weed")) {


    // 清除杂草


    planter.reset();


}



// 设置除草剂免疫（一天时间）


if (planter instanceof com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity cropEntity) {


    cropEntity.setWeedControl(150);


}


planter.updateState();



// 消耗除草剂耐久度

    if (!player.getAbilities().instabuild) {


    heldItem.hurtAndBreak(1, player, null);


}



// 播放粒子效果


level.levelEvent(1505, pos, 0);



return ItemInteractionResult.SUCCESS;

    }


    // 检查是否是肥料（骨粉或带有 #c:fertilizer 标签的物品）

    if (isFertilizer(heldItem)) {


// 检查是否有作物且未成熟


if (planter.getPlant() != null && planter.getGrowthStage() < planter.getPlant().getMaxGrowthStage()) {


    // 催熟作物 - 随机跳过1-3个阶段


    int skipStages = level.random.nextInt(3) + 1; // 1-3


    int newStage = Math.min(planter.getGrowthStage() + skipStages, planter.getPlant().getMaxGrowthStage());


    planter.setGrowthStage(newStage);


    planter.updateState();



    // 消耗肥料

    if (!player.getAbilities().instabuild) {



heldItem.shrink(1);


    }



    // 播放粒子效果


    level.levelEvent(1505, pos, 0);



    return ItemInteractionResult.SUCCESS;


}


return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

    }


    // 如果玩家拿着种子，尝试种植

    if (heldItem.getItem() instanceof com.singularity_iteration.mio_icif.Items.Crop.CropSeedItem seedItem) {


// 检查种植架是否为空


if (planter.getPlant() == null) {


    // 从NBT获取植物类型


    com.singularity_iteration.mio_icif.api.crop.PlantType plantType = com.singularity_iteration.mio_icif.Items.Crop.CropSeedItem.getPlantType(heldItem);


    if (plantType != null) {



// 从NBT读取属性






int growthSpeed = 0;



int yield = 0;



int resilience = 0;




var customData = heldItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);



if (customData != null) {



    var tag = customData.copyTag();



    growthSpeed = tag.getInt("GrowthSpeed");



    yield = tag.getInt("Yield");



    resilience = tag.getInt("Resilience");



}




// 种植作物



planter.setPlant(plantType);



planter.setGrowthStage(1);



planter.setGrowthSpeed(growthSpeed);



planter.setYield(yield);



planter.setResilience(resilience);



planter.updateState();




// 消耗一个种子并返还空种子袋



if (!player.getAbilities().instabuild) {



    heldItem.shrink(1);



    // 返还空种子袋



    ItemStack emptyBag = new ItemStack(seedItem);



    if (!player.getInventory().add(emptyBag)) {




// 如果背包满了，掉落在地上




player.drop(emptyBag, false);



    }



}



return ItemInteractionResult.SUCCESS;


    }


}


return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

    }


    // 如果玩家拿着原版种子，尝试种植
    com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry.BaseSeed baseSeed =
    com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry.instance.getBaseSeed(heldItem);

    if (baseSeed != null && planter.getPlant() == null) {


// 种植作物


planter.setPlant(baseSeed.plantType);


planter.setGrowthStage(baseSeed.stage);


planter.setGrowthSpeed(baseSeed.growthSpeed);


planter.setYield(baseSeed.yield);


planter.setResilience(baseSeed.resilience);


planter.updateState();



// 消耗一个种子

if (!player.getAbilities().instabuild) {


    heldItem.shrink(1);


}


return ItemInteractionResult.SUCCESS;

    }


    // 如果玩家手持作物分析仪，优先执行分析而非收获

    if (heldItem.getItem() instanceof com.singularity_iteration.mio_icif.Items.Tools.CropAnalyzerItem) {


return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

    }


    // 右键收获作物（杂草不能直接收获，需要使用除草铲）
    // 右键收获只掉落产物，不掉落种子
    if (planter.getPlant() != null && planter.getPlant().isHarvestable(planter)) {


// 检查是否是杂草


if (planter.getPlant().getTypeId().equals("weed")) {


    player.sendSystemMessage(Component.translatable("message.mio_icif.crop.weed_need_trowel"));


    return ItemInteractionResult.FAIL;


}


planter.doHarvestWithoutSeeds();


return ItemInteractionResult.SUCCESS;

    }

}


return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 左键攻击方块 - 清除作物并掉落种子，或破坏空的作物架
     * IC2原版逻辑：
     * - 有作物时：清除作物，只掉落种子，不掉落作物产物
     * - 空作物架时：正常破坏方块
     */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {

if (level.isClientSide) {

    return;

}


BlockEntity blockEntity = level.getBlockEntity(pos);

if (blockEntity instanceof com.singularity_iteration.mio_icif.api.crop.IPlanter planter) {

    // 检查是否有作物

    if (planter.getPlant() != null) {


// 清除作物并掉落种子
dropSeedsOnAttack(planter, level, pos);


planter.reset();


return;

    }

}


// 空作物架，正常破�?
level.destroyBlock(pos, true);
    }

    /**
     * 左键攻击时掉落种子（IC2原版逻辑�?     * 只掉落种子，不掉落作物产�?     */
    private void dropSeedsOnAttack(com.singularity_iteration.mio_icif.api.crop.IPlanter planter, Level level, BlockPos pos) {

com.singularity_iteration.mio_icif.api.crop.PlantType plant = planter.getPlant();

if (plant == null) {

    return;

}


// 计算种子掉落概率（基于IC2的pick机制）
float seedChance = plant.dropSeedChance(planter);

int growthSpeed = planter.getGrowthSpeed();

int yield = planter.getYield();

int resilience = planter.getResilience();


// 增加种子掉落概率

for (int i = 0; i < resilience; i++) {

    seedChance *= 1.1F;

}


int seedDrop = 0;


// 第一颗种子概率

if (level.random.nextFloat() <= (seedChance + 1.0F) * 0.8F) {

    seedDrop++;

}


// 第二颗种子概率

float secondChance = plant.dropSeedChance(planter) + growthSpeed / 100.0F;

for (int k = 23; k < yield; k++) {

    secondChance *= 0.95F;

}

if (level.random.nextFloat() <= secondChance) {

    seedDrop++;

}


// 掉落种子

for (int j = 0; j < seedDrop; j++) {

    ItemStack seedStack = makeSeeds(plant, planter.getGrowthStage(), growthSpeed, yield, resilience);

    if (!seedStack.isEmpty()) {


net.minecraft.world.entity.item.ItemEntity seedEntity = new net.minecraft.world.entity.item.ItemEntity(



level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, seedStack);


level.addFreshEntity(seedEntity);

    }

}
    }

    /**
     * 创建种子物品堆
     */
    private ItemStack makeSeeds(com.singularity_iteration.mio_icif.api.crop.PlantType plant, int stage, int growthSpeed, int yield, int resilience) {

return com.singularity_iteration.mio_icif.Items.Crop.CropSeedItem.createSeedStack(


com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.CROP_SEED.get(),


plant.getModId(),


plant.getTypeId(),


growthSpeed,


yield,


resilience);
    }

    /**
     * 检查物品是否是肥料（骨粉或带有 #c:fertilizer 标签的物品）
     */
    private boolean isFertilizer(ItemStack stack) {

if (stack.isEmpty()) {

    return false;

}

// 检查是否是骨粉

if (stack.is(net.minecraft.world.item.Items.BONE_MEAL)) {

    return true;

}

// 检查是否带有 #c:fertilizer 标签

return stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "fertilizer")));
    }
}