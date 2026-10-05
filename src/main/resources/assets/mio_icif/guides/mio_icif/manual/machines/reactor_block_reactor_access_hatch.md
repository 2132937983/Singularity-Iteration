---
navigation:
  title: "Reactor Access Hatch"
  icon: mio_icif:reactor/block_reactor_access_hatch
  parent: heavy.md
  position: 10
item_ids:
  - mio_icif:reactor/block_reactor_access_hatch
---

# Reactor Access Hatch

<Row>
  <BlockImage id="mio_icif:reactor/block_reactor_access_hatch" scale="3" />
</Row>

## Function

The Reactor Access Hatch opens the GUI of the fluid reactor from the outer shell. The hatch has no inventory of its own.
The hatch switches the connected reactor to fluid mode when the structure is complete.

## Power data

This block uses no EU.

## Procedure

1. Replace one pressure vessel in the reactor shell with the hatch.
2. Right-click the hatch to open the reactor GUI.
3. Sneak and right-click the hatch to show a structure diagnosis.

## Notes


> **WARNING:** Do not place the hatch within 2 blocks of two reactors. The hatch then explodes.


## Recipe

<RecipesFor id="mio_icif:reactor/block_reactor_access_hatch" fallbackText="-" />
