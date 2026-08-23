package me.lengyu.qedge.coldrain.features;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
/**
 * @Author 冷雨
 * @Description 菜单处理
 */
public class MenuFeature implements ColdRainFeature {
    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        String menuName = ColdRainCore.getInstance().getMenuName();
        return text.equals(menuName);
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        if (core.isMenuRestricted() && !core.isAdminOrSelf(msgData)) {
            return;
        }

        String[] featureNames = {
            "群管菜单",
            "提示系统",
            "违禁系统",
            "黑白名单",
            "签到系统",
            "问答功能",
            "查询系统",
            "天气系统",
            "整点报时",
            "视频解析",
            "图片菜单",
            "视频菜单",
            "音乐菜单",
            "图片功能",
            "头像菜单",
            "自助上管",
            "头衔功能",
            "赞我点赞",
            "艾特处理"
        };

        String[][] featureKeyGroups = {
            {"feature_group_manager"},
            {"feature_welcome_join", "feature_welcome_quit"},
            {"feature_ban"},
            {"feature_black_white_list"},
            {"feature_signin"},
            {"feature_question"},
            {"feature_query"},
            {"feature_weather"},
            {"feature_hourly"},
            {"feature_video_parse"},
            {"feature_image_menu"},
            {"feature_video_menu"},
            {"feature_music"},
            {"feature_image"},
            {"feature_avatar_menu"},
            {"feature_autoadmin"},
            {"feature_title"},
            {"feature_like"},
            {"feature_at"}
        };

        int length = 0;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < featureKeyGroups.length; i++) {
            boolean enabled = false;
            for (String key : featureKeyGroups[i]) {
                if (core.isFeatureEnabled(key)) {
                    enabled = true;
                    break;
                }
            }
            if (enabled) {
                String name = featureNames[i];
                if (length == 0) {
                    length++;
                    result.append("\n║").append(name);
                } else if (length == 1) {
                    length = 0;
                    result.append("  ").append(name).append("║");
                }
            }
        }

        if (length == 1) {
            result.append("  等待更新║");
        }

        String menu = "╭┅☆ 冷雨𝓙𝓪𝓿𝓪 ☆┅╮" + result.toString() + "\n╰┅━ 运行状态 ━┅╯";
        core.reply(msgData, menu);
    }
}