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

The machine is a concentrator: lenses focus the direct sunlight on its cells to make EU. It has the highest daytime output, but reaches its rated output only around noon (850 W/m²).
The machine uses the direct sunlight only: it needs open sky straight above it and makes nothing under cover. Rain keeps about 37 %, a thunderstorm almost nothing; places without rain, such as deserts, have no weather loss.
A liquid loop carries half the heat away: the cells are about 22 °C above the air at 800 W/m². The output falls 0.4 % per °C above 25 °C, even at full output. At noon in a desert it is about 83 % of the rating.
The machine makes no EU at night. For power at night, use energy storage or a photon resonance solar generator.
The screen shows today's record: the sunlight on the panel, a clear day for comparison and the output, with the direct share, the cell temperature and the cooling.
The machine charges items in four slots.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 96 EU/t |
| Output at night | 0 EU/t |
| Full output from | 850 W/m² |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the panel under open sky in direct sun, for example in a desert.
2. Remove all blocks above the panel.
3. Connect a MV cable to the panel.
4. Put up to four batteries into the charge slots.
5. For power at night, connect energy storage to the network.

## Recipe

<RecipesFor id="mio_icif:generator/block_hybrid_solar_panel" fallbackText="-" />
