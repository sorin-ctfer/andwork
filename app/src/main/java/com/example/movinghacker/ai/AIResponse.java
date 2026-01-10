package com.example.movinghacker.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * AI API响应数据模型
 */
public class AIResponse {
    private String content;
    private List<FunctionCall> functionCalls;
    private String finishReason;  // "stop", "tool_calls", "length", "content_filter"

    public AIResponse() {
        this.functionCalls = new ArrayList<>();
    }

    public AIResponse(String content, List<FunctionCall> functionCalls, String finishReason) {
        this.content = content;
        this.functionCalls = functionCalls != null ? functionCalls : new ArrayList<>();
        this.finishReason = finishReason;
    }

    public boolean hasFunctionCalls() {
        return functionCalls != null && !functionCalls.isEmpty();
    }

    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }

    public boolean isComplete() {
        return "stop".equals(finishReason);
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<FunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    public void setFunctionCalls(List<FunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
    }

    public void addFunctionCall(FunctionCall functionCall) {
        if (this.functionCalls == null) {
            this.functionCalls = new ArrayList<>();
        }
        this.functionCalls.add(functionCall);
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    @Override
    public String toString() {
        return "AIResponse{" +
                "content='" + content + '\'' +
                ", functionCalls=" + functionCalls +
                ", finishReason='" + finishReason + '\'' +
                '}';
    }
}
