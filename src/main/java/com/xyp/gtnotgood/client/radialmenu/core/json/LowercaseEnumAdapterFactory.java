package com.xyp.gtnotgood.client.radialmenu.core.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class LowercaseEnumAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();
        if (!Enum.class.isAssignableFrom(raw) || raw == Enum.class) {
            return null;
        }

        if (!raw.isEnum()) {
            raw = raw.getSuperclass();
        }
        return (TypeAdapter<T>) new LowercaseEnumAdapter<>((Class<? extends Enum>) raw);
    }

    private static final class LowercaseEnumAdapter<E extends Enum<E>> extends TypeAdapter<E> {

        private final Map<String, E> byLowercaseName = new HashMap<>();

        LowercaseEnumAdapter(Class<E> enumType) {
            for (E constant : enumType.getEnumConstants()) {
                byLowercaseName.put(externalName(constant), constant);
                byLowercaseName.put(constant.name().toLowerCase(Locale.ROOT), constant);
            }
        }

        @Override
        public void write(JsonWriter out, E value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(externalName(value));
            }
        }

        @Override
        public E read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return byLowercaseName.get(in.nextString().trim().toLowerCase(Locale.ROOT));
        }
    }

    /** Keep upstream snake_case values when Java enum constants use UpperCamelCase. */
    public static String externalName(Enum<?> value) {
        return value.name().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
