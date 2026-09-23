// spotless:off
package com.xyp.gtnotgood.commandtree.command.serialization;

import com.google.common.collect.Maps;
import com.xyp.gtnotgood.commandtree.command.serialization.serializers.DoubleArgumentSerializer;
import com.xyp.gtnotgood.commandtree.command.serialization.serializers.EmptyArgumentSerializer;
import com.xyp.gtnotgood.commandtree.command.serialization.serializers.FloatArgumentSerializer;
import com.xyp.gtnotgood.commandtree.command.serialization.serializers.IntegerArgumentSerializer;
import com.xyp.gtnotgood.commandtree.command.serialization.serializers.LongArgumentSerializer;
import com.xyp.gtnotgood.commandtree.command.serialization.serializers.StringArgumentSerializer;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.ArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.BoolArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.DoubleArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.FloatArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.IntegerArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.LongArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.StringArgumentType;
import java.util.Map;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Ported command-tree ArgumentTypes used by the integrated chat suggestions. */
public class ArgumentTypes {
   private static final Map<Class<?>, ArgumentTypes.Entry<?>> BY_CLASS = Maps.newHashMap();
   private static final Map<ResourceLocation, ArgumentTypes.Entry<?>> BY_NAME = Maps.newHashMap();

   public static <T extends ArgumentType<?>> void register(String pName, Class<T> pClazz, ArgumentSerializer<T> pSerializer) {
      ResourceLocation resourcelocation = new ResourceLocation(pName);
      if (BY_CLASS.containsKey(pClazz)) {
         throw new IllegalArgumentException("Class " + pClazz.getName() + " already has a serializer!");
      } else if (BY_NAME.containsKey(resourcelocation)) {
         throw new IllegalArgumentException("'" + resourcelocation + "' is already a registered serializer!");
      } else {
         ArgumentTypes.Entry<T> entry = new ArgumentTypes.Entry<T>(pClazz, pSerializer, resourcelocation);
         BY_CLASS.put(pClazz, entry);
         BY_NAME.put(resourcelocation, entry);
      }
   }

   public static void init() {
      register("brigadier:bool", BoolArgumentType.class, new EmptyArgumentSerializer<BoolArgumentType>(BoolArgumentType::bool));
      register("brigadier:float", FloatArgumentType.class, new FloatArgumentSerializer());
      register("brigadier:double", DoubleArgumentType.class, new DoubleArgumentSerializer());
      register("brigadier:integer", IntegerArgumentType.class, new IntegerArgumentSerializer());
      register("brigadier:long", LongArgumentType.class, new LongArgumentSerializer());
      register("brigadier:string", StringArgumentType.class, new StringArgumentSerializer());
   }

   @Nullable
   private static ArgumentTypes.Entry<?> get(ResourceLocation pType) {
      return BY_NAME.get(pType);
   }

   @Nullable
   private static ArgumentTypes.Entry<?> get(ArgumentType<?> pType) {
      return BY_CLASS.get(pType.getClass());
   }

   @SuppressWarnings("unchecked")
   public static <T extends ArgumentType<?>> void serialize(PacketBuffer pBuffer, T pType) throws java.io.IOException {
      ArgumentTypes.Entry<T> entry = (ArgumentTypes.Entry<T>)get(pType);
      if (entry == null) {
         writeResourceLocation(pBuffer, new ResourceLocation(""));
      } else {
         writeResourceLocation(pBuffer, entry.name);
         entry.serializer.serializeToNetwork(pType, pBuffer);
      }
   }

   @Nullable
   public static ArgumentType<?> deserialize(PacketBuffer pBuffer) throws java.io.IOException {
      ResourceLocation resourcelocation = readResourceLocation(pBuffer);
      ArgumentTypes.Entry<?> entry = get(resourcelocation);
      return entry == null ? null : entry.serializer.deserializeFromNetwork(pBuffer);
   }

   public static void writeResourceLocation(PacketBuffer buffer, ResourceLocation loc) throws java.io.IOException {
      buffer.writeStringToBuffer(loc.toString());
   }

   public static ResourceLocation readResourceLocation(PacketBuffer buffer) throws java.io.IOException {
      return new ResourceLocation(buffer.readStringFromBuffer(32767));
   }

   static class Entry<T extends ArgumentType<?>> {
      public final Class<T> clazz;
      public final ArgumentSerializer<T> serializer;
      public final ResourceLocation name;

      Entry(Class<T> pClazz, ArgumentSerializer<T> pSerializer, ResourceLocation pName) {
         this.clazz = pClazz;
         this.serializer = pSerializer;
         this.name = pName;
      }
   }
}
// spotless:on
