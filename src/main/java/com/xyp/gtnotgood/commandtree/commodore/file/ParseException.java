// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

/** Relocated Commodore ParseException used to parse bundled command definitions. */
public final class ParseException extends Exception {
   ParseException(String message, int currentLine) {
      super(message + " (at line " + currentLine + ")");
   }

   ParseException(Throwable cause, int currentLine) {
      super("At line " + currentLine, cause);
   }

   ParseException(String message, Throwable cause, int currentLine) {
      super(message + " (at line " + currentLine + ")", cause);
   }
}
// spotless:on
