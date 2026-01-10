package com.example.movinghacker.ai.handlers;

import android.content.Context;
import android.os.Environment;

import com.example.movinghacker.ai.FunctionDefinition;
import com.example.movinghacker.ai.FunctionHandler;
import com.example.movinghacker.ai.FunctionResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 文件列表Function Handler
 * 列出指定目录下的所有文件和文件夹
 * 默认工作目录：/storage/emulated/0/Editor
 */
public class FileListFunctionHandler implements FunctionHandler {
    
    private final Context context;
    private final Gson gson;
    private final File defaultDir;

    public FileListFunctionHandler(Context context) {
        this.context = context;
        this.gson = new Gson();
        // 默认工作目录：Editor文件夹
        this.defaultDir = new File(Environment.getExternalStorageDirectory(), "Editor");
        // 确保Editor目录存在
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
    }

    @Override
    public String getName() {
        return "list_files";
    }

    @Override
    public FunctionDefinition getDefinition() {
        FunctionDefinition def = new FunctionDefinition();
        def.setName("list_files");
        def.setDescription(
            "列出指定目录下的所有文件和文件夹。\n\n" +
            "**默认工作目录**: /storage/emulated/0/Editor\n" +
            "- 如果不指定path参数，将列出Editor目录的内容\n" +
            "- Editor目录是AI的默认工作空间\n" +
            "- 建议所有文件操作都在Editor目录下进行\n\n" +
            "**返回信息**:\n" +
            "- 文件/文件夹名称\n" +
            "- 类型（文件/目录）\n" +
            "- 大小（文件）\n" +
            "- 修改时间\n" +
            "- 完整路径\n" +
            "- 文件扩展名\n\n" +
            "**使用场景**:\n" +
            "- 查看Editor目录下有哪些文件\n" +
            "- 浏览特定目录的内容\n" +
            "- 在文件操作前确认文件是否存在\n" +
            "- 查找特定文件"
        );
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // path参数（可选）
        Map<String, Object> path = new HashMap<>();
        path.put("type", "string");
        path.put("description", 
            "要列出的目录路径。\n" +
            "- 不指定：列出Editor目录（推荐）\n" +
            "- 相对路径：相对于Editor目录，如'projects/test'\n" +
            "- 绝对路径：完整路径，如'/storage/emulated/0/Documents'\n" +
            "- 特殊值'.'：当前Editor目录\n" +
            "- 特殊值'..'：Editor的父目录\n\n" +
            "示例：\n" +
            "- 不指定或'.' → /storage/emulated/0/Editor\n" +
            "- 'projects' → /storage/emulated/0/Editor/projects\n" +
            "- '/storage/emulated/0/Download' → 下载目录"
        );
        properties.put("path", path);
        
        parameters.put("properties", properties);
        parameters.put("required", Arrays.asList()); // path是可选的
        
        def.setParameters(parameters);
        return def;
    }

    @Override
    public FunctionResult execute(String arguments) {
        try {
            JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
            
            // 解析路径参数
            File targetDir;
            if (args.has("path") && !args.get("path").isJsonNull()) {
                String pathStr = args.get("path").getAsString().trim();
                
                if (pathStr.isEmpty() || pathStr.equals(".")) {
                    // 空或"."表示Editor目录
                    targetDir = defaultDir;
                } else if (pathStr.equals("..")) {
                    // ".."表示Editor的父目录
                    targetDir = defaultDir.getParentFile();
                } else if (pathStr.startsWith("/")) {
                    // 绝对路径
                    targetDir = new File(pathStr);
                } else {
                    // 相对路径，相对于Editor目录
                    targetDir = new File(defaultDir, pathStr);
                }
            } else {
                // 未指定路径，使用默认Editor目录
                targetDir = defaultDir;
            }
            
            // 检查目录是否存在
            if (!targetDir.exists()) {
                return FunctionResult.error(null, "目录不存在: " + targetDir.getAbsolutePath());
            }
            
            if (!targetDir.isDirectory()) {
                return FunctionResult.error(null, "不是一个目录: " + targetDir.getAbsolutePath());
            }
            
            if (!targetDir.canRead()) {
                return FunctionResult.error(null, "没有读取权限: " + targetDir.getAbsolutePath());
            }
            
            // 列出目录内容
            File[] files = targetDir.listFiles();
            if (files == null) {
                return FunctionResult.error(null, "无法读取目录内容: " + targetDir.getAbsolutePath());
            }
            
            // 构建结果
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("path", targetDir.getAbsolutePath());
            result.addProperty("isDefaultDir", targetDir.equals(defaultDir));
            result.addProperty("totalCount", files.length);
            
            // 统计文件和目录数量
            int fileCount = 0;
            int dirCount = 0;
            for (File file : files) {
                if (file.isDirectory()) {
                    dirCount++;
                } else {
                    fileCount++;
                }
            }
            result.addProperty("fileCount", fileCount);
            result.addProperty("dirCount", dirCount);
            
            // 添加文件列表
            JsonArray filesArray = new JsonArray();
            
            // 排序：目录在前，文件在后，按名称排序
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                } else if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                } else {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            for (File file : files) {
                JsonObject fileObj = new JsonObject();
                fileObj.addProperty("name", file.getName());
                fileObj.addProperty("type", file.isDirectory() ? "directory" : "file");
                fileObj.addProperty("path", file.getAbsolutePath());
                
                if (file.isFile()) {
                    fileObj.addProperty("size", file.length());
                    fileObj.addProperty("sizeFormatted", formatSize(file.length()));
                    fileObj.addProperty("extension", getFileExtension(file.getName()));
                }
                
                fileObj.addProperty("lastModified", file.lastModified());
                fileObj.addProperty("lastModifiedFormatted", sdf.format(new Date(file.lastModified())));
                fileObj.addProperty("canRead", file.canRead());
                fileObj.addProperty("canWrite", file.canWrite());
                
                filesArray.add(fileObj);
            }
            
            result.add("files", filesArray);
            
            return FunctionResult.success(null, result.toString());
            
        } catch (Exception e) {
            return FunctionResult.error(null, "列出文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
}
