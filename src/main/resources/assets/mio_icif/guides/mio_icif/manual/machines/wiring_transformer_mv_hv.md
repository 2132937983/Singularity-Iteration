---
navigation:
  title: "Transformer MV→HV"
  icon: mio_icif:wiring/transformer_mv_hv
  parent: power.md
  position: 36
item_ids:
  - mio_icif:wiring/transformer_mv_hv
---

# Transformer MV→HV

<Row>
  <BlockImage id="mio_icif:wiring/transformer_mv_hv" scale="3" />
</Row>

## Function

The transformer converts EU between the MV and HV voltage tiers.
The front face is the HV side. The other five faces are the MV side.
Step-down mode is the default. Redstone control mode steps up while a redstone signal is present.

## Power data

This block uses no EU.

## Procedure

1. Place the transformer with the front face toward you.
2. Connect the HV cable from the source to the front face.
3. Connect the MV machines to one of the other five faces.
4. Open the GUI and select Step down, Step up or Redstone control.
5. Right-click the transformer with a wrench to turn the front face.

## Notes

- In step-up mode, EU enters the MV faces and leaves the front face at HV.

> **WARNING:** An input above the input voltage tier makes the transformer explode.


## Recipe

<RecipesFor id="mio_icif:wiring/transformer_mv_hv" fallbackText="-" />
