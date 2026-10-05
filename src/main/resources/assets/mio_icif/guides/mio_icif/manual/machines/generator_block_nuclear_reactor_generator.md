---
navigation:
  title: "Nuclear Reactor Generator"
  icon: mio_icif:generator/block_nuclear_reactor_generator
  parent: generators.md
  position: 21
item_ids:
  - mio_icif:generator/block_nuclear_reactor_generator
---

# Nuclear Reactor Generator

<Row>
  <BlockImage id="mio_icif:generator/block_nuclear_reactor_generator" scale="3" />
</Row>

## Function

The machine burns nuclear fuel rods and makes EU and heat.
Each adjacent Reactor Chamber adds one column of component slots.
With a Reactor Pressure Vessel structure, the machine changes to fluid mode and heats coolant.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | IV (8,192 EU) |
| Output | 8,192 EU/t |
| Energy storage | 1,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place Reactor Chambers against the faces of the reactor.
2. Put fuel rods and cooling components into the slots.
3. Connect a cable to the reactor.
4. Apply a redstone signal to the reactor to start it.

## Notes


> **WARNING:** The reactor explodes when the hull heat reaches its limit.


> **WARNING:** At high hull heat, the reactor sets blocks on fire and hurts nearby mobs.


## Recipe

<RecipesFor id="mio_icif:generator/block_nuclear_reactor_generator" fallbackText="-" />
