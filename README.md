# FastMachines — Albion 26.2 Edition

A maintained English fork of FastMachines for Paper 26.2 and the Albion/Gugu Slimefun API.

FastMachines provides menu-based bulk crafting versions of vanilla, Slimefun, InfinityExpansion, InfinityExpansion2, and SlimeFrame machines. Ingredients can be placed anywhere in the input area, a valid output can be selected, and the machine crafts the requested quantity using materials and optional Slimefun energy.

## Included machines

- Fast Crafting Table and Fast Furnace
- Fast Enhanced Crafting Table
- Fast Grind Stone, Armor Forge, Ore Crusher, Compressor, Smeltery, and Pressure Chamber
- Fast Magic Workbench, Ore Washer, Table Saw, Composter, Panning Machine, Juicer, and Ancient Altar
- Optional InfinityExpansion and InfinityExpansion2 machines
- Optional SlimeFrame Foundry

## Albion maintenance changes

- Paper 26.2 and Java 25 build target
- Compiles against the exact `wickidcow/Slimefun4.1` fork
- Main-thread-safe machine inventory processing
- Correct bulk ingredient allocation and consumption
- Current-energy checks and partial energy-limited bulk crafting
- Fixed display-recipe quantities and Bukkit comparison setting
- Hopper, cauldron, and anvil protection fixes for machine blocks
- English default with no SlimefunTranslation requirement
- Archived binary updater disabled
- Automated GitHub build and tests

See [ALBION_26_2_NOTES.md](ALBION_26_2_NOTES.md) for the full technical change list.

## Build

The included GitHub Actions workflow:

1. Checks out `wickidcow/Slimefun4.1`.
2. Publishes that Slimefun API to Maven Local.
3. Builds and tests FastMachines using Java 25.
4. Uploads the production JAR as an artifact.

## Configuration

The default language is `en-US`. SlimefunTranslation is optional and is not required for English players.

Fast machine energy use, research requirements, tick rate, and item-comparison behavior are configurable in `config.yml`.

## Credits

FastMachines was originally created by ybw0014 from machine concepts and code originating in FinalTECH by Final_Root. This downstream fork preserves the original GPL-3.0 license and contributor attribution.
