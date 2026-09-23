// spotless:off
package com.xyp.gtnotgood.commandtree.command.serialization;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.ArgumentType;
import net.minecraft.network.PacketBuffer;

/** Ported command-tree ArgumentSerializer used by the integrated chat suggestions. */
public interface ArgumentSerializer<T extends ArgumentType<?>> {
   void serializeToNetwork(T var1, PacketBuffer var2);

   T deserializeFromNetwork(PacketBuffer var1);
}
// spotless:on
