# =====================
# QEdge 模块配置
# 策略：启用代码/资源压缩和优化，禁用类名混淆（Xposed模块必须）
# =====================

# 禁止混淆类名和方法名（Xposed模块必须）
-dontobfuscate

# 保持模块自身的所有类和成员（Xposed模块必须）
-keep class me.lengyu.qedge.** { *; }

# 保持 Kotlin 函数类及其所有成员
-keep class kotlin.jvm.functions.** { *; }

# 保持所有包含原生方法的类
-keepclasseswithmembernames class * {
    native <methods>;
}

# =====================
# Xposed 相关配置
# =====================

-adaptresourcefilecontents META-INF/xposed/java_init.list
-keepattributes RuntimeVisibleAnnotations

# 保持 Xposed API 相关的类
-keep class android.view.animation.PathInterpolator { *; }

# 忽略 Xposed 相关的警告
-dontwarn de.robv.android.xposed.**

# =====================
# 第三方库配置
# =====================

# BeanShell - 保持不变
-dontwarn bsh.**
-keep class bsh.** { *; }

# Rhino JS 引擎 - 内部大量反射(方法/字段名不能变)，需全量保留
-dontwarn org.mozilla.javascript.**
-keep class org.mozilla.javascript.** { *; }

# Protobuf - 保持必要的类
-dontwarn com.google.protobuf.**
-keepclassmembers public class * extends com.google.protobuf.MessageLite {*;}
-keepclassmembers public class * extends com.google.protobuf.MessageOrBuilder {*;}

# DexKit - 保持不变
-dontwarn com.swiftchains.**
-keep class com.swiftchains.** { *; }

# ByteBuddy - 保持不变
-keep class net.bytebuddy.** { *; }

# KavaRef references this reflection type, which is absent from Android SDK stubs.
-dontwarn java.lang.reflect.AnnotatedType

# =====================
# 通用保持规则
# =====================

# 保持必要的属性和注解
-keepattributes LineNumberTable,SourceFile,RuntimeVisibleAnnotations,AnnotationDefault,*Annotation*

# 保持枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保持 Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保持 Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =====================
# 优化配置（仅移除无用代码，不修改代码结构）
# =====================

# Kotlin Intrinsics 方法没有副作用，可以安全移除
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# Java Objects.requireNonNull 方法没有副作用，可以安全移除
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# 忽略警告
-dontwarn javax.**
-dontwarn java.awt.**
-dontwarn org.apache.bsf.**
-dontwarn com.sun.jna.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.lang.instrument.**
