package me.lengyu.qedge.plugin.bean;

/**
 * @Author 冷雨
 * @Description 共同群
 */
public class JointGroup {

    public String qun;
    public long joinTime;

    public JointGroup() {}

    public JointGroup(String qun, long joinTime) {
        this.qun = qun;
        this.joinTime = joinTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JointGroup{")
                .append("qun='").append(qun).append('\'')
                .append(", joinTime=").append(joinTime)
                .append('}');
        return sb.toString();
    }
}