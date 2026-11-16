---
navigation:
  title: "Advanced Stirling Generator"
  icon: mio_icif:generator/block_advanced_stirling_generator
  parent: generators.md
  position: 4
item_ids:
  - mio_icif:generator/block_advanced_stirling_generator
---

# Advanced Stirling Generator

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_stirling_generator" scale="3" />
</Row>

## Function

The machine converts heat (HU) into EU at a higher ratio than the Stirling Generator.
The machine draws heat from each adjacent heat generator whose front face points at it.
The ratio below is the design point: heater 650 °C, cooler 40 °C. The engine's efficiency follows the Carnot efficiency between its heater and its cooler, from 40 % to 110 % of the design ratio. The heater gets hotter the more heat flows through it (650 °C at the rated flow, about 530 °C at a quarter of it), and a cold engine warms up over a few seconds. The cooler is the air of the biome (a desert is worse than a snowy plain); water touching the machine cools it to about 15 °C.
The screen is a plant mimic: heat in, the engine with its heater and cooler temperatures, the flywheel, the generator and the EU out.
Rated heat flow: 150 HU/t.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Energy storage | 1,000,000 EU |
| Conversion | 100 HU → 85 EU |
| Heat input | up to 2,000 HU/t |

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change.

## Procedure

1. Place one or more heat generators next to the machine.
2. Turn the front face of each heat generator to the machine.
3. Connect an MV cable to the machine.

## Recipe

<RecipesFor id="mio_icif:generator/block_advanced_stirling_generator" fallbackText="-" />
