package me.lengyu.qedge.coldrain.features;

import java.io.File;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.QQCurrentEnv;
import me.lengyu.qedge.utils.qq.ExtraTool;
import me.lengyu.qedge.utils.qq.MsgTool;

public class AvatarMenuFeature implements ColdRainFeature {

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        if (text.equals("头像菜单")) return true;
        if (text.startsWith("上传头像")) return true;
        if (text.startsWith("上传封面")) return true;
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        final String text = msgData.msg.trim();

        if (text.equals("头像菜单")) {
            String menu = "头像菜单:\n上传头像 \n上传封面\n注:发送图片后直接回复+指令";
            core.reply(msgData, menu);
            return;
        }

        final ColdRainCore finalCore = core;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleCommand(msgData, text, finalCore);
                } catch (Throwable e) {
                    finalCore.reply(msgData, "出错: " + e.getMessage());
                }
            }
        }).start();
    }

    private void handleCommand(MsgData msgData, String text, ColdRainCore core) throws Exception {
        String imgUrl = null;

        if(msgData.msgType == 9) {
            MsgData replyMsgData = new MsgData(msgData.data.records.get(0));
            if(replyMsgData.picList != null && !replyMsgData.picList.isEmpty()){
                imgUrl = replyMsgData.picList.get(0);
            }
        }else {
            return;
        }

        if (imgUrl == null || imgUrl.isEmpty()) {
            core.reply(msgData, "请发送图片后再回复该图片[上传头像/上传封面]");
            return;
        }

        String baseDir = QQCurrentEnv.getCurrentDir();
        if (baseDir.endsWith("/")) {
            baseDir = baseDir.substring(0, baseDir.length() - 1);
        }
        String cacheDir = baseDir + "/cache/images";
        File cf = new File(cacheDir);
        if (!cf.exists()) cf.mkdirs();

        String tempSrc = cacheDir + "/tmp_avatar_" + System.currentTimeMillis() + ".png";
        boolean ok = HttpUtils.downloadSync(imgUrl, tempSrc);
        if (!ok || !new File(tempSrc).exists()) {
            core.reply(msgData, "图片下载失败");
            return;
        }

        boolean success = false;
        if (text.equals("上传头像")) {
            success = ExtraTool.uploadAvatar(tempSrc);
        } else if (text.equals("上传封面")) {
            success = ExtraTool.uploadCover(tempSrc);
        }

        if (success) {
            core.reply(msgData, text.equals("上传头像") ? "头像上传成功" : "封面上传成功");
        } else {
            core.reply(msgData, text.equals("上传头像") ? "头像上传失败" : "封面上传失败");
        }

        // new File(tempSrc).delete();
    }
}
