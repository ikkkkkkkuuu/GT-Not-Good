// spotless:off
package com.xyp.gtnotgood.commandtree.command.serialization.serializers;

import com.xyp.gtnotgood.commandtree.command.serialization.ArgumentSerializer;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.FloatArgumentType;
import net.minecraft.network.PacketBuffer;

/** Ported command-tree FloatArgumentSerializer used by the integrated chat suggestions. */
public class FloatArgumentSerializer implements ArgumentSerializer<FloatArgumentType> {
   public void serializeToNetwork(FloatArgumentType pArgument, PacketBuffer pBuffer) {
      boolean flag = pArgument.getMinimum() != -Float.MAX_VALUE;
      boolean flag1 = pArgument.getMaximum() != Float.MAX_VALUE;
      pBuffer.writeByte(BrigadierArgumentSerializers.createNumberFlags(flag, flag1));
      if (flag) {
         pBuffer.writeFloat(pArgument.getMinimum());
      }

      if (flag1) {
         pBuffer.writeFloat(pArgument.getMaximum());
      }
   }

   public FloatArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
      byte b0 = pBuffer.readByte();
      float f = BrigadierArgumentSerializers.numberHasMin(b0) ? pBuffer.readFloat() : -Float.MAX_VALUE;
      float f1 = BrigadierArgumentSerializers.numberHasMax(b0) ? pBuffer.readFloat() : Float.MAX_VALUE;
      return FloatArgumentType.floatArg(f, f1);
   }
}
// spotless:on
