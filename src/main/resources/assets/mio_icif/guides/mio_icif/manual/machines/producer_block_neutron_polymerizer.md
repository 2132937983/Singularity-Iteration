---
navigation:
  title: "Particle Aggregator"
  icon: mio_icif:producer/block_neutron_polymerizer
  parent: machines.md
  position: 50
item_ids:
  - mio_icif:producer/block_neutron_polymerizer
---

# Particle Aggregator

<Row>
  <BlockImage id="mio_icif:producer/block_neutron_polymerizer" scale="3" />
</Row>

## Function

The Particle Aggregator uses a large amount of EU to convert items into rare items.
Coal becomes a diamond, titanium becomes Iridium, and a glass cable becomes a superconducting cable.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input | 2,048 EU/t |
| Energy storage | 2,304,000 EU |
| Use while working | 1,536 EU/t |
| Operation time | 1,500 tick (75 s) |
| Energy per operation | 2,304,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an EV supply to the machine through transformers.
2. Put the input item into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above EV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_neutron_polymerizer" fallbackText="-" />
