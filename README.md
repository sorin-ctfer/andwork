# MovingHacker - 移动黑客工具集

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![License](https://img.shields.io/badge/license-MIT-orange.svg)
![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)

**一款功能强大的 Android 开发者工具集合**

[下载 APK](https://github.com/sorin-ctfer/andwork/releases) | [功能特性](#功能特性) | [使用指南](#使用指南) | [技术栈](#技术栈)

</div>

---

## 📱 项目简介

MovingHacker 是一款专为开发者和技术爱好者打造的 Android 工具集应用，集成了 Web 请求测试、文件管理、代码编辑、终端模拟、SSH 客户端、Python IDE 等多种实用功能。所有功能均在本地运行，无需服务器支持，保护您的隐私和数据安全。

**版本：** v1.0.0  
**发布日期：** 2026年1月8日  
**开发者：** Sorin  
**项目状态：** ✅ 正式发布

---

## ✨ 功能特性

### 🌐 Web 请求工具
- **HTTP/HTTPS 请求**：支持 GET、POST、PUT、DELETE 等所有 HTTP 方法
- **HTTP/2 支持**：完整支持 HTTP/2 协议
- **自定义请求头**：灵活添加和管理 HTTP Headers
- **多种 Body 类型**：支持 JSON、Form Data、Raw Text、File Upload
- **文件上传**：支持单文件和多文件上传
- **请求历史**：自动保存最近 100 条请求记录
- **响应搜索**：快速搜索响应内容
- **请求重放**：一键重新发送历史请求

### 📝 代码编辑器
- **多语言支持**：支持 20+ 种编程语言语法高亮
  - Java, Python, JavaScript, TypeScript, C/C++, C#, Go, Rust
  - HTML, CSS, XML, JSON, Markdown, SQL
  - Shell, Kotlin, Swift, PHP, Ruby, Lua
- **智能编辑**：基于 Sora Editor 的专业代码编辑体验
- **文件管理**：默认工作目录 `Documents/MovingHacker/`
- **实时保存**：自动保存编辑内容

### 📂 双屏文件管理器
- **双面板设计**：左右双屏同时浏览，提高效率
- **单/双屏切换**：灵活切换单屏或双屏模式
- **拖拽操作**：长按文件拖拽到另一面板直接移动
- **拖拽菜单**：拖拽文件到底部菜单区域打开操作菜单
- **文件搜索**：支持通配符、递归搜索、大小写敏感
- **文件操作**：编辑、重命名、删除、查看信息、图片预览
- **Wget 下载**：直接下载文件到当前目录
- **SSH 远程管理**：连接 SSH 服务器管理远程文件

### 🔐 SSH 客户端
- **SSH 连接管理**：保存和管理多个 SSH 连接
- **双重认证**：支持密码和密钥认证
- **远程终端**：完整的 SSH 终端模拟器
- **远程文件管理**：在文件管理器中直接操作远程文件
  - 浏览远程目录
  - 预览远程文件
  - 下载到本地
  - 删除和重命名
  - 查看文件信息
- **SSH 模式切换**：右下角 FAB 按钮快速切换本地/远程模式

### 💻 Linux 终端
- **原生 Shell**：基于 Android 原生 Shell 环境
- **自定义命令**：实现了常用 Linux 命令
  - `wget`：下载文件
  - `curl`：HTTP 请求工具
  - `strings`：提取文件中的可打印字符
  - `file`：识别文件类型
  - `find`：查找文件
  - `cd`：切换目录（持久化）
- **命令历史**：支持上下键浏览历史命令
- **清屏功能**：`clear` 命令清空终端

### 🐍 Python IDE
- **Python 3.11**：集成 Chaquopy Python 运行时
- **代码编辑**：专业的 Python 代码编辑器
- **即时执行**：直接运行 Python 代码
- **输出显示**：实时显示执行结果和错误信息
- **预装库**：内置 requests、numpy 等常用库
- **双架构支持**：支持 ARM64 和 x86_64 架构

---

## 🎯 核心亮点

### 🔒 隐私优先
- ✅ 所有数据本地存储
- ✅ 无需注册登录
- ✅ 不依赖云服务器
- ✅ 不收集用户数据

### ⚡ 高效便捷
- ✅ 模块化设计，按需使用
- ✅ 双屏文件管理，效率翻倍
- ✅ 拖拽操作，直观快捷
- ✅ SSH 集成，远程管理无缝衔接

### 🎨 现代设计
- ✅ Material Design 3 设计语言
- ✅ 简洁优雅的白灰配色
- ✅ 流畅的动画效果
- ✅ 响应式布局

---

## 📥 下载安装

### 系统要求
- **Android 版本**：Android 14 (API 34) 及以上
- **架构支持**：ARM64-v8a, x86_64
- **存储空间**：至少 100 MB 可用空间
- **权限需求**：
  - 存储读写权限（文件管理）
  - 网络权限（Web 请求、SSH）

### 安装步骤

1. **下载 APK**
   ```
   前往 Releases 页面下载最新版本：
   app-py311-release-unsigned.apk (约 69.8 MB)
   ```

2. **启用未知来源**
   - 打开设置 → 安全 → 允许安装未知来源应用
   - 或在安装时根据提示允许

3. **安装应用**
   - 点击下载的 APK 文件
   - 按照提示完成安装

4. **授予权限**
   - 首次启动时授予必要的存储和网络权限

---

## 🚀 快速开始

### 1. Web 请求测试
1. 点击主页的"Web 请求"模块
2. 输入 URL 和选择请求方法
3. 添加 Headers 和 Body（可选）
4. 点击"发送"查看响应
5. 在"历史"标签查看请求记录

### 2. 文件管理
1. 点击"双屏文件管理"模块
2. 左右两个面板独立浏览目录
3. 长按文件拖拽到另一面板移动文件
4. 拖拽到底部菜单区域打开操作菜单
5. 点击右下角 SSH 按钮连接远程服务器

### 3. SSH 远程管理
1. 在文件管理器中点击 SSH 按钮
2. 添加 SSH 连接（主机、端口、用户名、密码/密钥）
3. 选择连接，自动切换到远程模式
4. 右侧面板显示远程文件系统
5. 点击 SSH 按钮切换本地/远程模式

### 4. Python 编程
1. 点击"Python IDE"模块
2. 在编辑器中编写 Python 代码
3. 点击"运行"按钮执行
4. 查看下方输出区域的结果

### 5. 终端操作
1. 点击"Linux 终端"模块
2. 输入命令并回车执行
3. 使用 `cd` 切换目录
4. 使用 `wget` 下载文件
5. 使用 `clear` 清空屏幕

---

## 🛠️ 技术栈

### 核心框架
- **Android SDK 34** - 目标 Android 14
- **Java 11** - 主要开发语言
- **Material Design 3** - UI 设计规范

### 主要依赖库
- **OkHttp 4.12.0** - HTTP 客户端
- **JSch 0.1.55** - SSH 客户端
- **Gson 2.10.1** - JSON 序列化
- **Room 2.6.1** - 本地数据库
- **Sora Editor 0.23.4** - 代码编辑器
- **Chaquopy 15.0.1** - Python 集成
- **AndroidX Lifecycle** - MVVM 架构

### 架构设计
- **MVVM 架构**：ViewModel + LiveData
- **Fragment 导航**：模块化页面管理
- **Repository 模式**：数据访问层抽象
- **异步处理**：ExecutorService 线程池

---

## 📖 使用指南

### Web 请求高级功能

#### 添加自定义 Header
```
1. 点击"添加 Header"按钮
2. 输入 Header 名称和值
3. 点击确定添加
4. 可添加多个 Header
```

#### 文件上传
```
1. 选择 Body 类型为"文件"
2. 点击"选择文件"按钮
3. 从文件管理器选择文件
4. 支持多文件上传
```

#### 搜索响应内容
```
1. 发送请求后切换到"响应"标签
2. 点击搜索图标
3. 输入关键词搜索
4. 高亮显示匹配结果
```

### 文件管理器技巧

#### 拖拽移动文件
```
1. 长按文件开始拖拽
2. 拖拽到另一面板释放
3. 文件自动移动到目标目录
```

#### 拖拽打开菜单
```
1. 长按文件开始拖拽
2. 拖拽到底部菜单区域
3. 释放后打开文件操作菜单
4. 选择重命名、删除等操作
```

#### 文件搜索
```
1. 点击搜索图标
2. 输入文件名（支持 * 通配符）
3. 选择是否递归搜索
4. 选择是否区分大小写
```

### SSH 远程管理

#### 添加 SSH 连接
```
1. 点击文件管理器右下角 SSH 按钮
2. 点击"添加连接"
3. 填写连接信息：
   - 名称：连接显示名称
   - 主机：服务器 IP 或域名
   - 端口：SSH 端口（默认 22）
   - 用户名：SSH 用户名
   - 密码：SSH 密码（或留空使用密钥）
4. 点击"保存"
```

#### 使用 SSH 连接
```
1. 在 SSH 连接列表中点击连接
2. 等待连接建立
3. 右侧面板自动切换到远程模式
4. 浏览远程文件系统
5. 点击 SSH 按钮切换本地/远程
```

#### 远程文件操作
```
- 短按文件：预览文件内容
- 长按拖拽到菜单：打开操作菜单
  - 下载到本地
  - 删除远程文件
  - 重命名文件
  - 查看文件信息
```

---

## 🔧 开发构建

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高
- JDK 11
- Gradle 8.9
- Android SDK 34

### 克隆项目
```bash
git clone https://github.com/sorin-ctfer/andwork.git
cd andwork
```

### 构建 APK
```bash
# Windows
.\gradlew.bat assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

### 输出位置
```
app/build/outputs/apk/py311/release/app-py311-release-unsigned.apk
```

---

## 📝 更新日志

### v1.0.0 (2026-01-08)

#### 🎉 首次发布
- ✅ Web 请求工具完整实现
- ✅ 双屏文件管理器
- ✅ SSH 客户端和远程文件管理
- ✅ Linux 终端模拟器
- ✅ Python 3.11 IDE
- ✅ 代码编辑器（20+ 语言）
- ✅ 拖拽操作支持
- ✅ 请求历史管理
- ✅ 文件搜索功能

#### 🐛 已知问题
- 未签名 APK 可能在某些设备上需要额外权限
- Python 3.11 包生态相对较少（建议使用 3.8）
- 大文件上传可能需要较长时间

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交 Issue
- 描述问题或建议
- 提供复现步骤
- 附上设备信息和日志

### 提交 PR
- Fork 项目
- 创建特性分支
- 提交清晰的 commit 信息
- 确保代码通过测试

---

## 📄 开源协议

本项目采用 MIT 协议开源，详见 [LICENSE](LICENSE) 文件。

---

## 📧 联系方式

- **GitHub**: [@sorin-ctfer](https://github.com/sorin-ctfer)
- **项目地址**: [https://github.com/sorin-ctfer/andwork](https://github.com/sorin-ctfer/andwork)
- **Issue 反馈**: [https://github.com/sorin-ctfer/andwork/issues](https://github.com/sorin-ctfer/andwork/issues)

---

## 🙏 致谢

感谢以下开源项目：
- [OkHttp](https://square.github.io/okhttp/) - HTTP 客户端
- [JSch](http://www.jcraft.com/jsch/) - SSH 实现
- [Sora Editor](https://github.com/Rosemoe/sora-editor) - 代码编辑器
- [Chaquopy](https://chaquo.com/chaquopy/) - Python 集成
- [Material Design](https://material.io/) - 设计规范

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个 Star！⭐**

Made with ❤️ by Sorin

</div>

---
