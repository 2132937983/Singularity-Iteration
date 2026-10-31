---
navigation:
  title: "Fluid Heat Generator"
  icon: mio_icif:hugenerator/block_fluid_heat_generator
  parent: generators.md
  position: 10
item_ids:
  - mio_icif:hugenerator/block_fluid_heat_generator
---

# Fluid Heat Generator

<Row>
  <BlockImage id="mio_icif:hugenerator/block_fluid_heat_generator" scale="3" />
</Row>

## Function

The machine burns semifluid fuel and makes heat (HU).
The machine accepts biogas, biomass, crude oil and diesel oil.
The machine gives heat only through its front face.

## Power data

| Item | Value |
|---|---|
| Fuel: Biogas | 0.5 mB/t → 32 HU/t (64 HU/mB) |
| Fuel: Biomass | 1 mB/t → 16 HU/t (16 HU/mB) |
| Fuel: Crude Oil | 0.5 mB/t → 32 HU/t (64 HU/mB) |
| Fuel: Diesel Oil | 0.25 mB/t → 768 HU/t (3,072 HU/mB) |
| Fuel tank | 10,000 mB |

This block uses no EU.

## Procedure

1. Place the machine with its front face against the heat consumer.
2. Put a fuel bucket or a fuel cell into the input slot.
3. Remove the empty containers from the output slot.

## Recipe

<RecipesFor id="mio_icif:hugenerator/block_fluid_heat_generator" fallbackText="-" />
