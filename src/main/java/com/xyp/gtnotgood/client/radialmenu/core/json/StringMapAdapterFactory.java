package com.xyp.gtnotgood.client.radialmenu.core.json;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class StringMapAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!Map.class.isAssignableFrom(type.getRawType()) || !(type.getType() instanceof ParameterizedType)) {
            return null;
        }
        Type[] arguments = ((ParameterizedType) type.getType()).getActualTypeArguments();
        if (arguments.length != 2 || arguments[0] != String.class || arguments[1] != String.class) {
            return null;
        }
        return (TypeAdapter<T>) new Adapter();
    }

    private static final class Adapter extends TypeAdapter<Map<String, String>> {

        @Override
        public void write(JsonWriter out, Map<String, String> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            for (Map.Entry<String, String> entry : value.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                out.name(entry.getKey());
                if (entry.getValue().indexOf('\n') < 0) {
                    out.value(entry.getValue());
                } else {
                    out.beginArray();
                    for (String line : entry.getValue().split("\n", -1)) {
                        out.value(line);
                    }
                    out.endArray();
                }
            }
            out.endObject();
        }

        @Override
        public Map<String, String> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            Map<String, String> values = new LinkedHashMap<>();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                String value = readValue(in);
                if (value != null) {
                    values.put(name, value);
                }
            }
            in.endObject();
            return values;
        }

        private String readValue(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            if (in.peek() == JsonToken.BOOLEAN) {
                return Boolean.toString(in.nextBoolean());
            }
            if (in.peek() != JsonToken.BEGIN_ARRAY) {
                return in.nextString();
            }

            StringBuilder joined = new StringBuilder();
            in.beginArray();
            boolean first = true;
            while (in.hasNext()) {
                if (!first) {
                    joined.append('\n');
                }
                first = false;
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                } else if (in.peek() == JsonToken.BOOLEAN) {
                    joined.append(in.nextBoolean());
                } else {
                    joined.append(in.nextString());
                }
            }
            in.endArray();
            return joined.toString();
        }
    }
}
