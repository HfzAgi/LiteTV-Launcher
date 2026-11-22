plugins { id("com.android.application") }

android {
    namespace = "com.litetv.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.litetv.launcher"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
