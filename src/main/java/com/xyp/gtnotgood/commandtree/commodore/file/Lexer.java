// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

/** Relocated Commodore Lexer used to parse bundled command definitions. */
class Lexer extends AbstractIterator<Token> implements TokenStream {
   private final StreamTokenizer tokenizer;
   private boolean end = false;

   Lexer(Reader reader) {
      this.tokenizer = new StreamTokenizer(reader);
      this.tokenizer.resetSyntax();
      this.tokenizer.wordChars(33, 126);
      this.tokenizer.quoteChar(34);
      this.tokenizer.whitespaceChars(0, 32);
      "{};".chars().forEach(this.tokenizer::ordinaryChar);
      this.tokenizer.slashSlashComments(true);
      this.tokenizer.slashStarComments(true);
   }

   protected Token computeNext() throws ParseException {
      if (this.end) {
         return this.endOfData();
      } else {
         try {
            int token = this.tokenizer.nextToken();
            switch (token) {
               case -3:
                  return new Token.StringToken(this.tokenizer.sval);
               case -1:
                  this.end = true;
                  return Token.ConstantToken.EOF;
               case 59:
                  return Token.ConstantToken.SEMICOLON;
               case 123:
                  return Token.ConstantToken.OPEN_BRACKET;
               case 125:
                  return Token.ConstantToken.CLOSE_BRACKET;
               default:
                  throw this.createException("Unknown token: " + (char)token + "(" + token + ")");
            }
         } catch (IOException var2) {
            throw this.createException(var2);
         }
      }
   }

   @Override
   public ParseException createException(String message) {
      return new ParseException(message, this.tokenizer.lineno());
   }

   @Override
   public ParseException createException(Throwable cause) {
      return new ParseException(cause, this.tokenizer.lineno());
   }

   @Override
   public ParseException createException(String message, Throwable cause) {
      return new ParseException(message, cause, this.tokenizer.lineno());
   }
}
// spotless:on
