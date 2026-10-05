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

## Function

The transformer converts EU between the LV and MV voltage tiers.
The front face is the MV side. The other five faces are the LV side.
Step-down mode is the default. Redstone control mode steps up while a redstone signal is present.

## Power data

This block uses no EU.

## Procedure

1. Place the transformer with the front face toward you.
2. Connect the MV cable from the source to the front face.
3. Connect the LV machines to one of the other five faces.
4. Open the GUI and select Step down, Step up or Redstone control.
5. Right-click the transformer with a wrench to turn the front face.

## Notes

- In step-up mode, EU enters the LV faces and leaves the front face at MV.

> **WARNING:** An input above the input voltage tier makes the transformer explode.


## Recipe

<RecipesFor id="mio_icif:wiring/transformer_lv_mv" fallbackText="-" />
