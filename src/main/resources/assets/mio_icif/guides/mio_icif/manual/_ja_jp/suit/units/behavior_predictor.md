---
navigation:
  title: "行動予測器"
  icon: mio_icif:module/item_module_behavior_predictor
  parent: suit/index.md
  position: 15
item_ids:
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

## レシピ

<RecipesFor id="mio_icif:module/item_module_behavior_predictor" fallbackText="-" />
