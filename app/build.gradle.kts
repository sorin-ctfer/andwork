plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.movinghacker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.movinghacker"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val tencentSecretId = (project.findProperty("TENCENT_SECRET_ID") as String?) ?: ""
        val tencentSecretKey = (project.findProperty("TENCENT_SECRET_KEY") as String?) ?: ""
        buildConfigField("String", "TENCENT_SECRET_ID", "\"${tencentSecretId}\"")
        buildConfigField("String", "TENCENT_SECRET_KEY", "\"${tencentSecretKey}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
