// spotless:off
package com.xyp.gtnotgood.commandtree.util;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.Message;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;

/** Ported command-tree ComponentUtils used by the integrated chat suggestions. */
public class ComponentUtils {
   public static IChatComponent fromMessage(Message message) {
      return (message instanceof IChatComponent) ? (IChatComponent) message : new ChatComponentText(message.getString());
   }
}
// spotless:on
