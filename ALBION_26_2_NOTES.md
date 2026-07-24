# FastMachines 26.2 — Albion Maintenance Notes

This fork starts from archived FastMachines Build 43 and targets Paper 26.2 with the Albion English Gugu Slimefun API.

## Build and compatibility

- Paper API `26.2.build.+`
- Java 25 bytecode and toolchain
- Kotlin 2.4.10
- Gradle 9.4.1
- Shadow 9.6.1
- Builds against `wickidcow/Slimefun4.1` in GitHub Actions rather than an unrelated archived Slimefun snapshot
- Optional addon dependencies are non-transitive so they cannot replace the selected Slimefun API during compilation
- Removed Shadow minimization to protect classes reached through reflection, builders, services, and optional integrations
- Added a manual `plugin.yml` with `api-version: '26.2'`
- Disabled archived binary auto-updates

## Machine fixes

- Moved all BlockMenu and inventory ticking to the primary server thread.
- Replaced inventory hash-only change detection with complete item-map comparison.
- Corrected the `use-bukkit-item-comparison` config path while preserving the compatibility matcher as the default.
- Made `ItemWrapper.equals` and `hashCode` consistent.
- Added exact max-flow allocation for overlapping recipe alternatives.
- Fixed mixed material choices and repeated shapeless choices consuming the wrong quantity.
- Fixed bulk crafts using machine capacity instead of current stored energy.
- Energy-limited bulk crafting now performs as many crafts as the stored charge allows.
- Clears a selected recipe when the current ingredients no longer support it.
- Performs a final synchronous inventory validation before consuming anything.
- Preserves quantities shown in Slimefun display recipes.
- Replaced obsolete reflective Bukkit recipe output lookup with `Recipe.getResult()`.
- Clears loader state before recipe reload and locks recipes after registry finalization.
- Added cauldron-level protection for the Fast Ore Washer.
- Blocks vanilla hopper pickup, input, and output behavior for the Fast Panning Machine.
- Corrected anvil physics protection to inspect the affected Fast SlimeFrame Foundry block.

## Verification

- Added unit coverage for overlapping ingredient choices and split alternative stacks.
- Added an English-source guard for Kotlin code and the default `en-US` resources.
- GitHub Actions builds the exact Albion Slimefun API first and then builds/tests FastMachines using Java 25.

## Deployment

Back up the server, replace the old FastMachines JAR, and perform a full restart. Do not use `/reload` or a plugin hot-reloader.
