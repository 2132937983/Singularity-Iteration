---
navigation:
  title: "LESU Charger"
  icon: mio_icif:wiring/block_lesu_charger
  parent: power.md
  position: 24
item_ids:
  - mio_icif:wiring/block_lesu_charger
---

# LESU Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_lesu_charger" scale="3" />
</Row>

## Function

The LESU Charger is an HV storage block with a charge pad on top.
The pad charges the armor, held items and inventory items of a player on top.
The front face outputs EU. The other five faces receive EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 1,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the LESU Charger with the front face toward you.
2. Connect an HV generator cable to one of the five input faces.
3. Wait until the LESU Charger stores EU.
4. Stand on top of the LESU Charger to charge your equipment.

## Notes

- The pad charges only while the redstone mode allows output.

> **WARNING:** The output is HV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_lesu_charger" fallbackText="-" />
