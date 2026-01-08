package com.example.movinghacker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileItem {
    private File file;
    private boolean isSelected;
    private boolean isDirectory;
    private String name;
    private long size;
    private long lastModified;

    public FileItem(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
        this.name = file.getName();
        this.size = file.length();
        this.lastModified = file.lastModified();
        this.isSelected = false;
    }

    public File getFile() {
        return file;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getFormattedSize() {
        if (isDirectory) {
            return "";
        }
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format(Locale.US, "%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastModified));
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getIconResource() {
        if (isDirectory) {
            return android.R.drawable.ic_menu_sort_by_size;
        }
        
        String extension = getFileExtension().toLowerCase();
        switch (extension) {
            case "txt":
            case "log":
            case "md":
                return android.R.drawable.ic_menu_edit;
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return android.R.drawable.ic_menu_gallery;
            case "mp3":
            case "wav":
            case "ogg":
                return android.R.drawable.ic_lock_silent_mode_off;
            case "mp4":
            case "avi":
            case "mkv":
                return android.R.drawable.ic_menu_slideshow;
            case "zip":
            case "rar":
            case "7z":
                return android.R.drawable.ic_menu_save;
            default:
                return android.R.drawable.ic_menu_info_details;
        }
    }

    public String getFileExtension() {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < name.length() - 1) {
            return name.substring(dotIndex + 1);
        }
        return "";
    }
}
