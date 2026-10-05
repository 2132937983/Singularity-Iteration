---
navigation:
  title: "Transformer HV→EV"
  icon: mio_icif:wiring/transformer_hv_ev
  parent: power.md
  position: 32
item_ids:
  - mio_icif:wiring/transformer_hv_ev
---

# Transformer HV→EV

<Row>
  <BlockImage id="mio_icif:wiring/transformer_hv_ev" scale="3" />
</Row>

## Function

The transformer converts EU between the HV and EV voltage tiers.
The front face is the EV side. The other five faces are the HV side.
Step-down mode is the default. Redstone control mode steps up while a redstone signal is present.

## Power data

This block uses no EU.

## Procedure

1. Place the transformer with the front face toward you.
2. Connect the EV cable from the source to the front face.
3. Connect the HV machines to one of the other five faces.
4. Open the GUI and select Step down, Step up or Redstone control.
5. Right-click the transformer with a wrench to turn the front face.

## Notes

- In step-up mode, EU enters the HV faces and leaves the front face at EV.

> **WARNING:** An input above the input voltage tier makes the transformer explode.


## Recipe

<RecipesFor id="mio_icif:wiring/transformer_hv_ev" fallbackText="-" />
