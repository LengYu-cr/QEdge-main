package me.lengyu.qedge.coldrain.features;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import me.lengyu.qedge.utils.qq.CookieTool;
import me.lengyu.qedge.utils.qq.ExtraTool;
import me.lengyu.qedge.utils.qq.MsgTool;
/**
 * @Author 冷雨
 * @Description 音乐菜单处理
 */
public class MusicMenuFeature implements ColdRainFeature {

    private static final String MY_WEB = "https://api.yuafeng.cn/API/ly/";
    private static final String SECRET = "lengyu520";

    private static final Map<String, MusicSearchResult> searchMusicList = new HashMap<>();

    static class MusicSearchResult {
        String qun;
        String uin;
        String type;
        JSONArray json;

        MusicSearchResult(String qun, String uin, String type, JSONArray json) {
            this.qun = qun;
            this.uin = uin;
            this.type = type;
            this.json = json;
        }
    }

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        if (text.equals("音乐菜单")) return true;
        if (text.equals("随机音乐")) return true;
        if (text.equals("随机网易")) return true;
        if (text.equals("逆天语音")) return true;
        if (text.equals("坤坤音乐")) return true;
        if (text.equals("取消点歌")) return true;
        if (text.equals("切换卡片点歌")) return true;
        if (text.equals("切换语音点歌")) return true;
        if (text.equals("切换链接点歌")) return true;
        if (text.equals("切换文件点歌")) return true;
        if (text.equals("切换下载点歌")) return true;
        if (text.equals("切换空间点歌")) return true;
        if (text.equals("切换播放点歌")) return true;
        if (text.startsWith("点歌")) return true;
        if (text.startsWith("QQ点歌")) return true;
        if (text.startsWith("网易点歌")) return true;
        if (text.startsWith("酷狗点歌")) return true;
        if (text.startsWith("酷我点歌")) return true;
        if (text.startsWith("汽水点歌")) return true;
        if (text.matches("[0-9]+") && text.length() <= 2) {
            String key = msgData.peerUin + "_" + msgData.userUin;
            return searchMusicList.containsKey(key);
        }
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        final ColdRainCore finalCore = core;
        final MsgData finalMsgData = msgData;
        ModuleScope.launchIOJava("MusicMenuFeature", () -> {
                try {
                    String text = finalMsgData.msg.trim();
                    String qun = finalMsgData.peerUin;
                    String uin = finalMsgData.userUin;
                    int mtype = finalMsgData.type;

                    if (text.equals("音乐菜单")) {
                        boolean enabled;
                        if (mtype == 2 && qun != null && !qun.isEmpty()) {
                            enabled = finalCore.isGroupFeatureEnabled("feature_music", qun);
                        } else {
                            enabled = finalCore.isFeatureEnabled("feature_music");
                        }
                        if (enabled) {
                            String menu = "音乐菜单:\n随机音乐 随机网易\n逆天语音 坤坤音乐\n取消点歌\n点歌+歌曲\nQQ点歌+歌曲(vip)\n网易点歌+歌曲(vip)\n酷狗点歌+歌曲(vip)\n酷我点歌+歌曲(vip)\n汽水点歌+歌曲(vip)\n切换卡片/语音/链接/文件/下载/空间/播放点歌";
                            finalCore.reply(finalMsgData, menu);
                        } else {
                            finalCore.reply(finalMsgData, "本聊天未开启音乐菜单");
                        }
                        return;
                    }

                    if (!finalCore.isFeatureEnabled("feature_music")) {
                        if (mtype == 2 && qun != null && !qun.isEmpty()) {
                            if (!finalCore.isGroupFeatureEnabled("feature_music", qun)) {
                                return;
                            }
                        } else {
                            return;
                        }
                    }

                    String yyms = getMusicMode(qun);
                    if (yyms.isEmpty()) yyms = "卡片";

                    if (text.equals("逆天语音")) {
                        String url = fetchRedirectUrl(MY_WEB + "sjyy.php");
                        if (url != null && !url.isEmpty()) {
                            MsgTool.sendPtt(qun, url, mtype);
                        }
                        return;
                    }

                    if (text.equals("坤坤音乐")) {
                        MsgTool.sendPtt(qun, MY_WEB + "kun.php", mtype);
                        return;
                    }

                    if (text.equals("随机音乐")) {
                        randomMusic(qun, mtype, yyms, finalCore, finalMsgData);
                        return;
                    }

                    if (text.equals("随机网易")) {
                        randomNetease(qun, mtype, yyms, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("点歌")) {
                        searchQQMusicDirect(qun, uin, text.substring(2), mtype, yyms, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("QQ点歌")) {
                        searchQQMusic(qun, uin, text.substring(4), mtype, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("网易点歌")) {
                        searchNeteaseMusic(qun, uin, text.substring(4), mtype, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("酷狗点歌")) {
                        searchKugouMusic(qun, uin, text.substring(4), mtype, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("酷我点歌")) {
                        searchKuwoMusic(qun, uin, text.substring(4), mtype, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("汽水点歌")) {
                        searchQishuiMusic(qun, uin, text.substring(4), mtype, finalCore, finalMsgData);
                        return;
                    }

                    if (text.matches("[0-9]+") && text.length() <= 2) {
                        selectMusic(qun, uin, Integer.parseInt(text), mtype, yyms, finalCore, finalMsgData);
                        return;
                    }

                    if (text.equals("取消点歌")) {
                        searchMusicList.remove(qun + "_" + uin);
                        finalCore.reply(finalMsgData, "已清除您的所有点歌缓存数据");
                        return;
                    }

                    if (text.startsWith("切换")) {
                        String mode = text.replace("切换", "").replace("点歌", "");
                        setMusicMode(qun, mode);
                        finalCore.reply(finalMsgData, "已切换为" + mode + "点歌");
                    }
                } catch (Throwable e) {
                    LogUtils.e("MusicMenuFeature", "error: " + e.getMessage());
                }
        });
    }

    private void randomMusic(String qun, int mtype, String yyms, ColdRainCore core, MsgData msgData) {
        try {
            String url = HttpUtils.get(MY_WEB + "randomMusic.php?mode=Share&id=6283364726&type=中品质");
            if (url == null || url.isEmpty() || url.equals("访问网页失败")) {
                core.reply(msgData, url != null ? url : "获取失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            if (json.getInt("code") == 0) {
                JSONObject data = json.getJSONObject("data");
                String music = data.getString("music");
                String song = data.getString("song");
                String singer = data.getString("singer");
                String cover = data.getString("cover");
                String link = "https://i.y.qq.com/v8/playsong.html?songmid=" + data.get("mid");
                sendMusic(qun, song, singer, link, music, cover, "QQ", yyms, mtype, core, msgData);
            } else {
                core.reply(msgData, json.optString("msg"));
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "randomMusic error: " + e.getMessage());
        }
    }

    private void randomNetease(String qun, int mtype, String yyms, ColdRainCore core, MsgData msgData) {
        try {
            String url = HttpUtils.get(MY_WEB + "randomNetease.php?select=随机");
            if (url == null || url.isEmpty()) return;
            JSONObject json = new JSONObject(url);
            String dataStr = json.getString("data");
            JSONObject data = new JSONObject(dataStr);
            String song = data.getString("song");
            String music = data.getString("music");
            String cover = data.getString("cover");
            String singer = data.getString("singer");
            String link = "https://y.music.163.com/m/song?id=" + data.getString("id");
            sendMusic(qun, song, singer, link, music, cover, "网易", yyms, mtype, core, msgData);
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "randomNetease error: " + e.getMessage());
        }
    }

    private void searchQQMusicDirect(String qun, String uin, String keyword, int mtype, String yyms, ColdRainCore core, MsgData msgData) {
        try {
            if (keyword.isEmpty()) {
                core.reply(msgData, "[atUin=" + uin + "]\n不是，你怎么连歌名都不输入！");
                return;
            }
            String myUin = QQCurrentEnv.getCurrentUin();
            String pskey = CookieTool.getPskey("y.qq.com");
            String skey = CookieTool.getSkey();
            if (pskey == null || skey == null) {
                core.reply(msgData, "无法获取QQ音乐cookie");
                return;
            }
            String cookie = "uin=" + QQCurrentEnv.getCookieUin() + "; skey=" + skey + "; p_uin=" + QQCurrentEnv.getCookieUin() + "; p_skey=" + pskey;
            String dataJson = "{\"comm\":{\"format\":\"json\",\"inCharset\":\"utf-8\",\"outCharset\":\"utf-8\",\"notice\":0,\"platform\":\"h5\",\"needNewCode\":1,\"ct\":23,\"cv\":0},\"req_0\":{\"method\":\"DoSearchForQQMusicDesktop\",\"module\":\"music.search.SearchCgiService\",\"param\":{\"remoteplace\":\"txt.mqq.all\",\"search_type\":0,\"query\":\"" + keyword + "\",\"page_num\":1,\"num_per_page\":20}}}";
            String url = HttpUtils.postWithCookie("https://u.y.qq.com/cgi-bin/musicu.fcg?_webcgikey=DoSearchForQQMusicDesktop", dataJson, cookie);
            if (url == null || url.isEmpty()) {
                core.reply(msgData, "搜索失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            String code = json.optString("code");
            if (!code.equals("0")) {
                core.reply(msgData, "搜索失败");
                return;
            }
            String req_0 = json.getString("req_0");
            JSONObject json1 = new JSONObject(req_0);
            String data1 = json1.getString("data");
            JSONObject json2 = new JSONObject(data1);
            String body = json2.getString("body");
            JSONObject json3 = new JSONObject(body);
            String song = json3.getString("song");
            JSONObject json4 = new JSONObject(song);
            String list = json4.getString("list");
            if (list.length() < 5) {
                core.reply(msgData, "[atUin=" + uin + "]\n歌曲未搜索到");
                return;
            }
            JSONArray infos = json4.getJSONArray("list");
            JSONObject item = infos.getJSONObject(0);
            String mid = item.getString("mid");
            String album = item.getString("album");
            JSONObject albumObj = new JSONObject(album);
            String albumId = albumObj.getString("pmid");
            String coverUrl = "http://y.gtimg.cn/music/photo_new/T002R500x500M000" + albumId + ".jpg";
            String name = item.getString("name");
            JSONArray singers = item.getJSONArray("singer");
            StringBuilder singerBuilder = new StringBuilder();
            for (int i = 0; i < singers.length(); i++) {
                singerBuilder.append(singers.getJSONObject(i).getString("name")).append(" ");
            }
            String singer = singerBuilder.toString().trim();
            String link = "https://i.y.qq.com/v8/playsong.html?songmid=" + mid + "&senderName=ColdRain";

            String url2 = HttpUtils.get(MY_WEB + "qqmusicu.php?mid=" + mid + "&adaptive=1&type=中品质&sign=" + md5(SECRET + mid));
            if (url2 != null && !url2.isEmpty() && !url2.equals("访问网页失败")) {
                JSONObject musicJson = new JSONObject(url2);
                if (musicJson.getInt("code") == 0) {
                    JSONObject musicData = musicJson.getJSONObject("data");
                    String music = musicData.getString("music");
                    sendMusic(qun, name, singer, link, music, coverUrl, "QQ", yyms, mtype, core, msgData);
                    return;
                }
            }
            core.reply(msgData, "获取音乐直链失败\n『" + name + "——" + singer + "』\n" + link);
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "searchQQMusicDirect error: " + e.getMessage());
        }
    }

    private void searchQQMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            searchMusicList.remove(key);
            String url = HttpUtils.get(MY_WEB + "qqqmusic.php?msg=" + URLEncoder.encode(keyword, "UTF-8") + "&adaptive=1&num=15");
            if (url == null || url.isEmpty() || url.equals("访问网页失败")) {
                core.reply(msgData, url != null ? url : "搜索失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            if (json.getInt("code") == 0) {
                JSONArray data = json.getJSONArray("data");
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    String songname = item.getString("title");
                    String singer = item.getString("singer");
                    result.append((i + 1)).append("、").append(songname).append("——").append(singer).append("\n");
                }
                searchMusicList.put(key, new MusicSearchResult(qun, uin, "QQ", data));
                core.reply(msgData, result.toString() + "请发送序号选择\n不想点歌请发\"取消点歌\"");
            } else {
                core.reply(msgData, json.optString("msg"));
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "searchQQMusic error: " + e.getMessage());
        }
    }

    private void searchNeteaseMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            searchMusicList.remove(key);
            String url = HttpUtils.get(MY_WEB + "wymusic.php?msg=" + URLEncoder.encode(keyword, "UTF-8") + "&adaptive=1&num=15");
            if (url == null || url.isEmpty() || url.equals("访问网页失败")) {
                core.reply(msgData, url != null ? url : "搜索失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            if (json.getInt("code") == 0) {
                JSONArray data = json.getJSONArray("data");
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    String songname = item.getString("song");
                    String singer = item.getString("singer");
                    result.append((i + 1)).append("、").append(songname).append("——").append(singer).append("\n");
                }
                searchMusicList.put(key, new MusicSearchResult(qun, uin, "网易", data));
                core.reply(msgData, result.toString() + "请发送序号选择\n不想点歌请发\"取消点歌\"");
            } else {
                core.reply(msgData, json.optString("msg"));
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "searchNeteaseMusic error: " + e.getMessage());
        }
    }

    private void searchKugouMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            searchMusicList.remove(key);
            String url = HttpUtils.get(MY_WEB + "kgmusic.php?msg=" + URLEncoder.encode(keyword, "UTF-8") + "&adaptive=1&num=15");
            if (url == null || url.isEmpty() || url.equals("访问网页失败")) {
                core.reply(msgData, url != null ? url : "搜索失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            if (json.getInt("code") == 0) {
                JSONArray data = json.getJSONArray("data");
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    String songname = item.getString("song");
                    String singer = item.getString("singer");
                    result.append((i + 1)).append("、").append(songname).append("——").append(singer).append("\n");
                }
                searchMusicList.put(key, new MusicSearchResult(qun, uin, "酷狗", data));
                core.reply(msgData, result.toString() + "请发送序号选择\n不想点歌请发\"取消点歌\"");
            } else {
                core.reply(msgData, json.optString("msg"));
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "searchKugouMusic error: " + e.getMessage());
        }
    }

    private void searchKuwoMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            searchMusicList.remove(key);
            String url = HttpUtils.get("https://search.kuwo.cn/r.s?client=kt&all=" + URLEncoder.encode(keyword, "UTF-8") + "&pn=0&rn=20&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1&_=" + System.currentTimeMillis());
            if (url == null || url.isEmpty() || url.equals("访问网页失败")) {
                core.reply(msgData, url != null ? url : "搜索失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            String code = json.optString("HIT_BUT_OFFLINE");
            if (code.equals("0")) {
                JSONArray data = json.getJSONArray("abslist");
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    String songname = item.getString("NAME");
                    String singer = item.getString("ARTIST");
                    result.append((i + 1)).append("、").append(songname).append("——").append(singer).append("\n");
                }
                searchMusicList.put(key, new MusicSearchResult(qun, uin, "酷我", data));
                core.reply(msgData, result.toString() + "请发送序号选择\n不想点歌请发\"取消点歌\"");
            } else {
                core.reply(msgData, "搜索不到，请重试");
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "searchKuwoMusic error: " + e.getMessage());
        }
    }

    private void searchQishuiMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            searchMusicList.remove(key);
            String url = HttpUtils.get(MY_WEB + "qsmusic.php?msg=" + URLEncoder.encode(keyword, "UTF-8") + "&num=15");
            if (url == null || url.isEmpty() || url.equals("访问网页失败")) {
                core.reply(msgData, url != null ? url : "搜索失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            if (json.getInt("code") == 0) {
                JSONArray data = json.getJSONArray("data");
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    String songname = item.getString("song");
                    String singer = item.getString("singer");
                    result.append((i + 1)).append("、").append(songname).append("——").append(singer).append("\n");
                }
                searchMusicList.put(key, new MusicSearchResult(qun, uin, "汽水", data));
                core.reply(msgData, result.toString() + "请发送序号选择\n不想点歌请发\"取消点歌\"");
            } else {
                core.reply(msgData, json.optString("msg"));
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "searchQishuiMusic error: " + e.getMessage());
        }
    }

    private void selectMusic(String qun, String uin, int index, int mtype, String yyms, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            MusicSearchResult result = searchMusicList.get(key);
            if (result == null || index <= 0) return;
            index--;
            if (index >= result.json.length()) return;

            if ("QQ".equals(result.type)) {
                String mid = result.json.getJSONObject(index).optString("mid");
                String url = HttpUtils.get(MY_WEB + "qqmusicu.php?mid=" + mid + "&adaptive=1&type=中品质&sign=" + md5(SECRET + mid));
                if (url != null && !url.isEmpty() && !url.equals("访问网页失败")) {
                    JSONObject json = new JSONObject(url);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        String music = data.getString("music");
                        String song = data.getString("title");
                        String singer = data.getString("singer");
                        String cover = data.getString("cover");
                        String link = "https://i.y.qq.com/v8/playsong.html?songmid=" + data.get("mid");
                        sendMusic(qun, song, singer, link, music, cover, "QQ", yyms, mtype, core, msgData);
                    }
                }
            } else if ("网易".equals(result.type)) {
                String id = result.json.getJSONObject(index).optString("id");
                String url = HttpUtils.get(MY_WEB + "wyjx.php?id=" + id);
                if (url != null && !url.isEmpty() && !url.equals("访问网页失败")) {
                    JSONObject json = new JSONObject(url);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        String music = data.getString("music");
                        String song = data.getString("song");
                        String singer = data.getString("singer");
                        String cover = data.getString("cover");
                        String link = "https://y.music.163.com/m/song?id=" + data.get("id");
                        sendMusic(qun, song, singer, link, music, cover, "网易", yyms, mtype, core, msgData);
                    }
                }
            } else if ("酷狗".equals(result.type)) {
                String hash = result.json.getJSONObject(index).optString("hash");
                String url = HttpUtils.get(MY_WEB + "kgmusic.php?hash=" + hash);
                if (url != null && !url.isEmpty() && !url.equals("访问网页失败")) {
                    JSONObject json = new JSONObject(url);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        String music = data.getString("music");
                        String song = data.getString("song");
                        String singer = data.getString("singer");
                        String cover = data.getString("cover");
                        String link = data.getString("link");
                        sendMusic(qun, song, singer, link, music, cover, "酷狗", yyms, mtype, core, msgData);
                    }
                }
            } else if ("酷我".equals(result.type)) {
                String id = result.json.getJSONObject(index).optString("DC_TARGETID");
                String url = HttpUtils.get(MY_WEB + "kwmusic.php?type=320kmp3&id=" + id);
                if (url != null && !url.isEmpty() && !url.equals("访问网页失败")) {
                    JSONObject json = new JSONObject(url);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        String music = data.getString("music").replace("$", "=");
                        String song = data.getString("song");
                        String singer = data.getString("singer");
                        String cover = data.getString("cover");
                        String link = "http://m.kuwo.cn/newh5app/play_detail/" + id;
                        sendMusic(qun, song, singer, link, music, cover, "波点", yyms, mtype, core, msgData);
                    }
                }
            } else if ("汽水".equals(result.type)) {
                JSONObject item = result.json.getJSONObject(index);
                String music = item.getString("music");
                String song = item.getString("song");
                String singer = item.getString("singer");
                String cover = item.getString("cover");
                sendMusic(qun, song, singer, music, music, cover, "波点", yyms, mtype, core, msgData);
            }
            searchMusicList.remove(key);
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "selectMusic error: " + e.getMessage());
        }
    }

    private void sendMusic(String qun, String song, String singer, String link, String music, String cover, String app, String type, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            music = music.replace("&API=ly.aa.cab", "");
            
            if ("文件".equals(type)) {
                String filePath = QQCurrentEnv.getCurrentDir() + "下载/" + song + ".m4a";
                downloadAndSendFile(qun, music, filePath, song, singer, cover, mtype, core, msgData);
            } else if ("卡片".equals(type) || type.isEmpty()) {
                String platform = getPlatformCode(app);
                if (mtype == 2) {
                    switch (platform) {
                        case "qq": ExtraTool.qqsendTroopMusic(qun, song, singer, link, music, cover); break;
                        case "wy": ExtraTool.wysendTroopMusic(qun, song, singer, link, music, cover); break;
                        case "kg": ExtraTool.kgsendTroopMusic(qun, song, singer, link, music, cover); break;
                        case "kw": ExtraTool.kwsendTroopMusic(qun, song, singer, link, music, cover); break;
                        case "bd": ExtraTool.bdsendTroopMusic(qun, song, singer, link, music, cover); break;
                        case "mg": ExtraTool.mgsendTroopMusic(qun, song, singer, link, music, cover); break;
                        case "QQ": ExtraTool.qqsendTroopMusic(qun, song, singer, link, music, cover); break;
                        default: ExtraTool.qqsendTroopMusic(qun, song, singer, link, music, cover);
                    }
                } else {
                    switch (platform) {
                        case "qq": ExtraTool.qqsendFriendMusic(qun, song, singer, link, music, cover); break;
                        case "wy": ExtraTool.wysendFriendMusic(qun, song, singer, link, music, cover); break;
                        case "kg": ExtraTool.kgsendFriendMusic(qun, song, singer, link, music, cover); break;
                        case "kw": ExtraTool.kwsendFriendMusic(qun, song, singer, link, music, cover); break;
                        case "bd": ExtraTool.bdsendFriendMusic(qun, song, singer, link, music, cover); break;
                        case "mg": ExtraTool.mgsendFriendMusic(qun, song, singer, link, music, cover); break;
                        case "QQ": ExtraTool.qqsendFriendMusic(qun, song, singer, link, music, cover); break;
                        default: ExtraTool.qqsendFriendMusic(qun, song, singer, link, music, cover);
                    }
                }
            } else if ("语音".equals(type)) {
                MsgTool.sendPtt(qun, music, mtype);
            } else if ("链接".equals(type)) {
                core.reply(msgData, "[pic=" + cover + "]\n" + song + "——" + singer + "\n" + music.replace("\\/", "/"));
            } else if ("下载".equals(type)) {
                String filePath = QQCurrentEnv.getCurrentDir() + "下载/" + song + ".m4a";
                downloadAndSendFile(qun, music, filePath, song, singer, cover, mtype, core, msgData);
            } else {
                core.reply(msgData, "『" + song + "——" + singer + "』\n" + music);
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "sendMusic error: " + e.getMessage());
            core.reply(msgData, "音乐发送失败: " + e.getMessage());
        }
    }
    
    private String getPlatformCode(String app) {
        if ("QQ".equals(app) || "qq".equals(app)) return "qq";
        if ("网易".equals(app) || "wy".equals(app)) return "wy";
        if ("酷狗".equals(app) || "kg".equals(app)) return "kg";
        if ("酷我".equals(app) || "波点".equals(app) || "kw".equals(app)) return "kw";
        if ("汽水".equals(app) || "bd".equals(app)) return "bd";
        if ("咪咕".equals(app) || "mg".equals(app)) return "mg";
        return "qq";
    }
    
    private void downloadAndSendFile(String qun, String url, String filePath, String song, String singer, String cover, int mtype, ColdRainCore core, MsgData msgData) {
        ModuleScope.launchIOJava("MusicMenuFeature", () -> {
                try {
                    String resultPath = HttpUtils.download(url, filePath);
                    if (resultPath != null && new java.io.File(resultPath).exists()) {
                        core.reply(msgData, "[pic=" + cover + "]\n" + song + "——" + singer + "\n\n已下载到" + resultPath);
                    } else {
                        core.reply(msgData, "下载失败");
                    }
                } catch (Throwable e) {
                    LogUtils.e("MusicMenuFeature", "downloadAndSendFile error: " + e.getMessage());
                    core.reply(msgData, "下载失败");
                }
        });
    }

    private String getMusicMode(String qun) {
        try {
            String dir = QQCurrentEnv.getCurrentDir();
            java.io.File file = new java.io.File(dir + "/data/" + qun + "点歌模式.txt");
            if (file.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
                String mode = reader.readLine();
                reader.close();
                return mode != null ? mode : "";
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "getMusicMode error: " + e.getMessage());
        }
        return "";
    }

    private void setMusicMode(String qun, String mode) {
        try {
            String dir = QQCurrentEnv.getCurrentDir() + "/data";
            java.io.File dataDir = new java.io.File(dir);
            if (!dataDir.exists()) dataDir.mkdirs();
            java.io.FileWriter writer = new java.io.FileWriter(dir + "/" + qun + "点歌模式.txt");
            writer.write(mode);
            writer.close();
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "setMusicMode error: " + e.getMessage());
        }
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Throwable e) {
            return "";
        }
    }

    private String fetchRedirectUrl(String url) {
        try {
            java.net.URL imageUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) imageUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setFollowRedirects(true);
            String redirectUrl = connection.getHeaderField("Location");
            connection.disconnect();
            if (redirectUrl == null) {
                return connection.getURL().toString();
            } else {
                return redirectUrl;
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "fetchRedirectUrl error: " + e.getMessage());
            return url;
        }
    }
}