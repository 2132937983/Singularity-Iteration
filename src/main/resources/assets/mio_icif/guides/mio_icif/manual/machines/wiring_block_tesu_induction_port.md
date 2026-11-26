---
navigation:
  title: "TESU Wireless Induction Port"
  icon: mio_icif:wiring/block_tesu_induction_port
  parent: power.md
  position: 20
item_ids:
  - mio_icif:wiring/block_tesu_induction_port
---

# TESU Wireless Induction Port

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_induction_port" scale="3" />
</Row>

## Function

In a port slot of a formed TESU, it does everything a [TESU Wireless Output Port](wiring_block_tesu_wireless_port.md) does
(the centre of a wireless field; relays link to it; machines with a Wireless Upgrade are filled), and within ±16 blocks of itself:

* every player is charged: the electric items in armor, hands and inventory are filled every tick, as far as they take;
* every machine that takes energy is filled every tick **without** a Wireless Upgrade, whatever its voltage, with a particle
  stream from the port. Generators, energy stores, cables and nodes are left out.

The energy comes from the TESU controller. The port holds none itself.

## Use

1. Put the port in a port slot of the TESU base ring.
2. Right-click it to see whether it is on a formed TESU.

## Recipe

<RecipesFor id="mio_icif:wiring/block_tesu_induction_port" fallbackText="-" />
