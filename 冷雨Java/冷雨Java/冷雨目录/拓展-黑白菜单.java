public void 黑白菜单(Object Yu) {
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
                if(quntext.equals("本群黑名单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String[] List=列表(qun,"黑名单");
                        String x="本群黑名单列表有:";
                        String a=x;
                        long i=0;
                        for(String s:List) {
                            i++;
                            x=x+"\n"+i+"."+s;
                        }
                        if(x.equals(a)) x="目前还没有本群黑名单哦～";
                        sendText(data,x);
                    }
                }
                if(quntext.equals("本群白名单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String[] List=列表(qun,"白名单");
                        String x="本群白名单列表有:";
                        String a=x;
                        long i=0;
                        for(String s:List) {
                            i++;
                            x=x+"\n"+i+"."+s;
                        }
                        if(x.equals(a)) x="目前还没有本群白名单哦～";
                        sendText(data,x);
                    }
                }
                if(quntext.equals("全局黑名单")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String[] List=列表("0","黑名单");
                        String x="全局黑名单列表有:";
                        String a=x;
                        long i=0;
                        for(String s:List) {
                            i++;
                            x=x+"\n"+i+"."+s;
                        }
                        if(x.equals(a)) x="目前还没有全局黑名单哦～";
                        sendText(data,x);
                    }
                }
                if(quntext.equals("全局白名单")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String[] List=列表("0","白名单");
                        String x="全局白名单列表有:";
                        String a=x;
                        long i=0;
                        for(String s:List) {
                            i++;
                            x=x+"\n"+i+"."+s;
                        }
                        if(x.equals(a)) x="目前还没有全局白名单哦～";
                        sendText(data,x);
                    }
                }
                if(quntext.startsWith("本群拉黑@")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        if(读(qun,"黑名单",at)==1) {
                            sendText(data,"已添加过该QQ为本群黑名单～");
                            kickGroup(qun,at,false);
                        }
                        else {
                            写(qun,"黑名单",at,1);
                            sendText(data,"添加本群黑名单成功～");
                            kickGroup(qun,at,false);
                        }
                    }
                }
                if(quntext.matches("本群拉黑[0-9]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=quntext.substring(4);
                        if(读(qun,"黑名单",at)==1) {
                            sendText(data,"已添加过该QQ为本群黑名单～");
                            kickGroup(qun,at,false);
                        }
                        else {
                            写(qun,"黑名单",at,1);
                            sendText(data,"添加本群黑名单成功～");
                            kickGroup(qun,at,false);
                        }
                    }
                }
                if(quntext.startsWith("本群删黑@")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        if(读(qun,"黑名单",at)!=1) {
                            sendText(data,"该QQ并非本群黑名单～");
                        }
                        else {
                            清除(qun,"黑名单",at);
                            sendText(data,"删除本群黑名单成功～");
                        }
                    }
                }
                if(quntext.matches("本群删黑[0-9]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=quntext.substring(4);
                        if(读(qun,"黑名单",at)!=1) {
                            sendText(data,"该QQ并非本群黑名单～");
                        }
                        else {
                            清除(qun,"黑名单",at);
                            sendText(data,"删除本群黑名单成功～");
                        }
                    }
                }
                if(quntext.startsWith("全局拉黑@")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        if(读("0","黑名单",at)==1) {
                            sendText(data,"已添加过该QQ为全局黑名单～");
                            kickGroup(qun,at,false);
                        }
                        else {
                            写("0","黑名单",at,1);
                            sendText(data,"添加全局黑名单成功～");
                            kickGroup(qun,at,false);
                        }
                    }
                }
                if(quntext.matches("全局拉黑[0-9]+")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String at=quntext.substring(4);
                        if(读("0","黑名单",at)==1) {
                            sendText(data,"已添加过该QQ为全局黑名单～");
                            kickGroup(qun,at,false);
                        }
                        else {
                            写("0","黑名单",at,1);
                            sendText(data,"添加全局黑名单成功～");
                            kickGroup(qun,at,false);
                        }
                    }
                }
                if(quntext.startsWith("全局删黑@")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        if(读("0","黑名单",at)!=1) {
                            sendText(data,"该QQ并非全局黑名单～");
                        }
                        else {
                            清除("0","黑名单",at);
                            sendText(data,"删除全局黑名单成功～");
                        }
                    }
                }
                if(quntext.matches("全局删黑[0-9]+")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String at=quntext.substring(4);
                        if(读("0","黑名单",at)!=1) {
                            sendText(data,"该QQ并非全局黑名单～");
                        }
                        else {
                            清除("0","黑名单",at);
                            sendText(data,"删除全局黑名单成功～");
                        }
                    }
                }
                if(quntext.equals("清空本群黑名")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        删除(qun,"黑名单");
                        sendText(data,"已清空本群黑名单列表～");
                    }
                }
                if(quntext.equals("清空全局黑名")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        删除("0","黑名单");
                        sendText(data,"已清空全局黑名单列表～");
                    }
                }
                if(quntext.equals("黑白菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String menu="黑白菜单:\n全局拉黑@QQ/+QQ\n全局删黑@QQ/+QQ\n全局拉白@QQ/+QQ\n全局删白@QQ/+QQ\n本群拉黑@QQ/+QQ\n本群删黑@QQ/+QQ\n本群拉白@QQ/+QQ\n本群删白@QQ/+QQ\n清空本群黑名\n清空全局黑名\n清空本群白名\n清空全局白名\n本群/全局黑名/白名单\n踢黑@QQ/+QQ\n我的身份";
                        sendText(data,menu);
                    }
                }
                if(quntext.startsWith("本群拉白@")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        if(读(qun,"白名单",at)==1) {
                            sendText(data,"已添加过该QQ为本群白名单～");
                        }
                        else {
                            写(qun,"白名单",at,1);
                            sendText(data,"添加本群白名单成功～");
                        }
                    }
                }
                if(quntext.matches("本群拉白[0-9]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=quntext.substring(4);
                        if(读(qun,"白名单",at)==1) {
                            sendText(data,"已添加过该QQ为本群白名单～");
                        }
                        else {
                            写(qun,"白名单",at,1);
                            sendText(data,"添加本群白名单成功～");
                        }
                    }
                }
                if(quntext.startsWith("本群删白@")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        if(读(qun,"白名单",at)!=1) {
                            sendText(data,"该QQ并非本群白名单～");
                        }
                        else {
                            清除(qun,"白名单",at);
                            sendText(data,"删除本群白名单成功～");
                        }
                    }
                }
                if(quntext.matches("本群删白[0-9]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=quntext.substring(4);
                        if(读(qun,"白名单",at)!=1) {
                            sendText(data,"该QQ并非本群白名单～");
                        }
                        else {
                            清除(qun,"白名单",at);
                            sendText(data,"删除本群白名单成功～");
                        }
                    }
                }
                if(quntext.startsWith("全局拉白@")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        if(读("0","白名单",at)==1) {
                            sendText(data,"已添加过该QQ为全局白名单～");
                        }
                        else {
                            写("0","白名单",at,1);
                            sendText(data,"添加全局白名单成功～");
                        }
                    }
                }
                if(quntext.matches("全局拉白[0-9]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=quntext.substring(4);
                        if(读("0","白名单",at)==1) {
                            sendText(data,"已添加过该QQ为全局白名单～");
                        }
                        else {
                            写("0","白名单",at,1);
                            sendText(data,"添加全局白名单成功～");
                        }
                    }
                }
                if(quntext.startsWith("全局删白@")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        if(读("0","白名单",at)!=1) {
                            sendText(data,"该QQ并非全局白名单～");
                        }
                        else {
                            清除("0","白名单",at);
                            sendText(data,"删除全局白名单成功～");
                        }
                    }
                }
                if(quntext.matches("全局删白[0-9]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=quntext.substring(4);
                        if(读("0","白名单",at)!=1) {
                            sendText(data,"该QQ并非全局白名单～");
                        }
                        else {
                            清除("0","白名单",at);
                            sendText(data,"删除全局白名单成功～");
                        }
                    }
                }
                if(quntext.equals("清空本群白名")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        删除(qun,"白名单");
                        sendText(data,"已清空本群白名单列表～");
                    }
                }
                if(quntext.equals("清空全局白名")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        删除("0","白名单");
                        sendText(data,"已清空全局白名单列表～");
                    }
                }
                if("我的身份".equals(quntext)) {
                    if(读(qun,"代管",uin)==1) a="⭕";
                    else a="❌";
                    if(读("0","代管",uin)==1) b="⭕";
                    else b="❌";
                    if(读(qun,"白名单",uin)==1) c="⭕";
                    else c="❌";
                    if(读("0","白名单",uin)==1) d="⭕";
                    else d="❌";
                    if(读(qun,"黑名单",uin)==1) e="⭕";
                    else e="❌";
                    if(读("0","黑名单",uin)==1) f="⭕";
                    else f="❌";
                    if(Arrays.asList(owner).contains(uin)) zz="\n作者权限:⭕";
                    else zz="";
                    String menu="QQ:"+uin+"\n本群代管:"+a+"\n全局代管:"+b+"\n本群白名单:"+c+"\n全局白名单:"+d+"\n本群黑名单:"+e+"\n全局黑名单:"+f+zz+"\n在本群权限:"+getAuthority(qun,uin);
                    sendText(data,menu);
                }
                if(JudgeMyPermissions(qun))
                {
                    if(读(qun,"黑名单",uin)==1) {
                        shutUp(qun,uin,2592000);
                        kickGroup(qun,uin,true);
                        String menu="QQ:"+uin+"("+getUserName(uin)+")是本群黑名单，已踢出本群！";
                        sendText(data,menu);
                    }
                    if(读("0","黑名单",uin)==1) {
                        shutUp(qun,uin,2592000);
                        kickGroup(qun,uin,true);
                        String menu="QQ:"+uin+"("+getUserName(uin)+")是全局黑名单，已踢出本群！";
                        sendText(data,menu);
                    }
                }
            }
        }
    }
    ).start();
}