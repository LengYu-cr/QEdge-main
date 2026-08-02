package me.lengyu.qedge.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ObjectStore {

    private static File getDir(String dir) {
        String currentDir = QQCurrentEnv.getCurrentDir();
        File dirFile = new File(currentDir, dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        return dirFile;
    }

    public static boolean saveList(String dir, String fileName, List<String> list) {
        File file = new File(getDir(dir), fileName);
        try {
            JSONArray jsonArray = new JSONArray(list);
            FileWriter writer = new FileWriter(file);
            writer.write(jsonArray.toString());
            writer.close();
            return true;
        } catch (IOException e) {
            LogUtils.e("ObjectStore", "saveList failed: " + e.getMessage());
            return false;
        }
    }

    public static List<String> loadList(String dir, String fileName) {
        File file = new File(getDir(dir), fileName);
        if (!file.exists()) {
            return null;
        }
        try {
            FileReader reader = new FileReader(file);
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            reader.close();
            JSONArray jsonArray = new JSONArray(sb.toString());
            List<String> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
            return list;
        } catch (IOException | JSONException e) {
            LogUtils.e("ObjectStore", "loadList failed: " + e.getMessage());
            return null;
        }
    }

    public static boolean saveString(String dir, String fileName, String content) {
        File file = new File(getDir(dir), fileName);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            LogUtils.e("ObjectStore", "saveString failed: " + e.getMessage());
            return false;
        }
    }

    public static String loadString(String dir, String fileName) {
        File file = new File(getDir(dir), fileName);
        if (!file.exists()) {
            return null;
        }
        try {
            FileReader reader = new FileReader(file);
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            LogUtils.e("ObjectStore", "loadString failed: " + e.getMessage());
            return null;
        }
    }
}
