package com.garden.sim.core.orgjson;

import java.util.*;

public class JSONArray implements Iterable<Object> {
    private final List<Object> list;

    public JSONArray(List<Object> list) { this.list = list; }

    public int length() { return list.size(); }
    public Object get(int i) { return list.get(i); }

    @Override public Iterator<Object> iterator() { return list.iterator(); }
}
