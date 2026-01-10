package com.example.movinghacker.ai;

import java.util.HashMap;
import java.util.Map;

/**
 * Function定义数据模型（OpenAI Function格式）
 */
public class FunctionDefinition {
    private String name;
    private String description;
    private Map<String, Object> parameters;  // JSON Schema格式

    public FunctionDefinition() {
        this.parameters = new HashMap<>();
    }

    public FunctionDefinition(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "FunctionDefinition{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
