List weather_list=new ArrayList();
List moji_list=new ArrayList();
public void 天气系统(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu,Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq=myUin;
            int mtype=data.mtype;
            long msgid=data.msgid;
            int msgtype=data.msgtype;
            if(读(qun,"lengyu520","开关")==1&&cdxz(qun,uin)&&判断群(qun,mtype)==1) {
                if(quntext.equals("开启天气系统")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"天气系统","开关",1);
                        String menu="已开启本聊天天气系统";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭天气系统")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"天气系统","开关",0);
                        String menu="已关闭本聊天天气系统";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("天气系统")) {
                    String lengyu = "关";
                    if(读(qun,"天气系统","开关")==1) lengyu = "开";
                    String menu = "天气系统:\n开启/关闭天气系统("+lengyu+")\n详细天气#省#市\n卡片天气+地区(具体)\n墨迹天气+城市(多选)";
                    sendText(data,menu);
                }
                if(读(qun,"天气系统","开关")==1) {
                    if(quntext.matches("详细天气#[\\s\\S]+#[\\s\\S]+")) {
                        String one=quntext.split("#")[1];
                        String two=quntext.split("#")[2];
                        String url=get("https://wis.qq.com/weather/common?refer=mimprogram&source=wxa&weather_type=observe%7Calarm%7Cair%7Cforecast_1h%7Cforecast_24h%7Cindex%7Climit%7Ctips%7Crise&province="+one+"&city="+two+"&county=");
                        JSONObject json=new JSONObject(url);
                        int status=json.getInt("status");
                        if(status==200) {
                            String dataJson=json.getString("data");
                            JSONObject json1=new JSONObject(dataJson);
                            String air=json1.getString("air");
                            String observe=json1.getString("observe");
                            String index=json1.getString("index");
                            String result1="";
                            String result2="";
                            if(air.equals("{}")||observe.equals("{}")||index.equals("{}")) {
                                sendText(data,"获取空气质量或天气失败");
                            }
                            else {
                                JSONObject json_air=new JSONObject(air);
                                int aqi=json_air.getInt("aqi");
                                //空气质量
                                int aqi_level=json_air.getInt("aqi_level");
                                //空气等级
                                String aqi_name=json_air.get("aqi_name");
                                //空气等级名
                                JSONObject json_observe=new JSONObject(observe);
                                String degree=json_observe.get("degree");
                                //温度
                                String humidity=json_observe.get("humidity");
                                //湿度
                                String precipitation=json_observe.get("precipitation");
                                //降水
                                String pressure=json_observe.get("pressure");
                                //气压
                                String weather=json_observe.get("weather");
                                //天气
                                String wind_power=json_observe.optString("wind_power");
                                //风力
                                String wind_direction_name=json_observe.optString("wind_direction_name");
                                //风向
                                JSONObject json_index=new JSONObject(index);
                                String airconditioner_detail=json_index.getJSONObject("airconditioner").get("detail");
                                String allergy_detail=json_index.getJSONObject("allergy").get("detail");
                                String carwash_detail=json_index.getJSONObject("carwash").get("detail");
                                String chill_detail=json_index.getJSONObject("chill").get("detail");
                                String clothes_detail=json_index.getJSONObject("clothes").get("detail");
                                String cold_detail=json_index.getJSONObject("cold").get("detail");
                                String comfort_detail=json_index.getJSONObject("comfort").get("detail");
                                String diffusion_detail=json_index.getJSONObject("diffusion").get("detail");
                                String dry_detail=json_index.getJSONObject("dry").get("detail");
                                String drying_detail=json_index.getJSONObject("drying").get("detail");
                                String fish_detail=json_index.getJSONObject("fish").get("detail");
                                String heatstroke_detail=json_index.getJSONObject("heatstroke").get("detail");
                                String mood_detail=json_index.getJSONObject("mood").get("detail");
                                String makeup_detail=json_index.getJSONObject("makeup").get("detail");
                                String morning_detail=json_index.getJSONObject("morning").get("detail");
                                String sunglasses_detail=json_index.getJSONObject("sunglasses").get("detail");
                                String sunscreen_detail=json_index.getJSONObject("sunscreen").get("detail");
                                String tourism_detail=json_index.getJSONObject("tourism").get("detail");
                                String traffic_detail=json_index.getJSONObject("traffic").get("detail");
                                String ultraviolet_detail=json_index.getJSONObject("ultraviolet").get("detail");
                                String umbrella_detail=json_index.getJSONObject("umbrella").get("detail");
                                sendText(data,"城市:"+one+" "+two+"\n天气:"+weather+"(降水量:"+precipitation+")\n空气质量:32("+aqi_level+"级,"+aqi_name+")\n大气温度:"+degree+"℃\n大气湿度:"+humidity+"%\n大气气压:"+pressure+"\n风向:"+wind_direction_name+"(风力:"+wind_power+"级)\n\n"+airconditioner_detail+"\n"+allergy_detail+"\n"+carwash_detail+"\n"+chill_detail+"\n"+clothes_detail+"\n"+cold_detail+"\n"+comfort_detail+"\n"+diffusion_detail+"\n"+dry_detail+"\n"+drying_detail+"\n"+fish_detail+"\n"+heatstroke_detail+"\n"+mood_detail+"\n"+makeup_detail+"\n"+morning_detail+"\n"+sunglasses_detail+"\n"+sunscreen_detail+"\n"+tourism_detail+"\n"+traffic_detail+"\n"+ultraviolet_detail+"\n"+umbrella_detail);
                            }
                        }
                        else {
                            sendText(data,"查询详细天气失败");
                        }
                    }
                    if(quntext.startsWith("卡片天气")) {
                        String mppskey=getPskey("mp.qq.com");
                        if(mppskey==null) {
                            sendText(data,"出错啦！mp.qq.com获取不到pskey，请尝试重启QQ");
                        }
                        else {
                            String result1="";
                            weather_list.clear();
                            String msg=quntext.substring(4);
                            String cookie="uin=o"+qq+"; skey="+getSkey()+"; p_uin=o"+qq+"; p_skey="+mppskey;
                            String url=httppost1("https://weather.mp.qq.com/trpc/weather/SearchRegions?g_tk="+GetGTK(mppskey)+"&key="+msg+"&offset=0&count=100",cookie,"");
                            JSONObject json=new JSONObject(url);
                            JSONArray regions=json.getJSONArray("regions");
                            for(int q=0;
                            q<regions.length();
                            q++) {
                                JSONObject List=regions.get(q);
                                String adcode=List.optString("adcode");
                                String city=List.get("city");
                                String district=List.get("district");
                                if(district.equals("")) district="主城";
                                weather_list.add(adcode);
                                result1+=(q+1)+"、"+district+"\n";
                            }
                            if(result1.equals("")) {
                                sendText(data,"未搜索到该地区，请具体到XX市(县)");
                            }
                            else {
                                sendText(data,result1+"请输入序号选择");
                            }
                        }
                    }
                    if(quntext.startsWith("墨迹天气")) {
                        String result1="";
                        moji_list.clear();
                        String msg=quntext.substring(4);
                        String url=get(myWeb+"moji.php?city="+msg+"&num=99");
                        JSONObject json=new JSONObject(url);
                        JSONArray regions=json.getJSONArray("data");
                        for(int q=0;
                        q<regions.length();
                        q++) {
                            JSONObject List=regions.get(q);
                            String adcode=List.optString("city_code");
                            String city=List.get("city");
                            String district=List.get("province");
                            if(district.equals("")) district="主城";
                            moji_list.add(adcode);
                            result1+=(q+1)+"、"+district+city+"\n";
                        }
                        if(json.getInt("code")!=0||result1.equals("")) {
                            sendText(data,"未搜索到该地区，请具体到XX市(县)");
                        }
                        else {
                            sendText(data,result1+"请输入序号选择");
                        }
                    }
                    if(quntext.matches("[0-9]+")&&quntext.length()<=2) {
                        int index = Integer.parseInt(quntext);
                        if(index>0&&moji_list.size()>=index) {
                            String url=get(myWeb+"moji.php?city_code="+moji_list.get(index-1));
                            JSONObject json=new JSONObject(url);
                            JSONObject into=json.getJSONObject("data").getJSONObject("result");
                            String alerts=into.optString("alerts");
                            if(alerts==null||alerts.equals("null")||alerts.equals("false")||alerts.isEmpty()) {
                                alerts="无气象预警\n";
                                //预警
                            }
                            else {
                                JSONArray jsona = into.getJSONArray("alerts");
                                alerts="";
                                for(int i=0;
                                i<jsona.length();
                                i++) {
                                    JSONObject alertsj=jsona.getJSONObject(i);
                                    alerts+=(i+1)+"、"+alertsj.getString("title")+"("+alertsj.getInt("level")+"级)\n"+alertsj.getString("content")+"\n[pic="+alertsj.getString("ficonUrl")+"]\n";
                                }
                            }
                            String aqi=into.optString("aqi");
                            if(aqi==null||aqi.equals("null")||aqi.equals("false")||aqi.isEmpty()) {
                                aqi="无空气质量数据\n";
                            }
                            else {
                                JSONObject jsona = into.getJSONObject("aqi");
                                aqi="空气质量:"+jsona.getString("level")+"("+jsona.getInt("value")+")\n";
                            }
                            String condition=into.optString("condition");
                            if(condition==null||condition.equals("null")||condition.equals("false")||condition.isEmpty()) {
                                condition="无天气数据\n";
                            }
                            else {
                                JSONObject jsona = into.getJSONObject("condition");
                                condition="天气:"+jsona.getString("fcondition")+"\n目前气温:"+jsona.getString("ftemp")+"℃\n体感:"+jsona.getString("freal_feel")+"℃\n风向:"+jsona.getString("fwind_dir")+"("+jsona.getInt("fwind_level")+"级)\n日出:"+jsona.getString("fsun_rise")+"\n日落:"+jsona.getString("fsun_down")+"\n";
                            }
                            String liveIndex=into.optString("liveIndex");
                            if(liveIndex==null||liveIndex.equals("null")||liveIndex.equals("false")||liveIndex.isEmpty()) {
                                liveIndex="无生活提示\n";
                            }
                            else {
                                JSONArray jsona = into.getJSONArray("liveIndex");
                                liveIndex="";
                                for(int i=0;
                                i<jsona.length();
                                i++) {
                                    JSONObject liveIndexj=jsona.getJSONObject(i);
                                    liveIndex+=(i+1)+"、"+liveIndexj.getString("flive_name")+":"+liveIndexj.getString("flive_status")+"\n"+liveIndexj.getString("flive_url")+"\n";
                                }
                            }
                            String forecastDayList=into.optString("forecastDayList");
                            if(forecastDayList==null||forecastDayList.equals("null")||forecastDayList.equals("false")||forecastDayList.isEmpty()) {
                                forecastDayList="无15日天气预报\n";
                            }
                            else {
                                JSONArray jsona = into.getJSONArray("forecastDayList");
                                forecastDayList="";
                                for(int i=0;
                                i<jsona.length();
                                i++) {
                                    JSONObject forecastDayListj=jsona.getJSONObject(i);
                                    forecastDayList+="日期:"+forecastDayListj.getString("fpredict_date")+"\n天气:白天:"+forecastDayListj.getString("fcondition_day")+"(夜晚:"+forecastDayListj.getString("fcondition_night")+")\n气温:"+forecastDayListj.getString("ftemp_low")+"～"+forecastDayListj.getString("ftemp_high")+"℃\n湿度:"+forecastDayListj.getString("humidity")+"\n风向:"+forecastDayListj.getString("fwind_dir_day")+forecastDayListj.getString("fwind_level_day")+"级\n空气质量:"+forecastDayListj.getString("aqiDesc")+"("+forecastDayListj.getString("aqi")+")\n限行:"+forecastDayListj.getString("tail_desc")+"\n日出:"+forecastDayListj.getString("fsun_rise")+"\n日落:"+forecastDayListj.getString("fsun_down")+"\n\n";
                                }
                            }
                            String forecastHourList=into.optString("forecastHourList");
                            if(forecastHourList==null||forecastHourList.equals("null")||forecastHourList.equals("false")||forecastHourList.isEmpty()) {
                                forecastHourList="无今日时刻天气预报\n";
                            }
                            else {
                                JSONArray jsona = into.getJSONArray("forecastHourList");
                                forecastHourList="";
                                for(int i=0;
                                i<jsona.length();
                                i++) {
                                    JSONObject forecastHourListj=jsona.getJSONObject(i);
                                    forecastHourList+="今天/明天"+forecastHourListj.getString("hour")+"时\n天气:"+forecastHourListj.getString("dayDesc")+"\n气温:"+forecastHourListj.getString("tmp")+"℃\n\n";
                                }
                            }
                            sendText(data,"今日总天气:\n"+alerts+aqi+condition+liveIndex);
                            sendText(data,"十五日天气预报:\n"+forecastDayList);
                            sendText(data,"每时天气预报:\n"+forecastHourList);
                            moji_list.clear();
                        }
                        else if(weather_list.size()>0) {
                            sendText(data,"暂无此城市或序号错误");
                        }
                    }
                    if(quntext.matches("[0-9]+")&&quntext.length()<=2) {
                        String mppskey=getPskey("mp.qq.com");
                        int index = Integer.parseInt(quntext);
                        if(index>0&&weather_list.size()>=index) {
                            String cookie="uin=o"+qq+"; skey="+getSkey()+"; p_uin=o"+qq+"; p_skey="+mppskey;
                            String url=httppost1("https://weather.mp.qq.com/cgi/share?g_tk="+GetGTK(mppskey),cookie,"{\"adcode\":"+weather_list.get(index-1)+"}");
                            JSONObject json=new JSONObject(url);
                            String into=json.getString("data");
                            if(into.contains("com.tencent.weather.share")) {
                                sendCard(qun,into,mtype);
                                sendText(data,"目前对方看不见该卡片，仅自己可见");
                                weather_list.clear();
                            }
                        }
                        else if(weather_list.size()>0) {
                            sendText(data,"暂无此城市或序号错误");
                        }
                    }
                }
            }
        }
    }
    ).start();
}