public void 站长功能(Object msg)
{
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(msg,""+Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq=myUin;
            int mtype=data.mtype;
            long msgid=data.msgid;
            if(判断群(qun,mtype)==1) {
                if(qq.equals(uin)||读("0","代管",uin)==1) {
                    if(quntext.equals("站长功能")) {
                        sendText(data,"站长功能:\nGET+链接\n网页截图+链接\n防红检测+链接\nping+链接\n本地ping+链接\n上传文件+文件路径\n腾讯图床+图片链接\n生成二维码+内容");
                    }
                    if(quntext.startsWith("腾讯图床2https://")||quntext.startsWith("腾讯图床2http://")) {
                        String text=get(myWeb+"image.php?uin="+myUin+"&skey="+getSkey()+"&p_skey="+getPskey("qun.qq.com")+"&url="+URL(quntext.substring(5),1));
                        try {
                            JSONObject json = new JSONObject(text);
                            if(json.getInt("code")==0) {
                                sendText(data,"图片上传成功\n图片链接："+json.getJSONObject("data").getString("origin"));
                            }
                            else {
                                sendText(data,"错误:腾讯图床上传失败 => "+json.getString("msg"));
                            }
                        }
                        catch(e) {
                            sendText(data,"错误:腾讯图床上传失败 => "+e.getMessage());
                        }
                    }
                    if(quntext.startsWith("生成二维码")||quntext.startsWith("生成二维码")) {
                        String text=myWeb+"qrcode.php?text="+URL(quntext.substring(5),1);
                        sendImg(qun,text,mtype);
                    }
                    if(quntext.startsWith("腾讯图床https://")||quntext.startsWith("腾讯图床http://")) {
                        String text=get(myWeb+"image2.php?uin="+myUin+"&skey="+getSkey()+"&p_skey="+getPskey("qun.qq.com")+"&url="+URL(quntext.substring(4),1));
                        if(text.startsWith("https://p.qlogo.cn/gdynamic/")) {
                            sendText(data,"图片上传成功\n图片链接："+text);
                        }
                        else {
                            sendText(data,"错误:腾讯图床上传失败 => "+text);
                        }
                    }
                    if(quntext.startsWith("上传文件")) {
                        if(qq.equals(uin)) {
                            String url=quntext.substring(4);
                            if(url.startsWith("http")) {
                                String myUrl=ColdRainPath+"/下载/上传文件"+getFilesType(url);
                                DownloadToFile(url,myUrl);
                                url=myUrl;
                            }
                            String jsonStr=uploadFile("https://chatglm.cn/chatglm/backend-api/assistant/file_upload","file",url);
                            if(jsonStr.equals("10001")) {
                                sendText(data,"错误：文件不存在或不可读");
                                return;
                            }
                            else if(jsonStr.equals("10002")) {
                                sendText(data,"警告：文件大小超过20MB");
                                return;
                            }
                            JSONObject jsonObject = new JSONObject(jsonStr);
                            JSONObject resultObject = jsonObject.getJSONObject("result");
                            if(resultObject.has("file_url")) {
                                String file_url = resultObject.getString("file_url");
                                sendText(data,"文件上传成功\n文件链接："+file_url);
                            }
                            else {
                                sendText(data,jsonStr);
                            }
                        }
                    }
                    if(quntext.startsWith("GET")) {
                        String url=quntext.substring(3);
                        if(url.isEmpty()) {
                            sendText(data,"请携带链接");
                        }
                        else {
                            if(url.startsWith("https://")||url.startsWith("http://")) {
                                url=get(url);
                                if(url.length()>=10000) {
                                    sendText(data,"字节过长，无法发送，请抓包查看。\n当前字节:"+url.length());
                                }
                                else {
                                    sendText(data,u解(url));
                                    sendText(data,"成功，总字节:"+url.length());
                                }
                            }
                            else if(!quntext.equals("访问网页失败")) {
                                sendText(data,"不支持的链接，未以https://或者http://开头");
                            }
                        }
                    }
                    if(quntext.startsWith("网页截图")) {
                        String url=quntext.substring(4);
                        if(url.isEmpty()) {
                            sendText(data,"请携带链接");
                        }
                        else {
                            String u=User(6);
                            if(url.startsWith("https://")||url.startsWith("http://")) {
                                url=DownloadToFile("https://api.lolimi.cn/API/akz/?url="+URL(url,1),ColdRainPath+"图片/网页快照"+u+".png");
                                if(url.equals("成功")) {
                                    sendText(data,"截图"+url+":[pic="+ColdRainPath+"图片/网页快照"+u+".png"+"]");
                                }
                                else {
                                    sendText(data,"截图"+url);
                                }
                            }
                            else {
                                sendText(data,"不支持的链接，未以https://或者http://开头");
                            }
                        }
                    }
                    if(quntext.startsWith("防红检测")) {
                        String url=quntext.substring(4);
                        if(url.isEmpty()) {
                            sendText(data,"请携带链接");
                        }
                        else {
                            if(url.startsWith("https://")||url.startsWith("http://")) {
                                url=get(myWeb+"qq_get.php?url="+URL(url,1));
                                if(url.equals("")||url.equals("访问网页失败")) {
                                    sendText(data,"访问网页失败");
                                }
                                else {
                                    JSONObject json = new JSONObject(url);
                                    String text="无返回数据";
                                    int ret=json.getInt("ret");
                                    if(ret==0) {
                                        String title=json.getString("title");
                                        String abstracts=json.getString("abstract");
                                        if(title.equals(""))title=title;
                                        else title="\n"+title+"";
                                        if(abstracts.equals(""))abstracts=abstracts;
                                        else abstracts="\n"+abstracts+"";
                                        text="该网页在QQ可以正常打开"+title+""+abstracts+"[pic="+json.getString("cover")+"]";
                                    }
                                    else {
                                        text=json.getString("text");
                                    }
                                    sendText(data,text);
                                }
                            }
                            else {
                                sendText(data,"不支持的链接，未以https://或者http://开头");
                            }
                        }
                    }
                    if(quntext.startsWith("本地ping")) {
                        String url=quntext.substring(6);
                        if(url.isEmpty()) {
                            sendText(data,"请携带链接");
                        }
                        else {
                            if(url.startsWith("https://")||url.startsWith("http://")) {
                                url=pingGet(url);
                                sendText(data,url);
                            }
                            else {
                                sendText(data,"不支持的链接，未以https://或者http://开头");
                            }
                        }
                    }
                    if(quntext.startsWith("ping")) {
                        String url=quntext.substring(4);
                        if(url.isEmpty()) {
                            sendText(data,"请携带链接");
                        }
                        else {
                            if(url.startsWith("https://")||url.startsWith("http://")) {
                                url=pingGet2(url);
                                sendText(data,url);
                            }
                            else {
                                sendText(data,"不支持的链接，未以https://或者http://开头");
                            }
                        }
                    }
                }
            }
        }
    }
    ).start();
}
import java.net.InetAddress;
public String pingGet(String url)
{
    long start = System.currentTimeMillis();
    String result="";
    String result1="";
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL urlObj = new URL(url);
        URLConnection uc = urlObj.openConnection();
        uc.setRequestProperty("Cookie", "");
        uc.setRequestProperty("user-agent", "Dalvik/2.1.0 (Linux; U; Android 13; V2166BA Build/TP1A.220624.014)");
        uc.setConnectTimeout(10000);
        uc.setReadTimeout(10000);
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while((line = reader.readLine()) != null)
        {
            buffer.append(line + "\n");
        }
        for (String headerName : uc.getHeaderFields().keySet()) {
            result1+="Headers:\n"+headerName + ": " + uc.getHeaderField(headerName);
        }
        InetAddress inetAddress = InetAddress.getByName(urlObj.getHost());
        String getHostAddress=inetAddress.getHostAddress();
        int responseCode = uc.getResponseCode();
        double need=(System.currentTimeMillis()-start)/1000.0;
        result="Url:"+url+"\nCode:"+responseCode+"\nip:"+getHostAddress+"\nneedtime:"+need+" seconds\nbytes:"+buffer.length()+"("+formatSize((float)buffer.length())+")\n"+result1.replace("null:","");
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        try
        {
            if(null != isr)
            {
                isr.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    if(buffer.length() == 0) return "出错";
    buffer.delete(buffer.length() - 1, buffer.length());
    return result;
}
public String pingGet2(String url)
{
    String U=get("https://api.lolimi.cn/API/ping/api.php?url="+url);
    if(U.equals("")||U.equals("访问网页失败")) {
        return "接口失效:api.lolimi.cn";
    }
    else {
        JSONObject json=new JSONObject(U);
        if(json.getInt("code")==1) {
            json=json.getJSONObject("data");
            return "URL:"+json.getString("url")+
            "\nIP:"+json.getString("IP")+
            "\nAddress:"+json.getString("address")+
            "\nTime-Consuming:"+json.getString("Times");
        }
        else {
            return "获取数据失败:"+json.getString("text");
        }
    }
}