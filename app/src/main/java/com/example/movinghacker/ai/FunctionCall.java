package com.example.movinghacker.ai;

/**
 * Function调用数据模型
 */
public class FunctionCall {
    private String id;
    private String name;
    private String arguments;  // JSON格式的参数

    public FunctionCall() {
    }

    public FunctionCall(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "FunctionCall{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", arguments='" + arguments + '\'' +
                '}';
    }
}
