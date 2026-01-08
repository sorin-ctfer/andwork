@echo off
echo ========================================
echo MovingHacker APK 签名密钥生成工具
echo ========================================
echo.

REM 检查 keytool 是否可用
where keytool >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到 keytool 命令
    echo 请确保已安装 JDK 并将其添加到 PATH 环境变量
    echo.
    pause
    exit /b 1
)

echo [1/3] 生成签名密钥...
echo.
echo 请按照提示输入信息：
echo - 密钥库口令：建议使用强密码（至少8位，包含字母和数字）
echo - 名字与姓氏：例如 Sorin
echo - 组织单位：例如 Development
echo - 组织名称：例如 MovingHacker
echo - 城市：例如 Beijing
echo - 省份：例如 Beijing
echo - 国家代码：例如 CN
echo.

keytool -genkey -v -keystore movinghacker-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias movinghacker

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] 密钥生成失败
    pause
    exit /b 1
)

echo.
echo [2/3] 创建 keystore.properties 配置文件...
echo.

set /p STORE_PASSWORD="请输入刚才设置的密钥库口令: "
set /p KEY_PASSWORD="请输入密钥口令（如果与密钥库口令相同，直接回车）: "

if "%KEY_PASSWORD%"=="" set KEY_PASSWORD=%STORE_PASSWORD%

echo storePassword=%STORE_PASSWORD%> keystore.properties
echo keyPassword=%KEY_PASSWORD%>> keystore.properties
echo keyAlias=movinghacker>> keystore.properties
echo storeFile=movinghacker-release.jks>> keystore.properties

echo.
echo [3/3] 配置完成！
echo.
echo ========================================
echo 重要提示：
echo ========================================
echo 1. 密钥文件已生成：movinghacker-release.jks
echo 2. 配置文件已创建：keystore.properties
echo 3. 请妥善保管这两个文件和密码！
echo 4. 这些文件已自动添加到 .gitignore，不会提交到 Git
echo 5. 建议将密钥文件备份到安全位置
echo.
echo 下一步：运行以下命令构建签名 APK
echo    gradlew.bat clean assembleRelease
echo.
pause
