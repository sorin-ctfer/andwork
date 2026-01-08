# APK 构建指南

## 构建成功 ✓

APK 文件已成功构建！

### 文件位置
```
app\build\outputs\apk\py311\release\app-py311-release-unsigned.apk
```

### 文件信息
- **大小**: 69.8 MB (69,791,732 字节)
- **类型**: 未签名的 Release APK
- **构建时间**: 2026/1/8 13:57

## 关于未签名 APK

当前构建的是**未签名**的 APK。这意味着：
- ✓ 可以安装到开发设备上（需要启用"未知来源"）
- ✗ 不能上传到 Google Play Store
- ✗ 某些设备可能拒绝安装

## 签名 APK（用于发布）

如果需要发布到应用商店或正式分发，需要签名 APK：

### 1. 生成密钥库（首次）
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

### 2. 配置 build.gradle.kts

在 `app/build.gradle.kts` 中添加签名配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/my-release-key.jks")
            storePassword = "your-store-password"
            keyAlias = "my-key-alias"
            keyPassword = "your-key-password"
        }
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
}
```

### 3. 重新构建
```bash
.\gradlew.bat assembleRelease
```

## 快速测试安装

### 通过 ADB 安装
```bash
adb install app\build\outputs\apk\py311\release\app-py311-release-unsigned.apk
```

### 手动安装
1. 将 APK 文件复制到 Android 设备
2. 在设备上启用"未知来源"安装
3. 点击 APK 文件进行安装

## 构建其他版本

### Debug 版本
```bash
.\gradlew.bat assembleDebug
```

### 清理后重新构建
```bash
.\gradlew.bat clean assembleRelease
```

## 注意事项

1. **密钥库安全**: 妥善保管密钥库文件和密码，丢失后无法更新应用
2. **版本号**: 每次发布新版本时，记得更新 `versionCode` 和 `versionName`
3. **混淆**: 当前未启用代码混淆（`isMinifyEnabled = false`），正式发布建议启用
4. **Python 依赖**: APK 包含 Python 3.11 运行时和依赖库（requests, numpy），因此体积较大

## 下一步

- [ ] 在测试设备上安装并测试 APK
- [ ] 如需发布，生成签名密钥并重新构建
- [ ] 准备应用商店发布材料（截图、描述等）
- [ ] 推送代码到 GitHub（解决网络问题后）
