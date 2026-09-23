// spotless:off
package com.xyp.gtnotgood.commandtree.command.serialization.serializers;

import com.xyp.gtnotgood.commandtree.command.serialization.ArgumentSerializer;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.LongArgumentType;
import net.minecraft.network.PacketBuffer;

/** Ported command-tree LongArgumentSerializer used by the integrated chat suggestions. */
public class LongArgumentSerializer implements ArgumentSerializer<LongArgumentType> {
   public void serializeToNetwork(LongArgumentType pArgument, PacketBuffer pBuffer) {
      boolean flag = pArgument.getMinimum() != Long.MIN_VALUE;
      boolean flag1 = pArgument.getMaximum() != Long.MAX_VALUE;
      pBuffer.writeByte(BrigadierArgumentSerializers.createNumberFlags(flag, flag1));
      if (flag) {
         pBuffer.writeLong(pArgument.getMinimum());
      }

      if (flag1) {
         pBuffer.writeLong(pArgument.getMaximum());
      }
   }

   public LongArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
      byte b0 = pBuffer.readByte();
      long i = BrigadierArgumentSerializers.numberHasMin(b0) ? pBuffer.readLong() : Long.MIN_VALUE;
      long j = BrigadierArgumentSerializers.numberHasMax(b0) ? pBuffer.readLong() : Long.MAX_VALUE;
      return LongArgumentType.longArg(i, j);
   }
}
// spotless:on
