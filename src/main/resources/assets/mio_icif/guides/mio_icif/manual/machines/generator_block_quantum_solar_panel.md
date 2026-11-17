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

The machine makes EU from the sunlight on it. The sun is weak in the morning and evening and strongest at noon; the machine reaches its rated output at 700 W/m² and stays there above it.
Rain keeps about 70 % of the light and a thunderstorm about 37 %. A hot panel makes less (−0.4 % per °C above 25 °C), so deserts are sunny but hot.
The machine needs open sky straight above it; under cover it gets only the diffuse light, in the dark nothing.
The screen shows today's record: the sunlight on the panel, a clear day for comparison, and the output.
At night, while it sees the sky, its night core keeps a lower output.
The machine charges items in four slots.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | IV (8,192 EU) |
| Output | 8,192 EU/t |
| Energy storage | 10,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the panel in a dimension with a sky.
2. Remove all opaque blocks above the panel.
3. Connect an IV cable to the panel.
4. Put up to four batteries into the charge slots.

## Recipe

<RecipesFor id="mio_icif:generator/block_quantum_solar_panel" fallbackText="-" />
