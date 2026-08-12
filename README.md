<div align="center">

# ⚡🏭 FastMachines — Slimefun Legacy

**Bulk-craft machine recipes without laying every ingredient out by hand.**

![Slimefun Legacy](https://img.shields.io/badge/Slimefun-Legacy-6bd425?style=for-the-badge)
![Paper 26.2](https://img.shields.io/badge/Paper-26.2-blue?style=for-the-badge)
![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)
![Maintained for AlbionMC.com](https://img.shields.io/badge/Maintained%20for-albionmc.com-7b68ee?style=for-the-badge)

</div>

> [!IMPORTANT]
> This is an **unofficial Slimefun Legacy maintenance fork** of FastMachines, developed for use on **albionmc.com** while preserving the original project lineage and gameplay.

## 🏭 What does FastMachines do?

FastMachines provides menu-driven, **bulk-crafting versions of many vanilla and Slimefun machines**. Ingredients can be placed in the machine input area without reproducing the exact recipe layout, a valid output can be selected, and the machine crafts the requested quantity using available materials and energy.

Included machine families cover:

- Fast Crafting Table and Fast Furnace;
- Fast Enhanced Crafting Table;
- Grind Stone, Armor Forge, Ore Crusher, Compressor, Smeltery, and Pressure Chamber;
- Magic Workbench, Ore Washer, Table Saw, Composter, Panning Machine, Juicer, and Ancient Altar;
- optional Infinity Expansion / Infinity Expansion 2 machines;
- optional SlimeFrame Foundry support.

## 🧪 Slimefun Legacy maintenance

This maintenance line focuses on modern Paper and Slimefun Legacy compatibility while preserving FastMachines behavior.

Notable work includes:

- Paper 26.2 and modern Java build support;
- Slimefun Legacy as the primary API/runtime target;
- main-thread-safe machine inventory processing;
- corrected bulk ingredient allocation and consumption;
- current-energy checks and energy-limited partial bulk crafting;
- corrected display-recipe quantities and item comparison;
- hopper, cauldron, and anvil protection fixes;
- English-first operation without requiring SlimefunTranslation;
- disabling the archived binary self-updater;
- automated GitHub build and validation.

See `ALBION_26_2_NOTES.md` for historical technical notes; the maintained project itself is branded **Slimefun Legacy**, not as a separate Albion edition.

## ❤️ Credits & project lineage

FastMachines has several important layers of history and all of them deserve credit:

- **ybw0014** — original FastMachines creator/maintainer credited by the project.
- **Final_Root / FinalTECH** — machine concepts and code lineage that inspired and informed FastMachines.
- **GuizhanCraft/FastMachines** — the immediate upstream repository from which this fork was created and a source of later maintenance work.
- **Slimefun developers and community contributors** — APIs, compatibility work, and the ecosystem supporting the addon.
- **wickidcow / Slimefun Legacy** — current modern-server compatibility and maintenance for albionmc.com.

This fork is a preservation/compatibility continuation and does not claim ownership of upstream work.

## 📜 GNU General Public License v3.0

FastMachines is licensed under the **GNU General Public License v3.0 (GPLv3)**. The complete terms are in `LICENSE`.

If you distribute FastMachines or a modified GPL-covered version, comply with GPLv3: preserve applicable notices, identify modified versions, license covered modified source under GPLv3, and make the required Corresponding Source available when distributing object code.

The software is supplied **without warranty** as stated by GPLv3.

## ⚖️ Independence & trademark notice

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

FastMachines, Slimefun Legacy, and this maintenance fork are independent community projects. They are not sponsored, endorsed, approved, or operated by Mojang Studios or Microsoft. Minecraft-related names, brands, and assets remain the property of their respective rights holders.

This repository is also not represented as an official release of ybw0014, Final_Root, GuizhanCraft, the original Slimefun developers, or other upstream contributors unless explicitly stated by them.

---

<div align="center">

**⚡ Same recipes. Bigger batches. Faster factories. 🏭**

</div>
