import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// 签名口令不入库：从 local.properties（已 gitignore）读取，缺失时给出明确提示
val signingProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingProp(key: String): String =
    signingProps.getProperty(key) ?: error("local.properties 缺少 $key，无法签名 release 包")

android {
    namespace = "me.lengyu.qedge"
    compileSdk = 37

    defaultConfig {
        applicationId = "me.lengyu.qedge"
        minSdk = 29
        targetSdk = 37
        versionCode = 29
        versionName = "0.2.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // 仅保留 64 位 arm64，去掉 v7a 减小体积；32 位设备在 XposedEntry 处拦截提示
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("qedge.jks")
            storePassword = signingProp("qedge.storePassword")
            keyAlias = signingProp("qedge.keyAlias")
            keyPassword = signingProp("qedge.keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x69")
        // 资源表(arsc)只保留中文，其他语言不打包
        localeFilters += listOf("zh")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // 去除依赖 jar 打进来的无用资源：Rhino 调试/工具(test.js 等)、protobuf 源描述文本
            excludes += "org/mozilla/javascript/tools/**"
            excludes += "google/protobuf/**"
            // Rhino 只内置了英文与法语两套消息，模块面向中文用户，法语文案用不到
            excludes += "org/mozilla/javascript/resources/Messages_fr.properties"
            // 依赖 jar 的版本标记与 LICENSE 副本，与安装包运行无关
            excludes += "META-INF/*.version"
            excludes += "META-INF/androidx/**/LICENSE.txt"
            // kotlinx-coroutines 自带的协程调试探针元数据，release 用不到，纯冗余
            excludes += "DebugProbesKt.bin"
        }
    }
}

dependencies {
    // androidx
    implementation(libs.androidx.core.ktx)
    compileOnly(libs.androidx.savedstate)
    // compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    // ui
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    // material3
    implementation(libs.androidx.material3)
    // lifecycle
    compileOnly(libs.androidx.lifecycle.runtime.ktx)
    compileOnly(libs.androidx.lifecycle.viewmodel.ktx)
    compileOnly(libs.androidx.lifecycle.common.java8)
    // test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // dexkit
    implementation(libs.dexkit)
    // protobuf
    implementation(libs.protobuf.javalite)
    // legacy xposed api
    compileOnly(libs.xposed)
    // dx
    implementation(libs.dalvik.dx)
    // rhino js engine
    implementation(libs.rhino)
    // qq stub
    compileOnly(project(":qqinterface"))
}


val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
val packageName = "com.tencent.mobileqq"
val killQQ = tasks.register<Exec>("killQQ") {
    group = "adb"
    description = "杀死QQ主进程"
    commandLine(adb, "shell", "am", "force-stop", packageName)
    isIgnoreExitValue = true
}

val openQQ = tasks.register<Exec>("openQQ") {
    group = "adb"
    description = "打开QQ"
    commandLine(adb, "shell", "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1")
}

val restartQQ = tasks.register<Exec>("restartQQ") {
    group = "adb"
    description = "重新启动QQ"
    dependsOn(killQQ)
    commandLine(adb, "shell", "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1")
}

val installDebugAndRestartQQ = tasks.register("installDebugAndRestartQQ") {
    group = "adb"
    description = "安装Debug版本并重启QQ"
    dependsOn(":app:installDebug")
    finalizedBy(restartQQ)
}