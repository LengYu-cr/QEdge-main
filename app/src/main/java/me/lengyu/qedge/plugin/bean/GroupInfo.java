package me.lengyu.qedge.plugin.bean;

/**
 * @Author 冷雨
 * @Description 群信息（腾讯）
 */
public class GroupInfo {
    public final String group;
    public final String groupName;
    public final String groupOwner;
    public final Object groupInfo;

    public GroupInfo(String group, String groupName, String groupOwner, Object groupInfo) {
        this.group = group;
        this.groupName = groupName;
        this.groupOwner = groupOwner;
        this.groupInfo = groupInfo;
    }

    public String toString() {
        return "GroupInfo{" +
                "group='" + group + '\'' +
                ", groupName='" + groupName + '\'' +
                ", groupOwner='" + groupOwner + '\'' +
                ", groupInfo=" + groupInfo +
                '}';
    }
}