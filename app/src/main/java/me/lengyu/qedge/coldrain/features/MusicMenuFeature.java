package me.lengyu.qedge.coldrain.features;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.Toasts;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import me.lengyu.qedge.utils.qq.ExtraTool;
import me.lengyu.qedge.utils.qq.MsgTool;
/**
 * @Author 冷雨
 * @Description 音乐菜单处理
 */
public class MusicMenuFeature implements ColdRainFeature {

    /** 旧接口通道（抖音点歌、逆天语音等） */
    private static final String MY_WEB = "https://api.yuafeng.cn/API/ly/";
    /** 音乐接口地址（赞助通道）：QQ/酷我/酷狗/网易云/咪咕/汽水，请求需带 sponsor_key */
    private static final String API_V1 = "https://v.yuafeng.cn/QEdge/api/internal/";

    private static final Map<String, MusicSearchResult> searchMusicList = new HashMap<>();

    /** 平台别名 -> 音质配置 key */
    private static final Map<String, String> QUALITY_ALIAS = new LinkedHashMap<>();
    /** 平台 key -> 显示名 */
    private static final Map<String, String> QUALITY_NAME = new LinkedHashMap<>();
    /** 平台 key -> 音质表（音质值 -> 说明） */
    private static final Map<String, LinkedHashMap<String, String>> QUALITY_TABLE = new LinkedHashMap<>();
    /** 未主动设置时使用的音质（保持原有默认行为），为空表示用接口默认音质 */
    private static final Map<String, String> QUALITY_DEFAULT = new LinkedHashMap<>();

    static {
        QUALITY_ALIAS.put("QQ", "qq");
        QUALITY_ALIAS.put("qq", "qq");
        QUALITY_ALIAS.put("网易云", "wy");
        QUALITY_ALIAS.put("网易", "wy");
        QUALITY_ALIAS.put("酷狗", "kg");
        QUALITY_ALIAS.put("酷我", "kw");
        QUALITY_ALIAS.put("咪咕", "mg");

        QUALITY_NAME.put("qq", "QQ");
        QUALITY_NAME.put("wy", "网易云");
        QUALITY_NAME.put("kg", "酷狗");
        QUALITY_NAME.put("kw", "酷我");
        QUALITY_NAME.put("mg", "咪咕");

        QUALITY_TABLE.put("qq", quality(
                "1", "OGG 96k",
                "2", "OGG 192k",
                "3", "OGG 320k",
                "4", "OGG 640k",
                "5", "OGG 臻品全景声 3.0",
                "6", "M4A 48k",
                "7", "M4A 96k",
                "8", "M4A 192k",
                "9", "MP3 128k",
                "10", "MP3 320k",
                "-1", "试听片段",
                "12", "无损 FLAC",
                "13", "Hi-Res FLAC",
                "14", "臻品母带",
                "15", "臻品全景声 1.0",
                "16", "臻品全景声 2.0"));
        QUALITY_TABLE.put("wy", quality(
                "standard", "MP3 128k",
                "higher", "MP3 192k",
                "exhigh", "MP3 320k",
                "lossless", "无损 FLAC",
                "hires", "Hi-Res FLAC",
                "jyeffect", "高清环绕声",
                "dolby", "杜比全景声",
                "jymaster", "超清母带"));
        QUALITY_TABLE.put("kg", quality(
                "128", "MP3 128k",
                "320", "MP3 320k",
                "flac", "无损 FLAC",
                "hires", "臻品母带 FLAC"));
        QUALITY_TABLE.put("kw", quality(
                "100kogg", "OGG 96k",
                "192kogg", "OGG 192k",
                "300kogg", "OGG 320k",
                "48kaac", "M4A 48k",
                "128kmp3", "MP3 128k",
                "320kmp3", "MP3 320k",
                "2000kflac", "无损 FLAC",
                "20900kmflac", "臻品母带 FLAC"));
        QUALITY_TABLE.put("mg", quality(
                "128k", "MP3 128k",
                "320k", "MP3 320k",
                "flac", "无损 FLAC",
                "flac24bit", "Hi-Res FLAC",
                "hires", "Hi-Res FLAC",
                "atmos", "全景声 WAV",
                "master", "母带 WAV"));

        QUALITY_DEFAULT.put("kw", "320kmp3");
    }

    private static LinkedHashMap<String, String> quality(String... pairs) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

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
        if (text.startsWith("QQ点歌")) return true;
        if (text.startsWith("网易点歌")) return true;
        if (text.startsWith("酷狗点歌")) return true;
        if (text.startsWith("酷我点歌")) return true;
        if (text.startsWith("咪咕点歌")) return true;
        if (text.startsWith("汽水点歌")) return true;
        if (text.startsWith("抖音点歌")) return true;
        if (text.startsWith("设置") && text.contains("音质")) return true;
        if (text.startsWith("查看") && text.contains("音质")) return true;
        if (text.startsWith("配置赞助密钥")) return true;
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
                            String menu = "音乐菜单:\n随机音乐 随机网易\n逆天语音 坤坤音乐\n取消点歌\nQQ点歌+歌曲(vip)\n网易点歌+歌曲(vip)\n酷狗点歌+歌曲(vip)\n酷我点歌+歌曲(vip)\n咪咕点歌+歌曲(vip)\n汽水点歌+歌曲\n抖音点歌+歌曲\n设置(QQ/网易/酷狗/酷我/咪咕)音质10\n查看(QQ/网易/酷狗/酷我/咪咕)音质列表\n切换卡片/语音/链接/文件/下载/空间/播放点歌\n配置赞助密钥xxxx(赞助20后在后台获取)";
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

                    if (text.startsWith("配置赞助密钥")) {
                        String key = text.substring("配置赞助密钥".length()).trim();
                        if (key.isEmpty()) {
                            finalCore.reply(finalMsgData, "格式：配置赞助密钥xxxxx\n密钥请到用户后台申请（赞助20）");
                            return;
                        }
                        setSponsorKey(key, finalCore, finalMsgData);
                        return;
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

                    if (text.startsWith("设置") && text.contains("音质")) {
                        setMusicQuality(qun, text, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("查看") && text.contains("音质")) {
                        showMusicQuality(qun, text, finalCore, finalMsgData);
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

                    if (text.startsWith("咪咕点歌")) {
                        searchMiguMusic(qun, uin, text.substring(4), mtype, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("汽水点歌")) {
                        searchQishuiMusic(qun, uin, text.substring(4), mtype, finalCore, finalMsgData);
                        return;
                    }

                    if (text.startsWith("抖音点歌")) {
                        searchDouyinMusic(qun, uin, text.substring(4), mtype, finalCore, finalMsgData);
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

    /** 快捷点歌搜索结果（供聊天页弹窗使用） */
    public static class QuickSong {
        public final String mid;
        public final String title;
        public final String singer;
        public final String cover;

        public QuickSong(String mid, String title, String singer, String cover) {
            this.mid = mid;
            this.title = title;
            this.singer = singer;
            this.cover = cover == null ? "" : cover;
        }
    }

    /** 快捷点歌：搜索QQ音乐，失败时抛异常由调用方提示 */
    public static java.util.List<QuickSong> quickSearchQQ(String keyword) throws Exception {
        String url = HttpUtils.get(API_V1 + "qqmusic.php?select=search&msg="
                + URLEncoder.encode(keyword, "UTF-8") + "&num=15" + sponsorParam());
        if (url == null || url.isEmpty() || url.equals("访问网页失败")) {
            throw new Exception(url == null || url.isEmpty() ? "搜索失败" : url);
        }
        JSONObject json = new JSONObject(url);
        if (json.getInt("code") != 0) {
            throw new Exception(json.optString("msg", "搜索失败"));
        }
        JSONArray data = json.getJSONArray("data");
        java.util.List<QuickSong> songs = new java.util.ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            String cover = item.optString("cover", "");
            if (cover.isEmpty()) {
                // 搜索接口只有专辑 mid 时按 QQ 音乐封面规则拼接
                String albumMid = item.optString("albummid", item.optString("albumMid", ""));
                if (!albumMid.isEmpty()) {
                    cover = "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albumMid + ".jpg";
                }
            }
            songs.add(new QuickSong(item.optString("mid"), item.optString("title"),
                    item.optString("singer"), cover));
        }
        return songs;
    }

    /** 快捷点歌：QQ音乐可选音质（音质值 -> 说明） */
    public static LinkedHashMap<String, String> quickQQQualityTable() {
        LinkedHashMap<String, String> table = QUALITY_TABLE.get("qq");
        return table == null ? new LinkedHashMap<>() : new LinkedHashMap<>(table);
    }

    /** 快捷点歌：按指定音质取直链并以音乐卡片发送到当前聊天 */
    public static void quickSendQQ(String peerUin, int chatType, QuickSong song, String quality) throws Exception {
        String qualityPart = (quality == null || quality.isEmpty()) ? "" : "&type=" + quality;
        String url = HttpUtils.get(API_V1 + "qqmusic.php?select=play&mid=" + song.mid + "&adaptive=1" + qualityPart + sponsorParam());
        if (url == null || url.isEmpty() || url.equals("访问网页失败")) {
            throw new Exception(url == null || url.isEmpty() ? "获取音乐直链失败" : url);
        }
        JSONObject json = new JSONObject(url);
        if (json.getInt("code") != 0) {
            throw new Exception(json.optString("msg", "获取音乐直链失败"));
        }
        JSONObject data = json.getJSONObject("data");
        String music = data.optString("music", "").replace("&API=ly.aa.cab", "");
        if (music.isEmpty()) {
            throw new Exception("获取音乐直链失败");
        }
        String title = data.optString("title", song.title);
        String singer = data.optString("singer", song.singer);
        String cover = data.optString("cover", "");
        String mid = data.optString("mid", song.mid);
        String link = "https://i.y.qq.com/v8/playsong.html?songmid=" + mid;
        if (chatType == 2) {
            ExtraTool.qqsendTroopMusic(peerUin, title, singer, link, music, cover);
        } else {
            ExtraTool.qqsendFriendMusic(peerUin, title, singer, link, music, cover);
        }
    }

    private void searchQQMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            searchMusicList.remove(key);
            String url = HttpUtils.get(API_V1 + "qqmusic.php?select=search&msg=" + URLEncoder.encode(keyword, "UTF-8") + "&num=15" + sponsorParam());
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
            String url = HttpUtils.get(API_V1 + "wymusic.php?select=search&msg=" + URLEncoder.encode(keyword, "UTF-8") + "&num=15" + sponsorParam());
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
            String url = HttpUtils.get(API_V1 + "kgmusic.php?select=search&msg=" + URLEncoder.encode(keyword, "UTF-8") + "&num=15" + sponsorParam());
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

    /** 抖音点歌：走旧接口通道（/API/ly/qsmusic.php），搜索结果自带直链，保持原有逻辑 */
    private void searchDouyinMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
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
                searchMusicList.put(key, new MusicSearchResult(qun, uin, "抖音", data));
                core.reply(msgData, result.toString() + "请发送序号选择\n不想点歌请发\"取消点歌\"");
            } else {
                core.reply(msgData, json.optString("msg"));
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "searchDouyinMusic error: " + e.getMessage());
        }
    }

    /** 汽水点歌：音乐文档接口（qsmusic.php，无音质档位） */
    private void searchQishuiMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            searchMusicList.remove(key);
            String url = HttpUtils.get(API_V1 + "qsmusic.php?select=search&msg=" + URLEncoder.encode(keyword, "UTF-8") + "&num=15" + sponsorParam());
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

    /** 咪咕点歌：音乐文档接口（mgmusic.php） */
    private void searchMiguMusic(String qun, String uin, String keyword, int mtype, ColdRainCore core, MsgData msgData) {
        try {
            String key = qun + "_" + uin;
            searchMusicList.remove(key);
            String url = HttpUtils.get(API_V1 + "mgmusic.php?select=search&msg=" + URLEncoder.encode(keyword, "UTF-8") + "&num=15" + sponsorParam());
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
                searchMusicList.put(key, new MusicSearchResult(qun, uin, "咪咕", data));
                core.reply(msgData, result.toString() + "请发送序号选择\n不想点歌请发\"取消点歌\"");
            } else {
                core.reply(msgData, json.optString("msg"));
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "searchMiguMusic error: " + e.getMessage());
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
                String url = HttpUtils.get(API_V1 + "qqmusic.php?select=play&mid=" + mid + "&adaptive=1" + qualityParam(qun, "qq") + sponsorParam());
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
                    } else {
                        core.reply(msgData, json.optString("msg"));
                    }
                }
            } else if ("网易".equals(result.type)) {
                String id = result.json.getJSONObject(index).optString("id");
                String url = HttpUtils.get(API_V1 + "wymusic.php?select=play&id=" + id + "&adaptive=1" + qualityParam(qun, "wy") + sponsorParam());
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
                    } else {
                        core.reply(msgData, json.optString("msg"));
                    }
                }
            } else if ("酷狗".equals(result.type)) {
                String hash = result.json.getJSONObject(index).optString("hash");
                String url = HttpUtils.get(API_V1 + "kgmusic.php?select=play&hash=" + hash + qualityParam(qun, "kg") + sponsorParam());
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
                    } else {
                        core.reply(msgData, json.optString("msg"));
                    }
                }
            } else if ("酷我".equals(result.type)) {
                String id = result.json.getJSONObject(index).optString("DC_TARGETID");
                String url = HttpUtils.get(API_V1 + "kwmusic.php?select=play&id=" + id + "&adaptive=1" + qualityParam(qun, "kw") + sponsorParam());
                if (url != null && !url.isEmpty() && !url.equals("访问网页失败")) {
                    JSONObject json = new JSONObject(url);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        String music = data.getString("music").replace("$", "=");
                        String song = data.getString("song");
                        String singer = data.getString("singer");
                        String cover = data.getString("cover");
                        String link = "http://m.kuwo.cn/newh5app/play_detail/" + id;
                        sendMusic(qun, song, singer, link, music, cover, "酷我", yyms, mtype, core, msgData);
                    } else {
                        core.reply(msgData, json.optString("msg"));
                    }
                }
            } else if ("咪咕".equals(result.type)) {
                String id = result.json.getJSONObject(index).optString("id");
                String url = HttpUtils.get(API_V1 + "mgmusic.php?select=play&id=" + id + "&adaptive=1" + qualityParam(qun, "mg") + sponsorParam());
                if (url != null && !url.isEmpty() && !url.equals("访问网页失败")) {
                    JSONObject json = new JSONObject(url);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        String music = data.getString("music");
                        String song = data.getString("song");
                        String singer = data.getString("singer");
                        String cover = data.getString("cover");
                        String link = "https://music.migu.cn/v3/music/song/" + id;
                        sendMusic(qun, song, singer, link, music, cover, "咪咕", yyms, mtype, core, msgData);
                    } else {
                        core.reply(msgData, json.optString("msg"));
                    }
                }
            } else if ("汽水".equals(result.type)) {
                String id = result.json.getJSONObject(index).optString("id");
                String url = HttpUtils.get(API_V1 + "qsmusic.php?select=play&id=" + id + sponsorParam());
                if (url != null && !url.isEmpty() && !url.equals("访问网页失败")) {
                    JSONObject json = new JSONObject(url);
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        String music = data.getString("music");
                        String song = data.getString("song");
                        String singer = data.getString("singer");
                        String cover = data.getString("cover");
                        String link = data.optString("link", "https://www.qishui.com/share/track?track_id=" + id);
                        sendMusic(qun, song, singer, link, music, cover, "汽水", yyms, mtype, core, msgData);
                    } else {
                        core.reply(msgData, json.optString("msg"));
                    }
                }
            } else if ("抖音".equals(result.type)) {
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
                if ("酷我".equals(app)) {
                    // 酷我卡片不一定能发出去，先弹个提示告知，但仍然照发（万一成功了呢）
                    Toasts.showToast("酷我音乐卡片可能发送失败，已尝试发送");
                }
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

    /** 音质配置文件：每个聊天一个，存各平台已设置的音质 */
    private java.io.File getQualityFile(String qun) {
        return new java.io.File(QQCurrentEnv.getCurrentDir() + "/data/" + qun + "音质.txt");
    }

    private JSONObject loadQualityConfig(String qun) {
        try {
            java.io.File file = getQualityFile(qun);
            if (!file.exists()) return new JSONObject();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            String text = sb.toString().trim();
            return text.isEmpty() ? new JSONObject() : new JSONObject(text);
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "loadQualityConfig error: " + e.getMessage());
            return new JSONObject();
        }
    }

    private void saveQualityConfig(String qun, JSONObject config) {
        try {
            java.io.File dataDir = new java.io.File(QQCurrentEnv.getCurrentDir() + "/data");
            if (!dataDir.exists()) dataDir.mkdirs();
            java.io.FileWriter writer = new java.io.FileWriter(getQualityFile(qun));
            writer.write(config.toString());
            writer.close();
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "saveQualityConfig error: " + e.getMessage());
        }
    }

    /** 赞助密钥：全局保存一份，内部接口请求带上 sponsor_key */
    private static java.io.File getSponsorKeyFile() {
        return new java.io.File(QQCurrentEnv.getCurrentDir() + "/data/sponsor_key.txt");
    }

    private static volatile String sponsorKeyCache = null;

    private static String getSponsorKey() {
        String cached = sponsorKeyCache;
        if (cached != null) return cached;
        String key = "";
        try {
            java.io.File file = getSponsorKeyFile();
            if (file.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
                String line = reader.readLine();
                reader.close();
                if (line != null) key = line.trim();
            }
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "getSponsorKey error: " + e.getMessage());
        }
        sponsorKeyCache = key;
        return key;
    }

    private static String sponsorParam() {
        String key = getSponsorKey();
        if (key.isEmpty()) return "";
        try {
            return "&sponsor_key=" + URLEncoder.encode(key, "UTF-8");
        } catch (Throwable e) {
            return "&sponsor_key=" + key;
        }
    }

    /** 配置赞助密钥xxxxx */
    private void setSponsorKey(String key, ColdRainCore core, MsgData msgData) {
        try {
            java.io.File dataDir = new java.io.File(QQCurrentEnv.getCurrentDir() + "/data");
            if (!dataDir.exists()) dataDir.mkdirs();
            java.io.FileWriter writer = new java.io.FileWriter(getSponsorKeyFile());
            writer.write(key);
            writer.close();
            sponsorKeyCache = key;
            core.reply(msgData, "赞助密钥已保存，音乐接口已切换到赞助通道");
        } catch (Throwable e) {
            LogUtils.e("MusicMenuFeature", "setSponsorKey error: " + e.getMessage());
            core.reply(msgData, "赞助密钥保存失败");
        }
    }

    /** 取已设置音质；未设置时回落平台默认（保持原有默认行为），都没有则返回空 */
    private String getQuality(String qun, String key) {
        String value = loadQualityConfig(qun).optString(key, "");
        if (!value.isEmpty()) return value;
        String def = QUALITY_DEFAULT.get(key);
        return def == null ? "" : def;
    }

    /** 拼到 play 请求上的音质参数 */
    private String qualityParam(String qun, String key) {
        String value = getQuality(qun, key);
        return value.isEmpty() ? "" : "&type=" + value;
    }

    /** 设置XX音质YY */
    private void setMusicQuality(String qun, String text, ColdRainCore core, MsgData msgData) {
        String rest = text.substring(2);
        int idx = rest.indexOf("音质");
        String platform = idx > 0 ? rest.substring(0, idx).trim() : "";
        String value = idx > 0 ? rest.substring(idx + 2).trim() : "";
        if (platform.isEmpty() || value.isEmpty()) {
            core.reply(msgData, "格式：设置QQ音质10\n可设置平台：QQ、网易云、酷狗、酷我、咪咕");
            return;
        }
        if (platform.contains("汽水")) {
            core.reply(msgData, "汽水音乐只有一路 M4A（约126k），不支持切换音质");
            return;
        }
        if (platform.contains("抖音")) {
            core.reply(msgData, "抖音点歌使用固定音源，不支持设置音质");
            return;
        }
        String key = QUALITY_ALIAS.get(platform);
        if (key == null) {
            core.reply(msgData, "暂不支持该平台，可设置：QQ、网易云、酷狗、酷我、咪咕");
            return;
        }
        LinkedHashMap<String, String> table = QUALITY_TABLE.get(key);
        if (table == null || !table.containsKey(value)) {
            core.reply(msgData, "音质值不正确\n发送\"查看" + platform + "音质列表\"查看可选音质");
            return;
        }
        JSONObject config = loadQualityConfig(qun);
        try {
            config.put(key, value);
        } catch (Throwable ignored) {
        }
        saveQualityConfig(qun, config);
        core.reply(msgData, "已将" + QUALITY_NAME.get(key) + "音质设置为 " + value + "——" + table.get(value));
    }

    /** 查看XX音质列表 */
    private void showMusicQuality(String qun, String text, ColdRainCore core, MsgData msgData) {
        String rest = text.substring(2);
        int idx = rest.indexOf("音质");
        String platform = idx > 0 ? rest.substring(0, idx).trim() : "";
        if (platform.contains("汽水")) {
            core.reply(msgData, "汽水音乐音质列表:\nM4A（约126k）——固定单一音质，无档位可选");
            return;
        }
        if (platform.contains("抖音")) {
            core.reply(msgData, "抖音点歌音质列表:\n固定音源（music_high），无档位可选");
            return;
        }
        String key = QUALITY_ALIAS.get(platform);
        if (key == null) {
            core.reply(msgData, "格式：查看QQ音质列表\n可查看平台：QQ、网易云、酷狗、酷我、咪咕");
            return;
        }
        LinkedHashMap<String, String> table = QUALITY_TABLE.get(key);
        if (table == null) return;
        String current = loadQualityConfig(qun).optString(key, "");
        StringBuilder result = new StringBuilder(QUALITY_NAME.get(key));
        result.append("音质列表（当前：");
        if (current.isEmpty()) {
            String def = QUALITY_DEFAULT.get(key);
            result.append(def == null ? "接口默认" : def + " " + table.get(def));
        } else {
            result.append(current).append(" ").append(table.get(current));
        }
        result.append("）:\n");
        for (Map.Entry<String, String> entry : table.entrySet()) {
            result.append(entry.getKey()).append("——").append(entry.getValue()).append("\n");
        }
        result.append("发送\"设置").append(QUALITY_NAME.get(key)).append("音质<音质值>\"可切换");
        core.reply(msgData, result.toString());
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