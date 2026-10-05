---
navigation:
  title: "EESU Charger"
  icon: mio_icif:wiring/block_eesu_charger
  parent: power.md
  position: 8
item_ids:
  - mio_icif:wiring/block_eesu_charger
---

# EESU Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_eesu_charger" scale="3" />
</Row>

## Function

The EESU Charger is an IV storage block with a charge pad on top.
The pad charges the armor, held items and inventory items of a player on top.
The front face outputs EU. The other five faces receive EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | IV (8,192 EU) |
| Maximum input | 8,192 EU/t |
| Energy storage | 400,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the EESU Charger with the front face toward you.
2. Connect an IV generator cable to one of the five input faces.
3. Wait until the EESU Charger stores EU.
4. Stand on top of the EESU Charger to charge your equipment.

## Notes

- The pad charges only while the redstone mode allows output.

> **WARNING:** The output is IV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_eesu_charger" fallbackText="-" />
