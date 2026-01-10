package com.example.movinghacker.ai;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Function注册中心
 * 管理所有可用的Function Handler
 */
public class FunctionRegistry {
    private static final String TAG = "FunctionRegistry";
    private static FunctionRegistry instance;
    private Map<String, FunctionHandler> handlers;
    private Context context;

    private FunctionRegistry() {
        handlers = new HashMap<>();
    }

    public static synchronized FunctionRegistry getInstance() {
        if (instance == null) {
            instance = new FunctionRegistry();
        }
        return instance;
    }

    public static synchronized FunctionRegistry getInstance(Context context) {
        if (instance == null) {
            instance = new FunctionRegistry();
        }
        if (instance.context == null && context != null) {
            instance.initialize(context);
        }
        return instance;
    }

    /**
     * 初始化并注册所有Function
     * @param context Application Context
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        registerAllFunctions();
    }

    /**
     * 注册Function Handler
     * @param handler Function Handler实例
     */
    public void register(FunctionHandler handler) {
        if (handler == null) {
            Log.w(TAG, "Attempted to register null handler");
            return;
        }
        
        String name = handler.getName();
        if (name == null || name.isEmpty()) {
            Log.w(TAG, "Handler has invalid name");
            return;
        }
        
        handlers.put(name, handler);
        Log.d(TAG, "Registered function: " + name);
    }

    /**
     * 获取所有Function定义
     * @return Function定义列表
     */
    public List<FunctionDefinition> getAllDefinitions() {
        List<FunctionDefinition> definitions = new ArrayList<>();
        for (FunctionHandler handler : handlers.values()) {
            try {
                FunctionDefinition def = handler.getDefinition();
                if (def != null) {
                    definitions.add(def);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting definition from handler: " + handler.getName(), e);
            }
        }
        return definitions;
    }

    /**
     * 执行Function
     * @param call Function调用信息
     * @return 执行结果
     */
    public FunctionResult execute(FunctionCall call) {
        if (call == null) {
            return FunctionResult.error(null, "Function call is null");
        }

        String functionName = call.getName();
        FunctionHandler handler = handlers.get(functionName);
        
        if (handler == null) {
            String error = "Function not found: " + functionName;
            Log.w(TAG, error);
            return FunctionResult.error(call.getId(), error);
        }

        try {
            Log.d(TAG, "Executing function: " + functionName);
            FunctionResult result = handler.execute(call.getArguments());
            
            // 设置function call ID
            if (result != null) {
                result.setFunctionCallId(call.getId());
            }
            
            return result;
        } catch (Exception e) {
            String error = "Error executing function " + functionName + ": " + e.getMessage();
            Log.e(TAG, error, e);
            return FunctionResult.error(call.getId(), error);
        }
    }

    /**
     * 获取已注册的Function数量
     * @return Function数量
     */
    public int getRegisteredCount() {
        return handlers.size();
    }

    /**
     * 检查Function是否已注册
     * @param name Function名称
     * @return 是否已注册
     */
    public boolean isRegistered(String name) {
        return handlers.containsKey(name);
    }

    /**
     * 注册所有Function
     * 这个方法会在后续阶段逐步添加各个模块的Handler
     */
    private void registerAllFunctions() {
        Log.d(TAG, "Registering all functions...");
        
        // Web请求模块
        register(new com.example.movinghacker.ai.handlers.WebRequestFunctionHandler(context));
        
        // 文件管理模块
        register(new com.example.movinghacker.ai.handlers.FileListFunctionHandler(context));
        register(new com.example.movinghacker.ai.handlers.FileReadFunctionHandler(context));
        register(new com.example.movinghacker.ai.handlers.FileWriteFunctionHandler(context));
        register(new com.example.movinghacker.ai.handlers.FileDeleteFunctionHandler(context));
        register(new com.example.movinghacker.ai.handlers.FileSearchFunctionHandler(context));
        
        // Python执行模块
        register(new com.example.movinghacker.ai.handlers.PythonExecuteFunctionHandler(context));
        
        // 终端执行模块
        register(new com.example.movinghacker.ai.handlers.TerminalExecuteFunctionHandler(context));
        
        // TODO: 在后续阶段添加其他模块的Function Handler
        // 例如:
        // register(new SshExecuteFunctionHandler(context));
        // register(new CodeEditFunctionHandler(context));
        // 等等...
        
        Log.d(TAG, "Registered " + handlers.size() + " functions");
    }

    /**
     * 清除所有注册的Function（用于测试）
     */
    public void clear() {
        handlers.clear();
    }
}
