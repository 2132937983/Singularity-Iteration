---
navigation:
  title: "Reactor Accident Audit"
  icon: mio_icif:reactor/item_reactor_blackbox
  parent: generators.md
  position: 23
item_ids:
  - mio_icif:reactor/item_reactor_blackbox
---

# Reactor Accident Audit

<Row>
  <ItemImage id="mio_icif:reactor/item_reactor_blackbox" scale="3" />
</Row>

## Function

Every [Nuclear Reactor Generator](generator_block_nuclear_reactor_generator.md) has a flight recorder.
It writes one record per reactor cycle (1 second) and keeps the last 40 cycles.
When the reactor fails, the server saves the record as an incident and finds the causes.
The audit report shows the incident: what happened, where, and why.

The server keeps the last 64 incidents. The log is saved with the world.

## What makes an incident

| Event | Kind | Blackbox |
|---|---|---|
| Nuclear explosion | Nuclear explosion | Drops at the site |
| Local reactor explosion | Reactor explosion | Drops at the site |
| The hull reaches its limit while reactor explosions are off in the config | Meltdown (explosions off) | No (the reactor stays) |
| A running reactor, or a reactor with at least 10 % hull heat, is removed | Removed while hot | No |

The blackbox does not burn, survives explosions, glows and never despawns.

## Opening a report

* Use (right-click) the Reactor Blackbox. Anyone who holds it can read its report.
* `/mio_icif reactor audit` lists the incidents, newest first. Click a line to open its report.
* `/mio_icif reactor audit 12` opens the report of incident 12.
* `/mio_icif reactor audit clear` empties the log (operators only).

A player sees the incidents of their own reactors and the incidents they were near (within 64 blocks).
Operators see all incidents. The owner of a reactor is the player who placed it.

## The report

* **Header**: incident number, kind, date and time, game day, dimension, position, owner and the players nearby.
* **Main cause** in red, with an explanation and advice. Below it, the contributing factors.
* **Core layout**: the slots at the last cycle.
  * A melted part is shown in its slot, red and crossed out. A worn-out reflector is orange.
  * Spent fuel is grey. A bar shows the heat in a part or the fuel left in a rod.
  * Dark columns had no Reactor Chamber.
  * Point at a slot for the details.
* **Graph**: the hull heat over the last 40 seconds. The dashed red line is the limit.
  Orange bars are the heat made per cycle, blue bars the heat taken away. Point at the graph for each cycle.
* **Metrics** of the last cycle: hull heat, heat in and out, EU/t, feed rate, columns, coolant, fuel rods, cooling parts, automation.

## Causes

| Cause | Meaning |
|---|---|
| Thermal runaway: not enough cooling | The hull heated up for several cycles: the core made more heat than the parts took away. |
| Thermal runaway from melted cooling parts | Vents, cells or exchangers melted; their heat went into the hull. |
| Hot coolant tank full: heat removal stopped | Fluid reactor: the hot coolant was not pumped out. |
| No coolant | Fluid reactor: the cold coolant ran out. |
| Liquid cooling stopped | Generator mode: the liquid cooling ran dry or its hot coolant was not taken out. |
| External overvoltage (machine explosion) | A machine next to the reactor exploded from overvoltage just before the accident. |
| Reactor chamber lost | A chamber was removed or destroyed; the parts in its column dropped out. |
| Reactor chamber in an unloaded chunk | A chamber was in a chunk that was not loaded. Keep the reactor in one chunk. |
| Sudden heat surge | The hull went from below half to the limit in one second. |
| Fuel added while running, High feed rate, Automation moved items, Cooling parts saturated | Contributing factors. |

## Notes

> **TIP:** Test a design in the [Reactor Simulator](reactor_simulator.md) first. It uses the same calculation.

The recorder lives in memory: right after the world is loaded it has no cycles yet.
