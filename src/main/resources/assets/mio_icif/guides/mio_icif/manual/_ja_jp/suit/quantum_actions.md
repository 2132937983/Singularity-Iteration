---
navigation:
  title: "量子アクション"
  icon: mio_icif:module/item_module_phase_jump_drive
  parent: suit/index.md
  position: 3
item_ids:
  - mio_icif:module/item_module_phase_jump_drive
  - mio_icif:module/item_module_phase_jump_drive_mk2
  - mio_icif:module/item_module_phase_modulator
  - mio_icif:module/item_module_phase_modulator_mk2
  - mio_icif:module/item_module_coherence_capacitor
  - mio_icif:module/item_module_entanglement_relay
  - mio_icif:module/item_module_deflector_mk2
  - mio_icif:module/item_module_behavior_predictor_mk2
  - mio_icif:module/item_quantum_bay_expander
---

# 量子アクション

0.1.10.0 から、量子スーツに能動アクションがある。アクションキー **V** と **B**（ナノバーストと同じキー）で使う。各キーの内容は奇点管理パネルで選ぶ：量子部位を選び、「量子コア」の下の 2 つのキー行を使う。初期設定では、身に着けている最初の 2 つのアクションが割り当てられる。

## コヒーレンス

アクションは **コヒーレンス** で払う。照準まわりのリングに表示される（0〜100）。最後のアクションから 1.5 秒後に毎秒 8 ずつ自動で回復し、スーツの EU を使う。
0 になるとコアが **デコヒーレンス** する：4 秒間どのアクションも使えず、展開中の力場は消え、バイザーが乱れ、センサーの更新が半分になる。

## アクション

| アクション | ユニット | コヒーレンス | クールダウン |
|---|---|---|---|
| 量子転移 | <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> | 25 | 1 チャージ 2.5 秒 |
| 位相シフト | <ItemLink id="mio_icif:module/item_module_phase_modulator" /> | 35 | 8 秒 |
| 力場展開 | <ItemLink id="mio_icif:module/item_module_deflector_mk2" /> | 15、以後毎秒 10 | 消えてから 6 秒 |
| 時間歪曲 | <ItemLink id="mio_icif:module/item_module_behavior_predictor_mk2" /> | 60 | 25 秒 |
| 一斉射撃 | <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> | 20 | 10 秒 |

- **量子転移：** 視線の先へ最大 12 ブロック瞬間移動する（Mk.II：20 ブロック、2 チャージ）。壁は抜けられない：目・腰・足の高さから着地点まで遮るものがなく、着地点に体が収まる必要がある。転移直後は落下ダメージを受けない。
- **位相シフト：** 1 秒間（Mk.II：1.5 秒）、生物の攻撃・投射物・爆発がすり抜ける。その間は攻撃も採掘もできない。溶岩・火・溺水・奈落のダメージは受ける。Mk.II は解除時に衝撃波を放つ。
- **力場展開：** 押すと半径 4 ブロックのドームが張られ、もう一度押すと消える。敵の投射物は空中で止まり、ドームが消えると落ちる。火の玉はドームで破裂する。敵対生物は外へ押し出される。
- **時間歪曲：** 4 秒間、10 ブロック以内の敵対生物の移動が 70% 遅くなり（ボスは半分）、その投射物は 4 分の 1 の速さになる。自分は 20% 速く動ける。ゲームの時計は全員で 1 つなので、これは範囲内だけの減速である。サーバー設定（シングルプレイ専用、初期値オフ）で、ゲーム全体を遅くすることもできる。
- **一斉射撃：** 同じディメンションの全ウィングマンが、スマート誘導のロック目標（なければ視線の先の生物）へ撃つ。全弾が同時に着弾するよう発射時刻が調整される。

Mk.I の量子偏向器と行動予測器は従来の効果のまま。Mk.II でアクションが加わる。他プレイヤーへの効果は PvP が有効なときだけ（サーバー設定 QuantumSuit にアクションごとのスイッチがある）。

## コンボとシナジー

0.1.10.1 から、アクションは連鎖コンボ（量子トンネル、凍結斉射、静止弾幕）になり、量子ユニットの組をもつれで結んでシナジーを得られる。スーツ計算機には演算帯域もある。「もつれとシナジー」を参照。

## 拡張ベイ

<ItemLink id="mio_icif:module/item_quantum_bay_expander" /> を量子部位と一緒に汎用スーツ改造台のユニットスロットに入れる。部位のユニット枠が 1 つ増える：ヘルメット 5、チェスト 4、レギンス 3、ブーツ 3。取り外しはできない。拡張していない部位は従来の枠数のまま。

## コヒーレンスキャパシタ

<ItemLink id="mio_icif:module/item_module_coherence_capacitor" />（レギンスかブーツ）はコヒーレンスの上限を 50 増やし、回復を 25% 速くする。

## レシピ

<RecipesFor id="mio_icif:module/item_module_phase_jump_drive" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_jump_drive_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_modulator" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_modulator_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_coherence_capacitor" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_entanglement_relay" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_deflector_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_behavior_predictor_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_quantum_bay_expander" fallbackText="-" />
