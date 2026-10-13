---
navigation:
  title: "汎用スーツ改造台"
  icon: mio_icif:producer/block_quantum_modification_station
  parent: suit/index.md
  position: 0
item_ids:
  - mio_icif:producer/block_quantum_modification_station
---

# 汎用スーツ改造台

<Row>
  <BlockImage id="mio_icif:producer/block_quantum_modification_station" scale="3" />
</Row>

## 機能

量子スーツとナノスーツの部位に強化ユニットを取り付ける。取り付けのたびにEUを使う。
量子部位は個数で数える。ヘルメット4、チェストプレート3、レギンス2、ブーツ2。
ナノ部位はナノ格子を持ち、格子ポイント（LP）で数える。ヘルメット4、チェストプレート6、レギンス4、ブーツ4。
ナノユニットは1〜3 LPを使う。ナノ格子高密度化材は部位に2 LPを足す。1部位に2回まで。
一部のユニットは片方のスーツ専用。ユニットのツールチップに種別とLPが出る。

## 電力データ

| 項目 | 値 |
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 40,000 EU |
| 稼働時消費 | 64 EU/t |
| 1回の処理時間 | 100 tick (5 s) |
| 1回あたりの電力 | 6,400 EU |

## 使用手順

1. HVケーブルを改造台に接続する。
2. 上のスロットに量子またはナノスーツの部位を入れる。
3. 下のスロットにユニットか格子高密度化材を入れる。
4. 改造台が取り付けを終えるまで待つ。
5. 取り付け済みユニットの横の×を押し、ユニットを外す。
6. 奇点管理パネルでユニットをオンにする。

## 注意

- ナノ部位では格子バーを表示する。1マスが1 LPで、使っているユニットの色になる。下のスロットのユニットが入る位置は点滅する。
- センサーユニットはスーツのバイザー越しにだけデータを表示する。
- ユニットを外す時、EUは使わず、ユニットは戻る。高密度化材は部位に残る。

## レシピ

<RecipesFor id="mio_icif:producer/block_quantum_modification_station" fallbackText="-" />
