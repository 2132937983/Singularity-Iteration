---
navigation:
  title: "Reactor Chamber"
  icon: mio_icif:reactor/block_reactor_chamber
  parent: heavy.md
  position: 11
item_ids:
  - mio_icif:reactor/block_reactor_chamber
---

# Reactor Chamber

<Row>
  <BlockImage id="mio_icif:reactor/block_reactor_chamber" scale="3" />
</Row>

## Function

The Reactor Chamber adds one column of component slots to an adjacent Nuclear Reactor Generator.
The chamber shares the reactor inventory, EU output and heat. A redstone signal on the chamber also enables the reactor.

## Power data

This block uses no EU.

## Procedure

1. Place the Nuclear Reactor Generator.
2. Place up to six chambers against the faces of the reactor.
3. Right-click a chamber to open the reactor GUI.
4. Connect a cable to a chamber to take the EU output.

## Notes

- A chamber must touch exactly one reactor. Otherwise the chamber breaks and drops as an item.

> **WARNING:** A chamber adds no heat capacity. Check the reactor heat after each change of components.


## Recipe

<RecipesFor id="mio_icif:reactor/block_reactor_chamber" fallbackText="-" />
