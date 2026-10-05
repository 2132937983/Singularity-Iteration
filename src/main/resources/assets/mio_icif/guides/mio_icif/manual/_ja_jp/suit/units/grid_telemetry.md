---
navigation:
  title: "電網テレメトリセンサー"
  icon: mio_icif:module/item_module_grid_telemetry
  parent: suit/index.md
  position: 11
item_ids:
  - mio_icif:module/item_module_grid_telemetry
---

# 電網テレメトリセンサー

<ItemImage id="mio_icif:module/item_module_grid_telemetry" scale="3" />

## 機能

このユニットは視線先のブロックと着用者のチャンクのEU電網を読む。
視線先について、パネルは電圧Tier・パケット量・EU流量・ケーブル定格を表示する。
チャンクについて、パネルは発電機・消費機械・蓄電ブロック・ケーブル・蓄電量・過電圧リスクを表示する。

## 電力データ

| 項目 | 値 |
|---|---|
| 装着部位 | レギンス / ヘルメット |
| 消費電力 | 4 EU/t（オン時） |
| 表示 | 量子ヘルメットのバイザーが必要 |

## 使用手順

1. ユニットを量子レギンスかヘルメットに取り付ける。
2. 24ブロック内の機械かケーブルを見る。
3. テレメトリパネルのLOS行を読む。
4. パケット量とケーブル定格を比べる。
5. パネルにOVERが出たらケーブルを交換する。

## 注意

- OVカウンターは、電圧Tierを超えるパケットを受けたチャンク内の機械数を示す。

## レシピ

<RecipesFor id="mio_icif:module/item_module_grid_telemetry" fallbackText="-" />
