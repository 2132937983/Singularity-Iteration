---
navigation:
  title: "Steam Reactor"
  icon: mio_icif:generator/block_steam_reactor
  parent: generators.md
  position: 30
item_ids:
  - mio_icif:generator/block_steam_reactor
---

# Steam Reactor

<Row>
  <BlockImage id="mio_icif:generator/block_steam_reactor" scale="3" />
</Row>

## Function

The machine burns nuclear fuel rods and makes heat, but no EU.
Steam Vents in the machine change their heat into steam. Hotter vents make more steam.
Each adjacent Steam Reactor Chamber adds one column of component slots. A Reactor Chamber does not connect.

## Power data

This block uses no EU.

## Procedure

1. Place Steam Reactor Chambers against the faces of the reactor.
2. Put fuel rods and Steam Vents into the slots.
3. Pump water or distilled water into the reactor or into a chamber.
4. Put a Steam Turbine Generator or a fluid pipe next to the reactor.
5. Apply a redstone signal to the reactor to start it.

## Heat stages, radiation and accident audit

The heat stages, the radiation shielding and the accident audit are the same as those of the [Nuclear Reactor Generator](generator_block_nuclear_reactor_generator.md).

## Notes

- The reactor pushes steam into adjacent fluid machines and pipes. A full steam tank stops the steam.

> **WARNING:** Without water or steam room, the heat stays in the Steam Vents. The vents and the hull get hot.


> **WARNING:** The reactor explodes when the hull heat reaches its limit.


## Structure

Drag the view to turn it. The list gives the blocks and their rules. JEI shows the same plan in the category "Multiblock Structures"; the category "Reactor Layouts" shows where the Steam Vents go.

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:steam_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:steam_reactor" />

## Recipe

<RecipesFor id="mio_icif:generator/block_steam_reactor" fallbackText="-" />
