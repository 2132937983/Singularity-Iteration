---
navigation:
  title: "もつれとシナジー"
  icon: mio_icif:module/item_module_compute_core
  parent: suit/index.md
  position: 4
item_ids:
  - mio_icif:module/item_module_compute_core
  - mio_icif:module/item_module_smart_guidance_mk2
  - mio_icif:module/item_module_resonance_field_mk2
---

# もつれとシナジー

0.1.10.1 から、2 つの量子ユニットを **もつれ** で結べる。結んだ組は **シナジー** として働き、単体では持たない効果を生む。

## リンク

- 量子チェストが **リンクハブ** で、リンクを保存する。量子チェストを着けていなければシナジーは働かない。
- ハブのリンクは **2 本**。<ItemLink id="mio_icif:module/item_module_compute_core" /> が有効なら 3 本。
- 組の両ユニットが、装着中の量子部位に取り付けられている必要がある。両方がオンで給電されている間だけシナジーが働く。
- ユニットを付け替えてもリンクはチェストに残り、ユニットが戻れば再び働く。外された MOD のユニットへのリンクは保存されたまま何もしない。

## もつれマトリクス

特異点管理パネルを開き、量子チェストを選んで「量子コア」の下の **リンク** 行をクリックする。

- 左：量子ユニットが円周上のノードとして並ぶ。薄い破線：リンクできる組。実線：リンク済みだが片方がオフ。脈打つ二重らせん：シナジーが働いている。
- 右：全シナジーと状態（LINKED、READY、OFFLINE、OVER CAP、MISSING）。行をクリックするとリンク／解除。
- ユニットを別のユニットへ **ドラッグ** してもよい。ドラッグ中はその組のシナジーが表示され、離すとリンク／解除する。
- 下部に使用中のリンク数と演算帯域が出る。

HUD では、働いているシナジーが照準の横に小さなチップで表示される。

## シナジー

| シナジー | ユニット | 効果 |
|---|---|---|
| 予測誘導 | <ItemLink id="mio_icif:module/item_module_smart_guidance" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | 誘導弾は目標が来る位置へ飛ぶ。生物が歩く経路に沿って予測する。旋回 +4 度/tick、ロック +2。 |
| 脅威透視 | <ItemLink id="mio_icif:module/item_module_entity_esp" /> + <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | 自分を狙う敵が壁越しに赤く光り、攻撃が届くまでの時間をリングで示す。 |
| 地脈ホロ | <ItemLink id="mio_icif:module/item_module_holomap" /> + <ItemLink id="mio_icif:module/item_module_ore_scanner" /> | 震探で見つけた鉱石を、ホロマップと戦術マップに 3D ボクセルで表示する。 |
| 弾道同期 | <ItemLink id="mio_icif:module/item_module_ballistic" /> + <ItemLink id="mio_icif:module/item_module_smart_guidance" /> | ロック時間 -30%。誘導弾の予定軌道を描く。 |
| 爆心予報 | <ItemLink id="mio_icif:module/item_module_blast_warning" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | 爆発の半径を 3D の球で示し、退避方向を矢印で示す。 |
| 共鳴偏向 | <ItemLink id="mio_icif:module/item_module_deflector" /> + <ItemLink id="mio_icif:module/item_module_resonance_field" /> | 偏向器が止めたダメージの 25% を共鳴として蓄え、8 に達すると放出して半径 4 ブロックの敵に当てる。 |
| 残像欺瞞 | <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | 転移の出発点に残像デコイが残る。2 秒間、自分を狙っていた敵はデコイへ向かい、自分を狙い直せない。 |
| 位相反撃 | <ItemLink id="mio_icif:module/item_module_phase_modulator" /> + <ItemLink id="mio_icif:module/item_module_deflector" /> | 位相シフト中にすり抜けたダメージ（最大 20）を、解除時に衝撃波として放つ。 |
| 艦隊データリンク | <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> + <ItemLink id="mio_icif:module/item_module_entity_esp" /> | 僚機が見た敵を HUD に表示し、自分が狙っている敵を僚機が攻撃する。 |

各シナジーにはサーバー設定（QuantumSuit の synergies）のスイッチがある。シナジーの放出がプレイヤーに当たるのは PvP が有効なときだけ。

## 連鎖コンボ

コンボにリンクは要らず、タイミングだけで発動する。発動するとバナーと効果音が出る。

- **量子トンネル：** 量子転移の後 1 秒以内の位相シフトは、コヒーレンスが半分で済む。
- **凍結斉射：** 自分の時間歪曲の中で一斉射撃する。減速中の敵が最大 8 体まで目標になり、溜め時間なしで同時に着弾し、ダメージは 1.5 倍。
- **静止弾幕：** 力場をキーで解除すると、止めていた投射物がすべて撃った者へ向かって飛び返る（自分の弾として扱う）。

## 演算帯域

量子部位で有効なユニットは、スーツ計算機に負荷（QB）をかける。2 つの部位に付けた同じユニットは 1 回だけ数える。Mk.II は 1 QB 多い。

| QB | ユニット |
|---|---|
| 3 | 生体スキャナー（ESP）、行動予測器、ホロマップ、震探鉱石スキャナー、位相変調器、量子もつれ中継器 |
| 2 | スマート弾道誘導、脅威センサー、位相跳躍ドライブ |
| 1 | 弾道計算機、爆発警告タイマー、電網テレメトリ、量子偏向器、力場共振解体 |
| 0 | その他のユニット |

予算は **20 QB**、演算コアが有効なら 28 QB。0.1.9.2 で組めた構成はすべて 20 QB に収まる。予算を超えるとスーツは **演算飽和** する：センサーの更新が半分になり、コヒーレンスの回復が半分の速さになる。機能が止まることはない。照準付近の QB メーターが赤くなる。

## 量子演算コア

<ItemLink id="mio_icif:module/item_module_compute_core" />（チェストかレギンス、6 EU/t）は帯域予算を 8 QB 増やし、ハブのリンクを 1 本増やす。

## Mk.II：誘導と共振解体

- <ItemLink id="mio_icif:module/item_module_smart_guidance_mk2" />：ロック最大 10、誘導弾の旋回は最大 20 度/tick（Mk.I：6 ロック、14 度）。
- <ItemLink id="mio_icif:module/item_module_resonance_field_mk2" />：半径 3.75、1 tick に最大 9 ブロック（Mk.I：半径 2.5、6 ブロック）。

震探鉱石スキャナー、電網テレメトリ、生体スキャナー、弾道計算機の Mk.II は各ページを参照。

## レシピ

<RecipesFor id="mio_icif:module/item_module_compute_core" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_smart_guidance_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_resonance_field_mk2" fallbackText="-" />
