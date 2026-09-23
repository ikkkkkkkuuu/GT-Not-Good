// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContextBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.SuggestionContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.LiteralCommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.RootCommandNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Relocated Brigadier CommandDispatcher used by the command-tree parser. */
public class CommandDispatcher<S> {
   public static final String ARGUMENT_SEPARATOR = " ";
   public static final char ARGUMENT_SEPARATOR_CHAR = ' ';
   private static final String USAGE_OPTIONAL_OPEN = "[";
   private static final String USAGE_OPTIONAL_CLOSE = "]";
   private static final String USAGE_REQUIRED_OPEN = "(";
   private static final String USAGE_REQUIRED_CLOSE = ")";
   private static final String USAGE_OR = "|";
   private final RootCommandNode<S> root;
   private final Predicate<CommandNode<S>> hasCommand = new Predicate<CommandNode<S>>() {
      public boolean test(CommandNode<S> input) {
         return input != null && (input.getCommand() != null || input.getChildren().stream().anyMatch(CommandDispatcher.this.hasCommand));
      }
   };
   private ResultConsumer<S> consumer = (c, s, r) -> {};

   public CommandDispatcher(RootCommandNode<S> root) {
      this.root = root;
   }

   public CommandDispatcher() {
      this(new RootCommandNode<>());
   }

   public LiteralCommandNode<S> register(LiteralArgumentBuilder<S> command) {
      LiteralCommandNode<S> build = command.build();
      this.root.addChild(build);
      return build;
   }

   public void setConsumer(ResultConsumer<S> consumer) {
      this.consumer = consumer;
   }

   public int execute(String input, S source) throws CommandSyntaxException {
      return this.execute(new StringReader(input), source);
   }

   public int execute(StringReader input, S source) throws CommandSyntaxException {
      ParseResults<S> parse = this.parse(input, source);
      return this.execute(parse);
   }

   public int execute(ParseResults<S> parse) throws CommandSyntaxException {
      if (parse.getReader().canRead()) {
         if (parse.getExceptions().size() == 1) {
            throw (CommandSyntaxException)parse.getExceptions().values().iterator().next();
         } else if (parse.getContext().getRange().isEmpty()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader());
         } else {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parse.getReader());
         }
      } else {
         int result = 0;
         int successfulForks = 0;
         boolean forked = false;
         boolean foundCommand = false;
         String command = parse.getReader().getString();
         CommandContext<S> original = parse.getContext().build(command);
         List<CommandContext<S>> contexts = Collections.singletonList(original);

         for (ArrayList<CommandContext<S>> next = null; contexts != null; next = null) {
            int size = contexts.size();

            for (int i = 0; i < size; i++) {
               CommandContext<S> context = contexts.get(i);
               CommandContext<S> child = context.getChild();
               if (child != null) {
                  forked |= context.isForked();
                  if (child.hasNodes()) {
                     foundCommand = true;
                     RedirectModifier<S> modifier = context.getRedirectModifier();
                     if (modifier == null) {
                        if (next == null) {
                           next = new ArrayList<>(1);
                        }

                        next.add(child.copyFor(context.getSource()));
                     } else {
                        try {
                           Collection<S> results = modifier.apply(context);
                           if (!results.isEmpty()) {
                              if (next == null) {
                                 next = new ArrayList<>(results.size());
                              }

                              for (S source : results) {
                                 next.add(child.copyFor(source));
                              }
                           }
                        } catch (CommandSyntaxException var18) {
                           this.consumer.onCommandComplete(context, false, 0);
                           if (!forked) {
                              throw var18;
                           }
                        }
                     }
                  }
               } else if (context.getCommand() != null) {
                  foundCommand = true;

                  try {
                     int value = context.getCommand().run(context);
                     result += value;
                     this.consumer.onCommandComplete(context, true, value);
                     successfulForks++;
                  } catch (CommandSyntaxException var19) {
                     this.consumer.onCommandComplete(context, false, 0);
                     if (!forked) {
                        throw var19;
                     }
                  }
               }
            }

            contexts = next;
         }

         if (!foundCommand) {
            this.consumer.onCommandComplete(original, false, 0);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader());
         } else {
            return forked ? successfulForks : result;
         }
      }
   }

   public ParseResults<S> parse(String command, S source) {
      return this.parse(new StringReader(command), source);
   }

   public ParseResults<S> parse(StringReader command, S source) {
      CommandContextBuilder<S> context = new CommandContextBuilder<>(this, source, this.root, command.getCursor());
      return this.parseNodes(this.root, command, context);
   }

   private ParseResults<S> parseNodes(CommandNode<S> node, StringReader originalReader, CommandContextBuilder<S> contextSoFar) {
      S source = contextSoFar.getSource();
      Map<CommandNode<S>, CommandSyntaxException> errors = null;
      List<ParseResults<S>> potentials = null;
      int cursor = originalReader.getCursor();
      Iterator var8 = node.getRelevantNodes(originalReader).iterator();

      while (true) {
         CommandNode<S> child;
         CommandContextBuilder<S> context;
         StringReader reader;
         while (true) {
            if (!var8.hasNext()) {
               if (potentials != null) {
                  if (potentials.size() > 1) {
                     potentials.sort((a, b) -> {
                        if (!a.getReader().canRead() && b.getReader().canRead()) {
                           return -1;
                        } else if (a.getReader().canRead() && !b.getReader().canRead()) {
                           return 1;
                        } else if (a.getExceptions().isEmpty() && !b.getExceptions().isEmpty()) {
                           return -1;
                        } else {
                           return !a.getExceptions().isEmpty() && b.getExceptions().isEmpty() ? 1 : 0;
                        }
                     });
                  }

                  return potentials.get(0);
               }

               return new ParseResults<>(contextSoFar, originalReader, errors == null ? Collections.emptyMap() : errors);
            }

            child = (CommandNode<S>)var8.next();
            if (child.canUse(source)) {
               context = contextSoFar.copy();
               reader = new StringReader(originalReader);

               try {
                  try {
                     child.parse(reader, context);
                  } catch (RuntimeException var14) {
                     throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, var14.getMessage());
                  }

                  if (reader.canRead() && reader.peek() != ' ') {
                     throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherExpectedArgumentSeparator().createWithContext(reader);
                  }
                  break;
               } catch (CommandSyntaxException var15) {
                  if (errors == null) {
                     errors = new LinkedHashMap<>();
                  }

                  errors.put(child, var15);
                  reader.setCursor(cursor);
               }
            }
         }

         context.withCommand(child.getCommand());
         if (reader.canRead(child.getRedirect() == null ? 2 : 1)) {
            reader.skip();
            if (child.getRedirect() != null) {
               CommandContextBuilder<S> childContext = new CommandContextBuilder<>(this, source, child.getRedirect(), reader.getCursor());
               ParseResults<S> parse = this.parseNodes(child.getRedirect(), reader, childContext);
               context.withChild(parse.getContext());
               return new ParseResults<>(context, parse.getReader(), parse.getExceptions());
            }

            ParseResults<S> parse = this.parseNodes(child, reader, context);
            if (potentials == null) {
               potentials = new ArrayList<>(1);
            }

            potentials.add(parse);
         } else {
            if (potentials == null) {
               potentials = new ArrayList<>(1);
            }

            potentials.add(new ParseResults<>(context, reader, Collections.emptyMap()));
         }
      }
   }

   public String[] getAllUsage(CommandNode<S> node, S source, boolean restricted) {
      ArrayList<String> result = new ArrayList<>();
      this.getAllUsage(node, source, result, "", restricted);
      return result.toArray(new String[result.size()]);
   }

   private void getAllUsage(CommandNode<S> node, S source, ArrayList<String> result, String prefix, boolean restricted) {
      if (!restricted || node.canUse(source)) {
         if (node.getCommand() != null) {
            result.add(prefix);
         }

         if (node.getRedirect() != null) {
            String redirect = node.getRedirect() == this.root ? "..." : "-> " + node.getRedirect().getUsageText();
            result.add(prefix.isEmpty() ? node.getUsageText() + " " + redirect : prefix + " " + redirect);
         } else if (!node.getChildren().isEmpty()) {
            for (CommandNode<S> child : node.getChildren()) {
               this.getAllUsage(child, source, result, prefix.isEmpty() ? child.getUsageText() : prefix + " " + child.getUsageText(), restricted);
            }
         }
      }
   }

   public Map<CommandNode<S>, String> getSmartUsage(CommandNode<S> node, S source) {
      Map<CommandNode<S>, String> result = new LinkedHashMap<>();
      boolean optional = node.getCommand() != null;

      for (CommandNode<S> child : node.getChildren()) {
         String usage = this.getSmartUsage(child, source, optional, false);
         if (usage != null) {
            result.put(child, usage);
         }
      }

      return result;
   }

   private String getSmartUsage(CommandNode<S> node, S source, boolean optional, boolean deep) {
      if (!node.canUse(source)) {
         return null;
      } else {
         String self = optional ? "[" + node.getUsageText() + "]" : node.getUsageText();
         boolean childOptional = node.getCommand() != null;
         String open = childOptional ? "[" : "(";
         String close = childOptional ? "]" : ")";
         if (!deep) {
            if (node.getRedirect() != null) {
               String redirect = node.getRedirect() == this.root ? "..." : "-> " + node.getRedirect().getUsageText();
               return self + " " + redirect;
            }

            Collection<CommandNode<S>> children = node.getChildren().stream().filter(c -> c.canUse(source)).collect(Collectors.toList());
            if (children.size() == 1) {
               String usage = this.getSmartUsage(children.iterator().next(), source, childOptional, childOptional);
               if (usage != null) {
                  return self + " " + usage;
               }
            } else if (children.size() > 1) {
               Set<String> childUsage = new LinkedHashSet<>();

               for (CommandNode<S> child : children) {
                  String usage = this.getSmartUsage(child, source, childOptional, true);
                  if (usage != null) {
                     childUsage.add(usage);
                  }
               }

               if (childUsage.size() == 1) {
                  String usage = childUsage.iterator().next();
                  return self + " " + (childOptional ? "[" + usage + "]" : usage);
               }

               if (childUsage.size() > 1) {
                  StringBuilder builder = new StringBuilder(open);
                  int count = 0;

                  for (CommandNode<S> childx : children) {
                     if (count > 0) {
                        builder.append("|");
                     }

                     builder.append(childx.getUsageText());
                     count++;
                  }

                  if (count > 0) {
                     builder.append(close);
                     return self + " " + builder.toString();
                  }
               }
            }
         }

         return self;
      }
   }

   public CompletableFuture<Suggestions> getCompletionSuggestions(ParseResults<S> parse) {
      return this.getCompletionSuggestions(parse, parse.getReader().getTotalLength());
   }

   public CompletableFuture<Suggestions> getCompletionSuggestions(ParseResults<S> parse, int cursor) {
      CommandContextBuilder<S> context = parse.getContext();
      SuggestionContext<S> nodeBeforeCursor = context.findSuggestionContext(cursor);
      CommandNode<S> parent = nodeBeforeCursor.parent;
      int start = Math.min(nodeBeforeCursor.startPos, cursor);
      String fullInput = parse.getReader().getString();
      String truncatedInput = fullInput.substring(0, cursor);
      String truncatedInputLowerCase = truncatedInput.toLowerCase(Locale.ROOT);
      CompletableFuture<Suggestions>[] futures = new CompletableFuture[parent.getChildren().size()];
      int i = 0;

      for (CommandNode<S> node : parent.getChildren()) {
         CompletableFuture<Suggestions> future = Suggestions.empty();

         try {
            future = node.listSuggestions(context.build(truncatedInput), new SuggestionsBuilder(truncatedInput, truncatedInputLowerCase, start));
         } catch (CommandSyntaxException var16) {
         }

         futures[i++] = future;
      }

      CompletableFuture<Suggestions> result = new CompletableFuture<>();
      CompletableFuture.allOf(futures).thenRun(() -> {
         List<Suggestions> suggestions = new ArrayList<>();

         for (CompletableFuture<Suggestions> futurex : futures) {
            suggestions.add(futurex.join());
         }

         result.complete(Suggestions.merge(fullInput, suggestions));
      });
      return result;
   }

   public RootCommandNode<S> getRoot() {
      return this.root;
   }

   public Collection<String> getPath(CommandNode<S> target) {
      List<List<CommandNode<S>>> nodes = new ArrayList<>();
      this.addPaths(this.root, nodes, new ArrayList<>());

      for (List<CommandNode<S>> list : nodes) {
         if (list.get(list.size() - 1) == target) {
            List<String> result = new ArrayList<>(list.size());

            for (CommandNode<S> node : list) {
               if (node != this.root) {
                  result.add(node.getName());
               }
            }

            return result;
         }
      }

      return Collections.emptyList();
   }

   public CommandNode<S> findNode(Collection<String> path) {
      CommandNode<S> node = this.root;

      for (String name : path) {
         node = node.getChild(name);
         if (node == null) {
            return null;
         }
      }

      return node;
   }

   public void findAmbiguities(AmbiguityConsumer<S> consumer) {
      this.root.findAmbiguities(consumer);
   }

   private void addPaths(CommandNode<S> node, List<List<CommandNode<S>>> result, List<CommandNode<S>> parents) {
      List<CommandNode<S>> current = new ArrayList<>(parents);
      current.add(node);
      result.add(current);

      for (CommandNode<S> child : node.getChildren()) {
         this.addPaths(child, result, current);
      }
   }
}
// spotless:on
