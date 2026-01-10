package com.example.movinghacker.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 聊天消息数据模型
 */
public class ChatMessage {
    private String id;
    private String role;  // "user", "assistant", "system", "function", "tool"
    private String content;
    private long timestamp;
    private List<FunctionCall> functionCalls;
    private boolean isError;
    private String functionName;  // For function role messages
    private String toolCallId;    // For tool role messages (new format)
    private boolean isThinking;  // 是否是思考过程消息
    private boolean isSummary;   // 是否是上下文总结消息
    private String thinkingType; // 思考类型: "function_call", "code_generation", "analysis"

    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.functionCalls = new ArrayList<>();
        this.isError = false;
        this.isThinking = false;
        this.isSummary = false;
    }

    public ChatMessage(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }

    public static ChatMessage userMessage(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistantMessage(String content) {
        return new ChatMessage("assistant", content);
    }

    public static ChatMessage systemMessage(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage errorMessage(String content) {
        ChatMessage message = new ChatMessage("assistant", content);
        message.setError(true);
        return message;
    }

    public static ChatMessage thinkingMessage(String content, String thinkingType) {
        ChatMessage message = new ChatMessage("assistant", content);
        message.setThinking(true);
        message.setThinkingType(thinkingType);
        return message;
    }

    public static ChatMessage summaryMessage(String content) {
        ChatMessage message = new ChatMessage("assistant", content);
        message.setSummary(true);
        return message;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<FunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    public void setFunctionCalls(List<FunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public boolean isUser() {
        return "user".equals(role);
    }

    public boolean isAssistant() {
        return "assistant".equals(role);
    }

    public boolean isSystem() {
        return "system".equals(role);
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public boolean isThinking() {
        return isThinking;
    }

    public void setThinking(boolean thinking) {
        isThinking = thinking;
    }

    public boolean isSummary() {
        return isSummary;
    }

    public void setSummary(boolean summary) {
        isSummary = summary;
    }

    public String getThinkingType() {
        return thinkingType;
    }

    public void setThinkingType(String thinkingType) {
        this.thinkingType = thinkingType;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }
}
