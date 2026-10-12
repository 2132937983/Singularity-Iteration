// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.crop;

import com.miophas.singularity_iteration.core.api.crop.PlantType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p><b>EN:</b> Candidate list of breeding research (0.1.8.2). It uses the crossing weights of natural
 * crossing ({@link PlantHybridization#calculateRatio}), summed over the two parents. The list
 * excludes the weed and candidates with weight 0, sorts by weight (high first) and then by id,
 * and keeps at most {@link #MAX_CANDIDATES}. Client and server compute the same list, so a list
 * index identifies a target.
 * <p><b>ZH:</b> 交配研究的候选列表（0.1.8.2）。使用自然杂交的权重（{@link PlantHybridization#calculateRatio}），对两个亲本求和。
 * 列表排除杂草与权重为 0 的候选，按权重（高者在前）再按 ID 排序，最多保留 {@link #MAX_CANDIDATES} 个。客户端与服务器计算相同的列表，因此列表索引可以标识目标。
 * <p><b>JA:</b> 交配研究の候補一覧（0.1.8.2）。自然交配の重み（{@link PlantHybridization#calculateRatio}）を 2 つの親で合計して使う。
 * 雑草と重み 0 の候補を除き、重み（高い順）、次に ID で並べ、最大 {@link #MAX_CANDIDATES} 件を残す。クライアントとサーバーが同じ一覧を計算するので、
 * 一覧の番号で目標を特定できる。
 */
public final class BreedingResearch {
    /** EN: Most candidates in the list. ZH: 列表中的最多候选数。JA: 一覧の最大候補数。 */
    public static final int MAX_CANDIDATES = 64;

    private BreedingResearch() {}

    /**
     * <p><b>EN:</b> One target: the plant, its summed weight and its share of all weights (the natural chance).
     * <p><b>ZH:</b> 一个目标：植物、其权重之和及其在全部权重中的占比（自然概率）。
     * <p><b>JA:</b> 1 つの目標：植物、重みの合計、全重みに占める割合（自然確率）。
     */
    public record Candidate(PlantType plant, int weight, double share) {
        /** EN: Research EU for this target. ZH: 该目标的研究 EU。JA: この目標の研究 EU。 */
        public long cost() { return ScannerMath.breedingCost(plant.getStats().getLevel(), share); }
        /** EN: Key "modId:typeId". ZH: 键 "modId:typeId"。JA: キー "modId:typeId"。 */
        public String key() { return plant.getModId() + ":" + plant.getTypeId(); }
    }

    /**
     * <p><b>EN:</b> Candidates for the parents {@code a} and {@code b}; empty when a parent is null.
     * <p><b>ZH:</b> 亲本 {@code a} 与 {@code b} 的候选；任一亲本为 null 时为空。
     * <p><b>JA:</b> 親 {@code a} と {@code b} の候補。どちらかが null なら空。
     */
    public static List<Candidate> candidates(PlantType a, PlantType b) {
        List<Candidate> out = new ArrayList<>();
        if (a == null || b == null) return out;
        int total = 0;
        List<int[]> weights = new ArrayList<>();
        List<PlantType> plants = new ArrayList<>();
        for (PlantType candidate : PlantRegistry.instance.getAllPlants()) {
            if ("weed".equals(candidate.getTypeId())) continue;
            int w = PlantHybridization.calculateRatio(candidate, a) + PlantHybridization.calculateRatio(candidate, b);
            if (w <= 0) continue;
            plants.add(candidate);
            weights.add(new int[]{w});
            total += w;
        }
        for (int i = 0; i < plants.size(); i++) {
            int w = weights.get(i)[0];
            out.add(new Candidate(plants.get(i), w, total == 0 ? 0 : w / (double) total));
        }
        out.sort(Comparator.comparingInt((Candidate c) -> -c.weight()).thenComparing(Candidate::key));
        return out.size() > MAX_CANDIDATES ? new ArrayList<>(out.subList(0, MAX_CANDIDATES)) : out;
    }
}
