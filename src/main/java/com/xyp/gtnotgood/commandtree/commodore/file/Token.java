// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

import lombok.Getter;

/** Relocated Commodore Token used to parse bundled command definitions. */
public interface Token {
   public static enum ConstantToken implements Token {
      OPEN_BRACKET,
      CLOSE_BRACKET,
      SEMICOLON,
      EOF;
   }

   public static final class StringToken implements Token {
      @Getter
      private final String string;

      StringToken(String string) {
         this.string = string;
      }

   }
}
// spotless:on
