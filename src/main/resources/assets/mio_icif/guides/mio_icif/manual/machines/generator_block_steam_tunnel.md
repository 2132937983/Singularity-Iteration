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

- Each tick it changes 1,024 mB of steam into 512 EU, up to 4 times (4,096 mB into 2,048 EU), while its buffer has room. That is the 2 mB per EU of the <ItemLink id="mio_icif:generator/block_steam_turbine" />.
- It returns 1 mB of water per 100 mB of steam, the same ratio at which the Steam Reactor boils water. The water tank holds 2,000 mB; water that does not fit is lost.
- It has no menu. Right-click the controller to see its state (running, idle, or what is missing and where), or look at it with Jade.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | HV (512 EU), at the power ports |
| Output | up to 2,048 EU/t (4 packets of 512 EU a tick) |
| Steam tank | 16,000 mB |
| Water tank | 2,000 mB |
| Energy buffer | 10,000 EU |

## Structure

- The controller sits in the middle of the front end and faces out; the tunnel lies behind it.
- The 28 edge blocks are casing. The 13 other shell blocks (the middles of the four long sides and the centre of the back) are casing, a fluid port or a power port. The 3 blocks inside are air.
- Removing a part stops the tunnel at once; it starts again when the structure is complete (checked every second).

Drag the view to turn it.

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:steam_tunnel" />
</GameScene>

<SiStructureInfo id="mio_icif:steam_tunnel" />

## Procedure

1. Build the box with casings (a stonecutter makes one casing from one Basic Machine Hull).
2. Put at least one power port and connect an HV cable to it.
3. Pipe steam into a fluid port or into the controller. Fluid ports also push the water out into pipes and tanks next to them.

## Notes

- A steam reactor with a quad fuel rod and four overclocked steam vents makes about 192 mB of steam a tick; the tunnel takes up to 4,096 mB a tick. It is for many reactors in one place, not for better efficiency.
- Several power ports share the output.

## Recipe

<RecipesFor id="mio_icif:generator/block_steam_tunnel" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_casing" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_fluid_port" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_power_port" fallbackText="-" />
