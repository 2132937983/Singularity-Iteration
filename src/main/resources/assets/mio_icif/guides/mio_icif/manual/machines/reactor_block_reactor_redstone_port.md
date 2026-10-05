---
navigation:
  title: "Reactor Redstone Port"
  icon: mio_icif:reactor/block_reactor_redstone_port
  parent: heavy.md
  position: 14
item_ids:
  - mio_icif:reactor/block_reactor_redstone_port
---

# Reactor Redstone Port

<Row>
  <BlockImage id="mio_icif:reactor/block_reactor_redstone_port" scale="3" />
</Row>

## Function

The Reactor Redstone Port is a shell block of the fluid reactor. A redstone signal on the port enables the reactor.
The port has no GUI. The port texture changes when the port receives a signal.

## Power data

This block uses no EU.

## Procedure

1. Build the 5x5x5 fluid reactor shell.
2. Replace one pressure vessel in the shell with the redstone port.
3. Place a lever on the port.
4. Turn on the lever to start the reactor.
5. Turn off the lever to stop the reactor.

## Notes


> **WARNING:** Remove the redstone signal before you change reactor components. A running reactor can overheat and explode.


## Recipe

<RecipesFor id="mio_icif:reactor/block_reactor_redstone_port" fallbackText="-" />
