package me.lengyu.qedge.plugin.bean;

public class ForbidInfo {
    public final String uin;
    public final String name;
    public final long duration;

    public ForbidInfo(String uin, String name, long duration) {
        this.uin = uin;
        this.name = name;
        this.duration = duration;
    }

    public String toString() {
        return "ForbidInfo{" +
                "uin='" + uin + '\'' +
                ", name='" + name + '\'' +
                ", duration=" + duration +
                '}';
    }
}