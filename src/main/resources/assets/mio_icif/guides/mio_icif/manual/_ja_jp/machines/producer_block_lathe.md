---
navigation:
  title: "Lathe"
  icon: mio_icif:producer/block_lathe
  parent: machines.md
  position: 39
item_ids:
  - mio_icif:producer/block_lathe
---

# Lathe

<Row>
  <BlockImage id="mio_icif:producer/block_lathe" scale="3" />
</Row>

## 機能

旋盤は隣の運動エネルギー源のKUを使い、旋盤工具で旋削ブランクを削る。
加工物には5つの位置がある。各切削で棒や小さな鉄の粉の副産物が出ることがある。
旋盤工具は加工物より硬い必要がある。各切削は工具の耐久値を使う。
各位置は厚さ1まで削れる。
旋削ブランクのアイコンとツールチップは現在の形状を示す。アイコンの下のバーは残りの削れる材料を示す。

## 部品

一部のレシピは、決まった形状（位置1から5の厚さ）に削った鉄の旋削ブランクを使う。
完成した部品は青緑の輪郭線を持ち、ツールチップに名前が出る。ブランクの上でShiftを押すと、まだ削り出せる部品が出る。

| 部品 | 形状 | 用途 |
|---|---|---|
| Drill Tip | 5-4-3-2-1 | Diamond Drill |
| Water Wheel Shaft | 4-3-4-3-4 | Water Kinetic Generator |
| Piston Rod | 3-2-3-2-3 | Stirling Kinetic Generator |
| Laser Barrel | 4-3-2-2-2 | Mining Laser |
| Crank Handle | 5-5-5-2-2 | Manual Kinetic Generator |
| Rotor Hub | 5-3-3-3-5 | Wind Kinetic Generator |
| Turbine Spindle | 1-3-5-3-1 | Steam Kinetic Generator |

Electric Kinetic Generator以外の運動エネルギー発生機はすべて部品が必要。最初の旋盤はElectric Kinetic Generatorで動かす。

| アイテム | 硬度 | 回数 |
|---|---|---|
| 鉄の旋盤工具 | 3 | 50 |
| ダイヤモンドの旋盤工具 | 4 | 64 |
| 木の旋削ブランク（幅3、棒10%） | 0 | - |
| 鉄の旋削ブランク（幅5、小さな鉄の粉50%） | 2 | - |

## 電力データ

このブロックはEUを使わない。最大10,000 KUを蓄え、どの面からもKUを受け取る。
バッファは毎tick 1%減る。500 KUを超えると稼働状態になる。各切削は1,000 KUを使う。

## 使用手順

1. 旋盤を運動エネルギー発生機の隣に置く。
2. 旋盤工具を工具スロットに入れる。
3. 旋削ブランクを加工物スロットに入れる。
4. GUIで位置をクリックし、その位置を削る。
5. 出力スロットから副産物を取り出す。

## レシピ

<RecipesFor id="mio_icif:producer/block_lathe" fallbackText="-" />
