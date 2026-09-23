// spotless:off
package com.xyp.gtnotgood.commandtree.accessor;

import java.util.function.BiFunction;
import org.jetbrains.annotations.Nullable;

/** Extra drawing controls used only by the chat command input. */
public interface GuiTextFieldExtras {
   void brigo$chatInput(boolean enabled);
   void brigo$suggestion(@Nullable String var1);

   void brigo$textFormatter(BiFunction<String, Integer, String> var1);

   int brigo$screenX(int var1);
}
// spotless:on
