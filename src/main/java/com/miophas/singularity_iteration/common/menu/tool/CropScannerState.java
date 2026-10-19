// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.tool;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * <p><b>EN:</b> Saved state of an Advanced Crop Scanner (0.1.8.2). It lives in the scanner's custom data
 * under the key "Scanner": the 37 internal slots, the scan progress of the 27 analysis slots, the
 * unlock flags, and the breeding and gene-editing jobs. The menu loads it when it opens and saves
 * it when the contents change and when it closes. A scanner of 0.1.8.1 or older ("HandHeldItems",
 * three slots) is migrated: input and output go to the first two grid slots, the battery to the
 * battery slot.
 * <p><b>ZH:</b> 次世代作物扫描仪的保存状态（0.1.8.2）。保存在扫描仪自定义数据的 "Scanner" 键下：37 个内部槽位、27 个分析槽位的扫描进度、解锁标志，
 * 以及交配研究与基因编辑任务。菜单打开时读取，内容变化与关闭时保存。0.1.8.1 及更早的扫描仪（"HandHeldItems"，3 个槽位）会被迁移：
 * 输入与输出放入前两个网格槽位，电池放入电池槽。
 * <p><b>JA:</b> 次世代型作物スキャナーの保存状態（0.1.8.2）。スキャナーのカスタムデータの "Scanner" キーにある：37 個の内部スロット、27 個の分析スロットの
 * 走査進捗、アンロックフラグ、交配研究と遺伝子改変の作業。メニューは開くときに読み、内容が変わったときと閉じるときに保存する。
 * 0.1.8.1 以前のスキャナー（"HandHeldItems"、3 スロット）は移行する：入力と出力はグリッドの先頭 2 スロットへ、電池は電池スロットへ。
 */
public final class CropScannerState {
    public static final int GRID = 27;
    public static final int SLOT_BATTERY = 27, SLOT_UPGRADE = 28, UPGRADES = 4;
    public static final int SLOT_PARENT_A = 32, SLOT_PARENT_B = 33, SLOT_BREED_OUT = 34;
    public static final int SLOT_GENE = 35, SLOT_CATALYST = 36, SIZE = 37;
    public static final int UNLOCK_BREED = 1, UNLOCK_GENE = 2;
    /**
     * EN: 0.1.10.6: capacity of the internal UU-Matter tank (mB). 1 mB is one catalyst, the same as one UU-Matter item.
     * ZH: 0.1.10.6：内部 UU 物质储罐容量（mB）。1 mB 等于 1 个催化剂，与 1 个 UU 物质物品相同。
     * JA: 0.1.10.6：内蔵 UU 物質タンクの容量（mB）。1 mB は触媒 1 個で、UU 物質アイテム 1 個と同じ。
     */
    public static final int UU_CAPACITY = 4000;

    public final ItemStack[] items = new ItemStack[SIZE];
    /** EN: Scan ticks done per analysis slot. ZH: 每个分析槽位已完成的扫描 tick。JA: 分析スロットごとの走査済み tick。 */
    public final int[] progress = new int[GRID];
    public int unlocks;
    public String breedTarget = "";
    public int breedProgress;
    public boolean breedRun;
    public final int[] geneTarget = {0, 0, 0};
    public int geneProgress;
    public boolean geneRun;
    /** EN: UU-Matter in the internal tank (mB, 0.1.10.6). ZH: 内部储罐中的 UU 物质（mB，0.1.10.6）。JA: 内蔵タンクの UU 物質（mB、0.1.10.6）。 */
    public int uuMb;

    public CropScannerState() {
        java.util.Arrays.fill(items, ItemStack.EMPTY);
    }

    /** EN: True when the feature bit is unlocked. ZH: 该功能位已解锁时为真。JA: その機能ビットがアンロック済みなら真。 */
    public boolean unlocked(int flag) {
        return (unlocks & flag) != 0;
    }

    /** EN: Reads the unlock flags only (for tooltips and the HUD). ZH: 只读取解锁标志（用于提示与 HUD）。JA: アンロックフラグだけを読む（ツールチップと HUD 用）。 */
    public static int unlocksOf(ItemStack scanner) {
        CustomData data = scanner.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getCompound("Scanner").getInt("Unlock");
    }

    public static CropScannerState load(ItemStack scanner, HolderLookup.Provider registries) {
        CropScannerState state = new CropScannerState();
        CustomData data = scanner.get(DataComponents.CUSTOM_DATA);
        if (data == null) return state;
        CompoundTag root = data.copyTag();
        if (root.contains("Scanner", Tag.TAG_COMPOUND)) {
            CompoundTag tag = root.getCompound("Scanner");
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < SIZE) state.items[slot] = ItemStack.parse(registries, entry.getCompound("Item")).orElse(ItemStack.EMPTY);
            }
            int[] p = tag.getIntArray("Progress");
            System.arraycopy(p, 0, state.progress, 0, Math.min(p.length, GRID));
            state.unlocks = tag.getInt("Unlock");
            state.breedTarget = tag.getString("BreedTarget");
            state.breedProgress = tag.getInt("BreedProgress");
            state.breedRun = tag.getBoolean("BreedRun");
            int[] t = tag.getIntArray("GeneTarget");
            for (int i = 0; i < Math.min(3, t.length); i++) state.geneTarget[i] = Math.clamp(t[i], 0, 31);
            state.geneProgress = tag.getInt("GeneProgress");
            state.geneRun = tag.getBoolean("GeneRun");
            state.uuMb = Math.clamp(tag.getInt("UuMb"), 0, UU_CAPACITY);
        } else if (root.contains("HandHeldItems", Tag.TAG_LIST)) {
            ItemStack[] old = new ItemStack[3];
            com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool.loadHandHeldInventory(scanner, old, registries);
            state.items[0] = old[0];
            state.items[1] = old[1];
            state.items[SLOT_BATTERY] = old[2];
        }
        return state;
    }

    public void save(ItemStack scanner, HolderLookup.Provider registries) {
        if (scanner.isEmpty()) return;
        CompoundTag root = scanner.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < SIZE; i++) {
            if (items[i] == null || items[i].isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Item", items[i].save(registries));
            list.add(entry);
        }
        tag.put("Items", list);
        tag.putIntArray("Progress", progress.clone());
        tag.putInt("Unlock", unlocks);
        tag.putString("BreedTarget", breedTarget);
        tag.putInt("BreedProgress", breedProgress);
        tag.putBoolean("BreedRun", breedRun);
        tag.putIntArray("GeneTarget", geneTarget.clone());
        tag.putInt("GeneProgress", geneProgress);
        tag.putBoolean("GeneRun", geneRun);
        if (uuMb > 0) tag.putInt("UuMb", uuMb);
        root.put("Scanner", tag);
        root.remove("HandHeldItems");
        scanner.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
