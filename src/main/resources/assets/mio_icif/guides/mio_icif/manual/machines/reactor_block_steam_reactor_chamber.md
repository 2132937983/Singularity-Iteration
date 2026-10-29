---
navigation:
  title: "Steam Reactor Chamber"
  icon: mio_icif:reactor/block_steam_reactor_chamber
  parent: heavy.md
  position: 15
item_ids:
  - mio_icif:reactor/block_steam_reactor_chamber
---

# Steam Reactor Chamber

<Row>
  <BlockImage id="mio_icif:reactor/block_steam_reactor_chamber" scale="3" />
</Row>

## Function

The Steam Reactor Chamber adds one column of component slots to an adjacent Steam Reactor.
The chamber shares the reactor inventory, heat, water and steam. Pipes fill and drain the reactor through the chamber.

## Power data

This block uses no EU.

## Procedure

1. Place the chamber against one face of a Steam Reactor.
2. Connect a water pipe to the chamber to fill the reactor.

## Notes

- The chamber does not connect to a Nuclear Reactor Generator. A Reactor Chamber does not connect to a Steam Reactor.
- The chamber must touch only one reactor.

## Recipe

<RecipesFor id="mio_icif:reactor/block_steam_reactor_chamber" fallbackText="-" />
