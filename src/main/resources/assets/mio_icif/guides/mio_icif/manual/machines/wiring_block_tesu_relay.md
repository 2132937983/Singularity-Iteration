---
navigation:
  title: "TESU Relay"
  icon: mio_icif:wiring/block_tesu_relay
  parent: power.md
  position: 21
item_ids:
  - mio_icif:wiring/block_tesu_relay
---

# TESU Relay

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_relay" scale="3" />
</Row>

## Function

A relay carries the TESU wireless field: once linked, it charges machines with a Wireless Upgrade within ±16 blocks of itself,
without loss; while it charges them, a particle stream runs from the top of its pylon to each machine. It links with the TESU Wireless Connector to a wireless output port or another relay, and only if it stands inside
that node's field, so the field grows step by step.

## Use

1. Place the relay inside the field of a wireless output port or of a linked relay.
2. Sneak-right-click that port or relay with the TESU Wireless Connector: it becomes the target (as with the remote of the wireless power transmission node).
3. Right-click the relay with the connector: it is linked to the target. The target stays, so several relays can be linked to one port; to go further, sneak-right-click a linked relay to make it the target. Sneak-right-click the air to clear the target.
4. A powered relay shows a stream of particles from the node it is linked to. Right-click it with an empty hand to see where it is linked and whether it is powered.

## Notes

- Links cannot make a loop. If a relay on the way is removed, the relays after it lose power until relinked.
- The relay is a pylon three blocks tall: it needs two free blocks above it, and breaking any part of it takes the whole pylon. Its lights glow while it is powered, and the particle stream runs between the tops of the pylons.

## Recipe

<RecipesFor id="mio_icif:wiring/block_tesu_relay" fallbackText="-" />
