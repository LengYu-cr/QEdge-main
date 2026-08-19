package me.lengyu.qedge.utils.qq;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.tencent.mobileqq.app.CardHandler;
import com.tencent.mobileqq.addfriend.api.IDelFriendService;
import com.tencent.common.app.BaseApplicationImpl;
import com.tencent.qqnt.ntrelation.friendsinfo.api.IFriendsInfoService;
import com.tencent.relation.common.api.IRelationNTUinAndUidApi;
import me.lengyu.qedge.plugin.bean.FriendInfo;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;

public class FriendTool {

    public static List<FriendInfo> getAllFriend() {
        List<FriendInfo> friendInfoList = new ArrayList<>();
        try {
            IFriendsInfoService service = QQServiceHelper.getApi(IFriendsInfoService.class);
            if (service == null) return friendInfoList;

            List<Object> friendList = service.getAllFriend("");
            for (Object friend : friendList) {
                try {
                    String friendStr = friend.toString();
                    String[] info = friendStr.split(" ");
                    if (info.length >= 5) {
                        String uid = info[4];
                        String nick = service.getNickWithUid(uid, "");
                        String remark = service.getRemarkWithUid(uid, "");
                        friendInfoList.add(new FriendInfo(info[2], uid, nick, remark));
                    }
                } catch (Throwable e) {
                    LogUtils.e(e);
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return friendInfoList;
    }

    public static boolean isFriend(String uin) {
        try {
            IFriendsInfoService service = QQServiceHelper.getApi(IFriendsInfoService.class);
            if (service != null) {
                return service.isFriend(getUidFromUin(uin), "");
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return false;
    }


    public static boolean deleteFriend(String uin){
        try {
            IDelFriendService service = QQServiceHelper.getRuntimeService(IDelFriendService.class);
            if (service == null) return false;
            service.delFriend("FriendsManager_deleteFriend", uin, (byte) 0, 0);
            return !isFriend(uin);
        } catch (Throwable e) {
            LogUtils.e("FriendTool", "deleteFriend error: " + e.getMessage());
        }
        return false;
    }

    public static String getUidFromUin(String uin) {
        try {
            if(isValidUid(uin)) return uin;
            IRelationNTUinAndUidApi service = QQServiceHelper.getApi(IRelationNTUinAndUidApi.class);
            if (service != null) {
                return service.getUidFromUin(uin);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return uin;
    }

    public static String getUinFromUid(String uid) {
        try {
            if(isValidUin(uid)) return uid;
            IRelationNTUinAndUidApi service = QQServiceHelper.getApi(IRelationNTUinAndUidApi.class);
            if (service != null) {
                return service.getUinFromUid(uid);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return uid;
    }

    public static void sendZan(String uin, int num) {
        try {
            CardHandler handler = QQServiceHelper.getHandler(CardHandler.class);
            if (handler == null) return;

            byte[] bArr = new byte[10];
            bArr[0] = (byte) 12;
            bArr[1] = (byte) 24;
            bArr[2] = (byte) 0;
            bArr[3] = (byte) 1;
            bArr[4] = (byte) 6;
            bArr[5] = (byte) 1;
            bArr[6] = (byte) 49;
            bArr[7] = (byte) 22;
            bArr[8] = (byte) 1;
            bArr[9] = (byte) (isFriend(uin) ? 49 : 53);

            int type = isFriend(uin) ? 1 : 5;

            Class<?>[] paramType = new Class[]{
                    Long.TYPE, Long.TYPE, byte[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE
            };

            Method[] methods = CardHandler.class.getMethods();
            for (Method m : methods) {
                if (m.getParameterCount() == paramType.length) {
                    Class<?>[] params = m.getParameterTypes();
                    boolean match = true;
                    for (int i = 0; i < paramType.length; i++) {
                        if (!params[i].equals(paramType[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        m.setAccessible(true);
                        m.invoke(handler, new Object[]{
                                Long.parseLong(QQCurrentEnv.getCurrentUin()),
                                Long.parseLong(uin),
                                bArr,
                                type,
                                num,
                                0
                        });
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    public static boolean isValidUid(String uid) {
        return uid != null && uid.startsWith("u_") && uid.length() > 2;
    }

    public static boolean isValidUin(String uin) {
        if (uin == null || uin.isEmpty()) {
            return false;
        }
        if (!uin.matches("\\d+")) {
            return false;
        }
        try {
            long uinLong = Long.parseLong(uin);
            return uinLong > 10000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}