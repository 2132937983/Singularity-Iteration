---
navigation:
  title: "Steam Turbine Generator"
  icon: mio_icif:generator/block_steam_turbine
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:generator/block_steam_turbine
---

# Steam Turbine Generator

<Row>
  <BlockImage id="mio_icif:generator/block_steam_turbine" scale="3" />
</Row>

## Function

The machine changes steam into EU.
The machine charges a battery in its battery slot and empties steam cells into its tank.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 64 EU/t |
| Energy storage | 32,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Put the machine next to a Steam Reactor, or connect a fluid pipe with steam to the machine.
2. Connect an MV cable to the machine.

## Notes

- The machine uses steam only while its energy storage has room.

## Recipe

<RecipesFor id="mio_icif:generator/block_steam_turbine" fallbackText="-" />
