---
navigation:
  title: "Steam Tunnel"
  icon: mio_icif:generator/block_steam_tunnel
  parent: generators.md
  position: 32
item_ids:
  - mio_icif:generator/block_steam_tunnel
  - mio_icif:generator/block_steam_tunnel_casing
  - mio_icif:generator/block_steam_tunnel_fluid_port
  - mio_icif:generator/block_steam_turbine_rotor_bronze
  - mio_icif:generator/block_steam_turbine_rotor_iron
  - mio_icif:generator/block_steam_turbine_rotor_adviron
  - mio_icif:generator/block_steam_turbine_rotor_titanium
  - mio_icif:generator/block_steam_turbine_rotor_iridium
  - mio_icif:generator/block_steam_turbine_rotor_netherite
---

# Steam Tunnel

<Row>
  <BlockImage id="mio_icif:generator/block_steam_tunnel" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_casing" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_fluid_port" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_power_port" scale="3" />
</Row>

## Function

The large steam turbine of IC2 Classic: a hollow box of 3 x 3 x 5 blocks that changes much steam into EU in one place.

- Each tick it changes 1,024 mB of steam into 512 EU, up to 4 times (4,096 mB into 2,048 EU). That is the 2 mB per EU of the <ItemLink id="mio_icif:generator/block_steam_turbine" />. As in IC2 Classic it runs while its 10,000 EU buffer has room for one step and then makes every step the steam allows, so the buffer can go over by up to 1,536 EU. Steam Turbine Rotors inside raise the EU of each step (see Rotors).
- It returns 1 mB of water per 100 mB of steam, the same ratio at which the Steam Reactor boils water, so a reactor loop loses no water (IC2 Classic does the same at its own 1:160 ratio). The water tank holds 2,000 mB; water that does not fit is lost.
- The controller has no menu. Right-click it to see its state (running, idle, or what is missing and where), or look at it with Jade (steam, water, EU buffer, output).

## Power data

| Item | Value |
|---|---|
| Output voltage tier | HV (512 EU), at the power ports |
| Output | 2,048 EU/t without rotors, up to 3,968 EU/t with three netherite rotors (4 to 8 HV packets of 512 EU a tick) |
| Steam tank | 16,000 mB |
| Water tank | 2,000 mB |
| Energy buffer | 10,000 EU |

## Structure

- The controller sits in the middle of the front end and faces out; the tunnel lies behind it.
- The 28 edge blocks are casing. The 13 other shell blocks (the middles of the four long sides and the centre of the back) are casing, a fluid port or a power port. The 3 blocks inside are air or Steam Turbine Rotors.
- Removing a part stops the tunnel at once; it starts again when the structure is complete (checked every second).
- Casings and ports next to each other join into one panel. While the tunnel is formed, its front shows one large fan: the controller is the hub and the 8 casings around it carry the blades. The fan spins and blows steam while the tunnel runs; when the fan disappears, the structure is broken.

Drag the view to turn it.

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:steam_tunnel" />
</GameScene>

<SiStructureInfo id="mio_icif:steam_tunnel" />

## Rotors

Up to three Steam Turbine Rotors go into the three blocks inside the tunnel. Each adds EU to every step; the steam and the water per step stay the same. Tiers can be mixed and places can stay empty: without rotors the tunnel makes 512 EU per 1,024 mB as before.

<Row>
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_bronze" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_iron" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_adviron" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_titanium" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_iridium" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_netherite" scale="2" />
</Row>

| Rotor | EU added per step | With three of it |
|---|---|---|
| Bronze | +32 (+6.25%) | 608 EU per 1,024 mB |
| Iron | +64 (+12.5%) | 704 EU per 1,024 mB |
| Refined Iron | +80 (+15.625%) | 752 EU per 1,024 mB |
| Titanium | +96 (+18.75%) | 800 EU per 1,024 mB |
| Iridium | +128 (+25%) | 896 EU per 1,024 mB |
| Netherite | +160 (+31.25%) | 992 EU per 1,024 mB (3,968 EU/t) |

- One power port carries the full output: it sends 4 to 8 HV packets of 512 EU a tick, as the rotors need (up to 3,968 EU/t).
- Placing or removing a rotor takes effect at once, and a rotor may face any of the six directions. Jade shows the rotors inside, the steam use (full output needs 4,096 mB/t) and the maximum output, and says when the tunnel waits for steam or its buffer is full; a right-click on the controller says the same and shows the EU per step.
- A rotor is four rotor blades of its metal in the corners of the crafting grid with a shaft in the middle (an X; the plus shape makes a kinetic rotor). The netherite rotor is an iridium rotor upgraded at a smithing table with a netherite ingot and a Netherite Upgrade template.

## Procedure

1. Build the box with casings (a stonecutter makes one casing from one Basic Machine Hull).
2. Put at least one power port and connect an HV cable to it.
3. Pipe steam into a fluid port or into the controller. Fluid ports also push the water out into pipes and tanks next to them.
4. Right-click a fluid port to open its menu. Like the fluid reactor's fluid port it holds a Fluid Ejector Upgrade (water out, with the upgrade's directions and rate) or a Fluid Pulling Upgrade (steam in from tanks next to it).

## Notes

- A steam reactor with a quad fuel rod and four overclocked steam vents makes about 192 mB of steam a tick; the tunnel takes up to 4,096 mB a tick. It is for many reactors in one place, not for better efficiency.
- Several power ports share the output.

## Recipe

<RecipesFor id="mio_icif:generator/block_steam_tunnel" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_casing" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_fluid_port" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_power_port" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_bronze" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_iron" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_adviron" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_titanium" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_iridium" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_netherite" fallbackText="-" />
