package me.lengyu.qedge.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {

    public static boolean downloadSync(String urlStr, String savePath) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                File saveFile = new File(savePath);
                File parentDir = saveFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (InputStream is = connection.getInputStream();
                     FileOutputStream fos = new FileOutputStream(saveFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static String download(String urlStr, String savePath) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            // 禁止自动处理重定向，我们手动来，以便拿到重定向后的真实文件名
            connection.setInstanceFollowRedirects(false);

            int responseCode = connection.getResponseCode();

            // 1. 处理重定向 (301, 302, 303, 307, 308)
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 || responseCode == 308) {
                
                String newUrlStr = connection.getHeaderField("Location");
                if (newUrlStr != null && !newUrlStr.isEmpty()) {
                    connection.disconnect();
                    // 递归调用下载重定向后的地址
                    return download(newUrlStr, savePath);
                }
                return null;
            }

            // 2. 处理正常下载 (200)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String fileName = null;

                // 优先尝试从 Content-Disposition 获取文件名
                String disposition = connection.getHeaderField("Content-Disposition");
                if (disposition != null && disposition.contains("filename=")) {
                    int index = disposition.toLowerCase().indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 9).replace("\"", "").replace("'", "");
                    }
                }

                // 如果没拿到，根据 URL 和 Content-Type 判断
                if (fileName == null || fileName.isEmpty()) {
                    String currentUrl = connection.getURL().toString();
                    // 去掉 URL 参数 (?xxx=yyy)
                    String cleanUrl = currentUrl.split("\\?")[0];
                    // 去掉路径，只留最后一段作为文件名
                    String urlFileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
                    
                    // 解码文件名 (处理 %20 这种空格编码)
                    try {
                        urlFileName = java.net.URLDecoder.decode(urlFileName, "UTF-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // 检查后缀是否是动态脚本或无效后缀
                    String extension = "";
                    int dotIndex = urlFileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        extension = urlFileName.substring(dotIndex + 1).toLowerCase();
                    }

                    boolean isBadExtension = extension.matches("php|jsp|asp|aspx|do|action|html|htm|") || extension.isEmpty();

                    if (isBadExtension) {
                        // 如果是不常用后缀，根据 Content-Type 生成后缀
                        String contentType = connection.getContentType();
                        String newExt = getExtensionFromContentType(contentType);
                        // 使用时间戳作为文件名，避免重名
                        fileName = "download_" + System.currentTimeMillis() + newExt;
                    } else {
                        // 如果是常见后缀，直接用他的文件名
                        fileName = urlFileName;
                    }
                }

                // 生成最终保存路径
                File saveFile = new File(savePath, fileName);
                File parentDir = saveFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (InputStream is = connection.getInputStream();
                     FileOutputStream fos = new FileOutputStream(saveFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
                return saveFile.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    /**
     * 辅助方法：根据 MIME Type 获取文件后缀
     */
    private static String getExtensionFromContentType(String contentType) {
        if (contentType == null) return ".bin";
        
        // 去掉 charset 等参数，如 "audio/mpeg; charset=utf-8" -> "audio/mpeg"
        contentType = contentType.split(";")[0].trim().toLowerCase();
        
        switch (contentType) {
            case "audio/mpeg":
            case "audio/mp3":
                return ".mp3";
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "video/mp4":
                return ".mp4";
            case "application/zip":
                return ".zip";
            case "application/json":
                return ".json";
            case "text/plain":
                return ".txt";
            case "application/octet-stream":
                return ".bin"; // 通用二进制流
            // 如果有其他需要，可以继续添加
            default:
                // 尝试从 xxxx/yyy 中提取 yyy 作为后缀
                if (contentType.contains("/")) {
                    return "." + contentType.substring(contentType.lastIndexOf('/') + 1);
                }
                return ".bin";
        }
    }

    public static String get(String urlStr) {
        return get(urlStr, 10000, 30000);
    }

    public static String get(String urlStr, int connectTimeout, int readTimeout) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String post(String urlStr, String body) {
        return post(urlStr, body, "application/json");
    }

    public static String post(String urlStr, String body, String contentType) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setDoOutput(true);

            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String postWithCookie(String urlStr, String body, String cookie) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Cookie", cookie);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setDoOutput(true);

            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}