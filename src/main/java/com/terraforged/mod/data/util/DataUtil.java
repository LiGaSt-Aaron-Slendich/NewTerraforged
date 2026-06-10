package com.terraforged.mod.data.util;

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
import java.util.Map;

public class DataUtil {
    public static final String TYPE_KEY = "type";

    public static DataValue toData(JsonElement json) {
        return DataUtil.toData(json, TYPE_KEY);
    }

    public static DataValue toData(JsonElement json, String typeKey) {
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonElement name = jsonObject.get(typeKey);
            DataObject object = new DataObject(name == null ? "" : name.getAsString());
            for (Map.Entry entry : json.getAsJsonObject().entrySet()) {
                if (((String)entry.getKey()).equals(typeKey)) continue;
                object.add((String)entry.getKey(), DataUtil.toData((JsonElement)entry.getValue(), typeKey));
            }
            return object;
        }
        if (json.isJsonArray()) {
            DataList array = new DataList();
            for (JsonElement entry : json.getAsJsonArray()) {
                array.add(DataUtil.toData(entry, typeKey));
            }
            return array;
        }
        if (json.isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive();
            if (prim.isString()) {
                return DataValue.of(prim.getAsString());
            }
            if (prim.isNumber()) {
                return DataValue.of(prim.getAsNumber());
            }
            if (prim.isBoolean()) {
                return DataValue.of(prim.getAsBoolean());
            }
        }
        throw new Error("Unsupported data type: " + json);
    }

    public static JsonElement toJson(DataValue value) {
        return DataUtil.toJson(value, TYPE_KEY);
    }

    public static JsonElement toJson(DataValue value, String typeKey) {
        if (value.isObj()) {
            JsonObject object = new JsonObject();
            if (!value.asObj().getType().isEmpty()) {
                object.addProperty(typeKey, value.asObj().getType());
            }
            for (Map.Entry<String, DataValue> entry : value.asObj()) {
                object.add(entry.getKey(), DataUtil.toJson(entry.getValue(), typeKey));
            }
            return object;
        }
        if (value.isList()) {
            JsonArray array = new JsonArray();
            for (DataValue element : value.asList()) {
                array.add(DataUtil.toJson(element, typeKey));
            }
            return array;
        }
        return JsonParser.parseString((String)value.asString());
    }

    public static <T, V extends T> void registerSub(Class<T> type, DataSpec<V> spec) {
        DataSpecs.register(spec);
        DataSpecs.registerSub(type, spec);
    }
}
