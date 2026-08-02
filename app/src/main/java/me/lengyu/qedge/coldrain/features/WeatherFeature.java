package me.lengyu.qedge.coldrain.features;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.qq.MsgTool;

public class WeatherFeature implements ColdRainFeature {

    private static final String MY_WEB = "https://api.yuafeng.cn/API/ly/";

    private static final Map<String, WeatherSearchData> searchCache = new HashMap<>();

    private static class WeatherSearchData {
        List<String> adcodeList = new ArrayList<>();
        List<String> cityList = new ArrayList<>();
        List<String> districtList = new ArrayList<>();
        String type = "";
    }

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        if (text.equals("天气系统")) return true;
        if (text.startsWith("详细天气#") || text.matches("详细天气#.*#.*")) return true;
        if (text.startsWith("卡片天气")) return true;
        if (text.startsWith("墨迹天气")) return true;
        if (text.matches("[0-9]+") && text.length() <= 2) {
            WeatherSearchData data = searchCache.get(msgData.peerUin);
            if (data != null && !data.type.isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        final String text = msgData.msg.trim();
        final String peerUin = msgData.peerUin;

        if (text.equals("天气系统")) {
            String menu = "天气系统:\n详细天气#省#市\n卡片天气+地区(具体)\n墨迹天气+城市(多选)";
            core.reply(msgData, menu);
            return;
        }

        final ColdRainCore finalCore = core;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleWeather(msgData, text, peerUin, finalCore);
                } catch (Throwable e) {
                    finalCore.reply(msgData, "出错: " + e.getMessage());
                }
            }
        }).start();
    }

    private void handleWeather(MsgData msgData, String text, String peerUin, ColdRainCore core) throws Exception {
        if (text.matches("详细天气#[\\s\\S]+#[\\s\\S]+")) {
            String[] parts = text.split("#");
            if (parts.length < 3) {
                core.reply(msgData, "格式错误，应为：详细天气#省份#城市");
                return;
            }
            String one = parts[1];
            String two = parts[2];
            String url = HttpUtils.get("https://wis.qq.com/weather/common?refer=mimprogram&source=wxa&weather_type=observe%7Calarm%7Cair%7Cforecast_1h%7Cforecast_24h%7Cindex%7Climit%7Ctips%7Crise&province=" + urlEncode(one) + "&city=" + urlEncode(two) + "&county=");
            if (url == null || url.isEmpty()) {
                core.reply(msgData, "获取天气失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            int status = json.optInt("status", -1);
            if (status == 200) {
                JSONObject dataJson = json.getJSONObject("data");
                String air = dataJson.optString("air");
                String observe = dataJson.optString("observe");
                String index = dataJson.optString("index");
                if (air.equals("{}") || observe.equals("{}") || index.equals("{}")) {
                    core.reply(msgData, "获取空气质量或天气失败");
                    return;
                }
                JSONObject jsonAir = new JSONObject(air);
                int aqi = jsonAir.getInt("aqi");
                int aqiLevel = jsonAir.getInt("aqi_level");
                String aqiName = jsonAir.getString("aqi_name");

                JSONObject jsonObserve = new JSONObject(observe);
                String degree = jsonObserve.getString("degree");
                String humidity = jsonObserve.getString("humidity");
                String precipitation = jsonObserve.getString("precipitation");
                String pressure = jsonObserve.getString("pressure");
                String weather = jsonObserve.getString("weather");
                String windPower = jsonObserve.optString("wind_power");
                String windDirectionName = jsonObserve.optString("wind_direction_name");

                JSONObject jsonIndex = new JSONObject(index);

                StringBuilder sb = new StringBuilder();
                sb.append("城市:").append(one).append(" ").append(two).append("\n");
                sb.append("天气:").append(weather).append("(降水量:").append(precipitation).append(")\n");
                sb.append("空气质量:").append(aqi).append("(").append(aqiLevel).append("级,").append(aqiName).append(")\n");
                sb.append("大气温度:").append(degree).append("℃\n");
                sb.append("大气湿度:").append(humidity).append("%\n");
                sb.append("大气气压:").append(pressure).append("\n");
                sb.append("风向:").append(windDirectionName).append("(风力:").append(windPower).append("级)\n\n");

                String[] indexes = {"airconditioner", "allergy", "carwash", "chill", "clothes", "cold", "comfort", "diffusion", "dry", "drying", "fish", "heatstroke", "mood", "makeup", "morning", "sunglasses", "sunscreen", "tourism", "traffic", "ultraviolet", "umbrella"};
                for (String idx : indexes) {
                    if (jsonIndex.has(idx)) {
                        sb.append(jsonIndex.getJSONObject(idx).getString("detail")).append("\n");
                    }
                }
                core.reply(msgData, sb.toString());
            } else {
                core.reply(msgData, "查询详细天气失败");
            }
            return;
        }

        if (text.startsWith("墨迹天气")) {
            String msg = text.substring(4);
            String url = HttpUtils.get(MY_WEB + "moji.php?city=" + urlEncode(msg) + "&num=99");
            if (url == null || url.isEmpty()) {
                core.reply(msgData, "请求失败");
                return;
            }
            JSONObject json = new JSONObject(url);
            if (json.optInt("code", -1) != 0) {
                core.reply(msgData, "未搜索到该地区，请具体到XX市(县)");
                return;
            }
            JSONArray regions = json.getJSONArray("data");
            WeatherSearchData data = new WeatherSearchData();
            data.type = "moji";
            String result = "";
            for (int q = 0; q < regions.length(); q++) {
                JSONObject list = regions.getJSONObject(q);
                String adcode = list.optString("city_code");
                String city = list.getString("city");
                String province = list.getString("province");
                if (province.equals("")) province = "主城";
                data.adcodeList.add(adcode);
                data.cityList.add(city);
                data.districtList.add(province);
                result += (q + 1) + "、" + province + city + "\n";
            }
            if (result.equals("")) {
                core.reply(msgData, "未搜索到该地区");
                return;
            }
            searchCache.put(peerUin, data);
            core.reply(msgData, result + "请输入序号选择");
            return;
        }

        WeatherSearchData data = searchCache.get(peerUin);
        if (data != null && !data.type.isEmpty() && text.matches("[0-9]+") && text.length() <= 2) {
            int index = Integer.parseInt(text);
            if (index > 0 && data.type.equals("moji") && index <= data.adcodeList.size()) {
                String url = HttpUtils.get(MY_WEB + "moji.php?city_code=" + data.adcodeList.get(index - 1));
                if (url == null || url.isEmpty()) {
                    core.reply(msgData, "请求失败");
                    return;
                }
                JSONObject json = new JSONObject(url);
                JSONObject into = json.getJSONObject("data").getJSONObject("result");

                String alerts = into.optString("alerts");
                if (alerts == null || alerts.equals("null") || alerts.equals("false") || alerts.isEmpty()) {
                    alerts = "无气象预警\n";
                } else {
                    JSONArray jsona = into.getJSONArray("alerts");
                    alerts = "";
                    for (int i = 0; i < jsona.length(); i++) {
                        JSONObject alertsj = jsona.getJSONObject(i);
                        alerts += (i + 1) + "、" + alertsj.getString("title") + "(" + alertsj.getInt("level") + "级)\n" + alertsj.getString("content") + "\n[pic=" + alertsj.getString("ficonUrl") + "]\n";
                    }
                }

                String aqi = into.optString("aqi");
                if (aqi == null || aqi.equals("null") || aqi.equals("false") || aqi.isEmpty()) {
                    aqi = "无空气质量数据\n";
                } else {
                    JSONObject jsona = into.getJSONObject("aqi");
                    aqi = "空气质量:" + jsona.getString("level") + "(" + jsona.getInt("value") + ")\n";
                }

                String condition = into.optString("condition");
                if (condition == null || condition.equals("null") || condition.equals("false") || condition.isEmpty()) {
                    condition = "无天气数据\n";
                } else {
                    JSONObject jsona = into.getJSONObject("condition");
                    condition = "天气:" + jsona.getString("fcondition") + "\n目前气温:" + jsona.getString("ftemp") + "℃\n体感:" + jsona.getString("freal_feel") + "℃\n风向:" + jsona.getString("fwind_dir") + "(" + jsona.getInt("fwind_level") + "级)\n日出:" + jsona.getString("fsun_rise") + "\n日落:" + jsona.getString("fsun_down") + "\n";
                }

                String liveIndex = into.optString("liveIndex");
                if (liveIndex == null || liveIndex.equals("null") || liveIndex.equals("false") || liveIndex.isEmpty()) {
                    liveIndex = "无生活提示\n";
                } else {
                    JSONArray jsona = into.getJSONArray("liveIndex");
                    liveIndex = "";
                    for (int i = 0; i < jsona.length(); i++) {
                        JSONObject liveIndexj = jsona.getJSONObject(i);
                        liveIndex += (i + 1) + "、" + liveIndexj.getString("flive_name") + ":" + liveIndexj.getString("flive_status") + "\n" + liveIndexj.getString("flive_url") + "\n";
                    }
                }

                String forecastDayList = into.optString("forecastDayList");
                if (forecastDayList == null || forecastDayList.equals("null") || forecastDayList.equals("false") || forecastDayList.isEmpty()) {
                    forecastDayList = "无15日天气预报\n";
                } else {
                    JSONArray jsona = into.getJSONArray("forecastDayList");
                    forecastDayList = "";
                    for (int i = 0; i < jsona.length(); i++) {
                        JSONObject forecastDayListj = jsona.getJSONObject(i);
                        forecastDayList += "日期:" + forecastDayListj.getString("fpredict_date") + "\n天气:白天:" + forecastDayListj.getString("fcondition_day") + "(夜晚:" + forecastDayListj.getString("fcondition_night") + ")\n气温:" + forecastDayListj.getString("ftemp_low") + "～" + forecastDayListj.getString("ftemp_high") + "℃\n湿度:" + forecastDayListj.getString("humidity") + "\n风向:" + forecastDayListj.getString("fwind_dir_day") + forecastDayListj.getString("fwind_level_day") + "级\n空气质量:" + forecastDayListj.getString("aqiDesc") + "(" + forecastDayListj.getString("aqi") + ")\n限行:" + forecastDayListj.getString("tail_desc") + "\n日出:" + forecastDayListj.getString("fsun_rise") + "\n日落:" + forecastDayListj.getString("fsun_down") + "\n\n";
                    }
                }

                String forecastHourList = into.optString("forecastHourList");
                if (forecastHourList == null || forecastHourList.equals("null") || forecastHourList.equals("false") || forecastHourList.isEmpty()) {
                    forecastHourList = "无今日时刻天气预报\n";
                } else {
                    JSONArray jsona = into.getJSONArray("forecastHourList");
                    forecastHourList = "";
                    for (int i = 0; i < jsona.length(); i++) {
                        JSONObject forecastHourListj = jsona.getJSONObject(i);
                        forecastHourList += "今天/明天" + forecastHourListj.getString("hour") + "时\n天气:" + forecastHourListj.getString("dayDesc") + "\n气温:" + forecastHourListj.getString("tmp") + "℃\n\n";
                    }
                }

                core.reply(msgData, "今日总天气:\n" + alerts + aqi + condition + liveIndex);
                core.reply(msgData, "十五日天气预报:\n" + forecastDayList);
                core.reply(msgData, "每时天气预报:\n" + forecastHourList);
                searchCache.remove(peerUin);
            }
            return;
        }
    }

    private static String urlEncode(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return str;
        }
    }
}
