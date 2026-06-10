package com.terraforged.mod.data.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.function.Predicate;

public class JsonFormatter {
    private static final Gson GSON = new Gson();
    private static final String INDENT = "  ";
    private static final String COMPACT = "";
    private final Writer writer;
    private final JsonWriter jsonWriter;

    public JsonFormatter(Writer writer) {
        this.writer = writer;
        this.jsonWriter = new JsonWriter(writer);
        this.jsonWriter.setIndent(INDENT);
    }

    public void write(JsonElement json) throws IOException {
        if (json.isJsonObject()) {
            this.writeObject(json.getAsJsonObject());
        } else if (json.isJsonArray()) {
            this.writeArray(json.getAsJsonArray());
        } else if (json.isJsonPrimitive()) {
            this.writePrimitive(json.getAsJsonPrimitive(), false);
        }
    }

    private void writeObject(JsonObject object) throws IOException {
        String[] keys = JsonFormatter.getKeys(object);
        this.jsonWriter.beginObject();
        this.writeEntries(keys, object, JsonFormatter::isString);
        this.writeEntries(keys, object, JsonFormatter::isPrimitive);
        this.writeEntries(keys, object, JsonElement::isJsonArray);
        this.writeEntries(keys, object, JsonElement::isJsonObject);
        this.jsonWriter.endObject();
    }

    private void writeEntries(String[] keys, JsonObject object, Predicate<JsonElement> predicate) throws IOException {
        for (String key : keys) {
            JsonElement value = object.get(key);
            if (!predicate.test(value)) continue;
            this.jsonWriter.name(key);
            this.write(value);
        }
    }

    private void writeArray(JsonArray array) throws IOException {
        if (JsonFormatter.isCompactable(array)) {
            this.writeCompact(array);
        } else {
            this.writeNormal(array);
        }
    }

    private void writeCompact(JsonArray array) throws IOException {
        this.jsonWriter.beginArray();
        this.jsonWriter.setIndent(COMPACT);
        for (int i = 0; i < array.size(); ++i) {
            if (i > 0) {
                this.writer.write(", ");
            }
            this.writePrimitive(array.get(i).getAsJsonPrimitive(), true);
        }
        this.jsonWriter.endArray();
        this.jsonWriter.setIndent(INDENT);
    }

    private void writeNormal(JsonArray array) throws IOException {
        this.jsonWriter.beginArray();
        for (int i = 0; i < array.size(); ++i) {
            this.write(array.get(i));
        }
        this.jsonWriter.endArray();
    }

    private void writePrimitive(JsonPrimitive json, boolean direct) throws IOException {
        if (json.isNumber()) {
            this.writeNumber(json, direct);
        } else if (json.isBoolean()) {
            this.writeBool(json, direct);
        } else if (json.isString()) {
            this.writeString(json, direct);
        }
    }

    private void writeNumber(JsonPrimitive json, boolean direct) throws IOException {
        double d;
        long l = json.getAsLong();
        if ((double)l == (d = json.getAsDouble())) {
            if (direct) {
                this.writer.write(String.valueOf(l));
            } else {
                this.jsonWriter.value(l);
            }
        } else if (direct) {
            this.writer.write(String.valueOf(JsonFormatter.trimDouble(d)));
        } else {
            this.jsonWriter.value(JsonFormatter.trimDouble(d));
        }
    }

    private void writeBool(JsonPrimitive json, boolean direct) throws IOException {
        if (direct) {
            GSON.toJson((JsonElement)json, (Appendable)this.writer);
        } else {
            this.jsonWriter.value(json.getAsBoolean());
        }
    }

    private void writeString(JsonPrimitive json, boolean direct) throws IOException {
        if (direct) {
            GSON.toJson((JsonElement)json, (Appendable)this.writer);
        } else {
            this.jsonWriter.value(json.getAsString());
        }
    }

    private static double trimDouble(double value) {
        int factor = 1000;
        while (value * (double)factor < 1.0) {
            factor *= 10;
        }
        return (double)Math.round(value * (double)factor) / (double)factor;
    }

    private static boolean isString(JsonElement json) {
        return json.isJsonPrimitive() && json.getAsJsonPrimitive().isString();
    }

    private static boolean isPrimitive(JsonElement json) {
        return json.isJsonPrimitive() && !json.getAsJsonPrimitive().isString();
    }

    private static boolean isCompactable(JsonArray array) {
        int size = array.size();
        if (size == 0) {
            return false;
        }
        JsonElement first = array.get(0);
        if (!first.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive prim = first.getAsJsonPrimitive();
        return !prim.isString() || size < 3;
    }

    private static String[] getKeys(JsonObject json) {
        return (String[])json.keySet().stream().sorted().toArray(String[]::new);
    }

    public static void format(JsonElement jsonElement, Writer writer) throws IOException {
        new JsonFormatter(writer).write(jsonElement);
    }

    public static void format(JsonElement jsonElement, OutputStream out) {
        try (OutputStreamWriter writer = new OutputStreamWriter(out);){
            JsonFormatter.format(jsonElement, writer);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
