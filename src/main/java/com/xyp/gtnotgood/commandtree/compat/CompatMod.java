// spotless:off
package com.xyp.gtnotgood.commandtree.compat;

import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/** Ported command-tree CompatMod used by the integrated chat suggestions. */
public interface CompatMod {
   @NotNull
   String identifier();

   Collection<String> commands();
}
// spotless:on
