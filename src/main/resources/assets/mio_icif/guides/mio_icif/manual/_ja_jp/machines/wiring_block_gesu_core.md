---
navigation:
  title: "GESU Core"
  icon: mio_icif:wiring/block_gesu_core
  parent: power.md
  position: 13
item_ids:
  - mio_icif:wiring/block_gesu_core
---

# GESU Core

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_core" scale="3" />
</Row>

## 機能

GESUコアはGESUマルチブロックの中心で、EUを蓄える。
コアの6面すべてにGESUの入力または出力モジュールが接すると、構造が完成する。
入力モジュールが入力速度を決める。出力モジュールが出力速度を決める。

## 電力データ

| 項目 | 値 |
|---|---|
| 蓄電容量 | 2,147,483,647 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. GESUコアを置く。
2. コアの1面に1個以上のGESU入力モジュールを置く。
3. コアの残りの面にGESU出力モジュールを置く。
4. 発電機のケーブルを入力モジュールに接続する。
5. 機械のケーブルを出力モジュールに接続する。
6. コアのGUIを開き、モジュール数を確認する。

## 注意

- 構造はコアと6個のモジュールからなる7ブロックの十字形だ。
- モジュールを1個外すと、モジュールを戻すまでGESUが停止する。
- GUIには、アイテムを充電するスロットと、バッテリーからEUを取り出すスロットがある。

## レシピ

<RecipesFor id="mio_icif:wiring/block_gesu_core" fallbackText="-" />
