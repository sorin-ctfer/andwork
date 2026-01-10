package com.example.movinghacker.ai.handlers;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.example.movinghacker.ai.FunctionDefinition;
import com.example.movinghacker.ai.FunctionHandler;
import com.example.movinghacker.ai.FunctionResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Python代码执行Function Handler
 * 支持执行Python 3.11代码，包含预装的requests和numpy库
 */
public class PythonExecuteFunctionHandler implements FunctionHandler {
    private static final String TAG = "PythonExecuteHandler";
    private static final String FUNCTION_NAME = "python_execute";
    
    private Context context;
    private Python python;
    private Gson gson = new Gson();

    public PythonExecuteFunctionHandler(Context context) {
        this.context = context;
        try {
            this.python = Python.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get Python instance", e);
        }
    }

    @Override
    public String getName() {
        return FUNCTION_NAME;
    }

    @Override
    public FunctionDefinition getDefinition() {
        FunctionDefinition def = new FunctionDefinition();
        def.setName(FUNCTION_NAME);
        def.setDescription(
            "Execute Python 3.11 code with access to native Python libraries and pre-installed packages. " +
            "This function runs Python code in a Chaquopy environment with the following capabilities:\n\n" +
            "**Python Version**: Python 3.11\n\n" +
            "**Pre-installed Packages**:\n" +
            "- requests: HTTP library for making web requests\n" +
            "- numpy: Numerical computing library for arrays and mathematical operations\n" +
            "- certifi: SSL certificate bundle\n" +
            "- charset-normalizer: Character encoding detection\n" +
            "- idna: Internationalized Domain Names support\n" +
            "- urllib3: HTTP client library\n\n" +
            "**Native Python Libraries**: All standard Python 3.11 built-in modules are available " +
            "(os, sys, json, math, datetime, re, etc.)\n\n" +
            "**Output**: Captures both stdout and stderr output from the code execution.\n\n" +
            "**Use Cases**:\n" +
            "- Data processing and analysis with numpy\n" +
            "- Making HTTP requests with requests library\n" +
            "- Text processing and manipulation\n" +
            "- Mathematical calculations\n" +
            "- JSON data handling\n" +
            "- File operations (within app permissions)\n\n" +
            "**Example Usage**:\n" +
            "```python\n" +
            "import requests\n" +
            "response = requests.get('https://api.example.com/data')\n" +
            "print(response.json())\n" +
            "```\n\n" +
            "```python\n" +
            "import numpy as np\n" +
            "arr = np.array([1, 2, 3, 4, 5])\n" +
            "print(f'Mean: {np.mean(arr)}')\n" +
            "print(f'Sum: {np.sum(arr)}')\n" +
            "```"
        );

        Map<String, Object> parameters = new HashMap<>();
        
        Map<String, Object> codeParam = new HashMap<>();
        codeParam.put("type", "string");
        codeParam.put("description", 
            "The Python code to execute. Can be single or multiple lines. " +
            "Use print() to output results. The code will be executed in a persistent " +
            "Python environment where variables and imports are maintained across calls."
        );
        parameters.put("code", codeParam);

        // 添加required字段
        java.util.List<String> required = new java.util.ArrayList<>();
        required.add("code");
        parameters.put("required", required);

        def.setParameters(parameters);

        return def;
    }

    @Override
    public FunctionResult execute(String arguments) {
        // 解析参数
        Map<String, Object> args;
        try {
            args = gson.fromJson(arguments, new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
        } catch (Exception e) {
            return FunctionResult.error(null, "Invalid arguments: " + e.getMessage());
        }

        if (python == null) {
            return FunctionResult.error(null, "Python environment not initialized");
        }

        // 获取参数
        String code = (String) args.get("code");
        
        if (code == null || code.trim().isEmpty()) {
            return FunctionResult.error(null, "Parameter 'code' is required and cannot be empty");
        }

        try {
            Log.d(TAG, "Executing Python code: " + code.substring(0, Math.min(100, code.length())));
            
            // 获取Python模块
            PyObject sys = python.getModule("sys");
            PyObject io = python.getModule("io");
            PyObject builtins = python.getBuiltins();
            
            // 重定向stdout和stderr到StringIO
            PyObject stringIO = io.callAttr("StringIO");
            sys.put("stdout", stringIO);
            sys.put("stderr", stringIO);
            
            // 获取 __main__ 模块的全局命名空间
            PyObject mainModule = python.getModule("__main__");
            PyObject globals = mainModule.get("__dict__");
            
            // 编译并执行代码
            PyObject compiled = builtins.callAttr("compile", code, "<string>", "exec");
            builtins.callAttr("eval", compiled, globals);
            
            // 获取输出
            String output = stringIO.callAttr("getvalue").toString();
            
            // 构建结果
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("output", output.isEmpty() ? "Code executed successfully with no output" : output);
            result.addProperty("code_length", code.length());
            
            Log.d(TAG, "Python execution successful");
            return FunctionResult.success(null, result.toString());
            
        } catch (PyException e) {
            // Python执行错误
            String errorMsg = e.getMessage();
            Log.e(TAG, "Python execution error: " + errorMsg);
            
            JsonObject errorResult = new JsonObject();
            errorResult.addProperty("success", false);
            errorResult.addProperty("error_type", "PythonError");
            errorResult.addProperty("error_message", errorMsg);
            errorResult.addProperty("code", code);
            
            return FunctionResult.error(null, "Python execution failed: " + errorMsg);
            
        } catch (Exception e) {
            // 其他错误
            String errorMsg = e.getMessage();
            Log.e(TAG, "Unexpected error during Python execution", e);
            
            JsonObject errorResult = new JsonObject();
            errorResult.addProperty("success", false);
            errorResult.addProperty("error_type", "SystemError");
            errorResult.addProperty("error_message", errorMsg);
            
            return FunctionResult.error(null, "Failed to execute Python code: " + errorMsg);
        }
    }
}
