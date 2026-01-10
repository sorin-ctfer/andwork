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
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件写入Function Handler
 * 创建或覆盖文件内容
 * 默认工作目录：/storage/emulated/0/Editor
 */
public class FileWriteFunctionHandler implements FunctionHandler {
    
    private final Context context;
    private final Gson gson;
    private final File defaultDir;

    public FileWriteFunctionHandler(Context context) {
        this.context = context;
        this.gson = new Gson();
        // 使用应用的外部存储目录，不需要额外权限
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir != null) {
            this.defaultDir = new File(externalFilesDir, "Editor");
        } else {
            // 降级到内部存储
            this.defaultDir = new File(context.getFilesDir(), "Editor");
        }
    }

    @Override
    public String getName() {
        return "write_file";
    }

    @Override
    public FunctionDefinition getDefinition() {
        FunctionDefinition def = new FunctionDefinition();
        def.setName("write_file");
        def.setDescription(
            "创建新文件或覆盖现有文件的内容。\n\n" +
            "**默认工作目录**: 应用私有存储/Editor\n" +
            "- 相对路径会相对于Editor目录\n" +
            "- 如果文件已存在，将被覆盖\n" +
            "- 如果目录不存在，会自动创建\n" +
            "- 支持所有文本格式（.txt, .json, .xml, .py, .java等）\n\n" +
            "**使用场景**:\n" +
            "- 创建配置文件\n" +
            "- 保存代码文件\n" +
            "- 生成JSON/XML数据\n" +
            "- 创建日志文件\n" +
            "- 保存API响应数据\n" +
            "- 创建待上传的文件\n\n" +
            "**与Web请求集成**:\n" +
            "1. 使用write_file创建文件\n" +
            "2. 使用send_http_request上传文件\n" +
            "3. 实现完整的 创建→上传→查看响应 工作流"
        );
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // path参数（必需）
        Map<String, Object> path = new HashMap<>();
        path.put("type", "string");
        path.put("description", 
            "要写入的文件路径。\n" +
            "- 相对路径：相对于Editor目录，如'config.json'\n" +
            "- 绝对路径：完整路径（需要权限）\n" +
            "- 支持子目录：如'projects/app/main.py'\n\n" +
            "示例：\n" +
            "- 'test.txt' → Editor/test.txt\n" +
            "- 'data/config.json' → Editor/data/config.json\n" +
            "- 'projects/myapp/src/main.py' → 创建多级目录"
        );
        properties.put("path", path);
        
        // content参数（必需）
        Map<String, Object> content = new HashMap<>();
        content.put("type", "string");
        content.put("description", 
            "要写入的文件内容。\n" +
            "- 支持任意文本内容\n" +
            "- 支持多行文本（使用\\n换行）\n" +
            "- 支持JSON、XML、代码等格式\n" +
            "- 会覆盖现有文件内容\n\n" +
            "示例：\n" +
            "- JSON: '{\"name\": \"test\", \"value\": 123}'\n" +
            "- 多行文本: 'Line 1\\nLine 2\\nLine 3'\n" +
            "- 代码: 'def hello():\\n    print(\"Hello\")'"
        );
        properties.put("content", content);
        
        parameters.put("properties", properties);
        parameters.put("required", Arrays.asList("path", "content"));
        
        def.setParameters(parameters);
        return def;
    }

    @Override
    public FunctionResult execute(String arguments) {
        try {
            JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
            
            // 解析参数
            String pathStr = args.get("path").getAsString().trim();
            String content = args.get("content").getAsString();
            
            File targetFile;
            if (pathStr.startsWith("/")) {
                // 绝对路径
                targetFile = new File(pathStr);
            } else {
                // 相对路径，相对于Editor目录
                targetFile = new File(defaultDir, pathStr);
            }
            
            // 确保父目录存在
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    return FunctionResult.error(null, "无法创建目录: " + parentDir.getAbsolutePath());
                }
            }
            
            // 检查是否可写
            if (targetFile.exists() && !targetFile.canWrite()) {
                return FunctionResult.error(null, "没有写入权限: " + targetFile.getAbsolutePath());
            }
            
            // 写入文件
            boolean isNewFile = !targetFile.exists();
            try (FileWriter writer = new FileWriter(targetFile, false)) { // false表示覆盖
                writer.write(content);
            }
            
            // 构建结果
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("path", targetFile.getAbsolutePath());
            result.addProperty("name", targetFile.getName());
            result.addProperty("size", targetFile.length());
            result.addProperty("sizeFormatted", formatSize(targetFile.length()));
            result.addProperty("isNewFile", isNewFile);
            result.addProperty("action", isNewFile ? "created" : "overwritten");
            result.addProperty("lineCount", content.split("\n").length);
            result.addProperty("extension", getFileExtension(targetFile.getName()));
            result.addProperty("isInEditorDir", targetFile.getAbsolutePath().startsWith(defaultDir.getAbsolutePath()));
            
            return FunctionResult.success(null, result.toString());
            
        } catch (Exception e) {
            return FunctionResult.error(null, "写入文件失败: " + e.getMessage());
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
