---
navigation:
  title: "TESU Input Port"
  icon: mio_icif:wiring/block_tesu_input
  parent: power.md
  position: 18
item_ids:
  - mio_icif:wiring/block_tesu_input
---

# TESU Input Port

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_input" scale="3" />
</Row>

## Function

The TESU Input Port takes energy of any voltage (up to MAX) into a buffer of two MAX packets and hands up to one MAX packet
a tick to its controller. It works in a port slot of the TESU base ring.

## Power data

| Item | Value |
|---|---|
| Input voltage | any, up to MAX (2,147,483,647 EU a packet) |
| To the controller | up to 2,147,483,647 EU/t |

## Recipe

<RecipesFor id="mio_icif:wiring/block_tesu_input" fallbackText="-" />
