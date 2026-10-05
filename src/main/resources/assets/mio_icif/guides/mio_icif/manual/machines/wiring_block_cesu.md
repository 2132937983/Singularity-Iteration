---
navigation:
  title: "CESU"
  icon: mio_icif:wiring/block_cesu
  parent: power.md
  position: 5
item_ids:
  - mio_icif:wiring/block_cesu
---

# CESU

<Row>
  <BlockImage id="mio_icif:wiring/block_cesu" scale="3" />
</Row>

## Function

The CESU is an MV storage block. The CESU stores EU and supplies EU to machines.
The front face outputs EU. The other five faces receive EU.
The top slot charges an item. The bottom slot takes EU from a battery.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 300,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the CESU with the front face toward you.
2. Connect the generator cable to one of the five input faces.
3. Connect the front face to the machines with MV cable.
4. Open the GUI and select a redstone mode to control the output.

## Notes

- The redstone mode can stop the output or emit a signal at a charge level.

> **WARNING:** The output is MV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_cesu" fallbackText="-" />
