package me.lengyu.qedge.plugin.bean;

public class FriendInfo {
    public final String uin;
    public final String uid;
    public final String name;
    public final String remark;

    public FriendInfo(String uin, String uid, String name, String remark) {
        this.uin = uin;
        this.uid = uid;
        this.name = name;
        this.remark = remark;
    }
    public String toString() {
        return "FriendInfo{" +
                "uin='" + uin + '\'' +
                ", uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}