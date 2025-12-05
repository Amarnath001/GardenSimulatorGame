package com.garden.sim.core.orgjson;

import java.util.*;

/** Extremely tiny JSON parser for predictable config shape (objects/arrays/strings/ints). */
public class JSONTokener {
    private final String s;
    private int i = 0;

    public JSONTokener(String s) { this.s = s.trim(); }

    public Map<String,Object> nextObject() {
        skipWS(); expect('{'); skipWS();
        Map<String,Object> map = new LinkedHashMap<>();
        while (peek() != '}') {
            String key = nextString();
            skipWS(); expect(':'); skipWS();
            Object val = nextValue();
            map.put(key, val);
            skipWS();
            if (peek() == ',') { i++; skipWS(); } else break;
        }
        expect('}');
        return map;
    }

    private Object nextValue() {
        skipWS();
        char c = peek();
        if (c == '"') return nextString();
        if (c == '{') return new JSONObject(nextObject());
        if (c == '[') return nextArray();
        if (c == '-' || Character.isDigit(c)) return nextNumber();
        throw new RuntimeException("Unexpected char: " + c + " at " + i);
    }

    private JSONArray nextArray() {
        expect('['); skipWS();
        List<Object> list = new ArrayList<>();
        while (peek() != ']') {
            list.add(nextValue());
            skipWS();
            if (peek() == ',') { i++; skipWS(); } else break;
        }
        expect(']');
        return new JSONArray(list);
    }

    private Number nextNumber() {
        int start = i;
        if (s.charAt(i) == '-') i++;
        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        return Integer.parseInt(s.substring(start, i));
    }

    private String nextString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') break;
            if (c == '\\') {
                char n = s.charAt(i++);
                if (n == '"' || n == '\\' || n == '/') sb.append(n);
                else if (n == 'n') sb.append('\n');
                else if (n == 't') sb.append('\t');
                else sb.append(n);
            } else sb.append(c);
        }
        return sb.toString();
    }

    private void skipWS() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    private void expect(char c) { if (s.charAt(i++) != c) throw new RuntimeException("Expected " + c + " at " + (i-1)); }
    private char peek() { return s.charAt(i); }
}
