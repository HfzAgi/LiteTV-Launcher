plugins { id("com.android.application") }

android {
    namespace = "com.litetv.bersih"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.litetv.bersih"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
