package com.example.movinghacker.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI API客户端
 * 支持OpenAI和Gemini格式的API调用
 */
public class AIApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 60;

    private final OkHttpClient httpClient;
    private final Gson gson;
    private AIConfig config;

    public AIApiClient(AIConfig config) {
        this.config = config;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发送聊天请求
     */
    public AIResponse chat(List<ChatMessage> messages, List<FunctionDefinition> functions)
            throws IOException {
        if ("gemini".equals(config.getProvider())) {
            return chatGemini(messages, functions);
        } else {
            return chatOpenAI(messages, functions);
        }
    }

    public AIResponse chatStructuredJson(List<ChatMessage> messages, JsonObject responseSchema) throws IOException {
        if (!"gemini".equals(config.getProvider())) {
            return chat(messages, null);
        }

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        generationConfig.add("responseSchema", responseSchema);

        JsonObject requestJson = buildGeminiRequest(messages, null, generationConfig);
        String responseBody = sendRequest(requestJson);
        return parseGeminiResponse(responseBody);
    }

    /**
     * OpenAI格式聊天
     */
    private AIResponse chatOpenAI(List<ChatMessage> messages, List<FunctionDefinition> functions)
            throws IOException {
        JsonObject requestJson = buildOpenAIRequest(messages, functions);
        String responseBody = sendRequest(requestJson);
        return parseOpenAIResponse(responseBody);
    }

    /**
     * Gemini格式聊天
     */
    private AIResponse chatGemini(List<ChatMessage> messages, List<FunctionDefinition> functions)
            throws IOException {
        JsonObject requestJson = buildGeminiRequest(messages, functions, null);
        String responseBody = sendRequest(requestJson);
        return parseGeminiResponse(responseBody);
    }

    /**
     * 构建OpenAI请求JSON
     */
    private JsonObject buildOpenAIRequest(List<ChatMessage> messages,
                                          List<FunctionDefinition> functions) {
        JsonObject request = new JsonObject();
        request.addProperty("model", config.getModel());
        request.addProperty("max_tokens", 4096);
        if ("siliconflow".equals(config.getProvider())) {
            request.addProperty("enable_thinking", true);
        }

        // 添加消息
        JsonArray messagesArray = new JsonArray();
        for (ChatMessage message : messages) {
            if (message.isThinking() || message.isSummary()) {
                continue;
            }
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", message.getRole());
            
            if ("assistant".equals(message.getRole())
                    && message.getFunctionCalls() != null
                    && !message.getFunctionCalls().isEmpty()) {
                // 使用tool_calls格式（新格式，ZenMux和现代OpenAI API）
                msgObj.addProperty("content", "");
                JsonArray toolCallsArray = new JsonArray();
                for (FunctionCall call : message.getFunctionCalls()) {
                    JsonObject toolCallObj = new JsonObject();
                    toolCallObj.addProperty("id", call.getId() != null ? call.getId() : "call_" + System.currentTimeMillis());
                    toolCallObj.addProperty("type", "function");
                    JsonObject functionObj = new JsonObject();
                    functionObj.addProperty("name", call.getName());
                    functionObj.addProperty("arguments", call.getArguments() != null ? call.getArguments() : "{}");
                    toolCallObj.add("function", functionObj);
                    toolCallsArray.add(toolCallObj);
                }
                msgObj.add("tool_calls", toolCallsArray);
            } else if ("function".equals(message.getRole()) && message.getFunctionName() != null && !message.getFunctionName().isEmpty()) {
                // function角色改为tool角色（新格式）
                msgObj.addProperty("role", "tool");
                msgObj.addProperty("content", message.getContent());
                // 使用消息中保存的tool_call_id，如果没有则生成一个
                String toolCallId = message.getToolCallId();
                if (toolCallId == null || toolCallId.isEmpty()) {
                    toolCallId = "call_" + System.currentTimeMillis();
                }
                msgObj.addProperty("tool_call_id", toolCallId);
            } else {
                msgObj.addProperty("content", message.getContent());
            }
            
            messagesArray.add(msgObj);
        }
        request.add("messages", messagesArray);

        // 添加Tool定义（新格式）
        if (functions != null && !functions.isEmpty()) {
            JsonArray toolsArray = new JsonArray();
            for (FunctionDefinition func : functions) {
                JsonObject toolObj = new JsonObject();
                toolObj.addProperty("type", "function");
                toolObj.add("function", gson.toJsonTree(func));
                toolsArray.add(toolObj);
            }
            request.add("tools", toolsArray);
            request.addProperty("tool_choice", "auto");
        }

        return request;
    }

    /**
     * 构建Gemini请求JSON
     */
    private JsonObject buildGeminiRequest(List<ChatMessage> messages,
                                          List<FunctionDefinition> functions,
                                          JsonObject generationConfig) {
        JsonObject request = new JsonObject();

        // Gemini使用contents数组
        JsonArray contentsArray = new JsonArray();
        String systemText = null;
        for (ChatMessage message : messages) {
            // 跳过thinking和summary消息
            if (message.isThinking() || message.isSummary()) {
                continue;
            }
            if ("system".equals(message.getRole())) {
                systemText = message.getContent();
                continue;
            }

            JsonObject content = new JsonObject();
            content.addProperty("role", convertRoleForGemini(message.getRole()));

            JsonArray partsArray = new JsonArray();
            if ("assistant".equals(message.getRole())
                    && message.getFunctionCalls() != null
                    && !message.getFunctionCalls().isEmpty()) {
                for (FunctionCall call : message.getFunctionCalls()) {
                    JsonObject functionCall = new JsonObject();
                    functionCall.addProperty("name", call.getName());
                    JsonObject argsObj;
                    try {
                        argsObj = gson.fromJson(call.getArguments(), JsonObject.class);
                    } catch (Exception ignored) {
                        argsObj = null;
                    }
                    if (argsObj == null) {
                        argsObj = new JsonObject();
                    }
                    functionCall.add("args", argsObj);

                    JsonObject part = new JsonObject();
                    part.add("functionCall", functionCall);
                    partsArray.add(part);
                }
            } else if ("function".equals(message.getRole()) && message.getFunctionName() != null && !message.getFunctionName().isEmpty()) {
                JsonObject functionResponse = new JsonObject();
                functionResponse.addProperty("name", message.getFunctionName());
                JsonObject response = new JsonObject();
                response.addProperty("result", message.getContent() != null ? message.getContent() : "");
                functionResponse.add("response", response);

                JsonObject part = new JsonObject();
                part.add("functionResponse", functionResponse);
                partsArray.add(part);
                content.addProperty("role", "user");
            } else {
                JsonObject part = new JsonObject();
                part.addProperty("text", message.getContent());
                partsArray.add(part);
            }
            content.add("parts", partsArray);

            contentsArray.add(content);
        }
        request.add("contents", contentsArray);

        if (systemText != null && !systemText.isEmpty()) {
            JsonObject systemInstruction = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", systemText);
            parts.add(part);
            systemInstruction.add("parts", parts);
            request.add("systemInstruction", systemInstruction);
        }

        if (generationConfig != null) {
            request.add("generationConfig", generationConfig);
        }

        // 添加Function声明
        if (functions != null && !functions.isEmpty()) {
            JsonArray toolsArray = new JsonArray();
            JsonObject toolObj = new JsonObject();
            JsonArray functionDeclarations = new JsonArray();

            for (FunctionDefinition func : functions) {
                JsonObject funcDecl = new JsonObject();
                funcDecl.addProperty("name", func.getName());
                funcDecl.addProperty("description", func.getDescription());

                // 构建完整的parameters JSON Schema
                JsonObject parameters = buildParametersSchema(func.getParameters());
                funcDecl.add("parameters", parameters);

                functionDeclarations.add(funcDecl);
            }

            toolObj.add("functionDeclarations", functionDeclarations);
            toolsArray.add(toolObj);
            request.add("tools", toolsArray);

            JsonObject toolConfig = new JsonObject();
            JsonObject functionCallingConfig = new JsonObject();
            functionCallingConfig.addProperty("mode", "AUTO");
            toolConfig.add("functionCallingConfig", functionCallingConfig);
            request.add("toolConfig", toolConfig);
        }

        return request;
    }

    /**
     * 构建符合JSON Schema的parameters对象
     */
    private JsonObject buildParametersSchema(Map<String, Object> params) {
        JsonObject schema = new JsonObject();

        if (params == null || params.isEmpty()) {
            schema.addProperty("type", "object");
            schema.add("properties", new JsonObject());
            return schema;
        }

        // 如果已经包含type字段，说明是完整的schema
        if (params.containsKey("type")) {
            return gson.toJsonTree(params).getAsJsonObject();
        }

        // 否则，构建完整的schema
        schema.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("required".equals(key) && value instanceof List) {
                // 处理required数组
                for (Object item : (List<?>) value) {
                    required.add(item.toString());
                }
            } else if (value instanceof Map) {
                // 处理属性定义
                properties.add(key, gson.toJsonTree(value));
            }
        }

        schema.add("properties", properties);
        if (required.size() > 0) {
            schema.add("required", required);
        }

        return schema;
    }

    /**
     * 转换角色名称为Gemini格式
     */
    private String convertRoleForGemini(String role) {
        if ("assistant".equals(role)) {
            return "model";
        }
        return "user"; // system和user都映射为user
    }

    /**
     * 发送HTTP请求（带重试机制）
     */
    private String sendRequest(JsonObject requestJson) throws IOException {
        int maxRetries = 3;
        int retryDelay = 1000; // 初始延迟1秒

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return sendRequestOnce(requestJson);
            } catch (IOException e) {
                // 检查是否是429错误
                if (e.getMessage() != null && e.getMessage().contains("请求过于频繁")) {
                    if (attempt < maxRetries - 1) {
                        // 指数退避：等待后重试
                        try {
                            Thread.sleep(retryDelay);
                            retryDelay *= 2; // 下次等待时间翻倍
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                        continue;
                    }
                }
                // 其他错误或最后一次重试失败，直接抛出
                throw e;
            }
        }

        throw new IOException("请求失败：已达到最大重试次数");
    }

    /**
     * 执行单次HTTP请求
     */
    private String sendRequestOnce(JsonObject requestJson) throws IOException {
        String jsonString = gson.toJson(requestJson);
        RequestBody body = RequestBody.create(jsonString, JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(config.getApiUrl())
                .addHeader("Content-Type", "application/json")
                .post(body);

        // 根据provider设置认证header
        if ("gemini".equals(config.getProvider())) {
            // Gemini使用x-goog-api-key header
            requestBuilder.addHeader("x-goog-api-key", config.getApiKey());
        } else if ("zenmux".equals(config.getProvider())) {
            // Zenmux使用x-api-key header
            requestBuilder.addHeader("x-api-key", config.getApiKey());
        } else {
            // OpenAI使用Authorization Bearer token
            requestBuilder.addHeader("Authorization", "Bearer " + config.getApiKey());
        }

        Request request = requestBuilder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                String errorMessage = parseErrorMessage(response.code(), errorBody);
                throw new IOException(errorMessage);
            }

            return response.body().string();
        }
    }

    /**
     * 解析错误信息
     */
    private String parseErrorMessage(int code, String errorBody) {
        switch (code) {
            case 401:
                return "认证失败：请检查API Key是否正确";
            case 403:
                return "权限不足：API Key可能没有访问权限";
            case 404:
                return "模型不存在：请检查模型名称是否正确";
            case 429:
                return "请求过于频繁：请稍后再试";
            case 500:
            case 502:
            case 503:
                return "服务器错误：请稍后再试";
            default:
                // 尝试解析错误详情
                try {
                    JsonObject error = gson.fromJson(errorBody, JsonObject.class);
                    if (error.has("error")) {
                        JsonObject errorObj = error.getAsJsonObject("error");
                        if (errorObj.has("message")) {
                            return "API错误：" + errorObj.get("message").getAsString();
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
                return String.format("请求失败 (HTTP %d)：%s", code, errorBody);
        }
    }

    /**
     * 解析OpenAI响应
     */
    private AIResponse parseOpenAIResponse(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json == null) {
                throw new RuntimeException("API returned null response");
            }
            
            if (!json.has("choices") || json.get("choices").isJsonNull()) {
                throw new RuntimeException("No choices in API response");
            }
            
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new RuntimeException("Empty choices array in API response");
            }

            JsonObject choice = choices.get(0).getAsJsonObject();
            if (choice == null || !choice.has("message")) {
                throw new RuntimeException("Invalid choice format in API response");
            }
            
            JsonObject message = choice.getAsJsonObject("message");
            if (message == null) {
                throw new RuntimeException("Message is null in API response");
            }
            
            String finishReason = choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull() 
                ? choice.get("finish_reason").getAsString() 
                : "stop";

            AIResponse aiResponse = new AIResponse();
            aiResponse.setFinishReason(finishReason);

            // 检查是否有tool_calls（新格式，ZenMux和现代OpenAI API）
            if (message.has("tool_calls") && !message.get("tool_calls").isJsonNull()) {
                JsonArray toolCalls = message.getAsJsonArray("tool_calls");
                for (int i = 0; i < toolCalls.size(); i++) {
                    JsonObject toolCall = toolCalls.get(i).getAsJsonObject();
                    if (toolCall.has("type") && "function".equals(toolCall.get("type").getAsString())) {
                        String id = toolCall.has("id") ? toolCall.get("id").getAsString() : null;
                        JsonObject function = toolCall.getAsJsonObject("function");
                        String functionName = function.get("name").getAsString();
                        String arguments = function.get("arguments").getAsString();
                        
                        FunctionCall call = new FunctionCall(id, functionName, arguments);
                        aiResponse.addFunctionCall(call);
                    }
                }
            }
            // 兼容旧格式function_call
            else if (message.has("function_call") && !message.get("function_call").isJsonNull()) {
                JsonObject functionCall = message.getAsJsonObject("function_call");
                String functionName = functionCall.get("name").getAsString();
                String arguments = functionCall.get("arguments").getAsString();

                FunctionCall call = new FunctionCall(null, functionName, arguments);
                aiResponse.addFunctionCall(call);
            }
            
            // 获取文本内容
            if (message.has("content") && !message.get("content").isJsonNull()) {
                String content = message.get("content").getAsString();
                if (content != null && !content.isEmpty()) {
                    aiResponse.setContent(content);
                }
            }

            return aiResponse;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage() + "\nResponse: " + responseBody, e);
        }
    }

    /**
     * 解析Gemini响应
     */
    private AIResponse parseGeminiResponse(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json == null) {
                throw new RuntimeException("API returned null response");
            }
            
            if (!json.has("candidates") || json.get("candidates").isJsonNull()) {
                throw new RuntimeException("No candidates in API response");
            }
            
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                throw new RuntimeException("Empty candidates array in API response");
            }

            JsonObject candidate = candidates.get(0).getAsJsonObject();
            if (candidate == null || !candidate.has("content")) {
                throw new RuntimeException("Invalid candidate format in API response");
            }
            
            JsonObject content = candidate.getAsJsonObject("content");
            if (content == null || !content.has("parts")) {
                throw new RuntimeException("Content or parts is null in API response");
            }
            
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null) {
                throw new RuntimeException("Parts array is null in API response");
            }

            AIResponse aiResponse = new AIResponse();

            // 获取finishReason
            if (candidate.has("finishReason") && !candidate.get("finishReason").isJsonNull()) {
                aiResponse.setFinishReason(candidate.get("finishReason").getAsString());
            }

            StringBuilder contentBuilder = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                JsonObject part = parts.get(i).getAsJsonObject();
                if (part == null) {
                    continue;
                }

                // 检查是否有Function调用
                if (part.has("functionCall") && !part.get("functionCall").isJsonNull()) {
                    JsonObject functionCall = part.getAsJsonObject("functionCall");
                    if (functionCall != null && functionCall.has("name")) {
                        String functionName = functionCall.get("name").getAsString();
                        JsonObject args = functionCall.has("args") ? functionCall.getAsJsonObject("args") : new JsonObject();
                        String arguments = gson.toJson(args);

                        FunctionCall call = new FunctionCall(null, functionName, arguments);
                        aiResponse.addFunctionCall(call);
                    }
                } else if (part.has("text") && !part.get("text").isJsonNull()) {
                    String text = part.get("text").getAsString();
                    if (text != null && !text.isEmpty()) {
                        if (contentBuilder.length() > 0) {
                            contentBuilder.append("\n");
                        }
                        contentBuilder.append(text);
                    }
                }
            }

            if (contentBuilder.length() > 0) {
                aiResponse.setContent(contentBuilder.toString());
            }

            return aiResponse;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage() + "\nResponse: " + responseBody, e);
        }
    }

    /**
     * 更新配置
     */
    public void updateConfig(AIConfig config) {
        this.config = config;
    }
}
