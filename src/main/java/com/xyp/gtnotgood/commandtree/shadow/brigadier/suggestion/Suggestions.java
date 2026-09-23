// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.StringRange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Relocated Brigadier Suggestions used by the command-tree parser. */
public class Suggestions {
   private static final Suggestions EMPTY = new Suggestions(StringRange.at(0), new ArrayList<>());
   private final StringRange range;
   private final List<Suggestion> suggestions;

   public Suggestions(StringRange range, List<Suggestion> suggestions) {
      this.range = range;
      this.suggestions = suggestions;
   }

   public StringRange getRange() {
      return this.range;
   }

   public List<Suggestion> getList() {
      return this.suggestions;
   }

   public boolean isEmpty() {
      return this.suggestions.isEmpty();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof Suggestions)) {
         return false;
      } else {
         Suggestions that = (Suggestions)o;
         return Objects.equals(this.range, that.range) && Objects.equals(this.suggestions, that.suggestions);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.range, this.suggestions);
   }

   @Override
   public String toString() {
      return "Suggestions{range=" + this.range + ", suggestions=" + this.suggestions + '}';
   }

   public static CompletableFuture<Suggestions> empty() {
      return CompletableFuture.completedFuture(EMPTY);
   }

   public static Suggestions merge(String command, Collection<Suggestions> input) {
      if (input.isEmpty()) {
         return EMPTY;
      } else if (input.size() == 1) {
         return input.iterator().next();
      } else {
         Set<Suggestion> texts = new HashSet<>();

         for (Suggestions suggestions : input) {
            texts.addAll(suggestions.getList());
         }

         return create(command, texts);
      }
   }

   public static Suggestions create(String command, Collection<Suggestion> suggestions) {
      if (suggestions.isEmpty()) {
         return EMPTY;
      } else {
         int start = Integer.MAX_VALUE;
         int end = Integer.MIN_VALUE;

         for (Suggestion suggestion : suggestions) {
            start = Math.min(suggestion.getRange().getStart(), start);
            end = Math.max(suggestion.getRange().getEnd(), end);
         }

         StringRange range = new StringRange(start, end);
         Set<Suggestion> texts = new HashSet<>();

         for (Suggestion suggestion : suggestions) {
            texts.add(suggestion.expand(command, range));
         }

         List<Suggestion> sorted = new ArrayList<>(texts);
         sorted.sort((a, b) -> a.compareToIgnoreCase(b));
         return new Suggestions(range, sorted);
      }
   }
}
// spotless:on
