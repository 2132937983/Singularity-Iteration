// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import java.util.Map;
import net.minecraft.network.chat.Component;

/**
 * <p><b>EN:</b> Scientific names of the crop-stick plants (0.1.8.2), for the Advanced Crop Scanner HUD
 * and menu. A real plant gets its real binomial name; a fictional IC2 plant gets a Latin name that
 * describes it. Latin names are the same in all languages, so they are not language keys. An addon
 * can give a name with the language key {@code <modId>.plant.<typeId>.scientific}; else the name is
 * "<Type> sp.".
 * <p><b>ZH:</b> 作物架植物的学名（0.1.8.2），用于次世代作物扫描仪的 HUD 与菜单。真实植物使用真实的双名法学名；IC2 的虚构植物使用描述其特征的拉丁名。
 * 拉丁名在所有语言中相同，因此不使用语言键。附属可通过语言键 {@code <modId>.plant.<typeId>.scientific} 提供学名；否则显示 “<类型> sp.”。
 * <p><b>JA:</b> 作物棒の植物の学名（0.1.8.2）。次世代型作物スキャナーの HUD とメニューで使う。実在の植物は実際の二名法の学名、IC2 の架空の植物は
 * 特徴を表すラテン名を使う。ラテン名は全言語で同じなので、言語キーにはしない。アドオンは言語キー {@code <modId>.plant.<typeId>.scientific}
 * で学名を与えられる。なければ「<タイプ> sp.」と表示する。
 */
public final class CropTaxonomy {
    private CropTaxonomy() {}

    private static final Map<String, String> NAMES = Map.ofEntries(
        // EN: Vanilla and IC2 food crops (real species).
        // ZH: 原版与 IC2 粮食作物（真实物种）。
        // JA: バニラと IC2 の食用作物（実在種）。
        Map.entry("wheat", "Triticum aestivum"),
        Map.entry("carrots", "Daucus carota subsp. sativus"),
        Map.entry("potato", "Solanum tuberosum"),
        Map.entry("beetroot", "Beta vulgaris"),
        Map.entry("pumpkin", "Cucurbita maxima"),
        Map.entry("melon", "Citrullus lanatus"),
        Map.entry("cocoa", "Theobroma cacao"),
        Map.entry("coffee", "Coffea arabica"),
        Map.entry("hops", "Humulus lupulus"),
        Map.entry("flax", "Linum usitatissimum"),
        Map.entry("reed", "Saccharum officinarum"),
        Map.entry("dandelion", "Taraxacum officinale"),
        Map.entry("rose", "Rosa gallica"),
        Map.entry("tulip", "Tulipa gesneriana"),
        Map.entry("cyazint", "Hyacinthus orientalis"),
        Map.entry("blackthorn", "Prunus spinosa"),
        Map.entry("redMushroom", "Amanita muscaria"),
        Map.entry("brownMushroom", "Agaricus bisporus"),
        Map.entry("corpse_plant", "Monotropa uniflora"),
        Map.entry("egg_plant", "Solanum melongena"),
        Map.entry("oak_sapling", "Quercus robur"),
        Map.entry("spruce_sapling", "Picea abies"),
        Map.entry("birch_sapling", "Betula pendula"),
        Map.entry("jungle_sapling", "Ceiba pentandra"),
        Map.entry("acacia_sapling", "Vachellia tortilis"),
        Map.entry("dark_oak_sapling", "Quercus nigra"),
        // EN: Fictional IC2 / SI plants (descriptive Latin).
        // ZH: IC2／SI 虚构植物（描述性拉丁名）。
        // JA: IC2／SI の架空植物（説明的なラテン名）。
        Map.entry("weed", "Herba invasiva"),
        Map.entry("redwheat", "Triticum rubrum"),
        Map.entry("stickreed", "Saccharum resiniferum"),
        Map.entry("netherwart", "Verruca infernalis"),
        Map.entry("terrawart", "Verruca terrestris"),
        Map.entry("venomilia", "Venomilia toxica"),
        Map.entry("eatingplant", "Dionaea voracissima"),
        Map.entry("ferru", "Ferrum radicans"),
        Map.entry("cyprium", "Cuprum radicans"),
        Map.entry("stagnium", "Stannum radicans"),
        Map.entry("plumbiscus", "Hibiscus plumbeus"),
        Map.entry("aurelia", "Aurelia aurifera"),
        Map.entry("shining", "Argentum lucens"),
        Map.entry("titanium", "Titanifolia radicans"),
        Map.entry("uranium", "Uranifolia radians"),
        Map.entry("blazereed", "Saccharum flammeum"),
        Map.entry("bobs_yer_uncle_ranks_berries", "Rubus smaragdinus"),
        Map.entry("corium", "Vitis coriacea"),
        Map.entry("creeper_weed", "Herba explosiva"),
        Map.entry("diareed", "Saccharum adamantinum"),
        Map.entry("ender_blossom", "Anthemis enderica"),
        Map.entry("meat_rose", "Rosa carnea"),
        Map.entry("milk_wart", "Verruca lactea"),
        Map.entry("oil_berries", "Rubus oleiferus"),
        Map.entry("slime_plant", "Mucilago viscosa"),
        Map.entry("spidernip", "Nepeta arachnoidea"),
        Map.entry("tearstalks", "Calamus lacrimosus"),
        Map.entry("withereed", "Saccharum marcescens"));

    /**
     * <p><b>EN:</b> Scientific name of the plant {@code modId:typeId} (never null).
     * <p><b>ZH:</b> 植物 {@code modId:typeId} 的学名（不为 null）。
     * <p><b>JA:</b> 植物 {@code modId:typeId} の学名（null にならない）。
     */
    public static Component scientificName(String modId, String typeId) {
        if ("mio_icif".equals(modId)) {
            String name = NAMES.get(typeId);
            if (name != null) return Component.literal(name);
        }
        String fallback = typeId.isEmpty() ? "?" : Character.toUpperCase(typeId.charAt(0)) + typeId.substring(1).replace('_', ' ') + " sp.";
        return Component.translatableWithFallback(modId + ".plant." + typeId + ".scientific", fallback);
    }

    /** EN: True when the built-in table has the plant. ZH: 内置表含有该植物时为真。JA: 内蔵表にその植物があれば真。 */
    public static boolean known(String typeId) {
        return NAMES.containsKey(typeId);
    }
}
