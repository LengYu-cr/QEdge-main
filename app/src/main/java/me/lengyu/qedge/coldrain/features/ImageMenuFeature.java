package me.lengyu.qedge.coldrain.features;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.MsgTool;

public class ImageMenuFeature implements ColdRainFeature {

    private static final String MY_API = "https://api.yuafeng.cn/";

    private static void sendImg(String peerUin, String url, int type) {
        try {
            if (url == null || url.isEmpty()) return;
            MsgTool.sendPic(peerUin, url, type);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    private static int randInt(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        if (text.equals("图片菜单")) return true;
        if (text.startsWith("度娘搜图") || text.startsWith("搜索表情")) return true;
        String[] cmds = {
            "随机腹肌","幼态动漫","高清壁纸","七濑胡桃","随机龙图","坤坤表情",
            "原神系列","动漫综合","少女写真","风景系列","物语系列","猫娘系列",
            "动漫人物","二次元图","舔狗日记","电脑壁纸","帅哥图片","随机柴郡",
            "每日早报","随机咖波","抹茶旦旦","一二布布","可爱表情","永雏塔菲",
            "随机腿图","随机妹子","可爱龙图","每日60s",
            "妹子JK","doro","卖家秀","kemomimi","miku","QQ壁纸","bing","米哈游",
            "随机JK","小豆泥","COS","小狐狸","猫羽雫","fufu"
        };
        for (String c : cmds) {
            if (text.equals(c)) return true;
        }
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        final String text = msgData.msg.trim();
        final String peerUin = msgData.peerUin;
        final int mtype = msgData.type;

        if (text.equals("图片菜单")) {
            String menu = "图片菜单:\n随机腹肌 幼态动漫\n高清壁纸 七濑胡桃\n随机龙图 坤坤表情\n原神系列 动漫综合\n少女写真 风景系列\n物语系列 猫娘系列\n动漫人物 二次元图\n舔狗日记 电脑壁纸\n帅哥图片 随机柴郡\n每日早报 随机咖波\n抹茶旦旦 一二布布\n可爱表情 永雏塔菲\n随机腿图 随机妹子\n可爱龙图 每日60s\n妹子JK doro 卖家秀\nkemomimi miku QQ壁纸\nbing 米哈游 随机JK\n小豆泥 COS 小狐狸\n猫羽雫 fufu 等待添加\n度娘搜图+内容\n搜索表情+内容";
            core.reply(msgData, menu);
            return;
        }

        final ColdRainCore finalCore = core;
        ModuleScope.launchIOJava("ImageMenuFeature", () -> {
                try {
                    handleCommand(msgData, text, peerUin, mtype, finalCore);
                } catch (Throwable e) {
                    finalCore.reply(msgData, "出错: " + e.getMessage());
                }
        });
    }

    private void handleCommand(MsgData msgData, String text, String peerUin, int mtype, ColdRainCore core) throws Exception {
        if (text.equals("可爱龙图")) {
            String tt = HttpUtils.get(MY_API + "API/ly/long.php?type=text");
            sendImg(peerUin, tt, mtype);
            return;
        }

        if (text.equals("COS")) {
            String tt = HttpUtils.get(MY_API + "API/ly/mys.php");
            if (tt == null || tt.isEmpty()) {
                core.reply(msgData, "请求服务器出错：服务器未响应");
                return;
            }
            try {
                JSONObject json = new JSONObject(tt);
                if (json.getInt("code") == 0) {
                    String channel = json.getString("channel");
                    json = json.getJSONObject("data");
                    String type = json.getString("type");
                    if (type.equals("video")) {
                        String subject = json.getString("subject");
                        String content = json.getString("content");
                        String cover = json.getString("cover");
                        JSONArray array = json.getJSONArray("video").getJSONObject(0).getJSONArray("resolutions");
                        String video = array.getJSONObject(array.length()-1).getString("url");
                        String tag = array.getJSONObject(array.length()-1).getString("definition");
                        String jump = json.getString("url");
                        String coser = json.getJSONObject("user").getString("name");
                        String gender = json.getJSONObject("user").getString("gender");
                        core.reply(msgData, "[pic=" + cover + "]" + coser + "(" + gender + ")\n" + subject + "\n" + content + "\n来源：" + jump + "\n来自于" + channel + "\n" + tag + "视频正在发送中......");
                        sendVideoLocal(msgData, video);
                    } else if (type.equals("image")) {
                        String subject = json.getString("subject");
                        String content = json.getString("content");
                        String cover = json.getString("cover");
                        JSONArray array = json.getJSONArray("image");
                        String jump = json.getString("url");
                        String coser = json.getJSONObject("user").getString("name");
                        String gender = json.getJSONObject("user").getString("gender");
                        core.reply(msgData, "[pic=" + cover + "]" + coser + "(" + gender + ")\n" + subject + "\n" + content + "\n来源：" + jump + "\n来自于" + channel + "\n图集正在发送中......");
                        for (int i = 0; i < array.length(); i++) {
                            String a = array.getString(i);
                            sendImg(peerUin, a, mtype);
                            Thread.sleep(500);
                        }
                    } else {
                        core.reply(msgData, "返回类型出错：返回类型不是image或video");
                    }
                } else {
                    core.reply(msgData, "请求服务器出错：" + json.getString("msg"));
                }
            } catch (Exception e) {
                core.reply(msgData, "JSON解析出错：" + e.getMessage());
            }
            return;
        }

        if (text.equals("每日早报")) {
            sendImg(peerUin, MY_API + "API/60sn/", mtype);
            return;
        }
        if (text.equals("每日60s")) {
            sendImg(peerUin, MY_API + "API/60s/", mtype);
            return;
        }
        if (text.equals("随机腹肌")) {
            sendImg(peerUin, "https://free.wqwlkj.cn/wqwlapi/sgfj.php?type=image", mtype);
            return;
        }
        if (text.equals("幼态动漫")) {
            sendImg(peerUin, "https://api.s01s.cn/API/ytdm/", mtype);
            return;
        }
        if (text.equals("猫羽雫")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=myn", mtype);
            return;
        }
        if (text.equals("随机JK")) {
            sendImg(peerUin, "http://shanhe.kim/api/tu/jk.php", mtype);
            return;
        }
        if (text.equals("bing")) {
            sendImg(peerUin, "https://imgapi.cn/bing.php?rand=true", mtype);
            return;
        }
        if (text.equals("高清壁纸")) {
            sendImg(peerUin, "https://api.suyanw.cn/api/scenery?return=", mtype);
            return;
        }

        if (text.startsWith("度娘搜图")) {
            String msg = text.substring(4);
            String url = "https://m.baidu.com/sf/vsearch?pd=image_content&word=" + msg + "&tn=vsearch&atn=page&sa=vs_img_indexhot&fr=index";
            String html = HttpUtils.get(url);
            if (html == null || html.isEmpty()) {
                core.reply(msgData, "搜索失败");
                return;
            }
            String result = "";
            int index = html.lastIndexOf("type=\"application/json\">{\"data\":");
            if (index >= 0) {
                String text2 = html.substring(index + 24);
                int rd = text2.indexOf("}}</script>");
                if (rd >= 0) {
                    String re = text2.substring(0, rd+2);
                    JSONObject json = new JSONObject(re);
                    String data1 = json.getString("data");
                    if (!data1.contains("\"rsdata\":[]")) {
                        JSONObject json1 = new JSONObject(data1);
                        String rsInfo = json1.getString("rsInfo");
                        JSONObject json2 = new JSONObject(rsInfo);
                        JSONArray rsdata = json2.getJSONArray("rsdata");
                        for (int q = 0; q < rsdata.length() && q < 6; q++) {
                            JSONObject list = rsdata.getJSONObject(q);
                            String relateSearchQuery = list.getString("relateSearchQuery");
                            String rs_thumburl = list.getString("rs_thumburl");
                            if (!rs_thumburl.isEmpty()) {
                                result += relateSearchQuery + "\n[pic=" + rs_thumburl + "]\n";
                            }
                        }
                        core.reply(msgData, "图片来咯～\n" + result);
                    } else if (data1.contains("\"images\":[{")) {
                        JSONObject json1 = new JSONObject(data1);
                        JSONArray rsdata = json1.getJSONArray("images");
                        for (int q = 0; q < rsdata.length() && q < 6; q++) {
                            JSONObject list = rsdata.getJSONObject(q);
                            String fromPageSummaryOrig = list.getString("fromPageSummaryOrig");
                            String thumburl = list.getString("thumburl");
                            String fromUrlHost = list.getString("fromUrlHost");
                            if (!thumburl.isEmpty()) {
                                result += fromPageSummaryOrig + "\n[pic=" + thumburl + "]来源:" + fromUrlHost + "\n";
                            }
                        }
                        core.reply(msgData, "图片来咯～\n" + result);
                    }
                }
            }
            return;
        }

        if (text.startsWith("搜索表情")) {
            try {
                String msg = text.substring(4);
                String url = "https://h5api.sginput.qq.com/wxbq/search?key=" + msg + "&page=1&num=20";
                String resp = HttpUtils.get(url);
                JSONObject json = new JSONObject(resp);
                String returnmsg = json.getString("msg");
                if (returnmsg.equals("succ")) {
                    JSONArray datamsg = json.getJSONArray("data");
                    for (int q = 0; q < datamsg.length() && q < 6; q++) {
                        JSONObject list = datamsg.getJSONObject(q);
                        String indexUrl = list.getString("indexUrl");
                        sendImg(peerUin, indexUrl, mtype);
                        Thread.sleep(500);
                    }
                } else {
                    core.reply(msgData, "表情包搜索失败，请稍后重试！");
                }
            } catch (Exception e) {
                core.reply(msgData, "表情包搜索失败，请稍后重试");
            }
            return;
        }

        if (text.equals("动漫人物")) {
            sendImg(peerUin, "http://shanhe.kim/api/tu/anime.php", mtype);
            return;
        }
        if (text.equals("舔狗日记")) {
            sendImg(peerUin, "https://free.wqwlkj.cn/wqwlapi/tgbj.php", mtype);
            return;
        }
        if (text.equals("二次元图")) {
            sendImg(peerUin, "https://imgapi.xl0408.top/index.php", mtype);
            return;
        }
        if (text.equals("少女写真")) {
            int c = randInt(0, 6);
            sendImg(peerUin, "https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E5%B0%91%E5%A5%B3%E5%86%99%E7%9C%9F" + c, mtype);
            return;
        }
        if (text.equals("物语系列")) {
            int c = randInt(0, 2);
            sendImg(peerUin, "https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E7%89%A9%E8%AF%AD%E7%B3%BB%E5%88%97" + c, mtype);
            return;
        }
        if (text.equals("猫娘系列")) {
            sendImg(peerUin, "https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E7%8C%AB%E5%A8%981", mtype);
            return;
        }
        if (text.equals("风景系列")) {
            int c = randInt(0, 10);
            sendImg(peerUin, "https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E9%A3%8E%E6%99%AF%E7%B3%BB%E5%88%97" + c, mtype);
            return;
        }
        if (text.equals("动漫综合")) {
            int c = randInt(0, 18);
            sendImg(peerUin, "https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E5%8A%A8%E6%BC%AB%E7%BB%BC%E5%90%88" + c, mtype);
            return;
        }
        if (text.equals("原神系列")) {
            sendImg(peerUin, "https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E8%87%AA%E9%80%82%E5%BA%94%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E5%8E%9F%E7%A5%9E", mtype);
            return;
        }
        if (text.equals("帅哥图片")) {
            String tt = fetchRedirectUrl("https://api.lolimi.cn/API/boy/");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("随机柴郡")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=mm", mtype);
            return;
        }
        if (text.equals("坤坤表情")) {
            String tt = HttpUtils.get("http://api.tangdouz.com/zzz/kk.php");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("随机龙图")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=long", mtype);
            return;
        }
        if (text.equals("小狐狸")) {
            String tt = fetchRedirectUrl("https://t.alcy.cc/xhl");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("七濑胡桃")) {
            String tt = fetchRedirectUrl("https://t.alcy.cc/lai");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("小豆泥")) {
            String tt = fetchRedirectUrl("http://api.treason.cn/API/v1/xdn/api.php");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("QQ壁纸")) {
            sendImg(peerUin, MY_API + "bizhi.php", mtype);
            return;
        }
        if (text.equals("电脑壁纸")) {
            String tt = fetchRedirectUrl(MY_API + "API/dnbz/api.php");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("随机咖波")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=mmc", mtype);
            return;
        }
        if (text.equals("抹茶旦旦")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=mcdd", mtype);
            return;
        }
        if (text.equals("doro")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=doro", mtype);
            return;
        }
        if (text.equals("miku")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=miku", mtype);
            return;
        }
        if (text.equals("kemomimi")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=kemomimi", mtype);
            return;
        }
        if (text.equals("fufu")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=fufu", mtype);
            return;
        }
        if (text.equals("一二布布")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=yebb", mtype);
            return;
        }
        if (text.equals("永雏塔菲")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=yctf", mtype);
            return;
        }
        if (text.equals("可爱表情")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=ka", mtype);
            return;
        }
        if (text.equals("米哈游")) {
            sendImg(peerUin, MY_API + "API/ly/tu.php?type=mhy", mtype);
            return;
        }
        if (text.equals("随机妹子")) {
            String tt = HttpUtils.get(MY_API + "API/ly/tui.php?type=text");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("妹子JK")) {
            String tt = HttpUtils.get(MY_API + "API/ly/jk.php?type=text");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("随机腿图")) {
            String tt = HttpUtils.get(MY_API + "API/ly/meitui.php?type=text");
            sendImg(peerUin, tt, mtype);
            return;
        }
        if (text.equals("卖家秀")) {
            String tt = HttpUtils.get(MY_API + "API/ly/mjx.php?type=text");
            sendImg(peerUin, tt, mtype);
            return;
        }
    }

    private String fetchRedirectUrl(String urlStr) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            String redirect = conn.getHeaderField("Location");
            conn.disconnect();
            if (redirect != null && !redirect.isEmpty()) {
                return redirect;
            }
            return urlStr;
        } catch (Exception e) {
            return urlStr;
        }
    }

    private void sendVideoLocal(MsgData msgData, String videoUrl) {
        try {
            if (videoUrl == null || videoUrl.isEmpty()) return;
            MsgTool.sendVideo(msgData.peerUin, videoUrl, msgData.type);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }
}
