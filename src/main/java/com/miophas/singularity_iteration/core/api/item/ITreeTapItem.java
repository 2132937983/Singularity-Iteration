package com.miophas.singularity_iteration.core.api.item;

/**
 * 树液采集器物品标记接口。
 * <p>
 * 实现此接口的物品可以被识别为树液采集器工具，
 * 用于从橡胶木中提取树脂。
 *
 * <p>附属模组可以让自定义物品实现本接口，即可被 {@code IRubberTreeAPI}
 * 与橡胶木方块识别并用于采集树脂。默认参数对应普通（木质）采集器：
 * 每次消耗 1 点耐久、掉落 1~3 个树脂。电动版本可覆盖相应方法以调整行为。
 */
public interface ITreeTapItem {

    /**
     * 采集一次对工具造成的耐久损耗。
     *
     * <p>默认 1；电动采集器应返回 0（改为消耗电能）。
     *
     * @return 耐久损耗值
     */
    default int getDurabilityCost() {
        return 1;
    }

    /**
     * 采集一次可获得树脂的最小数量（含）。
     *
     * @return 最小数量
     */
    default int getResinDropMin() {
        return 1;
    }

    /**
     * 采集一次可获得树脂的最大数量（含）。
     *
     * @return 最大数量
     */
    default int getResinDropMax() {
        return 3;
    }
}
