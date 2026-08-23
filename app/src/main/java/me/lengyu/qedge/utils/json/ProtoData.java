package me.lengyu.qedge.utils.json;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @Author 冷雨
 * @Description JSON 和 Protobuffer 数据转换类
 */
public class ProtoData {
    private static final String HEX_PREFIX = "hex->";
    private final HashMap<Integer, List<Object>> values = new HashMap<>();

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public byte[] getUnpPackage(byte[] b) {
        if (b == null) return null;
        if (b.length < 4) return b;
        if ((b[0] & 0xFF) == 0) {
            return Arrays.copyOfRange(b, 4, b.length);
        } else {
            return b;
        }
    }

    public void fromJSON(JSONObject json) {
        try {
            Iterator<String> key_it = json.keys();
            while (key_it.hasNext()) {
                String key = key_it.next();
                int k = Integer.parseInt(key);
                Object value = json.get(key);
                if (value instanceof JSONObject) {
                    ProtoData newProto = new ProtoData();
                    newProto.fromJSON((JSONObject) value);
                    putValue(k, newProto);
                } else if (value instanceof JSONArray arr) {
                    for (int i = 0; i < arr.length(); i++) {
                        Object arr_obj = arr.get(i);
                        if (arr_obj instanceof JSONObject) {
                            ProtoData newProto = new ProtoData();
                            newProto.fromJSON((JSONObject) arr_obj);
                            putValue(k, newProto);
                        } else {
                            putValue(k, arr_obj);
                        }
                    }
                } else {
                    putValue(k, value);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void putValue(int key, Object value) {
        List<Object> list = values.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(value);
    }

    public void fromBytes(byte[] b) {
        try {
            b = getUnpPackage(b);
            if (b == null || b.length == 0) return;
            CodedInputStream in = CodedInputStream.newInstance(b);
            while (in.getBytesUntilLimit() > 0) {
                int tag = in.readTag();
                int fieldNumber = tag >>> 3;
                int wireType = tag & 7;
                if (wireType == 4 || wireType == 3 || wireType > 5) {
                    return;
                }
                switch (wireType) {
                    case 0:
                        putValue(fieldNumber, in.readInt64());
                        break;
                    case 1:
                        putValue(fieldNumber, in.readRawVarint64());
                        break;
                    case 2: {
                        byte[] subBytes = in.readByteArray();
                        try {
                            String decoded = new String(subBytes, StandardCharsets.UTF_8);
                            byte[] reEncoded = decoded.getBytes(StandardCharsets.UTF_8);
                            if (Arrays.equals(subBytes, reEncoded)) {
                                ProtoData sub_data = new ProtoData();
                                sub_data.fromBytes(subBytes);
                                if (isLikelyString(sub_data, subBytes)) {
                                    putValue(fieldNumber, decoded);
                                } else {
                                    putValue(fieldNumber, sub_data);
                                }
                            } else {
                                ProtoData sub_data = new ProtoData();
                                sub_data.fromBytes(subBytes);
                                putValue(fieldNumber, sub_data);
                            }
                        } catch (Exception e) {
                            String decoded = new String(subBytes, StandardCharsets.UTF_8);
                            byte[] reEncoded = decoded.getBytes(StandardCharsets.UTF_8);
                            if (Arrays.equals(subBytes, reEncoded)) {
                                putValue(fieldNumber, decoded);
                            } else {
                                putValue(fieldNumber, HEX_PREFIX + bytesToHex(subBytes));
                            }
                        }
                        break;
                    }
                    case 5:
                        putValue(fieldNumber, in.readFixed32());
                        break;
                    default:
                        putValue(fieldNumber, "Unknown wireType: " + wireType);
                        break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isLikelyString(ProtoData parsed, byte[] originalBytes) {
        if (parsed.values.isEmpty()) {
            return true;
        }

        if (parsed.values.size() == 1) {
            Integer firstField = parsed.values.keySet().iterator().next();
            List<Object> values = parsed.values.get(firstField);

            if (values.size() == 1) {
                Object value = values.get(0);

                if (value instanceof Number) {
                    long num = ((Number) value).longValue();
                    if (num > 1000000000000000000L) {
                        return true;
                    }

                    if (firstField == 14 || firstField == 7 || firstField == 2) {
                        return true;
                    }
                }

                if (value instanceof ProtoData nested && nested.values.isEmpty()) {
                    return true;
                }
            }
        }

        int totalValues = 0;
        for (List<?> list : parsed.values.values()) {
            totalValues += list.size();
        }
        if (totalValues <= 2) {
            double fieldDensity = (double) totalValues / originalBytes.length;
            if (fieldDensity < 0.1) {
                return true;
            }
        }

        return false;
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();
            for (Integer k_index : values.keySet()) {
                List<?> list = values.get(k_index);
                if (list.size() > 1) {
                    JSONArray arr = new JSONArray();
                    for (Object o : list) {
                        arr.put(valueToText(o));
                    }
                    obj.put(String.valueOf(k_index), arr);
                } else {
                    for (Object o : list) {
                        obj.put(String.valueOf(k_index), valueToText(o));
                    }
                }
            }
            return obj;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private Object valueToText(Object value) throws Exception {
        if (value instanceof ProtoData data) {
            return data.toJSON();
        } else {
            return value;
        }
    }

    public byte[] toBytes() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bos);
        try {
            for (Integer k_index : values.keySet()) {
                List<?> list = values.get(k_index);
                for (Object o : list) {
                    writeField(out, k_index, o);
                }
            }
            out.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private void writeField(CodedOutputStream out, int fieldNumber, Object value) throws Exception {
        if (value instanceof Long) {
            out.writeInt64(fieldNumber, (Long) value);
        } else if (value instanceof Integer) {
            out.writeInt32(fieldNumber, (Integer) value);
        } else if (value instanceof Boolean) {
            out.writeBool(fieldNumber, (Boolean) value);
        } else if (value instanceof Double) {
            out.writeDouble(fieldNumber, (Double) value);
        } else if (value instanceof Float) {
            out.writeFloat(fieldNumber, (Float) value);
        } else if (value instanceof String s) {
            if (s.startsWith(HEX_PREFIX)) {
                String hexStr = s.substring(HEX_PREFIX.length());
                out.writeByteArray(fieldNumber, hexToBytes(hexStr));
            } else {
                out.writeByteArray(fieldNumber, s.getBytes(StandardCharsets.UTF_8));
            }
        } else if (value instanceof ProtoData data) {
            byte[] subBytes = data.toBytes();
            out.writeByteArray(fieldNumber, subBytes);
        } else if (value instanceof byte[] bytes) {
            out.writeByteArray(fieldNumber, bytes);
        }
    }

    public boolean hasField(int fieldNumber) {
        return values.containsKey(fieldNumber);
    }

    public int getFieldCount(int fieldNumber) {
        List<Object> list = values.get(fieldNumber);
        return list != null ? list.size() : 0;
    }

    public Object get(int fieldNumber) {
        List<Object> list = values.get(fieldNumber);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public List<Object> getAll(int fieldNumber) {
        List<Object> list = values.get(fieldNumber);
        return list != null ? list : new ArrayList<>();
    }

    public int getInt(int fieldNumber) {
        return getInt(fieldNumber, 0);
    }

    public int getInt(int fieldNumber, int defaultValue) {
        Object val = get(fieldNumber);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    public long getLong(int fieldNumber) {
        return getLong(fieldNumber, 0L);
    }

    public long getLong(int fieldNumber, long defaultValue) {
        Object val = get(fieldNumber);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return defaultValue;
    }

    public boolean getBool(int fieldNumber) {
        return getBool(fieldNumber, false);
    }

    public boolean getBool(int fieldNumber, boolean defaultValue) {
        Object val = get(fieldNumber);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        }
        return defaultValue;
    }

    public float getFloat(int fieldNumber) {
        return getFloat(fieldNumber, 0f);
    }

    public float getFloat(int fieldNumber, float defaultValue) {
        Object val = get(fieldNumber);
        if (val instanceof Number) {
            if (val instanceof Float) {
                return (Float) val;
            }
            return Float.intBitsToFloat(((Number) val).intValue());
        }
        return defaultValue;
    }

    public double getDouble(int fieldNumber) {
        return getDouble(fieldNumber, 0.0);
    }

    public double getDouble(int fieldNumber, double defaultValue) {
        Object val = get(fieldNumber);
        if (val instanceof Number) {
            if (val instanceof Double) {
                return (Double) val;
            }
            return Double.longBitsToDouble(((Number) val).longValue());
        }
        return defaultValue;
    }

    public String getString(int fieldNumber) {
        return getString(fieldNumber, "");
    }

    public String getString(int fieldNumber, String defaultValue) {
        Object val = get(fieldNumber);
        if (val instanceof String) {
            return (String) val;
        }
        if (val instanceof byte[]) {
            return new String((byte[]) val, StandardCharsets.UTF_8);
        }
        return defaultValue;
    }

    public ProtoData getMessage(int fieldNumber) {
        Object val = get(fieldNumber);
        if (val instanceof ProtoData) {
            return (ProtoData) val;
        }
        return null;
    }

    public List<ProtoData> getMessages(int fieldNumber) {
        List<ProtoData> result = new ArrayList<>();
        List<Object> list = values.get(fieldNumber);
        if (list != null) {
            for (Object o : list) {
                if (o instanceof ProtoData) {
                    result.add((ProtoData) o);
                }
            }
        }
        return result;
    }

    public List<Integer> getInts(int fieldNumber) {
        List<Integer> result = new ArrayList<>();
        List<Object> list = values.get(fieldNumber);
        if (list != null) {
            for (Object o : list) {
                if (o instanceof Number) {
                    result.add(((Number) o).intValue());
                }
            }
        }
        return result;
    }

    public List<Long> getLongs(int fieldNumber) {
        List<Long> result = new ArrayList<>();
        List<Object> list = values.get(fieldNumber);
        if (list != null) {
            for (Object o : list) {
                if (o instanceof Number) {
                    result.add(((Number) o).longValue());
                }
            }
        }
        return result;
    }

    public List<String> getStrings(int fieldNumber) {
        List<String> result = new ArrayList<>();
        List<Object> list = values.get(fieldNumber);
        if (list != null) {
            for (Object o : list) {
                if (o instanceof String) {
                    result.add((String) o);
                } else if (o instanceof byte[]) {
                    result.add(new String((byte[]) o, StandardCharsets.UTF_8));
                }
            }
        }
        return result;
    }

    // ==================== 字段修改方法（防撤回篡改用） ====================

    public void removeField(int fieldNumber) {
        values.remove(fieldNumber);
    }

    public void clearAll() {
        values.clear();
    }

    public void setInt(int fieldNumber, int value) {
        List<Object> list = new ArrayList<>();
        list.add((long) value);
        values.put(fieldNumber, list);
    }

    public void setLong(int fieldNumber, long value) {
        List<Object> list = new ArrayList<>();
        list.add(value);
        values.put(fieldNumber, list);
    }

    public void setString(int fieldNumber, String value) {
        List<Object> list = new ArrayList<>();
        list.add(value);
        values.put(fieldNumber, list);
    }

    public void setMessage(int fieldNumber, ProtoData value) {
        List<Object> list = new ArrayList<>();
        list.add(value);
        values.put(fieldNumber, list);
    }

    public void set(int fieldNumber, Object value) {
        List<Object> list = new ArrayList<>();
        list.add(value);
        values.put(fieldNumber, list);
    }

    public void setAll(int fieldNumber, List<Object> newList) {
        values.put(fieldNumber, newList);
    }
}