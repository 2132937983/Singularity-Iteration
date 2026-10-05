---
navigation:
  title: "Matter Generator"
  icon: mio_icif:producer/block_matter_elc
  parent: machines.md
  position: 42
item_ids:
  - mio_icif:producer/block_matter_elc
---

# Matter Generator

<Row>
  <BlockImage id="mio_icif:producer/block_matter_elc" scale="3" />
</Row>

## Function

The Matter Generator converts a large amount of EU into UU-Matter.
Scrap, Scrap Box or Thorium Scrap Box in the amplifier slot reduces the EU cost.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input | 8,192 EU/t |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an EV supply to the machine through transformers.
2. Put Scrap into the amplifier slot.
3. Put Empty Cells into the container slot to collect UU-Matter, or connect a pipe.
4. Take the UU-Matter Cells from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above EV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_matter_elc" fallbackText="-" />
