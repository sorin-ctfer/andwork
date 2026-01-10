# MovingHacker

一个功能强大的Android渗透测试和开发工具集合应用。

## 功能特性

### 🌐 Web请求模块
- 支持所有HTTP方法（GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS）
- 自定义请求头和请求体
- 支持表单数据、JSON、XML等多种格式
- 文件上传功能
- 请求历史记录

### 📁 文件管理器
- 双窗格文件浏览
- SSH远程文件管理
- 文件搜索和编辑
- 拖拽操作支持

### 💻 终端模拟器
- 本地Shell终端
- SSH远程连接
- 命令历史记录
- 多会话管理

### 🐍 Python IDE
- Python代码编辑器
- 语法高亮
- 代码执行
- 支持numpy、requests等常用库

### 🤖 AI助手
- 集成多个AI模型（OpenAI、Gemini、SiliconFlow、ZenMux）
- Function Calling支持
- 可执行Web请求、文件操作、Python代码、终端命令
- 支持Hacking模式

### 📷 OCR识别
- 身份证识别
- 银行卡识别
- 通用文字识别
- 支持腾讯云OCR API

## 构建要求

- Android Studio Arctic Fox或更高版本
- JDK 11或更高版本
- Android SDK API 24+（最低）/ API 34（目标）
- Gradle 8.9

## 构建步骤

1. 克隆仓库：
```bash
git clone <repository-url>
cd MovingHacker
```

2. 配置Android SDK路径：
```bash
# 复制示例配置文件
cp local.properties.example local.properties

# 编辑local.properties，设置你的Android SDK路径
# Windows: sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
# macOS/Linux: sdk.dir=/Users/YourUsername/Library/Android/sdk
```

3. 使用Android Studio打开项目，或使用命令行构建

4. 等待Gradle同步完成

5. 构建APK：
```bash
./gradlew assembleRelease
```

或在Windows上：
```bash
gradlew.bat assembleRelease
```

6. 生成的APK位于：`app/build/outputs/apk/py311/release/`

## 配置说明

### 必需配置

#### 1. Android SDK配置
创建 `local.properties` 文件（参考 `local.properties.example`）：
```properties
sdk.dir=你的Android SDK路径
```

### 可选配置

#### 2. AI功能配置
首次使用AI功能需要配置API密钥：
1. 打开应用，进入AI聊天界面
2. 点击设置图标
3. 选择AI提供商（OpenAI、Gemini、SiliconFlow、ZenMux）
4. 输入API密钥和模型名称
5. 保存配置

#### 3. OCR功能配置
使用OCR功能需要腾讯云API密钥：
1. 打开应用，进入首页
2. 点击OCR配置
3. 输入腾讯云SecretId和SecretKey
4. 保存配置

## 权限说明

应用需要以下权限：
- `INTERNET` - 网络请求和AI功能
- `READ_EXTERNAL_STORAGE` - 读取文件
- `WRITE_EXTERNAL_STORAGE` - 写入文件
- `CAMERA` - OCR拍照识别

## 技术栈

- **语言**: Java
- **UI框架**: Android原生（Material Design）
- **网络库**: OkHttp
- **JSON解析**: Gson
- **Python支持**: Chaquopy
- **SSH客户端**: JSch
- **代码编辑器**: CodeEditor

## 项目结构

```
MovingHacker/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/movinghacker/
│   │   │   ├── ai/                    # AI功能模块
│   │   │   │   ├── handlers/          # Function handlers
│   │   │   │   ├── AIService.java
│   │   │   │   └── ...
│   │   │   ├── MainActivity.java
│   │   │   ├── HomeFragment.java
│   │   │   ├── WebRequestFragment.java
│   │   │   ├── FileManagerFragment.java
│   │   │   ├── TerminalFragment.java
│   │   │   ├── PythonEditorFragment.java
│   │   │   └── ...
│   │   └── res/                       # 资源文件
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties                  # Gradle配置（已提交）
├── local.properties.example           # SDK配置示例
└── README.md
```

## 常见问题

### Q: 构建失败，提示找不到SDK
A: 请确保已创建 `local.properties` 文件并正确配置Android SDK路径。

### Q: Python库安装失败
A: Chaquopy会自动下载Python库，请确保网络连接正常。如果下载失败，可以尝试使用代理或更换网络。

### Q: AI功能无法使用
A: 请确保已在应用内配置正确的API密钥和模型名称。

## 开源协议

本项目采用MIT协议开源。

## 贡献

欢迎提交Issue和Pull Request！

## 免责声明

本工具仅供学习和研究使用，请勿用于非法用途。使用本工具造成的任何后果由使用者自行承担。

