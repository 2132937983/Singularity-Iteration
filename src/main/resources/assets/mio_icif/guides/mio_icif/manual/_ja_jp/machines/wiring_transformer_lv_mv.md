---
navigation:
  title: "Transformer LV→MV"
  icon: mio_icif:wiring/transformer_lv_mv
  parent: power.md
  position: 34
item_ids:
  - mio_icif:wiring/transformer_lv_mv
---

# Transformer LV→MV

<Row>
  <BlockImage id="mio_icif:wiring/transformer_lv_mv" scale="3" />
</Row>

## 機能

変圧器がLVとMVの電圧Tierの間でEUを変換する。
正面がMV側だ。他の5面がLV側だ。
初期モードは降圧モードだ。レッドストーン制御モードは信号がある間に昇圧する。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 変圧器を置き、正面を自分の方に向ける。
2. 電源のMVケーブルを正面に接続する。
3. LVの機械を他の5面のどれかに接続する。
4. GUIを開き、降圧、昇圧、レッドストーン制御のどれかを選ぶ。
5. レンチで変圧器を右クリックし、正面の向きを変える。

## 注意

- 昇圧モードでは、EUがLVの面から入り、MVで正面から出る。

> **警告：** 入力側の電圧Tierを超える入力で変圧器が爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/transformer_lv_mv" fallbackText="-" />
