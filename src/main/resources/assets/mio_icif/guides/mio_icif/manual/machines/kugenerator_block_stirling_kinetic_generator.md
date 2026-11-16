---
navigation:
  title: "Stirling Kinetic Generator"
  icon: mio_icif:kugenerator/block_stirling_kinetic_generator
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:kugenerator/block_stirling_kinetic_generator
---

# Stirling Kinetic Generator

<Row>
  <BlockImage id="mio_icif:kugenerator/block_stirling_kinetic_generator" scale="3" />
</Row>

## Function

The machine converts heat (HU) and water into kinetic energy (KU) and hot water.
The machine takes heat through all faces except the front face.
The machine sends KU only through its front face.
The water is the cooling water: the cooler stays near 15 °C, and the ratio below holds at the design point (heater 650 °C, rated flow 500 HU/t); a lightly loaded or cold engine makes less per HU (see the Stirling Generator).
The KU go into a flywheel (20,000 KU). The faster it turns, the more it gives each tick (up to 2,000 KU/t when full), so the shaft speeds up until it gives what the engine makes, and slows down when the load falls. No KU are lost; a full flywheel (overspeed) stops the engine taking heat.
The screen is a plant mimic: heat in, the water-cooled engine, the water and hot water tanks, the flywheel with its speed, and the KU out of the front.

## Power data

| Item | Value |
|---|---|
| Conversion | 4 HU + 1 mB Water → 12 KU + 1 mB Hot Water |
| Flywheel | 20,000 KU, gives up to 2,000 KU/t when full |
| Heat buffer | 1,000 HU |

This block uses no EU.

## Procedure

1. Place the machine with its front face against the KU consumer.
2. Place a heat generator against another face of the machine.
3. Put water buckets or water cells into the water input slot.
4. Put empty containers into the hot water slot to collect the hot water.

## Notes

- The machine stops when the water tank is empty or the hot water tank is full.

## Recipe

<RecipesFor id="mio_icif:kugenerator/block_stirling_kinetic_generator" fallbackText="-" />
