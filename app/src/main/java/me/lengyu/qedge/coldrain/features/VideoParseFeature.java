package me.lengyu.qedge.coldrain.features;

import org.json.JSONArray;
import org.json.JSONObject;

import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.MsgTool;
/**
 * @Author 冷雨
 * @Description 视频解析功能处理
 */
public class VideoParseFeature implements ColdRainFeature {

    private static final String MY_API = "https://api.yuafeng.cn/";
    private static final String MY_WEB = "https://api.yuafeng.cn/API/ly/";

    @Override
    public boolean shouldHandle(MsgData msgData) {
        if (msgData.msg == null || msgData.msg.isEmpty()) return false;
        String text = msgData.msg.trim();
        if (text.equals("视频解析") || text.equals("解析菜单")) {
            return true;
        }
        if (text.contains("https://v.douyin.com/")) return true;
        if (text.contains("https://qishui.douyin.com/s/")) return true;
        if (text.contains("https://www.douyin.com/user/")) return true;
        if (text.contains("https://v.kuaishou.com/")) return true;
        if (text.contains("https://b23.tv/")) return true;
        if (text.contains("http://xhslink.com/")) return true;
        if (text.contains("https://h5.pipix.com/s/")) return true;
        if (text.contains("https://pd.qq.com/s/")) return true;
        if (text.contains("https://mp.weixin.qq.com/s/")) return true;
        if (text.contains("https://s.viviv.com/")) return true;
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();
        String qun = msgData.peerUin;
        int mtype = msgData.type;

        if (text.equals("视频解析") || text.equals("解析菜单")) {
            boolean enabled;
            if (mtype == 2 && qun != null && !qun.isEmpty()) {
                enabled = core.isGroupFeatureEnabled("feature_video_parse", qun);
            } else {
                enabled = core.isFeatureEnabled("feature_video_parse");
            }
            if (enabled) {
                String menu = "解析菜单:\nTips:1.开启后直接发链接(可加文字)\n" +
                    "仅支持快手，抖音，小红书，哔哩哔哩，皮皮虾，西瓜视频的视频/图集解析\n" +
                    "2.支持QQ小世界解析(转发卡片)\n" +
                    "3.支持微信公众号/QQ频道图集/视频解析\n" +
                    "4.支持抖音用户主页解析";
                sendMsg(msgData, menu);
                return;
            } else {
                sendMsg(msgData, "本聊天未开启视频解析");
                return;
            }
        }

        if (!core.isFeatureEnabled("feature_video_parse")) {
            if (mtype == 2 && qun != null && !qun.isEmpty()) {
                if (!core.isGroupFeatureEnabled("feature_video_parse", qun)) {
                    return;
                }
            } else {
                return;
            }
        }

        ModuleScope.launchIOJava("VideoParse", () -> {
            try {
                parseVideo(msgData, text);
            } catch (Throwable e) {
                sendMsg(msgData, "解析出错: " + e.getMessage());
            }
        });
    }

    private void parseVideo(MsgData msgData, String text) throws Exception {
        String peerUin = msgData.peerUin;
        int mtype = msgData.type;

        if (text.contains("https://v.douyin.com/") || text.contains("https://qishui.douyin.com/s/") || text.contains("https://www.douyin.com/user/")) {
            String sl = findRealUrl(text);
            String url = MY_API + "API/ly/dyjx.php?url=" + urlEncode(sl);
            String sj = HttpUtils.get(url);
            if (sj == null || sj.isEmpty()) {
                sendMsg(msgData, "请求服务器出错");
                return;
            }
            JSONObject json = new JSONObject(sj);
            String msg = json.optString("msg");
            if ("获取成功".equals(msg)) {
                String type = json.getString("type");
                JSONObject data1 = json.getJSONObject("data");
                if ("视频".equals(type)) {
                    String cover = data1.getString("cover");
                    String video = data1.getString("url");
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n收藏数:" + count.getLong("collect") +
                        "\n视频发送中...");
                    sendVideo(msgData, video);
                } else if ("图集".equals(type)) {
                    String cover = data1.getString("cover");
                    JSONArray ttp = data1.getJSONArray("images");
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n收藏数:" + count.getLong("collect") +
                        "\n图集发送中...");
                    for (int i = 0; i < ttp.length(); i++) {
                        sendImg(msgData, ttp.getString(i));
                    }
                } else if ("视频集合".equals(type)) {
                    String cover = data1.getString("cover");
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    JSONArray ttp = data1.getJSONArray("videos");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n收藏数:" + count.getLong("collect") +
                        "\n视频集合发送中...(共" + ttp.length() + "个视频)");
                    for (int i = 0; i < ttp.length(); i++) {
                        sendVideo(msgData, ttp.getString(i));
                    }
                } else if ("视频集合 and 图集".equals(type)) {
                    String cover = data1.getString("cover");
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    JSONArray videos = data1.optJSONArray("video");
                    JSONArray images = data1.optJSONArray("images");
                    int vCount = videos != null ? videos.length() : 0;
                    int iCount = images != null ? images.length() : 0;
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n收藏数:" + count.getLong("collect") +
                        "\n视频+图集发送中...(共" + vCount + "个视频," + iCount + "张图片)");
                    if (videos != null) {
                        for (int i = 0; i < videos.length(); i++) {
                            sendVideo(msgData, videos.getString(i));
                        }
                    }
                    if (images != null) {
                        for (int i = 0; i < images.length(); i++) {
                            sendImg(msgData, images.getString(i));
                        }
                    }
                } else if ("主页".equals(type)) {
                    JSONObject author = data1.getJSONObject("author");
                    JSONObject stats = data1.getJSONObject("statistics");
                    String avatar = author.optString("avatar", "");
                    String name = author.optString("name", "未知");
                    String gender = author.optString("gender", "未知");
                    String signature = author.optString("signature", "");
                    long following = stats.optLong("following_count", 0);
                    long follower = stats.optLong("follower_count", 0);
                    long totalFavorited = stats.optLong("total_favorited", 0);
                    long awemeCount = stats.optLong("aweme_count", 0);
                    StringBuilder sb = new StringBuilder();
                    if (!avatar.isEmpty()) {
                        sb.append("[pic=").append(avatar).append("]\n");
                    }
                    sb.append("昵称:").append(name).append("\n");
                    sb.append("性别:").append(gender).append("\n");
                    sb.append("关注:").append(following).append("\n");
                    sb.append("粉丝:").append(follower).append("\n");
                    sb.append("获赞:").append(totalFavorited).append("\n");
                    sb.append("作品:").append(awemeCount);
                    if (!signature.isEmpty()) {
                        sb.append("\n签名:").append(signature);
                    }
                    sendMsg(msgData, sb.toString());
                } else if ("音乐".equals(type)) {
                    JSONObject music = data1.optJSONObject("music");
                    String cover = data1.optString("cover", "");
                    String title = music != null ? music.optString("title", data1.optString("desc", "")) : data1.optString("desc", "");
                    String musicAuthor = music != null ? music.optString("author", "") : "";
                    int duration = music != null ? music.optInt("duration", 0) : 0;
                    String musicUrl = data1.optString("url", "");
                    if (musicUrl.isEmpty() && music != null) {
                        musicUrl = music.optString("url", "");
                    }
                    JSONObject count = data1.optJSONObject("count");
                    StringBuilder sb = new StringBuilder();
                    if (!cover.isEmpty()) sb.append("[pic=").append(cover).append("]\n");
                    sb.append("歌名:").append(title);
                    if (!musicAuthor.isEmpty()) sb.append("\n歌手:").append(musicAuthor);
                    if (duration > 0) sb.append("\n时长:").append(String.format("%02d:%02d", duration / 60, duration % 60));
                    if (count != null) {
                        sb.append("\n喜欢:").append(count.optLong("like", 0));
                        sb.append("\n评论:").append(count.optLong("comment", 0));
                        sb.append("\n分享:").append(count.optLong("share", 0));
                        sb.append("\n收藏:").append(count.optLong("collect", 0));
                    }
                    sb.append("\n音乐发送中...");
                    sendMsg(msgData, sb.toString());
                    if (!musicUrl.isEmpty()) {
                        sendPtt(msgData, musicUrl);
                    }
                }
            } else {
                sendMsg(msgData, sj);
            }
            return;
        }

        if (text.contains("https://v.kuaishou.com/")) {
            String sl = findRealUrl(text);
            String url = MY_API + "API/ly/ksjx.php?url=" + urlEncode(sl);
            String sj = HttpUtils.get(url);
            if (sj == null || sj.isEmpty()) {
                sendMsg(msgData, "请求服务器出错");
                return;
            }
            JSONObject json = new JSONObject(sj);
            String msg = json.optString("msg");
            if ("获取成功".equals(msg)) {
                String type = json.getString("type");
                JSONObject data1 = json.getJSONObject("data");
                if ("视频".equals(type)) {
                    String cover = data1.getString("cover");
                    String video = data1.getString("url");
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n收藏数:" + count.getLong("collect") +
                        "\n视频发送中...");
                    sendVideo(msgData, video);
                } else if ("图集".equals(type)) {
                    String cover = data1.getString("cover");
                    JSONArray ttp = data1.getJSONArray("images");
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n收藏数:" + count.getLong("collect") +
                        "\n图集发送中...");
                    for (int i = 0; i < ttp.length(); i++) {
                        sendImg(msgData, ttp.getString(i));
                    }
                }
            } else {
                sendMsg(msgData, sj);
            }
            return;
        }

        if (text.contains("https://b23.tv/")) {
            String msgs = fetchRedirectUrl(findRealUrl(text));
            try {
                String url = HttpUtils.get(MY_API + "API/ly/bilibili_jx.php?url=" + urlEncode(msgs));
                if (url == null || url.isEmpty()) {
                    sendMsg(msgData, "请求服务器出错");
                    return;
                }
                JSONObject json = new JSONObject(url);
                String msg = json.getString("msg");
                if ("获取成功".equals(msg)) {
                    JSONObject data1 = json.getJSONObject("data");
                    String type = json.optString("type", "");
                    String cover = data1.optString("cover", "");
                    String title = data1.optString("title", "");
                    String author = json.optJSONObject("author") != null
                        ? json.getJSONObject("author").optString("name", "未知") : "未知";
                    String publishTime = data1.optString("publish_time", "");
                    if ("图集".equals(type)) {
                        JSONArray images = data1.optJSONArray("images");
                        int imgCount = images != null ? images.length() : 0;
                        StringBuilder sb = new StringBuilder();
                        if (!cover.isEmpty()) sb.append("[pic=").append(cover).append("]\n");
                        sb.append("标题:").append(title).append("\n作者:").append(author);
                        if (!publishTime.isEmpty()) sb.append("\n发布时间:").append(publishTime);
                        sb.append("\n图集发送中...(").append(imgCount).append("张图片)");
                        sendMsg(msgData, sb.toString());
                        if (images != null) {
                            for (int i = 0; i < images.length(); i++) {
                                sendImg(msgData, images.getString(i));
                            }
                        }
                    } else if ("视频".equals(type)) {
                        String video = data1.optString("video", "");
                        JSONArray pages = data1.optJSONArray("pages");
                        String duration = "";
                        if (pages != null && pages.length() > 0) {
                            duration = pages.getJSONObject(0).optString("duration_format", "");
                        }
                        StringBuilder sb = new StringBuilder();
                        if (!cover.isEmpty()) sb.append("[pic=").append(cover).append("]\n");
                        sb.append("标题:").append(title).append("\n作者:").append(author);
                        if (!duration.isEmpty()) sb.append("\n时长:").append(duration);
                        if (!publishTime.isEmpty()) sb.append("\n发布时间:").append(publishTime);
                        sb.append("\n视频发送中...");
                        sendMsg(msgData, sb.toString());
                        if (!video.isEmpty()) {
                            sendVideo(msgData, video);
                        }
                    }
                } else {
                    sendMsg(msgData, "出现错误:" + json.optString("msg"));
                }
            } catch (Exception e) {
                sendMsg(msgData, "出现错误:" + e.getMessage());
            }
            return;
        }

        if (text.contains("http://xhslink.com/") ||
            text.contains("https://h5.pipix.com/s/")) {
            String sl = findRealUrl(text);
            String url = MY_API + "API/ly/spjx.php?url=" + urlEncode(sl);
            String sj = HttpUtils.get(url);
            if (sj == null || sj.isEmpty()) {
                sendMsg(msgData, "请求服务器出错");
                return;
            }
            JSONObject json = new JSONObject(sj);
            String msg = json.optString("msg");
            if ("获取成功".equals(msg)) {
                String data1 = json.getString("data");
                if (!sj.contains("images")) {
                    JSONObject json1 = new JSONObject(data1);
                    String cover = json1.getString("cover");
                    String video = json1.optString("url", json1.optString("video", ""));
                    String title = json1.getString("title");
                    sendMsg(msgData, "[pic=" + cover + "]\n" + title);
                    if (!video.isEmpty()) {
                        sendVideo(msgData, video);
                    }
                } else {
                    JSONObject json1 = new JSONObject(data1);
                    String cover = json1.getString("cover");
                    JSONArray ttp = json1.getJSONArray("images");
                    String title = json1.getString("title");
                    String tp2 = "";
                    for (int i = 0; i < ttp.length(); i++) {
                        tp2 += "[pic=" + ttp.getString(i) + "]";
                    }
                    sendMsg(msgData, "图片来咯～" + tp2 + "标题:" + title);
                }
            } else {
                sendMsg(msgData, sj);
            }
            return;
        }

        if (text.contains("https://pd.qq.com/s/")) {
            String sl = findRealUrl(text);
            String url = MY_WEB + "pdjx.php?url=" + urlEncode(sl);
            String sj = HttpUtils.get(url);
            if (sj == null || sj.isEmpty()) {
                sendMsg(msgData, "请求服务器出错");
                return;
            }
            JSONObject json = new JSONObject(sj);
            String msg = json.optString("msg");
            if ("获取成功".equals(msg)) {
                String type = json.getString("type");
                JSONObject data1 = json.getJSONObject("data");
                if ("视频".equals(type)) {
                    String cover = data1.getString("cover");
                    String video = data1.getString("video");
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n浏览数:" + count.getLong("view") +
                        "\n视频发送中...");
                    sendVideo(msgData, video);
                } else if ("图集".equals(type)) {
                    String cover = data1.getString("cover");
                    JSONArray ttp = data1.getJSONArray("images");
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n浏览数:" + count.getLong("view") +
                        "\n图集发送中...");
                    for (int i = 0; i < ttp.length(); i++) {
                        sendImg(msgData, ttp.getString(i));
                    }
                }
            } else {
                sendMsg(msgData, sj);
            }
            return;
        }

        if (text.contains("https://s.viviv.com/")) {
            String sl = findRealUrl(text);
            String url = MY_WEB + "hsjx.php?url=" + urlEncode(sl);
            String sj = HttpUtils.get(url);
            if (sj == null || sj.isEmpty()) {
                sendMsg(msgData, "请求服务器出错");
                return;
            }
            JSONObject json = new JSONObject(sj);
            String msg = json.optString("msg");
            if ("获取成功".equals(msg)) {
                String type = json.getString("type");
                JSONObject data1 = json.getJSONObject("data");
                if ("视频".equals(type)) {
                    String cover = data1.getString("cover");
                    String video = data1.getString("video");
                    String title = data1.getString("desc");
                    String lyric = data1.getJSONObject("origin_music").optString("lyric");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n浏览数:" + count.getLong("view") +
                        "\n歌词:" + lyric + "\n视频发送中...");
                    sendVideo(msgData, video);
                } else if ("语音".equals(type)) {
                    String cover = data1.getString("cover");
                    String audio = data1.getString("audio");
                    String title = data1.getString("desc");
                    String lyric = data1.optString("lyric");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "[pic=" + cover + "]\n标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n浏览数:" + count.getLong("view") +
                        "\n歌词:" + lyric + "\n语音发送中...");
                    sendPtt(msgData, audio);
                } else if ("文字".equals(type)) {
                    String title = data1.getString("desc");
                    JSONObject count = data1.getJSONObject("count");
                    String author = data1.getJSONObject("author").getString("name");
                    sendMsg(msgData, "标题:" + title + "\n作者:" + author +
                        "\n喜欢数:" + count.getLong("like") + "\n评论数:" + count.getLong("comment") +
                        "\n分享数:" + count.getLong("share") + "\n浏览数:" + count.getLong("view")
                    );
                }
            } else {
                sendMsg(msgData, sj);
            }
            return;
        }

        if (text.contains("https://mp.weixin.qq.com/s/")) {
            String sl = findRealUrl(text);
            String url = MY_WEB + "wxjx.php?url=" + urlEncode(sl);
            String sj = HttpUtils.get(url);
            if (sj == null || sj.isEmpty()) {
                sendMsg(msgData, "请求服务器出错");
                return;
            }
            JSONObject json = new JSONObject(sj);
            String msg = json.optString("msg");
            if ("获取成功".equals(msg)) {
                String type = json.getString("type");
                JSONObject data1 = json.getJSONObject("data");
                String title = data1.getString("title");
                String desc = data1.getString("desc");
                if ("视频".equals(type)) {
                    JSONArray ttp = data1.getJSONArray("video");
                    sendMsg(msgData, "标题:" + title + "\n文章内容:\n" + desc);
                    if (ttp.length() > 1 || !ttp.getString(0).contains(".mp4")) {
                        StringBuilder sb = new StringBuilder("视频列表:\n");
                        for (int i = 0; i < ttp.length(); i++) {
                            sb.append(ttp.getString(i)).append("\n");
                        }
                        sendMsg(msgData, sb.toString());
                    } else {
                        sendVideo(msgData, ttp.getString(0));
                    }
                } else if ("图集".equals(type)) {
                    JSONArray ttp = data1.getJSONArray("images");
                    StringBuilder sb = new StringBuilder("图片来咯～");
                    for (int i = 0; i < ttp.length(); i++) {
                        sb.append("[pic=").append(ttp.getString(i)).append("]");
                    }
                    sb.append("标题:").append(title).append("\n文章内容:\n").append(desc);
                    sendMsg(msgData, sb.toString());
                } else if ("图集and视频".equals(type)) {
                    JSONArray ttp = data1.getJSONArray("images");
                    StringBuilder sb = new StringBuilder("图片来咯～");
                    for (int i = 0; i < ttp.length(); i++) {
                        sb.append("[pic=").append(ttp.getString(i)).append("]");
                    }
                    sb.append("标题:").append(title).append("\n文章内容:\n").append(desc);
                    sendMsg(msgData, sb.toString());
                    JSONArray ttp2 = data1.getJSONArray("video");
                    if (ttp2.length() > 1 || !ttp2.getString(0).contains(".mp4")) {
                        StringBuilder sb2 = new StringBuilder("视频列表:\n");
                        for (int i = 0; i < ttp2.length(); i++) {
                            sb2.append(ttp2.getString(i)).append("\n");
                        }
                        sendMsg(msgData, sb2.toString());
                    } else {
                        sendVideo(msgData, ttp2.getString(0));
                    }
                }
            } else {
                sendMsg(msgData, sj);
            }
            return;
        }
    }

    private static String findRealUrl(String text) {
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(https?://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])")
                .matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LogUtils.e(e);
        }
        return text;
    }

    private static String fetchRedirectUrl(String urlStr) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 10; MI 9 Build/QKQ1.190825.002; wv) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/87.0.4280.101 " +
                "Mobile Safari/537.36");
            conn.connect();
            String redirect = conn.getHeaderField("Location");
            conn.disconnect();
            if (redirect != null && !redirect.isEmpty()) {
                return redirect;
            }
        } catch (Exception e) {
            LogUtils.e(e);
        }
        return urlStr;
    }

    private static String urlEncode(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return str;
        }
    }

    private static void sendMsg(MsgData msgData, String text) {
        ColdRainCore.getInstance().reply(msgData, text);
    }

    private static void sendImg(MsgData msgData, String imgUrl) {
        try {
            if (imgUrl == null || imgUrl.isEmpty()) return;
            MsgTool.sendPic(msgData.peerUin, imgUrl, msgData.type);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    private static void sendVideo(MsgData msgData, String videoUrl) {
        try {
            if (videoUrl == null || videoUrl.isEmpty()) return;
            MsgTool.sendVideo(msgData.peerUin, videoUrl, msgData.type);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    private static void sendPtt(MsgData msgData, String pttUrl) {
        try {
            if (pttUrl == null || pttUrl.isEmpty()) return;
            MsgTool.sendPtt(msgData.peerUin, pttUrl, msgData.type);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }
}
