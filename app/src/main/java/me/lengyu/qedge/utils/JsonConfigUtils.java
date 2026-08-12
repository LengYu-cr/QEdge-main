package me.lengyu.qedge.utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import me.lengyu.qedge.utils.LogUtils;

public class JsonConfigUtils {

    public static void putString(String absoluteDir, String configName, String key, String value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static void putInt(String absoluteDir, String configName, String key, int value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static void putBoolean(String absoluteDir, String configName, String key, boolean value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static void putLong(String absoluteDir, String configName, String key, long value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static void putDouble(String absoluteDir, String configName, String key, double value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static String getString(String absoluteDir, String configName, String key, String defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optString(key, defaultValue);
    }

    public static int getInt(String absoluteDir, String configName, String key, int defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optInt(key, defaultValue);
    }

    public static boolean getBoolean(String absoluteDir, String configName, String key, boolean defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optBoolean(key, defaultValue);
    }

    public static long getLong(String absoluteDir, String configName, String key, long defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optLong(key, defaultValue);
    }

    public static double getDouble(String absoluteDir, String configName, String key, double defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optDouble(key, defaultValue);
    }

    public static void remove(String absoluteDir, String configName, String key) {
        JSONObject json = loadConfig(absoluteDir, configName);
        json.remove(key);
        saveConfig(absoluteDir, configName, json);
    }

    public static boolean contains(String absoluteDir, String configName, String key) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.has(key);
    }

    public static void clear(String absoluteDir, String configName) {
        saveConfig(absoluteDir, configName, new JSONObject());
    }

    public static java.util.Map<String, Object> getConfigMap(String absoluteDir, String configName) {
        JSONObject json = loadConfig(absoluteDir, configName);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        try {
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, json.get(key));
            }
        } catch (JSONException e) {
            LogUtils.e(e);
        }
        return map;
    }

    public static String getConfigPath(String absoluteDir, String configName) {
        return getConfigFile(absoluteDir, configName).getAbsolutePath();
    }

    private static void putValue(String absoluteDir, String configName, String key, Object value) {
        JSONObject json = loadConfig(absoluteDir, configName);
        try {
            json.put(key, value);
            saveConfig(absoluteDir, configName, json);
        } catch (JSONException e) {
            LogUtils.e(e);
        }
    }

    private static JSONObject loadConfig(String absoluteDir, String configName) {
        File configFile = getConfigFile(absoluteDir, configName);
        if (!configFile.exists()) {
            return new JSONObject();
        }

        StringBuilder contentBuilder = new StringBuilder();
        try (FileReader fileReader = new FileReader(configFile);
             BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
            String content = contentBuilder.toString();
            return new JSONObject(new JSONTokener(content));
        } catch (IOException | JSONException e) {
            LogUtils.e(e);
            return new JSONObject();
        }
    }

    private static void saveConfig(String absoluteDir, String configName, JSONObject json) {
        File configFile = getConfigFile(absoluteDir, configName);
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(json.toString(4));
        } catch (IOException | org.json.JSONException e) {
            LogUtils.e(e);
        }
    }

    private static File getConfigFile(String absoluteDir, String configName) {
        File dir = new File(absoluteDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, configName + ".json");
    }
}