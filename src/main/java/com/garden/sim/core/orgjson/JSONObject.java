package com.garden.sim.core.orgjson;

import java.util.*;

public class JSONObject {
    private final Map<String, Object> map;

    public JSONObject(String json) {
        this.map = new JSONTokener(json).nextObject();
    }

    public JSONObject(Map<String, Object> map) { this.map = map; }

    public String getString(String key) { return (String) map.get(key); }
    public int getInt(String key) { return ((Number) map.get(key)).intValue(); }
    public JSONArray getJSONArray(String key) { return (JSONArray) map.get(key); }
    public JSONArray optJSONArray(String key) { Object v = map.get(key); return v instanceof JSONArray ? (JSONArray) v : null; }
}
