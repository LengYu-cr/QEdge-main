plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.tencent"
    compileSdk = 37
    enableKotlin = false

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("androidx.annotation:annotation:1.7.0")
    compileOnly("org.jetbrains:annotations:26.1.0")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
}
