//作者自用写的
public void 解密菜单(Object Yu) {
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
                if(读("0","代管",uin)==1||读(qun,"代管",uin)==1||qq.equals(uin)) {
                    if(quntext.equals("解密菜单")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            sendText(data,"解密菜单:\nbase加密+内容\nbase解密+内容\nu加密+内容\nu解密+内容\nURL加+内容\nURL解+内容\nUnicode加密+文件路径\nUnicode解密+文件路径\n凯撒加#内容#密码\n凯撒解#内容#密码\n数组加密+内容\n数组解密+内容");
                        }
                    }
                    if(quntext.startsWith("数组解密")) {
                        String jm=byteDecrypte(quntext.substring(4));
                        sendText(data,jm);
                    }
                    if(quntext.startsWith("数组加密")) {
                        String jm=byteEncrypte(quntext.substring(4));
                        sendText(data,jm);
                    }
                    if(quntext.startsWith("URL解")) {
                        String msg=quntext.substring(4);
                        String result=URL(msg,0);
                        sendText(data,""+result);
                    }
                    if(quntext.startsWith("URL加")) {
                        String msg=quntext.substring(4);
                        String result=URL(msg,1);
                        sendText(data,""+result);
                    }
                    if(quntext.startsWith("base解密")) {
                        String jm=jm(quntext.substring(6));
                        sendText(data,jm);
                    }
                    if(quntext.startsWith("base加密")) {
                        String jm=jam(quntext.substring(6));
                        sendText(data,jm);
                    }
                    if(quntext.startsWith("u解密")) {
                        String jm=u解(quntext.substring(3));
                        sendText(data,jm);
                    }
                    if(quntext.startsWith("u加密")) {
                        String jm=u加(quntext.substring(3));
                        sendText(data,jm);
                    }
                    if(quntext.startsWith("Unicode解密")) {
                        String text=quntext.substring(9);
                        String text1=读(text);
                        String jm=u解(text1);
                        put(text,jm);
                        sendText(data,"解密成功");
                    }
                    if(quntext.startsWith("Unicode加密")) {
                        String text=quntext.substring(9);
                        String text1=读(text);
                        String jm=u加(text1);
                        put(text,jm);
                        sendText(data,"加密成功");
                    }
                    if(quntext.startsWith("凯撒加#")) {
                        String one=quntext.split("#")[1];
                        String two=quntext.split("#")[2];
                        String jm=encryptKaiser(one,two,0);
                        sendText(data,jm);
                    }
                    if(quntext.startsWith("凯撒解#")) {
                        String one=quntext.split("#")[1];
                        String two=quntext.split("#")[2];
                        String jm=encryptKaiser(one,two,1);
                        sendText(data,jm);
                    }
                }
            }
        }
    }
    ).start();
}
//云上升
public String byteDecrypte(String msg)
{
    String text = msg.replace("new String(new byte[]{", "").replace("})", "");
    String[] byteValues = text.substring(0, text.length()).split(",");
    byte[] bytes = new byte[byteValues.length];
    for(int i = 0;
    i < bytes.length;
    i++)
    {
        bytes[i] = Byte.parseByte(byteValues[i].trim());
    }
    String str = new String(bytes, "UTF-8");
    return str;
}
public String byteEncrypte(String msg)
{
    String str = msg + "";
    byte[] bytes = str.getBytes("UTF-8");
    String text = "";
    for(byte b: bytes)
    {
        text += b + ",";
    }
    text = text.substring(0, text.length() - 1);
    text = "new String(new byte[]{" + text + "})";
    return text;
}