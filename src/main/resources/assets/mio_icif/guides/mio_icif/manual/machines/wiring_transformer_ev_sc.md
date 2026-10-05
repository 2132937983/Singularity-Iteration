---
navigation:
  title: "Transformer EV→SC"
  icon: mio_icif:wiring/transformer_ev_sc
  parent: power.md
  position: 31
item_ids:
  - mio_icif:wiring/transformer_ev_sc
---

# Transformer EV→SC

<Row>
  <BlockImage id="mio_icif:wiring/transformer_ev_sc" scale="3" />
</Row>

## Function

The transformer converts EU between the EV and IV voltage tiers.
The front face is the IV side. The other five faces are the EV side.
Step-down mode is the default. Redstone control mode steps up while a redstone signal is present.

## Power data

This block uses no EU.

## Procedure

1. Place the transformer with the front face toward you.
2. Connect the IV cable from the source to the front face.
3. Connect the EV machines to one of the other five faces.
4. Open the GUI and select Step down, Step up or Redstone control.
5. Right-click the transformer with a wrench to turn the front face.

## Notes

- In step-up mode, EU enters the EV faces and leaves the front face at IV.

> **WARNING:** An input above the input voltage tier makes the transformer explode.


## Recipe

<RecipesFor id="mio_icif:wiring/transformer_ev_sc" fallbackText="-" />
