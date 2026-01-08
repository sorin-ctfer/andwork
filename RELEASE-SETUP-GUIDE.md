# 发布版配置完整指南

## 问题说明

你遇到了两个问题：
1. **安装时提示没有开发者证书** - 因为 APK 未签名
2. **没有使用 favicon.ico 作为应用图标** - 需要转换并替换图标

## 解决方案概览

我已经为你准备了完整的自动化工具：

### 📁 新增文件
- `generate-keystore.bat` - 生成签名密钥的脚本
- `generate-icons.py` - 转换图标的 Python 脚本
- `setup-release.bat` - 一键配置发布版的脚本
- `SIGNING-AND-ICON-GUIDE.md` - 详细的手动操作指南
- `RELEASE-SETUP-GUIDE.md` - 本文档

### 🔧 修改文件
- `app/build.gradle.kts` - 添加了签名配置
- `.gitignore` - 添加了密钥文件排除规则

---

## 🚀 快速开始（推荐）

### 方法 1: 一键自动配置

直接运行自动化脚本：

```bash
setup-release.bat
```

这个脚本会自动：
1. ✅ 生成签名密钥
2. ✅ 转换应用图标
3. ✅ 构建签名 APK

按照提示操作即可！

---

## 📝 手动配置（详细步骤）

如果自动脚本遇到问题，可以手动执行：

### 步骤 1: 生成签名密钥

运行密钥生成脚本：

```bash
generate-keystore.bat
```

或手动执行：

```bash
keytool -genkey -v -keystore movinghacker-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias movinghacker
```

**填写信息示例：**
```
密钥库口令: MySecurePassword123
再次输入: MySecurePassword123
名字与姓氏: Sorin
组织单位: Development
组织名称: MovingHacker
城市: Beijing
省份: Beijing
国家代码: CN
```

**创建 keystore.properties 文件：**

```properties
storePassword=MySecurePassword123
keyPassword=MySecurePassword123
keyAlias=movinghacker
storeFile=movinghacker-release.jks
```

### 步骤 2: 转换应用图标

**前提条件：**
- 安装 Python 3.x
- 安装 Pillow 库：`pip install Pillow`

**运行图标生成脚本：**

```bash
python generate-icons.py
```

这会将 `favicon.ico` 转换为以下尺寸的 PNG 图标：
- mipmap-mdpi: 48x48
- mipmap-hdpi: 72x72
- mipmap-xhdpi: 96x96
- mipmap-xxhdpi: 144x144
- mipmap-xxxhdpi: 192x192

旧图标会自动备份到 `icon_backup` 目录。

### 步骤 3: 构建签名 APK

```bash
gradlew.bat clean assembleRelease
```

### 步骤 4: 安装测试

```bash
adb install app\build\outputs\apk\py311\release\app-py311-release.apk
```

---

## 🔍 验证签名

### 查看 APK 签名信息

```bash
keytool -printcert -jarfile app\build\outputs\apk\py311\release\app-py311-release.apk
```

应该显示：
```
所有者: CN=Sorin, OU=Development, O=MovingHacker, L=Beijing, ST=Beijing, C=CN
发布者: CN=Sorin, OU=Development, O=MovingHacker, L=Beijing, ST=Beijing, C=CN
```

### 验证 APK 完整性

```bash
jarsigner -verify -verbose -certs app\build\outputs\apk\py311\release\app-py311-release.apk
```

应该显示：`jar verified.`

---

## 📱 安装说明

### 方法 1: ADB 安装（推荐）

```bash
# 连接设备
adb devices

# 安装 APK
adb install app\build\outputs\apk\py311\release\app-py311-release.apk

# 如果已安装旧版本，使用 -r 参数覆盖安装
adb install -r app\build\outputs\apk\py311\release\app-py311-release.apk
```

### 方法 2: 手动安装

1. 将 APK 文件复制到 Android 设备
2. 在设备上找到 APK 文件
3. 点击安装
4. 如果提示"未知来源"，在设置中允许安装

**签名后的 APK 不会再提示"没有开发者证书"！**

---

## 🎨 图标说明

### 图标文件位置

生成的图标位于：
```
app/src/main/res/
├── mipmap-mdpi/
│   ├── ic_launcher.png (48x48)
│   └── ic_launcher_round.png (48x48)
├── mipmap-hdpi/
│   ├── ic_launcher.png (72x72)
│   └── ic_launcher_round.png (72x72)
├── mipmap-xhdpi/
│   ├── ic_launcher.png (96x96)
│   └── ic_launcher_round.png (96x96)
├── mipmap-xxhdpi/
│   ├── ic_launcher.png (144x144)
│   └── ic_launcher_round.png (144x144)
└── mipmap-xxxhdpi/
    ├── ic_launcher.png (192x192)
    └── ic_launcher_round.png (192x192)
```

### 图标类型

- **ic_launcher.png** - 标准方形图标
- **ic_launcher_round.png** - 圆形图标（Android 7.1+）

### 查看效果

安装 APK 后，在设备的应用列表中查看图标效果。

---

## ⚠️ 重要注意事项

### 密钥安全

1. **妥善保管密钥文件和密码**
   - 密钥文件：`movinghacker-release.jks`
   - 配置文件：`keystore.properties`
   - 密码：记录在安全的地方

2. **备份密钥**
   ```bash
   # 建议将密钥文件备份到多个安全位置
   copy movinghacker-release.jks D:\Backup\
   copy keystore.properties D:\Backup\
   ```

3. **不要提交到 Git**
   - 这些文件已自动添加到 `.gitignore`
   - 永远不要将密钥文件提交到版本控制

4. **密钥丢失后果**
   - 无法更新已发布的应用
   - 必须使用新的包名重新发布
   - 用户需要卸载旧版本安装新版本

### 版本管理

每次发布新版本时，记得更新版本号：

在 `app/build.gradle.kts` 中：
```kotlin
defaultConfig {
    versionCode = 2  // 每次发布递增
    versionName = "1.1"  // 版本名称
}
```

---

## 🐛 故障排除

### 问题 1: keytool 命令不存在

**原因：** JDK 未安装或未添加到 PATH

**解决：**
1. 确认 JDK 已安装
2. 添加 JDK bin 目录到 PATH
3. 重启命令行窗口

### 问题 2: Python 或 Pillow 未安装

**解决：**
```bash
# 安装 Python（从 python.org 下载）
# 安装 Pillow
pip install Pillow
```

### 问题 3: 签名配置错误

**错误信息：** `Keystore file not found`

**解决：**
1. 检查 `keystore.properties` 文件是否存在
2. 检查文件中的路径是否正确
3. 确保 `movinghacker-release.jks` 在项目根目录

### 问题 4: 构建失败

**解决：**
```bash
# 清理后重新构建
gradlew.bat clean
gradlew.bat assembleRelease
```

### 问题 5: 安装时仍提示证书问题

**检查：**
1. 确认使用的是签名后的 APK（文件名不含 "unsigned"）
2. 确认 APK 路径：`app\build\outputs\apk\py311\release\app-py311-release.apk`
3. 验证签名：`jarsigner -verify app\build\outputs\apk\py311\release\app-py311-release.apk`

### 问题 6: 图标未更新

**解决：**
1. 清理构建缓存：`gradlew.bat clean`
2. 重新构建：`gradlew.bat assembleRelease`
3. 卸载旧版本后重新安装

---

## 📊 文件清单

### 生成的文件（不要提交到 Git）
- ✅ `movinghacker-release.jks` - 签名密钥文件
- ✅ `keystore.properties` - 密钥配置文件
- ✅ `icon_backup/` - 旧图标备份目录

### 工具脚本（可以提交到 Git）
- ✅ `generate-keystore.bat` - 密钥生成脚本
- ✅ `generate-icons.py` - 图标转换脚本
- ✅ `setup-release.bat` - 一键配置脚本
- ✅ `SIGNING-AND-ICON-GUIDE.md` - 签名和图标指南
- ✅ `RELEASE-SETUP-GUIDE.md` - 本文档

---

## 🎯 下一步

1. ✅ 运行 `setup-release.bat` 完成配置
2. ✅ 安装生成的 APK 到测试设备
3. ✅ 验证签名和图标是否正确
4. ✅ 将 APK 上传到 GitHub Releases
5. ✅ 更新 README.md 中的下载链接

---

## 📞 需要帮助？

如果遇到问题：
1. 查看本文档的"故障排除"部分
2. 查看 `SIGNING-AND-ICON-GUIDE.md` 获取更多细节
3. 检查构建日志中的错误信息
4. 在 GitHub Issues 中提问

---

**祝你发布顺利！🚀**
