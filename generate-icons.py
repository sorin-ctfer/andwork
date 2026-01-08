#!/usr/bin/env python3
"""
MovingHacker 应用图标生成工具
将 favicon.ico 转换为 Android 所需的各种尺寸图标
"""

import os
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("错误: 未安装 Pillow 库")
    print("请运行: pip install Pillow")
    sys.exit(1)

# Android 图标尺寸配置
ICON_SIZES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

def convert_ico_to_png(ico_path, output_dir, sizes):
    """
    将 ICO 文件转换为不同尺寸的 PNG 图标
    
    Args:
        ico_path: ICO 文件路径
        output_dir: 输出目录
        sizes: 尺寸字典 {目录名: 尺寸}
    """
    print(f"正在读取图标文件: {ico_path}")
    
    try:
        # 打开 ICO 文件
        with Image.open(ico_path) as img:
            # 转换为 RGBA 模式
            if img.mode != 'RGBA':
                img = img.convert('RGBA')
            
            print(f"原始图标尺寸: {img.size}")
            
            # 为每个尺寸生成图标
            for folder, size in sizes.items():
                # 创建输出目录
                output_folder = Path(output_dir) / folder
                output_folder.mkdir(parents=True, exist_ok=True)
                
                # 调整图标大小
                resized = img.resize((size, size), Image.Resampling.LANCZOS)
                
                # 保存为 PNG
                output_path = output_folder / 'ic_launcher.png'
                resized.save(output_path, 'PNG')
                print(f"✓ 已生成: {output_path} ({size}x{size})")
                
                # 同时生成圆形图标（相同内容）
                output_path_round = output_folder / 'ic_launcher_round.png'
                resized.save(output_path_round, 'PNG')
                print(f"✓ 已生成: {output_path_round} ({size}x{size})")
            
            print("\n所有图标生成完成！")
            return True
            
    except Exception as e:
        print(f"错误: {e}")
        return False

def backup_existing_icons(res_dir):
    """备份现有图标"""
    backup_dir = Path('icon_backup')
    backup_dir.mkdir(exist_ok=True)
    
    print("正在备份现有图标...")
    for folder in ICON_SIZES.keys():
        src_folder = Path(res_dir) / folder
        if src_folder.exists():
            dst_folder = backup_dir / folder
            dst_folder.mkdir(parents=True, exist_ok=True)
            
            for icon_file in ['ic_launcher.webp', 'ic_launcher_round.webp', 
                             'ic_launcher.png', 'ic_launcher_round.png']:
                src_file = src_folder / icon_file
                if src_file.exists():
                    dst_file = dst_folder / icon_file
                    import shutil
                    shutil.copy2(src_file, dst_file)
                    print(f"  备份: {src_file} -> {dst_file}")
    
    print("备份完成！\n")

def main():
    print("=" * 60)
    print("MovingHacker 应用图标生成工具")
    print("=" * 60)
    print()
    
    # 检查 favicon.ico 是否存在
    ico_path = 'favicon.ico'
    if not os.path.exists(ico_path):
        print(f"错误: 未找到 {ico_path} 文件")
        print("请确保 favicon.ico 文件在项目根目录")
        sys.exit(1)
    
    # 资源目录
    res_dir = 'app/src/main/res'
    if not os.path.exists(res_dir):
        print(f"错误: 未找到资源目录 {res_dir}")
        sys.exit(1)
    
    # 备份现有图标
    backup_existing_icons(res_dir)
    
    # 生成新图标
    print("开始生成图标...")
    print()
    
    success = convert_ico_to_png(ico_path, res_dir, ICON_SIZES)
    
    if success:
        print()
        print("=" * 60)
        print("图标生成成功！")
        print("=" * 60)
        print()
        print("下一步：")
        print("1. 检查生成的图标是否正确")
        print("2. 运行 gradlew.bat clean assembleRelease 重新构建 APK")
        print("3. 安装新的 APK 查看图标效果")
        print()
        print("注意：旧图标已备份到 icon_backup 目录")
    else:
        print()
        print("图标生成失败，请检查错误信息")
        sys.exit(1)

if __name__ == '__main__':
    main()
