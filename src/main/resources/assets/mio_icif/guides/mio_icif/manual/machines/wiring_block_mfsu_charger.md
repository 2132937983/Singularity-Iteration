---
navigation:
  title: "MFSU Charger"
  icon: mio_icif:wiring/block_mfsu_charger
  parent: power.md
  position: 29
item_ids:
  - mio_icif:wiring/block_mfsu_charger
---

# MFSU Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_mfsu_charger" scale="3" />
</Row>

## Function

The MFSU Charger is an EV storage block with a charge pad on top.
The pad charges the armor, held items and inventory items of a player on top.
The front face outputs EU. The other five faces receive EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input | 2,048 EU/t |
| Energy storage | 40,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the MFSU Charger with the front face toward you.
2. Connect an EV generator cable to one of the five input faces.
3. Wait until the MFSU Charger stores EU.
4. Stand on top of the MFSU Charger to charge your equipment.

## Notes

- The pad charges only while the redstone mode allows output.

> **WARNING:** The output is EV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_mfsu_charger" fallbackText="-" />
