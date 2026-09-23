// spotless:off
package com.xyp.gtnotgood.commandtree.command.serialization.serializers;

import com.xyp.gtnotgood.commandtree.command.serialization.ArgumentSerializer;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.DoubleArgumentType;
import net.minecraft.network.PacketBuffer;

/** Ported command-tree DoubleArgumentSerializer used by the integrated chat suggestions. */
public class DoubleArgumentSerializer implements ArgumentSerializer<DoubleArgumentType> {
   public void serializeToNetwork(DoubleArgumentType pArgument, PacketBuffer pBuffer) {
      boolean flag = pArgument.getMinimum() != -Double.MAX_VALUE;
      boolean flag1 = pArgument.getMaximum() != Double.MAX_VALUE;
      pBuffer.writeByte(BrigadierArgumentSerializers.createNumberFlags(flag, flag1));
      if (flag) {
         pBuffer.writeDouble(pArgument.getMinimum());
      }

      if (flag1) {
         pBuffer.writeDouble(pArgument.getMaximum());
      }
   }

   public DoubleArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
      byte b0 = pBuffer.readByte();
      double d0 = BrigadierArgumentSerializers.numberHasMin(b0) ? pBuffer.readDouble() : -Double.MAX_VALUE;
      double d1 = BrigadierArgumentSerializers.numberHasMax(b0) ? pBuffer.readDouble() : Double.MAX_VALUE;
      return DoubleArgumentType.doubleArg(d0, d1);
   }
}
// spotless:on
