// spotless:off
package com.xyp.gtnotgood.commandtree.compat.mods;

import com.xyp.gtnotgood.commandtree.compat.CompatMod;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

/** Ported command-tree ThaumcraftMod used by the integrated chat suggestions. */
public class ThaumcraftMod implements CompatMod {
   @NotNull
   @Override
   public String identifier() {
      return "thaumcraft";
   }

   @Override
   public Collection<String> commands() {
      return Collections.singletonList("thaumcraft.common.lib.CommandThaumcraft");
   }
}
// spotless:on
