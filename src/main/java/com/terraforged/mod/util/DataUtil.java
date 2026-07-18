package com.terraforged.mod.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.terraforged.cereal.spec.DataSpec;
import com.terraforged.cereal.spec.DataSpecs;
import com.terraforged.cereal.value.DataList;
import com.terraforged.cereal.value.DataObject;
import com.terraforged.cereal.value.DataValue;
import java.util.Map.Entry;

public class DataUtil {
   public static final String TYPE_KEY = "type";

   public static DataValue toData(JsonElement json) {
      return toData(json, "type");
   }

   public static DataValue toData(JsonElement json, String typeKey) {
      if (json.isJsonObject()) {
         JsonObject jsonobject = json.getAsJsonObject();
         JsonElement jsonelement1 = jsonobject.get(typeKey);
         DataObject dataobject = new DataObject(jsonelement1 == null ? "" : jsonelement1.getAsString());

         for (Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            if (!entry.getKey().equals(typeKey)) {
               dataobject.add(entry.getKey(), toData(entry.getValue(), typeKey));
            }
         }

         return dataobject;
      } else if (!json.isJsonArray()) {
         if (json.isJsonPrimitive()) {
            JsonPrimitive jsonprimitive = json.getAsJsonPrimitive();
            if (jsonprimitive.isString()) {
               return DataValue.of(jsonprimitive.getAsString());
            }

            if (jsonprimitive.isNumber()) {
               return DataValue.of(jsonprimitive.getAsNumber());
            }

            if (jsonprimitive.isBoolean()) {
               return DataValue.of(jsonprimitive.getAsBoolean());
            }
         }

         throw new Error("Unsupported data type: " + json);
      } else {
         DataList datalist = new DataList();

         for (JsonElement jsonelement : json.getAsJsonArray()) {
            datalist.add(toData(jsonelement, typeKey));
         }

         return datalist;
      }
   }

   public static JsonElement toJson(DataValue value) {
      return toJson(value, "type");
   }

   public static JsonElement toJson(DataValue value, String typeKey) {
      if (value.isObj()) {
         JsonObject jsonobject = new JsonObject();
         if (!value.asObj().getType().isEmpty()) {
            jsonobject.addProperty(typeKey, value.asObj().getType());
         }

         for (Entry<String, DataValue> entry : value.asObj()) {
            jsonobject.add(entry.getKey(), toJson(entry.getValue(), typeKey));
         }

         return jsonobject;
      } else if (!value.isList()) {
         return JsonParser.parseString(value.asString());
      } else {
         JsonArray jsonarray = new JsonArray();

         for (DataValue datavalue : value.asList()) {
            jsonarray.add(toJson(datavalue, typeKey));
         }

         return jsonarray;
      }
   }

   public static <T, V extends T> void registerSub(Class<T> type, DataSpec<V> spec) {
      DataSpecs.register(spec);
      DataSpecs.registerSub(type, spec);
   }
}
