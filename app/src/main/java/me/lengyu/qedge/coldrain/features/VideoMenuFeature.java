package me.lengyu.qedge.coldrain.features;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.MsgTool;
/**
 * @Author 冷雨
 * @Description 视频菜单处理
 */
public class VideoMenuFeature implements ColdRainFeature {

    private static final String MY_WEB = "https://api.yuafeng.cn/API/ly/";

    private static final Map<String, VideoSearchData> searchCache = new HashMap<>();

    private static class VideoSearchData {
        List<String> authorList = new ArrayList<>();
        List<String> descList = new ArrayList<>();
        List<String> imgList = new ArrayList<>();
        List<String> videoList = new ArrayList<>();
        List<Long> durationList = new ArrayList<>();
        List<String> likeCountList = new ArrayList<>();
        List<String> viewCountList = new ArrayList<>();
        List<String> coverUrlList = new ArrayList<>();
        List<String> photoUrlList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();
        List<String> uriList = new ArrayList<>();
        List<String> author2List = new ArrayList<>();
        String type = "";
    }

    private static void sendVideo(MsgData msgData, String videoUrl) {
        try {
            if (videoUrl == null || videoUrl.isEmpty()) return;
            MsgTool.sendVideo(msgData.peerUin, videoUrl, msgData.type);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    private static void sendImg(MsgData msgData, String imgUrl) {
        try {
            if (imgUrl == null || imgUrl.isEmpty()) return;
            MsgTool.sendPic(msgData.peerUin, imgUrl, msgData.type);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    private static void sendMsg(MsgData msgData, String text) {
        ColdRainCore.getInstance().reply(msgData, text);
    }

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        if (text.equals("视频菜单")) return true;
        if (text.startsWith("哔哩哔哩")) {
            return true;
        }
        String[] cmds = {
            "纯情女高","蛇姐系列","狱卒系列","动漫系列",
            "帅哥系列","网抑系列","漫画芋系","冷雨推荐",
            "治愈系列","随机快手","小姐姐系"
        };
        for (String c : cmds) {
            if (text.equals(c)) return true;
        }
        if (text.matches("[0-9]+") && text.length() < 3) {
            VideoSearchData data = searchCache.get(msgData.peerUin);
            if (data != null && !data.type.isEmpty()) {
                if (data.type.equals("bilibili")) return true;
                if (data.type.equals("kuaishou_random")) return true;
            }
        }
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        final String text = msgData.msg.trim();
        final String peerUin = msgData.peerUin;

        if (text.equals("视频菜单")) {
            String menu = "视频菜单:\n纯情女高 蛇姐系列\n狱卒系列 动漫系列\n帅哥系列 网抑系列\n漫画芋系 冷雨推荐\n治愈系列 随机快手\n小姐姐系 等待添加\n哔哩哔哩+内容";
            sendMsg(msgData, menu);
            return;
        }

        ModuleScope.launchIOJava("VideoMenuFeature", () -> {
                try {
                    handleCommand(msgData, text, peerUin);
                } catch (Throwable e) {
                    sendMsg(msgData, "出错: " + e.getMessage());
                }
        });
    }

    private void handleCommand(MsgData msgData, String text, String peerUin) throws Exception {
        if (text.startsWith("哔哩哔哩")) {
            String msg = text.substring(4);
            String result = "";
            String url = HttpUtils.get("http://app.bilibili.com/x/v2/search?appkey=1d8b6e7d45233436&build=560161&duration=0&keyword=" + java.net.URLEncoder.encode(msg, "UTF-8") + "&mobi_app=android&platform=android&pn=1&ps=10&ts=1534807273&sign=58fec668fa2fb65a5149d04aeca15cbb");
            if (url == null || url.isEmpty()) {
                sendMsg(msgData, "请求失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            String code = json.optString("code");
            if (code.equals("0")) {
                VideoSearchData data = new VideoSearchData();
                data.type = "bilibili";
                String data1 = json.getString("data");
                JSONObject json1 = new JSONObject(data1);
                String items = json1.getString("items");
                JSONObject json2 = new JSONObject(items);
                JSONArray archive = json2.getJSONArray("archive");
                for (int q = 0; q < archive.length(); q++) {
                    JSONObject list = archive.getJSONObject(q);
                    String title = list.getString("title");
                    String cover = list.getString("cover");
                    String author = list.getString("author");
                    String view_content = list.getString("view_content");
                    String share = list.getString("share");
                    JSONObject json3 = new JSONObject(share);
                    String video = json3.getString("video");
                    JSONObject json4 = new JSONObject(video);
                    String uri = json4.getString("short_link");
                    result += (q+1) + "、" + title + "——" + author + "(" + view_content + ")\n";
                    data.coverUrlList.add(cover);
                    data.titleList.add(title);
                    data.uriList.add(uri);
                    data.author2List.add(author);
                }
                searchCache.put(peerUin, data);
                sendMsg(msgData, "" + result);
            } else {
                sendMsg(msgData, "未搜索到");
            }
            return;
        }

        VideoSearchData data = searchCache.get(peerUin);
        if (data != null && !data.type.isEmpty() && text.matches("[0-9]+") && text.length() < 3) {
            int idx = Integer.parseInt(text) - 1;
            if (idx < 0) return;

            if (data.type.equals("kuaishou_random") && idx < data.descList.size()) {
                sendMsg(msgData, "[pic=" + data.coverUrlList.get(idx) + "]描述:" + data.descList.get(idx) + "\n作者:" + data.authorList.get(idx) + "\n时长:" + data.durationList.get(idx)/1000 + "秒\n点赞量:" + data.likeCountList.get(idx) + "\n播放量:" + data.viewCountList.get(idx));
                sendVideo(msgData, data.photoUrlList.get(idx));
                searchCache.remove(peerUin);
            } else if (data.type.equals("bilibili") && idx < data.titleList.size()) {
                try {
                    String uri = data.uriList.get(idx);
                    int index = uri.lastIndexOf("https://b23.tv/");
                    String id = "";
                    if (index >= 0) {
                        id = uri.substring(index + "https://b23.tv/".length());
                    } else {
                        id = uri;
                    }
                    String detailUrl = HttpUtils.get("https://api.bilibili.com/x/web-interface/view/detail?aid=&bvid=" + id);
                    if (detailUrl == null) {
                        sendMsg(msgData, "获取视频信息失败");
                        return;
                    }
                    JSONObject json = new JSONObject(detailUrl);
                    if (json.getInt("code") == 0) {
                        json = json.getJSONObject("data");
                        JSONObject json2 = json.getJSONObject("View");
                        JSONObject json3 = json2.getJSONObject("stat");
                        JSONObject json4 = json2.getJSONObject("owner");
                        String cid = json2.optString("cid");
                        String bvid = id;
                        sendMsg(msgData, json2.getString("title") + "[pic=" + json2.getString("pic") + "]\n作者:" + json4.getString("name") + "\n播放量:" + json3.optString("view") + "\n点赞量:" + json3.optString("like") + "\n正在输出视频:320p(流畅)\n时长:" + json2.optString("duration") + "秒");
                        String playUrl = HttpUtils.get("https://api.bilibili.com/x/player/playurl?cid=" + cid + "&avid=&bvid=" + bvid + "&otype=json&platform=html5&type=mp4&html5=1");
                        searchCache.remove(peerUin);
                        if (playUrl != null) {
                            JSONObject playJson = new JSONObject(playUrl);
                            if (playJson.getInt("code") == 0) {
                                String videoUrl = playJson.getJSONObject("data").getJSONArray("durl").getJSONObject(0).getString("url");
                                sendVideo(msgData, videoUrl);
                            } else {
                                sendMsg(msgData, "出现错误:" + playJson.getString("message"));
                            }
                        } else {
                            sendMsg(msgData, "获取视频地址失败");
                        }
                    } else {
                        sendMsg(msgData, "出现错误:" + json.getString("message"));
                    }
                } catch (Exception e) {
                    sendMsg(msgData, "出现错误:" + e.getMessage());
                }
            }
            return;
        }

        if (text.equals("小姐姐系")) {
            sendVideo(msgData, MY_WEB + "xjj.php");
            return;
        }
        if (text.equals("纯情女高")) {
            sendVideo(msgData, MY_WEB + "cqng.php");
            return;
        }
        if (text.equals("蛇姐系列")) {
            sendVideo(msgData, MY_WEB + "sjxl.php");
            return;
        }
        if (text.equals("狱卒系列")) {
            sendVideo(msgData, MY_WEB + "yzxl.php");
            return;
        }
        if (text.equals("动漫系列")) {
            sendVideo(msgData, MY_WEB + "dmxl.php");
            return;
        }
        if (text.equals("帅哥系列")) {
            sendVideo(msgData, MY_WEB + "sgxl.php");
            return;
        }
        if (text.equals("网抑系列")) {
            sendVideo(msgData, MY_WEB + "emo.php");
            return;
        }
        if (text.equals("漫画芋系")) {
            sendVideo(msgData, MY_WEB + "mhy.php");
            return;
        }
        if (text.equals("治愈系列")) {
            sendVideo(msgData, MY_WEB + "zyxl.php");
            return;
        }
        if (text.equals("冷雨推荐")) {
            sendVideo(msgData, MY_WEB + "sp.php");
            return;
        }

        if (text.equals("随机快手")) {
            String url = HttpUtils.get(MY_WEB + "ksRandom.php");
            if (url == null || url.isEmpty()) {
                sendMsg(msgData, "请求服务器出错：服务器未响应");
                return;
            }
            JSONObject json = new JSONObject(url);
            if (json.getInt("code") == 0) {
                JSONArray jsona = json.getJSONArray("data");
                VideoSearchData ksData = new VideoSearchData();
                ksData.type = "kuaishou_random";
                String result = "";
                for (int i = 0; i < jsona.length(); i++) {
                    JSONObject j = jsona.getJSONObject(i);
                    String author = j.getJSONObject("author").getString("name");
                    JSONObject jj = j.getJSONObject("photo");
                    long duration = jj.getLong("duration");
                    String caption = jj.getString("caption");
                    String likeCount = jj.getString("likeCount");
                    String viewCount = jj.optString("viewCount");
                    String coverUrl = jj.getString("coverUrl");
                    String photoUrl = jj.getString("photoUrl");
                    ksData.authorList.add(author);
                    ksData.durationList.add(duration);
                    ksData.descList.add(caption);
                    ksData.likeCountList.add(likeCount);
                    ksData.viewCountList.add(viewCount);
                    ksData.coverUrlList.add(coverUrl);
                    ksData.photoUrlList.add(photoUrl);
                    result += (i+1) + "、" + caption + "--" + author + "\n";
                }
                searchCache.put(peerUin, ksData);
                sendMsg(msgData, result + "发送序号选择");
            } else {
                sendMsg(msgData, json.getString("msg"));
            }
            return;
        }
    }
}
