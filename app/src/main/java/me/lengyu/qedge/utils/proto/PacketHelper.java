package me.lengyu.qedge.utils.proto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;
import android.os.Bundle;
import com.tencent.qphone.base.remote.ToServiceMsg;
import com.tencent.qphone.base.remote.FromServiceMsg;
import mqq.app.NewIntent;
import mqq.app.api.impl.SSOEasyServlet;
import mqq.observer.BusinessObserver;
import com.tencent.common.app.BaseApplicationImpl;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import me.lengyu.qedge.utils.json.ProtoData;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.proto.packetListener;
import me.lengyu.qedge.utils.proto.protoListener;


public class PacketHelper {

    public static int rand(int min, int max) {
        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

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

    public static void sendRequest(String serviceCmd, byte[] rawData, protoListener listener) {
        try {
            byte[] reqBytes = packet(rawData);
            NewIntent intent = new NewIntent(BaseApplicationImpl.getApplication(), SSOEasyServlet.class);
            ToServiceMsg toServiceMsg = new ToServiceMsg("mobileqq.service", "0", serviceCmd);
            toServiceMsg.putWupBuffer(reqBytes);

            intent.setObserver(new BusinessObserver() {
                public void onReceive(int type, boolean isSuccess, Bundle bundle) {
                    if (isSuccess && bundle != null) {
                        FromServiceMsg fromMsg = (FromServiceMsg) bundle.getParcelable("FromServiceMsg");
                        if (fromMsg != null) {
                            try {
                                ProtoData protoData = new ProtoData();
                                protoData.fromBytes(fromMsg.getWupBuffer());
                                JSONObject json = protoData.toJSON();
                                listener.onSuccess(serviceCmd, json);
                            } catch (Exception e) {
                            }
                        } else {
                        
                                listener.onFailure(serviceCmd, "响应数据为空");
                        }
                    }
                }
            });

            intent.putExtra("ToServiceMsg", toServiceMsg);
            BaseApplicationImpl.getApplication().getRuntime().startServlet(intent);
        } catch (Exception e) {
        }
    }

    public static void sendPacket(String serviceCmd, JSONObject pbData, packetListener listener) {
        try {
            ProtoData protoData = new ProtoData();
            protoData.fromJSON(pbData);
            byte[] sendData = protoData.toBytes();

            sendRequest(serviceCmd, sendData, new protoListener() {
                @Override
                public void onSuccess(String serviceCmd, JSONObject json) {
                    try {
                        listener.onResult(true, json);
                    } catch (Exception ex) {
                        LogUtils.e("PacketHelper", "Failed to send packet: " + ex.getMessage());
                    }
                }

                @Override
                public void onFailure(String serviceCmd, String errorMsg) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("1", serviceCmd);
                        json.put("2", errorMsg);
                        json.put("12", 0);
                        listener.onResult(false, json);
                    } catch (Exception ex) {
                        LogUtils.e("PacketHelper", "Failed to send packet: " + ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            if (listener != null) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("1", serviceCmd);
                    json.put("2", e.getMessage());
                    json.put("12", 0);
                    listener.onResult(false, json);
                } catch (Exception ex) {
                    LogUtils.e("PacketHelper", "Failed to send packet: " + ex.getMessage());
                }
            }
        }
    }
}