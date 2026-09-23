// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.LiteralCommandNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Relocated Commodore CommodoreFileReader used to parse bundled command definitions. */
public class CommodoreFileReader {
   public static final CommodoreFileReader INSTANCE = builder().withArgumentTypeParser(BrigadierArgumentTypeParser.INSTANCE).build();
   private final List<ArgumentTypeParser> argumentTypeParsers;

   public static CommodoreFileReader.Builder builder() {
      return new CommodoreFileReader.Builder();
   }

   CommodoreFileReader(List<ArgumentTypeParser> argumentTypeParsers) {
      this.argumentTypeParsers = Collections.unmodifiableList(argumentTypeParsers);
   }

   public <S> LiteralCommandNode<S> parse(Reader reader) throws IOException {
      try {
         return new Parser<S>(new Lexer(reader), this.argumentTypeParsers).parse();
      } catch (ParseException var3) {
         if (var3.getCause() instanceof IOException) {
            throw (IOException)var3.getCause();
         } else {
            throw new IOException(var3);
         }
      }
   }

   public <S> LiteralCommandNode<S> parse(InputStream inputStream) throws IOException {
      LiteralCommandNode var4;
      try (InputStreamReader reader = new InputStreamReader(inputStream)) {
         var4 = this.parse(reader);
      }

      return var4;
   }

   public <S> LiteralCommandNode<S> parse(Path path) throws IOException {
      LiteralCommandNode var4;
      try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         var4 = this.parse(reader);
      }

      return var4;
   }

   public <S> LiteralCommandNode<S> parse(File file) throws IOException {
      return this.parse(file.toPath());
   }

   public static final class Builder {
      private final List<ArgumentTypeParser> argumentTypeParsers = new ArrayList<>();

      Builder() {
      }

      public CommodoreFileReader.Builder withArgumentTypeParser(ArgumentTypeParser argumentTypeParser) {
         Objects.requireNonNull(argumentTypeParser, "argumentTypeParser");
         this.argumentTypeParsers.add(argumentTypeParser);
         return this;
      }

      public CommodoreFileReader build() {
         return new CommodoreFileReader(new ArrayList<>(this.argumentTypeParsers));
      }
   }
}
// spotless:on
