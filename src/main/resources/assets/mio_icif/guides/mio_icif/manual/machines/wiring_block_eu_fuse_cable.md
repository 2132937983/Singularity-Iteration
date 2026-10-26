---
navigation:
  title: "EU Fuse Cable"
  icon: mio_icif:wiring/block_eu_fuse_cable
  parent: power.md
  position: 10
item_ids:
  - mio_icif:wiring/block_eu_fuse_cable
---

# EU Fuse Cable

<Row>
  <BlockImage id="mio_icif:wiring/block_eu_fuse_cable" scale="3" />
</Row>

## Function

The EU Fuse Cable is a cheap one-time protection. It blows for an overload, a short circuit or an overvoltage like the breaker cable, but it stays open. Redstone does not open it.

## Power data

This block uses no EU.

## Procedure

1. Replace one cable block of the line with the fuse cable.
2. Right-click it with an empty hand to set the rated flow, the rated voltage and the delay.
3. When it blows, right-click it with a new fuse cable to restore it. The new fuse is used up. A blown fuse drops nothing.

## Recipe

<RecipesFor id="mio_icif:wiring/block_eu_fuse_cable" fallbackText="-" />
