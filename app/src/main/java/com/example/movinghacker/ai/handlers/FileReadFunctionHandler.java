package com.example.movinghacker.ai.handlers;

import android.content.Context;
import android.os.Environment;

import com.example.movinghacker.ai.FunctionDefinition;
import com.example.movinghacker.ai.FunctionHandler;
import com.example.movinghacker.ai.FunctionResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件读取Function Handler
 * 读取文件内容
 * 默认工作目录：/storage/emulated/0/Editor
 */
public class FileReadFunctionHandler implements FunctionHandler {
    
    private final Context context;
    private final Gson gson;
    private final File defaultDir;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB限制

    public FileReadFunctionHandler(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.defaultDir = new File(Environment.getExternalStorageDirectory(), "Editor");
    }

    @Override
    public String getName() {
        return "read_file";
    }

    @Override
    public FunctionDefinition getDefinition() {
        FunctionDefinition def = new FunctionDefinition();
        def.setName("read_file");
        def.setDescription(
            "读取文件内容。\n\n" +
            "**默认工作目录**: /storage/emulated/0/Editor\n" +
            "- 相对路径会相对于Editor目录\n" +
            "- 支持读取文本文件（.txt, .json, .xml, .md, .log等）\n" +
            "- 文件大小限制：10MB\n\n" +
            "**返回信息**:\n" +
            "- 文件内容（完整文本）\n" +
            "- 文件大小\n" +
            "- 文件路径\n" +
            "- 行数\n" +
            "- 编码信息\n\n" +
            "**使用场景**:\n" +
            "- 查看配置文件内容\n" +
            "- 读取日志文件\n" +
            "- 检查代码文件\n" +
            "- 读取JSON/XML数据\n" +
            "- 在修改前查看文件内容"
        );
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // path参数（必需）
        Map<String, Object> path = new HashMap<>();
        path.put("type", "string");
        path.put("description", 
            "要读取的文件路径。\n" +
            "- 相对路径：相对于Editor目录，如'config.json'\n" +
            "- 绝对路径：完整路径，如'/storage/emulated/0/Editor/test.txt'\n\n" +
            "示例：\n" +
            "- 'config.json' → /storage/emulated/0/Editor/config.json\n" +
            "- 'projects/app/main.py' → /storage/emulated/0/Editor/projects/app/main.py\n" +
            "- '/storage/emulated/0/Download/data.txt' → 下载目录中的文件"
        );
        properties.put("path", path);
        
        parameters.put("properties", properties);
        parameters.put("required", Arrays.asList("path"));
        
        def.setParameters(parameters);
        return def;
    }

    @Override
    public FunctionResult execute(String arguments) {
        try {
            JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
            
            // 解析路径参数
            String pathStr = args.get("path").getAsString().trim();
            
            File targetFile;
            if (pathStr.startsWith("/")) {
                // 绝对路径
                targetFile = new File(pathStr);
            } else {
                // 相对路径，相对于Editor目录
                targetFile = new File(defaultDir, pathStr);
            }
            
            // 检查文件是否存在
            if (!targetFile.exists()) {
                return FunctionResult.error(null, "文件不存在: " + targetFile.getAbsolutePath());
            }
            
            if (!targetFile.isFile()) {
                return FunctionResult.error(null, "不是一个文件: " + targetFile.getAbsolutePath());
            }
            
            if (!targetFile.canRead()) {
                return FunctionResult.error(null, "没有读取权限: " + targetFile.getAbsolutePath());
            }
            
            // 检查文件大小
            long fileSize = targetFile.length();
            if (fileSize > MAX_FILE_SIZE) {
                return FunctionResult.error(null, 
                    String.format("文件太大（%.2f MB），超过10MB限制", fileSize / (1024.0 * 1024)));
            }
            
            // 读取文件内容
            StringBuilder content = new StringBuilder();
            int lineCount = 0;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(targetFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (lineCount > 0) {
                        content.append("\n");
                    }
                    content.append(line);
                    lineCount++;
                }
            }
            
            // 构建结果
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("path", targetFile.getAbsolutePath());
            result.addProperty("name", targetFile.getName());
            result.addProperty("size", fileSize);
            result.addProperty("sizeFormatted", formatSize(fileSize));
            result.addProperty("lineCount", lineCount);
            result.addProperty("content", content.toString());
            result.addProperty("extension", getFileExtension(targetFile.getName()));
            result.addProperty("isInEditorDir", targetFile.getAbsolutePath().startsWith(defaultDir.getAbsolutePath()));
            
            return FunctionResult.success(null, result.toString());
            
        } catch (Exception e) {
            return FunctionResult.error(null, "读取文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
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
