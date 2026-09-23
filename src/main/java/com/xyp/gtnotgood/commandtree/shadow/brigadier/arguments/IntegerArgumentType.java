// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.StringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;

/** Relocated Brigadier IntegerArgumentType used by the command-tree parser. */
public class IntegerArgumentType implements ArgumentType<Integer> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0", "123", "-123");
   private final int minimum;
   private final int maximum;

   private IntegerArgumentType(int minimum, int maximum) {
      this.minimum = minimum;
      this.maximum = maximum;
   }

   public static IntegerArgumentType integer() {
      return integer(Integer.MIN_VALUE);
   }

   public static IntegerArgumentType integer(int min) {
      return integer(min, Integer.MAX_VALUE);
   }

   public static IntegerArgumentType integer(int min, int max) {
      return new IntegerArgumentType(min, max);
   }

   public static int getInteger(CommandContext<?> context, String name) {
      return context.getArgument(name, int.class);
   }

   public int getMinimum() {
      return this.minimum;
   }

   public int getMaximum() {
      return this.maximum;
   }

   public Integer parse(StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();
      int result = reader.readInt();
      if (result < this.minimum) {
         reader.setCursor(start);
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, result, this.minimum);
      } else if (result > this.maximum) {
         reader.setCursor(start);
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, result, this.maximum);
      } else {
         return result;
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof IntegerArgumentType)) {
         return false;
      } else {
         IntegerArgumentType that = (IntegerArgumentType)o;
         return this.maximum == that.maximum && this.minimum == that.minimum;
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.minimum + this.maximum;
   }

   @Override
   public String toString() {
      if (this.minimum == Integer.MIN_VALUE && this.maximum == Integer.MAX_VALUE) {
         return "integer()";
      } else {
         return this.maximum == Integer.MAX_VALUE ? "integer(" + this.minimum + ")" : "integer(" + this.minimum + ", " + this.maximum + ")";
      }
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
// spotless:on
