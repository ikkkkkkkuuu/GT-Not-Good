// spotless:off
package com.xyp.gtnotgood.commandtree.command.serialization.serializers;

import com.xyp.gtnotgood.commandtree.command.serialization.ArgumentSerializer;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.StringArgumentType;
import net.minecraft.network.PacketBuffer;

/** Ported command-tree StringArgumentSerializer used by the integrated chat suggestions. */
public class StringArgumentSerializer implements ArgumentSerializer<StringArgumentType> {
   public void serializeToNetwork(StringArgumentType pArgument, PacketBuffer pBuffer) {
      pBuffer.writeVarIntToBuffer(pArgument.getType().ordinal());
   }

   public StringArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
      int ordinal = pBuffer.readVarIntFromBuffer();
      StringArgumentType.StringType[] values = StringArgumentType.StringType.values();
      StringArgumentType.StringType stringtype = ordinal >= 0 && ordinal < values.length ? values[ordinal] : StringArgumentType.StringType.GREEDY_PHRASE;
      switch (stringtype) {
         case SINGLE_WORD:
            return StringArgumentType.word();
         case QUOTABLE_PHRASE:
            return StringArgumentType.string();
         case GREEDY_PHRASE:
         default:
            return StringArgumentType.greedyString();
      }
   }
}
// spotless:on
