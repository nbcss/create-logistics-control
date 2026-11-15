# Create: Logistics Control

A [Create](https://www.curseforge.com/minecraft/mc-mods/create) add-on for NeoForge 1.21.1 that adds finer control over
Create's package logistics. Currently only **Filter Link** is introduced, which automatically set the filter to blocks.

## Filter Link

Some machines need a filter set to produce the intended result. The Filter Link sets those filters for you, based on
what a package was actually requested to produce.

1. **Select targets.** With a Filter Link in hand, right-click any block that has a filter slot (a Mechanical Crafter,
   a Deployer, and so on) to mark it as a target. You can select multiple targets, up to a configurable range.
2. **Mount it.** Place the Filter Link on a **Packager**.
3. **Done.** Whenever that Packager unpacks a package carrying a filter label, the Filter Link applies that label to
   every configured target - and clears it again when the package has none.

Packages requested by a **Stock Keeper** with a JEI recipe, or by a **Factory Gauge**, carry their expected output as
the filter label, so the Filter Link fills the target machines in automatically. The portable tickers of
**Create: Phantom** (1.0.3+) and **Create: Mobile Packages**, and **Deployer**'s Stock Keeper, work the same way.

An order with several different recipes needs a **Re-Packager** on its route: it repacks the order one recipe per
package, each labelled with that recipe's output. Items in such an order that belong to no recipe arrive in packages
that leave the targets' filters as they are.

With **Create: Fluid Logistics**, fluid outputs become fluid filter labels, and a recipe's fluid travels in the same
package as its items.

An in-game **Ponder** scene ("Using the Filter Link") walks through the whole setup.

## Requirements

- Minecraft 1.21.1
- NeoForge
- Create 6.0+

### Developing alongside Create: Factory Controller

Factory Controller consumes this repository as a Gradle **composite build**, expecting it to be checked out next to it:

```
some-folder/
  create-factory-controller/
  create-logistics-control/   <- this repo
```

With that layout, Factory Controller's build picks up the live sources here and bundles the result. This repository
still builds and runs entirely on its own.

## License

[MIT](LICENSE) (c) nbcss
