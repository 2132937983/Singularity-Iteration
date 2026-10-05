---
navigation:
  title: "LESU"
  icon: mio_icif:wiring/block_lesu
  parent: power.md
  position: 23
item_ids:
  - mio_icif:wiring/block_lesu
---

# LESU

<Row>
  <BlockImage id="mio_icif:wiring/block_lesu" scale="3" />
</Row>

## Function

The LESU is an HV storage block. The LESU stores EU and supplies EU to machines.
The front face outputs EU. The other five faces receive EU.
The top slot charges an item. The bottom slot takes EU from a battery.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 1,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the LESU with the front face toward you.
2. Connect the generator cable to one of the five input faces.
3. Connect the front face to the machines with HV cable.
4. Open the GUI and select a redstone mode to control the output.

## Notes

- The redstone mode can stop the output or emit a signal at a charge level.

> **WARNING:** The output is HV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_lesu" fallbackText="-" />
