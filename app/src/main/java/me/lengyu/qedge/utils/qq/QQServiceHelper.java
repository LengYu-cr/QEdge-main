package me.lengyu.qedge.utils.qq;

import java.lang.reflect.Method;
import mqq.app.AppRuntime;
import mqq.app.api.IRuntimeService;
import com.tencent.mobileqq.qroute.QRoute;
import com.tencent.mobileqq.app.QQAppInterface;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import com.tencent.common.app.BaseApplicationImpl;

public class QQServiceHelper {

    public static <T> T getApi(Class<T> apiClass) {
        try {
            T result = (T) QRoute.api(apiClass);
            if (result != null) {
                return result;
            }
        } catch (Throwable ignored) {
        }

        try {
            Object qqAppInterface = QQCurrentEnv.getQQAppInterface();
            if (qqAppInterface != null) {
                ClassLoader qqClassLoader = qqAppInterface.getClass().getClassLoader();
                Class<?> qRouteClass = qqClassLoader.loadClass("com.tencent.mobileqq.qroute.QRoute");
                Method apiMethod = qRouteClass.getMethod("api", Class.class);
                Object apiObj = apiMethod.invoke(null, apiClass);
                return (T) apiObj;
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static <T> T getRuntime(Class<T> runtimeClass) {
        try {
            T runtime = getApi(runtimeClass);
            if (runtime != null) return runtime;

            Object qqAppInterface = QQCurrentEnv.getQQAppInterface();
            if (qqAppInterface != null) {
                Method getRuntimeMethod = ReflectUtils.findMethod(QQAppInterface.class, runtimeClass);
                if (getRuntimeMethod != null) {
                    @SuppressWarnings("unchecked")
                    T result = (T) getRuntimeMethod.invoke(qqAppInterface);
                    return result;
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static <T> T getHandler(Class<T> handlerClass) {
        try {
            Object qqAppInterface = QQCurrentEnv.getQQAppInterface();
            if (qqAppInterface != null) {
                Method getBusinessHandlerMethod = ReflectUtils.findMethod(qqAppInterface.getClass(), "getBusinessHandler", String.class);
                if (getBusinessHandlerMethod != null) {
                    Object handler = getBusinessHandlerMethod.invoke(qqAppInterface, handlerClass.getName());
                    return (T) handler;
                }
                
                Method getHandlerMethod = ReflectUtils.findMethod(qqAppInterface.getClass(), handlerClass);
                if (getHandlerMethod != null) {
                    @SuppressWarnings("unchecked")
                    T result = (T) getHandlerMethod.invoke(qqAppInterface);
                    return result;
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static AppRuntime getRuntime(){
        BaseApplicationImpl impl = BaseApplicationImpl.getApplication();
        return impl.getRuntime();
    }

    public static <T extends IRuntimeService> T getRuntimeService(Class<T> serviceClass){
        return getRuntimeService(serviceClass, "");
    }

    public static <T extends IRuntimeService> T getRuntimeService(Class<T> serviceClass, String serviceId){
        AppRuntime runtime = getRuntime();
        if (runtime == null) return null;
        return runtime.getRuntimeService(serviceClass, serviceId);
    }
}
