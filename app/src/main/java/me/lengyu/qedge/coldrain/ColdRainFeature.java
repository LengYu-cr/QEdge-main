package me.lengyu.qedge.coldrain;

import me.lengyu.qedge.plugin.bean.MsgData;
/**
 * @Author 冷雨
 * @Description 冷雨Java接口
 */

public interface ColdRainFeature {
    boolean shouldHandle(MsgData msgData);
    void handle(MsgData msgData, ColdRainCore core);
}
