package com.tencent.sc.qzonepush.QQService;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;

public final class SvcMsgPush extends JceStruct {
    static ArrayList<SvcMsgInfo> cache_vecMsgInfos;
    public byte cMore;
    public int iUnread;
    public ArrayList<SvcMsgInfo> vecMsgInfos;

    public SvcMsgPush() {
        this.cMore = (byte) 0;
        this.iUnread = 0;
        this.vecMsgInfos = null;
    }

    @Override
    public void readFrom(JceInputStream jceInputStream) {
        this.cMore = jceInputStream.read(this.cMore, 0, false);
        this.iUnread = jceInputStream.read(this.iUnread, 1, false);
        if (cache_vecMsgInfos == null) {
            cache_vecMsgInfos = new ArrayList<>();
            cache_vecMsgInfos.add(new SvcMsgInfo());
        }
        this.vecMsgInfos = (ArrayList) jceInputStream.read(cache_vecMsgInfos, 2, false);
    }

    @Override
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.cMore, 0);
        jceOutputStream.write(this.iUnread, 1);
        jceOutputStream.write(this.vecMsgInfos, 2);
    }

    public SvcMsgPush(byte b, int i, ArrayList<SvcMsgInfo> arrayList) {
        this.cMore = b;
        this.iUnread = i;
        this.vecMsgInfos = arrayList;
    }
}
