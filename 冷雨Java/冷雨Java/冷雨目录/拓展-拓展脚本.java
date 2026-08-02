public void 拓展脚本(Object Yu) {
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
            if(判断群(qun,mtype)==1) {
                if(quntext.equals("拓展脚本")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        getJavaList3();
                        String result="";
                        for(int i=0;
                        i<download1.size();
                        i++) {
                            boolean a = isAutoLoad(download5.get(i));
                            boolean a2 = hasLoaded(download6.get(i));
                            String switchs="";
                            String switchs2="未加载";
                            boolean a = isAutoLoad(download5.get(i));
                            boolean a2 = hasLoaded(download6.get(i));
                            if(a)switchs="，自启动";
                            if(a2)switchs2="已加载";
                            String load=switchs2+switchs;
                            result+=(i+1)+"、"+download1.get(i)+"("+load+")\n";
                        }
                        sendText(data,"本地拓展脚本列表:\n"+result+"请发送\"加载\"或\"开启/关闭自启动\"+序号选择\n发送\"一键全载\"即可加载全部");
                    }
                }
                if(quntext.matches("加载[0-9]+")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        try {
                            getJavaList3();
                            int num=Integer.parseInt(quntext.substring(2));
                            if(num>download1.size()) {
                                sendText(data,"暂无此拓展脚本");
                            }
                            else {
                                int tf=0;
                                String name="";
                                for(int i=0;
                                i<download1.size();
                                i++) {
                                    if(i+1==num) {
                                        name=download1.get(i);
                                        if(hasLoaded(download6.get(i))) {
                                            tf=2;
                                        }
                                        else {
                                            loadJava(download5.get(i));
                                            json_java.put(download6.get(i),1);
                                            tf=1;
                                        }
                                        break;
                                    }
                                }
                                if(tf==1) {
                                    sendText(data,"加载脚本("+name+")成功");
                                }
                                else if(tf==0) {
                                    sendText(data,"无此拓展脚本");
                                }
                                else if(tf==2) {
                                    sendText(data,"禁止重复加载，如有需要，请重启ColdRain_Java");
                                }
                            }
                        }
                        catch(e) {
                            sendText(data,"加载脚本出错了:"+e);
                        }
                    }
                }
                if(quntext.equals("一键全载")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        try {
                            getJavaList3();
                            String name="";
                            for(int i=0;
                            i<download1.size();
                            i++) {
                                if(hasLoaded(download6.get(i))) {
                                    //已加载
                                }
                                else {
                                    loadJava(download5.get(i));
                                    json_java.put(download6.get(i),1);
                                    name+=download1.get(i)+"\n";
                                }
                            }
                            sendText(data,"加载脚本:\n"+name+"成功");
                        }
                        catch(e) {
                            sendText(data,"加载脚本出错了:"+e);
                        }
                    }
                }
                if(quntext.matches("开启自启动[0-9]+")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        try {
                            getJavaList3();
                            int num=Integer.parseInt(quntext.substring(5));
                            if(num>download1.size()) {
                                sendText(data,"暂无此拓展脚本");
                            }
                            else {
                                boolean tf=false;
                                String name="";
                                for(int i=0;
                                i<download1.size();
                                i++) {
                                    if(i+1==num) {
                                        name=download1.get(i);
                                        AddJava(download5.get(i));
                                        tf=true;
                                        break;
                                    }
                                }
                                if(tf) {
                                    sendText(data,"添加自动加载脚本("+name+")成功");
                                }
                                else {
                                    sendText(data,"无此拓展脚本");
                                }
                            }
                        }
                        catch(e) {
                            sendText(data,"添加自动加载脚本出错了:"+e);
                        }
                    }
                }
                if(quntext.matches("关闭自启动[0-9]+")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        try {
                            getJavaList3();
                            int num=Integer.parseInt(quntext.substring(5));
                            if(num>download1.size()) {
                                sendText(data,"暂无此拓展脚本");
                            }
                            else {
                                boolean tf=false;
                                String name="";
                                for(int i=0;
                                i<download1.size();
                                i++) {
                                    if(i+1==num) {
                                        name=download1.get(i);
                                        DeleteJava(download5.get(i));
                                        tf=true;
                                        break;
                                    }
                                }
                                if(tf) {
                                    sendText(data,"删除自动加载脚本("+name+")成功");
                                }
                                else {
                                    sendText(data,"无此拓展脚本");
                                }
                            }
                        }
                        catch(e) {
                            sendText(data,"删除自动加载脚本出错了:"+e);
                        }
                    }
                }
            }
        }
    }
    ).start();
}