package com.example.movinghacker.ai.handlers;

import android.content.Context;

import com.example.movinghacker.HttpRequest;
import com.example.movinghacker.HttpRequestExecutor;
import com.example.movinghacker.HttpResponse;
import com.example.movinghacker.RequestHeader;
import com.example.movinghacker.ai.FunctionDefinition;
import com.example.movinghacker.ai.FunctionHandler;
import com.example.movinghacker.ai.FunctionResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web请求Function Handler
 * 提供高级HTTP请求功能，支持：
 * - 自定义请求方法（GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS）
 * - 自定义请求头
 * - 自定义请求体（支持JSON、文本等）
 * - 文件上传（与文件管理模块集成）
 * - 完整的响应信息（状态码、响应头、响应体、耗时等）
 */
public class WebRequestFunctionHandler implements FunctionHandler {
    
    private final Context context;
    private final HttpRequestExecutor executor;
    private final Gson gson;

    public WebRequestFunctionHandler(Context context) {
        this.context = context;
        this.executor = new HttpRequestExecutor();
        this.executor.setContext(context);
        this.gson = new Gson();
    }

    @Override
    public String getName() {
        return "web_request";
    }

    @Override
    public FunctionDefinition getDefinition() {
        FunctionDefinition def = new FunctionDefinition();
        def.setName("web_request");
        def.setDescription(
            "发送高级HTTP请求。这是一个功能强大的HTTP客户端，支持：\n" +
            "1. 所有HTTP方法（GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS）\n" +
            "2. 自定义请求头（headers）- 可以设置任意请求头，如Authorization、Content-Type等\n" +
            "3. 自定义请求体（body）- 支持JSON、XML、文本等任意格式\n" +
            "4. 文件上传（filePath）- 可以上传文件，支持multipart/form-data\n" +
            "5. 完整响应信息 - 返回状态码、响应头、响应体、请求耗时、数据大小、协议版本等\n\n" +
            "使用场景：\n" +
            "- API测试和调试\n" +
            "- 数据抓取和爬虫\n" +
            "- 与第三方服务集成\n" +
            "- 文件上传和下载\n" +
            "- Webhook调用\n\n" +
            "与文件管理模块集成：\n" +
            "- 可以先使用文件管理功能创建/编辑文件\n" +
            "- 然后使用此功能上传文件\n" +
            "- 实现完整的 编写→上传→查看响应 工作流"
        );
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // url参数
        Map<String, Object> url = new HashMap<>();
        url.put("type", "string");
        url.put("description", "请求URL，必须是完整的URL（包含http://或https://）");
        properties.put("url", url);
        
        // method参数
        Map<String, Object> method = new HashMap<>();
        method.put("type", "string");
        method.put("enum", Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"));
        method.put("description", 
            "HTTP请求方法：\n" +
            "- GET: 获取资源\n" +
            "- POST: 创建资源或提交数据\n" +
            "- PUT: 更新资源（完整更新）\n" +
            "- PATCH: 更新资源（部分更新）\n" +
            "- DELETE: 删除资源\n" +
            "- HEAD: 获取响应头（不返回body）\n" +
            "- OPTIONS: 查询支持的方法"
        );
        properties.put("method", method);
        
        // headers参数（可选）
        Map<String, Object> headers = new HashMap<>();
        headers.put("type", "object");
        headers.put("description", 
            "自定义请求头（键值对对象）。常用请求头：\n" +
            "- Content-Type: 指定请求体格式（如application/json）\n" +
            "- Authorization: 认证信息（如Bearer token）\n" +
            "- User-Agent: 客户端标识\n" +
            "- Accept: 期望的响应格式\n" +
            "- Cookie: Cookie信息\n" +
            "示例：{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer xxx\"}"
        );
        properties.put("headers", headers);
        
        // body参数（可选）
        Map<String, Object> body = new HashMap<>();
        body.put("type", "string");
        body.put("description", 
            "请求体内容。支持任意格式：\n" +
            "- JSON: {\"key\": \"value\"}\n" +
            "- XML: <root><item>value</item></root>\n" +
            "- 表单: key1=value1&key2=value2\n" +
            "- 纯文本: 任意文本内容\n" +
            "注意：如果提供了filePath，body将作为额外的表单数据"
        );
        properties.put("body", body);
        
        // filePath参数（可选）
        Map<String, Object> filePath = new HashMap<>();
        filePath.put("type", "string");
        filePath.put("description", 
            "要上传的文件路径（绝对路径）。\n" +
            "使用场景：\n" +
            "1. 先使用文件管理功能创建/编辑文件\n" +
            "2. 获取文件的完整路径\n" +
            "3. 使用此参数上传文件\n" +
            "文件将以multipart/form-data格式上传，字段名为'file'"
        );
        properties.put("filePath", filePath);
        
        parameters.put("properties", properties);
        parameters.put("required", Arrays.asList("url", "method"));
        
        def.setParameters(parameters);
        return def;
    }

    @Override
    public FunctionResult execute(String arguments) {
        try {
            JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
            
            // 解析参数
            String url = args.get("url").getAsString();
            String method = args.get("method").getAsString().toUpperCase();
            
            // 解析请求头
            List<RequestHeader> headers = new ArrayList<>();
            if (args.has("headers") && !args.get("headers").isJsonNull()) {
                JsonObject headersObj = args.getAsJsonObject("headers");
                for (String key : headersObj.keySet()) {
                    String value = headersObj.get(key).getAsString();
                    headers.add(new RequestHeader(key, value));
                }
            }
            
            // 解析请求体
            String body = null;
            if (args.has("body") && !args.get("body").isJsonNull()) {
                body = args.get("body").getAsString();
            }
            
            // 解析文件路径
            android.net.Uri fileUri = null;
            if (args.has("filePath") && !args.get("filePath").isJsonNull()) {
                String filePath = args.get("filePath").getAsString();
                java.io.File file = new java.io.File(filePath);
                if (file.exists()) {
                    fileUri = android.net.Uri.fromFile(file);
                } else {
                    return FunctionResult.error(null, "文件不存在: " + filePath);
                }
            }
            
            // 创建请求
            HttpRequest request = new HttpRequest(url, method, headers, body, fileUri);
            
            // 执行请求
            HttpResponse response = executor.execute(request);
            
            // 构建结果
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("statusCode", response.getStatusCode());
            result.addProperty("statusMessage", response.getStatusMessage());
            result.addProperty("protocol", response.getProtocol());
            result.addProperty("duration", response.getDuration() + "ms");
            result.addProperty("size", formatSize(response.getSize()));
            
            // 添加响应头
            JsonObject responseHeaders = new JsonObject();
            for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
                responseHeaders.addProperty(entry.getKey(), entry.getValue());
            }
            result.add("headers", responseHeaders);
            
            // 添加响应体
            String responseBody = response.getBody();
            if (responseBody != null && !responseBody.isEmpty()) {
                // 尝试解析为JSON
                try {
                    if (responseBody.trim().startsWith("{") || responseBody.trim().startsWith("[")) {
                        result.add("body", JsonParser.parseString(responseBody));
                    } else {
                        result.addProperty("body", responseBody);
                    }
                } catch (Exception e) {
                    result.addProperty("body", responseBody);
                }
            } else {
                result.addProperty("body", "");
            }
            
            // 添加请求摘要
            JsonObject requestSummary = new JsonObject();
            requestSummary.addProperty("method", method);
            requestSummary.addProperty("url", url);
            requestSummary.addProperty("hasBody", body != null && !body.isEmpty());
            requestSummary.addProperty("hasFile", fileUri != null);
            requestSummary.addProperty("headerCount", headers.size());
            result.add("request", requestSummary);
            
            return FunctionResult.success(null, result.toString());
            
        } catch (Exception e) {
            return FunctionResult.error(null, "HTTP请求失败: " + e.getMessage());
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
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
