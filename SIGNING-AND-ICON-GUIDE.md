# APK 签名和图标配置指南

## 问题 1: 开发者证书问题

### 解决方案：生成签名密钥并签名 APK

#### 步骤 1: 生成密钥库文件

在项目根目录执行以下命令：

```bash
keytool -genkey -v -keystore movinghacker-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias movinghacker
```

**填写信息示例：**
```
输入密钥库口令: [输入密码，例如: MySecurePassword123]
再次输入新口令: [再次输入相同密码]
您的名字与姓氏是什么? Sorin
您的组织单位名称是什么? Development
您的组织名称是什么? MovingHacker
您所在的城市或区域名称是什么? Beijing
您所在的省/市/自治区名称是什么? Beijing
该单位的双字母国家/地区代码是什么? CN
CN=Sorin, OU=Development, O=MovingHacker, L=Beijing, ST=Beijing, C=CN是否正确? [否]: 是

输入 <movinghacker> 的密钥口令 [直接回车使用与密钥库相同的密码]
```

**重要：** 请妥善保管密钥库文件和密码！丢失后无法更新应用！

#### 步骤 2: 配置 Gradle 签名

创建 `keystore.properties` 文件（不要提交到 Git）：

```properties
storePassword=MySecurePassword123
keyPassword=MySecurePassword123
keyAlias=movinghacker
storeFile=movinghacker-release.jks
```

#### 步骤 3: 更新 build.gradle.kts

在 `app/build.gradle.kts` 中添加签名配置（已为你准备好）

#### 步骤 4: 重新构建签名 APK

```bash
.\gradlew.bat clean assembleRelease
```

签名后的 APK 将位于：
```
app/build/outputs/apk/py311/release/app-py311-release.apk
```

---

## 问题 2: 应用图标

### 解决方案：使用 favicon.ico 创建应用图标

Android 需要多种尺寸的图标，我将为你：
1. 将 favicon.ico 转换为 PNG 格式
2. 生成不同尺寸的图标
3. 替换现有的默认图标

#### 所需图标尺寸：
- mipmap-mdpi: 48x48
- mipmap-hdpi: 72x72
- mipmap-xhdpi: 96x96
- mipmap-xxhdpi: 144x144
- mipmap-xxxhdpi: 192x192

---

## 快速执行步骤

### 1. 生成密钥（首次）
```bash
keytool -genkey -v -keystore movinghacker-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias movinghacker
```

### 2. 创建 keystore.properties
```bash
echo storePassword=你的密码 > keystore.properties
echo keyPassword=你的密码 >> keystore.properties
echo keyAlias=movinghacker >> keystore.properties
echo storeFile=movinghacker-release.jks >> keystore.properties
```

### 3. 构建签名 APK
```bash
.\gradlew.bat clean assembleRelease
```

### 4. 安装测试
```bash
adb install app\build\outputs\apk\py311\release\app-py311-release.apk
```

---

## 注意事项

### 密钥安全
- ✅ 将 `movinghacker-release.jks` 和 `keystore.properties` 添加到 `.gitignore`
- ✅ 备份密钥文件到安全位置
- ✅ 记录密码（使用密码管理器）
- ❌ 不要将密钥文件提交到 Git
- ❌ 不要分享密钥文件

### 签名验证
签名后的 APK 可以：
- ✅ 正常安装到所有 Android 设备
- ✅ 上传到 Google Play Store
- ✅ 通过 APK 签名验证
- ✅ 支持应用更新

### 图标说明
- 使用 favicon.ico 作为基础
- 自动生成所有需要的尺寸
- 支持圆形图标（Android 8.0+）
- 支持自适应图标

---

## 故障排除

### 问题：keytool 命令不存在
**解决：** 确保 JDK 已安装并添加到 PATH

### 问题：密钥库已存在
**解决：** 删除旧的 .jks 文件或使用不同的文件名

### 问题：签名失败
**解决：** 检查 keystore.properties 中的路径和密码是否正确

### 问题：安装时仍提示证书问题
**解决：** 确保使用的是签名后的 APK，而不是 unsigned 版本
