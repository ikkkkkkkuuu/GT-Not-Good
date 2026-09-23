// spotless:off
package com.xyp.gtnotgood.commandtree.command.serialization.serializers;

import com.xyp.gtnotgood.commandtree.command.serialization.ArgumentSerializer;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.ArgumentType;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;

/** Ported command-tree EmptyArgumentSerializer used by the integrated chat suggestions. */
public class EmptyArgumentSerializer<T extends ArgumentType<?>> implements ArgumentSerializer<T> {
   private final Supplier<T> constructor;

   public EmptyArgumentSerializer(Supplier<T> pConstructor) {
      this.constructor = pConstructor;
   }

   @Override
   public void serializeToNetwork(T pArgument, PacketBuffer pBuffer) {
   }

   @Override
   public T deserializeFromNetwork(PacketBuffer pBuffer) {
      return this.constructor.get();
   }
}
// spotless:on
