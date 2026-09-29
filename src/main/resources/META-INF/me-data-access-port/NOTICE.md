# ME data access hatch

Based on `DataHatchME.java` from reobf/Programmable-Hatches-Mod, MIT license.
Copyright (c) 2024 reobf. Full upstream license is in `LICENSE` beside this notice.

Source: https://github.com/reobf/Programmable-Hatches-Mod
Commit: `d035ae837db01d91a3a6bcc779dc5504459c4754` (`290-daily-latest`).
Original: `src/main/java/reobf/proghatches/gt/metatileentity/DataHatchME.java`.
Destination: `com.xyp.gtnotgood.common.machines.hatch.me.MEDataAccessHatch`.

Changes: host registration and generated translations; IV tier and IV assembler recipe;
GT 5.09.54.183 recipe API without reflection; read-only, NBT-sensitive ME snapshot;
coalesced network invalidation; explicit listener cleanup; disconnected research revocation;
rebuild cache after loading rather than persisting borrowed items.

Uses existing GregTech ME input overlays and IV casing textures through their runtime API.
No upstream image assets or reference checkout are packaged.
