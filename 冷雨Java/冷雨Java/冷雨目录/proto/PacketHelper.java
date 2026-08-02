import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPOutputStream;
import android.os.Bundle;
import com.tencent.qphone.base.remote.ToServiceMsg;
import com.tencent.qphone.base.remote.FromServiceMsg;
import mqq.app.NewIntent;
import mqq.app.api.impl.SSOEasyServlet;
import mqq.observer.BusinessObserver;
import com.tencent.common.app.BaseApplicationImpl;
import android.content.Context;
import org.json.JSONObject;
import java.io.*;

public static int rand(int min, int max)
{
    Random random = new Random();
    return random.nextInt((max - min) + 1) + min;
}

loadJava(RootPath + "proto/IReceiver.java");
loadJava(RootPath + "proto/ProtoData.java");
public class PacketHelper {

    public static byte[] compressGzip(byte[] data) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(bos);
            gos.write(data);
            gos.close();
            return bos.toByteArray();
        } catch (Exception e) {
            return data;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
    
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] packet(byte[] data) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(data.length + 4);
            dos.write(data);
            dos.close();
            return bos.toByteArray();
        } catch (Exception e) {
            return data;
        }
    }

    public static void sendRequest(String serviceCmd, byte[] rawData, Object receiver) {
        try {
            byte[] reqBytes = packet(rawData);
            
            NewIntent intent = new NewIntent(context, SSOEasyServlet.class);
            ToServiceMsg toServiceMsg = new ToServiceMsg("mobileqq.service", myUin, serviceCmd);
            toServiceMsg.wupBuffer = reqBytes;
            
            intent.setObserver(new BusinessObserver() {
                
                public void onReceive(int type, boolean isSuccess, Bundle bundle) {
                    if (isSuccess && bundle != null) {
                        FromServiceMsg fromMsg = (FromServiceMsg) bundle.getParcelable("FromServiceMsg");
                        if (fromMsg != null) {
                            receiver.onReceive(fromMsg.wupBuffer);
                        } else {
                            receiver.onReceive(null);
                        }
                    } else {
                        receiver.onReceive(null);
                    }
                }
            });
            
            intent.putExtra("ToServiceMsg", toServiceMsg);
            Object app = BaseApplicationImpl.getApplication().getRuntime();
            app.startServlet(intent);
        } catch (Exception e) {
            receiver.onReceive(null);
        }
    }
}

public void sendPacket(String serviceCmd, JSONObject pbData, Object listener) {
    try {
        ProtoData protoData = new ProtoData();
        protoData.fromJSON(pbData);
        byte[] sendData = protoData.toBytes();
        
        PacketHelper.sendRequest(serviceCmd, sendData, new Object() {
            public void onReceive(byte[] data) {
                if (listener != null && listener.onResponse()) {
                    if (data != null && data.length > 4) {
                        byte[] respData = new byte[data.length - 4];
                        System.arraycopy(data, 4, respData, 0, data.length - 4);
                        
                        try {
                            ProtoData response = new ProtoData();
                            response.fromBytes(respData);
                            JSONObject result = response.toJSON();
                            listener.onSuccess(serviceCmd, result);
                        } catch (Exception e) {
                            listener.onFailure(serviceCmd, e.getMessage());
                        }
                    } else {
                        listener.onFailure(serviceCmd, "响应数据为空");
                    }
                }
            }
        });
    } catch (Exception e) {
        if (listener != null) {
            listener.onFailure(serviceCmd, e.getMessage());
        }
    }
}

public void changeMyName(String title,Object listener) {
    try {
        JSONObject json = new JSONObject();
        json.put("1", 4394);
        json.put("2", 2);
        json.put("12", 0);
        
        JSONObject json2 = new JSONObject();
        json2.put("1", Long.parseLong(myUin));
        
        JSONObject json3 = new JSONObject();
        json3.put("1", 20002);
        json3.put("2", title);
        json2.put("2", json3);
        json.put("4", json2);
        
        sendPacket("OidbSvcTrpcTcp.0x112a_2", json, listener);
        
    } catch (Exception e) {
        Toast("昵称修改失败(报错)");
        sendMsg(myUin, "ProtoData报错: " + e.getMessage(), 1);
    }
}
public void makeMusicCard(String title, String singer, String link, String img, String audio, String qun, int mtype, long appid, String package_name, String token) {
    try {
        if (audio == null || audio.isEmpty()) {
            audio = "https://cdn.yuafeng.cn/ly/playerror.mp3";
        }
        
        JSONObject json = new JSONObject();
        json.put("1", 2935);
        json.put("2", 9);
        json.put("6", "android " + versionName);
        
        JSONObject obj4 = new JSONObject();
        obj4.put("1", appid);
        obj4.put("2", 1);
        obj4.put("3", 4);
        obj4.put("20", 0);
        obj4.put("21", 0);
        obj4.put("11", Long.parseLong(qun));
        
        if (mtype == 2) {
            obj4.put("10", 3 - mtype);
        } else if (mtype == 1) {
            obj4.put("10", 1 - mtype);
        } else {
            obj4.put("10", mtype);
        }
        
        JSONObject obj5 = new JSONObject();
        obj5.put("1", 1);
        obj5.put("2", "0.0.0");
        obj5.put("3", package_name);
        obj5.put("4", token);
        obj4.put("5", obj5);
        
        JSONObject obj7 = new JSONObject();
        obj7.put("15", rand(100, 99999));
        obj4.put("7", obj7);
        
        JSONObject obj12 = new JSONObject();
        obj12.put("16", audio);
        obj12.put("10", title);
        obj12.put("11", singer);
        obj12.put("13", link);
        obj12.put("14", img);
        obj4.put("12", obj12);
        
        obj4.put("22", new JSONObject());
        obj4.put("23", new JSONObject());
        json.put("4", obj4);
        
        sendPacket("OidbSvc.0xb77_9", json, null);
        
    } catch (Exception e) {
        Toast("音卡制作失败: " + e.getMessage());
    }
}

public void makeLocationArk(String qun, int mtype, String location, String desc) {
    try {
        JSONObject json = new JSONObject();
        json.put("1", Long.parseLong(qun));
        json.put("2", 1);
        json.put("3", location);
        json.put("4", desc);
        json.put("5", "39.908465");
        json.put("6", "116.39758");
        
        sendPacket("trpc.qq_lbs.qq_lbs_ark.LocationArk.SsoSendMessage", json, null);
        
    } catch (Exception e) {
        Toast("位卡制作失败: " + e.getMessage());
    }
}

public void setMemberUniqueTitle(String qun, String uin, String title) {
    try {
        JSONObject json = new JSONObject();
        json.put("1", Long.parseLong(qun));
        
        JSONObject json1 = new JSONObject();
        json1.put("1", Long.parseLong(uin));
        json1.put("5", title);
        json1.put("6", rand(9999, 99999));
        json1.put("7", title);
        json.put("3", json1);
        
        JSONObject json2 = new JSONObject();
        json2.put("1", 2300);
        json2.put("2", 2);
        json2.put("4", json);
        
        sendPacket("OidbSvc.0x8fc_2", json2, null);
        
    } catch (Exception e) {
        Toast("头衔设置失败: " + e.getMessage());
    }
}

public void setGroupLocation(String qun, String location) {
    try {
        JSONObject json = new JSONObject();
        json.put("1", 2202);
        json.put("2", 0);
        json.put("12", 0);
        
        JSONObject obj4 = new JSONObject();
        obj4.put("1", Long.parseLong(qun));
        
        JSONObject obj2 = new JSONObject();
        JSONObject obj20 = new JSONObject();
        obj20.put("1", 10120);
        obj20.put("2", 0);
        obj20.put("3", 0);
        obj20.put("4", location);
        obj2.put("20", obj20);
        obj4.put("2", obj2);
        obj4.put("4", 0);
        
        json.put("4", obj4);
        
        sendPacket("OidbSvcTrpcTcp.0x89a_0", json, null);
        
    } catch (Exception e) {
        Toast("群地点设置失败: " + e.getMessage());
    }
}

public void getAbnormalUsersInfo(String qun, protoListener listener) {
    try {
        JSONObject requestJson = new JSONObject();
        requestJson.put("1", 37312);
        requestJson.put("2", 0);
        requestJson.put("12", 0);
        
        JSONObject obj4 = new JSONObject();
        obj4.put("1", Long.parseLong(qun));
        requestJson.put("4", obj4);
        
        sendPacket("OidbSvcTrpcTcp.0x91c0_0", requestJson, new protoListener() {
            
            public boolean onResponse() {
                return true;
            }
            
            public void onSuccess(String cmd, JSONObject response) {
                try {
                    String resultInfo = "";
                    
                    if (response.has("4")) {
                        JSONObject obj4 = response.getJSONObject("4");
                        
                        if (obj4.has("1")) {
                            JSONArray usersArray = obj4.getJSONArray("1");
                            StringBuilder sb = new StringBuilder();
                            sb.append("异常账号列表:\n");
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObj = usersArray.getJSONObject(i);
                                sb.append(String.valueOf(i+1)).append("、").append(userObj.getLong("1")).append("(").append(getUserName(String.valueOf(userObj.getLong("1")))).append(")").append("\n");
                            }
                                sb.append("本群共").append(usersArray.length()).append("个异常用户");
                            resultInfo = sb.toString();
                        } else {
                            resultInfo = "未获取到本群异常用户数据";
                        }
                    } else if (response.has("5")) {
                        resultInfo = response.getString("5");
                    } else {
                        resultInfo = "未知响应格式";
                    }
                    
                    if (listener != null) {
                        JSONObject result = new JSONObject();
                        result.put("info", resultInfo);
                        listener.onSuccess(cmd, result);
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        listener.onFailure(cmd, "解析失败: " + e.getMessage());
                    }
                }
            }
            
            public void onFailure(String cmd, String error) {
                if (listener != null) {
                    listener.onFailure(cmd, "请求失败: " + error);
                }
            }
        });
        
    } catch (Exception e) {
        if (listener != null) {
            listener.onFailure("OidbSvcTrpcTcp.0x91c0_0", "异常: " + e.getMessage());
        }
    }
}


public void sendVideoMessage(String peerUid, String fileUuid, String videoMd5) {
    try {
        JSONObject pbJson = new JSONObject();
        
        JSONObject obj53 = new JSONObject();
        obj53.put("1", 48);
        
        JSONObject obj2 = new JSONObject();
        JSONObject obj1 = new JSONObject();
        JSONObject obj1_1 = new JSONObject();
        JSONObject obj1_1_1 = new JSONObject();
        obj1_1_1.put("2", videoMd5);
        obj1_1_1.put("4", "test.mp4");
        obj1_1.put("1", obj1_1_1);
        obj1_1.put("2", fileUuid);
        obj1_1.put("3", 1);
        obj1_1.put("6", 0);
        obj1.put("1", obj1_1);
        obj1.put("5", 1);
        obj1.put("6", new JSONObject());
        obj2.put("1", obj1);
        
        JSONObject obj2_2 = new JSONObject();
        JSONObject obj2_2_1 = new JSONObject();
        obj2_2_1.put("1", 0);
        obj2_2_1.put("2", new JSONObject());
        obj2_2.put("1", obj2_2_1);
        obj2.put("2", obj2_2);
        
        obj53.put("2", obj2);
        pbJson.put("53", obj53);
        pbJson.put("3", 24);
        
        sendPbMsg("MessageSvc.PbSendMsg", pbJson.toString(), peerUid);
        
    } catch (Exception e) {
        Toast("发送视频消息失败: " + e.getMessage());
    }
}

public void searchUserRegTime(String uin, protoListener listener) {
    try {
        JSONObject requestJson = new JSONObject();
        requestJson.put("1", uin);
        requestJson.put("2", "9.2.65");
        
        JSONObject obj3 = new JSONObject();
        obj3.put("2", 128);
        obj3.put("4", 20);
        
        JSONObject obj55 = new JSONObject();
        obj55.put("search_by_id_only", 1);
        obj3.put("55", obj55.toString());
        obj3.put("56", 1);
        
        JSONArray arr10 = new JSONArray();
        arr10.put(1001);
        arr10.put(1002);
        arr10.put(8001);
        obj3.put("10", arr10);
        
        requestJson.put("3", obj3);
        
        JSONObject obj100 = new JSONObject();
        obj100.put("7", 0);
        obj100.put("10", "");
        requestJson.put("100", obj100);
        
        sendPacket("UnifySearch.Tab", requestJson, new protoListener() {
            
            public boolean onResponse() {
                return true;
            }
            
            public void onSuccess(String cmd, JSONObject response) {
                try {
                    long regTime = 0;
                    
                    if (response.has("3")) {
                        JSONArray resultArray = response.getJSONArray("3");
                        for (int i = 0; i < resultArray.length(); i++) {
                            JSONObject item = resultArray.getJSONObject(i);
                            if (item.has("1") && item.getInt("1") == 1001) {
                                if (item.has("3")) {
                                    JSONObject obj3 = item.getJSONObject("3");
                                    
                                    // 格式1: obj3有"6"字段，里面有"14"数组
                                    if (obj3.has("6")) {
                                        Object obj6 = obj3.get("6");
                                        if (obj6 instanceof JSONObject) {
                                            JSONObject obj6Json = (JSONObject) obj6;
                                            if (obj6Json.has("14")) {
                                                Object val14 = obj6Json.get("14");
                                                if (val14 instanceof JSONArray) {
                                                    JSONArray arr14 = (JSONArray) val14;
                                                    if (arr14.length() > 0) {
                                                        regTime = arr14.getLong(0);
                                                    }
                                                } else if (val14 instanceof Long) {
                                                    regTime = (Long) val14;
                                                }
                                            }
                                        }
                                    }
                                    
                                    // 格式2: obj3有"2"字段，里面直接有"14"
                                    if (regTime == 0 && obj3.has("2") && obj3.get("2") instanceof JSONObject) {
                                        JSONObject obj2 = obj3.getJSONObject("2");
                                        if (obj2.has("14")) {
                                            Object val14 = obj2.get("14");
                                            if (val14 instanceof Long) {
                                                regTime = (Long) val14;
                                            } else if (val14 instanceof JSONArray) {
                                                JSONArray arr14 = (JSONArray) val14;
                                                if (arr14.length() > 0) {
                                                    regTime = arr14.getLong(0);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (listener != null) {
                        JSONObject result = new JSONObject();
                        result.put("regTime", regTime);
                        result.put("uin", uin);
                        writeLog(response.toString());
                        writeLog(regTime+"");
                        listener.onSuccess(cmd, result);
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        listener.onFailure(cmd, "解析失败: " + e.getMessage());
                    }
                }
            }
            
            public void onFailure(String cmd, String error) {
                if (listener != null) {
                    listener.onFailure(cmd, "请求失败: " + error);
                }
            }
        });
        
    } catch (Exception e) {
        if (listener != null) {
            listener.onFailure("UnifySearch.Tab", "异常: " + e.getMessage());
        }
    }
}