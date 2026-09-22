package me.lengyu.qedge.utils.json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

import me.lengyu.qedge.utils.LogUtils;
/**
 * @Author 冷雨
 * @Description JSON 扩展工具类
 */
public class JsonExt {

    /**
     * 模拟 walk 函数，按路径获取值
     */
    public static Object walk(JSONObject json, String... path) {
        Object current = json;
        for (String key : path) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).opt(key);
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * 模拟 .str 扩展属性，安全转字符串
     */
    public static String getStr(Object obj) {
        if (obj instanceof String) return (String) obj;
        return null;
    }

    /**
     * 模拟 findFirstValueByKey
     * 深度遍历 JSON 寻找特定 key 的值（用于找 u_ 开头的 UID）
     */
    public static String findUidDeep(Object obj) {
        try {
            if (obj instanceof JSONObject) {
                JSONObject jo = (JSONObject) obj;
                // 1. 优先检查当前层是否有 u_ 开头的值
                Iterator<String> keys = jo.keys();
                while (keys.hasNext()) {
                    Object val = jo.get(keys.next());
                    if (val instanceof String && ((String) val).startsWith("u_")) {
                        return (String) val;
                    }
                }
                // 2. 递归查找
                keys = jo.keys();
                while (keys.hasNext()) {
                    String result = findUidDeep(jo.get(keys.next()));
                    if (result != null) return result;
                }
            } else if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;
                for (int i = 0; i < arr.length(); i++) {
                    String result = findUidDeep(arr.get(i));
                    if (result != null) return result;
                }
            }
        } catch (Exception e) {
            LogUtils.e(e);
        }
        return null;
    }
}
