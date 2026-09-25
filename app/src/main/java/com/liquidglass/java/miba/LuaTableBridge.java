package com.liquidglass.java.miba;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Tiny dependency-free adapter for Lua/Java bridges.
 *
 * It understands Java List/Map/arrays/Iterable directly and also tries common
 * Lua-table reflection shapes such as length() + get(int) or get(Object).
 * No Lua runtime dependency is required, so the project remains plain Java 7.
 */
public final class LuaTableBridge {
    private LuaTableBridge() {}

    public static List<Object> toList(Object table) {
        ArrayList<Object> out = new ArrayList<Object>();
        if (table == null) return out;

        if (table instanceof List) {
            out.addAll((List<?>) table);
            return out;
        }

        if (table.getClass().isArray()) {
            int n = Array.getLength(table);
            int i;
            for (i = 0; i < n; i++) out.add(Array.get(table, i));
            return out;
        }

        if (table instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) table;
            Object nested = firstNonNull(map.get("tabs"), map.get("items"), map.get("data"));
            if (nested != null && nested != table) return toList(nested);

            ArrayList<Map.Entry<?, ?>> numeric = new ArrayList<Map.Entry<?, ?>>();
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> e = iterator.next();
                if (numericIndex(e.getKey()) != null) numeric.add(e);
            }
            if (!numeric.isEmpty()) {
                Collections.sort(numeric, new Comparator<Map.Entry<?, ?>>() {
                    public int compare(Map.Entry<?, ?> a, Map.Entry<?, ?> b) {
                        return numericIndex(a.getKey()).compareTo(numericIndex(b.getKey()));
                    }
                });
                int i;
                for (i = 0; i < numeric.size(); i++) out.add(numeric.get(i).getValue());
                return out;
            }

            // A map without numeric keys is treated as one item/config record.
            out.add(table);
            return out;
        }

        if (table instanceof Iterable) {
            Iterator<?> it = ((Iterable<?>) table).iterator();
            while (it.hasNext()) out.add(it.next());
            return out;
        }

        // Common LuaTable/LuaJ shape: length()/len() plus get(index).
        int length = reflectedLength(table);
        if (length > 0) {
            int i;
            for (i = 1; i <= length; i++) {
                Object value = reflectedGet(table, Integer.valueOf(i));
                if (value == null) value = reflectedGet(table, Integer.valueOf(i - 1));
                out.add(value);
            }
            return out;
        }

        out.add(table);
        return out;
    }

    public static Object get(Object object, String key) {
        if (object == null || key == null) return null;
        if (object instanceof Map) return ((Map<?, ?>) object).get(key);

        Object value = reflectedGet(object, key);
        if (value != null) return normalizeLuaNil(value);

        try {
            Field field = object.getClass().getField(key);
            field.setAccessible(true);
            return normalizeLuaNil(field.get(object));
        } catch (Throwable ignored) {}

        String suffix = key.length() == 0 ? key : Character.toUpperCase(key.charAt(0)) + key.substring(1);
        String[] names = new String[] { "get" + suffix, "is" + suffix };
        int i;
        for (i = 0; i < names.length; i++) {
            try {
                Method method = object.getClass().getMethod(names[i]);
                method.setAccessible(true);
                return normalizeLuaNil(method.invoke(object));
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static Object getAny(Object object, String[] keys) {
        if (keys == null) return null;
        int i;
        for (i = 0; i < keys.length; i++) {
            Object value = get(object, keys[i]);
            if (value != null) return value;
        }
        return null;
    }

    public static String getString(Object object, String[] keys, String fallback) {
        Object value = getAny(object, keys);
        if (value == null) return fallback;
        String s = stringValue(value);
        return s == null ? fallback : s;
    }

    public static int getInt(Object object, String[] keys, int fallback) {
        Object value = getAny(object, keys);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) {
            String text = String.valueOf(value).trim();
            try { return Long.decode(text).intValue(); }
            catch (Throwable ignored) {}
            try { return (int) Long.parseLong(text); }
            catch (Throwable ignored) {}
            try { return (int) Double.parseDouble(text); }
            catch (Throwable ignored) {}
        }
        return fallback;
    }

    public static long getLong(Object object, String[] keys, long fallback) {
        Object value = getAny(object, keys);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value != null) {
            String text = String.valueOf(value).trim();
            try { return Long.decode(text).longValue(); }
            catch (Throwable ignored) {}
            try { return Long.parseLong(text); }
            catch (Throwable ignored) {}
            try { return (long) Double.parseDouble(text); }
            catch (Throwable ignored) {}
        }
        return fallback;
    }

    public static double getDouble(Object object, String[] keys, double fallback) {
        Object value = getAny(object, keys);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value != null) {
            try { return Double.parseDouble(String.valueOf(value)); }
            catch (Throwable ignored) {}
        }
        return fallback;
    }

    public static boolean has(Object object, String key) {
        return get(object, key) != null;
    }

    public static float getFloat(Object object, String[] keys, float fallback) {
        Object value = getAny(object, keys);
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value != null) {
            try { return Float.parseFloat(String.valueOf(value)); }
            catch (Throwable ignored) {}
        }
        return fallback;
    }

    public static boolean getBoolean(Object object, String[] keys, boolean fallback) {
        Object value = getAny(object, keys);
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        if (value != null) {
            String s = String.valueOf(value);
            if ("true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "1".equals(s)) return true;
            if ("false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s) || "0".equals(s)) return false;
        }
        return fallback;
    }

    private static Integer numericIndex(Object key) {
        if (key instanceof Number) return Integer.valueOf(((Number) key).intValue());
        if (key != null) {
            try { return Integer.valueOf(Integer.parseInt(String.valueOf(key))); }
            catch (Throwable ignored) {}
        }
        return null;
    }

    private static int reflectedLength(Object object) {
        String[] names = new String[] { "length", "len", "size" };
        int i;
        for (i = 0; i < names.length; i++) {
            try {
                Method method = object.getClass().getMethod(names[i]);
                method.setAccessible(true);
                Object value = method.invoke(object);
                Integer n = toInt(value);
                if (n != null) return Math.max(0, n.intValue());
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    private static Object reflectedGet(Object object, Object key) {
        Method[] methods = object.getClass().getMethods();
        int i;
        for (i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (!"get".equals(m.getName()) || m.getParameterTypes().length != 1) continue;
            Class<?> p = m.getParameterTypes()[0];
            Object arg = adaptKey(key, p);
            if (arg == NO_VALUE) continue;
            try {
                m.setAccessible(true);
                return normalizeLuaNil(m.invoke(object, arg));
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static final Object NO_VALUE = new Object();

    private static Object adaptKey(Object key, Class<?> target) {
        if (target == Object.class) return key;
        if (target.isInstance(key)) return key;
        if (key instanceof Number) {
            Number n = (Number) key;
            if (target == int.class || target == Integer.class) return Integer.valueOf(n.intValue());
            if (target == long.class || target == Long.class) return Long.valueOf(n.longValue());
            if (target == double.class || target == Double.class) return Double.valueOf(n.doubleValue());
            if (target == float.class || target == Float.class) return Float.valueOf(n.floatValue());
        }
        if (target == String.class) return String.valueOf(key);
        return NO_VALUE;
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number) return Integer.valueOf(((Number) value).intValue());
        if (value != null) {
            try {
                Method method = value.getClass().getMethod("toint");
                Object r = method.invoke(value);
                if (r instanceof Number) return Integer.valueOf(((Number) r).intValue());
            } catch (Throwable ignored) {}
            try { return Integer.valueOf(Integer.parseInt(String.valueOf(value))); }
            catch (Throwable ignored) {}
        }
        return null;
    }

    private static String stringValue(Object value) {
        value = normalizeLuaNil(value);
        if (value == null) return null;
        try {
            Method method = value.getClass().getMethod("tojstring");
            Object r = method.invoke(value);
            if (r != null) return String.valueOf(r);
        } catch (Throwable ignored) {}
        return String.valueOf(value);
    }

    private static Object normalizeLuaNil(Object value) {
        if (value == null) return null;
        String name = value.getClass().getName();
        if (name.endsWith("LuaNil") || "nil".equals(String.valueOf(value))) return null;
        return value;
    }

    private static Object firstNonNull(Object a, Object b, Object c) {
        return a != null ? a : (b != null ? b : c);
    }
}
