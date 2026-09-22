package me.lengyu.qedge.plugin.bean;

/**
 * @Author 冷雨
 * @Description 成员退群事件数据
 */
public class QuitData {

    public final String troopUin;
    public final String memberUin;
    /** 是否触发成员退场提示(handleMemberExit)，即是否显示"已退出群聊/被移出"红字提示 */
    public final boolean updateHeadAndName;

    public QuitData(String troopUin, String memberUin, boolean updateHeadAndName) {
        this.troopUin = troopUin;
        this.memberUin = memberUin;
        this.updateHeadAndName = updateHeadAndName;
    }

    public String getTroopUin() {
        return troopUin;
    }

    public String getMemberUin() {
        return memberUin;
    }

    public boolean isUpdateHeadAndName() {
        return updateHeadAndName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QuitData{")
                .append("troopUin='").append(troopUin).append('\'')
                .append(", memberUin='").append(memberUin).append('\'')
                .append(", updateHeadAndName=").append(updateHeadAndName)
                .append('}');
        return sb.toString();
    }
}