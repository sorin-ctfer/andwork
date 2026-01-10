package com.example.movinghacker.ai;

/**
 * Function Handler接口
 * 每个功能模块实现此接口来提供AI可调用的功能
 */
public interface FunctionHandler {
    
    /**
     * 获取函数定义（OpenAI Function格式）
     * @return Function定义
     */
    FunctionDefinition getDefinition();
    
    /**
     * 执行函数
     * @param arguments JSON格式的参数
     * @return 执行结果
     */
    FunctionResult execute(String arguments);
    
    /**
     * 获取函数名称
     * @return 函数名称
     */
    String getName();
}
