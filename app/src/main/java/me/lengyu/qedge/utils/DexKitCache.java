package me.lengyu.qedge.utils;

import java.util.HashMap;
import java.util.Map;

public class DexKitCache {

    private static final Map<String, String> cacheMap = new HashMap<>();

    public static void put(String key, String descriptor) {
        cacheMap.put(key, descriptor);
    }

    public static String getDescriptor(String key) {
        return cacheMap.get(key);
    }

    public static boolean contains(String key) {
        return cacheMap.containsKey(key);
    }

    public static void clear() {
        cacheMap.clear();
    }
}