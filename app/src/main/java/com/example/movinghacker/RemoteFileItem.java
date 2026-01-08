package com.example.movinghacker;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 远程文件项 - 表示SSH服务器上的文件
 */
public class RemoteFileItem {
    private String name;
    private String path;
    private boolean isDirectory;
    private long size;
    private long modifiedTime;
    private String permissions;
    
    public RemoteFileItem(String name, String path, boolean isDirectory) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.size = 0;
        this.modifiedTime = System.currentTimeMillis();
        this.permissions = "";
    }
    
    public String getName() {
        return name;
    }
    
    public String getPath() {
        return path;
    }
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public long getModifiedTime() {
        return modifiedTime;
    }
    
    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
    
    public String getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
    
    public String getFormattedSize() {
        if (isDirectory) return "-";
        
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date(modifiedTime));
    }
    
    public int getIconResource() {
        if (isDirectory) {
            return android.R.drawable.ic_menu_view;
        } else {
            String ext = "";
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                ext = name.substring(lastDot + 1).toLowerCase();
            }
            
            // 根据扩展名返回不同图标
            switch (ext) {
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                case "bmp":
                    return android.R.drawable.ic_menu_gallery;
                case "txt":
                case "log":
                case "md":
                    return android.R.drawable.ic_menu_edit;
                case "zip":
                case "rar":
                case "tar":
                case "gz":
                    return android.R.drawable.ic_menu_save;
                default:
                    return android.R.drawable.ic_menu_info_details;
            }
        }
    }
    
    /**
     * 将FileItem转换为RemoteFileItem（用于适配器）
     */
    public FileItem toFileItem() {
        // 创建一个虚拟的File对象用于显示
        java.io.File dummyFile = new java.io.File(path);
        FileItem item = new FileItem(dummyFile) {
            @Override
            public String getName() {
                return name;
            }
            
            @Override
            public boolean isDirectory() {
                return RemoteFileItem.this.isDirectory;
            }
            
            @Override
            public String getFormattedSize() {
                return RemoteFileItem.this.getFormattedSize();
            }
            
            @Override
            public String getFormattedDate() {
                return RemoteFileItem.this.getFormattedDate();
            }
            
            @Override
            public int getIconResource() {
                return RemoteFileItem.this.getIconResource();
            }
        };
        return item;
    }
}
