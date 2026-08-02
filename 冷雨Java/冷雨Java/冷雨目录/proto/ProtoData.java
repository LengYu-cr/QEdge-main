import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProtoData {
    public HashMap values = new HashMap();
    
    public void fromJSON(JSONObject jsonObject) {
        try {
            Iterator keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                int fieldNumber = Integer.parseInt(key);
                Object value = jsonObject.get(key);
                
                if (value instanceof JSONObject) {
                    ProtoData nestedData = new ProtoData();
                    nestedData.fromJSON((JSONObject) value);
                    putValue(fieldNumber, nestedData);
                } else if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    for (int i = 0; i < array.length(); i++) {
                        Object item = array.get(i);
                        if (item instanceof JSONObject) {
                            ProtoData nestedData = new ProtoData();
                            nestedData.fromJSON((JSONObject) item);
                            putValue(fieldNumber, nestedData);
                        } else {
                            putValue(fieldNumber, item);
                        }
                    }
                } else {
                    putValue(fieldNumber, value);
                }
            }
        } catch (Exception e) {
            Toast("JSON解析失败: " + e.getMessage());
        }
    }
    
    public void fromBytes(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return;
            }
            parseBytes(data, 0);
        } catch (Exception e) {
            Toast("字节解析失败: " + e.getMessage());
        }
    }
    
    public int parseBytes(byte[] data, int offset) throws Exception {
        int pos = offset;
        while (pos < data.length) {
            long tag = readVarint(data, pos);
            pos += varintSize(tag);
            
            int fieldNumber = (int) (tag >> 3);
            int wireType = (int) (tag & 0x07);
            
            switch (wireType) {
                case 0:
                    long varintValue = readVarint(data, pos);
                    pos += varintSize(varintValue);
                    putValue(fieldNumber, varintValue);
                    break;
                    
                case 1:
                    if (pos + 8 <= data.length) {
                        long fixed64 = 0;
                        for (int i = 0; i < 8; i++) {
                            fixed64 |= ((long) (data[pos + i] & 0xFF)) << (i * 8);
                        }
                        pos += 8;
                        putValue(fieldNumber, fixed64);
                    }
                    break;
                    
                case 2:
                    long length = readVarint(data, pos);
                    pos += varintSize(length);
                    int len = (int) length;
                    
                    if (pos + len <= data.length) {
                        byte[] bytes = new byte[len];
                        System.arraycopy(data, pos, bytes, 0, len);
                        pos += len;
                        
                        try {
                            ProtoData nested = new ProtoData();
                            nested.parseBytes(bytes, 0);
                            if (nested.values.size() > 0) {
                                putValue(fieldNumber, nested);
                            } else {
                                putValue(fieldNumber, new String(bytes));
                            }
                        } catch (Exception e) {
                            putValue(fieldNumber, new String(bytes));
                        }
                    }
                    break;
                    
                case 5:
                    if (pos + 4 <= data.length) {
                        long fixed32 = 0;
                        for (int i = 0; i < 4; i++) {
                            fixed32 |= ((long) (data[pos + i] & 0xFF)) << (i * 8);
                        }
                        pos += 4;
                        putValue(fieldNumber, fixed32);
                    }
                    break;
                    
                default:
                    break;
            }
        }
        return pos;
    }
    
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        try {
            Iterator entries = values.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                Integer fieldNumber = (Integer) entry.getKey();
                List fieldValues = (List) entry.getValue();
                
                if (fieldValues.size() == 1) {
                    Object value = fieldValues.get(0);
                    result.put(String.valueOf(fieldNumber), convertToJSON(value));
                } else {
                    JSONArray array = new JSONArray();
                    for (Object value : fieldValues) {
                        array.put(convertToJSON(value));
                    }
                    result.put(String.valueOf(fieldNumber), array);
                }
            }
        } catch (Exception e) {
            Toast("转JSON失败: " + e.getMessage());
        }
        return result;
    }
    
    public Object convertToJSON(Object value) {
        if (value instanceof ProtoData) {
            return ((ProtoData) value).toJSON();
        } else if (value instanceof byte[]) {
            return new String((byte[]) value);
        } else if (value instanceof Long) {
            return value;
        } else if (value instanceof Integer) {
            return value;
        } else if (value instanceof String) {
            return value;
        }
        return value.toString();
    }
    
    public byte[] toBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Iterator entries = values.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                Integer fieldNumber = (Integer) entry.getKey();
                List fieldValues = (List) entry.getValue();
                
                for (int i = 0; i < fieldValues.size(); i++) {
                    Object value = fieldValues.get(i);
                    encodeField(baos, fieldNumber.intValue(), value);
                }
            }
        } catch (Exception e) {
            Toast("编码失败: " + e.getMessage());
            return new byte[0];
        }
        return baos.toByteArray();
    }
    
    public void encodeField(ByteArrayOutputStream baos, int fieldNumber, Object value) throws IOException {
        if (value instanceof ProtoData) {
            byte[] nestedBytes = ((ProtoData) value).toBytes();
            writeVarint(baos, (fieldNumber << 3) | 2);
            writeVarint(baos, nestedBytes.length);
            baos.write(nestedBytes);
        } else if (value instanceof Long) {
            writeVarint(baos, (fieldNumber << 3) | 0);
            writeVarint(baos, ((Long) value).longValue());
        } else if (value instanceof Integer) {
            writeVarint(baos, (fieldNumber << 3) | 0);
            writeVarint(baos, ((Integer) value).longValue());
        } else if (value instanceof String) {
            byte[] strBytes = ((String) value).getBytes();
            writeVarint(baos, (fieldNumber << 3) | 2);
            writeVarint(baos, strBytes.length);
            baos.write(strBytes);
        }
    }
    
    public void writeVarint(ByteArrayOutputStream baos, long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                baos.write((int) value);
                return;
            } else {
                baos.write((int) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }
    
    public long readVarint(byte[] data, int offset) {
        long result = 0;
        int shift = 0;
        int pos = offset;
        
        while (pos < data.length) {
            byte b = data[pos];
            result |= ((long) (b & 0x7F)) << shift;
            pos++;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return result;
    }
    
    public int varintSize(long value) {
        int size = 0;
        do {
            size++;
            value >>>= 7;
        } while (value != 0);
        return size;
    }
    
    public void putValue(int fieldNumber, Object value) {
        List list = (List) values.get(new Integer(fieldNumber));
        if (list == null) {
            list = new ArrayList();
            values.put(new Integer(fieldNumber), list);
        }
        list.add(value);
    }
    
    public Object getValue(int fieldNumber) {
        List list = (List) values.get(new Integer(fieldNumber));
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }
    
    public List getValues(int fieldNumber) {
        return (List) values.get(new Integer(fieldNumber));
    }
}