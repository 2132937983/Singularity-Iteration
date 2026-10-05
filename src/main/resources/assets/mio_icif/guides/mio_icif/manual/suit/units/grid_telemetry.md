---
navigation:
  title: "Grid Telemetry Sensor"
  icon: mio_icif:module/item_module_grid_telemetry
  parent: suit/index.md
  position: 11
item_ids:
  - mio_icif:module/item_module_grid_telemetry
---

# Grid Telemetry Sensor

<ItemImage id="mio_icif:module/item_module_grid_telemetry" scale="3" />

## Function

The unit reads the EU grid of the block in view and of the chunk of the wearer.
For the block in view, the panel shows the voltage tier, the packet size, the EU flow and the cable rating.
For the chunk, the panel shows generators, consumers, storage blocks, cables, stored EU and overvoltage risks.

## Power data

| Item | Value |
|---|---|
| Fits | Leggings / Helmet |
| Power | 4 EU/t while on |
| Display | Needs a quantum helmet visor |

## Procedure

1. Install the unit into the quantum leggings or helmet.
2. Look at a machine or a cable within 24 blocks.
3. Read the LOS lines of the telemetry panel.
4. Compare the packet size with the cable rating.
5. Replace the cable when the panel shows OVER.

## Notes

- The OV counter shows machines in the chunk that receive packets above their voltage tier.

## Recipe

<RecipesFor id="mio_icif:module/item_module_grid_telemetry" fallbackText="-" />
