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

dependencies {
    // ADB di dalam app (Wireless debugging / USB debugging lewat jaringan). Lisensi Apache-2.0 dipilih.
    implementation("com.github.MuntashirAkon:libadb-android:3.1.1")
    implementation("com.github.MuntashirAkon:sun-security-android:1.1")
    implementation("org.conscrypt:conscrypt-android:2.5.3")
}
