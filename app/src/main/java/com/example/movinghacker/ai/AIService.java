package com.example.movinghacker.ai;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AI服务层
 * 处理AI对话和Function调用的业务逻辑
 */
public class AIService {
    
    private final Context context;
    private final AIConfigManager configManager;
    private final FunctionRegistry functionRegistry;
    private AIApiClient apiClient;

    public static class StopToken {
        private volatile boolean stopped;

        public void stop() {
            this.stopped = true;
        }

        public boolean isStopped() {
            return stopped;
        }
    }

    public AIService(Context context) {
        this.context = context;
        this.configManager = AIConfigManager.getInstance(context);
        this.functionRegistry = FunctionRegistry.getInstance(context);
        
        AIConfig config = configManager.getConfig();
        if (config != null) {
            this.apiClient = new AIApiClient(config);
        }
    }

    /**
     * 发送消息并处理AI响应
     * @param messages 消息历史
     * @return AI响应
     */
    public AIResponse chat(List<ChatMessage> messages) throws IOException {
        return chat(messages, null, true);
    }

    private AIResponse chat(List<ChatMessage> messages, List<ChatMessage> extraMessages, boolean enableFunctions) throws IOException {
        ensureApiClient();
        
        // 添加系统提示词
        AIConfig config = configManager.getConfig();
        String systemPrompt = SystemPrompt.getSystemPrompt(config.isHackingMode());

        String memorySummary = extractLatestSummary(messages);
        List<ChatMessage> messagesWithSystem = new ArrayList<>();
        messagesWithSystem.add(new ChatMessage("system", systemPrompt));
        if (memorySummary != null && !memorySummary.trim().isEmpty()) {
            messagesWithSystem.add(new ChatMessage("system", "Conversation memory:\n" + memorySummary.trim()));
        }
        messagesWithSystem.addAll(filterConversationMessages(messages));
        if (extraMessages != null && !extraMessages.isEmpty()) {
            messagesWithSystem.addAll(filterConversationMessages(extraMessages));
        }
        
        // 获取Function定义（仅Hacking模式）
        List<FunctionDefinition> functions = null;
        if (enableFunctions && config.isHackingMode()) {
            functions = functionRegistry.getAllDefinitions();
        }
        
        // 调用AI API
        return apiClient.chat(messagesWithSystem, functions);
    }

    /**
     * 执行Function调用
     * @param functionCall Function调用信息
     * @return Function执行结果
     */
    public FunctionResult executeFunction(FunctionCall functionCall) {
        return functionRegistry.execute(functionCall);
    }

    /**
     * 执行多个Function调用
     * @param functionCalls Function调用列表
     * @return Function执行结果列表
     */
    public List<FunctionResult> executeFunctions(List<FunctionCall> functionCalls) {
        return executeFunctions(functionCalls, null);
    }

    public List<FunctionResult> executeFunctions(List<FunctionCall> functionCalls, StopToken stopToken) {
        List<FunctionResult> results = new ArrayList<>();
        for (FunctionCall call : functionCalls) {
            if (stopToken != null && stopToken.isStopped()) {
                results.add(FunctionResult.error(call != null ? call.getId() : null, "用户已停止"));
                break;
            }
            FunctionResult result = executeFunction(call);
            if (result == null) {
                result = FunctionResult.error(call != null ? call.getId() : null, "Function returned null result");
            }
            results.add(result);
            if (!result.isSuccess()) {
                break;
            }
        }
        return results;
    }

    /**
     * 处理完整的对话流程（包括Function调用）
     * @param messages 消息历史
     * @param callback 回调接口
     */
    public void chatWithFunctionHandling(List<ChatMessage> messages, ChatCallback callback) {
        chatWithFunctionHandling(messages, new StopToken(), callback);
    }

    public Thread chatWithFunctionHandling(List<ChatMessage> messages, StopToken stopToken, ChatCallback callback) {
        Thread thread = new Thread(() -> {
            try {
                if (stopToken != null && stopToken.isStopped()) {
                    callback.onError(new IOException("用户已停止"));
                    return;
                }

                List<ChatMessage> toolHistory = new ArrayList<>();
                String lastCallsSignature = null;
                int maxRounds = 6;
                AIResponse response = null;
                for (int round = 0; round < maxRounds; round++) {
                    if (stopToken != null && stopToken.isStopped()) {
                        callback.onError(new IOException("用户已停止"));
                        return;
                    }

                    if (round == 0) {
                        callback.onThinking("正在分析您的问题...", "analysis");
                    } else {
                        callback.onThinking("继续处理并尝试完成任务...", "analysis");
                    }

                    response = chat(messages, toolHistory, true);
                    if (response == null || !response.hasFunctionCalls()) {
                        break;
                    }

                    List<FunctionCall> calls = response.getFunctionCalls();
                    String callsSignature = buildCallsSignature(calls);
                    if (callsSignature != null && callsSignature.equals(lastCallsSignature)) {
                        callback.onError(new IOException("检测到重复请求相同功能调用，已停止以避免Token消耗"));
                        return;
                    }
                    lastCallsSignature = callsSignature;

                    StringBuilder thinkingMsg = new StringBuilder("准备调用以下功能：\n");
                    for (FunctionCall call : calls) {
                        thinkingMsg.append("• ").append(getFunctionDisplayName(call.getName())).append("\n");
                    }
                    callback.onThinking(thinkingMsg.toString().trim(), "function_call");
                    callback.onFunctionCallsRequested(calls);

                    // 将function call添加到主消息列表和工具历史
                    for (FunctionCall call : calls) {
                        ChatMessage assistantCall = new ChatMessage("assistant", "");
                        List<FunctionCall> one = new ArrayList<>();
                        one.add(call);
                        assistantCall.setFunctionCalls(one);
                        toolHistory.add(assistantCall);
                        messages.add(assistantCall);
                    }

                    List<FunctionResult> results = executeFunctions(calls, stopToken);
                    for (int i = 0; i < results.size(); i++) {
                        FunctionResult result = results.get(i);
                        if (result != null && !result.isSuccess()) {
                            FunctionCall failedCall = i < calls.size() ? calls.get(i) : null;
                            String functionName = failedCall != null ? failedCall.getName() : "unknown";
                            String error = result.getError() != null ? result.getError() : "unknown error";
                            callback.onError(new IOException("功能执行失败：" + getFunctionDisplayName(functionName) + " - " + error));
                            return;
                        }
                    }

                    StringBuilder executionMsg = new StringBuilder("功能执行完成：\n");
                    for (int i = 0; i < calls.size(); i++) {
                        FunctionCall call = calls.get(i);
                        FunctionResult result = i < results.size() ? results.get(i) : null;
                        executionMsg.append("• ").append(getFunctionDisplayName(call.getName()));
                        if (result != null && result.isSuccess()) {
                            executionMsg.append(" ✓\n");
                        } else {
                            executionMsg.append(" ✗\n");
                        }
                    }
                    callback.onThinking(executionMsg.toString().trim(), "execution");
                    callback.onFunctionExecuted(results);

                    for (int i = 0; i < calls.size() && i < results.size(); i++) {
                        FunctionCall call = calls.get(i);
                        FunctionResult result = results.get(i);
                        ChatMessage functionMessage = new ChatMessage("function", result.getResult() != null ? result.getResult() : "");
                        functionMessage.setFunctionName(call.getName());
                        functionMessage.setToolCallId(call.getId());
                        messages.add(functionMessage);
                    }

                    callback.onThinking("正在整理结果并生成回复...", "code_generation");
                }

                if (response != null && response.hasFunctionCalls() && !response.hasContent()) {
                    toolHistory.add(new ChatMessage("user", "请基于已有结果给出最终答复，不要再调用任何功能。"));
                    response = chat(messages, toolHistory, false);
                }

                // 返回最终响应
                if (response != null) {
                    callback.onSuccess(response);
                } else {
                    callback.onError(new IOException("AI未返回有效响应"));
                    return;
                }
                
            } catch (Exception e) {
                callback.onError(e);
            }
        });
        thread.start();
        return thread;
    }

    private String buildCallsSignature(List<FunctionCall> calls) {
        if (calls == null || calls.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < calls.size(); i++) {
            FunctionCall call = calls.get(i);
            if (call == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(call.getName() != null ? call.getName() : "");
            sb.append("|");
            sb.append(call.getArguments() != null ? call.getArguments().trim() : "");
        }
        return sb.toString();
    }

    private List<ChatMessage> filterConversationMessages(List<ChatMessage> messages) {
        List<ChatMessage> filtered = new ArrayList<>();
        if (messages == null) {
            return filtered;
        }
        for (ChatMessage msg : messages) {
            if (msg == null) {
                continue;
            }
            if (msg.isSystem() || msg.isThinking() || msg.isSummary()) {
                continue;
            }
            filtered.add(msg);
        }
        return filtered;
    }

    private String extractLatestSummary(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg != null && msg.isSummary()) {
                return msg.getContent();
            }
        }
        return null;
    }

    /**
     * 生成上下文总结
     */
    private void generateContextSummary(List<ChatMessage> messages, ChatCallback callback) {
        try {
            // 构建总结请求
            List<ChatMessage> summaryMessages = new ArrayList<>();
            summaryMessages.add(new ChatMessage("system", 
                "你是一个专业的对话总结助手。请用最简洁的语言（不超过100字）总结这次对话的关键内容：\n" +
                "1. 用户的主要需求或问题\n" +
                "2. 执行了哪些操作\n" +
                "3. 当前进度或结果\n" +
                "格式：简洁的要点列表，每点不超过20字。"));
            
            // 只取最近的几条消息用于总结
            int startIndex = Math.max(0, messages.size() - 6);
            for (int i = startIndex; i < messages.size(); i++) {
                ChatMessage msg = messages.get(i);
                if (!msg.isSystem() && !msg.isThinking() && !msg.isSummary()) {
                    summaryMessages.add(msg);
                }
            }
            
            summaryMessages.add(new ChatMessage("user", "请总结上述对话"));

            AIConfig config = configManager.getConfig();
            if (config != null && "gemini".equals(config.getProvider())) {
                JsonObject schema = new JsonObject();
                schema.addProperty("type", "object");
                JsonObject properties = new JsonObject();
                JsonObject itemsSchema = new JsonObject();
                itemsSchema.addProperty("type", "array");
                JsonObject itemSchema = new JsonObject();
                itemSchema.addProperty("type", "string");
                itemsSchema.add("items", itemSchema);
                properties.add("items", itemsSchema);
                schema.add("properties", properties);
                JsonArray required = new JsonArray();
                required.add("items");
                schema.add("required", required);

                AIResponse summaryResponse = apiClient.chatStructuredJson(summaryMessages, schema);
                if (summaryResponse.hasContent()) {
                    String formatted = formatSummaryJson(summaryResponse.getContent());
                    if (formatted != null && !formatted.trim().isEmpty()) {
                        callback.onContextSummary(formatted);
                        return;
                    }
                }
            }

            AIResponse summaryResponse = apiClient.chat(summaryMessages, null);
            if (summaryResponse.hasContent()) {
                callback.onContextSummary(summaryResponse.getContent());
            }
            
        } catch (Exception e) {
            // 总结失败不影响主流程
            e.printStackTrace();
        }
    }

    private String formatSummaryJson(String jsonText) {
        try {
            JSONObject obj = new JSONObject(jsonText);
            JSONArray arr = obj.optJSONArray("items");
            if (arr == null || arr.length() == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                String line = arr.optString(i, "").trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append("• ").append(line);
            }
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * 获取Function的显示名称
     */
    private String getFunctionDisplayName(String functionName) {
        switch (functionName) {
            case "web_request": return "Web请求";
            case "list_files": return "文件列表";
            case "read_file": return "读取文件";
            case "write_file": return "写入文件";
            case "delete_file": return "删除文件";
            case "search_files": return "搜索文件";
            case "python_execute": return "Python执行";
            case "terminal_execute": return "终端命令";
            default: return functionName;
        }
    }

    /**
     * 更新API配置
     */
    public void updateConfig(AIConfig config) {
        configManager.saveConfig(config);
        this.apiClient = new AIApiClient(config);
    }

    /**
     * 确保API客户端已初始化
     */
    private void ensureApiClient() throws IOException {
        if (apiClient == null) {
            AIConfig config = configManager.getConfig();
            if (config == null) {
                throw new IOException("AI API not configured");
            }
            apiClient = new AIApiClient(config);
        }
    }

    /**
     * 聊天回调接口
     */
    public interface ChatCallback {
        void onSuccess(AIResponse response);
        void onError(Exception e);
        void onFunctionCallsRequested(List<FunctionCall> calls);
        void onFunctionExecuted(List<FunctionResult> results);
        void onThinking(String message, String thinkingType);
        void onContextSummary(String summary);
    }
}
