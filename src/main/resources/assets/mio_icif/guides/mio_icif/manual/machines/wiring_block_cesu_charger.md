---
navigation:
  title: "CESU Charger"
  icon: mio_icif:wiring/block_cesu_charger
  parent: power.md
  position: 6
item_ids:
  - mio_icif:wiring/block_cesu_charger
---

# CESU Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_cesu_charger" scale="3" />
</Row>

## Function

The CESU Charger is an MV storage block with a charge pad on top.
The pad charges the armor, held items and inventory items of a player on top.
The front face outputs EU. The other five faces receive EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 300,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the CESU Charger with the front face toward you.
2. Connect an MV generator cable to one of the five input faces.
3. Wait until the CESU Charger stores EU.
4. Stand on top of the CESU Charger to charge your equipment.

## Notes

- The pad charges only while the redstone mode allows output.

> **WARNING:** The output is MV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_cesu_charger" fallbackText="-" />
