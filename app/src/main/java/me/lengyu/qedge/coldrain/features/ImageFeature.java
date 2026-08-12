package me.lengyu.qedge.coldrain.features;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Color;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.QQCurrentEnv;
import me.lengyu.qedge.utils.qq.MsgTool;

public class ImageFeature implements ColdRainFeature {

    private static final String MY_WEB = "https://api.yuafeng.cn/API/ly/";
    private static final String MATERIAL_ZIP_URL = "https://gitee.com/ColdRainJava/update/raw/master/图片.zip";

    private static final Map<String, String> MATERIAL_MAP = new HashMap<String, String>() {{
        put("阴影头像", "阴影头像");
        put("双重阴影头像", "双重阴影头像");
        put("V10头像", "V10");
        put("红旗头像1", "红旗1");
        put("红旗头像2", "红旗2");
        put("蓝V头像", "蓝V");
        put("黄V头像", "黄V");
        put("红勾认证", "红勾");
        put("绿勾认证", "绿勾");
        put("圆圈头像", "圆圈");
        put("企业认证", "企业");
        put("半透明头像", "全透明");
        put("六边形头像", "六边形");
        put("羽化圆形", "");
    }};

    private static final String[] FUNCTION_NAMES = {
        "阴影头像", "红旗头像1", "红旗头像2", "V10头像",
        "双重阴影头像", "蓝V头像", "黄V头像", "红勾认证",
        "绿勾认证", "企业认证", "圆圈头像", "半透明头像",
        "羽化圆形", "六边形头像"
    };

    private static String getMaterialDir() {
        String baseDir = QQCurrentEnv.getCurrentDir();
        if (baseDir.endsWith("/")) {
            baseDir = baseDir.substring(0, baseDir.length() - 1);
        }
        String dir = baseDir + "/image_material";
        File f = new File(dir);
        if (!f.exists()) f.mkdirs();
        return dir;
    }

    private static boolean isMaterialReady() {
        String dir = getMaterialDir();
        return new File(dir, "阴影头像").exists();
    }

    private static void downloadMaterial() throws Exception {
        String dir = getMaterialDir();
        String zipPath = dir + "/images.zip";
        boolean ok = HttpUtils.downloadSync(MATERIAL_ZIP_URL, zipPath);
        if (!ok) throw new Exception("素材下载失败");
        unZip(zipPath, dir);
        new File(zipPath).delete();
    }

    private static void unZip(String zipPath, String outDir) throws Exception {
        ZipFile zipFile = new ZipFile(zipPath);
        Enumeration<?> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            int idx = name.lastIndexOf("/");
            if (idx >= 0) name = name.substring(idx + 1);
            if (name.isEmpty()) continue;
            File outFile = new File(outDir, name);
            if (outFile.exists()) continue;
            InputStream is = zipFile.getInputStream(entry);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            is.close();
            fos.close();
        }
        zipFile.close();
    }

    private static void sendImg(MsgData msgData, String path) {
        MsgTool.sendPic(msgData.peerUin, path, msgData.type);
    }

    private static void sendMsg(MsgData msgData, String text) {
        ColdRainCore.getInstance().reply(msgData, text);
    }

    private static Bitmap getroundbmp(Bitmap bitmap, float roundPx) {
        Bitmap bmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return bmp;
    }

    private static void pinpic(String path1, String path2, float sw, float sh, float x, float y, float yd, String path) {
        Bitmap bm1 = BitmapFactory.decodeFile(path1);
        Bitmap bm2 = BitmapFactory.decodeFile(path2);
        if (bm1 == null || bm2 == null) return;

        if (!bm1.isMutable()) {
            bm1 = bm1.copy(Bitmap.Config.ARGB_8888, true);
        }

        Matrix ma = new Matrix();
        float a = sw * ((float) bm1.getWidth() / (float) bm2.getWidth());
        float b = sh * ((float) bm1.getHeight() / (float) bm2.getHeight());
        if (sw > 998 || sh > 998) {
            float i = Math.min(a, b);
            ma.postScale(i, i);
        } else {
            ma.postScale(a, b);
        }
        Bitmap zmp = Bitmap.createBitmap(bm2, 0, 0, bm2.getWidth(), bm2.getHeight(), ma, true);
        float x1 = x * bm1.getWidth() - 0.5f * zmp.getWidth();
        float y1 = y * bm1.getHeight() - 0.5f * zmp.getHeight();
        Bitmap smp = getroundbmp(zmp, yd);
        Canvas cas = new Canvas(bm1);
        cas.drawBitmap(smp, x1, y1, null);

        try {
            FileOutputStream fs = new FileOutputStream(path);
            bm1.compress(Bitmap.CompressFormat.PNG, 100, fs);
            fs.flush();
            fs.close();
        } catch (Exception e) {
            LogUtils.e(e);
        }

        zmp.recycle();
        smp.recycle();
        bm1.recycle();
        bm2.recycle();
    }

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        if (text.equals("图片功能")) return true;
        if (text.startsWith("抠图") || text.startsWith("扣图")) return true;
        if (text.startsWith("画质修复") || text.startsWith("修复画质")) return true;
        if (text.startsWith("抠头像") || text.startsWith("扣头像")) return true;
        for (String name : FUNCTION_NAMES) {
            if (text.startsWith(name)) return true;
        }
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        final String text = msgData.msg.trim();
        final MsgData fMsg = msgData;

        if (text.equals("图片功能")) {
            String menu = "图片功能:\n阴影头像 红旗头像1\nV10头像 红旗头像2\n双重阴影头像\n蓝/黄V头像\n绿/红勾认证\n企业认证 圆圈头像\n半透明头像 羽化圆形\n六边形头像\n抠图/扣图+图片链接/路径\n画质修复+图片链接/路径\n抠/扣头像+QQ/@QQ(支持多人)\n用法:指令+@人/QQ号/图片链接";
            sendMsg(msgData, menu);
            return;
        }

        ModuleScope.launchIOJava("ImageFeature", () -> {
                try {
                    handleCommand(fMsg, text);
                } catch (Throwable e) {
                    sendMsg(fMsg, "出错: " + e.getMessage());
                }
        });
    }

    private void handleCommand(MsgData msgData, String text) throws Exception {
        String function = null;
        for (String name : FUNCTION_NAMES) {
            if (text.startsWith(name)) {
                function = name;
                break;
            }
        }

        if (function != null) {
            String rest = text.substring(function.length()).trim();

            String imgUrl = null;
            if(msgData.msgType == 1){
                if (msgData.atList != null && !msgData.atList.isEmpty()) {
                    imgUrl = "https://q2.qlogo.cn/headimg_dl?dst_uin=" + msgData.atList.get(0) + "&spec=640";
                } else if (rest.startsWith("http://") || rest.startsWith("https://")) {
                    imgUrl = rest;
                } else if (rest.matches("\\d{5,11}")) {
                    imgUrl = "https://q2.qlogo.cn/headimg_dl?dst_uin=" + rest + "&spec=640";
                }
            }else if(msgData.msgType == 2){
                if(msgData.picList != null && !msgData.picList.isEmpty()){
                    imgUrl = msgData.picList.get(0);
                }
            }else if (msgData.msgType == 9){
                MsgData replyMsgData = new MsgData(msgData.data.records.get(0));
                if(replyMsgData.picList != null && !replyMsgData.picList.isEmpty()){
                    imgUrl = replyMsgData.picList.get(0);
                }
            }

            if (imgUrl == null || imgUrl.isEmpty()) {
                sendMsg(msgData, "请@某人、附带QQ号、图片、图片链接或者回复一条图片消息\n例如：" + function + "@QQ");
                return;
            }

            if (!isMaterialReady() && !function.equals("羽化圆形")) {
                sendMsg(msgData, "正在下载素材，请稍候...");
                downloadMaterial();
            }

            String dir = getMaterialDir();
            String matFileName = MATERIAL_MAP.get(function);
            if (matFileName == null) {
                sendMsg(msgData, "不支持的功能: " + function);
                return;
            }

            String matPath = "";
            if (!function.equals("羽化圆形")) {
                matPath = new File(dir, matFileName).getAbsolutePath();
            }

            String baseDir = QQCurrentEnv.getCurrentDir();
            if (baseDir.endsWith("/")) {
                baseDir = baseDir.substring(0, baseDir.length() - 1);
            }
            String cacheDir = baseDir + "/cache/images";
            File cf = new File(cacheDir);
            if (!cf.exists()) cf.mkdirs();

            String tempSrc = cacheDir + "/tmp_src_" + System.currentTimeMillis() + ".png";
            boolean ok = HttpUtils.downloadSync(imgUrl, tempSrc);
            if (!ok || !new File(tempSrc).exists()) {
                sendMsg(msgData, "图片下载失败");
                return;
            }

            String outPath = cacheDir + "/imgfunc_" + System.currentTimeMillis() + ".png";

            if (function.equals("羽化圆形")) {
                circleCutWithShadowAndBlur(tempSrc, outPath);
                if (new File(outPath).exists()) {
                    sendImg(msgData, outPath);
                } else {
                    sendMsg(msgData, "处理失败");
                }
            } else {
                makeImage(function, tempSrc, matPath, outPath);
                if (new File(outPath).exists()) {
                    sendImg(msgData, outPath);
                } else {
                    sendMsg(msgData, "处理失败");
                }
            }

            new File(tempSrc).delete();
            new File(outPath).delete();
        }

        if (text.startsWith("抠图") || text.startsWith("扣图")) {
            String rest = text.substring(2).trim();
            if (rest.startsWith("http://") || rest.startsWith("https://")) {
                String url = MY_WEB + "koutu.php?url=" + java.net.URLEncoder.encode(rest, "UTF-8");
                String resp = HttpUtils.get(url);
                if (resp == null) {
                    sendMsg(msgData, "请求失败");
                    return;
                }
                org.json.JSONObject json = new org.json.JSONObject(resp);
                if (json.getString("msg").equals("success")) {
                    String img = json.getJSONObject("data").getString("image");
                    sendMsg(msgData, "ok,抠图成功!\n抠图图片发送中ing...\n链接:" + img);
                    sendImgFromUrl(msgData, img);
                } else {
                    sendMsg(msgData, json.getString("msg"));
                }
            } else {
                sendMsg(msgData, "请附带图片链接\n例如：抠图https://xxx.jpg");
            }
            return;
        }

        if (text.startsWith("画质修复") || text.startsWith("修复画质")) {
            String rest = text.substring(4).trim();
            if (rest.startsWith("http://") || rest.startsWith("https://")) {
                String url = MY_WEB + "clear_image.php?url=" + java.net.URLEncoder.encode(rest, "UTF-8");
                String resp = HttpUtils.get(url);
                if (resp == null) {
                    sendMsg(msgData, "请求失败");
                    return;
                }
                org.json.JSONObject json = new org.json.JSONObject(resp);
                if (json.getString("msg").equals("success")) {
                    String img = json.getJSONObject("data").getString("image");
                    sendMsg(msgData, "ok,画质修复成功!\n图片发送中ing...\n链接:" + img);
                    sendImgFromUrl(msgData, img);
                } else {
                    sendMsg(msgData, json.getString("msg"));
                }
            } else {
                sendMsg(msgData, "请附带图片链接\n例如：画质修复https://xxx.jpg");
            }
            return;
        }

        if (text.startsWith("抠头像") || text.startsWith("扣头像")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                int max = Math.min(msgData.atList.size(), 10);
                for (int i = 0; i < max; i++) {
                    String at = msgData.atList.get(i);
                    String avatarUrl = "https://q2.qlogo.cn/headimg_dl?dst_uin=" + at + "&spec=640";
                    String url = MY_WEB + "koutu.php?url=" + java.net.URLEncoder.encode(avatarUrl, "UTF-8");
                    String resp = HttpUtils.get(url);
                    if (resp == null) continue;
                    org.json.JSONObject json = new org.json.JSONObject(resp);
                    if (json.getString("msg").equals("success")) {
                        String img = json.getJSONObject("data").getString("image");
                        sendMsg(msgData, "ok,[atUin=" + at + "]抠图成功!\n抠图图片发送中ing...\n链接:" + img);
                        sendImgFromUrl(msgData, img);
                    } else {
                        sendMsg(msgData, json.getString("msg"));
                    }
                    Thread.sleep(500);
                }
            } else {
                String rest = text.substring(3).trim();
                if (rest.matches("\\d{5,11}")) {
                    String avatarUrl = "https://q2.qlogo.cn/headimg_dl?dst_uin=" + rest + "&spec=640";
                    String url = MY_WEB + "koutu.php?url=" + java.net.URLEncoder.encode(avatarUrl, "UTF-8");
                    String resp = HttpUtils.get(url);
                    if (resp == null) {
                        sendMsg(msgData, "请求失败");
                        return;
                    }
                    org.json.JSONObject json = new org.json.JSONObject(resp);
                    if (json.getString("msg").equals("success")) {
                        String img = json.getJSONObject("data").getString("image");
                        sendMsg(msgData, "ok,抠图成功!\n抠图图片发送中ing...\n链接:" + img);
                        sendImgFromUrl(msgData, img);
                    } else {
                        sendMsg(msgData, json.getString("msg"));
                    }
                } else {
                    sendMsg(msgData, "请@某人或附带QQ号\n例如：抠头像123456");
                }
            }
            return;
        }
    }

    private void makeImage(String type, String imgPath, String matPath, String outPath) {
        switch (type) {
            case "阴影头像":
                pinpic(matPath, imgPath, 0.61f, 0.61f, 0.542f, 0.448f, 210f, outPath);
                break;
            case "V10头像":
                pinpic(imgPath, matPath, 999f, 0.65f, 0.6f, 0.6f, 90f, outPath);
                break;
            case "双重阴影头像":
                pinpic(matPath, imgPath, 0.52f, 0.52f, 0.540f, 0.448f, 75f, outPath);
                break;
            case "红旗头像1":
                pinpic(imgPath, matPath, 999f, 1f, 0.5f, 0.5f, 0f, outPath);
                break;
            case "红旗头像2":
                pinpic(imgPath, matPath, 999f, 1f, 0.5f, 0.5f, 0f, outPath);
                break;
            case "蓝V头像":
                pinpic(imgPath, matPath, 999f, 0.65f, 0.6f, 0.6f, 90f, outPath);
                break;
            case "黄V头像":
                pinpic(imgPath, matPath, 999f, 0.65f, 0.6f, 0.6f, 90f, outPath);
                break;
            case "红勾认证":
                pinpic(imgPath, matPath, 999f, 0.65f, 0.6f, 0.6f, 90f, outPath);
                break;
            case "绿勾认证":
                pinpic(imgPath, matPath, 999f, 0.65f, 0.6f, 0.6f, 90f, outPath);
                break;
            case "圆圈头像":
                pinpic(matPath, imgPath, 999f, 0.85f, 0.5f, 0.5f, 360f, outPath);
                break;
            case "企业认证":
                pinpic(imgPath, matPath, 999f, 0.65f, 0.6f, 0.6f, 90f, outPath);
                break;
            case "半透明头像":
                pinpic(matPath, imgPath, 999f, 0.65f, 0.5f, 0.5f, 0f, outPath);
                break;
            case "六边形头像":
                pinpic(matPath, imgPath, 0.355f, 0.355f, 0.4989f, 0.499f, 360f, outPath);
                break;
        }
    }

    private void sendImgFromUrl(MsgData msgData, String url) {
        try {
            if (url == null || url.isEmpty()) return;
            MsgTool.sendPic(msgData.peerUin, url, msgData.type);
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }

    private void circleCutWithShadowAndBlur(String infile, String outfile) {
        try {
            Bitmap input = BitmapFactory.decodeFile(infile);
            if (input == null) return;

            int w = input.getWidth();
            int h = input.getHeight();
            Bitmap squareBmp;
            if (w == h) {
                squareBmp = input;
            } else {
                int size = Math.min(w, h);
                int x = (w - size) / 2;
                int y = (h - size) / 2;
                squareBmp = Bitmap.createBitmap(input, x, y, size, size);
            }

            int size = squareBmp.getWidth();
            int padding = 40;
            int canvasSize = size + padding * 2;

            Bitmap result = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);

            float cx = canvasSize / 2.0f;
            float cy = canvasSize / 2.0f;
            float radius = size / 2.0f;

            Bitmap maskBitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(maskBitmap);

            RadialGradient maskGradient = new RadialGradient(
                cx, cy, radius,
                new int[]{
                    Color.argb(255, 0, 0, 0),
                    Color.argb(255, 0, 0, 0),
                    Color.argb(128, 0, 0, 0),
                    Color.argb(0, 0, 0, 0)
                },
                new float[]{0.0f, 0.7f, 0.9f, 1.0f},
                Shader.TileMode.CLAMP
            );

            Paint maskPaint = new Paint();
            maskPaint.setShader(maskGradient);
            maskCanvas.drawRect(0, 0, canvasSize, canvasSize, maskPaint);

            canvas.drawBitmap(squareBmp, cx - radius, cy - radius, null);

            Paint alphaPaint = new Paint();
            alphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawBitmap(maskBitmap, 0, 0, alphaPaint);

            maskBitmap.recycle();

            if (squareBmp != input) {
                squareBmp.recycle();
            }

            FileOutputStream fos = new FileOutputStream(outfile);
            result.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            result.recycle();
            input.recycle();
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }
}
