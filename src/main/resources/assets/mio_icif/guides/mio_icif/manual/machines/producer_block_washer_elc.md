---
navigation:
  title: "Ore Washer"
  icon: mio_icif:producer/block_washer_elc
  parent: machines.md
  position: 49
item_ids:
  - mio_icif:producer/block_washer_elc
---

# Ore Washer

<Row>
  <BlockImage id="mio_icif:producer/block_washer_elc" scale="3" />
</Row>

## Function

The Ore Washer uses EU and water to wash crushed ores into purified crushed ores and by-products.
Each operation uses water from the internal tank. The machine has three output slots.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 8,000 EU |
| Use while working | 16 EU/t |
| Operation time | 400 tick (20 s) |
| Energy per operation | 6,400 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Put a Water Bucket or a Water Cell into the water slot, or connect a water pipe.
3. Put the crushed ore into the input slot.
4. Take the products from the three output slots.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_washer_elc" fallbackText="-" />
