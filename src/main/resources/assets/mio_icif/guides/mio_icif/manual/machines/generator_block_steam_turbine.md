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
The steam expands through the turbine from its inlet pressure (Steam: 10 bar, saturated; Superheated Steam: 40 bar, 400 °C) to the back pressure. Vented into the open air the back pressure is 1 bar. With a working Condenser beside the turbine the exhaust goes into the Condenser and the back pressure falls to 0.1 bar: the same steam gives more work (Steam 0.8 EU/mB instead of 0.5, Superheated Steam 1.3 instead of 1.0).
The screen is a plant mimic: inlet, turbine, generator, exhaust to the Condenser or the vent, with the expansion readings.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 64 EU/t |
| Energy storage | 32,000 EU |
| Steam use | 128 mB/t → 64 EU/t (2 mB per EU) |
| Superheated Steam use | 64 mB/t → 64 EU/t (1 mB per EU) |
| With a Condenser | Steam 80 mB/t, Superheated Steam about 49 mB/t → 64 EU/t |

The game measured these values from the block entity of this version.

## Procedure

1. Put the machine next to a Steam Reactor, or connect a fluid pipe with steam to the machine.
2. Connect an MV cable to the machine.

## Notes

- The machine uses steam only while its energy storage has room.

## Recipe

<RecipesFor id="mio_icif:generator/block_steam_turbine" fallbackText="-" />
