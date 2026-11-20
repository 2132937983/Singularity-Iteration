---
navigation:
  title: "Quantum Solar Panel"
  icon: mio_icif:generator/block_quantum_solar_panel
  parent: generators.md
  position: 24
item_ids:
  - mio_icif:generator/block_quantum_solar_panel
---

# Quantum Solar Panel

<Row>
  <BlockImage id="mio_icif:generator/block_quantum_solar_panel" scale="3" />
</Row>

## Function

The machine is a concentrator: lenses focus the direct sunlight on its cells to make EU. It has the highest daytime output, but reaches its rated output only around noon (850 W/m²).
The machine uses the direct sunlight only: it needs open sky straight above it and makes nothing under cover. Rain keeps about 37 %, a thunderstorm almost nothing; places without rain, such as deserts, have no weather loss.
The cooling holds the cells at 25 °C, so the temperature does not change the output.
The machine makes no EU at night. For power at night, use energy storage or a photon resonance solar generator.
The screen shows today's record: the sunlight on the panel, a clear day for comparison and the output, with the direct share, the cell temperature and the cooling.
The machine charges items in four slots.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | IV (8,192 EU) |
| Output | 7,168 EU/t |
| Output at night | 0 EU/t |
| Full output from | 850 W/m² |
| Energy storage | 10,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the panel under open sky in direct sun, for example in a desert.
2. Remove all blocks above the panel.
3. Connect a IV cable to the panel.
4. Put up to four batteries into the charge slots.
5. For power at night, connect energy storage to the network.

## Recipe

<RecipesFor id="mio_icif:generator/block_quantum_solar_panel" fallbackText="-" />
