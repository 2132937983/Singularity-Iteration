---
navigation:
  title: "BatBox"
  icon: mio_icif:wiring/block_bat_box
  parent: power.md
  position: 3
item_ids:
  - mio_icif:wiring/block_bat_box
---

# BatBox

<Row>
  <BlockImage id="mio_icif:wiring/block_bat_box" scale="3" />
</Row>

## Function

The BatBox is an LV storage block. The BatBox stores EU and supplies EU to machines.
The front face outputs EU. The other five faces receive EU.
The top slot charges an item. The bottom slot takes EU from a battery.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 40,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the BatBox with the front face toward you.
2. Connect the generator cable to one of the five input faces.
3. Connect the front face to the machines with LV cable.
4. Open the GUI and select a redstone mode to control the output.

## Notes

- The redstone mode can stop the output or emit a signal at a charge level.

## Recipe

<RecipesFor id="mio_icif:wiring/block_bat_box" fallbackText="-" />
