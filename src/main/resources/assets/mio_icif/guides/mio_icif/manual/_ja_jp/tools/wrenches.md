---
navigation:
  title: "レンチとツールボックス"
  icon: mio_icif:item_tool_wrench
  parent: tools.md
  position: 3
item_ids:
  - mio_icif:item_tool_wrench
  - mio_icif:item_tool_wrench_elc
  - mio_icif:normal/item_toolbox
  - mio_icif:normal/item_toolbox_x
  - mio_icif:build/item_obscurator
  - mio_icif:build/item_cf_sprayer
  - mio_icif:build/item_painter
---

# レンチとツールボックス

## レンチ

<ItemLink id="mio_icif:item_tool_wrench" />（耐久値 120）と <ItemLink id="mio_icif:item_tool_wrench_elc" />（50,000 EU）は、タグ `mio_icif:wrenchable` のブロックに使える：機械、ケーブル、パイプ、ストレージボックス、マルチブロックの外装とポート。

| 操作 | 結果 |
|---|---|
| 機械の正面、または向きのないブロックを右クリック | 取り外す。アイテムは設定と蓄電を保持する |
| ほかの面を右クリック | 正面をその面へ向ける |
| スニーク右クリック | 正面をクリックした面の反対側へ向ける |
| ケーブルやパイプの腕を右クリック | その面をふさぐ。中心をクリックすると解除する |

- 取り外しで失うものはない。ストレージボックスは右クリックでそのまま取り外せる。
- 電動レンチ：取り外し 100 EU、向きの変更 5 EU、ケーブルとパイプは 2 EU。奇点管理パネルで「取り外し時に中身を保持」と「正面クリックで取り外し」をオン・オフできる。電力がないとアクションバーに表示が出る。クリエイティブでは電力を使わない。
- ほかの MOD のレンチ：スニーク右クリックでこの MOD のブロックを取り外す。
- アドオン MOD のブロックは `IWrenchable` で向きの変更や取り外しを拒否し、ドロップを決められる。

## ツールボックス

<ItemLink id="mio_icif:normal/item_toolbox" />（9 スロット）と <ItemLink id="mio_icif:normal/item_toolbox_x" />（27 スロット）は右クリックで開き、ツールだけを入れられる。出し入れのたびにすぐ保存される。開いている間、ツールボックスは手に固定される。

## 建築ツール

- <ItemLink id="mio_icif:build/item_obscurator" />（100,000 EU）：スニーク右クリックでブロックの見た目をスキャンする（5,000 EU）。この MOD の機械やケーブルカバーを右クリックしてその見た目を適用する（1,000 EU）。
- <ItemLink id="mio_icif:build/item_cf_sprayer" />：建築フォームを入れる。右クリックで空いた場所にフォームを吹き付け、スニーク右クリックでモードを切り替える。保護区域の権限はブロックごとに確認する。
- <ItemLink id="mio_icif:build/item_painter" />：染色した刷毛で羊毛、テラコッタ、コンクリート、ガラスなどを右クリックするとその色になる。建築フォームは同じ色のコンクリートになる。
