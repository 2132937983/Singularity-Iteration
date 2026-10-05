---
navigation:
  title: "EESU"
  icon: mio_icif:wiring/block_eesu
  parent: power.md
  position: 7
item_ids:
  - mio_icif:wiring/block_eesu
---

# EESU

<Row>
  <BlockImage id="mio_icif:wiring/block_eesu" scale="3" />
</Row>

## Function

The EESU is an IV storage block. The EESU stores EU and supplies EU to machines.
The front face outputs EU. The other five faces receive EU.
The top slot charges an item. The bottom slot takes EU from a battery.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | IV (8,192 EU) |
| Maximum input | 8,192 EU/t |
| Energy storage | 400,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the EESU with the front face toward you.
2. Connect the generator cable to one of the five input faces.
3. Connect the front face to the machines with IV cable.
4. Open the GUI and select a redstone mode to control the output.

## Notes

- The redstone mode can stop the output or emit a signal at a charge level.

> **WARNING:** The output is IV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_eesu" fallbackText="-" />
