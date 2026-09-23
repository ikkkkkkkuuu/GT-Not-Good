// spotless:off
package com.xyp.gtnotgood.commandtree.compat;

import com.google.common.collect.Maps;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.commandtree.command.CommandSource;
import com.xyp.gtnotgood.commandtree.compat.mods.BaublesMod;
import com.xyp.gtnotgood.commandtree.compat.mods.MinecraftMod;
import com.xyp.gtnotgood.commandtree.compat.mods.ThaumcraftMod;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.LiteralCommandNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;
import com.xyp.gtnotgood.commandtree.commodore.file.CommodoreFileReader;
import org.jetbrains.annotations.NotNull;

/** Loads bundled syntax definitions for commands supplied by installed mods. */
public class CompatRegistry {
   private static final Map<String, LiteralCommandNode<CommandSource>> REGISTERED_COMMANDS = Maps.newHashMap();

   public static void init() {
      register("minecraft", MinecraftMod::new);
      register(ModList.Baubles.getID(), BaublesMod::new);
      register(ModList.Thaumcraft.getID(), ThaumcraftMod::new);
   }

   public static boolean hasCompatFor(Class<?> clazz) {
      return REGISTERED_COMMANDS.containsKey(clazz.getName());
   }

   public static LiteralCommandNode<CommandSource> getCompatCommand(Class<?> clazz) {
      return REGISTERED_COMMANDS.get(clazz.getName());
   }

   private static InputStream getResource(String path) {
      return CompatRegistry.class.getResourceAsStream("/assets/" + ModList.GTNotGood.getID() + "/commandtree/commands/" + path);
   }

   private static void register(@NotNull String modId, @NotNull Supplier<CompatMod> lazyMod) {
      if (modId.equals("minecraft") || cpw.mods.fml.common.Loader.isModLoaded(modId)) {
         GTNotGood.LOG.info("Loading compatibility for mod: {}", modId);
         CompatMod mod = lazyMod.get();
         mod.commands().forEach(clazz -> {
            GTNotGood.LOG.info("Registering compatibility command: {}", clazz);

            try {
               REGISTERED_COMMANDS.put(clazz, CommodoreFileReader.INSTANCE.parse(getResource(mod.identifier() + "/" + clazz + ".commodore")));
               GTNotGood.LOG.info("Registered compatibility command: {}", clazz);
            } catch (IOException var3) {
               GTNotGood.LOG.error("Failed to load compatibility for {} in mod {}", clazz, mod.identifier(), var3);
            }
         });
      }
   }
}
// spotless:on
