package me.lengyu.qedge.hook.item;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.HookUtils;

/**
 * @Author 冷雨
 * @Description 禁用QQ日志上报（拦截最终SSO发送）
 */
@HookItemAnnotation(value = "禁用QQ日志上报", category = "item")
public class AntiReport extends BaseApiHookItem {
    public static final AntiReport INSTANCE = new AntiReport();
    private static final String TAG = "AntiReport";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("anti_report", false);
    }

    @Override
    public void loadHook() {
        // ========== 方案1：拦截 Packet.setSSOCommand（最稳定）==========
        hookPacketSetSSOCommand();
        
        // ========== 方案2：拦截 MSFServlet 发送 ==========
        hookMSFServlet();
        
        // LogUtils.d(TAG, "loadHook: 已禁用日志上报");
    }

    /**
     * 拦截 Packet.setSSOCommand
     * 所有上报都会设置 SSO 命令为 "CliLogSvc.UploadReq"
     */
    private void hookPacketSetSSOCommand() {
        try {
            Class<?> packetClass = ReflectUtils.hostClassLoader.loadClass("mqq.app.Packet");
            
            Method setSSOCommand = null;
            for (Method method : packetClass.getDeclaredMethods()) {
                if (method.getName().equals("setSSOCommand")) {
                    setSSOCommand = method;
                    break;
                }
            }
            
            if (setSSOCommand != null) {
                setSSOCommand.setAccessible(true);
                HookUtils.hookBefore(setSSOCommand, param -> {
                    if (!isEnabled()) {
                        return;
                    }
                    
                    String cmd = param.args.length > 0 ? (String) param.args[0] : "";
                    // 只拦截上报命令
                    if (cmd != null && cmd.equals("CliLogSvc.UploadReq")) {
                        // LogUtils.d(TAG, "拦截上报: " + cmd);
                        param.setResult(null);
                    }
                });
                // LogUtils.d(TAG, "hookPacketSetSSOCommand: 已拦截 setSSOCommand");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "hookPacketSetSSOCommand: " + e.getMessage());
        }
    }

    /**
     * 拦截 MSFServlet 的发送方法
     */
    private void hookMSFServlet() {
        try {
            Class<?> servletClass = ReflectUtils.hostClassLoader.loadClass("mqq.app.MSFServlet");
            
            // 拦截 sendToMSF 方法
            Method sendToMSF = null;
            for (Method method : servletClass.getDeclaredMethods()) {
                if (method.getName().equals("sendToMSF") || 
                    method.getName().equals("send") ||
                    method.getName().equals("sendRequest")) {
                    sendToMSF = method;
                    break;
                }
            }
            
            if (sendToMSF != null) {
                sendToMSF.setAccessible(true);
                HookUtils.hookBefore(sendToMSF, param -> {
                    if (!isEnabled()) {
                        return;
                    }
                    
                    // 检查 Intent 中的命令
                    Object intent = param.args.length > 0 ? param.args[0] : null;
                    if (intent != null) {
                        try {
                            Object extras = XposedHelpers.callMethod(intent, "getExtras");
                            if (extras != null) {
                                String cmd = (String) XposedHelpers.callMethod(extras, "getString", "ssoCmd");
                                if (cmd != null && cmd.equals("CliLogSvc.UploadReq")) {
                                    // LogUtils.d(TAG, "拦截 MSFServlet.send: " + cmd);
                                    param.setResult(null);
                                }
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                });
                // LogUtils.d(TAG, "hookMSFServlet: 已拦截 sendToMSF");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "hookMSFServlet: " + e.getMessage());
        }
    }
}