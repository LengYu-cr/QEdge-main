package me.lengyu.qedge.plugin.bean;

/**
 * @Author 冷雨
 * @Description 成员入群事件数据
 */
public class JoinData {

    public final String troopUin;
    public final String memberUin;
    /** joinType: 131=邀请进群, 130=主动加入群 */
    public final int joinType;
    public final String memberUid;
    /** 操作者(邀请人)的uin，主动入群时为null */
    public final String adminUin;
    /** 操作者(邀请人)的uid */
    public final String adminUid;
    public final String desc;

    public JoinData(String troopUin, String memberUin, int joinType, String memberUid) {
        this(troopUin, memberUin, joinType, memberUid, null, null);
    }

    public JoinData(String troopUin, String memberUin, int joinType, String memberUid, String adminUin, String adminUid) {
        this.troopUin = troopUin;
        this.memberUin = memberUin;
        this.joinType = joinType;
        this.memberUid = memberUid;
        this.adminUin = adminUin;
        this.adminUid = adminUid;
        if (joinType == 131) {
            this.desc = "邀请进群";
        } else if (joinType == 130) {
            this.desc = "主动加入群";
        } else {
            this.desc = "其他方式入群";
        }
    }

    public String getTroopUin() {
        return troopUin;
    }

    public String getMemberUin() {
        return memberUin;
    }

    public int getJoinType() {
        return joinType;
    }

    public String getMemberUid() {
        return memberUid;
    }

    public String getAdminUin() {
        return adminUin;
    }

    public String getAdminUid() {
        return adminUid;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JoinData{")
                .append("troopUin='").append(troopUin).append('\'')
                .append(", memberUin='").append(memberUin).append('\'')
                .append(", memberUid='").append(memberUid).append('\'')
                .append(", adminUin='").append(adminUin).append('\'')
                .append(", adminUid='").append(adminUid).append('\'')
                .append(", joinType=").append(joinType)
                .append(", desc='").append(desc).append('\'')
                .append('}');
        return sb.toString();
    }
}