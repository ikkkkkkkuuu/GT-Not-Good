// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.ArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.BoolArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.DoubleArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.FloatArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.IntegerArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.LongArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.StringArgumentType;

/** Relocated Commodore BrigadierArgumentTypeParser used to parse bundled command definitions. */
public class BrigadierArgumentTypeParser implements ArgumentTypeParser {
   public static final BrigadierArgumentTypeParser INSTANCE = new BrigadierArgumentTypeParser();

   private BrigadierArgumentTypeParser() {
   }

   @Override
   public boolean canParse(String namespace, String name) {
      if (!namespace.equals("brigadier")) {
         return false;
      } else {
         switch (name) {
            case "bool":
            case "string":
            case "integer":
            case "long":
            case "float":
            case "double":
               return true;
            default:
               return false;
         }
      }
   }

   @Override
   public ArgumentType<?> parse(String namespace, String name, TokenStream tokens) throws ParseException {
      switch (name) {
         case "bool":
            return BoolArgumentType.bool();
         case "string":
            return parseStringArgumentType(tokens);
         case "integer":
            return parseIntegerArgumentType(tokens);
         case "long":
            return parseLongArgumentType(tokens);
         case "float":
            return parseFloatArgumentType(tokens);
         case "double":
            return parseDoubleArgumentType(tokens);
         default:
            throw new AssertionError();
      }
   }

   private static StringArgumentType parseStringArgumentType(TokenStream tokens) throws ParseException {
      Token token = tokens.next();
      if (!(token instanceof Token.StringToken)) {
         throw tokens.createException("Expected string token for string type but got " + token);
      } else {
         String stringType = ((Token.StringToken)token).getString();
         switch (stringType) {
            case "single_word":
               return StringArgumentType.word();
            case "quotable_phrase":
               return StringArgumentType.string();
            case "greedy_phrase":
               return StringArgumentType.greedyString();
            default:
               throw tokens.createException("Unknown string type: " + stringType);
         }
      }
   }

   private static IntegerArgumentType parseIntegerArgumentType(TokenStream tokens) throws ParseException {
      if (tokens.peek() instanceof Token.StringToken) {
         int min = parseInt(tokens);
         if (tokens.peek() instanceof Token.StringToken) {
            int max = parseInt(tokens);
            return IntegerArgumentType.integer(min, max);
         } else {
            return IntegerArgumentType.integer(min);
         }
      } else {
         return IntegerArgumentType.integer();
      }
   }

   private static LongArgumentType parseLongArgumentType(TokenStream tokens) throws ParseException {
      if (tokens.peek() instanceof Token.StringToken) {
         long min = parseLong(tokens);
         if (tokens.peek() instanceof Token.StringToken) {
            long max = parseLong(tokens);
            return LongArgumentType.longArg(min, max);
         } else {
            return LongArgumentType.longArg(min);
         }
      } else {
         return LongArgumentType.longArg();
      }
   }

   private static FloatArgumentType parseFloatArgumentType(TokenStream tokens) throws ParseException {
      if (tokens.peek() instanceof Token.StringToken) {
         float min = parseFloat(tokens);
         if (tokens.peek() instanceof Token.StringToken) {
            float max = parseFloat(tokens);
            return FloatArgumentType.floatArg(min, max);
         } else {
            return FloatArgumentType.floatArg(min);
         }
      } else {
         return FloatArgumentType.floatArg();
      }
   }

   private static DoubleArgumentType parseDoubleArgumentType(TokenStream tokens) throws ParseException {
      if (tokens.peek() instanceof Token.StringToken) {
         double min = parseDouble(tokens);
         if (tokens.peek() instanceof Token.StringToken) {
            double max = parseDouble(tokens);
            return DoubleArgumentType.doubleArg(min, max);
         } else {
            return DoubleArgumentType.doubleArg(min);
         }
      } else {
         return DoubleArgumentType.doubleArg();
      }
   }

   private static int parseInt(TokenStream tokens) throws ParseException {
      Token token = tokens.next();
      if (!(token instanceof Token.StringToken)) {
         throw tokens.createException("Expected string token for integer but got " + token);
      } else {
         String value = ((Token.StringToken)token).getString();
         if (value.equals("min")) {
            return Integer.MIN_VALUE;
         } else if (value.equals("max")) {
            return Integer.MAX_VALUE;
         } else {
            try {
               return Integer.parseInt(value);
            } catch (NumberFormatException var4) {
               throw tokens.createException("Expected int but got " + value, var4);
            }
         }
      }
   }

   private static long parseLong(TokenStream tokens) throws ParseException {
      Token token = tokens.next();
      if (!(token instanceof Token.StringToken)) {
         throw tokens.createException("Expected string token for long but got " + token);
      } else {
         String value = ((Token.StringToken)token).getString();
         if (value.equals("min")) {
            return Long.MIN_VALUE;
         } else if (value.equals("max")) {
            return Long.MAX_VALUE;
         } else {
            try {
               return Long.parseLong(value);
            } catch (NumberFormatException var4) {
               throw tokens.createException("Expected long but got " + value, var4);
            }
         }
      }
   }

   private static float parseFloat(TokenStream tokens) throws ParseException {
      Token token = tokens.next();
      if (!(token instanceof Token.StringToken)) {
         throw tokens.createException("Expected string token for float but got " + token);
      } else {
         String value = ((Token.StringToken)token).getString();
         if (value.equals("min")) {
            return Float.MIN_VALUE;
         } else if (value.equals("max")) {
            return Float.MAX_VALUE;
         } else {
            try {
               return Float.parseFloat(value);
            } catch (NumberFormatException var4) {
               throw tokens.createException("Expected float but got " + value, var4);
            }
         }
      }
   }

   private static double parseDouble(TokenStream tokens) throws ParseException {
      Token token = tokens.next();
      if (!(token instanceof Token.StringToken)) {
         throw tokens.createException("Expected string token for double but got " + token);
      } else {
         String value = ((Token.StringToken)token).getString();
         if (value.equals("min")) {
            return Double.MIN_VALUE;
         } else if (value.equals("max")) {
            return Double.MAX_VALUE;
         } else {
            try {
               return Double.parseDouble(value);
            } catch (NumberFormatException var4) {
               throw tokens.createException("Expected double but got " + value);
            }
         }
      }
   }
}
// spotless:on
