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
| Use while working | 1,000 EU/t |

The game measured these values from the block entity of this version.

## Procedure

1. Place the injector with its front face against the reactor or a reactor chamber.
2. Connect an MV cable to the machine.
3. Fill the storage slots with Block of Redstone.
4. Check the stock of Block of Redstone at regular intervals.

## Notes

- When the stock is empty, the condensators in the reactor wear out.

> **WARNING:** The machine explodes on overvoltage above MV.


## Recipe

<RecipesFor id="mio_icif:producer/block_redstone_reactor_coolant_injector" fallbackText="-" />
