---
navigation:
  title: "TESU Wireless Output Port"
  icon: mio_icif:wiring/block_tesu_wireless_port
  parent: power.md
  position: 20
item_ids:
  - mio_icif:wiring/block_tesu_wireless_port
---

# TESU Wireless Output Port

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_wireless_port" scale="3" />
</Row>

## Function

In a port slot of a formed TESU, the wireless output port is the centre of a wireless field: a cube of ±16 blocks round it.
Every machine inside it with a Wireless Upgrade in an upgrade slot is filled every tick from the TESU, whatever its voltage
(no cable, no overvoltage). The port holds no energy itself. TESU Relays carry the field further.
While it charges machines, the port sends a particle stream to each of them (as the Wireless Transmission Node does).

## Use

1. Put the port in a port slot of the TESU base ring.
2. Put a Wireless Upgrade in each machine that should be charged (any machine with upgrade slots that uses energy).
3. Right-click the port to see whether it is on a formed TESU.

## Notes

- A machine inside several fields takes from the first that has energy.
- The Wireless Upgrade fits machines that take a transformer or energy storage upgrade.

## Recipe

<RecipesFor id="mio_icif:wiring/block_tesu_wireless_port" fallbackText="-" />
