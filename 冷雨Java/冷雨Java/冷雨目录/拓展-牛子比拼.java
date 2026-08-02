public void 牛子比拼(Object Yu)
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
            long msgid=data.msgid;
            int msgtype=data.msgtype;
            if(mtype==2) {
                if(quntext.equals("开启牛子比拼")) {
                    if(读("0","代管",uin)==1||qq.equals(uin)) {
                        写(qun,"牛子比拼","开关",1);
                        String menu="本群已开启牛子比拼";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭牛子比拼")) {
                    if(读("0","代管",uin)==1||qq.equals(uin)) {
                        写(qun,"牛子比拼","开关",0);
                        String menu="本群已关闭牛子比拼";
                        sendText(data,menu);
                    }
                }
                
                if(读(qun,"牛子比拼","开关")==1) {
                    if(quntext.equals("牛子比拼")) {
                        String menu="牛子比拼:\n领养牛子 我的牛子\n申请炉管 我要月抛\n牛子比拼@QQ/+QQ\n牛子攻击@QQ/+QQ\n牛子排行榜\n开启/关闭牛子比拼 (开)\nTip:若引起不适，请视而不见😋";
                        sendText(data,menu);
                    }
                    if(quntext.matches("设置每日比拼次数[0-9]+")) {
                        if(读("0","代管",uin)==1||qq.equals(uin)) {
                            String at=quntext.substring(8);
                            写("0","牛子比拼","次数",at);
                            sendText(data,"设置成功");
                        }
                    }
                    if(quntext.matches("设置每日攻击次数[0-9]+")) {
                        if(读("0","代管",uin)==1||qq.equals(uin)) {
                            String at=quntext.substring(8);
                            写("0","牛子攻击","次数",at);
                            sendText(data,"设置成功");
                        }
                    }
                    int 每日次数=读("0","牛子比拼","次数");
                    if(每日次数==0) {
                        每日次数=20;
                        写("0","牛子比拼","次数",20);
                    }
                    int 每日攻击次数=读("0","牛子攻击","次数");
                    if(每日攻击次数==0) {
                        每日攻击次数=20;
                        写("0","牛子攻击","次数",20);
                    }
                    if(quntext.equals("我的牛子")) {
                        if(文字(qun,"牛子比拼",uin).equals("")) {
                            sendText(data,uin+"您还没有长牛子，请『领养牛子』");
                        }
                        else {
                            int length=读(qun,"牛子长度",uin);
                            String lengths=getLength(length);
                            sendText(data,uin+"您的牛子长度为"+lengths+"("+length+"mm)\n称号:"+getNick(length));
                        }
                    }
                    if(quntext.startsWith("牛子比拼@")||quntext.startsWith("比拼牛子@")) {
                        int 次数=读(qun,"牛子比拼次数"+uin,年月日());
                        if(次数<=0&&次数>-每日次数) {
                            if(data.atList.size()<1) {
                                sendText(data,uin+"请真实艾特对方");
                            }
                            else {
                                String at=data.atList.get(0);
                                if(at.equals(uin)) {
                                    sendText(data,uin+"不能和自己比拼牛子");
                                }
                                else {
                                    if(文字(qun,"牛子比拼",uin).equals("")) {
                                        sendText(data,uin+"您还没有牛子，请『领养牛子』");
                                    }
                                    else {
                                        次数=次数-1;
                                        String text="";
                                        int length=读(qun,"牛子长度",uin);
                                        String lengths=getLength(length);
                                        int a=随机数(-5,10);
                                        int lengthes=length+a;
                                        String lengthess=getLength(lengthes);
                                        写(qun,"牛子长度",uin,lengthes);
                                        if(a<0) {
                                            text="您的牛子未比拼过对方，已"+a+"mm";
                                        }
                                        else if(a==0) {
                                            text="您的牛子与对方打成平手";
                                        }
                                        else if(a>0) {
                                            text="您的牛子胜过对方，已+"+a+"mm";
                                        }
                                        写(qun,"牛子比拼次数"+uin,年月日(),次数);
                                        int 剩余=次数+每日次数;
                                        sendText(data,"[atUin="+uin+"]\n比拼结果:\n"+text+"\n\n牛子数据:\n比拼前:"+lengths+"\n比拼后:"+lengthess+"\n称号:"+getNick(lengthes)+"\n\n您还可以进行:"+剩余+"次");
                                    }
                                }
                            }
                        }
                        else {
                            int length=读(qun,"牛子长度",uin);
                            String lengths=getLength(length);
                            sendText(data,"[atUin="+uin+"]\n已超出今日比拼次数，每日限制"+每日次数+"次\n\n牛子数据:\n您的牛子长度为"+lengths+"("+length+"mm)\n称号:"+getNick(length));
                        }
                    }
                    if(quntext.equals("领养牛子")) {
                        if(文字(qun,"牛子比拼",uin).equals("")) {
                            写(qun,"牛子比拼",uin,uin);
                            sendText(data,uin+"领养牛子成功，请发送『比拼牛子@QQ/+QQ』来增加牛子长度吧");
                        }
                        else {
                            sendText(data,"[atUin="+uin+"]\n不是吧，你有牛子了还想要？再要给你割了！");
                        }
                    }
                    if(quntext.equals("申请炉管")) {
                        if(文字(qun,"牛子比拼",uin).equals("")) {
                            sendText(data,uin+"您还没有牛子，请『领养牛子』");
                        }
                        else {
                            String time=文字(qun,"申请炉管","时间"+uin);
                            String time1=年月日();
                            if(!time1.equals(time)) {
                                int length=读(qun,"牛子长度",uin);
                                String lengths=getLength(length);
                                if(length<=10) {
                                    sendText(data,uin+"您的牛子长度为"+lengths+"，<1cm哦，牛牛还想不想要了？");
                                }
                                else {
                                    int a=随机数(1,5);
                                    int lengthes=length+a;
                                    String lengthess=getLength(lengthes);
                                    写(qun,"牛子长度",uin,lengthes);
                                    写(qun,"申请炉管","时间"+uin,time1);
                                    sendText(data,"[atUin="+uin+"]\n炉管成功\n\n牛子数据:\n炉管前:"+lengths+"\n炉管后:"+lengthess+"\n一天一次哦，注意身体更重要呢！");
                                }
                            }
                            else {
                                sendText(data,uin+"一天一次呀，您得注意身体，不允许您今日再炉管！");
                            }
                        }
                    }
                    if(quntext.equals("我要月抛")) {
                        if(文字(qun,"牛子比拼",uin).equals("")) {
                            sendText(data,uin+"您还没有牛子哎，请『领养牛子』");
                        }
                        else {
                            String time=文字(qun,"我要月抛","时间"+uin);
                            String time1=年月();
                            if(!time1.equals(time)) {
                                int length=读(qun,"牛子长度",uin);
                                String lengths=getLength(length);
                                if(length<=10) {
                                    sendText(data,uin+"您的牛子长度为"+lengths+"，<1cm，没有妹子跟你月，妹子看到你的牛牛都嫌弃");
                                }
                                else {
                                    int b=随机数(1,3);
                                    if(b==2) {
                                        int a=随机数(-10,-1);
                                        int lengthes=length+a;
                                        String lengthess=getLength(lengthes);
                                        写(qun,"牛子长度",uin,lengthes);
                                        写(qun,"我要月抛","时间"+uin,time1);
                                        sendText(data,"[atUin="+uin+"]\n月抛成功但被抓，牛子被剪短\n\n牛子数据:\n月抛前:"+lengths+"\n月抛后:"+lengthess+"\n一月一次哦，注意身体最重要！");
                                    }
                                    else {
                                        int a=随机数(1,5);
                                        int lengthes=length+a;
                                        String lengthess=getLength(lengthes);
                                        写(qun,"牛子长度",uin,lengthes);
                                        写(qun,"我要月抛","时间"+uin,time1);
                                        sendText(data,"[atUin="+uin+"]\n月抛成功\n\n牛子数据:\n月抛前:"+lengths+"\n月抛后:"+lengthess+"\n一月一次，注意身体！");
                                    }
                                }
                            }
                            else {
                                sendText(data,uin+"一月一次呀，注意身体更重要呢！已阻止您本月内月抛！");
                            }
                        }
                    }
                    if(quntext.matches("牛子比拼[0-9]+")) {
                        String at=quntext.substring(4);
                        int 次数=读(qun,"牛子比拼次数"+uin,年月日());
                        if(次数<=0&&次数>-每日次数) {
                            if(at.equals(uin)) {
                                sendText(data,uin+"不能和自己比拼牛子");
                            }
                            else {
                                if(文字(qun,"牛子比拼",uin).equals("")) {
                                    sendText(data,uin+"您还没有牛子，请『领养牛子』");
                                }
                                else {
                                    String text="";
                                    int length=读(qun,"牛子长度",uin);
                                    //0
                                    String lengths=getLength(length);
                                    //0mm
                                    int a=随机数(-5,10);
                                    //5
                                    int lengthes=length+a;
                                    //0+5
                                    写(qun,"牛子长度",uin,lengthes);
                                    //5
                                    String lengthess=getLength(lengthes);
                                    //5mm
                                    if(a<0) {
                                        text="您的牛子未比拼过对方，已"+a+"mm";
                                    }
                                    else if(a==0) {
                                        text="您的牛子与对方打成平手";
                                    }
                                    else if(a>0) {
                                        text="您的牛子胜过对方，已+"+a+"mm";
                                    }
                                    写(qun,"牛子比拼次数"+uin,年月日(),次数);
                                    int 剩余=次数+每日次数;
                                    sendText(data,"[atUin="+uin+"]\n比拼结果:\n"+text+"\n\n牛子数据:\n比拼前:"+lengths+"\n比拼后:"+lengthess+"\n称号:"+getNick(lengthes)+"\n\n您还可以进行:"+剩余+"次");
                                }
                            }
                        }
                        else {
                            int length=读(qun,"牛子长度",uin);
                            String lengths=getLength(length);
                            sendText(data,"[atUin="+uin+"]\n已超出今日比拼次数，每日限制"+每日次数+"次\n\n牛子数据:\n您的牛子长度为"+lengths+"("+length+"mm)\n称号:"+getNick(length));
                        }
                    }
                    if(quntext.startsWith("牛子攻击@")) {
                        int 次数=读(qun,"牛子攻击次数"+uin,年月日());
                        if(次数<=0&&次数>-每日攻击次数) {
                            if(data.atList.size()<1) {
                                sendText(data,uin+"请真实艾特对方");
                            }
                            else if(data.atList.size()==1) {
                                String at=data.atList.get(0);
                                if(at.equals(uin)) {
                                    sendText(data,uin+"不能攻击自己牛子");
                                }
                                else {
                                    if(文字(qun,"牛子比拼",at).equals("")) {
                                        sendText(data,uin+"对方还没有牛子，请先让对方『领养牛子』");
                                    }
                                    else {
                                        次数=次数-1;
                                        int length=读(qun,"牛子长度",at);
                                        int a=随机数(1,3);
                                        int lengthes;
                                        if(length>=a) lengthes=length-a;
                                        else if(length<a) lengthes=0;
                                        String lengthess=getLength(lengthes);
                                        写(qun,"牛子攻击次数"+uin,年月日(),次数);
                                        int 剩余=次数+每日攻击次数;
                                        写(qun,"牛子长度",at,lengthes);
                                        sendText(data,"[atUin="+uin+"]\n牛子攻击成功\n对方("+at+")的牛子-"+a+"mm\n目前对方牛子剩余:"+lengthess+"\n\n还可以攻击"+剩余+"次");
                                    }
                                }
                            }
                            else {
                                sendText(data,uin+"每次请只艾特一个人");
                            }
                        }
                        else {
                            int length=读(qun,"牛子长度",uin);
                            String lengths=getLength(length);
                            sendText(data,"[atUin="+uin+"]\n已超出今日攻击次数，每日限制"+每日攻击次数+"次\n\n牛子数据:\n您的牛子长度为"+lengths+"("+length+"mm)");
                        }
                    }
                    if(quntext.matches("牛子攻击[0-9]+")) {
                        int 次数=读(qun,"牛子攻击次数"+uin,年月日());
                        if(次数<=0&&次数>-每日攻击次数) {
                            String at=quntext.substring(4);
                            if(at.equals(uin)) {
                                sendText(data,uin+"不能攻击自己牛子");
                            }
                            else {
                                if(文字(qun,"牛子比拼",at).equals("")) {
                                    sendText(data,uin+"对方还没有牛子，请先让对方『领养牛子』");
                                }
                                else {
                                    次数=次数-1;
                                    int length=读(qun,"牛子长度",at);
                                    int a=随机数(1,3);
                                    int lengthes;
                                    if(length>=a) lengthes=length-a;
                                    else if(length<a) lengthes=0;
                                    String lengthess=getLength(lengthes);
                                    写(qun,"牛子长度",at,lengthes);
                                    写(qun,"牛子攻击次数"+uin,年月日(),次数);
                                    int 剩余=次数+每日攻击次数;
                                    sendText(data,"[atUin="+uin+"]\n牛子攻击成功\n对方("+at+")的牛子-"+a+"mm\n目前对方牛子剩余:"+lengthess+"\n\n还可以攻击"+剩余+"次");
                                }
                            }
                        }
                        else {
                            int length=读(qun,"牛子长度",uin);
                            String lengths=getLength(length);
                            sendText(data,"[atUin="+uin+"]\n已超出今日攻击次数，每日限制"+每日次数+"次\n\n牛子数据:\n您的牛子长度为"+lengths+"("+length+"mm)");
                        }
                    }
                    if(quntext.equals("牛子排行榜")) {
                        String[] lengths=列表2(qun,"牛子长度");
                        
                        Dick[] dicks=new Dick[lengths.length];
                        int i=0;
                        for(String uins:lengths) {
                            int length = 读(qun,"牛子长度",uins);
                            dicks[i] = new Dick(uins,length);
                            i++;
                            //sendText(data,uins+":"+length);
                        }
                        Arrays.sort(dicks);
                        int q=0;
                        String result="";
                        for(Dick dick:dicks) {
                            q++;
                            result+="\n"+q+"、"+dick.name+"："+getLength(dick.length)+"("+dick.length+"mm)";
                        }
                        sendText(data,"牛子比拼:"+result);
                    }
                }
            }
        }
    }
    ).start();
}
public String getLength(int length) {
    if(length<10) {
        return length+"mm";
    }
    else if(length>=10&&length<100) {
        double result = length / 10.0;
        return result+"cm";
    }
    else if(length>=100&&length<1000) {
        double result = length / 100.00;
        return result+"dm";
    }
    else if(length>=1000&&length<10000) {
        double result = length / 1000.000;
        return result+"m";
    }
    else {
        double result = length / 1000000.000;
        return result+"km";
    }
}
public String getNick(int length) {
    if(length<0) {
        return "凹牛子战神(好像是β)";
    }
    else if(length<10) {
        return "小牛子战神(小小的也很可爱)";
    }
    else if(length>=10&&length<100) {
        double result = length / 10.0;
        return "牛子战神(正常)";
    }
    else if(length>=100&&length<1000) {
        double result = length / 100.00;
        return "大牛子战神(巨无霸)";
    }
    else if(length>=1000&&length<10000) {
        double result = length / 1000.000;
        return "逆天牛子战神(地球最大)";
    }
    else {
        double result = length / 1000000.000;
        return "无敌牛子战神(捅破宇宙)";
    }
}
import java.util.Comparator;
public class Dick implements Comparable {
    public String name;
    public int length;
    public int compareTo(Object dick) {
        return Integer.compare(dick.length,this.length);
        // 按大小排序
    }
    public Dick(String name,int length) {
        this.name=name;
        this.length=length;
    }
    public String toString() {
        return "昵称:"+this.name+"\n牛子长度:"+this.length;
    }
}
public String 年月()
{
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
    Date date = new Date();
    String currentDate = df.format(date);
    return currentDate;
}
//Toast("[牛子比拼]加载成功");