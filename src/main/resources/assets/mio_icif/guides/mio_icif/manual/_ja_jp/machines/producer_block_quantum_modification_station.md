---
navigation:
  title: "量子改造ステーション"
  icon: mio_icif:producer/block_quantum_modification_station
  parent: suit/index.md
  position: 0
item_ids:
  - mio_icif:producer/block_quantum_modification_station
---

# 量子改造ステーション

<Row>
  <BlockImage id="mio_icif:producer/block_quantum_modification_station" scale="3" />
</Row>

## 機能

量子改造ステーションは量子スーツの部位にアップグレードユニットを取り付ける。取り付けのたびにEUを使う。
使えるユニット：震探鉱石スキャナー、電網テレメトリセンサー、生体スキャナー、弾道計算機、爆発警告タイマー。
ほかのユニット：行動予測器、戦術3Dホロマップ、脅威センサー、量子偏向器。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 40,000 EU |
| 稼働時消費 | 64 EU/t |
| 1回の処理時間 | 100 tick (5 s) |
| 1回あたりの電力 | 6,400 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. HVケーブルをステーションに接続する。
2. 上のスロットに量子スーツの部位を入れる。
3. 下のスロットにアップグレードユニットを入れる。
4. ステーションが取り付けを終えるまで待つ。
5. 取り付け済みユニットの横の×を押し、ユニットを外す。
6. 装備特性管理でユニットをオンにする。

## 注意

- ヘルメットはユニット4個、チェストプレートは3個、レギンスとブーツは2個ずつ付く。各ユニットは決まった部位だけに付く。
- センサーユニットは量子ヘルメットのバイザーでだけデータを表示する。オンのユニットは部位のEUを使う。
- ユニットを外す時、EUは使わず、ユニットは戻る。

## レシピ

<RecipesFor id="mio_icif:producer/block_quantum_modification_station" fallbackText="-" />
