package com.singularity_iteration.mio_icif.entity.villager;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Items.mio_icif_items;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 村民系统注册
 * 
 * 职责�? * 1. 注册期货商人村民职业
 * 2. 注册兴趣点（POI）类�? * 3. 配置交易内容
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
@SuppressWarnings("null")
public class mio_icif_villagers {

    // POI 类型注册�
public static final DeferredRegister<PoiType> POI_TYPES = 
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, Singularity_Iteration.MOD_ID);

    // 村民职业注册�
public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = 
            DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, Singularity_Iteration.MOD_ID);

    // POI 类型�?ResourceKey
    public static final ResourceKey<PoiType> FUTURE_ELC_POI_KEY = ResourceKey.create(
            BuiltInRegistries.POINT_OF_INTEREST_TYPE.key(),
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "future_elc_poi")
    );

    // 期货机兴趣点 - 使用所有可能的方块状态
public static final DeferredHolder<PoiType, PoiType> FUTURE_ELC_POI = POI_TYPES.register("future_elc_poi",
            () -> {
                // 获取期货机方块的所有可能状态
            Set<BlockState> states = ImmutableSet.copyOf(
                        mio_icif_blocks.FUTURE_ELC.get().getStateDefinition().getPossibleStates()
                );
                Singularity_Iteration.LOGGER.info("Registering POI type for future_elc with {} states: {}",
                        states.size(), states);
                // 创建 POI 类型：最终?1 个村民使用，范围 1 �
            return new PoiType(states, 1, 1);
            });

    // 期货商人职业 - 使用 Holder 直接比较�?.21 正确方式�
public static final DeferredHolder<VillagerProfession, VillagerProfession> FUTURE_TRADER = VILLAGER_PROFESSIONS.register("future_trader",
            () -> {
                Singularity_Iteration.LOGGER.info("Registering villager profession: future_trader");
                
                // 创建谓词：使�?ResourceKey 进行匹配
                Predicate<Holder<PoiType>> poiPredicate = holder -> holder.is(FUTURE_ELC_POI_KEY);
                
                return new VillagerProfession(
                        Singularity_Iteration.MOD_ID + ":future_trader",
                        poiPredicate,  // heldWorkstation - 村民持有的工作站�
                    poiPredicate,  // acquirableWorkstation - 村民可以获取的工作站�
                    ImmutableSet.of(),  // gatherableItems - 可收集物�
                    ImmutableSet.of(),  // secondaryJobSites - 辅助工作站点
                        SoundEvents.VILLAGER_WORK_LIBRARIAN);
            });

    /**
     * 注册到事件总线
     */
    public static void register(IEventBus eventBus) {
        Singularity_Iteration.LOGGER.info("Registering villager system...");
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
        Singularity_Iteration.LOGGER.info("Villager system registered");
    }

    /**
     * 自定义交易类 - 用绿宝石购买物品
     */
    public static class ItemsForEmeraldsTrade implements VillagerTrades.ItemListing {
        private final Item item;
        private final int itemCount;
        private final int emeraldCost;
        private final int maxUses;
        private final float priceMultiplier;

        public ItemsForEmeraldsTrade(Item item, int itemCount, int emeraldCost, int maxUses, float priceMultiplier) {
            this.item = item;
            this.itemCount = itemCount;
            this.emeraldCost = emeraldCost;
            this.maxUses = maxUses;
            this.priceMultiplier = priceMultiplier;
        }

        @Override
        public MerchantOffer getOffer(net.minecraft.world.entity.Entity trader, net.minecraft.util.RandomSource random) {
            ItemCost cost = new ItemCost(Items.EMERALD, emeraldCost);
            ItemStack result = new ItemStack(item, itemCount);
            return new MerchantOffer(cost, result, maxUses, 1, priceMultiplier);
        }
    }

    /**
     * 自定义交易类 - 用物品换绿宝�
 */
    public static class EmeraldForItemsTrade implements VillagerTrades.ItemListing {
        private final Item item;
        private final int itemCount;
        private final int maxUses;
        private final int villagerXp;

        public EmeraldForItemsTrade(Item item, int itemCount, int maxUses, int villagerXp) {
            this.item = item;
            this.itemCount = itemCount;
            this.maxUses = maxUses;
            this.villagerXp = villagerXp;
        }

        @Override
        public MerchantOffer getOffer(net.minecraft.world.entity.Entity trader, net.minecraft.util.RandomSource random) {
            ItemCost cost = new ItemCost(item, itemCount);
            ItemStack result = new ItemStack(Items.EMERALD);
            return new MerchantOffer(cost, result, maxUses, villagerXp, 0.05f);
        }
    }
    
    /**
     * 自定义交易类 - 用硬币购买物�
 */
    public static class ItemsForCoinsTrade implements VillagerTrades.ItemListing {
        private final Item item;
        private final int itemCount;
        private final int coinCost;
        private final int maxUses;
        private final float priceMultiplier;

        public ItemsForCoinsTrade(Item item, int itemCount, int coinCost, int maxUses, float priceMultiplier) {
            this.item = item;
            this.itemCount = itemCount;
            this.coinCost = coinCost;
            this.maxUses = maxUses;
            this.priceMultiplier = priceMultiplier;
        }

        @Override
        public MerchantOffer getOffer(net.minecraft.world.entity.Entity trader, net.minecraft.util.RandomSource random) {
            ItemCost cost = new ItemCost(mio_icif_normal.COIN.get(), coinCost);
            ItemStack result = new ItemStack(item, itemCount);
            return new MerchantOffer(cost, result, maxUses, 1, priceMultiplier);
        }
    }

    /**
     * 注册交易内容
     */
    @SubscribeEvent
    public static void registerTrades(VillagerTradesEvent event) {
        if (event.getType() == FUTURE_TRADER.get()) {
            Singularity_Iteration.LOGGER.info("Registering trades for future_trader");
            
            // 新手级别交易（等�?1�
        event.getTrades().put(1, List.of(
                    // 用绿宝石买硬币（1 绿宝�?5 硬币�
                new ItemsForEmeraldsTrade(mio_icif_normal.COIN.get(), 5, 1, 12, 0.05f),
                    // 用硬币换绿宝石（10 硬币=1 绿宝石）
                    new EmeraldForItemsTrade(mio_icif_normal.COIN.get(), 10, 12, 2)
            ));

            // 学徒级别交易（等�?2�
        event.getTrades().put(2, List.of(
                    // 出售空电池（8 绿宝石）
                    new ItemsForEmeraldsTrade(mio_icif_normal.BAT_LEV0.get(), 1, 8, 8, 0.05f),
                    // 出售橡胶�? 橡胶=10 绿宝石）
                    new ItemsForEmeraldsTrade(mio_icif_resources.RUBBER.get(), 4, 10, 10, 0.05f)
            ));

            // 老手级别交易（等�?3�
        event.getTrades().put(3, List.of(
                    // 出售木转子（6 绿宝石）
                    new ItemsForEmeraldsTrade(mio_icif_items.ROTOR_WOOD.get(), 1, 6, 6, 0.05f),
                    // 出售树脂�? 树脂=8 绿宝石）
                    new ItemsForEmeraldsTrade(mio_icif_resources.HARZ.get(), 4, 8, 8, 0.05f)
            ));

            // 专家级别交易（等�?4�
        event.getTrades().put(4, List.of(
                    // 出售高级电池�? 绿宝石）
                    new ItemsForEmeraldsTrade(mio_icif_normal.ADVBAT_LEV0.get(), 1, 4, 4, 0.05f),
                    // 出售电路板（2 电路�?6 绿宝石）
                    new ItemsForEmeraldsTrade(mio_icif_normal.CIRCUIT.get(), 2, 6, 6, 0.05f)
            ));

            // 大师级别交易（等�?5�
        event.getTrades().put(5, List.of(
                    // 出售能量水晶�? 绿宝石）
                    new ItemsForEmeraldsTrade(mio_icif_normal.CRYSTAL_LEV0.get(), 1, 2, 2, 0.05f),
                    // 出售兰波顿水晶电池（1 绿宝石）
                    new ItemsForEmeraldsTrade(mio_icif_normal.CRYSTAL_CHARGEBAT_LEV0.get(), 1, 1, 1, 0.05f)
            ));
            
            // 添加新物品交易（使用硬币�
        // 情报�?000 硬币�? 等级 3
            event.getTrades().put(3, List.of(
                    new ItemsForCoinsTrade(mio_icif_normal.INTELLIGENCE.get(), 1, 1000, 5, 0.05f)
            ));
            
            // 价格修改器（2000 硬币�? 等级 4
            event.getTrades().put(4, List.of(
                    new ItemsForCoinsTrade(mio_icif_normal.MODIFY.get(), 1, 2000, 3, 0.05f)
            ));
        }
    }
}

