---
navigation:
  title: "震探鉱石スキャナー"
  icon: mio_icif:module/item_module_ore_scanner
  parent: suit/index.md
  position: 10
item_ids:
  - mio_icif:module/item_module_ore_scanner
---

# 震探鉱石スキャナー

<ItemImage id="mio_icif:module/item_module_ore_scanner" scale="3" />

## 機能

ユニットをオンにすると、半径16ブロックの地層にソナーパルスを送る。
パルスは光の輪として着用者から広がる。バイザーに短いグリッチ効果が走る。
エコーが届いた鉱石ブロックが光る。鉱石は鉱石色の光るセルで表示され、岩越しにも見える。
パルスの後、小さなタグが種類ごとに最も近い鉱石と距離を示す。

## 電力データ

| 項目 | 値 |
|---|---|
| 装着部位 | ブーツ / レギンス |
| 消費電力 | 24 EU/t（オン時） |
| 表示 | 量子ヘルメットのバイザーが必要 |

## 使用手順

1. ユニットを量子ブーツか量子レギンスに取り付ける。
2. 量子ヘルメットを着用する。
3. 採掘場所まで移動する。
4. ユニットをオンにしてエコーを待つ。
5. 光るセルに向かって掘る。

## 注意

- 最初のパルスの後、ユニットは50 tickごとにパルス効果なしで再スキャンする。
- ユニットをオフにしてから再びオンにすると、新しいパルスを送る。

## レシピ

<RecipesFor id="mio_icif:module/item_module_ore_scanner" fallbackText="-" />
