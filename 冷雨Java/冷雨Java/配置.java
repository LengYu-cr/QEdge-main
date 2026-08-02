//更新完记得重新加载本脚本，否则无效！
String Colour="随机";
//图片模式的字体颜色可填：红色、黑色、蓝色、蓝绿、白灰、灰色、绿色、深灰、洋红、透明、白色、黄色、随机    仅字体为其他字体时可用
String Backgroundcolor="#FFFFFFFF";
//图片模式的背景色，采用RGB十六进制色码
String WindowColour="#FF6EC7";
//弹窗字体颜色，采用RGB十六进制色码
String APP="自适应";
//自适应是适应点歌对应APP，如果你没安装对应APP就发不出去；填其他内容就发送QQ音乐转发的卡片
int textFaceType=1;
//字体，1为冷雨Java自带字体，其他为系统默认字体
String myWeb="https://api.yuafeng.cn/API/ly/";
//咳咳，我的域名，不懂别乱改，会出现未知错误
String myWang="https://api.yuafeng.cn/ly/";
// 我的个人主页 https://api.yuafeng.cn/ly/
String myApi="https://api.yuafeng.cn/";
// 枫林api https://api.yuafeng.cn/
String myDialog="https://v.yuafeng.cn//";
//我的主页 https://v.yuafeng.cn/
public String Beta(String hex) {
    if(hex == null || hex.equals("")){
        return "";
    }
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String hexChar = hex.substring(i, Math.min(i + 2, hex.length()));
            int charCode = Integer.parseInt(hexChar, 16);
            strBuilder.append((char) charCode);
        }
        return strBuilder.toString();
}