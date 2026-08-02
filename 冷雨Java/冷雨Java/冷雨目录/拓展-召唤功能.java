public void 召唤功能(Object Yu) {
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
                if(读("0","代管",uin)==1||qq.equals(uin)) {
                    if(quntext.equals("召唤功能")) {
                        String menu="召唤功能:\n召唤群主 召唤管理\n召唤管家 召唤群员";
                        sendText(data,""+menu);
                    }
                    if(quntext.startsWith("召唤管家")) {
                        sendText(data,"[atUin=2854196310]"+quntext.substring(4));
                    }
                    if(quntext.startsWith("召唤群主")) {
                        if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                            for(HashMap uin:getGroupMemberList(qun)) {
                                if(getAuthority(qun,""+uin.get("uin")).equals("群主")) {
                                    sendText(data,"[atUin="+uin.get("uin")+"]\n"+quntext.substring(4));
                                }
                            }
                        }
                        else if(Module.equals("QStory")) {
                            Object st=getGroupMemberList(qun);
                            for(Object b:st)
                            {
                                String um=b.UserUin;
                                if(getAuthority(qun,um).equals("群主")) {
                                    sendText(data,"[atUin="+um+"]\n"+quntext.substring(4));
                                }
                            }
                        }else if(Module.equals("QFun")) {
                            Object st=getGroupMemberList(qun);
                            for(Object b:st)
                            {
                                String um=b.uin;
                                if(getAuthority(qun,um).equals("群主")) {
                                    sendText(data,"[atUin="+um+"]\n"+quntext.substring(4));
                                }
                            }
                        }
                    }
                    if(quntext.startsWith("召唤管理")) {
                        String result="";
                        if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                            for(HashMap uin:getGroupMemberList(qun)) {
                                if(getAuthority(qun,""+uin.get("uin")).equals("管理员")) {
                                    result+="[atUin="+uin.get("uin")+"]";
                                }
                            }
                        }
                        else if(Module.equals("QStory")) {
                            Object st=getGroupMemberList(qun);
                            for(Object b:st)
                            {
                                String um=b.UserUin;
                                if(getAuthority(qun,um).equals("群主")) {
                                    result+="[atUin="+um+"]";
                                }
                            }
                        }else if(Module.equals("QFun")) {
                            Object st=getGroupMemberList(qun);
                            for(Object b:st)
                            {
                                String um=b.uin;
                                if(getAuthority(qun,um).equals("群主")) {
                                    result+="[atUin="+um+"]";
                                }
                            }
                        }
                        sendText(data,result+"\n"+quntext.substring(4));
                    }
                    if(quntext.startsWith("召唤群员")) {
                        String result="";
                        int num=0;
                        //<50
                        if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                            for(HashMap uin:getGroupMemberList(qun)) {
                                if(getAuthority(qun,""+uin.get("uin")).equals("群员")) {
                                    if(num>50) {
                                        num=0;
                                        sendText(data,result+"\n"+quntext.substring(4));
                                        result="";
                                    }
                                    result+="[atUin="+uin.get("uin")+"]";
                                    num++;
                                }
                            }
                        }
                        else if(Module.equals("QStory")) {
                            Object st=getGroupMemberList(qun);
                            for(Object b:st)
                            {
                                String um=b.UserUin;
                                if(getAuthority(qun,um).equals("群员")) {
                                    if(num>50) {
                                        num=0;
                                        sendText(data,result+"\n"+quntext.substring(4));
                                        result="";
                                    }
                                    result+="[atUin="+um+"]";
                                    num++;
                                }
                            }
                        }else if(Module.equals("QFun")) {
                            Object st=getGroupMemberList(qun);
                            for(Object b:st)
                            {
                                String um=b.uin;
                                if(getAuthority(qun,um).equals("群员")) {
                                    if(num>50) {
                                        num=0;
                                        sendText(data,result+"\n"+quntext.substring(4));
                                        result="";
                                    }
                                    result+="[atUin="+um+"]";
                                    num++;
                                }
                            }
                        }
                        if(!result.equals("")) {
                            sendText(data,result+"\n"+quntext.substring(4));
                        }
                    }
                }
            }
        }
    }
    ).start();
}