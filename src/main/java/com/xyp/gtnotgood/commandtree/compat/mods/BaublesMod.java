// spotless:off
package com.xyp.gtnotgood.commandtree.compat.mods;

import com.xyp.gtnotgood.commandtree.compat.CompatMod;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

/** Ported command-tree BaublesMod used by the integrated chat suggestions. */
public class BaublesMod implements CompatMod {
   @NotNull
   @Override
   public String identifier() {
      return "baubles";
   }

   @Override
   public Collection<String> commands() {
      return Collections.singletonList("baubles.common.event.CommandBaubles");
   }
}
// spotless:on
