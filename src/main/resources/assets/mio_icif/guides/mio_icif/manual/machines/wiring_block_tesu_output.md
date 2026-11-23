---
navigation:
  title: "TESU Output Port"
  icon: mio_icif:wiring/block_tesu_output
  parent: power.md
  position: 19
item_ids:
  - mio_icif:wiring/block_tesu_output
---

# TESU Output Port

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_output" scale="3" />
</Row>

## Function

The TESU Output Port sends the controller's energy at the output set on it. Right-click opens its screen (like the Creative
Generator's): type any output from 0 to 2,147,483,647 EU/t (k, M, G work) and click "Set" or press Enter; the ten keys set
the full packet of each voltage from IV to MAX, x10 and /10 scale it, and the switch stops the output. The screen shows the
output, the voltage, the EU sent in the last tick and the TESU's energy (in red when the port is not on a formed TESU).

The voltage follows the output: the lowest one from IV up whose packet holds it, so a small output does not come at a
needlessly high voltage. The port keeps two packets, refilled from the controller every tick, and sends one packet (the
output) a tick. It works in a port slot of the TESU base ring. A port from an older save keeps the full packet of its voltage.

## Power data

| Voltage | Full packet EU (= highest EU/t) |
|---|---|
| IV | 8,192 |
| LuV | 32,768 |
| ZPMV | 131,072 |
| UV | 524,288 |
| UHV | 2,097,152 |
| UEV | 8,388,608 |
| UIV | 33,554,432 |
| UXV | 134,217,728 |
| OpV | 536,870,912 |
| MAX | 2,147,483,647 |

## Recipe

<RecipesFor id="mio_icif:wiring/block_tesu_output" fallbackText="-" />
