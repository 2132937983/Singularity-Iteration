---
navigation:
  title: "BatBox Charger"
  icon: mio_icif:wiring/block_batbox_charger
  parent: power.md
  position: 4
item_ids:
  - mio_icif:wiring/block_batbox_charger
---

# BatBox Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_batbox_charger" scale="3" />
</Row>

## Function

The BatBox Charger is an LV storage block with a charge pad on top.
The pad charges the armor, held items and inventory items of a player on top.
The front face outputs EU. The other five faces receive EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 40,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the BatBox Charger with the front face toward you.
2. Connect an LV generator cable to one of the five input faces.
3. Wait until the BatBox Charger stores EU.
4. Stand on top of the BatBox Charger to charge your equipment.

## Notes

- The pad charges only while the redstone mode allows output.

## Recipe

<RecipesFor id="mio_icif:wiring/block_batbox_charger" fallbackText="-" />
