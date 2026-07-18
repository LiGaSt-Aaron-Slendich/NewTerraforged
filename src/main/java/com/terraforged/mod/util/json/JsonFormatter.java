package com.terraforged.mod.util.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
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
      this.jsonWriter.setIndent("  ");
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
      String[] astring = getKeys(object);
      this.jsonWriter.beginObject();
      this.writeEntries(astring, object, JsonFormatter::isString);
      this.writeEntries(astring, object, JsonFormatter::isPrimitive);
      this.writeEntries(astring, object, JsonElement::isJsonArray);
      this.writeEntries(astring, object, JsonElement::isJsonObject);
      this.jsonWriter.endObject();
   }

   private void writeEntries(String[] keys, JsonObject object, Predicate<JsonElement> predicate) throws IOException {
      for (String s : keys) {
         JsonElement jsonelement = object.get(s);
         if (predicate.test(jsonelement)) {
            this.jsonWriter.name(s);
            this.write(jsonelement);
         }
      }
   }

   private void writeArray(JsonArray array) throws IOException {
      if (isCompactable(array)) {
         this.writeCompact(array);
      } else {
         this.writeNormal(array);
      }
   }

   private void writeCompact(JsonArray array) throws IOException {
      this.jsonWriter.beginArray();
      this.jsonWriter.setIndent("");

      for (int i = 0; i < array.size(); i++) {
         if (i > 0) {
            this.writer.write(", ");
         }

         this.writePrimitive(array.get(i).getAsJsonPrimitive(), true);
      }

      this.jsonWriter.endArray();
      this.jsonWriter.setIndent("  ");
   }

   private void writeNormal(JsonArray array) throws IOException {
      this.jsonWriter.beginArray();

      for (int i = 0; i < array.size(); i++) {
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
      long i = json.getAsLong();
      double d0 = json.getAsDouble();
      if (i == d0) {
         if (direct) {
            this.writer.write(String.valueOf(i));
         } else {
            this.jsonWriter.value(i);
         }
      } else if (direct) {
         this.writer.write(String.valueOf(trimDouble(d0)));
      } else {
         this.jsonWriter.value(trimDouble(d0));
      }
   }

   private void writeBool(JsonPrimitive json, boolean direct) throws IOException {
      if (direct) {
         GSON.toJson(json, this.writer);
      } else {
         this.jsonWriter.value(json.getAsBoolean());
      }
   }

   private void writeString(JsonPrimitive json, boolean direct) throws IOException {
      if (direct) {
         GSON.toJson(json, this.writer);
      } else {
         this.jsonWriter.value(json.getAsString());
      }
   }

   private static double trimDouble(double value) {
      int i = 1000;

      while (value * i < 1.0) {
         i *= 10;
      }

      return (double)Math.round(value * i) / i;
   }

   private static boolean isString(JsonElement json) {
      return json.isJsonPrimitive() && json.getAsJsonPrimitive().isString();
   }

   private static boolean isPrimitive(JsonElement json) {
      return json.isJsonPrimitive() && !json.getAsJsonPrimitive().isString();
   }

   private static boolean isCompactable(JsonArray array) {
      int i = array.size();
      if (i == 0) {
         return false;
      } else {
         JsonElement jsonelement = array.get(0);
         if (!jsonelement.isJsonPrimitive()) {
            return false;
         } else {
            JsonPrimitive jsonprimitive = jsonelement.getAsJsonPrimitive();
            return !jsonprimitive.isString() || i < 3;
         }
      }
   }

   private static String[] getKeys(JsonObject json) {
      return json.keySet().stream().sorted().toArray(String[]::new);
   }

   public static void apply(JsonElement jsonElement, Writer writer) throws IOException {
      new JsonFormatter(writer).write(jsonElement);
   }
}
