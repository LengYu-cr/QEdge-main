package com.qq.taf.jce;

import java.util.Map;

public class JceInputStream {
    public JceInputStream(byte[] data) {}
    public byte read(byte b, int tag, boolean required) { return b; }
    public int read(int i, int tag, boolean required) { return i; }
    public long read(long l, int tag, boolean required) { return l; }
    public String readString(int tag, boolean required) { return ""; }
    public Object read(Object obj, int tag, boolean required) { return obj; }
}
