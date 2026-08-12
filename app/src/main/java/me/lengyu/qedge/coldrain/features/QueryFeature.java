package me.lengyu.qedge.coldrain.features;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.QQCurrentEnv;
import me.lengyu.qedge.utils.qq.CookieTool;
import me.lengyu.qedge.utils.qq.FriendTool;

public class QueryFeature implements ColdRainFeature {

    private static final String MY_WEB = "https://api.yuafeng.cn/API/ly/";

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        return text.equals("查询系统") ||
            text.matches("查Q音[0-9]+") ||
            text.startsWith("查Q音@") ||
            text.matches("查QID[0-9]+") ||
            text.startsWith("查QID@") ||
            text.matches("查达人[0-9]+") ||
            text.startsWith("查达人@") ||
            text.matches("查等级[0-9]+") ||
            text.startsWith("查等级@") ||
            text.matches("查询信息[0-9]+") ||
            text.startsWith("查询信息@") ||
            text.matches("查访客[0-9]+") ||
            text.startsWith("查访客@") ||
            text.matches("查任务[0-9]+") ||
            text.startsWith("查任务@") ||
            text.matches("查信息[0-9]+") ||
            text.startsWith("查信息@");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();

        if (text.equals("查询系统")) {
            boolean enabled;
            if (msgData.type == 2 && msgData.peerUin != null && !msgData.peerUin.isEmpty()) {
                enabled = core.isGroupFeatureEnabled("feature_query", msgData.peerUin);
            } else {
                enabled = core.isFeatureEnabled("feature_query");
            }
            String menu = "查询系统:\n查Q音@QQ/+QQ\n查访客/任务@QQ/+QQ\n查达人/等级@QQ/+QQ\n查QID/信息@QQ/+QQ\n查询信息@QQ/+QQ\n\n状态(" + (enabled ? "开" : "关") + ")";
            core.reply(msgData, menu);
            return;
        }

        String uin = null;
        if (msgData.atList != null && !msgData.atList.isEmpty()) {
            uin = msgData.atList.get(0);
        } else {
            if (text.matches("查Q音[0-9]+")) uin = text.substring(3);
            else if (text.matches("查QID[0-9]+")) uin = text.substring(4);
            else if (text.matches("查达人[0-9]+")) uin = text.substring(3);
            else if (text.matches("查等级[0-9]+")) uin = text.substring(3);
            else if (text.matches("查询信息[0-9]+")) uin = text.substring(4);
            else if (text.matches("查访客[0-9]+")) uin = text.substring(3);
            else if (text.matches("查任务[0-9]+")) uin = text.substring(3);
            else if (text.matches("查信息[0-9]+")) uin = text.substring(3);
            if(!FriendTool.isValidUin(uin)){
                core.reply(msgData, "请输入正确的QQ号");
                return;
            }
        }

        if (uin == null || uin.isEmpty()) {
            core.reply(msgData, "请@某人或附带QQ号");
            return;
        }

        final String finalUin = uin;
        final MsgData finalMsg = msgData;
        ModuleScope.launchIOJava("QueryFeature", () -> {
                try {
                    if (text.startsWith("查Q音")) {
                        queryQMusic(finalUin, finalMsg, core);
                    } else if (text.startsWith("查QID")) {
                        queryQID(finalUin, finalMsg, core);
                    } else if (text.startsWith("查达人")) {
                        queryDaren(finalUin, finalMsg, core);
                    } else if (text.startsWith("查等级")) {
                        queryLevel(finalUin, finalMsg, core);
                    } else if (text.startsWith("查询信息")) {
                        queryInfo(finalUin, finalMsg, core);
                    } else if (text.startsWith("查访客")) {
                        queryVisitor(finalUin, finalMsg, core);
                    } else if (text.startsWith("查任务")) {
                        queryTask(finalUin, finalMsg, core);
                    } else if (text.startsWith("查信息")) {
                        queryUserInfo(finalUin, finalMsg, core);
                    }
                } catch (Throwable e) {
                    core.reply(finalMsg, "查询失败: " + e.getMessage());
                }
        });
    }

    private void queryQMusic(String uin, MsgData msgData, ColdRainCore core) {
        try {
            String url = MY_WEB + "music_cha.php?uin=" + uin;
            String result = HttpUtils.get(url);
            if (result == null || result.isEmpty()) {
                core.reply(msgData, "接口出错！");
                return;
            }
            JSONObject json = new JSONObject(result);
            if (json.getInt("code") == 0) {
                JSONObject data = json.getJSONObject("data");
                StringBuilder sb = new StringBuilder();
                sb.append("[pic=").append(data.getString("avatar")).append("]");
                sb.append("\nQQ:").append(data.getString("uin"));
                sb.append("\n昵称:").append(data.getString("name"));
                sb.append("\n性别:").append(data.getString("gender"));
                sb.append("\n星座:").append(data.getString("constellation"));
                sb.append("\n粉丝:").append(data.getLong("fans_num"));
                sb.append("\n关注:").append(data.getLong("follow_num"));
                sb.append("\n访客:").append(data.getLong("visitor"));
                sb.append("\n地址:").append(data.getString("ip"));
                sb.append("\n头像修改:").append(data.getString("avatar_modify_time"));
                String msg = HttpUtils.get(MY_WEB + "qq_music.php?uin=" + uin + "&select=%E5%A4%9A%E9%80%89");
                if (msg != null && !msg.isEmpty()) {
                    try {
                        JSONObject mjson = new JSONObject(msg);
                        if (mjson.getInt("code") == 0) {
                            JSONArray array = mjson.getJSONArray("music_list");
                            if (array.length() >= 1) {
                                sb.append("\n收藏歌单列表:");
                            }
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject j = array.getJSONObject(i);
                                sb.append("\n歌单:").append(j.getString("title"));
                                sb.append("\n详情:").append(j.getString("info"));
                                sb.append("\n歌单id:").append(j.getLong("id"));
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                core.reply(msgData, sb.toString());
            } else {
                core.reply(msgData, json.getString("msg"));
            }
        } catch (Throwable e) {
            core.reply(msgData, "查Q音失败: " + e.getMessage());
        }
    }

    private void queryQID(String uin, MsgData msgData, ColdRainCore core) {
        try {
            String myUin = QQCurrentEnv.getCurrentUin();
            if (myUin == null) {
                core.reply(msgData, "无法获取当前QQ");
                return;
            }
            String vippskey = CookieTool.getPskey("vip.qq.com");
            if (vippskey == null) {
                core.reply(msgData, "无法获取VIP pskey");
                return;
            }
            String skey = CookieTool.getSkey();
            if (skey == null) {
                core.reply(msgData, "无法获取skey");
                return;
            }
            long gtk = CookieTool.getGtk(vippskey);
            String url = "https://club.vip.qq.com/api/trpc/qid_server/GetQid?g_tk=" + gtk;
            String dataJson = "{\"uin\":" + uin + "}";
            String cookie = "uin=" + QQCurrentEnv.getCookieUin() + "; skey=" + skey + "; p_uin=" + QQCurrentEnv.getCookieUin() + "; p_skey=" + vippskey;
            String post = httppost5(url, cookie, dataJson);
            if (post.isEmpty()) {
                core.reply(msgData, "查询失败");
                return;
            }
            JSONObject json = new JSONObject(post);
            if (json.getInt("code") == 0) {
                String data1 = json.getString("data");
                JSONObject json1 = new JSONObject(data1);
                String qid = json1.optString("qid", "");
                core.reply(msgData, "QQ:" + uin + "\nQID:" + qid);
            } else {
                core.reply(msgData, "查询失败");
            }
        } catch (Throwable e) {
            core.reply(msgData, "查询失败:" + e.getMessage());
        }
    }

    private void queryDaren(String uin, MsgData msgData, ColdRainCore core) {
        try {
            String myUin = QQCurrentEnv.getCurrentUin();
            if (myUin == null) {
                core.reply(msgData, "无法获取当前QQ");
                return;
            }
            String vippskey = CookieTool.getPskey("vip.qq.com");
            if (vippskey == null) {
                core.reply(msgData, "无法获取VIP pskey");
                return;
            }
            String skey = CookieTool.getSkey();
            if (skey == null) {
                core.reply(msgData, "无法获取skey");
                return;
            }
            long gtk = CookieTool.getGtk(vippskey);
            String url = "https://cgi.vip.qq.com/card/getExpertInfo?ps_tk=" + gtk + "&fuin=" + uin + "&g_tk=" + gtk;
            String cookie = "uin=" + QQCurrentEnv.getCookieUin() + "; skey=" + skey + "; p_uin=" + QQCurrentEnv.getCookieUin() + "; p_skey=" + vippskey;
            String post = cookieGetWithRefer(url, cookie, "https://club.vip.qq.com/");
            if (post.isEmpty()) {
                core.reply(msgData, "查询失败");
                return;
            }
            JSONObject json = new JSONObject(post);
            if (json.getInt("ret") == 0) {
                String data1 = json.getString("data");
                JSONObject json1 = new JSONObject(data1);
                String g = json1.getString("g");
                int rd = g.indexOf(",");
                if (rd >= 0) {
                    String gg = g.substring(rd + 1);
                    int rr = gg.indexOf(",");
                    if (rr >= 0) {
                        String ggg = gg.substring(rr + 1);
                        String gggg = gg.replace(ggg, "");
                        core.reply(msgData, "QQ:" + uin + "\n达人天数:" + gggg.replace(",", ""));
                        return;
                    }
                }
            }
            core.reply(msgData, "查询失败");
        } catch (Throwable e) {
            core.reply(msgData, "查询失败:" + e.getMessage());
        }
    }

    private void queryLevel(String uin, MsgData msgData, ColdRainCore core) {
        String result = getVipInfo1(uin);
        if (result.isEmpty()) {
            core.reply(msgData, "查询失败");
        } else {
            core.reply(msgData, result);
        }
    }

    private void queryInfo(String uin, MsgData msgData, ColdRainCore core) {
        try {
            String myUin = QQCurrentEnv.getCurrentUin();
            if (myUin == null) {
                core.reply(msgData, "无法获取当前QQ");
                return;
            }
            String qzone = CookieTool.getPskey("qzone.qq.com");
            if (qzone == null) {
                core.reply(msgData, "无法获取QZone pskey");
                return;
            }
            String skey = CookieTool.getSkey();
            if (skey == null) {
                core.reply(msgData, "无法获取skey");
                return;
            }
            long gtk = CookieTool.getGtk(qzone);
            String cookie = "p_uin=o0" + myUin + ";skey=" + skey + ";p_skey=" + qzone;
            String url = "https://r.qzone.qq.com/cgi-bin/user/cgi_personal_card?uin=" + uin + "&remark=0&g_tk=" + gtk;
            String nm = httpget(url, cookie);
            if (nm.isEmpty()) {
                core.reply(msgData, "获取失败");
                return;
            }
            String nv = nm.replace("_Callback(", "");
            String nan = nv.replace(");", "");
            String boy = nan.replace("\n", "");
            JSONObject json1 = new JSONObject(boy);
            String UIN = json1.optString("uin");
            String intimacyScore = json1.optString("intimacyScore");
            String qzonee = json1.optString("qzone").replace("1", "有").replace("0", "无");
            String realname = json1.optString("realname");
            String nickname = json1.optString("nickname");
            String logolabel = json1.optString("logolabel");
            String qqvip = json1.optString("qqvip");
            String greenvip = json1.optString("greenvip");
            String gender = json1.optString("gender").replace("1", "男").replace("2", "女");
            String isFriend = json1.optString("isFriend").replace("1", "是").replace("0", "否");
            String commfrd = json1.optString("commfrd");
            String isSpecialCare = json1.optString("isSpecialCare").replace("1", "是");
            String biaoshi = "";
            try {
                biaoshi = timestampToDate(Long.parseLong(logolabel) * 1000);
            } catch (Throwable ignored) {}
            String menu = "QQ:" + UIN + "\n昵称:" + nickname + "\n性别:" + gender + "\n备注:" + realname +
                "\n亲密度:" + intimacyScore + "\n空间有无访问权限:" + qzonee +
                "\n是否为特别关心:" + isSpecialCare + "\n是否为好友:" + isFriend +
                "\n标识时间:" + biaoshi + "\n共同好友数量:" + commfrd +
                "\nSVIP等级:" + qqvip + "\n绿钻等级:" + greenvip;
            core.reply(msgData, menu + "\n" + getUserInfo2(uin));
        } catch (Throwable e) {
            core.reply(msgData, "查询失败:" + e.getMessage());
        }
    }

    private void queryVisitor(String uin, MsgData msgData, ColdRainCore core) {
        String result = getQzoneVisiters(uin);
        if (result.isEmpty()) {
            core.reply(msgData, "获取失败");
        } else {
            core.reply(msgData, result);
        }
    }

    private void queryTask(String uin, MsgData msgData, ColdRainCore core) {
        String result1 = getVipInfo1(uin);
        String result3 = getVipInfo3(uin);
        String result = "";
        if (!result1.isEmpty()) result = result1;
        if (!result3.isEmpty()) {
            if (!result.isEmpty()) result += "\n";
            result += result3;
        }
        if (result.isEmpty()) {
            core.reply(msgData, "查询失败");
        } else {
            core.reply(msgData, result);
        }
    }

    private void queryUserInfo(String uin, MsgData msgData, ColdRainCore core) {
        String result = getUserInfo2(uin);
        if (result.isEmpty()) {
            core.reply(msgData, "获取失败");
        } else {
            core.reply(msgData, result);
        }
    }

    private String getVipInfo1(String uin) {
        try {
            String myUin = QQCurrentEnv.getCurrentUin();
            if (myUin == null) return "";
            String pskey = CookieTool.getPskey("vip.qq.com");
            if (pskey == null) return "";
            String skey = CookieTool.getSkey();
            if (skey == null) return "";
            long gtk = CookieTool.getGtk(pskey);
            String cookie = "uin=" + QQCurrentEnv.getCookieUin() + "; skey=" + skey + "; p_uin=" + QQCurrentEnv.getCookieUin() + "; p_skey=" + pskey;
            String url = "https://club.vip.qq.com/api/vip/getQQLevelInfo?g_tk=" + gtk +
                "&requestBody={\"sClientIp\":\"\",\"sSessionKey\":\"" + skey + "\",\"iKeyType\":1,\"iAppId\":0,\"iUin\":\"" + uin + "\"}";
            String result = httpget(url, cookie);
            if (result.isEmpty()) return "";
            JSONObject json = new JSONObject(result);
            if (json.getInt("ret") == 0) {
                String data2 = json.getString("data");
                JSONObject data3 = new JSONObject(data2);
                String mRes = data3.getString("mRes");
                JSONObject data1 = new JSONObject(mRes);
                String sNickName = data1.optString("sNickName");
                String iPCQQOnlineTime = data1.optString("iPCQQOnlineTime");
                String iPCQQOnline = data1.optString("iPCQQOnline");
                String iMaxLvlTotalDays = data1.optString("iMaxLvlTotalDays");
                String iNextLevelDay = data1.optString("iNextLevelDay");
                String iBigClubGrowth = data1.optString("iBigClubGrowth");
                String iQQLevel = data1.optString("iQQLevel");
                String iMobileQQOnline = data1.optString("iMobileQQOnline");
                String iMobileQQOnlineTime = data1.optString("iMobileQQOnlineTime");
                String iTotalActiveDay = data1.optString("iTotalActiveDay");
                return "QQ:" + uin + "\n昵称:" + sNickName + "\nQQ等级:" + iQQLevel +
                    "\n最大日可活跃天数:" + iMaxLvlTotalDays + "\n升下一级需:" + iNextLevelDay + "天" +
                    "\n大会员成长值:" + iBigClubGrowth +
                    "\n手机在线:" + iMobileQQOnline.replace("0", "否").replace("1", "是") + "(共" + iMobileQQOnlineTime + "小时)" +
                    "\n电脑在线:" + iPCQQOnline.replace("0", "否").replace("1", "是") + "(共" + iPCQQOnlineTime + "小时)" +
                    "\n总活跃天数:" + iTotalActiveDay;
            }
        } catch (Throwable e) {
            LogUtils.e("QueryFeature", "getVipInfo1 error: " + e.getMessage());
        }
        return "";
    }

    private String getVipInfo3(String uin) {
        try {
            String myUin = QQCurrentEnv.getCurrentUin();
            if (myUin == null) return "";
            String pskey = CookieTool.getPskey("vip.qq.com");
            if (pskey == null) return "";
            String skey = CookieTool.getSkey();
            if (skey == null) return "";
            long gtk = CookieTool.getGtk(pskey);
            String cookie = "uin=" + QQCurrentEnv.getCookieUin() + "; skey=" + skey + "; p_uin=" + QQCurrentEnv.getCookieUin() + "; p_skey=" + pskey;
            String url = "https://h5.vip.qq.com/proxy/domain/club.vip.qq.com/api/aggregation?from=hippy-vipAggregation&g_tk=" + gtk;
            String result = httppost1(url, cookie, "{\"commonInfo__getQQLevelInfo\":{\"args\":[\"" + uin + "\"],\"needCtx\":false}}");
            if (result.isEmpty()) return "";
            JSONObject json = new JSONObject(result);
            if (json.getInt("code") == 0) {
                String data1 = json.getString("data");
                JSONObject json1 = new JSONObject(data1);
                String json2 = json1.getString("commonInfo__getQQLevelInfo");
                JSONObject data3 = new JSONObject(json2);
                String iAddFriend = data3.optString("iAddFriend").replace("0", "未完成").replace("1", "已完成");
                String iDailySign = data3.optString("iDailySign").replace("0", "未完成").replace("1", "已完成");
                String WeishiVideoview = data3.optString("WeishiVideoview").replace("0", "未完成").replace("1", "已完成");
                String iVipSpeedRate = data3.optString("iVipSpeedRate");
                String iMobileGameOnline = data3.optString("iMobileGameOnline").replace("0", "未完成").replace("1", "已完成");
                String iTotalDays = data3.optString("iTotalDays");
                String iPCSafeOnline = data3.optString("iPCSafeOnline").replace("0", "未完成").replace("1", "已完成");
                String iContinueLogin = data3.optString("iContinueLogin").replace("0", "未完成").replace("1", "已完成");
                String QzoneVisitor = data3.optString("QzoneVisitor");
                String iBigClubGrowth = data3.optString("iBigClubGrowth");
                String iBigClubLevel = data3.optString("iBigClubLevel");
                String iVip = data3.optString("iVip").replace("0", "否").replace("1", "是");
                String iSVip = data3.optString("iSVip").replace("0", "否").replace("1", "是");
                String iYearVip = data3.optString("iYearVip").replace("0", "否").replace("1", "是");
                String iBigClubVipFlag = data3.optString("iBigClubVipFlag").replace("0", "否").replace("1", "是");
                return "新增好友:" + iAddFriend + "\n日签打卡:" + iDailySign + "\n微视加速:" + WeishiVideoview +
                    "\n手游在线:" + iMobileGameOnline + "\n电脑管家在线:" + iPCSafeOnline +
                    "\n连续登录:" + iContinueLogin + "\n会员速度:" + iVipSpeedRate + "\n今日成长:" + iTotalDays +
                    "\n空间访客:" + QzoneVisitor + "\n是否有会员:" + iVip + "\n是否有年会:" + iYearVip +
                    "\n是否有超会:" + iSVip + "\n是否有大会:" + iBigClubVipFlag +
                    "\n大会成长值:" + iBigClubGrowth + "\n大会等级:" + iBigClubLevel;
            }
        } catch (Throwable e) {
            LogUtils.e("QueryFeature", "getVipInfo3 error: " + e.getMessage());
        }
        return "";
    }

    private String getUserInfo2(String uin) {
        try {
            String myUin = QQCurrentEnv.getCurrentUin();
            if (myUin == null) return "";
            String pskey = CookieTool.getPskey("qzone.qq.com");
            if (pskey == null) return "";
            String skey = CookieTool.getSkey();
            if (skey == null) return "";
            long gtk = CookieTool.getGtk(pskey);
            String cookie = "uin=" + QQCurrentEnv.getCookieUin() + "; skey=" + skey + "; p_uin=" + QQCurrentEnv.getCookieUin() + "; p_skey=" + pskey;
            String url = "https://h5.qzone.qq.com/webapp/json/vpageCover_v2/getMainPage?g_tk=" + gtk +
                "&uin=" + uin + "&visituin=" + myUin + "&force=1&format=json";
            String result = httpget(url, cookie);
            if (result.isEmpty()) return "";
            JSONObject json = new JSONObject(result);
            if (json.getInt("ret") == 0) {
                String msg = json.optString("msg", "");
                if (!msg.isEmpty()) msg = msg + "\n";
                JSONObject profile = json.getJSONObject("data").getJSONObject("profile");
                String nickname = profile.optString("nickname");
                String qzonename = profile.optString("qzonename");
                String gender = profile.optString("gender").replace("1", "男").replace("2", "女").replace("0", "未知");
                String astro = profile.optString("astro");
                String viplevel = profile.optString("viplevel");
                String vipscore = profile.optString("vipscore");
                String vipspeed = profile.optString("vipspeed");
                String space_desc = profile.optString("space_desc");
                String vip_keepdays = profile.optString("vip_keepdays");
                JSONObject count = json.getJSONObject("data").getJSONObject("count");
                String pic_allnum = count.optString("pic_allnum");
                String shuoshuo_allnum = count.optString("shuoshuo_allnum");
                JSONObject visit = json.getJSONObject("data").getJSONObject("visit");
                String todaynum = visit.optString("todaynum");
                String totalnum = visit.optString("totalnum");
                return msg + "空间:" + qzonename + "\n星座:" + astro + "\n空间说明:" + space_desc +
                    "\n会员持续:" + vip_keepdays + "天\n相册数量:" + pic_allnum +
                    "\n说说数量:" + shuoshuo_allnum + "\n今日访客:" + todaynum + "人次" +
                    "\n总访客量:" + totalnum + "人次";
            }
        } catch (Throwable e) {
            LogUtils.e("QueryFeature", "getUserInfo2 error: " + e.getMessage());
        }
        return "";
    }

    private String getQzoneVisiters(String uin) {
        try {
            String myUin = QQCurrentEnv.getCurrentUin();
            if (myUin == null) return "";
            String pskey = CookieTool.getPskey("qzone.qq.com");
            if (pskey == null) return "";
            String skey = CookieTool.getSkey();
            if (skey == null) return "";
            long gtk = CookieTool.getGtk(pskey);
            String cookie = "uin=" + QQCurrentEnv.getCookieUin() + "; skey=" + skey + "; p_uin=" + QQCurrentEnv.getCookieUin() + "; p_skey=" + pskey;
            String url = "https://h5.qzone.qq.com/webapp/json/vpageCover_v2/getMainPage?g_tk=" + gtk +
                "&uin=" + uin + "&visituin=" + myUin + "&force=1&format=json";
            String result = httpget(url, cookie);
            if (result.isEmpty()) return "";
            JSONObject json = new JSONObject(result);
            if (json.getInt("ret") == 0) {
                String msg = json.optString("msg", "");
                if (!msg.isEmpty()) msg = msg + "\n";
                JSONObject profile = json.getJSONObject("data").getJSONObject("profile");
                String qzonename = profile.optString("qzonename");
                JSONObject visit = json.getJSONObject("data").getJSONObject("visit");
                String todaynum = visit.optString("todaynum");
                String totalnum = visit.optString("totalnum");
                return msg + "空间:" + qzonename + "\n今日访客:" + todaynum + "人次" + "\n总访客量:" + totalnum + "人次";
            }
        } catch (Throwable e) {
            LogUtils.e("QueryFeature", "getQzoneVisiters error: " + e.getMessage());
        }
        return "";
    }

    private String httpget(String url, String cookie) {
        try {
            URL urlObj = new URL(url);
            java.net.HttpURLConnection uc = (java.net.HttpURLConnection) urlObj.openConnection();
            uc.setRequestMethod("GET");
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; V2166BA Build/TP1A.220624.014; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/109.0.5414.86 MQQBrowser/6.2 TBS/046715 Mobile Safari/537.36");
            if (cookie != null && !cookie.isEmpty()) {
                uc.setRequestProperty("Cookie", cookie);
            }
            uc.setConnectTimeout(10000);
            uc.setReadTimeout(10000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Throwable e) {
            LogUtils.e("QueryFeature", "httpget error: " + e.getMessage());
            return "";
        }
    }

    private String httppost1(String url, String cookie, String data) {
        try {
            URL urlObj = new URL(url);
            java.net.HttpURLConnection uc = (java.net.HttpURLConnection) urlObj.openConnection();
            uc.setDoInput(true);
            uc.setDoOutput(true);
            uc.setConnectTimeout(10000);
            uc.setReadTimeout(10000);
            uc.setRequestMethod("POST");
            uc.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            if (cookie != null && !cookie.isEmpty()) {
                uc.setRequestProperty("Cookie", cookie);
            }
            uc.getOutputStream().write(data.getBytes("UTF-8"));
            uc.getOutputStream().flush();
            uc.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Throwable e) {
            LogUtils.e("QueryFeature", "httppost1 error: " + e.getMessage());
            return "";
        }
    }

    private String httppost5(String urlPath, String cookie, String data) {
        try {
            URL url = new URL(urlPath);
            java.net.HttpURLConnection uc = (java.net.HttpURLConnection) url.openConnection();
            uc.setDoInput(true);
            uc.setDoOutput(true);
            uc.setConnectTimeout(10000);
            uc.setReadTimeout(10000);
            uc.setRequestMethod("POST");
            uc.setRequestProperty("Host", "club.vip.qq.com");
            uc.setRequestProperty("Connection", "keep-alive");
            uc.setRequestProperty("Content-Length", String.valueOf(data.getBytes("UTF-8").length));
            uc.setRequestProperty("Accept", "application/json, text/plain,*/*");
            uc.setRequestProperty("qname-service", "976321:131072");
            uc.setRequestProperty("qname-space", "Production");
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; V2166BA Build/TP1A.220624.014; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/109.0.5414.86 MQQBrowser/6.2 TBS/046715 Mobile Safari/537.36 V1_AND_SQ_8.9.83_4680_YYB_D QQ/8.9.83.12605 NetType/WIFI WebP/0.3.0 AppId/537178657 Pixel/1080 StatusBarHeight/100 SimpleUISwitch/0 QQTheme/1000 StudyMode/0 CurrentMode/0 CurrentFontScale/1.0 GlobalDensityScale/0.90000004 AllowLandscape/false InMagicWin/0");
            uc.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            uc.setRequestProperty("Origin", "https://qun.qq.com");
            uc.setRequestProperty("Sec-Fetch-Site", "same-origin");
            uc.setRequestProperty("Sec-Fetch-Mode", "cors");
            uc.setRequestProperty("Sec-Fetch-Dest", "empty");
            uc.setRequestProperty("Referer", "https://club.vip.qq.com/qid/card?_wv=16777216&_proxy=0&src=icon&from=icon&trace_detail=base64-eyJhcHBpZCI6Im91dHNpZGUiLCJwYWdlX2lkIjoiNzgifQ%3D%3D&_proxyByURL=1");
            uc.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
            if (cookie != null && !cookie.isEmpty()) {
                uc.setRequestProperty("Cookie", cookie);
            }
            uc.getOutputStream().write(data.getBytes("UTF-8"));
            uc.getOutputStream().flush();
            uc.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Throwable e) {
            LogUtils.e("QueryFeature", "httppost5 error: " + e.getMessage());
            return "";
        }
    }

    private String cookieGetWithRefer(String url, String cookie, String refer) {
        try {
            URL urlObj = new URL(url);
            java.net.HttpURLConnection uc = (java.net.HttpURLConnection) urlObj.openConnection();
            uc.setRequestMethod("GET");
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; V2166BA Build/TP1A.220624.014; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/109.0.5414.86 MQQBrowser/6.2 TBS/046715 Mobile Safari/537.36");
            if (cookie != null && !cookie.isEmpty()) {
                uc.setRequestProperty("Cookie", cookie);
            }
            if (refer != null && !refer.isEmpty()) {
                uc.setRequestProperty("Referer", refer);
            }
            uc.setConnectTimeout(10000);
            uc.setReadTimeout(10000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Throwable e) {
            LogUtils.e("QueryFeature", "cookieGetWithRefer error: " + e.getMessage());
            return "";
        }
    }

    private String timestampToDate(long timestamp) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(timestamp));
        } catch (Throwable e) {
            return "";
        }
    }
}
