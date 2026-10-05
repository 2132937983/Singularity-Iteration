---
navigation:
  title: "Reactor Fluid Port"
  icon: mio_icif:reactor/block_reactor_fluid_port
  parent: heavy.md
  position: 12
item_ids:
  - mio_icif:reactor/block_reactor_fluid_port
---

# Reactor Fluid Port

<Row>
  <BlockImage id="mio_icif:reactor/block_reactor_fluid_port" scale="3" />
</Row>

## Function

The Reactor Fluid Port connects pipes to the coolant tanks of a fluid reactor. Coolant goes in and hot coolant comes out.
The port has one upgrade slot for a fluid ejector upgrade or a fluid pulling upgrade.

## Power data

This block uses no EU.

## Procedure

1. Replace one pressure vessel in the reactor shell with the fluid port.
2. Connect a coolant supply pipe to the port.
3. Put a fluid ejector upgrade in the port slot.
4. Connect a pipe from the port to a tank or to a machine that uses hot coolant.

## Notes

- The port works with a reactor in fluid mode within 2 blocks.

> **WARNING:** Keep the hot coolant output free. A full hot coolant tank stops the cooling, and the reactor can overheat.


## Recipe

<RecipesFor id="mio_icif:reactor/block_reactor_fluid_port" fallbackText="-" />
