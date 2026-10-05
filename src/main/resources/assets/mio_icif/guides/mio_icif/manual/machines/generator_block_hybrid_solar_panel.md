---
navigation:
  title: "Hybrid Solar Panel"
  icon: mio_icif:generator/block_hybrid_solar_panel
  parent: generators.md
  position: 16
item_ids:
  - mio_icif:generator/block_hybrid_solar_panel
---

# Hybrid Solar Panel

<Row>
  <BlockImage id="mio_icif:generator/block_hybrid_solar_panel" scale="3" />
</Row>

## Function

The machine makes EU from sunlight in the day and at a lower rate at night.
The machine needs a clear view of the sky above it.
In rain, the machine uses the night rate.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 128 EU/t |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the panel in a dimension with a sky.
2. Remove all opaque blocks above the panel.
3. Connect an MV cable to the panel.
4. Put up to four batteries into the charge slots.

## Recipe

<RecipesFor id="mio_icif:generator/block_hybrid_solar_panel" fallbackText="-" />
