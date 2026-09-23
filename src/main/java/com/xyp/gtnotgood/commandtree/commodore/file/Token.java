// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

/** Relocated Commodore Token used to parse bundled command definitions. */
public interface Token {
   public static enum ConstantToken implements Token {
      OPEN_BRACKET,
      CLOSE_BRACKET,
      SEMICOLON,
      EOF;
   }

   public static final class StringToken implements Token {
      private final String string;

      StringToken(String string) {
         this.string = string;
      }

      public String getString() {
         return this.string;
      }
   }
}
// spotless:on
