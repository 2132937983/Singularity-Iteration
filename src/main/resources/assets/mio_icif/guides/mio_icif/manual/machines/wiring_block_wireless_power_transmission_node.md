---
navigation:
  title: "Wireless Power Transmission Node"
  icon: mio_icif:wiring/block_wireless_power_transmission_node
  parent: power.md
  position: 37
item_ids:
  - mio_icif:wiring/block_wireless_power_transmission_node
---

# Wireless Power Transmission Node

<Row>
  <BlockImage id="mio_icif:wiring/block_wireless_power_transmission_node" scale="3" />
</Row>

## Function

The node sends EU without cables to a target block in a loaded chunk, at any distance.
A node with a target takes EU from adjacent cables and sends the EU to the target.
A node without a target supplies adjacent machines and cables. The packet matches the weakest neighbour.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LuV (32,768 EU) |
| Maximum input | 32,768 EU/t |
| Energy storage | 196,608 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place a receiver node next to the machines or cables that need EU.
2. Place a sender node next to the generator cable.
3. Shift-right-click the receiver node with the Electric Wireless Manager to store the target.
4. Right-click the sender node with the Electric Wireless Manager to link the node.

## Notes

- The target can also be a machine or a storage block.
- The node glows while the node sends EU. A target in an unloaded chunk gets no EU.

## Recipe

<RecipesFor id="mio_icif:wiring/block_wireless_power_transmission_node" fallbackText="-" />
