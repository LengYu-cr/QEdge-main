package com.qq.taf.jce;

public abstract class JceStruct {
    public abstract void readFrom(JceInputStream jceInputStream);
    public abstract void writeTo(JceOutputStream jceOutputStream);
}
