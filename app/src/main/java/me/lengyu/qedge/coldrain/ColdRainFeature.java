package me.lengyu.qedge.coldrain;

import me.lengyu.qedge.plugin.bean.MsgData;

public interface ColdRainFeature {
    boolean shouldHandle(MsgData msgData);
    void handle(MsgData msgData, ColdRainCore core);
}
