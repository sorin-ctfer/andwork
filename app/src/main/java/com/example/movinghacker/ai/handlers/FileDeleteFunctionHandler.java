package com.example.movinghacker.ai.handlers;

import android.content.Context;
import android.os.Environment;

import com.example.movinghacker.ai.FunctionDefinition;
import com.example.movinghacker.ai.FunctionHandler;
import com.example.movinghacker.ai.FunctionResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件删除Function Handler
 * 删除文件或空目录
 * 默认工作目录：/storage/emulated/0/Editor
 */
public class FileDeleteFunctionHandler implements FunctionHandler {
    
    private final Context context;
    private final Gson gson;
    private final File defaultDir;

    public FileDeleteFunctionHandler(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.defaultDir = new File(Environment.getExternalStorageDirectory(), "Editor");
    }

    @Override
    public String getName() {
        return "delete_file";
    }

    @Override
    public FunctionDefinition getDefinition() {
        FunctionDefinition def = new FunctionDefinition();
        def.setName("delete_file");
        def.setDescription(
            "删除文件或空目录。\n\n" +
            "**默认工作目录**: /storage/emulated/0/Editor\n" +
            "- 相对路径会相对于Editor目录\n" +
            "- 可以删除文件\n" +
            "- 可以删除空目录\n" +
            "- 非空目录需要先清空才能删除\n" +
            "- 删除操作不可恢复，请谨慎使用\n\n" +
            "**安全限制**:\n" +
            "- 建议只删除Editor目录下的文件\n" +
            "- 删除前会确认文件存在\n" +
            "- 删除前会确认有删除权限\n\n" +
            "**使用场景**:\n" +
            "- 清理临时文件\n" +
            "- 删除测试文件\n" +
            "- 清理过期日志\n" +
            "- 删除错误的文件\n" +
            "- 清理空目录"
        );
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // path参数（必需）
        Map<String, Object> path = new HashMap<>();
        path.put("type", "string");
        path.put("description", 
            "要删除的文件或目录路径。\n" +
            "- 相对路径：相对于Editor目录，如'temp.txt'\n" +
            "- 绝对路径：完整路径（需要权限）\n" +
            "- 目录必须为空才能删除\n\n" +
            "示例：\n" +
            "- 'test.txt' → 删除Editor目录下的test.txt\n" +
            "- 'temp/old.log' → 删除temp子目录中的old.log\n" +
            "- 'empty_folder' → 删除空目录"
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
                return FunctionResult.error(null, "文件或目录不存在: " + targetFile.getAbsolutePath());
            }
            
            // 检查是否有删除权限
            if (!targetFile.canWrite()) {
                return FunctionResult.error(null, "没有删除权限: " + targetFile.getAbsolutePath());
            }
            
            // 记录文件信息（删除前）
            String name = targetFile.getName();
            String absolutePath = targetFile.getAbsolutePath();
            boolean wasDirectory = targetFile.isDirectory();
            long size = targetFile.length();
            
            // 如果是目录，检查是否为空
            if (wasDirectory) {
                File[] files = targetFile.listFiles();
                if (files != null && files.length > 0) {
                    return FunctionResult.error(null, 
                        "目录不为空，包含 " + files.length + " 个文件/文件夹。请先清空目录。");
                }
            }
            
            // 删除文件或目录
            boolean deleted = targetFile.delete();
            
            if (!deleted) {
                return FunctionResult.error(null, "删除失败: " + absolutePath);
            }
            
            // 构建结果
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("path", absolutePath);
            result.addProperty("name", name);
            result.addProperty("type", wasDirectory ? "directory" : "file");
            result.addProperty("size", size);
            result.addProperty("sizeFormatted", formatSize(size));
            result.addProperty("message", 
                (wasDirectory ? "目录" : "文件") + " 已删除: " + name);
            result.addProperty("wasInEditorDir", absolutePath.startsWith(defaultDir.getAbsolutePath()));
            
            return FunctionResult.success(null, result.toString());
            
        } catch (Exception e) {
            return FunctionResult.error(null, "删除失败: " + e.getMessage());
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
}
