// spotless:off
package com.xyp.gtnotgood.commandtree.compat.mods;

import com.xyp.gtnotgood.commandtree.compat.CompatMod;
import java.util.Arrays;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/** Ported command-tree MinecraftMod used by the integrated chat suggestions. */
public class MinecraftMod implements CompatMod {
   @NotNull
   @Override
   public String identifier() {
      return "minecraft";
   }

   @Override
   public Collection<String> commands() {
      return Arrays.asList(
         "net.minecraft.command.CommandDefaultGameMode",
         "net.minecraft.command.CommandDifficulty",
         "net.minecraft.command.CommandGameMode",
         "net.minecraft.command.CommandGameRule",
         "net.minecraft.command.CommandTime",
         "net.minecraft.command.CommandWeather",
         "net.minecraft.command.server.CommandWhitelist"
      );
   }
}
// spotless:on
