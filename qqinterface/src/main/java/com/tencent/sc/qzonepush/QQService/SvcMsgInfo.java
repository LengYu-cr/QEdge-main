package com.tencent.sc.qzonepush.QQService;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import java.util.HashMap;
import java.util.Map;

public final class SvcMsgInfo extends JceStruct {
    static Map<String, byte[]> cache_msgByteInfos;
    static Map<String, String> cache_msgInfos;
    public int iMsgType;
    public long lFromUin;
    public Map<String, byte[]> msgByteInfos;
    public Map<String, String> msgInfos;
    public String strOther;
    public int tTimeStamp;

    public SvcMsgInfo() {
        this.lFromUin = 0L;
        this.tTimeStamp = 0;
        this.iMsgType = 0;
        this.strOther = "";
        this.msgInfos = null;
        this.msgByteInfos = null;
    }

    @Override
    public void readFrom(JceInputStream jceInputStream) {
        this.lFromUin = jceInputStream.read(this.lFromUin, 0, false);
        this.tTimeStamp = jceInputStream.read(this.tTimeStamp, 1, false);
        this.iMsgType = jceInputStream.read(this.iMsgType, 2, false);
        this.strOther = jceInputStream.readString(3, false);
        if (cache_msgInfos == null) {
            HashMap<String, String> hashMap = new HashMap<>();
            cache_msgInfos = hashMap;
            hashMap.put("", "");
        }
        this.msgInfos = (Map) jceInputStream.read(cache_msgInfos, 4, false);
        if (cache_msgByteInfos == null) {
            HashMap<String, byte[]> hashMap2 = new HashMap<>();
            cache_msgByteInfos = hashMap2;
            hashMap2.put("", new byte[]{0});
        }
        this.msgByteInfos = (Map) jceInputStream.read(cache_msgByteInfos, 5, false);
    }

    @Override
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.lFromUin, 0);
        jceOutputStream.write(this.tTimeStamp, 1);
        jceOutputStream.write(this.iMsgType, 2);
        jceOutputStream.write(this.strOther, 3);
        Map<String, String> map = this.msgInfos;
        if (map != null) {
            jceOutputStream.write(map, 4);
        }
        Map<String, byte[]> map2 = this.msgByteInfos;
        if (map2 != null) {
            jceOutputStream.write(map2, 5);
        }
    }

    public SvcMsgInfo(long j, int i, int i2, String str, Map<String, String> map, Map<String, byte[]> map2) {
        this.lFromUin = j;
        this.tTimeStamp = i;
        this.iMsgType = i2;
        this.strOther = str;
        this.msgInfos = map;
        this.msgByteInfos = map2;
    }
}
