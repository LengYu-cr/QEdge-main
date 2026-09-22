package me.lengyu.qedge.plugin.bean;
/**
 * @Author 冷雨
 * @Description 群禁言用户信息（腾讯）
 */
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