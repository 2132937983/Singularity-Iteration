package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/**
 * <p><b>EN:</b> Seed bag contract. The defaults read and write the SI seed data (custom data: PlantModId,
 * PlantId, GrowthSpeed, Yield, Resilience, ScanLevel); an addon can override them for its own storage.
 * The Advanced Crop Scanner, the Crop Harvester and the external farm integrations use only this interface.
 * <p><b>ZH:</b> 种子袋约定。默认实现读写 SI 种子数据（自定义数据：PlantModId、PlantId、GrowthSpeed、Yield、Resilience、ScanLevel）；
 * 附属可为自己的存储格式覆盖这些方法。次世代作物扫描仪、作物收割机与外部农场集成只使用本接口。
 * <p><b>JA:</b> 種子袋の契約。既定の実装は SI の種子データ（カスタムデータ：PlantModId、PlantId、GrowthSpeed、Yield、Resilience、ScanLevel）を
 * 読み書きする。アドオンは独自の保存形式のために上書きできる。次世代型作物スキャナー、作物収穫機、外部農場連携はこのインターフェースだけを使う。
 */
public interface ICropSeedItem {
    default net.minecraft.nbt.CompoundTag cropData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    default String cropModId(ItemStack stack) { return cropData(stack).getString("PlantModId"); }
    default String cropId(ItemStack stack) { return cropData(stack).getString("PlantId"); }
    default int cropGrowth(ItemStack stack) { return Math.clamp(cropData(stack).getInt("GrowthSpeed"), 0, 31); }
    default int cropGain(ItemStack stack) { return Math.clamp(cropData(stack).getInt("Yield"), 0, 31); }
    default int cropResistance(ItemStack stack) { return Math.clamp(cropData(stack).getInt("Resilience"), 0, 31); }
    default int cropScanLevel(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.clamp(data.copyTag().getInt("ScanLevel"), 0, 4);
    }
    default void setCropScanLevel(ItemStack stack, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ScanLevel", Math.clamp(level, 0, 4)));
    }
    /**
     * <p><b>EN:</b> Writes the three traits (each clamped to 0..31). Since 0.1.8.2; the scanner's gene editing uses it.
     * <p><b>ZH:</b> 写入三项属性（各限制在 0..31）。0.1.8.2 起，扫描仪的基因编辑使用它。
     * <p><b>JA:</b> 3 つの特性値を書き込む（各 0..31 に制限）。0.1.8.2 以降、スキャナーの遺伝子改変が使う。
     */
    default void setCropTraits(ItemStack stack, int growth, int gain, int resistance) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("GrowthSpeed", Math.clamp(growth, 0, 31));
            tag.putInt("Yield", Math.clamp(gain, 0, 31));
            tag.putInt("Resilience", Math.clamp(resistance, 0, 31));
        });
    }
}
