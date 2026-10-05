---
navigation:
  title: "Transformer IV→LuV"
  icon: mio_icif:wiring/transformer_iv_luv
  parent: power.md
  position: 33
item_ids:
  - mio_icif:wiring/transformer_iv_luv
---

# Transformer IV→LuV

<Row>
  <BlockImage id="mio_icif:wiring/transformer_iv_luv" scale="3" />
</Row>

## Function

The transformer converts EU between the IV and LuV voltage tiers.
The front face is the LuV side. The other five faces are the IV side.
Step-down mode is the default. Redstone control mode steps up while a redstone signal is present.

## Power data

This block uses no EU.

## Procedure

1. Place the transformer with the front face toward you.
2. Connect the LuV cable from the source to the front face.
3. Connect the IV machines to one of the other five faces.
4. Open the GUI and select Step down, Step up or Redstone control.
5. Right-click the transformer with a wrench to turn the front face.

## Notes

- In step-up mode, EU enters the IV faces and leaves the front face at LuV.

> **WARNING:** An input above the input voltage tier makes the transformer explode.


## Recipe

<RecipesFor id="mio_icif:wiring/transformer_iv_luv" fallbackText="-" />
