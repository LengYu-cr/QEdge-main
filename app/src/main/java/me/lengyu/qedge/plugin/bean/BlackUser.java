package me.lengyu.qedge.plugin.bean;

/**
 * @Author 冷雨
 * @Description 群黑名单用户信息（腾讯）
 */
public class BlackUser {

    public String uin;
    public String name;
    public long time;
    public String op_uin;

    public BlackUser() {}

    public BlackUser(String uin, String name, long time, String op_uin) {
        this.uin = uin;
        this.name = name;
        this.time = time;
        this.op_uin = op_uin;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BlackUser{")
                .append("uin='").append(uin).append('\'')
                .append(", name='").append(name).append('\'')
                .append(", time=").append(time)
                .append(", op_uin='").append(op_uin).append('\'')
                .append('}');
        return sb.toString();
    }
}