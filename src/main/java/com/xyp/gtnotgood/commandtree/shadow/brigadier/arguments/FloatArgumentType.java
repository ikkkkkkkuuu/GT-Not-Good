// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.StringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;

/** Relocated Brigadier FloatArgumentType used by the command-tree parser. */
public class FloatArgumentType implements ArgumentType<Float> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0", "1.2", ".5", "-1", "-.5", "-1234.56");
   private final float minimum;
   private final float maximum;

   private FloatArgumentType(float minimum, float maximum) {
      this.minimum = minimum;
      this.maximum = maximum;
   }

   public static FloatArgumentType floatArg() {
      return floatArg(-Float.MAX_VALUE);
   }

   public static FloatArgumentType floatArg(float min) {
      return floatArg(min, Float.MAX_VALUE);
   }

   public static FloatArgumentType floatArg(float min, float max) {
      return new FloatArgumentType(min, max);
   }

   public static float getFloat(CommandContext<?> context, String name) {
      return context.getArgument(name, Float.class);
   }

   public float getMinimum() {
      return this.minimum;
   }

   public float getMaximum() {
      return this.maximum;
   }

   public Float parse(StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();
      float result = reader.readFloat();
      if (result < this.minimum) {
         reader.setCursor(start);
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow().createWithContext(reader, result, this.minimum);
      } else if (result > this.maximum) {
         reader.setCursor(start);
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().createWithContext(reader, result, this.maximum);
      } else {
         return result;
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof FloatArgumentType)) {
         return false;
      } else {
         FloatArgumentType that = (FloatArgumentType)o;
         return this.maximum == that.maximum && this.minimum == that.minimum;
      }
   }

   @Override
   public int hashCode() {
      return (int)(31.0F * this.minimum + this.maximum);
   }

   @Override
   public String toString() {
      if (this.minimum == -Float.MAX_VALUE && this.maximum == Float.MAX_VALUE) {
         return "float()";
      } else {
         return this.maximum == Float.MAX_VALUE ? "float(" + this.minimum + ")" : "float(" + this.minimum + ", " + this.maximum + ")";
      }
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
// spotless:on
