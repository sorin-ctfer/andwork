plugins {
    alias(libs.plugins.android.application)
    id("com.chaquo.python") version "15.0.1"
}

import java.util.Properties
import java.io.FileInputStream

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
        
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
    
    // 签名配置
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    
    flavorDimensions += "pyVersion"
    productFlavors {
        create("py311") {
            dimension = "pyVersion"
        }
    }
    
    chaquopy {
        defaultConfig {
            version = "3.11"
            pip {
                install("requests")
                install("numpy")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    
    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSch for SSH
    implementation("com.jcraft:jsch:0.1.55")
    
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Room for database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    
    // ViewPager2 for tabs
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // Sora Editor with language support
    implementation("io.github.Rosemoe.sora-editor:editor:0.23.4")
    implementation("io.github.Rosemoe.sora-editor:language-java:0.23.4")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
