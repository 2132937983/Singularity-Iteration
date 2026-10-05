---
navigation:
  title: "MFE Charger"
  icon: mio_icif:wiring/block_mfe_charger
  parent: power.md
  position: 27
item_ids:
  - mio_icif:wiring/block_mfe_charger
---

# MFE Charger

<Row>
  <BlockImage id="mio_icif:wiring/block_mfe_charger" scale="3" />
</Row>

## Function

The MFE Charger is an HV storage block with a charge pad on top.
The pad charges the armor, held items and inventory items of a player on top.
The front face outputs EU. The other five faces receive EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 4,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the MFE Charger with the front face toward you.
2. Connect an HV generator cable to one of the five input faces.
3. Wait until the MFE Charger stores EU.
4. Stand on top of the MFE Charger to charge your equipment.

## Notes

- The pad charges only while the redstone mode allows output.

> **WARNING:** The output is HV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_mfe_charger" fallbackText="-" />
