public void 代管功能(Object msg)
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
                if("代管功能".equals(quntext)) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String menu="代管功能:\n添加本群代管@QQ\n添加全局代管@QQ\n删除本群代管@QQ\n删除全局代管@QQ\n清空本群代管\n清空全局代管\n本群代管列表\n全局代管列表";
                        sendText(data,menu);
                    }
                }
                if(quntext.startsWith("添加全局代管@") && qq.equals(uin))
                {
                    String at = data.atList.get(0);
                    if(at.equals(qq))
                    {
                        sendText(data, "请不要添加自己为代管！");
                    }
                    else
                    {
                        if(读("0", "代管", at) == 1)
                        {
                            sendText(data, "已添加过该QQ为全局代管～");
                        }
                        else
                        {
                            写("0", "代管", at, 1);
                            sendText(data, "写入全局代管成功～");
                        }
                    }
                }
                if(quntext.matches("添加全局代管[0-9]+") && qq.equals(uin))
                {
                    String at = quntext.substring(6);
                    if(at.equals(qq))
                    {
                        sendText(data, "请不要添加自己为代管！");
                    }
                    else
                    {
                        if(读("0", "代管", at) == 1)
                        {
                            sendText(data, "已添加过该QQ为全局代管～");
                        }
                        else
                        {
                            写("0", "代管", at, 1);
                            sendText(data, "写入全局代管成功～");
                        }
                    }
                }
                if(quntext.matches("添加本群代管[0-9]+"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String at = quntext.substring(6);
                        if(at.equals(qq))
                        {
                            sendText(data, "请不要添加自己为代管！");
                        }
                        else
                        {
                            if(读(qun, "代管", at) == 1)
                            {
                                sendText(data, "已添加过该QQ为本群代管～");
                            }
                            else
                            {
                                写(qun, "代管", at, 1);
                                sendText(data, "写入本群代管成功～");
                            }
                        }
                    }
                }
                if(quntext.startsWith("删除全局代管@") && qq.equals(uin))
                {
                    String at = data.atList.get(0);
                    if(读("0", "代管", at) != 1)
                    {
                        sendText(data, "该QQ并非全局代管～");
                    }
                    else
                    {
                        清除("0", "代管", at);
                        sendText(data, "删除全局代管成功～");
                    }
                }
                if(quntext.equals("清空全局代管") && qq.equals(uin))
                {
                    删除("0", "代管");
                    sendText(data, "已清空全局代管列表～");
                }
                if(quntext.startsWith("添加本群代管@"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String at = data.atList.get(0);
                        if(qq.equals(at) || 读("0", "代管", at) == 1)
                        {
                            sendText(data, "请不要添加自己或全局代管为代管！");
                        }
                        else
                        {
                            if(读(qun, "代管", at) == 1)
                            {
                                sendText(data, "已添加过该QQ为本群代管～");
                            }
                            else
                            {
                                写(qun, "代管", at, 1);
                                sendText(data, "写入本群代管成功～");
                            }
                        }
                    }
                }
                if(quntext.startsWith("删除本群代管@"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String at = data.atList.get(0);
                        if(读(qun, "代管", at) != 1)
                        {
                            sendText(data, "该QQ并非本群代管～");
                        }
                        else
                        {
                            清除(qun, "代管", at);
                            sendText(data, "删除本群代管成功～");
                        }
                    }
                }
                if(quntext.matches("删除本群代管[0-9]+"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String at = quntext.substring(6);
                        if(读(qun, "代管", at) != 1)
                        {
                            sendText(data, "该QQ并非本群代管～");
                        }
                        else
                        {
                            清除(qun, "代管", at);
                            sendText(data, "删除本群代管成功～");
                        }
                    }
                }
                if(quntext.matches("删除全局代管[0-9]+"))
                {
                    if(qq.equals(uin))
                    {
                        String at = quntext.substring(6);
                        if(读("0", "代管", at) != 1)
                        {
                            sendText(data, "该QQ并非全局代管～");
                        }
                        else
                        {
                            清除("0", "代管", at);
                            sendText(data, "删除全局代管成功～");
                        }
                    }
                }
                if(quntext.equals("清空本群代管"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        删除(qun, "代管");
                        sendText(data, "已清空本群代管列表～");
                    }
                }
                if(quntext.equals("本群代管列表"))
                {
                    if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                    {
                        String[] List = 列表(qun, "代管");
                        String x = "本群代管列表有:";
                        String a = x;
                        long i = 0;
                        for(String s: List)
                        {
                            i++;
                            x = x + "\n" + i + "." + getUserName(s) + "(" + s + ")";
                        }
                        if(x.equals(a)) x = "目前还没有本群代管哦～";
                        sendText(data, x);
                    }
                }
                if(quntext.equals("全局代管列表") && qq.equals(uin))
                {
                    String[] List = 列表("0", "代管");
                    String x = "全局代管列表有:";
                    String a = x;
                    long i = 0;
                    for(String s: List)
                    {
                        i++;
                        x = x + "\n" + i + "." + getUserName(s) + "(" + s + ")";
                    }
                    if(x.equals(a)) x = "目前还没有全局代管哦～";
                    sendText(data, x);
                }
            }
        }
    }
    ).start();
}