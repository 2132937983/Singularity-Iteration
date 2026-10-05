---
navigation:
  title: "Molecular Transformer"
  icon: mio_icif:producer/block_molecular_transformer
  parent: machines.md
  position: 47
item_ids:
  - mio_icif:producer/block_molecular_transformer
---

# Molecular Transformer

<Row>
  <BlockImage id="mio_icif:producer/block_molecular_transformer" scale="3" />
</Row>

## Function

The Molecular Transformer uses a very large amount of EU to transform one item into another item.
Glowstone dust becomes Sunnarium, an iron ingot becomes iridium ore, and a wither skeleton skull becomes a nether star.
The machine accepts any voltage tier and has no input limit.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | No limit |
| Energy storage | 120,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect a high-output generator or storage block to the machine.
2. Put the input item into the input slot.
3. Wait until the progress bar is full.
4. Take the result from the output slot.

## Notes

- A large stored EU buffer lets the machine finish more than one operation in the same tick.

## Recipe

<RecipesFor id="mio_icif:producer/block_molecular_transformer" fallbackText="-" />
