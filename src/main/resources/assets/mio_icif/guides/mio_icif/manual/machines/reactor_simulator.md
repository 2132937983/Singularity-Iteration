---
navigation:
  title: "Reactor Simulator"
  icon: mio_icif:reactor/item_reactor_uranium_quad
  parent: generators.md
  position: 22
---

# Reactor Simulator

## Function

The simulator calculates a reactor design before you build it.
It uses the same calculation as the [Nuclear Reactor Generator](generator_block_nuclear_reactor_generator.md).
It starts with a cold reactor, new components and full fuel rods.
It calculates 100 seconds (100 reactor cycles).

## Procedure

1. Click a component in the two bottom rows. The component becomes the selected component.
2. Click a reactor slot. The selected component goes into the slot.
3. To remove a component, right-click the slot.
4. To set the number of Reactor Chambers (0 to 6), click "-" or "+".
5. Read the values on the right side.

## Values

| Value | Meaning |
|---|---|
| Output | Mean EU output for each tick |
| Heat | Heat that the fuel rods make in one cycle (1 second) |
| Vented | Heat that the vents remove in one cycle |
| Hull | Hull heat at the end of the calculation / maximum hull heat |
| Trend | Change of the hull heat in one cycle (mean of the last 10 cycles) |
| Margin | 100 % minus the highest hull heat, in percent of the maximum |

The design is **SAFE** when these three conditions are true:

* No component melts.
* The hull heat does not increase.
* No meltdown occurs.

## Rules

* A fuel rod gives its heat only to adjacent components that can hold heat. If no adjacent component can hold heat, the hull gets the heat.
* Each adjacent fuel rod or reflector adds one pulse. Each pulse makes more EU. The heat increases faster than the EU.
* A vent removes heat from itself. Some vents also pull heat from the hull.
* A component vent removes heat from the adjacent components.
* Plating increases the maximum hull heat.
* A component that gets more heat than its capacity melts. Then its heat goes to the hull.

## Tutorial 1: first reactor (5 EU/t)

One single fuel rod makes 4 heat in each cycle. Four basic vents remove up to 6 heat each.

<ReactorSimulator preset="_,vent,_/vent,uranium_simple,vent/_,vent,_" />

## Tutorial 2: dual fuel rod (20 EU/t)

A dual fuel rod has two pulses for each cell. It makes 24 heat in each cycle. Each of the four vents gets 6 heat. This is the limit of a basic vent.

<ReactorSimulator preset="_,vent,_/vent,uranium_dual,vent/_,vent,_" />

## Tutorial 3: high output (300 EU/t, 4 chambers)

Five quad fuel rods make 96 heat each in each cycle. Each quad fuel rod has its own iridium vents. No vent touches two fuel rods. Thus each vent gets 24 to 32 heat and removes 36.

<ReactorSimulator preset="iridium_vent,uranium_quad,iridium_vent,_,iridium_vent,uranium_quad,iridium_vent/_,iridium_vent,_,iridium_vent,_,iridium_vent,_/iridium_vent,iridium_vent,iridium_vent,uranium_quad,iridium_vent,iridium_vent,iridium_vent/iridium_vent,uranium_quad,iridium_vent,iridium_vent,iridium_vent,uranium_quad,iridium_vent/_,iridium_vent,_,_,_,iridium_vent,_/_,_,_,_,_,_,_" />

## Example of a failure

<ReactorSimulator preset="vent,uranium_quad,vent/_,vent,_/_,_,_" />

The quad fuel rod makes 96 heat in each cycle. Three basic vents can remove only 18 heat. The vents melt. Then the hull gets all the heat. The hull heat increases by 96 in each cycle. A meltdown occurs after approximately 143 seconds.

**WARNING:** A meltdown causes an explosion. The explosion destroys blocks near the reactor. Do a check of each design in the simulator before you put fuel rods in a reactor.

## Free design

<ReactorSimulator chambers="6" />
