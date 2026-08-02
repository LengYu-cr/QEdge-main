public void put(String Path, String WriteData)
{
    try
    {
        FileMemCache.put(Path, WriteData);
        File file = new File(Path);
        FileOutputStream fos = new FileOutputStream(file);
        if(!file.exists())
        {
            file.createNewFile();
        }
        byte[] bytesArray = WriteData.getBytes();
        fos.write(bytesArray);
        fos.flush();
    }
    catch(IOException ioe)
    {
        Toast("" + ioe);
    }
}
public void sc(String Path)
{
    File file = null;
    try
    {
        file = new File(Path);
        if(file.exists())
        {
            file.delete();
            FileMemCache.remove(Path);
        }
    }
    catch(Exception e)
    {
        Toast(e + "");
    }
}
HashMap FileMemCache = new HashMap();
public 写(QQUin, SetName, ItemName, data)
{
    try
    {
        新建(ColdRainPath + "data/" + QQUin);
        String UserData = 读(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt");
        JSONObject UserDataJson = null;
        if(UserData.equals(""))
        {
            UserDataJson = new JSONObject("{}");
        }
        else
        {
            UserDataJson = new JSONObject(UserData);
        }
        UserDataJson.put(ItemName, data);
        写(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt", UserDataJson.toString());
        return;
    }
    catch(Exception e)
    {
        return;
    }
}
public 清除(QQUin, SetName, ItemName)
{
    try
    {
        新建(ColdRainPath + "data/" + QQUin);
        String UserData = 读(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt");
        JSONObject UserDataJson = null;
        if(UserData.equals(""))
        {
            return;
        }
        else
        {
            UserDataJson = new JSONObject(UserData);
        }
        UserDataJson.remove(ItemName);
        写(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt", UserDataJson.toString());
        return;
    }
    catch(Exception e)
    {
        return;
    }
}
public String[] 列表(QQUin, SetName)
{
    try
    {
        String UserData = 读(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt");
        JSONObject UserDataJson = null;
        if(UserData.equals(""))
        {
            return new String[0];
        }
        else
        {
            UserDataJson = new JSONObject(UserData);
        }
        ArrayList MY_list = new ArrayList();
        for(String it : UserDataJson.keys()) {
            if(读(QQUin,SetName,it)==1) {
                MY_list.add(it);
            }
        }
        String[] fintext = MY_list.toArray(new String[0]);
        return fintext;
    }
    catch(Exception e)
    {
        Toast("列表()出错");
        return new String[0];
    }
}
public String 文字(QQUin, SetName, ItemName)
{
    try
    {
        String UserData = 读(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt");
        JSONObject UserDataJson = null;
        if(UserData.equals(""))
        {
            return "";
        }
        else
        {
            UserDataJson = new JSONObject(UserData);
        }
        if(!UserDataJson.has(ItemName)) return "";
        return UserDataJson.getString(ItemName);
    }
    catch(Exception e)
    {
        return "";
    }
}
public 写(QQUin, SetName, ItemName, long data)
{
    try
    {
        新建(ColdRainPath + "data/" + QQUin);
        String UserData = 读(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt");
        JSONObject UserDataJson = null;
        if(UserData.equals(""))
        {
            UserDataJson = new JSONObject("{}");
        }
        else
        {
            UserDataJson = new JSONObject(UserData);
        }
        UserDataJson.put(ItemName, String.valueOf(data));
        写(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt", UserDataJson.toString());
        return;
    }
    catch(Exception e)
    {
        Toast(e + "");
        return;
    }
}
public long 读(QQUin, SetName, ItemName)
{
    try
    {
        String UserData = 读(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt");
        JSONObject UserDataJson = null;
        if(UserData.equals(""))
        {
            return 0;
        }
        else
        {
            UserDataJson = new JSONObject(UserData);
        }
        if(!UserDataJson.has(ItemName)) return 0;
        return Long.parseLong(UserDataJson.getString(ItemName));
    }
    catch(Exception e)
    {
        return 0;
    }
}
public 删除(QQUin, SetName)
{
    删除(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt");
}
public 删除(QQUin)
{
    删除(ColdRainPath + "data/" + QQUin + "/");
}
public 读(String FilePath)
{
    try
    {
        if(FileMemCache.containsKey(FilePath))
        {
            return FileMemCache.get(FilePath);
        }
        File file = new File(FilePath);
        if(!file.exists())
        {
            file.createNewFile();
        }
        InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
        BufferedReader bf = new BufferedReader(inputReader);
        String Text = "";
        while((str = bf.readLine()) != null)
        {
            Text = Text + "\n" + str;
        }
        if(Text.isEmpty())
        {
            return "";
        }
        FileMemCache.put(FilePath, Text.substring(1));
        return Text.substring(1);
    }
    catch(IOException ioe)
    {
        return "";
    }
}
public 写(String Path, WriteData)
{
    try
    {
        FileMemCache.put(Path, WriteData);
        File file = new File(Path);
        FileOutputStream fos = new FileOutputStream(file);
        if(!file.exists())
        {
            file.createNewFile();
        }
        byte[] bytesArray = WriteData.getBytes();
        fos.write(bytesArray);
        fos.flush();
    }
    catch(IOException ioe)
    {
    }
}
public 新建(String Path)
{
    File dir = null;
    try
    {
        dir = new File(Path);
        if(!dir.exists())
        {
            dir.mkdirs();
        }
    }
    catch(Exception e)
    {
        Toast("创建文件夹时发生错误,相关信息:\n" + e);
    }
}
public 删除(String Path)
{
    File file = null;
    try
    {
        file = new File(Path);
        if(file.exists())
        {
            file.delete();
            FileMemCache.remove(Path);
        }
    }
    catch(Exception e)
    {
        Toast("删除文件时发生错误,相关信息:\n" + e);
    }
}
public String[] 全局列表(SetName)
{
    try
    {
        String UserData = 读(ColdRainPath + "data/" + SetName + ".txt");
        JSONObject UserDataJson = null;
        if(UserData.equals(""))
        {
            return new String[0];
        }
        else
        {
            UserDataJson = new JSONObject(UserData);
        }
        ArrayList MY_list = new ArrayList();
        for(String it : UserDataJson.keys()) {
            MY_list.add(it);
        }
        String[] fintext = MY_list.toArray(new String[0]);
        return fintext;
    }
    catch(Exception e)
    {
        return new String[0];
    }
}
public String[] 列表2(QQUin, SetName)
{
    try
    {
        String UserData = 读(ColdRainPath + "data/" + QQUin + "/" + SetName + ".txt");
        JSONObject UserDataJson = null;
        if(UserData.equals(""))
        {
            return new String[0];
        }
        else
        {
            UserDataJson = new JSONObject(UserData);
        }
        ArrayList MY_list = new ArrayList();
        for(String it : UserDataJson.keys()) {
            MY_list.add(it);
        }
        String[] fintext = MY_list.toArray(new String[0]);
        return fintext;
    }
    catch(Exception e)
    {
        return new String[0];
    }
}
public String Unzip(String zipFilePath, String destDir) throws Exception {
    File dir = new File(destDir);
    if(!dir.exists()) {
        dir.mkdirs();
    }
    FileInputStream fis = new FileInputStream(zipFilePath);
    ZipInputStream zipIn = new ZipInputStream(fis);
    ZipEntry entry = zipIn.getNextEntry();
    while(entry != null) {
        String filePath = destDir + File.separator + entry.getName();
        if(!entry.isDirectory()) {
            createParentDirectory(filePath);
            FileOutputStream fos = new FileOutputStream(filePath);
            byte[] buffer = new byte[1024];
            int length;
            while((length = zipIn.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.close();
        }
        else {
            File dirFile = new File(filePath);
            dirFile.mkdirs();
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
    }
    zipIn.close();
    fis.close();
    return "true";
}
public void createParentDirectory(String filePath) {
    File file = new File(filePath);
    File parentDir = file.getParentFile();
    if(!parentDir.exists()) {
        parentDir.mkdirs();
    }
}
public boolean 删除文件(String filePath) {
    File file = new File(filePath);
    if (file.exists()) {
        return file.delete();
    }
    return false;
}
public void zip(String sourceDirectory, String zipFilePath) throws IOException {
    File sourceDir = new File(sourceDirectory);
    FileOutputStream fos = new FileOutputStream(zipFilePath);
    ZipOutputStream zipOut = new ZipOutputStream(fos);
    zipFile(sourceDir, sourceDir.getName(), zipOut);
    zipOut.close();
    fos.close();
}
public void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
    if (fileToZip.isHidden()) {
        return;
    }
    if (fileToZip.isDirectory()) {
        if (fileName.endsWith("/")) {
            zipOut.putNextEntry(new ZipEntry(fileName));
            zipOut.closeEntry();
        }
        else {
            zipOut.putNextEntry(new ZipEntry(fileName + "/"));
            zipOut.closeEntry();
        }
        File[] children = fileToZip.listFiles();
        for (File childFile : children) {
            zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
        }
        return;
    }
    FileInputStream fis = new FileInputStream(fileToZip);
    ZipEntry zipEntry = new ZipEntry(fileName);
    zipOut.putNextEntry(zipEntry);
    byte[] bytes = new byte[1024];
    int length;
    while ((length = fis.read(bytes)) >= 0) {
        zipOut.write(bytes, 0, length);
    }
    fis.close();
}
public String getZipName(String zipFilePath) {
    try {
        InputStream fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry = zis.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            int i=name.indexOf("/");
            // 检查是否是目录
            return name.substring(0,i);
        }
    }
    catch (Exception e) {
        return e+"";
    }
}
public static String getFormattedSize(File folder) {
    if (folder == null || !folder.exists()) {
        return "文件夹不存在或为空";
    }
    long sizeInBytes=getFolderSize(folder);
    double sizeInKB=sizeInBytes / 1024.0;
    // 文件夹大小（KB）
    DecimalFormat decimalFormat=new DecimalFormat("#.###");
    if (sizeInKB < 1024) {
        return decimalFormat.format(sizeInKB) + "KB";
    }
    else if (sizeInKB < 1024 * 1024) {
        double sizeInMB=sizeInKB / 1024.0;
        // 文件夹大小（MB）
        return decimalFormat.format(sizeInMB) + "MB";
    }
    else {
        double sizeInGB=sizeInKB / (1024.0 * 1024.0);
        // 文件夹大小（GB）
        return decimalFormat.format(sizeInGB) + "GB";
    }
}
public static String getFormattedSize(long sizeInBytes) {
    double sizeInKB=sizeInBytes / 1024.0;
    // 文件夹大小（KB）
    DecimalFormat decimalFormat=new DecimalFormat("#.###");
    if (sizeInKB < 1024) {
        return decimalFormat.format(sizeInKB) + "KB";
    }
    else if (sizeInKB < 1024 * 1024) {
        double sizeInMB=sizeInKB / 1024.0;
        // 文件夹大小（MB）
        return decimalFormat.format(sizeInMB) + "MB";
    }
    else {
        double sizeInGB=sizeInKB / (1024.0 * 1024.0);
        // 文件夹大小（GB）
        return decimalFormat.format(sizeInGB) + "GB";
    }
}
public static long getFolderSize(File folder) {
    long size=0;
    File[] files=folder.listFiles();
    if (files != null) {
        for (File file : files) {
            if (file.isFile()) {
                size += file.length();
            }
            else if (file.isDirectory()) {
                size += getFolderSize(file);
            }
        }
    }
    return size;
}
public static long getFileSize(File file) {
    long size= file.length();
    return size;
}
public boolean cdxz(String qun,String uin) {
    if(uin.equals(myUin)||读("0","代管",uin)==1||读(qun,"代管",uin)==1) {
        return true;
    }
    else {
        if(读(qun,"菜单限制","开关")==0) {
            return true;
        }
        else {
            return false;
        }
    }
}
public void deleteFolder(String Path) {
    File folder = new File(Path);
    if (folder.isDirectory()) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolders(file);
                }
                else {
                    file.delete();
                }
            }
        }
    }
    folder.delete();
}
public void deleteFolders(File folder) {
    if (folder.isDirectory()) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolders(file);
                }
                else {
                    file.delete();
                }
            }
        }
    }
    folder.delete();
}
public void writeLog(String msg) {
    try {
        File logDir = new File(RootPath + "logs/");
        if (!logDir.exists()) {
            logDir.mkdirs();  // 创建目录
        }
        
        FileWriter fw = new FileWriter(RootPath + "logs/error.txt", true);
        fw.write(msg + "\n");
        fw.close();
    } catch (Exception e) {
    }
}