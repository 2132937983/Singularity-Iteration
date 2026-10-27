---
navigation:
  title: "量子スーツのアップグレード"
  icon: mio_icif:armor/item_armor_quantum_helmet
  parent: index.md
  position: 5
---

# 量子スーツのアップグレード

量子スーツはアップグレードユニットを受け付ける。各ユニットがセンサーか防御の機能を1つ加える。
汎用スーツ改造台がユニットを取り付け、取り外す。0.1.8.5 からナノスーツもユニットを受け付ける（「ナノスーツのユニット」を参照）。
各部位のユニットスロット数は固定だ。ヘルメット4、チェストプレート3、レギンス2、ブーツ2。
量子拡張ベイで部位ごとに 1 枠増やせる（「量子アクション」を参照）。
量子ユニット 2 つをもつれで結ぶとシナジーが働く。スーツ計算機には演算帯域がある（「もつれとシナジー」を参照）。
量子もつれ中継器があれば、スーツで僚機を指揮できる：コマンドホイール、命令、ホロテーブル（「量子指揮」を参照）。
HUDユニットは量子ヘルメット着用時だけデータを表示する。ヘルメットがバイザーになる。
取り付けた各ユニットは装備コンソールにスイッチを加える。スイッチがオンの間だけ電力を使う。
ナノスーツと量子スーツは光る。ライトと回路ラインが発光色で光り、充電が半分を切ると琥珀色、4 分の 1 を切ると赤になる。ゆっくり呼吸するように明滅し、エネルギーの帯が体を流れ下り、残量が少ないと鼓動のように瞬く。空の部位は暗い。充電済みの部位が起動すると（着用、武装庫の装着、空からの充電）、光が胸のコアから広がる。シェーダーパックはこの光を発光として扱う。クライアント設定 suit_glow と suit_glow_strength、または装備コンソールの外観ページで切り替える。
0.1.10.30 から量子スーツもナノスーツと同じく配色を設定できる（装備コンソール→外観ページ→配色）：部位ごとの装甲色と発光色。発光色はフレームコア外装の光る部分の色にもなる。

| ユニット | 装着部位 | 消費電力 |
|---|---|---|
| <ItemLink id="mio_icif:module/item_module_ore_scanner" /> | ブーツ / レギンス | 24 EU/t（オン時） |
| <ItemLink id="mio_icif:module/item_module_grid_telemetry" /> | レギンス / ヘルメット | 4 EU/t（オン時） |
| <ItemLink id="mio_icif:module/item_module_entity_esp" /> | ヘルメット | 12 EU/t（オン時） |
| <ItemLink id="mio_icif:module/item_module_ballistic" /> | ヘルメット | 4 EU/t（オン時） |
| <ItemLink id="mio_icif:module/item_module_blast_warning" /> | チェストプレート / ヘルメット | 2 EU/t（オン時） |
| <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | ヘルメット | 8 EU/t（オン時） |
| <ItemLink id="mio_icif:module/item_module_holomap" /> | ヘルメット | 10 EU/t（オン時） |
| <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | チェストプレート | 6 EU/t（オン時） |
| <ItemLink id="mio_icif:module/item_module_deflector" /> | チェストプレート | ダメージ1点につき4000 EU、1回最大12点 |
| <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> | レギンス / ブーツ | アクション：量子転移（コヒーレンス） |
| <ItemLink id="mio_icif:module/item_module_phase_modulator" /> | チェストプレート | アクション：位相シフト（コヒーレンス） |
| <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> | チェストプレート / レギンス | アクション：一斉射撃（コヒーレンス） |
| <ItemLink id="mio_icif:module/item_module_coherence_capacitor" /> | レギンス / ブーツ | 常時：コヒーレンス上限 +50 |
| <ItemLink id="mio_icif:module/item_module_compute_core" /> | チェストプレート / レギンス | オン時 6 EU/t、帯域 +8 QB、リンク +1 |

<SubPages id="suit/index.md" icons={true} />
