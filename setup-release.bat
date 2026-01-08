@echo off
chcp 65001 >nul
echo ========================================
echo MovingHacker 发布版配置工具
echo ========================================
echo.
echo 此脚本将帮助你：
echo 1. 生成签名密钥
echo 2. 转换应用图标
echo 3. 构建签名 APK
echo.
pause

REM 步骤 1: 检查密钥是否已存在
echo.
echo [步骤 1/4] 检查签名密钥...
if exist movinghacker-release.jks (
    echo ✓ 密钥文件已存在: movinghacker-release.jks
    echo.
    set /p REGENERATE="是否重新生成密钥？(y/N): "
    if /i "%REGENERATE%"=="y" (
        echo 正在重新生成密钥...
        call generate-keystore.bat
    ) else (
        echo 跳过密钥生成
    )
) else (
    echo ✗ 未找到密钥文件
    echo 开始生成密钥...
    call generate-keystore.bat
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo [错误] 密钥生成失败
        pause
        exit /b 1
    )
)

REM 步骤 2: 检查 Python 和 Pillow
echo.
echo [步骤 2/4] 检查 Python 环境...
where python >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [警告] 未找到 Python
    echo 跳过图标生成步骤
    echo 你可以稍后手动运行: python generate-icons.py
    goto :skip_icons
)

python -c "import PIL" >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [警告] 未安装 Pillow 库
    echo 正在尝试安装...
    python -m pip install Pillow
    if %ERRORLEVEL% NEQ 0 (
        echo [警告] Pillow 安装失败
        echo 跳过图标生成步骤
        goto :skip_icons
    )
)

REM 步骤 3: 生成图标
echo.
echo [步骤 3/4] 生成应用图标...
if exist favicon.ico (
    python generate-icons.py
    if %ERRORLEVEL% NEQ 0 (
        echo [警告] 图标生成失败
        echo 将使用默认图标
    )
) else (
    echo [警告] 未找到 favicon.ico 文件
    echo 将使用默认图标
)

:skip_icons

REM 步骤 4: 构建签名 APK
echo.
echo [步骤 4/4] 构建签名 APK...
echo.
set /p BUILD="是否立即构建签名 APK？(Y/n): "
if /i "%BUILD%"=="n" (
    echo 跳过构建步骤
    echo.
    echo 你可以稍后手动运行: gradlew.bat clean assembleRelease
    goto :end
)

echo 正在清理旧的构建文件...
call gradlew.bat clean

echo.
echo 正在构建签名 APK...
call gradlew.bat assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] APK 构建失败
    echo 请检查错误信息
    pause
    exit /b 1
)

echo.
echo ========================================
echo 构建完成！
echo ========================================
echo.
echo APK 文件位置：
echo   app\build\outputs\apk\py311\release\app-py311-release.apk
echo.
echo 下一步：
echo 1. 将 APK 文件复制到 Android 设备
echo 2. 在设备上安装 APK
echo 3. 检查应用图标和签名是否正确
echo.

:end
pause
