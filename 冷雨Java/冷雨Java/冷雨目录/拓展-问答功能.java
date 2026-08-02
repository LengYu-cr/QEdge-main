public void 问答功能(Object Yu)
{
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu,""+Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq=myUin;
            int mtype=data.mtype;
            int msgtype=data.msgtype;
            long msgid=data.msgid;
            if(读(qun,"lengyu520","开关")==1&&cdxz(qun,uin)&&判断群(qun,mtype)==1) {
                if("问答功能".equals(quntext))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        String menu = "问答功能:\n精准类:\n开启/关闭本群精准问答\n添加本群精准问#问#答\n删除本群精准问#问\n本群精准问答列表\n开启/关闭全局精准问答\n添加全局精准问#问#答\n删除全局精准问#问\n全局精准问答列表\n模糊类:\n开启/关闭本群模糊问答\n添加本群模糊问#问#答\n删除本群模糊问#问\n本群模糊问答列表\n开启/关闭全局模糊问答\n添加全局模糊问#问#答\n删除全局模糊问#问\n全局模糊问答列表\n变量类:\n查看变量";
                        sendText(data, menu);
                    }
                }
                if("开启本群精准问答".equals(quntext))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        写(qun, "问答功能", "开关", 1);
                        String menu = "开启本群精准问答功能成功";
                        sendText(data, menu);
                    }
                }
                if("关闭本群精准问答".equals(quntext))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        写(qun, "问答功能", "开关", 0);
                        String menu = "关闭本群精准问答功能成功";
                        sendText(data, menu);
                    }
                }
                if(quntext.startsWith("添加本群精准问#"))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        String one = quntext.split("#")[1];
                        String two = quntext.split("#")[2];
                        写(qun, "问答系统", one, two);
                        sendText(data, "添加成功～");
                    }
                }
                if(读(qun, "问答功能", "开关") == 1)
                {
                    String text = 文字(qun, "问答系统", quntext);
                    if(text.startsWith("[语音"))
                    {
                        String cnm = text.substring(3);
                        String cnmm = cnm.replace("]", "");
                        sendPtt(qun, cnmm, 2);
                    }
                    else if(text.startsWith("[图片"))
                    {
                        String cnm = text.substring(3);
                        String cnmm = cnm.replace("]", "");
                        sendImg(qun, cnmm, 2);
                    }
                    else if(text.startsWith("[访问"))
                    {
                        String cnm = text.substring(3);
                        String cnmm = cnm.replace("]", "");
                        String cnmmb = get(cnmm);
                        sendText(data, cnmmb);
                    }
                    else if(text.equals("[拍一拍]"))
                    {
                        sendPaiYiPai(qun, uin, mtype);
                    }
                    else if(text.startsWith("Java:"))
                    {
                        String cnm = text.substring(5);
                                                this.interpreter.set("data", data);
                        this.interpreter.set("msg", data.originMsg);
                        this.interpreter.set("qun", qun);
                        this.interpreter.set("uin", uin);
                        this.interpreter.set("mtype", mtype);
                        this.interpreter.set("msgtype", msgtype);
                        this.interpreter.set("qq", myUin);
                        this.interpreter.eval(cnm,"eval stream");
                    }
                    else if(!text.equals(""))
                    {
                        String msg = text.replace("[at]", "[atUin=" + uin + "]");
                        msg = msg.replace("[qq]", qq);
                        msg = msg.replace("[uin]", uin);
                        msg = msg.replace("[qun]", qun);
                        msg = msg.replace("[Name]", getUserName(uin));
                        msg = msg.replace("[GroupName]", data.originMsg.peerName);
                        msg = msg.replace("[time]", timestampToDate(data.originMsg.msgTime * 1000));
                        msg = msg.replace("[图片", "\n[pic=");
                        msg = msg.replace("[GroupMemberCount]", getGroupMemberList(qun).size()+"");
                        File d = new File(ColdRainPath + "下载/随机一言.txt");
                        String menu = msg.replace("[一言]", 取文件(d));
                        sendText(data, menu);
                    }
                }
                if(quntext.startsWith("删除本群精准问#"))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        String one = quntext.split("#")[1];
                        清除(qun, "问答系统", one);
                        sendText(data, "删除成功～");
                    }
                }
                if(quntext.equals("清空本群精准问答"))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        删除(qun, "问答系统");
                        sendText(data, "已清空本群精准问答列表～");
                    }
                }
                if(quntext.equals("本群精准问答列表"))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        String[] List = 列表2(qun, "问答系统");
                        String x = "本群精准问答列表有:";
                        String a = x;
                        long i = 0;
                        for(String s: List)
                        {
                            i++;
                            x = x + "\n" + i + "." + s;
                        }
                        if(x.equals(a)) x = "目前还没有精准问答哦～";
                        sendText(data, x);
                    }
                }
                if("开启全局精准问答".equals(quntext))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        写("0", "问答功能", "开关", 1);
                        String menu = "开启全局精准问答功能成功";
                        sendText(data, menu);
                    }
                }
                if("关闭全局精准问答".equals(quntext))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        写("0", "问答功能", "开关", 0);
                        String menu = "关闭全局精准问答功能成功";
                        sendText(data, menu);
                    }
                }
                if(quntext.startsWith("添加全局精准问#"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String one = quntext.split("#")[1];
                        String two = quntext.split("#")[2];
                        写("0", "问答系统", one, two);
                        sendText(data, "添加成功～");
                    }
                }
                if(读("0", "问答功能", "开关") == 1)
                {
                    String text = 文字("0", "问答系统", quntext);
                    if(text.startsWith("[语音"))
                    {
                        String cnm = text.substring(3);
                        String cnmm = cnm.replace("]", "");
                        sendPtt(qun, cnmm, 2);
                    }
                    else if(text.startsWith("[图片"))
                    {
                        String cnm = text.substring(3);
                        String cnmm = cnm.replace("]", "");
                        sendImg(qun, cnmm, 2);
                    }
                    else if(text.startsWith("[访问"))
                    {
                        String cnm = text.substring(3);
                        String cnmm = cnm.replace("]", "");
                        String cnmmb = get(cnmm);
                        sendText(data, cnmmb);
                    }
                    else if(text.equals("[拍一拍]"))
                    {
                        sendPaiYiPai(qun, uin, mtype);
                    }
                    else if(text.startsWith("Java:"))
                    {
                        String cnm = text.substring(5);
                        msgCache=Yu;
                        dataCache=data;
                        this.interpreter.eval("Object data=dataCache;Object msg=msgCache;String uin=\""+uin+"\";int msgtype="+msgtype+";int mtype="+mtype+";String qun=\""+qun+"\";"+cnm,"eval stream");
                    }
                    else if(!text.equals(""))
                    {
                        String msg = text.replace("[at]", "[atUin=" + uin + "]");
                        msg = msg.replace("[qq]", qq);
                        msg = msg.replace("[uin]", uin);
                        msg = msg.replace("[qun]", qun);
                        msg = msg.replace("[Name]", getUserName(uin));
                        msg = msg.replace("[GroupName]", data.originMsg.peerName);
                        msg = msg.replace("[time]", timestampToDate(data.originMsg.msgTime * 1000));
                        msg = msg.replace("[图片", "\n[pic=");
                        msg = msg.replace("[GroupMemberCount]", getGroupMemberList(qun).size()+"");
                        File d = new File(ColdRainPath + "下载/随机一言.txt");
                        String menu = msg.replace("[一言]", 取文件(d));
                        sendText(data, menu);
                    }
                }
                if(quntext.startsWith("删除全局精准问#"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String one = quntext.split("#")[1];
                        清除("0", "问答系统", one);
                        sendText(data, "删除成功～");
                    }
                }
                if(quntext.equals("清空全局精准问答"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        删除("0", "问答系统");
                        sendText(data, "已清空全局精准问答列表～");
                    }
                }
                if(quntext.equals("全局精准问答列表"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String[] List = 列表2("0", "问答系统");
                        String x = "全局精准问答列表有:";
                        String a = x;
                        long i = 0;
                        for(String s: List)
                        {
                            i++;
                            x = x + "\n" + i + "." + s;
                        }
                        if(x.equals(a)) x = "目前还没有精准问答哦～";
                        sendText(data, x);
                    }
                }
                String quntexts="";
                if(读(qun, "模糊问答", "开关") == 1&&!quntext.contains("本群模糊问答列表有:")&&!quntext.contains("添加本群模糊问#")&&!quntext.contains("删除本群模糊问#"))
                {
                    cb = quntext.replace("⁡", "");
                    cd = cb.replace("'", "");
                    cf = cb.replace(" ", "");
                    cg = cf.replace(".", "");
                    ch = cg.replace("•", "");
                    op = ch.replace(",", "");
                    {
                        String[] 问答列表 = 列表2(qun, "模糊问答内容");
                        boolean tf=false;
                        for(String u: 问答列表)
                        {
                            if(op.contains(u)&&!op.equals(""))
                            {
                                tf=true;
                                quntexts=u;
                                break;
                            }
                        }
                        if(tf) {
                            String text = 文字(qun, "模糊问答内容", quntexts);
                            if(text.startsWith("[语音"))
                            {
                                String cnm = text.substring(3);
                                String cnmm = cnm.replace("]", "");
                                sendPtt(qun, cnmm, 2);
                            }
                            else if(text.startsWith("[图片"))
                            {
                                String cnm = text.substring(3);
                                String cnmm = cnm.replace("]", "");
                                sendImg(qun, cnmm, 2);
                            }
                            else if(text.startsWith("[访问"))
                            {
                                String cnm = text.substring(3);
                                String cnmm = cnm.replace("]", "");
                                String cnmmb = get(cnmm);
                                sendText(data, cnmmb);
                            }
                            else if(text.equals("[拍一拍]"))
                            {
                                sendPaiYiPai(qun, uin, mtype);
                            }
                            else if(text.startsWith("Java:"))
                            {
                                String cnm = text.substring(5);
                                msgCache=Yu;
                                dataCache=data;
                                this.interpreter.eval("Object data=dataCache;Object msg=msgCache;String uin=\""+uin+"\";int msgtype="+msgtype+";int mtype="+mtype+";String qun=\""+qun+"\";"+cnm,"eval stream");
                            }
                            else if(!text.equals(""))
                            {
                                String msg = text.replace("[at]", "[atUin=" + uin + "]");
                                msg = msg.replace("[qq]", qq);
                                msg = msg.replace("[uin]", uin);
                                msg = msg.replace("[qun]", qun);
                                msg = msg.replace("[Name]", getUserName(uin));
                                msg = msg.replace("[GroupName]", data.originMsg.peerName);
                                msg = msg.replace("[time]", timestampToDate(data.originMsg.msgTime * 1000));
                                msg = msg.replace("[图片", "\n[pic=");
                                msg = msg.replace("[GroupMemberCount]", getGroupMemberList(qun).size()+"");
                                File d = new File(ColdRainPath + "下载/随机一言.txt");
                                String menu = msg.replace("[一言]", 取文件(d));
                                sendText(data, menu);
                            }
                        }
                    }
                }
                if(读("0", "模糊问答", "开关") == 1&&!quntext.contains("全局模糊问答列表有:")&&!quntext.contains("添加全局模糊问#")&&!quntext.contains("删除全局模糊问#"))
                {
                    cb = quntext.replace("⁡", "");
                    cd = cb.replace("'", "");
                    cf = cb.replace(" ", "");
                    cg = cf.replace(".", "");
                    ch = cg.replace("•", "");
                    op = ch.replace(",", "");
                    {
                        String[] 问答列表 = 列表2("0", "模糊问答内容");
                        boolean tf=false;
                        for(String u: 问答列表)
                        {
                            if(op.contains(u)&&!op.equals(""))
                            {
                                tf=true;
                                quntexts=u;
                                break;
                            }
                        }
                        if(tf) {
                            String text = 文字("0", "模糊问答内容", quntexts);
                            if(text.startsWith("[语音"))
                            {
                                String cnm = text.substring(3);
                                String cnmm = cnm.replace("]", "");
                                sendPtt(qun, cnmm, 2);
                            }
                            else if(text.startsWith("[图片"))
                            {
                                String cnm = text.substring(3);
                                String cnmm = cnm.replace("]", "");
                                sendImg(qun, cnmm, 2);
                            }
                            else if(text.startsWith("[访问"))
                            {
                                String cnm = text.substring(3);
                                String cnmm = cnm.replace("]", "");
                                String cnmmb = get(cnmm);
                                sendText(data, cnmmb);
                            }
                            else if(text.equals("[拍一拍]"))
                            {
                                sendPaiYiPai(qun, uin, mtype);
                            }
                            else if(text.startsWith("Java:"))
                            {
                                String cnm = text.substring(5);
                                msgCache=Yu;
                                dataCache=data;
                                this.interpreter.eval("String uin=\""+uin+"\";int msgtype="+msgtype+";int mtype="+mtype+";String qun=\""+qun+"\";"+cnm,"eval stream");
                            }
                            else if(!text.equals(""))
                            {
                                String msg = text.replace("[at]", "[atUin=" + uin + "]");
                                msg = msg.replace("[qq]", qq);
                                msg = msg.replace("[uin]", uin);
                                msg = msg.replace("[qun]", qun);
                                msg = msg.replace("[Name]", getUserName(uin));
                                msg = msg.replace("[GroupName]", data.originMsg.peerName);
                                msg = msg.replace("[time]", timestampToDate(data.originMsg.msgTime * 1000));
                                msg = msg.replace("[图片", "\n[pic=");
                                msg = msg.replace("[GroupMemberCount]", getGroupMemberList(qun).size()+"");
                                File d = new File(ColdRainPath + "下载/随机一言.txt");
                                String menu = msg.replace("[一言]", 取文件(d));
                                sendText(data, menu);
                            }
                        }
                    }
                }
                if("开启本群模糊问答".equals(quntext))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        写(qun, "模糊问答", "开关", 1);
                        String menu = "开启本群模糊问答功能成功";
                        sendText(data, menu);
                    }
                }
                if("关闭本群模糊问答".equals(quntext))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        写(qun, "模糊问答", "开关", 0);
                        String menu = "关闭本群模糊问答功能成功";
                        sendText(data, menu);
                    }
                }
                if(quntext.startsWith("添加本群模糊问#"))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        String one = quntext.split("#")[1];
                        String two = quntext.split("#")[2];
                        写(qun, "模糊问答内容", one, two);
                        sendText(data, "添加成功～");
                    }
                }
                if(quntext.startsWith("删除本群模糊问#"))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        String one = quntext.split("#")[1];
                        清除(qun, "模糊问答内容", one);
                        sendText(data, "删除成功～");
                    }
                }
                if(quntext.equals("清空本群模糊问答"))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        删除(qun, "模糊问答内容");
                        sendText(data, "已清空本群模糊问答列表～");
                    }
                }
                if(quntext.equals("本群模糊问答列表"))
                {
                    if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                    {
                        String[] List = 列表2(qun, "模糊问答内容");
                        String x = "本群模糊问答列表有:";
                        String a = x;
                        long i = 0;
                        for(String s: List)
                        {
                            i++;
                            x = x + "\n" + i + "." + s;
                        }
                        if(x.equals(a)) x = "目前还没有模糊问答哦～";
                        sendText(data, x);
                    }
                }
                if("开启全局模糊问答".equals(quntext))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        写("0", "模糊问答", "开关", 1);
                        String menu = "开启全局模糊问答功能成功";
                        sendText(data, menu);
                    }
                }
                if("关闭全局模糊问答".equals(quntext))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        写("0", "模糊问答", "开关", 0);
                        String menu = "关闭全局模糊问答功能成功";
                        sendText(data, menu);
                    }
                }
                if(quntext.startsWith("添加全局模糊问#"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String one = quntext.split("#")[1];
                        String two = quntext.split("#")[2];
                        写("0", "模糊问答内容", one, two);
                        sendText(data, "添加成功～");
                    }
                }
                if(quntext.startsWith("删除全局模糊问#"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String one = quntext.split("#")[1];
                        清除("0", "模糊问答内容", one);
                        sendText(data, "删除成功～");
                    }
                }
                if(quntext.equals("清空全局模糊问答"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        删除("0", "模糊问答内容");
                        sendText(data, "已清空全局模糊问答列表～");
                    }
                }
                if(quntext.equals("全局模糊问答列表"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String[] List = 列表2("0", "模糊问答内容");
                        String x = "全局模糊问答列表有:";
                        String a = x;
                        long i = 0;
                        for(String s: List)
                        {
                            i++;
                            x = x + "\n" + i + "." + s;
                        }
                        if(x.equals(a)) x = "目前还没有模糊问答哦～";
                        sendText(data, x);
                    }
                }
            }
        }
    }
    ).start();
}