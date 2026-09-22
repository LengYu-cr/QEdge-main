package me.lengyu.qedge.plugin.bean;

/**
 * @Author 冷雨
 * @Description 群成员信息（腾讯）
 */
public class MemberInfo {
    public final long joinGroupTime;
    public final long lastActiveTime;
    public final String uin;
    public final int uinLevel;
    public final String uinName;
    public final String role;
    public final Object memberInfo;

    public MemberInfo(long joinGroupTime, long lastActiveTime, String uin, int uinLevel, String uinName, String role, Object memberInfo) {
        this.joinGroupTime = joinGroupTime;
        this.lastActiveTime = lastActiveTime;
        this.uin = uin;
        this.uinLevel = uinLevel;
        this.uinName = uinName;
        this.role = role;
        this.memberInfo = memberInfo;
    }

    public String toString() {
        return "MemberInfo{" +
                "joinGroupTime=" + joinGroupTime +
                ", lastActiveTime=" + lastActiveTime +
                ", uin='" + uin + '\'' +
                ", uinLevel=" + uinLevel +
                ", uinName='" + uinName + '\'' +
                ", role='" + role + '\'' +
                ", memberInfo=" + memberInfo +
                '}';
    }
}