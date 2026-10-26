---
navigation:
  title: "行動予測器"
  icon: mio_icif:module/item_module_behavior_predictor
  parent: suit/index.md
  position: 15
item_ids:
  - mio_icif:module/item_module_behavior_predictor_mk2
  - mio_icif:module/item_module_behavior_predictor
---

# 行動予測器

<ItemImage id="mio_icif:module/item_module_behavior_predictor" scale="3" />

## 機能

このユニットは24ブロック内の各生物の動きを追う。
生物の次の1秒の経路を描く。
攻撃前の兆候（照準・溜め・詠唱・膨張・光線・急降下）をマークする。
赤線は武器を構える生物の照準軸を示す。

## 電力データ

| 項目 | 値 |
|---|---|
| 装着部位 | ヘルメット |
| 消費電力 | 8 EU/t（オン時） |
| 表示 | 量子ヘルメットのバイザーが必要 |

## 使用手順

1. ユニットを量子ヘルメットに取り付ける。
2. タグ横の意図コードを見る。
3. 射撃前に赤い照準線から外れる。

## 注意

- 近接と遠隔のコード：ATK近接、AIM遠隔武器、DET爆発。
- 特殊攻撃のコード：CAST呪文、BEAMガーディアン、FIREガスト、DIVEヴェックス、AGGROエンダーマン。

## 経路・カウントダウン・順位

- 経路はゲームの移動ルールに従う：氷の上ではより遠くまで滑り、水中では遅くなってゆっくり沈み、ハーフブロックは登り、1 ブロックの段差には跳び乗り（以前はそこで経路が止まった）、端から落ちる。
- 攻撃を受けた直後の生物は後ろへ滑り、その後また歩き出す。着用者を追う生物は目の前で止まる。
- 攻撃予告には攻撃までの時間が付く：AIM は弓の引き・クロスボウの装填、FIRE はガストの溜め、DET はクリーパーの膨張、ATK はその生物から学んだ攻撃の間隔。
- タグの横の #1～#3 は最も危険な敵対生物の順位：敵意の強さ、1 回の攻撃や爆発の大きさ、攻撃できるまでの早さで決まる。
- 12 ブロックより遠く、着用者を脅かしていない生物の経路は 2 tick ごとではなく 4 tick ごとに計算し直す。

## Mk.II

Mk.II ではアクション **時間歪曲**（「量子アクション」を参照）と、下のデータリンクが加わる。

## Mk.II データリンク

Mk.II の予測器には、24 ブロック以内で最も危険な 8 体について、歩くルートの次の数歩をサーバーが送る。その経路は曲がり角に沿って表示され、紫色で LINK の印が付く。

## レシピ

<RecipesFor id="mio_icif:module/item_module_behavior_predictor" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_behavior_predictor_mk2" fallbackText="-" />
