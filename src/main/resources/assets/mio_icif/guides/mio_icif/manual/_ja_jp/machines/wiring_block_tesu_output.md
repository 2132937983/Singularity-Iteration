---
navigation:
  title: "TESU出力ポート"
  icon: mio_icif:wiring/block_tesu_output
  parent: power.md
  position: 19
item_ids:
  - mio_icif:wiring/block_tesu_output
---

# TESU出力ポート

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_output" scale="3" />
</Row>

## 機能

TESU出力ポートは設定した出力でコントローラーの電力を送ります。右クリックで画面を開きます（クリエイティブ発電機と同じ形）：
入力欄に 0～2,147,483,647 EU/t の任意の値（k、M、G 可）を入れて「設定」かEnter。10 個のボタンで IV～MAX 各電圧の満パケット、
x10・/10 で増減、「オン/オフ」で出力を止められます。画面には出力、電圧、前の tick に送った量、TESU の蓄電量が出ます
（完成した TESU につながっていないと赤字）。

電圧は出力に合わせて自動で決まります：その出力が入る一番低い電圧（最低 IV）なので、小さな出力が不要に高い電圧になりません。
パケットを 2 個保持し、毎 tick コントローラーから補充して 1 tick に 1 パケット（設定した出力）送ります。TESU 底部リングの
ポート位置に置くと働きます。古いセーブのポートは元の電圧の満パケットのまま出力します。

## 電力データ

| 電圧 | 満パケットの EU（= 最大 EU/t） |
|---|---|
| IV | 8,192 |
| LuV | 32,768 |
| ZPMV | 131,072 |
| UV | 524,288 |
| UHV | 2,097,152 |
| UEV | 8,388,608 |
| UIV | 33,554,432 |
| UXV | 134,217,728 |
| OpV | 536,870,912 |
| MAX | 2,147,483,647 |

## レシピ

<RecipesFor id="mio_icif:wiring/block_tesu_output" fallbackText="-" />
