// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.prefab.crop.Ic2PlantType;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * <p><b>EN:</b> Four-stage metal crop whose last stage needs an ore or storage block in the root zone
 * (titanium and uranium). 0.1.8.2: the two crops used the legacy per-tick model. That model
 * applied Gain twice: the number of harvest rolls grew with Gain, and each roll also gave
 * 1 + Gain / 4 items. A Gain 31 crop thus dropped up to 96 items in one harvest, and the growth
 * points rose every tick. They now use the IC2 crop cycle like the other metal crops: one small
 * dust per roll, growth time 800 (2000 for the last stage), and half the drop chance.
 * <p><b>ZH:</b> 最后阶段需要根系范围内有矿石或储存方块的四阶段金属作物（钛、铀）。0.1.8.2：这两种作物原先使用旧的逐 tick 模型，
 * 该模型把产量属性计算了两次：收获次数随产量增加，每次又给出 1 + 产量 / 4 个物品，产量 31 的作物一次收获最多掉落 96 个，
 * 且生长点每 tick 都增加。现在与其他金属作物一样使用 IC2 作物周期：每次 1 个小撮粉，生长时间 800（最后阶段 2000），掉落概率减半。
 * <p><b>JA:</b> 最終段階で根の範囲に鉱石か貯蔵ブロックが必要な 4 段階の金属作物（チタン・ウラン）。0.1.8.2：この 2 種は旧来の毎 tick モデルを
 * 使っていた。このモデルは収量値を二重に適用していた：収穫回数が収量値で増え、さらに 1 回ごとに 1 + 収量 / 4 個を出していた。
 * 収量 31 の作物は 1 回の収穫で最大 96 個を落とし、成長ポイントも毎 tick 増えていた。現在は他の金属作物と同じく IC2 作物サイクルを使う：
 * 1 回につき小さな粉 1 個、成長時間 800（最終段階は 2000）、ドロップ確率は半分。
 */
public abstract class OreRootCrop extends Ic2PlantType {
    private final String id;
    private final String[] traits;
    private final PlantStats stats;
    private final Supplier<Item> product;
    private final Supplier<Block[]> roots;

    protected OreRootCrop(String id, String[] traits, PlantStats stats, Supplier<Item> product, Supplier<Block[]> roots) {
        this.id = id;
        this.traits = traits;
        this.stats = stats;
        this.product = product;
        this.roots = roots;
    }

    @Override public String getTypeId() { return id; }
    @Override public String getModId() { return Singularity_Iteration.MOD_ID; }
    @Override public String getFoundBy() { return "Notch"; }
    @Override public String[] getTraits() { return traits; }
    @Override public PlantStats getStats() { return stats; }
    @Override public int getMaxGrowthStage() { return 4; }
    @Override public int getHarvestStage() { return 4; }
    @Override public int getStageAfterHarvest() { return 2; }
    @Override public int getRootDepth(IPlanter planter) { return 5; }

    @Override
    public boolean canGrow(IPlanter planter) {
        int stage = planter.getGrowthStage();
        if (stage >= getMaxGrowthStage()) return false;
        if (stage < getMaxGrowthStage() - 1) return true;
        for (Block root : roots.get()) if (planter.isBlockBelow(root)) return true;
        return false;
    }

    /** EN: Blocks that let the last stage grow (scanner HUD). ZH: 使最后阶段能生长的方块（扫描仪 HUD 使用）。JA: 最終段階を成長させるブロック（スキャナー HUD が使う）。 */
    public Block[] rootBlocks() {
        return roots.get().clone();
    }

    @Override
    public int getGrowthTime(IPlanter planter) {
        return planter.getGrowthStage() == 3 ? 2000 : 800;
    }

    /** EN: Half of the IC2 drop chance, like the other four-stage metal crops. ZH: IC2 掉落概率的一半，与其他四阶段金属作物相同。JA: 他の 4 段階金属作物と同じく、IC2 のドロップ確率の半分。 */
    @Override
    public double dropGainChance() {
        return super.dropGainChance() / 2;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        return new ItemStack[]{new ItemStack(product.get())};
    }

    @Override
    public String getTexture(int stage) {
        return "mio_icif:block/crop/" + id + "_" + Math.clamp(stage, 1, getMaxGrowthStage());
    }
}
