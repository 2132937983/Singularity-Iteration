---
navigation:
  title: "MFSU"
  icon: mio_icif:wiring/block_mfsu
  parent: power.md
  position: 28
item_ids:
  - mio_icif:wiring/block_mfsu
---

# MFSU

<Row>
  <BlockImage id="mio_icif:wiring/block_mfsu" scale="3" />
</Row>

## Function

The MFSU is an EV storage block. The MFSU stores EU and supplies EU to machines.
The front face outputs EU. The other five faces receive EU.
The top slot charges an item. The bottom slot takes EU from a battery.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input | 2,048 EU/t |
| Energy storage | 40,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the MFSU with the front face toward you.
2. Connect the generator cable to one of the five input faces.
3. Connect the front face to the machines with EV cable.
4. Open the GUI and select a redstone mode to control the output.

## Notes

- The redstone mode can stop the output or emit a signal at a charge level.

> **WARNING:** The output is EV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_mfsu" fallbackText="-" />
