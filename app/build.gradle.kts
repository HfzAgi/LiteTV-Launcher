plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.litetv.launcher"
 compileSdk = 35
 defaultConfig { applicationId = "com.litetv.launcher"; minSdk = 21; targetSdk = 28; versionCode = 1; versionName = "1.0" }
 signingConfigs { getByName("debug") { enableV1Signing = true; enableV2Signing = true } }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
