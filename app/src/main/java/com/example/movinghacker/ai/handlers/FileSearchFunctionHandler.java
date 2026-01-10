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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 文件搜索Function Handler
 * 在指定目录中搜索文件
 * 默认工作目录：/storage/emulated/0/Editor
 */
public class FileSearchFunctionHandler implements FunctionHandler {
    
    private final Context context;
    private final Gson gson;
    private final File defaultDir;
    private static final int MAX_RESULTS = 100; // 最多返回100个结果

    public FileSearchFunctionHandler(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.defaultDir = new File(Environment.getExternalStorageDirectory(), "Editor");
    }

    @Override
    public String getName() {
        return "search_files";
    }

    @Override
    public FunctionDefinition getDefinition() {
        FunctionDefinition def = new FunctionDefinition();
        def.setName("search_files");
        def.setDescription(
            "在指定目录中搜索文件。\n\n" +
            "**默认工作目录**: /storage/emulated/0/Editor\n" +
            "- 默认在Editor目录中搜索\n" +
            "- 支持递归搜索子目录\n" +
            "- 支持文件名模糊匹配\n" +
            "- 支持按扩展名过滤\n" +
            "- 最多返回100个结果\n\n" +
            "**搜索功能**:\n" +
            "- 文件名包含关键词\n" +
            "- 按扩展名筛选\n" +
            "- 递归搜索所有子目录\n" +
            "- 返回完整路径和文件信息\n\n" +
            "**使用场景**:\n" +
            "- 查找特定文件\n" +
            "- 查找所有某类型文件（如.json, .txt）\n" +
            "- 查找包含关键词的文件\n" +
            "- 在上传前查找文件位置"
        );
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // query参数（必需）
        Map<String, Object> query = new HashMap<>();
        query.put("type", "string");
        query.put("description", 
            "搜索关键词（文件名）。\n" +
            "- 不区分大小写\n" +
            "- 支持部分匹配\n" +
            "- 可以是文件名的任意部分\n\n" +
            "示例：\n" +
            "- 'config' → 匹配config.json, myconfig.txt等\n" +
            "- 'test' → 匹配test.py, unittest.java等\n" +
            "- '.json' → 匹配所有JSON文件"
        );
        properties.put("query", query);
        
        // path参数（可选）
        Map<String, Object> path = new HashMap<>();
        path.put("type", "string");
        path.put("description", 
            "搜索的起始目录。\n" +
            "- 不指定：在Editor目录中搜索\n" +
            "- 相对路径：相对于Editor目录\n" +
            "- 绝对路径：完整路径\n\n" +
            "示例：\n" +
            "- 不指定 → /storage/emulated/0/Editor\n" +
            "- 'projects' → /storage/emulated/0/Editor/projects\n" +
            "- '/storage/emulated/0/Download' → 下载目录"
        );
        properties.put("path", path);
        
        // extension参数（可选）
        Map<String, Object> extension = new HashMap<>();
        extension.put("type", "string");
        extension.put("description", 
            "按文件扩展名过滤（不含点号）。\n" +
            "- 只返回指定扩展名的文件\n" +
            "- 不区分大小写\n\n" +
            "示例：\n" +
            "- 'json' → 只返回.json文件\n" +
            "- 'txt' → 只返回.txt文件\n" +
            "- 'py' → 只返回.py文件"
        );
        properties.put("extension", extension);
        
        parameters.put("properties", properties);
        parameters.put("required", Arrays.asList("query"));
        
        def.setParameters(parameters);
        return def;
    }

    @Override
    public FunctionResult execute(String arguments) {
        try {
            JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
            
            // 解析参数
            String query = args.get("query").getAsString().trim().toLowerCase();
            
            // 解析搜索目录
            File searchDir;
            if (args.has("path") && !args.get("path").isJsonNull()) {
                String pathStr = args.get("path").getAsString().trim();
                if (pathStr.startsWith("/")) {
                    searchDir = new File(pathStr);
                } else {
                    searchDir = new File(defaultDir, pathStr);
                }
            } else {
                searchDir = defaultDir;
            }
            
            // 解析扩展名过滤
            String extensionFilter = null;
            if (args.has("extension") && !args.get("extension").isJsonNull()) {
                extensionFilter = args.get("extension").getAsString().trim().toLowerCase();
            }
            
            // 检查搜索目录
            if (!searchDir.exists()) {
                return FunctionResult.error(null, "搜索目录不存在: " + searchDir.getAbsolutePath());
            }
            
            if (!searchDir.isDirectory()) {
                return FunctionResult.error(null, "不是一个目录: " + searchDir.getAbsolutePath());
            }
            
            if (!searchDir.canRead()) {
                return FunctionResult.error(null, "没有读取权限: " + searchDir.getAbsolutePath());
            }
            
            // 执行搜索
            List<File> results = new ArrayList<>();
            searchFiles(searchDir, query, extensionFilter, results);
            
            // 构建结果
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("query", query);
            result.addProperty("searchPath", searchDir.getAbsolutePath());
            result.addProperty("totalFound", results.size());
            result.addProperty("maxResults", MAX_RESULTS);
            
            if (extensionFilter != null) {
                result.addProperty("extensionFilter", extensionFilter);
            }
            
            // 添加搜索结果
            JsonArray filesArray = new JsonArray();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            int count = Math.min(results.size(), MAX_RESULTS);
            for (int i = 0; i < count; i++) {
                File file = results.get(i);
                JsonObject fileObj = new JsonObject();
                fileObj.addProperty("name", file.getName());
                fileObj.addProperty("path", file.getAbsolutePath());
                fileObj.addProperty("type", file.isDirectory() ? "directory" : "file");
                
                if (file.isFile()) {
                    fileObj.addProperty("size", file.length());
                    fileObj.addProperty("sizeFormatted", formatSize(file.length()));
                    fileObj.addProperty("extension", getFileExtension(file.getName()));
                }
                
                fileObj.addProperty("lastModified", file.lastModified());
                fileObj.addProperty("lastModifiedFormatted", sdf.format(new Date(file.lastModified())));
                fileObj.addProperty("relativePath", getRelativePath(searchDir, file));
                
                filesArray.add(fileObj);
            }
            
            result.add("files", filesArray);
            
            if (results.size() > MAX_RESULTS) {
                result.addProperty("message", 
                    String.format("找到%d个结果，只显示前%d个", results.size(), MAX_RESULTS));
            }
            
            return FunctionResult.success(null, result.toString());
            
        } catch (Exception e) {
            return FunctionResult.error(null, "搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 递归搜索文件
     */
    private void searchFiles(File dir, String query, String extensionFilter, List<File> results) {
        if (results.size() >= MAX_RESULTS) {
            return; // 已达到最大结果数
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (results.size() >= MAX_RESULTS) {
                break;
            }
            
            if (file.isDirectory()) {
                // 递归搜索子目录
                searchFiles(file, query, extensionFilter, results);
            } else {
                // 检查文件名是否匹配
                String fileName = file.getName().toLowerCase();
                boolean nameMatches = fileName.contains(query);
                
                // 检查扩展名是否匹配
                boolean extensionMatches = true;
                if (extensionFilter != null) {
                    String fileExtension = getFileExtension(fileName);
                    extensionMatches = fileExtension.equalsIgnoreCase(extensionFilter);
                }
                
                if (nameMatches && extensionMatches) {
                    results.add(file);
                }
            }
        }
    }
    
    /**
     * 获取相对路径
     */
    private String getRelativePath(File base, File file) {
        String basePath = base.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        
        if (filePath.startsWith(basePath)) {
            String relative = filePath.substring(basePath.length());
            if (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            return relative;
        }
        
        return filePath;
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
