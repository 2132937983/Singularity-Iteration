---
navigation:
  title: "ナノスーツのユニット"
  icon: mio_icif:armor/item_armor_nano_chestplate
  parent: suit/index.md
  position: 1
item_ids:
  - mio_icif:module/item_module_myofiber
  - mio_icif:module/item_module_thermoregulator
  - mio_icif:module/item_module_emp_emitter
  - mio_icif:module/item_module_optical_camo
  - mio_icif:module/item_module_kinetic_mesh
  - mio_icif:module/item_module_recon_cloud
  - mio_icif:module/item_module_jammer
  - mio_icif:module/item_module_thermal_camo
  - mio_icif:module/item_module_gecko_setae
  - mio_icif:module/item_module_shock_gel
  - mio_icif:module/item_module_heat_sink_fins
  - mio_icif:module/item_module_micro_capacitor
  - mio_icif:module/item_module_solar_skin
  - mio_icif:module/item_module_toxin_filter
  - mio_icif:module/item_module_self_repair
  - mio_icif:module/item_module_hydrophobic
  - mio_icif:module/item_module_kinetic_generator
  - mio_icif:module/item_nano_lattice_densifier
---

# ナノスーツのユニット

ナノスーツは強化ユニットを受け付ける。ナノのユニットは軽く、満載でも約 10 EU/t しか使わない。
強い効果はバーストで、ナノ予備量を使い、熱を加える。

## ナノコア

- **予備量**（0〜100）：バーストが使う。ユニット付きのナノ部位の着用中、毎秒 2 回復する。1 ポイントにスーツの電力 400 EU を使う。
- **熱**（0〜100、放熱フィンで増える）：バーストのたびにたまる。毎秒 4 放熱する。
- **過熱**：上限に達すると過熱する。熱が上限の 40% を下回るまでバーストできない。フィンがないと移動も 20% 遅くなる。

## バーストキー

**V**（メイン）と **B**（サブ）。奇点管理パネルで各キーにユニットを割り当てる。「自動」の順番：キネティックダッシュ、光学迷彩、運動装甲メッシュ、EMP、ナノ偵察雲。
運動装甲メッシュと光学迷彩は 2 つのキーに同時に割り当てられず、同時に動くこともない。

## ユニット

| ユニット | 部位 | LP | 消費 | バースト | 種別 |
|---|---|---|---|---|---|
| <ItemLink id="mio_icif:module/item_module_myofiber" /> | レギンス | 2 | 3 EU/t | Dash | ナノ |
| <ItemLink id="mio_icif:module/item_module_thermoregulator" /> | チェストプレート | 2 | 2 EU/t | - | ナノ |
| <ItemLink id="mio_icif:module/item_module_emp_emitter" /> | チェストプレート | 3 | - | EMP | ナノ |
| <ItemLink id="mio_icif:module/item_module_optical_camo" /> | チェストプレート | 3 | - | Camo | ナノ |
| <ItemLink id="mio_icif:module/item_module_kinetic_mesh" /> | チェストプレート | 2 | - | Mesh | ナノ |
| <ItemLink id="mio_icif:module/item_module_recon_cloud" /> | ヘルメット | 2 | - | Recon | ナノ |
| <ItemLink id="mio_icif:module/item_module_jammer" /> | ヘルメット | 2 | 4 EU/t | - | ナノ |
| <ItemLink id="mio_icif:module/item_module_thermal_camo" /> | チェストプレート | 2 | 2 EU/t | - | ナノ |
| <ItemLink id="mio_icif:module/item_module_gecko_setae" /> | ブーツ | 2 | 1 EU/t | - | ナノ |
| <ItemLink id="mio_icif:module/item_module_shock_gel" /> | ブーツ | 1 | - | - | ナノ |
| <ItemLink id="mio_icif:module/item_module_heat_sink_fins" /> | 胴 / 脚 | 1 | - | - | ナノ |
| <ItemLink id="mio_icif:module/item_module_micro_capacitor" /> | 全部位 | 1 | - | - | ナノ |
| <ItemLink id="mio_icif:module/item_module_solar_skin" /> | 頭 / 胴 | 1 | +2 EU/t | - | ナノ |
| <ItemLink id="mio_icif:module/item_module_toxin_filter" /> | ヘルメット | 1 | 1 EU/t | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_self_repair" /> | 全部位 | 1 | 2000 EU / heal | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_hydrophobic" /> | 胴 / 脚 | 1 | - | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_kinetic_generator" /> | レギンス | 1 | +1 EU / block | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | チェストプレート | 1 | 6 EU/t | - | 共用 |
| <ItemLink id="mio_icif:module/item_module_blast_warning" /> | 頭 / 胴 | 1 | 2 EU/t | - | 共用 |

## Mk.II

ナノ格子用のユニットにはすべて Mk.II がある：Mk.I ユニット、生体回路、縮退ゼロ物質 4 個、系統の素材 3 つで作る。
Mk.II は効果が強く（ツールチップに内容が出る）、消費電力が 20% 少ない。LP は同じ。
強化するには、改造台に部位を入れ、ユニットスロットに Mk.II を 1 個入れる。改造台が Mk.I と入れ替え、Mk.I を返す。

## HF 同期

メインハンドの起動したナノセイバーは、ナノ筋繊維と運動装甲メッシュの織り目に同期する。Mk.I の源は 1 段、Mk.II は 2 段、最大 3 段。
1 段：攻撃速度 +12%、防具貫通 15%。2 段：+15%、22%。3 段：+18%、30%。段が上がるほど刃が強く震える。

## ナノから量子へ

ナノ部位から量子部位をクラフトすると、共用ユニットは量子部位へ移る。ナノ専用ユニットと格子高密度化材はアイテムとして戻る。

## サーバー設定

コモンコンフィグの NanoSuit 節に、ユニットごとのスイッチ、PvP スイッチ（初期値はすべてオン）、数値がある。

<ItemLink id="mio_icif:producer/block_quantum_modification_station" />
