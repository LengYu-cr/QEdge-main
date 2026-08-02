public void 文案菜单(Object Yu) {
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
                if(quntext.equals("开启文案菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"文案菜单","开关",1);
                        String menu="已开启本聊天文案菜单";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭文案菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"文案菜单","开关",0);
                        String menu="已关闭本聊天文案菜单";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("文案菜单")) {
                    if(读(qun,"文案菜单","开关")==1) {
                        String menu = "文案菜单:\n伤感语录 随机笑话\n骚话语录 舔狗文案\n土味情话 随机古诗\n人间凑数 心理鸡汤\n温柔语录 爱情语录\n人生语录 社会语录\n文案一言 随机一言\n名言警句 感悟语录\n早安文案 午安文案\n晚安文案 毕业语录\n情感语录 正能量馆\n哲理文案 励志文案";
                        sendText(data,menu);
                    }
                    else {
                        sendText(data,"本聊天未开启文案菜单");
                    }
                }
                if("伤感语录".equals(quntext)) {
                    String nb=get(myWeb+"shanggan.php");
                    sendText(data,nb);
                }
                if("随机笑话".equals(quntext)) {
                    String nb=get(myWeb+"xiaohua.php");
                    sendText(data,nb);
                }
                if("英汉语录".equals(quntext)) {
                    String nb=get(myWeb+"yhyl.php");
                    sendText(data,nb);
                }
                if("骚话语录".equals(quntext)) {
                    String nb=get(myWeb+"saohua.php");
                    sendText(data,nb);
                }
                if("土味情话".equals(quntext)) {
                    String nb=get(myWeb+"twqh.php");
                    sendText(data,nb);
                }
                if("舔狗文案".equals(quntext)) {
                    String nb=get(myWeb+"tiangou.php");
                    sendText(data,nb);
                }
                if("随机古诗".equals(quntext)) {
                    String nb=get(myWeb+"gushi.php");
                    sendText(data,nb);
                }
                if("人间凑数".equals(quntext)) {
                    String nb=get(myWeb+"wzrjcs.php");
                    sendText(data,nb);
                }
                if("心理鸡汤".equals(quntext)) {
                    String nb=get(myWeb+"djt.php");
                    sendText(data,nb);
                }
                if("温柔语录".equals(quntext)) {
                    String nb=get(myWeb+"wenrou.php");
                    sendText(data,nb);
                }
                if("爱情语录".equals(quntext)) {
                    String nb=get(myWeb+"aiqing.php");
                    sendText(data,nb);
                }
                if("人生语录".equals(quntext)) {
                    String nb=get(myWeb+"rensheng.php");
                    sendText(data,nb);
                }
                if("社会语录".equals(quntext)) {
                    String nb=get(myWeb+"shehui.php");
                    sendText(data,nb);
                }
                if("文案一言".equals(quntext)) {
                    String nb=get(myWeb+"yiyan_new.php");
                    sendText(data,nb);
                }
                if("随机一言".equals(quntext)) {
                    String nb=get(myWeb+"yiyan.php");
                    sendText(data,nb);
                }
                if(quntext.equals("名言警句")) {
                    String url=get("https://zhengnengliang.52cha.com/t/7.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("感悟语录")) {
                    String url=get("https://zhengnengliang.52cha.com/t/6.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("午安文案")) {
                    String url=get("https://zhengnengliang.52cha.com/t/13.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("毕业语录")) {
                    String url=get("https://zhengnengliang.52cha.com/t/9.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("情感语录")) {
                    String url=get("https://zhengnengliang.52cha.com/t/11.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("正能量馆")) {
                    String url=get("https://zhengnengliang.52cha.com/t/4.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("励志文案")) {
                    String url=get("https://zhengnengliang.52cha.com/t/1.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("哲理文案")) {
                    String url=get("https://zhengnengliang.52cha.com/t/3.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("晚安文案")) {
                    String url=get("https://zhengnengliang.52cha.com/t/12.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
                if(quntext.equals("早安文案")) {
                    String url=get("https://zhengnengliang.52cha.com/t/2.html");
                    int index = url.lastIndexOf("<p class=\"textCnt\">");
                    String text = url.substring(index + 19);
                    int rd = text.indexOf("</p>");
                    String re = text.substring(0,rd);
                    sendText(data,""+re);
                }
            }
        }
    }
    ).start();
}