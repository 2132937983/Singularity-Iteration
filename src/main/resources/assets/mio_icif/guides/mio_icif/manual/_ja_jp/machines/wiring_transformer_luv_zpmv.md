---
navigation:
  title: "Transformer LuV→ZPMV"
  icon: mio_icif:wiring/transformer_luv_zpmv
  parent: power.md
  position: 35
item_ids:
  - mio_icif:wiring/transformer_luv_zpmv
---

# Transformer LuV→ZPMV

<Row>
  <BlockImage id="mio_icif:wiring/transformer_luv_zpmv" scale="3" />
</Row>

## 機能

変圧器がLuVとZPMVの電圧Tierの間でEUを変換する。
正面がZPMV側だ。他の5面がLuV側だ。
初期モードは降圧モードだ。レッドストーン制御モードは信号がある間に昇圧する。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 変圧器を置き、正面を自分の方に向ける。
2. 電源のZPMVケーブルを正面に接続する。
3. LuVの機械を他の5面のどれかに接続する。
4. GUIを開き、降圧、昇圧、レッドストーン制御のどれかを選ぶ。
5. レンチで変圧器を右クリックし、正面の向きを変える。

## 注意

- 昇圧モードでは、EUがLuVの面から入り、ZPMVで正面から出る。

> **警告：** 入力側の電圧Tierを超える入力で変圧器が爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/transformer_luv_zpmv" fallbackText="-" />
