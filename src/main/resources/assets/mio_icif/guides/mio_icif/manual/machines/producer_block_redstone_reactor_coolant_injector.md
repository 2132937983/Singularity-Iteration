---
navigation:
  title: "Reactor Coolant Injector (RSH)"
  icon: mio_icif:producer/block_redstone_reactor_coolant_injector
  parent: machines.md
  position: 55
item_ids:
  - mio_icif:producer/block_redstone_reactor_coolant_injector
---

# Reactor Coolant Injector (RSH)

<Row>
  <BlockImage id="mio_icif:producer/block_redstone_reactor_coolant_injector" scale="3" />
</Row>

## Function

The Reactor Coolant Injector (RSH) repairs Redstone Condensators in a nuclear reactor. Each repair uses one Block of Redstone and EU.
The front face must touch the reactor or a reactor chamber. The machine has storage slots for the Block of Redstone.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 10,000 EU |
| Use per repair | 1,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the injector with its front face against the reactor or a reactor chamber.
2. Connect an MV (or lower) cable to a free face: the back, a side, the top or the bottom. The front and every face that touches a reactor part take no power.
3. Fill the storage slots with Block of Redstone.
4. Check the stock of Block of Redstone at regular intervals.

## Notes

- When the stock is empty, the condensators in the reactor wear out.
- Since 0.1.10.4 the injector takes no power on its front nor on a face that touches the reactor, a reactor chamber, the pressure vessel or its interface blocks. Before, the reactor fed its own output (up to 8,192 EU per packet) straight into the injector, and the injector exploded from overvoltage as soon as the reactor started.
- The injector gives no redstone signal (only a comparator level) and moves no heat.

> **WARNING:** The machine explodes on overvoltage above MV.


## Recipe

<RecipesFor id="mio_icif:producer/block_redstone_reactor_coolant_injector" fallbackText="-" />
