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

## Function

The transformer converts EU between the LuV and ZPMV voltage tiers.
The front face is the ZPMV side. The other five faces are the LuV side.
Step-down mode is the default. Redstone control mode steps up while a redstone signal is present.

## Power data

This block uses no EU.

## Procedure

1. Place the transformer with the front face toward you.
2. Connect the ZPMV cable from the source to the front face.
3. Connect the LuV machines to one of the other five faces.
4. Open the GUI and select Step down, Step up or Redstone control.
5. Right-click the transformer with a wrench to turn the front face.

## Notes

- In step-up mode, EU enters the LuV faces and leaves the front face at ZPMV.

> **WARNING:** An input above the input voltage tier makes the transformer explode.


## Recipe

<RecipesFor id="mio_icif:wiring/transformer_luv_zpmv" fallbackText="-" />
